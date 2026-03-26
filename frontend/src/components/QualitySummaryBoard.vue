<script setup lang="ts">
import { ref } from 'vue';
import { ElMessage } from 'element-plus';
import { Download, RefreshRight, Search } from '@element-plus/icons-vue';
import type { DrilldownActiveCell } from '../types/drilldown-table';
import DrilldownMatrixTable from './DrilldownMatrixTable.vue';
import type { DrilldownFilterState } from '../modules/drilldown-table/types';
import { QualitySummaryTableDefinition } from '../modules/quality-board/definition/QualitySummaryTableDefinition';

const definition = new QualitySummaryTableDefinition();
const viewModel = definition.buildViewModel();
const filters = ref<DrilldownFilterState>(definition.resetFilters());

function runQuery() {
  ElMessage.success('已按当前筛选条件刷新汇总统计。');
}

function exportSnapshot() {
  ElMessage.success('已触发导出任务，后续可接真实文件下载接口。');
}

function resetFilters() {
  filters.value = definition.resetFilters();
  ElMessage.info('筛选条件已恢复默认。');
}
</script>

<template>
  <section class="quality-board-shell">
    <el-card shadow="never" class="quality-query-card">
      <div class="quality-query-layout">
        <div class="quality-query-left">
          <div class="quality-chip">{{ viewModel.chip }}</div>
          <div class="quality-title">{{ viewModel.title }}</div>
          <div class="quality-subtitle">{{ viewModel.subtitle }}</div>
        </div>

        <div class="quality-query-right">
          <el-form inline class="quality-inline-form">
            <el-form-item
              v-for="field in viewModel.filterFields"
              :key="field.key"
              :label="field.label"
            >
              <el-select v-model="filters[field.key]" :style="{ width: `${field.width ?? 148}px` }">
                <el-option
                  v-for="option in field.options"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
            </el-form-item>
          </el-form>

          <div class="quality-action-row">
            <el-button type="primary" :icon="Search" @click="runQuery">查询</el-button>
            <el-button :icon="RefreshRight" @click="resetFilters">重置</el-button>
            <el-button :icon="Download" @click="exportSnapshot">导出当前</el-button>
          </div>
        </div>
      </div>

      <div class="quality-meta-strip">
        <div v-for="item in viewModel.stats" :key="item.label" class="quality-meta-item">
          <span class="quality-meta-label">{{ item.label }}</span>
          <span class="quality-meta-value">{{ item.value }}</span>
        </div>
      </div>
    </el-card>

    <DrilldownMatrixTable
      :board-title="viewModel.boardTitle"
      :board-description="viewModel.boardDescription"
      :drawer-description="viewModel.drawerDescription"
      :column-groups="viewModel.columnGroups"
      :rows="viewModel.rows"
      :detail-columns="viewModel.detailColumns"
      :detail-builder="(activeCell: DrilldownActiveCell) => definition.buildDetails(activeCell)"
    >
      <template #drawer-tags="{ activeCell }">
        <el-tag
          v-for="tag in definition.getDrawerTags(filters, activeCell)"
          :key="tag"
          :type="tag === filters.version ? 'primary' : 'info'"
          effect="plain"
        >
          {{ tag }}
        </el-tag>
      </template>
    </DrilldownMatrixTable>
  </section>
</template>

<style scoped>
.quality-board-shell {
  display: grid;
  gap: 22px;
}

.quality-query-card {
  border-radius: 24px !important;
  border: 1px solid rgba(208, 220, 236, 0.92) !important;
  background: rgba(255, 255, 255, 0.94) !important;
  box-shadow: 0 22px 44px rgba(15, 38, 71, 0.08) !important;
}

.quality-query-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 20px;
  align-items: start;
}

.quality-chip {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(31, 111, 255, 0.1);
  color: #1a5ccd;
  font-size: 12px;
  font-weight: 700;
}

.quality-title {
  margin-top: 14px;
  font-size: 30px;
  font-weight: 800;
  letter-spacing: -0.03em;
  color: #0f1f32;
}

.quality-subtitle {
  margin-top: 10px;
  max-width: 720px;
  color: #60748b;
  font-size: 14px;
  line-height: 1.75;
}

.quality-query-right {
  min-width: 520px;
  display: grid;
  gap: 14px;
  justify-items: end;
}

.quality-inline-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.quality-action-row {
  display: flex;
  gap: 10px;
}

.quality-meta-strip {
  margin-top: 18px;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.quality-meta-item {
  padding: 14px 16px;
  border-radius: 16px;
  border: 1px solid rgba(225, 233, 242, 0.96);
  background: linear-gradient(180deg, rgba(249, 252, 255, 0.96), rgba(244, 248, 253, 0.92));
}

.quality-meta-label {
  display: block;
  color: #7a8ca1;
  font-size: 12px;
}

.quality-meta-value {
  display: block;
  margin-top: 8px;
  color: #102033;
  font-size: 14px;
  font-weight: 700;
}

@media (max-width: 1480px) {
  .quality-query-layout {
    grid-template-columns: 1fr;
  }

  .quality-query-right {
    min-width: 0;
    justify-items: stretch;
  }

  .quality-meta-strip {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
