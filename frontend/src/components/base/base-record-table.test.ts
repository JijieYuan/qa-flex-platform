import { describe, expect, it } from 'vitest';
import { mount } from '@vue/test-utils';
import ElementPlus from 'element-plus';
import BaseRecordTable from './BaseRecordTable.vue';

describe('BaseRecordTable', () => {
  it('emits query with the current keyword when query button is clicked', async () => {
    const wrapper = mount(BaseRecordTable, {
      props: {
        columns: [],
        rows: [],
        page: 1,
        pageSize: 20,
        total: 0,
      },
      global: {
        plugins: [ElementPlus],
      },
    });

    const input = wrapper.find('input');
    await input.setValue('review');
    const buttons = wrapper.findAll('button');
    const queryButton = buttons.find((button) => button.text() === '查询');

    expect(queryButton).toBeTruthy();
    await queryButton!.trigger('click');

    expect(wrapper.emitted('query')).toBeTruthy();
    expect(wrapper.emitted('query')?.[0]).toEqual(['review']);
  });
});
