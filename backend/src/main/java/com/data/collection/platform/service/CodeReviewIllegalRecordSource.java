package com.data.collection.platform.service;

import java.time.LocalDateTime;
import java.util.List;

record CodeReviewIllegalRecordSource(
    Long mergeRequestId,
    Integer mergeRequestIid,
    Long projectId,
    String mergeRequestContent,
    String projectName,
    String repositoryName,
    LocalDateTime mergedAt,
    String mergedBy,
    String owner,
    String targetBranch,
    String moduleName,
    List<String> labelTitles,
    String reviewStatus,
    Integer reviewDurationMinutes,
    String scanStatus,
    Integer scanBugCount,
    Double commentRate,
    Integer defectCount,
    Integer addedLines) {
}
