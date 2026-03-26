package com.data.collection.platform.entity.statistics;

import java.time.LocalDateTime;

public record StatisticBoardMeta(
    LocalDateTime generatedAt,
    long queryDurationMs,
    int rowCount,
    int columnCount,
    int drilldownColumnCount) {
}
