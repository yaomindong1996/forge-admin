# H5 支持发起低代码「业务流程」

## 背景

低代码应用的审批由「业务流程」编排（手动开始 → 审批节点 → 结束），发布后会在单据渲染配置里投影出
`actionType: START_PROCESS` 的动作（含 `applicationCode`、`processCode`、权限 `ai:businessProcess:start`），
PC 端 `AiCrudPage` 已能发起。H5 目前只读取单据运行态里的 `START_FLOW`，遇到 `START_PROCESS` 一律提示
「该流程请在 PC 端发起」。低代码应用又没有配置对象主流程（`START_FLOW`）的入口，导致 H5 无法发起报销等审批。

2026-11-13 报销审批 E2E 已确认：App 服务包含 `forge-plugin-generator`，经 H5 网关调用
`GET /ai/business/process/runtime/{applicationCode}/{processCode}/start-config` 返回 200，后端能力已具备。

## 目标

1. H5 低代码运行页（新建与详情）展示已发布的手动业务流程按钮，并能发起。
2. 发起流程与 PC 一致：确认 → 查询发起配置 → 需要时选择审批人 → 启动 → 刷新单据状态。
3. 已发起或运行中的流程按钮隐藏（与 PC `shouldHideProcessStartAction` 一致），避免重复发起。
4. 保持现有 `START_FLOW`（对象主流程）行为不变。

## 非目标

- 不新增或修改后端接口、权限码、数据库结构。
- 不改业务流程编排器、审批模型设计器。
- H5「发起审批」入口页（`pages/approval/start`）是否列出业务流程，放到第二阶段单独评审（需要后端查询改造）。

## 方案

### 动作来源

- 从渲染配置 `options.rowActions` / `detailActions` / `formActions` / `runtimeActions` 中取
  `actionType === 'START_PROCESS'`（或 key 以 `startProcess:` 开头）的动作，按记录显示条件过滤后按 `processCode` 去重。
  `toolbarActions` 属于列表级操作，不在单据页展示。
- H5 列表行操作排除 `START_PROCESS`：列表不加载单据运行态，无法判断是否已发起，统一进详情页发起。
- 只展示用户具备 `ai:businessProcess:start` 权限的动作；无权限时提示「暂无发起业务流程权限，请联系管理员」。
- 运行态 `activeProcessCodes` / `startedProcessCodes` 包含该 `processCode` 时按钮隐藏，
  判断规则与 PC `business-action-runtime.js` 保持一致。运行失败（FAILED）不计入已发起，按钮保留以便重试。

### 发起流程

1. 新建页：先按现有 `persist` 保存拿到 `recordId`，再发起；详情页直接发起。
2. `GET start-config`；`initiatorSelectNodes` 非空时复用现有 `InitiatorSelectSheet`，取消则不发起。
3. `POST /ai/business/process/runtime/{applicationCode}/{processCode}/start`，
   请求体 `{ recordId, objectCode, variables }`，与 PC `startBusinessProcess` 相同。
4. 成功提示「业务流程已启动」并重载详情与单据运行态；`loadingKey` 防止连点重复提交。
   启动接口返回 `status: FAILED` 时提示失败节点原因并刷新，不提示成功。

### 状态展示

- 单据未启用单据配置时（`documentEnabled: false`），运行态 store 仍需保留流程相关字段，
  用于按钮隐藏；审批进度沿用现有流程时间线组件。
- 同一记录刷新运行态时保留旧状态，只在切换记录时清空，避免状态卡闪烁。

### 代码落位（H5）

| 位置 | 内容 |
|------|------|
| `utils/document-flow-actions.js` | 新增业务流程动作解析，移除 `START_PROCESS` 的「仅 PC」提示分支 |
| `store/modules/documentFlow.js` | 合并主流程按钮与业务流程按钮，保留 `documentEnabled: false` 时的流程状态 |
| `composables/lowcode/useLowcodeDocumentFlow.js` | 新增 `startProcess` 分支（保存 → 配置 → 选人 → 启动 → 刷新） |
| `api/index.js` | 新增 `getBusinessProcessStartConfig`、`startBusinessProcess` |

`pages/lowcode-runtime.vue` 已超 600 行，逻辑全部放 composable / store，页面只透传。

## 风险与约束

- 鉴权以后端 `@SaCheckPermission("ai:businessProcess:start")` 为准，前端权限判断仅控制展示。
- 同一单据既有主流程又有业务流程时，两类按钮同时展示，文案取动作 `label`，避免都叫「提交审批」。
- 不涉及资金、权限放开与数据库迁移。

## 验收

1. 报销应用「费用报销E2E」（仅业务流程、无对象主流程）：H5 新建报销 → 点业务流程按钮 → 审批人在 H5 待办同意 →
   单据显示已通过，「我发起的」可查看结果。
2. 同一单据流程进行中时按钮隐藏，刷新后仍隐藏。
3. 已配置对象主流程的单据，原「提交审批」行为不变。
4. 无 `ai:businessProcess:start` 权限的账号看不到按钮，并看到无权限提示。
