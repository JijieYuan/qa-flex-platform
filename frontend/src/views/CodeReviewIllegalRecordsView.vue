<script setup lang="ts">
import { computed, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { InfoFilled } from '@element-plus/icons-vue';
import BaseRecordTable from '../components/base/BaseRecordTable.vue';
import SyncMetaBadge from '../components/realtime/SyncMetaBadge.vue';
import {
  api,
  type CodeReviewIllegalRecordFilterOptionsResponse,
  type CodeReviewIllegalRecordRowResponse,
  type RealtimeWorkspaceStatusResponse,
  type StatisticBoardRuleExplanationResponse,
} from '../api';
import { useRouteTableState } from '../composables/useRouteTableState';
import type { RecordTableActiveFilterTag, RecordTableColumn, RecordTableFilterField } from '../types/record-table';

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
    sortBy: 'mergedAt',
    sortOrder: 'desc',
  },
});

const advancedVisible = ref(false);
const rows = ref<CodeReviewIllegalRecordRowResponse[]>([]);
const total = ref(0);
const detailVisible = ref(false);
const ruleExplanationVisible = ref(false);
const ruleExplanationLoading = ref(false);
const selectedRow = ref<CodeReviewIllegalRecordRowResponse | null>(null);
const syncStatus = ref<RealtimeWorkspaceStatusResponse | null>(null);
const ruleExplanation = ref<StatisticBoardRuleExplanationResponse | null>(null);

const filterOptions = ref<CodeReviewIllegalRecordFilterOptionsResponse>({
  requestTypes: [{ label: '合并请求', value: 'merge_request' }],
  repositoryNames: [],
  illegalTypes: [],
  targetBranches: [],
  mergedBys: [],
  moduleNames: [],
  projectNames: [],
});

const filterValues = computed<Record<string, unknown>>(() => {
  const start = String(route.query.mergedAtStart ?? '');
  const end = String(route.query.mergedAtEnd ?? '');
  return {
    repositoryName: String(route.query.repositoryName ?? ''),
    mergedAtRange: start && end ? [start, end] : [],
    illegalType: String(route.query.illegalType ?? ''),
    keyword: String(route.query.keyword ?? ''),
    requestType: String(route.query.requestType ?? ''),
    mergeRequestIid: String(route.query.mergeRequestIid ?? ''),
    owner: String(route.query.owner ?? ''),
    targetBranch: String(route.query.targetBranch ?? ''),
    mergedBy: String(route.query.mergedBy ?? ''),
    moduleName: String(route.query.moduleName ?? ''),
    projectName: String(route.query.projectName ?? ''),
  };
});

const primaryFilters = computed<RecordTableFilterField[]>(() => [
  {
    key: 'repositoryName',
    label: '代码库',
    type: 'select',
    width: 180,
    options: [{ label: '全部代码库', value: '' }, ...filterOptions.value.repositoryNames],
  },
  {
    key: 'mergedAtRange',
    label: '合并时间',
    type: 'daterange',
    width: 280,
    startPlaceholder: '开始日期',
    endPlaceholder: '结束日期',
  },
  {
    key: 'illegalType',
    label: '非法类型',
    type: 'select',
    width: 180,
    options: [{ label: '全部非法类型', value: '' }, ...filterOptions.value.illegalTypes],
  },
  {
    key: 'keyword',
    label: '关键字',
    type: 'input',
    width: 260,
    placeholder: '搜索合并请求内容、责任人或项目',
  },
]);

const advancedFilters = computed<RecordTableFilterField[]>(() => [
  {
    key: 'requestType',
    label: '请求类型',
    type: 'select',
    options: [{ label: '全部请求类型', value: '' }, ...filterOptions.value.requestTypes],
  },
  { key: 'mergeRequestIid', label: '合并请求编号', type: 'input', placeholder: '输入合并请求编号' },
  { key: 'owner', label: '标注责任人', type: 'input', placeholder: '输入标注责任人' },
  {
    key: 'targetBranch',
    label: '目标分支',
    type: 'select',
    options: [{ label: '全部目标分支', value: '' }, ...filterOptions.value.targetBranches],
  },
  {
    key: 'mergedBy',
    label: '合并人',
    type: 'select',
    options: [{ label: '全部合并人', value: '' }, ...filterOptions.value.mergedBys],
  },
  {
    key: 'moduleName',
    label: '模块名称',
    type: 'select',
    options: [{ label: '全部模块名称', value: '' }, ...filterOptions.value.moduleNames],
  },
  {
    key: 'projectName',
    label: '项目名称',
    type: 'select',
    options: [{ label: '全部项目名称', value: '' }, ...filterOptions.value.projectNames],
  },
]);

