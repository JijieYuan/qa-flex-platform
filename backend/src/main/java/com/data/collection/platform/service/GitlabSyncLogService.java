package com.data.collection.platform.service;

import com.data.collection.platform.common.JsonUtils;
import com.data.collection.platform.entity.GitlabSyncLog;
import com.data.collection.platform.entity.SyncStatus;
import com.data.collection.platform.entity.SyncType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class GitlabSyncLogService {
  private final JdbcTemplate jdbcTemplate;
  private final JsonUtils jsonUtils;

  public GitlabSyncLogService(JdbcTemplate jdbcTemplate, JsonUtils jsonUtils) {
    this.jdbcTemplate = jdbcTemplate;
    this.jsonUtils = jsonUtils;
  }

  public long start(Long configId, SyncType syncType, List<String> whitelistSnapshot, String message) {
    return jdbcTemplate.queryForObject("""
        insert into gitlab_sync_logs(config_id, sync_type, status, message, whitelist_snapshot, table_count, record_count, started_at)
        values (?, ?, ?, ?, ?, 0, 0, current_timestamp)
        returning id
        """,
        Long.class,
        configId,
        syncType.name(),
        SyncStatus.RUNNING.name(),
        message,
        jsonUtils.toJson(whitelistSnapshot));
  }

  public void finish(long id, SyncStatus status, String message, int tableCount, int recordCount) {
    jdbcTemplate.update("""
        update gitlab_sync_logs
        set status = ?, message = ?, table_count = ?, record_count = ?, finished_at = current_timestamp
        where id = ?
        """, status.name(), message, tableCount, recordCount, id);
  }

  public List<GitlabSyncLog> listRecent(Long configId, int limit) {
    return jdbcTemplate.query("""
        select * from gitlab_sync_logs
        where config_id = ?
        order by started_at desc
        limit ?
        """, (rs, rowNum) -> map(rs), configId, limit);
  }

  public GitlabSyncLog findRunning(Long configId) {
    List<GitlabSyncLog> logs = jdbcTemplate.query("""
        select * from gitlab_sync_logs
        where config_id = ? and status = 'RUNNING'
        order by started_at desc
        limit 1
        """, (rs, rowNum) -> map(rs), configId);
    return logs.isEmpty() ? null : logs.get(0);
  }

  public GitlabSyncLog findLatest(Long configId) {
    List<GitlabSyncLog> logs = jdbcTemplate.query("""
        select * from gitlab_sync_logs
        where config_id = ?
        order by started_at desc
        limit 1
        """, (rs, rowNum) -> map(rs), configId);
    return logs.isEmpty() ? null : logs.get(0);
  }

  public int markRunningAsFailed(Long configId, String message) {
    return jdbcTemplate.update("""
        update gitlab_sync_logs
        set status = ?, message = ?, finished_at = current_timestamp
        where config_id = ? and status = ?
        """, SyncStatus.FAILED.name(), message, configId, SyncStatus.RUNNING.name());
  }

  private GitlabSyncLog map(ResultSet rs) throws SQLException {
    GitlabSyncLog log = new GitlabSyncLog();
    log.setId(rs.getLong("id"));
    log.setConfigId(rs.getLong("config_id"));
    log.setSyncType(SyncType.valueOf(rs.getString("sync_type")));
    log.setStatus(SyncStatus.valueOf(rs.getString("status")));
    log.setMessage(rs.getString("message"));
    log.setWhitelistSnapshot(rs.getString("whitelist_snapshot"));
    log.setTableCount(rs.getInt("table_count"));
    log.setRecordCount(rs.getInt("record_count"));
    Timestamp startedAt = rs.getTimestamp("started_at");
    Timestamp finishedAt = rs.getTimestamp("finished_at");
    log.setStartedAt(startedAt == null ? null : startedAt.toLocalDateTime());
    log.setFinishedAt(finishedAt == null ? null : finishedAt.toLocalDateTime());
    return log;
  }
}
