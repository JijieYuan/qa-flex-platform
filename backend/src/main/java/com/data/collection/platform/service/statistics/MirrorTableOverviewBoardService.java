package com.data.collection.platform.service.statistics;

import com.data.collection.platform.common.exception.BizException;
import com.data.collection.platform.entity.statistics.StatisticBoardDefinition;
import com.data.collection.platform.entity.statistics.StatisticBoardMeta;
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
import com.data.collection.platform.mapper.MirrorTableOverviewMapper;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

  private final MirrorTableOverviewMapper overviewMapper;

  public MirrorTableOverviewBoardService(MirrorTableOverviewMapper overviewMapper) {
    this.overviewMapper = overviewMapper;
  }

  @Override
  public String boardKey() {
    return BOARD_KEY;
  }

  @Override
  protected StatisticBoardDefinition buildDefinition() {
    return buildDefinition(List.of());
  }

  @Override
  protected StatisticBoardResponse doLoadBoard(Map<String, String> filters) {
    long startedAt = System.currentTimeMillis();
    List<StatisticFilterOption> tableOptions = queryTableOptions();
    String tableName = trimToNull(filters.get("tableName"));
    List<Map<String, Object>> rows = overviewMapper.selectSummaryRows(tableName);
    List<StatisticRowData> boardRows = rows.stream().map(this::toStatisticRow).toList();

    StatisticBoardDefinition definition = buildDefinition(tableOptions);
    Map<String, String> appliedFilters = new LinkedHashMap<>();
    appliedFilters.put("tableName", tableName == null ? "" : tableName);

    int columnCount = definition.columnGroups().stream().mapToInt(group -> group.columns().size()).sum();
    int drilldownColumnCount =
        definition.columnGroups().stream()
            .flatMap(group -> group.columns().stream())
            .mapToInt(column -> column.drilldown() ? 1 : 0)
            .sum();

    StatisticBoardMeta meta =
        new StatisticBoardMeta(
            LocalDateTime.now(),
            System.currentTimeMillis() - startedAt,
            boardRows.size(),
            columnCount,
            drilldownColumnCount);
    return new StatisticBoardResponse(definition, appliedFilters, boardRows, meta);
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

    long total = overviewMapper.countDetails(tableName, columnKey);
    List<Map<String, Object>> records =
        overviewMapper.selectDetails(tableName, columnKey, sortField, sortOrder, size, (page - 1) * size);

    return new StatisticDetailResponse(
        buildDetailTitle(tableName, columnKey),
        "展示当前统计单元格对应的镜像原始记录。",
        DETAIL_COLUMNS,
        records,
        total,
        page,
        size,
        request.sortField() == null ? "syncedAt" : request.sortField(),
        "ascending".equalsIgnoreCase(request.sortOrder()) ? "ascending" : "descending");
  }

  private StatisticBoardDefinition buildDefinition(List<StatisticFilterOption> tableOptions) {
    return new StatisticBoardDefinition(
        BOARD_KEY,
        "GitLab 镜像表基础统计",
        "基于已同步到本地的镜像记录，对各张 GitLab 源表的记录规模和同步覆盖情况进行汇总展示。",
        "查询与操作",
        "按真实镜像表名筛选当前统计范围。本页只使用镜像表中的标准字段，不引入任何额外业务规则。",
        List.of(
            new StatisticFilterField(
                "tableName",
                "镜像表",
                "select",
                "",
                "",
                260,
                tableOptions)),
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

  private List<StatisticFilterOption> queryTableOptions() {
    return overviewMapper.selectTableNames().stream()
        .map(tableName -> new StatisticFilterOption(tableName, tableName))
        .toList();
  }

  private StatisticRowData toStatisticRow(Map<String, Object> row) {
    String tableName = String.valueOf(row.get("tableName"));
    return new StatisticRowData(
        tableName,
        tableName,
        List.of(
            buildCell(tableName, "totalRecords", row.get("totalRecords")),
            buildCell(tableName, "withSourceUpdate", row.get("withSourceUpdate")),
            buildCell(tableName, "withoutSourceUpdate", row.get("withoutSourceUpdate")),
            buildCell(tableName, "syncedIn24h", row.get("syncedIn24h")),
            buildCell(tableName, "staleSync", row.get("staleSync"))));
  }

  private StatisticCellData buildCell(String tableName, String columnKey, Object value) {
    long numericValue = value == null ? 0L : ((Number) value).longValue();
    return new StatisticCellData(
        columnKey,
        numericValue,
        String.valueOf(numericValue),
        true,
        DETAIL_VIEW_KEY,
        Map.of("tableName", tableName, "columnKey", columnKey));
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
}
