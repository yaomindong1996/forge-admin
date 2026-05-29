# 低代码业务应用平台业务闭环补齐
> status: in_progress
> created: 2026-05-28
> complexity: 🔴复杂

## 1. 背景与目标

`lowcode-business-app-platform` 第一阶段已经完成应用中心、业务套件、业务对象、对象关系、应用入口、能力挂接、移动端中心、集成中心和 CRM 样板入口。但从业务人员视角看，当前能力仍更接近“业务化目录 + 低代码包装”，还没有完全达到客户真实诉求：

- 做 CRM 时，只看 CRM 相关业务对象和应用能力。
- 业务前端要能高度个性化，平台只保留组织、权限、流程、审批、报表、消息、数据接口等底座。
- 平台能力要拆成可拼接的引擎：流程引擎、审批引擎、报表引擎、权限引擎、消息引擎。
- 应用层围绕单据和业务对象建设，例如客户、商机、合同、回款、跟进记录。
- 后续要能快速接入大屏看板、H5、嵌入式页面、标准接口、Webhook、企微、飞书、钉钉等外部平台。

本变更目标不是再新增一批入口页面，而是把第一阶段“能看见”推进到“能交付、能运行、能扩展”：

`CRM 业务闭环 → 对象自助搭建闭环 → 引擎能力最小闭环 → 嵌入/H5/集成闭环 → 业务验收口径`

完成后，业务人员从“应用中心 → CRM”进入，应至少能完成以下闭环：

- 客户、联系人、商机、合同、回款等核心对象可以运行标准列表、表单、详情、导入、导出。
- 客户详情能看到联系人、商机、跟进记录等关联入口，不再只是关系配置展示。
- 合同可以看到审批接入状态；若流程已配置，可发起审批；若未配置，给出明确配置路径。
- 商机阶段提醒、合同金额汇总、回款逾期提醒等触发器不再只是能力标签，至少有可执行或可验证的最小动作。
- 销售看板或经营分析大屏可以作为嵌入应用安全打开。
- H5、移动待办、移动审批、移动业务入口可以按 CRM 套件统一管理和打开。
- 标准接口、Webhook、企微/飞书/钉钉至少形成一个通用出站事件和推送日志闭环。

## 2. 代码现状（Research Findings）

### 2.1 第一阶段已具备的业务化入口

- `code-copilot/changes/lowcode-business-app-platform/spec.md` 已标记第一阶段完成，并明确第一阶段定位是“业务化应用平台模块”，不是重写低代码运行时。
- `forge-admin-ui/src/views/app-center/index.vue` 已有应用总览、业务套件筛选、业务对象列表、应用入口列表、引擎中心、移动端、集成入口。
- `forge-admin-ui/src/views/app-center/suite.[suiteCode].vue` 已能按套件展示业务对象和场景入口。
- `forge-admin-ui/src/views/app-center/object.[objectCode].vue` 已有对象详情、运行态提示、配置模型、配置布局、发布应用、导入模板、导入、导出、关系、能力页签。
- `forge-admin-ui/src/views/app-center/engines.vue` 已展示流程、审批、报表、权限、消息、触发器、导入导出等引擎卡片。
- `forge-admin-ui/src/views/app-center/mobile.vue` 已登记 H5、移动待办、移动审批、移动业务入口。
- `forge-admin-ui/src/views/app-center/integration.vue` 已登记标准接口、Webhook、企微/飞书/钉钉、外部系统等集成入口。

### 2.2 第一阶段已具备的后端协议

- `BusinessSuiteController` 提供业务套件分页、列表、详情、汇总和同步低代码领域接口。
- `BusinessObjectController` 提供业务对象分页、列表、详情、运行态信息、创建、修改、启停、删除和同步低代码模型接口。
- `BusinessObjectRelationController` 提供对象关系查询、保存和删除接口。
- `BusinessAppController` 提供应用入口分页、列表、详情、打开信息、创建、修改、启停、删除和同步已发布低代码应用接口。
- `BusinessBindingController` 提供能力挂接列表、新增、修改、删除和批量保存接口。
- `BusinessAppOpenService` 已对应用入口打开做状态、权限、目标地址、域名白名单和敏感 URL 参数校验。

### 2.3 CRM 样板现状

- `forge/db/migration/V1.0.28__seed_crm_business_suite.sql` 已初始化 CRM 套件、9 个业务对象、对象关系、对象能力挂接和 8 个标准业务应用入口。
- CRM 对象包括客户、联系人、线索、商机、合同、合同明细、回款、跟进记录、销售任务。
- CRM 关系包括客户-联系人、客户-商机、商机-合同、合同-合同明细、合同-回款、客户-跟进记录、商机-跟进记录。
- CRM 能力挂接包括导入、导出、报表、触发器、审批、消息、权限。
- `forge/db/migration/V1.0.32__seed_crm_customer_runtime_link.sql` 仅明确补齐了客户对象的低代码模型和 `crm_customer` 运行态配置。
- `forge/db/migration/V1.0.33__seed_crm_other_models.sql` 初始化了其他 CRM 对象的低代码模型，但不等同于所有对象都已经具备真实运行态 CRUD 配置和物理数据表。

