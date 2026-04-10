import { describe, expect, it } from 'vitest';
import { codeReviewRuleConfigDemoSupport } from './code-review-rule-demo';

const rows = [
  {
    requestType: 'merge_request',
    mergeRequestId: 1,
    mergeRequestIid: 101,
    projectId: 1,
    mergeRequestContent: 'Fix auth module',
    mergeRequestLink: null,
    owner: '张三',
    projectName: '项目A',
    repositoryName: 'repo-a',
    mergedAt: '2026-04-10T10:00:00',
    mergedBy: '李四',
    moduleName: '',
    targetBranch: 'master',
    illegalTypes: ['缺少模块标签'],
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
    projectName: '项目B',
    repositoryName: 'repo-b',
    mergedAt: '2026-04-10T11:00:00',
    mergedBy: '王五',
    moduleName: '文档模块',
    targetBranch: 'release',
    illegalTypes: [],
    commentRate: null,
    defectCount: null,
    addedLines: 5,
  },
];

describe('code review rule demo', () => {
  it('builds fields and default rules from shared abstract support', () => {
    const fields = codeReviewRuleConfigDemoSupport.buildFields({
      repositoryNames: [{ label: 'repo-a', value: 'repo-a' }],
      illegalTypes: [{ label: '缺少模块标签', value: '缺少模块标签' }],
      targetBranches: [],
      mergedBys: [],
      moduleNames: [],
      projectNames: [],
    });

    const defaults = codeReviewRuleConfigDemoSupport.createDefaultRules(fields);
    expect(fields[0].label).toBe('代码库');
    expect(defaults[0].illegalType).toBe('缺少模块标签');
    expect(codeReviewRuleConfigDemoSupport.operatorLabel('contains')).toBe('包含');
    expect(codeReviewRuleConfigDemoSupport.usesValueInput('isEmpty')).toBe(false);
    expect(codeReviewRuleConfigDemoSupport.buildIllegalTypeOptions([])[0].label).toBe('缺少模块标签');
  });

  it('matches and counts sentence rules', () => {
    const fields = codeReviewRuleConfigDemoSupport.buildFields({
      repositoryNames: [],
      illegalTypes: [{ label: '缺少模块标签', value: '缺少模块标签' }],
      targetBranches: [],
      mergedBys: [],
      moduleNames: [],
      projectNames: [],
    });
    const rule = codeReviewRuleConfigDemoSupport.createDefaultRules(fields)[0];

    expect(codeReviewRuleConfigDemoSupport.matchesRule(rows[0], rule, fields)).toBe(true);
    expect(codeReviewRuleConfigDemoSupport.matchesRule(rows[1], rule, fields)).toBe(false);
    expect(codeReviewRuleConfigDemoSupport.countMatches(rows, rule, fields)).toBe(1);
    expect(codeReviewRuleConfigDemoSupport.describeRule(rule, fields)).toContain('模块名称为空');
  });

  it('filters rows by any matched rule', () => {
    const fields = codeReviewRuleConfigDemoSupport.buildFields({
      repositoryNames: [],
      illegalTypes: [],
      targetBranches: [],
      mergedBys: [],
      moduleNames: [],
      projectNames: [],
    });
    const rules = codeReviewRuleConfigDemoSupport.createDefaultRules(fields).slice(0, 2);

    const result = codeReviewRuleConfigDemoSupport.evaluateRows(rows, rules, fields);
    expect(result).toHaveLength(2);
    expect(result.map((item) => item.mergeRequestIid)).toEqual([101, 102]);
  });
});
