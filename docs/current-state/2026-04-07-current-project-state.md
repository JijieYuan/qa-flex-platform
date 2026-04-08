# 数据采集平台当前项目现状说明

更新时间：2026-04-07  
仓库路径：`D:\projects\data_collection_platform`  
当前分支：`main`  
当前提交：`33afb7a807822efb5bb9a8ca3fb77f8536b87d1a`  
工作区状态：存在未跟踪文档文件 `docs/current-state/2026-04-07-current-project-state.md`

> 本文档只记录当前项目已实现内容、当前行为、当前接口和当前限制，不包含后续方案设计。

## 1. 项目定位

当前项目是一个以 **GitLab 数据镜像同步** 为基础、同时承载 **统计分析、代码走查、表单采集、数据库浏览** 的单体平台。

项目当前的核心目标已经落地到代码中的部分包括：

- GitLab 源数据同步到本地镜像库
- 基于镜像库提供统计表和记录表
- 提供独立代码走查表单页
- 提供平台数据库浏览与 `collect_form_records` 专属编辑能力

当前项目尚未实现完整的“规则配置中心”“规则版本管理”等独立模块，但已经补充了**规则说明与 Flow 解释能力的一期实现**：当前 `mirror-table-overview`、`system-test-defect-summary`、`code-review-illegal-records` 均支持只读规则说明、过滤/判定 Flow 展示和内置版本号，这些内容仍主要体现在后端服务代码中。

## 2. 技术栈与启动方式

### 2.1 后端

- Java 21
- Spring Boot
- MyBatis-Plus
- JdbcTemplate
- PostgreSQL

主要启动脚本：

- `backend/run-backend.ps1`

当前脚本内容会：

- 设置 `JAVA_HOME`
- 设置 `MAVEN_HOME`
- 使用 `spring-boot:run`
- 关闭 `spring.devtools.restart`

后端默认访问地址：

- `http://localhost:18080/`

### 2.2 前端

- Vue 3
- Vue Router（Hash 模式）
- Element Plus
- TypeScript

主要启动脚本：

- `frontend/run-frontend.ps1`

前端默认访问地址：

- `http://localhost:18181/`

### 2.3 本地工具链

当前仓库 `tools/` 下存在：

- `tools/jdk/`
- `tools/maven/apache-maven-3.9.9/`
- `tools/apache-maven-3.9.6/`

其中：

- 启动脚本当前默认使用 `tools/maven/apache-maven-3.9.9`
- 本地也额外存在 `apache-maven-3.9.6` 目录，可用于手工编译验证

## 3. 项目目录结构

当前仓库主结构：

- `backend/`：Spring Boot 后端
- `frontend/`：Vue 前端
- `tools/`：JDK / Maven
- `docs/`：项目文档

当前 `docs/` 下已有：

- `docs/project-progress.md`
- `docs/plans/`
- 本文档所在目录 `docs/current-state/`

## 4. 当前命名与入口现状

### 4.1 浏览器标题

前端 `frontend/index.html` 当前标题为：

- `Data Collection Platform`

### 4.2 README 标题

仓库 `README.md` 当前标题仍为：

- `QA Flex Platform`

也就是说，当前存在“README 标题”和“浏览器标题”不一致的现状。

### 4.3 前端壳结构

`frontend/src/App.vue` 当前实现了两套壳：

- 平台壳：顶部模块导航 + 左侧菜单 + 内容区
- 独立页壳：用于 `/#/external/...` 这类 standalone 页面

## 5. 前端模块与页面现状

前端模块定义在：

- `frontend/src/navigation.ts`

当前模块如下：

- 统计分析
- 评审数据
- 代码走查
- 集成测试
- 议题统计
- 客户问题
- 系统设置

其中已经接正式页面的模块如下。

### 5.1 统计分析

页面：

- `/#/quality-board/home`

当前实际承载：

- `GitLab 镜像表基础统计`
- 已接入只读规则说明抽屉

对应后端统计板：

- `mirror-table-overview`

### 5.2 代码走查

页面：

- `/#/code-review/illegal-records`

当前实际承载：

- `代码走查非法记录`
- 已接入只读规则说明抽屉

另外有一个独立表单入口：

- `/#/external/code-review-form`

当前独立表单允许的 query 参数仅有：

- `gitlabBaseUrl`
- `projectId`
- `mrIid`

### 5.3 议题统计

页面：

- `/#/question-metrics/home`

当前实际承载：

- `系统测试缺陷汇总`
- 已接入只读规则说明抽屉

对应后端统计板：

- `system-test-defect-summary`

### 5.4 系统设置

已接页面：

- `/#/system-settings/mirror-settings`
- `/#/system-settings/database-browser`

### 5.5 当前仍为占位页面的模块

以下模块当前仍使用 `ModulePlaceholderView.vue`：

- 评审数据
- 集成测试
- 客户问题
- 系统设置 / 模块管理

