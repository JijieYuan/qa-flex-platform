package com.data.collection.platform.service;

import com.data.collection.platform.common.JsonUtils;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
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

  public void upsertBatch(Long configId, String tableName, List<MirrorRow> rows) {
    if (rows == null || rows.isEmpty()) {
      return;
    }
    jdbcTemplate.batchUpdate("""
            insert into gitlab_mirror_records(config_id, table_name, record_key, updated_at_source, row_data, synced_at, created_at)
            values (?, ?, ?, ?, cast(? as jsonb), current_timestamp, current_timestamp)
            on conflict (config_id, table_name, record_key)
            do update set updated_at_source = excluded.updated_at_source,
                          row_data = excluded.row_data,
                          synced_at = current_timestamp
            """,
        rows,
        rows.size(),
        (PreparedStatement ps, MirrorRow row) -> {
          ps.setLong(1, configId);
          ps.setString(2, tableName);
          ps.setString(3, row.recordKey());
          if (row.updatedAtSource() == null) {
            ps.setTimestamp(4, null);
          } else {
            ps.setTimestamp(4, Timestamp.valueOf(row.updatedAtSource()));
          }
          ps.setString(5, jsonUtils.toJson(row.rowData()));
        });
  }

  public record MirrorRow(String recordKey, LocalDateTime updatedAtSource, Map<String, Object> rowData) {
  }
}
