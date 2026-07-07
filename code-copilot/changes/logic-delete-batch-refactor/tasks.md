# 任务拆分 — 数据表逻辑删除分批改造
> 拆分顺序：数据模型 → 底层实现 → 查询过滤 → 验证记录
> 每个任务 = 可独立提交的原子变更（3-5 个文件）
> 每个任务必须精确到文件路径和函数签名

## 前置条件

- [x] 用户确认进入 `/apply logic-delete-batch-refactor`。
- [x] 读取 `code-copilot/memory/pitfalls.md`、`code-copilot/memory/decisions.md`、`code-copilot/memory/preferences.md`。
- [x] 执行当前库只读 schema 盘点：已尝试远程查询，沙箱连接失败且提权审批服务 503；本轮使用静态 DDL 盘点和幂等迁移兜底。
- [x] 确认 Flyway 新脚本版本号：仓库当前最高 `V1.0.1`，Batch 1 使用 `V1.0.2__fix_existing_logic_delete_contract.sql`。

## Task 0: 当前库盘点与分类确认

> 状态：completed-with-warning。远程库只读查询因环境审批失败未完成；已记录阻断原因，并基于仓库全量初始化 SQL 与目标实体/XML 做静态盘点。

- **目标**: 形成实际数据库表字段清单，避免只按仓库全量 SQL 误判。
- **涉及文件**:
  - `code-copilot/changes/logic-delete-batch-refactor/execution-log.md` — 记录只读查询结果和排除表。
- **关键命令**:
  ```bash
  MYSQL_PWD='***' mysql -h 120.48.96.178 -P 3306 -u rdsroot -D forge_admin_new -N -e "
  SELECT t.table_name,
         COALESCE(t.table_comment, '') AS table_comment,
         COALESCE(GROUP_CONCAT(CASE WHEN c.column_name IN ('del_flag','deleted','is_deleted','delete_flag','deleted_at') THEN c.column_name END ORDER BY c.ordinal_position), '') AS logic_delete_cols
  FROM information_schema.tables t
  LEFT JOIN information_schema.columns c
    ON c.table_schema = t.table_schema
   AND c.table_name = t.table_name
  WHERE t.table_schema = DATABASE()
    AND t.table_type = 'BASE TABLE'
  GROUP BY t.table_name, t.table_comment
  ORDER BY t.table_name;"
  ```
- **验收标准**:
  - 明确标出 `qrtz_*`、`act_*`、`flw_*` 排除。
  - 明确 `sys_job_config`、`sys_job_log` 纳入项目内部表盘点。

## Task 1: Batch 1 修复已有删除字段但未生效的表

> 状态：completed。

