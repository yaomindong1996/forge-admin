# 一行配置，把 Spring Boot 后台变成 AI Agent 的工具箱：Forge MCP Server 插件源码拆解

> 《给 AI Agent 修城墙》系列第 1 篇。这个系列聊一个问题：企业后台接入 AI Agent，认证、授权、审批、审计到底该怎么做。所有代码都来自开源项目 Forge Admin（Vue3 + Spring Boot 3 微内核插件化中后台框架），可直接对照源码阅读。

## 先说痛点

MCP（Model Context Protocol）今年火得一塌糊涂，很多团队的第一反应是：给后台系统也开一个 MCP Server，让 AI Agent 能直接操作系统能力。

真动手就会发现，Spring AI 官方 starter 五分钟就能把 `/mcp` 端点跑起来——但跑出来的是一个**裸奔的 Server**：

1. **没有认证**。默认配置下任何能访问端点的人都能列出工具、发起调用。后台系统里躺着的可是用户数据、订单、审批流。
2. **协议可以乱配**。SSE、STATELESS、stdio 随便开，工具异步执行时调用方身份上下文直接丢失——AI 用谁的身份删了数据？不知道。
3. **工具无限膨胀**。每个业务模块都往上注册工具，重名冲突、Schema 随意、业务接口被原样暴露给大模型，授权和幂等根本没人管。

Forge Admin 的做法是把 MCP Server 收进一个独立插件 `forge-plugin-mcp`，用三层设计把这三个坑全填上：**启动期协议守卫 fail-fast、传输层认证过滤器、固定元工具 SPI**。这篇文章逐层拆源码。

## 整体结构

```
MCP Client (Claude / 自研 Agent)
   │  POST /mcp  (Bearer fdu_xxx...)
   ▼
ForgeMcpAuthenticationFilter   ← 只挂 /mcp，401/403/503 分级拒绝
   │  写入 caller + requestId 到 request attribute
   ▼
WebMvcStreamableServerTransportProvider
   │  contextExtractor 把 caller 塞进 transportContext
   ▼
固定元工具（ping / capability.search / describe / invoke）
   │  McpToolContributor SPI 贡献，启动期聚合查重
   ▼
CapabilityRegistry.invoke()   ← 授权、幂等、审计在能力层收口
```

关键设计取舍一句话：**MCP 层只做协议和传输，不做业务；业务能力的授权、幂等、审计全部下沉到能力注册中心（Capability Registry）**。MCP 插件甚至不允许业务模块直接注册业务工具——只允许贡献固定的"元工具"。后面会看到这条约束是怎么落地的。

## 一、装配：一个依赖 + 一个开关

插件通过 Spring Boot 3 的 `AutoConfiguration.imports` 注册自动配置，主应用想用 MCP，只需要在 `pom.xml` 里加依赖：

```xml
<!-- forge-admin-server/pom.xml -->
<dependency>
    <groupId>com.mdframe</groupId>
    <artifactId>forge-plugin-mcp</artifactId>
</dependency>
```

然后打开开关：

```yaml
# application.yml
spring:
  ai:
    mcp:
      server:
        enabled: ${FORGE_MCP_ENABLED:false}   # 默认关闭
        name: forge-ai-hub
        type: SYNC
        stdio: false
        streamable-http:
          mcp-endpoint: /mcp
```

自动配置类本身的条件很克制：

```java
// forge-plugin-mcp/.../config/ForgeMcpServerAutoConfiguration.java
@AutoConfiguration(
        after = CapabilityAutoConfiguration.class,
        before = McpServerStreamableHttpWebMvcAutoConfiguration.class)
@ConditionalOnProperty(
        prefix = "spring.ai.mcp.server",
        name = "enabled",
        havingValue = "true")
public class ForgeMcpServerAutoConfiguration { ... }
```

两个细节值得注意：

- **`after` / `before` 显式编排装配顺序**：必须在能力注册中心之后、Spring AI 官方 MCP 自动配置之前完成装配，这样官方配置检测到 `WebMvcStreamableServerTransportProvider` 已存在（`@ConditionalOnMissingBean`）就会让位，自定义的传输层（带 contextExtractor）生效。
- **默认关闭**。`FORGE_MCP_ENABLED=false` 时整个插件不装配，生产环境不存在"忘了关"的问题。

## 二、协议守卫：启动期 fail-fast 的三条铁律

这是我最喜欢的一段代码，一共 20 行：

