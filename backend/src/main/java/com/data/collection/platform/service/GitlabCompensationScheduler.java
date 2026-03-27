package com.data.collection.platform.service;

import com.data.collection.platform.config.GitlabMirrorProperties;
import com.data.collection.platform.entity.GitlabSyncConfig;
import java.time.Duration;
import java.time.LocalDateTime;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class GitlabCompensationScheduler {
  private final GitlabMirrorProperties properties;
  private final GitlabConfigService configService;
  private final GitlabMirrorSyncService syncService;
  private final GitlabSyncTaskService taskService;

  public GitlabCompensationScheduler(
      GitlabMirrorProperties properties,
      GitlabConfigService configService,
      GitlabMirrorSyncService syncService,
      GitlabSyncTaskService taskService) {
    this.properties = properties;
    this.configService = configService;
    this.syncService = syncService;
    this.taskService = taskService;
  }

  @Scheduled(fixedDelayString = "${platform.gitlab-mirror.scheduler-delay-ms:60000}")
  public void run() {
    if (!properties.isSchedulerEnabled()) {
      return;
    }
    syncService.recoverTimedOutTasks();
    GitlabSyncConfig config = configService.getConfig();
    if (config.getId() == null || !config.isEnabled() || !config.isAutoSyncEnabled()) {
      return;
    }
    if (syncService.hasActiveTask(config.getId()) || taskService.isInCooldown(config.getId())) {
      return;
    }
    LocalDateTime latestActivityAt = taskService.resolveLatestActivityAt(config.getId());
    if (latestActivityAt == null) {
      return;
    }
    long minutes = Duration.between(latestActivityAt, LocalDateTime.now()).toMinutes();
    if (minutes >= config.getCompensationIntervalMinutes()) {
      syncService.startCompensationSync();
    }
  }
}
