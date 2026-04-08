package com.data.collection.platform.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class IssueFactNormalizationRules {
  private static final Map<String, List<String>> SEVERITY_TOKENS = ordered(
      Map.entry("一级", List.of("一级", "level1", "l1")),
      Map.entry("二级", List.of("二级", "level2", "l2")),
      Map.entry("三级", List.of("三级", "level3", "l3")));
  private static final Map<String, List<String>> PRIORITY_TOKENS = ordered(
      Map.entry("P1", List.of("p1")),
      Map.entry("P2", List.of("p2")),
      Map.entry("P3", List.of("p3")));
  private static final Map<String, List<String>> CATEGORY_TOKENS = ordered(
      Map.entry("回退", List.of("回退", "rollback")),
      Map.entry("挂机", List.of("挂机", "hang")));
  private static final List<String> TESTING_PHASE_TOKENS = List.of("系统测试", "测试");
  private static final List<String> SYSTEM_TEST_LABEL_TOKENS = List.of("系统测试");
  private static final List<String> DELAY_TOKENS = List.of("延期", "delay");

  private IssueFactNormalizationRules() {
  }

  public static String normalizeSeverityLevel(List<String> labels, String title) {
    return normalizeByTokens(SEVERITY_TOKENS, labels, title);
  }

  public static String normalizeUrgency(List<String> labels, String title) {
    return normalizeByTokens(PRIORITY_TOKENS, labels, title);
  }

  public static String normalizeCategory(List<String> labels, String title) {
    return normalizeByTokens(CATEGORY_TOKENS, labels, title);
  }

  public static String normalizeTestingPhase(List<String> labels) {
    return firstMatchingLabel(labels, TESTING_PHASE_TOKENS);
  }

  public static String normalizeSystemTestLabel(List<String> labels) {
    return firstMatchingLabel(labels, SYSTEM_TEST_LABEL_TOKENS);
  }

  public static boolean hasDelayFlag(List<String> labels, String title) {
    return containsAny(labels, title, DELAY_TOKENS);
  }

  public static String inferDelayCause(List<String> labels, String title) {
    return hasDelayFlag(labels, title) ? "标签或标题包含延期信息" : null;
  }

  public static String normalizeModuleName(List<String> labels) {
    for (String label : labels) {
      String normalized = normalizeLabel(label);
      if (normalized == null) {
        continue;
      }
      if (containsAny(List.of(normalized), "", List.of("一级", "二级", "三级", "level1", "level2", "level3", "l1", "l2", "l3"))) {
        continue;
      }
      if (containsAny(List.of(normalized), "", List.of("p1", "p2", "p3", "系统测试", "测试", "延期", "delay"))) {
        continue;
      }
      return label;
    }
    return null;
  }

  private static String normalizeByTokens(Map<String, List<String>> mappings, List<String> labels, String title) {
    for (Map.Entry<String, List<String>> entry : mappings.entrySet()) {
      if (containsAny(labels, title, entry.getValue())) {
        return entry.getKey();
      }
    }
    return null;
  }

  private static String firstMatchingLabel(List<String> labels, List<String> tokens) {
    for (String label : labels) {
      String normalized = normalizeLabel(label);
      if (normalized != null && containsToken(normalized, tokens)) {
        return label;
      }
    }
    return null;
  }

  private static boolean containsAny(List<String> labels, String title, List<String> tokens) {
    String haystack = (title == null ? "" : title).toLowerCase(Locale.ROOT) + "|" + String.join("|", labels);
    return containsToken(haystack, tokens);
  }

  private static boolean containsToken(String haystack, List<String> tokens) {
    String normalizedHaystack = haystack.toLowerCase(Locale.ROOT);
    for (String token : tokens) {
      if (normalizedHaystack.contains(token.toLowerCase(Locale.ROOT))) {
        return true;
      }
    }
    return false;
  }

  private static String normalizeLabel(String label) {
    if (label == null) {
      return null;
    }
    String normalized = label.trim().toLowerCase(Locale.ROOT);
    return normalized.isEmpty() ? null : normalized;
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
