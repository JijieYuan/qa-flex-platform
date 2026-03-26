package com.data.collection.platform.entity;

import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SyncProgress {
  private String phase;
  private int totalTables;
  private int completedTables;
  private int syncedRecords;
  private String currentTable;
  private LocalDateTime startedAt;
}
