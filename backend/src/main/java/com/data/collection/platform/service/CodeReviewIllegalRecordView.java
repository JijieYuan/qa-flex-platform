package com.data.collection.platform.service;

import java.time.LocalDateTime;
import java.util.List;

record CodeReviewIllegalRecordView(
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
    String reviewStatus,
    Integer reviewDurationMinutes,
    String scanStatus,
    Integer scanBugCount,
    Double commentRate,
    Integer defectCount,
    Integer addedLines) {
}
