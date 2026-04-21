<script setup lang="ts">
import { ref, watch } from 'vue';
import { Search } from '@element-plus/icons-vue';

const props = withDefaults(
  defineProps<{
    modelValue?: string;
    placeholder?: string;
    clearable?: boolean;
    disabled?: boolean;
  }>(),
  {
    modelValue: '',
    placeholder: '',
    clearable: true,
    disabled: false,
  },
);

const emit = defineEmits<{
  (event: 'update:modelValue', value: string): void;
  (event: 'change', value: string): void;
  (event: 'search', value: string): void;
  (event: 'clear'): void;
}>();

const draft = ref(props.modelValue);

watch(
  () => props.modelValue,
  (value) => {
    draft.value = value ?? '';
  },
);

function emitChange(value: string) {
  emit('update:modelValue', value);
  emit('change', value);
}

function handleModelUpdate(value: string) {
  draft.value = value;
  emit('update:modelValue', value);
}

function handleChange(value: string) {
  emitChange(String(value ?? ''));
}

function handleSearch() {
  emit('search', draft.value.trim());
}

function handleClear() {
  draft.value = '';
  emitChange('');
  emit('clear');
}
</script>

<template>
  <el-input
    :model-value="draft"
    :placeholder="placeholder"
    :clearable="clearable"
    :disabled="disabled"
    @update:model-value="handleModelUpdate"
    @change="handleChange"
    @keyup.enter="handleSearch"
    @clear="handleClear"
  >
    <template #prefix>
      <el-icon><Search /></el-icon>
    </template>
  </el-input>
</template>
