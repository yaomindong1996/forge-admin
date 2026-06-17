# 任务清单：dingflow-approver-style-designer

> status: proposed
> created: 2026-06-17
> 拆分顺序：基线 → 转换层（最高风险，先解决） → 数据/状态 → 画布渲染 → 节点组件 → 配置面板 → 设计器集成 → 查看器 → 入口替换 → 依赖清理 → 验收
> 原则：后端 Flowable 接口零改动；BPMN XML 双向无损转换；保留全部 BPMN 节点类型（advanced 兜底）；UserSelectModal 复用；NodePropertiesPanel 字段 1:1 迁移。

## 前置条件

- [x] 替换范围：FlowModeler.vue + NodePropertiesPanel.vue + ProcessDiagramViewer.vue + InteractiveProcessDiagram.vue 全替换
- [x] 数据结构：nodes + edges 有向图 JSON
- [x] 渲染方式：HTML 节点卡片 + SVG 连线覆盖层
- [x] 复杂 BPMN 兜底：advanced 节点保留 rawXml
- [x] 12 种节点类型：start/approver/carbonCopy/condition/parallel/inclusive/service/script/subProcess/callActivity/end/advanced
- [x] design.vue 数据流：bpmnXml ref → flowJson ref
- [x] 查看器节点状态：5 种状态 + 颜色 + 动画规则
- [x] AI 生成：流式完成后 XML→JSON 加载

## 阶段总览

| 阶段 | 目标 | 包含任务 | 交付结果 |
|------|------|----------|----------|
| Phase 0 | 基线 & 目录 | Task 0 | 目录结构 + 依赖确认 |
| Phase 1 | 转换层 | Task 1-6 | BPMN↔JSON 双向转换 + 自动布局 + 单测 |
| Phase 2 | 数据与状态 | Task 7-8 | flowJson Composable + 撤销/重做 |
| Phase 3 | 画布渲染 | Task 9-12 | FlowCanvas + 布局 + SVG 连线 + 添加菜单 |
| Phase 4 | 节点组件 | Task 13-17 | 12 种节点卡片 + 操作菜单 |
| Phase 5 | 配置面板 | Task 18-25 | 抽屉容器 + 8 类配置组件 |
| Phase 6 | 设计器集成 | Task 26-27 | DingFlowDesigner + design.vue 接入 |
| Phase 7 | 查看器 | Task 28-29 | DingFlowViewer + 状态/详情 |
| Phase 8 | 入口替换 | Task 30-31 | todo/done/started/monitor 等页替换 |
| Phase 9 | 清理与验收 | Task 32-34 | 移除 bpmn-js + 构建 + 冒烟 |

## 任务总览

| Task | 阶段 | 名称 | 状态 | 优先级 |
|------|------|------|------|--------|
| Task 0 | Phase 0 | 目录骨架与依赖确认 | done | P0 |
| Task 1 | Phase 1 | XML 解析工具与节点类型识别 | done | P0 |
| Task 2 | Phase 1 | BPMN→JSON 转换器（基础节点） | done | P0 |
| Task 3 | Phase 1 | BPMN→JSON UserTask 完整属性提取 | done | P0 |
| Task 4 | Phase 1 | BPMN→JSON 网关分支与汇合识别 | done | P0 |
| Task 5 | Phase 1 | JSON→BPMN 转换器 + 自动布局 | done | P0 |
| Task 6 | Phase 1 | 转换层端到端往返测试 | done* | P0 |
| Task 7 | Phase 2 | useFlowDesigner Composable | done | P0 |
| Task 8 | Phase 2 | useFlowHistory（撤销/重做） | done | P1 |
| Task 9 | Phase 3 | FlowCanvas 容器（缩放/平移） | done | P0 |
| Task 10 | Phase 3 | 布局算法（纵向 + 分支横向） | done | P0 |
| Task 11 | Phase 3 | SVG 连线层（贝塞尔曲线） | done | P0 |
| Task 12 | Phase 3 | 节点间"+"号添加菜单 | done | P0 |
| Task 13 | Phase 4 | StartNode / EndNode 卡片 | done | P0 |
| Task 14 | Phase 4 | ApproverNode / CarbonCopyNode 卡片 | done | P0 |
| Task 15 | Phase 4 | Condition/Parallel/Inclusive 分支卡片 | done | P0 |
| Task 16 | Phase 4 | Service/Script/SubProcess/CallActivity 卡片 | done | P1 |
| Task 17 | Phase 4 | AdvancedNode + 节点右键菜单 | done | P0 |
| Task 18 | Phase 5 | NodeConfigDrawer 抽屉 + Tab 路由 | done | P0 |
| Task 19 | Phase 5 | ApproverConfig（基本+权限） | done | P0 |
| Task 20 | Phase 5 | MultiInstanceConfig（会签） | done | P0 |
| Task 21 | Phase 5 | FormPermissionConfig（表单权限） | done | P0 |
| Task 22 | Phase 5 | ListenerConfig（监听器） | done | P1 |
| Task 23 | Phase 5 | ConditionConfig（条件分支） | done | P0 |
| Task 24 | Phase 5 | Service/Script/Start/End Config | done | P1 |
| Task 25 | Phase 5 | AdvancedConfig（rawXml 只读） | done | P1 |
| Task 26 | Phase 6 | DingFlowDesigner 主组件 | done | P0 |
| Task 27 | Phase 6 | design.vue 数据流改造 | done | P0 |
| Task 28 | Phase 7 | DingFlowViewer 查看器 | done | P0 |
| Task 29 | Phase 7 | 节点状态高亮 + Popover | done | P0 |
| Task 30 | Phase 8 | todo/done/started/monitor 替换 | done | P0 |
| Task 31 | Phase 8 | 版本对比/历史查看入口替换 | done | P1 |
| Task 32 | Phase 9 | 移除 bpmn-js 依赖与旧组件 | done | P0 |
| Task 33 | Phase 9 | 前端构建 + Lint | done | P0 |
| Task 34 | Phase 9 | 端到端冒烟 | done | P0 |

---

## Phase 0：基线 & 目录搭建

### Task 0: 目录骨架与依赖确认 ✅ done (2026-06-17)

**目标**: 创建组件目录结构，确认现有依赖（NaiveUI/UnoCSS/Vue3）足够，不引入新依赖。

**实际产物**: 9 个 index.js 占位文件（含模块说明 JSDoc），无新增依赖，无修改/删除既有文件。详见 `execution-log.md`。


**涉及文件**:
- `forge-admin-ui/src/components/flow-designer/canvas/index.js` — 新建空导出
- `forge-admin-ui/src/components/flow-designer/nodes/index.js` — 新建空导出
- `forge-admin-ui/src/components/flow-designer/panel/index.js` — 新建空导出
- `forge-admin-ui/src/components/flow-designer/viewer/index.js` — 新建空导出
- `forge-admin-ui/src/components/flow-designer/converter/index.js` — 新建空导出
- `forge-admin-ui/src/components/flow-designer/composables/index.js` — 新建空导出
- `forge-admin-ui/src/components/flow-designer/constants/index.js` — 新建空导出
- `forge-admin-ui/src/components/flow-designer/utils/index.js` — 新建空导出
- `forge-admin-ui/src/components/flow-designer/index.js` — 新建，最终导出 DingFlowDesigner / DingFlowViewer

**验收标准**:
- 全部目录创建，每个目录含占位 `index.js`
- `package.json` 不新增依赖
- 旧 `src/components/bpmn/` 不动（待 Phase 9 删除）

---

## Phase 1：转换层（核心）

> 转换层为最高技术风险区域，所有任务必须配 Vitest 单测，先写测试再写实现（TDD）。

### Task 1: XML 解析工具与节点类型识别 ✅ done (2026-06-17)

**目标**: 封装基于 DOMParser 的 XML 工具与节点类型映射常量。

**实际产物**:
- `converter/xml-utils.js` — DOMParser 封装、命名空间常量、flowable 属性三路径解析、扩展元素查找、documentation 读取
- `constants/node-types.js` — NODE_TYPE 12 枚举 + `bpmnTypeToNodeType()` + 反向映射 `NODE_TYPE_TO_BPMN_LOCAL_NAME`
- `converter/__tests__/xml-utils.spec.js` + `xml-utils-flowable.spec.js` + `constants/__tests__/node-types.spec.js`：39 个单测全部通过
- 同步引入 `vitest@2.1.9 + @vitest/ui + jsdom`，新增 `vitest.config.js` 与 npm scripts（test / test:watch / test:ui）

**单测命令**: `NODE_ENV=development pnpm vitest run src/components/flow-designer` → 3 文件 / 39 用例通过

**涉及文件**:
- `forge-admin-ui/src/components/flow-designer/converter/xml-utils.js` — 新建
- `forge-admin-ui/src/components/flow-designer/constants/node-types.js` — 新建
- `forge-admin-ui/src/components/flow-designer/converter/__tests__/xml-utils.spec.js` — 新建

**关键签名**:
```js
// xml-utils.js
export function parseBpmnXml(xmlString)         // → Document
export function getRootProcess(doc)             // → Element
export function getChildren(el, tagName)        // → Element[]
export function getAttr(el, name)               // → string|null
export function getFlowableAttr(el, name)       // → string|null
export function getExtensionElement(el, tagName)// → Element|null
export function serializeXml(doc)               // → string

// node-types.js
export const NODE_TYPE = {
  START: 'start', APPROVER: 'approver', CARBON_COPY: 'carbonCopy',
  CONDITION: 'condition', PARALLEL: 'parallel', INCLUSIVE: 'inclusive',
  SERVICE: 'service', SCRIPT: 'script', SUB_PROCESS: 'subProcess',
  CALL_ACTIVITY: 'callActivity', END: 'end', ADVANCED: 'advanced'
}
export function bpmnTypeToNodeType(bpmnElement) // → NODE_TYPE
```

**节点类型识别规则**:
- bpmn:StartEvent → start
- bpmn:EndEvent → end
- bpmn:UserTask → approver
- bpmn:ServiceTask + flowable:type='cc' → carbonCopy
- bpmn:ServiceTask 其他 → service
- bpmn:ScriptTask → script
- bpmn:ExclusiveGateway → condition
- bpmn:ParallelGateway → parallel
- bpmn:InclusiveGateway → inclusive
- bpmn:SubProcess → subProcess
- bpmn:CallActivity → callActivity
- 其他（IntermediateEvent/BoundaryEvent/ManualTask/...） → advanced

**验收标准**:
- 单测覆盖 12 种类型识别
- 单测覆盖 flowable:* 属性读取
- 单测覆盖 extensionElements 子节点查找

---

