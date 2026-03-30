package com.data.collection.platform.service;

import com.data.collection.platform.common.exception.BizException;
import com.data.collection.platform.common.logging.GitlabSyncLogContext;
import com.data.collection.platform.config.GitlabMirrorProperties;
import com.data.collection.platform.entity.GitlabSyncConfig;
import com.data.collection.platform.entity.SourceMode;
import com.data.collection.platform.entity.SourceTableColumn;
import com.data.collection.platform.entity.SourceTableSchema;
import com.data.collection.platform.entity.TableWhitelistOption;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class GitlabExternalDbService {
  private static final TypeReference<LinkedHashMap<String, Object>> MAP_TYPE = new TypeReference<>() {};
  private static final List<String> UPDATED_AT_CANDIDATES = List.of(
      "updatedat",
      "modifiedat",
      "lastmodifiedat",
      "lastupdatedat",
      "updatetime",
      "modifiedtime",
      "lastupdatetime",
      "lastmodifytime",
      "updatedon",
      "modifiedon",
      "lastmodifiedon",
      "lastupdatedon",
      "gmtmodified",
      "operatetime",
      "eventtime",
      "synctime");
  private static final List<String> CREATED_AT_CANDIDATES = List.of(
      "createdat",
      "createdon",
      "createtime",
      "inserttime",
      "writetime",
      "gmtcreate",
      "loadtime",
      "etltime");

  private final GitlabMirrorProperties properties;
  private final ObjectMapper objectMapper;

  public GitlabExternalDbService(GitlabMirrorProperties properties, ObjectMapper objectMapper) {
    this.properties = properties;
    this.objectMapper = objectMapper;
  }

  public void testConnection(GitlabSyncConfig config) {
    try {
      if (isDockerMode(config)) {
        executeDockerSql(config, "select 1");
        return;
      }
      try (Connection connection = openConnection(config); Statement statement = connection.createStatement()) {
        statement.execute("select 1");
      }
    } catch (Exception e) {
      try (GitlabSyncLogContext.Scope action = GitlabSyncLogContext.action("Connection_Test")) {
        log.error("GitLab PostgreSQL connection test failed", e);
      }
      throw new BizException("GitLab PostgreSQL connection failed: " + e.getMessage());
    }
  }

  public List<TableWhitelistOption> discoverTables(GitlabSyncConfig config, Map<String, String> labels, List<String> recommendedTables) {
    String sql = """
        select
          t.table_name,
          string_agg(kcu.column_name, ',' order by kcu.ordinal_position) as primary_key
        from information_schema.tables t
        join information_schema.table_constraints tc
          on tc.table_schema = t.table_schema
         and tc.table_name = t.table_name
         and tc.constraint_type = 'PRIMARY KEY'
        join information_schema.key_column_usage kcu
          on kcu.table_schema = tc.table_schema
         and kcu.table_name = tc.table_name
         and kcu.constraint_name = tc.constraint_name
        where t.table_schema = 'public'
          and t.table_type = 'BASE TABLE'
        group by t.table_name
        order by t.table_name
        """;
    List<Map<String, Object>> rows = isDockerMode(config) ? executeDockerQuery(config, sql) : executeJdbcQuery(config, sql);
    Map<String, String> updatedAtColumnMap = discoverUpdatedAtColumns(config);
    List<TableWhitelistOption> result = new ArrayList<>(rows.size());
    for (Map<String, Object> row : rows) {
      String tableName = String.valueOf(row.get("table_name"));
      String primaryKey = String.valueOf(row.get("primary_key"));
      String updatedAtColumn = updatedAtColumnMap.get(tableName);
      String label = labels.getOrDefault(tableName, tableName);
      boolean recommended = recommendedTables.contains(tableName);
      result.add(new TableWhitelistOption(tableName, label, primaryKey, updatedAtColumn, recommended));
    }
    return result;
  }

  public SourceTableSchema discoverTableSchema(GitlabSyncConfig config, TableWhitelistOption option) {
    String sql = """
        select
          a.attname as column_name,
          pg_catalog.format_type(a.atttypid, a.atttypmod) as formatted_type,
          not a.attnotnull as nullable,
          a.attnum as ordinal_position
        from pg_attribute a
        join pg_class c on a.attrelid = c.oid
        join pg_namespace n on c.relnamespace = n.oid
        where n.nspname = 'public'
          and c.relname = '%s'
          and a.attnum > 0
          and not a.attisdropped
        order by a.attnum
        """.formatted(option.tableName().replace("'", "''"));
    List<Map<String, Object>> rows = isDockerMode(config) ? executeDockerQuery(config, sql) : executeJdbcQuery(config, sql);
    List<SourceTableColumn> columns = new ArrayList<>(rows.size());
    for (Map<String, Object> row : rows) {
      columns.add(new SourceTableColumn(
          String.valueOf(row.get("column_name")),
          String.valueOf(row.get("formatted_type")),
          Boolean.parseBoolean(String.valueOf(row.get("nullable"))),
          ((Number) row.get("ordinal_position")).intValue()));
    }
    if (columns.isEmpty()) {
      throw new BizException("未发现源表结构: " + option.tableName());
    }
    List<String> primaryKeys = List.of(option.primaryKey().split(","))
        .stream()
        .map(String::trim)
        .filter(value -> !value.isBlank())
        .toList();
    return new SourceTableSchema(option.tableName(), primaryKeys, option.updatedAtColumn(), columns);
  }

  public List<Map<String, Object>> fullTableScan(GitlabSyncConfig config, TableWhitelistOption option) {
    String sql = "select * from public.%s".formatted(option.tableName());
    return isDockerMode(config) ? executeDockerQuery(config, sql) : executeJdbcQuery(config, sql);
  }

  public List<Map<String, Object>> incrementalScan(GitlabSyncConfig config, TableWhitelistOption option, LocalDateTime since) {
    if (since == null || option.updatedAtColumn() == null || option.updatedAtColumn().isBlank()) {
      return List.of();
    }
    return timeWindowScan(config, option, since);
  }

  public List<Map<String, Object>> compensationScan(GitlabSyncConfig config, TableWhitelistOption option, LocalDateTime since) {
    if (since == null || option.updatedAtColumn() == null || option.updatedAtColumn().isBlank()) {
      return List.of();
    }
    return timeWindowScan(config, option, since);
  }

  public List<Map<String, Object>> preciseScan(
      GitlabSyncConfig config,
      TableWhitelistOption option,
      String lookupColumn,
      Object lookupValue) {
    if (lookupColumn == null || lookupColumn.isBlank() || lookupValue == null) {
      return List.of();
    }
    String sql = "select * from public.%s where %s = %s".formatted(
        option.tableName(),
        quoteIdentifier(lookupColumn),
        toSqlLiteral(lookupValue));
    return isDockerMode(config) ? executeDockerQuery(config, sql) : executeJdbcQuery(config, sql);
  }

  private List<Map<String, Object>> timeWindowScan(GitlabSyncConfig config, TableWhitelistOption option, LocalDateTime since) {
    LocalDateTime gitlabSince = toGitlabSourceTime(since);
    String sql = "select * from public.%s where %s >= timestamp '%s'".formatted(
        option.tableName(),
        option.updatedAtColumn(),
        gitlabSince.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    return isDockerMode(config) ? executeDockerQuery(config, sql) : executeJdbcQuery(config, sql);
  }

  Map<String, String> discoverUpdatedAtColumns(GitlabSyncConfig config) {
    String sql = """
        select table_name, column_name
        from information_schema.columns
        where table_schema = 'public'
        order by table_name, ordinal_position
        """;
    List<Map<String, Object>> rows = isDockerMode(config) ? executeDockerQuery(config, sql) : executeJdbcQuery(config, sql);
    Map<String, List<String>> columnsByTable = new HashMap<>();
    for (Map<String, Object> row : rows) {
      String tableName = String.valueOf(row.get("table_name"));
      String columnName = String.valueOf(row.get("column_name"));
      columnsByTable.computeIfAbsent(tableName, ignored -> new ArrayList<>()).add(columnName);
    }

    Map<String, String> resolved = new HashMap<>();
    for (Map.Entry<String, List<String>> entry : columnsByTable.entrySet()) {
      String updatedAtColumn = resolveUpdatedAtColumn(entry.getValue());
      if (updatedAtColumn != null) {
        resolved.put(entry.getKey(), updatedAtColumn);
      }
    }
    return resolved;
  }

  String resolveUpdatedAtColumn(List<String> columnNames) {
    return resolveCandidate(columnNames, UPDATED_AT_CANDIDATES)
        .orElseGet(() -> resolveCandidate(columnNames, CREATED_AT_CANDIDATES).orElse(null));
  }

  private java.util.Optional<String> resolveCandidate(List<String> columnNames, List<String> candidates) {
    return columnNames.stream()
        .map(columnName -> Map.entry(columnName, normalizeColumnName(columnName)))
        .filter(entry -> candidates.contains(entry.getValue()))
        .min(Comparator.comparingInt(entry -> candidates.indexOf(entry.getValue())))
        .map(Map.Entry::getKey);
  }

  private String normalizeColumnName(String columnName) {
    return columnName == null ? "" : columnName.replaceAll("[^A-Za-z0-9]", "").toLowerCase();
  }

  private String quoteIdentifier(String identifier) {
    return "\"" + identifier.replace("\"", "\"\"") + "\"";
  }

  private String toSqlLiteral(Object value) {
    if (value instanceof Number || value instanceof Boolean) {
      return String.valueOf(value);
    }
    return "'" + String.valueOf(value).replace("'", "''") + "'";
  }

  public String buildRecordKey(TableWhitelistOption option, Map<String, Object> row) {
    String[] primaryKeys = option.primaryKey().split(",");
    List<String> values = new ArrayList<>();
    for (String primaryKey : primaryKeys) {
      Object value = row.get(primaryKey.trim());
      values.add(value == null ? "null" : String.valueOf(value));
    }
    return String.join("::", values);
  }

  public LocalDateTime extractUpdatedAt(TableWhitelistOption option, Map<String, Object> row) {
    if (option.updatedAtColumn() == null || option.updatedAtColumn().isBlank()) {
      return null;
    }
    Object value = row.get(option.updatedAtColumn());
    if (value == null) {
      return null;
    }
    if (value instanceof Timestamp timestamp) {
      return timestamp.toLocalDateTime();
    }
    if (value instanceof java.util.Date date) {
      return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }
    if (value instanceof String text) {
      return parseDateTime(text);
    }
    return null;
  }

  private boolean isDockerMode(GitlabSyncConfig config) {
    return config.getSourceMode() == SourceMode.DOCKER;
  }

  private List<Map<String, Object>> executeJdbcQuery(GitlabSyncConfig config, String sql) {
    try (Connection connection = openConnection(config);
         Statement statement = connection.createStatement();
         ResultSet resultSet = statement.executeQuery(sql)) {
      List<Map<String, Object>> rows = new ArrayList<>();
      ResultSetMetaData metaData = resultSet.getMetaData();
      int count = metaData.getColumnCount();
      while (resultSet.next()) {
        Map<String, Object> row = new LinkedHashMap<>();
        for (int i = 1; i <= count; i++) {
          row.put(metaData.getColumnLabel(i), resultSet.getObject(i));
        }
        rows.add(row);
      }
      return rows;
    } catch (Exception e) {
      try (GitlabSyncLogContext.Scope action = GitlabSyncLogContext.action("Data_Fetching")) {
        log.error("Failed to query GitLab database via JDBC", e);
      }
      throw new BizException("Failed to query GitLab database: " + e.getMessage());
    }
  }

  private List<Map<String, Object>> executeDockerQuery(GitlabSyncConfig config, String sql) {
    try {
      List<String> lines = executeDockerSql(config, "select row_to_json(t)::text from (%s) t".formatted(sql));
      List<Map<String, Object>> rows = new ArrayList<>();
      for (String line : lines) {
        if (line == null || line.isBlank()) {
          continue;
        }
        if (line.startsWith("ERROR:") || line.startsWith("FATAL:")) {
          throw new BizException(line);
        }
        rows.add(objectMapper.readValue(line, MAP_TYPE));
      }
      return rows;
    } catch (BizException e) {
      try (GitlabSyncLogContext.Scope action = GitlabSyncLogContext.action("Data_Fetching")) {
        log.error("Failed to query GitLab database via Docker", e);
      }
      throw e;
    } catch (Exception e) {
      try (GitlabSyncLogContext.Scope action = GitlabSyncLogContext.action("Data_Fetching")) {
        log.error("Failed to query GitLab database via Docker", e);
      }
      throw new BizException("Failed to query GitLab database via Docker: " + e.getMessage());
    }
  }

  private List<String> executeDockerSql(GitlabSyncConfig config, String sql) {
    String containerName = config.getDockerContainerName();
    if (containerName == null || containerName.isBlank()) {
      throw new BizException("Docker mode requires a container name");
    }

    String script = """
        gitlab-psql -d "%s" -At <<'SQL'
        %s;
        SQL
        """.formatted(
        sanitizeShell(normalizeDbName(config)),
        sql);

    try {
      ProcessBuilder builder = new ProcessBuilder(
          properties.getDockerCommand(),
          "exec",
          containerName,
          "bash",
          "-lc",
          script);
      builder.redirectErrorStream(true);
      Process process = builder.start();
      List<String> lines = new ArrayList<>();
      try (BufferedReader reader = new BufferedReader(
          new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
        String line;
        while ((line = reader.readLine()) != null) {
          lines.add(line);
        }
      }
      int exitCode = process.waitFor();
      if (exitCode != 0) {
        throw new BizException("Docker GitLab PostgreSQL command failed: " + String.join(System.lineSeparator(), lines));
      }
      return lines;
    } catch (BizException e) {
      try (GitlabSyncLogContext.Scope action = GitlabSyncLogContext.action("Data_Fetching")) {
        log.error("Docker GitLab PostgreSQL command failed", e);
      }
      throw e;
    } catch (Exception e) {
      try (GitlabSyncLogContext.Scope action = GitlabSyncLogContext.action("Data_Fetching")) {
        log.error("Docker GitLab PostgreSQL command failed", e);
      }
      throw new BizException("Docker GitLab PostgreSQL command failed: " + e.getMessage());
    }
  }

  private Connection openConnection(GitlabSyncConfig config) throws Exception {
    String url = "jdbc:postgresql://%s:%d/%s".formatted(config.getDbHost(), config.getDbPort(), normalizeDbName(config));
    return DriverManager.getConnection(url, normalizeDbUser(config), config.getDbPassword());
  }

  private String normalizeDbName(GitlabSyncConfig config) {
    return config.getDbName() == null || config.getDbName().isBlank() ? "gitlabhq_production" : config.getDbName().trim();
  }

  private String normalizeDbUser(GitlabSyncConfig config) {
    return config.getDbUsername() == null || config.getDbUsername().isBlank() ? "gitlab" : config.getDbUsername().trim();
  }

  private String sanitizeShell(String text) {
    return text.replace("\"", "\\\"");
  }

  private LocalDateTime parseDateTime(String text) {
    try {
      return LocalDateTime.parse(text);
    } catch (Exception ignored) {
    }
    try {
      return LocalDateTime.parse(text.replace(" ", "T"));
    } catch (Exception ignored) {
    }
    try {
      return OffsetDateTime.parse(text).toLocalDateTime();
    } catch (Exception ignored) {
    }
    return null;
  }

  private LocalDateTime toGitlabSourceTime(LocalDateTime localTime) {
    return localTime.atZone(ZoneId.systemDefault())
        .withZoneSameInstant(ZoneOffset.UTC)
        .toLocalDateTime();
  }
}
