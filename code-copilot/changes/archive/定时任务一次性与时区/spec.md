# 定时任务一次性与时区
> status: complete
> created: 2026-07-19
> complexity: 🟡中等
> parent: code-copilot/changes/定时任务优化/spec.md
> version: V3
> dependency: 定时任务可靠性加固、定时任务配置工作台
> ui-baseline: code-copilot/changes/定时任务优化/ui-reference.md

## 1. 目标

在可靠 Cron 调度基础上增加真正的一次性任务和每任务独立 IANA 时区，不用“每天执行的 Cron”模拟单次触发。

## 2. 功能范围

- [x] 调度类型支持 CRON、ONCE。
- [x] CRON 任务保存 cron_expression，fire_once_time 必须为空。
- [x] ONCE 任务保存 fire_once_time，cron_expression 必须为空。
- [x] 新增或编辑 ONCE 时拒绝过去时间。
- [x] 每个任务保存 IANA timezone，默认 Asia/Shanghai。
- [x] CronTrigger 显式使用任务时区；ONCE 使用 SimpleTrigger。
- [x] 计划触发的一次性任务执行结束后进入 2=已结束，成功和失败都不自动再次执行。
- [x] 手动执行一次不改变 CRON 或 ONCE 的计划状态。
- [x] 前端在调度类型、时区和时间配置增多后使用独立任务编辑工作台。

## 3. 明确不做

- 不增加任务级重试、并发策略或可配置 Misfire。
- 不处理 Flowable 调用方式。
- 不自动重排执行失败的一次性任务。
- 不允许批量迁移历史 Cron 为 ONCE。

## 4. 数据变更

| 表 | 字段 | 说明 |
|---|---|---|
| sys_job_config | schedule_type varchar(20) | CRON/ONCE |
| sys_job_config | fire_once_time datetime | 一次性本地时间，必须与 timezone 一起解释 |
| sys_job_config | timezone varchar(64) | IANA 时区 |
| sys_job_config | cron_expression 改为可空 | ONCE 不保存 Cron |
| sys_job_config.status | 新增值 2 | 已结束 |

存量任务全部回填 CRON；时区必须按现网 JVM/Quartz 实际默认时区回填，不能无证据改变历史触发时刻。

## 5. 接口和界面

- 保存、详情、分页和 Cron 预览协议增加 scheduleType、fireOnceTime、timezone。
- Cron 预览按请求时区计算。
- 新增 /system/job-config/editor 和 /system/job-config/editor/:id 隐藏路由。
- 列表展示调度类型、时区和下一次触发时间。

## 6. 关键规则

- LocalDateTime 不单独解释，所有转换必须显式携带 ZoneId。
- DST 不存在时间必须拒绝；重复时间必须采用固定且有测试的偏移选择策略。
- 只有 context.getScheduledFireTime 对应计划 ONCE 触发时才能标记 COMPLETED。
- 已结束任务可以查看和手动执行，但重新计划必须通过明确编辑操作。

## 7. 验收标准

- CRON/ONCE 互斥字段校验完整。
- 上海、UTC 和一个有 DST 的时区在固定时间测试下结果正确。
- 一次性任务计划执行一次后已结束，服务重启不会复活。
- 手动执行一次性任务后仍保留原计划状态。

## 8. 确认门禁

- [x] 确认新增/编辑一次性任务拒绝过去时间。
- [x] 确认一次性任务计划执行失败后仍进入已结束。
- [x] 确认默认时区 Asia/Shanghai；当前部署基线为 Asia/Shanghai，存量值据此回填。

## 9. 实施说明

- V2 已提供扁平全屏编辑路由，V3 只增量扩展现有工作台，不重复创建编辑页面。
- 用户于 2026-07-19 明确本阶段不执行 UI 自动化验收，由用户自行验证界面；代码级前端单测、Lint 和生产构建继续执行。
