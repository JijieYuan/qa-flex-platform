<script setup lang="ts">
import { computed, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { Document, RefreshRight, View } from '@element-plus/icons-vue';
import BaseRecordTable from '../components/base/BaseRecordTable.vue';
import {
  api,
  type ReviewDataFilterOptionsResponse,
  type ReviewDataRecordRowResponse,
  type ReviewDataSummaryResponse,
} from '../api';
import { useRouteTableState } from '../composables/useRouteTableState';
import type { RecordTableFilterField } from '../types/record-table';
import {
  buildReviewDataFilterTags,
  buildReviewDataSummaryCards,
  buildReviewDataTableRows,
  reviewDataColumns,
} from './review-data-management';

const {
  route,
  page,
  pageSize,
  sortBy,
  sortOrder,
  patchQuery,
  bindLoader,
  isTableLoading,
} = useRouteTableState({
  defaults: {
    page: 1,
    pageSize: 20,
    sortBy: 'updatedAt',
    sortOrder: 'desc',
  },
});

const advancedVisible = ref(false);
const rows = ref<ReviewDataRecordRowResponse[]>([]);
const total = ref(0);
const summary = ref<ReviewDataSummaryResponse | null>(null);
const detailVisible = ref(false);
const selectedRow = ref<ReviewDataRecordRowResponse | null>(null);

const filterOptions = ref<ReviewDataFilterOptionsResponse>({
  projectNames: [],
  repositoryNames: [],
  moduleNames: [],
  reviewers: [],
  templateCodes: [],
  targetBranches: [],
  recordStatuses: [],
});

const filterValues = computed<Record<string, unknown>>(() => {
  const start = String(route.query.updatedAtStart ?? '');
  const end = String(route.query.updatedAtEnd ?? '');
  return {
    projectName: String(route.query.projectName ?? ''),
    repositoryName: String(route.query.repositoryName ?? ''),
    moduleName: String(route.query.moduleName ?? ''),
    reviewer: String(route.query.reviewer ?? ''),
    templateCode: String(route.query.templateCode ?? ''),
    targetBranch: String(route.query.targetBranch ?? ''),
    recordStatus: String(route.query.recordStatus ?? ''),
    keyword: String(route.query.keyword ?? ''),
    mergeRequestIid: String(route.query.mergeRequestIid ?? ''),
    updatedAtRange: start && end ? [start, end] : [],
  };
});

const primaryFilters = computed<RecordTableFilterField[]>(() => [
  {
    key: 'projectName',
    label: '项目',
    type: 'select',
    width: 180,
    options: [{ label: '全部项目', value: '' }, ...filterOptions.value.projectNames],
  },
  {
    key: 'repositoryName',
    label: '代码库',
    type: 'select',
    width: 200,
    options: [{ label: '全部代码库', value: '' }, ...filterOptions.value.repositoryNames],
  },
  {
    key: 'moduleName',
    label: '模块',
    type: 'select',
    width: 180,
    options: [{ label: '全部模块', value: '' }, ...filterOptions.value.moduleNames],
  },
  {
    key: 'keyword',
    label: '关键字',
    type: 'input',
    width: 280,
    placeholder: '搜索标题、备注、评审人或仓库',
  },
]);

const advancedFilters = computed<RecordTableFilterField[]>(() => [
  {
    key: 'reviewer',
    label: '评审人',
    type: 'select',
    options: [{ label: '全部评审人', value: '' }, ...filterOptions.value.reviewers],
  },
  {
    key: 'templateCode',
    label: '模板编码',
    type: 'select',
    options: [{ label: '全部模板', value: '' }, ...filterOptions.value.templateCodes],
  },
  {
    key: 'targetBranch',
    label: '目标分支',
    type: 'select',
    options: [{ label: '全部目标分支', value: '' }, ...filterOptions.value.targetBranches],
  },
  {
    key: 'recordStatus',
    label: '记录状态',
    type: 'select',
    options: [{ label: '全部状态', value: '' }, ...filterOptions.value.recordStatuses],
  },
  {
    key: 'mergeRequestIid',
    label: '合并请求编号',
    type: 'input',
    placeholder: '输入合并请求编号',
  },
  {
    key: 'updatedAtRange',
    label: '更新时间',
    type: 'daterange',
    width: 280,
    startPlaceholder: '开始日期',
    endPlaceholder: '结束日期',
  },
]);

const columns = reviewDataColumns();
const activeFilterTags = computed(() => buildReviewDataFilterTags(filterValues.value));
const summaryCards = computed(() => buildReviewDataSummaryCards(summary.value));
const tableRows = computed(() => buildReviewDataTableRows(rows.value));

async function loadFilterOptions() {
  filterOptions.value = await api.getReviewDataFilterOptions(route.query.projectId as string | undefined);
}

async function loadRows() {
  const response = await api.getReviewDataRecords({
    projectId: route.query.projectId as string | undefined,
    projectName: String(route.query.projectName ?? ''),
    repositoryName: String(route.query.repositoryName ?? ''),
    moduleName: String(route.query.moduleName ?? ''),
    reviewer: String(route.query.reviewer ?? ''),
    templateCode: String(route.query.templateCode ?? ''),
    targetBranch: String(route.query.targetBranch ?? ''),
    recordStatus: String(route.query.recordStatus ?? ''),
    keyword: String(route.query.keyword ?? ''),
    mergeRequestIid: String(route.query.mergeRequestIid ?? ''),
    updatedAtStart: String(route.query.updatedAtStart ?? ''),
    updatedAtEnd: String(route.query.updatedAtEnd ?? ''),
    page: page.value,
    size: pageSize.value,
    sortBy: sortBy.value,
    sortOrder: sortOrder.value || 'desc',
  });
  rows.value = response.records;
  total.value = response.total;
  summary.value = response.summary;
}

bindLoader(async () => {
  try {
    await Promise.all([loadFilterOptions(), loadRows()]);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '评审数据加载失败');
  }
});

