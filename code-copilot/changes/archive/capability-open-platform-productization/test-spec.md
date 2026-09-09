# 测试规格 — 统一能力开放平台产品化改造

> 依据：`code-copilot/rules/automated-testing-standard.md`
> 基线：复用 `unified-capability-open-platform` 已通过的认证、授权、限流、幂等和审计测试，仅验证本轮差异与必要回归。

## 1. P0 自动化验证

| 编号 | 验证项 | 证据 |
|---|---|---|
| P0-1 | 通用执行适配器正确分派，未知/重复来源失败关闭 | ✅ `OpenGatewayCapabilityResolverTest` 3/3、`CapabilityInvokeOrchestratorTest` 10/10 |
| P0-2 | BUSINESS_ACTION/FLOW_ACTION 现有调用兼容 | ✅ `BusinessActionOpenGatewayAdapterTest` 3/3、`FlowActionExecutionAdapterTest` 6/6 |
| P0-3 | REST/MCP audience 严格分离 | ✅ `CapabilityAccessTokenServiceTest` 7/7、`OpenGatewayAuthenticatorTest` 4/4；MCP 委托身份集成测试 3/3 |
| P0-4 | Markdown 完整参数表、规则、示例、错误和敏感信息排除 | ✅ `CapabilityOpenApiDocumentServiceTest` 3/3，覆盖 Spring 容器构造器注入、递归 Schema、业务规则、权限、错误码、Token Exchange 和敏感占位符 |
| P0-5 | OpenAPI Body/Header 幂等契约一致 | ✅ `CapabilityOpenApiDocumentServiceTest` 验证 `Idempotency-Key` 仅位于 Header，错误码契约完整 |
| P0-6 | 调用指南 readiness 和示例分支 | ✅ `CapabilityCallGuideServiceTest` 5/5，覆盖客户端/授权/网关开关阻断、网关强制 Identity 运行态和 OAuth/HMAC 示例 |
| P0-7 | 系统服务仅执行代码注册定义 | ✅ `SystemServiceDefinitionRegistryTest` 4/4、`SystemServiceCapabilityPublisherTest` 3/3、`SystemServiceOpenGatewayAdapterTest` 3/3 |
| P0-8 | 流程启动固定模型、拒绝身份/模型注入、快照漂移失败关闭 | ✅ `FlowProcessStartSystemServiceTest` 5/5 |
| P0-9 | Blob 显式保留，Markdown/OpenAPI 下载可用 | ✅ capability 相关 4 个前端文件定向 ESLint 零错误；生产构建成功并包含调用指南/系统服务注册组件；真实浏览器下载保留为 P1 |
| P0-10 | Admin 聚合编译与前端生产构建 | ✅ Admin `47/47` Reactor 模块编译成功；前端 `pnpm build` 成功 |

## 2. P1 环境验证

- 配置真实 MySQL/Redis/OIDC 后完成 OAuth Token → 调用指南 curl → 流程实例启动。
- 使用 MCP audience Token 调 REST，预期 401；使用 REST audience Token 调 MCP，预期 401。
- 浏览器完成“注册流程启动能力 → 创建客户端 → 授权 → 查看就绪状态 → 下载 Markdown → curl 调用”。

P1 环境不可用时记录跳过原因，不伪造通过结论。

本轮未启动真实 Admin/Flow 服务，也未连接 MySQL、Redis 或 OIDC，所以上述 P1 项保持待验证；详细原因和复跑入口见 `execution-log.md`。

## 3. 安全回归

- 不存在任意 URL/Method/Bean 配置入口。
- 请求 Schema 不包含 `modelKey/modelId/tenantId/userId/activeOrgId/initiator`。
- 指南和文档不含任何真实 Secret、Token 或用户敏感信息。
- 未注册来源、流程快照失效、权限不足、身份不兼容均在副作用前拒绝。

## 4. 2026-08-02 启动装配缺陷增量验证

- [x] Identity 关闭但 Open Gateway 开启时，Identity 自动配置仍提供 `CapabilityAccessTokenService`。
- [x] Open Gateway 与 Identity 联合上下文可创建认证器和调用编排器，不再出现缺 Bean。
- [x] 调用指南采用与运行态一致的有效 Identity 状态，OAuth 检查和示例不产生假阻断。
- [x] Identity、Open Gateway、Control Plane 相关模块全量测试通过。
- [x] Admin 聚合编译通过，`git diff --check` 无空白错误。

## 5. 2026-08-02 授权参数与中文字典增量验证（待用户执行）

本轮用户明确要求由其自行测试，因此以下项目只记录验证范围，不标记为通过：