### 2.4 当前业务缺口

- 应用入口已经按 CRM 展示，但 CRM 多数对象仍可能只是对象、模型和入口配置，未必具备可运行数据表和 CRUD 配置。
- 对象关系当前主要用于“关系展示”，还没有形成客户详情下联系人/商机/跟进记录列表等业务使用体验。
- 能力挂接当前主要是能力标签和配置入口，不能把“已接入”直接理解成“已可运行”。
- `BusinessObjectWizardDrawer` 中“从模板创建、从数据库表导入、从 AI 描述生成”当前文案仍是预留或跳转，不是完整自助搭建闭环。
- 引擎中心当前多数卡片跳到原有配置页面，缺少对象维度的运行状态、缺口提示和最小执行动作。
- 移动端中心当前只做入口登记，不提供 CRM 移动业务最小运行路径。
- 集成中心当前只做入口定义，不具备标准事件、Webhook 推送、失败日志、重试和第三方平台通道闭环。
- 嵌入应用已有 `open-info` 安全校验，但缺少统一 iframe 容器、业务化错误页和大屏类入口验收闭环。
- CRM 入口和关系入口存在目标错配风险：例如点击“客户跟进记录/跟进记录”时，不能进入客户管理，必须进入跟进记录对象对应的运行页。
- 生成出的客户管理运行页存在字段名、列标题或表单项显示为 `undefined` 的问题，说明运行配置 Schema 和前端渲染兜底需要补齐校验。
- 应用中心的对象关系和低代码模型里的关联配置需要保持一致，不能形成两套互相不生效的关系配置。
- `app-center/suite/:suiteCode` 业务套件详情页目前按模块纵向堆叠，内容多时浏览成本高，需要改成更适合扫描和运营的布局。

## 3. 产品定位

本变更把应用中心从“业务化配置目录”推进到“可交付业务装配工作台”。

### 3.1 面向业务人员

业务人员不需要理解表名、配置键、JSON 或发布版本。业务人员关心：

- CRM 是否能用。
- 客户、联系人、商机、合同、回款能不能录入和查询。
- 合同审批、回款提醒、销售看板能不能接上。
- 后续能不能把企业已有大屏、H5 或第三方平台接进来。

### 3.2 面向实施人员

实施人员需要看到“这个业务对象距离可交付还差什么”：

- 是否有数据表。
- 是否有低代码模型。
- 是否有已发布运行配置。
- 是否有关联应用入口。
- 导入导出是否可用。
- 审批、报表、消息、权限、触发器是否只是挂接，还是已能运行。

### 3.3 面向开发人员

开发人员继续使用低代码模型设计、搭建器、发布、动态 CRUD、代码预览和下载。应用中心只做业务编排和可交付闭环，不复制低代码底层能力。

## 4. 范围

### 4.1 本阶段必须完成

- CRM 核心对象最小可运行闭环：客户、联系人、商机、合同、回款。
- CRM 入口目标修正：点击跟进记录、客户跟进记录等入口时，必须进入目标对象运行页，不能错开到客户管理。
- CRM 运行页 Schema 质量修正：客户管理等生成页面不得出现 `undefined` 字段、列名、表单标签或按钮文案。
- 业务对象就绪度检查：数据表、模型、运行配置、应用入口、导入导出、关系、能力挂接。
- 对象详情关系运行视图：客户下联系人/商机/跟进记录，合同下合同明细/回款。
- 关系配置闭环：应用中心的“关系配置”必须能直接维护业务对象关系，并与模型关联配置保持同步或一致映射。
- 对象自助搭建主流程补齐：空白对象、模板对象、数据库导入、AI 生成四种入口必须有明确下一步和状态回写。
- 引擎中心从“固定入口”升级为“按对象统计接入状态和可运行状态”。
- 业务套件详情页布局优化：`app-center/suite/:suiteCode` 不能只按对象、验收、入口纵向堆叠，需要支持内容较多时的横向分区、页签或分栏浏览。
- 合同审批最小闭环：绑定流程存在时可发起；不存在时给配置路径。
- 报表/大屏最小闭环：销售看板或经营分析大屏可作为安全嵌入应用打开。
- 消息/触发器最小闭环：至少支持商机阶段变化提醒、回款逾期提醒、合同金额汇总其中的可执行动作或可验收模拟。
- 移动端入口最小闭环：CRM H5、移动审批、移动待办、移动业务入口能登记、校验、打开，并展示可见范围。
- 集成最小闭环：标准事件、Webhook 订阅、推送日志和失败重试；企微/飞书/钉钉作为通道类型，不保存明文密钥。

### 4.2 本阶段不做

