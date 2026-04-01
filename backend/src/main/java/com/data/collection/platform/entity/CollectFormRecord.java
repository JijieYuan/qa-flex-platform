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
@TableName("collect_form_records")
public class CollectFormRecord {
  @TableId(type = IdType.AUTO)
  private Long id;

  @TableField("gitlab_base_url")
  private String gitlabBaseUrl;

  @TableField("project_id")
  private Long projectId;

  @TableField("request_iid")
  private Long requestIid;

  @TableField("resource_type")
  private String resourceType;

  @TableField("resource_id")
  private String resourceId;

  @TableField("template_code")
  private String templateCode;

  @TableField("form_title")
  private String formTitle;

  private String reviewer;

  @TableField("review_duration_minutes")
  private Integer reviewDurationMinutes;

  @TableField("specification_score")
  private Integer specificationScore;

  @TableField("logic_score")
  private Integer logicScore;

  @TableField("performance_score")
  private Integer performanceScore;

  @TableField("design_score")
  private Integer designScore;

  @TableField("other_score")
  private Integer otherScore;

  private String remark;

  private boolean deleted;

  @TableField("created_at")
  private LocalDateTime createdAt;

  @TableField("updated_at")
  private LocalDateTime updatedAt;
}
