package com.data.collection.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.data.collection.platform.common.JsonUtils;
import com.data.collection.platform.common.exception.BizException;
import com.data.collection.platform.entity.GitlabMirrorTableRegistry;
import com.data.collection.platform.entity.GitlabSyncConfig;
import com.data.collection.platform.entity.SourceTableColumn;
import com.data.collection.platform.entity.SourceTableSchema;
import com.data.collection.platform.entity.TableWhitelistOption;
import com.data.collection.platform.mapper.GitlabMirrorTableRegistryMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class GitlabMirrorSchemaService {
  private static final String MIRROR_PREFIX = "ods_gitlab_";
  private static final int POSTGRES_IDENTIFIER_MAX_LENGTH = 63;

  private final GitlabExternalDbService externalDbService;
  private final GitlabMirrorTableRegistryMapper registryMapper;
  private final JdbcTemplate jdbcTemplate;
  private final JsonUtils jsonUtils;

  public GitlabMirrorSchemaService(
      GitlabExternalDbService externalDbService,
      GitlabMirrorTableRegistryMapper registryMapper,
      JdbcTemplate jdbcTemplate,
      JsonUtils jsonUtils) {
    this.externalDbService = externalDbService;
    this.registryMapper = registryMapper;
    this.jdbcTemplate = jdbcTemplate;
    this.jsonUtils = jsonUtils;
  }

  public SourceTableSchema ensureMirrorTable(GitlabSyncConfig config, TableWhitelistOption option) {
    SourceTableSchema schema = externalDbService.discoverTableSchema(config, option);
    String mirrorTableName = buildMirrorTableName(schema.tableName());
    ensureMirrorTableExists(mirrorTableName, schema);
    saveRegistry(config, option, schema, mirrorTableName);
    return new SourceTableSchema(mirrorTableName, schema.primaryKeys(), schema.updatedAtColumn(), schema.columns());
  }

  public List<String> listRegisteredMirrorTables() {
    return registryMapper.selectList(new LambdaQueryWrapper<GitlabMirrorTableRegistry>()
            .orderByAsc(GitlabMirrorTableRegistry::getMirrorTableName))
        .stream()
        .map(GitlabMirrorTableRegistry::getMirrorTableName)
        .toList();
  }

  private void ensureMirrorTableExists(String mirrorTableName, SourceTableSchema schema) {
    jdbcTemplate.execute(buildCreateTableSql(mirrorTableName, schema));
    for (SourceTableColumn column : schema.columns()) {
      jdbcTemplate.execute(
          "alter table " + quoteIdentifier(mirrorTableName)
              + " add column if not exists "
              + quoteIdentifier(column.columnName())
              + " "
              + column.formattedType());
    }
    ensureBaseMetadataColumns(mirrorTableName);
    validateColumnTypes(mirrorTableName, schema);
    ensurePrimaryKeyIndex(mirrorTableName, schema.primaryKeys());
    ensureStandardIndexes(mirrorTableName);
  }

  private String buildCreateTableSql(String mirrorTableName, SourceTableSchema schema) {
    List<String> definitions = new ArrayList<>();
    definitions.add("mirror_id bigserial primary key");
    for (SourceTableColumn column : schema.columns()) {
      StringBuilder builder = new StringBuilder()
          .append(quoteIdentifier(column.columnName()))
          .append(' ')
          .append(column.formattedType());
      if (!column.nullable()) {
        builder.append(" not null");
      }
      definitions.add(builder.toString());
    }
    definitions.add("mirror_task_id bigint");
    definitions.add("source_updated_at timestamp");
    definitions.add("mirror_synced_at timestamp not null default current_timestamp");
    definitions.add("mirror_deleted boolean not null default false");
    definitions.add("mirror_created_at timestamp not null default current_timestamp");
    definitions.add("mirror_updated_at timestamp not null default current_timestamp");
    return "create table if not exists " + quoteIdentifier(mirrorTableName) + " ("
        + String.join(", ", definitions) + ")";
  }

  private void ensureBaseMetadataColumns(String mirrorTableName) {
    jdbcTemplate.execute("alter table " + quoteIdentifier(mirrorTableName) + " add column if not exists mirror_task_id bigint");
    jdbcTemplate.execute("alter table " + quoteIdentifier(mirrorTableName) + " add column if not exists source_updated_at timestamp");
    jdbcTemplate.execute("alter table " + quoteIdentifier(mirrorTableName) + " add column if not exists mirror_synced_at timestamp not null default current_timestamp");
    jdbcTemplate.execute("alter table " + quoteIdentifier(mirrorTableName) + " add column if not exists mirror_deleted boolean not null default false");
    jdbcTemplate.execute("alter table " + quoteIdentifier(mirrorTableName) + " add column if not exists mirror_created_at timestamp not null default current_timestamp");
    jdbcTemplate.execute("alter table " + quoteIdentifier(mirrorTableName) + " add column if not exists mirror_updated_at timestamp not null default current_timestamp");
  }

  private void validateColumnTypes(String mirrorTableName, SourceTableSchema schema) {
    Map<String, String> localTypes = jdbcTemplate.query(
        """
            select a.attname as column_name, pg_catalog.format_type(a.atttypid, a.atttypmod) as formatted_type
            from pg_attribute a
            join pg_class c on a.attrelid = c.oid
            join pg_namespace n on c.relnamespace = n.oid
            where n.nspname = current_schema()
              and c.relname = ?
              and a.attnum > 0
              and not a.attisdropped
            """,
        ps -> ps.setString(1, mirrorTableName),
        rs -> {
          Map<String, String> result = new java.util.LinkedHashMap<>();
          while (rs.next()) {
            result.put(rs.getString("column_name"), rs.getString("formatted_type"));
          }
          return result;
        });
    for (SourceTableColumn column : schema.columns()) {
      String localType = localTypes.get(column.columnName());
      if (localType != null && !normalizeType(localType).equals(normalizeType(column.formattedType()))) {
        throw new BizException("镜像表字段类型冲突: %s.%s 本地=%s, 源端=%s".formatted(
            mirrorTableName, column.columnName(), localType, column.formattedType()));
      }
    }
  }

  private void ensurePrimaryKeyIndex(String mirrorTableName, List<String> primaryKeys) {
    if (primaryKeys == null || primaryKeys.isEmpty()) {
      return;
    }
    String indexName = abbreviateIdentifier("uq_" + mirrorTableName + "_pk");
    String columns = primaryKeys.stream().map(this::quoteIdentifier).collect(Collectors.joining(", "));
    jdbcTemplate.execute(
        "create unique index if not exists " + quoteIdentifier(indexName) + " on " + quoteIdentifier(mirrorTableName) + " (" + columns + ")");
  }

  private void ensureStandardIndexes(String mirrorTableName) {
    jdbcTemplate.execute(
        "create index if not exists " + quoteIdentifier(abbreviateIdentifier("idx_" + mirrorTableName + "_source_updated"))
            + " on " + quoteIdentifier(mirrorTableName) + " (source_updated_at)");
    jdbcTemplate.execute(
        "create index if not exists " + quoteIdentifier(abbreviateIdentifier("idx_" + mirrorTableName + "_mirror_synced"))
            + " on " + quoteIdentifier(mirrorTableName) + " (mirror_synced_at)");
  }

  private void saveRegistry(GitlabSyncConfig config, TableWhitelistOption option, SourceTableSchema schema, String mirrorTableName) {
    GitlabMirrorTableRegistry current = registryMapper.selectOne(
        new LambdaQueryWrapper<GitlabMirrorTableRegistry>()
            .eq(GitlabMirrorTableRegistry::getConfigId, config.getId())
            .eq(GitlabMirrorTableRegistry::getSourceTableName, option.tableName())
            .last("limit 1"));
    LocalDateTime now = LocalDateTime.now();
    GitlabMirrorTableRegistry registry = current == null ? new GitlabMirrorTableRegistry() : current;
    registry.setConfigId(config.getId());
    registry.setSourceTableName(option.tableName());
    registry.setMirrorTableName(mirrorTableName);
    registry.setPrimaryKeyColumns(String.join(",", schema.primaryKeys()));
    registry.setUpdatedAtColumn(schema.updatedAtColumn());
    registry.setColumnSnapshot(jsonUtils.toJson(schema.columns()));
    registry.setLastSchemaSyncedAt(now);
    registry.setUpdatedAt(now);
    if (current == null) {
      registry.setCreatedAt(now);
      jdbcTemplate.update(
          """
              insert into gitlab_mirror_table_registry(
                config_id,
                source_table_name,
                mirror_table_name,
                primary_key_columns,
                updated_at_column,
                column_snapshot,
                last_schema_synced_at,
                created_at,
                updated_at
              ) values (?, ?, ?, ?, ?, cast(? as jsonb), ?, ?, ?)
              """,
          registry.getConfigId(),
          registry.getSourceTableName(),
          registry.getMirrorTableName(),
          registry.getPrimaryKeyColumns(),
          registry.getUpdatedAtColumn(),
          registry.getColumnSnapshot(),
          registry.getLastSchemaSyncedAt(),
          registry.getCreatedAt(),
          registry.getUpdatedAt());
    } else {
      jdbcTemplate.update(
          """
              update gitlab_mirror_table_registry
              set mirror_table_name = ?,
                  primary_key_columns = ?,
                  updated_at_column = ?,
                  column_snapshot = cast(? as jsonb),
                  last_schema_synced_at = ?,
                  updated_at = ?
              where id = ?
              """,
          registry.getMirrorTableName(),
          registry.getPrimaryKeyColumns(),
          registry.getUpdatedAtColumn(),
          registry.getColumnSnapshot(),
          registry.getLastSchemaSyncedAt(),
          registry.getUpdatedAt(),
          registry.getId());
    }
  }

  private String buildMirrorTableName(String sourceTableName) {
    String normalized = sourceTableName.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
    String candidate = MIRROR_PREFIX + normalized;
    if (candidate.length() <= POSTGRES_IDENTIFIER_MAX_LENGTH) {
      return candidate;
    }
    String hash = shortHash(sourceTableName);
    int keepLength = POSTGRES_IDENTIFIER_MAX_LENGTH - MIRROR_PREFIX.length() - hash.length() - 1;
    return MIRROR_PREFIX + normalized.substring(0, Math.max(8, keepLength)) + "_" + hash;
  }

  private String shortHash(String text) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash).substring(0, 10);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to hash mirror table name", e);
    }
  }

  private String quoteIdentifier(String identifier) {
    return "\"" + identifier.replace("\"", "\"\"") + "\"";
  }

  private String abbreviateIdentifier(String identifier) {
    if (identifier.length() <= POSTGRES_IDENTIFIER_MAX_LENGTH) {
      return identifier;
    }
    return identifier.substring(0, POSTGRES_IDENTIFIER_MAX_LENGTH - 11) + "_" + shortHash(identifier);
  }

  private String normalizeType(String type) {
    return type == null ? "" : type.replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
  }
}
