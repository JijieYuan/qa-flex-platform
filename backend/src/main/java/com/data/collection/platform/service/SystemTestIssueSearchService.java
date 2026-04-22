package com.data.collection.platform.service;

import com.data.collection.platform.entity.OptionItemResponse;
import com.data.collection.platform.entity.SystemTestIssueSearchFilterOptionsResponse;
import com.data.collection.platform.entity.SystemTestIssueSearchListResponse;
import com.data.collection.platform.entity.SystemTestIssueSearchRowResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SystemTestIssueSearchService {
  private static final String FACT_SQL =
      """
      select project_id,
             coalesce(project_name, '') as project_name,
             issue_id,
             issue_iid,
             coalesce(title, '') as title,
             coalesce(issue_state, 'opened') as issue_state,
             coalesce(testing_phase, '') as testing_phase,
             coalesce(system_test_label, '') as system_test_label,
             coalesce(severity_level, '') as severity_level,
             coalesce(bug_status, '') as bug_status,
             coalesce(category, '') as category,
             coalesce(milestone_title, '') as milestone_title,
             coalesce(author_name, '') as author_name,
             coalesce(assignee_name, '') as assignee_name,
             coalesce(module_names, '') as module_names,
             coalesce(label_names, '') as label_names,
             created_at_source,
             updated_at_source,
             closed_at_source
        from issue_fact
       where deleted = false
      """;

  private final IssueFactQueryService issueFactQueryService;

  public SystemTestIssueSearchService(IssueFactQueryService issueFactQueryService) {
    this.issueFactQueryService = issueFactQueryService;
  }

  public SystemTestIssueSearchListResponse listRecords(
      Long projectId,
      String keyword,
      String issueIid,
      String title,
      String projectName,
      String moduleName,
      String testingPhase,
      String authorName,
      String assigneeName,
      String issueState,
      String severityLevel,
      String bugStatus,
      String category,
      String milestoneTitle,
      String createdAtStart,
      String createdAtEnd,
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

    List<IssueSearchView> filtered =
        loadScopedViews(projectId).stream()
            .filter(view -> matchesKeyword(view, keyword))
            .filter(view -> matchesIssueIid(view, issueIid))
            .filter(view -> matchesText(view.title(), title))
            .filter(view -> matchesEquals(view.projectName(), projectName))
            .filter(view -> matchesModule(view, moduleName))
            .filter(view -> matchesTestingPhase(view, testingPhase))
            .filter(view -> matchesEquals(view.authorName(), authorName))
            .filter(view -> matchesEquals(view.assigneeName(), assigneeName))
            .filter(view -> matchesEquals(view.issueState(), issueState))
            .filter(view -> matchesEquals(view.severityLevel(), severityLevel))
            .filter(view -> matchesEquals(view.bugStatus(), bugStatus))
            .filter(view -> matchesEquals(view.category(), category))
            .filter(view -> matchesEquals(view.milestoneTitle(), milestoneTitle))
            .filter(view -> matchesDateRange(view.createdAt(), createdAtStart, createdAtEnd))
            .filter(view -> matchesDateRange(view.updatedAt(), updatedAtStart, updatedAtEnd))
            .sorted(buildComparator(safeSortField, safeSortOrder))
            .toList();

    PageSlice<IssueSearchView> pageSlice = PageSliceSupport.slice(filtered, safePage, safeSize);
    List<SystemTestIssueSearchRowResponse> records =
        pageSlice.records().stream().map(this::toResponse).toList();
    return new SystemTestIssueSearchListResponse(
        records, pageSlice.total(), pageSlice.page(), pageSlice.size(), safeSortField, safeSortOrder);
  }

  public SystemTestIssueSearchFilterOptionsResponse getFilterOptions(Long projectId) {
    List<IssueSearchView> scopedViews = loadScopedViews(projectId);
    return new SystemTestIssueSearchFilterOptionsResponse(
        toOptions(scopedViews, IssueSearchView::projectName),
        toOptions(scopedViews.stream().flatMap(view -> view.moduleNames().stream()).toList()),
        toOptions(
            scopedViews.stream()
                .map(IssueSearchView::phaseFilterValue)
                .filter(StringUtils::hasText)
                .toList()),
        toOptions(scopedViews, IssueSearchView::authorName),
        toOptions(scopedViews, IssueSearchView::assigneeName),
        toOptions(scopedViews, IssueSearchView::issueState),
        toOptions(scopedViews, IssueSearchView::severityLevel),
        toOptions(scopedViews, IssueSearchView::bugStatus),
        toOptions(scopedViews, IssueSearchView::category),
        toOptions(scopedViews, IssueSearchView::milestoneTitle));
  }

  private List<IssueSearchView> loadScopedViews(Long projectId) {
    Map<String, String> filters = new LinkedHashMap<>();
    if (projectId != null) {
      filters.put("projectId", String.valueOf(projectId));
    }
    try {
      return issueFactQueryService.query(FACT_SQL, filters, this::mapIssueFact).stream()
          .filter(IssueSearchView::inSystemTestScope)
          .toList();
    } catch (DataAccessException error) {
      return List.of();
    }
  }

  private IssueSearchView mapIssueFact(ResultSet rs, int rowNum) throws SQLException {
    return new IssueSearchView(
        rs.getLong("project_id"),
        text(rs.getString("project_name")),
        rs.getLong("issue_id"),
        rs.getInt("issue_iid"),
        text(rs.getString("title")),
        text(rs.getString("issue_state")),
        text(rs.getString("testing_phase")),
        text(rs.getString("system_test_label")),
        text(rs.getString("severity_level")),
        text(rs.getString("bug_status")),
        text(rs.getString("category")),
        text(rs.getString("milestone_title")),
        text(rs.getString("author_name")),
        text(rs.getString("assignee_name")),
        split(rs.getString("module_names")),
        split(rs.getString("label_names")),
        time(rs.getTimestamp("created_at_source")),
        time(rs.getTimestamp("updated_at_source")),
        time(rs.getTimestamp("closed_at_source")));
  }

  private SystemTestIssueSearchRowResponse toResponse(IssueSearchView view) {
    return new SystemTestIssueSearchRowResponse(
        view.issueId(),
        view.issueIid(),
        view.projectId(),
        view.projectName(),
        view.title(),
        view.issueState(),
        view.primaryPhaseLabel(),
        view.severityLevel(),
        view.bugStatus(),
        view.category(),
        view.milestoneTitle(),
        view.authorName(),
        view.assigneeName(),
        String.join("、", view.moduleNames()),
        view.createdAt(),
        view.updatedAt(),
        view.closedAt(),
        view.labels());
  }

  private boolean matchesKeyword(IssueSearchView view, String keyword) {
    String normalizedKeyword = TextQuerySupport.trimToNull(keyword);
    if (normalizedKeyword == null) {
      return true;
    }
    return TextQuerySupport.containsAbstractSearch(String.valueOf(view.issueIid()), normalizedKeyword)
        || TextQuerySupport.containsAbstractSearch(view.title(), normalizedKeyword)
        || TextQuerySupport.containsAbstractSearch(view.projectName(), normalizedKeyword)
        || TextQuerySupport.containsAbstractSearch(String.join(" ", view.moduleNames()), normalizedKeyword)
        || TextQuerySupport.containsAbstractSearch(view.primaryPhaseLabel(), normalizedKeyword)
        || TextQuerySupport.containsAbstractSearch(view.authorName(), normalizedKeyword)
        || TextQuerySupport.containsAbstractSearch(view.assigneeName(), normalizedKeyword)
        || TextQuerySupport.containsAbstractSearch(view.bugStatus(), normalizedKeyword)
        || TextQuerySupport.containsAbstractSearch(view.category(), normalizedKeyword)
        || TextQuerySupport.containsAbstractSearch(view.milestoneTitle(), normalizedKeyword);
  }

  private boolean matchesIssueIid(IssueSearchView view, String issueIid) {
    String normalized = TextQuerySupport.trimToNull(issueIid);
    return normalized == null
        || TextQuerySupport.containsIgnoreCase(String.valueOf(view.issueIid()), normalized);
  }

  private boolean matchesText(String source, String query) {
    return TextQuerySupport.containsAbstractSearch(source, query);
  }

  private boolean matchesModule(IssueSearchView view, String moduleName) {
    String normalized = TextQuerySupport.trimToNull(moduleName);
    if (normalized == null) {
      return true;
    }
    return view.moduleNames().stream()
        .anyMatch(item -> TextQuerySupport.equalsNormalized(item, normalized));
  }

  private boolean matchesTestingPhase(IssueSearchView view, String testingPhase) {
    String normalized = TextQuerySupport.trimToNull(testingPhase);
    return normalized == null || TextQuerySupport.equalsNormalized(view.phaseFilterValue(), normalized);
  }

  private boolean matchesEquals(String source, String target) {
    return TextQuerySupport.equalsNormalized(source, target);
  }

  private boolean matchesDateRange(LocalDateTime source, String start, String end) {
    LocalDate startDate = parseDate(start);
    if (startDate != null && (source == null || source.isBefore(startDate.atStartOfDay()))) {
      return false;
    }
    LocalDate endDate = parseDate(end);
    return endDate == null || (source != null && source.isBefore(endDate.plusDays(1).atStartOfDay()));
  }

  private Comparator<IssueSearchView> buildComparator(String sortField, String sortOrder) {
    Comparator<IssueSearchView> comparator =
        switch (sortField) {
          case "issueIid" -> SortSupport.nullableComparable(IssueSearchView::issueIid);
          case "title" -> SortSupport.nullableString(IssueSearchView::title);
          case "projectName" -> SortSupport.nullableString(IssueSearchView::projectName);
          case "moduleNames" -> SortSupport.nullableString(view -> String.join("、", view.moduleNames()));
          case "testingPhase" -> SortSupport.nullableString(IssueSearchView::primaryPhaseLabel);
          case "severityLevel" -> SortSupport.nullableString(IssueSearchView::severityLevel);
          case "bugStatus" -> SortSupport.nullableString(IssueSearchView::bugStatus);
          case "issueState" -> SortSupport.nullableString(IssueSearchView::issueState);
          case "authorName" -> SortSupport.nullableString(IssueSearchView::authorName);
          case "assigneeName" -> SortSupport.nullableString(IssueSearchView::assigneeName);
          case "category" -> SortSupport.nullableString(IssueSearchView::category);
          case "milestoneTitle" -> SortSupport.nullableString(IssueSearchView::milestoneTitle);
          case "createdAt" -> SortSupport.nullableComparable(IssueSearchView::createdAt);
          case "closedAt" -> SortSupport.nullableComparable(IssueSearchView::closedAt);
          default -> SortSupport.nullableComparable(IssueSearchView::updatedAt);
        };
    comparator = comparator.thenComparing(IssueSearchView::issueIid);
    return SortSupport.applyDirection(comparator, "asc".equalsIgnoreCase(sortOrder));
  }

  private String normalizeSortField(String sortField) {
    String normalized = TextQuerySupport.trimToNull(sortField);
    if (normalized == null) {
      return "updatedAt";
    }
    return switch (normalized) {
      case "issueIid",
          "title",
          "projectName",
          "moduleNames",
          "testingPhase",
          "severityLevel",
          "bugStatus",
          "issueState",
          "authorName",
          "assigneeName",
          "category",
          "milestoneTitle",
          "createdAt",
          "updatedAt",
          "closedAt" -> normalized;
      default -> "updatedAt";
    };
  }

  private String normalizeSortOrder(String sortOrder) {
    return "asc".equalsIgnoreCase(sortOrder) ? "asc" : "desc";
  }

  private List<OptionItemResponse> toOptions(
      List<IssueSearchView> rows, java.util.function.Function<IssueSearchView, String> extractor) {
    return OptionItemResponseFactory.from(rows, extractor, TextQuerySupport::trimToNull);
  }

  private List<OptionItemResponse> toOptions(List<String> values) {
    return OptionItemResponseFactory.from(values, TextQuerySupport::trimToNull);
  }

  private LocalDate parseDate(String value) {
    String normalized = TextQuerySupport.trimToNull(value);
    return normalized == null ? null : LocalDate.parse(normalized);
  }

  private LocalDateTime time(Timestamp timestamp) {
    return timestamp == null ? null : timestamp.toLocalDateTime();
  }

  private String text(String value) {
    return value == null ? "" : value.trim();
  }

  private List<String> split(String raw) {
    if (!StringUtils.hasText(raw)) {
      return List.of();
    }
    Set<String> values = new LinkedHashSet<>();
    for (String item : raw.split(",")) {
      String normalized = item == null ? "" : item.trim();
      if (!normalized.isEmpty()) {
        values.add(normalized);
      }
    }
    return List.copyOf(values);
  }

  private record IssueSearchView(
      Long projectId,
      String projectName,
      Long issueId,
      Integer issueIid,
      String title,
      String issueState,
      String testingPhase,
      String systemTestLabel,
      String severityLevel,
      String bugStatus,
      String category,
      String milestoneTitle,
      String authorName,
      String assigneeName,
      List<String> moduleNames,
      List<String> labels,
      LocalDateTime createdAt,
      LocalDateTime updatedAt,
      LocalDateTime closedAt) {
    boolean inSystemTestScope() {
      return StringUtils.hasText(primaryPhaseLabel());
    }

    String primaryPhaseLabel() {
      if (hasScope(testingPhase)) {
        return testingPhase;
      }
      if (hasScope(systemTestLabel)) {
        return systemTestLabel;
      }
      return labels.stream().filter(this::hasScope).findFirst().orElse("");
    }

    String phaseFilterValue() {
      String phase = primaryPhaseLabel();
      if (!StringUtils.hasText(phase)) {
        return "";
      }
      int systemTestIndex = phase.indexOf("系统测试");
      int regressionIndex = phase.indexOf("回归测试");
      int cutIndex = systemTestIndex >= 0 ? systemTestIndex : regressionIndex;
      if (cutIndex > 0) {
        return phase.substring(0, cutIndex).trim();
      }
      return phase.trim();
    }

    private boolean hasScope(String value) {
      return StringUtils.hasText(value)
          && (value.contains("系统测试") || value.contains("回归测试"));
    }
  }
}
