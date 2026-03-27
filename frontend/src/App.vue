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
import {
  api,
  type GitlabSyncConfig,
  type GitlabSyncLog,
  type GitlabSyncTask,
  type MirrorStatusResponse,
  type SyncProgress,
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
        description: '基于当前 GitLab 镜像数据，展示标准字段的汇总统计与明细下钻。',
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
        description: '当前模块为空，可在后续接入新的统计表。',
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
        description: '当前模块为空，可在后续接入新的统计表。',
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
        description: '当前模块为空，可在后续接入新的统计表。',
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
        description: '当前模块为空，可在后续接入新的统计表。',
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
        description: '当前模块为空，可在后续接入新的统计表。',
      },
    ],
  },
  {
    key: 'system-settings',
    label: '系统设置',
    icon: Setting,
    title: '系统设置',
    description: '维护 GitLab 数据镜像、配置与模块管理。',
    pages: [
      {
        key: 'mirror-settings',
        label: '数据镜像设置',
        description: '管理 GitLab 数据镜像的连接、同步和日志。',
      },
      {
        key: 'module-management',
        label: '模块管理',
        description: '预留模块和菜单管理功能。',
      },
    ],
  },
];

const activeModuleKey = ref<ModuleKey>('quality-board');
const activePageKey = ref<PageKey>('quality-board-home');
const loading = ref(false);
const saving = ref(false);
const syncing = ref(false);
const cancelling = ref(false);
const status = ref<MirrorStatusResponse | null>(null);
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
const whitelistOptions = computed(() => status.value?.whitelistOptions ?? []);
const recommendedCount = computed(() => whitelistOptions.value.filter((item) => item.recommended).length);
const isDockerMode = computed(() => form.value.sourceMode === 'DOCKER');
const progress = computed<SyncProgress | null>(() => status.value?.progress ?? null);
const currentTask = computed<GitlabSyncTask | null>(() => status.value?.currentTask ?? null);
const recentLogs = computed(() => status.value?.logs ?? []);
const latestLog = computed(() => recentLogs.value[0] ?? null);
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
    return { text: '等待执行', type: 'warning' as const };
  }
  if (raw === 'CANCELLING') {
    return { text: '停止中', type: 'warning' as const };
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
      return '已有同步任务正在执行，当前任务已进入队列等待。';
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

async function loadStatus(showError = true) {
  loading.value = true;
  try {
    const data = await api.getStatus();
    status.value = data;
    form.value = {
      ...data.config,
      name: data.config.name || 'GitLab 默认数据源',
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
    await api.saveConfig(form.value);
    if (showSuccess) {
      ElMessage.success('配置已保存');
    }
    await loadStatus(false);
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
    await loadStatus(false);
  } catch (error) {
    ElMessage.error((error as Error).message);
  }
}

async function startFullSync() {
  syncing.value = true;
  try {
    await saveConfig(false);
    await api.startFullSync();
    ElMessage.success('首次全量同步已开始');
    await loadStatus(false);
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
    await api.startIncrementalSync();
    ElMessage.success('增量同步已开始');
    await loadStatus(false);
  } catch (error) {
    ElMessage.error((error as Error).message);
  } finally {
    syncing.value = false;
  }
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
    await loadStatus(false);
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
    default:
      return statusValue;
  }
}

function syncTypeText(syncType: string) {
  switch (syncType) {
    case 'FULL':
      return '全量同步';
    case 'INCREMENTAL':
      return '增量同步';
    case 'COMPENSATION':
      return '补偿同步';
    case 'WEBHOOK':
      return 'Webhook';
    default:
      return syncType;
  }
}

onMounted(async () => {
  await loadStatus(false);
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
            <el-button v-if="showingMirrorSettings" :icon="Refresh" @click="loadStatus()">刷新</el-button>
          </div>
        </section>

        <template v-if="showingStatisticBoard">
          <StatisticBoardView board-key="mirror-table-overview" />
        </template>

        <template v-else-if="showingMirrorSettings">
          <div class="settings-grid">
            <el-card shadow="never" class="panel-card" v-loading="loading">
              <template #header>
                <div class="panel-header">
                  <div>
                    <div class="panel-title">GitLab 数据镜像设置</div>
                    <div class="panel-caption">全量靠 DB，增量靠 Webhook，一致性靠补偿。</div>
                  </div>
                  <el-tag type="primary" effect="dark">数据源设置</el-tag>
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

                <el-form-item label="启用数据源">
                  <el-switch v-model="form.enabled" />
                </el-form-item>
                <el-form-item label="自动同步">
                  <el-switch v-model="form.autoSyncEnabled" />
                </el-form-item>
                <el-form-item label="补偿间隔(分钟)">
                  <el-input-number v-model="form.compensationIntervalMinutes" :min="1" :max="720" />
                </el-form-item>
                <el-form-item label="白名单模式">
                  <el-radio-group v-model="form.whitelistMode">
                    <el-radio value="RECOMMENDED">推荐业务表（{{ recommendedCount }} 张）</el-radio>
                    <el-radio value="ALL">全部表</el-radio>
                    <el-radio value="CUSTOM">自定义白名单</el-radio>
                  </el-radio-group>
                </el-form-item>
                <el-form-item v-if="form.whitelistMode === 'CUSTOM'" label="自定义白名单">
                  <el-select v-model="form.whitelistTables" multiple filterable style="width: 100%">
                    <el-option
                      v-for="option in whitelistOptions"
                      :key="option.tableName"
                      :label="`${option.label} (${option.tableName})`"
                      :value="option.tableName"
                    />
                  </el-select>
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

                <el-space wrap>
                  <el-button type="primary" :loading="saving" @click="saveConfig()">保存配置</el-button>
                  <el-button :icon="Tools" @click="testConnection">测试连接</el-button>
                  <el-button type="success" :loading="syncing" :disabled="canCancel" @click="startFullSync">首次全量同步</el-button>
                  <el-button :loading="syncing" :disabled="canCancel" @click="startIncrementalSync">立即增量同步</el-button>
                  <el-button type="danger" plain :loading="cancelling" :disabled="!canCancel" @click="cancelSyncTask">
                    中止导入
                  </el-button>
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
                      <div class="panel-caption">快速确认最近一次全量、增量或补偿执行结果。</div>
                    </div>
                    <el-button link :icon="Refresh" @click="loadStatus()">刷新</el-button>
                  </div>
                </template>

                <el-table :data="recentLogs" size="small" border class="sync-log-table">
                  <el-table-column label="类型" width="110">
                    <template #default="{ row }">
                      <el-tag size="small" effect="plain">{{ syncTypeText(row.syncType) }}</el-tag>
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
                    <template #default="{ row }">{{ row.message || '-' }}</template>
                  </el-table-column>
                </el-table>
              </el-card>
            </div>
          </div>
        </template>

        <template v-else>
          <section class="blank-stage">
            <el-empty description="当前模块暂未接入统计表。" />
          </section>
        </template>
      </main>
    </div>
  </div>
</template>
