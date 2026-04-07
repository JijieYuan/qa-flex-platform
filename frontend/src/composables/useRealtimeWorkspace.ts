import { ElMessage } from 'element-plus';
import { onBeforeUnmount, ref, toValue, type MaybeRefOrGetter, type Ref } from 'vue';
import type { RealtimeWorkspaceStatusResponse } from '../api';

type RealtimeMessageConfig = {
  refreshStarted?: string;
  refreshCompleted?: string;
  refreshFailed?: string;
  refreshRequestFailed?: string;
  statusPollFailed?: string;
};

type UseRealtimeWorkspaceOptions = {
  enabled?: MaybeRefOrGetter<boolean>;
  fetchStatus: () => Promise<RealtimeWorkspaceStatusResponse>;
  requestRefresh: () => Promise<RealtimeWorkspaceStatusResponse>;
  reloadData: () => Promise<void>;
  messages?: RealtimeMessageConfig;
  pollIntervalMs?: number;
  initialDelayMs?: number;
};

type UseRealtimeWorkspaceResult = {
  status: Ref<RealtimeWorkspaceStatusResponse | null>;
  refreshing: Ref<boolean>;
  loadStatus: () => Promise<void>;
  triggerRefresh: (silent?: boolean) => Promise<void>;
  ensureInitialSilentRefresh: () => void;
  stopPolling: () => void;
};

const DEFAULT_MESSAGES: Required<RealtimeMessageConfig> = {
  refreshStarted: '已开始刷新最新数据',
  refreshCompleted: '已刷新为最新数据',
  refreshFailed: '刷新失败，已保留当前结果',
  refreshRequestFailed: '刷新请求失败',
  statusPollFailed: '实时状态刷新失败',
};

export function useRealtimeWorkspace(options: UseRealtimeWorkspaceOptions): UseRealtimeWorkspaceResult {
  const status = ref<RealtimeWorkspaceStatusResponse | null>(null);
  const refreshing = ref(false);

  const messages = {
    ...DEFAULT_MESSAGES,
    ...options.messages,
  };

  let pollTimer: number | null = null;
  let initialSilentRefreshTriggered = false;

  function isEnabled() {
    return toValue(options.enabled ?? true);
  }

  async function loadStatus() {
    if (!isEnabled()) {
      status.value = null;
      return;
    }
    status.value = await options.fetchStatus();
  }

  function stopPolling() {
    if (pollTimer != null) {
      window.clearTimeout(pollTimer);
      pollTimer = null;
    }
  }

  async function finishRefresh(silent: boolean) {
    refreshing.value = false;
    await options.reloadData();
    if (!silent) {
      ElMessage.success(status.value?.status === 'FAILED' ? messages.refreshFailed : messages.refreshCompleted);
    }
  }

  function startPolling(silent: boolean) {
    stopPolling();
    const poll = async () => {
      try {
        await loadStatus();
        if (status.value?.refreshing) {
          pollTimer = window.setTimeout(poll, options.pollIntervalMs ?? 1500);
          return;
        }
        await finishRefresh(silent);
      } catch (error) {
        refreshing.value = false;
        if (!silent) {
          ElMessage.error(error instanceof Error ? error.message : messages.statusPollFailed);
        }
      }
    };
    pollTimer = window.setTimeout(poll, options.initialDelayMs ?? 1200);
  }

  async function triggerRefresh(silent = false) {
    if (!isEnabled()) {
      return;
    }
    try {
      refreshing.value = true;
      status.value = await options.requestRefresh();
      if (!silent) {
        ElMessage.success(messages.refreshStarted);
      }
      startPolling(silent);
    } catch (error) {
      refreshing.value = false;
      if (!silent) {
        ElMessage.error(error instanceof Error ? error.message : messages.refreshRequestFailed);
      }
    }
  }

  function ensureInitialSilentRefresh() {
    if (!isEnabled() || initialSilentRefreshTriggered) {
      return;
    }
    initialSilentRefreshTriggered = true;
    void triggerRefresh(true);
  }

  onBeforeUnmount(() => {
    stopPolling();
  });

  return {
    status,
    refreshing,
    loadStatus,
    triggerRefresh,
    ensureInitialSilentRefresh,
    stopPolling,
  };
}
