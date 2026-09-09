# 定时任务一次性与时区 Implementation Plan
> status: complete
> scope: V3 only

## 进度

| Task | 状态 | 完成度 |
|---|---|---|
| Task 1 调度类型和时区迁移 | completed | 3/3 |
| Task 2 时间领域校验 | completed | 1/1 |
| Task 3 Quartz Trigger 扩展 | completed | 3/3 |
| Task 4 一次性完成语义 | completed | 3/3 |
| Task 5 全屏任务编辑工作台 | completed | 3/3 |
| Task 6 验证 | completed | 4/4 |

## Task 1: 调度类型和时区迁移

- **涉及文件**:
  - Create: forge-server/db/migration/V1.0.42__add_job_once_schedule_and_timezone.sql
  - Modify: entity/SysJobConfig.java
  - Test: migration/JobOnceTimezoneMigrationContractTest.java
- [x] 增加 schedule_type、fire_once_time、timezone 并调整 cron_expression 可空。
- [x] 回填存量 CRON 和实际默认时区。
- [x] 增加状态字典 2=已结束。

## Task 2: 时间领域校验

- **涉及文件**:
  - Create: constant/JobScheduleType.java
  - Create/Modify: service/JobScheduleDomainService.java
  - Modify: support/JobConfigValidator.java
- **关键签名**:
  - ZoneId requireZoneId(String timezone)
  - Instant resolveOnceInstant(LocalDateTime fireOnceTime, ZoneId zoneId)
- [x] 测试互斥字段、过去时间、非法时区和 DST 边界。

## Task 3: Quartz Trigger 扩展

- **涉及文件**:
  - Modify: scheduler/JobScheduler.java
  - Test: scheduler/JobSchedulerTriggerTest.java
- [x] CRON 构建带时区 CronTrigger。
- [x] ONCE 构建 SimpleTrigger。
- [x] 运行态快照返回 nextFireTime。

## Task 4: 一次性完成语义

- **涉及文件**:
  - Modify: scheduler/QuartzJobExecutor.java
  - Modify: mapper/SysJobConfigMapper.java、SysJobConfigMapper.xml
- [x] 只在计划 ONCE 触发结束后标记 COMPLETED。
- [x] 成功和失败均结束，手动触发不结束。
- [x] 测试重启恢复不会复活已结束任务。

## Task 5: 全屏任务编辑工作台

- **涉及文件**:
  - Modify: forge-admin-ui/src/views/system/job-config/components/JobConfigWorkbench.vue
  - Modify: forge-admin-ui/src/views/system/job-config/components/JobScheduleSection.vue
  - Modify: forge-admin-ui/src/views/system/job-config/components/JobSchedulePreview.vue
  - Modify: forge-admin-ui/src/views/system/job-config.vue、job-config-form.js
- [x] 根据 CRON/ONCE 显示互斥配置。
- [x] 增加时区选择和下一次触发预览。
- [x] 禁止过去的一次性时间提交。

## Task 6: 验证

- [x] 创建 test-spec.md 和 execution-log.md。
- [x] 运行后端时间/Quartz 测试和 admin 聚合构建。
- [x] 运行前端单元测试、Lint 和构建。
- [x] UI 自动化验收由用户自行执行，本阶段不启动浏览器验证。
