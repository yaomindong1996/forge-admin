# 仿钉钉审批样式流程设计器

> status: propose
> created: 2026-06-17
> complexity: 🔴复杂

## 1. 背景与目标

### 为什么做

当前流程设计器基于 `bpmn-js 17.11`（标准 BPMN 建模器），交互范式是自由拖拽、palette 拖拽添加节点、手动画线。这种范式对非技术人员不友好，学习成本高。

钉钉/企业微信的审批流程设计器采用**纵向自上而下、点击"+"号添加节点、卡片式节点、自动连线**的交互范式，更符合审批场景的直觉操作。

### 做完后的效果

1. 流程设计器从 BPMN 自由画布改为钉钉审批样式（纵向卡片流 + "+"号添加 + 自动连线）
2. 节点配置从侧边停靠面板改为右侧抽屉
3. 流程查看器从 BPMN 图片改为钉钉样式卡片流 + 节点状态高亮
4. **后端 Flowable 零改动**——前端内部用 JSON 节点树编辑，保存时转换为 BPMN XML 提交后端
5. 已有流程模型完全兼容——加载时 XML→JSON 转换，编辑后 JSON→XML 转换
6. AI 生成流程功能保留并适配
7. 所有现有 BPMN 节点类型保留（审批人/抄送/条件分支/并行分支/包容分支/服务任务/脚本任务/子流程/调用活动等）

### 可验证的结果

- 打开流程设计页面，画布显示纵向卡片流，节点间有"+"号
- 点击"+"号弹出节点类型菜单，选择后自动插入节点并连线
- 点击节点弹出右侧抽屉配置审批人/会签/权限等
- 保存后后端收到 BPMN XML，与改造前格式一致
- 加载已有流程模型，正确显示为钉钉样式
- 审批中/已完成的流程图以钉钉样式展示节点状态（已完成/进行中/待处理）
- AI 生成流程后画布正确渲染

## 2. 代码现状（Research Findings）

### 2.1 相关入口与链路

**前端入口**：
- `forge-admin-ui/src/views/flow/design.vue`（3124行）— 流程设计页面，集成 FlowModeler + NodePropertiesPanel + AI面板
- `forge-admin-ui/src/views/flow/model.vue`（1350行）— 流程模型管理列表，跳转到 design.vue
- `forge-admin-ui/src/views/flow/todo.vue` / `done.vue` / `started.vue` / `monitor.vue` — 流程审批页，使用 ProcessDiagramViewer / InteractiveProcessDiagram 查看流程图

**前端组件**：
- `forge-admin-ui/src/components/bpmn/FlowModeler.vue`（1307行）— BPMN 建模器主组件，封装 bpmn-js
- `forge-admin-ui/src/components/bpmn/BpmnModeler.vue`（620行）— 基础 BPMN 建模器
- `forge-admin-ui/src/components/bpmn/NodePropertiesPanel.vue`（2655行）— 节点配置侧边面板
- `forge-admin-ui/src/components/bpmn/CustomRenderer.js`（311行）— 自定义 SVG 渲染器
- `forge-admin-ui/src/components/bpmn/ProcessDiagramViewer.vue`（754行）— 流程图查看器
- `forge-admin-ui/src/components/bpmn/InteractiveProcessDiagram.vue`（666行）— 交互式流程图查看器
- `forge-admin-ui/src/components/bpmn/flowable-moddle.json`（234行）— Flowable BPMN 扩展属性定义
- `forge-admin-ui/src/components/bpmn/UserSelectModal.vue`（317行）— 用户选择弹窗
- `forge-admin-ui/src/components/bpmn/AutoLayout.js` / `CanvasBackground.js` / `Minimap.vue` / `QuickActionRing.vue` / `ShortcutsBar.vue` — 辅助组件

**前端 API**：
- `forge-admin-ui/src/api/flow.js`（564行）— 流程相关 API，与后端交互的统一入口

**前端依赖**：
- `forge-admin-ui/package.json:30` — `"bpmn-js": "^17.11.1"`
- `forge-admin-ui/package.json:31` — `"bpmn-js-properties-panel": "^5.23.0"`

**后端**（本次不改，仅列出相关模块）：
- `forge/forge-framework/forge-plugin-parent/forge-plugin-flow/` — Flowable 流程引擎插件
- 后端接口统一前缀 `/api/flow/*`，接收和返回 BPMN XML

### 2.2 现有实现

**设计器内核**（FlowModeler.vue:308-408）：
- 使用 `bpmn-js` 的 `BpmnModeler` 实例
- 加载 `flowable-moddle.json` 作为 moddle 扩展
- 通过 `importXML` / `saveXML` 与后端交互
- 监听 `selection.changed` / `element.changed` / `commandStack.changed` 等事件

**节点配置**（NodePropertiesPanel.vue:936-985）：
- `properties` reactive 对象包含完整字段：taskType / assignee / assigneeExpr / candidateUsers / candidateGroups / multiInstanceType / completionCondition / passRate / allowApprove 等 30+ 字段
- 通过 `extractPropertiesFromElement`（1562行）从 BPMN businessObject 读取属性
- 通过 `applyPropertiesToElement` 将配置写回 BPMN 元素
- UserTask 属性读取逻辑（1562-1640）：解析 `flowable:assigneeType` 判断 spel/custom/静态变量；解析 `flowable:assignee` 提取 `${user_xxx}` 用户ID

