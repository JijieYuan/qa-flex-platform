package com.data.collection.platform.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.util.StringUtils;

final class IssueRuleSupport {
  private IssueRuleSupport() {
  }

  static boolean containsAny(List<String> labels, String text, List<String> tokens) {
    return containsAnyLabel(labels, tokens) || containsToken(text, tokens);
  }

  static boolean containsAnyLabel(List<String> labels, List<String> tokens) {
    for (String label : labels) {
      if (containsToken(label, tokens)) {
        return true;
      }
    }
    return false;
  }

  static boolean hasLabel(List<String> labels, String token) {
    return containsAnyLabel(labels, List.of(token));
  }

  static boolean containsToken(String text, List<String> tokens) {
    String normalized = normalizeText(text);
    if (normalized == null) {
      return false;
    }
    for (String token : tokens) {
      String normalizedToken = normalizeText(token);
      if (normalizedToken != null && normalized.contains(normalizedToken)) {
        return true;
      }
    }
    return false;
  }

  static String firstMatchingLabel(List<String> labels, List<String> tokens) {
    for (String label : labels) {
      if (containsToken(label, tokens)) {
        return label.trim();
      }
    }
    return null;
  }

  static String normalizeText(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    return value.trim().toLowerCase(Locale.ROOT);
  }

  static List<String> flatten(Map<String, List<String>> mapping) {
    List<String> values = new ArrayList<>();
    mapping.values().forEach(values::addAll);
    return values;
  }

  @SafeVarargs
  static <K, V> Map<K, V> ordered(Map.Entry<K, V>... entries) {
    Map<K, V> result = new LinkedHashMap<>();
    for (Map.Entry<K, V> entry : entries) {
      result.put(entry.getKey(), entry.getValue());
    }
    return result;
  }
}
