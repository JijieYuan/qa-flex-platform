package com.data.collection.platform.service;

import com.data.collection.platform.entity.FactBuildResponse;
import com.data.collection.platform.entity.IssueFact;
import com.data.collection.platform.entity.MergeRequestFact;
import com.data.collection.platform.mapper.IssueFactMapper;
import com.data.collection.platform.mapper.MergeRequestFactMapper;
import java.math.BigDecimal;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class FactBuildService {
  private static final String DEFAULT_SOURCE_SYSTEM = "GITLAB";
  private static final String DEFAULT_SOURCE_INSTANCE = "default";
  private static final String MIRROR_INGEST_CHANNEL = "MIRROR";
  private static final String ISSUE_SOURCE_SQL = """
      with issue_labels as (
        select ll.target_id as issue_id,
               array_agg(distinct lower(l.title) order by lower(l.title))
                 filter (where l.title is not null and l.title <> '') as label_titles
          from ods_gitlab_label_links ll
          join ods_gitlab_labels l
            on l.id = ll.label_id
           and coalesce(l.mirror_deleted, false) = false
         where coalesce(ll.mirror_deleted, false) = false
           and ll.target_type = 'Issue'
         group by ll.target_id
      )
      select
        i.id as issue_id,
        i.iid as issue_iid,
        i.project_id,
        p.name as project_name,
        i.title,
        coalesce(author.name, '') as author_name,
        i.created_at,
        i.updated_at,
        coalesce(i.updated_at, i.created_at) as ods_updated_at,
        i.closed_at,
        i.state_id,
        labels.label_titles
      from ods_gitlab_issues i
      left join ods_gitlab_projects p
        on p.id = i.project_id
       and coalesce(p.mirror_deleted, false) = false
      left join ods_gitlab_users author
        on author.id = i.author_id
       and coalesce(author.mirror_deleted, false) = false
      left join issue_labels labels
        on labels.issue_id = i.id
      where coalesce(i.mirror_deleted, false) = false
      """;
  private static final String MERGE_REQUEST_SOURCE_SQL = """
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
               array_remove(
                 array_agg(
                   nullif(btrim(l.title), '')
                   order by coalesce(ll.source_updated_at, ll.updated_at, ll.created_at) desc nulls last, ll.id desc
                 ),
                 null
               ) as label_titles
          from ods_gitlab_label_links ll
          join ods_gitlab_labels l
            on l.id = ll.label_id
           and coalesce(l.mirror_deleted, false) = false
         where coalesce(ll.mirror_deleted, false) = false
           and ll.target_type = 'MergeRequest'
         group by ll.target_id
      ),
      imported_metrics as (
        select m.project_id,
               m.merge_request_id,
               m.merge_request_iid,
               m.comment_rate,
               m.comment_rate_source,
               m.defect_count,
               m.defect_count_source,
               m.source_summary as metric_source_summary,
               m.raw_payload as metric_raw_payload
          from code_review_external_metrics m
      ),
      form_records as (
        select f.project_id,
               f.request_iid,
               max(nullif(btrim(f.reviewer), '')) as reviewer_name,
               max(f.review_duration_minutes) as review_duration_minutes
          from collect_form_records f
         where f.deleted = false
           and f.resource_type = 'merge_request'
         group by f.project_id, f.request_iid
      )
      select
        mr.id as merge_request_id,
        mr.iid as merge_request_iid,
        mr.target_project_id as project_id,
        mr.title,
        p.name as project_name,
        coalesce(owner_ns.path || '/' || p.path, p.path) as repository_name,
        coalesce(metrics.merged_at, mr.updated_at) as merged_at,
        mr.created_at,
        mr.updated_at,
        coalesce(mr.updated_at, mr.created_at) as ods_updated_at,
        coalesce(author.name, '') as author_name,
        coalesce(merge_user.name, '') as merge_user_name,
        coalesce(nullif(trim(reviewers.reviewer_names), ''), nullif(trim(assignees.assignee_names), ''), '') as owner_name,
        coalesce(reviewers.reviewer_names, '') as reviewer_names,
        coalesce(assignees.assignee_names, '') as assignee_names,
        coalesce(mr.target_branch, '') as target_branch,
        coalesce(mr.source_branch, '') as source_branch,
        coalesce((labels.label_titles)[1], '') as module_name,
        labels.label_titles as label_titles,
        metrics.added_lines as added_lines,
        forms.review_duration_minutes,
        imported_metrics.comment_rate,
        imported_metrics.comment_rate_source,
        imported_metrics.defect_count,
        imported_metrics.defect_count_source,
        imported_metrics.metric_source_summary,
        imported_metrics.metric_raw_payload
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
      left join imported_metrics
        on imported_metrics.project_id = mr.target_project_id
       and (imported_metrics.merge_request_id = mr.id or imported_metrics.merge_request_iid = mr.iid)
      left join form_records forms
        on forms.project_id = mr.target_project_id
       and forms.request_iid = mr.iid
      where coalesce(mr.mirror_deleted, false) = false
      """;

  private final JdbcTemplate jdbcTemplate;
  private final IssueFactMapper issueFactMapper;
  private final MergeRequestFactMapper mergeRequestFactMapper;

  public FactBuildService(
      JdbcTemplate jdbcTemplate,
      IssueFactMapper issueFactMapper,
      MergeRequestFactMapper mergeRequestFactMapper) {
    this.jdbcTemplate = jdbcTemplate;
    this.issueFactMapper = issueFactMapper;
    this.mergeRequestFactMapper = mergeRequestFactMapper;
  }

  public FactBuildResponse rebuildAllFacts(boolean full) {
    FactBuildResponse issue = rebuildIssueFacts(full);
    FactBuildResponse mergeRequest = rebuildMergeRequestFacts(full);
    return new FactBuildResponse(
        "all",
        full,
        issue.affectedRows() + mergeRequest.affectedRows(),
        "事实表构建完成：议题 " + issue.affectedRows() + " 条，合并请求 " + mergeRequest.affectedRows() + " 条");
  }

  public FactBuildResponse rebuildIssueFacts(boolean full) {
    LocalDateTime changedSince = full ? null : getIssueFactChangedSince();
    String sql = ISSUE_SOURCE_SQL + (changedSince == null ? "" : " and coalesce(i.updated_at, i.created_at) > ?");
    try {
      List<IssueFact> facts =
          changedSince == null
              ? jdbcTemplate.query(sql, this::mapIssueFact)
              : jdbcTemplate.query(sql, this::mapIssueFact, Timestamp.valueOf(changedSince));
      facts.forEach(issueFactMapper::upsert);
      return new FactBuildResponse(
          "issue",
          full,
          facts.size(),
          changedSince == null ? "议题事实已全量构建" : "议题事实已按增量构建");
    } catch (DataAccessException e) {
      log.warn("Failed to rebuild issue facts", e);
      throw e;
    }
  }

  public FactBuildResponse rebuildMergeRequestFacts(boolean full) {
    LocalDateTime changedSince = full ? null : getMergeRequestFactChangedSince();
    String sql = MERGE_REQUEST_SOURCE_SQL + (changedSince == null ? "" : " and coalesce(mr.updated_at, mr.created_at) > ?");
    try {
      List<MergeRequestFact> facts =
          changedSince == null
              ? jdbcTemplate.query(sql, this::mapMergeRequestFact)
              : jdbcTemplate.query(sql, this::mapMergeRequestFact, Timestamp.valueOf(changedSince));
      facts.forEach(mergeRequestFactMapper::upsert);
      return new FactBuildResponse(
          "merge-request",
          full,
          facts.size(),
          changedSince == null ? "合并请求事实已全量构建" : "合并请求事实已按增量构建");
    } catch (DataAccessException e) {
      log.warn("Failed to rebuild merge request facts", e);
      throw e;
    }
  }

  private LocalDateTime getIssueFactChangedSince() {
    return jdbcTemplate.queryForObject(
        """
            select max(ods_updated_at)
              from issue_fact
             where source_system = ?
               and source_instance = ?
            """,
        LocalDateTime.class,
        DEFAULT_SOURCE_SYSTEM,
        DEFAULT_SOURCE_INSTANCE);
  }

  private LocalDateTime getMergeRequestFactChangedSince() {
    return jdbcTemplate.queryForObject(
        """
            select max(ods_updated_at)
              from merge_request_fact
             where source_system = ?
               and source_instance = ?
            """,
        LocalDateTime.class,
        DEFAULT_SOURCE_SYSTEM,
        DEFAULT_SOURCE_INSTANCE);
  }

  private IssueFact mapIssueFact(ResultSet rs, int rowNum) throws SQLException {
    List<String> labels = readTextArray(rs.getArray("label_titles"));
    String title = defaultText(rs.getString("title"));
    IssueFact fact = new IssueFact();
    fact.setSourceSystem(DEFAULT_SOURCE_SYSTEM);
    fact.setSourceInstance(DEFAULT_SOURCE_INSTANCE);
    fact.setIngestChannel(MIRROR_INGEST_CHANNEL);
    fact.setSourceSummary("GitLab issue 镜像聚合");
    fact.setRawPayload(null);
    fact.setProjectId(rs.getLong("project_id"));
    fact.setProjectName(defaultText(rs.getString("project_name")));
    fact.setIssueId(rs.getLong("issue_id"));
    fact.setIssueIid(rs.getLong("issue_iid"));
    fact.setTitle(title);
    fact.setIssueState(isClosed(rs) ? "closed" : "opened");
    fact.setAuthorName(defaultText(rs.getString("author_name")));
    fact.setCreatedAtSource(toLocalDateTime(rs.getTimestamp("created_at")));
    fact.setUpdatedAtSource(toLocalDateTime(rs.getTimestamp("updated_at")));
    fact.setOdsUpdatedAt(toLocalDateTime(rs.getTimestamp("ods_updated_at")));
    fact.setClosedAtSource(toLocalDateTime(rs.getTimestamp("closed_at")));
    fact.setModuleName(IssueFactNormalizationRules.normalizeModuleName(labels));
    fact.setTestingPhase(IssueFactNormalizationRules.normalizeTestingPhase(labels));
    fact.setSeverityLevel(IssueFactNormalizationRules.normalizeSeverityLevel(labels, title));
    fact.setUrgency(IssueFactNormalizationRules.normalizeUrgency(labels, title));
    fact.setBugStatus(isClosed(rs) ? "已关闭" : "未关闭");
    fact.setCategory(IssueFactNormalizationRules.normalizeCategory(labels, title));
    fact.setSystemTestLabel(IssueFactNormalizationRules.normalizeSystemTestLabel(labels));
    fact.setLabelNames(String.join(", ", labels));
    fact.setDelayIssue(IssueFactNormalizationRules.hasDelayFlag(labels, title));
    fact.setDelayCause(IssueFactNormalizationRules.inferDelayCause(labels, title));
    fact.setDeleted(false);
    return fact;
  }

  private MergeRequestFact mapMergeRequestFact(ResultSet rs, int rowNum) throws SQLException {
    List<String> labels = readTextArray(rs.getArray("label_titles"));
    MergeRequestFact fact = new MergeRequestFact();
    fact.setSourceSystem(DEFAULT_SOURCE_SYSTEM);
    fact.setSourceInstance(DEFAULT_SOURCE_INSTANCE);
    fact.setIngestChannel(MIRROR_INGEST_CHANNEL);
    fact.setSourceSummary(defaultText(rs.getString("metric_source_summary"), "GitLab merge request 镜像聚合"));
    fact.setRawPayload(defaultText(rs.getString("metric_raw_payload"), null));
    fact.setProjectId(rs.getLong("project_id"));
    fact.setProjectName(defaultText(rs.getString("project_name")));
    fact.setRepositoryName(defaultText(rs.getString("repository_name")));
    fact.setMergeRequestId(rs.getLong("merge_request_id"));
    fact.setMergeRequestIid(rs.getLong("merge_request_iid"));
    fact.setTitle(defaultText(rs.getString("title")));
    fact.setMergeRequestState(toLocalDateTime(rs.getTimestamp("merged_at")) == null ? "opened" : "merged");
    fact.setTargetBranch(defaultText(rs.getString("target_branch")));
    fact.setSourceBranch(defaultText(rs.getString("source_branch")));
    fact.setAuthorName(defaultText(rs.getString("author_name")));
    fact.setMergeUserName(defaultText(rs.getString("merge_user_name")));
    fact.setOwnerName(defaultText(rs.getString("owner_name")));
    fact.setReviewerNames(defaultText(rs.getString("reviewer_names")));
    fact.setAssigneeNames(defaultText(rs.getString("assignee_names")));
    fact.setModuleName(defaultText(rs.getString("module_name")));
    fact.setLabelNames(String.join(", ", labels));
    fact.setCreatedAtSource(toLocalDateTime(rs.getTimestamp("created_at")));
    fact.setUpdatedAtSource(toLocalDateTime(rs.getTimestamp("updated_at")));
    fact.setOdsUpdatedAt(toLocalDateTime(rs.getTimestamp("ods_updated_at")));
    fact.setMergedAtSource(toLocalDateTime(rs.getTimestamp("merged_at")));
    fact.setReviewStatus(rs.getObject("review_duration_minutes") == null ? "PENDING" : "COMPLETED");
    fact.setReviewDurationMinutes((Integer) rs.getObject("review_duration_minutes"));
    fact.setCommentRate((BigDecimal) rs.getObject("comment_rate"));
    fact.setCommentRateSource(defaultText(rs.getString("comment_rate_source")));
    fact.setDefectCount((Integer) rs.getObject("defect_count"));
    fact.setDefectCountSource(defaultText(rs.getString("defect_count_source")));
    fact.setScanStatus(null);
    fact.setScanBugCount(null);
    fact.setAddedLines((Integer) rs.getObject("added_lines"));
    fact.setDeleted(false);
    return fact;
  }

  private boolean isClosed(ResultSet rs) throws SQLException {
    Timestamp closedAt = rs.getTimestamp("closed_at");
    Integer stateId = (Integer) rs.getObject("state_id");
    return closedAt != null || (stateId != null && stateId != 1);
  }

  private LocalDateTime toLocalDateTime(Timestamp timestamp) {
    return timestamp == null ? null : timestamp.toLocalDateTime();
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
      String normalized = defaultText(value == null ? null : String.valueOf(value), null);
      if (normalized != null) {
        result.add(normalized.toLowerCase(Locale.ROOT));
      }
    }
    return result;
  }

  private String defaultText(String value) {
    return defaultText(value, "");
  }

  private String defaultText(String value, String fallback) {
    return StringUtils.hasText(value) ? value.trim() : fallback;
  }
}