**流程查看器**（ProcessDiagramViewer.vue / InteractiveProcessDiagram.vue）：
- 调用 `getProcessDiagramInfo(processInstanceId)` 获取 `{ bpmnXml, nodeStatuses }`
- ProcessDiagramViewer 用 bpmn-js 渲染 BPMN XML
- InteractiveProcessDiagram 用后端返回的 `diagramBase64` 图片 + 节点位置叠加状态指示器

**design.vue 数据流**：
- `bpmnXml` ref（578行）存储 BPMN XML 字符串
- `loadModel`（920行）从后端加载 `res.data.bpmnXml` 赋值
- `handleSaveDraft` / `handleDeploy` 调用 `modelerRef.value?.getXML(true)` 获取 XML 提交
- AI 生成（1032-1080行）调用 `streamFlowGenerate`，AI 返回 BPMN XML

### 2.3 发现与风险

1. **NodePropertiesPanel.vue 达 2655 行**，包含审批人/会签/权限/监听器/表单权限/条件/服务任务等全部配置逻辑，迁移时需确保零功能丢失
2. **UserTask 属性映射复杂**——`flowable:assigneeType` 标识 + `${user_xxx}` 格式 + 静态变量 + SPEL 表达式四种分支，转换层必须完整复刻
3. **多实例（会签）的 completionCondition 解析**——现有逻辑按 passRate 生成 Flowable 表达式，转换层需双向支持
4. **BPMN 是有向图，钉钉是树形布局**——循环、多入节点、跳转等复杂结构无法用纯树形表达，需 advanced 节点兜底
5. **已有流程模型兼容**——所有已部署模型的 BPMN XML 必须能正确转换为 JSON 并编辑后再转回 XML，语义等价
6. **移除 bpmn-js 依赖**——需确认无其他模块引用 `bpmn-js` / `bpmn-js-properties-panel` / `inherits-browser`
7. **图形坐标**——JSON→XML 时必须生成 `<bpmndi:BPMNDiagram>` 图形信息，否则 Flowable 管理界面无法显示

## 3. 功能点

- [ ] **F1 钉钉样式画布渲染**：纵向自上而下卡片流，HTML 节点卡片 + SVG 连线覆盖层混合渲染，支持缩放/平移
- [ ] **F2 "+"号添加节点**：节点间显示"+"号，点击弹出节点类型菜单（审批人/抄送/条件分支/并行分支/包容分支/服务任务/脚本任务/子流程/调用活动/结束节点）
- [ ] **F3 节点卡片展示**：每种节点类型有独立图标和颜色，卡片显示节点名称和摘要信息（审批人头像/条件表达式/分支数等）
- [ ] **F4 分支节点布局**：条件分支/并行分支/包容分支的子分支横向并排，分支内纵向延伸，分支末尾自动汇聚
- [ ] **F5 右侧配置抽屉**：点击节点弹出 NDrawer（480px），按节点类型展示对应配置面板，配置分 Tab（基本/会签/权限/表单权限/监听器）
- [ ] **F6 审批人配置**：完整迁移现有 NodePropertiesPanel 的审批人配置——指定审批人/候选用户/候选组、SPEL表达式、静态变量（发起人/发起人上级/部门经理/HR）
- [ ] **F7 会签配置**：并行会签/顺序会签、完成条件（全部/任一/比例），双向转换 multiInstanceLoopCharacteristics
- [ ] **F8 操作权限配置**：允许通过/驳回/转办/退回/终结、必须填写意见、需要手写签名
- [ ] **F9 表单权限配置**：字段级只读/编辑/隐藏权限
- [ ] **F10 监听器配置**：任务监听器（create/assignment/complete/delete）+ 执行监听器（start/end/take）
- [ ] **F11 条件分支配置**：分支列表管理（增删改）、条件类型（表达式/脚本）、条件值、默认分支标记
- [ ] **F12 节点操作**：右键菜单（编辑/复制/上移/下移/删除）、键盘删除（Delete键）、start/end 不可删除
- [ ] **F13 撤销/重做**：基于 JSON 快照的命令栈，Ctrl+Z / Ctrl+Y
- [ ] **F14 BPMN XML → JSON 转换**：解析 BPMN XML，按元素类型映射为钉钉 JSON 节点，提取 flowable:* 扩展属性，未识别元素走 advanced 兜底
- [ ] **F15 JSON → BPMN XML 转换**：钉钉 JSON 转为 BPMN XML，包含 process 元素、sequenceFlow、extensionElements、multiInstanceLoopCharacteristics、BPMNDiagram 图形坐标
- [ ] **F16 图形坐标自动布局**：JSON→XML 时生成 BPMNDiagram，纵向自上而下布局，分支横向并排
- [ ] **F17 已有模型兼容加载**：加载已有流程模型时 XML→JSON 转换，转换失败降级为默认流程
- [ ] **F18 保存/部署**：保存和部署时 JSON→XML 转换后提交后端，后端收到 BPMN XML 格式不变
- [ ] **F19 AI 生成适配**：AI 返回 BPMN XML 后 XML→JSON 转换并加载到画布
- [ ] **F20 流程查看器（钉钉样式）**：替代 ProcessDiagramViewer，用钉钉样式卡片流展示流程实例审批进度，节点状态高亮（已完成/进行中/待处理/驳回/跳过）
- [ ] **F21 查看器节点详情**：点击查看器节点弹出 Popover 显示处理人、处理时间、审批意见
- [ ] **F22 UserSelectModal 复用**：用户选择弹窗复用现有组件，配置抽屉中调用
- [ ] **F23 依赖清理**：移除 bpmn-js / bpmn-js-properties-panel / inherits-browser 依赖

