# 增量验证计划

## P0

- 业务对象服务与 Generator 模块使用 JDK 17 编译。
- `BusinessFlowServiceBusinessKeyTest` 验证待办对象按 `configKey` 优先解析。
- Flyway 脚本执行 `git diff --check`、占位符扫描，并检查唯一索引/重复数据修复语句。

## P1（需真实环境）

- 在开发库执行 V1.0.136/V1.0.137，确认 `ai_business_object` 有效行不存在重复 `(tenant_id, object_code)`，并修复历史流程/表单引用。
- 用预售登记待办调用任务表单上下文接口，确认 `businessObjectName=测试`、`configKey=presale_registration_business_object`。
- 用 HR、BFMA、预售三个历史对象分别查询待办列表，确认对象名称和表单不串单。

本轮未启动 Admin/Flow 服务；已在当前开发库事务执行并提交 V1.0.137，完成预售流程草稿、流程模型表单引用及应用快照修复。

## 2026-08-30 本轮增量验证

- 修复 `low12` 流程模型的全局业务表单引用、业务流程草稿 `dependencies.objects`/动作节点 `objectCode`，统一为 `bo_2089974506884993026`。
- 修复应用版本快照中遗留的 `business_object` 引用；查询结果为 0 条残留。
- `business_flow_form_assets` 的当前应用页面资产仍保留，表单 key 为 `app_2089968247981060098_page_page_page_form_form_form`。
