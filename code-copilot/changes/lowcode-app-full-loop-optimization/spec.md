# 低代码应用全链路闭环优化
> status: proposed
> created: 2026-06-01
> complexity: 🔴复杂
> related: `code-copilot/changes/lowcode-business-app-platform-business-closure/spec.md`, `code-copilot/changes/form-first-business-object-designer/spec.md`

## 1. 背景与目标

当前低代码应用已经具备应用中心、业务套件、业务对象、运行态 CRUD、对象设计器、触发器、流程桥接、消息通道、统计看板等基础能力，但这些能力还没有被组织成业务人员能理解和使用的一条闭环主链路。

本次改造目标是把低代码应用从“功能分散可配置”收敛为“业务单据驱动的全链路闭环”：

`应用/套件 → 业务对象 → 单据 → 表单录入 → 流程流转 → 触发器自动化 → 消息推送 → 权限控制 → 报表展示 → 业务验收`

完成后，以“客户商机管理”为例，业务人员应能完成：

- 在 CRM 套件中配置“商机”作为业务单据。
- 商机录入后，可手动发起流程，也可由触发器按条件自动发起流程。
- 选择已有流程模型并配置“商机字段 → 流程变量”的映射。
- 流程审批通过或驳回后，回写商机单据状态。
- 审批结果通过站内消息推送；企微、飞书、钉钉先保留通道与适配器 TODO，不实现真实外部 API 调用。
- 不同用户按数据权限看到不同商机，不同角色按按钮权限看到不同操作。
- 报表看板展示商机数量、阶段分布、金额汇总、审批通过率等统计信息。

同时，本次要做产品边界收敛：

- 去掉“审批引擎”独立概念，审批场景统一归入“流程引擎”。
- 去掉“导入导出引擎”独立概念，导入导出保留为业务对象运行页能力。
- 去掉“移动端中心”和“集成中心”独立中心入口。
- 第三方平台推送只保留扩展点、通道配置引用、执行日志和 TODO 状态，不在本阶段实现企微、飞书、钉钉真实推送。

## 2. 代码现状（Research Findings）

### 2.1 应用中心与中心化入口

- `forge-admin-ui/src/views/app-center/index.vue:3-21` 当前“应用总览”首屏提供“引擎中心”按钮，并围绕业务套件、对象、应用入口组织页面。
- `forge-admin-ui/src/views/app-center/engines.vue:43-49` 当前硬编码展示流程引擎、报表引擎、权限引擎、消息引擎、触发器，其中“流程引擎”同时包含 `FLOW` 和 `APPROVAL` 类型。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessEngineSummaryService.java:34-50` 后端汇总仍包含 `APPROVAL` 审批引擎和 `IMPORT_EXPORT` 导入导出引擎，和本次“去掉审批引擎、导入导出引擎”的目标冲突。
- `forge-admin-ui/src/views/app-center/mobile.vue:17-18` 当前存在独立“移动端中心”，用于登记 H5、移动待办、移动审批和移动业务入口。
- `forge-admin-ui/src/views/app-center/integration.vue:17-18` 当前存在独立“集成中心”，用于登记开放接口、Webhook 和第三方平台入口。
- `forge/db/migration/V1.0.27__add_business_app_platform_dicts_menus.sql:101-103` 和 `forge/db/migration/V1.0.31__normalize_app_center_menu_directory.sql:65-67` 已注册引擎中心、移动端中心、集成中心菜单资源，后续需要用新增 Flyway 脚本收敛可见入口。

### 2.2 动态 CRUD 已能发布业务事件

- `DynamicCrudController#create/update/delete` 在新增、修改、删除后分别调用业务事件发布器，见 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/controller/DynamicCrudController.java:67-105`。
- `BusinessEventPublisher` 会根据 `configKey` 查询 `ai_crud_config.object_code`，只有能解析为业务对象的动态 CRUD 才触发自动化，见 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessEventPublisher.java:93-139`。
- `BusinessEventPublisher#checkStatusChange` 当前只识别 `status/state/audit_status/approval_status` 作为状态字段，见 `BusinessEventPublisher.java:63-88`；单据状态字段需要标准化，否则状态变更触发器不稳定。

### 2.3 触发器已具备骨架，但业务语义和动作闭环不足

- `forge/db/migration/V1.0.46__add_business_trigger_and_message_channel.sql:4-78` 已新增 `ai_business_trigger`、`ai_business_trigger_log`、`ai_business_message_channel` 三张表，覆盖触发器、执行日志和消息通道。
- `BusinessTriggerController` 已提供触发器分页、详情、新增、修改、删除、启停和日志接口，见 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/controller/BusinessTriggerController.java:32-89`。
- `BusinessTriggerService#selectPage/selectLogPage` 当前分页查询仍使用 `LambdaQueryWrapper`，见 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessTriggerService.java:35-44` 和 `122-131`；按项目规则，查询类 SQL 后续应迁移到 Mapper XML。
- `BusinessTriggerMapper.xml:5-18` 已有按对象和事件查询启用触发器的 XML SQL，可作为后续分页查询下沉 XML 的参考。
- `BusinessTriggerExecutor` 支持 `START_FLOW`、`SEND_MESSAGE`、`CREATE_RECORD`、`UPDATE_FIELD`、`WEBHOOK` 动作分发，见 `BusinessTriggerExecutor.java:177-195`。
- `BusinessTriggerExecutor#executeCreateRecordAction`、`executeUpdateFieldAction`、`executeWebhookAction` 仍是 TODO 或模拟状态，见 `BusinessTriggerExecutor.java:335-374`；“审批通过后自动生成交接记录”这类数据闭环还没有真实落地。