- [ ] 后端 `forge.crypto.enabled=false` 时新增授权，请求体应直接绑定 `clientId/capabilityId/versionStrategy/fixedVersion`，授权成功。
- [ ] 后端加密开启时新增授权，应先完成密钥协商并保持加密传输，授权成功。
- [ ] 服务端安全配置无法读取时，前端应提示“无法确认服务端安全配置”并阻止敏感请求。
- [ ] 能力目录的来源类型、风险等级、调用主体、可见性、发布状态均显示中文；当前版本继续显示语义版本号。
- [ ] 能力详情的来源类型、行为类型及其它策略属性均显示中文。
- [ ] 流程能力新增授权时，“允许操作”显示“发起流程/审批通过/审批驳回”，字典未就绪时禁止提交。
- [ ] 临时制造某一个字典请求失败后，其它已成功字典仍正常显示；重新进入页面或点击刷新后可恢复，不需要重启整个 SPA。
- [ ] Flyway 执行 `V1.0.78__add_capability_catalog_dicts.sql` 后，两类字典及六条字典数据存在且无重复。

## 6. 2026-08-02 OAuth 路由、能力恢复与在线测试（待用户执行）

本轮继续遵循用户“自行测试”的要求，以下项目不标记为已通过：

- [ ] 配置 `forge.capability.open-gateway.enabled=true`、`forge.capability.identity.enabled=false` 并重启 Admin，`POST /oauth2/token` 不再返回 404，OAuth Client Credentials 能取得 OpenAPI audience Token。
- [ ] 能力停用后目录显示“启用”；当前版本为 `PUBLISHED` 时可重新启用，能力状态恢复为 `PUBLISHED/enabled=1`。
- [ ] 人为使当前版本缺失或非发布状态后执行启用，接口应返回明确中文错误且能力保持停用。
- [ ] SERVICE + OAuth 在线测试可完成 Token 与真实网关调用，页面展示两段完整脱敏报文。
- [ ] USER 委托能力未填 `subject_token` 时前端阻止调用；填写受信 OIDC JWT 后走 Token Exchange，不伪造用户身份。
- [ ] SERVICE + HMAC 在线测试签名通过；修改 Body 后重新测试仍按本次原始 Body 正确计算 SHA-256。
- [ ] `ACTION/FLOW` 能力点击测试先出现副作用确认，确认后的调用报文包含唯一 `Idempotency-Key`。
- [ ] 下载测试报文不包含真实 Client Secret、Signing Key、Bearer Token、OIDC Token 或 HMAC Signature。
- [ ] 下载接入示例包含地址、Resource、Scope、请求 Body、curl 和 Java 17 示例；Java 示例通过环境变量读取密钥。
- [ ] 关闭调用指南或切换客户端后，之前输入的 Secret、Signing Key 和 subject token 被清空。

## 7. 2026-08-02 无 OIDC 客户端用户断言（待用户执行）

本轮用户明确要求由其自行测试，因此以下项目只记录验证范围，不标记为通过：

- [ ] 执行 Flyway `V1.0.79__add_capability_client_user_assertion.sql` 后，客户端断言四个字段和映射 `subject_hint` 存在；重复检查无重复字段。
- [ ] USER_DELEGATION/HYBRID + OAuth 客户端可生成 RSA-2048 密钥；返回 PKCS#8 私钥只出现一次，数据库仅保存 X.509 公钥、`kid` 和版本。
- [ ] SERVICE、非 OAuth、过期、吊销客户端均不能启用用户断言。
- [ ] 绑定外围 `sub` 后数据库只存在 SHA-256、脱敏提示和 Forge userId，不存在原始 `sub`；重复绑定同一用户幂等，绑定不同用户拒绝。
- [ ] 系统管理员、租户管理员、禁用用户、强制改密用户、无组织或无有效角色用户均不能绑定。
- [ ] 使用专用 `urn:forge:params:oauth:token-type:user-assertion+jwt` 可换取 USER Token；未预绑定 `sub` 返回 `invalid_grant`。
- [ ] 篡改签名、错误 `kid/iss/aud/client_id`、缺失 `iat/exp/jti`、未来签发、过期、TTL 超限、非法组织 ID 均返回 `invalid_grant`。
- [ ] 同一 `jti` 第二次使用被拒绝；Redis/Redisson 不可用时返回 `temporarily_unavailable`，不得降级放行。
- [ ] 客户端断言验签失败后不会尝试 OIDC；OIDC JWT 也不会尝试客户端公钥验签。
- [ ] 密钥轮换、用户断言停用和客户端吊销会递增 `credential_version`，旧私钥及旧 Forge Token 失效；用户映射保留。
- [ ] 每次 Token Exchange 都重新加载 Forge 用户组织、角色和权限；用户状态、组织或角色失效后不能再换取 Token。
- [ ] 客户端页面可一次下载私钥、管理脱敏映射；在线测试可选 OIDC/客户端断言，并能在浏览器内临时生成 RS256 JWT。
- [ ] 下载测试报文和 Markdown 接入示例不包含 Client Secret、私钥、可用 JWT 或 Bearer Token；Java 17 示例从私钥文件和环境变量读取凭据。
