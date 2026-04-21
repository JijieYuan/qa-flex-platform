import type {
  CodeReviewRuleCondition,
  CodeReviewRuleConfig,
  CodeReviewRuleFieldDefinition,
  CodeReviewRuleGroup,
  CodeReviewRuleOperator,
} from '../types/code-review-rule-config';

let groupSeed = 0;
let conditionSeed = 0;

function nextGroupId() {
  groupSeed += 1;
  return `code-review-rule-group-${groupSeed}`;
}

function nextConditionId() {
  conditionSeed += 1;
  return `code-review-rule-condition-${conditionSeed}`;
}

export function cloneCodeReviewRuleConfig(config: CodeReviewRuleConfig): CodeReviewRuleConfig {
  if (typeof globalThis.structuredClone === 'function') {
    try {
      return globalThis.structuredClone(config) as CodeReviewRuleConfig;
    } catch {
      // Ignore and fall back to JSON clone for proxied reactive objects.
    }
  }
  return JSON.parse(JSON.stringify(config)) as CodeReviewRuleConfig;
}

export function createCodeReviewRuleCondition(field?: CodeReviewRuleFieldDefinition): CodeReviewRuleCondition {
  return {
    id: nextConditionId(),
    fieldKey: field?.key ?? '',
    operator: field?.operators[0] ?? 'contains',
    value: '',
  };
}

export function createCodeReviewRuleGroup(field?: CodeReviewRuleFieldDefinition): CodeReviewRuleGroup {
  return {
    id: nextGroupId(),
    matchMode: 'all',
    conditions: [createCodeReviewRuleCondition(field)],
  };
}

export function createDefaultCodeReviewRuleConfig(field?: CodeReviewRuleFieldDefinition): CodeReviewRuleConfig {
  return {
    enabled: false,
    groups: [createCodeReviewRuleGroup(field)],
    updatedAt: null,
  };
}

export function usesConditionValue(operator: CodeReviewRuleOperator) {
  return operator !== 'isEmpty' && operator !== 'isNotEmpty';
}

export function normalizeCodeReviewRuleConfig(
  config: CodeReviewRuleConfig,
  fields: CodeReviewRuleFieldDefinition[],
): CodeReviewRuleConfig {
  const normalized = cloneCodeReviewRuleConfig(config);
  normalized.groups = normalized.groups.map((group) => ({
    ...group,
    conditions: group.conditions.map((condition) => normalizeCondition(condition, fields)),
  }));
  return normalized;
}

function normalizeCondition(
  condition: CodeReviewRuleCondition,
  fields: CodeReviewRuleFieldDefinition[],
): CodeReviewRuleCondition {
  const field = fields.find((item) => item.key === condition.fieldKey);
  if (!field) {
    const fallbackField = fields[0];
    return {
      ...condition,
      fieldKey: fallbackField?.key ?? '',
      operator: fallbackField?.operators[0] ?? 'contains',
      value: '',
    };
  }
  const operator = field.operators.includes(condition.operator) ? condition.operator : field.operators[0];
  if (!usesConditionValue(operator)) {
    return {
      ...condition,
      operator,
      value: '',
    };
  }
  if (field.type === 'select' && field.options?.length && !field.options.some((item) => item.value === condition.value)) {
    return {
      ...condition,
      operator,
      value: '',
    };
  }
  if (field.type === 'multi-select') {
    const allowedValues = new Set(field.options?.map((item) => item.value) ?? []);
    const values = String(condition.value ?? '')
      .split(',')
      .map((item) => item.trim())
      .filter(Boolean)
      .filter((item) => !allowedValues.size || allowedValues.has(item));
    return {
      ...condition,
      operator,
      value: values.join(','),
    };
  }
  return {
    ...condition,
    operator,
    value: String(condition.value ?? ''),
  };
}

export function isReadyCodeReviewRuleCondition(
  condition: CodeReviewRuleCondition,
  fields: CodeReviewRuleFieldDefinition[],
) {
  const field = fields.find((item) => item.key === condition.fieldKey);
  if (!field) {
    return false;
  }
  if (!field.operators.includes(condition.operator)) {
    return false;
  }
  return usesConditionValue(condition.operator) ? condition.value.trim().length > 0 : true;
}

export function hasReadyCodeReviewRuleConfig(
  config: CodeReviewRuleConfig | null | undefined,
  fields: CodeReviewRuleFieldDefinition[],
) {
  if (!config?.enabled) {
    return false;
  }
  return config.groups.some((group) => group.conditions.some((condition) => isReadyCodeReviewRuleCondition(condition, fields)));
}

export function describeCodeReviewRuleCondition(
  condition: CodeReviewRuleCondition,
  fields: CodeReviewRuleFieldDefinition[],
) {
  const field = fields.find((item) => item.key === condition.fieldKey);
  const fieldLabel = field?.label || '字段';
  const operatorLabel = operatorText(condition.operator);
  if (!usesConditionValue(condition.operator)) {
    return `${fieldLabel}${operatorLabel}`;
  }
  const displayValue = formatConditionValue(condition, field);
  return `${fieldLabel}${operatorLabel}${displayValue || '未填写'}`;
}

function formatConditionValue(
  condition: CodeReviewRuleCondition,
  field: CodeReviewRuleFieldDefinition | undefined,
) {
  if (field?.type === 'select') {
    return field.options?.find((item) => item.value === condition.value)?.label || condition.value;
  }
  if (field?.type === 'multi-select') {
    const labels = String(condition.value ?? '')
      .split(',')
      .map((item) => item.trim())
      .filter(Boolean)
      .map((value) => field.options?.find((item) => item.value === value)?.label || value);
    return labels.join('、');
  }
  return condition.value;
}

function operatorText(operator: CodeReviewRuleOperator) {
  const labels: Record<CodeReviewRuleOperator, string> = {
    eq: '等于',
    ne: '不等于',
    contains: '包含',
    notContains: '不包含',
    notIn: '不在允许范围：',
    gt: '大于',
    gte: '大于等于',
    lt: '小于',
    lte: '小于等于',
    isEmpty: '为空',
    isNotEmpty: '不为空',
  };
  return labels[operator];
}

export function describeCodeReviewRuleGroup(
  group: CodeReviewRuleGroup,
  fields: CodeReviewRuleFieldDefinition[],
) {
  const readyConditions = group.conditions.filter((condition) => isReadyCodeReviewRuleCondition(condition, fields));
  if (!readyConditions.length) {
    return '当前这组条件还没有填写完整。';
  }
  const joiner = group.matchMode === 'all' ? '，并且' : '，或者';
  return readyConditions.map((condition) => describeCodeReviewRuleCondition(condition, fields)).join(joiner);
}
