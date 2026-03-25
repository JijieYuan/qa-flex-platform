package com.data.collection.qaflexplatform.web;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {
  @GetMapping("/")
  public Map<String, String> home() {
    return Map.of(
        "message", "QA Flex Platform backend started successfully",
        "status", "ok");
  }
}
