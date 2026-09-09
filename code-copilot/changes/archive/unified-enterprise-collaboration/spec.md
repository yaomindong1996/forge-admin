# 统一企业协同集成与企业微信全能力接入
> status: propose
> created: 2026-07-28
> complexity: 🔴复杂

## 1. 背景与目标

Forge 当前已经具备第三方 OAuth 登录、组织/用户/岗位、消息中心、任务调度和 Flowable 待办能力，但这些能力尚未形成可复用的企业协同集成层。若直接把企业微信逻辑写进登录、用户、消息或流程模块，后续接入飞书、钉钉时会重复建设配置、Token、通讯录、消息、待办、回调、同步日志和异常补偿。

本变更以现有 `sys_social_config` 为连接根，建设平台无关的企业协同能力，企业微信作为首个完整适配器。完成后达到以下可验证结果：

1. 一个 Forge 租户可以配置一个或多个企业协同连接，每个连接可维护多个物理应用，并把登录、通讯录、消息和待办能力绑定到指定应用；同一应用可被多种能力复用且 Secret 只保存一份。
2. 企业微信部门、成员、岗位文本和标签可以全量同步、增量处理、定期校准；冲突、失败和人工重试可查询。
3. 企业微信登录只认服务端校验过的连接和外部身份，并优先绑定通讯录已建立的 Forge 用户，不因手机号/邮箱相同而自动合并账号。
4. Forge 消息中心可以通过统一 `COLLABORATION` 渠道投递企微应用消息，保留逐接收人结果、幂等键、失败重试和发送记录。
5. `sys_flow_task` 的创建、转派、完成、撤回和终结可以投影为企微待办卡片；点击或回调后仍由 Forge 校验当前用户、任务状态、办理权限和幂等性。
6. 所有平台凭据均采用 Forge 版本化 `AES_GCM` 可逆加密或外部 Secret 引用；管理接口只返回“已配置”状态和固定掩码。
7. 飞书、钉钉后续只需实现 Provider Connector 和通过统一合同测试，不再修改组织同步、消息、待办和运维主链路。

### 1.1 本次交付边界

**本次必须交付**：

- 通用 `forge-starter-collaboration` SPI 与 `forge-plugin-collaboration` 编排插件。
- `sys_social_config` 兼容升级、独立应用配置、组织/用户/岗位/标签映射、同步日志、问题单、待办投影和回调事件模型。
- 企业微信登录、通讯录、标签、消息、待办卡片、回调、安全跳转、失败补偿和管理界面。
- Provider 合同测试套件以及 Fake Provider，证明通用编排不依赖企业微信分支。

本变更按两个可独立验收和上线的里程碑交付：

| 里程碑 | 范围 | 独立完成标志 |
|------|------|-------------|
| M1 一期基础协同 | P0 + P1：通用底座、安全登录、企微通讯录/标签、消息、对应管理与补偿 | 完成 Task 0-13、Task 18 的一期接口、Task 16-17 的一期页面和 Task 19A；可独立 Review、UAT 和上线，不依赖待办应用资料 |
| M2 二期待办协同 | P2：待办投影、企微卡片、安全跳转、回调动作、对应运维 | 完成 Task 14-15、Task 17-18 的二期范围和 Task 19B；通过后本变更整体可标记 `done` |

**本次不交付**：

- 飞书、钉钉真实 API 适配器；二者作为后续独立变更实现。
- 将企业微信、飞书或钉钉设为 Forge 业务角色和权限的权威来源。
- 默认交付只允许安全打开 Forge 待办详情。通用受控动作网关和关闭状态的开关属于 M2 技术交付，但真正启用卡片内同意/驳回、流程白名单及真实 UAT 属客户可选项，不计入 M2 基线验收。
- 移动端复杂业务表单重做；继续复用 Forge 已有 H5/PC 待办页面。

## 2. 代码现状（Research Findings）

### 2.1 登录与连接配置

