# 绩效场景全低代码能力闭环补齐
> status: propose
> created: 2026-08-03
> complexity: 🔴复杂
> source: `code-copilot/changes/20260728需求/绩效打分系统_详细设计文档.md`
> research baseline: branch `codex/capability-guide-version-sync`, commit `bc96278b`（进入 `/apply` 前必须按最新工作区复核）

## 1. 背景与目标

客户希望不为绩效打分系统编写 KPI 专用 Java、Vue 或外部脚本，而是直接复用 Forge 低代码业务对象、主子表、Flowable、业务动作、公式、数据权限、任务中心和消息中心完成搭建。

当前平台已经具备普通 CRUD、主子表、字段公式、聚合公式、业务动作事务、`FOREACH`、`CREATE_RECORD`、流程绑定、角色数据权限和站内消息等底座。本变更不重复建设这些能力，而是补齐它们之间尚未闭合的运行链路，使实施人员只通过低代码配置即可完成以下绩效流程：

`周期任务生成 -> 员工自评 -> 上级评分 -> 员工确认/申诉 -> HR 处理 -> 关闭`

### 1.1 可验证目标

- [ ] 流程节点能够按主表字段、子表字段和子表行操作分别配置可见、可写、必填权限。
- [ ] 自评节点只能修改现有评分明细的自评分字段，上级评分节点只能修改上级评分字段，均不能越权新增、删除或修改指标快照字段。
- [ ] 节点表单支持保存草稿；仅在办理节点时执行“全部明细已填写”等完成态校验。
- [ ] 低代码主子表支持声明式集合校验，例如 `SUM(明细.weight) = 100`，前后端均给出明确反馈，后端为最终可信边界。
- [ ] 表单条件必填规则和节点结果专属补充字段由服务端执行，支持“分差超阈值必须填评语”“申诉必须填类型和原因”“确认调分必须填调整分数”。
- [ ] 节点“通过/驳回”可以配置业务化按钮文案和幂等业务动作，由动作唯一负责业务状态及副作用，不再向绩效用户暴露通用审批术语。
- [ ] 定时触发器能够按 Cron/日历周期查询一个源对象的候选记录，逐条执行现有业务动作，并生成目标主表和子表快照。
- [ ] 业务动作能够通过白名单只读查询步骤加载关联对象及子表集合，例如按员工岗位查找已启用模板及模板明细，供 `FOREACH/CREATE_RECORD` 消费。
- [ ] 批量生成具备预览、手工执行、稳定幂等键、失败明细、失败重试和运行汇总。
- [ ] 消息动作支持从当前业务记录字段动态解析接收人，例如 `FIELD:evaluatee_id` 和 `FIELD:evaluator_id`。
- [ ] 使用上述通用能力可以配置出绩效样板，不新增 KPI 专用后端 Service、Controller 或前端业务页面。

### 1.2 非目标

- 不建设绩效报表、数据透视、动态指标列导出或 BI 能力。
- 不重做低代码数据权限引擎；继续复用 `FOLLOW_SYSTEM`、本人、本组织、本组织及下级组织、自定义组织和全部数据权限。
- 不重做消息中心、消息模板和站内信投递；本期只补业务字段接收人规则。
- 不接通企业微信、飞书、钉钉或企微待办；外部企业协同通道继续由 `unified-enterprise-collaboration` 变更负责。
- 不新增任意 JavaScript/Groovy/SpEL 脚本执行入口；校验、查询和动作只能使用白名单协议。
- 不在本变更中创建正式 KPI 业务表或内置客户数据；绩效对象仅作为验收配置样板。
- 不建设一个独立于真实流程设计器的节点配置页面。

### 1.3 能力满足度结论

| 能力 | 当前判断 | 本变更处理 |
|------|----------|------------|
| 指标、模板、任务、申诉等普通对象 CRUD | 已满足 | 直接使用低代码对象、主子表、字典、文件和自动编号 |
| 员工本人、考评人、HR 数据范围 | 已满足（需拆运行入口） | 复用 `FOLLOW_SYSTEM`，分别映射 `evaluatee_id/evaluator_id`，不重做数据权限 |
| 站内消息模板、发送、记录 | 已满足 | 继续复用消息中心 |
| 按任务记录字段选择消息接收人 | 部分满足 | 补 `FIELD:<fieldCode>`，不重做消息模块 |
| Flowable 流程绑定和普通主表字段权限 | 已满足 | 继续复用现有流程设计器和绑定链路 |
| 流程节点子表编辑、行操作、草稿/办理校验 | 不满足 | 补节点子表权限、受控保存和唯一办理命令 |
| 权重合计、条件必填、结果专属输入 | 不满足 | 补服务端声明式集合/条件校验和结果补充字段 |
| 节点业务文案、业务动作和失败补偿 | 部分满足 | 复用 Business Action，补节点结果绑定与补偿闭环 |
| 周期批量查询员工、查模板明细、生成任务快照 | 不满足 | 补周期上下文、可信身份、`QUERY_RECORDS`、主子表快照和批量运维 |
| 报表、动态指标列导出、外部企微/飞书/钉钉 | 本期排除 | 由独立变更负责，不影响本期核心绩效低代码验收 |

## 2. 代码现状（Research Findings）

> 每个结论均以当前工作区源码为准；历史可行性文档中的成熟度描述不作为本 Spec 的代码事实。

### 2.1 已有能力，可直接复用

1. **本绩效场景所需的低代码数据权限已经具备**
   - `forge-admin-ui/src/views/app-center/components/designer/BusinessPermissionFlowPanel.vue` 的 `dataScopeOptions` 已提供 `TENANT/FOLLOW_SYSTEM` 和本人字段、组织字段映射。
   - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/DynamicDataScopeService.java#buildCondition` 已实现 `SELF/ORG/ORG_AND_CHILD/CUSTOM/TENANT_ALL/REGION` 条件；`DynamicCrudService#selectPage/#selectById/#updateById/#updateInternalFieldsById/#deleteById` 均把该条件传入 Repository，覆盖查询和写操作。
   - 绩效配置可把“我的考核”和“我的待考评”拆成两个运行入口，分别映射 `evaluatee_id` 和 `evaluator_id`；本变更不扩展按角色动态切换本人字段。