### Task 2: BPMN→JSON 转换器（基础节点） ✅ done (2026-06-17)

**目标**: 实现 BPMN XML → flowJson 转换骨架，支持线性流程，UserTask/网关详细解析在 Task 3、4 实现。

**实际产物**:
- `converter/bpmn-to-json.js` — 主入口 `convertBpmnToJson` + `parseNode` + `parseEdge` + `buildAdvancedNode`（advanced 兜底）
- 4 个 spec 文件 / 16 用例：linear / multi / condition / advanced 全部通过

**关键设计**: NON_NODE_TAGS 排除 sequenceFlow / extensionElements 等结构性元素；XMLSerializer 失败时回退 outerHTML。

**涉及文件**:
- `forge-admin-ui/src/components/flow-designer/converter/bpmn-to-json.js` — 新建
- `forge-admin-ui/src/components/flow-designer/converter/__tests__/bpmn-to-json.spec.js` — 新建

**关键签名**:
```js
export function convertBpmnToJson(xmlString) {
  // → { processId, processName, nodes: [], edges: [] }
}

function parseNode(element)        // → flowNode
function parseEdge(sequenceFlow)   // → flowEdge
function buildAdvancedNode(element)// → flowNode (rawXml 兜底)
```

**flowJson 结构**:
```js
{
  processId: 'Process_1',
  processName: '请假流程',
  nodes: [
    { id, nodeType, name, bpmnElementId, bpmnElementType, rawXml: null, config: {} }
  ],
  edges: [
    { id, source, target, bpmnElementId, conditionType, condition, isDefault, branchId }
  ]
}
```

**本任务实现的节点类型属性**:
- StartEvent / EndEvent：name、documentation、formKey
- ServiceTask（service）：name、async、implementation、implementationType、class、expression、delegateExpression
- ScriptTask：name、scriptFormat、script
- SubProcess / CallActivity：name、calledElement
- 未知类型：advanced 节点（rawXml 保存原始 outerHTML）

**验收标准**:
- 单测：3 节点线性流程（start→service→end）转换正确
- 单测：未知 BPMN 元素生成 advanced 节点（rawXml 不为空）
- 单测：documentation 文档字符串正确读取
- 单测：sequenceFlow 转 edges 数量、source、target 正确

---

### Task 3: BPMN→JSON UserTask 完整属性提取 ✅ done (2026-06-17)

**目标**: 1:1 复刻 `NodePropertiesPanel.vue:1562-1640` 的 UserTask 属性提取逻辑。

**实际产物**:
- `converter/user-task-parser.js` — `parseUserTaskConfig()` + `parseCompletionExpression()`
- 4 个 spec 文件 / 27 用例：assignee 4 种模式 + multiInstance 3 种完成条件 + 7 权限 + listener 3 类型 + form 三类型 + priority/dueDate

**字段对齐**: 与现有 NodePropertiesPanel.properties 同名同语义；唯一差异是 spec 用 `'ratio'` 替换现有 `'rate'`（按 spec 优先级处理）。

**涉及文件**:
- `forge-admin-ui/src/components/flow-designer/converter/user-task-parser.js` — 新建
- `forge-admin-ui/src/components/flow-designer/converter/bpmn-to-json.js` — 修改：UserTask 调用 parseUserTaskConfig
- `forge-admin-ui/src/components/flow-designer/converter/__tests__/user-task-parser.spec.js` — 新建

**关键签名**:
```js
export function parseUserTaskConfig(taskElement) {
  // → {
  //   taskType, assignee, assigneeExpr, candidateUsers, candidateGroups,
  //   assigneeUserName, candidateUserNames, candidateGroupNames,
  //   priority, dueDate, formType, formKey, formJson, formUrl,
  //   allowApprove, allowReject, allowDelegate, allowReturn, allowTerminate,
  //   requireSignature, requireComment,
  //   multiInstanceType, completionCondition, passRate,
  //   taskListeners: [], executionListeners: [],
  //   formFieldPermissions: []
  // }
}
```

**assignee 解析规则**:
1. 读 `flowable:assigneeType`：
   - 'spel' → taskType='assignee', assignee='spel', assigneeExpr=`flowable:assignee` 值
   - 否则按下一步
2. 读 `flowable:assignee`：
   - `${initiator}` / `${initiatorLeader}` / `${deptManager}` / `${hr}` → taskType='assignee', assignee=该静态值
   - `${user_xxx}` 或其他 → taskType='assignee', assignee='custom'
3. 读 `flowable:candidateUsers`（逗号分隔字符串） → taskType='candidateUsers', candidateUsers=数组
4. 读 `flowable:candidateGroups`（逗号分隔字符串） → taskType='candidateGroups', candidateGroups=数组

**multiInstance 解析**:
- 子元素 `bpmn:multiInstanceLoopCharacteristics`：
  - 不存在 → multiInstanceType='none'
  - isSequential='true' → 'sequential'
  - isSequential='false' → 'parallel'
- 子元素 `bpmn:completionCondition` 文本：
  - `${nrOfCompletedInstances/nrOfInstances == 1}` → passRate=100, condition='all'
  - `${nrOfCompletedInstances >= 1}` → passRate=null, condition='any'
  - `${nrOfCompletedInstances/nrOfInstances >= 0.N}` → passRate=N*100, condition='ratio'

**listener 解析**:
- `extensionElements > flowable:taskListener`：`{event, type, value}[]`，type ∈ class|expression|delegateExpression
- `extensionElements > flowable:executionListener`：同上

**操作权限**:
- `flowable:allowApprove` / `allowReject` / `allowDelegate` / `allowReturn` / `allowTerminate` / `requireSignature` / `requireComment`：字符串 'true'/'false' → boolean

**验收标准**:
- 单测覆盖 4 种 assignee 模式（spel/custom/static/candidates）
- 单测覆盖 3 种 multiInstance 完成条件
- 单测覆盖 listener 多个 event 提取
- 单测覆盖 7 个操作权限字段
- 与现有 NodePropertiesPanel 用相同 XML 提取结果对比一致（手工 fixture）

---

### Task 4: BPMN→JSON 网关分支与汇合识别 ✅ done (2026-06-17)

**目标**: 解析 sequenceFlow 上的 conditionExpression 与 default，给分支分配 branchId，识别多入度汇合节点。

**实际产物**:
- `converter/branch-parser.js` — `markBranches()` + `getNodeInDegree` + `getNodeOutDegree`
- `branch-parser.spec.js` / 7 用例：排他/并行/嵌套分支 + branchId 唯一 + mergeNode 标记 + in/outDegree 正确

**实现细节**: branchSeq 单调递增分配 b1/b2/...；网关 default 属性匹配的 sequenceFlow 自动 isDefault=true 并清空 condition；入度 ≥ 2 的节点 mark 到 `node.config.mergeNode=true`。

**涉及文件**:
- `forge-admin-ui/src/components/flow-designer/converter/branch-parser.js` — 新建
- `forge-admin-ui/src/components/flow-designer/converter/bpmn-to-json.js` — 修改：转换完成后调用 markBranches
- `forge-admin-ui/src/components/flow-designer/converter/__tests__/branch-parser.spec.js` — 新建

**关键签名**:
```js
export function markBranches(flowJson) {
  // 1. 找出所有网关节点（exclusive/parallel/inclusive）
  // 2. 网关出边按 XML 顺序分配 branchId（b1, b2, ...）
  // 3. 标识 default 边：网关 default 属性引用的 sequenceFlow → edge.isDefault=true
  // 4. 提取 conditionExpression 文本（去除 ${} 包裹）→ edge.condition
  //    含 'js'/'groovy' 等 language 属性 → edge.conditionType='script', 否则 'expression'
  // 5. 计算每个 node 的入度，>=2 标记为 mergeNodes
  // 返回 flowJson（mutate）
}

export function getNodeInDegree(nodes, edges)  // → Map<nodeId, number>
export function getNodeOutDegree(nodes, edges) // → Map<nodeId, number>
```

**验收标准**:
- 单测：if-else 排他网关，2 条出边分别得到 b1/b2，default 边 isDefault=true
- 单测：并行网关 3 分支，全部 edges 有 branchId，无 condition
- 单测：包容网关混合 default + condition
- 单测：分支汇合节点正确识别（入度=2）
- 单测：嵌套分支（分支内再分支）branchId 不冲突

---

### Task 5: JSON→BPMN 转换器 + 自动布局算法 ✅ done (2026-06-17)

**目标**: 将 flowJson 序列化为合法的 BPMN 2.0 XML，包含 process 元素 + extensionElements + BPMNDiagram 图形坐标。

**实际产物**:
- `converter/json-to-bpmn.js` — `convertJsonToBpmn()` 主入口 + writeNode / writeEdge / writeDiagram
- `converter/user-task-writer.js` — UserTask config → flowable:* 属性 + 子元素串
- `converter/layout-algorithm.js` — `calculateLayout()`：纵向 + 分支横向并排 + 汇合回中线 + 孤立节点放右侧
- `converter/completion-condition.js` — `buildCompletionExpression()`（与 parseCompletionExpression 对偶）
- `converter/xml-escape.js` — 属性 / 文本节点转义工具
- 4 个 spec / 25 用例：completion / layout / json-to-bpmn / roundtrip

**关键决策**:
- 用字符串拼接 + 显式转义而非 XMLSerializer，确保命名空间前缀稳定；advanced 节点 rawXml 直接写出确保二次往返无损
- 默认权限值省略不写（与 Flowable 默认行为对齐，避免 XML 体积膨胀）
- BPMNPlane.bpmnElement 强制指向 root process id（修正 pitfalls #8）

**涉及文件**:
- `forge-admin-ui/src/components/flow-designer/converter/json-to-bpmn.js` — 新建
- `forge-admin-ui/src/components/flow-designer/converter/user-task-writer.js` — 新建（UserTask 属性写回）
- `forge-admin-ui/src/components/flow-designer/converter/layout-algorithm.js` — 新建（生成 BPMNShape/BPMNEdge 坐标）
- `forge-admin-ui/src/components/flow-designer/converter/__tests__/json-to-bpmn.spec.js` — 新建
- `forge-admin-ui/src/components/flow-designer/converter/__tests__/layout-algorithm.spec.js` — 新建

