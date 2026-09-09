# 定时任务开放 API 增量测试计划

> status: complete
> version: V8
> created: 2026-07-20
> updated: 2026-07-21
> baseline: V6 Job `106/106`、前端 `18/18`、Admin Reactor `42/42`

## 1. 本轮差异

- 新增服务账号 Token、HMAC Hash、Scope、任务 ID/任务组资源范围和管理权限。
- 新增开放任务查询、幂等触发和执行状态查询协议。
- 新增 Redisson 限流、幂等锁及 Redis 故障失败关闭。
- 修改 Quartz 手动触发链路，允许预留并复用执行 ID。
- 新增 Token 管理工作台和一次性明文展示。

## 2. P0 验证

| 编号 | 场景 | 预期 |
|---|---|---|
| P0-01 | Token 创建与轮换 | 明文只在响应 VO 出现，数据库实体/SQL 无明文字段，旧 Token 立即失效 |
| P0-02 | Token 认证 | 格式错误、Hash 错误、过期、吊销均返回 401，比较使用恒定时间方法 |
| P0-03 | Scope 与资源授权 | 缺 Scope 或越权任务返回 403，ID/组范围取并集且不允许空范围 |
| P0-04 | 幂等触发 | 同 Token、任务、Key 在 24 小时内返回同一执行 ID且只提交一次 |
| P0-05 | Redis 故障 | 限流或幂等锁不可用时返回 503，不能创建执行记录或提交 Quartz |
| P0-06 | 执行生命周期 | 预留记录为 ACCEPTED，Quartz 复用同一 ID 推进 RUNNING 和终态 |
| P0-07 | 开放响应安全 | 不返回 Bean、Handler、Service、任务参数、结果正文、异常正文或堆栈 |
| P0-08 | HTTP 错误边界 | 返回真实 400/401/403/404/409/429/503 和 Forge `RespInfo` |
| P0-09 | 管理权限审计 | 创建、吊销、轮换有独立权限；创建和轮换审计不保存响应正文 |
| P0-10 | 模块与聚合构建 | Job 插件测试、Admin Reactor 测试/构建通过 |

## 3. P1 验证

| 编号 | 场景 | 预期 |
|---|---|---|
| P1-01 | 分页边界 | `pageNum >= 1`，`pageSize` 上限 100，资源过滤在 SQL 内完成 |
| P1-02 | Idempotency-Key 校验 | 缺失、过短、过长或非法字符返回 400，原文不落库不入日志 |
| P1-03 | 限流 | 维度来自已认证 Key ID，超限返回 429，不信任客户端身份 Header |
| P1-04 | 任务状态 | 仅启用且 `SYNCED` 的任务可由开放接口触发，其余返回 409 |
| P1-05 | 最后使用时间 | 按配置间隔节流且使用条件更新，失败时认证失败关闭 |
| P1-06 | 前端一次性结果 | 创建/轮换成功后展示 Token，关闭后列表和详情均无法再次获取 |
| P1-07 | 前端质量 | Vitest、ESLint 和生产构建通过，Token 页面无文本溢出和嵌套卡片 |
| P1-08 | Flyway | 版本唯一、防重复、默认租户为 1、逻辑删除唯一键语义正确 |

## 4. 执行命令

```bash
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home \
PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH \
mvn -pl forge-framework/forge-plugin-parent/forge-plugin-job -am test
```

```bash
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home \
PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH \
mvn -pl forge-admin-server -am package -DskipTests
```

```bash
source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm test
source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm exec eslint <本轮前端文件>
source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm build
```

```bash
rg -n '\$\{[^}]+\}' forge-server/db/migration
rg -n 'rawToken|Authorization|Idempotency-Key|executorBean|executorHandler|executorService|exceptionMsg|jobParam' \
  forge-server/forge-framework/forge-plugin-parent/forge-plugin-job/src/main
git diff --check
```

## 5. 跳过项

- 不自动启动真实 MySQL、Redis、Admin 或 Quartz；Flyway 实跑、真实 Bearer 请求、并发 Redis 和端到端 Quartz 执行由用户侧环境验收。
- 不修改或清理工作区中 V1-V6 的未提交成果。

## 6. 执行结果