2. **业务动作已有事务、幂等和循环底座**
   - `BusinessActionExecutionService#executeInternal` 使用 `TransactionTemplate` 执行白名单步骤，并通过动作日志处理幂等命中。
   - `ForeachActionStepExecutor` 已支持遍历集合和逐行执行子步骤。
   - `CreateRecordActionStepExecutor#execute` 已能调用 `DynamicCrudService.insertInternal` 创建目标记录。
   - `ForeachActionStepExecutor#resolveItems` 只能遍历 `items/collection` 或动作上下文中已有的 `collectionPath`；`CreateRecordActionStepExecutor` 也只读取现有上下文，当前没有受控 `QUERY_RECORDS` 步骤来按员工岗位加载模板及模板明细。
   - `BusinessActionExecutionController#logs` 已支持失败日志分页查询；但 `BusinessActionExecutionService#findReusableLog/#reserveLog` 会拒绝使用相同幂等键重试 `FAILED` 日志，当前没有可信的失败重放入口。
   - 因此本变更不新建第二套状态机/动作 DSL，而是补节点结果和批量触发器到现有动作引擎的桥接。

3. **公式和父子聚合已有底座**
   - `FormulaType.AGGREGATE`、`StoredFormulaRuntime` 和 `StoredAggregateRefreshService` 已支持保存态公式；`DynamicCrudService#insert/#updateById/#deleteById` 分别调用 `refreshAfterChildInsert/#refreshAfterChildUpdate/#refreshAfterChildDelete` 刷新父表聚合。
   - 本变更只补节点主子表保存路径的公式重算一致性和“聚合结果必须满足条件”的声明式校验，不新建公式语言。

4. **站内消息动作已经可用**
   - `BusinessTriggerExecutor#executeSendMessageAction` 和 `SendMessageActionStepExecutor#execute` 已复用消息模板、业务记录变量和现有消息中心。
   - `BusinessMessageChannelService#sendInternalMessage` 已调用 `MessageService` 发送站内消息。

### 2.2 当前阻断点

1. **流程待办子表固定只读**
   - `forge-admin-ui/src/views/flow/todo.vue` 对 `ChildTableEditor` 固定传入 `readonly`。
   - `FormPermissionConfig.vue` 的字段权限协议只有扁平 `{field, readable, writable, required}`，无法表达子表字段及新增/删除行权限。
   - `BusinessTaskFormContextVO` 虽包含 `childrenConfig`，但没有节点级子表权限结果。

2. **业务对象节点保存只更新主表字段**
   - `BusinessFlowService#saveTaskFormContext` 收集扁平 `writableFields` 后调用 `DynamicCrudService.updateInternalFieldsById`，不会保存 `children` payload。
   - `BusinessTaskFormSaveDTO` 只有一个扁平 `data` Map，没有草稿/办理校验阶段语义。

3. **子表校验只覆盖单行必填和数值范围**
   - `DynamicCrudService#validateChildRow` 逐行检查 `required/min/max`，不支持 `SUM(weight) = 100`、至少一行、唯一明细或按办理阶段校验全部行。
   - `forge-admin-ui/src/components/lowcode-builder/shared/runtime-rules.js#resolveRuntimeControl` 能在前端按条件改变 `required`，但后端动态 CRUD/流程保存没有执行同协议；绕过前端可跳过“分差过大必须填评语”等条件必填。

4. **待办动作仍固定为 approve/reject**
   - `BusinessFlowService#completeBusinessTask` 只接受 `approve/reject`。
   - `forge-admin-ui/src/views/flow/todo.vue` 固定显示“同意/驳回”。
   - 当前流程回调动作主要由 `BusinessFlowService#executeFlowCallbackAction` 在终态 `APPROVED/REJECTED/CANCELED` 执行，缺少按任务节点和办理结果执行对象动作的配置；现有动作日志能查询失败，但不能用同一逻辑幂等键安全补偿失败动作。

5. **定时触发器只能扫描已有到期记录**
   - `BusinessTriggerSchedulerService#scanSingleTrigger` 要求配置 `dueField`，定位是对已有记录做临期/逾期扫描。
   - 它不能按 Cron 遍历“所有符合条件的源对象记录”，也不能形成一次批量运行的预览、汇总和失败清单。
   - 现有动作既缺少按上下文字段查询关联对象/子表集合的只读步骤，`CREATE_RECORD` 也没有面向“目标主表 + 目标子表集合”的结构化快照映射配置。

6. **消息接收人不能选择任意记录字段**
   - `BusinessTriggerExecutor#resolveReceivers` 和 `SendMessageActionStepExecutor#resolveReceivers` 只支持 `STARTER/OWNER/CREATOR/USERS/ROLES/DEPTS/ALL`。
   - 自动生成的绩效任务同时包含被考核人和考评人，单一 `OWNER` 无法覆盖不同节点的收件人。

7. **在职员工目录还没有冻结成低代码可查询来源**
   - 原需求按 `sys_user/sys_user_position` 描述员工、岗位和上下级关系，但当前 Forge 的真实主岗位关系是 `sys_user_post.is_main`，名称和数据语义不一致。
   - 低代码批量查询不能默认直查系统原始表；必须在 HARD-GATE 前选择“已发布员工目录业务对象”“平台系统目录适配器”或客户已有权威 HR 对象，并冻结在职、主岗位、主组织和直属上级字段契约。

8. **没有完整低代码应用配置包导入接口**
   - 当前仓库具备业务数据 Excel 导入导出和数据库表导入，不等于应用、对象、表单、流程、动作、触发器、权限和消息配置的一体化导入。
   - 因此验收 fixture 不能直接声称可导入；本变更必须先冻结版本化 fixture Schema 和明确的测试加载命令/API。是否进一步建设生产级通用配置包导入导出属于 HARD-GATE 选择，不作为绩效运行闭环的隐含前提。

### 2.3 主要风险

