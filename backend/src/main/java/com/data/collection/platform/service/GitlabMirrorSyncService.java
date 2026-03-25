package com.data.collection.platform.service;

import com.data.collection.platform.entity.GitlabSyncConfig;
import com.data.collection.platform.entity.SyncStatus;
import com.data.collection.platform.entity.SyncType;
import com.data.collection.platform.entity.TableWhitelistOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class GitlabMirrorSyncService {
  private final GitlabConfigService configService;
  private final GitlabWhitelistService whitelistService;
  private final GitlabExternalDbService externalDbService;
  private final GitlabMirrorRepository mirrorRepository;
  private final GitlabSyncLogService logService;
  private final AtomicBoolean running = new AtomicBoolean(false);

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
      externalDbService.testConnection(config);
      LocalDateTime since = type == SyncType.FULL ? null : config.getLastIncrementalSyncAt();
      for (TableWhitelistOption table : tables) {
        List<Map<String, Object>> rows = type == SyncType.FULL
            ? externalDbService.fullTableScan(config, table)
            : externalDbService.incrementalScan(config, table, since);
        for (Map<String, Object> row : rows) {
          mirrorRepository.upsert(
              config.getId(),
              table.tableName(),
              externalDbService.buildRecordKey(table, row),
              externalDbService.extractUpdatedAt(table, row),
              row);
          recordCount++;
        }
        tableCount++;
      }
      configService.updateSyncTime(config.getId(), type == SyncType.FULL || type == SyncType.COMPENSATION);
      if (type == SyncType.INCREMENTAL || type == SyncType.WEBHOOK) {
        configService.updateSyncTime(config.getId(), false);
      }
      logService.finish(logId, SyncStatus.SUCCESS, "Sync completed successfully", tableCount, recordCount);
    } catch (Exception e) {
      logService.finish(logId, SyncStatus.FAILED, e.getMessage(), tableCount, recordCount);
    } finally {
      running.set(false);
    }
  }
}