## 4. 业务规则

### 4.1 节点规则

| 规则 | 说明 |
|---|---|
| start 节点不可删除 | 每个流程必须有且仅有一个发起人节点 |
| end 节点不可删除 | 每个流程至少有一个结束节点 |
| 线性链中节点可上下移动 | 分支内节点不可跨分支移动 |
| 删除审批节点自动连接前后 | 保持流程链路完整 |
| 删除分支内节点使分支为空时删除整条分支 | 分支至少保留一个节点 |
| 条件分支至少 2 条 | 删除到 2 条时禁止继续删除 |
| 并行/包容分支至少 2 条 | 同上 |
| 条件分支必须有一条默认分支 | 默认分支对应 BPMN default flow |

### 4.2 审批人配置规则

| 规则 | 说明 |
|---|---|
| taskType=assignee 时 assignee 必填 | 保存时校验 |
| taskType=candidateUsers 时 candidateUsers 不能为空 | 保存时校验 |
| taskType=candidateGroups 时 candidateGroups 不能为空 | 保存时校验 |
| assignee=custom 时必须选择用户 | `${user_xxx}` 格式 |
| assignee=spel 时 assigneeExpr 必填 | SPEL 表达式 |
| multiInstanceType≠none 时 completionCondition 必选 | all/any/ratio |
| completionCondition=ratio 时 passRate 必填 | 1-100 整数 |

### 4.3 会签转换规则

| JSON | BPMN |
|---|---|
| multiInstanceType=parallel | `<multiInstanceLoopCharacteristics isSequential="false">` |
| multiInstanceType=sequential | `<multiInstanceLoopCharacteristics isSequential="true">` |
| completionCondition=all (passRate=100) | `<completionCondition>${nrOfCompletedInstances/nrOfInstances == 1}</completionCondition>` |
| completionCondition=any (passRate=1) | `<completionCondition>${nrOfCompletedInstances >= 1}</completionCondition>` |
| completionCondition=ratio (passRate=N) | `<completionCondition>${nrOfCompletedInstances/nrOfInstances >= N/100}</completionCondition>` |

### 4.4 转换兼容规则

| 规则 | 说明 |
|---|---|
| 无法识别的 BPMN 元素 → advanced 节点 | rawXml 保留原始 XML 片段 |
| advanced 节点 JSON→XML 时原样回写 rawXml | 保证无损往返 |
| 边界事件（BoundaryEvent）→ 附加到父节点的 advanced 信息 | 不单独显示为节点 |
| 子流程内部未识别元素 → advanced 兜底 | 子流程本身正常映射 |
| XML→JSON→XML 往返语义等价 | 节点数量、类型、属性一致，坐标可不同 |

## 5. 数据变更

本次改造**不涉及数据库变更**，后端 Flowable 接口和数据格式完全不变。

| 操作 | 表名 | 字段/索引 | 说明 |
|---|---|---|---|
| — | — | — | 无数据库变更 |

## 6. 接口变更

本次改造**不涉及后端接口变更**，前端与后端的交互格式（BPMN XML）不变。

| 操作 | 接口 | 方法 | 变更内容 |
|---|---|---|---|
| — | — | — | 无接口变更 |

**前端内部数据流变更**（对后端透明）：
- 加载流程模型：`GET /api/flow/model/{id}` 返回 `bpmnXml` → 前端 `BpmnToJsonConverter.convert(xml)` → `flowJson`
- 保存流程模型：`PUT /api/flow/model` 提交 `bpmnXml` ← 前端 `JsonToBpmnConverter.convert(flowJson)` ← `flowJson`
- 流程图查看：`GET /api/flow/task/diagram-info/{processInstanceId}` 返回 `bpmnXml` + `nodeStatuses` → 前端转换 + 状态叠加

## 7. 影响范围

### 7.1 新增文件

