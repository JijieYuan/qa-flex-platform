package com.data.collection.platform.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class CodeReviewIllegalRecordSourceLoader {
  private static final String FACT_SQL = """
      select
        merge_request_id,
        merge_request_iid,
        project_id,
        title as merge_request_content,
        project_name,
        repository_name,
        merged_at_source as merged_at,
        merge_user_name as merged_by,
        owner_name as owner,
        target_branch,
        module_name,
        coalesce(label_names, '') as label_names,
        review_status,
        review_duration_minutes,
        scan_status,
        scan_bug_count,
        comment_rate,
        defect_count,
        added_lines
      from merge_request_fact
      where deleted = false
      """;

  private final FactBuildService factBuildService;
  private final MergeRequestFactQueryService mergeRequestFactQueryService;

  public CodeReviewIllegalRecordSourceLoader(
      FactBuildService factBuildService,
      MergeRequestFactQueryService mergeRequestFactQueryService) {
    this.factBuildService = factBuildService;
    this.mergeRequestFactQueryService = mergeRequestFactQueryService;
  }

  public List<CodeReviewIllegalRecordSource> loadSources(Map<String, String> filters) {
    try {
      List<CodeReviewIllegalRecordSource> facts = ensureFactsReady(filters);
      if (!facts.isEmpty()) {
        return facts;
      }
    } catch (DataAccessException e) {
      log.warn("Failed to load merge request facts", e);
      return List.of();
    }
    return List.of();
  }

  private List<CodeReviewIllegalRecordSource> ensureFactsReady(Map<String, String> filters) {
    List<CodeReviewIllegalRecordSource> facts = mergeRequestFactQueryService.query(FACT_SQL, filters, this::mapFactSource);
    if (!facts.isEmpty()) {
      return facts;
    }
    factBuildService.rebuildMergeRequestFacts(true);
    return mergeRequestFactQueryService.query(FACT_SQL, filters, this::mapFactSource);
  }

  private CodeReviewIllegalRecordSource mapFactSource(ResultSet rs, int rowNum) throws SQLException {
    return new CodeReviewIllegalRecordSource(
        rs.getLong("merge_request_id"),
        rs.getInt("merge_request_iid"),
        rs.getLong("project_id"),
        rs.getString("merge_request_content"),
        rs.getString("project_name"),
        rs.getString("repository_name"),
        rs.getTimestamp("merged_at") == null ? null : rs.getTimestamp("merged_at").toLocalDateTime(),
        rs.getString("merged_by"),
        rs.getString("owner"),
        rs.getString("target_branch"),
        rs.getString("module_name"),
        splitLabels(rs.getString("label_names")),
        rs.getString("review_status"),
        (Integer) rs.getObject("review_duration_minutes"),
        rs.getString("scan_status"),
        (Integer) rs.getObject("scan_bug_count"),
        toDouble(rs.getObject("comment_rate")),
        (Integer) rs.getObject("defect_count"),
        (Integer) rs.getObject("added_lines"));
  }

  private Double toDouble(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Number number) {
      return number.doubleValue();
    }
    return Double.valueOf(String.valueOf(value));
  }

  private List<String> splitLabels(String labelNames) {
    if (!StringUtils.hasText(labelNames)) {
      return List.of();
    }
    List<String> result = new ArrayList<>();
    for (String value : labelNames.split(",")) {
      String normalized = TextQuerySupport.trimToNull(value);
      if (normalized != null) {
        result.add(normalized);
      }
    }
    return result;
  }
}