- `SysSocialConfig` 仅包含 `platform/clientId/clientSecret/redirectUri/agentId/scope/tenantId`，实体定位仍是“三方登录配置”，不能表达同一企业连接下的独立通讯录、消息和待办应用。出处：`forge-server/forge-framework/forge-starter-parent/forge-starter-social/src/main/java/com/mdframe/forge/starter/social/domain/entity/SysSocialConfig.java`，`SysSocialConfig`。
- `sys_social_config` 唯一索引为 `(platform, tenant_id)`，同租户同平台只能配置一行；`client_secret` 非空且为普通字符串列。出处：`forge-server/db/全量初始化SQL.sql:5514`，`sys_social_config` DDL。
- 配置读取固定返回平台/租户下第一条启用记录，且查询由 Service 的 `LambdaQueryWrapper` 构建。出处：`forge-server/forge-framework/forge-starter-parent/forge-starter-social/src/main/java/com/mdframe/forge/starter/social/service/impl/SocialConfigServiceImpl.java`，`SocialConfigServiceImpl#selectByPlatformAndTenant`。
- OAuth 请求缓存键只有 `platform:tenantId`，无法区分同平台下的连接或应用。出处：`forge-server/forge-framework/forge-starter-parent/forge-starter-social/src/main/java/com/mdframe/forge/starter/social/factory/SocialAuthRequestFactory.java`，`SocialAuthRequestFactory#buildCacheKey`。
- `SocialAuthRequestFactory#buildRequest` 已支持 `WECHAT_ENTERPRISE`、`FEISHU`、`DINGTALK` 和 `DINGTALK_ACCOUNT`，说明登录库具备平台基础适配，但只覆盖授权登录。出处：`forge-server/forge-framework/forge-starter-parent/forge-starter-social/src/main/java/com/mdframe/forge/starter/social/factory/SocialAuthRequestFactory.java`，`SocialAuthRequestFactory#buildRequest`。
- `/social/callback` 将完整 `AuthUser` 返回前端，并记录完整第三方用户 JSON；前端再把 `socialUuid/socialNickname/socialAvatar/socialEmail` 提交给 `/auth/login`。`SocialAuthStrategyImpl` 直接信任这些客户端字段。出处：`forge-server/forge-framework/forge-starter-parent/forge-starter-social/src/main/java/com/mdframe/forge/starter/social/controller/SocialController.java` 的 `SocialController#callback`、`forge-admin-ui/src/views/login/callback.vue`、`forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/strategy/SocialAuthStrategyImpl.java` 的 `SocialAuthStrategyImpl#doAuthenticate`。这是必须在开放企业登录前修复的身份伪造和令牌泄露风险。
- `SocialAuthStrategyImpl` 在未绑定时可自动创建用户，默认部门逻辑尚未实现；如果企微通讯录同步和登录并行开放，可能形成重复账号和无组织账号。出处：`forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/strategy/SocialAuthStrategyImpl.java`，`SocialAuthStrategyImpl#doAuthenticate`。

### 2.2 用户、组织与外部身份

- `SysUserSocial` 只按 `platform + uuid` 表达第三方账号，缺少连接、企业和来源维度；表中还保存访问令牌和刷新令牌。出处：`forge-server/forge-framework/forge-starter-parent/forge-starter-social/src/main/java/com/mdframe/forge/starter/social/domain/entity/SysUserSocial.java` 的 `SysUserSocial`、`forge-server/db/全量初始化SQL.sql:5682`。
- `SocialUserServiceImpl#selectByPlatformAndUuid` 和 `#selectByUserIdAndPlatform` 均未带租户/连接条件，无法安全支持同一平台的多个企业。出处：`forge-server/forge-framework/forge-starter-parent/forge-starter-social/src/main/java/com/mdframe/forge/starter/social/service/impl/SocialUserServiceImpl.java`，`SocialUserServiceImpl`。
- Forge 已有 `sys_org`、`sys_user`、`sys_user_org`、`sys_post`、`sys_user_post`，可以承载同步结果；当前初始化结构中没有 `sys_social_org_mapping/sys_social_post_mapping/sys_social_tag/sys_social_sync_log`，即没有外部映射、快照、冲突和来源所有权模型。出处：`forge-server/db/全量初始化SQL.sql` 的 System 表 DDL 集合。
- 项目长期决策规定权限按“租户 + 当前组织”计算，第三方同步不能绕过 `sys_user_org/sys_user_org_role` 或回退旧 `sys_user_role`。出处：`code-copilot/memory/decisions.md`“组织上下文权限按当前组织计算”。

### 2.3 消息能力

- `forge-starter-message` 已提供可插拔 `MessageChannel`，但 `ChannelType` 只有 `EMAIL/SMS/WEB`。出处：`forge-server/forge-framework/forge-starter-parent/forge-starter-message/src/main/java/com/mdframe/forge/starter/message/channel/MessageChannel.java` 的 `MessageChannel`、同目录 `ChannelType.java`。
- `MessageClient` 通过 Spring 注入的渠道集合选择实现，适合增加一个统一 `COLLABORATION` 渠道。出处：`forge-server/forge-framework/forge-starter-parent/forge-starter-message/src/main/java/com/mdframe/forge/starter/message/config/MessageAutoConfiguration.java` 的 `MessageAutoConfiguration#messageClient`、`forge-server/forge-framework/forge-starter-parent/forge-starter-message/src/main/java/com/mdframe/forge/starter/message/sdk/MessageClient.java` 的 `MessageClient#resolveChannel`。
- `MessageServiceImpl` 已创建消息、接收人和发送记录，但对外渠道只传手机号/邮箱；发送结果是整批成功或失败，不能表达企微的无映射用户、部分失败和逐人重试。出处：`forge-server/forge-framework/forge-plugin-parent/forge-plugin-message/src/main/java/com/mdframe/forge/plugin/message/service/impl/MessageServiceImpl.java`，`MessageServiceImpl#sendToChannel/#createSendRecord`。
- `sendIfAbsent` 先查后写，`sys_message` 的 `idx_biz` 不是唯一索引，存在并发重复发送窗口；同一个流程任务转派后也不能只用 `taskId` 作为唯一消息键。出处：`forge-server/forge-framework/forge-plugin-parent/forge-plugin-message/src/main/java/com/mdframe/forge/plugin/message/service/impl/MessageServiceImpl.java` 的 `MessageServiceImpl#sendIfAbsent`、`forge-server/db/全量初始化SQL.sql:4997`。