```
forge-admin-ui/src/components/flow-designer/
├── DingFlowDesigner.vue                          # 设计器主组件
├── canvas/
│   ├── FlowCanvas.vue                            # 画布容器（缩放/平移/快捷键）
│   ├── NodeCard.vue                              # 通用节点卡片（HTML渲染）
│   ├── NodeAdder.vue                             # "+"号添加器
│   └── ConnectorLayer.vue                        # SVG 连线覆盖层
├── nodes/
│   ├── StartNode.vue                             # 发起人节点
│   ├── ApproverNode.vue                          # 审批人节点
│   ├── CarbonCopyNode.vue                        # 抄送人节点
│   ├── ConditionNode.vue                         # 条件分支节点
│   ├── ParallelNode.vue                          # 并行分支节点
│   ├── InclusiveNode.vue                         # 包容分支节点
│   ├── ServiceNode.vue                           # 服务任务节点
│   ├── ScriptNode.vue                            # 脚本任务节点
│   ├── SubProcessNode.vue                        # 子流程节点
│   ├── CallActivityNode.vue                      # 调用活动节点
│   ├── EndNode.vue                               # 结束节点
│   └── AdvancedNode.vue                          # 高级节点兜底
├── panel/
│   ├── NodeConfigDrawer.vue                      # 右侧配置抽屉容器
│   ├── ApproverConfig.vue                        # 审批人配置
│   ├── ConditionConfig.vue                       # 条件分支配置
│   ├── MultiInstanceConfig.vue                   # 会签配置
│   ├── ListenerConfig.vue                        # 监听器配置
│   ├── FormPermissionConfig.vue                  # 表单权限配置
│   └── ServiceConfig.vue                         # 服务/脚本/子流程配置
├── viewer/
│   ├── DingFlowViewer.vue                        # 钉钉样式查看器
│   └── NodeStatusBadge.vue                       # 节点状态徽章
├── converter/
│   ├── BpmnToJsonConverter.js                    # BPMN XML → JSON
│   ├── JsonToBpmnConverter.js                    # JSON → BPMN XML
│   ├── AutoLayout.js                             # 图形坐标自动布局
│   └── flowable-moddle.json                      # Flowable 扩展定义（迁移自 bpmn/）
└── composables/
    ├── useFlowTree.js                            # 节点树操作（增删改查/移动）
    ├── useFlowHistory.js                         # 撤销/重做
    └── useFlowZoom.js                            # 缩放/平移
```

### 7.2 删除文件

| 文件 | 原因 |
|---|---|
| `components/bpmn/FlowModeler.vue` | 被 DingFlowDesigner 替代 |
| `components/bpmn/BpmnModeler.vue` | 被 DingFlowDesigner 替代 |
| `components/bpmn/CustomRenderer.js` | 不再需要 bpmn-js 自定义渲染 |
| `components/bpmn/CanvasBackground.js` | 钉钉样式不需要画布背景 |
| `components/bpmn/AutoLayout.js` | 改用 converter/AutoLayout.js |
| `components/bpmn/NodePropertiesPanel.vue` | 配置逻辑迁移到 panel/ |
| `components/bpmn/Minimap.vue` | 钉钉样式不需要小地图 |
| `components/bpmn/QuickActionRing.vue` | 改用 NodeAdder |
| `components/bpmn/ShortcutsBar.vue` | 钉钉样式不需要快捷操作栏 |
| `components/bpmn/ProcessDiagramViewer.vue` | 被 DingFlowViewer 替代 |
| `components/bpmn/InteractiveProcessDiagram.vue` | 被 DingFlowViewer 替代 |
| `components/bpmn/flowable-moddle.json` | 迁移到 converter/ |

### 7.3 保留文件

| 文件 | 说明 |
|---|---|
| `components/bpmn/UserSelectModal.vue` | 用户选择弹窗，配置抽屉复用 |
| `components/flow/FlowTimeline.vue` | 流程时间轴，不变 |
| `components/flow/FlowStats.vue` / `FlowModelStats.vue` | 流程统计，不变 |
| `components/flow/SignaturePad.vue` | 手写签名，不变 |

### 7.4 修改文件

| 文件 | 改动内容 |
|---|---|
| `views/flow/design.vue` | 替换 FlowModeler→DingFlowDesigner，替换 NodePropertiesPanel→NodeConfigDrawer，bpmnXml→flowJson，加载/保存/AI 逻辑适配 |
| `views/flow/todo.vue` | 替换 ProcessDiagramViewer→DingFlowViewer |
| `views/flow/done.vue` | 替换 ProcessDiagramViewer→DingFlowViewer |
| `views/flow/started.vue` | 替换 ProcessDiagramViewer→DingFlowViewer |
| `views/flow/monitor.vue` | 替换 ProcessDiagramViewer→DingFlowViewer |
| `package.json` | 移除 bpmn-js / bpmn-js-properties-panel / inherits-browser 依赖 |

## 8. 风险与关注点

> ⚠️ 本次改造不涉及资金/状态流转/权限变更，但涉及流程定义数据格式转换，需确保转换无损。

### 8.1 技术风险

