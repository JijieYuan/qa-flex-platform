<script setup lang="ts">
import { computed } from 'vue';
import { Delete, Plus } from '@element-plus/icons-vue';
import SmartSelect from '../base/SmartSelect.vue';
import type {
  CodeReviewRuleCondition,
  CodeReviewRuleConfig,
  CodeReviewRuleFieldDefinition,
} from '../../types/code-review-rule-config';
import {
  createCodeReviewRuleCondition,
  describeCodeReviewRuleCondition,
  listActiveCodeReviewRuleConditions,
  listInactiveCodeReviewRuleFields,
  setCodeReviewRuleConditions,
  usesConditionValue,
} from '../../views/code-review-rule-config-utils';

const props = defineProps<{
  modelValue: CodeReviewRuleConfig;
  fields: CodeReviewRuleFieldDefinition[];
}>();

const emit = defineEmits<{
  (event: 'update:modelValue', value: CodeReviewRuleConfig): void;
}>();

const activeRules = computed(() => listActiveCodeReviewRuleConditions(props.modelValue));
const inactiveFields = computed(() => listInactiveCodeReviewRuleFields(props.modelValue, props.fields));
const activeCountText = computed(() => `${activeRules.value.length} 条规则正在生效`);

function findField(fieldKey: string) {
  return props.fields.find((item) => item.key === fieldKey) ?? null;
}

function selectOptions(fieldKey: string) {
  return findField(fieldKey)?.options ?? [];
}

function updateConditions(conditions: CodeReviewRuleCondition[]) {
  emit('update:modelValue', setCodeReviewRuleConditions(props.modelValue, conditions));
}

function addRule(field: CodeReviewRuleFieldDefinition) {
  updateConditions([...activeRules.value, createCodeReviewRuleCondition(field)]);
}

function removeRule(conditionId: string) {
  updateConditions(activeRules.value.filter((condition) => condition.id !== conditionId));
}

function updateConditionValue(conditionId: string, value: string | string[]) {
  const nextValue = Array.isArray(value) ? value.join(',') : value;
  updateConditions(
    activeRules.value.map((condition) =>
      condition.id === conditionId
        ? {
            ...condition,
            value: nextValue,
          }
        : condition,
    ),
  );
}

function valuePlaceholder(condition: CodeReviewRuleCondition) {
  if (condition.operator === 'contains') {
    return '请输入风险词，例如：临时、绕过、测试';
  }
  if (condition.operator === 'lt') {
    return '请输入最低要求，例如：0.2';
  }
  if (condition.operator === 'gt') {
    return '请输入上限，例如：500';
  }
  return '请输入规则值';
}

function conditionValueModel(condition: CodeReviewRuleCondition) {
  const field = findField(condition.fieldKey);
  if (field?.type !== 'multi-select') {
    return condition.value;
  }
  return condition.value
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean);
}

function ruleSentence(condition: CodeReviewRuleCondition) {
  const field = findField(condition.fieldKey);
  if (!field) {
    return '这条规则暂时不可用，可以删除后重新添加。';
  }
  switch (condition.operator) {
    case 'isEmpty':
      return `${field.label}时，判定为非法数据`;
    case 'isMissingReview':
      return '没有形成有效代码走查记录时，判定为非法数据';
    case 'isNotScanned':
      return '明确标记为未完成代码扫描时，判定为非法数据';
    case 'hasOpenScanIssue':
      return '静态扫描问题数大于 0 时，判定为非法数据';
    case 'notIn':
      return '目标分支不在允许范围内时，判定为非法数据';
    case 'contains':
      return '合并内容包含风险词时，判定为非法数据';
    case 'lt':
      return `${field.label}时，低于填写值就判定为非法数据`;
    case 'gt':
      return `${field.label}时，高于填写值就判定为非法数据`;
    default:
      return '满足这条规则时，判定为非法数据';
  }
}
</script>