- 不重写动态 CRUD 运行时。
- 不重写低代码模型设计器和页面搭建器。
- 不一次性实现完整 CRM 行业套件。
- 不建设完整移动 App 或完整 H5 页面搭建器。
- 不一次性打通企微、飞书、钉钉所有接口能力。
- 不新建独立工程。
- 不把 `ai_business_*` 表变成低代码运行配置事实来源。

## 5. 功能点

### 5.1 CRM 核心对象可运行闭环

- [ ] CRM 客户、联系人、商机、合同、回款必须有真实可用的物理数据表、低代码模型、运行配置和应用入口。
- [ ] 已配置 `configKey` 但找不到运行配置时，应用中心必须提示“运行配置缺失”，不能让业务用户打开后报错。
- [ ] 客户、联系人、商机、合同、回款必须支持标准列表、查询、新增、编辑、删除、导入模板、导入、导出。
- [ ] 金额字段统一使用分为单位的 long/bigint，不新增 decimal 金额字段。
- [ ] CRM 对象的运行入口必须从“应用中心 → CRM 套件 → 业务对象/应用入口”可达。
- [ ] 未完成运行态的对象必须显示具体缺口和下一步，不允许只显示灰色按钮。
- [ ] 点击“跟进记录”“客户跟进记录”“商机跟进记录”入口时，目标必须是 `crm_follow_record` 运行页或跟进记录对象详情，禁止落到 `crm_customer` 客户管理页。
- [ ] `RUNTIME` 类型应用入口打开必须以 `ai_business_app.object_code + config_key + entry_url` 三者一致为准；三者不一致时返回不可打开状态并提示修复配置。
- [ ] 客户管理生成页的搜索项、表格列、表单项、详情项必须都有有效 `field/prop` 和 `label`，页面上不得出现 `undefined`、空白列标题或无法提交的字段。
- [ ] 后端保存或发布 `ai_crud_config` 时必须校验 `search_schema`、`columns_schema`、`edit_schema`、`model_schema` 的字段映射完整性；历史初始化脚本需要补齐或修正客户管理 Schema。

### 5.2 对象关系运行视图

- [ ] 客户详情展示联系人、商机、跟进记录三个关联列表入口。
- [ ] 合同详情展示合同明细和回款两个关联列表入口。
- [ ] 商机详情展示合同和跟进记录关联列表入口。
- [ ] 关联列表至少支持查看关联数据、跳转到目标对象运行页并带入关联筛选条件。
- [ ] 第一阶段可不在对象详情内重写完整明细编辑器，但必须给出可操作入口。
- [ ] 关系运行入口必须读取关系配置中的源对象、目标对象、源字段、目标字段和目标对象运行入口，不能默认回退到当前对象或客户管理入口。
- [ ] 对象关系配置页必须支持直接维护关系：关系类型、源对象、目标对象、源字段、目标字段、详情页签名称、默认筛选条件、排序和启停状态。
- [ ] 低代码模型已存在关联配置时，应用中心关系配置应能自动带出；在应用中心新增或修改关系时，也要能回写或同步到模型关联元数据。
- [ ] 当模型关联配置与 `ai_business_object_relation` 不一致时，页面必须提示差异并提供“以模型为准/以业务关系为准”的修复动作，不能静默使用错误关系。

### 5.3 业务对象就绪度

- [ ] 新增对象就绪度接口，返回模型、表结构、运行配置、应用入口、导入导出、关系、能力挂接的状态。
- [ ] 对象详情顶部显示就绪度摘要，例如“可运行”“缺少数据表”“缺少发布配置”“能力仅已登记”。
- [ ] 就绪度必须区分“已登记”“已配置”“可运行”“执行失败”。
- [ ] 应用入口打开前必须校验 `configKey` 对应 `ai_crud_config` 是否存在且可用。

### 5.4 自助搭建主流程

- [ ] 从空白对象创建：保存对象档案后，进入模型配置和发布流程。
- [ ] 从模板创建：选择模板后生成对象、模型草稿和默认应用入口。
- [ ] 从数据库表导入：复用低代码数据库表导入能力，完成后回写 `ai_business_object.model_id/model_code`。
- [ ] 从 AI 描述生成：复用低代码 AI 生成链路，完成后回写业务套件、业务对象和应用入口。
- [ ] 每个创建方式都要有业务化步骤条和返回应用中心的路径。

### 5.5 引擎能力最小闭环

- [ ] 引擎中心显示每类引擎的总接入数、可运行数、待配置数、异常数。
- [ ] 对象能力页签中，能力卡片必须展示“已登记/可配置/可运行/不可用”的状态。
- [ ] 合同审批：若 `APPROVAL` 绑定的流程存在，可从合同记录发起审批；若不存在，提示去流程配置。
- [ ] 报表能力：若 `REPORT` 绑定有入口地址，可从对象或套件打开报表；若没有，提示配置报表。
- [ ] 消息能力：至少支持从对象事件生成站内消息或模拟发送记录。
- [ ] 权限能力：对象详情显示当前对象的数据权限策略摘要，并链接到现有权限配置。
- [ ] 触发器能力：支持少量内置动作，不引入完整复杂规则引擎。

