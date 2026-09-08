# Forge 企业级框架能力加固 Tasks

> 变更名：`enterprise-framework-hardening`  
> 关联 Spec：`spec.md`  
> 当前状态：进行中  
> 执行规则：每项任务按“测试 → 实现 → 目标验证 → 人工审查 → 记录”执行；不修改低代码/generator。

## 执行状态

- [~] Phase 0：凭据止血与边界冻结
- [~] Phase 1：安全基线与 Web 防护
- [~] Phase 2：身份认证与账号安全
- [~] Phase 3：字段加密、脱敏与密钥治理
- [~] Phase 4：API 可靠性与消息一致性
- [~] Phase 5：可观测性、数据治理与灾备
- [ ] Phase 6：CI/CD、集成测试与 Docker 收敛
- [ ] Phase 7：灰度验收与回滚演练

## Phase 0：凭据止血与边界冻结（P0）

### T0.1 轮换并清理已暴露凭据

- **范围**：`forge-server/**/application-dev.yml`、部署脚本、Docker 示例、Secret scan 配置。
- **前置**：列出所有受影响数据库、Redis、外部系统和 SSH 凭据；建立轮换窗口。
- **步骤**：立即轮换真实凭据；改为环境变量/Secret Manager/受控文件；补 `.example` 模板；扫描 Git 工作树和历史引用；记录不可重写历史的补救措施。
- **验收**：扫描无真实 Secret；缺少必需 Secret 时应用启动失败；旧凭据全部失效。
- **风险/回滚**：连接中断时恢复上一版本 Secret 并重试；禁止把旧明文重新提交。
- **人工审查**：安全负责人和运维负责人必须确认轮换完成。

**本轮状态**：配置文件和部署脚本已完成清理；真实凭据轮换和历史仓库处置仍待运维执行。

### T0.2 冻结范围、基线与依赖版本

- **范围**：变更目录、模块清单、当前分支、Spring Boot/Maven/Node 文档。
- **前置**：读取 `automated-testing-standard.md`，保存初始 `git diff --name-only` 和构建基线。
- **验收**：变更清单明确排除 `forge-plugin-generator`、lowcode 和表单设计器；版本文档与根 POM 一致。
- **回滚**：仅回退本任务文档，不改变业务代码。

## Phase 1：安全基线与 Web 防护（P0）

### T1.1 服务端验证码挑战

- **范围**：认证插件验证码生成/校验、Redis/缓存、Admin 登录请求。
- **实现**：challenge 绑定会话/IP 摘要和过期时间，一次性消费，服务端签名校验；删除对客户端 `verified` 字段的信任。
- **测试**：伪造 verified、过期、重复消费、跨会话、并发消费和错误 challenge。
- **验收**：只有有效 challenge 可通过；失败不泄露账号状态；关键事件有审计。
- **风险/回滚**：旧前端灰度期间提供兼容版本接口，但不得放开无 challenge 路径。
- **人工审查**：认证和风控规则。

**本轮状态**：滑块登录已改为提交服务端缓存校验所需的 `codeKey + moveX`，客户端 `verified` 标记不再被信任；已增加 Redis 一次性消费标记，并将 challenge 保存为 HMAC 签名的版本化载荷，绑定服务端解析的远端地址、User-Agent 和会话标识，错误答案也会消费挑战。生产或混合生产 Profile 必须注入至少 32 字节的 `FORGE_CAPTCHA_CHALLENGE_SECRET`，纯开发 Profile 才允许进程内临时密钥；签名、跨请求绑定、篡改载荷和密钥策略测试已编译，执行受本机 Mockito inline Byte Buddy self-attach 限制。

### T1.2 请求、响应和浏览器安全策略

