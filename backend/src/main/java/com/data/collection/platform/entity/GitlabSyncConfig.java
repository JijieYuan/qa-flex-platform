package com.data.collection.platform.entity;

import java.time.LocalDateTime;
import java.util.List;

public class GitlabSyncConfig {
  private SourceMode sourceMode;
  private Long id;
  private String name;
  private boolean enabled;
  private boolean autoSyncEnabled;
  private WhitelistMode whitelistMode;
  private List<String> whitelistTables;
  private String dbHost;
  private Integer dbPort;
  private String dbName;
  private String dbUsername;
  private String dbPassword;
  private String dockerContainerName;
  private String webhookSecret;
  private Long webhookProjectId;
  private Integer compensationIntervalMinutes;
  private LocalDateTime lastFullSyncAt;
  private LocalDateTime lastIncrementalSyncAt;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public SourceMode getSourceMode() { return sourceMode; }
  public void setSourceMode(SourceMode sourceMode) { this.sourceMode = sourceMode; }
  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public boolean isEnabled() { return enabled; }
  public void setEnabled(boolean enabled) { this.enabled = enabled; }
  public boolean isAutoSyncEnabled() { return autoSyncEnabled; }
  public void setAutoSyncEnabled(boolean autoSyncEnabled) { this.autoSyncEnabled = autoSyncEnabled; }
  public WhitelistMode getWhitelistMode() { return whitelistMode; }
  public void setWhitelistMode(WhitelistMode whitelistMode) { this.whitelistMode = whitelistMode; }
  public List<String> getWhitelistTables() { return whitelistTables; }
  public void setWhitelistTables(List<String> whitelistTables) { this.whitelistTables = whitelistTables; }
  public String getDbHost() { return dbHost; }
  public void setDbHost(String dbHost) { this.dbHost = dbHost; }
  public Integer getDbPort() { return dbPort; }
  public void setDbPort(Integer dbPort) { this.dbPort = dbPort; }
  public String getDbName() { return dbName; }
  public void setDbName(String dbName) { this.dbName = dbName; }
  public String getDbUsername() { return dbUsername; }
  public void setDbUsername(String dbUsername) { this.dbUsername = dbUsername; }
  public String getDbPassword() { return dbPassword; }
  public void setDbPassword(String dbPassword) { this.dbPassword = dbPassword; }
  public String getDockerContainerName() { return dockerContainerName; }
  public void setDockerContainerName(String dockerContainerName) { this.dockerContainerName = dockerContainerName; }
  public String getWebhookSecret() { return webhookSecret; }
  public void setWebhookSecret(String webhookSecret) { this.webhookSecret = webhookSecret; }
  public Long getWebhookProjectId() { return webhookProjectId; }
  public void setWebhookProjectId(Long webhookProjectId) { this.webhookProjectId = webhookProjectId; }
  public Integer getCompensationIntervalMinutes() { return compensationIntervalMinutes; }
  public void setCompensationIntervalMinutes(Integer compensationIntervalMinutes) { this.compensationIntervalMinutes = compensationIntervalMinutes; }
  public LocalDateTime getLastFullSyncAt() { return lastFullSyncAt; }
  public void setLastFullSyncAt(LocalDateTime lastFullSyncAt) { this.lastFullSyncAt = lastFullSyncAt; }
  public LocalDateTime getLastIncrementalSyncAt() { return lastIncrementalSyncAt; }
  public void setLastIncrementalSyncAt(LocalDateTime lastIncrementalSyncAt) { this.lastIncrementalSyncAt = lastIncrementalSyncAt; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
  public LocalDateTime getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
