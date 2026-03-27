package com.data.collection.platform.service;

import com.data.collection.platform.common.JsonUtils;
import com.data.collection.platform.entity.SourceTableColumn;
import com.data.collection.platform.entity.SourceTableSchema;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class GitlabMirrorTableStorageService {

  private final JdbcTemplate jdbcTemplate;
  private final JsonUtils jsonUtils;

  public GitlabMirrorTableStorageService(JdbcTemplate jdbcTemplate, JsonUtils jsonUtils) {
    this.jdbcTemplate = jdbcTemplate;
    this.jsonUtils = jsonUtils;
  }

  public void upsertBatch(SourceTableSchema mirrorSchema, List<Map<String, Object>> rows, Long taskId) {
    if (rows == null || rows.isEmpty()) {
      return;
    }
    String sql = buildUpsertSql(mirrorSchema);
    jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
      @Override
      public void setValues(PreparedStatement ps, int i) throws SQLException {
        ps.setLong(1, taskId);
        ps.setString(2, jsonUtils.toJson(rows.get(i)));
      }

      @Override
      public int getBatchSize() {
        return rows.size();
      }
    });
  }

  private String buildUpsertSql(SourceTableSchema schema) {
    String tableName = quoteIdentifier(schema.tableName());
    List<String> sourceColumns = schema.columns().stream().map(SourceTableColumn::columnName).toList();
    String insertColumns = sourceColumns.stream().map(this::quoteIdentifier).collect(Collectors.joining(", "));
    String selectColumns = sourceColumns.stream().map(column -> "p." + quoteIdentifier(column)).collect(Collectors.joining(", "));
    String conflictColumns = schema.primaryKeys().stream().map(this::quoteIdentifier).collect(Collectors.joining(", "));
    String updateAssignments = sourceColumns.stream()
        .map(column -> quoteIdentifier(column) + " = excluded." + quoteIdentifier(column))
        .collect(Collectors.joining(", "));
    String sourceUpdatedExpression = schema.updatedAtColumn() == null || schema.updatedAtColumn().isBlank()
        ? "null"
        : "p." + quoteIdentifier(schema.updatedAtColumn());
    return """
        insert into %s (%s, mirror_task_id, source_updated_at, mirror_synced_at, mirror_deleted, mirror_updated_at)
        select %s, ?, %s, current_timestamp, false, current_timestamp
        from jsonb_populate_record(null::%s, cast(? as jsonb)) as p
        on conflict (%s) do update
        set %s,
            mirror_task_id = excluded.mirror_task_id,
            source_updated_at = excluded.source_updated_at,
            mirror_synced_at = current_timestamp,
            mirror_deleted = false,
            mirror_updated_at = current_timestamp
        """.formatted(
        tableName,
        insertColumns,
        selectColumns,
        sourceUpdatedExpression,
        tableName,
        conflictColumns,
        updateAssignments);
  }

  private String quoteIdentifier(String identifier) {
    return "\"" + identifier.replace("\"", "\"\"") + "\"";
  }
}
