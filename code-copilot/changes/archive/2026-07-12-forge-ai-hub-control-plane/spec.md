# Forge AI 中枢控制面

> status: done
> created: 2026-07-11
> complexity: 🔴复杂
> parent-stage: Forge AI 中枢阶段 1 — 能力内核 + 只读 MCP MVP

## 1. 背景与目标

`forge-ai-hub-foundation` 已归档，阶段 0 已提供协议无关 Capability 内核、Streamable HTTP `/mcp`、可信身份接口、Schema 校验、HMAC 游标和静态 `capability.ping`。当前 Registry 仍是进程内静态快照，没有能力目录、版本、机器客户端、授权和调用日志的数据库权威来源。

阶段 1 包含控制面、机器认证、动态目录、低代码只读执行器和管理端，不能在一个变更中同时完成。本变更交付阶段 1 的第一个独立纵向切片：持久化控制面和安全领域服务，为后续 `capability.search/describe/invoke` 与低代码只读能力提供稳定事实来源。

### 1.1 成功标准

- 新增五张表：`ai_capability`、`ai_capability_version`、`ai_capability_client`、`ai_capability_grant`、`ai_capability_invocation_log`；
- 配置数据和日志均显式逻辑删除，Mapper XML 查询显式过滤 `del_flag=0`；
- 能力版本不可变，同一租户能力编码和同一能力版本只允许一个有效记录；
- 客户端密钥使用 128 bit 全局 `keyId` + 256 bit Secret，只在创建/轮换时返回一次；数据库只保存 keyId、前缀与 `HMAC-SHA-256(secret, serverPepper)`；
- Pepper 只从环境配置读取，未配置时创建/轮换密钥失败关闭，不能使用代码默认值；
- 授权结果是客户端状态、有效期、租户、组织、grant 状态、grant 有效期和版本策略的交集；
- 日志只保存 requestId、客户端/能力引用、租户/组织、版本、结果码、错误码、Schema 路径、traceId 和耗时，不保存 arguments、data、Header、Token、Secret；
- 管理 API 使用 Forge POST-safe CRUD 契约、权限注解和统一响应；
- MCP 仍只发布 `capability.ping`，Admin 组合根通过控制面机器凭据建立可信调用上下文，不把未完成动态目录的持久化能力注册为顶层 Tool；
- Flyway 静态检查、插件测试、Admin 聚合构建和敏感信息扫描通过。

## 2. 范围

### 2.1 本变更包含

- 五张控制面表、逻辑删除唯一键、索引和字典；
- Capability/Version/Client/Grant/InvocationLog 实体、Mapper、XML 和领域服务；
- 能力发布、版本快照、下线和按租户分页查询；
- 客户端创建、密钥轮换、吊销和安全校验；
- 授权创建、撤销和授权决策；
- 安全调用日志写入和分页查询；
- 管理 Controller 与 API 权限资源；
- 领域服务和安全边界单元测试。

### 2.2 明确不做

- 不实现低代码 `CapabilitySource`、Schema Builder 或 DynamicCrudReadExecutor；
- 不实现 `capability.search/describe/invoke` MCP 工具；
- 不动态发布业务 Tool，不修改 SDK 私有工具集合；
- 不实现 API Key 换短期 Sa-Token、OAuth2 Client Credentials 或公网接入；
- 不实现具体人员委托令牌；客户端直传的 `userId`、`tenantId`、`activeOrgId` Header 均不可信；
- 不新增前端管理页面；前端页面随下一阶段动态目录一起交付，避免先出现不可使用的配置入口；
- 不接 Nacos MCP Registry/Admin、AgentScope、Agent Runtime、RAG；
- 不实现写能力、流程、消息、任意 SQL/HTTP/文件/代码执行；
- 不使用旧 SSE、STATELESS 或 stdio transport。

### 2.3 模块边界

本变更将持久化控制面放入独立模块 `forge-plugin-capability-control-plane`，避免核心协议和 MCP 适配层传递依赖 ORM、认证、Redis 与管理 Web：

```text
forge-admin-server ─┬─> forge-plugin-capability-control-plane ─> forge-plugin-capability
                    └─> forge-plugin-mcp ──────────────────────> forge-plugin-capability
```

- `forge-plugin-capability` 只保留协议无关模型、Registry、Schema、授权 SPI 和执行 SPI；
- `forge-plugin-mcp` 不依赖控制面，不扫描或静态发布持久化业务能力；
- `forge-plugin-capability-control-plane` 承载实体、Mapper XML、管理 API、机器凭据、grant 和审计；
- Admin 作为组合根同时聚合 MCP 与控制面，并以 `CapabilityMcpCallerContextResolver` 完成 Bearer 机器凭据到 MCP 调用上下文的接线；协议模块之间不形成反向依赖。

