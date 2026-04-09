<script setup lang="ts">
import type { Component } from 'vue';
import { ArrowRight } from '@element-plus/icons-vue';
import StatisticTableColumnGroup from './StatisticTableColumnGroup.vue';
import type {
  StatisticBoardResponse,
  StatisticCellData,
  StatisticColumnGroup,
  StatisticColumnLeaf,
  StatisticRowData,
} from '../../api';
import { flattenStatisticColumnLeavesFromGroup } from '../../api';
import type { StatisticBoardUiHooks } from '../statistic-board-ui';
import { ROW_LABEL_SORT_KEY, type SortDirection } from '../statistic-board-sorting';

const props = withDefaults(
  defineProps<{
    board: StatisticBoardResponse | null;
    uiHooks?: StatisticBoardUiHooks;
    tableRenderKey: string;
    paginatedRows: StatisticRowData[];
    sortedRowsLength: number;
    totalTableRows: number;
    rowHeaderLabel: string;
    orderedColumnGroups: StatisticColumnGroup[];
    currentSortSummary: string;
    firstColumnWidth: number;
    firstColumnMinWidth: number;
    pageSizeOptions?: number[];
    tableCurrentPage: number;
    tablePageSize: number;
    settingsVisible: boolean;
    widthStrategy: 'compact' | 'header' | 'content';
    currentVisibleColumnCount: number;
    allColumnsSelected: boolean;
    partiallySelectedColumns: boolean;
    draftVisibleColumnKeysCount: number;
    allColumnKeysCount: number;
    expandedViewSettingGroups: string[];
    onExpandedViewSettingGroupsChange: (value: string[]) => void;
    groupCheckAllStates: Record<string, boolean>;
    groupIndeterminateStates: Record<string, boolean>;
    sortDirectionForColumn: (columnKey: string) => SortDirection;
    sortStateLabel: (direction: SortDirection) => string;
    sortIconForDirection: (direction: SortDirection) => Component;
    toggleColumnSort: (columnKey: string) => void;
    cellForColumn: (row: StatisticRowData, columnKey: string) => StatisticCellData | undefined;
    openDetail: (row: StatisticRowData, cell: StatisticCellData) => void | Promise<void>;
    columnMinWidth: (column: StatisticColumnLeaf) => number;
    columnResizable: (column: StatisticColumnLeaf) => boolean;
    isGroupDragging: (groupKey: string) => boolean;
    onGroupDragStart: (groupKey: string) => void;
    onGroupDrop: (groupKey: string) => void;
    isColumnDragging: (groupKey: string, columnKey: string) => boolean;
    onColumnDragStart: (groupKey: string, columnKey: string) => void;
    onColumnDrop: (groupKey: string, columnKey: string) => void;
    clearDragState: () => void;
    handleTableCurrentChange: (nextPage: number) => void;
    handleTableSizeChange: (nextSize: number) => void;
    onSettingsVisibleChange: (visible: boolean) => void;
    onWidthStrategyChange: (value: 'compact' | 'header' | 'content') => void;
    onSaveViewPrefs: () => void;
    onRestoreDefaultView: () => void;
    toggleAllColumns: (checked: boolean | string | number) => void;
    toggleGroupColumns: (group: StatisticColumnGroup, checked: boolean | string | number) => void;
    isColumnSelected: (columnKey: string) => boolean;
    toggleColumnSelection: (columnKey: string, checked: boolean | string | number) => void;
  }>(),
  {
    uiHooks: () => ({}),
    pageSizeOptions: () => [20, 50, 100, 200],
  },
);
</script>

