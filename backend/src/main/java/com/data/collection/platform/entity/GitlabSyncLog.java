package com.data.collection.platform.entity;

import java.time.LocalDateTime;

public class GitlabSyncLog {
  private Long id;
  private Long configId;
  private SyncType syncType;
  private SyncStatus status;
  private String message;
  private String whitelistSnapshot;
  private Integer tableCount;
  private Integer recordCount;
  private LocalDateTime startedAt;
  private LocalDateTime finishedAt;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getConfigId() { return configId; }
  public void setConfigId(Long configId) { this.configId = configId; }
  public SyncType getSyncType() { return syncType; }
  public void setSyncType(SyncType syncType) { this.syncType = syncType; }
  public SyncStatus getStatus() { return status; }
  public void setStatus(SyncStatus status) { this.status = status; }
  public String getMessage() { return message; }
  public void setMessage(String message) { this.message = message; }
  public String getWhitelistSnapshot() { return whitelistSnapshot; }
  public void setWhitelistSnapshot(String whitelistSnapshot) { this.whitelistSnapshot = whitelistSnapshot; }
  public Integer getTableCount() { return tableCount; }
  public void setTableCount(Integer tableCount) { this.tableCount = tableCount; }
  public Integer getRecordCount() { return recordCount; }
  public void setRecordCount(Integer recordCount) { this.recordCount = recordCount; }
  public LocalDateTime getStartedAt() { return startedAt; }
  public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
  public LocalDateTime getFinishedAt() { return finishedAt; }
  public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }
}
