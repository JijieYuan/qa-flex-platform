package com.data.collection.platform.entity.statistics;

import java.util.List;

public record StatisticRowData(
    String rowKey,
    String rowLabel,
    List<StatisticCellData> cells) {
}