| 风险 | 影响 | 缓解措施 |
|---|---|---|
| BPMN→JSON→XML 往返不等价 | 已有流程模型编辑后部署可能丢失配置 | 转换层完整复刻 NodePropertiesPanel 的读写逻辑；编写往返测试用例；advanced 节点 rawXml 兜底 |
| UserTask 属性映射遗漏 | 审批人/会签配置丢失 | 逐字段对照 flowable-moddle.json 的 24 个属性，确保双向转换完整覆盖 |
| 图形坐标生成错误 | Flowable 管理界面流程图显示异常 | 自动布局算法生成标准 BPMNDiagram；保存后用 Flowable 管理界面验证 |
| 分支汇合点识别错误 | 分支流程结构错乱 | 分支链跟踪算法 + 多入节点检测；复杂分支场景测试用例 |
| 移除 bpmn-js 后其他模块引用 | 编译错误 | 全局搜索 bpmn-js 引用，确认仅在 bpmn/ 目录下使用 |

### 8.2 兼容性风险

| 风险 | 影响 | 缓解措施 |
|---|---|---|
| 已有复杂 BPMN 模型（循环/跳转/多入节点）无法转为树形 JSON | 加载到设计器后结构错乱 | advanced 节点兜底，保留 rawXml；加载时检测复杂结构并提示用户 |
| AI 生成的 BPMN XML 格式不规范 | XML→JSON 转换失败 | 转换失败时保留原始 XML 展示在 AI 面板，提示用户手动修正 |
| 子流程内部复杂结构 | 子流程编辑后内部丢失 | 子流程整体映射为 subProcess 节点，内部未识别元素走 advanced 兜底 |

## 8.5 测试策略

- **测试范围**：
  - 转换层：BPMN XML ↔ JSON 双向转换的完整性（节点类型、属性、分支、会签、监听器）
  - 画布交互：节点添加/删除/移动、分支操作、撤销/重做、缩放/平移
  - 配置抽屉：各节点类型配置的保存/加载、字段校验
  - 查看器：节点状态渲染、状态颜色/图标映射
  - 集成：design.vue 加载/保存/AI 生成全流程
  - 兼容性：已有流程模型的往返转换
- **覆盖率目标**：
  - 转换层：100%（核心逻辑，必须全覆盖）
  - composables：80%
  - 组件交互：核心场景覆盖（添加/删除/配置/保存/加载）
- **独立 Test Spec**：是
- **关键测试用例**：
  - 线性流程往返：XML→JSON→XML 语义等价
  - 条件分支流程往返：分支条件、默认分支保留
  - 并行分支流程往返：分支汇聚正确
  - 会签节点往返：parallel/sequential、all/any/ratio 全覆盖
  - 审批人类型全覆盖：assignee(custom/spel/静态变量)、candidateUsers、candidateGroups
  - 监听器往返：taskListener + executionListener
  - advanced 兜底：未识别元素 rawXml 保留
  - AI 生成流程加载：XML→JSON 正确渲染

## 9. 待澄清

- [x] 改造范围：设计器 + 配置面板 + 查看器（全部）
- [x] 节点类型：保留全部 BPMN 节点
- [x] 数据格式：JSON 编辑 + 双向转换
- [x] AI 生成：保留并适配
- [x] 节点配置交互：右侧抽屉
- [x] 已有模型兼容：完全兼容
- [x] 技术路线：完全自研（Vue3 + NaiveUI）
- [x] 画布渲染：混合渲染（HTML 节点 + SVG 连线）
- [x] 复杂 BPMN 处理：树形 JSON + advanced 节点兜底

## 10. 技术决策

### 10.1 数据结构：nodes+edges 有向图 JSON

**决策**：采用 nodes + edges 有向图结构，而非纯树形结构。

**原因**：
- BPMN 本身是有向图，树形结构无法表达多入节点和跳转
- 有向图 JSON 可以无损表达所有 BPMN 结构
- 布局上通过算法强制纵向自上而下，视觉上呈现钉钉树形效果
- 分支用 branchId 关联，布局算法按 branchId 横向分组

**节点结构**：
```js
{
  id: 'node_1',
  nodeType: 'approver',            // start|approver|carbonCopy|condition|parallel|inclusive|service|script|subProcess|callActivity|end|advanced
  name: '部门经理审批',
  bpmnElementId: 'Task_1',
  bpmnElementType: 'bpmn:UserTask',
  rawXml: null,                    // advanced 节点兜底
  config: { /* 各节点类型独有配置 */ }
}
```

**连线结构**：
```js
{
  id: 'edge_1',
  source: 'node_1',
  target: 'node_2',
  bpmnElementId: 'Flow_1',
  conditionType: 'expression',     // 仅排他/包容网关出线
  condition: '${days > 3}',
  isDefault: false,
  branchId: 'b1'                   // 分支标识，布局用
}
```

### 10.2 画布渲染：HTML 节点 + SVG 连线混合渲染

**决策**：节点用 HTML div 卡片，连线用 SVG path 覆盖层。

**原因**：
- HTML 节点交互最简单（点击/右键/拖拽/表单嵌入）
- SVG 连线精确（贝塞尔曲线）、支持缩放
- 两者在同一 transform 容器内，坐标同步
- 钉钉/企业微信审批均采用此方案

**架构**：
```
FlowCanvas.vue (position: relative, overflow: hidden)
├─ transform 层 (scale + translate)
│  ├─ HTML 节点层 (absolute 定位，坐标来自布局算法)
│  └─ SVG 连线层 (absolute, pointer-events: none，贝塞尔曲线)
└─ 工具栏覆盖层 (absolute, 不受 transform 影响)
```

