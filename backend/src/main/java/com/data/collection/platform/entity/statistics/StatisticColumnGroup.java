package com.data.collection.platform.entity.statistics;

import java.util.ArrayList;
import java.util.List;

public record StatisticColumnGroup(
    String key,
    String label,
    List<StatisticColumnGroup> children,
    List<StatisticColumnLeaf> columns) {

  public StatisticColumnGroup {
    children = children == null ? List.of() : List.copyOf(children);
    columns = columns == null ? List.of() : List.copyOf(columns);
  }

  public StatisticColumnGroup(String key, String label, List<StatisticColumnLeaf> columns) {
    this(key, label, List.of(), columns);
  }

  public static StatisticColumnGroup withChildren(String key, String label, List<StatisticColumnGroup> children) {
    return new StatisticColumnGroup(key, label, children, List.of());
  }

  public List<StatisticColumnLeaf> leafColumns() {
    List<StatisticColumnLeaf> leaves = new ArrayList<>(columns);
    for (StatisticColumnGroup child : children) {
      leaves.addAll(child.leafColumns());
    }
    return List.copyOf(leaves);
  }

  public int columnCount() {
    return leafColumns().size();
  }
}
