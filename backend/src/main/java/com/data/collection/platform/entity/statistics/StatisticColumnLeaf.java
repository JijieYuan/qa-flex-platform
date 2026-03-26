package com.data.collection.platform.entity.statistics;

public record StatisticColumnLeaf(
    String key,
    String label,
    boolean drilldown,
    String metricType) {
}
