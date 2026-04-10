package com.data.collection.platform.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class PageSliceSupportTest {
  @Test
  void sliceShouldNormalizeInvalidPageAndSize() {
    PageSlice<Integer> slice = PageSliceSupport.slice(List.of(1, 2, 3), 0, 0);

    assertEquals(List.of(1, 2, 3), slice.records());
    assertEquals(3, slice.total());
    assertEquals(1, slice.page());
    assertEquals(20, slice.size());
  }

  @Test
  void sliceShouldReturnRequestedPageWindow() {
    PageSlice<Integer> slice = PageSliceSupport.slice(List.of(1, 2, 3, 4, 5), 2, 2);

    assertEquals(List.of(3, 4), slice.records());
    assertEquals(5, slice.total());
    assertEquals(2, slice.page());
    assertEquals(2, slice.size());
  }
}
