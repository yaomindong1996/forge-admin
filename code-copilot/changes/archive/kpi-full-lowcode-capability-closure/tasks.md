# 任务拆分 — 绩效场景全低代码能力闭环补齐
> status: propose
> created: 2026-08-03
> 拆分顺序：协议与权限 -> 节点保存 -> 集合校验 -> 节点动作 -> 批量生成 -> 消息 -> 验收
> 每个任务为可独立审查的原子变更；开始 `/apply` 时再根据工作区最新代码校准文件列表和 Flyway 版本。

## 前置条件

- [ ] 用户逐项确认 `spec.md` 第 9 节待澄清问题。
- [ ] 用户完成 `spec.md` 第 13 节 HARD-GATE，明确授权 `/apply kpi-full-lowcode-capability-closure`。
- [ ] 冻结绩效周期、直属上级、主岗位、评分公式、申诉调分和幂等业务键口径。
- [ ] 冻结在职员工权威来源：已发布低代码员工对象、系统目录适配器或客户 HR 对象三选一；禁止默认直查 `sys_user` 原表或沿用不存在的 `sys_user_position` 名称。
- [ ] 确认一期仅使用站内消息，企微消息/待办不进入本变更。
- [ ] HARD-GATE 明确本次 `/apply` 授权全部 P0/P1，或仅授权 P0；未明确授权的 P1 不得实施。
- [ ] `/apply` 前读取最新 `AGENTS.md`、本变更四份文档、memory 和编码规范；每个 Phase 验证前读取自动化测试标准并增量更新 `test-spec.md/execution-log.md`。
- [ ] 所有新增配置协议必须兼容历史 BPMN、历史触发器和历史低代码对象。

## Task 0：冻结验证基线与文档证据（P0）

- **目标**：在任何生产代码改动前维护可复用测试矩阵和执行证据，确保四个 Phase 均可独立验收。
- **涉及文件**：
  - `code-copilot/changes/kpi-full-lowcode-capability-closure/test-spec.md` — 按确认后的业务口径增量冻结 P0/P1 测试矩阵。
  - `code-copilot/changes/kpi-full-lowcode-capability-closure/execution-log.md` — 逐次追加命令、结果、警告、跳过项和服务清理。
  - `code-copilot/changes/kpi-full-lowcode-capability-closure/spec.md` — 只回填确认、风险、执行和审查状态。
  - `code-copilot/changes/kpi-full-lowcode-capability-closure/tasks.md` — 只更新当前 Task 状态和实际文件。
- **验收**：Proposal 文档检查已有证据；每个 Phase 开始前明确增量范围，结束后留下可复跑命令和结果。

## Task 1：扩展流程字段目录与子表权限协议（P0）

- **目标**：让真实流程设计器可以按关系和字段配置子表可见、可写、必填及行操作权限，并能无损写入/读取 BPMN。
- **涉及文件**：
  - `forge-admin-ui/src/views/flow/utils/form-field-catalog.js` — 扩展主表/子表字段目录标准化，输出稳定 `relationCode/fieldCode`。
  - `forge-admin-ui/src/components/flow-designer/panel/FormPermissionConfig.vue` — 分组展示子表权限和 `allowAdd/allowDelete`。
  - `forge-admin-ui/src/components/flow-designer/converter/user-task-writer.js` — 只写入子表权限扩展属性。
  - `forge-admin-ui/src/components/flow-designer/converter/user-task-parser.js` — 兼容解析新旧权限协议。
  - `forge-admin-ui/src/components/flow-designer/panel/__tests__/FormPermissionConfig.spec.js` — 覆盖权限编辑与历史协议回显。
- **关键协议**：
  ```json
  {
    "relationCode": "task_details",
    "field": "self_score",
    "readable": true,
    "writable": true,
    "requiredOnComplete": true,
    "allowAdd": false,
    "allowDelete": false
  }
  ```
- **验收**：BPMN JSON/XML round-trip 后权限不丢失；旧 BPMN 子表默认只读，主表权限行为不变。

## Task 2：后端任务表单上下文输出受控子表权限（P0）

