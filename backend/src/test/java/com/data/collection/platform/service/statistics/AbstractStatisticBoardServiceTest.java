package com.data.collection.platform.service.statistics;

import static org.assertj.core.api.Assertions.assertThat;

import com.data.collection.platform.entity.statistics.StatisticBoardDefinition;
import com.data.collection.platform.entity.statistics.StatisticBoardMeta;
import com.data.collection.platform.entity.statistics.StatisticBoardResponse;
import com.data.collection.platform.entity.statistics.StatisticCellData;
import com.data.collection.platform.entity.statistics.StatisticColumnGroup;
import com.data.collection.platform.entity.statistics.StatisticColumnLeaf;
import com.data.collection.platform.entity.statistics.StatisticDetailColumn;
import com.data.collection.platform.entity.statistics.StatisticDetailRequest;
import com.data.collection.platform.entity.statistics.StatisticDetailResponse;
import com.data.collection.platform.entity.statistics.StatisticFilterField;
import com.data.collection.platform.entity.statistics.StatisticRowData;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AbstractStatisticBoardServiceTest {

  @Test
  void shouldExportBoardAsCsv() {
    AbstractStatisticBoardService service =
        new AbstractStatisticBoardService() {
          @Override
          public String boardKey() {
            return "demo";
          }

          @Override
          protected StatisticBoardDefinition buildDefinition() {
            return new StatisticBoardDefinition(
                "demo",
                "演示统计",
                "演示统计描述",
                "查询与操作",
                "演示查询描述",
                List.<StatisticFilterField>of(),
                List.of(
                    new StatisticColumnGroup(
                        "g1",
                        "分组",
                        List.of(new StatisticColumnLeaf("c1", "总数", true, "count")))),
                List.<StatisticDetailColumn>of(),
                10,
                "无数据");
          }

          @Override
          protected StatisticBoardResponse doLoadBoard(Map<String, String> filters) {
            return new StatisticBoardResponse(
                buildDefinition(),
                filters,
                List.of(
                    new StatisticRowData(
                        "issues",
                        "issues",
                        List.of(new StatisticCellData("c1", 12, "12", true, "detail", Map.of())))),
                new StatisticBoardMeta(LocalDateTime.now(), 10L, 1, 1, 1));
          }

          @Override
          protected StatisticDetailResponse doLoadDetail(StatisticDetailRequest request) {
            throw new UnsupportedOperationException();
          }
        };

    String csv = service.exportBoardCsv(Map.of());

    assertThat(csv).contains("\"统计对象\"");
    assertThat(csv).contains("\"总数\"");
    assertThat(csv).contains("\"issues\"");
    assertThat(csv).contains("\"12\"");
  }
}
