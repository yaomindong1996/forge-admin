# Forge项目决策记录

> 记录项目级架构和产品决策，避免后续变更重复讨论。

## 1. 低代码应用与代码生成统一主链路

**记录日期**: 2026-05-25

低代码应用、AI 应用生成和代码生成统一收敛到“应用管理/应用开发”主入口。用户从需求描述生成模型和应用草稿后，必须确认才保存；应用级代码预览和 ZIP 下载默认使用已保存草稿，发布版本作为可选来源。

模型管理继续保留独立入口，因为模型是领域资产，可以不参与应用设计。模型导入直接读取数据源表结构生成 `ai_lowcode_model.model_schema`，不从旧 `GenTable` 选择；旧 `GenTable` 数据保留但新流程忽略。

数据源管理保留为开发者菜单；模板管理菜单去掉。首期代码生成支持单表/单主模型，主子表、左树右表和树形单表作为后续扩展。

## 2. AI Agent 配置来源

**记录日期**: 2026-05-25

Forge 的 AI Agent 角色提示词必须优先配置在 `ai_agent.system_prompt`，可维护上下文、输出协议和规则必须放在 `ai_context_config`，代码只传 `agentCode`、用户输入和运行时变量。低代码业务系统生成使用 `lowcode_system_generator`，禁止把完整业务 Prompt 长期硬编码在 Java Service 中；Java 里只保留规则降级和协议归一化逻辑。

## 3. 表单优先业务对象设计器使用 fcDesigner 作为首期画布

**记录日期**: 2026-05-31

低代码业务对象设计器后续主链路调整为“表单优先”：普通用户默认先设计最终表单，平台再维护字段注册表、视图投影、级联规则和运行态配置。

首期不从零自研完整表单画布，优先复用系统已集成的 `fcDesigner` / form-create 能力。`fcDesigner` 负责拖拽画布、组件排序、基础属性编辑和预览，Forge 负责业务组件适配、字段绑定、FormDesignerSchema、ViewSchema、LinkageSchema、发布检查和运行态编译。

form-create rule/options 只作为设计器可编辑表示，不能成为 Forge 运行时唯一事实来源。保存和发布必须通过 Forge Adapter 转换为 `FormDesignerSchema + FieldRegistry + ViewSchema + LinkageSchema`，发布运行态继续编译到 `AiCrudPage`、`AiForm`、`DynamicCrudController` 和 `LowcodeRuntimeConfigBuilder`。

## 4. 审批场景统一归入 Flowable 流程引擎

**记录日期**: 2026-06-02

低代码应用不再建设独立“审批引擎”。审批是业务单据绑定 Flowable 流程后的使用场景，发起、待办、结果回写、消息通知和触发器联动都围绕流程实例完成。

内置示例和 seed 数据必须部署真实 Flowable 流程定义，不能用模拟审批实例 ID 代替。业务对象的流程绑定和自动触发器需要显式维护业务字段到流程变量的映射，确保 BPMN 节点表达式需要的变量在启动时已经存在。

## 5. 低代码定时触发接入系统任务调度中心

**记录日期**: 2026-06-02

低代码业务触发器的 `SCHEDULE` 定时能力不为每个触发器创建独立 Quartz Job，也不默认做秒级扫描。平台只注册一个 `LOWCODE.lowcodeBusinessTriggerScanJob` 到系统任务调度中心，默认每 5 分钟扫描一次启用的到期提醒触发器，后台任务中心可统一启停、改 cron 和查看日志。

集群部署下主防重依赖 Quartz JDBC 集群调度，`forge.job.clustered` 默认应开启；Redis/Redisson 全局扫描锁、记录级执行锁和同日日志去重作为手动触发、补偿执行或任务配置缺失时的兜底防线。

## 6. 登录验证码配置解析规则

**记录日期**: 2026-06-07

登录验证码配置采用“全局默认 + 客户端覆盖”的单一解析规则。系统登录配置维护全局默认验证码开关和默认验证码类型；`sys_client.captcha_type` 只作为客户端覆盖项，空值表示继承全局配置。

登录页获取 `/auth/loginConfig` 时必须传入当前 `userClient`，后端登录配置接口和登录校验策略必须共用同一个解析器返回最终生效配置，避免前端展示验证码类型和后端校验验证码类型不一致。

## 7. 代码生成模板更新和删除必须使用 POST

**记录日期**: 2026-06-14

Forge 代码生成模板不能按通用 REST 风格生成 `PUT` 更新或 `DELETE` 删除接口。出于项目安全策略和网关兼容要求，生成 Controller、前端 API 和 `AiCrudPage.apiConfig` 必须保持既有 POST 风格：

- 详情：`POST /getById`
- 新增：`POST /add`
- 更新：`POST /edit`
- 删除：`POST /remove/{id}`
- 批量删除：`POST /removeBatch`

应用管理下载代码模式可以把接口前缀替换为业务专属 `businessApiBase`，但不能把更新、删除改成 `PUT` / `DELETE`。

## 8. 长期记忆统一归集到 code-copilot/memory

**记录日期**: 2026-06-14

项目决策、踩坑记录、用户偏好三类长期记忆从 `.opencode/memory/` 迁移到 `code-copilot/memory/`，后续只维护 code-copilot 下的权威文件：

- 项目决策：`code-copilot/memory/decisions.md`
- 踩坑记录：`code-copilot/memory/pitfalls.md`
- 用户偏好：`code-copilot/memory/preferences.md`

`AGENTS.md`、`code-copilot/AGENTS.md`、`code-copilot/agents/*.md` 和变更模板必须指向上述权威路径；`.opencode/memory/` 不再作为 Forge 项目长期记忆维护位置，`code-copilot/knowledge/` 只保留专题技术知识材料。

## 9. 数据权限控制面元数据固定在平台主库

**记录日期**: 2026-06-22

租户业务数据源切换只影响业务 Mapper 查询的主业务表。数据权限控制面元数据，包括 `sys_data_scope_config`、`sys_role`、`sys_role_data_scope`、`sys_org` 和 `sys_region_code`，必须固定由 Forge 平台主库提供，不能要求租户业务库复制这些平台表。

`forge-starter-datascope` 运行时应先从 `forge.datascope.metadata-datasource`（默认 `master`）加载控制面快照，再在业务 SQL 拦截时只读取内存快照。行政区划权限等需要平台字典/树数据的规则，必须提前解析成业务库可执行的字面量条件，禁止在业务库 SQL 中拼接 `sys_region_code` 等平台表子查询。

## 10. 低代码业务域目录按资源权限解析，不覆写领域默认菜单父级

**记录日期**: 2026-06-25

低代码业务域的自动生成目录不再写回 `ai_lowcode_domain.menu_parent_id` 作为唯一真相。领域目录应通过 `sys_resource.perms=ai:lowcode:domain-menu:{domainCode}` 解析或创建，并按业务域父链递归挂载到管理端菜单；`ai_lowcode_domain.menu_parent_id` 仅保留用户在领域上配置的默认应用菜单父级，不能被自动生成目录 ID 覆盖。

应用总览的领域筛选应按当前领域及其子树查询，而不是只看直系 `domain_id`。这样父业务域既能作为目录节点继续挂子目录，也能在列表页查看整个子树下的应用。

## 10. 低代码运行规则和字段展示统一走共享封装

**记录日期**: 2026-06-25

列表设计器、表单设计器、发布运行页和详情页中，涉及组件/字段/模块的显示隐藏、只读、禁用、必填、颜色和样式控制，统一通过 `forge-admin-ui/src/components/lowcode-builder/shared/runtime-rules.js` 解析。

字段展示不再在列表列、详情字段、表单只读态中分别实现。文本、字典标签、状态 Tag、链接、金额和颜色规则统一走 `FieldValueRenderer.vue`；运行规则配置入口统一走 `RuntimeRulesEditor.vue`。后续新增动态展示组件或复杂字段渲染时必须优先扩展这三个共享封装，避免列表和表单再次分叉。

运行规则的数据来源统一使用 `source` 表达：`record` 表示当前记录/详情，`row` 表示当前行数据，`formData` 表示当前表单数据，`query` 表示 URL 查询参数，`params` 表示路由参数，`user` 表示当前用户。列表行操作打开的编辑/详情弹窗必须把当前行放入 `context.currentRow`，运行规则上下文再统一映射为 `row`。

## 11. AiCrudPage 行展开能力按通用面板协议设计

**记录日期**: 2026-06-27

AiCrudPage 的行展开能力不能限制为“展开子表 table”。后续实现必须使用通用 `expandConfig.panels[]` 协议承载展开内容，至少支持子表表格、描述信息、只读表单、Tabs 多面板和自定义组件/插槽。

子表只是展开面板的常见数据源之一，可从 `childrenConfig` 或模型关系中生成默认 table panel，但不能替代 `expandConfig`。展开数据源默认懒加载并支持按当前行 `row` 映射请求参数，避免主列表加载时产生 N+1 请求。设计器入口应提供飞书式低门槛配置体验，让用户选择“子表 / 描述 / 多面板 / 自定义”预设，而不是要求手写 JSON。

