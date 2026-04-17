package com.data.collection.platform.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.data.collection.platform.entity.OptionItemResponse;
import com.data.collection.platform.entity.ReviewDataFilterOptionsResponse;
import com.data.collection.platform.entity.ReviewDataRecordListResponse;
import com.data.collection.platform.entity.ReviewDataRecordRowResponse;
import com.data.collection.platform.entity.ReviewDataSummaryResponse;
import com.data.collection.platform.service.ReviewDataRecordService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class ReviewDataControllerTest {

  @Mock
  private ReviewDataRecordService reviewDataRecordService;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(new ReviewDataController(reviewDataRecordService)).build();
  }

  @Test
  void shouldReturnReviewDataRecordList() throws Exception {
    when(reviewDataRecordService.listRecords(
            eq(1001L),
            eq("项目甲"),
            eq("repo/a"),
            eq("模块A"),
            eq("张三"),
            eq("code_review"),
            eq("master"),
            eq("ACTIVE"),
            eq("关键字"),
            eq("88"),
            eq("2026-04-01"),
            eq("2026-04-17"),
            eq(2),
            eq(10),
            eq("updatedAt"),
            eq("desc")))
        .thenReturn(
            new ReviewDataRecordListResponse(
                List.of(
                    new ReviewDataRecordRowResponse(
                        1L,
                        1001L,
                        2001L,
                        88L,
                        "评审记录A",
                        "code_review",
                        "张三",
                        35,
                        18,
                        4,
                        4,
                        4,
                        3,
                        3,
                        "备注A",
                        false,
                        "项目甲",
                        "repo/a",
                        "MR 标题A",
                        "模块A",
                        "master",
                        12.5,
                        2,
                        160,
                        LocalDateTime.of(2026, 4, 10, 9, 0),
                        LocalDateTime.of(2026, 4, 12, 10, 0))),
                1,
                2,
                10,
                "updatedAt",
                "desc",
                new ReviewDataSummaryResponse(1, 1, 0, 35, 18, 12.5)));

    mockMvc.perform(
            get("/api/review-data/records")
                .param("projectId", "1001")
                .param("projectName", "项目甲")
                .param("repositoryName", "repo/a")
                .param("moduleName", "模块A")
                .param("reviewer", "张三")
                .param("templateCode", "code_review")
                .param("targetBranch", "master")
                .param("recordStatus", "ACTIVE")
                .param("keyword", "关键字")
                .param("mergeRequestIid", "88")
                .param("updatedAtStart", "2026-04-01")
                .param("updatedAtEnd", "2026-04-17")
                .param("page", "2")
                .param("size", "10")
                .param("sortBy", "updatedAt")
                .param("sortOrder", "desc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.records[0].formTitle").value("评审记录A"))
        .andExpect(jsonPath("$.data.records[0].reviewDurationMinutes").value(35))
        .andExpect(jsonPath("$.data.summary.totalRecords").value(1))
        .andExpect(jsonPath("$.data.summary.averageTotalScore").value(18.0));
  }

  @Test
  void shouldReturnReviewDataFilterOptions() throws Exception {
    when(reviewDataRecordService.getFilterOptions(1001L))
        .thenReturn(
            new ReviewDataFilterOptionsResponse(
                List.of(new OptionItemResponse("项目甲", "项目甲")),
                List.of(new OptionItemResponse("repo/a", "repo/a")),
                List.of(new OptionItemResponse("模块A", "模块A")),
                List.of(new OptionItemResponse("张三", "张三")),
                List.of(new OptionItemResponse("code_review", "code_review")),
                List.of(new OptionItemResponse("master", "master")),
                List.of(new OptionItemResponse("有效", "ACTIVE"))));

    mockMvc.perform(get("/api/review-data/records/filter-options").param("projectId", "1001"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.projectNames[0].value").value("项目甲"))
        .andExpect(jsonPath("$.data.recordStatuses[0].value").value("ACTIVE"));
  }
}
