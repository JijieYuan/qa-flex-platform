<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import {
  ArrowLeft,
  Bell,
  DataAnalysis,
  Document,
  Histogram,
  Monitor,
  Operation,
  Plus,
  Refresh,
  Setting,
  Tools,
} from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import StatisticBoardView from './components/StatisticBoardView.vue';
import DatabaseBrowserView from './components/DatabaseBrowserView.vue';
import {
  api,
  type GitlabSyncConfig,
  type GitlabSyncLog,
  type GitlabSyncTask,
  type MirrorPurgeScope,
  type MirrorStatusResponse,
  type MirrorPurgeResult,
  type SyncProgress,
  type SyncSubmissionResponse,
  type TableWhitelistOption,
} from './api';

type ModuleKey =
  | 'quality-board'
  | 'review-data'
  | 'code-review'
  | 'integration-test'
  | 'question-metrics'
  | 'customer-issues'
  | 'system-settings';

type PageKey =
  | 'quality-board-home'
  | 'review-data-home'
  | 'code-review-home'
  | 'integration-test-home'
  | 'question-metrics-home'
  | 'customer-issues-home'
  | 'mirror-settings'
  | 'database-browser'
  | 'module-management';

interface ShellPage {
  key: PageKey;
  label: string;
  description: string;
}

interface ShellModule {
  key: ModuleKey;
  label: string;
  icon: unknown;
  title: string;
  description: string;
  pages: ShellPage[];
}

const modules: ShellModule[] = [
  {
    key: 'quality-board',
    label: '统计分析',
    icon: Histogram,
    title: '统计分析',
    description: '统一承载汇总统计表与明细下钻视图。',
    pages: [
      {
        key: 'quality-board-home',
        label: '镜像表基础统计',
        description: '基于当前 GitLab 镜像数据展示汇总统计与明细下钻。',
      },
    ],
  },
  {
    key: 'review-data',
    label: '评审数据',
    icon: Document,
    title: '评审数据',
    description: '预留后续统计分析页面。',
    pages: [
      {
        key: 'review-data-home',
        label: '评审数据',
        description: '当前模块暂未接入统计表。',
      },
    ],
  },
  {
    key: 'code-review',
    label: '代码走查',
    icon: Operation,
    title: '代码走查',
    description: '预留后续统计分析页面。',
    pages: [
      {
        key: 'code-review-home',
        label: '代码走查',
        description: '当前模块暂未接入统计表。',
      },
    ],
  },
  {
    key: 'integration-test',
    label: '集成测试',
    icon: Monitor,
    title: '集成测试',
    description: '预留后续统计分析页面。',
    pages: [
      {
        key: 'integration-test-home',
        label: '集成测试',
        description: '当前模块暂未接入统计表。',
      },
    ],
  },
  {
    key: 'question-metrics',
    label: '议题统计',
    icon: DataAnalysis,
    title: '议题统计',
    description: '预留后续统计分析页面。',
    pages: [
      {
        key: 'question-metrics-home',
        label: '议题统计',
        description: '当前模块暂未接入统计表。',
      },
    ],
  },
  {
    key: 'customer-issues',
    label: '客户问题',
    icon: Bell,
    title: '客户问题',
    description: '预留后续统计分析页面。',
    pages: [
      {
        key: 'customer-issues-home',
        label: '客户问题',
        description: '当前模块暂未接入统计表。',
      },
    ],
  },
  {
    key: 'system-settings',
    label: '系统设置',
    icon: Setting,
    title: '系统设置',
    description: '维护 GitLab 数据镜像、数据库查看与系统模块配置。',
    pages: [
      {
        key: 'mirror-settings',
        label: '数据镜像设置',
        description: '管理 GitLab 数据镜像的连接、同步和日志。',
      },
      {
        key: 'database-browser',
        label: '数据库查看',
        description: '快速浏览本地平台数据库中的核心业务表数据。',
      },
      {
        key: 'module-management',
        label: '模块管理',
        description: '预留模块和菜单管理能力。',
      },
    ],
  },
];