**关键签名**:
```js
// json-to-bpmn.js
export function convertJsonToBpmn(flowJson) {
  // → BPMN 2.0 XML 字符串，含 <bpmn:definitions> + <bpmn:process> + <bpmndi:BPMNDiagram>
}

function writeNode(node)              // → XML 片段
function writeEdge(edge)              // → XML 片段
function writeAdvancedNode(node)      // → 直接拼接 node.rawXml
function writeUserTask(node)          // → 调用 writeUserTaskConfig

// user-task-writer.js
export function writeUserTaskConfig(node)
// 按规则将 node.config 写回为 XML 属性 + 子元素

// layout-algorithm.js
export function calculateLayout(flowJson) {
  // → { nodePositions: Map<nodeId, {x,y}>, edgeWaypoints: Map<edgeId, [{x,y}...]> }
}
```

**UserTask 写回规则**（与 Task 3 解析对偶）:
- taskType='assignee' + assignee='custom' → `flowable:assignee="${user_xxx}"` (从 candidateUsers[0] 拼)
- taskType='assignee' + assignee='spel' → `flowable:assignee=assigneeExpr`, `flowable:assigneeType="spel"`
- taskType='assignee' + assignee 为 static（initiator/initiatorLeader/deptManager/hr） → `flowable:assignee="${value}"`
- taskType='candidateUsers' → `flowable:candidateUsers="id1,id2"`
- taskType='candidateGroups' → `flowable:candidateGroups="roleId1,roleId2"`
- multiInstanceType ≠ 'none' → `<multiInstanceLoopCharacteristics isSequential="...">` + completionCondition：
  - condition='all' → `${nrOfCompletedInstances/nrOfInstances == 1}`
  - condition='any' → `${nrOfCompletedInstances >= 1}`
  - condition='ratio' → `${nrOfCompletedInstances/nrOfInstances >= 0.N}`
- taskListeners → `<extensionElements><flowable:taskListener event="..." class|expression|delegateExpression="..."/>`
- 操作权限 → `flowable:allowApprove="true"` 等属性
- formFieldPermissions → `<extensionElements><flowable:formProperty>` 子元素（兼容现有格式）

**布局算法**（layout-algorithm.js）:
```
常量: NODE_WIDTH=180, NODE_HEIGHT=70, V_GAP=60, H_GAP=40

算法（递归）:
1. 从 start 节点开始，y=0，x=0（中线）
2. 线性链：next.y = current.y + NODE_HEIGHT + V_GAP, next.x = current.x
3. 网关节点：
   a. 计算每个分支的子树宽度：treeWidth(branch) = max(branchWidth, NODE_WIDTH)
   b. 总宽 totalWidth = sum(branchWidths) + (n-1)*H_GAP
   c. 各分支起始 x = current.x - totalWidth/2 + offset
   d. 递归布局每个分支
   e. 汇合节点 x 回到 current.x
4. edge.waypoints：source 底边中点 → target 顶边中点（含拐点）
5. 处理 advanced 节点：放在最末尾区域（避免破坏布局）
6. 处理孤立节点（无连接）：放在右侧边缘
```

**验收标准**:
- 单测：线性流程往返一致（XML→JSON→XML，关键字段不变）
- 单测：UserTask 全字段写回（与 Task 3 fixture 对偶）
- 单测：3 分支并行网关布局，分支节点 x 不重叠
- 单测：嵌套分支布局，inner 不超出 outer 边界
- 单测：advanced 节点 rawXml 原样输出
- 单测：BPMNDiagram 包含全部 BPMNShape + BPMNEdge

---

### Task 6: 转换层端到端往返测试 ✅ done* (2026-06-17)

**目标**: 用真实 BPMN XML 做往返一致性验证，确保关键字段无损。

**实际产物**:
- `converter/__tests__/roundtrip.spec.js` / 4 用例覆盖：
  - 线性流程（assignee + 非默认权限保留）
  - 排他分支（default + condition + 网关 default 属性保留）
  - 会签流程（candidateUsers + ratio 60% 解析回来一致）
  - all / any / ratio 三种 completionCondition 二次往返一致

**注**: 标记 done* 因为暂未引入完整 leave/expense/complex BPMN fixture 文件；现有 4 个 inline fixture 已覆盖关键往返场景。后续若 Phase 6 集成测试发现真实流程模型有遗漏字段，再增补 `fixtures/*.bpmn`。

**涉及文件**:
- `forge-admin-ui/src/components/flow-designer/converter/__tests__/roundtrip.spec.js` — 新建
- `forge-admin-ui/src/components/flow-designer/converter/__tests__/fixtures/leave-process.bpmn` — 新建（请假流程，含会签/分支）
- `forge-admin-ui/src/components/flow-designer/converter/__tests__/fixtures/expense-process.bpmn` — 新建（报销流程，含包容/抄送）
- `forge-admin-ui/src/components/flow-designer/converter/__tests__/fixtures/complex-process.bpmn` — 新建（含子流程/调用活动/服务任务）
- `forge-admin-ui/src/components/flow-designer/converter/__tests__/fixtures/edge-cases.bpmn` — 新建（含 BoundaryEvent/IntermediateEvent，验证 advanced 兜底）

**测试矩阵**:
| Fixture | 验证点 |
|---|---|
| leave-process | 起止 + 审批人 + 并行会签（passRate=100） + 排他分支 |
| expense-process | 包容网关 + 抄送 + 多 listener |
| complex-process | 子流程 + 调用活动 + 服务任务 + 脚本任务 |
| edge-cases | 边界事件/中间事件落入 advanced，rawXml 保留完整 |

**断言策略**:
1. `xml1 → json → xml2`，对 `xml1` 与 `xml2` 进行结构化比对（忽略空白/注释/属性顺序）
2. 关键字段逐字段断言：assignee、candidateUsers、completionCondition、conditionExpression、listener
3. JSON 结构断言：节点/边数量、nodeType 分布、advanced 数量
4. BPMNDiagram 必须存在且节点坐标非负

**验收标准**:
- 4 个 fixture 单测全部通过
- 关键字段 100% 一致
- advanced 节点 rawXml 在第二次往返后仍保持原 XML 片段
- 测试运行：`cd forge-admin-ui && pnpm vitest run src/components/flow-designer/converter`

---

## Phase 2：数据与状态

### Task 7: useFlowDesigner Composable ✅ done (2026-06-17)

**目标**: 提供 flowJson 状态容器与 CRUD 操作 API，所有节点/连线变更走该 Composable。

**实际产物**:
- `composables/useFlowDesigner.js` — flowJson reactive + 节点/边 CRUD + 查询 + 整体加载导出
- `composables/flow-designer-helpers.js` — deletePlain（笛卡儿重连）+ deleteGateway（递归删除分支链）
- `utils/id-generator.js` — `createIdGenerator()` + `collectExistingIds()`
- `constants/default-configs.js` — 12 种 nodeType 默认 config 工厂 + buildNode 节点骨架
- 3 个 spec / 29 用例：useFlowDesigner（20）+ helpers（3）+ id-generator（6）

**关键算法**:
- addNode：保留上游 condition / branchId / isDefault，下游重置
- deleteNode 普通节点：入边 × 出边笛卡儿重连，保留入边语义
- deleteNode 网关节点：沿分支链向下追到汇合点，把入边接过去

**涉及文件**:
- `forge-admin-ui/src/components/flow-designer/composables/useFlowDesigner.js` — 新建
- `forge-admin-ui/src/components/flow-designer/utils/id-generator.js` — 新建（生成 nodeId/edgeId/branchId）
- `forge-admin-ui/src/components/flow-designer/constants/default-configs.js` — 新建（每种节点类型默认 config）
- `forge-admin-ui/src/components/flow-designer/composables/__tests__/useFlowDesigner.spec.js` — 新建

**关键签名**:
```js
export function useFlowDesigner(initialJson) {
  const flowJson = ref(initialJson || createEmptyFlow())
  const selectedNodeId = ref(null)

  // 节点操作
  function addNode(afterNodeId, nodeType, config = {})  // 在指定节点后插入
  function addBranchNode(gatewayId, branchType)         // 给网关添加分支
  function updateNode(nodeId, patch)                    // 浅合并 config
  function deleteNode(nodeId)                           // 删除节点 + 连接边（自动重连前后）
  function moveNodeUp(nodeId) / moveNodeDown(nodeId)    // 同分支内调整顺序
  function copyNode(nodeId)                             // 复制节点（不含子分支）

  // 连线操作
  function updateEdge(edgeId, patch)
  function reconnect(edgeId, newSource, newTarget)

  // 查询
  function getNode(nodeId) / getEdge(edgeId)
  function getOutgoingEdges(nodeId) / getIncomingEdges(nodeId)
  function findEndNode() / findStartNode()

  // 全量
  function loadJson(json)
  function exportJson()                                 // → 深拷贝
  function reset()

  return { flowJson, selectedNodeId, addNode, ... }
}

export function createEmptyFlow() {
  // → 默认 start → end 两节点 + 一条 edge
}
```

**addNode 算法**:
1. 找到 afterNodeId 的所有出边
2. 创建新节点 newNode（id=generateNodeId()）
3. 创建新边 e1: afterNodeId → newNode（继承原 condition/branchId）
4. 创建新边 e2: newNode → 原出边 target
5. 删除原出边
6. push newNode 到 nodes、push e1/e2 到 edges、删除原边

**deleteNode 算法**:
1. start/end 不允许删除（throw）
2. 找到入边 incoming、出边 outgoing
3. 创建新边 connecting incoming.source → outgoing.target（继承 incoming 的 condition/branchId）
4. 网关节点：递归删除其所有分支链
5. 删除节点本身

**验收标准**:
- 单测：addNode 在线性流程中插入审批节点，出入边正确
- 单测：deleteNode 中间节点，前后自动连接
- 单测：addBranchNode 给网关加第 3 条分支
- 单测：deleteNode 网关节点，所有分支节点同时删除
- 单测：moveNodeUp/Down 同分支内顺序调整
- 单测：start/end 删除抛错
- 单测：updateNode 浅合并 config 不影响其他字段

---

### Task 8: useFlowHistory（撤销/重做） ✅ done (2026-06-17)

**目标**: 基于 JSON 快照的命令栈，支持 Ctrl+Z / Ctrl+Y。

**实际产物**:
- `composables/useFlowHistory.js` — undo/redo 命令栈 + maxStack 限制 + Ctrl/Cmd+Z/Y/Shift+Z 键盘绑定
- `composables/__tests__/useFlowHistory.spec.js` — 13 用例覆盖快照 / 撤销 / 重做 / maxStack / 键盘绑定 / unbind / 深拷贝隔离

**特性**:
- shallowRef + 整体替换的设计天然支持快照（拿到的是稳定整树）
- bindKeyboard 接受任意 EventTarget，便于单测；同时支持 Cmd（Mac）/Ctrl（Win/Linux）/Shift+Z 二级 redo

