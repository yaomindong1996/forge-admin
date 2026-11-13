-- Repair Chinese seed data that was imported through a latin1 client and then
-- persisted as UTF-8. The marker predicate makes every update idempotent: once
-- a value is repaired it no longer contains the mojibake characters below.
SET NAMES utf8mb4;

SET @OLD_FOREIGN_KEY_CHECKS = @@FOREIGN_KEY_CHECKS;
SET FOREIGN_KEY_CHECKS = 0;
SET @MOJIBAKE_PATTERN = '[äåæçèé]';

-- Quartz rows are runtime projections of sys_job_config. Correct rows may already
-- coexist with their mojibake copies, so updating primary keys can collide.
-- Remove only the broken projections; the scheduler recreates enabled jobs.
DELETE FROM `QRTZ_CRON_TRIGGERS` WHERE `trigger_name` REGEXP @MOJIBAKE_PATTERN;
DELETE FROM `QRTZ_TRIGGERS` WHERE `trigger_name` REGEXP @MOJIBAKE_PATTERN OR `job_name` REGEXP @MOJIBAKE_PATTERN;
DELETE FROM `QRTZ_JOB_DETAILS` WHERE `job_name` REGEXP @MOJIBAKE_PATTERN;

UPDATE `ai_business_app` SET `app_name` = CONVERT(BINARY(CONVERT(`app_name` USING latin1)) USING utf8mb4) WHERE `app_name` REGEXP @MOJIBAKE_PATTERN;
UPDATE `ai_business_app` SET `description` = CONVERT(BINARY(CONVERT(`description` USING latin1)) USING utf8mb4) WHERE `description` REGEXP @MOJIBAKE_PATTERN;
UPDATE `ai_business_application` SET `application_name` = CONVERT(BINARY(CONVERT(`application_name` USING latin1)) USING utf8mb4) WHERE `application_name` REGEXP @MOJIBAKE_PATTERN;
UPDATE `ai_business_application` SET `description` = CONVERT(BINARY(CONVERT(`description` USING latin1)) USING utf8mb4) WHERE `description` REGEXP @MOJIBAKE_PATTERN;
UPDATE `ai_business_binding` SET `binding_name` = CONVERT(BINARY(CONVERT(`binding_name` USING latin1)) USING utf8mb4) WHERE `binding_name` REGEXP @MOJIBAKE_PATTERN;
UPDATE `ai_business_binding` SET `description` = CONVERT(BINARY(CONVERT(`description` USING latin1)) USING utf8mb4) WHERE `description` REGEXP @MOJIBAKE_PATTERN;
UPDATE `ai_business_document_config` SET `document_name` = CONVERT(BINARY(CONVERT(`document_name` USING latin1)) USING utf8mb4) WHERE `document_name` REGEXP @MOJIBAKE_PATTERN;
UPDATE `ai_business_object` SET `description` = CONVERT(BINARY(CONVERT(`description` USING latin1)) USING utf8mb4) WHERE `description` REGEXP @MOJIBAKE_PATTERN;
UPDATE `ai_business_object` SET `designer_options` = CONVERT(BINARY(CONVERT(`designer_options` USING latin1)) USING utf8mb4) WHERE `designer_options` REGEXP @MOJIBAKE_PATTERN;
UPDATE `ai_business_object` SET `object_name` = CONVERT(BINARY(CONVERT(`object_name` USING latin1)) USING utf8mb4) WHERE `object_name` REGEXP @MOJIBAKE_PATTERN;
UPDATE `ai_business_object_design_version` SET `designer_options_snapshot` = CONVERT(BINARY(CONVERT(`designer_options_snapshot` USING latin1)) USING utf8mb4) WHERE `designer_options_snapshot` REGEXP @MOJIBAKE_PATTERN;
UPDATE `ai_business_object_design_version` SET `model_snapshot` = CONVERT(BINARY(CONVERT(`model_snapshot` USING latin1)) USING utf8mb4) WHERE `model_snapshot` REGEXP @MOJIBAKE_PATTERN;
UPDATE `ai_business_object_design_version` SET `page_snapshot` = CONVERT(BINARY(CONVERT(`page_snapshot` USING latin1)) USING utf8mb4) WHERE `page_snapshot` REGEXP @MOJIBAKE_PATTERN;
UPDATE `ai_business_object_design_version` SET `remark` = CONVERT(BINARY(CONVERT(`remark` USING latin1)) USING utf8mb4) WHERE `remark` REGEXP @MOJIBAKE_PATTERN;
UPDATE `ai_business_object_relation` SET `description` = CONVERT(BINARY(CONVERT(`description` USING latin1)) USING utf8mb4) WHERE `description` REGEXP @MOJIBAKE_PATTERN;
UPDATE `ai_business_object_relation` SET `relation_name` = CONVERT(BINARY(CONVERT(`relation_name` USING latin1)) USING utf8mb4) WHERE `relation_name` REGEXP @MOJIBAKE_PATTERN;
UPDATE `ai_business_process` SET `draft_schema_json` = CONVERT(BINARY(CONVERT(`draft_schema_json` USING latin1)) USING utf8mb4) WHERE `draft_schema_json` REGEXP @MOJIBAKE_PATTERN;
UPDATE `ai_business_process` SET `process_description` = CONVERT(BINARY(CONVERT(`process_description` USING latin1)) USING utf8mb4) WHERE `process_description` REGEXP @MOJIBAKE_PATTERN;
UPDATE `ai_business_process` SET `process_name` = CONVERT(BINARY(CONVERT(`process_name` USING latin1)) USING utf8mb4) WHERE `process_name` REGEXP @MOJIBAKE_PATTERN;
UPDATE `ai_business_suite` SET `description` = CONVERT(BINARY(CONVERT(`description` USING latin1)) USING utf8mb4) WHERE `description` REGEXP @MOJIBAKE_PATTERN;
UPDATE `ai_business_suite` SET `suite_name` = CONVERT(BINARY(CONVERT(`suite_name` USING latin1)) USING utf8mb4) WHERE `suite_name` REGEXP @MOJIBAKE_PATTERN;
UPDATE `ai_crud_config` SET `app_name` = CONVERT(BINARY(CONVERT(`app_name` USING latin1)) USING utf8mb4) WHERE `app_name` REGEXP @MOJIBAKE_PATTERN;
UPDATE `ai_crud_config` SET `columns_schema` = CONVERT(BINARY(CONVERT(`columns_schema` USING latin1)) USING utf8mb4) WHERE `columns_schema` REGEXP @MOJIBAKE_PATTERN;
UPDATE `ai_crud_config` SET `edit_schema` = CONVERT(BINARY(CONVERT(`edit_schema` USING latin1)) USING utf8mb4) WHERE `edit_schema` REGEXP @MOJIBAKE_PATTERN;
UPDATE `ai_crud_config` SET `menu_name` = CONVERT(BINARY(CONVERT(`menu_name` USING latin1)) USING utf8mb4) WHERE `menu_name` REGEXP @MOJIBAKE_PATTERN;
UPDATE `ai_crud_config` SET `model_schema` = CONVERT(BINARY(CONVERT(`model_schema` USING latin1)) USING utf8mb4) WHERE `model_schema` REGEXP @MOJIBAKE_PATTERN;
UPDATE `ai_crud_config` SET `object_name` = CONVERT(BINARY(CONVERT(`object_name` USING latin1)) USING utf8mb4) WHERE `object_name` REGEXP @MOJIBAKE_PATTERN;
UPDATE `ai_crud_config` SET `options` = CONVERT(BINARY(CONVERT(`options` USING latin1)) USING utf8mb4) WHERE `options` REGEXP @MOJIBAKE_PATTERN;
UPDATE `ai_crud_config` SET `page_schema` = CONVERT(BINARY(CONVERT(`page_schema` USING latin1)) USING utf8mb4) WHERE `page_schema` REGEXP @MOJIBAKE_PATTERN;
UPDATE `ai_crud_config` SET `search_schema` = CONVERT(BINARY(CONVERT(`search_schema` USING latin1)) USING utf8mb4) WHERE `search_schema` REGEXP @MOJIBAKE_PATTERN;
UPDATE `ai_crud_config` SET `table_comment` = CONVERT(BINARY(CONVERT(`table_comment` USING latin1)) USING utf8mb4) WHERE `table_comment` REGEXP @MOJIBAKE_PATTERN;
UPDATE `ai_lowcode_domain` SET `domain_desc` = CONVERT(BINARY(CONVERT(`domain_desc` USING latin1)) USING utf8mb4) WHERE `domain_desc` REGEXP @MOJIBAKE_PATTERN;
UPDATE `ai_lowcode_domain` SET `domain_name` = CONVERT(BINARY(CONVERT(`domain_name` USING latin1)) USING utf8mb4) WHERE `domain_name` REGEXP @MOJIBAKE_PATTERN;
UPDATE `ai_lowcode_domain` SET `domain_schema` = CONVERT(BINARY(CONVERT(`domain_schema` USING latin1)) USING utf8mb4) WHERE `domain_schema` REGEXP @MOJIBAKE_PATTERN;
UPDATE `ai_lowcode_model` SET `model_desc` = CONVERT(BINARY(CONVERT(`model_desc` USING latin1)) USING utf8mb4) WHERE `model_desc` REGEXP @MOJIBAKE_PATTERN;
UPDATE `ai_lowcode_model` SET `model_name` = CONVERT(BINARY(CONVERT(`model_name` USING latin1)) USING utf8mb4) WHERE `model_name` REGEXP @MOJIBAKE_PATTERN;
UPDATE `ai_lowcode_model` SET `model_schema` = CONVERT(BINARY(CONVERT(`model_schema` USING latin1)) USING utf8mb4) WHERE `model_schema` REGEXP @MOJIBAKE_PATTERN;

