package com.data.collection.platform.entity;

import java.util.List;

public record CodeReviewRulePreviewResponse(
    long baseTotal,
    long filteredTotal,
    long deltaCount,
    double retainedRate,
    List<CodeReviewRulePreviewSample> samples) {
}