### 2.4 流程待办与任务调度

- `FlowTaskEventListener` 已监听 `TASK_CREATED/TASK_ASSIGNED/TASK_COMPLETED/ENTITY_DELETED` 并维护 `sys_flow_task`，可以作为待办投影的领域事件来源。出处：`forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/com/mdframe/forge/starter/flow/listener/FlowTaskEventListener.java`，`FlowTaskEventListener#onEvent`。
- 当前任务创建和转派只发送 `WEB` 站内信，任务完成后只把对应站内信置已读。出处：`forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/com/mdframe/forge/starter/flow/listener/FlowTaskEventListener.java`，`FlowTaskEventListener#sendTaskCreatedMessage/#markTaskTodoMessageRead`。
- `sys_flow_task` 已保存任务、处理人、候选人、业务键、状态和动作幂等摘要；外部待办回调可复用现有任务权限和幂等能力，不能直接更新该表。出处：`forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/com/mdframe/forge/starter/flow/entity/FlowTask.java` 的 `FlowTask`、同模块 `service/impl/FlowTaskActionAuthorization.java`。
- Forge Job 已支持 `@JobHandler`、集群调度、日志、重试和告警，本变更应注册同步与补偿 Handler，不新增第二套调度配置表。出处：`forge-server/forge-framework/forge-starter-parent/forge-starter-job/src/main/java/com/mdframe/forge/starter/job/annotation/JobHandler.java`、`forge-server/forge-framework/forge-plugin-parent/forge-plugin-job/src/main/java/com/mdframe/forge/plugin/job/example/JobExamples.java`、同模块 `entity/SysJobConfig.java`。

### 2.5 可复用安全基础

- `PersistentCryptoService` 已提供版本化持久化密文的加密、解密、盘点和重加密能力，数据连接已按“空值保留、非空轮换”使用；本变更在该机制中增加 `AES_GCM` 算法，不另建第二套密文协议。出处：`forge-server/forge-framework/forge-starter-parent/forge-starter-crypto/src/main/java/com/mdframe/forge/starter/crypto/persistence/PersistentCryptoService.java`、同目录 `VersionedPersistentCryptoService.java`、`forge-server/forge-framework/forge-plugin-parent/forge-plugin-data/src/main/java/com/mdframe/forge/plugin/data/service/impl/DataConnectionServiceImpl.java` 的 `DataConnectionServiceImpl#toEntity`。
- 当前 AES 实现通过 Hutool 默认 AES 模式加密，没有在代码中体现 GCM 的随机 IV 和认证标签。企业协同 Secret 上线前必须完成认证加密门禁，或接入外部 Secret Resolver，不能直接复用明文列。出处：`forge-server/forge-framework/forge-starter-parent/forge-starter-crypto/src/main/java/com/mdframe/forge/starter/crypto/crypto/impl/AESEncryptor.java`。
- `forge-starter-outbound` 已具备出站目标校验和白名单，可新增 `COLLABORATION_PROVIDER` 场景保护供应商 API 请求。出处：`forge-server/forge-framework/forge-starter-parent/forge-starter-outbound/src/main/java/com/mdframe/forge/starter/outbound/security/OutboundPolicyService.java`、同模块 `constant/OutboundScenes.java`。

## 3. 功能点

### 3.1 P0：通用底座与安全门禁

- [ ] 新增 Provider/Connector SPI、能力枚举、统一上下文、Provider Registry 和合同测试套件。
- [ ] 将 `sys_social_config` 升级为企业协同连接根，允许同租户同平台多个企业连接。
- [ ] 新增独立应用配置，登录、通讯录、消息、待办凭据互不覆盖。
- [ ] 实现版本化认证加密、Secret 轮换、旧明文盘点/迁移和失败关闭。
- [ ] OAuth state 服务端一次性校验，回调只发放短期一次性登录票据，不再返回 `AuthUser` 或信任客户端 `socialUuid`。
- [ ] Token 缓存按租户、连接、应用、Token 类型隔离，并使用分布式刷新锁。
- [ ] 完成企业微信 Token、部门、成员、标签、测试消息和回调解密的纵向连通验证。

### 3.2 P1：企业微信通讯录、登录与消息

- [ ] 部门树全量同步，校验父子关系、循环、缺失父级和映射冲突。
- [ ] 成员全量同步，建立企微 `userid` 到 Forge 用户的稳定映射及主组织关系。
- [ ] 同步企微标签及标签下成员/部门关系；标签不自动转换为 Forge 角色。
- [ ] 支持成员新增、变更、离职、转部门、删除的增量事件，并以定时全量校准兜底。
- [ ] 同步只修改本连接拥有的映射记录；Forge 角色、组织内角色和手工岗位默认不覆盖。
- [ ] 同步日志、问题单、统计、重试、限流、断点和管理端人工处理可用。
- [ ] 企微登录必须命中连接内映射；未同步或存在冲突时失败关闭并进入问题单。
- [ ] 增加统一 `COLLABORATION` 消息渠道，支持企微文本/模板卡片、跳转链接、逐接收人结果和重试。

