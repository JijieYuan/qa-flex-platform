package com.data.collection.platform.service;

import com.data.collection.platform.entity.CodeReviewIllegalRecordFilterOptionsResponse;
import com.data.collection.platform.entity.CodeReviewIllegalRecordListResponse;
import com.data.collection.platform.entity.CodeReviewIllegalRecordRowResponse;
import com.data.collection.platform.entity.OptionItemResponse;
import java.sql.Array;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class CodeReviewIllegalRecordService {

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

  private static final String BASE_SQL = """
      with reviewer_names as (
        select mr.merge_request_id,
               string_agg(distinct u.name, ', ' order by u.name) as reviewer_names
          from ods_gitlab_merge_request_reviewers mr
          join ods_gitlab_users u
            on u.id = mr.user_id
           and coalesce(u.mirror_deleted, false) = false
         where coalesce(mr.mirror_deleted, false) = false
         group by mr.merge_request_id
      ),
      assignee_names as (
        select ma.merge_request_id,
               string_agg(distinct u.name, ', ' order by u.name) as assignee_names
          from ods_gitlab_merge_request_assignees ma
          join ods_gitlab_users u
            on u.id = ma.user_id
           and coalesce(u.mirror_deleted, false) = false
         where coalesce(ma.mirror_deleted, false) = false
         group by ma.merge_request_id
      ),
      merge_request_labels as (
        select ll.target_id as merge_request_id,
               array_agg(distinct l.title order by l.title) filter (where l.title is not null and l.title <> '') as label_titles
          from ods_gitlab_label_links ll
          join ods_gitlab_labels l
            on l.id = ll.label_id
           and coalesce(l.mirror_deleted, false) = false
         where coalesce(ll.mirror_deleted, false) = false
           and ll.target_type = 'MergeRequest'
         group by ll.target_id
      )
      select
        mr.id as merge_request_id,
        mr.iid as merge_request_iid,
        mr.target_project_id as project_id,
        mr.title as merge_request_content,
        p.name as project_name,
        coalesce(owner_ns.path || '/' || p.path, p.path) as repository_name,
        coalesce(metrics.merged_at, mr.updated_at) as merged_at,
        coalesce(merge_user.name, author.name, '') as merged_by,
        coalesce(nullif(trim(reviewers.reviewer_names), ''), nullif(trim(assignees.assignee_names), ''), '') as owner,
        coalesce(mr.target_branch, '') as target_branch,
        coalesce((labels.label_titles)[1], '') as module_name,
        labels.label_titles as label_titles,
        metrics.added_lines as added_lines,
        cast(null as double precision) as comment_rate,
        cast(null as integer) as defect_count
      from ods_gitlab_merge_requests mr
      left join ods_gitlab_merge_request_metrics metrics
        on metrics.merge_request_id = mr.id
       and coalesce(metrics.mirror_deleted, false) = false
      left join ods_gitlab_projects p
        on p.id = mr.target_project_id
       and coalesce(p.mirror_deleted, false) = false
      left join ods_gitlab_namespaces owner_ns
        on owner_ns.id = p.namespace_id
       and coalesce(owner_ns.mirror_deleted, false) = false
      left join ods_gitlab_users author
        on author.id = mr.author_id
       and coalesce(author.mirror_deleted, false) = false
      left join ods_gitlab_users merge_user
        on merge_user.id = mr.merge_user_id
       and coalesce(merge_user.mirror_deleted, false) = false
      left join reviewer_names reviewers
        on reviewers.merge_request_id = mr.id
      left join assignee_names assignees
        on assignees.merge_request_id = mr.id
      left join merge_request_labels labels
        on labels.merge_request_id = mr.id
      where coalesce(mr.mirror_deleted, false) = false
      """;

  private final JdbcTemplate jdbcTemplate;
  private final GitlabMirrorSyncService gitlabMirrorSyncService;
  private final String defaultGitlabBaseUrl;

  public CodeReviewIllegalRecordService(
      JdbcTemplate jdbcTemplate,
      GitlabMirrorSyncService gitlabMirrorSyncService,
      @Value("${gitlab-mirror.web-base-url:http://172.22.10.233}") String defaultGitlabBaseUrl) {
    this.jdbcTemplate = jdbcTemplate;
    this.gitlabMirrorSyncService = gitlabMirrorSyncService;
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
    refreshMirrorForRealtimeView();

    int safePage = page <= 0 ? 1 : page;
    int safeSize = size <= 0 ? 20 : Math.min(size, 100);
    String safeSortField = normalizeSortField(sortField);
    String safeSortOrder = normalizeSortOrder(sortOrder);

    List<IllegalRecordView> filtered = loadSources().stream()
        .map(this::toView)
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
    refreshMirrorForRealtimeView();

    List<IllegalRecordView> rows = loadSources().stream()
        .map(this::toView)
        .filter(row -> matchesProjectId(row, projectId))
        .toList();

    return new CodeReviewIllegalRecordFilterOptionsResponse(
        List.of(new OptionItemResponse("合并请求", "merge_request")),
        toOptions(rows, IllegalRecordView::repositoryName),
        toOptions(rows.stream().flatMap(row -> row.illegalTypes().stream()).toList()),
        toOptions(rows, IllegalRecordView::targetBranch),
        toOptions(rows, IllegalRecordView::mergedBy),
        toOptions(rows, IllegalRecordView::moduleName),
        toOptions(rows, IllegalRecordView::projectName));
  }

  private void refreshMirrorForRealtimeView() {
    try {
      gitlabMirrorSyncService.refreshTablesOnDemand(REALTIME_REFRESH_TABLES, "code-review-illegal-records");
    } catch (Exception e) {
      log.warn("On-demand mirror refresh for code review illegal records failed, fallback to current mirror snapshot", e);
    }
  }

  private List<IllegalRecordSource> loadSources() {
    return jdbcTemplate.query(BASE_SQL, this::mapSource);
  }

  private IllegalRecordSource mapSource(ResultSet rs, int rowNum) throws SQLException {
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
        readTextArray(rs.getArray("label_titles")),
        (Double) rs.getObject("comment_rate"),
        (Integer) rs.getObject("defect_count"),
        (Integer) rs.getObject("added_lines"));
  }

  private List<String> readTextArray(Array array) throws SQLException {
    if (array == null) {
      return List.of();
    }
    Object raw = array.getArray();
    if (!(raw instanceof Object[] values)) {
      return List.of();
    }
    List<String> result = new ArrayList<>();
    for (Object value : values) {
      String text = normalizeText(value == null ? null : String.valueOf(value));
      if (text != null) {
        result.add(text);
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
