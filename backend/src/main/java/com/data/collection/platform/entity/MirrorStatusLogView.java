package com.data.collection.platform.entity;

import java.time.LocalDateTime;

public record MirrorStatusLogView(
    Long id,
    SyncType syncType,
    SyncStatus status,
    String message,
    Integer tableCount,
    Integer recordCount,
    LocalDateTime startedAt,
    LocalDateTime finishedAt) {

  public static MirrorStatusLogView from(GitlabSyncLog log) {
    return new MirrorStatusLogView(
        log.getId(),
        log.getSyncType(),
        log.getStatus(),
        log.getMessage(),
        log.getTableCount(),
        log.getRecordCount(),
        log.getStartedAt(),
        log.getFinishedAt());
  }
}