### 3.3 P2：企业微信待办、回调与补偿

- [ ] 流程任务创建、转派、完成、撤回、退回、终结形成幂等待办投影，不在 Flowable 事件事务内调用外网。
- [ ] 待办投影异步创建/更新/关闭企微卡片，记录期望状态、外部状态、版本和最后错误。
- [ ] 企微点击先完成外部身份校验，再校验接收人、租户、任务当前状态和 Forge 办理权限。
- [ ] 企微事件回调完成验签、解密、时间窗、防重放、去重、持久化和异步处理。
- [ ] 基线只开放“查看详情”；交付受控动作扩展点和默认关闭的开关。客户另行启用卡片内同意/驳回时，必须配置流程白名单并再次调用 Forge 受控流程动作和幂等校验。
- [ ] 网络、限流、Token 失效、部分失败和回调处理失败支持指数退避、自动补偿和人工重试。

### 3.4 P0/P1：通用扩展合同门禁

- [ ] Fake Provider 通过登录、目录、消息、待办、回调的公共合同测试。
- [ ] 编排层和管理接口不存在按 `WECHAT_ENTERPRISE/FEISHU/DINGTALK` 分支的业务判断。
- [ ] 上述两项随 M1 完成，属于通用底座上线门禁，不属于客户清单中的 P3 可选能力。
- [ ] 飞书、钉钉真实适配器分别以后续独立变更接入，不修改通用数据模型和编排协议。

## 4. 业务规则

### 4.1 连接、应用与能力

1. `sys_social_config` 表示一个租户下的一个外部企业连接，不再表示某一个 OAuth 应用。
2. 同一个连接可维护多个物理应用，并通过能力绑定把 `LOGIN/DIRECTORY/MESSAGE/TODO` 指向指定应用；同一应用可复用多个能力，每个能力只有一个当前启用绑定。应用支持哪些业务能力只以能力绑定表为准，连接表和应用表不重复保存能力集合。
3. `CALLBACK` 是 Provider 技术能力，不是连接级业务能力绑定。回调入口必须同时携带公开的 `connectionCode + appCode`，据此加载该物理应用自己的 Callback Token/EncodingAESKey；禁止只按连接随机选择回调凭据。
4. Provider 能力由 Registry 查询；调用不存在的能力必须返回明确的 `CAPABILITY_NOT_SUPPORTED`，禁止静默降级到其他平台或渠道。
5. 连接停用后禁止新登录、同步、发送和回调动作；已存在映射和历史记录保留。

### 4.2 身份和账号绑定

1. 企业协同登录的租户、连接、平台和外部用户 ID 必须来自服务端已验证的 state/票据，不接受前端自报值。
2. OAuth state 和登录票据均为一次性、短有效期；消费后立即失效，并绑定连接、租户、客户端和原始登录动作。
3. 企业连接默认 `SYNC_REQUIRED`：通讯录同步成功并建立映射后才允许登录，禁止登录时自动创建无组织用户。
4. 不按姓名、昵称、手机号或邮箱自动合并账号。客户确认员工唯一编码后，可增加“精确员工编码匹配”策略；匹配不唯一时进入问题单。
5. 同一连接内一个外部用户只能绑定一个 Forge 用户，一个 Forge 用户也只能绑定一个该连接身份。
6. 离职或外部删除默认停用连接映射；仅当该 Forge 用户由本连接独占托管且客户启用策略时，才停用 `sys_user`。不物理删除历史用户。

### 4.3 目录同步

1. 企业微信默认是部门、成员基础身份和标签的权威来源；Forge 是角色、权限、组织内角色、业务岗位和流程授权的权威来源。
2. 同步只管理带本连接映射的组织、用户关系和标签，不删除或覆盖 Forge 手工维护且没有该连接映射的数据。
3. 全量同步采用“读取完整快照 -> 校验 -> 分阶段落库 -> 成功后处理未出现记录”的模式；拉取中断时不得误停用存量人员。
4. 同一连接同一时刻只允许一个目录同步；锁键至少包含 `tenantId + connectionId + syncType`。
5. 重复事件和重复全量同步必须幂等；外部对象未变化时不得重复更新 Forge 审计字段。
6. 标签同步使用标签列表和标签详情接口建立关系；不得假设成员详情直接包含反向标签集合。
7. 平台 position 文本默认只进入岗位映射问题单或只读资料，不自动新建/覆盖 Forge 岗位，除非客户确认岗位权威规则。

### 4.4 消息与待办

1. 普通通知复用 `sys_message/sys_message_receiver/sys_message_send_record`，不另建一套消息模板中心。
2. 每次渠道投递必须有确定性幂等键；任务转派等重复业务键使用“事件类型 + 版本 + 接收人”区分。
3. 无外部身份映射的接收人记为可查询失败，不把手机号/邮箱直接当企微接收标识。
4. 待办的权威状态始终是 Forge `sys_flow_task`/Flowable；外部平台只是投影，回调不能直接修改投影表来冒充审批完成。
5. 外部回调执行流程动作前必须重新加载 Forge 用户、租户、当前组织、任务、处理人/候选人、任务状态和权限。
6. 任务已完成、已撤回、已转派或操作者不再有权时，回调返回业务已失效并更新外部卡片，不重复执行。
7. 外部网络失败不能回滚业务流程；投影进入待补偿状态，并由 Job 中心重试和告警。

