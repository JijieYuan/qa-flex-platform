package com.data.collection.platform.entity;

import java.util.List;

public record GitlabWebhookRegistrationStatus(
    boolean supported,
    boolean configured,
    boolean registered,
    Long projectId,
    String webhookUrl,
    String message,
    List<RegisteredGitlabWebhook> hooks) {

  public record RegisteredGitlabWebhook(
      Long id,
      String url,
      boolean issuesEvents,
      boolean mergeRequestsEvents,
      boolean noteEvents,
      boolean pipelineEvents,
      boolean jobEvents,
      boolean releasesEvents,
      boolean enableSslVerification) {
  }
}
