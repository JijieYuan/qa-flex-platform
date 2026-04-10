package com.data.collection.platform.service.statistics;

import com.data.collection.platform.common.JsonUtils;
import com.data.collection.platform.entity.statistics.StatisticBoardDefinition;
import com.data.collection.platform.entity.statistics.StatisticBoardResponse;
import com.data.collection.platform.entity.statistics.StatisticColumnLeaf;
import com.data.collection.platform.entity.statistics.StatisticDetailRequest;
import com.data.collection.platform.entity.statistics.StatisticDetailResponse;
import com.data.collection.platform.entity.statistics.StatisticFilterGroup;
import com.data.collection.platform.entity.statistics.StatisticRowData;
import java.util.Map;
import java.util.StringJoiner;

public abstract class AbstractStatisticBoardService {
  private final JsonUtils jsonUtils;

  protected AbstractStatisticBoardService(JsonUtils jsonUtils) {
    this.jsonUtils = jsonUtils;
  }

  public abstract String boardKey();

  protected abstract StatisticBoardDefinition buildDefinition();

  public StatisticBoardDefinition getDefinition() {
    return buildDefinition();
  }

  public StatisticBoardResponse loadBoard(Map<String, String> filters) {
    Map<String, String> safeFilters = filters == null ? Map.of() : filters;
    StatisticBoardDefinition definition = buildDefinition();
    StatisticFilterGroup filterGroup = parseFilterGroup(safeFilters, definition);
    return doLoadBoard(safeFilters, filterGroup);
  }

  public StatisticDetailResponse loadDetail(StatisticDetailRequest request) {
    StatisticBoardDefinition definition = buildDefinition();
    StatisticFilterGroup filterGroup = parseFilterGroup(request.filters(), definition);
    return doLoadDetail(request, filterGroup);
  }

  public String exportBoardCsv(Map<String, String> filters) {
    StatisticBoardResponse response = loadBoard(filters);
    java.util.List<StatisticColumnLeaf> leafColumns =
        response.definition().columnGroups().stream().flatMap(group -> group.leafColumns().stream()).toList();
    StringBuilder builder = new StringBuilder();
    StringJoiner headerJoiner = new StringJoiner(",");
    headerJoiner.add(csvValue(response.definition().rowHeaderLabel()));
    for (StatisticColumnLeaf column : leafColumns) {
      headerJoiner.add(csvValue(column.label()));
    }
    builder.append(headerJoiner).append('\n');

    for (StatisticRowData row : response.rows()) {
      StringJoiner rowJoiner = new StringJoiner(",");
      rowJoiner.add(csvValue(row.rowLabel()));
      for (StatisticColumnLeaf column : leafColumns) {
        String displayValue =
            row.cells().stream()
                .filter(cell -> cell.columnKey().equals(column.key()))
                .findFirst()
                .map(cell -> cell.displayValue())
                .orElse("");
        rowJoiner.add(csvValue(displayValue));
      }
      builder.append(rowJoiner).append('\n');
    }
    return builder.toString();
  }

  private String csvValue(String value) {
    if (value == null) {
      return "";
    }
    return "\"" + value.replace("\"", "\"\"") + "\"";
  }

  protected abstract StatisticBoardResponse doLoadBoard(Map<String, String> filters, StatisticFilterGroup filterGroup);

  protected abstract StatisticDetailResponse doLoadDetail(StatisticDetailRequest request, StatisticFilterGroup filterGroup);

  protected StatisticFilterGroup parseFilterGroup(Map<String, String> filters, StatisticBoardDefinition definition) {
    return StatisticFilterGroupSupport.parseFilterGroup(jsonUtils, filters, definition);
  }

  protected Map<String, String> withoutReservedFilters(Map<String, String> filters) {
    return StatisticFilterGroupSupport.withoutReservedFilters(filters);
  }

  protected StatisticFilterGroup emptyFilterGroup() {
    return StatisticFilterGroupSupport.emptyFilterGroup();
  }

  protected java.util.Set<String> filterableFieldKeys(StatisticBoardDefinition definition) {
    return StatisticFilterGroupSupport.filterableFieldKeys(definition);
  }

  protected String trimToNull(String value) {
    return StatisticFilterGroupSupport.trimToNull(value);
  }
}