### 4.5 安全、隐私与日志

1. Secret、Access Token、Refresh Token、回调 Token、EncodingAESKey 和解密后回调正文禁止出现在 API 响应、应用日志、操作日志和异常消息中。
2. 管理端读取凭据只返回 `hasSecret`、固定掩码、最后轮换时间和 keyId；空 Secret 更新表示保留，显式轮换使用比较更新避免并发恢复旧值。
3. 认证加密不可用、keyId 未配置或密文损坏时失败关闭，不允许回退明文。
4. 供应商响应只保存必要的错误码、请求 ID、外部消息 ID和脱敏摘要；成员手机号、邮箱和姓名不得写入同步错误日志。
5. 回调先校验请求大小、签名、时间窗和重放，再解密；原始负载如需补偿必须加密存储并设置留存期。
6. 所有管理接口显式校验租户和权限；超级管理员也必须向 Mapper 传入目标租户，不能依赖租户拦截器隐式隔离。

## 5. 数据变更

> 迁移版本暂定从当前最大版本 `V1.0.56` 之后顺延。实施前如出现新版本，文件名必须整体后移，不得复用已执行版本。

| 操作 | 表名 | 字段/索引 | 说明 |
|------|------|-----------|------|
| 修改 | `sys_social_config` | `connection_code/connection_name/enterprise_id/connection_type/identity_policy/directory_authority/default_org_id/del_flag/create_dept` | 升级为连接根；能力集合由绑定表实时解析；活动唯一键为 `(tenant_id, connection_code, del_flag)` 和 `(tenant_id, platform, enterprise_id, del_flag)` |
| 兼容 | `sys_social_config` | 原 `client_id/client_secret/redirect_uri/agent_id/scope` | 暂作旧登录配置双读；迁移完成后 `client_secret` 必须清空，旧字段不再通过 API 返回 |
| 新增 | `sys_social_app_config` | `connection_id/app_code/app_name/client_id/agent_id/secret_mode/secret_cipher/secret_ref/callback_token_cipher/encoding_aes_key_cipher/redirect_uri/scope/config_json/status/del_flag` | 连接下的物理应用；业务能力由绑定表表达，回调凭据归属具体应用；活动唯一键 `(tenant_id, connection_id, app_code, del_flag)` |
| 新增 | `sys_social_capability_binding` | `connection_id/capability/app_config_id/config_json/status/del_flag` | 将 LOGIN/DIRECTORY/MESSAGE/TODO 绑定到物理应用；活动唯一键 `(tenant_id, connection_id, capability, del_flag)` |
| 修改 | `sys_user_social` | `connection_id/external_enterprise_id/managed_by_sync/external_status/source_hash/last_sync_time/del_flag` | `uuid` 继续承载外部用户 ID；新增连接维度和来源状态；访问/刷新令牌默认不再落库 |
| 修改 | `sys_user_social` | 唯一索引 | 替换为 `(tenant_id, connection_id, uuid, del_flag)` 及 `(tenant_id, connection_id, user_id, del_flag)` |
| 新增 | `sys_social_org_mapping` | 外部部门、父部门、`org_id/source_hash/last_seen_run_id/status/del_flag` | 稳定部门映射和全量快照对账 |
| 新增 | `sys_social_post_mapping` | 外部岗位编码/文本、`post_id/status/del_flag` | 岗位映射；企微首期默认不自动创建岗位 |
| 新增 | `sys_social_tag` | 外部标签、名称、状态、`last_seen_run_id/del_flag` | 保存平台标签资产 |
| 新增 | `sys_social_tag_member` | `tag_id/member_type/external_member_id/local_target_id` | 标签成员/部门纯关系重建表；允许同步事务内物理替换，并在 Spec 记录原因 |
| 新增 | `sys_social_sync_log` | 批次、类型、触发来源、阶段、计数、游标、状态、错误码、起止时间 | 运行日志；按留存任务物理清理 |
| 新增 | `sys_social_sync_issue` | 对象类型、外部 ID、问题码、脱敏摘要、处理状态、处理动作、重试次数 | 冲突和人工处理队列，不保存明文敏感快照 |
| 新增 | `sys_social_todo_link` | `task_id/connection_id/user_id/external_user_id/desired_state/delivery_state/version/external_id/idempotency_key/retry_at/del_flag` | Flowable 任务到外部待办的可靠投影/出站状态 |
| 新增 | `sys_social_callback_event` | `connection_id/event_id/dedup_hash/event_type/signature_status/payload_cipher/process_status/retry_count` | 回调收件箱、去重和补偿；原文加密并按留存任务清理 |
| 修改 | `sys_message` | `connection_id/idempotency_key` | 绑定企业协同连接和确定性消息键 |
| 修改 | `sys_message_receiver` | `delivery_status/delivery_attempts/external_id/last_error_code/last_attempt_time/next_retry_time` | 支持逐接收人发送结果和补偿 |
| 修改 | `sys_message_send_record` | `connection_id/idempotency_key/attempt_no/provider_request_id` | 记录每次渠道尝试和供应商请求 ID |
| 新增 | `sys_dict_type/sys_dict_data` | 连接平台、能力、身份策略、权威来源、同步/投递/回调状态 | `tenant_id=1`，全部使用 `NOT EXISTS` |
| 新增 | `sys_resource` | 企业协同连接、应用、同步、映射、消息/待办、回调权限 | 菜单与按钮资源使用 `NOT EXISTS`，并显式配置角色授权策略 |
| 新增 | `sys_job_config` | 全量校准、回调处理、消息补偿、待办补偿 Handler | 复用 Job 中心，不新增调度配置表 |