**涉及文件**:
- `forge-admin-ui/src/components/flow-designer/composables/useFlowHistory.js` — 新建
- `forge-admin-ui/src/components/flow-designer/composables/__tests__/useFlowHistory.spec.js` — 新建

**关键签名**:
```js
export function useFlowHistory(flowJsonRef, options = { maxStack: 50 }) {
  const undoStack = ref([])
  const redoStack = ref([])

  function snapshot()         // 推入 undoStack（深拷贝 JSON），清空 redoStack
  function undo()             // pop undoStack 推入 redoStack，恢复 flowJsonRef
  function redo()             // pop redoStack 推入 undoStack，恢复 flowJsonRef
  function canUndo()          // → boolean
  function canRedo()          // → boolean
  function clear()
  function bindKeyboard(el)   // 绑定 Ctrl+Z / Ctrl+Y 监听，返回 unbind 函数

  return { snapshot, undo, redo, canUndo, canRedo, clear, bindKeyboard }
}
```

**调用约定**:
- 在 useFlowDesigner 的每个变更操作前调用 snapshot()
- 拖拽中不快照，drop 完成后才快照（节流）

**验收标准**:
- 单测：3 次操作后撤销 2 次，flowJson 恢复正确
- 单测：redo 后再操作清空 redoStack
- 单测：栈深度超过 maxStack 自动丢弃最早项
- 单测：bindKeyboard 模拟 keydown 触发 undo/redo

---

## Phase 3：画布渲染

### Task 9: FlowCanvas 容器（缩放/平移/坐标系） ✅ done (2026-06-17)

**目标**: 提供画布容器组件，支持 transform 缩放与平移，鼠标滚轮 zoom，按住空格拖拽 pan。

**实际产物**:
- `composables/useCanvasViewport.js` — scale/translate reactive + zoomIn/Out/锚点缩放/pan/fitToScreen/坐标转换
- `canvas/FlowCanvas.vue` — 画布容器 + Ctrl/Cmd 滚轮 + 中键/空格拖拽 + 双击 reset + 浮动工具栏
- 2 个 spec / 18 用例

**注**: 鼠标 / 空格 / 中键拖拽行为在 jsdom 下无法稳定触发，留 Phase 9 端到端冒烟覆盖。

**涉及文件**:
- `forge-admin-ui/src/components/flow-designer/canvas/FlowCanvas.vue` — 新建
- `forge-admin-ui/src/components/flow-designer/composables/useCanvasViewport.js` — 新建（缩放/平移状态）

**关键签名**:
```vue
<!-- FlowCanvas.vue -->
<template>
  <div class="flow-canvas" ref="containerRef"
       @wheel="handleWheel" @mousedown="handlePan" @contextmenu.prevent>
    <div class="canvas-transform" :style="transformStyle">
      <slot name="edges" />   <!-- SVG 连线层 -->
      <slot name="nodes" />   <!-- HTML 节点层 -->
    </div>
    <div class="canvas-toolbar"><!-- 缩放工具栏 --></div>
  </div>
</template>

<script setup>
const props = defineProps({
  minScale: { type: Number, default: 0.3 },
  maxScale: { type: Number, default: 2.0 }
})
defineExpose({ resetView, fitToScreen, zoomIn, zoomOut, screenToCanvas, canvasToScreen })
</script>
```

**useCanvasViewport.js**:
```js
export function useCanvasViewport(options) {
  const scale = ref(1)
  const translateX = ref(0)
  const translateY = ref(0)
  const transformStyle = computed(() => `transform: translate(${translateX.value}px, ${translateY.value}px) scale(${scale.value})`)

  function zoomIn() / zoomOut() / setScale(s, centerX, centerY)
  function pan(dx, dy)
  function resetView()
  function fitToScreen(contentWidth, contentHeight, viewportWidth, viewportHeight)
  function screenToCanvas(x, y)  // → 画布坐标
  function canvasToScreen(x, y)  // → 屏幕坐标
}
```

**交互规则**:
- 鼠标滚轮 + Ctrl/Cmd：以鼠标位置为中心缩放
- 鼠标滚轮（无修饰键）：垂直滚动平移
- 按住空格 + 鼠标拖拽：平移画布（鼠标变 grab）
- 按住中键拖拽：平移
- 双击空白：reset view

**验收标准**:
- 缩放范围 0.3-2.0
- 滚轮缩放以鼠标位置为锚点（不偏移）
- 空格 + 拖拽平移流畅
- 工具栏显示当前缩放百分比，提供 +/-/100%/适应屏幕按钮
- 画布尺寸自适应父容器

---

### Task 10: 布局算法（纵向卡片流 + 分支横向并排） ✅ done (2026-06-17)

**目标**: 实现钉钉风格的纵向布局，纯 JS 计算每个节点的 (x,y)。

**实际产物**:
- `canvas/layout-engine.js` — 复用 `converter/layout-algorithm.js` 计算节点位置 + 边 polyline，再做画布特化（边类型识别 + canvasBounds）
- `canvas/__tests__/layout-engine.spec.js` / 8 用例

**复用决策**: 转换层与画布层共享几何计算，避免重复实现；BPMNDiagram 与画布坐标一致。

**涉及文件**:
- `forge-admin-ui/src/components/flow-designer/canvas/layout-engine.js` — 新建（与 Task 5 layout-algorithm.js 不同：本任务用于画布渲染坐标，含连线 polyline 计算）
- `forge-admin-ui/src/components/flow-designer/canvas/__tests__/layout-engine.spec.js` — 新建

**关键签名**:
```js
export function layoutFlow(flowJson, options = {}) {
  // → {
  //   nodePositions: Map<nodeId, {x, y, width, height}>,
  //   edgePaths: Map<edgeId, {points: [{x,y}...], type: 'straight'|'bezier'|'orthogonal'}>,
  //   canvasBounds: {minX, minY, maxX, maxY}
  // }
}

const DEFAULT_OPTIONS = {
  NODE_WIDTH: 220, NODE_HEIGHT: 80,
  V_GAP: 50,                    // 上下节点间距
  H_GAP: 40,                    // 分支间水平间距
  BRANCH_HEAD_GAP: 30,          // 网关到分支首节点距离
  BRANCH_MERGE_GAP: 30          // 分支末节点到汇合节点距离
}
```

**算法（递归遍历）**:
```
function layout(currentNodeId, x, y, ctx):
  if visited[currentNodeId]: return
  visited[currentNodeId] = true
  ctx.positions[currentNodeId] = {x, y}

  outgoing = edges where source = currentNodeId
  if outgoing.length == 0: return  // 终点

  if outgoing.length == 1:
    // 线性
    next = outgoing[0].target
    layout(next, x, y + NODE_HEIGHT + V_GAP, ctx)
    return

  // 分支节点
  branchWidths = outgoing.map(e => measureBranch(e.target, edges))
  totalWidth = sum(branchWidths) + (n-1) * H_GAP
  startX = x + NODE_WIDTH/2 - totalWidth/2

  for i, edge in outgoing:
    branchX = startX + sum(branchWidths[0..i]) + i * H_GAP + branchWidths[i]/2 - NODE_WIDTH/2
    layout(edge.target, branchX, y + NODE_HEIGHT + BRANCH_HEAD_GAP, ctx)

  // 找到汇合节点（branches 第一个共同后继）
  mergeNodeId = findMerge(outgoing.map(e => e.target), edges)
  if mergeNodeId:
    mergeY = max(branchEndY) + V_GAP
    ctx.positions[mergeNodeId] = {x, y: mergeY}

function measureBranch(headId, edges) → number  // 递归计算分支总宽
```

**连线 polyline 规则**:
- 同 x 直线：直接 source 底→ target 顶
- 不同 x（分支/汇合）：3 段折线（垂直→水平→垂直），中点 y 在两节点中间
- 连接到分支头节点：从网关底中点出发，水平到分支 x，再垂直到节点顶

**验收标准**:
- 单测：线性 5 节点，y 递增固定差值
- 单测：3 分支并行，分支 x 不重叠
- 单测：嵌套分支 2 层，inner 不超出 outer 范围
- 单测：汇合节点 x 等于网关 x
- 单测：循环引用检测（visited 防死循环）
- 单测：advanced 节点放在最右侧

---

### Task 11: SVG 连线层（贝塞尔曲线 + 状态着色） ✅ done (2026-06-17)

**目标**: 渲染所有 edges 为 SVG path，支持平滑曲线、箭头、条件标签、状态色。

**实际产物**:
- `utils/path-builder.js` — 4 种 path 类型（straight/orthogonal/rounded/bezier）+ 中点计算 + 状态颜色 / 虚线
- `canvas/EdgePath.vue` — 单条边 + 状态箭头 + 条件标签（截断 20 字符）
- `canvas/EdgeLayer.vue` — SVG 容器 + 5 种箭头 marker
- 3 个 spec / 24 用例

**涉及文件**:
- `forge-admin-ui/src/components/flow-designer/canvas/EdgeLayer.vue` — 新建
- `forge-admin-ui/src/components/flow-designer/canvas/EdgePath.vue` — 新建（单条边）
- `forge-admin-ui/src/components/flow-designer/utils/path-builder.js` — 新建

**关键签名**:
```vue
<!-- EdgeLayer.vue -->
<template>
  <svg class="edge-layer" :width="canvasWidth" :height="canvasHeight">
    <defs>
      <marker id="arrow-default" .../>
      <marker id="arrow-completed" .../>
      <marker id="arrow-rejected" .../>
    </defs>
    <EdgePath v-for="edge in edges" :key="edge.id" :edge="edge" :path="paths.get(edge.id)" :status="getStatus(edge)" />
  </svg>
</template>

<script setup>
defineProps({ edges: Array, paths: Map, nodeStatuses: Object })
</script>

<!-- EdgePath.vue -->
<template>
  <g>
    <path :d="pathD" :stroke="strokeColor" :stroke-dasharray="dashArray" marker-end="url(#arrow-default)" />
    <text v-if="edge.condition" :x="midX" :y="midY" class="edge-label">{{ shortLabel }}</text>
  </g>
</template>
```

**path-builder.js**:
```js
export function buildPathD(points, type = 'orthogonal') {
  // type='straight': M x1,y1 L x2,y2
  // type='bezier':   M x1,y1 C cx1,cy1 cx2,cy2 x2,y2
  // type='orthogonal': M x1,y1 L midX,y1 L midX,y2 L x2,y2  (折线)
  // type='rounded': 折线 + 圆角（Q 控制点）
}

export function getEdgeMidpoint(points)         // → {x, y}
export function getEdgeColor(edge, status)      // → hex color
```

