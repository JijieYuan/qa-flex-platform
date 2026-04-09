# 数据与表解耦的统计架构设计方案

更新时间：2026-04-08  
适用项目：`D:\projects\data_collection_platform`

## 1. 设计目标

这份方案面向当前项目的真实约束：

- 用户少，但平台非常重要
- 开发人力紧张，不能引入过重的新中间件
- 规则变化频繁，必须能持续扩展
- 前端使用者以 QA 和业务人员为主，页面必须直观
- 希望“数据语义”和“物理表结构”解耦，避免一改规则就改表

本方案不追求企业级大而全平台，而是以“现有 Spring Boot + PostgreSQL + Vue 单体”做增量演进。

## 2. 核心结论

推荐采用：

- `ODS 镜像层 + 业务事实层 + 规则聚合层 + 展示解释层`

明确不采用：

- 每个指标一张表
- 把规则直接写死成页面逻辑
- 把解析逻辑、采集逻辑、统计逻辑全部揉在一个服务里
- 为了这类项目引入重量级规则引擎或微服务拆分

## 3. 总体架构

### 3.1 四层结构

#### 第一层：ODS 镜像层

职责：

- 保存 GitLab 原始镜像数据
- 保持来源真实、结构接近源系统
- 不承载业务定义

当前对应：

- `ods_gitlab_issues`
- `ods_gitlab_merge_requests`
- `ods_gitlab_notes`
- `ods_gitlab_labels`
- `ods_gitlab_label_links`
- 其他 `ods_gitlab_*`

原则：

- 不把业务解析结果回写进 ODS
- ODS 只做“原始来源层”

#### 第二层：业务事实层

职责：

- 保存归一化后的“事实数据”
- 把多个来源统一成稳定字段
- 为统计和规则提供统一输入

当前已存在：

- `collect_form_records`
- `code_review_external_metrics`

建议新增：

- `issue_fact`
- `merge_request_fact`

当前实现状态：

- `issue_fact`
  - 已落入 `backend/src/main/resources/schema.sql`
  - 已补充实体 `IssueFact` 与 `IssueFactMapper`
- `merge_request_fact`
  - 已落入 `backend/src/main/resources/schema.sql`
  - 已补充实体 `MergeRequestFact` 与 `MergeRequestFactMapper`
- 数据库浏览页已补充这两张表的查看入口

原则：

- 事实表不是指标表
- 一条事实记录代表一个业务对象的当前快照或某次归一化结果

#### 第三层：规则聚合层

职责：

- 根据过滤规则、筛选条件、计算公式产出统计结果
- 屏蔽底层表差异
- 输出给页面统一的业务语义结果

建议实现方式：

- 先保留 Java 服务实现
- 逐步把规则抽成可配置元数据
- 不急着上独立规则引擎

#### 第四层：展示解释层

职责：

- 面向 QA / 业务展示“为什么是这个结果”
- 强调：
  - 哪些数据被排除
  - 当前筛选范围是什么
  - 最终数字怎么算

原则：

- 不直接暴露底层表名
- 不用技术术语做主文案

## 4. 当前项目的推荐模块划分

在单体项目内保持模块化，不拆微服务。

### 4.1 `sync` 模块

职责：

- GitLab 镜像同步
- Webhook
- 补偿同步
- 表结构发现和镜像表维护

当前已有：

- `GitlabMirrorSyncService`
- `GitlabMirrorSchemaService`
- `GitlabWebhook*`

### 4.2 `fact` 模块

职责：

- 将 ODS 数据和外部导入数据整理成统一事实
- 提供事实构建、事实刷新、事实查询

建议新增职责：

- `IssueFactBuildService`
- `MergeRequestFactBuildService`

### 4.3 `import` 模块

职责：

- 接收外部工具回传
- 解析 MR 评论 / Sonar / 机器人消息
- 落正式导入表

当前已有基础：

- `code_review_external_metrics`

后续建议：

- 增加导入接口或定时导入任务
- 增加来源类型、原始载荷、导入状态

### 4.4 `rule` 模块

职责：

- 定义过滤规则
- 定义判定规则
- 定义统计公式

第一阶段做法：

- 规则仍由 Java 服务承载
- 但结构上抽成统一对象和统一出口

第二阶段做法：

- 抽成数据库配置或 JSON 配置

