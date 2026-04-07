package com.data.collection.platform.entity.statistics;

import java.util.List;

public record StatisticBoardRuleExplanationResponse(
    String boardKey,
    boolean supported,
    String title,
    String version,
    String scopeDescription,
    String summary,
    List<StatisticRuleFlowStep> flowSteps,
    List<StatisticRuleMetricDefinition> metricDefinitions,
    String unsupportedReason) {
}