- **目标**: 修复最明确的问题，保证已有删除字段真正生效。
- **涉及文件**:
  - `forge-server/db/migration/V<next>__fix_existing_logic_delete_contract.sql` — 新增防重复迁移。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/agent/domain/AiAgent.java` — 添加 `@TableLogic private String delFlag`。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/provider/domain/AiProvider.java` — 添加 `@TableLogic private String delFlag`。
  - `forge-server/forge-report-server/src/main/java/com/mdframe/forge/report/project/domain/ReportProject.java` — 添加 `@TableLogic private String delFlag`。
  - `forge-server/forge-report-server/src/main/java/com/mdframe/forge/report/project/template/domain/ReportTemplate.java` — 添加 `@TableLogic private String delFlag`。
  - `forge-server/forge-admin-server/src/main/java/com/mdframe/forge/employee/entity/Employee.java` — 给现有 `delFlag` 添加 `@TableLogic`。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/com/mdframe/forge/starter/flow/entity/FlowNodeConfig.java` — 添加 `@TableLogic private Integer delFlag`。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/com/mdframe/forge/starter/flow/entity/FlowApprovalLevel.java` — 确认 `deleted` 字段与迁移一致。
  - `forge-server/forge-report-server/src/main/resources/mapper/project/ReportProjectMapper.xml` — 查询补 `del_flag = '0'`。
  - `forge-server/forge-report-server/src/main/resources/mapper/project/template/ReportTemplateMapper.xml` — resultMap/base column 补 `del_flag`。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/resources/mapper/FlowNodeConfigMapper.xml` — 查询补 `c.del_flag = 0`。
  - `forge-server/forge-report-server/src/main/java/com/mdframe/forge/report/project/template/mapper/ReportTemplateMapper.java` — 增加包含已删除记录的模板查询和恢复方法。
  - `forge-server/forge-report-server/src/main/java/com/mdframe/forge/report/project/template/service/ReportTemplateService.java` — 修复模板逻辑删除后按来源项目重新创建时的主键冲突。
- **关键签名**:
  ```java
  @TableLogic
  private String delFlag;

  @TableLogic
  private Integer delFlag;
  ```
- **验收标准**:
  - `ai_agent`、`ai_provider`、`ai_report_project`、`ai_report_template`、`sys_employee`、`sys_flow_node_config` 删除不再物理删。
  - `sys_flow_approval_level` 不因缺 `deleted` 字段导致删除失败。
  - `ai_agent.agent_code`、`sys_employee.emp_no`、`sys_flow_node_config(model_id,node_id)` 只约束未删除记录唯一，逻辑删除后允许重新创建有效记录。

## Task 2: Batch 2 平台内部主数据/配置表

> 状态：completed。Batch 2.1/2.2 已完成低风险内部配置表和系统配置表；Batch 2.3a 已完成组织/岗位表；Batch 2.3b-1 已完成租户表；Batch 2.3b-2 已完成角色表；Batch 2.3b-3 已完成菜单资源表；Batch 2.3b-4 已完成用户表。

- **目标**: 将系统配置、用户可见主数据、任务内部表纳入逻辑删除。
- **候选表**:
  - Batch 2.1 已完成：`ai_context_config`, `ai_model`, `ai_page_template`, `ai_crud_config`, `sys_message_template`, `sys_message_biz_type`, `sys_job_config`, `sys_job_log`
  - Batch 2.2 已完成：`sys_config`, `sys_dict_type`, `sys_dict_data`, `sys_notice`, `sys_api_config`, `sys_data_scope_config`, `sys_file_storage_config`
  - Batch 2.3a 已完成：`sys_org`, `sys_post`
  - Batch 2.3b-1 已完成：`sys_tenant`
  - Batch 2.3b-2 已完成：`sys_role`
  - Batch 2.3b-3 已完成：`sys_resource`
  - Batch 2.3b-4 已完成：`sys_user`
- **涉及文件**:
  - `forge-server/db/migration/V1.0.3__add_logic_delete_to_platform_internal_tables.sql` — Batch 2.1 防重复迁移。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/context/domain/AiContextConfig.java` — 添加 `@TableLogic private String delFlag`。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/model/domain/AiModel.java` — 添加 `@TableLogic private String delFlag`。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/domain/entity/AiPageTemplate.java` — 添加 `@TableLogic private String delFlag`。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/domain/entity/AiCrudConfig.java` — 添加 `@TableLogic private String delFlag`。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-message/src/main/java/com/mdframe/forge/plugin/message/domain/entity/SysMessageTemplate.java` — 添加 `@TableLogic private Integer delFlag`。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-message/src/main/java/com/mdframe/forge/plugin/message/domain/entity/SysMessageBizType.java` — 添加 `@TableLogic private Integer delFlag`。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-job/src/main/java/com/mdframe/forge/plugin/job/entity/SysJobConfig.java` — 添加 `@TableLogic private Integer delFlag`。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-job/src/main/java/com/mdframe/forge/plugin/job/entity/SysJobLog.java` — 添加 `@TableLogic private Integer delFlag`。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/AiCrudConfigMapper.xml` — 自定义查询补 `del_flag = '0'`。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/AiLowcodeDomainMapper.xml` — 领域统计/工作台查询补 `ai_crud_config.del_flag = '0'`。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-job/src/main/java/com/mdframe/forge/plugin/job/mapper/SysJobLogMapper.java` — 增加日志留存物理清理方法。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-job/src/main/resources/mapper/SysJobLogMapper.xml` — 增加 `cleanPhysicalBefore` 专用 `DELETE`。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-job/src/main/java/com/mdframe/forge/plugin/job/service/impl/SysJobLogServiceImpl.java` — `cleanLog(days)` 改走专用物理清理。
  - `forge-server/db/migration/V1.0.4__add_logic_delete_to_system_config_tables.sql` — Batch 2.2 防重复迁移。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/entity/SysConfig.java` — 添加 `@TableLogic private Integer delFlag`。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/entity/SysDictType.java` — 添加 `@TableLogic private Integer delFlag`。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/entity/SysDictData.java` — 添加 `@TableLogic private Integer delFlag`。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/entity/SysNotice.java` — 添加 `@TableLogic private Integer delFlag`。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/entity/SysFileStorageConfig.java` — 添加 `@TableLogic private Integer delFlag`。
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-api-config/src/main/java/com/mdframe/forge/starter/apiconfig/domain/entity/SysApiConfig.java` — 添加 `@TableLogic private Integer delFlag`。
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-datascope/src/main/java/com/mdframe/forge/starter/datascope/entity/SysDataScopeConfig.java` — 添加 `@TableLogic private Integer delFlag`。
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-api-config/src/main/resources/mapper/SysApiConfigMapper.xml` — API 运行时配置查询补 `del_flag = 0`。
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-datascope/src/main/resources/mapper/datascope/SysDataScopeConfigMapper.xml` — 数据权限快照查询补 `del_flag = 0`。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/resources/mapper/SysNoticeReadRecordMapper.xml` — 公告已读/未读统计补公告逻辑删除过滤。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/AiCrudExportTaskMapper.xml` — 导出配置读取 `sys_config` 时补逻辑删除过滤。
  - `forge-server/db/migration/V1.0.5__add_logic_delete_to_org_post_tables.sql` — Batch 2.3a 防重复迁移。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/entity/SysOrg.java` — 添加 `@TableLogic private Integer delFlag`。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/entity/SysPost.java` — 添加 `@TableLogic private Integer delFlag`。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/resources/mapper/SysOrgMapper.xml` — 组织列表/树/子级/启用查询补 `del_flag = 0`。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/resources/mapper/SysPostMapper.xml` — 岗位列表补 `p.del_flag = 0`，组织 JOIN 过滤已删除组织。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/resources/mapper/SysUserMapper.xml` — 用户列表组织筛选和组织名聚合过滤已删除组织。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/resources/mapper/SysUserOrgMapper.xml` — 当前用户可切换组织过滤已删除组织。
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-datascope/src/main/java/com/mdframe/forge/starter/datascope/mapper/DataScopeOrgMapper.java` — 数据权限子组织注解 SQL 过滤已删除组织。
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-datascope/src/main/resources/mapper/datascope/DataScopeOrgMapper.xml` — 数据权限组织层级快照过滤已删除组织。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-message/src/main/resources/mapper/SysMessageReceiverMapper.xml` — 消息接收人组织名展示过滤已删除组织。
  - `forge-server/db/migration/V1.0.6__add_logic_delete_to_tenant_table.sql` — Batch 2.3b-1 租户表防重复迁移。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/entity/SysTenant.java` — 添加 `@TableLogic private Integer delFlag`。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/resources/mapper/SysTenantMapper.xml` — resultMap/base column 补 `del_flag`。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/resources/mapper/SysUserTenantMapper.xml` — 用户可切换租户过滤已删除租户。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/resources/mapper/SysUserMapper.xml` — 用户列表租户 JOIN 过滤已删除租户。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/resources/mapper/SysRoleMapper.xml` — 角色列表租户 JOIN 过滤已删除租户。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/resources/mapper/SysOrgMapper.xml` — 组织列表租户 JOIN 和有效租户判断过滤已删除租户。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/resources/mapper/SysPostMapper.xml` — 岗位列表有效租户判断过滤已删除租户。
  - `forge-server/db/migration/V1.0.7__add_logic_delete_to_role_table.sql` — Batch 2.3b-2 角色表防重复迁移。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/entity/SysRole.java` — 添加 `@TableLogic private Integer delFlag`。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/resources/mapper/SysRoleMapper.xml` — 角色列表/详情补 `r.del_flag = 0` 和 resultMap/base column。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/resources/mapper/SysUserOrgRoleMapper.xml` — 用户组织角色、流程按角色找人和角色名摘要过滤已删除角色。
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-datascope/src/main/java/com/mdframe/forge/starter/datascope/mapper/DataScopeRoleMapper.java` — 数据权限注解 SQL 过滤已删除角色。
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-datascope/src/main/resources/mapper/datascope/DataScopeRoleMapper.xml` — 数据权限角色快照过滤已删除角色。
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-datascope/src/main/resources/mapper/datascope/SysRoleDataScopeMapper.xml` — 自定义数据权限组织快照过滤已删除角色。
  - `forge-server/db/migration/V1.0.8__add_logic_delete_to_resource_table.sql` — Batch 2.3b-3 菜单/权限资源表防重复迁移。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/entity/SysResource.java` — 添加 `@TableLogic private Integer delFlag`。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/resources/mapper/SysResourceMapper.xml` — 菜单树/API权限/菜单注册查询补 `del_flag = 0`。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/BusinessPermissionMapper.xml` — 低代码权限去重查询过滤已删除资源。
  - `forge-server/db/migration/V1.0.9__add_logic_delete_to_user_table.sql` — Batch 2.3b-4 用户表防重复迁移。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/entity/SysUser.java` — 添加 `@TableLogic private Integer delFlag`。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/resources/mapper/SysUserMapper.xml` — 用户列表和登录查询补 `u.del_flag = 0`。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/resources/mapper/SysTenantMapper.xml` — 租户用户计数过滤已删除用户。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/resources/mapper/SysRoleMapper.xml` — 角色用户列表过滤已删除用户。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/resources/mapper/SysUserOrgRoleMapper.xml` — 按角色找用户过滤已删除用户。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/resources/mapper/SysNoticeReadRecordMapper.xml` — 公告已读/未读用户过滤已删除用户。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-message/src/main/resources/mapper/SysMessageReceiverMapper.xml` — 消息接收人用户展示过滤已删除用户。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/BusinessMessageChannelMapper.xml` — 业务消息按角色/组织找人过滤已删除用户。
  - `forge-server/forge-business/forge-business-core/src/main/resources/mapper/business/SamplePurchaseOrderMapper.xml` — 采购样例审批人/会签人名称过滤已删除用户。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/resources/mapper/AiDashboardGenerateRecordMapper.xml` — AI 大屏生成记录用户展示过滤已删除用户。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/resources/mapper/AiChatSessionMapper.xml` — AI 会话用户展示过滤已删除用户。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/resources/mapper/AiPromptTemplateMapper.xml` — AI 提示词模板创建/更新人展示过滤已删除用户。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/resources/mapper/FlowModelVersionMapper.xml` — 流程模型版本发布人展示过滤已删除用户。