- **目标**：后端基于当前任务节点生成主子表运行权限，不信任前端自行推断。
- **涉及文件**：
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/vo/businessapp/BusinessTaskFormContextVO.java` — 增加子表权限和校验阶段字段。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessFlowService.java` — 解析、归一和校验节点子表权限。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/businessapp/BusinessTaskFormContextQueryDTO.java` — 保持任务身份参数明确，禁止客户端指定权限。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/test/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessFlowTaskChildPermissionTest.java` — 新增权限归一、旧协议和越权测试。
- **关键签名**：
  ```java
  private List<Map<String, Object>> resolveTaskChildPermissions(
          TaskFormRuntimeContext runtime,
          JSONObject nodeForm);
  ```
- **验收**：同一任务在自评/上级节点返回不同子表写权限；已办/历史上下文全部只读。

## Task 3：任务表单主子表草稿和办理保存（P0）

- **目标**：`PUT task-form-context` 只受控保存主子表草稿，办理数据统一交给 Task 8B 的唯一命令。
- **涉及文件**：
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/businessapp/BusinessTaskFormSaveDTO.java` — 增加受控 `children` payload，协议固定为草稿保存。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessFlowService.java` — 过滤主子表字段、校验归属并编排草稿保存。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/DynamicCrudService.java` — 提供节点权限感知的主子表 merge 内部入口，忽略并审计客户端公式字段，触发公式刷新。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/test/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessFlowTaskChildSaveTest.java` — 新增草稿、越权字段、公式字段和跨主记录行测试。
- **关键签名**：
  ```java
  public BusinessTaskFormContextVO saveTaskFormContext(BusinessTaskFormSaveDTO dto);

  public Map<String, Object> mergeTaskFormDataById(
          String configKey,
          Long recordId,
          Map<String, Object> mainData,
          Map<String, Object> children,
          TaskFormWritePolicy policy);
  ```
- **验收**：草稿可保留空评分；PUT 不能以任何参数触发办理；只读字段、公式字段和其它任务明细不能被修改。

## Task 4：待办页按节点权限编辑子表与保存草稿（P0）

- **目标**：去掉待办子表全局只读限制，让本阶段草稿只走 PUT，并保持布局稳定、已办只读；POST 唯一办理命令在 Phase 2 的 Task 7B/8B 闭环。
- **涉及文件**：
  - `forge-admin-ui/src/views/flow/todo.vue` — 向子表编辑器传递节点权限并组装草稿请求；不在本 Task 改造办理协议。
  - `forge-admin-ui/src/components/page-templates/ChildTableEditor.vue` — 支持字段级只读、必填和行操作权限。
  - `forge-admin-ui/src/api/flow.js` — 扩展任务表单保存 payload，不改变现有接口路径。
  - `forge-admin-ui/src/views/flow/utils/__tests__/form-field-catalog.spec.js` — 覆盖子表字段目录和权限映射。
  - `forge-admin-ui/src/components/page-templates/__tests__/ChildTableEditorTaskPermission.spec.js` — 新增节点权限交互测试。
- **验收**：自评节点只能编辑自评分，上级节点只能编辑上级评分；保存草稿只发一次 PUT；固定格式表格在桌面和窄屏不发生字段/按钮重叠。办理单次 POST 的验收归 Task 7B/8B。

## Task 5A：服务端条件必填协议与执行器（P0）

- **目标**：让低代码条件必填在服务端成为可信规则，并支持按节点结果校验申诉/调分补充字段。
- **涉及文件**：
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/domain/validation/ConditionalValidationRule.java` — 新增字段、公式字段和节点结果条件协议。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/validation/ConditionalValidationService.java` — 新增白名单条件比较和 required 执行器。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/DynamicCrudService.java` — 普通保存接入允许的服务端条件规则。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessFlowService.java` — 基于完整候选状态，在公式重算后、任何持久化和任务完成前按节点结果执行条件必填。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/test/java/com/mdframe/forge/plugin/generator/service/validation/ConditionalValidationServiceTest.java` — 覆盖分差评语、申诉原因、调分、非法来源和绕过前端提交。
- **验收**：路由/URL/客户端用户/表达式条件不能进入服务端规则；公式分差超阈值缺评语被拒绝；确认结果不误要求申诉字段。

## Task 5B：声明式集合校验后端协议与执行器（P0）

- **目标**：提供安全、可审计的跨行规则，严格按普通保存 `SAVE`、节点办理 `COMPLETE` 的阶段矩阵执行。
- **涉及文件**：
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/domain/validation/CollectionValidationRule.java` — 新增白名单规则模型。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/validation/CollectionValidationService.java` — 新增 `ROW_COUNT/SUM/UNIQUE/ALL_REQUIRED` 执行器。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/DynamicCrudService.java` — 普通主子表保存接入 SAVE 规则。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessFlowService.java` — 在内存合并完整主子表候选、重算公式后接入 COMPLETE 规则；校验成功后再原子持久化和刷新聚合。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/test/java/com/mdframe/forge/plugin/generator/service/validation/CollectionValidationServiceTest.java` — 覆盖精度、空集合、唯一性和阶段规则。
- **关键签名**：
  ```java
  public void validate(
          List<CollectionValidationRule> rules,
          ValidationPhase phase,
          Map<String, Object> mainData,
          Map<String, List<Map<String, Object>>> children);
  ```
