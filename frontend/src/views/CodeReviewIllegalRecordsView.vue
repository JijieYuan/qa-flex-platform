<script setup lang="ts">
import { computed } from 'vue';
import BaseRecordTable from '../components/base/BaseRecordTable.vue';
import { useRouteTableState } from '../composables/useRouteTableState';
import type { RecordTableColumn } from '../types/record-table';

const { route, page, pageSize, sortBy, sortOrder, patchQuery } = useRouteTableState({
  defaults: {
    page: 1,
    pageSize: 20,
    sortBy: 'mergedAt',
    sortOrder: 'desc',
  },
});

const projectName = computed(() => String(route.query.projectName ?? ''));
const requestType = computed(() => String(route.query.requestType ?? ''));
const targetBranch = computed(() => String(route.query.targetBranch ?? ''));
const mergedBy = computed(() => String(route.query.mergedBy ?? ''));
const moduleName = computed(() => String(route.query.moduleName ?? ''));
const illegalType = computed(() => String(route.query.illegalType ?? ''));
const mergeRequestIid = computed(() => String(route.query.mergeRequestIid ?? ''));
const owner = computed(() => String(route.query.owner ?? ''));

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

const requestTypeOptions = [
  { label: '全部请求类型', value: '' },
  { label: '合并请求', value: 'merge_request' },
  { label: '议题', value: 'issue' },
] as const;

const targetBranchOptions = [
  { label: '全部目标分支', value: '' },
] as const;

const mergedByOptions = [
  { label: '全部合并人', value: '' },
] as const;

const moduleNameOptions = [
  { label: '全部模块名', value: '' },
] as const;

const illegalTypeOptions = [
  { label: '全部非法类型', value: '' },
] as const;

const projectNameOptions = [
  { label: '全部项目名称', value: '' },
] as const;

async function handleFilterChange(patch: Record<string, string>) {
  await patchQuery({
    page: 1,
    ...patch,
  });
}

async function handleReset() {
  await patchQuery({
    page: 1,
    sortBy: 'mergedAt',
    sortOrder: 'desc',
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
      empty-description="当前尚未接入真实非法记录数据，先保留真实表头和空表结构。"
      :show-search="false"
      @reset="handleReset"
      @refresh="handleRefresh"
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
      @sort-change="handleSortChange"
    >
      <template #toolbar-prefix>
        <div class="record-page-filters">
          <el-select
            :model-value="requestType"
            class="record-filter-select record-filter-select-wide"
            placeholder="请求类型"
            @change="handleFilterChange({ requestType: String($event ?? '') })"
          >
            <el-option v-for="option in requestTypeOptions" :key="option.value" :label="option.label" :value="option.value" />
          </el-select>

          <el-input
            :model-value="mergeRequestIid"
            class="record-filter-input"
            placeholder="合并请求编号"
            clearable
            @change="handleFilterChange({ mergeRequestIid: String($event ?? '') })"
            @clear="handleFilterChange({ mergeRequestIid: '' })"
          />

          <el-input
            :model-value="owner"
            class="record-filter-input"
            placeholder="被挂责任人"
            clearable
            @change="handleFilterChange({ owner: String($event ?? '') })"
            @clear="handleFilterChange({ owner: '' })"
          />

          <el-select
            :model-value="targetBranch"
            class="record-filter-select"
            placeholder="目标分支"
            @change="handleFilterChange({ targetBranch: String($event ?? '') })"
          >
            <el-option v-for="option in targetBranchOptions" :key="option.value" :label="option.label" :value="option.value" />
          </el-select>

          <el-select
            :model-value="mergedBy"
            class="record-filter-select"
            placeholder="合并人"
            @change="handleFilterChange({ mergedBy: String($event ?? '') })"
          >
            <el-option v-for="option in mergedByOptions" :key="option.value" :label="option.label" :value="option.value" />
          </el-select>

          <el-select
            :model-value="moduleName"
            class="record-filter-select"
            placeholder="模块名称"
            @change="handleFilterChange({ moduleName: String($event ?? '') })"
          >
            <el-option v-for="option in moduleNameOptions" :key="option.value" :label="option.label" :value="option.value" />
          </el-select>

          <el-select
            :model-value="illegalType"
            class="record-filter-select"
            placeholder="非法类型"
            @change="handleFilterChange({ illegalType: String($event ?? '') })"
          >
            <el-option v-for="option in illegalTypeOptions" :key="option.value" :label="option.label" :value="option.value" />
          </el-select>

          <el-select
            :model-value="projectName"
            class="record-filter-select"
            placeholder="项目名称"
            @change="handleFilterChange({ projectName: String($event ?? '') })"
          >
            <el-option v-for="option in projectNameOptions" :key="option.value" :label="option.label" :value="option.value" />
          </el-select>
        </div>
      </template>

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

.record-page-filters {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  flex: 1 1 auto;
}

.record-filter-select,
.record-filter-input {
  width: 168px;
}

.record-filter-select-wide {
  width: 180px;
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
