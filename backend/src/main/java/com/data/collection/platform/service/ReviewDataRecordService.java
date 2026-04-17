package com.data.collection.platform.service;

import com.data.collection.platform.entity.OptionItemResponse;
import com.data.collection.platform.entity.ReviewDataFilterOptionsResponse;
import com.data.collection.platform.entity.ReviewDataRecordListResponse;
import com.data.collection.platform.entity.ReviewDataRecordRowResponse;
import com.data.collection.platform.entity.ReviewDataSummaryResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class ReviewDataRecordService {
  private static final String BASE_SQL = """
      select
        f.id,
        f.project_id,
        mr.merge_request_id,
        coalesce(mr.merge_request_iid, f.request_iid) as merge_request_iid,
        f.form_title,
        f.template_code,
        f.reviewer,
        f.review_duration_minutes,
        f.specification_score,
        f.logic_score,
        f.performance_score,
        f.design_score,
        f.other_score,
        f.remark,
        f.deleted,
        f.created_at,
        f.updated_at,
        coalesce(mr.project_name, '') as project_name,
        coalesce(mr.repository_name, '') as repository_name,
        coalesce(mr.title, '') as merge_request_title,
        coalesce(mr.module_name, '') as module_name,
        coalesce(mr.target_branch, '') as target_branch,
        mr.comment_rate,
        mr.defect_count,
        mr.added_lines
      from collect_form_records f
      left join merge_request_fact mr
        on mr.project_id = f.project_id
       and mr.merge_request_iid = f.request_iid
       and coalesce(mr.deleted, false) = false
      where f.resource_type = 'merge_request'
      """;

  private static final List<OptionItemResponse> RECORD_STATUS_OPTIONS =
      List.of(
          new OptionItemResponse("有效", "ACTIVE"),
          new OptionItemResponse("已作废", "DELETED"));

  private final JdbcTemplate jdbcTemplate;

  public ReviewDataRecordService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public ReviewDataRecordListResponse listRecords(
      Long projectId,
      String projectName,
      String repositoryName,
      String moduleName,
      String reviewer,
      String templateCode,
      String targetBranch,
      String recordStatus,
      String keyword,
      String mergeRequestIid,
      String updatedAtStart,
      String updatedAtEnd,
      int page,
      int size,
      String sortField,
      String sortOrder) {
    int safePage = page <= 0 ? 1 : page;
    int safeSize = size <= 0 ? 20 : Math.min(size, 100);
    String safeSortField = normalizeSortField(sortField);
    String safeSortOrder = normalizeSortOrder(sortOrder);

    List<ReviewDataRecordRowResponse> filtered =
        loadRecords(
            projectId,
            projectName,
            repositoryName,
            moduleName,
            reviewer,
            templateCode,
            targetBranch,
            recordStatus,
            keyword,
            mergeRequestIid,
            updatedAtStart,
            updatedAtEnd)
            .stream()
            .sorted(buildComparator(safeSortField, safeSortOrder))
            .toList();

    PageSlice<ReviewDataRecordRowResponse> pageSlice =
        PageSliceSupport.slice(filtered, safePage, safeSize);
    ReviewDataSummaryResponse summary = buildSummary(filtered);

    return new ReviewDataRecordListResponse(
        pageSlice.records(),
        pageSlice.total(),
        pageSlice.page(),
        pageSlice.size(),
        safeSortField,
        safeSortOrder,
        summary);
  }

  public ReviewDataFilterOptionsResponse getFilterOptions(Long projectId) {
    List<ReviewDataRecordRowResponse> rows =
        loadRecords(
            projectId,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null);

    return new ReviewDataFilterOptionsResponse(
        toOptions(rows, ReviewDataRecordRowResponse::projectName),
        toOptions(rows, ReviewDataRecordRowResponse::repositoryName),
        toOptions(rows, ReviewDataRecordRowResponse::moduleName),
        toOptions(rows, ReviewDataRecordRowResponse::reviewer),
        toOptions(rows, ReviewDataRecordRowResponse::templateCode),
        toOptions(rows, ReviewDataRecordRowResponse::targetBranch),
        RECORD_STATUS_OPTIONS);
  }

  private List<ReviewDataRecordRowResponse> loadRecords(
      Long projectId,
      String projectName,
      String repositoryName,
      String moduleName,
      String reviewer,
      String templateCode,
      String targetBranch,
      String recordStatus,
      String keyword,
      String mergeRequestIid,
      String updatedAtStart,
      String updatedAtEnd) {
    StringBuilder sql = new StringBuilder(BASE_SQL);
    List<Object> args = new ArrayList<>();

    appendEq(sql, args, "f.project_id", projectId);
    appendContains(sql, args, "mr.project_name", projectName);
    appendContains(sql, args, "mr.repository_name", repositoryName);
    appendContains(sql, args, "mr.module_name", moduleName);
    appendContains(sql, args, "f.reviewer", reviewer);
    appendContains(sql, args, "f.template_code", templateCode);
    appendContains(sql, args, "mr.target_branch", targetBranch);
    appendMergeRequestIid(sql, args, mergeRequestIid);
    appendUpdatedAtStart(sql, args, updatedAtStart);
    appendUpdatedAtEnd(sql, args, updatedAtEnd);
    appendRecordStatus(sql, args, recordStatus);
    appendKeyword(sql, args, keyword);

    return jdbcTemplate.query(sql.toString(), this::mapRow, args.toArray());
  }

  private ReviewDataRecordRowResponse mapRow(ResultSet rs, int rowNum) throws SQLException {
    int totalScore =
        safeInt((Integer) rs.getObject("specification_score"))
            + safeInt((Integer) rs.getObject("logic_score"))
            + safeInt((Integer) rs.getObject("performance_score"))
            + safeInt((Integer) rs.getObject("design_score"))
            + safeInt((Integer) rs.getObject("other_score"));

    return new ReviewDataRecordRowResponse(
        rs.getLong("id"),
        rs.getLong("project_id"),
        (Long) rs.getObject("merge_request_id"),
        (Long) rs.getObject("merge_request_iid"),
        TextQuerySupport.normalizeDisplay(rs.getString("form_title")),
        TextQuerySupport.normalizeDisplay(rs.getString("template_code")),
        TextQuerySupport.normalizeDisplay(rs.getString("reviewer")),
        (Integer) rs.getObject("review_duration_minutes"),
        totalScore,
        (Integer) rs.getObject("specification_score"),
        (Integer) rs.getObject("logic_score"),
        (Integer) rs.getObject("performance_score"),
        (Integer) rs.getObject("design_score"),
        (Integer) rs.getObject("other_score"),
        TextQuerySupport.normalizeDisplay(rs.getString("remark")),
        rs.getBoolean("deleted"),
        TextQuerySupport.normalizeDisplay(rs.getString("project_name")),
        TextQuerySupport.normalizeDisplay(rs.getString("repository_name")),
        TextQuerySupport.normalizeDisplay(rs.getString("merge_request_title")),
        TextQuerySupport.normalizeDisplay(rs.getString("module_name")),
        TextQuerySupport.normalizeDisplay(rs.getString("target_branch")),
        toDouble(rs.getObject("comment_rate")),
        (Integer) rs.getObject("defect_count"),
        (Integer) rs.getObject("added_lines"),
        rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime(),
        rs.getTimestamp("updated_at") == null ? null : rs.getTimestamp("updated_at").toLocalDateTime());
  }

  private ReviewDataSummaryResponse buildSummary(List<ReviewDataRecordRowResponse> rows) {
    long totalRecords = rows.size();
    long activeRecords = rows.stream().filter(row -> !row.deleted()).count();
    long deletedRecords = totalRecords - activeRecords;

    double averageDurationMinutes =
        average(rows.stream().map(ReviewDataRecordRowResponse::reviewDurationMinutes).toList());
    double averageTotalScore =
        average(rows.stream().map(ReviewDataRecordRowResponse::totalScore).toList());
    double averageCommentRate =
        average(rows.stream().map(ReviewDataRecordRowResponse::commentRate).toList());

    return new ReviewDataSummaryResponse(
        totalRecords,
        activeRecords,
        deletedRecords,
        averageDurationMinutes,
        averageTotalScore,
        averageCommentRate);
  }

  private double average(List<? extends Number> values) {
    List<Double> normalized =
        values.stream()
            .filter(value -> value != null)
            .map(Number::doubleValue)
            .toList();
    if (normalized.isEmpty()) {
      return 0D;
    }
    return normalized.stream().mapToDouble(Double::doubleValue).average().orElse(0D);
  }

  private List<OptionItemResponse> toOptions(
      List<ReviewDataRecordRowResponse> rows,
      Function<ReviewDataRecordRowResponse, String> extractor) {
    Set<String> values = new LinkedHashSet<>();
    rows.forEach(
        row -> {
          String value = TextQuerySupport.trimToNull(extractor.apply(row));
          if (value != null) {
            values.add(value);
          }
        });
    return values.stream().map(value -> new OptionItemResponse(value, value)).toList();
  }

  private void appendEq(StringBuilder sql, List<Object> args, String column, Long value) {
    if (value == null || value <= 0) {
      return;
    }
    sql.append(" and ").append(column).append(" = ?");
    args.add(value);
  }

  private void appendContains(StringBuilder sql, List<Object> args, String column, String value) {
    String normalized = TextQuerySupport.trimToNull(value);
    if (normalized == null) {
      return;
    }
    sql.append(" and lower(coalesce(").append(column).append(", '')) like ?");
    args.add("%" + normalized.toLowerCase(Locale.ROOT) + "%");
  }

  private void appendMergeRequestIid(StringBuilder sql, List<Object> args, String value) {
    String normalized = TextQuerySupport.trimToNull(value);
    if (normalized == null) {
      return;
    }
    sql.append(" and coalesce(mr.merge_request_iid, f.request_iid) = ?");
    args.add(Long.parseLong(normalized));
  }

  private void appendUpdatedAtStart(StringBuilder sql, List<Object> args, String value) {
    LocalDate date = parseDate(value);
    if (date == null) {
      return;
    }
    sql.append(" and f.updated_at >= ?");
    args.add(date.atStartOfDay());
  }

  private void appendUpdatedAtEnd(StringBuilder sql, List<Object> args, String value) {
    LocalDate date = parseDate(value);
    if (date == null) {
      return;
    }
    sql.append(" and f.updated_at < ?");
    args.add(date.plusDays(1).atStartOfDay());
  }

  private void appendRecordStatus(StringBuilder sql, List<Object> args, String value) {
    String normalized = TextQuerySupport.trimToNull(value);
    if ("ACTIVE".equalsIgnoreCase(normalized)) {
      sql.append(" and f.deleted = false");
      return;
    }
    if ("DELETED".equalsIgnoreCase(normalized)) {
      sql.append(" and f.deleted = true");
    }
  }

  private void appendKeyword(StringBuilder sql, List<Object> args, String keyword) {
    String normalized = TextQuerySupport.trimToNull(keyword);
    if (normalized == null) {
      return;
    }
    sql.append("""
         and (
           lower(coalesce(f.form_title, '')) like ?
           or lower(coalesce(f.reviewer, '')) like ?
           or lower(coalesce(f.remark, '')) like ?
           or lower(coalesce(mr.title, '')) like ?
           or lower(coalesce(mr.project_name, '')) like ?
           or lower(coalesce(mr.repository_name, '')) like ?
           or lower(coalesce(mr.module_name, '')) like ?
         )
        """);
    String like = "%" + normalized.toLowerCase(Locale.ROOT) + "%";
    args.add(like);
    args.add(like);
    args.add(like);
    args.add(like);
    args.add(like);
    args.add(like);
    args.add(like);
  }

  private Comparator<ReviewDataRecordRowResponse> buildComparator(String sortField, String sortOrder) {
    Comparator<ReviewDataRecordRowResponse> comparator =
        switch (sortField) {
          case "projectName" -> comparingString(ReviewDataRecordRowResponse::projectName);
          case "repositoryName" -> comparingString(ReviewDataRecordRowResponse::repositoryName);
          case "reviewer" -> comparingString(ReviewDataRecordRowResponse::reviewer);
          case "moduleName" -> comparingString(ReviewDataRecordRowResponse::moduleName);
          case "reviewDurationMinutes" -> comparingNumber(ReviewDataRecordRowResponse::reviewDurationMinutes);
          case "totalScore" -> comparingNumber(ReviewDataRecordRowResponse::totalScore);
          case "commentRate" -> comparingNumber(ReviewDataRecordRowResponse::commentRate);
          case "defectCount" -> comparingNumber(ReviewDataRecordRowResponse::defectCount);
          case "addedLines" -> comparingNumber(ReviewDataRecordRowResponse::addedLines);
          case "createdAt" -> Comparator.comparing(ReviewDataRecordRowResponse::createdAt, Comparator.nullsLast(LocalDateTime::compareTo));
          case "updatedAt" -> Comparator.comparing(ReviewDataRecordRowResponse::updatedAt, Comparator.nullsLast(LocalDateTime::compareTo));
          default -> Comparator.comparing(ReviewDataRecordRowResponse::updatedAt, Comparator.nullsLast(LocalDateTime::compareTo));
        };
    return "asc".equalsIgnoreCase(sortOrder) ? comparator : comparator.reversed();
  }

  private Comparator<ReviewDataRecordRowResponse> comparingString(Function<ReviewDataRecordRowResponse, String> extractor) {
    return Comparator.comparing(
        row -> TextQuerySupport.normalizeDisplay(extractor.apply(row)).toLowerCase(Locale.ROOT),
        Comparator.nullsLast(String::compareTo));
  }

  private Comparator<ReviewDataRecordRowResponse> comparingNumber(Function<ReviewDataRecordRowResponse, ? extends Number> extractor) {
    return Comparator.comparing(
        row -> extractor.apply(row) == null ? null : extractor.apply(row).doubleValue(),
        Comparator.nullsLast(Double::compareTo));
  }

  private String normalizeSortField(String value) {
    return switch (TextQuerySupport.normalizeDisplay(value)) {
      case "projectName",
          "repositoryName",
          "reviewer",
          "moduleName",
          "reviewDurationMinutes",
          "totalScore",
          "commentRate",
          "defectCount",
          "addedLines",
          "createdAt",
          "updatedAt" -> value;
      default -> "updatedAt";
    };
  }

  private String normalizeSortOrder(String value) {
    return "asc".equalsIgnoreCase(value) ? "asc" : "desc";
  }

  private LocalDate parseDate(String value) {
    String normalized = TextQuerySupport.trimToNull(value);
    if (normalized == null) {
      return null;
    }
    return LocalDate.parse(normalized);
  }

  private int safeInt(Integer value) {
    return value == null ? 0 : value;
  }

  private Double toDouble(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Number number) {
      return number.doubleValue();
    }
    return Double.valueOf(String.valueOf(value));
  }
}
