package com.data.collection.platform.entity;

import java.util.List;

public record SystemTestIssueSearchListResponse(
    List<SystemTestIssueSearchRowResponse> records,
    long total,
    int page,
    int size,
    String sortField,
    String sortOrder) {}
