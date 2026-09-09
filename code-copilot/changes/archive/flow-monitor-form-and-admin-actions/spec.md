# 流程实例监控：只读表单与管理员干预入口

> status: apply
> created: 2026-08-29
> complexity: 🟡中等

## 1. 背景与目标

流程实例监控详情只能看基本信息和审批时间轴，「更多 → 变量」只展示 Flowable 原始键值。管理员干预（终止、回退、转派）藏在运行中实例的「更多 → 管理」，且 `flow:monitor:manage` 升级后不自动授权，挂起实例进不了管理弹窗。

做完后：

- 监控详情用已办同款只读业务表单渲染，不再把业务数据当成变量 JSON。
- 「变量」仍保留在「更多」，仅用于排障。
- 运行中和已挂起实例的详情里直接露出终止、回退、转派、挂起/激活，不再藏在「更多 → 管理」。

## 2. 代码现状（Research Findings）

### 2.1 相关入口与链路

- `forge-admin-ui/src/views/flow/monitor.vue`：监控列表、600px 详情抽屉、变量弹窗、管理员操作弹窗。
- `forge-admin-ui/src/views/flow/done.vue`：已办只读表单，调用 `getProcessFormInfo` + `businessTaskFormReadonlyContext`。
- `forge-admin-ui/src/api/flow.js`：`getProcessFormInfo` → `GET /api/flow/task/form`。
- `FlowTaskController.getProcessFormInfo`：已办/抄送/历史只读表单，`@ApiPermissionIgnore`。
- `FlowMonitorController`：`flow:monitor:view` 查变量/当前任务/历史节点；`flow:monitor:manage` 终止/回退/转派/挂起/激活。
- `V1.0.55__secure_flow_monitor_cleanup.sql`：管理权限不回填给原监控角色。

### 2.2 现有实现

- 监控详情只有 `n-descriptions` + `n-timeline`，没有复用 `FlowBusinessForm` / `AiForm`。
- 「管理」仅 `canManage && row.status === 'running'`。
- 转派只取当前任务列表第一项。
- 已办表单渲染逻辑集中在 `done.vue`，未抽公共组件。

### 2.3 发现与风险

- `getProcessFormInfo` 只需要 `processInstanceId` 或 `businessKey`，监控列表行已有 `id`、`businessKey`、`processDefKey`。
- 只读表单字段权限必须走 `pickFirstNonEmptyFieldPermissions(..., { readOnly: true })`，不能把变量 JSON 直接铺成表单。
- 管理员操作继续走现有监控接口和 `flow:monitor:manage`，本轮不回填角色权限。
- 挂起实例允许进入干预区：激活/终止始终可用；回退/转派仅运行中可用，避免对挂起执行引擎变更。

## 3. 功能点

- [x] 功能 1：抽取 `FlowReadonlyFormPanel`，按已办链路只读渲染业务对象表单、代码优先表单和节点动态表单。
- [x] 功能 2：监控详情改为 `FlowTaskDetailShell` 全屏详情，主区展示基本信息 + 只读表单 + 流程图。
- [x] 功能 3：有 `flow:monitor:manage` 且实例为 running/active/suspended 时，详情内展示管理员干预区（挂起/激活、终止、回退、转派）。
- [x] 功能 4：转派支持选择当前活动任务；「更多」去掉「管理」，保留流程图、变量、错误日志。
- [x] 功能 5：已办页改用同一只读表单组件，避免两套渲染。

## 4. 业务规则

- 监控详情表单只读，不允许审批通过、驳回、转办、暂存。
- 代码优先表单可「打开完整业务页」，query 带 `readOnly=true`、`source=flowMonitor`。
- 无表单资产时展示空状态，不回退成变量 JSON。
- 终止、回退、转派必须填写原因。
- 挂起实例可激活、终止；回退和转派需先处于运行中。
- 无 `flow:monitor:manage` 时不展示干预区，查询和只读表单仍可用。
- 用户 ID 在选人和请求体中保持字符串。

## 5. 数据变更

无数据库变更，不回填 `flow:monitor:manage`。

## 6. 接口变更

无新增接口。监控详情复用：

| 操作 | 接口 | 方法 | 变更内容 |
|------|------|------|----------|
| 复用 | `/api/flow/task/form` | GET | 监控详情只读表单 |
| 复用 | 业务应用只读表单上下文 | GET | 低代码/代码优先表单 |
| 复用 | `/api/flow/monitor/terminate/{id}` 等 | POST | 详情内触发，协议不变 |

## 7. 影响范围

- `forge-admin-ui/src/components/flow/FlowReadonlyFormPanel.vue`（新增）
- `forge-admin-ui/src/views/flow/utils/monitorAdmin.js`（新增）
- `forge-admin-ui/src/views/flow/monitor.vue`
- `forge-admin-ui/src/views/flow/done.vue`
- `forge-admin-ui/src/components/flow/FlowTaskDetailShell.vue`（转派/回退时间轴文案）

## 8. 风险与关注点

- ⚠️ 管理员干预改变流程状态，必须保持现有权限码和原因审计。
- 只读表单加载失败时不能阻断详情和干预操作。
- 并行多任务转派必须显式选择任务，禁止静默只用第一项。

## 8.5 测试策略

- **测试范围**：监控干预判定、表单查询组装、只读表单加载分支。
- **覆盖率目标**：P0 工具函数 + 只读表单加载调用。
- **独立 Test Spec**：是

## 9. 待澄清

无。范围已按用户确认执行。

## 10. 技术决策

- 抽取共享只读表单组件，而不是在监控页复制 `done.vue`。
- 详情使用 `FlowTaskDetailShell`，与待办/已办交互一致。
- 不改后端权限模型；角色缺 `flow:monitor:manage` 时需管理员在角色里勾选。

## 11. 执行日志

| Task | 状态 | 实际改动文件 | 备注 |
|------|------|--------------|------|
| Task 1 只读表单组件 | done | `FlowReadonlyFormPanel.vue`、`monitorAdmin.js` | 复用 `/api/flow/task/form` |
| Task 2 监控详情与干预 | done | `monitor.vue`、`FlowTaskDetailShell.vue` | 详情内终止/回退/转派 |
| Task 3 已办复用 | done | `done.vue` | 去掉页面内重复表单逻辑 |
| Task 4 测试 | done | 2 个 Vitest spec | 7 通过；`pnpm build` 通过 |

## 12. 审查结论

未审查。

## 13. 确认记录（HARD-GATE）

- **确认时间**：2026-08-29
- **确认人**：用户（对话确认“按照这个改吧”）
