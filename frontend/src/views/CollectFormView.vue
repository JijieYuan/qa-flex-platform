<script setup lang="ts">
import { computed, reactive, watch } from 'vue';
import { useRoute } from 'vue-router';

const route = useRoute();

function queryText(value: unknown) {
  if (Array.isArray(value)) {
    return String(value[0] ?? '').trim();
  }
  return String(value ?? '').trim();
}

const context = computed(() => {
  const gitlabBaseUrl = queryText(route.query.gitlabBaseUrl);
  const projectId = queryText(route.query.projectId);
  const mrIid = queryText(route.query.mrIid);
  const resourceType = queryText(route.query.resourceType) || 'merge_request';
  const resourceId = queryText(route.query.resourceId) || mrIid;
  const templateCode = queryText(route.query.templateCode) || 'code_review';

  return {
    gitlabBaseUrl,
    projectId,
    mrIid,
    resourceType,
    resourceId,
    templateCode,
  };
});

const formModel = reactive({
  formTitle: '代码走查表',
  reviewer: '',
  reviewDurationMinutes: 1,
  specification: 0,
  logic: 0,
  performance: 0,
  design: 0,
  other: 0,
  remark: '',
});

watch(
  () => context.value.templateCode,
  (templateCode) => {
    formModel.formTitle = templateCode === 'code_review' ? '代码走查表' : '通用采集表';
  },
  { immediate: true },
);
</script>

<template>
  <div class="external-form-shell">
    <div class="external-form-page">
      <div class="external-form-hero">
        <div>
          <div class="external-form-eyebrow">独立表单链接入口</div>
          <h1 class="external-form-title">{{ formModel.formTitle }}</h1>
          <p class="external-form-desc">当前版本用于承接 CI 机器人拼装后的链接，并展示固定模板。页面会直接从 URL 中读取上下文参数。</p>
        </div>
        <el-tag type="primary" effect="plain">{{ context.templateCode }}</el-tag>
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
            <span class="external-context-value">{{ context.mrIid || '-' }}</span>
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

      <el-card class="external-form-card" shadow="never">
        <template #header>
          <div class="external-form-card-title">模板内容</div>
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
              <el-input-number v-model="formModel.specification" :min="0" :step="1" controls-position="right" />
            </el-form-item>
            <el-form-item label="逻辑">
              <el-input-number v-model="formModel.logic" :min="0" :step="1" controls-position="right" />
            </el-form-item>
            <el-form-item label="性能">
              <el-input-number v-model="formModel.performance" :min="0" :step="1" controls-position="right" />
            </el-form-item>
            <el-form-item label="设计">
              <el-input-number v-model="formModel.design" :min="0" :step="1" controls-position="right" />
            </el-form-item>
            <el-form-item label="其他">
              <el-input-number v-model="formModel.other" :min="0" :step="1" controls-position="right" />
            </el-form-item>
          </div>

          <el-form-item label="备注">
            <el-input v-model="formModel.remark" type="textarea" :rows="4" placeholder="请输入补充说明" />
          </el-form-item>

          <div class="external-form-footnote">
            当前阶段只完成独立链接打开与模板展示，不接入提交、发布或存库逻辑。
          </div>
        </el-form>
      </el-card>
    </div>
  </div>
</template>