## 6. 前端表格抽象现状

### 6.1 统计表抽象

核心组件：

- `frontend/src/components/base/BaseStatisticTable.vue`

当前能力：

- 多级表头
- 列拖拽
- 排序
- 分页
- 设置抽屉
- 详情下钻

当前统计页容器：

- `frontend/src/components/StatisticBoardView.vue`

当前该容器已包含：

- 条件构建器
- 查询 / 重置 / 刷新 / 导出 / 设置
- “最近同步”轻量展示
- 明细弹窗
- 规则说明抽屉（当前在所有正式统计板上展示，代码走查非法记录页也提供独立规则说明抽屉）

### 6.2 记录表抽象

核心组件：

- `frontend/src/components/base/BaseRecordTable.vue`

当前能力：

- 主筛选区
- 高级筛选区
- 条件标签回显
- 服务端分页
- 排序
- 工具栏插槽
- 行操作插槽
- 支持 `link` / `tags` / `number` / `datetime` 等列类型

当前实际使用该抽象的页面：

- `代码走查非法记录`

### 6.3 最近同步组件

通用组件：

- `frontend/src/components/realtime/SyncMetaBadge.vue`

当前已接入：

- `StatisticBoardView.vue`
- `CodeReviewIllegalRecordsView.vue`
- `DatabaseBrowserView.vue`

该组件当前只负责展示：

- `最近同步`

不负责刷新逻辑。

## 7. 后端模块与职责现状

### 7.1 镜像同步链路

核心服务：

- `GitlabMirrorSyncService`
- `GitlabWebhookService`
- `GitlabCompensationScheduler`
- `GitlabSyncTaskService`
- `GitlabSyncLogService`
- `GitlabWebhookRegistrationService`

当前已具备的能力：

- 保存同步配置
- 测试外部 GitLab PostgreSQL 连接
- 全量同步
- 增量同步
- 补偿同步
- Webhook 接收
- Webhook 注册状态查看
- 任务排队 / 去重 / 取消
- 镜像数据清理

镜像同步当前采用：

- Webhook
- 手工全量 / 增量
- 定时补偿

### 7.2 统计板服务

当前注册到 `StatisticBoardRegistry` 的统计板只有两张：

- `mirror-table-overview`
- `system-test-defect-summary`

对应服务：

- `MirrorTableOverviewBoardService`
- `SystemTestDefectSummaryBoardService`

基础抽象：

- `AbstractStatisticBoardService`

### 7.3 代码走查记录服务

核心服务：

- `CodeReviewIllegalRecordService`

当前职责：

- 查询代码走查非法记录列表
- 计算筛选项
- 生成非法原因
- 输出代码走查非法记录规则说明与判定 Flow
- 生成 MR 链接
- 返回页面级最近同步状态
- 保留刷新接口能力

### 7.4 数据库浏览服务

核心服务：

- `DatabaseBrowserService`

当前职责：

- 管理数据库浏览白名单
- 动态发现已注册镜像表
- 输出表字段元数据
- 安全执行分页、排序、关键字搜索
- 为 `collect_form_records` 提供专属编辑入口

### 7.5 表单采集服务

核心服务：

- `CollectFormService`

当前职责：

- 按业务上下文查询表单
- 保存 / 更新 `collect_form_records`
- 逻辑作废
- 构造通知载荷

## 8. 后端控制器与当前接口

### 8.1 GitLab 镜像同步

控制器：

- `GitlabSyncController`

当前接口：

- `GET /api/gitlab-sync/status`
- `GET /api/gitlab-sync/webhook-registration-status`
- `GET /api/gitlab-sync/whitelist-options`
- `PUT /api/gitlab-sync/config`
- `POST /api/gitlab-sync/test-connection`
- `POST /api/gitlab-sync/full-sync`
- `POST /api/gitlab-sync/incremental-sync`
- `POST /api/gitlab-sync/register-webhook`
- `POST /api/gitlab-sync/cancel`
- `POST /api/gitlab-sync/purge`
- `POST /api/gitlab-sync/webhook`

### 8.2 统计板

控制器：

- `StatisticBoardController`

当前接口：

- `GET /api/statistic-boards/{boardKey}`
- `GET /api/statistic-boards/{boardKey}/details`
- `GET /api/statistic-boards/{boardKey}/rule-explanation`
- `GET /api/statistic-boards/{boardKey}/export`
- `GET /api/statistic-boards/{boardKey}/status`
- `POST /api/statistic-boards/{boardKey}/refresh`

当前说明：

- 页面当前主要使用查询、详情、规则说明、导出、状态
- `refresh` 接口后端仍保留，但前端统计页当前的“刷新”按钮只是重新加载当前表数据，不直接调用该接口
- `rule-explanation` 接口当前按统计板能力返回只读规则说明；当前正式统计板 `mirror-table-overview` 与 `system-test-defect-summary` 均已实现对应支持接口

