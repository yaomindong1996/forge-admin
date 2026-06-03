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

## 18. Phase 9 BUG 增补：应用入口、单据流程配置和自动化体验优化
> status: proposed
> created: 2026-06-02
> priority: P0
> source: 低代码应用运行态用户反馈

### 18.1 背景

Phase 8 已经跑通低代码业务单据的最小闭环，但实际配置和使用时仍有明显断点：

- 管理端同步出的应用菜单点击后会先进入 `/app-center/app/{appId}` 中转页，再跳到 `/ai/crud-page/{configKey}`，当前菜单无法稳定保持选中态，父级“应用总览”和套件目录会短暂闪动。
- 单据设置和流程自动化虽然已有表单，但仍偏技术配置，用户需要手填编号变量、状态值、流程变量和标题模板，不知道可填什么。
- 单据设置里的“默认流程”和流程与自动化里的“流程模型”概念重叠，用户不知道两处选流程有什么区别。
- 业务单据作为应用入口挂载时，用户预期点击后进入填报页面，而不是普通管理列表。
- 手动发起流程时，配置了 `MANUAL` 或 `BOTH` 后，运行页没有自动出现可用的“发起流程”按钮，也没有自动绑定按钮事件。
- 全局新增触发器时，如果没有先选择业务对象，会提交到后端并报“业务对象编码不能为空”。
- 单据配置、流程配置、触发器配置、发布检查和试运行之间仍是分散页面，串联不流畅。

本阶段目标不是新增独立中心，而是把“配置完成即可使用”补齐为产品级体验。

### 18.2 目标

- 应用入口菜单点击后，当前动态应用菜单稳定选中，不再闪动 `/app-center` 或父级目录。
- 单据设置、流程与自动化、触发器配置形成一个向导化主链路，减少跨页面跳转和重复配置。
- 编号规则、状态映射、流程变量映射、流程标题模板从“手填文本”升级为“选择变量、插入变量、实时预览、必要时高级输入”。
- 单据主流程只配置一次，单据运行、手动发起和触发器自动发起复用同一套流程绑定。
- 单据填报类应用入口支持直接打开新增填报页面，普通管理类入口仍打开列表页。
- 手动发起流程按钮由运行态根据单据配置、流程绑定、状态和权限自动生成。
- 触发器新增时必须先确定业务对象，前端在提交前给出明确阻断，不把空 `objectCode` 交给后端。

### 18.3 非目标

- 不重写 `AiCrudPage` 为新的运行时框架。
- 不新建“审批中心”或“自动化中心”。
- 不实现完整 BPMN 静态分析器；本阶段只抽取低代码配置需要的流程变量候选项。
- 不实现复杂编号服务的全量规则引擎；本阶段先支持内置变量、序列号和预览校验。
- 不删除历史兼容字段和接口，旧配置需要能继续读取并迁移到新展示协议。

### 18.4 现状定位

- `BusinessAppService#syncManagementMenu` 当前把管理端应用菜单固定注册到 `/app-center/app/{id}`，组件为 `app-center/app-entry`。
- `app-entry.vue` 对 `RUNTIME` 入口调用 `businessAppOpenInfo` 后执行 `router.replace(info.targetUrl)`，最终路由变成 `/ai/crud-page/{configKey}`。
- `useMenu.activeKey` 主要通过当前 `route.path` 和菜单 path 匹配选中项，最终运行页没有与动态应用菜单 path 相同的菜单项，因此选中态丢失。
- `BusinessDocumentPanel.vue` 的编号规则是普通输入框，状态映射是原始值输入。
- `BusinessFlowBindingPanel.vue` 和 `TriggerActionConfigPanel.vue` 的流程变量名仍需要手填。
- `AiCrudPage.vue` 当前只内置处理新增、编辑、详情、删除、路由跳转和外链，自定义 `START_FLOW` 没有运行态执行分支。
- `trigger.vue` 全局新增触发器时允许空对象进入弹窗，后端 `BusinessTriggerService#validateTrigger` 会抛出“业务对象编码不能为空”。

