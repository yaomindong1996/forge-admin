# 任务拆分 — 低代码设计器精简与整合

> 分三期实施。每期可独立交付，互不阻塞。

---

## 第一期：对象设计器精简（P0，2 天）

> 目标：对象设计器从 5 Tab 缩减为 3 Tab，用户体感立刻改善。

### Task 1：移除"默认视图"和"触发器"Tab

**状态：completed（2026-08-15，`e65b6bc8`）**

**文件**：`forge-admin-ui/src/views/app-center/components/designer/BusinessObjectDesignerShell.vue`

- [ ] 从 `standaloneNavItems` 数组中移除 `default-view`（默认视图）项
- [ ] 从 `standaloneNavItems` 数组中移除 `triggers`（触发器）项
- [ ] 移除模板中 `default-view` 对应的 `<NTabPane>` 块
- [ ] 移除模板中 `triggers` 对应的 `<NTabPane>` 块
- [ ] 移除或注释掉 `BusinessListDesigner` 的 import（组件保留，只是不在对象设计器中使用）
- [ ] 移除或注释掉 trigger.vue embedded 相关的 import 和引用

**验证**：
- 对象设计器只显示 3 个 Tab（基本信息、字段设计、数据关系）
- 切换 Tab 不报错
- `pnpm build` 编译通过

### Task 2：移除"单据闭环配置"进度块

**状态：completed（2026-08-15，`d2873a3a`）**

**文件**：`forge-admin-ui/src/views/app-center/components/designer/BusinessObjectDesignerShell.vue`

- [ ] 移除模板中 `closure-steps` 相关的 `<div>` 块（约 L113-L129）
- [ ] 移除相关的 `closureSteps` computed 和样式
- [ ] 移除相关的 `closureStatus` 数据加载逻辑

**验证**：
- 对象设计器顶部不再显示"单据闭环配置"进度条
- 页面布局不出现空白区域

### Task 3：流程绑定子Tab 改为只读卡片

**状态：completed（2026-08-15，`2641bb66`、`06afa841`）**

**文件**：
- `forge-admin-ui/src/views/app-center/components/designer/BusinessObjectDesignerShell.vue`
- 新建 `forge-admin-ui/src/views/app-center/components/designer/ObjectProcessReadOnlyPanel.vue`

- [ ] 从数据模型 Tab 的子Tab 中移除 `flow-app`（流程绑定）
- [ ] 在数据关系 Tab 底部新增一个只读"流程信息"区域
- [ ] 新建 `ObjectProcessReadOnlyPanel.vue` 组件：
  - 调用 `GET /business/object/{objectCode}/processes` 查询已参与的业务流程
  - 以列表形式展示流程名称、状态、开始节点类型
  - 底部显示"去应用工作台配置 →"跳转链接
- [ ] 如果 API 未实现，先展示"请在应用工作台的业务流程画布中配置"提示文字

**验证**：
- 数据模型 Tab 下只有"对象关系"和"数据权限"两个子Tab
- 只读流程信息卡片正确展示
- 跳转链接能正确打开应用工作台

### Task 4：移除 BusinessActionDesigner 入口

**状态：completed（2026-08-15，`8ed7ee21`）**

**文件**：`forge-admin-ui/src/views/app-center/components/designer/BusinessObjectDesignerShell.vue`

- [ ] 移除模板中任何直接打开 BusinessActionDesigner 的按钮或链接
- [ ] 移除 `BusinessActionDesigner` 的 import（组件保留，只是不在对象设计器中引用）
- [ ] 如果有 `actions` 面板（legacy 面板白名单中），从白名单中移除 `actions`

**验证**：
- 对象设计器中不再出现"业务动作"或"自动化动作"入口
- `pnpm build` 编译通过

### Task 5：旧入口的引导提示

**状态：completed（2026-08-15，`1f0d2a64`）**

**文件**：`forge-admin-ui/src/views/app-center/components/designer/BusinessObjectDesignerShell.vue`

