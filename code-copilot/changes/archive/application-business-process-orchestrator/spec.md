# 应用级业务流程编排器
> status: apply
> created: 2026-08-03
> complexity: 🔴复杂
> change: `application-business-process-orchestrator`

## 1. 背景与目标

Forge 已经以“应用”作为低代码开发主入口，一个应用可以聚合页面、表单、业务对象、访问入口和发布版本。但流程相关能力仍分散在业务对象流程绑定、自动化触发器、业务动作和 Flowable 流程设计器中。用户必须先理解这些底层资产的差异，再手工拼接“何时触发、操作哪条记录、是否发起审批、审批完成后执行什么动作”，导致应用工作台虽然以应用为入口，业务流程设计仍以对象和技术配置为中心。

本变更在应用工作台建立统一“业务流程”核心面板。一个应用可以创建多个业务流程，每个流程选择一个主业务对象作为记录主体，通过一张可视化画布完成触发、条件、审批子流程、数据动作、消息和结束结果的编排。Flowable 继续作为人工审批权威引擎，现有动作、消息、定时任务、企业集成和统一能力开放平台继续作为节点执行能力；新画布只负责应用级编排、版本、运行状态和恢复，不重造这些底层引擎。

完成后必须达到以下可验证结果：

1. 应用工作台只有一个面向普通设计者的“业务流程”入口，不再要求在“业务流程、自动化触发器、自动化动作”三个入口间切换。
2. 一个应用可以维护多个有稳定编码、独立草稿和发布版本的业务流程；每个流程首版固定一个开始节点和一个主业务对象，可包含多个条件、动作、审批和结束节点。
3. “提交审批”“记录新增后自动审批”“审批通过后更新状态并通知”等场景可以在一张业务画布中完整表达。
4. 审批节点引用一个已发布 Flowable 模型；点击节点打开现有真实流程设计器，会签、驳回、退回、抄送、审批人和节点字段权限仍保存在 BPMN 中。
5. 业务流程发布后形成不可变版本。应用发布快照固定业务流程版本、业务对象版本和审批模型版本，运行中的实例不受后续草稿修改影响。
6. 事件、定时和手动入口先创建持久化运行实例，再异步执行；审批节点进入等待态，收到通过、驳回或取消回调后从对应出口继续运行。
7. 旧触发器、对象流程绑定和自动化动作可以预览迁移结果并幂等转换；旧入口停止新增和修改前必须完成迁移校验，历史配置、运行实例和日志不物理删除。
8. 所有状态变更、权限、执行身份、幂等、重试、外部调用和审计都在服务端校验；画布和前端按钮不能绕过现有业务运行时。

### 1.1 目标信息架构

```text
应用工作台
├── 概览
├── 页面与表单
├── 业务流程
│   ├── 流程列表
│   ├── 业务编排画布
│   ├── 运行记录
│   └── 迁移与问题
├── 访问入口
├── 高级数据设置
└── 发布
```

### 1.2 本次范围

- 应用级业务流程定义、不可变版本、运行实例和节点运行记录。
- 独立的 `businessProcessJson` 协议、服务端校验器和发布编译器。
- 触发、条件、审批子流程、动作和结束节点的可视化编排。
- 事件、定时、手动三类开始节点；开始节点支持结构化条件，不开放任意脚本。
- 更新记录、创建记录、复用业务动作、发送消息、调用受治理能力、调用子流程等动作节点。
- 审批子流程的启动、等待、回调恢复、结果出口和业务状态修复。
- 手动开始节点发布为列表、详情或表单操作，复用现有页面动作权限与运行机制。
- 应用发布快照、就绪检查、回滚、执行日志、失败重试和迁移工具。
- 采购审批样例和现有低代码示例迁移为新业务流程画布。

### 1.3 非目标

- 不用新的业务编排器替代 Flowable，也不在应用中心建设第二套审批节点配置界面。
- 不把现有 `DingFlowDesigner.flowJson` 直接作为业务编排存储协议，也不把整张业务画布转换为一个大 BPMN 模型。
- 首版不支持业务画布任意回环、无限循环、动态代码、任意 Java 类、任意 SQL 或任意 URL Webhook。
- 首版不支持跨应用引用未发布流程；子流程只能引用当前应用内已发布业务流程，并必须通过静态循环检查和最大调用深度限制。
- 不承诺跨数据库业务写入、Flowable 启动和外部系统调用的全局事务；通过检查点、幂等、重试和补偿处理部分成功。
- 不允许同一业务记录同时存在多个活动审批子流程；多个纯自动化流程可以并行，但审批启动必须按 `tenantId + businessKey` 检查活动实例。
- 不删除业务动作运行时。业务动作继续作为原子能力存在，只取消普通用户需要单独进入 `BusinessActionDesigner` 维护审批自动化的主路径。
- 不在本 Proposal 阶段修改任何生产代码、数据库、菜单、样例或运行配置。

## 2. 代码现状（Research Findings）

### 2.1 应用工作台只有跳转聚合，没有流程编排

- `forge-admin-ui/src/views/app-center/application-workspace/ApplicationAutomationPanel.vue` 的 `ApplicationAutomationPanel` 按应用对象展示“业务流程、自动化触发器、业务动作”三个按钮，点击后分别打开对象设计器面板；当前“流程自动化”只是导航聚合，不是应用级流程资产。
- `forge-admin-ui/src/views/app-center/application.[applicationCode].vue` 把 `automation` 映射到 `ApplicationAutomationPanel`，工作台没有业务流程列表、画布、版本或运行记录。
- `forge-admin-ui/src/views/app-center/object-designer.[objectCode].vue` 继续分别加载 `BusinessFlowAppConfigPanel`、`trigger.vue` 和 `BusinessActionDesigner`，应用上下文在进入对象后退化为对象上下文。

### 2.2 触发器只能表达一个触发和一个动作

- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/domain/entity/AiBusinessTrigger.java` 的 `AiBusinessTrigger` 只保存 `eventType/eventCondition/actionType/actionConfig`，无法表达多步骤、审批等待、结果出口和节点级重试。
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessTriggerExecutor.java` 的 `BusinessTriggerExecutor#executeAction` 直接在 `START_FLOW/SEND_MESSAGE/CREATE_RECORD/UPDATE_FIELD/WEBHOOK` 之间分支；`WEBHOOK` 仍返回 TODO，且整个触发器只产生一次动作结果。
- `forge-admin-ui/src/views/app-center/components/TriggerActionConfigPanel.vue` 的 `START_FLOW` 配置固定 `useMainFlow=true`，要求先在对象流程配置中维护主流程，用户仍需跨入口完成同一业务链路。
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessTriggerSchedulerService.java` 已通过单个 `LOWCODE.lowcodeBusinessTriggerScanJob` 扫描定时触发器并具备集群锁、记录锁和分层提醒，应保留调度机制，只替换配置来源和启动目标。

### 2.3 业务动作已经具备可复用步骤执行基础

- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessActionExecutionService.java` 的 `BusinessActionExecutionService#executeSteps` 已按 `BusinessActionStepExecutor.supportType()` 注册并顺序执行步骤，具备步骤结果、幂等日志和失败边界。
- 当前步骤执行器已经覆盖 `CREATE_RECORD/UPDATE_FIELD/SEND_MESSAGE/START_FLOW/DOMAIN_ACTION/FOREACH`，出处为同包下 `CreateRecordActionStepExecutor`、`UpdateFieldActionStepExecutor`、`SendMessageActionStepExecutor`、`StartFlowActionStepExecutor`、`DomainActionStepExecutor` 和 `ForeachActionStepExecutor`。
- `forge-admin-ui/src/views/app-center/components/designer/BusinessActionDesigner.vue` 只对数量处理和部分嵌套步骤提供可视化，其余步骤回退高级 JSON；它把审批发起、审批结果动作和页面动作分开解释，进一步增加认知负担。
- 结论：动作执行器可以作为新业务流程动作节点的底层能力，但需要抽取公共步骤运行服务，不能让新编排器复制 `BusinessTriggerExecutor` 的动作分支。

### 2.4 Flowable 绑定仍以业务对象和业务记录为运行主体

- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessFlowService.java` 的 `BusinessFlowService#startFlow` 固定查询 `targetType=OBJECT/bindingType=FLOW`，运行时使用 `objectCode:recordId` 作为 `businessKey`。
- `AiBusinessBinding` 虽允许 `APPLICATION` 目标，但现有业务启动、任务表单、状态回写和 `ai_business_flow_instance_link` 都以业务对象和记录为事实来源，不能仅把绑定行从 `OBJECT` 改成 `APPLICATION`。
- `BusinessFlowService` 已提供流程启动锁、业务实例链接、变量映射、任务表单、回调和结果事件，应由审批子流程节点调用，不新增第二条前端自定义启动链路。
- `.agents/skills/forge-business-flow-development/references/bpmn-configuration.md` 与 `code-copilot/memory/decisions.md` 已冻结：审批节点表单、字段权限、审批人、会签、驳回和监听器归真实流程设计器维护，应用中心只能维护业务对象、流程模型和变量映射。

### 2.5 DingFlowDesigner 可以复用画布基础，不能直接复用协议

- `forge-admin-ui/src/components/flow-designer/constants/node-types.js` 把 `flowJson` 的 12 类节点直接映射到 BPMN `StartEvent/UserTask/ServiceTask/Gateway/SubProcess/CallActivity`。
- `forge-admin-ui/src/components/flow-designer/converter/json-to-bpmn.js` 的 `convertJsonToBpmn` 会把整份 `flowJson` 写成 BPMN 2.0 XML；协议中没有应用、主业务对象、触发身份、节点重试和发布依赖语义。
- `forge-admin-ui/src/components/flow-designer/composables/useFlowDesigner.js` 已提供节点/边增删、分支、布局、撤销重做等可复用图编辑能力，但新增条件分支会默认创建审批人节点，不能直接用于通用业务节点。
- 结论：抽取或复用 `FlowCanvas`、连线、布局、选择、撤销重做等视图基础；业务流程使用独立节点注册表和 `businessProcessJson`，现有 `DingFlowDesigner` 继续服务 BPMN 审批设计。

### 2.6 应用发布已经具备快照和可恢复步骤

- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessApplicationSnapshotService.java` 已把应用、对象、入口、绑定、扩展和权限写入白名单化快照，并清洗敏感键。
- `BusinessApplicationPublishService` 和 `BusinessApplicationPublishStep` 已按 `PRECHECK/SNAPSHOT/OBJECTS/ENTRIES/PAGE_MENUS/EXTENSIONS/COMMIT` 执行可恢复发布步骤，可新增 `PROCESSES` 步骤和流程版本引用，不另建第二套应用发布入口。
- `forge-admin-ui/src/components/ai-form/AiCrudPage.vue` 已识别 `START_FLOW/COMMAND` 等运行时动作，适合增加稳定的 `START_PROCESS` 动作类型，由应用发布编译生成，不在浏览器动态注册后端能力。

### 2.7 主要问题与风险

- 应用级流程、对象主流程、触发器和动作缺少唯一事实来源，发布快照无法固定完整业务链路。
- `@Async` 触发执行没有应用级持久化运行实例；服务重启后无法从审批等待节点或失败节点恢复整张业务链路。
- 审批结果动作与状态回写分散在 Flow Binding、BusinessAction 和 Trigger 中，可能重复执行或产生相互覆盖。
- 定时任务、事件触发和外部能力调用的执行身份不同；若不冻结身份策略，可能以管理员或错误租户发起审批。
- 直接删除旧配置会破坏运行中的 Flowable 实例、历史审计和存量应用回滚，且违反项目逻辑删除与可恢复规则。

## 3. 功能点

### 3.1 P0：应用级流程资产与统一入口

- [ ] 功能 1：应用工作台新增“业务流程”面板，展示流程名称、主业务对象、触发方式、草稿/发布状态、当前版本、运行统计和问题数。
- [ ] 功能 2：支持在应用内新增、复制、编辑、停用和逻辑删除业务流程；同一租户和应用内有效 `processCode` 唯一，编码创建后不可修改。
- [ ] 功能 3：每个业务流程首版必须选择当前应用已关联的一个主业务对象；业务运行记录使用该对象和记录 ID 构建 `businessKey=<objectCode>:<recordId>`。
- [ ] 功能 4：应用可创建多个流程，但首版每个流程只允许一个开始节点，业务画布必须是有向无环图；多个流程可以引用同一对象，但同一记录同时只允许一个活动审批子流程。

### 3.2 P0：独立业务编排协议与设计器基础

- [ ] 功能 5：定义版本化 `businessProcessJson`，至少包含 `schemaVersion/processCode/subject/nodes/edges/policies/dependencies`，所有 ID 以字符串传输。
- [ ] 功能 6：复用 DingFlowDesigner 的画布、连线、布局、选择和历史能力，建立独立业务节点注册表、节点卡片和右侧配置面板；不得修改 BPMN `flowJson` 含义来兼容业务节点。
- [ ] 功能 7：服务端发布校验覆盖单开始节点、可达结束节点、无环、合法出口、对象/字段存在、节点配置完整、依赖已发布、子流程无递归和敏感配置清洗。
- [ ] 功能 8：设计器支持草稿自动保存提示、显式保存、撤销/重做、缩放、居中、节点复制、删除、分支和发布问题定位。

### 3.3 P0/P1：开始节点

- [ ] 功能 9：事件开始节点支持 `RECORD_CREATED/RECORD_UPDATED/RECORD_DELETED/STATUS_CHANGED/FIELD_CHANGED/FLOW_APPROVED/FLOW_REJECTED/FLOW_CANCELED` 和结构化条件。
- [ ] 功能 10：新增 `FORM_SUBMITTED/ACTION_EXECUTED` 业务语义事件；审批场景默认使用显式“提交审批”，不把普通草稿保存等同于业务提交。
- [ ] 功能 11：定时开始节点支持到期字段、提前/回看天数、分层规则、批量和最小间隔，继续由 `LOWCODE.lowcodeBusinessTriggerScanJob` 统一扫描。
- [ ] 功能 12：手动开始节点支持列表行、详情、表单和工具栏位置，发布后编译为 `START_PROCESS` 页面动作；按钮权限、可见条件、确认文案和幂等键由服务端发布快照固定。

### 3.4 P1：条件、动作与结束节点

- [ ] 功能 13：条件节点使用结构化规则，支持 AND/OR、字段比较、状态、审批结果和当前上下文；普通模式不暴露 SpEL、脚本或任意表达式。
- [ ] 功能 14：动作节点支持更新记录、创建记录、执行已发布业务动作、发送消息、调用受治理能力和调用当前应用子流程。
- [ ] 功能 15：消息动作选择消息模板、发送通道和接收人规则；企业微信等外部协同渠道继续经过消息模块和协同连接，不在画布保存 Secret。
- [ ] 功能 16：外部调用使用统一能力开放平台或企业集成连接引用；不提供自由 URL Webhook，凭据、目标白名单、超时和审计由既有平台能力负责。
- [ ] 功能 17：结束节点只表达 `SUCCESS/REJECTED/FAILED/CANCELED` 结果，不执行隐藏状态回写；业务状态变化必须使用显式更新记录节点或审批状态映射。
- [ ] 功能 18：子流程节点只能引用同一应用已发布业务流程，发布时检查直接/间接循环，运行时限制最大调用深度并继承可信租户和执行身份。

### 3.5 P1：审批子流程节点

- [ ] 功能 19：审批节点选择已发布 Flowable 模型，配置标题模板、表单资产、字段到流程变量映射、审批版本策略和结果出口。
- [ ] 功能 20：点击审批节点内嵌打开现有 `flow/design.vue` 和 `DingFlowDesigner`；审批人、会签、抄送、驳回、退回、监听器和节点字段权限只写 BPMN。
- [ ] 功能 21：审批节点启动成功后保存 `processInstanceId` 和 `ai_business_flow_instance_link`，业务流程运行实例进入 `WAITING`；不得在画布执行线程中轮询 Flowable。
- [ ] 功能 22：Flowable 的通过、驳回、取消和终结回调以 `processInstanceId + businessKey` 恢复唯一等待节点，并从 `APPROVED/REJECTED/CANCELED/FAILED` 出口继续。
- [ ] 功能 23：审批状态继续由任务创建、任务完成和终态回调幂等维护并支持查询修复；不能只依赖流程结束事件更新业务状态。

### 3.6 P1：持久化运行、身份与可靠性

- [ ] 功能 24：触发命中后同步创建唯一业务流程运行实例，再异步调度节点；运行状态支持 `PENDING/RUNNING/WAITING/SUCCESS/FAILED/CANCELED`。
- [ ] 功能 25：节点运行记录保存节点、尝试次数、开始/结束时间、安全输入摘要、结果摘要、关联 ID、错误码和下一重试时间，不保存 Token、Secret 或完整敏感报文。
- [ ] 功能 26：节点执行器按注册表解析，现有 `BusinessActionStepExecutor` 抽为公共步骤运行服务，新触发器和旧业务动作共用同一执行合同。
- [ ] 功能 27：事件、定时、手动和流程回调均生成稳定幂等键；外部副作用使用 `runId + nodeId` 稳定键，重试不能重复创建记录、重复发起审批或重复发送消息。
- [ ] 功能 28：失败策略支持停止、有限次数重试和人工重试；跨系统部分成功不做伪事务回滚，节点必须记录检查点并按能力提供补偿。
- [ ] 功能 29：手动触发使用当前登录用户和当前组织；记录事件使用可信原操作人；定时任务使用显式服务身份。需要发起审批但没有合法普通用户发起人时失败关闭，不回退管理员。
- [ ] 功能 30：所有业务对象读写继续经过租户、当前组织、数据权限、字段白名单和业务状态校验；计划任务和回调在最小数据库边界建立并恢复可信租户上下文。

### 3.7 P1：应用发布、版本和运行治理

- [x] 功能 31：应用发布新增 `PROCESSES` 步骤，为每个选中流程生成不可变版本，并把业务流程版本、业务对象版本、Flowable 模型版本和能力引用写入应用快照。
- [ ] 功能 32：流程草稿修改不影响正式运行；新触发只解析应用当前发布快照，运行中的实例始终读取自身固定流程版本。
- [ ] 功能 33：应用回滚生成新的应用发布版本，并重新激活目标流程版本和手动按钮投影；已开始的流程实例不回滚、不切换定义。
- [ ] 功能 34：运行记录支持按应用、流程、对象、记录、状态、时间和关联 ID 查询，提供节点时间线、失败原因和受权限控制的人工重试。
- [x] 功能 35：发布就绪检查阻断无主对象、对象未发布、审批模型未发布、字段映射失效、无结束路径、递归子流程、审批并发冲突和手动按钮权限缺失。

### 3.8 P2：存量迁移与旧入口收口

- [ ] 功能 36：迁移预览按旧触发器、FLOW Binding 和自动化动作生成新流程草稿、字段映射、警告和无法转换原因，不修改旧配置。
- [ ] 功能 37：迁移执行以 `legacySourceType + legacySourceId` 幂等；简单触发器转换为“开始节点 → 动作/审批节点 → 结束节点”，审批结果动作转换为结果出口后的显式动作。
- [ ] 功能 38：旧业务动作定义继续作为可复用原子能力；仅审批自动化的独立普通入口收口到新画布，页面交互动作和领域动作运行时不删除。
- [ ] 功能 39：迁移校验通过后，应用和对象设计器不再展示旧触发器、旧流程绑定和旧自动化动作入口；旧 API 进入受控只读/兼容期，运行中的旧实例继续执行。
- [ ] 功能 40：采购审批样例和相关 seed 转为应用级业务流程；初始化不得覆盖用户已经编辑或部署的 BPMN XML。

## 4. 业务规则

### 4.1 应用、流程和业务对象所有权

1. 应用拥有业务流程目录、草稿、版本、发布和权限；业务流程不能脱离应用独立发布。
2. 业务对象拥有字段、数据表、表单、列表、详情和业务记录，是流程事件与 `businessKey` 的来源；对象仍可被多个应用复用。
3. 每个首版业务流程必须有一个主业务对象。动作可以读取或写入当前应用内其它对象，但必须显式配置对象和字段映射。
4. 一个应用可以有多个流程；同一流程只有一个开始节点。需要多种触发方式时创建多个流程或由一个手动动作调用子流程，不在首版允许多个开始节点竞争同一运行实例。
5. 一个业务记录同一时间只允许一个活动审批子流程。纯数据和消息自动化可以并行，但必须使用各自流程版本和幂等键。

### 4.2 业务编排与 BPMN 边界

1. `businessProcessJson` 是应用业务编排事实来源；`flowJson/BPMN XML` 是审批子流程事实来源，二者不能双写同一审批节点配置。
2. 业务画布只保存审批模型引用、业务字段映射、标题、表单引用和结果出口。审批节点内部配置只能在真实流程设计器修改。
3. 审批节点引用必须在应用发布时固定到已发布模型版本或部署标识；后续重新部署不静默改变已发布业务流程版本。
4. 业务流程结束节点不得隐式更新字段、发消息或调用外部系统。所有副作用必须是可见、可审计的动作节点。
5. `APPROVED/REJECTED/CANCELED/FAILED` 是审批节点标准出口。一般字段判断使用条件节点，不要求用户再次编写“审批是否通过”的重复条件。

### 4.3 触发与手动提交

1. `RECORD_CREATED` 表示业务记录新增成功，不等同于“提交审批”。草稿型单据默认通过 `FORM_SUBMITTED` 或手动“提交审批”开始流程。
2. 记录事件必须在业务事务完成后进入流程调度；流程运行实例创建成功后才异步执行节点，禁止仅依赖进程内事件维持可靠性。
3. 手动按钮只引用稳定 `applicationCode + processCode`，不能把整份流程 JSON 或 Flowable 参数下发到前端。
4. 手动触发必须重新读取业务记录、校验当前状态、操作权限、数据权限和活动审批实例；前端传入的记录数据只能作为显示提示，不能作为权威执行数据。
5. 定时触发不为每个流程创建 Quartz Job；统一扫描任务读取已发布的定时开始节点，保留集群锁、记录锁和分层规则。

### 4.4 状态、身份与幂等

1. 状态变化必须通过业务状态机或显式受控更新节点，禁止任意写状态字段绕过合法流转校验。
2. 业务审批状态至少兼容 `DRAFT/IN_PROCESS/NEED_MODIFY/APPROVED/REJECTED/CANCELED`，具体字典可以由对象配置扩展。
3. Flowable 审批启动和节点恢复必须使用 `businessKey=<objectCode>:<recordId>`，前端与 JSON 中的雪花 ID 全部使用字符串。
4. 手动、事件、定时和外部调用分别解析可信执行身份；租户、用户、当前组织和权限不得来自画布自由输入或请求 Header 自报。
5. 流程运行唯一键至少包含租户、流程版本和来源幂等键。每个副作用节点使用稳定节点幂等键，重试和回调乱序必须安全。
6. 回调恢复必须校验运行实例仍处于对应审批节点的 `WAITING` 状态、流程实例匹配、结果尚未消费；重复回调返回幂等成功。

### 4.5 版本、发布和删除

1. 设计草稿可修改；发布版本不可变。任何节点、连线、映射或依赖变更必须形成新流程版本。
2. 应用发布快照是正式运行入口，必须固定选中的流程版本和所有依赖摘要。正式运行不得读取当前流程草稿。
3. 删除流程使用逻辑删除。存在运行中实例或仍被已发布应用版本引用时阻断删除，允许停用以禁止新触发。
4. 运行实例、节点日志和流程版本用于审计，不提供普通物理删除；超期日志只能由明确留存任务按策略清理。
5. 旧配置迁移完成前可以兼容执行，但只有一个可写设计入口。迁移不得物理删除旧配置，也不得改变运行中 Flowable 实例使用的历史绑定。

### 4.6 外部能力安全

1. Webhook、企业微信、邮件、短信、系统服务和第三方接口只能引用已注册消息模板、企业集成连接或统一能力开放平台能力。
2. 业务流程 JSON、版本快照、运行上下文和日志不得包含 Secret、Token、私钥、Cookie、Authorization 或完整敏感报文。
3. 外部调用必须具备目标白名单、超时、限流、幂等和安全审计；失败不得自动切换到未配置的其它通道或身份。
4. 高风险、资金、权限和状态动作继续执行原业务服务的专项校验，流程画布不能因节点已发布而绕过审批或人工审查。

## 5. 数据变更

进入实施时已重新扫描 Flyway 目录：当前最新为并行变更中的 `V1.0.82`，本变更连续使用未占用的 `V1.0.83/V1.0.84`；若提交前出现新版本占用，必须 Reverse Sync 后顺延，禁止覆盖或修改已执行脚本。

### 5.1 新表

| 操作 | 表名 | 关键字段/索引 | 说明 |
|---|---|---|---|
| 新增 | `ai_business_process` | `id, tenant_id, application_id, process_code, process_name, subject_object_id, subject_object_code, draft_schema_json, design_status, current_version, published_version, status, legacy_source_type, legacy_source_id, del_flag`；有效唯一索引 `(tenant_id, application_id, process_code, del_flag)`；非空旧来源唯一索引 `(tenant_id, legacy_source_type, legacy_source_id, del_flag)` | 应用级流程定义和当前草稿；`del_flag BIGINT` 删除时写主键；旧来源唯一索引保障简单来源迁移幂等，合并来源继续由迁移服务和 `metadata.legacySources[]` 校验 |
| 新增 | `ai_business_process_version` | `id, tenant_id, process_id, version_no, schema_version, schema_json, schema_hash, dependency_snapshot_json, publish_time, status, del_flag`；唯一索引 `(tenant_id, process_id, version_no, del_flag)` | 不可变发布版本和依赖快照，不提供普通修改接口 |
| 新增 | `ai_business_process_run` | `id, tenant_id, application_id, process_id, process_version_id, process_code, subject_object_code, subject_record_id, business_key, trigger_type, source_event_id, idempotency_key, actor_type, actor_user_id, active_org_id, status, current_node_id, flow_process_instance_id, context_snapshot, retry_count, next_retry_time, error_code, error_summary, start_time, end_time`；唯一索引 `(tenant_id, process_version_id, idempotency_key)` | 持久化编排实例、审批等待关联和恢复检查点 |
| 新增 | `ai_business_process_node_run` | `id, tenant_id, run_id, node_id, node_type, attempt_no, status, idempotency_key, correlation_id, input_summary, output_summary, error_code, error_summary, next_retry_time, start_time, end_time`；唯一索引 `(tenant_id, run_id, node_id, attempt_no)` | 节点时间线、重试和安全摘要 |

上述表全部包含 `create_by/create_time/create_dept/update_by/update_time`，字符集 `utf8mb4`、引擎 `InnoDB`。定义和版本表属于用户可见设计元数据，使用逻辑删除；运行与节点运行表属于审计运行表，不提供行级删除接口，超期数据由后续明确留存任务物理清理。

### 5.2 既有表和快照

| 操作 | 对象 | 变更 | 说明 |
|---|---|---|---|
| 修改 | 应用发布快照 JSON | 增加 `processes[]`、`publishedProcessVersions[]` 和 `runtimeActions[]` | 固定业务流程版本、依赖和手动动作投影 |
| 兼容读取 | `ai_business_trigger` | 不新增新配置；迁移后进入只读兼容 | 不物理删除，旧运行和回滚继续可追溯 |
| 兼容读取 | `ai_business_binding` | 旧 `OBJECT/FLOW` 绑定继续服务运行中实例；新流程不再以该表作为编排事实来源 | BPMN 节点表单兼容兜底继续保留 |
| 保留 | 业务对象动作配置和 `ai_business_action_execution_log` | 继续作为原子业务动作和执行审计 | 取消分散主入口，不删除运行时能力 |
| 修改 | `sys_resource` | 新增业务流程管理、运行记录、重试、迁移预览/执行 API 权限 | `tenant_id=1`，全部使用 `NOT EXISTS` |

### 5.3 迁移标识

`ai_business_process.legacy_source_type/legacy_source_id` 用于记录 `TRIGGER/FLOW_BINDING/AUTOMATION_ACTION` 来源并建立幂等迁移约束。同一旧配置重复执行迁移时返回已有流程，不重复创建。包含多个旧来源的合并流程在 `draft_schema_json.metadata.legacySources[]` 保存脱敏引用，迁移报告列出被合并关系。

## 6. 接口变更

### 6.1 设计与管理接口

| 操作 | 接口 | 方法 | 变更内容 |
|---|---|---|---|
| 新增 | `/ai/business/process/page` | GET | 按应用分页查询业务流程 |
| 新增 | `/ai/business/process/:id` | GET | 查询流程定义、草稿摘要和发布状态 |
| 新增 | `/ai/business/process` | POST | 在应用内创建业务流程 |
| 新增 | `/ai/business/process/:id/copy` | POST | 在同一应用内复制流程为新草稿；必须提供新的唯一编码，不复制发布和运行状态 |
| 新增 | `/ai/business/process` | PUT | 修改名称、描述、主对象和草稿协议；编码不可修改 |
| 新增 | `/ai/business/process/:id` | DELETE | 无运行中实例和有效发布引用时逻辑删除 |
| 新增 | `/ai/business/process/:id/designer` | GET | 获取完整草稿、字段目录、节点能力和依赖候选 |
| 新增 | `/ai/business/process/:id/schema` | PUT | 保存 `businessProcessJson` 草稿和摘要哈希 |
| 新增 | `/ai/business/process/:id/validate` | POST | 执行发布前图、对象、字段、身份和依赖校验 |
| 新增 | `/ai/business/process/:id/status` | PUT | 启用或停用新触发，不改变历史版本 |

### 6.2 运行与运维接口

| 操作 | 接口 | 方法 | 变更内容 |
|---|---|---|---|
| 新增 | `/ai/business/process/runtime/:applicationCode/:processCode/start` | POST | 执行已发布手动开始节点；服务端重新加载业务记录和权限 |
| 新增 | `/ai/business/process/run/page` | GET | 查询应用级流程运行记录 |
| 新增 | `/ai/business/process/run/:id` | GET | 查询运行详情和节点时间线 |
| 新增 | `/ai/business/process/run/:id/retry` | POST | 对可重试失败实例执行受权限控制的人工重试 |
| 新增 | `/ai/business/process/run/:id/cancel` | POST | 取消尚未进入不可逆终态的业务流程运行；审批取消仍调用 Flowable 受控接口 |

### 6.3 迁移接口

| 操作 | 接口 | 方法 | 变更内容 |
|---|---|---|---|
| 新增 | `/ai/business/process/migration/preview` | POST | 按应用预览旧触发器、绑定和动作转换结果 |
| 新增 | `/ai/business/process/migration/apply` | POST | 对预览签名一致的结果执行幂等迁移 |
| 新增 | `/ai/business/process/migration/issues` | GET | 查询无法自动转换、字段失效和审批冲突问题 |

旧 `/ai/business/trigger/**` 和 `/ai/business/flow/binding/**` 在迁移期保留兼容。停止编辑前先在前端移除普通入口并对写接口增加迁移状态保护，不能直接删除 Controller 或数据库表。

## 7. 影响范围

### 7.1 后端

- `forge-plugin-generator/domain/entity`：新增流程定义、版本、运行和节点运行实体。
- `forge-plugin-generator/mapper` 与 `resources/mapper`：新增 XML 查询、CAS 状态迁移、运行锁定和迁移查询。
- `forge-plugin-generator/service/businessprocess`：新增 Schema、校验、编译、触发分发、编排运行、节点注册、审批恢复、发布和迁移服务。
- `BusinessEventPublisher/BusinessTriggerExecutor/BusinessTriggerSchedulerService`：新配置优先进入编排器，旧配置保留兼容适配。
- `BusinessActionExecutionService` 及步骤执行器：抽取公共步骤运行合同，供业务动作与流程动作节点共同使用。
- `BusinessFlowService`：支持显式审批节点上下文、返回关联信息并发布可靠恢复事件，任务表单和状态回写逻辑不迁移到新模块。
- `BusinessApplicationSnapshotService/BusinessApplicationPublishService/BusinessApplicationRuntimeService`：加入流程版本、`PROCESSES` 发布步骤和手动动作投影。
- `forge-server/db/migration`：新增表、索引、字典和权限资源；迁移 JSON 语义由 Java 服务处理，不在 Flyway 中拼接复杂画布。

### 7.2 前端

- `application-workspace/ApplicationAutomationPanel.vue`：替换为应用级业务流程列表、运行记录和迁移问题入口。
- `application.[applicationCode].vue` 与 `ApplicationWorkspaceNav.vue`：将 `automation` 收口为唯一“业务流程”核心分区。
- `components/business-process-designer/`：新增业务画布、节点注册、节点卡片、配置面板、协议归一化和校验提示。
- `components/flow-designer/`：只抽取可复用图基础，不改变 `DingFlowDesigner` 的 BPMN 语义和转换测试。
- `object-designer.[objectCode].vue`：迁移完成后移除普通用户的旧流程绑定、触发器和自动化动作入口，保留高级兼容诊断。
- `AiCrudPage.vue` 与 `views/ai/crud-page.vue`：新增 `START_PROCESS` 动作协议、加载态、确认、权限和运行结果处理。
- `api/business-process.js`：新增设计、运行和迁移 API。

### 7.3 现有样例与运行数据

- 采购审批样例的手动提交、审批流程和结果动作迁移为新业务流程版本。
- `ai_business_flow_instance_link`、Flowable 历史、旧触发器日志和业务动作日志继续保留。
- 旧应用发布快照仍可回滚和运行；新应用发布快照才包含 `processes` 协议。

## 8. 风险与关注点

> ⚠️ 本变更涉及业务状态流转、审批权限、定时服务身份、跨系统调用和旧配置迁移，进入 `/apply` 前必须完成人工安全与状态机审查。

| 风险 | 级别 | 控制措施 |
|---|---|---|
| 把业务画布错误转换为大 BPMN，导致审批和自动化双重事实来源 | Critical | 新建 `businessProcessJson`，只复用画布基础；审批节点引用独立 BPMN |
| 同一记录重复发起审批 | Critical | 活动实例检查、流程运行唯一键、审批节点幂等键和 Flowable 启动锁 |
| 回调乱序或重复导致审批后动作重复 | Critical | 节点 CAS、`processInstanceId` 关联、结果一次性消费和动作幂等键 |
| 定时任务以错误用户或管理员发起审批 | Critical | 显式服务身份和发起人策略；没有合法普通用户时失败关闭 |
| 状态更新绕过业务状态机 | 高 | 状态动作调用领域状态服务；发布校验识别高风险字段，禁止通用字段节点直接改受保护状态 |
| 旧配置清理破坏运行实例和回滚 | 高 | 只读兼容、幂等迁移、逻辑删除、旧快照保留和分阶段停写 |
| 外部调用泄露 Secret 或形成 SSRF | 高 | 只引用统一能力/企业集成连接；快照敏感键清洗和出站白名单 |
| 服务重启丢失等待或失败节点 | 高 | 持久化 run/node_run，启动扫描恢复 `PENDING/RUNNING/WAITING` 异常状态 |
| 应用发布与流程版本部分成功 | 高 | 新增可恢复 `PROCESSES` 步骤、不可变版本和幂等提交，不伪装全局事务 |
| 画布协议无限扩张导致运行时不可控 | 中 | `schemaVersion`、节点白名单、DAG、最大节点数/分支数/子流程深度和严格校验 |

### 8.1 回滚原则

- 新表和新 API 可以通过功能开关关闭，新发布入口回退到旧兼容运行，不删除已生成版本和运行记录。
- 应用发布 `PROCESSES` 步骤失败时保留发布运行单和候选快照，不提交应用新版本；重试复用同一运行单。
- 已迁移旧配置不删除，回退时可以重新启用旧兼容读取；迁移后产生的新运行实例不转换回旧触发器。
- 已启动 Flowable 实例继续按启动时的 BPMN 和业务流程版本完成，不因关闭新设计入口被终止。

## 8.5 测试策略

- **单元测试**：Schema 归一化、图校验、无环/可达性、字段映射、身份策略、幂等键、节点状态机、审批结果出口、子流程循环和敏感键清洗。
- **服务测试**：流程 CRUD、版本不可变、运行 CAS、重试、迁移预览/执行、应用发布 `PROCESSES` 步骤和回滚快照。
- **合同测试**：每种节点执行器使用 Fake Context 通过成功、失败、重试、重复调用和权限失败合同；现有 BusinessAction/Message/Flowable 行为回归。
- **前端测试**：业务画布节点/连线编辑、草稿保存、条件和动作配置、审批设计器打开、问题定位、手动动作投影及旧入口隐藏。
- **集成测试**：记录提交 → 持久化 run → 启动 Flowable → 回调恢复 → 状态更新 → 消息；定时扫描与手动按钮分别覆盖。
- **迁移测试**：旧触发器、FLOW Binding、审批结果动作、无法解析 JSON、失效字段、重复执行和运行中旧实例。
- **安全测试**：跨租户/跨应用/跨对象、无数据权限、伪造 actor、定时无发起人、回调重放、任意 URL/Secret 注入和日志脱敏。
- **性能目标**：应用 100 个流程、单流程 100 个节点时设计详情和发布校验可用；事件匹配只读取当前发布快照索引，不扫描全租户草稿。
- **覆盖率目标**：新增后端协议、状态机、迁移和核心编排分支覆盖率不低于 80%；关键前端协议工具分支覆盖率不低于 80%。
- **独立 Test Spec**：是。进入 `/test` 或编码阶段验证前创建 `test-spec.md` 和 `execution-log.md`，并读取 `code-copilot/rules/automated-testing-standard.md`。

## 9. 待澄清

以下采用推荐默认值写入 Proposal，用户明确确认后才能进入 `/apply`：

- [x] **首版图能力**：每个流程一个开始节点、业务画布只允许 DAG；循环只允许在 Flowable 审批内部或受控 `FOREACH` 动作中存在。
- [x] **审批并发**：同一 `businessKey` 同时只允许一个活动审批子流程；多个应用级自动化可以并行。
- [x] **定时审批发起人**：必须配置一个受限普通服务用户或从记录字段解析唯一普通用户；解析失败时不启动审批，不回退 admin。
- [x] **旧入口停用节奏**：“迁移预览 → 沙箱转换 → 新旧结果对比 → 新入口启用 → 旧写接口锁定 → 观察期后隐藏兼容入口”，不一次性删除旧表和 API。
- [x] **子流程范围**：首版只允许同应用已发布业务流程，最大调用深度 5；跨应用调用后续通过统一能力开放平台实现。
- [x] **自由 Webhook**：不提供，统一使用企业集成连接或能力开放平台中的受治理能力。

## 10. 技术决策

1. **应用拥有流程，业务对象拥有记录。** 流程定义、版本和发布属于应用；每次运行仍必须绑定明确业务对象和记录，不能把应用 ID 当作审批 `businessKey`。
2. **一张业务画布，两套清晰运行边界。** 应用画布负责触发和业务编排；Flowable 负责人工审批。审批节点是引用和等待点，不复制 BPMN 节点配置。
3. **复用画布基础，不复用 BPMN 协议。** `DingFlowDesigner` 的渲染、布局和历史能力可以抽取；`flowJson` 与 BPMN 双向转换保持不变，新建版本化 `businessProcessJson`。
4. **编排器只协调，节点委托现有能力。** 动作、消息、审批、企业集成和开放能力通过节点执行器注册表接入；不在编排器复制各模块业务逻辑。
5. **持久化运行优先于异步执行。** 先创建 run/node_run 检查点，再异步执行；审批节点进入等待并由回调恢复，服务重启可以扫描恢复。
6. **显式副作用。** 结束节点不做状态回写，审批结果通过标准出口连接到显式动作，保证设计、审计和排障一致。
7. **应用发布是唯一正式发布边界。** 流程可以保存和校验草稿，但正式版本随应用发布生成；运行入口只读取应用发布快照。
8. **一个可写入口，兼容运行分阶段退出。** 旧触发器、绑定和动作数据不物理删除；迁移后停止旧入口新增修改，新运行优先新流程，历史实例继续旧链路。
9. **复杂 JSON 迁移使用 Java 服务。** Flyway 只建表、索引、字典和权限；旧 JSON 的语义转换需要预览、验证、问题单和幂等，禁止在 SQL 中猜测。
10. **默认失败关闭。** 无可信身份、对象/字段失效、审批版本漂移、权限不足、回调不匹配或外部能力不可用时停止当前节点并记录安全错误，不静默跳过或换身份执行。

## 11. 执行日志

| Task | 状态 | 实际改动文件 | 备注 |
|---|---|---|---|
| Research | completed | 本 `spec.md`、现有应用/触发器/动作/Flowable/发布代码 | 已核对当前入口、协议、执行器和迁移边界，未修改生产代码 |
| Proposal | completed | `spec.md`, `tasks.md` | 已形成应用级业务流程编排器提案和任务拆分，未创建测试或执行日志 |
| HARD-GATE | completed | 本 `spec.md`、`tasks.md`、`test-spec.md`、`execution-log.md` | 用户于 2026-08-03 明确要求开始开发，按第 9 章六项推荐默认值进入 `/apply` |
| Apply/M1 | in_progress | 当前变更目录及后续 Task 1-7、14-16 实现文件 | 先完成控制面与画布，不接管正式触发 |
| Apply/M2-发布边界 | completed | Task 12 后端发布、快照、就绪检查与回滚投影 | 已固定不可变流程/对象/Flowable 版本；正式触发与手动动作仍由 Task 10/13 接入 |

## 12. 审查结论

- Proposal 自审：目标、现状、功能、数据、接口、迁移、风险、测试和任务链路已经对应。
- 架构边界：未建设第二套审批引擎；未把应用直接当作业务记录；未直接扩展 BPMN `flowJson` 承载应用自动化。
- 安全边界：状态、权限、身份、外部调用、幂等和旧数据迁移均已标记人工审查门禁。
- 当前结论：第 9 章默认决策已确认，允许按 `tasks.md` 顺序进入 `/apply`；状态、权限、幂等、迁移和真实 Flowable 联调门禁继续保留。

## 13. 确认记录（HARD-GATE）

- **确认时间**：2026-08-03。
- **确认人**：用户（当前会话）。
- **确认范围**：第 9 章首版图能力、审批并发、定时发起人、旧入口停用、子流程和 Webhook 六项推荐默认值全部确认。
- **当前授权**：允许按本 Spec 与 `tasks.md` 进入 `/apply`，修改生产代码、Flyway 脚本、前端页面和样例；不授权自动启动真实服务、执行数据库迁移或改变现有 Flowable 运行态。

## 14. 2026-08-07 产品分层纠偏

上一轮 Task 20 将“当前应用可用审批模型”错误实现为“当前应用对象存在有效 `FLOW Binding` 的审批模型”。这与本变更第 10 条“应用拥有流程，业务对象拥有记录”的技术决策冲突，并造成新应用流程反向依赖旧对象流程入口。该前提作废，后续实现以本节为准。

### 14.1 能力归属

```text
业务对象（定义数据是什么）
├── 基本信息、数据结构与字段映射
├── 关联关系与级联规则
├── 校验、默认值、公式、编号和领域动作/事件契约
└── 可复用表单资产（不绑定某一个应用页面）

应用（定义用户怎样使用数据）
├── 页面与页面模板
├── 列表视图、查询区、详情布局和页面动作
├── 业务流程、自动化触发器和受控动作编排
├── 角色、字段权限和数据范围策略
└── 发布版本、入口和运行治理
```

- “列表设计”保留，但从业务对象普通主导航迁移到应用的页面/视图配置。对象层只提供默认列表预设和字段目录，供应用页面继承。
- “关联关系”继续属于业务对象，因为它决定数据模型和运行时级联语义；应用层只配置关系的导航、展示和可用动作。
- “数据权限”采用两层合同：对象声明 owner/组织/创建人等可授权字段，应用为角色配置可见、可写和数据范围，运行时继续使用平台租户/组织/DataScope，不建设第二套身份体系。
- 对象层不再提供新建应用流程所需的流程模型选择、审批条件、审批后动作或自动化触发器写入口；旧对象 `FLOW Binding` 只作为存量运行兼容和迁移来源。

### 14.2 审批模型目录合同

- Flowable 模型是租户级可复用流程资产。应用业务流程的审批节点直接选择当前租户有权限且已发布、已部署的模型，不要求主对象先存在 `FLOW Binding`。
- 应用与流程模型的隔离在应用发布时通过流程版本/授权快照固定；当前阶段目录不得用对象绑定推导“属于当前应用”。跨应用授权能力另立变更，不以隐藏的对象绑定作为替代。
- 发布校验只验证所选 `modelKey + version + processDefinitionId + deploymentId` 仍然有效；失效时报告“审批模型已失效，请重新选择”，不报告“节点引用未发布、不属于当前应用”这类无法行动的复合错误。

### 14.3 画布兼容合同

- 业务流程画布不再复用 BPMN 递归布局的网关尺寸和访问顺序；使用独立的 DAG 分层布局，所有业务节点保持卡片尺寸，分支出口按端口顺序生成独立路由，汇合节点按入边分配顶部锚点。
- 读取历史草稿时归一化缺失/重复出口、旧网关标记和悬空边；无法安全推断的边保留为发布阻断问题，不静默改写用户流程。
- 画布测试必须覆盖不同分支走不同下游、多个分支汇合、同一后继的多个结果出口、删除/插入节点后的重排和旧草稿读入，不能只用“所有分支共享同一后继”的构造图证明稳定。