- **关键 SQL 模板**:
  ```sql
  SET @table_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user');
  SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'del_flag');
  SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
      'ALTER TABLE sys_user ADD COLUMN del_flag char(1) NOT NULL DEFAULT ''0'' COMMENT ''删除标志（0正常 1删除）'' AFTER update_time',
      'SELECT 1');
  PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
  ```
- **验收标准**:
  - Batch 2.1/2.2 列表/详情不返回逻辑删除记录。
  - Batch 2.3a `sys_org`、`sys_post` 删除改为逻辑删除；组织树、岗位列表、用户组织筛选、数据权限组织快照不返回已删除组织。
  - Batch 2.3b-1 `sys_tenant` 删除改为逻辑删除；用户可切换租户、用户/角色/组织/岗位列表关联租户时不返回已删除租户。
  - Batch 2.3b-2 `sys_role` 删除改为逻辑删除；角色列表/详情、登录角色加载、流程按角色找人、数据权限角色快照不返回已删除角色。
  - Batch 2.3b-3 `sys_resource` 删除改为逻辑删除；菜单树、权限加载、API权限、低代码菜单注册和权限去重不返回已删除资源。
  - Batch 2.3b-4 `sys_user` 删除改为逻辑删除；登录、用户列表、角色用户、公告/消息接收人和业务按用户展示不返回已删除用户。
  - `sys_job_config` 删除配置时删除 Quartz 调度后，Forge 配置记录逻辑删除。
  - `sys_job_log` 普通删除逻辑删除，`cleanLog(days)` 作为留存策略继续物理清理历史日志。
  - 授权关系表不在本任务中改造。

