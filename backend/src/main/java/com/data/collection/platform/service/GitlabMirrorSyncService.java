package com.data.collection.platform.service;

import com.data.collection.platform.entity.GitlabSyncConfig;
import com.data.collection.platform.entity.SyncProgress;
import com.data.collection.platform.entity.SyncStatus;
import com.data.collection.platform.entity.SyncType;
import com.data.collection.platform.entity.TableWhitelistOption;
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
  private final GitlabMirrorRepository mirrorRepository;
  private final GitlabSyncLogService logService;
  private final AtomicBoolean running = new AtomicBoolean(false);
  private final AtomicReference<SyncProgress> progress = new AtomicReference<>();

  public GitlabMirrorSyncService(
      GitlabConfigService configService,
      GitlabWhitelistService whitelistService,
      GitlabExternalDbService externalDbService,
      GitlabMirrorRepository mirrorRepository,
      GitlabSyncLogService logService) {
    this.configService = configService;
    this.whitelistService = whitelistService;
    this.externalDbService = externalDbService;
    this.mirrorRepository = mirrorRepository;
    this.logService = logService;
  }

  public boolean isRunning() {
    return running.get();
  }

  public SyncProgress getProgress() {
    return progress.get();
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
    if (!running.compareAndSet(false, true)) {
      return;
    }
    GitlabSyncConfig config = configService.getConfig();
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
        List<GitlabMirrorRepository.MirrorRow> mirrorRows = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
          mirrorRows.add(new GitlabMirrorRepository.MirrorRow(
              externalDbService.buildRecordKey(table, row),
              externalDbService.extractUpdatedAt(table, row),
              row));
        }
        for (int index = 0; index < mirrorRows.size(); index += UPSERT_BATCH_SIZE) {
          int end = Math.min(index + UPSERT_BATCH_SIZE, mirrorRows.size());
          mirrorRepository.upsertBatch(config.getId(), table.tableName(), mirrorRows.subList(index, end));
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
