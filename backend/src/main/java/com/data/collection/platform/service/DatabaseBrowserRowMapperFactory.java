package com.data.collection.platform.service;

import java.sql.SQLException;
import java.time.temporal.TemporalAccessor;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.jdbc.core.RowMapper;

final class DatabaseBrowserRowMapperFactory {
  private DatabaseBrowserRowMapperFactory() {
  }

  static RowMapper<Map<String, Object>> createTableRowMapper() {
    return (resultSet, rowNum) -> {
      int columnCount = resultSet.getMetaData().getColumnCount();
      Map<String, Object> row = new LinkedHashMap<>();
      for (int index = 1; index <= columnCount; index++) {
        String columnName = resultSet.getMetaData().getColumnName(index);
        row.put(columnName, normalizeValue(resultSet.getObject(index)));
      }
      return row;
    };
  }

  private static Object normalizeValue(Object value) throws SQLException {
    if (value == null) {
      return null;
    }
    if ("org.postgresql.util.PGobject".equals(value.getClass().getName())) {
      return value.toString();
    }
    if (value instanceof TemporalAccessor) {
      return value.toString();
    }
    return value;
  }
}