async function handleFilterChange(payload: { key: string; value: string | string[] | null }) {
  if (payload.key === 'updatedAtRange') {
    const nextValue = Array.isArray(payload.value) ? payload.value : [];
    await patchQuery({
      updatedAtStart: nextValue[0] ?? '',
      updatedAtEnd: nextValue[1] ?? '',
      page: 1,
    });
    return;
  }

  await patchQuery({
    [payload.key]: Array.isArray(payload.value) ? payload.value.join(',') : payload.value ?? '',
    page: 1,
  });
}

async function handleReset() {
  await patchQuery({
    projectName: '',
    repositoryName: '',
    moduleName: '',
    reviewer: '',
    templateCode: '',
    targetBranch: '',
    recordStatus: '',
    keyword: '',
    mergeRequestIid: '',
    updatedAtStart: '',
    updatedAtEnd: '',
    sortBy: 'updatedAt',
    sortOrder: 'desc',
    page: 1,
  });
}

async function handleQuery() {
  await patchQuery({ page: 1 });
}

async function handleRefresh() {
  await loadFilterOptions();
  await loadRows();
}

async function handleSortChange(payload: { prop: string; order: 'ascending' | 'descending' | null }) {
  await patchQuery({
    sortBy: payload.prop || 'updatedAt',
    sortOrder: payload.order === 'ascending' ? 'asc' : payload.order === 'descending' ? 'desc' : 'desc',
    page: 1,
  });
}

async function handlePageChange(nextPage: number) {
  await patchQuery({ page: nextPage });
}

async function handleSizeChange(nextSize: number) {
  await patchQuery({ pageSize: nextSize, page: 1 });
}

async function clearFilter(key: string) {
  if (key === 'updatedAtRange') {
    await patchQuery({ updatedAtStart: '', updatedAtEnd: '', page: 1 });
    return;
  }
  await patchQuery({ [key]: '', page: 1 });
}

function openDetail(row: Record<string, unknown>) {
  selectedRow.value = (row.__raw as ReviewDataRecordRowResponse) ?? null;
  detailVisible.value = true;
}

