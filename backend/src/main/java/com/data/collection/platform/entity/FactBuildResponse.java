package com.data.collection.platform.entity;

public record FactBuildResponse(
    String scope,
    boolean full,
    int affectedRows,
    String message) {
}