- 流程任务完成与业务动作执行跨越 Flowable 和 Admin 两个边界，不能假装成单库事务；必须通过稳定幂等键、执行日志和补偿重试保证最终一致。
- 节点子表权限若只在前端控制，会产生越权修改指标、权重或他人评分的安全漏洞。
- 批量触发若没有源查询白名单、分页上限和幂等键，可能造成全表扫描、重复任务或跨租户数据污染。
- 草稿保存和节点办理共用一套必填校验，会导致草稿不可保存；二者必须显式区分。
- 公式计算值必须由服务端生成，客户端提交的加权分和总分不得作为可信值直接落库。

## 3. 功能点

### 3.1 节点主子表权限

- [ ] 流程字段目录按“主表 / 子表关系 / 子表字段”分组展示。
- [ ] 子表字段权限使用稳定的 `relationCode + fieldCode` 标识，不使用显示名称或数据库表名作为运行协议。
- [ ] 每个子表关系支持 `readable/writable/requiredOnComplete/allowAdd/allowDelete`；`requiredOnComplete` 表示办理节点时每个有效行均必填。`required` 只保留给条件校验规则的效果名，二者不得混用。
- [ ] 流程设计器将权限写入 BPMN 用户任务扩展属性，解析/保存/重新打开后无损回显。
- [ ] 历史扁平字段权限保持兼容，未配置子表权限时继续按只读展示，不能默认放开写权限。

### 3.2 节点草稿与办理保存

- [ ] `PUT task-form-context` 只承担 `DRAFT` 保存，不再接受 `COMPLETE`，避免出现两个办理入口。
- [ ] `DRAFT` 只校验已填写值的类型、范围和字段权限，不执行办理必填及集合完整性校验。
- [ ] `POST task-action` 是唯一办理命令，在同一请求中接收业务结果、主表和子表数据，依次完成权限/任务状态复核、服务端公式计算、`COMPLETE` 校验、数据保存和 Flowable 任务办理。
- [ ] 办理命令以 `taskId + idempotencyKey + requestDigest` 防重；相同键但请求摘要不同必须拒绝，任务已被其他请求完成时返回明确的冲突结果。
- [ ] 复用并扩展 `sys_flow_task` 持久化办理命令状态，阶段固定为 `RESERVED -> FORM_SAVED -> FLOW_COMPLETED -> ACTION_SUCCEEDED`；后置动作失败进入 `COMPENSATION_PENDING`。命令同时固定结果、规范化流程变量、动作编码及已发布版本，不保存完整表单或评分正文。
- [ ] 命令恢复必须覆盖三种中断：业务数据已保存但 Flowable 调用失败时从 `FORM_SAVED` 继续；Flowable 已成功但响应丢失时先查询任务/历史并收敛为 `FLOW_COMPLETED`，不得再次办理；最终响应丢失时相同键和摘要直接返回已持久化结果。
- [ ] 同一任务已有命令预留时，相同键不同摘要返回幂等冲突；不同键不得抢占。命令状态只能通过带期望阶段的 CAS 前进，不能由客户端指定或回退。
- [ ] 子表只允许更新属于当前主记录的行；新增和删除必须同时满足关系权限和数据归属校验。
- [ ] 只读历史/已办上下文始终返回只读子表，不受原节点写权限影响。

### 3.3 声明式集合与条件校验

- [ ] 在主子表关系上支持白名单规则：`ROW_COUNT`、`SUM`、`UNIQUE`、`ALL_REQUIRED`。
- [ ] 首期比较运算只支持 `EQ/NE/GT/GTE/LT/LTE`，值类型限制为数字、字符串、布尔值和空值。
- [ ] 运行态集合规则阶段只支持 `SAVE/COMPLETE`；绩效模板权重合计使用 `SAVE`，评分完整性使用节点 `COMPLETE`。
- [ ] 阶段调用固定为：普通动态 CRUD 保存执行 `SAVE`；流程草稿只执行基础字段校验；唯一办理命令执行 `COMPLETE`。低代码对象/应用发布只静态检查规则引用、公式精度和依赖，不扫描业务记录。
- [ ] 前端可以即时预览校验结果，但后端必须基于本次事务中的完整主子表数据重新校验。
- [ ] 服务端先在内存中合并主表、现有子表和本次增删改，形成完整候选状态；再重算字段公式及候选聚合、执行条件/集合校验，全部通过后才在一个本地事务中持久化主子表并刷新存储聚合。任一校验失败必须零写入。
- [ ] 校验失败返回规则名称、关系名称和业务化错误文案，不返回 SQL、表名或表达式堆栈。
- [ ] 服务端支持表单条件规则的安全子集：条件来源限当前主表字段、服务端公式字段和当前节点结果，操作符限 `EQ/NE/GT/GTE/LT/LTE/EMPTY/NOT_EMPTY`，效果首期只执行 `required`。
- [ ] 前端运行规则可以继续控制显示和只读，但凡影响数据完整性的 `required` 必须同步为服务端条件校验协议；路由、URL、客户端用户对象和任意表达式不能作为服务端必填依据。

### 3.4 公式重算一致性

- [ ] 节点子表保存复用动态 CRUD 的字段公式、存储公式和父表聚合刷新链路。
- [ ] 子表的单项加权分由服务端字段公式计算，父表总分由聚合公式计算。
- [ ] 公式字段在节点权限目录中只读；客户端传入公式字段时忽略并记录安全审计，不覆盖服务端结果。
- [ ] 公式精度和舍入配置缺失、公式字段被节点配置为可写、或公式依赖失效时，发布检查必须阻断。

### 3.5 节点业务化结果动作

