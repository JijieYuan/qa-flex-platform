# 评审数据管理页落地说明

更新时间：2026-04-17  
适用项目：`D:\projects\data_collection_platform`

## 1. 本次落地内容

新版“评审数据管理”页面已经接入正式路由：

- `/#/review-data/home`

本次实现采用以下边界：

- 页面类型：记录类
- 组件基底：Element Plus
- 页面风格：轻阿里系
- 复用底座：`frontend/src/components/base/BaseRecordTable.vue`
- 不复刻老平台 `ReviewBoard.vue` 的深耦合结构
- 不改“代码走查非法记录”页面
- 不改统计表抽象和规则说明模块

## 2. 数据模型

本次没有继续沿用 `collect_form_records` 承载评审数据，而是补齐了独立的评审域数据表：

- `review_records`
- `review_record_experts`
- `review_problem_items`

这样做的目的，是让“评审数据管理”围绕正式评审主记录展开，同时支持：

- 主记录筛选、分页、排序
- 评审专家多值维护
- 行展开的“评审问题清单”
- 问题清单的新增、编辑、删除

## 3. 后端接口

控制器：

- `backend/src/main/java/com/data/collection/platform/controller/ReviewDataController.java`

服务：

- `backend/src/main/java/com/data/collection/platform/service/ReviewDataRecordService.java`

当前已提供接口：

- `GET /api/review-data/records`
- `GET /api/review-data/records/filter-options`
- `GET /api/review-data/records/{recordId}`
- `POST /api/review-data/records`
- `PUT /api/review-data/records/{recordId}`
- `DELETE /api/review-data/records/{recordId}`
- `GET /api/review-data/records/{recordId}/problem-items`
- `POST /api/review-data/records/{recordId}/problem-items`
- `PUT /api/review-data/records/{recordId}/problem-items/{itemId}`
- `DELETE /api/review-data/records/{recordId}/problem-items/{itemId}`

## 4. 前端页面结构

主页面：

- `frontend/src/views/ReviewDataManagementView.vue`

页面 helper：

- `frontend/src/views/review-data-management.ts`

新增弹窗：

- `frontend/src/views/review-data/ReviewRecordFormDialog.vue`
- `frontend/src/views/review-data/ReviewProblemItemFormDialog.vue`

新增通用选择器：

- `frontend/src/components/base/SmartSelect.vue`

当前页面结构为：

1. 顶部轻量标题区
2. 摘要卡区
3. 主记录筛选区
4. 主记录表
5. 行展开的评审问题清单
6. 详情抽屉
7. 新增/编辑评审弹窗
8. 新增/编辑问题弹窗

## 5. 本次交互调整

### 5.1 顶部刷新按钮

评审数据管理页已移除顶部“刷新数据”按钮，只保留：

- 高级筛选
- 重置
- 查询
- 新增评审

### 5.2 首字母搜索

平台搜索选择器新增首字母搜索能力，封装在：

- `frontend/src/components/base/SmartSelect.vue`

当前支持：

- 原文匹配
- 英文首字母匹配
- 中文拼音首字母匹配

### 5.3 紧凑型下拉

Element Plus 没有原生的块状紧凑下拉样式，因此本次在 `SmartSelect` 上增加了轻量紧凑模式：

- 默认仍兼容普通下拉
- 可按页面按字段启用 `compact`
- 不破坏原有表格和筛选结构

## 6. 评审问题清单

本次已按“主表 + 行展开子表”的方式实现评审问题清单：

- 子表挂在评审主记录下
- 支持新增问题
- 支持编辑问题
- 支持删除问题
- 支持和主记录同页联动刷新

这部分参考了老平台“评审数据管理”的功能形态，但没有沿用其深耦合代码结构。

## 7. 当前保留边界

- 当前只做评审数据管理页，不扩散到代码走查非法记录页
- 当前不做批量编辑
- 当前不做复杂导出工作台
- 当前不做平台级规则配置中心
- 当前不做企业级超重中台结构

## 8. 验证结果

后端：

- `mvn -q "-Dtest=ReviewDataControllerTest" test`
- `mvn -q -DskipTests compile`

前端：

- `npm test`
- `npm run build`

均已通过。

当前仍保留一个非阻塞项：

- Vite chunk 体积 warning，属于既有体量问题，不是本次改动引入的功能故障
