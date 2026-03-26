// @vitest-environment jsdom
import { afterEach, describe, expect, it, vi } from 'vitest';
import { api } from './api';

describe('statistic board api', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('requests statistic board with query params', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      new Response(
        JSON.stringify({
          success: true,
          data: {
            definition: { boardKey: 'mirror-table-overview', filters: [], columnGroups: [], detailColumns: [] },
            appliedFilters: {},
            rows: [],
            meta: null,
          },
        }),
        { status: 200, headers: { 'Content-Type': 'application/json' } },
      ),
    );

    await api.getStatisticBoard('mirror-table-overview', { tableName: 'issues' });

    expect(fetchMock).toHaveBeenCalledWith('/api/statistic-boards/mirror-table-overview?tableName=issues', expect.any(Object));
  });

  it('requests statistic board details and export', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch');
    fetchMock
      .mockResolvedValueOnce(
        new Response(
          JSON.stringify({
            success: true,
            data: {
              title: 'issues / 总记录数',
              description: 'detail',
              columns: [],
              records: [],
              total: 0,
              page: 1,
              size: 10,
              sortField: 'syncedAt',
              sortOrder: 'descending',
            },
          }),
          { status: 200, headers: { 'Content-Type': 'application/json' } },
        ),
      )
      .mockResolvedValueOnce(new Response('csv-content', { status: 200, headers: { 'Content-Type': 'text/csv' } }));

    await api.getStatisticBoardDetails('mirror-table-overview', {
      rowKey: 'issues',
      columnKey: 'totalRecords',
      page: 2,
      size: 20,
      sortField: 'syncedAt',
      sortOrder: 'descending',
      filters: { tableName: 'issues' },
    });

    await api.exportStatisticBoard('mirror-table-overview', { tableName: 'issues' });

    expect(fetchMock).toHaveBeenNthCalledWith(
      1,
      '/api/statistic-boards/mirror-table-overview/details?rowKey=issues&columnKey=totalRecords&page=2&size=20&sortField=syncedAt&sortOrder=descending&tableName=issues',
      expect.any(Object),
    );
    expect(fetchMock).toHaveBeenNthCalledWith(
      2,
      '/api/statistic-boards/mirror-table-overview/export?tableName=issues',
    );
  });
});
