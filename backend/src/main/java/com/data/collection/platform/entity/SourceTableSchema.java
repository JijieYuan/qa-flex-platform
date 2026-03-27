package com.data.collection.platform.entity;

import java.util.List;

public record SourceTableSchema(
    String tableName,
    List<String> primaryKeys,
    String updatedAtColumn,
    List<SourceTableColumn> columns) {
}