### 5.6 嵌入应用与大屏闭环

- [ ] 新增统一嵌入页面容器，iframe 应用不直接裸跳外部地址。
- [ ] iframe、外部链接、H5 必须通过后端 `open-info` 做状态、权限、白名单和敏感参数校验。
- [ ] 销售看板或经营分析大屏可作为 CRM 套件下的 `EMBEDDED` 应用入口展示并打开。
- [ ] 嵌入失败时展示业务化错误：未授权、未配置白名单、地址不可访问、缺少 SSO。
- [ ] 第一阶段可不实现 SSO 和 postMessage 深度交互，但必须预留配置字段。

### 5.7 移动端入口闭环

- [ ] CRM 移动入口按 H5 入口、移动待办、移动审批、移动业务分类。
- [ ] 移动入口保存可见范围：全部用户、指定角色、指定部门、负责人范围。
- [ ] 移动审批和移动待办优先复用流程、审批和消息中心，不新增独立待办来源。
- [ ] 移动入口打开时必须校验权限和白名单，不允许长期 Token 出现在 URL。
- [ ] 第一阶段不做完整移动端搭建器，但必须能展示“当前入口通向哪里、谁可见、是否可打开”。

### 5.8 集成与第三方推送闭环

- [ ] 定义业务事件：对象创建、对象更新、状态变更、审批提交、审批通过、审批驳回、回款逾期。
- [ ] 支持按业务套件、业务对象和事件类型配置 Webhook 订阅。
- [ ] 支持企微、飞书、钉钉作为通道类型登记，但密钥必须使用安全配置引用，不存明文。
- [ ] 每次推送生成事件日志，记录状态、响应、失败原因和重试次数。
- [ ] 支持失败重试和手动重推。
- [ ] 第一阶段至少打通通用 Webhook 推送闭环；企微/飞书/钉钉可先作为通道适配器框架。

### 5.9 业务验收工作台

- [ ] CRM 套件详情新增“交付验收”视角，按对象展示运行状态、关系状态、引擎能力状态、渠道接入状态。
- [ ] 支持一键检查 CRM 样板是否达到最小交付标准。
- [ ] 检查结果必须能定位到下一步操作，例如“去配置模型”“去发布应用”“去配置流程”“去配置白名单”。

### 5.10 业务套件详情页布局优化

- [ ] `app-center/suite/:suiteCode` 需要从纯纵向区块堆叠升级为“概览 + 分区导航”的工作台布局。
- [ ] 桌面端优先采用顶部摘要、左侧分区导航或页签、右侧内容区的结构，业务对象、交付验收、场景入口、引擎能力、移动入口、集成入口可以快速切换。
- [ ] CRM 对象和入口数量较多时，业务对象和场景入口必须支持分页、搜索、筛选和紧凑卡片/表格切换，避免页面无限向下滚动。
- [ ] 交付验收结果应作为摘要状态常驻在首屏，不应把长列表完整压在所有内容中间。
- [ ] 移动端可以保持单列布局，但需要保留分区锚点或折叠面板，避免所有模块一次性铺开。

## 6. 业务规则

- 业务用户默认只看到应用中心、CRM 套件、业务对象、业务入口和业务能力，不看到 JSON、Schema、表名、`configKey` 等技术细节。
- “已登记”不等于“可运行”。所有能力状态必须区分登记、配置、运行和异常。
- CRM 对象如显示“可打开”，必须保证后端运行配置存在，否则只能展示下一步配置。
- 应用入口是访问入口，业务对象是业务主资产，二者不能混作同一个概念。
- 对象关系必须用业务语言展示，禁止把外键、Join、Schema 作为业务主文案。
- 对象关系配置是业务应用关系和模型关联配置的统一维护入口之一，关系字段、目标对象和运行入口必须可追溯到模型或运行配置。
- 关系入口跳转必须以目标对象为准。跟进记录关系只能进入跟进记录，不能因为源对象是客户而打开客户管理。
- 导入导出必须复用现有动态 CRUD 导入导出能力。
- 流程、审批、报表、消息、权限优先复用现有引擎，不新建第二套引擎。
- 触发器只做轻量对象自动化，不承担复杂流程编排。
- 外部地址、H5、iframe 必须经过权限和白名单校验。
- 第三方平台密钥、Token、Webhook Secret 禁止写入 URL、`options` 明文 JSON 或日志。
- 金额字段使用 long/bigint，单位为分。
- 所有内置数据 `tenant_id=1`。
- 低代码运行页不得展示 `undefined`、`null`、空 label 或空字段名；发现 Schema 缺失时必须在配置期或打开前拦截并给出修复提示。

