import type { LocationQuery } from 'vue-router';
import type { StatisticFilterOperator } from '../api';
import type { SortDirection } from './statistic-board-sorting';
import {
  createEmptyFilterGroup,
  type StatisticFilterConditionDraft,
  type StatisticFilterDraftGroup,
} from './statistic-board-filters';

function nextConditionId() {
  if (typeof globalThis.crypto?.randomUUID === 'function') {
    return globalThis.crypto.randomUUID();
  }
  return `condition-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
}

export function parsePositiveInteger(rawValue: unknown, fallback: number) {
  const parsed = Number.parseInt(String(rawValue ?? ''), 10);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback;
}

export function parseSortDirection(rawValue: unknown, fallback: SortDirection) {
  return rawValue === 'asc' || rawValue === 'desc' || rawValue === 'default' ? rawValue : fallback;
}

export function routeBoardSortColumn(query: LocationQuery) {
  return String(query.sortBy ?? '');
}

export function routeBoardSortDirection(query: LocationQuery) {
  return parseSortDirection(query.sortOrder, 'default');
}

export function routeDetailPage(query: LocationQuery) {
  return parsePositiveInteger(query.detailPage, 1);
}

export function routeDetailPageSize(query: LocationQuery, fallback: number) {
  return parsePositiveInteger(query.detailPageSize, fallback);
}

export function routeDetailSortBy(query: LocationQuery) {
  return String(query.detailSortBy ?? 'syncedAt');
}

export function routeDetailSortOrder(query: LocationQuery) {
  const value = String(query.detailSortOrder ?? 'descending');
  return value === 'ascending' || value === 'descending' ? value : 'descending';
}

export function routeDetailVisible(query: LocationQuery) {
  return String(query.detailVisible ?? '') === '1';
}

export function buildFilterGroupFromRouteQuery(query: LocationQuery) {
  const draftGroup = createEmptyFilterGroup();
  draftGroup.logic = query.filterLogic === 'OR' ? 'OR' : 'AND';
  const keyedConditions = new Map<number, StatisticFilterConditionDraft>();

  for (const [key, rawValue] of Object.entries(query)) {
    const match = /^filters\.(\d+)\.(field|operator|value|value2)$/.exec(key);
    if (!match) {
      continue;
    }
    const index = Number.parseInt(match[1], 10);
    const nextValue = Array.isArray(rawValue) ? String(rawValue[0] ?? '') : String(rawValue ?? '');
    const currentCondition =
      keyedConditions.get(index) ?? {
        id: nextConditionId(),
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

export function buildFilterQueryPatch(query: LocationQuery, filterDraft: Pick<StatisticFilterDraftGroup, 'logic' | 'conditions'>) {
  const patch: Record<string, string | number | null> = {
    filterLogic: filterDraft.conditions.length ? filterDraft.logic : null,
  };
  for (const key of Object.keys(query)) {
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

export function mergeRouteQuery(query: LocationQuery, patch: Record<string, string | number | null | undefined>) {
  const nextQuery = { ...query } as Record<string, string>;
  for (const [key, value] of Object.entries(patch)) {
    if (value == null || value === '') {
      delete nextQuery[key];
    } else {
      nextQuery[key] = String(value);
    }
  }
  return nextQuery;
}