const activeModuleKey = ref<ModuleKey>('quality-board');
const activePageKey = ref<PageKey>('quality-board-home');
const loading = ref(false);
const refreshing = ref(false);
const saving = ref(false);
const syncing = ref(false);
const cancelling = ref(false);
const registeringWebhook = ref(false);
const purging = ref<MirrorPurgeScope | null>(null);
const purgeDialogVisible = ref(false);
const purgeScope = ref<MirrorPurgeScope>('MIRROR_DATA_ONLY');
const purgeConfirmText = ref('');
const status = ref<MirrorStatusResponse | null>(null);
const webhookRegistrationState = ref<MirrorStatusResponse['webhookRegistration'] | null>(null);
const webhookRegistrationLoading = ref(false);
const whitelistOptions = ref<TableWhitelistOption[]>([]);
const whitelistOptionsLoading = ref(false);
const whitelistOptionsLoaded = ref(false);
const refreshTimer = ref<number | null>(null);

const form = ref<GitlabSyncConfig>({
  name: 'GitLab 默认数据源',
  enabled: true,
  autoSyncEnabled: true,
  sourceMode: 'DOCKER',
  whitelistMode: 'RECOMMENDED',
  whitelistTables: [],
  dbHost: 'localhost',
  dbPort: 5432,
  dbName: 'gitlabhq_production',
  dbUsername: 'gitlab',
  dbPassword: '',
  dockerContainerName: 'gitlab-data-web-1',
  webhookSecret: '',
  webhookProjectId: null,
  compensationIntervalMinutes: 10,
});

const activeModule = computed(() => modules.find((item) => item.key === activeModuleKey.value) ?? modules[0]);
const activePage = computed(() => activeModule.value.pages.find((item) => item.key === activePageKey.value) ?? activeModule.value.pages[0]);
const showingMirrorSettings = computed(() => activePageKey.value === 'mirror-settings');
const showingStatisticBoard = computed(() => activePageKey.value === 'quality-board-home');
const showingDatabaseBrowser = computed(() => activePageKey.value === 'database-browser');
const recommendedCount = computed(() => whitelistOptions.value.filter((item) => item.recommended).length);
const isDockerMode = computed(() => form.value.sourceMode === 'DOCKER');
const syncEnabled = computed(() => form.value.autoSyncEnabled);
const progress = computed<SyncProgress | null>(() => status.value?.progress ?? null);
const currentTask = computed<GitlabSyncTask | null>(() => status.value?.currentTask ?? null);
const recentLogs = computed(() => status.value?.logs ?? []);
const latestLog = computed(() => recentLogs.value[0] ?? null);
const webhookRegistration = computed(() => webhookRegistrationState.value ?? null);
const activePollingStatuses = ['PENDING', 'QUEUED', 'RUNNING', 'CANCELLING'];
const canCancel = computed(() => currentTask.value != null && activePollingStatuses.includes(currentTask.value.status));

const progressPercent = computed(() => {
  const current = progress.value;
  if (!current) {
    return 0;
  }
  if (current.totalTables <= 0) {
    return currentTask.value && activePollingStatuses.includes(currentTask.value.status) ? 5 : 0;
  }
  return Math.min(100, Math.round((current.completedTables / current.totalTables) * 100));
});

const displayStatus = computed(() => {
  const raw = status.value?.currentStatus ?? 'IDLE';
  if (raw === 'RUNNING') {
    return { text: '同步中', type: 'warning' as const };
  }
  if (raw === 'PENDING' || raw === 'QUEUED') {
    return { text: '排队中', type: 'warning' as const };
  }
  if (raw === 'CANCELLING') {
    return { text: '中止中', type: 'warning' as const };
  }
  if (raw === 'CANCELLED') {
    return { text: '已中止', type: 'info' as const };
  }
  if (raw === 'TIMEOUT') {
    return { text: '已超时', type: 'danger' as const };
  }
  const log = latestLog.value;
  if (log?.status === 'FAILED') {
    return { text: '最近一次失败', type: 'danger' as const };
  }
  if (log?.status === 'SUCCESS') {
    return { text: '最近一次成功', type: 'success' as const };
  }
  return { text: '空闲', type: 'info' as const };
});

const phaseText = computed(() => {
  const phase = progress.value?.phase;
  switch (phase) {
    case 'FULL_SYNC':
      return '首次全量同步';
    case 'INCREMENTAL_SYNC':
      return '增量同步';
    case 'COMPENSATION_SYNC':
      return '补偿同步';
    default:
      return currentTask.value?.status === 'QUEUED' ? '排队中' : '空闲';
  }
});