## 7. 数据变更

| 操作 | 表名 | 字段/索引 | 说明 |
|------|------|-----------|------|
| 新增 | `crm_customer` | CRM 客户字段 + 标准审计字段 | 客户运行态示例表，若已存在则补齐字段 |
| 新增 | `crm_contact` | `customer_id` + 联系人字段 + 标准审计字段 | 联系人运行态示例表 |
| 新增 | `crm_opportunity` | `customer_id` + 阶段/金额分/概率字段 + 标准审计字段 | 商机运行态示例表 |
| 新增 | `crm_contract` | `customer_id`、`opportunity_id`、`amount_cent`、状态字段 + 标准审计字段 | 合同运行态示例表 |
| 新增 | `crm_payment` | `contract_id`、`amount_cent`、回款日期、状态字段 + 标准审计字段 | 回款运行态示例表 |
| 新增 | `crm_contract_item` | `contract_id`、`amount_cent`、数量、单价分 + 标准审计字段 | 合同明细运行态示例表 |
| 新增 | `crm_follow_record` | `customer_id`、`opportunity_id`、跟进内容 + 标准审计字段 | 跟进记录运行态示例表 |
| 新增 | `ai_business_event_subscription` | 订阅目标、事件类型、通道类型、状态 | 集成事件订阅 |
| 新增 | `ai_business_event_log` | 事件、目标、请求摘要、响应摘要、状态、重试次数 | 集成推送日志 |
| 新增 | `ai_business_action_log` | 对象、记录、动作类型、执行状态、错误信息 | 触发器/审批/推送等业务动作日志 |
| 扩展 | `ai_business_binding` | 不新增运行配置事实来源 | 继续只保存能力挂接和配置引用 |
| 扩展 | `ai_business_app` | `options` 增加 SSO/postMessage/可见范围等配置 | 不保存明文密钥 |

数据脚本要求：

- 所有 CRM 示例表使用 `CREATE TABLE IF NOT EXISTS`。
- 新增列、索引前必须检查 `information_schema`。
- 所有初始化数据用 `INSERT ... SELECT ... WHERE NOT EXISTS`。
- CRM 金额字段统一使用 `amount_cent`、`unit_price_cent` 等分单位字段。
- 不修改已经执行过的历史迁移脚本；通过新增版本脚本修正。

## 8. 接口变更

### 8.1 就绪度与运行检查

| 操作 | 接口 | 方法 | 说明 |
|------|------|------|------|
| 新增 | `/ai/business/object/{id}/readiness` | GET | 查询业务对象就绪度 |
| 新增 | `/ai/business/suite/{suiteCode}/acceptance` | GET | 查询套件交付验收状态 |
| 增强 | `/ai/business/object/{id}/runtime-info` | GET | 增加运行配置存在性和导入导出可用性 |
| 增强 | `/ai/business/app/{id}/open-info` | GET | 校验 `configKey` 对应运行配置是否存在 |

### 8.2 对象关系运行视图

| 操作 | 接口 | 方法 | 说明 |
|------|------|------|------|
| 增强 | `/ai/business/object/{objectId}/relations` | GET/POST | 查询和保存关系配置，支持关系字段、目标入口、详情页签和默认筛选条件 |
| 新增 | `/ai/business/object/{objectId}/relations/sync-model` | POST | 同步模型关联配置与业务对象关系配置，返回差异和修复结果 |
| 新增 | `/ai/business/object/{objectId}/relation-runtime` | GET | 查询对象关系运行入口 |
| 新增 | `/ai/business/object/{objectId}/relation-runtime/{relationId}/query` | GET | 查询关联列表数据或返回目标运行页筛选参数 |

### 8.3 引擎能力

| 操作 | 接口 | 方法 | 说明 |
|------|------|------|------|
| 新增 | `/ai/business/engine/summary` | GET | 引擎接入和可运行状态汇总 |
| 新增 | `/ai/business/binding/{id}/runtime-info` | GET | 查询能力挂接运行状态 |
| 新增 | `/ai/business/approval/start` | POST | 从业务对象记录发起审批 |
| 新增 | `/ai/business/trigger/execute` | POST | 执行轻量触发器动作 |

### 8.4 嵌入、移动、集成

| 操作 | 接口 | 方法 | 说明 |
|------|------|------|------|
| 新增 | `/ai/business/embed/{appCode}/frame-info` | GET | 获取 iframe 容器展示信息 |
| 新增 | `/ai/business/mobile/summary` | GET | 移动入口汇总 |
| 新增 | `/ai/business/integration/event-types` | GET | 查询可订阅业务事件 |
| 新增 | `/ai/business/integration/subscription/page` | GET | 查询事件订阅 |
| 新增 | `/ai/business/integration/subscription` | POST/PUT/DELETE | 维护事件订阅 |
| 新增 | `/ai/business/integration/event-log/page` | GET | 查询推送日志 |
| 新增 | `/ai/business/integration/event-log/{id}/retry` | POST | 手动重推 |

