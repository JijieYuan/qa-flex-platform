# 评审数据管理页 Implementation Plan

> For Claude: 本计划已执行完成，当前文件保留为落地边界与实现归档。

**Goal:** 落地新版“评审数据管理”记录页，并支持主记录下的“评审问题清单”子表维护。  
**Architecture:** 以 `BaseRecordTable` 为底座，页面层增加行展开子表、摘要卡、详情抽屉和新增/编辑弹窗；后端补充独立评审域表与 CRUD 接口；选择器统一收口到可复用的 `SmartSelect`。  
**Tech Stack:** Spring Boot, JdbcTemplate, PostgreSQL, Vue 3, Element Plus, TypeScript

---

## 1. 页面定位

- 页面类型：记录类
- 页面入口：`/#/review-data/home`
- 风格：轻阿里系
- 组件基底：Element Plus
- 不复用老平台代码结构，只参考其功能集合

## 2. 数据模型边界

本期采用独立评审域表，不再沿用 `collect_form_records` 承载评审记录：

- `review_records`
- `review_record_experts`
- `review_problem_items`

设计原则：

- 主记录与问题清单分表存储
- UI 上是嵌套子表
- 数据上是独立关联
- 保证可维护、可复用、可扩展

## 3. 后端任务

### Task 1: 主记录列表与筛选

实现：

- `GET /api/review-data/records`
- `GET /api/review-data/records/filter-options`

支持：

- 标题
- 项目名称
- 模块
- 评审负责人
- 评审类型
- 问题状态
- 评审专家
- 分页
- 排序

### Task 2: 主记录 CRUD

实现：

- `POST /api/review-data/records`
- `PUT /api/review-data/records/{recordId}`
- `DELETE /api/review-data/records/{recordId}`

字段范围：

- 项目名称
- 标题
- 模块
- 评审类型
- 评审日期
- 评审负责人
- 评审专家
- 评审规模(页)
- 评审工作产品
- 作者
- 评审版本

### Task 3: 评审问题清单 CRUD

实现：

- `GET /api/review-data/records/{recordId}/problem-items`
- `POST /api/review-data/records/{recordId}/problem-items`
- `PUT /api/review-data/records/{recordId}/problem-items/{itemId}`
- `DELETE /api/review-data/records/{recordId}/problem-items/{itemId}`

字段范围：

- 评审专家
- 评审工作量
- 评审类别
- 在文档中的位置
- 问题类别
- 问题描述
- 建议解决方案
- 责任人
- 不接受理由
- 问题状态

## 4. 前端任务

### Task 1: 记录页主表

页面：

- `frontend/src/views/ReviewDataManagementView.vue`

能力：

- 顶部筛选
- 摘要卡
- 主记录表
- 详情抽屉
- 行操作
- 去掉顶部刷新按钮
- 在筛选动作区加入“新增评审”

### Task 2: 行展开子表

能力：

- 每条评审记录支持展开
- 展开后展示“评审问题清单”
- 子表支持新增、编辑、删除

### Task 3: 通用选择器增强

新增：

- `frontend/src/components/base/SmartSelect.vue`

能力：

- 原文搜索
- 英文首字母搜索
- 中文拼音首字母搜索
- 可选紧凑型下拉面板

### Task 4: 基类无侵入扩展

修改：

- `frontend/src/components/base/BaseRecordTable.vue`

扩展点：

- `primary-actions` 插槽
- 行展开插槽
- 展开行 key 控制
- 继续保持对其他记录页兼容

## 5. 不做的内容

当前不做：

- 企业级重型工作台
- 批量编辑
- 平台级规则配置中心
- 代码走查非法记录页迁移
- 大规模自定义下拉面板系统

## 6. 验证命令

后端：

```powershell
& D:\projects\data_collection_platform\tools\maven\apache-maven-3.9.9\bin\mvn.cmd -q "-Dtest=ReviewDataControllerTest" test
& D:\projects\data_collection_platform\tools\maven\apache-maven-3.9.9\bin\mvn.cmd -q -DskipTests compile
```

前端：

```powershell
cd D:\projects\data_collection_platform\frontend
npm test
npm run build
```

## 7. 当前状态

本计划已完成，对应落地说明见：

- `docs/current-state/2026-04-17-review-data-management-update.md`
