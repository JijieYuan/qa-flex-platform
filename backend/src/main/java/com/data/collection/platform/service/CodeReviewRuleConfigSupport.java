package com.data.collection.platform.service;

import com.data.collection.platform.entity.CodeReviewRuleConfig;
import com.data.collection.platform.entity.CodeReviewRuleConfigCondition;
import com.data.collection.platform.entity.CodeReviewRuleConfigGroup;
import java.util.Arrays;
import java.util.List;
import org.springframework.util.StringUtils;

final class CodeReviewRuleConfigSupport {
  private CodeReviewRuleConfigSupport() {
  }

  static boolean hasReadyConfig(CodeReviewRuleConfig config) {
    return config != null
        && config.enabled()
        && config.groups() != null
        && config.groups().stream().anyMatch(CodeReviewRuleConfigSupport::hasReadyGroup);
  }

  static List<CodeReviewIllegalRecordView> apply(
      List<CodeReviewIllegalRecordView> rows, CodeReviewRuleConfig config) {
    if (!hasReadyConfig(config)) {
      return rows;
    }
    return rows.stream().filter(row -> matchesRow(row, config)).toList();
  }

  static boolean matchesRow(CodeReviewIllegalRecordView row, CodeReviewRuleConfig config) {
    if (!hasReadyConfig(config)) {
      return true;
    }
    return config.groups().stream()
        .filter(CodeReviewRuleConfigSupport::hasReadyGroup)
        .anyMatch(group -> matchesGroup(row, group));
  }

  static List<String> explainRow(CodeReviewIllegalRecordView row, CodeReviewRuleConfig config) {
    if (!hasReadyConfig(config)) {
      return List.of("当前没有启用我的判定规则");
    }
    List<String> reasons =
        config.groups().stream()
            .filter(CodeReviewRuleConfigSupport::hasReadyGroup)
            .filter(group -> matchesGroup(row, group))
            .map(CodeReviewRuleConfigSupport::describeGroup)
            .toList();
    return reasons.isEmpty() ? List.of("满足我的判定规则") : reasons;
  }

  private static boolean hasReadyGroup(CodeReviewRuleConfigGroup group) {
    return group != null
        && group.conditions() != null
        && group.conditions().stream().anyMatch(CodeReviewRuleConfigSupport::isReadyCondition);
  }

  private static boolean matchesGroup(CodeReviewIllegalRecordView row, CodeReviewRuleConfigGroup group) {
    List<CodeReviewRuleConfigCondition> readyConditions =
        group.conditions().stream().filter(CodeReviewRuleConfigSupport::isReadyCondition).toList();
    if (readyConditions.isEmpty()) {
      return false;
    }
    boolean matchAny = "any".equalsIgnoreCase(TextQuerySupport.trimToNull(group.matchMode()));
    return matchAny
        ? readyConditions.stream().anyMatch(condition -> matchesCondition(row, condition))
        : readyConditions.stream().allMatch(condition -> matchesCondition(row, condition));
  }

  private static boolean isReadyCondition(CodeReviewRuleConfigCondition condition) {
    if (condition == null || !StringUtils.hasText(condition.fieldKey()) || !StringUtils.hasText(condition.operator())) {
      return false;
    }
    if (!isAllowedCondition(condition)) {
      return false;
    }
    return usesConditionValue(condition) ? StringUtils.hasText(condition.value()) : true;
  }

  private static boolean isAllowedCondition(CodeReviewRuleConfigCondition condition) {
    String fieldKey = condition.fieldKey();
    String operator = condition.operator();
    return switch (fieldKey) {
      case "moduleName", "owner", "commentRateMissing", "defectCountMissing", "addedLinesMissing" ->
          "isEmpty".equals(operator);
      case "targetBranch" -> "notIn".equals(operator);
      case "mergeRequestContent" -> "contains".equals(operator);
      case "commentRateLow" -> "lt".equals(operator);
      case "defectCountHigh", "addedLinesHigh" -> "gt".equals(operator);
      default -> false;
    };
  }

  private static boolean usesConditionValue(CodeReviewRuleConfigCondition condition) {
    return switch (condition.operator()) {
      case "isEmpty" -> false;
      default -> true;
    };
  }

