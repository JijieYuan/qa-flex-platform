package com.data.collection.platform.service.statistics;

import com.data.collection.platform.common.JsonUtils;
import com.data.collection.platform.entity.RealtimeWorkspaceStatusResponse;
import com.data.collection.platform.entity.statistics.*;
import com.data.collection.platform.service.FactBuildService;
import com.data.collection.platform.service.GitlabMirrorSyncService;
import com.data.collection.platform.service.RealtimeWorkspaceService;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
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
  private static final String RULE_VERSION = "system-test-defect-summary@2026-04-09-v5";
  private static final String TOTAL_ROW_KEY = "__total__";
  private static final String TOTAL_ROW_LABEL = "总计";
  private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
  private static final List<String> REALTIME_REFRESH_TABLES = List.of("issues", "projects", "users", "label_links", "labels", "notes");
  private static final String FACT_SQL = """
      select issue_id as id, issue_iid as iid, title, project_id, project_name,
             coalesce(author_name,'') as author_name, created_at_source as created_at,
             updated_at_source as updated_at, closed_at_source as closed_at,
             coalesce(issue_state,'opened') as issue_state, coalesce(testing_phase,'') as testing_phase,
             coalesce(system_test_label,'') as system_test_label, coalesce(severity_level,'') as severity_level,
             coalesce(priority_level,'') as priority_level, coalesce(is_excluded,false) as is_excluded,
             coalesce(exclusion_reason,'') as exclusion_reason, coalesce(is_fixed,false) as is_fixed,
             coalesce(delay_issue,false) as delay_issue, coalesce(is_regression,false) as is_regression,
             coalesce(is_crash,false) as is_crash, coalesce(is_level1_other,false) as is_level1_other,
             coalesce(is_illegal,false) as is_illegal, coalesce(illegal_reason,'') as illegal_reason,
             coalesce(is_legacy,false) as is_legacy, coalesce(module_names,'') as module_names,
             coalesce(label_names,'') as label_names
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
        BOARD_KEY, "系统测试缺陷汇总", "基于 issue_fact 的模块维度系统测试汇总。", "", "", "模块名称", List.of(),
        List.of(
            StatisticColumnGroup.withChildren("level1", "一级缺陷", List.of(
                new StatisticColumnGroup("level1-classification", "分类", List.of(
                    leaf("level1_back", "回退(个)", true, "count", null),
                    leaf("level1_hang", "挂机(个)", true, "count", null),
                    leaf("level1_other", "其他(个)", true, "count", null))),
                new StatisticColumnGroup("level1-status", "状态统计", List.of(
                    leaf("level1_fixed", "一级缺陷已修复数量", true, "count", null),
                    leaf("level1_total", "一级缺陷数量(个)", true, "count", null),
                    leaf("level1_rate", "一级缺陷修复率%", false, "ratio", "一级缺陷修复率 = 一级缺陷已修复数量 / 一级缺陷总数"))))),
            new StatisticColumnGroup("level2", "二级缺陷", List.of(
                leaf("level2_fixed", "二级缺陷已修复数量", true, "count", null),
                leaf("level2_total", "二级缺陷(个)", true, "count", null),
                leaf("level2_rate", "二级缺陷修复率%", false, "ratio", "二级缺陷修复率 = 二级缺陷已修复数量 / 二级缺陷总数"))),
            new StatisticColumnGroup("level3", "三级缺陷", List.of(
                leaf("level3_fixed", "三级缺陷已修复数量", true, "count", null),
                leaf("level3_total", "三级缺陷(个)", true, "count", null),
                leaf("level3_rate", "三级缺陷修复率%", false, "ratio", "三级缺陷修复率 = 三级缺陷已修复数量 / 三级缺陷总数"))),
            new StatisticColumnGroup("suggestion", "建议类缺陷", List.of(
                leaf("suggestion_total", "建议类缺陷(个)", true, "count", null))),
            StatisticColumnGroup.withChildren("priority-summary", "缺陷级别汇总", List.of(
                new StatisticColumnGroup("p1", "P1", List.of(
                    leaf("p1_count", "P1级别缺陷", true, "count", null),
                    leaf("p1_fix_rate", "P1缺陷修复率(%)", false, "ratio", "已修复 P1 / P1 总数"),
                    leaf("p1_close_rate", "P1缺陷关闭率(%)", false, "ratio", "已关闭 P1 / P1 总数"))),
                new StatisticColumnGroup("p2", "P2", List.of(
                    leaf("p2_count", "P2级别缺陷", true, "count", null),
                    leaf("p2_fix_rate", "P2缺陷修复率(%)", false, "ratio", "已修复 P2 / P2 总数"),
                    leaf("p2_close_rate", "P2缺陷关闭率(%)", false, "ratio", "已关闭 P2 / P2 总数"))),
                new StatisticColumnGroup("p3", "P3", List.of(
                    leaf("p3_count", "P3级别缺陷", true, "count", null),
                    leaf("p3_fix_rate", "P3缺陷修复率(%)", false, "ratio", "已修复 P3 / P3 总数"))),
                new StatisticColumnGroup("summary", "综合汇总", List.of(
                    leaf("module_total", "模块总缺陷数(个)", true, "count", null),
                    leaf("defect_ratio", "缺陷占比(%)", false, "ratio", "当前模块缺陷数 / 当前范围全部缺陷数"),
                    leaf("delay_defect_ratio", "延期缺陷占比(%)", false, "ratio", "当前模块延期缺陷数 / 当前模块缺陷总数"),
                    leaf("solved_count", "已修复/未更新", true, "count", "当前按已修复数量展示"),
                    leaf("fix_rate", "修复率(%)", false, "ratio", "已修复数量 / 当前模块缺陷总数"),
                    leaf("close_rate", "关闭率(%)", false, "ratio", "已关闭数量 / 当前模块缺陷总数"),
                    leaf("open_count", "未关闭缺陷数(个)", true, "count", null),
                    leaf("extension_count", "申请延期(个)", true, "count", null),
                    leaf("retest_failed_count", "复测未通过缺陷数(个)", true, "count", null))))),
            new StatisticColumnGroup("new-issue", "新发议题", List.of(
                leaf("new_issue_fixed", "新发议题修复数量", true, "count", null),
                leaf("new_issue_total", "新发议题数量", true, "count", null),
                leaf("new_issue_fix_rate", "新发议题修复率(%)", false, "ratio", "已修复新发议题 / 新发议题总数"),
                leaf("new_issue_close_rate", "新发议题关闭率(%)", false, "ratio", "已关闭新发议题 / 新发议题总数"))),
            new StatisticColumnGroup("legacy", "遗留率", List.of(
                leaf("level1_legacy_rate", "一级缺陷遗留率(%)", false, "ratio", "一级缺陷未关闭数量 / 一级缺陷总数"),
                leaf("level2_legacy_count", "二级缺陷遗留数量", true, "count", null),
                leaf("level3_legacy_count", "三级缺陷遗留数量", true, "count", null),
                leaf("level23_legacy_rate", "二三级缺陷遗留率(%)", false, "ratio", "二三级未关闭数量 / 二三级缺陷总数")))),
        List.of(
            new StatisticDetailColumn("iid", "议题编号", 120, 120, true),
            new StatisticDetailColumn("title", "标题", null, 260, true),
            new StatisticDetailColumn("moduleNames", "模块", null, 180, true),
            new StatisticDetailColumn("projectName", "所属项目", null, 160, true),
            new StatisticDetailColumn("authorName", "创建人", 140, 140, true),
            new StatisticDetailColumn("state", "状态", 120, 120, true),
            new StatisticDetailColumn("labels", "标签", null, 240, false),
            new StatisticDetailColumn("updatedAt", "更新时间", 180, 180, true)),
        10, "当前没有可展示的系统测试缺陷统计数据。");
  }

  private StatisticColumnLeaf leaf(String key, String label, boolean drilldown, String metricType, String helpText) {
    return new StatisticColumnLeaf(key, label, drilldown, metricType, helpText);
  }

  @Override
  protected StatisticBoardResponse doLoadBoard(Map<String, String> filters, StatisticFilterGroup filterGroup) {
    long startedAt = System.currentTimeMillis();
    List<IssueSource> sources = loadBoardScopedSources(filters);
    Map<String, AggregateBucket> buckets = new LinkedHashMap<>();
    for (IssueSource issue : sources) {
      for (String moduleName : issue.moduleNames()) {
        buckets.computeIfAbsent(moduleName, AggregateBucket::new).accept(issue);
      }
    }
    List<StatisticRowData> rows = new ArrayList<>(buckets.values().stream()
        .map(bucket -> bucket.toRowData(sources.size()))
        .sorted(Comparator.comparing(StatisticRowData::rowLabel, String.CASE_INSENSITIVE_ORDER))
        .toList());
    rows.add(new AggregateBucket(TOTAL_ROW_LABEL).acceptAll(sources).toRowData(sources.size(), TOTAL_ROW_KEY));
    StatisticBoardDefinition definition = buildDefinition();
    int columnCount = definition.columnGroups().stream().mapToInt(StatisticColumnGroup::columnCount).sum();
    int drilldownCount = definition.columnGroups().stream().flatMap(group -> group.leafColumns().stream()).mapToInt(c -> c.drilldown() ? 1 : 0).sum();
    return new StatisticBoardResponse(definition, withoutReservedFilters(filters), filterGroup, rows,
        new StatisticBoardMeta(LocalDateTime.now(), System.currentTimeMillis() - startedAt, rows.size(), columnCount, drilldownCount));
  }

  @Override
  protected StatisticDetailResponse doLoadDetail(StatisticDetailRequest request, StatisticFilterGroup filterGroup) {
    List<IssueSource> scoped = loadBoardScopedSources(request.filters()).stream()
        .filter(issue -> matchesRow(issue, request.rowKey())).filter(matchesMetric(request.columnKey()))
        .sorted(buildDetailComparator(request.sortField(), request.sortOrder())).toList();
    int page = request.page() <= 0 ? 1 : request.page(), size = request.size() <= 0 ? 10 : request.size();
    int from = Math.min((page - 1) * size, scoped.size()), to = Math.min(from + size, scoped.size());
    return new StatisticDetailResponse("系统测试缺陷明细", "展示当前模块与指标命中的 issue_fact 明细。", buildDefinition().detailColumns(),
        scoped.subList(from, to).stream().map(this::toDetailRecord).toList(), scoped.size(), page, size,
        StringUtils.hasText(request.sortField()) ? request.sortField() : "updatedAt",
        "ascending".equalsIgnoreCase(request.sortOrder()) ? "ascending" : "descending");
  }

  @Override
  public RealtimeWorkspaceStatusResponse getRealtimeStatus() {
    return realtimeWorkspaceService.getStatus(BOARD_KEY);
  }

  @Override
  public RealtimeWorkspaceStatusResponse requestRealtimeRefresh() {
    return realtimeWorkspaceService.requestRefresh(BOARD_KEY, this::refreshMirrorForRealtimeView);
  }

  @Override
  public StatisticBoardRuleExplanationResponse getRuleExplanation(Map<String, String> filters) {
    RuleFlowSnapshot s = buildRuleFlowSnapshot(loadSources(filters));
    return new StatisticBoardRuleExplanationResponse(
        BOARD_KEY, true, "系统测试缺陷汇总规则说明", RULE_VERSION,
        "当前统计基于 issue_fact 的归一化事实字段，先限定系统测试/回归测试范围，再按模块展开。",
        "同一条议题如果关联多个模块，会分别计入对应模块；总计行仍按议题本身统计。", s.flowSteps(), buildMetricDefinitions(), null);
  }

  private List<IssueSource> loadBoardScopedSources(Map<String, String> filters) {
    return buildRuleFlowSnapshot(loadSources(filters)).finalSources();
  }

  private RuleFlowSnapshot buildRuleFlowSnapshot(List<IssueSource> loaded) {
    List<IssueSource> initial = loaded == null ? List.of() : List.copyOf(loaded);
    List<IssueSource> scoped = initial.stream().filter(IssueSource::inSystemTestScope).toList();
    List<IssueSource> valid = scoped.stream().filter(i -> !i.excluded()).toList();
    return new RuleFlowSnapshot(valid, List.of(
        step("source-load", "加载议题事实", "从 issue_fact 读取已经归一化的议题事实。", initial, initial.size()),
        step("scope-filter", "限定系统测试范围", "只保留带有系统测试或回归测试标签的议题。", scoped, initial.size()),
        step("exclude-invalid-issues", "排除无效数据", "剔除功能屏蔽、已拒绝、建议，以及关闭后属于申请否决/数据异常/需求如此的议题。", valid, scoped.size()),
        new StatisticRuleFlowStep("module-expand", "按模块展开", "同一条议题可能属于多个模块，模块行会分别计入；总计行仍按议题本身统计。", valid.size(),
            valid.stream().mapToLong(i -> i.moduleNames().size()).sum(), sample(valid))));
  }

  private StatisticRuleFlowStep step(String key, String title, String desc, List<IssueSource> output, long inputCount) {
    return new StatisticRuleFlowStep(key, title, desc, inputCount, output.size(), sample(output));
  }

  private List<StatisticRuleFlowStepSample> sample(List<IssueSource> issues) {
    return issues.stream().limit(3).map(i -> new StatisticRuleFlowStepSample("#" + i.iid() + " " + i.projectName(),
        i.title() + (i.moduleNames().isEmpty() ? "" : " | 模块: " + String.join("、", i.moduleNames())))).toList();
  }

  private List<StatisticRuleMetricDefinition> buildMetricDefinitions() {
    return List.of(
        new StatisticRuleMetricDefinition("level1", "一级缺陷", "一级缺陷基于 severity_level = LEVEL1，再拆分回退、挂机、其他一级。", "一级缺陷修复率 = 一级缺陷已修复数量 / 一级缺陷总数", null),
        new StatisticRuleMetricDefinition("priority-summary", "缺陷级别汇总", "P1/P2/P3 与一级/二级/三级缺陷是两套独立统计体系，直接按 priority_level 聚合。", "Pn 修复率 = 已修复 Pn 数量 / Pn 总数；Pn 关闭率 = 已关闭 Pn 数量 / Pn 总数", null),
        new StatisticRuleMetricDefinition("summary", "综合汇总", "综合区展示模块总缺陷、缺陷占比、延期占比、修复率、关闭率、未关闭数量、申请延期和复测未通过。", "缺陷占比 = 当前模块缺陷数 / 当前范围全部缺陷数", null),
        new StatisticRuleMetricDefinition("new-issue", "新发议题", "新发议题当前按“不是历史遗留”的议题统计。", "新发议题修复率 = 已修复新发议题数量 / 新发议题总数", null),
        new StatisticRuleMetricDefinition("legacy", "遗留率", "遗留区展示一级缺陷遗留率、二级缺陷遗留数量、三级缺陷遗留数量、二三级缺陷遗留率。", "一级遗留率 = 一级缺陷未关闭数量 / 一级缺陷总数", null));
  }

  private List<IssueSource> loadSources(Map<String, String> filters) {
    Long projectId = parseLong(withoutReservedFilters(filters).get("projectId"));
    try {
      List<IssueSource> facts = ensureFactsReady(projectId);
      return facts.isEmpty() ? List.of() : facts;
    } catch (DataAccessException e) {
      log.warn("Failed to load issue facts", e);
      return List.of();
    }
  }

  private void refreshMirrorForRealtimeView() {
    try {
      gitlabMirrorSyncService.refreshTablesOnDemand(REALTIME_REFRESH_TABLES, BOARD_KEY);
      factBuildService.rebuildIssueFacts(false);
    } catch (Exception e) {
      log.warn("On-demand mirror refresh for {} failed", BOARD_KEY, e);
    }
  }

  private List<IssueSource> ensureFactsReady(Long projectId) {
    List<IssueSource> facts = loadSourcesFromFact(projectId);
    if (!facts.isEmpty()) return facts;
    factBuildService.rebuildIssueFacts(true);
    return loadSourcesFromFact(projectId);
  }

  private List<IssueSource> loadSourcesFromFact(Long projectId) {
    String sql = FACT_SQL + (projectId == null ? "" : " and project_id = ?");
    return projectId == null ? jdbcTemplate.query(sql, this::mapIssueFact) : jdbcTemplate.query(sql, this::mapIssueFact, projectId);
  }

  private IssueSource mapIssueFact(ResultSet rs, int rowNum) throws SQLException {
    return new IssueSource(
        rs.getLong("id"), rs.getInt("iid"), text(rs.getString("title"), ""), rs.getLong("project_id"),
        text(rs.getString("project_name"), "未命名项目"), text(rs.getString("author_name"), ""),
        time(rs.getTimestamp("created_at")), time(rs.getTimestamp("updated_at")), time(rs.getTimestamp("closed_at")),
        text(rs.getString("issue_state"), "opened"), text(rs.getString("testing_phase"), ""),
        text(rs.getString("system_test_label"), ""), text(rs.getString("severity_level"), ""),
        text(rs.getString("priority_level"), ""), rs.getBoolean("is_excluded"), text(rs.getString("exclusion_reason"), ""),
        rs.getBoolean("is_fixed"), rs.getBoolean("delay_issue"), rs.getBoolean("is_regression"),
        rs.getBoolean("is_crash"), rs.getBoolean("is_level1_other"), rs.getBoolean("is_illegal"),
        text(rs.getString("illegal_reason"), ""), rs.getBoolean("is_legacy"), split(rs.getString("module_names")), split(rs.getString("label_names")));
  }

  private Map<String, Object> toDetailRecord(IssueSource i) {
    Map<String, Object> r = new LinkedHashMap<>();
    r.put("iid", i.iid()); r.put("title", i.title()); r.put("moduleNames", String.join("、", i.moduleNames()));
    r.put("projectName", i.projectName()); r.put("authorName", i.authorName()); r.put("state", i.isClosed() ? "已关闭" : "未关闭");
    r.put("labels", String.join(", ", i.labels())); r.put("updatedAt", i.updatedAt() == null ? "" : DATE_TIME_FORMATTER.format(i.updatedAt())); return r;
  }

  private Predicate<IssueSource> matchesMetric(String key) {
    return switch (key) {
      case "level1_back" -> IssueSource::isLevel1Back;
      case "level1_hang" -> IssueSource::isLevel1Hang;
      case "level1_other" -> IssueSource::isLevel1Other;
      case "level1_fixed" -> i -> i.isLevel1() && i.fixed();
      case "level1_total" -> IssueSource::isLevel1;
      case "level2_fixed" -> i -> i.isLevel2() && i.fixed();
      case "level2_total" -> IssueSource::isLevel2;
      case "level3_fixed" -> i -> i.isLevel3() && i.fixed();
      case "level3_total" -> IssueSource::isLevel3;
      case "suggestion_total" -> IssueSource::isSuggestion;
      case "p1_count" -> i -> i.isPriority("P1");
      case "p2_count" -> i -> i.isPriority("P2");
      case "p3_count" -> i -> i.isPriority("P3");
      case "solved_count" -> IssueSource::fixed;
      case "open_count" -> i -> !i.isClosed();
      case "extension_count" -> IssueSource::hasExtensionLabel;
      case "retest_failed_count" -> IssueSource::isRetestFailed;
      case "new_issue_fixed" -> i -> i.isNewIssue() && i.fixed();
      case "new_issue_total" -> IssueSource::isNewIssue;
      case "level2_legacy_count" -> i -> i.isLevel2() && !i.isClosed();
      case "level3_legacy_count" -> i -> i.isLevel3() && !i.isClosed();
      default -> i -> true;
    };
  }

  private boolean matchesRow(IssueSource issue, String rowKey) {
    return !StringUtils.hasText(rowKey) || TOTAL_ROW_KEY.equals(rowKey) || issue.moduleNames().contains(rowKey);
  }

  private Comparator<IssueSource> buildDetailComparator(String sortField, String sortOrder) {
    Comparator<IssueSource> c = switch (StringUtils.hasText(sortField) ? sortField.trim() : "updatedAt") {
      case "iid" -> Comparator.comparing(IssueSource::iid);
      case "title" -> Comparator.comparing(IssueSource::title, String.CASE_INSENSITIVE_ORDER);
      case "moduleNames" -> Comparator.comparing(i -> String.join("、", i.moduleNames()), String.CASE_INSENSITIVE_ORDER);
      case "projectName" -> Comparator.comparing(IssueSource::projectName, String.CASE_INSENSITIVE_ORDER);
      case "authorName" -> Comparator.comparing(IssueSource::authorName, String.CASE_INSENSITIVE_ORDER);
      case "state" -> Comparator.comparing(i -> i.isClosed() ? 1 : 0);
      default -> Comparator.comparing(IssueSource::updatedAt, Comparator.nullsLast(LocalDateTime::compareTo));
    };
    c = c.thenComparing(IssueSource::iid);
    return "ascending".equalsIgnoreCase(sortOrder) ? c : c.reversed();
  }

  private Long parseLong(String value) { try { return StringUtils.hasText(value) ? Long.parseLong(value.trim()) : null; } catch (NumberFormatException e) { return null; } }
  private String text(String value, String fallback) { return StringUtils.hasText(value) ? value.trim() : fallback; }
  private LocalDateTime time(java.sql.Timestamp timestamp) { return timestamp == null ? null : timestamp.toLocalDateTime(); }
  private List<String> split(String raw) {
    if (!StringUtils.hasText(raw)) return List.of();
    Set<String> values = new LinkedHashSet<>();
    for (String value : raw.split(",")) { String t = value == null ? "" : value.trim(); if (!t.isEmpty()) values.add(t); }
    return List.copyOf(values);
  }
  private static String count(long v) { return String.valueOf(v); }
  private static String rate(long n, long d) { return d <= 0 ? "/" : String.format(Locale.ROOT, "%.2f%%", n * 100.0 / d); }
  private static String percent(double value) { return String.format(Locale.ROOT, "%.2f%%", value); }

  private record AggregateBucket(String rowLabel, List<IssueSource> issues) {
    AggregateBucket(String rowLabel) { this(rowLabel, new ArrayList<>()); }
    AggregateBucket acceptAll(List<IssueSource> sourceIssues) { issues.addAll(sourceIssues); return this; }
    void accept(IssueSource issue) { issues.add(issue); }
    StatisticRowData toRowData(long overall) { return toRowData(overall, rowLabel); }
    StatisticRowData toRowData(long overall, String rowKey) {
      long total = issues.size(), fixed = issues.stream().filter(IssueSource::fixed).count(), closed = issues.stream().filter(IssueSource::isClosed).count(), open = total - closed;
      long delayed = issues.stream().filter(IssueSource::delayIssue).count(), extension = issues.stream().filter(IssueSource::hasExtensionLabel).count(), retest = issues.stream().filter(IssueSource::isRetestFailed).count();
      long l1b = issues.stream().filter(IssueSource::isLevel1Back).count(), l1h = issues.stream().filter(IssueSource::isLevel1Hang).count(), l1o = issues.stream().filter(IssueSource::isLevel1Other).count(), l1 = issues.stream().filter(IssueSource::isLevel1).count(), l1f = issues.stream().filter(i -> i.isLevel1() && i.fixed()).count(), l1open = issues.stream().filter(i -> i.isLevel1() && !i.isClosed()).count();
      long l2 = issues.stream().filter(IssueSource::isLevel2).count(), l2f = issues.stream().filter(i -> i.isLevel2() && i.fixed()).count(), l2open = issues.stream().filter(i -> i.isLevel2() && !i.isClosed()).count();
      long l3 = issues.stream().filter(IssueSource::isLevel3).count(), l3f = issues.stream().filter(i -> i.isLevel3() && i.fixed()).count(), l3open = issues.stream().filter(i -> i.isLevel3() && !i.isClosed()).count();
      long sug = issues.stream().filter(IssueSource::isSuggestion).count();
      long p1 = issues.stream().filter(i -> i.isPriority("P1")).count(), p1f = issues.stream().filter(i -> i.isPriority("P1") && i.fixed()).count(), p1c = issues.stream().filter(i -> i.isPriority("P1") && i.isClosed()).count();
      long p2 = issues.stream().filter(i -> i.isPriority("P2")).count(), p2f = issues.stream().filter(i -> i.isPriority("P2") && i.fixed()).count(), p2c = issues.stream().filter(i -> i.isPriority("P2") && i.isClosed()).count();
      long p3 = issues.stream().filter(i -> i.isPriority("P3")).count(), p3f = issues.stream().filter(i -> i.isPriority("P3") && i.fixed()).count();
      long newTotal = issues.stream().filter(IssueSource::isNewIssue).count(), newFixed = issues.stream().filter(i -> i.isNewIssue() && i.fixed()).count(), newClosed = issues.stream().filter(i -> i.isNewIssue() && i.isClosed()).count();
      long l23open = l2open + l3open, l23 = l2 + l3; double defectRatio = overall <= 0 ? 0 : total * 100.0 / overall, delayRatio = total <= 0 ? 0 : delayed * 100.0 / total;
      return new StatisticRowData(rowKey, rowLabel, List.of(
          cell("level1_back", l1b, count(l1b), true, rowKey), cell("level1_hang", l1h, count(l1h), true, rowKey), cell("level1_other", l1o, count(l1o), true, rowKey),
          cell("level1_fixed", l1f, count(l1f), true, rowKey), cell("level1_total", l1, count(l1), true, rowKey), cell("level1_rate", l1f, rate(l1f, l1), false, rowKey),
          cell("level2_fixed", l2f, count(l2f), true, rowKey), cell("level2_total", l2, count(l2), true, rowKey), cell("level2_rate", l2f, rate(l2f, l2), false, rowKey),
          cell("level3_fixed", l3f, count(l3f), true, rowKey), cell("level3_total", l3, count(l3), true, rowKey), cell("level3_rate", l3f, rate(l3f, l3), false, rowKey),
          cell("suggestion_total", sug, count(sug), true, rowKey), cell("p1_count", p1, count(p1), true, rowKey), cell("p1_fix_rate", p1f, rate(p1f, p1), false, rowKey),
          cell("p1_close_rate", p1c, rate(p1c, p1), false, rowKey), cell("p2_count", p2, count(p2), true, rowKey), cell("p2_fix_rate", p2f, rate(p2f, p2), false, rowKey),
          cell("p2_close_rate", p2c, rate(p2c, p2), false, rowKey), cell("p3_count", p3, count(p3), true, rowKey), cell("p3_fix_rate", p3f, rate(p3f, p3), false, rowKey),
          cell("module_total", total, count(total), true, rowKey), cell("defect_ratio", Math.round(defectRatio), percent(defectRatio), false, rowKey), cell("delay_defect_ratio", Math.round(delayRatio), percent(delayRatio), false, rowKey),
          cell("solved_count", fixed, count(fixed), true, rowKey), cell("fix_rate", fixed, rate(fixed, total), false, rowKey), cell("close_rate", closed, rate(closed, total), false, rowKey),
          cell("open_count", open, count(open), true, rowKey), cell("extension_count", extension, count(extension), true, rowKey), cell("retest_failed_count", retest, count(retest), true, rowKey),
          cell("new_issue_fixed", newFixed, count(newFixed), true, rowKey), cell("new_issue_total", newTotal, count(newTotal), true, rowKey), cell("new_issue_fix_rate", newFixed, rate(newFixed, newTotal), false, rowKey),
          cell("new_issue_close_rate", newClosed, rate(newClosed, newTotal), false, rowKey), cell("level1_legacy_rate", l1open, rate(l1open, l1), false, rowKey), cell("level2_legacy_count", l2open, count(l2open), true, rowKey),
          cell("level3_legacy_count", l3open, count(l3open), true, rowKey), cell("level23_legacy_rate", l23open, rate(l23open, l23), false, rowKey)));
    }
    private StatisticCellData cell(String key, long numericValue, String displayValue, boolean drilldown, String rowKey) {
      return new StatisticCellData(key, numericValue, displayValue, drilldown, drilldown ? "issue-list" : null, Map.of("rowKey", rowKey));
    }
  }

  private record IssueSource(Long id, Integer iid, String title, Long projectId, String projectName, String authorName, LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime closedAt, String issueState, String testingPhase, String systemTestLabel, String severityLevel, String priorityLevel, boolean excluded, String exclusionReason, boolean fixed, boolean delayIssue, boolean regression, boolean crash, boolean level1Other, boolean illegal, String illegalReason, boolean legacy, List<String> moduleNames, List<String> labels) {
    boolean inSystemTestScope() { return hasScope(testingPhase) || hasScope(systemTestLabel) || labels.stream().anyMatch(this::hasScope); }
    boolean isClosed() { return closedAt != null || "closed".equalsIgnoreCase(issueState); }
    boolean isPriority(String priority) { return priority.equalsIgnoreCase(priorityLevel); }
    boolean isSeverity(String severity) { return severity.equalsIgnoreCase(severityLevel); }
    boolean isLevel1() { return isSeverity("LEVEL1"); }
    boolean isLevel1Back() { return isLevel1() && regression; }
    boolean isLevel1Hang() { return isLevel1() && crash; }
    boolean isLevel1Other() { return isLevel1() && level1Other; }
    boolean isLevel2() { return isSeverity("LEVEL2"); }
    boolean isLevel3() { return isSeverity("LEVEL3"); }
    boolean isSuggestion() { return isSeverity("SUGGESTION"); }
    boolean isNewIssue() { return !legacy; }
    boolean hasExtensionLabel() { return labels.contains("申请延期"); }
    boolean isRetestFailed() { return labels.contains("复测未通过"); }
    private boolean hasScope(String value) { return StringUtils.hasText(value) && (value.contains("系统测试") || value.contains("回归测试")); }
  }

  private record RuleFlowSnapshot(List<IssueSource> finalSources, List<StatisticRuleFlowStep> flowSteps) {}
}
