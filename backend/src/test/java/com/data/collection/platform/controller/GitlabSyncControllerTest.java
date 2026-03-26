package com.data.collection.platform.controller;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.data.collection.platform.config.GitlabMirrorProperties;
import com.data.collection.platform.entity.GitlabSyncConfig;
import com.data.collection.platform.entity.SourceMode;
import com.data.collection.platform.entity.SyncProgress;
import com.data.collection.platform.entity.WhitelistMode;
import com.data.collection.platform.service.GitlabConfigService;
import com.data.collection.platform.service.GitlabMirrorSyncService;
import com.data.collection.platform.service.GitlabSyncLogService;
import com.data.collection.platform.service.GitlabWebhookService;
import com.data.collection.platform.service.GitlabWhitelistService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class GitlabSyncControllerTest {

  @Mock
  private GitlabConfigService configService;

  @Mock
  private GitlabMirrorSyncService syncService;

  @Mock
  private GitlabSyncLogService logService;

  @Mock
  private GitlabWhitelistService whitelistService;

  @Mock
  private GitlabWebhookService webhookService;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    GitlabMirrorProperties properties = new GitlabMirrorProperties();
    properties.setWebhookBaseUrl("http://localhost:18080/api/gitlab-sync/webhook");
    GitlabSyncController controller = new GitlabSyncController(
        configService, syncService, logService, whitelistService, properties, webhookService);
    mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
  }

  @Test
  void statusShouldReturnProgressPayloadWhenRunning() throws Exception {
    GitlabSyncConfig config = baseConfig();
    SyncProgress progress = new SyncProgress();
    progress.setPhase("FULL_SYNC");
    progress.setTotalTables(20);
    progress.setCompletedTables(5);
    progress.setSyncedRecords(120);
    progress.setCurrentTable("issues");

    when(configService.getConfig()).thenReturn(config);
    when(syncService.isRunning()).thenReturn(true);
    when(syncService.getProgress()).thenReturn(progress);
    when(logService.listRecent(anyLong(), anyInt())).thenReturn(List.of());
    when(logService.findRunning(anyLong())).thenReturn(null);
    when(whitelistService.listOptions(config)).thenReturn(List.of());

    mockMvc.perform(get("/api/gitlab-sync/status"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.currentStatus").value("RUNNING"))
        .andExpect(jsonPath("$.data.progress.phase").value("FULL_SYNC"))
        .andExpect(jsonPath("$.data.progress.totalTables").value(20))
        .andExpect(jsonPath("$.data.progress.completedTables").value(5))
        .andExpect(jsonPath("$.data.progress.syncedRecords").value(120))
        .andExpect(jsonPath("$.data.progress.currentTable").value("issues"));

    org.mockito.Mockito.verify(syncService).reconcileRunningState(1L);
  }

  @Test
  void statusShouldReturnEmptyProgressWhenIdle() throws Exception {
    GitlabSyncConfig config = baseConfig();

    when(configService.getConfig()).thenReturn(config);
    when(syncService.isRunning()).thenReturn(false);
    when(syncService.getProgress()).thenReturn(null);
    when(logService.listRecent(anyLong(), anyInt())).thenReturn(List.of());
    when(whitelistService.listOptions(config)).thenReturn(List.of());

    mockMvc.perform(get("/api/gitlab-sync/status"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.currentStatus").value("IDLE"))
        .andExpect(jsonPath("$.data.progress").isEmpty());

    org.mockito.Mockito.verify(syncService).reconcileRunningState(1L);
  }

  private GitlabSyncConfig baseConfig() {
    GitlabSyncConfig config = new GitlabSyncConfig();
    config.setId(1L);
    config.setName("GitLab 默认数据源");
    config.setEnabled(true);
    config.setAutoSyncEnabled(true);
    config.setSourceMode(SourceMode.DOCKER);
    config.setWhitelistMode(WhitelistMode.RECOMMENDED);
    return config;
  }
}