### 8.3 代码走查非法记录

控制器：

- `CodeReviewController`

当前接口：

- `GET /api/code-review/illegal-records`
- `GET /api/code-review/illegal-records/filter-options`
- `GET /api/code-review/illegal-records/rule-explanation`
- `GET /api/code-review/illegal-records/status`
- `POST /api/code-review/illegal-records/refresh`

当前说明：

- 页面当前使用列表、筛选项、规则说明、状态
- 页面当前不展示“刷新最新数据”按钮
- `refresh` 接口后端仍保留

### 8.4 数据库浏览

控制器：

- `DatabaseBrowserController`

当前接口：

- `GET /api/database-browser/tables`
- `GET /api/database-browser/rows`

### 8.5 采集表单

控制器：

- `CollectFormController`

当前接口：

- `GET /api/collect-forms/detail`
- `GET /api/collect-forms/notification-payload`
- `POST /api/collect-forms/save`
- `POST /api/collect-forms/delete`
- `POST /api/collect-forms/update-record`

## 9. 数据表现状

当前 `schema.sql` 中定义的核心平台表如下：

- `gitlab_sync_configs`
- `gitlab_sync_logs`
- `gitlab_sync_tasks`
- `gitlab_webhook_events`
- `gitlab_mirror_records`
- `collect_form_records`
- `sys_table_registry`

### 9.1 collect_form_records

当前定位：

- 平台正式业务采集表

主要字段：

- `gitlab_base_url`
- `project_id`
- `request_iid`
- `resource_type`
- `resource_id`
- `template_code`
- `form_title`
- `reviewer`
- `review_duration_minutes`
- `specification_score`
- `logic_score`
- `performance_score`
- `design_score`
- `other_score`
- `remark`
- `deleted`
- `created_at`
- `updated_at`

当前唯一约束：

- `(gitlab_base_url, project_id, resource_type, resource_id, template_code)`

### 9.2 镜像表

实际镜像业务表由：

- `sys_table_registry`

登记，并以：

- `ods_gitlab_*`

形式存在于数据库中。

## 10. 关键业务页面当前行为

### 10.1 GitLab 镜像表基础统计

页面：

- `统计分析 > 镜像表基础统计`

后端服务：

- `MirrorTableOverviewBoardService`

当前特点：

- 统计对象为镜像表
- 支持条件构建器筛选
- 支持明细下钻
- 统计口径直接建立在镜像记录上
- 页面右上使用统一的“最近同步”展示
- 支持“规则说明”只读抽屉，展示内置规则版本、聚合 Flow 步骤和指标口径说明

当前规则说明能力（代码事实）：

- 当前内置规则版本号：`mirror-table-overview@2026-04-07-v1`
- 规则说明接口返回的是镜像聚合口径说明，不涉及业务分类规则配置
- Flow 步骤展示的是“加载镜像汇总 -> 统计源更新时间覆盖 -> 统计最近同步覆盖”的聚合过程

### 10.2 系统测试缺陷汇总

页面：

- `议题统计 > 系统测试缺陷汇总`

后端服务：

- `SystemTestDefectSummaryBoardService`

当前特点：

- 数据来源为本地镜像的 `issues / labels / label_links / users / projects`
- 统计规则当前硬编码在 Java 服务类中
- 支持统计表、详情下钻、导出、最近同步展示
- 支持“规则说明”只读抽屉，展示内置规则版本、过滤 Flow 步骤和指标口径说明
- 当前页面“刷新”行为是重新拉取当前统计结果，不是页面级实时刷新壳

当前过滤步骤（代码事实）：

- 初始范围：当前查询条件下从本地 GitLab Issue 镜像表读取的议题
- 排除携带“功能屏蔽”“已拒绝”“建议”标签的议题
- 排除关闭状态下携带“申请否决”或“需求如此”标签的议题

当前规则说明能力（代码事实）：

- 当前内置规则版本号：`system-test-defect-summary@2026-04-07-v1`
- 规则说明接口返回的是**当前代码真实执行口径**，不是独立配置中心中的外部可编辑规则
- Flow 步骤展示的是“加载镜像议题 -> 排除基础无效标签 -> 排除关闭且无需继续统计的议题”的顺序计数过程

当前分类方式（代码事实）：

- 一级缺陷：基于标题/标签中包含“回退 / rollback / 挂机 / hang / 一级 / level1 / l1”等关键词
- 二级、三级：基于标题/标签中包含“二级 / level2 / l2”“三级 / level3 / l3”等关键词
- P1/P2/P3：基于标题/标签中匹配对应优先级关键词

### 10.3 代码走查非法记录

页面：

- `代码走查 > 代码走查非法记录`

后端服务：

- `CodeReviewIllegalRecordService`

当前特点：

