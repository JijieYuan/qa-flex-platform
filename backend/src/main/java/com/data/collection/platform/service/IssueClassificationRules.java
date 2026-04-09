package com.data.collection.platform.service;

import java.util.List;
import java.util.Map;

final class IssueClassificationRules {
  static final Map<String, List<String>> REASON_CATEGORY_TOKENS = IssueRuleSupport.ordered(
      Map.entry("需求理解偏差", List.of("新增理解偏差数量", "需求理解有误数量")),
      Map.entry("新增需求", List.of("新增需求数量", "新增需求问题数量")),
      Map.entry("编码逻辑错误", List.of("业务逻辑错误", "编码逻辑错误")),
      Map.entry("环境部署问题", List.of("编译/打包/部署问题", "编译打包问题")),
      Map.entry("算法机制不支持", List.of("机制不支持", "算法/机制不支持")));
  static final Map<String, List<String>> DELAY_REASON_TOKENS = IssueRuleSupport.ordered(
      Map.entry("技术卡点", List.of("技术卡点")),
      Map.entry("方案卡点", List.of("方案卡点")),
      Map.entry("资源卡点", List.of("资源卡点")),
      Map.entry("数据异常", List.of("数据异常")),
      Map.entry("算法问题", List.of("算法问题")),
      Map.entry("机制问题", List.of("机制问题")),
      Map.entry("计算效率", List.of("计算效率")));
  private static final List<String> REGRESSION_TITLE_TOKENS = List.of("回退", "倒退", "退");
  private static final List<String> CRASH_TITLE_TOKENS = List.of("挂机");
  private static final List<String> FLOW_OK_LABELS = List.of("待合并", "设计如此", "建议", "需求");
  private static final List<String> APPLY_DELAY_LABELS = List.of("申请延期");

  private IssueClassificationRules() {
  }

  static String normalizeReasonCategory(List<String> labels, String notesText) {
    for (Map.Entry<String, List<String>> entry : REASON_CATEGORY_TOKENS.entrySet()) {
      if (IssueRuleSupport.containsAny(labels, notesText, entry.getValue())) {
        return entry.getKey();
      }
    }
    return null;
  }

  static boolean hasDelayFlag(List<String> labels, String notesText) {
    return IssueRuleSupport.containsAnyLabel(labels, APPLY_DELAY_LABELS)
        || normalizeDelayReason(labels, notesText) != null;
  }

  static String normalizeDelayReason(List<String> labels, String notesText) {
    for (Map.Entry<String, List<String>> entry : DELAY_REASON_TOKENS.entrySet()) {
      if (IssueRuleSupport.containsAny(labels, notesText, entry.getValue())) {
        return entry.getKey();
      }
    }
    return null;
  }

  static String inferDelayCause(List<String> labels, String notesText) {
    String delayReason = normalizeDelayReason(labels, notesText);
    if (delayReason != null) {
      return delayReason;
    }
    if (IssueRuleSupport.containsAnyLabel(labels, APPLY_DELAY_LABELS)) {
      return "申请延期";
    }
    return null;
  }

  static boolean isRegression(List<String> labels, String title) {
    return isLevel1(labels) && IssueRuleSupport.containsToken(title, REGRESSION_TITLE_TOKENS);
  }

  static boolean isCrash(List<String> labels, String title) {
    return isLevel1(labels) && IssueRuleSupport.containsToken(title, CRASH_TITLE_TOKENS);
  }

  static boolean isLevel1Other(List<String> labels, String title) {
    return isLevel1(labels) && !isRegression(labels, title) && !isCrash(labels, title);
  }

  static boolean isIllegal(List<String> labels, boolean closed, List<String> modules) {
    return illegalReason(labels, closed, modules) != null;
  }

  static String illegalReason(List<String> labels, boolean closed, List<String> modules) {
    if (IssueLabelRules.normalizeSeverityLevel(labels) == null) {
      return "缺失严重程度";
    }
    if (modules == null || modules.isEmpty()) {
      return "缺失模块";
    }
    if (!closed
        && !IssueRuleSupport.containsAnyLabel(labels, FLOW_OK_LABELS)
        && !IssueRuleSupport.containsAnyLabel(labels, APPLY_DELAY_LABELS)) {
      return "流程越位";
    }
    return null;
  }

  private static boolean isLevel1(List<String> labels) {
    return "LEVEL1".equals(IssueLabelRules.normalizeSeverityLevel(labels));
  }
}
