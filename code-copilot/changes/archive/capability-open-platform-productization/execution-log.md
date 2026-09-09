# 执行日志 — 统一能力开放平台产品化改造

## 1. 基线

- 日期：2026-08-01。
- 工作区包含一期开放平台及其它用户未提交差异；本轮不重置、不清理、不提交无关文件。
- 已读取根目录/`code-copilot` 指令、项目记忆、编码规范、流程开发 Skill、前端设计 Skill与自动化测试标准。
- 已复用 `unified-capability-open-platform` 的 Spec、测试规格和执行日志。

## 2. 设计结论

- 采用代码显式注册的系统服务，不提供任意 URL 代理。
- 网关增加通用执行适配器，低代码业务动作/对象流程动作作为兼容适配器保留。
- 首个系统服务为固定已发布模型的流程启动，调用者不可传入模型或身份字段。
- Markdown 为默认人读文档，OpenAPI JSON 保留为机器文档。
- REST OpenAPI resource 与 MCP resource 分离。

## 3. 验证记录

### 3.1 执行环境与范围

- 执行日期：2026-08-02。
- Java：JDK 17，`JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home`。
- 前端：Node `v20.19.0`；生产构建使用 `NODE_OPTIONS=--max-old-space-size=8192`。
- Maven 测试均显式启用 `-Penable-tests`，并核对 Surefire `Tests run` 非 0。
- 验证范围：Control Plane、Identity、Secure Actions、Flow Actions、Open Gateway、MCP 委托身份集成、Admin 聚合装配、Capability 前端页面与 V1.0.77 权限迁移。

### 3.2 编译与定向测试

以下命令均在 `forge-server/` 执行，并使用上述 Java 17 环境：

```bash
mvn -pl :forge-plugin-capability-control-plane,:forge-plugin-capability-identity,:forge-plugin-capability-secure-actions,:forge-plugin-capability-flow-actions,:forge-plugin-capability-open-gateway -am -DskipTests test-compile

mvn -Penable-tests -pl :forge-plugin-capability-control-plane -Dtest=CapabilityOpenApiDocumentServiceTest,CapabilityCallGuideServiceTest test
mvn -Penable-tests -pl :forge-plugin-capability-secure-actions -Dtest=SystemServiceDefinitionRegistryTest,SystemServiceCapabilityPublisherTest,SystemServiceOpenGatewayAdapterTest test
mvn -Penable-tests -pl :forge-plugin-capability-flow-actions -Dtest=FlowProcessStartSystemServiceTest,FlowActionExecutionAdapterTest test
mvn -Penable-tests -pl :forge-plugin-capability-identity -Dtest=CapabilityAccessTokenServiceTest test
mvn -Penable-tests -pl :forge-plugin-capability-open-gateway -Dtest=BusinessActionOpenGatewayAdapterTest,OpenGatewayCapabilityResolverTest,CapabilityInvokeOrchestratorTest,OpenGatewayAuthenticatorTest test

mvn -pl :forge-admin-server -am -DskipTests compile
```

结果：

| 范围 | 结果 | 覆盖结论 |
|---|---|---|
| 关联模块 `test-compile` | ✅ 36/36 Reactor 模块成功 | 主码与新增测试源码均完成编译 |
| Control Plane 定向测试 | ✅ 6/6 | Markdown/OpenAPI 文档、调用指南 readiness 与安全示例 |
| Secure Actions 定向测试 | ✅ 10/10 | 注册表重复/未知定义失败关闭、发布参数白名单、系统服务网关适配 |
| Flow Actions 定向测试 | ✅ 11/11 | 流程固定快照、变量白名单、USER 委托、模型漂移拒绝及既有流程动作兼容 |
| Identity 定向测试 | ✅ 7/7 | OpenAPI/MCP audience 严格隔离 |
| Open Gateway 定向测试 | ✅ 20/20 | 适配器分派、未知来源、Bearer resource 校验和异常映射 |
| 定向测试合计 | ✅ 54/54 | 本轮 P0 核心后端差异全部通过 |
| Admin 聚合编译 | ✅ 47/47 Reactor 模块成功 | 主应用依赖和自动装配可编译 |

MCP 委托身份集成测试另行执行：

```bash
mvn -Penable-tests -pl :forge-starter-crypto,:forge-plugin-capability-identity -am -Dtest=McpDelegatedIdentityIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test
```

结果：✅ `McpDelegatedIdentityIntegrationTest` 3/3，通过 USER 委托身份上下文集成回归。

### 3.3 模块全量测试补充说明

执行五个目标插件的全量测试时，Control Plane 为 ✅ 41/41；Identity 共 49 个用例，其中首轮 46 个普通测试通过，3 个 `McpDelegatedIdentityIntegrationTest` 在 Spring Context 初始化阶段失败。失败信息为：

```text
自动密钥文件包含不允许的配置键: forge.capability.identity.authorization-code-pepper
```

根因不是产品逻辑失败，而是该次 Reactor 未包含当前工作区的 `forge-starter-crypto`，测试类路径解析到了本地 Maven 仓库中的旧版 Starter。将 `:forge-starter-crypto` 显式加入同一 Reactor 后，3/3 集成测试通过。首轮命令在 Identity 处停止，后续 Secure Actions、Flow Actions、Open Gateway 全量模块被 Maven 标为 skipped；不将它们记作全量通过，仅采用 3.2 节已经实际通过的定向用例作为本轮证据。

`CapabilityGrantPolicyTest` 的 Surefire 报告显示 `Time elapsed: 925.9 s`，但测试命令实际墙钟仅数秒且 6/6 通过，判定为测试计时元数据异常，不是性能结论。目标测试日志中的 WARN/ERROR 来自故障映射和失败关闭用例的预期分支。

### 3.4 前端与 SQL 静态验证

以下命令在 `forge-admin-ui/`、Node `v20.19.0` 下执行：

```bash
pnpm exec eslint src/views/ai/capability/catalog.vue src/views/ai/capability/grant.vue src/views/ai/capability/components/CapabilityRegisterModal.vue src/views/ai/capability/components/CapabilityCallGuideModal.vue
NODE_OPTIONS=--max-old-space-size=8192 pnpm build
```

结果：

