<script setup lang="ts">
import { computed, ref } from 'vue';
import type {
  DrilldownActiveCell,
  DrilldownColumnGroup,
  DrilldownDetailColumn,
  DrilldownLeafColumn,
  DrilldownMatrixRow,
} from '../types/drilldown-table';

const props = withDefaults(
  defineProps<{
    boardTitle: string;
    boardDescription?: string;
    drawerDescription?: string;
    columnGroups: DrilldownColumnGroup[];
    rows: DrilldownMatrixRow[];
    detailColumns: DrilldownDetailColumn[];
    detailBuilder: (activeCell: DrilldownActiveCell) => Record<string, unknown>[];
    summaryLabel?: string;
    drawerSize?: string;
    emptyText?: string;
  }>(),
  {
    boardDescription: '',
    drawerDescription: '这里展示的是当前统计单元格对应的明细记录，可继续扩展排序、字段模板与操作列。',
    summaryLabel: '总计',
    drawerSize: '56%',
    emptyText: '当前统计值为 0，没有对应的明细记录。',
  },
);

const activeCell = ref<DrilldownActiveCell | null>(null);
const drawerVisible = ref(false);
const currentPage = ref(1);
const pageSize = ref(10);

const totalColumns = computed(() => props.columnGroups.flatMap((group) => group.columns));

const summaryTotals = computed<Record<string, number>>(() => {
  const initial = Object.fromEntries(totalColumns.value.map((column) => [column.key, 0])) as Record<string, number>;
  for (const row of props.rows) {
    for (const column of totalColumns.value) {
      initial[column.key] += row.values[column.key] ?? 0;
    }
  }
  return initial;
});

const detailRecords = computed(() => {
  if (!activeCell.value) {
    return [];
  }
  return props.detailBuilder(activeCell.value);
});

const pagedRecords = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  return detailRecords.value.slice(start, start + pageSize.value);
});

const currentPageVisibleCount = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  if (detailRecords.value.length <= start) {
    return 0;
  }
  return Math.min(pageSize.value, detailRecords.value.length - start);
});

const drawerTitle = computed(() => {
  if (!activeCell.value) {
    return '统计明细';
  }
  return `${activeCell.value.row.rowLabel} / ${activeCell.value.group.label} / ${activeCell.value.column.label}`;
});

function formatCell(row: DrilldownMatrixRow, column: DrilldownLeafColumn) {
  const value = row.values[column.key] ?? 0;
  if (column.format) {
    return column.format(value);
  }
  return `${value}`;
}

function formatTotal(column: DrilldownLeafColumn) {
  const value = summaryTotals.value[column.key] ?? 0;
  if (column.format) {
    return column.format(value);
  }
  return `${value}`;
}

function openDrilldown(row: DrilldownMatrixRow, group: DrilldownColumnGroup, column: DrilldownLeafColumn) {
  if (!column.drilldown) {
    return;
  }
  activeCell.value = { row, group, column };
  currentPage.value = 1;
  drawerVisible.value = true;
}
</script>

<template>
  <el-card shadow="never" class="matrix-card">
    <template #header>
      <div class="matrix-header">
        <div>
          <div class="matrix-title">{{ boardTitle }}</div>
          <div v-if="boardDescription" class="matrix-caption">{{ boardDescription }}</div>
        </div>
        <div class="matrix-legend">
          <span class="matrix-legend-dot interactive" />
          <span>可下钻</span>
          <span class="matrix-legend-dot readonly" />
          <span>仅展示</span>
        </div>
      </div>
    </template>

    <div class="matrix-scroll">
      <table class="matrix-table">
        <thead>
          <tr>
            <th class="sticky-col module-head" rowspan="2">模块名</th>
            <th v-for="group in columnGroups" :key="group.key" :colspan="group.columns.length" class="group-head">
              {{ group.label }}
            </th>
          </tr>
          <tr>
            <th
              v-for="column in totalColumns"
              :key="column.key"
              class="leaf-head"
              :style="{ minWidth: `${column.width ?? 112}px` }"
            >
              {{ column.label }}
            </th>
          </tr>
        </thead>

        <tbody>
          <tr v-for="row in rows" :key="row.rowKey">
            <th class="sticky-col row-label">{{ row.rowLabel }}</th>
            <td v-for="group in columnGroups" :key="`${row.rowKey}-${group.key}`" class="group-cell-holder">
              <div class="group-cell-grid" :style="{ gridTemplateColumns: `repeat(${group.columns.length}, minmax(112px, 1fr))` }">
                <button
                  v-for="column in group.columns"
                  :key="`${row.rowKey}-${column.key}`"
                  class="metric-chip"
                  :class="{ interactive: column.drilldown, readonly: !column.drilldown }"
                  type="button"
                  @click="openDrilldown(row, group, column)"
                >
                  {{ formatCell(row, column) }}
                </button>
              </div>
            </td>
          </tr>

          <tr class="summary-row">
            <th class="sticky-col row-label">{{ summaryLabel }}</th>
            <td v-for="group in columnGroups" :key="`summary-${group.key}`" class="group-cell-holder">
              <div class="group-cell-grid" :style="{ gridTemplateColumns: `repeat(${group.columns.length}, minmax(112px, 1fr))` }">
                <div
                  v-for="column in group.columns"
                  :key="`summary-${column.key}`"
                  class="metric-chip total"
                >
                  {{ formatTotal(column) }}
                </div>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </el-card>

  <el-drawer v-model="drawerVisible" :title="drawerTitle" :size="drawerSize" destroy-on-close class="matrix-detail-drawer">
    <template #header>
      <div class="matrix-drawer-header">
        <div>
          <div class="matrix-drawer-title">{{ drawerTitle }}</div>
          <div class="matrix-drawer-caption">{{ drawerDescription }}</div>
        </div>
        <div class="matrix-drawer-tags">
          <slot name="drawer-tags" :active-cell="activeCell" />
        </div>
      </div>
    </template>

    <slot name="drawer-toolbar" :active-cell="activeCell" />

    <el-empty v-if="detailRecords.length === 0" :description="emptyText" />

    <template v-else>
      <el-table :data="pagedRecords" border stripe class="matrix-detail-table">
        <el-table-column
          v-for="column in detailColumns"
          :key="column.prop"
          :prop="column.prop"
          :label="column.label"
          :width="column.width"
          :min-width="column.minWidth"
        />
      </el-table>

      <div class="matrix-detail-footer">
        <div class="matrix-detail-summary">
          共 <strong>{{ detailRecords.length }}</strong> 条记录，当前展示
          <strong>{{ currentPageVisibleCount }}</strong> 条。
        </div>
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          background
          layout="total, sizes, prev, pager, next"
          :total="detailRecords.length"
          :page-sizes="[10, 20, 50]"
        />
      </div>
    </template>
  </el-drawer>
