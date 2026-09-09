# 低代码设计器精简与整合
> status: propose
> created: 2026-08-14
> complexity: 🟡中等
> change: `lowcode-designer-simplification`

## 1. 背景与目标

用户反馈对象设计器太复杂、业务动作配置看不懂、触发器/流程绑定/业务动作三个入口分散。总体要求是**好用、简单、不复杂**。

本变更将对象设计器精简为"只管数据"，将流程编排统一到应用工作台的业务流程画布，将页面组装从多层 JSON 配置简化为可视化拖拽。

### 1.1 目标信息架构

```text
对象设计器（只管"数据是什么"）
├── 基本信息
├── 字段设计
└── 数据关系（关系 + 数据权限）

应用工作台（只管"用户怎么用"）
├── 概览
├── 页面与表单（拖拽配字段 + 子表 + 按钮）
├── 业务流程（画布配"按钮点了干嘛" + 事件/定时触发）
├── 访问入口
└── 发布
```

### 1.2 必须达到的可验证结果

1. 对象设计器从 5 个 Tab 精简为 3 个，普通用户 3 分钟能理解每个 Tab 的职责
2. 用户在页面设计器中添加子表分区时，只需选择一个关联关系，系统自动生成 pageSection + masterDetailConfig.children + modelRef 三层配置
3. 用户在页面设计器中配置按钮行为时，能直接选择或新建业务流程，不需要跳到其他入口
4. 触发器、流程绑定、业务动作三套配置入口合并为业务流程画布一个入口
5. 旧的触发器和流程绑定配置变为只读，不阻断已有运行实例
6. 对象设计器不再暴露 BusinessActionDesigner（77KB 2300 行的复杂配置面板）

### 1.3 非目标

- 不重写 BusinessProcessDesigner 画布组件（已有实现，只做接入和体验优化）
- 不替换 Flowable 引擎（审批节点继续引用已发布的 Flowable 模型）
- 不删除旧触发器、旧流程绑定的运行时代码（只停用配置入口）
- 不在本变更中实现拖拽式页面设计器（本期用选择式 + 向导式替代）
- 不做数据迁移（旧配置保留只读，新配置在新入口维护）

## 2. 现状与问题

### 2.1 对象设计器 Tab 过多

`BusinessObjectDesignerShell.vue` 定义 5 个 Tab：

| Tab | 组件 | 问题 |
|-----|------|------|
| 基本信息 | BusinessObjectBasicInfo | 保留 |
| 字段设计 | BusinessFieldManager | 保留 |
| 数据模型 | BusinessRelationDesigner + BusinessFlowBindingPanel + BusinessPermissionFlowPanel | 流程绑定应移出 |
| 默认视图 | BusinessListDesigner | 属于应用页面配置，不属于对象 |
| 触发器 | trigger.vue (embedded) | 应由业务流程画布取代 |

另有"单据闭环配置"进度块（closure-steps），引用了已移出的流程和触发器步骤，也需要调整。

### 2.2 触发器、流程绑定、业务动作三套并存

三个入口分散在对象设计器不同位置，用户要配"审批通过后更新状态"需要跨三个界面：

| 旧入口 | 组件 | 职责 | 画布中的对应 |
|--------|------|------|-------------|
| 触发器 | trigger.vue | 事件/定时触发 → 执行一个动作 | 画布的开始节点（事件/定时） |
| 流程绑定 | BusinessFlowBindingPanel.vue | 绑定 Flowable 模型 + 审批结果回调 | 画布的审批节点 + 出口动作 |
| 业务动作 | BusinessActionDesigner.vue | 多步骤业务逻辑（本地事务/编排） | 画布的动作节点 |

### 2.3 业务流程画布已实现但未接通

`components/business-process-designer/` 已有完整画布实现（14 个文件），支持：
- 开始节点（事件触发 / 定时触发 / 手动触发）
- 条件分支
- 审批子流程（引用 Flowable 模型）
- 动作节点（更新记录 / 创建记录 / 发送消息 / 调用业务动作）
- 结束节点

应用工作台 `automation` Tab 已指向 `ApplicationProcessPanel.vue`（业务流程列表），但：
- 对象设计器仍保留旧的触发器、流程绑定、业务动作入口
- 没有引导提示告诉用户去应用工作台配置
- 页面设计器没有"按钮行为 → 选择/创建流程"的入口

### 2.4 页面组装需要用户理解 4 层 JSON

预售单页面由 4 层配置组装，用户无法通过 UI 直观理解：

| 层 | 配置位置 | JSON 路径 |
|----|---------|----------|
| 对象关系 | 对象设计器 > 数据关系 | `ai_business_object_relation` 表 |
| modelRefs | 自动生成 | `page_schema.modelRefs` |
| 子表渲染 | 表单设计器分区 | `options.masterDetailConfig.children` |
| 页面布局 | 表单设计器分区 | `formDesignerSchema.pageSections` |

用户配好关系后，还需要在表单设计器手动添加分区、手动选 relationKey、手动匹配 child config。没有"选择一个关系 → 自动生成所有配置"的向导。