<template>
  <div v-if="currentSortSummary" class="stat-board-sortbar">
    <span class="stat-board-sortbar-label">褰撳墠鎺掑簭</span>
    <el-tag size="small" type="primary" effect="plain">{{ currentSortSummary }}</el-tag>
  </div>

  <div v-if="board" class="stat-matrix-wrapper">
    <el-table
      :key="tableRenderKey"
      :data="paginatedRows"
      border
      stripe
      fit
      class="base-stat-table stat-matrix-table"
      :class="props.uiHooks.tableClass"
      style="width: 100%"
    >
      <el-table-column
        prop="rowLabel"
        :label="rowHeaderLabel"
        fixed="left"
        :width="firstColumnWidth"
        :min-width="firstColumnMinWidth"
        :resizable="true"
      >
        <template #header>
          <div
            class="stat-column-header first-column"
            :class="{ sorting: sortDirectionForColumn(ROW_LABEL_SORT_KEY) !== 'default' }"
          >
            <span class="stat-column-header-label" :title="rowHeaderLabel">{{ rowHeaderLabel }}</span>
            <span class="stat-header-zone stat-header-zone-right">
              <button
                class="sort-trigger"
                :class="`is-${sortDirectionForColumn(ROW_LABEL_SORT_KEY)}`"
                type="button"
                :title="sortStateLabel(sortDirectionForColumn(ROW_LABEL_SORT_KEY))"
                @click.stop="toggleColumnSort(ROW_LABEL_SORT_KEY)"
              >
                <el-icon class="sort-trigger-icon">
                  <component :is="sortIconForDirection(sortDirectionForColumn(ROW_LABEL_SORT_KEY))" />
                </el-icon>
                <span class="sort-trigger-state">
                  {{ sortDirectionForColumn(ROW_LABEL_SORT_KEY) === 'asc' ? '鍗囧簭' : sortDirectionForColumn(ROW_LABEL_SORT_KEY) === 'desc' ? '闄嶅簭' : '鎺掑簭' }}
                </span>
              </button>
            </span>
          </div>
        </template>

        <template #default="{ row }">
          <span class="stat-row-label">{{ row.rowLabel }}</span>
        </template>
      </el-table-column>

      <el-table-column v-for="group in orderedColumnGroups" :key="group.key" align="center">
        <template #header>
          <div
            class="stat-group-header"
            :class="{ dragging: isGroupDragging(group.key) }"
            draggable="true"
            @dragstart="onGroupDragStart(group.key)"
            @dragover.prevent
            @drop.prevent="onGroupDrop(group.key)"
            @dragend="clearDragState"
          >
            <span class="stat-header-zone stat-header-zone-left" aria-hidden="true">
              <span class="drag-handle group">
                <span></span>
                <span></span>
                <span></span>
                <span></span>
                <span></span>
                <span></span>
              </span>
            </span>
            <span class="stat-group-header-label" :title="group.label">{{ group.label }}</span>
            <span class="stat-header-zone stat-header-zone-right stat-header-zone-placeholder" aria-hidden="true"></span>
          </div>
        </template>

        <StatisticTableColumnGroup
          v-for="child in group.children ?? []"
          :key="child.key"
          :group="child"
          :root-group-key="group.key"
          :sort-direction-for-column="sortDirectionForColumn"
          :sort-state-label="sortStateLabel"
          :sort-icon-for-direction="sortIconForDirection"
          :toggle-column-sort="toggleColumnSort"
          :cell-for-column="cellForColumn"
          :open-detail="openDetail"
          :column-min-width="columnMinWidth"
          :column-resizable="columnResizable"
          :is-column-dragging="isColumnDragging"
          :on-column-drag-start="onColumnDragStart"
          :on-column-drop="onColumnDrop"
          :clear-drag-state="clearDragState"
        />
      </el-table-column>
    </el-table>
  </div>

  <div v-if="board && sortedRowsLength" class="stat-board-pagination">
    <el-pagination
      background
      layout="total, sizes, prev, pager, next, jumper"
      :current-page="tableCurrentPage"
      :page-size="tablePageSize"
      :page-sizes="pageSizeOptions"
      :total="totalTableRows"
      @size-change="handleTableSizeChange"
      @current-change="handleTableCurrentChange"
    />
  </div>

  <el-empty
    v-if="board && !sortedRowsLength"
    :description="board?.definition.emptyText || '褰撳墠绛涢€夋潯浠朵笅娌℃湁鍙睍绀虹殑缁熻缁撴灉銆?'"
    class="stat-empty"
  />

  <el-drawer
    :model-value="settingsVisible"
    title="琛ㄦ牸瑙嗗浘璁剧疆"
    size="360px"
    append-to-body
    class="view-settings-drawer"
    @update:model-value="onSettingsVisibleChange"
  >
    <div v-if="board" class="view-settings-panel" :class="props.uiHooks.settingsPanelClass">
      <div class="view-settings-summary">
        <div class="view-settings-summary-title">列显示控制</div>
        <div class="view-settings-summary-text">当前已选择 {{ currentVisibleColumnCount }} 列，可按需调整当前页面的展示视图。</div>
      </div>

      <div class="view-settings-strategy">
        <div class="view-settings-group-title">鍒楀灞曠ず绛栫暐</div>
        <el-radio-group
          :model-value="widthStrategy"
          class="width-strategy-group"
          @update:model-value="(value) => onWidthStrategyChange(value as 'compact' | 'header' | 'content')"
        >
          <el-radio-button value="compact">统一紧凑</el-radio-button>
          <el-radio-button value="header">按表头宽度</el-radio-button>
          <el-radio-button value="content">按内容宽度</el-radio-button>
        </el-radio-group>
        <div class="view-settings-strategy-tip">首列继续单独压缩处理，纯数字统计列保持紧凑，宽度策略可在紧凑、表头优先和内容优先之间切换。</div>
      </div>

      <div class="view-settings-scroll">
        <div class="view-settings-global-toggle">
          <el-checkbox
            :model-value="allColumnsSelected"
            :indeterminate="partiallySelectedColumns"
            @update:model-value="toggleAllColumns"
          >
            鍕鹃€夊叏閮ㄥ垪
          </el-checkbox>
          <span class="view-settings-global-meta">{{ draftVisibleColumnKeysCount }}/{{ allColumnKeysCount }}</span>
        </div>

        <div class="view-settings-checklist">
          <el-collapse
            :model-value="expandedViewSettingGroups"
            class="view-settings-collapse"
            @update:model-value="(value) => onExpandedViewSettingGroupsChange(value as string[])"
          >
            <el-collapse-item
              v-for="group in board.definition.columnGroups"
              :key="group.key"
              :name="group.key"
              class="view-settings-group"
            >
              <template #title>
                <div class="view-settings-group-head">
                  <span class="view-settings-group-title">{{ group.label }}</span>
                  <el-checkbox
                    :model-value="groupCheckAllStates[group.key]"
                    :indeterminate="groupIndeterminateStates[group.key]"
                    @click.stop
                    @update:model-value="(value) => toggleGroupColumns(group, value)"
                  >
                    鍏ㄩ€?
                  </el-checkbox>
                </div>
              </template>
              <div class="view-settings-group-body">
                <el-checkbox
                  v-for="column in flattenStatisticColumnLeavesFromGroup(group)"
                  :key="column.key"
                  :model-value="isColumnSelected(column.key)"
                  class="view-settings-check"
                  @update:model-value="(value) => toggleColumnSelection(column.key, value)"
                >
                  {{ column.label }}
                </el-checkbox>
              </div>
            </el-collapse-item>
          </el-collapse>
        </div>
      </div>

      <div class="view-settings-actions">
        <div class="view-settings-actions-main">
          <el-button @click="onRestoreDefaultView">閲嶇疆</el-button>
          <el-button @click="onSettingsVisibleChange(false)">鍙栨秷</el-button>
          <el-button type="primary" :icon="ArrowRight" @click="onSaveViewPrefs">淇濆瓨瑙嗗浘</el-button>
        </div>
      </div>
    </div>
  </el-drawer>
</template>