### 18.5 功能要求

#### 18.5.1 动态应用菜单选中态

- [ ] 管理端同步出的业务应用菜单必须有稳定的最终打开路由，不允许只依赖 `/app-center/app/{appId}` 中转后丢失菜单归属。
- [ ] `RUNTIME` 菜单可以直接注册为 `/ai/crud-page/{configKey}`，并通过 query 或 meta 携带 `appId/menuKey/menuResourceId`；如果继续保留中转页，也必须在跳转后保留 active menu key。
- [ ] `BusinessAppOpenInfoVO` 需要返回当前入口的 `activeMenuKey`、`menuResourceId`、`targetRoute` 和 `runtimeOpenMode`，前端打开时按这些字段写入路由 query。
- [ ] `useMenu.activeKey` 需要优先识别 `route.query.menuKey`、`route.query.appId` 或 `route.meta.parentKey`，再按路径匹配。
- [ ] 点击应用菜单时，侧边栏只选中当前应用菜单；父级套件目录只保持展开，不显示为当前选中项。
- [ ] 浏览器刷新 `/ai/crud-page/{configKey}?appId=...&menuKey=...` 后，仍能恢复同一个应用菜单选中态。
- [ ] Tab 标题和浏览器标题使用业务应用名称优先，其次才是运行配置名称。

#### 18.5.2 应用入口打开模式

- [ ] 应用入口配置增加“运行态打开模式”：`LIST` 列表管理、`CREATE_FORM` 新增填报、`DETAIL` 详情查看。
- [ ] 普通业务管理入口默认 `LIST`；启用单据模式且入口名称/类型为填报类时默认建议 `CREATE_FORM`。
- [ ] `CREATE_FORM` 模式打开 `/ai/crud-page/{configKey}?mode=create&appId=...&menuKey=...` 后，运行页自动打开新增表单。
- [ ] 新增表单保存成功后，页面按配置选择停留在详情、回到列表或继续新增，默认回到列表并刷新数据。
- [ ] 如果对象未发布、运行配置缺失或单据状态字段缺失，入口页展示明确下一步，不进入空白表单。
- [ ] 外链、H5、IFRAME、API 入口继续沿用现有安全校验，不受本阶段影响。

#### 18.5.3 单据设置体验

- [ ] 编号规则从普通输入升级为模板编辑器，提供变量选择、点击插入、实时样例预览和错误提示。
- [ ] 内置编号变量至少包括：`${yyyy}`、`${yyyyMM}`、`${yyyyMMdd}`、`${HHmmss}`、`${seq}`、`${seq:4}`、`${suiteCode}`、`${objectCode}`、`${starter}`、`${deptCode}`、`${field:<fieldCode>}`。
- [ ] 编号变量以分组菜单或变量标签展示，用户选择后自动插入到光标位置，不要求记忆语法。
- [ ] 编号规则保存前校验未知变量、空序列、非法字符和长度风险。
- [ ] 状态映射改为结构化表格：标准状态、存储值、展示名称、标签类型、是否允许编辑、是否允许删除、是否允许发起流程。
- [ ] 状态字段如果绑定了字典，存储值优先从字典项选择；如果没有字典，允许使用默认状态值并提示建议维护字典。
- [ ] 提供“一键使用默认状态集”和“一键从字典生成映射”。
- [ ] 单据设置页采用工作台布局：左侧主配置、中间映射表、右侧发布检查摘要，不再使用松散堆叠的小卡片。
- [ ] 页面内所有保存按钮必须有 loading、防重复提交和保存后摘要刷新。

#### 18.5.4 单据主流程合并