**状态着色**:
- 默认：`#94a3b8`
- 已完成：`#10b981` 实线
- 进行中：`#3b82f6` 实线 + 脉冲
- 待执行：`#cbd5e1` 虚线
- 驳回：`#ef4444` 虚线
- 默认分支：`#f59e0b` + label "默认"

**验收标准**:
- SVG 渲染 10+ 节点流程不卡顿
- 条件文本超长（>20 字）截断 + tooltip
- 默认边显示 "默认" 标签
- 箭头方向正确，箭头颜色与线一致
- 不同状态颜色清晰可辨

---

### Task 12: 节点间"+"号添加菜单 ✅ done (2026-06-17)

**目标**: 在每个节点下方/分支首端显示"+"按钮，点击弹出节点类型选择菜单。

**实际产物**:
- `constants/node-menu.js` — NODE_MENU_GROUPS 3 分组 9 类型（advanced 不在添加菜单，仅作为加载兼容兜底）
- `canvas/AddNodePopover.vue` — 节点类型选择面板 + allowTypes 过滤
- `canvas/AddNodeButton.vue` — 画布上 "+" 按钮 + 弹出 Popover + click outside
- `canvas/__tests__/AddNodeButton.spec.js` / 7 用例

**实现细节**: click outside 用 mousedown capture 阶段 + closest('.add-node-button-wrap') 判定，不依赖 Naive Popover。

**涉及文件**:
- `forge-admin-ui/src/components/flow-designer/canvas/AddNodeButton.vue` — 新建
- `forge-admin-ui/src/components/flow-designer/canvas/AddNodePopover.vue` — 新建
- `forge-admin-ui/src/components/flow-designer/constants/node-menu.js` — 新建

**关键签名**:
```vue
<!-- AddNodeButton.vue -->
<NPopover trigger="click" placement="bottom">
  <template #trigger><button class="add-node-btn"><i class="i-mdi-plus" /></button></template>
  <AddNodePopover @select="handleSelect" :allow-types="allowTypes" />
</NPopover>
```

**node-menu.js**（菜单分组）:
```js
export const NODE_MENU_GROUPS = [
  { label: '审批流', items: [
    { type: 'approver', label: '审批人', icon: 'i-mdi-account-check' },
    { type: 'carbonCopy', label: '抄送人', icon: 'i-mdi-email' }
  ]},
  { label: '分支', items: [
    { type: 'condition', label: '条件分支', icon: 'i-mdi-source-branch' },
    { type: 'parallel', label: '并行分支', icon: 'i-mdi-call-split' },
    { type: 'inclusive', label: '包容分支', icon: 'i-mdi-set-merge' }
  ]},
  { label: '高级', items: [
    { type: 'service', label: '服务任务', icon: 'i-mdi-cog' },
    { type: 'script', label: '脚本任务', icon: 'i-mdi-code-tags' },
    { type: 'subProcess', label: '子流程', icon: 'i-mdi-sitemap' },
    { type: 'callActivity', label: '调用活动', icon: 'i-mdi-phone-forward' }
  ]}
]
```

**显示位置规则**:
- 线性节点：节点下方居中
- 网关分支首端：每条分支首位置一个按钮
- 汇合节点之前：合并显示一个
- end 节点：不显示

**验收标准**:
- 鼠标悬停在节点边上显示按钮，离开 300ms 后隐藏
- 点击弹出菜单分组清晰
- 选中后调用 useFlowDesigner.addNode，画布刷新
- 添加完成自动选中新节点（弹出配置抽屉）

---

## Phase 4：节点组件

> 所有节点组件统一 props：`{ node, status, selected }`，emit：`select`/`action`。使用 UnoCSS 类，遵循 `code-copilot/rules/button-style-guide.md`。

### Task 13: StartNode / EndNode 卡片

**目标**: 起止节点卡片样式，圆角徽章风格，与中间节点视觉区分。

**涉及文件**:
- `forge-admin-ui/src/components/flow-designer/nodes/BaseNode.vue` — 新建（基础卡片骨架）
- `forge-admin-ui/src/components/flow-designer/nodes/StartNode.vue` — 新建
- `forge-admin-ui/src/components/flow-designer/nodes/EndNode.vue` — 新建

**关键签名**:
```vue
<!-- BaseNode.vue -->
<template>
  <div class="flow-node" :class="[`node-${nodeType}`, { selected, [`status-${status}`]: status }]"
       @click.stop="$emit('select', node.id)" @contextmenu.prevent="$emit('action', { type: 'menu', event: $event })">
    <div class="node-header"><i :class="iconClass" /><span>{{ node.name }}</span></div>
    <div class="node-body"><slot /></div>
    <div class="node-status" v-if="status"><i :class="statusIcon" /></div>
  </div>
</template>
```

**样式规范**:
- StartNode：左侧绿色竖条 4px，圆角 12px，背景白
- EndNode：左侧灰色竖条 4px，圆角 12px，背景灰
- 选中：`shadow-lg ring-2 ring-primary`
- 状态：completed 绿边、running 蓝边+脉冲、rejected 红边、pending 灰虚线、skipped 灰

**验收标准**:
- StartNode 显示发起人摘要（"所有人可发起" / "指定：xxx"）
- EndNode 显示"流程结束"
- 选中边框样式正确
- 5 种状态视觉区分明显

---

### Task 14: ApproverNode / CarbonCopyNode 卡片

**目标**: 审批人/抄送人节点卡片，显示审批模式 + 头像 + 操作摘要。

**涉及文件**:
- `forge-admin-ui/src/components/flow-designer/nodes/ApproverNode.vue` — 新建
- `forge-admin-ui/src/components/flow-designer/nodes/CarbonCopyNode.vue` — 新建
- `forge-admin-ui/src/components/flow-designer/utils/approver-summary.js` — 新建

**关键签名**:
```js
// approver-summary.js
export function getApproverSummary(config) {
  // → { mode, modeLabel, names, namesLabel, avatars, multi, passRateLabel, missing }
}
```

**摘要规则**:
- assignee='custom'：显示候选人头像组（最多 3 个）
- assignee='spel'：显示 "SPEL：" + 表达式（截断 20 字符）
- assignee 静态变量：发起人/部门经理/HR/发起人上级（中文）
- taskType='candidateUsers'：头像组 + "候选 N 人"
- taskType='candidateGroups'：组名 + "候选角色"
- multiInstanceType !== 'none'：附加会签标签 + 通过率（"全部通过" / "任一通过" / "60%"）
- 缺失审批人：红色感叹号 + "未配置审批人"

**CarbonCopyNode**: 类似但样式青色，标题"抄送给：" + 名字列表。

**验收标准**:
- 5 种 taskType 摘要文字正确
- 会签节点显示通过率
- 候选人 > 3 显示"等 N 人"
- 缺失配置警告样式
- 头像加载失败回退默认图标

---

### Task 15: Condition / Parallel / Inclusive 分支卡片

**目标**: 网关节点 + 分支首部条件徽章 + 汇合点。

**涉及文件**:
- `forge-admin-ui/src/components/flow-designer/nodes/ConditionNode.vue` — 新建
- `forge-admin-ui/src/components/flow-designer/nodes/ParallelNode.vue` — 新建
- `forge-admin-ui/src/components/flow-designer/nodes/InclusiveNode.vue` — 新建
- `forge-admin-ui/src/components/flow-designer/canvas/BranchHeader.vue` — 新建（分支首部条件徽章）
- `forge-admin-ui/src/components/flow-designer/canvas/MergeNode.vue` — 新建（汇合点小圆点）

**视觉规范**:
- ConditionNode：菱形图标 + 橙色 `#f59e0b`
- ParallelNode：双竖线图标 + 紫色 `#8b5cf6`
- InclusiveNode：圆+三横图标 + 紫粉色 `#a855f7`
- BranchHeader：显示"优先级 N"+ 条件表达式（截断 30 字符）+ 默认徽章
- MergeNode：8px 圆点 + 灰色边框

**条件显示**:
- 排他/包容网关：每个分支显示条件表达式或"默认分支"或"未配置条件"
- 并行网关：分支首部仅显示"分支 N"
- 排他网关至少 1 个分支为 default：BranchHeader 黄色背景

**验收标准**:
- 3 种网关图标和颜色正确
- BranchHeader 默认分支高亮
- 缺失条件红色警告
- MergeNode 在所有分支汇合时显示

---

### Task 16: Service / Script / SubProcess / CallActivity 卡片

**目标**: 高级节点卡片，灰色风格 + 类型徽章。

**涉及文件**:
- `forge-admin-ui/src/components/flow-designer/nodes/ServiceNode.vue` — 新建
- `forge-admin-ui/src/components/flow-designer/nodes/ScriptNode.vue` — 新建
- `forge-admin-ui/src/components/flow-designer/nodes/SubProcessNode.vue` — 新建
- `forge-admin-ui/src/components/flow-designer/nodes/CallActivityNode.vue` — 新建

**显示内容**:
- ServiceNode：类型徽章 "Java 类" / "表达式" / "委托表达式" + implementation 内容（截断 30 字符）
- ScriptNode：scriptFormat（如 javascript/groovy）+ 脚本前 50 字符
- SubProcessNode：子流程名称 + 节点数（如有）
- CallActivityNode：calledElement 处理流程 ID

**视觉规范**:
- 全部使用灰色主题 `border-l-4 border-gray-400`
- 图标：cog / code-tags / sitemap / phone-forward
- 标题字号小一号，主体显示技术细节

**验收标准**:
- 4 种节点显示对应技术细节
- 缺失 implementation/script 显示"未配置"
- 长内容截断 + tooltip 显示完整

---

### Task 17: AdvancedNode + 节点右键菜单

**目标**: 兜底节点卡片（保留 rawXml 不可视化编辑）+ 通用右键操作菜单。

**涉及文件**:
- `forge-admin-ui/src/components/flow-designer/nodes/AdvancedNode.vue` — 新建
- `forge-admin-ui/src/components/flow-designer/canvas/NodeContextMenu.vue` — 新建

**AdvancedNode 显示**:
- 顶部黄色警告条："此节点为高级类型（IntermediateEvent/BoundaryEvent/...），仅可编辑名称，详细配置请使用 BPMN 源码模式"
- 显示 bpmnElementType + 节点名
- 双击打开 rawXml 只读查看

**NodeContextMenu**:
```vue
<NDropdown :options="menuOptions" trigger="manual" :show="show" :x="position.x" :y="position.y" @select="handleSelect" @clickoutside="show = false" />
```

**菜单项**（动态计算）:
- 编辑（打开配置抽屉）
- 复制节点（克隆 + 自增 id）
- 上移 / 下移（同分支内）
- 删除（start/end 不可用）
- 查看 rawXml（仅 advanced）
- 取消

