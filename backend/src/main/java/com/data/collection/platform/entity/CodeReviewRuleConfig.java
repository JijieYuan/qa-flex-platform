package com.data.collection.platform.entity;

import java.util.List;

public record CodeReviewRuleConfig(
    boolean enabled,
    List<CodeReviewRuleConfigGroup> groups,
    String updatedAt) {
}