- [ ] “单据设置里的默认流程”和“流程与自动化里的流程模型”合并为一个“主流程配置”。
- [ ] 主流程配置的事实来源优先使用 `ai_business_binding` 的 `binding_type=FLOW` 配置；`ai_business_document_config.default_flow_key` 仅保留兼容读取或同步快照。
- [ ] 单据设置页只展示主流程摘要和“去配置主流程”入口，不再提供第二个独立流程选择器。
- [ ] 流程与自动化页维护主流程模型、变量映射、标题模板、发起方式、条件流程和状态回写。
- [ ] 保存主流程后，同步更新单据配置摘要，避免两个页面显示不一致。
- [ ] 发布检查以主流程配置为准，提示“未配置主流程”“变量映射缺失”“发起方式未配置”“手动按钮缺失”等问题。

#### 18.5.5 流程变量映射和标题模板

- [ ] 选择流程模型后，系统自动加载流程变量候选项。
- [ ] 流程变量候选项需要从以下来源提取：BPMN 条件表达式中的 `${variable}`、审批人 SPEL 表达式、`flowable:assignee`/`candidateUsers`/`candidateGroups` 中的变量、动态表单字段、流程模板变量配置、流程引擎内置变量。
- [ ] 内置流程变量至少包括：`initiator`、`startUserId`、`businessKey`、`recordId`、`objectCode`、`deptId`、`deptManager`。
- [ ] 变量映射右侧不再是普通文本输入，默认使用流程变量下拉选择；高级模式允许新增自定义变量。
- [ ] 系统按字段编码、字段名称、常见别名自动推荐映射，例如 `amount`、`opportunityAmount`、`deptManager`。
- [ ] 标题模板编辑器支持点击插入单据字段和内置变量，并展示实时预览，例如“商机名称-商机审批-20260602”。
- [ ] 标题模板保存前校验未知变量；未知变量必须给出具体变量名和修复入口。
- [ ] 变量映射仍统一保存为 `formField` 和 `flowVariable`，旧协议 `field` 和 `variable` 继续兼容读取。

#### 18.5.6 手动发起流程按钮

- [ ] 当主流程发起方式为 `MANUAL` 或 `BOTH` 时，运行态自动生成“发起流程”按钮，不要求用户在自定义操作里手动添加。
- [ ] 自动按钮默认出现在行操作和详情页；是否展示受单据状态、流程状态、按钮权限和主流程配置共同控制。
- [ ] 状态为草稿、已提交且未存在进行中流程时可以发起；流程中、已通过、已驳回、已撤回、已关闭默认不展示或禁用。
- [ ] 点击按钮调用 `/ai/business/flow/start`，payload 至少包含 `objectCode`、`recordId`、`flowModelKey`、`titleTemplate` 或主流程绑定引用。
- [ ] 发起成功后刷新列表行、单据运行态、流程状态和操作按钮，不需要用户手动刷新。
- [ ] 自定义操作页保留“发起流程”覆盖配置，用于修改按钮文案、位置、确认文案和权限标识，但不得造成重复按钮。
- [ ] 后端仍必须校验 `ai:businessFlow:start` 权限和单据状态，前端隐藏不是安全边界。

#### 18.5.7 触发器新增体验

- [ ] 全局进入触发器页面时，新增触发器弹窗第一步必须选择业务对象；未选择时禁用后续字段或禁用保存。
- [ ] 从对象设计器或套件对象上下文进入触发器页面时，URL query 携带 `objectCode`，新增弹窗自动带入并锁定或默认选中当前对象。
- [ ] 业务对象选择后再加载字段、条件构造器和动作配置，避免字段下拉为空。
- [ ] 场景模板选择后自动填充事件类型、动作类型、默认条件和默认动作配置；如果缺少主流程、消息模板或目标对象，页面展示缺口提示。
- [ ] 前端提交前必须校验 `objectCode`、`triggerName`、`eventType`、`actionType`、`actionConfig`，不允许把空对象编码提交给后端。
- [ ] 后端错误仍保留，但前端错误文案应转成用户可处理的动作，例如“请先选择业务对象”。

#### 18.5.8 配置串联主链路

