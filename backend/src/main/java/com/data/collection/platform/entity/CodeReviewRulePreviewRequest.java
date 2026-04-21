package com.data.collection.platform.entity;

public record CodeReviewRulePreviewRequest(
    Long projectId,
    String repositoryName,
    String mergedAtStart,
    String mergedAtEnd,
    String keyword,
    String projectName,
    String requestType,
    String targetBranch,
    String mergedBy,
    String moduleName,
    String illegalType,
    String mergeRequestIid,
    String owner,
    CodeReviewRuleConfig ruleConfig) {
}
