package com.data.collection.platform.service;

import com.data.collection.platform.entity.OptionItemResponse;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class OptionItemResponseFactory {
  private OptionItemResponseFactory() {
  }

  public static <T> List<OptionItemResponse> from(Collection<T> rows, Function<T, String> extractor, Function<String, String> normalizer) {
    return from(rows.stream().map(extractor).toList(), normalizer);
  }

  public static List<OptionItemResponse> from(Collection<String> values, Function<String, String> normalizer) {
    Set<String> normalized = values.stream()
        .map(normalizer)
        .filter(Objects::nonNull)
        .collect(Collectors.toCollection(LinkedHashSet::new));
    return normalized.stream()
        .map(value -> new OptionItemResponse(value, value))
        .sorted(Comparator.comparing(OptionItemResponse::label, String::compareToIgnoreCase))
        .toList();
  }
}