- [ ] 业务对象设计器增加“单据闭环配置”步骤条：单据设置 → 主流程 → 发起方式 → 自动化触发器 → 发布检查 → 试运行。
- [ ] 每一步展示完成状态、阻断项、警告项和下一步按钮。
- [ ] 单据设置保存后，如果主流程未配置，下一步直接进入主流程配置。
- [ ] 主流程保存后，如果发起方式包含 `TRIGGER` 或 `BOTH`，下一步进入触发器配置并自动带入对象和流程。
- [ ] 发布检查页汇总单据字段、编号规则、状态映射、主流程、变量映射、手动按钮、触发器、菜单入口和权限缺口。
- [ ] 试运行至少支持打开填报入口、创建一条草稿记录、手动发起流程、查看流程状态四个动作的入口。
- [ ] 所有配置页保持紧凑后台产品风格，避免大面积空白、重复说明、嵌套卡片和纯技术字段暴露。

### 18.6 数据与配置变更

| 类型 | 位置 | 变更 |
|------|------|------|
| 复用 | `ai_business_app.options` | 增加 `runtimeOpenMode`, `activeMenuKey`, `adminMenu.menuResourceId` 回显使用，不新增表字段 |
| 复用 | `ai_business_document_config.options` | 保存编号规则预览配置、状态动作策略和兼容标记 |
| 复用 | `ai_business_binding.binding_config` | 作为主流程配置事实来源，保存流程模型、变量映射、标题模板、发起方式和条件流程 |
| 复用 | `ai_business_object.designer_options` | 保存自定义按钮覆盖配置，自动按钮不强制写入此处 |
| 可选 | `sys_resource` | 如调整应用菜单 path 或组件，需要用新 Flyway 脚本修复已有动态应用菜单，不修改历史脚本 |

数据约束：

- 新增或修复菜单资源必须 `tenant_id=1`，并具备 `NOT EXISTS` 或按 `menuResourceId` 防重复保护。
- 已有应用入口如果已同步菜单，需要保留原角色授权，不得删除菜单后重建导致授权丢失。
- `ai_business_document_config.default_flow_key` 和 `ai_business_binding` 不一致时，以 `ai_business_binding` 为准，并在保存时回写兼容字段。

### 18.7 接口变更

| 操作 | 接口 | 方法 | 说明 |
|------|------|------|------|
| 调整 | `/ai/business/app/{id}/open-info` | GET | 返回 `activeMenuKey`, `menuResourceId`, `targetRoute`, `runtimeOpenMode` |
| 调整 | `/ai/business/app` | POST/PUT | 保存入口时校验并保存 `runtimeOpenMode`，同步菜单时保留菜单归属 |
| 新增 | `/ai/business/document/no-rule/tokens` | GET | 返回编号规则内置变量、说明、示例和适用范围 |
| 新增 | `/ai/business/document/no-rule/preview` | POST | 按对象、规则和样例数据返回编号预览和校验结果 |
| 调整 | `/ai/business/document/config/{objectId}` | GET/PUT | 单据配置返回主流程摘要，不再把 `defaultFlowKey` 作为唯一流程配置入口 |
| 调整 | `/ai/business/flow/binding/{objectCode}` | GET/PUT | 返回流程变量候选项摘要、自动映射建议和配置完整度 |
| 新增 | `/ai/business/flow/model/{modelKey}/variables` | GET | 返回流程变量候选项，供单据流程和触发器动作配置使用 |
| 调整 | `/ai/business/flow/start` | POST | 支持运行态自动按钮发起，返回流程实例和单据状态摘要 |
| 调整 | `/ai/business/trigger` | POST/PUT | 后端继续校验对象编码，前端提交前必须拦截空对象 |

接口安全要求：

- 新增保存和发起类接口必须按项目现有规则补齐 `@SaCheckPermission`。
- 查询流程变量候选项不得返回敏感表单数据或历史流程实例变量值，只返回变量定义和来源。
- 编号预览接口只使用样例数据或当前对象字段元数据，不生成真实编号，不占用序列号。

