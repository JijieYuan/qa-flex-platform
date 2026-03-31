import { computed, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';

type QueryValue = string | number | undefined | null;
type QueryPatch = Record<string, QueryValue>;
type QueryMode = 'push' | 'replace';

function parsePositiveInteger(rawValue: unknown, fallback: number) {
  const parsed = Number.parseInt(String(rawValue ?? ''), 10);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback;
}

function parseSortOrder(rawValue: unknown) {
  return rawValue === 'asc' || rawValue === 'desc' ? rawValue : '';
}

export interface RouteTableStateOptions {
  defaults?: {
    page?: number;
    pageSize?: number;
    sortBy?: string;
    sortOrder?: 'asc' | 'desc' | '';
    keyword?: string;
  };
}

export function useRouteTableState(options: RouteTableStateOptions = {}) {
  const route = useRoute();
  const router = useRouter();
  const isTableLoading = ref(false);

  const page = computed(() => parsePositiveInteger(route.query.page, options.defaults?.page ?? 1));
  const pageSize = computed(() => parsePositiveInteger(route.query.pageSize, options.defaults?.pageSize ?? 20));
  const sortBy = computed(() => String(route.query.sortBy ?? options.defaults?.sortBy ?? ''));
  const sortOrder = computed(() => parseSortOrder(route.query.sortOrder ?? options.defaults?.sortOrder ?? ''));
  const keyword = computed(() => String(route.query.keyword ?? options.defaults?.keyword ?? ''));

  async function patchQuery(patch: QueryPatch, mode: QueryMode = 'replace') {
    const nextQuery = { ...route.query } as Record<string, string>;
    for (const [key, rawValue] of Object.entries(patch)) {
      if (rawValue == null || rawValue === '') {
        delete nextQuery[key];
      } else {
        nextQuery[key] = String(rawValue);
      }
    }
    await router[mode]({
      path: route.path,
      query: nextQuery,
      hash: route.hash,
    });
  }

  function bindLoader(loader: () => Promise<void>) {
    watch(
      () => route.query,
      async () => {
        isTableLoading.value = true;
        try {
          await loader();
        } finally {
          isTableLoading.value = false;
        }
      },
      { immediate: true, deep: true },
    );
  }

  return {
    route,
    router,
    page,
    pageSize,
    sortBy,
    sortOrder,
    keyword,
    isTableLoading,
    patchQuery,
    bindLoader,
  };
}

