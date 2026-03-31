import { createRouter, createWebHashHistory, type RouteLocationNormalized, type RouteRecordRaw } from 'vue-router';
import { pageByKey, type ModuleKey, type PageKey } from './navigation';
import { routerState } from './router-state';

import StatisticBoardPage from './views/StatisticBoardPage.vue';
import MirrorSettingsView from './views/MirrorSettingsView.vue';
import DatabaseBrowserView from './components/DatabaseBrowserView.vue';
import ModulePlaceholderView from './views/ModulePlaceholderView.vue';
import NotFoundView from './views/NotFoundView.vue';

declare module 'vue-router' {
  interface RouteMeta {
    moduleKey: ModuleKey;
    pageKey: PageKey;
    title: string;
    description: string;
    allowedQueryKeys?: string[];
    allowedQueryPrefixes?: string[];
    persistedQueryKeys?: string[];
  }
}

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/quality-board/home',
  },
  {
    path: '/quality-board/home',
    component: StatisticBoardPage,
    meta: {
      moduleKey: 'quality-board',
      pageKey: 'quality-board-home',
      title: pageByKey.get('quality-board-home')!.label,
      description: pageByKey.get('quality-board-home')!.description,
      allowedQueryKeys: ['sortBy', 'sortOrder', 'detailPage', 'detailPageSize', 'detailSortBy', 'detailSortOrder', 'detailVisible', 'detailRowKey', 'detailColumnKey', 'projectId'],
      allowedQueryPrefixes: ['filters.'],
      persistedQueryKeys: ['projectId'],
    },
  },
  {
    path: '/review-data/home',
    component: ModulePlaceholderView,
    meta: {
      moduleKey: 'review-data',
      pageKey: 'review-data-home',
      title: pageByKey.get('review-data-home')!.label,
      description: pageByKey.get('review-data-home')!.description,
      persistedQueryKeys: ['projectId'],
    },
  },
  {
    path: '/code-review/home',
    component: ModulePlaceholderView,
    meta: {
      moduleKey: 'code-review',
      pageKey: 'code-review-home',
      title: pageByKey.get('code-review-home')!.label,
      description: pageByKey.get('code-review-home')!.description,
      persistedQueryKeys: ['projectId'],
    },
  },
  {
    path: '/integration-test/home',
    component: ModulePlaceholderView,
    meta: {
      moduleKey: 'integration-test',
      pageKey: 'integration-test-home',
      title: pageByKey.get('integration-test-home')!.label,
      description: pageByKey.get('integration-test-home')!.description,
      persistedQueryKeys: ['projectId'],
    },
  },
  {
    path: '/question-metrics/home',
    component: ModulePlaceholderView,
    meta: {
      moduleKey: 'question-metrics',
      pageKey: 'question-metrics-home',
      title: pageByKey.get('question-metrics-home')!.label,
      description: pageByKey.get('question-metrics-home')!.description,
      persistedQueryKeys: ['projectId'],
    },
  },
  {
    path: '/customer-issues/home',
    component: ModulePlaceholderView,
    meta: {
      moduleKey: 'customer-issues',
      pageKey: 'customer-issues-home',
      title: pageByKey.get('customer-issues-home')!.label,
      description: pageByKey.get('customer-issues-home')!.description,
      persistedQueryKeys: ['projectId'],
    },
  },
  {
    path: '/system-settings/mirror-settings',
    component: MirrorSettingsView,
    meta: {
      moduleKey: 'system-settings',
      pageKey: 'mirror-settings',
      title: pageByKey.get('mirror-settings')!.label,
      description: pageByKey.get('mirror-settings')!.description,
      persistedQueryKeys: ['projectId'],
    },
  },
  {
    path: '/system-settings/database-browser',
    component: DatabaseBrowserView,
    meta: {
      moduleKey: 'system-settings',
      pageKey: 'database-browser',
      title: pageByKey.get('database-browser')!.label,
      description: pageByKey.get('database-browser')!.description,
      allowedQueryKeys: ['table', 'keyword', 'page', 'pageSize', 'sortBy', 'sortOrder', 'projectId'],
      persistedQueryKeys: ['projectId'],
    },
  },
  {
    path: '/system-settings/module-management',
    component: ModulePlaceholderView,
    meta: {
      moduleKey: 'system-settings',
      pageKey: 'module-management',
      title: pageByKey.get('module-management')!.label,
      description: pageByKey.get('module-management')!.description,
      persistedQueryKeys: ['projectId'],
    },
  },
  {
    path: '/:pathMatch(.*)*',
    component: NotFoundView,
    meta: {
      moduleKey: 'quality-board',
      pageKey: 'quality-board-home',
      title: '页面不存在',
      description: '访问的地址无效，已为你保留平台基础壳子。',
    },
  },
];

