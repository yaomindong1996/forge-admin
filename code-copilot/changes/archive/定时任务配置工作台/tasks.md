# 定时任务配置工作台 Implementation Plan
> status: applying
> scope: V2 only

## 进度

| Task | 状态 | 完成度 |
|---|---|---|
| Task 1 处理器目录和 Cron 预览 | complete | 3/3 |
| Task 2 前端 API 和表单状态 | complete | 2/2 |
| Task 3 重构任务总览 | complete | 4/4 |
| Task 4 工作台外壳 | complete | 3/3 |
| Task 5 基本信息和执行内容 | complete | 4/4 |
| Task 6 CronBuilder 和执行计划 | complete | 4/4 |
| Task 7 路由资源和兼容 | complete | 3/3 |
| Task 8 UI 验证 | applying | 2/4 |

## Task 1: 处理器目录和 Cron 预览

- **涉及文件**:
  - Create: service/JobExecutorCatalogService.java、service/JobCronService.java
  - Create: vo/JobExecutorCatalogVO.java、dto/JobCronPreviewRequest.java、vo/JobCronPreviewVO.java
  - Modify: controller/JobConfigController.java
- **关键签名**:
  - List<JobExecutorCatalogVO> listExecutors()
  - JobCronPreviewVO preview(JobCronPreviewRequest request, Clock clock)
- [x] 从 JobHandler/ScheduledJob 注册信息构建目录。
- [x] 固定时钟测试 Cron 合法性和未来 5 次触发。
- [x] 错误响应不暴露 Quartz 内部异常。

## Task 2: 前端 API 和表单状态

- **涉及文件**:
  - Create: forge-admin-ui/src/api/system/job.js
  - Create: forge-admin-ui/src/views/system/job-config/job-config-form.js
  - Test: forge-admin-ui/src/views/system/job-config/__tests__/job-config-form.test.js
- [x] 实现详情、保存、处理器目录和 Cron 预览 API。
- [x] 实现新建默认停用、编辑归一化和提交转换。

## Task 3: 重构任务总览

- **涉及文件**:
  - Modify: forge-admin-ui/src/views/system/job-config.vue
- [x] 默认列改为任务、执行内容、执行计划、状态、下次执行和最近结果。
- [x] 编辑、立即运行、日志为高频操作，其它动作进入更多菜单。
- [x] 日志清理移入页面更多菜单。
- [x] 新增和编辑跳转独立路由。

## Task 4: 工作台外壳

- **涉及文件**:
  - Create: forge-admin-ui/src/views/system/job-config.editor.vue
  - Create: forge-admin-ui/src/views/system/job-config.editor.[id].vue
  - Create: forge-admin-ui/src/views/system/job-config/components/JobConfigWorkbench.vue
- [x] 实现固定顶部操作、左侧分区、主表单和右侧摘要。
- [x] 接入 dirty tab、离开确认、保存中和保存结果状态。
- [x] 中窄屏和移动端按 ui-reference.md 降级。

## Task 5: 基本信息和执行内容

- **涉及文件**:
  - Create: components/JobBasicSection.vue
  - Create: components/JobExecutionSection.vue
- [x] HANDLER 使用可搜索处理器目录和业务描述。
- [x] BEAN/RPC 仅在技术执行方式中显示对应字段。
- [x] 历史未注册目标可回显且不被静默清空。
- [x] JSON 参数提供格式校验和格式化。

## Task 6: CronBuilder 和执行计划

- **涉及文件**:
  - Create: forge-admin-ui/src/components/job/CronBuilder.vue
  - Create: forge-admin-ui/src/components/job/cron-builder.js
  - Create: components/JobScheduleSection.vue、JobSchedulePreview.vue
  - Test: forge-admin-ui/src/components/job/__tests__/cron-builder.test.js
- [x] 覆盖五类简单计划生成。
- [x] 只反解析组件可无损表达的 Cron。
- [x] 复杂表达式进入专家模式并保留原值。
- [x] 展示自然语言摘要和未来 5 次执行时间。

## Task 7: 路由资源和兼容

- **涉及文件**:
  - Create: forge-server/db/migration/V<next>__add_job_editor_routes.sql
  - Modify: 任务列表前端入口
- [x] 创建新建/编辑隐藏路由并继承任务管理访问范围。
- [x] 现有管理 API 路径保持兼容。
- [x] 旧任务详情可以无损打开工作台。

## Task 8: UI 验证

- [x] 创建 test-spec.md 和 execution-log.md。
- [x] 运行后端定向测试、前端单元测试、Lint 和生产构建。
- [ ] 使用 Playwright 验证 1366×768、1920×1080 和移动宽度。
- [ ] 覆盖 ui-reference.md 第 10 节的 7 个用户场景。

> Playwright 脚本已落在 `ui-validation.py`；当前宿主沙箱禁止本地端口监听，并禁止 Chromium 注册 Mach Port，本轮无法生成截图或互动通过证据。
