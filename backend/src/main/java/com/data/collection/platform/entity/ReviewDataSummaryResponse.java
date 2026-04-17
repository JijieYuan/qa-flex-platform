package com.data.collection.platform.entity;

public record ReviewDataSummaryResponse(
    long totalRecords,
    long activeRecords,
    long deletedRecords,
    double averageDurationMinutes,
    double averageTotalScore,
    double averageCommentRate) {
}
