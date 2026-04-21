<script setup lang="ts">
import { computed } from 'vue';
import type { CodeReviewRulePreviewResponse } from '../../types/code-review-rule-config';

const props = defineProps<{
  loading: boolean;
  preview: CodeReviewRulePreviewResponse | null;
}>();

const retainedRateText = computed(() => `${(props.preview?.retainedRate ?? 0).toFixed(1)}%`);
const deltaText = computed(() => {
  const delta = props.preview?.deltaCount ?? 0;
  if (delta > 0) {
    return `比当前规则多判定 ${delta} 条`;
  }
  if (delta < 0) {
    return `比当前规则少判定 ${Math.abs(delta)} 条`;
  }
  return '与当前规则结果一致';
});
</script>

<template>
  <section class="rule-preview-shell" v-loading="loading">
    <header class="rule-preview-head">
      <div class="rule-preview-head-main">
        <h3 class="rule-preview-title">改完之后会怎样</h3>
        <p class="rule-preview-subtitle">这里会直接告诉你按这套规则会判定出多少条非法数据，以及它们为什么会被判定出来。</p>
      </div>
    </header>

    <template v-if="preview">
      <section class="rule-preview-summary">
        <article class="rule-preview-card">
          <span class="rule-preview-label">会判定为非法</span>
          <strong class="rule-preview-value">{{ preview.filteredTotal }}</strong>
        </article>
        <article class="rule-preview-card">
          <span class="rule-preview-label">当前范围总数</span>
          <strong class="rule-preview-value">{{ preview.baseTotal }}</strong>
        </article>
        <article class="rule-preview-card">
          <span class="rule-preview-label">判定比例</span>
          <strong class="rule-preview-value">{{ retainedRateText }}</strong>
        </article>
      </section>

      <section class="rule-preview-outcome">
        <div class="rule-preview-outcome-title">{{ deltaText }}</div>
        <div class="rule-preview-outcome-desc">
          当前规则指代码走查非法数据列表默认使用的判定口径；我的规则只改变判定结果，不改原始数据。
        </div>
      </section>

      <section class="rule-preview-samples">
        <div class="rule-preview-section-title">先看几条被判定出来的样本</div>
        <el-empty v-if="!preview.samples.length" description="当前规则还没有判定出非法数据，你可以继续调整规则项。" />

        <div v-else class="rule-preview-sample-list">
          <article v-for="sample in preview.samples" :key="`${sample.mergeRequestId}-${sample.mergeRequestIid}`" class="rule-preview-sample-card">
            <header class="rule-preview-sample-head">
              <div class="rule-preview-sample-title">MR #{{ sample.mergeRequestIid || '-' }}</div>
              <div class="rule-preview-sample-meta">
                <span>{{ sample.projectName || '-' }}</span>
                <span class="rule-preview-meta-dot" />
                <span>{{ sample.moduleName || '-' }}</span>
                <span class="rule-preview-meta-dot" />
                <span>{{ sample.targetBranch || '-' }}</span>
              </div>
            </header>

            <div class="rule-preview-sample-content">{{ sample.mergeRequestContent || '-' }}</div>

            <div class="rule-preview-sample-owner">标注责任人：{{ sample.owner || '-' }}</div>

            <div class="rule-preview-sample-reasons">
              <div class="rule-preview-sample-reason-label">为什么会被判定出来</div>
              <el-tag
                v-for="reason in sample.reasons"
                :key="reason"
                size="small"
                effect="plain"
                class="rule-preview-reason-tag"
              >
                {{ reason }}
              </el-tag>
            </div>
          </article>
        </div>
      </section>
    </template>
  </section>
</template>

<style scoped>
.rule-preview-shell {
  display: grid;
  gap: 16px;
  min-height: 240px;
}

.rule-preview-head-main {
  display: grid;
  gap: 6px;
}

.rule-preview-title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #1f2937;
}

.rule-preview-subtitle {
  margin: 0;
  font-size: 13px;
  line-height: 1.6;
  color: rgba(31, 41, 55, 0.64);
}

.rule-preview-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.rule-preview-card {
  display: grid;
  gap: 6px;
  padding: 16px;
  border-radius: 18px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(248, 250, 252, 0.96));
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.04);
}

.rule-preview-label {
  font-size: 12px;
  font-weight: 600;
  color: rgba(31, 41, 55, 0.54);
}

.rule-preview-value {
  font-size: 26px;
  line-height: 1;
  color: #0f172a;
}

.rule-preview-outcome {
  display: grid;
  gap: 6px;
  padding: 16px 18px;
  border-radius: 18px;
  background: linear-gradient(135deg, rgba(239, 246, 255, 0.92), rgba(255, 255, 255, 0.96));
  border: 1px solid rgba(59, 130, 246, 0.14);
}

.rule-preview-outcome-title {
  font-size: 16px;
  font-weight: 700;
  color: #1d4ed8;
}

.rule-preview-outcome-desc {
  font-size: 12px;
  line-height: 1.7;
  color: rgba(30, 64, 175, 0.76);
}

.rule-preview-section-title {
  font-size: 13px;
  font-weight: 700;
  color: rgba(15, 23, 42, 0.76);
}

.rule-preview-sample-list {
  display: grid;
  gap: 12px;
}

.rule-preview-sample-card {
  display: grid;
  gap: 10px;
  padding: 16px;
  border-radius: 16px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(255, 255, 255, 0.96);
}

.rule-preview-sample-head {
  display: grid;
  gap: 6px;
}

.rule-preview-sample-title {
  font-size: 14px;
  font-weight: 700;
  color: #111827;
}

.rule-preview-sample-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  font-size: 12px;
  color: rgba(31, 41, 55, 0.56);
}

.rule-preview-meta-dot {
  width: 4px;
  height: 4px;
  border-radius: 999px;
  background: rgba(31, 41, 55, 0.22);
}

.rule-preview-sample-content {
  font-size: 13px;
  line-height: 1.7;
  color: rgba(15, 23, 42, 0.84);
}

.rule-preview-sample-owner {
  font-size: 12px;
  color: rgba(31, 41, 55, 0.62);
}

.rule-preview-sample-reasons {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.rule-preview-sample-reason-label {
  font-size: 12px;
  font-weight: 600;
  color: rgba(31, 41, 55, 0.7);
}

.rule-preview-reason-tag {
  border-radius: 999px;
}

@media (max-width: 1200px) {
  .rule-preview-summary {
    grid-template-columns: 1fr;
  }
}
</style>
