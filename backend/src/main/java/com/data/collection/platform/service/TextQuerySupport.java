package com.data.collection.platform.service;

import java.util.Locale;
import java.util.Objects;
import org.springframework.util.StringUtils;

public final class TextQuerySupport {
  private TextQuerySupport() {}

  public static String normalizeDisplay(String value) {
    String normalized = trimToNull(value);
    return normalized == null ? "" : normalized;
  }

  public static String trimToNull(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }

  public static String normalizeForMatch(String value) {
    String normalized = trimToNull(value);
    return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
  }

  public static boolean equalsNormalized(String left, String right) {
    String normalizedRight = normalizeForMatch(right);
    return normalizedRight == null || Objects.equals(normalizeForMatch(left), normalizedRight);
  }

  public static boolean containsIgnoreCase(String source, String keyword) {
    String normalizedKeyword = normalizeForMatch(keyword);
    return normalizedKeyword == null
        || (source != null && source.toLowerCase(Locale.ROOT).contains(normalizedKeyword));
  }
}
