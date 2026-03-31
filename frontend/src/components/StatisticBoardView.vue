<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import { ArrowDown, ArrowRight, Download, RefreshRight, Search, Sort } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { useRoute, useRouter } from 'vue-router';
import {
  api,
  type StatisticBoardResponse,
  type StatisticCellData,
  type StatisticColumnGroup,
  type StatisticColumnLeaf,
  type StatisticDetailColumn,
  type StatisticDetailResponse,
  type StatisticFilterField,
  type StatisticFilterOperator,
  type StatisticRowData,
} from '../api';
import {
  defaultVisibleColumnKeys,
  loadStatisticBoardViewPrefs,
  resetStatisticBoardViewPrefs,
  saveStatisticBoardViewPrefs,
  type StatisticBoardViewPrefs,
} from './statistic-board-view-prefs';
import type { StatisticBoardUiHooks } from './statistic-board-ui';
import {
  clearSortState,
  nextColumnSortState,
  ROW_LABEL_SORT_KEY,
  type SortDirection,
  sortDirectionForColumn as resolveSortDirectionForColumn,
  sortRowsFromSource,
} from './statistic-board-sorting';
import {
  createEmptyFilterGroup,
  createFilterConditionDraft,
  normalizeFilterDraftGroup,
  operatorLabel,
  sanitizeFilterDraftGroup,
  usesSecondaryValue,
  type StatisticFilterConditionDraft,
  type StatisticFilterDraftGroup,
} from './statistic-board-filters';

const props = withDefaults(
  defineProps<{
    boardKey: string;
    uiHooks?: StatisticBoardUiHooks;
  }>(),
  {
    uiHooks: () => ({}),
  },
);

const route = useRoute();
const router = useRouter();

function parsePositiveInteger(rawValue: unknown, fallback: number) {
  const parsed = Number.parseInt(String(rawValue ?? ''), 10);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback;
}

function parseSortDirection(rawValue: unknown, fallback: SortDirection) {
  return rawValue === 'asc' || rawValue === 'desc' || rawValue === 'default' ? rawValue : fallback;
}

function routeBoardSortColumn() {
  return String(route.query.sortBy ?? '');
}

function routeBoardSortDirection() {
  return parseSortDirection(route.query.sortOrder, 'default');
}

function routeDetailPage() {
  return parsePositiveInteger(route.query.detailPage, 1);
}

function routeDetailPageSize() {
  return parsePositiveInteger(route.query.detailPageSize, board.value?.definition.defaultPageSize ?? 10);
}

function routeDetailSortBy() {
  return String(route.query.detailSortBy ?? 'syncedAt');
}

function routeDetailSortOrder() {
  const value = String(route.query.detailSortOrder ?? 'descending');
  return value === 'ascending' || value === 'descending' ? value : 'descending';
}

function routeDetailVisible() {
  return String(route.query.detailVisible ?? '') === '1';
}

function buildFilterGroupFromRoute() {
  const draftGroup = createEmptyFilterGroup();
  draftGroup.logic = route.query.filterLogic === 'OR' ? 'OR' : 'AND';
  const keyedConditions = new Map<number, StatisticFilterConditionDraft>();

  for (const [key, rawValue] of Object.entries(route.query)) {
    const match = /^filters\.(\d+)\.(field|operator|value|value2)$/.exec(key);
    if (!match) {
      continue;
    }
    const index = Number.parseInt(match[1], 10);
    const nextValue = Array.isArray(rawValue) ? String(rawValue[0] ?? '') : String(rawValue ?? '');
    const currentCondition =
      keyedConditions.get(index) ?? {
        id: crypto.randomUUID(),
        fieldKey: '',
        operator: '' as StatisticFilterOperator | '',
        value: '',
        secondaryValue: '',
      };
    if (match[2] === 'field') {
      currentCondition.fieldKey = nextValue;
    } else if (match[2] === 'operator') {
      currentCondition.operator = nextValue as StatisticFilterOperator;
    } else if (match[2] === 'value') {
      currentCondition.value = nextValue;
    } else {
      currentCondition.secondaryValue = nextValue;
    }
    keyedConditions.set(index, currentCondition);
  }

  draftGroup.conditions.push(
    ...[...keyedConditions.entries()].sort((left, right) => left[0] - right[0]).map(([, condition]) => condition),
  );
  return draftGroup;
}

async function replaceRouteQuery(patch: Record<string, string | number | null | undefined>) {
  const nextQuery = { ...route.query } as Record<string, string>;
  for (const [key, value] of Object.entries(patch)) {
    if (value == null || value === '') {
      delete nextQuery[key];
    } else {
      nextQuery[key] = String(value);
    }
  }
  await router.replace({
    path: route.path,
    query: nextQuery,
    hash: route.hash,
  });
}

