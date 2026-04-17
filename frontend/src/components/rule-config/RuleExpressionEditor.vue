<script setup lang="ts">
import { Delete, Plus } from '@element-plus/icons-vue';
import type { PropType } from 'vue';
import {
  AbstractRuleConfigSchemaSupport,
  RuleGroupOperator,
  RuleNodeType,
  RuleOperator,
  type RuleConditionNode,
  type RuleConfigField,
  type RuleExpressionNode,
  type RuleGroupNode,
} from '../../views/rule-config-core';

defineOptions({
  name: 'RuleExpressionEditor',
});

type IllegalPatternMode = 'missing' | 'match' | 'exclude' | 'threshold';
type ThresholdDirection = 'lt' | 'gt';

const props = defineProps({
  node: {
    type: Object as PropType<RuleExpressionNode>,
    required: true,
  },
  fields: {
    type: Array as PropType<RuleConfigField[]>,
    required: true,
  },
  schema: {
    type: Object as PropType<AbstractRuleConfigSchemaSupport<unknown, RuleConfigField>>,
    required: true,
  },
  canRemove: {
    type: Boolean,
    default: true,
  },
  depth: {
    type: Number,
    default: 0,
  },
});

const emit = defineEmits<{
  remove: [nodeId: string];
}>();

const modeOptions = [
  { label: '内容缺失', value: 'missing' },
  { label: '内容匹配', value: 'match' },
  { label: '内容排除', value: 'exclude' },
  { label: '数值判断', value: 'threshold' },
] as const;

const thresholdOptions = [
  { label: '小于', value: 'lt' },
  { label: '大于', value: 'gt' },
] as const;

function resolveField(fieldKey: string) {
  return props.fields.find((field) => field.key === fieldKey) ?? null;
}

function fieldOptions(field: RuleConfigField | null) {
  return field?.options ?? [];
}

function isSelectField(field: RuleConfigField | null) {
  return field?.type === 'select' && fieldOptions(field).length > 0;
}

function numberFields() {
  return props.fields.filter((field) => field.type === 'number');
}

function inferMode(node: RuleConditionNode): IllegalPatternMode {
  if (node.operator === RuleOperator.IS_EMPTY) {
    return 'missing';
  }
  if (
    node.operator === RuleOperator.GT ||
    node.operator === RuleOperator.GTE ||
    node.operator === RuleOperator.LT ||
    node.operator === RuleOperator.LTE
  ) {
    return 'threshold';
  }
  if (node.operator === RuleOperator.NE || node.operator === RuleOperator.NOT_CONTAINS) {
    return 'exclude';
  }
  return 'match';
}

function inferThresholdDirection(node: RuleConditionNode): ThresholdDirection {
  return node.operator === RuleOperator.GT || node.operator === RuleOperator.GTE ? 'gt' : 'lt';
}

function ensureThresholdField(node: RuleConditionNode) {
  const currentField = resolveField(node.fieldKey);
  if (currentField?.type === 'number') {
    return;
  }
  const fallbackField = numberFields()[0];
  if (fallbackField) {
    node.fieldKey = fallbackField.key;
  }
}

function applyMode(node: RuleConditionNode, mode: IllegalPatternMode) {
  const field = resolveField(node.fieldKey);
  if (mode === 'missing') {
    node.operator = RuleOperator.IS_EMPTY;
    node.value = '';
  } else if (mode === 'match') {
    node.operator = field?.type === 'number' ? RuleOperator.EQ : RuleOperator.CONTAINS;
  } else if (mode === 'exclude') {
    node.operator = field?.type === 'number' ? RuleOperator.NE : RuleOperator.NOT_CONTAINS;
  } else {
    ensureThresholdField(node);
    node.operator = RuleOperator.LT;
  }
  props.schema.syncExpressionNode(node, props.fields);
}

function handleModeChange(node: RuleConditionNode, mode: IllegalPatternMode) {
  applyMode(node, mode);
}

function handleFieldChange(node: RuleConditionNode) {
  const mode = inferMode(node);
  if (mode === 'threshold') {
    ensureThresholdField(node);
  }
  applyMode(node, mode);
}