### 4.5 `board` / `record` 模块

职责：

- 输出统计板数据
- 输出记录列表数据
- 输出规则说明、筛选条件说明、计算公式说明

当前已有基础：

- `StatisticBoard*`
- `CodeReviewIllegalRecordService`

## 5. 数据模型方案

### 5.1 议题类

#### 原始来源

- `ods_gitlab_issues`
- `ods_gitlab_labels`
- `ods_gitlab_label_links`
- `ods_gitlab_notes`
- issue label event 对应镜像表

#### 建议正式事实表：`issue_fact`

推荐字段：

- `issue_id`
- `issue_iid`
- `project_id`
- `project_name`
- `title`
- `status`
- `severity_level`
- `priority_level`
- `urgency`（兼容字段）
- `testing_phase`
- `bug_status`
- `module_name`
- `category`
- `delay_issue`
- `delay_cause`
- `cause`
- `fix_user`
- `fix_label_time`
- `is_deleted`
- `source_summary`
- `raw_payload`
- `fact_version`
- `last_source_sync_at`
- `updated_at`

针对当前系统测试缺陷统计，`issue_fact` 还应正式承接以下归一化业务字段：

- 严重程度与别名
  - `severity_level`
  - `severity_alias`
  - 统一口径：
    - `LEVEL1` = `一级缺陷`、`一级严重`
    - `LEVEL2` = `二级缺陷`、`二级严重`
    - `LEVEL3` = `三级缺陷`、`三级严重`
    - `SUGGESTION` = `建议`、`需求`、`需求如此`
- 优先级
  - `priority_level`
  - 统一口径：
    - `P1` 只识别真实 `P1` 标签
    - `P2` 只识别真实 `P2` 标签
    - `P3` 只识别真实 `P3` 标签
  - 不由 `severity_level` 自动映射
- 公共过滤与排除
  - `is_excluded`
  - `exclusion_reason`
  - 统一口径：
    - 标签含 `功能屏蔽 / 已拒绝 / 建议`
    - `申请否决 + Closed`
    - `数据异常 + Closed`
    - `设计如此 + Closed`
- 修复状态
  - `is_fixed`
  - 统一口径：
    - 标签含 `已修复 / 待合并`
    - 或 `Closed + 未复现`
- 缺陷原因归一化
  - `reason_category`
  - 统一口径：
    - `需求理解偏差` = `新增理解偏差数量 + 需求理解有误数量`
    - `新增需求` = `新增需求数量 + 新增需求问题数量`
    - `编码逻辑错误` = `业务逻辑错误 + 编码逻辑错误`
    - `环境部署问题` = `编译/打包/部署问题 + 编译打包问题`
    - `算法机制不支持` = `机制不支持 + 算法/机制不支持`
- 延期分析
  - `delay_issue`
  - `delay_reason`
  - `delay_cause`
  - 延期原因固定七类：
    - `技术卡点`
    - `方案卡点`
    - `资源卡点`
    - `数据异常`
    - `算法问题`
    - `机制问题`
    - `计算效率`
- 一级缺陷二次分类
  - `is_regression`
  - `is_crash`
  - `is_level1_other`
  - 强校验口径：
    - `一级缺陷 + 标题含 退 / 回退 / 倒退` => `is_regression`
    - `一级缺陷 + 标题含 挂机` => `is_crash`
    - `一级缺陷` 且排除上述两类 => `is_level1_other`
- 非法数据断言
  - `is_illegal`
  - `illegal_reason`
  - 统一口径：
    - 缺失严重程度
    - 缺失模块
    - 流程越位：未关闭且缺失 `待合并 / 设计如此 / 建议 / 需求 / 申请延期`
- SLA 与响应时效
  - `has_response`
  - `is_response_delayed`
  - `resolve_sla_days`
  - `resolve_deadline_at`
  - `is_resolve_delayed`
  - 统一口径：
    - 评论区出现 `# 问题调研情况说明` 视为已响应
    - 默认解决时限 18 天
    - 模板中若存在更短的预计解决时间，则用更短值
    - 超时且未带 `申请延期 / 数据异常 / 需求如此 / 未复现 / 已修复` 标签，标记解决延期
- 模块全量性
  - `primary_module_name`
  - `module_names`
  - 一条 issue 可以映射多个模块
  - 项目总数按 issue 去重，按模块拆分时按 `module_names` 展开