- ✅ 4 个本轮直接相关文件定向 ESLint 零错误。
- ✅ 前端生产构建成功，构建产物包含调用指南和系统服务注册组件。
- ⚠️ 构建保留既有非阻断警告：`UserSelectModal` 组件命名冲突、静态/动态导入导致无法拆分 chunk、CSS 使用 `//` 注释；均不属于本轮功能错误。

V1.0.77 与差异静态检查：

```bash
rg -n '\$\{[^}]+\}' forge-server/db/migration/V1.0.77__add_capability_system_service_permissions.sql
git diff --check
```

结果：✅ V1.0.77 无 Flyway `${...}` 占位符，权限资源使用 `tenant_id=1` 与 `NOT EXISTS` 防重复；`git diff --check` 无空白错误。全迁移目录扫描发现的 `${...}` 仅位于既有 V1.0.72 消息模板文本，与本变更无关。

## 4. 未执行项与环境门禁

- 未连接真实 MySQL/Redis，未实际执行 V1.0.77，也未查询 `forge_schema_history` 或权限资源落库结果。
- 未启动 Admin/Flow 服务，未执行真实 OIDC Token Exchange、OAuth Client Credentials、HMAC 签名、nonce 重放、幂等重试和流程实例落库 E2E。
- 未执行浏览器登录态下的“注册流程启动能力 → 创建客户端 → 授权 → 查看调用指南 → 下载 Markdown/OpenAPI → 复制 curl 调用”完整走查。
- 上述项目依赖用户真实开发环境与已发布流程模型，保留为部署前 P1 门禁，不将静态验证或 Mock 单测替代为通过结论。

## 5. 服务清理

- 本轮未启动任何 Admin、Flow、Vite、MySQL 或 Redis 进程，无本轮 PID 需要停止。
- 工作区原有进程与其它未提交修改均未触碰。

## 6. Capability 文档服务启动失败修复

- 日期：2026-08-02。
- 现象：Admin 创建 `CapabilityOpenApiDocumentService` 时抛出 `No default constructor found`。
- 根因：该 Service 同时存在生产四参数构造器与测试便利三参数构造器；多构造器场景下未显式标记注入构造器，Spring 回退为无参实例化。
- 修复：在生产四参数构造器上显式添加 `@Autowired`，保留必需依赖的构造器注入，不增加可能产生空依赖的无参构造器。
- 回归：新增 `ApplicationContextRunner + @Import` 容器装配测试，真实经过 Spring Bean 创建路径。

| 范围 | 命令 | 结果 |
|---|---|---|
| 失败基线 | `mvn -Penable-tests -pl :forge-plugin-capability-control-plane -Dtest=CapabilityOpenApiDocumentServiceTest test` | 按预期复现：3 个用例中新增容器测试 1 个失败，异常与用户日志一致 |
| 定向复跑 | 同上 | ✅ 3/3，Spring Context 正常创建文档服务 |
| Control Plane 全量 | `mvn -Penable-tests -pl :forge-plugin-capability-control-plane test` | ✅ 42/42，Failures 0、Errors 0、Skipped 0 |
| Admin 聚合编译 | `mvn -pl :forge-admin-server -am -DskipTests compile` | ✅ 47/47 Reactor 模块，`BUILD SUCCESS`；仅保留既有 deprecated/unchecked 编译警告 |

- 本次没有启动 Admin；需要重启现有 Admin 进程后加载修复类。

## 7. Open Gateway 缺少 Token Service 装配修复

- 日期：2026-08-02。
- 现象：Admin 创建 `OpenGatewayAuthenticator` 时找不到 `CapabilityAccessTokenService`。
- 根因：Open Gateway 与 Identity 使用两个独立的自动配置条件；存在 `open-gateway.enabled=true`、`identity.enabled=false` 的半开启组合，网关已创建但令牌服务未装配。
- 修复：Identity 自动配置改为“Identity 开启或 Open Gateway 开启”任一满足即生效；Open Gateway 声明在 Identity 之后装配。应用配置恢复安全默认关闭网关，显式开启网关时自动带起 Identity。
- 一致性：调用指南用 `identityEnabled || gatewayEnabled` 计算有效 Identity 状态，避免运行态可用但页面误报 OAuth 不可用。

验证命令均在 `forge-server/` 下使用 JDK 17 执行：

```bash
mvn -Penable-tests -pl :forge-plugin-capability-identity -Dtest=CapabilityIdentityAutoConfigurationTest test
mvn -Penable-tests -pl :forge-plugin-capability-open-gateway -Dtest=OpenGatewayAutoConfigurationTest test
mvn -Penable-tests -pl :forge-plugin-capability-control-plane -Dtest=CapabilityCallGuideServiceTest test
mvn -Penable-tests -pl :forge-plugin-capability-control-plane,:forge-plugin-capability-identity,:forge-plugin-capability-open-gateway test
mvn -pl :forge-admin-server -am -DskipTests compile
git diff --check
```

| 范围 | 结果 |
|---|---|
| Identity 失败基线 | 新增网关强制 Identity 用例按预期失败：2 个用例中 1 个找不到 `CapabilityAccessTokenService` |
| Identity 定向复跑 | ✅ 2/2，网关开启且 Identity 显式关闭时仍提供 Token Service |
| Open Gateway 联合装配 | ✅ 1/1，同时创建 Token Service、认证器和调用编排器 |
| 调用指南失败基线 | 新增有效 Identity 状态用例按预期失败：5 个用例中 1 个 `ready=false` |
| 调用指南定向复跑 | ✅ 5/5，OAuth 状态及示例与真实运行态一致 |
| 三模块全量测试 | ✅ Control Plane 43/43、Identity 50/50、Open Gateway 21/21，共 114/114 |
| Admin 聚合编译 | ✅ 47/47 Reactor 模块，`BUILD SUCCESS` |
| 差异静态检查 | ✅ `git diff --check` 无空白错误 |

- Identity 测试中的数据库不可用 ERROR、MCP 认证 WARN，以及 Open Gateway 的 `INTERNAL_ERROR` WARN，均为既有故障映射测试的预期分支，不是测试失败。
- 本轮未启动或重启 Admin、Flow、MySQL、Redis；修改生效前需要重启现有 Admin 进程。

