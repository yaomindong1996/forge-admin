# enterprise-framework-hardening 执行记录

## 2026-09-05

### 变更范围

- 清理 Admin/App/Flow/Report 开发配置中的数据库、Redis 明文凭据，改为环境变量注入。
- 部署脚本改为 SSH 密钥、固定 known_hosts、非 root 配置输入，移除密码和 `StrictHostKeyChecking=no`。
- 各服务 Undertow 请求体默认上限 20MB；Admin Spring AI prompt/completion 日志默认关闭。
- Maven 根 POM 默认开启编译和测试。
- 字段加密/解密异常失败关闭；脱敏异常输出固定掩码。
- API 配置缓存索引在单条、模块、全量刷新和预热时同步重载；MODULE 事件接入模块刷新。

### 验证

- 命令：`git diff --check`
  - 结果：通过。
- 命令：`mvn -pl forge-framework/forge-starter-parent/forge-starter-crypto,forge-framework/forge-starter-parent/forge-starter-api-config -am test -Dforge.compiler.skip=false -Dforge.tests.skip=false`（JDK 17）
  - 结果：通过；目标模块及其依赖编译成功，现有测试执行器报告 0 个可执行测试。
- 命令：敏感值、`StrictHostKeyChecking=no`、无限请求体和 AI 日志静态扫描
  - 结果：未发现本轮目标字符串；扫描命令需在后续 CI 固化。
- 命令：`source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 ./node_modules/.bin/vite build`
  - 结果：构建成功；存在既有 Vite native config 和 CSS 注释警告，不阻断本轮构建。
- 命令：`mvn -pl forge-framework/forge-plugin-parent/forge-plugin-system -am test -Dforge.compiler.skip=false -Dforge.tests.skip=false`（JDK 17）
  - 结果：构建成功；目标模块编译通过，现有测试执行器报告 0 个可执行测试。
- 命令：`mvn -pl forge-framework/forge-starter-parent/forge-starter-core -am compile -Dforge.compiler.skip=false -Dforge.tests.skip=false`（JDK 17）
  - 结果：通过；全局异常处理器现在同步设置 HTTP 状态码。

### 警告与跳过项

- 初次使用系统 JDK 8 执行测试时失败，原因是已有测试 class 文件版本 61 与 JDK 8 不兼容；切换 JDK 17 后通过。
- 尚未执行真实服务启动、数据库迁移、MFA/验证码端到端验证、Admin UI 构建和生产部署演练；这些需要后续阶段环境和人工审查。
- 工作区原有 `.DS_Store` 修改/删除未由本变更产生，未触碰。

### 第二轮：Web 安全头与错误协议收敛

- 核心 starter 新增 `SecurityHeadersFilter` 自动配置，为响应补充 `X-Content-Type-Options`、`X-Frame-Options`、`Referrer-Policy`、`Permissions-Policy` 和基础 CSP；仅 HTTPS 请求发送 HSTS，且保留应用已设置的同名响应头。
- `SecurityHeadersFilter` 对 `/auth/**` 和 `/oauth2/**` 响应追加 `Cache-Control: no-store` 和 `Pragma: no-cache`，防止认证、验证码和令牌响应被缓存。
- Admin/App/Flow/Report 的 `server.error` 默认关闭错误消息、绑定错误、堆栈和异常类名回显。
- Admin/App Actuator 暴露范围收敛为 `health,metrics`；健康探针匿名可用，metrics 继续经过认证拦截器，避免匿名读取运行指标。
- Admin/App 为 HTTP 服务请求启用 `application` 指标标签和延迟直方图配置，便于后续接入 Prometheus/SLO 看板。
- `GlobalExceptionHandler` 的参数校验、绑定、类型不匹配、方法不支持、404、403、空指针和上传超限分支现在同步设置 HTTP 状态码；上传大小超限使用 `413`。
- 对业务错误码 `429` 的响应统一补充 `Retry-After: 60`，并保留调用方已设置的值。
- 命令：`mvn -pl forge-framework/forge-starter-parent/forge-starter-core -am compile -Dforge.compiler.skip=false -Dforge.tests.skip=true`（JDK 17）
  - 结果：通过；异常处理器新增 `Retry-After` 逻辑编译成功。

### 第二轮验证

- 命令：`export JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home; export PATH="$JAVA_HOME/bin:$PATH"; mvn -pl forge-framework/forge-starter-parent/forge-starter-core -am test -Dforge.compiler.skip=false -Dforge.tests.skip=false`
  - 结果：通过（核心模块编译成功；模块无现有测试源）。
- 命令：`mvn -pl forge-admin-server -am package -DskipTests -Dforge.compiler.skip=false -Dforge.tests.skip=true`（JDK 17）
  - 结果：未通过，失败发生在既有 `forge-plugin-generator` 测试编译；错误为测试源码 UTF-8 编码和构造器签名不匹配，属于明确排除的 generator/lowcode 范围，本轮未修改。