- [ ] 流程用户任务支持配置 `approveLabel/rejectLabel`，未配置时兼容“同意/驳回”。
- [ ] 每个结果支持配置 `actionCode` 及受控流程变量和值；业务状态更新、申诉创建、日志和消息均由该已发布动作完成，Flow Service 不再直接提供第二个状态写入口。
- [ ] 每个结果可选择当前任务表单中的补充字段和结果必填字段；点击结果后在办理确认弹窗中填写，服务端按当前结果再次校验，例如申诉类型/原因或 HR 调整分数。
- [ ] 节点完成动作复用 `BusinessActionExecutionService`，幂等键至少包含租户、流程实例、任务 ID、节点、结果和动作编码。
- [ ] 动作失败必须写入通用流程节点动作补偿台账；补偿重试复用同一业务动作日志和请求摘要，通过受控 CAS 把原 `FAILED` 执行恢复为 `RUNNING`，不得换幂等键制造第二次逻辑动作。
- [ ] 首期节点后置动作失败策略固定为 `COMPENSATE`，不开放“忽略失败”或任意失败脚本。
- [ ] 补偿详情和人工重试需要独立权限；已完成的 Flowable 任务保持已完成，页面展示“业务动作待补偿”，不能伪装成未完成。
- [ ] 补偿支持 Forge `@ScheduledJob` 自动扫描和人工重试，两者走同一 CAS 服务；达到最大次数后进入人工处理状态，不无限重放。
- [ ] 补偿记录必须固定动作编码及已发布版本、流程实例/任务/节点/结果、业务对象/记录、规范化流程变量、原幂等键和原请求摘要；不得保存完整任务表单、评分明细正文或消息正文。重试只执行该固定版本，不能随设计态动作变化漂移。
- [ ] 一旦流程绑定启用节点结果动作，`APPROVED/REJECTED` 的状态及副作用只归节点结果动作所有；同一绑定不得再配置历史 `callbackActions.APPROVED/REJECTED` 或 `approvedActionCode/rejectedActionCode`。发布检查发现共存必须阻断；`CANCELED` 终态回调可继续保留。
- [ ] 流程设计器仍以现有用户任务节点为唯一配置入口，不新增第二套工作台。

### 3.6 定时批量源查询与快照生成

- [ ] `SCHEDULE` 触发器新增“批量源查询”模式，与现有“到期字段扫描”模式并存。
- [ ] 批量模式支持 Cron、源业务对象、启用状态、白名单等值/范围条件、排序、单批大小和单次最大记录数。
- [ ] 批量分页必须使用配置排序并自动追加主键作为最终稳定排序；源记录在运行中发生变化时不得导致同一记录重复执行。
- [ ] 批量模式支持结构化周期策略 `DAY/WEEK/MONTH/QUARTER/YEAR`、时区和周期偏移，生成只读上下文 `period.type/key/start/end/label/scheduledTime`，供字段映射、消息模板和幂等键引用。
- [ ] 触发器必须绑定可信执行用户和当前组织；预览、手工执行、定时执行及失败重试均按该身份实时解析数据权限，操作者权限只决定能否管理/运行触发器，不能改变候选集。
- [ ] 支持预览候选数量和前 N 条脱敏样例，不执行写操作。
- [ ] 新增通用只读 `QUERY_RECORDS` 动作步骤：只允许选择已发布对象、白名单字段/操作符、排序、上限和是否加载已声明子表，并把结果写入命名输出变量。
- [ ] `QUERY_RECORDS` 的条件值可以引用当前源记录和循环项，但不能写 SQL/脚本；查询必须使用触发器绑定身份的数据权限，默认最多返回一页，超上限失败关闭。按岗位匹配模板时必须包含启用条件、确定性排序和 `expect=EXACTLY_ONE`，零条或多条均明确失败，不能静默取第一条。
- [ ] 每个源记录调用现有业务动作；可组合 `QUERY_RECORDS + FOREACH + CREATE_RECORD` 查模板、取模板明细并创建目标主表和子表。
- [ ] `CREATE_RECORD` 支持结构化 `children` 映射，将模板明细复制为任务明细快照。
- [ ] 支持稳定幂等表达式，至少可以由“目标对象 + 员工 + 周期”组成；重复执行返回已存在结果，不重复生成。
- [ ] 批量动作继续以 `ai_business_action_execution_log` 的唯一幂等键作为并发防重权威；触发器日志只记录每次尝试，目标低代码对象必须配置并发布对应复合唯一索引作为最终数据约束。
- [ ] 手工执行、定时执行和失败重试使用同一执行服务和相同幂等规则。
- [ ] 流程补偿和批量失败项统一通过可信 `BusinessActionRetryService` 对原 `FAILED` 动作日志执行 CAS 重试；`ai_business_trigger_log` 必须关联 `action_log_id`，不得另建一套动作重放语义。
- [ ] 无模板、无考评人、字段映射失败等情况进入失败明细，不影响其它源记录继续处理。

### 3.7 批量运行运维

- [ ] 每次执行产生运行汇总，记录候选、成功、失败、跳过、幂等命中和耗时。
- [ ] 运行详情使用 `pageNum/pageSize` 分页查看失败源记录、失败阶段和脱敏错误，不保存敏感业务全文。
- [ ] 管理员可以只重试失败项；已成功项不得再次执行。
- [ ] 每个批量运行固定触发器配置版本/摘要、解析后的 `period` JSON、绑定用户/组织 ID、源查询版本/摘要和动作已发布版本；失败重试只使用原运行上下文，不读取触发器当前已编辑配置。用户/组织 ID 固定，但账号有效性和当前角色/数据权限仍在每次重试时实时校验，撤权后失败关闭。
- [ ] 触发器禁用、应用下线或源/目标对象未发布时拒绝执行。

### 3.8 消息动态接收人

- [ ] `SEND_MESSAGE` 增加 `FIELD:<fieldCode>` 和多个字段接收人配置；候选字段仅限用户选择组件、用户引用字段或设计器显式标记的接收人字段。
- [ ] 字段值支持单个用户 ID、用户 ID 集合和逗号分隔值，统一规范化为字符串后安全转换。
- [ ] 字段不存在、值非法或解析为空时按配置执行 `FAIL/SKIP`，默认失败关闭，不回退给操作人。
- [ ] 设计器必须从当前对象字段目录选择接收人字段，不要求用户手写协议。
- [ ] 继续复用现有消息模板、业务变量、站内消息投递和消息记录。

### 3.9 绩效纯低代码验收样板

