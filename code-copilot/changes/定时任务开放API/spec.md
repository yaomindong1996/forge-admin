# 定时任务开放 API
> status: complete
> created: 2026-07-19
> complexity: 🔴复杂
> parent: code-copilot/changes/定时任务优化/spec.md
> version: V8
> dependency: 定时任务可靠性加固、定时任务并发重试治理
> ui-baseline: code-copilot/changes/定时任务优化/ui-reference.md

## 1. 目标

允许受信任外部系统使用独立服务账号 Token 查询允许访问的任务、触发执行并查询执行状态，不暴露内部 Handler 协议。

## 2. 功能范围

- [x] 新建 `/openapi/v1/jobs/**` 和 `/openapi/v1/executions/**`，与管理接口和 `/job/executor/execute` 完全隔离。
- [x] Token 使用高强度随机值，只在创建或轮换响应中显示一次，数据库只保存 Key ID、前缀和 HMAC Hash。
- [x] 首期只支持服务账号 Token。
- [x] Scope 区分 `jobs:read`、`jobs:trigger`、`executions:read`。
- [x] Token 资源范围限制到任务 ID 或任务组，Scope 通过不代表拥有所有任务。
- [x] 触发请求必须携带 `Idempotency-Key`，同一 Token、任务和 Key 在 24 小时内返回同一执行 ID。
- [x] 使用 Redisson 串行化同幂等键请求并按 Token Key ID 限流；Redis 不可用时开放触发接口失败关闭。
- [x] 返回真实 HTTP 401、403、409、429、503 和 Forge 统一响应，不暴露 Bean、Handler、堆栈或数据库信息。
- [x] Token 支持启用、吊销、过期、最后使用时间和调用方说明。
- [x] 管理端提供独立 Token 工作台，创建或轮换后只在一次性结果弹窗展示明文。
- [x] 管理端提供常驻调用说明，并在一次性 Token 弹窗中生成按授权 Scope 可直接复制的 cURL 示例。

## 3. 明确不做

- 不复用 MCP Token、用户登录 Token 或内部执行器端点。
- 不支持用户委托 Token、OAuth 授权码或动态客户端注册。
- 不允许外部调用方覆盖 tenantId、operatorId、executorBean、handler 或 service。
- 不包含 Webhook、任意 URL 调用或 Flowable 技术流程。

## 4. 数据变更

| 表 | 作用 |
|---|---|
| sys_job_api_token | Token HMAC Hash、Scope、JSON 资源范围、状态、过期和吊销信息 |
| sys_job_api_idempotency | Token、任务、幂等键 Hash、执行 ID 和过期时间 |

表必须包含 Forge 标准审计字段、`tenant_id`（默认租户数据为 `1`）、逻辑删除字段和必要唯一索引。明文 Token 和原始幂等键禁止落库和日志。

开放触发先创建状态为“已接受”的 `sys_job_log` 记录，再把执行 ID 透传给 Quartz；Quartz 启动后把同一记录推进到运行中和终态，不另建第二套执行日志。

## 5. 接口

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | /openapi/v1/jobs | 查询 Token 有权访问的任务 |
| GET | /openapi/v1/jobs/{id} | 查询任务摘要 |
| POST | /openapi/v1/jobs/{id}/executions | 幂等触发任务 |
| GET | /openapi/v1/executions/{id} | 查询执行状态 |

管理端另提供 Token 创建、列表、吊销和轮换接口，均需要独立资源权限和操作审计。

## 6. 安全规则

- Token 比对使用恒定时间 Hash 校验。
- Idempotency-Key 长度和字符集受限，不能直接作为 Redis 或 SQL 拼接片段。
- 执行权限实时计算 Token Scope 与资源范围交集。
- 外部接口只能触发启用且已同步的任务，不能覆盖任务参数或任何执行目标字段。
- 开放接口日志只记录 Key ID、任务 ID、执行 ID、结果码和耗时。
- 默认限流维度为 Token Key ID，不以客户端自报 Header 为身份。
- 开放接口只返回任务和执行摘要，禁止返回 `executorBean`、`executorHandler`、`executorService`、任务参数、结果正文或异常正文。

## 7. 验收标准

- 明文 Token 只能在创建响应出现一次。
- 越权任务返回 403，重复幂等请求返回同一执行 ID。
- Redis 故障时不产生无幂等执行。
- 吊销或过期 Token 立即失效。
- 响应和日志不暴露内部执行目标及敏感字段。

## 8. 确认门禁

- [x] 确认首期只支持服务账号 Token。
- [x] 确认 Token 资源范围只支持任务 ID 和任务组。
- [x] 确认 Redis 不可用时开放触发失败关闭。

## 9. 实施决策

- Token 格式固定为 `fja_<keyId>_<secret>`，随机部分使用 `SecureRandom` 和 URL-safe Base64；Hash 使用 `HmacSHA256` 与独立 Pepper。
- Scope 使用空格分隔的标准 Token 表达，任务 ID 和任务组使用 JSON 数组存储并通过 Jackson 解析，禁止手工拼接 SQL。
- `Idempotency-Key` 只接受 8-128 位字母、数字及 `._:-`，数据库和 Redis 均只使用其 SHA-256 Hash。
- 开放接口通过独立认证服务处理 Bearer Token；通用 Sa-Token 和 API 资源拦截器只对精确的开放任务路径放行。
- Token 管理接口继续使用 Sa-Token、细粒度权限、API 加解密和操作审计，创建与轮换审计禁止保存响应正文。
- 真实 MySQL、Redis、Quartz 和 Admin 联调由用户按既有分工执行；本阶段完成单元/契约测试、静态检查和聚合构建。

## 10. 完成结论

- Job 模块 `137/137`、前端全量 `463/463`、Admin Reactor `42/42` 通过。
- V7 目标 ESLint、前端生产构建、Mapper XML、Flyway placeholder、敏感字段和 `git diff --check` 静态检查通过。
- V8 示例生成与组件测试 `7/7`、目标 ESLint 和前端生产构建通过，生成命令已确认包含 Bearer Token 与 `Idempotency-Key`。
- 真实 Flyway、Bearer 请求、Redis 并发和 Quartz 端到端执行未自动启动，保留为用户侧环境验收项，不计入本阶段自动化通过结论。

## 11. V8 调用示例增量

- 服务账号工作台增加常驻“调用说明”入口，说明 Admin 服务地址、Bearer Token、Scope、任务资源和常见 HTTP 状态。
- 创建或轮换成功后，在 Token 仅展示一次的同一弹窗内提供可复制 cURL，避免用户保存 Token 后仍不知道调用路径。
- 示例只展示当前 Token Scope 允许的接口；触发任务示例必须携带 `Idempotency-Key`，查询示例保留任务 ID、执行 ID 占位符。
- 示例生成逻辑使用纯函数并由 Vitest 覆盖；本轮不修改开放 API 后端协议、鉴权、限流或数据结构。