function handleThresholdDirectionChange(node: RuleConditionNode, direction: ThresholdDirection) {
  node.operator = direction === 'gt' ? RuleOperator.GT : RuleOperator.LT;
  props.schema.syncExpressionNode(node, props.fields);
}

function patternFields(node: RuleConditionNode) {
  return inferMode(node) === 'threshold' ? numberFields() : props.fields;
}

function addCondition(group: RuleGroupNode) {
  group.children.push(props.schema.createConditionNode(props.fields[0]));
}

function addGroup(group: RuleGroupNode) {
  group.children.push(
    props.schema.createGroupNode(RuleGroupOperator.AND, [props.schema.createConditionNode(props.fields[0])]),
  );
}

function removeChild(group: RuleGroupNode, nodeId: string) {
  const index = group.children.findIndex((child) => child.id === nodeId);
  if (index >= 0) {
    group.children.splice(index, 1);
  }
}

function requestRemove() {
  emit('remove', props.node.id);
}
</script>

<template>
  <div
    v-if="node.type === RuleNodeType.GROUP"
    class="rule-editor-group"
    :class="{
      'rule-editor-group-root': depth === 0,
      'rule-editor-group-nested': depth > 0,
    }"
  >
    <div class="rule-editor-group-header">
      <div class="rule-editor-group-header-main">
        <span class="rule-editor-group-label">{{ depth === 0 ? '条件组' : '嵌套分组' }}</span>
        <el-radio-group v-model="node.operator" size="small" class="rule-editor-group-operator">
          <el-radio-button :label="RuleGroupOperator.AND">且</el-radio-button>
          <el-radio-button :label="RuleGroupOperator.OR">或</el-radio-button>
        </el-radio-group>
      </div>
      <div class="rule-editor-group-actions">
        <el-button text size="small" :icon="Plus" @click="addCondition(node)">添加条件</el-button>
        <el-button text size="small" @click="addGroup(node)">添加分组</el-button>
        <el-button
          v-if="canRemove"
          text
          size="small"
          type="danger"
          :icon="Delete"
          @click="requestRemove"
        >
          删除
        </el-button>
      </div>
    </div>

    <div class="rule-editor-group-body">
      <template v-for="(child, index) in node.children" :key="child.id">
        <div v-if="index > 0" class="rule-editor-joiner">
          <span class="rule-editor-joiner-chip">
            {{ node.operator === RuleGroupOperator.AND ? '且' : '或' }}
          </span>
        </div>
        <RuleExpressionEditor
          :node="child"
          :fields="fields"
          :schema="schema"
          :depth="depth + 1"
          @remove="removeChild(node, $event)"
        />
      </template>
    </div>
  </div>

  <div v-else class="rule-editor-condition-row">
    <el-select
      :model-value="inferMode(node)"
      size="small"
      class="rule-editor-control rule-editor-mode"
      placeholder="类型"
      @change="handleModeChange(node, $event as IllegalPatternMode)"
    >
      <el-option
        v-for="option in modeOptions"
        :key="option.value"
        :label="option.label"
        :value="option.value"
      />
    </el-select>

    <el-select
      v-model="node.fieldKey"
      size="small"
      class="rule-editor-control rule-editor-field"
      placeholder="字段"
      @change="handleFieldChange(node)"
    >
      <el-option
        v-for="field in patternFields(node)"
        :key="field.key"
        :label="field.label"
        :value="field.key"
      />
    </el-select>

    <el-select
      v-if="inferMode(node) === 'threshold'"
      :model-value="inferThresholdDirection(node)"
      size="small"
      class="rule-editor-control rule-editor-relation"
      placeholder="关系"
      @change="handleThresholdDirectionChange(node, $event as ThresholdDirection)"
    >
      <el-option
        v-for="option in thresholdOptions"
        :key="option.value"
        :label="option.label"
        :value="option.value"
      />
    </el-select>

    <template v-if="props.schema.usesValueInput(node.operator)">
      <el-select
        v-if="isSelectField(resolveField(node.fieldKey))"
        v-model="node.value"
        size="small"
        class="rule-editor-control rule-editor-value"
        placeholder="取值"
        clearable
      >
        <el-option
          v-for="option in fieldOptions(resolveField(node.fieldKey))"
          :key="option.value"
          :label="option.label"
          :value="option.value"
        />
      </el-select>

      <el-input
        v-else
        v-model="node.value"
        size="small"
        class="rule-editor-control rule-editor-value"
        :inputmode="resolveField(node.fieldKey)?.type === 'number' ? 'decimal' : 'text'"
        :placeholder="resolveField(node.fieldKey)?.type === 'number' ? '阈值' : '取值'"
        clearable
      />
    </template>

    <el-button
      v-if="canRemove"
      text
      size="small"
      circle
      :icon="Delete"
      @click="requestRemove"
    />
  </div>
