package com.data.collection.platform.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

public final class IssueFactNormalizationRules {
  private static final int DEFAULT_RESOLVE_SLA_DAYS = 18;
  private static final Pattern DAY_PATTERN = Pattern.compile("(预计解决时间|预计修复时间|预计完成时间)[^0-9]{0,8}(\\d{1,2})");

  private static final Map<String, List<String>> SEVERITY_TOKENS = ordered(
      Map.entry("P1", List.of("一级缺陷", "一级严重")),
      Map.entry("P2", List.of("二级缺陷", "二级严重")),
      Map.entry("P3", List.of("三级缺陷", "三级严重")),
      Map.entry("SUGGESTION", List.of("建议", "需求", "需求如此")));

  private static final Map<String, List<String>> REASON_CATEGORY_TOKENS = ordered(
      Map.entry("需求理解偏差", List.of("新增理解偏差数量", "需求理解有误数量")),
      Map.entry("新增需求", List.of("新增需求数量", "新增需求问题数量")),
      Map.entry("编码逻辑错误", List.of("业务逻辑错误", "编码逻辑错误")),
      Map.entry("环境部署问题", List.of("编译/打包/部署问题", "编译打包问题")),
      Map.entry("算法机制不支持", List.of("机制不支持", "算法/机制不支持")));

  private static final Map<String, List<String>> DELAY_REASON_TOKENS = ordered(
      Map.entry("技术卡点", List.of("技术卡点")),
      Map.entry("方案卡点", List.of("方案卡点")),
      Map.entry("资源卡点", List.of("资源卡点")),
      Map.entry("数据异常", List.of("数据异常")),
      Map.entry("算法问题", List.of("算法问题")),
      Map.entry("机制问题", List.of("机制问题")),
      Map.entry("计算效率", List.of("计算效率")));

  private static final List<String> EXCLUDED_LABELS = List.of("功能屏蔽", "已拒绝", "建议");
  private static final List<String> CLOSED_EXCLUSION_LABELS = List.of("申请否决", "数据异常", "设计如此");
  private static final List<String> FIXED_LABELS = List.of("已修复", "待合并");
  private static final List<String> UNREPRODUCED_LABELS = List.of("未复现");
  private static final List<String> RESPONSE_DELAY_LABELS = List.of("响应已延期");
  private static final List<String> RESOLVE_DELAY_EXEMPT_LABELS =
      List.of("申请延期", "数据异常", "需求如此", "未复现", "已修复");
  private static final List<String> FLOW_OK_LABELS = List.of("待合并", "设计如此", "建议", "需求");
  private static final List<String> APPLY_DELAY_LABELS = List.of("申请延期");
  private static final List<String> SYSTEM_TEST_LABEL_TOKENS = List.of("系统测试");
  private static final List<String> TESTING_PHASE_TOKENS = List.of("系统测试", "联调测试", "冒烟测试", "测试");
  private static final List<String> REGRESSION_TITLE_TOKENS = List.of("回退", "倒退", "退");
  private static final List<String> CRASH_TITLE_TOKENS = List.of("挂机");

  private static final Set<String> NON_MODULE_TOKENS = Set.of(
      "一级缺陷", "一级严重", "二级缺陷", "二级严重", "三级缺陷", "三级严重",
      "建议", "需求", "需求如此", "功能屏蔽", "已拒绝", "申请否决", "数据异常", "设计如此",
      "已修复", "待合并", "未复现", "申请延期", "响应已延期", "系统测试", "联调测试", "冒烟测试",
      "技术卡点", "方案卡点", "资源卡点", "算法问题", "机制问题", "计算效率",
      "新增理解偏差数量", "需求理解有误数量", "新增需求数量", "新增需求问题数量",
      "业务逻辑错误", "编码逻辑错误", "编译/打包/部署问题", "编译打包问题",
      "机制不支持", "算法/机制不支持");

  private IssueFactNormalizationRules() {
  }

  public static String normalizeSeverityLevel(List<String> labels) {
    for (Map.Entry<String, List<String>> entry : SEVERITY_TOKENS.entrySet()) {
      if (containsAnyLabel(labels, entry.getValue())) {
        return entry.getKey();
      }
    }
    return null;
  }