## 3. 设计方案

### 3.1 对象设计器精简

#### 精简后的 Tab 结构

```
对象设计器
├── 基本信息（名称、编码、图标、所属业务域）
├── 字段设计（字段增删改、类型、约束、默认值）
└── 数据关系
    ├── 对象关系（BusinessRelationDesigner — 保留，精简配置项）
    └── 数据权限（BusinessPermissionFlowPanel — 保留）
```

#### 去掉的 Tab 和组件

| 去掉的内容 | 去掉方式 | 替代入口 |
|-----------|---------|---------|
| 默认视图 Tab (BusinessListDesigner) | 从 standaloneNavItems 移除 | 应用工作台 > 页面与表单 |
| 触发器 Tab (trigger.vue embedded) | 从 standaloneNavItems 移除 | 应用工作台 > 业务流程画布 |
| 流程绑定子Tab (BusinessFlowBindingPanel) | 从 data-model 子Tab 移除 | 应用工作台 > 业务流程画布 |
| 单据闭环配置进度块 (closure-steps) | 从模板移除 | 应用工作台 > 发布就绪检查 |
| BusinessActionDesigner 入口 | 不再从对象设计器打开 | 应用工作台 > 业务流程画布 > 动作节点 |

#### 流程绑定的只读兼容

对象设计器中保留一个只读的"流程信息"卡片（替代原流程绑定子Tab），显示：
- 此对象已参与的业务流程列表（从 `ai_business_process` 查询）
- 每个流程的状态（草稿/已发布）
- "去应用工作台配置"跳转链接

### 3.2 应用工作台页面设计器增强

#### 新增"子表分区"配置向导

在表单设计器中新增一个"添加子表分区"操作，用户只需：

1. 点击"添加子表分区"
2. 从下拉列表选择一个关联关系（从对象关系配置读取）
3. 系统自动生成：
   - `pageSchema.modelRefs` 中新增一条 modelRef（含 modelCode、tableName、props.relationKey）
   - `options.masterDetailConfig.children` 中新增一条 child（含 fields、saveMode、showInEdit、relationKey）
   - `formDesignerSchema.pageSections` 中新增一条 section（含 type=child_table、displayMode=inline_grid、relationKey）
4. 用户可以在分区上调整：标题、显示模式（内联表格/卡片列表/底部弹窗）、字段可见性

#### 新增"按钮行为"配置

在页面设计器的交互配置区，每个按钮可以配置行为类型：

| 行为类型 | 配置内容 | 对应旧入口 |
|---------|---------|----------|
| 提交保存 | 无额外配置 | 页面操作（内置） |
| 跳转页面 | 目标页面选择 | 页面操作（内置） |
| 启动业务流程 | 选择已有流程 / 新建流程 | 流程绑定 + 触发器 |
| 执行自定义动作 | 选择已有流程（含动作节点） | 业务动作 |

选择"启动业务流程"时：
- 下拉列表展示当前应用下所有已发布的业务流程
- 列表底部有"+ 新建业务流程"按钮，点击后跳转到画布
- 选中流程后显示权限标识输入框

### 3.3 业务流程画布整合

#### 画布作为唯一编排入口

`ApplicationProcessPanel.vue` 已有业务流程列表。画布（`BusinessProcessDesigner.vue`）已支持所需节点类型。

需要补充的接入工作：

1. **页面设计器 → 画布的跳转链路**：页面按钮配置中"新建业务流程"跳转到画布，并在开始节点默认选中"手动触发"
2. **画布开始节点的手动触发**：发布后生成页面按钮配置，回写到页面设计器的按钮列表
3. **对象设计器只读流程信息**：查询当前对象参与的业务流程，展示只读列表
4. **旧触发器入口隐藏**：trigger.vue 保留路由但不在导航中暴露，仅支持历史数据查看

#### 节点配置体验优化

画布中各节点的配置面板需要简化的部分：

| 节点 | 当前配置 | 简化方向 |
|------|---------|---------|
| 开始节点-手动触发 | 无 | 新建流程时默认选中，关联到页面按钮 |
| 开始节点-事件触发 | eventType + eventCondition | 提供场景模板（创建后/状态变更后/字段变更后） |
| 开始节点-定时触发 | scheduleConfig + tierRules | 提供常用模板（到期提醒/超期预警） |
| 审批节点 | 选择 Flowable 模型 + 变量映射 | 模型选择后自动推断变量映射 |
| 动作节点-更新记录 | 步骤配置 + 字段映射 | 提供常用模板（更新状态/更新数量） |
| 动作节点-发送消息 | 消息模板选择 | 提供常用模板（审批通知/到期提醒） |

## 4. 数据与接口

### 4.1 不新增数据表

本变更不新增数据表。业务流程画布的数据已存在于 `ai_business_process` 和 `ai_business_process_node` 表。

### 4.2 接口变更

