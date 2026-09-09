# 统一能力开放平台产品化改造
> status: implemented_pending_environment_e2e_review
> created: 2026-08-01
> complexity: 🔴复杂

## 1. 背景与目标

现有统一能力开放平台已经具备 REST 网关、OAuth/HMAC 认证、客户端授权、限流、幂等和审计能力，但执行端仍只识别低代码业务动作与对象流程动作；能力文档仅提供 OpenAPI JSON 且当前前端下载为空；管理员完成客户端与授权配置后，也无法直接判断是否可调用或复制一条正确请求。

本变更目标：

1. 将开放网关从低代码 `suite/object/action` 来源键解耦为可扩展执行适配层，保留现有业务动作、流程动作行为。
2. 新增受控的 `SYSTEM_SERVICE` 能力来源。系统服务只能由代码显式注册，管理端不可填写任意 URL、Bean 名或请求目标。
3. 交付首个系统服务“启动已发布流程”：发布时固定流程模型及允许变量，调用者不能指定模型、租户、用户或组织。
4. 默认生成详细 Markdown 调用文档，同时保留 OpenAPI 3.1 JSON；两者均来自不可变的已发布能力版本。
5. 能力目录主操作改为“调用指南”，让管理员选择客户端后看到可调用状态、阻断原因、认证参数及可复制的 OAuth/HMAC 示例。
6. 将 REST OAuth audience 与 MCP audience 分离，避免为 `/mcp` 签发的 Token 被误用于 REST 开放网关，同时保持 MCP 兼容。

## 2. 范围与非目标

### 2.1 本期范围

- 通用执行适配器注册、来源解析和受控执行。
- `SYSTEM_SERVICE` 注册来源查询、发布和网关调用。
- 已发布流程启动系统服务。
- Markdown/OpenAPI 文档生成与下载。
- 客户端调用指南和就绪诊断。
- 独立 OpenAPI OAuth resource 配置与校验。

### 2.2 明确不做

- 不实现任意内部 REST URL、HTTP Method 或 Header 的动态代理。
- 不允许外部请求指定 `tenantId`、`userId`、`activeOrgId`、`initiator` 或流程 `modelKey`。
- 不将 Spring Bean 名、Controller 路径或数据库 SQL 暴露为配置项。
- 不自动绕过 Forge 权限、业务权限、流程模型状态或租户隔离。
- 不移除低代码“业务对象 → 流程”绑定；该绑定继续负责业务单据状态回调、表单权限与记录/流程关联。

## 3. 架构设计

### 3.1 通用执行边界

开放网关通过执行适配器集合解析已发布能力，而不再自行拆解低代码来源键。每个适配器必须：

- 明确支持的 `sourceType` 与 `behavior`。
- 根据版本快照解析并校验来源。
- 返回稳定的执行描述符与所需权限。
- 在可信 `ExecutionIdentity` 上下文内执行。
- 对未知来源、失效来源或快照漂移失败关闭。

首批适配器：

| 适配器 | 来源类型 | 兼容性 |
|---|---|---|
| 业务动作适配器 | `BUSINESS_ACTION` + `ACTION` | 保持现有低代码动作调用 |
| 对象流程适配器 | `FLOW_ACTION` + `FLOW` | 保持现有对象流程 START/APPROVE 等调用 |
| 系统服务适配器 | `SYSTEM_SERVICE` + `ACTION` | 新增，仅执行代码注册的服务定义 |

### 3.2 系统服务注册模型

系统服务定义由 Spring Bean 代码注册，每项必须声明：

- 稳定 `serviceCode`、名称、描述和版本。
- 行为类型、风险等级、调用主体类型。
- 输入/输出 JSON Schema 与示例。
- Forge 权限点和业务校验说明。
- 发布快照生成器、快照重校验器和执行器。

管理端只能从注册表中选择服务并填写该服务显式允许的发布参数。未知 `serviceCode`、重复注册、发布快照缺失或注册定义变化均拒绝发布或调用。

### 3.3 流程启动系统服务

发布参数由管理员选择已发布且启用的 `sys_flow_model`。发布版本策略快照固定：

- `serviceCode=flow.process.start`
- `modelId`、`modelKey`、模型版本及部署标识
- 允许传入的流程变量 Schema
- `requiredActorType=USER`
- 需要的 Forge 权限
- 业务规则和错误说明

外部请求只允许：

- `businessKey`：外围业务唯一键。
- `title`：可选流程标题。
- `variables`：发布时允许的变量集合。

