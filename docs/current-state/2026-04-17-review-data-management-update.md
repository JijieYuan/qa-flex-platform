# 评审数据管理页落地说明

更新时间：2026-04-17  
适用项目：`D:\projects\data_collection_platform`

## 1. 本次落地内容

新版“评审数据管理”页面已经接入正式路由：

- `/#/review-data/home`

本次实现遵循以下边界：

- 页面类型：记录类
- 组件基底：Element Plus
- 页面风格：轻阿里系
- 复用基础：`BaseRecordTable.vue`
- 不复刻老平台 `ReviewBoard.vue` 的深耦合结构
- 不改“代码走查非法记录”页面
- 不改统计表抽象和规则说明模块

## 2. 数据来源

本页当前主数据来自：

- `collect_form_records`

本页当前补充字段来自：

- `merge_request_fact`

这样做的目的，是让“评审数据管理”真正围绕**正式评审记录**展开，而不是围绕镜像事实做反推。

## 3. 后端新增接口

新增控制器：

- `backend/src/main/java/com/data/collection/platform/controller/ReviewDataController.java`

新增接口：

- `GET /api/review-data/records`
- `GET /api/review-data/records/filter-options`

新增服务：

- `backend/src/main/java/com/data/collection/platform/service/ReviewDataRecordService.java`

接口当前支持：

- 列表分页
- 排序
- 项目、代码库、模块、评审人、模板编码、目标分支、记录状态、关键字、合并请求编号、更新时间范围筛选
- 页面摘要数据返回

## 4. 前端页面结构

新增页面：

- `frontend/src/views/ReviewDataManagementView.vue`

新增页面 helper：

- `frontend/src/views/review-data-management.ts`

页面当前结构：

1. 顶部页面说明区
2. 4 个轻量摘要卡
3. 记录表筛选区
4. 记录表格
5. 详情抽屉

页面当前展示字段包括：

- 评审标题
- 项目
- 代码库
- 模块
- 评审人
- 评审时长
- 总分
- 注释率
- 缺陷数
- 新增代码行数
- 记录状态
- 更新时间

## 5. 当前仍然保留的限制

- 目前没有复刻老平台“主记录展开问题子表”的模式
- 目前没有做批量编辑、多弹窗簇和复杂导出能力
- 目前详情抽屉只做查看，不做页内编辑
- 当前页面仍然是“轻量记录管理页”，不是“复合工作台”

## 6. 验证结果

后端：

- `mvn -q "-Dtest=ReviewDataControllerTest" test`
- `mvn -q -DskipTests compile`

前端：

- `npm test`
- `npm run build`

均已通过。