## 3. 数据模型

### 3.1 `ai_capability`

保存租户内稳定能力标识与当前发布指针。关键字段：

```text
capability_code / protocol_tool_name / capability_name / description
source_type / source_key / source_version
current_version / schema_checksum
behavior / risk_level / visibility / publish_status / enabled
del_flag / logic_delete_active / 标准审计字段
```

唯一约束：`(tenant_id, capability_code, logic_delete_active)` 与 `(tenant_id, protocol_tool_name, logic_delete_active)`。

### 3.2 `ai_capability_version`

保存不可变版本快照：`capability_id/version/input_schema/output_schema/source_type/source_key/source_version/behavior/risk_level/visibility/policy_snapshot/schema_checksum/status`。发布后禁止更新执行语义或 Schema；修正必须创建新版本。

### 3.3 `ai_capability_client`

保存机器客户端：`client_code/client_name/key_id/key_prefix/key_hash/credential_version/service_user_id/active_org_id/status/expires_at/last_used_at`。`key_id` 全局唯一并用于认证前定位权威租户，`key_hash` 固定 64 位小写十六进制，不保存原始密钥。

### 3.4 `ai_capability_grant`

保存 `client_id -> capability_id` 显式授权，支持 `PINNED` 固定版本和 `FOLLOW_MAJOR` 跟随同主版本。阶段 1 第一切片只允许读能力授权，ACTION/FLOW/MESSAGE/EXTERNAL 拒绝创建 grant。

### 3.5 `ai_capability_invocation_log`

保存调用安全元数据，包括 `actor_type/actor_user_id/service_user_id` 双身份审计。`request_id` 在租户内唯一；普通删除逻辑删除，后续留存任务才允许物理清理超期日志。

## 4. 核心契约

```java
public interface CapabilityCatalogService {
    Page<CapabilityCatalogVO> page(PageQuery pageQuery, CapabilityCatalogQuery query);
    CapabilityCatalogVO getById(Long id);
    Long publish(CapabilityPublishCommand command);
    void disable(Long id);
}

public interface CapabilityClientCredentialService {
    CapabilityClientSecret create(CapabilityClientCreateCommand command);
    CapabilityClientSecret rotate(Long clientId);
    CapabilityClientPrincipal authenticate(String rawSecret);
    void revoke(Long clientId);
}

public interface CapabilityGrantService {
    Long grant(CapabilityGrantCommand command);
    void revoke(Long grantId);
    CapabilityGrantDecision evaluate(Long clientId, String capabilityCode, String requestedVersion);
}

public interface CapabilityInvocationAuditService {
    void record(CapabilityInvocationAuditEvent event);
    Page<CapabilityInvocationLogVO> page(PageQuery pageQuery, CapabilityInvocationLogQuery query);
}
```

## 5. 安全规则

1. `forge.capability.client-pepper` 无默认值；仅创建、轮换和验证密钥时要求非空；
2. 原始 Secret 格式为 `fcp_<22位Base64URL keyId>_<43位Base64URL随机串>`；只允许用不可猜测 keyId 跨租户定位凭据，日志和异常禁止出现完整值；
3. HMAC 比较使用 `MessageDigest.isEqual`；客户端不存在时也计算一次哑 HMAC，降低明显时序差异；
4. 机器模式的 `actorUserId` 固定等于客户端绑定的 `serviceUserId`；后续用户委托模式必须由 Forge 登录/OAuth 签发短期令牌证明实际人员，禁止信任调用方自报用户 Header；
5. 租户 ID 只能来自当前管理会话或已认证客户端记录，DTO 中不接受 tenantId；
6. API 返回客户端记录时仅返回 `keyPrefix`，Secret 只出现在创建/轮换响应；
7. 能力 Schema 入库前复用 `CapabilitySchemaValidator`，checksum 使用规范化 JSON 的 SHA-256；
8. 不记录原始 Schema 之外的业务参数摘要，不记录 Throwable 文本。
9. authenticate/rotate/revoke 使用 `credential_version` CAS 原子更新；认证只更新 `last_used_at`，旧认证快照不得恢复已轮换或吊销状态。

## 6. API