- 历史遗留
  - `is_legacy`
  - 判定：当前 Open 且创建时间早于当前测试阶段开始时间

为支撑 `is_legacy` 与阶段性口径，建议补一张轻量正式配置表：

- `testing_phase_calendar`
  - `project_id`
  - `testing_phase`
  - `phase_start_at`
  - `phase_end_at`
  - `enabled`

用途：

- 统一承接“一级/二级/三级缺陷、P1/P2/P3、延期、修复率、遗留率”等议题类统计
- 其中严重程度和优先级为两套独立业务维度

### 5.2 代码走查类

#### 原始来源

- `ods_gitlab_merge_requests`
- `ods_gitlab_merge_request_metrics`
- `ods_gitlab_merge_request_reviewers`
- `ods_gitlab_merge_request_assignees`
- `ods_gitlab_notes`
- `ods_gitlab_labels`
- `ods_gitlab_label_links`

#### 当前保留的正式表

- `collect_form_records`
- `code_review_external_metrics`

#### 建议正式事实表：`merge_request_fact`

推荐字段：

- `merge_request_id`
- `merge_request_iid`
- `project_id`
- `project_name`
- `repository_name`
- `title`
- `merged_at`
- `merged_by`
- `owner`
- `target_branch`
- `module_name`
- `review_status`
- `review_duration_minutes`
- `comment_rate`
- `comment_rate_source`
- `defect_count`
- `defect_count_source`
- `scan_status`
- `scan_bug_count`
- `added_lines`
- `is_deleted`
- `source_summary`
- `raw_payload`
- `fact_version`
- `last_source_sync_at`
- `updated_at`

用途：

- 统一承接“非法代码走查、注释率、缺陷数、review 时长、缺陷密度、扫描结果”等 MR 类统计

## 6. 规则设计方案

### 6.1 设计原则

- 规则不直接绑定某张表
- 规则面向“统一语义字段”
- 规则输出面向业务语言
- 统计服务不重复保存业务判定逻辑，`FactBuildService + *NormalizationRules` 才是 SSOT

### 6.2 第一阶段

用 Java 结构化承载规则：

- `FilterRule`
- `MetricFormula`
- `ClassificationRule`
- `RuleExplanation`

好处：

- 对现有项目侵入小
- 便于快速迭代
- 可直接服务当前页面

### 6.3 第二阶段

逐步配置化：

- 把规则定义存到数据库 JSON 字段或独立配置文件
- 支持版本号
- 支持预览和发布

### 6.4 不建议当前就做的事

- 不建议直接上 Drools 或复杂规则引擎
- 不建议先做完全可视化规则编排器

原因：

- 成本高
- 心智负担大
- 维护复杂
- 不适合当前团队规模

## 7. 前端展示方案

### 7.1 面向 QA 的统一展示原则

- 入口明显
- 先看结论
- 再看哪些被排除
- 再看当前筛选范围
- 最后看这个数字怎么算

### 7.2 规则说明统一结构

建议所有统计页和记录页都统一成：

- `先看结果`
- `当前统计范围`
- `哪些情况会被排除`
- `数据是怎么一步步变化的`
- `最后数字怎么算`

### 7.3 前端不应该展示的内容

- 底层表名
- SQL 条件
- 当前字段为空
- 当前系统尚未接入
- 研发临时实现细节

这些内容只保留给研发文档和排查信息。

## 8. 推荐的迭代顺序

### 第一阶段：稳住现有架构

- 保留 ODS 镜像层
- 保留 `collect_form_records`
- 保留 `code_review_external_metrics`
- 修正现有统计页和记录页的规则解释展示

### 第二阶段：补事实层

- 新增 `issue_fact`
- 新增 `merge_request_fact`
- 增加事实构建服务

### 第三阶段：补真实数据导入

- 从 `ods_gitlab_notes` 解析 issue / MR 评论
- 接入外部注释率结果
- 接入 Sonar / 机器人结果
- 统一导入到事实层或正式导入表

### 第四阶段：规则配置化

- 抽出统一规则对象
- 补规则版本
- 支持预览和发布

### 第五阶段：Flow 与解释增强

- 页面继续优化可读性
- 不强调技术 Flow
- 强调“数量变化 + 排除原因 + 计算公式”

