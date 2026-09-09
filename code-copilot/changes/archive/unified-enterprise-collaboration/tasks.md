# 任务拆分 — 统一企业协同集成与企业微信全能力接入
> 拆分顺序：客户输入与纵向验证 → 通用 SPI → 数据/安全 → 登录 → 通讯录 → 消息 → 待办/回调 → 管理端 → 上线门禁
> 每个任务完成后同步回填 `spec.md`、`test-spec.md` 和 `execution-log.md`

## 前置条件

- [ ] Gate A：用户确认 Spec 范围、架构、优先级、安全默认值和 M1/M2 分期；只允许执行 Task 0，不修改生产代码。
- [ ] Gate B：Task 0 的 M1 纵向验证通过，客户提供登录/通讯录/消息应用资料及一期回调条件，并冻结人员权威、唯一键、离职、根部门、岗位、一期 SLA 和留存规则；完成后才进入 `/apply` 实施 P0/P1。
- [ ] Gate C：开始 Task 14 前，客户提供待办应用权限、对应回调凭据、测试用户、待办 SLA 和交互边界；缺少这些资料只阻塞 P2，不阻塞 M1。
- [ ] 所有凭据只进入部署环境或安全 Secret 渠道，不写入 Git、文档、测试数据、聊天记录或日志。
- [ ] 确认部署环境已配置 Forge 持久化加密 key ring；认证加密门禁未通过时禁止迁移 Secret 或开放连接。
- [ ] 实施和验证前按 `code-copilot/rules/automated-testing-standard.md` 读取本变更四份文档及 memory 基线。

## 交付顺序与优先级

| 顺序 | 优先级 | 任务 | 完成标志 | 依赖 |
|------|--------|------|----------|------|
| 1 | P0 | Task 0：企微纵向连通验证 | Token、部门、成员、标签、测试消息、回调解密全部通过 | 客户资料 |
| 2 | P0 | Task 1-4C：SPI、模块、数据、配置、凭据安全和兼容迁移 | 通用插件装配、迁移、连接/应用/身份存储和 Secret 生命周期合同通过 | Task 0 |
| 3 | P0 | Task 5：安全登录协议 | 前端无法伪造外部身份，企微映射登录通过 | Task 2-4C |
| 4 | P1 | Task 6-11：企微通讯录与同步运维 | 全量/增量/标签/冲突/定时校准闭环 | Task 1-5 |
| 5 | P1 | Task 12-13：企微消息 | 逐人投递、部分失败、补偿、记录闭环 | Task 6-11 |
| 6 | P2 | Task 14-15：企微待办与安全回调 | 创建/转派/完成/撤回联动和安全动作闭环 | Task 5、Task 7、Task 12-13 |
| 7 | P1/P2 | Task 18 后端接口/资源 → Task 16-17 管理端与运维页面 | 管理员可配置、观察、处理、重试 | 对应核心服务任务 |
| 8 | P0/P1 | Task 19A：M1 自动化、真实企微 UAT 与上线门禁 | 目录、登录、消息和一期运维可独立上线 | Task 0-13、Task 16-18 一期范围 |
| 9 | P2 | Task 19B：M2 待办 UAT 与上线门禁 | 待办、回调和补偿可独立上线，变更整体完成 | Task 14-15、Task 17-18 二期范围 |

## 执行状态

- [x] Research：完成现状、依赖、数据库和安全风险调查。
- [x] Proposal：创建 Spec、Tasks、Test Spec 和执行日志。
- [ ] Gate A：用户确认 Proposal；仅进入 Task 0。
- [ ] Gate B：M1 输入和纵向验证完成；授权 P0/P1 `/apply`。
- [x] Apply/P0：通用底座、凭据安全和安全登录（Task 1-5 编码完成，`mvn -pl forge-admin-server -am compile` BUILD SUCCESS）。
- [x] Apply/P1：企微通讯录、消息和运维闭环（Task 6-13、Task 18 一期、Task 16/17 一期前端编码完成；前端 ESLint 通过）。
- [ ] Review/Test/UAT M1：一期独立审查、真实企微验收和上线门禁（待 Task 19A：自动化测试、真实企微 UAT 需客户提供测试企业资料）。
- [ ] Gate C：待办资料和交互边界完成；授权 P2 `/apply`。
- [ ] Apply/P2：企微待办、回调、安全跳转和补偿。
- [ ] Review/Test/UAT M2：二期独立审查、真实企微验收和部署门禁。

## Task 0：企业微信接入条件与纵向验证（P0）

- **目标**：在大量编码前证明客户授权和企微能力真实可用，冻结供应商错误码、权限和限流基线。
- **涉及文件**：
  - `code-copilot/changes/unified-enterprise-collaboration/wecom-spike-report.md` — 新增，记录非敏感配置清单、调用结果、错误码、速率和结论。
  - `forge-docs/guide/integration/wecom-setup.md` — 新增，记录自建应用、权限、可信域名/IP、回调和测试步骤。
  - `code-copilot/changes/unified-enterprise-collaboration/execution-log.md` — 追加实际命令、时间、结果和跳过项，不记录凭据或个人资料。
- **验证动作**：
  1. 获取应用 Token。
  2. 拉取一个测试部门、成员和标签详情。
  3. 给测试成员发送一条应用消息/模板卡片。
  4. 完成回调 URL 校验、加密事件接收和解密。
  5. 可选预检待办卡片更新/回调权限；不具备时记录为 M2 外部阻塞，不用模拟结果替代，也不判定 M1 Task 0 失败。
- **验收**：Token、测试部门/成员/标签、测试消息和一期回调条件均有脱敏证据；待办预检结果单独记录，任何权限缺口都有客户责任人和处理结论。

## Task 1：建立企业协同 Starter SPI 与 Provider 合同（P0）