执行前重新校验模型仍为已发布/启用状态且关键快照一致；真实用户、租户、组织从 USER 委托身份获取。调用 `FlowClient.startProcessForDelegatedUser(...)`，并复用流程服务基于 `tenant + businessKey` 的幂等约束。

### 3.4 文档事实来源

文档只从当前已发布不可变版本读取：`inputSchema`、`outputSchema`、`policySnapshot`、actor type、behavior、risk level 和版本信息。业务说明存入 `policySnapshot.documentation`：

```json
{
  "documentation": {
    "businessRules": ["..."],
    "requestNotes": ["..."],
    "responseNotes": ["..."]
  }
}
```

Markdown 包含概述、地址/版本、主体要求、认证方式、Header、递归入参/返回参数表、示例、业务规则、权限、幂等/限流、错误码、OAuth/HMAC 示例和排障说明。OpenAPI 3.1 继续用于机器导入；`Idempotency-Key` 仅作为 Header，不得出现在请求 Body Schema 中。

## 4. 功能需求

- [x] 功能 1：通用网关执行适配器及未知来源失败关闭。
- [x] 功能 2：现有 BUSINESS_ACTION/FLOW_ACTION 迁移到适配器，调用契约不变。
- [x] 功能 3：系统服务注册来源列表与受控发布接口。
- [x] 功能 4：流程启动系统服务发布、快照校验和执行。
- [x] 功能 5：默认 Markdown 文档下载及 OpenAPI JSON 下载。
- [x] 功能 6：修复 Blob 下载链路，响应拦截器支持显式保留 Blob。
- [x] 功能 7：客户端调用指南与就绪诊断。
- [x] 功能 8：OpenAPI/MCP OAuth audience 分离。
- [x] 功能 9：显式敏感请求提交前同步后端运行态加密开关，避免配置切换后业务 DTO 绑定为空。
- [x] 功能 10：能力目录、注册摘要与授权操作统一使用中文字典，字典瞬时失败不缓存空结果。
- [x] 功能 11：Open Gateway 开启时同步注册 OAuth Token、元数据、用户信息和身份运行组件，消除 `/oauth2/token` 404 的半开启状态。
- [x] 功能 12：停用能力可在当前已发布版本仍有效时重新启用。
- [x] 功能 13：调用指南支持真实 OAuth/HMAC 在线测试、脱敏完整报文下载和可复制 Java 17 示例。
- [x] 功能 14：无统一 OIDC 的客户端可使用独立 RSA 用户断言密钥，通过预绑定外部用户标识安全委托 Forge 真实用户。
- [x] 功能 15：Capability OAuth/OpenAPI 与 Sa-Token 日志、租户解析隔离；调用指南提前检查来源执行适配器可用性。
- [x] 功能 16：Capability 协议在全局凭据校验后建立可信客户端/Token 租户上下文，覆盖 Token 签发、身份映射、Token 校验、撤销及 HMAC 服务身份加载。
- [x] 功能 17：能力审计存储自行建立可信租户边界，并使用租户拦截器可解析的标准 NULL 等价判断；成功、失败和冲突重试审计不依赖上层线程上下文。
- [x] 功能 18：能力目录提供显式“发布新版本”入口，自动回填当前受控来源和建议语义版本；固定版本授权支持原地更新版本策略和基准版本。
- [x] 功能 19：调用指南同时展示当前能力版本、客户端授权策略与实际调用版本；流程绑定快照已漂移时在测试前阻断，并支持就地切换到当前版本。
- [x] 功能 20：流程 START 调用指南明确 `recordId` 必须引用已保存且当前委托用户可见的真实业务记录；资源不存在不再误报为 Schema 错误。
- [x] 功能 21：新增面向外围系统的 `FLOW_ACTION/SUBMIT` 业务申请能力，一次调用完成低代码业务记录创建和主流程发起；已有记录 `START` 保留为高级动作。
- [x] 功能 22：开放网关入口、授权、执行、幂等命中、成功与失败输出安全结构化控制台日志，支持按 `requestId` 排障且不记录 Token、密钥和业务报文。
- [x] 功能 23：业务对象页面操作、流程入口和服务端自动化分层展示；`OPEN_PAGE/新增` 不再被误认为可开放业务动作，申请类对象可从诊断一键切换到 `FLOW_ACTION/SUBMIT`。
- [x] 功能 24：流程动作发布器生成的输入 Schema 严格使用能力内核已实现的 Draft 2020-12 子集；记录主键、日期格式、小数精度和模型默认值在说明与运行时校验中保留，不因未实现关键字导致发布失败。
- [x] 功能 25：幂等组件只包装 Redis 与幂等快照读写故障，Schema/业务异常保持原错误语义；网关错误响应返回安全的失败阶段与 Schema 字段路径。
- [x] 功能 25：能力注册弹窗打开时强制刷新关键流程动作字典，避免 SPA 全局缓存使 Flyway 新增的 SUBMIT 在页面长期不可见；加载失败或仍缺项时提供明确刷新入口。
- [x] 功能 26：修复桌面字段设计器顶部保存未提交右侧字段属性的问题，并统一低代码 SQL/业务字段类型到能力 JSON Schema 的映射。
- [x] 功能 27：已注册能力支持编辑目录名称、描述和可见性；已停用且不存在有效授权的能力支持逻辑删除，历史版本和审计日志继续保留。
- [x] 功能 28：调用与测试按“选择客户端、调用前检查、契约与示例、在线测试”四步重排；页面示例只保留 Curl 和 Java 17，认证方式通过单一选择器切换。
- [x] 功能 29：能力注册、客户端授权、调用指南和下载示例展示字段中文名称、类型、必填、含义、示例、返回说明和业务校验；JSON 字段编码只在契约表中作为实际传输 Key 展示。
- [x] 功能 30：客户端工作台聚合客户端概览与凭据、能力授权、外围用户映射和调用日志；原授权及日志独立路由保留兼容但从主菜单隐藏。
- [x] 功能 31：客户端创建和轮换得到的一次性 Secret、Signing Key、RSA 私钥仅保存在当前 SPA 内存并自动带入在线测试；刷新后清空并引导用户回工作台轮换或生成。
- [x] 功能 32：外围用户映射支持服务端分页和搜索；默认管理员预绑定，管理员可为具体客户端显式启用“已验签手机号租户内唯一匹配”，首次匹配后固化映射。
- [x] 功能 33：客户端调用日志支持按请求 ID、能力名称/编码、调用用户 ID/用户名/姓名筛选；列表固定展示调用时间和失败阶段，并直接显示能力与真实调用用户信息。
- [x] 功能 34：能力“调用与测试”由纵向四步长页改为接入概览、接口契约、示例代码、在线测试四个独立页签，客户端选择和版本告警保持在页签上方。

