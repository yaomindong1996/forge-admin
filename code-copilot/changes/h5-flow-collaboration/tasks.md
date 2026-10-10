# H5 审批协同 Tasks

> 依赖：spec.md 第 10 章决策全部确认。每个任务完成后执行 test-spec.md 对应检查，并追加到 execution-log.md。

## 阶段一：拆分审批详情（单独提交，不改行为）

- [x] T1 `useTodoDetailStore` + `useFlowBusinessForm`：把业务表单状态和弹层/加锁状态从 `todo-detail.vue` 移出；加载和办理动作拆为 `useTodoDetailLoader`、`useTodoTaskActions`。
- [x] T2 组件：`TodoActionBar`、`TodoMoreActionSheet`、`FlowUserPicker`（内嵌选人，不单独弹层）、`TodoDelegateSheet`、`FlowBusinessFormPanel`；`todo-detail.vue` 220 行。
- [x] T3 回归：全部单测、构建、预览审批详情；已有契约断言改读拆分后的源码集合（`utils/__tests__/todo-detail-source.js`），正则不变。

## 阶段二：后端

- [x] T4 催办：`FlowTaskRemindCoordinator`（未签收报错、Redis 限流、降级）；`FlowTaskServiceImpl.remind` 改为委托；`FlowTaskRemindCoordinatorTest`。
- [x] T5 加签姓名：`FlowTaskSignRelationVO.targetUserName` + `selectDynamicSignRelations` 关联用户；契约测试加在 `FlowTaskCandidateRelationContractTest`。
- [x] T6 节点加签策略：`TaskFormInfo.allowAddSign`、`FlowTaskNodePolicy` 只读 BPMN 属性（见 spec 6.3）、加签专用协调器校验；`FlowTaskNodePolicyTest` 4 条。

## 阶段三：H5 抄送

- [x] T7 API 9 个方法（未读数改用 `/cc/unread/count`，见 spec 2.1）；`useCcStore` 并单测；`utils/flow-cc.js` 并单测。
- [x] T8 待办页"抄送我的"页签、未读数、全部已读；列表面板 `CcListPanel`（自带分页与已读筛选）。
- [x] T9 抄送详情页 `pages/flow/cc-detail`：抄送信息、只读业务表单（降级提示）、审批流程、自动已读。

## 阶段四：H5 催办与加签

- [x] T10 催办（`useFlowRemind`、`TodoRemindBar`）：`utils/flow-remind.js` 并单测；"我发起的"列表和只读详情入口；权限判断与本地冷却。
- [x] T11 加签/减签（`useTodoSignActions`、`TodoSignSheet`、`TodoSignRelations`）：`utils/flow-sign.js` 并单测；更多操作入口、加签选人、减签列表、加签人员展示。

## 阶段五：收尾

- [x] T12 契约测试：按 spec 8.2 追加 5 条。
- [x] T13 全量验证：`node --test`、`build:h5`、本地预览截图；后端编译和测试（本机无 JDK 时记录跳过）；更新 execution-log。
