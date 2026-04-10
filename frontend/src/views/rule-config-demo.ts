import type { OptionItemResponse } from '../api';

export type RuleConfigDemoOperator =
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

export type RuleConfigDemoFieldType = 'text' | 'number' | 'select';

export interface RuleConfigDemoField {
  key: string;
  label: string;
  type: RuleConfigDemoFieldType;
  operators: RuleConfigDemoOperator[];
  options?: OptionItemResponse[];
}

export interface RuleConfigDemoRule {
  id: string;
  fieldKey: string;
  operator: RuleConfigDemoOperator;
  value: string;
  illegalType: string;
}

const TEXT_OPERATORS: RuleConfigDemoOperator[] = ['eq', 'ne', 'contains', 'notContains', 'isEmpty', 'isNotEmpty'];
const NUMBER_OPERATORS: RuleConfigDemoOperator[] = ['eq', 'gt', 'gte', 'lt', 'lte', 'isEmpty', 'isNotEmpty'];
const SELECT_OPERATORS: RuleConfigDemoOperator[] = ['eq', 'ne', 'contains', 'isEmpty', 'isNotEmpty'];

let ruleSeed = 0;

export abstract class AbstractRuleConfigDemoSupport<TRow> {
  protected textOperators() {
    return TEXT_OPERATORS;
  }

  protected numberOperators() {
    return NUMBER_OPERATORS;
  }

  protected selectOperators() {
    return SELECT_OPERATORS;
  }

  createRule(field?: RuleConfigDemoField, illegalType?: string): RuleConfigDemoRule {
    ruleSeed += 1;
    return {
      id: `rule-config-demo-${ruleSeed}`,
      fieldKey: field?.key ?? '',
      operator: field?.operators[0] ?? 'eq',
      value: '',
      illegalType: illegalType ?? '',
    };
  }

  operatorLabel(operator: RuleConfigDemoOperator) {
    return (
      {
        eq: '等于',
        ne: '不等于',
        contains: '包含',
        notContains: '不包含',
        gt: '大于',
        gte: '大于等于',
        lt: '小于',
        lte: '小于等于',
        isEmpty: '为空',
        isNotEmpty: '不为空',
      } as Record<RuleConfigDemoOperator, string>
    )[operator];
  }

  usesValueInput(operator: RuleConfigDemoOperator) {
    return operator !== 'isEmpty' && operator !== 'isNotEmpty';
  }

  evaluateRows(rows: TRow[], rules: RuleConfigDemoRule[], fields: RuleConfigDemoField[]) {
    const validRules = rules.filter((rule) => this.isRuleReady(rule, fields));
    if (!validRules.length) {
      return rows;
    }
    return rows.filter((row) => validRules.some((rule) => this.matchesRule(row, rule, fields)));
  }

  countMatches(rows: TRow[], rule: RuleConfigDemoRule, fields: RuleConfigDemoField[]) {
    if (!this.isRuleReady(rule, fields)) {
      return 0;
    }
    return rows.filter((row) => this.matchesRule(row, rule, fields)).length;
  }

  describeRule(rule: RuleConfigDemoRule, fields: RuleConfigDemoField[]) {
    const field = fields.find((item) => item.key === rule.fieldKey);
    const fieldLabel = field?.label ?? '字段';
    const operatorLabel = this.operatorLabel(rule.operator);
    if (!this.usesValueInput(rule.operator)) {
      return `如果 ${fieldLabel}${operatorLabel}，就会被判定为 ${rule.illegalType || '未选择非法类型'}。`;
    }
    return `如果 ${fieldLabel}${operatorLabel}${rule.value || '未填写取值'}，就会被判定为 ${rule.illegalType || '未选择非法类型'}。`;
  }

  syncRuleWithField(rule: RuleConfigDemoRule, fields: RuleConfigDemoField[]) {
    const field = fields.find((item) => item.key === rule.fieldKey);
    if (!field) {
      rule.operator = 'eq';
      rule.value = '';
      return;
    }
    if (!field.operators.includes(rule.operator)) {
      rule.operator = field.operators[0] ?? 'eq';
    }
    if (!this.usesValueInput(rule.operator)) {
      rule.value = '';
      return;
    }
    if (field.type === 'select' && field.options?.length) {
      if (!field.options.some((item) => item.value === rule.value)) {
        rule.value = '';
      }
    }
  }

  matchesRule(row: TRow, rule: RuleConfigDemoRule, fields: RuleConfigDemoField[]) {
    const field = fields.find((item) => item.key === rule.fieldKey);
    if (!field) {
      return false;
    }
    if (!field.operators.includes(rule.operator)) {
      return false;
    }
    if (this.usesValueInput(rule.operator) && rule.value.trim() === '') {
      return false;
    }

    const fieldValue = this.readFieldValue(row, rule.fieldKey);

    if (rule.operator === 'isEmpty') {
      return this.isEmptyValue(fieldValue);
    }
    if (rule.operator === 'isNotEmpty') {
      return !this.isEmptyValue(fieldValue);
    }

    if (field.type === 'number') {
      const leftValue = typeof fieldValue === 'number' ? fieldValue : null;
      const rightValue = Number.parseFloat(rule.value);
      if (leftValue == null || Number.isNaN(rightValue)) {
        return false;
      }
      return this.compareNumber(leftValue, rightValue, rule.operator);
    }

    const values = this.normalizeTextList(fieldValue);
    if (!values.length) {
      return false;
    }
    return this.compareText(values, rule.value.trim(), rule.operator);
  }

  protected abstract readFieldValue(row: TRow, fieldKey: string): unknown;

  protected isRuleReady(rule: RuleConfigDemoRule, fields: RuleConfigDemoField[]) {
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
    return this.usesValueInput(rule.operator) ? rule.value.trim() !== '' : true;
  }

  protected isEmptyValue(value: unknown) {
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

  protected normalizeTextList(value: unknown) {
    if (Array.isArray(value)) {
      return value.map((item) => String(item ?? '').trim()).filter(Boolean);
    }
    if (value == null) {
      return [];
    }
    const normalized = String(value).trim();
    return normalized ? [normalized] : [];
  }

  protected compareText(values: string[], expected: string, operator: RuleConfigDemoOperator) {
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
      } as Record<RuleConfigDemoOperator, boolean>
    )[operator];
  }

  protected compareNumber(left: number, right: number, operator: RuleConfigDemoOperator) {
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
      } as Record<RuleConfigDemoOperator, boolean>
    )[operator];
  }
}