const activeFilterTags = computed<RecordTableActiveFilterTag[]>(() => {
  const values = filterValues.value;
  const tags: RecordTableActiveFilterTag[] = [];
  if (values.repositoryName) tags.push({ key: 'repositoryName', label: '代码库', value: String(values.repositoryName) });
  if (Array.isArray(values.mergedAtRange) && values.mergedAtRange.length === 2) {
    tags.push({ key: 'mergedAtRange', label: '合并时间', value: `${values.mergedAtRange[0]} ~ ${values.mergedAtRange[1]}` });
  }
  if (values.illegalType) tags.push({ key: 'illegalType', label: '非法类型', value: String(values.illegalType) });
  if (values.keyword) tags.push({ key: 'keyword', label: '关键字', value: String(values.keyword) });
  if (values.requestType) tags.push({ key: 'requestType', label: '请求类型', value: String(values.requestType) });
  if (values.mergeRequestIid) tags.push({ key: 'mergeRequestIid', label: '合并请求编号', value: String(values.mergeRequestIid) });
  if (values.owner) tags.push({ key: 'owner', label: '标注责任人', value: String(values.owner) });
  if (values.targetBranch) tags.push({ key: 'targetBranch', label: '目标分支', value: String(values.targetBranch) });
  if (values.mergedBy) tags.push({ key: 'mergedBy', label: '合并人', value: String(values.mergedBy) });
  if (values.moduleName) tags.push({ key: 'moduleName', label: '模块名称', value: String(values.moduleName) });
  if (values.projectName) tags.push({ key: 'projectName', label: '项目名称', value: String(values.projectName) });
  return tags;
});

const columns = computed<RecordTableColumn[]>(() => [
  { key: 'mergeRequestIid', label: '合并请求编号', type: 'link', sortable: true, width: 128, fixed: 'left' },
  { key: 'mergeRequestContent', label: '合并请求内容', sortable: true, minWidth: 260 },
  { key: 'owner', label: '标注责任人', sortable: true, minWidth: 140 },
  { key: 'projectName', label: '所属项目', sortable: true, minWidth: 160 },
  { key: 'mergedAt', label: '合并时间', type: 'datetime', sortable: true, minWidth: 180 },
  { key: 'mergedBy', label: '合并人', sortable: true, minWidth: 140 },
  { key: 'moduleName', label: '模块名', sortable: true, minWidth: 140 },
  { key: 'targetBranch', label: '合并目标分支', sortable: true, minWidth: 180 },
  { key: 'illegalTypes', label: '非法类型', type: 'tags', minWidth: 220 },
  { key: 'commentRate', label: '代码注释比例(%)', sortable: true, width: 160, align: 'right' },
  { key: 'defectCount', label: '缺陷数量', type: 'number', sortable: true, width: 120, align: 'right' },
  { key: 'addedLines', label: '新增代码行数(行)', type: 'number', sortable: true, width: 150, align: 'right' },
]);

const tableRows = computed<Record<string, unknown>[]>(() =>
  rows.value.map((row) => ({
    __raw: row,
    mergeRequestIid: row.mergeRequestLink
      ? { label: String(row.mergeRequestIid), href: row.mergeRequestLink }
      : String(row.mergeRequestIid),
    mergeRequestContent: row.mergeRequestContent,
    owner: row.owner || '-',
    projectName: row.projectName || '-',
    mergedAt: row.mergedAt ? row.mergedAt.replace('T', ' ').slice(0, 19) : '-',
    mergedBy: row.mergedBy || '-',
    moduleName: row.moduleName || '-',
    targetBranch: row.targetBranch || '-',
    illegalTypes: row.illegalTypes.map((label) => ({ label, type: 'warning' as const })),
    commentRate: formatPercent(row.commentRate),
    defectCount: row.defectCount,
    addedLines: row.addedLines,
  })),
);

