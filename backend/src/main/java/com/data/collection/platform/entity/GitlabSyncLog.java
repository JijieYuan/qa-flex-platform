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
@TableName("gitlab_sync_logs")
public class GitlabSyncLog {
  @TableId(type = IdType.AUTO)
  private Long id;

  @TableField("config_id")
  private Long configId;

  @TableField("sync_type")
  private SyncType syncType;

  private SyncStatus status;
  private String message;

  @TableField("whitelist_snapshot")
  private String whitelistSnapshot;

  @TableField("table_count")
  private Integer tableCount;

  @TableField("record_count")
  private Integer recordCount;

  @TableField("started_at")
  private LocalDateTime startedAt;

  @TableField("finished_at")
  private LocalDateTime finishedAt;
}
