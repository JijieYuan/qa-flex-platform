<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { Refresh, Search, WarningFilled } from '@element-plus/icons-vue';
import { api, type DatabaseTableOption, type DatabaseTableRowsResponse } from '../api';
import { useRouteTableState } from '../composables/useRouteTableState';

const tablesLoading = ref(false);
const tableOptions = ref<DatabaseTableOption[]>([]);
const keywordDraft = ref('');
const rowsResponse = ref<DatabaseTableRowsResponse | null>(null);
const viewportHeight = ref(typeof window !== 'undefined' ? window.innerHeight : 1080);

const {
  route,
  page,
  pageSize,
  sortBy,
  sortOrder,
  keyword,
  isTableLoading,
  patchQuery,
  debouncedPatchQuery,
  cancelDebouncedQuery,
  bindLoader,
} = useRouteTableState({
  defaults: {
    page: 1,
    pageSize: 20,
    sortBy: '',
    sortOrder: '',
    keyword: '',
  },
  debounceMs: 300,
});

const selectedTable = computed(() => String(route.query.table ?? ''));

const columns = computed(() => rowsResponse.value?.columns ?? []);
const rows = computed(() => rowsResponse.value?.rows ?? []);
const total = computed(() => rowsResponse.value?.total ?? 0);
const selectedOption = computed(() => tableOptions.value.find((option) => option.tableName === selectedTable.value) ?? null);
const syncStatusTagType = computed(() => {
  const status = rowsResponse.value?.syncStatus ?? selectedOption.value?.syncStatus;
  switch (status) {
    case 'SYNCING':
      return 'warning';
    case 'ERROR':
      return 'danger';
    default:
      return 'info';
  }
});
const tableHeight = computed(() => Math.max(460, viewportHeight.value - 272));

async function loadTables() {
  tablesLoading.value = true;
  try {
    const tables = await api.getDatabaseTables();
    tableOptions.value = tables;
    if (!selectedTable.value && tables.length > 0) {
      await patchQuery({ table: tables[0].tableName }, 'replace');
    }
  } catch (error) {
    ElMessage.error((error as Error).message);
  } finally {
    tablesLoading.value = false;
  }
}

async function loadRows(showError = true) {
  if (!selectedTable.value) {
    rowsResponse.value = null;
    return;
  }
  try {
    rowsResponse.value = await api.getDatabaseTableRows({
      tableName: selectedTable.value,
      page: page.value,
      size: pageSize.value,
      keyword: keyword.value || undefined,
      sortField: sortBy.value || undefined,
      sortOrder: sortOrder.value || undefined,
    });
  } catch (error) {
    if (showError) {
      ElMessage.error((error as Error).message);
    }
  }
}

async function handleSearch() {
  cancelDebouncedQuery();
  await patchQuery({
    keyword: keywordDraft.value.trim(),
    page: 1,
  });
}

async function handleReset() {
  cancelDebouncedQuery();
  keywordDraft.value = '';
  await patchQuery({
    keyword: '',
    page: 1,
    sortBy: '',
    sortOrder: '',
  });
}

async function handleRefresh() {
  await loadTables();
  await loadRows();
}

async function handleSizeChange(nextSize: number) {
  await patchQuery({
    pageSize: nextSize,
    page: 1,
  });
}

async function handleCurrentChange(nextPage: number) {
  await patchQuery({
    page: nextPage,
  });
}

async function handleSortChange(payload: { prop: string; order: 'ascending' | 'descending' | null }) {
  await patchQuery({
    sortBy: payload.prop || '',
    sortOrder: payload.order === 'ascending' ? 'asc' : payload.order === 'descending' ? 'desc' : '',
    page: 1,
  });
}

function formatCellValue(value: unknown) {
  if (value == null || value === '') {
    return '-';
  }
  if (typeof value === 'object') {
    return JSON.stringify(value);
  }
  return String(value);
}

function formatTime(value?: string | null) {
  return value || '-';
}

function syncStatusText() {
  return rowsResponse.value?.syncStatus || selectedOption.value?.syncStatus || 'IDLE';
}

function handleResize() {
  viewportHeight.value = window.innerHeight;
}

watch(keyword, (nextKeyword) => {
  keywordDraft.value = nextKeyword;
}, { immediate: true });

