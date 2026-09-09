---
kind: logging_system
name: 基于 SLF4J + Logback 的 Forge 日志体系与操作/登录审计
category: logging_system
scope:
    - '**'
source_files:
    - forge-server/forge-framework/forge-starter-parent/forge-starter-log/src/main/java/com/mdframe/forge/starter/log/aspect/OperationLogAspect.java
    - forge-server/forge-framework/forge-starter-parent/forge-starter-log/src/main/java/com/mdframe/forge/starter/log/config/LogThreadPoolConfig.java
    - forge-server/forge-framework/forge-starter-parent/forge-starter-log/src/main/java/com/mdframe/forge/starter/log/context/OperationAuditContext.java
    - forge-server/forge-framework/forge-starter-parent/forge-starter-log/src/main/java/com/mdframe/forge/starter/log/domain/OperationLogInfo.java
    - forge-server/forge-framework/forge-starter-parent/forge-starter-log/src/main/java/com/mdframe/forge/starter/log/domain/LoginLogInfo.java
    - forge-server/forge-framework/forge-starter-parent/forge-starter-log/src/main/java/com/mdframe/forge/starter/log/listener/LoginLogListener.java
    - forge-server/forge-framework/forge-starter-parent/forge-starter-log/src/main/java/com/mdframe/forge/starter/log/service/ILogService.java
    - forge-server/forge-framework/forge-starter-parent/forge-starter-core/src/main/java/com/mdframe/forge/starter/core/context/LogProperties.java
    - forge-server/forge-admin-server/src/main/resources/application.yml
    - forge-server/forge-app-server/src/main/resources/application.yml
    - forge-server/forge-flow/forge-flow-server/src/main/resources/application.yml
    - forge-server/forge-report-server/src/main/resources/application.yml
---

## 1. 使用的框架与工具

- **日志门面**：SLF4J（`org.slf4j.Logger`），业务代码统一通过 Lombok `@Slf4j` 注入 logger。
- **日志实现**：Logback，每个后端服务通过 `logging.config=classpath:logback.xml` 指定配置文件。
- **AOP 切面**：Spring AOP + AspectJ，用于自动拦截 Controller 方法并记录操作日志。
- **线程池**：`ThreadPoolTaskExecutor`，以 `@Bean("logTaskExecutor")` 暴露，供异步保存日志使用。
- **MDC**：使用 `org.slf4j.MDC` 在请求级别注入 `traceId`，便于跨组件追踪。
- **MyBatis 日志**：通过 `mybatis-plus.configuration.logImpl=org.apache.ibatis.logging.slf4j.Slf4jImpl` 将 SQL 输出到 SLF4J。
- **Sa-Token 集成**：通过 `SaTokenListener` 监听登录/登出/踢下线等事件，记录登录日志。

## 2. 核心文件与包

| 路径 | 作用 |
|---|---|
| `forge-starter-parent/forge-starter-log/.../aspect/OperationLogAspect.java` | 操作日志 AOP 切面，拦截所有 `@Controller` / `@RestController` 方法 |
| `forge-starter-parent/forge-starter-log/.../config/LogThreadPoolConfig.java` | 定义 `logTaskExecutor` 异步线程池 |
| `forge-starter-parent/forge-starter-log/.../context/OperationAuditContext.java` | 基于 `ThreadLocal` 的操作审计上下文，允许业务在一次请求内写入 before/after/diff 快照 |
| `forge-starter-parent/forge-starter-log/.../domain/OperationLogInfo.java` | 操作日志实体（模块、类型、URL、参数、响应、耗时、状态等） |
| `forge-starter-parent/forge-starter-log/.../domain/LoginLogInfo.java` | 登录日志实体（登录类型、IP、浏览器、OS、客户端代码等） |
| `forge-starter-parent/forge-starter-log/.../listener/LoginLogListener.java` | Sa-Token 登录事件监听器，异步持久化登录日志 |
| `forge-starter-parent/forge-starter-log/.../service/ILogService.java` | 日志持久化接口，由具体业务模块实现 |
| `forge-starter-parent/forge-starter-core/.../context/LogProperties.java` | `forge.log.*` 配置属性（开关、长度限制、排除路径、线程池大小） |
| 各服务 `application.yml` | 设置 `logging.level.com.forge=info`、`logging.config=classpath:logback.xml`、MyBatis logImpl |
| `var/logs/*.log` | 运行时落盘日志目录（如 `forge-admin-info.log`、`forge-flow.log`） |

## 3. 架构与设计决策

### 3.1 分层结构

- **基础设施层**：`forge-starter-log` 提供通用能力（切面、线程池、上下文、实体、监听器、配置），不关心日志最终落库方式。
- **业务适配层**：各服务实现 `ILogService` 接口，把 `OperationLogInfo` / `LoginLogInfo` 持久化到数据库或外部系统。
- **调用层**：业务 Controller 通过注解 `@OperationLog(module, type, desc)` 声明要记录的操作；未标注时切面根据 URL 后缀（`/page`、`/list`、`/add`、`/delete` 等）和 HTTP 方法自动推断操作类型。

