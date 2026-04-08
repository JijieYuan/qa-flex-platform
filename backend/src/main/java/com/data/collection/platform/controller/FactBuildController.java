package com.data.collection.platform.controller;

import com.data.collection.platform.common.response.ApiResponse;
import com.data.collection.platform.entity.FactBuildResponse;
import com.data.collection.platform.service.FactBuildService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/facts")
public class FactBuildController {
  private final FactBuildService factBuildService;

  public FactBuildController(FactBuildService factBuildService) {
    this.factBuildService = factBuildService;
  }

  @PostMapping("/rebuild")
  public ApiResponse<FactBuildResponse> rebuildFacts(
      @RequestParam(defaultValue = "all") String scope,
      @RequestParam(defaultValue = "false") boolean full) {
    FactBuildResponse response = switch (scope) {
      case "issue" -> factBuildService.rebuildIssueFacts(full);
      case "merge-request", "merge_request" -> factBuildService.rebuildMergeRequestFacts(full);
      default -> factBuildService.rebuildAllFacts(full);
    };
    return ApiResponse.success(response.message(), response);
  }
}