## 12. 业务应用绑定流程后的节点配置归流程设计器维护

**记录日期**: 2026-06-28

业务对象启用单据流程闭环后，应用中心“业务流程配置”只负责业务对象与流程模型的关联、业务表单资产目录、业务字段到流程变量的映射，以及进入流程设计器的入口。审批办理、节点表单资产、字段可见/可写/必填、会签、抄送、驳回、监听器等节点级策略必须在流程设计器里选中节点后配置。

运行时解析优先级固定为 `BPMN 节点 formKey/formUrl/formJson/formFieldPermissions > BusinessFlowBinding.nodeForms 兼容兜底 > 流程默认表单`。`BusinessFlowBinding.nodeForms` 只用于历史配置和未重发流程模型的兼容，不再作为新配置主数据源。

**2026-06-29 修订**：用户明确纠正，节点配置工作台不是低代码应用配置里的独立功能，应拉起真实流程设计器页面，在流程节点抽屉中配置。应用中心不能维护一份脱离流程图的节点配置副本，否则运行时和设计时会割裂。

## 13. 代码应用字段目录在应用中心只读展示

**记录日期**: 2026-06-29

采购单审批这类代码 Provider 接入的业务对象，字段定义来自业务代码，不由低代码表单设计器编辑。应用中心对象设计器可以开放“表单字段”面板，但定位是只读字段目录和节点权限矩阵检视，不能提供字段新增、删除、类型修改等编辑能力。

节点表单资产、字段可见/可写/必填等可变策略仍归流程设计器节点抽屉维护。应用中心只负责帮助业务管理员理解“代码 Provider 提供了哪些字段、当前流程节点如何使用这些字段”，避免形成第二套脱离 BPMN 的节点配置入口。

**2026-06-29 修订**：用户再次明确，代码应用设计入口不再单独展示“表单字段”面板，即使只读目录也会让业务管理员误以为这里需要维护一份字段配置。代码应用在应用中心不维护脱离既有设计器的字段配置副本。

**2026-06-30 修订**：用户进一步明确，代码应用不是只能保留“业务流程配置”。应用中心左侧已经有“表单设计”“列表设计”“详情设置”组件，代码应用字段展示、列表列和详情字段配置必须复用这些既有组件与 `formDesignerSchema/viewSchema` 协议；禁止在“业务流程配置”里另造“字段与视图”面板。业务流程配置仍只负责流程绑定和打开真实流程设计器，节点字段权限仍归流程设计器节点抽屉维护。

## 14. 自动编号统一沉淀到字段生成配置

**记录日期**: 2026-07-01

低代码业务对象的自动编号能力统一收敛为通用编码规则生成器。表单设计里的字段“自动编号”和单据设置里的“编号生成”不能维护两套规则；单据设置只选择编号字段并写回该字段的 `generation` 配置，运行态新增数据时由后端读取字段配置并调用通用编码规则生成真实编号。

前端只负责选择规则和预览样例，不消费真实流水号。真实编号必须在 `DynamicCrudService` 新增链路后端生成，并复用 `forge-starter-id` 的 `ISequenceService` 做业务序列，避免用户手填或浏览器端生成造成重复编号。

## 15. 组织上下文权限按当前组织计算

**记录日期**: 2026-07-06

Forge 权限上下文固定为“数据中心 + 当前组织”两层。数据中心继续使用 `tenantId` 和 `sys_user_tenant`；组织使用 `LoginUser.activeOrgId` 表示一次请求的唯一当前组织。

用户组织成员关系由 `sys_user_org` 表达，角色适用组织由 `sys_role_org` 表达，用户在组织内的真实授权由 `sys_user_org_role` 表达。旧 `sys_user_role` 仅保留为迁移和兼容来源，普通登录态、菜单、按钮、API、数据权限、消息、流程和数据集旁路都不能回退旧表计算权限。

`LoginUser.orgIds` 只表示当前数据中心下可切换组织集合，不代表本次请求的数据范围。`ORG` 数据权限只使用 `activeOrgId`，`ORG_AND_CHILD` 只展开当前组织及其子组织，`create_dept` 自动填充也优先写当前组织。

## 16. Spring AI Alibaba 作为供应商与治理增强层接入

**记录日期**: 2026-07-10

Forge 保留 Spring AI 作为统一 `ChatModel/ChatClient` 接口层，Spring AI Alibaba 作为增强层叠加，不进行“从 Spring AI 切换到 Spring AI Alibaba”的替换式重写。当前经过依赖树、测试和主应用装配验证的基线固定为 Spring AI `1.1.2`、Spring AI Alibaba/Extensions `1.1.2.3`。

多租户供应商凭据来自数据库，因此 DashScope 只引入 `spring-ai-alibaba-dashscope` 核心模型模块，运行时动态构建模型；不引入会读取全局 API Key 并自动装配单例 Bean 的 DashScope Starter。模型协议由 `ai_provider.adapter_code` 显式路由，当前稳定值为 `openai_compatible` 和 `dashscope_native`，禁止依据 `providerType` 品牌或 Base URL 猜测。历史记录保持 `openai_compatible`，只有管理员显式切换并通过连接测试后才能使用 Native。

Nacos MCP Registry、Admin、Agent Framework、MCP Server 和 Agent Runtime 仍按 AI 中枢阶段闸门另立变更，不能因 DashScope Adapter 已落地而视为完成。回退到不识别 Native Adapter 的旧应用前，必须确认不存在 `dashscope_native` 记录；存在时先改为 Compatible 协议和 URL/config，并通过连接测试后再部署旧应用。

## 17. AI 模型治理使用显式候选和模型级健康键

**记录日期**: 2026-07-11

模型路由只允许从管理员配置的显式候选中按 `priority ASC → target.id ASC` 确定性选择，不扫描全库、不按品牌猜测、不使用随机权重。调用前可以跳过已经 OPEN 的策略候选；请求一旦发送，失败必须立即结束，禁止在同一请求内换模型补发。

健康状态键固定为 `tenantId/providerPk/modelPk`，默认使用单实例内存 `AiModelHealthRegistry`，通过 Lease 管理 HALF_OPEN 单试探。供应商/模型配置提交后清理对应健康状态。后续多实例共享状态可以替换为 Redis/Nacos 实现，但不得让业务调用链直接依赖注册中心 SDK。

调用治理只保存路由、耗时、Token、价格快照和白名单错误元数据，不保存 Prompt、响应正文、API Key、Header 或原始供应商异常。成本是治理估算，不是计费出账。

## 18. Forge MCP Server 统一使用 Streamable HTTP

**记录日期**: 2026-07-11

Forge 后续 MCP Server 的标准传输协议固定为最新的 **Streamable HTTP**，不实现或启用旧版独立 SSE transport。`forge-ai-hub-foundation` 的依赖 Spike、服务端配置、客户端联调、自动化测试和文档示例都必须以 Streamable HTTP 为准。

允许 Streamable HTTP 协议自身在同一 MCP HTTP 端点内按规范使用流式响应，但禁止重新建设旧式的独立 SSE 建连端点、SSE session 管理或双端点兼容层。若目标 Spring AI 版本无法稳定支持 Streamable HTTP，阶段 0 必须失败关闭并形成版本兼容结论，不能为了演示退回旧 SSE 方案。

## 19. Forge AI 能力内核与 MCP 出口采用协议隔离和失败关闭

**记录日期**: 2026-07-11

Forge AI 能力的协议无关内核统一由 `forge-plugin-capability-core` 承载，`forge-plugin-mcp` 只负责 Streamable HTTP 与 MCP Schema/结果投影。业务插件只能通过 `CapabilitySource/CapabilityExecutor` 注册能力，不能直接创建 MCP Bean，也不能让 Capability 内核反向依赖 Spring AI、MCP SDK 或 `forge-plugin-ai`。

MCP enabled 时启动期只允许 `STREAMABLE`，并拒绝 `SSE`、`STATELESS` 和 stdio。身份必须在 `/mcp` 进入 SDK 前完成验证，租户、用户、当前组织和 scope 只能来自可信传输上下文。能力游标绑定快照、查询、调用方并使用进程内 HMAC-SHA256 防篡改；Schema 必须先完整校验再投影；安全日志只记录 requestId、客户端安全引用、租户、组织、能力、结果码、Schema 路径和耗时。

Spring AI MCP SDK `0.17.0` 的 `tools/list` 使用静态全局工具集合且不支持请求级游标。因此在公开扩展点或隔离适配器解决动态目录之前，MCP 只能发布所有认证客户端统一可见、统一可调用的 `capability.ping`，禁止发布权限异构的业务工具。Nacos MCP Registry/Admin 和 Agent Runtime 继续按后续阶段闸门另立变更。

## 20. MCP 审计采用机器客户端与实际操作者双身份