- [ ] 使用低代码创建指标分组、指标、岗位模板、模板明细、绩效任务、任务明细、申诉和业务日志对象。
- [ ] 使用现有数据权限配置两个运行入口：“我的考核”映射 `evaluatee_id`，“我的待考评”映射 `evaluator_id`；HR 角色使用全部数据权限。
- [ ] 配置模板权重 `SUM(weight) = 100`，自评/上级评分节点分别控制对应子表字段。
- [ ] 配置模板至少一条指标、同一指标不可重复，分别使用 `ROW_COUNT >= 1` 和 `UNIQUE(indicator_id)`，证明白名单集合规则不是 KPI 专用 SUM 实现。
- [ ] 使用服务端公式生成评分差异字段，并配置条件必填评语；确认申诉和 HR 调分结果分别配置专属补充字段及必填规则。
- [ ] 配置节点按钮文案和节点动作，完成任务状态、申诉记录、业务日志及站内消息联动。
- [ ] 配置人工和定时批量生成，验证模板明细、指标、权重、满分、岗位和考评人均为历史快照。
- [ ] 样板提交版本化脱敏低代码配置 fixture、合成测试数据准备/清理脚本和配置说明；仅在测试加载器已实现时标记为可导入，不提交 KPI 专用生产代码或客户真实数据。
- [ ] 验收 fixture 使用显式 `schemaVersion`、`fixtureId` 和测试命名空间；测试加载器只调用受权限保护的现有管理 API 按依赖顺序创建/更新配置，不直接写平台元数据表。
- [ ] fixture 重复加载采用受控 UPSERT：只更新同一 `fixtureId + namespace` 管理的资源；与非 fixture 同编码资源冲突时失败关闭。清理命令只删除/逻辑删除该命名空间创建的合成配置和数据，并在非测试环境默认拒绝运行。

## 4. 业务规则

1. **权限规则**
   - 服务端按当前任务、当前办理人、主记录归属、子记录归属和节点字段权限五层校验。
   - 未识别的关系、字段和行操作一律拒绝；旧配置未声明子表写权限时默认只读。
   - 数据权限继续使用现有低代码能力，本变更不绕过 `DynamicDataScopeService`。

2. **草稿与办理规则**
   - 草稿允许评分字段为空，但不允许越界值、非法类型和越权字段。
   - 办理先预留持久化命令，再在内存构造完整主子表候选状态并重算公式；只有完整校验通过后，才在本地事务中原子持久化候选状态、刷新聚合并推进到 `FORM_SAVED`，随后完成 Flowable 任务和节点动作。
   - 校验失败不得创建业务数据写入或推进命令阶段；本地事务失败整体回滚。跨 Flowable 边界的中断按持久化命令阶段恢复，不重复保存、办理或执行动作。
   - 前端按钮防重不能替代服务端任务状态和幂等校验。

3. **计算规则**
   - 权重、评分和总分使用 `BigDecimal` 语义，不使用浮点数。
   - 公式精度和舍入方式沿用公式字段配置；未配置时发布检查必须阻断，而不是猜测默认业务口径。
   - 服务端公式字段为唯一可信结果。

4. **条件校验规则**
   - 条件必填使用结构化字段引用和白名单操作符，不执行 JavaScript、SpEL 或 SQL。
   - 节点结果专属字段必须属于当前任务表单且具备可写权限；客户端不能通过自报结果绕过 BPMN 节点允许的结果集合。
   - 条件校验在服务端公式重算之后、数据持久化和 Flowable 任务完成之前执行。

5. **节点动作规则**
   - 节点动作只引用已发布、属于当前业务对象的动作编码。
   - 动作步骤继续受现有白名单、事务、权限和幂等约束。
   - Flowable 已完成但后置动作失败时必须进入补偿台账；重试只能以原请求摘要和原逻辑幂等键重放后置动作，不能再次完成任务。
   - 新节点结果动作与历史绑定级 `APPROVED/REJECTED` 回调动作互斥；发布和运行时均失败关闭，避免同一状态或副作用被执行两次。`CANCELED` 继续由终态回调处理。

6. **批量规则**
   - 每次运行固定租户上下文，禁止跨租户查询或写入。
   - 源查询字段和操作符必须来自已发布模型白名单，禁止任意 SQL。
   - 动作内 `QUERY_RECORDS` 与批量源查询使用同一白名单查询服务和可信执行身份，不允许形成绕过数据权限的第二查询通道。
   - 默认分页执行，单批和单次上限由平台配置约束；任何一条失败不得回滚其它已成功源记录。
   - 单条源记录内创建主表、子表和动作日志保持本地事务；跨源记录不使用大事务。
   - 幂等键必须稳定、可审计，不能只依赖执行时间或随机 UUID。
   - 幂等职责固定为三层：业务动作日志负责并发执行占位，触发器日志负责批次/尝试审计，目标对象复合唯一索引负责最终数据防重；三者不得互相替代。
   - 后台执行身份必须绑定有效租户、用户和当前组织，并在每次运行时重新计算该身份的实时权限；身份失效、组织不可用或权限不足时运行失败关闭。
   - 批量失败重试只能读取原 `run` 固定的配置/周期/身份/源查询/动作版本快照；触发器编辑后新运行使用新配置，旧运行不得漂移。

7. **验收 fixture 规则**
   - Proposal 阶段不假定存在完整应用配置导入接口。验收 fixture 必须由版本化 Schema 和明确的测试加载命令消费，并记录调用的管理 API、资源依赖顺序和清理结果。
   - 加载器仅用于测试/验收环境，使用测试命名空间和受控 UPSERT；遇到人工资源编码冲突、未知 Schema 版本或环境不允许清理时失败关闭。

8. **消息规则**
   - 动态字段接收人只能读取当前授权记录中的字段。
   - 接收人字段不得通过任意表达式执行代码；只允许字段引用和静态角色/用户/组织组合。
   - 本期只承诺站内消息。外部协同消息是否发送不影响本变更验收。

## 5. 数据变更

