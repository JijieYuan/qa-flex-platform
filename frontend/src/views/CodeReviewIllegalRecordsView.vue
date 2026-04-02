<script setup lang="ts">
import { computed, ref } from 'vue';
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

const advancedVisible = ref(false);
const repositoryName = computed(() => String(route.query.repositoryName ?? ''));
const mergedAtRange = computed<[string, string] | []>(() => {
  const start = String(route.query.mergedAtStart ?? '');
  const end = String(route.query.mergedAtEnd ?? '');
  return start && end ? [start, end] : [];
});
const keyword = computed(() => String(route.query.keyword ?? ''));
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

const repositoryNameOptions = [
  { label: '全部代码库', value: '' },
] as const;

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

async function handleDateRangeChange(range: string[] | null) {
  const [start, end] = Array.isArray(range) ? range : [];
  await patchQuery({
    page: 1,
    mergedAtStart: start || null,
    mergedAtEnd: end || null,
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

const activeFilterTags = computed(() => {
  const tags: Array<{ key: string; label: string; value: string }> = [];
  if (repositoryName.value) tags.push({ key: 'repositoryName', label: '代码库', value: repositoryName.value });
  if (mergedAtRange.value.length === 2) {
    tags.push({ key: 'mergedAtRange', label: '合并时间', value: `${mergedAtRange.value[0]} ~ ${mergedAtRange.value[1]}` });
  }
  if (illegalType.value) tags.push({ key: 'illegalType', label: '非法类型', value: illegalType.value });
  if (keyword.value) tags.push({ key: 'keyword', label: '关键字', value: keyword.value });
  if (requestType.value) tags.push({ key: 'requestType', label: '请求类型', value: requestType.value });
  if (mergeRequestIid.value) tags.push({ key: 'mergeRequestIid', label: '合并请求编号', value: mergeRequestIid.value });
  if (owner.value) tags.push({ key: 'owner', label: '被挂责任人', value: owner.value });
  if (targetBranch.value) tags.push({ key: 'targetBranch', label: '目标分支', value: targetBranch.value });
  if (mergedBy.value) tags.push({ key: 'mergedBy', label: '合并人', value: mergedBy.value });
  if (moduleName.value) tags.push({ key: 'moduleName', label: '模块名称', value: moduleName.value });
  if (projectName.value) tags.push({ key: 'projectName', label: '项目名称', value: projectName.value });
  return tags;
});

async function clearFilterTag(key: string) {
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
    <el-card shadow="never" class="record-filter-card">
      <div class="record-filter-primary">
        <el-select
          :model-value="repositoryName"
          class="record-filter-main-select"
          placeholder="代码库"
          @change="handleFilterChange({ repositoryName: String($event ?? '') })"
        >
          <el-option
            v-for="option in repositoryNameOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>

        <el-date-picker
          :model-value="mergedAtRange"
          class="record-filter-main-date"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          @change="handleDateRangeChange"
        />

        <el-select
          :model-value="illegalType"
          class="record-filter-main-select"
          placeholder="非法类型"
          @change="handleFilterChange({ illegalType: String($event ?? '') })"
        >
          <el-option v-for="option in illegalTypeOptions" :key="option.value" :label="option.label" :value="option.value" />
        </el-select>

        <el-input
          :model-value="keyword"
          class="record-filter-main-keyword"
          placeholder="搜索合并请求内容、责任人或项目"
          clearable
          @change="handleFilterChange({ keyword: String($event ?? '') })"
          @clear="handleFilterChange({ keyword: '' })"
        />

        <div class="record-filter-primary-actions">
          <el-button @click="advancedVisible = !advancedVisible">
            {{ advancedVisible ? '收起高级筛选' : '高级筛选' }}
          </el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="primary" @click="handleRefresh">查询</el-button>
        </div>
      </div>

      <el-collapse-transition>
        <div v-show="advancedVisible" class="record-filter-advanced">
          <el-select
            :model-value="requestType"
            class="record-filter-select"
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
            :model-value="projectName"
            class="record-filter-select"
            placeholder="项目名称"
            @change="handleFilterChange({ projectName: String($event ?? '') })"
          >
            <el-option v-for="option in projectNameOptions" :key="option.value" :label="option.label" :value="option.value" />
          </el-select>
        </div>
      </el-collapse-transition>
    </el-card>

    <div v-if="activeFilterTags.length" class="record-filter-tags">
      <span class="record-filter-tags-label">已选条件</span>
      <el-tag
        v-for="tag in activeFilterTags"
        :key="tag.key"
        closable
        effect="plain"
        @close="clearFilterTag(tag.key)"
      >
        {{ tag.label }}：{{ tag.value }}
      </el-tag>
    </div>

    <BaseRecordTable
      :columns="columns"
      :rows="rows"
      :loading="false"
      :page="page"
      :page-size="pageSize"
      :total="total"
      empty-description="当前尚未接入真实非法记录数据，先保留真实表头和空表结构。"
      :show-search="false"
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

.record-filter-card {
  border-radius: 12px;
}

.record-filter-primary {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.record-filter-primary-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: auto;
  flex-wrap: wrap;
}

.record-filter-advanced {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
}

.record-filter-main-select {
  width: 180px;
}

.record-filter-main-date {
  width: 280px;
}

.record-filter-main-keyword {
  width: 260px;
}

.record-filter-tags {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  padding: 0 2px;
}

.record-filter-tags-label {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
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
