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
@TableName("gitlab_mirror_table_registry")
public class GitlabMirrorTableRegistry {
  @TableId(type = IdType.AUTO)
  private Long id;

  @TableField("config_id")
  private Long configId;

  @TableField("source_table_name")
  private String sourceTableName;

  @TableField("mirror_table_name")
  private String mirrorTableName;

  @TableField("primary_key_columns")
  private String primaryKeyColumns;

  @TableField("updated_at_column")
  private String updatedAtColumn;

  @TableField("column_snapshot")
  private String columnSnapshot;

  @TableField("last_schema_synced_at")
  private LocalDateTime lastSchemaSyncedAt;

  @TableField("created_at")
  private LocalDateTime createdAt;

  @TableField("updated_at")
  private LocalDateTime updatedAt;
}