- 数据来源为本地镜像的 MR 相关表
- 页面使用记录表抽象
- 支持主筛选、高级筛选、条件标签回显、排序、分页
- 第一列“合并请求编号”是 GitLab 超链接
- “合并请求内容”为普通文本
- 每行支持右侧抽屉查看详情
- 页面工具区展示统一的“最近同步”
- 页面工具区支持“规则说明”按钮，并通过独立抽屉展示判定 Flow 和指标口径

当前非法原因判定（代码事实）：

- 缺少模块标签
- 缺少标注责任人
- 缺少代码注释比例
- 缺少缺陷数量
- 缺少新增代码行数

当前字段来源说明：

- `moduleName` 当前取 MR 标签数组中的第一个标签
- `commentRate` 当前 SQL 中仍为 `null`
- `defectCount` 当前 SQL 中仍为 `null`
- `addedLines` 来源于 `ods_gitlab_merge_request_metrics`

当前规则说明能力（代码事实）：

- 当前内置规则版本号：`code-review-illegal-records@2026-04-07-v1`
- 规则说明接口返回的是当前后端真实判定逻辑，不是外部配置中心中的可编辑规则
- Flow 步骤展示的是“加载 MR 镜像 -> 检查模块标签 -> 检查责任人 -> 检查外部指标”的判定过程

因此当前这张表更接近：

- 已接通真实镜像链路的业务明细页

但还不是外部指标完全接入后的最终业务态。

### 10.4 独立代码走查表单

页面：

- `/#/external/code-review-form`

前端实现：

- `CollectFormView.vue`

当前特点：

- 独立 standalone 页面，不使用平台壳
- 只接受：
  - `gitlabBaseUrl`
  - `projectId`
  - `mrIid`
- 前端固定把其映射为：
  - `resourceType = merge_request`
  - `resourceId = mrIid`
  - `templateCode = code_review`

当前页面能力：

- 加载同一上下文下的表单记录
- 保存
- 作废
- 重置页面输入

当前“作废”语义：

- 逻辑作废，不是物理删除
- 对应 `collect_form_records.deleted = true`
- 作废后该记录仍保留在数据库中
- 表单详情查询当前只返回 `deleted = false` 的记录，因此作废后页面会回到空白/未保存状态

当前“重置”语义：

- 代码走查表单页中的重置：恢复到最近一次保存值，或恢复为空白模板
- 不是数据库回滚

### 10.5 数据库浏览

页面：

- `系统设置 > 数据库查看`

后端服务：

- `DatabaseBrowserService`

当前特点：

- 只展示白名单中的系统表和已注册镜像表
- 支持搜索、排序、分页
- 表头工具区展示统一的“最近同步”
- `collect_form_records` 支持专属编辑弹窗

当前“重置”语义：

- 仅重置列表搜索和排序条件
- 不修改数据库中的真实数据

## 11. 最近同步与近实时能力现状

### 11.1 前端现状

当前前端已经移除了“近实时工作区壳”，不再在页面级挂一层统一刷新状态条。

当前页面统一保留的是：

- `最近同步`

并通过 `SyncMetaBadge.vue` 展示。

### 11.2 后端现状

后端仍然保留：

- `RealtimeWorkspaceService`
- 统计板 `status / refresh` 接口
- 代码走查 `status / refresh` 接口

也就是说：

- 页面级近实时刷新壳已移除
- 后端状态/刷新能力仍存在

这是一项当前代码事实。

## 12. 当前仍然是占位或未完成的内容

以下内容当前尚未完成到业务最终态：

- 客户问题模块：前端仍为占位页
- 集成测试模块：前端仍为占位页
- 评审数据模块：前端仍为占位页
- 模块管理页：前端仍为占位页
- 规则配置中心：未实现
- 规则 flow 可视化：已完成一期，当前 `mirror-table-overview`、`system-test-defect-summary`、`code-review-illegal-records` 均支持只读规则说明与 Flow 展示
- 规则说明可读性优化：统计页（`mirror-table-overview`、`system-test-defect-summary`）与 `code-review-illegal-records` 均已开始改造成面向 QA 的“先看结论 / 哪些会被排除 / 数据如何一步步变化 / 最后这些数字怎么算”卡片式展示；其中代码走查页已去掉拥挤的指标表格式说明
- 规则版本管理：未实现完整管理能力；当前仅以上三个页面内置只读版本号，尚无统一发布、回滚、审批与审计能力
- 代码走查外部指标正式接入：未完成真实采集链路；但后端已预留正式导入表 `code_review_external_metrics`，`code-review-illegal-records` 会优先读取其中的 `commentRate` / `defectCount`
- 本地 GitLab 合法测试样例：已补充一批可重复执行的种子数据脚本 `scripts/seed-local-gitlab-valid-code-review-data.sql`
  - 当前脚本会向本地库补充 3 条已合并 MR、对应 reviewer / assignee / 模块标签 / metrics / `collect_form_records`
  - 当前脚本还会向 `code_review_external_metrics` 补充 3 条导入指标样例，用于本地验证“CC/DGM 注释率 + 代码走查缺陷数”链路
  - 当前脚本用于验证“基础镜像链路、代码走查页面、采集表单联动数据”是否可正常读取
  - 这些外部指标当前仍是本地导入样例，不代表真实采集链路已经完成

