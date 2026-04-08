package com.data.collection.platform.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

final class IssueSlaRules {
  private static final int DEFAULT_RESOLVE_SLA_DAYS = 18;
  private static final Pattern DAY_PATTERN =
      Pattern.compile("(预计解决时间|预计修复时间|预计完成时间)[^0-9]{0,8}(\\d{1,2})");
  private static final List<String> RESPONSE_DELAY_LABELS = List.of("响应已延期");
  private static final List<String> RESOLVE_DELAY_EXEMPT_LABELS =
      List.of("申请延期", "数据异常", "需求如此", "未复现", "已修复");
  private static final List<String> RESPONSE_HEADER_TOKENS = List.of("# 问题调研情况说明");

  private IssueSlaRules() {
  }

  static boolean hasResponse(String notesText) {
    return IssueRuleSupport.containsToken(notesText, RESPONSE_HEADER_TOKENS);
  }

  static boolean isResponseDelayed(List<String> labels, String notesText) {
    return IssueRuleSupport.containsAnyLabel(labels, RESPONSE_DELAY_LABELS)
        || (!hasResponse(notesText) && IssueRuleSupport.containsAnyLabel(labels, RESPONSE_DELAY_LABELS));
  }

  static int resolveSlaDays(String notesText) {
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
      }
    }
    return candidate;
  }

  static LocalDateTime resolveDeadline(LocalDateTime createdAt, int resolveSlaDays) {
    if (createdAt == null) {
      return null;
    }
    return createdAt.plusDays(Math.max(resolveSlaDays, 1));
  }

  static boolean isResolveDelayed(
      List<String> labels, boolean fixed, LocalDateTime resolveDeadlineAt, LocalDateTime now) {
    if (fixed || resolveDeadlineAt == null || now == null) {
      return false;
    }
    if (IssueRuleSupport.containsAnyLabel(labels, RESOLVE_DELAY_EXEMPT_LABELS)) {
      return false;
    }
    return now.isAfter(resolveDeadlineAt);
  }
}