  public static String normalizeSeverityAlias(List<String> labels) {
    for (List<String> aliases : SEVERITY_TOKENS.values()) {
      for (String alias : aliases) {
        if (hasLabel(labels, alias)) {
          return alias;
        }
      }
    }
    return null;
  }

  public static boolean isExcluded(List<String> labels, boolean closed) {
    return exclusionReason(labels, closed) != null;
  }

  public static String exclusionReason(List<String> labels, boolean closed) {
    for (String excluded : EXCLUDED_LABELS) {
      if (hasLabel(labels, excluded)) {
        return excluded;
      }
    }
    if (closed) {
      for (String excluded : CLOSED_EXCLUSION_LABELS) {
        if (hasLabel(labels, excluded)) {
          return excluded + "+Closed";
        }
      }
    }
    return null;
  }

  public static boolean isFixed(List<String> labels, boolean closed) {
    if (containsAnyLabel(labels, FIXED_LABELS)) {
      return true;
    }
    return closed && containsAnyLabel(labels, UNREPRODUCED_LABELS);
  }

  public static String normalizeReasonCategory(List<String> labels, String notesText) {
    for (Map.Entry<String, List<String>> entry : REASON_CATEGORY_TOKENS.entrySet()) {
      if (containsAny(labels, notesText, entry.getValue())) {
        return entry.getKey();
      }
    }
    return null;
  }

  public static boolean hasDelayFlag(List<String> labels, String notesText) {
    return containsAnyLabel(labels, APPLY_DELAY_LABELS) || normalizeDelayReason(labels, notesText) != null;
  }

  public static String normalizeDelayReason(List<String> labels, String notesText) {
    for (Map.Entry<String, List<String>> entry : DELAY_REASON_TOKENS.entrySet()) {
      if (containsAny(labels, notesText, entry.getValue())) {
        return entry.getKey();
      }
    }
    return null;
  }

  public static String inferDelayCause(List<String> labels, String notesText) {
    String delayReason = normalizeDelayReason(labels, notesText);
    if (delayReason != null) {
      return delayReason;
    }
    if (containsAnyLabel(labels, APPLY_DELAY_LABELS)) {
      return "申请延期";
    }
    return null;
  }

  public static String normalizeTestingPhase(List<String> labels) {
    return firstMatchingLabel(labels, TESTING_PHASE_TOKENS);
  }

  public static String normalizeSystemTestLabel(List<String> labels) {
    return firstMatchingLabel(labels, SYSTEM_TEST_LABEL_TOKENS);
  }

  public static List<String> normalizeModuleNames(List<String> labels) {
    Set<String> modules = new LinkedHashSet<>();
    for (String label : labels) {
      String normalized = normalizeLabel(label);
      if (normalized == null) {
        continue;
      }
      if (NON_MODULE_TOKENS.contains(label.trim())) {
        continue;
      }
      if (isKnownCategory(label) || isKnownSeverityAlias(label) || isKnownDelayReason(label) || isTestingPhase(label)) {
        continue;
      }
      modules.add(label.trim());
    }
    return new ArrayList<>(modules);
  }

  public static String normalizePrimaryModuleName(List<String> labels) {
    List<String> modules = normalizeModuleNames(labels);
    return modules.isEmpty() ? null : modules.get(0);
  }

  public static boolean isRegression(List<String> labels, String title) {
    return isLevel1(labels) && containsToken(title, REGRESSION_TITLE_TOKENS);
  }

  public static boolean isCrash(List<String> labels, String title) {
    return isLevel1(labels) && containsToken(title, CRASH_TITLE_TOKENS);
  }

  public static boolean isLevel1Other(List<String> labels, String title) {
    return isLevel1(labels) && !isRegression(labels, title) && !isCrash(labels, title);
  }

  public static boolean isIllegal(List<String> labels, boolean closed, List<String> modules) {
    return illegalReason(labels, closed, modules) != null;
  }

  public static String illegalReason(List<String> labels, boolean closed, List<String> modules) {
    if (normalizeSeverityLevel(labels) == null) {
      return "缺失严重程度";
    }
    if (modules == null || modules.isEmpty()) {
      return "缺失模块";
    }
    if (!closed
        && !containsAnyLabel(labels, FLOW_OK_LABELS)
        && !containsAnyLabel(labels, APPLY_DELAY_LABELS)) {
      return "流程越位";
    }
    return null;
  }

