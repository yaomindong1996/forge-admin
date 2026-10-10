# H5 审批协同 Spec（抄送我的 + 催办 + 加签/减签）

> 变更名：`h5-flow-collaboration`
> 状态：`confirmed`（第 10 章决策已于 2026-10-10 全部确认，可进入 /apply）
> 创建日期：2026-10-10
> 前置变更：`h5-office-essentials`（发起审批、单据审批动作、`authStore.hasPermission`）
> 涉及：H5 待办页和审批详情页；flow 插件催办限流、加签记录返回姓名、节点"允许加签"策略下发（权限/状态相关，需人工审查）

## 1. 背景与目标

### 1.1 背景

后端流程服务已经提供抄送、催办、动态加签/减签接口，H5 都没有接入：

- **抄送我的**：别人抄送给我的审批，H5 看不到。
- **催办**：发起人只能干等，没有提醒审批人的手段。
- **加签/减签**：审批人遇到需要别人一起审的情况，只能回 PC 端；而管理端把加签放在"我发起的"页面，但后端只允许当前办理人加签（见 2.3），发起人在 PC 端点加签实际会失败。

另外 `pages/todo-detail.vue` 已有 862 行，接近 1000 行上限，加功能前必须先拆分。

### 1.2 目标

1. 拆分 `todo-detail.vue`：业务表单、底部操作、转办选人拆为组件和 composable，页面降到 600 行以内。
2. 待办页新增"抄送我的"页签，显示未读数；抄送详情页展示抄送信息、业务表单（只读）和审批流程，打开即标记已读，支持全部已读。
3. "我发起的"列表和审批详情（只读模式，发起人视角）提供"催办"；后端增加频率限制。
4. 审批详情"更多"里提供"加签""减签"，仅当前办理人可见；加签记录返回姓名。

### 1.3 不做

- 抄送他人（审批时抄送、`/api/flow/cc/send`）和撤回抄送：本次只做接收侧。
- "我抄送的"列表。
- 前加签、后加签：后端目前只支持并行加签（`PARALLEL`），H5 固定传 `PARALLEL`，不出模式选择。
- 改派（`/reassign`）：管理端已有，H5 后续按需再做。
- 管理端 `started.vue` 加签入口的问题（发起人不能加签）只在本 spec 记录，不在本变更修改 PC 页面。

## 2. 调研结论

### 2.1 抄送接口（`FlowCcController`，`/api/flow/cc`，类上 `@ApiDecrypt` `@ApiEncrypt`）

| 接口 | 说明 |
|------|------|
| `GET /my?pageNum&pageSize&isRead&title` | 抄送给我的，按会话用户过滤，`status = 0`（有效）；返回 `FlowCc` 分页 |
| `GET /form/{id}` | 抄送关联的流程表单信息（`TaskFormInfo`），只返回当前用户可见的抄送 |
| `POST /read/{id}` | 标记已读；SQL 带 `cc_user_id = 当前用户`、`status = 有效`，不能标记别人的 |
| `POST /read/all` | 全部已读，返回更新条数 |

- 未读数：`GET /unread/count` 返回 `{ count }`。（提案时只查了插件内控制器，误判为不存在；H5 代理到的 flow 服务 `FlowCcController` 有该接口，实施时改用。）
- `FlowCc` 关键字段：`id`、`processInstanceId`、`processDefKey`、`processName`、`taskId`、`title`、`content`、`businessKey`、`objectCode`、`recordId`、`businessObjectName`、`businessSummary`、`sendUserName`、`ccTime`、`isRead`。
- 可见性：任务详情 `requireTaskVisible` 不包含抄送人，所以抄送详情**不能复用**按 `taskId` 加载的待办详情；流程级接口（审批历史、流程图）走 `requireProcessVisible`，包含抄送人，可以复用。

### 2.2 催办（`POST /api/flow/task/remind?taskId=`）

- 权限 `@SaCheckPermission("flow:task:remind")`，`sys_resource` 已有该资源，需给员工角色授权。
- 后端只校验任务可见（发起人、办理人、候选人都可见），**没有频率限制**；任务未签收（无办理人）时直接返回成功但不发消息。
- 消息为站内信"流程催办提醒"，接收人是当前办理人。
- 管理端定义了 `remindTask`，但没有页面使用。

### 2.3 加签/减签（`POST /api/flow/task/add-sign`、`/reduce-sign`，`GET /{taskId}/sign-relations?userId=`）