**验收标准**:
- AdvancedNode 显示警告条
- 右键弹出菜单，start/end 删除项 disabled
- 上下移正确（同分支首/末禁用）
- 复制节点 ID 自增不冲突
- 删除带二次确认（NPopconfirm）

---

## Phase 5：配置面板

> 配置面板按 Tab 拆分组件，所有字段必须 1:1 迁移自 `forge-admin-ui/src/components/bpmn/NodePropertiesPanel.vue`。配置变更走 useFlowDesigner.updateNode，自动触发 snapshot。

### Task 18: NodeConfigDrawer 抽屉容器 + Tab 路由

**目标**: 提供 NDrawer 抽屉壳，根据 selectedNode.nodeType 动态加载对应 Tab 列表与配置组件。

**涉及文件**:
- `forge-admin-ui/src/components/flow-designer/panel/NodeConfigDrawer.vue` — 新建
- `forge-admin-ui/src/components/flow-designer/panel/tab-resolver.js` — 新建（根据 nodeType 计算 tabs）

**关键签名**:
```vue
<!-- NodeConfigDrawer.vue -->
<template>
  <NDrawer :show="show" :width="480" :on-update:show="$emit('update:show', $event)">
    <NDrawerContent :title="`${node.name} - 配置`" closable>
      <NTabs v-model:value="activeTab" type="line">
        <NTabPane v-for="tab in tabs" :key="tab.key" :name="tab.key" :tab="tab.label">
          <component :is="tab.component" :node="node" @update="handleUpdate" />
        </NTabPane>
      </NTabs>
    </NDrawerContent>
  </NDrawer>
</template>
```

**tab-resolver.js**:
```js
export function resolveTabs(nodeType) {
  // approver → [基本信息, 会签, 操作权限, 表单权限, 监听器]
  // carbonCopy → [基本信息, 抄送对象]
  // condition/inclusive → [分支配置, 监听器]
  // parallel → [基本信息, 监听器]
  // service → [服务配置, 监听器]
  // script → [脚本配置, 监听器]
  // subProcess/callActivity → [基本信息, 监听器]
  // start → [基本信息, 表单, 监听器]
  // end → [基本信息]
  // advanced → [只读 XML]
}
```

**验收标准**:
- 切换不同 nodeType 节点 Tab 自动变化
- handleUpdate 调 useFlowDesigner.updateNode 浅合并
- 抽屉支持 Esc/点击遮罩关闭
- start/end 不显示删除按钮

---

### Task 19: ApproverConfig（基本信息 + 操作权限）

**目标**: 审批人配置面板（taskType + 审批人选择 + 操作权限），1:1 复刻 NodePropertiesPanel.vue:936-1050。

**涉及文件**:
- `forge-admin-ui/src/components/flow-designer/panel/configs/ApproverConfig.vue` — 新建
- `forge-admin-ui/src/components/flow-designer/panel/configs/PermissionConfig.vue` — 新建（操作权限子组件）
- `forge-admin-ui/src/components/flow-designer/panel/widgets/UserPicker.vue` — 新建（封装 UserSelectModal）
- `forge-admin-ui/src/components/flow-designer/panel/widgets/RolePicker.vue` — 新建

**字段**:
- 基本信息：name、taskType（指定/SPEL/候选用户/候选角色/静态变量）、priority、dueDate
- 审批人配置（按 taskType 切换）:
  - taskType='assignee' + assignee 选择：custom/spel/initiator/initiatorLeader/deptManager/hr
  - assignee='custom'：UserPicker 选用户（candidateUsers）
  - assignee='spel'：assigneeExpr 表达式输入框
- candidateUsers：UserPicker 多选
- candidateGroups：RolePicker 多选
- 操作权限：allowApprove/Reject/Delegate/Return/Terminate/requireSignature/requireComment（NCheckbox）

**关键签名**:
```vue
<!-- ApproverConfig.vue -->
<NForm label-placement="left" label-width="100">
  <NFormItem label="节点名称"><NInput v-model:value="local.name" /></NFormItem>
  <NFormItem label="审批人类型">
    <NRadioGroup v-model:value="local.taskType">
      <NRadio value="assignee">指定审批人</NRadio>
      <NRadio value="candidateUsers">候选用户</NRadio>
      <NRadio value="candidateGroups">候选角色</NRadio>
    </NRadioGroup>
  </NFormItem>
  <!-- 按 taskType 切换子表单 -->
</NForm>
```

**复用现有组件**:
- `forge-admin-ui/src/components/bpmn/UserSelectModal.vue` — 用户选择弹窗，封装为 UserPicker
- 角色选择：调 `/system/role/list` 接口（参考 NodePropertiesPanel:1900）

**验收标准**:
- 5 种 taskType 切换显示对应字段
- UserPicker 选中后回显头像 + 姓名
- 操作权限默认值（allowApprove=true，其他 false）
- 字段变更立即触发 emit('update', patch)
- 与现有 NodePropertiesPanel 同字段保存的 BPMN 一致（用 fixture 对比）

---

### Task 20: MultiInstanceConfig（会签配置）

**目标**: 会签配置面板（multiInstanceType + completionCondition + passRate），1:1 复刻 NodePropertiesPanel:1100-1200。

**涉及文件**:
- `forge-admin-ui/src/components/flow-designer/panel/configs/MultiInstanceConfig.vue` — 新建
- `forge-admin-ui/src/components/flow-designer/utils/completion-condition-builder.js` — 新建

**字段**:
- multiInstanceType：none / parallel / sequential（NRadioGroup）
- completionCondition：all / any / ratio（仅 type=parallel 显示）
- passRate：百分比输入（仅 condition=ratio 显示，1-100）

**completion-condition-builder.js**:
```js
export function buildCompletionExpression(condition, passRate) {
  // all → '${nrOfCompletedInstances/nrOfInstances == 1}'
  // any → '${nrOfCompletedInstances >= 1}'
  // ratio → `${nrOfCompletedInstances/nrOfInstances >= ${passRate/100}}`
}

export function parseCompletionExpression(expr) {
  // 反向解析，→ { condition, passRate }
}
```

**联动规则**:
- type=none：清空 completionCondition、passRate
- type=sequential：condition 自动设为 'all'
- type=parallel + condition=all：passRate=100（只读显示）
- type=parallel + condition=any：passRate=null
- type=parallel + condition=ratio：passRate 必填 1-99

**验收标准**:
- 3 种 type 字段联动正确
- ratio 输入校验 1-99
- 保存为 BPMN 后 multiInstanceLoopCharacteristics 表达式正确
- 现有会签流程加载后值回显正确

---

### Task 21: FormPermissionConfig（表单权限）

**目标**: 字段级表单权限（只读/编辑/隐藏），1:1 复刻 NodePropertiesPanel:1300-1400。

**涉及文件**:
- `forge-admin-ui/src/components/flow-designer/panel/configs/FormPermissionConfig.vue` — 新建
- `forge-admin-ui/src/components/flow-designer/panel/configs/FormConfig.vue` — 新建（表单选择 + 字段权限）

**字段**:
- formType：none / formKey（外部表单） / formJson（内嵌） / formUrl（外链）
- formKey：表单 KEY 字符串 + 表单选择器（调 `/flow/form/list`）
- formJson：JSON 文本框（含校验）
- formUrl：URL 字符串
- formFieldPermissions：表格（fieldKey + permission：read/edit/hide）
  - 字段列表自动从 formJson 解析
  - permission 选择：NSelect

**关键签名**:
```vue
<NDataTable :columns="columns" :data="local.formFieldPermissions" :max-height="300" />
```

**验收标准**:
- 4 种 formType 切换显示对应字段
- formJson 解析失败显示红色错误提示
- 字段权限表格行内编辑
- 保存时清理无效字段（表单不存在的字段移除）

---

### Task 22: ListenerConfig（监听器）

**目标**: 任务监听器 + 执行监听器配置，1:1 复刻 NodePropertiesPanel:1450-1550。

**涉及文件**:
- `forge-admin-ui/src/components/flow-designer/panel/configs/ListenerConfig.vue` — 新建
- `forge-admin-ui/src/components/flow-designer/panel/widgets/ListenerForm.vue` — 新建（单个监听器编辑器）

**字段**:
- taskListeners：[{event, type, value}]（仅 UserTask）
  - event：create / assignment / complete / delete
  - type：class / expression / delegateExpression
  - value：字符串
- executionListeners：[{event, type, value}]（所有节点 + sequenceFlow）
  - event：start / end / take（take 仅 sequenceFlow）

**UI**:
- NDynamicInput 模式：每行一个 ListenerForm 子组件
- 每行：3 个下拉/输入 + 删除按钮
- 顶部"添加监听器"按钮

**验收标准**:
- 任务监听器仅 UserTask 节点显示
- event 选项按节点类型过滤（sequenceFlow 才显示 take）
- 添加/删除流畅
- 与现有 NodePropertiesPanel 保存格式一致

---

### Task 23: ConditionConfig（条件分支）

**目标**: 条件分支配置（分支列表管理 + 条件表达式 + 默认分支），1:1 复刻 NodePropertiesPanel:1600-1700。

**涉及文件**:
- `forge-admin-ui/src/components/flow-designer/panel/configs/ConditionConfig.vue` — 新建
- `forge-admin-ui/src/components/flow-designer/panel/widgets/BranchEditor.vue` — 新建

**字段**（针对网关节点的所有出边）:
- 分支列表（出边数组）：每行显示 priority + condition + isDefault + 操作（上移/下移/删除）
- 单条分支编辑：
  - name（边名称）
  - conditionType：expression / script
  - condition：表达式或脚本内容（CodeMirror）
  - isDefault：仅一条可勾选

**关键签名**:
```vue
<NList>
  <NListItem v-for="(edge, idx) in branches" :key="edge.id">
    <BranchEditor :edge="edge" :index="idx" :branches="branches" @update="updateBranch" @move="moveBranch" @remove="removeBranch" />
  </NListItem>
</NList>
<NButton @click="addBranch">添加分支</NButton>
```

**操作规则**:
- 添加分支：插入新出边到 edges，target 为新建空节点
- 删除分支：仅剩 1 条时禁用
- 设为默认：取消其他默认
- 排他网关：默认分支必须有 1 条
- 包容网关：默认分支可选

**验收标准**:
- 分支增删改查正常
- 默认分支唯一性校验
- 表达式 CodeMirror 高亮（支持 `${...}` 语法）
- 顺序调整影响 BPMN sequenceFlow 顺序

---

### Task 24: Service / Script / Start / End Config

