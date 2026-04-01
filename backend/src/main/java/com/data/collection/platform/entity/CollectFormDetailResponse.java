package com.data.collection.platform.entity;

import java.time.LocalDateTime;

public record CollectFormDetailResponse(
    Long id,
    String gitlabBaseUrl,
    Long projectId,
    Long mrIid,
    String resourceType,
    String resourceId,
    String templateCode,
    String formTitle,
    String reviewer,
    Integer reviewDurationMinutes,
    Integer specificationScore,
    Integer logicScore,
    Integer performanceScore,
    Integer designScore,
    Integer otherScore,
    String remark,
    boolean deleted,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {

  public static CollectFormDetailResponse from(CollectFormRecord record) {
    return new CollectFormDetailResponse(
        record.getId(),
        record.getGitlabBaseUrl(),
        record.getProjectId(),
        record.getMrIid(),
        record.getResourceType(),
        record.getResourceId(),
        record.getTemplateCode(),
        record.getFormTitle(),
        record.getReviewer(),
        record.getReviewDurationMinutes(),
        record.getSpecificationScore(),
        record.getLogicScore(),
        record.getPerformanceScore(),
        record.getDesignScore(),
        record.getOtherScore(),
        record.getRemark(),
        record.isDeleted(),
        record.getCreatedAt(),
        record.getUpdatedAt());
  }
}
