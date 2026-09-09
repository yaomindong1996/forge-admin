# Execution log

- 2026-09-03 实现流程经手可见：参与者索引、对象开关、读路径 OR、角色说明与列表标记。
- 后端单测：DynamicDataScopeServiceTest 4、BusinessApplicationPermissionServiceTest 7、FlowRecordParticipantServiceImplTest 3，均通过。
- 前端 vitest：application-data-scope-adapter-modal、application-permission-utils 通过。
- 未启动 Admin/Flow，未做浏览器 E2E。重启后台后 Flyway 会执行 V1.0.140。
- 2026-09-03 业务拦截器补经手可见：`sys_data_scope_config` 增加开关/业务类型/主键列；查询 OR EXISTS 经手表。业务表不加字段，对齐 `businessType:recordId`。
- DataScopeFlowRelatedVisibilityTest 3 通过。
