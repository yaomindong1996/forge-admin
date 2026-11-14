# 任务清单：H5 支持发起低代码「业务流程」

状态：阶段一已提交（4b9cd895a）；阶段二已编码并自测，待提交 / 待部署

## 阶段一：H5 发起业务流程

- [x] T1 `api/index.js` 新增 `getBusinessProcessStartConfig`、`startBusinessProcess`（加密方式与 PC 一致）；
      `permission.js` 新增 `processStart`
- [x] T2 `utils/document-flow-actions.js` 解析渲染配置中的 `START_PROCESS` 动作，按 `processCode` 去重；
      运行中/已发起隐藏规则对齐 PC `business-action-runtime.js`；`resolveProcessRunFailure` 解析失败原因；补 node:test 单测
- [x] T3 `store/modules/documentFlow.js` 合并主流程与业务流程按钮；`documentEnabled: false` 时保留流程状态字段；补单测
- [x] T4 `useLowcodeDocumentFlow.js` 新增发起分支：保存 → 查配置 → 自选审批人 → 启动 → 刷新，防重复点击；
      同一记录刷新不再清空运行态（修复状态卡闪烁）
- [x] T5 `pages/lowcode-runtime.vue` 透传 `config` / `record`（页面仅加 2 行）；
      `LowcodeDocumentFlowStatus` 在非单据对象 + 自定义底部栏时也展示按钮卡；
      `normalizeActions` 列表行操作排除 `START_PROCESS`
- [x] T6 自测：H5 构建通过；node:test 207 条，失败 3 条均为存量 PC 设计器契约测试（改动前同样失败）

## 阶段二

- [x] T7 「发起审批」目录列出已发布的手动业务流程：
      后端 `selectStartableProcesses` + 服务端按手动开始节点过滤，VO 新增 `processCode` / `processName`；
      H5 目录按两类权限过滤，业务流程条目跳转 `mode=create&process=<code>`，新建页只展示该流程按钮
- [x] T8 业务流程按钮文案：开始节点默认名「手动开始」被当作按钮文案 → 已拆为变更
      `process-start-button-label` 完成（默认名回退流程名称，发布快照链路带入流程名称）

## 验收

- [x] V1 演示环境完整闭环：H5 新建单据 3 → 详情发起业务流程 → H5 待办同意 → 单据「已通过」，
      业务流程运行 SUCCESS，「我发起的」可查到已通过记录（数据源临时开启写入，测完已关闭）
- [ ] V5 阶段二部署后验收：「发起审批」出现「报销审批」→ 新建 → 发起 → 审批通过
- [x] V2 进行中按钮隐藏，刷新后仍隐藏
- [~] V3 主流程单据行为不变：单测覆盖；演示存量单据均已审批结束，无可点击的「提交审批」
- [~] V4 无权限：单测覆盖，未用无权限账号实测
- [x] 结果追加到本目录 `execution-log.md`
