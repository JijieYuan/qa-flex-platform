package com.data.collection.platform.service.statistics;

import com.data.collection.platform.common.JsonUtils;
import com.data.collection.platform.entity.statistics.StatisticBoardDefinition;
import com.data.collection.platform.entity.statistics.StatisticBoardMeta;
import com.data.collection.platform.entity.statistics.StatisticBoardResponse;
import com.data.collection.platform.entity.statistics.StatisticColumnGroup;
import com.data.collection.platform.entity.statistics.StatisticColumnLeaf;
import com.data.collection.platform.entity.statistics.StatisticDetailColumn;
import com.data.collection.platform.entity.statistics.StatisticDetailRequest;
import com.data.collection.platform.entity.statistics.StatisticDetailResponse;
import com.data.collection.platform.entity.statistics.StatisticFilterGroup;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class SystemTestDefectSummaryBoardService extends AbstractStatisticBoardService {
  private static final String BOARD_KEY = "system-test-defect-summary";

  public SystemTestDefectSummaryBoardService(JsonUtils jsonUtils) {
    super(jsonUtils);
  }

  @Override
  public String boardKey() {
    return BOARD_KEY;
  }

  @Override
  protected StatisticBoardDefinition buildDefinition() {
    return new StatisticBoardDefinition(
        BOARD_KEY,
        "系统测试缺陷汇总",
        "",
        "",
        "",
        "模块名称",
        List.of(),
        List.of(
            new StatisticColumnGroup(
                "level1",
                "一级缺陷",
                List.of(
                    new StatisticColumnLeaf("level1_back", "回退数量", false, "count"),
                    new StatisticColumnLeaf("level1_hang", "挂机数量", false, "count"),
                    new StatisticColumnLeaf("level1_others", "其他数量", false, "count"),
                    new StatisticColumnLeaf("level1_fixed", "已修复数量", false, "count"),
                    new StatisticColumnLeaf("level1_rate", "修复率%", false, "ratio"))),
            new StatisticColumnGroup(
                "level2",
                "二级缺陷",
                List.of(
                    new StatisticColumnLeaf("level2_total", "总数量", false, "count"),
                    new StatisticColumnLeaf("level2_fixed", "已修复数量", false, "count"),
                    new StatisticColumnLeaf("level2_rate", "修复率%", false, "ratio"))),
            new StatisticColumnGroup(
                "level3",
                "三级缺陷",
                List.of(
                    new StatisticColumnLeaf("level3_total", "总数量", false, "count"),
                    new StatisticColumnLeaf("level3_fixed", "已修复数量", false, "count"),
                    new StatisticColumnLeaf("level3_rate", "修复率%", false, "ratio"))),
            new StatisticColumnGroup(
                "priority",
                "优先级统计",
                List.of(
                    new StatisticColumnLeaf("p1_count", "P1 数量", false, "count"),
                    new StatisticColumnLeaf("p1_rate", "P1 修复率%", false, "ratio"),
                    new StatisticColumnLeaf("p2_count", "P2 数量", false, "count"),
                    new StatisticColumnLeaf("p2_rate", "P2 修复率%", false, "ratio"),
                    new StatisticColumnLeaf("p3_count", "P3 数量", false, "count"),
                    new StatisticColumnLeaf("p3_rate", "P3 修复率%", false, "ratio"))),
            new StatisticColumnGroup(
                "summary",
                "综合汇总",
                List.of(
                    new StatisticColumnLeaf("totalDefects", "模块总缺陷数", false, "count"),
                    new StatisticColumnLeaf("defectRatio", "模块缺陷占比", false, "ratio"),
                    new StatisticColumnLeaf("closeRate", "缺陷关闭率", false, "ratio"),
                    new StatisticColumnLeaf("unclosedCount", "未关闭缺陷数", false, "count")))),
        List.<StatisticDetailColumn>of(),
        10,
        "当前暂无系统测试缺陷数据。");
  }

  @Override
  protected StatisticBoardResponse doLoadBoard(Map<String, String> filters, StatisticFilterGroup filterGroup) {
    StatisticBoardDefinition definition = buildDefinition();
    int columnCount = definition.columnGroups().stream().mapToInt(group -> group.columns().size()).sum();
    return new StatisticBoardResponse(
        definition,
        Map.of(),
        filterGroup,
        List.of(),
        new StatisticBoardMeta(LocalDateTime.now(), 0L, 0, columnCount, 0));
  }

  @Override
  protected StatisticDetailResponse doLoadDetail(StatisticDetailRequest request, StatisticFilterGroup filterGroup) {
    return new StatisticDetailResponse(
        "系统测试缺陷汇总",
        "当前统计表未配置明细下钻。",
        List.of(),
        List.of(),
        0,
        request.page() <= 0 ? 1 : request.page(),
        request.size() <= 0 ? 10 : request.size(),
        request.sortField() == null ? "" : request.sortField(),
        "ascending".equalsIgnoreCase(request.sortOrder()) ? "ascending" : "descending");
  }
}