## 9. 为什么这版方案适合当前项目

- 不推翻现有代码
- 不要求微服务
- 不要求新中间件
- 能和当前 ODS 镜像体系直接兼容
- 能支持数据与数据表解耦
- 后续扩展不会被某一张表绑死
- 更适合小团队长期维护

## 10. 最终建议

当前项目后续的主方向应明确为：

- 保留 `ODS 镜像层`
- 增加轻量 `业务事实层`
- 规则和指标围绕“统一语义字段”构建
- 页面只看业务解释，不看底层表结构

一句话总结：

不要再按“一个指标一张表”去设计，也不要让页面和规则直接绑表；要让“表存事实，服务做语义，规则做聚合，前端做解释”。

## 11. 对老平台“双库 + 动态切换”设计的参考结论

你补充的老平台设计说明了三件事：

- 议题和 MR 的原始数据确实来自多个 GitLab 数据源
- 注释率等补充指标可能来自独立外部存储
- 老平台通过请求前缀和拦截器做动态数据源切换

这些信息对当前项目有参考价值，但不能原样照搬。

### 11.1 可以参考的部分

- 多来源数据接入是合理的
  - 当前项目后续完全可能同时接多个 GitLab 来源
  - 例如 CC、DGM、其他环境
- 注释率等外部结果单独存储是合理的
  - 老平台用 Mongo 的 `AnnotationRateInfo`
  - 当前项目可以保留“外部结果独立存储”的思想
- 统计结果实时计算是合理的
  - 老平台没有专门的结果表
  - 当前项目也应继续保持“事实表 + 实时计算”

### 11.2 不建议直接照搬的部分

- 不建议当前项目照搬“按请求前缀切数据库连接”的复杂动态数据源方案
  - 当前项目开发人力紧张
  - 现有代码主链路已经围绕单主库 PostgreSQL 建立
  - 如果现在引入复杂动态数据源，会显著增加：
  - 调试成本
  - 事务边界复杂度
  - 测试成本
  - 文档和心智负担
- 不建议为了复刻老平台再引入 Mongo
  - 当前项目现阶段不预设外部注释率一定来自 Mongo
  - 外部来源的真实接入方式尚未最终确定
  - 现阶段更适合保留统一导入口，而不是先绑定某一种外部存储

### 11.3 更适合当前项目的轻量替代方案

当前项目建议采用：

- 一个主业务库
  - 即当前 PostgreSQL
- 多来源数据在库内分层管理
  - `ods_gitlab_*` 承接不同来源的原始镜像
  - `issue_fact`
  - `merge_request_fact`
  - `collect_form_records`
  - `code_review_external_metrics`
- 在数据中显式保留来源字段
  - 例如：
  - `source_system`
  - `source_instance`
  - `source_project_type`
  - `source_summary`

也就是说：

- 我们不靠“切换数据库连接”来区分 CC / DGM
- 我们靠“来源字段 + 统一事实模型”来区分 CC / DGM

### 11.3.1 外部来源数据的当前原则

对于注释率、机器人结果、Sonar 结果、其他外部平台回传数据，当前方案明确采用：

- 先预留统一导入口
- 先统一字段语义
- 先统一导入责任边界
- 暂不提前绑定外部系统类型

当前明确不预设：

- 一定来自 MongoDB
- 一定来自 HTTP 回调
- 一定来自定时拉取
- 一定来自消息订阅

当前只确定：

- 这些数据属于“外部来源补充事实”
- 需要和 MR / Issue 主事实关联
- 需要在文档和字段设计中预留来源标识、来源摘要、原始载荷

因此，现阶段文档和表设计只保留“统一外部导入口”的口子，不把某一个外部技术方案提前写死。

当前实现状态：

- `issue_fact` / `merge_request_fact` 已正式预留：
  - `source_system`
  - `source_instance`
  - `ingest_channel`
  - `source_summary`
  - `raw_payload`
- 当前尚未实现真实导入链路
- 当前已把 `system-test-defect-summary` 与 `code-review-illegal-records` 接成“fact 优先、ODS 回退”
- 当前已补充 `FactBuildService` 与 `POST /api/facts/rebuild` 手动构建入口
- 当前刷新统计页/代码走查页时，会尝试增量构建对应事实表

