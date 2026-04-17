package com.data.collection.platform.entity;

import java.time.LocalDateTime;

public record ReviewDataRecordRowResponse(
    Long id,
    Long projectId,
    Long mergeRequestId,
    Long mergeRequestIid,
    String formTitle,
    String templateCode,
    String reviewer,
    Integer reviewDurationMinutes,
    Integer totalScore,
    Integer specificationScore,
    Integer logicScore,
    Integer performanceScore,
    Integer designScore,
    Integer otherScore,
    String remark,
    boolean deleted,
    String projectName,
    String repositoryName,
    String mergeRequestTitle,
    String moduleName,
    String targetBranch,
    Double commentRate,
    Integer defectCount,
    Integer addedLines,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {
}
