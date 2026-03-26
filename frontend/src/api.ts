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

export interface StatisticFilterOption {
  label: string;
  value: string;
}

export interface StatisticFilterField {
  key: string;
  label: string;
  type: 'text' | 'select';
  placeholder?: string | null;
  defaultValue?: string | null;
  width?: number | null;
  options: StatisticFilterOption[];
}

export interface StatisticColumnLeaf {
  key: string;
  label: string;
  drilldown: boolean;
  metricType: string;
}

export interface StatisticColumnGroup {
  key: string;
  label: string;
  columns: StatisticColumnLeaf[];
}

export interface StatisticDetailColumn {
  key: string;
  label: string;
  width?: number | null;
  minWidth?: number | null;
  sortable: boolean;
}

export interface StatisticBoardDefinition {
  boardKey: string;
  title: string;
  description: string;
  queryTitle: string;
  queryDescription: string;
  filters: StatisticFilterField[];
  columnGroups: StatisticColumnGroup[];
  detailColumns: StatisticDetailColumn[];
  defaultPageSize?: number | null;
  emptyText?: string | null;
}

export interface StatisticBoardMeta {
  generatedAt: string;
  queryDurationMs: number;
  rowCount: number;
  columnCount: number;
  drilldownColumnCount: number;
}

export interface StatisticCellData {
  columnKey: string;
  numericValue: number;
  displayValue: string;
  drilldown: boolean;
  detailViewKey?: string | null;
  detailParams: Record<string, string>;
}

export interface StatisticRowData {
  rowKey: string;
  rowLabel: string;
  cells: StatisticCellData[];
}

export interface StatisticBoardResponse {
  definition: StatisticBoardDefinition;
  appliedFilters: Record<string, string>;
  rows: StatisticRowData[];
  meta: StatisticBoardMeta;
}

export interface StatisticDetailResponse {
  title: string;
  description: string;
  columns: StatisticDetailColumn[];
  records: Record<string, unknown>[];
  total: number;
  page: number;
  size: number;
  sortField?: string | null;
  sortOrder?: string | null;
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
  getStatisticBoard(boardKey: string, filters?: Record<string, string>) {
    const params = new URLSearchParams(filters ?? {});
    const queryString = params.toString();
    return request<StatisticBoardResponse>(
      `/api/statistic-boards/${boardKey}${queryString ? `?${queryString}` : ''}`,
    );
  },
  getStatisticBoardDetails(
    boardKey: string,
    params: {
      rowKey: string;
      columnKey: string;
      page?: number;
      size?: number;
      sortField?: string;
      sortOrder?: string;
      filters?: Record<string, string>;
    },
  ) {
    const query = new URLSearchParams({
      rowKey: params.rowKey,
      columnKey: params.columnKey,
      page: String(params.page ?? 1),
      size: String(params.size ?? 10),
      ...(params.sortField ? { sortField: params.sortField } : {}),
      ...(params.sortOrder ? { sortOrder: params.sortOrder } : {}),
      ...(params.filters ?? {}),
    });
    return request<StatisticDetailResponse>(`/api/statistic-boards/${boardKey}/details?${query.toString()}`);
  },
  async exportStatisticBoard(boardKey: string, filters?: Record<string, string>) {
    const params = new URLSearchParams(filters ?? {});
    const queryString = params.toString();
    const response = await fetch(`/api/statistic-boards/${boardKey}/export${queryString ? `?${queryString}` : ''}`);
    if (!response.ok) {
      const text = await response.text();
      throw new Error(text || `Export failed: ${response.status}`);
    }
    return response.text();
  },
};
