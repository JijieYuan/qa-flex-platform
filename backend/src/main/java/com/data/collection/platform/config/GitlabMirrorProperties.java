package com.data.collection.platform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "platform.gitlab-mirror")
public class GitlabMirrorProperties {
  private boolean schedulerEnabled = true;
  private int schedulerDelayMs = 60000;
  private String webhookBaseUrl = "http://localhost:18080/api/gitlab-sync/webhook";
  private String dockerCommand = "docker";

  public boolean isSchedulerEnabled() {
    return schedulerEnabled;
  }

  public void setSchedulerEnabled(boolean schedulerEnabled) {
    this.schedulerEnabled = schedulerEnabled;
  }

  public int getSchedulerDelayMs() {
    return schedulerDelayMs;
  }

  public void setSchedulerDelayMs(int schedulerDelayMs) {
    this.schedulerDelayMs = schedulerDelayMs;
  }

  public String getWebhookBaseUrl() {
    return webhookBaseUrl;
  }

  public void setWebhookBaseUrl(String webhookBaseUrl) {
    this.webhookBaseUrl = webhookBaseUrl;
  }

  public String getDockerCommand() {
    return dockerCommand;
  }

  public void setDockerCommand(String dockerCommand) {
    this.dockerCommand = dockerCommand;
  }
}