- [ ] 在对象设计器顶部或数据关系 Tab 底部，新增一个 `NAlert` 提示卡片：
  - 标题："流程与自动化配置已移至应用工作台"
  - 内容："触发器、流程绑定和业务动作已统一为业务流程画布，请在应用工作台 → 业务流程中配置"
  - 类型：info
  - 右侧按钮："前往应用工作台"（跳转到 `/app-center/application/{applicationCode}`）

**验证**：
- 用户在对象设计器中能看到明确的引导提示
- 点击按钮能跳转到应用工作台

---

## 第二期：页面设计器增强（P1，3 天）

> 目标：用户在页面设计器中能自动生成子表配置、配置按钮行为。

### Task 6：子表分区配置向导

**状态：completed（2026-08-15，`a83f618b`）**

**文件**：
- 新建 `forge-admin-ui/src/views/app-center/components/designer/ChildTableSectionWizard.vue`
- 修改 `forge-admin-ui/src/views/app-center/components/designer/BusinessFormDesigner.vue`

- [ ] 新建 `ChildTableSectionWizard.vue` 组件，包含以下交互：
  1. 下拉选择关联关系（从 `relations` 中筛选 `relationType = DETAIL` 的项）
  2. 选中后自动读取子对象的字段列表
  3. 显示分区标题输入框（默认使用关系名称）
  4. 显示显示模式下拉（内联表格 inline_grid / 卡片列表 card_list / 底部弹窗 bottom_sheet）
  5. 字段可见性勾选（默认全部可见）
  6. 点击确认后调用 `onConfirm` 回调，传出配置对象

- [ ] 在 `BusinessFormDesigner.vue` 中新增"添加子表分区"按钮
- [ ] 点击后弹出 `ChildTableSectionWizard` 弹窗
- [ ] 向导确认后，自动生成三层配置：
  - `pageSchema.modelRefs` 新增一条 `{ modelCode, tableName, primary: false, props: { relationKey, ... } }`
  - `options.masterDetailConfig.children` 新增一条 `{ key, relationKey, modelCode, tableName, fields, saveMode: 'CASCADE', showInEdit: true, showInDetail: true, rowActions: [], toolbarActions: [] }`
  - `formDesignerSchema.pageSections` 新增一条 `{ sectionKey, title, type: 'child_table', displayMode, relationKey }`
- [ ] 生成后自动刷新表单设计器的分区列表

**验证**：
- 用户选择一个关系后，分区自动出现在表单设计器中
- 生成的配置结构正确，运行时能正常渲染子表
- 不需要手动编辑 JSON

### Task 7：按钮行为配置组件

**状态：completed（2026-08-15，`70c1cc3b`；页面运行时启动流程依赖 Task 10）**

**文件**：
- 新建 `forge-admin-ui/src/views/app-center/components/designer/ButtonActionConfig.vue`
- 修改 `forge-admin-ui/src/views/app-center/components/designer/BusinessFormDesigner.vue`

- [ ] 新建 `ButtonActionConfig.vue` 组件，包含：
  1. 行为类型下拉（提交保存 / 跳转页面 / 启动业务流程 / 执行自定义动作）
  2. 选择"启动业务流程"时：
     - 显示业务流程下拉列表（调用 `businessProcessPage` API，过滤当前应用）
     - 列表底部有"+ 新建业务流程"按钮
     - 选中后显示权限标识输入框
  3. 选择"跳转页面"时：显示目标页面下拉
  4. 选择"提交保存"时：无额外配置
  5. 确认后输出行为配置对象

- [ ] 在 `BusinessFormDesigner.vue` 的按钮配置区集成 `ButtonActionConfig`
- [ ] 按钮行为配置保存到 `formDesignerSchema.bottomBar.actions[].type` 和 `actionCode`

**验证**：
- 能选择已有的业务流程
- "+ 新建业务流程"按钮能跳转到画布
- 配置保存后，运行时按钮点击能正确触发对应流程

### Task 8：子表分区编辑与删除

**状态：completed（2026-08-15，`4bf4ce79`）**

**文件**：`forge-admin-ui/src/views/app-center/components/designer/BusinessFormDesigner.vue`

