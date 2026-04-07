package com.data.collection.platform.entity.statistics;

public record StatisticRuleMetricDefinition(
    String key,
    String label,
    String definition,
    String formula,
    String note) {
}
