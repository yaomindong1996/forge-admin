# 业务流程启动按钮文案回退到流程名称

## 背景

业务流程「手动开始」节点编译成页面 `START_PROCESS` 按钮时，文案回退链路为
`buttonLabel → 开始节点名 → 流程名称 → 流程编码 → 「启动流程」`。新建业务流程时
`BusinessProcessService` 把开始节点默认命名为「手动开始」，设计者通常不改，于是 PC 与 H5 的按钮都显示
「手动开始」，用户看不出要发起的是哪个流程（2026-11-13 报销审批 E2E 发现，见 `h5-business-process-start` T8）。

调用链：

- 运行时渲染配置：`BusinessApplicationRuntimeConfigOverlayService` → `BusinessProcessRuntimeActionProjectionService`
  → `compiler.compileActions(snapshot, applicationCode, processName)`，已传入真实流程名称。
- 应用发布快照：`BusinessApplicationSnapshotService.finalizeProcesses` → `compiler.compileSnapshots`，
  以 `processCode` 充当流程名称，仅写入版本快照 `runtimeActions`（运行时不读取，用于版本记录）。

## 目标

1. 开始节点名为默认值「手动开始」时视为未配置，回退到流程名称（如「报销审批」）。
2. 发布快照链路从候选快照 `processes[].processName` 取流程名称，与运行时文案一致。
3. 设计者显式配置的 `buttonLabel` 或自定义开始节点名保持最高优先级，行为不变。

## 非目标

- 不改数据库、接口协议、权限码；不迁移存量流程 JSON（编译期回退即可生效，存量流程无需重新发布）。
- 不改流程设计器 UI。

## 方案

- `BusinessProcessRuntimeActionCompiler`：常量 `DEFAULT_START_NODE_NAME = "手动开始"`，
  计算文案时开始节点名等于该默认值则跳过。
- `compileSnapshots(snapshots, applicationCode, processNames)`：新增流程编码 → 名称映射参数，缺失时回退流程编码。
- `BusinessApplicationSnapshotService.finalizeProcesses`：从候选快照 `processes` 构建映射后传入。

## 风险

- 历史流程若特意把开始节点命名为「手动开始」作为按钮文案，按钮将改为流程名称；属预期体验改进。
- 不涉及资金、状态流转、权限变更。

## 验收

1. 单测：默认节点名回退流程名称；自定义节点名、`buttonLabel` 优先级不变；快照链路使用流程名称。
2. 编译通过，相关测试通过（`-Penable-tests`）。
