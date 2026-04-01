package com.data.collection.platform.entity;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExternalFormResponse {
  private boolean found;
  private Long id;
  private String gitlabBaseUrl;
  private Long projectId;
  private Long mrIid;
  private String resourceType;
  private String resourceId;
  private String templateCode;
  private String formTitle;
  private String reviewer;
  private Integer reviewDurationMinutes;
  private Integer specificationScore;
  private Integer logicScore;
  private Integer performanceScore;
  private Integer designScore;
  private Integer otherScore;
  private String remark;
  private Boolean deleted;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
