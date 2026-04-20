<script setup lang="ts">
import { computed, ref } from 'vue';
import {
  ElButton,
  ElDescriptions,
  ElDescriptionsItem,
  ElDropdown,
  ElDropdownItem,
  ElDropdownMenu,
  ElIcon,
  ElMessage,
  ElMessageBox,
  ElTag,
} from 'element-plus';
import { ArrowDown, Document, Download, Plus } from '@element-plus/icons-vue';
import BaseRecordTable from '../components/base/BaseRecordTable.vue';
import ReviewProblemItemFormDialog from './review-data/ReviewProblemItemFormDialog.vue';
import ReviewRecordFormDialog from './review-data/ReviewRecordFormDialog.vue';
import {
  api,
  type ReviewDataFilterOptionsResponse,
  type ReviewDataProblemItemResponse,
  type ReviewDataProblemItemSaveRequest,
  type ReviewDataRecordDetailResponse,
  type ReviewDataRecordRowResponse,
  type ReviewDataRecordSaveRequest,
  type ReviewDataSummaryResponse,
} from '../api';
import { useRouteTableState } from '../composables/useRouteTableState';
import type { RecordTableFilterField } from '../types/record-table';
import {
  buildProblemItemTableRows,
  buildReviewDataExportCsv,
  buildReviewDataFilterTags,
  buildReviewDataSummaryCards,
  buildReviewDataTableRows,
  createEmptyProblemItemForm,
  createEmptyReviewRecordForm,
  createProblemItemFormFromRow,
  createReviewRecordFormFromRow,
  reviewDataColumns,
  reviewProblemItemColumns,
  type ReviewProblemItemFormModel,
  type ReviewRecordFormModel,
} from './review-data-management';

