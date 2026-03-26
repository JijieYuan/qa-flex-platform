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
@TableName("gitlab_mirror_records")
public class GitlabMirrorRecord {
  @TableId(type = IdType.AUTO)
  private Long id;

  @TableField("config_id")
  private Long configId;

  @TableField("table_name")
  private String tableName;

  @TableField("record_key")
  private String recordKey;

  @TableField("updated_at_source")
  private LocalDateTime updatedAtSource;

  @TableField("row_data")
  private String rowData;

  @TableField("synced_at")
  private LocalDateTime syncedAt;

  @TableField("created_at")
  private LocalDateTime createdAt;
}
