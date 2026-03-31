package com.data.collection.platform.service.statistics;

import com.data.collection.platform.common.JsonUtils;
import com.data.collection.platform.entity.statistics.StatisticBoardDefinition;
import com.data.collection.platform.entity.statistics.StatisticBoardResponse;
import com.data.collection.platform.entity.statistics.StatisticColumnLeaf;
import com.data.collection.platform.entity.statistics.StatisticDetailRequest;
import com.data.collection.platform.entity.statistics.StatisticDetailResponse;
import com.data.collection.platform.entity.statistics.StatisticFilterCondition;
import com.data.collection.platform.entity.statistics.StatisticFilterField;
import com.data.collection.platform.entity.statistics.StatisticFilterGroup;
import com.data.collection.platform.entity.statistics.StatisticRowData;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

public abstract class AbstractStatisticBoardService {
  private static final String FILTER_GROUP_PARAM = "filterGroup";

  private final JsonUtils jsonUtils;

  protected AbstractStatisticBoardService(JsonUtils jsonUtils) {
    this.jsonUtils = jsonUtils;
  }

  public abstract String boardKey();

  protected abstract StatisticBoardDefinition buildDefinition();

  public StatisticBoardDefinition getDefinition() {
    return buildDefinition();
  }

  public StatisticBoardResponse loadBoard(Map<String, String> filters) {
    Map<String, String> safeFilters = filters == null ? Map.of() : filters;
    StatisticBoardDefinition definition = buildDefinition();
    StatisticFilterGroup filterGroup = parseFilterGroup(safeFilters, definition);
    return doLoadBoard(safeFilters, filterGroup);
  }

  public StatisticDetailResponse loadDetail(StatisticDetailRequest request) {
    StatisticBoardDefinition definition = buildDefinition();
    StatisticFilterGroup filterGroup = parseFilterGroup(request.filters(), definition);
    return doLoadDetail(request, filterGroup);
  }

  public String exportBoardCsv(Map<String, String> filters) {
    StatisticBoardResponse response = loadBoard(filters);
    StringBuilder builder = new StringBuilder();
    StringJoiner headerJoiner = new StringJoiner(",");
    headerJoiner.add(csvValue(response.definition().rowHeaderLabel()));
    for (StatisticColumnLeaf column : response.definition().columnGroups().stream().flatMap(group -> group.columns().stream()).toList()) {
      headerJoiner.add(csvValue(column.label()));
    }
    builder.append(headerJoiner).append('\n');

    for (StatisticRowData row : response.rows()) {
      StringJoiner rowJoiner = new StringJoiner(",");
      rowJoiner.add(csvValue(row.rowLabel()));
      for (StatisticColumnLeaf column : response.definition().columnGroups().stream().flatMap(group -> group.columns().stream()).toList()) {
        String displayValue =
            row.cells().stream()
                .filter(cell -> cell.columnKey().equals(column.key()))
                .findFirst()
                .map(cell -> cell.displayValue())
                .orElse("");
        rowJoiner.add(csvValue(displayValue));
      }
      builder.append(rowJoiner).append('\n');
    }
    return builder.toString();
  }

  private String csvValue(String value) {
    if (value == null) {
      return "";
    }
    return "\"" + value.replace("\"", "\"\"") + "\"";
  }

  protected abstract StatisticBoardResponse doLoadBoard(Map<String, String> filters, StatisticFilterGroup filterGroup);

  protected abstract StatisticDetailResponse doLoadDetail(StatisticDetailRequest request, StatisticFilterGroup filterGroup);

  protected StatisticFilterGroup parseFilterGroup(Map<String, String> filters, StatisticBoardDefinition definition) {
    if (filters == null || filters.isEmpty()) {
      return emptyFilterGroup();
    }
    String filterGroupJson = trimToNull(filters.get(FILTER_GROUP_PARAM));
    if (filterGroupJson != null) {
      StatisticFilterGroup parsed = jsonUtils.fromJson(filterGroupJson, new TypeReference<>() {});
      return validateFilterGroup(parsed, definition);
    }
    return validateFilterGroup(buildLegacyFilterGroup(filters, definition), definition);
  }

