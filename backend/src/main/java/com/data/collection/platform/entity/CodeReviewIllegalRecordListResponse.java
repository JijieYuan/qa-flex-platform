package com.data.collection.platform.entity;

import java.util.List;

public record CodeReviewIllegalRecordListResponse(
    List<CodeReviewIllegalRecordRowResponse> records,
    long total,
    int page,
    int size,
    String sortField,
    String sortOrder) {
}
