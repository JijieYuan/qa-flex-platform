import type { DrilldownActiveCell } from '../../../types/drilldown-table';
import type {
  DrilldownBoardViewModel,
  DrilldownFilterState,
} from '../../drilldown-table/types';
import { AbstractDrilldownTableDefinition } from '../../drilldown-table/types';

export class QualitySummaryTableDefinition extends AbstractDrilldownTableDefinition {
  buildViewModel(): DrilldownBoardViewModel {
    return {
      chip: '模块一 / 汇总统计试运行',
      title: '研发质量汇总分析表',
      subtitle:
        '先看整体分布，再按单个统计值下钻到对应明细记录，形成“汇总总览 - 明细追溯”的完整分析链路。',
      filterFields: [
        {
          key: 'version',
          label: '统计版本',
          width: 148,
          options: [
            { label: 'CC2026R2', value: 'CC2026R2' },
            { label: 'CC2026R1', value: 'CC2026R1' },
          ],
        },
        {
          key: 'round',
          label: '轮次',
          width: 128,
          options: [
            { label: '第 1 轮', value: '第 1 轮' },
            { label: '第 2 轮', value: '第 2 轮' },
            { label: '累计', value: '累计' },
          ],
        },
        {
          key: 'owner',
          label: '负责人',
          width: 146,
          options: [
            { label: '全部负责人', value: '全部负责人' },
            { label: 'Super Admin', value: 'Super Admin' },
            { label: 'Jason', value: 'Jason' },
          ],
        },
      ],
      initialFilters: {
        version: 'CC2026R2',
        round: '第 2 轮',
        owner: '全部负责人',
      },
      stats: [
        { label: '统计范围', value: 'GitLab 镜像 / rocksdb / 缺陷视图' },
        { label: '统计口径', value: '模块 x 缺陷分类 x 当前轮次' },
        { label: '最后刷新', value: '2026-03-26 10:30:45' },
        { label: '执行耗时', value: '1.28 秒' },
      ],
      boardTitle: '汇总统计矩阵',
      boardDescription:
        '左侧是模块维度，顶部是缺陷分类与统计指标。蓝色统计值支持继续查看明细。',
      drawerDescription:
        '这里展示的是当前统计单元格对应的明细记录，可继续扩展排序、字段模板与操作列。',
      columnGroups: [
        {
          key: 'level-1',
          label: '一级缺陷',
          columns: [
            { key: 'fallback', label: '回退(个)', drilldown: true, metricType: 'count' },
            { key: 'hang', label: '挂机(个)', drilldown: true, metricType: 'count' },
            { key: 'other', label: '其他(个)', drilldown: true, metricType: 'count' },
            { key: 'repaired', label: '已修复数', drilldown: false, metricType: 'count' },
            { key: 'remaining', label: '剩余数', drilldown: true, metricType: 'count' },
            {
              key: 'repaired-rate',
              label: '修复率(%)',
              drilldown: false,
              metricType: 'ratio',
              format: (value: number) => `${value.toFixed(2)}%`,
            },
          ],
        },
        {
          key: 'level-2',
          label: '二级缺陷',
          columns: [
            { key: 'critical', label: '严重(个)', drilldown: true, metricType: 'count' },
            { key: 'major', label: '一般(个)', drilldown: true, metricType: 'count' },
            { key: 'minor', label: '轻微(个)', drilldown: true, metricType: 'count' },
          ],
        },
        {
          key: 'risk',
          label: '风险建议',
          columns: [
            { key: 'suggestion', label: '建议类缺陷(个)', drilldown: true, metricType: 'count' },
          ],
        },
      ],
      rows: [
        {
          rowKey: 'working-drawing',
          rowLabel: '工程图',
          values: {
            fallback: 6,
            hang: 4,
            other: 13,
            repaired: 19,
            remaining: 23,
            'repaired-rate': 82.61,
            critical: 64,
            major: 102,
            minor: 8,
            suggestion: 0,
          },
        },
        {
          rowKey: 'feature',
          rowLabel: '特征',
          values: {
            fallback: 2,
            hang: 3,
            other: 3,
            repaired: 7,
            remaining: 8,
            'repaired-rate': 87.5,
            critical: 36,
            major: 87,
            minor: 12,
            suggestion: 0,
          },
        },
        {
          rowKey: 'assembly',
          rowLabel: '装配',
          values: {
            fallback: 1,
            hang: 1,
            other: 1,
            repaired: 1,
            remaining: 3,
            'repaired-rate': 33.33,
            critical: 24,
            major: 49,
            minor: 5,
            suggestion: 0,
          },
        },
        {
          rowKey: 'sketch',
          rowLabel: '草图',
          values: {
            fallback: 0,
            hang: 1,
            other: 1,
            repaired: 1,
            remaining: 2,
            'repaired-rate': 50,
            critical: 12,
            major: 25,
            minor: 6,
            suggestion: 0,
          },
        },
        {
          rowKey: 'tooling',
          rowLabel: '工具',
          values: {
            fallback: 0,
            hang: 0,
            other: 0,
            repaired: 0,
            remaining: 0,
            'repaired-rate': 0,
            critical: 15,
            major: 25,
            minor: 3,
            suggestion: 0,
          },
        },
      ],
      detailColumns: [
        { prop: 'issueNo', label: '缺陷编号', width: 120 },
        { prop: 'title', label: '缺陷标题', minWidth: 240 },
        { prop: 'moduleName', label: '模块', width: 110 },
        { prop: 'issueLevel', label: '缺陷层级', width: 120 },
        { prop: 'category', label: '统计分类', width: 140 },
        { prop: 'owner', label: '负责人', width: 120 },
        { prop: 'reporter', label: '提报人', width: 120 },
        { prop: 'status', label: '状态', width: 110 },
        { prop: 'discoveredAt', label: '发现时间', width: 168 },
      ],
    };
  }

  buildDetails(activeCell: DrilldownActiveCell): Record<string, unknown>[] {
    const count = Math.max(0, Math.round(activeCell.row.values[activeCell.column.key] ?? 0));
    return Array.from({ length: count }, (_, index) => ({
      id: `${activeCell.row.rowKey}-${activeCell.group.key}-${activeCell.column.key}-${index + 1}`,
      issueNo: `DEF-${String(index + 1).padStart(4, '0')}`,
      moduleName: activeCell.row.rowLabel,
      issueLevel: activeCell.group.label,
      category: activeCell.column.label,
      title: `${activeCell.row.rowLabel} / ${activeCell.column.label} / 缺陷 ${index + 1}`,
      owner: ['Super Admin', 'Jason', 'Lynn', 'Mila'][index % 4],
      reporter: ['QA-王敏', 'QA-陈浩', 'QA-Lisa'][index % 3],
      status: ['待修复', '修复中', '待验证', '已关闭'][index % 4],
      discoveredAt: `2026-03-${String((index % 9) + 18).padStart(2, '0')} 1${index % 8}:3${index % 6}`,
    }));
  }

  getDrawerTags(filters: DrilldownFilterState, activeCell: DrilldownActiveCell | null): string[] {
    const tags: string[] = [];
    if (filters.version) {
      tags.push(filters.version);
    }
    if (filters.round) {
      tags.push(filters.round);
    }
    if (activeCell?.row?.rowLabel) {
      tags.push(activeCell.row.rowLabel);
    }
    return tags;
  }
}
