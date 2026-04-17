package com.data.collection.platform.controller;

import com.data.collection.platform.common.response.ApiResponse;
import com.data.collection.platform.entity.ReviewDataFilterOptionsResponse;
import com.data.collection.platform.entity.ReviewDataRecordListResponse;
import com.data.collection.platform.service.ReviewDataRecordService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/review-data")
public class ReviewDataController {

  private final ReviewDataRecordService reviewDataRecordService;

  public ReviewDataController(ReviewDataRecordService reviewDataRecordService) {
    this.reviewDataRecordService = reviewDataRecordService;
  }

  @GetMapping("/records")
  public ApiResponse<ReviewDataRecordListResponse> listRecords(
      @RequestParam(required = false) Long projectId,
      @RequestParam(required = false) String projectName,
      @RequestParam(required = false) String repositoryName,
      @RequestParam(required = false) String moduleName,
      @RequestParam(required = false) String reviewer,
      @RequestParam(required = false) String templateCode,
      @RequestParam(required = false) String targetBranch,
      @RequestParam(required = false) String recordStatus,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String mergeRequestIid,
      @RequestParam(required = false) String updatedAtStart,
      @RequestParam(required = false) String updatedAtEnd,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) String sortBy,
      @RequestParam(required = false) String sortOrder) {
    return ApiResponse.success(
        reviewDataRecordService.listRecords(
            projectId,
            projectName,
            repositoryName,
            moduleName,
            reviewer,
            templateCode,
            targetBranch,
            recordStatus,
            keyword,
            mergeRequestIid,
            updatedAtStart,
            updatedAtEnd,
            page,
            size,
            sortBy,
            sortOrder));
  }

  @GetMapping("/records/filter-options")
  public ApiResponse<ReviewDataFilterOptionsResponse> getFilterOptions(
      @RequestParam(required = false) Long projectId) {
    return ApiResponse.success(reviewDataRecordService.getFilterOptions(projectId));
  }
}
