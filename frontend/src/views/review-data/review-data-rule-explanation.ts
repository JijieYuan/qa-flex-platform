import type { StatisticRuleMetricDefinition } from '../../api';

export interface ReviewDataRuleFieldDefinition {
  key: string;
  label: string;
  definition: string;
  entryGuideline: string;
  viewerGuideline: string;
  note?: string;
}

export interface ReviewDataRuleCommonQuestion {
  key: string;
  title: string;
  description: string;
}

export interface ReviewDataRuleExplanationContent {
  title: string;
  version: string;
  summary: string;
  scopeDescription: string;
  fieldDefinitions: ReviewDataRuleFieldDefinition[];
  metricDefinitions: StatisticRuleMetricDefinition[];
  commonQuestions: ReviewDataRuleCommonQuestion[];
  recordDialogTip: string;
  problemDialogTip: string;
}

export const reviewDataRuleExplanationContent: ReviewDataRuleExplanationContent = {
  title: '评审数据管理规则说明',
  version: 'v1.0',
  summary:
    '这页同时服务录入和查看两类场景。大部分字段按实际情况填写即可，只有关键字段和统计指标需要统一口径。',
  scopeDescription:
    '当前列表、汇总卡片和详情页只统计未删除的评审记录，以及这些记录下未删除的问题项。所有汇总结果都基于当前筛选后的记录范围。',
  fieldDefinitions: [
    {
      key: 'reviewType',
      label: '评审类型',
      definition: '用于标识本次评审属于哪一种业务场景，是后续筛选和横向对比的基础字段。',
      entryGuideline: '优先选择系统已有类型，保持同类评审使用同一口径，不要为相近场景重复造新名称。',
      viewerGuideline: '查看时可把它理解为“这条记录属于哪类评审活动”，不是问题项的分类。',
    },
    {
      key: 'reviewExperts',
      label: '评审专家',
      definition: '记录实际参与本次评审并承担评审工作的人员，可多选。',
      entryGuideline: '只填写实际参与评审的人，不建议把被抄送、知会或仅旁听人员一起写入。',
      viewerGuideline: '列表里显示的是参与人摘要，详情里会展示完整人员清单。',
    },
    {
      key: 'reviewScalePages',
      label: '评审规模(页)',
      definition: '表示本次实际纳入评审范围的文档页数，是评审缺陷密度的分母。',
      entryGuideline: '填写本次真正评审到的页数，不是整份文档总页数；没有评审到的内容不要计入。',
      viewerGuideline: '这个字段越大，不代表问题越多，只说明本次评审覆盖的范围更大。',
      note: '当页数为空、为 0 或小于等于 0 时，评审缺陷密度按 0 处理。',
    },
    {
      key: 'reviewProduct',
      label: '评审工作产品',
      definition: '说明本次评审针对的具体产物，例如需求说明书、设计说明书或用户手册。',
      entryGuideline: '尽量写清楚被评审对象，避免只填过于泛化的“文档”“材料”等表述。',
      viewerGuideline: '它帮助判断问题出现在哪类产物上，便于后续归因和检索。',
    },
    {
      key: 'documentPosition',
      label: '文档中的位置',
      definition: '定位问题发生在文档中的具体章节、页码或条目位置，是复核问题的关键锚点。',
      entryGuideline: '建议填写到可复核粒度，例如章节号、页码或小节路径，避免只写“前面”“后面”等模糊描述。',
      viewerGuideline: '查看时可将它理解为问题在原文档中的落点，便于快速回看上下文。',
    },
    {
      key: 'problemCategory',
      label: '问题类别',
      definition: '用于归类问题的性质，是后续统计和复盘的主要维度之一。',
      entryGuideline: '按已有类别就近归类，重点看问题本身属于哪类，不要混入处理动作或责任判断。',
      viewerGuideline: '同一条问题通常只归入一个主类别，便于后续做分类汇总。',
    },
    {
      key: 'problemStatus',
      label: '问题状态',
      definition: '表示单条问题项当前的处理状态，口径作用在问题项层面，不是整条评审记录层面。',
      entryGuideline: '按每条问题当前真实状态维护；如果一条评审下有多个问题，不同问题可以处于不同状态。',
      viewerGuideline: '按问题状态筛选时，筛出来的是“包含该状态问题的评审记录”，不是把其他问题项从记录里剔除。',
      note: '这是最容易产生误解的字段，建议在查看汇总数据时重点留意。',
    },
  ],
  metricDefinitions: [
    {
      key: 'totalRecords',
      label: '评审记录',
      definition: '当前筛选结果中的评审记录数。',
      formula: '评审记录 = 当前筛选结果中的记录条数',
      note: '对应页面顶部“评审记录”汇总卡片。',
    },
    {
      key: 'totalProblemItems',
      label: '评审问题',
      definition: '当前筛选结果中全部评审记录的问题总数。',
      formula: '评审问题 = Σ 每条评审记录的问题总计',
      note: '对应页面顶部“评审问题”汇总卡片。',
    },
    {
      key: 'averageReviewScalePages',
      label: '平均页数',
      definition: '当前筛选结果中 reviewScalePages 的平均值。',
      formula: '平均页数 = Σ 评审规模(页) / 评审记录数',
      note: '对应页面顶部“平均页数”汇总卡片。',
    },
    {
      key: 'averageProblemCount',
      label: '平均问题数',
      definition: '当前筛选结果中 problemCount 的平均值。',
      formula: '平均问题数 = Σ 问题总计 / 评审记录数',
      note: '它反映的是每条评审记录平均有多少问题，不是平均缺陷密度。',
    },
    {
      key: 'problemCount',
      label: '问题总计',
      definition: '单条评审记录下的未删除问题项数量。',
      formula: '问题总计 = 当前评审记录下的问题项数量',
      note: '对应列表中的“问题总计”和详情里的“问题总计”。',
    },
    {
      key: 'problemDensity',
      label: '评审缺陷密度(个/页)',
      definition: '单条评审记录的问题数相对于评审页数的比值，用于衡量单位页数上的问题密集程度。',
      formula: '评审缺陷密度 = 问题总计 / 评审规模(页)',
      note: '当评审规模为空、为 0 或小于等于 0 时，系统按 0 处理。',
    },
  ],
  commonQuestions: [
    {
      key: 'status-filter',
      title: '按问题状态筛选后，页面上的“评审问题”是不是只统计该状态的问题？',
      description:
        '不是。当前口径是先筛出“包含该状态问题的评审记录”，再展示这些记录的完整问题总计和相关汇总数据。',
    },
    {
      key: 'average-density',
      title: '平均问题数能不能直接理解成评审缺陷密度？',
      description:
        '不能。平均问题数是“每条评审平均有多少问题”，评审缺陷密度是“每页平均有多少问题”，两者分母不同。',
    },
    {
      key: 'page-scope',
      title: '评审规模是不是直接填文档总页数？',
      description:
        '不建议。更推荐填写本次实际纳入评审范围的页数，这样缺陷密度才更能反映本次评审的真实情况。',
    },
  ],
  recordDialogTip:
    '请重点统一“评审类型”“评审专家”“评审规模(页)”和“评审工作产品”的填写口径，其中评审规模会直接影响评审缺陷密度。',
  problemDialogTip:
    '请重点统一“文档中的位置”“问题类别”“问题状态”的填写口径；问题状态按单条问题项维护，不代表整条评审记录状态。',
};