UPDATE `sys_client` SET `client_name` = CONVERT(BINARY(CONVERT(`client_name` USING latin1)) USING utf8mb4) WHERE `client_name` REGEXP @MOJIBAKE_PATTERN;
UPDATE `sys_client` SET `description` = CONVERT(BINARY(CONVERT(`description` USING latin1)) USING utf8mb4) WHERE `description` REGEXP @MOJIBAKE_PATTERN;
UPDATE `sys_dict_data` SET `dict_label` = CONVERT(BINARY(CONVERT(`dict_label` USING latin1)) USING utf8mb4) WHERE `dict_label` REGEXP @MOJIBAKE_PATTERN;
UPDATE `sys_dict_data` SET `remark` = CONVERT(BINARY(CONVERT(`remark` USING latin1)) USING utf8mb4) WHERE `remark` REGEXP @MOJIBAKE_PATTERN;
UPDATE `sys_dict_type` SET `dict_name` = CONVERT(BINARY(CONVERT(`dict_name` USING latin1)) USING utf8mb4) WHERE `dict_name` REGEXP @MOJIBAKE_PATTERN;
UPDATE `sys_dict_type` SET `remark` = CONVERT(BINARY(CONVERT(`remark` USING latin1)) USING utf8mb4) WHERE `remark` REGEXP @MOJIBAKE_PATTERN;
UPDATE `sys_external_api` SET `api_desc` = CONVERT(BINARY(CONVERT(`api_desc` USING latin1)) USING utf8mb4) WHERE `api_desc` REGEXP @MOJIBAKE_PATTERN;
UPDATE `sys_external_api` SET `api_name` = CONVERT(BINARY(CONVERT(`api_name` USING latin1)) USING utf8mb4) WHERE `api_name` REGEXP @MOJIBAKE_PATTERN;
UPDATE `sys_external_api` SET `input_schema_json` = CONVERT(BINARY(CONVERT(`input_schema_json` USING latin1)) USING utf8mb4) WHERE `input_schema_json` REGEXP @MOJIBAKE_PATTERN;
UPDATE `sys_external_api` SET `mock_response_json` = CONVERT(BINARY(CONVERT(`mock_response_json` USING latin1)) USING utf8mb4) WHERE `mock_response_json` REGEXP @MOJIBAKE_PATTERN;
UPDATE `sys_external_api` SET `output_schema_json` = CONVERT(BINARY(CONVERT(`output_schema_json` USING latin1)) USING utf8mb4) WHERE `output_schema_json` REGEXP @MOJIBAKE_PATTERN;
UPDATE `sys_external_api` SET `remark` = CONVERT(BINARY(CONVERT(`remark` USING latin1)) USING utf8mb4) WHERE `remark` REGEXP @MOJIBAKE_PATTERN;
UPDATE `sys_external_system` SET `remark` = CONVERT(BINARY(CONVERT(`remark` USING latin1)) USING utf8mb4) WHERE `remark` REGEXP @MOJIBAKE_PATTERN;
UPDATE `sys_external_system` SET `system_desc` = CONVERT(BINARY(CONVERT(`system_desc` USING latin1)) USING utf8mb4) WHERE `system_desc` REGEXP @MOJIBAKE_PATTERN;
UPDATE `sys_external_system` SET `system_name` = CONVERT(BINARY(CONVERT(`system_name` USING latin1)) USING utf8mb4) WHERE `system_name` REGEXP @MOJIBAKE_PATTERN;
UPDATE `sys_flow_comment_phrase` SET `content` = CONVERT(BINARY(CONVERT(`content` USING latin1)) USING utf8mb4) WHERE `content` REGEXP @MOJIBAKE_PATTERN;
UPDATE `sys_job_config` SET `description` = CONVERT(BINARY(CONVERT(`description` USING latin1)) USING utf8mb4) WHERE `description` REGEXP @MOJIBAKE_PATTERN;
-- Some job definitions were later re-seeded correctly. Logically delete only a
-- broken active duplicate before normalizing the remaining unique job name.
UPDATE `sys_job_config` AS `broken`
INNER JOIN `sys_job_config` AS `correct`
  ON `correct`.`job_group` = `broken`.`job_group`
 AND `correct`.`job_name` = CONVERT(BINARY(CONVERT(`broken`.`job_name` USING latin1)) USING utf8mb4)
 AND `correct`.`del_flag` = 0
 AND `correct`.`id` <> `broken`.`id`
