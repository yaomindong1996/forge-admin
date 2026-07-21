# 定时任务可靠性加固
> status: complete
> created: 2026-07-19
> complexity: 🟡中等
> parent: code-copilot/changes/定时任务优化/spec.md
> version: V1
> ui-baseline: code-copilot/changes/定时任务优化/ui-reference.md

## 1. 背景与目标

本版本只修复现有 Cron 调度中心的正确性和安全缺陷，不增加一次性任务、独立时区、并发策略、告警、开放 API 或流程编排。

完成后应满足：

- 管理端保存成功时，数据库期望状态和 Quartz 运行态差异可见、可手动重试。
- 服务启动时可以按数据库恢复缺失的 Quartz 任务，不删除非 Forge 管理任务。
- job_name/job_group 创建后不可编辑，避免遗留旧 JobKey。
- Cron、JSON 参数和 BEAN/HANDLER/RPC 执行目标由后端权威校验。
- 任务和日志查询进入 Mapper XML，日志时间筛选真实生效。
- 日志参数、结果和异常受控脱敏、限长，调度失败不再静默返回 false。
- job 模块建立可重复运行的单元测试基线。

## 2. 代码现状

- SysJobConfigServiceImpl.addJob 先写数据库再注册 Quartz，且事务注解被注释。
- SysJobConfigServiceImpl.updateJob 使用请求中的新 JobKey 更新 Quartz，改名可能遗留旧任务。
- JobScheduler 捕获所有异常并返回 boolean，事务和上层无法区分失败原因。
- JobConfigLoader 的 Component 注解被注释，数据库任务启动恢复未启用。
- JobAutoRegistrar 只在数据库没有记录时注册，不恢复缺失 Quartz 任务，也不按 job_group 精确匹配。
- 任务和日志分页仍在 Service 使用 LambdaQueryWrapper。
- QuartzJobExecutor 和 JobMonitor 会记录任务参数与结果，缺少结构化脱敏。

## 3. 功能范围

- [x] 引入 JobConfigQuery、JobConfigSaveRequest、JobConfigVO，Controller 不再直接接收或返回 Entity。
- [x] 增加 Cron、JSON 参数、执行目标和不可编辑 JobKey 校验。
- [x] 将任务分页、任务详情、启动恢复候选和日志分页迁移到 Mapper XML。
- [x] JobScheduler 改为抛出明确业务异常，不再用 false 表达调度异常。
- [x] sys_job_config 仅增加 sync_status、sync_error、sync_time、version。
- [x] 数据库作为期望状态；管理命令提交后同步 Quartz，失败标记 FAILED 并提供手动重试。
- [x] 启动时执行一次幂等对账，只恢复或更新 Forge 管理的数据库任务。
- [x] 注解任务只登记数据库期望配置，再复用统一同步入口。
- [x] sys_job_log 增加 job_config_id、trigger_type，区分 UNKNOWN、SCHEDULED、MANUAL。
- [x] 日志查询支持真实时间范围，参数、结果和异常脱敏限长。
- [x] 补齐配置校验、同步、恢复、日志查询和脱敏测试。

## 4. 明确不做

- 不增加 60 秒周期对账；启动恢复和手动同步足够覆盖本版本。
- 不引入 Redisson 调度写锁；使用乐观版本和幂等同步控制管理端低频写入。
- 不改变现有超级管理员管理边界，不建设细粒度角色模板。
- 不增加一次性任务、任务时区、Cron 向导、重试、并发、Misfire 或告警。
- 不创建任务类型、流程调用方式等占位字段。

## 5. 数据变更

| 表 | 字段/索引 | 说明 |
|---|---|---|
| sys_job_config | sync_status、sync_error、sync_time、version | 最小同步可见性和乐观锁 |
| sys_job_config | idx_job_sync_status_del | 同步失败查询 |
| sys_job_log | job_config_id、trigger_type | 稳定关联任务并区分计划/手动 |
| sys_job_log | idx_job_log_config_trigger | 任务日志查询 |

迁移必须保持既有 0/1 状态语义，并使用 information_schema 或 NOT EXISTS 防重复。

## 6. 接口变更

| 接口 | 变更 |
|---|---|
| GET /job/config/page | 使用 Query/VO，返回同步状态 |
| GET /job/config/:id | 返回 VO |
| POST/PUT /job/config | 使用 SaveRequest 和服务端校验 |
| POST /job/config/:id/sync | 手动重试数据库到 Quartz 同步 |
| GET /job/log/page | 支持任务、状态和时间范围筛选 |

## 7. 风险与验证

- Quartz 与业务表无法共享本地事务，本版本通过期望状态、失败可见和幂等重试收敛。
- 启动对账必须只处理带 Forge 管理标记或数据库匹配的 JobKey，禁止清理 Quartz 框架或其它模块任务。
- 验证覆盖新增、编辑、启停、删除、手动触发、Quartz 故障、重启恢复和日志脱敏。

## 8. 确认门禁

- [x] 确认本版本保持平台超级管理员控制面。
- [x] 确认不增加 owner_org_id 和普通租户自助任务。
- [x] 确认 V1 只做 Cron 可靠性，不包含一次性任务和独立时区。

## 9. 执行日志

| Task | 状态 | 实际改动文件 | 备注 |
|---|---|---|---|
| Proposal | complete | spec.md、tasks.md | 从原单体方案拆出 |
| Implementation | complete | Task 1-7 已完成 | V1 代码、静态检查、模块测试和聚合构建完成 |
| Task 1 最小数据库迁移 | complete | V1.0.40 迁移、迁移契约测试、测试依赖 | 契约测试 7/7 通过，占位符扫描与 diff 检查通过 |
| Task 2 协议、校验和 XML 查询 | complete | DTO/VO、校验器、实体字段、Mapper/XML、分页接口 | job 模块测试 16/16 通过，XML 与格式检查通过 |
| Task 3 Quartz 异常和幂等同步 | complete | JobScheduler、JobScheduleCoordinator、Service/Controller 写链路 | job 模块测试 21/21 通过，调度失败状态可见且可重试 |
| Task 4 启动恢复和注解登记 | complete | JobAutoRegistrar、JobStartupReconciler、删除旧 JobConfigLoader、恢复契约测试 | job 模块测试 26/26 通过，缺失任务可恢复且不清理非 Forge Job |
| Task 5 日志筛选和安全裁剪 | complete | JobLogSanitizer、JobMonitor、QuartzJobExecutor、日志读取清洗、时间筛选格式 | job 模块测试 33/33 通过，前端定向 ESLint、XML 和格式检查通过 |
| Task 6 Controller 和前端同步状态 | complete | 同步重试接口、保存失败提示、任务列表信息架构和同步操作入口 | job 模块测试 35/35、前端定向 ESLint 和生产构建通过；真实数据 UI 联调留待开发环境 |
| Task 7 回归验证 | complete | 手动触发同步门禁、最终模块/聚合/前端回归、文档收尾 | job 测试 37/37、Admin 42/42、前端构建通过；浏览器联调受沙箱端口权限阻断 |

## 10. 确认记录

- **确认时间**：2026-07-19
- **确认内容**：按路线图从 V1 开始；保持平台超级管理员控制面；不增加 owner_org_id、普通租户自助任务、一次性任务或独立时区。