**记录日期**: 2026-07-12

Forge MCP 身份分为两种可信模式。系统对系统调用使用控制面机器凭据，身份由 `clientId + serviceUserId + tenantId + activeOrgId` 的数据库绑定决定，审计实际主体等于服务账号。需要归因到具体人员 A 时，必须由 A 的 Forge 登录态或 OAuth 授权换取短期用户委托令牌，令牌同时绑定 A、机器客户端、租户和当前组织；审计记录 `actorUserId=A`，并保留 `clientId/serviceUserId`。

客户端自报的 `userId`、`tenantId`、`activeOrgId` Header 不能作为身份依据。机器凭据共享给多人时只能证明服务客户端，不能证明背后的实际自然人；因此包含人员责任、审批或业务归属的能力必须要求用户委托令牌，不能降级为机器账号代办。

## 21. MCP 用户委托采用受限 Forge OAuth 2.1 Profile

**记录日期**: 2026-07-12

Forge MCP 用户委托身份不引入 `spring-security-oauth2-authorization-server`，也不采用缺少 PKCE/resource/PUBLIC Client 支持的 Sa-Token OAuth2。阶段 2.0 仅实现 MCP 所需的受限 Forge OAuth 2.1 Profile：authorization code + PKCE S256、client credentials、RFC 8414/9728 metadata、RFC 8707 resource 和 revoke。

该 Profile 不扩展为通用 OAuth 平台，不实现 refresh token、implicit、password、动态客户端注册或任意 grant。长期 `fcp_` 机器密钥只能用于机密客户端换取短期令牌，不能直接调用 `/mcp`；MCP 只接受短期 `fdu_` Bearer，并在每次 Streamable HTTP 请求实时校验 client、actor、service user、tenant、active org、resource、scope 和 credentialVersion。

## 22. MCP 能力授权在 Identity 组合层计算实时交集

**记录日期**: 2026-07-12

Capability 内核继续保持协议和 ORM 无关，MCP 业务能力的最终授权由 `forge-plugin-capability-platform` 中的 Identity 组合层实现，固定计算 `Token scope ∩ client grant ∩ 当前 LoginUser.permissions ∩ tenant/activeOrg`。默认 Forge 权限映射为 `ai:capability:discover:{capabilityCode}` 和 `ai:capability:invoke:{capabilityCode}`；后续安全动作可以替换映射 Bean，但不能从客户端参数推导权限。

`capability.ping` 是阶段 2.0 唯一统一授权例外，仍必须先通过短期 Token scope 和可信执行身份校验。MCP Server 显式锁定 `type=SYNC + protocol=STREAMABLE + stdio=false`；用户、grant、权限或组织变化后由每请求实时身份加载与授权决策失败关闭。

## 23. 受控业务写入使用固定元工具与发布快照

**记录日期**: 2026-07-12

Forge AI 中枢阶段 2.1 不把每个业务动作动态发布成顶层 MCP Tool，固定只增加 `capability.search`、`capability.describe`、`capability.invoke`。授权目录在每次请求中计算 Token scope、client grant、当前用户权限、tenant/activeOrg 的实时交集，目录分页采用 keyset 分批读取；OAuth Profile 支持 `capability:discover`、`capability:invoke` 通用 scope，也支持能力级 scope，但 scope 本身不能绕过 grant 和 Forge 权限。

可写能力当前只允许不可变发布快照中的 `BUSINESS_ACTION + ACTION + MEDIUM`，步骤只允许 `UPDATE_FIELD`、`CREATE_RECORD`。执行字段固定取“能力版本白名单 ∩ grant 白名单 ∩ 指定发布模型可写字段”，并要求 16～128 位幂等键和同一次 MCP elicitation `ACCEPT + confirm=true`。Flowable、消息、领域动作、高风险能力和任意 CRUD/SQL/URL 继续失败关闭，后续分别另立阶段。

## 24. 受控写动作采用审计预留与可信身份条件更新

**记录日期**: 2026-07-12

受控写动作在 MCP elicitation ACCEPT 后、业务副作用发生前，必须先按 requestId、client、capability、actor、service user、tenant 和 active org 写入审计预留。预留失败直接返回 `AUDIT_UNAVAILABLE`，禁止进入业务执行。

执行结束后只允许按同一 requestId 与完整可信身份条件更新最终状态；重复 requestId 但身份不一致必须失败关闭。若业务已完成而最终审计更新暂时失败，保留 `EXECUTION_PENDING` 记录并返回 `AUDIT_UNAVAILABLE`，由相同幂等请求重试补齐最终状态，不能静默返回“业务成功但无 Capability 审计”。

## 25. 受控流程动作继续使用固定元工具与真实流程绑定

**记录日期**: 2026-07-12

流程能力不动态创建 `flow.process.start`、`flow.task.approve` 等顶层 MCP Tool，统一继续通过 `capability.search/describe/invoke` 发现和执行。`FLOW_ACTION` 只能从启用、已发布业务对象的当前真实 FLOW binding 发布，不新增第二套流程配置、任意 Flowable API 或旧 SSE 端点。

首批操作固定为 START/APPROVE/REJECT。taskId 只是任务定位符，授权必须由服务端实时校验当前 USER A 已签收、任务未完成、businessKey/objectCode/recordId/processDefKey 与能力发布快照一致。发起人和办理人只能来自可信执行身份，客户端 DTO/Header 中的 userId、tenantId、activeOrgId、flowModelKey 和 variables 均不能覆盖。

## 26. 低代码快捷模板只初始化统一 Schema，页面入口为可选资产

**记录日期**: 2026-07-14

单表 CRUD、左树右表和主子表快捷模板统一初始化既有 `LowcodeModelSchema`、`LowcodePageSchema` 和 `BusinessObjectRelation`，不得为三类场景建设独立生成器或运行协议。模板生成结果继续进入相同的数据结构、表单、列表和详情设计器修改；模板资产必须在单事务内生成，失败只保留应用草稿。

页面入口属于可选交付资产，不再是应用发布硬门。没有菜单或访问入口的应用仍可发布业务对象、预览设计草稿和生成代码；只有用户实际选择发布的入口自身配置错误时才按入口资产规则处理。简易数据库表导入统一收进应用/业务对象创建流程，旧模型资产页从主导航隐藏但保留兼容路由、API 和存量数据。

## 26. 独立 Flow 服务使用短期 Sa-Token 用户委托桥

**记录日期**: 2026-07-12

MCP `fdu_` 只属于 Admin 的 Forge OAuth 2.1 Profile，独立 Flow 服务不直接识别该 Token。受控流程动作跨服务调用时，由 Admin 根据当前可信 USER ExecutionIdentity 签发 60 秒内部 Sa-Token；Token Session 必须绑定完整 LoginUser、actorUserId、tenantId、activeOrgId、clientId 和内部委托标记。

委托 Token 每次使用唯一 device 会话，避免 Sa-Token 共享或并发登录策略复用、替换普通登录 Token。签发返回空 Token、无法建立 Token Session、身份字段不一致或 clientId 非正数时必须失败关闭，禁止降级为静态服务账号。

Flow 的 delegated START 使用专用内部入口，同时校验业务权限和 Session 委托标记；普通登录 Token 即使拥有发起权限也不能调用。START 发起人以及 APPROVE/REJECT 办理人和租户最终都从 Flow 服务端 Session 解析，客户端 userId/tenantId 只能为空或与可信 Session 一致，不能覆盖。

## 27. HIGH 业务动作使用加密审批快照与回调重新授权

**记录日期**: 2026-07-12

`BUSINESS_ACTION/ACTION/HIGH` 禁止在 MCP 请求线程直接执行业务副作用。调用在 MCP elicitation 确认后只创建 `ai_capability_approval`，以每记录 AES-256-GCM DEK 加密参数，DEK 由外部 Secret 提供的版本化 256-bit KEK 通过 AES Key Wrap 包装；数据库、日志、普通审计和 MCP 输出均不保存明文参数或密钥。

审批固定使用 `forge_capability_high_risk_approval` 和 `capability-approval:<approvalId>`，默认 BPMN 只在模型不存在或 BPMN 为空时写入，已有非空设计永不覆盖。APPROVED 回调必须按审批行加锁，并重新校验 policy、客户端有效期与 credentialVersion、服务账号、USER A、tenant/activeOrg、实时 grant/权限、能力版本、发布动作字段和业务状态摘要，再使用原幂等键至多执行一次。

审批状态查询只通过固定 `capability.approval.get` 暴露，并绑定原 client、actor、serviceUser、tenant 和 activeOrg；不返回密文、keyId、wrappedDek、taskId 或流程实例详情。高风险功能默认关闭，启用但缺少有效 KEK、固定流程模型或任一重新授权条件时必须失败关闭，禁止降级为 MEDIUM 同步执行。

## 28. 低代码对象保存与数据库同步使用独立编排链路

**记录日期**: 2026-07-13