function displayText(value: unknown) {
  const text = String(value ?? '').trim();
  return text || '-';
}

function formatDateTime(value?: string | null) {
  return value ? value.replace('T', ' ').slice(0, 19) : '-';
}

function formatPercent(value?: number | null) {
  return value == null ? '-' : `${value.toFixed(2)}%`;
}
</script>

<template>
  <section class="review-data-page">
    <header class="review-data-hero">
      <div>
        <p class="review-data-eyebrow">评审数据管理</p>
        <h1 class="review-data-title">评审记录总览</h1>
        <p class="review-data-desc">围绕正式评审记录查看标题、评审人、评分、外部指标与更新时间，适合做记录筛选、追溯和轻量复核。</p>
      </div>
      <el-button type="primary" plain :icon="RefreshRight" @click="handleRefresh">刷新数据</el-button>
    </header>

    <section class="review-data-summary">
      <article v-for="card in summaryCards" :key="card.key" class="summary-card">
        <span class="summary-card-label">{{ card.label }}</span>
        <strong class="summary-card-value">{{ card.value }}</strong>
      </article>
    </section>

    <BaseRecordTable
      :columns="columns"
      :rows="tableRows"
      :loading="isTableLoading"
      :page="page"
      :page-size="pageSize"
      :total="total"
      :primary-filters="primaryFilters"
      :advanced-filters="advancedFilters"
      :filter-values="filterValues"
      :active-filter-tags="activeFilterTags"
      :advanced-visible="advancedVisible"
      search-placeholder="搜索评审标题、备注、评审人或仓库"
      empty-description="当前筛选条件下没有可展示的评审记录。"
      query-button-text="查询"
      @filter-change="handleFilterChange"
      @reset="handleReset"
      @query="handleQuery"
      @refresh="handleRefresh"
      @clear-filter="clearFilter"
      @update:advanced-visible="advancedVisible = $event"
      @sort-change="handleSortChange"
      @current-change="handlePageChange"
      @size-change="handleSizeChange"
    >
      <template #cell-formTitle="{ row }">
        <div class="title-cell">
          <span class="title-main">{{ row.formTitle }}</span>
          <span class="title-sub">MR #{{ (row.__raw as ReviewDataRecordRowResponse).mergeRequestIid ?? '-' }}</span>
        </div>
      </template>

      <template #row-actions="{ row }">
        <el-button type="primary" link :icon="View" @click="openDetail(row)">查看</el-button>
      </template>
    </BaseRecordTable>

    <el-drawer
      v-model="detailVisible"
      title="评审记录详情"
      size="520px"
      append-to-body
      class="review-data-drawer"
    >
      <template v-if="selectedRow">
        <section class="detail-section">
          <header class="detail-section-head">
            <el-icon><Document /></el-icon>
            <span>基础信息</span>
          </header>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="评审标题">{{ displayText(selectedRow.formTitle) }}</el-descriptions-item>
            <el-descriptions-item label="项目">{{ displayText(selectedRow.projectName) }}</el-descriptions-item>
            <el-descriptions-item label="代码库">{{ displayText(selectedRow.repositoryName) }}</el-descriptions-item>
            <el-descriptions-item label="合并请求编号">MR #{{ selectedRow.mergeRequestIid ?? '-' }}</el-descriptions-item>
            <el-descriptions-item label="模块">{{ displayText(selectedRow.moduleName) }}</el-descriptions-item>
            <el-descriptions-item label="评审人">{{ displayText(selectedRow.reviewer) }}</el-descriptions-item>
            <el-descriptions-item label="目标分支">{{ displayText(selectedRow.targetBranch) }}</el-descriptions-item>
            <el-descriptions-item label="记录状态">{{ selectedRow.deleted ? '已作废' : '有效' }}</el-descriptions-item>
          </el-descriptions>
        </section>

        <section class="detail-section">
          <header class="detail-section-head">
            <span>评分信息</span>
          </header>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="总分">{{ selectedRow.totalScore }}</el-descriptions-item>
            <el-descriptions-item label="评审时长">{{ selectedRow.reviewDurationMinutes ?? 0 }} 分钟</el-descriptions-item>
            <el-descriptions-item label="规范">{{ selectedRow.specificationScore ?? 0 }}</el-descriptions-item>
            <el-descriptions-item label="逻辑">{{ selectedRow.logicScore ?? 0 }}</el-descriptions-item>
            <el-descriptions-item label="性能">{{ selectedRow.performanceScore ?? 0 }}</el-descriptions-item>
            <el-descriptions-item label="设计">{{ selectedRow.designScore ?? 0 }}</el-descriptions-item>
            <el-descriptions-item label="其他">{{ selectedRow.otherScore ?? 0 }}</el-descriptions-item>
            <el-descriptions-item label="模板编码">{{ displayText(selectedRow.templateCode) }}</el-descriptions-item>
          </el-descriptions>
        </section>

        <section class="detail-section">
          <header class="detail-section-head">
            <span>外部指标</span>
          </header>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="注释率">{{ formatPercent(selectedRow.commentRate) }}</el-descriptions-item>
            <el-descriptions-item label="缺陷数">{{ selectedRow.defectCount ?? 0 }}</el-descriptions-item>
            <el-descriptions-item label="新增代码行数">{{ selectedRow.addedLines ?? 0 }}</el-descriptions-item>
            <el-descriptions-item label="更新时间">{{ formatDateTime(selectedRow.updatedAt) }}</el-descriptions-item>
          </el-descriptions>
        </section>

        <section class="detail-section">
          <header class="detail-section-head">
            <span>备注</span>
          </header>
          <div class="remark-panel">{{ displayText(selectedRow.remark) }}</div>
        </section>
      </template>
    </el-drawer>
  </section>