const progressHint = computed(() => {
  const current = progress.value;
  if (!current) {
    if (currentTask.value?.status === 'QUEUED') {
      return '当前已有同步任务执行中，本次请求已进入队列等待。';
    }
    if (currentTask.value?.status === 'CANCELLING') {
      return '已收到中止请求，正在等待当前批次安全退出。';
    }
    return '当前没有正在执行的同步任务。';
  }
  if (current.currentTable) {
    return `正在处理表 ${current.currentTable}，已同步 ${current.syncedRecords} 条记录。`;
  }
  return '同步任务已启动，正在准备表扫描。';
});

async function loadStatus(showError = true, blocking = true) {
  loading.value = blocking;
  try {
    const data = await api.getStatus();
    status.value = data;
    form.value = {
      ...data.config,
      name: data.config.name || 'GitLab 默认数据源',
      enabled: data.config.autoSyncEnabled ?? data.config.enabled ?? true,
      autoSyncEnabled: data.config.autoSyncEnabled ?? data.config.enabled ?? true,
      sourceMode: data.config.sourceMode ?? 'DOCKER',
      whitelistTables: data.config.whitelistTables ?? [],
      dockerContainerName: data.config.dockerContainerName ?? 'gitlab-data-web-1',
      dbHost: data.config.dbHost ?? 'localhost',
      dbPort: data.config.dbPort ?? 5432,
      dbName: data.config.dbName ?? 'gitlabhq_production',
      dbUsername: data.config.dbUsername ?? 'gitlab',
      dbPassword: data.config.dbPassword ?? '',
      webhookProjectId: data.config.webhookProjectId ?? null,
    };
  } catch (error) {
    if (showError) {
      ElMessage.error((error as Error).message);
    }
  } finally {
    loading.value = false;
  }
}

async function refreshStatus() {
  refreshing.value = true;
  try {
    await loadStatus(false, false);
    void loadWebhookRegistration(false);
  } finally {
    refreshing.value = false;
  }
}

async function loadWebhookRegistration(showError = false) {
  webhookRegistrationLoading.value = true;
  try {
    webhookRegistrationState.value = await api.getWebhookRegistrationStatus();
  } catch (error) {
    webhookRegistrationState.value = null;
    if (showError) {
      ElMessage.error((error as Error).message);
    }
  } finally {
    webhookRegistrationLoading.value = false;
  }
}

async function ensureWhitelistOptions(force = false) {
  if (whitelistOptionsLoading.value) {
    return;
  }
  if (!force && whitelistOptionsLoaded.value) {
    return;
  }
  whitelistOptionsLoading.value = true;
  try {
    whitelistOptions.value = await api.getWhitelistOptions();
    whitelistOptionsLoaded.value = true;
  } catch (error) {
    ElMessage.error((error as Error).message);
  } finally {
    whitelistOptionsLoading.value = false;
  }
}

function startRunningRefresh() {
  stopRunningRefresh();
  refreshTimer.value = window.setInterval(() => {
    void loadStatus(false);
  }, 4000);
}

function stopRunningRefresh() {
  if (refreshTimer.value != null) {
    window.clearInterval(refreshTimer.value);
    refreshTimer.value = null;
  }
}

watch(
  () => currentTask.value?.status,
  (nextStatus) => {
    if (nextStatus && activePollingStatuses.includes(nextStatus)) {
      startRunningRefresh();
    } else {
      stopRunningRefresh();
    }
  },
);

watch(
  () => form.value.whitelistMode,
  (nextMode) => {
    if (nextMode === 'CUSTOM') {
      void ensureWhitelistOptions();
    }
  },
);

function switchModule(moduleKey: ModuleKey) {
  activeModuleKey.value = moduleKey;
  activePageKey.value = modules.find((item) => item.key === moduleKey)?.pages[0]?.key ?? activePageKey.value;
}

function switchPage(pageKey: PageKey) {
  activePageKey.value = pageKey;
}

async function saveConfig(showSuccess = true) {
  saving.value = true;
  try {
    form.value.enabled = form.value.autoSyncEnabled;
    await api.saveConfig(form.value);
    if (showSuccess) {
      ElMessage.success('配置已保存');
    }
    await loadStatus(false, false);
    void loadWebhookRegistration(false);
  } catch (error) {
    ElMessage.error((error as Error).message);
    throw error;
  } finally {
    saving.value = false;
  }
}

