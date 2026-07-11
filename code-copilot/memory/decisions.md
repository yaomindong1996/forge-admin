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
