<script setup lang="ts">
import { computed, ref } from 'vue';
import BaseRecordTable from '../components/base/BaseRecordTable.vue';
import { useRouteTableState } from '../composables/useRouteTableState';
import type { RecordTableActiveFilterTag, RecordTableColumn, RecordTableFilterField } from '../types/record-table';

const { route, page, pageSize, sortBy, sortOrder, patchQuery } = useRouteTableState({
  defaults: {
    page: 1,
    pageSize: 20,
    sortBy: 'mergedAt',
    sortOrder: 'desc',
  },
});

const advancedVisible = ref(false);

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
    options: [{ label: '全部代码库', value: '' }],
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
    options: [{ label: '全部非法类型', value: '' }],
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
    options: [
      { label: '全部请求类型', value: '' },
      { label: '合并请求', value: 'merge_request' },
      { label: '议题', value: 'issue' },
    ],
  },
  { key: 'mergeRequestIid', label: '合并请求编号', type: 'input', placeholder: '合并请求编号' },
  { key: 'owner', label: '被挂责任人', type: 'input', placeholder: '被挂责任人' },
  {
    key: 'targetBranch',
    label: '目标分支',
    type: 'select',
    options: [{ label: '全部目标分支', value: '' }],
  },
  {
    key: 'mergedBy',
    label: '合并人',
    type: 'select',
    options: [{ label: '全部合并人', value: '' }],
  },
  {
    key: 'moduleName',
    label: '模块名称',
    type: 'select',
    options: [{ label: '全部模块名称', value: '' }],
  },
  {
    key: 'projectName',
    label: '项目名称',
    type: 'select',
    options: [{ label: '全部项目名称', value: '' }],
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
  if (values.owner) tags.push({ key: 'owner', label: '被挂责任人', value: String(values.owner) });
  if (values.targetBranch) tags.push({ key: 'targetBranch', label: '目标分支', value: String(values.targetBranch) });
  if (values.mergedBy) tags.push({ key: 'mergedBy', label: '合并人', value: String(values.mergedBy) });
  if (values.moduleName) tags.push({ key: 'moduleName', label: '模块名称', value: String(values.moduleName) });
  if (values.projectName) tags.push({ key: 'projectName', label: '项目名称', value: String(values.projectName) });
  return tags;
});

const columns = computed<RecordTableColumn[]>(() => [
  { key: 'mergeRequestIid', label: '合并请求编号', type: 'number', sortable: true, width: 128, fixed: 'left' },
  { key: 'mergeRequestContent', label: '合并请求内容', type: 'link', sortable: true, minWidth: 260 },
  { key: 'owner', label: '标注责任人', sortable: true, minWidth: 140 },
  { key: 'projectName', label: '所属项目', sortable: true, minWidth: 160 },
  { key: 'mergedAt', label: '合并时间', type: 'datetime', sortable: true, minWidth: 180 },
  { key: 'mergedBy', label: '合并人', sortable: true, minWidth: 140 },
  { key: 'moduleName', label: '模块名', sortable: true, minWidth: 140 },
  { key: 'targetBranch', label: '合并目标分支', sortable: true, minWidth: 180 },
  { key: 'illegalType', label: '非法类型', type: 'tags', minWidth: 220 },
  { key: 'commentRate', label: '代码注释比例(%)', sortable: true, width: 160, align: 'right' },
  { key: 'defectCount', label: '缺陷数量', type: 'number', sortable: true, width: 120, align: 'right' },
  { key: 'addedLines', label: '新增代码行数(行)', type: 'number', sortable: true, width: 150, align: 'right' },
]);

const rows = computed<Record<string, unknown>[]>(() => []);
const total = computed(() => 0);

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

async function handleRefresh() {
  await patchQuery({});
}

async function handleQuery() {
  await patchQuery({ page: 1 });
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
    sortBy: payload.prop || 'mergedAt',
    sortOrder: payload.order === 'ascending' ? 'asc' : payload.order === 'descending' ? 'desc' : 'desc',
    page: 1,
  });
}

async function handleClearFilter(key: string) {
  if (key === 'mergedAtRange') {
    await patchQuery({
      page: 1,
      mergedAtStart: null,
      mergedAtEnd: null,
    });
    return;
  }

  await patchQuery({
    page: 1,
    [key]: null,
  });
}
</script>

<template>
  <section class="record-page-shell">
    <BaseRecordTable
      :columns="columns"
      :rows="rows"
      :loading="false"
      :page="page"
      :page-size="pageSize"
      :total="total"
      :primary-filters="primaryFilters"
      :advanced-filters="advancedFilters"
      :filter-values="filterValues"
      :active-filter-tags="activeFilterTags"
      :advanced-visible="advancedVisible"
      empty-description="当前尚未接入真实非法记录数据，先保留真实表头和空表结构。"
      :show-search="false"
      @filter-change="handleFilterChange"
      @reset="handleReset"
      @refresh="handleRefresh"
      @query="handleQuery"
      @clear-filter="handleClearFilter"
      @update:advanced-visible="advancedVisible = $event"
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
      @sort-change="handleSortChange"
    >
      <template #toolbar-actions>
        <div class="record-page-summary">
          <span class="record-page-summary-label">当前排序</span>
          <el-tag effect="plain">{{ sortBy || 'mergedAt' }} / {{ sortOrder || 'desc' }}</el-tag>
        </div>
      </template>
    </BaseRecordTable>
  </section>
</template>

<style scoped>
.record-page-shell {
  display: grid;
  gap: 12px;
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
</style>
