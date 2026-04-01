package com.data.collection.platform.controller;

import com.data.collection.platform.common.response.ApiResponse;
import com.data.collection.platform.entity.ExternalFormContextRequest;
import com.data.collection.platform.entity.ExternalFormResponse;
import com.data.collection.platform.entity.ExternalFormSaveRequest;
import com.data.collection.platform.service.ExternalFormService;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/external-forms")
public class ExternalFormController {

  private final ExternalFormService externalFormService;

  public ExternalFormController(ExternalFormService externalFormService) {
    this.externalFormService = externalFormService;
  }

  @GetMapping("/detail")
  public ApiResponse<ExternalFormResponse> getDetail(@Valid @ModelAttribute ExternalFormContextRequest request) {
    return ApiResponse.success(externalFormService.getByContext(request));
  }

  @PostMapping("/save")
  public ApiResponse<ExternalFormResponse> save(@Valid @RequestBody ExternalFormSaveRequest request) {
    return ApiResponse.success("保存成功", externalFormService.save(request));
  }

  @PostMapping("/delete")
  public ApiResponse<ExternalFormResponse> delete(@Valid @RequestBody ExternalFormContextRequest request) {
    return ApiResponse.success("删除成功", externalFormService.delete(request));
  }
}
