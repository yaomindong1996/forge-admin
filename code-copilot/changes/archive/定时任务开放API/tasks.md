# 定时任务开放 API Implementation Plan
> **For agentic workers:** 按本文件复用当前会话内联执行，逐项使用复选框追踪；禁止回退 V1-V6 工作区改动。

> status: complete
> scope: V8 incremental

**Goal:** 为受信任外部系统提供独立的定时任务查询、幂等触发和执行状态接口，并提供只显示一次明文的服务账号 Token 管理能力。

**Architecture:** 管理端使用 Sa-Token 和 Forge 权限体系维护只存 HMAC Hash 的 Token；开放端使用专用 Bearer 认证、Scope 与任务资源交集授权。触发请求先在 Redis 锁内预留执行日志和幂等记录，再把执行 ID 传给 Quartz，Quartz 复用同一日志完成现有执行生命周期。

**Tech Stack:** Java 17、Spring Boot 3、MyBatis-Plus/Mapper XML、Quartz、Redisson、Sa-Token、Vue 3、Naive UI、Vitest、Flyway。

---

## Task 1: Spec 与测试基线

- **涉及文件**:
  - Modify: `code-copilot/changes/定时任务开放API/spec.md`
  - Modify: `code-copilot/changes/定时任务开放API/tasks.md`
  - Create: `code-copilot/changes/定时任务开放API/test-spec.md`
  - Create: `code-copilot/changes/定时任务开放API/execution-log.md`
- [x] 固化服务账号、任务 ID/任务组资源范围和 Redis 失败关闭三个门禁。
- [x] 明确预留执行 ID、专用认证、真实 HTTP 状态和敏感字段边界。
- [x] 建立 P0/P1 增量验证矩阵和执行记录模板。

## Task 2: Token 和幂等数据模型

- **涉及文件**:
  - Create: `forge-server/db/migration/V1.0.46__add_job_open_api_credentials.sql`
  - Create: `entity/SysJobApiToken.java`
  - Create: `entity/SysJobApiIdempotency.java`
  - Create: `mapper/SysJobApiTokenMapper.java`
  - Create: `mapper/SysJobApiIdempotencyMapper.java`
  - Create: `resources/mapper/SysJobApiTokenMapper.xml`
  - Create: `resources/mapper/SysJobApiIdempotencyMapper.xml`
- [x] 创建两个含标准审计字段、`tenant_id`、`del_flag` 的表。
- [x] Token 表只包含 `token_key_id`、`token_prefix`、`token_hash`，不存在明文字段。
- [x] 幂等表只保存幂等键 SHA-256 Hash，并用生成列保证逻辑删除后的唯一键语义。
- [x] Mapper XML 显式过滤 `del_flag = 0`，开放端方法显式忽略租户插件并携带可信 `tenant_id`。

## Task 3: Token 安全服务和管理接口

- **涉及文件**:
  - Modify: `config/JobProperties.java`
  - Modify: `resources/application-job-example.yml`
  - Create: `support/JobApiTokenCodec.java`
  - Create: `service/JobApiTokenService.java`
  - Create: `controller/JobApiTokenController.java`
  - Create: `dto/JobApiTokenCreateRequest.java`
  - Create: `model/JobApiPrincipal.java`
  - Create: `vo/JobApiTokenCreatedVO.java`
  - Create: `vo/JobApiTokenVO.java`
- [x] 生成 `fja_` Token，使用 `SecureRandom`、URL-safe Base64、HMAC-SHA256 和恒定时间比较。
- [x] 创建、分页、吊销和轮换均使用租户条件和 CAS 更新；轮换在同一事务插入新 Token 并吊销旧 Token。
- [x] `last_used_at` 使用目标字段更新且节流，禁止完整实体回写。
- [x] 创建与轮换使用 `@OperationLog(saveResponseResult = false)`，响应经 API 加密且明文只出现一次。
- [x] 单元测试覆盖 Token 格式、错误 Pepper、过期、吊销、Hash 不匹配、轮换回滚和泄露契约。

## Task 4: Scope、资源授权和开放查询

- **涉及文件**:
  - Create: `constant/JobApiScopes.java`
  - Create: `service/JobApiAuthorizationService.java`
  - Add Mapper XML methods in `SysJobConfigMapper` and `SysJobLogMapper`
  - Create: `vo/JobOpenApiSummaryVO.java`
  - Create: `vo/JobOpenApiExecutionVO.java`
- [x] 只允许三个白名单 Scope，Token 至少选择一个任务 ID 或任务组。
- [x] JSON 资源数组通过 Jackson 解析，任务授权取 ID/任务组并集。
- [x] 列表、详情和执行查询 SQL 只投影安全摘要字段，不包含执行目标、参数、结果或异常正文。
- [x] 越权资源返回 403；不存在资源返回 404；客户端不能提交身份或执行目标字段。

## Task 5: 限流、幂等和预留执行生命周期

