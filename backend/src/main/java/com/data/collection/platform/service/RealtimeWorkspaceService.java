package com.data.collection.platform.service;

import com.data.collection.platform.entity.GitlabSyncConfig;
import com.data.collection.platform.entity.RealtimeWorkspaceStatusResponse;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RealtimeWorkspaceService {

  private final GitlabConfigService configService;
  private final RealtimeWorkspaceService self;
  private final Map<String, WorkspaceRefreshState> states = new ConcurrentHashMap<>();

  public RealtimeWorkspaceService(
      GitlabConfigService configService,
      @Lazy RealtimeWorkspaceService self) {
    this.configService = configService;
    this.self = self;
  }

  public RealtimeWorkspaceStatusResponse getStatus(String workspaceKey) {
    WorkspaceRefreshState state = states.get(workspaceKey);
    LocalDateTime lastSyncedAt = resolveLastSyncedAt();
    if (state == null) {
      return new RealtimeWorkspaceStatusResponse(
          workspaceKey,
          true,
          lastSyncedAt == null ? "IDLE" : "READY",
          lastSyncedAt == null ? "尚未完成镜像同步" : "当前展示最近一次成功同步结果",
          false,
          lastSyncedAt,
          null,
          null);
    }
    return toResponse(workspaceKey, state, lastSyncedAt);
  }

  public synchronized RealtimeWorkspaceStatusResponse requestRefresh(
      String workspaceKey,
      Runnable refreshAction) {
    WorkspaceRefreshState state = states.computeIfAbsent(workspaceKey, key -> new WorkspaceRefreshState());
    if (state.refreshing) {
      return toResponse(workspaceKey, state, resolveLastSyncedAt());
    }
    LocalDateTime now = LocalDateTime.now();
    state.refreshing = true;
    state.status = "REFRESHING";
    state.message = "正在刷新最新数据";
    state.lastRefreshStartedAt = now;
    self.executeRefreshAsync(workspaceKey, refreshAction);
    return toResponse(workspaceKey, state, resolveLastSyncedAt());
  }

  @Async
  public void executeRefreshAsync(String workspaceKey, Runnable refreshAction) {
    try {
      refreshAction.run();
      synchronized (this) {
        WorkspaceRefreshState state = states.computeIfAbsent(workspaceKey, key -> new WorkspaceRefreshState());
        state.refreshing = false;
        state.status = "READY";
        state.message = "已刷新为最新数据";
        state.lastRefreshFinishedAt = LocalDateTime.now();
      }
    } catch (Exception ex) {
      log.warn("Realtime workspace refresh failed, workspaceKey={}", workspaceKey, ex);
      synchronized (this) {
        WorkspaceRefreshState state = states.computeIfAbsent(workspaceKey, key -> new WorkspaceRefreshState());
        state.refreshing = false;
        state.status = "FAILED";
        state.message = "刷新失败，当前展示最近一次成功同步结果";
        state.lastRefreshFinishedAt = LocalDateTime.now();
      }
    }
  }

  private RealtimeWorkspaceStatusResponse toResponse(
      String workspaceKey,
      WorkspaceRefreshState state,
      LocalDateTime lastSyncedAt) {
    return new RealtimeWorkspaceStatusResponse(
        workspaceKey,
        true,
        state.status,
        state.message,
        state.refreshing,
        lastSyncedAt,
        state.lastRefreshStartedAt,
        state.lastRefreshFinishedAt);
  }

  private LocalDateTime resolveLastSyncedAt() {
    GitlabSyncConfig config = configService.getConfig();
    LocalDateTime incremental = config.getLastIncrementalSyncAt();
    LocalDateTime full = config.getLastFullSyncAt();
    if (incremental == null) {
      return full;
    }
    if (full == null) {
      return incremental;
    }
    return incremental.isAfter(full) ? incremental : full;
  }

  private static final class WorkspaceRefreshState {
    private boolean refreshing;
    private String status = "IDLE";
    private String message = "尚未触发刷新";
    private LocalDateTime lastRefreshStartedAt;
    private LocalDateTime lastRefreshFinishedAt;
  }
}