- **目标**：定义平台无关能力边界，使编排层只依赖 Capability/Connector，不出现平台分支。
- **涉及文件**：
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-collaboration/pom.xml` — 新增 Starter 模块。
  - `forge-server/forge-framework/forge-starter-parent/pom.xml` — 注册新模块。
  - `forge-server/forge-framework/forge-dependencies/pom.xml` — 纳入 BOM。
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-collaboration/src/main/java/com/mdframe/forge/starter/collaboration/CollaborationCapability.java` — 定义 `LOGIN/DIRECTORY/MESSAGE/TODO/CALLBACK`。
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-collaboration/src/main/java/com/mdframe/forge/starter/collaboration/provider/CollaborationProviderRegistry.java` — Connector 注册、能力发现和重复 Provider 失败关闭。
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-collaboration/src/main/java/com/mdframe/forge/starter/collaboration/provider/CollaborationProvider.java` — Provider 元数据合同。
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-collaboration/src/main/java/com/mdframe/forge/starter/collaboration/connector/CollaborationConnector.java`、`LoginConnector.java`、`DirectoryConnector.java`、`MessageConnector.java`、`TodoConnector.java`、`CallbackConnector.java`、`AccessTokenProvider.java`、`ExternalSecretResolver.java` — 同目录下的基础、能力和外部 Secret 扩展接口。
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-collaboration/src/main/java/com/mdframe/forge/starter/collaboration/todo/CollaborationFlowActionGateway.java` — 外部待办动作调用 Forge 流程的低层反向 SPI，避免 Collaboration Plugin 依赖 Flow Plugin。
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-collaboration/src/main/java/com/mdframe/forge/starter/collaboration/model/CollaborationExecutionContext.java`、`VerifiedSocialIdentity.java`、`ProviderError.java`、`ExternalDepartment.java`、`ExternalUser.java`、`ExternalTag.java`、`DirectorySnapshot.java`、`ProviderMessageRequest.java`、`ProviderMessageResult.java`、`ProviderTodoRequest.java`、`ProviderTodoResult.java`、`CollaborationTaskEvent.java`、`ExternalTodoAction.java`、`FlowActionResult.java` — 同目录下的平台无关输入/输出记录。
- **关键签名**：
  ```java
  public interface CollaborationProvider {
      String platform();
      Set<CollaborationCapability> capabilities();
  }

  public final class CollaborationProviderRegistry {
      public CollaborationProvider requireProvider(String platform);
      public <T> T requireConnector(String platform, CollaborationCapability capability, Class<T> type);
  }
  ```
- **验收**：Fake Provider 可只实现部分能力；缺能力和重复平台均明确失败；业务编排层无企微/飞书/钉钉 switch。

## Task 2：建立 Collaboration Plugin 与应用装配（P0）

- **目标**：承载连接管理、目录编排、企微适配器、消息/待办桥接，保持 Starter 不反向依赖业务插件。
- **涉及文件**：
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-collaboration/pom.xml` — 新增插件，依赖 Collaboration/Social/Crypto/Cache/Outbound/Message Starter 及 System Plugin；通过 Starter 渠道合同与 Message Plugin 运行时协作，不形成 Plugin 反向依赖。
  - `forge-server/forge-framework/forge-plugin-parent/pom.xml` — 注册插件。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-collaboration/src/main/java/com/mdframe/forge/plugin/collaboration/config/CollaborationPluginConfiguration.java` — 扫描 Mapper、Provider 和 Handler。
  - `forge-server/forge-admin-server/pom.xml` — 主应用引入插件。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/pom.xml` — 仅依赖低层 Collaboration Starter 的待办发布 SPI。
- **关键签名**：
  ```java
  @Configuration
  public class CollaborationPluginConfiguration {
      @Bean
      CollaborationProviderRegistry collaborationProviderRegistry(
              List<CollaborationProvider> providers,
              List<CollaborationConnector> connectors);
  }
  ```
- **验收**：不启用 Collaboration Plugin 时 Social/Message/Flow 现有功能可启动；启用时 Provider、Job Handler 和 `COLLABORATION` 渠道只注册一次。

## Task 3：连接、应用、映射与运维数据迁移（P0）

- **目标**：建立 Spec 第 5 节的数据合同和兼容索引，不在 Flyway 中猜测或明文迁移 Secret。
- **涉及文件**：
  - `forge-server/db/migration/V1.0.57__add_collaboration_connection_foundation.sql` — 扩展 `sys_social_config/sys_user_social`，新建应用、映射、同步、待办和回调表。
  - `forge-server/db/migration/V1.0.58__extend_message_delivery_for_collaboration.sql` — 扩展消息主表、接收人和发送记录。
  - `forge-server/db/migration/V1.0.59__add_collaboration_resources_and_jobs.sql` — 字典、权限、菜单、出站场景和 Job 配置。
  - `forge-server/db/全量初始化SQL.sql` — 同步最新结构和内置数据，不写任何 Secret。
- **关键约束**：
  - 数值主键逻辑删除表使用 `del_flag BIGINT DEFAULT 0`，活动唯一键包含 `del_flag`，删除写当前主键。
  - `sys_social_tag_member` 是可重建纯关系表，允许物理替换；同步日志/回调日志仅由留存任务物理清理。
  - 结构、索引、字典和资源具备 `information_schema`/`NOT EXISTS` 防重复保护，`tenant_id=1`。
- **验收**：MySQL 8 新库、存量库和重复执行检查通过；歧义旧绑定只进入迁移阻塞清单，不自动归属。

## Task 4：认证加密与凭据生命周期服务（P0）

