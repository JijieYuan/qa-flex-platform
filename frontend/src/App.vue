<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { api, type GitlabSyncConfig, type MirrorStatusResponse } from './api';

const loading = ref(false);
const saving = ref(false);
const syncing = ref(false);
const status = ref<MirrorStatusResponse | null>(null);

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

const whitelistOptions = computed(() => status.value?.whitelistOptions ?? []);
const recommendedCount = computed(() => whitelistOptions.value.filter((item) => item.recommended).length);
const isDockerMode = computed(() => form.value.sourceMode === 'DOCKER');

async function loadStatus() {
  loading.value = true;
  try {
    const data = await api.getStatus();
    status.value = data;
    form.value = {
      ...data.config,
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
    ElMessage.error((error as Error).message);
  } finally {
    loading.value = false;
  }
}

async function saveConfig(showSuccess = true) {
  saving.value = true;
  try {
    await api.saveConfig(form.value);
    if (showSuccess) {
      ElMessage.success('配置已保存');
    }
    await loadStatus();
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
    const result = await api.testConnection();
    ElMessage.success(result.message ?? '连接测试成功');
    await loadStatus();
  } catch (error) {
    ElMessage.error((error as Error).message);
  }
}

async function startFullSync() {
  syncing.value = true;
  try {
    await saveConfig(false);
    const result = await api.startFullSync();
    ElMessage.success(result.message ?? '首次全量同步已开始');
    await loadStatus();
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
    ElMessage.success(result.message ?? '增量同步已开始');
    await loadStatus();
  } catch (error) {
    ElMessage.error((error as Error).message);
  } finally {
    syncing.value = false;
  }
}

onMounted(async () => {
  await loadStatus();
  window.setInterval(loadStatus, 10000);
});
</script>

<template>
  <div class="page">
    <el-page-header content="GitLab 数据镜像设置" />
    <div class="grid">
      <el-card shadow="never" class="main-card" v-loading="loading">
        <template #header>
          <div class="card-header">
            <span>数据源设置</span>
            <el-tag type="success">全量靠 DB / 增量靠 Webhook / 一致性靠补偿</el-tag>
          </div>
        </template>

        <el-form label-width="150px">
          <el-form-item label="数据源名称">
            <el-input v-model="form.name" />
          </el-form-item>

          <el-divider>源数据库模式</el-divider>

          <el-form-item label="读取方式">
            <el-radio-group v-model="form.sourceMode">
              <el-radio value="DOCKER">Docker 模式（推荐当前 GitLab 环境）</el-radio>
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
              title="Docker 模式会通过 docker exec 进入 GitLab 容器内部读取 PostgreSQL，不需要外部数据库密码。"
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
            <el-input-number v-model="form.compensationIntervalMinutes" :min="1" :max="1440" />
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
            <el-button @click="testConnection">测试连接</el-button>
            <el-button type="success" :loading="syncing" @click="startFullSync">首次全量同步</el-button>
            <el-button :loading="syncing" @click="startIncrementalSync">立即增量同步</el-button>
          </el-space>
        </el-form>
      </el-card>

      <div class="side-panel">
        <el-card shadow="never" class="side-card">
          <template #header>
            <div class="card-header">
              <span>当前状态</span>
              <el-tag :type="status?.currentStatus === 'RUNNING' ? 'warning' : 'success'">
                {{ status?.currentStatus || 'IDLE' }}
              </el-tag>
            </div>
          </template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="当前消息">{{ status?.currentMessage || '-' }}</el-descriptions-item>
            <el-descriptions-item label="上次全量同步">{{ status?.config?.lastFullSyncAt || '-' }}</el-descriptions-item>
            <el-descriptions-item label="上次增量同步">{{ status?.config?.lastIncrementalSyncAt || '-' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <el-card shadow="never" class="side-card">
          <template #header>
            <div class="card-header">
              <span>最近同步日志</span>
              <el-button link @click="loadStatus">刷新</el-button>
            </div>
          </template>
          <el-table :data="status?.logs || []" size="small" border>
            <el-table-column prop="syncType" label="类型" width="110" />
            <el-table-column prop="status" label="状态" width="110" />
            <el-table-column prop="tableCount" label="表数" width="80" />
            <el-table-column prop="recordCount" label="记录数" width="100" />
            <el-table-column prop="message" label="说明" min-width="180" show-overflow-tooltip />
            <el-table-column prop="startedAt" label="开始时间" min-width="180" />
          </el-table>
        </el-card>
      </div>
    </div>
  </div>
</template>