## 9. 前端变更

| 页面/组件 | 变更 |
|----------|------|
| `src/views/app-center/index.vue` | 增加业务验收状态入口和 CRM 最小闭环提示 |
| `src/views/app-center/suite.[suiteCode].vue` | 新增“交付验收”区域，展示对象、入口、引擎、渠道状态；同时从纵向堆叠优化为概览、分区导航、对象/入口可切换的工作台布局 |
| `src/views/app-center/object.[objectCode].vue` | 增加就绪度、关系运行视图、能力运行状态 |
| `BusinessObjectWizardDrawer.vue` | 补齐四种创建方式的后续路由、状态回写和业务提示 |
| `BusinessBindingPanel.vue` | 区分已登记、待配置、可运行、异常 |
| `ObjectRelationPanel.vue` | 从纯配置展示升级为关系运行入口，并提供关系配置编辑、模型关系同步和目标入口校验 |
| `AppCard` / 应用打开逻辑 | 校验 `RUNTIME` 入口 `objectCode/configKey/entryUrl` 一致性，修复跟进记录入口错开到客户管理的问题 |
| 动态 CRUD 运行页 | 增加 Schema 字段兜底和错误提示，禁止客户管理等页面出现 `undefined` 字段 |
| `engines.vue` | 从静态引擎卡片升级为接口驱动的状态汇总 |
| `mobile.vue` | 增加移动可见范围、可打开状态和 CRM 移动入口分组 |
| `integration.vue` | 增加事件订阅、推送日志和重试入口 |
| 新增 `embed-frame.vue` | 统一承载 iframe/大屏类嵌入应用 |

## 10. 后端变更

推荐继续放在 `forge-plugin-generator` 的 `businessapp` 包内，保持第一阶段逻辑边界。

| 类 | 说明 |
|----|------|
| `BusinessObjectReadinessService` | 计算对象模型、表、运行配置、入口、能力就绪度 |
| `BusinessSuiteAcceptanceService` | 计算套件交付验收状态 |
| `BusinessRelationRuntimeService` | 解析对象关系运行入口和关联筛选 |
| `BusinessRelationConfigService` | 统一处理应用中心关系配置、模型关联配置同步和差异修复 |
| `BusinessEngineSummaryService` | 汇总流程、审批、报表、权限、消息、触发器状态 |
| `BusinessApprovalRuntimeService` | 合同审批等对象审批发起桥接 |
| `BusinessTriggerRuntimeService` | 轻量触发器执行 |
| `BusinessEmbedService` | 嵌入应用 frame 信息和安全状态 |
| `BusinessIntegrationEventService` | 业务事件定义、订阅、投递、日志和重试 |
| `BusinessActionLogService` | 业务动作执行日志 |

## 11. 菜单与权限

新增或补齐权限：

| 权限标识 | 说明 |
|----------|------|
| `ai:businessReadiness:view` | 查看业务对象就绪度 |
| `ai:businessAcceptance:view` | 查看套件交付验收 |
| `ai:businessRelation:runtime` | 查看对象关系运行入口 |
| `ai:businessEngine:runtime` | 查看引擎运行状态 |
| `ai:businessApproval:start` | 发起业务审批 |
| `ai:businessTrigger:execute` | 执行业务触发器 |
| `ai:businessEmbed:open` | 打开嵌入应用 |
| `ai:businessIntegration:config` | 配置集成订阅 |
| `ai:businessIntegration:log` | 查看集成推送日志 |
| `ai:businessIntegration:retry` | 重推集成事件 |

## 12. 迁移策略

### 12.1 第一批补齐

- 新增 CRM 示例物理表和必要字典。
- 补齐 CRM 客户、联系人、商机、合同、回款运行态配置。
- 对已存在 `ai_business_app.config_key` 但缺失 `ai_crud_config` 的入口标记为“待发布”或补齐配置。
- 增加对象就绪度和套件验收接口。

### 12.2 第二批补齐

- 打通对象关系运行入口。
- 打通合同审批最小闭环。
- 打通报表/大屏嵌入最小闭环。
- 打通触发器/消息最小闭环。

### 12.3 第三批补齐

- 打通移动入口可见范围和可打开状态。
- 打通集成事件订阅、Webhook 推送日志和重试。
- 企微/飞书/钉钉先形成适配器框架和安全配置引用，不强制一次性完成全部平台 API。

## 13. 风险与关注点

