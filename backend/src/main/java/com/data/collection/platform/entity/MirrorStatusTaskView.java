package com.data.collection.platform.entity;

import java.time.LocalDateTime;

public record MirrorStatusTaskView(
    Long id,
    String runId,
    SyncType taskType,
    SyncTriggerType triggerType,
    SourceMode sourceMode,
    SyncStatus status,
    boolean cancelRequested,
    boolean pendingResync,
    Integer retryCount,
    LocalDateTime queuedAt,
    LocalDateTime startedAt,
    LocalDateTime finishedAt,
    String finishedReason) {

  public static MirrorStatusTaskView from(GitlabSyncTask task) {
    return new MirrorStatusTaskView(
        task.getId(),
        task.getRunId(),
        task.getTaskType(),
        task.getTriggerType(),
        task.getSourceMode(),
        task.getStatus(),
        task.isCancelRequested(),
        task.isPendingResync(),
        task.getRetryCount(),
        task.getQueuedAt(),
        task.getStartedAt(),
        task.getFinishedAt(),
        task.getFinishedReason());
  }
}
