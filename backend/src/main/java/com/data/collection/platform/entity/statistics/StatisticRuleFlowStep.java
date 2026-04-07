package com.data.collection.platform.entity.statistics;

import java.util.List;

public record StatisticRuleFlowStep(
    String key,
    String title,
    String description,
    long inputCount,
    long outputCount,
    List<StatisticRuleFlowStepSample> samples) {
}
