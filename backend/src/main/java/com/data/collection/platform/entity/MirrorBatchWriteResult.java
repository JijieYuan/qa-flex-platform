package com.data.collection.platform.entity;

public record MirrorBatchWriteResult(
    int attemptedRows,
    int appliedRows,
    int skippedConflicts) {
}
