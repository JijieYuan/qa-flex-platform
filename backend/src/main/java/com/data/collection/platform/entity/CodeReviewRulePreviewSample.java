package com.data.collection.platform.entity;

import java.util.List;

public record CodeReviewRulePreviewSample(
    Long mergeRequestId,
    Integer mergeRequestIid,
    String projectName,
    String moduleName,
    String owner,
    String targetBranch,
    String mergeRequestContent,
    List<String> reasons) {
}
