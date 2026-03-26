package com.data.collection.platform.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.data.collection.platform.config.GitlabMirrorProperties;
import com.data.collection.platform.entity.GitlabSyncConfig;
import com.data.collection.platform.entity.GitlabSyncLog;
import com.data.collection.platform.entity.SyncStatus;
import com.data.collection.platform.entity.SyncType;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GitlabCompensationSchedulerTest {
  private GitlabConfigService configService;
  private GitlabMirrorSyncService syncService;
  private GitlabSyncLogService logService;
  private GitlabCompensationScheduler scheduler;

  @BeforeEach
  void setUp() {
    GitlabMirrorProperties properties = new GitlabMirrorProperties();
    properties.setSchedulerEnabled(true);
    properties.setSchedulerDelayMs(1000);

    configService = mock(GitlabConfigService.class);
    syncService = mock(GitlabMirrorSyncService.class);
    logService = mock(GitlabSyncLogService.class);
    scheduler = new GitlabCompensationScheduler(properties, configService, syncService, logService);
  }

  @Test
  void shouldTriggerCompensationWhenLatestActivityExceededInterval() {
    GitlabSyncConfig config = baseConfig();
    config.setLastIncrementalSyncAt(LocalDateTime.now().minusMinutes(20));
    when(syncService.isRunning()).thenReturn(false);
    when(configService.getConfig()).thenReturn(config);
    when(logService.findLatest(1L)).thenReturn(null);

    scheduler.run();

    verify(syncService).reconcileRunningState(1L);
    verify(syncService).startCompensationSync();
  }

  @Test
  void shouldSkipCompensationWhenRecentFailedAttemptExists() {
    GitlabSyncConfig config = baseConfig();
    config.setLastIncrementalSyncAt(LocalDateTime.now().minusMinutes(40));
    GitlabSyncLog latest = new GitlabSyncLog();
    latest.setSyncType(SyncType.COMPENSATION);
    latest.setStatus(SyncStatus.FAILED);
    latest.setStartedAt(LocalDateTime.now().minusMinutes(2));
    latest.setFinishedAt(LocalDateTime.now().minusMinutes(1));

    when(syncService.isRunning()).thenReturn(false);
    when(configService.getConfig()).thenReturn(config);
    when(logService.findLatest(1L)).thenReturn(latest);

    scheduler.run();

    verify(syncService).reconcileRunningState(1L);
    verify(syncService, never()).startCompensationSync();
  }

  private GitlabSyncConfig baseConfig() {
    GitlabSyncConfig config = new GitlabSyncConfig();
    config.setId(1L);
    config.setEnabled(true);
    config.setAutoSyncEnabled(true);
    config.setCompensationIntervalMinutes(10);
    return config;
  }
}