## 8. 授权空参数与字典英文展示修复

- 日期：2026-08-02。
- 现象一：新增能力授权时后端返回“不能为null；不能为空；不能为空；不能为null”，日志显示四个必填字段均为空。
- 根因一：页面运行期间后端加密配置发生变化；浏览器仍按旧配置发送 `{data, algorithm}` 加密信封，而当前后端已关闭 API 解密，Spring 因此只能绑定出空业务 DTO。
- 修复一：所有 `postEncrypt` 显式敏感请求在提交前重新读取 `/crypto/config`。服务端关闭加密时发送普通 DTO；开启时继续强制密钥协商与加密；配置无法确认时失败关闭。授权 DTO 同时补充字段级中文校验消息。
- 现象二：能力目录和授权流程操作偶发显示英文枚举，来源类型与行为类型始终没有中文字典。
- 根因二：`useDict` 将请求失败返回的空数组写入全局缓存，单次瞬时失败会持续到 SPA 整体刷新；同时数据库缺少来源类型、行为类型两个字典。
- 修复二：字典改为 `Promise.allSettled` 逐项处理，仅缓存成功结果，失败项自动重试一次且不覆盖已成功数据；授权弹窗支持刷新字典并在流程操作未翻译时禁止提交。新增 V1.0.78 字典迁移，能力目录表格/详情和系统服务摘要统一读取字典。
- 变更范围：前端 HTTP 加密拦截器、公共 `useDict`、能力目录/授权/注册组件、授权 DTO、V1.0.78 迁移及本变更文档。
- 验证状态：按用户明确要求，本轮未执行 Maven、前端 ESLint/build、Flyway、服务启动、接口调用或浏览器测试；所有结果保持“待用户验证”，未表述为通过。
- 服务清理：本轮未启动、停止或重启任何服务，未触碰用户现有 Admin 进程。

## 9. OAuth Token 404、能力重新启用与在线测试

- 日期：2026-08-02。
- OAuth 404 根因：Open Gateway 开启时 Identity 自动配置会提供 `CapabilityAccessTokenService`，但 Token Controller 等公开组件仍只判断 `identity.enabled=true`，形成“服务已装配、路由未注册”的半开启状态。
- OAuth 修复：新增统一 `CapabilityIdentityRequiredCondition`；Identity 或 Open Gateway 任一开启时，自动配置、Token/OAuth 路由、UserInfo、MCP 身份上下文、调用审计和 Token 清理任务采用同一装配语义。
- 能力恢复：新增 `/ai/capability/enable/{id}`，重新启用前校验当前版本存在且为 `PUBLISHED`；前端停用状态显示绿色“启用”操作并二次确认。
- 在线测试：调用指南使用浏览器临时凭据依次调用真实 Token/开放网关；支持 OAuth Client Credentials、OIDC Token Exchange 和 HMAC-SHA256。副作用能力二次确认并自动携带一次性幂等键。
- 接入材料：新增 OAuth/HMAC Java 17 标准库示例、客户端级 Markdown 接入示例和最近一次测试 JSON 报文下载；凭据、Token 与签名在页面展示及下载时统一脱敏。
- 验证状态：按用户明确要求，本轮未执行 Maven、前端 ESLint/build、Flyway、服务启动、接口调用或浏览器测试；所有结果保持“待用户验证”，未表述为通过。
- 服务清理：本轮未启动、停止或重启任何服务。

## 10. 无统一 OIDC 的客户端签名用户断言

- 日期：2026-08-02。
- 身份协议：为 USER_DELEGATION/HYBRID OAuth 客户端增加独立 RSA-2048 密钥；外围系统使用最长两分钟的 RS256 JWT 和专用 `subject_token_type` 做 Token Exchange。
- 可信映射：管理员预绑定外围 `sub` 到 Forge 普通用户；原始 `sub` 不落库，只保存 SHA-256 与脱敏提示。运行时禁止管理员身份，并实时加载用户状态、租户、组织和角色。
- 安全校验：固定校验 `alg/kid/iss/aud/client_id/sub/iat/exp/jti`，使用 Redis 防重放；验签、防重放或目录基础设施不可用时失败关闭，OIDC 与客户端断言不得回退。
- 密钥治理：Forge 只保存 X.509 公钥、`kid` 和版本；PKCS#8 私钥通过加密响应只展示一次。轮换和停用同步递增客户端 `credential_version`，撤销旧客户端 Token。
- 管理端：客户端页面增加“用户断言”入口、密钥下载、协议参数、用户选择和脱敏映射列表；停用后可再次轮换生成新密钥。
- 调用闭环：调用指南默认生成专用 Token Exchange curl，增加完整 Java 17 RS256 示例；在线测试可选择 OIDC 或客户端断言，并用浏览器 Web Crypto 临时签名。
- 数据迁移：新增 `V1.0.79__add_capability_client_user_assertion.sql`，全部新增字段使用 `information_schema` 防重复，未写入任何密钥或真实用户标识。
- 验证状态：按用户明确要求，本轮未执行 Maven、前端 ESLint/build、Flyway、服务启动、接口调用或浏览器测试；所有结果保持“待用户验证”，未表述为通过。
- 服务清理：本轮未启动、停止或重启任何服务，未触碰用户现有 Admin/Flow/Vite 进程。

## 11. Capability Token 误报与执行适配器现场修复

- 日期：2026-08-02。
- 日志结论：`/oauth2/token` 未登录属于 OAuth 公开端点的正常状态；`fdu_` 是 Capability 短期 Token，不是 Sa-Token。通用操作日志切面在网关控制器执行前尝试读取 Sa-Token 用户，造成“token 无效”堆栈，但真实网关认证随后已成功。
- 协议隔离：租户拦截器直接跳过 Capability OAuth/OpenAPI 协议入口的 Sa-Token 解析；通用操作日志默认且硬性排除 `/oauth2/token`、`/oauth2/revoke` 与能力开放网关路径，凭据和业务报文继续由专用安全审计治理。
- 适配器修复：Open Gateway 自动配置显式等待 Secure Action/Flow Action 配置，并强制注入业务动作、系统服务基础适配器；缺失来源错误增加 `sourceType/behavior` 提示。
- 易用性：调用指南增加“执行能力”阻断项。`BUSINESS_ACTION/ACTION`、`SYSTEM_SERVICE/ACTION` 和已开启的 `FLOW_ACTION/FLOW` 可进入测试；旧的未知来源或关闭的流程执行器会提前给出中文修复建议。
- 用户映射：客户端签名用户断言仍默认要求管理员预绑定，但每个外围 `sub` 只需绑定一次，后续所有 Token Exchange 复用。该边界用于阻止客户端任意指定 Forge 用户；受信 OIDC 模式仍可按已验证手机号完成首次自动映射。
- 验证状态：按用户明确要求，本轮未执行 Maven、前端 ESLint/build、服务启动或接口测试；`git diff --check` 无新增空白错误，目标文件未发现冲突标记，运行结果待用户验证。

