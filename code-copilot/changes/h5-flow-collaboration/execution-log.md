# H5 审批协同 Execution Log

## 2026-10-10 提案

- 调研流程服务抄送（`FlowCcController`）、催办（`FlowTaskServiceImpl.remind`）、动态加签（`FlowTaskDynamicSignCoordinator`）、访问控制（`FlowAccessGuard`）、节点策略（`FlowTaskNodePolicy`）、API 权限拦截（`ApiPermissionInterceptor`），以及管理端 `cc.vue`、`started.vue` 和 H5 待办/审批详情。
- 关键发现：
  - 加签/减签只允许当前办理人或所有者，发起人不行；管理端把加签放在"我发起的"页面，发起人操作会失败。H5 放在审批详情。
  - 催办没有频率限制，未签收任务静默返回成功。
  - 加签记录只有用户 ID。
  - 节点 `allowAddSign` 配置写入 BPMN 但从未被读取或校验，设计器也没有开关。
  - 抄送人不能按 `taskId` 查看任务详情，但可以查看流程级历史和流程图。
  - 管理端调用的 `/api/flow/cc/unread/count` 在控制器中不存在，H5 改用 `/my?isRead=0` 的 `total`。
- `todo-detail.vue` 862 行，先拆分再加功能。
- 用户确认：催办后端限流 + H5 冷却；加签记录后端返回姓名；抄送未读数只在待办页页签显示。节点加签策略（spec 10.4）待确认。
- 本轮只写文档，没有改代码。

## 2026-10-10 /apply 阶段一：拆分审批详情

- 用户确认 10.4：节点加签策略下发并校验，未配置视为允许。
- `todo-detail.vue` 862 → 220 行。新增 `store/modules/todoDetail.js`、`composables/flow/useFlowBusinessForm.js`、`useTodoDetailLoader.js`、`useTodoTaskActions.js`，组件 `FlowBusinessFormPanel`、`FlowUserPicker`、`TodoActionBar`、`TodoMoreActionSheet`、`TodoDelegateSheet`；样式随组件迁移。
- 已有契约测试改读拆分后的源码集合 `utils/__tests__/todo-detail-source.js`，正则未改。新增 `store/modules/__tests__/todoDetail.test.js` 6 条。
- `node --test src/utils/__tests__ src/store/modules/__tests__`：148 通过、0 失败。
- `build:h5`：通过，仅 `badge.js` 两条已知动态导入提示。H5 没有 `lint` 脚本。
- 预览发现并修复一个拆分引入的问题：直接打开详情链接时页面空白（显示"待办不存在"）。原因是该 uni-app 运行时的 `toRefs()` 会立即求值 reactive 对象中的全部 computed，业务上下文为空时 `extractPageSections(null)` 抛错，`setup` 中断。修复：页面改用 `computed` 读取表单状态；`pageSections`、`flowInteraction` 对空上下文兜底。
- 预览（390 宽，注入 store 数据）：表单左标签右控件、审批职责/要点、底栏"更多/驳回/同意"与拆分前一致；更多操作三项、转办选人和"已选择"、转办缺说明提示、同意缺意见提示并滚动到审批意见均正常。

## 2026-10-10 /apply 阶段二：后端

- T4 催办：新增 `FlowTaskRemindCoordinator`（包内 final 类，由 `FlowTaskServiceImpl` 构造）。顺序：任务可见性 → 任务存在 → 已签收（否则"任务尚未被签收，暂时无法催办"）→ Redis `SET NX` 10 分钟（`forge:flow:remind:{tenantId}:{taskId}:{userId}`，重复报"已催办过，请 10 分钟后再试"）→ 发送站内信。Redis 不可用时放行并告警；消息发送失败只记日志，不释放限流键。`FlowTaskServiceImpl.remind` 改为委托，净减约 45 行。
  - 偏离说明：`FlowTaskServiceImpl` 新增一个 `@Autowired(required = false) StringRedisTemplate` 字段，沿用该类现有的字段注入方式（整类构造器注入改造不在本变更范围）。