## 13. 当前项目的代码事实总结

截至本文档更新时间，当前项目已经实现的核心能力可以概括为：

- GitLab 镜像同步主链路已存在
- 本地镜像驱动的统计页已存在两张：
  - 镜像表基础统计
  - 系统测试缺陷汇总
- 本地镜像驱动的统计页与记录页已补充一期规则解释能力：
  - 镜像表基础统计：规则说明抽屉、聚合 Flow、内置规则版本号
  - 系统测试缺陷汇总：规则说明抽屉、过滤 Flow、内置规则版本号
  - 代码走查非法记录：规则说明抽屉、判定 Flow、内置规则版本号
- 统计页的规则说明抽屉已开始做 QA 友好化改造：
  - 已完成：统计页默认先展示“结论、排除规则、数量变化、保留比例、公式解释”
  - 已完成：代码走查非法记录页已切换到同一套 QA 友好展示，并将“当前为空/过渡实现”等研发现状文案从用户说明中移除
- 本地镜像驱动的记录页已存在一张：
  - 代码走查非法记录
- 独立采集表单页已存在：
  - 代码走查表
- 本地测试数据已补充一批可复用合法样例：
  - 脚本：`scripts/seed-local-gitlab-valid-code-review-data.sql`
  - 已写入 MR：`iid=101/102/103`
  - 已写入模块标签：`支付中心 / 订单服务 / 报表平台`
  - 已写入代码走查表单记录：`collect_form_records.request_iid = 101/102/103`
  - 已写入导入指标：`code_review_external_metrics.merge_request_iid = 101/102/103`
- 代码走查非法记录页当前已修正两项展示/查询口径：
  - 列表默认只展示 `illegalTypes` 非空的记录，不再把合法 MR 混入“非法数据”表
  - `commentRate` 前端按后端返回的百分数字面值直接展示，不再重复乘以 `100`
- 数据库浏览页已存在，并支持 `collect_form_records` 编辑
- 页面“最近同步”展示已统一为同一组件

当前项目尚未实现的核心能力主要集中在：

- 规则配置化
- 规则解释化的平台化与配置化
- 审计与对账能力平台化

但这些内容当前尚未成为独立模块或配置中心，仍主要分散在具体服务类中；当前实现的是只读规则说明，不包含统一规则中心、可编辑版本管理、审计留痕与对账平台化能力。

## 14. 老平台数据来源映射评估

基于当前项目已经落地的架构，老平台“同级指标多数不是独立表，而是基于少数原始事实表实时聚合”的思路，整体上是可以采纳的；但需要按当前项目的 `ODS 镜像表 + 平台正式业务表 + 统计/记录服务` 结构做一次收敛，不能直接照搬老平台的类和表命名。

### 14.1 可以直接采纳的思路

- 议题类指标不必为“一级缺陷 / P1 / 延期 / 已修复”等每个指标各建一张表
  - 当前项目已经有 `ods_gitlab_issues`、`ods_gitlab_labels`、`ods_gitlab_label_links` 等镜像表
  - `system-test-defect-summary` 也已经在按同一批 Issue 源数据做派生聚合
  - 这与老平台 `spider_issue_data / project_issue_info` 作为“议题事实表”的思想一致
- 代码走查类指标不必为“注释率 / 缺陷数 / review 时长 / 缺陷密度”等每项各建一张表
  - 当前项目已经有 `ods_gitlab_merge_requests`、`ods_gitlab_merge_request_metrics`、`ods_gitlab_merge_request_reviewers`、`ods_gitlab_merge_request_assignees`
  - 代码走查页面天然适合围绕 “一张 MR 事实主表 + 若干补充指标表” 来实现
- 外部结果单独落正式表，这个思路可以保留
  - 老平台里 CC 注释率走 `annotation_rate_info`
  - 当前项目已经按这个方向落了正式表 `code_review_external_metrics`
  - 这说明“主事实来自 GitLab，补充指标来自外部导入”这条路与当前架构兼容
- 运行时按规则聚合，而不是把所有统计结果预先写死落表
  - 当前统计板和记录页都是服务端现算
  - 对用户少、规则变化快、开发人力紧张的场景，这个方向比“预聚合几十张结果表”更合适

### 14.2 需要修改后再采纳的部分