### 18.8 前端影响范围

| 文件 | 变更方向 |
|------|----------|
| `forge-admin-ui/src/composables/useMenu.js` | activeKey 支持 `menuKey/appId` 优先匹配，避免动态应用菜单失焦 |
| `forge-admin-ui/src/utils/menu-utils.js` | 保持路径匹配能力，必要时支持 query 中的菜单归属解析 |
| `forge-admin-ui/src/views/app-center/app-entry.vue` | 中转页保留菜单归属 query；RUNTIME 可直接 replace 到带 `menuKey` 的目标路由 |
| `forge-admin-ui/src/views/app-center/components/AppEditorDrawer.vue` | 增加运行态打开模式，保存到 `options.runtimeOpenMode` |
| `forge-admin-ui/src/views/ai/crud-page.vue` | 支持 `mode=create` 自动打开新增表单；向 `AiCrudPage` 传入单据运行态和自动流程按钮配置 |
| `forge-admin-ui/src/components/ai-form/AiCrudPage.vue` | 增加 `START_FLOW` 自定义动作执行分支和自动流程按钮渲染 |
| `BusinessDocumentPanel.vue` | 编号规则变量选择、预览、状态映射表格化，移除独立流程选择器 |
| `BusinessFlowBindingPanel.vue` | 主流程配置、流程变量候选项、自动映射建议、标题模板变量插入和预览 |
| `BusinessActionDesigner.vue` | 发起流程按钮变成覆盖配置，避免和自动按钮重复 |
| `trigger.vue` | 新增触发器前置业务对象选择、query 默认对象、提交前阻断 |
| `TriggerActionConfigPanel.vue` | START_FLOW 变量下拉选择，复用流程变量候选项和标题模板编辑器 |
| `BusinessObjectDesignerShell.vue` | 增加单据闭环配置步骤条和配置完成度摘要 |

### 18.9 后端影响范围

| 类/模块 | 变更方向 |
|---------|----------|
| `BusinessAppService` | 保存运行态打开模式，调整菜单同步时的 path 或菜单归属信息，保留已有授权 |
| `BusinessAppOpenService` | open-info 返回目标路由、菜单归属和打开模式 |
| `BusinessDocumentConfigService` | 单据配置与主流程绑定联动，兼容 `default_flow_key` |
| `BusinessFlowService` | 提供主流程完整度、变量候选项摘要、自动映射建议和运行态发起结果 |
| `BusinessTriggerService` | 场景模板补齐默认动作配置，错误文案保持明确 |
| `BusinessObjectPublishService` | 发布检查增加菜单选中态、运行态打开模式、自动按钮和触发器串联检查 |
| `FlowClient` 或业务侧适配服务 | 获取流程模型详情并解析变量候选项，避免前端直接解析完整 BPMN |
| `MenuRegisterAdapter` | 如菜单 path 变更，需要支持更新已有菜单且不丢角色授权 |

### 18.10 验收标准

- [ ] 配置 CRM 商机应用入口并同步为管理端菜单后，点击菜单直接进入商机运行页或填报页，当前商机菜单保持选中，父级只展开不闪动为选中态。
- [ ] 刷新运行页后，菜单选中态、Tab 标题和页面标题仍是当前业务应用。
- [ ] 单据设置中编号规则可以通过变量选择完成，不需要用户手记 `${yyyyMMdd}`、`${seq}` 等语法。
- [ ] 状态映射可以通过默认状态集或字典生成，不再要求逐项手填原始状态值。
- [ ] 单据设置页不再独立选择默认流程，只展示主流程摘要和下一步入口。
- [ ] 选择流程模型后，变量映射右侧自动出现流程变量候选项，并能一键应用推荐映射。
- [ ] 流程标题模板可以插入字段变量并实时预览。
- [ ] 主流程发起方式为手动时，商机运行页行操作或详情页自动出现“发起流程”按钮；点击后真实调用 `/ai/business/flow/start` 并刷新状态。
- [ ] 业务单据填报类应用入口打开后自动弹出新增表单，保存后能回到列表并看到新记录。
- [ ] 新增触发器时未选择业务对象，前端保存按钮禁用或提示“请先选择业务对象”，不会向后端提交空 `objectCode`。
- [ ] 从对象设计器进入触发器配置时，新增弹窗自动带入当前对象并加载字段。
- [ ] 单据闭环配置步骤条能显示每一步完成状态，并能从单据设置连续进入主流程、触发器、发布检查和试运行。

