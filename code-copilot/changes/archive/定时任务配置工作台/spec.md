# 定时任务配置工作台
> status: applying
> created: 2026-07-19
> complexity: 🟡中等
> parent: code-copilot/changes/定时任务优化/spec.md
> version: V2
> dependency: 定时任务可靠性加固
> ui-baseline: code-copilot/changes/定时任务优化/ui-reference.md

## 1. 目标

把当前以 Bean、Handler 和 Cron 为中心的技术表单改为用户能够理解的任务配置工作台。用户先理解“执行什么、什么时候执行、保存后是否启用”，技术字段按需展开。

## 2. 功能范围

- [x] 重构任务列表默认列，展示执行内容摘要、自然语言执行计划、状态、下次执行和最近结果。
- [x] 新增/编辑使用独立全屏工作台，不再使用 AiCrudPage 通用编辑弹窗。
- [x] 工作台分为基本信息、执行内容、执行计划、高级设置，并提供固定配置摘要。
- [x] 增加任务处理器目录接口，HANDLER 默认通过可搜索目录选择并展示描述。
- [x] BEAN/RPC 作为技术执行方式，字段按选择条件展开，不同时堆在表单中。
- [x] 后端提供 Cron 校验和预览，返回标准表达式、自然语言描述和未来 5 次执行时间。
- [x] 简单计划支持每隔 N 分钟、每小时、每天、每周、每月。
- [x] 专家模式直接编辑 Quartz 6 段表达式；复杂表达式无法反解析时原值不丢失。
- [x] 新建任务默认保存后停用，用户可显式选择保存后启用。
- [x] 离开未保存页面时接入项目 dirty tab 和路由确认。
- [x] 全局日志清理移入页面更多菜单，不再与新建任务并列。

## 3. 明确不做

- 不支持一次性任务或每任务独立时区。
- 不增加并发、重试、Misfire、告警或流程模式。
- 不承诺任意复杂 Cron 都能转换成简单表单。
- 不删除 BEAN/RPC 兼容能力，只降低其默认视觉优先级。
- 不在 V2 重做日志页面，独立日志页属于 V5。

## 4. 后端协议

- GET /job/config/executors：返回已注册 JobHandler/ScheduledJob 的编码、展示名称、描述、分组和来源。
- POST /job/config/cron/preview：服务端校验 Cron 并返回描述和未来 5 次时间。
- 任务分页 VO 增加 executionSummary、scheduleSummary、nextFireTime、lastExecutionStatus。
- 任务保存继续复用 V1 DTO 和校验，不新增数据库字段。

## 5. 前端结构

- /system/job-config：任务总览和高频操作。
- /system/job-config/editor：新建任务。
- /system/job-config/editor/:id：编辑任务。
- JobConfigWorkbench：全屏外壳、分区导航、保存状态和摘要。
- JobExecutionSection：处理器目录与技术执行方式。
- JobScheduleSection：简单计划、专家模式和未来时间预览。

详细布局、术语、状态和响应式要求以 ui-reference.md 为准。

## 6. 验收标准

- 用户无需输入 Cron 即可创建每天、每周或每月任务。
- HANDLER 任务可以通过业务名称搜索选择，不要求记忆处理器编码。
- 列表默认不展示 Bean、方法、Handler 编码和 Cron 原文。
- 编辑复杂历史 Cron 不会覆盖原表达式。
- 保存前后明确展示启停选择和调度同步结果。
- 1366×768 下工作台主要操作无需横向滚动。

## 7. 确认门禁

- [x] 确认 V2 使用独立全屏配置工作台。
- [x] 确认 HANDLER 默认使用处理器目录选择，BEAN/RPC 放入技术执行方式。
- [x] 确认新建任务默认保存后停用。
- [x] 确认 V2 继续使用平台默认时区。

## 8. 执行进度

| Task | 状态 | 说明 |
|---|---|---|
| Task 1 处理器目录和 Cron 预览 | complete | Job 模块最终 44/44 测试通过 |
| Task 2 前端 API 和表单状态 | complete | 表单状态单测 4/4 通过 |
| Task 3 重构任务总览 | complete | 默认列与操作层级完成，定向 ESLint 通过 |
| Task 4 工作台外壳 | complete | 三栏布局、dirty tab 和响应式完成 |
| Task 5 基本信息和执行内容 | complete | 目录选择、技术模式和 JSON 参数完成 |
| Task 6 CronBuilder 和执行计划 | complete | 五类简单计划与专家模式单测 3/3 通过 |
| Task 7 路由资源和兼容 | complete | 扁平路由入口、动态参数匹配和角色继承迁移完成 |
| Task 8 UI 验证 | applying | 构建与静态检查通过；Playwright 被宿主端口和 Chromium 权限阻断 |
