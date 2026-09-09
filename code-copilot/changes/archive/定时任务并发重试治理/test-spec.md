# 定时任务并发重试治理测试计划
> status: completed
> updated: 2026-07-20

## 1. 验证目标

复用 V3 Job 模块 63/63、前端定向单测 10/10、Admin Reactor 42 模块和生产构建基线，增量验证 V4 的执行状态机、任务级分布式 tryLock、显式幂等重试和 Quartz Misfire 策略。

## 2. 验证矩阵

| 优先级 | 范围 | 验证方式 |
|---|---|---|
| P0 | 策略字段、日志执行字段、字典和存量回填 | JobExecutionPolicyMigrationContractTest |
| P0 | 非幂等重试拒绝、策略值和次数上限 | JobConfigValidatorTest |
| P0 | RUNNING 到 SUCCESS/FAILED/SKIPPED 单记录流转 | JobExecutionLifecycleServiceTest、Mapper XML 契约 |
| P0 | ALLOW、锁竞争立即跳过、Redis 缺失/异常失败关闭 | JobExecutionLockManagerTest |
| P0 | 固定退避、有限重试和实际重试次数 | JobRetryExecutorTest |
| P0 | 手动/计划触发统一策略、一次执行只产生一条日志 | QuartzJobExecutorTest |
| P0 | CRON/ONCE 的 FIRE_ONCE_NOW、DO_NOTHING | JobSchedulerTest |
| P1 | 配置工作台策略回显、提交和日志状态展示 | 前端定向 Vitest、ESLint、生产构建 |

## 3. 必跑命令

- Job 模块：`mvn -Penable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-job -am test`
- 前端单测：`pnpm exec vitest run src/views/system/job-config/__tests__/job-config-form.test.js src/components/job/__tests__/cron-builder.test.js`
- 前端检查：V4 定向 ESLint、`pnpm build`
- 静态检查：Mapper XML、Flyway placeholder、Service 查询规范、冲突标记、`git diff --check`
- 聚合验证：`mvn -pl forge-admin-server -am package -DskipTests`

## 4. 边界

- 不启动真实 MySQL、Redis、Admin 或 Quartz 集群；Flyway 和多实例 Redis 联调由开发环境验收。
- 分布式竞争使用两个锁管理器共享同一测试锁状态复现，不把单 JVM 测试表述为真实集群 E2E。
- 本轮不增加告警、监控、开放 API、Webhook 或 Flowable 编排。

## 5. 验证结果

- 生命周期和锁管理器收尾测试：5/5 通过；覆盖脱敏堆栈、Provider 异常失败关闭和两个锁管理器竞争。
- Job 模块全量测试：84/84 通过，Maven Reactor 14/14 成功。
- 前端定向 Vitest：13/13 通过；V4 相关文件定向 ESLint 通过。
- 前端生产构建通过；Admin Maven Reactor 42/42 成功。
- Mapper XML、Flyway placeholder、Service 查询规范、冲突标记和 `git diff --check` 通过。
- 未执行真实 MySQL、Redis 集群、Admin 服务或浏览器自动化，按第 4 节边界留待环境验收。
