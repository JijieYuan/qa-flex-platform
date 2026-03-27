# GitLab 同步链路 JSON 日志重构设计

## 目标

将数据采集平台后端日志统一切换为基于 `logstash-logback-encoder` 的 JSON 结构化日志体系，替换现有零散、弱结构化的日志输出方式，并围绕 GitLab 同步链路建立“任务上下文 + 动作节点 + 异常堆栈 + 文件持久化”的日志闭环。

## 范围

本轮仅覆盖 `backend` 模块，但要求做到：

- `logstash-logback-encoder` 成为唯一标准日志出口
- 控制台与滚动文件同时输出 JSON
- 所有同步任务相关日志带统一 MDC 上下文
- 清理所有 `System.out.println` / `printStackTrace`
- 关键链路补足动作级日志埋点

## 设计约束

1. 日志字段最少包含：
   - `timestamp`
   - `level`
   - `thread`
   - `logger`
   - `message`
   - `traceId`
   - `taskId`
   - `scope`
   - `gitlabUrl`
   - `taskType`
2. `gitlab_sync_tasks` 仍是任务状态真源，日志不参与状态判定。
3. 异常日志必须保留原始堆栈，不允许吞堆栈或仅记录 `message`。
4. 文件日志必须启用立即刷新和时间+大小滚动策略，以适应断电、网络风暴、节点离线场景。

## 方案

### 1. 统一日志配置

新增 `logback-spring.xml`：

- ConsoleAppender：JSON 输出
- RollingFileAppender：JSON 输出
- `immediateFlush=true`
- `TimeBasedRollingPolicy + SizeAndTimeBasedFNATP`
- 设置合理保留天数与单文件大小

### 2. MDC 上下文统一管理

新增同步日志上下文工具，例如：

- `GitlabSyncLogContext`

职责：

- 生成/注入 `traceId`
- 注入 `taskId`
- 注入 `scope`
- 注入 `gitlabUrl`
- 注入 `taskType`
- 在异步执行、Webhook 接入、任务结束时统一清理

### 3. 同步链路动作日志

在以下关键点输出结构化日志：

- `Task_Start`
- `Webhook_Received`
- `Data_Fetching`
- `Mirror_Writing`
- `Commit_Success`
- `Task_End`

日志必须补充关键属性，例如：

- 表名
- 批次大小
- 补偿窗口分钟数
- 跳过表数
- 重试退避时间
- 取消请求状态

### 4. 异常与防御性日志

对以下异常统一记录：

- Docker / 网络连接异常
- GitLab 源端查询异常
- 数据落库异常
- 中止导入异常
- 状态回收 / 超时任务异常

要求：

- `log.error("...", e)` 保留堆栈
- 标记当前任务、当前表、当前重试次数或补偿窗口

### 5. 非标输出清理

全项目扫描并禁止：

- `System.out.println`
- `printStackTrace`

如需人工辅助输出，一律改用标准 logger。

## 核心文件

- `backend/pom.xml`
- `backend/src/main/resources/logback-spring.xml`
- `backend/src/main/resources/application.yml`
- `backend/src/main/java/com/data/collection/platform/service/GitlabMirrorSyncService.java`
- `backend/src/main/java/com/data/collection/platform/service/GitlabWebhookService.java`
- `backend/src/main/java/com/data/collection/platform/service/GitlabExternalDbService.java`
- `backend/src/main/java/com/data/collection/platform/service/GitlabCompensationScheduler.java`
- `backend/src/main/java/com/data/collection/platform/service/GitlabSyncTaskService.java`
- `backend/src/main/java/com/data/collection/platform/controller/GitlabSyncController.java`
- `backend/src/main/java/com/data/collection/platform/common/exception/GlobalExceptionHandler.java`

## 验证方式

1. 编译通过
2. 现有同步相关单测/集成测试通过
3. 本地真实 GitLab 集成测试通过
4. 运行应用后检查：
   - 控制台 JSON
   - 文件 JSON
   - MDC 字段齐全
   - 关键动作日志存在

## 交付物

- 新版 `logback-spring.xml`
- 同步成功 / 网络失败 / 补偿场景的 JSON 日志示例
- 代码中统一的日志上下文管理与关键埋点