## Task 3: Batch 3 设计态元数据和业务样例表

> 状态：completed。Batch 3.1 已完成低代码/应用中心主设计态表；Batch 3.2 已完成数据资产/报表目录主元数据表；Batch 3.3 已完成采购样例业务表。关系表、日志表和重建型子表进入 Task 4 评审。

- **目标**: 将低代码、数据资产、报表目录、应用中心设计态数据改为逻辑删除。
- **候选表**:
  - Batch 3.1 已完成：`ai_business_suite`, `ai_business_app`, `ai_business_object`, `ai_business_trigger`
  - Batch 3.1 已完成：`ai_code_rule`, `ai_lowcode_domain`, `ai_lowcode_model`
  - Batch 3.2 已完成：`ai_report_data_connection`, `ai_report_data_dataset`, `ai_report_data_dataset_category`
  - Batch 3.2 已完成：`ai_report_data_business_definition`, `ai_report_data_dimension`, `ai_report_directory`
  - Batch 3.3 已完成：`sample_purchase_order`
- **涉及文件**:
  - `forge-server/db/migration/V1.0.10__add_logic_delete_to_lowcode_design_metadata_tables.sql` — Batch 3.1 防重复迁移。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/domain/entity/AiBusinessSuite.java` — 添加 `@TableLogic private String delFlag`。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/domain/entity/AiBusinessApp.java` — 添加 `@TableLogic private String delFlag`。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/domain/entity/AiBusinessObject.java` — 添加 `@TableLogic private String delFlag`。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/domain/entity/AiBusinessTrigger.java` — 添加 `@TableLogic private String delFlag`。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/domain/entity/AiCodeRule.java` — 添加 `@TableLogic private String delFlag`。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/domain/entity/AiLowcodeDomain.java` — 添加 `@TableLogic private String delFlag`。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/domain/entity/AiLowcodeModel.java` — 添加 `@TableLogic private String delFlag`。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/BusinessSuiteMapper.xml` — 查询、统计和编码校验补 `del_flag = '0'`。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/BusinessAppMapper.xml` — 列表、详情、运行入口和编码校验补 `del_flag = '0'`。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/BusinessObjectMapper.xml` — 对象列表、详情、运行态定位、统计和套件迁移更新补过滤。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/BusinessTriggerMapper.xml` — 触发器列表、事件执行、定时扫描和活跃统计补过滤。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/CodeRuleMapper.xml` — 编码规则列表、生成、预览、详情和唯一校验补过滤。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/AiLowcodeDomainMapper.xml` — 领域列表、树、父子校验、唯一校验和工作台查询补过滤。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/AiLowcodeModelMapper.xml` — 模型列表、详情、运行解析和唯一校验补过滤。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/BusinessObjectRelationMapper.xml` — 关系展示 join 已删除对象时过滤对象名称来源，关系表仍物理删除。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/BusinessBindingMapper.xml` — 流程绑定摘要过滤已删除业务对象/套件。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/AiCrudConfigMapper.xml` — 低代码应用列表 join 领域时过滤已删除领域。
  - `forge-server/db/migration/V1.0.11__add_logic_delete_to_report_data_metadata_tables.sql` — Batch 3.2 防重复迁移。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-data/src/main/java/com/mdframe/forge/plugin/data/entity/DataConnection.java` — 添加 `@TableLogic private String delFlag`。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-data/src/main/java/com/mdframe/forge/plugin/data/entity/DataDataset.java` — 添加 `@TableLogic private String delFlag`。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-data/src/main/java/com/mdframe/forge/plugin/data/entity/DataDatasetCategory.java` — 添加 `@TableLogic private String delFlag`。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-data/src/main/java/com/mdframe/forge/plugin/data/entity/DataBusinessDefinition.java` — 添加 `@TableLogic private String delFlag`。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-data/src/main/java/com/mdframe/forge/plugin/data/entity/DataDimension.java` — 添加 `@TableLogic private String delFlag`。
  - `forge-server/forge-report-server/src/main/java/com/mdframe/forge/report/project/directory/domain/ReportDirectory.java` — 添加 `@TableLogic private String delFlag`。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-data/src/main/resources/mapper/DataConnectionMapper.xml` — 连接列表、编码查询和数据集引用计数过滤已删除记录。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-data/src/main/resources/mapper/DataDatasetMapper.xml` — 数据集列表/编码查询/分类计数过滤已删除记录，连接和分类 join 过滤已删除主表。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-data/src/main/resources/mapper/DataDatasetCategoryMapper.xml` — 分类列表、编码查询和子分类计数过滤已删除记录。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-data/src/main/resources/mapper/DataBusinessDefinitionMapper.xml` — 业务定义列表、编码查询过滤已删除记录，数据集摘要只统计未删除数据集。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-data/src/main/resources/mapper/DataDimensionMapper.xml` — 维度列表、编码/ID 查询过滤已删除记录，连接 join 过滤已删除连接。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-data/src/main/resources/mapper/DataBusinessDatasetMapper.xml` — 业务-数据集绑定展示过滤已删除数据集，绑定表仍物理删除。
  - `forge-server/forge-report-server/src/main/resources/mapper/project/directory/ReportDirectoryMapper.xml` — 目录列表、子孙目录和子目录计数过滤已删除目录。
  - `forge-server/db/migration/V1.0.12__add_logic_delete_to_sample_purchase_order.sql` — Batch 3.3 防重复迁移。
  - `forge-server/forge-business/forge-business-core/src/main/java/com/mdframe/forge/business/core/purchase/domain/SamplePurchaseOrder.java` — 添加 `@TableLogic private String delFlag`。
  - `forge-server/forge-business/forge-business-core/src/main/resources/mapper/business/SamplePurchaseOrderMapper.xml` — 采购样例列表、详情、批量详情和流程业务 Key 查询过滤已删除记录。