- [ ] 分区列表中，子表分区支持点击编辑（重新打开向导，回填配置）
- [ ] 分区列表中，子表分区支持删除
- [ ] 删除时同步清理三层 JSON 配置（modelRef、child config、section）
- [ ] 删除前弹出确认提示

**验证**：
- 编辑子表分区能正确回填当前配置
- 删除后三层 JSON 配置同步清理
- 不残留孤儿 modelRef 或 child config

---

## 第三期：画布接入与体验优化（P2，3 天）

> 目标：画布成为唯一编排入口，旧入口导航引导完善。

### Task 9：页面设计器 → 画布跳转链路

**状态：completed（2026-08-15，`d0f019e1`）**

**文件**：
- `forge-admin-ui/src/views/app-center/components/designer/ButtonActionConfig.vue`
- `forge-admin-ui/src/views/app-center/business-process.[processId].vue`

- [ ] `ButtonActionConfig` 中"新建业务流程"按钮点击后：
  1. 调用 `createBusinessProcess` API 创建草稿流程
  2. 流程的开始节点默认选中"手动触发"
  3. 跳转到 `/app-center/business-process/{newProcessId}`
  4. 画布加载时 URL 携带 `from=button&objectCode=xxx` 参数
- [ ] 画布保存后，返回页面设计器时刷新流程下拉列表

**验证**：
- 从页面按钮配置能一键跳到画布
- 画布默认手动触发开始节点
- 返回后下拉列表包含新建的流程

### Task 10：画布开始节点关联页面按钮

**状态：blocked（2026-08-15）**。仓库尚未实现业务流程运行时编排器、手动发起接口、节点执行服务或页面运行时 `START_PROCESS` 投影；现有应用发布快照的 `runtimeActions` 明确保持为空，待编排运行时变更实现。继续本项将扩大为 `application-business-process-orchestrator` 中未完成的运行时状态机、动作执行与审批恢复工作，不能用只渲染按钮或返回 404 的占位实现替代。

**文件**：
- `forge-admin-ui/src/components/business-process-designer/StartNodeConfig.vue`
- `forge-admin-ui/src/components/business-process-designer/business-process-schema.js`

- [ ] 开始节点配置为"手动触发"时，显示关联信息：
  - 关联对象（从 URL 参数或流程配置读取）
  - 按钮位置（列表按钮 / 详情按钮 / 子表行按钮）
  - 按钮标签
  - 权限标识
- [ ] 流程发布后，这些信息写入运行时配置，页面运行时读取并渲染按钮

**验证**：
- 手动触发开始节点能配置按钮位置和标签
- 发布后页面正确渲染对应按钮
- 按钮点击能启动对应流程

### Task 11：节点配置场景模板

**状态：completed（2026-08-15，`e89c6578`）**

**文件**：
- 新建 `forge-admin-ui/src/components/business-process-designer/node-templates.js`
- 修改 `forge-admin-ui/src/components/business-process-designer/StartNodeConfig.vue`
- 修改 `forge-admin-ui/src/components/business-process-designer/ActionAndApprovalNodeConfig.vue`

- [ ] 新建 `node-templates.js`，定义常用场景模板：

```javascript
export const START_NODE_TEMPLATES = [
  { label: '手动点击按钮', value: 'MANUAL', description: '用户在列表或详情页点击按钮触发' },
  { label: '记录创建后', value: 'EVENT_CREATED', description: '新记录保存到数据库后自动触发' },
  { label: '状态变更后', value: 'EVENT_STATUS_CHANGED', description: '记录状态字段变化后自动触发' },
  { label: '定时扫描', value: 'SCHEDULED', description: '按固定周期扫描到期或超期记录' },
]

export const ACTION_NODE_TEMPLATES = [
  { label: '更新状态', value: 'UPDATE_STATUS', steps: [{ type: 'TRANSITION_STATUS', ... }] },
  { label: '调整数量', value: 'ADJUST_NUMBER', steps: [{ type: 'ADJUST_NUMBER', ... }] },
  { label: '创建记录', value: 'CREATE_RECORD', steps: [{ type: 'CREATE_RECORD', ... }] },
  { label: '发送消息', value: 'SEND_MESSAGE', steps: [{ type: 'SEND_MESSAGE', ... }] },
]
```