## 12. DataScope 与 MyBatis-Plus 启动循环依赖修复

- 日期：2026-08-03。
- 现象：Admin 启动创建 `sysJobConfigMapper` 时失败，异常链最终落在 `MybatisPlusAutoConfiguration -> DataScopeInterceptor -> DataScopeServiceImpl -> SysDataScopeConfigMapper -> sqlSessionFactory` 的循环依赖；`jobAutoRegistrar` 只是最先触发 Mapper 创建的入口。
- 根因：MyBatis-Plus 在创建全局拦截器集合时实例化 `DataScopeInterceptor`，自动配置方法又直接注入依赖 Mapper 的 `IDataScopeService`，导致 `SqlSessionFactory` 尚未完成创建时反向请求自身。
- 修复：`DataScopeAutoConfiguration` 改为注入 `ObjectProvider<IDataScopeService>`，通过 `Supplier` 将 Service 解析延迟到普通业务 SQL 真正进入数据权限拦截器时；数据权限模块自己的 Mapper 仍在解析 Supplier 前直接跳过，避免递归。保留原 Service 构造器以兼容现有调用和测试，延迟服务未就绪时明确失败关闭。
- 静态检查：`DataScopeInterceptor` 不再保存直接 Service 引用，运行时调用统一通过延迟取值；`git diff --check` 无空白错误。
- 验证状态：按用户明确要求，本轮未执行 Maven、服务启动或接口测试，启动结果待用户环境验证。

## 13. OAuth Token Exchange 缺少可信租户上下文修复

- 日期：2026-08-03。
- 现象：客户端签名用户断言已完成验签后，`AiCapabilityExternalIdentityMapper.selectActive` 查询报 `访问租户表[ai_capability_external_identity]时缺少租户上下文`，Token 端点返回 `temporarily_unavailable`。
- 根因：Capability OAuth/OpenAPI 为避免误读 Sa-Token 登录态而跳过通用租户解析，但协议自身只完成了客户端认证，没有在访问租户表前使用客户端绑定的可信 `tenantId` 建立上下文。首个异常出现在外围身份映射；继续执行还会影响 Token 签发、Bearer 校验和撤销等租户表访问。
- 修复：新增 `CapabilityTenantContext`，在已验证客户端或 Token 的租户内执行安全数据访问，强制 `ignoreTenant=false`、跳过不适用于认证基础设施的用户数据权限，并嵌套安全地恢复租户、忽略标记和数据权限标记。
- 覆盖：Token Exchange 外围映射、Token 签发、Bearer Token 校验、Token 撤销、全局客户端/Token 凭据查询和 HMAC 服务用户加载。客户端及 Token 租户为空或非法时继续失败关闭。
- 日志：已识别的 RFC 8693 grant 在安全日志中显示为 `token_exchange`，不再误显示 `unknown`。
- 静态检查：`git diff --check` 无空白错误，目标 Java/XML 未发现冲突标记。
- 验证状态：按用户明确要求，本轮未执行 Maven、服务启动或接口测试，运行结果待用户环境验证。

## 14. 能力审计 SQL 解析与失败路径租户上下文修复

- 日期：2026-08-03。
- 现象：能力授权目录查询成功后，执行前审计预留立即抛出 `MyBatisSystemException`，开放网关返回“能力审计服务暂时不可用”；日志中没有出现对应审计 UPDATE 的 `Preparing` 记录。
- 根因：`AiCapabilityInvocationLogMapper.updateResultByRequestIdentity` 使用 MySQL NULL 安全等号 `<=>` 比较可空的 `service_user_id`。租户拦截器会先通过 JSqlParser 改写 SQL，而当前解析器不支持该运算符，因此 SQL 尚未发送到数据库就失败。
- 修复：改用 `service_user_id = ? OR (service_user_id IS NULL AND ? IS NULL)` 的标准等价表达式，保留 USER 委托时 `service_user_id=NULL` 的身份匹配语义；数据库字段原本已允许 NULL，无需新增迁移。
- 二次现象：SQL 解析问题修复后，失败补记审计报 `访问租户表[ai_capability_invocation_log]时缺少租户上下文`。完整堆栈确认调用来自 `CapabilityInvokeOrchestrator.failure()`，而外层 `try-with-resources` 已先关闭开放网关上下文。
- 生命周期根因：审计服务过去隐式依赖调用方线程上下文；成功审计位于 `OpenGatewayContextBridge` 内，失败审计位于其关闭之后，导致相同审计方法在不同路径具有不同租户语义。
- 完整修复：`CapabilityInvocationAuditService` 对 record、recordOrUpdate、冲突查询和分页统一以已校验的显式 `tenantId` 建立受控上下文，强制 `ignoreTenant=false`，跳过不适用于审计基础设施的用户 DataScope，并在 finally 中恢复原租户、租户忽略和 DataScope 标记。未把审计表加入租户忽略名单。
- 可观测性：审计失败 WARN 现在附带底层异常堆栈，只保留 requestId、能力编码和异常类型等安全元数据，不记录 Token、密钥或业务报文。
- 验证资产：补充失败审计独立建立可信租户、强制关闭租户忽略并恢复原上下文的单元测试代码；按用户明确要求未执行该测试、Maven、服务启动或接口调用，仅执行静态差异检查，运行结果待用户环境验证。

## 15. 已发布能力版本升级与授权版本修改

