# Framework Security Gap Hardening Spec

## 1. 状态

- 变更名：`framework-security-gap-hardening`
- 当前阶段：`review-ready`
- 发起日期：2026-08-02
- 用户授权：用户明确要求按优先级自动修复已核验问题，并要求不得影响正在并行修改的统一能力开放平台代码。

## 2. 背景

源码审计确认框架存在内部调用标记可伪造、WebSocket 未鉴权、租户上下文缺失时 SQL 隔离失败开放、本地文件路径未约束、防重放 nonce 非原子、登录密码加密失败降级明文等问题。另有数据权限错误放行、固定 Job Token pepper、消息部分成功状态不准确及若干低优先级技术债。

本变更按风险优先级分阶段加固，先关闭可直接跨越安全边界的入口，再处理兼容性和可观测性问题。

## 3. 目标

1. `X-Inner-Call` 只有来自显式可信源地址的请求才可跳过应用层加解密与防重放。
2. WebSocket STOMP `CONNECT` 必须校验 Sa-Token，服务端会话绑定真实用户，客户端不得向 broker topic/queue 直接发送消息。
3. 对需要租户隔离的表，缺失租户上下文默认失败关闭；显式忽略表和 `@IgnoreTenant` 场景保持可用。
4. 本地文件上传、下载、删除、分片和 bucket 操作都限制在规范化基础目录内，并拒绝路径穿越和符号链接逃逸。
5. nonce 通过原子 `set-if-absent` 登记，TTL 至少为时间窗口两倍。
6. 启用登录密码加密时，加密或解密失败必须拒绝登录，不再降级明文。
7. 已配置 DataScope 的查询在上下文获取或 SQL 改写失败时失败关闭；未配置 Mapper 至少提供去重告警和可选严格拒绝策略。
8. Job 开放 API 默认关闭且不再提供仓库固定 pepper；显式启用时缺少合规 pepper 必须启动失败。
9. 完成不改变协议的低风险修复：算法自适配自动密钥长度、稳定 JSON 幂等键、消息部分成功状态。
10. 对无法在缺少产品规则时安全补齐的 WorkerId 回收和流程/AI 未实现能力，采用可验证的安全收口，不伪造完成结果。

## 4. 非目标与并行开发隔离

本变更不修改：

- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-capability-*`
- `forge-admin-ui/src/views/ai/capability/client.vue`
- 当前工作树中由并行 Agent 修改的 `TenantInterceptor.java`
- 当前工作树中由并行 Agent 修改的 `LogProperties.java`、`OperationLogAspect.java`
- 统一能力开放平台的协议、Token、授权、目录解析和 UI 行为

本变更不使用 `git switch`、不自动提交，避免改变并行 Agent 所在工作区的 HEAD。最终通过 `git diff --name-only` 证明未触碰上述边界。

## 5. 设计

### 5.1 内部调用边界

在 Crypto Starter 新增独立配置 `forge.crypto.internal-call.trusted-addresses`，默认只信任 IPv4/IPv6 loopback。校验仅使用 Servlet 容器提供的 `remoteAddr`，不信任可由客户端伪造的转发头。支持显式 IP 和 CIDR；生产容器或网关地址必须通过部署配置加入白名单。

三处加解密/防重放跳过逻辑统一调用同一个验证器。只有 Header 值为 `true` 且来源匹配白名单时才视为内部调用；伪造 Header 按普通外部请求处理并记录安全告警。

### 5.2 WebSocket 鉴权与消息隔离

WebSocket Starter 定义认证 SPI 和 STOMP 入站拦截器；Auth Starter 提供 Sa-Token 实现，避免 websocket/auth Maven 循环依赖。CONNECT 从 STOMP Header 读取 Bearer Token，认证成功后把 loginId 写入 `Principal`。

SUBSCRIBE/SEND 必须有已认证 Principal；客户端 SEND 只允许 `/app/**`，禁止直发 `/topic/**`、`/queue/**`、`/user/**`。认证类通知改为服务端 `convertAndSendToUser`，前端订阅 `/user/queue/messages`，不再广播后由浏览器按 userId 过滤。

允许 Origin 默认限制为 localhost 开发地址；生产通过 `forge.websocket.allowed-origin-patterns` 显式配置。

### 5.3 租户失败关闭

复用已有 `forge.tenant.strict-mode`，默认改为 `true`。处理顺序为：显式上下文忽略、已知非租户表、自动检测非租户表、租户上下文校验。只有确实需要租户列的表在 tenantId 缺失时抛异常；后台全局任务必须使用现有 `TenantContextHolder.executeIgnore` 或 `@IgnoreTenant`。

### 5.4 文件路径约束

初始化时将基础目录转为绝对规范路径。所有相对路径通过统一 resolver 解析，拒绝绝对路径、`..` 逃逸、非法 businessType/bucketName 和路径中的符号链接。元数据中的历史路径同样重新校验，避免数据库污染后扩大为任意文件读删。

### 5.5 防重放

`ReplayTokenCache` 提供原子 `markIfAbsent`，底层使用 Redisson bucket `setIfAbsent`；Filter 只在登记成功后放行。TTL 使用 `replayTimeWindow * 2` 并检查溢出和非法窗口。

### 5.6 兼容性加固

- 密码：前端加密失败抛错；后端存在 RSA 密钥时解密失败抛业务异常。
- DataScope：配置存在时任何上下文/改写异常都拒绝；未配置策略支持 `WARN` 和 `DENY`，默认 `WARN` 保留兼容，生产可切换 `DENY`。
- Job Open API：Java 与 YAML 默认关闭，pepper 无默认值；启用时配置 Bean 阶段校验。
- 自动密钥：SM4 生成 16 字节，AES/AES_GCM 生成 32 字节，校验与算法一致。
- 幂等键：无 SpEL 时使用稳定 JSON 序列化参数后摘要。
- 消息：部分接收人失败时主消息和发送记录使用状态 `3=部分成功`，字典迁移同步增加状态。
- H5 构建：补齐构建配置直接依赖的 `glob`/`sass`，并将旧 Glob 默认导入迁移到仓库已使用的 Glob 11 API，保证登录安全改动可在干净环境构建。

## 6. 风险与回滚

1. 内部服务若非 loopback 调用，需要先配置真实来源 IP/CIDR，否则明文内部调用会被当作外部加密请求拒绝。回滚可临时加入精确可信网段，不允许恢复全网信任。
2. 租户严格模式可能暴露未正确建立上下文的后台任务。修复方式是补充明确的 tenantId 或 `executeIgnore`，不允许恢复默认 fail-open。
3. WebSocket 旧客户端若未在 CONNECT Header 携带 Token 将连接失败。Admin UI 与本变更同步升级。
4. Job Open API 未配置环境变量时将保持关闭；启用前必须注入 pepper。
5. DataScope 未配置默认保持告警，避免一次性阻断全量 Mapper；生产启用 `DENY` 前需先完成配置盘点。

## 7. 验收标准

1. 外部地址携带 `X-Inner-Call: true` 不再触发任何跳过路径，loopback/显式可信 CIDR 可通过。
2. 无 Token、无效 Token、客户端直发 broker 的 STOMP 帧被拒绝；有效 Token 绑定 Principal 并只能收到自己的用户队列消息。
3. tenantId 缺失访问租户表抛异常，忽略表与显式 ignore 正常。
4. `../`、绝对路径、符号链接和恶意 bucket/businessType 均无法越出基础目录。
5. 并发相同 nonce 只有一次成功，TTL 为窗口两倍。
6. 密码加密失败不会发出登录请求，服务端不接受加密模式下的明文降级。
7. 目标模块单测通过，Admin UI 构建通过，Admin 聚合 package 通过。
8. capability 相关文件的工作树 diff 与本变更开始时相比没有由本变更产生的修改。

## 8. HARD-GATE

本变更涉及认证、权限和租户隔离。用户于 2026-08-02 明确要求“按照优先级自动修复这些问题”，并要求不影响正在并行修改的统一能力开放平台代码，视为本 Spec 的实施授权。真实生产网段、Origin、pepper 和部署回归仍是上线门禁。

## 9. 实施结论与剩余项

已完成内部调用来源校验、WebSocket 鉴权、租户上下文失败关闭、文件路径约束、nonce 原子登记、密码失败关闭、DataScope 可选严格策略、Job Open API fail-closed、密钥生成、幂等键、消息部分成功状态、WorkerId 容量门禁和未实现能力安全收口。

以下事项未做破坏性自动改造：

1. `TenantInterceptor` 属并行 Agent 冻结文件；其中无 auth 的 Header 回退在当前必选 Core 依赖图下不可达，超管跨租户语义仍需“目标租户 + 审计”产品规则后单独实施。
2. DataScope 未配置 Mapper 默认采用去重 `WARN` 保持兼容，生产完成配置盘点后应设置 `forge.datascope.unconfigured-policy=DENY`。
3. 自定义 XML 的租户条件仍由 MyBatis-Plus TenantLine 拦截器处理；`del_flag` 全库 AST/CI 审计需单独变更，不能无差别改写历史 SQL。
4. WorkerId 已做 80% 告警和越界拒绝；无租约模型前不实现可能产生重复 ID 的回收复用。
5. `ExternalProxyServiceImpl.trustedInternal` 仍依赖受控配置；入站服务已增加直接对端白名单，但网关仍必须剥离外部 `X-Inner-Call` Header。