### 11.4 为什么这个方案更适合当前项目

- 实现更简单
- 运维成本更低
- 测试和排查更直接
- 更适合小团队长期维护
- 更容易和当前页面、服务、同步逻辑兼容

### 11.5 当前项目的推荐数据库策略

最终建议是：

- 当前阶段：
  - 继续使用单 PostgreSQL 主库
  - 不引入 Mongo
  - 不引入复杂动态数据源切换
- 数据来源区分方式：
  - 通过表内来源字段区分
  - 通过业务规则区分
  - 通过页面筛选条件区分
- 如果未来确实出现“多个源库必须独立直连且无法汇聚”的情况
  - 再补充轻量动态数据源能力
  - 但那应是后续演进，不是当前第一优先级

## 12. 性能瓶颈的预设治理方案

当前方案不是“先不考虑性能”，而是明确采用“先把瓶颈局部化，再按层治理”的思路。

### 12.1 总体原则

- 不追求一步到位的大数据平台
- 先通过分层把性能问题拆小
- 优先解决最容易成为瓶颈的局部链路
- 没有出现瓶颈前，不提前引入重型基础设施

换句话说：

- 先把问题变成“可定位、可替换、可局部优化”
- 再按真实瓶颈做针对性治理

### 12.2 当前预设的三类主要瓶颈

#### 一类：ODS 到事实层的构建压力

这是最可能最早出现的瓶颈。

场景：

- GitLab 镜像数据持续增长
- issue / MR 更新频繁
- `issue_fact` / `merge_request_fact` 需要不断重建或补写

预设治理方案：

- 增量构建优先
  - 只处理最近变更的数据
  - 以 `updated_at_source`、`mirror_synced_at`、`fact_refreshed_at` 作为增量依据
- 批量写入优先
  - 采用分批 upsert
  - 避免逐条写入
- 构建链路异步化
  - 镜像同步完成后，触发事实构建任务
  - 页面查询不直接等待大批量事实刷新完成
- 分域构建
  - `issue_fact` 和 `merge_request_fact` 分开构建
  - 不做全平台一把梭重刷

当前实现状态：

- 事实表已建
- 增量构建服务尚未实现
- 批量 upsert 机制在 Mapper 级别已具备基础形态
- 当前已补充 `ods_updated_at` 作为事实表增量水位线字段

#### 二类：事实表空间膨胀

场景：

- `raw_payload` 长期保存大文本
- 来源说明和标签汇总字段不断增长
- 历史事实数据积累后主表变重

预设治理方案：

- 查询严格禁止 `select *`
  - 统计和页面查询只取所需字段
- 大字段默认不参与列表查询
  - `raw_payload` 只用于追溯和排查
- 保留冷热分离口子
  - 后续如数据量持续增大，可按时间迁移历史事实
  - 主表只保留近一段时间的高频数据
- 索引只围绕高频筛选字段建立
  - 不对 `raw_payload` 这类大字段做无意义索引

当前实现状态：

- 已在表设计上把来源字段与业务字段拆开
- 已明确 `raw_payload` 只作为追溯字段
- 还未实现冷热分离和归档策略

#### 三类：规则聚合查询压力

场景：

- 规则越来越复杂
- 统计接口查询范围越来越大
- 服务层出现多次回表或 N+1 查询

预设治理方案：

- 规则尽量围绕单条事实记录可判定的字段展开
  - 让复杂判断尽量在事实构建阶段归一化
- 页面查询优先单表或少量固定 join
  - 避免运行时跨多张原始镜像表频繁拼接
- 对高频统计维度优先做事实字段沉淀
  - 例如模块名、测试阶段、严重程度、owner、注释率、缺陷数
- 只有在真实查询变慢时，才考虑补轻量预聚合
  - 不是先做结果表
  - 而是为热点场景增加局部辅助表或缓存

当前实现状态：

- 这套治理思想已写入方案
- 现有统计服务仍主要基于 ODS 直接聚合
- 尚未切换到 `issue_fact` / `merge_request_fact`

### 12.3 当前不建议做的重型方案

在没有真实性能证据前，当前不建议先做：

- 微服务拆分
- 消息队列驱动的大规模异步编排
- 复杂规则引擎
- 专门的结果表体系
- 多数据库动态路由作为主要查询模式