- 命令：`mvn -pl forge-framework/forge-starter-parent/forge-starter-auth -am package -DskipTests -Dforge.compiler.skip=false -Dforge.tests.skip=true`（JDK 17）
  - 结果：通过；验证码服务及依赖模块编译成功，滑块坐标增加边界校验。构建保留既有 Lombok `@Builder` 警告。
- 命令：`mvn -pl forge-framework/forge-starter-parent/forge-starter-core -am compile -Dforge.compiler.skip=false -Dforge.tests.skip=true`（JDK 17）
  - 结果：通过；安全响应头自动配置和全局异常处理器编译成功。
- 命令：`mvn -pl forge-framework/forge-starter-parent/forge-starter-auth -am package -DskipTests -Dforge.compiler.skip=false -Dforge.tests.skip=true`（JDK 17）
  - 结果：通过；认证模块及依赖成功打包，保留既有 Lombok `@Builder` 警告。
- 命令：`mvn -pl forge-framework/forge-starter-parent/forge-starter-core -am compile -Dforge.compiler.skip=false -Dforge.tests.skip=true && git diff --check`（JDK 17）
  - 结果：通过；认证响应缓存控制头编译成功，差异格式检查通过。
- 配置收敛：Admin/App/Flow/Report 的 Sa-Token 默认改为环境变量可调的 30 分钟 token、2 小时活跃超时，关闭 token 共享和 Cookie 读取，仅接受 Header；用于降低长期会话和跨请求复用风险。
- 变更范围：未修改 generator、lowcode、表单设计器或其测试代码。
- 静态检查：`git diff --check` 通过；四个服务的 `server.error` 配置均包含 `include-message/binding-errors/stacktrace: never` 和 `include-exception: false`。
- 追加构建尝试：Flow/Report 聚合包同样在 `forge-plugin-generator` 测试编译阶段失败；即使使用 `-Dmaven.test.skip=true`，项目自定义测试属性仍触发该模块测试编译。该失败仍属于既有 generator/lowcode 排除范围。
- 本轮配置静态检查：Admin/App/Flow/Report 的 metrics 标签与 HTTP 延迟直方图已统一配置，均只对认证用户暴露 metrics；未引入新的依赖或低代码代码。
- 请求关联加固：新增 `RequestCorrelationFilter` 和自动配置，校验/生成 `X-Request-Id`，同时写入 `requestId` 与现有日志 pattern 使用的 `traceId` MDC，并在请求结束恢复上下文；四个服务新增 16KB 默认请求头上限。
- 运行探针收敛：Admin/App/Flow/Report 均增加 liveness/readiness 健康组和 probes 开关，健康详情继续隐藏。
- 运行指标统一：Flow/Report 同步启用 `health,metrics` 暴露、应用标签和 HTTP 延迟直方图；metrics 未加入匿名放行路径。
- 命令：`export JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home; export PATH="$JAVA_HOME/bin:$PATH"; mvn -pl forge-framework/forge-starter-parent/forge-starter-core -am compile -Dforge.compiler.skip=false -Dforge.tests.skip=true`（JDK 17）
  - 结果：通过；核心 starter 编译 37 个源文件成功。
- 脱敏策略修复：当内置策略工厂缺失或未注册策略时，`DesensitizeSerializer` 现在抛出异常并输出固定 `***`，不再把原文作为兜底结果。
- 认证缓存边界扩展：`/oauth2/**` 令牌和撤销接口与 `/auth/**` 一样强制禁止缓存；上下文路径处理兼容根路径和非根 context-path。
- 命令：`mvn -pl forge-framework/forge-starter-parent/forge-starter-crypto -am package -DskipTests -Dforge.compiler.skip=false -Dforge.tests.skip=true`（JDK 17）
  - 结果：通过；加密 starter 及依赖成功打包。
- 命令：`mvn -pl forge-framework/forge-starter-parent/forge-starter-core -am compile -Dforge.compiler.skip=false -Dforge.tests.skip=true && git diff --check`（JDK 17）
  - 结果：通过；OAuth 缓存边界变更编译成功，差异格式检查通过。
- 命令：`mvn -pl forge-framework/forge-starter-parent/forge-starter-core -am compile -Dforge.compiler.skip=false -Dforge.tests.skip=true`（JDK 17）
  - 结果：通过；请求关联过滤器和健康配置阶段变更保持可编译。
- 命令：`mvn -pl forge-framework/forge-starter-parent/forge-starter-core -am compile -Dforge.compiler.skip=false -Dforge.tests.skip=true && git diff --check`（JDK 17）
  - 结果：通过；`traceId` MDC 兼容和工作区差异检查通过。