- **范围**：`forge-starter-web`、各服务 `application.yml`、Nginx/TLS、CORS/CSRF。
- **实现**：设置请求体、Header、参数和上传上限；增加安全响应头、TLS 最低版本、生产 Origin 白名单和 CSRF 策略；修复 AI 日志默认暴露。
- **测试**：超限请求、恶意 Origin、跨站请求、压缩包/大 Header 和响应头合同。
- **验收**：超限返回明确 `413/400`；生产不允许 HTTP 明文和无限体积；prompt/completion 不出现在普通日志。
- **风险/回滚**：按环境灰度调整大小和 Origin 白名单；保留审计配置版本。

**本轮状态**：各服务请求体上限和 AI prompt/completion 日志默认已收敛；核心 starter 新增安全响应头过滤器（nosniff、SAMEORIGIN、Referrer-Policy、Permissions-Policy、CSP，HTTPS 请求追加 HSTS）。CORS/CSRF、TLS 和生产 Origin 白名单仍需按部署拓扑配置。

认证和 OAuth token/revoke 路径响应同时设置 `Cache-Control: no-store` 和 `Pragma: no-cache`，避免登录、验证码和令牌响应被浏览器或中间代理缓存。

四个服务的 `server.error` 默认关闭 message、binding errors、stacktrace 和 exception 回显，覆盖未命中统一异常处理器的容器错误响应。

### T1.3 Actuator 与安全配置暴露

- **范围**：各服务 Actuator exposure、健康组、管理端点鉴权。
- **实现**：匿名仅健康探针；指标、env、loggers、threaddump 等需要管理员和网络隔离；敏感值统一脱敏。
- **测试**：匿名、普通用户、管理员和内网探针访问矩阵。
- **验收**：端点暴露最小化，未授权返回正确 HTTP 状态码。

**本轮状态**：Admin/App 已限制 Actuator Web 暴露为 `health,metrics` 且隐藏健康详情；认证拦截器仅放行健康探针，metrics 和其他管理端点仍需鉴权。Flow/Report 已声明仅 health 暴露，但当前未引入 Actuator 依赖，待按部署拓扑决定是否启用管理端口。

## Phase 2：身份认证与账号安全（P0/P1）

### T2.1 MFA 与恢复流程

- **范围**：`forge-starter-auth`、系统用户、登录 UI、Redis 会话。
- **实现**：TOTP、一次性恢复码、管理员强制 MFA、绑定/解绑二次认证、审计和限时应急豁免；预留 WebAuthn SPI。
- **测试**：注册、登录、错误码、重放、恢复码消费、解绑、并发设备和租户隔离。
- **验收**：管理员无 MFA 不可进入管理面；恢复码不可复用；所有敏感操作可审计。
- **人工审查**：安全负责人确认恢复策略和应急账号。

### T2.2 密码与登录风控

- **范围**：用户密码服务、登录策略、登录/重置 UI。
- **实现**：复杂度、历史密码、过期、锁定、泄露密码检查、IP/账号/租户/设备限流；重置流程必须使用已启用的短信/邮件通道并统一文案。
- **测试**：弱密码、历史复用、过期、爆破、账号枚举、验证码失败和通道未启用。
- **验收**：默认账号首次登录改密；错误响应不暴露账号存在性。

### T2.3 会话、设备和企业目录

- **范围**：Sa-Token 会话、前端 auth store、LDAP/AD/OIDC/SAML SPI。
- **实现**：短期 access token、刷新轮换、撤销黑名单、设备管理、单设备/全端退出；外部身份映射租户与最小角色。
- **测试**：token 重放、刷新轮换、撤销传播、并发登录、目录断连降级和租户映射。
- **风险/回滚**：保留兼容 token 窗口但不延长长期有效期；目录切换可回到本地认证。

**本轮状态**：四个服务统一设置环境变量可调的短期 token（默认 30 分钟）和活跃超时（默认 2 小时），关闭 token 共享和 Cookie 读取，继续使用 Authorization Header；刷新轮换、撤销黑名单、设备管理和企业目录 SPI 仍待实施。

