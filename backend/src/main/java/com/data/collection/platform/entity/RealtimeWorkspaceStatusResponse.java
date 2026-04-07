package com.data.collection.platform.entity;

import java.time.LocalDateTime;

public record RealtimeWorkspaceStatusResponse(
    String workspaceKey,
    boolean supported,
    String status,
    String message,
    boolean refreshing,
    LocalDateTime lastSyncedAt,
    LocalDateTime lastRefreshStartedAt,
    LocalDateTime lastRefreshFinishedAt) {}