```java
// forge-plugin-mcp/.../config/ForgeMcpProtocolGuard.java
public final class ForgeMcpProtocolGuard {

    public ForgeMcpProtocolGuard(McpServerProperties properties) {
        if (properties.getProtocol() != McpServerProperties.ServerProtocol.STREAMABLE) {
            throw new IllegalStateException(
                    "Forge MCP Server 只允许 STREAMABLE 协议，禁止 SSE 或 STATELESS transport");
        }
        if (properties.isStdio()) {
            throw new IllegalStateException("Forge MCP Server 禁止同时启用 stdio transport");
        }
        if (properties.getType() != McpServerProperties.ApiType.SYNC) {
            throw new IllegalStateException(
                    "Forge MCP Server 必须使用 SYNC 类型，以保证受信执行身份在工具线程中完整生效");
        }
    }
}
```

它在 Bean 构造阶段直接抛异常——**配错协议，应用根本启动不起来**，而不是等到 AI Agent 连上来才发现行为诡异。

三条铁律各自的道理：

- **只允许 STREAMABLE HTTP**：MCP 官方新协议，单一端点、支持流式响应；老的 SSE transport 要维护两个端点，STATELESS 模式则没有会话概念，身份传递无从谈起。
- **禁止 stdio**：stdio 是给本地进程用的，一个 Web 后台同时开 stdio 没有任何正当场景，开了就是攻击面。
- **强制 SYNC**：异步（ASYNC）模式下工具在别的线程执行，`ThreadLocal` 风格的身份上下文会断。同步执行保证"谁在调用"这个信息从 HTTP 线程一路传到工具执行。错误信息里也把这个原因写明了——读异常信息就能理解设计意图，这是好代码。

安全策略最怕的不是严格，而是"运行期才发现"。把校验压到启动期，是成本最低的防线。

## 三、传输层认证：一个只盯 `/mcp` 的过滤器

MCP 端点不走 Sa-Token 的 Web 会话体系（调用方是机器，不是浏览器用户），所以插件注册了一个独立的认证过滤器：

```java
// ForgeMcpServerAutoConfiguration.java
@Bean
public FilterRegistrationBean<ForgeMcpAuthenticationFilter> forgeMcpAuthenticationFilter(
        McpServerStreamableHttpProperties properties, ...) {
    ForgeMcpAuthenticationFilter filter = new ForgeMcpAuthenticationFilter(
            properties.getMcpEndpoint(), callerContextResolver, requestLifecycle);
    FilterRegistrationBean<ForgeMcpAuthenticationFilter> registration =
            new FilterRegistrationBean<>(filter);
    registration.addUrlPatterns(properties.getMcpEndpoint());  // 只挂 /mcp
    registration.setOrder(Ordered.LOWEST_PRECEDENCE - 100);
    return registration;
}
```

过滤器的核心逻辑是"先解析身份，失败分类拒绝，成功则注入上下文"：

```java
// ForgeMcpAuthenticationFilter.java（有删减）
@Override
protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
        FilterChain filterChain) throws ServletException, IOException {
    String requestId = UUID.randomUUID().toString();
    CapabilityCallerContext caller;
    try {
        caller = callerContextResolver.resolve(request);
    }
    catch (McpAccessDeniedException exception) {
        writeForbidden(response, requestId);      // 403：来源不合法
        return;
    }
    catch (BusinessException exception) {
        if (Integer.valueOf(403).equals(exception.getCode())) {
            writeForbidden(response, requestId);
        } else if (Integer.valueOf(401).equals(exception.getCode())) {
            writeUnauthorized(response, requestId); // 401：未认证
        } else {
            writeUnavailable(response, requestId, exception); // 503：身份服务故障
        }
        return;
    }
    if (caller == null) {
        writeUnauthorized(response, requestId);
        return;
    }
    request.setAttribute(McpTransportContextKeys.CALLER_CONTEXT, caller);
    request.setAttribute(McpTransportContextKeys.REQUEST_ID, requestId);
    response.setHeader(REQUEST_ID_HEADER, requestId);
    try (AutoCloseable ignored = requestLifecycle.open(request, caller)) {
        filterChain.doFilter(request, response);
    }
}
```

三个工程细节：

- **拒绝分级**：401（没带身份）、403（Origin 不在白名单）、503（身份服务本身挂了）严格区分。503 这个设计很务实——身份服务抖动时返回"暂不可用"而不是 401，避免客户端误判为凭证失效而触发重新授权流程。
- **每个请求发一个 `X-Request-Id`**，同时写进响应头和错误响应体。AI Agent 是程序不是人，排障全靠这个 ID 串联日志。
- **401 响应带 `WWW-Authenticate` 挑战头**，指向 OAuth Protected Resource 元数据端点——这是 MCP 规范里认证协商的标准姿势，合规的客户端能自动发现该去哪儿换 Token。

