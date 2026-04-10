package com.data.collection.platform.service;

import java.util.List;

public record PageSlice<T>(
    List<T> records,
    long total,
    int page,
    int size) {
}
