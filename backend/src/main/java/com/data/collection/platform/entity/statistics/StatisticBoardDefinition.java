package com.data.collection.platform.entity.statistics;

import java.util.List;

public record StatisticBoardDefinition(
    String boardKey,
    String title,
    String description,
    String queryTitle,
    String queryDescription,
    String rowHeaderLabel,
    List<StatisticFilterField> filters,
    List<StatisticColumnGroup> columnGroups,
    List<StatisticDetailColumn> detailColumns,
    Integer defaultPageSize,
    String emptyText) {
}
