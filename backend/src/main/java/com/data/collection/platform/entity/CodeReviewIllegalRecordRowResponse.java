package com.data.collection.platform.entity;

import java.time.LocalDateTime;
import java.util.List;

public record CodeReviewIllegalRecordRowResponse(
    String requestType,
    Long mergeRequestId,
    Integer mergeRequestIid,
    Long projectId,
    String mergeRequestContent,
    String mergeRequestLink,
    String owner,
    String projectName,
    String repositoryName,
    LocalDateTime mergedAt,
    String mergedBy,
    String moduleName,
    String targetBranch,
    List<String> illegalTypes,
    Double commentRate,
    Integer defectCount,
    Integer addedLines) {
}