async function testConnection() {
  try {
    await saveConfig(false);
    await api.testConnection();
    ElMessage.success('连接测试成功');
    await loadStatus(false, false);
  } catch (error) {
    ElMessage.error((error as Error).message);
  }
}

async function startFullSync() {
  syncing.value = true;
  try {
    await saveConfig(false);
    const result = await api.startFullSync();
    showSubmissionFeedback(result);
    await loadStatus(false, false);
  } catch (error) {
    ElMessage.error((error as Error).message);
  } finally {
    syncing.value = false;
  }
}


async function startIncrementalSync() {
  syncing.value = true;
  try {
    await saveConfig(false);
    const result = await api.startIncrementalSync();
    showSubmissionFeedback(result);
    await loadStatus(false, false);
  } catch (error) {
    ElMessage.error((error as Error).message);
  } finally {
    syncing.value = false;
  }
}

function showSubmissionFeedback(result: SyncSubmissionResponse) {
  if (result.action === 'CREATED') {
    ElMessage.success(result.message);
    return;
  }
  if (result.action === 'QUEUED') {
    ElMessage.warning(result.message);
    return;
  }
  ElMessage.info(result.message);
}

async function cancelSyncTask() {
  cancelling.value = true;
  try {
    const result = await api.cancelSync();
    if (result.accepted) {
      ElMessage.success('已提交中止请求');
    } else {
      ElMessage.info('当前没有可中止的任务');
    }
    await loadStatus(false, false);
  } catch (error) {
    ElMessage.error((error as Error).message);
  } finally {
    cancelling.value = false;
  }
}