- **目标**：所有供应商 Secret 可安全取回调用、可轮换、可掩码，并共用 Forge 版本化持久化密文协议。
- **涉及文件**：
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-crypto/src/main/java/com/mdframe/forge/starter/crypto/crypto/CryptoAlgorithm.java` — 增加 `AES_GCM` 算法代码。
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-crypto/src/main/java/com/mdframe/forge/starter/crypto/crypto/impl/AESGCMEncryptor.java` — 使用 12-byte 随机 IV、128-bit 认证标签和现有 key ring 实现认证加密。
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-crypto/src/main/java/com/mdframe/forge/starter/crypto/crypto/EncryptorFactory.java` — 注册 `AES_GCM`，由 `VersionedPersistentCryptoService` 继续输出统一版本化密文。
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-social/src/main/java/com/mdframe/forge/starter/social/security/SocialAppCredentialService.java`、`DefaultSocialAppCredentialService.java` — 同目录下定义和实现加密、解密、掩码、空值保留、外部 Secret 解析和显式轮换，供登录和 Collaboration Plugin 共用。
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-social/src/main/java/com/mdframe/forge/starter/social/security/SecretContext.java`、`SecretSummary.java` — 同目录下定义租户/连接/应用/凭据类型绑定上下文和安全摘要，禁止 DTO 携带明文。
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-social/pom.xml` — 增加 Collaboration SPI 和持久化加密依赖，不把明文 Secret 交给上层 Controller。
- **关键签名**：
  ```java
  public interface SocialAppCredentialService {
      String encrypt(char[] plaintext, SecretContext context);
      char[] decrypt(String ciphertext, SecretContext context);
      SecretSummary summary(String ciphertext);
      String preserveOrRotate(String currentCiphertext, char[] requestedSecret, SecretContext context);
  }
  ```
- **验收**：密文篡改/未知 keyId 失败关闭；空更新零写；外部引用缺少 Resolver 时失败关闭；扫描不到明文、Token、密钥或真实个人数据。

## Task 4A：连接与独立应用配置服务（P0）

- **目标**：把 `sys_social_config` 从单一 OAuth 配置升级为连接根，并按能力安全读取独立应用；旧登录配置只在受控兼容期开启双读。
- **涉及文件**：
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-social/src/main/java/com/mdframe/forge/starter/social/domain/entity/SysSocialConfig.java` — 增加连接代码、企业 ID、权威策略、逻辑删除和审计字段，旧凭据字段标记兼容；能力集合不在连接实体重复保存。
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-social/src/main/java/com/mdframe/forge/starter/social/domain/entity/SysSocialAppConfig.java`、`SysSocialCapabilityBinding.java` — 新增物理应用和能力绑定实体，Secret 只保存一份密文或外部引用。
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-social/src/main/java/com/mdframe/forge/starter/social/mapper/SysSocialConfigMapper.java`、`SysSocialAppConfigMapper.java`、`SysSocialCapabilityBindingMapper.java` — 连接/应用/能力绑定查询和 CAS 签名。
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-social/src/main/resources/mapper/SysSocialConfigMapper.xml`、`SysSocialAppConfigMapper.xml`、`SysSocialCapabilityBindingMapper.xml` — 显式租户、连接、能力、状态和逻辑删除 SQL。
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-social/src/main/java/com/mdframe/forge/starter/social/service/impl/SocialConfigServiceImpl.java`、`SocialAppConfigServiceImpl.java` — 连接 CRUD、独立应用选择、缓存清理和旧配置兼容读取。
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-social/src/main/java/com/mdframe/forge/starter/social/controller/SocialConfigController.java`、`forge-server/forge-framework/forge-starter-parent/forge-starter-social/src/main/java/com/mdframe/forge/starter/social/domain/vo/SocialConnectionSummaryVO.java` — 旧管理接口在兼容期只返回脱敏 VO，并把写操作迁移到新连接接口。
- **关键签名**：
  ```java
  public SysSocialConfig selectConnectionByCode(String connectionCode);
  public SysSocialAppConfig requireEnabledApp(Long tenantId, Long connectionId,
                                               CollaborationCapability capability);
  public boolean bindCapability(Long tenantId, Long connectionId,
                                CollaborationCapability capability, Long appConfigId);
  public boolean updateApp(SocialAppSaveCommand command, String expectedCredentialCipher);
  ```
- **验收**：同租户可有多个同平台连接；一个物理应用可绑定多个能力且 Secret 只保存一份；每连接每能力最多一个活动绑定；Controller/VO 不获得密文；旧 `(platform, tenant)` 读取只有唯一连接时才兼容，否则失败关闭。

## Task 4B：外部身份绑定增加连接与企业维度（P0）

