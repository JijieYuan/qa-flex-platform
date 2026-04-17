import { describe, expect, it } from 'vitest';
import type { ReviewDataRecordRowResponse } from '../api';
import { buildReviewDataSummaryCards, buildReviewDataTableRows } from './review-data-management';

describe('review-data-management helpers', () => {
  it('should build compact summary cards from summary payload', () => {
    const cards = buildReviewDataSummaryCards({
      totalRecords: 12,
      activeRecords: 10,
      deletedRecords: 2,
      averageDurationMinutes: 35.25,
      averageTotalScore: 18.4,
      averageCommentRate: 12.56,
    });

    expect(cards[0].value).toBe('12');
    expect(cards[1].value).toBe('10');
    expect(cards[2].value).toBe('35.3 分钟');
    expect(cards[3].value).toBe('18.4');
  });

  it('should map review data rows to record table rows', () => {
    const rows: ReviewDataRecordRowResponse[] = [
      {
        id: 1,
        projectId: 1001,
        mergeRequestId: 2001,
        mergeRequestIid: 88,
        formTitle: '评审标题A',
        templateCode: 'code_review',
        reviewer: '张三',
        reviewDurationMinutes: 30,
        totalScore: 19,
        specificationScore: 4,
        logicScore: 4,
        performanceScore: 4,
        designScore: 4,
        otherScore: 3,
        remark: '备注A',
        deleted: false,
        projectName: '项目甲',
        repositoryName: 'repo/a',
        mergeRequestTitle: 'MR 标题A',
        moduleName: '模块A',
        targetBranch: 'master',
        commentRate: 12.5,
        defectCount: 2,
        addedLines: 180,
        createdAt: '2026-04-10T09:00:00',
        updatedAt: '2026-04-12T10:00:00',
      },
    ];

    const tableRows = buildReviewDataTableRows(rows);
    expect(tableRows[0].formTitle).toBe('评审标题A');
    expect(tableRows[0].commentRate).toBe('12.50');
    expect((tableRows[0].recordStatus as Array<{ label: string }>)[0].label).toBe('有效');
    expect(tableRows[0].updatedAt).toBe('2026-04-12 10:00:00');
  });
});
