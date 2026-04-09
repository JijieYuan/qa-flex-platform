import type { StatisticBoardRuleExplanationResponse } from '../api';

export function createFallbackRuleExplanation(boardKey: string, reason: string): StatisticBoardRuleExplanationResponse {
  return {
    boardKey,
    supported: false,
    title: '规则说明',
    version: null,
    scopeDescription: null,
    summary: null,
    flowSteps: [],
    metricDefinitions: [],
    unsupportedReason: reason,
  };
}

export function ruleStepRemovedCount(step: { inputCount: number; outputCount: number }) {
  return Math.max(step.inputCount - step.outputCount, 0);
}

export function ruleStepRetainedRate(step: { inputCount: number; outputCount: number }) {
  if (!step.inputCount) {
    return '0%';
  }
  return `${((step.outputCount / step.inputCount) * 100).toFixed(1)}%`;
}

export function ruleStepSummary(step: { inputCount: number; outputCount: number }, index: number) {
  const removed = ruleStepRemovedCount(step);
  if (removed <= 0) {
    return `第 ${index + 1} 步处理后，数据没有减少，仍保留 ${step.outputCount} 条。`;
  }
  return `第 ${index + 1} 步处理后，剩余 ${step.outputCount} 条，较上一步减少 ${removed} 条，保留比例 ${ruleStepRetainedRate(step)}。`;
}

export function metricFormulaSummary(metric: { label: string; definition: string }) {
  return `${metric.label}：${metric.definition}`;
}
