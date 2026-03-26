package com.data.collection.platform.entity.statistics;

public record StatisticDetailColumn(
    String key,
    String label,
    Integer width,
    Integer minWidth,
    boolean sortable) {
}
