# 定时任务出站安全 Implementation Plan

> **For agentic workers:** 本计划按仓库 `code-copilot` 工作流在当前会话内联执行；逐项使用复选框追踪，禁止回退 V1-V7 工作区成果。

> status: complete
> scope: V8 only

**Goal:** 建立平台级受控出站 HTTP 能力，并让 Flow Webhook 不再通过直连客户端形成 SSRF 旁路。

**Architecture:** `forge-starter-outbound` 持有白名单模型、策略接口、DNS/IP 校验和 OkHttp 受控客户端；`forge-plugin-system` 提供平台管理员维护 API；`forge-plugin-flow` 只依赖 `SecureOutboundClient`。连接前的实际 DNS 回调再次解析并验证全部地址，重定向逐跳重新走完整策略。

**Tech Stack:** Java 17、Spring Boot 3、MyBatis-Plus/Mapper XML、OkHttp 4、Sa-Token、Flyway、JUnit 5、Mockito、MockWebServer。

---

## Task 1: Spec 与测试基线

**Files:**
- Modify: `code-copilot/changes/定时任务出站安全/spec.md`
- Modify: `code-copilot/changes/定时任务出站安全/tasks.md`
- Create: `code-copilot/changes/定时任务出站安全/test-spec.md`
- Create: `code-copilot/changes/定时任务出站安全/execution-log.md`
- Create: `code-copilot/changes/定时任务出站安全/outbound-callsite-audit.md`

- [x] 修正 V8 scope，确认三个安全门禁和失败关闭边界。
- [x] 固化 Starter、系统管理 API、Flow 接入和增量验证矩阵。
- [x] 建立直接出站调用点清单并在实施后标记治理状态。

## Task 2: Starter 骨架与白名单持久化

**Files:**
- Create: `forge-server/forge-framework/forge-starter-parent/forge-starter-outbound/pom.xml`
- Modify: `forge-server/forge-framework/forge-starter-parent/pom.xml`
- Create: `forge-starter-outbound/src/main/java/com/mdframe/forge/starter/outbound/config/OutboundAutoConfiguration.java`
- Create: `forge-starter-outbound/src/main/java/com/mdframe/forge/starter/outbound/config/OutboundProperties.java`
- Create: `forge-starter-outbound/src/main/java/com/mdframe/forge/starter/outbound/domain/entity/SysOutboundWhitelist.java`
- Create: `forge-starter-outbound/src/main/java/com/mdframe/forge/starter/outbound/mapper/SysOutboundWhitelistMapper.java`
- Create: `forge-starter-outbound/src/main/resources/mapper/SysOutboundWhitelistMapper.xml`
- Create: `forge-server/db/migration/V1.0.47__add_outbound_security_policy.sql`
- Test: `forge-starter-outbound/src/test/java/com/mdframe/forge/starter/outbound/migration/OutboundMigrationContractTest.java`

- [x] 先写迁移合约测试，覆盖标准审计字段、`tenant_id=1`、逻辑删除生成列、JOB_WEBHOOK 私网约束、权限资源和无角色自动授权。
- [x] 创建 Starter 并注册到 Reactor，配置协议、超时、请求/响应上限、重定向开关和最大跳数。
- [x] 创建白名单表、实体和 XML Mapper；运行时查询显式使用可信默认租户并过滤 `del_flag=0`、`status=1`。
- [x] 运行 Starter 测试，确认迁移合约由失败转为通过。

## Task 3: 平台管理员白名单管理