| 操作 | 表名/协议 | 字段/索引 | 说明 |
|------|-----------|-----------|------|
| 扩展 | `sys_flow_task` | 复用 `action_idempotency_key/action_request_digest/action_type`，新增 `action_stage, action_code, action_version, action_context_json, action_result_json, action_update_time` 及阶段索引 | 持久化唯一办理命令；上下文只保存结果、规范化流程变量和固定动作引用，结果只保存可安全重放的响应摘要，不保存完整表单/评分正文 |
| 新增 | `ai_business_flow_action_compensation` | 流程实例、任务、节点、业务对象/记录、结果、动作编码及已发布版本、规范化流程变量、原幂等键/摘要、`action_log_id`、状态、重试次数、下次重试时间、错误摘要及标准审计字段；唯一键 `(tenant_id, task_id, outcome, action_code)` | 不保存完整表单/评分正文，不提供普通行删除；仅承载节点动作补偿状态和重试上下文，`ai_business_action_execution_log` 仍是动作幂等权威 |
| 新增 | `ai_business_trigger_run` | `id, tenant_id, trigger_id, run_type, scheduled_time, execute_status, total_count, success_count, failed_count, skipped_count, idempotent_count, correlation_id, trigger_version_digest, period_json, run_as_user_id, run_as_org_id, source_config_digest, action_code, action_version, start_time, finish_time, error_summary` 及标准审计字段 | 保存一次批量运行汇总并冻结失败重试上下文；不提供普通行删除接口，留存清理由专用策略处理 |
| 扩展 | `ai_business_trigger_log` | `run_id, source_record_id, idempotency_key, action_log_id, attempt_no, retryable` | 关联运行批次和动作日志，逐次记录初次执行和失败重试；不作为唯一幂等占位 |
| 新增索引 | `ai_business_trigger_log` | `(tenant_id, run_id, execute_status)`、普通索引 `(tenant_id, trigger_id, idempotency_key, attempt_no)` | 支持运行详情和尝试追踪，不设置幂等唯一索引 |
| 扩展 JSON | `ai_business_trigger.event_condition` | `schedule.mode=BATCH_QUERY`、Cron、周期策略/时区/偏移、源对象查询、分页、上限、`runAsUserId/runAsOrgId` | 与现有 `DUE_FIELD` 模式兼容；执行时实时验证绑定身份 |
| 扩展 JSON | `ai_business_trigger.action_config` | 子表快照映射、幂等表达式、失败策略 | 复用现有业务动作协议 |
| 扩展 BPMN | 用户任务扩展属性 | 子表字段权限、行操作权限、按钮文案、节点结果动作 | 由真实流程设计器维护 |
| 扩展 Schema | 低代码关系/表单规则 | 集合校验规则及校验阶段 | 不新增任意脚本字段 |
| 字典/资源 | `sys_dict_type/sys_dict_data/sys_resource` | 批量运行状态、运行类型、失败策略及相关权限 | Flyway 使用 `NOT EXISTS`，`tenant_id=1` |

所有正式结构和内置数据变更必须新增 `forge-server/db/migration/V<next>__*.sql`，不得修改已执行脚本。迁移版本在 `/apply` 开始时根据仓库最新版本分配。

## 6. 接口变更

| 操作 | 接口 | 方法 | 变更内容 |
|------|------|------|----------|
| 扩展 | `/ai/business/flow/task-form-context` | GET | 返回子表字段权限、行操作权限、校验阶段和节点结果文案 |
| 扩展 | `/ai/business/flow/task-form-context` | PUT | 只保存 `DRAFT` 主子表数据，不办理任务 |
| 扩展 | `/ai/business/flow/task-action` | POST | 唯一办理入口；接收业务结果及完整主子表数据，按持久化命令阶段执行或恢复，返回稳定的办理/节点动作/补偿状态 |
| 新增 | `/ai/business/flow/action-compensations` | GET | 按 `pageNum/pageSize` 分页查询节点动作补偿记录 |
| 新增 | `/ai/business/flow/action-compensations/{id}` | GET | 查询固定动作版本、流程/业务定位、尝试历史和脱敏错误，不返回表单/评分正文 |
| 新增 | `/ai/business/flow/action-compensations/{id}/retry` | POST | 使用原请求摘要和逻辑幂等键 CAS 重试失败节点动作，不重复办理 Flowable 任务 |
| 新增 | `/ai/business/trigger/{id}/preview` | POST | 预览批量源查询数量和脱敏样例，不执行动作 |
| 新增 | `/ai/business/trigger/{id}/run` | POST | 手工执行批量触发器，返回 `runId` |
| 新增 | `/ai/business/trigger/runs` | GET | 分页查询批量运行汇总，参数使用 `pageNum/pageSize` |
| 新增 | `/ai/business/trigger/runs/{runId}` | GET | 查询运行汇总；失败明细通过分页日志接口按 `runId` 查询 |
| 新增 | `/ai/business/trigger/runs/{runId}/retry-failed` | POST | 只重试可重试失败项 |
| 兼容 | `/ai/business/trigger/logs` | GET | 保留原接口，并增加 `runId/executeStatus` 筛选 |
| 测试工具 | `fixtures/kpi-lowcode-fixture-loader.mjs load|cleanup` | CLI | 按版本化 fixture 调用受保护的管理 API；仅允许测试环境和测试命名空间，重复加载受控 UPSERT，清理输出删除清单 |

接口继续使用现有 `@ApiEncrypt/@ApiDecrypt`、Sa-Token 权限和 `RespInfo` 协议。所有 `Long` ID 在前端按字符串处理。

## 7. 影响范围

- **低代码流程运行时**：`BusinessFlowService`、任务表单 DTO/VO、待办页、子表编辑器。
- **流程设计器**：字段权限目录、BPMN 用户任务解析/写回、节点权限和结果动作配置。
- **动态 CRUD**：受控主子表节点保存、集合校验、公式刷新和子记录归属校验。
- **业务动作**：节点结果桥接、只读 `QUERY_RECORDS`、`CREATE_RECORD` 子表映射和幂等上下文。
- **业务触发器**：批量源查询模式、运行汇总、失败重试、预览和运维页面。
- **消息动作**：动态字段收件人规则及其前端配置。
- **数据库**：触发器运行汇总、日志扩展、字典和权限资源。
- **流程任务持久化**：扩展 `sys_flow_task` 办理命令阶段和最小恢复上下文。
- **文档/验收**：绩效纯低代码搭建说明、版本化 fixture 和测试加载/清理工具，不包含报表部分。