### 18.11 测试策略

- **菜单回归**：用已同步菜单的 CRM 商机入口验证点击、刷新、浏览器后退、Tab 切换后的 activeKey。
- **入口模式验证**：分别验证 `LIST` 和 `CREATE_FORM`，确认普通对象不自动弹表单，单据填报入口自动弹新增表单。
- **单据设置验证**：编号规则 token 插入、未知变量校验、预览接口、状态映射默认生成和保存回显。
- **流程配置验证**：流程变量候选项接口、自动映射建议、标题模板预览、保存后主流程摘要一致。
- **手动发起验证**：草稿记录显示按钮，流程中记录隐藏或禁用按钮，无权限用户不可见且接口拒绝。
- **触发器验证**：全局新增必须选择对象；对象上下文新增自动带对象；场景模板填充事件和动作配置。
- **构建验证**：`cd forge && mvn -pl forge-admin-server -am package -DskipTests`，`cd forge-admin-ui && pnpm build`。
- **日志记录**：执行前读取 `code-copilot/rules/automated-testing-standard.md`，并把命令、结果、警告和跳过项追加到 `execution-log.md`。

### 18.12 风险与关注点

- ⚠️ 菜单授权风险：已有动态应用菜单可能已经绑定角色，更新 path 时必须保留原 `sys_resource.id`。
- ⚠️ 兼容风险：历史入口仍可能依赖 `/app-center/app/{id}` 中转页，不能直接删除该路由。
- ⚠️ 双流程配置风险：`default_flow_key` 和 `ai_business_binding` 可能存在历史不一致，必须给出迁移优先级和页面提示。
- ⚠️ 变量解析风险：BPMN 表达式无法 100% 静态识别，本阶段只做候选项和建议，不阻断高级自定义变量。
- ⚠️ 自动按钮风险：自动生成和自定义操作可能重复，需要按 `START_FLOW` 动作和位置去重。
- ⚠️ 编号预览风险：预览不能消耗真实序列号，真实编号生成如未落地，需要在页面明确标记“预览规则”或补齐后端生成点。

### 18.13 Task 30-33 实施结论

- 流程与自动化页面已接入标题模板变量插入、实时预览、流程变量候选下拉和推荐映射补空；触发器 `START_FLOW` 动作复用同一套配置体验。
- 运行态单据接口已返回自动动作摘要，主流程发起方式为 `MANUAL` 或 `BOTH` 时，前端可自动渲染“发起流程”行操作并调用 `/ai/business/flow/start`。
- 触发器新增已前置业务对象选择，从对象设计器进入时通过 `objectCode` query 自动带入上下文，保存前不再把空对象编码提交给后端。
- 对象设计器已增加单据闭环步骤条，串联单据设置、主流程、触发器、发布检查和试运行入口；发布检查/readiness 已补充菜单入口、打开模式、编号规则、状态映射、主流程、自动按钮和触发器缺口。
- 本轮验证已完成：后端 `forge-plugin-generator` 及依赖模块 compile 通过，前端 `pnpm build` 通过，`git diff --check` 通过。接口联调和浏览器点击验证因本轮未启动服务/数据库记录为跳过项。

## 19. Phase 10 增补：定时触发闭环

### 19.1 背景

当前触发器表结构和前端已经预留 `SCHEDULE`/到期提醒配置，但真实执行链路仍只依赖业务事件发布。`BusinessTriggerMapper.selectActiveByObjectAndEvent` 还限定 `trigger_type='EVENT'`，因此定时触发器不会被执行。用户明确要求补齐定时触发能力，同时注意效率和执行间隔，避免过于频繁扫描。