watch(keywordDraft, (nextKeyword, previousKeyword) => {
  if (nextKeyword === previousKeyword || nextKeyword === keyword.value) {
    return;
  }
  debouncedPatchQuery({
    keyword: nextKeyword.trim(),
    page: 1,
  });
});

bindLoader(async () => {
  await loadRows(false);
});

onMounted(async () => {
  window.addEventListener('resize', handleResize);
  await loadTables();
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize);
});
</script>

<template>
  <div class="db-browser-card">
    <div class="db-toolbar">
      <div class="db-toolbar-filters">
        <el-select
          :model-value="selectedTable"
          class="db-table-select"
          placeholder="请选择要查看的本地表"
          filterable
          :loading="tablesLoading"
          @change="(value:string) => patchQuery({ table: value, page: 1, sortBy: '', sortOrder: '' })"
        >
          <el-option
            v-for="option in tableOptions"
            :key="option.tableName"
            :label="`${option.label} (${option.tableName})`"
            :value="option.tableName"
          />
        </el-select>

        <el-input
          v-model="keywordDraft"
          class="db-search-input"
          placeholder="输入关键字搜索当前表"
          clearable
          @keyup.enter="handleSearch"
          @clear="handleReset"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </div>

      <div class="db-toolbar-actions">
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="handleReset">重置</el-button>
        <el-button :icon="Refresh" @click="handleRefresh">刷新</el-button>
      </div>
    </div>

    <el-card shadow="never" class="db-table-card">
      <template #header>
        <div class="db-table-header">
          <div class="db-table-context">
            <el-tag v-if="rowsResponse?.label || selectedOption?.label" effect="plain" round>
              {{ rowsResponse?.label || selectedOption?.label }}
            </el-tag>
            <span class="db-table-name">{{ rowsResponse?.tableName || selectedTable || '-' }}</span>
          </div>

          <div class="db-table-meta">
            <el-tag :type="syncStatusTagType" round>{{ syncStatusText() }}</el-tag>
            <span class="db-table-meta-text">Last Sync: {{ formatTime(rowsResponse?.lastSyncTime || selectedOption?.lastSyncTime) }}</span>
            <span class="db-table-meta-text">Total: {{ total }}</span>
          </div>
        </div>
      </template>

      <el-empty v-if="!selectedTable" description="当前没有可查看的本地表" />

      <template v-else>
        <el-alert
          v-if="rowsResponse?.statusMessage"
          class="db-status-alert"
          :icon="WarningFilled"
          type="warning"
          :closable="false"
          :title="rowsResponse.statusMessage"
        />

        <el-table
          v-loading="isTableLoading"
          :data="rows"
          border
          stripe
          class="db-table"
          :height="tableHeight"
          @sort-change="handleSortChange"
        >
          <el-table-column
            v-for="column in columns"
            :key="column.key"
            :prop="column.key"
            :label="column.label"
            :sortable="column.sortable ? 'custom' : false"
            min-width="150"
            show-overflow-tooltip
          >
            <template #default="{ row }">
              <span class="db-cell-text">{{ formatCellValue(row[column.key]) }}</span>
            </template>
          </el-table-column>
        </el-table>

        <div class="db-pagination">
          <el-pagination
            background
            layout="total, sizes, prev, pager, next"
            :current-page="page"
            :page-size="pageSize"
            :page-sizes="[10, 20, 50, 100]"
            :total="total"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>
      </template>
    </el-card>
  </div>
</template>

<style scoped>
.db-browser-card {
  display: grid;
  gap: 12px;
}

.db-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.db-toolbar-filters {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1 1 720px;
  flex-wrap: wrap;
}

.db-table-select {
  width: 360px;
}

.db-search-input {
  width: 320px;
}

.db-toolbar-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.db-table-card {
  border-radius: 12px;
}

.db-table-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.db-table-context {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.db-table-name {
  color: rgba(0, 0, 0, 0.45);
  font-size: 12px;
}

.db-table-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.db-table-meta-text {
  color: rgba(0, 0, 0, 0.45);
  font-size: 12px;
  line-height: 1.4;
}

.db-status-alert {
  margin-bottom: 12px;
}

.db-table {
  width: 100%;
}

.db-cell-text {
  display: inline-block;
  max-width: 100%;
}

.db-pagination {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}
</style>
