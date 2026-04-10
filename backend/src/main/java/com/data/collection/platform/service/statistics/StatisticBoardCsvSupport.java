package com.data.collection.platform.service.statistics;

import com.data.collection.platform.entity.statistics.StatisticBoardResponse;
import com.data.collection.platform.entity.statistics.StatisticColumnLeaf;
import com.data.collection.platform.entity.statistics.StatisticRowData;
import java.util.List;
import java.util.StringJoiner;

final class StatisticBoardCsvSupport {
  private StatisticBoardCsvSupport() {
  }

  static String export(StatisticBoardResponse response) {
    List<StatisticColumnLeaf> leafColumns =
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

  private static String csvValue(String value) {
    if (value == null) {
      return "";
    }
    return "\"" + value.replace("\"", "\"\"") + "\"";
  }
}
