import type { RecordTableFilterOption } from '../../types/record-table';
import { matchesAbstractSearchText } from './abstract-search';

export function matchesSmartSelectOption(option: RecordTableFilterOption, query: string) {
  if (!query.trim()) {
    return true;
  }

  return matchesAbstractSearchText(option.label, query) || matchesAbstractSearchText(option.value, query);
}
