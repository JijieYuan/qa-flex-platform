package com.data.collection.platform.service.statistics;

import com.data.collection.platform.common.exception.BizException;
import com.data.collection.platform.entity.statistics.StatisticBoardDefinition;
import com.data.collection.platform.entity.statistics.StatisticBoardResponse;
import com.data.collection.platform.entity.statistics.StatisticCellData;
import com.data.collection.platform.entity.statistics.StatisticColumnGroup;
import com.data.collection.platform.entity.statistics.StatisticColumnLeaf;
import com.data.collection.platform.entity.statistics.StatisticDetailColumn;
import com.data.collection.platform.entity.statistics.StatisticDetailRequest;
import com.data.collection.platform.entity.statistics.StatisticDetailResponse;
import com.data.collection.platform.entity.statistics.StatisticFilterField;
import com.data.collection.platform.entity.statistics.StatisticFilterOption;
import com.data.collection.platform.entity.statistics.StatisticRowData;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class MirrorTableOverviewBoardService extends AbstractStatisticBoardService {
  private static final String BOARD_KEY = "mirror-table-overview";
  private static final String DETAIL_VIEW_KEY = "mirror-record-list";
  private static final List<StatisticDetailColumn> DETAIL_COLUMNS =
      List.of(
          new StatisticDetailColumn("id", "镜像 ID", 96, null, true),
          new StatisticDetailColumn("tableName", "表名", 180, null, true),
          new StatisticDetailColumn("recordKey", "记录键", 220, null, true),
          new StatisticDetailColumn("updatedAtSource", "源更新时间", 180, null, true),
          new StatisticDetailColumn("syncedAt", "同步时间", 180, null, true),
          new StatisticDetailColumn("rowData", "原始数据", null, 360, false));
  private static final Map<String, String> SORT_FIELD_MAPPING =
      Map.of(
          "id", "id",
          "tableName", "table_name",
          "recordKey", "record_key",
          "updatedAtSource", "updated_at_source",
          "syncedAt", "synced_at");

  private final JdbcTemplate jdbcTemplate;

  public MirrorTableOverviewBoardService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public String boardKey() {
    return BOARD_KEY;
  }

  @Override
  protected StatisticBoardDefinition buildDefinition() {
    return new StatisticBoardDefinition(
        BOARD_KEY,
        "GitLab 镜像表基础统计",
        "基于已同步到本地的镜像记录，对各张 GitLab 源表的记录规模和同步覆盖情况进行汇总展示。",
        "查询与操作",
        "按表名关键字和展示数量过滤当前统计范围。本页只使用镜像表中的标准字段，不引入任何额外业务规则。",
        List.of(
            new StatisticFilterField(
                "tableKeyword",
                "表名关键字",
                "text",
                "输入表名关键字",
                "",
                220,
                List.of()),
            new StatisticFilterField(
                "limit",
                "展示数量",
                "select",
                "",
                "20",
                140,
                List.of(
                    new StatisticFilterOption("10 张", "10"),
                    new StatisticFilterOption("20 张", "20"),
                    new StatisticFilterOption("50 张", "50"),
                    new StatisticFilterOption("100 张", "100")))),
        List.of(
            new StatisticColumnGroup(
                "scale",
                "记录规模",
                List.of(new StatisticColumnLeaf("totalRecords", "总记录数", true, "count"))),
            new StatisticColumnGroup(
                "sourceTime",
                "源更新时间",
                List.of(
                    new StatisticColumnLeaf("withSourceUpdate", "有源更新时间", true, "count"),
                    new StatisticColumnLeaf("withoutSourceUpdate", "缺少源更新时间", true, "count"))),
            new StatisticColumnGroup(
                "syncStatus",
                "同步时效",
                List.of(
                    new StatisticColumnLeaf("syncedIn24h", "24 小时内同步", true, "count"),
                    new StatisticColumnLeaf("staleSync", "24 小时前同步", true, "count")))),
        DETAIL_COLUMNS,
        10,
        "当前筛选条件下没有可展示的镜像统计结果。");
  }

  @Override
  protected StatisticBoardResponse doLoadBoard(Map<String, String> filters) {
    String keyword = trimToNull(filters.get("tableKeyword"));
    int limit = parsePositiveInt(filters.get("limit"), 20);
    List<Map<String, Object>> rows = querySummaryRows(keyword, limit);
    List<StatisticRowData> boardRows = rows.stream().map(this::toStatisticRow).toList();
    Map<String, String> appliedFilters = new LinkedHashMap<>();
    appliedFilters.put("tableKeyword", keyword == null ? "" : keyword);
    appliedFilters.put("limit", String.valueOf(limit));
    return new StatisticBoardResponse(buildDefinition(), appliedFilters, boardRows);
  }

  @Override
  protected StatisticDetailResponse doLoadDetail(StatisticDetailRequest request) {
    String tableName = trimToNull(request.rowKey());
    if (tableName == null) {
      throw new BizException("明细查询缺少行维度标识");
    }
    String columnKey = trimToNull(request.columnKey());
    if (columnKey == null) {
      throw new BizException("明细查询缺少列维度标识");
    }
    int page = request.page() <= 0 ? 1 : request.page();
    int size = request.size() <= 0 ? 10 : request.size();
    String sortField = SORT_FIELD_MAPPING.getOrDefault(request.sortField(), "synced_at");
    String sortOrder = "ascending".equalsIgnoreCase(request.sortOrder()) ? "asc" : "desc";

    StringBuilder where = new StringBuilder(" where table_name = ? ");
    List<Object> args = new ArrayList<>();
    args.add(tableName);
    appendColumnFilter(where, args, columnKey);

    long total =
        jdbcTemplate.queryForObject(
            "select count(*) from gitlab_mirror_records" + where, Long.class, args.toArray());

    List<Object> detailArgs = new ArrayList<>(args);
    detailArgs.add(size);
    detailArgs.add((page - 1) * size);
    List<Map<String, Object>> records =
        jdbcTemplate.query(
            "select id, table_name, record_key, updated_at_source, synced_at, row_data::text as row_data "
                + "from gitlab_mirror_records"
                + where
                + " order by "
                + sortField
                + " "
                + sortOrder
                + " nulls last limit ? offset ?",
            detailArgs.toArray(),
            (rs, rowNum) -> {
              Map<String, Object> row = new LinkedHashMap<>();
              row.put("id", rs.getLong("id"));
              row.put("tableName", rs.getString("table_name"));
              row.put("recordKey", rs.getString("record_key"));
              Timestamp updatedAtSource = rs.getTimestamp("updated_at_source");
              row.put(
                  "updatedAtSource",
                  updatedAtSource == null ? null : updatedAtSource.toLocalDateTime());
              Timestamp syncedAt = rs.getTimestamp("synced_at");
              row.put("syncedAt", syncedAt == null ? null : syncedAt.toLocalDateTime());
              row.put("rowData", rs.getString("row_data"));
              return row;
            });

    return new StatisticDetailResponse(
        buildDetailTitle(tableName, columnKey),
        "基于当前统计单元格生成的镜像原始记录列表。",
        DETAIL_COLUMNS,
        records,
        total,
        page,
        size,
        request.sortField() == null ? "syncedAt" : request.sortField(),
        "ascending".equalsIgnoreCase(request.sortOrder()) ? "ascending" : "descending");
  }

  private List<Map<String, Object>> querySummaryRows(String keyword, int limit) {
    StringBuilder sql =
        new StringBuilder(
            """
        select
          table_name,
          count(*) as total_records,
          count(updated_at_source) as with_source_update,
          count(*) - count(updated_at_source) as without_source_update,
          count(*) filter (where synced_at >= current_timestamp - interval '24 hours') as synced_in_24h,
          count(*) filter (where synced_at < current_timestamp - interval '24 hours') as stale_sync
        from gitlab_mirror_records
        where 1 = 1
        """);
    List<Object> args = new ArrayList<>();
    if (keyword != null) {
      sql.append(" and table_name ilike ? ");
      args.add("%" + keyword + "%");
    }
    sql.append(
        """
         group by table_name
         order by count(*) desc, table_name asc
         limit ?
        """);
    args.add(limit);
    return jdbcTemplate.query(
        sql.toString(),
        args.toArray(),
        (rs, rowNum) -> {
          Map<String, Object> row = new LinkedHashMap<>();
          row.put("tableName", rs.getString("table_name"));
          row.put("totalRecords", rs.getLong("total_records"));
          row.put("withSourceUpdate", rs.getLong("with_source_update"));
          row.put("withoutSourceUpdate", rs.getLong("without_source_update"));
          row.put("syncedIn24h", rs.getLong("synced_in_24h"));
          row.put("staleSync", rs.getLong("stale_sync"));
          return row;
        });
  }

  private StatisticRowData toStatisticRow(Map<String, Object> row) {
    String tableName = String.valueOf(row.get("tableName"));
    return new StatisticRowData(
        tableName,
        tableName,
        List.of(
            buildCell(tableName, "totalRecords", "总记录数", row.get("totalRecords")),
            buildCell(tableName, "withSourceUpdate", "有源更新时间", row.get("withSourceUpdate")),
            buildCell(tableName, "withoutSourceUpdate", "缺少源更新时间", row.get("withoutSourceUpdate")),
            buildCell(tableName, "syncedIn24h", "24 小时内同步", row.get("syncedIn24h")),
            buildCell(tableName, "staleSync", "24 小时前同步", row.get("staleSync"))));
  }

  private StatisticCellData buildCell(
      String tableName, String columnKey, String columnLabel, Object value) {
    long numericValue = value == null ? 0L : ((Number) value).longValue();
    return new StatisticCellData(
        columnKey,
        numericValue,
        String.valueOf(numericValue),
        true,
        DETAIL_VIEW_KEY,
        Map.of("tableName", tableName, "columnKey", columnKey, "columnLabel", columnLabel));
  }

  private void appendColumnFilter(StringBuilder where, List<Object> args, String columnKey) {
    switch (columnKey) {
      case "totalRecords" -> {
      }
      case "withSourceUpdate" -> where.append(" and updated_at_source is not null ");
      case "withoutSourceUpdate" -> where.append(" and updated_at_source is null ");
      case "syncedIn24h" -> where.append(" and synced_at >= current_timestamp - interval '24 hours' ");
      case "staleSync" -> where.append(" and synced_at < current_timestamp - interval '24 hours' ");
      default -> throw new BizException("不支持的统计列: " + columnKey);
    }
  }

  private String buildDetailTitle(String tableName, String columnKey) {
    return tableName
        + " / "
        + switch (columnKey) {
          case "totalRecords" -> "总记录数";
          case "withSourceUpdate" -> "有源更新时间";
          case "withoutSourceUpdate" -> "缺少源更新时间";
          case "syncedIn24h" -> "24 小时内同步";
          case "staleSync" -> "24 小时前同步";
          default -> columnKey;
        };
  }

  private String trimToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private int parsePositiveInt(String value, int defaultValue) {
    try {
      int parsed = Integer.parseInt(value);
      return parsed > 0 ? parsed : defaultValue;
    } catch (Exception ignored) {
      return defaultValue;
    }
  }
}