- **验收**：权重 `99.99/100.00/100.01` 判定准确；后端拒绝绕过前端提交的非法集合。

## Task 6A：集合校验可视化配置与即时预览（P1）

- **目标**：实施人员不写 JSON 即可配置权重合计、明细必填和明细唯一规则，并对当前设计样例进行非权威即时预览。
- **涉及文件**：
  - `forge-admin-ui/src/views/app-center/components/designer/BusinessRelationDesigner.vue` — 增加关系集合规则入口。
  - `forge-admin-ui/src/views/app-center/components/designer/CollectionValidationRuleDesigner.vue` — 新增结构化规则编辑器。
  - `forge-admin-ui/src/views/app-center/components/designer/__tests__/CollectionValidationRuleDesigner.spec.js` — 新增配置交互测试。
- **验收**：通过下拉选择即可生成 `SUM(weight)=100`、`ROW_COUNT>=1` 和 `UNIQUE(indicator_id)`；即时预览明确标注为设计辅助，不替代后端保存/办理校验。

## Task 6B：集合/条件规则归一化与发布检查（P0）

- **目标**：服务端保存结构化规则并在对象/应用发布前做纯静态协议检查，不扫描运行态业务数据。
- **涉及文件**：
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessObjectDesignerService.java` — 保存时归一规则并做字段、关系、阶段和操作符白名单校验。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessApplicationReadinessService.java` — 静态检查损坏规则、条件必填的不可信来源、缺失字段、公式精度/舍入和公式字段误配置为可写。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/test/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessValidationReadinessTest.java` — 覆盖合法协议、未知阶段、损坏引用、公式字段可写和零业务数据扫描。
- **验收**：删除/改名字段、未知阶段/操作符、公式精度缺失或公式字段可写时发布检查明确阻断；检查不读取业务记录，不把运行态 `SAVE/COMPLETE` 校验当发布校验执行。

## Task 7A：流程节点业务化结果文案与动作配置（P0）

- **目标**：在真实流程设计器中把 approve/reject 配置为“提交自评/发起申诉”等业务结果，并绑定唯一负责状态及副作用的现有对象动作。
- **涉及文件**：
  - `forge-admin-ui/src/components/flow-designer/panel/PermissionConfig.vue` — 配置主/次结果文案、动作、受控流程变量及补充/必填字段；失败策略首期固定为补偿。
  - `forge-admin-ui/src/components/flow-designer/constants/default-configs.js` — 增加兼容默认值。
  - `forge-admin-ui/src/components/flow-designer/converter/user-task-writer.js` — 写入节点结果配置。
  - `forge-admin-ui/src/components/flow-designer/converter/user-task-parser.js` — 解析并回显节点结果配置。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessApplicationReadinessService.java` — 阻断节点结果动作与绑定级 `APPROVED/REJECTED` callbackActions 共存，允许独立 `CANCELED` 回调。
  - `forge-admin-ui/src/components/flow-designer/converter/__tests__/user-task-parser-permissions.spec.js` — 增加 round-trip 测试。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/test/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessFlowOutcomeActionReadinessTest.java` — 覆盖新旧动作互斥及 CANCELED 兼容。
- **关键协议**：
  ```json
  {
    "approveLabel": "提交自评",
    "approveActionCode": "kpi_submit_self",
    "approveVariables": {"outcome": "self_submitted"},
    "rejectLabel": "发起申诉",
    "rejectActionCode": "kpi_create_appeal",
    "rejectVariables": {"outcome": "appeal_requested"},
    "rejectFormFields": ["appeal_type", "appeal_reason", "appeal_evidence"],
    "rejectRequiredFields": ["appeal_type", "appeal_reason"],
    "failureStrategy": "COMPENSATE"
  }
  ```
- **验收**：未配置仍显示同意/驳回；配置后协议 round-trip 不丢失业务文案、动作和结果字段；未知/只读字段不能发布；节点结果动作与旧 APPROVED/REJECTED 回调共存时发布失败，CANCELED 回调仍可用。

## Task 7B：待办结果补充字段弹窗（P0）

- **目标**：业务结果可以在办理前收集该结果专属字段，不要求把申诉/调分输入项长期铺在主表单上。
- **涉及文件**：
  - `forge-admin-ui/src/views/flow/todo.vue` — 根据结果配置打开补充字段弹窗，并把字段合入唯一 task-action 请求。
  - `forge-admin-ui/src/views/flow/components/BusinessTaskOutcomeDialog.vue` — 新增复用 AiForm 的结果字段弹窗。
  - `forge-admin-ui/src/api/flow.js` — 保持单次 POST，传递结果字段、完整表单、幂等键和请求摘要。
  - `forge-admin-ui/src/views/flow/components/__tests__/BusinessTaskOutcomeDialog.spec.js` — 覆盖申诉、确认、HR 调分、取消和重复提交。
- **验收**：确认结果不弹无关字段；申诉和确认调分分别要求正确字段；取消零写入；点击办理只发送一次 POST，不先保存 PUT。

## Task 8A：办理命令状态与节点动作补偿模型（P0）

- **目标**：复用 `sys_flow_task` 建立可恢复的唯一办理命令状态，并建立可查询、可 CAS 重试的流程节点动作补偿台账；二者均不替代现有动作日志的幂等权威。
- **涉及文件**：
  - `forge-server/db/migration/V<next>__add_lowcode_flow_task_action_state_and_compensation.sql` — 扩展 `sys_flow_task` 命令阶段/最小恢复上下文，新建补偿表、索引、状态字典和查询/重试权限资源。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/com/mdframe/forge/starter/flow/entity/FlowTask.java` — 复用既有幂等键/摘要/动作类型并增加阶段、固定动作引用、最小上下文和稳定结果摘要。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/resources/mapper/FlowTaskMapper.xml` — 增加按期望阶段 CAS 预留/推进命令和恢复查询 SQL。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/domain/entity/AiBusinessFlowActionCompensation.java` — 新增补偿实体。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/mapper/BusinessFlowActionCompensationMapper.java` — 新增查询和 CAS Mapper。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/BusinessFlowActionCompensationMapper.xml` — 显式租户、状态、分页和 CAS SQL。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessFlowActionCompensationService.java` — 固定动作发布版本、流程/业务定位、规范化变量、原幂等键/摘要和 `action_log_id`，并编排分页及补偿状态。
- **验收**：命令阶段只能按 `RESERVED -> FORM_SAVED -> FLOW_COMPLETED -> ACTION_SUCCEEDED/COMPENSATION_PENDING` CAS 前进；相同 `tenant/task/outcome/action` 只能有一条逻辑补偿记录；补偿固定原动作发布版本且不保存完整表单/评分正文；跨租户查询/重试拒绝。

## Task 8B：唯一办理命令、节点动作执行和受控重试（P0）

- **目标**：POST 作为唯一办理命令保存 COMPLETE 数据、完成任务并执行已发布对象动作；失败可观察、可重试且不重复完成任务。
- **涉及文件**：
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/businessapp/BusinessTaskActionDTO.java` — 携带节点结果、完整主子表 COMPLETE 数据、幂等键和请求摘要。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessFlowService.java` — 编排命令预留、完整候选状态/公式/校验、本地原子保存、Flowable 历史核对、任务完成、动作和补偿状态。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessActionExecutionService.java` — 提供受控 FAILED 日志 CAS 基础能力，强制原幂等键、原请求摘要和固定动作版本。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessActionRetryService.java` — 新增流程补偿和批量失败项共用的可信重试入口，统一 CAS、权限主体、尝试审计和返回语义。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/controller/BusinessFlowController.java` — 保持 task-action 原路径，并增加补偿分页/详情/重试接口和权限。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/test/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessFlowNodeActionTest.java` — 新增重复回调、动作失败和重试测试。
- **关键签名**：
  ```java
  private BusinessActionExecuteResultVO executeNodeOutcomeAction(
          TaskFormRuntimeContext runtime,
          String taskId,
          String outcome,
          String actionCode);
  ```
- **验收**：校验失败零业务写入；业务数据已保存但 Flowable 失败时从 `FORM_SAVED` 恢复；Flowable 成功但响应丢失时通过任务/历史核对收敛；最终响应丢失时相同键/摘要返回稳定结果；相同键不同摘要、不同键抢占和任务并发完成均有确定冲突语义。受控重试复用原失败日志且只产生一次逻辑业务副作用，绝不再次完成 Flowable 任务。

## Task 8C：节点动作补偿自动扫描（P1）

- **目标**：复用 Forge 任务调度自动重试到期补偿项，达到上限后转人工处理。
- **涉及文件**：
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessFlowActionCompensationSchedulerService.java` — 使用 `@ScheduledJob`、租户上下文、集群锁和分页扫描执行 CAS 重试。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessFlowActionCompensationService.java` — 统一自动/人工重试、退避、最大次数和人工处理状态，并委托通用 `BusinessActionRetryService`。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/test/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessFlowActionCompensationSchedulerServiceTest.java` — 覆盖集群锁、退避、并发人工重试、成功收敛和超限停止。
- **验收**：自动与人工并发只有一方 CAS 成功；失败按配置退避；超过最大次数不再自动重放且可由有权限管理员查看。