- 出站链路：`OkHttpSecureOutboundClient` 在调用方未提供 `X-Request-Id` 时透传安全 MDC 请求 ID，重定向仍沿用原有请求头安全处理。
- 命令：`mvn -pl forge-framework/forge-starter-parent/forge-starter-outbound -am package -DskipTests -Dforge.compiler.skip=false -Dforge.tests.skip=true`（JDK 17）
  - 结果：通过；出站 starter 及依赖成功打包，保留既有未检查操作警告。

### 服务清理

- 本轮未启动常驻服务，无需清理进程。

## 2026-09-06

### 第五轮：验证码、普通 API 限流和私有文件缓存边界

- 验证码 challenge：显式 challenge secret 少于 32 字节时失败关闭；无 secret 仅允许纯开发 Profile 使用进程内临时密钥，混合生产/预发布 Profile 不再回退；滑块验证接口在错误答案、过期或签名无效后也消费一次性挑战，防止重复尝试。
- 普通 API 限流：从 API 权限拦截器拆为独立 `ApiRateLimitInterceptor`，在 Sa-Token 登录拦截器之后、权限拦截器之前运行，不受权限开关或注解豁免影响；使用 API 配置匹配后的模板路径和方法作为桶键，OPTIONS 不扣额；新增 `forge.api-config.rate-limit.mode`，默认 `observe`，显式 `enforce` 才抛出 429/503。
- 文件安全：私有文件下载响应和访问 URL 响应增加 `Cache-Control: private, no-store`、`Pragma: no-cache`；读取授权和过期检查继续在访问存储前执行。

### 验证

- 命令：`export JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home; export PATH="$JAVA_HOME/bin:$PATH"; mvn -pl forge-framework/forge-starter-parent/forge-starter-auth,forge-framework/forge-starter-parent/forge-starter-file -am package -DskipTests -Dforge.compiler.skip=false -Dforge.tests.skip=true`。
  - 结果：通过；认证、文件及依赖模块成功编译打包，新增测试源码编译成功。保留既有 Lombok Builder 警告。
- 命令：`mvn -f forge-framework/forge-starter-parent/forge-starter-auth/pom.xml clean test -Penable-tests -Dforge.compiler.skip=false -Dforge.tests.skip=false -DfailIfNoTests=false`。
  - 结果：实际执行 33 项，5 项通过，28 项因本机 Mockito inline Byte Buddy 无法 self-attach 失败；失败同时覆盖既有测试和本轮新增验证码/限流测试，未修改测试框架配置。
- 命令：`git diff --check`。
  - 结果：通过。

- 增量修正：限流拦截器调整为 Sa-Token 之后执行，确保已登录请求按可信租户/用户维度限流；限流主体写入 SHA-256 摘要，桶设置空闲 2 分钟过期；观测模式在 Redis 不可用时记录告警并放行。
- 复验命令：`mvn -pl forge-framework/forge-starter-parent/forge-starter-auth,forge-framework/forge-starter-parent/forge-starter-file -am package -DskipTests -Dforge.compiler.skip=false -Dforge.tests.skip=true`（JDK 17）。
  - 结果：通过；`git diff --check` 同步通过。

## 2026-09-06（第六轮：出站并发隔离与熔断）

- `OkHttpSecureOutboundClient` 按业务场景增加默认 32 并发舱壁；连续 5 次网络/超时失败打开 30 秒，成功后清零，策略拒绝和参数校验错误不计入熔断失败。舱壁耗尽或熔断打开快速失败。
- `OutboundProperties` 新增 `bulkhead-*`、`circuit-breaker-*` 配置，可按环境关闭或调整阈值。
- 命令：`mvn -pl forge-framework/forge-starter-parent/forge-starter-outbound -am package -DskipTests -Dforge.compiler.skip=false -Dforge.tests.skip=true`（JDK 17）。
  - 结果：通过；出站 starter 及依赖编译、测试源码编译和打包成功。
- `git diff --check`：通过。
- 增量复验：熔断窗口配置为空时回退 30 秒，避免配置缺失导致异常链污染；出站 starter 聚合 package 复验通过。

### 警告与跳过项

- 未启动常驻服务、未连接真实 Redis/MySQL、未执行生产部署；无服务进程需要清理。
- 病毒扫描、压缩包炸弹、租户配额、隔离区、下载审计、完整 Prometheus/OpenTelemetry、熔断舱壁和真实集成测试仍按后续任务实施。

### 第三轮：验证码挑战签名与普通 API 限流