对象设计器的保存接口只写 ModelSchema、PageSchema、表单/视图 Schema 和设计态元数据，不再根据 `syncDdl/confirmSyncDdl` 兼容字段执行数据库 DDL。数据库差异预览和在线同步固定使用独立的 table-mapping、database-diff、database-sync API，并绑定当前 `draftVersion`。

在线同步必须同时满足显式二次确认、`ai:lowcode:deploy-ddl`、数据源 `allowDdl`、非只读和设计版本一致；只允许 CREATE TABLE 与 ADD COLUMN。MODIFY/ALTER/DROP/RENAME 及其它非追加式 DDL 仅预览/导出，且底层 `LowcodeDdlService` 也不再把 MODIFY/ALTER COLUMN 视为可执行安全语句，避免预览和执行之间结构变化造成绕过。

表映射服务通过小接口 `BusinessObjectDesignContextProvider` 复用统一对象设计上下文，不复制 Schema 或版本表。最近同步摘要保存在 `ai_business_object.designer_options.databaseSync`；应用对象列表会比较同步版本与当前设计版本，版本落后时显示 `OUT_OF_SYNC`。

## 29. 低代码扩展采用隔离执行和显式服务注册

**记录日期**: 2026-07-13

Forge 低代码扩展固定分为 CLIENT_JS、SCOPED_CSS 和 SERVER_BINDING 三类治理能力。CLIENT_JS 只能在独立 Worker 中通过受限消息协议运行，设置超时并禁止访问主页面全局对象；SCOPED_CSS 必须先经过 CSS AST 校验，再限定到应用/对象作用域；SERVER_BINDING 只能绑定服务端显式注册的处理器编码，禁止提交类名、方法名后任意反射。

扩展必须经过版本、编辑锁、验证、测试和状态机后才能启用，执行日志只保存脱敏摘要。任意在线 Java、任意 SQL、主页面 `eval/new Function` 和未注册服务调用不进入低代码扩展能力。

## 30. 应用发布版本与可恢复运行单分离

**记录日期**: 2026-07-13

应用级发布使用两类持久化对象：`ai_business_application_version` 保存成功提交后的不可变快照，只允许新增；`ai_business_application_publish_run` 保存幂等键、候选快照、固定步骤、失败位置、尝试次数和恢复证据。只有全部发布步骤完成后才能写不可变版本并提交应用为 `PUBLISHED`，后续资产变更传播应用为 `CHANGED`。

发布步骤固定为 `PRECHECK → SNAPSHOT → OBJECTS → ENTRIES → EXTENSIONS → COMMIT`，对象完成版本需要逐个写入运行单快照，恢复时跳过已有证据的副作用。历史回滚生成新的 `ROLLBACK` 版本，不覆盖旧版本，不执行反向 DDL 或业务数据回滚；物理表/字段、对象版本、入口、扩展版本或 APPLICATION Binding 不兼容时必须失败关闭。

## 31. 字段资产与页面用法采用单向继承和显式提升

**记录日期**: 2026-07-15

低代码字段资产是字段身份、数据库映射、业务类型、字典/关联、公式、数据硬约束和安全属性的唯一事实源。表单与列表 Schema 只保存当前页面是否使用字段、顺序、布局、标题/提示覆盖、控件表现、列宽/渲染、只读/隐藏和页面校验。

字段资产可以作为新页面的默认值向页面用法单向继承，但页面属性不得自动反写字段资产；需要提升为字段默认时必须由用户显式保存字段。字段编码、数据库映射和安全约束变化继续传播引用，页面覆盖继续保留。运行态展示采用“字段默认 → 页面覆盖 → 动态规则”，但后两层不能放宽字段或数据库硬约束。

设计预览必须从当前草稿图编译；发布版本中的运行配置只服务正式运行，不能作为草稿预览缓存。主子表预览和应用发布前只刷新 PRIMARY 对象的关系聚合图，避免用户逐个对象发布和无边界 N+1 刷新。

## 32. 对象关系可视化复用统一关系协议和按需字段加载

**记录日期**: 2026-07-15

关系可视化不引入第二套图模型或后端接口。`LowcodeErDiagram` 只负责对象布局、字段连接和用户事件，`BusinessRelationDesigner` 负责把任意方向的端点连接归一化为“当前对象字段 → 目标对象字段”的既有 `DETAIL` 关系，保存继续使用 `BusinessObjectRelation` 协议。

ER 画布只加载用户选择加入画布的目标对象及已有关系目标，不能为展示关系页而一次查询业务域内全部对象字段。对象卡拖动属于临时视图状态，不写入关系协议；字段连线、关系属性和高级行为才进入设计草稿。

## 33. 应用代码包复用对象生成器并按关系图聚合

**记录日期**: 2026-07-15

应用级代码预览和下载不建设第二套模板引擎，固定复用 `LowcodeRuntimeConfigBuilder + AiCrudCodegenService + VelocityCodegenStrategy`。应用只负责编排生成范围：主对象先刷新最新草稿关系图并生成单表、左树右表或主子表聚合代码；被主页面消费的 DETAIL/REFERENCE 对象不再重复生成冲突控制器，其余共享对象可批量合并到同一 ZIP。

存在 `modelSchema + pageSchema` 时，代码生成必须重新构建查询、列表、表单、接口和 options，旧派生运行字段不能覆盖最新设计。多对象合并遇到同路径不同内容时失败关闭，禁止静默覆盖。应用代码包只物化数据对象的前后端、Mapper XML、SQL 和清单；流程、扩展与外部集成继续走 Forge 治理运行，不生成绕过治理的任意 Java/SQL。

## 34. 下载代码采用完整协议快照和共享运行内核

**记录日期**: 2026-07-15

> **已被决策 35 替代**：本条只保留 Phase 11 的历史背景。前端共享解释器继续有效，后端委托动态 CRUD 和 classpath 配置注册不再执行。

低代码下载代码不再由 Vue/Java 模板分别解释字段、布局和业务规则。前端生成页固定复用在线 `LowcodeRuntimePage`，后端生成 Controller 固定委托 `DynamicCrudService/DynamicCrudExcelService`；代码包同时携带结构化协议快照、覆盖报告、前端运行配置和后端 classpath 配置。

所有进入 Velocity 的下载入口必须在公共生成策略中派生独立 `generated_*` 运行键，并把当前对象的动态 CRUD URL 投影为业务 Controller URL。普通数据库配置键只作为来源追溯和输出路径，不能让 classpath 配置覆盖或误读平台数据库配置。未知的 model/page/options 嵌套字段整体透传；后续能力由共享解释器和共享运行内核升级，重新下载后自动获得，不在生成模板维护字段白名单或第二套分支。

协议不完整、JSON 非法、存在无法改写的平台通用接口或 classpath 同键不同内容时必须失败关闭。生成包继续依赖 Forge 共享运行模块；若要求完全脱离 Forge 插件独立部署，需要另立运行时抽取变更，不能在本生成链路静默复制实现。

## 35. 下载后端采用静态 MyBatis-Plus 编译和用户所有扩展层

**记录日期**: 2026-07-15

在线低代码预览/运行继续使用 `DynamicCrudService`；下载源码的 Controller 不再调用 `DynamicCrudService/DynamicCrudExcelService`，而是调用生成的类型化 Service。生成 ServiceImpl 继承 MyBatis-Plus `ServiceImpl`，基础写操作使用 MP 内置方法，分页、列表、树、主子明细和复杂查询 SQL 固定写在 Mapper XML，主子和导入写入在生成 Service 事务内完成。

前端下载页继续复用 `LowcodeRuntimePage + runtime-config.json`。后端新增低代码能力统一通过 `VelocityCodegenStrategy` 和 `LowcodeStaticCodegenContributor` 静态编译，所有下载入口重新生成后自动获得，不允许各入口维护分支。协议快照保留完整 JSON；coverage 必须区分静态已编译能力和 `REQUIRES_EXTENSION`，禁止把仅携带 JSON 报告为后端已实现。

每个主对象生成带默认方法的 `ServiceExtension` around 契约。用户扩展实现放在生成器永不输出正式 Java 的 custom 目录，可调用 `operation.proceed()` 增强默认逻辑，也可跳过默认逻辑完整替换。ZIP 只输出 `.java.example` 和 ownership 清单，后续重新下载不能覆盖用户实现。

## 36. 下载包命名与输出目录采用统一受限协议

**记录日期**: 2026-07-15

下载包的业务类名固定按“物理表名 → 删除首个匹配的有序表前缀 → PascalCase → 追加规范化实体前缀”生成，主表、树对象、明细对象以及 Entity/DTO/Query/Mapper/Service/Controller 全部消费同一个最终 `className`。非法 Java 标识符和同路径不同内容必须失败关闭。

