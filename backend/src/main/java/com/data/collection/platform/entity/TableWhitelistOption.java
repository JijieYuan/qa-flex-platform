package com.data.collection.platform.entity;

public record TableWhitelistOption(
    String tableName,
    String label,
    String primaryKey,
    String updatedAtColumn,
    boolean recommended) {
}
