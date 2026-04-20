
import { describe, expect, it, vi } from 'vitest';
import { mount, flushPromises } from '@vue/test-utils';
import { createRouter, createWebHashHistory } from 'vue-router';
import ElementPlus from 'element-plus';
import ReviewDataManagementView from './ReviewDataManagementView.vue';

function jsonResponse(data: unknown) {
  return Promise.resolve({ ok: true, text: () => Promise.resolve(JSON.stringify({ success: true, data })) } as Response);
}

describe('ReviewDataManagementView mount smoke', () => {
  it('mounts without route errors', async () => {
    vi.stubGlobal('fetch', vi.fn((url: string) => {
      if (url.includes('/filter-options')) {
        return jsonResponse({
          projectNames: [], moduleNames: [], reviewOwners: [], reviewTypes: [], reviewExperts: [],
          problemStatuses: [], reviewCategories: [], problemCategories: [],
        });
      }
      if (url.includes('/api/review-data/records?')) {
        return jsonResponse({
          records: [], total: 0, page: 1, size: 20, sortField: 'updatedAt', sortOrder: 'desc',
          summary: { totalRecords: 0, totalProblemItems: 0, averageReviewScalePages: 0, averageProblemCount: 0 },
        });
      }
      return jsonResponse({});
    }));

    const router = createRouter({
      history: createWebHashHistory(),
      routes: [{ path: '/review-data/home', component: ReviewDataManagementView }],
    });
    await router.push('/review-data/home');
    await router.isReady();
    const wrapper = mount(ReviewDataManagementView, {
      global: { plugins: [router, ElementPlus] },
    });
    await flushPromises();
    expect(wrapper.exists()).toBe(true);
    vi.unstubAllGlobals();
  });
});
