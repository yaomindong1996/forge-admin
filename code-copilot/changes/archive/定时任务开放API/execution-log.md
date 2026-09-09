# 定时任务开放 API 执行记录

> version: V7
> status: complete
> baseline-created: 2026-07-20

## 2026-07-20 实施启动

- 变更范围：V7 Spec、实施任务和增量测试基线。
- 结果：三个安全门禁已确认，`spec.md` 已切换为 `apply`，`tasks.md` 已修正为 V7。
- 基线：复用 V6 Job `106/106`、前端 `18/18`、Admin Reactor `42/42` 成功记录。
- 警告：工作区包含 V1-V6 未提交改动，本阶段仅叠加 V7，不回退、不重写历史成果。
- 服务：本轮尚未启动 MySQL、Redis、Admin、Quartz 或 Vite。

## 2026-07-20 V7 实施完成

- 数据库：新增 `V1.0.46__add_job_open_api_credentials.sql`，创建 Token 和幂等表、逻辑删除生成列唯一键、Scope/状态字典、细粒度权限资源及隐藏路由；业务内置数据使用 `tenant_id=1` 和防重复写入。
- 后端：新增专用 Bearer Token 认证、HMAC Hash、Scope/资源交集授权、Redisson 限流与幂等锁、预留执行 ID 和真实 HTTP 错误边界；Redis 故障失败关闭，Quartz 复用预留日志推进执行生命周期。
- 安全：数据库不保存明文 Token 或原始幂等键；创建和轮换只在一次性加密响应返回 Token，审计不保存响应正文；开放响应只投影任务和执行摘要。
- 前端：新增 Token 管理工作台，支持分页筛选、创建、吊销、轮换、资源选项、字典 Scope 和一次性 Token 复制；关闭弹窗后清除明文状态，入口和操作按权限裁剪。

## 2026-07-20 自动化验证

- Java 17：`mvn -Penable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-job -am test`，Job `137/137` 通过，0 failure、0 error、0 skipped，Reactor `BUILD SUCCESS`。Quartz 测试中的预期异常日志来自故障分支断言，不是测试失败。
- Java 17：`mvn -pl forge-admin-server -am package -DskipTests`，Admin Reactor `42/42` 模块成功，`BUILD SUCCESS`，生成 `forge-admin-server.jar`；既有 `EmployeeServiceImpl` deprecated API 提示不阻断。
- Node 20.19.0：`pnpm test`，57 个测试文件、`463/463` 通过，V7 `job-api-token.test.js` 为 `3/3`。流程设计器组件 stub 和 Sass legacy API 警告不阻断。
- Node 20.19.0：V7 目标 `pnpm exec eslint` 无输出通过。
- Node 20.19.0：`NODE_OPTIONS=--max-old-space-size=8192 pnpm build` 通过，8721 个模块完成转换，生成 `job-api-token` 产物；保留仓库既有组件命名冲突、动态/静态导入、CSS 注释和大包体提示。
- 静态检查：`rg -n '\$\{[^}]+\}' forge-server/db/migration` 无输出；四个 Job Mapper XML 通过 `xmllint --noout`；V1.0.46 版本唯一；敏感字段扫描未发现明文 Token 持久化或开放响应泄露；`git diff --check` 通过。
- 服务清理：本轮未启动 MySQL、Redis、Admin、Quartz、Vite 或其它常驻服务，无需清理端口或 PID。
- 用户侧验收：真实 Flyway、Bearer 请求、Redis 并发竞争和 Quartz 端到端执行按既有分工由开发环境验证，本轮不将这些未执行项表述为已通过。

## 2026-07-21 V8 调用示例实施启动

- 变更范围：服务账号工作台常驻调用说明、一次性 Token 弹窗 Scope 感知 cURL、示例生成单元测试。
- 复用基线：V7 前端 `463/463`、目标 Token helper `3/3`、生产构建通过。
- 不变范围：开放 API 后端协议、认证、限流、幂等、数据库和权限资源不变。
- 环境：本轮不启动 Admin、MySQL、Redis、Quartz 或 Vite 常驻服务。

## 2026-07-21 V8 调用示例实施完成

- 前端：服务账号工作台增加常驻“调用说明”按钮；创建和轮换成功弹窗复用同一说明组件，并按当前 Scope 裁剪查询任务、触发任务和查询执行结果示例。
- 安全：实际 Token 只进入既有 `issuedToken` 一次性内存状态，示例不写浏览器存储或日志；关闭弹窗后继续由 `clearIssuedToken()` 清理。常驻说明使用 `${TOKEN}` 环境变量占位并提示从 Secret/环境变量读取。
- 协议：触发 cURL 带 `Authorization: Bearer`、`${JOB_ID}` 和 `Idempotency-Key: $(uuidgen)`；服务地址可编辑并自动去除末尾 `/`。
- TDD：新增用例首次执行得到 `2 failed / 3 passed`，失败原因为 `buildJobOpenApiExamples is not a function`；实现后两个目标测试文件最终 `2/2`、`7/7` 通过。
- ESLint：Node 20.19.0 下对六个目标文件执行 ESLint，无输出通过。
- 构建：Node 20.19.0 下执行 `NODE_OPTIONS=--max-old-space-size=8192 pnpm build`，8725 个模块完成转换，`BUILD` 成功并生成 `job-api-token` 产物。
- 警告：保留仓库既有组件命名冲突、动态/静态导入、CSS `//` 注释和大包体提示，均不由本轮引入且不阻断。
- 静态检查：目标生成命令人工输出核对通过；敏感信息扫描无命中；`git diff --check` 及新增文件逐项空白检查无输出。
- 服务清理：本轮未启动 Admin、MySQL、Redis、Quartz、Vite 或其它常驻服务，无需清理端口或 PID。
