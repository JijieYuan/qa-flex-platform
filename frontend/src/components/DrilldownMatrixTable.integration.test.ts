import { nextTick } from 'vue';
import { afterEach, describe, expect, it } from 'vitest';
import { mount } from '@vue/test-utils';
import ElementPlus from 'element-plus';
import DrilldownMatrixTable from './DrilldownMatrixTable.vue';
import type {
  DrilldownActiveCell,
  DrilldownColumnGroup,
  DrilldownDetailColumn,
  DrilldownMatrixRow,
} from '../types/drilldown-table';

const baseColumns: DrilldownColumnGroup[] = [
  {
    key: 'level-1',
    label: '一级缺陷',
    columns: [
      { key: 'fallback', label: '回退(个)', drilldown: true, metricType: 'count' },
      { key: 'repaired', label: '已修复数', drilldown: false, metricType: 'count' },
    ],
  },
  {
    key: 'risk',
    label: '风险建议',
    columns: [{ key: 'suggestion', label: '建议类缺陷(个)', drilldown: true, metricType: 'count' }],
  },
];

const baseRows: DrilldownMatrixRow[] = [
  {
    rowKey: 'working-drawing',
    rowLabel: '工程图',
    values: {
      fallback: 6,
      repaired: 3,
      suggestion: 1,
    },
  },
  {
    rowKey: 'sketch',
    rowLabel: '草图',
    values: {
      fallback: 2,
      repaired: 1,
      suggestion: 0,
    },
  },
];

const detailColumns: DrilldownDetailColumn[] = [
  { prop: 'issueNo', label: '缺陷编号', width: 120 },
  { prop: 'title', label: '缺陷标题', minWidth: 220 },
];

function detailBuilder(activeCell: DrilldownActiveCell) {
  const count = Math.max(0, Math.round(activeCell.row.values[activeCell.column.key] ?? 0));
  return Array.from({ length: count }, (_, index) => ({
    issueNo: `DEF-${String(index + 1).padStart(3, '0')}`,
    title: `${activeCell.row.rowLabel}-${activeCell.column.label}-${index + 1}`,
  }));
}

function createWrapper(override?: Partial<InstanceType<typeof DrilldownMatrixTable>['$props']>) {
  return mount(DrilldownMatrixTable, {
    attachTo: document.body,
    props: {
      boardTitle: '汇总统计矩阵',
      boardDescription: '用于验证抽象表格组件的复用能力。',
      columnGroups: baseColumns,
      rows: baseRows,
      detailColumns,
      detailBuilder,
      ...override,
    },
    global: {
      plugins: [ElementPlus],
      stubs: {
        transition: false,
        teleport: true,
        ElDrawer: {
          props: ['modelValue', 'title'],
          template: `
            <section v-if="modelValue" class="drawer-stub">
              <header class="drawer-stub-header">
                <slot name="header">
                  <div class="matrix-drawer-title">{{ title }}</div>
                </slot>
              </header>
              <div class="drawer-stub-body">
                <slot />
              </div>
            </section>
          `,
        },
      },
    },
  });
}

afterEach(() => {
  document.body.innerHTML = '';
});

function queryBodyText(selector: string) {
  return document.body.querySelector(selector)?.textContent?.trim() ?? '';
}

describe('DrilldownMatrixTable integration', () => {
  it('renders grouped headers and row labels correctly', () => {
    const wrapper = createWrapper();

    expect(wrapper.text()).toContain('汇总统计矩阵');
    expect(wrapper.text()).toContain('一级缺陷');
    expect(wrapper.text()).toContain('风险建议');
    expect(wrapper.text()).toContain('工程图');
    expect(wrapper.text()).toContain('草图');
  });

  it('opens detail drawer and keeps record count equal to the clicked metric value', async () => {
    const wrapper = createWrapper();

    const interactiveButtons = wrapper.findAll('button.metric-chip.interactive');
    await interactiveButtons[0].trigger('click');
    await nextTick();
    await nextTick();

    expect(queryBodyText('.matrix-drawer-title')).toBe('工程图 / 一级缺陷 / 回退(个)');
    expect(queryBodyText('.matrix-detail-summary')).toContain('共 6 条记录');
    expect(queryBodyText('.matrix-detail-summary')).toContain('当前展示 6 条');
  });

  it('does not open drawer when clicking a readonly metric cell', async () => {
    const wrapper = createWrapper();

    const readonlyButton = wrapper.findAll('button.metric-chip.readonly')[0];
    await readonlyButton.trigger('click');
    await nextTick();

    expect(document.body.querySelector('.matrix-drawer-title')).toBeNull();
    expect(document.body.querySelector('.matrix-detail-summary')).toBeNull();
  });

  it('supports adding and removing rows or columns through prop updates', async () => {
    const wrapper = createWrapper();

    const newRows: DrilldownMatrixRow[] = [
      ...baseRows,
      {
        rowKey: 'feature',
        rowLabel: '特征',
        values: {
          fallback: 4,
          repaired: 2,
          suggestion: 3,
        },
      },
    ];

    const newColumns: DrilldownColumnGroup[] = [
      {
        key: 'level-1',
        label: '一级缺陷',
        columns: [{ key: 'fallback', label: '回退(个)', drilldown: true, metricType: 'count' }],
      },
    ];

    await wrapper.setProps({
      rows: newRows,
      columnGroups: newColumns,
    });
    await nextTick();

    expect(wrapper.text()).toContain('特征');
    expect(wrapper.text()).not.toContain('风险建议');
    expect(wrapper.text()).not.toContain('建议类缺陷(个)');
  });

  it('supports changing a column from readonly to drilldown-enabled through configuration updates', async () => {
    const wrapper = createWrapper({
      columnGroups: [
        {
          key: 'level-1',
          label: '一级缺陷',
          columns: [{ key: 'repaired', label: '已修复数', drilldown: false, metricType: 'count' }],
        },
      ],
      rows: [
        {
          rowKey: 'working-drawing',
          rowLabel: '工程图',
          values: {
            repaired: 3,
          },
        },
      ],
    });

    const readonlyButton = wrapper.find('button.metric-chip.readonly');
    await readonlyButton.trigger('click');
    await nextTick();
    expect(document.body.textContent ?? '').not.toContain('共 3 条记录');

    await wrapper.setProps({
      columnGroups: [
        {
          key: 'level-1',
          label: '一级缺陷',
          columns: [{ key: 'repaired', label: '已修复数', drilldown: true, metricType: 'count' }],
        },
      ],
    });
    await nextTick();

    const interactiveButton = wrapper.find('button.metric-chip.interactive');
    await interactiveButton.trigger('click');
    await nextTick();
    await nextTick();

    expect(queryBodyText('.matrix-drawer-title')).toBe('工程图 / 一级缺陷 / 已修复数');
    expect(queryBodyText('.matrix-detail-summary')).toContain('共 3 条记录');
  });
});