- **验收标准**:
  - Batch 3.1 App 中心、低代码领域/模型、触发器、编码规则删除后列表不可见但数据库记录保留。
  - 低代码关系表和触发日志表不在 Batch 3.1 中改为软删。
  - Batch 3.2 数据资产连接/数据集/分类/业务定义/维度和报表目录删除后列表不可见但数据库记录保留。
  - 数据集字段、ACL、行权限、维度项和业务数据集绑定表不在 Batch 3.2 中改为软删。
  - Batch 3.3 采购样例业务表删除后列表/详情/流程业务 Key 查询不可见但数据库记录保留。
  - Flowable/Forge 流程任务表不在 Batch 3.3 中改为软删。

## Task 4: Batch 4 关系表和 XML 物理删除评审

> 状态：completed。已扫描 Mapper XML `DELETE FROM`，将用户配置主表 `ai_prompt_template`、`ai_custom_query_scheme` 转为逻辑删除；其余日志、会话、关系和重建型子配置继续保留物理删除并记录原因。

- **目标**: 精准处理 XML `DELETE FROM`，避免把应该物理清理的数据改成软删。
- **默认保留物理删除**:
  - 授权关系：`sys_user_role`, `sys_role_resource`, `sys_user_org`, `sys_user_org_role`, `sys_user_post`, `sys_user_tenant`, `sys_role_org`
  - 框架表：`qrtz_*`, `act_*`, `flw_*`
  - 重建型子表：数据集字段、ACL、行权限、对象关系 diff 清理
  - 清理日志：`sys_external_api_log`, `sys_operation_log`, `sys_login_log`, `ai_business_trigger_log`, `ai_formula_execution_log`