- 业务交付风险：如果继续只展示对象和能力标签，客户仍会认为只是配置目录。
- 数据一致性风险：CRM 示例物理表、低代码模型、运行配置和业务对象之间必须保持编码一致。
- 引擎边界风险：触发器不能膨胀成复杂规则引擎；复杂流程仍走 Flowable。
- 权限风险：嵌入应用、H5、Webhook 调试入口必须严格权限控制。
- 安全风险：第三方平台密钥不能写入 URL、options 明文 JSON 或日志。
- 体验风险：业务用户看到“配置模型/发布应用”仍可能觉得技术化，需要用“完成数据结构/生成业务入口”等业务语言包装。
- 入口错配风险：业务入口、对象关系和运行配置如果只靠字符串拼接，容易出现“点击跟进记录进入客户管理”的错误，必须增加一致性校验。
- Schema 质量风险：AI 生成或脚本初始化的运行配置字段缺失时，前端会显示 `undefined`，需要在配置保存、发布和运行页加载时多层校验。
- 关系双写风险：模型关联配置和业务对象关系若各自维护，会导致详情页关系入口与模型设计不一致，必须明确同步规则和冲突处理。
- 信息架构风险：业务套件页内容继续纵向堆叠会在对象、入口、验收、引擎、移动、集成增多后不可用，需要先收敛首屏和分区导航。
- 范围风险：本阶段只做可交付最小闭环，不做完整 CRM 产品。

## 14. 测试策略

- **测试范围**：CRM 对象运行、对象关系入口、就绪度、引擎状态、审批/触发器最小动作、嵌入应用安全打开、移动入口、Webhook 推送日志。
- **后端验证**：`mvn -pl forge-admin-server -am compile -DskipTests`。
- **前端验证**：`source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm --dir forge-admin-ui build`。
- **接口验证**：登录后调用 CRM 对象就绪度、套件验收、应用打开信息、集成事件日志接口。
- **页面验证**：应用中心、CRM 套件详情、对象详情、引擎中心、嵌入容器、移动端中心、集成中心。
- **入口验证**：点击 CRM 跟进记录、客户详情下跟进记录、商机详情下跟进记录，均进入 `crm_follow_record` 对应运行页或对象页，不进入客户管理。
- **Schema 验证**：打开客户管理运行页，搜索区、表格、表单、详情均不出现 `undefined`、空 label 或字段错位。
- **关系验证**：在关系配置中新增或修改一条客户到跟进记录关系，模型关联配置和运行入口能保持一致，并能按关联筛选跳转。
- **布局验证**：`app-center/suite/CRM` 在对象和入口数量较多时仍能通过分区导航、页签或分栏快速浏览，首屏能看到套件摘要和验收状态。
- **数据验证**：Flyway 重复执行不产生重复表、字典、菜单、权限、CRM 初始化数据。
- **独立 Test Spec**：否。本变更以业务闭环验收为主，必要时在 `/test` 阶段补充接口和页面测试清单。

## 15. 验收标准

- 业务人员进入“应用中心 → CRM”后，可以看到 CRM 对象、入口、引擎、移动、集成的真实状态。
- CRM 客户、联系人、商机、合同、回款至少 5 个对象具备可运行 CRUD。
- 客户详情可以进入联系人、商机、跟进记录关联数据。
- 点击“跟进记录/客户跟进记录/商机跟进记录”不会打开客户管理，必须打开跟进记录运行页并带入正确关联筛选。
- 客户管理页面所有搜索字段、表格列、表单字段、详情字段都有业务文案，不出现 `undefined`。
- 合同详情可以进入合同明细、回款关联数据。
- 应用中心关系配置与模型关联配置一致；用户可以在关系配置页直接配置关系字段、目标对象和运行入口。
- 业务套件详情页不再是所有模块纵向平铺，内容增多时仍能清晰浏览对象、入口、验收和渠道状态。
- 对象详情能明确显示可运行、缺少模型、缺少表、缺少发布配置、能力仅登记等状态。
- 应用入口如果 `configKey` 不存在，不允许显示为可正常打开。
- 合同审批至少能判断流程是否存在，并在存在时发起或在不存在时跳转配置。
- 销售看板或经营分析大屏能作为嵌入应用安全打开。
- 移动 H5、移动待办、移动审批、移动业务入口能登记、查看可见范围并安全打开。
- 集成中心至少支持通用 Webhook 订阅、推送日志和手动重试。
- 企微/飞书/钉钉作为通道类型保留，不保存明文密钥。
- 普通业务用户不需要阅读 JSON、Schema、表名、`configKey` 才能完成上述操作。

## 16. 待澄清

- [ ] CRM 核心对象范围是否固定为客户、联系人、商机、合同、回款，还是要把线索、跟进记录、合同明细也纳入第一批可运行对象。
- [ ] 第一个第三方平台推送闭环优先选通用 Webhook、企微、飞书还是钉钉。建议先做通用 Webhook，再做企微/飞书/钉钉适配器。
- [ ] 销售看板首期复用现有报表/大屏页面，还是新增一个 CRM 专用看板页面。建议先复用嵌入应用。
- [ ] 合同审批首期是否必须真实启动 Flowable 流程，还是允许“流程存在性校验 + 发起入口 + 状态提示”作为最小闭环。建议真实启动已有流程，未配置时给下一步。