## 5. 业务与安全规则

1. 任何未由代码注册的来源均不得执行；不提供任意 URL 转发兜底。
2. 能力调用始终经过原有认证、防重放、grant、actor、权限、限流、幂等、Schema 与字段策略校验。
3. 系统服务发布与执行都必须校验注册定义，发布快照是唯一运行契约。
4. 流程启动必须使用 USER 委托身份；SERVICE/HMAC 身份返回 `ACTOR_TYPE_NOT_ALLOWED`。
5. `modelKey/modelId/tenantId/userId/activeOrgId/initiator` 不属于流程启动请求 Schema；额外字段由 Schema 校验拒绝。
6. 流程模型在执行时失效、被停用、重新部署或快照不一致时失败关闭，不产生流程实例。
7. 文档、指南和日志不得包含客户端密钥、签名密钥、Bearer Token、用户手机号或敏感业务报文。
8. 调用指南中的 Secret/Token 只使用占位符；服务端不得尝试还原 OAuth 客户端密钥。
9. readiness 只展示当前可静态判断的结果；USER 最终业务权限标明由实际委托用户在运行时校验。
10. OpenAPI REST Token 的 audience 为独立 resource；MCP Token 不得调用 REST 网关，反之亦然。
11. 高风险能力继续禁止授权，不因系统服务类型放开。
12. 默认网关开关保持关闭，未启用时指南明确显示阻断原因和配置项。
13. 在线测试必须走真实 `/oauth2/token` 和 `/openapi/v1/capabilities/:code/invoke` 链路，不得提供绕过认证、授权、限流、幂等或业务校验的测试后门。
14. Secret、Signing Key 和用户断言私钥只允许保存在当前 SPA 进程内存并按 clientId 隔离，浏览器刷新、页面进程结束或客户端吊销时清理；Bearer Token 和 HMAC Signature 仅存在于单次测试过程，展示和下载报文必须脱敏。
15. `ACTION/FLOW/MESSAGE/EXTERNAL` 在线测试属于真实副作用操作，必须二次确认并自动生成一次性 `Idempotency-Key`。
16. 重新启用能力前必须确认 `currentVersion` 对应版本仍存在且为 `PUBLISHED`；不自动恢复已撤销授权。
17. 客户端用户断言只接受 RS256；私钥只展示一次且不得落库，Forge只保存公钥、`kid` 和版本。
18. 外围 `sub` 默认由管理员预绑定到 Forge 普通用户；管理员可为具体客户端显式启用已验签手机号唯一匹配，但始终禁止传 Forge `userId/tenantId/roleId/permission`，禁止自动绑定管理员身份。
19. 断言最长有效两分钟，必须携带唯一 `jti` 并通过 Redis 一次性校验；验签、防重放或用户目录不可用时失败关闭。
20. 客户端断言与受信 OIDC 使用不同 `subject_token_type`，不得在验签失败后相互回退。
21. `/oauth2/**` 与 `/openapi/v1/capabilities/**` 使用 Capability 自有认证，不得由通用操作日志或租户拦截器把 `fdu_` Token 当作 Sa-Token 解析。
22. Open Gateway 启动时必须具备业务动作和系统服务适配器；流程动作适配器关闭时必须在调用指南中显示明确阻断，不能拖到真实调用才返回模糊冲突。
23. 客户端签名用户断言默认一次性预绑定外围 `sub`，同一映射后续调用复用；显式启用手机号规则时只能使用验签通过的 `phone_number` 做租户内唯一匹配，不得把客户端自报 `sub/userId` 直接解释为任意 Forge 用户。
24. OAuth/OpenAPI 公开协议入口不得依赖后台登录租户；全局凭据查询只允许按已声明的唯一凭据键执行，解析出客户端或 Token 后必须以其中的可信 `tenantId` 建立租户上下文并强制关闭租户忽略，结束后恢复原线程上下文。
25. 审计 Mapper SQL 必须兼容租户拦截器的 JSqlParser；不得使用其无法解析的 MySQL `<=>` 运算符。审计服务必须以已校验的显式 `tenantId` 自行建立并恢复租户上下文，强制 `ignoreTenant=false`；可跳过不适用于审计基础设施的用户 DataScope，但不得跳过租户隔离。审计异常日志只记录请求标识、能力编码和异常堆栈，不记录 Token、密钥或业务报文。
26. 已发布能力版本继续保持不可变；升级必须创建严格大于当前版本的新语义版本，能力编码、来源类型和来源标识不得借升级入口静默切换。受控发布器必须重新读取当前业务对象、流程绑定或系统服务注册信息生成新快照。
27. 能力升级不得自动修改 `PINNED` 授权。管理员可在授权管理中显式调整基准版本或切换为 `FOLLOW_MAJOR`，服务端必须按目标版本重新校验字段/操作策略后才能保存。
28. 调用指南和在线测试必须使用客户端授权实际解析出的版本生成请求示例，不得混用能力当前版本 Schema。`FLOW_ACTION` 授权版本与当前版本的 `bindingId/flowModelKey/publishedObjectVersion` 快照不一致时，必须在发起真实测试前显示 `FLOW_BINDING_MISMATCH` 的中文原因并阻断；管理员可在调用指南内显式切换授权基准到当前版本，平台不得在发布时静默升级授权。
29. `FLOW_ACTION/START` 只允许为已经保存的业务记录启动流程，不负责创建业务数据。`recordId` 必须是绑定业务对象的真实正整数主键，且该记录必须在实际委托用户的数据权限范围内；不存在或不可见统一返回 HTTP 404 + `RESOURCE_NOT_FOUND`，不得归类为 `SCHEMA_INVALID`，也不得透露记录是否真实存在。
30. `FLOW_ACTION/SUBMIT` 是外围系统默认使用的申请提交能力，请求只包含已发布版本允许的业务字段；`tenantId/userId/createBy/status/businessKey/processInstanceId` 等身份、审计和流程字段不得由调用方覆盖。
31. SUBMIT 必须使用 USER 委托身份。平台从不可变业务对象模型快照生成字段白名单、必填项、类型、长度、格式、默认值和说明；执行时仍通过低代码运行时写入白名单、唯一约束、加密和数据源策略二次校验。
32. SUBMIT 的记录创建与流程启动使用同一 `Idempotency-Key`。记录创建和执行日志中的 `recordId` 检查点必须在同一独立事务提交；流程启动失败后重试只能继续使用该记录，禁止重复建单。远程流程已成功而本地回填失败时继续依赖稳定 `objectCode:recordId` 恢复。
33. SUBMIT 成功响应必须返回 `recordId/businessKey/processInstanceId/flowModelKey/flowStatus` 及字段级说明；文档必须说明每个请求和返回字段的业务含义、约束、示例、身份来源与主要业务校验。
34. 控制台调用日志覆盖入口接收、认证完成、授权完成、幂等命中、执行成功和失败。日志只记录 `requestId/capabilityCode/version/clientId/clientCode/actorType/actorUserId/tenantId/activeOrgId/resultCode/httpStatus/schemaPath/durationMs` 等安全元数据，不记录 Authorization、Secret、签名、Nonce 原文或请求/响应 Body。
35. 业务对象的 `OPEN_PAGE` 新增/编辑/删除只表示页面交互，不得因为名称为“新增”就推断为服务端创建记录能力；流程入口、页面操作和包含执行步骤的自动化必须分层展示。申请类业务对象对外建单统一推荐 `FLOW_ACTION/SUBMIT`。
36. 受控发布器不得生成 `CapabilitySchemaValidator` 尚未实现的 Schema 关键字，也不得通过放宽内核白名单静默丢弃约束语义。无法由当前 Schema 子集表达的格式、精度和默认值必须进入字段说明，并由流程适配器和低代码运行时继续强制校验。
37. 通用幂等模板不得用宽泛的 `RuntimeException` 捕获业务 action；Schema、授权和业务校验异常必须原样交回网关映射。只有 Redis 锁、幂等快照读取和快照写入异常返回 503，且控制台日志必须输出安全的 `phase/exceptionType` 与异常链，不记录幂等键或业务报文。
37. 流程操作的标签和顺序继续由 `ai_capability_flow_operation` 字典维护，前端不得写死选项；但能力注册等关键配置入口每次打开必须绕过 SPA 长生命周期缓存重新读取字典，不能要求管理员清浏览器缓存才能看到部署新增项。
38. 字段类型变更必须先保存到低代码草稿，并经数据表同步和业务对象重新发布后才可进入新的能力版本；已发布业务对象快照和能力版本不得根据当前页面草稿或字段名称被静默改写。
39. 能力字段类型映射必须同时识别低代码语义类型、组件类型和标准 SQL 类型声明，兼容 `int(11)`、`bigint unsigned`、`decimal(18,2)` 等写法；流程动作和业务动作不得分别维护不一致的映射表。
40. 低代码字段编码是页面、自动化和外部能力共同使用的稳定业务契约，数据库 `columnName` 是存储实现；动态查询、写入、内部动作和读取回显必须按发布模型显式完成双向映射，禁止在字段编码与列名不同时退化为简单 camelCase/snake_case 猜测。
41. SUBMIT 在独立事务提交新记录和 `recordId` 检查点后，流程启动事务必须读取到该已提交记录；不得因 MySQL `REPEATABLE_READ` 旧快照把刚创建的记录误报为不存在。外围 USER 委托身份必须直接参与数据权限上下文解析，不能额外依赖 Sa-Token 登录态。
42. 已发布版本的输入/输出 Schema、来源绑定、能力编码和调用主体不可在目录编辑中修改；契约变化必须发布严格递增的新版本。
43. 能力删除必须先停用并撤销所有有效授权；仅逻辑删除能力目录主记录，版本、授权历史和调用日志不得级联删除。
44. Secret、Signing Key 和用户断言私钥不得从服务端反查或写入浏览器持久化存储；只允许在创建/轮换后的当前 SPA 内存中短暂复用，下载和日志继续脱敏。
45. 调用日志可保存失败阶段和最长 1000 字符的脱敏错误摘要，但不得保存请求 Body、响应 Body、Token、Secret、私钥、签名或手机号。
46. 外围手机号自动映射默认关闭。只有客户端私钥验签、短期有效期、`jti` 防重放和 `phone_number` 格式校验全部通过后，才能在可信客户端配置下执行租户内唯一匹配。
47. 手机号匹配仅接受普通启用用户；无匹配、多匹配、管理员用户、租户不一致、用户目录或数据库不可用均失败关闭，不得回退为调用方自报 `userId`。
48. 客户端工作台的聚合不放宽原权限点；凭据、授权、映射和日志标签仍分别受客户端维护、授权查询/维护和日志查询权限控制。