## 四、身份解析：严格到"洁癖"的 Bearer 校验

过滤器本身不认识 Token，解析委托给 `McpCallerContextResolver` 接口（这是个 SPI，默认实现由身份插件 `forge-plugin-capability-identity` 提供）：

```java
// forge-plugin-capability-identity/.../mcp/CapabilityMcpAccessTokenResolver.java
@Override
public CapabilityCallerContext resolve(HttpServletRequest request) {
    validateOrigin(request);
    // 明确拒绝 query 参数传 token
    if (request.getParameter("access_token") != null || request.getParameter("token") != null) {
        return null;
    }
    String rawToken = extractBearerToken(request);
    if (rawToken == null
            || !rawToken.matches("^fdu_[A-Za-z0-9_-]{22}_[A-Za-z0-9_-]{43}$")) {
        return null;
    }
    AuthenticatedCapabilityIdentity authenticated = accessTokenService.authenticate(
            rawToken, properties.validatedResource(), Set.of());
    CapabilitySecurityPrincipal principal = authenticated.principal();
    return new CapabilityCallerContext(
            principal.clientCode(), principal.tenantId(), principal.actorUserId(),
            principal.activeOrgId(), principal.scopes());
}
```

这段代码的"洁癖"体现在四个地方：

1. **禁止 query 参数传 Token**。URL 会进 access log、会进浏览器历史、会被代理记录，Token 放 query 里是经典泄露渠道——直接拒绝，没有商量。
2. **Authorization 头必须恰好一个**。`Collections.list(request.getHeaders(...)).size() != 1` 就拒绝。多头场景在代理链路里真实存在，宽容处理等于给走私留门。
3. **Token 格式正则先行**：`fdu_` 前缀 + 定长字符集，格式不对根本不打到认证服务，挡住一大波垃圾流量。
4. **Origin 白名单**：浏览器型 MCP 客户端必须配置精确 Origin，不在名单直接 403。

注意到 `CapabilityCallerContext` 里带的是什么：**clientCode（哪个机器客户端）、tenantId（哪个租户）、actorUserId（委托给哪个用户）、scopes（授权范围）**。AI Agent 在系统里不是匿名幽灵，而是一个有租户归属、有用户委托、有权限边界的"一等公民调用方"。这是后面所有授权和审计的地基。

## 五、工具贡献 SPI：业务模块不能为所欲为

Spring AI 允许任何 Bean 注册 MCP 工具，这在大团队里是灾难—— Forge 加了一层收口。业务模块想暴露能力，只能实现这个函数式接口：

```java
// forge-plugin-mcp/.../spi/McpToolContributor.java
/**
 * Forge MCP 固定工具贡献者。业务模块只能贡献稳定元工具，不能创建额外 transport。
 */
@FunctionalInterface
public interface McpToolContributor {
    Collection<McpServerFeatures.SyncToolSpecification> contribute(
            McpToolSchemaProjector schemaProjector);
}
```

Javadoc 里那句"不能创建额外 transport"是架构红线：传输层全局只有一个，就是带认证的 `/mcp`。任何模块自己开端点，等于在城墙上私开暗门。

所有贡献者在启动期被聚合，**重名直接拒绝启动**：

```java
// McpToolContributorAggregator.java（有删减）
public static List<McpServerFeatures.SyncToolSpecification> aggregate(
        List<McpToolContributor> contributors, McpToolSchemaProjector schemaProjector) {
    Map<String, McpServerFeatures.SyncToolSpecification> byName = new LinkedHashMap<>();
    for (McpToolContributor contributor : contributors) {
        for (var specification : contributor.contribute(schemaProjector)) {
            if (specification.tool().name() == null || specification.tool().name().isBlank()) {
                throw new IllegalStateException("MCP Tool contributor 返回了无效工具定义");
            }
            if (byName.putIfAbsent(specification.tool().name(), specification) != null) {
                throw new IllegalStateException("MCP Tool 名称重复: " + specification.tool().name());
            }
        }
    }
    // 按名称排序后返回不可变列表
}
```

和协议守卫同一个思路：**冲突在启动期爆炸，而不是在运行期随机覆盖**。两个模块注册了同名工具，谁覆盖谁？在 MCP 场景下这不是小问题——AI Agent 调错工具的代价可能是真实的数据变更。

## 六、元工具模式：只暴露 search / describe / invoke

那业务模块到底贡献什么工具？看受控能力插件的实现：

