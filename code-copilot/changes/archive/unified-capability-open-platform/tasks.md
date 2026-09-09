# 任务拆分 — 统一能力开放平台（一期：REST 开放网关闭环）
> 拆分顺序：数据模型 → 接口协议 → 底层实现 → 上层编排 → 入口层
> 每个任务 = 可独立提交的原子变更（3-5 个文件）
> 每个任务必须精确到文件路径和函数签名

## 前置条件
- [x] spec.md 第 9 节待澄清问题全部确认（2026-07-31：签名模式一期；FLOW_ACTION USER 委托 REST 一期；新建"开放平台"一级目录）
- [ ] 本地 Redis 可用（防重放 nonce、限流、幂等锁均依赖）
- [x] Capability 三个 Pepper 由 Crypto Bootstrap 首次自动生成并稳定持久化；关闭 Bootstrap 时需显式配置

## Task 1: Flyway 迁移与字典 ✅ 2026-07-31
- **目标**: 落地一期全部结构变更与内置数据
- **涉及文件**:
    - `forge-server/db/migration/V1.0.74__capability_open_gateway.sql` — 新增：ai_capability / ai_capability_version 加 `required_actor_type`（存量 FLOW_ACTION 回填 USER）；ai_capability_client 加 `auth_modes`、`signing_key_cipher`、`signing_key_version`；新建 `ai_capability_openapi_idempotency` 表（含 del_flag、logic_delete_active 唯一索引）；字典 `ai_capability_actor_type`、`ai_capability_auth_mode`；新建"开放平台"一级目录（resource_type=1）+ 其下 4 个菜单 sys_resource + admin 角色绑定
- **关键约束**: 所有 DDL 查 `information_schema` 防重复；INSERT 显式列名 + NOT EXISTS；tenant_id=1
- **执行记录**: 已按 V1.0.51 之后的最终范式实施——幂等表逻辑删除改用墓碑 `del_flag bigint`（0 正常，删除写主键）+ `UNIQUE(tenant_id, client_id, capability_id, idempotency_key_hash, del_flag)`，**不新增 `logic_delete_active` 生成列**（AGENTS.md 5.11 禁止，V1.0.51 已全局移除该生成列）。菜单挂 `/open-platform` 一级目录，component 指向 `ai/capability/{catalog,client,grant,invocation}`，权限点复用 V1.0.21 的 `ai:capability:*`；同步插入 `sys_job_config` CAPABILITY 组幂等清理任务（默认停用，executor_handler=`capabilityOpenapiIdempotencyClean`，供 Task 7 对接）。

## Task 2: 通用开放 API 安全组件 starter ✅ 2026-07-31
- **目标**: 泛化 job 模块的限流/幂等实现为可复用组件，并新增防重放组件（job 存量不动）
- **涉及文件**:
    - `forge-server/forge-framework/forge-starter-parent/forge-starter-openapi-security/pom.xml` — 新模块，注册到 forge-starter-parent 与 forge-dependencies
    - `.../openapi/security/ratelimit/OpenApiRateLimitManager.java` — 新增，Redisson RRateLimiter，key 前缀参数化
    - `.../openapi/security/idempotency/OpenApiIdempotencyManager.java` — 新增，Idempotency-Key 校验 + SHA-256 + Redisson 锁模板方法
    - `.../openapi/security/replay/OpenApiReplayGuard.java` — 新增，timestamp 窗口 + nonce SETNX
    - `.../openapi/security/config/OpenApiSecurityAutoConfiguration.java` — 新增，条件装配
- **关键签名**:
  ```java
  public void acquire(String scopeKey, String operation, RateLimitPolicy policy); // 超限抛 429 BusinessException
  public <T> IdempotencyResult<T> execute(IdempotencyCommand command, Supplier<T> action);
  public void assertNotReplayed(String appId, long timestampMillis, String nonce); // 失败关闭
  ```