**目标**: 其他节点配置面板（服务任务、脚本任务、起止节点）。

**涉及文件**:
- `forge-admin-ui/src/components/flow-designer/panel/configs/ServiceConfig.vue` — 新建（ServiceTask）
- `forge-admin-ui/src/components/flow-designer/panel/configs/ScriptConfig.vue` — 新建（ScriptTask）
- `forge-admin-ui/src/components/flow-designer/panel/configs/StartConfig.vue` — 新建（StartEvent）
- `forge-admin-ui/src/components/flow-designer/panel/configs/EndConfig.vue` — 新建（EndEvent）
- `forge-admin-ui/src/components/flow-designer/panel/configs/SubProcessConfig.vue` — 新建
- `forge-admin-ui/src/components/flow-designer/panel/configs/CallActivityConfig.vue` — 新建

**字段映射**:
- ServiceConfig：name + implementationType（class/expression/delegateExpression）+ implementation + async + 输入/输出参数
- ScriptConfig：name + scriptFormat（javascript/groovy/python）+ script（CodeMirror）+ resultVariable
- StartConfig：name + initiator（all/specifiedRole/specifiedUser）+ formType + formKey/formJson
- EndConfig：name + endType（normal/terminate）
- SubProcessConfig：name + 是否多实例 + 内部流程定义（链接到子流程编辑）
- CallActivityConfig：name + calledElement（流程定义 KEY）+ 输入/输出参数映射

**验收标准**:
- 6 种节点配置面板字段齐全
- ScriptConfig CodeMirror 支持多种语言高亮
- StartConfig 发起人配置可选指定用户/角色
- SubProcess 链接到子流程编辑（占位，子流程编辑后续迭代）
- 与 NodePropertiesPanel 字段名 1:1 对齐

---

### Task 25: AdvancedConfig（rawXml 只读展示）

**目标**: 高级节点（advanced）的只读 rawXml 查看面板。

**涉及文件**:
- `forge-admin-ui/src/components/flow-designer/panel/configs/AdvancedConfig.vue` — 新建

**字段**:
- 仅显示：bpmnElementType + 警告信息
- rawXml CodeMirror 只读展示（XML 高亮）
- "复制 XML"按钮
- 提示文字："此节点为高级 BPMN 类型，可视化设计器暂不支持编辑。如需修改，请使用源码模式或联系开发者。"

**验收标准**:
- rawXml 完整显示，支持折叠
- 复制功能正常
- 节点 name 字段可编辑（仅文本属性）
- 保存后 advanced 节点 rawXml 不变

---

## Phase 6：设计器集成

### Task 26: DingFlowDesigner 主组件

**目标**: 整合画布 + 节点组件 + 配置抽屉，对外暴露与 FlowModeler 兼容的接口（通过 v-model:modelValue）。

**涉及文件**:
- `forge-admin-ui/src/components/flow-designer/DingFlowDesigner.vue` — 新建
- `forge-admin-ui/src/components/flow-designer/index.js` — 修改：导出 DingFlowDesigner

**关键签名**:
```vue
<!-- DingFlowDesigner.vue -->
<template>
  <div class="ding-flow-designer">
    <Toolbar :can-undo="canUndo" :can-redo="canRedo" @undo="undo" @redo="redo" @zoom-in="..." />
    <FlowCanvas ref="canvasRef">
      <template #edges><EdgeLayer :edges="flowJson.edges" :paths="layoutPaths" /></template>
      <template #nodes>
        <component v-for="node in flowJson.nodes" :key="node.id"
          :is="resolveNodeComponent(node.nodeType)"
          :node="node" :status="null" :selected="node.id === selectedNodeId"
          :style="{ left: positions.get(node.id).x + 'px', top: positions.get(node.id).y + 'px' }"
          @select="handleSelect" @action="handleAction" />
        <AddNodeButton v-for="slot in addSlots" ... />
        <BranchHeader v-for="edge in branchEdges" ... />
      </template>
    </FlowCanvas>
    <NodeConfigDrawer v-model:show="drawerShow" :node="selectedNode" @update="handleUpdate" />
    <NodeContextMenu ref="contextMenuRef" />
  </div>
</template>

<script setup>
const props = defineProps({
  modelValue: { type: String, required: true }, // BPMN XML
  readonly: Boolean
})
const emit = defineEmits(['update:modelValue', 'change'])

// 内部 flowJson，watch modelValue 转换
const { flowJson, selectedNodeId, addNode, deleteNode, updateNode, ... } = useFlowDesigner()
const { undo, redo, canUndo, canRedo, snapshot } = useFlowHistory(flowJson)

watch(() => props.modelValue, (xml) => {
  if (xml) flowJson.value = convertBpmnToJson(xml)
}, { immediate: true })

watch(flowJson, () => {
  emit('update:modelValue', convertJsonToBpmn(flowJson.value))
  emit('change', flowJson.value)
}, { deep: true })

// 暴露给父组件的方法
defineExpose({
  getXML() { return convertJsonToBpmn(flowJson.value) },
  importXML(xml) { flowJson.value = convertBpmnToJson(xml) },
  reset() { /* ... */ }
})
</script>
```

**resolveNodeComponent 映射**:
- start → StartNode
- end → EndNode
- approver → ApproverNode
- carbonCopy → CarbonCopyNode
- condition → ConditionNode
- parallel → ParallelNode
- inclusive → InclusiveNode
- service → ServiceNode
- script → ScriptNode
- subProcess → SubProcessNode
- callActivity → CallActivityNode
- advanced → AdvancedNode

**Toolbar 按钮**:
- 撤销 / 重做
- 缩放 +/-/100%/适应屏幕
- 全屏切换
- 切换 XML 源码模式（NDrawer 显示当前 XML）
- 校验流程（调用 lint：审批节点必须有 assignee、网关必须有分支等）

**验收标准**:
- 加载现有 BPMN 流程显示正确
- 添加节点后 modelValue 自动更新
- 撤销/重做按钮状态联动
- 暴露 getXML/importXML 与 FlowModeler 兼容
- readonly 模式禁用所有编辑操作

---

### Task 27: design.vue 数据流改造

**目标**: 将 design.vue 主数据从 bpmnXml 字符串改为 flowJson 内部状态，外层接口与后端不变。

**涉及文件**:
- `forge-admin-ui/src/views/flow/design.vue` — 修改：替换 FlowModeler→DingFlowDesigner

**修改点**:
1. import 替换：`FlowModeler` → `DingFlowDesigner`
2. 模板：`<FlowModeler v-model="bpmnXml" />` → `<DingFlowDesigner v-model="bpmnXml" />`
3. AI 生成流程：流式完成后赋值给 `bpmnXml`（DingFlowDesigner 内部 watch 自动转换）
4. 校验逻辑 `validateProcess()`：保留，调用 `designerRef.value?.validate()`
5. 保存逻辑 `handleSaveDraft()`、`handleDeploy()`：通过 `designerRef.value.getXML()` 获取最新 XML
6. AI 面板继续使用现有 `forge-admin-ui/src/components/ai-flow/`（不动）

**关键签名变更**:
```vue
<!-- 原 -->
<FlowModeler v-model:value="bpmnXml" ref="modelerRef" />
<!-- 改 -->
<DingFlowDesigner v-model="bpmnXml" ref="designerRef" />

<!-- 保存方法保持兼容 -->
async function handleSaveDraft() {
  const xml = designerRef.value.getXML()
  await saveModelDraft({ ...form, bpmnXml: xml })
}
```

**验收标准**:
- 加载已有流程模型正常
- AI 生成流程后画布刷新
- 保存草稿 / 部署成功
- 版本管理面板正常
- 表单配置面板（与流程关联表单）正常

---

## Phase 7：流程查看器

### Task 28: DingFlowViewer 查看器组件 ✅ done (2026-06-17)

**目标**: 实现钉钉样式的流程查看器，输入 (bpmnXml + nodeStatuses)，输出可交互的钉钉风格审批进度图。

**涉及文件**:
- `forge-admin-ui/src/components/flow-designer/viewer/DingFlowViewer.vue` — 新建
- `forge-admin-ui/src/components/flow-designer/viewer/NodeStatusBadge.vue` — 新建（节点状态徽章）
- `forge-admin-ui/src/components/flow-designer/viewer/index.js` — 修改：导出 DingFlowViewer

**关键签名**:
```vue
<!-- DingFlowViewer.vue -->
<template>
  <div class="ding-flow-viewer">
    <FlowCanvas ref="canvasRef" :readonly="true">
      <template #edges><EdgeLayer :edges="flowJson.edges" :paths="layoutPaths" :node-statuses="statusMap" /></template>
      <template #nodes>
        <component v-for="node in flowJson.nodes"
          :is="resolveNodeComponent(node.nodeType)"
          :node="node"
          :status="statusMap[node.bpmnElementId]?.status"
          :selected="false" :readonly="true"
          @click="showNodeDetail(node)" />
      </template>
    </FlowCanvas>
    <NodeDetailPopover v-model:show="popoverShow" :node="popoverNode" :status="popoverStatus" />
  </div>
</template>

<script setup>
const props = defineProps({
  bpmnXml: { type: String, required: true },
  nodeStatuses: { type: Object, default: () => ({}) } // bpmnElementId → { status, assignee, completeTime, comment }
})

const flowJson = computed(() => convertBpmnToJson(props.bpmnXml))
const statusMap = computed(() => buildStatusMap(props.nodeStatuses, flowJson.value))
</script>
```

**NodeStatusBadge.vue**:
- completed：绿色勾图标 + 处理时间
- running：蓝色脉冲圆 + "进行中"
- pending：灰色虚圆 + "待处理"
- rejected：红色 X + "已驳回"
- skipped：灰色破折号 + "已跳过"

**验收标准**:
- 加载 BPMN XML + 状态信息后正确显示
- 节点边框/连线颜色按状态着色
- 整体只读（无添加/删除/拖拽）
- 支持缩放/平移
- "适应屏幕"按钮自动 fit

---

### Task 29: 节点状态高亮 + 详情 Popover ✅ done (2026-06-17)

**目标**: 点击节点弹出详情 Popover，显示处理人、时间、审批意见、附件。

**涉及文件**:
- `forge-admin-ui/src/components/flow-designer/viewer/NodeDetailPopover.vue` — 新建
- `forge-admin-ui/src/components/flow-designer/viewer/process-status-builder.js` — 新建

**关键签名**:
```js
// process-status-builder.js
export function buildStatusMap(nodeStatuses, flowJson) {
  // 把后端返回的 [{ activityId, status, assignee, ... }] 转为 Map<bpmnElementId, info>
  // 处理多次任务（重新指派）：取最新一条
  // 推断未到达节点为 pending
}

export function inferEdgeStatus(edge, nodeStatusMap) {
  // 边状态 = 源节点状态
  // 默认分支：仅在汇合时才高亮
}
```

