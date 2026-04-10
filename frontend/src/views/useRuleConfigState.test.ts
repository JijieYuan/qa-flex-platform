import { computed, nextTick, ref } from 'vue';
import { beforeEach, describe, expect, it } from 'vitest';
import {
  AbstractRuleConfigSchemaSupport,
  RuleGroupOperator,
  RuleNodeType,
  RuleOperator,
  type RuleConfigField,
  type RuleConfigResultRule,
} from './rule-config-core';
import { useRuleConfigState } from './useRuleConfigState';

class TestRuleSchemaSupport extends AbstractRuleConfigSchemaSupport<Record<string, unknown>> {
  buildFields() {
    return [
      {
        key: 'moduleName',
        label: '模块名称',
        type: 'text',
        operators: [RuleOperator.EQ, RuleOperator.IS_EMPTY, RuleOperator.IS_NOT_EMPTY],
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
    const moduleField = fields[0];
    return [
      {
        id: 'missing-module',
        resultKey: '缺少模块标签',
        expression: {
          id: 'root-1',
          type: RuleNodeType.GROUP,
          operator: RuleGroupOperator.AND,
          children: [
            {
              id: 'condition-1',
              type: RuleNodeType.CONDITION,
              fieldKey: moduleField.key,
              operator: RuleOperator.IS_EMPTY,
              value: '',
            },
          ],
        },
      },
    ] satisfies RuleConfigResultRule[];
  }

  protected override readFieldValue(row: Record<string, unknown>, fieldKey: string) {
    return row[fieldKey];
  }
}

const schema = new TestRuleSchemaSupport();

describe('useRuleConfigState', () => {
  beforeEach(() => {
    window.localStorage.clear();
  });

  it('loads defaults, appends rules, and resets with deep-cloned tree defaults', async () => {
    const fieldsRef = ref(schema.buildFields());
    const state = useRuleConfigState({
      workspaceKey: 'rule-config-state-test',
      fields: computed(() => fieldsRef.value),
      schema,
      createRule(fields) {
        return schema.createResultRule('新增结果', fields[0]);
      },
    });

    await nextTick();

    expect(state.rules.value).toHaveLength(1);
    expect(state.rules.value[0].expression.children).toHaveLength(1);

    state.rules.value[0].expression.children[0].fieldKey = 'owner';
    state.enabled.value = true;
    await nextTick();

    state.resetToDefault();
    await nextTick();

    expect(state.enabled.value).toBe(false);
    expect(state.rules.value[0].resultKey).toBe('缺少模块标签');
    expect(state.rules.value[0].expression.children[0].fieldKey).toBe('moduleName');

    state.appendRule();
    await nextTick();

    expect(state.rules.value).toHaveLength(2);
    expect(state.rules.value[1].resultKey).toBe('新增结果');
    expect(state.dirty.value).toBe(true);
  });

  it('restores persisted workspace tree state independently', async () => {
    const fieldsRef = ref(schema.buildFields());
    const workspaceKey = 'rule-config-state-restore';
    window.localStorage.setItem(
      'rule-config-state:rule-config-state-restore',
      JSON.stringify({
        enabled: true,
        version: 'v1',
        rules: [
          {
            id: 'saved-rule',
            resultKey: '自定义结果',
            expression: {
              id: 'saved-root',
              type: RuleNodeType.GROUP,
              operator: RuleGroupOperator.OR,
              children: [
                {
                  id: 'saved-condition',
                  type: RuleNodeType.CONDITION,
                  fieldKey: 'owner',
                  operator: RuleOperator.IS_NOT_EMPTY,
                  value: '',
                },
              ],
            },
          } satisfies RuleConfigResultRule,
        ],
      }),
    );

    const state = useRuleConfigState({
      workspaceKey,
      fields: computed(() => fieldsRef.value),
      schema,
      createRule(fields) {
        return schema.createResultRule('新增结果', fields[0]);
      },
    });

    await nextTick();

    expect(state.enabled.value).toBe(true);
    expect(state.version.value).toBe('v1');
    expect(state.rules.value[0].resultKey).toBe('自定义结果');
    expect(state.rules.value[0].expression.operator).toBe(RuleGroupOperator.OR);
  });
});
