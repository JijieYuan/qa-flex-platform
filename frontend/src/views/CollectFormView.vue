<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useRoute } from 'vue-router';
import { api, type ExternalFormContext, type ExternalFormRecord, type ExternalFormSavePayload } from '../api';

type FormModel = {
  formTitle: string;
  reviewer: string;
  reviewDurationMinutes: number;
  specificationScore: number;
  logicScore: number;
  performanceScore: number;
  designScore: number;
  otherScore: number;
  remark: string;
};

const route = useRoute();

function queryText(value: unknown) {
  if (Array.isArray(value)) {
    return String(value[0] ?? '').trim();
  }
  return String(value ?? '').trim();
}

function queryNumber(value: unknown) {
  const raw = queryText(value);
  if (!raw) {
    return null;
  }
  const parsed = Number(raw);
  return Number.isFinite(parsed) ? parsed : null;
}

const context = computed<ExternalFormContext>(() => {
  const mrIid = queryNumber(route.query.mrIid);
  const resourceType = queryText(route.query.resourceType) || 'merge_request';
  const resourceId = queryText(route.query.resourceId) || (mrIid != null ? String(mrIid) : '');
  const templateCode = queryText(route.query.templateCode) || 'code_review';

  return {
    gitlabBaseUrl: queryText(route.query.gitlabBaseUrl),
    projectId: queryNumber(route.query.projectId) ?? 0,
    mrIid,
    resourceType,
    resourceId,
    templateCode,
  };
});

const formModel = reactive<FormModel>({
  formTitle: '代码走查表',
  reviewer: '',
  reviewDurationMinutes: 1,
  specificationScore: 0,
  logicScore: 0,
  performanceScore: 0,
  designScore: 0,
  otherScore: 0,
  remark: '',
});

const initialSnapshot = ref<FormModel | null>(null);
const recordState = ref<ExternalFormRecord | null>(null);
const loading = ref(false);
const saving = ref(false);
const deleting = ref(false);

const pageTitle = computed(() => (context.value.templateCode === 'code_review' ? '代码走查表' : '通用采集表'));
const contextReady = computed(() => Boolean(context.value.gitlabBaseUrl && context.value.projectId && context.value.resourceType && context.value.resourceId && context.value.templateCode));
const hasSavedRecord = computed(() => Boolean(recordState.value?.found && !recordState.value?.deleted));

function applyRecord(record: ExternalFormRecord) {
  recordState.value = record;
  formModel.formTitle = record.formTitle || pageTitle.value;
  formModel.reviewer = record.reviewer ?? '';
  formModel.reviewDurationMinutes = record.reviewDurationMinutes ?? 1;
  formModel.specificationScore = record.specificationScore ?? 0;
  formModel.logicScore = record.logicScore ?? 0;
  formModel.performanceScore = record.performanceScore ?? 0;
  formModel.designScore = record.designScore ?? 0;
  formModel.otherScore = record.otherScore ?? 0;
  formModel.remark = record.remark ?? '';
  initialSnapshot.value = snapshot();
}

function snapshot(): FormModel {
  return {
    formTitle: formModel.formTitle,
    reviewer: formModel.reviewer,
    reviewDurationMinutes: formModel.reviewDurationMinutes,
    specificationScore: formModel.specificationScore,
    logicScore: formModel.logicScore,
    performanceScore: formModel.performanceScore,
    designScore: formModel.designScore,
    otherScore: formModel.otherScore,
    remark: formModel.remark,
  };
}

function restoreInitial() {
  if (!initialSnapshot.value) {
    applyRecord({
      found: false,
      gitlabBaseUrl: context.value.gitlabBaseUrl,
      projectId: context.value.projectId,
      mrIid: context.value.mrIid,
      resourceType: context.value.resourceType,
      resourceId: context.value.resourceId,
      templateCode: context.value.templateCode,
      formTitle: pageTitle.value,
      reviewDurationMinutes: 1,
      specificationScore: 0,
      logicScore: 0,
      performanceScore: 0,
      designScore: 0,
      otherScore: 0,
      deleted: false,
    });
    return;
  }
  Object.assign(formModel, initialSnapshot.value);
}

async function loadDetail() {
  if (!contextReady.value) {
    return;
  }

  loading.value = true;
  try {
    const record = await api.getExternalFormDetail(context.value);
    applyRecord(record);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '表单上下文加载失败');
  } finally {
    loading.value = false;
  }
}

function validateBeforeSave() {
  if (!contextReady.value) {
    throw new Error('缺少必要的链接参数，请确认 gitlabBaseUrl、projectId 和资源编号是否完整。');
  }
  if (!formModel.reviewer.trim()) {
    throw new Error('请先填写走查人。');
  }
}

async function saveForm() {
  try {
    validateBeforeSave();
  } catch (error) {
    ElMessage.warning(error instanceof Error ? error.message : '表单校验失败');
    return;
  }

  const payload: ExternalFormSavePayload = {
    ...context.value,
    formTitle: formModel.formTitle.trim() || pageTitle.value,
    reviewer: formModel.reviewer.trim(),
    reviewDurationMinutes: formModel.reviewDurationMinutes,
    specificationScore: formModel.specificationScore,
    logicScore: formModel.logicScore,
    performanceScore: formModel.performanceScore,
    designScore: formModel.designScore,
    otherScore: formModel.otherScore,
    remark: formModel.remark.trim() || null,
  };

  saving.value = true;
  try {
    const record = await api.saveExternalForm(payload);
    applyRecord(record);
    ElMessage.success('表单已保存到数据采集平台数据库');
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败');
  } finally {
    saving.value = false;
  }
}