const loading = ref(false);
const detailLoading = ref(false);
const board = ref<StatisticBoardResponse | null>(null);
const errorMessage = ref('');
const filterDraft = reactive<StatisticFilterDraftGroup>(createEmptyFilterGroup());
const detailVisible = ref(false);
const activeRow = ref<StatisticRowData | null>(null);
const activeCell = ref<StatisticCellData | null>(null);
const detail = ref<StatisticDetailResponse | null>(null);
const settingsVisible = ref(false);
const boardViewPrefs = ref<StatisticBoardViewPrefs>({
  visibleColumnKeys: [],
  groupOrder: [],
  columnOrderByGroup: {},
  sortColumnKey: '',
  sortDirection: 'default',
  widthStrategy: 'compact',
});
const draftVisibleColumnKeys = ref<string[]>([]);
const expandedViewSettingGroups = ref<string[]>([]);

const detailPagination = reactive({
  page: 1,
  size: 10,
  sortField: '',
  sortOrder: 'descending',
});

const dragState = reactive<{
  type: 'group' | 'column' | '';
  sourceGroupKey: string;
  sourceColumnKey: string;
}>({
  type: '',
  sourceGroupKey: '',
  sourceColumnKey: '',
});

const activeFilterFields = computed(() => board.value?.definition.filters ?? []);

const visibleColumnKeySet = computed(() => new Set(boardViewPrefs.value.visibleColumnKeys));

const orderedColumnGroups = computed(() => {
  if (!board.value) {
    return [];
  }
  const groupMap = new Map(board.value.definition.columnGroups.map((group) => [group.key, group]));
  return boardViewPrefs.value.groupOrder
    .map((groupKey) => groupMap.get(groupKey))
    .filter((group): group is StatisticColumnGroup => Boolean(group))
    .map((group) => {
      const columnOrder = boardViewPrefs.value.columnOrderByGroup[group.key] ?? group.columns.map((column) => column.key);
      const columnMap = new Map(group.columns.map((column) => [column.key, column]));
      return {
        ...group,
        columns: columnOrder
          .map((columnKey) => columnMap.get(columnKey))
          .filter((column): column is StatisticColumnLeaf => Boolean(column))
          .filter((column) => visibleColumnKeySet.value.has(column.key)),
      };
    })
    .filter((group) => group.columns.length > 0);
});

const sortedRows = computed(() => {
  const rows = board.value?.rows ?? [];
  const columns = board.value?.definition.columnGroups.flatMap((group) => group.columns) ?? [];
  return sortRowsFromSource(rows, columns, boardViewPrefs.value);
});

const currentVisibleColumnCount = computed(() => boardViewPrefs.value.visibleColumnKeys.length);
const currentSortColumn = computed(() => {
  if (!board.value || !boardViewPrefs.value.sortColumnKey || boardViewPrefs.value.sortDirection === 'default') {
    return null;
  }
  if (boardViewPrefs.value.sortColumnKey === ROW_LABEL_SORT_KEY) {
    return {
      key: ROW_LABEL_SORT_KEY,
      label: '统计对象',
    };
  }
  return (
    board.value.definition.columnGroups
      .flatMap((group) => group.columns)
      .find((column) => column.key === boardViewPrefs.value.sortColumnKey) ?? null
  );
});
const currentSortSummary = computed(() => {
  if (!currentSortColumn.value) {
    return '';
  }
  return `${currentSortColumn.value.label} / ${boardViewPrefs.value.sortDirection === 'asc' ? '升序' : '降序'}`;
});

const tableRenderKey = computed(
  () =>
    [
      props.boardKey,
      boardViewPrefs.value.widthStrategy,
      boardViewPrefs.value.groupOrder.join('|'),
      Object.entries(boardViewPrefs.value.columnOrderByGroup)
        .map(([groupKey, columnKeys]) => `${groupKey}:${columnKeys.join(',')}`)
        .join('|'),
      boardViewPrefs.value.visibleColumnKeys.join('|'),
    ].join('::'),
);

const rowHeaderLabel = computed(() => board.value?.definition.rowHeaderLabel || '统计对象');
const allColumnKeys = computed(() => board.value?.definition.columnGroups.flatMap((group) => group.columns.map((column) => column.key)) ?? []);
const allColumnsSelected = computed(() => allColumnKeys.value.length > 0 && allColumnKeys.value.every((key) => draftVisibleColumnKeys.value.includes(key)));
const partiallySelectedColumns = computed(
  () => draftVisibleColumnKeys.value.length > 0 && draftVisibleColumnKeys.value.length < allColumnKeys.value.length,
);

const firstColumnWidth = computed(() => {
  if (!board.value) {
    return 132;
  }
  const longestLabelLength = board.value.rows.reduce((max, row) => Math.max(max, row.rowLabel.length), 4);
  if (boardViewPrefs.value.widthStrategy === 'compact') {
    return Math.min(148, Math.max(112, longestLabelLength * 14 + 24));
  }
  if (boardViewPrefs.value.widthStrategy === 'header') {
    return Math.min(176, Math.max(120, longestLabelLength * 16 + 30));
  }
  return Math.min(220, Math.max(128, longestLabelLength * 18 + 42));
});

const firstColumnMinWidth = computed(() => {
  if (boardViewPrefs.value.widthStrategy === 'compact') {
    return 108;
  }
  if (boardViewPrefs.value.widthStrategy === 'header') {
    return 120;
  }
  return 132;
});