  protected Map<String, String> withoutReservedFilters(Map<String, String> filters) {
    if (filters == null || filters.isEmpty()) {
      return Map.of();
    }
    return filters.entrySet().stream()
        .filter(entry -> !FILTER_GROUP_PARAM.equals(entry.getKey()))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  protected StatisticFilterGroup emptyFilterGroup() {
    return new StatisticFilterGroup("AND", java.util.List.of());
  }

  private StatisticFilterGroup buildLegacyFilterGroup(Map<String, String> filters, StatisticBoardDefinition definition) {
    Map<String, String> safeFilters = withoutReservedFilters(filters);
    if (safeFilters.isEmpty()) {
      return emptyFilterGroup();
    }
    Map<String, StatisticFilterField> fieldMap = toFieldMap(definition);
    java.util.List<StatisticFilterCondition> conditions =
        safeFilters.entrySet().stream()
            .filter(entry -> fieldMap.containsKey(entry.getKey()))
            .map(entry -> toLegacyCondition(fieldMap.get(entry.getKey()), entry.getValue()))
            .filter(Objects::nonNull)
            .toList();
    if (conditions.isEmpty()) {
      return emptyFilterGroup();
    }
    return new StatisticFilterGroup("AND", conditions);
  }

  private StatisticFilterCondition toLegacyCondition(StatisticFilterField field, String value) {
    String safeValue = trimToNull(value);
    if (safeValue == null) {
      return null;
    }
    return new StatisticFilterCondition(field.key(), "eq", safeValue, null);
  }

  private StatisticFilterGroup validateFilterGroup(StatisticFilterGroup input, StatisticBoardDefinition definition) {
    if (input == null || input.conditions() == null || input.conditions().isEmpty()) {
      return emptyFilterGroup();
    }
    String logic = "OR".equalsIgnoreCase(input.logic()) ? "OR" : "AND";
    Map<String, StatisticFilterField> fieldMap = toFieldMap(definition);
    java.util.List<StatisticFilterCondition> normalized = new java.util.ArrayList<>();
    for (StatisticFilterCondition condition : input.conditions()) {
      if (condition == null) {
        continue;
      }
      StatisticFilterField field = fieldMap.get(trimToNull(condition.fieldKey()));
      if (field == null) {
        throw new IllegalArgumentException("Unsupported statistic filter field: " + condition.fieldKey());
      }
      String operator = trimToNull(condition.operator());
      if (operator == null || !field.operators().contains(operator)) {
        throw new IllegalArgumentException("Unsupported operator for field " + field.key() + ": " + condition.operator());
      }
      String value = trimToNull(condition.value());
      String secondaryValue = trimToNull(condition.secondaryValue());
      if (requiresPrimaryValue(operator) && value == null) {
        continue;
      }
      if ("between".equals(operator) && secondaryValue == null) {
        continue;
      }
      normalized.add(new StatisticFilterCondition(field.key(), operator, value, secondaryValue));
    }
    if (normalized.isEmpty()) {
      return emptyFilterGroup();
    }
    return new StatisticFilterGroup(logic, normalized);
  }

  private boolean requiresPrimaryValue(String operator) {
    return !"isEmpty".equals(operator) && !"isNotEmpty".equals(operator);
  }

  private Map<String, StatisticFilterField> toFieldMap(StatisticBoardDefinition definition) {
    Map<String, StatisticFilterField> fieldMap = new HashMap<>();
    for (StatisticFilterField field : definition.filters()) {
      fieldMap.put(field.key(), field);
    }
    return fieldMap;
  }

  protected Set<String> filterableFieldKeys(StatisticBoardDefinition definition) {
    return new HashSet<>(toFieldMap(definition).keySet());
  }

  protected String trimToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
