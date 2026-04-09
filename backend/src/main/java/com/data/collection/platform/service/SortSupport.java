package com.data.collection.platform.service;

import java.util.Comparator;
import java.util.function.Function;

public final class SortSupport {
  private SortSupport() {}

  public static <T> Comparator<T> nullableString(Function<T, String> extractor) {
    return Comparator.comparing(extractor, Comparator.nullsLast(String::compareToIgnoreCase));
  }

  public static <T, U extends Comparable<? super U>> Comparator<T> nullableComparable(
      Function<T, U> extractor) {
    return Comparator.comparing(extractor, Comparator.nullsLast(Comparator.naturalOrder()));
  }

  public static <T> Comparator<T> applyDirection(Comparator<T> comparator, boolean ascending) {
    return ascending ? comparator : comparator.reversed();
  }
}
