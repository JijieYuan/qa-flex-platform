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
  cloneCodeReviewRuleConfig,
  createCodeReviewRuleCondition,
  createCodeReviewRuleGroup,
  describeCodeReviewRuleGroup,
  usesConditionValue,
} from '../../views/code-review-rule-config-utils';

const props = defineProps<{
  modelValue: CodeReviewRuleConfig;
  fields: CodeReviewRuleFieldDefinition[];
}>();

const emit = defineEmits<{
  (event: 'update:modelValue', value: CodeReviewRuleConfig): void;
}>();

const fieldOptions = computed(() => props.fields.map((field) => ({ label: field.label, value: field.key })));

function updateConfig(mutator: (draft: CodeReviewRuleConfig) => void) {
  const next = cloneCodeReviewRuleConfig(props.modelValue);
  mutator(next);
  emit('update:modelValue', next);
}

function findField(fieldKey: string) {
  return props.fields.find((item) => item.key === fieldKey) ?? null;
}

function selectOptions(fieldKey: string) {
  return findField(fieldKey)?.options ?? [];
}

function updateGroupMatchMode(groupId: string, matchMode: 'all' | 'any') {
  updateConfig((draft) => {
    const group = draft.groups.find((item) => item.id === groupId);
    if (group) {
      group.matchMode = matchMode;
    }
  });
}

function addGroup() {
  updateConfig((draft) => {
    draft.groups.push(createCodeReviewRuleGroup(props.fields[0]));
  });
}

function removeGroup(groupId: string) {
  updateConfig((draft) => {
    draft.groups = draft.groups.filter((group) => group.id !== groupId);
    if (!draft.groups.length) {
      draft.groups = [createCodeReviewRuleGroup(props.fields[0])];
    }
  });
}

function addCondition(groupId: string) {
  updateConfig((draft) => {
    const group = draft.groups.find((item) => item.id === groupId);
    if (group) {
      group.conditions.push(createCodeReviewRuleCondition(props.fields[0]));
    }
  });
}

function removeCondition(groupId: string, conditionId: string) {
  updateConfig((draft) => {
    const group = draft.groups.find((item) => item.id === groupId);
    if (!group) {
      return;
    }
    group.conditions = group.conditions.filter((condition) => condition.id !== conditionId);
    if (!group.conditions.length) {
      group.conditions = [createCodeReviewRuleCondition(props.fields[0])];
    }
  });
}

function updateConditionField(groupId: string, conditionId: string, fieldKey: string) {
  updateConfig((draft) => {
    const condition = findCondition(draft, groupId, conditionId);
    if (!condition) {
      return;
    }
    const field = findField(fieldKey);
    condition.fieldKey = fieldKey;
    condition.operator = field?.operators[0] ?? 'contains';
    condition.value = '';
  });
}

function updateConditionValue(groupId: string, conditionId: string, value: string | string[]) {
  updateConfig((draft) => {
    const condition = findCondition(draft, groupId, conditionId);
    if (condition) {
      condition.value = Array.isArray(value) ? value.join(',') : value;
    }
  });
}

function findCondition(draft: CodeReviewRuleConfig, groupId: string, conditionId: string): CodeReviewRuleCondition | null {
  const group = draft.groups.find((item) => item.id === groupId);
  return group?.conditions.find((condition) => condition.id === conditionId) ?? null;
}

