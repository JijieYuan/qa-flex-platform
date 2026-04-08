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