## Task 8D：节点动作补偿运维页面（P1）

- **目标**：有权限管理员可以查看补偿列表/详情并手工重试，且页面不泄露表单和评分正文。
- **涉及文件**：
  - `forge-admin-ui/src/api/business-app.js` — 增加补偿分页、详情和手工重试 API。
  - `forge-admin-ui/src/views/app-center/flow-action-compensation.vue` — 新增紧凑列表、状态筛选、详情和重试入口。
  - `forge-admin-ui/src/views/app-center/components/FlowActionCompensationDetail.vue` — 展示固定动作版本、流程/业务定位、尝试历史和脱敏错误。
  - `forge-admin-ui/src/views/app-center/components/__tests__/FlowActionCompensationDetail.spec.js` — 覆盖权限、长 ID、状态、并发重试和敏感字段隐藏。
- **验收**：列表/详情使用独立权限；只有可重试状态显示重试命令；重复点击以服务端 CAS 结果为准；页面不展示完整表单、评分明细、消息正文或原始异常堆栈。

## Task 9A：权威员工目录来源接入（条件 P0）

- **目标**：把 HARD-GATE 选择的在职员工、主岗位、主组织和直属上级来源冻结为批量查询可消费的已发布契约，不让业务动作直接查询系统原始表。
- **实施分支**：
  - 选择已发布低代码员工目录对象或客户 HR 对象：只校验对象发布状态、字段契约、数据权限和确定性排序，不新增系统表适配代码。
  - 选择系统目录适配器：在公共 SPI 中定义只读目录查询协议，由 system 插件提供 `sys_user + sys_user_post.is_main + 当前组织关系` 的白名单投影，generator 只依赖 SPI，不直接跨插件查询表。
