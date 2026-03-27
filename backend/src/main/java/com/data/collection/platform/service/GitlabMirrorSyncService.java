package com.data.collection.platform.service;

import com.data.collection.platform.common.JsonUtils;
import com.data.collection.platform.entity.GitlabSyncConfig;
import com.data.collection.platform.entity.GitlabMirrorRecord;
import com.data.collection.platform.entity.GitlabSyncTask;
import com.data.collection.platform.entity.SyncTriggerType;
import com.data.collection.platform.entity.SyncProgress;
import com.data.collection.platform.entity.SyncStatus;
import com.data.collection.platform.entity.SyncType;
import com.data.collection.platform.entity.TableWhitelistOption;
import com.data.collection.platform.mapper.GitlabMirrorRecordMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
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
        List<Map<String, Object>> rows;
        if (task.getTaskType() == SyncType.FULL) {
          rows = externalDbService.fullTableScan(config, table);
        } else if (task.getTaskType() == SyncType.COMPENSATION) {
          if (table.updatedAtColumn() == null || table.updatedAtColumn().isBlank()) {
            skippedTableCount++;
            tableCount++;
            currentProgress.setCompletedTables(tableCount);
            continue;
          }
          rows = externalDbService.compensationScan(config, table, since);
        } else {
          rows = externalDbService.incrementalScan(config, table, since);
        }
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
          mirrorRecordMapper.upsertBatch(mirrorRows.subList(index, end));
          recordCount += (end - index);
          currentProgress.setSyncedRecords(recordCount);
          taskService.heartbeat(taskId);
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
      logService.finish(logId, SyncStatus.CANCELLED, e.getMessage(), tableCount, recordCount);
      taskService.finish(taskId, SyncStatus.CANCELLED, e.getMessage(), null);
    } catch (Exception e) {
      logService.finish(logId, SyncStatus.FAILED, e.getMessage(), tableCount, recordCount);
      taskService.finish(
          taskId,
          SyncStatus.FAILED,
          e.getMessage(),
          LocalDateTime.now().plusMinutes(taskService.getFailureBackoffMinutes()));
    } finally {
      progressMap.remove(taskId);
      GitlabSyncTask nextTask = taskService.promoteNextQueued(task.getScopeKey());
      if (nextTask != null) {
        self.executeTaskAsync(nextTask.getId());
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
