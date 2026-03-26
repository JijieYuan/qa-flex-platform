<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { ArrowRight, Download, RefreshRight, Search } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import {
  api,
  type StatisticBoardResponse,
  type StatisticCellData,
  type StatisticDetailColumn,
  type StatisticDetailResponse,
  type StatisticFilterField,
  type StatisticRowData,
} from '../api';
import {
  defaultVisibleColumnKeys,
  loadStatisticBoardViewPrefs,
  resetStatisticBoardViewPrefs,
  saveStatisticBoardViewPrefs,
} from './statistic-board-view-prefs';

const props = defineProps<{
  boardKey: string;
}>();

const loading = ref(false);
const detailLoading = ref(false);
const board = ref<StatisticBoardResponse | null>(null);
const errorMessage = ref('');
const filters = reactive<Record<string, string>>({});
const detailVisible = ref(false);
const activeRow = ref<StatisticRowData | null>(null);
const activeCell = ref<StatisticCellData | null>(null);
const detail = ref<StatisticDetailResponse | null>(null);
const settingsVisible = ref(false);
const persistedVisibleColumnKeys = ref<string[]>([]);
const draftVisibleColumnKeys = ref<string[]>([]);

const detailPagination = reactive({
  page: 1,
  size: 10,
  sortField: '',
  sortOrder: 'descending',
});

const flatColumns = computed(() => board.value?.definition.columnGroups.flatMap((group) => group.columns) ?? []);

const activeFilterFields = computed(() => board.value?.definition.filters ?? []);

const visibleColumnKeySet = computed(() => new Set(persistedVisibleColumnKeys.value));

const visibleColumnGroups = computed(() => {
  if (!board.value) {
    return [];
  }
  return board.value.definition.columnGroups
    .map((group) => ({
      ...group,
      columns: group.columns.filter((column) => visibleColumnKeySet.value.has(column.key)),
    }))
    .filter((group) => group.columns.length > 0);
});

const currentVisibleColumnCount = computed(() => persistedVisibleColumnKeys.value.length);

function initializeFilters(fields: StatisticFilterField[], appliedFilters?: Record<string, string>) {
  const activeKeys = new Set(fields.map((field) => field.key));
  Object.keys(filters).forEach((key) => {
    if (!activeKeys.has(key)) {
      delete filters[key];
    }
  });
  for (const field of fields) {
    const value = appliedFilters?.[field.key] ?? field.defaultValue ?? '';
    filters[field.key] = value;
  }
}

function applyStoredViewPrefs(response: StatisticBoardResponse) {
  const prefs = loadStatisticBoardViewPrefs(props.boardKey, response.definition);
  persistedVisibleColumnKeys.value = prefs.visibleColumnKeys;
  draftVisibleColumnKeys.value = [...prefs.visibleColumnKeys];
}

function buildFilterPayload() {
  return Object.fromEntries(
    Object.entries(filters)
      .map(([key, value]) => [key, value?.trim?.() ?? value])
      .filter(([, value]) => value !== '' && value != null),
  );
}

async function loadBoard(showError = true) {
  loading.value = true;
  errorMessage.value = '';
  try {
    const response = await api.getStatisticBoard(props.boardKey, buildFilterPayload());
    board.value = response;
    initializeFilters(response.definition.filters, response.appliedFilters);
    applyStoredViewPrefs(response);
    if (detailPagination.size <= 0) {
      detailPagination.size = response.definition.defaultPageSize ?? 10;
    }
  } catch (error) {
    errorMessage.value = (error as Error).message;
    if (showError) {
      ElMessage.error((error as Error).message);
    }
  } finally {
    loading.value = false;
  }
}