### 19.2 功能要求

- 定时触发器统一保存为 `triggerType=SCHEDULE`，兼容历史 `SCHEDULED`。
- 默认只实现“到期提醒”类定时触发，事件类型为 `SCHEDULED_DUE`。
- 定时触发配置写入 `eventCondition.schedule`，至少包含：
  - `dueField`：到期日期/时间字段，必填。
  - `lookAheadDays`：提前扫描天数，默认 `0`。
  - `lookBehindDays`：回看天数，默认 `0`。
  - `batchSize`：单轮处理条数，默认 `50`，最大 `200`。
  - `minIntervalMinutes`：同一触发器最小扫描间隔，默认 `5`。
- 扫描任务必须接入系统自带任务调度中心，默认 cron 为 `0 0/5 * * * ?`，可通过任务中心调整，但不得默认秒级轮询。
- 集群部署时优先依赖 Quartz JDBC 集群调度保证同一任务只被一个节点触发；Redisson 全局锁和记录级锁作为手动触发、配置缺失或补偿场景的兜底防重。
- 单条到期记录执行前需要有记录级防重保护，避免锁超时、手动补偿或后续扩展导致同一记录重复动作。
- 未配置到期字段、对象没有运行配置、到期字段不在运行配置字段白名单内时，必须跳过，不允许全表扫描。
- 同一触发器、同一记录、同一事件默认按自然日去重；当天成功或 TODO 后不重复执行。

### 19.3 非目标

- 不实现用户自定义 cron 表达式的全量调度平台。
- 不为每个业务触发器动态创建 Quartz Job。
- 不做复杂时间轮或秒级任务。
- 不绕过动态 CRUD 安全读取业务表数据。
- 不在本阶段引入新的分布式任务调度平台；复用系统已有任务调度中心和 Redis/Redisson 基础设施。

### 19.4 验收策略

- 后端编译验证扫描器、Mapper XML 和动态读取方法可编译。
- 前端构建验证定时触发配置项可打包。
- 如本地服务和数据库可用，再追加接口/数据库验证：新增 `SCHEDULE` 到期提醒触发器，扫描器执行后写入 `ai_business_trigger_log`，同一记录当天不重复。

### 19.5 Task 34 实施结论

- 已新增 `BusinessTriggerSchedulerService` 并注册为系统任务中心任务：任务名 `lowcodeBusinessTriggerScanJob`，分组 `LOWCODE`，默认 cron `0 0/5 * * * ?`，每轮最多扫描有限数量触发器，单触发器默认处理 50 条、最大 200 条候选记录。
- 已将集群安全方案调整为“系统任务调度中心 Quartz 集群触发 + Redisson 全局扫描锁 + 记录级执行锁”。全局锁 key 为 `forge:business-trigger:schedule:scan`，未抢到锁的节点直接跳过本轮；记录级锁按 `tenantId + triggerId + recordId + naturalDay` 生成。
- `RedissonClient` 通过 `ObjectProvider` 可选注入；未检测到 Redisson 时仍可依赖任务调度中心集群触发和日志去重，生产如需手动并发兜底建议启用 Redis/Redisson。
- 已补齐 `ScheduleConfig` 的 Quartz 集群配置：`forge.job.clustered` 默认 `true`，并支持 `thread-pool-size`、`cluster-checkin-interval`、`misfire-threshold`、`table-prefix` 配置。
- 定时触发执行改为同步调用单条触发器动作，确保记录级锁覆盖动作执行和执行日志写入；普通业务事件触发仍保留异步执行。
- 到期字段必须来自运行态字段白名单，候选读取走 `DynamicCrudService.selectScheduledCandidateRows`，按字段区间和批量上限查询，不拼接未校验业务表字段。
- 同一自然日内已写入 `SUCCESS/TODO` 日志的 `triggerId + recordId + SCHEDULED_DUE` 不再重复执行；每次有效扫描后回写扫描时间，减少无候选数据触发器的重复优先扫描。
- 本轮验证已完成：后端 `forge-plugin-generator`、`forge-plugin-job` 及依赖模块 compile 通过，前端 `pnpm build` 通过，`git diff --check` 通过。未启动后端服务、数据库或多实例环境，真实 Quartz 集群触发、任务中心注册和落库去重验证记录为跳过项。