### 2.4 流程桥接存在，但审批独立服务仍是模拟

- `BusinessFlowService#startFlow` 已能从 `ai_business_binding` 读取 `FLOW` 绑定，通过 `FlowClient.startProcess` 发起流程，见 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessFlowService.java:45-90`。
- `BusinessFlowService#buildFlowVariables` 后端期望变量映射字段名是 `formField` 和 `flowVariable`，见 `BusinessFlowService.java:189-208`。
- `BusinessTriggerExecutor#executeStartFlowAction` 也期望 `formField` 和 `flowVariable`，见 `BusinessTriggerExecutor.java:200-214`。
- `forge-admin-ui/src/views/app-center/trigger.vue:101-119` 前端触发器变量映射当前使用 `field` 和 `variable`，并在 `trigger.vue:486-488` 直接序列化保存，和后端期望不一致，会导致触发器发起流程时变量映射失效。
- `BusinessApprovalRuntimeService#startApproval` 当前没有真实调用流程引擎，只返回 `recordId` 并标记 TODO，见 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessApprovalRuntimeService.java:93-110`；该服务应被“流程运行服务”替代或收敛。
- `BusinessApprovalController` 暴露 `/ai/business/approval/runtime` 和 `/ai/business/approval/start`，见 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/controller/BusinessApprovalController.java:28-45`；本次不再保留独立审批引擎入口。

### 2.5 消息推送和第三方通道已有预留

- `BusinessTriggerExecutor#executeSendMessageAction` 已复用现有消息中心 `MessageService.send`，支持模板、通道、接收人规则和业务参数，见 `BusinessTriggerExecutor.java:235-287`。
- `BusinessTriggerExecutor#resolveReceivers` 当前支持 `OWNER`、`CREATOR`、`USERS`、`ALL`，见 `BusinessTriggerExecutor.java:290-333`；需要补齐基于流程处理人、部门主管、角色的接收人规则。
- `V1.0.46__add_business_trigger_and_message_channel.sql:91-120` 已初始化企业微信、飞书、钉钉通道为停用且备注“待实现”，符合本阶段只保留 TODO 的方向。

### 2.6 报表统计已有基础，但还不是业务场景看板