### 3.2 操作日志流程

1. 进入 `OperationLogAspect.around()`，读取 `LogProperties.enableOperationLog`，为 false 则直接放行。
2. 解析请求 URL、HTTP 方法、User-Agent、页面标题（`X-Page-Title`）、页面路径（`X-Page-Path`）。
3. 若方法标注了 `@OperationLog`，取 module/type/desc；否则通过 `IApiConfigManager` 或 URL 规则推断默认值。
4. 生成 `traceId` 放入 MDC，收集请求参数（过滤 `HttpServletRequest`/`HttpServletResponse`/`MultipartFile`，对带 `@RequestBody` 且启用了 API 解密的端点用 `[DECRYPTED_REQUEST_BODY_OMITTED]` 脱敏）。
5. 执行目标方法，捕获成功/失败状态、异常信息、执行时长。
6. 从 `OperationAuditContext` 读取业务主动写入的 before/after/diff 快照，合并进 `OperationLogInfo`。
7. 通过 `logTaskExecutor` 异步调用 `ILogService.saveOperationLog`，finally 中清理 ThreadLocal 与 MDC。

### 3.3 登录日志流程

`LoginLogListener` 实现 `SaTokenListener`，在 `doLogin`/`doLogout`/`doKickout`/`doReplaced`/`doDisable`/`doUntieDisable` 等事件中构造 `LoginLogInfo`，同样通过 `logTaskExecutor` 异步持久化，并通过 `UserAgentUtil` 解析浏览器与操作系统。

### 3.4 可观测性字段

- **Trace ID**：`yyyyMMddHHmmssSSS` + `System.nanoTime() % 1000000` + `_LOG`，写入 MDC 的 `traceId`。
- **用户上下文**：优先从 `SessionHelper.getUserId()` 获取，回退到 Sa-Token 的 `StpUtil.getLoginId()`。
- **请求来源**：支持 `X-Forwarded-For` / `X-Real-IP` / `Proxy-Client-IP` / `WL-Proxy-Client-IP` 多级 IP 解析，取第一个。
- **前端元数据**：通过 `X-Page-Path`、`X-Page-Title` 传递页面级上下文。

## 4. 约定与约束

### 4.1 日志级别策略
- 应用根日志级别：`com.forge=info`，`org.springframework=error`，`org.mybatis.spring.mapper=error`。
- MyBatis SQL 通过 SLF4J 输出，可通过调整 `org.mybatis` 级别控制。
- 控制台打印开关由 `forge.log.printOperationLog` / `forge.log.printLoginLog` 控制，默认仅打印登录日志。

### 4.2 安全与脱敏约定
- 以下路径**始终跳过操作日志**：`/auth/login`、`/auth/register`、`/auth/changePassword`、`/auth/resetPassword`、`/auth/resetPassword/code`、`/auth/online/kickout`、`/auth/online/batchKickout`、`/oauth2/**`、`/openapi/v1/capabilities/**`。
- 额外排除路径由 `forge.log.excludePaths` 配置，默认包含 `/actuator/**`、`/swagger-ui/**`、`/v3/api-docs/**`。
- 对启用 `@ApiDecrypt` 的端点，`@RequestBody` 参数会被替换为 `[DECRYPTED_REQUEST_BODY_OMITTED]`，避免明文密码落入日志。
- 请求参数与响应结果分别按 `requestParamsMaxLength` / `responseResultMaxLength`（默认 2000）截断。

### 4.3 性能约定
- 日志持久化**必须异步**：通过 `logTaskExecutor` 提交任务，拒绝策略为 `CallerRunsPolicy`，关闭时等待最多 60 秒。
- 线程池默认 `core=2`、`max=5`、`queueCapacity=500`，可通过 `forge.log.threadPoolCoreSize` 等属性覆盖。
- 查询类操作（`QUERY`、GET/HEAD/OPTIONS、`/page`、`/list`、`/tree`、`/detail`、`/getbyid`、`/options`、`/profile`、`/query`）默认不持久化操作日志，仅可能打印到控制台。

### 4.4 扩展点
- 新增日志落地方式只需实现 `ILogService` 并注册为 Spring Bean，切面通过 `@ConditionalOnBean(ILogService.class)` 条件装配。
- 业务可在任意位置调用 `OperationAuditContext.setBeforeData/setAfterData/setDiffData/setOperationContent`，在 finally 中由切面统一采集。
- 通过 `@OperationLog(module, type, desc, saveRequestParams, saveResponseResult)` 精细控制单个接口的日志行为。

### 4.5 前端侧配合
- 前端通过请求头 `X-Page-Path`、`X-Page-Title` 向服务端传递页面上下文，使操作日志能关联到具体页面。