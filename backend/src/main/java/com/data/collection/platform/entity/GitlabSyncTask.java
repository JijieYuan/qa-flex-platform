package com.data.collection.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@TableName("gitlab_sync_tasks")
public class GitlabSyncTask {
  @TableId(type = IdType.AUTO)
  private Long id;

  @TableField("run_id")
  private String runId;

  @TableField("config_id")
  private Long configId;

  @TableField("task_type")
  private SyncType taskType;

  @TableField("trigger_type")
  private SyncTriggerType triggerType;

  @TableField("source_mode")
  private SourceMode sourceMode;

  @TableField("scope_key")
  private String scopeKey;

  @TableField("dedupe_key")
  private String dedupeKey;

  private SyncStatus status;

  @TableField("cancel_requested")
  private boolean cancelRequested;

  @TableField("pending_resync")
  private boolean pendingResync;

  @TableField("retry_count")
  private Integer retryCount;

  @TableField("cooldown_until")
  private LocalDateTime cooldownUntil;

  @TableField("heartbeat_at")
  private LocalDateTime heartbeatAt;

  @TableField("queued_at")
  private LocalDateTime queuedAt;

  @TableField("run_after")
  private LocalDateTime runAfter;

  @TableField("started_at")
  private LocalDateTime startedAt;

  @TableField("finished_at")
  private LocalDateTime finishedAt;

  @TableField("finished_reason")
  private String finishedReason;

  @TableField("lock_owner")
  private String lockOwner;

  private Integer version;

  @TableField("payload_json")
  private String payloadJson;

  @TableField("created_at")
  private LocalDateTime createdAt;

  @TableField("updated_at")
  private LocalDateTime updatedAt;
}
