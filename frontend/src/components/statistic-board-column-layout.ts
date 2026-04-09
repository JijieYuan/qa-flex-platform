import type {
  StatisticColumnGroup,
  StatisticColumnLeaf,
  StatisticRowData,
} from '../api';
import { flattenStatisticColumnLeavesFromGroup } from '../api';
import type { StatisticBoardViewPrefs } from './statistic-board-view-prefs';

export function applyOrderedColumnsToGroup(group: StatisticColumnGroup, orderedLeafColumns: StatisticColumnLeaf[]): StatisticColumnGroup | null {
  const directColumnKeys = new Set((group.columns ?? []).map((leaf) => leaf.key));
  const directColumns = orderedLeafColumns.filter((column) => directColumnKeys.has(column.key));
  const children = (group.children ?? [])
    .map((child) => {
      const childLeafKeys = new Set(flattenStatisticColumnLeavesFromGroup(child).map((column) => column.key));
      const childColumns = orderedLeafColumns.filter((column) => childLeafKeys.has(column.key));
      return applyOrderedColumnsToGroup(child, childColumns);
    })
    .filter((child): child is StatisticColumnGroup => Boolean(child));
  if (!directColumns.length && !children.length) {
    return null;
  }
  return {
    ...group,
    children,
    columns: directColumns,
  };
}

export function resolveOrderedColumnGroups(columnGroups: StatisticColumnGroup[], prefs: StatisticBoardViewPrefs): StatisticColumnGroup[] {
  const visibleColumnKeySet = new Set(prefs.visibleColumnKeys);
  const groupMap = new Map(columnGroups.map((group) => [group.key, group]));
  return prefs.groupOrder
    .map((groupKey) => groupMap.get(groupKey))
    .filter((group): group is StatisticColumnGroup => Boolean(group))
    .map((group) => {
      const leafColumns = flattenStatisticColumnLeavesFromGroup(group);
      const columnOrder = prefs.columnOrderByGroup[group.key] ?? leafColumns.map((column) => column.key);
      const columnMap = new Map(leafColumns.map((column) => [column.key, column]));
      const orderedLeafColumns = columnOrder
        .map((columnKey) => columnMap.get(columnKey))
        .filter((column): column is StatisticColumnLeaf => Boolean(column))
        .filter((column) => visibleColumnKeySet.has(column.key));
      return applyOrderedColumnsToGroup(group, orderedLeafColumns);
    })
    .filter((group): group is StatisticColumnGroup => Boolean(group));
}

export function visualTextUnits(text: string) {
  return Array.from(text).reduce((total, character) => total + (/[\u0000-\u00ff]/.test(character) ? 1 : 2), 0);
}

export function headerLabelMinimumWidth(label: string, reservePx: number) {
  return Math.max(96, visualTextUnits(label) * 7 + reservePx);
}

export function computeFirstColumnWidth(rows: StatisticRowData[], widthStrategy: StatisticBoardViewPrefs['widthStrategy']) {
  const longestLabelLength = rows.reduce((max, row) => Math.max(max, row.rowLabel.length), 4);
  if (widthStrategy === 'compact') {
    return Math.min(148, Math.max(112, longestLabelLength * 14 + 24));
  }
  if (widthStrategy === 'header') {
    return Math.min(176, Math.max(120, longestLabelLength * 16 + 30));
  }
  return Math.min(220, Math.max(128, longestLabelLength * 18 + 42));
}

export function computeFirstColumnMinWidth(rowHeaderLabel: string, widthStrategy: StatisticBoardViewPrefs['widthStrategy']) {
  const baseWidth =
    widthStrategy === 'compact'
      ? 108
      : widthStrategy === 'header'
        ? 120
        : 132;
  return Math.max(baseWidth, headerLabelMinimumWidth(rowHeaderLabel, 96));
}

export function columnWidth(
  column: StatisticColumnLeaf,
  widthStrategy: StatisticBoardViewPrefs['widthStrategy'],
  rows: StatisticRowData[],
) {
  if (column.metricType.includes('count') || column.metricType.includes('ratio') || column.metricType.includes('number')) {
    return widthStrategy === 'compact' ? 96 : widthStrategy === 'header' ? 124 : 156;
  }
  if (widthStrategy === 'compact') {
    return compactColumnWidth(column);
  }
  if (widthStrategy === 'header') {
    return headerBasedWidth(column);
  }
  return contentBasedWidth(column, rows);
}

export function columnMinWidth(
  column: StatisticColumnLeaf,
  widthStrategy: StatisticBoardViewPrefs['widthStrategy'],
  rows: StatisticRowData[],
) {
  return Math.max(columnWidth(column, widthStrategy, rows), headerLabelMinimumWidth(column.label, 98));
}

export function columnResizable(column: StatisticColumnLeaf) {
  return !(column.metricType.includes('count') || column.metricType.includes('ratio') || column.metricType.includes('number'));
}

function compactColumnWidth(column: StatisticColumnLeaf) {
  if (column.metricType.includes('time') || column.metricType.includes('date')) {
    return 132;
  }
  return Math.min(136, Math.max(88, column.label.length * 11 + 24));
}

function headerBasedWidth(column: StatisticColumnLeaf) {
  return Math.min(196, Math.max(116, column.label.length * 16 + 34));
}

function contentBasedWidth(column: StatisticColumnLeaf, rows: StatisticRowData[]) {
  const maxLength = rows.reduce((current, row) => {
    const valueLength = (row.cells.find((item) => item.columnKey === column.key)?.displayValue ?? '').length;
    return Math.max(current, valueLength);
  }, column.label.length);
  if (column.metricType.includes('time') || column.metricType.includes('date')) {
    return Math.min(240, Math.max(156, maxLength * 9 + 34));
  }
  return Math.min(280, Math.max(132, maxLength * 13 + 26));
}
