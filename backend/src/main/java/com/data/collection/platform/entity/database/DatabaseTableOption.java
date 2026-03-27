package com.data.collection.platform.entity.database;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DatabaseTableOption {
  private String tableName;
  private String label;
}