function valuePlaceholder(condition: CodeReviewRuleCondition) {
  const field = findField(condition.fieldKey);
  if (condition.operator === 'contains') {
    return '请输入风险词，例如：临时、绕过、测试';
  }
  return field?.type === 'number' ? '请输入阈值数字' : '请输入规则值';
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

function ruleActionText(condition: CodeReviewRuleCondition) {
  const field = findField(condition.fieldKey);
  if (!field) {
    return '请选择一项判定规则';
  }
  switch (condition.operator) {
    case 'isEmpty':
      return '为空时，判定为非法数据';
    case 'notIn':
      return '不在允许范围内时，判定为非法数据';
    case 'contains':
      return '包含这些词时，判定为非法数据';
    case 'lt':
      return '低于这个值时，判定为非法数据';
    case 'gt':
      return '高于这个值时，判定为非法数据';
    default:
      return '满足这项规则时，判定为非法数据';
  }
}
</script>

<template>
  <section class="rule-editor-shell">
    <header class="rule-editor-head">
      <div class="rule-editor-head-main">
        <h3 class="rule-editor-title">怎么判定</h3>
        <p class="rule-editor-subtitle">这里只保留有业务意义的判定规则。改完后，右侧会告诉你哪些记录会被重新判定为非法数据。</p>
      </div>
      <el-button type="primary" plain :icon="Plus" @click="addGroup">新增一组规则</el-button>
    </header>

    <div class="rule-editor-groups">
      <article v-for="(group, groupIndex) in modelValue.groups" :key="group.id" class="rule-editor-group">
        <header class="rule-editor-group-head">
          <div class="rule-editor-group-title">规则组 {{ groupIndex + 1 }}</div>
          <div class="rule-editor-group-tools">
            <span class="rule-editor-group-label">这组规则需要</span>
            <el-segmented
              :model-value="group.matchMode"
              :options="[
                { label: '同时满足', value: 'all' },
                { label: '满足任一项', value: 'any' },
              ]"
              @change="updateGroupMatchMode(group.id, $event as 'all' | 'any')"
            />
            <el-button text :icon="Delete" @click="removeGroup(group.id)">删除这组</el-button>
          </div>
        </header>

        <div class="rule-editor-condition-list">
          <div v-for="condition in group.conditions" :key="condition.id" class="rule-editor-condition-row">
            <SmartSelect
              :model-value="condition.fieldKey"
              :options="fieldOptions"
              compact
              placeholder="选择判定规则"
              @change="updateConditionField(group.id, condition.id, String($event ?? ''))"
            />

            <div class="rule-editor-action">{{ ruleActionText(condition) }}</div>

            <SmartSelect
              v-if="findField(condition.fieldKey)?.type === 'multi-select' && usesConditionValue(condition.operator)"
              :model-value="conditionValueModel(condition)"
              :options="selectOptions(condition.fieldKey)"
              compact
              multiple
              collapse-tags
              placeholder="选择允许范围"
              @change="updateConditionValue(group.id, condition.id, $event as string | string[])"
            />

            <el-input
              v-else-if="usesConditionValue(condition.operator)"
              :model-value="condition.value"
              :placeholder="valuePlaceholder(condition)"
              @input="updateConditionValue(group.id, condition.id, String($event ?? ''))"
            />

            <div v-else class="rule-editor-no-value">这项规则不需要填写额外值</div>

            <el-button text :icon="Delete" @click="removeCondition(group.id, condition.id)">删除</el-button>
          </div>
        </div>

        <footer class="rule-editor-group-foot">
          <div class="rule-editor-group-summary">这组规则当前表达的是：{{ describeCodeReviewRuleGroup(group, fields) }}</div>
          <el-button plain size="small" :icon="Plus" @click="addCondition(group.id)">新增规则项</el-button>
        </footer>
      </article>
    </div>
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

.rule-editor-groups {
  display: grid;
  gap: 14px;
}

.rule-editor-group {
  display: grid;
  gap: 12px;
  padding: 16px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 18px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(249, 250, 251, 0.96));
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.04);
}

.rule-editor-group-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.rule-editor-group-title {
  font-size: 14px;
  font-weight: 700;
  color: #111827;
}

.rule-editor-group-tools {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.rule-editor-group-label {
  font-size: 12px;
  color: rgba(31, 41, 55, 0.58);
}

.rule-editor-condition-list {
  display: grid;
  gap: 10px;
}

.rule-editor-condition-row {
  display: grid;
  grid-template-columns: minmax(140px, 180px) minmax(120px, 150px) minmax(200px, 1fr) auto;
  gap: 10px;
  align-items: center;
}

.rule-editor-action {
  display: flex;
  align-items: center;
  min-height: 40px;
  padding: 0 12px;
  border-radius: 10px;
  background: rgba(239, 246, 255, 0.88);
  border: 1px solid rgba(59, 130, 246, 0.14);
  font-size: 12px;
  color: #1d4ed8;
}

.rule-editor-no-value {
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

.rule-editor-group-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  padding-top: 4px;
}

.rule-editor-group-summary {
  font-size: 12px;
  line-height: 1.7;
  color: rgba(31, 41, 55, 0.72);
}

:deep(.el-segmented) {
  --el-segmented-padding: 3px;
}

@media (max-width: 1200px) {
  .rule-editor-condition-row {
    grid-template-columns: 1fr;
  }
}
</style>