**Files:**
- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/pom.xml`
- Create: `forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/controller/OutboundWhitelistController.java`
- Create: `forge-starter-outbound/src/main/java/com/mdframe/forge/starter/outbound/domain/dto/OutboundWhitelistQuery.java`
- Create: `forge-starter-outbound/src/main/java/com/mdframe/forge/starter/outbound/domain/dto/OutboundWhitelistSaveRequest.java`
- Create: `forge-starter-outbound/src/main/java/com/mdframe/forge/starter/outbound/service/OutboundWhitelistService.java`
- Create: `forge-starter-outbound/src/main/java/com/mdframe/forge/starter/outbound/service/impl/OutboundWhitelistServiceImpl.java`
- Test: `forge-starter-outbound/src/test/java/com/mdframe/forge/starter/outbound/service/OutboundWhitelistServiceTest.java`

- [x] 先写规范化、端口范围、重叠规则、JOB_WEBHOOK 私网拒绝和 FLOW_API 私网例外测试。
- [x] 实现 XML 分页、精确规则匹配和重叠检查；新增、修改、删除使用逻辑删除并固定平台租户。
- [x] 管理 Controller 同时执行 `SessionHelper.assertAdmin`、`@SaCheckPermission`、`@ApiEncrypt/@ApiDecrypt` 和写操作审计。
- [x] 运行服务测试并扫描权限资源，确认普通角色没有自动获得白名单权限。

## Task 4: URL、IP 与 DNS 策略

**Files:**
- Create: `forge-starter-outbound/src/main/java/com/mdframe/forge/starter/outbound/security/OutboundPolicyService.java`
- Create: `forge-starter-outbound/src/main/java/com/mdframe/forge/starter/outbound/security/DefaultOutboundPolicyService.java`
- Create: `forge-starter-outbound/src/main/java/com/mdframe/forge/starter/outbound/security/IpAddressClassifier.java`
- Create: `forge-starter-outbound/src/main/java/com/mdframe/forge/starter/outbound/security/OutboundDnsResolver.java`
- Create: `forge-starter-outbound/src/main/java/com/mdframe/forge/starter/outbound/security/SystemOutboundDnsResolver.java`
- Create: `forge-starter-outbound/src/main/java/com/mdframe/forge/starter/outbound/security/OutboundSecurityException.java`
- Create: `forge-starter-outbound/src/main/java/com/mdframe/forge/starter/outbound/support/OutboundHostNormalizer.java`
- Test: `forge-starter-outbound/src/test/java/com/mdframe/forge/starter/outbound/security/IpAddressClassifierTest.java`
- Test: `forge-starter-outbound/src/test/java/com/mdframe/forge/starter/outbound/security/DefaultOutboundPolicyServiceTest.java`

- [x] 先写 HTTP/HTTPS、userinfo、非法端口、编码 authority、IPv4/IPv6/mapped/保留地址和混合 DNS 失败测试。
- [x] 实现精确主机规范化、白名单匹配、三类 IP 分类和全部 A/AAAA 地址校验。
- [x] 策略首次校验和实际连接 DNS 回调均解析地址；任何混合非法结果整体失败关闭。
- [x] 运行策略测试，确认 127.0.0.1、::1、169.254.169.254、RFC1918、ULA 和保留网段边界。

## Task 5: 受控 HTTP 客户端

**Files:**
- Create: `forge-starter-outbound/src/main/java/com/mdframe/forge/starter/outbound/client/SecureOutboundClient.java`
- Create: `forge-starter-outbound/src/main/java/com/mdframe/forge/starter/outbound/client/OkHttpSecureOutboundClient.java`
- Create: `forge-starter-outbound/src/main/java/com/mdframe/forge/starter/outbound/model/OutboundRequest.java`
- Create: `forge-starter-outbound/src/main/java/com/mdframe/forge/starter/outbound/model/OutboundResponse.java`
- Create: `forge-starter-outbound/src/main/java/com/mdframe/forge/starter/outbound/model/OutboundRequestContext.java`
- Create: `forge-starter-outbound/src/main/java/com/mdframe/forge/starter/outbound/model/ValidatedOutboundTarget.java`
- Test: `forge-starter-outbound/src/test/java/com/mdframe/forge/starter/outbound/client/OkHttpSecureOutboundClientTest.java`

- [x] 先写 DNS 结果变化、自动重定向阻断、逐跳校验、超大请求/响应、超时和危险 Header 测试。
- [x] 实现自定义 OkHttp DNS、关闭自动重定向/自动重试/连接复用，并为每一跳应用整体剩余超时。
- [x] 响应按字节上限流式读取；跨源重定向裁剪凭据 Header；日志只记录安全目标摘要和状态。
- [x] 运行客户端测试，确认 DNS 重绑定模拟在连接前被阻断。

## Task 6: Flow Webhook 接入与旁路审计

**Files:**
- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/pom.xml`
- Modify: `forge-plugin-flow/src/main/java/com/mdframe/forge/starter/flow/event/FlowWebhookNotifier.java`
- Test: `forge-plugin-flow/src/test/java/com/mdframe/forge/starter/flow/event/FlowWebhookNotifierTest.java`
- Modify: `code-copilot/changes/定时任务出站安全/outbound-callsite-audit.md`

- [x] 先写 Flow Webhook 使用 FLOW_API、失败关闭、无 `X-Inner-Call` 和不记录完整 URL/响应正文的测试。
- [x] 用 `SecureOutboundClient` 替换直连 `RestTemplate`，保留受控重试且只接受 2xx。
- [x] 扫描仓库直接 HTTP 调用，记录已治理、禁用预留和需后续独立迁移的调用点。
- [x] 运行 Flow 目标测试并确认生产源码不再出现 Flow Webhook 直连客户端。

## Task 7: 聚合验证与回填

- [x] 运行 Starter 与 Flow 测试，记录用例数量和预期故障日志。
- [x] 运行 `forge-flow-server` 与 `forge-admin-server` 聚合构建，验证两种装配路径。
- [x] 运行 Flyway placeholder、Mapper XML、直接 HTTP 旁路、敏感日志和 `git diff --check` 静态检查。
- [x] 回填 V8 `execution-log.md`、`test-spec.md`、`tasks.md`、`spec.md` 和父路线图；真实 Flyway/外部网络 E2E 标记为用户侧验收。
