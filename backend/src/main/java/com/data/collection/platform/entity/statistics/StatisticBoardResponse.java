package com.data.collection.platform.entity.statistics;

import java.util.List;
import java.util.Map;

public record StatisticBoardResponse(
    StatisticBoardDefinition definition,
    Map<String, String> appliedFilters,
    StatisticFilterGroup appliedFilterGroup,
    List<StatisticRowData> rows,
    StatisticBoardMeta meta) {
}