- **目标**：消除 `platform + uuid` 在多企业下的冲突，并禁止用户 Token 明文落库。
- **涉及文件**：
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-social/src/main/java/com/mdframe/forge/starter/social/domain/entity/SysUserSocial.java` — 增加 connection/enterprise/source/status/hash/logic-delete 字段，访问/刷新 Token 标记弃用。
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-social/src/main/java/com/mdframe/forge/starter/social/mapper/SysUserSocialMapper.java` — 按租户/连接/外部 ID 和 Forge 用户查询签名。
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-social/src/main/resources/mapper/SysUserSocialMapper.xml` — 显式租户/连接/逻辑删除 SQL 和绑定 CAS。
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-social/src/main/java/com/mdframe/forge/starter/social/service/ISocialUserService.java` — 将所有企业身份方法增加 tenantId/connectionId。
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-social/src/main/java/com/mdframe/forge/starter/social/service/impl/SocialUserServiceImpl.java` — 连接内绑定、解绑、停用和兼容迁移；默认不保存用户 Token。
- **关键签名**：
  ```java
  public SysUserSocial selectBinding(Long tenantId, Long connectionId, String externalUserId);
  public SysUserSocial selectBindingByUser(Long tenantId, Long connectionId, Long userId);
  public boolean bindVerifiedIdentity(VerifiedSocialIdentity identity, Long forgeUserId);
  ```
- **验收**：两企业相同 userid 可分别绑定；同连接外部用户/Forge 用户双向唯一；跨租户查询无结果；新绑定的 access/refresh token 均为空。

## Task 4C：旧明文凭据与存量身份兼容迁移（P0）

- **目标**：在新表和配置服务就绪后，对旧 `client_secret` 与缺少连接维度的身份绑定执行可盘点、可阻塞、可比较更新的迁移，不在 Flyway 中处理明文归属。
- **涉及文件**：
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-collaboration/src/main/java/com/mdframe/forge/plugin/collaboration/service/CollaborationCredentialMigrationService.java` — 旧配置 inventory、dry-run、完整预检、批次加密写入和旧列清空。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-collaboration/src/main/java/com/mdframe/forge/plugin/collaboration/service/CollaborationIdentityMigrationService.java` — 按唯一租户/平台连接回填身份 connectionId，歧义数据形成阻塞报告。
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-social/src/main/java/com/mdframe/forge/starter/social/mapper/SysSocialConfigMapper.java`、`SysSocialAppConfigMapper.java`、`SysUserSocialMapper.java` 及对应 XML — 复用 Task 4A/4B 的单一 Mapper，补充 inventory 与 CAS SQL，不在 Collaboration Plugin 新建重复应用配置 Mapper。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-collaboration/src/main/java/com/mdframe/forge/plugin/collaboration/controller/CollaborationCredentialMigrationController.java` — 仅授权管理员执行 inventory/dry-run/migrate，不返回明文或完整外部引用。
- **关键签名**：
  ```java
  public CryptoMigrationReport migrateCredentials(Long tenantId, String expectedActiveKeyId,
                                                    Integer batchSize, boolean dryRun);
  public IdentityMigrationReport migrateIdentities(Long tenantId, Integer batchSize,
                                                    boolean dryRun);
  ```
- **验收**：歧义连接、空租户和并发 CAS 冲突均阻塞且不猜测归属；批次冲突完整回滚；旧明文成功清空后不能回滚到只读旧列的历史二进制。

## Task 5：重构 OAuth state、回调票据与企业身份登录（P0）

- **目标**：消除前端自报第三方身份，登录严格绑定连接、租户和已同步用户映射。
- **回归修复（2026-08-24）**：授权地址生成与授权码换身份统一读取 LOGIN 应用配置，通过 `decryptAppSecret` 解密 `secretCipher` 并使用非缓存请求；修复旧连接 Secret 清空后企业微信自建应用无法发起授权的问题，同时保留未迁移连接的旧字段回退。
- **涉及文件**：
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-social/src/main/java/com/mdframe/forge/starter/social/service/SocialOAuthStateService.java` — Redis 一次性 state 和 `socialTicket`。
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-social/src/main/java/com/mdframe/forge/starter/social/controller/SocialController.java` — 按 `connectionCode` 发起授权，回调只返回票据，不记录/返回 `AuthUser`。
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-social/src/main/java/com/mdframe/forge/starter/social/domain/dto/SocialLoginRequest.java`、`forge-server/forge-framework/forge-starter-parent/forge-starter-auth/src/main/java/com/mdframe/forge/starter/auth/domain/LoginRequest.java` — 增加 `connectionCode/socialTicket`，弃用可信 `socialUuid`。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/strategy/SocialAuthStrategyImpl.java` — 消费票据、按连接映射加载用户、关闭企业连接自动注册。
  - `forge-admin-ui/src/views/login/api.js`、`forge-admin-ui/src/views/login/callback.vue` — 前端只传一次性票据。
- **关键签名**：
  ```java
  public String issueState(SocialOAuthIntent intent);
  public SocialOAuthIntent consumeState(String state);
  public String issueLoginTicket(VerifiedSocialIdentity identity);
  public VerifiedSocialIdentity consumeLoginTicket(String ticket, LoginClientContext client);
  ```
- **验收**：伪造 uuid/tenantId/connectionCode 不能登录；state/ticket 重放失败；未同步、停用、冲突、跨租户映射均失败关闭；现有消费型社交登录兼容策略有明确测试。

## Task 6：企业微信 Provider、HTTP 传输与 Token 管理（P0/P1）

- **目标**：建立企微适配器公共传输层，统一错误分类、出站安全、Token 缓存和刷新并发。
- **涉及文件**：
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-collaboration/src/main/java/com/mdframe/forge/plugin/collaboration/provider/wecom/WeComProvider.java` — 声明 LOGIN/DIRECTORY/MESSAGE/TODO/CALLBACK 能力。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-collaboration/src/main/java/com/mdframe/forge/plugin/collaboration/provider/wecom/WeComApiClient.java` — 使用安全出站客户端调用固定官方端点，解析 `errcode/errmsg/requestId`。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-collaboration/src/main/java/com/mdframe/forge/plugin/collaboration/provider/wecom/WeComAccessTokenProvider.java` — Token 读取、提前刷新、失效单次刷新和分布式锁。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-collaboration/src/main/java/com/mdframe/forge/plugin/collaboration/provider/wecom/WeComErrorClassifier.java` — 将限流、Token 失效、永久参数错误、临时网络错误分类。
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-outbound/src/main/java/com/mdframe/forge/starter/outbound/constant/OutboundScenes.java` — 增加 `COLLABORATION_PROVIDER`。
- **关键签名**：
  ```java
  public String getAccessToken(CollaborationExecutionContext context, TokenType tokenType);
  public <T> T execute(WeComRequest<T> request, CollaborationExecutionContext context);
  public ProviderError classify(int httpStatus, int errorCode);
  ```
- **验收**：缓存键含租户/连接/应用/Token 类型；并发刷新只调用一次；Token 失效只强制刷新一次；URL、Header、Token 和响应正文不写日志。

## Task 7：回调验签、解密、收件箱与快速应答（P0/P2）

