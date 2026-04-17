# 评审数据管理种子数据与交互补强 Plan

> For Claude: 该计划用于记录本轮小范围增强，聚焦“可见数据、可刷新、会使用”，不改评审域建模边界。

**Goal:** 为评审数据管理页补齐本地可视化数据、恢复刷新入口，并在弹窗内加入简洁使用说明。

**Architecture:** 保持现有独立评审域表 `review_records / review_record_experts / review_problem_items` 不变；通过新增本地 seed 脚本提供可重复演示数据；前端仅补刷新动作和弹窗提示，不调整主流程与数据契约。

**Tech Stack:** PostgreSQL SQL seed, Spring Boot existing API, Vue 3, Element Plus

---

## 1. 范围

- 保持评审数据管理页继续使用独立评审域表
- 不切回 `collect_form_records`
- 不改代码走查非法记录页
- 不新增导入能力，本轮只为后续导入方案留出更稳定的体验基础

## 2. 本轮落地项

1. 新增本地种子脚本
   - 文件：`scripts/seed-local-review-data-management-demo.sql`
   - 目标：快速生成主记录、评审专家、评审问题清单

2. 恢复刷新按钮
   - 页面：`frontend/src/views/ReviewDataManagementView.vue`
   - 目标：允许用户在新增/编辑/删改后手动重新拉取列表与筛选项

3. 在对话框里补使用说明
   - 文件：
     - `frontend/src/views/review-data/ReviewRecordFormDialog.vue`
     - `frontend/src/views/review-data/ReviewProblemItemFormDialog.vue`
   - 目标：让 QA 不需要额外培训就能理解“先建主记录，再维护问题清单”

4. 文档同步
   - 更新：
     - `docs/current-state/2026-04-17-review-data-management-update.md`
     - `docs/current-state/2026-04-07-current-project-state.md`

## 3. 验证

- 前端：
  - `npm test`
  - `npm run build`
- 数据：
  - 执行 seed 后，`review_records / review_record_experts / review_problem_items` 均存在演示数据