- 老平台的“解析逻辑直接塞进采集服务”不能原样照搬
  - 当前项目更适合分三层：
  - `ods_gitlab_*`：只保留原始镜像
  - 平台正式业务表：保存补充采集结果、人工录入结果、外部回传结果
  - 统计服务：负责聚合、判定、展示
  - 不建议把 label 解析、评论模板解析、延期判定、缺陷归类全部重新塞回同步链路里
- 老平台“一张大事实表承载大量解析后字段”的方式，当前项目要做轻量化改造
  - 当前项目没有 `spider_issue_data` / `spider_crowncad_data` 这种重加工总表
  - 如果直接补一张超宽总表，后续字段变动和来源追踪会很痛苦
  - 更适合拆成：
  - GitLab 原始镜像表
  - 议题派生事实表
  - MR 派生事实表
  - 外部指标导入表
  - 人工采集表单表
- 老平台“评论模板解析后直接覆盖业务字段”的做法需要改成“可追溯导入”
  - 当前项目后续如果接 `issue notes` / `MR notes` 解析，建议保留 `raw_payload`、来源类型、导入时间
  - 这样规则错了时能追来源，也方便 QA 对账
- 注释率外部回调虽然要采纳，但存储层建议统一进 PostgreSQL 正式表
  - 当前项目现有主库就是 PostgreSQL
  - 不建议为了复刻老平台再引入 Mongo 专门存一份注释率
  - 更适合把外部工具结果统一导入 `code_review_external_metrics` 或后续同类正式表

### 14.3 当前最值得落地的正式表模型

- `ods_gitlab_*`
  - 继续作为 GitLab 原始镜像层，不做业务语义污染
- `collect_form_records`
  - 继续承载代码走查表等人工采集结果
- `code_review_external_metrics`
  - 继续承载 CC / DGM / Sonar / MR 机器人等外部回传或解析出的补充指标
- 已新增“正式业务事实表”，按业务域建，不按指标建
  - `issue_fact`
  - `merge_request_fact`
  - 这两张表当前已进入 `schema.sql`，并已补充对应实体、Mapper、数据库浏览入口
  - 已补充 `FactBuildService` 与 `/api/facts/rebuild` 手动构建入口
  - `system-test-defect-summary` 与 `code-review-illegal-records` 刷新时会尝试增量构建对应事实表
  - 它们表示“正式业务事实记录”，不是“某个单独指标结果表”

### 14.4 议题类数据建议

- 可以采纳
  - 基于同一批 Issue 原始数据实时算一级/二级/三级、P1/P2/P3、延期、修复率
  - labels 仍然可以作为重要来源
  - issue 评论和 label event 仍然可以作为补充来源
- 需要修改
  - 不建议把所有解析结果直接写回镜像表
  - 建议未来新增 `issue_fact`
  - 其中保存：
  - `severity_level`
  - `urgency`
  - `testing_phase`
  - `bug_status`
  - `delay_issue`
  - `delay_cause`
  - `module_name`
  - `cause`
  - `fix_user`
  - `fix_label_time`
  - `source_summary`
  - `raw_payload`
- 这样做的好处
  - 统计口径统一
  - 可追来源
  - 不影响镜像同步
  - 后续规则配置化时更容易做

### 14.5 代码走查类数据建议

- 可以采纳
  - 以 MR 为核心事实对象
  - 注释率、缺陷数、review 时长、是否完成走查等围绕同一条 MR 聚合
  - 外部工具结果单独导入，再与 MR 关联
- 需要修改
  - 当前项目不适合复刻一张老式 `spider_crowncad_data` 大总表后把所有逻辑都堆进去
  - 更适合维持：
  - `ods_gitlab_merge_requests*` 作为主源
  - `collect_form_records` 作为人工表单补充
  - `code_review_external_metrics` 作为外部指标补充
  - 必要时再新增 `merge_request_fact` 统一做已归一化字段
- 当前优先级最高的接入项
  - 从 `ods_gitlab_notes` 解析 DGM / MR 评论里的走查结论
  - 把 Sonar / 机器人 / 外部工具结果导入 `code_review_external_metrics`
  - 明确 `commentRate` / `defectCount` / `reviewDuration` / `scanStatus` 的统一字段口径

### 14.6 最终结论

- 可直接采纳的核心原则
  - “少数原始事实表 + 运行时规则聚合”，这个方向适合当前项目
  - “外部指标独立导入正式表”，这个方向也适合当前项目
- 必须调整的地方
  - 不能把老平台那种“采集逻辑、解析逻辑、统计逻辑全堆在一层服务里”的实现方式直接搬过来
  - 不能为每个指标单独建表，但也不建议补一张无边界的超宽总表
- 当前项目更合适的落地方向
  - 继续保留 `ODS 镜像层`
  - 补 `业务事实层`
  - 用 `统计服务/记录服务` 负责规则解释、过滤和聚合
  - 外部系统、机器人、评论解析结果统一导入 PostgreSQL 正式表

## 15. 当前架构倾向补充：数据与数据表解耦

