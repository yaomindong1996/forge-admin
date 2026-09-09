# enterprise-framework-hardening 增量测试计划

## 本轮范围（2026-09-05）

- 配置凭据清理、请求体上限、AI 日志默认关闭、Maven 默认质量门禁。
- `CryptoField` 加密/解密失败关闭，脱敏失败固定掩码。
- API 配置 MODULE 刷新和启用配置路径索引同步。

## 必跑命令

```bash
cd forge-server
export JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
mvn -pl forge-framework/forge-starter-parent/forge-starter-crypto,forge-framework/forge-starter-parent/forge-starter-api-config -am test -Dforge.compiler.skip=false -Dforge.tests.skip=false
```

```bash
git diff --check
rg -n '!Ymd199606|forge123456|yml199606|StrictHostKeyChecking=no|max-http-post-size: -1|log-prompt: true|log-completion: true' forge-server
```

## 后续增量验证

- 为 `CryptoFieldSerializer`、`CryptoFieldDeserializer` 和 `DesensitizeSerializer` 增加失败关闭/固定掩码单测。
- 滑块验证码 challenge 签名、请求绑定、篡改载荷和一次性消费单测。
- 普通 API `limit_flag` 限流器在 Redis 无许可和 Redis 不可用时分别返回 `429/503`，并验证限流桶维度。
- 私有文件下载无权限时返回 403；本地存储路径穿越和符号链接测试继续复用既有用例。
- 本轮认证模块 clean test 实际执行 31 项；26 项受本机 Mockito inline Byte Buddy self-attach 环境限制失败，5 项通过，详见 execution-log。
- 使用 JDK 17 执行 Admin 聚合 package；使用 Node 20.19.0 执行 Admin UI build。
- 启动隔离环境验证请求体限制、验证码 challenge、MFA、Actuator 鉴权和 API 配置多实例刷新。

## 本轮增量验证（第二阶段）

- 核心 starter：安全响应头自动配置、认证路径 `no-store` 缓存控制、统一异常 HTTP 状态码和 `429 Retry-After`。
- 认证配置：四个服务 Sa-Token 短期 token、活跃超时、Header-only 会话配置可被环境变量覆盖。
- 命令：`JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-starter-parent/forge-starter-core -am compile -Dforge.compiler.skip=false -Dforge.tests.skip=true`。
- 命令：`JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-starter-parent/forge-starter-auth -am package -DskipTests -Dforge.compiler.skip=false -Dforge.tests.skip=true`。
- 结果：两条命令均通过；Admin 聚合 package 仍受既有 generator 测试源码错误阻断，未进入排除范围修复。
- 配置静态检查：Admin/App/Flow/Report 的 Actuator 暴露均为 `health,metrics`；Sa-Token 拦截器只排除健康探针路径，metrics 不在匿名白名单。
- 配置静态检查：四个服务错误回显关闭，认证路径缓存控制和安全响应头由核心 starter 自动配置。
- 本轮新增请求关联与请求头边界验证：`RequestCorrelationFilter` 编译通过；四个服务均配置 `FORGE_MAX_HTTP_REQUEST_HEADER_SIZE` 默认 `16KB`，用于限制异常大 Header。
- 健康探针静态检查：四个服务均配置 `management.endpoint.health.probes.enabled=true` 及 liveness/readiness 分组，`show-details` 仍为 `never`。
- 出站关联验证：`forge-starter-outbound` 及依赖使用 JDK 17 打包通过；出站请求在缺少显式关联头时读取安全校验后的 MDC `requestId`。

## 本轮增量验证（2026-09-06）

- 验证码：生产/混合 Profile 缺少 challenge secret 时失败关闭；显式 secret 少于 32 字节时失败关闭；滑块错误答案在验证接口中一次性消费。
- 普通 API 限流：改为独立 MVC 拦截器；使用 API 配置模板和方法生成桶键；OPTIONS 跳过扣额；默认 `observe`，显式 `enforce` 才返回 429/503。
- 文件：私有下载和访问 URL 设置 `Cache-Control: private, no-store`、`Pragma: no-cache`；继续覆盖读取授权与过期返回 410。
- 命令：`mvn -pl forge-framework/forge-starter-parent/forge-starter-auth,forge-framework/forge-starter-parent/forge-starter-file -am package -DskipTests -Dforge.compiler.skip=false -Dforge.tests.skip=true`（JDK 17），通过，测试源码编译成功。
- 命令：`mvn -f forge-framework/forge-starter-parent/forge-starter-auth/pom.xml clean test -Penable-tests -Dforge.compiler.skip=false -Dforge.tests.skip=false -DfailIfNoTests=false`（JDK 17），实际执行 33 项，5 项通过，28 项因本机 Mockito inline Byte Buddy 无法 self-attach 失败；新增验证码与限流测试也受同一环境限制。
- 由于聚合测试在 `forge-starter-core` 上使用 `-Penable-tests` 时缺少可识别 JUnit 引擎，未将该失败归因于本轮代码；目标模块 package 验证已通过。
- 出站客户端：验证每场景并发舱壁、熔断阈值/恢复窗口，以及策略拒绝不计入失败；本轮先完成编译级验证，真实并发与超时场景需在后续隔离环境执行。


## 本轮增量验证（2026-09-07）

- 分片上传：验证未完成上下文数量上限、TTL 定时清理、过期上传拒绝、分片编号范围和缺片合并拒绝。
- 普通 API 限流：默认 `enforce` 时 Redis 无许可返回 429、Redis 不可用返回 503；显式 `observe` 仍可作为灰度配置。
- 文件模块测试契约：补齐 `getAccessUrl` 的响应参数签名，并在测试中建立可信执行身份，验证权限业务异常。
- 前端用户信息：区划转换函数空值/对象/字符串用例。
- 文档同步：后端根目录为 `forge-server`，Spring Boot 版本为 3.5.13，模块数量以 reactor 为准。
