import { pinyin } from 'pinyin-pro';
import type { RecordTableFilterOption } from '../../types/record-table';

function normalizeText(text: string) {
  return text.trim().toLowerCase();
}

function compactText(text: string) {
  return normalizeText(text).replace(/\s+/g, '');
}

function buildInitials(text: string) {
  const normalized = normalizeText(text);
  if (!normalized) {
    return '';
  }
  if (/^[a-z0-9\s]+$/i.test(normalized)) {
    return normalized
      .split(/[^a-z0-9]+/i)
      .filter(Boolean)
      .map((part) => part[0] ?? '')
      .join('');
  }

  return pinyin(text, { pattern: 'first', toneType: 'none', type: 'string' }).replace(/\s+/g, '').toLowerCase();
}

function buildSpell(text: string) {
  const normalized = compactText(text);
  if (!normalized) {
    return '';
  }
  if (/^[a-z0-9]+$/i.test(normalized)) {
    return normalized;
  }

  return pinyin(text, { toneType: 'none', type: 'string' }).replace(/\s+/g, '').toLowerCase();
}

export function matchesSmartSelectOption(option: RecordTableFilterOption, query: string) {
  const normalizedQuery = compactText(query);
  if (!normalizedQuery) {
    return true;
  }

  const label = normalizeText(option.label);
  const value = normalizeText(option.value);
  const compactLabel = compactText(option.label);
  const compactValue = compactText(option.value);
  const labelSpell = buildSpell(option.label);
  const valueSpell = buildSpell(option.value);
  const labelInitials = buildInitials(option.label);
  const valueInitials = buildInitials(option.value);

  return (
    label.includes(normalizedQuery) ||
    value.includes(normalizedQuery) ||
    compactLabel.includes(normalizedQuery) ||
    compactValue.includes(normalizedQuery) ||
    labelSpell.includes(normalizedQuery) ||
    valueSpell.includes(normalizedQuery) ||
    labelInitials.includes(normalizedQuery) ||
    valueInitials.includes(normalizedQuery)
  );
}
