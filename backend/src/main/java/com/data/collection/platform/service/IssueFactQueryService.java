package com.data.collection.platform.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class IssueFactQueryService {
  private final JdbcTemplate jdbcTemplate;

  public IssueFactQueryService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public <T> List<T> query(String selectSql, Map<String, String> filters, RowMapper<T> rowMapper) {
    StringBuilder sql = new StringBuilder(selectSql);
    List<Object> args = new ArrayList<>();
    applyCommonFilters(sql, args, filters == null ? Map.of() : filters);
    return jdbcTemplate.query(sql.toString(), rowMapper, args.toArray());
  }

  private void applyCommonFilters(StringBuilder sql, List<Object> args, Map<String, String> filters) {
    appendEq(sql, args, "projectId", "project_id", filters.get("projectId"), Long::parseLong);
    appendContains(sql, args, "projectName", "project_name", filters.get("projectName"));
    appendContains(sql, args, "testingPhase", "testing_phase", filters.get("testingPhase"));
    appendContains(sql, args, "moduleName", "module_names", filters.get("moduleName"));
    appendEq(sql, args, "severityLevel", "severity_level", filters.get("severityLevel"), value -> value);
    appendEq(sql, args, "priorityLevel", "priority_level", filters.get("priorityLevel"), value -> value);
  }

  private <T> void appendEq(
      StringBuilder sql,
      List<Object> args,
      String filterKey,
      String column,
      String rawValue,
      java.util.function.Function<String, T> mapper) {
    String value = trimToNull(rawValue);
    if (value == null) {
      return;
    }
    sql.append(" and ").append(column).append(" = ?");
    args.add(mapper.apply(value));
  }

  private void appendContains(StringBuilder sql, List<Object> args, String filterKey, String column, String rawValue) {
    String value = trimToNull(rawValue);
    if (value == null) {
      return;
    }
    sql.append(" and ").append(column).append(" like ?");
    args.add("%" + value + "%");
  }

  private String trimToNull(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }
}
