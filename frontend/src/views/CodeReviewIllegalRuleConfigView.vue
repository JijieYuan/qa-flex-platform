<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue';
import { ArrowLeft, RefreshLeft, Select, Setting } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { useRoute, useRouter } from 'vue-router';
import CodeReviewRuleConfigEditor from '../components/rule-config/CodeReviewRuleConfigEditor.vue';
import CodeReviewRuleConfigPreview from '../components/rule-config/CodeReviewRuleConfigPreview.vue';
import {
  api,
  type CodeReviewIllegalRecordFilterOptionsResponse,
} from '../api';
import type { CodeReviewRuleConfig, CodeReviewRulePreviewResponse } from '../types/code-review-rule-config';
import { buildCodeReviewRuleFields } from './code-review-rule-config-schema';
import {
  loadStoredCodeReviewRuleConfig,
  saveStoredCodeReviewRuleConfig,
} from './code-review-rule-config-storage';
import {
  cloneCodeReviewRuleConfig,
  createDefaultCodeReviewRuleConfig,
  normalizeCodeReviewRuleConfig,
} from './code-review-rule-config-utils';

const router = useRouter();
const route = useRoute();

const filterOptions = ref<CodeReviewIllegalRecordFilterOptionsResponse>({
  requestTypes: [{ label: '合并请求', value: 'merge_request' }],
  repositoryNames: [],
  illegalTypes: [],
  targetBranches: [],
  mergedBys: [],
  moduleNames: [],
  projectNames: [],
});
const filterLoading = ref(false);
const previewLoading = ref(false);
const saving = ref(false);
const preview = ref<CodeReviewRulePreviewResponse | null>(null);
const draftConfig = ref<CodeReviewRuleConfig>(createDefaultCodeReviewRuleConfig());
const savedConfig = ref<CodeReviewRuleConfig>(createDefaultCodeReviewRuleConfig());
let previewTimer: number | null = null;

const fields = computed(() => buildCodeReviewRuleFields(filterOptions.value));
const dirty = computed(() => JSON.stringify(draftConfig.value) !== JSON.stringify(savedConfig.value));
const scopeTags = computed(() => {
  const tags: Array<{ label: string; value: string }> = [];
  const append = (label: string, key: string) => {
    const value = String(route.query[key] ?? '').trim();
    if (value) {
      tags.push({ label, value });
    }
  };
  append('代码库', 'repositoryName');
  append('非法类型', 'illegalType');
  append('关键字', 'keyword');
  append('项目', 'projectName');
  append('请求类型', 'requestType');
  append('目标分支', 'targetBranch');
  append('合并人', 'mergedBy');
  append('模块名', 'moduleName');
  append('合并请求编号', 'mergeRequestIid');
  append('责任人', 'owner');

  const mergedAtStart = String(route.query.mergedAtStart ?? '').trim();
  const mergedAtEnd = String(route.query.mergedAtEnd ?? '').trim();
  if (mergedAtStart || mergedAtEnd) {
    tags.push({
      label: '合并时间',
      value: [mergedAtStart || '不限', mergedAtEnd || '不限'].join(' ~ '),
    });
  }
  return tags;
});

const scopeDescription = computed(() => {
  if (!scopeTags.value.length) {
    return '当前会以“代码走查非法数据”页面的默认数据范围作为判定基础。';
  }
  return '当前预览会沿用你从列表页带过来的数据范围，再按“我的规则”重新判定哪些记录属于非法数据。';
});

async function loadFilterOptions() {
  filterLoading.value = true;
  try {
    filterOptions.value = await api.getCodeReviewIllegalRecordFilterOptions(route.query.projectId as string | undefined);
  } finally {
    filterLoading.value = false;
  }
}

function syncDraftFromStorage() {
  const stored = loadStoredCodeReviewRuleConfig();
  const normalized = normalizeCodeReviewRuleConfig(
    stored ?? createDefaultCodeReviewRuleConfig(fields.value[0]),
    fields.value,
  );
  draftConfig.value = normalized;
  savedConfig.value = cloneCodeReviewRuleConfig(normalized);
}

function schedulePreview() {
  if (previewTimer != null) {
    window.clearTimeout(previewTimer);
  }
  previewTimer = window.setTimeout(() => {
    previewTimer = null;
    void loadPreview();
  }, 250);
}

