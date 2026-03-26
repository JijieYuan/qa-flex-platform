package com.data.collection.platform.service.statistics;

import com.data.collection.platform.entity.statistics.StatisticBoardDefinition;
import com.data.collection.platform.entity.statistics.StatisticBoardResponse;
import com.data.collection.platform.entity.statistics.StatisticColumnLeaf;
import com.data.collection.platform.entity.statistics.StatisticDetailRequest;
import com.data.collection.platform.entity.statistics.StatisticDetailResponse;
import com.data.collection.platform.entity.statistics.StatisticRowData;
import java.util.Map;
import java.util.StringJoiner;

public abstract class AbstractStatisticBoardService {
  public abstract String boardKey();

  protected abstract StatisticBoardDefinition buildDefinition();

  protected abstract StatisticBoardResponse doLoadBoard(Map<String, String> filters);

  protected abstract StatisticDetailResponse doLoadDetail(StatisticDetailRequest request);

  public StatisticBoardDefinition getDefinition() {
    return buildDefinition();
  }

  public StatisticBoardResponse loadBoard(Map<String, String> filters) {
    return doLoadBoard(filters == null ? Map.of() : filters);
  }

  public StatisticDetailResponse loadDetail(StatisticDetailRequest request) {
    return doLoadDetail(request);
  }

  public String exportBoardCsv(Map<String, String> filters) {
    StatisticBoardResponse response = loadBoard(filters);
    StringBuilder builder = new StringBuilder();
    StringJoiner headerJoiner = new StringJoiner(",");
    headerJoiner.add(csvValue("统计对象"));
    for (StatisticColumnLeaf column : response.definition().columnGroups().stream().flatMap(group -> group.columns().stream()).toList()) {
      headerJoiner.add(csvValue(column.label()));
    }
    builder.append(headerJoiner).append('\n');

    for (StatisticRowData row : response.rows()) {
      StringJoiner rowJoiner = new StringJoiner(",");
      rowJoiner.add(csvValue(row.rowLabel()));
      for (StatisticColumnLeaf column : response.definition().columnGroups().stream().flatMap(group -> group.columns().stream()).toList()) {
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
}
