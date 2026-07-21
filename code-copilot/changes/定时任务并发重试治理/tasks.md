# 定时任务并发重试治理 Implementation Plan
> status: completed
> scope: V4 only

## 进度

| Task | 状态 | 完成度 |
|---|---|---|
| Task 1 策略字段和枚举 | completed | 2/2 |
| Task 2 执行生命周期 | completed | 1/1 |
| Task 3 分布式并发控制 | completed | 3/3 |
| Task 4 有限重试 | completed | 3/3 |
| Task 5 Misfire 适配 | completed | 2/2 |
| Task 6 验证 | completed | 3/3 |

## Task 1: 策略字段和枚举

- **涉及文件**:
  - Create: forge-server/db/migration/V<next>__add_job_execution_policies.sql
  - Create: constant/JobConcurrentPolicy.java、JobMisfirePolicy.java
  - Modify: entity/SysJobConfig.java、entity/SysJobLog.java
- [x] 增加并发、Misfire、幂等和执行实例字段。
- [x] 保持日志 0=失败、1=成功兼容。

## Task 2: 执行生命周期

- **涉及文件**:
  - Create: service/JobExecutionLifecycleService.java
  - Modify: mapper/SysJobLogMapper.java、SysJobLogMapper.xml
- **关键签名**:
  - Long accept(JobExecutionContext context, Long jobConfigId)
  - void markSuccess(Long executionId, String result, int retryCount)
  - void markFailed(Long executionId, Throwable error, int retryCount)
  - void markSkipped(Long executionId, String reason)
- [x] 测试单记录状态流转和重复更新保护。

## Task 3: 分布式并发控制

- **涉及文件**:
  - Create: manager/JobExecutionLockManager.java
  - Modify: pom.xml
  - Test: manager/JobExecutionLockManagerTest.java
- [x] 使用 Redisson tryLock，不等待排队。
- [x] Redis 故障时受保护任务失败关闭。
- [x] 锁键包含平台任务 ID，不使用可编辑展示字段。

## Task 4: 有限重试

- **涉及文件**:
  - Create: service/JobRetryExecutor.java
  - Modify: support/JobConfigValidator.java
- [x] 非幂等任务 retryCount 大于 0 时拒绝保存。
- [x] 固定退避和最大次数受平台上限约束。
- [x] 最终结果和实际次数写回同一执行记录。

## Task 5: Misfire 适配

- **涉及文件**:
  - Modify: scheduler/JobScheduler.java
  - Test: scheduler/JobMisfirePolicyTest.java
- [x] CRON 映射 FIRE_ONCE_NOW、DO_NOTHING。
- [x] 若 V3 已实施，同步验证 ONCE 的适用规则。

## Task 6: 验证

- [x] 创建 test-spec.md 和 execution-log.md。
- [x] 运行并发、重试、Misfire 定向测试和 job 模块全量测试。
- [x] 使用两个锁管理器共享测试锁状态，可重复验证竞争立即跳过和释放后重新获取。
