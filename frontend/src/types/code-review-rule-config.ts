export type CodeReviewRuleFieldType = 'text' | 'number' | 'select' | 'multi-select';

export type CodeReviewRuleOperator =
  | 'eq'
  | 'ne'
  | 'contains'
  | 'notContains'
  | 'notIn'
  | 'gt'
  | 'gte'
  | 'lt'
  | 'lte'
  | 'isEmpty'
  | 'isNotEmpty';

export type CodeReviewRuleMatchMode = 'all' | 'any';

export interface CodeReviewRuleOption {
  label: string;
  value: string;
}

export interface CodeReviewRuleFieldDefinition {
  key: string;
  label: string;
  type: CodeReviewRuleFieldType;
  operators: CodeReviewRuleOperator[];
  options?: CodeReviewRuleOption[];
}

export interface CodeReviewRuleCondition {
  id: string;
  fieldKey: string;
  operator: CodeReviewRuleOperator;
  value: string;
}

export interface CodeReviewRuleGroup {
  id: string;
  matchMode: CodeReviewRuleMatchMode;
  conditions: CodeReviewRuleCondition[];
}

export interface CodeReviewRuleConfig {
  enabled: boolean;
  groups: CodeReviewRuleGroup[];
  updatedAt: string | null;
}

export interface CodeReviewRulePreviewSample {
  mergeRequestId: number | null;
  mergeRequestIid: number | null;
  projectName: string;
  moduleName: string;
  owner: string;
  targetBranch: string;
  mergeRequestContent: string;
  reasons: string[];
}

export interface CodeReviewRulePreviewResponse {
  baseTotal: number;
  filteredTotal: number;
  deltaCount: number;
  retainedRate: number;
  samples: CodeReviewRulePreviewSample[];
}
