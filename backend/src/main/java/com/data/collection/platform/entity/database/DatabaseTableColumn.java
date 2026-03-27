package com.data.collection.platform.entity.database;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DatabaseTableColumn {
  private String key;
  private String label;
  private boolean sortable;
}