- **涉及文件**：在 HARD-GATE 选择后按模块依赖校准；系统适配分支至少包含公共目录 SPI、system 实现、generator 查询桥接及租户/组织/权限测试。
- **验收**：候选员工只来自冻结来源；停用用户、非主岗位、无当前组织或无直属上级按确认口径处理；字段/排序协议稳定；任何分支都不能通过 `QUERY_RECORDS` 任意访问 `sys_user/sys_user_post` 原表。

## Task 9B：批量触发运行模型与数据库迁移（P0）

- **目标**：建立批量运行汇总和逐次尝试审计模型；并发幂等继续由现有业务动作日志负责。
- **涉及文件**：
  - `forge-server/db/migration/V<next>__add_lowcode_batch_trigger_runtime.sql` — 新建运行表、扩展日志、索引、字典和权限资源。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/domain/entity/AiBusinessTriggerRun.java` — 新增运行汇总实体并固定触发器版本/摘要、period JSON、绑定用户/组织、源查询摘要和动作发布版本。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/domain/entity/AiBusinessTriggerLog.java` — 增加运行、源记录、逻辑幂等键、`actionLogId`、尝试序号和可重试字段。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/mapper/BusinessTriggerRunMapper.java` — 新增运行 Mapper。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/BusinessTriggerRunMapper.xml` — 运行汇总和失败项查询 SQL。