  private static boolean matchesCondition(CodeReviewIllegalRecordView row, CodeReviewRuleConfigCondition condition) {
    return switch (condition.fieldKey()) {
      case "moduleName" -> !StringUtils.hasText(row.moduleName());
      case "owner" -> !StringUtils.hasText(row.owner());
      case "targetBranch" -> matchesNotIn(row.targetBranch(), condition.value());
      case "mergeRequestContent" -> containsAny(row.mergeRequestContent(), condition.value());
      case "commentRateMissing" -> row.commentRate() == null;
      case "commentRateLow" -> matchesNumber(row.commentRate(), condition.value(), NumberRule.LESS_THAN);
      case "defectCountMissing" -> row.defectCount() == null;
      case "defectCountHigh" -> matchesNumber(toDouble(row.defectCount()), condition.value(), NumberRule.GREATER_THAN);
      case "addedLinesMissing" -> row.addedLines() == null;
      case "addedLinesHigh" -> matchesNumber(toDouble(row.addedLines()), condition.value(), NumberRule.GREATER_THAN);
      default -> false;
    };
  }

  private static boolean matchesNotIn(String value, String allowedValuesText) {
    String normalizedValue = TextQuerySupport.trimToNull(value);
    if (normalizedValue == null) {
      return true;
    }
    List<String> allowedValues = splitValues(allowedValuesText);
    return !allowedValues.isEmpty()
        && allowedValues.stream().noneMatch(allowed -> TextQuerySupport.equalsNormalized(allowed, normalizedValue));
  }

  private static boolean containsAny(String value, String keywordsText) {
    String normalizedValue = TextQuerySupport.trimToNull(value);
    if (normalizedValue == null) {
      return false;
    }
    return splitValues(keywordsText).stream()
        .anyMatch(keyword -> TextQuerySupport.containsAbstractSearch(normalizedValue, keyword));
  }

  private static boolean matchesNumber(Double value, String expectedText, NumberRule rule) {
    if (value == null) {
      return false;
    }
    Double expected = parseNumber(expectedText);
    if (expected == null) {
      return false;
    }
    return rule == NumberRule.LESS_THAN ? value < expected : value > expected;
  }

  private static Double parseNumber(String value) {
    try {
      return Double.parseDouble(value.trim());
    } catch (Exception error) {
      return null;
    }
  }

  private static Double toDouble(Integer value) {
    return value == null ? null : value.doubleValue();
  }

  private static List<String> splitValues(String value) {
    return Arrays.stream(String.valueOf(value == null ? "" : value).split("[,，、\\n]"))
        .map(String::trim)
        .filter(StringUtils::hasText)
        .toList();
  }

  private static String describeGroup(CodeReviewRuleConfigGroup group) {
    String joiner = "any".equalsIgnoreCase(TextQuerySupport.trimToNull(group.matchMode())) ? "，或者" : "，并且";
    String description =
        group.conditions().stream()
            .filter(CodeReviewRuleConfigSupport::isReadyCondition)
            .map(CodeReviewRuleConfigSupport::describeCondition)
            .reduce((left, right) -> left + joiner + right)
            .orElse("当前规则还没有填写完整");
    return "满足：" + description;
  }

  private static String describeCondition(CodeReviewRuleConfigCondition condition) {
    return switch (condition.fieldKey()) {
      case "moduleName" -> "模块名为空";
      case "owner" -> "责任人为空";
      case "targetBranch" -> "目标分支不在允许范围内：" + condition.value();
      case "mergeRequestContent" -> "合并内容包含风险词：" + condition.value();
      case "commentRateMissing" -> "注释率缺失";
      case "commentRateLow" -> "注释率低于 " + condition.value();
      case "defectCountMissing" -> "缺陷数缺失";
      case "defectCountHigh" -> "缺陷数高于 " + condition.value();
      case "addedLinesMissing" -> "新增代码行数缺失";
      case "addedLinesHigh" -> "新增代码行数高于 " + condition.value();
      default -> "满足我的规则";
    };
  }

  private enum NumberRule {
    LESS_THAN,
    GREATER_THAN
  }
}
