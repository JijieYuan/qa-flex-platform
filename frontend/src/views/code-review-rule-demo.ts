import type { CodeReviewIllegalRecordRowResponse, OptionItemResponse } from '../api';

export type CodeReviewDemoRuleOperator =
  | 'eq'
  | 'ne'
  | 'contains'
  | 'notContains'
  | 'gt'
  | 'gte'
  | 'lt'
  | 'lte'
  | 'isEmpty'
  | 'isNotEmpty';

export type CodeReviewDemoRuleFieldType = 'text' | 'number' | 'select';

export interface CodeReviewDemoRuleField {
  key: string;
  label: string;
  type: CodeReviewDemoRuleFieldType;
  operators: CodeReviewDemoRuleOperator[];
  options?: OptionItemResponse[];
}

export interface CodeReviewDemoRule {
  id: string;
  fieldKey: string;
  operator: CodeReviewDemoRuleOperator;
  value: string;
  illegalType: string;
}

const TEXT_OPERATORS: CodeReviewDemoRuleOperator[] = ['eq', 'ne', 'contains', 'notContains', 'isEmpty', 'isNotEmpty'];
const NUMBER_OPERATORS: CodeReviewDemoRuleOperator[] = ['eq', 'gt', 'gte', 'lt', 'lte', 'isEmpty', 'isNotEmpty'];
const SELECT_OPERATORS: CodeReviewDemoRuleOperator[] = ['eq', 'ne', 'contains', 'isEmpty', 'isNotEmpty'];

const FALLBACK_ILLEGAL_TYPES = [
  '\u7f3a\u5c11\u6a21\u5757\u6807\u7b7e',
  '\u7f3a\u5c11\u6807\u6ce8\u8d23\u4efb\u4eba',
  '\u7f3a\u5c11\u4ee3\u7801\u6ce8\u91ca\u6bd4\u4f8b',
  '\u7f3a\u5c11\u7f3a\u9677\u6570\u91cf',
  '\u7f3a\u5c11\u65b0\u589e\u4ee3\u7801\u884c\u6570',
];

let ruleSeed = 0;

export function buildCodeReviewDemoRuleFields(optionGroups: {
  repositoryNames: OptionItemResponse[];
  illegalTypes: OptionItemResponse[];
  targetBranches: OptionItemResponse[];
  mergedBys: OptionItemResponse[];
  moduleNames: OptionItemResponse[];
  projectNames: OptionItemResponse[];
}) {
  return [
    { key: 'repositoryName', label: '\u4ee3\u7801\u5e93', type: 'select', operators: SELECT_OPERATORS, options: optionGroups.repositoryNames },
    { key: 'projectName', label: '\u9879\u76ee\u540d\u79f0', type: 'select', operators: SELECT_OPERATORS, options: optionGroups.projectNames },
    { key: 'owner', label: '\u6807\u6ce8\u8d23\u4efb\u4eba', type: 'text', operators: TEXT_OPERATORS },
    { key: 'mergedBy', label: '\u5408\u5e76\u4eba', type: 'select', operators: SELECT_OPERATORS, options: optionGroups.mergedBys },
    { key: 'moduleName', label: '\u6a21\u5757\u540d\u79f0', type: 'select', operators: SELECT_OPERATORS, options: optionGroups.moduleNames },
    { key: 'targetBranch', label: '\u76ee\u6807\u5206\u652f', type: 'select', operators: SELECT_OPERATORS, options: optionGroups.targetBranches },
    { key: 'illegalTypes', label: '\u975e\u6cd5\u7c7b\u578b', type: 'select', operators: SELECT_OPERATORS, options: optionGroups.illegalTypes },
    { key: 'mergeRequestContent', label: '\u5408\u5e76\u8bf7\u6c42\u5185\u5bb9', type: 'text', operators: TEXT_OPERATORS },
    { key: 'commentRate', label: '\u4ee3\u7801\u6ce8\u91ca\u6bd4\u4f8b', type: 'number', operators: NUMBER_OPERATORS },
    { key: 'defectCount', label: '\u7f3a\u9677\u6570\u91cf', type: 'number', operators: NUMBER_OPERATORS },
    { key: 'addedLines', label: '\u65b0\u589e\u4ee3\u7801\u884c\u6570', type: 'number', operators: NUMBER_OPERATORS },
  ] satisfies CodeReviewDemoRuleField[];
}