- **目标**：所有企微回调先安全落入幂等收件箱，再由异步处理器执行业务动作。
- **涉及文件**：
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-collaboration/src/main/java/com/mdframe/forge/plugin/collaboration/provider/wecom/WeComCallbackCrypto.java` — URL 验证、SHA1 签名校验、AES 解密和 CorpId 校验。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-collaboration/src/main/java/com/mdframe/forge/plugin/collaboration/controller/CollaborationCallbackController.java` — 接收 `connectionCode + appCode` 的 GET/POST 回调入口、请求大小/时间窗限制和快速应答。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-collaboration/src/main/java/com/mdframe/forge/plugin/collaboration/service/CollaborationCallbackInboxService.java` — 事件 ID/规范摘要去重、加密负载、处理状态和重试。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-collaboration/src/main/java/com/mdframe/forge/plugin/collaboration/mapper/SocialCallbackEventMapper.java`、`forge-server/forge-framework/forge-plugin-parent/forge-plugin-collaboration/src/main/resources/mapper/SocialCallbackEventMapper.xml` — 显式租户/连接查询和状态 CAS。
- **关键签名**：
  ```java
  public CallbackVerificationResult verifyAndDecrypt(WeComCallbackRequest request, CallbackCredential credential);
  public CallbackAcceptResult accept(String connectionCode, String appCode, VerifiedCallback callback);
  public int claimPendingEvents(Long tenantId, int batchSize, String workerId);
  ```
- **验收**：签名篡改、CorpId 错误、过期时间戳、nonce/event 重放、超大正文均被拒绝；回调响应不等待目录或流程处理完成。

## Task 8：组织/用户/岗位/标签映射仓储（P1）

- **目标**：所有复杂查询和比较更新进入 Mapper XML，形成显式租户、连接和逻辑删除边界。
- **涉及文件**：
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-collaboration/src/main/java/com/mdframe/forge/plugin/collaboration/domain/entity/SocialOrgMapping.java`、`SocialPostMapping.java`、`SocialTag.java`、`SocialTagMember.java`、`SocialSyncLog.java`、`SocialSyncIssue.java` — 同目录下新增实体。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-collaboration/src/main/java/com/mdframe/forge/plugin/collaboration/mapper/SocialDirectoryMappingMapper.java` — 定义批量查询、upsert、last-seen 和停用签名。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-collaboration/src/main/resources/mapper/SocialDirectoryMappingMapper.xml` — 部门/用户/岗位/标签映射 SQL。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-collaboration/src/main/java/com/mdframe/forge/plugin/collaboration/mapper/SocialSyncLogMapper.java`、`forge-server/forge-framework/forge-plugin-parent/forge-plugin-collaboration/src/main/resources/mapper/SocialSyncLogMapper.xml` — 批次和问题单 SQL。
- **关键签名**：
  ```java
  List<SocialOrgMapping> selectOrgMappings(Long tenantId, Long connectionId, Collection<String> externalIds);
  int markOrgSeen(Long tenantId, Long connectionId, String externalId, Long runId, String sourceHash);
  int markUnseenOrgInactive(Long tenantId, Long connectionId, Long completedRunId);
  Page<SocialSyncIssue> selectIssuePage(Page<?> page, Long tenantId, SocialSyncIssueQuery query);
  ```
- **验收**：XML 显式过滤 `tenant_id/connection_id/del_flag`；批量同步不在 Service 构造 Wrapper；活动唯一键和 CAS 合同测试通过。

## Task 9：目录快照规划器与全量同步编排（P1）

