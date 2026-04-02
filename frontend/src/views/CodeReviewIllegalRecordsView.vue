<script setup lang="ts">
import { computed } from 'vue';
import BaseRecordTable from '../components/base/BaseRecordTable.vue';
import { useRouteTableState } from '../composables/useRouteTableState';
import type { RecordTableColumn } from '../types/record-table';

const { page, pageSize, sortBy, sortOrder, keyword, patchQuery } = useRouteTableState({
  defaults: {
    page: 1,
    pageSize: 20,
    sortBy: 'updatedAt',
    sortOrder: 'desc',
    keyword: '',
  },
});

const columns = computed<RecordTableColumn[]>(() => [
  { key: 'requestIid', label: '请求类型 IID', type: 'number', sortable: true, width: 136, fixed: 'left' },
  { key: 'title', label: '标题', type: 'link', sortable: true, minWidth: 260 },
  { key: 'author', label: '作者', sortable: true, minWidth: 140 },
  { key: 'reviewer', label: '审查人', sortable: true, minWidth: 140 },
  { key: 'mergeState', label: '合入状态', type: 'tag', sortable: true, minWidth: 120, align: 'center' },
  { key: 'labelCount', label: '标签数', type: 'number', sortable: true, width: 100, align: 'center' },
  { key: 'illegalReasons', label: '非法原因', type: 'tags', minWidth: 260 },
  { key: 'updatedAt', label: '最近更新时间', type: 'datetime', sortable: true, minWidth: 180 },
  { key: 'gitlabLink', label: 'GitLab 链接', type: 'link', minWidth: 220 },
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
    sortBy: 'updatedAt',
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
    sortBy: payload.prop || 'updatedAt',
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
        当前页面先完成记录表抽象基座和非法记录明细表头结构，不接假数据、不做临时 mock。
        后续接入真实 GitLab 数据、非法判定规则和外部指标时，直接在这套记录表基座上扩展即可。
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
      search-placeholder="请输入 IID、标题、作者或非法原因"
      empty-description="当前尚未接入真实非法记录数据，先保留记录表结构和表头。"
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
          <el-tag effect="plain">{{ sortBy || 'updatedAt' }} / {{ sortOrder || 'desc' }}</el-tag>
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