- **执行记录**: 包路径 `com.mdframe.forge.starter.openapi.security.*`；`IdempotencyCommand<T>` 带泛型（scopeKey/idempotencyKey/snapshotLoader/snapshotWriter），快照读写由调用方回调提供，DuplicateKeyException 回查兜底；配置前缀 `forge.openapi.security.*`（key-prefix/timestamp-window-millis/nonce-ttl-millis/idempotency-lock-*）；异常统一用 BusinessException 携 HTTP 语义码（400/401/409/429/503），失败关闭。已注册 forge-starter-parent modules + forge-dependencies；`mvn install -pl forge-starter-openapi-security` 编译通过（需 JDK17：`/opt/homebrew/Cellar/openjdk@17/...`）。单测按 Task 10 集中补。

## Task 3: 客户端签名凭据（依赖 Task 1；已确认一期交付） ✅ 2026-07-31
- **目标**: 机器客户端支持签名密钥的创建/轮换/吊销与 KEK 加密存储
- **涉及文件**:
    - `forge-plugin-capability-control-plane/.../domain/AiCapabilityClient.java` — 修改，新增 authModes / signingKeyCipher / signingKeyVersion 字段
    - `.../dto/CapabilityClientCreateDTO.java` — 修改，新增 `authModes`
    - `.../service/CapabilityClientService.java` + impl — 修改，创建含 SIGNATURE 模式时生成 32 字节随机密钥、KEK 加密落库、明文仅返回一次；新增 `rotateSigningKey(Long id)`
    - `.../controller/CapabilityClientController.java` — 修改，新增 `POST /ai/capability/client/signing-key/rotate/:id`（`@SaCheckPermission("ai:capability:client:edit")` + `@ApiEncrypt` + `@OperationLog`）
    - `.../mapper/AiCapabilityClientMapper.xml` — 修改，查询列补齐，列表查询不返回密文列
- **关键签名**:
  ```java
  public CapabilitySigningKeyVO rotateSigningKey(Long clientId); // 返回一次性明文 + keyVersion
  ```
- **执行记录**: 实际签名 `rotateSigningKey(Long tenantId, Long clientId)`（与现有 rotate/revoke 保持同构）。authModes 归一化为逗号分隔集合（仅允许 OAUTH/SIGNATURE，空默认 OAUTH）；含 SIGNATURE 时 SecureRandom 32 字节 Base64Url 生成密钥，经 `PersistentCryptoService.encrypt(plain, null)` KEK 加密落 `signing_key_cipher`，版本从 1 起；明文仅随创建响应（CapabilityClientSecretVO 新增 signingKey/signingKeyVersion 字段，轮换凭据路径传 null）或 `CapabilitySigningKeyVO` 返回一次。轮换用 Mapper `rotateSigningKey` 乐观锁（WHERE signing_key_version = 当前版本 AND status='ENABLED'），未启用 SIGNATURE 或已吊销拒绝。XML 拆 `BaseColumns`（含 auth_modes/signing_key_version，不含密文）与 `CredentialColumns`（额外 signing_key_cipher，仅 selectCredentialByKeyId/ById 使用）。pom 新增 forge-starter-crypto 依赖。`ai:capability:client:edit` 权限点原不存在，已在 V1.0.74 追加按钮资源（挂 AI 目录，同 V1.0.21 范式）+ admin 角色绑定。插件模块 `mvn install` 编译通过。

## Task 4: 能力元数据 requiredActorType（依赖 Task 1） ✅ 2026-07-31
- **目标**: 发布链路声明并快照 required_actor_type，授权校验可读取
- **涉及文件**:
    - `forge-plugin-capability-control-plane/.../domain/AiCapability.java`、`AiCapabilityVersion.java` — 修改，新增 requiredActorType
    - `.../dto/CapabilityPublishDTO.java` — 修改，新增 `requiredActorType`（默认 SERVICE；FLOW_ACTION 发布器强制 USER）
    - `.../service/impl/CapabilityPublishServiceImpl.java` — 修改，发布/版本快照写入
    - `forge-plugin-capability-flow-actions/.../publish/FlowActionCapabilityPublisher.java` — 修改，发布时固定 requiredActorType=USER
    - 相关 Mapper XML — 修改，查询列补齐
