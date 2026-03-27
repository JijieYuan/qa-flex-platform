package com.data.collection.platform.service;

import com.data.collection.platform.common.exception.BizException;
import com.data.collection.platform.entity.database.DatabaseTableColumn;
import com.data.collection.platform.entity.database.DatabaseTableOption;
import com.data.collection.platform.entity.database.DatabaseTableRowsResponse;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DatabaseBrowserService {

  private static final int DEFAULT_PAGE = 1;
  private static final int DEFAULT_SIZE = 20;
  private static final int MAX_SIZE = 100;

  private static final Map<String, TableDefinition> TABLE_DEFINITIONS = Map.of(
      "gitlab_sync_configs", new TableDefinition(
          "同步配置",
          List.of("name", "source_mode", "whitelist_mode", "db_name", "db_username", "docker_container_name"),
          List.of("id", "name", "source_mode", "whitelist_mode", "updated_at", "created_at"),
          "id"),
      "gitlab_sync_logs", new TableDefinition(
          "同步日志",
          List.of("sync_type", "status", "message"),
          List.of("id", "sync_type", "status", "table_count", "record_count", "started_at", "finished_at"),
          "id"),
      "gitlab_sync_tasks", new TableDefinition(
          "同步任务",
          List.of("run_id", "task_type", "trigger_type", "scope_key", "status", "finished_reason"),
          List.of("id", "task_type", "trigger_type", "status", "queued_at", "started_at", "finished_at"),
          "id"),
      "gitlab_webhook_events", new TableDefinition(
          "Webhook 事件",
          List.of("event_type", "project_path", "event_id", "delivery_id"),
          List.of("id", "event_type", "project_path", "received_at"),
          "id"),
      "gitlab_mirror_records", new TableDefinition(
          "镜像记录",
          List.of("table_name", "source_primary_key", "payload_json"),
          List.of("id", "table_name", "source_primary_key", "source_updated_at", "mirrored_at"),
          "id"));

  private final JdbcTemplate jdbcTemplate;

  public DatabaseBrowserService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public List<DatabaseTableOption> listTables() {
    return TABLE_DEFINITIONS.entrySet().stream()
        .map(entry -> new DatabaseTableOption(entry.getKey(), entry.getValue().label()))
        .sorted((left, right) -> left.getTableName().compareToIgnoreCase(right.getTableName()))
        .toList();
  }

  public DatabaseTableRowsResponse getTableRows(
      String tableName,
      Integer page,
      Integer size,
      String keyword,
      String sortField,
      String sortOrder) {
    TableDefinition definition = getRequiredTableDefinition(tableName);
    int safePage = page == null || page < 1 ? DEFAULT_PAGE : page;
    int safeSize = size == null || size < 1 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
    String validatedSortField = normalizeSortField(definition, sortField);
    String validatedSortOrder = normalizeSortOrder(sortOrder);
    String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;

    SqlBundle sqlBundle = buildSql(definition, tableName, normalizedKeyword, validatedSortField, validatedSortOrder, safePage, safeSize);
    long total = jdbcTemplate.queryForObject(sqlBundle.countSql(), Long.class, sqlBundle.arguments().toArray());
    List<Map<String, Object>> rows = jdbcTemplate.query(sqlBundle.rowsSql(), tableRowMapper(), sqlBundle.arguments().toArray());
    List<DatabaseTableColumn> columns = resolveColumns(tableName);
    return new DatabaseTableRowsResponse(
        tableName,
        definition.label(),
        columns,
        rows,
        total,
        safePage,
        safeSize,
        validatedSortField,
        validatedSortOrder,
        normalizedKeyword);
  }

  private TableDefinition getRequiredTableDefinition(String tableName) {
    TableDefinition definition = TABLE_DEFINITIONS.get(tableName);
    if (definition == null) {
      throw new BizException("当前表不在数据库查看白名单中");
    }
    return definition;
  }

  private String normalizeSortField(TableDefinition definition, String sortField) {
    if (!StringUtils.hasText(sortField)) {
      return definition.defaultSortField();
    }
    if (!definition.sortableFields().contains(sortField)) {
      throw new BizException("当前排序字段不允许使用");
    }
    return sortField;
  }

  private String normalizeSortOrder(String sortOrder) {
    if (!StringUtils.hasText(sortOrder)) {
      return "desc";
    }
    String normalized = sortOrder.trim().toLowerCase(Locale.ROOT);
    if (!Objects.equals(normalized, "asc") && !Objects.equals(normalized, "desc")) {
      throw new BizException("当前排序方向不允许使用");
    }
    return normalized;
  }

  private SqlBundle buildSql(
      TableDefinition definition,
      String tableName,
      String keyword,
      String sortField,
      String sortOrder,
      int page,
      int size) {
    StringBuilder whereBuilder = new StringBuilder(" where 1 = 1");
    List<Object> arguments = new ArrayList<>();
    if (StringUtils.hasText(keyword) && !definition.searchableFields().isEmpty()) {
      whereBuilder.append(" and (");
      whereBuilder.append(
          definition.searchableFields().stream()
              .map(field -> "cast(" + field + " as text) ilike ?")
              .collect(Collectors.joining(" or ")));
      whereBuilder.append(")");
      String likeValue = "%" + keyword + "%";
      definition.searchableFields().forEach(field -> arguments.add(likeValue));
    }
    String orderClause = " order by " + sortField + " " + sortOrder + ", " + definition.defaultSortField() + " desc";
    String countSql = "select count(*) from " + tableName + whereBuilder;
    String rowsSql = "select * from " + tableName + whereBuilder + orderClause + " limit " + size + " offset " + ((page - 1) * size);
    return new SqlBundle(countSql, rowsSql, arguments);
  }

  private List<DatabaseTableColumn> resolveColumns(String tableName) {
    return jdbcTemplate.query(
        "select * from " + tableName + " limit 1",
        resultSet -> {
          ResultSetMetaData metaData = resultSet.getMetaData();
          List<DatabaseTableColumn> columns = new ArrayList<>();
          TableDefinition definition = getRequiredTableDefinition(tableName);
          for (int index = 1; index <= metaData.getColumnCount(); index++) {
            String columnName = metaData.getColumnName(index);
            columns.add(new DatabaseTableColumn(columnName, prettifyColumnName(columnName), definition.sortableFields().contains(columnName)));
          }
          return columns;
        });
  }

  private RowMapper<Map<String, Object>> tableRowMapper() {
    return (resultSet, rowNum) -> {
      ResultSetMetaData metaData = resultSet.getMetaData();
      Map<String, Object> row = new LinkedHashMap<>();
      for (int index = 1; index <= metaData.getColumnCount(); index++) {
        String columnName = metaData.getColumnName(index);
        Object value = resultSet.getObject(index);
        row.put(columnName, normalizeValue(value));
      }
      return row;
    };
  }

  private Object normalizeValue(Object value) throws SQLException {
    if (value == null) {
      return null;
    }
    if ("org.postgresql.util.PGobject".equals(value.getClass().getName())) {
      return value.toString();
    }
    if (value instanceof TemporalAccessor) {
      return value.toString();
    }
    return value;
  }

  private String prettifyColumnName(String columnName) {
    String[] parts = columnName.split("_");
    StringBuilder builder = new StringBuilder();
    for (String part : parts) {
      if (!builder.isEmpty()) {
        builder.append(' ');
      }
      builder.append(part.substring(0, 1).toUpperCase(Locale.ROOT));
      if (part.length() > 1) {
        builder.append(part.substring(1));
      }
    }
    return builder.toString();
  }

  private record TableDefinition(
      String label,
      List<String> searchableFields,
      List<String> sortableFields,
      String defaultSortField) {
  }

  private record SqlBundle(String countSql, String rowsSql, List<Object> arguments) {
  }
}
