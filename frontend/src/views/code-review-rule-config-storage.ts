import type { CodeReviewRuleConfig } from '../types/code-review-rule-config';
import { cloneCodeReviewRuleConfig } from './code-review-rule-config-utils';

const STORAGE_KEY = 'code-review-illegal-records.rule-config.v2';

interface StoredCodeReviewRuleConfig {
  version: 2;
  config: CodeReviewRuleConfig;
}

export function loadStoredCodeReviewRuleConfig() {
  if (typeof window === 'undefined') {
    return null;
  }
  const raw = window.localStorage.getItem(STORAGE_KEY);
  if (!raw) {
    return null;
  }
  try {
    const parsed = JSON.parse(raw) as Partial<StoredCodeReviewRuleConfig>;
    if (parsed.version !== 2 || !parsed.config) {
      return null;
    }
    return cloneCodeReviewRuleConfig(parsed.config);
  } catch {
    return null;
  }
}

export function saveStoredCodeReviewRuleConfig(config: CodeReviewRuleConfig) {
  if (typeof window === 'undefined') {
    return;
  }
  const payload: StoredCodeReviewRuleConfig = {
    version: 2,
    config: cloneCodeReviewRuleConfig(config),
  };
  window.localStorage.setItem(STORAGE_KEY, JSON.stringify(payload));
}

export function clearStoredCodeReviewRuleConfig() {
  if (typeof window === 'undefined') {
    return;
  }
  window.localStorage.removeItem(STORAGE_KEY);
}