const { route, page, pageSize, sortBy, sortOrder, patchQuery, bindLoader, isTableLoading } = useRouteTableState({
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
const filterOptions = ref<ReviewDataFilterOptionsResponse>({
  projectNames: [],
  moduleNames: [],
  reviewOwners: [],
  reviewTypes: [],
  reviewExperts: [],
  problemStatuses: [],
  reviewCategories: [],
  problemCategories: [],
});

const detailVisible = ref(false);
const detailData = ref<ReviewDataRecordDetailResponse | null>(null);

const recordDialogVisible = ref(false);
const recordDialogSaving = ref(false);
const recordEditMode = ref(false);
const editingRecordId = ref<number | null>(null);
const recordForm = ref<ReviewRecordFormModel>(createEmptyReviewRecordForm());
const exportLoading = ref(false);

const problemDialogVisible = ref(false);
const problemDialogSaving = ref(false);
const problemDialogEditMode = ref(false);
const currentProblemRecordId = ref<number | null>(null);
const currentProblemItemId = ref<number | null>(null);
const currentProblemExpertOptions = ref<string[]>([]);
const problemForm = ref<ReviewProblemItemFormModel>(createEmptyProblemItemForm());

const expandedRowKeys = ref<Array<number | string>>([]);
const problemItemsMap = ref<Record<number, ReviewDataProblemItemResponse[]>>({});
const problemLoadingMap = ref<Record<number, boolean>>({});

const columns = reviewDataColumns();
const problemColumns = reviewProblemItemColumns();

const filterValues = computed<Record<string, unknown>>(() => ({
  title: String(route.query.title ?? ''),
  projectName: String(route.query.projectName ?? ''),
  moduleName: String(route.query.moduleName ?? ''),
  reviewOwner: String(route.query.reviewOwner ?? ''),
  reviewType: String(route.query.reviewType ?? ''),
  problemStatus: String(route.query.problemStatus ?? ''),
  reviewExpert: String(route.query.reviewExpert ?? ''),
}));

const primaryFilters = computed<RecordTableFilterField[]>(() => [
  {
    key: 'title',
    label: '标题',
    type: 'input',
    width: 220,
    placeholder: '标题',
  },
  {
    key: 'projectName',
    label: '项目',
    type: 'select',
    width: 180,
    selectMode: 'compact',
    options: [{ label: '全部项目', value: '' }, ...filterOptions.value.projectNames],
  },
  {
    key: 'moduleName',
    label: '模块',
    type: 'select',
    width: 168,
    selectMode: 'compact',
    options: [{ label: '全部模块', value: '' }, ...filterOptions.value.moduleNames],
  },
  {
    key: 'reviewOwner',
    label: '负责人',
    type: 'select',
    width: 168,
    selectMode: 'compact',
    options: [{ label: '全部负责人', value: '' }, ...filterOptions.value.reviewOwners],
  },
]);

const advancedFilters = computed<RecordTableFilterField[]>(() => [
  {
    key: 'reviewType',
    label: '评审类型',
    type: 'select',
    width: 180,
    selectMode: 'compact',
    options: [{ label: '全部评审类型', value: '' }, ...filterOptions.value.reviewTypes],
  },
  {
    key: 'problemStatus',
    label: '问题状态',
    type: 'select',
    width: 168,
    selectMode: 'compact',
    options: [{ label: '全部问题状态', value: '' }, ...filterOptions.value.problemStatuses],
  },
  {
    key: 'reviewExpert',
    label: '评审专家',
    type: 'select',
    width: 168,
    selectMode: 'compact',
    options: [{ label: '全部评审专家', value: '' }, ...filterOptions.value.reviewExperts],
  },
]);

const activeFilterTags = computed(() => buildReviewDataFilterTags(filterValues.value));
const summaryCards = computed(() => buildReviewDataSummaryCards(summary.value));
const tableRows = computed(() => buildReviewDataTableRows(rows.value));

bindLoader(async () => {
  try {
    await Promise.all([loadFilterOptions(), loadRows()]);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '评审数据加载失败');
  }
});

async function loadFilterOptions() {
  filterOptions.value = await api.getReviewDataFilterOptions();
}

async function loadRows() {
  const response = await api.getReviewDataRecords(buildReviewDataRecordQueryParams({
    page: page.value,
    size: pageSize.value,
  }));
  rows.value = response.records;
  total.value = response.total;
  summary.value = response.summary;
}

function buildReviewDataRecordQueryParams(overrides: { page?: number; size?: number } = {}) {
  return {
    title: String(route.query.title ?? ''),
    projectName: String(route.query.projectName ?? ''),
    moduleName: String(route.query.moduleName ?? ''),
    reviewOwner: String(route.query.reviewOwner ?? ''),
    reviewType: String(route.query.reviewType ?? ''),
    problemStatus: String(route.query.problemStatus ?? ''),
    reviewExpert: String(route.query.reviewExpert ?? ''),
    page: overrides.page ?? page.value,
    size: overrides.size ?? pageSize.value,
    sortBy: sortBy.value,
    sortOrder: (sortOrder.value as 'asc' | 'desc' | null) ?? 'desc',
  };
}

async function handleRefresh() {
  try {
    await Promise.all([loadFilterOptions(), loadRows()]);
    ElMessage.success('已刷新评审数据');
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '评审数据刷新失败');
  }
}

async function loadDetail(recordId: number) {
  detailData.value = await api.getReviewDataRecordDetail(recordId);
}

async function loadProblemItems(recordId: number) {
  problemLoadingMap.value[recordId] = true;
  try {
    problemItemsMap.value[recordId] = await api.getReviewDataProblemItems(recordId);
  } finally {
    problemLoadingMap.value[recordId] = false;
  }
}

async function handleFilterChange(payload: { key: string; value: string | string[] | null }) {
  await patchQuery({
    [payload.key]: Array.isArray(payload.value) ? payload.value.join(',') : payload.value ?? '',
    page: 1,
  });
}

async function handleReset() {
  await patchQuery({
    title: '',
    projectName: '',
    moduleName: '',
    reviewOwner: '',
    reviewType: '',
    problemStatus: '',
    reviewExpert: '',
    sortBy: 'updatedAt',
    sortOrder: 'desc',
    page: 1,
  });
}

async function handleQuery() {
  await patchQuery({ page: 1 });
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
  await patchQuery({ [key]: '', page: 1 });
}

async function handleExpandChange(row: Record<string, unknown>, expandedRows: Record<string, unknown>[]) {
  const raw = row.__raw as ReviewDataRecordRowResponse | undefined;
  if (!raw) {
    return;
  }
  const rowIsExpanded = expandedRows.some((item) => Number((item.__raw as ReviewDataRecordRowResponse | undefined)?.id) === raw.id);
  expandedRowKeys.value = rowIsExpanded ? [raw.id] : expandedRowKeys.value.filter((item) => Number(item) !== raw.id);
  if (rowIsExpanded && !problemItemsMap.value[raw.id]) {
    await loadProblemItems(raw.id);
  }
}

