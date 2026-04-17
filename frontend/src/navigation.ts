import {
  Bell,
  DataAnalysis,
  Document,
  Histogram,
  Monitor,
  Operation,
  Setting,
} from '@element-plus/icons-vue';

export type ModuleKey =
  | 'quality-board'
  | 'review-data'
  | 'code-review'
  | 'integration-test'
  | 'question-metrics'
  | 'customer-issues'
  | 'system-settings';

export type PageKey =
  | 'quality-board-home'
  | 'review-data-home'
  | 'code-review-home'
  | 'code-review-illegal-records'
  | 'integration-test-home'
  | 'question-metrics-home'
  | 'customer-issues-home'
  | 'mirror-settings'
  | 'database-browser'
  | 'module-management';

export interface ShellPage {
  key: PageKey;
  label: string;
  description: string;
  path: string;
}

export interface ShellModule {
  key: ModuleKey;
  label: string;
  icon: unknown;
  title: string;
  description: string;
  pages: ShellPage[];
}

export const modules: ShellModule[] = [
  {
    key: 'quality-board',
    label: '统计分析',
    icon: Histogram,
    title: '统计分析',
    description: '统一承载汇总统计表与明细下钻视图。',
    pages: [
      {
        key: 'quality-board-home',
        label: '镜像表基础统计',
        description: '基于当前 GitLab 镜像数据展示汇总统计与明细下钻。',
        path: '/quality-board/home',
      },
    ],
  },
  {
    key: 'review-data',
    label: '评审数据',
    icon: Document,
    title: '评审数据',
    description: '承载评审记录查看、筛选和详情复核的记录类页面。',
    pages: [
      {
        key: 'review-data-home',
        label: '评审数据管理',
        description: '展示评审记录、评分，以及关联的外部指标与更新情况。',
        path: '/review-data/home',
      },
    ],
  },
  {
    key: 'code-review',
    label: '代码走查',
    icon: Operation,
    title: '代码走查',
    description: '承载代码走查相关的记录类页面与独立表单入口。',
    pages: [
      {
        key: 'code-review-illegal-records',
        label: '代码走查非法记录',
        description: '展示代码走查场景下的非法记录明细列表，并预留后续接入镜像字段和外部指标。',
        path: '/code-review/illegal-records',
      },
    ],
  },
  {
    key: 'integration-test',
    label: '集成测试',
    icon: Monitor,
    title: '集成测试',
    description: '预留后续统计分析页面。',
    pages: [
      {
        key: 'integration-test-home',
        label: '集成测试',
        description: '当前模块暂未接入正式业务页面。',
        path: '/integration-test/home',
      },
    ],
  },
  {
    key: 'question-metrics',
    label: '议题统计',
    icon: DataAnalysis,
    title: '议题统计',
    description: '按业务专题查看议题相关统计看板。',
    pages: [
      {
        key: 'question-metrics-home',
        label: '系统测试缺陷汇总',
        description: '展示系统测试缺陷的多级统计表头与模块维度汇总。',
        path: '/question-metrics/home',
      },
    ],
  },
  {
    key: 'customer-issues',
    label: '客户问题',
    icon: Bell,
    title: '客户问题',
    description: '预留后续统计分析页面。',
    pages: [
      {
        key: 'customer-issues-home',
        label: '客户问题',
        description: '当前模块暂未接入正式业务页面。',
        path: '/customer-issues/home',
      },
    ],
  },
  {
    key: 'system-settings',
    label: '系统设置',
    icon: Setting,
    title: '系统设置',
    description: '维护 GitLab 数据镜像、数据库查看与系统配置。',
    pages: [
      {
        key: 'mirror-settings',
        label: '数据镜像设置',
        description: '管理 GitLab 数据镜像的连接、同步和日志。',
        path: '/system-settings/mirror-settings',
      },
      {
        key: 'database-browser',
        label: '数据库查看',
        description: '快速浏览本地平台数据库中的核心业务表数据。',
        path: '/system-settings/database-browser',
      },
      {
        key: 'module-management',
        label: '模块管理',
        description: '预留模块与菜单配置能力。',
        path: '/system-settings/module-management',
      },
    ],
  },
];

export const moduleByKey = new Map(modules.map((item) => [item.key, item] as const));
export const pageByKey = new Map(modules.flatMap((item) => item.pages.map((page) => [page.key, page] as const)));
