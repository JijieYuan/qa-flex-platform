package com.data.collection.platform.entity.statistics;

import java.util.Map;

public record StatisticDetailRequest(
    String boardKey,
    String rowKey,
    String columnKey,
    int page,
    int size,
    String sortField,
    String sortOrder,
    Map<String, String> filters) {
}
