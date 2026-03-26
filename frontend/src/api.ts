export type WhitelistMode = 'RECOMMENDED' | 'ALL' | 'CUSTOM';
export type SourceMode = 'DIRECT' | 'DOCKER';

export interface GitlabSyncConfig {
  id?: number;
  name: string;
  enabled: boolean;
  autoSyncEnabled: boolean;
  sourceMode: SourceMode;
  whitelistMode: WhitelistMode;
  whitelistTables: string[];
  dbHost: string;
  dbPort: number;
  dbName: string;
  dbUsername: string;
  dbPassword: string;
  dockerContainerName?: string;
  webhookSecret?: string;
  webhookProjectId?: number | null;
  compensationIntervalMinutes: number;
  lastFullSyncAt?: string | null;
  lastIncrementalSyncAt?: string | null;
}

export interface TableWhitelistOption {
  tableName: string;
  label: string;
  primaryKey: string;
  updatedAtColumn?: string | null;
  recommended: boolean;
}

export interface GitlabSyncLog {
  id: number;
  syncType: string;
  status: string;
  message: string;
  tableCount: number;
  recordCount: number;
  startedAt: string;
  finishedAt?: string | null;
}

export interface SyncProgress {
  phase: string;
  totalTables: number;
  completedTables: number;
  syncedRecords: number;
  currentTable?: string | null;
  startedAt?: string | null;
}

export interface MirrorStatusResponse {
  config: GitlabSyncConfig;
  currentStatus: string;
  currentMessage: string;
  currentStartedAt?: string | null;
  progress?: SyncProgress | null;
  logs: GitlabSyncLog[];
  whitelistOptions: TableWhitelistOption[];
  webhookUrl: string;
}

async function request<T>(url: string, init?: RequestInit): Promise<T> {
  const response = await fetch(url, {
    headers: { 'Content-Type': 'application/json' },
    ...init,
  });
  const rawText = await response.text();
  let payload: any = null;
  try {
    payload = rawText ? JSON.parse(rawText) : null;
  } catch {
    payload = null;
  }

  if (!response.ok) {
    throw new Error(payload?.message || rawText || `Request failed: ${response.status}`);
  }

  if (payload && typeof payload === 'object' && 'success' in payload) {
    if (!payload.success) {
      throw new Error(payload.message || 'Request failed');
    }
    return payload.data as T;
  }

  return payload as T;
}

export const api = {
  getStatus() {
    return request<MirrorStatusResponse>('/api/gitlab-sync/status');
  },
  saveConfig(config: GitlabSyncConfig) {
    return request<GitlabSyncConfig>('/api/gitlab-sync/config', {
      method: 'PUT',
      body: JSON.stringify(config),
    });
  },
  testConnection() {
    return request<{ success: boolean; message: string }>('/api/gitlab-sync/test-connection', {
      method: 'POST',
    });
  },
  startFullSync() {
    return request<{ accepted: boolean; message: string }>('/api/gitlab-sync/full-sync', {
      method: 'POST',
    });
  },
  startIncrementalSync() {
    return request<{ accepted: boolean; message: string }>('/api/gitlab-sync/incremental-sync', {
      method: 'POST',
    });
  },
};
