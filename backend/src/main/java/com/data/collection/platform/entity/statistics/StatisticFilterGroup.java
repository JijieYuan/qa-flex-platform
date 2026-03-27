package com.data.collection.platform.entity.statistics;

import java.util.List;

public record StatisticFilterGroup(
    String logic,
    List<StatisticFilterCondition> conditions) {
}
