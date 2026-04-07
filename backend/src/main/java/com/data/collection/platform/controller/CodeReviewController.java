package com.data.collection.platform.controller;

import com.data.collection.platform.common.response.ApiResponse;
import com.data.collection.platform.entity.CodeReviewIllegalRecordFilterOptionsResponse;
import com.data.collection.platform.entity.CodeReviewIllegalRecordListResponse;
import com.data.collection.platform.service.CodeReviewIllegalRecordService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/code-review")
public class CodeReviewController {

  private final CodeReviewIllegalRecordService codeReviewIllegalRecordService;

  public CodeReviewController(CodeReviewIllegalRecordService codeReviewIllegalRecordService) {
    this.codeReviewIllegalRecordService = codeReviewIllegalRecordService;
  }

  @GetMapping("/illegal-records")
  public ApiResponse<CodeReviewIllegalRecordListResponse> listIllegalRecords(
      @RequestParam(required = false) Long projectId,
      @RequestParam(required = false) String repositoryName,
      @RequestParam(required = false) String mergedAtStart,
      @RequestParam(required = false) String mergedAtEnd,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String projectName,
      @RequestParam(required = false) String requestType,
      @RequestParam(required = false) String targetBranch,
      @RequestParam(required = false) String mergedBy,
      @RequestParam(required = false) String moduleName,
      @RequestParam(required = false) String illegalType,
      @RequestParam(required = false) String mergeRequestIid,
      @RequestParam(required = false) String owner,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) String sortBy,
      @RequestParam(required = false) String sortOrder) {
    return ApiResponse.success(
        codeReviewIllegalRecordService.listRecords(
            projectId,
            repositoryName,
            mergedAtStart,
            mergedAtEnd,
            keyword,
            projectName,
            requestType,
            targetBranch,
            mergedBy,
            moduleName,
            illegalType,
            mergeRequestIid,
            owner,
            page,
            size,
            sortBy,
            sortOrder));
  }

  @GetMapping("/illegal-records/filter-options")
  public ApiResponse<CodeReviewIllegalRecordFilterOptionsResponse> getIllegalRecordFilterOptions(
      @RequestParam(required = false) Long projectId) {
    return ApiResponse.success(codeReviewIllegalRecordService.getFilterOptions(projectId));
  }
}
