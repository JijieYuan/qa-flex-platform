# 2026-04-10 规则配置核心复用设计补充

## 背景

代码走查非法记录页面最初接的是“线性句式规则 Demo”。这版适合验证字段、关系、取值、结果的单条编辑体验，但不适合继续承接“逻辑重叠”“多特征交并集”“树状规则”这类需求。

因此本轮前端不再继续给线性模型打补丁，而是改为局部重写规则核心层，保留页面 UI 外壳。

## 收口后的目标结构

### 1. `rule-config-core.ts`

统一承接：

- `RuleOperator`
- `RuleNodeType`
- `RuleGroupOperator`
- `RuleConditionNode`
- `RuleGroupNode`
- `RuleConfigResultRule`
- `AbstractRuleConfigSchemaSupport`

这个抽象层现在负责：

- 创建条件节点
- 创建逻辑组节点
- 创建结果规则
- 递归同步节点字段和操作符
- 递归评估命中结果
- 递归生成人类可读说明
- 深拷贝默认规则

### 2. `useRuleConfigState.ts`

统一承接：

- 页面级规则状态
- `workspaceKey` 隔离
- `enabled / rules / version / dirty`
- 本地持久化
- 恢复默认时的深拷贝重置

### 3. 页面专属 schema support

例如当前首个实现：

- `frontend/src/views/code-review-rule-demo.ts`

它只负责：

- 定义当前页面允许使用的字段
- 定义默认逻辑树规则
- 定义字段值如何从当前行数据中读取

### 4. 通用递归编辑组件

新增：

- `frontend/src/components/rule-config/RuleExpressionEditor.vue`

它负责：

- 编辑组节点
- 编辑条件节点
- 递归新增子组
- 递归新增条件
- 删除子节点

## 当前推荐复用方式

后续其它页面如果也要接树结构规则编辑，优先按这个顺序：

1. 新建页面专属 schema support
2. 复用 `rule-config-core.ts`
3. 复用 `useRuleConfigState.ts`
4. 复用 `RuleExpressionEditor.vue`
5. 页面自身只保留结果展示与交互编排

## 设计收益

这一版比旧线性模型更适合后续演进，因为它已经具备：

- 原子条件节点
- 逻辑组节点
- `AND / OR` 组合
- 多层嵌套扩展空间

同时它仍然保持当前项目需要的轻量特征：

- 还是白名单字段
- 还是受控操作符
- 还是页面级 Demo 预览
- 没有直接引入重型低代码规则平台

## 下一步建议

下一阶段如果继续推进，建议按这个顺序：

1. 先让代码走查页稳定使用树结构前端规则编辑器
2. 再选一个第二页面验证复用性
3. 最后再补后端规则树引擎与正式保存接口