SET `broken`.`del_flag` = `broken`.`id`
WHERE `broken`.`del_flag` = 0
  AND `broken`.`job_name` REGEXP @MOJIBAKE_PATTERN;
UPDATE `sys_job_config` SET `job_name` = CONVERT(BINARY(CONVERT(`job_name` USING latin1)) USING utf8mb4) WHERE `job_name` REGEXP @MOJIBAKE_PATTERN;
UPDATE `sys_message_template` SET `content_template` = CONVERT(BINARY(CONVERT(`content_template` USING latin1)) USING utf8mb4) WHERE `content_template` REGEXP @MOJIBAKE_PATTERN;
UPDATE `sys_message_template` SET `remark` = CONVERT(BINARY(CONVERT(`remark` USING latin1)) USING utf8mb4) WHERE `remark` REGEXP @MOJIBAKE_PATTERN;
UPDATE `sys_message_template` SET `template_name` = CONVERT(BINARY(CONVERT(`template_name` USING latin1)) USING utf8mb4) WHERE `template_name` REGEXP @MOJIBAKE_PATTERN;
UPDATE `sys_message_template` SET `title_template` = CONVERT(BINARY(CONVERT(`title_template` USING latin1)) USING utf8mb4) WHERE `title_template` REGEXP @MOJIBAKE_PATTERN;
UPDATE `sys_org` SET `org_name` = CONVERT(BINARY(CONVERT(`org_name` USING latin1)) USING utf8mb4) WHERE `org_name` REGEXP @MOJIBAKE_PATTERN;
UPDATE `sys_org` SET `remark` = CONVERT(BINARY(CONVERT(`remark` USING latin1)) USING utf8mb4) WHERE `remark` REGEXP @MOJIBAKE_PATTERN;
UPDATE `sys_outbound_whitelist` SET `remark` = CONVERT(BINARY(CONVERT(`remark` USING latin1)) USING utf8mb4) WHERE `remark` REGEXP @MOJIBAKE_PATTERN;
UPDATE `sys_resource` SET `icon` = CONVERT(BINARY(CONVERT(`icon` USING latin1)) USING utf8mb4) WHERE `icon` REGEXP @MOJIBAKE_PATTERN;
UPDATE `sys_resource` SET `perms` = CONVERT(BINARY(CONVERT(`perms` USING latin1)) USING utf8mb4) WHERE `perms` REGEXP @MOJIBAKE_PATTERN;
UPDATE `sys_resource` SET `remark` = CONVERT(BINARY(CONVERT(`remark` USING latin1)) USING utf8mb4) WHERE `remark` REGEXP @MOJIBAKE_PATTERN;
UPDATE `sys_resource` SET `resource_name` = CONVERT(BINARY(CONVERT(`resource_name` USING latin1)) USING utf8mb4) WHERE `resource_name` REGEXP @MOJIBAKE_PATTERN;
UPDATE `sys_resource_0607` SET `icon` = CONVERT(BINARY(CONVERT(`icon` USING latin1)) USING utf8mb4) WHERE `icon` REGEXP @MOJIBAKE_PATTERN;
UPDATE `sys_resource_0607` SET `perms` = CONVERT(BINARY(CONVERT(`perms` USING latin1)) USING utf8mb4) WHERE `perms` REGEXP @MOJIBAKE_PATTERN;
UPDATE `sys_role` SET `remark` = CONVERT(BINARY(CONVERT(`remark` USING latin1)) USING utf8mb4) WHERE `remark` REGEXP @MOJIBAKE_PATTERN;
UPDATE `sys_role` SET `role_name` = CONVERT(BINARY(CONVERT(`role_name` USING latin1)) USING utf8mb4) WHERE `role_name` REGEXP @MOJIBAKE_PATTERN;
UPDATE `sys_social_app_config` SET `app_name` = CONVERT(BINARY(CONVERT(`app_name` USING latin1)) USING utf8mb4) WHERE `app_name` REGEXP @MOJIBAKE_PATTERN;
UPDATE `sys_social_app_config` SET `remark` = CONVERT(BINARY(CONVERT(`remark` USING latin1)) USING utf8mb4) WHERE `remark` REGEXP @MOJIBAKE_PATTERN;
UPDATE `sys_social_capability_binding` SET `remark` = CONVERT(BINARY(CONVERT(`remark` USING latin1)) USING utf8mb4) WHERE `remark` REGEXP @MOJIBAKE_PATTERN;
UPDATE `sys_social_config` SET `connection_name` = CONVERT(BINARY(CONVERT(`connection_name` USING latin1)) USING utf8mb4) WHERE `connection_name` REGEXP @MOJIBAKE_PATTERN;
UPDATE `sys_social_config` SET `remark` = CONVERT(BINARY(CONVERT(`remark` USING latin1)) USING utf8mb4) WHERE `remark` REGEXP @MOJIBAKE_PATTERN;
UPDATE `sys_tenant` SET `browser_title` = CONVERT(BINARY(CONVERT(`browser_title` USING latin1)) USING utf8mb4) WHERE `browser_title` REGEXP @MOJIBAKE_PATTERN;
UPDATE `sys_tenant` SET `system_name` = CONVERT(BINARY(CONVERT(`system_name` USING latin1)) USING utf8mb4) WHERE `system_name` REGEXP @MOJIBAKE_PATTERN;
UPDATE `sys_tenant` SET `tenant_desc` = CONVERT(BINARY(CONVERT(`tenant_desc` USING latin1)) USING utf8mb4) WHERE `tenant_desc` REGEXP @MOJIBAKE_PATTERN;
UPDATE `sys_tenant` SET `tenant_name` = CONVERT(BINARY(CONVERT(`tenant_name` USING latin1)) USING utf8mb4) WHERE `tenant_name` REGEXP @MOJIBAKE_PATTERN;

SET FOREIGN_KEY_CHECKS = @OLD_FOREIGN_KEY_CHECKS;