- **目标**：部门、成员和标签采用先完整读取校验、后分阶段落库的可靠全量同步。
- **涉及文件**：
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-collaboration/src/main/java/com/mdframe/forge/plugin/collaboration/provider/wecom/WeComDirectoryConnector.java` — 分页读取部门、成员、成员详情、标签列表和标签详情。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-collaboration/src/main/java/com/mdframe/forge/plugin/collaboration/service/directory/DirectorySnapshotValidator.java` — 校验重复 ID、父级、循环、根节点、分页完整性。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-collaboration/src/main/java/com/mdframe/forge/plugin/collaboration/service/directory/DirectorySyncPlanner.java` — 计算 CREATE/UPDATE/UNCHANGED/INACTIVATE/ISSUE。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-collaboration/src/main/java/com/mdframe/forge/plugin/collaboration/service/directory/DirectorySyncOrchestrator.java` — 锁、批次、阶段事务、last-seen 和完成后停用。
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-collaboration/src/main/java/com/mdframe/forge/starter/collaboration/model/ExternalDepartment.java`、`ExternalUser.java`、`ExternalTag.java`、`DirectorySnapshot.java` — Provider SPI 共用的外部快照模型；`forge-server/forge-framework/forge-plugin-parent/forge-plugin-collaboration/src/main/java/com/mdframe/forge/plugin/collaboration/domain/model/DirectorySyncPlan.java` — 编排层差异计划。
- **关键签名**：
  ```java
  public DirectorySnapshot fetchSnapshot(CollaborationExecutionContext context, DirectorySyncScope scope);
  public void validate(DirectorySnapshot snapshot, DirectorySyncPolicy policy);
  public DirectorySyncPlan plan(DirectorySnapshot snapshot, DirectoryMappingSnapshot current);
  public DirectorySyncResult synchronize(Long connectionId, DirectorySyncCommand command);
  ```
- **验收**：拉取中断、分页重复、部门循环、缺失父级时零停用；相同快照重复同步零业务更新；成功快照才处理未出现对象。

## Task 10：Forge 组织/用户写入适配与冲突处理（P1）

- **目标**：按来源所有权安全写入 `sys_org/sys_user/sys_user_org`，不覆盖 RBAC 和手工资产。
- **涉及文件**：
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-collaboration/src/main/java/com/mdframe/forge/plugin/collaboration/service/directory/ForgeDirectoryWriter.java` — 按父子顺序创建/更新映射拥有的组织和用户关系。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-collaboration/src/main/java/com/mdframe/forge/plugin/collaboration/service/directory/UserIdentityMatchPolicy.java` — 已绑定、稳定员工编码、冲突和新建策略。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-collaboration/src/main/java/com/mdframe/forge/plugin/collaboration/service/directory/DirectorySyncIssueService.java` — 建问题单、人工绑定/忽略/重试和审计。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/mapper/SysOrgMapper.java`、`SysUserMapper.java` — 增加协同同步所需显式 Mapper 签名。
  - 对应 `forge-plugin-system/src/main/resources/mapper/SysOrgMapper.xml`、`SysUserMapper.xml` — 租户和所有权边界 SQL。
- **关键签名**：
  ```java
  public DirectoryWriteResult apply(DirectorySyncPlan plan, DirectoryWriteContext context);
  public IdentityMatchResult resolve(ExternalUser user, IdentityMatchContext context);
  public void resolveIssue(Long issueId, SyncIssueResolution command, Long operatorId);
  ```
- **验收**：手机号/邮箱相同不自动合并；角色/组织角色/手工岗位不被覆盖；非本连接拥有的组织和用户不被停用；人工绑定有权限和审计。

## Task 11：增量事件、Job Handler、重试与校准（P1/P2）

- **目标**：回调增量提升时效，定时全量修复丢事件，所有失败由 Job 中心统一补偿。
- **涉及文件**：
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-collaboration/src/main/java/com/mdframe/forge/plugin/collaboration/service/directory/DirectoryCallbackEventProcessor.java` — 成员/部门/标签事件转平台无关命令。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-collaboration/src/main/java/com/mdframe/forge/plugin/collaboration/job/CollaborationDirectorySyncJobHandler.java` — 全量/范围同步 Handler。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-collaboration/src/main/java/com/mdframe/forge/plugin/collaboration/job/CollaborationCallbackRetryJobHandler.java` — 回调收件箱重试 Handler。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-collaboration/src/main/java/com/mdframe/forge/plugin/collaboration/job/CollaborationDeliveryRetryJobHandler.java` — 消息/待办到期重试 Handler。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-collaboration/src/main/java/com/mdframe/forge/plugin/collaboration/service/CollaborationRetryPolicy.java` — 指数退避、抖动、最大次数和永久失败分类。
- **关键签名**：
  ```java
  @JobHandler(value = "collaborationDirectorySync", group = "COLLABORATION")
  public String execute(String param);

  public RetryDecision nextAttempt(ProviderError error, int attempt, Instant now);
  ```
- **验收**：同连接并发 Job 只有一个执行；限流/网络/Token 失效策略正确；永久参数错误不无限重试；Job 日志不含个人资料或 Secret。

## Task 12：扩展消息核心的企业协同投递合同（P1）

- **目标**：消息中心向渠道提供 Forge 接收人，并保存逐人结果和并发安全幂等键。
- **涉及文件**：
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-message/src/main/java/com/mdframe/forge/starter/message/channel/ChannelType.java` — 增加 `COLLABORATION`。
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-message/src/main/java/com/mdframe/forge/starter/message/channel/MessageChannel.java` — 增加连接上下文、接收人和逐人结果模型，保持旧渠道兼容。
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-message/src/main/java/com/mdframe/forge/starter/message/sdk/MessageClient.java` — 能力路由和不存在渠道的稳定错误。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-message/src/main/java/com/mdframe/forge/plugin/message/service/impl/MessageServiceImpl.java` — 批量接收人、幂等写入、部分失败和逐人状态。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-message/src/main/resources/mapper/SysMessageMapper.xml`、`SysMessageReceiverMapper.xml` — 同目录下的原子幂等和投递状态 SQL。
- **关键签名**：
  ```java
  public record ChannelSendRequest(Long tenantId, Long connectionId, Long messageId,
                                   String idempotencyKey, List<ChannelRecipient> recipients,
                                   String title, String content, Map<String, Object> params) {}
  public record ChannelSendResult(String providerRequestId, List<RecipientDeliveryResult> deliveries) {}
  ```
- **验收**：WEB/SMS/EMAIL 回归通过；相同幂等键并发只创建一份逻辑消息；部分失败只重试失败接收人。

## Task 13：企业微信消息 Connector 与模板卡片（P1）

- **目标**：把 Forge 用户映射为企微 userid，发送文本/模板卡片并正确处理部分失败。
- **涉及文件**：
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-collaboration/src/main/java/com/mdframe/forge/plugin/collaboration/provider/wecom/WeComMessageConnector.java` — 组装 `touser/agentid/msgtype`，调用企微消息 API。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-collaboration/src/main/java/com/mdframe/forge/plugin/collaboration/message/CollaborationMessageChannel.java` — 实现统一 `COLLABORATION` 渠道，按连接选择 Provider Connector。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-collaboration/src/main/java/com/mdframe/forge/plugin/collaboration/message/CollaborationRecipientResolver.java` — 批量读取连接内用户映射，区分未映射/停用/可发送。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-collaboration/src/main/java/com/mdframe/forge/plugin/collaboration/message/CollaborationMessageTemplatePolicy.java` — 文本、卡片字段、跳转 URL 和长度校验。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-collaboration/src/main/java/com/mdframe/forge/plugin/collaboration/controller/CollaborationMessageTestController.java` — 仅授权管理员给明确测试用户发送测试消息。
- **关键签名**：
  ```java
  public ProviderMessageResult send(ProviderMessageRequest request, CollaborationExecutionContext context);
  public RecipientResolution resolve(Long tenantId, Long connectionId, Collection<Long> forgeUserIds);
  ```
