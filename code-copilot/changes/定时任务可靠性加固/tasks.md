# 定时任务可靠性加固 Implementation Plan
> status: complete
> scope: V1 only

## 进度总览

| Task | 状态 | 完成情况 |
|---|---|---|
| Task 1 最小数据库迁移 | complete | 3/3 |
| Task 2 DTO、VO、校验和 Mapper XML | complete | 3/3 |
| Task 3 Quartz 异常和幂等同步 | complete | 3/3 |
| Task 4 启动恢复和注解登记 | complete | 3/3 |
| Task 5 日志筛选和安全裁剪 | complete | 3/3 |
| Task 6 Controller 和前端同步状态 | complete | 3/3 |
| Task 7 回归验证 | complete | 4/4 |

## Task 1: 最小数据库迁移

- **目标**: 增加同步可见性、乐观版本和日志关联字段。
- **涉及文件**:
  - Create: forge-server/db/migration/V<next>__harden_job_scheduler_reliability.sql
  - Test: forge-server/forge-framework/forge-plugin-parent/forge-plugin-job/src/test/java/com/mdframe/forge/plugin/job/migration/JobReliabilityMigrationContractTest.java
- [x] 先写迁移契约测试并确认失败。
- [x] 编写幂等 Flyway，保持历史状态值。
- [x] 运行迁移契约测试并确认通过。

## Task 2: DTO、VO、校验和 Mapper XML

- **目标**: 隔离 Entity 协议并修复查询规范。
- **涉及文件**:
  - Create: dto/JobConfigQuery.java、dto/JobConfigSaveRequest.java
  - Create: vo/JobConfigVO.java、support/JobConfigValidator.java
  - Modify: mapper/SysJobConfigMapper.java、mapper/SysJobLogMapper.java
  - Create/Modify: resources/mapper/SysJobConfigMapper.xml、SysJobLogMapper.xml
- **关键签名**:
  - void validateCreate(JobConfigSaveRequest request)
  - void validateUpdate(JobConfigSaveRequest request, SysJobConfig current)
  - Page<JobConfigVO> selectJobPage(Page<JobConfigVO> page, JobConfigQuery query)
- [x] 先写校验和 XML 契约失败测试。
- [x] 实现最小协议与 XML 查询。
- [x] 扫描 Service，确认分页查询不再使用 LambdaQueryWrapper。

## Task 3: Quartz 异常和幂等同步

- **目标**: 用明确异常和统一同步替代 boolean 静默失败。
- **涉及文件**:
  - Modify: scheduler/JobScheduler.java
  - Create: manager/JobScheduleCoordinator.java
  - Modify: service/impl/SysJobConfigServiceImpl.java
- **关键签名**:
  - void synchronize(Long jobConfigId)
  - void retrySynchronization(Long jobConfigId)
  - void reconcileOnStartup()
- [x] 写新增、更新、暂停、删除和调度失败测试。
- [x] 实现同步状态流转 PENDING、SYNCED、FAILED、DELETE_PENDING。
- [x] 确认失败对调用方可见且可重试。

## Task 4: 统一启动恢复和注解登记

- **目标**: 只保留一个数据库到 Quartz 的调度入口。
- **涉及文件**:
  - Modify: registry/JobAutoRegistrar.java
  - Replace/Delete: loader/JobConfigLoader.java
  - Create: scheduler/JobStartupReconciler.java
- [x] 注解任务仅按 job_name + job_group 登记期望配置。
- [x] 启动后执行一次幂等恢复。
- [x] 测试缺失任务恢复、暂停状态保持和非 Forge Job 不受影响。

## Task 5: 日志筛选和安全裁剪

- **目标**: 修复时间筛选并降低敏感数据泄露风险。
- **涉及文件**:
  - Modify: entity/SysJobLog.java、monitor/JobMonitor.java、scheduler/QuartzJobExecutor.java
  - Create: support/JobLogSanitizer.java
  - Modify: service/impl/SysJobLogServiceImpl.java
- [x] 测试 Token、手机号、Authorization 等字段脱敏。
- [x] 实现参数、结果、异常的长度上限。
- [x] 验证 startTime/endTime 查询真实进入 XML SQL。

## Task 6: Controller 和前端同步状态

- **目标**: 接入新协议并提供同步失败重试入口。
- **涉及文件**:
  - Modify: controller/JobConfigController.java、controller/JobLogController.java
  - Modify: forge-admin-ui/src/views/system/job-config.vue
  - Modify: forge-admin-ui/src/views/system/job-log-list.vue
- [x] Controller 使用 DTO/VO 并保留现有路径兼容。
- [x] 列表展示同步状态和错误摘要。
- [x] 同步失败任务提供重试操作。

## Task 7: 回归验证

- [x] 读取 automated-testing-standard.md 并创建 test-spec.md、execution-log.md。
- [x] 运行 job 模块全部测试和 admin 聚合构建。
- [x] 运行前端定向检查、Lint 和生产构建。
- [x] 回填 Spec、Tasks 和执行日志。
