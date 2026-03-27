package com.data.collection.platform.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectProvider;

public interface MirrorTableOverviewMapper {
  @Select("""
      select table_name
      from gitlab_mirror_records
      group by table_name
      order by count(*) desc, table_name asc
      """)
  List<String> selectTableNames();

  @SelectProvider(type = SqlProvider.class, method = "buildSummaryRowsSql")
  List<Map<String, Object>> selectSummaryRows(@Param("tableName") String tableName);

  @SelectProvider(type = SqlProvider.class, method = "buildCountDetailsSql")
  Long countDetails(@Param("tableName") String tableName, @Param("columnKey") String columnKey);

  @SelectProvider(type = SqlProvider.class, method = "buildSelectDetailsSql")
  List<Map<String, Object>> selectDetails(
      @Param("tableName") String tableName,
      @Param("columnKey") String columnKey,
      @Param("sortField") String sortField,
      @Param("sortOrder") String sortOrder,
      @Param("size") int size,
      @Param("offset") int offset);

  final class SqlProvider {
    private SqlProvider() {
    }

    public static String buildSummaryRowsSql(Map<String, Object> params) {
      String tableName = (String) params.get("tableName");
      StringBuilder sql = new StringBuilder("""
          select
            table_name as "tableName",
            count(*) as "totalRecords",
            count(updated_at_source) as "withSourceUpdate",
            count(*) - count(updated_at_source) as "withoutSourceUpdate",
            count(*) filter (where synced_at >= current_timestamp - interval '24 hours') as "syncedIn24h",
            count(*) filter (where synced_at < current_timestamp - interval '24 hours') as "staleSync",
            max(synced_at) as "lastSyncedAt",
            max(updated_at_source) as "lastSourceUpdatedAt"
          from gitlab_mirror_records
          where 1 = 1
          """);
      if (tableName != null && !tableName.isBlank()) {
        sql.append(" and table_name = #{tableName} ");
      }
      sql.append("""
          group by table_name
          order by count(*) desc, table_name asc
          """);
      return sql.toString();
    }

    public static String buildCountDetailsSql(Map<String, Object> params) {
      return new StringBuilder("select count(*) from gitlab_mirror_records where table_name = #{tableName} ")
          .append(detailWhereClause((String) params.get("columnKey")))
          .toString();
    }

    public static String buildSelectDetailsSql(Map<String, Object> params) {
      return new StringBuilder("""
          select
            id as "id",
            table_name as "tableName",
            record_key as "recordKey",
            updated_at_source as "updatedAtSource",
            synced_at as "syncedAt",
            row_data::text as "rowData"
          from gitlab_mirror_records
          where table_name = #{tableName}
          """)
          .append(detailWhereClause((String) params.get("columnKey")))
          .append(" order by ")
          .append(params.get("sortField"))
          .append(" ")
          .append(params.get("sortOrder"))
          .append(" nulls last limit #{size} offset #{offset}")
          .toString();
    }

    private static String detailWhereClause(String columnKey) {
      return switch (columnKey) {
        case "totalRecords" -> "";
        case "withSourceUpdate" -> " and updated_at_source is not null ";
        case "withoutSourceUpdate" -> " and updated_at_source is null ";
        case "syncedIn24h" -> " and synced_at >= current_timestamp - interval '24 hours' ";
        case "staleSync" -> " and synced_at < current_timestamp - interval '24 hours' ";
        default -> throw new IllegalArgumentException("Unsupported statistic column: " + columnKey);
      };
    }
  }
}