### 10.3 节点类型映射

| 钉钉 nodeType | BPMN elementType | 说明 |
|---|---|---|
| `start` | `bpmn:StartEvent` | 发起人节点 |
| `approver` | `bpmn:UserTask` | 审批人节点 |
| `carbonCopy` | `bpmn:ServiceTask` + flowable:type=cc | 抄送人节点 |
| `condition` | `bpmn:ExclusiveGateway` | 条件分支 |
| `parallel` | `bpmn:ParallelGateway` | 并行分支 |
| `inclusive` | `bpmn:InclusiveGateway` | 包容分支 |
| `service` | `bpmn:ServiceTask` | 服务任务 |
| `script` | `bpmn:ScriptTask` | 脚本任务 |
| `subProcess` | `bpmn:SubProcess` | 子流程 |
| `callActivity` | `bpmn:CallActivity` | 调用活动 |
| `end` | `bpmn:EndEvent` | 结束节点 |
| `advanced` | 任意 | 兜底：保留 rawXml |

### 10.4 节点配置迁移策略

**决策**：将 NodePropertiesPanel.vue（2655行）按 Tab 拆分为独立组件，零功能丢失。

**迁移映射**：

| 现有 properties 字段 | 迁移到 | 组件 |
|---|---|---|
| taskType / assignee / assigneeExpr / candidateUsers / candidateGroups / assigneeUserName / candidateUserNames / candidateGroupNames | 基本信息 Tab | ApproverConfig.vue |
| multiInstanceType / completionCondition / passRate | 会签配置 Tab | MultiInstanceConfig.vue |
| allowApprove / allowReject / allowDelegate / allowReturn / allowTerminate / requireSignature / requireComment | 操作权限 Tab | ApproverConfig.vue |
| formType / formKey / formJson / formUrl + 字段权限 | 表单权限 Tab | FormPermissionConfig.vue |
| taskListeners / executionListeners | 监听器 Tab | ListenerConfig.vue |
| priority / dueDate | 基本信息 Tab | ApproverConfig.vue |
| implementationType / implementation / async | 服务配置 | ServiceConfig.vue |
| script / scriptFormat | 脚本配置 | ServiceConfig.vue |
| hasCondition / conditionType / condition / isDefault | 条件配置 | ConditionConfig.vue |
| initiator | 发起人配置 | ApproverConfig.vue |
| endType | 结束配置 | ApproverConfig.vue |

### 10.5 BPMN→JSON 转换关键规则

**UserTask 属性提取**（复刻 NodePropertiesPanel.vue:1562-1640）：

1. 读取 `flowable:assigneeType`：
   - `'spel'` → taskType='assignee', assignee='spel', assigneeExpr=值
   - `'${user_xxx}'` → taskType='assignee', assignee='custom'
   - `'${initiator}'` / `'${initiatorLeader}'` / `'${deptManager}'` / `'${hr}'` → taskType='assignee', assignee=原值
2. 读取 `flowable:candidateUsers` → taskType='candidateUsers'，逗号分隔转数组
3. 读取 `flowable:candidateGroups` → taskType='candidateGroups'，逗号分隔转数组
4. 读取 `<multiInstanceLoopCharacteristics>`：
   - `isSequential=true` → multiInstanceType='sequential'
   - `isSequential=false` → multiInstanceType='parallel'
   - `completionCondition` → 解析 passRate：
     - `== 1` → all (100%)
     - `>= 1` → any (1人)
     - `>= 0.N` → ratio (N%)
5. 读取 `<extensionElements>` 下的 `flowable:taskListener` / `flowable:executionListener`
6. 读取 `flowable:formKey` / `flowable:formJson` / `flowable:formUrl`
7. 读取 `flowable:allowApprove` 等 6 个操作权限布尔值

**网关分支跟踪算法**：
```
1. 找到网关的所有出边 sequenceFlow
2. 每条出边生成一个 branch：
   - conditionExpression → branches[].condition
   - flowable:default → isDefault=true
3. 跟踪每条分支的后续节点链，直到遇到汇合点（多条分支汇聚到同一节点）
4. 汇合点标记为 mergeNode

function traceBranch(headNodeId, allEdges):
  chain = [headNodeId]
  current = headNodeId
  loop:
    nextEdges = edges.filter(e => e.source == current)
    if nextEdges.length != 1: break
    next = nextEdges[0].target
    if hasMultipleInputs(next): break
    chain.push(next)
    current = next
  return chain
```

### 10.6 JSON→BPMN 转换关键规则

**UserTask 属性写入**：

1. taskType='assignee' + assignee='custom' → `flowable:assignee='${user_xxx}'`
2. taskType='assignee' + assignee='spel' → `flowable:assignee=assigneeExpr`, `flowable:assigneeType='spel'`
3. taskType='candidateUsers' → `flowable:candidateUsers='id1,id2'`
4. taskType='candidateGroups' → `flowable:candidateGroups='roleId1,roleId2'`
5. multiInstanceType='parallel' → `<multiInstanceLoopCharacteristics isSequential="false">` + completionCondition 按 passRate 生成
6. taskListeners → `<extensionElements><flowable:taskListener .../></extensionElements>`
7. allowApprove 等 → `flowable:allowApprove="true"` 属性

