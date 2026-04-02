<script setup lang="ts">
import { Refresh, Search } from '@element-plus/icons-vue';
import { computed, ref, useSlots, watch } from 'vue';
import type { RecordTableColumn, RecordTableLinkValue, RecordTableTagValue } from '../../types/record-table';

const props = withDefaults(
  defineProps<{
    columns: RecordTableColumn[];
    rows: Record<string, unknown>[];
    loading?: boolean;
    keyword?: string;
    page: number;
    pageSize: number;
    total: number;
    pageSizeOptions?: number[];
    searchPlaceholder?: string;
    emptyDescription?: string;
    showSearch?: boolean;
    showRefresh?: boolean;
  }>(),
  {
    loading: false,
    keyword: '',
    pageSizeOptions: () => [10, 20, 50, 100],
    searchPlaceholder: '请输入关键字搜索',
    emptyDescription: '当前暂无可展示记录',
    showSearch: true,
    showRefresh: true,
  },
);

const emit = defineEmits<{
  (event: 'search', keyword: string): void;
  (event: 'reset'): void;
  (event: 'refresh'): void;
  (event: 'size-change', size: number): void;
  (event: 'current-change', page: number): void;
  (event: 'sort-change', payload: { prop: string; order: 'ascending' | 'descending' | null }): void;
}>();

const keywordDraft = ref(props.keyword);

watch(
  () => props.keyword,
  (value) => {
    keywordDraft.value = value;
  },
);

const slots = useSlots();
const hasRowActions = computed(() => Boolean(slots['row-actions']));

function normalizeTagList(value: unknown): RecordTableTagValue[] {
  if (!Array.isArray(value)) {
    return [];
  }
  return value
    .map((item) => {
      if (!item) {
        return null;
      }
      if (typeof item === 'string') {
        return { label: item } satisfies RecordTableTagValue;
      }
      if (typeof item === 'object' && 'label' in item) {
        const record = item as Record<string, unknown>;
        return {
          label: String(record.label ?? ''),
          type: typeof record.type === 'string' ? (record.type as RecordTableTagValue['type']) : undefined,
        } satisfies RecordTableTagValue;
      }
      return null;
    })
    .filter((item): item is RecordTableTagValue => Boolean(item?.label));
}

function normalizeLink(value: unknown): RecordTableLinkValue | null {
  if (!value) {
    return null;
  }
  if (typeof value === 'object' && 'href' in value) {
    const record = value as Record<string, unknown>;
    const href = String(record.href ?? '').trim();
    if (!href) {
      return null;
    }
    return {
      href,
      label: String(record.label ?? href),
    };
  }
  const href = String(value).trim();
  if (!href) {
    return null;
  }
  return {
    href,
    label: href,
  };
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

function handleSearch() {
  emit('search', keywordDraft.value.trim());
}

function handleReset() {
  keywordDraft.value = '';
  emit('reset');
}
</script>

<template>
  <div class="record-table-card">
    <div class="record-table-toolbar">
      <div class="record-table-toolbar-main">
        <slot name="toolbar-prefix" />
        <el-input
          v-if="showSearch"
          v-model="keywordDraft"
          class="record-table-search"
          :placeholder="searchPlaceholder"
          clearable
          @keyup.enter="handleSearch"
          @clear="handleReset"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </div>

      <div class="record-table-toolbar-actions">
        <slot name="toolbar-actions" />
        <el-button v-if="showSearch" type="primary" @click="handleSearch">搜索</el-button>
        <el-button v-if="showSearch" @click="handleReset">重置</el-button>
        <el-button v-if="showRefresh" :icon="Refresh" @click="emit('refresh')">刷新</el-button>
      </div>
    </div>

    <el-table
      v-loading="loading"
      :data="rows"
      border
      stripe
      class="record-table"
      @sort-change="emit('sort-change', $event)"
    >
      <el-table-column
        v-for="column in columns"
        :key="column.key"
        :prop="column.key"
        :label="column.label"
        :sortable="column.sortable ? 'custom' : false"
        :width="column.width"
        :min-width="column.minWidth"
        :fixed="column.fixed"
        :align="column.align ?? 'left'"
        :header-align="column.headerAlign ?? 'center'"
        :show-overflow-tooltip="column.showOverflowTooltip ?? true"
      >
        <template #default="{ row }">
          <slot
            v-if="$slots[`cell-${column.key}`]"
            :name="`cell-${column.key}`"
            :row="row"
            :value="row[column.key]"
          />
          <template v-else-if="column.type === 'tags'">
            <div class="record-table-tags">
              <el-tag
                v-for="tag in normalizeTagList(row[column.key])"
                :key="`${column.key}-${tag.label}`"
                size="small"
                :type="tag.type ?? 'info'"
                effect="plain"
              >
                {{ tag.label }}
              </el-tag>
              <span v-if="!normalizeTagList(row[column.key]).length" class="record-table-empty">-</span>
            </div>
          </template>
          <template v-else-if="column.type === 'tag'">
            <template v-if="normalizeTagList(row[column.key])[0]">
              <el-tag
                size="small"
                :type="normalizeTagList(row[column.key])[0].type ?? 'info'"
                effect="plain"
              >
                {{ normalizeTagList(row[column.key])[0].label }}
              </el-tag>
            </template>
            <span v-else class="record-table-empty">-</span>
          </template>
          <template v-else-if="column.type === 'link'">
            <a
              v-if="normalizeLink(row[column.key])"
              class="record-table-link"
              :href="normalizeLink(row[column.key])!.href"
              target="_blank"
              rel="noreferrer"
            >
              {{ normalizeLink(row[column.key])!.label }}
            </a>
            <span v-else class="record-table-empty">-</span>
          </template>
          <span v-else class="record-table-text">{{ formatCellValue(row[column.key]) }}</span>
        </template>
      </el-table-column>

      <el-table-column v-if="hasRowActions" label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <slot name="row-actions" :row="row" />
        </template>
      </el-table-column>

      <template #empty>
        <el-empty :description="emptyDescription" />
      </template>
    </el-table>

    <div class="record-table-pagination">
      <el-pagination
        background
        layout="total, sizes, prev, pager, next"
        :current-page="page"
        :page-size="pageSize"
        :page-sizes="pageSizeOptions"
        :total="total"
        @size-change="emit('size-change', $event)"
        @current-change="emit('current-change', $event)"
      />
    </div>
  </div>
</template>

<style scoped>
.record-table-card {
  display: grid;
  gap: 12px;
}

.record-table-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.record-table-toolbar-main {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1 1 560px;
  flex-wrap: wrap;
}

.record-table-search {
  width: 320px;
}

.record-table-toolbar-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.record-table {
  width: 100%;
}

.record-table-pagination {
  display: flex;
  justify-content: flex-end;
}

.record-table-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.record-table-link {
  color: #2563eb;
  text-decoration: none;
}

.record-table-link:hover {
  text-decoration: underline;
}

.record-table-text,
.record-table-empty {
  color: rgba(0, 0, 0, 0.88);
}

.record-table-empty {
  color: rgba(0, 0, 0, 0.45);
}
</style>