function formatDateTime(value?: string | null) {
  return value ? value.replace('T', ' ').slice(0, 19) : '-';
}

function formatMetric(value?: number | null, suffix = '') {
  if (value == null) {
    return '-';
  }
  return `${value}${suffix}`;
}

function formatPercent(value?: number | null) {
  if (value == null) {
    return '-';
  }
  return `${(value * 100).toFixed(2)}%`;
}
const lastSyncedText = computed(() => formatDateTime(syncStatus.value?.lastSyncedAt));
const ruleExplanationSteps = computed(() => ruleExplanation.value?.flowSteps || []);
const ruleExplanationMetrics = computed(() => ruleExplanation.value?.metricDefinitions || []);
const ruleFirstInputCount = computed(() => ruleExplanationSteps.value[0]?.inputCount || 0);
const __duplicateRuleFinalOutputCount = computed(() => {
  const steps = ruleExplanationSteps.value;
  return steps.length ? steps[steps.length - 1].outputCount : 0;
});
const __duplicateRuleFinalRetainedRate = computed(() => {
  if (!ruleFirstInputCount.value) {
    return '0%';
  }
  return `${((__duplicateRuleFinalOutputCount.value / ruleFirstInputCount.value) * 100).toFixed(1)}%`;
});
const __duplicateQaFriendlyRuleSummary = computed(() => {
  if (!ruleExplanation.value?.supported) {
    return '';
  }
  if (!ruleExplanationSteps.value.length) {
    return ruleExplanation.value?.summary || '当前页面已经启用规则说明，但暂时没有可展示的统计过程。';
  }
  return `当前结果一共基于 ${ruleFirstInputCount.value} 条合并请求逐步检查，最终命中 ${ruleFinalOutputCount.value} 条需要关注的记录，占原始数据的 ${ruleFinalRetainedRate.value}。`;
});
const __duplicateRuleExplanationSteps = computed(() => ruleExplanation.value?.flowSteps ?? []);
const __duplicateRuleExplanationMetrics = computed(() => ruleExplanation.value?.metricDefinitions ?? []);
const __duplicateRuleFirstInputCount = computed(() => __duplicateRuleExplanationSteps.value[0]?.inputCount ?? 0);
const ruleFinalOutputCount = computed(() => {
  const steps = ruleExplanationSteps.value;
  return steps.length ? steps[steps.length - 1].outputCount : 0;
});
const ruleFinalRetainedRate = computed(() => {
  if (!ruleFirstInputCount.value) {
    return '0%';
  }
  return `${((ruleFinalOutputCount.value / ruleFirstInputCount.value) * 100).toFixed(1)}%`;
});
const qaFriendlyRuleSummary = computed(() => {
  if (!ruleExplanation.value?.supported) {
    return '';
  }
  if (!ruleExplanationSteps.value.length) {
    return ruleExplanation.value?.summary || '当前页面已经启用规则说明，但暂时没有可展示的统计过程。';
  }
  return `当前结果一共基于 ${ruleFirstInputCount.value} 条合并请求逐步检查，最终命中 ${ruleFinalOutputCount.value} 条需要关注的记录，占原始数据的 ${ruleFinalRetainedRate.value}。`;
});

function openDetailDrawer(row: Record<string, unknown>) {
  selectedRow.value = (row.__raw as CodeReviewIllegalRecordRowResponse) ?? null;
  detailVisible.value = true;
}

async function loadFilterOptions() {
  filterOptions.value = await api.getCodeReviewIllegalRecordFilterOptions(route.query.projectId as string | undefined);
}

async function loadSyncStatus() {
  try {
    syncStatus.value = await api.getCodeReviewIllegalRecordRealtimeStatus();
  } catch {
    syncStatus.value = null;
  }
}

async function loadRuleExplanation() {
  ruleExplanationLoading.value = true;
  try {
    ruleExplanation.value = await api.getCodeReviewIllegalRecordRuleExplanation();
  } catch {
    ruleExplanation.value = null;
  } finally {
    ruleExplanationLoading.value = false;
  }
}