后端 Java、Mapper XML、前端页面和前端 API 使用四个独立且只能位于 ZIP 内的相对根目录；拒绝绝对路径、空路径段、`.` 和 `..`。下载范围可以关闭后端、前端或 Excel SQL，但完整 protocol、coverage、ownership 和 README 始终保留。Service/Mapper/Controller 后缀、Lombok、基础实体与 Forge 注解属于框架规范，不开放成任意模板组合。

## 37. 编码规则场景筛选与低代码字段映射分层

**记录日期**: 2026-07-16

编码规则的 `scene` 保留为旧调用方兼容筛选维度，配置值统一来自 `sys_code_rule_scene` 字典，不再允许自由输入。分段顺序由 `segmentOrder` 和拖拽排序表达；SEQ 是按取号顺序递增的流水号段，不新增语义重复的“顺序段”。

VARIABLE 的来源是分段级属性，固定为 `CUSTOM | LOWCODE`。CUSTOM 由业务代码按安全变量名通过 `fields` 传值，不绑定低代码对象；LOWCODE 使用低代码元数据，规则保存来源业务对象 ID 和稳定对象编码，变量只能选择该对象中启用的非系统字段。纯 CUSTOM 规则是通用规则；含任一 LOWCODE 分段的混合规则是对象专属规则。规则选择接口在有 `objectCode` 时只返回通用规则和当前对象专属规则，无对象上下文时不暴露对象专属规则；运行时生成再次校验当前对象编码。

`ruleCode` 和既有 SEQ 的 `segmentKey` 都是计数器永久身份。规则逻辑删除后编码不得复用；编辑已有流水规则必须保留同一 SEQ `segmentKey`，只允许排序和属性调整。需要全新计数器时必须创建不同 `ruleCode`，不能通过删段、改型或重建同编码规则隐式重置。

## 38. 定时任务 Flowable 编排使用固定定义与技术身份

**记录日期**: 2026-07-21

定时任务启动 Flowable 时，`invoke_mode` 只区分 SINGLE/FLOW 上层编排；SINGLE 继续解释既有 BEAN/HANDLER/RPC，FLOW 不复用或扩展任意脚本执行入口。流程绑定在保存任务时固定 `modelKey + modelVersion + deploymentId + processDefinitionId`，执行时只按 `processDefinitionId` 启动，不查询 latestVersion、不自动部署草稿。

跨模块契约固定放在 `forge-starter-job` 的 `JobFlowExecutor` SPI 中，内嵌 Flowable 和独立 Flow 服务分别提供本地/远程适配器。远程适配必须走 `SecureOutboundClient + FLOW_API`，不发送 `X-Inner-Call`；专用 Flow API 复用 `system:jobConfig:trigger` 权限，Token 只能来自服务端配置。

流程发起身份只来自 Flow 服务技术身份配置，业务参数只能作为嵌套 `jobInput`，不能覆盖用户、租户、当前组织或 businessKey。businessKey 固定为 `job:<jobConfigId>:<executionId>`；同一执行按该键幂等恢复。Flowable 返回真实 processInstanceId 即视为调度成功，后续流程节点结果由流程历史和流程告警负责，不反向改写任务启动结果。

## 39. 定时任务内部 RPC 使用专用身份和独立出站场景

**记录日期**: 2026-07-21

`/job/executor/execute` 是内部服务端点，不复用用户登录态、开放 API Token 或可伪造的内部请求头。端点默认关闭，显式启用后只接受环境注入的专用 Bearer Token；通用认证拦截器可忽略该路径，但端点自身认证必须失败关闭并返回真实 HTTP 401/503。

调度端 RPC 固定进入 `SecureOutboundClient + JOB_RPC`，服务 Token 只来自服务端配置。JOB_RPC 使用独立白名单和私网授权，不能借用 JOB_WEBHOOK/FLOW_API；成功必须同时满足 HTTP 2xx 与 `RespInfo.code=200`。

## 40. 定时任务配置同步使用分布式锁与版本收敛

**记录日期**: 2026-07-21

DB 到 Quartz 的同步不是普通最终写入。同一 Quartz Key 必须先取得 Redis 分布式锁，Redis 不可用时失败关闭；同步状态更新必须携带读取时的配置 version，版本变化后重新读取最新配置并继续收敛，不能让旧请求覆盖新状态或在逻辑删除后复活任务。

连续失败统计也按完成顺序推进，以 `end_time + executionId` 作为原子排序条件。ALLOW 并行执行中较早开始但较晚结束的旧执行，不能覆盖已由更新执行推进的统计状态。

## 41. 活跃业务唯一键使用主键墓碑删除标记

**记录日期**: 2026-07-22

Forge 不再通过可见 `logic_delete_active` 生成列、函数索引或部分索引表达“仅未删除记录唯一”。只有业务键要求未删除记录唯一且删除后允许同值重建时，才使用普通唯一索引 `(tenant_id, 业务键..., del_flag)`；没有业务唯一键或要求跨历史永久唯一的表不机械附加删除标记。

数值主键表使用 `BIGINT/Long del_flag`：`0` 表示有效，删除后由 `@TableLogic(value = "0", delval = "主键数据库列名")` 或自定义 Mapper 写入当前行主键。禁止固定写 `1`，否则同一业务键只能保留一条删除历史。字符串主键表使用同类型字段和专用原子删除 Mapper，禁止调用 MyBatis-Plus 通用字符串逻辑删除方法。

## 42. 持久化密钥轮换采用双读、单写切换和全租户归零门禁

**记录日期**: 2026-07-26

Forge 的 API 传输根密钥与数据库持久化 keyring 分离。持久化密文使用 `FPC1:<algorithm>:<keyId>:<payload>`，活动 keyId 只负责新写，历史 keyring 只负责读取；旧无版本密文仅在显式兼容开关和 legacy key 可用时读取。

轮换顺序固定为：全节点部署新旧双读版本且继续写旧格式，注入活动 keyring，确认全节点一致后切换版本化单写，逐租户 dry-run，受控执行迁移，再次盘点所有数据连接和非空低代码加密配置。只有 `LEGACY/HISTORICAL/UNKNOWN/UNKNOWN_KEY/BLOCKED/FAILED/CONFLICT` 全部归零，才允许关闭 legacy read 并移除旧钥。

一旦产生 `FPC1` 密文，禁止回滚到不识别该协议的版本。生产迁移、观察窗口、归零证据和旧钥退役属于部署门禁，不能由代码测试通过自动替代。

## 43. 新安装密钥使用启动前自动引导和外部稳定文件

**记录日期**: 2026-07-27

当传输根密钥未显式配置时，Starter Crypto 通过 `EnvironmentPostProcessor` 在 Spring 配置绑定前自动引导。优先级固定为“非空环境/JVM 配置 > 已有外部密钥文件 > 首次原子生成”。自动生成的传输根密钥和持久化活动密钥必须独立，后续启动只复用文件，禁止每次启动重生成。

本地默认文件为 `~/.forge/secrets/crypto.properties`；Docker Compose 使用 `crypto_secrets` 命名卷保存 `/var/lib/forge/secrets/crypto.properties`。POSIX 目录权限为 `0700`，密钥/锁文件为 `0600`；文件损坏或无法持久化时启动失败，不静默换钥。已有历史密文的 legacy key 无法从密文推导，仍必须由既有 Secret Manager 或部署配置提供。

## 44. 应用发布门禁按页面数据依赖判断，正式运行只读发布快照

**记录日期**: 2026-07-27

低代码应用发布不再无条件要求“必须且只能一个主对象”。纯内容应用允许 0 个业务对象；单对象自动规范为 `PRIMARY`；多对象应用只要每个数据页面和 `AiCrudPage` 都能通过显式引用、唯一对象或唯一主对象解析，就允许发布。页面级表单资产不等价于数据库业务对象，代码生成需要根对象时在生成阶段单独校验。

正式运行固定读取 `lastPublishVersion` 的不可变应用快照并按页面角色过滤；`edit=1` 和 `draft=1` 才读取工作台草稿。`navigationVisible=false` 只控制应用内导航展示，不等价于无访问权，隐藏但有权页面继续允许直达。超级管理员绕过页面角色过滤，无权或失效 `pageId` 回退到发布配置中的可访问首页。

页面设计器是普通用户的默认开发入口，对象创建/关联/导入和应用发布复用现有工作台面板以内嵌抽屉完成。对象变化只刷新应用与对象元数据，不能重载整张画布或覆盖未保存草稿；传统工作台继续作为自动化、权限、历史和兼容入口。

## 45. 页面表单保存时自动准备数据存储，业务对象退出普通主路径

**记录日期**: 2026-07-28

页面表单是普通用户可见的字段和布局事实。CRUD、左树右表和主子表等数据页面从模板创建后直接进入表单设计；当表单被未绑定的 `AiCrudPage` 使用且存在持久化字段时，保存表单由应用聚合服务自动准备内部数据存储，不再要求用户理解、创建或选择业务对象。

