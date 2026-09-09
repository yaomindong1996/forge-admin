# 客户端签名用户身份断言 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为没有统一 OIDC 的外围系统提供可审计、可轮换、防重放的真实用户委托认证，并保持 Forge 用户、组织与权限实时校验。

**Architecture:** 每个 USER_DELEGATION/HYBRID 客户端生成独立 RSA-2048 密钥对，私钥仅一次返回，数据库只保存公钥。管理员预绑定外部 `sub` 到 Forge 用户；外围系统签发最长两分钟的 RS256 JWT，通过 OAuth Token Exchange 换取 Forge 短期 Token。Forge校验客户端密钥、JWT 签名、`iss/aud/client_id/iat/exp/jti`、Redis 防重放和映射后，再从用户目录加载实时组织与权限。

**Tech Stack:** Java 17、Spring Boot 3、Nimbus JOSE JWT、MyBatis XML、Redisson 防重放、Vue 3、Naive UI、Flyway MySQL。

---

### Task 1: 数据契约与迁移

**Files:**
- Create: `forge-server/db/migration/V1.0.79__add_capability_client_user_assertion.sql`
- Modify: `AiCapabilityClient.java`、`AiCapabilityClientMapper.java/xml`
- Modify: `AiCapabilityExternalIdentity.java`、`AiCapabilityExternalIdentityMapper.java/xml`

- [x] 为客户端增加断言开关、`kid`、RSA 公钥和版本；为身份映射增加脱敏 `subject_hint`。
- [x] Mapper 列表不返回完整公钥，只有凭据查询读取公钥；密钥轮换和停用使用乐观版本条件并递增 `credential_version`。
- [x] 映射列表使用 XML 联查 Forge 用户，所有查询显式包含 `tenant_id`、`status` 和 `del_flag=0`。

核心更新协议：

```java
int rotateUserAssertionKey(Long tenantId, Long id, Integer credentialVersion,
        String keyId, String publicKeyPem, Integer expectedKeyVersion);
int disableUserAssertion(Long tenantId, Long id, Integer credentialVersion);
```

### Task 2: 断言验签与身份映射

**Files:**
- Create: `ClientUserAssertionVerifier.java`
- Create: `ClientUserAssertionAdminService.java`
- Create: 管理 DTO/VO 与 `ClientUserAssertionController.java`
- Modify: `CapabilityIdentityAutoConfiguration.java`、`CapabilityIdentityProperties.java`

- [x] 生成 RSA-2048 密钥，私钥以 PKCS#8 PEM 一次返回，公钥以 X.509 PEM 保存。
- [x] 只接受 `RS256` 且 `kid` 精确匹配的 JWT；固定校验：

```text
iss       = clientCode
aud       = forge.capability.identity.issuer
client_id = 客户端数字 ID
sub       = 已预绑定的外围用户稳定标识
exp-iat   <= 120 秒
jti       = 8~64 位一次性随机数
```

- [x] 使用 `OpenApiReplayGuard.assertNotReplayed("user-assertion:" + clientId, iat, jti)`；Redis 不可用时失败关闭。
- [x] 管理员绑定 `sub → Forge userId`，数据库只保存 SHA-256 和脱敏提示；禁止绑定管理员、禁用用户或无有效角色用户。

### Task 3: OAuth Token Exchange 扩展

**Files:**
- Modify: `CapabilityTokenController.java`
- Modify: `ExternalIdentityMappingService.java`

- [x] 新增 token type：

```text
urn:forge:params:oauth:token-type:user-assertion+jwt
```

- [x] OIDC JWT 继续走原验证器；客户端用户断言走客户端公钥验签与预绑定映射，二者不得模糊回退。
- [x] Token 仍由 Forge 签发，租户、组织、角色和权限只从映射及 Forge 用户目录获取。

### Task 4: 管理端易用配置

**Files:**
- Modify: `forge-admin-ui/src/api/ai/capability.js`
- Modify: `forge-admin-ui/src/views/ai/capability/client.vue`

- [x] USER_DELEGATION/HYBRID 客户端显示“用户断言”操作。
- [x] 弹窗展示固定 Issuer、Audience、Token Type、`kid`、密钥版本和两分钟有效期。
- [x] “生成/轮换密钥”后只展示一次私钥；支持复制和下载 PEM。
- [x] 支持输入外围用户标识并选择 Forge 普通用户完成映射；列表显示脱敏标识、用户、最近认证时间并支持解除。

### Task 5: 调用指南、在线测试和接入样例

**Files:**
- Modify: `CapabilityCallGuideVO.java`、`CapabilityCallGuideService.java`
- Modify: `CapabilityCallGuideModal.vue`、`CapabilityOnlineTestPanel.vue`

- [x] 客户端启用用户断言后，调用指南默认生成客户端断言 Token Exchange curl。
- [x] 在线测试明确区分“受信 OIDC JWT”和“客户端签名用户断言 JWT”，并发送对应 `subject_token_type`。
- [x] 接入包增加 Java 17 RS256 断言生成示例，私钥从文件读取，不写入源码。

### Task 6: 增量验证清单与交付记录

**Files:**
- Modify: `spec.md`、`tasks.md`、`test-spec.md`、`execution-log.md`

- [x] 记录签名篡改、错误 `kid/iss/aud/client_id`、超时、超长 TTL、`jti` 重放、未映射用户、管理员映射、密钥轮换与客户端吊销验证项。
- [x] 按用户要求不执行 Maven、前端构建、Flyway、接口或浏览器测试，所有验证项标记为待用户执行。