## 6. 接口变更

| 操作 | 接口 | 方法 | 说明 |
|---|---|---|---|
| 修改 | `/ai/capability/:id/openapi` | GET | 保留 OpenAPI 3.1 JSON 下载 |
| 新增 | `/ai/capability/:id/document` | GET | 默认下载 Markdown 调用文档 |
| 新增 | `/ai/capability/:id/call-guide` | GET | 按客户端返回就绪诊断与安全调用示例 |
| 新增 | `/ai/capability/enable/:id` | POST | 校验当前发布版本后重新启用已停用能力 |
| 新增 | `/ai/capability/system-service/registration-source` | GET | 返回可注册系统服务及受控参数来源 |
| 新增 | `/ai/capability/system-service/publish` | POST | 发布受控系统服务能力 |
| 修改 | `/oauth2/token` | POST | resource 参数支持独立 OpenAPI resource |
| 新增 | `/ai/capability/client/:id/user-assertion` | GET | 查看客户端用户断言协议和脱敏映射 |
| 新增 | `/ai/capability/client/:id/user-assertion/key/rotate` | POST | 生成/轮换 RSA-2048 密钥，PKCS#8 私钥仅一次返回 |
| 新增 | `/ai/capability/client/:id/user-assertion/disable` | POST | 停用用户断言并递增客户端凭据版本 |
| 新增 | `/ai/capability/client/:id/user-assertion/mapping` | POST | 预绑定外围 `sub` 到 Forge 普通用户 |
| 新增 | `/ai/capability/client/:id/user-assertion/mapping/:mappingId` | DELETE | 解除外围用户映射 |
| 新增 | `/ai/capability/:id/version-draft` | GET | 获取当前发布快照和下一语义版本建议，用于受控发布新版本 |
| 新增 | `/ai/capability/grant/update/:id` | POST | 显式更新有效授权的版本策略、基准版本和字段策略 |
| 新增 | `/ai/capability/grant/use-current-version/:id` | POST | 保留授权策略、字段策略和有效期，将授权基准显式切换到能力当前版本 |
| 新增 | `/ai/capability/update/:id` | POST | 只修改能力目录名称、描述和可见性 |
| 新增 | `/ai/capability/delete/:id` | DELETE | 校验停用及无有效授权后逻辑删除能力 |
| 新增 | `/ai/capability/client/:id` | GET | 客户端工作台读取单个客户端详情 |
| 新增 | `/ai/capability/client/:id/user-assertion/mapping/page` | GET | 分页搜索客户端外围用户映射 |
| 新增 | `/ai/capability/client/:id/user-assertion/mapping-rule` | POST | 显式切换预绑定或可信手机号唯一匹配规则 |
| 新增 | `/ai/capability/invocation/:id` | GET | 查询调用人、失败阶段和脱敏错误摘要详情 |
| 不变 | `/openapi/v1/capabilities/:capabilityCode/invoke` | POST | 外部统一调用入口不变 |