## Phase 3：字段加密、脱敏与密钥治理（P0/P1）

### T3.1 统一持久化字段加密边界

- **范围**：`forge-starter-crypto`、ORM/Mapper、受保护业务模块（不含 generator/lowcode）、导出和缓存适配器。
- **实现**：定义字段策略注册表、用途 keyId、算法、AAD/tenant 绑定、密文长度和可检索合同；持久化读写统一调用 `PersistentCryptoService`；`@CryptoField` 仅保留为 API 兼容层。
- **测试**：新增/修改/批量/事务回滚、旧 FPC1/legacy 密文、历史 key、错误 key、损坏密文、长度和租户 AAD。
- **验收**：任何加解密异常失败关闭，不返回或保存原文；Mapper、导出、缓存路径均遵守同一策略。
- **风险/回滚**：先兼容读取再切换新写入；迁移失败保留原值并可重跑。
- **人工审查**：字段目录、算法、检索需求、数据迁移和旧 key 退役。

### T3.2 修复 CryptoField 失败开放与协议缺陷

- **范围**：`CryptoFieldSerializer`、`CryptoFieldDeserializer`、`CryptoProperties`、错误映射和日志。
- **实现**：删除异常返回原值逻辑；增加密文版本/策略标识和字段用途校验；日志仅输出字段元数据和摘要。
- **测试**：工厂缺 Bean、禁用开关、算法非法、密钥缺失、序列化/反序列化异常。
- **验收**：异常统一为安全错误；禁用仅在明确开发配置生效，生产不能静默明文。

**本轮状态**：已删除加解密异常的原文回退；脱敏异常改为固定掩码。字段用途/AAD/策略注册表尚未实现。

### T3.3 脱敏策略中心与全链路防泄漏

- **范围**：`DesensitizeSerializer`、策略工厂、全局 Jackson、日志/异常/导出/消息/WebSocket/审计扩展点。
- **实现**：集中 PII 字段目录与策略版本；未登记敏感字段生产默认拒绝或固定掩码；脱敏异常固定掩码；支持角色/租户/用途条件显示和二次认证审计。
- **测试**：各内置策略、Unicode、空值、异常策略、未标注字段、Map/VO/导出/日志/消息路径。
- **验收**：任一已登记 PII 在所有出口不出现原文；明文查看有权限、原因、审批号和审计记录。
- **人工审查**：个人信息保护负责人确认字段目录和显示规则。

**本轮状态**：脱敏异常和策略未注册均固定输出 `***`，已消除该序列化路径的失败开放；PII 字段目录、日志/导出/消息全链路覆盖和权限条件显示仍待实施。

### T3.4 密钥轮换、盘点与迁移门禁

- **范围**：`CryptoConfigurationValidator`、`PersistentCryptoService`、迁移服务、配置 API 和 Runbook。
- **实现**：活动/历史 key 双读、轮换预检、dry-run/execute、批量原值比较更新、密文计数、失败重试和退役门禁；配置管理 API 永不返回密钥。
- **测试**：多实例兼容、并发迁移、部分失败、重跑、未知 keyId、租户隔离和回滚。
- **验收**：旧格式/未知格式/缺 key/失败数均为 0 前不得退役旧 key；响应和日志无秘密值。

## Phase 4：API 可靠性与消息一致性（P1）

### T4.1 限流、配额、超时、熔断和舱壁

- **范围**：Web starter、Redis/网关、外部客户端、普通业务 API。
- **实现**：用户/IP/租户/接口维度限流和配额；统一连接/读取超时默认 3 秒；按幂等性重试、熔断、舱壁和降级；暴露指标。
- **测试**：突发流量、租户隔离、Redis 故障、外部超时、重复请求和恢复。
- **验收**：超限 `429`，超时/熔断错误可识别且不占满线程池。

