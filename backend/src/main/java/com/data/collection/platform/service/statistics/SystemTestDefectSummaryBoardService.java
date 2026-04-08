package com.data.collection.platform.service.statistics;

import com.data.collection.platform.common.JsonUtils;
import com.data.collection.platform.entity.statistics.StatisticBoardDefinition;
import com.data.collection.platform.entity.statistics.StatisticBoardMeta;
import com.data.collection.platform.entity.statistics.StatisticBoardRuleExplanationResponse;
import com.data.collection.platform.entity.statistics.StatisticBoardResponse;
import com.data.collection.platform.entity.statistics.StatisticCellData;
import com.data.collection.platform.entity.statistics.StatisticColumnGroup;
import com.data.collection.platform.entity.statistics.StatisticColumnLeaf;
import com.data.collection.platform.entity.statistics.StatisticDetailColumn;
import com.data.collection.platform.entity.statistics.StatisticDetailRequest;
import com.data.collection.platform.entity.statistics.StatisticDetailResponse;
import com.data.collection.platform.entity.statistics.StatisticFilterField;
import com.data.collection.platform.entity.statistics.StatisticFilterGroup;
import com.data.collection.platform.entity.statistics.StatisticRowData;
import com.data.collection.platform.entity.statistics.StatisticRuleFlowStep;
import com.data.collection.platform.entity.statistics.StatisticRuleFlowStepSample;
import com.data.collection.platform.entity.statistics.StatisticRuleMetricDefinition;
import com.data.collection.platform.service.FactBuildService;
import com.data.collection.platform.service.GitlabMirrorSyncService;
import com.data.collection.platform.entity.RealtimeWorkspaceStatusResponse;
import com.data.collection.platform.service.RealtimeWorkspaceService;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class SystemTestDefectSummaryBoardService extends AbstractStatisticBoardService
    implements RealtimeStatisticBoardSupport, RuleExplainableStatisticBoardSupport {
  private static final String BOARD_KEY = "system-test-defect-summary";
  private static final String RULE_VERSION = "system-test-defect-summary@2026-04-07-v1";
  private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
  private static final List<String> REALTIME_REFRESH_TABLES = List.of(
      "issues",
      "projects",
      "users",
      "label_links",
      "labels");
  private static final List<String> EXCLUDED_LABEL_TOKENS = List.of("功能屏蔽", "已拒绝", "建议");
  private static final List<String> CLOSED_EXCLUDED_LABEL_TOKENS = List.of("申请否决", "需求如此");
  private static final String FACT_SQL = """
      select
        issue_id as id,
        issue_iid as iid,
        title,
        project_id,
        project_name,
        coalesce(author_name, '') as author_name,
        updated_at_source as updated_at,
        closed_at_source as closed_at,
        coalesce(issue_state, 'opened') as issue_state,
        coalesce(severity_level, '') as severity_level,
        coalesce(urgency, '') as urgency,
        coalesce(category, '') as category,
        coalesce(bug_status, '') as bug_status,
        coalesce(label_names, '') as label_names
      from issue_fact
      where deleted = false
      """;

  private final JdbcTemplate jdbcTemplate;
  private final GitlabMirrorSyncService gitlabMirrorSyncService;
  private final RealtimeWorkspaceService realtimeWorkspaceService;
  private final FactBuildService factBuildService;

  public SystemTestDefectSummaryBoardService(
      JdbcTemplate jdbcTemplate,
      JsonUtils jsonUtils,
      GitlabMirrorSyncService gitlabMirrorSyncService,
      RealtimeWorkspaceService realtimeWorkspaceService,
      FactBuildService factBuildService) {
    super(jsonUtils);
    this.jdbcTemplate = jdbcTemplate;
    this.gitlabMirrorSyncService = gitlabMirrorSyncService;
    this.realtimeWorkspaceService = realtimeWorkspaceService;
    this.factBuildService = factBuildService;
  }

  @Override
  public String boardKey() {
    return BOARD_KEY;
  }

  @Override
  protected StatisticBoardDefinition buildDefinition() {
    return new StatisticBoardDefinition(
        BOARD_KEY,
        "系统测试缺陷汇总",
        "基于本地镜像 Issue 数据的真实聚合结果，当前先按所属项目汇总。",
        "",
        "",
        "所属项目",
        List.<StatisticFilterField>of(),
        List.of(
            new StatisticColumnGroup(
                "level1",
                "一级缺陷",
                List.of(
                    new StatisticColumnLeaf("level1_back", "回退数量", true, "count"),
                    new StatisticColumnLeaf("level1_hang", "挂机数量", true, "count"),
                    new StatisticColumnLeaf("level1_others", "其他数量", true, "count"),
                    new StatisticColumnLeaf("level1_fixed", "已修复数量", true, "count"),
                    new StatisticColumnLeaf("level1_rate", "修复率", false, "ratio"))),
            new StatisticColumnGroup(
                "level2",
                "二级缺陷",
                List.of(
                    new StatisticColumnLeaf("level2_total", "总数量", true, "count"),
                    new StatisticColumnLeaf("level2_fixed", "已修复数量", true, "count"),
                    new StatisticColumnLeaf("level2_rate", "修复率", false, "ratio"))),
            new StatisticColumnGroup(
                "level3",
                "三级缺陷",
                List.of(
                    new StatisticColumnLeaf("level3_total", "总数量", true, "count"),
                    new StatisticColumnLeaf("level3_fixed", "已修复数量", true, "count"),
                    new StatisticColumnLeaf("level3_rate", "修复率", false, "ratio"))),
            new StatisticColumnGroup(
                "priority",
                "优先级统计",
                List.of(
                    new StatisticColumnLeaf("p1_count", "P1 数量", true, "count"),
                    new StatisticColumnLeaf("p1_rate", "P1 修复率", false, "ratio"),
                    new StatisticColumnLeaf("p2_count", "P2 数量", true, "count"),
                    new StatisticColumnLeaf("p2_rate", "P2 修复率", false, "ratio"),
                    new StatisticColumnLeaf("p3_count", "P3 数量", true, "count"),
                    new StatisticColumnLeaf("p3_rate", "P3 修复率", false, "ratio"))),
            new StatisticColumnGroup(
                "summary",
                "综合汇总",
                List.of(
                    new StatisticColumnLeaf("totalDefects", "模块总缺陷数", true, "count"),
                    new StatisticColumnLeaf("defectRatio", "模块缺陷占比", false, "ratio"),
                    new StatisticColumnLeaf("closeRate", "缺陷关闭率", false, "ratio"),
                    new StatisticColumnLeaf("unclosedCount", "未关闭缺陷数", true, "count")))),
        List.of(
            new StatisticDetailColumn("iid", "议题编号", 120, 120, true),
            new StatisticDetailColumn("title", "标题", null, 260, true),
            new StatisticDetailColumn("projectName", "所属项目", null, 160, true),
            new StatisticDetailColumn("authorName", "创建人", 140, 140, true),
            new StatisticDetailColumn("state", "状态", 120, 120, true),
            new StatisticDetailColumn("labels", "标签", null, 240, false),
            new StatisticDetailColumn("updatedAt", "更新时间", 180, 180, true)),
        10,
        "当前本地镜像库下没有可展示的系统测试缺陷统计数据。");
  }

  @Override
  protected StatisticBoardResponse doLoadBoard(Map<String, String> filters, StatisticFilterGroup filterGroup) {
    long startedAt = System.currentTimeMillis();
    List<IssueSource> sources = loadBoardScopedSources(filters);
    Map<Long, AggregateBucket> buckets = new LinkedHashMap<>();
    long totalIssueCount = sources.size();
    for (IssueSource issue : sources) {
      AggregateBucket bucket = buckets.computeIfAbsent(
          issue.projectId(),
          key -> new AggregateBucket(issue.projectId(), issue.projectName()));
      bucket.accept(issue);
    }

    List<StatisticRowData> rows = buckets.values().stream()
        .map(bucket -> bucket.toRowData(totalIssueCount))
        .sorted(Comparator.comparing(StatisticRowData::rowLabel, String.CASE_INSENSITIVE_ORDER))
        .toList();

    int columnCount = buildDefinition().columnGroups().stream().mapToInt(group -> group.columns().size()).sum();
    return new StatisticBoardResponse(
        buildDefinition(),
        withoutReservedFilters(filters),
        filterGroup,
        rows,
        new StatisticBoardMeta(LocalDateTime.now(), System.currentTimeMillis() - startedAt, rows.size(), columnCount, 10));
  }

  @Override
  protected StatisticDetailResponse doLoadDetail(StatisticDetailRequest request, StatisticFilterGroup filterGroup) {
    List<IssueSource> sources = loadBoardScopedSources(request.filters()).stream()
        .filter(source -> Objects.equals(String.valueOf(source.projectId()), request.rowKey()))
        .filter(matchesMetric(request.columnKey()))
        .sorted(buildDetailComparator(request.sortField(), request.sortOrder()))
        .toList();

    int safePage = request.page() <= 0 ? 1 : request.page();
    int safeSize = request.size() <= 0 ? 10 : request.size();
    int fromIndex = Math.min((safePage - 1) * safeSize, sources.size());
    int toIndex = Math.min(fromIndex + safeSize, sources.size());
    List<Map<String, Object>> records = sources.subList(fromIndex, toIndex).stream()
        .map(this::toDetailRecord)
        .toList();

    return new StatisticDetailResponse(
        "系统测试缺陷明细",
        "展示统计指标背后的真实 Issue 明细。",
        buildDefinition().detailColumns(),
        records,
        sources.size(),
        safePage,
        safeSize,
        StringUtils.hasText(request.sortField()) ? request.sortField() : "updatedAt",
        "ascending".equalsIgnoreCase(request.sortOrder()) ? "ascending" : "descending");
  }

  private List<IssueSource> loadSources(Map<String, String> filters) {
    Long projectId = parseLong(withoutReservedFilters(filters).get("projectId"));
    try {
      List<IssueSource> facts = ensureFactsReady(projectId);
      if (!facts.isEmpty()) {
        return facts;
      }
    } catch (DataAccessException e) {
      log.warn("Failed to load issue facts", e);
      return List.of();
    }
    return List.of();
  }

  private void refreshMirrorForRealtimeView() {
    try {
      gitlabMirrorSyncService.refreshTablesOnDemand(REALTIME_REFRESH_TABLES, BOARD_KEY);
      factBuildService.rebuildIssueFacts(false);
    } catch (Exception e) {
      log.warn("On-demand mirror refresh for {} failed, fallback to current mirror snapshot", BOARD_KEY, e);
    }
  }

  private List<IssueSource> loadSourcesFromFact(Long projectId) {
    String sql = FACT_SQL + (projectId == null ? "" : " and project_id = ?");
    return projectId == null
        ? jdbcTemplate.query(sql, this::mapIssueFact)
        : jdbcTemplate.query(sql, this::mapIssueFact, projectId);
  }

  private List<IssueSource> ensureFactsReady(Long projectId) {
    List<IssueSource> facts = loadSourcesFromFact(projectId);
    if (!facts.isEmpty()) {
      return facts;
    }
    factBuildService.rebuildIssueFacts(true);
    return loadSourcesFromFact(projectId);
  }

  public RealtimeWorkspaceStatusResponse getRealtimeStatus() {
    return realtimeWorkspaceService.getStatus(BOARD_KEY);
  }

  public RealtimeWorkspaceStatusResponse requestRealtimeRefresh() {
    return realtimeWorkspaceService.requestRefresh(BOARD_KEY, this::refreshMirrorForRealtimeView);
  }

  @Override
  public StatisticBoardRuleExplanationResponse getRuleExplanation(Map<String, String> filters) {
    RuleFlowSnapshot flowSnapshot = buildRuleFlowSnapshot(loadSources(filters));
    return new StatisticBoardRuleExplanationResponse(
        BOARD_KEY,
        true,
        "系统测试缺陷汇总规则说明",
        RULE_VERSION,
        "当前一期基于 issue_fact 执行统计；支持按项目筛选，并在统计前执行标签排除规则。",
        "统计口径、过滤步骤和指标说明全部基于事实表中的统一字段，目的是让使用者看到“事实数据 -> 过滤后 -> 指标汇总”的真实过程。",
        flowSnapshot.flowSteps(),
        buildMetricDefinitions(),
        null);
  }

  private List<IssueSource> loadBoardScopedSources(Map<String, String> filters) {
    return buildRuleFlowSnapshot(loadSources(filters)).finalSources();
  }

  private RuleFlowSnapshot buildRuleFlowSnapshot(List<IssueSource> loadedSources) {
    List<IssueSource> initial = loadedSources == null ? List.of() : List.copyOf(loadedSources);
    List<StatisticRuleFlowStep> steps = new ArrayList<>();

    steps.add(
        new StatisticRuleFlowStep(
            "source-load",
            "加载议题事实",
            "从 issue_fact 中读取当前查询范围内已经归一化的议题记录。",
            initial.size(),
            initial.size(),
            sampleIssues(initial)));

    List<IssueSource> afterLabelExclusion =
        initial.stream()
            .filter(issue -> !issue.hasAnyLabel(EXCLUDED_LABEL_TOKENS))
            .toList();
    steps.add(
        new StatisticRuleFlowStep(
            "exclude-basic-labels",
            "排除基础无效标签",
            "排除携带“功能屏蔽”“已拒绝”“建议”标签的议题。",
            initial.size(),
            afterLabelExclusion.size(),
            sampleIssues(afterLabelExclusion)));

    List<IssueSource> finalSources =
        afterLabelExclusion.stream()
            .filter(issue -> !(issue.isClosed() && issue.hasAnyLabel(CLOSED_EXCLUDED_LABEL_TOKENS)))
            .toList();
    steps.add(
        new StatisticRuleFlowStep(
            "exclude-closed-veto-and-as-designed",
            "排除关闭且无需继续统计的议题",
            "排除关闭状态下携带“申请否决”或“需求如此”标签的议题。",
            afterLabelExclusion.size(),
            finalSources.size(),
            sampleIssues(finalSources)));

    return new RuleFlowSnapshot(finalSources, steps);
  }

  private List<StatisticRuleFlowStepSample> sampleIssues(List<IssueSource> issues) {
    return issues.stream()
        .limit(3)
        .map(
            issue ->
                new StatisticRuleFlowStepSample(
                    "#" + issue.iid() + " " + issue.projectName(),
                    issue.title() + (issue.labels().isEmpty() ? "" : " | 标签: " + String.join(", ", issue.labels()))))
        .toList();
  }

  private List<StatisticRuleMetricDefinition> buildMetricDefinitions() {
    return List.of(
        new StatisticRuleMetricDefinition(
            "level1",
            "一级缺陷",
            "基于事实表中的严重程度和缺陷分类字段识别一级缺陷。",
            "一级缺陷数量 = 回退数量 + 挂机数量 + 其他一级数量",
            "严重程度和分类由事实构建服务统一归一化。"),
        new StatisticRuleMetricDefinition(
            "level2",
            "二级缺陷",
            "基于事实表中的严重程度字段识别二级缺陷。",
            "二级修复率 = 二级已修复数量 / 二级总数量",
            "当分母为 0 时返回 0.00%。"),
        new StatisticRuleMetricDefinition(
            "level3",
            "三级缺陷",
            "基于事实表中的严重程度字段识别三级缺陷。",
            "三级修复率 = 三级已修复数量 / 三级总数量",
            "当分母为 0 时返回 0.00%。"),
        new StatisticRuleMetricDefinition(
            "priority",
            "优先级统计",
            "基于事实表中的优先级字段识别 P1/P2/P3。",
            "Pn 修复率 = Pn 已关闭数量 / Pn 总数量",
            "优先级与严重级别相互独立。"),
        new StatisticRuleMetricDefinition(
            "summary",
            "综合汇总",
            "展示模块总缺陷数、缺陷占比、关闭率和未关闭数量。",
            "缺陷占比 = 当前项目缺陷数 / 当前统计结果中的全部缺陷数",
            "当前实现按项目维度汇总。"));
  }

  private IssueSource mapIssueFact(ResultSet rs, int rowNum) throws SQLException {
    return new IssueSource(
        rs.getLong("id"),
        rs.getInt("iid"),
        rs.getString("title"),
        rs.getLong("project_id"),
        defaultText(rs.getString("project_name"), "未命名项目"),
        defaultText(rs.getString("author_name"), ""),
        rs.getTimestamp("updated_at") == null ? null : rs.getTimestamp("updated_at").toLocalDateTime(),
        rs.getTimestamp("closed_at") == null ? null : rs.getTimestamp("closed_at").toLocalDateTime(),
        defaultText(rs.getString("issue_state"), "opened"),
        defaultText(rs.getString("severity_level"), ""),
        defaultText(rs.getString("urgency"), ""),
        defaultText(rs.getString("category"), ""),
        defaultText(rs.getString("bug_status"), ""),
        splitLabels(rs.getString("label_names")));
  }

  private List<String> splitLabels(String labelNames) {
    if (!StringUtils.hasText(labelNames)) {
      return List.of();
    }
    List<String> result = new ArrayList<>();
    for (String value : labelNames.split(",")) {
      String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
      if (!normalized.isEmpty()) {
        result.add(normalized);
      }
    }
    return result;
  }

  private Map<String, Object> toDetailRecord(IssueSource issue) {
    Map<String, Object> record = new LinkedHashMap<>();
    record.put("iid", issue.iid());
    record.put("title", issue.title());
    record.put("projectName", issue.projectName());
    record.put("authorName", issue.authorName());
    record.put("state", issue.isClosed() ? "已关闭" : "未关闭");
    record.put("labels", String.join(", ", issue.labels()));
    record.put("updatedAt", issue.updatedAt() == null ? "" : DATE_TIME_FORMATTER.format(issue.updatedAt()));
    return record;
  }

  private Predicate<IssueSource> matchesMetric(String columnKey) {
    return switch (columnKey) {
      case "level1_back" -> IssueSource::isLevel1Back;
      case "level1_hang" -> IssueSource::isLevel1Hang;
      case "level1_others" -> IssueSource::isLevel1Other;
      case "level1_fixed" -> issue -> issue.isLevel1() && issue.isClosed();
      case "level2_total" -> IssueSource::isLevel2;
      case "level2_fixed" -> issue -> issue.isLevel2() && issue.isClosed();
      case "level3_total" -> IssueSource::isLevel3;
      case "level3_fixed" -> issue -> issue.isLevel3() && issue.isClosed();
      case "p1_count" -> issue -> issue.hasPriority("p1");
      case "p2_count" -> issue -> issue.hasPriority("p2");
      case "p3_count" -> issue -> issue.hasPriority("p3");
      case "unclosedCount" -> issue -> !issue.isClosed();
      default -> issue -> true;
    };
  }

  private Comparator<IssueSource> buildDetailComparator(String sortField, String sortOrder) {
    String safeSortField = StringUtils.hasText(sortField) ? sortField.trim() : "updatedAt";
    Comparator<IssueSource> comparator = switch (safeSortField) {
      case "iid" -> Comparator.comparing(IssueSource::iid);
      case "title" -> Comparator.comparing(IssueSource::title, String.CASE_INSENSITIVE_ORDER);
      case "projectName" -> Comparator.comparing(IssueSource::projectName, String.CASE_INSENSITIVE_ORDER);
      case "authorName" -> Comparator.comparing(IssueSource::authorName, String.CASE_INSENSITIVE_ORDER);
      case "state" -> Comparator.comparing(issue -> issue.isClosed() ? 1 : 0);
      default -> Comparator.comparing(IssueSource::updatedAt, Comparator.nullsLast(LocalDateTime::compareTo));
    };
    Comparator<IssueSource> withTieBreaker = comparator.thenComparing(IssueSource::iid);
    return "ascending".equalsIgnoreCase(sortOrder) ? withTieBreaker : withTieBreaker.reversed();
  }

  private Long parseLong(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    try {
      return Long.parseLong(value.trim());
    } catch (NumberFormatException ex) {
      return null;
    }
  }

  private String defaultText(String value, String fallback) {
    return StringUtils.hasText(value) ? value.trim() : fallback;
  }

  private static String displayCount(long value) {
    return String.valueOf(value);
  }

  private static String displayRate(long numerator, long denominator) {
    if (denominator <= 0) {
      return "0.00%";
    }
    return String.format(Locale.ROOT, "%.2f%%", numerator * 100.0 / denominator);
  }

  private static String displayPercent(double value) {
    return String.format(Locale.ROOT, "%.2f%%", value);
  }

  private record AggregateBucket(
      Long projectId,
      String rowLabel,
      List<IssueSource> issues) {
    private AggregateBucket(Long projectId, String rowLabel) {
      this(projectId, rowLabel, new ArrayList<>());
    }

    void accept(IssueSource issue) {
      issues.add(issue);
    }

    StatisticRowData toRowData(long overallTotal) {
      long total = issues.size();
      long closed = issues.stream().filter(IssueSource::isClosed).count();
      long unclosed = total - closed;
      long level1Back = issues.stream().filter(IssueSource::isLevel1Back).count();
      long level1Hang = issues.stream().filter(IssueSource::isLevel1Hang).count();
      long level1Others = issues.stream().filter(IssueSource::isLevel1Other).count();
      long level1Total = level1Back + level1Hang + level1Others;
      long level1Fixed = issues.stream().filter(issue -> issue.isLevel1() && issue.isClosed()).count();
      long level2Total = issues.stream().filter(IssueSource::isLevel2).count();
      long level2Fixed = issues.stream().filter(issue -> issue.isLevel2() && issue.isClosed()).count();
      long level3Total = issues.stream().filter(IssueSource::isLevel3).count();
      long level3Fixed = issues.stream().filter(issue -> issue.isLevel3() && issue.isClosed()).count();
      long p1Total = issues.stream().filter(issue -> issue.hasPriority("p1")).count();
      long p1Fixed = issues.stream().filter(issue -> issue.hasPriority("p1") && issue.isClosed()).count();
      long p2Total = issues.stream().filter(issue -> issue.hasPriority("p2")).count();
      long p2Fixed = issues.stream().filter(issue -> issue.hasPriority("p2") && issue.isClosed()).count();
      long p3Total = issues.stream().filter(issue -> issue.hasPriority("p3")).count();
      long p3Fixed = issues.stream().filter(issue -> issue.hasPriority("p3") && issue.isClosed()).count();
      double defectRatio = overallTotal <= 0 ? 0 : total * 100.0 / overallTotal;

      return new StatisticRowData(
          String.valueOf(projectId),
          rowLabel,
          List.of(
              cell("level1_back", level1Back, displayCount(level1Back), true),
              cell("level1_hang", level1Hang, displayCount(level1Hang), true),
              cell("level1_others", level1Others, displayCount(level1Others), true),
              cell("level1_fixed", level1Fixed, displayCount(level1Fixed), true),
              cell("level1_rate", level1Fixed, displayRate(level1Fixed, level1Total), false),
              cell("level2_total", level2Total, displayCount(level2Total), true),
              cell("level2_fixed", level2Fixed, displayCount(level2Fixed), true),
              cell("level2_rate", level2Fixed, displayRate(level2Fixed, level2Total), false),
              cell("level3_total", level3Total, displayCount(level3Total), true),
              cell("level3_fixed", level3Fixed, displayCount(level3Fixed), true),
              cell("level3_rate", level3Fixed, displayRate(level3Fixed, level3Total), false),
              cell("p1_count", p1Total, displayCount(p1Total), true),
              cell("p1_rate", p1Fixed, displayRate(p1Fixed, p1Total), false),
              cell("p2_count", p2Total, displayCount(p2Total), true),
              cell("p2_rate", p2Fixed, displayRate(p2Fixed, p2Total), false),
              cell("p3_count", p3Total, displayCount(p3Total), true),
              cell("p3_rate", p3Fixed, displayRate(p3Fixed, p3Total), false),
              cell("totalDefects", total, displayCount(total), true),
              cell("defectRatio", Math.round(defectRatio), displayPercent(defectRatio), false),
              cell("closeRate", closed, displayRate(closed, total), false),
              cell("unclosedCount", unclosed, displayCount(unclosed), true)));
    }

    private StatisticCellData cell(String key, long numericValue, String displayValue, boolean drilldown) {
      return new StatisticCellData(
          key,
          numericValue,
          displayValue,
          drilldown,
          drilldown ? "issue-list" : null,
          Map.of("projectId", String.valueOf(projectId)));
    }
  }

  private record IssueSource(
      Long id,
      Integer iid,
      String title,
      Long projectId,
      String projectName,
      String authorName,
      LocalDateTime updatedAt,
      LocalDateTime closedAt,
      String issueState,
      String severityLevel,
      String urgency,
      String category,
      String bugStatus,
      List<String> labels) {

    boolean isClosed() {
      return closedAt != null
          || "closed".equalsIgnoreCase(issueState)
          || "已关闭".equals(bugStatus);
    }

    boolean hasPriority(String priority) {
      return priority.equalsIgnoreCase(urgency);
    }

    boolean hasAnyLabel(List<String> candidates) {
      return candidates.stream().anyMatch(this::hasLabel);
    }

    boolean hasLabel(String candidate) {
      String safeCandidate = candidate.toLowerCase(Locale.ROOT);
      return labels.stream().anyMatch(label -> label.contains(safeCandidate));
    }

    boolean isLevel1() {
      return isLevel1Back() || isLevel1Hang() || isLevel1Other();
    }

    boolean isLevel1Back() {
      return "一级".equals(severityLevel) && "回退".equals(category);
    }

    boolean isLevel1Hang() {
      return "一级".equals(severityLevel) && "挂机".equals(category);
    }

    boolean isLevel1Other() {
      return "一级".equals(severityLevel) && !isLevel1Back() && !isLevel1Hang();
    }

    boolean isLevel2() {
      return "二级".equals(severityLevel);
    }

    boolean isLevel3() {
      return "三级".equals(severityLevel);
    }
  }

  private record RuleFlowSnapshot(
      List<IssueSource> finalSources,
      List<StatisticRuleFlowStep> flowSteps) {
  }
}
