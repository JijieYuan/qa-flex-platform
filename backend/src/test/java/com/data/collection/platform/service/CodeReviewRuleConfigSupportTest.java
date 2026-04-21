package com.data.collection.platform.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.data.collection.platform.entity.CodeReviewRuleConfig;
import com.data.collection.platform.entity.CodeReviewRuleConfigCondition;
import com.data.collection.platform.entity.CodeReviewRuleConfigGroup;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class CodeReviewRuleConfigSupportTest {

  @Test
  void shouldIgnoreMeaninglessNotEmptyRuleForModuleName() {
    CodeReviewIllegalRecordView row = row("module-a", "owner-a", "master");
    CodeReviewRuleConfig config = config("moduleName", "isNotEmpty", "");

    assertThat(CodeReviewRuleConfigSupport.hasReadyConfig(config)).isFalse();
    assertThat(CodeReviewRuleConfigSupport.matchesRow(row, config)).isTrue();
    assertThat(CodeReviewRuleConfigSupport.apply(List.of(row), config)).containsExactly(row);
  }

  @Test
  void shouldJudgeIllegalWhenTargetBranchIsOutsideAllowedBranches() {
    CodeReviewIllegalRecordView row = row("module-a", "owner-a", "feature/demo");
    CodeReviewRuleConfig config = config("targetBranch", "notIn", "master,dev");

    assertThat(CodeReviewRuleConfigSupport.hasReadyConfig(config)).isTrue();
    assertThat(CodeReviewRuleConfigSupport.apply(List.of(row), config)).containsExactly(row);
    assertThat(CodeReviewRuleConfigSupport.explainRow(row, config).get(0)).contains("目标分支不在允许范围内");
  }

  private CodeReviewRuleConfig config(String fieldKey, String operator, String value) {
    return new CodeReviewRuleConfig(
        true,
        List.of(new CodeReviewRuleConfigGroup(
            "g1",
            "all",
            List.of(new CodeReviewRuleConfigCondition("c1", fieldKey, operator, value)))),
        null);
  }

  private CodeReviewIllegalRecordView row(String moduleName, String owner, String targetBranch) {
    return new CodeReviewIllegalRecordView(
        "merge_request",
        1001L,
        101,
        1L,
        "feat: demo",
        "http://gitlab.example.com/root/demo/-/merge_requests/101",
        owner,
        "Project A",
        "repo-a",
        LocalDateTime.of(2026, 4, 21, 10, 0),
        "Alice",
        moduleName,
        targetBranch,
        List.of(),
        "DONE",
        20,
        "SCANNED",
        0,
        0.8,
        0,
        120);
  }
}