| 接口 | 变更 | 说明 |
|------|------|------|
| `GET /business/object/{objectCode}/designer` | 响应中不再要求返回 trigger 列表 | 减少加载时间 |
| `GET /business/process/page` | 已有 | 业务流程列表，应用工作台使用 |
| `GET /business/process/{id}/designer` | 已有 | 画布设计器数据 |
| `POST /business/process/{id}/schema` | 已有 | 保存画布配置 |
| `GET /business/object/{objectCode}/processes` | 新增 | 查询对象参与的业务流程（只读列表） |

### 4.3 前端路由变更

| 路由 | 变更 |
|------|------|
| `/app-center/object-designer/:objectCode` | Tab 减少，不加载 trigger 和 flow-binding |
| `/app-center/business-process/:processId` | 已有，画布设计器页面 |
| `/app-center/application/:applicationCode` | 页面设计器增加子表向导和按钮行为配置 |

## 5. 兼容与迁移

### 5.1 旧配置只读兼容

| 旧配置 | 兼容方式 |
|--------|---------|
| `ai_business_trigger` 表数据 | 保留，运行时继续执行。配置入口变为只读 |
| `ai_business_binding` (FLOW) 表数据 | 保留，运行时继续使用。对象设计器只读展示 |
| `designerOptions.actions` 中的业务动作 | 保留，画布的动作节点可引用已有动作编码 |

### 5.2 迁移路径（后续变更，不在本期）

后续可提供一个迁移工具，将旧触发器 + 流程绑定 + 业务动作自动转换为业务流程画布节点。本期只做入口收敛，不做数据迁移。

## 6. 关键文件清单

### 需要修改的文件

| 文件 | 改动概述 |
|------|---------|
| `forge-admin-ui/src/views/app-center/components/designer/BusinessObjectDesignerShell.vue` | 移除"默认视图"和"触发器"Tab，移除"单据闭环配置"进度块，移除流程绑定子Tab |
| `forge-admin-ui/src/views/app-center/components/designer/BusinessObjectDesignerShell.vue` 的 `standaloneNavItems` | 从 5 项缩减为 3 项 |
| `forge-admin-ui/src/views/app-center/components/designer/BusinessFormDesigner.vue` | 新增"子表分区"配置向导和"按钮行为"配置 |
| `forge-admin-ui/src/views/app-center/components/designer/BusinessListDesigner.vue` | 从对象设计器移除，保留组件本身供应用页面配置使用 |
| `forge-admin-ui/src/views/app-center/application-workspace/ApplicationProcessPanel.vue` | 补充"从页面按钮新建流程"的跳转入口 |
| `forge-admin-ui/src/views/app-center/components/business-process-designer/StartNodeConfig.vue` | 手动触发开始节点关联页面按钮 |

### 需要新增的文件

| 文件 | 用途 |
|------|------|
| `forge-admin-ui/src/views/app-center/components/designer/ChildTableSectionWizard.vue` | 子表分区配置向导组件 |
| `forge-admin-ui/src/views/app-center/components/designer/ButtonActionConfig.vue` | 按钮行为配置组件 |
| `forge-admin-ui/src/views/app-center/components/designer/ObjectProcessReadOnlyPanel.vue` | 对象设计器中的只读流程信息卡片 |

### 不改动的文件

| 文件 | 原因 |
|------|------|
| `components/business-process-designer/*` | 画布已实现，只做接入不做重写 |
| `trigger.vue` | 保留路由，只从导航中隐藏 |
| `BusinessActionDesigner.vue` | 保留组件，只从对象设计器入口中移除 |
| `BusinessFlowBindingPanel.vue` | 保留组件，只从对象设计器 Tab 中移除 |

## 7. 风险与缓解

| 风险 | 缓解措施 |
|------|---------|
| 用户找不到旧入口 | 在移除位置放提示卡片，引导到新入口 |
| 旧触发器仍在运行 | 只停用配置入口，运行时不受影响 |
| 子表向导自动生成配置有误 | 向导生成后允许手动调整，不锁死 |
| 画布节点配置仍然复杂 | 提供场景模板，减少空白配置 |
| 页面设计器改动范围大 | 分两期：先加子表向导，后加按钮行为 |

## 8. 实施分期

### 第一期：对象设计器精简（体感改善最大，改动最小）

- 砍 Tab（默认视图、触发器）
- 砍单据闭环配置进度块
- 流程绑定改为只读卡片
- 移除 BusinessActionDesigner 入口

### 第二期：页面设计器增强

- 子表分区配置向导
- 按钮行为配置（选流程/建流程）

### 第三期：画布接入优化

- 开始节点关联页面按钮
- 节点配置场景模板
- 旧入口导航引导

## 9. 验收标准

1. 对象设计器只有 3 个 Tab：基本信息、字段设计、数据关系
2. 数据关系 Tab 下只有 2 个子 Tab：对象关系、数据权限
3. 对象设计器中不再出现 trigger.vue、BusinessFlowBindingPanel、BusinessActionDesigner
4. 页面设计器中能通过"添加子表分区"向导自动生成三层 JSON 配置
5. 页面设计器中能通过按钮行为配置选择或新建业务流程
6. 应用工作台的业务流程列表能正常打开画布
7. 旧触发器和流程绑定的运行实例不受影响
