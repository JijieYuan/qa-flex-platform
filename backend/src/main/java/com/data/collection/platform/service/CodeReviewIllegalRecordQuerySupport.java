package com.data.collection.platform.service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

final class CodeReviewIllegalRecordQuerySupport {
  private CodeReviewIllegalRecordQuerySupport() {
  }

  static Map<String, String> buildFactFilters(
      Long projectId,
      String repositoryName,
      String mergedAtStart,
      String mergedAtEnd,
      String projectName,
      String targetBranch,
      String moduleName,
      String mergeRequestIid,
      String owner) {
    Map<String, String> filters = new LinkedHashMap<>();
    if (projectId != null) {
      filters.put("projectId", String.valueOf(projectId));
    }
    putIfPresent(filters, "repositoryName", repositoryName);
    putIfPresent(filters, "mergedAtStart", mergedAtStart);
    putIfPresent(filters, "mergedAtEnd", mergedAtEnd);
    putIfPresent(filters, "projectName", projectName);
    putIfPresent(filters, "targetBranch", targetBranch);
    putIfPresent(filters, "moduleName", moduleName);
    putIfPresent(filters, "mergeRequestIid", mergeRequestIid);
    putIfPresent(filters, "owner", owner);
    return filters;
  }

  static String normalizeSortField(String sortField) {
    String normalized = TextQuerySupport.trimToNull(sortField);
    return switch (normalized == null ? "mergedAt" : normalized) {
      case "mergeRequestIid",
           "mergeRequestContent",
           "owner",
           "projectName",
           "mergedAt",
           "mergedBy",
           "moduleName",
           "targetBranch",
           "commentRate",
           "defectCount",
           "addedLines" -> normalized == null ? "mergedAt" : normalized;
      default -> "mergedAt";
    };
  }

  static String normalizeSortOrder(String sortOrder) {
    String normalized = TextQuerySupport.trimToNull(sortOrder);
    return "asc".equalsIgnoreCase(normalized) ? "asc" : "desc";
  }

  static Comparator<CodeReviewIllegalRecordView> buildComparator(String sortField, String sortOrder) {
    Comparator<CodeReviewIllegalRecordView> comparator = switch (sortField) {
      case "mergeRequestIid" -> SortSupport.nullableComparable(CodeReviewIllegalRecordView::mergeRequestIid);
      case "mergeRequestContent" -> SortSupport.nullableString(CodeReviewIllegalRecordView::mergeRequestContent);
      case "owner" -> SortSupport.nullableString(CodeReviewIllegalRecordView::owner);
      case "projectName" -> SortSupport.nullableString(CodeReviewIllegalRecordView::projectName);
      case "mergedBy" -> SortSupport.nullableString(CodeReviewIllegalRecordView::mergedBy);
      case "moduleName" -> SortSupport.nullableString(CodeReviewIllegalRecordView::moduleName);
      case "targetBranch" -> SortSupport.nullableString(CodeReviewIllegalRecordView::targetBranch);
      case "commentRate" -> SortSupport.nullableComparable(CodeReviewIllegalRecordView::commentRate);
      case "defectCount" -> SortSupport.nullableComparable(CodeReviewIllegalRecordView::defectCount);
      case "addedLines" -> SortSupport.nullableComparable(CodeReviewIllegalRecordView::addedLines);
      default -> SortSupport.nullableComparable(CodeReviewIllegalRecordView::mergedAt);
    };
    Comparator<CodeReviewIllegalRecordView> tieBreaker = SortSupport.nullableComparable(CodeReviewIllegalRecordView::mergedAt)
        .thenComparing(SortSupport.nullableComparable(CodeReviewIllegalRecordView::mergeRequestIid));
    Comparator<CodeReviewIllegalRecordView> combined = comparator.thenComparing(tieBreaker);
    return SortSupport.applyDirection(combined, "asc".equalsIgnoreCase(sortOrder));
  }

  static boolean matchesEquals(String left, String right) {
    return TextQuerySupport.equalsNormalized(left, right);
  }

  static boolean matchesRequestType(String requestType, String expected) {
    String normalizedExpected = TextQuerySupport.normalizeForMatch(expected);
    return normalizedExpected == null || Objects.equals(requestType, normalizedExpected);
  }

  static boolean matchesIllegalType(List<String> illegalTypes, String expected) {
    String normalizedExpected = TextQuerySupport.trimToNull(expected);
    return normalizedExpected == null || illegalTypes.contains(normalizedExpected);
  }

  static boolean matchesKeyword(CodeReviewIllegalRecordView row, String keyword) {
    return TextQuerySupport.containsIgnoreCase(row.mergeRequestContent(), keyword)
        || TextQuerySupport.containsIgnoreCase(row.owner(), keyword)
        || TextQuerySupport.containsIgnoreCase(row.projectName(), keyword)
        || TextQuerySupport.containsIgnoreCase(row.repositoryName(), keyword)
        || TextQuerySupport.containsIgnoreCase(row.moduleName(), keyword)
        || TextQuerySupport.containsIgnoreCase(row.targetBranch(), keyword)
        || TextQuerySupport.containsIgnoreCase(row.mergedBy(), keyword);
  }

  private static void putIfPresent(Map<String, String> filters, String key, String value) {
    String normalized = TextQuerySupport.trimToNull(value);
    if (normalized != null) {
      filters.put(key, normalized);
    }
  }
}