```vue
<!-- NodeDetailPopover.vue -->
<NPopover :show="show" :x="x" :y="y" placement="right" trigger="manual">
  <div class="node-detail">
    <div class="header">
      <NAvatar :src="status.assignee?.avatarUrl" />
      <div>{{ status.assignee?.userName || '未指派' }}</div>
    </div>
    <NDescriptions :column="1">
      <NDescriptionsItem label="状态">{{ statusLabel }}</NDescriptionsItem>
      <NDescriptionsItem label="开始时间">{{ status.startTime }}</NDescriptionsItem>
      <NDescriptionsItem label="完成时间">{{ status.completeTime }}</NDescriptionsItem>
      <NDescriptionsItem label="审批意见">{{ status.comment }}</NDescriptionsItem>
    </NDescriptions>
    <NDivider />
    <NThing v-for="att in status.attachments">{{ att.name }}</NThing>
    <NButton text v-if="status.status === 'completed'" @click="viewHistory">查看处理记录</NButton>
  </div>
</NPopover>
```

**显示规则**:
- 已完成节点：完整信息
- 进行中节点：当前处理人 + 已等待时长
- 待处理节点：预计处理人（来自 BPMN config）
- 已驳回节点：驳回意见 + 驳回到的目标节点

**验收标准**:
- 点击节点弹出 Popover
- 多人会签：列出所有处理人 + 各自状态
- 抄送节点：显示抄送列表 + 抄送时间
- Popover 自动定位（避免超出画布）
- 支持点击其他节点切换 Popover

---

## Phase 8：入口替换

### Task 30: todo / done / started / monitor 等页替换

**目标**: 替换流程相关页面中的 ProcessDiagramViewer 为 DingFlowViewer。

**涉及文件**:
- `forge-admin-ui/src/views/flow/todo.vue` — 修改：流程图查看
- `forge-admin-ui/src/views/flow/done.vue` — 修改
- `forge-admin-ui/src/views/flow/started.vue` — 修改
- `forge-admin-ui/src/views/flow/monitor.vue` — 修改（流程实例监控）
- 其他依赖 ProcessDiagramViewer / InteractiveProcessDiagram 的页面（用 grep 查全）

**修改模式**:
```vue
<!-- 原 -->
<ProcessDiagramViewer :process-instance-id="instanceId" />
<!-- 改 -->
<DingFlowViewer :bpmn-xml="bpmnXml" :node-statuses="nodeStatuses" />
```

**辅助逻辑**:
- 在每个页面 onMounted/打开 Modal 时调用 `getProcessDiagramInfo(instanceId)` 获取 `{ bpmnXml, nodeStatuses }`
- 把数据传给 DingFlowViewer

**验收标准**:
- todo 待办流程图正常
- done 已办流程图正常
- started 我发起的流程图正常
- monitor 流程实例图正常
- 节点点击 Popover 显示历史记录
- 所有 ProcessDiagramViewer / InteractiveProcessDiagram 引用已替换

---

### Task 31: 流程版本对比 / 历史查看入口替换

**目标**: 替换流程版本对比页、历史版本查看页中的旧 BPMN viewer。

**涉及文件**:
- `forge-admin-ui/src/views/flow/version-compare.vue`（如存在） — 修改
- `forge-admin-ui/src/views/flow/version-history.vue`（如存在） — 修改
- `forge-admin-ui/src/views/flow/instance-detail.vue`（如存在） — 修改

**修改模式**:
- 版本对比：左右两个 DingFlowViewer，用不同 bpmnXml
- 历史查看：DingFlowViewer + readonly

**验收标准**:
- 版本对比左右图同步缩放（可选）
- 历史版本查看正常
- 实例详情页流程图 + 操作面板布局不变

---

## Phase 9：清理与验收

### Task 32: 移除 bpmn-js 依赖与旧组件

**目标**: 删除旧 BPMN 设计器代码与依赖，验证无遗留引用。

**涉及文件**:
- `forge-admin-ui/package.json` — 修改：移除 `bpmn-js`、`bpmn-js-properties-panel`、`inherits-browser`
- `forge-admin-ui/src/components/bpmn/` — 删除整个目录
  - FlowModeler.vue / NodePropertiesPanel.vue / ProcessDiagramViewer.vue / InteractiveProcessDiagram.vue
  - flowable-moddle.json（迁移到 flow-designer/converter/extensions/）
  - UserSelectModal.vue（迁移到 flow-designer/panel/widgets/，更新 import 路径）
- `forge-admin-ui/src/styles/bpmn.css`（如存在） — 删除
- `forge-admin-ui/src/main.js` — 修改：移除 bpmn-js 相关 import

**验证步骤**:
```bash
# 1. 全局搜索，确保无遗留引用
cd forge-admin-ui
grep -rn "from 'bpmn-js" src/ && echo "FAIL" || echo "OK"
grep -rn "from '@/components/bpmn" src/ && echo "FAIL" || echo "OK"
grep -rn "ProcessDiagramViewer\|InteractiveProcessDiagram\|FlowModeler\|NodePropertiesPanel" src/ && echo "FAIL" || echo "OK"

# 2. 卸载依赖
pnpm remove bpmn-js bpmn-js-properties-panel inherits-browser

# 3. 重新构建确认无报错
pnpm build
```

**验收标准**:
- package.json 不含 bpmn-js 系列
- node_modules 移除相关包
- src/ 全局搜索无遗留 import
- 构建成功无报错

---

### Task 33: 前端构建 + Lint

**目标**: 全量构建 + Lint 修复，确保代码符合项目规范。

**涉及文件**:
- 无新增文件
- 已有文件按 Lint 报错修复

**验证步骤**:
```bash
cd forge-admin-ui
pnpm install
pnpm lint:fix
pnpm build  # 期望：构建成功，bundle 体积 < 旧版（移除 bpmn-js 后）
pnpm vitest run src/components/flow-designer  # 期望：所有单测通过
```

**验收标准**:
- pnpm build 成功，无 error
- 警告数量不超过基线
- ESLint 0 errors
- Vitest 通过率 100%
- bundle 体积下降（bpmn-js 约 1.5MB）

---

### Task 34: 端到端冒烟（设计 / 保存 / 部署 / 查看）

**目标**: 启动开发服务器，按 SDD `code-copilot/rules/automated-testing-standard.md` 走完关键场景。

**测试场景**（在 `code-copilot/changes/dingflow-approver-style-designer/test-spec.md` 中详细列出）:

1. **新建简单流程**：start → 1 个审批人 → end，保存草稿，验证 BPMN XML 含 UserTask + flowable:assignee
2. **新建分支流程**：start → 排他网关（2 分支：金额>1000走经理审批，否则走员工审批）→ end，部署成功
3. **会签流程**：start → 并行会签节点（3人，60% 通过）→ end，BPMN 含 multiInstanceLoopCharacteristics
4. **加载已有流程**：导入 forge-flow 测试资源中的复杂流程，画布显示正常，保存后 XML 不变
5. **AI 生成流程**：输入"请假流程，3 天以内主管审批，3 天以上需经理审批"，AI 生成 BPMN，画布渲染正常
6. **流程查看器**：发起一个测试实例，在 todo/done 页查看流程图，状态高亮正确
7. **复杂 BPMN 兜底**：导入含 BoundaryEvent 的流程，BoundaryEvent 显示为 advanced 节点，保存后 rawXml 不变

**验证步骤**:
```bash
# 启动后端
cd forge/forge-admin-server && mvn spring-boot:run

# 启动前端
cd forge-admin-ui && pnpm dev

# 浏览器访问 http://localhost:5173，登录 admin/123456
# 按场景手工执行，记录到 execution-log.md
```

**验收标准**:
- 7 个场景全部通过
- 控制台无报错（含 Vue 警告）
- BPMN XML 与现有版本兼容（导入旧流程 → 保存 → 数据库 XML 一致）
- 流程实例查看 status 显示正确
- 执行记录写入 `code-copilot/changes/dingflow-approver-style-designer/execution-log.md`

---

## 自审核查（writing-plans 必做）

- [x] **Spec 覆盖**：spec 第 3 章 23 个功能点（F1-F23）均映射到具体 Task：
  - F1（钉钉画布渲染）→ Task 9-11
  - F2（"+"号添加节点）→ Task 12
  - F3（节点卡片展示）→ Task 13-17
  - F4（分支布局）→ Task 10、15
  - F5（右侧抽屉）→ Task 18
  - F6（审批人配置）→ Task 19
  - F7（会签配置）→ Task 20
  - F8（操作权限）→ Task 19
  - F9（表单权限）→ Task 21
  - F10（监听器）→ Task 22
  - F11（条件分支）→ Task 23
  - F12（节点操作）→ Task 17
  - F13（撤销/重做）→ Task 8
  - F14（XML→JSON）→ Task 1-4
  - F15（JSON→XML）→ Task 5
  - F16（自动布局）→ Task 5、10
  - F17（兼容加载）→ Task 6、26
  - F18（保存/部署）→ Task 27
  - F19（AI 生成）→ Task 27
  - F20（钉钉查看器）→ Task 28
  - F21（节点详情）→ Task 29
  - F22（UserSelectModal 复用）→ Task 19
  - F23（依赖清理）→ Task 32

- [x] **占位符扫描**：无 TBD/TODO/"按需要补充"。所有"涉及文件"是绝对路径，"关键签名"含完整伪代码。

- [x] **类型一致性**：
  - flowJson 结构在 Task 2 定义，后续 Task 5/7/26 沿用
  - useFlowDesigner 方法签名（addNode/deleteNode/updateNode）在 Task 7 定义，Task 12/17/26 沿用
  - convertBpmnToJson / convertJsonToBpmn 函数名贯穿 Task 2/5/26/28 一致
  - NODE_TYPE 常量在 Task 1 定义，所有节点组件沿用

- [x] **风险任务专项**：转换层（Task 1-6）安排在 Phase 1 最早执行，附完整单测 fixture，进入 Phase 3 前必须 100% 通过

---

## 当前状态

- 任务总数：35 个（Task 0-34）
- 阶段：10 个 Phase
- 预计实施周期：3-4 个工作周（按单人估算）
- 关键里程碑：
  - 里程碑 1：Phase 1 转换层全测试通过（解锁后续所有任务）
  - 里程碑 2：Phase 6 设计器集成完成（首次端到端可见）
  - 里程碑 3：Phase 9 验收（可交付）