- **验收**：无映射用户明确失败；非法/超长模板发送前拒绝；企微 `invaliduser` 转逐人失败；测试接口不能向全员发送。

## Task 14：Flowable 待办可靠投影与状态机（P2）

- **目标**：流程事件只更新本地待办期望状态，异步发送/更新企微卡片，不阻塞流程事务。
- **涉及文件**：
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-collaboration/src/main/java/com/mdframe/forge/starter/collaboration/todo/CollaborationTodoPublisher.java` — Flow 插件依赖的低层 SPI。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-collaboration/src/main/java/com/mdframe/forge/plugin/collaboration/todo/CollaborationTodoProjectionService.java` — 创建、转派、完成、撤回、退回、终结状态机和版本。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-collaboration/src/main/java/com/mdframe/forge/plugin/collaboration/mapper/SocialTodoLinkMapper.java`、`forge-server/forge-framework/forge-plugin-parent/forge-plugin-collaboration/src/main/resources/mapper/SocialTodoLinkMapper.xml` — 期望状态 CAS、领取到期记录和幂等键。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-collaboration/src/main/java/com/mdframe/forge/plugin/collaboration/provider/wecom/WeComTodoConnector.java` — 创建/更新/关闭企微待办卡片。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/com/mdframe/forge/starter/flow/listener/FlowTaskEventListener.java` — 调用可选 Publisher，不直接调用企微 API。
- **关键签名**：
  ```java
  public interface CollaborationTodoPublisher {
      void taskCreated(CollaborationTaskEvent event);
      void taskAssigned(CollaborationTaskEvent event);
      void taskClosed(CollaborationTaskEvent event);
  }

  public TodoProjectionResult project(CollaborationTaskEvent event);
  public DeliveryClaim claim(Long linkId, long expectedVersion, String workerId);
  ```
- **验收**：创建/重复创建幂等；转派关闭旧人并创建新人版本；完成/撤回/终结关闭所有活动投影；外网失败不改变 Forge 任务状态。

## Task 15：待办安全入口、身份交换与受控回调动作（P2）

- **目标**：用户从企微进入或点击卡片动作时，重新验证身份、任务和权限，禁止链接篡改和过期任务执行。
- **涉及文件**：
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-collaboration/src/main/java/com/mdframe/forge/plugin/collaboration/todo/CollaborationTodoEntryService.java` — 生成/消费短期入口票据，发起企微 OAuth，安全跳转 Forge。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-collaboration/src/main/java/com/mdframe/forge/plugin/collaboration/todo/CollaborationTodoActionService.java` — 映射外部身份并调用 Forge 受控流程动作。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-collaboration/src/main/java/com/mdframe/forge/plugin/collaboration/service/CollaborationCallbackEventDispatcher.java` — 将卡片事件路由到 OPEN/APPROVE/REJECT/ACK 处理器。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-collaboration/src/main/java/com/mdframe/forge/plugin/collaboration/controller/CollaborationTodoEntryController.java` — 入口和 OAuth 回跳。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/com/mdframe/forge/starter/flow/integration/FlowCollaborationActionGateway.java` — 实现 `CollaborationFlowActionGateway`，内部复用现有 `FlowTaskActionAuthorization/FlowTaskService`，不向 Collaboration Plugin 暴露 Flow 内部类。
- **关键签名**：
  ```java
  public TodoEntryResult enter(String opaqueTicket, VerifiedSocialIdentity identity);
  public FlowActionResult execute(VerifiedSocialIdentity identity, ExternalTodoAction action,
                                  String idempotencyKey);
  ```
- **验收**：票据篡改/重放/过期、身份不匹配、任务转派/完成、跨租户、无权限均失败；直接审批默认关闭，开启后仍通过现有状态和幂等校验。

## Task 16：连接与应用管理前端（P1）

- **目标**：管理员在一个工作台完成连接、分能力应用、Secret 轮换和能力测试，保留旧菜单兼容入口。
- **涉及文件**：
  - `forge-admin-ui/src/api/collaboration.js` — 连接、应用、测试和迁移摘要 API。
  - `forge-admin-ui/src/views/system/collaboration/index.vue` — 企业协同连接列表和状态总览。
  - `forge-admin-ui/src/views/system/collaboration/components/ConnectionEditor.vue` — 连接基础信息、权威和身份策略。
  - `forge-admin-ui/src/views/system/collaboration/components/ApplicationEditor.vue` — LOGIN/DIRECTORY/MESSAGE/TODO 应用与 Secret 空值保留。
  - `forge-admin-ui/src/views/system/socialConfig.vue` — 兼容跳转或只读提示，不继续回显旧 Secret。
- **验收**：所有枚举来自字典；Secret 永不回填；空值保留和显式轮换文案清楚；最长平台/连接名称不溢出。
- **实施偏差（2026-07-28）**：页面文件按 V1.0.59 菜单 `component` 值实现为 `connections.vue`（连接列表+详情弹窗+应用/能力绑定编辑，替代 index.vue + ConnectionEditor/ApplicationEditor 拆分），API 层 `src/api/collaboration.js` 已建；`socialConfig.vue` 兼容改造与迁移摘要接口延后至 Task 19A 前补充。补齐后端能力绑定/应用删除端点与 `V1.0.62__add_collaboration_binding_api_resources.sql`（含 4 个新字典类型与 API 资源授权）。

## Task 17：同步、映射、投递和回调运维前端（P1/P2）

- **目标**：管理员可查看同步批次、问题单、映射、消息/待办投递和回调处理，并执行受控重试。
- **涉及文件**：
  - `forge-admin-ui/src/views/system/collaboration/sync.vue` — 同步触发、进度、计数、阶段和日志。
  - `forge-admin-ui/src/views/system/collaboration/issues.vue` — 问题单绑定/忽略/重试。
  - `forge-admin-ui/src/views/system/collaboration/mappings.vue` — 部门/用户/岗位/标签映射只读检索。
  - `forge-admin-ui/src/views/system/collaboration/deliveries.vue` — 消息/待办投递、错误分类和重试。
  - `forge-admin-ui/src/views/system/collaboration/callback-events.vue` — 回调元数据和处理状态，不展示解密正文。
