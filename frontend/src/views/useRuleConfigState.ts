import { computed, ref, watch, type ComputedRef } from 'vue';
import type {
  AbstractRuleConfigSchemaSupport,
  RuleConfigField,
  RuleConfigResultRule,
  RuleConfigState,
} from './rule-config-core';

const STORAGE_PREFIX = 'rule-config-state:';

interface StoredRuleConfigState<TRule extends RuleConfigResultRule> {
  enabled: boolean;
  rules: TRule[];
  version: string | null;
}

interface UseRuleConfigStateOptions<TField extends RuleConfigField, TRule extends RuleConfigResultRule> {
  workspaceKey: string;
  fields: ComputedRef<TField[]>;
  schema: AbstractRuleConfigSchemaSupport<unknown, TField, TRule>;
  createRule: (fields: TField[]) => TRule;
}

function storageKey(workspaceKey: string) {
  return `${STORAGE_PREFIX}${workspaceKey}`;
}

export function useRuleConfigState<TField extends RuleConfigField, TRule extends RuleConfigResultRule>(
  options: UseRuleConfigStateOptions<TField, TRule>,
) {
  const enabled = ref(false);
  const rules = ref<TRule[]>([]);
  const version = ref<string | null>(null);
  const initialized = ref(false);

  function buildDefaultSnapshot(fields: TField[]): StoredRuleConfigState<TRule> {
    return {
      enabled: false,
      rules: options.schema.cloneRules(options.schema.createDefaultRules(fields)),
      version: null,
    };
  }

  function normalizeRules(nextRules: TRule[], fields: TField[]) {
    const clonedRules = options.schema.cloneRules(nextRules);
    clonedRules.forEach((rule) => options.schema.syncRuleWithField(rule, fields));
    return clonedRules;
  }

  function applySnapshot(snapshot: StoredRuleConfigState<TRule>, fields: TField[]) {
    enabled.value = snapshot.enabled;
    version.value = snapshot.version ?? null;
    rules.value = normalizeRules(snapshot.rules, fields);
  }

  function loadSnapshot(fields: TField[]) {
    const raw = window.localStorage.getItem(storageKey(options.workspaceKey));
    if (!raw) {
      return buildDefaultSnapshot(fields);
    }
    try {
      const parsed = JSON.parse(raw) as Partial<StoredRuleConfigState<TRule>>;
      const nextRules = Array.isArray(parsed.rules) ? parsed.rules : [];
      const baseSnapshot = buildDefaultSnapshot(fields);
      if (!nextRules.length) {
        return baseSnapshot;
      }
      return {
        enabled: parsed.enabled === true,
        rules: nextRules,
        version: typeof parsed.version === 'string' ? parsed.version : null,
      } satisfies StoredRuleConfigState<TRule>;
    } catch {
      return buildDefaultSnapshot(fields);
    }
  }

  function persistSnapshot() {
    if (!initialized.value) {
      return;
    }
    window.localStorage.setItem(
      storageKey(options.workspaceKey),
      JSON.stringify({
        enabled: enabled.value,
        rules: options.schema.cloneRules(rules.value),
        version: version.value,
      } satisfies StoredRuleConfigState<TRule>),
    );
  }

  function ensureInitialized() {
    if (initialized.value || !options.fields.value.length) {
      return;
    }
    applySnapshot(loadSnapshot(options.fields.value), options.fields.value);
    initialized.value = true;
  }

  function resetToDefault() {
    ensureInitialized();
    enabled.value = false;
    rules.value = options.schema.cloneRules(options.schema.createDefaultRules(options.fields.value));
    version.value = null;
  }

  function appendRule() {
    ensureInitialized();
    rules.value = [...rules.value, options.createRule(options.fields.value)];
  }

  function removeRule(ruleId: string) {
    ensureInitialized();
    rules.value = rules.value.filter((rule) => rule.id !== ruleId);
  }

  function replaceRules(nextRules: TRule[], nextEnabled = enabled.value) {
    ensureInitialized();
    enabled.value = nextEnabled;
    rules.value = normalizeRules(nextRules, options.fields.value);
  }

  watch(
    options.fields,
    (nextFields) => {
      if (!nextFields.length) {
        return;
      }
      if (!initialized.value) {
        applySnapshot(loadSnapshot(nextFields), nextFields);
        initialized.value = true;
        return;
      }
      rules.value = normalizeRules(rules.value, nextFields);
      if (!rules.value.length) {
        rules.value = options.schema.cloneRules(options.schema.createDefaultRules(nextFields));
      }
    },
    { immediate: true },
  );

  watch([enabled, rules, version], persistSnapshot, { deep: true });

  const dirty = computed(() => {
    const defaultSnapshot = buildDefaultSnapshot(options.fields.value);
    return (
      enabled.value !== defaultSnapshot.enabled ||
      JSON.stringify(rules.value) !== JSON.stringify(defaultSnapshot.rules) ||
      version.value !== defaultSnapshot.version
    );
  });

  const state = computed<RuleConfigState<TRule>>(() => ({
    enabled: enabled.value,
    rules: rules.value,
    version: version.value,
    dirty: dirty.value,
  }));

  return {
    enabled,
    rules,
    version,
    dirty,
    state,
    ensureInitialized,
    appendRule,
    removeRule,
    replaceRules,
    resetToDefault,
  };
}
