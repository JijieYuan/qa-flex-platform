<script setup lang="ts">
import type { StatisticFilterField, StatisticFilterOperator } from '../api';
import {
  createFilterConditionDraft,
  operatorLabel,
  usesSecondaryValue,
  type StatisticFilterConditionDraft,
  type StatisticFilterDraftGroup,
} from './statistic-board-filters';

const props = withDefaults(
  defineProps<{
    modelValue: StatisticFilterDraftGroup;
    fields: StatisticFilterField[];
    addButtonText?: string;
  }>(),
  {
    addButtonText: '添加条件',
  },
);

function addFilterCondition() {
  const field = props.fields[0];
  props.modelValue.conditions.push(createFilterConditionDraft(field));
}

function removeFilterCondition(conditionId: string) {
  const index = props.modelValue.conditions.findIndex((condition) => condition.id === conditionId);
  if (index >= 0) {
    props.modelValue.conditions.splice(index, 1);
  }
}

function fieldForCondition(fieldKey: string) {
  return props.fields.find((field) => field.key === fieldKey) ?? null;
}

function handleConditionFieldChange(condition: StatisticFilterConditionDraft) {
  const field = fieldForCondition(condition.fieldKey);
  condition.operator = (field?.operators?.[0] ?? '') as StatisticFilterOperator | '';
  condition.value = '';
  condition.secondaryValue = '';
}

function operatorOptionsForCondition(condition: StatisticFilterConditionDraft) {
  return fieldForCondition(condition.fieldKey)?.operators ?? [];
}

function usesDatePicker(condition: StatisticFilterConditionDraft) {
  return fieldForCondition(condition.fieldKey)?.type === 'datetime';
}

function datePickerType(condition: StatisticFilterConditionDraft) {
  return (
    {
      year: 'year',
      month: 'month',
      day: 'date',
      at: 'datetime',
      before: 'datetime',
      after: 'datetime',
      between: 'datetime',
    } as Record<string, string>
  )[condition.operator] ?? 'datetime';
}

function dateValueFormat(condition: StatisticFilterConditionDraft) {
  return (
    {
      year: 'YYYY',
      month: 'YYYY-MM',
      day: 'YYYY-MM-DD',
      at: 'YYYY-MM-DD HH:mm:ss',
      before: 'YYYY-MM-DD HH:mm:ss',
      after: 'YYYY-MM-DD HH:mm:ss',
      between: 'YYYY-MM-DD HH:mm:ss',
    } as Record<string, string>
  )[condition.operator] ?? 'YYYY-MM-DD HH:mm:ss';
}

function isNumericField(condition: StatisticFilterConditionDraft) {
  return fieldForCondition(condition.fieldKey)?.type === 'number';
}

function isSelectField(condition: StatisticFilterConditionDraft) {
  return fieldForCondition(condition.fieldKey)?.type === 'select';
}

function fieldOptions(condition: StatisticFilterConditionDraft) {
  return fieldForCondition(condition.fieldKey)?.options ?? [];
}

function needsValue(condition: StatisticFilterConditionDraft) {
  return condition.operator !== 'isEmpty' && condition.operator !== 'isNotEmpty';
}
</script>

<template>
  <div class="stat-filter-builder">
    <el-segmented
      :model-value="modelValue.logic"
      :options="[{ label: '满足全部', value: 'AND' }, { label: '满足任意', value: 'OR' }]"
      class="stat-filter-logic"
      @update:model-value="modelValue.logic = $event === 'OR' ? 'OR' : 'AND'"
    />
    <div v-if="modelValue.conditions.length" class="stat-filter-list">
      <div v-for="condition in modelValue.conditions" :key="condition.id" class="stat-filter-row">
        <el-select
          v-model="condition.fieldKey"
          class="stat-filter-field"
          placeholder="字段"
          @change="handleConditionFieldChange(condition)"
        >
          <el-option v-for="field in fields" :key="field.key" :label="field.label" :value="field.key" />
        </el-select>
        <el-select v-model="condition.operator" class="stat-filter-operator" placeholder="关系">
          <el-option
            v-for="operator in operatorOptionsForCondition(condition)"
            :key="operator"
            :label="operatorLabel(operator)"
            :value="operator"
          />
        </el-select>
        <template v-if="needsValue(condition)">
          <el-select
            v-if="isSelectField(condition)"
            v-model="condition.value"
            class="stat-filter-value"
            placeholder="值"
            clearable
            filterable
          >
            <el-option
              v-for="option in fieldOptions(condition)"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
          <el-input-number
            v-else-if="isNumericField(condition)"
            v-model="condition.value"
            class="stat-filter-value"
            controls-position="right"
            placeholder="值"
          />
          <el-date-picker
            v-else-if="usesDatePicker(condition)"
            v-model="condition.value"
            class="stat-filter-value"
            :type="datePickerType(condition)"
            :value-format="dateValueFormat(condition)"
            placeholder="时间"
          />
          <el-input v-else v-model="condition.value" class="stat-filter-value" placeholder="值" clearable />
        </template>
        <el-input-number
          v-if="usesSecondaryValue(condition.operator) && isNumericField(condition)"
          v-model="condition.secondaryValue"
          class="stat-filter-value secondary"
          controls-position="right"
          placeholder="结束值"
        />
        <el-date-picker
          v-else-if="usesSecondaryValue(condition.operator) && usesDatePicker(condition)"
          v-model="condition.secondaryValue"
          class="stat-filter-value secondary"
          :type="datePickerType(condition)"
          :value-format="dateValueFormat(condition)"
          placeholder="结束时间"
        />
        <el-input
          v-else-if="usesSecondaryValue(condition.operator)"
          v-model="condition.secondaryValue"
          class="stat-filter-value secondary"
          placeholder="结束值"
          clearable
        />
        <el-button text type="danger" @click="removeFilterCondition(condition.id)">删除</el-button>
      </div>
    </div>
    <el-button plain @click="addFilterCondition">{{ addButtonText }}</el-button>
  </div>
</template>
