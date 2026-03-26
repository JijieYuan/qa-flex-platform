package com.data.collection.platform.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.data.collection.platform.entity.statistics.StatisticBoardResponse;
import com.data.collection.platform.entity.statistics.StatisticDetailResponse;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StatisticBoardControllerTest {
  @Autowired
  private StatisticBoardController controller;

  @Test
  void shouldLoadMirrorTableOverviewBoard() {
    StatisticBoardResponse response = controller.getBoard("mirror-table-overview", Map.of()).getData();
    assertThat(response).isNotNull();
    assertThat(response.definition().boardKey()).isEqualTo("mirror-table-overview");
    assertThat(response.definition().columnGroups()).isNotEmpty();
    assertThat(response.rows()).isNotEmpty();
    assertThat(response.meta()).isNotNull();
    assertThat(response.meta().rowCount()).isGreaterThan(0);
    assertThat(response.meta().columnCount()).isGreaterThan(0);
    assertThat(response.definition().filters()).hasSize(1);
    assertThat(response.definition().filters().get(0).key()).isEqualTo("tableName");
    assertThat(response.definition().filters().get(0).type()).isEqualTo("select");
    assertThat(response.definition().filters().get(0).options()).isNotEmpty();
    assertThat(response.definition().columnGroups())
        .allSatisfy(group -> assertThat(group.columns()).isNotEmpty());
    assertThat(
            response.definition().columnGroups().stream()
                .flatMap(group -> group.columns().stream())
                .map(column -> column.metricType()))
        .allMatch(metricType -> metricType != null && !metricType.isBlank());
  }

  @Test
  void shouldFilterBoardBySelectedTableName() {
    StatisticBoardResponse initial = controller.getBoard("mirror-table-overview", Map.of()).getData();
    String selectedTable = initial.definition().filters().get(0).options().get(0).value();

    StatisticBoardResponse filtered =
        controller.getBoard("mirror-table-overview", Map.of("tableName", selectedTable)).getData();

    assertThat(filtered.rows()).hasSize(1);
    assertThat(filtered.rows().get(0).rowKey()).isEqualTo(selectedTable);
    assertThat(filtered.appliedFilters()).containsEntry("tableName", selectedTable);
  }

  @Test
  void shouldLoadDetailsForDrilldownCell() {
    StatisticBoardResponse board = controller.getBoard("mirror-table-overview", Map.of()).getData();
    assertThat(board.rows()).isNotEmpty();

    String rowKey = board.rows().get(0).rowKey();
    StatisticDetailResponse detail =
        controller
            .getDetails(
                "mirror-table-overview",
                rowKey,
                "totalRecords",
                1,
                10,
                "syncedAt",
                "descending",
                Map.of())
            .getData();

    assertThat(detail).isNotNull();
    assertThat(detail.columns()).isNotEmpty();
    assertThat(detail.total()).isGreaterThan(0);
  }

  @Test
  void shouldExportMirrorTableOverviewBoardCsv() {
    ResponseEntity<String> response = controller.exportBoard("mirror-table-overview", Map.of());
    assertThat(response.getBody()).isNotBlank();
    assertThat(response.getBody()).contains("统计对象");
    assertThat(response.getBody()).contains("总记录数");
  }
}