内部托管对象使用 `applicationId + formAssetId` 作为稳定身份，从默认可写 `LOWCODE_RUNTIME` 数据源创建或恢复，重复保存只同步字段和 `formDesignerSchema`；同一表单对应的未绑定 CRUD 统一回写显式对象引用。区块级或页面级手工数据绑定优先，自动准备不得改写已有对象设计。

业务对象、数据源、对象角色、已有数据复用和数据库导入统一放入高级数据设置。自动准备先提交设计草稿、运行配置和应用关联，再仅为来源身份一致的 `PAGE_FORM` 托管对象执行受控的安全追加式 DDL；不自动发布对象或应用。纯内容应用和没有持久化字段的表单继续允许 0 个对象。

## 46. 应用设计态 CRUD 默认静态预览，真实预览统一走草稿协议

**记录日期**: 2026-07-28

应用 `edit=1` 和 `draft=1` 加载对象渲染配置及实际 CRUD 端点时统一使用 `designPreview=1`，允许设计者查看最新草稿；普通正式运行不携带该标记，发布门禁与发布快照隔离保持不变。

设计画布默认只渲染字段、列表和表单结构，不自动访问数据接口。用户显式开启真实数据预览后才请求草稿 CRUD。这样保存表单不会被“对象尚未发布”阻断；自动托管数据表同步失败时也不会由画布立即追加一个查表异常。

## 47. 低代码内部编码由系统生成，模型编码统一服从数据库边界

**记录日期**: 2026-07-28

低代码普通创建路径不再要求用户维护应用编码。应用编码为空时，后端根据业务域和应用名称生成合法、租户内唯一的稳定编码，重名按 `_2`、`_3` 递增避让；显式编码继续校验并保留重复失败语义。创建接口返回应用 ID 和服务端最终编码，前端跳转不得使用提交前猜测值。应用编码只在高级设置中可选指定，创建后保持不可修改。

`ai_business_object.model_code` 与 `ai_lowcode_model.model_code` 继续维持 48 位数据库契约。后端归一化、组合命名和前端对象创建辅助函数统一截断到 48 位，不通过扩大字段掩盖生成规则漂移。并发应用编码竞争最终由租户唯一索引失败关闭。

## 48. 自动托管表单以物理数据表可用作为准备完成条件

**记录日期**: 2026-07-28

页面表单只有在元数据和对应物理表结构都已准备后，才能反馈“表单和数据存储已准备完成”。应用聚合服务先用独立事务提交托管对象、字段、表单设计和应用关联，事务提交后再调用既有数据库差异服务；MySQL DDL 的隐式提交不能伪装为与元数据原子回滚。

自动 DDL 仅接受 `managedBy=PAGE_FORM` 且来源应用、表单资产一致的系统托管对象，只执行现有白名单认可的建表和安全追加式变更。新对象必须选择启用、可写、非只读且 `allowRuntimeDdl=1` 的 `LOWCODE_RUNTIME` 数据源；历史托管对象同步以数据源当前真实能力和 DDL 预检为准，避免旧 `allowDdl` 快照阻止管理员开启开关后的重试。普通对象、导入表和已有表的手工同步权限与确认门禁保持不变。

DDL 失败不删除已提交的表单设计和托管对象，下一次保存按稳定表单身份复用并重试；字段删除、重命名、类型/长度/必填调整等非追加式差异转高级数据设置人工确认。

## 49. 应用发布前自动收敛本应用托管表的安全数据库差异

**记录日期**: 2026-07-28

应用发布检查和最终发布都是显式准备动作。进入 readiness 检查前，系统必须重新同步当前应用中来源身份一致的 `PAGE_FORM` 托管数据表，使首次建表、追加字段和同步状态刷新在同一发布链路内完成，避免用保存后的陈旧 `OUT_OF_SYNC` 状态阻断用户。

自动发布同步不扩大普通数据库同步权限：只有关联及对象自身均能证明属于当前应用、当前表单的自动托管对象才能执行安全追加式 DDL。手工对象、导入表、已有表、其它应用对象以及字段删除、改名、改类型、改长度等非追加式变化继续进入高级数据设置人工确认。普通概览查询不得隐式执行 DDL。

## 50. 发布抽屉保持轻量，权威检查只在显式检查或最终发布执行

**记录日期**: 2026-07-28

打开应用发布抽屉只加载版本和运行历史，不自动执行完整 readiness。用户需要提前定位阻断时显式点击“执行发布检查”；直接点击“发布应用”时由最终发布接口执行权威检查，避免默认连续执行两次对象、权限和数据库扫描。

应用发布仍保留对象提交前最终表完整性检查，以及发布运行的预留、认领、步骤开始/成功、失败和恢复状态更新。这些更新是幂等、并发认领和断点恢复证据，不能为减少 SQL 数量而删除。自动托管表同步可以复用同一设计版本的 `IN_SYNC` 证据，避免在最终对象校验之外增加一次无变化的 DDL 差异同步。

## 51. 页面查询方式作为受控元数据传输并由服务端重新校验

**记录日期**: 2026-07-28

页面可以局部配置查询字段、查询组件、查询方式和映射字段，但不反向修改共享业务对象的查询设计。用户输入继续使用平铺业务参数；查询方式通过保留控制参数 `_searchTypes` 随列表和导出请求传输，Controller 必须将其与业务字段隔离。

服务端只允许动态配置已经公开在 `searchSchema`、`columnsSchema`、`editSchema` 的字段以及 `id` 进入页面查询，并继续通过真实列映射生成命名参数 SQL。页面只能从固定操作符集合覆盖当前字段的默认查询方式；非法字段、非法操作符和损坏元数据不进入 SQL。没有页面级配置的传统动态页面继续使用对象原始 `searchSchema` 协议。

## 52. 能力发布控制面与外部执行开关解耦

**记录日期**: 2026-08-01

能力来源校验、发布和目录管理属于管理控制面，只要 Admin 引入对应插件就应装配，由权限、租户和发布模型校验限制。`forge.capability.secure-actions.enabled` 和 `forge.capability.flow-actions.enabled` 只控制 MCP/REST 真实执行目录、Handler 与执行适配器；关闭时仍必须失败关闭，但不得删除控制面路由。

## 53. 外围用户调用采用受信 Token Exchange，不绑定客户端服务账号

**记录日期**: 2026-08-01

`USER_DELEGATION` 客户端不绑定 Forge 服务账号或固定组织，只允许机密 OAuth 客户端提交受信 OIDC/JWT 做 RFC 8693 Token Exchange。首次认证按已验签 JWT 手机号唯一匹配现有 Forge 用户并固化 `issuer + sub` 映射，后续每次实时校验用户、租户、组织、角色和权限；平台不自动创建用户。`SERVICE/HYBRID` 保留原有绑定语义，HMAC 签名仅用于服务身份。

## 54. Capability Pepper 纳入外部稳定密钥自动引导

**记录日期**: 2026-08-01

Capability Client、Access Token 和 Authorization Code 三个 Pepper 复用 Starter Crypto 的启动前密钥引导。首次生成三个独立 32 字节随机 Base64Url 值并写入外部 `crypto.properties`，后续稳定复用；已有旧密钥文件在文件锁内原子补齐缺失项，不改动既有 Crypto 密钥。

非空环境变量/JVM 参数对每个 Pepper 独立优先，仓库不提交真实值。生产多实例必须通过共享 Secret Manager 或共享安全卷保持节点一致；显式关闭 Crypto Bootstrap 后不提供 Pepper 时，Capability 启动校验继续失败关闭。

## 55. 应用发布以实时物理表映射作为数据库门禁事实

**记录日期**: 2026-08-01

应用对象列表中的数据库同步状态只用于轻量展示，整体设计版本变化时不得据此断言物理表失步。应用发布检查必须通过 `BusinessObjectTableMappingService` 读取目标数据源的实时表、列、类型、索引与 DDL 预览，并以该结果作为数据库门禁事实。

Forge 标准系统列属于框架管理列：未在业务字段模型中重复声明时仍可在结构视图只读展示，但不算未映射业务列。自定义额外列、缺失业务列、类型不一致、表不存在、结构检查失败和待执行 DDL 继续失败关闭；发布问题消息必须给出可操作的具体差异，不能只返回 `OUT_OF_SYNC` 状态码。

## 56. 无统一 OIDC 的外围系统采用客户端签名用户断言与预绑定

**记录日期**: 2026-08-02

没有统一 OIDC 的外围系统不允许通过“加密 Forge userId”直接冒充真实用户。每个 USER_DELEGATION/HYBRID OAuth 客户端使用独立 RSA-2048 密钥对，Forge 只保存公钥、`kid` 和版本，私钥只在生成/轮换时通过加密响应展示一次。

管理员预先把外围稳定 `sub` 绑定到当前租户 Forge 普通用户，数据库只保存 `issuer/sub` SHA-256 和脱敏提示。外围系统签发最长两分钟的 RS256 JWT，通过专用 `urn:forge:params:oauth:token-type:user-assertion+jwt` 做 Token Exchange；Forge 固定校验签名、claims、Redis `jti` 防重放和预绑定关系，并每次重新加载用户组织、角色和权限。受信 OIDC JWT 保留原标准 token type，两种验签路径禁止模糊回退。