- 滑块验证码缓存值升级为版本化 HMAC 载荷，签名覆盖 challenge key、正确坐标和请求绑定摘要；生产缺少 `FORGE_CAPTCHA_CHALLENGE_SECRET` 时失败关闭。
- 请求绑定使用服务端 `remoteAddr`、User-Agent 和会话标识摘要，不读取客户端 `X-Forwarded-For`；校验阶段签名、绑定和坐标边界均需通过。
- 新增普通 API `ApiRateLimitManager`，仅当数据库 API 配置 `limit_flag=1` 时由认证拦截器执行，默认每主体每接口每分钟 120 次；Redis 限流桶无许可返回 429，Redis 不可用返回 503。
- `forge-starter-api-config` 增加测试依赖和限流单测；未修改 generator/lowcode。

### 第三轮验证

- 命令：`mvn -f forge-framework/forge-starter-parent/forge-starter-auth/pom.xml clean test -Penable-tests -Dforge.compiler.skip=false -Dforge.tests.skip=false -DfailIfNoTests=false`（JDK 17）。结果：测试源码编译通过并实际执行 31 项；26 项因本机 Mockito inline Byte Buddy 无法 self-attach 而失败，5 项通过。该环境问题同时影响既有测试和本轮新增测试，未修改测试框架配置。
- 命令：`mvn -pl forge-framework/forge-starter-parent/forge-starter-api-config -am package -DskipTests -Dforge.compiler.skip=false -Dforge.tests.skip=true`（JDK 17）。结果：通过；限流组件及 API 配置模块成功打包。
- 命令：`git diff --check`。结果：通过。
- 保留警告：既有 Lombok `@Builder` 默认值、过时 API 和未检查操作警告；未启动常驻服务。

### 第四轮：私有文件下载授权

- `FileManager.download` 对私有文件强制执行 `FileMetadataPersistence.checkPermission`，使用当前可信会话用户 ID；拒绝访问返回 403，避免通用下载接口因 `@ApiPermissionIgnore` 直接暴露私有文件。
- 保留既有本地存储安全边界：规范化路径、绝对路径拒绝、符号链接拒绝、业务目录和桶名称安全段校验；本轮未修改低代码/generator。
- 命令：`mvn -pl forge-framework/forge-starter-parent/forge-starter-file -am package -DskipTests -Dforge.compiler.skip=false -Dforge.tests.skip=true`（JDK 17）。结果：通过，文件 starter 及依赖编译打包成功。
- 命令：`git diff --check`。结果：通过。
- 新增测试：私有文件无权限下载合同测试已编译；完整 Mockito 测试执行仍受本机 Byte Buddy self-attach 环境限制。
- 修正：普通 API 限流器放置在 `forge-starter-auth`，与认证拦截器同一可部署模块，避免单模块测试或旧本地构件缺少新增 API 配置类。
- 命令：`mvn -pl forge-framework/forge-starter-parent/forge-starter-auth -am package -DskipTests -Dforge.compiler.skip=false -Dforge.tests.skip=true`（JDK 17）。结果：通过，认证 starter 及依赖成功打包。
- 命令：`git diff --check`。结果：通过。


## 2026-09-07（第七轮：用户指定 4/5/6/7/9）

- 分片上传：`LocalFileStorage` 增加未完成上下文上限（默认 100）、活动 TTL（默认 30 分钟）、定时清理（默认 60 秒），限制分片编号并在合并前校验分片连续完整。
- 普通 API 限流：默认模式从 `observe` 调整为 `enforce`；显式 `observe` 仍可用于灰度，Admin 配置补充默认限流参数。
- 前端用户信息：补充 `getTransitionEparchyCode` 空值安全转换，并复制 OA/UAC staff 对象避免直接改写源数据；新增 2 个 Vitest 用例。
- 文件测试契约：修正 `getAccessUrl` 反射签名；`FileManagerTest` 使用 `ExecutionIdentityContextHolder` 建立测试身份。
- 文档同步：修正 `AGENTS.md`、`code-copilot/rules/project-context.md`、`README.md` 的后端路径、Spring Boot 3.5.13 和模块数量说明。

### 验证

- 命令：`mvn -Penable-tests -pl forge-framework/forge-starter-parent/forge-starter-file,forge-framework/forge-starter-parent/forge-starter-auth -am -Dtest=LocalFileStorageTest,FileManagerTest,FileControllerPermissionContractTest,ApiRateLimitManagerTest -Dsurefire.failIfNoSpecifiedTests=false test`（JDK 17）。
  - 结果：通过，文件模块 16 个目标测试全部通过，限流模块另有 2 个目标测试通过。保留既有 Lombok Builder、过时 API 和未检查操作警告。
- 命令：`./node_modules/.bin/vitest run src/store/modules/__tests__/user.spec.js`（Node 20.19.0）。
  - 结果：通过，1 个测试文件、2 个用例全部通过。
- 命令：`git diff --check`。
  - 结果：通过。