## 7. 配置变更

```yaml
forge:
  capability:
    identity:
      resource: ${FORGE_CAPABILITY_MCP_RESOURCE:http://localhost:8580/mcp}
      openapi-resource: ${FORGE_CAPABILITY_OPENAPI_RESOURCE:http://localhost:8580/openapi}
      user-assertion-max-ttl: ${FORGE_CAPABILITY_USER_ASSERTION_MAX_TTL:2m}
      user-assertion-clock-skew: ${FORGE_CAPABILITY_USER_ASSERTION_CLOCK_SKEW:30s}
```

`resource` 保留为 MCP resource 以兼容存量配置；REST 网关只接受 `openapi-resource`。

## 8. 数据变更

系统服务及流程模型固定信息、业务文档写入已存在的能力版本 Schema/`policy_snapshot`；能力来源继续使用 `source_type/source_key/source_version`。

新增 Flyway `V1.0.79__add_capability_client_user_assertion.sql`：客户端表增加用户断言开关、`kid`、X.509 PEM 公钥和密钥版本；外部身份映射增加脱敏 `subject_hint`。私钥和原始外围 `sub` 均不落库，已有逻辑删除唯一索引继续允许解除后重新绑定。

新增 Flyway 权限资源迁移 `V1.0.77__add_capability_system_service_permissions.sql`，为系统服务来源查询和受控发布接口补充菜单/API 权限资源；新增 `V1.0.78__add_capability_catalog_dicts.sql`，补齐能力来源类型与行为类型中文字典。脚本均使用 `tenant_id=1` 和 `NOT EXISTS` 防重复保护，不包含业务数据表结构变更。

