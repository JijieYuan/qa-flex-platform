import { describe, expect, it } from 'vitest';
import { matchesSmartSelectOption } from './smart-select-search';

describe('smart-select-search', () => {
  it('matches Chinese name by initials', () => {
    expect(matchesSmartSelectOption({ label: '王强', value: '王强' }, 'wq')).toBe(true);
    expect(matchesSmartSelectOption({ label: '王青', value: '王青' }, 'wq')).toBe(true);
  });

  it('matches Chinese name by full pinyin', () => {
    expect(matchesSmartSelectOption({ label: '张晓涵', value: '张晓涵' }, 'zhangxiaohan')).toBe(true);
    expect(matchesSmartSelectOption({ label: '张晓涵', value: '张晓涵' }, 'zxh')).toBe(true);
  });

  it('matches English words by compact text', () => {
    expect(matchesSmartSelectOption({ label: 'Wang Qing', value: 'Wang Qing' }, 'wangqing')).toBe(true);
    expect(matchesSmartSelectOption({ label: 'Wang Qing', value: 'Wang Qing' }, 'wq')).toBe(true);
  });
});