export function buildCodeReviewDemoIllegalTypeOptions(options: OptionItemResponse[]) {
  if (options.length) {
    return options;
  }
  return FALLBACK_ILLEGAL_TYPES.map((item) => ({ label: item, value: item }));
}

export function createCodeReviewDemoRule(field?: CodeReviewDemoRuleField, illegalType?: string): CodeReviewDemoRule {
  ruleSeed += 1;
  return {
    id: `code-review-rule-${ruleSeed}`,
    fieldKey: field?.key ?? '',
    operator: field?.operators[0] ?? 'eq',
    value: '',
    illegalType: illegalType ?? '',
  };
}

export function createDefaultCodeReviewDemoRules(fields: CodeReviewDemoRuleField[]) {
  return [
    createConfiguredRule(fields, 'moduleName', 'isEmpty', '', '\u7f3a\u5c11\u6a21\u5757\u6807\u7b7e'),
    createConfiguredRule(fields, 'owner', 'isEmpty', '', '\u7f3a\u5c11\u6807\u6ce8\u8d23\u4efb\u4eba'),
    createConfiguredRule(fields, 'commentRate', 'isEmpty', '', '\u7f3a\u5c11\u4ee3\u7801\u6ce8\u91ca\u6bd4\u4f8b'),
    createConfiguredRule(fields, 'defectCount', 'isEmpty', '', '\u7f3a\u5c11\u7f3a\u9677\u6570\u91cf'),
    createConfiguredRule(fields, 'addedLines', 'isEmpty', '', '\u7f3a\u5c11\u65b0\u589e\u4ee3\u7801\u884c\u6570'),
  ];
}

export function codeReviewDemoOperatorLabel(operator: CodeReviewDemoRuleOperator) {
  return (
    {
      eq: '\u7b49\u4e8e',
      ne: '\u4e0d\u7b49\u4e8e',
      contains: '\u5305\u542b',
      notContains: '\u4e0d\u5305\u542b',
      gt: '\u5927\u4e8e',
      gte: '\u5927\u4e8e\u7b49\u4e8e',
      lt: '\u5c0f\u4e8e',
      lte: '\u5c0f\u4e8e\u7b49\u4e8e',
      isEmpty: '\u4e3a\u7a7a',
      isNotEmpty: '\u4e0d\u4e3a\u7a7a',
    } as Record<CodeReviewDemoRuleOperator, string>
  )[operator];
}

export function usesValueInput(operator: CodeReviewDemoRuleOperator) {
  return operator !== 'isEmpty' && operator !== 'isNotEmpty';
}

export function matchesCodeReviewDemoRule(
  row: CodeReviewIllegalRecordRowResponse,
  rule: CodeReviewDemoRule,
  fields: CodeReviewDemoRuleField[],
) {
  const fieldMap = new Map(fields.map((field) => [field.key, field]));
  const field = fieldMap.get(rule.fieldKey);
  if (!field) {
    return false;
  }
  if (!field.operators.includes(rule.operator)) {
    return false;
  }
  if (usesValueInput(rule.operator) && rule.value.trim() === '') {
    return false;
  }
  return matchesCondition(row, rule, field);
}

export function evaluateCodeReviewDemoRules(
  rows: CodeReviewIllegalRecordRowResponse[],
  rules: CodeReviewDemoRule[],
  fields: CodeReviewDemoRuleField[],
) {
  const validRules = rules.filter((rule) => isRuleReady(rule, fields));
  if (!validRules.length) {
    return rows;
  }
  return rows.filter((row) => validRules.some((rule) => matchesCodeReviewDemoRule(row, rule, fields)));
}

export function countCodeReviewDemoRuleMatches(
  rows: CodeReviewIllegalRecordRowResponse[],
  rule: CodeReviewDemoRule,
  fields: CodeReviewDemoRuleField[],
) {
  if (!isRuleReady(rule, fields)) {
    return 0;
  }
  return rows.filter((row) => matchesCodeReviewDemoRule(row, rule, fields)).length;
}

