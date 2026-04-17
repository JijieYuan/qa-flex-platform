package com.data.collection.platform.entity;

import java.util.List;

public record ReviewDataRecordListResponse(
    List<ReviewDataRecordRowResponse> records,
    long total,
    int page,
    int size,
    String sortField,
    String sortOrder,
    ReviewDataSummaryResponse summary) {
}
