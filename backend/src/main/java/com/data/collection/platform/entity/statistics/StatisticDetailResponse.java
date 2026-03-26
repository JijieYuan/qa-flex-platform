package com.data.collection.platform.entity.statistics;

import java.util.List;
import java.util.Map;

public record StatisticDetailResponse(
    String title,
    String description,
    List<StatisticDetailColumn> columns,
    List<Map<String, Object>> records,
    long total,
    int page,
    int size,
    String sortField,
    String sortOrder) {
}