- **验收**：手机号/邮箱默认脱敏；危险操作二次确认和权限控制；页面不展示 Secret、Token、原始回调；PC 常用宽度和移动窄屏无重叠。

## Task 18：管理接口、权限资源与运维文档（P1/P2）

- **目标**：补齐 Controller/DTO/VO、权限、API 加解密、配置和运行手册，使交付可运维。
- **涉及文件**：
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-collaboration/src/main/java/com/mdframe/forge/plugin/collaboration/controller/CollaborationConnectionController.java` — 连接、应用、能力测试和逻辑删除。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-collaboration/src/main/java/com/mdframe/forge/plugin/collaboration/controller/CollaborationOperationsController.java` — 同步、问题单、映射、投递和回调查询/重试。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-collaboration/src/main/java/com/mdframe/forge/plugin/collaboration/dto/CollaborationConnectionSaveRequest.java`、`CollaborationAppSaveRequest.java`、`CollaborationSyncCommand.java` — 同目录下的入参校验和敏感字段边界。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-collaboration/src/main/java/com/mdframe/forge/plugin/collaboration/vo/CollaborationConnectionVO.java`、`CollaborationSyncLogVO.java`、`CollaborationDeliveryVO.java` — 同目录下的脱敏输出。
  - `forge-docs/guide/integration/enterprise-collaboration.md` — 架构、Provider 扩展、配置、迁移、监控、故障和回退。
- **关键权限**：
  ```text
  system:collaboration:connection:list|create|update|delete|test
  system:collaboration:sync:view|execute|resolve
  system:collaboration:delivery:view|retry
  system:collaboration:callback:view|retry
  system:collaboration:credential:migrate
  ```
- **验收**：Controller 不返回实体或密文；敏感写接口使用 `@ApiDecrypt`；Service 和 Mapper 均显式校验租户；操作日志不记录请求 Secret。

## Task 19：分期自动化、真实企微 UAT 与部署门禁（P0-P2）

- **目标**：按 `test-spec.md` 形成可复跑证据，使 M1 和 M2 可以分别审查、验收和上线，避免待办资料阻塞一期。
- **涉及文件**：
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-collaboration/src/test/java/com/mdframe/forge/starter/collaboration/provider/ProviderContractTestKit.java` — 公共 Provider 合同。
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-social/src/test/java/com/mdframe/forge/starter/social/security/SocialAppCredentialServiceTest.java` — 凭据生命周期、认证密文和外部引用测试。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-collaboration/src/test/java/com/mdframe/forge/plugin/collaboration/service/CollaborationCredentialMigrationServiceTest.java`、`service/CollaborationIdentityMigrationServiceTest.java`、`provider/wecom/WeComAccessTokenProviderTest.java`、`service/directory/DirectorySyncOrchestratorTest.java`、`message/CollaborationMessageChannelTest.java`、`todo/CollaborationTodoProjectionServiceTest.java`、`todo/CollaborationFlowActionGatewayContractTest.java`、`service/CollaborationCallbackInboxServiceTest.java`、`mapper/CollaborationMapperXmlContractTest.java` — 同测试源码根下的迁移、编排和合同测试。
  - `forge-admin-ui/src/views/system/collaboration/__tests__/` — Secret 表单、状态转换、问题单和投递交互测试。
  - `code-copilot/changes/unified-enterprise-collaboration/test-spec.md` — 更新执行状态和基线。
  - `code-copilot/changes/unified-enterprise-collaboration/execution-log.md` — 记录命令、数量、接口/数据库结论、警告、跳过和服务清理。
- **Task 19A / M1 必过门禁**：
  1. Java P0/P1 目标测试、相关 Reactor package、Admin Server 装配和一期前端 production build。
  2. Mapper XML、Flyway 防重复/placeholder、Secret/Token/PII 日志和平台分支静态扫描。
  3. MySQL 8 新库/存量库迁移、重复执行、歧义数据阻塞、Secret dry-run/migrate/rotate。
  4. 两租户、两连接、同平台同 userid 的隔离测试。
  5. 真实企微全量/增量/标签/登录/消息/目录回调和失败补偿 UAT。
  6. M1 回滚演练证明不恢复明文 Secret，旧二进制不会在清理明文后被误部署。
- **Task 19B / M2 必过门禁**：
  1. 待办投影状态机、安全入口、回调动作、并发版本和补偿自动化测试。
  2. 真实企微待办创建/转派/签收/完成/撤回、卡片失效、回调验签和故障恢复 UAT。
  3. 默认关闭的卡片直批只验证拒绝行为；只有客户另行启用并提供流程白名单时才执行直批 UAT。
  4. M2 部署、回退、监控、告警和留存演练。
- **验收**：19A 通过即可把 M1 标记为 `milestone accepted` 并独立上线；19B 通过后本变更才标记 `done`。未验证的能力不得写入对应里程碑完成声明。

## 后续独立变更（不计入本次完成状态）

### Feishu Adapter

- 实现 `FeishuProvider`、登录/目录/消息/待办/回调 Connector。
- 复用本变更的连接、映射、同步、投递、回调和运维页面。
- 通过同一 `ProviderContractTestKit` 和真实飞书测试企业 UAT。

### DingTalk Adapter

- 实现 `DingTalkProvider`、登录/目录/消息/待办/回调 Connector。
- 复用本变更的连接、映射、同步、投递、回调和运维页面。
- 通过同一 `ProviderContractTestKit` 和真实钉钉测试企业 UAT。