- 日期：2026-08-03。
- 现场问题：业务对象重新发布后，流程能力旧版本快照与当前绑定版本不一致；平台虽然要求创建新版本，但能力目录没有升级入口，用户只能看到“已发布能力版本不可修改”。
- 能力升级：目录增加“发布新版本”，服务端返回当前不可变版本草稿和下一补丁版本建议；前端自动回填原业务对象、流程动作、字段策略或系统服务参数，并锁定能力编码和来源身份。
- 版本安全：新版本必须严格高于当前语义版本；同一能力编码不得借升级切换来源类型或来源标识。受控发布器仍重新读取当前流程绑定，因此本次流程对象版本变化会进入新能力快照。
- 授权闭环：有效授权增加“修改”入口，可将 `PINNED` 基准版本切换到当前版本，或改为 `FOLLOW_MAJOR`。服务端按目标版本重新校验字段/操作策略，不自动修改现有固定授权。
- 验证状态：按用户明确要求，本轮未执行 Maven、前端 ESLint/build、服务启动、接口调用或浏览器测试，运行结果由用户环境验证。
- 服务清理：本轮未启动、停止或重启任何服务。

## 16. 调用指南授权版本与流程绑定一致性修复

- 日期：2026-08-03。
- 现场结论：能力 `1.0.1` 已发布成功并包含新的流程绑定，但所选客户端授权仍为 `PINNED + 1.0.0`；调用指南和开放网关按授权解析旧版本，因此测试继续命中旧流程快照并返回 `FLOW_BINDING_MISMATCH`。发布能力版本本身不会自动修改既有授权，这是版本安全策略，不是发布失败。
- 版本展示：调用指南新增能力当前版本、授权策略、授权基准版本和实际调用版本，旧授权时显示明确警告，不再只显示含糊的“能力版本 1.0.0”。
- 绑定诊断：`FLOW_ACTION` 同时比较授权版本与当前版本的 `bindingId`、`flowModelKey`、`publishedObjectVersion`；快照变化时在真实调用前显示中文阻断原因并禁用在线测试。
- 授权切换：调用指南提供“切换到当前版本”，后端保留原版本策略、字段/操作白名单和有效期，按当前已发布版本重新校验后只更新授权基准版本，成功后刷新指南。
- 契约一致性：请求 Body、OAuth/HMAC 命令和 Java 示例改为从客户端实际授权版本生成，避免旧授权混用当前版本 Schema。
- 验证状态：按用户明确要求，本轮未执行 Maven、前端 ESLint/build、服务启动、接口调用或浏览器测试；仅进行静态差异检查，运行结果由用户环境验证。
- 服务清理：本轮未启动、停止或重启任何服务。

## 17. 流程 START 真实记录约束与错误语义修复

- 日期：2026-08-03。
- 现场结论：版本、授权、流程绑定和用户委托均已通过；请求使用 `recordId=121212` 进入低代码业务对象查询后未找到当前委托用户可见的请假记录。START 的职责是为已保存记录启动流程，不会创建请假申请，因此原始业务拒绝是正确的。
- 易用性：调用指南按实际授权版本识别 `FLOW_ACTION` 和操作类型，在在线测试请求区明确提示 recordId 必须来自已保存业务记录、记录受实际委托用户数据权限约束、START 不创建记录；示例改用 `<REAL_RECORD_ID>`，避免把随机数字误当可执行示例。
- 客户端校验：FLOW_ACTION 测试前校验 recordId 为正整数长整型字符串，START 强制 `arguments={}`；下载的外围接入示例同步包含请求前提。
- 错误契约：业务记录不存在或不可见改为 HTTP 404 + `RESOURCE_NOT_FOUND`，保留统一文案防止记录存在性枚举；在线测试摘要直接展示服务端错误码和原因，不再只提示 HTTP 状态或误报 `SCHEMA_INVALID`。
- 版本契约：后续新发布的流程能力版本会在输入 Schema 中携带 recordId 格式和业务说明；当前已发布版本无需再次升级，也能通过调用指南的动态提示和占位符获得正确测试体验。
- 验证状态：按用户明确要求，本轮未执行 Maven、前端 ESLint/build、服务启动、接口调用或浏览器测试；仅进行静态差异检查，运行结果由用户环境验证。
- 服务清理：本轮未启动、停止或重启任何服务。

## 18. 一次提交业务申请与开放网关安全日志

- 日期：2026-08-03。
- 能力语义：新增 `FLOW_ACTION/SUBMIT`。外围系统请求只提交 `data` 业务字段；Forge 使用可信 USER 委托身份补齐申请人、归属人、租户、组织、审计和初始单据状态，在一次调用中创建低代码业务记录并启动已发布主流程。原 `START` 改为“发起已有记录流程”，继续只接受真实 `recordId`。
- 发布契约：从业务对象不可变发布模型生成字段白名单、必填项、JSON 类型、长度、日期格式、默认值、字典类型和业务说明；系统字段、只读字段、公式字段、主键、租户、审计、状态和流程字段不进入开放 Schema。成功响应明确返回 `recordId/businessKey/processInstanceId/flowModelKey/flowStatus`。
- 幂等恢复：流程动作日志在独立事务中先预留；SUBMIT 创建业务记录与写入 `recordId` 检查点使用同一 `REQUIRES_NEW` 事务。流程启动失败后，复用相同 `Idempotency-Key` 只会使用检查点记录，不会重复建单；稳定业务键继续用于远端流程成功、本地回填失败后的恢复。
- 数据源边界：主库低代码运行对象支持 SUBMIT。外部运行数据源无法与本地能力日志共享原子事务，注册来源接口和注册页提前标记不支持并给出中文原因；直接发布或执行仍返回冲突错误，避免产生无检查点业务记录。
- 易用性：注册页默认优先“提交业务申请”，展示允许输入字段和模型必填字段；客户端授权可在能力版本白名单内进一步收窄申请字段，模型必填字段不能移除，在线测试示例按实际授权字段生成。调用指南、Markdown/OpenAPI、在线测试和下载样例均说明请求前提、字段含义、返回字段及幂等重试方式，SUBMIT 不再要求 `recordId`。
- 可观测性：开放网关按 `requestId` 输出入口、认证、授权、输入准备、Schema、策略、审计预留、适配器执行、幂等命中、成功和失败日志。失败日志增加 `failureStage/resultCode/httpStatus/schemaPath/durationMs/exceptionType`；5xx 保留异常堆栈，4xx 不打印堆栈。日志不记录 Authorization、Token、Secret、签名、Nonce、Idempotency-Key 原文或请求/响应 Body。
- 数据迁移：新增 `V1.0.81__add_capability_flow_submit_operation.sql`，增加“提交业务申请”字典项并设为默认，将 START 展示名改为“发起已有记录流程”；内置数据 `tenant_id=1`，写入具备防重复保护。
- 静态检查：执行 `git diff --check`，目标目录未发现冲突标记；流程动作与能力审计 Mapper 已移除 MySQL NULL 安全等号运算符，安全日志关键字检查未发现业务报文或凭据输出。
- 验证状态：按用户明确要求，本轮未执行 Maven、pnpm、服务启动、Flyway、接口调用或浏览器测试；运行结果由用户环境验证。
- 服务清理：本轮未启动、停止或重启任何服务。

