<script setup lang="ts">
import { computed } from 'vue';
import BaseRecordTable from '../components/base/BaseRecordTable.vue';
import { useRouteTableState } from '../composables/useRouteTableState';
import type { RecordTableColumn } from '../types/record-table';

const { page, pageSize, sortBy, sortOrder, keyword, patchQuery } = useRouteTableState({
  defaults: {
    page: 1,
    pageSize: 20,
    sortBy: 'mergedAt',
    sortOrder: 'desc',
    keyword: '',
  },
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

async function handleSearch(nextKeyword: string) {
  await patchQuery({
    keyword: nextKeyword,
    page: 1,
  });
}

async function handleReset() {
  await patchQuery({
    keyword: '',
    page: 1,
    sortBy: 'mergedAt',
    sortOrder: 'desc',
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
    <el-card shadow="never" class="record-page-intro">
      <div class="record-page-intro-title">代码走查非法记录</div>
      <div class="record-page-intro-desc">
        当前本地库还没有这张业务表和对应明细数据，所以这一版先只落真实表头和空表结构，
        不接假数据、不做 mock。后续接入真实镜像字段、非法判定规则和外部指标时，直接复用这套记录表抽象即可。
      </div>
    </el-card>

    <BaseRecordTable
      :columns="columns"
      :rows="rows"
      :loading="false"
      :keyword="keyword"
      :page="page"
      :page-size="pageSize"
      :total="total"
      search-placeholder="请输入合并请求编号、内容、责任人、项目或非法类型"
      empty-description="当前尚未接入真实非法记录数据，先保留真实表头和空表结构。"
      @search="handleSearch"
      @reset="handleReset"
      @refresh="handleRefresh"
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
      @sort-change="handleSortChange"
    >
      <template #toolbar-prefix>
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

.record-page-intro {
  border-radius: 12px;
}

.record-page-intro-title {
  font-size: 16px;
  font-weight: 700;
  color: rgba(0, 0, 0, 0.88);
}

.record-page-intro-desc {
  margin-top: 8px;
  color: rgba(0, 0, 0, 0.6);
  line-height: 1.7;
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
