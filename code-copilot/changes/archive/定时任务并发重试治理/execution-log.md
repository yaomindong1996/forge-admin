# 定时任务并发重试治理执行记录
> status: completed

## 2026-07-20 基线

- 用户确认继续执行路线图下一步，V4 按 `定时任务并发重试治理` 进入 apply。
- 依赖 V1、V3 已完成；V2 功能完成但真实浏览器 UI 验收仍由用户执行。
- V3 基线：Job 模块 63/63、前端定向单测 10/10、Admin Reactor 42/42 和前端生产构建通过。
- 当前最大 Flyway 版本为 V1.0.42，V4 迁移使用 V1.0.43。
- 确认语义：SKIP_IF_RUNNING 立即跳过不排队；只有显式幂等任务允许有限重试；Misfire 只支持 FIRE_ONCE_NOW、DO_NOTHING。
- 本轮默认不启动真实 MySQL、Redis、Admin 或 Quartz 集群，不把可重复单元测试表述为真实多实例 E2E。
- 工作区已有 V1-V3 未提交改动并与 V4 共用 Job 核心文件；为避免把既有改动误归入独立提交，本轮不自动提交或推送。

## 2026-07-20 Task 1 策略字段和枚举

- 新增并发策略 `ALLOW/SKIP_IF_RUNNING`、Misfire 策略 `FIRE_ONCE_NOW/DO_NOTHING` 和日志执行状态常量。
- 配置 DTO、Entity、Model、VO 增加策略与幂等字段；日志 Entity、Model 增加计划触发时间和 Quartz 执行实例 ID。
- 新增 `V1.0.43__add_job_execution_policies.sql`，使用 `information_schema` 和 `WHERE NOT EXISTS` 保护字段、存量默认值和字典数据。
- 校验规则：空策略归一为 `ALLOW/DO_NOTHING`，重试次数限制为 0-5，非幂等任务禁止自动重试。
- 定向测试：`JobExecutionPolicyMigrationContractTest,JobConfigValidatorTest`，17/17 通过。
- 首次实现后测试编译发现测试变量不满足 effectively-final，拆分独立请求变量后重跑通过；主代码编译无错误。

## 2026-07-20 Task 2-5 执行治理

- 执行生命周期：接受触发时插入 `RUNNING`，终态 SQL 仅允许 `status=2` 的原记录转成功、失败或跳过；生命周期与 Mapper 契约测试 5/5 通过。
- 分布式并发：`SKIP_IF_RUNNING` 使用 `forge:job:execution:<jobConfigId>` 零等待 Redisson 锁；竞争、Redis 缺失和异常均不放行业务执行；锁与重试测试 7/7 通过。
- 有限重试：固定 1000ms 退避、最多 5 次，非幂等任务在执行层强制不重试；远程 RPC 路由内部重试已移除，避免嵌套放大。
- Quartz 集成：手动/计划触发复用同一治理链路，ONCE 在成功、失败和跳过后均执行完成态；执行入口与 Misfire 定向测试 14/14 通过。
- Misfire：CRON 映射 `FireAndProceed/DoNothing`，ONCE 映射 `FireNow/NextWithRemainingCount`。
- 前端：高级设置增加并发、Misfire、幂等与重试策略；日志和最近结果改用 `sys_job_log_status`；表单与 Cron 定向测试 13/13 通过。

## 2026-07-20 最终验收

- 收尾修正：失败执行记录保存经脱敏和 4000 字符限长的异常堆栈；Redisson Provider 获取异常也按失败关闭处理；清理 `application-job-example.yml` 末尾失效注释。
- 可重复竞争测试：两个 `JobExecutionLockManager` 共享同一测试锁状态，首个管理器持锁时第二个立即返回竞争，释放后可以重新获取；该测试不等同于真实 Redis 多实例 E2E。
- 定向后端：Java 17 下执行 `mvn -Penable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-job -am -Dtest=JobExecutionLifecycleServiceTest,JobExecutionLockManagerTest -Dsurefire.failIfNoSpecifiedTests=false test`，5/5 通过，Reactor 14/14 成功。
- Job 全量：Java 17 下执行 `mvn -Penable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-job -am test`，84/84 通过，Reactor 14/14 成功。
- 前端单测：Node 20.19.0 下执行 `pnpm exec vitest run src/views/system/job-config/__tests__/job-config-form.test.js src/components/job/__tests__/cron-builder.test.js`，13/13 通过。
- 前端 Lint：Node 20.19.0 下对 Job API、工作台、计划组件、日志页面和两组测试执行定向 `pnpm exec eslint`，通过且无输出。
- 前端构建：Node 20.19.0 下执行 `NODE_OPTIONS=--max-old-space-size=8192 pnpm build`，通过；保留仓库既有组件命名、动态/静态导入和 CSS 注释警告。
- Admin 聚合：Java 17 下执行 `mvn -pl forge-admin-server -am package -DskipTests`，Reactor 42/42 成功；保留既有 deprecation 和 unchecked 警告。
- 静态检查：两个 Job Mapper XML 通过 `xmllint --noout`；V1.0.43 无 Flyway `${...}` 占位符；Job Service 无 `LambdaQueryWrapper/lambdaQuery`；冲突标记扫描和 `git diff --check` 通过。
- 过程说明：首次收尾命令因显式 `PATH` 未包含 Maven 而未启动；首次堆栈断言因 Mockito 混用匹配器失败，统一参数匹配器后定向和全量测试均通过。
- 跳过项：未启动真实 MySQL、Redis 集群、Admin 后端或浏览器自动化；真实 Flyway 执行和 Redis 多实例竞争需在部署环境验收。
- 启停服务：无。
- 提交：未创建。V1-V3 未提交改动与 V4 共用 Job 核心文件，当前不适合生成边界不清的独立提交。