export function describeCodeReviewDemoRule(rule: CodeReviewDemoRule, fields: CodeReviewDemoRuleField[]) {
  const field = fields.find((item) => item.key === rule.fieldKey);
  const fieldLabel = field?.label ?? '\u5b57\u6bb5';
  const operatorLabel = codeReviewDemoOperatorLabel(rule.operator);
  if (!usesValueInput(rule.operator)) {
    return `\u5982\u679c ${fieldLabel}${operatorLabel}\uff0c\u5c31\u4f1a\u88ab\u5224\u5b9a\u4e3a ${rule.illegalType || '\u672a\u9009\u62e9\u975e\u6cd5\u7c7b\u578b'}\u3002`;
  }
  return `\u5982\u679c ${fieldLabel}${operatorLabel}${rule.value || '\u672a\u586b\u5199\u53d6\u503c'}\uff0c\u5c31\u4f1a\u88ab\u5224\u5b9a\u4e3a ${rule.illegalType || '\u672a\u9009\u62e9\u975e\u6cd5\u7c7b\u578b'}\u3002`;
}

function createConfiguredRule(
  fields: CodeReviewDemoRuleField[],
  fieldKey: string,
  operator: CodeReviewDemoRuleOperator,
  value: string,
  illegalType: string,
) {
  const field = fields.find((item) => item.key === fieldKey);
  const rule = createCodeReviewDemoRule(field, illegalType);
  rule.fieldKey = fieldKey;
  rule.operator = operator;
  rule.value = value;
  rule.illegalType = illegalType;
  return rule;
}

function isRuleReady(rule: CodeReviewDemoRule, fields: CodeReviewDemoRuleField[]) {
  const field = fields.find((item) => item.key === rule.fieldKey);
  if (!field) {
    return false;
  }
  if (!rule.illegalType.trim()) {
    return false;
  }
  if (!field.operators.includes(rule.operator)) {
    return false;
  }
  return usesValueInput(rule.operator) ? rule.value.trim() !== '' : true;
}

function matchesCondition(
  row: CodeReviewIllegalRecordRowResponse,
  rule: CodeReviewDemoRule,
  field: CodeReviewDemoRuleField,
) {
  const fieldValue = readFieldValue(row, rule.fieldKey);

  if (rule.operator === 'isEmpty') {
    return isEmptyValue(fieldValue);
  }
  if (rule.operator === 'isNotEmpty') {
    return !isEmptyValue(fieldValue);
  }

  if (field.type === 'number') {
    const leftValue = typeof fieldValue === 'number' ? fieldValue : null;
    const rightValue = Number.parseFloat(rule.value);
    if (leftValue == null || Number.isNaN(rightValue)) {
      return false;
    }
    return compareNumber(leftValue, rightValue, rule.operator);
  }

  const values = normalizeTextList(fieldValue);
  if (!values.length) {
    return false;
  }
  return compareText(values, rule.value.trim(), rule.operator);
}

function readFieldValue(row: CodeReviewIllegalRecordRowResponse, fieldKey: string) {
  if (fieldKey === 'illegalTypes') {
    return row.illegalTypes;
  }
  return row[fieldKey as keyof CodeReviewIllegalRecordRowResponse];
}

function isEmptyValue(value: unknown) {
  if (value == null) {
    return true;
  }
  if (Array.isArray(value)) {
    return value.length === 0;
  }
  if (typeof value === 'string') {
    return value.trim() === '';
  }
  return false;
}

function normalizeTextList(value: unknown) {
  if (Array.isArray(value)) {
    return value.map((item) => String(item ?? '').trim()).filter(Boolean);
  }
  if (value == null) {
    return [];
  }
  const normalized = String(value).trim();
  return normalized ? [normalized] : [];
}

function compareText(values: string[], expected: string, operator: CodeReviewDemoRuleOperator) {
  const target = expected.trim().toLowerCase();
  const normalizedValues = values.map((value) => value.toLowerCase());
  return (
    {
      eq: normalizedValues.some((value) => value === target),
      ne: normalizedValues.every((value) => value !== target),
      contains: normalizedValues.some((value) => value.includes(target)),
      notContains: normalizedValues.every((value) => !value.includes(target)),
      gt: false,
      gte: false,
      lt: false,
      lte: false,
      isEmpty: false,
      isNotEmpty: false,
    } as Record<CodeReviewDemoRuleOperator, boolean>
  )[operator];
}

function compareNumber(left: number, right: number, operator: CodeReviewDemoRuleOperator) {
  return (
    {
      eq: left === right,
      ne: left !== right,
      gt: left > right,
      gte: left >= right,
      lt: left < right,
      lte: left <= right,
      contains: false,
      notContains: false,
      isEmpty: false,
      isNotEmpty: false,
    } as Record<CodeReviewDemoRuleOperator, boolean>
  )[operator];
}