**本轮状态**：受控出站 HTTP 客户端在调用方未显式提供 `X-Request-Id` 时，自动从 MDC 透传经过校验的请求关联 ID，保持跨服务排障链路；并按业务场景增加默认 32 并发舱壁和连续 5 次网络/超时失败、30 秒自动恢复的熔断，舱壁耗尽和熔断打开快速失败。普通 API 已由独立 MVC 拦截器接入基于 `limit_flag` 的 Redis 分布式限流，按匹配的配置模板、方法及用户/租户或远端地址生成限流桶，OPTIONS 不扣额度，默认强制，超限返回 `429`、Redis 不可用返回 `503`；显式 `forge.api-config.rate-limit.mode=observe` 时仅记录并放行。配额、动态策略和完整熔断指标仍待实施。

### T4.2 HTTP 错误、API 配置缓存和版本治理

- **范围**：`GlobalExceptionHandler`、`ApiConfigRefreshListener`、`ApiConfigManagerImpl`、接口文档。
- **实现**：补齐 HTTP 状态码映射；修复 MODULE 刷新和 `allEnabledConfigsCache` 同步；增加配置版本、审计和回滚；定义 API 版本/弃用窗口。
- **测试**：校验/认证/授权/限流/冲突/系统异常和多实例刷新一致性。
- **验收**：状态码与错误码语义一致；刷新后所有缓存视图一致。

**本轮状态**：已完成 MODULE 刷新入口、启用配置路径索引重载，并补齐全局异常处理器的 HTTP 状态码映射（含 413 上传超限）；配置版本和回滚仍待实施。

限流错误为 `429` 时，统一异常处理器现在补充 `Retry-After: 60` 响应头（调用方已有同名头时保留）。

### T4.3 Outbox、死信和补偿

- **范围**：消息插件、事务 starter、Flow/App/Report 跨服务调用。
- **实现**：本地事务写 Outbox，异步投递、幂等消费、重试退避、死信、人工重放和状态审计；移除事务内长 RPC。
- **测试**：提交后宕机、重复投递、部分接收人失败、死信重放和跨租户隔离。
- **验收**：消息最终状态可追踪；失败不丢失、不重复造成业务副作用。
- **人工审查**：状态机和补偿策略。

## Phase 5：可观测性、数据治理与灾备（P1）

### T5.1 Metrics、Tracing、日志和 SLO

- **范围**：各 Spring Boot 服务、前端错误上报、日志采集配置。
- **实现**：Micrometer/Prometheus、OpenTelemetry、结构化日志、采样和敏感字段过滤；定义核心 SLO 和告警 runbook。
- **测试**：指标标签基数、Trace 传播、采样、日志脱敏和告警触发。
- **验收**：能按 traceId 关联请求；任何 Token、密码、PII、prompt 原文不进入指标/Trace/日志。

**本轮状态**：Admin/App/Flow/Report 均暴露需认证的 Actuator `metrics`，健康探针仍单独匿名可用；完整 Prometheus/OpenTelemetry 接入、结构化日志和 SLO 告警仍待实施。

核心 starter 已增加请求关联过滤器：校验并透传安全的 `X-Request-Id`，无效值自动生成 UUID，将值同时写入日志使用的 `requestId`/`traceId` MDC 键并在请求结束恢复线程上下文；四个服务的请求头上限默认为 16KB 且支持环境变量调整。

四个服务均配置 liveness/readiness 健康组，仅纳入 Spring Boot 状态探针，不改变匿名健康详情隐藏策略。

### T5.2 备份、恢复、PITR 和保留策略

- **范围**：`forge-server/db/backup`、数据库/Redis/对象存储运维脚本和文档。
- **实现**：加密备份、跨区保留、PITR、恢复校验、RPO/RTO 记录和季度演练；定义日志、审计、PII 删除与法律留存策略。
- **测试**：隔离环境全量恢复、增量恢复、时间点恢复、校验失败和密钥不可用。
- **验收**：恢复结果满足目标并有证据；备份中敏感字段仍受加密和访问控制保护。
- **人工审查**：运维、安全和数据负责人签字。