**图形坐标自动布局**：
```
布局参数：
  NODE_WIDTH = 180, NODE_HEIGHT = 70
  V_GAP = 60, H_GAP = 40, BRANCH_WIDTH = 220

算法：
1. 从 start 节点开始，y=0
2. 线性链：y 递增 NODE_HEIGHT + V_GAP，x 居中
3. 遇到分支节点：计算各分支宽度（递归）→ 分支横向并排 → 每条分支内部递归布局
4. 汇合节点：x 回到中线
5. 连线坐标：source 底边中点 → target 顶边中点
6. 生成 <BPMNDiagram> + <BPMNShape> + <BPMNEdge>
```

### 10.7 design.vue 数据流改造

**现有数据流**：
```
后端 → bpmnXml(字符串) → FlowModeler → bpmn-js 内部模型 → 保存时 getXML() → bpmnXml → 后端
```

**新数据流**：
```
后端 → bpmnXml(字符串) → BpmnToJsonConverter → flowJson(JSON) → DingFlowDesigner
                                                                              ↓
                                                    保存时 JsonToBpmnConverter → bpmnXml → 后端
```

**关键改动**：
- `bpmnXml` ref → `flowJson` ref
- `loadModel`：`bpmnXml = res.data.bpmnXml` → `flowJson = BpmnToJsonConverter.convert(res.data.bpmnXml)`
- `handleSaveDraft`：`modelerRef.getXML()` → `JsonToBpmnConverter.convert(flowJson)`
- AI 生成：流式完成后 `BpmnToJsonConverter.convert(aiXml)` → flowJson
- 顶部工具栏/表单配置/版本管理/AI面板容器：不变

### 10.8 流程查看器改造

**DingFlowViewer** 替代 ProcessDiagramViewer + InteractiveProcessDiagram：

1. 调用 `getProcessDiagramInfo(processInstanceId)` 获取 `{ bpmnXml, nodeStatuses }`
2. `BpmnToJsonConverter.convert(bpmnXml)` → flowJson
3. 将 nodeStatuses 按 bpmnElementId 匹配到 flowJson.nodes
4. 渲染钉钉样式卡片流，每个节点叠加状态

**节点状态映射**：

| 节点状态 | 图标 | 卡片边框 | 连线颜色 | 动画 |
|---|---|---|---|---|
| completed | check-circle | 绿色 `#10b981` | 绿色实线 | 无 |
| running | sync | 蓝色 `#3b82f6` | — | 脉冲发光 |
| pending | circle-outline | 灰色 `#cbd5e1` | 灰色虚线 | 无 |
| rejected | cancel | 红色 `#ef4444` | 红色虚线 | 无 |
| skipped | dash | 灰色 `#94a3b8` | 灰色虚线 | 无 |

## 11. 执行日志