async function handleExportExcel() {
  exportLoading.value = true;
  try {
    const exportRows: ReviewDataRecordRowResponse[] = [];
    let nextPage = 1;
    let expectedTotal = Math.max(total.value, rows.value.length);
    do {
      const response = await api.getReviewDataRecords(buildReviewDataRecordQueryParams({ page: nextPage, size: 100 }));
      exportRows.push(...response.records);
      expectedTotal = response.total;
      nextPage += 1;
    } while (exportRows.length < expectedTotal && nextPage < 1000);

    const csv = buildReviewDataExportCsv(exportRows);
    downloadCsv(csv, `评审数据管理_${formatExportFileDate(new Date())}.csv`);
    ElMessage.success(`已导出 ${exportRows.length} 条评审记录`);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '评审数据导出失败');
  } finally {
    exportLoading.value = false;
  }
}

function downloadCsv(csv: string, filename: string) {
  const blob = new Blob([`\uFEFF${csv}`], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = filename;
  document.body.appendChild(anchor);
  anchor.click();
  document.body.removeChild(anchor);
  URL.revokeObjectURL(url);
}

function formatExportFileDate(date: Date) {
  const pad = (value: number) => String(value).padStart(2, '0');
  return [
    date.getFullYear(),
    pad(date.getMonth() + 1),
    pad(date.getDate()),
    pad(date.getHours()),
    pad(date.getMinutes()),
    pad(date.getSeconds()),
  ].join('');
}

function isProblemExpanded(recordId: number) {
  return expandedRowKeys.value.includes(recordId);
}

function recordFromTableRow(row: Record<string, unknown>) {
  const raw = row.__raw as ReviewDataRecordRowResponse | undefined;
  return typeof raw?.id === 'number' ? raw : null;
}

async function toggleProblemPanel(recordId: number) {
  if (isProblemExpanded(recordId)) {
    expandedRowKeys.value = [];
    return;
  }
  expandedRowKeys.value = [recordId];
  if (!problemItemsMap.value[recordId]) {
    await loadProblemItems(recordId);
  }
}

async function toggleProblemPanelByRow(row: Record<string, unknown>) {
  const raw = recordFromTableRow(row);
  if (!raw) {
    return;
  }
  await toggleProblemPanel(raw.id);
}

function isProblemExpandedByRow(row: Record<string, unknown>) {
  const raw = recordFromTableRow(row);
  return raw ? isProblemExpanded(raw.id) : false;
}

async function handleCreateProblemItemByRow(row: Record<string, unknown>) {
  const raw = recordFromTableRow(row);
  if (!raw) {
    return;
  }
  await handleCreateProblemItem(raw.id);
}

async function handleOpenDetail(row: Record<string, unknown>) {
  const raw = row.__raw as ReviewDataRecordRowResponse | undefined;
  if (!raw) {
    return;
  }
  try {
    await loadDetail(raw.id);
    detailVisible.value = true;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '评审详情加载失败');
  }
}

async function handleEditRecord(row: Record<string, unknown>) {
  const raw = row.__raw as ReviewDataRecordRowResponse | undefined;
  if (!raw) {
    return;
  }
  try {
    const detail = await api.getReviewDataRecordDetail(raw.id);
    editingRecordId.value = raw.id;
    recordEditMode.value = true;
    recordForm.value = createReviewRecordFormFromRow(detail.record, detail.reviewExperts);
    recordDialogVisible.value = true;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '评审详情加载失败');
  }
}

function handleCreateRecord() {
  editingRecordId.value = null;
  recordEditMode.value = false;
  recordForm.value = createEmptyReviewRecordForm();
  recordDialogVisible.value = true;
}

async function submitRecord(payload: ReviewDataRecordSaveRequest) {
  recordDialogSaving.value = true;
  try {
    if (recordEditMode.value && editingRecordId.value != null) {
      await api.updateReviewDataRecord(editingRecordId.value, payload);
      ElMessage.success('评审记录已更新');
    } else {
      await api.createReviewDataRecord(payload);
      ElMessage.success('评审记录已创建');
    }
    recordDialogVisible.value = false;
    await Promise.all([loadFilterOptions(), loadRows()]);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '评审记录保存失败');
  } finally {
    recordDialogSaving.value = false;
  }
}

