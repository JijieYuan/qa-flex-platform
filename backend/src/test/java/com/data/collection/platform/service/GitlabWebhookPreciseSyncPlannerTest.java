package com.data.collection.platform.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GitlabWebhookPreciseSyncPlannerTest {

  private GitlabWebhookPreciseSyncPlanner planner;

  @BeforeEach
  void setUp() {
    planner = new GitlabWebhookPreciseSyncPlanner();
  }

  @Test
  void shouldPlanIssueWebhookToIssuesTable() {
    Map<String, Object> payload = Map.of(
        "object_kind", "issue",
        "object_attributes", Map.of("id", 101L));

    List<GitlabWebhookPreciseSyncTarget> targets = planner.planTargets(payload);

    assertThat(targets).containsExactly(
        new GitlabWebhookPreciseSyncTarget("issues", "id", 101L),
        new GitlabWebhookPreciseSyncTarget("issue_assignees", "issue_id", 101L),
        new GitlabWebhookPreciseSyncTarget("issue_metrics", "issue_id", 101L),
        new GitlabWebhookPreciseSyncTarget("label_links", "target_id", 101L));
  }

  @Test
  void shouldPlanMergeRequestWebhookToMergeRequestsAndMergeTrains() {
    Map<String, Object> payload = Map.of(
        "object_kind", "merge_request",
        "object_attributes", Map.of("id", 202L));

    List<GitlabWebhookPreciseSyncTarget> targets = planner.planTargets(payload);

    assertThat(targets).containsExactly(
        new GitlabWebhookPreciseSyncTarget("merge_requests", "id", 202L),
        new GitlabWebhookPreciseSyncTarget("merge_request_assignees", "merge_request_id", 202L),
        new GitlabWebhookPreciseSyncTarget("merge_request_reviewers", "merge_request_id", 202L),
        new GitlabWebhookPreciseSyncTarget("merge_request_metrics", "merge_request_id", 202L),
        new GitlabWebhookPreciseSyncTarget("label_links", "target_id", 202L),
        new GitlabWebhookPreciseSyncTarget("merge_trains", "merge_request_id", 202L));
  }

  @Test
  void shouldPlanNoteWebhookToNotesAndParentIssue() {
    Map<String, Object> payload = Map.of(
        "object_kind", "note",
        "object_attributes", Map.of(
            "id", 303L,
            "noteable_type", "Issue",
            "noteable_id", 404L));

    List<GitlabWebhookPreciseSyncTarget> targets = planner.planTargets(payload);

    assertThat(targets).containsExactly(
        new GitlabWebhookPreciseSyncTarget("notes", "id", 303L),
        new GitlabWebhookPreciseSyncTarget("issues", "id", 404L));
  }

  @Test
  void shouldPlanPipelineAndBuildWebhookToCiTables() {
    Map<String, Object> pipelinePayload = Map.of(
        "object_kind", "pipeline",
        "object_attributes", Map.of("id", 505L));
    Map<String, Object> buildPayload = Map.of(
        "object_kind", "build",
        "build_id", 606L);

    assertThat(planner.planTargets(pipelinePayload))
        .containsExactly(new GitlabWebhookPreciseSyncTarget("ci_pipelines", "id", 505L));
    assertThat(planner.planTargets(buildPayload))
        .containsExactly(new GitlabWebhookPreciseSyncTarget("ci_builds", "id", 606L));
  }

  @Test
  void shouldPlanDeploymentReleaseProjectAndUserWebhooks() {
    Map<String, Object> deploymentPayload = Map.of(
        "object_kind", "deployment",
        "object_attributes", Map.of("id", 707L));
    Map<String, Object> releasePayload = Map.of(
        "object_kind", "release",
        "object_attributes", Map.of("id", 808L));
    Map<String, Object> projectPayload = Map.of(
        "object_kind", "project",
        "project", Map.of("id", 909L));
    Map<String, Object> userPayload = Map.of(
        "object_kind", "user",
        "object_attributes", Map.of("id", 1001L));

    assertThat(planner.planTargets(deploymentPayload))
        .containsExactly(new GitlabWebhookPreciseSyncTarget("deployments", "id", 707L));
    assertThat(planner.planTargets(releasePayload))
        .containsExactly(new GitlabWebhookPreciseSyncTarget("releases", "id", 808L));
    assertThat(planner.planTargets(projectPayload))
        .containsExactly(
            new GitlabWebhookPreciseSyncTarget("projects", "id", 909L),
            new GitlabWebhookPreciseSyncTarget("members", "source_id", 909L),
            new GitlabWebhookPreciseSyncTarget("environments", "project_id", 909L),
            new GitlabWebhookPreciseSyncTarget("todos", "project_id", 909L));
    assertThat(planner.planTargets(userPayload))
        .containsExactly(new GitlabWebhookPreciseSyncTarget("users", "id", 1001L));
  }

  @Test
  void shouldReturnEmptyTargetsForUnsupportedWebhook() {
    Map<String, Object> payload = Map.of(
        "object_kind", "push",
        "project", Map.of("id", 707L));

    assertThat(planner.planTargets(payload)).isEmpty();
  }

  @Test
  void shouldBuildStableObjectKeyForIssueWebhook() {
    Map<String, Object> payload = Map.of(
        "object_kind", "issue",
        "object_attributes", Map.of("id", 101L));

    GitlabWebhookPreciseSyncPlan plan = planner.plan(payload);

    assertThat(plan.objectKey()).isEqualTo("issue:101");
    assertThat(plan.objectId()).isEqualTo("101");
    assertThat(plan.targets()).isNotEmpty();
  }
}
