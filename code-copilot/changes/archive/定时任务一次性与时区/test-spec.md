# 定时任务一次性与时区测试计划
> status: complete
> updated: 2026-07-19

## 1. 验证目标

复用 V2 Job 模块 44/44、前端定向单测 7/7、Admin Reactor 42 模块和生产构建基线，增量验证 V3 的 CRON/ONCE 互斥模型、IANA 时区、DST 解析、Quartz Trigger 类型和一次性完成态。

## 2. 验证矩阵

| 优先级 | 范围 | 验证方式 |
|---|---|---|
| P0 | 迁移字段、存量回填、Cron 可空、字典数据 | JobOnceTimezoneMigrationContractTest |
| P0 | 过去时间、非法时区、互斥字段、DST 缺口与重复时间 | JobScheduleDomainServiceTest、JobConfigValidatorTest |
| P0 | CRON 显式时区、ONCE SimpleTrigger | JobSchedulerTest |
| P0 | 计划 ONCE 成功或失败后已结束，手动执行不结束 | QuartzJobExecutorTest、Mapper 契约测试 |
| P0 | 已结束任务启动恢复不复活 | JobScheduleCoordinatorTest |
| P1 | 表单转换、计划预览和列表状态文案 | 前端定向 Vitest、ESLint、生产构建 |

## 3. 必跑命令

- Job 模块：`mvn -Penable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-job -am test`
- 前端单测：`pnpm exec vitest run <V3 定向测试>`
- 前端检查：V3 定向 ESLint、`pnpm build`
- 静态检查：Mapper XML、Flyway placeholder、Service 查询规范、`git diff --check`
- 聚合验证：`mvn -pl forge-admin-server -am package -DskipTests`

## 4. 边界

- 不启动真实 MySQL、Redis、Admin 或 Quartz 集群，不执行会改变真实运行态的联调。
- 按用户要求不执行 UI 自动化、截图和浏览器验收，UI 由用户自行验证。
- 本轮不引入任务级重试、并发、Misfire 配置或 Flowable 调用。

## 5. 执行结果

- Job 模块全量测试：63/63 通过。
- 前端调度相关单元测试：10/10 通过。
- V3 定向 ESLint、前端生产构建、Admin 42 模块聚合构建通过。
- Mapper XML、Flyway placeholder、Service 查询规范、冲突标记和 `git diff --check` 通过。
- 按用户要求未执行 UI 自动化、截图或浏览器交互验收。