<template>
  <section class="rule-editor-shell">
    <header class="rule-editor-head">
      <div class="rule-editor-head-main">
        <h3 class="rule-editor-title">当前正在生效的规则</h3>
        <p class="rule-editor-subtitle">
          表格会按下面这些规则重新判定非法数据。关闭或新增规则后，右侧会立即预览结果变化。
        </p>
      </div>
      <div class="rule-editor-head-actions">
        <el-tag effect="plain" type="success">{{ activeCountText }}</el-tag>
        <el-popover placement="bottom-end" width="360" trigger="click">
          <template #reference>
            <el-button type="primary" plain :icon="Plus">添加规则</el-button>
          </template>
          <div class="rule-add-panel">
            <div class="rule-add-title">可添加的规则</div>
            <div v-if="inactiveFields.length" class="rule-add-list">
              <button
                v-for="field in inactiveFields"
                :key="field.key"
                type="button"
                class="rule-add-item"
                @click="addRule(field)"
              >
                <span>{{ field.label }}</span>
                <small>加入当前判定规则</small>
              </button>
            </div>
            <el-empty v-else description="所有可配置规则都已经在生效列表中。" :image-size="80" />
          </div>
        </el-popover>
      </div>
    </header>

    <div v-if="activeRules.length" class="rule-card-list">
      <article v-for="condition in activeRules" :key="condition.id" class="rule-card">
        <div class="rule-card-main">
          <div class="rule-card-kicker">已启用</div>
          <div class="rule-card-title">{{ findField(condition.fieldKey)?.label || '未知规则' }}</div>
          <div class="rule-card-sentence">{{ ruleSentence(condition) }}</div>
          <div class="rule-card-summary">{{ describeCodeReviewRuleCondition(condition, fields) }}</div>
        </div>

        <div class="rule-card-control">
          <SmartSelect
            v-if="findField(condition.fieldKey)?.type === 'multi-select' && usesConditionValue(condition.operator)"
            :model-value="conditionValueModel(condition)"
            :options="selectOptions(condition.fieldKey)"
            compact
            multiple
            collapse-tags
            placeholder="选择允许范围"
            @change="updateConditionValue(condition.id, $event as string | string[])"
          />

          <el-input
            v-else-if="usesConditionValue(condition.operator)"
            :model-value="condition.value"
            :placeholder="valuePlaceholder(condition)"
            @input="updateConditionValue(condition.id, String($event ?? ''))"
          />

          <div v-else class="rule-card-fixed">这条规则不需要填写额外值</div>
        </div>

        <el-button text :icon="Delete" class="rule-card-remove" @click="removeRule(condition.id)">
          停用
        </el-button>
      </article>
    </div>

    <el-empty
      v-else
      description="当前没有启用任何个人判定规则。你可以点击“添加规则”选择要生效的规则。"
    />
  </section>
</template>

<style scoped>
.rule-editor-shell {
  display: grid;
  gap: 16px;
}

.rule-editor-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.rule-editor-head-main {
  display: grid;
  gap: 6px;
}

.rule-editor-head-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.rule-editor-title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #1f2937;
}

.rule-editor-subtitle {
  margin: 0;
  font-size: 13px;
  line-height: 1.6;
  color: rgba(31, 41, 55, 0.64);
}

.rule-card-list {
  display: grid;
  gap: 12px;
}

.rule-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(220px, 320px) auto;
  gap: 14px;
  align-items: center;
  padding: 16px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 18px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(249, 250, 251, 0.96)),
    radial-gradient(circle at top left, rgba(219, 234, 254, 0.5), transparent 36%);
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.04);
}

.rule-card-main {
  display: grid;
  gap: 6px;
}

.rule-card-kicker {
  width: fit-content;
  padding: 2px 8px;
  border-radius: 999px;
  background: rgba(22, 163, 74, 0.1);
  color: #15803d;
  font-size: 11px;
  font-weight: 700;
}

.rule-card-title {
  font-size: 15px;
  font-weight: 700;
  color: #111827;
}

.rule-card-sentence {
  font-size: 13px;
  line-height: 1.6;
  color: rgba(31, 41, 55, 0.8);
}

.rule-card-summary {
  font-size: 12px;
  color: rgba(31, 41, 55, 0.56);
}

.rule-card-control {
  min-width: 0;
}

.rule-card-fixed {
  display: flex;
  align-items: center;
  min-height: 40px;
  padding: 0 12px;
  border-radius: 10px;
  border: 1px dashed rgba(148, 163, 184, 0.5);
  background: rgba(248, 250, 252, 0.9);
  font-size: 12px;
  color: rgba(71, 85, 105, 0.8);
}

.rule-card-remove {
  justify-self: end;
}

.rule-add-panel {
  display: grid;
  gap: 12px;
}

.rule-add-title {
  font-size: 14px;
  font-weight: 700;
  color: #111827;
}

.rule-add-list {
  display: grid;
  gap: 8px;
  max-height: 360px;
  overflow: auto;
}

.rule-add-item {
  display: grid;
  gap: 4px;
  width: 100%;
  padding: 12px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 12px;
  background: #fff;
  text-align: left;
  cursor: pointer;
  transition:
    border-color 0.18s ease,
    background-color 0.18s ease,
    transform 0.18s ease;
}

.rule-add-item:hover {
  border-color: rgba(37, 99, 235, 0.3);
  background: rgba(239, 246, 255, 0.72);
  transform: translateY(-1px);
}

.rule-add-item span {
  font-size: 13px;
  font-weight: 700;
  color: #111827;
}

.rule-add-item small {
  font-size: 12px;
  color: rgba(31, 41, 55, 0.56);
}

@media (max-width: 1200px) {
  .rule-card {
    grid-template-columns: 1fr;
  }

  .rule-card-remove {
    justify-self: start;
  }
}
</style>
