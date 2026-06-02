# Phase 8 执行日志：lowcode-app-full-loop-optimization

> 执行时间：2026-06-02 08:00 CST  
> 范围：Phase 8 验证归档，覆盖后端构建、前端构建、Flyway、核心接口、商机主流程和文档回填。

## 1. 代码与迁移补丁

- 修复 `V1.0.51__seed_crm_opportunity_document_flow.sql`、`V1.0.52__seed_leave_document_flow_demo.sql` 中内嵌模板的 Flyway 占位符冲突，将 seed JSON 模板从 `${field}` 改为 `{field}`。
- `BusinessFlowService`、`BusinessTriggerExecutor`、`MessageTemplateEngine` 已兼容 `{field}` 与历史 `${field}` 两种模板变量格式。
- 新增 `V1.0.53__align_flow_template_logic_delete_column.sql`，为 `sys_flow_template` 补齐 `del_flag` 逻辑删除字段，匹配 `FlowTemplate` 实体。
- 新增 `V1.0.54__patch_opportunity_flow_dept_manager_mapping.sql`，为 CRM 商机默认流程绑定和自动发起流程触发器补齐 `createBy -> deptManager` 变量映射。

## 2. Flyway 与流程部署

- dev 库 `forge_schema_history` 已执行到 `1.0.54`，最新脚本 `patch opportunity flow dept manager mapping` 执行成功。
- dev 库原有 `sys_flow_template(template_key=leave_multi)`，但缺少对应 Flowable 模型和已部署流程定义；Phase 8 已通过流程服务创建并部署模型。
- `leave_multi` 模型 ID：`e7d55f0a4087ec5dc0189784a00204ad`。
- `leave_multi` 部署 ID：`dcbda981-5e14-11f1-a0fd-d67ed5f8e875`。
- `act_re_procdef` 已存在 `leave_multi` version `1`。

## 3. 商机主流程验证

- 调用 `/ai/business/flow/start`，请求 `{"objectCode":"OPPORTUNITY","recordId":10}`，返回 `code=200`。
- 流程实例 ID：`602ad5c2-5e15-11f1-a0fd-d67ed5f8e875`。
- 单据流程关联 ID：`2061597550728736769`。
- `crm_opportunity.id=10` 的 `document_status` 已回写为 `IN_PROCESS`。
- `ai_business_flow_instance_link` 已写入 `OPPORTUNITY:10`，流程状态 `RUNNING`。
- `sys_flow_business` 已写入 `OPPORTUNITY:10`，状态 `running`。
- `sys_flow_task` 已生成 `部门经理审批` 待办，任务 ID `602b2410-5e15-11f1-a0fd-d67ed5f8e875`，assignee `1`。

## 4. 接口验证

以下接口均通过 `X-Inner-Call: true` 验证，返回 `code=200`：

- `/ai/business/document/config/1910000000000000104`：单据配置已启用，默认流程 `leave_multi`。
- `/ai/business/document/OPPORTUNITY/10/runtime`：返回 `documentStatus=IN_PROCESS`、`flowStatus=RUNNING`，可用动作包含 `START_FLOW`、`VIEW_FLOW`。
- `/ai/business/flow/status/OPPORTUNITY/10`：返回 `RUNNING`。
- `/ai/business/trigger/page?pageNum=1&pageSize=10`：返回总数 `5`。
- `/ai/business/trigger/scenario-templates`：返回模板数 `5`。
- `/ai/business/stats/crm_opportunity/metrics`：返回指标 `TOTAL`、`TODAY`、`MONTH`。

## 5. 构建验证

- `git diff --check`：通过。
- 后端：`mvn -pl forge-admin-server -am package -DskipTests`：`BUILD SUCCESS`。
- 前端：`source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm build`：构建通过。
- 前端构建仅保留既有警告：UnoCSS 图标加载、CSS `//` 注释、动态/静态导入混用和 chunk size 提示。

## 6. 结论

Phase 8 验证通过。数据库迁移、流程定义部署、商机单据手动发起流程、运行态状态查询、触发器模板、触发器分页和业务指标接口均已完成最小闭环验证。
