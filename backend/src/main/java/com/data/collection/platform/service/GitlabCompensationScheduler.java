package com.data.collection.platform.service;

import com.data.collection.platform.config.GitlabMirrorProperties;
import com.data.collection.platform.entity.GitlabSyncConfig;
import com.data.collection.platform.entity.GitlabSyncLog;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.stream.Stream;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class GitlabCompensationScheduler {
  private final GitlabMirrorProperties properties;
  private final GitlabConfigService configService;
  private final GitlabMirrorSyncService syncService;
  private final GitlabSyncLogService logService;

  public GitlabCompensationScheduler(
      GitlabMirrorProperties properties,
      GitlabConfigService configService,
      GitlabMirrorSyncService syncService,
      GitlabSyncLogService logService) {
    this.properties = properties;
    this.configService = configService;
    this.syncService = syncService;
    this.logService = logService;
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
    syncService.reconcileRunningState(config.getId());
    LocalDateTime latestActivityAt = resolveLatestActivityAt(config);
    if (latestActivityAt == null) {
      return;
    }
    long minutes = Duration.between(latestActivityAt, LocalDateTime.now()).toMinutes();
    if (minutes >= config.getCompensationIntervalMinutes()) {
      syncService.startCompensationSync();
    }
  }

  private LocalDateTime resolveLatestActivityAt(GitlabSyncConfig config) {
    GitlabSyncLog latestLog = logService.findLatest(config.getId());
    LocalDateTime latestLogTime = latestLog == null
        ? null
        : (latestLog.getFinishedAt() != null ? latestLog.getFinishedAt() : latestLog.getStartedAt());
    return Stream.of(config.getLastIncrementalSyncAt(), config.getLastFullSyncAt(), latestLogTime)
        .filter(value -> value != null)
        .max(LocalDateTime::compareTo)
        .orElse(null);
  }
}
