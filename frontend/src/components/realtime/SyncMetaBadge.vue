<script setup lang="ts">
import { computed } from 'vue';

const props = withDefaults(
  defineProps<{
    value?: string | null;
    label?: string;
  }>(),
  {
    value: '',
    label: '最近同步',
  },
);

const displayValue = computed(() => (props.value && props.value.trim() ? props.value : '暂无同步记录'));
</script>

<template>
  <div class="sync-meta-badge" :title="`${label}：${displayValue}`">
    <span class="sync-meta-badge-label">{{ label }}</span>
    <span class="sync-meta-badge-value">{{ displayValue }}</span>
  </div>
</template>

<style scoped>
.sync-meta-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 32px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(248, 250, 252, 0.88);
  white-space: nowrap;
}

.sync-meta-badge-label {
  font-size: 12px;
  color: rgba(15, 23, 42, 0.45);
}

.sync-meta-badge-value {
  font-size: 12px;
  font-weight: 600;
  color: rgba(15, 23, 42, 0.82);
}
</style>