async function handleDeleteRecord(row: Record<string, unknown>) {
  const raw = row.__raw as ReviewDataRecordRowResponse | undefined;
  if (!raw) {
    return;
  }
  try {
    await ElMessageBox.confirm(`确认删除评审“${raw.title}”吗？`, '删除评审', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    });
    await api.deleteReviewDataRecord(raw.id);
    ElMessage.success('评审记录已删除');
    await Promise.all([loadFilterOptions(), loadRows()]);
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '评审记录删除失败');
    }
  }
}

async function handleCreateProblemItem(recordId: number) {
  try {
    const detail = await api.getReviewDataRecordDetail(recordId);
    currentProblemRecordId.value = recordId;
    currentProblemItemId.value = null;
    currentProblemExpertOptions.value = detail.reviewExperts;
    problemDialogEditMode.value = false;
    problemForm.value = createEmptyProblemItemForm();
    problemDialogVisible.value = true;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '评审问题初始化失败');
  }
}

async function handleEditProblemItem(recordId: number, item: ReviewDataProblemItemResponse) {
  try {
    const detail = await api.getReviewDataRecordDetail(recordId);
    currentProblemRecordId.value = recordId;
    currentProblemItemId.value = item.id;
    currentProblemExpertOptions.value = detail.reviewExperts;
    problemDialogEditMode.value = true;
    problemForm.value = createProblemItemFormFromRow(item);
    problemDialogVisible.value = true;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '评审问题详情加载失败');
  }
}

async function submitProblemItem(payload: ReviewDataProblemItemSaveRequest) {
  if (currentProblemRecordId.value == null) {
    return;
  }
  problemDialogSaving.value = true;
  try {
    if (problemDialogEditMode.value && currentProblemItemId.value != null) {
      await api.updateReviewDataProblemItem(currentProblemRecordId.value, currentProblemItemId.value, payload);
      ElMessage.success('评审问题已更新');
    } else {
      await api.createReviewDataProblemItem(currentProblemRecordId.value, payload);
      ElMessage.success('评审问题已新增');
    }
    problemDialogVisible.value = false;
    await Promise.all([loadRows(), loadProblemItems(currentProblemRecordId.value)]);
    if (detailVisible.value && detailData.value?.record.id === currentProblemRecordId.value) {
      await loadDetail(currentProblemRecordId.value);
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '评审问题保存失败');
  } finally {
    problemDialogSaving.value = false;
  }
}

async function handleDeleteProblemItem(recordId: number, itemId: number) {
  try {
    await ElMessageBox.confirm('确认删除这条评审问题吗？', '删除评审问题', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    });
    await api.deleteReviewDataProblemItem(recordId, itemId);
    ElMessage.success('评审问题已删除');
    await Promise.all([loadRows(), loadProblemItems(recordId)]);
    if (detailVisible.value && detailData.value?.record.id === recordId) {
      await loadDetail(recordId);
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '评审问题删除失败');
    }
  }
}

function problemItemsFor(recordId: number) {
  return buildProblemItemTableRows(problemItemsMap.value[recordId] ?? []);
}

function displayText(value: unknown) {
  const text = String(value ?? '').trim();
  return text || '-';
}

function formatDate(value?: string | null) {
  return value ? value.slice(0, 10) : '-';
}
</script>

