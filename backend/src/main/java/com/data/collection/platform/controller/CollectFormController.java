package com.data.collection.platform.controller;

import com.data.collection.platform.common.response.ApiResponse;
import com.data.collection.platform.entity.CollectFormDetailResponse;
import com.data.collection.platform.entity.CollectFormNotificationPayloadResponse;
import com.data.collection.platform.service.CollectFormService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/collect-forms")
public class CollectFormController {

  private final CollectFormService collectFormService;

  public CollectFormController(CollectFormService collectFormService) {
    this.collectFormService = collectFormService;
  }

  @GetMapping("/detail")
  public ApiResponse<CollectFormDetailResponse> detail(
      @RequestParam @NotBlank String gitlabBaseUrl,
      @RequestParam @NotNull @Positive Long projectId,
      @RequestParam @NotBlank String resourceType,
      @RequestParam @NotBlank String resourceId,
      @RequestParam @NotBlank String templateCode) {
    return ApiResponse.success(
        collectFormService.getDetail(gitlabBaseUrl, projectId, resourceType, resourceId, templateCode));
  }

  @GetMapping("/notification-payload")
  public ApiResponse<CollectFormNotificationPayloadResponse> notificationPayload(
      @RequestParam @NotBlank String gitlabBaseUrl,
      @RequestParam @NotNull @Positive Long projectId,
      @RequestParam @NotNull @Positive Long requestIid,
      @RequestParam @NotBlank String resourceType) {
    return ApiResponse.success(
        collectFormService.buildNotificationPayload(gitlabBaseUrl, projectId, requestIid, resourceType));
  }

  @PostMapping("/save")
  public ApiResponse<CollectFormDetailResponse> save(@RequestBody SaveRequest request) {
    return ApiResponse.success(
        "表单已保存到平台正式数据表",
        collectFormService.save(
            request.gitlabBaseUrl(),
            request.projectId(),
            request.requestIid(),
            request.resourceType(),
            request.resourceId(),
            request.templateCode(),
            request.formTitle(),
            request.reviewer(),
            request.reviewDurationMinutes(),
            request.specificationScore(),
            request.logicScore(),
            request.performanceScore(),
            request.designScore(),
            request.otherScore(),
            request.remark()));
  }

  @PostMapping("/delete")
  public ApiResponse<Boolean> delete(@RequestBody DeleteRequest request) {
    return ApiResponse.success(
        "表单记录已作废",
        collectFormService.delete(
            request.gitlabBaseUrl(),
            request.projectId(),
            request.resourceType(),
            request.resourceId(),
            request.templateCode()));
  }

  public record SaveRequest(
      @NotBlank String gitlabBaseUrl,
      @NotNull @Positive Long projectId,
      Long requestIid,
      @NotBlank String resourceType,
      @NotBlank String resourceId,
      @NotBlank String templateCode,
      String formTitle,
      String reviewer,
      Integer reviewDurationMinutes,
      Integer specificationScore,
      Integer logicScore,
      Integer performanceScore,
      Integer designScore,
      Integer otherScore,
      String remark) {}

  public record DeleteRequest(
      @NotBlank String gitlabBaseUrl,
      @NotNull @Positive Long projectId,
      @NotBlank String resourceType,
      @NotBlank String resourceId,
      @NotBlank String templateCode) {}
}