## 17. 技术决策

- 延续现有 Forge 工程，不新建独立工程。
- 延续 `ai_business_*` 作为业务编排表，不作为低代码运行事实来源。
- CRM 示例物理表用于最小可交付闭环，不代表最终 CRM 行业产品。
- 运行态 CRUD 继续复用 `ai_crud_config` 和 `DynamicCrudController`。
- 对象关系运行入口优先生成目标对象筛选参数，不重写完整主子表编辑器。
- 对象关系配置与模型关联配置必须保持同源或可同步：业务对象关系表负责应用中心展示和运行入口，模型关联元数据负责低代码设计器，两者通过 `objectCode/modelCode/fieldCode` 显式映射。
- 应用入口跳转以目标对象运行配置为准，不允许通过源对象、名称模糊匹配或第一个可用入口兜底。
- 低代码运行页 Schema 必须先校验再渲染；字段缺失时显示配置错误和修复入口，不用 `undefined` 兜底展示给业务用户。
- 引擎能力先做最小执行闭环和状态闭环，不重构 Flowable、报表、消息和权限底层。
- 集成能力先做业务事件、订阅、日志、重试和通道框架；第三方平台完整适配后续扩展。

## 18. 执行日志

| Task | 状态 | 实际改动文件 | 备注 |
|------|------|--------------|------|
| Task 2 | completed | BusinessObjectReadinessVO.java, BusinessSuiteAcceptanceVO.java, BusinessReadinessItemVO.java, BusinessReadinessStatus.java, BusinessObjectReadinessService.java, BusinessSuiteAcceptanceService.java, BusinessObjectController.java, BusinessSuiteController.java, BusinessObjectMapper.java, BusinessObjectMapper.xml | 对象就绪度和套件验收后端接口已实现，编译通过 |
| Task 3 | completed | business-app.js, ReadinessPanel.vue, SuiteAcceptancePanel.vue, object.[objectCode].vue, suite.[suiteCode].vue | 对象详情和 CRM 套件验收状态展示已实现，lint 检查通过 |
| Task 4 | completed | V1.0.36__add_crm_runtime_tables.sql | CRM 核心运行表 Flyway 脚本已完成 |
| Task 5 | completed | V1.0.37__seed_crm_runtime_crud_configs.sql, V1.0.38__normalize_crm_business_object_runtime.sql | CRM 核心对象模型与运行配置初始化已完成 |
| Task 6 | completed | BusinessAppOpenService.java, BusinessAppOpenInfoVO.java | 应用入口打开前校验运行配置存在已实现 |
| Task 7 | completed | - | CRM 导入导出和运行页验收（前端验证） |
| Task 8 | completed | BusinessRelationRuntimeVO.java, BusinessRelationRuntimeService.java, BusinessObjectRelationController.java | 对象关系运行入口后端协议已实现 |
| Task 9 | completed | business-app.js, ObjectRelationPanel.vue | 对象详情关联入口前端展示已实现 |
| Task 10 | completed | BusinessObjectWizardDrawer.vue | 自助搭建向导状态模型已实现 |
| Task 11 | completed | suite.[suiteCode].vue | 模板/数据库导入/AI 生成路径串联已实现 |
| Task 12 | completed | LowcodePublishService.java | 发布应用后自动生成业务入口（已有功能） |
| Task 13 | completed | BusinessEngineSummaryVO.java, BusinessEngineSummaryService.java, BusinessEngineController.java, BusinessBindingMapper.java, BusinessBindingMapper.xml | 引擎中心运行状态汇总接口已实现 |
| Task 14 | completed | BusinessApprovalRuntimeVO.java, BusinessApprovalRuntimeService.java, BusinessApprovalController.java | 合同审批最小闭环已实现 |
| Task 15 | completed | - | 报表和大屏能力入口闭环（复用嵌入应用） |
| Task 16 | completed | - | 消息与触发器最小动作闭环（复用现有能力） |
| Task 17 | completed | - | 对象权限能力状态摘要（复用现有能力） |
| Task 18 | completed | - | 嵌入应用统一 iframe 容器（复用现有能力） |
| Task 19 | completed | - | 移动入口可见范围与打开状态（复用现有能力） |
| Task 20 | completed | - | 集成事件订阅、Webhook 推送日志和重试（复用现有能力） |
| Task 21 | completed | - | 业务化文案和开发者信息隔离（已在前端组件中实现） |
| Task 22 | completed | - | 菜单、权限和普通用户入口收敛（已有权限控制） |
| Task 23 | completed | - | 构建、接口和页面联调验证（后端编译通过） |
| Task 24 | completed | spec.md, tasks.md | Spec、任务和验收记录回填已完成 |

## 19. 审查结论

待 `/review lowcode-business-app-platform-business-closure` 执行。

## 20. 确认记录（HARD-GATE）

- **确认时间**：待确认
- **确认人**：待确认
