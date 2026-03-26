package com.data.collection.platform.entity;

import java.time.LocalDateTime;

public class SyncProgress {
  private String phase;
  private int totalTables;
  private int completedTables;
  private int syncedRecords;
  private String currentTable;
  private LocalDateTime startedAt;

  public String getPhase() { return phase; }
  public void setPhase(String phase) { this.phase = phase; }
  public int getTotalTables() { return totalTables; }
  public void setTotalTables(int totalTables) { this.totalTables = totalTables; }
  public int getCompletedTables() { return completedTables; }
  public void setCompletedTables(int completedTables) { this.completedTables = completedTables; }
  public int getSyncedRecords() { return syncedRecords; }
  public void setSyncedRecords(int syncedRecords) { this.syncedRecords = syncedRecords; }
  public String getCurrentTable() { return currentTable; }
  public void setCurrentTable(String currentTable) { this.currentTable = currentTable; }
  public LocalDateTime getStartedAt() { return startedAt; }
  public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
}