## 8. 风险与关注点

> ⚠️ 本变更涉及状态流转、权限控制和批量数据创建，必须人工审查。

1. **状态一致性风险（P0）**：Flowable 任务完成和业务动作不是同一数据库事务，必须设计补偿状态、重试入口和状态修复，不允许吞掉动作失败。
2. **越权写子表风险（P0）**：必须验证任务办理人、主记录、子记录、关系编码和字段权限，不能只过滤 JSON 字段名。
3. **重复生成风险（P0）**：批量任务必须同时有平台幂等键和目标业务唯一约束；仅依赖查询“是否存在”会有并发竞态。
4. **配置表达能力风险（P0）**：白名单协议必须覆盖绩效样板，但不能演变为可执行任意代码的脚本引擎。
5. **兼容风险（P1）**：历史 BPMN 没有子表权限和自定义文案时，必须维持主表现有行为、子表只读和默认同意/驳回文案。
6. **批量性能风险（P1）**：源查询、模板明细读取和目标写入必须分页，避免 N+1 和单大事务；压测上限待业务规模确认。
7. **消息误发风险（P1）**：接收人字段为空或非法时默认失败关闭，禁止无提示回退给当前操作人或全员。
8. **配置泄露风险（P1）**：预览、运行日志和错误信息必须脱敏，不保存整份员工或评分数据快照。
9. **后台身份风险（P0）**：Cron 无天然登录用户；未绑定可信执行身份或手工/定时使用不同身份会造成候选集漂移或越权。
10. **办理恢复风险（P0）**：本地数据保存、Flowable 完成和后置动作分属不同边界；缺少持久化阶段或历史核对会造成重复办理、已保存未流转或响应丢失后无法判断结果。
11. **配置漂移风险（P0）**：补偿或批量失败重试若读取当前设计态动作/触发器配置，可能对历史失败项执行不同逻辑；必须固定发布版本和原运行上下文。
12. **员工目录口径风险（P0）**：原需求的 `sys_user_position` 与 Forge 当前 `sys_user_post` 不一致，未冻结权威来源前不能进入批量生成人员查询实现。
13. **fixture 误导风险（P1）**：仓库当前没有完整应用配置包导入 API；未定义加载器就把 JSON 称为可导入，会形成无法复跑的验收证据。

## 8.5 测试策略

- **测试范围**：后端单元测试、Mapper 集成测试、前端组件测试、BPMN round-trip、动态 CRUD 主子表测试、流程回调/补偿测试、定时批量并发幂等测试、站内消息接收人测试和绩效配置 E2E。
- **覆盖率目标**：新增核心 Service/Executor 分支覆盖率不低于 85%；P0 权限、幂等、状态补偿和跨行校验路径必须 100% 场景覆盖。
- **独立 Test Spec**：是；Proposal 阶段已创建 `test-spec.md` 和 `execution-log.md` 骨架，进入 `/apply`、每个 Phase 收尾和 `/test` 时按 `code-copilot/rules/automated-testing-standard.md` 增量维护。
- **关键回归**：普通主表流程待办、历史 BPMN、旧绑定级终态回调、现有到期提醒、现有 `SEND_MESSAGE` 规则、现有 `CREATE_RECORD/FOREACH` 和动态 CRUD 普通主子表不得回退。
- **E2E 最小链路**：人工生成一名员工任务 -> 自评草稿 -> 自评提交 -> 上级评分 -> 员工确认；另一路进入申诉 -> HR 驳回/调分；重复生成和重复回调均不产生重复业务副作用。

## 9. 待澄清

> 以下问题全部解决并写回 Spec 后才能进入 `/apply`。

- [ ] **范围确认**：本变更是否只承诺站内消息，不要求低代码消息动作直达企业微信？推荐：是，企微通道由独立协同变更验收。
- [ ] **数据权限入口**：是否接受“我的考核”和“我的待考评”拆成两个运行入口，分别映射 `evaluatee_id/evaluator_id`？推荐：接受，不扩展按角色动态切换本人字段。
- [ ] **节点结果模型**：首期是否只支持两个结果槽位（主结果/次结果，可改文案并绑定动作），还是要求任意数量业务按钮？推荐：两个结果槽位，覆盖绩效并保持 Flowable approve/reject 兼容。
- [ ] **批量源对象（HARD-GATE）**：在职员工的权威来源选择哪一种：A. 客户已有并已发布的低代码员工目录对象；B. 新增只读“系统目录适配器”，以 `sys_user + sys_user_post.is_main + 当前组织关系` 暴露白名单字段；C. 客户现有权威 HR 业务对象。不得默认让 `QUERY_RECORDS` 直查原始 `sys_user` 表，也不得继续使用需求文档中并不存在于 Forge 的 `sys_user_position` 名称。
- [ ] **直属上级**：使用当前部门负责人/上级部门负责人，还是新增员工个人直属上级关系？推荐：客户冻结后作为配置来源；本变更不新建组织主数据模型。
- [ ] **目标唯一约束**：绩效任务防重业务键是否固定为 `tenant + evaluatee + cycle_type + cycle_key`？推荐：是。
- [ ] **绩效周期口径**：月/季/年任务按当前周期还是上一完整周期生成，业务时区和周期起止边界是什么？推荐：默认 `Asia/Shanghai`，由触发器显式配置周期偏移，禁止从 Cron 文本猜测。
- [ ] **后台执行身份**：触发器绑定哪个 Forge 用户及当前组织作为预览、手工、定时和重试的统一权限主体？推荐：创建最小权限服务账号并绑定明确组织，禁止使用超级管理员或临时操作人。
- [ ] **批量规模**：单租户最大员工数、单模板最大指标数、允许执行时长和失败重试次数需要给出验收基线。
- [ ] **公式口径**：满分可变时是否使用 `score / max_score * weight`，以及中间精度、最终精度和舍入方式。
- [ ] **评分差异规则**：上级评分与自评分“差异过大”的阈值是多少，按总分差还是任一指标明细分差判断？推荐：先按总分绝对差，阈值做低代码配置。
- [ ] **申诉调分**：HR 调整总分还是逐项明细；如果只调总分，任务明细 `final_score` 如何解释。
- [ ] **验收配置载体**：是否接受提交版本化脱敏 fixture、测试专用 API 加载器和合成数据准备/清理脚本，用于自动化重建绩效样板？推荐：接受；本期不把它表述为生产级应用配置导入功能。若客户要求生产环境一键导入/导出完整低代码应用，应另立通用平台变更。
- [ ] **实施授权范围**：HARD-GATE 是一次授权全部 P0/P1，还是先只授权 P0、P1 另行确认？推荐：先完成并验收 P0，再单独确认 P1 运维体验、自动重试和验收工具。