function initializeFilters(fields: StatisticFilterField[]) {
  const routeFilterGroup = buildFilterGroupFromRoute();
  const nextDraft = normalizeFilterDraftGroup(routeFilterGroup, fields);
  filterDraft.logic = nextDraft.logic;
  filterDraft.conditions.splice(0, filterDraft.conditions.length, ...nextDraft.conditions);
}

function applyStoredViewPrefs(response: StatisticBoardResponse) {
  const storedPrefs = loadStatisticBoardViewPrefs(props.boardKey, response.definition);
  boardViewPrefs.value = {
    ...storedPrefs,
    sortColumnKey: routeBoardSortColumn(),
    sortDirection: routeBoardSortDirection(),
  };
  draftVisibleColumnKeys.value = [...boardViewPrefs.value.visibleColumnKeys];
}

function persistViewPrefs() {
  saveStatisticBoardViewPrefs(props.boardKey, boardViewPrefs.value);
}

function buildFilterPayload() {
  return sanitizeFilterDraftGroup(filterDraft);
}

async function loadBoard(showError = true) {
  loading.value = true;
  errorMessage.value = '';
  try {
    const response = await api.getStatisticBoard(props.boardKey, {
      filterGroup: buildFilterPayload(),
    });
    board.value = response;
    initializeFilters(response.definition.filters);
    applyStoredViewPrefs(response);
    detailPagination.page = routeDetailPage();
    detailPagination.size = routeDetailPageSize();
    detailPagination.sortField = routeDetailSortBy();
    detailPagination.sortOrder = routeDetailSortOrder();
  } catch (error) {
    errorMessage.value = (error as Error).message;
    if (showError) {
      ElMessage.error((error as Error).message);
    }
  } finally {
    loading.value = false;
  }
}

