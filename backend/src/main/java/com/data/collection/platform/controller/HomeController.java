package com.data.collection.platform.controller;

import com.data.collection.platform.common.response.ApiResponse;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {
  @GetMapping("/")
  public ApiResponse<Map<String, String>> home() {
    return ApiResponse.success("服务启动成功", Map.of(
        "message", "Data Collection Platform backend started successfully",
        "status", "ok"));
  }
}
