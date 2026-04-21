import { describe, expect, it } from 'vitest';
import type { CodeReviewRuleConfig } from '../types/code-review-rule-config';
import { buildCodeReviewRuleFields } from './code-review-rule-config-schema';
import { normalizeCodeReviewRuleConfig } from './code-review-rule-config-utils';

const fields = buildCodeReviewRuleFields({
  requestTypes: [],
  repositoryNames: [],
  illegalTypes: [],
  targetBranches: [
    { label: 'master', value: 'master' },
    { label: 'dev', value: 'dev' },
  ],
  mergedBys: [],
  moduleNames: [],
  projectNames: [],
});

describe('code review rule config templates', () => {
  it('only exposes meaningful illegal-judgement rules', () => {
    expect(fields.map((field) => field.key)).toEqual([
      'moduleName',
      'owner',
      'targetBranch',
      'mergeRequestContent',
      'commentRateMissing',
      'commentRateLow',
      'defectCountMissing',
      'defectCountHigh',
      'addedLinesMissing',
      'addedLinesHigh',
    ]);
    expect(fields.find((field) => field.key === 'moduleName')?.operators).toEqual(['isEmpty']);
    expect(fields.find((field) => field.key === 'targetBranch')?.operators).toEqual(['notIn']);
    expect(fields.every((field) => !field.operators.includes('isNotEmpty'))).toBe(true);
  });

  it('normalizes old meaningless conditions back to a valid judgement rule', () => {
    const config: CodeReviewRuleConfig = {
      enabled: true,
      updatedAt: null,
      groups: [
        {
          id: 'g1',
          matchMode: 'all',
          conditions: [
            {
              id: 'c1',
              fieldKey: 'moduleName',
              operator: 'isNotEmpty',
              value: '',
            },
            {
              id: 'c2',
              fieldKey: 'repositoryName',
              operator: 'eq',
              value: 'repo-a',
            },
          ],
        },
      ],
    };

    const normalized = normalizeCodeReviewRuleConfig(config, fields);

    expect(normalized.groups[0].conditions[0]).toMatchObject({
      fieldKey: 'moduleName',
      operator: 'isEmpty',
      value: '',
    });
    expect(normalized.groups[0].conditions[1]).toMatchObject({
      fieldKey: 'moduleName',
      operator: 'isEmpty',
      value: '',
    });
  });
});
