package com.data.collection.platform.service;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;
import org.springframework.util.StringUtils;

abstract class AbstractFactQueryService {
  protected <T> void appendEq(
      StringBuilder sql,
      List<Object> args,
      String column,
      String rawValue,
      Function<String, T> mapper) {
    String value = trimToNull(rawValue);
    if (value == null) {
      return;
    }
    sql.append(" and ").append(column).append(" = ?");
    args.add(mapper.apply(value));
  }

  protected void appendContains(StringBuilder sql, List<Object> args, String column, String rawValue) {
    String value = trimToNull(rawValue);
    if (value == null) {
      return;
    }
    sql.append(" and ").append(column).append(" like ?");
    args.add("%" + value + "%");
  }

  protected void appendDateFrom(StringBuilder sql, List<Object> args, String column, String rawValue) {
    LocalDate value = parseDate(rawValue);
    if (value == null) {
      return;
    }
    sql.append(" and ").append(column).append(" >= ?");
    args.add(value.atStartOfDay());
  }

  protected void appendDateTo(StringBuilder sql, List<Object> args, String column, String rawValue) {
    LocalDate value = parseDate(rawValue);
    if (value == null) {
      return;
    }
    sql.append(" and ").append(column).append(" < ?");
    args.add(value.plusDays(1).atStartOfDay());
  }

  protected String trimToNull(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }

  protected LocalDate parseDate(String value) {
    String normalized = trimToNull(value);
    return normalized == null ? null : LocalDate.parse(normalized);
  }
}