- 请求体 `FlowTaskSignDTO`：`taskId`、`userId`、`targetUserId`、`comment`、`signMode`、`idempotencyKey`、`requestDigest`。
- 操作人校验 `assertTaskMutationActor(taskId, userId, allowInitiator=false)`：**只有当前办理人或任务所有者**可以加签/减签，发起人不行。
- 只支持 `PARALLEL`；不能把自己或当前办理人加进名单；单任务最多 50 人；支持幂等键（H5 已有 `createFlowActionCredentials`）。
- 加签记录 `FlowTaskSignRelationVO` 只有 `targetUserId`，没有姓名；`status = 1` 为有效。
- 接口未配置 API 资源，按 `ApiPermissionInterceptor` 规则只校验登录，再由上面的业务校验兜底。
- 节点配置 `FlowNodeConfig.allowAddSign` 会写进 BPMN（`FlowBpmnGenerateService`），但 `FlowTaskNodePolicy` 不读取、`TaskFormInfo` 不返回、加签时也不校验；管理端设计器也没有这个开关。

### 2.4 H5 现状

- 待办页 `pages/todo.vue`（372 行）：页签"待处理 / 已处理 / 我发起的"；"我发起的"列表每行是一条任务（`sys_flow_task`，`start_user_id = 我`），运行中可撤回。
- 审批详情 `pages/todo-detail.vue`（862 行）：业务表单、流程进度、审批意见、底部"驳回 / 同意 / 更多"、更多里有退回发起人、转办、终结；转办选人用 `getUserPage`。

## 3. 拆分 `todo-detail.vue`（先做，不改行为）

| 新文件 | 内容 |
|--------|------|
| `composables/flow/useFlowBusinessForm.js` | 业务表单状态：`mainData`、`childData`、`dictOptions`、表单引用注册、`applyBusinessContext`、`loadDictOptions`、字段/子表/分区计算属性、暂存修改 |
| `components/flow/TodoActionBar.vue` | 底部签收、驳回、同意、更多按钮 |
| `components/flow/TodoMoreActionSheet.vue` | "更多"操作宫格（退回发起人、转办、终结，以及本变更新增的加签、减签、催办入口） |
| `components/flow/FlowUserPicker.vue` | 通用选人（内嵌在弹层中，避免弹层套弹层）：搜索、分页、单选、排除指定用户；转办和加签共用 |
| `components/flow/TodoDelegateSheet.vue` | 转办：选人 + 说明 + 签名 |
| `components/flow/FlowBusinessFormPanel.vue` | 业务表单主体（Provider 提示、分区渲染、字段缺失提示）；审批详情和抄送详情共用 |
| `composables/flow/useTodoDetailLoader.js`、`useTodoTaskActions.js` | 详情加载；办理动作（签收、同意、驳回、转办、退回、终结、暂存） |

- 页面状态较多且跨多个弹层，按 AGENTS.md 5.14 放 Pinia：`store/modules/todoDetail.js`（`useTodoDetailStore`：当前任务、表单信息、动作加锁、弹层开关），组件直接读写 store，不经 props 层层传递。
- 拆分后 `todo-detail.vue` 不超过 600 行；每个方法不超过 80 行。
- 已有契约测试（`console-design-system.test.js` 中对 `todo-detail.vue` 的断言）若因代码搬移失效，按"断言跟随代码位置迁移"处理：改读新文件，断言内容不放宽。
- 拆分单独一个提交，构建和全部测试通过后再做后续功能。

## 4. 抄送我的

### 4.1 待办页

- 页签新增"抄送我的"（`cc`），排在"我发起的"之后；页签上显示未读数（超过 99 显示 `99+`）。
- 列表项：标题（`title`，缺省用 `processName`）、业务摘要（`businessSummary`）、"`sendUserName` 抄送 · 时间"、未读圆点。
- 筛选：全部 / 未读（`isRead=0`）；搜索复用顶部关键字（`title` 参数）。
- 顶栏"全部已读"：二次确认后调用 `/read/all`，刷新列表和未读数。
- 未读数**不计入**底部"待办"页签角标（第 10 章决策 3）。

### 4.2 抄送详情 `pages/flow/cc-detail`

- 路由参数：`id`（抄送 ID）、`processInstanceId`；列表点击时把抄送记录暂存到 `useCcStore`，详情优先使用，避免再查一次。
- 内容：
  1. 抄送信息卡：标题、发送人、时间、抄送说明（`content`）。
  2. 业务表单（只读）：先调 `/cc/form/{id}` 拿 `TaskFormInfo`，再按其中的 `businessKey`、`processInstanceId`、`objectCode`、`recordId` 调只读业务上下文接口，渲染复用 `useFlowBusinessForm` + `PageSectionRenderer`（只读）。只读上下文接口拒绝时，显示"业务表单请在 PC 端查看"，不报错。
  3. 审批流程：复用 `TodoFlowTrace`，数据来自 `getFlowTaskHistory`、`getFlowDiagramInfo`。