<template>
  <section class="review-data-page">
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
      :expanded-row-keys="expandedRowKeys"
      :expand-column-visible="false"
      :row-actions-width="168"
      query-button-text="查询"
      empty-description="当前筛选条件下没有可展示的评审记录。"
      @filter-change="handleFilterChange"
      @reset="handleReset"
      @refresh="handleRefresh"
      @query="handleQuery"
      @clear-filter="clearFilter"
      @update:advanced-visible="advancedVisible = $event"
      @sort-change="handleSortChange"
      @current-change="handlePageChange"
      @size-change="handleSizeChange"
      @expand-change="handleExpandChange"
    >
      <template #primary-actions>
        <el-button type="primary" :icon="Plus" @click="handleCreateRecord">新增评审</el-button>
        <el-button plain :icon="Download" :loading="exportLoading" @click="handleExportExcel">导出Excel</el-button>
      </template>

      <template #cell-title="{ row }">
        <div class="title-cell">
          <span class="title-main">{{ row.title }}</span>
          <span class="title-sub">{{ row.reviewExpertsSummary }}</span>
        </div>
      </template>

      <template #expand="{ row }">
        <div class="problem-panel">
          <div class="problem-panel-head">
            <div class="problem-panel-title">
              <span>评审问题清单</span>
              <el-tag size="small" effect="plain">共 {{ (row.__raw as ReviewDataRecordRowResponse).problemCount }} 条</el-tag>
            </div>
            <el-button type="primary" text :icon="Plus" @click="handleCreateProblemItem((row.__raw as ReviewDataRecordRowResponse).id)">
              新增问题
            </el-button>
          </div>

          <el-table
            v-loading="problemLoadingMap[(row.__raw as ReviewDataRecordRowResponse).id]"
            :data="problemItemsFor((row.__raw as ReviewDataRecordRowResponse).id)"
            class="problem-subtable"
            border
            stripe
            empty-text="当前评审下还没有录入问题清单。"
          >
            <el-table-column
              v-for="column in problemColumns"
              :key="column.key"
              :prop="column.key"
              :label="column.label"
              :width="column.width"
              :min-width="column.minWidth"
              :align="column.align ?? 'left'"
              :show-overflow-tooltip="column.showOverflowTooltip ?? true"
            >
              <template #default="{ row: problemRow }">
                <template v-if="column.type === 'tag'">
                  <el-tag
                    v-for="tag in problemRow[column.key] as Array<{ label: string; type?: 'success' | 'warning' | 'info' | 'danger' | 'primary' }>"
                    :key="tag.label"
                    size="small"
                    :type="tag.type ?? 'info'"
                    effect="plain"
                  >
                    {{ tag.label }}
                  </el-tag>
                </template>
                <span v-else>{{ problemRow[column.key] }}</span>
              </template>
            </el-table-column>

            <el-table-column label="操作" width="120" fixed="right" align="center">
              <template #default="{ row: problemRow }">
                <el-button
                  type="primary"
                  link
                  @click="handleEditProblemItem((row.__raw as ReviewDataRecordRowResponse).id, problemRow.__raw as ReviewDataProblemItemResponse)"
                >
                  编辑
                </el-button>
                <el-button
                  type="danger"
                  link
                  @click="handleDeleteProblemItem((row.__raw as ReviewDataRecordRowResponse).id, (problemRow.__raw as ReviewDataProblemItemResponse).id)"
                >
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </template>

      <template #row-actions="{ row }">
        <div class="record-actions">
          <el-button
            class="record-actions-chip"
            :class="{ active: isProblemExpandedByRow(row) }"
            type="primary"
            link
            @click="toggleProblemPanelByRow(row)"
          >
            {{ isProblemExpandedByRow(row) ? '收起' : '清单' }}
          </el-button>
          <el-button class="record-actions-link" type="primary" link @click="handleOpenDetail(row)">查看</el-button>
          <el-dropdown>
            <span class="record-actions-more">
              更多
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="handleEditRecord(row)">编辑评审</el-dropdown-item>
                <el-dropdown-item @click="handleCreateProblemItemByRow(row)">新增问题</el-dropdown-item>
                <el-dropdown-item divided @click="handleDeleteRecord(row)">删除评审</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </template>
    </BaseRecordTable>

    <el-drawer
      v-model="detailVisible"
      title="评审详情"
      size="560px"
      append-to-body
      class="review-data-drawer"
    >
      <template v-if="detailData">
        <section class="detail-section">
          <header class="detail-section-head">
            <el-icon><Document /></el-icon>
            <span>评审基础信息</span>
          </header>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="项目名称">{{ displayText(detailData.record.projectName) }}</el-descriptions-item>
            <el-descriptions-item label="标题">{{ displayText(detailData.record.title) }}</el-descriptions-item>
            <el-descriptions-item label="模块">{{ displayText(detailData.record.moduleName) }}</el-descriptions-item>
            <el-descriptions-item label="评审类型">{{ displayText(detailData.record.reviewType) }}</el-descriptions-item>
            <el-descriptions-item label="评审日期">{{ formatDate(detailData.record.reviewDate) }}</el-descriptions-item>
            <el-descriptions-item label="评审负责人">{{ displayText(detailData.record.reviewOwner) }}</el-descriptions-item>
            <el-descriptions-item label="评审专家">{{ detailData.reviewExperts.join('、') || '-' }}</el-descriptions-item>
            <el-descriptions-item label="评审规模">{{ detailData.record.reviewScalePages }} 页</el-descriptions-item>
            <el-descriptions-item label="评审工作产品">{{ displayText(detailData.record.reviewProduct) }}</el-descriptions-item>
            <el-descriptions-item label="作者">{{ displayText(detailData.record.authorName) }}</el-descriptions-item>
            <el-descriptions-item label="评审版本">{{ displayText(detailData.record.reviewVersion) }}</el-descriptions-item>
          </el-descriptions>
        </section>

        <section class="detail-section">
          <header class="detail-section-head">
            <span>问题概览</span>
          </header>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="问题合计">{{ detailData.record.problemCount }}</el-descriptions-item>
            <el-descriptions-item label="缺陷密度">{{ detailData.record.problemDensity.toFixed(2) }}</el-descriptions-item>
            <el-descriptions-item label="更新时间">{{ displayText(detailData.record.updatedAt?.replace('T', ' ').slice(0, 19)) }}</el-descriptions-item>
            <el-descriptions-item label="当前状态">{{ detailData.record.deleted ? '已删除' : '有效' }}</el-descriptions-item>
          </el-descriptions>
        </section>
      </template>
    </el-drawer>

    <ReviewRecordFormDialog
      v-model:visible="recordDialogVisible"
      :saving="recordDialogSaving"
      :model-value="recordForm"
      :filter-options="filterOptions"
      :edit-mode="recordEditMode"
      @submit="submitRecord"
    />

    <ReviewProblemItemFormDialog
      v-model:visible="problemDialogVisible"
      :saving="problemDialogSaving"
      :model-value="problemForm"
      :filter-options="filterOptions"
      :expert-options-override="currentProblemExpertOptions"
      :edit-mode="problemDialogEditMode"
      @submit="submitProblemItem"
    />
  </section>
