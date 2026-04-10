package com.data.collection.platform.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class CodeReviewIllegalRuleRegistryTest {

  @Test
  void shouldEvaluateIllegalTypesInStableOrder() {
    CodeReviewIllegalRecordSource source =
        new CodeReviewIllegalRecordSource(
            1L,
            101,
            2001L,
            "MR",
            "Project A",
            "repo-a",
            LocalDateTime.of(2026, 4, 10, 9, 0),
            "Alice",
            null,
            "master",
            null,
            List.of(),
            null,
            null,
            "NOT_SCANNED",
            2,
            null,
            null,
            null);

    assertThat(CodeReviewIllegalRuleRegistry.evaluateIllegalTypes(source))
        .containsExactly(
            CodeReviewIllegalRuleRegistry.MISSING_MODULE_LABEL,
            CodeReviewIllegalRuleRegistry.MISSING_OWNER_LABEL,
            CodeReviewIllegalRuleRegistry.MISSING_REVIEW_LABEL,
            CodeReviewIllegalRuleRegistry.NOT_SCANNED_LABEL,
            CodeReviewIllegalRuleRegistry.OPEN_SCAN_ISSUE_LABEL,
            CodeReviewIllegalRuleRegistry.MISSING_COMMENT_RATE_LABEL,
            CodeReviewIllegalRuleRegistry.MISSING_DEFECT_COUNT_LABEL,
            CodeReviewIllegalRuleRegistry.MISSING_ADDED_LINES_LABEL);
  }

  @Test
  void shouldMatchGroupedRulesForExplanation() {
    CodeReviewIllegalRecordView view =
        new CodeReviewIllegalRecordView(
            "merge_request",
            1L,
            101,
            2001L,
            "MR",
            "http://gitlab/repo/-/merge_requests/101",
            "Owner",
            "Project A",
            "repo-a",
            LocalDateTime.of(2026, 4, 10, 9, 0),
            "Alice",
            "module-a",
            "master",
            List.of(
                CodeReviewIllegalRuleRegistry.NOT_SCANNED_LABEL,
                CodeReviewIllegalRuleRegistry.OPEN_SCAN_ISSUE_LABEL),
            "DONE",
            10,
            "NOT_SCANNED",
            2,
            0.5,
            1,
            100);

    CodeReviewIllegalRuleGroup scanGroup =
        CodeReviewIllegalRuleRegistry.explanationGroups().stream()
            .filter(group -> group.key().equals("scan-check"))
            .findFirst()
            .orElseThrow();

    assertThat(CodeReviewIllegalRuleRegistry.countMatches(List.of(view), scanGroup)).isEqualTo(1);
    assertThat(CodeReviewIllegalRuleRegistry.filterMatches(List.of(view), scanGroup))
        .containsExactly(view);
  }
}