- 进入详情且未读时调用 `/read/{id}`，成功后本地标记已读、未读数减 1。

### 4.3 状态

- `store/modules/cc.js`（`useCcStore`）：`unreadCount`、`current`（列表暂存的记录）、`loadUnreadCount()`、`markRead(id)`、`markAllRead()`。

## 5. 催办

### 5.1 后端限流（第 10 章决策 1）

- 新建 `FlowTaskRemindCoordinator`（实施时与同包 `FlowTaskDynamicSignCoordinator` 一致：包内 final 类、构造参数传入依赖，由 service 创建），把 `FlowTaskServiceImpl.remind` 的逻辑整体移过去，`FlowTaskServiceImpl.remind` 只做一行委托（该类 958 行，本次净减少）。
- 顺序：校验任务可见 → 任务不存在时报"任务不存在或已处理" → **任务未签收（无办理人）时报"任务尚未被签收，暂时无法催办"**（原来静默返回成功）→ 抢占限流 → 发送站内信。
- 限流：Redis `SET NX EX`，key `forge:flow:remind:{tenantId}:{taskId}:{userId}`，有效期 10 分钟；抢占失败报"已催办过，请 10 分钟后再试"。
- Redis 不可用时放行并记录 `warn` 日志（催办不是关键业务，宁可多发不阻断）。
- 消息发送失败只记日志，不释放限流 key，避免失败后被连续重试刷屏。

### 5.2 H5

- 入口一："我发起的"列表中，运行中且已有办理人（`status = 1` 或 `assignee` 非空）的任务显示"催办"按钮，与"撤回"并列。
- 入口二：审批详情只读模式（从"我发起的"进入）底部显示"催办"。
- 没有 `flow:task:remind` 权限时不显示按钮。
- 点击后直接调用；成功提示"已催办"，按钮进入 10 分钟本地冷却（显示"已催办"并置灰），刷新页面后以后端返回为准。

## 6. 加签/减签

### 6.1 H5

- 入口：审批详情处理模式的"更多"里新增"加签""减签"；仅当前用户是办理人（`assignee`）或所有者（`owner`），且节点允许加签（见 6.3）时显示。
- 加签：弹层内用 `FlowUserPicker` 选一人（排除自己和当前办理人），填写原因（选填），确认后调用 `/add-sign`，`signMode` 固定 `PARALLEL`，带幂等凭证。
- 减签：弹层列出 `sign-relations` 中 `status = 1` 的人员（显示姓名），选一人确认后调用 `/reduce-sign`。没有有效加签人员时不显示"减签"。
- 成功后刷新任务详情和加签记录；审批流程卡片下方展示"加签人员"列表（姓名 + 原因 + 有效/已撤回）。

### 6.2 后端：加签记录返回姓名（第 10 章决策 2）

- `FlowTaskSignRelationVO` 增加 `targetUserName`。
- `FlowTaskCandidateMapper.xml` 的 `selectDynamicSignRelations` 增加 `LEFT JOIN sys_user`，写法与 `FlowTaskMapper.xml` 的 `TaskListUserJoins` 一致：`candidate_value` 为纯数字时关联、`del_flag = 0`、同租户有效成员；只读改动，没有 `${}`。

### 6.3 后端：节点"允许加签"下发与校验（第 10 章决策 4）

- `TaskFormInfo` 增加 `allowAddSign`；`FlowTaskNodePolicy` 读取 BPMN 节点属性 `flowable:allowAddSign`。
- **只认 BPMN 属性，不读 `sys_flow_node_config.allow_add_sign`**（实施时修正）：该列默认值为 `0`，设计器从未写入，所有存有节点配置的流程都会是 `0`，读取它会让这些节点全部禁止加签，违背"未配置视为允许"。
- **未配置时视为允许**（与当前行为一致）；BPMN 明确配置为 `false` 时 H5 隐藏按钮，加签时拒绝"当前节点不允许加签"。校验挂在加签专用协调器的操作人校验之后（`FlowTaskServiceImpl.addSignCoordinator`），`FlowTaskDynamicSignCoordinator` 本身不改。减签不受影响，保证已加的人可以移除。
- 回滚：删除 `addSignCoordinator` 中的策略校验即恢复原行为；无数据库变更。