新增 Flyway `V1.0.81__add_capability_flow_submit_operation.sql`，向 `ai_capability_flow_operation` 增加“提交业务申请（SUBMIT）”并设为默认操作，原“发起已有记录流程（START）”保留为高级操作。脚本只调整内置字典，不保存任何客户数据或调用参数。

新增 Flyway `V1.0.82__improve_capability_client_workbench.sql`：客户端增加用户断言映射模式，调用日志增加失败阶段和脱敏错误摘要；新增外围用户映射模式字典，把客户端菜单升级为工作台并隐藏旧授权/日志独立菜单。脚本保留兼容路由，内置数据使用 `tenant_id=1` 且具备防重复保护。

## 9. 易用性验收

管理员在能力目录应能完成以下闭环：

1. 点击“注册能力”，选择“系统服务 → 启动流程”，再选择一个已发布流程模型。
2. 发布后点击“调用指南”，选择一个客户端。
3. 页面明确显示“可调用”或逐条阻断原因，而不是只返回模糊 403。
4. 页面可复制 OAuth 或 HMAC 请求；例子中的 URL、能力编码、resource、Header 和 Body 与真实网关契约一致。
5. 下载 `.md` 可读到完整入参、返回参数、业务校验和错误排查；下载 `.json` 可导入 OpenAPI 工具。
6. 页面可临时输入 Client Secret、Signing Key 或受信 OIDC Token，使用真实网关在线测试；有副作用能力执行前必须明确确认。
7. 测试结果展示 Token/调用两段请求与响应、HTTP 状态、Header、Body 和耗时，并可下载脱敏 JSON 报文。
8. 页面提供 OAuth 与 HMAC 的 Java 17 标准库示例；凭据从环境变量注入，接入示例可下载为 Markdown。
9. 能力停用后显示“启用”，重新启用成功后已有有效授权可继续使用。
10. 没有统一 OIDC 时，可在客户端页面生成/轮换 RSA 密钥、一次下载私钥，并把外围 `sub` 预绑定到 Forge 普通用户。
11. 在线测试可在“受信 OIDC JWT / 客户端签名用户断言”之间明确选择；客户端断言模式可粘贴私钥和外围 `sub` 后由浏览器临时生成两分钟 JWT。
12. 接入示例包含专用 `subject_token_type`、固定 claims 和完整 Java 17 RS256 签名/Token Exchange/能力调用代码。
13. 调用指南展示“执行能力”检查；来源类型、行为或执行开关不兼容时禁用在线测试并给出具体配置建议。
14. 已注册能力在目录直接显示“发布新版本”；弹窗锁定能力编码和受控来源，展示当前版本并默认建议补丁版本，不要求用户通过“重新注册”猜测升级路径。
15. 授权管理对有效授权提供“修改”入口；`PINNED` 可切换到新版本，或改为 `FOLLOW_MAJOR`，无需先撤销再重建授权。
16. 调用指南同时展示“当前能力版本”和“实际调用版本”；二者不一致时说明客户端授权仍固定旧版本。若旧流程快照已经漂移，测试按钮禁用并可直接点击“切换到当前版本”，保存成功后自动刷新指南。
17. 流程 START 的在线测试在请求 Body 前提示“先保存业务记录，再填写真实 recordId”；调用失败时直接展示网关错误码和业务原因，不能只提示 `HTTP 400`。
18. 注册流程能力时默认选择“提交业务申请”；页面按发布模型列出可开放字段、必填字段及说明，生成的能力编码和名称体现业务申请语义。
19. SUBMIT 在线测试不要求 `recordId`，只填写 `data` 中的业务字段；一次成功调用同时返回新记录 ID 和流程实例 ID。
20. 下载 Markdown/OpenAPI 时，请求字段和返回字段均展示中文含义、必填性、类型、长度/格式、示例与业务规则，外围开发人员无需查看 Forge 源码即可接入。
21. 任意调用都能在 Admin 控制台按 `requestId` 串联入口、客户端、用户、能力版本、授权结果、幂等结果、最终状态和耗时；控制台不得出现 Token、密钥和完整业务报文。
22. 输入 Schema 校验失败时，在线测试和外围响应直接显示中文失败原因、`schemaPath` 与失败阶段，不得被幂等层误报为 503。
23. 桌面端在“字段与数据库映射”修改字段类型后，点击页面顶部保存必须真实提交字段；类型契约变化后明确提示同步数据表、重新发布业务单元和发布能力新版本。
24. 当能力请求使用业务字段编码、而物理列使用设计器生成列名时，CREATE_RECORD、流程 SUBMIT 和普通低代码新增均能写入正确列；若真实列未同步，错误必须同时显示业务字段、目标列、运行配置和数据表。
25. SUBMIT 第一次调用创建记录后可立即读取并启动流程；流程启动失败重试相同 `Idempotency-Key` 时复用检查点记录，不重复建单。FOLLOW_SYSTEM 数据权限使用 Token 对应的委托用户、角色和当前组织进行判断。
26. 已注册能力可以编辑目录信息；停用且撤销有效授权后可以删除，页面明确说明历史版本和日志会保留。
27. 调用与测试页面按四步分区，示例只显示 Curl/Java；请求和返回字段以中文名称、类型、必填、编码、说明和示例表格展示。
28. 新增客户端时，主体模式、服务账号、生效组织、认证模式和有效期均有面向用户的用途说明和安全建议。
29. 点击一个客户端即可在工作台内完成凭据轮换、新增/撤销授权、身份映射维护和调用日志排查，不需要在多个一级菜单间来回切换。
30. 创建或轮换凭据后回到在线测试会自动带入当前浏览器会话凭据；刷新后明确提示凭据不可反查，并能跳转到指定客户端工作台。
31. 外围用户映射列表支持分页和关键字搜索；启用手机号自动匹配前必须显示安全确认、匹配边界和失败关闭条件。
32. 调用日志列表可看到调用用户 ID、失败阶段和错误摘要；详情展示用户名/姓名、组织、结果码、Schema 路径、Trace ID 和耗时。
33. 客户端工作台调用日志可按请求 ID、能力名称或编码、调用用户 ID/用户名/姓名组合筛选；调用时间和操作列固定可见，失败记录明确显示中文失败阶段，历史失败日志缺少阶段时显示“未记录”。
34. 能力调用与测试弹窗一次只展示一个任务页签；切换客户端后默认回到“接入概览”，用户无需滚动完整契约和示例才能进入在线测试。

