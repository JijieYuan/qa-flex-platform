package com.data.collection.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.data.collection.platform.common.JsonUtils;
import com.data.collection.platform.common.exception.BizException;
import com.data.collection.platform.entity.GitlabMirrorTableRegistry;
import com.data.collection.platform.entity.SourceTableColumn;
import com.data.collection.platform.entity.database.DatabaseTableColumn;
import com.data.collection.platform.entity.database.DatabaseTableOption;
import com.data.collection.platform.entity.database.DatabaseTableRowsResponse;
import com.data.collection.platform.mapper.GitlabMirrorTableRegistryMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import java.sql.SQLException;
import java.time.LocalDateTime;
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
  private static final TypeReference<List<SourceTableColumn>> COLUMN_LIST_TYPE = new TypeReference<>() {};

  private static final Map<String, TableDefinition> TABLE_DEFINITIONS = Map.of(
      "gitlab_sync_configs", new TableDefinition(
          "同步配置",
          List.of("name", "source_mode", "whitelist_mode", "db_name", "db_username", "docker_container_name"),
          List.of(
              new DatabaseTableColumn("id", "ID", true),
              new DatabaseTableColumn("name", "数据源名称", true),
              new DatabaseTableColumn("source_mode", "读取方式", true),
              new DatabaseTableColumn("whitelist_mode", "白名单模式", true),
              new DatabaseTableColumn("updated_at", "更新时间", true),
              new DatabaseTableColumn("created_at", "创建时间", true)),
          "id"),
      "gitlab_sync_logs", new TableDefinition(
          "同步日志",
          List.of("sync_type", "status", "message"),
          List.of(
              new DatabaseTableColumn("id", "ID", true),
              new DatabaseTableColumn("sync_type", "同步类型", true),
              new DatabaseTableColumn("status", "状态", true),
              new DatabaseTableColumn("table_count", "表数", true),
              new DatabaseTableColumn("record_count", "记录数", true),
              new DatabaseTableColumn("started_at", "开始时间", true),
              new DatabaseTableColumn("finished_at", "结束时间", true)),
          "id"),
      "gitlab_sync_tasks", new TableDefinition(
          "同步任务",
          List.of("run_id", "task_type", "trigger_type", "scope_key", "status", "finished_reason"),
          List.of(
              new DatabaseTableColumn("id", "ID", true),
              new DatabaseTableColumn("task_type", "任务类型", true),
              new DatabaseTableColumn("trigger_type", "触发方式", true),
              new DatabaseTableColumn("status", "任务状态", true),
              new DatabaseTableColumn("queued_at", "排队时间", true),
              new DatabaseTableColumn("started_at", "开始时间", true),
              new DatabaseTableColumn("finished_at", "结束时间", true)),
          "id"),
      "gitlab_webhook_events", new TableDefinition(
          "Webhook 事件",
          List.of("event_type", "project_path", "event_id", "delivery_id"),
          List.of(
              new DatabaseTableColumn("id", "ID", true),
              new DatabaseTableColumn("event_type", "事件类型", true),
              new DatabaseTableColumn("project_path", "项目路径", true),
              new DatabaseTableColumn("received_at", "接收时间", true)),
          "id"),
      "gitlab_mirror_records", new TableDefinition(
          "旧镜像记录",
          List.of("table_name", "record_key", "row_data"),
          List.of(
              new DatabaseTableColumn("id", "ID", true),
              new DatabaseTableColumn("table_name", "源表名", true),
              new DatabaseTableColumn("record_key", "主键值", true),
              new DatabaseTableColumn("updated_at_source", "源更新时间", true),
              new DatabaseTableColumn("synced_at", "镜像时间", true)),
          "id"),
      "collect_form_records", new TableDefinition(
          "采集表单记录",
          List.of("template_code", "resource_type", "resource_id", "reviewer", "remark"),
          List.of(
              new DatabaseTableColumn("id", "ID", true),
              new DatabaseTableColumn("gitlab_base_url", "GitLab 来源地址", true),
              new DatabaseTableColumn("project_id", "Project ID", true),
              new DatabaseTableColumn("mr_iid", "MR IID", true),
              new DatabaseTableColumn("resource_type", "资源类型", true),
              new DatabaseTableColumn("resource_id", "资源编号", true),
              new DatabaseTableColumn("template_code", "模板编码", true),
              new DatabaseTableColumn("form_title", "表单标题", true),
              new DatabaseTableColumn("reviewer", "走查人", true),
              new DatabaseTableColumn("review_duration_minutes", "走查时间(分钟)", true),
              new DatabaseTableColumn("specification_score", "规范", true),
              new DatabaseTableColumn("logic_score", "逻辑", true),
              new DatabaseTableColumn("performance_score", "性能", true),
              new DatabaseTableColumn("design_score", "设计", true),
              new DatabaseTableColumn("other_score", "其他", true),
              new DatabaseTableColumn("deleted", "是否作废", true),
              new DatabaseTableColumn("updated_at", "更新时间", true),
              new DatabaseTableColumn("created_at", "创建时间", true)),
          "updated_at"));

  private final JdbcTemplate jdbcTemplate;
  private final GitlabMirrorTableRegistryMapper registryMapper;
  private final JsonUtils jsonUtils;

  public DatabaseBrowserService(
      JdbcTemplate jdbcTemplate,
      GitlabMirrorTableRegistryMapper registryMapper,
      JsonUtils jsonUtils) {
    this.jdbcTemplate = jdbcTemplate;
    this.registryMapper = registryMapper;
    this.jsonUtils = jsonUtils;
  }

  public List<DatabaseTableOption> listTables() {
    Map<String, DatabaseTableOption> allTables = new LinkedHashMap<>();
    TABLE_DEFINITIONS.forEach((key, value) ->
        allTables.put(key, new DatabaseTableOption(key, value.label(), "IDLE", null)));
    listMirrorRegistries().forEach(registry -> allTables.put(
        registry.getMirrorTableName(),
        new DatabaseTableOption(
            registry.getMirrorTableName(),
            buildMirrorLabel(registry.getSourceTableName()),
            registry.getSyncStatus(),
            registry.getLastSyncTime())));
    return allTables.values().stream()
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
    TableContext context = resolveTableContext(tableName);
    int safePage = page == null || page < 1 ? DEFAULT_PAGE : page;
    int safeSize = size == null || size < 1 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
    String validatedSortField = normalizeSortField(context.definition(), sortField);
    String validatedSortOrder = normalizeSortOrder(sortOrder);
    String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;

    SqlBundle sqlBundle =
        buildSql(context.definition(), tableName, normalizedKeyword, validatedSortField, validatedSortOrder, safePage, safeSize);
    long total = jdbcTemplate.queryForObject(sqlBundle.countSql(), Long.class, sqlBundle.arguments().toArray());
    List<Map<String, Object>> rows = jdbcTemplate.query(sqlBundle.rowsSql(), tableRowMapper(), sqlBundle.arguments().toArray());

    String statusMessage = null;
    if (context.registry() != null && Objects.equals(context.registry().getSyncStatus(), "SYNCING")) {
      statusMessage = "数据正在同步中，当前展示为历史稳定版本。";
    }

    return new DatabaseTableRowsResponse(
        tableName,
        context.definition().label(),
        context.definition().columns(),
        rows,
        total,
        safePage,
        safeSize,
        validatedSortField,
        validatedSortOrder,
        normalizedKeyword,
        context.registry() == null ? "IDLE" : context.registry().getSyncStatus(),
        context.registry() == null ? null : context.registry().getLastSyncTime(),
        statusMessage);
  }

  private List<GitlabMirrorTableRegistry> listMirrorRegistries() {
    return registryMapper.selectList(new LambdaQueryWrapper<GitlabMirrorTableRegistry>()
        .eq(GitlabMirrorTableRegistry::getInitialized, true)
        .orderByAsc(GitlabMirrorTableRegistry::getSourceTableName));
  }

  private TableContext resolveTableContext(String tableName) {
    TableDefinition systemDefinition = TABLE_DEFINITIONS.get(tableName);
    if (systemDefinition != null) {
      return new TableContext(systemDefinition, null);
    }
    GitlabMirrorTableRegistry registry = registryMapper.selectOne(new LambdaQueryWrapper<GitlabMirrorTableRegistry>()
        .eq(GitlabMirrorTableRegistry::getMirrorTableName, tableName)
        .eq(GitlabMirrorTableRegistry::getInitialized, true)
        .last("limit 1"));
    if (registry == null) {
      throw new BizException("当前表不在数据库查看白名单中");
    }
    return new TableContext(buildMirrorTableDefinition(registry), registry);
  }

  private TableDefinition buildMirrorTableDefinition(GitlabMirrorTableRegistry registry) {
    List<SourceTableColumn> sourceColumns = jsonUtils.fromJson(registry.getColumnSnapshot(), COLUMN_LIST_TYPE);
    if (sourceColumns == null || sourceColumns.isEmpty()) {
      throw new BizException("当前镜像表注册信息缺少字段快照，无法展示数据库查看");
    }
    List<DatabaseTableColumn> columns = new ArrayList<>();
    for (SourceTableColumn sourceColumn : sourceColumns) {
      columns.add(new DatabaseTableColumn(
          sourceColumn.columnName(),
          prettifyColumnName(sourceColumn.columnName()),
          true));
    }
    columns.add(new DatabaseTableColumn("mirror_task_id", "同步任务 ID", true));
    columns.add(new DatabaseTableColumn("source_updated_at", "源更新时间", true));
    columns.add(new DatabaseTableColumn("mirror_synced_at", "镜像时间", true));
    columns.add(new DatabaseTableColumn("mirror_deleted", "是否删除", true));

    List<String> searchableFields = sourceColumns.stream()
        .map(SourceTableColumn::columnName)
        .filter(this::isSearchableField)
        .limit(6)
        .toList();
    if (searchableFields.isEmpty()) {
      searchableFields = List.of(sourceColumns.get(0).columnName());
    }

    String defaultSortField = StringUtils.hasText(registry.getUpdatedAtColumn()) && columns.stream()
        .anyMatch(column -> Objects.equals(column.getKey(), registry.getUpdatedAtColumn()))
        ? registry.getUpdatedAtColumn()
        : "mirror_synced_at";
    return new TableDefinition(
        buildMirrorLabel(registry.getSourceTableName()),
        searchableFields,
        columns,
        defaultSortField);
  }

  private boolean isSearchableField(String columnName) {
    return !List.of("metadata", "payload", "description_html").contains(columnName);
  }

  private String normalizeSortField(TableDefinition definition, String sortField) {
    if (!StringUtils.hasText(sortField)) {
      return definition.defaultSortField();
    }
    boolean allowed = definition.columns().stream().anyMatch(column -> column.isSortable() && Objects.equals(column.getKey(), sortField));
    if (!allowed) {
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
      whereBuilder.append(definition.searchableFields().stream()
          .map(field -> "cast(" + quoteIdentifier(field) + " as text) ilike ?")
          .collect(Collectors.joining(" or ")));
      whereBuilder.append(")");
      String likeValue = "%" + keyword + "%";
      definition.searchableFields().forEach(field -> arguments.add(likeValue));
    }
    String orderClause =
        " order by " + quoteIdentifier(sortField) + " " + sortOrder + ", " + quoteIdentifier(definition.defaultSortField()) + " desc";
    String countSql = "select count(*) from " + quoteIdentifier(tableName) + whereBuilder;
    String rowsSql = "select * from " + quoteIdentifier(tableName) + whereBuilder + orderClause + " limit " + size + " offset "
        + ((page - 1) * size);
    return new SqlBundle(countSql, rowsSql, arguments);
  }

  private RowMapper<Map<String, Object>> tableRowMapper() {
    return (resultSet, rowNum) -> {
      int columnCount = resultSet.getMetaData().getColumnCount();
      Map<String, Object> row = new LinkedHashMap<>();
      for (int index = 1; index <= columnCount; index++) {
        String columnName = resultSet.getMetaData().getColumnName(index);
        row.put(columnName, normalizeValue(resultSet.getObject(index)));
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

  private String buildMirrorLabel(String sourceTableName) {
    return "镜像表 / " + sourceTableName;
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

  private String quoteIdentifier(String identifier) {
    return "\"" + identifier.replace("\"", "\"\"") + "\"";
  }

  private record TableDefinition(
      String label,
      List<String> searchableFields,
      List<DatabaseTableColumn> columns,
      String defaultSortField) {
  }

  private record SqlBundle(String countSql, String rowsSql, List<Object> arguments) {
  }

  private record TableContext(TableDefinition definition, GitlabMirrorTableRegistry registry) {
  }
}
