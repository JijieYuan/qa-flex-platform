package com.data.collection.platform.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.data.collection.platform.entity.OptionItemResponse;
import com.data.collection.platform.entity.ReviewDataFilterOptionsResponse;
import com.data.collection.platform.entity.ReviewDataProblemItemResponse;
import com.data.collection.platform.entity.ReviewDataRecordDetailResponse;
import com.data.collection.platform.entity.ReviewDataRecordListResponse;
import com.data.collection.platform.entity.ReviewDataRecordRowResponse;
import com.data.collection.platform.entity.ReviewDataSummaryResponse;
import com.data.collection.platform.service.ReviewDataRecordService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class ReviewDataControllerTest {

  @Mock private ReviewDataRecordService reviewDataRecordService;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(new ReviewDataController(reviewDataRecordService)).build();
  }

  @Test
  void shouldReturnReviewDataRecordList() throws Exception {
    when(reviewDataRecordService.listRecords(
            eq("说明书"),
            eq("CrownCAD"),
            eq("草图"),
            eq("王青"),
            eq("设计说明书评审"),
            eq("已修复"),
            eq("张三"),
            eq(2),
            eq(10),
            eq("updatedAt"),
            eq("desc")))
        .thenReturn(
            new ReviewDataRecordListResponse(
                List.of(
                    new ReviewDataRecordRowResponse(
                        1L,
                        "CrownCAD",
                        "草图功能设计说明书评审",
                        "草图",
                        "设计说明书评审",
                        LocalDate.of(2026, 4, 10),
                        "王青",
                        "张三、李四",
                        24,
                        "设计说明书",
                        "路士坤",
                        "V1.0",
                        5,
                        0.21,
                        LocalDateTime.of(2026, 4, 12, 10, 0),
                        false)),
                1,
                2,
                10,
                "updatedAt",
                "desc",
                new ReviewDataSummaryResponse(1, 5, 24, 5)));

    mockMvc.perform(
            get("/api/review-data/records")
                .param("title", "说明书")
                .param("projectName", "CrownCAD")
                .param("moduleName", "草图")
                .param("reviewOwner", "王青")
                .param("reviewType", "设计说明书评审")
                .param("problemStatus", "已修复")
                .param("reviewExpert", "张三")
                .param("page", "2")
                .param("size", "10")
                .param("sortBy", "updatedAt")
                .param("sortOrder", "desc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.records[0].title").value("草图功能设计说明书评审"))
        .andExpect(jsonPath("$.data.records[0].problemCount").value(5))
        .andExpect(jsonPath("$.data.summary.totalProblemItems").value(5));
  }

  @Test
  void shouldReturnFilterOptions() throws Exception {
    when(reviewDataRecordService.getFilterOptions())
        .thenReturn(
            new ReviewDataFilterOptionsResponse(
                List.of(new OptionItemResponse("CrownCAD", "CrownCAD")),
                List.of(new OptionItemResponse("草图", "草图")),
                List.of(new OptionItemResponse("王青", "王青")),
                List.of(new OptionItemResponse("设计说明书评审", "设计说明书评审")),
                List.of(new OptionItemResponse("张三", "张三")),
                List.of(new OptionItemResponse("已修复", "已修复")),
                List.of(new OptionItemResponse("会议评审", "会议评审")),
                List.of(new OptionItemResponse("文档规范", "文档规范"))));

    mockMvc.perform(get("/api/review-data/records/filter-options"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.projectNames[0].value").value("CrownCAD"))
        .andExpect(jsonPath("$.data.reviewExperts[0].value").value("张三"));
  }

  @Test
  void shouldReturnRecordDetail() throws Exception {
    when(reviewDataRecordService.getRecordDetail(1L))
        .thenReturn(
            new ReviewDataRecordDetailResponse(
                new ReviewDataRecordRowResponse(
                    1L,
                    "CrownCAD",
                    "草图功能设计说明书评审",
                    "草图",
                    "设计说明书评审",
                    LocalDate.of(2026, 4, 10),
                    "王青",
                    "张三、李四",
                    24,
                    "设计说明书",
                    "路士坤",
                    "V1.0",
                    5,
                    0.21,
                    LocalDateTime.of(2026, 4, 12, 10, 0),
                    false),
                List.of("张三", "李四"),
                List.of(
                    new ReviewDataProblemItemResponse(
                        9L,
                        1L,
                        "张三",
                        0.5,
                        "会议评审",
                        "3.3.2",
                        "文档规范",
                        "命名不规范",
                        "统一命名",
                        "路士坤",
                        "",
                        "已修复",
                        LocalDateTime.of(2026, 4, 12, 11, 0)))));

    mockMvc.perform(get("/api/review-data/records/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.record.projectName").value("CrownCAD"))
        .andExpect(jsonPath("$.data.reviewExperts[0]").value("张三"))
        .andExpect(jsonPath("$.data.problemItems[0].problemDescription").value("命名不规范"));
  }

  @Test
  void shouldCreateRecord() throws Exception {
    when(reviewDataRecordService.createRecord(any()))
        .thenReturn(
            new ReviewDataRecordDetailResponse(
                new ReviewDataRecordRowResponse(
                    12L,
                    "CrownCAD",
                    "新建评审",
                    "工具",
                    "设计说明书评审",
                    LocalDate.of(2026, 4, 17),
                    "王青",
                    "张三",
                    18,
                    "设计说明书",
                    "路士坤",
                    "V2.0",
                    0,
                    0.0,
                    LocalDateTime.of(2026, 4, 17, 10, 0),
                    false),
                List.of("张三"),
                List.of()));

    mockMvc.perform(
            post("/api/review-data/records")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "projectName":"CrownCAD",
                      "title":"新建评审",
                      "moduleName":"工具",
                      "reviewType":"设计说明书评审",
                      "reviewDate":"2026-04-17",
                      "reviewOwner":"王青",
                      "reviewExperts":["张三"],
                      "reviewScalePages":18,
                      "reviewProduct":"设计说明书",
                      "authorName":"路士坤",
                      "reviewVersion":"V2.0"
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("新增评审成功"))
        .andExpect(jsonPath("$.data.record.id").value(12));
  }

  @Test
  void shouldUpdateProblemItem() throws Exception {
    when(reviewDataRecordService.updateProblemItem(eq(1L), eq(9L), any()))
        .thenReturn(
            new ReviewDataProblemItemResponse(
                9L,
                1L,
                "张三",
                0.8,
                "会议评审",
                "3.3.2",
                "文档规范",
                "命名不规范",
                "统一命名",
                "路士坤",
                "",
                "已修复",
                LocalDateTime.of(2026, 4, 17, 11, 0)));

    mockMvc.perform(
            put("/api/review-data/records/1/problem-items/9")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "reviewerName":"张三",
                      "workloadHours":0.8,
                      "reviewCategory":"会议评审",
                      "documentPosition":"3.3.2",
                      "problemCategory":"文档规范",
                      "problemDescription":"命名不规范",
                      "suggestedSolution":"统一命名",
                      "ownerName":"路士坤",
                      "rejectionReason":"",
                      "problemStatus":"已修复"
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.workloadHours").value(0.8));
  }

  @Test
  void shouldDeleteProblemItem() throws Exception {
    mockMvc.perform(delete("/api/review-data/records/1/problem-items/9"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("删除评审问题成功"));
  }
}
