# 逻辑删除表结构扫描清单

## 扫描范围与结论

- 实体层：81 张 `@TableName` 表显式使用 `@TableLogic`。
- Flyway 层：55 张表存在可见生成列 `logic_delete_active`。
- 索引层：64 个唯一索引通过该列实现“仅未删除记录唯一”。
- 对照组：26 张逻辑删除表不含该辅助列，说明逻辑删除本身只依赖 `del_flag/deleted`。
- 实际库：只读连接返回 MySQL 1045，本清单以版本化 Flyway 和实体代码为本轮权威基线；部署后必须用 `information_schema` 复核。

## 需要转换的 55 张表与 64 个索引

其中 54 张表使用数值主键，可由 MyBatis-Plus 直接执行 `SET del_flag = 主键列`；`sys_flow_node_config.id` 为字符串，使用同类型字符串删除标记和专用 Mapper 删除 SQL。

| 表名 | 活跃唯一索引 |
|------|--------------|
| `ai_agent` | `uk_agent_code_active` |
| `ai_business_app` | `uk_ai_business_app_code_active` |
| `ai_business_application` | `uk_ai_business_application_code_active` |
| `ai_business_application_object` | `uk_ai_business_application_object_active` |
| `ai_business_application_publish_run` | `uk_ai_business_publish_run_key_active`, `uk_ai_business_publish_run_version_active` |
| `ai_business_application_version` | `uk_ai_business_application_version_active` |
| `ai_business_extension` | `uk_ai_business_extension_code_active` |
| `ai_business_extension_version` | `uk_ai_business_extension_version_active` |
| `ai_business_object` | `uk_ai_business_object_code_active` |
| `ai_business_suite` | `uk_ai_business_suite_code_active` |
| `ai_capability` | `uk_ai_capability_code_active`, `uk_ai_capability_tool_active` |
| `ai_capability_access_token` | `uk_ai_capability_token_key_active` |
| `ai_capability_approval` | `uk_capability_approval_idempotency`, `uk_capability_approval_request` |
| `ai_capability_client` | `uk_ai_capability_client_code_active`, `uk_ai_capability_client_key_active` |
| `ai_capability_flow_action_log` | `uk_cap_flow_action_idempotency`, `uk_cap_flow_action_request` |
| `ai_capability_grant` | `uk_ai_capability_grant_active` |
| `ai_capability_oauth_redirect_uri` | `uk_ai_capability_redirect_active` |
| `ai_capability_policy` | `uk_capability_policy_version` |
| `ai_capability_version` | `uk_ai_capability_version_active` |
| `ai_code_rule` | `uk_ai_code_rule_code_active` |
| `ai_code_rule_segment` | `uk_ai_code_rule_segment_key_active` |
| `ai_crud_config` | `uk_config_key_active` |
| `ai_lowcode_domain` | `uk_ai_lowcode_domain_code_active`, `uk_ai_lowcode_domain_name_active` |
| `ai_lowcode_model` | `uk_ai_lowcode_model_code_active` |
| `ai_model` | `uk_ai_model_provider_model_active` |
| `ai_model_capability` | `uk_ai_model_capability_active` |
| `ai_model_route_policy` | `uk_ai_route_policy_code_active` |
| `ai_model_route_target` | `uk_ai_route_target_active` |
| `ai_page_template` | `uk_template_key_active` |
| `ai_prompt_template` | `uk_ai_prompt_template_code_active` |
| `ai_report_data_business_definition` | `uk_data_business_code_tenant_active` |
| `ai_report_data_connection` | `uk_data_connection_code_tenant_active` |
| `ai_report_data_dataset` | `uk_data_dataset_code_tenant_active` |
| `ai_report_data_dataset_category` | `uk_data_dataset_category_code_tenant_active` |
| `ai_report_data_dimension` | `uk_data_dimension_code_tenant_active` |
| `sample_purchase_order` | `uk_sample_purchase_order_business_key_active`, `uk_sample_purchase_order_no_active` |
| `sys_api_config` | `uk_method_url_active` |
| `sys_config` | `uk_tenant_config_key_active` |
| `sys_data_scope_config` | `uk_tenant_mapper_active` |
| `sys_dict_data` | `uk_tenant_dict_data_active` |
| `sys_dict_type` | `uk_tenant_dict_type_active` |
| `sys_employee` | `uk_emp_no_active` |
| `sys_flow_node_config` | `uk_model_node_active` |
| `sys_job_api_idempotency` | `uk_job_api_idempotency_active` |
| `sys_job_api_token` | `uk_job_api_token_key_active` |
| `sys_job_config` | `uk_job_name_group_active` |
| `sys_message_biz_type` | `uk_tenant_type_active` |
| `sys_message_template` | `uk_tenant_code_active` |
| `sys_org` | `uk_tenant_org_name_active` |
| `sys_outbound_whitelist` | `uk_outbound_whitelist_active` |
| `sys_post` | `uk_tenant_org_post_active`, `uk_tenant_post_code_active` |
| `sys_resource` | `uk_tenant_resource_active` |
| `sys_role` | `uk_tenant_role_key_active`, `uk_tenant_role_name_active` |
| `sys_tenant` | `uk_tenant_name_active` |
| `sys_user` | `sys_user_unique_active` |

### 历史索引漂移

`ai_code_rule` 是已确认的历史例外：V1.0.10 曾创建 `uk_ai_code_rule_code_active`，但 V1.0.36 后续将其删除并使用永久唯一索引 `uk_ai_code_rule_code`。因此迁移不能仅依据早期脚本静态执行 `DROP INDEX`；V1.0.51 会从 `information_schema.STATISTICS` 发现实际存在的索引，并将这两个候选索引统一替换为最终墓碑唯一索引 `uk_ai_code_rule_code_active (tenant_id, rule_code, del_flag)`。

## 不需要机械添加辅助结构的 26 张逻辑删除表

`ai_business_extension_execution_log`、`ai_business_trigger`、`ai_capability_invocation_log`、`ai_context_config`、`ai_custom_query_scheme`、`ai_provider`、`ai_report_directory`、`ai_report_project`、`ai_report_template`、`biz_leave_request`、`sys_file_group`、`sys_file_storage_config`、`sys_flow_approval_level`、`sys_flow_condition_rule`、`sys_flow_entry`、`sys_flow_entry_field_mapping`、`sys_flow_fill_batch`、`sys_flow_fill_batch_item`、`sys_flow_form`、`sys_flow_form_instance`、`sys_flow_model`、`sys_flow_model_version`、`sys_flow_spel_template`、`sys_flow_template`、`sys_job_log`、`sys_notice`。

这些表仍然正常使用逻辑删除。若未来为其中某张表新增业务唯一键，必须先判断唯一性是覆盖历史记录还是仅覆盖未删除记录；只有后者才需要把 `del_flag` 升级为唯一删除标记并加入普通唯一索引。
