import { ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

const TABLE_PAGE_SIZE_STORAGE_PREFIX = 'stat-board-page-size:';
const PAGE_SIZE_OPTIONS = [20, 50, 100, 200] as const;

function parsePositiveInteger(rawValue: unknown, fallback: number) {
  const parsed = Number.parseInt(String(rawValue ?? ''), 10);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback;
}

function isAllowedPageSize(value: number) {
  return PAGE_SIZE_OPTIONS.includes(value as (typeof PAGE_SIZE_OPTIONS)[number]);
}

export function useStatisticRoutePagination(boardKey: string) {
  const route = useRoute();
  const router = useRouter();

  function routeTablePage() {
    return parsePositiveInteger(route.query.tablePage, 1);
  }

  function tablePageSizeStorageKey() {
    return `${TABLE_PAGE_SIZE_STORAGE_PREFIX}${boardKey}`;
  }

  function readPersistedTablePageSize() {
    if (typeof window === 'undefined') {
      return 50;
    }
    const rawValue = window.sessionStorage.getItem(tablePageSizeStorageKey());
    const parsed = parsePositiveInteger(rawValue, 50);
    return isAllowedPageSize(parsed) ? parsed : 50;
  }

  function persistTablePageSize(value: number) {
    if (typeof window === 'undefined') {
      return;
    }
    window.sessionStorage.setItem(tablePageSizeStorageKey(), String(value));
  }

  function routeTablePageSize() {
    const fallback = readPersistedTablePageSize();
    const value = parsePositiveInteger(route.query.tablePageSize, fallback);
    return isAllowedPageSize(value) ? value : 50;
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

  const tableCurrentPage = ref(routeTablePage());
  const tablePageSize = ref(routeTablePageSize());

  function syncFromRoute() {
    tableCurrentPage.value = routeTablePage();
    tablePageSize.value = routeTablePageSize();
    persistTablePageSize(tablePageSize.value);
  }

  function handleTableCurrentChange(nextPage: number) {
    void replaceRouteQuery({
      tablePage: nextPage,
    });
  }

  function handleTableSizeChange(nextSize: number) {
    persistTablePageSize(nextSize);
    void replaceRouteQuery({
      tablePageSize: nextSize,
      tablePage: 1,
    });
  }

  function clampPageWithinBounds(totalRows: number) {
    const maxPage = Math.max(1, Math.ceil(totalRows / tablePageSize.value));
    if (tableCurrentPage.value > maxPage) {
      void replaceRouteQuery({
        tablePage: 1,
      });
    }
  }

  return {
    tableCurrentPage,
    tablePageSize,
    pageSizeOptions: [...PAGE_SIZE_OPTIONS],
    syncFromRoute,
    handleTableCurrentChange,
    handleTableSizeChange,
    clampPageWithinBounds,
  };
}