async function loadPreview() {
  previewLoading.value = true;
  try {
    preview.value = await api.previewCodeReviewIllegalRecordRuleConfig({
      projectId: route.query.projectId != null && route.query.projectId !== '' ? Number(route.query.projectId) : null,
      repositoryName: String(route.query.repositoryName ?? ''),
      mergedAtStart: String(route.query.mergedAtStart ?? ''),
      mergedAtEnd: String(route.query.mergedAtEnd ?? ''),
      keyword: String(route.query.keyword ?? ''),
      projectName: String(route.query.projectName ?? ''),
      requestType: String(route.query.requestType ?? ''),
      targetBranch: String(route.query.targetBranch ?? ''),
      mergedBy: String(route.query.mergedBy ?? ''),
      moduleName: String(route.query.moduleName ?? ''),
      illegalType: String(route.query.illegalType ?? ''),
      mergeRequestIid: String(route.query.mergeRequestIid ?? ''),
      owner: String(route.query.owner ?? ''),
      ruleConfig: {
        ...draftConfig.value,
        enabled: true,
      },
    });
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '规则配置预览失败');
  } finally {
    previewLoading.value = false;
  }
}

async function initialize() {
  try {
    await loadFilterOptions();
    syncDraftFromStorage();
    await loadPreview();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '规则配置页面加载失败');
  }
}

function handleReset() {
  draftConfig.value = createDefaultCodeReviewRuleConfig(fields.value[0]);
}

function handleSave() {
  saving.value = true;
  const nextConfig: CodeReviewRuleConfig = {
    ...cloneCodeReviewRuleConfig(draftConfig.value),
    enabled: false,
    updatedAt: new Date().toISOString(),
  };
  saveStoredCodeReviewRuleConfig(nextConfig);
  savedConfig.value = cloneCodeReviewRuleConfig(nextConfig);
  draftConfig.value = cloneCodeReviewRuleConfig(nextConfig);
  saving.value = false;
  ElMessage.success('我的规则已保存');
}

function handleApply() {
  const nextConfig: CodeReviewRuleConfig = {
    ...cloneCodeReviewRuleConfig(draftConfig.value),
    enabled: true,
    updatedAt: new Date().toISOString(),
  };
  saveStoredCodeReviewRuleConfig(nextConfig);
  savedConfig.value = cloneCodeReviewRuleConfig(nextConfig);
  draftConfig.value = cloneCodeReviewRuleConfig(nextConfig);
  ElMessage.success('已按我的规则重新展示当前列表');
  void router.push({
    path: '/code-review/illegal-records',
    query: {
      ...route.query,
      page: '1',
    },
  });
}

function handleBack() {
  void router.push({
    path: '/code-review/illegal-records',
    query: route.query,
  });
}

watch(
  draftConfig,
  () => {
    schedulePreview();
  },
  { deep: true },
);

onBeforeUnmount(() => {
  if (previewTimer != null) {
    window.clearTimeout(previewTimer);
  }
});

void initialize();
</script>

