<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { Delete, InfoFilled, Plus } from '@element-plus/icons-vue';
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
import {
  buildCodeReviewDemoRuleFields,
  buildCodeReviewDemoIllegalTypeOptions,
  codeReviewDemoOperatorLabel,
  countCodeReviewDemoRuleMatches,
  createCodeReviewDemoRule,
  createDefaultCodeReviewDemoRules,
  describeCodeReviewDemoRule,
  evaluateCodeReviewDemoRules,
  usesValueInput,
  type CodeReviewDemoRule,
  type CodeReviewDemoRuleField,
} from './code-review-rule-demo';

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
const demoRuleEnabled = ref(false);
const demoRules = reactive<CodeReviewDemoRule[]>([]);

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

const demoRuleFields = computed(() =>
  buildCodeReviewDemoRuleFields({
    repositoryNames: filterOptions.value.repositoryNames,
    illegalTypes: filterOptions.value.illegalTypes,
    targetBranches: filterOptions.value.targetBranches,
    mergedBys: filterOptions.value.mergedBys,
    moduleNames: filterOptions.value.moduleNames,
    projectNames: filterOptions.value.projectNames,
  }),
);

const demoIllegalTypeOptions = computed(() => buildCodeReviewDemoIllegalTypeOptions(filterOptions.value.illegalTypes));
const demoMatchedRows = computed(() => {
  if (!demoRuleEnabled.value) {
    return rows.value;
  }
  return evaluateCodeReviewDemoRules(rows.value, demoRules, demoRuleFields.value);
});

const demoHasConditions = computed(() => demoRules.length > 0);
const demoMatchedCount = computed(() => demoMatchedRows.value.length);
const demoRuleSummaryText = computed(() => {
  if (!demoRuleEnabled.value || !demoHasConditions.value) {
    return '当前未启用 Demo 规则，本页仍展示后端返回的原始结果。';
  }
  return `当前页已加载 ${rows.value.length} 条记录，Demo 规则命中 ${demoMatchedCount.value} 条。`;
});
const tableEmptyDescription = computed(() =>
  demoRuleEnabled.value && demoHasConditions.value
    ? '当前页数据未命中 Demo 规则，请调整字段、关系或取值。'
    : '当前筛选条件下没有查询到非法记录。',
);

