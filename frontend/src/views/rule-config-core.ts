import type { OptionItemResponse } from '../api';

export enum RuleOperator {
  EQ = 'eq',
  NE = 'ne',
  CONTAINS = 'contains',
  NOT_CONTAINS = 'notContains',
  GT = 'gt',
  GTE = 'gte',
  LT = 'lt',
  LTE = 'lte',
  IS_EMPTY = 'isEmpty',
  IS_NOT_EMPTY = 'isNotEmpty',
}

export enum RuleNodeType {
  CONDITION = 'condition',
  GROUP = 'group',
}

export enum RuleGroupOperator {
  AND = 'AND',
  OR = 'OR',
}

export type RuleConfigFieldType = 'text' | 'number' | 'select';

export interface RuleConfigField {
  key: string;
  label: string;
  type: RuleConfigFieldType;
  operators: RuleOperator[];
  options?: OptionItemResponse[];
}

export interface RuleConditionNode {
  id: string;
  type: RuleNodeType.CONDITION;
  fieldKey: string;
  operator: RuleOperator;
  value: string;
}

export interface RuleGroupNode {
  id: string;
  type: RuleNodeType.GROUP;
  operator: RuleGroupOperator;
  children: RuleExpressionNode[];
}

export type RuleExpressionNode = RuleConditionNode | RuleGroupNode;

export interface RuleConfigResultRule {
  id: string;
  resultKey: string;
  expression: RuleGroupNode;
}

export interface RuleConfigState<TRule extends RuleConfigResultRule> {
  enabled: boolean;
  rules: TRule[];
  version: string | null;
  dirty: boolean;
}

const TEXT_OPERATORS = [
  RuleOperator.EQ,
  RuleOperator.NE,
  RuleOperator.CONTAINS,
  RuleOperator.NOT_CONTAINS,
  RuleOperator.IS_EMPTY,
  RuleOperator.IS_NOT_EMPTY,
] satisfies RuleOperator[];

const NUMBER_OPERATORS = [
  RuleOperator.EQ,
  RuleOperator.GT,
  RuleOperator.GTE,
  RuleOperator.LT,
  RuleOperator.LTE,
  RuleOperator.IS_EMPTY,
  RuleOperator.IS_NOT_EMPTY,
] satisfies RuleOperator[];

const SELECT_OPERATORS = [
  RuleOperator.EQ,
  RuleOperator.NE,
  RuleOperator.CONTAINS,
  RuleOperator.IS_EMPTY,
  RuleOperator.IS_NOT_EMPTY,
] satisfies RuleOperator[];

let ruleSeed = 0;
let nodeSeed = 0;

export function cloneRuleConfigRules<TRule extends RuleConfigResultRule>(rules: TRule[]): TRule[] {
  if (typeof globalThis.structuredClone === 'function') {
    try {
      return globalThis.structuredClone(rules) as TRule[];
    } catch {
      // Vue 响应式代理在部分运行时里不能直接 structuredClone，这里回退到 JSON 深拷贝。
    }
  }
  return JSON.parse(JSON.stringify(rules)) as TRule[];
}

function nextRuleId() {
  ruleSeed += 1;
  return `rule-config-${ruleSeed}`;
}

function nextNodeId() {
  nodeSeed += 1;
  return `rule-node-${nodeSeed}`;
}

export abstract class AbstractRuleConfigSchemaSupport<
  TRow,
  TField extends RuleConfigField = RuleConfigField,
  TRule extends RuleConfigResultRule = RuleConfigResultRule,
