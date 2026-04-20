package com.data.collection.platform.service;

import com.data.collection.platform.common.JsonUtils;
import com.data.collection.platform.entity.OptionItemResponse;
import com.data.collection.platform.entity.ReviewDataFilterOptionsResponse;
import com.data.collection.platform.entity.ReviewDataProblemItemResponse;
import com.data.collection.platform.entity.ReviewDataProblemItemSaveRequest;
import com.data.collection.platform.entity.ReviewDataRecordDetailResponse;
import com.data.collection.platform.entity.ReviewDataRecordListResponse;
import com.data.collection.platform.entity.ReviewDataRecordRowResponse;
import com.data.collection.platform.entity.ReviewDataRecordSaveRequest;
import com.data.collection.platform.entity.ReviewDataSummaryResponse;
import com.data.collection.platform.entity.statistics.StatisticFilterCondition;
import com.data.collection.platform.entity.statistics.StatisticFilterGroup;
import com.fasterxml.jackson.core.type.TypeReference;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
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
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewDataRecordService {
  private static final List<OptionItemResponse> REVIEW_TYPE_OPTIONS =
      List.of(
          new OptionItemResponse("需求说明书评审", "需求说明书评审"),
          new OptionItemResponse("设计说明书评审", "设计说明书评审"),
          new OptionItemResponse("产品用户手册", "产品用户手册"),
          new OptionItemResponse("项目计划评审", "项目计划评审"),
          new OptionItemResponse("其他", "其他"));

  private static final List<OptionItemResponse> REVIEW_CATEGORY_OPTIONS =
      List.of(
          new OptionItemResponse("走查", "走查"),
          new OptionItemResponse("独立评审", "独立评审"),
          new OptionItemResponse("会议评审", "会议评审"));

  private static final List<OptionItemResponse> PROBLEM_CATEGORY_OPTIONS =
      List.of(
          new OptionItemResponse("文档规范", "文档规范"),
          new OptionItemResponse("完整性", "完整性"),
          new OptionItemResponse("功能性", "功能性"),
          new OptionItemResponse("可行性", "可行性"),
          new OptionItemResponse("无问题", "无问题"));

  private static final List<OptionItemResponse> PROBLEM_STATUS_OPTIONS =
      List.of(
          new OptionItemResponse("新提交", "新提交"),
          new OptionItemResponse("已修复", "已修复"),
          new OptionItemResponse("已关闭", "已关闭"),
          new OptionItemResponse("已拒绝", "已拒绝"),
          new OptionItemResponse("无问题", "无问题"),
          new OptionItemResponse("未评审", "未评审"));

  private static final Map<String, List<String>> FILTER_OPERATORS =
      Map.ofEntries(
          Map.entry("title", List.of("contains", "eq", "ne", "isEmpty", "isNotEmpty")),
          Map.entry("projectName", List.of("eq", "ne", "isEmpty", "isNotEmpty")),
          Map.entry("moduleName", List.of("eq", "ne", "isEmpty", "isNotEmpty")),
          Map.entry("reviewOwner", List.of("eq", "ne", "isEmpty", "isNotEmpty")),
          Map.entry("reviewType", List.of("eq", "ne", "isEmpty", "isNotEmpty")),
          Map.entry("reviewExpert", List.of("eq", "ne", "isEmpty", "isNotEmpty")),
          Map.entry("problemStatus", List.of("eq", "ne", "isEmpty", "isNotEmpty")),
          Map.entry("reviewScalePages", List.of("eq", "gt", "gte", "lt", "lte", "between")),
          Map.entry("problemCount", List.of("eq", "gt", "gte", "lt", "lte", "between")),
          Map.entry("problemDensity", List.of("eq", "gt", "gte", "lt", "lte", "between")),
          Map.entry("reviewDate", List.of("day", "before", "after", "between")));

  private static final String BASE_LIST_SQL =
      """
      select
        r.id,
        r.project_name,
        r.title,
        r.module_name,
        r.review_type,
        r.review_date,
        r.review_owner,
        r.review_scale_pages,
        r.review_product,
        r.author_name,
        r.review_version,
        r.updated_at,
        r.deleted,
        coalesce(expert.expert_names, '') as review_experts_summary,
        coalesce(problem.problem_count, 0) as problem_count
      from review_records r
      left join (
        select
          review_record_id,
          string_agg(expert_name, '、' order by sort_order asc, id asc) as expert_names
        from review_record_experts
        where deleted = false
        group by review_record_id
      ) expert on expert.review_record_id = r.id
      left join (
        select
          review_record_id,
          count(*)::integer as problem_count
        from review_problem_items
        where deleted = false
        group by review_record_id
      ) problem on problem.review_record_id = r.id
      where r.deleted = false
      """;

  private final JdbcTemplate jdbcTemplate;
  private final JsonUtils jsonUtils;

  public ReviewDataRecordService(JdbcTemplate jdbcTemplate, JsonUtils jsonUtils) {
    this.jdbcTemplate = jdbcTemplate;
    this.jsonUtils = jsonUtils;
  }

  public ReviewDataRecordListResponse listRecords(
      String title,
      String projectName,
      String moduleName,
      String reviewOwner,
      String reviewType,
      String problemStatus,
      String reviewExpert,
      String filterGroupJson,
      int page,
      int size,
      String sortField,
      String sortOrder) {
    int safePage = page <= 0 ? 1 : page;
    int safeSize = size <= 0 ? 20 : Math.min(size, 100);
    String safeSortField = normalizeSortField(sortField);
    String safeSortOrder = normalizeSortOrder(sortOrder);

    List<ReviewDataRecordRowResponse> legacyFiltered =
        loadRecords(title, projectName, moduleName, reviewOwner, reviewType, problemStatus, reviewExpert);
    StatisticFilterGroup filterGroup = parseFilterGroup(filterGroupJson);
    Map<Long, List<String>> problemStatusesByRecordId =
        needsField(filterGroup, "problemStatus") ? loadProblemStatusesByRecordIds(legacyFiltered) : Map.of();
    List<ReviewDataRecordRowResponse> filtered =
        legacyFiltered.stream()
            .filter(row -> matchesFilterGroup(row, filterGroup, problemStatusesByRecordId))
            .sorted(buildComparator(safeSortField, safeSortOrder))
            .toList();

    PageSlice<ReviewDataRecordRowResponse> pageSlice =
        PageSliceSupport.slice(filtered, safePage, safeSize);

    return new ReviewDataRecordListResponse(
        pageSlice.records(),
        pageSlice.total(),
        pageSlice.page(),
        pageSlice.size(),
        safeSortField,
        safeSortOrder,
        buildSummary(filtered));
  }

  public ReviewDataFilterOptionsResponse getFilterOptions() {
    List<ReviewDataRecordRowResponse> records = loadRecords(null, null, null, null, null, null, null);

    return new ReviewDataFilterOptionsResponse(
        toOptions(records.stream().map(ReviewDataRecordRowResponse::projectName).toList()),
        toOptions(records.stream().map(ReviewDataRecordRowResponse::moduleName).toList()),
        toOptions(records.stream().map(ReviewDataRecordRowResponse::reviewOwner).toList()),
        REVIEW_TYPE_OPTIONS,
        loadExpertOptions(),
        PROBLEM_STATUS_OPTIONS,
        REVIEW_CATEGORY_OPTIONS,
        PROBLEM_CATEGORY_OPTIONS);
  }

  public ReviewDataRecordDetailResponse getRecordDetail(Long recordId) {
    ReviewDataRecordRowResponse record = getRecordOrThrow(recordId);
    return new ReviewDataRecordDetailResponse(record, listRecordExperts(recordId), listProblemItems(recordId));
  }

  public List<ReviewDataProblemItemResponse> listProblemItems(Long recordId) {
    assertRecordExists(recordId);
    return jdbcTemplate.query(
        """
        select
          id,
          review_record_id,
          reviewer_name,
          workload_hours,
          review_category,
          document_position,
          problem_category,
          problem_description,
          suggested_solution,
          owner_name,
          rejection_reason,
          problem_status,
          updated_at
        from review_problem_items
        where review_record_id = ? and deleted = false
        order by updated_at desc, id desc
        """,
        this::mapProblemItem,
        recordId);
  }

  @Transactional
  public ReviewDataRecordDetailResponse createRecord(ReviewDataRecordSaveRequest request) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(
        connection -> {
          PreparedStatement statement =
              connection.prepareStatement(
                  """
                  insert into review_records(
                    project_name,
                    title,
                    module_name,
                    review_type,
                    review_date,
                    review_owner,
                    review_scale_pages,
                    review_product,
                    author_name,
                    review_version,
                    created_at,
                    updated_at
                  ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, current_timestamp, current_timestamp)
                  """,
                  new String[] {"id"});
          statement.setString(1, normalizeText(request.projectName()));
          statement.setString(2, normalizeText(request.title()));
          statement.setString(3, normalizeText(request.moduleName()));
          statement.setString(4, normalizeText(request.reviewType()));
          statement.setDate(5, request.reviewDate() == null ? null : Date.valueOf(request.reviewDate()));
          statement.setString(6, normalizeText(request.reviewOwner()));
          statement.setInt(7, safeInt(request.reviewScalePages()));
          statement.setString(8, normalizeText(request.reviewProduct()));
          statement.setString(9, normalizeText(request.authorName()));
          statement.setString(10, normalizeText(request.reviewVersion()));
          return statement;
        },
        keyHolder);

    Long recordId = keyHolder.getKey() == null ? null : keyHolder.getKey().longValue();
    if (recordId == null) {
      throw new IllegalStateException("创建评审记录失败");
    }

    replaceExperts(recordId, request.reviewExperts());
    return getRecordDetail(recordId);
  }

  @Transactional
  public ReviewDataRecordDetailResponse updateRecord(Long recordId, ReviewDataRecordSaveRequest request) {
    assertRecordExists(recordId);
    jdbcTemplate.update(
        """
        update review_records
        set
          project_name = ?,
          title = ?,
          module_name = ?,
          review_type = ?,
          review_date = ?,
          review_owner = ?,
          review_scale_pages = ?,
          review_product = ?,
          author_name = ?,
          review_version = ?,
          updated_at = current_timestamp
        where id = ?
        """,
        normalizeText(request.projectName()),
        normalizeText(request.title()),
        normalizeText(request.moduleName()),
        normalizeText(request.reviewType()),
        request.reviewDate() == null ? null : Date.valueOf(request.reviewDate()),
        normalizeText(request.reviewOwner()),
        safeInt(request.reviewScalePages()),
        normalizeText(request.reviewProduct()),
        normalizeText(request.authorName()),
        normalizeText(request.reviewVersion()),
        recordId);

    replaceExperts(recordId, request.reviewExperts());
    return getRecordDetail(recordId);
  }

  @Transactional
  public void deleteRecord(Long recordId) {
    assertRecordExists(recordId);
    jdbcTemplate.update(
        "update review_records set deleted = true, updated_at = current_timestamp where id = ?", recordId);
  }

  @Transactional
  public ReviewDataProblemItemResponse createProblemItem(
      Long recordId, ReviewDataProblemItemSaveRequest request) {
    assertRecordExists(recordId);
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(
        connection -> {
          PreparedStatement statement =
              connection.prepareStatement(
                  """
                  insert into review_problem_items(
                    review_record_id,
                    reviewer_name,
                    workload_hours,
                    review_category,
                    document_position,
                    problem_category,
                    problem_description,
                    suggested_solution,
                    owner_name,
                    rejection_reason,
                    problem_status,
                    created_at,
                    updated_at
                  ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, current_timestamp, current_timestamp)
                  """,
                  new String[] {"id"});
          statement.setLong(1, recordId);
          statement.setString(2, normalizeText(request.reviewerName()));
          statement.setBigDecimal(3, BigDecimal.valueOf(safeDouble(request.workloadHours())));
          statement.setString(4, normalizeText(request.reviewCategory()));
          statement.setString(5, normalizeNullableText(request.documentPosition()));
          statement.setString(6, normalizeText(request.problemCategory()));
          statement.setString(7, normalizeText(request.problemDescription()));
          statement.setString(8, normalizeNullableText(request.suggestedSolution()));
          statement.setString(9, normalizeNullableText(request.ownerName()));
          statement.setString(10, normalizeNullableText(request.rejectionReason()));
          statement.setString(11, normalizeText(request.problemStatus()));
          return statement;
        },
        keyHolder);

    Long itemId = keyHolder.getKey() == null ? null : keyHolder.getKey().longValue();
    if (itemId == null) {
      throw new IllegalStateException("创建评审问题失败");
    }
    touchRecord(recordId);
    return getProblemItemOrThrow(recordId, itemId);
  }

  @Transactional
  public ReviewDataProblemItemResponse updateProblemItem(
      Long recordId, Long itemId, ReviewDataProblemItemSaveRequest request) {
    assertRecordExists(recordId);
    assertProblemItemExists(recordId, itemId);
    jdbcTemplate.update(
        """
        update review_problem_items
        set
          reviewer_name = ?,
          workload_hours = ?,
          review_category = ?,
          document_position = ?,
          problem_category = ?,
          problem_description = ?,
          suggested_solution = ?,
          owner_name = ?,
          rejection_reason = ?,
          problem_status = ?,
          updated_at = current_timestamp
        where id = ? and review_record_id = ?
        """,
        normalizeText(request.reviewerName()),
        BigDecimal.valueOf(safeDouble(request.workloadHours())),
        normalizeText(request.reviewCategory()),
        normalizeNullableText(request.documentPosition()),
        normalizeText(request.problemCategory()),
        normalizeText(request.problemDescription()),
        normalizeNullableText(request.suggestedSolution()),
        normalizeNullableText(request.ownerName()),
        normalizeNullableText(request.rejectionReason()),
        normalizeText(request.problemStatus()),
        itemId,
        recordId);
    touchRecord(recordId);
    return getProblemItemOrThrow(recordId, itemId);
  }

  @Transactional
  public void deleteProblemItem(Long recordId, Long itemId) {
    assertRecordExists(recordId);
    assertProblemItemExists(recordId, itemId);
    jdbcTemplate.update(
        """
        update review_problem_items
        set deleted = true, updated_at = current_timestamp
        where id = ? and review_record_id = ?
        """,
        itemId,
        recordId);
    touchRecord(recordId);
  }

  private List<ReviewDataRecordRowResponse> loadRecords(
      String title,
      String projectName,
      String moduleName,
      String reviewOwner,
      String reviewType,
      String problemStatus,
      String reviewExpert) {
    StringBuilder sql = new StringBuilder(BASE_LIST_SQL);
    List<Object> args = new ArrayList<>();

    appendContains(sql, args, "r.title", title);
    appendContains(sql, args, "r.project_name", projectName);
    appendContains(sql, args, "r.module_name", moduleName);
    appendContains(sql, args, "r.review_owner", reviewOwner);
    appendEqText(sql, args, "r.review_type", reviewType);
    appendProblemStatusFilter(sql, args, problemStatus);
    appendReviewExpertFilter(sql, args, reviewExpert);

    return jdbcTemplate.query(sql.toString(), this::mapRecordRow, args.toArray());
  }

  private StatisticFilterGroup parseFilterGroup(String filterGroupJson) {
    String normalized = TextQuerySupport.trimToNull(filterGroupJson);
    if (normalized == null) {
      return new StatisticFilterGroup("AND", List.of());
    }
    StatisticFilterGroup parsed = jsonUtils.fromJson(normalized, new TypeReference<>() {});
    if (parsed == null || parsed.conditions() == null || parsed.conditions().isEmpty()) {
      return new StatisticFilterGroup("AND", List.of());
    }
    List<StatisticFilterCondition> conditions =
        parsed.conditions().stream()
            .map(this::normalizeFilterCondition)
            .filter(Objects::nonNull)
            .toList();
    return new StatisticFilterGroup("OR".equalsIgnoreCase(parsed.logic()) ? "OR" : "AND", conditions);
  }

  private StatisticFilterCondition normalizeFilterCondition(StatisticFilterCondition condition) {
    if (condition == null) {
      return null;
    }
    String fieldKey = TextQuerySupport.trimToNull(condition.fieldKey());
    String operator = TextQuerySupport.trimToNull(condition.operator());
    if (fieldKey == null || operator == null || !FILTER_OPERATORS.getOrDefault(fieldKey, List.of()).contains(operator)) {
      return null;
    }
    String value = TextQuerySupport.trimToNull(condition.value());
    String secondaryValue = TextQuerySupport.trimToNull(condition.secondaryValue());
    if (requiresPrimaryValue(operator) && value == null) {
      return null;
    }
    if ("between".equals(operator) && secondaryValue == null) {
      return null;
    }
    return new StatisticFilterCondition(fieldKey, operator, value, secondaryValue);
  }

  private boolean needsField(StatisticFilterGroup filterGroup, String fieldKey) {
    return filterGroup != null
        && filterGroup.conditions() != null
        && filterGroup.conditions().stream().anyMatch(condition -> fieldKey.equals(condition.fieldKey()));
  }

  private boolean matchesFilterGroup(
      ReviewDataRecordRowResponse row,
      StatisticFilterGroup filterGroup,
      Map<Long, List<String>> problemStatusesByRecordId) {
    if (filterGroup == null || filterGroup.conditions() == null || filterGroup.conditions().isEmpty()) {
      return true;
    }
    boolean isOr = "OR".equalsIgnoreCase(filterGroup.logic());
    for (StatisticFilterCondition condition : filterGroup.conditions()) {
      boolean matched = matchesCondition(row, condition, problemStatusesByRecordId);
      if (isOr && matched) {
        return true;
      }
      if (!isOr && !matched) {
        return false;
      }
    }
    return !isOr;
  }

  private boolean matchesCondition(
      ReviewDataRecordRowResponse row,
      StatisticFilterCondition condition,
      Map<Long, List<String>> problemStatusesByRecordId) {
    List<String> values = valuesForField(row, condition.fieldKey(), problemStatusesByRecordId);
    return switch (condition.operator()) {
      case "isEmpty" -> values.stream().allMatch(value -> TextQuerySupport.trimToNull(value) == null);
      case "isNotEmpty" -> values.stream().anyMatch(value -> TextQuerySupport.trimToNull(value) != null);
      case "ne" -> values.stream().noneMatch(value -> equalsIgnoreCase(value, condition.value()));
      case "contains" -> values.stream().anyMatch(value -> containsIgnoreCase(value, condition.value()));
      case "notContains" -> values.stream().noneMatch(value -> containsIgnoreCase(value, condition.value()));
      case "gt", "gte", "lt", "lte", "between" -> values.stream().anyMatch(value -> matchesNumber(value, condition));
      case "day" -> values.stream().anyMatch(value -> Objects.equals(firstDatePart(value), condition.value()));
      case "before" -> values.stream().anyMatch(value -> compareText(firstDatePart(value), firstDatePart(condition.value())) < 0);
      case "after" -> values.stream().anyMatch(value -> compareText(firstDatePart(value), firstDatePart(condition.value())) > 0);
      default -> values.stream().anyMatch(value -> equalsIgnoreCase(value, condition.value()));
    };
  }

  private List<String> valuesForField(
      ReviewDataRecordRowResponse row,
      String fieldKey,
      Map<Long, List<String>> problemStatusesByRecordId) {
    return switch (fieldKey) {
      case "title" -> List.of(Objects.toString(row.title(), ""));
      case "projectName" -> List.of(Objects.toString(row.projectName(), ""));
      case "moduleName" -> List.of(Objects.toString(row.moduleName(), ""));
      case "reviewOwner" -> List.of(Objects.toString(row.reviewOwner(), ""));
      case "reviewType" -> List.of(Objects.toString(row.reviewType(), ""));
      case "reviewExpert" -> splitMultiValue(row.reviewExpertsSummary());
      case "problemStatus" -> problemStatusesByRecordId.getOrDefault(row.id(), List.of());
      case "reviewScalePages" -> List.of(Objects.toString(row.reviewScalePages(), ""));
      case "problemCount" -> List.of(Objects.toString(row.problemCount(), ""));
      case "problemDensity" -> List.of(Objects.toString(row.problemDensity(), ""));
      case "reviewDate" -> List.of(row.reviewDate() == null ? "" : row.reviewDate().toString());
      default -> List.of();
    };
  }

  private Map<Long, List<String>> loadProblemStatusesByRecordIds(List<ReviewDataRecordRowResponse> records) {
    List<Long> recordIds = records.stream().map(ReviewDataRecordRowResponse::id).filter(Objects::nonNull).toList();
    if (recordIds.isEmpty()) {
      return Map.of();
    }
    String placeholders = recordIds.stream().map(id -> "?").collect(Collectors.joining(","));
    return jdbcTemplate.query(
        """
        select review_record_id, problem_status
        from review_problem_items
        where deleted = false and review_record_id in (
        """ + placeholders + ")",
        rs -> {
          Map<Long, List<String>> result = new java.util.HashMap<>();
          while (rs.next()) {
            result.computeIfAbsent(rs.getLong("review_record_id"), ignored -> new ArrayList<>())
                .add(TextQuerySupport.normalizeDisplay(rs.getString("problem_status")));
          }
          return result;
        },
        recordIds.toArray());
  }

  private boolean matchesNumber(String value, StatisticFilterCondition condition) {
    Double left = parseDouble(value);
    Double right = parseDouble(condition.value());
    Double secondary = parseDouble(condition.secondaryValue());
    if (left == null || right == null) {
      return false;
    }
    return switch (condition.operator()) {
      case "gt" -> left > right;
      case "gte" -> left >= right;
      case "lt" -> left < right;
      case "lte" -> left <= right;
      case "between" -> secondary != null && left >= Math.min(right, secondary) && left <= Math.max(right, secondary);
      default -> Double.compare(left, right) == 0;
    };
  }

  private List<String> splitMultiValue(String value) {
    String normalized = TextQuerySupport.trimToNull(value);
    if (normalized == null) {
      return List.of();
    }
    return java.util.Arrays.stream(normalized.split("[、,，]"))
        .map(TextQuerySupport::trimToNull)
        .filter(Objects::nonNull)
        .toList();
  }

  private boolean equalsIgnoreCase(String left, String right) {
    String safeLeft = TextQuerySupport.trimToNull(left);
    String safeRight = TextQuerySupport.trimToNull(right);
    return safeLeft != null && safeRight != null && safeLeft.equalsIgnoreCase(safeRight);
  }

  private boolean containsIgnoreCase(String left, String right) {
    String safeLeft = TextQuerySupport.trimToNull(left);
    String safeRight = TextQuerySupport.trimToNull(right);
    return safeLeft != null && safeRight != null && safeLeft.toLowerCase(Locale.ROOT).contains(safeRight.toLowerCase(Locale.ROOT));
  }

  private int compareText(String left, String right) {
    if (left == null || right == null) {
      return 0;
    }
    return left.compareTo(right);
  }

  private String firstDatePart(String value) {
    String normalized = TextQuerySupport.trimToNull(value);
    return normalized == null ? null : normalized.substring(0, Math.min(normalized.length(), 10));
  }

  private Double parseDouble(String value) {
    String normalized = TextQuerySupport.trimToNull(value);
    if (normalized == null) {
      return null;
    }
    try {
      return Double.parseDouble(normalized);
    } catch (NumberFormatException exception) {
      return null;
    }
  }

  private boolean requiresPrimaryValue(String operator) {
    return !"isEmpty".equals(operator) && !"isNotEmpty".equals(operator);
  }

  private ReviewDataRecordRowResponse mapRecordRow(ResultSet rs, int rowNum) throws SQLException {
    Integer reviewScalePages = (Integer) rs.getObject("review_scale_pages");
    Integer problemCount = (Integer) rs.getObject("problem_count");
    return new ReviewDataRecordRowResponse(
        rs.getLong("id"),
        TextQuerySupport.normalizeDisplay(rs.getString("project_name")),
        TextQuerySupport.normalizeDisplay(rs.getString("title")),
        TextQuerySupport.normalizeDisplay(rs.getString("module_name")),
        TextQuerySupport.normalizeDisplay(rs.getString("review_type")),
        rs.getDate("review_date") == null ? null : rs.getDate("review_date").toLocalDate(),
        TextQuerySupport.normalizeDisplay(rs.getString("review_owner")),
        TextQuerySupport.normalizeDisplay(rs.getString("review_experts_summary")),
        reviewScalePages,
        TextQuerySupport.normalizeDisplay(rs.getString("review_product")),
        TextQuerySupport.normalizeDisplay(rs.getString("author_name")),
        TextQuerySupport.normalizeDisplay(rs.getString("review_version")),
        problemCount == null ? 0 : problemCount,
        calculateProblemDensity(problemCount, reviewScalePages),
        rs.getTimestamp("updated_at") == null ? null : rs.getTimestamp("updated_at").toLocalDateTime(),
        rs.getBoolean("deleted"));
  }

  private ReviewDataProblemItemResponse mapProblemItem(ResultSet rs, int rowNum) throws SQLException {
    return new ReviewDataProblemItemResponse(
        rs.getLong("id"),
        rs.getLong("review_record_id"),
        TextQuerySupport.normalizeDisplay(rs.getString("reviewer_name")),
        rs.getBigDecimal("workload_hours") == null
            ? 0D
            : rs.getBigDecimal("workload_hours").doubleValue(),
        TextQuerySupport.normalizeDisplay(rs.getString("review_category")),
        TextQuerySupport.normalizeDisplay(rs.getString("document_position")),
        TextQuerySupport.normalizeDisplay(rs.getString("problem_category")),
        TextQuerySupport.normalizeDisplay(rs.getString("problem_description")),
        TextQuerySupport.normalizeDisplay(rs.getString("suggested_solution")),
        TextQuerySupport.normalizeDisplay(rs.getString("owner_name")),
        TextQuerySupport.normalizeDisplay(rs.getString("rejection_reason")),
        TextQuerySupport.normalizeDisplay(rs.getString("problem_status")),
        rs.getTimestamp("updated_at") == null ? null : rs.getTimestamp("updated_at").toLocalDateTime());
  }

  private ReviewDataSummaryResponse buildSummary(List<ReviewDataRecordRowResponse> rows) {
    long totalRecords = rows.size();
    long totalProblemItems =
        rows.stream().map(ReviewDataRecordRowResponse::problemCount).filter(Objects::nonNull).mapToLong(Integer::longValue).sum();
    double averageReviewScalePages =
        average(rows.stream().map(ReviewDataRecordRowResponse::reviewScalePages).toList());
    double averageProblemCount =
        average(rows.stream().map(ReviewDataRecordRowResponse::problemCount).toList());
    return new ReviewDataSummaryResponse(
        totalRecords, totalProblemItems, averageReviewScalePages, averageProblemCount);
  }

  private double average(List<? extends Number> values) {
    List<Double> normalized =
        values.stream().filter(Objects::nonNull).map(Number::doubleValue).toList();
    if (normalized.isEmpty()) {
      return 0D;
    }
    return normalized.stream().mapToDouble(Double::doubleValue).average().orElse(0D);
  }

  private List<OptionItemResponse> toOptions(List<String> values) {
    Set<String> distinct = new LinkedHashSet<>();
    values.stream().map(TextQuerySupport::trimToNull).filter(Objects::nonNull).forEach(distinct::add);
    return distinct.stream().map(value -> new OptionItemResponse(value, value)).toList();
  }

  private List<OptionItemResponse> loadExpertOptions() {
    List<String> experts =
        jdbcTemplate.query(
            """
            select distinct expert_name
            from review_record_experts
            where deleted = false and coalesce(expert_name, '') <> ''
            order by expert_name asc
            """,
            (rs, rowNum) -> rs.getString("expert_name"));
    return toOptions(experts);
  }

  private List<String> listRecordExperts(Long recordId) {
    return jdbcTemplate.query(
        """
        select expert_name
        from review_record_experts
        where review_record_id = ? and deleted = false
        order by sort_order asc, id asc
        """,
        (rs, rowNum) -> rs.getString("expert_name"),
        recordId);
  }

  private ReviewDataRecordRowResponse getRecordOrThrow(Long recordId) {
    try {
      return jdbcTemplate.queryForObject(
          BASE_LIST_SQL + " and r.id = ?",
          this::mapRecordRow,
          recordId);
    } catch (EmptyResultDataAccessException exception) {
      throw new IllegalArgumentException("评审记录不存在: " + recordId);
    }
  }

  private ReviewDataProblemItemResponse getProblemItemOrThrow(Long recordId, Long itemId) {
    try {
      return jdbcTemplate.queryForObject(
          """
          select
            id,
            review_record_id,
            reviewer_name,
            workload_hours,
            review_category,
            document_position,
            problem_category,
            problem_description,
            suggested_solution,
            owner_name,
            rejection_reason,
            problem_status,
            updated_at
          from review_problem_items
          where id = ? and review_record_id = ? and deleted = false
          """,
          this::mapProblemItem,
          itemId,
          recordId);
    } catch (EmptyResultDataAccessException exception) {
      throw new IllegalArgumentException("评审问题不存在: " + itemId);
    }
  }

  private void assertRecordExists(Long recordId) {
    Integer exists =
        jdbcTemplate.queryForObject(
            "select count(*) from review_records where id = ? and deleted = false",
            Integer.class,
            recordId);
    if (exists == null || exists == 0) {
      throw new IllegalArgumentException("评审记录不存在: " + recordId);
    }
  }

  private void assertProblemItemExists(Long recordId, Long itemId) {
    Integer exists =
        jdbcTemplate.queryForObject(
            """
            select count(*)
            from review_problem_items
            where review_record_id = ? and id = ? and deleted = false
            """,
            Integer.class,
            recordId,
            itemId);
    if (exists == null || exists == 0) {
      throw new IllegalArgumentException("评审问题不存在: " + itemId);
    }
  }

  private void replaceExperts(Long recordId, List<String> experts) {
    jdbcTemplate.update("delete from review_record_experts where review_record_id = ?", recordId);
    List<String> normalized =
        experts == null
            ? List.of()
            : experts.stream()
                .map(TextQuerySupport::trimToNull)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    for (int index = 0; index < normalized.size(); index++) {
      jdbcTemplate.update(
          """
          insert into review_record_experts(
            review_record_id,
            expert_name,
            sort_order,
            deleted,
            created_at,
            updated_at
          ) values (?, ?, ?, false, current_timestamp, current_timestamp)
          """,
          recordId,
          normalized.get(index),
          index);
    }
  }

  private void touchRecord(Long recordId) {
    jdbcTemplate.update(
        "update review_records set updated_at = current_timestamp where id = ?",
        recordId);
  }

  private void appendContains(StringBuilder sql, List<Object> args, String column, String value) {
    String normalized = TextQuerySupport.trimToNull(value);
    if (normalized == null) {
      return;
    }
    sql.append(" and lower(coalesce(").append(column).append(", '')) like ?");
    args.add("%" + normalized.toLowerCase(Locale.ROOT) + "%");
  }

  private void appendEqText(StringBuilder sql, List<Object> args, String column, String value) {
    String normalized = TextQuerySupport.trimToNull(value);
    if (normalized == null) {
      return;
    }
    sql.append(" and ").append(column).append(" = ?");
    args.add(normalized);
  }

  private void appendProblemStatusFilter(StringBuilder sql, List<Object> args, String problemStatus) {
    String normalized = TextQuerySupport.trimToNull(problemStatus);
    if (normalized == null) {
      return;
    }
    sql.append(
        """
         and exists (
          select 1
          from review_problem_items problem_filter
          where problem_filter.review_record_id = r.id
            and problem_filter.deleted = false
            and problem_filter.problem_status = ?
        )
        """);
    args.add(normalized);
  }

  private void appendReviewExpertFilter(StringBuilder sql, List<Object> args, String reviewExpert) {
    String normalized = TextQuerySupport.trimToNull(reviewExpert);
    if (normalized == null) {
      return;
    }
    sql.append(
        """
         and exists (
          select 1
          from review_record_experts expert_filter
          where expert_filter.review_record_id = r.id
            and expert_filter.deleted = false
            and expert_filter.expert_name = ?
        )
        """);
    args.add(normalized);
  }

  private Comparator<ReviewDataRecordRowResponse> buildComparator(String sortField, String sortOrder) {
    Comparator<ReviewDataRecordRowResponse> comparator =
        switch (sortField) {
          case "projectName" ->
              Comparator.comparing(
                  ReviewDataRecordRowResponse::projectName,
                  Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
          case "moduleName" ->
              Comparator.comparing(
                  ReviewDataRecordRowResponse::moduleName,
                  Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
          case "reviewType" ->
              Comparator.comparing(
                  ReviewDataRecordRowResponse::reviewType,
                  Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
          case "reviewDate" ->
              Comparator.comparing(
                  ReviewDataRecordRowResponse::reviewDate, Comparator.nullsLast(LocalDate::compareTo));
          case "reviewOwner" ->
              Comparator.comparing(
                  ReviewDataRecordRowResponse::reviewOwner,
                  Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
          case "reviewScalePages" ->
              Comparator.comparing(
                  ReviewDataRecordRowResponse::reviewScalePages, Comparator.nullsLast(Integer::compareTo));
          case "problemCount" ->
              Comparator.comparing(
                  ReviewDataRecordRowResponse::problemCount, Comparator.nullsLast(Integer::compareTo));
          case "problemDensity" ->
              Comparator.comparing(
                  ReviewDataRecordRowResponse::problemDensity, Comparator.nullsLast(Double::compareTo));
          case "updatedAt" ->
              Comparator.comparing(
                  ReviewDataRecordRowResponse::updatedAt,
                  Comparator.nullsLast(LocalDateTime::compareTo));
          default ->
              Comparator.comparing(
                  ReviewDataRecordRowResponse::title,
                  Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
        };
    if ("desc".equalsIgnoreCase(sortOrder)) {
      comparator = comparator.reversed();
    }
    return comparator.thenComparing(ReviewDataRecordRowResponse::id, Comparator.nullsLast(Long::compareTo));
  }

  private String normalizeSortField(String sortField) {
    String normalized = TextQuerySupport.trimToNull(sortField);
    if (normalized == null) {
      return "updatedAt";
    }
    return switch (normalized) {
      case "title",
          "projectName",
          "moduleName",
          "reviewType",
          "reviewDate",
          "reviewOwner",
          "reviewScalePages",
          "problemCount",
          "problemDensity",
          "updatedAt" -> normalized;
      default -> "updatedAt";
    };
  }

  private String normalizeSortOrder(String sortOrder) {
    String normalized = TextQuerySupport.trimToNull(sortOrder);
    if ("asc".equalsIgnoreCase(normalized)) {
      return "asc";
    }
    return "desc";
  }

  private Double calculateProblemDensity(Integer problemCount, Integer reviewScalePages) {
    if (problemCount == null || reviewScalePages == null || reviewScalePages <= 0) {
      return 0D;
    }
    return problemCount.doubleValue() / reviewScalePages.doubleValue();
  }

  private String normalizeText(String value) {
    return Objects.requireNonNullElse(TextQuerySupport.trimToNull(value), "");
  }

  private String normalizeNullableText(String value) {
    return TextQuerySupport.trimToNull(value);
  }

  private int safeInt(Integer value) {
    return value == null ? 0 : Math.max(value, 0);
  }

  private double safeDouble(Double value) {
    return value == null ? 0D : Math.max(value, 0D);
  }
}