### 5.1 迁移与回滚

1. Flyway 先增加可空字段和新表，不直接删除旧列或旧索引。
2. 应用迁移服务按批次盘点旧配置；确认活动加密 keyId 后，将旧 `client_secret` 加密写入 `LOGIN` 应用配置，再以 `id + 旧值` 比较更新清空旧列。
3. `sys_user_social` 通过 `tenant_id + platform` 关联唯一连接回填 `connection_id`；空租户、重复连接或无法唯一归属的行进入阻塞清单，禁止猜测归属。
4. 所有批次先完整预检，再在 `REQUIRES_NEW` 事务中比较更新；任一冲突回滚当前批次。
5. 清空旧明文后，不允许回滚到只读取旧列的历史二进制。回滚版本必须同时支持新应用配置读取；数据库回滚以停用新连接和恢复兼容版本为主，不恢复明文 Secret。

## 6. 接口变更

### 6.1 管理接口

| 操作 | 接口 | 方法 | 变更内容 |
|------|------|------|----------|
| 新增 | `/system/collaboration/connections/page` | GET | 分页查询当前租户连接，Secret 永不返回 |
| 新增 | `/system/collaboration/connections/:id` | GET | 查询连接、能力和凭据状态摘要 |
| 新增 | `/system/collaboration/connections` | POST/PUT | 创建/修改连接；敏感请求使用 `@ApiDecrypt` |
| 新增 | `/system/collaboration/connections/:id` | DELETE | 逻辑删除未被活动同步/待办引用的连接 |
| 新增 | `/system/collaboration/connections/:id/apps` | GET/POST/PUT | 管理登录、目录、消息、待办应用及 Secret 轮换 |
| 新增 | `/system/collaboration/connections/:id/test` | POST | 按能力执行 Token/读取/测试消息连通验证 |
| 新增 | `/system/collaboration/connections/:id/sync` | POST | 触发全量、部门、成员或标签同步，返回批次 ID |
| 新增 | `/system/collaboration/sync-logs/page` | GET | 查询同步批次和阶段统计 |
| 新增 | `/system/collaboration/sync-issues/page` | GET | 查询冲突/失败问题单 |
| 新增 | `/system/collaboration/sync-issues/:id/resolve` | POST | 执行绑定、忽略、重试等显式处理动作 |
| 新增 | `/system/collaboration/mappings/orgs|users|posts|tags` | GET | 查看外部与 Forge 的映射，不返回敏感资料 |
| 新增 | `/system/collaboration/deliveries/page` | GET | 查询消息和待办投递状态 |
| 新增 | `/system/collaboration/deliveries/:id/retry` | POST | 对失败记录执行权限受控的人工重试 |
| 新增 | `/system/collaboration/callback-events/page` | GET | 查询回调元数据和处理状态，不返回解密正文 |

### 6.2 登录和公开回调

| 操作 | 接口 | 方法 | 变更内容 |
|------|------|------|----------|
| 兼容修改 | `/social/platforms` | GET | 返回公开连接代码和品牌信息；不暴露 tenantId、enterpriseId 或应用 ID |
| 兼容修改 | `/social/authUrl/:connectionCode` | GET | 服务端生成并保存一次性 state，绑定连接、应用、租户和动作 |
| 兼容修改 | `/social/callback` | POST | 校验并消费 state，服务端完成平台登录，只返回短期一次性 `socialTicket` |
| 兼容修改 | `/auth/login` | POST | OAuth2 分支只接受 `socialTicket`；旧 `socialUuid` 等字段不再作为身份依据 |
| 新增 | `/collaboration/callback/:connectionCode/:appCode` | GET/POST | 按连接和物理应用定位回调凭据；企微 URL 校验和事件回调验签解密后快速持久化并应答 |
| 新增 | `/collaboration/todo/entry` | GET | 消费待办入口票据、完成企微身份校验并安全跳转 Forge 待办 |

### 6.3 兼容策略

- 旧 `/system/socialConfig/*` 在一个发布周期内保留只读或重定向能力；新增/修改统一进入新企业协同接口。
- 旧前端在兼容期开启时可以显示登录入口，但服务端不得继续信任客户端提交的第三方身份字段。
- 所有 URL 占位符在 `AiCrudPage` 配置中使用 `:id`，不使用 `{id}`。

## 7. 影响范围

