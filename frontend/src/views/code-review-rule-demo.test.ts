import { describe, expect, it } from 'vitest';
import {
  buildCodeReviewDemoIllegalTypeOptions,
  buildCodeReviewDemoRuleFields,
  codeReviewDemoOperatorLabel,
  countCodeReviewDemoRuleMatches,
  createDefaultCodeReviewDemoRules,
  describeCodeReviewDemoRule,
  evaluateCodeReviewDemoRules,
  matchesCodeReviewDemoRule,
  usesValueInput,
} from './code-review-rule-demo';

const rows = [
  {
    requestType: 'merge_request',
    mergeRequestId: 1,
    mergeRequestIid: 101,
    projectId: 1,
    mergeRequestContent: 'Fix auth module',
    mergeRequestLink: null,
    owner: '\u5f20\u4e09',
    projectName: '\u9879\u76eeA',
    repositoryName: 'repo-a',
    mergedAt: '2026-04-10T10:00:00',
    mergedBy: '\u674e\u56db',
    moduleName: '',
    targetBranch: 'master',
    illegalTypes: ['\u7f3a\u5c11\u6a21\u5757\u6807\u7b7e'],
    commentRate: 12,
    defectCount: 3,
    addedLines: 40,
  },
  {
    requestType: 'merge_request',
    mergeRequestId: 2,
    mergeRequestIid: 102,
    projectId: 1,
    mergeRequestContent: 'Docs update',
    mergeRequestLink: null,
    owner: '',
    projectName: '\u9879\u76eeB',
    repositoryName: 'repo-b',
    mergedAt: '2026-04-10T11:00:00',
    mergedBy: '\u738b\u4e94',
    moduleName: '\u6587\u6863\u6a21\u5757',
    targetBranch: 'release',
    illegalTypes: [],
    commentRate: null,
    defectCount: null,
    addedLines: 5,
  },
];

describe('code review rule demo', () => {
  it('builds fields and default rules', () => {
    const fields = buildCodeReviewDemoRuleFields({
      repositoryNames: [{ label: 'repo-a', value: 'repo-a' }],
      illegalTypes: [{ label: '\u7f3a\u5c11\u6a21\u5757\u6807\u7b7e', value: '\u7f3a\u5c11\u6a21\u5757\u6807\u7b7e' }],
      targetBranches: [],
      mergedBys: [],
      moduleNames: [],
      projectNames: [],
    });

    const defaults = createDefaultCodeReviewDemoRules(fields);
    expect(fields[0].label).toBe('\u4ee3\u7801\u5e93');
    expect(defaults[0].illegalType).toBe('\u7f3a\u5c11\u6a21\u5757\u6807\u7b7e');
    expect(codeReviewDemoOperatorLabel('contains')).toBe('\u5305\u542b');
    expect(usesValueInput('isEmpty')).toBe(false);
    expect(buildCodeReviewDemoIllegalTypeOptions([])[0].label).toBe('\u7f3a\u5c11\u6a21\u5757\u6807\u7b7e');
  });

  it('matches and counts sentence rules', () => {
    const fields = buildCodeReviewDemoRuleFields({
      repositoryNames: [],
      illegalTypes: [{ label: '\u7f3a\u5c11\u6a21\u5757\u6807\u7b7e', value: '\u7f3a\u5c11\u6a21\u5757\u6807\u7b7e' }],
      targetBranches: [],
      mergedBys: [],
      moduleNames: [],
      projectNames: [],
    });
    const rule = createDefaultCodeReviewDemoRules(fields)[0];

    expect(matchesCodeReviewDemoRule(rows[0], rule, fields)).toBe(true);
    expect(matchesCodeReviewDemoRule(rows[1], rule, fields)).toBe(false);
    expect(countCodeReviewDemoRuleMatches(rows, rule, fields)).toBe(1);
    expect(describeCodeReviewDemoRule(rule, fields)).toContain('\u6a21\u5757\u540d\u79f0\u4e3a\u7a7a');
  });

  it('filters rows by any matched rule', () => {
    const fields = buildCodeReviewDemoRuleFields({
      repositoryNames: [],
      illegalTypes: [],
      targetBranches: [],
      mergedBys: [],
      moduleNames: [],
      projectNames: [],
    });
    const rules = createDefaultCodeReviewDemoRules(fields).slice(0, 2);

    const result = evaluateCodeReviewDemoRules(rows, rules, fields);
    expect(result).toHaveLength(2);
    expect(result.map((item) => item.mergeRequestIid)).toEqual([101, 102]);
  });
});
