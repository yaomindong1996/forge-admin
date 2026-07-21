# 定时任务扫描问题修复执行记录

> version: V10
> status: complete
> baseline-created: 2026-07-21

## 2026-07-21 实施启动

- 变更范围：整体扫描确认的执行器安全、RPC 出站、日志生命周期、调度一致性、Flow 恢复和前端契约问题。
- 基线：复用 V9 Job `48/48`、Flow Client `6/6`、前端 `25/25` 及 Admin/Flow 聚合构建成功记录。
- 工作区：保留 V1-V9 大量未提交成果，只叠加 V10，不回退无关改动。
- 分工：代码、目标测试、静态和聚合构建由本轮执行；真实 MySQL、Redis、Flyway、Quartz、RPC、登录态 UI 和 Flow E2E 由用户侧验收。
- 服务：实施启动阶段未启动真实服务，无 PID 需要清理。

## 2026-07-21 核心修复

- 执行器与 RPC：端点默认关闭并使用专用 Bearer Token；RPC 进入 `SecureOutboundClient/JOB_RPC`，双重校验 HTTP 与 `RespInfo.code`，不再使用 Hutool 直连。
- 日志生命周期：清理仅处理终态并保护有效幂等引用；执行期间刷新心跳，启动时幂等终结超时 RUNNING/ACCEPTED。
- 调度一致性：配置同步使用 Redis 任务级锁、版本条件更新和最多 10 次重新收敛；过去的 ONCE + DO_NOTHING 直接结束；并行完成统计按完成时间和 executionId 原子推进。
- Flow 与前端：Flow 5xx、传输和解析错误按 businessKey 恢复，4xx 和明确业务失败不恢复；监控补 ACCEPTED；FLOW 标签读取调用模式字典；流程历史入口按真实访问路由显示并在跳转前再次校验。

## 2026-07-21 自动化验证

- `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-job -Penable-tests -Dforge.test.groups= test`：通过，`178/178`。
- `mvn -pl forge-framework/forge-starter-parent/forge-starter-outbound -Penable-tests -Dforge.test.groups= test`：通过，`48/48`。
- `mvn -pl forge-flow/forge-flow-client -Penable-tests -Dforge.test.groups= -Dtest=RemoteJobFlowExecutorTest test`：通过，`9/9`。
- `pnpm exec vitest run src/views/system/job-config/__tests__ src/views/system/__tests__`：通过，`29/29`。
- `mvn -pl forge-admin-server -am package -DskipTests`：通过，Admin Reactor `43/43`。
- `mvn -pl forge-flow/forge-flow-server -am package -DskipTests`：通过，Flow Reactor `32/32`。
- `NODE_OPTIONS=--max-old-space-size=8192 pnpm build`：通过。
- Java 命令显式使用 `/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home`。

## 2026-07-21 静态门禁

- V1.0.49 Flyway placeholder、`tenant_id=0` 和版本号唯一性检查：通过。
- Job 生产代码 Hutool/直连 HTTP 扫描：通过。
- Job Mapper XML `xmllint --noout`：通过。
- 相关前端文件 ESLint：通过。
- `git diff --check`：通过。

## 警告与跳过项

- Maven 保留仓库既有 deprecated/unchecked 与 Lombok Builder 警告；前端保留组件重名、动态/静态导入混用和 CSS `//` 注释警告，均未导致构建失败且不属于 V10 改动。
- 未启动真实 MySQL、Redis、Admin、Flow、Vite 或远程执行器，无服务 PID 需要清理。
- 真实 Flyway、JOB_RPC 私网白名单与 Token、Redis/Quartz 集群竞态、RPC 调用、登录态 UI 和 Flow E2E 由用户侧环境验收。
- 未创建提交、未 Push、未清理或回退 V1-V9 工作区成果。

## 2026-07-21 启动故障增量修正

- 现象：`JobExecutionHeartbeatService` 构造器要求不存在的 `ScheduledExecutorService` Bean，导致 Admin 启动失败。
- 根因：生产用双参数构造器会自行创建模块专用 daemon 调度器，但 `@Autowired` 误标在仅供测试注入调度器的三参数构造器上。
- 修复：把 Spring 注入入口切换到双参数构造器；三参数构造器继续保留为包级测试注入入口，不向应用上下文增加通用线程池 Bean。
- 回归：新增无 `ScheduledExecutorService` Bean 的 `AnnotationConfigApplicationContext` 装配测试，目标测试 `2/2`、Job 全模块 `178/178`、Admin Reactor `43/43` 通过。
