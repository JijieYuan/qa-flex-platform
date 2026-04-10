package com.data.collection.platform.service;

import java.util.List;

public final class PageSliceSupport {
  private PageSliceSupport() {
  }

  public static <T> PageSlice<T> slice(List<T> items, int page, int size) {
    int safePage = page <= 0 ? 1 : page;
    int safeSize = size <= 0 ? 20 : size;
    int fromIndex = Math.min((safePage - 1) * safeSize, items.size());
    int toIndex = Math.min(fromIndex + safeSize, items.size());
    return new PageSlice<>(
        items.subList(fromIndex, toIndex),
        items.size(),
        safePage,
        safeSize);
  }
}