原因：

- 人力成本高
- 调试复杂
- 维护门槛高
- 很容易把“小团队可维护系统”做成“局部先进、整体脆弱”

### 12.4 适合当前项目的性能演进路线

第一阶段：

- 先把 `issue_fact` / `merge_request_fact` 真正接入构建链路
- 查询逐步从 ODS 直查迁到事实层

第二阶段：

- 补增量构建
- 补批量 upsert
- 补事实刷新任务

第三阶段：

- 基于真实慢查询决定是否加局部预聚合
- 基于真实数据量决定是否做冷热分离

### 12.5 最终结论

当前方案是有性能预设的，但不是“先做一个大而重的性能方案”。

我们的预设核心是：

- 用 ODS / 事实 / 规则 / 展示 分层，把瓶颈局部化
- 用事实层降低看板查询复杂度
- 用增量构建和批量 upsert 控制高频更新成本
- 用大字段隔离和后续冷热分离控制表膨胀
- 真遇到热点瓶颈，再做局部强化，不提前铺开企业级重方案

## 13. 当前新增的设计收口

### 13.1 唯一真相源

当前已开始按 “Fact 为唯一业务判定来源” 收口：

- `system-test-defect-summary`
  - 统计服务已改为读取 `issue_fact`
  - 严重程度、优先级、分类等判定不再在 BoardService 中重复实现旧的 ODS 关键词逻辑
- `code-review-illegal-records`
  - 已改为读取 `merge_request_fact`

当前仍需继续推进的部分：

- 其他后续统计页也应逐步切到事实层
- 事实层字段一旦稳定，统计服务应继续退化成“取数器”

### 13.2 增量水位线

当前已正式引入：

- `issue_fact.ods_updated_at`
- `merge_request_fact.ods_updated_at`

当前用途：

- 增量构建时，以 `ods_updated_at` 作为事实表已处理到哪里的水位线
- 后续数据量继续增大时，优先基于该字段做增量构建，而不是全量重刷

### 13.3 字段归一化的单点映射

当前已新增：

- `IssueFactNormalizationRules`

当前目标：

- 把严重程度、优先级、测试阶段、分类、延期等字段的归一化逻辑收敛到单点
- 后续修改映射规则时，只改这一处，不在构建链路各处散落 if/else

### 13.4 当前确认的 issue_fact 全量收口范围

围绕系统测试缺陷统计，当前已确认必须一次性收口进 `issue_fact` 的范围如下：

- 严重程度标准化：`LEVEL1 / LEVEL2 / LEVEL3 / SUGGESTION`
- 优先级标准化：`P1 / P2 / P3`
- 公共过滤口径：`功能屏蔽 / 已拒绝 / 建议 / 申请否决+Closed / 数据异常+Closed / 设计如此+Closed`
- 修复状态：`已修复 / 待合并 / 未复现`
- 缺陷原因归一化
- 延期原因七大类
- 特殊一级分类：`回退 / 挂机 / 其他一级`
- 非法数据断言：缺级别、缺模块、流程越位
- SLA：已响应、响应延期、解决时限、解决延期
- 模块全量性：支持一条 issue 关联多个模块
- 历史遗留：`is_legacy`

这部分不再按“先补部分规则”推进，而是按整套口径一次性设计和实现，避免后续再出现 BoardService 与 FactBuildService 双边维护。
### 14. 2026-04-08 最新落地状态

本方案中围绕 `issue_fact` 的第一阶段实现已经完成，当前不是“只停留在设计”，而是已经具备真实运行能力：

- 已新增正式表：
  - `issue_fact`
  - `testing_phase_calendar`
- 已新增事实构建服务：
  - `FactBuildService`
- 已新增手动构建入口：
  - `POST /api/facts/rebuild?scope=issue&full=true|false`
- 已新增统一归一化入口：
  - `IssueFactNormalizationRules`
- 已完成首个消费方改造：
  - `SystemTestDefectSummaryBoardService` 改为优先读取 `issue_fact`

当前已落地到 `issue_fact` 的字段能力：

- 严重程度标准化
- 公共排除标记
- 修复状态判定
- 缺陷原因归一化
- 延期原因归一化
- 回退 / 挂机 / 其他一级分类
- 非法数据断言
- 响应与解决时效
- 多模块字段
- 历史遗留标记
- 来源追溯字段