## 10. 技术决策

| 决策 | 选择 | 理由 |
|------|------|------|
| 实现定位 | 平台通用能力补齐，不开发 KPI 专用模块 | 用户目标是完全低代码，能力应可复用于评分、巡检、验收等主子表流程 |
| 节点配置归属 | 真实流程设计器/BPMN 用户任务 | 遵循 Forge 流程配置所有权，不建设第二套节点配置 UI |
| 子表权限协议 | 稳定 `relationCode + fieldCode`，行操作单独授权 | 避免表名耦合，支持字段级和结构级双重安全控制 |
| 校验协议 | 白名单声明式集合规则 | 满足权重、完整性和唯一性，同时避免任意脚本安全风险 |
| 条件必填 | 前端运行规则 + 同源服务端安全子集 + 节点结果必填 | 保留低代码交互，同时保证绕过前端也不能跳过评语、申诉和调分必填 |
| 办理入口 | PUT 只存草稿，POST task-action 单一承载 COMPLETE 数据与结果 | 消除双入口、重复保存和校验后数据变化风险 |
| 办理恢复 | `sys_flow_task` 持久化阶段 + CAS 前进 + Flowable 历史核对 | 处理业务数据已保存、Flowable 响应丢失和最终响应丢失，不依赖客户端猜测 |
| 公式能力 | 复用现有 Formula Runtime | 当前已有存储公式和聚合刷新，不重复实现计算引擎 |
| 状态/副作用 | 节点结果绑定现有 Business Action | 复用事务、幂等、执行日志、`FOREACH` 和消息步骤 |
| 关联数据加载 | 新增白名单 `QUERY_RECORDS` 动作步骤 | 现有 FOREACH 只能消费已有集合；统一复用动态 CRUD 查询和数据权限，不允许脚本/SQL |
| 跨服务一致性 | Flowable 完成 + 幂等后置动作 + 补偿重试 | Admin 与 Flowable 不能构造虚假本地事务，采用可观察的最终一致 |
| 动作补偿 | 新增流程节点补偿台账，复用原动作日志及幂等键 | 补偿表只负责编排重试，不成为第二套动作幂等权威 |
| 回调所有权 | 节点结果动作与绑定级 APPROVED/REJECTED 回调互斥，CANCELED 可保留 | 避免状态和消息等副作用重复执行，同时兼容取消终态 |
| 定时生成 | 扩展 Business Trigger 为批量源查询模式 | 复用 Forge Job、触发器、动作和日志，不使用外部 Python/crontab |
| 周期变量 | 结构化 period 上下文 | Cron 只控制触发时间；周期键、边界、消息和幂等使用统一服务端结果，不依赖脚本格式化 |
| 批量防重 | 动作日志占位 + 尝试日志审计 + 目标复合唯一索引 | 分离并发控制、可观测性和最终数据约束，允许失败重试保留完整历史 |
| 失败动作重试 | 流程补偿与批量重试共用 `BusinessActionRetryService` | 对原 FAILED 日志做同一套 CAS 恢复，触发日志仅引用动作日志 |
| 批量上下文 | run 固定触发器/源查询/周期/身份/动作版本 | 历史失败重试不受当前设计态配置编辑影响 |
| 员工目录 | HARD-GATE 选择已发布对象、系统目录适配器或客户 HR 对象 | 原需求表名与当前 Forge 不一致，禁止默认为系统原表开旁路查询 |
| 数据权限 | 复用现有 FOLLOW_SYSTEM，绩效拆运行入口 | 当前能力足够，避免把业务视图差异升级成权限引擎重构 |
| 消息范围 | 复用站内消息，仅新增 FIELD 接收人 | 当前需求文档建议 OA 内部消息，外部协同通道另有独立变更 |
| 报表范围 | 明确排除 | 用户本轮要求不考虑报表部分 |
| 验收 fixture | 版本化 Schema + 测试加载/清理工具，不冒充生产导入接口 | 当前没有完整应用配置导入 API；先保证验收可重复，生产配置包能力另立变更 |

## 11. 执行日志

| Task | 状态 | 实际改动文件 | 备注 |
|------|------|--------------|------|
| Research | completed | 无生产代码改动 | 已核对流程待办、动态 CRUD、公式、动作、触发器、数据权限和消息链路 |
| Proposal | completed | `spec.md`、`tasks.md`、`test-spec.md`、`execution-log.md` | 等待待澄清项和 HARD-GATE 确认 |
| Apply | pending | - | 未授权，不得修改生产代码 |

## 12. 审查结论

- **Reader Test**：completed；两轮审查已修复阶段语义、双办理入口、持久化命令恢复、动作/回调所有权、补偿版本、通用 CAS 重试、批量上下文冻结、后台身份、员工目录来源和验收载体等歧义。
- **Spec 合规审查**：待用户确认范围和待澄清项后执行。
- **代码质量审查**：未进入 `/apply`，不适用。
- **当前结论**：该方案能够把绩效当前缺口收敛为通用低代码链路补齐；现有数据权限、消息中心、动作引擎和公式引擎不重复建设。

## 13. 确认记录（HARD-GATE）

- **确认状态**：未确认
- **确认时间**：
- **确认人**：
- **确认范围**：
- **约束**：用户明确确认本 Spec 和 `tasks.md` 前，不得进入 `/apply` 或修改生产代码。
