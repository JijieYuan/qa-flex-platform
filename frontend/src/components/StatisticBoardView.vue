<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { Download, RefreshRight, Search } from '@element-plus/icons-vue';
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

const props = defineProps<{
  boardKey: string;
}>();

const loading = ref(false);
const detailLoading = ref(false);
const board = ref<StatisticBoardResponse | null>(null);
const filters = reactive<Record<string, string>>({});
const detailVisible = ref(false);
const activeRow = ref<StatisticRowData | null>(null);
const activeCell = ref<StatisticCellData | null>(null);
const detail = ref<StatisticDetailResponse | null>(null);

const detailPagination = reactive({
  page: 1,
  size: 10,
  sortField: '',
  sortOrder: 'descending',
});

const flatColumns = computed(() => board.value?.definition.columnGroups.flatMap((group) => group.columns) ?? []);

const activeFilterFields = computed(() => board.value?.definition.filters ?? []);

function initializeFilters(fields: StatisticFilterField[], appliedFilters?: Record<string, string>) {
  for (const field of fields) {
    const value = appliedFilters?.[field.key] ?? field.defaultValue ?? '';
    filters[field.key] = value;
  }
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
  try {
    const response = await api.getStatisticBoard(props.boardKey, buildFilterPayload());
    board.value = response;
    initializeFilters(response.definition.filters, response.appliedFilters);
    if (detailPagination.size <= 0) {
      detailPagination.size = response.definition.defaultPageSize ?? 10;
    }
  } catch (error) {
    if (showError) {
      ElMessage.error((error as Error).message);
    }
  } finally {
    loading.value = false;
  }
}

function resetFilters() {
  if (!board.value) {
    return;
  }
  initializeFilters(board.value.definition.filters);
  void loadBoard();
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
        <div class="stat-board-card-header">
          <div>
            <div class="stat-board-title">{{ board?.definition.title || '统计表' }}</div>
            <div class="stat-board-subtitle">{{ board?.definition.description || '正在加载统计定义。' }}</div>
          </div>
          <div class="stat-board-header-actions">
            <el-button :icon="RefreshRight" @click="loadBoard()">刷新</el-button>
            <el-button type="primary" plain :icon="Download" disabled>导出预留</el-button>
          </div>
        </div>
      </template>

      <section class="stat-query-section">
        <div class="section-title">{{ board?.definition.queryTitle || '查询与操作' }}</div>
        <p class="section-description">
          {{ board?.definition.queryDescription || '根据筛选条件生成当前统计矩阵。' }}
        </p>

        <el-form class="stat-query-form" inline>
          <el-form-item
            v-for="field in activeFilterFields"
            :key="field.key"
            :label="field.label"
            :style="{ width: `${field.width || 220}px` }"
          >
            <el-input
              v-if="field.type === 'text'"
              v-model="filters[field.key]"
              :placeholder="field.placeholder || ''"
              clearable
            />
            <el-select v-else v-model="filters[field.key]">
              <el-option v-for="option in field.options" :key="option.value" :label="option.label" :value="option.value" />
            </el-select>
          </el-form-item>

          <div class="stat-query-actions">
            <el-button type="primary" :icon="Search" @click="loadBoard()">查询</el-button>
            <el-button @click="resetFilters">重置</el-button>
          </div>
        </el-form>
      </section>

      <section class="stat-matrix-section">
        <div class="section-title">汇总统计</div>
        <p class="section-description">单元格中的统计值可按配置决定是否支持下钻查看对应原始记录。</p>

        <div v-if="board && board.rows.length" class="stat-matrix-wrapper">
          <el-table :data="board.rows" border stripe class="stat-matrix-table">
            <el-table-column prop="rowLabel" label="统计对象" fixed="left" min-width="180" />
            <el-table-column
              v-for="group in board.definition.columnGroups"
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
                    v-if="row.cells.find((item: StatisticCellData) => item.columnKey === column.key)?.drilldown"
                    class="stat-cell drilldown"
                    @click="openDetail(row, row.cells.find((item: StatisticCellData) => item.columnKey === column.key))"
                  >
                    {{ row.cells.find((item: StatisticCellData) => item.columnKey === column.key)?.displayValue || '-' }}
                  </button>
                  <span v-else class="stat-cell">
                    {{ row.cells.find((item: StatisticCellData) => item.columnKey === column.key)?.displayValue || '-' }}
                  </span>
                </template>
              </el-table-column>
            </el-table-column>
          </el-table>
        </div>

        <el-empty
          v-else
          :description="board?.definition.emptyText || '当前筛选条件下没有统计数据。'"
          class="stat-empty"
        />
      </section>
    </el-card>

    <el-drawer
      v-model="detailVisible"
      size="56%"
      :title="detail?.title || '明细数据'"
      class="stat-detail-drawer"
      destroy-on-close
    >
      <div class="stat-detail-shell" v-loading="detailLoading">
        <p class="section-description">{{ detail?.description || '根据当前统计单元格生成的明细列表。' }}</p>

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
            @size-change="() => { detailPagination.page = 1; loadDetail(); }"
          />
        </div>
      </div>
    </el-drawer>
  </div>
</template>
