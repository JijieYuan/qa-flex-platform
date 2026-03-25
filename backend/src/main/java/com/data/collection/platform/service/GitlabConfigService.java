package com.data.collection.platform.service;

import com.data.collection.platform.common.JsonUtils;
import com.data.collection.platform.entity.GitlabSyncConfig;
import com.data.collection.platform.entity.SourceMode;
import com.data.collection.platform.entity.WhitelistMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Service
public class GitlabConfigService {
  private final JdbcTemplate jdbcTemplate;
  private final JsonUtils jsonUtils;

  public GitlabConfigService(JdbcTemplate jdbcTemplate, JsonUtils jsonUtils) {
    this.jdbcTemplate = jdbcTemplate;
    this.jsonUtils = jsonUtils;
  }

  public GitlabSyncConfig getConfig() {
    List<GitlabSyncConfig> configs = jdbcTemplate.query("select * from gitlab_sync_configs order by id asc limit 1", rowMapper());
    if (configs.isEmpty()) {
      return defaultConfig();
    }
    return configs.get(0);
  }

  public GitlabSyncConfig saveConfig(GitlabSyncConfig input) {
    GitlabSyncConfig current = getConfig();
    if (current.getId() == null) {
      jdbcTemplate.update("""
          insert into gitlab_sync_configs (
              name, enabled, auto_sync_enabled, source_mode, whitelist_mode, whitelist_tables,
              db_host, db_port, db_name, db_username, db_password,
              docker_container_name, webhook_secret, webhook_project_id, compensation_interval_minutes, updated_at
          ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, current_timestamp)
          """,
          input.getName(), input.isEnabled(), input.isAutoSyncEnabled(), normalizeSourceMode(input).name(), input.getWhitelistMode().name(),
          jsonUtils.toJson(input.getWhitelistTables()), input.getDbHost(), input.getDbPort(), input.getDbName(),
          input.getDbUsername(), input.getDbPassword(), input.getDockerContainerName(), input.getWebhookSecret(), input.getWebhookProjectId(),
          input.getCompensationIntervalMinutes());
    } else {
      jdbcTemplate.update("""
          update gitlab_sync_configs
          set name = ?, enabled = ?, auto_sync_enabled = ?, source_mode = ?, whitelist_mode = ?, whitelist_tables = ?,
              db_host = ?, db_port = ?, db_name = ?, db_username = ?, db_password = ?,
              docker_container_name = ?, webhook_secret = ?, webhook_project_id = ?, compensation_interval_minutes = ?, updated_at = current_timestamp
          where id = ?
          """,
          input.getName(), input.isEnabled(), input.isAutoSyncEnabled(), normalizeSourceMode(input).name(), input.getWhitelistMode().name(),
          jsonUtils.toJson(input.getWhitelistTables()), input.getDbHost(), input.getDbPort(), input.getDbName(),
          input.getDbUsername(), input.getDbPassword(), input.getDockerContainerName(), input.getWebhookSecret(), input.getWebhookProjectId(),
          input.getCompensationIntervalMinutes(), current.getId());
    }
    return getConfig();
  }

  public void updateSyncTime(Long id, boolean fullSync) {
    jdbcTemplate.update(fullSync
        ? "update gitlab_sync_configs set last_full_sync_at = current_timestamp, updated_at = current_timestamp where id = ?"
        : "update gitlab_sync_configs set last_incremental_sync_at = current_timestamp, updated_at = current_timestamp where id = ?",
        id);
  }

  private GitlabSyncConfig defaultConfig() {
    GitlabSyncConfig config = new GitlabSyncConfig();
    config.setName("GitLab 默认数据源");
    config.setEnabled(true);
    config.setAutoSyncEnabled(true);
    config.setSourceMode(SourceMode.DOCKER);
    config.setWhitelistMode(WhitelistMode.RECOMMENDED);
    config.setWhitelistTables(List.of());
    config.setDbHost("localhost");
    config.setDbPort(5432);
    config.setDbName("gitlabhq_production");
    config.setDbUsername("gitlab");
    config.setDbPassword("");
    config.setDockerContainerName("gitlab-data-web-1");
    config.setCompensationIntervalMinutes(10);
    return config;
  }

  private RowMapper<GitlabSyncConfig> rowMapper() {
    return (rs, rowNum) -> {
      GitlabSyncConfig config = new GitlabSyncConfig();
      config.setId(rs.getLong("id"));
      config.setName(rs.getString("name"));
      config.setEnabled(rs.getBoolean("enabled"));
      config.setAutoSyncEnabled(rs.getBoolean("auto_sync_enabled"));
      String sourceMode = rs.getString("source_mode");
      config.setSourceMode(sourceMode == null || sourceMode.isBlank() ? SourceMode.DOCKER : SourceMode.valueOf(sourceMode));
      config.setWhitelistMode(WhitelistMode.valueOf(rs.getString("whitelist_mode")));
      config.setWhitelistTables(jsonUtils.toStringList(rs.getString("whitelist_tables")));
      config.setDbHost(rs.getString("db_host"));
      config.setDbPort(rs.getInt("db_port"));
      config.setDbName(rs.getString("db_name"));
      config.setDbUsername(rs.getString("db_username"));
      config.setDbPassword(rs.getString("db_password"));
      config.setDockerContainerName(rs.getString("docker_container_name"));
      config.setWebhookSecret(rs.getString("webhook_secret"));
      config.setWebhookProjectId((Long) rs.getObject("webhook_project_id"));
      config.setCompensationIntervalMinutes(rs.getInt("compensation_interval_minutes"));
      config.setLastFullSyncAt(toLocalDateTime(rs, "last_full_sync_at"));
      config.setLastIncrementalSyncAt(toLocalDateTime(rs, "last_incremental_sync_at"));
      config.setCreatedAt(toLocalDateTime(rs, "created_at"));
      config.setUpdatedAt(toLocalDateTime(rs, "updated_at"));
      return config;
    };
  }

  private LocalDateTime toLocalDateTime(ResultSet rs, String column) throws SQLException {
    Timestamp timestamp = rs.getTimestamp(column);
    return timestamp == null ? null : timestamp.toLocalDateTime();
  }

  private SourceMode normalizeSourceMode(GitlabSyncConfig config) {
    return config.getSourceMode() == null ? SourceMode.DOCKER : config.getSourceMode();
  }
}
