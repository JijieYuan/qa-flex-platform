package com.data.collection.platform.entity;

import java.time.LocalDateTime;
import java.util.List;

public record MirrorStatusResponse(
    GitlabSyncConfig config,
    SyncStatus currentStatus,
    String currentMessage,
    LocalDateTime currentStartedAt,
    SyncProgress progress,
    List<GitlabSyncLog> logs,
    List<TableWhitelistOption> whitelistOptions,
    String webhookUrl) {
}
