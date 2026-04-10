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
  { label: '缺失', value: 'missing' },
  { label: '匹配', value: 'match' },
  { label: '排除', value: 'exclude' },
  { label: '数值', value: 'threshold' },
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
  const field = resolveField(node.fieldKey);
  if (field?.type === 'number') {
    return;
  }
  const nextField = numberFields()[0];
  if (nextField) {
    node.fieldKey = nextField.key;
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

function addCondition(group: RuleGroupNode) {
  group.children.push(props.schema.createConditionNode(props.fields[0]));
}

function addGroup(group: RuleGroupNode) {
  group.children.push(
    props.schema.createGroupNode(RuleGroupOperator.OR, [props.schema.createConditionNode(props.fields[0])]),
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

function patternFields(node: RuleConditionNode) {
  const mode = inferMode(node);
  return mode === 'threshold' ? numberFields() : props.fields;
}

function toggleGroupOperator(group: RuleGroupNode) {
  group.operator = group.operator === RuleGroupOperator.AND ? RuleGroupOperator.OR : RuleGroupOperator.AND;
}
</script>

<template>
  <div v-if="node.type === RuleNodeType.GROUP" class="compact-group" :class="`compact-group-depth-${Math.min(depth, 2)}`">
    <div class="compact-group-body">
      <template v-for="(child, index) in node.children" :key="child.id">
        <RuleExpressionEditor
          :node="child"
          :fields="fields"
          :schema="schema"
          :depth="depth + 1"
          @remove="removeChild(node, $event)"
        />
        <button
          v-if="index < node.children.length - 1"
          type="button"
          class="compact-joiner"
          @click="toggleGroupOperator(node)"
        >
          {{ node.operator === RuleGroupOperator.AND ? '且' : '或' }}
        </button>
      </template>
    </div>

    <div class="compact-group-actions">
      <el-button text size="small" :icon="Plus" @click="addCondition(node)">添加条件</el-button>
      <el-button text size="small" @click="addGroup(node)">新增条件组</el-button>
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

  <div v-else class="compact-condition-row">
    <el-select
      :model-value="inferMode(node)"
      size="small"
      class="compact-control compact-mode"
      placeholder="模式"
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
      class="compact-control compact-field"
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
      class="compact-control compact-operator"
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
        class="compact-control compact-value"
        placeholder="值"
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
        class="compact-control compact-value"
        :inputmode="resolveField(node.fieldKey)?.type === 'number' ? 'decimal' : 'text'"
        :placeholder="resolveField(node.fieldKey)?.type === 'number' ? '阈值' : '关键词'"
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
.compact-group {
  display: grid;
  gap: 8px;
  padding: 8px 10px;
  border-radius: 8px;
  background: #fff;
  border: 1px solid #f0f0f0;
}

.compact-group-depth-1,
.compact-group-depth-2 {
  margin-left: 6px;
  padding: 6px 8px;
  background: #fcfcfc;
}

.compact-group-body {
  display: grid;
  gap: 6px;
}

.compact-group-actions {
  display: flex;
  align-items: center;
  gap: 2px;
  flex-wrap: wrap;
}

.compact-joiner {
  width: fit-content;
  margin-left: 2px;
  padding: 0 8px;
  border: 1px solid #d9d9d9;
  border-radius: 999px;
  background: #fafafa;
  color: #595959;
  font-size: 12px;
  font-weight: 600;
  line-height: 22px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.compact-joiner:hover {
  border-color: #bfbfbf;
  color: #262626;
  background: #fff;
}

.compact-condition-row {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.compact-control {
  min-width: 108px;
}

.compact-field,
.compact-value {
  min-width: 156px;
}

.compact-condition-row :deep(.el-input__inner),
.compact-condition-row :deep(.el-select__placeholder) {
  font-size: 14px;
}

.compact-group :deep(.el-button.is-text) {
  color: #595959;
  padding-inline: 6px;
}

.compact-group :deep(.el-button.is-text:hover) {
  color: #262626;
}

@media (max-width: 900px) {
  .compact-condition-row {
    align-items: stretch;
    flex-direction: column;
  }

  .compact-control,
  .compact-field,
  .compact-value {
    width: 100%;
    min-width: 0;
  }
}
</style>