- `BusinessStatsService#overview/groupCount/trend` 已支持按 `configKey` 做总数、今日新增、本月新增、字段分组和时间趋势统计，见 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessStatsService.java:35-126`。
- `forge-admin-ui/src/views/app-center/stats-dashboard.vue:12-13` 当前看板文案是通用“数据统计看板”，`stats-dashboard.vue:135-158` 只固定读取概览、`status` 分组和近 30 天趋势；商机阶段、金额汇总、流程结果等业务指标还没有场景化配置。

### 2.7 权限已有设计入口，但按钮权限和流程动作未闭环

- `BusinessPermissionFlowPanel.vue:5-19` 当前只维护数据权限和树形模型，未承载“流程绑定、单据状态、按钮权限”的完整配置。
- `BusinessActionDesigner.vue:12-16` 已提供“添加发起审批”和“新增操作”入口，`BusinessActionDesigner.vue:152-158` 支持打开页面、调用能力、发起审批、执行触发器、打开外部链接等动作类型。
- `BusinessActionDesigner.vue:84-86` 仅用文本输入配置流程标识，缺少已发布流程选择、变量映射、发起条件、按钮权限与单据状态联动。

## 3. 核心概念

### 3.1 业务对象

业务对象是低代码平台的领域资产，例如客户、联系人、商机、合同、离职申请。业务对象负责定义字段、表单、列表、详情、关系、权限和运行配置。

### 3.2 单据

单据是具备业务生命周期的业务对象记录。不是所有业务对象都必须是单据。

单据至少具备：

- 单据类型：例如商机单、合同审批单、离职申请单。
- 单据编号：可按规则生成，支持人工录入或系统生成。
- 单据状态：草稿、已提交、流程中、已通过、已驳回、已撤回、已关闭。
- 发起人和归属人：用于流程、消息和数据权限。
- 可执行动作：保存、提交、撤回、发起流程、查看流程、审批结果查看。
- 状态流转规则：哪些状态可编辑、可删除、可发起流程、可触发消息。

商机管理中的“商机录入”可以是普通记录；当它需要审批、编号、状态流转和结果回写时，它就是“商机单据”。

### 3.3 流程引擎

流程引擎是唯一承载审批流转的底座。不再单独定义“审批引擎”。

审批场景通过以下组合完成：

- 单据绑定已有流程模型。
- 配置单据字段到流程变量的映射。
- 单据操作或触发器发起流程。
- 流程回调更新单据状态。
- 流程节点和结果触发消息。

### 3.4 触发器

触发器是业务事件自动化，不替代流程引擎。它适合处理“当某件事发生时，自动做一件明确动作”：

- 新增记录时自动发起流程。
- 状态变更时发送消息。
- 流程通过后创建关联记录。
- 字段变更时更新汇总字段。
- 到期或逾期时生成提醒。

本阶段触发器只做低复杂度规则。复杂串并行审批、多人任务、节点条件、超时等仍交给 Flowable 流程引擎。

### 3.5 报表数据展示

报表展示不是独立“报表中心”的堆入口，而是业务套件和业务对象的看板能力。业务对象可以配置统计指标，CRM 套件可以配置组合看板。

### 3.6 消息推送

消息推送优先打通站内消息。第三方平台通道保留为 `WECHAT_WORK`、`FEISHU`、`DINGTALK` 适配器扩展点，返回明确 TODO 状态并记录日志，不保存明文密钥。

## 4. 范围

### 4.1 本阶段必须完成

- 建立“单据”概念和配置入口，至少支持商机单据和离职申请单据两类示例。
- 将审批能力并入流程引擎，不再展示或新增独立审批引擎。
- 将导入导出保留为业务对象运行页能力，不再展示或统计为独立引擎。
- 移除移动端中心和集成中心的可见菜单入口；代码可先保留但不作为本阶段产品入口。
- 对象设计器中补齐“流程与自动化”配置：流程绑定、变量映射、发起方式、状态回写、消息动作。
- 触发器配置从 JSON 输入升级为业务化结构配置，保留高级 JSON 作为开发者模式。
- 修复流程变量映射字段协议，统一使用 `formField`/`flowVariable` 或在后端兼容旧的 `field`/`variable`。
- 商机录入后可通过按钮或触发器发起已有流程。
- 流程实例和单据记录建立关联，可查询单据当前流程状态。
- 流程通过、驳回、撤回等结果能回写单据状态并发布业务事件。
- 审批结果可通过站内消息推送给发起人、负责人或指定角色。
- 企微、飞书、钉钉只保留通道定义、配置引用、适配器接口和 TODO 状态。
- 报表看板支持按业务场景配置指标：商机阶段分布、商机金额汇总、审批结果统计、趋势统计。
- 数据权限和按钮权限进入单据运行态，控制列表可见范围和行操作按钮。
- 触发器和流程相关接口补齐权限注解、操作日志、分页参数和 XML SQL 规范。

### 4.2 本阶段不做

- 不重写 Flowable 流程引擎。
- 不重写动态 CRUD 运行时。
- 不重写消息中心。
- 不实现完整复杂规则引擎。
- 不实现企微、飞书、钉钉真实 API 调用。
- 不建设移动端中心、集成中心或新的门户中心。
- 不把导入导出从 `AiCrudPage` / `DynamicCrudExcelService` 拆成新引擎。
- 不让业务用户直接编辑完整 JSON、Schema、表名、`configKey`。

## 5. 功能点

### 5.1 产品入口收敛

- [ ] 应用中心首页不再突出“引擎中心”作为中心化入口，改为按业务套件和业务对象进入配置。
- [ ] 引擎中心如保留，应只展示流程、触发器、消息、权限、报表五类底座能力。
- [ ] 引擎中心不得展示审批引擎和导入导出引擎。
- [ ] 移动端中心和集成中心菜单设为不可见或移除资源，前端入口不再出现。
- [ ] 移动和集成相关旧页面如果暂不删除，必须不在应用中心和菜单中暴露。

### 5.2 单据配置

- [ ] 业务对象设计器新增“单据设置”区域。
- [ ] 可选择当前对象是否启用单据模式。
- [ ] 单据模式启用后配置单据名称、编号规则、状态字段、发起人字段、负责人字段。
- [ ] 默认状态集：草稿、已提交、流程中、已通过、已驳回、已撤回、已关闭。
- [ ] 单据状态支持映射到现有字段，不强制新建字段；但发布检查必须保证字段存在。
- [ ] 单据配置进入发布检查，未配置状态字段或主流程时给出阻断项或警告项。
- [ ] 单据详情展示流程状态、触发器执行日志和消息推送记录摘要。

### 5.3 流程绑定与变量映射

- [ ] 对象可绑定一个默认流程和多个条件流程。
- [ ] 流程选择必须来自已发布流程模型。
- [ ] 变量映射支持从单据字段选择源字段，填写或选择流程变量名。
- [ ] 映射协议统一为 `formField` 和 `flowVariable`；保存时对旧协议 `field` 和 `variable` 做兼容转换。
- [ ] 流程标题支持模板，例如 `${opportunityName}-商机审批`。
- [ ] 流程业务键统一为 `${objectCode}:${recordId}`。
- [ ] 发起流程前校验记录已保存、流程已发布、变量映射字段存在。
- [ ] 发起成功后写入单据流程关联记录，并更新单据状态为“流程中”。
- [ ] 流程结束后按结果更新单据状态为“已通过”或“已驳回”。

### 5.4 发起流程入口

- [ ] 对象设计器的自定义操作支持“发起流程”，不再使用“发起审批”作为底层动作名。
- [ ] 行操作按钮根据单据状态、按钮权限、流程绑定状态显示或隐藏。
- [ ] 未配置流程时，按钮展示“配置流程”下一步，不允许点击后报错。
- [ ] 触发器动作 `START_FLOW` 和手动按钮发起流程复用同一后端服务。
- [ ] 原 `/ai/business/approval/start` 不作为新页面调用入口；需要保留时仅作为兼容层转发到流程运行服务。

### 5.5 触发器场景化配置

- [ ] 触发器新增业务场景模板：新增记录后发起流程、状态变更后发消息、流程通过后创建记录、字段变更后更新字段、到期提醒。
- [ ] 事件类型支持记录创建、记录更新、记录删除、状态变更、字段变更、流程通过、流程驳回。
- [ ] 条件配置支持字段、操作符、目标值、AND/OR 分组。
- [ ] 动作配置支持发起流程、发送消息、创建记录、更新字段、Webhook TODO。
- [ ] 创建记录动作必须支持源字段到目标字段映射，可用于“离职审批通过后自动生成离职交接记录”。
- [ ] 更新字段动作必须通过动态 CRUD 安全更新指定记录，不允许拼接未校验字段。
- [ ] Webhook 动作本阶段返回 TODO 状态并写日志，不实际请求外部地址。
- [ ] 触发器执行日志展示跳过、成功、失败、TODO 状态和错误原因。

### 5.6 消息推送

- [ ] 站内消息推送复用现有消息中心。
- [ ] 接收人规则支持发起人、负责人、记录创建人、当前流程处理人、指定用户、指定角色、指定部门。
- [ ] 消息模板参数可引用单据字段和流程结果。
- [ ] 触发器发送消息动作必须记录推送结果。
- [ ] 企微、飞书、钉钉通道仅保留配置引用、适配器接口和 TODO 状态。
- [ ] 第三方通道不保存明文密钥，不在日志输出 Token、Secret、Webhook URL 中的敏感参数。

### 5.7 报表展示

- [ ] 业务对象可配置统计指标：计数、金额求和、状态/阶段分布、趋势、流程结果分布。
- [ ] CRM 商机默认指标包括商机总数、阶段分布、预计金额分、审批通过/驳回数量、本月新增。
- [ ] 报表字段必须来自模型字段或单据状态字段，不允许用户输入未校验 SQL。
- [ ] 金额字段统一以分为单位展示，前端负责格式化为元。
- [ ] 报表页面从业务套件或对象进入，不再作为泛化中心堆入口。

### 5.8 权限闭环

- [ ] 数据权限继续复用系统数据权限和业务对象模型策略。
- [ ] 按钮权限必须覆盖保存、删除、提交、撤回、发起流程、查看流程、触发器执行、报表查看。
- [ ] 业务对象设计器展示当前按钮权限缺口。
- [ ] 运行页行操作按钮在前端按权限隐藏，后端接口仍必须校验权限。
- [ ] 涉及状态流转的接口必须记录操作日志。

### 5.9 业务示例验收

- [ ] 商机录入：新增商机单据后可进入草稿或已提交状态。
- [ ] 商机审批：可选择已有流程并映射商机名称、客户、金额、阶段、负责人等变量。
- [ ] 自动触发：可配置“新增商机且预计金额大于阈值时自动发起流程”。
- [ ] 审批结果推送：流程通过或驳回后发送站内消息给发起人和负责人。
- [ ] 数据回写：流程通过后商机状态变为已通过，驳回后变为已驳回。
- [ ] 统计展示：商机看板显示阶段分布、金额汇总、审批结果分布。
- [ ] 离职申请示例：新增离职申请满足条件后自动发起流程，部门主管审批后流转到人事，人事通过后自动生成离职交接记录。

## 6. 业务规则

- “审批”是流程场景，不是独立引擎。
- “导入导出”是对象运行能力，不是独立引擎。
- 单据是业务对象记录的生命周期增强，不替代业务对象。
- 未启用单据模式的对象仍可作为普通 CRUD 使用。
- 单据状态字段必须明确，触发器不得依赖猜测字段。
- 发起流程必须绑定业务对象、记录 ID、业务键和变量快照。
- 触发器不得绕过数据权限直接读取或修改非授权数据。
- 触发器动作失败不能影响原始 CRUD 事务提交，除非该触发器被显式配置为同步阻断型。
- 第三方消息通道本阶段只允许 TODO 状态和配置引用，不允许明文密钥。
- 所有内置数据 `tenant_id=1`。
- 所有查询类 SQL 必须写在 Mapper XML 中。
- 分页参数统一使用 `pageNum` 和 `pageSize`。
- URL 占位符统一使用 `:id` 格式。

## 7. 数据变更

| 操作 | 表名 | 字段/索引 | 说明 |
|------|------|-----------|------|
| 新增 | `ai_business_document_config` | `tenant_id`, `suite_code`, `object_code`, `config_key`, `document_name`, `document_no_rule`, `status_field`, `starter_field`, `owner_field`, `default_flow_key`, `status_mapping`, `options` | 业务对象单据模式配置 |
| 新增 | `ai_business_flow_instance_link` | `tenant_id`, `object_code`, `record_id`, `business_key`, `flow_model_key`, `process_instance_id`, `flow_status`, `start_user_id`, `start_time`, `end_time`, `result`, `variables_snapshot` | 单据记录与流程实例关联 |
| 扩展 | `ai_business_trigger` | 可选增加 `scenario_type`, `blocking_mode`, `developer_mode` | 支持场景模板、同步阻断和开发者高级模式 |
| 扩展 | `ai_business_trigger_log` | 可选增加 `todo_code`, `correlation_id` | 标识第三方 TODO 动作和关联流程/消息 |
| 复用 | `ai_business_message_channel` | `channel_config_ref`, `channel_type`, `status` | 保留企微、飞书、钉钉通道定义，不存明文密钥 |
| 复用 | `ai_business_binding` | `binding_type=FLOW/REPORT/MESSAGE/PERMISSION/TRIGGER` | 不再新增 `APPROVAL` 和 `IMPORT_EXPORT` 作为业务引擎类型 |
| 调整 | `sys_resource` | 移动端中心、集成中心、审批引擎、导入导出引擎相关菜单或按钮权限 | 新脚本隐藏或废弃旧入口，保留必要兼容权限 |

Flyway 要求：

- 新脚本版本必须大于当前最新版本。
- `CREATE TABLE IF NOT EXISTS`。
- 新增列前检查 `information_schema`。
- 内置数据和资源 `tenant_id=1`。
- 菜单隐藏或权限废弃必须用新脚本，不修改已执行历史脚本。

## 8. 接口变更

| 操作 | 接口 | 方法 | 变更内容 |
|------|------|------|----------|
| 新增 | `/ai/business/document/config/{objectId}` | GET | 查询对象单据配置 |
| 新增 | `/ai/business/document/config/{objectId}` | PUT | 保存对象单据配置 |
| 新增 | `/ai/business/document/{objectCode}/{recordId}/runtime` | GET | 查询单据状态、流程状态、可执行动作 |
| 新增 | `/ai/business/flow/binding/{objectCode}` | GET/PUT | 查询和保存对象流程绑定 |
| 新增 | `/ai/business/flow/start` | POST | 从单据记录手动发起流程 |
| 新增 | `/ai/business/flow/status/{objectCode}/{recordId}` | GET | 查询单据流程状态 |
| 新增 | `/ai/business/flow/callback` | POST | 接收流程结束或节点事件并回写单据 |
| 调整 | `/ai/business/trigger/page` | GET | 分页查询改为 Mapper XML SQL，支持场景筛选 |
| 调整 | `/ai/business/trigger` | POST/PUT | 动作配置统一变量映射协议，兼容旧字段 |
| 新增 | `/ai/business/trigger/scenario-templates` | GET | 查询触发器场景模板 |
| 调整 | `/ai/business/engine/summary` | GET | 移除审批引擎、导入导出引擎统计 |
| 废弃 | `/ai/business/approval/runtime` | GET | 不作为新页面调用入口，兼容期内转流程运行状态 |
| 废弃 | `/ai/business/approval/start` | POST | 不作为新页面调用入口，兼容期内转流程发起 |

接口安全要求：

- 涉及保存、发起流程、状态回写、触发器启停必须加 `@SaCheckPermission`。
- 保存类接口按项目加密链路需要补齐 `@ApiDecrypt`，查询敏感配置需要 `@ApiEncrypt`。
- 流程回调必须校验来源或签名，不能裸露为任意状态修改接口。

## 9. 前端影响范围

| 文件 | 变更方向 |
|------|----------|
| `forge-admin-ui/src/views/app-center/index.vue` | 入口收敛，不突出移动端中心、集成中心；业务对象卡片增加单据/流程状态摘要 |
| `forge-admin-ui/src/views/app-center/engines.vue` | 移除审批引擎、导入导出引擎；优先展示流程、触发器、消息、权限、报表底座状态 |
| `forge-admin-ui/src/views/app-center/mobile.vue` | 本阶段不暴露入口；可保留文件但菜单隐藏 |
| `forge-admin-ui/src/views/app-center/integration.vue` | 本阶段不暴露入口；第三方推送从消息通道和触发器动作中体现 |
| `forge-admin-ui/src/views/app-center/trigger.vue` | 触发器场景化配置、修复变量映射协议、补齐日志状态展示 |
| `forge-admin-ui/src/views/app-center/stats-dashboard.vue` | 从通用统计升级为业务场景指标配置和展示 |
| `BusinessPermissionFlowPanel.vue` | 从“数据权限”扩展为单据状态、流程绑定、按钮权限摘要 |
| `BusinessActionDesigner.vue` | “发起审批”改为“发起流程”，支持已发布流程选择、变量映射和按钮权限 |
| `forge-admin-ui/src/api/business-app.js` | 新增 document/flow/scenario 接口，废弃 approval 新调用 |

## 10. 后端影响范围

| 类/模块 | 变更方向 |
|---------|----------|
| `BusinessFlowService` | 升级为单据流程运行服务，负责流程发起、变量映射、状态查询、流程实例关联 |
| `BusinessApprovalRuntimeService` | 废弃或兼容转发到流程运行服务，不再模拟返回 |
| `BusinessApprovalController` | 废弃新调用入口，兼容期转发 |
| `BusinessTriggerService` | 查询类 SQL 下沉 Mapper XML，新增场景模板与配置校验 |
| `BusinessTriggerExecutor` | 实现创建记录、更新字段动作；Webhook 保持 TODO 状态和日志 |
| `BusinessEventPublisher` | 单据状态字段标准化，增加流程结果事件 |
| `BusinessEngineSummaryService` | 移除 `APPROVAL`、`IMPORT_EXPORT` 引擎统计 |
| `BusinessStatsService` | 增加指标配置校验、金额字段聚合、流程结果分布 |
| `BusinessPermissionService` | 增加按钮权限和单据动作权限摘要 |
| `DynamicCrudController` | 保持事件发布，必要时补充单据运行上下文 |

## 11. 测试策略

- **后端编译**：`cd forge && mvn clean compile`
- **前端构建**：`cd forge-admin-ui && pnpm build`
- **数据库验证**：执行新增 Flyway 脚本，检查表、列、索引、菜单可见性。
- **接口验证**：登录后依次验证单据配置、流程绑定、发起流程、触发器、消息、报表接口。
- **业务闭环验证**：用商机单据跑通录入、发起流程、流程结果回写、消息推送、报表统计。
- **回归验证**：普通动态 CRUD 不启用单据模式时，新增/编辑/删除、导入导出不受影响。
- **权限验证**：无发起流程权限的用户看不到按钮，直接调接口返回无权限。
- **触发器验证**：变量映射使用新旧协议都能转换，执行日志能区分成功、失败、跳过、TODO。

## 12. 风险与关注点

- ⚠️ 状态流转风险：流程回调会修改业务单据状态，必须有幂等校验和操作日志。
- ⚠️ 权限风险：按钮隐藏只是体验，后端发起流程、状态回写、触发器执行必须校验权限或系统上下文。
- ⚠️ 数据一致性风险：触发器异步执行失败不能让用户误以为流程或消息已成功。
- ⚠️ 变量映射风险：当前前端 `field/variable` 与后端 `formField/flowVariable` 不一致，必须优先修复或兼容。
- ⚠️ 服务边界风险：不要重新造审批引擎；所有审批语义必须回到 Flowable 流程。
- ⚠️ 菜单收敛风险：移动端中心、集成中心隐藏后，历史菜单资源和角色权限需要新脚本处理，不能改历史脚本。
- ⚠️ 第三方推送风险：TODO 通道必须明确展示“待实现”，避免业务误以为已经真实发送企微、飞书、钉钉。

## 13. 待澄清

- [x] 移动端中心、集成中心首期仅隐藏菜单入口，保留历史前端页面和路由兼容。
- [x] 单据状态字段默认建议采用 `documentStatus` / `document_status`，同时允许高级模式映射到已有字段；保存和发布检查必须校验字段存在。
- [x] 商机审批流程首期必须真实启动已有 Flowable 流程，不再接受模拟审批 ID。
- [x] 流程回调采用新增业务流程回调接口作为统一入口，后续由 Flowable 监听器或 FlowClient 回调桥接进入。
- [x] 商机示例首批字段固定为客户、商机名称、阶段、预计金额、预计成交日期、负责人、审批状态。
- [x] 企微、飞书、钉钉本阶段严格只返回 TODO 日志，不做任何网络请求。

## 14. 技术决策

- 审批能力归入流程引擎，不再保留独立审批引擎。
- 导入导出归入动态 CRUD 运行页，不再保留独立导入导出引擎。
- 单据配置作为业务对象的增强配置，不替代低代码模型和 `ai_crud_config`。
- 触发器作为事件自动化，不承担复杂流程编排。
- 第三方推送通道只保留适配器接口和 TODO 状态，后续由独立变更实现。
- 业务对象设计器继续作为主配置入口，减少中心化配置页。
- 查询类 SQL 必须写 Mapper XML，后续实现时先处理 `BusinessTriggerService` 的分页查询。

## 15. 执行日志

| Task | 状态 | 实际改动文件 | 备注 |
|------|------|--------------|------|
| Spec | completed | `code-copilot/changes/lowcode-app-full-loop-optimization/spec.md` | 已创建改造分析和需求 Spec |
| Task 0 | completed | `spec.md`, `tasks.md` | 确认 Phase 0 边界，保留历史页面兼容，冻结三方通道 TODO 边界 |
| Task 1 | completed | `forge/db/migration/V1.0.47__add_business_document_flow_closure.sql` | 新增单据配置表、流程实例关联表、触发器扩展字段，隐藏移动端/集成中心菜单 |
| Task 2 | completed | `AiBusinessDocumentConfig.java`, `AiBusinessFlowInstanceLink.java`, `BusinessDocumentConfigMapper.java`, `BusinessFlowInstanceLinkMapper.java`, `BusinessDocumentConfigMapper.xml`, `BusinessFlowInstanceLinkMapper.xml`, `BusinessDocumentConfigDTO.java`, `BusinessFlowStartDTO.java`, `BusinessFlowCallbackDTO.java`, `BusinessDocumentConfigVO.java`, `BusinessDocumentRuntimeVO.java`, `BusinessFlowRuntimeVO.java` | 建立单据和流程关联 Java 数据模型 |
| Task 3 | completed | `BusinessDocumentController.java`, `BusinessDocumentConfigService.java`, `BusinessObjectPublishService.java`, `BusinessObjectDesignerService.java`, `BusinessObjectDesignerVO.java` | 提供单据配置查询/保存，设计器带出单据配置，发布检查校验单据字段和默认流程 |
| Task 4 | completed | `BusinessDocumentRuntimeService.java`, `BusinessDocumentController.java`, `BusinessPermissionService.java` | 提供单据运行态查询，普通对象返回未启用，单据对象返回状态、流程摘要和可执行动作 |
| Task 5 | completed | `BusinessFlowController.java`, `BusinessFlowService.java`, `BusinessTriggerController.java`, `BusinessFlowBindingDTO.java`, `BusinessFlowBindingVO.java` | 新增 `/ai/business/flow/binding/{objectCode}`，旧触发器路径兼容转发，变量映射统一输出 `formField`/`flowVariable` |
| Task 6 | completed | `BusinessFlowController.java`, `BusinessFlowService.java`, `BusinessFlowInstanceLinkMapper.java`, `BusinessFlowInstanceLinkMapper.xml`, `BusinessDocumentRuntimeService.java`, `DynamicCrudService.java`, `BusinessTriggerExecutor.java`, `BusinessFlowRuntimeVO.java` | 新增 `/ai/business/flow/start` 和 `/status/{objectCode}/{recordId}`，真实调用 `FlowClient.startProcess`，写入流程实例关联并回写单据状态为流程中 |
| Task 7 | completed | `BusinessFlowController.java`, `BusinessFlowService.java`, `BusinessEvent.java`, `BusinessEventPublisher.java`, `BusinessApprovalRuntimeService.java`, `BusinessApprovalController.java`, `BusinessFlowCallbackDTO.java`, `V1.0.49__add_business_flow_callback_permission.sql` | 新增流程回调接口，按通过/驳回/撤回幂等回写单据状态并发布流程结果事件，旧审批发起接口转发到流程运行服务 |
| Task 8 | completed | `BusinessTriggerMapper.java`, `BusinessTriggerMapper.xml`, `BusinessTriggerLogMapper.java`, `BusinessTriggerLogMapper.xml`, `BusinessTriggerService.java`, `BusinessTriggerController.java`, `AiBusinessTrigger.java`, `AiBusinessTriggerLog.java` | 触发器和执行日志分页查询下沉 Mapper XML，分页继续使用 `pageNum/pageSize`，支持 `scenarioType` 筛选并补齐 TODO/关联日志字段 |
| Task 9 | completed | `BusinessTriggerScenarioTemplateVO.java`, `BusinessTriggerController.java`, `BusinessTriggerService.java`, `BusinessTriggerExecutor.java`, `trigger.vue`, `business-app.js` | 新增触发器场景模板接口，`START_FLOW` 变量映射统一保存 `formField`/`flowVariable`，执行端兼容历史 `field`/`variable` |
| Task 10 | completed | `BusinessTriggerExecutor.java`, `DynamicCrudService.java`, `BusinessTriggerService.java`, `BusinessEventPublisher.java`, `AiBusinessTriggerLog.java` | 创建记录和更新字段动作走受控内部 CRUD 方法，Webhook 返回 `TODO` 并写入 `WEBHOOK_NOT_IMPLEMENTED`、`correlationId` |
| Task 11 | completed | `BusinessMessageChannelMapper.java`, `BusinessMessageChannelMapper.xml`, `BusinessMessageChannelService.java`, `BusinessMessageChannelStatus.java`, `BusinessTriggerExecutor.java` | 站内信继续调用消息中心，企业微信/飞书/钉钉/Webhook 通道只返回 TODO；接收人规则扩展到 STARTER/OWNER/CREATOR/USERS/ROLES/DEPTS/ALL，日志不输出通道密钥 |
| Task 12 | completed | `BusinessStatsController.java`, `BusinessStatsService.java`, `BusinessStatsMapper.java`, `BusinessStatsMapper.xml`, `BusinessStatsMetricQueryDTO.java`, `BusinessStatsMetricVO.java` | 新增 `/ai/business/stats/{configKey}/metrics`，指标 SQL 下沉 XML，支持总量、今日、本月、状态/阶段分布、金额分值汇总和流程结果分布，非模型字段直接业务报错 |
| Task 13 | completed | `BusinessPermissionService.java`, `BusinessPermissionMapper.java`, `BusinessPermissionMapper.xml`, `BusinessPermissionSummaryVO.java`, `BusinessObjectActionService.java`, `BusinessObjectDesignerController.java`, `BusinessObjectPublishService.java`, `V1.0.50__add_business_stats_and_action_permissions.sql` | 增加单据动作权限摘要、动作保存权限标识校验、发布检查权限缺口提示，并补齐保存/提交/撤回/触发器执行/报表查看权限资源 |
| Task 14 | completed | `BusinessEngineSummaryService.java`, `engines.vue`, `index.vue`, `suite.[suiteCode].vue`, `ObjectCard.vue`, `app-entry.vue`, `AppEditorDrawer.vue` | 应用中心入口回到业务套件/对象主链路，后端和前端引擎摘要移除独立审批/导入导出类型，API 入口不再跳转独立集成中心 |
| Task 15 | completed | `business-app.js`, `BusinessDocumentPanel.vue`, `BusinessObjectDesignerShell.vue`, `object-designer.[objectCode].vue` | 新增单据配置/运行态和流程新协议前端 API，设计器增加单据设置面板，字段下拉来自当前业务对象字段 |
| Task 16 | completed | `BusinessFlowBindingPanel.vue`, `BusinessActionDesigner.vue`, `BusinessObjectDesignerShell.vue`, `object-designer.[objectCode].vue` | 设计器增加流程与自动化面板，自定义操作新增“发起流程”并保存 `START_FLOW`、`formField`/`flowVariable` 映射 |
| Task 17 | completed | `trigger.vue`, `TriggerConditionBuilder.vue`, `TriggerActionConfigPanel.vue`, `business-app.js` | 触发器接入场景模板、条件构造器和动作结构化配置，开发者模式保留原始 JSON 输入 |
| Task 18 | completed | `stats-dashboard.vue`, `BusinessMetricPanel.vue`, `suite.[suiteCode].vue`, `business-app.js` | 业务看板改用 `/ai/business/stats/{configKey}/metrics`，按对象字段推断状态/阶段/金额指标，金额分值前端格式化为元 |
| Task 19 | completed | `V1.0.51__seed_crm_opportunity_document_flow.sql`, `DynamicCrudController.java`, `DynamicCrudService.java`, `BusinessTriggerExecutor.java` | 初始化 CRM 商机单据状态字段、单据配置、流程绑定、自动发起流程触发器、审批结果站内信触发器和商机看板入口；新增记录事件改用创建后数据以携带 `recordId` |
| Task 20 | completed | `V1.0.52__seed_leave_document_flow_demo.sql`, `BusinessTriggerExecutor.java` | 初始化 HR 离职申请与离职交接样板对象、运行配置、单据流程绑定、日期条件自动发起流程和流程通过后创建交接记录触发器 |
| Task 21 | completed | `V1.0.53__align_flow_template_logic_delete_column.sql`, `V1.0.54__patch_opportunity_flow_dept_manager_mapping.sql`, `BusinessFlowService.java`, `BusinessTriggerExecutor.java`, `MessageTemplateEngine.java`, `execution-log.md` | 完成 Flyway 实跑、`leave_multi` 流程部署、商机手动发起流程、核心接口、后端 package、前端 build 和 `git diff --check` 验证 |
| Task 22 | completed | `spec.md`, `tasks.md`, `execution-log.md`, `.opencode/memory/pitfalls.md`, `.opencode/memory/decisions.md` | 回填 Phase 8 执行记录、审查结论、HARD-GATE 结论，并沉淀 Flyway 占位符与 Flowable 变量映射踩坑 |

### Phase 7 收尾验证

- `git diff --check` 通过。
- `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests` 通过（JDK 17）。
- `V1.0.51__seed_crm_opportunity_document_flow.sql` 与 `V1.0.52__seed_leave_document_flow_demo.sql` 内嵌 JSON 片段离线解析通过。
- 本地 `127.0.0.1:3407` MySQL 未运行，Flyway 迁移实跑验证留到 Phase 8 或数据库恢复后执行。

### Phase 8 收尾验证

- `git diff --check` 通过。
- `mvn -pl forge-admin-server -am package -DskipTests` 通过，后端主应用及依赖模块 package 成功。
- `pnpm build` 通过；仅保留既有 UnoCSS 图标加载、CSS `//` 注释、动态/静态导入混用和 chunk size 警告。
- Flyway 已在 dev 库执行到 `1.0.54`，`V1.0.53` 补齐 `sys_flow_template.del_flag`，`V1.0.54` 补齐商机样板 `deptManager` 流程变量映射。
- 已创建并部署 `leave_multi` Flowable 流程定义，模型 ID `e7d55f0a4087ec5dc0189784a00204ad`，部署 ID `dcbda981-5e14-11f1-a0fd-d67ed5f8e875`，流程定义 version `1`。
- `/ai/business/flow/start` 对 `OPPORTUNITY/10` 返回 `code=200`，流程实例 `602ad5c2-5e15-11f1-a0fd-d67ed5f8e875`，单据状态回写为 `IN_PROCESS`，流程待办进入 `部门经理审批`。
- `/ai/business/document/config/1910000000000000104`、`/ai/business/document/OPPORTUNITY/10/runtime`、`/ai/business/flow/status/OPPORTUNITY/10`、`/ai/business/trigger/page?pageNum=1&pageSize=10`、`/ai/business/trigger/scenario-templates`、`/ai/business/stats/crm_opportunity/metrics` 均返回 `code=200`。
- 详细执行命令和结果已记录到 `code-copilot/changes/lowcode-app-full-loop-optimization/execution-log.md`。

