<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { Refresh, Search } from '@element-plus/icons-vue';
import { api, type DatabaseTableOption, type DatabaseTableRowsResponse } from '../api';

const loading = ref(false);
const tablesLoading = ref(false);
const tableOptions = ref<DatabaseTableOption[]>([]);
const selectedTable = ref('');
const keyword = ref('');
const keywordDraft = ref('');
const page = ref(1);
const size = ref(20);
const sortField = ref('');
const sortOrder = ref<'asc' | 'desc' | ''>('');
const rowsResponse = ref<DatabaseTableRowsResponse | null>(null);

const columns = computed(() => rowsResponse.value?.columns ?? []);
const rows = computed(() => rowsResponse.value?.rows ?? []);
const total = computed(() => rowsResponse.value?.total ?? 0);

async function loadTables() {
  tablesLoading.value = true;
  try {
    const tables = await api.getDatabaseTables();
    tableOptions.value = tables;
    if (!selectedTable.value && tables.length > 0) {
      selectedTable.value = tables[0].tableName;
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
  loading.value = true;
  try {
    rowsResponse.value = await api.getDatabaseTableRows({
      tableName: selectedTable.value,
      page: page.value,
      size: size.value,
      keyword: keyword.value || undefined,
      sortField: sortField.value || undefined,
      sortOrder: sortOrder.value || undefined,
    });
  } catch (error) {
    if (showError) {
      ElMessage.error((error as Error).message);
    }
  } finally {
    loading.value = false;
  }
}

async function handleTableChange() {
  page.value = 1;
  sortField.value = '';
  sortOrder.value = '';
  await loadRows();
}

async function handleSearch() {
  keyword.value = keywordDraft.value.trim();
  page.value = 1;
  await loadRows();
}

async function handleReset() {
  keywordDraft.value = '';
  keyword.value = '';
  page.value = 1;
  sortField.value = '';
  sortOrder.value = '';
  await loadRows();
}

async function handleRefresh() {
  await loadRows();
}

async function handleSizeChange(nextSize: number) {
  size.value = nextSize;
  page.value = 1;
  await loadRows();
}

async function handleCurrentChange(nextPage: number) {
  page.value = nextPage;
  await loadRows();
}

async function handleSortChange(payload: { prop: string; order: 'ascending' | 'descending' | null }) {
  sortField.value = payload.prop || '';
  sortOrder.value = payload.order === 'ascending' ? 'asc' : payload.order === 'descending' ? 'desc' : '';
  page.value = 1;
  await loadRows();
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

onMounted(async () => {
  await loadTables();
  await loadRows(false);
});
</script>

<template>
  <div class="db-browser-card">
    <div class="db-toolbar">
      <div class="db-toolbar-filters">
        <el-select
          v-model="selectedTable"
          class="db-table-select"
          placeholder="请选择要查看的本地表"
          filterable
          :loading="tablesLoading"
          @change="handleTableChange"
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
          <div>
            <div class="db-table-title">{{ rowsResponse?.label || '数据库查看' }}</div>
            <div class="db-table-caption">{{ rowsResponse?.tableName || '请选择一个本地表开始查看' }}</div>
          </div>
          <el-tag type="info" round>共 {{ total }} 条</el-tag>
        </div>
      </template>

      <el-empty v-if="!selectedTable" description="当前没有可查看的本地表" />

      <template v-else>
        <el-table
          v-loading="loading"
          :data="rows"
          border
          stripe
          class="db-table"
          height="620"
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
            :page-size="size"
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
  gap: 16px;
}

.db-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
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
  gap: 10px;
}

.db-table-card {
  border-radius: 24px;
}

.db-table-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.db-table-title {
  font-size: 20px;
  font-weight: 700;
  color: #122131;
}

.db-table-caption {
  margin-top: 4px;
  font-size: 13px;
  color: #6b7d91;
}

.db-table {
  width: 100%;
}

.db-cell-text {
  display: inline-block;
  max-width: 100%;
}

.db-pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