## 57. 已发布能力通过显式新版本升级，固定授权不自动漂移

**记录日期**: 2026-08-03

能力目录不提供对已发布版本的原地编辑。管理员通过“发布新版本”读取当前不可变快照，锁定能力编码、来源类型和来源标识，由受控发布器重新读取最新业务对象、流程绑定或系统服务配置，并发布严格递增的语义版本。旧版本继续保留，避免外围系统契约被静默改写。

`PINNED` 授权不会随能力发布自动漂移。授权管理提供显式修改入口，管理员可切换基准版本或改用 `FOLLOW_MAJOR`；服务端按目标版本重新校验字段和流程操作白名单，不能通过版本升级扩大既有客户端权限。

调用指南必须同时展示能力当前版本、授权基准版本和实际解析版本，并按实际解析版本生成请求示例。流程授权旧版本的 `bindingId/flowModelKey/publishedObjectVersion` 已与当前版本不一致时，在线测试前直接阻断并说明 `FLOW_BINDING_MISMATCH`；管理员可在指南中显式切换到当前版本，平台继续禁止发布动作静默修改授权。

## 58. 流程 START 能力只启动已保存业务记录

**记录日期**: 2026-08-03

`FLOW_ACTION/START` 的 `recordId` 必须指向绑定业务对象中已经保存的真实记录，START 不承担业务数据创建职责。记录查询继续使用实际委托用户的租户、组织和数据权限，不允许开放网关为了测试便利绕过权限或创建孤立流程实例。

记录不存在与无权访问统一对外返回 HTTP 404 + `RESOURCE_NOT_FOUND`，避免泄露记录存在性；这类错误属于业务资源定位失败，不得映射成 `SCHEMA_INVALID`。调用指南和测试页面必须明确提示先保存记录并替换真实 ID，示例使用不可误认为真实数据的占位符。

## 59. 外围业务申请使用 SUBMIT 组合能力，START 保持已有记录语义

**记录日期**: 2026-08-03

面向外围系统的“离职申请、请假申请”等业务语义使用 `FLOW_ACTION/SUBMIT`：调用方只传能力发布版本允许的业务字段，Forge 从可信 USER 委托身份生成申请人、租户、组织、审计、初始单据状态和流程发起人，并在一次调用中创建业务记录、启动主流程。`FLOW_ACTION/START` 继续作为高级集成动作，只启动已存在记录，不能根据任意外部数据隐式建单。

SUBMIT 的字段契约来源于不可变业务对象发布模型；能力版本保存字段白名单和必填快照，客户端授权只能进一步收窄可写字段且不能移除模型必填字段，调用指南按实际授权生成示例。运行时重新核对当前发布对象、流程绑定并经过低代码写入管线二次校验。相同 `Idempotency-Key` 下，业务记录创建和本地 `recordId` 检查点必须在同一独立事务提交，流程失败重试只能复用该记录。

当前仅主库低代码运行对象支持该原子组合能力。外部运行数据源无法和本地能力执行日志共享事务，注册、发布和执行都必须失败关闭；未来只有引入事务消息/Outbox 和可恢复状态机后才能放开，不能以“尽量写日志”替代幂等证据。

## 60. 业务动作只有通过发布快照步骤白名单才能开放

**记录日期**: 2026-08-03

业务动作的“启用”只表示设计态允许运行，不代表它已具备可安全开放的执行语义。开放候选必须从不可变业务对象发布快照读取，并通过与真实发布、运行时相同的 `SecureActionStepValidator` 校验。

当前中等风险受控动作只允许顶层 `UPDATE_FIELD` 和 `CREATE_RECORD`，空步骤、嵌套步骤、流程、消息、领域动作和其它未审核类型继续失败关闭。管理端不复制该白名单规则，由服务端返回 `publishable/unavailableReason/stepTypes` 诊断；页面只负责提前禁用并说明修正方式，直接 API 调用仍必须经过服务端安全校验。

## 61. 登录密码 RSA 与通用 API 传输加密采用独立策略

**记录日期**：2026-08-04

Forge 的通用 API 传输加密继续由配置中心 `crypto` 分组和匿名 `/crypto/config` 统一控制；Admin、H5、报表端只保留服务端配置的运行时镜像，不再维护产品级独立开关。

登录密码 RSA 改由 `login.enablePasswordEncryption` 独立控制，默认启用，并通过 `/auth/loginConfig` 下发。后端普通密码和密码验证码认证共享统一解码器：开启时 RSA 解密失败关闭，关闭时才接收应用层明文；客户端 `encrypted` 标记不作为信任依据。浏览器/H5/报表属于公共客户端，不保存或发送固定 AppSecret，生产环境无论是否启用密码 RSA 都必须使用 HTTPS。

## 62. Capability 使用专属父模块并收敛为四层依赖

**记录日期**：2026-08-04

Capability 不再以 7 个同级小插件散落在 `forge-plugin-parent` 下，统一由 `forge-plugin-capability-parent` 聚合四个子模块：`core`、`platform`、`actions`、`high-risk-approval`。父模块只负责 Maven 聚合，不放业务代码。

依赖方向固定为 `core ← platform ← actions ← high-risk-approval`。控制面、Identity 和 Open Gateway 归入 platform；Secure Actions 与 Flow Actions 归入 actions；高风险审批继续独立并默认关闭。开放网关只依赖 core 中的通用执行 SPI，通过 Spring 收集 actions 提供的适配器，禁止 platform 反向依赖 actions、generator 或 flow-client。Java 业务包、REST 路径、配置前缀和数据库表保持兼容。

## 63. 业务域删除显式清理孤立业务对象

**记录日期**：2026-08-04

删除业务应用继续保留业务对象，便于对象被同一业务域内其他应用复用；不能为了让业务域可删而改变应用删除语义。业务域已无子域、业务应用和访问入口，仅剩未被有效应用引用的业务对象时，删除入口必须明确展示对象数量和清理边界，并显式传入孤立对象清理意图。

后端默认不级联，收到显式清理意图后在同一事务内再次校验有效应用引用，物理清理 `ai_business_object_relation` 关系重建数据，按主键墓碑逻辑删除 `ai_business_object` 和 `ai_business_suite`。动态业务数据表、设计/发布历史和运行日志不做物理删除；存在有效应用引用时失败关闭。

## 64. 后端数据库性能优化模式（P1 归档）

**记录日期**：2026-08-05

后端分页查询性能优化遵循以下固定模式，后续 P2/P3 变更继续复用：

1. **N+1 查询消除**：分页后循环单条查询改为一次 IN 批量查询 + Map 组装。新增 Mapper 方法返回聚合统计 VO（如 `ReceiverStatVO`），XML 用 `GROUP BY` + `SUM(CASE WHEN ...)` 一次聚合。
2. **LIKE 逗号列表匹配改 FIND_IN_SET**：`column = userId OR column LIKE 'userId,%' OR column LIKE '%,userId' OR column LIKE '%,userId,%'` 统一改为 `FIND_IN_SET(#{userId}, column)`，单条件替代 4 路 LIKE。
3. **JOIN OR 索引失效消除**：`LEFT JOIN t c ON c.id = m.col OR c.code = m.col` 统一存储为 ID，JOIN 条件改为 `c.id = m.col`。存量数据用 Flyway `UPDATE ... INNER JOIN ... SET` 迁移。
4. **全量加载改按需查询**：`service.list()` 全量加载改为按需 `listByIds` / `lambdaQuery().eq()`。需要祖先链补齐时用 `loadResourcesWithAncestors` 迭代查询模式（`listByIds` + 收集 parentId + 循环直到无新 ID，`put` 返回非 null 防死循环）。
5. **子查询改 LEFT JOIN 派生表聚合**：分页中每行相关子查询（COUNT/SUM）改为 `LEFT JOIN (SELECT session_id, COUNT(*) ... GROUP BY session_id) r ON r.session_id = s.id`，派生表先聚合再 JOIN，避免结果集膨胀。
6. **统计查询加时间范围**：全表 COUNT/SUM 统计增加 `WHERE create_time >= DATE_SUB(NOW(), INTERVAL 90 DAY)` 时间过滤，减少扫描行数。前端文案需同步标注时间范围。

## 65. 低代码对象定义数据，应用定义数据的使用方式

**记录日期**：2026-08-07

Forge 低代码能力按“业务对象资产 + 应用编排交付”收口。业务对象回答“数据是什么”，拥有字段、物理表映射、关联关系与级联语义、校验/公式/编号、可复用表单资产，以及 owner、组织、创建人等可授权字段声明；应用回答“用户怎样使用数据”，拥有页面、列表与查询视图、详情布局、业务流程、自动化、角色与字段权限、数据范围、入口、版本和运行治理。

