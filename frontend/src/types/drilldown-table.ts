export interface DrilldownMatrixRow {
  rowKey: string;
  rowLabel: string;
  values: Record<string, number>;
}

export interface DrilldownLeafColumn {
  key: string;
  label: string;
  width?: number;
  drilldown?: boolean;
  metricType?: 'count' | 'ratio';
  format?: (value: number) => string;
}

export interface DrilldownColumnGroup {
  key: string;
  label: string;
  columns: DrilldownLeafColumn[];
}

export interface DrilldownDetailColumn {
  prop: string;
  label: string;
  width?: number;
  minWidth?: number;
}

export interface DrilldownActiveCell {
  row: DrilldownMatrixRow;
  column: DrilldownLeafColumn;
  group: DrilldownColumnGroup;
}

