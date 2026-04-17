import type {
  ReviewDataRecordRowResponse,
  ReviewDataSummaryResponse,
} from '../api';
import type { RecordTableActiveFilterTag, RecordTableColumn, RecordTableTagValue } from '../types/record-table';

export interface ReviewDataSummaryCard {
  key: string;
  label: string;
  value: string;
}

export function reviewDataColumns(): RecordTableColumn[] {
  return [
    { key: 'formTitle', label: '评审标题', sortable: true, minWidth: 220, fixed: 'left' },
    { key: 'projectName', label: '项目', sortable: true, minWidth: 140 },
    { key: 'repositoryName', label: '代码库', sortable: true, minWidth: 180 },
    { key: 'moduleName', label: '模块', sortable: true, minWidth: 140 },
    { key: 'reviewer', label: '评审人', sortable: true, width: 120 },
    { key: 'reviewDurationMinutes', label: '评审时长(分钟)', type: 'number', sortable: true, width: 140, align: 'right' },
    { key: 'totalScore', label: '总分', type: 'number', sortable: true, width: 90, align: 'right' },
    { key: 'commentRate', label: '注释率(%)', sortable: true, width: 110, align: 'right' },
    { key: 'defectCount', label: '缺陷数', type: 'number', sortable: true, width: 100, align: 'right' },
    { key: 'addedLines', label: '新增代码行数', type: 'number', sortable: true, width: 130, align: 'right' },
    { key: 'recordStatus', label: '记录状态', type: 'tag', width: 100, align: 'center' },
    { key: 'updatedAt', label: '更新时间', type: 'datetime', sortable: true, minWidth: 170 },
  ];
}

export function buildReviewDataTableRows(rows: ReviewDataRecordRowResponse[]) {
  return rows.map((row) => ({
    __raw: row,
    formTitle: row.formTitle || row.mergeRequestTitle || `MR #${row.mergeRequestIid ?? '-'}`,
    projectName: row.projectName || '-',
    repositoryName: row.repositoryName || '-',
    moduleName: row.moduleName || '-',
    reviewer: row.reviewer || '-',
    reviewDurationMinutes: row.reviewDurationMinutes ?? 0,
    totalScore: row.totalScore ?? 0,
    commentRate: formatNullableNumber(row.commentRate, 2),
    defectCount: row.defectCount ?? 0,
    addedLines: row.addedLines ?? 0,
    recordStatus: [recordStatusTag(row.deleted)],
    updatedAt: formatDateTime(row.updatedAt),
  }));
}

export function buildReviewDataSummaryCards(summary: ReviewDataSummaryResponse | null): ReviewDataSummaryCard[] {
  if (!summary) {
    return [
      { key: 'total', label: '评审记录', value: '0' },
      { key: 'active', label: '有效记录', value: '0' },
      { key: 'duration', label: '平均时长', value: '0 分钟' },
      { key: 'score', label: '平均总分', value: '0.0' },
    ];
  }
  return [
    { key: 'total', label: '评审记录', value: String(summary.totalRecords) },
    { key: 'active', label: '有效记录', value: String(summary.activeRecords) },
    { key: 'duration', label: '平均时长', value: `${formatFixed(summary.averageDurationMinutes, 1)} 分钟` },
    { key: 'score', label: '平均总分', value: formatFixed(summary.averageTotalScore, 1) },
  ];
}

export function buildReviewDataFilterTags(values: Record<string, unknown>): RecordTableActiveFilterTag[] {
  const tags: RecordTableActiveFilterTag[] = [];
  pushTag(tags, 'projectName', '项目', values.projectName);
  pushTag(tags, 'repositoryName', '代码库', values.repositoryName);
  pushTag(tags, 'moduleName', '模块', values.moduleName);
  pushTag(tags, 'reviewer', '评审人', values.reviewer);
  pushTag(tags, 'templateCode', '模板编码', values.templateCode);
  pushTag(tags, 'targetBranch', '目标分支', values.targetBranch);
  pushTag(tags, 'recordStatus', '记录状态', values.recordStatus);
  pushTag(tags, 'keyword', '关键字', values.keyword);
  pushTag(tags, 'mergeRequestIid', '合并请求编号', values.mergeRequestIid);
  if (Array.isArray(values.updatedAtRange) && values.updatedAtRange.length === 2) {
    tags.push({
      key: 'updatedAtRange',
      label: '更新时间',
      value: `${values.updatedAtRange[0]} ~ ${values.updatedAtRange[1]}`,
    });
  }
  return tags;
}

function pushTag(tags: RecordTableActiveFilterTag[], key: string, label: string, value: unknown) {
  const text = String(value ?? '').trim();
  if (text) {
    tags.push({ key, label, value: text });
  }
}

function recordStatusTag(deleted: boolean): RecordTableTagValue {
  return deleted ? { label: '已作废', type: 'info' } : { label: '有效', type: 'success' };
}

function formatNullableNumber(value: number | null | undefined, fractionDigits = 0) {
  if (value == null) {
    return '-';
  }
  return formatFixed(value, fractionDigits);
}

function formatFixed(value: number, fractionDigits: number) {
  return Number.isFinite(value) ? value.toFixed(fractionDigits) : '0';
}

function formatDateTime(value?: string | null) {
  return value ? value.replace('T', ' ').slice(0, 19) : '-';
}