</template>

<style scoped>
.review-data-page {
  display: grid;
  gap: 8px;
}

.review-data-summary {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 10px;
}

.summary-card {
  display: grid;
  gap: 6px;
  min-height: 72px;
  padding: 12px 16px;
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
  font-size: 22px;
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

.record-actions {
  display: grid;
  grid-template-columns: 48px 40px 48px;
  align-items: center;
  justify-content: center;
  column-gap: 8px;
}

.record-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

.record-actions-chip {
  height: 24px;
  width: 48px;
  padding: 0 8px;
  border-radius: 6px;
  background: rgba(37, 99, 235, 0.08);
  color: #2563eb;
  font-size: 12px;
  font-weight: 600;
  justify-content: center;
  line-height: 24px;
}

.record-actions-chip:hover,
.record-actions-chip.active {
  background: rgba(37, 99, 235, 0.13);
  color: #1d4ed8;
}

.record-actions-link {
  height: 24px;
  width: 40px;
  padding: 0;
  color: #2563eb;
  font-size: 12px;
  font-weight: 500;
  justify-content: center;
  line-height: 24px;
}

.record-actions-link:hover {
  color: #1d4ed8;
}

.record-actions-more {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
  height: 24px;
  width: 48px;
  padding: 0;
  border-radius: 6px;
  cursor: pointer;
  font-size: 12px;
  color: rgba(37, 99, 235, 0.88);
  line-height: 24px;
}

.record-actions-more:hover {
  background: rgba(37, 99, 235, 0.08);
  color: #1d4ed8;
}

.problem-panel {
  display: grid;
  gap: 12px;
  padding: 12px 8px 4px 40px;
  background: rgba(248, 250, 252, 0.72);
}

.problem-panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.problem-panel-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: rgba(15, 23, 42, 0.82);
}

.problem-subtable {
  border-radius: 12px;
  overflow: hidden;
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

:deep(.review-data-drawer .el-drawer__header) {
  margin-bottom: 10px;
}

:deep(.review-data-drawer .el-descriptions__label) {
  width: 116px;
}

:deep(.problem-subtable .el-table__header th.el-table__cell) {
  background: #f8fafc;
}
</style>
