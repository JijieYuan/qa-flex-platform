package com.data.collection.platform.service.statistics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.data.collection.platform.common.JsonUtils;
import com.data.collection.platform.entity.statistics.StatisticBoardRuleExplanationResponse;
import com.data.collection.platform.service.FactBuildService;
import com.data.collection.platform.service.GitlabMirrorSyncService;
import com.data.collection.platform.service.RealtimeWorkspaceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

@ExtendWith(MockitoExtension.class)
class SystemTestDefectSummaryRuleExplanationTest {

  @Mock
  private JdbcTemplate jdbcTemplate;

  @Mock
  private GitlabMirrorSyncService gitlabMirrorSyncService;

  @Mock
  private RealtimeWorkspaceService realtimeWorkspaceService;

  @Mock
  private FactBuildService factBuildService;

  @Test
  void shouldDescribeSystemTestBoardRuleFlowEvenWhenMirrorTablesAreEmpty() {
    when(jdbcTemplate.query(anyString(), org.mockito.ArgumentMatchers.<RowMapper<Object>>any())).thenReturn(List.of());
    SystemTestDefectSummaryBoardService service = new SystemTestDefectSummaryBoardService(
        jdbcTemplate,
        new JsonUtils(new ObjectMapper()),
        gitlabMirrorSyncService,
        realtimeWorkspaceService,
        factBuildService);

    StatisticBoardRuleExplanationResponse response = service.getRuleExplanation(Map.of());

    assertThat(response.supported()).isTrue();
    assertThat(response.version()).isEqualTo("system-test-defect-summary@2026-04-08-v2");
    assertThat(response.flowSteps()).extracting("key")
        .containsExactly(
            "source-load",
            "exclude-invalid-issues",
            "aggregate-normalized-facts");
    assertThat(response.flowSteps()).allSatisfy(step -> {
      assertThat(step.title()).isNotBlank();
      assertThat(step.description()).isNotBlank();
      assertThat(step.inputCount()).isZero();
      assertThat(step.outputCount()).isZero();
    });
    assertThat(response.metricDefinitions()).extracting("key")
        .contains("level1", "level2", "level3", "priority", "summary");
  }
}
