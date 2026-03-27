package com.data.collection.platform.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.data.collection.platform.common.JsonUtils;
import com.data.collection.platform.entity.GitlabSyncConfig;
import com.data.collection.platform.entity.GitlabSyncTask;
import com.data.collection.platform.entity.SourceMode;
import com.data.collection.platform.entity.SyncStatus;
import com.data.collection.platform.entity.SyncTriggerType;
import com.data.collection.platform.entity.SyncType;
import com.data.collection.platform.entity.WhitelistMode;
import com.data.collection.platform.mapper.GitlabMirrorRecordMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GitlabMirrorSyncServiceTest {
  private GitlabConfigService configService;
  private GitlabWhitelistService whitelistService;
  private GitlabExternalDbService externalDbService;
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
    when(taskService.submitTask(config, SyncType.FULL, SyncTriggerType.MANUAL, "Manual full sync", Map.of()))
        .thenReturn(task);

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
    when(taskService.submitTask(config, SyncType.INCREMENTAL, SyncTriggerType.WEBHOOK, "Triggered by webhook", Map.of()))
        .thenReturn(task);

    syncService.startIncrementalSync(SyncTriggerType.WEBHOOK, "Triggered by webhook");

    verify(selfProxy, org.mockito.Mockito.never()).executeTaskAsync(anyLong());
  }

  @Test
  void recoverTimedOutTasksShouldDelegateToTaskService() {
    syncService.recoverTimedOutTasks();

    verify(taskService).recoverTimedOutTasks();
  }

  private GitlabSyncConfig baseConfig() {
    GitlabSyncConfig config = new GitlabSyncConfig();
    config.setId(1L);
    config.setSourceMode(SourceMode.DOCKER);
    config.setWhitelistMode(WhitelistMode.RECOMMENDED);
    config.setWhitelistTables(List.of());
    return config;
  }
}