- **关键约束**: 存量数据由 Task 1 迁移脚本回填，代码不做运行时兜底猜测
- **执行记录**: 实际发布服务为 `CapabilityCatalogService`（非 tasks 原文的 CapabilityPublishServiceImpl）。DTO 在 visibility 之后插入可空 `requiredActorType`；`resolveRequiredActorType` 归一化：空默认 SERVICE（FLOW_ACTION 默认 USER），取值限 SERVICE/USER/BOTH，FLOW_ACTION 非 USER 直接拒绝。快照写入 capability + version 两张表；`assertImmutableVersion` 新增 requiredActorType 比对（不进 checksum/fingerprint，存量版本校验不受影响，迁移回填值与归一化值一致）。FlowActionCapabilityPublisher 传 "USER"，BusinessActionCapabilityPublisher（含高风险复用）传 "SERVICE"；两张 Mapper XML BaseColumns 补 `required_actor_type`。control-plane/flow-actions/secure-actions/high-risk-approval 4 模块编译通过，3 模块单测全部通过。

## Task 5: 开放网关插件 — 认证与编排核心（依赖 Task 2/3/4） ✅ 2026-07-31
- **目标**: 新插件实现认证解析 + 九步编排服务（先不含 Controller）
- **涉及文件**:
    - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-capability-open-gateway/pom.xml` — 新模块，admin-server 引入
    - `.../gateway/auth/OpenGatewayAuthenticator.java` — 新增，双模式认证：Bearer 委托给 `CapabilityAccessTokenService.authenticate()`；签名模式验签（KEK 解密签名密钥 → HMAC-SHA256 常量时间比较）+ `OpenApiReplayGuard.assertNotReplayed()`，产出 `AuthenticatedCapabilityIdentity`（SERVICE 身份复用 loadCurrentUser 语义加载服务用户）
    - `.../gateway/service/CapabilityInvokeOrchestrator.java` — 新增，编排：能力/grant 解析 → requiredActorType 校验 → 限流 → 幂等（写操作） → input_schema 校验 + field_policy 过滤 → `ExecutionIdentityContextHolder.open(identity)` 内调用 CapabilityExecutor → 统一响应组装 → invocation_log 落库（含拒绝路径）
    - `.../gateway/entity/AiCapabilityOpenapiIdempotency.java` + Mapper + XML — 新增，`@TableLogic(value="0", delval="id")`，查询过滤 logic_delete_active
    - `.../gateway/dto/OpenGatewayResponse.java` — 新增，`{code,message,requestId,timestamp,data}`
- **关键签名**:
  ```java
  public AuthenticatedCapabilityIdentity authenticate(HttpServletRequest request);
  public OpenGatewayResponse invoke(AuthenticatedCapabilityIdentity identity, String capabilityCode,
          String idempotencyKey, Map<String, Object> payload, String requestId);
  ```
- **执行记录**: 新模块 `forge-plugin-capability-open-gateway`（包名 `opengateway`），共 16 文件；plugin-parent module/dependencies BOM/admin-server 三处 pom 已注册。三处偏离：①`authenticate` 实际签名增加 `byte[] body` 参数（验签需 body 摘要，流只能读一次，由入口层缓存字节传入）；②`OpenGatewayProperties`+`OpenGatewayAutoConfiguration` 从 Task 6 前置到本任务（Bean 装配不可分割），且防重放窗口/nonce TTL/幂等锁参数不重复定义、直接复用 `forge.openapi.security.*`；③审计从能力解析成功后开始（解析前无 capabilityId，与 MCP 链路同构）。关键实现：Bearer 委托 `CapabilityAccessTokenService.authenticate(raw, identityProperties.getResource(), null)` 并回查凭据校验 OAUTH 模式；签名模式验 4 Header + `OpenApiReplayGuard.assertNotReplayed` + KEK 解密 HMAC-SHA256 十六进制常量时间比较（签名串 `appId\nts\nnonce\nMETHOD\nURI\nsha256Hex(body)`），镜像 loadCurrentUser SERVICE 语义加载服务用户。编排九步全部失败路径收敛为 8 错误码，不向入口层抛异常；写操作幂等走 `OpenApiIdempotencyManager` + 快照表（`@TableLogic(value="0", delval="id")`）；同步分支 BUSINESS_ACTION+MEDIUM 镜像 MCP invoke，其余走 `GovernedCapabilityExecutionAdapter`；`AtomicBoolean executionCompleted` 防快照写失败时 FAILED 审计覆盖 SUCCESS 审计。目录 SQL 硬性过滤 `risk_level <> 'HIGH'` + PUBLISHED/enabled/ENABLED 未过期 grant + 版本 requiredActorType 一致。插件与 admin-server 编译通过。

## Task 6: 开放网关入口层与配置（依赖 Task 5） ✅ 2026-07-31
- **目标**: 对外 Controller、白名单、配置组、失败关闭开关
- **涉及文件**:
    - `forge-plugin-capability-open-gateway/.../controller/CapabilityOpenGatewayController.java` — 新增，`POST /openapi/v1/capabilities/{capabilityCode}/invoke`，禁用开关时 404；异常统一转 OpenGatewayResponse 错误码
    - `.../config/OpenGatewayProperties.java` + `OpenGatewayAutoConfiguration.java` — 新增，`forge.capability.open-gateway.*`（enabled 默认 false、timestamp-window、read/write 限流、nonce-ttl、idempotency-ttl）
    - `forge-starter-auth/.../config/SaTokenConfig.java` — 修改，白名单追加 `/openapi/v1/capabilities/**`
    - `forge-admin-server/src/main/resources/application.yml` — 修改，配置组 + 环境变量 `FORGE_CAPABILITY_OPEN_GATEWAY_ENABLED`
- **关键约束**: 白名单只放行路径，鉴权全部在 OpenGatewayAuthenticator 内完成；网关响应不走 RespInfo（对外契约独立）
- **执行记录**: Controller 镜像 `JobOpenApiController` 模式：`@RestController` + `@ConditionalOnProperty(forge.capability.open-gateway.enabled)` 由组件扫描注册，开关关闭时无映射自然 404。请求体用 `@RequestBody(required=false) byte[]` 接收原始字节（ByteArrayHttpMessageConverter 支持 `*/*`），先验签后反序列化；JSON 非法→SCHEMA_INVALID 400；requestId 用 `UUID.randomUUID()`（同 MCP 兜底惯例）；入口双层 catch（OpenGatewayException + RuntimeException 兜底 500）保证异常不外溢到全局处理器；响应状态码取 `response.status()`。SaTokenConfig 登录校验与 API 权限拦截器两处同步追加 `/openapi/v1/capabilities/**`。application.yml 新增 `forge.capability.open-gateway` 配置组（enabled 默认 false + 读/写限流 + idempotency-ttl，均带环境变量覆盖）；timestamp-window/nonce-ttl 不单独定义，复用 `forge.openapi.security.*` 默认值（Task 5 偏离②的延续）。starter-auth/网关插件/admin-server 编译通过。

## Task 7: 幂等快照清理任务（依赖 Task 5） ✅ 2026-07-31
- **目标**: 超期幂等记录物理清理（留存清理场景，spec 8-4 已说明）
- **涉及文件**:
    - `forge-plugin-capability-open-gateway/.../job/OpenapiIdempotencyCleanJob.java` — 新增，每小时清理 `expires_at < NOW()` 记录
    - Task 1 迁移脚本中同步插入 `sys_job_config` 内置任务（NOT EXISTS 防重复，默认停用）
- **执行记录**: Handler 镜像 `CollaborationCallbackRetryJobHandler` 范式：`@Component + @ConditionalOnClass(IJobExecutor.class) + @IgnoreTenant + @JobHandler(value="capabilityOpenapiIdempotencyClean", group="CAPABILITY")`，与 V1.0.74 已插入的 `sys_job_config`（默认停用，cron 每小时）executor_handler 一致。分批循环删除（默认 500/批，参数可调上限 5000，MAX_BATCHES=200 防失控），复用 Task 5 已建 `deleteExpired`（XML LIMIT + `@InterceptorIgnore(tenantLine)` 跨租户）。pom 新增 forge-starter-job（普通）+ forge-plugin-job（optional，同 collaboration 先例避免传递到不启调度器的服务）。编译通过。

## Task 8: 控制台前端 — API 层与能力目录/调用日志页 ✅ 2026-07-31
- **目标**: 前端 API 封装 + 只读侧两个页面
- **涉及文件**:
    - `forge-admin-ui/src/api/ai/capability.js` — 新增，catalog/client/grant/invocation 全部接口，轮换密钥用 `postEncrypt`
    - `forge-admin-ui/src/views/ai/capability/catalog/index.vue` — 新增，能力目 录（AiCrudPage，占位符 `:id`；来源类型/风险等级/actorType 用 DictTag；schema 查 看抽屉）
    - `forge-admin-ui/src/views/ai/capability/invocation/index.vue` — 新增，调用日志（分页查询、结果状态 DictTag、耗时/错误码列，无删除操作）
- **关键约束**: 字典用 `useDict()` + computed schema；分页参数 pageNum/pageSize
- **执行记录**: ①页面文件为**平铺** `views/ai/capability/catalog.vue` / `invocation.vue`（非 `catalog/index.vue`）——动态路由按 `permission.js` 直接拼 `/src/views/${component}.vue`，V1.0.74 菜单 component 值为 `ai/capability/catalog` 无 `/index` 后缀；②「schema 查看抽屉」降级为能力元数据详情弹窗——后端 `POST /ai/capability/getById` 返回 `AiCapability`（无 inputSchema/outputSchema 字段，schema 存 `ai_capability_version` 表且一期控制台无查询端点），弹窗展示基本信息/来源版本/schemaChecksum/策略属性；③2026-08-01 补齐可视化「注册能力」入口，业务动作和流程动作分别调用受控发布接口，按已发布业务对象、可执行动作、可写字段和流程动作字典生成结构化请求，不向用户暴露原始 Schema JSON；流程对象选择后通过 `GET /ai/capability/flow-action/registration-source` 校验启用主流程绑定，并禁用不支持的 START。API 层：`publish/client/add/grant/add` 用 `postEncrypt`（后端 @ApiDecrypt），`rotate/signing-key/rotate` 亦用 `postEncrypt`（响应 @ApiEncrypt 由拦截器 `decryptResponse` 自动解密），`revoke/disable` 普通 POST。invocation 页 `resultStatus`（SUCCESS/PENDING_APPROVAL/FAILED，网关内部状态机、无字典）用 NTag 着色直渲染；`actorType` 用 DictTag(`ai_capability_actor_type`)；sourceType 无字典按原值文本展示。ESLint 通过。
- **404 修复补充**: 2026-08-01 将发布/来源校验从 MCP/REST 真实执行开关中拆出；管理控制面始终装配，真实执行 Bean 继续受 `secure-actions.enabled` / `flow-actions.enabled` 失败关闭。旧 Admin 进程返回 404 时，前端显示更新并重启 Admin 的明确提示。

## Task 9: 控制台前端 — 机器客户端/授权管理页（依赖 Task 3/8） ✅ 2026-07-31
- **目标**: 管理侧两个页面
- **涉及文件**:
    - `forge-admin-ui/src/views/ai/capability/client/index.vue` — 新增，客户端 CRUD + 凭据/签名密钥一次性展示弹窗（参考 `job-api-token.vue` 模式）+ 轮换/吊销（text-warning / text-error）
    - `forge-admin-ui/src/views/ai/capability/grant/index.vue` — 新增，授权管理 （客户端×能力矩阵、field_policy 编辑、版本策略选择、HIGH 风险禁选提示）
- **关键约束**: 密钥展示脱敏保留前4后4；一次性明文弹窗关闭后不可再取
- **执行记录**: 同 Task 8 偏离①，页面文件为平铺 `views/ai/capability/client.vue` / `grant.vue`（菜单 component 无 `/index` 后缀）。client 页：服务账号改用 `UserSelectPicker`，再从 `/system/user/{id}/org-bindings` 加载真实组织并默认选择主组织；认证模式来自字典，缺失时显示 V1.0.74 迁移错误态且禁止提交；创建时按 `authModes` 同步 `oauth_enabled`（OAUTH=1，纯 SIGNATURE=0），修复默认 OAUTH 客户端无法兑换 Token；创建/轮换密钥/轮换签名密钥均使用一次性凭据弹窗，表格不返回密文。grant 页：新增专用 `GET /ai/capability/grant/options`，不再用受 100 条上限和额外权限影响的分页接口拼下拉；每次开窗刷新候选项，区分“无客户端/无能力/加载失败/字典缺失”，过滤吊销、过期和不可用项；HIGH 风险禁选；业务动作和流程动作分别使用字段/操作多选生成 `allowedFields` / `allowedOperations`，不再接受任意 JSON。运行时授权策略允许 ACTION/FLOW，仍拒绝 HIGH 风险并保留租户、组织、状态、有效期、版本与字段策略约束。ESLint 通过。

## Task 10: 测试与端到端验证（依赖全部） ⚠️ 自动化完成，环境 E2E 待执行
- **目标**: 按 spec 8.5 落地 test-spec 并执行
- **涉及文件**:
    - `code-copilot/changes/unified-capability-open-platform/test-spec.md` — 新增
    - `forge-starter-openapi-security/src/test/java/.../OpenApiReplayGuardTest.java` 等 — 新增，签名/时间窗/nonce/限流/幂等并发单测
    - `forge-plugin-capability-open-gateway/src/test/java/.../CapabilityInvokeOrchestratorTest.java` — 新增，九步编排分支覆盖（含 ACTOR_TYPE_NOT_ALLOWED、RATE_LIMITED、幂等命中）
- **验证清单**: `mvn clean install -DskipTests` 编译通过 → 单测通过 → 启动后 curl 端到端（OAuth 模式建单 + 幂等重试同 Key 返回原结果 + 签名模式验签 + 篡改 body 拒绝 + 重放 nonce 拒绝 + USER 委托 Token 经网关调用 FLOW_ACTION 审批成功、非办理人被 FLOW_TASK_ASSIGNEE_MISMATCH 拒绝、SERVICE 身份调同能力返回 ACTOR_TYPE_NOT_ALLOWED）→ `pnpm lint:fix` 通过 → 控制台 4 页面手工走查
- **执行记录**: 2026-08-01 增量验证已完成目标单测（流程来源/发布 5 条、控制面 23 条、防重放 10 条、网关认证/编排 12 条）、前端定向 ESLint、Admin 47 模块聚合打包和前端生产构建。真实 MySQL/Redis/Flyway、OAuth/签名 curl、USER 委托流程审批和登录后浏览器走查因未启动后端与数据库而保留为部署验收门禁，详见 `execution-log.md`。

## Task 11: 客户端主体模式与兼容迁移（依赖 Task 3） ✅
- **目标**: USER_DELEGATION 客户端不绑定服务账号；存量客户端保持行为不变
- **涉及文件**:
    - `forge-server/db/migration/V1.0.76__add_capability_external_user_delegation.sql`
    - `forge-plugin-capability-control-plane/.../domain/AiCapabilityClient.java`
    - `.../dto/CapabilityClientCreateDTO.java`
    - `.../service/CapabilityClientService.java`
    - `.../security/CapabilityGrantPolicy.java`
    - `forge-admin-ui/src/views/ai/capability/client.vue`
- **关键约束**: 新增默认 USER_DELEGATION；该模式 serviceUserId/activeOrgId 必须为空且 authModes 只能 OAUTH；SERVICE/HYBRID 必须绑定有效服务账号和组织；存量行回填 HYBRID。
- **执行记录**: 已完成客户端主体模式、可空服务身份迁移、前端动态表单与兼容策略；控制面 36/36 测试通过。

## Task 12: 受信 OIDC/JWT Token Exchange 与自动身份映射（依赖 Task 11） ✅
- **目标**: 外围系统用标准 JWT 动态换取 Forge USER Token，不人工绑定操作人
- **涉及文件**:
    - `forge-plugin-capability-identity/pom.xml` — 增加 Spring Security OAuth2 JOSE
    - `.../config/CapabilityIdentityProperties.java` — 外部 provider 配置
    - `.../external/*` — JWT verifier、映射实体/Mapper/Service
    - `.../token/CapabilityTokenController.java` — RFC 8693 grant
    - `.../token/CapabilityAccessTokenService.java` — USER_DELEGATION 无 service user 语义
    - `.../oauth/CapabilityUserInfoController.java` — USER Token 身份信息
- **关键约束**: RS256；issuer/JWK/audience 服务端白名单；issuer+sub 稳定映射；手机号仅首次匹配；姓名一致性校验；用户/租户/组织/权限实时加载；无 Forge 用户时失败而非自动建号。
- **执行记录**: 已完成 RFC 8693、`/oauth2/userinfo`、稳定映射与唯一手机号匹配；无匹配/重复手机号返回 `invalid_grant`，数据库/JWK 故障失败关闭；System 46/46、Identity 48/48 测试通过。

## Task 13: 单能力 OpenAPI 3.1 文档下载（依赖 Task 8） ✅
- **目标**: 能力目录可下载当前发布版本的完整机器可读调用文档
- **涉及文件**:
    - `forge-plugin-capability-control-plane/.../service/CapabilityOpenApiDocumentService.java`
    - `.../controller/CapabilityCatalogController.java`
    - `forge-admin-ui/src/api/ai/capability.js`
    - `forge-admin-ui/src/views/ai/capability/catalog.vue`
- **关键约束**: 文档包含 invoke 路径、Bearer/HMAC 认证、Idempotency-Key、请求/响应 Schema、稳定错误码、actor type 和版本；不包含凭据及用户数据。
- **执行记录**: 已完成管理端受权下载、发布版本 Schema 生成和前端目录下载入口；控制面契约测试与前端构建通过。

## Task 14: 外部用户委托增量测试与验收（依赖 Task 11~13） ✅
- **目标**: 验证无手工账号绑定、JWT 失败关闭、USER 流程调用上下文和文档契约
- **验证清单**: 控制面/Identity/Flow Actions 目标单测；Flyway 静态检查；前端 ESLint/构建；Admin 47 模块聚合编译；真实 issuer/JWK/MySQL/Redis E2E 由用户执行并回填。
- **执行记录**: 自动化与静态验收已完成；Flow Actions 26/26、Open Gateway 13/13、Admin 47/47 聚合编译、前端 8818 modules 构建和定向 ESLint 均通过。真实 OIDC/MySQL/Redis/Flyway 与 FLOW_ACTION E2E 按约定保留为用户部署验收门禁。

## Task 15: Capability Pepper 自动引导与旧文件升级 ✅
- **目标**: 开发/单实例无需手工填写 Pepper，同时保证客户端哈希、Token 和授权码跨重启稳定。
- **涉及文件**:
    - `forge-starter-crypto/.../CryptoSecretEnvironmentPostProcessor.java`
    - `forge-starter-crypto/.../CryptoSecretEnvironmentPostProcessorTest.java`
    - `forge-admin-server/src/main/resources/application.yml`
    - `forge-admin-server/src/main/resources/application-dev.example.yml`
- **关键约束**: 三个 Pepper 分别生成 32 字节随机 Base64Url 值并互不相同；复用外部密钥文件、旧文件原子升级、POSIX 0600；环境变量/JVM 参数逐项优先；Bootstrap 关闭时不生成且继续失败关闭。
- **执行记录**: Starter Crypto 全量测试 43/43、Capability Identity 全量测试 48/48、Admin 聚合编译 47/47 通过；覆盖仅显式配置 Crypto 传输密钥时仍自动补齐 Pepper 的现场分支。
