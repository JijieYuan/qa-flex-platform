import type { StatisticBoardDefinition } from '../api';

const STORAGE_PREFIX = 'stat-board-view:';

interface StoredStatisticBoardViewPrefs {
  visibleColumnKeys: string[];
  groupOrder: string[];
  columnOrderByGroup: Record<string, string[]>;
  sortColumnKey: string;
  sortDirection: 'default' | 'asc' | 'desc';
  widthStrategy: 'compact' | 'header' | 'content';
}

export interface StatisticBoardViewPrefs {
  visibleColumnKeys: string[];
  groupOrder: string[];
  columnOrderByGroup: Record<string, string[]>;
  sortColumnKey: string;
  sortDirection: 'default' | 'asc' | 'desc';
  widthStrategy: 'compact' | 'header' | 'content';
}

function storageKey(boardKey: string) {
  return `${STORAGE_PREFIX}${boardKey}`;
}

function defaultGroupOrder(definition: StatisticBoardDefinition): string[] {
  return definition.columnGroups.map((group) => group.key);
}

function defaultColumnOrderByGroup(definition: StatisticBoardDefinition): Record<string, string[]> {
  return Object.fromEntries(
    definition.columnGroups.map((group) => [group.key, group.columns.map((column) => column.key)]),
  );
}

export function defaultVisibleColumnKeys(definition: StatisticBoardDefinition): string[] {
  return definition.columnGroups.flatMap((group) => group.columns.map((column) => column.key));
}

export function loadStatisticBoardViewPrefs(
  boardKey: string,
  definition: StatisticBoardDefinition,
): StatisticBoardViewPrefs {
  const fallbackVisible = defaultVisibleColumnKeys(definition);
  const fallbackGroupOrder = defaultGroupOrder(definition);
  const fallbackColumnOrderByGroup = defaultColumnOrderByGroup(definition);
  const raw = window.localStorage.getItem(storageKey(boardKey));

  if (!raw) {
    return {
      visibleColumnKeys: fallbackVisible,
      groupOrder: fallbackGroupOrder,
      columnOrderByGroup: fallbackColumnOrderByGroup,
      sortColumnKey: '',
      sortDirection: 'default',
      widthStrategy: 'compact',
    };
  }

  try {
    const parsed = JSON.parse(raw) as Partial<StoredStatisticBoardViewPrefs>;
    const allowedColumnKeys = new Set(fallbackVisible);
    const allowedGroupKeys = new Set(fallbackGroupOrder);

    const visibleColumnKeys = (parsed.visibleColumnKeys ?? []).filter((key) => allowedColumnKeys.has(key));
    const groupOrder = (parsed.groupOrder ?? []).filter((key) => allowedGroupKeys.has(key));
    const resolvedGroupOrder = groupOrder.length ? groupOrder : fallbackGroupOrder;
    const columnOrderByGroup = Object.fromEntries(
      resolvedGroupOrder.map((groupKey) => {
        const fallbackColumns = fallbackColumnOrderByGroup[groupKey] ?? [];
        const persisted = (parsed.columnOrderByGroup?.[groupKey] ?? []).filter((key) => allowedColumnKeys.has(key));
        return [groupKey, persisted.length ? persisted : fallbackColumns];
      }),
    );

    return {
      visibleColumnKeys: visibleColumnKeys.length ? visibleColumnKeys : fallbackVisible,
      groupOrder: resolvedGroupOrder,
      columnOrderByGroup,
      sortColumnKey:
        parsed.sortColumnKey && allowedColumnKeys.has(parsed.sortColumnKey) ? parsed.sortColumnKey : '',
      sortDirection:
        parsed.sortDirection === 'asc' || parsed.sortDirection === 'desc' ? parsed.sortDirection : 'default',
      widthStrategy:
        parsed.widthStrategy === 'header' || parsed.widthStrategy === 'content' ? parsed.widthStrategy : 'compact',
    };
  } catch {
    return {
      visibleColumnKeys: fallbackVisible,
      groupOrder: fallbackGroupOrder,
      columnOrderByGroup: fallbackColumnOrderByGroup,
      sortColumnKey: '',
      sortDirection: 'default',
      widthStrategy: 'compact',
    };
  }
}

export function saveStatisticBoardViewPrefs(boardKey: string, prefs: StatisticBoardViewPrefs) {
  window.localStorage.setItem(storageKey(boardKey), JSON.stringify(prefs));
}

export function resetStatisticBoardViewPrefs(boardKey: string) {
  window.localStorage.removeItem(storageKey(boardKey));
}
