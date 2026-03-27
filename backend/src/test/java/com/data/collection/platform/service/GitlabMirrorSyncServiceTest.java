package com.data.collection.platform.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.data.collection.platform.common.JsonUtils;
import com.data.collection.platform.entity.GitlabSyncConfig;
import com.data.collection.platform.entity.GitlabSyncTask;
import com.data.collection.platform.entity.SourceMode;
import com.data.collection.platform.entity.SourceTableColumn;
import com.data.collection.platform.entity.SourceTableSchema;
import com.data.collection.platform.entity.SyncStatus;
import com.data.collection.platform.entity.SyncSubmissionAction;
import com.data.collection.platform.entity.SyncTaskSubmissionResult;
import com.data.collection.platform.entity.SyncTriggerType;
import com.data.collection.platform.entity.SyncType;
import com.data.collection.platform.entity.TableWhitelistOption;
import com.data.collection.platform.entity.WhitelistMode;
import com.data.collection.platform.mapper.GitlabMirrorRecordMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GitlabMirrorSyncServiceTest {
  private GitlabConfigService configService;
  private GitlabWhitelistService whitelistService;
  private GitlabExternalDbService externalDbService;
  private GitlabMirrorSchemaService mirrorSchemaService;
  private GitlabMirrorTableStorageService mirrorTableStorageService;
  private GitlabMirrorRecordMapper mirrorRecordMapper;
  private GitlabSyncLogService logService;
  private GitlabSyncTaskService taskService;
  private JsonUtils jsonUtils;
  private GitlabMirrorSyncService selfProxy;
  private GitlabMirrorSyncService syncService;

  @BeforeEach
  void setUp() {
    configService = mock(GitlabConfigService.class);
    whitelistService = mock(GitlabWhitelistService.class);
    externalDbService = mock(GitlabExternalDbService.class);
    mirrorSchemaService = mock(GitlabMirrorSchemaService.class);
    mirrorTableStorageService = mock(GitlabMirrorTableStorageService.class);
    mirrorRecordMapper = mock(GitlabMirrorRecordMapper.class);
    logService = mock(GitlabSyncLogService.class);
    taskService = mock(GitlabSyncTaskService.class);
    jsonUtils = mock(JsonUtils.class);
    selfProxy = mock(GitlabMirrorSyncService.class);
    syncService =
        new GitlabMirrorSyncService(
            configService,
            whitelistService,
            externalDbService,
            mirrorSchemaService,
            mirrorTableStorageService,
            mirrorRecordMapper,
            logService,
            taskService,
            jsonUtils,
            selfProxy);
  }

  @Test
  void fullSyncShouldDispatchAsyncExecutionWhenTaskIsPending() {
    GitlabSyncConfig config = baseConfig();
    GitlabSyncTask task = new GitlabSyncTask();
    task.setId(100L);
    task.setStatus(SyncStatus.PENDING);

    when(configService.getConfig()).thenReturn(config);
    when(taskService.submitTaskResult(config, SyncType.FULL, SyncTriggerType.MANUAL, "Manual full sync", Map.of()))
        .thenReturn(new SyncTaskSubmissionResult(task, SyncSubmissionAction.CREATED));

    syncService.startFullSync();

    verify(selfProxy).executeTaskAsync(100L);
  }

  @Test
  void incrementalSyncShouldNotDispatchWhenTaskIsQueued() {
    GitlabSyncConfig config = baseConfig();
    GitlabSyncTask task = new GitlabSyncTask();
    task.setId(101L);
    task.setStatus(SyncStatus.QUEUED);

    when(configService.getConfig()).thenReturn(config);
    when(taskService.submitTaskResult(config, SyncType.INCREMENTAL, SyncTriggerType.WEBHOOK, "Triggered by webhook", Map.of()))
        .thenReturn(new SyncTaskSubmissionResult(task, SyncSubmissionAction.QUEUED));

    syncService.startIncrementalSync(SyncTriggerType.WEBHOOK, "Triggered by webhook");

    verify(selfProxy, org.mockito.Mockito.never()).executeTaskAsync(anyLong());
  }

  @Test
  void recoverTimedOutTasksShouldDelegateToTaskService() {
    syncService.recoverTimedOutTasks();

    verify(taskService).recoverTimedOutTasks();
  }

  @Test
  void compensationSyncShouldUseWindowedScanForTablesWithTimeColumn() {
    GitlabSyncTask task = runningTask(SyncType.COMPENSATION);
    GitlabSyncConfig config = baseConfig();
    config.setCompensationIntervalMinutes(30);
    TableWhitelistOption option = new TableWhitelistOption("issues", "issues", "id", "updated_at", true);

    when(taskService.claimPendingTask(eq(200L), anyString())).thenReturn(task);
    when(configService.getConfigById(1L)).thenReturn(config);
    when(whitelistService.resolveOptions(config)).thenReturn(List.of(option));
    when(mirrorSchemaService.prepareMirrorTable(config, option))
        .thenReturn(new GitlabMirrorSchemaService.PreparedMirrorTable(sourceSchema(option), "ods_gitlab_issues", true, null));
    when(taskService.extractMessage(task)).thenReturn("Scheduled compensation sync");
    when(logService.start(anyLong(), any(), any(), anyString())).thenReturn(1L);
    when(externalDbService.compensationScan(any(), any(), any())).thenReturn(List.of());
    when(taskService.promoteNextQueued(anyString())).thenReturn(null);

    syncService.executeTaskAsync(200L);

    verify(externalDbService).compensationScan(any(), any(), any());
    verify(externalDbService, never()).fullTableScan(any(), any());
    verify(externalDbService, never()).incrementalScan(any(), any(), any());
  }

  @Test
  void compensationSyncShouldSkipTablesWithoutTimeColumn() {
    GitlabSyncTask task = runningTask(SyncType.COMPENSATION);
    GitlabSyncConfig config = baseConfig();
    config.setCompensationIntervalMinutes(30);
    TableWhitelistOption option = new TableWhitelistOption("events", "events", "id", null, false);

    when(taskService.claimPendingTask(eq(201L), anyString())).thenReturn(task);
    when(configService.getConfigById(1L)).thenReturn(config);
    when(whitelistService.resolveOptions(config)).thenReturn(List.of(option));
    when(mirrorSchemaService.prepareMirrorTable(config, option))
        .thenReturn(new GitlabMirrorSchemaService.PreparedMirrorTable(sourceSchema(option), "ods_gitlab_events", true, null));
    when(taskService.extractMessage(task)).thenReturn("Scheduled compensation sync");
    when(logService.start(anyLong(), any(), any(), anyString())).thenReturn(1L);
    when(taskService.promoteNextQueued(anyString())).thenReturn(null);

    syncService.executeTaskAsync(201L);

    verify(externalDbService, never()).compensationScan(any(), any(), any());
    verify(logService).finish(eq(1L), eq(SyncStatus.SUCCESS), contains("skipped 1 tables"), eq(1), eq(0));
  }

  private GitlabSyncConfig baseConfig() {
    GitlabSyncConfig config = new GitlabSyncConfig();
    config.setId(1L);
    config.setSourceMode(SourceMode.DOCKER);
    config.setWhitelistMode(WhitelistMode.RECOMMENDED);
    config.setWhitelistTables(List.of());
    return config;
  }

  private GitlabSyncTask runningTask(SyncType type) {
    GitlabSyncTask task = new GitlabSyncTask();
    task.setId(1L);
    task.setConfigId(1L);
    task.setScopeKey("gitlab:docker:" + type.name());
    task.setTaskType(type);
    task.setStatus(SyncStatus.RUNNING);
    task.setStartedAt(LocalDateTime.now());
    return task;
  }

  private SourceTableSchema sourceSchema(TableWhitelistOption option) {
    return new SourceTableSchema(
        "ods_gitlab_" + option.tableName(),
        List.of("id"),
        option.updatedAtColumn(),
        List.of(new SourceTableColumn("id", "bigint", false, 1)));
  }
}
