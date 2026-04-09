import { describe, expect, it } from 'vitest';
import type { StatisticColumnGroup, StatisticRowData } from '../api';
import {
  columnMinWidth,
  columnResizable,
  computeFirstColumnMinWidth,
  computeFirstColumnWidth,
  resolveOrderedColumnGroups,
} from './statistic-board-column-layout';
import type { StatisticBoardViewPrefs } from './statistic-board-view-prefs';

const columnGroups: StatisticColumnGroup[] = [
  {
    key: 'summary',
    label: 'summary',
    columns: [
      { key: 'count', label: 'count', drilldown: true, metricType: 'count' },
      { key: 'owner', label: 'owner', drilldown: true, metricType: 'text' },
    ],
  },
];

const prefs: StatisticBoardViewPrefs = {
  visibleColumnKeys: ['owner'],
  groupOrder: ['summary'],
  columnOrderByGroup: {
    summary: ['owner', 'count'],
  },
  sortColumnKey: '',
  sortDirection: 'default',
  widthStrategy: 'content',
};

const rows: StatisticRowData[] = [
  {
    rowKey: 'a',
    rowLabel: 'module-a',
    cells: [
      { columnKey: 'count', displayValue: '2', numericValue: 2, drilldown: true, detailParams: {} },
      { columnKey: 'owner', displayValue: 'owner-name-long', numericValue: null, drilldown: true, detailParams: {} },
    ],
  },
];

describe('statistic board column layout', () => {
  it('resolves visible ordered groups', () => {
    const result = resolveOrderedColumnGroups(columnGroups, prefs);

    expect(result).toHaveLength(1);
    expect(result[0].columns?.map((column) => column.key)).toEqual(['owner']);
  });

  it('computes widths from width strategy and content', () => {
    expect(computeFirstColumnWidth(rows, 'compact')).toBeGreaterThan(100);
    expect(computeFirstColumnMinWidth('row-label', 'content')).toBeGreaterThan(120);
    expect(columnMinWidth(columnGroups[0].columns![1], 'content', rows)).toBeGreaterThan(120);
    expect(columnResizable(columnGroups[0].columns![0])).toBe(false);
  });
});