### 19.6 用户反馈跟进结论

- 对象类型只描述数据结构，不决定入口只能打开哪种页面；`SINGLE`/单表对象仍可选择“列表管理”或“单据填报”。
- “单据填报”是一次性填报场景，进入页面直接显示新增表单，没有列表上下文，因此不显示列表操作列和行操作按钮。
- 需要自定义操作、行级发起主流程、编辑/删除等列表上下文时，应选择“列表管理”；发起主流程按钮复用“流程与自动化”的主流程配置，不再重复选择流程。
- 应用入口详情接口需要拍平父级菜单字段，并保留 `originalParentId`，避免套件父目录实际挂载后编辑回填丢失。
- 自定义操作保存于业务对象 `designerOptions.actions`，发布时必须注入运行态 table zone 的 `customActions`，并由运行态列表合并到操作列。
- 发起主流程是“业务对象主流程”能力，不强制依赖单据模式；启用单据模式时执行状态字段和单据动作校验，未启用单据模式时按对象主流程绑定和已发布运行配置直接发起流程。

### 19.7 发布检查与回显一致性补充

- 发布检查读取应用入口时必须优先选择启用、已绑定运行配置、已同步菜单资源、已配置打开模式且最近更新的运行态入口，避免同一对象存在历史入口时误报“菜单资源未同步”。
- `runtimeOpenMode` 缺省值按 `LIST` 处理，不再作为“未配置”警告；只有存在非法值时才提示配置错误。
- 菜单资源 ID、父级菜单 ID 和原始父级菜单 ID 在 `options.adminMenu` 中按字符串保存，避免雪花 Long 被前端 `JSON.parse` 转 Number 后丢精度。
- 前端父级菜单选择器统一用字符串 key/value，应用入口编辑器优先使用详情接口拍平的 `adminMenuParentId`、`menuResourceId` 回填和保存。
- 主流程摘要读取优先启用的 `FLOW` 绑定，并兼容历史 `APPROVAL` 绑定；旧空绑定或停用绑定不应抢先导致“请先配置主流程”误报。
- 旧审批兼容运行态在对象未启用完整单据状态能力但已经配置主流程时，应提示“已配置主流程，未启用完整单据状态能力”，不再把“未启用单据模式”作为流程不可发起原因。

### 19.8 运行态按钮、主流程诊断和套件目录回显补充

- `/ai/crud-page` 运行态工具栏中，`add`/`create`/`新增` 这类标准新增动作由 `AiCrudPage` 内置新增按钮承载，不应再作为自定义 toolbar action 渲染第二个“新增”。
- 业务对象自定义操作默认列表不再预置标准 CRUD 动作；发布运行态时也会跳过历史保存的标准新增、编辑、详情、删除动作，避免和内置按钮重复。
- 保存主流程绑定时，如已有 `FLOW` 绑定曾被停用，保存后必须同步置为启用，避免页面显示已配置但运行态发起仍认为无可用主流程。
- 主流程发起失败时必须输出诊断日志，至少包含租户、对象编码、记录 ID、运行配置、单据配置、绑定 ID/类型/状态/key、`binding_config.flowModelKey` 和配置预览，便于定位是对象编码、租户、绑定状态还是配置内容不一致。
- “套件作为父级目录”时，入口配置里的父级字段表示套件目录的上级；实际创建出的套件目录资源 ID 应保存为 `adminMenu.actualParentId/suiteMenuResourceId` 并在编辑抽屉中只读回显，避免用户误以为父级未回填。
