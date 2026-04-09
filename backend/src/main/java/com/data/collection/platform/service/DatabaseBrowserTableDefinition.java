package com.data.collection.platform.service;

import com.data.collection.platform.entity.database.DatabaseTableColumn;
import java.util.List;

record DatabaseBrowserTableDefinition(
    String label,
    List<String> searchableFields,
    List<DatabaseTableColumn> columns,
    String defaultSortField) {
}
