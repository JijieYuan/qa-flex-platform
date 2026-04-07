<script setup lang="ts">
import { computed } from 'vue';
import { RefreshRight } from '@element-plus/icons-vue';
import type { RealtimeWorkspaceStatusResponse } from '../../types/realtime';

const props = withDefaults(
  defineProps<{
    title: string;
    description?: string;
    status?: RealtimeWorkspaceStatusResponse | null;
    refreshing?: boolean;
    compact?: boolean;
    showRefresh?: boolean;
  }>(),
  {
    description: '',
    status: null,
    refreshing: false,
    compact: false,
    showRefresh: true,
  },
);

const emit = defineEmits<{
  (event: 'refresh'): void;
}>();

const statusType = computed(() => {
  if (!props.status?.supported) {
    return 'info';
  }
  if (props.status.refreshing || props.status.status === 'REFRESHING') {
    return 'warning';
  }
  if (props.status.status === 'FAILED') {
    return 'danger';
  }
  return 'success';
});

const statusLabel = computed(() => {
  if (!props.status?.supported) {
    return '未启用实时刷新';
  }
  if (props.status.refreshing || props.status.status === 'REFRESHING') {
    return '刷新中';
  }
  if (props.status.status === 'FAILED') {
    return '刷新失败';
  }
  if (props.status.status === 'READY') {
    return '已同步';
  }
  return '待同步';
});

const lastSyncedText = computed(() => {
  if (!props.status?.lastSyncedAt) {
    return '暂无同步记录';
  }
  return props.status.lastSyncedAt.replace('T', ' ').slice(0, 19);
});

const showHeader = computed(
  () => Boolean(props.title || props.description || props.showRefresh || props.status),
);

const showInlineAlert = computed(() => {
  if (!props.status?.message) {
    return false;
  }
  return props.status.status === 'FAILED';
});
</script>

<template>
  <section class="realtime-shell" :class="{ 'realtime-shell-compact': compact }">
    <header v-if="showHeader" class="realtime-shell-header">
      <div class="realtime-shell-meta">
        <h3 v-if="title" class="realtime-shell-title">{{ title }}</h3>
        <p v-if="description" class="realtime-shell-description">{{ description }}</p>
      </div>

      <div class="realtime-shell-actions">
        <div v-if="status" class="realtime-shell-status">
          <span class="realtime-shell-status-label">最近同步</span>
          <strong class="realtime-shell-status-value">{{ lastSyncedText }}</strong>
          <el-tag size="small" effect="plain" :type="statusType">{{ statusLabel }}</el-tag>
        </div>
        <slot name="actions" />
        <el-button
          v-if="showRefresh"
          type="primary"
          plain
          :icon="RefreshRight"
          :loading="refreshing || status?.refreshing"
          @click="emit('refresh')"
        >
          刷新最新数据
        </el-button>
      </div>
    </header>

    <el-alert
      v-if="showInlineAlert"
      :title="status.message"
      :type="statusType"
      :closable="false"
      show-icon
      class="realtime-shell-alert"
    />

    <div class="realtime-shell-body">
      <slot />
    </div>
  </section>
</template>

<style scoped>
.realtime-shell {
  display: grid;
  gap: 12px;
}

.realtime-shell-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 2px 0 4px;
}

.realtime-shell-meta {
  display: grid;
  gap: 4px;
}

.realtime-shell-title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: rgba(15, 23, 42, 0.92);
}

.realtime-shell-description {
  margin: 0;
  font-size: 12px;
  line-height: 1.5;
  color: rgba(15, 23, 42, 0.52);
}

.realtime-shell-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  flex-wrap: wrap;
}

.realtime-shell-status {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(248, 250, 252, 0.88);
  border: 1px solid rgba(15, 23, 42, 0.08);
}

.realtime-shell-status-label {
  font-size: 12px;
  color: rgba(15, 23, 42, 0.45);
}

.realtime-shell-status-value {
  font-size: 12px;
  font-weight: 600;
  color: rgba(15, 23, 42, 0.82);
}

.realtime-shell-alert {
  margin: 0;
}

.realtime-shell-body {
  min-width: 0;
}

.realtime-shell-compact {
  gap: 8px;
}

.realtime-shell-compact .realtime-shell-header {
  align-items: center;
  gap: 12px;
  padding-top: 0;
}

.realtime-shell-compact .realtime-shell-meta {
  gap: 2px;
}

.realtime-shell-compact .realtime-shell-title {
  font-size: 14px;
}

.realtime-shell-compact .realtime-shell-description {
  font-size: 11px;
  color: rgba(15, 23, 42, 0.42);
}

.realtime-shell-compact .realtime-shell-actions {
  gap: 10px;
}

.realtime-shell-compact .realtime-shell-status {
  padding: 4px 10px;
  gap: 6px;
  background: transparent;
  border-color: rgba(15, 23, 42, 0.06);
}

.realtime-shell-compact .realtime-shell-status-label,
.realtime-shell-compact .realtime-shell-status-value {
  font-size: 11px;
}

@media (max-width: 1200px) {
  .realtime-shell-header {
    flex-direction: column;
    align-items: stretch;
  }

  .realtime-shell-actions {
    justify-content: flex-start;
  }
}
</style>