列表设计继续保留，但正式归属应用页面/视图，同一对象在不同应用可以有不同列表；对象只提供默认列表预设和字段目录。对象间 ER 关系继续归对象，应用只配置关系导航和展示。数据权限采用“对象声明可授权字段 + 应用配置角色策略”的两层合同，运行时继续复用租户、当前组织和 DataScope，不建设第二套身份体系。

Flowable 审批模型属于租户级可复用流程资产。应用业务流程的审批节点可以直接选择当前可信租户下已发布、已部署、正版本的审批模型，不要求主对象预先存在 `FLOW Binding`；应用发布快照固定模型版本和部署标识。旧对象流程绑定只作为存量运行兼容和迁移来源，不再作为新应用审批目录的前置条件。

信息架构迁移必须渐进完成：先建立应用侧唯一写入口和兼容读取，再把对象侧流程、自动化、列表和应用权限入口降为只读迁移入口，最后移除旧入口；不得直接删除存量配置或让运行中实例失去来源。

## 66. 字段查询事件归应用表单并只引用受管查询源

**记录日期**：2026-08-10

低代码页面打开、change、blur、手动查询和扫码完成后的只读查询回填，统一存放在应用表单 `formDesignerSchema.settings.governance.fieldEvents`，随不可变发布快照交付。业务对象继续只定义字段和存储，不承担“在哪个页面、何时查询、回填到哪里”的应用行为。

字段事件只允许引用稳定键标识的 `EXTERNAL_API`、`DATASET`，参数只从显式表单字段、最小只读运行上下文或路由参数映射，结果只写入显式目标字段。设计器和运行时都不得接受任意 URL、Header、认证、凭据、SQL、脚本或 handler；发布检查对主表单和嵌套表单统一失败关闭。

浏览器上下文中的 userId、tenantId、activeOrgId、工号或门店等值只可作为业务查询条件，不能作为授权依据。服务端仍以可信登录 Session、租户、ACL、数据权限和查询源权限为准。统一运行时负责防抖、取消、过期响应隔离、空结果与字段级状态，H5 和扫码组件后续复用同一事件入口。

## 67. 事务型低代码命令复用发布态 BusinessAction 并显式划分一致性边界

**记录日期**：2026-08-11

低代码写操作继续增强现有 `BusinessActionExecutionService`，不建设第二套命令引擎。正式执行只能读取不可变业务对象发布快照，并使用 tenant、object、record、action、发布版本和幂等键组成唯一域；页面确认生成安全幂等键，同一表单载荷失败重试复用，修改输入后换新键。

`LOCAL_TRANSACTION` 只允许 Forge 主数据源内的 `CREATE_RECORD`、`UPDATE_FIELD`、`ADJUST_NUMBER` 及只嵌套这些步骤的 `FOREACH`，共享单一事务并要求失败回滚。流程、消息、领域动作、外接数据库或其它外部副作用统一归 `ORCHESTRATION`，只承诺顺序、幂等和审计，不宣称跨系统自动回滚。

动作输入以 `inputSchema` 为边界。显式 `inputSchema: []` 表示不接受任何表单输入，只有完全缺失该字段的存量动作保留旧输入兼容；浏览器只可提交 `routeQuery` 普通参数，当前记录必须由服务端按 `recordId` 重读，用户、租户和组织等身份只来自可信 Session/SYSTEM 上下文。设计器以执行模式、输入字段和本地数据步骤卡片为普通入口，高级 JSON 仅作兼容兜底并继续接受相同后端失败关闭校验。

## 68. 子表行动作复用 BusinessAction 并以 relationKey 固化父子上下文

**记录日期**: 2026-08-11

低代码子表行按钮不建设专用接口或第二套动作引擎，统一复用已发布 `BusinessAction` 的 `COMMAND`、输入 Schema、权限、幂等、事务和执行日志。设计态使用 `CHILD_ROW` 位置和稳定 `relationKey`；发布时把动作投影到对应 `masterDetailConfig.children[].rowActions`，执行时只相信不可变发布版本的关系快照。

浏览器只提交父记录 ID、子记录 ID、relationKey、声明输入、routeQuery 和幂等键，不提交完整父/子行。服务端按父 ID 重读权威详情，校验子行属于父记录后构建 `record/row`、`parentRecord/parent` 和 `SYSTEM` 父子字段。未保存子行直接禁用，跨父子或关系不一致请求在副作用前失败关闭。
### 69. 低代码扫码统一走协同容器运行时与字段事件（2026-08-10）

企业微信/H5 的扫码能力采用前端通用 `collaboration-runtime` 抽象：企业微信 `wx.scanQRCode` 只作为可选宿主适配器，其他容器和 H5 由宿主注入 scanner；扫码结果归一化为 `value/type/platform`，通过既有 `SCAN_COMPLETE` 字段事件查询受管 `EXTERNAL_API`/`DATASET`。客户端不得提交或覆盖 `userId`、`tenantId`、`activeOrgId` 等可信身份；服务端继续以 Session、租户和查询源 ACL 为准。这样手机号查会员、条码查商品和静态码查收款可由低代码配置完成，不把具体业务接口耦合进平台。
### 70. 低代码离线只保存草稿并以发布/记录版本门禁重放（2026-08-10）

离线能力不直接写服务端或模拟已保存记录，浏览器只保留绑定发布版本、schemaHash 和记录版本的草稿，以及以幂等键去重的待重放意图。恢复网络后必须由调用方先读取当前版本和权限，再逐条交给既有 BusinessAction 接口；冲突、不可用记录和执行失败均停止并标记，不能自动覆盖或无限重试。本地存储清洗 token、密码、Secret、租户/用户身份等键，避免把浏览器自报身份变成可信上下文。

### 71. 普通单据状态、金额与离线提交统一复用发布态治理协议（2026-08-11）

普通低代码单据不为预售、会员、商品或收款建设专用状态接口。状态变更统一使用发布态 `BusinessAction` 的 `TRANSITION_STATUS`，把固定 `from/to` 与目标字段固化到不可变动作协议，并在同一条条件 UPDATE 中完成 expected-status 校验；成功和失败尝试都写脱敏结构化 `outcome`。金额输入使用 `MONEY` 十进制定点协议，当前固定为展示单位输入、最小货币单位 `long` 存储，超 scale、负数和溢出失败关闭。

`AiCrudPage` 的断网提交不直接调用普通 CRUD，也不把本地草稿当服务端事实。发布配置只声明草稿和可选重放动作，恢复在线后重新读取最新发布版本、schemaHash、服务端记录版本与权限，由用户显式确认后交给既有幂等业务动作执行。动作审计日志保留可信操作者引用、版本、幂等键、状态摘要和留存截止时间，不保存目标记录 ID、表单原值、凭据或外部响应。

## 72. 应用入口权限与移动菜单按客户端统一治理

**记录日期**：2026-08-16

应用入口的访问范围统一由应用权限工作台和角色资源授权控制，不再使用没有运行时鉴权语义的 `visibleScope`。`mobileScene` 同样不再作为入口行为配置；移动入口打开列表、填报或详情由应用入口的“打开内容”协议决定。存量字段可以兼容保留，但新编辑界面不再暴露或写入。

移动入口同步系统菜单时固定写入 `client_code=h5`，管理端入口固定写入 `client_code=pc`。移动菜单使用顶级父节点，不能挂到 PC 业务域目录；业务域恢复、入口从管理端切换到移动端等更新路径都必须重新校正父节点、移动运行路径和客户端编码。产品界面统一称“移动端”，`H5/h5` 只作为数据库、接口和构建条件中的兼容技术编码保留。

## 73. 受管缓存使用 Forge 自有注解运行时和异步控制面

**记录日期**：2026-08-17

Forge 缓存统一治理采用自有 `@ForgeCacheConfig`、`@ForgeCacheable`、`@ForgeCachePut`、`@ForgeCacheEvict` 和 Spring AOP/SpEL，不启用 Spring Cache，也不实现其 `CacheManager`。有效策略优先级固定为“数据库管理覆盖 > 代码注解默认 > starter 全局默认”；系统插件只在启动和管理操作时同步数据库覆盖到 Redis 控制面，业务调用只读取本地策略快照，禁止逐次查询数据库。

缓存键由代码声明可信作用域并对作用域材料和业务键做 SHA-256 摘要；缺少 TENANT/USER/ORG 所需可信身份时必须绕过缓存。Put、Evict 和事务内 Cacheable 回填在事务提交后执行，回滚不改变缓存。MULTI 采用 Caffeine 本地层与 Redisson 失效通知，属于有界最终一致，严格实时数据使用 REDIS 或停用缓存。

缓存基础设施异常、非法 SpEL 和非法运行覆盖均失败开放：业务方法继续执行，非法覆盖回退代码默认，且一次调用最多执行业务方法一次。管理端只能调整代码允许范围内的启停、模式、TTL、容量和空值策略，不能修改 scope、key 表达式或允许模式；停用、恢复默认和关键策略变化时主动清空旧缓存。