function formatDateTime(value?: string | null) {
  if (!value) {
    return '-';
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return date.toLocaleString('zh-CN', { hour12: false });
}

function formatDuration(log: GitlabSyncLog) {
  if (!log.finishedAt || !log.startedAt) {
    return log.status === 'RUNNING' ? '进行中' : '-';
  }
  const start = new Date(log.startedAt).getTime();
  const end = new Date(log.finishedAt).getTime();
  if (Number.isNaN(start) || Number.isNaN(end)) {
    return '-';
  }
  const seconds = Math.max(0, Math.round((end - start) / 1000));
  if (seconds < 60) {
    return `${seconds} 秒`;
  }
  const minutes = Math.floor(seconds / 60);
  const remain = seconds % 60;
  return `${minutes} 分 ${remain} 秒`;
}

function logStatusType(statusValue: string) {
  switch (statusValue) {
    case 'SUCCESS':
      return 'success';
    case 'FAILED':
      return 'danger';
    case 'RUNNING':
      return 'warning';
    default:
      return 'info';
  }
}

function logStatusText(statusValue: string) {
  switch (statusValue) {
    case 'SUCCESS':
      return '成功';
    case 'FAILED':
      return '失败';
    case 'RUNNING':
      return '进行中';
    case 'CANCELLED':
      return '已中止';
    case 'TIMEOUT':
      return '超时';
    default:
      return statusValue;
  }
}

function syncTypeText(syncType: string) {
  switch (syncType) {
    case 'FULL':
      return '全量同步';
    case 'INCREMENTAL':
      return '手工恢复增量';
    case 'COMPENSATION':
      return '补偿同步';
    case 'WEBHOOK':
      return '精确更新';
    default:
      return syncType;
  }
}

async function registerWebhook() {
  registeringWebhook.value = true;
  try {
    await saveConfig(false);
    await api.registerWebhook();
    ElMessage.success('GitLab Webhook 已注册');
    await loadStatus(false, false);
    await loadWebhookRegistration(false);
  } catch (error) {
    ElMessage.error((error as Error).message);
  } finally {
    registeringWebhook.value = false;
  }
}

const purgeDialogCopy = computed(() => {
  if (purgeScope.value === 'MIRROR_DATA_EXCLUDING_CURRENT_WHITELIST') {
    return {
      title: '删除镜像数据（排除当前白名单）',
      confirmText: '删除白名单外镜像数据',
      detail:
        '将真实删除当前白名单之外的镜像表、镜像注册信息和旧镜像总表数据。当前白名单内的镜像数据会保留，GitLab 源端和本地非镜像数据不会受影响。',
    };
  }
  return {
    title: '删除镜像数据',
    confirmText: '删除镜像数据',
    detail:
      '将真实删除全部镜像表、镜像注册信息和旧镜像总表数据。GitLab 源端和本地非镜像数据不会受影响，此操作不可恢复。',
  };
});

const purgeConfirmMatched = computed(() => purgeConfirmText.value === purgeDialogCopy.value.confirmText);

function openPurgeDialog() {
  purgeScope.value = 'MIRROR_DATA_ONLY';
  purgeConfirmText.value = '';
  purgeDialogVisible.value = true;
}

function closePurgeDialog() {
  if (purging.value) {
    return;
  }
  purgeDialogVisible.value = false;
  purgeConfirmText.value = '';
}

async function purgeMirrorData() {
  if (!purgeConfirmMatched.value) {
    return;
  }

  purging.value = purgeScope.value;
  try {
    const result = await api.purgeMirrorData(purgeScope.value);
    ElMessage.success(buildPurgeMessage(result));
    await loadStatus(false, false);
    purgeDialogVisible.value = false;
    purgeConfirmText.value = '';
  } catch (error) {
    ElMessage.error((error as Error).message);
  } finally {
    purging.value = null;
  }
}

function buildPurgeMessage(result: MirrorPurgeResult) {
  if (result.scope === 'MIRROR_DATA_EXCLUDING_CURRENT_WHITELIST') {
    return `已真实删除当前白名单之外的镜像数据：删除 ${result.droppedMirrorTables} 张镜像表，清理 ${result.truncatedTables} 张镜像数据表。`;
  }
  return `已真实删除镜像数据：删除 ${result.droppedMirrorTables} 张镜像表，清理 ${result.truncatedTables} 张镜像数据表。`;
}

function syncTypeTagType(syncType: string) {
  switch (syncType) {
    case 'FULL':
      return 'warning';
    case 'INCREMENTAL':
      return 'info';
    case 'COMPENSATION':
      return 'success';
    case 'WEBHOOK':
      return '';
    default:
      return 'info';
  }
}

function syncLogMessage(log: GitlabSyncLog) {
  const message = (log.message || '').trim();
  switch (log.syncType) {
    case 'WEBHOOK':
      if (!message) {
        return 'Webhook 触发的业务对象精确更新。';
      }
      return message.replace(/^Triggered by webhook:\s*/i, 'Webhook 精确更新：');
    case 'INCREMENTAL':
      if (!message) {
        return '人工触发的恢复型时间窗口增量同步。';
      }
      return message
        .replace(/^Manual recovery incremental sync$/i, '人工触发的恢复型增量同步')
        .replace(/^Manual recovery incremental sync requested$/i, '人工触发的恢复型增量同步');
    case 'COMPENSATION':
      if (!message) {
        return '定时补偿窗口兜底同步。';
      }
      return message.replace(/^Scheduled compensation sync$/i, '定时补偿窗口兜底同步');
    case 'FULL':
      if (!message) {
        return '全量重建或初始化同步。';
      }
      return message.replace(/^Manual full sync$/i, '手工触发的全量同步');
    default:
      return message || '-';
  }
}

onMounted(async () => {
  await loadStatus(false, false);
  void loadWebhookRegistration(false);
});

onBeforeUnmount(() => {
  stopRunningRefresh();
});
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
          :class="{ active: activeModuleKey === module.key }"
          @click="switchModule(module.key)"
        >
          {{ module.label }}
        </button>
      </nav>

      <div class="header-actions">
        <el-tag :type="displayStatus.type" round>{{ displayStatus.text }}</el-tag>
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
            @click="switchPage(page.key)"
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
        <section v-if="!showingStatisticBoard" class="content-head">
          <div>
            <div class="content-title-row">
              <el-button
                v-if="showingMirrorSettings"
                class="back-button"
                circle
                :icon="ArrowLeft"
                @click="switchModule('quality-board')"
              />
              <h1 class="content-title">{{ activePage.label }}</h1>
            </div>
            <p class="content-description">{{ activePage.description }}</p>
          </div>

          <div class="content-head-actions">
            <el-button
              v-if="showingMirrorSettings"
              :icon="Refresh"
              :loading="refreshing"
              @click="refreshStatus"
            >
              刷新
            </el-button>
          </div>
        </section>

        <template v-if="showingStatisticBoard">
          <StatisticBoardView board-key="mirror-table-overview" />
        </template>

        <template v-else-if="showingMirrorSettings">
          <div class="settings-grid">
            <el-card shadow="never" class="panel-card">
              <template #header>
                <div class="panel-header">
                  <div>
                    <div class="panel-title">GitLab 数据镜像设置</div>
                    <div class="panel-caption">全量靠 DB，增量靠 Webhook，一致性靠补偿同步。</div>
                  </div>
                  <el-space>
                    <el-tag v-if="loading" type="info">正在加载</el-tag>
                    <el-tag type="primary" effect="dark">数据源设置</el-tag>
                  </el-space>
                </div>
              </template>

              <el-form label-width="150px">
                <el-form-item label="数据源名称">
                  <el-input v-model="form.name" />
                </el-form-item>

                <el-divider>源数据库模式</el-divider>

                <el-form-item label="读取方式">
                  <el-radio-group v-model="form.sourceMode">
                    <el-radio value="DOCKER">Docker 模式</el-radio>
                    <el-radio value="DIRECT">直连 PostgreSQL</el-radio>
                  </el-radio-group>
                </el-form-item>

                <template v-if="isDockerMode">
                  <el-form-item label="GitLab 容器名">
                    <el-input v-model="form.dockerContainerName" placeholder="例如 gitlab-data-web-1" />
                  </el-form-item>
                  <el-form-item label="数据库名称">
                    <el-input v-model="form.dbName" />
                  </el-form-item>
                  <el-form-item label="数据库用户名">
                    <el-input v-model="form.dbUsername" />
                  </el-form-item>
                  <el-alert
                    title="Docker 模式通过 docker exec 进入 GitLab 容器内部读取 PostgreSQL，不需要额外数据库密码。"
                    type="info"
                    :closable="false"
                    show-icon
                  />
                </template>

                <template v-else>
                  <el-row :gutter="16">
                    <el-col :span="12">
                      <el-form-item label="数据库主机">
                        <el-input v-model="form.dbHost" />
                      </el-form-item>
                    </el-col>
                    <el-col :span="12">
                      <el-form-item label="数据库端口">
                        <el-input-number v-model="form.dbPort" :min="1" :max="65535" style="width: 100%" />
                      </el-form-item>
                    </el-col>
                  </el-row>

                  <el-row :gutter="16">
                    <el-col :span="12">
                      <el-form-item label="数据库名称">
                        <el-input v-model="form.dbName" />
                      </el-form-item>
                    </el-col>
                    <el-col :span="12">
                      <el-form-item label="数据库用户名">
                        <el-input v-model="form.dbUsername" />
                      </el-form-item>
                    </el-col>
                  </el-row>

                  <el-form-item label="数据库密码">
                    <el-input v-model="form.dbPassword" type="password" show-password />
                  </el-form-item>
                </template>

                <el-divider>同步策略</el-divider>

                <el-form-item label="自动同步">
                  <el-switch v-model="form.autoSyncEnabled" />
                </el-form-item>
                <el-form-item label="补偿间隔(分钟)">
                  <el-input-number v-model="form.compensationIntervalMinutes" :min="1" :max="720" />
                </el-form-item>
                <el-form-item label="白名单模式">
                  <el-radio-group v-model="form.whitelistMode">
                    <el-radio value="RECOMMENDED">推荐业务表</el-radio>
                    <el-radio value="ALL">全部表</el-radio>
                    <el-radio value="CUSTOM">自定义白名单</el-radio>
                  </el-radio-group>
                </el-form-item>
                <el-form-item v-if="form.whitelistMode === 'CUSTOM'" label="自定义白名单">
                  <el-select
                    v-model="form.whitelistTables"
                    multiple
                    filterable
                    style="width: 100%"
                    :loading="whitelistOptionsLoading"
                    @visible-change="(visible:boolean) => visible && ensureWhitelistOptions()"
                  >
                    <el-option
                      v-for="option in whitelistOptions"
                      :key="option.tableName"
                      :label="`${option.label} (${option.tableName})`"
                      :value="option.tableName"
                    />
                  </el-select>
                  <div class="form-help-text">
                    {{
                      whitelistOptionsLoaded
                        ? `已加载 ${whitelistOptions.length} 张可选表，推荐表 ${recommendedCount} 张。`
                        : '进入自定义白名单时按需加载表选项，避免刷新设置页时等待。'
                    }}
                  </div>
                </el-form-item>

                <el-divider>Webhook 增量同步</el-divider>

                <el-form-item label="Webhook URL">
                  <el-input :model-value="status?.webhookUrl || ''" readonly />
                </el-form-item>
                <el-form-item label="Webhook Secret">
                  <el-input v-model="form.webhookSecret" />
                </el-form-item>
                <el-form-item label="GitLab Project ID">
                  <el-input-number v-model="form.webhookProjectId" :min="1" style="width: 100%" />
                </el-form-item>
                <el-form-item label="Webhook 状态">
                  <div class="webhook-status-line">
                    <el-tag
                      :type="webhookRegistrationLoading ? 'info' : webhookRegistration?.registered ? 'success' : webhookRegistration?.configured ? 'warning' : 'info'"
                      round
                    >
                      {{
                        webhookRegistrationLoading
                          ? '检测中'
                          : webhookRegistration?.registered
                          ? '已注册'
                          : webhookRegistration?.configured
                            ? '未注册'
                            : '未配置'
                      }}
                    </el-tag>
                    <span class="webhook-status-text">
                      {{
                        webhookRegistrationLoading
                          ? '正在异步检测 GitLab Webhook 状态，不影响页面其他信息加载。'
                          : webhookRegistration?.message || '尚未检测 GitLab Webhook 状态'
                      }}
                    </span>
                  </div>
                </el-form-item>

                <el-space wrap>
                  <el-button type="primary" :loading="saving" @click="saveConfig()">保存配置</el-button>
                  <el-button :icon="Tools" @click="testConnection">测试连接</el-button>
                  <el-button :loading="registeringWebhook" @click="registerWebhook">注册 Webhook</el-button>

                  <el-button type="success" :loading="syncing" :disabled="!syncEnabled" @click="startFullSync">首次全量同步</el-button>
                  <el-button :loading="syncing" :disabled="!syncEnabled" @click="startIncrementalSync">立即增量同步</el-button>
                  <el-button type="danger" plain :loading="cancelling" :disabled="!canCancel" @click="cancelSyncTask">
                    中止导入
                  </el-button>
                  <el-button type="danger" plain @click="openPurgeDialog">删除镜像数据</el-button>
                </el-space>
              </el-form>
            </el-card>

            <div class="settings-side-panel">
              <el-card shadow="never" class="panel-card progress-panel">
                <template #header>
                  <div class="panel-header">
                    <div>
                      <div class="panel-title">同步状态</div>
                      <div class="panel-caption">仅在排队、执行或中止过程中自动轮询刷新。</div>
                    </div>
                    <el-tag :type="displayStatus.type">{{ displayStatus.text }}</el-tag>
                  </div>
                </template>

                <div class="status-message">{{ status?.currentMessage || '当前没有正在执行的同步任务。' }}</div>

                <div class="progress-shell">
                  <div class="progress-head">
                    <div>
                      <div class="progress-title">同步进度</div>
                      <div class="progress-subtitle">{{ phaseText }}</div>
                    </div>
                    <div class="progress-percentage">{{ progressPercent }}%</div>
                  </div>
                  <el-progress
                    :percentage="progressPercent"
                    :stroke-width="18"
                    :status="currentTask?.status === 'RUNNING' || currentTask?.status === 'CANCELLING' ? undefined : 'success'"
                  />
                  <div class="progress-tip">{{ progressHint }}</div>
                  <div class="progress-meta-grid">
                    <div class="meta-item">
                      <span class="meta-label">当前表</span>
                      <span class="meta-value mono">{{ progress?.currentTable || '-' }}</span>
                    </div>
                    <div class="meta-item">
                      <span class="meta-label">表进度</span>
                      <span class="meta-value">{{ progress?.completedTables || 0 }}/{{ progress?.totalTables || 0 }}</span>
                    </div>
                    <div class="meta-item">
                      <span class="meta-label">已同步记录</span>
                      <span class="meta-value">{{ progress?.syncedRecords || 0 }}</span>
                    </div>
                    <div class="meta-item">
                      <span class="meta-label">开始时间</span>
                      <span class="meta-value">{{ formatDateTime(progress?.startedAt || status?.currentStartedAt) }}</span>
                    </div>
                  </div>
                </div>
              </el-card>

              <el-card shadow="never" class="panel-card">
                <template #header>
                  <div class="panel-header">
                    <div>
                      <div class="panel-title">最近同步日志</div>
                      <div class="panel-caption">快速确认最近一次全量、手工恢复增量、补偿或精确更新执行结果。</div>
                    </div>
                    <el-button link :icon="Refresh" :loading="refreshing" @click="refreshStatus">刷新</el-button>
                  </div>
                </template>

                <el-table :data="recentLogs" size="small" border class="sync-log-table">
                  <el-table-column label="类型" width="126">
                    <template #default="{ row }">
                      <el-tag size="small" effect="plain" :type="syncTypeTagType(row.syncType)">
                        {{ syncTypeText(row.syncType) }}
                      </el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column label="状态" width="96">
                    <template #default="{ row }">
                      <el-tag size="small" :type="logStatusType(row.status)">{{ logStatusText(row.status) }}</el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column prop="tableCount" label="表数" width="76" />
                  <el-table-column prop="recordCount" label="记录数" width="90" />
                  <el-table-column label="耗时" width="100">
                    <template #default="{ row }">{{ formatDuration(row) }}</template>
                  </el-table-column>
                  <el-table-column label="说明" min-width="220" show-overflow-tooltip>
                    <template #default="{ row }">{{ syncLogMessage(row) }}</template>
                  </el-table-column>
                </el-table>
              </el-card>
            </div>
          </div>
        </template>

        <template v-else-if="showingDatabaseBrowser">
          <DatabaseBrowserView />
        </template>

        <template v-else>
          <section class="blank-stage">
            <el-empty description="当前模块暂未接入统计表。" />
          </section>
        </template>
      </main>
    </div>
  </div>

  <el-dialog
    v-model="purgeDialogVisible"
    :title="purgeDialogCopy.title"
    width="680px"
    class="mirror-purge-dialog"
    :close-on-click-modal="false"
    :close-on-press-escape="!purging"
    @close="closePurgeDialog"
  >
    <div class="purge-dialog-body">
      <div class="purge-hero">
        <div class="purge-hero-badge">高风险操作</div>
        <div class="purge-hero-title">此操作会真实删除本地镜像数据，且不可恢复。</div>
        <div class="purge-hero-description">
          {{ purgeDialogCopy.detail }}
        </div>
      </div>

      <div class="purge-scope-cards">
        <label
          class="purge-scope-card"
          :class="{ active: purgeScope === 'MIRROR_DATA_ONLY' }"
        >
          <input v-model="purgeScope" type="radio" value="MIRROR_DATA_ONLY" />
          <div class="purge-scope-card-title">删除镜像数据</div>
          <div class="purge-scope-card-desc">
            删除所有镜像表、镜像注册信息和旧镜像总表数据，不影响 GitLab 源端和本地非镜像数据。
          </div>
        </label>

        <label
          class="purge-scope-card"
          :class="{ active: purgeScope === 'MIRROR_DATA_EXCLUDING_CURRENT_WHITELIST' }"
        >
          <input v-model="purgeScope" type="radio" value="MIRROR_DATA_EXCLUDING_CURRENT_WHITELIST" />
          <div class="purge-scope-card-title">删除镜像数据（排除当前设置的白名单）</div>
          <div class="purge-scope-card-desc">
            仅删除当前白名单之外的镜像数据，保留当前白名单内的镜像内容，不影响 GitLab 源端和本地非镜像数据。
          </div>
        </label>
      </div>

      <div class="purge-warning-list">
        <div class="purge-warning-item">删除前请确认当前没有运行中或排队中的同步任务。</div>
        <div class="purge-warning-item">本操作只作用于本地镜像数据，不会删除 GitLab 源端数据。</div>
        <div class="purge-warning-item">本地非镜像业务数据不会被删除。</div>
      </div>

      <div class="purge-confirm-panel">
        <div class="purge-confirm-label">请输入确认短语以继续</div>
        <div class="purge-confirm-phrase">{{ purgeDialogCopy.confirmText }}</div>
        <el-input v-model="purgeConfirmText" :placeholder="purgeDialogCopy.confirmText" />
      </div>
    </div>
    <template #footer>
      <el-button @click="closePurgeDialog">取消</el-button>
      <el-button
        type="danger"
        :loading="purging != null"
        :disabled="!purgeConfirmMatched"
        @click="purgeMirrorData()"
      >
        确认删除
      </el-button>
    </template>
  </el-dialog>
</template>