async function deleteForm() {
  if (!hasSavedRecord.value) {
    ElMessage.info('当前还没有已保存记录，无需删除。');
    return;
  }

  try {
    await ElMessageBox.confirm('此操作会将当前表单记录标记为删除状态，是否继续？', '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    });
  } catch {
    return;
  }

  deleting.value = true;
  try {
    const record = await api.deleteExternalForm(context.value);
    applyRecord(record);
    ElMessage.success('表单记录已删除');
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除失败');
  } finally {
    deleting.value = false;
  }
}

watch(
  () => context.value.templateCode,
  (templateCode) => {
    formModel.formTitle = templateCode === 'code_review' ? '代码走查表' : '通用采集表';
  },
  { immediate: true },
);

watch(
  () => JSON.stringify(context.value),
  () => {
    void loadDetail();
  },
);

onMounted(() => {
  void loadDetail();
});
</script>

<template>
  <div class="external-form-shell">
    <div class="external-form-page">
      <div class="external-form-hero">
        <div>
          <div class="external-form-eyebrow">独立表单链接入口</div>
          <h1 class="external-form-title">{{ pageTitle }}</h1>
          <p class="external-form-desc">
            当前页面会直接读取链接里的 GitLab 上下文参数，并将填写结果保存到数据采集平台数据库。
          </p>
        </div>
        <div class="external-form-hero-meta">
          <el-tag type="primary" effect="plain">{{ context.templateCode }}</el-tag>
          <el-tag v-if="hasSavedRecord" type="success" effect="plain">已保存</el-tag>
          <el-tag v-else type="info" effect="plain">待填写</el-tag>
        </div>
      </div>

      <el-card class="external-form-card" shadow="never">
        <template #header>
          <div class="external-form-card-title">上下文信息</div>
        </template>

        <div class="external-context-grid">
          <div class="external-context-item">
            <span class="external-context-label">GitLab 来源地址</span>
            <span class="external-context-value">{{ context.gitlabBaseUrl || '-' }}</span>
          </div>
          <div class="external-context-item">
            <span class="external-context-label">Project ID</span>
            <span class="external-context-value">{{ context.projectId || '-' }}</span>
          </div>
          <div class="external-context-item">
            <span class="external-context-label">MR IID</span>
            <span class="external-context-value">{{ context.mrIid ?? '-' }}</span>
          </div>
          <div class="external-context-item">
            <span class="external-context-label">资源类型</span>
            <span class="external-context-value">{{ context.resourceType || '-' }}</span>
          </div>
          <div class="external-context-item">
            <span class="external-context-label">资源编号</span>
            <span class="external-context-value">{{ context.resourceId || '-' }}</span>
          </div>
        </div>
      </el-card>

      <el-card class="external-form-card" shadow="never" v-loading="loading">
        <template #header>
          <div class="external-form-card-header">
            <div class="external-form-card-title">模板内容</div>
            <div class="external-form-actions">
              <el-button @click="restoreInitial">重置</el-button>
              <el-button type="danger" plain :loading="deleting" @click="deleteForm">删除</el-button>
              <el-button type="primary" :loading="saving" @click="saveForm">保存</el-button>
            </div>
          </div>
        </template>

        <el-form label-position="top" class="external-form-layout">
          <div class="external-form-grid">
            <el-form-item label="走查人">
              <el-input v-model="formModel.reviewer" placeholder="请输入走查人" />
            </el-form-item>
            <el-form-item label="走查时间（分钟）">
              <el-input-number
                v-model="formModel.reviewDurationMinutes"
                :min="0"
                :step="1"
                controls-position="right"
              />
            </el-form-item>
          </div>

          <div class="external-score-grid">
            <el-form-item label="规范">
              <el-input-number v-model="formModel.specificationScore" :min="0" :step="1" controls-position="right" />
            </el-form-item>
            <el-form-item label="逻辑">
              <el-input-number v-model="formModel.logicScore" :min="0" :step="1" controls-position="right" />
            </el-form-item>
            <el-form-item label="性能">
              <el-input-number v-model="formModel.performanceScore" :min="0" :step="1" controls-position="right" />
            </el-form-item>
            <el-form-item label="设计">
              <el-input-number v-model="formModel.designScore" :min="0" :step="1" controls-position="right" />
            </el-form-item>
            <el-form-item label="其他">
              <el-input-number v-model="formModel.otherScore" :min="0" :step="1" controls-position="right" />
            </el-form-item>
          </div>

          <el-form-item label="备注">
            <el-input v-model="formModel.remark" type="textarea" :rows="4" placeholder="请输入补充说明" />
          </el-form-item>

          <div class="external-form-footnote">
            当前页已支持按链接上下文查询、保存和删除表单记录。后续如果需要，还可以继续接入 GitLab 评论发布链路。
          </div>
        </el-form>
      </el-card>
    </div>
  </div>
</template>