const tableRows = computed<Record<string, unknown>[]>(() =>
  demoMatchedRows.value.map((row) => ({
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
  return `${value.toFixed(2)}%`;
}
const lastSyncedText = computed(() => formatDateTime(syncStatus.value?.lastSyncedAt));
const ruleExplanationSteps = computed(() => ruleExplanation.value?.flowSteps || []);
const ruleExplanationMetrics = computed(() => ruleExplanation.value?.metricDefinitions || []);
const ruleFirstInputCount = computed(() => ruleExplanationSteps.value[0]?.inputCount || 0);
const illegalTotalStep = computed(() => ruleExplanationSteps.value.find((step) => step.key === 'illegal-total') || null);
const ruleFinalOutputCount = computed(() => {
  if (illegalTotalStep.value) {
    return illegalTotalStep.value.outputCount;
  }
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
const ruleExclusionSteps = computed(() => ruleExplanationSteps.value.slice(1));
const ruleConfigTitle = computed(() => (ruleExplanation.value?.title || '代码走查非法记录规则说明').replace('规则说明', '规则配置'));

function resolveDemoRuleField(fieldKey: string) {
  return demoRuleFields.value.find((field) => field.key === fieldKey) ?? null;
}

function demoFieldOptions(field: CodeReviewDemoRuleField | null) {
  return field?.options ?? [];
}

function isDemoSelectField(field: CodeReviewDemoRuleField | null) {
  return field?.type === 'select' && demoFieldOptions(field).length > 0;
}

function demoRuleMatchCount(rule: CodeReviewDemoRule) {
  return countCodeReviewDemoRuleMatches(rows.value, rule, demoRuleFields.value);
}

function demoRuleSentence(rule: CodeReviewDemoRule) {
  return describeCodeReviewDemoRule(rule, demoRuleFields.value);
}

function ensureDemoRulesInitialized() {
  if (demoRules.length) {
    return;
  }
  demoRules.push(...createDefaultCodeReviewDemoRules(demoRuleFields.value));
}

function syncDemoRuleWithField(rule: CodeReviewDemoRule) {
  const field = resolveDemoRuleField(rule.fieldKey);
  if (!field) {
    rule.operator = 'eq';
    rule.value = '';
    return;
  }
  if (!field.operators.includes(rule.operator)) {
    rule.operator = field.operators[0] ?? 'eq';
  }
  if (!usesValueInput(rule.operator)) {
    rule.value = '';
    return;
  }
  if (isDemoSelectField(field)) {
    const options = demoFieldOptions(field);
    if (!options.some((item) => item.value === rule.value)) {
      rule.value = '';
    }
  }
}

function addDemoRule() {
  demoRules.push(createCodeReviewDemoRule(demoRuleFields.value[0], demoIllegalTypeOptions.value[0]?.value ?? ''));
}

function removeDemoRule(ruleId: string) {
  const index = demoRules.findIndex((rule) => rule.id === ruleId);
  if (index >= 0) {
    demoRules.splice(index, 1);
  }
}

function handleDemoFieldChange(rule: CodeReviewDemoRule) {
  const field = resolveDemoRuleField(rule.fieldKey);
  rule.operator = field?.operators[0] ?? 'eq';
  rule.value = '';
  syncDemoRuleWithField(rule);
}

function handleDemoOperatorChange(rule: CodeReviewDemoRule) {
  syncDemoRuleWithField(rule);
}

function resetDemoRules() {
  demoRuleEnabled.value = false;
  demoRules.splice(0, demoRules.length, ...createDefaultCodeReviewDemoRules(demoRuleFields.value));
}

function createFallbackRuleExplanation(reason: string): StatisticBoardRuleExplanationResponse {
  return {
    boardKey: 'code-review-illegal-records',
    supported: false,
    title: '代码走查非法记录规则配置',
    version: null,
    scopeDescription: null,
    summary: null,
    flowSteps: [],
    metricDefinitions: [],
    unsupportedReason: reason,
  };
}

function openDetailDrawer(row: Record<string, unknown>) {
  selectedRow.value = (row.__raw as CodeReviewIllegalRecordRowResponse) ?? null;
  detailVisible.value = true;
}

async function loadFilterOptions() {
  filterOptions.value = await api.getCodeReviewIllegalRecordFilterOptions(route.query.projectId as string | undefined);
  if (!demoRules.length) {
    ensureDemoRulesInitialized();
  }
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
    ruleExplanation.value = createFallbackRuleExplanation('规则配置说明加载失败，请稍后重试。');
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
  if (!ruleExplanation.value) {
    ruleExplanation.value = createFallbackRuleExplanation('规则配置说明暂未加载完成，请稍后再试。');
  }
  ensureDemoRulesInitialized();
  if (!ruleExplanation.value.supported) {
    ElMessage.warning(ruleExplanation.value.unsupportedReason || '当前页面暂不支持规则配置说明');
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

function ruleStepSummary(step: { key?: string; inputCount: number; outputCount: number }, index: number) {
  if (step.key === 'illegal-total') {
    return `第 ${index + 1} 步检查后，识别出 ${step.outputCount} 条需要关注的记录，占原始数据的 ${ruleStepRetainedRate(step)}。`;
  }
  return `在已经命中的非法记录里，有 ${step.outputCount} 条命中第 ${index} 项检查规则，占全部非法记录的 ${ruleStepRetainedRate(step)}。`;
}

function metricFormulaSummary(metric: { label: string; definition: string; formula: string; note?: string | null }) {
  return `${metric.label}：${metric.definition}`;
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
      :empty-description="tableEmptyDescription"
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
            plain
            size="small"
            :icon="InfoFilled"
            :loading="ruleExplanationLoading"
            @click="openRuleExplanation"
          >
            规则配置
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
            <div class="record-detail-header-kicker">规则配置</div>
            <div class="record-detail-header-title">{{ ruleConfigTitle }}</div>
            <div class="record-detail-header-meta">
              <span class="record-detail-meta-item">版本 {{ ruleExplanation?.version || '-' }}</span>
              <span class="record-detail-meta-dot" />
              <span class="record-detail-meta-item">前端 Demo</span>
            </div>
          </div>
        </div>
      </template>

      <div v-loading="ruleExplanationLoading" class="record-rule-panel">
        <section class="record-detail-section">
          <div class="record-detail-section-title">如何使用这块配置</div>
          <div class="record-detail-content">
            当前这里展示的是“规则配置 Demo”。你可以直接修改每条规则里的字段、关系、取值和判定结果，用来预览句式化配置的交互效果。
            现在的修改只作用于本页已加载数据，不会写回后端，也不会改变正式非法判定口径。
          </div>
        </section>

        <section class="record-detail-section">
          <div class="record-detail-section-title">当前规则配置</div>
          <div class="rule-demo-header">
            <div class="rule-demo-header-main">
              <div class="rule-demo-title">把规则说明改成可编辑句式</div>
              <div class="rule-demo-note">{{ demoRuleSummaryText }}</div>
            </div>
            <div class="rule-demo-header-actions">
              <el-switch v-model="demoRuleEnabled" inline-prompt active-text="预览开" inactive-text="预览关" />
            </div>
          </div>
          <div class="rule-demo-toolbar">
            <div class="rule-demo-note">每条规则都表示“如果满足这个条件，就会被判定为某一种非法类型”。</div>
            <div class="rule-demo-toolbar-actions">
              <el-button size="small" :icon="Plus" @click="addDemoRule">新增规则</el-button>
              <el-button size="small" text @click="resetDemoRules">恢复默认</el-button>
            </div>
          </div>

          <div v-if="demoRules.length" class="rule-demo-list">
            <article
              v-for="rule in demoRules"
              :key="rule.id"
              class="rule-demo-card"
            >
              <div class="rule-demo-sentence">
                <span class="rule-demo-sentence-text">如果</span>
                <el-select
                  v-model="rule.fieldKey"
                  class="rule-demo-inline-select"
                  placeholder="选择字段"
                  @change="handleDemoFieldChange(rule)"
                >
                  <el-option
                    v-for="field in demoRuleFields"
                    :key="field.key"
                    :label="field.label"
                    :value="field.key"
                  />
                </el-select>
                <el-select
                  v-model="rule.operator"
                  class="rule-demo-inline-select"
                  placeholder="选择关系"
                  @change="handleDemoOperatorChange(rule)"
                >
                  <el-option
                    v-for="operator in resolveDemoRuleField(rule.fieldKey)?.operators ?? []"
                    :key="operator"
                    :label="codeReviewDemoOperatorLabel(operator)"
                    :value="operator"
                  />
                </el-select>
                <template v-if="usesValueInput(rule.operator)">
                  <el-select
                    v-if="isDemoSelectField(resolveDemoRuleField(rule.fieldKey))"
                    v-model="rule.value"
                    class="rule-demo-inline-value"
                    placeholder="选择取值"
                    clearable
                  >
                    <el-option
                      v-for="option in demoFieldOptions(resolveDemoRuleField(rule.fieldKey))"
                      :key="option.value"
                      :label="option.label"
                      :value="option.value"
                    />
                  </el-select>
                  <el-input
                    v-else
                    v-model="rule.value"
                    class="rule-demo-inline-value"
                    :inputmode="resolveDemoRuleField(rule.fieldKey)?.type === 'number' ? 'decimal' : 'text'"
                    :placeholder="resolveDemoRuleField(rule.fieldKey)?.type === 'number' ? '输入数值' : '输入取值'"
                    clearable
                  />
                </template>
                <span v-else class="rule-demo-sentence-text">时</span>
                <span class="rule-demo-sentence-text">，就会被判定为</span>
                <el-select
                  v-model="rule.illegalType"
                  class="rule-demo-inline-select"
                  placeholder="选择非法类型"
                >
                  <el-option
                    v-for="option in demoIllegalTypeOptions"
                    :key="option.value"
                    :label="option.label"
                    :value="option.value"
                  />
                </el-select>
                <span class="rule-demo-sentence-text">。</span>
              </div>
              <div class="rule-demo-card-footer">
                <div class="rule-demo-card-meta">
                  <el-tag size="small" effect="plain" type="warning">当前页命中 {{ demoRuleMatchCount(rule) }} 条</el-tag>
                  <span class="rule-demo-card-summary">{{ demoRuleSentence(rule) }}</span>
                </div>
                <el-button
                  class="rule-demo-delete"
                  circle
                  :icon="Delete"
                  @click="removeDemoRule(rule.id)"
                />
              </div>
            </article>
          </div>
          <el-empty
            v-else
            description="还没有配置规则，可以先新增一条句式规则。"
          />
        </section>

        <el-empty
          v-if="!ruleExplanation?.supported"
          :description="ruleExplanation?.unsupportedReason || '当前页面暂不支持规则配置说明。'"
        />

        <template v-else>
          <section class="record-detail-section">
            <div class="record-detail-section-title">先看结论</div>
            <div class="record-rule-summary-card">
              <div class="record-rule-summary-main">{{ qaFriendlyRuleSummary }}</div>
              <div v-if="ruleExplanation?.summary" class="record-rule-summary-sub">{{ ruleExplanation.summary }}</div>
            </div>
            <div class="record-rule-overview-grid">
              <article class="record-rule-overview-card">
                <span class="record-rule-overview-label">原始数据</span>
                <strong class="record-rule-overview-value">{{ ruleFirstInputCount }}</strong>
              </article>
              <article class="record-rule-overview-card">
                <span class="record-rule-overview-label">最终命中</span>
                <strong class="record-rule-overview-value">{{ ruleFinalOutputCount }}</strong>
              </article>
              <article class="record-rule-overview-card">
                <span class="record-rule-overview-label">命中比例</span>
                <strong class="record-rule-overview-value">{{ ruleFinalRetainedRate }}</strong>
              </article>
            </div>
          </section>

          <section class="record-detail-section">
            <div class="record-detail-section-title">这次统计包含哪些数据</div>
            <div class="record-detail-content">{{ ruleExplanation?.scopeDescription || '-' }}</div>
          </section>

          <section class="record-detail-section">
            <div class="record-detail-section-title">当前正式口径下，哪些情况会被判定为需要关注</div>
            <div class="record-rule-card-grid">
              <article
                v-for="(step, index) in ruleExclusionSteps"
                :key="step.key"
                class="record-rule-card"
              >
                <div class="record-rule-card-title">规则 {{ index + 1 }}：{{ step.title }}</div>
                <div class="record-rule-card-description">{{ step.description }}</div>
                <div class="record-rule-card-summary">{{ ruleStepSummary(step, index + 1) }}</div>
                <div class="record-rule-card-stats">
                  <span class="record-rule-card-stat">{{ step.key === 'illegal-total' ? '识别' : '命中该类' }} {{ step.outputCount }} 条</span>
                  <span class="record-rule-card-stat">{{ step.key === 'illegal-total' ? '占原始' : '占全部非法' }} {{ ruleStepRetainedRate(step) }}</span>
                  <span class="record-rule-card-stat">检查基数 {{ step.inputCount }} 条</span>
                </div>
              </article>
            </div>
          </section>

          <section class="record-detail-section">
            <div class="record-detail-section-title">数据是怎么一步步变化的</div>
            <div class="record-process-chain">
              <article
                v-for="(step, index) in ruleExplanationSteps"
                :key="`${step.key}-process`"
                class="record-process-card"
              >
                <div class="record-process-step">第 {{ index + 1 }} 步</div>
                <div class="record-process-title">{{ step.title }}</div>
                <div class="record-process-value">{{ step.outputCount }} 条</div>
                <div class="record-process-note">
                  {{ index === 0 ? '这是最开始纳入检查的合并请求。' : `这一轮处理后，共有 ${step.outputCount} 条被标记为需要关注。` }}
                </div>
              </article>
            </div>
          </section>

          <section class="record-detail-section">
            <div class="record-detail-section-title">最后这些数字怎么算</div>
            <div class="record-formula-card-grid">
              <article
                v-for="metric in ruleExplanationMetrics"
                :key="metric.key"
                class="record-formula-card"
              >
                <div class="record-formula-card-title">{{ metric.label }}</div>
                <div class="record-formula-card-definition">{{ metricFormulaSummary(metric) }}</div>
                <div class="record-formula-card-formula">{{ metric.formula }}</div>
                <div v-if="metric.note" class="record-formula-card-note">{{ metric.note }}</div>
              </article>
            </div>
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

.rule-demo-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.rule-demo-header-main {
  display: grid;
  gap: 6px;
}

.rule-demo-kicker {
  font-size: 12px;
  font-weight: 700;
  color: rgba(59, 130, 246, 0.86);
}

.rule-demo-title {
  font-size: 18px;
  font-weight: 700;
  color: rgba(15, 23, 42, 0.92);
}

.rule-demo-note,
.rule-demo-stat {
  font-size: 13px;
  line-height: 1.7;
  color: rgba(15, 23, 42, 0.6);
}

.rule-demo-header-actions {
  display: grid;
  gap: 10px;
  justify-items: end;
}

.rule-demo-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.rule-demo-toolbar-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.rule-demo-list {
  display: grid;
  gap: 10px;
}

.rule-demo-card {
  display: grid;
  gap: 10px;
  padding: 12px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(15, 23, 42, 0.06);
}

.rule-demo-sentence {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.rule-demo-sentence-text {
  font-size: 13px;
  color: rgba(15, 23, 42, 0.72);
}

.rule-demo-inline-select {
  width: 180px;
}

.rule-demo-inline-value {
  width: 220px;
}

.rule-demo-card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.rule-demo-card-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.rule-demo-card-summary {
  font-size: 12px;
  color: rgba(15, 23, 42, 0.56);
}

.rule-demo-delete {
  flex-shrink: 0;
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

.record-rule-summary-card {
  display: grid;
  gap: 8px;
  padding: 16px 18px;
  border-radius: 16px;
  background: linear-gradient(180deg, rgba(239, 246, 255, 0.95) 0%, rgba(248, 250, 252, 0.98) 100%);
  border: 1px solid rgba(59, 130, 246, 0.14);
}

.record-rule-summary-main {
  font-size: 15px;
  font-weight: 700;
  line-height: 1.7;
  color: rgba(15, 23, 42, 0.9);
}

.record-rule-summary-sub {
  font-size: 13px;
  line-height: 1.7;
  color: rgba(15, 23, 42, 0.66);
}

.record-rule-overview-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.record-rule-overview-card,
.record-rule-card,
.record-formula-card,
.record-process-card {
  display: grid;
  gap: 10px;
  padding: 16px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid rgba(15, 23, 42, 0.06);
}

.record-rule-overview-label {
  font-size: 12px;
  color: rgba(15, 23, 42, 0.52);
}

.record-rule-overview-value,
.record-process-value {
  font-size: 22px;
  line-height: 1;
  color: rgba(15, 23, 42, 0.92);
}

.record-rule-card-grid,
.record-formula-card-grid {
  display: grid;
  gap: 12px;
}

.record-rule-card-title,
.record-formula-card-title,
.record-process-title {
  font-size: 14px;
  font-weight: 700;
  color: rgba(15, 23, 42, 0.9);
}

.record-rule-card-description,
.record-formula-card-definition,
.record-formula-card-note,
.record-process-note {
  font-size: 13px;
  line-height: 1.7;
  color: rgba(15, 23, 42, 0.68);
}

.record-rule-card-summary,
.record-formula-card-formula {
  padding: 10px 12px;
  border-radius: 12px;
  background: rgba(248, 250, 252, 0.96);
  border: 1px solid rgba(15, 23, 42, 0.06);
  font-size: 13px;
  line-height: 1.7;
  color: rgba(15, 23, 42, 0.8);
}

.record-rule-card-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.record-rule-card-stat {
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(241, 245, 249, 0.95);
  font-size: 12px;
  color: rgba(15, 23, 42, 0.72);
}

.record-process-chain {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 12px;
}

.record-process-step {
  font-size: 12px;
  color: rgba(15, 23, 42, 0.46);
}


@media (max-width: 960px) {
  .rule-demo-header,
  .rule-demo-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .rule-demo-header-actions {
    justify-items: start;
  }

  .rule-demo-card-footer {
    flex-direction: column;
    align-items: flex-start;
  }

  .record-rule-overview-grid,
  .record-detail-metrics {
    grid-template-columns: 1fr;
  }
}
</style>