- **重点评审 XML**:
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/resources/mapper/AiPromptTemplateMapper.xml`
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/CustomQuerySchemeMapper.xml`
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/resources/mapper/SysExcelColumnConfigMapper.xml`
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-data/src/main/resources/mapper/DataDatasetFieldMapper.xml`
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/BusinessObjectRelationMapper.xml`
- **已转换逻辑删除**:
  - `forge-server/db/migration/V1.0.13__add_logic_delete_to_prompt_and_query_scheme_tables.sql` — Task 4 防重复迁移。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/prompt/domain/AiPromptTemplate.java` — 添加 `@TableLogic private String delFlag`。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/resources/mapper/AiPromptTemplateMapper.xml` — 列表/详情/编码校验/计数更新过滤已删除记录，删除改为 `UPDATE del_flag='1'`。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/domain/entity/CustomQueryScheme.java` — 添加 `@TableLogic private String delFlag`。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/CustomQuerySchemeMapper.xml` — 查询方案列表/详情/默认方案清理过滤已删除记录，删除改为 `UPDATE del_flag='1'`。
- **保留物理删除并记录原因**:
  - `SysJobLogMapper.xml#cleanPhysicalBefore`：日志留存清理，语义是释放历史日志。
  - `ExternalApiLogMapper.xml#clearLogs`：外部 API 调用日志清理。
  - `SysOnlineUserMapper.xml#cleanExpiredUsers`：在线会话过期清理。
  - `BusinessBindingMapper.xml#deleteByTarget`：业务绑定关系重建清理。
  - `BusinessObjectRelationMapper.xml#deleteBySuiteCode`：业务对象关系重建清理。
  - `SysExcelColumnConfigMapper.xml#deleteByConfigKey`：Excel 导出列子配置重建清理。
  - `DataBusinessDatasetMapper.xml#deleteByBusinessId`：业务定义-数据集绑定关系重建清理。
  - `DataDatasetAclMapper.xml#deleteByDatasetId`：数据集 ACL 重建清理。
  - `DataDatasetFieldMapper.xml#deleteByDatasetId`：数据集字段重建清理。
  - `DataDatasetRowScopeMapper.xml#deleteByDatasetId`：数据集行权限配置重建清理。
  - `DataDimensionItemMapper.xml#deleteByDimensionId`：维度项重建清理。
  - `AiDashboardComponentLineageMapper.xml#deleteByRecordId`：AI 大屏生成血缘子记录清理。
  - `AiDashboardGenerateRecordMapper.xml#deleteOwn`：AI 大屏生成记录属于用户历史/生成日志，按用户清理语义保留物理删除。