</template>

<style scoped>
.review-data-page {
  display: grid;
  gap: 14px;
}

.review-data-hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 4px 2px;
}

.review-data-eyebrow {
  margin: 0 0 6px;
  font-size: 12px;
  font-weight: 600;
  color: rgba(37, 99, 235, 0.82);
}

.review-data-title {
  margin: 0;
  font-size: 26px;
  line-height: 1.2;
  color: #0f172a;
}

.review-data-desc {
  margin: 8px 0 0;
  max-width: 760px;
  font-size: 14px;
  line-height: 1.7;
  color: rgba(15, 23, 42, 0.64);
}

.review-data-summary {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 12px;
}

.summary-card {
  display: grid;
  gap: 8px;
  min-height: 88px;
  padding: 16px 18px;
  border: 1px solid rgba(15, 23, 42, 0.06);
  border-radius: 16px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(248, 250, 252, 0.94));
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.03);
}

.summary-card-label {
  font-size: 12px;
  font-weight: 600;
  color: rgba(15, 23, 42, 0.52);
}

.summary-card-value {
  font-size: 24px;
  font-weight: 700;
  line-height: 1.1;
  color: #0f172a;
}

.title-cell {
  display: grid;
  gap: 4px;
}

.title-main {
  color: rgba(15, 23, 42, 0.88);
  font-weight: 600;
}

.title-sub {
  font-size: 12px;
  color: rgba(15, 23, 42, 0.48);
}

.detail-section {
  display: grid;
  gap: 10px;
  margin-bottom: 18px;
}

.detail-section-head {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 700;
  color: rgba(15, 23, 42, 0.75);
}

.remark-panel {
  min-height: 88px;
  padding: 14px 16px;
  border: 1px solid rgba(15, 23, 42, 0.06);
  border-radius: 12px;
  background: rgba(248, 250, 252, 0.88);
  color: rgba(15, 23, 42, 0.78);
  line-height: 1.7;
  white-space: pre-wrap;
}

:deep(.review-data-drawer .el-drawer__header) {
  margin-bottom: 10px;
}

:deep(.review-data-drawer .el-descriptions__label) {
  width: 108px;
}
</style>
