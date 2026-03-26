package com.data.collection.platform.entity.statistics;

import java.util.Map;

public record StatisticCellData(
    String columnKey,
    long numericValue,
    String displayValue,
    boolean drilldown,
    String detailViewKey,
    Map<String, String> detailParams) {
}