async function exportBoard() {
  try {
    const csv = await api.exportStatisticBoard(props.boardKey, buildFilterPayload());
    const blob = new Blob([`\uFEFF${csv}`], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = `${props.boardKey}.csv`;
    document.body.appendChild(anchor);
    anchor.click();
    document.body.removeChild(anchor);
    URL.revokeObjectURL(url);
    ElMessage.success('导出成功');
  } catch (error) {
    ElMessage.error((error as Error).message);
  }
}

function resetFilters() {
  if (!board.value) {
    return;
  }
  initializeFilters(board.value.definition.filters);
  void loadBoard();
}

function openSettings() {
  if (!board.value) {
    return;
  }
  draftVisibleColumnKeys.value = [...persistedVisibleColumnKeys.value];
  settingsVisible.value = true;
}

function handleSettingsCommand(command: string) {
  if (command === 'open-settings') {
    openSettings();
    return;
  }
  if (command === 'restore-default-view') {
    restoreDefaultView();
  }
}

function saveViewPrefs() {
  if (!board.value) {
    return;
  }
  if (!draftVisibleColumnKeys.value.length) {
    ElMessage.warning('至少保留一列用于展示');
    return;
  }
  persistedVisibleColumnKeys.value = [...draftVisibleColumnKeys.value];
  saveStatisticBoardViewPrefs(props.boardKey, {
    visibleColumnKeys: persistedVisibleColumnKeys.value,
  });
  settingsVisible.value = false;
  ElMessage.success('视图配置已保存');
}

function restoreDefaultView() {
  if (!board.value) {
    return;
  }
  const defaultKeys = defaultVisibleColumnKeys(board.value.definition);
  draftVisibleColumnKeys.value = [...defaultKeys];
  persistedVisibleColumnKeys.value = [...defaultKeys];
  resetStatisticBoardViewPrefs(props.boardKey);
  settingsVisible.value = false;
  ElMessage.success('已恢复默认视图');
}

function detailCellValue(record: Record<string, unknown>, column: StatisticDetailColumn) {
  const value = record[column.key];
  if (value == null || value === '') {
    return '-';
  }
  if (typeof value === 'object') {
    return JSON.stringify(value);
  }
  return String(value);
}

async function loadDetail() {
  if (!activeRow.value || !activeCell.value || !board.value) {
    return;
  }
  detailLoading.value = true;
  try {
    detail.value = await api.getStatisticBoardDetails(props.boardKey, {
      rowKey: activeRow.value.rowKey,
      columnKey: activeCell.value.columnKey,
      page: detailPagination.page,
      size: detailPagination.size,
      sortField: detailPagination.sortField || undefined,
      sortOrder: detailPagination.sortOrder || undefined,
      filters: buildFilterPayload(),
    });
  } catch (error) {
    ElMessage.error((error as Error).message);
  } finally {
    detailLoading.value = false;
  }
}

async function openDetail(row: StatisticRowData, cell: StatisticCellData) {
  if (!cell.drilldown) {
    return;
  }
  activeRow.value = row;
  activeCell.value = cell;
  detailVisible.value = true;
  detailPagination.page = 1;
  detailPagination.size = board.value?.definition.defaultPageSize ?? 10;
  detailPagination.sortField = 'syncedAt';
  detailPagination.sortOrder = 'descending';
  await loadDetail();
}

function handleDetailSortChange({
  prop,
  order,
}: {
  column: unknown;
  prop: string;
  order: 'ascending' | 'descending' | null;
}) {
  detailPagination.sortField = prop || '';
  detailPagination.sortOrder = order ?? 'descending';
  detailPagination.page = 1;
  void loadDetail();
}

function cellForColumn(row: StatisticRowData, columnKey: string) {
  return row.cells.find((item) => item.columnKey === columnKey);
}

watch(detailVisible, (visible) => {
  if (!visible) {
    detail.value = null;
    activeRow.value = null;
    activeCell.value = null;
  }
});

onMounted(async () => {
  await loadBoard();
});
</script>

<template>
  <div class="stat-board">
    <el-card shadow="never" class="stat-board-card" v-loading="loading">
      <template #header>
        <div class="stat-board-toolbar">
          <div class="stat-board-toolbar-main">
            <div class="stat-board-heading">
              <div class="stat-board-title">{{ board?.definition.title || '统计表' }}</div>
            </div>

            <el-form class="stat-toolbar-form" inline>
              <el-form-item
                v-for="field in activeFilterFields"
                :key="field.key"
                :label="field.label"
                class="stat-toolbar-item"
              >
                <el-select
                  v-if="field.type === 'select'"
                  v-model="filters[field.key]"
                  clearable
                  filterable
                  :placeholder="field.placeholder || `请选择${field.label}`"
                  :style="{ width: `${field.width || 220}px` }"
                >
                  <el-option
                    v-for="option in field.options"
                    :key="option.value"
                    :label="option.label"
                    :value="option.value"
                  />
                </el-select>
                <el-input
                  v-else
                  v-model="filters[field.key]"
                  :placeholder="field.placeholder || ''"
                  clearable
                  :style="{ width: `${field.width || 220}px` }"
                />
              </el-form-item>
            </el-form>
          </div>

          <div class="stat-board-toolbar-actions">
            <el-button type="primary" :icon="Search" @click="loadBoard()">查询</el-button>
            <el-button @click="resetFilters">重置</el-button>
            <el-button :icon="RefreshRight" @click="loadBoard()">刷新</el-button>
            <el-button plain :icon="Download" @click="exportBoard">导出</el-button>
            <el-dropdown trigger="click" @command="handleSettingsCommand">
              <el-button class="view-settings-trigger">
                <span class="hamburger-icon" aria-hidden="true">
                  <span></span>
                  <span></span>
                  <span></span>
                </span>
                <span>设置</span>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="open-settings">列显示设置</el-dropdown-item>
                  <el-dropdown-item command="restore-default-view">恢复默认视图</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </template>

      <el-alert
        v-if="errorMessage"
        :title="errorMessage"
        type="error"
        :closable="false"
        show-icon
        class="stat-board-alert"
      />

      <div v-if="board && board.rows.length" class="stat-matrix-wrapper">
        <el-table :data="board.rows" border stripe class="stat-matrix-table">
          <el-table-column prop="rowLabel" label="统计对象" fixed="left" min-width="180" />
          <el-table-column
            v-for="group in visibleColumnGroups"
            :key="group.key"
            :label="group.label"
            align="center"
          >
            <el-table-column
              v-for="column in group.columns"
              :key="column.key"
              :label="column.label"
              min-width="132"
              align="center"
            >
              <template #default="{ row }">
                <button
                  v-if="cellForColumn(row, column.key)?.drilldown"
                  class="stat-cell drilldown"
                  @click="openDetail(row, cellForColumn(row, column.key)!)"
                >
                  {{ cellForColumn(row, column.key)?.displayValue || '-' }}
                </button>
                <span v-else class="stat-cell">
                  {{ cellForColumn(row, column.key)?.displayValue || '-' }}
                </span>
              </template>
            </el-table-column>
          </el-table-column>
        </el-table>
      </div>

      <el-empty
        v-else
        :description="board?.definition.emptyText || '当前筛选条件下没有可展示的统计结果。'"
        class="stat-empty"
      />
    </el-card>

    <el-drawer
      v-model="detailVisible"
      size="56%"
      :title="detail?.title || '明细数据'"
      class="stat-detail-drawer"
      destroy-on-close
    >
      <div class="stat-detail-shell" v-loading="detailLoading">
        <el-table
          v-if="detail"
          :data="detail.records"
          border
          stripe
          class="stat-detail-table"
          @sort-change="handleDetailSortChange"
        >
          <el-table-column
            v-for="column in detail.columns"
            :key="column.key"
            :prop="column.key"
            :label="column.label"
            :width="column.width || undefined"
            :min-width="column.minWidth || 140"
            :sortable="column.sortable ? 'custom' : false"
            show-overflow-tooltip
          >
            <template #default="{ row }">
              <span class="detail-cell-text">{{ detailCellValue(row, column) }}</span>
            </template>
          </el-table-column>
        </el-table>

        <div class="detail-pagination">
          <el-pagination
            v-if="detail"
            v-model:current-page="detailPagination.page"
            v-model:page-size="detailPagination.size"
            background
            layout="total, sizes, prev, pager, next"
            :page-sizes="[10, 20, 50, 100]"
            :total="detail.total"
            @current-change="loadDetail"
            @size-change="
              () => {
                detailPagination.page = 1;
                loadDetail();
              }
            "
          />
        </div>
      </div>
    </el-drawer>

    <el-drawer v-model="settingsVisible" title="表格视图设置" size="360px" append-to-body>
      <div class="view-settings-panel" v-if="board">
        <div class="view-settings-summary">
          <div class="view-settings-summary-title">列显示控制</div>
          <div class="view-settings-summary-text">当前已选择 {{ currentVisibleColumnCount }} 列，可按需调整当前页面的展示视图。</div>
        </div>

        <el-checkbox-group v-model="draftVisibleColumnKeys" class="view-settings-checklist">
          <div v-for="group in board.definition.columnGroups" :key="group.key" class="view-settings-group">
            <div class="view-settings-group-title">{{ group.label }}</div>
            <div class="view-settings-group-body">
              <el-checkbox
                v-for="column in group.columns"
                :key="column.key"
                :value="column.key"
                class="view-settings-check"
              >
                {{ column.label }}
              </el-checkbox>
            </div>
          </div>
        </el-checkbox-group>

        <div class="view-settings-actions">
          <el-button @click="restoreDefaultView">恢复默认</el-button>
          <el-button @click="settingsVisible = false">取消</el-button>
          <el-button type="primary" :icon="ArrowRight" @click="saveViewPrefs">保存视图</el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>
