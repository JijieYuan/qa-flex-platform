package com.data.collection.platform.service;

import com.data.collection.platform.common.JsonUtils;
import com.data.collection.platform.common.logging.GitlabSyncLogContext;
import com.data.collection.platform.entity.GitlabSyncConfig;
import com.data.collection.platform.entity.GitlabMirrorRecord;
import com.data.collection.platform.entity.GitlabSyncTask;
import com.data.collection.platform.entity.SyncProgress;
import com.data.collection.platform.entity.SyncStatus;
import com.data.collection.platform.entity.SyncTriggerType;
import com.data.collection.platform.entity.SyncType;
import com.data.collection.platform.entity.TableWhitelistOption;
import com.data.collection.platform.mapper.GitlabMirrorRecordMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class GitlabMirrorSyncService {
  private static final int UPSERT_BATCH_SIZE = 200;
  private static final int COMPENSATION_EXTRA_LOOKBACK_MINUTES = 60;

  private final GitlabConfigService configService;
  private final GitlabWhitelistService whitelistService;
  private final GitlabExternalDbService externalDbService;
  private final GitlabMirrorRecordMapper mirrorRecordMapper;
  private final GitlabSyncLogService logService;
  private final GitlabSyncTaskService taskService;
  private final JsonUtils jsonUtils;
  private final GitlabMirrorSyncService self;
  private final ConcurrentMap<Long, SyncProgress> progressMap = new ConcurrentHashMap<>();
  private final String lockOwner = java.util.UUID.randomUUID().toString();

  public GitlabMirrorSyncService(
      GitlabConfigService configService,
      GitlabWhitelistService whitelistService,
      GitlabExternalDbService externalDbService,
      GitlabMirrorRecordMapper mirrorRecordMapper,
      GitlabSyncLogService logService,
      GitlabSyncTaskService taskService,
      JsonUtils jsonUtils,
      @Lazy GitlabMirrorSyncService self) {
    this.configService = configService;
    this.whitelistService = whitelistService;
    this.externalDbService = externalDbService;
    this.mirrorRecordMapper = mirrorRecordMapper;
    this.logService = logService;
    this.taskService = taskService;
    this.jsonUtils = jsonUtils;
    this.self = self;
  }

  public boolean hasActiveTask(Long configId) {
    return taskService.hasActiveTask(configId);
  }

  public SyncProgress getProgress(Long taskId) {
    return taskId == null ? null : progressMap.get(taskId);
  }

  public void recoverTimedOutTasks() {
    taskService.recoverTimedOutTasks();
  }

  public void testConnection() {
    externalDbService.testConnection(configService.getConfig());
  }

  public GitlabSyncTask startFullSync() {
    return submitTask(SyncType.FULL, SyncTriggerType.MANUAL, "Manual full sync");
  }

  public GitlabSyncTask startIncrementalSync(SyncTriggerType triggerType, String message) {
    return submitTask(SyncType.INCREMENTAL, triggerType, message);
  }

  public GitlabSyncTask startCompensationSync() {
    return submitTask(SyncType.COMPENSATION, SyncTriggerType.SCHEDULE, "Scheduled compensation sync");
  }

  public GitlabSyncTask requestCancel(Long configId) {
    return taskService.requestCancelLatest(configId);
  }

  private GitlabSyncTask submitTask(SyncType type, SyncTriggerType triggerType, String message) {
    GitlabSyncConfig config = configService.getConfig();
    GitlabSyncTask task = taskService.submitTask(config, type, triggerType, message, Map.of());
    try (GitlabSyncLogContext.Scope context = GitlabSyncLogContext.openTask(task, config);
         GitlabSyncLogContext.Scope action = GitlabSyncLogContext.action("Task_Submit")) {
      log.info(
          "Sync task submission processed, triggerType={}, currentStatus={}, message={}",
          triggerType,
          task.getStatus(),
          message);
    }
    if (task.getStatus() == SyncStatus.PENDING) {
      self.executeTaskAsync(task.getId());
    }
    return task;
  }

  @Async
  public void executeTaskAsync(Long taskId) {
    executeTask(taskId);
  }

  private void executeTask(Long taskId) {
    GitlabSyncTask task = taskService.claimPendingTask(taskId, lockOwner);
    if (task == null) {
      return;
    }
    GitlabSyncConfig config = configService.getConfigById(task.getConfigId());
    try (GitlabSyncLogContext.Scope context = GitlabSyncLogContext.openTask(task, config)) {
      List<TableWhitelistOption> tables = whitelistService.resolveOptions(config);
      long logId = logService.start(
          config.getId(),
          task.getTaskType(),
          tables.stream().map(TableWhitelistOption::tableName).toList(),
          taskService.extractMessage(task));
      int tableCount = 0;
      int recordCount = 0;
      int skippedTableCount = 0;
      try {
        try (GitlabSyncLogContext.Scope action = GitlabSyncLogContext.action("Task_Start")) {
          log.info(
              "Task started, sourceMode={}, totalTables={}, triggerType={}, compensationWindowMinutes={}",
              task.getSourceMode(),
              tables.size(),
              task.getTriggerType(),
              task.getTaskType() == SyncType.COMPENSATION
                  ? config.getCompensationIntervalMinutes() + COMPENSATION_EXTRA_LOOKBACK_MINUTES
                  : 0);
        }

        SyncProgress currentProgress = new SyncProgress();
        currentProgress.setPhase(resolvePhase(task.getTaskType()));
        currentProgress.setTotalTables(tables.size());
        currentProgress.setCompletedTables(0);
        currentProgress.setSyncedRecords(0);
        currentProgress.setStartedAt(LocalDateTime.now());
        progressMap.put(taskId, currentProgress);

        checkCancelled(taskId);
        externalDbService.testConnection(config);
        LocalDateTime since = resolveSince(config, task.getTaskType());

        for (TableWhitelistOption table : tables) {
          checkCancelled(taskId);
          currentProgress.setCurrentTable(table.tableName());
          currentProgress.setCompletedTables(tableCount);
          currentProgress.setSyncedRecords(recordCount);
          taskService.heartbeat(taskId);

          if (task.getTaskType() == SyncType.COMPENSATION
              && (table.updatedAtColumn() == null || table.updatedAtColumn().isBlank())) {
            skippedTableCount++;
            tableCount++;
            currentProgress.setCompletedTables(tableCount);
            try (GitlabSyncLogContext.Scope action = GitlabSyncLogContext.action("Data_Fetching")) {
              log.info("Skipped table during compensation because no time column exists, tableName={}", table.tableName());
            }
            continue;
          }

          try (GitlabSyncLogContext.Scope action = GitlabSyncLogContext.action("Data_Fetching")) {
            log.info(
                "Fetching source data, tableName={}, scanMode={}, since={}, updatedAtColumn={}",
                table.tableName(),
                resolveScanMode(task.getTaskType()),
                since,
                table.updatedAtColumn());
          }

          List<Map<String, Object>> rows = switch (task.getTaskType()) {
            case FULL -> externalDbService.fullTableScan(config, table);
            case COMPENSATION -> externalDbService.compensationScan(config, table, since);
            case INCREMENTAL, WEBHOOK -> externalDbService.incrementalScan(config, table, since);
          };

          List<GitlabMirrorRecord> mirrorRows = new ArrayList<>(rows.size());
          for (Map<String, Object> row : rows) {
            GitlabMirrorRecord record = new GitlabMirrorRecord();
            record.setConfigId(config.getId());
            record.setTableName(table.tableName());
            record.setRecordKey(externalDbService.buildRecordKey(table, row));
            record.setUpdatedAtSource(externalDbService.extractUpdatedAt(table, row));
            record.setRowData(jsonUtils.toJson(row));
            mirrorRows.add(record);
          }

          for (int index = 0; index < mirrorRows.size(); index += UPSERT_BATCH_SIZE) {
            checkCancelled(taskId);
            int end = Math.min(index + UPSERT_BATCH_SIZE, mirrorRows.size());
            int batchSize = end - index;
            try (GitlabSyncLogContext.Scope action = GitlabSyncLogContext.action("Mirror_Writing")) {
              log.info(
                  "Writing mirror batch, tableName={}, batchSize={}, batchStart={}, batchEnd={}",
                  table.tableName(),
                  batchSize,
                  index,
                  end);
            }
            mirrorRecordMapper.upsertBatch(mirrorRows.subList(index, end));
            recordCount += batchSize;
            currentProgress.setSyncedRecords(recordCount);
            taskService.heartbeat(taskId);
            try (GitlabSyncLogContext.Scope action = GitlabSyncLogContext.action("Commit_Success")) {
              log.info(
                  "Mirror batch committed, tableName={}, batchSize={}, cumulativeRecords={}",
                  table.tableName(),
                  batchSize,
                  recordCount);
            }
          }
          tableCount++;
          currentProgress.setCompletedTables(tableCount);
          currentProgress.setSyncedRecords(recordCount);
        }

        configService.updateSyncTime(config.getId(), task.getTaskType() == SyncType.FULL);
        String successMessage = buildCompletionMessage(task.getTaskType(), skippedTableCount);
        logService.finish(logId, SyncStatus.SUCCESS, successMessage, tableCount, recordCount);
        taskService.finish(taskId, SyncStatus.SUCCESS, successMessage, null);
      } catch (SyncCancelledException e) {
        try (GitlabSyncLogContext.Scope action = GitlabSyncLogContext.action("Task_End")) {
          log.warn("Task cancelled during execution, completedTables={}, syncedRecords={}", tableCount, recordCount, e);
        }
        logService.finish(logId, SyncStatus.CANCELLED, e.getMessage(), tableCount, recordCount);
        taskService.finish(taskId, SyncStatus.CANCELLED, e.getMessage(), null);
      } catch (Exception e) {
        LocalDateTime cooldownUntil = LocalDateTime.now().plusMinutes(taskService.getFailureBackoffMinutes());
        try (GitlabSyncLogContext.Scope action = GitlabSyncLogContext.action("Task_End")) {
          log.error(
              "Task failed, completedTables={}, syncedRecords={}, retryCount={}, retryBackoffMinutes={}",
              tableCount,
              recordCount,
              task.getRetryCount(),
              taskService.getFailureBackoffMinutes(),
              e);
        }
        logService.finish(logId, SyncStatus.FAILED, e.getMessage(), tableCount, recordCount);
        taskService.finish(taskId, SyncStatus.FAILED, e.getMessage(), cooldownUntil);
      } finally {
        progressMap.remove(taskId);
        GitlabSyncTask nextTask = taskService.promoteNextQueued(task.getScopeKey());
        if (nextTask != null) {
          self.executeTaskAsync(nextTask.getId());
        }
      }
    }
  }

  private String resolvePhase(SyncType type) {
    return switch (type) {
      case FULL -> "FULL_SYNC";
      case COMPENSATION -> "COMPENSATION_SYNC";
      case INCREMENTAL, WEBHOOK -> "INCREMENTAL_SYNC";
    };
  }

  private String resolveScanMode(SyncType type) {
    return switch (type) {
      case FULL -> "FULL";
      case COMPENSATION -> "WINDOW_COMPENSATION";
      case INCREMENTAL, WEBHOOK -> "INCREMENTAL";
    };
  }

  private void checkCancelled(Long taskId) {
    if (taskService.isCancelRequested(taskId)) {
      throw new SyncCancelledException("Sync cancelled by user");
    }
  }

  private LocalDateTime resolveSince(GitlabSyncConfig config, SyncType type) {
    if (type == SyncType.FULL) {
      return null;
    }
    if (type == SyncType.COMPENSATION) {
      int intervalMinutes = config.getCompensationIntervalMinutes() == null ? 10 : config.getCompensationIntervalMinutes();
      return LocalDateTime.now().minusMinutes((long) intervalMinutes + COMPENSATION_EXTRA_LOOKBACK_MINUTES);
    }
    return config.getLastIncrementalSyncAt();
  }

  private String buildCompletionMessage(SyncType type, int skippedTableCount) {
    if (type != SyncType.COMPENSATION || skippedTableCount <= 0) {
      return "Sync completed successfully";
    }
    return "Sync completed successfully, skipped %d tables without time columns during compensation window scan"
        .formatted(skippedTableCount);
  }

  private static final class SyncCancelledException extends RuntimeException {
    private SyncCancelledException(String message) {
      super(message);
    }
  }
}
