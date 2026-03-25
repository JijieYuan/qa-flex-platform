package com.data.collection.platform.service;

import com.data.collection.platform.common.JsonUtils;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class GitlabMirrorRepository {
  private final JdbcTemplate jdbcTemplate;
  private final JsonUtils jsonUtils;

  public GitlabMirrorRepository(JdbcTemplate jdbcTemplate, JsonUtils jsonUtils) {
    this.jdbcTemplate = jdbcTemplate;
    this.jsonUtils = jsonUtils;
  }

  public void upsert(Long configId, String tableName, String recordKey, LocalDateTime updatedAtSource, Map<String, Object> rowData) {
    jdbcTemplate.update("""
        insert into gitlab_mirror_records(config_id, table_name, record_key, updated_at_source, row_data, synced_at, created_at)
        values (?, ?, ?, ?, cast(? as jsonb), current_timestamp, current_timestamp)
        on conflict (config_id, table_name, record_key)
        do update set updated_at_source = excluded.updated_at_source,
                      row_data = excluded.row_data,
                      synced_at = current_timestamp
        """,
        configId,
        tableName,
        recordKey,
        updatedAtSource == null ? null : Timestamp.valueOf(updatedAtSource),
        jsonUtils.toJson(rowData));
  }
}