当前项目后续设计，明确更偏向于“数据语义”和“物理表结构”解耦，而不是让页面指标、业务口径直接绑定某一张表或某几个固定字段。

### 15.1 解耦原则

- 表只承担“存事实”的职责
  - 原始镜像表存原始数据
  - 正式业务表存归一化后的事实或外部导入结果
- 指标、口径、规则不直接写死成“某张表 = 某个业务概念”
  - 例如“一级缺陷”不是一张表
  - “非法代码走查”也不是一张表
  - 它们应是对事实数据按规则解释后的结果
- 页面展示不依赖底层表名
  - 页面应面向“过滤规则、筛选条件、计算公式、结果数量”
  - 不面向 `ods_gitlab_*`、`issue_fact_snapshot` 这类存储细节

### 15.2 对当前项目的实际含义

- `ods_gitlab_*`
  - 继续作为原始来源层
  - 不承载业务定义
- `collect_form_records`
  - 继续作为人工表单事实层
- `code_review_external_metrics`
  - 继续作为外部指标事实层
- `issue_fact` / `merge_request_fact`
  - 当前已作为正式事实表设计落地
  - 只承载“归一化后的事实”
  - 不直接等同于页面指标定义
  - 当前统计服务已接入“fact 优先、ODS 回退”的读取策略

### 15.3 服务层应承担的职责

- 由服务层统一做字段映射
  - 把不同来源字段整理成统一语义字段
- 由服务层统一做规则聚合
  - 例如缺陷等级、延期、修复率、非法类型判定
- 由服务层统一做解释输出
  - 输出给前端看的应该是业务定义，不是底层表结构

### 15.4 这样做的直接收益

- 底层表结构调整时，不会直接冲击页面和规则说明
- 新接入一个数据来源时，只需要补映射和导入，不需要重做整页逻辑
- 规则可以逐步配置化，而不会和某张表强绑定
- 更适合当前“用户少但平台重要、开发人力紧张、需求经常变化”的场景

## 16. 已形成的后续架构方案文档

围绕“数据与数据表解耦”的后续落地方案，已单独整理为：

- `docs/plans/2026-04-08-data-decoupled-statistics-architecture-design.md`

这份方案文档的定位是：

- 面向当前项目的增量改造方案
- 不追求企业级大而全
- 明确采用 `ODS 镜像层 + 业务事实层 + 规则聚合层 + 展示解释层`
- 明确建议后续补充 `issue_fact` 与 `merge_request_fact`
- 明确页面与规则不直接绑定底层表结构
- 已补充对老平台“双库 + 动态数据源切换 + Mongo 注释率结果表”设计的参考结论
- 已明确“注释率等外部来源数据先保留统一导入口，不预设 Mongo / HTTP 回调 / 定时拉取等具体接入方式”
- 已将 `issue_fact` / `merge_request_fact` 及其来源字段骨架正式落入代码
- 已补充“性能瓶颈预设治理方案”到架构方案文档，包括事实构建压力、事实表膨胀、规则聚合查询压力三类主要风险及对应治理思路
- 已补充事实构建链路骨架：
  - `FactBuildService`
  - `FactBuildController`
  - `POST /api/facts/rebuild`
- 已开始按 SSOT 收口：
  - `system-test-defect-summary` 不再在 BoardService 中保留严重程度/优先级的 ODS 关键词判定逻辑
  - 相关判定已改为消费 `issue_fact` 中的归一化字段
  - `code-review-illegal-records` 已改为只读取 `merge_request_fact`
- 已补充增量水位线字段：
  - `issue_fact.ods_updated_at`
  - `merge_request_fact.ods_updated_at`
- 已将议题字段归一化规则集中到单点映射：
  - `IssueFactNormalizationRules`
  - 当前用于统一严重程度、优先级、测试阶段、缺陷分类、延期标记、模块名等字段归一化

## 17. 当前 issue_fact 全量收口状态

围绕 `系统测试缺陷汇总`，当前已经确认后续要统一沉入 `issue_fact` 的完整口径范围如下：

- 严重程度标准化：
  - `P1 = 一级缺陷 / 一级严重`
  - `P2 = 二级缺陷 / 二级严重`
  - `P3 = 三级缺陷 / 三级严重`
  - `SUGGESTION = 建议 / 需求 / 需求如此`
- 公共排除口径：
  - `功能屏蔽 / 已拒绝 / 建议`
  - `申请否决 + Closed`
  - `数据异常 + Closed`
  - `设计如此 + Closed`
- 修复状态：
  - `已修复 / 待合并`
  - `Closed + 未复现`
- 缺陷原因归一化
- 延期原因七大类
- 标题强校验：
  - `回退`
  - `挂机`
  - `其他一级`
- 非法数据断言：
  - 缺级别
  - 缺模块
  - 流程越位
- SLA：
  - 已响应
  - 响应延期
  - 解决时限
  - 解决延期
