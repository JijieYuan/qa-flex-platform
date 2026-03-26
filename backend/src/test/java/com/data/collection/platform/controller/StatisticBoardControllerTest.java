package com.data.collection.platform.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.data.collection.platform.entity.statistics.StatisticBoardResponse;
import com.data.collection.platform.entity.statistics.StatisticDetailResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StatisticBoardControllerTest {
  @Autowired
  private StatisticBoardController controller;

  @Test
  void shouldLoadMirrorTableOverviewBoard() {
    StatisticBoardResponse response = controller.getBoard("mirror-table-overview", java.util.Map.of("limit", "10")).getData();
    assertThat(response).isNotNull();
    assertThat(response.definition().boardKey()).isEqualTo("mirror-table-overview");
    assertThat(response.definition().columnGroups()).isNotEmpty();
    assertThat(response.rows()).isNotEmpty();
  }

  @Test
  void shouldLoadDetailsForDrilldownCell() {
    StatisticBoardResponse board = controller.getBoard("mirror-table-overview", java.util.Map.of("limit", "10")).getData();
    assertThat(board.rows()).isNotEmpty();
    String rowKey = board.rows().get(0).rowKey();
    StatisticDetailResponse detail = controller.getDetails(
        "mirror-table-overview",
        rowKey,
        "totalRecords",
        1,
        10,
        "syncedAt",
        "descending",
        java.util.Map.of()).getData();
    assertThat(detail).isNotNull();
    assertThat(detail.columns()).isNotEmpty();
    assertThat(detail.total()).isGreaterThan(0);
  }
}