</template>

<style scoped>
.rule-editor-group {
  display: grid;
  gap: 10px;
}

.rule-editor-group-root {
  padding: 12px;
  border-radius: 10px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: #fff;
}

.rule-editor-group-nested {
  padding: 10px;
  border-radius: 8px;
  border: 1px dashed rgba(15, 23, 42, 0.12);
  background: rgba(248, 250, 252, 0.7);
}

.rule-editor-group-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.rule-editor-group-header-main {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.rule-editor-group-label {
  font-size: 12px;
  font-weight: 600;
  color: rgba(15, 23, 42, 0.72);
}

.rule-editor-group-operator :deep(.el-radio-button__inner) {
  min-width: 34px;
  padding: 0 10px;
  border-color: rgba(15, 23, 42, 0.12);
  background: #fff;
  color: rgba(15, 23, 42, 0.68);
  box-shadow: none;
  font-size: 12px;
  font-weight: 600;
}

.rule-editor-group-operator :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background: rgba(219, 234, 254, 0.96);
  border-color: rgba(59, 130, 246, 0.28);
  color: rgba(30, 64, 175, 0.96);
  box-shadow: none;
}

.rule-editor-group-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;
}

.rule-editor-group-actions :deep(.el-button.is-text) {
  padding-inline: 6px;
  color: rgba(15, 23, 42, 0.6);
}

.rule-editor-group-actions :deep(.el-button.is-text:hover) {
  color: rgba(37, 99, 235, 0.9);
}

.rule-editor-group-body {
  display: grid;
  gap: 8px;
}

.rule-editor-joiner {
  display: flex;
  align-items: center;
  gap: 8px;
}

.rule-editor-joiner::before {
  content: '';
  flex: 1;
  height: 1px;
  background: rgba(15, 23, 42, 0.08);
}

.rule-editor-joiner::after {
  content: '';
  flex: 1;
  height: 1px;
  background: rgba(15, 23, 42, 0.08);
}

.rule-editor-joiner-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 28px;
  height: 22px;
  padding: 0 8px;
  border-radius: 999px;
  border: 1px solid rgba(15, 23, 42, 0.1);
  background: #fff;
  color: rgba(15, 23, 42, 0.52);
  font-size: 12px;
  font-weight: 600;
}

.rule-editor-condition-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.rule-editor-control {
  min-width: 120px;
}

.rule-editor-field,
.rule-editor-value {
  min-width: 160px;
}

.rule-editor-condition-row :deep(.el-input__wrapper),
.rule-editor-condition-row :deep(.el-select__wrapper) {
  min-height: 34px;
  border-radius: 8px;
  box-shadow: 0 0 0 1px rgba(15, 23, 42, 0.08) inset;
  background: rgba(255, 255, 255, 0.96);
}

.rule-editor-condition-row :deep(.el-input__inner),
.rule-editor-condition-row :deep(.el-select__selected-item),
.rule-editor-condition-row :deep(.el-select__placeholder) {
  font-size: 14px;
}

@media (max-width: 900px) {
  .rule-editor-group-header {
    align-items: stretch;
    flex-direction: column;
  }

  .rule-editor-condition-row {
    flex-direction: column;
    align-items: stretch;
  }

  .rule-editor-control,
  .rule-editor-field,
  .rule-editor-value {
    width: 100%;
    min-width: 0;
  }
}
</style>