```java
// forge-plugin-capability-secure-actions/.../mcp/SecureActionMcpToolContributor.java
@Override
public Collection<McpServerFeatures.SyncToolSpecification> contribute(
        McpToolSchemaProjector schemaProjector) {
    return List.of(
            tool("capability.search",   "搜索当前调用方可用的已发布受控能力", ...),
            tool("capability.describe", "查看一个已授权受控能力的输入输出规范", ...),
            tool("capability.invoke",   "经幂等校验和人工确认后执行一个受控能力", ...));
}
```

这是整个设计里最反直觉、也最值得琢磨的一笔：**不管系统里有多少业务能力，暴露给 AI 的工具永远只有三个（外加一个 ping 健康检查）**。

为什么不让 AI 直接看到 `createOrder`、`deleteUser` 这样的业务工具？因为那样每个工具都要自己解决授权、参数校验、幂等、确认——必然烂尾。收敛成元工具之后：

- `capability.search`：AI 只能搜到**当前调用方被授权**的能力，未授权的能力对它来说不存在；
- `capability.describe`：返回某个能力的输入输出 Schema、字段白名单、风险等级、是否需要人工确认；
- `capability.invoke`：必须带 16-128 位**幂等键**，高风险动作还要过人工确认才执行。

工具描述里那句"经幂等校验和人工确认后执行"不是注释，是给大模型读的行为契约。同时每个工具都带 `ToolAnnotations`（readOnly / destructive / idempotent），支持这些标注的客户端可以据此调整调用策略。

发现（search）→ 理解（describe）→ 执行（invoke），AI 的使用路径和人类开发者读文档调 API 的路径完全同构——这套心智模型大模型天然擅长。

## 七、上下文穿透：身份怎么穿过传输层到达工具

最后一个技术点：过滤器在 Servlet 层解析出的 caller，怎么传到 MCP 工具的执行回调里？答案是 MCP 传输层的 `contextExtractor` 机制：

```java
// ForgeMcpServerAutoConfiguration.java
return WebMvcStreamableServerTransportProvider.builder()
        .jsonMapper(new JacksonMcpJsonMapper(objectMapper))
        .mcpEndpoint(properties.getMcpEndpoint())
        .contextExtractor(new ForgeMcpTransportContextExtractor(callerContextResolver))
        .build();
```

`contextExtractor` 把 caller 和 requestId 塞进 `transportContext`，工具执行端再从 exchange 里取出来：

```java
// McpCapabilityAdapter.java
private CapabilityCallerContext resolveCaller(McpSyncServerExchange exchange) {
    if (exchange == null || exchange.transportContext() == null) {
        return null;
    }
    Object caller = exchange.transportContext().get(McpTransportContextKeys.CALLER_CONTEXT);
    return caller instanceof CapabilityCallerContext context ? context : null;
}
```

取不到 caller？直接返回 `UNAUTHENTICATED` 错误，绝不"匿名放行"。结合前面强制 SYNC 的协议守卫，身份链路的每一环都是闭合的：HTTP 线程 → 过滤器 → transportContext → 工具回调 → `CapabilityRegistry.invoke()`。

## 总结

回看这个插件，真正值得抄走的不是 MCP 接法（官方 starter 谁都会），而是三条工程纪律：

1. **配置错误在启动期爆炸**：协议守卫、工具重名查重，全部 fail-fast，不给运行期留惊喜。
2. **传输层只有一扇门**：全局唯一 `/mcp` 端点 + 独立认证过滤器，业务模块没有私开 transport 的能力。
3. **业务工具收敛为元工具**：search / describe / invoke 三件套，授权、幂等、审计在能力注册中心统一收口，MCP 层保持纯粹。

下一篇我们拆身份体系：`fdu_` 短期 Token 是怎么签发的、为什么需要三个互不相同的 HMAC Pepper、机器客户端的凭证轮换怎么做——AI 调用的"OAuth"完整链路。

---

**项目地址**：Forge Admin（Gitee/GitHub 搜索 forge-admin），本文涉及源码位于 `forge-server/forge-framework/forge-plugin-parent/forge-plugin-mcp` 与 `forge-plugin-capability-identity` 模块，欢迎对照阅读、提 Issue 交流。

**系列目录**：
- 第 1 篇：一行配置，把 Spring Boot 后台变成 AI Agent 的工具箱（本篇）
- 第 2 篇：AI 调用也要过 OAuth——fdu_ 短期 Token 与三 Pepper 凭证体系（待发布）
- 第 3 篇：只暴露三个元工具——受控能力网关的字段白名单与幂等设计（待发布）
- 第 4 篇：AI 的高危操作必须过人——挂 Flowable 审批的完整链路（待发布）