</template>

<style scoped>
.matrix-card {
  border-radius: 24px !important;
  border: 1px solid rgba(208, 220, 236, 0.92) !important;
  background: rgba(255, 255, 255, 0.94) !important;
  box-shadow: 0 22px 44px rgba(15, 38, 71, 0.08) !important;
}

.matrix-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.matrix-title {
  font-size: 18px;
  font-weight: 800;
}

.matrix-caption {
  margin-top: 6px;
  color: #6b7f95;
  font-size: 13px;
}

.matrix-legend {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #6b7f95;
  font-size: 12px;
}

.matrix-legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 999px;
}

.matrix-legend-dot.interactive {
  background: #5e9cff;
}

.matrix-legend-dot.readonly {
  background: #c8d4e3;
}

.matrix-scroll {
  overflow: auto;
  border-radius: 18px;
  border: 1px solid rgba(223, 231, 241, 0.96);
}

.matrix-table {
  width: 100%;
  min-width: 1280px;
  border-collapse: separate;
  border-spacing: 0;
  background: #fff;
}

.matrix-table th,
.matrix-table td {
  border-right: 1px solid rgba(230, 236, 244, 0.96);
  border-bottom: 1px solid rgba(230, 236, 244, 0.96);
}

.matrix-table thead th {
  position: sticky;
  top: 0;
  z-index: 2;
}

.group-head {
  padding: 16px 12px;
  background: linear-gradient(180deg, #f7fbff 0%, #eef5fd 100%);
  color: #18324e;
  font-size: 13px;
  font-weight: 800;
  text-align: center;
}

.leaf-head {
  padding: 14px 10px;
  background: linear-gradient(180deg, #fcfdff 0%, #f5f8fc 100%);
  color: #5c7188;
  font-size: 12px;
  font-weight: 700;
  text-align: center;
}

.module-head,
.row-label {
  left: 0;
  z-index: 3;
  min-width: 150px;
  text-align: left;
}

.module-head {
  padding: 0 16px;
  background: linear-gradient(180deg, #f6fbff 0%, #edf5fd 100%);
  color: #18324e;
  font-size: 13px;
  font-weight: 800;
}

.row-label {
  padding: 18px 16px;
  background: #fbfdff;
  color: #24384d;
  font-size: 14px;
  font-weight: 700;
}

.sticky-col {
  position: sticky;
}

.group-cell-holder {
  padding: 0;
}

.group-cell-grid {
  display: grid;
}

.metric-chip {
  min-height: 58px;
  padding: 0 12px;
  border: 0;
  border-right: 1px solid rgba(234, 239, 246, 0.96);
  background: #fff;
  color: #15314d;
  font-size: 15px;
  font-weight: 700;
  cursor: default;
  transition: 160ms ease;
}

.metric-chip:last-child {
  border-right: 0;
}

.metric-chip.interactive {
  cursor: pointer;
  color: #1f69e2;
  background: linear-gradient(180deg, rgba(242, 248, 255, 0.9), rgba(238, 245, 255, 0.95));
}

.metric-chip.interactive:hover {
  background: linear-gradient(180deg, rgba(221, 236, 255, 0.95), rgba(212, 229, 255, 0.98));
  box-shadow: inset 0 0 0 1px rgba(56, 120, 241, 0.18);
}

.metric-chip.readonly {
  color: #53677d;
}

.metric-chip.total {
  background: linear-gradient(180deg, #f7fbff, #f0f5fb);
  color: #143353;
  font-weight: 800;
}

.summary-row .row-label {
  background: linear-gradient(180deg, #f4f8fc, #edf3fa);
}

.matrix-drawer-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  width: 100%;
}

.matrix-drawer-title {
  font-size: 18px;
  font-weight: 800;
  color: #14263b;
}

.matrix-drawer-caption {
  margin-top: 8px;
  color: #6a7f96;
  font-size: 13px;
}

.matrix-drawer-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.matrix-detail-table {
  margin-top: 6px;
}

.matrix-detail-footer {
  margin-top: 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.matrix-detail-summary {
  color: #60748b;
  font-size: 13px;
}
</style>
