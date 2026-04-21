import { describe, expect, it, vi } from 'vitest';
import { mount, flushPromises } from '@vue/test-utils';
import { createRouter, createWebHashHistory } from 'vue-router';
import ElementPlus from 'element-plus';
import CodeReviewIllegalRuleConfigView from './CodeReviewIllegalRuleConfigView.vue';

function jsonResponse(data: unknown) {
  return Promise.resolve({ ok: true, text: () => Promise.resolve(JSON.stringify({ success: true, data })) } as Response);
}

describe('CodeReviewIllegalRuleConfigView mount smoke', () => {
  it('mounts without route errors', async () => {
    vi.stubGlobal('fetch', vi.fn((url: string) => {
      if (url.includes('/api/code-review/illegal-records/filter-options')) {
        return jsonResponse({
          requestTypes: [{ label: '合并请求', value: 'merge_request' }],
          repositoryNames: [],
          illegalTypes: [],
          targetBranches: [],
          mergedBys: [],
          moduleNames: [],
          projectNames: [],
        });
      }
      if (url.includes('/api/code-review/illegal-records/rule-config/preview')) {
        return jsonResponse({
          baseTotal: 10,
          filteredTotal: 10,
          deltaCount: 0,
          retainedRate: 100,
          samples: [],
        });
      }
      return jsonResponse({});
    }));

    const router = createRouter({
      history: createWebHashHistory(),
      routes: [{ path: '/code-review/illegal-records/rule-config', component: CodeReviewIllegalRuleConfigView }],
    });
    await router.push('/code-review/illegal-records/rule-config?projectId=1');
    await router.isReady();
    const wrapper = mount(CodeReviewIllegalRuleConfigView, {
      global: { plugins: [router, ElementPlus] },
    });
    await flushPromises();
    expect(wrapper.exists()).toBe(true);
    vi.unstubAllGlobals();
  });
});