### T5.3 文件安全与配额

- **范围**：`forge-starter-file`、文件元数据、下载审计、对象存储配置。
- **实现**：MIME/扩展名校验、病毒扫描、压缩包炸弹防护、租户配额、隔离区、下载审计和符号链接/路径穿越防护。
- **测试**：恶意 MIME、双扩展、Zip bomb、超额、并发上传、路径穿越、符号链接和未授权下载。
- **验收**：恶意或越权文件不可落入正式存储；删除和下载行为可审计。

**本轮状态**：通用文件下载、访问 URL、Base64、元数据读取和秒传复用现在对 `isPrivate=true` 的元数据统一调用 `FileMetadataPersistence.checkPermission(fileId, SessionHelper.getUserId())`，无权访问返回 403，过期文件返回 410；私有下载及访问 URL 响应设置 `Cache-Control: private, no-store` 与 `Pragma: no-cache`。本地存储已有路径穿越、符号链接和安全业务目录校验，上传已有扩展名/MIME/大小上限。病毒扫描、压缩包炸弹检测、租户配额、隔离区和下载审计仍待后续实现。

## Phase 6：CI/CD、集成测试与 Docker 收敛（P1）

### T6.1 质量门禁与真实集成测试

- **范围**：根 `pom.xml`、CI 配置、`forge-server` 测试目录、Admin/H5 package scripts。
- **实现**：默认关闭 `forge.compiler.skip`/`forge.tests.skip`；增加 Testcontainers 或等价 MySQL/Redis 集成测试；接入 SAST、SCA、Secret scan、SBOM 和镜像扫描。
- **测试**：干净环境 compile/test/package、依赖漏洞阻断、Secret 阻断、集成测试失败传播。
- **验收**：任何编译/测试失败都会阻断合并；测试报告和扫描证据可追溯。

**本轮状态**：根 POM 已将编译和测试 skip 默认关闭；Admin 聚合 package 被既有 generator 测试源码错误阻断，未修改排除范围，真实 MySQL/Redis 集成测试和 CI 扫描仍待后续。

### T6.2 Docker 与生产运行模型收敛

- **范围**：`docker/`、`docker-forge-admin/`、Dockerfile、Compose、Nginx、健康检查。
- **实现**：统一镜像构建上下文和模块路径；非 root 运行；移除默认 root/弱密码；TLS、资源限制、健康检查、只读文件系统和 Secret 挂载。
- **测试**：compose config、镜像扫描、非 root 写入、健康检查、滚动升级和回滚。
- **验收**：两套定义无路径/模块漂移；生产配置缺失 Secret 时拒绝启动。

## Phase 7：灰度验收与回滚演练（P0/P1）

### T7.1 分阶段灰度

- **顺序**：内部环境 → 单租户 → 低风险租户 → 全量；每阶段记录错误率、延迟、认证失败、加解密失败、队列积压和告警。
- **门禁**：无未解释的安全告警、数据泄漏、状态不一致或恢复失败才能进入下一阶段。

### T7.2 回滚与人工签收

- **内容**：Secret 回退、应用版本回退、配置版本回退、密文迁移暂停/重跑、旧 key 保留、MFA 应急恢复、消息死信重放、数据库 PITR 和 Docker 回滚。
- **验收**：每条回滚路径在隔离环境至少演练一次；安全、运维、数据负责人完成签收。

## 任务完成定义

- 代码、配置、迁移和文档均与 Spec 一致，未触碰排除范围。
- 目标测试、集成测试、构建、扫描和部署验证通过。
- `test-spec.md` 与 `execution-log.md` 记录命令、结果、警告、跳过项和清理情况。
- 涉及凭据、权限、状态流转、密文迁移、备份恢复和生产部署的人工审查记录齐全。
