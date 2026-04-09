package com.data.collection.platform.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class SortSupportTest {
  @Test
  void shouldSortNullableStringsAndApplyDirection() {
    record Row(String value) {}

    List<Row> rows = List.of(new Row("beta"), new Row(null), new Row("Alpha"));
    List<String> ascending = rows.stream()
        .sorted(SortSupport.applyDirection(SortSupport.nullableString(Row::value), true))
        .map(Row::value)
        .toList();
    List<String> descending = rows.stream()
        .sorted(SortSupport.applyDirection(SortSupport.nullableString(Row::value), false))
        .map(Row::value)
        .toList();

    assertEquals(Arrays.asList("Alpha", "beta", null), ascending);
    assertEquals(Arrays.asList(null, "beta", "Alpha"), descending);
  }

  @Test
  void shouldSortNullableComparableValues() {
    record Row(LocalDateTime value) {}

    LocalDateTime first = LocalDateTime.of(2026, 4, 1, 10, 0);
    LocalDateTime second = LocalDateTime.of(2026, 4, 2, 10, 0);
    List<Row> rows = List.of(new Row(second), new Row(null), new Row(first));

    List<LocalDateTime> sorted = rows.stream()
        .sorted(SortSupport.nullableComparable(Row::value))
        .map(Row::value)
        .toList();

    assertEquals(Arrays.asList(first, second, null), sorted);
  }
}
