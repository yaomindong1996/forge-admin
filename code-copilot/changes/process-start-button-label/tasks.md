# 任务清单：业务流程启动按钮文案回退到流程名称

状态：已编码并自测，待提交 / 待部署

- [x] T1 `BusinessProcessRuntimeActionCompiler` 跳过默认开始节点名；`compileSnapshots` 接收流程名称映射；
      `BusinessProcessService` 新建流程默认节点名改引用 `DEFAULT_START_NODE_NAME`
- [x] T2 `BusinessApplicationSnapshotService.finalizeProcesses` 从候选快照 `processes` 传入流程名称
- [x] T3 单测：编译器文案回退（默认名 → 流程名、自定义节点名优先、快照映射与缺省兜底）、快照链路文案
- [x] T4 编译 + 相关测试（`-Penable-tests`），结果写入 `execution-log.md`
