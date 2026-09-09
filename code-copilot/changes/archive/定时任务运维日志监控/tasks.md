# 定时任务运维日志监控 Implementation Plan
> status: complete
> scope: V5 only

## 进度

| Task | 状态 | 完成度 |
|---|---|---|
| Task 1 日志索引和导出配置 | complete | 2/2 |
| Task 2 日志查询和详情协议 | complete | 2/2 |
| Task 3 概览与监控聚合 | complete | 2/2 |
| Task 4 Controller 和前端 | complete | 2/2 |
| Task 5 验证 | complete | 3/3 |

## Task 1: 日志索引和导出配置

- **涉及文件**:
  - Create: forge-server/db/migration/V<next>__add_job_log_observability.sql
  - Modify: entity/SysJobConfig.java
- [x] 增加 consecutive_failures 和日志组合索引。
- [x] 创建安全导出配置并排除敏感字段。

## Task 2: 日志查询和详情协议

- **涉及文件**:
  - Create: dto/JobLogQuery.java、vo/JobLogVO.java、vo/JobLogDetailVO.java
  - Modify: mapper/SysJobLogMapper.java、SysJobLogMapper.xml
  - Modify: service/ISysJobLogService.java、SysJobLogServiceImpl.java
- [x] 完成组合筛选和安全详情裁剪。
- [x] 导出查询复用 Mapper XML。

## Task 3: 概览与监控聚合

- **涉及文件**:
  - Create: vo/JobOverviewVO.java、vo/JobMonitorSummaryVO.java
  - Modify: mapper/SysJobConfigMapper.java、SysJobConfigMapper.xml
  - Modify: mapper/SysJobLogMapper.java、SysJobLogMapper.xml
- [x] 聚合最近执行、下一次触发和连续失败。
- [x] 聚合近 24 小时执行摘要。

## Task 4: Controller 和前端

- **涉及文件**:
  - Modify: controller/JobLogController.java、JobConfigController.java
  - Create: controller/JobMonitorController.java
  - Modify: forge-admin-ui/src/views/system/job-config.vue、job-log-list.vue
- [x] 接入日志详情、导出、概览和监控接口。
- [x] 保持现有超级管理员控制边界。

## Task 5: 验证

- [x] 创建 test-spec.md 和 execution-log.md。
- [x] 验证聚合 SQL、导出字段和留存物理清理边界。
- [x] 运行后端测试、前端 Lint/构建和浏览器检查；业务页实测因后端未启动按环境边界跳过。

## Task 6: V5.1 运行日志界面优化

- **涉及文件**:
  - Modify: `forge-admin-ui/src/views/system/job-log-list.vue`
  - Modify: `forge-admin-ui/src/views/system/job-config.vue`
  - Modify: `forge-admin-ui/src/views/system/job-log-query.js`
  - Test: `forge-admin-ui/src/views/system/__tests__/job-log-query.test.js`
  - Modify: `code-copilot/changes/定时任务运维日志监控/spec.md`
  - Modify: `code-copilot/changes/定时任务运维日志监控/test-spec.md`
  - Modify: `code-copilot/changes/定时任务运维日志监控/execution-log.md`
- [x] 先为列表开始时间回退规则补充失败测试，再实现纯函数并用于表格渲染。
- [x] 合并筛选与列表工作区，压缩默认列并增加有权限行的点击反馈。
- [x] 将详情重构为右侧抽屉，按概览、时间线、摘要和折叠技术信息分层。
- [x] 完成窄屏适配、目标 Vitest、ESLint、生产构建和可执行范围内的浏览器检查。
- 不变范围：日志 API、数据库、权限码、导出协议、任务执行状态流转均不修改。