- [ ] 开始节点配置面板顶部显示模板选择卡片
- [ ] 动作节点配置面板顶部显示模板选择卡片
- [ ] 选择模板后自动填充步骤配置，用户可在模板基础上调整

**验证**：
- 用户选模板后，节点配置自动填充
- 模板配置可在此基础上手动调整
- 不选模板时保留空白配置路径

### Task 12：旧入口导航引导

**状态：completed（2026-08-15，`466bb929`）**

**文件**：
- `forge-admin-ui/src/views/app-center/trigger.vue`
- `forge-admin-ui/src/views/app-center/components/designer/BusinessFlowBindingPanel.vue`

- [ ] `trigger.vue` 顶部新增 `NAlert` 提示：
  - "触发器配置已整合到业务流程画布，建议在新入口维护。此处仅支持查看历史配置。"
  - 按钮："前往应用工作台业务流程"
- [ ] `trigger.vue` 中的"新增触发器"按钮改为只读模式或隐藏
- [ ] `BusinessFlowBindingPanel.vue` 顶部新增同样的提示
- [ ] 如果组件在对象设计器中已被移除（第一期），此 Task 仅需处理独立路由的 trigger.vue

**验证**：
- 旧入口能看到引导提示
- 旧入口不再允许新增配置
- 不影响已有运行实例

### Task 13：后端 API — 对象参与流程查询

**状态：completed（2026-08-15，`06afa841`）**

**文件**：
- `forge-server/.../service/businessapp/BusinessProcessService.java`（或新建）
- `forge-server/.../controller/BusinessAppController.java`

- [ ] 新增 `GET /business/object/{objectCode}/processes` 接口：
  - 查询 `ai_business_process` 表中 `primary_object_code = {objectCode}` 的记录
  - 返回流程列表：`[{ id, processName, processCode, status, startNodeType }]`
- [ ] 添加权限注解 `@SaCheckPermission`
- [ ] 返回 `RespInfo.success(list)`

**验证**：
- 接口能正确返回对象参与的业务流程列表
- 无权限时返回 403
- 对象未参与任何流程时返回空列表

---

## 验收清单

### 第一期验收

- [ ] 对象设计器只有 3 个 Tab（基本信息、字段设计、数据关系）
- [ ] 数据关系 Tab 下只有 2 个子 Tab（对象关系、数据权限）
- [ ] 不再出现触发器、流程绑定、业务动作、默认视图入口
- [ ] 有引导提示指向应用工作台
- [ ] `pnpm build` 编译通过
- [ ] 已有的触发器运行实例不受影响

### 第二期验收

- [ ] 表单设计器中有"添加子表分区"按钮
- [ ] 选择关系后自动生成三层 JSON 配置
- [ ] 子表分区支持编辑和删除
- [ ] 按钮行为配置能选择已有业务流程
- [ ] 按钮行为配置能跳转到画布新建流程
- [ ] 运行时按钮点击能触发对应流程

### 第三期验收

- [ ] 从页面按钮配置能一键跳到画布
- [ ] 画布手动触发节点能配置按钮位置和标签
- [ ] 画布节点有场景模板可选
- [ ] 旧 trigger.vue 入口有引导提示
- [ ] 对象只读流程信息卡片正确展示
- [ ] 旧入口不再允许新增配置

---

## 依赖关系

```
Task 1 ─┐
Task 2 ─┤
Task 3 ─┼─→ 第一期交付（可独立上线）
Task 4 ─┤
Task 5 ─┘

Task 6 ─┐
Task 7 ─┼─→ 第二期交付（依赖第一期完成）
Task 8 ─┘

Task 9  ─┐
Task 10 ─┤
Task 11 ─┼─→ 第三期交付（依赖第二期完成）
Task 12 ─┤
Task 13 ─┘
```

- Task 1-5 互相独立，可并行
- Task 6-8 互相独立，可并行，但依赖第一期完成（对象设计器已精简）
- Task 9 依赖 Task 7（按钮行为配置已实现）
- Task 10 依赖 Task 9（跳转链路已通）
- Task 11-13 互相独立，可并行
