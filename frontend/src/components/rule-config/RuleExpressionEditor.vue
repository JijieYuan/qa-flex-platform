<script setup lang="ts">
import { computed } from 'vue';
import { Delete, Plus } from '@element-plus/icons-vue';
import type { PropType } from 'vue';
import {
  AbstractRuleConfigSchemaSupport,
  RuleGroupOperator,
  RuleNodeType,
  type RuleConditionNode,
  type RuleConfigField,
  type RuleExpressionNode,
  type RuleGroupNode,
} from '../../views/rule-config-core';

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

const isGroup = computed(() => props.node.type === RuleNodeType.GROUP);

function resolveField(fieldKey: string) {
  return props.fields.find((field) => field.key === fieldKey) ?? null;
}

function fieldOptions(field: RuleConfigField | null) {
  return field?.options ?? [];
}

function isSelectField(field: RuleConfigField | null) {
  return field?.type === 'select' && fieldOptions(field).length > 0;
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

function handleFieldChange(condition: RuleConditionNode) {
  const field = resolveField(condition.fieldKey);
  condition.operator = field?.operators[0] ?? condition.operator;
  condition.value = '';
  props.schema.syncExpressionNode(condition, props.fields);
}

function handleOperatorChange(condition: RuleConditionNode) {
  props.schema.syncExpressionNode(condition, props.fields);
}

function requestRemove() {
  emit('remove', props.node.id);
}
</script>

<template>
  <div
    :class="[
      'rule-expression-node',
      isGroup ? 'rule-expression-group' : 'rule-expression-condition',
      `rule-expression-depth-${Math.min(depth, 3)}`,
    ]"
  >
    <template v-if="node.type === RuleNodeType.GROUP">
      <div class="rule-expression-group-header">
        <div class="rule-expression-group-meta">
          <span class="rule-expression-group-title">逻辑组</span>
          <el-segmented
            v-model="node.operator"
            class="rule-expression-group-switch"
            :options="[
              { label: '并且 AND', value: RuleGroupOperator.AND },
              { label: '或者 OR', value: RuleGroupOperator.OR },
            ]"
          />
        </div>
        <div class="rule-expression-group-actions">
          <el-button size="small" :icon="Plus" @click="addCondition(node)">条件</el-button>
          <el-button size="small" text @click="addGroup(node)">子组</el-button>
          <el-button v-if="canRemove" size="small" text type="danger" :icon="Delete" @click="requestRemove">删除</el-button>
        </div>
      </div>

      <div class="rule-expression-group-children">
        <RuleExpressionEditor
          v-for="child in node.children"
          :key="child.id"
          :node="child"
          :fields="fields"
          :schema="schema"
          :depth="depth + 1"
          @remove="removeChild(node, $event)"
        />
      </div>
    </template>

    <template v-else>
      <div class="rule-expression-condition-row">
        <el-select
          v-model="node.fieldKey"
          class="rule-expression-inline-select"
          placeholder="选择字段"
          @change="handleFieldChange(node)"
        >
          <el-option
            v-for="field in fields"
            :key="field.key"
            :label="field.label"
            :value="field.key"
          />
        </el-select>

        <el-select
          v-model="node.operator"
          class="rule-expression-inline-select"
          placeholder="选择关系"
          @change="handleOperatorChange(node)"
        >
          <el-option
            v-for="operator in resolveField(node.fieldKey)?.operators ?? []"
            :key="operator"
            :label="schema.operatorLabel(operator)"
            :value="operator"
          />
        </el-select>

        <template v-if="schema.usesValueInput(node.operator)">
          <el-select
            v-if="isSelectField(resolveField(node.fieldKey))"
            v-model="node.value"
            class="rule-expression-inline-value"
            placeholder="选择取值"
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
            class="rule-expression-inline-value"
            :inputmode="resolveField(node.fieldKey)?.type === 'number' ? 'decimal' : 'text'"
            :placeholder="resolveField(node.fieldKey)?.type === 'number' ? '输入数值' : '输入取值'"
            clearable
          />
        </template>

        <span v-else class="rule-expression-empty-value">无需取值</span>

        <el-button v-if="canRemove" class="rule-expression-delete" circle :icon="Delete" @click="requestRemove" />
      </div>
    </template>
  </div>
</template>

<style scoped>
.rule-expression-node {
  display: grid;
  gap: 10px;
}

.rule-expression-group,
.rule-expression-condition {
  padding: 12px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid rgba(15, 23, 42, 0.06);
}

.rule-expression-depth-1 {
  background: rgba(248, 250, 252, 0.98);
}

.rule-expression-depth-2,
.rule-expression-depth-3 {
  background: rgba(241, 245, 249, 0.98);
}

.rule-expression-group-header,
.rule-expression-condition-row {
  display: flex;
  align-items: center;
  gap: 10px;
  justify-content: space-between;
  flex-wrap: wrap;
}

.rule-expression-group-meta,
.rule-expression-group-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.rule-expression-group-title {
  font-size: 13px;
  font-weight: 700;
  color: rgba(15, 23, 42, 0.76);
}

.rule-expression-group-children {
  display: grid;
  gap: 10px;
}

.rule-expression-inline-select {
  width: 180px;
}

.rule-expression-inline-value {
  width: 220px;
}

.rule-expression-empty-value {
  font-size: 12px;
  color: rgba(15, 23, 42, 0.5);
}

.rule-expression-delete {
  flex-shrink: 0;
}

@media (max-width: 960px) {
  .rule-expression-group-header,
  .rule-expression-condition-row {
    align-items: stretch;
  }

  .rule-expression-group-meta,
  .rule-expression-group-actions {
    width: 100%;
  }
}
</style>