- 模块全量性：
  - 单 issue 多模块
  - 项目总数去重、模块维度展开
- 历史遗留：
  - `is_legacy`

当前真实实现状态：

- 已落地：
  - `issue_fact` 正式表骨架
  - `FactBuildService`
  - `IssueFactNormalizationRules`
  - `system-test-defect-summary` fact 优先读取
- 已预留到 `schema.sql` 但尚未完整贯通：
  - `severity_alias`
  - `reason_category`
  - `is_excluded`
  - `exclusion_reason`
  - `is_fixed`
  - `delay_reason`
  - `is_regression`
  - `is_crash`
  - `is_illegal`
  - `illegal_reason`
  - `has_response`
  - `response_overdue`
  - `resolve_sla_days`
- 仍待本轮继续补齐：
  - `IssueFact` 实体与 `IssueFactMapper` 对上述字段的完整支持
  - `FactBuildService` 从 `ods_gitlab_notes`、标题关键词、组合标签状态中构建完整业务字段
  - 多模块字段承接
  - `testing_phase_calendar` 及 `is_legacy`
  - `SystemTestDefectSummaryBoardService` 彻底退化为 fact 聚合取数器

也就是说，当前项目已经进入“事实层收口”阶段，但 `issue_fact` 还没有完成你确认的全量业务口径落地。
## 18. 2026-04-08 issue_fact 实际落地结果

围绕系统测试缺陷统计，`issue_fact` 本轮已经从“方案态”进入“真实可运行”状态，当前本地工程的实际情况如下：

- 已落地正式表：
  - `issue_fact`
  - `testing_phase_calendar`
- 已落地构建链路：
  - `FactBuildService`
  - `POST /api/facts/rebuild?scope=issue&full=true|false`
- 已落地单点归一化规则：
  - `IssueFactNormalizationRules`
- 已落地事实优先查询：
  - `SystemTestDefectSummaryBoardService` 已改为优先读取 `issue_fact`
  - 一级/二级/三级、回退/挂机/其他一级、修复状态、非法标记、历史遗留等统计不再在 BoardService 中重复写一套口径

本轮已正式沉入 `issue_fact` 的业务字段包括：

- 严重程度标准化：`P1 / P2 / P3 / SUGGESTION`
- 公共排除：`is_excluded / exclusion_reason`
- 修复状态：`is_fixed`
- 缺陷原因归一化：`reason_category`
- 延期归一化：`delay_issue / delay_reason / delay_cause`
- 特殊一级分类：`is_regression / is_crash / is_level1_other`
- 非法数据断言：`is_illegal / illegal_reason`
- 响应与解决时效：`has_response / is_response_delayed / resolve_sla_days / resolve_deadline_at / is_resolve_delayed`
- 模块全量字段：`primary_module_name / module_names`
- 历史遗留：`is_legacy`
- 来源追溯：`source_system / source_instance / ingest_channel / source_summary / raw_payload`

本地运行验证结果：

- 已成功执行：`POST /api/facts/rebuild?scope=issue&full=true`
- 本地库已成功全量构建 `issue_fact = 382`
- 规则说明接口已返回新版说明：
  - `system-test-defect-summary@2026-04-08-v2`

当前真实数据边界：

- 当前本地 `ods_gitlab_issues` 中绝大部分记录仍是测试/压测生成标签，不符合真实项目标签口径
- 因此当前构建结果里：
  - `severity_level` 仍大量为空
  - `is_illegal = true` 数量较高，主要原因是 `缺失严重程度`
  - `is_excluded / is_fixed / has_response` 等字段在当前测试数据集上命中较少
- 这说明 `issue_fact` 构建链路已打通，但“真实业务口径是否充分命中”仍取决于后续导入的真实项目标签、评论模板和阶段日历配置

当前结论：

- `issue_fact` 全量口径框架、字段设计、构建逻辑、接口触发链路均已落地
- 当前剩余重点已经不再是“有没有这套架构”，而是继续补真实项目数据和阶段日历，让归一化字段在真实数据集上充分命中

### 18.1 当前已补的性能与维护性收口

为了避免后续随着事实字段增多而让构建链路退化，本轮又补了两项基础治理：

- `FactBuildService` 已从“逐条 upsert”改为“分批 batch upsert”
  - 当前批次大小：`200`
  - `issue_fact` 与 `merge_request_fact` 都已改为批量写入
- `IssueFactNormalizationRules` 已从单一大类拆分为领域规则类
  - `IssueLabelRules`
  - `IssueClassificationRules`
  - `IssueSlaRules`
  - `IssueLegacyRules`
  - `IssueRuleSupport`

当前收益：

- 后续 `rebuild` 时数据库往返次数明显减少，更适合持续增量构建
- 规则扩展时不再继续向一个超大类堆逻辑，后续补充真实项目别名时更安全
