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

  public GitlabCompensationScheduler(
      GitlabMirrorProperties properties,
      GitlabConfigService configService,
      GitlabMirrorSyncService syncService) {
    this.properties = properties;
    this.configService = configService;
    this.syncService = syncService;
  }

  @Scheduled(fixedDelayString = "${platform.gitlab-mirror.scheduler-delay-ms:60000}")
  public void run() {
    if (!properties.isSchedulerEnabled() || syncService.isRunning()) {
      return;
    }
    GitlabSyncConfig config = configService.getConfig();
    if (config.getId() == null || !config.isEnabled() || !config.isAutoSyncEnabled()) {
      return;
    }
    if (config.getLastIncrementalSyncAt() == null) {
      return;
    }
    long minutes = Duration.between(config.getLastIncrementalSyncAt(), LocalDateTime.now()).toMinutes();
    if (minutes >= config.getCompensationIntervalMinutes()) {
      syncService.startCompensationSync();
    }
  }
}
