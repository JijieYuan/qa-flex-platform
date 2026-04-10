import type { CodeReviewIllegalRecordRowResponse, OptionItemResponse } from '../api';
import {
  AbstractRuleConfigSchemaSupport,
  RuleOperator,
  type RuleConfigField,
  type RuleConfigResultRule,
} from './rule-config-core';

export type CodeReviewDemoRuleField = RuleConfigField;
export type CodeReviewDemoRule = RuleConfigResultRule;

const FALLBACK_ILLEGAL_TYPES = [
  '缺少模块标签',
  '缺少标注责任人',
  '缺少代码注释比例',
  '缺少缺陷数量',
  '缺少新增代码行数',
];

class CodeReviewRuleConfigDemoSupport extends AbstractRuleConfigSchemaSupport<CodeReviewIllegalRecordRowResponse> {
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

  override createDefaultRules(fields: CodeReviewDemoRuleField[]) {
    return [
      this.createConfiguredRule(fields, 'moduleName', RuleOperator.IS_EMPTY, '', '缺少模块标签'),
      this.createConfiguredRule(fields, 'owner', RuleOperator.IS_EMPTY, '', '缺少标注责任人'),
      this.createConfiguredRule(fields, 'commentRate', RuleOperator.IS_EMPTY, '', '缺少代码注释比例'),
      this.createConfiguredRule(fields, 'defectCount', RuleOperator.IS_EMPTY, '', '缺少缺陷数量'),
      this.createConfiguredRule(fields, 'addedLines', RuleOperator.IS_EMPTY, '', '缺少新增代码行数'),
    ];
  }

  protected override readFieldValue(row: CodeReviewIllegalRecordRowResponse, fieldKey: string) {
    if (fieldKey === 'illegalTypes') {
      return row.illegalTypes;
    }
    return row[fieldKey as keyof CodeReviewIllegalRecordRowResponse];
  }

  private createConfiguredRule(
    fields: CodeReviewDemoRuleField[],
    fieldKey: string,
    operator: RuleOperator,
    value: string,
    resultKey: string,
  ) {
    const field = fields.find((item) => item.key === fieldKey);
    const rule = this.createResultRule(resultKey, field);
    const condition = rule.expression.children[0];
    if (condition?.type === 'condition') {
      condition.fieldKey = fieldKey;
      condition.operator = operator;
      condition.value = value;
    }
    return rule;
  }
}

export const codeReviewRuleConfigDemoSupport = new CodeReviewRuleConfigDemoSupport();