- Job 模块：Java 17 下执行 `mvn -Penable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-job -am test`，Job `137/137` 通过，失败、错误和跳过均为 0，Reactor `BUILD SUCCESS`。
- Admin 聚合：Java 17 下执行 `mvn -pl forge-admin-server -am package -DskipTests`，`42/42` 模块成功，生成 `forge-admin-server.jar`。
- 前端全量：Node 20.19.0 下执行 `pnpm test`，57 个测试文件、`463/463` 通过；V7 Token 页面 `3/3` 通过。
- 前端质量：V7 目标 ESLint 无输出通过；`pnpm build` 生产构建通过，8721 个模块完成转换并生成 `job-api-token` 产物。
- 静态检查：V1.0.46 迁移版本唯一；Flyway placeholder 无命中；四个 Job Mapper XML 通过 `xmllint`；开放响应 VO 不含执行目标、参数、结果或异常正文；持久化只命中幂等键 Hash；`git diff --check` 通过。
- 非阻断警告：Admin 构建存在既有 `EmployeeServiceImpl` deprecated API 提示；Vitest 存在流程设计器组件 stub 和 Sass legacy API 提示；前端构建存在既有组件命名冲突、动态/静态导入、CSS 注释和大包体提示。
- 用户侧验收：真实 Flyway、Bearer 请求、Redis 并发和 Quartz 端到端执行未自动启动，结果不得表述为已通过。

## 7. V8 本轮增量验证

| 编号 | 场景 | 预期 |
|---|---|---|
| V8-P0-01 | Scope 感知示例 | 创建/轮换弹窗只展示当前 Scope 允许的调用示例，不引导用户调用必然返回 403 的接口 |
| V8-P0-02 | 触发任务示例 | cURL 同时包含 Bearer Token、任务 ID 和 `Idempotency-Key` |
| V8-P0-03 | 明文生命周期 | 真实 Token 仅存在于一次性弹窗状态，关闭后继续由 `clearIssuedToken()` 清除 |
| V8-P1-01 | 常驻说明 | 工作台可随时查看完整接口、Scope、服务地址和常见错误码，示例使用 Token 占位符 |
| V8-P1-02 | 前端质量 | 目标 Vitest、目标 ESLint、生产构建和 `git diff --check` 通过 |

```bash
source ~/.nvm/nvm.sh && nvm use v20.19.0
pnpm exec vitest run src/views/system/__tests__/job-api-token.test.js \
  src/components/job/__tests__/job-open-api-usage.test.js
pnpm exec eslint src/components/job/JobOpenApiUsageGuide.vue \
  src/components/job/job-open-api-usage.js \
  src/components/job/__tests__/job-open-api-usage.test.js \
  src/views/system/job-api-token.vue \
  src/views/system/job-api-token.js \
  src/views/system/__tests__/job-api-token.test.js
NODE_OPTIONS=--max-old-space-size=8192 pnpm build
```

本轮只修改前端调用指导，不启动 Admin、MySQL、Redis 或 Quartz，不重复执行 V7 后端与迁移验证。

## 8. V8 执行结果

- 目标测试：Node 20.19.0 下执行两个目标 Vitest 文件，`2/2` 文件、`7/7` 测试通过；覆盖 Scope 裁剪、触发幂等头、Token 占位、真实 Token 带入和服务地址更新。
- 目标质量：六个目标 Vue/JS/测试文件执行 ESLint，无输出通过。
- 生产构建：Node 20.19.0 下执行 `NODE_OPTIONS=--max-old-space-size=8192 pnpm build`，8725 个模块完成转换，`job-api-token` 产物生成，构建成功。
- 静态审查：生成的触发 cURL 包含 Bearer Token、`${JOB_ID}` 和 `Idempotency-Key: $(uuidgen)`；敏感信息扫描未发现真实 Token 持久化、浏览器存储或日志输出；空白检查无输出。
- 非阻断警告：保留仓库既有 `UserSelectModal` 命名冲突、动态/静态导入、CSS `//` 注释和大包体提示。
- 跳过项：本轮未启动 Admin、MySQL、Redis、Quartz 或 Vite，不执行真实 Bearer E2E；交互由组件测试覆盖。