- **验收**：迁移具备防重复保护、`tenant_id=1`、显式列名；触发日志幂等索引不是 UNIQUE，同一逻辑项的失败重试可保留多个 attempt 并关联同一动作日志；run 能独立恢复原配置/周期/身份/源查询/动作版本；运行和失败项按租户隔离。

## Task 10A：受控关联对象与子表查询步骤（P0）

- **目标**：让业务动作按当前源记录字段查询已发布模板及明细集合，为 `FOREACH/CREATE_RECORD` 提供命名输出，不开放 SQL 或脚本。
- **涉及文件**：
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/QueryRecordsActionStepExecutor.java` — 新增 `QUERY_RECORDS` 白名单只读步骤和命名输出。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessActionExecutionContext.java` — 承载后续步骤可读取的受控查询输出。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/DynamicCrudService.java` — 提供显式 `DataScopeContext`、字段/操作符白名单、分页上限和已声明子表加载的内部查询入口。
  - `forge-admin-ui/src/views/app-center/components/designer/BusinessActionDesigner.vue` — 通过对象、字段、操作符、上下文值、排序、上限和输出名配置查询步骤。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/test/java/com/mdframe/forge/plugin/generator/service/businessapp/QueryRecordsActionStepExecutorTest.java` — 覆盖岗位查模板、模板明细、越权、非法字段、超上限和命名输出。
- **关键协议**：
  ```json
  {
    "stepType": "QUERY_RECORDS",
    "targetObjectCode": "KPI_TEMPLATE",
    "filters": [
      {"field": "post_id", "operator": "EQ", "valueFrom": "record.post_id"},
      {"field": "status", "operator": "EQ", "value": 1}
    ],
    "includeChildren": ["template_details"],
    "limit": 1,
    "orderBy": [{"field": "update_time", "direction": "DESC"}, {"field": "id", "direction": "ASC"}],
    "expect": "EXACTLY_ONE",
    "output": "matchedTemplates"
  }
  ```
- **验收**：只查询已发布对象和允许字段；使用绑定执行身份的数据权限；模板必须启用、排序确定且恰好匹配一条，零条或多条均返回明确业务错误；模板或明细不可见时不旁路返回；输出可被后续 FOREACH 读取。

## Task 10B：创建记录子表快照映射（P0）

- **目标**：先冻结 `CREATE_RECORD + FOREACH` 的目标主表及子表快照协议，供批量服务稳定调用。
- **涉及文件**：
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/CreateRecordActionStepExecutor.java` — 支持结构化 `children` 映射。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessActionStepConfigHelper.java` — 解析父上下文、循环项和嵌套子表数据。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/DynamicCrudService.java` — 保证目标主子表同一单条事务写入并复用唯一索引校验。
  - `forge-admin-ui/src/views/app-center/components/TriggerActionConfigPanel.vue` — 结构化配置目标主表、子表字段映射和目标唯一键。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/test/java/com/mdframe/forge/plugin/generator/service/businessapp/CreateRecordWithChildrenActionTest.java` — 覆盖快照、回滚、非法字段和目标唯一键。
- **验收**：修改源模板后，已生成目标任务和明细不变化；主表创建失败或任一子表写入失败时单条源记录整体回滚；未配置并发布目标复合唯一索引时批量触发器不能启用。

## Task 11：批量源查询、预览和执行服务（P0）