| Task | 状态 | 实际改动文件 | 备注 |
|---|---|---|---|
| Task 0 目录骨架与依赖确认 | done (2026-06-17) | `forge-admin-ui/src/components/flow-designer/{index.js, canvas/, nodes/, panel/, viewer/, converter/, composables/, constants/, utils/}` 共 9 个占位 index.js | 不新增依赖；旧 `components/bpmn/` 不动 |
| Task 1 XML 解析工具与节点类型识别 | done (2026-06-17) | `converter/xml-utils.js`、`constants/node-types.js`、3 个 __tests__ 文件、`vitest.config.js`、`package.json`（+vitest 2.1.9 / jsdom 25 / @vitest/ui） | TDD：3 spec / 39 用例通过；不依赖 bpmn-js / moddle |
| Task 2 BPMN→JSON 转换器（基础节点） | done (2026-06-17) | `converter/bpmn-to-json.js` + 4 个 spec | 16 用例通过；advanced 节点 rawXml 兜底 |
| Task 3 UserTask 完整属性提取 | done (2026-06-17) | `converter/user-task-parser.js` + 4 个 spec | 27 用例通过；assignee / multiInstance / listener / 7 权限 / form 全覆盖 |
| Task 4 网关分支与汇合识别 | done (2026-06-17) | `converter/branch-parser.js` + spec | 7 用例通过；branchId 单调分配 + default 标记 + mergeNode |
| Task 5 JSON→BPMN + 自动布局 | done (2026-06-17) | `converter/json-to-bpmn.js` + `user-task-writer.js` + `layout-algorithm.js` + `completion-condition.js` + `xml-escape.js` + 4 个 spec | 25 用例通过（含 4 用例 roundtrip）；BPMNPlane.bpmnElement 强制指向 process（修正 pitfalls #8） |
| Task 6 端到端往返测试 | done* (2026-06-17) | `converter/__tests__/roundtrip.spec.js` | 4 用例：线性 / 排他分支 / 会签 ratio / 三种 completionCondition；fixture 文件待 Phase 6 集成测试需要时再补 |
| Task 7 useFlowDesigner Composable | done (2026-06-17) | `composables/useFlowDesigner.js` + `flow-designer-helpers.js` + `utils/id-generator.js` + `constants/default-configs.js` + 3 个 spec | 29 用例通过；CRUD + 查询 + 整体加载；deletePlain 笛卡儿重连保留入边语义；deleteGateway 递归删除分支链 |
| Task 8 useFlowHistory | done (2026-06-17) | `composables/useFlowHistory.js` + spec | 13 用例通过；JSON 快照命令栈 + maxStack + Ctrl/Cmd+Z/Y/Shift+Z 键盘绑定 |
| Task 9 FlowCanvas + viewport | done (2026-06-17) | `composables/useCanvasViewport.js` + `canvas/FlowCanvas.vue` + 2 个 spec | 18 用例通过；锚点缩放公式 + 4 种平移触发 + transform 容器；鼠标交互留 Phase 9 端到端 |
| Task 10 画布布局引擎 | done (2026-06-17) | `canvas/layout-engine.js` + spec | 8 用例通过；复用 converter/layout-algorithm + 边类型识别 + canvasBounds |
| Task 11 SVG 连线层 | done (2026-06-17) | `utils/path-builder.js` + `canvas/EdgePath.vue` + `canvas/EdgeLayer.vue` + 3 spec | 24 用例通过；4 种 path 类型 + 5 种状态箭头 + "默认" 标签 |
| Task 12 节点添加菜单 | done (2026-06-17) | `constants/node-menu.js` + `canvas/AddNodeButton.vue` + `canvas/AddNodePopover.vue` + spec | 7 用例通过；3 分组 9 节点类型；click outside 自包含实现 |
| Task 13-17 Phase 4 节点组件 | done (2026-06-17) | `nodes/NodeCard.vue` + 11 节点卡片 + `canvas/{BranchHeader,MergeNode,NodeContextMenu,NodeRenderer}.vue` + `utils/approver-summary.js` + 3 spec | 51 用例通过；NodeCard 基类复用样式；NodeRenderer 12 种类型调度 + 兜底；approver-summary 覆盖 4 种 assignee 模式 + 多实例 |
| Task 18-25 Phase 5 配置抽屉 | done (2026-06-17) | `panel/NodeConfigDrawer.vue` + `BasicConfig.vue` + 11 个节点专属配置 + 5 个子表单 + `config-renderer-map.js` + spec | 5 用例通过；19 个 Vue 组件；ApproverConfig 拆 5 Tab；ConditionConfig 处理网关出边 |
| Task 26-27 Phase 6 主组件集成 | done (2026-06-17) | `flow-designer/DingFlowDesigner.vue` + `__tests__/` + `views/flow/{design,template}.vue` 替换 import 与使用 | 8 用例通过；接口 1:1 兼容 FlowModeler；pitfalls #7 防回环 + #8 不再是问题；端到端可见里程碑 ✓ |
| 阶段累计：Phase 0-6 完成 | 31 spec / 275 用例通过 | — | 进入 Phase 7（DingFlowViewer 查看器） |
| Task 28-29 Phase 7 流程查看器 | done (2026-06-17) | `viewer/{DingFlowViewer.vue, NodeStatusBadge.vue, NodeDetailPopover.vue}` + 2 个 spec | 8 用例通过；接口与 ProcessDiagramViewer 兼容（processInstanceId / compact）+ 直接 bpmnXml 模式；nodeInstanceList → nodeStatusMap 映射 + 节点点击弹出详情 |
| 阶段累计：Phase 0-7 完成 | 33 spec / 283 用例通过 | — | 进入 Phase 8（todo / done / started / monitor 入口替换） |
| Task 30-31 Phase 8 入口替换 | done (2026-06-17) | `views/flow/{todo,done,started,monitor}.vue` + `components/ai-form/AiCrudFlowDetail.vue` 5 处 import 改为 DingFlowViewer | 业务模板零修改（仅替换 import 路径，局部变量名 `ProcessDiagramViewer` 保留）；lint 本次变更文件零错误；全量单测 33/283 通过 |
| 阶段累计：Phase 0-8 完成 | 33 spec / 283 用例通过 | — | 进入 Phase 9（移除 bpmn-js + 构建冒烟） |
| Task 32-34 Phase 9 收尾 | done (2026-06-17) | 移除 bpmn-js 等 7 依赖 + 删除 components/bpmn/ 14 文件 + UserSelectModal 迁 common/ + version.vue 直接 bpmn-js → DingFlowViewer + design.vue NodePropertiesPanel 死代码清理 | pnpm build 成功（52.27s，DingFlowDesigner 独立 chunk 47KB/gzip 13.58KB）；全量单测 33/283 通过；lint 本次变更零错误 |
| **🎉 变更完成：Phase 0-9 全部 done** | 33 spec / 283 用例 / 35 Task | — | 端到端可见 + 构建冒烟 + 依赖清理全部通过 |

## 12. 审查结论

待 /review 阶段填写。

## 13. 确认记录（HARD-GATE）

- **确认时间**：
- **确认人**：
- **确认内容**：
