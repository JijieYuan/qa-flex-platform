package com.data.collection.platform.service.statistics;

import com.data.collection.platform.common.JsonUtils;
import com.data.collection.platform.entity.RealtimeWorkspaceStatusResponse;
import com.data.collection.platform.entity.statistics.StatisticBoardDefinition;
import com.data.collection.platform.entity.statistics.StatisticBoardMeta;
import com.data.collection.platform.entity.statistics.StatisticBoardResponse;
import com.data.collection.platform.entity.statistics.StatisticBoardRuleExplanationResponse;
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
  private static final String RULE_VERSION = "system-test-defect-summary@2026-04-09-v3";
  private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
  private static final List<String> REALTIME_REFRESH_TABLES = List.of("issues", "projects", "users", "label_links", "labels", "notes");

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
        coalesce(priority_level, '') as priority_level,
        coalesce(is_excluded, false) as is_excluded,
        coalesce(exclusion_reason, '') as exclusion_reason,
        coalesce(is_fixed, false) as is_fixed,
        coalesce(is_regression, false) as is_regression,
        coalesce(is_crash, false) as is_crash,
        coalesce(is_level1_other, false) as is_level1_other,
        coalesce(is_illegal, false) as is_illegal,
        coalesce(illegal_reason, '') as illegal_reason,
        coalesce(is_legacy, false) as is_legacy,
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
        "基于 issue_fact 中已归一化的议题事实做项目级统计。",
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
                    new StatisticColumnLeaf("level1_others", "其他一级", true, "count"),
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
                "优先级",
                List.of(
                    new StatisticColumnLeaf("p1_count", "P1 数量", true, "count"),
                    new StatisticColumnLeaf("p1_rate", "P1 修复率", false, "ratio"),
                    new StatisticColumnLeaf("p2_count", "P2 数量", true, "count"),
                    new StatisticColumnLeaf("p2_rate", "P2 修复率", false, "ratio"),
                    new StatisticColumnLeaf("p3_count", "P3 数量", true, "count"),
                    new StatisticColumnLeaf("p3_rate", "P3 修复率", false, "ratio"))),
            new StatisticColumnGroup(
                "summary",
                "综合",
                List.of(
                    new StatisticColumnLeaf("totalDefects", "项目总缺陷数", true, "count"),
                    new StatisticColumnLeaf("defectRatio", "缺陷占比", false, "ratio"),
                    new StatisticColumnLeaf("closeRate", "关闭率", false, "ratio"),
                    new StatisticColumnLeaf("unclosedCount", "未关闭数量", true, "count")))),
        List.of(
            new StatisticDetailColumn("iid", "议题编号", 120, 120, true),
            new StatisticDetailColumn("title", "标题", null, 260, true),
            new StatisticDetailColumn("projectName", "所属项目", null, 160, true),
            new StatisticDetailColumn("authorName", "创建人", 140, 140, true),
            new StatisticDetailColumn("state", "状态", 120, 120, true),
            new StatisticDetailColumn("labels", "标签", null, 240, false),
            new StatisticDetailColumn("updatedAt", "更新时间", 180, 180, true)),
        10,
        "当前没有可展示的系统测试缺陷统计数据。");
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
        "展示当前统计指标背后的 issue_fact 明细。",
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
        "当前统计完全基于 issue_fact 中已归一化的字段执行，页面不再重复解释底层标签和评论逻辑。",
        "你看到的数字来自三步：先加载议题事实，再剔除无效数据，最后按严重程度和修复状态聚合。",
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

    steps.add(new StatisticRuleFlowStep(
        "source-load",
        "加载议题事实",
        "从 issue_fact 读取当前范围内已经归一化完成的议题记录。",
        initial.size(),
        initial.size(),
        sampleIssues(initial)));

    List<IssueSource> afterExclusion = initial.stream()
        .filter(issue -> !issue.excluded())
        .toList();
    steps.add(new StatisticRuleFlowStep(
        "exclude-invalid-issues",
        "排除无效数据",
        "剔除功能屏蔽、已拒绝、建议，以及关闭后属于申请否决/数据异常/设计如此的议题。",
        initial.size(),
        afterExclusion.size(),
        sampleIssues(afterExclusion)));

    steps.add(new StatisticRuleFlowStep(
        "aggregate-normalized-facts",
        "按归一化字段统计",
        "基于严重程度、优先级、一级缺陷分类、修复状态等事实字段生成最终统计结果。",
        afterExclusion.size(),
        afterExclusion.size(),
        sampleIssues(afterExclusion)));

    return new RuleFlowSnapshot(afterExclusion, steps);
  }

  private List<StatisticRuleFlowStepSample> sampleIssues(List<IssueSource> issues) {
    return issues.stream()
        .limit(3)
        .map(issue -> new StatisticRuleFlowStepSample(
            "#" + issue.iid() + " " + issue.projectName(),
            issue.title() + (issue.labels().isEmpty() ? "" : " | 标签: " + String.join(", ", issue.labels()))))
        .toList();
  }

  private List<StatisticRuleMetricDefinition> buildMetricDefinitions() {
    return List.of(
        new StatisticRuleMetricDefinition(
            "level1",
            "一级缺陷",
            "一级缺陷直接读取 issue_fact 中的 severity_level = LEVEL1，再结合回退/挂机/其他一级标记。",
            "一级缺陷数量 = 回退数量 + 挂机数量 + 其他一级数量",
            "已修复数量读取 is_fixed，不再临时按页面逻辑推断。"),
        new StatisticRuleMetricDefinition(
            "level2",
            "二级缺陷",
            "二级缺陷读取 severity_level = LEVEL2。",
            "二级修复率 = 二级已修复数量 / 二级总数量",
            "分母为 0 时返回 0.00%。"),
        new StatisticRuleMetricDefinition(
            "level3",
            "三级缺陷",
            "三级缺陷读取 severity_level = LEVEL3。",
            "三级修复率 = 三级已修复数量 / 三级总数量",
            "分母为 0 时返回 0.00%。"),
        new StatisticRuleMetricDefinition(
            "priority",
            "优先级统计",
            "P1/P2/P3 数量直接按 priority_level 聚合，不再和一级/二级/三级缺陷互相映射。",
            "Pn 修复率 = Pn 已修复数量 / Pn 总数量",
            "建议类不参与 P1/P2/P3 分组。"),
        new StatisticRuleMetricDefinition(
            "summary",
            "综合汇总",
            "综合列展示项目总缺陷数、缺陷占比、关闭率和未关闭数量。",
            "缺陷占比 = 当前项目缺陷数 / 当前统计结果中的全部缺陷数",
            "当前页面默认已经剔除无效数据。"));
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
        defaultText(rs.getString("priority_level"), ""),
        rs.getBoolean("is_excluded"),
        defaultText(rs.getString("exclusion_reason"), ""),
        rs.getBoolean("is_fixed"),
        rs.getBoolean("is_regression"),
        rs.getBoolean("is_crash"),
        rs.getBoolean("is_level1_other"),
        rs.getBoolean("is_illegal"),
        defaultText(rs.getString("illegal_reason"), ""),
        rs.getBoolean("is_legacy"),
        splitLabels(rs.getString("label_names")));
  }

  private List<String> splitLabels(String labelNames) {
    if (!StringUtils.hasText(labelNames)) {
      return List.of();
    }
    List<String> result = new ArrayList<>();
    for (String value : labelNames.split(",")) {
      String normalized = value == null ? "" : value.trim();
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
      case "level1_fixed" -> issue -> issue.isLevel1() && issue.fixed();
      case "level2_total" -> IssueSource::isLevel2;
      case "level2_fixed" -> issue -> issue.isLevel2() && issue.fixed();
      case "level3_total" -> IssueSource::isLevel3;
      case "level3_fixed" -> issue -> issue.isLevel3() && issue.fixed();
      case "p1_count" -> issue -> issue.isPriority("P1");
      case "p2_count" -> issue -> issue.isPriority("P2");
      case "p3_count" -> issue -> issue.isPriority("P3");
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

  private record AggregateBucket(Long projectId, String rowLabel, List<IssueSource> issues) {
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
      long level1Fixed = issues.stream().filter(issue -> issue.isLevel1() && issue.fixed()).count();
      long level2Total = issues.stream().filter(IssueSource::isLevel2).count();
      long level2Fixed = issues.stream().filter(issue -> issue.isLevel2() && issue.fixed()).count();
      long level3Total = issues.stream().filter(IssueSource::isLevel3).count();
      long level3Fixed = issues.stream().filter(issue -> issue.isLevel3() && issue.fixed()).count();
      long p1Total = issues.stream().filter(issue -> issue.isPriority("P1")).count();
      long p1Fixed = issues.stream().filter(issue -> issue.isPriority("P1") && issue.fixed()).count();
      long p2Total = issues.stream().filter(issue -> issue.isPriority("P2")).count();
      long p2Fixed = issues.stream().filter(issue -> issue.isPriority("P2") && issue.fixed()).count();
      long p3Total = issues.stream().filter(issue -> issue.isPriority("P3")).count();
      long p3Fixed = issues.stream().filter(issue -> issue.isPriority("P3") && issue.fixed()).count();
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
      String priorityLevel,
      boolean excluded,
      String exclusionReason,
      boolean fixed,
      boolean regression,
      boolean crash,
      boolean level1Other,
      boolean illegal,
      String illegalReason,
      boolean legacy,
      List<String> labels) {

    boolean isClosed() {
      return closedAt != null || "closed".equalsIgnoreCase(issueState);
    }

    boolean isSeverity(String severity) {
      return severity.equalsIgnoreCase(severityLevel);
    }

    boolean isPriority(String priority) {
      return priority.equalsIgnoreCase(priorityLevel);
    }

    boolean isLevel1() {
      return isSeverity("LEVEL1");
    }

    boolean isLevel1Back() {
      return isSeverity("LEVEL1") && regression;
    }

    boolean isLevel1Hang() {
      return isSeverity("LEVEL1") && crash;
    }

    boolean isLevel1Other() {
      return isSeverity("LEVEL1") && level1Other;
    }

    boolean isLevel2() {
      return isSeverity("LEVEL2");
    }

    boolean isLevel3() {
      return isSeverity("LEVEL3");
    }
  }

  private record RuleFlowSnapshot(List<IssueSource> finalSources, List<StatisticRuleFlowStep> flowSteps) {
  }
}