- **目标**：让 SCHEDULE 触发器按已发布源对象分页查询候选记录并逐条调用现有动作。
- **涉及文件**：
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessTriggerBatchService.java` — 新增预览、运行、结构化周期上下文、分页、可信执行身份和单条隔离事务编排。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessTriggerSchedulerService.java` — 路由 `DUE_FIELD/BATCH_QUERY` 两种模式。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessTriggerService.java` — 保存运行/尝试日志；失败重试读取原 run 上下文并委托通用 `BusinessActionRetryService`。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/DynamicCrudService.java` — 使用显式 `DataScopeContext` 提供受数据权限和字段白名单保护的批量源查询内部入口。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/test/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessTriggerBatchServiceTest.java` — 覆盖稳定分页、周期、身份、幂等、部分失败和租户隔离。
- **关键签名**：
  ```java
  public BusinessTriggerPreviewVO preview(Long triggerId, Integer sampleSize);

  public Long run(Long triggerId, TriggerRunType runType, Long retryRunId);
  ```
- **验收**：月/季/年及时区/偏移生成稳定 `period` 上下文；一次 run 固定触发器配置、period、绑定用户/组织 ID、源查询和动作发布版本；触发器编辑后失败重试仍使用原 run 上下文，但实时复核固定身份的账号状态和当前权限。身份失效或撤权时失败关闭；一条失败不回滚其它记录；触发日志通过 `actionLogId` 引用原动作日志；动作日志原子占位和目标唯一索引共同阻止重复目标记录。

## Task 12：批量触发器管理与运维页面（P1）

- **目标**：实施人员可以配置、预览、执行和重试批量触发器，不接触 JSON 或数据库。
- **涉及文件**：
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/controller/BusinessTriggerController.java` — 增加 preview/run/runs/retry-failed 接口。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/vo/businessapp/BusinessTriggerPreviewVO.java` — 新增脱敏预览 VO。
  - `forge-admin-ui/src/api/business-app.js` — 增加批量触发器 API。
  - `forge-admin-ui/src/views/app-center/trigger.vue` — 增加批量模式配置、预览和运行记录入口。
  - `forge-admin-ui/src/views/app-center/components/TriggerActionConfigPanel.vue` — 增加源查询、周期/时区/偏移、绑定执行用户/当前组织、幂等、目标唯一键和失败策略配置。
- **验收**：预览不写数据；运行详情和失败项按 `pageNum/pageSize` 分页；失败项可单独重试；长 ID 保持字符串；错误信息不泄露敏感记录全文。

## Task 13：消息字段接收人规则（P1）

- **目标**：站内消息可以从当前记录中已声明为用户引用/消息接收人的合法字段解析接收人。
- **涉及文件**：
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessMessageReceiverResolver.java` — 新增统一接收人解析器，消除两个 Executor 的重复逻辑。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessTriggerExecutor.java` — 复用统一解析器。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/SendMessageActionStepExecutor.java` — 复用统一解析器。
  - `forge-admin-ui/src/views/app-center/components/TriggerActionConfigPanel.vue` — 从字段目录选择接收人字段和失败策略。
  - `forge-admin-ui/src/views/app-center/components/designer/BusinessActionDesigner.vue` — 为业务动作的 `SEND_MESSAGE` 步骤提供同一字段接收人选择器和失败策略，替代高级 JSON 主路径。
  - `forge-admin-ui/src/views/app-center/components/designer/__tests__/BusinessMessageActionDesigner.spec.js` — 覆盖触发器/业务动作两类配置入口、字段筛选和协议回显。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/test/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessMessageReceiverResolverTest.java` — 覆盖 FIELD、多值、空值和非法字段。
- **关键协议**：
  ```text
  FIELD:evaluatee_id
  FIELD:evaluator_id
  ```
- **验收**：普通数字字段即使值恰好等于用户 ID 也不可选；接收人为空默认失败关闭；现有 OWNER/CREATOR/USERS/ROLES/DEPTS/ALL 行为不变。

## Task 14A：验收 fixture 契约与测试加载器（P1）

- **目标**：先冻结可复跑的 fixture 消费契约；不把普通 JSON 文件误称为平台已支持的完整应用配置导入包。
- **涉及文件**：
  - `code-copilot/changes/kpi-full-lowcode-capability-closure/fixtures/kpi-lowcode-fixture.schema.json` — 定义 `schemaVersion/fixtureId/namespace`、资源依赖、稳定编码和清理元数据。
  - `code-copilot/changes/kpi-full-lowcode-capability-closure/fixtures/kpi-lowcode-fixture-loader.mjs` — 提供 `load/cleanup` 命令，按依赖顺序调用现有受保护管理 API。
  - `code-copilot/changes/kpi-full-lowcode-capability-closure/fixtures/README.md` — 冻结命令、所需身份、测试环境门禁、UPSERT/冲突和清理行为。
- **关键命令**：
  ```bash
  node code-copilot/changes/kpi-full-lowcode-capability-closure/fixtures/kpi-lowcode-fixture-loader.mjs load --fixture <path> --namespace kpi_acceptance
  node code-copilot/changes/kpi-full-lowcode-capability-closure/fixtures/kpi-lowcode-fixture-loader.mjs cleanup --fixture-id kpi_lowcode_v1 --namespace kpi_acceptance
  ```
- **验收**：未知 Schema 版本、非测试环境、非 fixture 同编码资源冲突时失败关闭；相同 `fixtureId + namespace` 重复加载受控 UPSERT；清理预览和结果只包含该命名空间资源。若 HARD-GATE 不授权测试加载器，则 Task 14B 改为人工配置验收且不得声称 fixture 可直接导入。

## Task 14B：绩效纯低代码验收配置（P0/P1）

- **目标**：形成版本化、可由已冻结加载契约消费或人工重建的绩效验收样板，不引入 KPI 专用生产代码。
- **涉及文件**：
  - `code-copilot/changes/kpi-full-lowcode-capability-closure/kpi-lowcode-acceptance.md` — 新增对象、关系、公式、权限、流程、动作、触发器和消息配置说明。
  - `code-copilot/changes/kpi-full-lowcode-capability-closure/fixtures/kpi-lowcode-acceptance.json` — 新增符合 Task 14A Schema 的脱敏低代码配置 fixture；加载器未实施时仅作为声明式配置清单。
  - `code-copilot/changes/kpi-full-lowcode-capability-closure/fixtures/kpi-lowcode-test-data.sql` — 新增仅含合成人员/评分数据的准备与清理脚本。
- **验收**：在 Task 14A 契约已实现时，空测试环境可按明确命令重建样板；重复加载不产生重复配置；清理只作用于测试命名空间。未实现加载器时仅交付配置说明，不得把 fixture 标记为可导入。

## Task 14C：绩效纯低代码 E2E 与最终回归（P0/P1）

- **目标**：证明不写 KPI 专用代码即可完成核心绩效流程，并收敛最终验证证据。
- **涉及文件**：
  - `forge-admin-ui/tests/e2e/kpi-lowcode-closure.spec.js` — 新增核心流程 E2E；具体目录在 `/apply` 前按现有 E2E 基线校准。
  - `code-copilot/changes/kpi-full-lowcode-capability-closure/test-spec.md` — 增量回填实际测试范围、命令和跳过项。
  - `code-copilot/changes/kpi-full-lowcode-capability-closure/execution-log.md` — 追加最终构建、接口、数据库、浏览器和服务清理证据。
- **验收**：人工/定时生成、自评草稿/提交、上级评分、确认、申诉、HR 处理、消息、越权和重复执行全部通过；报表不在验收范围。

## 执行顺序与门禁

| 阶段 | Tasks | 完成门禁 |
|------|-------|----------|
| Phase 0：验证基线 | 0 | 四份 SDD 文档完整，增量验证矩阵和 Proposal 证据可复用 |
| Phase 1：节点主子表 | 1-4 | 子表权限、草稿和历史只读全部通过；办理能力在 Phase 2 闭环 |
| Phase 2：规则与动作 | 5A-8D（含 6A/6B） | 条件/集合校验、公式、唯一办理命令、结果补充字段、节点动作及自动/人工补偿通过 |
| Phase 3：批量生成 | 9A-12（含 9B/10A/10B） | 权威员工来源、关联模板查询、预览、分页、主子表快照、幂等和失败重试通过 |
| Phase 4：消息与验收 | 13-14C | 动态收件人、可重复样板和绩效纯低代码 E2E 通过 |

每个 Phase 完成后先更新 `spec.md/tasks.md/execution-log.md` 并执行增量验证，再进入下一阶段。HARD-GATE 必须明确授权“全部 P0/P1”或“仅 P0”；仅确认 P0 时，P1 Tasks 保持 pending，不能随 P0 实现自动带入。任何实现发现与 Spec 冲突时，先修订并重新确认 Spec，不允许直接扩大代码范围。

各 Phase 的 P0 Task 构成“核心运行闭环”里程碑，P1 Task 构成对应的可视化配置、自动运维或可重复验收增强里程碑。若只授权 P0，阶段日志必须写明未交付的 P1 能力及其人工/API 替代方式，最终结论不得表述为“全部低代码体验已完成”。
