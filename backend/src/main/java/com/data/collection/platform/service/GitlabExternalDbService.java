package com.data.collection.platform.service;

import com.data.collection.platform.common.exception.BizException;
import com.data.collection.platform.config.GitlabMirrorProperties;
import com.data.collection.platform.entity.GitlabSyncConfig;
import com.data.collection.platform.entity.SourceMode;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class GitlabExternalDbService {
  private static final TypeReference<LinkedHashMap<String, Object>> MAP_TYPE = new TypeReference<>() {};

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
      throw new BizException("GitLab PostgreSQL connection failed: " + e.getMessage());
    }
  }

  public List<Map<String, Object>> fullTableScan(GitlabSyncConfig config, TableWhitelistOption option) {
    String sql = "select * from public.%s".formatted(option.tableName());
    return isDockerMode(config) ? executeDockerQuery(config, sql) : executeJdbcQuery(config, sql);
  }

  public List<Map<String, Object>> incrementalScan(GitlabSyncConfig config, TableWhitelistOption option, LocalDateTime since) {
    if (since == null || option.updatedAtColumn() == null || option.updatedAtColumn().isBlank()) {
      return fullTableScan(config, option);
    }
    String sql = "select * from public.%s where %s >= timestamp '%s'".formatted(
        option.tableName(),
        option.updatedAtColumn(),
        since.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    return isDockerMode(config) ? executeDockerQuery(config, sql) : executeJdbcQuery(config, sql);
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
        rows.add(objectMapper.readValue(line, MAP_TYPE));
      }
      return rows;
    } catch (BizException e) {
      throw e;
    } catch (Exception e) {
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
      throw e;
    } catch (Exception e) {
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
}
