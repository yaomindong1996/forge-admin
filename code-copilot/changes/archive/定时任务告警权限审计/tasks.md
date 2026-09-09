# 定时任务告警权限审计 Implementation Plan
> status: complete
> scope: V6 only

## 进度

| Task | 状态 | 完成度 |
|---|---|---|
| Task 1 告警字段和权限资源 | complete | 2/2 |
| Task 2 消息中心告警 | complete | 3/3 |
| Task 3 权限二次校验 | complete | 3/3 |
| Task 4 操作审计 | complete | 2/2 |
| Task 5 前端权限和告警配置 | complete | 3/3 |
| Task 6 验证 | complete | 3/3 |

## Task 1: 告警字段和权限资源

- **涉及文件**:
  - Create: forge-server/db/migration/V<next>__add_job_alarm_permissions.sql
  - Modify: entity/SysJobConfig.java
- [x] 增加站内信/邮件告警配置。
- [x] 创建配置、执行、日志和敏感详情权限资源。

## Task 2: 消息中心告警

- **涉及文件**:
  - Create: service/JobFailureAlarmService.java
  - Modify: service/JobExecutionLifecycleService.java
- [x] 只在最终失败后调用消息中心。
- [x] 站内信和邮件失败不覆盖任务结果。
- [x] 测试一次执行只发送一次告警。

## Task 3: 权限二次校验

- **涉及文件**:
  - Modify: controller/JobConfigController.java、JobLogController.java
  - Modify: service/impl/SysJobConfigServiceImpl.java、SysJobLogServiceImpl.java
- [x] 移除统一 ApiPermissionIgnore + assertAdmin。
- [x] Controller 使用资源权限。
- [x] Service 保留保护任务和敏感字段二次校验。

## Task 4: 操作审计

- **涉及文件**:
  - Modify: controller/JobConfigController.java、JobLogController.java
- [x] 新增、编辑、删除、启停、触发、同步和清理增加 OperationLog。
- [x] 审计内容不包含任务参数和完整结果。

## Task 5: 前端权限和告警配置

- **涉及文件**:
  - Modify: forge-admin-ui/src/views/system/job-config.vue
  - Modify: forge-admin-ui/src/views/system/job-config/components/JobConfigWorkbench.vue（若 V3 已实施）
- [x] 按权限显示管理动作。
- [x] 配置站内信/邮件渠道和收件人。
- [x] 不展示 Webhook 配置。

## Task 6: 验证

- [x] 创建 test-spec.md 和 execution-log.md。
- [x] 验证权限矩阵、告警去重、发送失败和操作审计。
- [x] 运行后端测试、前端 Lint/构建和浏览器检查。
