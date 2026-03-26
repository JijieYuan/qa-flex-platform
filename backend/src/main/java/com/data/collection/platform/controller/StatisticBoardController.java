package com.data.collection.platform.controller;

import com.data.collection.platform.common.response.ApiResponse;
import com.data.collection.platform.entity.statistics.StatisticBoardResponse;
import com.data.collection.platform.entity.statistics.StatisticDetailRequest;
import com.data.collection.platform.entity.statistics.StatisticDetailResponse;
import com.data.collection.platform.service.statistics.StatisticBoardRegistry;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/statistic-boards")
public class StatisticBoardController {
  private final StatisticBoardRegistry registry;

  public StatisticBoardController(StatisticBoardRegistry registry) {
    this.registry = registry;
  }

  @GetMapping("/{boardKey}")
  public ApiResponse<StatisticBoardResponse> getBoard(
      @PathVariable @NotBlank String boardKey,
      @RequestParam Map<String, String> filters) {
    return ApiResponse.success(registry.getRequired(boardKey).loadBoard(filters));
  }

  @GetMapping("/{boardKey}/details")
  public ApiResponse<StatisticDetailResponse> getDetails(
      @PathVariable @NotBlank String boardKey,
      @RequestParam String rowKey,
      @RequestParam String columnKey,
      @RequestParam(defaultValue = "1") @Positive int page,
      @RequestParam(defaultValue = "10") @Positive int size,
      @RequestParam(required = false) String sortField,
      @RequestParam(required = false) String sortOrder,
      @RequestParam Map<String, String> filters) {
    return ApiResponse.success(
        registry.getRequired(boardKey).loadDetail(
            new StatisticDetailRequest(boardKey, rowKey, columnKey, page, size, sortField, sortOrder, filters)));
  }

  @GetMapping("/{boardKey}/export")
  public ResponseEntity<String> exportBoard(
      @PathVariable @NotBlank String boardKey,
      @RequestParam Map<String, String> filters) {
    String csv = registry.getRequired(boardKey).exportBoardCsv(filters);
    return ResponseEntity.ok()
        .contentType(new MediaType("text", "csv"))
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"" + boardKey + ".csv\"")
        .body(csv);
  }
}