## 16. 审查结论

Phase 8 自检通过。当前变更已完成 Spec 定义的低代码业务单据闭环最小验证，包括数据库迁移、流程部署、商机单据发起流程、运行态状态查询、触发器分页/模板和业务指标接口验证。

遗留项：

- 前端构建仍存在既有告警：UnoCSS 图标加载、CSS `//` 注释、动态/静态导入混用和 chunk size 提示；未阻断构建。
- 企微、飞书、钉钉仍按 Spec 保持 TODO 通道，不在本阶段实现真实外部推送。

## 17. 确认记录（HARD-GATE）

- **确认时间**：2026-06-02 08:00（Asia/Shanghai）
- **确认人**：Codex（按本轮用户指令自动执行 Phase 8）
- **结论**：Phase 0 边界已冻结；Phase 1-2 已落地单据配置、流程实例关联、单据运行态和流程绑定后端协议；Phase 3 已完成真实发起流程、实例关联、单据状态回写、流程回调幂等处理和旧审批入口兼容转发；Phase 4 已完成触发器 XML 分页查询、场景模板、变量映射协议兼容、创建记录/更新字段动作和 Webhook TODO 日志闭环；Phase 5 已完成站内消息与三方 TODO 通道、业务统计指标后端能力、单据动作权限摘要和权限资源补齐；Phase 6 已完成应用中心入口收敛、单据设置、流程自动化、自定义操作、触发器结构化配置和业务报表看板前端接入；Phase 7 已补齐 CRM 商机和 HR 离职申请示例初始化；Phase 8 已完成 Flyway、后端 package、前端 build、核心接口、商机主流程和文档归档验证。本变更进入可 review/archive 状态。