## 7. 接口清单（H5 `api/index.js` 新增）

| 方法 | 请求 |
|------|------|
| `getMyCcPage(params)` | `GET /api/flow/cc/my` |
| `getCcUnreadCount()` | `GET /api/flow/cc/unread/count`，取 `count`（实施时核实：H5 代理到的 flow 服务控制器有该接口，提案调研看的是插件内控制器） |
| `getCcFormInfo(id)` | `GET /api/flow/cc/form/{id}` |
| `markCcRead(id)` | `POST /api/flow/cc/read/{id}` |
| `markAllCcRead()` | `POST /api/flow/cc/read/all` |
| `remindFlowTask(taskId)` | `POST /api/flow/task/remind`，`params: { taskId }` |
| `addFlowTaskSign(data)` | `POST /api/flow/task/add-sign` |
| `reduceFlowTaskSign(data)` | `POST /api/flow/task/reduce-sign` |
| `getFlowTaskSignRelations(taskId)` | `GET /api/flow/task/{taskId}/sign-relations`，`params: { userId }` |

- 全部 `needTip: false`，由页面统一提示；POST 带 `encrypt: true`（flow 服务类上 `@ApiDecrypt`），路径参数 `encodeURIComponent`。
- 请求体全部是 DTO 字段，不新增 Map 请求体。

## 8. 测试

### 8.1 前端单测

- `utils/__tests__/flow-cc.test.js`：抄送分页归一化、列表项展示字段回退、未读数格式化。
- `utils/__tests__/flow-remind.test.js`：可催办判断（运行中且有办理人）、本地冷却读写和过期。
- `utils/__tests__/flow-sign.test.js`：加签可见性（办理人/所有者、节点策略、只读模式）、有效加签人员筛选、姓名回退为 ID。
- `store/modules/__tests__/cc.test.js`：标记已读后未读数减 1 且不为负；全部已读清零；接口失败保留旧值。

### 8.2 契约测试（`console-design-system.test.js` 追加）

1. `pages/flow/cc-detail` 已注册且不是 `custom` 导航。
2. 第 7 章接口路径存在，POST 带 `encrypt: true`。
3. `todo-detail.vue` 不超过 600 行；拆出的组件文件存在。
4. 待办页有 `cc` 页签；底部 `AiTabBar` 待办角标不读取抄送未读数。
5. 加签请求固定 `signMode: 'PARALLEL'` 并使用 `createFlowActionCredentials`。

### 8.3 后端测试（`forge-plugin-flow`，`-Penable-tests`）

- `FlowTaskRemindCoordinatorTest`：未签收报错；限流 key 格式与 10 分钟有效期；第二次催办报错；Redis 异常时放行；消息发送失败不释放 key。
- `FlowTaskCandidateRelationContractTest`（实施时并入既有契约类）：`selectDynamicSignRelations` 返回 `targetUserName`、关联条件含租户和 `del_flag = 0`、没有 `${}`。
- `FlowTaskNodePolicy` 用例：`allowAddSign` 未配置为 `true`、配置 `false` 生效；加签被拒、减签不受影响。
- 已有 `FlowTaskSignContractTest` 等用例必须保持通过。

## 9. 状态流转与风险（人工审查项）

- **加签/减签改变任务候选人**：H5 只调用现有接口，操作人校验、幂等、50 人上限都在后端；本变更只增加节点策略校验（6.3）。
- **催办行为变化**：未签收任务由"静默成功"改为报错；同一人同一任务 10 分钟内只能催一次。管理端没有页面使用催办，不受影响。
- **限流降级**：Redis 不可用时放行，可能短时间多发提醒，不影响流程状态。
- **抄送业务表单可见性**：只读业务上下文接口对抄送人是否放行未确认；不放行时降级为提示，联调时确认，若需要放行另立变更评估。
- **拆分回归**：`todo-detail` 拆分不改行为，拆分提交单独验证（单测、构建、预览审批详情）。
- **回滚**：前端回滚相关提交；后端改动无数据库变更（加签姓名为只读 SQL、限流用 Redis 临时 key），回滚代码即可。

## 10. 决策

### 已确认（2026-10-10）

1. 催办频率：后端 Redis 限流（同一人同一任务 10 分钟一次）+ H5 按钮冷却。
2. 减签显示姓名：后端加签记录查询顺带返回姓名。
3. 抄送未读数：只在待办页"抄送我的"页签显示，不计入底部"待办"角标。
4. 节点"允许加签"：按 6.3 下发并校验，未配置视为允许。
