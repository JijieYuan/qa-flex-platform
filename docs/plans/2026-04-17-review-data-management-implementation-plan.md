# 评审数据管理页 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 基于现有 `merge_request_fact` 与记录表抽象，落地新版“评审数据管理”记录页第一版。

**Architecture:** 后端新增面向页面的评审数据读模型接口，统一从 `merge_request_fact` 输出列表、筛选项与详情摘要；前端基于 `BaseRecordTable` 实现记录页，并在页面层补充轻量统计摘要与详情抽屉。规则说明模块和旧的代码走查非法记录页不动。

**Tech Stack:** Spring Boot, JdbcTemplate, Vue 3, Element Plus, TypeScript

---

### Task 1: 固化评审数据页边界

**Files:**
- Modify: `D:\projects\data_collection_platform\docs\current-state\2026-04-07-current-project-state.md`
- Modify: `D:\projects\data_collection_platform\frontend\src\navigation.ts`

**Step 1: 明确页面定位**

- 页面类型：记录类
- 数据来源：`merge_request_fact`
- 页面增强：详情抽屉、轻量摘要卡
- 本期不做：子表内维护、多弹窗簇、复杂导出工作台

**Step 2: 同步文档与导航文案**

- 将 `review-data` 从“预留模块”文案收口为正式记录页
- 保持整体风格为轻阿里系 + Element Plus

### Task 2: 实现后端评审数据读模型接口

**Files:**
- Create: `D:\projects\data_collection_platform\backend\src\main\java\com\data\collection\platform\entity\ReviewDataRecordRowResponse.java`
- Create: `D:\projects\data_collection_platform\backend\src\main\java\com\data\collection\platform\entity\ReviewDataRecordListResponse.java`
- Create: `D:\projects\data_collection_platform\backend\src\main\java\com\data\collection\platform\entity\ReviewDataFilterOptionsResponse.java`
- Create: `D:\projects\data_collection_platform\backend\src\main\java\com\data\collection\platform\entity\ReviewDataSummaryResponse.java`
- Create: `D:\projects\data_collection_platform\backend\src\main\java\com\data\collection\platform\service\ReviewDataRecordService.java`
- Create: `D:\projects\data_collection_platform\backend\src\main\java\com\data\collection\platform\controller\ReviewDataController.java`
- Test: `D:\projects\data_collection_platform\backend\src\test\java\com\data\collection\platform\controller\ReviewDataControllerTest.java`

**Step 1: 先写控制器测试**

- 校验列表接口
- 校验筛选项接口
- 校验摘要接口

**Step 2: 用最小实现让测试通过**

- 从 `merge_request_fact` 查询
- 支持项目、仓库、模块、评审状态、评审人、标题关键字、时间范围
- 输出分页、排序与轻量摘要

**Step 3: 补充服务层边界**

- 只做页面读模型，不回写
- 只返回页面确实需要的字段

### Task 3: 实现前端评审数据管理页

**Files:**
- Create: `D:\projects\data_collection_platform\frontend\src\views\ReviewDataManagementView.vue`
- Modify: `D:\projects\data_collection_platform\frontend\src\router.ts`
- Modify: `D:\projects\data_collection_platform\frontend\src\api.ts`
- Test: `D:\projects\data_collection_platform\frontend\src\views\review-data-management.test.ts`

**Step 1: 先接路由**

- 将 `/review-data/home` 从占位页切到正式页面

**Step 2: 复用 `BaseRecordTable`**

- 顶部筛选
- 活动筛选标签
- 记录表格
- 行操作：查看详情

**Step 3: 页面层补充增强**

- 顶部 3 到 4 个轻量摘要卡
- 右侧详情抽屉
- 统一走轻阿里系表达，不做重容器工作台

### Task 4: 验证与文档同步

**Files:**
- Modify: `D:\projects\data_collection_platform\docs\current-state\2026-04-07-current-project-state.md`
- Create: `D:\projects\data_collection_platform\docs\current-state\2026-04-17-review-data-management-update.md`

**Step 1: 后端验证**

Run:

```powershell
& D:\projects\data_collection_platform\tools\maven\apache-maven-3.9.9\bin\mvn.cmd -q "-Dtest=ReviewDataControllerTest" test
```

**Step 2: 前端验证**

Run:

```powershell
cd D:\projects\data_collection_platform\frontend
npm test
npm run build
```

**Step 3: 同步当前状态文档**

- 记录新版评审数据页的入口、数据来源、当前能力和保留边界
