# 任务拆分 — 流程实例监控：只读表单与管理员干预入口

> 拆分顺序：共享组件 → 监控详情 → 已办复用 → 测试

## 前置条件

- [x] 用户确认范围：监控详情渲染只读表单；终止/回退/转派放到详情；挂起实例可进干预区。
- [x] Task 1-4 已完成，见 execution-log。

## Task 1: 抽取只读表单面板

- **目标**: 把已办页的只读表单渲染抽成可复用组件。
- **涉及文件**:
    - `forge-admin-ui/src/components/flow/FlowReadonlyFormPanel.vue` — 新增
    - `forge-admin-ui/src/views/flow/utils/monitorAdmin.js` — 新增查询/状态判定
- **关键签名**:
  ```js
  export function canInterveneInstance(status) {}
  export function canMutateRunningInstance(status) {}
  export function buildMonitorFormQuery(row = {}) {}
  ```
- **完成标准**: 传入 `processInstanceId`/`businessKey` 后能按 external / business-object / business-code / dynamic 只读渲染，失败时展示空状态。

## Task 2: 监控详情接入表单与干预区

- **目标**: 详情改为全屏 Shell，内嵌只读表单和管理员干预。
- **涉及文件**:
    - `forge-admin-ui/src/views/flow/monitor.vue` — 替换抽屉、去掉「更多 → 管理」、内联终止/回退/转派
    - `forge-admin-ui/src/components/flow/FlowTaskDetailShell.vue` — 补 reassign/rollback 时间轴文案
- **完成标准**:
    - 详情能看到业务表单。
    - running/active/suspended 且有 manage 权限时能干预。
    - 挂起实例可激活/终止；回退和转派仅运行中。
    - 多当前任务转派必须先选任务。

## Task 3: 已办页复用只读表单组件

- **目标**: `done.vue` 表单区改用 `FlowReadonlyFormPanel`，行为与原来一致。
- **涉及文件**:
    - `forge-admin-ui/src/views/flow/done.vue`
- **完成标准**: 已办详情仍展示只读表单、完整业务页入口和空状态。

## Task 4: 增量测试

- **目标**: 覆盖状态判定、表单查询组装和只读表单加载。
- **涉及文件**:
    - `forge-admin-ui/src/views/flow/utils/__tests__/monitorAdmin.spec.js`
    - `forge-admin-ui/src/components/flow/__tests__/FlowReadonlyFormPanel.spec.js`
    - `code-copilot/changes/flow-monitor-form-and-admin-actions/test-spec.md`
    - `code-copilot/changes/flow-monitor-form-and-admin-actions/execution-log.md`
- **完成标准**: 相关 Vitest 通过，前端 lint/build 无本轮引入错误。
