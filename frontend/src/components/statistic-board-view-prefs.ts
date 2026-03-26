import type { StatisticBoardDefinition } from '../api';

const STORAGE_PREFIX = 'stat-board-view:';

interface StoredStatisticBoardViewPrefs {
  visibleColumnKeys: string[];
}

export interface StatisticBoardViewPrefs {
  visibleColumnKeys: string[];
}

function storageKey(boardKey: string) {
  return `${STORAGE_PREFIX}${boardKey}`;
}

export function defaultVisibleColumnKeys(definition: StatisticBoardDefinition): string[] {
  return definition.columnGroups.flatMap((group) => group.columns.map((column) => column.key));
}

export function loadStatisticBoardViewPrefs(
  boardKey: string,
  definition: StatisticBoardDefinition,
): StatisticBoardViewPrefs {
  const fallback = defaultVisibleColumnKeys(definition);
  const raw = window.localStorage.getItem(storageKey(boardKey));
  if (!raw) {
    return { visibleColumnKeys: fallback };
  }

  try {
    const parsed = JSON.parse(raw) as StoredStatisticBoardViewPrefs;
    const allowedKeys = new Set(fallback);
    const visibleColumnKeys = (parsed.visibleColumnKeys ?? []).filter((key) => allowedKeys.has(key));
    return { visibleColumnKeys: visibleColumnKeys.length ? visibleColumnKeys : fallback };
  } catch {
    return { visibleColumnKeys: fallback };
  }
}

export function saveStatisticBoardViewPrefs(boardKey: string, prefs: StatisticBoardViewPrefs) {
  const payload: StoredStatisticBoardViewPrefs = {
    visibleColumnKeys: prefs.visibleColumnKeys,
  };
  window.localStorage.setItem(storageKey(boardKey), JSON.stringify(payload));
}

export function resetStatisticBoardViewPrefs(boardKey: string) {
  window.localStorage.removeItem(storageKey(boardKey));
}
