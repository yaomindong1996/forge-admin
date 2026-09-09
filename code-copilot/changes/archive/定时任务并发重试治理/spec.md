# 定时任务并发重试治理
> status: complete
> created: 2026-07-19
> complexity: 🟡中等
> parent: code-copilot/changes/定时任务优化/spec.md
> version: V4
> dependency: 定时任务可靠性加固；ONCE Misfire 依赖定时任务一次性与时区
> ui-baseline: code-copilot/changes/定时任务优化/ui-reference.md

## 1. 目标

为存在重复执行和临时故障风险的任务增加明确、受控、可审计的执行策略。本版本只处理执行策略，不建设告警、监控或开放 API。

## 2. 功能范围

- [x] 并发策略支持 ALLOW、SKIP_IF_RUNNING。
- [x] SKIP_IF_RUNNING 在集群中使用任务级分布式 tryLock；上一轮未完成时本轮立即记为跳过，不排队补跑。
- [x] 执行状态扩展为失败、成功、运行中、已跳过，保留既有 0/1 兼容值。
- [x] 任务执行开始即创建或更新运行记录，结束后原记录转成功/失败。
- [x] 任务级重试只对明确声明幂等安全的任务开放。
- [x] 重试采用有限次数和固定退避，每次尝试在同一执行记录中保留次数与最终结果。
- [x] Cron Misfire 支持 FIRE_ONCE_NOW、DO_NOTHING。
- [x] Redis 不可用且任务要求 SKIP_IF_RUNNING 时失败关闭并记录跳过/失败原因。

## 3. 明确不做

- 不提供第三种没有独立语义的 Misfire 策略。
- 不自动判断业务代码是否幂等。
- 不发送站内信、邮件或 Webhook 告警。
- 不建设监控大盘、开放 API 或批量治理。

## 4. 数据变更

| 表 | 字段 | 说明 |
|---|---|---|
| sys_job_config | concurrent_policy | ALLOW/SKIP_IF_RUNNING |
| sys_job_config | misfire_policy | FIRE_ONCE_NOW/DO_NOTHING |
| sys_job_config | idempotent_flag | 是否允许自动重试 |
| sys_job_log | scheduled_fire_time | 原计划时间 |
| sys_job_log | fire_instance_id | Quartz 执行实例 |
| sys_job_log | retry_count | 实际重试次数 |

继续复用现有 retry_count 配置字段，不增加第二套重试次数字段。

## 5. 执行规则

- 手动触发和计划触发都遵循并发策略。
- 跳过必须形成日志，状态为已跳过，不能伪装成功。
- retry_count 大于 0 且 idempotent_flag 不为 1 时拒绝保存。
- Misfire 只影响计划 Trigger，不影响手动触发。
- 每次重试不得重复创建新的顶层执行记录。

## 6. 验收标准

- 双实例竞争同一任务时，SKIP_IF_RUNNING 只有一个进入业务执行。
- ALLOW 策略允许并行执行。
- Redis 故障时禁止无锁执行受保护任务。
- 非幂等任务无法开启自动重试。
- 两类 Misfire 在固定时钟测试下符合 Quartz 实际行为。

## 7. 确认门禁

- [x] 确认禁止并发语义是“跳过”，不是“排队”。
- [x] 确认只有显式幂等安全任务才能配置自动重试。
- [x] 确认 Misfire 只实现 FIRE_ONCE_NOW、DO_NOTHING。

## 8. 实施结论

- V4 功能、数据迁移、前端配置和日志展示已完成。
- Job 模块 84/84、前端定向测试 13/13、定向 ESLint、前端生产构建和 Admin 42 模块聚合构建通过。
- Mapper XML、Flyway placeholder、Service 查询规范、冲突标记和 `git diff --check` 通过。
- 真实 MySQL Flyway 执行和真实 Redis 多实例竞争保留为部署环境验收项。