function sameStringArray(left: string[], right: string[]) {
  return left.length === right.length && left.every((value, index) => value === right[index]);
}

function normalizeQuery(to: RouteLocationNormalized, from?: RouteLocationNormalized) {
  const allowedKeys = to.meta.allowedQueryKeys ?? [];
  const allowedPrefixes = to.meta.allowedQueryPrefixes ?? [];
  const persistedKeys = to.meta.persistedQueryKeys ?? [];
  const moduleChanged = from != null && from.meta.moduleKey !== to.meta.moduleKey;
  const currentEntries = Object.entries(to.query);
  const normalizedEntries: Array<[string, string | string[]]> = [];

  for (const [key, rawValue] of currentEntries) {
    if (key.startsWith('temp_')) {
      continue;
    }
    const keyAllowed = allowedKeys.includes(key) || allowedPrefixes.some((prefix) => key.startsWith(prefix));
    if (moduleChanged && !persistedKeys.includes(key) && !keyAllowed) {
      continue;
    }
    if (!keyAllowed && !persistedKeys.includes(key)) {
      continue;
    }
    if (rawValue == null) {
      continue;
    }
    if (Array.isArray(rawValue)) {
      const nextValue = rawValue.map((item) => String(item)).filter(Boolean);
      if (nextValue.length) {
        normalizedEntries.push([key, nextValue]);
      }
    } else {
      const nextValue = String(rawValue).trim();
      if (nextValue) {
        normalizedEntries.push([key, nextValue]);
      }
    }
  }

  const nextQuery = Object.fromEntries(normalizedEntries);

  for (const key of persistedKeys) {
    const currentValue = nextQuery[key];
    if (typeof currentValue === 'string' && currentValue) {
      sessionStorage.setItem(`route-query:${key}`, currentValue);
      continue;
    }
    const storedValue = sessionStorage.getItem(`route-query:${key}`);
    if (storedValue) {
      nextQuery[key] = storedValue;
    }
  }

  const currentKeys = Object.keys(to.query).sort();
  const nextKeys = Object.keys(nextQuery).sort();
  if (!sameStringArray(currentKeys, nextKeys)) {
    return nextQuery;
  }

  for (const key of nextKeys) {
    const currentValue = to.query[key];
    const normalizedValue = nextQuery[key];
    if (Array.isArray(currentValue) && Array.isArray(normalizedValue)) {
      if (!sameStringArray(currentValue.map(String), normalizedValue.map(String))) {
        return nextQuery;
      }
      continue;
    }
    if (String(currentValue) !== String(normalizedValue)) {
      return nextQuery;
    }
  }

  return null;
}

const router = createRouter({
  history: createWebHashHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 };
  },
});

router.beforeEach((to, from) => {
  routerState.routeLoading = true;
  routerState.routeError = '';
  const normalizedQuery = normalizeQuery(to, from);
  if (normalizedQuery) {
    return {
      path: to.path,
      query: normalizedQuery,
      hash: to.hash,
      replace: true,
    };
  }
  return true;
});

router.afterEach(() => {
  routerState.routeLoading = false;
});

router.onError((error) => {
  routerState.routeLoading = false;
  routerState.routeError = error instanceof Error ? error.message : '页面加载失败，请稍后重试。';
});

export default router;
