package com.data.collection.platform.entity;

public record CodeReviewRuleConfigCondition(
    String id,
    String fieldKey,
    String operator,
    String value) {
}
