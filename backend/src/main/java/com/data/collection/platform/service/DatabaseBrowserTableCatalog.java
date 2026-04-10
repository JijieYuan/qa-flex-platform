package com.data.collection.platform.service;

import java.util.LinkedHashMap;
import java.util.Map;

final class DatabaseBrowserTableCatalog {

  private static final Map<String, DatabaseBrowserTableDefinition> SYSTEM_TABLE_DEFINITIONS =
      createDefinitions();

  private DatabaseBrowserTableCatalog() {
  }

  static Map<String, DatabaseBrowserTableDefinition> listDefinitions() {
    return SYSTEM_TABLE_DEFINITIONS;
  }

  static DatabaseBrowserTableDefinition findDefinition(String tableName) {
    return SYSTEM_TABLE_DEFINITIONS.get(tableName);
  }

  private static Map<String, DatabaseBrowserTableDefinition> createDefinitions() {
    Map<String, DatabaseBrowserTableDefinition> definitions = new LinkedHashMap<>();
    definitions.putAll(DatabaseBrowserSyncTableDefinitions.definitions());
    definitions.putAll(DatabaseBrowserCollectionTableDefinitions.definitions());
    definitions.putAll(DatabaseBrowserFactTableDefinitions.definitions());
    return Map.copyOf(definitions);
  }
}
