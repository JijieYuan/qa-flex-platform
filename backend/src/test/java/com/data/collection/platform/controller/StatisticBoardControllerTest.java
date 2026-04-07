package com.data.collection.platform.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.data.collection.platform.entity.statistics.StatisticBoardResponse;
import com.data.collection.platform.entity.statistics.StatisticDetailResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StatisticBoardControllerTest {
  private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  @Autowired
  private StatisticBoardController controller;

  @Test
  void shouldLoadMirrorTableOverviewBoard() {
    StatisticBoardResponse response = controller.getBoard("mirror-table-overview", Map.of()).getData();

    assertThat(response).isNotNull();
    assertThat(response.definition().boardKey()).isEqualTo("mirror-table-overview");
    assertThat(response.definition().rowHeaderLabel()).isEqualTo("统计对象");
    assertThat(response.definition().columnGroups()).isNotEmpty();
    assertThat(response.meta()).isNotNull();
    assertThat(response.meta().rowCount()).isEqualTo(response.rows().size());
    assertThat(response.definition().filters()).extracting("key")
        .contains("tableName", "totalRecords", "lastSyncedAt");
    assertThat(response.definition().filters())
        .anySatisfy(field -> {
          assertThat(field.key()).isEqualTo("tableName");
          assertThat(field.type()).isEqualTo("select");
          assertThat(field.operators()).contains("eq", "ne");
        })
        .anySatisfy(field -> {
          assertThat(field.key()).isEqualTo("totalRecords");
          assertThat(field.type()).isEqualTo("number");
          assertThat(field.operators()).contains("gt", "between");
        })
        .anySatisfy(field -> {
          assertThat(field.key()).isEqualTo("lastSyncedAt");
          assertThat(field.type()).isEqualTo("datetime");
          assertThat(field.operators()).contains("before", "after", "between");
        });
  }

  @Test
  void shouldLoadSystemTestDefectSummaryBoard() {
    StatisticBoardResponse response = controller.getBoard("system-test-defect-summary", Map.of()).getData();

    assertThat(response).isNotNull();
    assertThat(response.definition().boardKey()).isEqualTo("system-test-defect-summary");
    assertThat(response.definition().rowHeaderLabel()).isEqualTo("所属项目");
    assertThat(response.definition().columnGroups()).extracting("key")
        .containsExactly("level1", "level2", "level3", "priority", "summary");
    assertThat(response.definition().columnGroups())
        .anySatisfy(group -> {
          assertThat(group.key()).isEqualTo("level1");
          assertThat(group.columns()).extracting("key")
              .containsExactly("level1_back", "level1_hang", "level1_others", "level1_fixed", "level1_rate");
        })
        .anySatisfy(group -> {
          assertThat(group.key()).isEqualTo("priority");
          assertThat(group.columns()).extracting("key")
              .containsExactly("p1_count", "p1_rate", "p2_count", "p2_rate", "p3_count", "p3_rate");
        })
        .anySatisfy(group -> {
          assertThat(group.key()).isEqualTo("summary");
          assertThat(group.columns()).extracting("key")
              .containsExactly("totalDefects", "defectRatio", "closeRate", "unclosedCount");
        });
    assertThat(response.rows()).allSatisfy(row -> assertThat(row.rowLabel()).isNotBlank());
    assertThat(response.meta().rowCount()).isEqualTo(response.rows().size());
    assertThat(response.meta().columnCount()).isEqualTo(21);
  }

  @Test
  void shouldFilterBoardBySelectedTableName() {
    StatisticBoardResponse initial = controller.getBoard("mirror-table-overview", Map.of()).getData();
    var tableField =
        initial.definition().filters().stream()
            .filter(field -> field.key().equals("tableName"))
            .findFirst()
            .orElseThrow();
    if (tableField.options().isEmpty()) {
      assertThat(initial.rows()).isEmpty();
      return;
    }
    String selectedTable = tableField.options().get(0).value();

    StatisticBoardResponse filtered =
        controller.getBoard("mirror-table-overview", Map.of("filterGroup", textFilter("tableName", "eq", selectedTable))).getData();

    assertThat(filtered.rows()).hasSize(1);
    assertThat(filtered.rows().get(0).rowKey()).isEqualTo(selectedTable);
    assertThat(filtered.appliedFilterGroup()).isNotNull();
    assertThat(filtered.appliedFilterGroup().conditions()).hasSize(1);
  }

  @Test
  void shouldFilterBoardByNumericCondition() {
    StatisticBoardResponse initial = controller.getBoard("mirror-table-overview", Map.of()).getData();
    if (initial.rows().isEmpty()) {
      assertThat(initial.meta().rowCount()).isZero();
      return;
    }
    long threshold =
        initial.rows().get(0).cells().stream()
            .filter(cell -> cell.columnKey().equals("totalRecords"))
            .findFirst()
            .orElseThrow()
            .numericValue();

    StatisticBoardResponse filtered =
        controller.getBoard("mirror-table-overview", Map.of("filterGroup", textFilter("totalRecords", "gte", String.valueOf(threshold)))).getData();

    assertThat(filtered.rows()).isNotEmpty();
    assertThat(filtered.rows())
        .allSatisfy(row ->
            assertThat(
                row.cells().stream()
                    .filter(cell -> cell.columnKey().equals("totalRecords"))
                    .findFirst()
                    .orElseThrow()
                    .numericValue()).isGreaterThanOrEqualTo(threshold));
  }

  @Test
  void shouldFilterBoardByDateTimeCondition() {
    String farPast = LocalDateTime.of(2000, 1, 1, 0, 0, 0).format(DATE_TIME_FORMATTER);
    StatisticBoardResponse filtered =
        controller.getBoard("mirror-table-overview", Map.of("filterGroup", textFilter("lastSyncedAt", "after", farPast))).getData();

    assertThat(filtered).isNotNull();
    assertThat(filtered.meta().rowCount()).isEqualTo(filtered.rows().size());
  }

  @Test
  void shouldLoadDetailsForDrilldownCell() {
    StatisticBoardResponse board = controller.getBoard("mirror-table-overview", Map.of()).getData();
    if (board.rows().isEmpty()) {
      assertThat(board.meta().rowCount()).isZero();
      return;
    }
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
                Map.of("filterGroup", textFilter("tableName", "eq", rowKey)))
            .getData();

    assertThat(detail).isNotNull();
    assertThat(detail.columns()).isNotEmpty();
    assertThat(detail.total()).isGreaterThan(0);
  }

  @Test
  void shouldExportMirrorTableOverviewBoardCsv() {
    ResponseEntity<String> response =
        controller.exportBoard("mirror-table-overview", Map.of("filterGroup", textFilter("totalRecords", "gt", "0")));
    assertThat(response.getBody()).isNotBlank();
    assertThat(response.getBody()).contains("总记录数");
  }

  @Test
  void shouldRejectUnsupportedFilterOperator() {
    assertThatThrownBy(
            () ->
                controller.getBoard(
                    "mirror-table-overview",
                    Map.of("filterGroup", textFilter("tableName", "gt", "abc"))))
        .hasMessageContaining("Unsupported operator");
  }

  private String textFilter(String fieldKey, String operator, String value) {
    return "{\"logic\":\"AND\",\"conditions\":[{\"fieldKey\":\"" + fieldKey + "\",\"operator\":\"" + operator + "\",\"value\":\"" + value + "\"}]}";
  }
}
