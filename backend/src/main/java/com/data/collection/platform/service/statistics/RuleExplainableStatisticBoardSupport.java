package com.data.collection.platform.service.statistics;

import com.data.collection.platform.entity.statistics.StatisticBoardRuleExplanationResponse;
import java.util.Map;

public interface RuleExplainableStatisticBoardSupport {
  StatisticBoardRuleExplanationResponse getRuleExplanation(Map<String, String> filters);
}