async function exportBoard() {
  try {
    const csv = await api.exportStatisticBoard(props.boardKey, {
      filterGroup: buildFilterPayload(),
    });
    const blob = new Blob([`\uFEFF${csv}`], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = `${props.boardKey}.csv`;
    document.body.appendChild(anchor);
    anchor.click();
    document.body.removeChild(anchor);
    URL.revokeObjectURL(url);
    ElMessage.success('导出成功');
  } catch (error) {
    ElMessage.error((error as Error).message);
  }
}

function resetFilters() {
  filterDraft.logic = 'AND';
  filterDraft.conditions.splice(0, filterDraft.conditions.length);
  const nextQuery = { ...route.query } as Record<string, string>;
  delete nextQuery.filterLogic;
  for (const key of Object.keys(nextQuery)) {
    if (key.startsWith('filters.')) {
      delete nextQuery[key];
    }
  }
  void router.replace({
    path: route.path,
    query: nextQuery,
    hash: route.hash,
  });
}

function openSettings() {
  if (!board.value) {
    return;
  }
  draftVisibleColumnKeys.value = [...boardViewPrefs.value.visibleColumnKeys];
  expandedViewSettingGroups.value = [];
  settingsVisible.value = true;
}

function handleSettingsCommand(command: string) {
  if (command === 'open-settings') {
    openSettings();
    return;
  }
  if (command === 'clear-sort') {
    clearCurrentSort();
    return;
  }
  if (command === 'restore-default-view') {
    restoreDefaultView();
  }
}

function saveViewPrefs() {
  if (!board.value) {
    return;
  }
  if (!draftVisibleColumnKeys.value.length) {
    ElMessage.warning('至少保留一列用于展示');
    return;
  }
  boardViewPrefs.value = {
    ...boardViewPrefs.value,
    visibleColumnKeys: [...draftVisibleColumnKeys.value],
  };
  persistViewPrefs();
  settingsVisible.value = false;
  ElMessage.success('视图配置已保存');
}

function restoreDefaultView() {
  if (!board.value) {
    return;
  }
  boardViewPrefs.value = {
    visibleColumnKeys: defaultVisibleColumnKeys(board.value.definition),
    groupOrder: board.value.definition.columnGroups.map((group) => group.key),
    columnOrderByGroup: Object.fromEntries(
      board.value.definition.columnGroups.map((group) => [group.key, group.columns.map((column) => column.key)]),
    ),
    sortColumnKey: '',
    sortDirection: 'default',
    widthStrategy: 'compact',
  };
  draftVisibleColumnKeys.value = [...boardViewPrefs.value.visibleColumnKeys];
  resetStatisticBoardViewPrefs(props.boardKey);
  settingsVisible.value = false;
  ElMessage.success('已恢复默认视图');
}

function clearCurrentSort() {
  boardViewPrefs.value = {
    ...boardViewPrefs.value,
    ...clearSortState(),
  };
  persistViewPrefs();
  void replaceRouteQuery({
    sortBy: '',
    sortOrder: '',
  });
  ElMessage.success('已恢复默认排序');
}

function toggleAllColumns(checked: boolean | string | number) {
  draftVisibleColumnKeys.value = checked ? [...allColumnKeys.value] : [];
}

function groupColumnKeys(group: StatisticColumnGroup) {
  return group.columns.map((column) => column.key);
}

function isGroupFullySelected(group: StatisticColumnGroup) {
  const columnKeys = groupColumnKeys(group);
  return columnKeys.length > 0 && columnKeys.every((key) => draftVisibleColumnKeys.value.includes(key));
}

function isGroupPartiallySelected(group: StatisticColumnGroup) {
  const columnKeys = groupColumnKeys(group);
  const selectedCount = columnKeys.filter((key) => draftVisibleColumnKeys.value.includes(key)).length;
  return selectedCount > 0 && selectedCount < columnKeys.length;
}

function toggleGroupColumns(group: StatisticColumnGroup, checked: boolean | string | number) {
  const groupKeys = new Set(groupColumnKeys(group));
  if (checked) {
    draftVisibleColumnKeys.value = [...new Set([...draftVisibleColumnKeys.value, ...groupKeys])];
    return;
  }
  draftVisibleColumnKeys.value = draftVisibleColumnKeys.value.filter((key) => !groupKeys.has(key));
}

function detailCellValue(record: Record<string, unknown>, column: StatisticDetailColumn) {
  const value = record[column.key];
  if (value == null || value === '') {
    return '-';
  }
  if (typeof value === 'object') {
    return JSON.stringify(value);
  }
  return String(value);
}

async function loadDetail() {
  if (!activeRow.value || !activeCell.value) {
    return;
  }
  detailLoading.value = true;
  try {
    detail.value = await api.getStatisticBoardDetails(props.boardKey, {
      rowKey: activeRow.value.rowKey,
      columnKey: activeCell.value.columnKey,
      page: detailPagination.page,
      size: detailPagination.size,
      sortField: detailPagination.sortField || undefined,
      sortOrder: detailPagination.sortOrder || undefined,
      filterGroup: buildFilterPayload(),
    });
  } catch (error) {
    ElMessage.error((error as Error).message);
  } finally {
    detailLoading.value = false;
  }
}

async function openDetail(row: StatisticRowData, cell: StatisticCellData) {
  if (!cell.drilldown) {
    return;
  }
  activeRow.value = row;
  activeCell.value = cell;
  await replaceRouteQuery({
    detailVisible: '1',
    detailRowKey: row.rowKey,
    detailColumnKey: cell.columnKey,
    detailPage: 1,
    detailPageSize: board.value?.definition.defaultPageSize ?? 10,
    detailSortBy: 'syncedAt',
    detailSortOrder: 'descending',
  });
}

function handleDetailSortChange({
  prop,
  order,
}: {
  column: unknown;
  prop: string;
  order: 'ascending' | 'descending' | null;
}) {
  void replaceRouteQuery({
    detailSortBy: prop || '',
    detailSortOrder: order ?? 'descending',
    detailPage: 1,
  });
}

function handleDetailCurrentChange(nextPage: number) {
  void replaceRouteQuery({
    detailPage: nextPage,
  });
}

function handleDetailSizeChange(nextSize: number) {
  void replaceRouteQuery({
    detailPageSize: nextSize,
    detailPage: 1,
  });
}

function handleDetailVisibleChange(visible: boolean) {
  if (visible) {
    return;
  }
  detailVisible.value = false;
  void replaceRouteQuery({
    detailVisible: '',
    detailRowKey: '',
    detailColumnKey: '',
    detailPage: '',
    detailPageSize: '',
    detailSortBy: '',
    detailSortOrder: '',
  });
}

function cellForColumn(row: StatisticRowData, columnKey: string) {
  return row.cells.find((item) => item.columnKey === columnKey);
}

function sortDirectionForColumn(columnKey: string) {
  return resolveSortDirectionForColumn(boardViewPrefs.value, columnKey);
}

function toggleColumnSort(columnKey: string) {
  const nextSortState = nextColumnSortState(boardViewPrefs.value, columnKey);
  boardViewPrefs.value = {
    ...boardViewPrefs.value,
    ...nextSortState,
  };
  persistViewPrefs();
  void replaceRouteQuery({
    sortBy: nextSortState.sortColumnKey,
    sortOrder: nextSortState.sortDirection,
  });
}

function sortStateLabel(direction: SortDirection) {
  if (direction === 'asc') {
    return '当前为升序，点击切换为降序';
  }
  if (direction === 'desc') {
    return '当前为降序，点击切换为升序';
  }
  return '当前为默认顺序，点击开始排序';
}

function addFilterCondition() {
  const field = activeFilterFields.value[0];
  filterDraft.conditions.push(createFilterConditionDraft(field));
}

function buildFilterQueryPatch() {
  const patch: Record<string, string | number | null> = {
    filterLogic: filterDraft.conditions.length ? filterDraft.logic : null,
  };
  for (const key of Object.keys(route.query)) {
    if (key.startsWith('filters.')) {
      patch[key] = null;
    }
  }
  filterDraft.conditions.forEach((condition, index) => {
    patch[`filters.${index}.field`] = condition.fieldKey || null;
    patch[`filters.${index}.operator`] = condition.operator || null;
    patch[`filters.${index}.value`] = condition.value || null;
    patch[`filters.${index}.value2`] = condition.secondaryValue || null;
  });
  return patch;
}

async function applyFiltersToRoute() {
  await replaceRouteQuery({
    ...buildFilterQueryPatch(),
    detailVisible: '',
    detailRowKey: '',
    detailColumnKey: '',
    detailPage: '',
    detailPageSize: '',
    detailSortBy: '',
    detailSortOrder: '',
  });
}

async function refreshBoard() {
  loading.value = true;
  try {
    await loadBoard();
    if (detailVisible.value) {
      await loadDetail();
    }
  } finally {
    loading.value = false;
  }
}

function removeFilterCondition(conditionId: string) {
  const index = filterDraft.conditions.findIndex((condition) => condition.id === conditionId);
  if (index >= 0) {
    filterDraft.conditions.splice(index, 1);
  }
}

function fieldForCondition(fieldKey: string) {
  return activeFilterFields.value.find((field) => field.key === fieldKey) ?? null;
}

function handleConditionFieldChange(condition: StatisticFilterConditionDraft) {
  const field = fieldForCondition(condition.fieldKey);
  condition.operator = (field?.operators?.[0] ?? '') as StatisticFilterOperator | '';
  condition.value = '';
  condition.secondaryValue = '';
}

function operatorOptionsForCondition(condition: StatisticFilterConditionDraft) {
  return fieldForCondition(condition.fieldKey)?.operators ?? [];
}

function usesDatePicker(condition: StatisticFilterConditionDraft) {
  return fieldForCondition(condition.fieldKey)?.type === 'datetime';
}

function datePickerType(condition: StatisticFilterConditionDraft) {
  return (
    {
      year: 'year',
      month: 'month',
      day: 'date',
      at: 'datetime',
      before: 'datetime',
      after: 'datetime',
      between: 'datetime',
    } as Record<string, string>
  )[condition.operator] ?? 'datetime';
}

function dateValueFormat(condition: StatisticFilterConditionDraft) {
  return (
    {
      year: 'YYYY',
      month: 'YYYY-MM',
      day: 'YYYY-MM-DD',
      at: 'YYYY-MM-DD HH:mm:ss',
      before: 'YYYY-MM-DD HH:mm:ss',
      after: 'YYYY-MM-DD HH:mm:ss',
      between: 'YYYY-MM-DD HH:mm:ss',
    } as Record<string, string>
  )[condition.operator] ?? 'YYYY-MM-DD HH:mm:ss';
}

function isNumericField(condition: StatisticFilterConditionDraft) {
  return fieldForCondition(condition.fieldKey)?.type === 'number';
}

function isSelectField(condition: StatisticFilterConditionDraft) {
  const type = fieldForCondition(condition.fieldKey)?.type;
  return type === 'select';
}

function fieldOptions(condition: StatisticFilterConditionDraft) {
  return fieldForCondition(condition.fieldKey)?.options ?? [];
}

function onGroupDragStart(groupKey: string) {
  dragState.type = 'group';
  dragState.sourceGroupKey = groupKey;
  dragState.sourceColumnKey = '';
}

function isGroupDragging(groupKey: string) {
  return dragState.type === 'group' && dragState.sourceGroupKey === groupKey;
}

function onGroupDrop(targetGroupKey: string) {
  if (dragState.type !== 'group' || dragState.sourceGroupKey === targetGroupKey) {
    clearDragState();
    return;
  }

  const nextOrder = [...boardViewPrefs.value.groupOrder];
  const sourceIndex = nextOrder.indexOf(dragState.sourceGroupKey);
  const targetIndex = nextOrder.indexOf(targetGroupKey);
  if (sourceIndex < 0 || targetIndex < 0) {
    clearDragState();
    return;
  }
  const [moved] = nextOrder.splice(sourceIndex, 1);
  nextOrder.splice(targetIndex, 0, moved);
  boardViewPrefs.value = {
    ...boardViewPrefs.value,
    groupOrder: nextOrder,
  };
  persistViewPrefs();
  clearDragState();
}

function onColumnDragStart(groupKey: string, columnKey: string) {
  dragState.type = 'column';
  dragState.sourceGroupKey = groupKey;
  dragState.sourceColumnKey = columnKey;
}

function isColumnDragging(groupKey: string, columnKey: string) {
  return (
    dragState.type === 'column' &&
    dragState.sourceGroupKey === groupKey &&
    dragState.sourceColumnKey === columnKey
  );
}

function onColumnDrop(targetGroupKey: string, targetColumnKey: string) {
  if (
    dragState.type !== 'column' ||
    dragState.sourceGroupKey !== targetGroupKey ||
    dragState.sourceColumnKey === targetColumnKey
  ) {
    clearDragState();
    return;
  }

  const nextColumns = [...(boardViewPrefs.value.columnOrderByGroup[targetGroupKey] ?? [])];
  const sourceIndex = nextColumns.indexOf(dragState.sourceColumnKey);
  const targetIndex = nextColumns.indexOf(targetColumnKey);
  if (sourceIndex < 0 || targetIndex < 0) {
    clearDragState();
    return;
  }
  const [moved] = nextColumns.splice(sourceIndex, 1);
  nextColumns.splice(targetIndex, 0, moved);
  boardViewPrefs.value = {
    ...boardViewPrefs.value,
    columnOrderByGroup: {
      ...boardViewPrefs.value.columnOrderByGroup,
      [targetGroupKey]: nextColumns,
    },
  };
  persistViewPrefs();
  clearDragState();
}

function clearDragState() {
  dragState.type = '';
  dragState.sourceGroupKey = '';
  dragState.sourceColumnKey = '';
}

function columnWidth(column: StatisticColumnLeaf) {
  if (column.metricType.includes('count') || column.metricType.includes('ratio') || column.metricType.includes('number')) {
    return boardViewPrefs.value.widthStrategy === 'compact' ? 96 : boardViewPrefs.value.widthStrategy === 'header' ? 124 : 156;
  }
  if (boardViewPrefs.value.widthStrategy === 'compact') {
    return compactColumnWidth(column);
  }
  if (boardViewPrefs.value.widthStrategy === 'header') {
    return headerBasedWidth(column);
  }
  return contentBasedWidth(column);
}

function columnMinWidth(column: StatisticColumnLeaf) {
  return columnWidth(column);
}

function columnResizable(column: StatisticColumnLeaf) {
  return !(column.metricType.includes('count') || column.metricType.includes('ratio') || column.metricType.includes('number'));
}

function compactColumnWidth(column: StatisticColumnLeaf) {
  if (column.metricType.includes('time') || column.metricType.includes('date')) {
    return 132;
  }
  return Math.min(136, Math.max(88, column.label.length * 11 + 24));
}

function headerBasedWidth(column: StatisticColumnLeaf) {
  return Math.min(196, Math.max(116, column.label.length * 16 + 34));
}

function contentBasedWidth(column: StatisticColumnLeaf) {
  if (!board.value) {
    return headerBasedWidth(column);
  }
  const maxLength = board.value.rows.reduce((current, row) => {
    const valueLength = (cellForColumn(row, column.key)?.displayValue ?? '').length;
    return Math.max(current, valueLength);
  }, column.label.length);
  if (column.metricType.includes('time') || column.metricType.includes('date')) {
    return Math.min(240, Math.max(156, maxLength * 9 + 34));
  }
  return Math.min(280, Math.max(132, maxLength * 13 + 26));
}

watch(detailVisible, (visible) => {
  if (!visible) {
    detail.value = null;
    activeRow.value = null;
    activeCell.value = null;
  }
});

watch(
  () => route.query,
  async () => {
    loading.value = true;
    try {
      await loadBoard(false);
      detailVisible.value = routeDetailVisible();
      if (detailVisible.value) {
        activeRow.value = board.value?.rows.find((row) => row.rowKey === String(route.query.detailRowKey ?? '')) ?? null;
        activeCell.value = activeRow.value?.cells.find((cell) => cell.columnKey === String(route.query.detailColumnKey ?? '')) ?? null;
        if (activeRow.value && activeCell.value) {
          await loadDetail();
        } else {
          detail.value = null;
        }
      } else {
        detail.value = null;
      }
    } finally {
      loading.value = false;
    }
  },
  { immediate: true, deep: true },
);
</script>

<template>
  <div class="stat-board" :class="props.uiHooks.rootClass">
    <el-card shadow="never" class="stat-board-card" :class="props.uiHooks.cardClass" v-loading="loading">
      <template #header>
        <div class="stat-board-toolbar" :class="props.uiHooks.toolbarClass">
          <div class="stat-board-toolbar-main" :class="props.uiHooks.toolbarMainClass">
            <div class="stat-filter-builder">
              <el-segmented
                v-model="filterDraft.logic"
                :options="[{ label: '满足全部', value: 'AND' }, { label: '满足任意', value: 'OR' }]"
                class="stat-filter-logic"
              />
              <div v-if="filterDraft.conditions.length" class="stat-filter-list">
                <div v-for="condition in filterDraft.conditions" :key="condition.id" class="stat-filter-row">
                  <el-select v-model="condition.fieldKey" class="stat-filter-field" placeholder="字段" @change="handleConditionFieldChange(condition)">
                    <el-option v-for="field in activeFilterFields" :key="field.key" :label="field.label" :value="field.key" />
                  </el-select>
                  <el-select v-model="condition.operator" class="stat-filter-operator" placeholder="关系">
                    <el-option v-for="operator in operatorOptionsForCondition(condition)" :key="operator" :label="operatorLabel(operator)" :value="operator" />
                  </el-select>
                  <el-select v-if="isSelectField(condition)" v-model="condition.value" class="stat-filter-value" placeholder="值" clearable filterable>
                    <el-option v-for="option in fieldOptions(condition)" :key="option.value" :label="option.label" :value="option.value" />
                  </el-select>
                  <el-input-number v-else-if="isNumericField(condition)" v-model="condition.value" class="stat-filter-value" controls-position="right" placeholder="值" />
                  <el-date-picker v-else-if="usesDatePicker(condition)" v-model="condition.value" class="stat-filter-value" :type="datePickerType(condition)" :value-format="dateValueFormat(condition)" placeholder="时间" />
                  <el-input v-else v-model="condition.value" class="stat-filter-value" placeholder="值" clearable />
                  <el-input-number v-if="usesSecondaryValue(condition.operator) && isNumericField(condition)" v-model="condition.secondaryValue" class="stat-filter-value secondary" controls-position="right" placeholder="结束值" />
                  <el-date-picker v-else-if="usesSecondaryValue(condition.operator) && usesDatePicker(condition)" v-model="condition.secondaryValue" class="stat-filter-value secondary" :type="datePickerType(condition)" :value-format="dateValueFormat(condition)" placeholder="结束时间" />
                  <el-input v-else-if="usesSecondaryValue(condition.operator)" v-model="condition.secondaryValue" class="stat-filter-value secondary" placeholder="结束值" clearable />
                  <el-button text type="danger" @click="removeFilterCondition(condition.id)">删除</el-button>
                </div>
              </div>
              <el-button plain @click="addFilterCondition">添加条件</el-button>
            </div>
          </div>

          <div class="stat-board-toolbar-actions" :class="props.uiHooks.toolbarActionsClass">
            <span v-if="board?.definition.title" class="stat-board-meta-text">{{ board.definition.title }}</span>
            <el-button type="primary" :icon="Search" @click="applyFiltersToRoute">查询</el-button>
            <el-button @click="resetFilters">重置</el-button>
            <el-button :icon="RefreshRight" @click="refreshBoard">刷新</el-button>
            <el-button plain :icon="Download" @click="exportBoard">导出</el-button>
            <el-dropdown trigger="click" @command="handleSettingsCommand">
              <el-button class="view-settings-trigger">
                <span class="hamburger-icon" aria-hidden="true">
                  <span></span>
                  <span></span>
                  <span></span>
                </span>
                <span>设置</span>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="open-settings">列显示设置</el-dropdown-item>
                  <el-dropdown-item command="clear-sort">恢复默认排序</el-dropdown-item>
                  <el-dropdown-item command="restore-default-view">恢复默认视图</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </template>

      <el-alert
        v-if="errorMessage"
        :title="errorMessage"
        type="error"
        :closable="false"
        show-icon
        class="stat-board-alert"
      />

      <div v-if="currentSortColumn" class="stat-board-sortbar">
        <span class="stat-board-sortbar-label">当前排序</span>
        <el-tag size="small" type="primary" effect="plain">{{ currentSortSummary }}</el-tag>
      </div>

      <div v-if="board" class="stat-matrix-wrapper">
        <el-table
          :key="tableRenderKey"
          :data="sortedRows"
          border
          stripe
          fit
          class="stat-matrix-table"
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
                <span class="stat-column-header-label">{{ rowHeaderLabel }}</span>
                <button
                  class="sort-trigger"
                  :class="`is-${sortDirectionForColumn(ROW_LABEL_SORT_KEY)}`"
                  type="button"
                  :title="sortStateLabel(sortDirectionForColumn(ROW_LABEL_SORT_KEY))"
                  @click.stop="toggleColumnSort(ROW_LABEL_SORT_KEY)"
                >
                  <el-icon class="sort-trigger-icon"><Sort /></el-icon>
                  <span class="sort-trigger-state">
                    {{ sortDirectionForColumn(ROW_LABEL_SORT_KEY) === 'asc' ? '升序' : sortDirectionForColumn(ROW_LABEL_SORT_KEY) === 'desc' ? '降序' : '排序' }}
                  </span>
                </button>
              </div>
            </template>
          </el-table-column>
          <el-table-column
            v-for="group in orderedColumnGroups"
            :key="group.key"
            align="center"
          >
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
                <span class="drag-handle group" aria-hidden="true">
                  <span></span>
                  <span></span>
                  <span></span>
                </span>
                <span>{{ group.label }}</span>
                <span class="drag-handle" aria-hidden="true">
                  <span></span>
                  <span></span>
                </span>
              </div>
            </template>
            <el-table-column
              v-for="column in group.columns"
              :key="column.key"
              align="center"
              :min-width="columnMinWidth(column)"
              :resizable="columnResizable(column)"
            >
              <template #header>
                <div
                  class="stat-column-header"
                  :class="{
                    dragging: isColumnDragging(group.key, column.key),
                    sorting: sortDirectionForColumn(column.key) !== 'default',
                  }"
                  draggable="true"
                  @dragstart="onColumnDragStart(group.key, column.key)"
                  @dragover.prevent
                  @drop.prevent="onColumnDrop(group.key, column.key)"
                  @dragend="clearDragState"
                >
                  <span class="drag-handle subtle" aria-hidden="true">
                    <span></span>
                    <span></span>
                    <span></span>
                  </span>
                  <span class="stat-column-header-label">{{ column.label }}</span>
                  <button
                    class="sort-trigger"
                    :class="`is-${sortDirectionForColumn(column.key)}`"
                    type="button"
                    :title="sortStateLabel(sortDirectionForColumn(column.key))"
                    @click.stop="toggleColumnSort(column.key)"
                  >
                    <el-icon class="sort-trigger-icon"><Sort /></el-icon>
                    <span class="sort-trigger-state">
                      {{ sortDirectionForColumn(column.key) === 'asc' ? '升序' : sortDirectionForColumn(column.key) === 'desc' ? '降序' : '排序' }}
                    </span>
                  </button>
                </div>
              </template>
              <template #default="{ row }">
                <button
                  v-if="cellForColumn(row, column.key)?.drilldown"
                  class="stat-cell drilldown"
                  @click="openDetail(row, cellForColumn(row, column.key)!)"
                >
                  {{ cellForColumn(row, column.key)?.displayValue || '-' }}
                </button>
                <span v-else class="stat-cell">
                  {{ cellForColumn(row, column.key)?.displayValue || '-' }}
                </span>
              </template>
            </el-table-column>
          </el-table-column>
        </el-table>
      </div>

      <el-empty
        v-if="board && !sortedRows.length"
        :description="board?.definition.emptyText || '当前筛选条件下没有可展示的统计结果。'"
        class="stat-empty"
      />
    </el-card>

    <el-dialog
      :model-value="detailVisible"
      :title="detail?.title || '明细数据'"
      class="stat-detail-dialog"
      width="72%"
      top="8vh"
      align-center
      @update:model-value="handleDetailVisibleChange"
      destroy-on-close
      append-to-body
    >
      <div class="stat-detail-shell" v-loading="detailLoading">
        <el-table
          v-if="detail"
          :data="detail.records"
          border
          stripe
          class="stat-detail-table"
          :class="props.uiHooks.detailTableClass"
          @sort-change="handleDetailSortChange"
        >
          <el-table-column
            v-for="column in detail.columns"
            :key="column.key"
            :prop="column.key"
            :label="column.label"
            :width="column.width || undefined"
            :min-width="column.minWidth || 140"
            :sortable="column.sortable ? 'custom' : false"
            show-overflow-tooltip
          >
            <template #default="{ row }">
              <span class="detail-cell-text">{{ detailCellValue(row, column) }}</span>
            </template>
          </el-table-column>
        </el-table>

        <div class="detail-pagination">
          <el-pagination
            v-if="detail"
            :current-page="detailPagination.page"
            :page-size="detailPagination.size"
            background
            layout="total, sizes, prev, pager, next"
            :page-sizes="[10, 20, 50, 100]"
            :total="detail.total"
            @current-change="handleDetailCurrentChange"
            @size-change="handleDetailSizeChange"
          />
        </div>
      </div>
    </el-dialog>

    <el-drawer v-model="settingsVisible" title="表格视图设置" size="360px" append-to-body class="view-settings-drawer">
      <div class="view-settings-panel" :class="props.uiHooks.settingsPanelClass" v-if="board">
        <div class="view-settings-summary">
          <div class="view-settings-summary-title">列显示控制</div>
          <div class="view-settings-summary-text">当前已选择 {{ currentVisibleColumnCount }} 列，可按需调整当前页面的展示视图。</div>
        </div>

        <div class="view-settings-strategy">
          <div class="view-settings-group-title">列宽展示策略</div>
          <el-radio-group v-model="boardViewPrefs.widthStrategy" class="width-strategy-group">
            <el-radio-button value="compact">统一紧凑</el-radio-button>
            <el-radio-button value="header">按字段长度</el-radio-button>
            <el-radio-button value="content">按内容长度</el-radio-button>
          </el-radio-group>
          <div class="view-settings-strategy-tip">首列继续单独压缩处理，纯数字统计列保持固定宽度，但会随策略在紧凑、标准、宽松之间切换。</div>
        </div>

        <div class="view-settings-scroll">
          <div class="view-settings-global-toggle">
            <el-checkbox
              :model-value="allColumnsSelected"
              :indeterminate="partiallySelectedColumns"
              @update:model-value="toggleAllColumns"
            >
              勾选全部列
            </el-checkbox>
            <span class="view-settings-global-meta">{{ draftVisibleColumnKeys.length }}/{{ allColumnKeys.length }}</span>
          </div>

          <el-checkbox-group v-model="draftVisibleColumnKeys" class="view-settings-checklist">
            <el-collapse v-model="expandedViewSettingGroups" class="view-settings-collapse">
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
                      :model-value="isGroupFullySelected(group)"
                      :indeterminate="isGroupPartiallySelected(group)"
                      @click.stop
                      @update:model-value="(value) => toggleGroupColumns(group, value)"
                    >
                      全选
                    </el-checkbox>
                  </div>
                </template>
                <div class="view-settings-group-body">
                  <el-checkbox
                    v-for="column in group.columns"
                    :key="column.key"
                    :value="column.key"
                    class="view-settings-check"
                  >
                    {{ column.label }}
                  </el-checkbox>
                </div>
              </el-collapse-item>
            </el-collapse>
          </el-checkbox-group>
        </div>

        <div class="view-settings-actions">
          <div class="view-settings-actions-main">
            <el-button @click="restoreDefaultView">重置</el-button>
            <el-button @click="settingsVisible = false">取消</el-button>
            <el-button type="primary" :icon="ArrowRight" @click="saveViewPrefs">保存视图</el-button>
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>
