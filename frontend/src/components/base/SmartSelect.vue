<script setup lang="ts">
import { computed, ref } from 'vue';
import { pinyin } from 'pinyin-pro';
import type { RecordTableFilterOption } from '../../types/record-table';

const props = withDefaults(
  defineProps<{
    modelValue: string | string[];
    options: RecordTableFilterOption[];
    placeholder?: string;
    clearable?: boolean;
    multiple?: boolean;
    collapseTags?: boolean;
    compact?: boolean;
    disabled?: boolean;
  }>(),
  {
    placeholder: '',
    clearable: true,
    multiple: false,
    collapseTags: false,
    compact: false,
    disabled: false,
  },
);

const emit = defineEmits<{
  (event: 'update:modelValue', value: string | string[]): void;
  (event: 'change', value: string | string[]): void;
}>();

const query = ref('');

const filteredOptions = computed(() => {
  const normalizedQuery = query.value.trim().toLowerCase();
  if (!normalizedQuery) {
    return props.options;
  }
  return props.options.filter((option) => matchesOption(option, normalizedQuery));
});

const popperClass = computed(() =>
  props.compact ? 'smart-select-dropdown smart-select-dropdown--compact' : 'smart-select-dropdown',
);

function handleFilter(keyword: string) {
  query.value = keyword;
}

function handleVisibleChange(visible: boolean) {
  if (!visible) {
    query.value = '';
  }
}

function handleChange(value: string | string[]) {
  emit('update:modelValue', value);
  emit('change', value);
}

function matchesOption(option: RecordTableFilterOption, normalizedQuery: string) {
  const label = option.label.toLowerCase();
  const value = option.value.toLowerCase();
  const spell = pinyin(option.label, { toneType: 'none', type: 'string' }).replace(/\s+/g, '').toLowerCase();
  const initials = pinyin(option.label, { pattern: 'first', toneType: 'none', type: 'string' })
    .replace(/\s+/g, '')
    .toLowerCase();
  return (
    label.includes(normalizedQuery) ||
    value.includes(normalizedQuery) ||
    spell.includes(normalizedQuery) ||
    initials.includes(normalizedQuery)
  );
}
</script>

<template>
  <el-select
    :model-value="modelValue"
    filterable
    :filter-method="handleFilter"
    :placeholder="placeholder"
    :clearable="clearable"
    :multiple="multiple"
    :collapse-tags="collapseTags"
    collapse-tags-tooltip
    :disabled="disabled"
    :popper-class="popperClass"
    @change="handleChange"
    @visible-change="handleVisibleChange"
  >
    <el-option
      v-for="option in filteredOptions"
      :key="option.value"
      :label="option.label"
      :value="option.value"
    >
      <div class="smart-select-option">
        <span class="smart-select-option-label">{{ option.label }}</span>
      </div>
    </el-option>
  </el-select>
</template>

<style>
.smart-select-dropdown .smart-select-option {
  display: flex;
  align-items: center;
  width: 100%;
}

.smart-select-dropdown--compact .el-select-dropdown__list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 8px;
  padding: 8px;
}

.smart-select-dropdown--compact .el-select-dropdown__item {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 36px;
  height: auto;
  line-height: 1.4;
  margin: 0;
  padding: 8px 10px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 10px;
  background: #fff;
  color: rgba(15, 23, 42, 0.78);
  transition:
    border-color 0.18s ease,
    background-color 0.18s ease,
    color 0.18s ease,
    box-shadow 0.18s ease;
}

.smart-select-dropdown--compact .el-select-dropdown__item.hover,
.smart-select-dropdown--compact .el-select-dropdown__item:hover {
  border-color: rgba(64, 158, 255, 0.36);
  background: rgba(64, 158, 255, 0.08);
  color: #1d4ed8;
}

.smart-select-dropdown--compact .el-select-dropdown__item.selected {
  border-color: rgba(64, 158, 255, 0.48);
  background: rgba(64, 158, 255, 0.12);
  color: #1d4ed8;
  font-weight: 600;
}

.smart-select-dropdown--compact .smart-select-option {
  justify-content: center;
  text-align: center;
}

.smart-select-dropdown--compact .smart-select-option-label {
  font-size: 13px;
  line-height: 1.35;
  white-space: normal;
}
</style>
