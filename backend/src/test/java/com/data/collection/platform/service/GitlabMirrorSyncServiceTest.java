package com.data.collection.platform.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.data.collection.platform.entity.GitlabSyncConfig;
import com.data.collection.platform.entity.SyncStatus;
import com.data.collection.platform.entity.SyncType;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class GitlabMirrorSyncServiceTest {
  private GitlabConfigService configService;
  private GitlabWhitelistService whitelistService;
  private GitlabExternalDbService externalDbService;
  private GitlabMirrorRepository mirrorRepository;
  private GitlabSyncLogService logService;
  private GitlabMirrorSyncService syncService;

  @BeforeEach
  void setUp() {
    configService = mock(GitlabConfigService.class);
    whitelistService = mock(GitlabWhitelistService.class);
    externalDbService = mock(GitlabExternalDbService.class);
    mirrorRepository = mock(GitlabMirrorRepository.class);
    logService = mock(GitlabSyncLogService.class);
    syncService =
        new GitlabMirrorSyncService(
            configService, whitelistService, externalDbService, mirrorRepository, logService);
  }

  @Test
  void compensationSyncShouldRefreshIncrementalTimestampInsteadOfFullTimestamp() {
    GitlabSyncConfig config = new GitlabSyncConfig();
    config.setId(1L);
    when(configService.getConfig()).thenReturn(config);
    when(whitelistService.resolveOptions(config)).thenReturn(List.of());
    when(logService.start(anyLong(), any(), anyList(), anyString())).thenReturn(100L);
    doNothing().when(externalDbService).testConnection(config);

    ReflectionTestUtils.invokeMethod(
        syncService, "runSync", SyncType.COMPENSATION, "Scheduled compensation sync");

    verify(configService).updateSyncTime(1L, false);
    verify(logService).finish(100L, SyncStatus.SUCCESS, "Sync completed successfully", 0, 0);
  }

  @Test
  void fullSyncShouldRefreshFullTimestampOnly() {
    GitlabSyncConfig config = new GitlabSyncConfig();
    config.setId(2L);
    when(configService.getConfig()).thenReturn(config);
    when(whitelistService.resolveOptions(config)).thenReturn(List.of());
    when(logService.start(anyLong(), any(), anyList(), anyString())).thenReturn(101L);
    doNothing().when(externalDbService).testConnection(config);

    ReflectionTestUtils.invokeMethod(syncService, "runSync", SyncType.FULL, "Manual full sync");

    verify(configService).updateSyncTime(2L, true);
    verify(configService, never()).updateSyncTime(2L, false);
  }
}