- **涉及文件**:
  - Create: `manager/JobApiRateLimitManager.java`
  - Create: `manager/JobApiIdempotencyManager.java`
  - Create: `service/JobApiExecutionService.java`
  - Modify: `constant/JobExecutionStatus.java`
  - Modify: `service/JobExecutionLifecycleService.java`
  - Modify: `scheduler/JobScheduler.java`
  - Modify: `scheduler/QuartzJobExecutor.java`
- [x] Redisson 限流以 Token Key ID 为维度；超限返回 429，Redis 不可用返回 503。
- [x] `Idempotency-Key` 限制为 8-128 位安全字符，Redis/SQL 只使用 Hash。
- [x] 同 Token、任务和幂等键在锁内复用 24 小时有效记录；过期记录先逻辑删除再创建。
- [x] 预留 `ACCEPTED` 执行日志，Quartz 启动时原子推进同一行到 `RUNNING`，提交失败则安全落为失败。
- [x] 并发测试覆盖重复请求同一执行 ID、锁竞争、唯一键竞争和 Redis 故障零执行。

## Task 6: Open API Controller 和独立错误边界

- **涉及文件**:
  - Modify: `forge-starter-auth/.../SaTokenConfig.java`
  - Create: `controller/openapi/JobOpenApiController.java`
  - Create: `controller/openapi/JobExecutionOpenApiController.java`
  - Create: `controller/openapi/JobOpenApiExceptionHandler.java`
  - Create: `support/JobOpenApiException.java`
- [x] 通用登录和 API 权限拦截器只排除 `/openapi/v1/jobs/**` 与 `/openapi/v1/executions/**`。
- [x] 每个开放接口先认证 Bearer Token，再执行 Scope、限流和资源授权。
- [x] 错误处理返回真实 HTTP 400/401/403/404/409/429/503 和 `RespInfo`，未知异常统一脱敏。
- [x] 安全日志只记录 Key ID、任务 ID、执行 ID、结果码和耗时。

## Task 7: 权限资源和 Token 管理工作台

- **涉及文件**:
  - Modify: `constant/JobPermissions.java`
  - Modify: `forge-admin-ui/src/api/system/job.js`
  - Modify: `forge-admin-ui/src/views/system/job-config/job-permission.js`
  - Modify: `forge-admin-ui/src/views/system/job-config.vue`
  - Create: `forge-admin-ui/src/views/system/job-api-token.vue`
  - Modify: `V1.0.46__add_job_open_api_credentials.sql`
- [x] 增加 Token 查询、创建、吊销、轮换按钮权限及管理 API 资源，不自动扩大普通角色权限。
- [x] 增加隐藏管理路由，并从任务工作台按权限进入。
- [x] 工作台支持分页、筛选、创建、吊销、轮换和一次性 Token 弹窗。
- [x] Scope 使用字典，任务 ID/任务组来自后端资源选项，状态不在前端硬编码。

## Task 8: 增量验证与回填

- [x] 运行 Job 插件单测并记录通过数量。
- [x] 运行 Admin Reactor 聚合测试/构建，验证 Starter Auth 装配。
- [x] 使用 Node `v20.19.0` 运行前端单测、ESLint 和生产构建。
- [x] 运行 Flyway `${...}`、明文 Token、敏感字段响应、Mapper XML 和 `git diff --check` 静态检查。
- [x] 回填 `execution-log.md`、`tasks.md` 和 `spec.md`；真实服务与数据库联调明确标记为用户侧待验收。

## Task 9: 开放 API 调用说明与可复制示例

- **涉及文件**:
  - Create: `forge-admin-ui/src/components/job/job-open-api-usage.js`
  - Test: `forge-admin-ui/src/views/system/__tests__/job-api-token.test.js`
  - Test: `forge-admin-ui/src/components/job/__tests__/job-open-api-usage.test.js`
  - Create: `forge-admin-ui/src/components/job/JobOpenApiUsageGuide.vue`
  - Modify: `forge-admin-ui/src/views/system/job-api-token.vue`
- [x] 新增失败测试，调用 `buildJobOpenApiExamples({ token, scopes })` 后断言：

```js
expect(examples.map(item => item.key)).toEqual(['list-jobs', 'trigger-job'])
expect(examples[1].command).toContain('Idempotency-Key')
expect(examples[1].command).toContain('Authorization: Bearer fja_test_token')
```

- [x] 执行 `pnpm exec vitest run src/views/system/__tests__/job-api-token.test.js`，确认新增测试先因导出不存在而失败。
- [x] 在纯函数中生成 `jobs:read`、`jobs:trigger`、`executions:read` 三类示例；传入 Scope 时只返回允许的示例，未传 Scope 时返回完整使用说明。
- [x] 新建调用说明组件，提供可编辑 Admin 服务地址、接口方法/路径、Scope 提示、cURL 复制和 401/403/429/503 说明。
- [x] 组件测试验证 Scope 裁剪、真实 Token 带入，以及修改服务地址后 cURL 实时更新。
- [x] 工作台增加常驻“调用说明”按钮；创建和轮换成功时把当前 Scope 传入一次性 Token 弹窗并展示真实 Token 示例。
- [x] 使用 Node `v20.19.0` 运行目标 Vitest、目标 ESLint 和生产构建，结果追加到 `execution-log.md`。
