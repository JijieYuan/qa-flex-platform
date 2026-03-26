package com.data.collection.platform.entity.statistics;

import java.util.List;

public record StatisticColumnGroup(String key, String label, List<StatisticColumnLeaf> columns) {
}
