package com.data.collection.platform.entity.statistics;

public record StatisticFilterCondition(
    String fieldKey,
    String operator,
    String value,
    String secondaryValue) {
}