## 10. 测试策略

- 通用适配器：正确分派、重复适配器、未知来源、来源失效、执行异常映射。
- 文档：递归 Schema 表、业务规则、认证示例、敏感字段排除、Body/Header 幂等一致性。
- Audience：MCP/REST 分离与存量兼容。
- 流程启动：禁止调用方指定身份/模型字段、必须 USER、模型快照漂移拒绝、正常委托启动。
- 指南：网关开关、客户端状态、grant 状态、actor/auth/resource/权限诊断。
- 前端：Blob 保留、调用指南交互、Markdown/OpenAPI 下载、定向 ESLint 与生产构建。
- 在线测试：OAuth client credentials、USER Token Exchange、HMAC 签名、副作用确认、幂等 Header、敏感字段脱敏和报文下载。
- 状态恢复：当前版本有效时可重新启用，版本缺失或未发布时拒绝。
- 用户断言：错误签名/`kid/iss/aud/client_id`、过期/超长 TTL、`jti` 重放、Redis 不可用、未映射用户、管理员用户、密钥轮换/停用与 OIDC 无回退。
- 协议隔离与适配器：Capability 协议端点不触发 Sa-Token 用户日志；网关强制装配基础适配器，调用指南阻断未知来源和关闭的流程执行器。
- 版本一致性：调用指南按授权解析版本生成 Body；固定旧版本时展示版本漂移，流程绑定快照变化时阻断在线测试，一键切换后保留原授权字段策略与有效期。
- 流程记录：START 使用真实已保存 recordId 时进入流程；任意/不存在/无权限 recordId 返回 `RESOURCE_NOT_FOUND`，不创建孤立流程实例；在线测试展示可操作提示。
- 业务申请：SUBMIT 从发布模型生成字段契约，拒绝未授权字段和系统字段；同一幂等键在流程启动失败后重试复用同一记录，成功响应返回记录及流程定位信息。
- 可观测性：认证失败、授权失败、Schema 失败、执行失败、幂等命中和成功均产生不含敏感报文的结构化日志，并能按 requestId 关联。
- 类型一致性：字段类型使用 `int/int(11)/bigint unsigned/decimal(18,2)` 及 NUMBER/MONEY/SWITCH 等语义时生成正确 JSON Schema；桌面字段页顶部保存能提交当前属性面板变更。
- 聚合：相关 Maven 模块测试及 Admin 聚合编译。

## 11. 风险与回滚

本变更扩展外部调用边界，属于权限敏感变更，需人工审查。代码回滚时可先停用所有客户端用户断言，再回滚身份验证、控制面和前端代码；V1.0.79 新增列可保留为未使用兼容字段，避免破坏已执行的 Flyway 历史。既有 OIDC、业务动作、对象流程动作和 REST 网关路径保持兼容，客户端断言验签失败不会回退到 OIDC。

## 12. 确认记录

- 2026-08-01：用户确认按上述方向开发，并强调易用性、可扩展性和安全性，以普通使用者能够看懂并完成调用为验收标准。
