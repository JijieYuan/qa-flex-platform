package com.data.collection.platform.service;

import com.data.collection.platform.entity.CodeReviewIllegalRecordFilterOptionsResponse;
import com.data.collection.platform.entity.CodeReviewIllegalRecordListResponse;
import com.data.collection.platform.entity.CodeReviewIllegalRecordRowResponse;
import com.data.collection.platform.entity.OptionItemResponse;
import com.data.collection.platform.entity.RealtimeWorkspaceStatusResponse;
import com.data.collection.platform.entity.statistics.StatisticBoardRuleExplanationResponse;
import com.data.collection.platform.entity.statistics.StatisticRuleFlowStep;
import com.data.collection.platform.entity.statistics.StatisticRuleFlowStepSample;
import com.data.collection.platform.entity.statistics.StatisticRuleMetricDefinition;
import com.data.collection.platform.service.FactBuildService;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class CodeReviewIllegalRecordService {
  public static final String WORKSPACE_KEY = "code-review-illegal-records";
  private static final String RULE_VERSION = "code-review-illegal-records@2026-04-08-v3";

  private static final List<String> REALTIME_REFRESH_TABLES = List.of(
      "merge_requests",
      "merge_request_metrics",
      "merge_request_reviewers",
      "merge_request_assignees",
      "label_links",
      "labels",
      "projects",
      "namespaces",
      "users");

  private static final List<OptionItemResponse> REQUEST_TYPE_OPTIONS =
      List.of(new OptionItemResponse("合并请求", "merge_request"));

  private static final String FACT_SQL = """
      select
        merge_request_id,
        merge_request_iid,
        project_id,
        title as merge_request_content,
        project_name,
        repository_name,
        merged_at_source as merged_at,
        merge_user_name as merged_by,
        owner_name as owner,
        target_branch,
        module_name,
        coalesce(label_names, '') as label_names,
        comment_rate,
        defect_count,
        added_lines
      from merge_request_fact
      where deleted = false
      """;

  private final JdbcTemplate jdbcTemplate;
  private final GitlabMirrorSyncService gitlabMirrorSyncService;
  private final RealtimeWorkspaceService realtimeWorkspaceService;
  private final FactBuildService factBuildService;
  private final String defaultGitlabBaseUrl;

  public CodeReviewIllegalRecordService(
      JdbcTemplate jdbcTemplate,
      GitlabMirrorSyncService gitlabMirrorSyncService,
      RealtimeWorkspaceService realtimeWorkspaceService,
      FactBuildService factBuildService,
      @Value("${gitlab-mirror.web-base-url:http://172.22.10.233}") String defaultGitlabBaseUrl) {
    this.jdbcTemplate = jdbcTemplate;
    this.gitlabMirrorSyncService = gitlabMirrorSyncService;
    this.realtimeWorkspaceService = realtimeWorkspaceService;
    this.factBuildService = factBuildService;
    this.defaultGitlabBaseUrl = defaultGitlabBaseUrl;
  }

  public CodeReviewIllegalRecordListResponse listRecords(
      Long projectId,
      String repositoryName,
      String mergedAtStart,
      String mergedAtEnd,
      String keyword,
      String projectName,
      String requestType,
      String targetBranch,
      String mergedBy,
      String moduleName,
      String illegalType,
      String mergeRequestIid,
      String owner,
      int page,
      int size,
      String sortField,
      String sortOrder) {
    int safePage = page <= 0 ? 1 : page;
    int safeSize = size <= 0 ? 20 : Math.min(size, 100);
    String safeSortField = normalizeSortField(sortField);
    String safeSortOrder = normalizeSortOrder(sortOrder);

    List<IllegalRecordView> filtered = loadSources().stream()
        .map(this::toView)
        .filter(row -> !row.illegalTypes().isEmpty())
        .filter(row -> matchesProjectId(row, projectId))
        .filter(row -> matchesEquals(row.repositoryName(), repositoryName))
        .filter(row -> matchesDateRange(row.mergedAt(), mergedAtStart, mergedAtEnd))
        .filter(row -> matchesKeyword(row, keyword))
        .filter(row -> matchesEquals(row.projectName(), projectName))
        .filter(row -> matchesRequestType(row.requestType(), requestType))
        .filter(row -> matchesEquals(row.targetBranch(), targetBranch))
        .filter(row -> matchesEquals(row.mergedBy(), mergedBy))
        .filter(row -> matchesEquals(row.moduleName(), moduleName))
        .filter(row -> matchesIllegalType(row.illegalTypes(), illegalType))
        .filter(row -> matchesNumeric(row.mergeRequestIid(), mergeRequestIid))
        .filter(row -> matchesEquals(row.owner(), owner))
        .sorted(buildComparator(safeSortField, safeSortOrder))
        .toList();

    long total = filtered.size();
    int fromIndex = Math.min((safePage - 1) * safeSize, filtered.size());
    int toIndex = Math.min(fromIndex + safeSize, filtered.size());
    List<CodeReviewIllegalRecordRowResponse> records = filtered.subList(fromIndex, toIndex).stream()
        .map(this::toResponse)
        .toList();

    return new CodeReviewIllegalRecordListResponse(records, total, safePage, safeSize, safeSortField, safeSortOrder);
  }

  public CodeReviewIllegalRecordFilterOptionsResponse getFilterOptions(Long projectId) {
    List<IllegalRecordView> rows = loadSources().stream()
        .map(this::toView)
        .filter(row -> matchesProjectId(row, projectId))
        .toList();

    return new CodeReviewIllegalRecordFilterOptionsResponse(
        REQUEST_TYPE_OPTIONS,
        toOptions(rows, IllegalRecordView::repositoryName),
        toOptions(rows.stream().flatMap(row -> row.illegalTypes().stream()).toList()),
        toOptions(rows, IllegalRecordView::targetBranch),
        toOptions(rows, IllegalRecordView::mergedBy),
        toOptions(rows, IllegalRecordView::moduleName),
        toOptions(rows, IllegalRecordView::projectName));
  }

  public RealtimeWorkspaceStatusResponse getRealtimeStatus() {
    return realtimeWorkspaceService.getStatus(WORKSPACE_KEY);
  }

  public RealtimeWorkspaceStatusResponse requestRealtimeRefresh() {
    return realtimeWorkspaceService.requestRefresh(WORKSPACE_KEY, this::refreshMirrorForRealtimeView);
  }

  public StatisticBoardRuleExplanationResponse getRuleExplanation() {
    List<IllegalRecordSource> sources = loadSources();
    List<IllegalRecordView> views = sources.stream().map(this::toView).toList();
    long total = views.size();
    long missingModule = views.stream().filter(row -> row.illegalTypes().contains("缺少模块标签")).count();
    long missingOwner = views.stream().filter(row -> row.illegalTypes().contains("缺少标注责任人")).count();
    long missingMetrics = views.stream().filter(row -> row.illegalTypes().stream().anyMatch(type -> type.contains("缺少代码注释比例") || type.contains("缺少缺陷数量") || type.contains("缺少新增代码行数"))).count();

    return new StatisticBoardRuleExplanationResponse(
        WORKSPACE_KEY,
        true,
        "代码走查非法记录规则说明",
        RULE_VERSION,
        "当前统计范围是已归一化到事实表中的 Merge Request 相关数据；页面查询条件在此基础上继续筛选。",
        "非法记录明细页的核心目的是说明“哪些字段缺失、为什么被判定为非法”，当前判定基于 merge_request_fact 中的统一字段。",
        List.of(
            new StatisticRuleFlowStep(
                "source-load",
                "加载合并请求事实",
                "从 merge_request_fact 中读取已经归一化的合并请求、责任人、模块和指标数据。",
                total,
                total,
                views.stream().limit(3).map(this::toIllegalRecordSample).toList()),
            new StatisticRuleFlowStep(
                "missing-module-check",
                "检查模块标签",
                "若标签数组为空，则判定为“缺少模块标签”。",
                total,
                missingModule,
                views.stream().filter(row -> row.illegalTypes().contains("缺少模块标签")).limit(3).map(this::toIllegalRecordSample).toList()),
            new StatisticRuleFlowStep(
                "missing-owner-check",
                "检查标注责任人",
                "若责任人为空，则判定为“缺少标注责任人”。",
                total,
                missingOwner,
                views.stream().filter(row -> row.illegalTypes().contains("缺少标注责任人")).limit(3).map(this::toIllegalRecordSample).toList()),
            new StatisticRuleFlowStep(
                "missing-metric-check",
                "检查外部指标",
                "若代码注释比例、缺陷数量或新增代码行数缺失，则判定为对应非法类型。",
                total,
                missingMetrics,
                views.stream().filter(row -> row.illegalTypes().stream().anyMatch(type -> type.contains("缺少代码注释比例") || type.contains("缺少缺陷数量") || type.contains("缺少新增代码行数"))).limit(3).map(this::toIllegalRecordSample).toList())),
        List.of(
            new StatisticRuleMetricDefinition("illegalTypes", "非法类型", "系统根据规则检查结果，标记这条记录需要补充哪些信息。", "非法类型 = 缺少模块标签 / 缺少标注责任人 / 缺少外部指标", "用于告诉用户这条记录为什么需要继续处理。"),
            new StatisticRuleMetricDefinition("moduleName", "模块名称", "表示这条合并请求所属的功能模块。", "模块名称来自合并请求关联的模块标识", "如果缺少模块信息，这条记录会被判定为需要关注。"),
            new StatisticRuleMetricDefinition("commentRate", "代码注释比例", "表示本次改动中代码注释的覆盖情况。", "代码注释比例 = 导入表中的外部工具结果 或 MR 机器人解析结果", "如果缺少该指标，这条记录会被判定为需要关注。"),
            new StatisticRuleMetricDefinition("defectCount", "缺陷数量", "表示本次改动关联的缺陷数量。", "缺陷数量 = 导入表中的 MR 评论 / 机器人结果 / Sonar 汇总结果", "如果缺少该指标，这条记录会被判定为需要关注。"),
            new StatisticRuleMetricDefinition("addedLines", "新增代码行数", "表示本次合并请求新增的代码规模。", "新增代码行数 = 本次改动新增代码行数", "用于辅助判断记录是否完整。")),
        null);
  }

  private void refreshMirrorForRealtimeView() {
    try {
      gitlabMirrorSyncService.refreshTablesOnDemand(REALTIME_REFRESH_TABLES, "code-review-illegal-records");
      factBuildService.rebuildMergeRequestFacts(false);
    } catch (Exception e) {
      log.warn("On-demand mirror refresh for code review illegal records failed, fallback to current mirror snapshot", e);
    }
  }

  private List<IllegalRecordSource> loadSources() {
    try {
      List<IllegalRecordSource> facts = ensureFactsReady();
      if (!facts.isEmpty()) {
        return facts;
      }
    } catch (DataAccessException e) {
      log.warn("Failed to load merge request facts", e);
      return List.of();
    }
    return List.of();
  }

  private List<IllegalRecordSource> ensureFactsReady() {
    List<IllegalRecordSource> facts = jdbcTemplate.query(FACT_SQL, this::mapFactSource);
    if (!facts.isEmpty()) {
      return facts;
    }
    factBuildService.rebuildMergeRequestFacts(true);
    return jdbcTemplate.query(FACT_SQL, this::mapFactSource);
  }

  private IllegalRecordSource mapFactSource(ResultSet rs, int rowNum) throws SQLException {
    return new IllegalRecordSource(
        rs.getLong("merge_request_id"),
        rs.getInt("merge_request_iid"),
        rs.getLong("project_id"),
        rs.getString("merge_request_content"),
        rs.getString("project_name"),
        rs.getString("repository_name"),
        rs.getTimestamp("merged_at") == null ? null : rs.getTimestamp("merged_at").toLocalDateTime(),
        rs.getString("merged_by"),
        rs.getString("owner"),
        rs.getString("target_branch"),
        rs.getString("module_name"),
        splitLabels(rs.getString("label_names")),
        toDouble(rs.getObject("comment_rate")),
        (Integer) rs.getObject("defect_count"),
        (Integer) rs.getObject("added_lines"));
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

  private List<String> splitLabels(String labelNames) {
    if (!StringUtils.hasText(labelNames)) {
      return List.of();
    }
    List<String> result = new ArrayList<>();
    for (String value : labelNames.split(",")) {
      String normalized = normalizeText(value);
      if (normalized != null) {
        result.add(normalized);
      }
    }
    return result;
  }

  private IllegalRecordView toView(IllegalRecordSource source) {
    List<String> illegalTypes = buildIllegalTypes(source);
    String mergeRequestLink = buildMergeRequestLink(source.repositoryName(), source.mergeRequestIid());
    return new IllegalRecordView(
        "merge_request",
        source.mergeRequestId(),
        source.mergeRequestIid(),
        source.projectId(),
        normalizeDisplay(source.mergeRequestContent()),
        mergeRequestLink,
        normalizeDisplay(source.owner()),
        normalizeDisplay(source.projectName()),
        normalizeDisplay(source.repositoryName()),
        source.mergedAt(),
        normalizeDisplay(source.mergedBy()),
        normalizeDisplay(source.moduleName()),
        normalizeDisplay(source.targetBranch()),
        illegalTypes,
        source.commentRate(),
        source.defectCount(),
        source.addedLines());
  }

  private StatisticRuleFlowStepSample toIllegalRecordSample(IllegalRecordView row) {
    return new StatisticRuleFlowStepSample(
        "MR #" + row.mergeRequestIid(),
        row.projectName() + " | " + (row.illegalTypes().isEmpty() ? "无非法类型" : String.join("、", row.illegalTypes())));
  }

  private List<String> buildIllegalTypes(IllegalRecordSource source) {
    List<String> result = new ArrayList<>();
    if (source.labelTitles().isEmpty()) {
      result.add("缺少模块标签");
    }
    if (!StringUtils.hasText(source.owner())) {
      result.add("缺少标注责任人");
    }
    if (source.commentRate() == null) {
      result.add("缺少代码注释比例");
    }
    if (source.defectCount() == null) {
      result.add("缺少缺陷数量");
    }
    if (source.addedLines() == null) {
      result.add("缺少新增代码行数");
    }
    return result;
  }

  private String buildMergeRequestLink(String repositoryName, Integer mergeRequestIid) {
    if (!StringUtils.hasText(defaultGitlabBaseUrl) || !StringUtils.hasText(repositoryName) || mergeRequestIid == null) {
      return null;
    }
    return defaultGitlabBaseUrl.replaceAll("/+$", "")
        + "/"
        + repositoryName
        + "/-/merge_requests/"
        + mergeRequestIid;
  }

  private CodeReviewIllegalRecordRowResponse toResponse(IllegalRecordView row) {
    String link = normalizeText(row.mergeRequestLink());
    return new CodeReviewIllegalRecordRowResponse(
        row.requestType(),
        row.mergeRequestId(),
        row.mergeRequestIid(),
        row.projectId(),
        row.mergeRequestContent(),
        link,
        row.owner(),
        row.projectName(),
        row.repositoryName(),
        row.mergedAt(),
        row.mergedBy(),
        row.moduleName(),
        row.targetBranch(),
        row.illegalTypes(),
        row.commentRate(),
        row.defectCount(),
        row.addedLines());
  }

  private boolean matchesProjectId(IllegalRecordView row, Long projectId) {
    return projectId == null || Objects.equals(row.projectId(), projectId);
  }

  private boolean matchesEquals(String left, String right) {
    String normalizedRight = normalizeText(right);
    return normalizedRight == null || Objects.equals(normalizeText(left), normalizedRight);
  }

  private boolean matchesRequestType(String requestType, String expected) {
    String normalizedExpected = normalizeText(expected);
    return normalizedExpected == null || Objects.equals(requestType, normalizedExpected);
  }

  private boolean matchesIllegalType(List<String> illegalTypes, String expected) {
    String normalizedExpected = normalizeText(expected);
    return normalizedExpected == null || illegalTypes.contains(normalizedExpected);
  }

  private boolean matchesNumeric(Integer value, String expected) {
    String normalizedExpected = normalizeText(expected);
    if (normalizedExpected == null) {
      return true;
    }
    return value != null && normalizedExpected.equals(String.valueOf(value));
  }

  private boolean matchesKeyword(IllegalRecordView row, String keyword) {
    String normalizedKeyword = normalizeText(keyword);
    if (normalizedKeyword == null) {
      return true;
    }
    String lowerKeyword = normalizedKeyword.toLowerCase(Locale.ROOT);
    return contains(row.mergeRequestContent(), lowerKeyword)
        || contains(row.owner(), lowerKeyword)
        || contains(row.projectName(), lowerKeyword)
        || contains(row.repositoryName(), lowerKeyword);
  }

  private boolean contains(String source, String keyword) {
    return source != null && source.toLowerCase(Locale.ROOT).contains(keyword);
  }

  private boolean matchesDateRange(LocalDateTime mergedAt, String mergedAtStart, String mergedAtEnd) {
    LocalDate start = parseDate(mergedAtStart);
    LocalDate end = parseDate(mergedAtEnd);
    if (start == null && end == null) {
      return true;
    }
    if (mergedAt == null) {
      return false;
    }
    LocalDate date = mergedAt.toLocalDate();
    if (start != null && date.isBefore(start)) {
      return false;
    }
    if (end != null && date.isAfter(end)) {
      return false;
    }
    return true;
  }

  private LocalDate parseDate(String value) {
    String normalized = normalizeText(value);
    return normalized == null ? null : LocalDate.parse(normalized);
  }

  private String normalizeSortField(String sortField) {
    return switch (normalizeText(sortField) == null ? "mergedAt" : normalizeText(sortField)) {
      case "mergeRequestIid",
           "mergeRequestContent",
           "owner",
           "projectName",
           "mergedAt",
           "mergedBy",
           "moduleName",
           "targetBranch",
           "commentRate",
           "defectCount",
           "addedLines" -> normalizeText(sortField) == null ? "mergedAt" : normalizeText(sortField);
      default -> "mergedAt";
    };
  }

  private String normalizeSortOrder(String sortOrder) {
    String normalized = normalizeText(sortOrder);
    return "asc".equalsIgnoreCase(normalized) ? "asc" : "desc";
  }

  private Comparator<IllegalRecordView> buildComparator(String sortField, String sortOrder) {
    Comparator<IllegalRecordView> comparator = switch (sortField) {
      case "mergeRequestIid" -> Comparator.comparing(IllegalRecordView::mergeRequestIid, Comparator.nullsLast(Integer::compareTo));
      case "mergeRequestContent" -> Comparator.comparing(IllegalRecordView::mergeRequestContent, Comparator.nullsLast(String::compareToIgnoreCase));
      case "owner" -> Comparator.comparing(IllegalRecordView::owner, Comparator.nullsLast(String::compareToIgnoreCase));
      case "projectName" -> Comparator.comparing(IllegalRecordView::projectName, Comparator.nullsLast(String::compareToIgnoreCase));
      case "mergedBy" -> Comparator.comparing(IllegalRecordView::mergedBy, Comparator.nullsLast(String::compareToIgnoreCase));
      case "moduleName" -> Comparator.comparing(IllegalRecordView::moduleName, Comparator.nullsLast(String::compareToIgnoreCase));
      case "targetBranch" -> Comparator.comparing(IllegalRecordView::targetBranch, Comparator.nullsLast(String::compareToIgnoreCase));
      case "commentRate" -> Comparator.comparing(IllegalRecordView::commentRate, Comparator.nullsLast(Double::compareTo));
      case "defectCount" -> Comparator.comparing(IllegalRecordView::defectCount, Comparator.nullsLast(Integer::compareTo));
      case "addedLines" -> Comparator.comparing(IllegalRecordView::addedLines, Comparator.nullsLast(Integer::compareTo));
      default -> Comparator.comparing(IllegalRecordView::mergedAt, Comparator.nullsLast(LocalDateTime::compareTo));
    };
    Comparator<IllegalRecordView> tieBreaker = Comparator
        .comparing(IllegalRecordView::mergedAt, Comparator.nullsLast(LocalDateTime::compareTo))
        .thenComparing(IllegalRecordView::mergeRequestIid, Comparator.nullsLast(Integer::compareTo));
    Comparator<IllegalRecordView> combined = comparator.thenComparing(tieBreaker);
    return "asc".equalsIgnoreCase(sortOrder) ? combined : combined.reversed();
  }

  private List<OptionItemResponse> toOptions(List<IllegalRecordView> rows, Function<IllegalRecordView, String> extractor) {
    return toOptions(rows.stream().map(extractor).toList());
  }

  private List<OptionItemResponse> toOptions(Collection<String> values) {
    Set<String> normalized = values.stream()
        .map(this::normalizeText)
        .filter(Objects::nonNull)
        .collect(Collectors.toCollection(LinkedHashSet::new));
    List<OptionItemResponse> options = new ArrayList<>();
    for (String value : normalized) {
      options.add(new OptionItemResponse(value, value));
    }
    options.sort(Comparator.comparing(OptionItemResponse::label, String::compareToIgnoreCase));
    return options;
  }

  private String normalizeDisplay(String value) {
    String normalized = normalizeText(value);
    return normalized == null ? "" : normalized;
  }

  private String normalizeText(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    return value.trim();
  }

  private record IllegalRecordSource(
      Long mergeRequestId,
      Integer mergeRequestIid,
      Long projectId,
      String mergeRequestContent,
      String projectName,
      String repositoryName,
      LocalDateTime mergedAt,
      String mergedBy,
      String owner,
      String targetBranch,
      String moduleName,
      List<String> labelTitles,
      Double commentRate,
      Integer defectCount,
      Integer addedLines) {
  }

  private record IllegalRecordView(
      String requestType,
      Long mergeRequestId,
      Integer mergeRequestIid,
      Long projectId,
      String mergeRequestContent,
      String mergeRequestLink,
      String owner,
      String projectName,
      String repositoryName,
      LocalDateTime mergedAt,
      String mergedBy,
      String moduleName,
      String targetBranch,
      List<String> illegalTypes,
      Double commentRate,
      Integer defectCount,
      Integer addedLines) {
  }
}