- T5 加签姓名：`selectDynamicSignRelations` 左关联 `sys_user`（仅纯数字 ID、未删除、且属于同租户 `sys_user_tenant` 有效成员），返回 `targetUserName`（`real_name` 优先，其次 `username`），仍 `LIMIT 50`；`FlowTaskSignRelationVO` 增加字段；契约用例加在 `FlowTaskCandidateRelationContractTest`。
- T6 节点加签策略：`TaskFormInfo.allowAddSign`；`FlowTaskNodePolicy` 默认允许，读取 BPMN 属性 `allowAddSign`；新增 `validateAddSign`（"当前节点不允许加签"）。`FlowTaskServiceImpl.addSignCoordinator()` 在操作人校验之后调用；减签仍走原协调器。
  - 实施修正（已写入 spec 6.3）：不读 `sys_flow_node_config.allow_add_sign`。该列默认 `0`、设计器从未写入，读取会导致所有存有节点配置的流程禁止加签。
- 新增/修改测试：`FlowTaskRemindCoordinatorTest` 7 条；`FlowTaskCandidateRelationContractTest` +1；`FlowTaskNodePolicyTest` +4（默认允许、节点配置列默认值不生效、BPMN false 隐藏并拒绝、仅加签路径校验）。
- `FlowTaskServiceImpl` 958 → 934 行。
- 本机无 JDK，后端编译和测试跳过，需在有 JDK 的环境执行 test-spec 第 4 节命令。

## 2026-10-10 /apply 阶段三至五：H5 抄送、催办、加签

- 实施时核实：H5 `/api/flow/**` 代理到 flow 服务，其 `FlowCcController` 有 `GET /unread/count`（提案只查了插件控制器）。未读数改用该接口，spec 2.1、第 7 章已更新。flow 服务 `FlowTaskController` 调用的就是插件 `FlowTaskService`，阶段二的后端改动对 H5 生效。
- API：`api/index.js` 新增 9 个方法，全部 `encrypt: true`、`needTip: false`，路径参数 `encodeURIComponent`。
- 抄送：`utils/flow-cc.js`、`store/modules/cc.js`（接口函数由调用方传入，避免新增动态导入告警）；`components/flow/CcListPanel.vue`（全部/未读、全部已读二次确认、分页、下拉刷新）；`pages/todo.vue` 新增"抄送我的"页签与未读数，抄送页签隐藏流程筛选；`pages/flow/cc-detail.vue` + `composables/flow/useCcDetail.js`（抄送信息、只读业务表单失败时提示"业务表单请在 PC 端查看"、审批流程、进入即标记已读）。
- 催办：`utils/flow-remind.js`、`composables/flow/useFlowRemind.js`（会话内 10 分钟冷却，后端返回"已催办过"时同步进入冷却）；"我发起的"卡片与撤回并列的"催办"；只读详情底部 `TodoRemindBar`。权限码 `flow:task:remind` 加入 `FLOW_PERMISSIONS`。
- 加签/减签：`utils/flow-sign.js`；`todoDetail` store 增加 `currentUserId`、`signRelations`、加签弹层状态与 `canAddSign`/`canReduceSign`；`TodoSignSheet`、`TodoSignRelations`、`useTodoSignActions`（`PARALLEL` + 幂等凭证）；页面 `refresh` 在详情加载后再查加签记录。
- 测试：新增 `flow-cc` 5、`flow-remind` 4、`flow-sign` 5、`cc` store 4、`todoDetail` store +2、`console-design-system` 契约 +5；`feedback-components` 页面清单加入抄送详情。一条既有断言要求 `onShow` 先 `loadTasks`，按断言调整代码顺序，未改测试。
- `node --test src/utils/__tests__ src/store/modules/__tests__`：173 通过、0 失败。
- `build:h5`：通过，仅 `badge.js` 两条已知动态导入提示。
- 行数：`todo.vue` 401、`todo-detail.vue` 234、`cc-detail.vue` 134、`CcListPanel.vue` 286。
- 预览（无后端，注入组件/store 数据；"安全通道初始化失败"提示来自无后端）：
  - "我发起的"：运行中且有办理人的任务显示"催办 / 撤回"，待签收任务只有"撤回"；四个页签一行放下。
  - "抄送我的"：页签红色未读数 2、全部/未读筛选、全部已读、未读圆点、摘要两行截断。
  - 抄送详情：原生导航"抄送详情"，抄送信息卡、PC 端查看提示、办理记录。
  - 审批详情：更多里转办/加签/减签；减签弹层只列有效加签人；`allowAddSign=false` 时只剩减签；审批流程卡片下展示加签人员（有效/已撤回）；只读模式底部"催办审批人"。
- 预览后已停止 3009 开发服务。
- 后端编译与测试仍未执行（本机无 JDK/Maven），需按 test-spec 第 4 节在有 JDK 的环境运行。
