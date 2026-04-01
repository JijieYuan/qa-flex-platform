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

export interface GitlabSyncTask {
  id: number;
  runId: string;
  taskType: string;
  triggerType: string;
  sourceMode: SourceMode;
  scopeKey: string;
  dedupeKey: string;
  status: string;
  cancelRequested: boolean;
  pendingResync: boolean;
  retryCount: number;
  cooldownUntil?: string | null;
  heartbeatAt?: string | null;
  queuedAt?: string | null;
  runAfter?: string | null;
  startedAt?: string | null;
  finishedAt?: string | null;
  finishedReason?: string | null;
  lockOwner?: string | null;
  payloadJson?: string | null;
}

export type SyncSubmissionAction =
  | 'CREATED'
  | 'QUEUED'
  | 'REUSED_ACTIVE'
  | 'REUSED_QUEUED'
  | 'DEDUPED';

export interface SyncSubmissionResponse {
  accepted: boolean;
  taskId: number;
  status: string;
  action: SyncSubmissionAction;
  message: string;
}

export type MirrorPurgeScope = 'MIRROR_DATA_ONLY' | 'MIRROR_DATA_EXCLUDING_CURRENT_WHITELIST';

export interface MirrorPurgeResult {
  scope: MirrorPurgeScope;
  droppedMirrorTables: number;
  droppedTableNames: string[];
  truncatedTables: number;
  truncatedTableNames: string[];
  syncTimestampsReset: boolean;
}

export interface MirrorStatusResponse {
  config: GitlabSyncConfig;
  currentTask?: GitlabSyncTask | null;
  currentStatus: string;
  currentMessage: string;
  currentStartedAt?: string | null;
  progress?: SyncProgress | null;
  logs: GitlabSyncLog[];
  webhookUrl: string;
  webhookRegistration?: GitlabWebhookRegistrationStatus | null;
}

export interface GitlabRegisteredWebhook {
  id: number;
  url: string;
  issuesEvents: boolean;
  mergeRequestsEvents: boolean;
  noteEvents: boolean;
  pipelineEvents: boolean;
  jobEvents: boolean;
  releasesEvents: boolean;
  enableSslVerification: boolean;
}

export interface GitlabWebhookRegistrationStatus {
  supported: boolean;
  configured: boolean;
  registered: boolean;
  projectId?: number | null;
  webhookUrl: string;
  message: string;
  hooks: GitlabRegisteredWebhook[];
}

export interface StatisticFilterOption {
  label: string;
  value: string;
}

export type StatisticFilterFieldType = 'text' | 'select' | 'number' | 'datetime';
export type StatisticFilterOperator =
  | 'eq'
  | 'ne'
  | 'contains'
  | 'notContains'
  | 'gt'
  | 'gte'
  | 'lt'
  | 'lte'
  | 'between'
  | 'year'
  | 'month'
  | 'day'
  | 'at'
  | 'before'
  | 'after';

export interface StatisticFilterField {
  key: string;
  label: string;
  type: StatisticFilterFieldType;
  placeholder?: string | null;
  defaultValue?: string | null;
  width?: number | null;
  operators: StatisticFilterOperator[];
  options: StatisticFilterOption[];
}

export interface StatisticFilterCondition {
  fieldKey: string;
  operator: StatisticFilterOperator;
  value: string;
  secondaryValue?: string | null;
}