> {
  protected textOperators() {
    return TEXT_OPERATORS;
  }

  protected numberOperators() {
    return NUMBER_OPERATORS;
  }

  protected selectOperators() {
    return SELECT_OPERATORS;
  }

  cloneRules(rules: TRule[]) {
    return cloneRuleConfigRules(rules);
  }

  createConditionNode(field?: TField): RuleConditionNode {
    return {
      id: nextNodeId(),
      type: RuleNodeType.CONDITION,
      fieldKey: field?.key ?? '',
      operator: field?.operators[0] ?? RuleOperator.EQ,
      value: '',
    };
  }

  createGroupNode(
    operator: RuleGroupOperator = RuleGroupOperator.AND,
    children: RuleExpressionNode[] = [],
  ): RuleGroupNode {
    return {
      id: nextNodeId(),
      type: RuleNodeType.GROUP,
      operator,
      children,
    };
  }

  createResultRule(resultKey = '', field?: TField): TRule {
    return {
      id: nextRuleId(),
      resultKey,
      expression: this.createGroupNode(RuleGroupOperator.AND, [this.createConditionNode(field)]),
    } as TRule;
  }

  createDefaultState(fields: TField[]): RuleConfigState<TRule> {
    return {
      enabled: false,
      rules: this.cloneRules(this.createDefaultRules(fields)),
      version: null,
      dirty: false,
    };
  }

  operatorLabel(operator: RuleOperator) {
    return (
      {
        [RuleOperator.EQ]: '等于',
        [RuleOperator.NE]: '不等于',
        [RuleOperator.CONTAINS]: '包含',
        [RuleOperator.NOT_CONTAINS]: '不包含',
        [RuleOperator.GT]: '大于',
        [RuleOperator.GTE]: '大于等于',
        [RuleOperator.LT]: '小于',
        [RuleOperator.LTE]: '小于等于',
        [RuleOperator.IS_EMPTY]: '为空',
        [RuleOperator.IS_NOT_EMPTY]: '不为空',
      } as Record<RuleOperator, string>
    )[operator];
  }

  groupOperatorLabel(operator: RuleGroupOperator) {
    return operator === RuleGroupOperator.AND ? '并且' : '或者';
  }

  usesValueInput(operator: RuleOperator) {
    return operator !== RuleOperator.IS_EMPTY && operator !== RuleOperator.IS_NOT_EMPTY;
  }

  evaluateRows(rows: TRow[], rules: TRule[], fields: TField[]) {
    const validRules = rules.filter((rule) => this.isRuleReady(rule, fields));
    if (!validRules.length) {
      return rows;
    }
    return rows.filter((row) => validRules.some((rule) => this.matchesRule(row, rule, fields)));
  }

  countMatches(rows: TRow[], rule: TRule, fields: TField[]) {
    if (!this.isRuleReady(rule, fields)) {
      return 0;
    }
    return rows.filter((row) => this.matchesRule(row, rule, fields)).length;
  }

  describeRule(rule: TRule, fields: TField[]) {
    if (!rule.expression) {
      return `如果未配置有效规则，就会被判定为${rule.resultKey || '未选择判定结果'}。`;
    }
    const expressionText = this.describeNode(rule.expression, fields);
    return `如果 ${expressionText}，就会被判定为${rule.resultKey || '未选择判定结果'}。`;
  }

  syncRuleWithField(rule: TRule, fields: TField[]) {
    if (!rule.expression) {
      return;
    }
    this.syncNode(rule.expression, fields);
  }

  syncExpressionNode(node: RuleExpressionNode, fields: TField[]) {
    this.syncNode(node, fields);
  }

  matchesRule(row: TRow, rule: TRule, fields: TField[]) {
    if (!rule.expression) {
      return false;
    }
    return this.evaluateNode(row, rule.expression, fields);
  }

  abstract createDefaultRules(fields: TField[]): TRule[];

  protected abstract readFieldValue(row: TRow, fieldKey: string): unknown;

  protected isRuleReady(rule: TRule, fields: TField[]) {
    if (!rule.resultKey?.trim()) {
      return false;
    }
    if (!rule.expression) {
      return false;
    }
    return this.isNodeReady(rule.expression, fields);
  }

  protected describeNode(node: RuleExpressionNode, fields: TField[]): string {
    if (node.type === RuleNodeType.CONDITION) {
      const field = fields.find((item) => item.key === node.fieldKey);
      const fieldLabel = field?.label ?? '字段';
      const operatorLabel = this.operatorLabel(node.operator);
      if (!this.usesValueInput(node.operator)) {
        return `${fieldLabel}${operatorLabel}`;
      }
      return `${fieldLabel}${operatorLabel}${node.value || '未填写取值'}`;
    }
    const childDescriptions = node.children
      .map((child) => this.describeNode(child, fields))
      .filter((item) => item.trim().length > 0);
    if (!childDescriptions.length) {
      return '未配置条件';
    }
    if (childDescriptions.length === 1) {
      return childDescriptions[0];
    }
    const joiner = ` ${this.groupOperatorLabel(node.operator)} `;
    return `(${childDescriptions.join(joiner)})`;
  }

  protected syncNode(node: RuleExpressionNode, fields: TField[]) {
    if (!node) {
      return;
    }
    if (node.type === RuleNodeType.CONDITION) {
      const field = fields.find((item) => item.key === node.fieldKey);
      if (!field) {
        node.operator = RuleOperator.EQ;
        node.value = '';
        return;
      }
      if (!field.operators.includes(node.operator)) {
        node.operator = field.operators[0] ?? RuleOperator.EQ;
      }
      if (!this.usesValueInput(node.operator)) {
        node.value = '';
        return;
      }
      if (field.type === 'select' && field.options?.length) {
        if (!field.options.some((item) => item.value === node.value)) {
          node.value = '';
        }
      }
      return;
    }
    node.children.forEach((child) => this.syncNode(child, fields));
  }

  protected isNodeReady(node: RuleExpressionNode, fields: TField[]): boolean {
    if (!node) {
      return false;
    }
    if (node.type === RuleNodeType.CONDITION) {
      const field = fields.find((item) => item.key === node.fieldKey);
      if (!field) {
        return false;
      }
      if (!field.operators.includes(node.operator)) {
        return false;
      }
      return this.usesValueInput(node.operator) ? node.value.trim() !== '' : true;
    }
    const readyChildren = node.children.filter((child) => this.isNodeReady(child, fields));
    return readyChildren.length > 0;
  }

  protected evaluateNode(row: TRow, node: RuleExpressionNode, fields: TField[]): boolean {
    if (!node) {
      return false;
    }
    if (node.type === RuleNodeType.CONDITION) {
      return this.matchesCondition(row, node, fields);
    }
    const readyChildren = node.children.filter((child) => this.isNodeReady(child, fields));
    if (!readyChildren.length) {
      return false;
    }
    if (node.operator === RuleGroupOperator.AND) {
      return readyChildren.every((child) => this.evaluateNode(row, child, fields));
    }
    return readyChildren.some((child) => this.evaluateNode(row, child, fields));
  }

  protected matchesCondition(row: TRow, node: RuleConditionNode, fields: TField[]): boolean {
    const field = fields.find((item) => item.key === node.fieldKey);
    if (!field) {
      return false;
    }
    if (!field.operators.includes(node.operator)) {
      return false;
    }
    if (this.usesValueInput(node.operator) && node.value.trim() === '') {
      return false;
    }

    const fieldValue = this.readFieldValue(row, node.fieldKey);

    if (node.operator === RuleOperator.IS_EMPTY) {
      return this.isEmptyValue(fieldValue);
    }
    if (node.operator === RuleOperator.IS_NOT_EMPTY) {
      return !this.isEmptyValue(fieldValue);
    }

    if (field.type === 'number') {
      const leftValue = typeof fieldValue === 'number' ? fieldValue : null;
      const rightValue = Number.parseFloat(node.value);
      if (leftValue == null || Number.isNaN(rightValue)) {
        return false;
      }
      return this.compareNumber(leftValue, rightValue, node.operator);
    }

    const values = this.normalizeTextList(fieldValue);
    if (!values.length) {
      return false;
    }
    return this.compareText(values, node.value.trim(), node.operator);
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

  protected compareText(values: string[], expected: string, operator: RuleOperator) {
    const target = expected.trim().toLowerCase();
    const normalizedValues = values.map((value) => value.toLowerCase());
    return (
      {
        [RuleOperator.EQ]: normalizedValues.some((value) => value === target),
        [RuleOperator.NE]: normalizedValues.every((value) => value !== target),
        [RuleOperator.CONTAINS]: normalizedValues.some((value) => value.includes(target)),
        [RuleOperator.NOT_CONTAINS]: normalizedValues.every((value) => !value.includes(target)),
        [RuleOperator.GT]: false,
        [RuleOperator.GTE]: false,
        [RuleOperator.LT]: false,
        [RuleOperator.LTE]: false,
        [RuleOperator.IS_EMPTY]: false,
        [RuleOperator.IS_NOT_EMPTY]: false,
      } as Record<RuleOperator, boolean>
    )[operator];
  }

  protected compareNumber(left: number, right: number, operator: RuleOperator) {
    return (
      {
        [RuleOperator.EQ]: left === right,
        [RuleOperator.NE]: left !== right,
        [RuleOperator.GT]: left > right,
        [RuleOperator.GTE]: left >= right,
        [RuleOperator.LT]: left < right,
        [RuleOperator.LTE]: left <= right,
        [RuleOperator.CONTAINS]: false,
        [RuleOperator.NOT_CONTAINS]: false,
        [RuleOperator.IS_EMPTY]: false,
        [RuleOperator.IS_NOT_EMPTY]: false,
      } as Record<RuleOperator, boolean>
    )[operator];
  }
}
