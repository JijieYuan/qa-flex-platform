package com.data.collection.platform.controller;

import com.data.collection.platform.common.response.ApiResponse;
import com.data.collection.platform.config.GitlabMirrorProperties;
import com.data.collection.platform.entity.GitlabSyncConfig;
import com.data.collection.platform.entity.GitlabSyncLog;
import com.data.collection.platform.entity.GitlabSyncTask;
import com.data.collection.platform.entity.MirrorStatusResponse;
import com.data.collection.platform.entity.SourceMode;
import com.data.collection.platform.entity.SyncProgress;
import com.data.collection.platform.entity.SyncStatus;
import com.data.collection.platform.entity.SyncTriggerType;
import com.data.collection.platform.entity.WhitelistMode;
import com.data.collection.platform.service.GitlabConfigService;
import com.data.collection.platform.service.GitlabMirrorSyncService;
import com.data.collection.platform.service.GitlabSyncLogService;
import com.data.collection.platform.service.GitlabSyncTaskService;
import com.data.collection.platform.service.GitlabWebhookService;
import com.data.collection.platform.service.GitlabWhitelistService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/gitlab-sync")
public class GitlabSyncController {
  private final GitlabConfigService configService;
  private final GitlabMirrorSyncService syncService;
  private final GitlabSyncLogService logService;
  private final GitlabWhitelistService whitelistService;
  private final GitlabMirrorProperties properties;
  private final GitlabWebhookService webhookService;
  private final GitlabSyncTaskService taskService;

  public GitlabSyncController(
      GitlabConfigService configService,
      GitlabMirrorSyncService syncService,
      GitlabSyncLogService logService,
      GitlabWhitelistService whitelistService,
      GitlabMirrorProperties properties,
      GitlabWebhookService webhookService,
      GitlabSyncTaskService taskService) {
    this.configService = configService;
    this.syncService = syncService;
    this.logService = logService;
    this.whitelistService = whitelistService;
    this.properties = properties;
    this.webhookService = webhookService;
    this.taskService = taskService;
  }

  @GetMapping("/status")
  public ApiResponse<MirrorStatusResponse> status() {
    GitlabSyncConfig config = configService.getConfig();
    syncService.recoverTimedOutTasks();
    List<GitlabSyncLog> logs = config.getId() == null ? List.of() : logService.listRecent(config.getId(), 20);
    GitlabSyncTask currentTask = taskService.findDisplayTask(config.getId());
    SyncProgress progress = currentTask == null ? null : syncService.getProgress(currentTask.getId());
    SyncStatus currentStatus = currentTask == null ? SyncStatus.IDLE : currentTask.getStatus();
    String currentMessage = currentTask == null ? "" : taskService.extractMessage(currentTask);
    java.time.LocalDateTime currentStartedAt = currentTask == null ? null : currentTask.getStartedAt();
    return ApiResponse.success(new MirrorStatusResponse(
        config,
        currentTask,
        currentStatus,
        currentMessage,
        currentStartedAt,
        progress,
        logs,
        whitelistService.listOptions(config),
        properties.getWebhookBaseUrl()));
  }

  @PutMapping("/config")
  public ApiResponse<GitlabSyncConfig> saveConfig(@RequestBody SaveConfigRequest request) {
    GitlabSyncConfig config = new GitlabSyncConfig();
    config.setName(request.name());
    config.setEnabled(request.enabled());
    config.setAutoSyncEnabled(request.autoSyncEnabled());
    config.setSourceMode(request.sourceMode());
    config.setWhitelistMode(request.whitelistMode());
    config.setWhitelistTables(request.whitelistTables());
    config.setDbHost(request.dbHost());
    config.setDbPort(request.dbPort());
    config.setDbName(request.dbName());
    config.setDbUsername(request.dbUsername());
    config.setDbPassword(request.dbPassword());
    config.setDockerContainerName(request.dockerContainerName());
    config.setWebhookSecret(request.webhookSecret());
    config.setWebhookProjectId(request.webhookProjectId());
    config.setCompensationIntervalMinutes(request.compensationIntervalMinutes());
    return ApiResponse.success("配置已保存", configService.saveConfig(config));
  }

  @PostMapping("/test-connection")
  public ApiResponse<Map<String, Object>> testConnection() {
    syncService.testConnection();
    return ApiResponse.success("GitLab PostgreSQL connection succeeded", Map.of("checked", true));
  }

  @PostMapping("/full-sync")
  public ApiResponse<Map<String, Object>> fullSync() {
    GitlabSyncTask task = syncService.startFullSync();
    return ApiResponse.success("Full sync accepted", Map.of("accepted", true, "taskId", task.getId(), "status", task.getStatus()));
  }

  @PostMapping("/incremental-sync")
  public ApiResponse<Map<String, Object>> incrementalSync() {
    GitlabSyncTask task = syncService.startIncrementalSync(SyncTriggerType.MANUAL, "Triggered manually");
    return ApiResponse.success("Incremental sync accepted", Map.of("accepted", true, "taskId", task.getId(), "status", task.getStatus()));
  }

  @PostMapping("/cancel")
  public ApiResponse<Map<String, Object>> cancel() {
    GitlabSyncConfig config = configService.getConfig();
    GitlabSyncTask task = syncService.requestCancel(config.getId());
    if (task == null) {
      return ApiResponse.success("No active task to cancel", Map.of("accepted", false));
    }
    return ApiResponse.success("Cancellation requested", Map.of("accepted", true, "taskId", task.getId(), "status", task.getStatus()));
  }

  @PostMapping("/webhook")
  public ApiResponse<Map<String, Object>> webhook(
      @RequestHeader(value = "X-Gitlab-Event", required = false) String eventType,
      @RequestHeader(value = "X-Gitlab-Token", required = false) String secret,
      @RequestBody Map<String, Object> payload) {
    webhookService.accept(eventType, payload, secret);
    return ApiResponse.success("Webhook accepted", Map.of("accepted", true));
  }

  public record SaveConfigRequest(
      @NotBlank String name,
      boolean enabled,
      boolean autoSyncEnabled,
      @NotNull SourceMode sourceMode,
      @NotNull WhitelistMode whitelistMode,
      List<String> whitelistTables,
      String dbHost,
      Integer dbPort,
      String dbName,
      String dbUsername,
      String dbPassword,
      String dockerContainerName,
      String webhookSecret,
      Long webhookProjectId,
      @NotNull Integer compensationIntervalMinutes) {
  }
}
