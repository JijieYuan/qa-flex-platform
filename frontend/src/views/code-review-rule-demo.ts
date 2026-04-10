import type { CodeReviewIllegalRecordRowResponse, OptionItemResponse } from '../api';
import { AbstractRuleConfigDemoSupport, type RuleConfigDemoField, type RuleConfigDemoRule } from './rule-config-demo';

export type CodeReviewDemoRuleField = RuleConfigDemoField;
export type CodeReviewDemoRule = RuleConfigDemoRule;

const FALLBACK_ILLEGAL_TYPES = [
  '缺少模块标签',
  '缺少标注责任人',
  '缺少代码注释比例',
  '缺少缺陷数量',
  '缺少新增代码行数',
];

class CodeReviewRuleConfigDemoSupport extends AbstractRuleConfigDemoSupport<CodeReviewIllegalRecordRowResponse> {
  buildFields(optionGroups: {
    repositoryNames: OptionItemResponse[];
    illegalTypes: OptionItemResponse[];
    targetBranches: OptionItemResponse[];
    mergedBys: OptionItemResponse[];
    moduleNames: OptionItemResponse[];
    projectNames: OptionItemResponse[];
  }) {
    return [
      { key: 'repositoryName', label: '代码库', type: 'select', operators: this.selectOperators(), options: optionGroups.repositoryNames },
      { key: 'projectName', label: '项目名称', type: 'select', operators: this.selectOperators(), options: optionGroups.projectNames },
      { key: 'owner', label: '标注责任人', type: 'text', operators: this.textOperators() },
      { key: 'mergedBy', label: '合并人', type: 'select', operators: this.selectOperators(), options: optionGroups.mergedBys },
      { key: 'moduleName', label: '模块名称', type: 'select', operators: this.selectOperators(), options: optionGroups.moduleNames },
      { key: 'targetBranch', label: '目标分支', type: 'select', operators: this.selectOperators(), options: optionGroups.targetBranches },
      { key: 'illegalTypes', label: '非法类型', type: 'select', operators: this.selectOperators(), options: optionGroups.illegalTypes },
      { key: 'mergeRequestContent', label: '合并请求内容', type: 'text', operators: this.textOperators() },
      { key: 'commentRate', label: '代码注释比例', type: 'number', operators: this.numberOperators() },
      { key: 'defectCount', label: '缺陷数量', type: 'number', operators: this.numberOperators() },
      { key: 'addedLines', label: '新增代码行数', type: 'number', operators: this.numberOperators() },
    ] satisfies CodeReviewDemoRuleField[];
  }

  buildIllegalTypeOptions(options: OptionItemResponse[]) {
    if (options.length) {
      return options;
    }
    return FALLBACK_ILLEGAL_TYPES.map((item) => ({ label: item, value: item }));
  }

  createDefaultRules(fields: CodeReviewDemoRuleField[]) {
    return [
      this.createConfiguredRule(fields, 'moduleName', 'isEmpty', '', '缺少模块标签'),
      this.createConfiguredRule(fields, 'owner', 'isEmpty', '', '缺少标注责任人'),
      this.createConfiguredRule(fields, 'commentRate', 'isEmpty', '', '缺少代码注释比例'),
      this.createConfiguredRule(fields, 'defectCount', 'isEmpty', '', '缺少缺陷数量'),
      this.createConfiguredRule(fields, 'addedLines', 'isEmpty', '', '缺少新增代码行数'),
    ];
  }

  protected readFieldValue(row: CodeReviewIllegalRecordRowResponse, fieldKey: string) {
    if (fieldKey === 'illegalTypes') {
      return row.illegalTypes;
    }
    return row[fieldKey as keyof CodeReviewIllegalRecordRowResponse];
  }

  private createConfiguredRule(
    fields: CodeReviewDemoRuleField[],
    fieldKey: string,
    operator: CodeReviewDemoRule['operator'],
    value: string,
    illegalType: string,
  ) {
    const field = fields.find((item) => item.key === fieldKey);
    const rule = this.createRule(field, illegalType);
    rule.fieldKey = fieldKey;
    rule.operator = operator;
    rule.value = value;
    rule.illegalType = illegalType;
    return rule;
  }
}

export const codeReviewRuleConfigDemoSupport = new CodeReviewRuleConfigDemoSupport();
