package com.data.collection.platform.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.data.collection.platform.entity.ReviewDataRecordRowResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class ReviewDataSearchSupportTest {

  @Test
  void shouldMatchKeywordAndContainsWithAbstractSearch() {
    ReviewDataRecordRowResponse row =
        new ReviewDataRecordRowResponse(
            1L,
            "草图项目",
            "[草图模块] 算数功能设计说明书评审",
            "草图模块",
            "文档评审",
            LocalDate.of(2026, 4, 20),
            "王强",
            "王强, 李四",
            12,
            "设计说明书",
            "赵六",
            "V1.0",
            3,
            0.25,
            LocalDateTime.of(2026, 4, 20, 10, 0),
            false);

    assertThat(ReviewDataSearchSupport.matchesKeyword(row, "ct")).isTrue();
    assertThat(ReviewDataSearchSupport.matchesKeyword(row, "wq")).isTrue();
    assertThat(ReviewDataSearchSupport.matchesKeyword(row, "wangqiang")).isTrue();
    assertThat(ReviewDataSearchSupport.matchesContains(row.title(), "ct")).isTrue();
    assertThat(ReviewDataSearchSupport.matchesContains(row.reviewOwner(), "wq")).isTrue();
    assertThat(ReviewDataSearchSupport.matchesContains(row.moduleName(), "fx")).isFalse();
  }
}