async function loadTableData() {
  const response = await api.getCodeReviewIllegalRecords({
    projectId: route.query.projectId as string | undefined,
    repositoryName: String(route.query.repositoryName ?? ''),
    mergedAtStart: String(route.query.mergedAtStart ?? ''),
    mergedAtEnd: String(route.query.mergedAtEnd ?? ''),
    keyword: String(route.query.keyword ?? ''),
    projectName: String(route.query.projectName ?? ''),
    requestType: String(route.query.requestType ?? ''),
    targetBranch: String(route.query.targetBranch ?? ''),
    mergedBy: String(route.query.mergedBy ?? ''),
    moduleName: String(route.query.moduleName ?? ''),
    illegalType: String(route.query.illegalType ?? ''),
    mergeRequestIid: String(route.query.mergeRequestIid ?? ''),
    owner: String(route.query.owner ?? ''),
    page: page.value,
    size: pageSize.value,
    sortBy: sortBy.value || 'mergedAt',
    sortOrder: (sortOrder.value || 'desc') as 'asc' | 'desc',
  });
  rows.value = response.records;
  total.value = response.total;
}

bindLoader(async () => {
  try {
    await loadTableData();
    await loadFilterOptions();
    await loadSyncStatus();
    await loadRuleExplanation();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '非法记录数据加载失败');
    rows.value = [];
    total.value = 0;
  }
});

async function handleFilterChange(payload: { key: string; value: string | string[] | null }) {
  if (payload.key === 'mergedAtRange') {
    const [start, end] = Array.isArray(payload.value) ? payload.value : [];
    await patchQuery({
      page: 1,
      mergedAtStart: start || null,
      mergedAtEnd: end || null,
    });
    return;
  }

  await patchQuery({
    page: 1,
    [payload.key]: Array.isArray(payload.value) ? payload.value[0] ?? null : payload.value,
  });
}

async function handleReset() {
  await patchQuery({
    page: 1,
    sortBy: 'mergedAt',
    sortOrder: 'desc',
    keyword: null,
    repositoryName: null,
    mergedAtStart: null,
    mergedAtEnd: null,
    projectName: null,
    requestType: null,
    targetBranch: null,
    mergedBy: null,
    moduleName: null,
    illegalType: null,
    mergeRequestIid: null,
    owner: null,
  });
}

async function handleQuery() {
  await patchQuery({ page: 1 });
}

async function handleSizeChange(nextSize: number) {
  await patchQuery({ pageSize: nextSize, page: 1 });
}

async function handleCurrentChange(nextPage: number) {
  await patchQuery({ page: nextPage });
}

async function handleSortChange(payload: { prop: string; order: 'ascending' | 'descending' | null }) {
  await patchQuery({
    sortBy: payload.prop || 'mergedAt',
    sortOrder: payload.order === 'ascending' ? 'asc' : 'desc',
    page: 1,
  });
}

async function handleClearFilter(key: string) {
  if (key === 'mergedAtRange') {
    await patchQuery({ page: 1, mergedAtStart: null, mergedAtEnd: null });
    return;
  }
  await patchQuery({ page: 1, [key]: null });
}

function openRuleExplanation() {
  if (!ruleExplanation.value?.supported) {
    ElMessage.warning(ruleExplanation.value?.unsupportedReason || '当前页面暂不支持规则说明');
    return;
  }
  ruleExplanationVisible.value = true;
}

function ruleStepRemovedCount(step: { inputCount: number; outputCount: number }) {
  return Math.max(step.inputCount - step.outputCount, 0);
}

function ruleStepRetainedRate(step: { inputCount: number; outputCount: number }) {
  if (!step.inputCount) {
    return '0%';
  }
  return `${((step.outputCount / step.inputCount) * 100).toFixed(1)}%`;
}

function ruleStepSummary(step: { inputCount: number; outputCount: number }, index: number) {
  const changed = ruleStepRemovedCount(step);
  if (changed <= 0) {
    return `第 ${index + 1} 步检查后，数据没有变化，仍命中 ${step.outputCount} 条。`;
  }
  return `第 ${index + 1} 步检查后，识别出 ${step.outputCount} 条需要关注的记录，较上一步变化 ${changed} 条，占当前输入数据的 ${ruleStepRetainedRate(step)}。`;
}

</script>