## 19. 业务动作缺少执行步骤的提前诊断

- 日期：2026-08-03。
- 现象：注册业务动作能力时，页面允许选择已启用动作，提交后才由 `SecureActionStepValidator` 返回“受控业务动作缺少执行步骤”。
- 根因：前端候选仅检查 `status != 0`，而真实发布还要求已发布快照存在非空执行步骤，且当前仅允许 `UPDATE_FIELD/CREATE_RECORD`；页面与服务端判断事实不一致。
- 后端修复：业务对象动作服务增加全量发布动作解析；新增 `/ai/capability/business-action/registration-source`，从同一份不可变发布快照返回动作、可写字段、发布版本及可发布诊断。
- 失败关闭：`SecureActionStepValidator` 继续作为发布和运行时安全边界，同时输出可复用诊断；直接调用发布接口仍会拒绝空步骤、不支持步骤和嵌套步骤。
- 前端修复：动作下拉不再调用草稿动作/字段接口，改用能力注册来源；不可发布项保留但禁用，展示具体原因、发布快照版本和修正入口，提交按钮同步禁用。
- 静态检查：`git diff --check` 通过；本轮目标 Java/Vue/JS/文档未发现冲突标记；新增差异未发现打印 Token、Secret、签名、Nonce 或请求/响应 Body 的日志。
- 验证状态：按用户明确要求，本轮未执行 Maven、pnpm、服务启动、Flyway、接口调用或浏览器测试；仅执行静态差异检查，运行结果由用户环境验证。
- 服务清理：本轮未启动、停止或重启任何服务。

## 20. 离职申请“新增”动作语义核查与引导修复

- 日期：2026-08-03。
- 数据结论：只读查询确认离职申请对象 `HR/LEAVE_APPLICATION` 当前状态为 `PUBLISHED`，最新发布版本为 v39；当前设计与 v39 快照中的 `add/新增` 均为 `actionType=OPEN_PAGE`、`actionConfig={}`。因此不是动作丢失或未重新发布，而是页面按钮没有服务端执行步骤。
- 分类修复：自动化设计器同时根据动作类型和步骤识别入口。`START_FLOW` 归入流程入口；无执行步骤的新增/编辑/删除等页面操作不再显示成业务自动化；`COMMAND/TRIGGER` 和已有执行步骤的兼容动作继续保留。
- 注册引导：业务动作候选仍保留不可发布项用于解释，但 `OPEN_PAGE` 标记为“页面操作，不能直接开放”；申请对象可以在诊断区直接切换到 `FLOW_ACTION/SUBMIT`，由 Forge 一次完成创建记录和发起流程。
- 服务端契约：直接发布 `OPEN_PAGE` 或 `START_FLOW` 空步骤动作时返回对应语义和正确注册路径，安全白名单及失败关闭策略不变。
- 静态检查：执行 `git diff --check`，未发现新增空白错误。
- 验证状态：按用户明确要求，本轮未执行 Maven、pnpm、服务、Flyway、接口或浏览器测试；页面与运行结果由用户环境验证。
- 服务清理：本轮未启动、停止或重启任何服务。

## 21. 流程动作发布 Schema 与内核子集对齐

- 日期：2026-08-03。
- 现场异常：`FlowActionCapabilityController.publish` 在目录保存前被 `CapabilitySchemaValidator` 拒绝，路径为 `$.properties.recordId.pattern`；说明流程动作发布器生成了能力内核未实现的 Schema 关键字。
- 完整核查：除 `pattern` 外，SUBMIT 字段还可能生成 `format/multipleOf/default/example`，数组字段原先生成空 `items={}`，均与内核“每个节点必须显式声明受支持 type”的规则冲突。
- 修复策略：不扩大核心 Schema 白名单，不让未实现关键字绕过运行时语义。流程动作发布器只生成当前内核完整支持并实际校验的关键字；recordId 数字格式由说明和 `FlowActionExecutionAdapter.parseRecordId` 强制校验。
- 字段契约：日期格式、小数精度、默认值行为继续进入字段说明；低代码运行时仍执行实际模型校验和数据库约束。数组 `items` 显式声明受控元素类型，避免再次出现无类型 Schema 节点。
- 数据核查：Flyway v1.0.81 已成功执行，流程动作字典包含 `SUBMIT/START/APPROVE/REJECT`，其中 SUBMIT 为默认值；本次错误不是字典缺失。
- 回归资产：发布器测试增加 START Schema 内核校验，并覆盖包含日期、小数、默认值和数组字段的 SUBMIT Schema。按用户要求未执行测试。
- 静态检查：执行 `git diff --check`，并确认流程动作发布源码不再生成上述未支持关键字。
- 验证状态：未执行 Maven、pnpm、服务、Flyway、接口或浏览器测试，发布结果由用户环境验证。
- 服务清理：本轮未启动、停止或重启任何服务。

## 22. 流程动作字典存在但 SUBMIT 页面不可见

