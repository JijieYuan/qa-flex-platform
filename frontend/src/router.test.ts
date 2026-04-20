import { beforeEach, describe, expect, it } from 'vitest';
import router, { normalizeQuery } from './router';

describe('router query normalization', () => {
  beforeEach(() => {
    sessionStorage.clear();
  });

  it('preserves serialized filterGroup on statistic board routes', () => {
    const to = router.resolve({
      path: '/quality-board/home',
      query: {
        detailPage: '2',
        filterGroup:
          '{"logic":"AND","conditions":[{"fieldKey":"tableName","operator":"eq","value":"issues","secondaryValue":""}]}',
      },
    });

    expect(normalizeQuery(to)).toBeNull();
  });

  it('preserves review-data keyword, serialized filterGroup, and legacy filter keys', () => {
    const to = router.resolve({
      path: '/review-data/home',
      query: {
        keyword: 'alpha',
        filterGroup:
          '{"logic":"OR","conditions":[{"fieldKey":"title","operator":"contains","value":"alpha","secondaryValue":""}]}',
        'filters.0.field': 'title',
        'filters.0.operator': 'contains',
        'filters.0.value': 'alpha',
      },
    });

    expect(normalizeQuery(to)).toBeNull();
  });
});