<template>
  <section class="record-page-shell">
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
      :show-search="false"
      :show-refresh="false"
      empty-description="当前筛选条件下没有查询到非法记录。"
      @filter-change="handleFilterChange"
      @reset="handleReset"
      @query="handleQuery"
      @clear-filter="handleClearFilter"
      @update:advanced-visible="advancedVisible = $event"
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
      @sort-change="handleSortChange"
    >
      <template #toolbar-prefix>
        <div class="record-page-toolbar-meta">
          <div class="record-page-toolbar-title">非法记录明细</div>
          <div class="record-page-toolbar-desc">代码走查结果明细与异常记录工作区</div>
        </div>
      </template>

      <template #toolbar-actions>
        <div class="record-page-summary">
          <SyncMetaBadge :value="lastSyncedText" />
          <el-button
            v-if="ruleExplanation?.supported"
            plain
            size="small"
            :icon="InfoFilled"
            :loading="ruleExplanationLoading"
            @click="openRuleExplanation"
          >
            规则说明
          </el-button>
          <span class="record-page-summary-divider" />
          <span class="record-page-summary-label">当前排序</span>
          <el-tag effect="plain" type="info" class="record-page-sort-tag">
            {{ sortBy || 'mergedAt' }} / {{ sortOrder || 'desc' }}
          </el-tag>
        </div>
      </template>

      <template #row-actions="{ row }">
        <el-button class="record-detail-trigger" link @click="openDetailDrawer(row)">查看详情</el-button>
      </template>
    </BaseRecordTable>

    <el-drawer
      v-model="detailVisible"
      size="540px"
      destroy-on-close
      class="record-detail-drawer"
    >
      <template #header>
        <div v-if="selectedRow" class="record-detail-header">
          <div class="record-detail-header-main">
            <div class="record-detail-header-kicker">代码走查详情</div>
            <div class="record-detail-header-title">MR #{{ selectedRow.mergeRequestIid }}</div>
            <div class="record-detail-header-meta">
              <span class="record-detail-meta-item">{{ selectedRow.projectName || '-' }}</span>
              <span class="record-detail-meta-dot" />
              <span class="record-detail-meta-item">{{ selectedRow.targetBranch || '-' }}</span>
              <span class="record-detail-meta-dot" />
              <span class="record-detail-meta-item">{{ selectedRow.mergedBy || '-' }}</span>
            </div>
          </div>
          <div class="record-detail-header-actions">
            <el-link
              v-if="selectedRow.mergeRequestLink"
              :href="selectedRow.mergeRequestLink"
              target="_blank"
              type="primary"
              :underline="false"
              class="record-detail-header-link"
            >
              打开 GitLab
            </el-link>
          </div>
        </div>
      </template>

      <template v-if="selectedRow">
        <section class="record-detail-section">
          <div class="record-detail-section-title">基础信息</div>
          <el-descriptions :column="2" border size="small" class="record-detail-descriptions">
            <el-descriptions-item label="合并请求编号">
              <el-link
                v-if="selectedRow.mergeRequestLink"
                :href="selectedRow.mergeRequestLink"
                target="_blank"
                type="primary"
              >
                {{ selectedRow.mergeRequestIid }}
              </el-link>
              <span v-else>{{ selectedRow.mergeRequestIid }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="请求类型">{{ selectedRow.requestType || '-' }}</el-descriptions-item>
            <el-descriptions-item label="所属项目">{{ selectedRow.projectName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="代码库">{{ selectedRow.repositoryName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="标注责任人">{{ selectedRow.owner || '-' }}</el-descriptions-item>
            <el-descriptions-item label="合并人">{{ selectedRow.mergedBy || '-' }}</el-descriptions-item>
            <el-descriptions-item label="模块名">{{ selectedRow.moduleName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="合并目标分支">{{ selectedRow.targetBranch || '-' }}</el-descriptions-item>
            <el-descriptions-item label="合并时间">{{ formatDateTime(selectedRow.mergedAt) }}</el-descriptions-item>
            <el-descriptions-item label="项目 ID">{{ selectedRow.projectId ?? '-' }}</el-descriptions-item>
          </el-descriptions>
        </section>

        <section class="record-detail-section">
          <div class="record-detail-section-title">合并请求内容</div>
          <div class="record-detail-content">{{ selectedRow.mergeRequestContent || '-' }}</div>
        </section>

        <section class="record-detail-section">
          <div class="record-detail-section-title">非法判定</div>
          <div class="record-detail-tags">
            <el-tag
              v-for="illegalType in selectedRow.illegalTypes"
              :key="illegalType"
              type="warning"
              effect="plain"
            >
              {{ illegalType }}
            </el-tag>
            <span v-if="!selectedRow.illegalTypes.length" class="record-detail-empty">-</span>
          </div>
        </section>

        <section class="record-detail-section">
          <div class="record-detail-section-title">度量指标</div>
          <div class="record-detail-metrics">
            <article class="record-detail-metric-card">
              <span class="record-detail-metric-label">代码注释比例</span>
              <strong class="record-detail-metric-value">{{ formatPercent(selectedRow.commentRate) }}</strong>
            </article>
            <article class="record-detail-metric-card">
              <span class="record-detail-metric-label">缺陷数量</span>
              <strong class="record-detail-metric-value">{{ formatMetric(selectedRow.defectCount) }}</strong>
            </article>
            <article class="record-detail-metric-card">
              <span class="record-detail-metric-label">新增代码行数</span>
              <strong class="record-detail-metric-value">{{ formatMetric(selectedRow.addedLines, ' 行') }}</strong>
            </article>
          </div>
        </section>
      </template>
    </el-drawer>

    <el-drawer
      v-model="ruleExplanationVisible"
      size="44%"
      destroy-on-close
      class="record-detail-drawer"
    >
      <template #header>
        <div class="record-detail-header">
          <div class="record-detail-header-main">
            <div class="record-detail-header-kicker">规则说明</div>
            <div class="record-detail-header-title">{{ ruleExplanation?.title || '代码走查非法记录规则说明' }}</div>
            <div class="record-detail-header-meta">
              <span class="record-detail-meta-item">版本 {{ ruleExplanation?.version || '-' }}</span>
            </div>
          </div>
        </div>
      </template>

      <div v-loading="ruleExplanationLoading" class="record-rule-panel">
        <el-empty
          v-if="!ruleExplanation?.supported"
          :description="ruleExplanation?.unsupportedReason || '当前页面暂不支持规则说明。'"
        />

        <template v-else>
          <section class="record-detail-section">
            <div class="record-detail-section-title">统计范围</div>
            <div class="record-detail-content">{{ ruleExplanation?.scopeDescription || '-' }}</div>
          </section>

          <section class="record-detail-section">
            <div class="record-detail-section-title">规则摘要</div>
            <div class="record-detail-content">{{ ruleExplanation?.summary || '-' }}</div>
          </section>

          <section class="record-detail-section">
            <div class="record-detail-section-title">Flow 过滤过程</div>
            <el-timeline>
              <el-timeline-item
                v-for="step in ruleExplanation?.flowSteps || []"
                :key="step.key"
                placement="top"
              >
                <div class="rule-flow-step">
                  <div class="rule-flow-step-title">{{ step.title }}</div>
                  <div class="rule-flow-step-description">{{ step.description }}</div>
                  <div class="rule-flow-step-metrics">
                    <el-tag effect="plain">输入 {{ step.inputCount }}</el-tag>
                    <el-tag effect="plain" type="success">命中 {{ step.outputCount }}</el-tag>
                  </div>
                  <div v-if="step.samples.length" class="rule-flow-step-samples">
                    <el-card
                      v-for="sample in step.samples"
                      :key="`${step.key}-${sample.label}-${sample.detail}`"
                      shadow="never"
                      class="rule-flow-sample-card"
                    >
                      <div class="rule-flow-sample-label">{{ sample.label }}</div>
                      <div class="rule-flow-sample-detail">{{ sample.detail }}</div>
                    </el-card>
                  </div>
                </div>
              </el-timeline-item>
            </el-timeline>
          </section>

          <section class="record-detail-section">
            <div class="record-detail-section-title">指标口径</div>
            <el-table :data="ruleExplanation?.metricDefinitions || []" border stripe>
              <el-table-column prop="label" label="指标" min-width="140" />
              <el-table-column prop="definition" label="定义" min-width="220" show-overflow-tooltip />
              <el-table-column prop="formula" label="计算方式" min-width="220" show-overflow-tooltip />
              <el-table-column prop="note" label="说明" min-width="180" show-overflow-tooltip />
            </el-table>
          </section>
        </template>
      </div>
    </el-drawer>
  </section>
</template>

<style scoped>
.record-page-shell {
  display: grid;
  gap: 12px;
}

.record-page-toolbar-meta {
  display: grid;
  gap: 2px;
}

.record-page-toolbar-title {
  font-size: 14px;
  font-weight: 700;
  color: rgba(15, 23, 42, 0.92);
  line-height: 1.2;
}

.record-page-toolbar-desc {
  font-size: 12px;
  color: rgba(15, 23, 42, 0.45);
  line-height: 1.4;
}

.record-page-summary {
  display: flex;
  align-items: center;
  gap: 8px;
}

.record-page-summary-label {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
}

.record-page-summary-divider {
  width: 1px;
  height: 14px;
  background: rgba(15, 23, 42, 0.1);
}

.record-page-sort-tag {
  border-radius: 999px;
}

.record-detail-trigger {
  padding-inline: 0;
  font-weight: 500;
  color: rgba(37, 99, 235, 0.88);
}

.record-detail-trigger:hover {
  color: rgb(29, 78, 216);
}

.record-detail-drawer :deep(.el-drawer__header) {
  margin-bottom: 0;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(15, 23, 42, 0.06);
}

.record-detail-drawer :deep(.el-drawer__body) {
  padding-top: 16px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
}

.record-detail-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.record-detail-header-main {
  display: grid;
  gap: 8px;
}

.record-detail-header-kicker {
  font-size: 12px;
  font-weight: 600;
  color: rgba(15, 23, 42, 0.42);
  letter-spacing: 0.02em;
}

.record-detail-header-title {
  font-size: 20px;
  font-weight: 700;
  line-height: 1.2;
  color: rgba(15, 23, 42, 0.94);
}

.record-detail-header-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.record-detail-meta-item {
  font-size: 13px;
  color: rgba(15, 23, 42, 0.58);
}

.record-detail-meta-dot {
  width: 4px;
  height: 4px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.18);
}

.record-detail-header-actions {
  display: flex;
  align-items: center;
}

.record-detail-section {
  display: grid;
  gap: 12px;
  margin-bottom: 20px;
}

.record-detail-section-title {
  font-size: 13px;
  font-weight: 700;
  color: rgba(15, 23, 42, 0.76);
}

.record-detail-content {
  padding: 14px 16px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid rgba(15, 23, 42, 0.06);
  color: rgba(15, 23, 42, 0.76);
  line-height: 1.7;
}

.record-detail-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.record-detail-empty {
  color: rgba(15, 23, 42, 0.4);
}

.record-detail-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.record-detail-metric-card {
  display: grid;
  gap: 8px;
  padding: 16px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(15, 23, 42, 0.06);
}

.record-detail-metric-label {
  font-size: 12px;
  color: rgba(15, 23, 42, 0.48);
}

.record-detail-metric-value {
  font-size: 22px;
  line-height: 1;
  color: rgba(15, 23, 42, 0.92);
}

.record-rule-panel {
  display: grid;
  gap: 16px;
}

.rule-flow-step {
  display: grid;
  gap: 10px;
}

.rule-flow-step-title {
  font-size: 14px;
  font-weight: 700;
  color: rgba(15, 23, 42, 0.9);
}

.rule-flow-step-description {
  font-size: 13px;
  line-height: 1.7;
  color: rgba(15, 23, 42, 0.68);
}

.rule-flow-step-metrics {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.rule-flow-step-samples {
  display: grid;
  gap: 8px;
}

.rule-flow-sample-card :deep(.el-card__body) {
  display: grid;
  gap: 4px;
  padding: 12px 14px;
}

.rule-flow-sample-label {
  font-size: 13px;
  font-weight: 600;
  color: rgba(15, 23, 42, 0.82);
}

.rule-flow-sample-detail {
  font-size: 12px;
  line-height: 1.6;
  color: rgba(15, 23, 42, 0.6);
}

@media (max-width: 960px) {
  .record-detail-metrics {
    grid-template-columns: 1fr;
  }
}
</style>