- 日期：2026-08-03。
- 数据结论：只读查询确认 `forge_schema_history` 中 v1.0.81 执行成功；`ai_capability_flow_operation` 存在启用的 SUBMIT、START、APPROVE、REJECT，SUBMIT 的 `dict_sort=1/is_default=Y`。
- 链路结论：能力注册使用 `useDict`，其模块级 `dictCache` 在整个 SPA 生命周期内无过期时间；组件后续挂载会直接复用旧列表。字典 `/system/dict/data/list` 调用 `selectDictDataList` 直接查库，本次不是 Redis 字典缓存或 Flyway 漏执行。
- 前端修复：能力注册弹窗每次打开先强制刷新流程动作字典，再初始化默认操作；从业务动作诊断切换到 SUBMIT 前再次刷新，防止旧列表导致回退到 START。
- 失败提示：刷新失败或响应仍不含 SUBMIT 时，流程动作区域展示错误和手动重新加载入口；操作标签和排序仍来自系统字典，没有在页面复制一份硬编码 options。
- 静态检查：执行 `git diff --check`，目标 Vue 文件未发现冲突标记。
- 验证状态：按用户明确要求未执行 pnpm、服务、接口或浏览器测试，页面结果由用户环境验证。
- 服务清理：本轮未启动、停止或重启任何服务。

## 23. Schema 异常被幂等层误报 503

- 日期：2026-08-03。
- 现场现象：SUBMIT 调用日志停在 `failureStage=INPUT_SCHEMA_VALIDATION`，但外部响应为 HTTP 503“开放API幂等服务暂不可用”，堆栈只保留二次创建的 `BusinessException`，没有原始校验原因和字段路径。
- 根因：`OpenApiIdempotencyManager.execute` 在 Redis 加锁后仍用同一个宽泛 `catch (RuntimeException)` 包围 `action.get()`；`CapabilitySchemaValidationException` 属于运行时异常，因此被错误重写为幂等基础设施故障。
- 异常边界：Redis 客户端/锁、幂等快照读取、并发冲突回查和快照写入继续失败关闭为 503；业务 action 在锁内执行但异常原样向上传递，由开放网关按 Schema、授权或业务错误正确映射。
- 可诊断性：真实幂等故障按 `REDIS_CLIENT/LOCK_ACQUIRE/SNAPSHOT_LOAD/DUPLICATE_SNAPSHOT_LOAD/SNAPSHOT_WRITE` 输出 phase、scope 和完整异常链，不记录幂等键或业务报文。Schema 失败返回中文原因，并在响应 data 中返回 `failureStage/schemaPath`。
- 验证状态：按用户明确要求未执行 Maven、pnpm、服务、接口或浏览器测试；仅进行源码和差异静态检查，运行结果由用户环境验证。
- 服务清理：本轮未启动、停止或重启任何服务。

## 24. 低代码整数保存断链与能力类型映射修复

- 日期：2026-08-03。
- 数据结论：只读核查确认离职申请当前草稿、v39 发布快照和物理列中的 `price` 均仍为 `varchar/TEXT/input`，现有能力 `1.0.0` 因而按不可变来源快照正确生成了 `string`；能力平台没有权限按字段名猜成整数。
- 页面根因：桌面版字段属性面板常驻显示，但顶部统一保存调用 `saveSelectedField` 时错误检查了仅代表移动端抽屉显示状态的 `propertyVisible`，导致用户已经选择整数后点击顶部保存并未调用字段更新接口。
- 页面修复：桌面布局直接读取当前属性面板并保存；紧凑布局仍要求先打开抽屉。属性面板尚未挂载时返回明确提示，不再静默空提交。
- 操作引导：字段的业务类型、数据类型或默认组件发生变化时，成功消息明确要求在发布检查中同步数据表、重新发布业务单元，再发布能力新版本。
- 映射修复：新增流程动作与业务动作共用的低代码 Schema 类型解析器；归一化 SQL 长度、精度和 unsigned 修饰，支持 `int(11)`、`bigint unsigned`、`decimal(18,2)`、`tinyint(1)`，并识别 NUMBER/MONEY/SWITCH 及数字组件语义。
- 安全边界：没有修改当前数据库记录、v39 对象快照或能力 `1.0.0`；旧契约继续不可变，类型修正只会通过后续对象发布和能力新版本生效。
- 验证状态：按用户明确要求未执行 Maven、pnpm、服务、Flyway、接口或浏览器测试；仅进行源码和差异静态检查，运行结果由用户环境验证。
- 服务清理：本轮未启动、停止或重启任何服务。

## 25. 低代码字段编码与物理列映射修复

- 日期：2026-08-03。
- 现场现象：能力执行进入 `ADAPTER_EXECUTION` 后返回 `SCHEMA_INVALID`，具体消息为“字段不存在: dpe”；输入 Schema 和客户端字段授权已经通过。
- 数据结论：只读核查确认离职申请对象 `HR/LEAVE_APPLICATION`、`ai_crud_config` 和最新发布设计版本均为版本 40，运行模型和发布快照都存在业务字段 `dpe`；该字段显式映射到物理列 `field_input4`，`hr_leave_application.field_input4` 真实存在且类型为 `varchar(128)`。
- 根因：`DynamicCrudService.insertInternal` 的允许字段来自发布模型，但写入列映射只读取数据库元数据并做 camelCase/snake_case 推导，把业务字段 `dpe` 错当成同名物理列，忽略了模型中的 `columnName=field_input4`。
- 修复：动态 CRUD 主模型列映射叠加发布态 `LowcodeFieldSchema.field -> columnName`，覆盖查询、普通写入、内部动作写入、关联主表、单号、脱敏和导出；读取链路补充物理列到业务字段的兼容别名。
- 诊断：若模型目标列确实不存在，错误现在返回业务字段、目标列、运行配置和运行表，并提示先同步低代码数据表结构后重新发布能力。
- 安全边界：未修改离职申请业务数据、发布快照、能力版本或物理表；字段白名单、标识符校验、真实列校验、租户和数据权限保持不变。
- 静态检查：执行 `git diff --check`，目标 Java 和变更文档未发现空白错误；未发现新增凭据或业务报文日志。
- 验证状态：按用户明确要求未执行 Maven、pnpm、服务、Flyway、接口或浏览器测试，运行结果由用户环境验证。
- 服务清理：本轮未启动、停止或重启任何服务。

## 26. SUBMIT 新记录被旧事务快照误判为不可见