<template>
  <section class="rule-config-page">
    <header class="rule-config-page-head">
      <div class="rule-config-page-head-main">
        <el-button text :icon="ArrowLeft" @click="handleBack">返回非法数据列表</el-button>
        <div class="rule-config-page-kicker">规则配置</div>
        <h1 class="rule-config-page-title">代码走查判定规则工作台</h1>
        <p class="rule-config-page-desc">
          这里配置的是“我的判定规则”，不会改动原始数据。你可以先看右侧结果，再决定是否让非法数据列表按这套规则重新展示。
        </p>
      </div>
      <div class="rule-config-page-head-actions">
        <el-button :icon="RefreshLeft" @click="handleReset">恢复当前规则</el-button>
        <el-button :icon="Select" :loading="saving" @click="handleSave">保存我的规则</el-button>
        <el-button type="primary" :icon="Setting" @click="handleApply">按我的规则展示</el-button>
      </div>
    </header>

    <div class="rule-config-layout">
      <aside class="rule-config-sidebar">
        <section class="rule-config-panel">
          <div class="rule-config-panel-title">我正在配置什么</div>
          <div class="rule-config-status-list">
            <div class="rule-config-status-item">
              <span class="rule-config-status-label">当前状态</span>
              <strong class="rule-config-status-value">{{ dirty ? '有未保存修改' : '已与本地保存内容一致' }}</strong>
            </div>
            <div class="rule-config-status-item">
              <span class="rule-config-status-label">当前列表</span>
              <strong class="rule-config-status-value">{{ savedConfig.enabled ? '会按我的规则重新判定' : '仍按当前规则展示' }}</strong>
            </div>
            <div class="rule-config-status-item">
              <span class="rule-config-status-label">最近保存</span>
              <strong class="rule-config-status-value">{{ savedConfig.updatedAt ? savedConfig.updatedAt.replace('T', ' ').slice(0, 19) : '还没有保存过' }}</strong>
            </div>
          </div>
        </section>

        <section class="rule-config-panel" v-loading="filterLoading">
          <div class="rule-config-panel-title">当前数据范围</div>
          <p class="rule-config-panel-desc">{{ scopeDescription }}</p>
          <div class="rule-config-scope-tags">
            <el-tag v-for="tag in scopeTags" :key="`${tag.label}-${tag.value}`" effect="plain" class="rule-config-scope-tag">
              {{ tag.label }}：{{ tag.value }}
            </el-tag>
            <span v-if="!scopeTags.length" class="rule-config-scope-empty">当前没有额外数据范围限制。</span>
          </div>
        </section>
      </aside>

      <main class="rule-config-editor">
        <section class="rule-config-panel">
          <CodeReviewRuleConfigEditor v-model="draftConfig" :fields="fields" />
        </section>
      </main>

      <aside class="rule-config-preview">
        <section class="rule-config-panel">
          <CodeReviewRuleConfigPreview :loading="previewLoading" :preview="preview" />
        </section>
      </aside>
    </div>
  </section>
</template>

<style scoped>
.rule-config-page {
  display: grid;
  gap: 16px;
}

.rule-config-page-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 20px;
  border-radius: 22px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  background:
    radial-gradient(circle at top left, rgba(219, 234, 254, 0.72), transparent 30%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(248, 250, 252, 0.98));
  box-shadow:
    0 1px 2px rgba(15, 23, 42, 0.04),
    0 18px 36px rgba(15, 23, 42, 0.05);
}

.rule-config-page-head-main {
  display: grid;
  gap: 8px;
}

.rule-config-page-kicker {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.04em;
  color: rgba(37, 99, 235, 0.86);
}

.rule-config-page-title {
  margin: 0;
  font-size: 28px;
  line-height: 1.1;
  color: #111827;
}

.rule-config-page-desc {
  margin: 0;
  max-width: 760px;
  font-size: 14px;
  line-height: 1.7;
  color: rgba(31, 41, 55, 0.7);
}

.rule-config-page-head-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.rule-config-layout {
  display: grid;
  grid-template-columns: minmax(240px, 280px) minmax(0, 1.25fr) minmax(340px, 420px);
  gap: 16px;
  align-items: start;
}

.rule-config-sidebar,
.rule-config-editor,
.rule-config-preview {
  display: grid;
  gap: 16px;
}

.rule-config-panel {
  display: grid;
  gap: 14px;
  padding: 18px;
  border-radius: 22px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.99), rgba(249, 250, 251, 0.96));
  box-shadow:
    0 1px 2px rgba(15, 23, 42, 0.04),
    0 12px 28px rgba(15, 23, 42, 0.04);
}

.rule-config-panel-title {
  font-size: 15px;
  font-weight: 700;
  color: #111827;
}

.rule-config-panel-desc {
  margin: 0;
  font-size: 13px;
  line-height: 1.7;
  color: rgba(31, 41, 55, 0.64);
}

.rule-config-status-list {
  display: grid;
  gap: 12px;
}

.rule-config-status-item {
  display: grid;
  gap: 4px;
}

.rule-config-status-label {
  font-size: 12px;
  color: rgba(31, 41, 55, 0.52);
}

.rule-config-status-value {
  font-size: 14px;
  color: #111827;
}

.rule-config-scope-tags {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.rule-config-scope-tag {
  border-radius: 999px;
}

.rule-config-scope-empty {
  font-size: 12px;
  color: rgba(31, 41, 55, 0.56);
}

@media (max-width: 1400px) {
  .rule-config-layout {
    grid-template-columns: 1fr;
  }
}
</style>