| 模块 | 路径 | 方法 | 权限 |
|---|---|---|---|
| 能力目录 | `/ai/capability/page` | GET | `ai:capability:query` |
| 能力目录 | `/ai/capability/getById` | POST | `ai:capability:query` |
| 能力发布 | `/ai/capability/publish` | POST | `ai:capability:publish` |
| 能力停用 | `/ai/capability/disable/{id}` | POST | `ai:capability:publish` |
| 客户端 | `/ai/capability/client/page` | GET | `ai:capability:client:query` |
| 客户端 | `/ai/capability/client/add` | POST | `ai:capability:client:add` |
| 客户端 | `/ai/capability/client/rotate/{id}` | POST | `ai:capability:client:rotate` |
| 客户端 | `/ai/capability/client/revoke/{id}` | POST | `ai:capability:client:revoke` |
| 授权 | `/ai/capability/grant/page` | GET | `ai:capability:grant:query` |
| 授权 | `/ai/capability/grant/add` | POST | `ai:capability:grant:add` |
| 授权 | `/ai/capability/grant/revoke/{id}` | POST | `ai:capability:grant:revoke` |
| 调用日志 | `/ai/capability/invocation/page` | GET | `ai:capability:invocation:query` |

写接口使用 `@ApiDecrypt`；包含一次性 Secret 的创建/轮换响应使用 `@ApiEncrypt`。

## 7. MCP 与版本决策

本地 SDK `0.17.0` 与 Maven Central `2.0.0` 的公开 API/字节码检查均显示 `tools/list` 仍读取进程级静态 Tool 集合，没有请求级目录 provider。升级 SDK 不能自动解决动态目录，且会破坏 Spring AI `1.1.2` 的受控依赖组合。

因此本变更不升级 SDK、不 fork SDK、不反射私有字段。下一变更采用所有客户端统一可见的元工具 `capability.search/describe/invoke`，真实能力目录由控制面按 caller/grant 动态返回；顶层业务 Tool 投影继续关闭。该设计保持 Streamable HTTP 和调用级授权，不泄露未授权能力。

## 8. 数据迁移与回滚

- 新增 `V1.0.21__add_ai_capability_control_plane.sql`；
- 脚本使用 `CREATE TABLE IF NOT EXISTS`，字典和资源使用 `NOT EXISTS`；
- 不插入客户端、密钥、grant 或真实能力数据；
- 回滚优先禁用相关 API 和后续元工具；物理删表只允许人工执行，不写自动 down migration；
- 本变更不修改已有供应商、模型路由、Capability ping 或 MCP transport 配置。

## 9. HARD-GATE

- Flyway 表结构、实体 `@TableLogic` 与 Mapper XML 条件一致；
- Secret 永不落库/日志，缺 Pepper 失败关闭；
- 跨租户、跨组织、过期/吊销客户端和过期/撤销 grant 全部拒绝；
- Schema/版本快照不可变；
- MCP 测试继续证明只发布 ping 且只使用 Streamable HTTP；
- 任一硬闸门失败时不进入下一变更的元工具或低代码执行器。

## 10. 确认记录

- **确认时间**：2026-07-11；
- **确认方式**：用户要求归档阶段 0 后创建下一阶段 Proposal 并由 Agent 自动执行；
- **批准范围**：本控制面切片，不代表批准业务写工具、Nacos、Agent Runtime、旧 SSE 或公网开放。

## 11. Review 结论

- **Spec Compliance**：五表控制面、不可变版本、一次性机器密钥、授权交集、安全审计、权限 API 与模块隔离均已落地；没有加入前端、低代码执行器、元工具、Nacos 或高风险能力；
- **Code Quality**：在原 Review 修复基础上，继续修复凭据并发吊销覆盖、认证前租户定位、不可变版本指纹缺字段、审计原始错误内容风险、MCP 生产认证未接线及认证前 `last_used_at` 被租户插件追加 NULL 条件；
- **验证状态**：Capability 13、Control Plane 20、MCP 13、AI 84、Admin 机器身份适配 2 项全绿，Admin 38/38 聚合成功；XML、Flyway 与安全静态扫描通过；
- **保留边界**：未连接真实 MySQL 执行 Flyway，未启动 Admin/Redis/Nacos，未运行前端构建；这些项目按本变更无前端、无受控测试数据库的实际条件明确跳过；
- **结论**：当前变更达到 `review-ready`，不自动归档；机器模式可归因到绑定服务账号。具体人员 A 的可信委托仍需独立变更，不能用可伪造 Header 代替；之后再建设统一静态元工具和调用级动态授权目录。

## 12. 归档记录

- **归档时间**：2026-07-12；
- **归档路径**：`code-copilot/changes/archive/2026-07-12-forge-ai-hub-control-plane/`；
- **归档结论**：Proposal、Apply、Review、Fix 与增量验证全部完成，状态更新为 `done`；
- **下一阶段闸门**：阶段 2 先实现 `mcp-user-delegation-identity`，在可信用户委托身份完成前禁止开放人员责任写入、流程发起和流程办理能力。
