package com.data.collection.platform.service;

import com.data.collection.platform.common.JsonUtils;
import com.data.collection.platform.entity.GitlabSyncConfig;
import com.data.collection.platform.entity.GitlabMirrorRecord;
import com.data.collection.platform.entity.SyncProgress;
import com.data.collection.platform.entity.SyncStatus;
import com.data.collection.platform.entity.SyncType;
import com.data.collection.platform.entity.TableWhitelistOption;
import com.data.collection.platform.mapper.GitlabMirrorRecordMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class GitlabMirrorSyncService {
  private static final int UPSERT_BATCH_SIZE = 200;

  private final GitlabConfigService configService;
  private final GitlabWhitelistService whitelistService;
  private final GitlabExternalDbService externalDbService;
  private final GitlabMirrorRecordMapper mirrorRecordMapper;
  private final GitlabSyncLogService logService;
  private final JsonUtils jsonUtils;
  private final AtomicBoolean running = new AtomicBoolean(false);
  private final AtomicReference<SyncProgress> progress = new AtomicReference<>();

  public GitlabMirrorSyncService(
      GitlabConfigService configService,
      GitlabWhitelistService whitelistService,
      GitlabExternalDbService externalDbService,
      GitlabMirrorRecordMapper mirrorRecordMapper,
      GitlabSyncLogService logService,
      JsonUtils jsonUtils) {
    this.configService = configService;
    this.whitelistService = whitelistService;
    this.externalDbService = externalDbService;
    this.mirrorRecordMapper = mirrorRecordMapper;
    this.logService = logService;
    this.jsonUtils = jsonUtils;
  }

  public boolean isRunning() {
    return running.get();
  }

  public SyncProgress getProgress() {
    return progress.get();
  }

  public void reconcileRunningState(Long configId) {
    if (running.get() || configId == null) {
      return;
    }
    logService.markRunningAsFailed(configId, "Recovered stale running task after process interruption");
  }

  public void testConnection() {
    externalDbService.testConnection(configService.getConfig());
  }

  @Async
  public void startFullSync() {
    runSync(SyncType.FULL, "Manual full sync");
  }

  @Async
  public void startIncrementalSync(String message) {
    runSync(SyncType.INCREMENTAL, message);
  }

  @Async
  public void startCompensationSync() {
    runSync(SyncType.COMPENSATION, "Scheduled compensation sync");
  }

  private void runSync(SyncType type, String message) {
    GitlabSyncConfig config = configService.getConfig();
    if (config.getId() != null) {
      logService.markRunningAsFailed(config.getId(), "Recovered stale running task before starting a new sync");
    }
    if (!running.compareAndSet(false, true)) {
      return;
    }
    List<TableWhitelistOption> tables = whitelistService.resolveOptions(config);
    long logId = logService.start(config.getId(), type, tables.stream().map(TableWhitelistOption::tableName).toList(), message);
    int tableCount = 0;
    int recordCount = 0;
    try {
      SyncProgress currentProgress = new SyncProgress();
      currentProgress.setPhase(resolvePhase(type));
      currentProgress.setTotalTables(tables.size());
      currentProgress.setCompletedTables(0);
      currentProgress.setSyncedRecords(0);
      currentProgress.setStartedAt(LocalDateTime.now());
      progress.set(currentProgress);
      externalDbService.testConnection(config);
      LocalDateTime since = type == SyncType.FULL ? null : config.getLastIncrementalSyncAt();
      for (TableWhitelistOption table : tables) {
        currentProgress.setCurrentTable(table.tableName());
        currentProgress.setCompletedTables(tableCount);
        currentProgress.setSyncedRecords(recordCount);
        List<Map<String, Object>> rows = shouldUseFullScan(type)
            ? externalDbService.fullTableScan(config, table)
            : externalDbService.incrementalScan(config, table, since);
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
          int end = Math.min(index + UPSERT_BATCH_SIZE, mirrorRows.size());
          mirrorRecordMapper.upsertBatch(mirrorRows.subList(index, end));
          recordCount += (end - index);
          currentProgress.setSyncedRecords(recordCount);
        }
        tableCount++;
        currentProgress.setCompletedTables(tableCount);
        currentProgress.setSyncedRecords(recordCount);
      }
      configService.updateSyncTime(config.getId(), type == SyncType.FULL);
      logService.finish(logId, SyncStatus.SUCCESS, "Sync completed successfully", tableCount, recordCount);
    } catch (Exception e) {
      logService.finish(logId, SyncStatus.FAILED, e.getMessage(), tableCount, recordCount);
    } finally {
      running.set(false);
      progress.set(null);
    }
  }

  private boolean shouldUseFullScan(SyncType type) {
    return type == SyncType.FULL || type == SyncType.COMPENSATION;
  }

  private String resolvePhase(SyncType type) {
    return switch (type) {
      case FULL -> "FULL_SYNC";
      case COMPENSATION -> "COMPENSATION_SYNC";
      case INCREMENTAL, WEBHOOK -> "INCREMENTAL_SYNC";
    };
  }
}
