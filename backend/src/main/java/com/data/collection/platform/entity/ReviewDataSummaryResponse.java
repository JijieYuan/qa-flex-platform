package com.data.collection.platform.entity;

public record ReviewDataSummaryResponse(
    long totalRecords,
    long totalProblemItems,
    double averageReviewScalePages,
    double averageProblemCount) {}