  public static boolean hasResponse(String notesText) {
    return containsToken(notesText, List.of("# 问题调研情况说明"));
  }

  public static boolean isResponseDelayed(List<String> labels, String notesText) {
    return containsAnyLabel(labels, RESPONSE_DELAY_LABELS) || (!hasResponse(notesText) && containsAnyLabel(labels, RESPONSE_DELAY_LABELS));
  }

  public static int resolveSlaDays(String notesText) {
    if (!StringUtils.hasText(notesText)) {
      return DEFAULT_RESOLVE_SLA_DAYS;
    }
    Matcher matcher = DAY_PATTERN.matcher(notesText);
    int candidate = DEFAULT_RESOLVE_SLA_DAYS;
    while (matcher.find()) {
      try {
        int parsed = Integer.parseInt(matcher.group(2));
        if (parsed > 0) {
          candidate = Math.min(candidate, parsed);
        }
      } catch (NumberFormatException ignored) {
        // ignore malformed note text
      }
    }
    return candidate;
  }

  public static LocalDateTime resolveDeadline(LocalDateTime createdAt, int resolveSlaDays) {
    if (createdAt == null) {
      return null;
    }
    return createdAt.plusDays(Math.max(resolveSlaDays, 1));
  }

  public static boolean isResolveDelayed(
      List<String> labels,
      boolean fixed,
      LocalDateTime resolveDeadlineAt,
      LocalDateTime now) {
    if (fixed || resolveDeadlineAt == null || now == null) {
      return false;
    }
    if (containsAnyLabel(labels, RESOLVE_DELAY_EXEMPT_LABELS)) {
      return false;
    }
    return now.isAfter(resolveDeadlineAt);
  }

  public static boolean isLegacy(
      boolean closed,
      LocalDateTime createdAt,
      LocalDateTime phaseStartAt) {
    return !closed && createdAt != null && phaseStartAt != null && createdAt.isBefore(phaseStartAt);
  }

  private static boolean isLevel1(List<String> labels) {
    return "P1".equals(normalizeSeverityLevel(labels));
  }

  private static boolean isKnownSeverityAlias(String label) {
    return containsAny(List.of(label), "", flatten(SEVERITY_TOKENS));
  }

  private static boolean isKnownCategory(String label) {
    return containsAny(List.of(label), "", flatten(REASON_CATEGORY_TOKENS));
  }

  private static boolean isKnownDelayReason(String label) {
    return containsAny(List.of(label), "", flatten(DELAY_REASON_TOKENS));
  }

  private static boolean isTestingPhase(String label) {
    return containsAny(List.of(label), "", TESTING_PHASE_TOKENS);
  }

  private static List<String> flatten(Map<String, List<String>> mapping) {
    List<String> values = new ArrayList<>();
    mapping.values().forEach(values::addAll);
    return values;
  }

  private static String firstMatchingLabel(List<String> labels, List<String> tokens) {
    for (String label : labels) {
      if (containsToken(label, tokens)) {
        return label.trim();
      }
    }
    return null;
  }

  private static boolean containsAny(List<String> labels, String text, List<String> tokens) {
    return containsAnyLabel(labels, tokens) || containsToken(text, tokens);
  }

  private static boolean containsAnyLabel(List<String> labels, List<String> tokens) {
    for (String label : labels) {
      if (containsToken(label, tokens)) {
        return true;
      }
    }
    return false;
  }

  private static boolean hasLabel(List<String> labels, String token) {
    return containsAnyLabel(labels, List.of(token));
  }

  private static boolean containsToken(String text, List<String> tokens) {
    String normalized = normalizeLabel(text);
    if (normalized == null) {
      return false;
    }
    for (String token : tokens) {
      String normalizedToken = normalizeLabel(token);
      if (normalizedToken != null && normalized.contains(normalizedToken)) {
        return true;
      }
    }
    return false;
  }

  private static String normalizeLabel(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    return value.trim().toLowerCase(Locale.ROOT);
  }

  @SafeVarargs
  private static <K, V> Map<K, V> ordered(Map.Entry<K, V>... entries) {
    Map<K, V> result = new LinkedHashMap<>();
    for (Map.Entry<K, V> entry : entries) {
      result.put(entry.getKey(), entry.getValue());
    }
    return result;
  }
}
