package com.data.collection.platform.entity;

import java.util.List;

public record CodeReviewRuleConfigGroup(
    String id,
    String matchMode,
    List<CodeReviewRuleConfigCondition> conditions) {
}
