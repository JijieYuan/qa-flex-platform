package com.data.collection.platform.entity;

public record SourceTableColumn(
    String columnName,
    String formattedType,
    boolean nullable,
    int ordinalPosition) {
}
