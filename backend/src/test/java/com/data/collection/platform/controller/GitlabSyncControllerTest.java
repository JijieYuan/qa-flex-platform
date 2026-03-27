package com.data.collection.platform.controller;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.data.collection.platform.config.GitlabMirrorProperties;
import com.data.collection.platform.entity.GitlabSyncConfig;
import com.data.collection.platform.entity.GitlabSyncTask;
import com.data.collection.platform.entity.SourceMode;
import com.data.collection.platform.entity.SyncProgress;
import com.data.collection.platform.entity.SyncStatus;
import com.data.collection.platform.entity.SyncTriggerType;
import com.data.collection.platform.entity.SyncType;
import com.data.collection.platform.entity.WhitelistMode;
import com.data.collection.platform.service.GitlabConfigService;
import com.data.collection.platform.service.GitlabMirrorSyncService;
import com.data.collection.platform.service.GitlabSyncLogService;
import com.data.collection.platform.service.GitlabSyncTaskService;
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

  @Mock
  private GitlabSyncTaskService taskService;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    GitlabMirrorProperties properties = new GitlabMirrorProperties();
    properties.setWebhookBaseUrl("http://localhost:18080/api/gitlab-sync/webhook");
    GitlabSyncController controller = new GitlabSyncController(
        configService, syncService, logService, whitelistService, properties, webhookService, taskService);
    mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
  }

  @Test
  void statusShouldReturnTaskDrivenProgressPayloadWhenRunning() throws Exception {
    GitlabSyncConfig config = baseConfig();
    GitlabSyncTask task = new GitlabSyncTask();
    task.setId(10L);
    task.setStatus(SyncStatus.RUNNING);
    task.setTaskType(SyncType.FULL);
    SyncProgress progress = new SyncProgress();
    progress.setPhase("FULL_SYNC");
    progress.setTotalTables(20);
    progress.setCompletedTables(5);
    progress.setSyncedRecords(120);
    progress.setCurrentTable("issues");

    when(configService.getConfig()).thenReturn(config);
    when(taskService.findDisplayTask(1L)).thenReturn(task);
    when(taskService.extractMessage(task)).thenReturn("Manual full sync");
    when(syncService.getProgress(10L)).thenReturn(progress);
    when(logService.listRecent(anyLong(), anyInt())).thenReturn(List.of());
    when(whitelistService.listOptions(config)).thenReturn(List.of());

    mockMvc.perform(get("/api/gitlab-sync/status"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.currentStatus").value("RUNNING"))
        .andExpect(jsonPath("$.data.currentTask.id").value(10))
        .andExpect(jsonPath("$.data.progress.phase").value("FULL_SYNC"))
        .andExpect(jsonPath("$.data.progress.totalTables").value(20))
        .andExpect(jsonPath("$.data.progress.completedTables").value(5));

    verify(syncService).recoverTimedOutTasks();
  }

  @Test
  void cancelShouldReturnAcceptedWhenActiveTaskExists() throws Exception {
    GitlabSyncConfig config = baseConfig();
    GitlabSyncTask task = new GitlabSyncTask();
    task.setId(11L);
    task.setStatus(SyncStatus.CANCELLING);

    when(configService.getConfig()).thenReturn(config);
    when(syncService.requestCancel(1L)).thenReturn(task);

    mockMvc.perform(post("/api/gitlab-sync/cancel"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.accepted").value(true))
        .andExpect(jsonPath("$.data.taskId").value(11))
        .andExpect(jsonPath("$.data.status").value("CANCELLING"));
  }

  @Test
  void incrementalSyncShouldReturnTaskPayload() throws Exception {
    GitlabSyncTask task = new GitlabSyncTask();
    task.setId(15L);
    task.setStatus(SyncStatus.PENDING);
    when(syncService.startIncrementalSync(SyncTriggerType.MANUAL, "Triggered manually")).thenReturn(task);

    mockMvc.perform(post("/api/gitlab-sync/incremental-sync"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.accepted").value(true))
        .andExpect(jsonPath("$.data.taskId").value(15))
        .andExpect(jsonPath("$.data.status").value("PENDING"));
  }

  private GitlabSyncConfig baseConfig() {
    GitlabSyncConfig config = new GitlabSyncConfig();
    config.setId(1L);
    config.setName("GitLab default source");
    config.setEnabled(true);
    config.setAutoSyncEnabled(true);
    config.setSourceMode(SourceMode.DOCKER);
    config.setWhitelistMode(WhitelistMode.RECOMMENDED);
    return config;
  }
}