当前工程验证结果：

- 本地已成功执行 `issue_fact` 全量构建
- 本地库已生成 `382` 条 `issue_fact`
- `system-test-defect-summary` 已切换到 `issue_fact` 优先读取

当前边界也需要明确保留：

- 当前本地 ODS 数据以测试/压测样例为主，真实业务标签覆盖不足
- 因此 `severity_level / priority_level / is_fixed / has_response / is_excluded` 等字段在当前样例库中的命中率还不代表真实生产效果
- `is_legacy` 依赖 `testing_phase_calendar`，如果未配置阶段起始时间，则不会命中真实历史遗留判断

### 10.1 2026-04-09 建模纠偏

- 根据交接文档确认：
  - 一级/二级/三级缺陷属于严重程度体系
  - `P1/P2/P3` 属于优先级体系
  - 二者不存在绝对一一对应关系
- 因此当前正式方案调整为：
  - `issue_fact.severity_level` 仅承接严重程度
  - `issue_fact.priority_level` 单独承接优先级
  - BoardService 不再使用 `severity_level` 代替 `P1/P2/P3`
- 同时补充系统测试口径修正：
  - 统计范围限定为带有“系统测试 / 回归测试”标签的议题
  - 公共过滤按 `需求如此 + Closed`，不再使用 `设计如此 + Closed`
  - 系统测试非法数据除缺失严重程度/模块外，还需要校验：
    - 已修复后是否按模板回复
    - 缺陷原因是否唯一
    - 多条原因评论取最后一条
  - 系统测试缺陷汇总中，分母为 `0` 时比率展示为 `/`

因此，本方案的下一阶段重点不是继续讨论是否采用 `issue_fact`，而是：

1. 导入真实项目标签与评论数据
2. 配置 `testing_phase_calendar`
3. 继续把其他统计服务改造成纯 fact 聚合

### 14.1 已按代码落实的两项治理

围绕后续数据量增长与规则持续变更，当前代码已经先落实了两项轻量治理，而不是只停留在建议层：

- 批量写入治理
  - `FactBuildService` 已改为按固定批次执行 `batch upsert`
  - 当前批次大小为 `200`
  - 目标是减少逐条 `upsert` 带来的数据库往返与事务开销
- 规则领域拆分
  - 原 `IssueFactNormalizationRules` 已退化为薄门面
  - 内部拆为：
    - `IssueLabelRules`
    - `IssueClassificationRules`
    - `IssueSlaRules`
    - `IssueLegacyRules`
    - `IssueRuleSupport`

这套做法更接近阿里常见的轻量 DWD 构建实践：

- 构建时强调增量与批处理
- 规则按领域拆分，但不引入重型规则引擎
- 查询层只消费事实字段，不重复做复杂业务判定
## 2026-04-09 本地样例落地补充

为了让现有两张已实现页面可以直接验收，本方案已补一套可重复执行的本地正式种子数据，脚本为：

- `scripts/seed-local-statistic-board-demo-data.sql`

设计原则：

- 不新增任何临时表
- 只写入当前正式 ODS / 导入 / 配置表
- 写完后通过 `FactBuildService` 重建 `issue_fact` 与 `merge_request_fact`

本轮落地范围：

- 系统测试缺陷汇总：
  - 新增 `801 ~ 809` 号 issue 样例
  - 同时覆盖合法、排除、非法、历史遗留、延期等场景
- 代码走查非法记录：
  - 保留 `101 ~ 103` 合法 MR
  - 新增 `104 ~ 107` 非法 MR
  - 覆盖缺失模块、缺失责任人、缺失外部指标、缺失新增代码行数

验证路径：

1. 执行 `seed-local-statistic-board-demo-data.sql`
2. 调用：
   - `POST /api/facts/rebuild?scope=issue&full=true`
   - `POST /api/facts/rebuild?scope=merge-request&full=true`
3. 验证：
   - `GET /api/statistic-boards/system-test-defect-summary`
   - `GET /api/code-review/illegal-records`

已知边界：

- 当前 `system-test-defect-summary` 仍然只过滤 `is_excluded`，不额外过滤 `is_illegal`
- 因此系统测试样例里的非法 issue 会进入汇总统计，这属于当前页面规则边界，不是种子脚本异常
