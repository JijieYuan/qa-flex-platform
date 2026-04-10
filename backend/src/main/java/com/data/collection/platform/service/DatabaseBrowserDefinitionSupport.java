package com.data.collection.platform.service;

import com.data.collection.platform.entity.database.DatabaseTableColumn;
import java.util.List;

final class DatabaseBrowserDefinitionSupport {

  private DatabaseBrowserDefinitionSupport() {
  }

  static DatabaseBrowserTableDefinition definition(
      String label,
      List<String> searchableFields,
      String defaultSortField,
      DatabaseTableColumn... columns) {
    return new DatabaseBrowserTableDefinition(
        label,
        searchableFields,
        List.of(columns),
        defaultSortField);
  }

  static DatabaseTableColumn column(String key, String label) {
    return new DatabaseTableColumn(key, label, true);
  }
}
