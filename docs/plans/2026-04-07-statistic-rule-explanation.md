# Statistic Rule Explanation Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 为现有统计板补充“规则说明 + Flow 过滤过程”能力，并先在系统测试缺陷汇总板上落地可用的一期闭环。

**Architecture:** 保持现有 Spring Boot 单体和 Vue 统计板壳不变，在后端新增统计板规则解释接口与可选支持接口，在前端统计页新增规则说明入口和 Flow 展示抽屉。第一期只接入 `system-test-defect-summary`，但后端接口和前端容器按可复用方式设计，后续可平滑扩展到其他统计板。

**Tech Stack:** Java 21, Spring Boot 3.5, JdbcTemplate, Vue 3, TypeScript, Element Plus, JUnit 5, Spring Boot Test

---

### Task 1: 定义规则说明数据结构与控制器测试

**Files:**
- Create: `backend/src/main/java/com/data/collection/platform/entity/statistics/StatisticBoardRuleExplanationResponse.java`
- Create: `backend/src/main/java/com/data/collection/platform/entity/statistics/StatisticRuleMetricDefinition.java`
- Create: `backend/src/main/java/com/data/collection/platform/entity/statistics/StatisticRuleFlowStep.java`
- Create: `backend/src/main/java/com/data/collection/platform/entity/statistics/StatisticRuleFlowStepSample.java`
- Create: `backend/src/main/java/com/data/collection/platform/service/statistics/RuleExplainableStatisticBoardSupport.java`
- Modify: `backend/src/main/java/com/data/collection/platform/controller/StatisticBoardController.java`
- Modify: `backend/src/test/java/com/data/collection/platform/controller/StatisticBoardControllerTest.java`

**Step 1: Write the failing tests**

- 为 `StatisticBoardControllerTest` 增加：
  - `shouldLoadSystemTestDefectSummaryRuleExplanation`
  - `shouldReturnUnsupportedRuleExplanationForMirrorTableOverviewBoard`

**Step 2: Run test to verify it fails**

Run:

```powershell
cd d:\projects\data_collection_platform\backend
..\tools\maven\apache-maven-3.9.9\bin\mvn.cmd -Dtest=StatisticBoardControllerTest test
```

Expected: FAIL，因为规则说明接口和返回类型尚未实现。

**Step 3: Write minimal implementation**

- 新增规则说明响应记录类。
- 新增 `RuleExplainableStatisticBoardSupport` 接口。
- 在 `StatisticBoardController` 中增加：
  - `GET /api/statistic-boards/{boardKey}/rule-explanation`

**Step 4: Run test to verify it passes**

Run the same Maven command and confirm controller tests pass.

### Task 2: 为系统测试缺陷汇总板实现规则说明与 Flow

**Files:**
- Modify: `backend/src/main/java/com/data/collection/platform/service/statistics/SystemTestDefectSummaryBoardService.java`
- Create: `backend/src/test/java/com/data/collection/platform/service/statistics/SystemTestDefectSummaryRuleExplanationTest.java`

**Step 1: Write the failing tests**

- 为系统测试板新增服务测试，覆盖：
  - 返回规则版本、规则范围、过滤步骤、指标定义
  - Flow 步骤包含过滤前后数量与说明
  - 样例记录内容可读

**Step 2: Run test to verify it fails**

Run:

```powershell
cd d:\projects\data_collection_platform\backend
..\tools\maven\apache-maven-3.9.9\bin\mvn.cmd -Dtest=SystemTestDefectSummaryRuleExplanationTest test
```

Expected: FAIL，因为服务还未实现说明接口。

**Step 3: Write minimal implementation**

- 让 `SystemTestDefectSummaryBoardService` 实现 `RuleExplainableStatisticBoardSupport`
- 提供：
  - 规则摘要
  - 固定版本号
  - 过滤步骤定义
  - 指标定义
  - 基于当前镜像数据的 Flow 计数

**Step 4: Run test to verify it passes**

Run the same Maven command and confirm tests pass.

### Task 3: 前端接入规则说明入口与抽屉

**Files:**
- Modify: `frontend/src/api.ts`
- Modify: `frontend/src/components/StatisticBoardView.vue`

**Step 1: Write the failing behavior check**

- 先补类型与 API 调用，再运行前端构建，验证因缺少实现或类型不匹配而失败。

**Step 2: Run build to verify it fails**

Run:

```powershell
cd d:\projects\data_collection_platform\frontend
npm run build
```

Expected: FAIL，直到规则说明类型、按钮、抽屉和加载逻辑全部接通。

**Step 3: Write minimal implementation**

- 新增规则说明相关 TS 类型和 API
- 在统计页工具栏增加“规则说明”按钮
- 新增抽屉展示：
  - 规则版本
  - 统计范围
  - 过滤规则说明
  - Flow 步骤数量变化
  - 指标口径说明

**Step 4: Run build to verify it passes**

Run the same build command and confirm it succeeds.

### Task 4: 同步统一文档

**Files:**
- Modify: `docs/current-state/2026-04-07-current-project-state.md`

**Step 1: Update documentation**

- 补充本次实际完成的后端接口、前端能力、现阶段限制。
- 明确“当前只有系统测试缺陷汇总支持规则说明与 Flow 解释”。
- 确保文档可作为当前工程统一正确入口。

**Step 2: Verify documentation matches code**

- 对照控制器、前端页面和实际实现内容逐条核对。

### Task 5: 整体验证

**Files:**
- No code changes expected

**Step 1: Run backend targeted tests**

```powershell
cd d:\projects\data_collection_platform\backend
..\tools\maven\apache-maven-3.9.9\bin\mvn.cmd -Dtest=StatisticBoardControllerTest,SystemTestDefectSummaryRuleExplanationTest test
```

**Step 2: Run frontend build**

```powershell
cd d:\projects\data_collection_platform\frontend
npm run build
```

**Step 3: Review changed files**

```powershell
git -C d:\projects\data_collection_platform diff -- backend/src/main frontend/src docs/current-state
```

**Step 4: Document actual verification evidence**

- 只根据本轮新鲜命令输出说明完成情况，不凭感觉声明成功。