export interface StatisticFilterGroup {
  logic: 'AND' | 'OR';
  conditions: StatisticFilterCondition[];
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
  rowHeaderLabel: string;
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
  appliedFilterGroup?: StatisticFilterGroup | null;
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

export interface DatabaseTableOption {
  tableName: string;
  label: string;
  syncStatus: string;
  lastSyncTime?: string | null;
}

export interface DatabaseTableColumn {
  key: string;
  label: string;
  sortable: boolean;
}

export interface DatabaseTableRowsResponse {
  tableName: string;
  label: string;
  columns: DatabaseTableColumn[];
  rows: Record<string, unknown>[];
  total: number;
  page: number;
  size: number;
  sortField?: string | null;
  sortOrder?: string | null;
  keyword?: string | null;
  syncStatus: string;
  lastSyncTime?: string | null;
  statusMessage?: string | null;
}

export interface CollectFormDetailResponse {
  id: number;
  gitlabBaseUrl: string;
  projectId: number;
  requestIid?: number | null;
  resourceType: string;
  resourceId: string;
  templateCode: string;
  formTitle: string;
  reviewer: string;
  reviewDurationMinutes: number;
  specificationScore: number;
  logicScore: number;
  performanceScore: number;
  designScore: number;
  otherScore: number;
  remark?: string | null;
  deleted: boolean;
  createdAt?: string | null;
  updatedAt?: string | null;
}

export interface CollectFormNotificationPayloadResponse {
  sourceAddress: string;
  projectId: number;
  requestIid: number;
  resourceType: string;
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
  getWebhookRegistrationStatus() {
    return request<GitlabWebhookRegistrationStatus>('/api/gitlab-sync/webhook-registration-status');
  },
  getWhitelistOptions() {
    return request<TableWhitelistOption[]>('/api/gitlab-sync/whitelist-options');
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
    return request<SyncSubmissionResponse>('/api/gitlab-sync/full-sync', {
      method: 'POST',
    });
  },
  startIncrementalSync() {
    return request<SyncSubmissionResponse>('/api/gitlab-sync/incremental-sync', {
      method: 'POST',
    });
  },
  registerWebhook() {
    return request<GitlabWebhookRegistrationStatus>('/api/gitlab-sync/register-webhook', {
      method: 'POST',
    });
  },
  cancelSync() {
    return request<{ accepted: boolean; taskId?: number; status?: string }>('/api/gitlab-sync/cancel', {
      method: 'POST',
    });
  },
  purgeMirrorData(scope: MirrorPurgeScope) {
    return request<MirrorPurgeResult>('/api/gitlab-sync/purge', {
      method: 'POST',
      body: JSON.stringify({ scope }),
    });
  },
  getStatisticBoard(boardKey: string, params?: { filters?: Record<string, string>; filterGroup?: StatisticFilterGroup | null }) {
    const searchParams = new URLSearchParams(params?.filters ?? {});
    if (params?.filterGroup && params.filterGroup.conditions.length) {
      searchParams.set('filterGroup', JSON.stringify(params.filterGroup));
    }
    const queryString = searchParams.toString();
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
      filterGroup?: StatisticFilterGroup | null;
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
    if (params.filterGroup && params.filterGroup.conditions.length) {
      query.set('filterGroup', JSON.stringify(params.filterGroup));
    }
    return request<StatisticDetailResponse>(`/api/statistic-boards/${boardKey}/details?${query.toString()}`);
  },
  async exportStatisticBoard(boardKey: string, params?: { filters?: Record<string, string>; filterGroup?: StatisticFilterGroup | null }) {
    const searchParams = new URLSearchParams(params?.filters ?? {});
    if (params?.filterGroup && params.filterGroup.conditions.length) {
      searchParams.set('filterGroup', JSON.stringify(params.filterGroup));
    }
    const queryString = searchParams.toString();
    const response = await fetch(`/api/statistic-boards/${boardKey}/export${queryString ? `?${queryString}` : ''}`);
    if (!response.ok) {
      const text = await response.text();
      throw new Error(text || `Export failed: ${response.status}`);
    }
    return response.text();
  },
  getDatabaseTables() {
    return request<DatabaseTableOption[]>('/api/database-browser/tables');
  },
  getDatabaseTableRows(params: {
    tableName: string;
    page?: number;
    size?: number;
    keyword?: string;
    sortField?: string;
    sortOrder?: 'asc' | 'desc';
  }) {
    const query = new URLSearchParams({
      tableName: params.tableName,
      page: String(params.page ?? 1),
      size: String(params.size ?? 20),
      ...(params.keyword ? { keyword: params.keyword } : {}),
      ...(params.sortField ? { sortField: params.sortField } : {}),
      ...(params.sortOrder ? { sortOrder: params.sortOrder } : {}),
    });
    return request<DatabaseTableRowsResponse>(`/api/database-browser/rows?${query.toString()}`);
  },
  getCollectFormDetail(params: {
    gitlabBaseUrl: string;
    projectId: number;
    resourceType: string;
    resourceId: string;
    templateCode: string;
  }) {
    const query = new URLSearchParams({
      gitlabBaseUrl: params.gitlabBaseUrl,
      projectId: String(params.projectId),
      resourceType: params.resourceType,
      resourceId: params.resourceId,
      templateCode: params.templateCode,
    });
    return request<CollectFormDetailResponse | null>(`/api/collect-forms/detail?${query.toString()}`);
  },
  saveCollectForm(payload: {
    gitlabBaseUrl: string;
    projectId: number;
    requestIid?: number | null;
    resourceType: string;
    resourceId: string;
    templateCode: string;
    formTitle: string;
    reviewer: string;
    reviewDurationMinutes: number;
    specificationScore: number;
    logicScore: number;
    performanceScore: number;
    designScore: number;
    otherScore: number;
    remark: string;
  }) {
    return request<CollectFormDetailResponse>('/api/collect-forms/save', {
      method: 'POST',
      body: JSON.stringify(payload),
    });
  },
  deleteCollectForm(payload: {
    gitlabBaseUrl: string;
    projectId: number;
    resourceType: string;
    resourceId: string;
    templateCode: string;
  }) {
    return request<boolean>('/api/collect-forms/delete', {
      method: 'POST',
      body: JSON.stringify(payload),
    });
  },
  getCollectFormNotificationPayload(params: {
    gitlabBaseUrl: string;
    projectId: number;
    requestIid: number;
    resourceType: string;
  }) {
    const query = new URLSearchParams({
      gitlabBaseUrl: params.gitlabBaseUrl,
      projectId: String(params.projectId),
      requestIid: String(params.requestIid),
      resourceType: params.resourceType,
    });
    return request<CollectFormNotificationPayloadResponse>(
      `/api/collect-forms/notification-payload?${query.toString()}`,
    );
  },
};