- 后端模块：新增 `forge-starter-collaboration`、`forge-plugin-collaboration`；修改 Social、System、Message、Flow、Job、Crypto、Outbound 和 Admin Server 装配。
- 前端模块：登录页/回调页、现有 `system/socialConfig.vue`、新增企业协同管理工作台及 API。
- 数据库：Social 两张存量表兼容迁移，新增 10 张集成表，扩展 3 张消息表，增加字典、资源和任务配置。
- 运行依赖：MySQL、Redis/Redisson、Forge Job；企微回调还需要公网 HTTPS、可信域名/IP 和稳定证书。
- 外部系统：企业微信通讯录、应用消息、模板卡片、回调和 OAuth API。
- 预计工作量：通用底座与安全迁移 8-12 人日，企微通讯录/登录 10-15 人日，消息 4-6 人日，待办/回调 8-12 人日，管理端/测试/文档 6-9 人日，合计约 36-54 人日，不含客户授权等待、历史脏数据治理和 UAT 等待时间。

## 8. 风险与关注点

> ⚠️ 本变更涉及身份认证、权限、人员停用和流程状态动作，必须完成安全与权限人工审查后方可上线。

| 风险 | 级别 | 控制措施 |
|------|------|----------|
| 客户把登录 Secret、通讯录 Secret、消息应用混为一套 | 高 | 独立应用模型；能力连通测试逐项验收 |
| 旧 OAuth 流程信任前端身份 | Critical | 一次性 state + 服务端票据；前端不再获得或提交可信身份资料 |
| 多企业 `platform + uuid` 冲突 | Critical | 迁移到 `tenant + connection + uuid` 唯一键，歧义数据阻塞迁移 |
| 企微全量拉取中断导致误离职 | Critical | 完整快照成功后才处理未出现记录；失败批次不做停用 |
| 自动匹配错误合并员工 | Critical | 默认禁止手机号/邮箱自动合并，只允许已确认的唯一员工编码策略 |
| Secret/Token 泄露 | Critical | 认证加密/外部引用、掩码、空值保留、日志扫描、旧明文迁移门禁 |
| 外部回调越权审批或重放 | Critical | 验签、解密、时间窗、事件去重、身份映射、Forge 权限和任务状态复核 |
| Flowable 事务中调用外网导致流程阻塞 | 高 | 本地待办投影/回调收件箱，Job 异步出站和补偿 |
| 消息部分失败无法重试 | 高 | 逐接收人投递状态、确定性幂等键、错误分类和补偿 Handler |
| API 限流或 Token 刷新风暴 | 高 | 分页、速率控制、指数退避、Token 失效单次刷新、分布式刷新锁 |
| 超级管理员跨租户读取/重试 | 高 | Mapper XML 显式 `tenantId` 条件，Controller 权限和 Service 归属双校验 |
| 供应商 API/卡片能力与客户许可不一致 | 高 | P0 使用客户测试企业做纵向验证；未通过能力不得写入上线承诺 |
| 飞书/钉钉扩展仍需修改核心 | 中 | Fake Provider 合同测试和编排层平台分支静态扫描作为完成门禁 |

## 8.5 测试策略

- **测试范围**：SPI 合同、密文与迁移、OAuth state/票据、Token 并发、目录快照/增量/冲突、企微 HTTP/回调、消息部分失败、待办状态机、安全跳转、租户/权限、Mapper XML、Flyway、前端管理和真实企微测试企业 UAT。
- **覆盖率目标**：新增通用编排与安全策略行覆盖率不低于 85%，分支覆盖率不低于 75%；身份、回调、同步停用、流程动作和幂等规则全部具备明确 P0 用例。
- **独立 Test Spec**：是，见 `test-spec.md`。
- **上线门禁**：单元/合同/构建通过不等于真实企微验收完成。M1 必须完成 Token、部门、成员、标签、登录和测试消息 E2E；M2 另行完成待办卡片、回调验签和状态联动 E2E。M2 资料或验收未完成不阻塞 M1 独立上线。

## 9. 待澄清与分期输入门禁

> Gate A 只确认本 Proposal 的范围、架构、优先级和安全默认值，不授权修改生产代码。Task 0 纵向验证与下列 M1 输入完成后，通过 Gate B 才进入 `/apply`。M2 输入在开始 Task 14 前确认，不阻塞 M1。

### 9.1 M1 必需输入（Gate B 前完成）

- [ ] **连接和应用资料**：提供测试企业 `corpId`，登录/通讯录/消息应用的 AgentId、Secret、权限截图；推荐按能力独立建应用，确需共用时明确共用范围。
- [ ] **一期回调资料**：如启用通讯录增量事件，提供公网 HTTPS 基础域名、企微可信域名/IP 配置、对应应用 Callback Token、EncodingAESKey、证书和网络白名单；Secret 仅在安全渠道配置，不写入文档或聊天记录。
- [ ] **人员权威规则**：推荐“企微维护姓名、在职状态、部门和标签；Forge 维护角色、权限、岗位和组织内角色”，客户需确认字段覆盖清单。
- [ ] **人员唯一键**：推荐使用客户稳定员工编码；在未提供前禁止按手机号/邮箱自动合并，只允许创建问题单人工绑定。
- [ ] **离职策略**：推荐停用映射；仅对“企微独占托管用户”停用 Forge 账号，手工/多连接用户不自动停用。
- [ ] **部门根节点**：确认企微根部门映射到哪个 Forge `sys_org`，以及 Forge 现有部门是否允许由企微接管。
- [ ] **岗位策略**：推荐企微 position 只做参考，不自动覆盖 `sys_post/sys_user_post`；客户确认是否存在外部岗位编码。
- [ ] **一期规模与 SLA**：提供部门数、员工数、标签数、日消息/目录回调量、同步时效和可接受全量窗口，用于冻结分页、并发、重试和性能验收值。
- [ ] **留存与隐私**：确认同步日志、加密回调正文、投递记录的留存天数，以及姓名/手机号/邮箱在管理页的脱敏要求。

