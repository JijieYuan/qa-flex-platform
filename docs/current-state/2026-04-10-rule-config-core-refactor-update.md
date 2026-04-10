# 2026-04-10 规则配置核心层重构更新

## 本轮目标

把代码走查非法记录页面里原来的“线性句式规则 Demo”升级成“树结构逻辑规则 Demo”，同时保留页面现有抽屉、说明区和预览区，不推翻页面壳层。

## 已完成内容

### 1. 通用核心层升级为树结构

新增或重写：

- `frontend/src/views/rule-config-core.ts`
- `frontend/src/views/useRuleConfigState.ts`

当前职责拆分为：

- `rule-config-core.ts`
  - 统一定义 `RuleOperator`
  - 统一定义 `RuleNodeType`
  - 统一定义 `RuleGroupOperator`
  - 统一定义条件节点、逻辑组节点、结果规则结构
  - 统一提供树结构命中判断
  - 统一提供树结构说明文本生成
  - 统一提供默认规则深拷贝
- `useRuleConfigState.ts`
  - 统一管理页面级规则状态
  - 统一管理 `enabled / rules / version / dirty`
  - 统一管理 `workspaceKey` 维度的持久化
  - 统一处理恢复默认时的深拷贝重置

### 2. 页面保留，规则编辑区升级

代码走查非法记录页面 [CodeReviewIllegalRecordsView.vue](/D:/projects/data_collection_platform/frontend/src/views/CodeReviewIllegalRecordsView.vue) 已切到新的树结构规则核心。

当前页面表现为：

- 抽屉壳层保留
- 下方预览说明区保留
- 正式后端规则说明区保留
- 上方编辑区从“单行字段-关系-取值”升级成“逻辑组 + 原子条件”的递归树编辑器

新增组件：

- `frontend/src/components/rule-config/RuleExpressionEditor.vue`

它负责：

- 编辑组节点 `AND / OR`
- 编辑条件节点 `字段 / 关系 / 取值`
- 递归新增子组
- 递归新增条件
- 删除子节点

### 3. 代码走查页继续作为首个实现

- `frontend/src/views/code-review-rule-demo.ts`

它现在负责：

- 定义代码走查页可配置字段白名单
- 定义默认结果规则
- 定义字段值如何从当前行数据中读取

## 当前状态

本轮完成后，前端规则配置已经从“线性键值对编辑”切换到“逻辑树编辑”。

但当前仍然是前端 Demo 预览层：

- 不写回后端
- 不改变后端正式非法判定口径
- 只作用于当前页已加载数据的预览命中效果

## 本轮验证

- `npm test` 通过
- `npm run build` 通过

补充说明：

- Vite 仍有历史遗留的 chunk 体积 warning
- 该 warning 不是本轮规则树重构引入的问题