- **验收标准**:
  - 每个保留物理删除的 XML 都在 `execution-log.md` 记录原因。
  - 每个改为逻辑删除的 XML 都有字段、实体、查询过滤同步改造。

## Task 5: 全量验证和文档同步

> 状态：completed-with-warning。admin/report/flow 聚合编译和静态检查通过；本地 MySQL 3407 不可用，SQL 实跑和接口 smoke 未执行。

- **目标**: 完成 SDD 验收记录。
- **涉及文件**:
  - `code-copilot/changes/logic-delete-batch-refactor/spec.md`
  - `code-copilot/changes/logic-delete-batch-refactor/tasks.md`
  - `code-copilot/changes/logic-delete-batch-refactor/test-spec.md`
  - `code-copilot/changes/logic-delete-batch-refactor/execution-log.md`
- **关键命令**:
  ```bash
  cd forge-server && mvn -q -pl forge-admin-server -am -DskipTests compile
  cd forge-server && mvn -q -pl forge-report-server -am -DskipTests compile
  cd forge-server && mvn -q -pl forge-flow/forge-flow-server -am -DskipTests compile
  ```
- **验收标准**:
  - 编译通过。
  - 迁移 SQL 可重复执行或具备防重复保护。
  - 删除接口 smoke 结果写入 `execution-log.md`。
