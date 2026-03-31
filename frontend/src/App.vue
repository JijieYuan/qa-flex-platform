<script setup lang="ts">
import { Loading, Plus } from '@element-plus/icons-vue';
import { computed } from 'vue';
import { RouterView, useRoute, useRouter } from 'vue-router';
import { modules, moduleByKey } from './navigation';
import { routerState } from './router-state';

const route = useRoute();
const router = useRouter();

const activeModule = computed(
  () => moduleByKey.get((route.meta.moduleKey as never) ?? 'quality-board') ?? modules[0],
);
const activePageKey = computed(() => String(route.meta.pageKey ?? activeModule.value.pages[0]?.key ?? ''));
const pageTitle = computed(() => String(route.meta.title ?? activeModule.value.pages[0]?.label ?? '数据平台'));
const pageDescription = computed(
  () => String(route.meta.description ?? activeModule.value.pages[0]?.description ?? '统一的数据采集、镜像与统计分析平台。'),
);

function openModule(moduleKey: string) {
  const targetModule = moduleByKey.get(moduleKey as never);
  if (!targetModule?.pages.length) {
    return;
  }
  void router.push(targetModule.pages[0].path);
}

function openPage(path: string) {
  void router.push(path);
}
</script>

<template>
  <div class="app-shell">
    <header class="shell-header">
      <div class="brand-wrap">
        <div class="brand-mark">DC</div>
        <div class="brand-copy">
          <div class="brand-title">Data Collection Platform</div>
          <div class="brand-subtitle">统一的数据采集、镜像与统计分析平台</div>
        </div>
      </div>

      <nav class="top-nav">
        <button
          v-for="module in modules"
          :key="module.key"
          class="top-nav-item"
          :class="{ active: activeModule.key === module.key }"
          @click="openModule(module.key)"
        >
          {{ module.label }}
        </button>
      </nav>

      <div class="header-actions">
        <el-tag v-if="routerState.routeLoading" type="warning" round>页面切换中</el-tag>
        <el-tag v-else-if="routerState.routeError" type="danger" round>连接异常</el-tag>
        <el-button class="ghost-button" :icon="Plus">添加菜单项</el-button>
      </div>
    </header>

    <div class="shell-body">
      <aside class="shell-sidebar">
        <div class="sidebar-title">
          <component :is="activeModule.icon" class="sidebar-title-icon" />
          <span>{{ activeModule.title }}</span>
        </div>

        <div class="sidebar-menu">
          <button
            v-for="page in activeModule.pages"
            :key="page.key"
            class="sidebar-menu-item"
            :class="{ active: activePageKey === page.key }"
            @click="openPage(page.path)"
          >
            {{ page.label }}
          </button>
        </div>

        <button class="sidebar-add-button">
          <el-icon><Plus /></el-icon>
          <span>添加菜单项</span>
        </button>
      </aside>

      <main class="shell-content">
        <section class="content-head">
          <div>
            <div class="content-title-row">
              <h1 class="content-title">{{ pageTitle }}</h1>
            </div>
            <p class="content-description">{{ pageDescription }}</p>
          </div>

          <div class="content-head-actions">
            <el-alert
              v-if="routerState.routeError"
              type="error"
              :closable="false"
              title="页面资源加载失败，当前保留基础壳子，请稍后重试。"
            />
          </div>
        </section>

        <RouterView v-slot="{ Component }">
          <component :is="Component" />
        </RouterView>

        <div v-if="routerState.routeLoading" class="route-loading-mask">
          <el-icon class="is-loading"><Loading /></el-icon>
          <span>页面切换中，请稍候…</span>
        </div>
      </main>
    </div>
  </div>
</template>