- 日期：2026-08-03。
- 现场现象：能力 `business.hr.leave_application.submit` v1.0.1 在字段写入修复后返回 HTTP 404 + `RESOURCE_NOT_FOUND`，提示记录不存在或无权限访问；用户确认业务表已有新数据。
- 请求核查：`requestId=4f332b99-9c02-40fe-aa7c-bb0b720f1f2f` 的流程动作日志为 `FLOW_ACTION/SUBMIT`，委托用户 45、当前组织 21，检查点已保存 `recordId=19`；业务表记录 19 为 `tenant_id=1/create_by=45/create_dept=21/document_status=DRAFT`。
- 权限排除：离职申请当前模型策略为租户范围 `TENANT`，记录租户与调用租户一致；本次不是角色或组织数据权限拒绝。
- 根因：数据库会话与全局事务隔离均为 `REPEATABLE-READ`。外层流程动作事务在内层 `REQUIRES_NEW` 建单前读取了能力来源元数据，形成旧一致性快照；记录 19 在独立事务提交后，外层流程启动查询仍看不到它，因而在 `BusinessFlowService.startDocumentFlowInternal` 被误报为不可见。
- 事务修复：仅对 SUBMIT 外层动作事务显式使用 `READ_COMMITTED`，保证后续读取能看到已提交检查点记录；START/APPROVE/REJECT 的现有事务策略保持不变，建单与 recordId 检查点仍在同一 `REQUIRES_NEW` 事务提交。
- 身份修复：数据权限上下文优先读取可信 `ExecutionIdentity`，外围 USER 委托不再因缺少 Sa-Token 登录态而失去 FOLLOW_SYSTEM 数据权限上下文。
- 可观测性：流程启动查询为空时输出安全元数据诊断，包括 tenantId/objectCode/configKey/recordId/starterUserId/activeOrgId，不记录 Token、密钥或业务 Body。
- 静态检查：执行 `git diff --check`，目标 Java 与变更文档未发现空白错误或冲突标记。
- 验证状态：按用户明确要求未执行 Maven、pnpm、服务、Flyway、接口或浏览器测试，运行结果由用户环境验证。
- 服务清理：本轮未启动、停止或重启任何服务。

## 27. 客户端接入工作台与契约易用性优化

- 日期：2026-08-03。
- 能力生命周期：能力目录增加受控编辑和逻辑删除；编辑仅覆盖名称、描述、可见性，已发布契约保持不可变；删除要求先停用且不存在有效授权，历史版本和审计日志继续保留。
- 调用指南：页面重排为选择客户端、调用前检查、请求/返回契约与示例、在线测试四步；示例收敛为 Curl/Java 17，并通过认证方式选择器切换 OAuth/HMAC。请求与返回字段展示中文名称、JSON Key、类型、必填、约束和示例，业务校验和返回说明同步进入页面及 Markdown 接入示例。
- 凭据体验：客户端创建、Secret/Signing Key 轮换和 RSA 私钥生成后的明文只保存到当前 SPA 模块内存并按 clientId 隔离；在线测试自动带入所需凭据，刷新或吊销后清空，无法反查时引导到指定客户端工作台。
- 客户端工作台：一个入口聚合概览与凭据、授权新增/调整/撤销、外围用户映射和该客户端调用日志；旧授权与日志路由保留兼容，Flyway 将其从主菜单隐藏。
- 外围用户映射：列表改为服务端分页和关键字搜索；新增管理员预绑定/已验签手机号唯一匹配字典和显式安全确认。自动规则仅在客户端 RSA 验签、短期 JWT 和防重放通过后执行租户内普通用户唯一匹配，失败关闭并固化成功映射。
- 调用日志：审计表增加失败阶段和最长 1000 字符脱敏错误摘要；列表和详情展示实际调用用户、服务账号、组织、结果码、Schema 路径、Trace ID 和耗时，不保存请求 Body、Token、密钥、签名或手机号。
- 数据迁移：新增 `V1.0.82__improve_capability_client_workbench.sql`，维护客户端映射模式、调用日志诊断字段、映射模式字典和菜单聚合；内置数据使用 `tenant_id=1` 并具备防重复保护。
- 静态检查：执行 `git diff --check` 未发现空白错误；目标能力模块、前端页面/API 和变更文档未发现行首 Git 冲突标记。
- 验证状态：按用户明确要求，本轮未执行 Maven、pnpm、服务启动、Flyway、接口调用或浏览器测试；编译、迁移和页面运行结果由用户环境验证。
- 服务清理：本轮未启动、停止或重启任何服务。

## 28. 调用日志检索与调用测试页签化

- 日期：2026-08-04。
- 日志查询：调用日志分页 SQL 关联能力目录和系统用户，新增请求 ID 模糊匹配、能力名称/编码匹配、调用用户 ID/用户名/姓名匹配；原客户端 ID、能力编码和结果码参数继续兼容。
- 日志展示：客户端工作台增加三项组合筛选和查询/重置操作；列表直接显示能力名称/编码、真实用户姓名/用户名/ID、耗时、中文失败阶段和调用时间，请求 ID 固定在左侧，调用时间与详情操作固定在右侧。
- 历史兼容：成功和等待审批记录不显示失败阶段；旧失败日志没有阶段数据时显示“未记录”，详情继续展示错误码、Schema 路径、Trace ID 和脱敏错误摘要。
- 独立页面：保留原调用日志兼容路由，同步新增请求 ID、能力和调用用户筛选，并将高频排查字段前移；低频诊断字段继续放在详情弹窗中。
- 调用测试：能力目录的“调用与测试”改为接入概览、接口契约、示例代码、在线测试四个独立页签；客户端选择、就绪状态和版本升级告警保持在页签上方，切换客户端后默认回到接入概览。
- 页面结构：在线测试页签移除重复标题和重复纵向间距，保留凭据引导、请求填写、真实调用和报文下载完整能力。
- 静态检查：执行 `git diff --check`，目标 Java、XML、Vue 和变更文档未发现空白错误；目标文件未发现行首 Git 冲突标记。
- 验证状态：按用户明确要求，本轮未执行 Maven、pnpm、服务启动、Flyway、接口调用或浏览器测试；编译和页面运行结果由用户环境验证。
- 服务清理：本轮未启动、停止或重启任何服务。
