import { describe, expect, it } from 'vitest';
import {
  AbstractRuleConfigSchemaSupport,
  RuleGroupOperator,
  RuleNodeType,
  RuleOperator,
  type RuleConfigField,
} from './rule-config-core';

class TestRuleSchemaSupport extends AbstractRuleConfigSchemaSupport<Record<string, unknown>> {
  buildFields() {
    return [
      {
        key: 'moduleName',
        label: '模块名称',
        type: 'text',
        operators: [RuleOperator.EQ, RuleOperator.CONTAINS, RuleOperator.IS_EMPTY],
      },
      {
        key: 'owner',
        label: '责任人',
        type: 'text',
        operators: [RuleOperator.EQ, RuleOperator.IS_EMPTY, RuleOperator.IS_NOT_EMPTY],
      },
    ] satisfies RuleConfigField[];
  }

  override createDefaultRules(fields: RuleConfigField[]) {
    return [
      this.createResultRule('缺少模块标签', fields[0]),
    ];
  }

  protected override readFieldValue(row: Record<string, unknown>, fieldKey: string) {
    return row[fieldKey];
  }
}

const schema = new TestRuleSchemaSupport();

describe('rule-config-core tree engine', () => {
  it('evaluates nested AND/OR expression trees against rows', () => {
    const fields = schema.buildFields();
    const rows = [
      { moduleName: '', owner: '张三' },
      { moduleName: '', owner: '' },
      { moduleName: '支付模块', owner: '' },
    ];
    const rule = {
      id: 'rule-1',
      resultKey: '目标结果',
      expression: {
        id: 'group-root',
        type: RuleNodeType.GROUP,
        operator: RuleGroupOperator.OR,
        children: [
          {
            id: 'group-a',
            type: RuleNodeType.GROUP,
            operator: RuleGroupOperator.AND,
            children: [
              {
                id: 'c1',
                type: RuleNodeType.CONDITION,
                fieldKey: 'moduleName',
                operator: RuleOperator.IS_EMPTY,
                value: '',
              },
              {
                id: 'c2',
                type: RuleNodeType.CONDITION,
                fieldKey: 'owner',
                operator: RuleOperator.IS_NOT_EMPTY,
                value: '',
              },
            ],
          },
          {
            id: 'c3',
            type: RuleNodeType.CONDITION,
            fieldKey: 'owner',
            operator: RuleOperator.IS_EMPTY,
            value: '',
          },
        ],
      },
    };

    const result = schema.evaluateRows(rows, [rule], fields);
    expect(result).toHaveLength(3);
    expect(schema.countMatches(rows, rule, fields)).toBe(3);
  });

  it('describes expression trees in readable text', () => {
    const fields = schema.buildFields();
    const rule = {
      id: 'rule-1',
      resultKey: '目标结果',
      expression: {
        id: 'group-root',
        type: RuleNodeType.GROUP,
        operator: RuleGroupOperator.AND,
        children: [
          {
            id: 'c1',
            type: RuleNodeType.CONDITION,
            fieldKey: 'moduleName',
            operator: RuleOperator.IS_EMPTY,
            value: '',
          },
          {
            id: 'c2',
            type: RuleNodeType.CONDITION,
            fieldKey: 'owner',
            operator: RuleOperator.IS_NOT_EMPTY,
            value: '',
          },
        ],
      },
    };

    const description = schema.describeRule(rule, fields);
    expect(description).toContain('模块名称为空');
    expect(description).toContain('并且');
    expect(description).toContain('责任人不为空');
    expect(description).toContain('目标结果');
  });
});