### 9.2 M2 必需输入（Task 14 前完成）

- [ ] **待办应用资料**：提供待办/模板卡片应用的 AgentId、Secret、权限截图、测试用户和卡片更新能力证明。
- [ ] **待办回调资料**：提供该物理应用对应的 Callback Token、EncodingAESKey、回调 URL 配置和网络白名单。
- [ ] **待办交互**：M2 基线为安全打开 Forge 详情；如客户要求简单卡片直批，另行确认流程白名单、动作范围和专项 UAT，不开放复杂表单直批。
- [ ] **待办规模与 SLA**：提供日均待办/回调量、期望投递时效、补偿窗口和告警阈值。

## 10. 技术决策

| 决策 | 选择 | 放弃方案 | 原因 |
|------|------|----------|------|
| 模块边界 | 通用 Starter SPI + Collaboration Plugin 编排 | 全部写进 `forge-starter-social` 或企微专用业务类 | 避免 Starter 依赖 System/Message/Flow，支持后续 Provider 插拔 |
| 连接根 | 兼容升级 `sys_social_config` | 新建完全独立的企微配置表 | 复用现有管理和登录资产，降低迁移断裂 |
| 应用模型 | 连接下按能力独立应用配置 | 一套 `clientId/secret/agentId` 承载全部能力 | 企微实际权限和应用通常分离，后续平台也存在多应用场景 |
| 平台扩展 | Provider Registry + Connector 能力发现 | 业务层 `switch(platform)` | 新平台只实现 Connector，不修改主编排 |
| 目录权威 | 外部只管理映射拥有的数据，Forge 保留 RBAC 权威 | 全量镜像覆盖 Forge 用户/角色 | 避免同步误删和权限扩大 |
| 消息 | 复用消息主表/模板/接收人/记录，增加 `COLLABORATION` 渠道 | 另建企微消息中心 | 保持统一消息生命周期和运维入口 |
| 待办 | Forge 权威任务 + 本地可靠投影 | 企微成为任务主库或监听器同步调外网 | 保证流程状态一致，外网故障不阻断流程事务 |
| 调度 | 复用 Forge Job Handler | 新建同步 cron 配置表 | 统一启停、重试、日志和告警 |
| 凭据 | 扩展 `PersistentCryptoService` 使用版本化 `AES_GCM`，并保留外部 Secret Resolver 扩展点 | 明文、不可逆哈希、另造密文协议 | 调供应商 API 必须取回原文，复用现有 key ring 和迁移能力，认证标签可发现篡改 |
| 登录交换 | 服务端一次性 state + 一次性 socialTicket | 前端持有 `AuthUser` 后自报 uuid | 消除身份伪造、Token 和个人资料暴露 |
| 回调路由 | `connectionCode + appCode` 定位物理应用凭据 | 只按连接选择一套 Callback Token/EncodingAESKey | 同一连接可有通讯录、消息、待办等多个应用，回调凭据不能混用或随机选择 |

## 11. 执行日志

| Task | 状态 | 实际改动文件 | 备注 |
|------|------|--------------|------|
| Research | completed | 本 Spec、`tasks.md`、`test-spec.md`、`execution-log.md` | 已完成代码现状、数据结构、依赖边界和安全风险调查；未修改生产代码 |
| Gate A：Proposal 确认 | pending | - | 用户确认范围、架构、优先级、安全默认值和分期方式；仅允许进入 Task 0，不授权生产代码修改 |
| Gate B：M1 实施门 | pending | - | Task 0 一期能力验证和 9.1 输入完成后，才进入 `/apply` 实施 P0/P1 |
| Gate C：M2 实施门 | pending | - | 9.2 待办资料完成后，才实施 Task 14-15；不阻塞 M1 Review/UAT/上线 |

## 12. 审查结论

- Spec 合规审查：已完成 Proposal 自审；需求、任务、数据、接口和测试均有对应关系。
- 安全/权限专项审查：已完成 Proposal 级审查；已识别 OAuth 客户端身份伪造、Secret 明文、多企业身份冲突、回调重放和外部待办越权等上线门禁，编码后仍需二阶段代码审查。
- 企微真实能力验证：待客户测试企业资料齐备后执行。

## 13. 确认记录（分期门禁）

- **Gate A 确认时间/确认人/内容**：待确认
- **Gate B 确认时间/确认人/内容**：待确认
- **Gate C 确认时间/确认人/内容**：待确认
