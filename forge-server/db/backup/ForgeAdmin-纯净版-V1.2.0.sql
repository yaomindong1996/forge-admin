-- =============================================================================
-- Forge Admin 纯净版初始化 SQL
-- Version : V1.2.0
-- Source  : 测试库克隆后经 clean-db.sh 清理
-- Keep    : 默认租户/管理员/角色、菜单权限、字典参数区划、任务配置、内置模板、
--           Flyway 历史、本地存储默认配置
-- Removed : 低代码业务表与演示数据、流程运行/模型垃圾数据、多租户与测试账号、
--           日志/文件运行数据；敏感配置默认不导出
-- Note    : Flowable act_*/flw_* 表均不导出（含 flw_ru_batch），应用首次启动时自动创建
-- Login   : admin / 123456
-- Usage   : 仅导入空库；启动 Admin 后 Flyway 应显示已是最新
-- Generated: 2026-10-13
-- =============================================================================

-- Forge safe full dump
-- Database: forge_admin_v120_pure
-- Generated at: 2026-10-13 10:54:33 +0800
-- DDL: all tables, routines, events and triggers
-- Data: INSERT statements, excluding full-skip, log/runtime and sensitive tables listed below
--
-- Fully skipped tables:
--   act_app_appdef
--   act_app_deployment
--   act_app_deployment_resource
--   act_cmmn_casedef
--   act_cmmn_deployment
--   act_cmmn_deployment_resource
--   act_cmmn_hi_case_inst
--   act_cmmn_hi_mil_inst
--   act_cmmn_hi_plan_item_inst
--   act_cmmn_ru_case_inst
--   act_cmmn_ru_mil_inst
--   act_cmmn_ru_plan_item_inst
--   act_cmmn_ru_sentry_part_inst
--   act_dmn_decision
--   act_dmn_deployment
--   act_dmn_deployment_resource
--   act_dmn_hi_decision_execution
--   act_evt_log
--   act_ge_bytearray
--   act_ge_property
--   act_hi_actinst
--   act_hi_attachment
--   act_hi_comment
--   act_hi_detail
--   act_hi_entitylink
--   act_hi_identitylink
--   act_hi_procinst
--   act_hi_taskinst
--   act_hi_tsk_log
--   act_hi_varinst
--   act_id_bytearray
--   act_id_group
--   act_id_info
--   act_id_membership
--   act_id_priv
--   act_id_priv_mapping
--   act_id_property
--   act_id_token
--   act_id_user
--   act_procdef_info
--   act_re_deployment
--   act_re_model
--   act_re_procdef
--   act_ru_actinst
--   act_ru_deadletter_job
--   act_ru_entitylink
--   act_ru_event_subscr
--   act_ru_execution
--   act_ru_external_job
--   act_ru_history_job
--   act_ru_identitylink
--   act_ru_job
--   act_ru_suspended_job
--   act_ru_task
--   act_ru_timer_job
--   act_ru_variable
--   ai_business_application_version
--   ai_business_extension_version
--   ai_business_object_design_version
--   ai_business_process_version
--   ai_capability_version
--   ai_crud_config_version
--   ai_formula_function_version
--   ai_report_project_version
--   sys_flow_form_version
--   sys_flow_model_version
--   sys_print_template_version
--
-- Data skipped tables:
--   act_evt_log
--   act_hi_tsk_log
--   ai_business_action_execution_log
--   ai_business_extension_execution_log
--   ai_business_message_channel
--   ai_business_trigger_log
--   ai_capability_flow_action_log
--   ai_capability_invocation_log
--   ai_chat_record
--   ai_chat_session
--   ai_crud_export_task
--   ai_dashboard_generate_record
--   ai_formula_execution_log
--   ai_model
--   ai_model_invocation_log
--   ai_provider
--   ai_report_data_connection
--   config_properties
--   forge_schema_history
--   forge_website_schema_history
--   gen_datasource
--   gen_project_task_log
--   qrtz_fired_triggers
--   qrtz_locks
--   qrtz_scheduler_state
--   sys_api_config
--   sys_auth_online_user
--   sys_auth_online_user_history
--   sys_email_config
--   sys_external_api_log
--   sys_file_metadata
--   sys_file_storage_config
--   sys_flow_error_log
--   sys_job_log
--   sys_login_log
--   sys_operation_log
--   sys_sms_config
--   sys_social_config
--   sys_social_sync_log
--   sys_user_password_history
--   sys_user_social
--   worker_node

-- Restore with: mysql <options> forge_admin_v120_pure < this_file.sql
SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;

-- Schema
-- MySQL dump 10.13  Distrib 5.7.24, for osx11.1 (x86_64)
--
-- Host: 120.48.96.178    Database: forge_admin_v120_pure
-- ------------------------------------------------------
-- Server version	8.0.32-2.0.0.2

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `ai_agent`
--

DROP TABLE IF EXISTS `ai_agent`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_agent` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户ID',
  `agent_name` varchar(100) NOT NULL COMMENT 'Agent名称',
  `agent_code` varchar(50) NOT NULL COMMENT 'Agent编码（唯一）',
  `description` varchar(500) DEFAULT NULL COMMENT '描述',
  `system_prompt` longtext NOT NULL COMMENT '系统提示词模板',
  `provider_id` bigint DEFAULT NULL COMMENT '关联供应商ID',
  `model_name` varchar(100) DEFAULT NULL COMMENT '使用的模型',
  `model_selection_mode` varchar(16) NOT NULL DEFAULT 'PINNED' COMMENT '模型选择模式（PINNED/POLICY）',
  `route_policy_id` bigint DEFAULT NULL COMMENT '模型路由策略ID',
  `temperature` decimal(3,2) DEFAULT '0.70' COMMENT '温度参数（0-1）',
  `max_tokens` int DEFAULT '4000' COMMENT '最大Token数',
  `extra_config` json DEFAULT NULL COMMENT '扩展配置',
  `greeting` varchar(500) DEFAULT NULL COMMENT '问候语',
  `preset_questions` text COMMENT '预设问题JSON数组',
  `max_iters` int DEFAULT '10' COMMENT 'ReAct最大轮次',
  `knowledge_ids` text COMMENT '关联知识库ID列表JSON',
  `rag_mode` varchar(32) DEFAULT 'none' COMMENT 'RAG模式(none/forced/smart)',
  `tool_group_mode` varchar(32) DEFAULT 'all' COMMENT '工具组模式(all/skill)',
  `asr_model_id` bigint DEFAULT NULL COMMENT '语音识别模型ID',
  `tts_model_id` bigint DEFAULT NULL COMMENT '语音合成模型ID',
  `status` char(1) NOT NULL DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  `create_dept` varchar(30) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_agent_code_active` (`agent_code`,`del_flag`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2084185152911921155 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI Agent配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_agent_event`
--

DROP TABLE IF EXISTS `ai_agent_event`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_agent_event` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `session_id` varchar(64) NOT NULL COMMENT '会话ID',
  `turn_index` int NOT NULL COMMENT 'ReAct轮次',
  `event_type` varchar(50) NOT NULL COMMENT '事件类型(28种)',
  `event_data` longtext COMMENT '事件数据JSON',
  `parent_id` bigint DEFAULT NULL COMMENT '父事件ID(工具结果关联工具调用)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_session` (`session_id`,`turn_index`),
  KEY `idx_session_type` (`session_id`,`event_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Agent事件流(全量持久化)';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_agent_generate_record`
--

DROP TABLE IF EXISTS `ai_agent_generate_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_agent_generate_record` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL,
  `user_id` bigint DEFAULT NULL COMMENT '用户ID',
  `description` longtext NOT NULL COMMENT '用户需求描述',
  `generated_config_json` longtext COMMENT '生成结果(名称/描述/问候语/预设问题/指令/推荐绑定)',
  `status` varchar(32) DEFAULT 'pending' COMMENT '状态(generating/success/failed)',
  `error_msg` varchar(1000) DEFAULT NULL COMMENT '错误信息',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_dept` bigint DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标志(0正常，删除后写主键)',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI创建Agent生成记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_agent_skill`
--

DROP TABLE IF EXISTS `ai_agent_skill`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_agent_skill` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL,
  `agent_id` bigint NOT NULL,
  `skill_id` bigint NOT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_dept` bigint DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标志(0正常，删除后写主键)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_agent_skill_active` (`agent_id`,`skill_id`,`del_flag`),
  KEY `idx_agent` (`agent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Agent技能绑定';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_agent_tool_config`
--

DROP TABLE IF EXISTS `ai_agent_tool_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_agent_tool_config` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL,
  `agent_id` bigint NOT NULL COMMENT 'Agent ID',
  `tool_source` varchar(32) NOT NULL COMMENT '工具来源(mcp/builtin/capability)',
  `tool_key` varchar(200) NOT NULL COMMENT '工具标识',
  `tool_group` varchar(64) DEFAULT 'default' COMMENT '工具组(技能激活)',
  `enabled` char(1) DEFAULT '0' COMMENT '是否启用(0否 1是)',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_dept` bigint DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标志(0正常，删除后写主键)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_agent_tool_active` (`agent_id`,`tool_source`,`tool_key`,`del_flag`),
  KEY `idx_agent` (`agent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Agent工具绑定';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_agent_tool_permission`
--

DROP TABLE IF EXISTS `ai_agent_tool_permission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_agent_tool_permission` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL,
  `agent_id` bigint NOT NULL,
  `tool_key` varchar(200) NOT NULL COMMENT '工具标识',
  `decision` varchar(16) NOT NULL COMMENT '权限(allowed/ask/denied)',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_dept` bigint DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标志(0正常，删除后写主键)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_agent_perm_active` (`agent_id`,`tool_key`,`del_flag`),
  KEY `idx_agent_tool` (`agent_id`,`tool_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Agent工具权限(ALLOW/ASK/DENY)';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_application_capability`
--

DROP TABLE IF EXISTS `ai_application_capability`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_application_capability` (
  `id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL DEFAULT '1',
  `application_id` bigint NOT NULL,
  `capability_id` bigint NOT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_dept` bigint DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_application_capability` (`tenant_id`,`application_id`,`capability_id`,`del_flag`),
  KEY `idx_application_capability_reverse` (`tenant_id`,`capability_id`,`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='低代码应用对外能力关联';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_application_integration`
--

DROP TABLE IF EXISTS `ai_application_integration`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_application_integration` (
  `id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL DEFAULT '1',
  `application_id` bigint NOT NULL,
  `connection_id` bigint DEFAULT NULL,
  `revision` bigint NOT NULL DEFAULT '0',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_dept` bigint DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_application_integration` (`tenant_id`,`application_id`,`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='低代码应用集成引用';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_business_action_execution_log`
--

DROP TABLE IF EXISTS `ai_business_action_execution_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_business_action_execution_log` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `suite_code` varchar(64) DEFAULT NULL COMMENT '业务套件编码',
  `object_code` varchar(64) NOT NULL COMMENT '业务对象编码',
  `record_id` varchar(128) NOT NULL DEFAULT '' COMMENT '业务记录ID',
  `action_code` varchar(64) NOT NULL COMMENT '动作编码',
  `action_name` varchar(128) DEFAULT NULL COMMENT '动作名称',
  `action_version` int NOT NULL DEFAULT '0' COMMENT '不可变业务对象发布版本',
  `execution_mode` varchar(32) NOT NULL DEFAULT 'ORCHESTRATION' COMMENT 'LOCAL_TRANSACTION/ORCHESTRATION',
  `execute_status` varchar(32) NOT NULL COMMENT '执行状态：RUNNING/SUCCESS/FAILED/TODO',
  `request_digest` varchar(1024) DEFAULT NULL COMMENT '请求摘要',
  `step_result` json DEFAULT NULL COMMENT '步骤执行结果JSON',
  `result_message` varchar(500) DEFAULT NULL COMMENT '执行结果提示',
  `error_message` varchar(2000) DEFAULT NULL COMMENT '错误信息',
  `correlation_id` varchar(64) DEFAULT NULL COMMENT '链路ID',
  `idempotency_key` varchar(128) DEFAULT NULL COMMENT '幂等键',
  `duration_ms` bigint DEFAULT NULL COMMENT '执行耗时毫秒',
  `capability_request_id` varchar(64) DEFAULT NULL COMMENT 'AI能力调用请求ID',
  `client_id` bigint DEFAULT NULL COMMENT 'AI能力机器客户端ID',
  `service_user_id` bigint DEFAULT NULL COMMENT 'AI能力绑定服务账号ID',
  `actor_type` varchar(16) DEFAULT NULL COMMENT 'AI能力实际主体类型（USER/SERVICE）',
  `audit_event_type` varchar(32) DEFAULT NULL COMMENT '结构化审计事件类型',
  `status_field` varchar(64) DEFAULT NULL COMMENT '状态字段编码',
  `status_from` varchar(64) DEFAULT NULL COMMENT '状态迁移前值',
  `status_to` varchar(64) DEFAULT NULL COMMENT '状态迁移后值',
  `change_summary` varchar(4000) DEFAULT NULL COMMENT '脱敏结构化变更摘要',
  `retention_until` datetime DEFAULT NULL COMMENT '审计留存截止时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_business_action_version_idem` (`tenant_id`,`object_code`,`record_id`,`action_code`,`action_version`,`idempotency_key`),
  KEY `idx_ai_business_action_record` (`tenant_id`,`object_code`,`record_id`,`create_time`),
  KEY `idx_ai_business_action_code` (`tenant_id`,`object_code`,`action_code`,`execute_status`,`create_time`),
  KEY `idx_ai_business_action_corr` (`tenant_id`,`correlation_id`),
  KEY `idx_ai_business_action_capability_request` (`tenant_id`,`capability_request_id`),
  KEY `idx_ai_business_action_audit` (`tenant_id`,`object_code`,`record_id`,`audit_event_type`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI业务应用平台-通用动作执行日志';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_business_app`
--

DROP TABLE IF EXISTS `ai_business_app`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_business_app` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `app_code` varchar(64) NOT NULL COMMENT '应用入口编码',
  `app_name` varchar(128) NOT NULL COMMENT '应用入口名称',
  `app_type` varchar(32) NOT NULL COMMENT '应用类型：BUSINESS/EMBEDDED/MOBILE/INTEGRATION',
  `application_id` bigint DEFAULT NULL COMMENT '归属业务应用聚合ID',
  `suite_code` varchar(48) NOT NULL COMMENT '所属业务套件编码',
  `object_code` varchar(48) DEFAULT NULL COMMENT '关联业务对象编码',
  `entry_mode` varchar(32) NOT NULL COMMENT '入口模式：RUNTIME/ROUTE/IFRAME/EXTERNAL/H5/API',
  `entry_url` varchar(512) DEFAULT NULL COMMENT '入口地址',
  `config_key` varchar(128) DEFAULT NULL COMMENT '关联低代码运行配置键',
  `icon` varchar(128) DEFAULT NULL COMMENT '应用图标',
  `description` varchar(500) DEFAULT NULL COMMENT '业务说明',
  `status` int NOT NULL DEFAULT '1' COMMENT '状态：1启用 0禁用',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `options` json DEFAULT NULL COMMENT '扩展配置',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_business_app_code_active` (`tenant_id`,`app_code`,`del_flag`),
  KEY `idx_ai_business_app_suite` (`tenant_id`,`suite_code`,`app_type`,`status`,`sort_order`),
  KEY `idx_ai_business_app_object` (`tenant_id`,`suite_code`,`object_code`,`status`),
  KEY `idx_ai_business_app_config_key` (`tenant_id`,`config_key`),
  KEY `idx_ai_business_app_application` (`tenant_id`,`application_id`,`status`,`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI业务应用平台-应用入口表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_business_application`
--

DROP TABLE IF EXISTS `ai_business_application`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_business_application` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `application_code` varchar(128) NOT NULL COMMENT '业务应用编码',
  `portal_slug` varchar(50) DEFAULT NULL COMMENT '门户访问 slug',
  `application_name` varchar(128) NOT NULL COMMENT '业务应用名称',
  `suite_code` varchar(128) NOT NULL COMMENT '所属业务域编码',
  `icon` varchar(255) DEFAULT NULL COMMENT '应用图标',
  `description` varchar(500) DEFAULT NULL COMMENT '应用说明',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1启用 0禁用',
  `design_status` varchar(32) NOT NULL DEFAULT 'DRAFT' COMMENT '设计状态：DRAFT/READY/PUBLISHED/CHANGED',
  `last_publish_version` int DEFAULT NULL COMMENT '最近发布版本号',
  `last_publish_time` datetime DEFAULT NULL COMMENT '最近发布时间',
  `options` json DEFAULT NULL COMMENT '扩展配置，不保存敏感密钥',
  `portal_config` json DEFAULT NULL COMMENT '门户主题、水印和导航配置',
  `ai_assistant_config` json DEFAULT NULL COMMENT '应用 AI 助理绑定配置',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_business_application_code_active` (`tenant_id`,`application_code`,`del_flag`),
  UNIQUE KEY `uk_ai_business_application_portal_slug_active` (`tenant_id`,`portal_slug`,`del_flag`),
  KEY `idx_ai_business_application_suite` (`tenant_id`,`suite_code`,`status`,`design_status`,`del_flag`),
  KEY `idx_ai_business_application_update` (`tenant_id`,`update_time`,`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='低代码业务应用聚合';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_business_application_object`
--

DROP TABLE IF EXISTS `ai_business_application_object`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_business_application_object` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `application_id` bigint NOT NULL COMMENT '业务应用ID',
  `object_id` bigint NOT NULL COMMENT '业务对象ID',
  `object_role` varchar(32) NOT NULL COMMENT '对象角色：PRIMARY/DETAIL/REFERENCE/SHARED',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '应用内排序',
  `options` json DEFAULT NULL COMMENT '应用内对象配置',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_business_application_object_active` (`tenant_id`,`application_id`,`object_id`,`del_flag`),
  KEY `idx_ai_business_application_object_app` (`tenant_id`,`application_id`,`object_role`,`sort_order`,`del_flag`),
  KEY `idx_ai_business_application_object_object` (`tenant_id`,`object_id`,`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='业务应用与业务对象关联';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_business_application_publish_run`
--

DROP TABLE IF EXISTS `ai_business_application_publish_run`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_business_application_publish_run` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `application_id` bigint NOT NULL COMMENT '业务应用ID',
  `idempotency_key` varchar(128) NOT NULL COMMENT '应用内发布幂等键',
  `operation_type` varchar(16) NOT NULL DEFAULT 'PUBLISH' COMMENT 'PUBLISH/ROLLBACK',
  `target_version_no` int NOT NULL COMMENT '本次发布目标版本号',
  `source_version_no` int DEFAULT NULL COMMENT '回滚来源版本号',
  `run_status` varchar(24) NOT NULL DEFAULT 'CREATED' COMMENT 'CREATED/RUNNING/PARTIAL/FAILED/SUCCESS',
  `current_step` varchar(32) DEFAULT NULL COMMENT '当前或失败步骤',
  `snapshot_json` longtext NOT NULL COMMENT '候选发布快照，不保存密钥',
  `snapshot_hash` char(64) NOT NULL COMMENT '候选快照SHA-256摘要',
  `selection_json` json NOT NULL COMMENT '本次选择及自动补齐的依赖资产',
  `step_results_json` json NOT NULL COMMENT '可恢复步骤结果，不保存原始异常',
  `result_version_id` bigint DEFAULT NULL COMMENT '成功生成的不可变应用版本ID',
  `error_code` varchar(64) DEFAULT NULL COMMENT '脱敏错误码',
  `error_summary` varchar(500) DEFAULT NULL COMMENT '脱敏错误摘要',
  `attempt_count` int NOT NULL DEFAULT '1' COMMENT '发布或恢复尝试次数',
  `started_by` bigint DEFAULT NULL COMMENT '可信发起人ID',
  `started_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '首次发起时间',
  `finished_time` datetime DEFAULT NULL COMMENT '完成或最近失败时间',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_business_publish_run_key_active` (`tenant_id`,`application_id`,`idempotency_key`,`del_flag`),
  UNIQUE KEY `uk_ai_business_publish_run_version_active` (`tenant_id`,`application_id`,`target_version_no`,`del_flag`),
  KEY `idx_ai_business_publish_run_status` (`tenant_id`,`application_id`,`run_status`,`update_time`,`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='低代码业务应用协调发布运行单';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_business_binding`
--

DROP TABLE IF EXISTS `ai_business_binding`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_business_binding` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `target_type` varchar(32) NOT NULL COMMENT '挂接目标类型：SUITE/OBJECT/APP',
  `target_id` bigint DEFAULT NULL COMMENT '挂接目标ID',
  `target_code` varchar(128) NOT NULL COMMENT '挂接目标编码',
  `binding_type` varchar(32) NOT NULL COMMENT '能力类型：FLOW/APPROVAL/REPORT/PERMISSION/MESSAGE/TRIGGER/IMPORT/EXPORT/MOBILE/INTEGRATION',
  `binding_key` varchar(128) NOT NULL COMMENT '能力业务键',
  `binding_name` varchar(128) NOT NULL COMMENT '挂接名称',
  `binding_config` json DEFAULT NULL COMMENT '挂接配置',
  `description` varchar(500) DEFAULT NULL COMMENT '挂接说明',
  `status` int NOT NULL DEFAULT '1' COMMENT '状态：1启用 0禁用',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_business_binding_target` (`tenant_id`,`target_type`,`target_code`,`binding_type`,`binding_key`),
  KEY `idx_ai_business_binding_target_id` (`tenant_id`,`target_type`,`target_id`,`binding_type`),
  KEY `idx_ai_business_binding_type` (`tenant_id`,`binding_type`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI业务应用平台-能力挂接表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_business_document_config`
--

DROP TABLE IF EXISTS `ai_business_document_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_business_document_config` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `object_id` bigint DEFAULT NULL COMMENT '业务对象ID',
  `suite_code` varchar(48) NOT NULL COMMENT '业务套件编码',
  `object_code` varchar(48) NOT NULL COMMENT '业务对象编码',
  `config_key` varchar(128) DEFAULT NULL COMMENT '动态CRUD运行配置键',
  `document_name` varchar(128) NOT NULL COMMENT '单据名称',
  `document_no_rule` varchar(256) DEFAULT NULL COMMENT '单据编号规则',
  `document_enabled` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否启用单据模式',
  `status_field` varchar(64) DEFAULT NULL COMMENT '单据状态字段',
  `starter_field` varchar(64) DEFAULT NULL COMMENT '发起人字段',
  `owner_field` varchar(64) DEFAULT NULL COMMENT '负责人字段',
  `default_flow_key` varchar(128) DEFAULT NULL COMMENT '默认流程模型Key',
  `status_mapping` json DEFAULT NULL COMMENT '单据状态映射JSON',
  `options` json DEFAULT NULL COMMENT '扩展配置JSON',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_business_document_object_code` (`tenant_id`,`object_code`),
  UNIQUE KEY `uk_ai_business_document_object_id` (`tenant_id`,`object_id`),
  KEY `idx_ai_business_document_suite` (`tenant_id`,`suite_code`),
  KEY `idx_ai_business_document_config` (`tenant_id`,`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI业务应用平台-业务单据配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_business_event_outbox`
--

DROP TABLE IF EXISTS `ai_business_event_outbox`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_business_event_outbox` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `event_id` varchar(128) NOT NULL COMMENT '稳定事件ID',
  `event_source` varchar(32) NOT NULL COMMENT '可信事件来源',
  `event_version` int NOT NULL COMMENT '事件协议版本',
  `source_digest` char(64) NOT NULL COMMENT '不含聚合序号的逻辑载荷摘要',
  `event_digest` char(64) NOT NULL COMMENT '完整信封载荷摘要',
  `event_type` varchar(64) NOT NULL COMMENT '事件类型',
  `suite_code` varchar(128) DEFAULT NULL COMMENT '业务套件编码',
  `object_code` varchar(128) NOT NULL COMMENT '业务对象编码',
  `config_key` varchar(128) DEFAULT NULL COMMENT '运行配置键',
  `record_id` varchar(128) NOT NULL COMMENT '业务记录ID',
  `aggregate_key` char(64) NOT NULL COMMENT '聚合摘要',
  `aggregate_sequence` bigint NOT NULL COMMENT '聚合内单调序号',
  `event_payload` json NOT NULL COMMENT '可信事件完整快照',
  `delivery_status` varchar(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PROCESSING/DELIVERED/FAILED/DEAD',
  `retry_count` int NOT NULL DEFAULT '0' COMMENT '认领次数',
  `next_retry_time` datetime DEFAULT NULL COMMENT '下次重试时间',
  `lock_owner` varchar(128) DEFAULT NULL COMMENT '投递租约持有者',
  `lock_time` datetime DEFAULT NULL COMMENT '投递租约时间',
  `error_type` varchar(128) DEFAULT NULL COMMENT '失败异常类型，不保存敏感消息',
  `delivered_time` datetime DEFAULT NULL COMMENT '投递完成时间',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_dept` bigint DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_business_event_outbox_event` (`tenant_id`,`event_id`),
  UNIQUE KEY `uk_ai_business_event_outbox_sequence` (`tenant_id`,`aggregate_key`,`aggregate_sequence`),
  KEY `idx_ai_business_event_outbox_recovery` (`delivery_status`,`next_retry_time`,`lock_time`,`create_time`),
  KEY `idx_ai_business_event_outbox_aggregate` (`tenant_id`,`aggregate_key`,`aggregate_sequence`,`delivery_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='低代码业务事件事务Outbox';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_business_event_sequence`
--

DROP TABLE IF EXISTS `ai_business_event_sequence`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_business_event_sequence` (
  `aggregate_key` char(64) NOT NULL COMMENT '租户、对象与记录组成的聚合摘要',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `object_code` varchar(128) NOT NULL COMMENT '业务对象编码',
  `record_id` varchar(128) NOT NULL COMMENT '业务记录ID',
  `last_sequence` bigint NOT NULL DEFAULT '0' COMMENT '聚合内最后分配序号',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`aggregate_key`),
  UNIQUE KEY `uk_ai_business_event_sequence_tenant_aggregate` (`tenant_id`,`aggregate_key`),
  KEY `idx_ai_business_event_sequence_object` (`tenant_id`,`object_code`,`record_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='低代码业务事件聚合序号';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_business_extension`
--

DROP TABLE IF EXISTS `ai_business_extension`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_business_extension` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `application_id` bigint NOT NULL COMMENT '所属业务应用ID',
  `object_id` bigint DEFAULT NULL COMMENT '可选业务对象ID',
  `entry_id` bigint DEFAULT NULL COMMENT '可选访问入口ID',
  `extension_code` varchar(64) NOT NULL COMMENT '扩展稳定编码',
  `extension_name` varchar(128) NOT NULL COMMENT '扩展名称',
  `extension_type` varchar(32) NOT NULL COMMENT 'VISUAL_RULE/CLIENT_JS/SCOPED_CSS/SERVER_BINDING',
  `hook_code` varchar(32) NOT NULL COMMENT '标准扩展钩子编码',
  `scope_type` varchar(32) NOT NULL DEFAULT 'APPLICATION' COMMENT 'APPLICATION/OBJECT/ENTRY/PAGE/COMPONENT',
  `scope_key` varchar(255) DEFAULT NULL COMMENT '页面或组件作用域键',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '同钩子执行顺序',
  `failure_policy` varchar(16) NOT NULL DEFAULT 'BLOCK' COMMENT 'BLOCK/WARN/IGNORE',
  `risk_level` varchar(16) NOT NULL DEFAULT 'MEDIUM' COMMENT 'LOW/MEDIUM/HIGH',
  `status` varchar(16) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/TESTED/ENABLED/DISABLED',
  `draft_version` int NOT NULL DEFAULT '1' COMMENT '当前草稿版本',
  `enabled_version` int DEFAULT NULL COMMENT '当前运行版本',
  `lock_user_id` bigint DEFAULT NULL COMMENT '编辑锁持有人',
  `lock_username` varchar(128) DEFAULT NULL COMMENT '编辑锁持有人名称',
  `lock_token_hash` varchar(64) DEFAULT NULL COMMENT '编辑锁令牌SHA-256摘要',
  `lock_time` datetime DEFAULT NULL COMMENT '编辑锁获得或续期时间',
  `lock_expire_time` datetime DEFAULT NULL COMMENT '编辑锁过期时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_business_extension_code_active` (`tenant_id`,`application_id`,`extension_code`,`del_flag`),
  KEY `idx_ai_business_extension_hook` (`tenant_id`,`application_id`,`hook_code`,`status`,`sort_order`,`del_flag`),
  KEY `idx_ai_business_extension_object` (`tenant_id`,`object_id`,`status`,`del_flag`),
  KEY `idx_ai_business_extension_entry` (`tenant_id`,`entry_id`,`status`,`del_flag`),
  KEY `idx_ai_business_extension_lock` (`tenant_id`,`lock_user_id`,`lock_expire_time`,`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='低代码受治理业务扩展';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_business_extension_execution_log`
--

DROP TABLE IF EXISTS `ai_business_extension_execution_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_business_extension_execution_log` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `extension_id` bigint NOT NULL COMMENT '业务扩展ID',
  `extension_code` varchar(64) NOT NULL COMMENT '扩展编码快照',
  `version_no` int NOT NULL COMMENT '执行版本号',
  `application_id` bigint NOT NULL COMMENT '业务应用ID',
  `object_id` bigint DEFAULT NULL COMMENT '业务对象ID',
  `entry_id` bigint DEFAULT NULL COMMENT '访问入口ID',
  `hook_code` varchar(32) NOT NULL COMMENT '执行钩子',
  `result_status` varchar(16) NOT NULL COMMENT 'SUCCESS/FAILED',
  `duration_ms` bigint NOT NULL DEFAULT '0' COMMENT '执行耗时毫秒',
  `error_code` varchar(64) DEFAULT NULL COMMENT '脱敏错误编码',
  `error_summary` varchar(500) DEFAULT NULL COMMENT '脱敏错误摘要',
  `actor_user_id` bigint DEFAULT NULL COMMENT '可信操作者ID',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志：0正常 1删除',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_ai_business_extension_audit_extension` (`tenant_id`,`extension_id`,`create_time`,`del_flag`),
  KEY `idx_ai_business_extension_audit_app` (`tenant_id`,`application_id`,`hook_code`,`create_time`,`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='低代码业务扩展脱敏执行审计';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_business_field_template`
--

DROP TABLE IF EXISTS `ai_business_field_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_business_field_template` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `template_code` varchar(64) NOT NULL COMMENT '字段模板编码',
  `template_name` varchar(128) NOT NULL COMMENT '字段模板名称',
  `suite_code` varchar(48) NOT NULL DEFAULT 'COMMON' COMMENT '适用业务套件编码，COMMON为通用',
  `field_type` varchar(32) NOT NULL COMMENT '业务字段类型',
  `field_schema` json NOT NULL COMMENT '字段Schema',
  `status` int NOT NULL DEFAULT '1' COMMENT '状态：1启用 0禁用',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `description` varchar(500) DEFAULT NULL COMMENT '模板说明',
  `options` json DEFAULT NULL COMMENT '扩展配置',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_business_field_template_code` (`tenant_id`,`template_code`),
  KEY `idx_ai_business_field_template_code` (`tenant_id`,`template_code`),
  KEY `idx_ai_business_field_template_suite` (`tenant_id`,`suite_code`,`status`,`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI业务应用平台-业务字段模板表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_business_flow_callback_inbox`
--

DROP TABLE IF EXISTS `ai_business_flow_callback_inbox`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_business_flow_callback_inbox` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `event_id` varchar(128) NOT NULL COMMENT 'Flow通知Outbox稳定事件ID',
  `event_version` int NOT NULL COMMENT '事件协议版本',
  `event_sequence` bigint NOT NULL COMMENT 'Flow通知Outbox顺序号',
  `aggregate_key` char(64) NOT NULL COMMENT '流程实例或业务Key聚合摘要',
  `event_type` varchar(64) NOT NULL COMMENT '流程事件类型',
  `process_instance_id` varchar(128) DEFAULT NULL COMMENT '流程实例ID',
  `business_key` varchar(256) DEFAULT NULL COMMENT '远端流程业务Key',
  `task_id` varchar(128) DEFAULT NULL COMMENT '任务ID',
  `event_digest` char(64) NOT NULL COMMENT '规范事件载荷SHA-256摘要',
  `event_payload` json NOT NULL COMMENT '不可变事件快照',
  `consume_status` varchar(32) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PROCESSING/FAILED/COMPLETED/DEAD',
  `retry_count` int NOT NULL DEFAULT '0' COMMENT '消费认领次数',
  `next_retry_time` datetime DEFAULT NULL COMMENT '下次允许恢复时间',
  `lock_owner` varchar(128) DEFAULT NULL COMMENT '消费租约持有者',
  `lock_time` datetime DEFAULT NULL COMMENT '消费租约时间',
  `error_type` varchar(128) DEFAULT NULL COMMENT '最近失败类型，不保存异常消息',
  `completed_time` datetime DEFAULT NULL COMMENT '本地事务消费完成时间',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_dept` bigint DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_business_flow_callback_event` (`tenant_id`,`event_id`),
  KEY `idx_ai_business_flow_callback_recovery` (`consume_status`,`next_retry_time`,`lock_time`,`create_time`),
  KEY `idx_ai_business_flow_callback_order` (`tenant_id`,`aggregate_key`,`event_sequence`,`consume_status`),
  KEY `idx_ai_business_flow_callback_process` (`tenant_id`,`process_instance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='低代码流程回调可靠Inbox';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_business_flow_instance_link`
--

DROP TABLE IF EXISTS `ai_business_flow_instance_link`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_business_flow_instance_link` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `object_code` varchar(48) NOT NULL COMMENT '业务对象编码',
  `record_id` bigint NOT NULL COMMENT '业务记录ID',
  `business_key` varchar(160) NOT NULL COMMENT '流程业务Key，格式 objectCode:recordId',
  `flow_model_key` varchar(128) NOT NULL COMMENT '流程模型Key',
  `process_instance_id` varchar(128) NOT NULL COMMENT '流程实例ID',
  `flow_status` varchar(32) NOT NULL DEFAULT 'STARTED' COMMENT '流程状态：STARTED/RUNNING/APPROVED/REJECTED/CANCELED/ENDED',
  `start_user_id` bigint DEFAULT NULL COMMENT '流程发起人ID',
  `start_time` datetime DEFAULT NULL COMMENT '流程发起时间',
  `end_time` datetime DEFAULT NULL COMMENT '流程结束时间',
  `result` varchar(32) DEFAULT NULL COMMENT '流程结果',
  `round_no` int NOT NULL DEFAULT '1' COMMENT '提交轮次，同一 business_key 第几次发起',
  `variables_snapshot` json DEFAULT NULL COMMENT '流程变量快照',
  `status_sync_status` varchar(32) NOT NULL DEFAULT 'PENDING' COMMENT '主动状态对账状态',
  `status_sync_retry_count` int NOT NULL DEFAULT '0' COMMENT '主动状态对账连续尝试次数',
  `status_sync_next_time` datetime DEFAULT NULL COMMENT '下次允许主动对账时间',
  `status_sync_lock_owner` varchar(128) DEFAULT NULL COMMENT '主动状态对账租约持有者',
  `status_sync_lock_time` datetime DEFAULT NULL COMMENT '主动状态对账租约时间',
  `status_sync_remote_status` varchar(32) DEFAULT NULL COMMENT '最近读取的Flow业务状态',
  `status_sync_error_type` varchar(128) DEFAULT NULL COMMENT '最近主动对账失败类型',
  `status_synced_time` datetime DEFAULT NULL COMMENT '最近成功主动对账时间',
  `status_sync_replay_count` int NOT NULL DEFAULT '0' COMMENT '状态对账人工重放次数',
  `status_sync_replayed_by` bigint DEFAULT NULL COMMENT '最近状态对账人工重放操作人',
  `status_sync_replayed_time` datetime DEFAULT NULL COMMENT '最近状态对账人工重放时间',
  `status_sync_replay_reason` varchar(500) DEFAULT NULL COMMENT '最近状态对账人工重放原因',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_business_flow_process` (`tenant_id`,`process_instance_id`),
  UNIQUE KEY `uk_ai_business_flow_business_process` (`tenant_id`,`business_key`,`process_instance_id`),
  KEY `idx_ai_business_flow_business_key` (`tenant_id`,`business_key`,`flow_status`),
  KEY `idx_ai_business_flow_record` (`tenant_id`,`object_code`,`record_id`),
  KEY `idx_ai_business_flow_business_latest` (`tenant_id`,`business_key`,`start_time`,`create_time`,`id`),
  KEY `idx_ai_business_flow_status_sync` (`status_sync_status`,`status_sync_next_time`,`status_sync_lock_time`,`flow_status`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI业务应用平台-单据流程实例关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_business_flow_remote_command`
--

DROP TABLE IF EXISTS `ai_business_flow_remote_command`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_business_flow_remote_command` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `command_key` char(64) NOT NULL COMMENT '租户、命令类型与远端业务Key的稳定摘要',
  `command_type` varchar(32) NOT NULL COMMENT '命令类型，当前支持 START',
  `request_digest` char(64) NOT NULL COMMENT '规范请求载荷SHA-256摘要',
  `request_payload` json NOT NULL COMMENT '恢复所需的不可变请求快照',
  `object_code` varchar(128) NOT NULL COMMENT '业务对象编码',
  `record_id` bigint NOT NULL COMMENT '业务记录ID',
  `business_key` varchar(160) NOT NULL COMMENT '本地业务Key',
  `flow_business_key` varchar(256) NOT NULL COMMENT '提交给流程服务的幂等业务Key',
  `flow_model_key` varchar(128) NOT NULL COMMENT '流程模型Key',
  `process_instance_id` varchar(128) DEFAULT NULL COMMENT '远端成功后返回或查回的流程实例ID',
  `command_status` varchar(32) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PROCESSING/RETRY/REMOTE_SUCCEEDED/COMPLETED/DEAD',
  `retry_count` int NOT NULL DEFAULT '0' COMMENT '认领及恢复尝试次数',
  `next_retry_time` datetime DEFAULT NULL COMMENT '下次允许自动恢复时间',
  `lock_owner` varchar(128) DEFAULT NULL COMMENT '恢复租约持有者',
  `lock_time` datetime DEFAULT NULL COMMENT '恢复租约时间',
  `error_type` varchar(128) DEFAULT NULL COMMENT '最近失败类型，不保存异常消息和敏感数据',
  `remote_succeeded_time` datetime DEFAULT NULL COMMENT '确认远端成功时间',
  `completed_time` datetime DEFAULT NULL COMMENT '本地关联与状态提交完成时间',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_dept` bigint DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_business_flow_remote_command` (`tenant_id`,`command_key`),
  KEY `idx_ai_business_flow_remote_recovery` (`command_status`,`next_retry_time`,`lock_time`,`create_time`),
  KEY `idx_ai_business_flow_remote_business` (`tenant_id`,`business_key`,`command_type`,`create_time`),
  KEY `idx_ai_business_flow_remote_process` (`tenant_id`,`process_instance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='低代码流程远程命令恢复日志';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_business_message_channel`
--

DROP TABLE IF EXISTS `ai_business_message_channel`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_business_message_channel` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `channel_code` varchar(64) NOT NULL COMMENT '通道编码',
  `channel_name` varchar(128) NOT NULL COMMENT '通道名称',
  `channel_type` varchar(32) NOT NULL COMMENT '通道类型：WECHAT_WORK/FEISHU/DINGTALK/WEBHOOK/INTERNAL',
  `channel_config_ref` varchar(256) DEFAULT NULL COMMENT '通道配置安全引用（不存明文密钥）',
  `webhook_url` varchar(512) DEFAULT NULL COMMENT 'Webhook地址（仅WEBHOOK类型）',
  `description` varchar(500) DEFAULT NULL COMMENT '通道说明',
  `status` int NOT NULL DEFAULT '1' COMMENT '状态：1启用 0禁用',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `options` json DEFAULT NULL COMMENT '扩展配置',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_message_channel_code` (`tenant_id`,`channel_code`),
  KEY `idx_ai_message_channel_type` (`tenant_id`,`channel_type`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI业务应用平台-消息推送通道表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_business_object`
--

DROP TABLE IF EXISTS `ai_business_object`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_business_object` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `suite_code` varchar(48) NOT NULL COMMENT '所属业务套件编码',
  `object_code` varchar(48) NOT NULL COMMENT '业务对象编码',
  `object_name` varchar(128) NOT NULL COMMENT '业务对象名称',
  `object_type` varchar(32) NOT NULL COMMENT '对象类型：MASTER/DETAIL/LOOKUP/TRANSACTION',
  `model_id` bigint DEFAULT NULL COMMENT '关联低代码模型ID',
  `model_code` varchar(48) DEFAULT NULL COMMENT '关联低代码模型编码',
  `display_field` varchar(64) DEFAULT NULL COMMENT '默认展示字段',
  `icon` varchar(128) DEFAULT NULL COMMENT '对象图标',
  `description` varchar(500) DEFAULT NULL COMMENT '业务说明',
  `status` int NOT NULL DEFAULT '1' COMMENT '状态：1启用 0禁用',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `options` json DEFAULT NULL COMMENT '扩展配置',
  `design_status` varchar(32) NOT NULL DEFAULT 'DRAFT' COMMENT '设计状态：DRAFT草稿 DESIGNING设计中 READY待发布 PUBLISHED已发布 CHANGED有未发布变更',
  `config_key` varchar(128) DEFAULT NULL COMMENT '关联低代码运行配置键',
  `last_publish_time` datetime DEFAULT NULL COMMENT '最近发布时间',
  `last_publish_version` int DEFAULT NULL COMMENT '最近发布版本号',
  `designer_options` json DEFAULT NULL COMMENT '业务对象设计器配置',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_business_object_code_tenant_active` (`tenant_id`,`object_code`,`del_flag`),
  KEY `idx_ai_business_object_suite` (`tenant_id`,`suite_code`,`status`,`sort_order`),
  KEY `idx_ai_business_object_model` (`tenant_id`,`model_id`),
  KEY `idx_ai_business_object_update` (`tenant_id`,`update_time`),
  KEY `idx_ai_business_object_config_key` (`tenant_id`,`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI业务应用平台-业务对象表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_business_object_relation`
--

DROP TABLE IF EXISTS `ai_business_object_relation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_business_object_relation` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `suite_code` varchar(48) NOT NULL COMMENT '所属业务套件编码',
  `source_object_code` varchar(48) NOT NULL COMMENT '来源业务对象编码',
  `target_object_code` varchar(48) NOT NULL COMMENT '目标业务对象编码',
  `relation_type` varchar(32) NOT NULL COMMENT '关系类型：REFERENCE/DETAIL/CHILD_LIST/MANY_TO_MANY',
  `relation_name` varchar(128) NOT NULL COMMENT '关系名称',
  `source_field_code` varchar(64) DEFAULT NULL COMMENT '来源对象字段编码',
  `target_field_code` varchar(64) DEFAULT NULL COMMENT '目标对象字段编码',
  `relation_config` json DEFAULT NULL COMMENT '关系配置',
  `description` varchar(500) DEFAULT NULL COMMENT '关系说明',
  `status` int NOT NULL DEFAULT '1' COMMENT '状态：1启用 0禁用',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_business_relation_scope` (`tenant_id`,`suite_code`,`source_object_code`,`target_object_code`,`relation_type`,`relation_name`),
  KEY `idx_ai_business_relation_source` (`tenant_id`,`suite_code`,`source_object_code`,`status`),
  KEY `idx_ai_business_relation_target` (`tenant_id`,`suite_code`,`target_object_code`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI业务应用平台-业务对象关系表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_business_process`
--

DROP TABLE IF EXISTS `ai_business_process`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_business_process` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `application_id` bigint NOT NULL COMMENT '所属业务应用ID',
  `process_code` varchar(128) NOT NULL COMMENT '应用内稳定流程编码，创建后不可修改',
  `process_name` varchar(128) NOT NULL COMMENT '业务流程名称',
  `process_description` varchar(500) DEFAULT NULL COMMENT '业务流程说明',
  `subject_object_id` bigint NOT NULL COMMENT '主业务对象ID',
  `subject_object_code` varchar(128) NOT NULL COMMENT '主业务对象编码',
  `draft_schema_json` longtext NOT NULL COMMENT 'businessProcessJson草稿，不保存敏感配置',
  `draft_schema_hash` char(64) NOT NULL COMMENT '规范化草稿SHA-256摘要',
  `design_status` varchar(32) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/VALIDATED/PUBLISHED/CHANGED',
  `current_version` int NOT NULL DEFAULT '0' COMMENT '当前已生成流程版本号',
  `published_version` int DEFAULT NULL COMMENT '当前应用快照引用的流程版本号',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1启用 0停用',
  `legacy_source_type` varchar(32) DEFAULT NULL COMMENT '旧来源类型：TRIGGER/FLOW_BINDING/AUTOMATION_ACTION',
  `legacy_source_id` varchar(128) DEFAULT NULL COMMENT '旧来源稳定ID',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_business_process_code_active` (`tenant_id`,`application_id`,`process_code`,`del_flag`),
  UNIQUE KEY `uk_ai_business_process_legacy_active` (`tenant_id`,`legacy_source_type`,`legacy_source_id`,`del_flag`),
  KEY `idx_ai_business_process_application` (`tenant_id`,`application_id`,`status`,`design_status`,`update_time`,`del_flag`),
  KEY `idx_ai_business_process_subject` (`tenant_id`,`subject_object_id`,`status`,`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='应用级业务流程定义与设计草稿';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_business_process_node_run`
--

DROP TABLE IF EXISTS `ai_business_process_node_run`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_business_process_node_run` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `run_id` bigint NOT NULL COMMENT '业务流程运行实例ID',
  `node_id` varchar(128) NOT NULL COMMENT '版本协议中的节点ID',
  `node_type` varchar(32) NOT NULL COMMENT '节点类型',
  `attempt_no` int NOT NULL COMMENT '节点尝试序号，从1开始',
  `status` varchar(24) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/RUNNING/WAITING/SUCCESS/FAILED/CANCELED',
  `idempotency_key` varchar(256) NOT NULL COMMENT '稳定副作用幂等键，重试保持不变',
  `correlation_id` varchar(256) DEFAULT NULL COMMENT 'Flowable实例、子流程run或外部调用关联ID',
  `input_summary` varchar(2000) DEFAULT NULL COMMENT '脱敏输入摘要，不保存完整业务报文',
  `output_summary` varchar(2000) DEFAULT NULL COMMENT '脱敏结果摘要，不保存完整业务报文',
  `error_code` varchar(64) DEFAULT NULL COMMENT '安全错误码',
  `error_summary` varchar(1000) DEFAULT NULL COMMENT '脱敏且截断的错误摘要',
  `next_retry_time` datetime DEFAULT NULL COMMENT '下一次自动重试时间',
  `start_time` datetime DEFAULT NULL COMMENT '开始执行时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_business_process_node_attempt` (`tenant_id`,`run_id`,`node_id`,`attempt_no`),
  KEY `idx_ai_business_process_node_timeline` (`tenant_id`,`run_id`,`create_time`),
  KEY `idx_ai_business_process_node_idempotency` (`tenant_id`,`idempotency_key`),
  KEY `idx_ai_business_process_node_correlation` (`tenant_id`,`correlation_id`,`status`),
  KEY `idx_ai_business_process_node_retry` (`tenant_id`,`status`,`next_retry_time`,`update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='应用级业务流程节点运行与重试时间线';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_business_process_run`
--

DROP TABLE IF EXISTS `ai_business_process_run`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_business_process_run` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `application_id` bigint NOT NULL COMMENT '业务应用ID',
  `process_id` bigint NOT NULL COMMENT '业务流程定义ID',
  `process_version_id` bigint NOT NULL COMMENT '固定业务流程版本ID',
  `process_code` varchar(128) NOT NULL COMMENT '业务流程编码',
  `subject_object_code` varchar(128) NOT NULL COMMENT '主业务对象编码',
  `subject_record_id` varchar(128) NOT NULL COMMENT '业务记录ID字符串，禁止损失长整型精度',
  `business_key` varchar(256) NOT NULL COMMENT '业务Key：objectCode:recordId',
  `trigger_type` varchar(32) NOT NULL COMMENT 'MANUAL/EVENT/SCHEDULE/PROCESS_CALLBACK/EXTERNAL',
  `source_event_id` varchar(128) DEFAULT NULL COMMENT '来源事件稳定ID',
  `idempotency_key` varchar(128) NOT NULL COMMENT '流程版本内稳定幂等键',
  `actor_type` varchar(32) NOT NULL COMMENT 'USER/SERVICE/CALLBACK',
  `actor_user_id` bigint DEFAULT NULL COMMENT '可信执行用户ID',
  `active_org_id` bigint DEFAULT NULL COMMENT '可信当前组织ID',
  `status` varchar(24) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/RUNNING/WAITING/SUCCESS/FAILED/CANCELED',
  `current_node_id` varchar(128) DEFAULT NULL COMMENT '当前检查点节点ID',
  `flow_process_instance_id` varchar(128) DEFAULT NULL COMMENT '当前等待的Flowable流程实例ID',
  `execution_token` bigint NOT NULL DEFAULT '0' COMMENT '单调递增执行栅栏令牌',
  `lease_owner` varchar(128) DEFAULT NULL COMMENT '当前执行租约持有者',
  `lease_expire_time` datetime(3) DEFAULT NULL COMMENT '执行租约过期时间',
  `heartbeat_time` datetime(3) DEFAULT NULL COMMENT '执行器最近心跳时间',
  `context_snapshot` longtext NOT NULL COMMENT '脱敏运行上下文快照，不保存Token或Secret',
  `retry_count` int NOT NULL DEFAULT '0' COMMENT '人工或自动重试次数',
  `next_retry_time` datetime DEFAULT NULL COMMENT '下一次自动重试时间',
  `error_code` varchar(64) DEFAULT NULL COMMENT '安全错误码',
  `error_summary` varchar(1000) DEFAULT NULL COMMENT '脱敏且截断的错误摘要',
  `start_time` datetime DEFAULT NULL COMMENT '开始执行时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_business_process_run_idempotency` (`tenant_id`,`process_version_id`,`idempotency_key`),
  UNIQUE KEY `uk_ai_business_process_run_flow` (`tenant_id`,`flow_process_instance_id`),
  KEY `idx_ai_business_process_run_application` (`tenant_id`,`application_id`,`status`,`create_time`),
  KEY `idx_ai_business_process_run_process` (`tenant_id`,`process_id`,`status`,`update_time`),
  KEY `idx_ai_business_process_run_business` (`tenant_id`,`business_key`,`status`,`update_time`),
  KEY `idx_ai_business_process_run_recovery` (`tenant_id`,`status`,`next_retry_time`,`update_time`),
  KEY `idx_ai_business_process_run_lease` (`tenant_id`,`status`,`lease_expire_time`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='应用级业务流程持久化运行实例';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_business_quantity_balance`
--

DROP TABLE IF EXISTS `ai_business_quantity_balance`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_business_quantity_balance` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `account_code` varchar(128) NOT NULL COMMENT '数量账户编码',
  `item_code` varchar(128) NOT NULL COMMENT '数量项编码',
  `dimension_key` varchar(256) NOT NULL DEFAULT '' COMMENT '数量维度键',
  `quantity` bigint NOT NULL DEFAULT '0' COMMENT '当前数量',
  `locked_quantity` bigint NOT NULL DEFAULT '0' COMMENT '锁定数量',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1启用，0停用',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_business_qty_balance` (`tenant_id`,`account_code`,`item_code`,`dimension_key`),
  KEY `idx_ai_business_qty_balance_item` (`tenant_id`,`item_code`,`dimension_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI业务应用平台-通用数量余额';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_business_quantity_ledger`
--

DROP TABLE IF EXISTS `ai_business_quantity_ledger`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_business_quantity_ledger` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `operation_type` varchar(32) NOT NULL COMMENT '操作类型：INBOUND/LOCK/RELEASE/COMMIT/TRANSFER',
  `account_code` varchar(128) NOT NULL COMMENT '数量账户编码',
  `item_code` varchar(128) NOT NULL COMMENT '数量项编码',
  `dimension_key` varchar(256) NOT NULL DEFAULT '' COMMENT '数量维度键',
  `quantity_delta` bigint NOT NULL DEFAULT '0' COMMENT '数量变动值',
  `balance_quantity` bigint NOT NULL DEFAULT '0' COMMENT '操作后当前数量',
  `locked_quantity` bigint NOT NULL DEFAULT '0' COMMENT '操作后锁定数量',
  `target_account_code` varchar(128) DEFAULT NULL COMMENT '目标数量账户编码',
  `target_item_code` varchar(128) DEFAULT NULL COMMENT '目标数量项编码',
  `target_dimension_key` varchar(256) DEFAULT NULL COMMENT '目标数量维度键',
  `source_object_code` varchar(64) DEFAULT NULL COMMENT '来源业务对象编码',
  `source_record_id` varchar(128) DEFAULT NULL COMMENT '来源业务记录ID',
  `source_detail_id` varchar(128) DEFAULT NULL COMMENT '来源明细ID',
  `lock_id` bigint DEFAULT NULL COMMENT '关联锁定ID',
  `correlation_id` varchar(64) DEFAULT NULL COMMENT '链路ID',
  `idempotency_key` varchar(128) DEFAULT NULL COMMENT '幂等键',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `extra_data` json DEFAULT NULL COMMENT '扩展数据',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_business_qty_ledger_idem` (`tenant_id`,`idempotency_key`),
  KEY `idx_ai_business_qty_ledger_item` (`tenant_id`,`account_code`,`item_code`,`dimension_key`,`create_time`),
  KEY `idx_ai_business_qty_ledger_source` (`tenant_id`,`source_object_code`,`source_record_id`),
  KEY `idx_ai_business_qty_ledger_corr` (`tenant_id`,`correlation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI业务应用平台-通用数量流水';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_business_quantity_lock`
--

DROP TABLE IF EXISTS `ai_business_quantity_lock`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_business_quantity_lock` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `lock_code` varchar(128) DEFAULT NULL COMMENT '锁定编码',
  `account_code` varchar(128) NOT NULL COMMENT '数量账户编码',
  `item_code` varchar(128) NOT NULL COMMENT '数量项编码',
  `dimension_key` varchar(256) NOT NULL DEFAULT '' COMMENT '数量维度键',
  `lock_quantity` bigint NOT NULL DEFAULT '0' COMMENT '锁定数量',
  `released_quantity` bigint NOT NULL DEFAULT '0' COMMENT '已释放数量',
  `committed_quantity` bigint NOT NULL DEFAULT '0' COMMENT '已扣减数量',
  `lock_status` varchar(32) NOT NULL DEFAULT 'LOCKED' COMMENT '锁定状态：LOCKED/RELEASED/COMMITTED',
  `source_object_code` varchar(64) DEFAULT NULL COMMENT '来源业务对象编码',
  `source_record_id` varchar(128) DEFAULT NULL COMMENT '来源业务记录ID',
  `source_detail_id` varchar(128) DEFAULT NULL COMMENT '来源明细ID',
  `correlation_id` varchar(64) DEFAULT NULL COMMENT '链路ID',
  `idempotency_key` varchar(128) DEFAULT NULL COMMENT '幂等键',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_business_qty_lock_idem` (`tenant_id`,`idempotency_key`),
  KEY `idx_ai_business_qty_lock_item` (`tenant_id`,`account_code`,`item_code`,`dimension_key`,`lock_status`),
  KEY `idx_ai_business_qty_lock_source` (`tenant_id`,`source_object_code`,`source_record_id`,`source_detail_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI业务应用平台-通用数量锁定';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_business_suite`
--

DROP TABLE IF EXISTS `ai_business_suite`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_business_suite` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `parent_id` bigint DEFAULT NULL COMMENT '上级业务域ID',
  `suite_code` varchar(48) NOT NULL COMMENT '业务套件编码',
  `suite_name` varchar(128) NOT NULL COMMENT '业务套件名称',
  `icon` varchar(128) DEFAULT NULL COMMENT '套件图标',
  `description` varchar(500) DEFAULT NULL COMMENT '业务说明',
  `status` int NOT NULL DEFAULT '1' COMMENT '状态：1启用 0禁用',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `options` json DEFAULT NULL COMMENT '扩展配置',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_business_suite_code_active` (`tenant_id`,`suite_code`,`del_flag`),
  KEY `idx_ai_business_suite_status` (`tenant_id`,`status`,`sort_order`),
  KEY `idx_ai_business_suite_update` (`tenant_id`,`update_time`),
  KEY `idx_ai_business_suite_parent` (`tenant_id`,`parent_id`,`sort_order`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI业务应用平台-业务套件表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_business_trigger`
--

DROP TABLE IF EXISTS `ai_business_trigger`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_business_trigger` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `suite_code` varchar(48) NOT NULL COMMENT '所属业务套件编码',
  `object_code` varchar(48) NOT NULL COMMENT '所属业务对象编码',
  `trigger_name` varchar(128) NOT NULL COMMENT '触发器名称',
  `trigger_desc` varchar(500) DEFAULT NULL COMMENT '触发器说明',
  `trigger_type` varchar(32) NOT NULL DEFAULT 'EVENT' COMMENT '触发器类型：EVENT事件触发/SCHEDULE定时触发/MANUAL手动触发',
  `scenario_type` varchar(64) DEFAULT NULL COMMENT '业务场景模板类型',
  `blocking_mode` varchar(32) NOT NULL DEFAULT 'ASYNC' COMMENT '阻断模式：ASYNC异步/SYNC_BLOCK同步阻断',
  `developer_mode` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否开发者高级模式',
  `event_type` varchar(64) DEFAULT NULL COMMENT '事件类型：RECORD_CREATED/RECORD_UPDATED/RECORD_DELETED/STATUS_CHANGED/FIELD_CHANGED',
  `event_condition` json DEFAULT NULL COMMENT '事件条件表达式JSON',
  `action_type` varchar(32) NOT NULL COMMENT '动作类型：START_FLOW/SEND_MESSAGE/CREATE_RECORD/UPDATE_FIELD/WEBHOOK',
  `action_config` json NOT NULL COMMENT '动作配置JSON',
  `status` int NOT NULL DEFAULT '1' COMMENT '状态：1启用 0禁用',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `execute_count` bigint NOT NULL DEFAULT '0' COMMENT '累计执行次数',
  `last_execute_time` datetime DEFAULT NULL COMMENT '最近执行时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0正常 1删除）',
  PRIMARY KEY (`id`),
  KEY `idx_ai_business_trigger_object` (`tenant_id`,`suite_code`,`object_code`,`status`),
  KEY `idx_ai_business_trigger_event` (`tenant_id`,`object_code`,`event_type`,`status`),
  KEY `idx_ai_business_trigger_type` (`tenant_id`,`trigger_type`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI业务应用平台-触发器规则表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_business_trigger_log`
--

DROP TABLE IF EXISTS `ai_business_trigger_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_business_trigger_log` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `trigger_id` bigint NOT NULL COMMENT '触发器ID',
  `trigger_name` varchar(128) DEFAULT NULL COMMENT '触发器名称',
  `suite_code` varchar(48) NOT NULL COMMENT '业务套件编码',
  `object_code` varchar(48) NOT NULL COMMENT '业务对象编码',
  `record_id` varchar(64) DEFAULT NULL COMMENT '触发记录ID',
  `event_type` varchar(64) NOT NULL COMMENT '触发事件类型',
  `event_id` varchar(128) NOT NULL COMMENT '稳定业务事件ID',
  `event_source` varchar(32) NOT NULL COMMENT '受信事件来源',
  `event_version` int NOT NULL COMMENT '事件协议版本',
  `event_digest` char(64) NOT NULL COMMENT '事件载荷SHA-256摘要',
  `event_data` json DEFAULT NULL COMMENT '事件数据快照',
  `trigger_snapshot` json DEFAULT NULL COMMENT '触发器配置不可变快照',
  `execution_digest` char(64) DEFAULT NULL COMMENT '事件与触发器快照联合摘要',
  `action_type` varchar(32) NOT NULL COMMENT '执行动作类型',
  `action_result` json DEFAULT NULL COMMENT '执行结果',
  `execute_status` varchar(32) NOT NULL DEFAULT 'PENDING' COMMENT '执行状态：PENDING/SUCCESS/FAILED/SKIPPED',
  `error_message` varchar(2000) DEFAULT NULL COMMENT '错误信息',
  `todo_code` varchar(64) DEFAULT NULL COMMENT 'TODO状态编码',
  `correlation_id` varchar(128) DEFAULT NULL COMMENT '关联流程/消息/外部动作ID',
  `execute_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '执行时间',
  `duration_ms` bigint DEFAULT NULL COMMENT '执行耗时(毫秒)',
  `retry_count` int NOT NULL DEFAULT '0' COMMENT '重试次数',
  `next_retry_time` datetime DEFAULT NULL COMMENT '下次自动恢复时间',
  `lock_owner` varchar(128) DEFAULT NULL COMMENT '执行租约持有者',
  `lock_time` datetime DEFAULT NULL COMMENT '执行租约获取时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_business_trigger_log_event` (`tenant_id`,`trigger_id`,`event_id`),
  KEY `idx_ai_trigger_log_trigger` (`tenant_id`,`trigger_id`,`execute_status`),
  KEY `idx_ai_trigger_log_object` (`tenant_id`,`object_code`,`execute_time`),
  KEY `idx_ai_trigger_log_record` (`tenant_id`,`object_code`,`record_id`),
  KEY `idx_ai_trigger_log_time` (`tenant_id`,`execute_time`),
  KEY `idx_ai_trigger_log_recovery` (`execute_status`,`next_retry_time`,`lock_time`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI业务应用平台-触发器执行日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_capability`
--

DROP TABLE IF EXISTS `ai_capability`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_capability` (
  `id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL DEFAULT '1',
  `capability_code` varchar(128) NOT NULL,
  `protocol_tool_name` varchar(128) NOT NULL,
  `capability_name` varchar(128) NOT NULL,
  `description` varchar(1000) NOT NULL,
  `source_type` varchar(32) NOT NULL,
  `source_key` varchar(128) NOT NULL,
  `source_version` varchar(64) NOT NULL,
  `current_version` varchar(32) NOT NULL,
  `schema_checksum` char(64) NOT NULL,
  `behavior` varchar(24) NOT NULL,
  `risk_level` varchar(16) NOT NULL,
  `required_actor_type` varchar(16) NOT NULL DEFAULT 'SERVICE' COMMENT '要求的调用主体类型：SERVICE/USER/BOTH',
  `visibility` varchar(16) NOT NULL DEFAULT 'PRIVATE',
  `publish_status` varchar(16) NOT NULL DEFAULT 'PUBLISHED',
  `enabled` tinyint NOT NULL DEFAULT '1',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_dept` bigint DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_capability_code_active` (`tenant_id`,`capability_code`,`del_flag`),
  UNIQUE KEY `uk_ai_capability_tool_active` (`tenant_id`,`protocol_tool_name`,`del_flag`),
  KEY `idx_ai_capability_source` (`tenant_id`,`source_type`,`source_key`,`del_flag`),
  KEY `idx_ai_capability_publish` (`tenant_id`,`publish_status`,`enabled`,`del_flag`,`update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI中枢能力目录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_capability_access_token`
--

DROP TABLE IF EXISTS `ai_capability_access_token`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_capability_access_token` (
  `id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL DEFAULT '1',
  `token_key_id` varchar(22) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `token_prefix` varchar(32) NOT NULL,
  `token_hash` char(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `client_id` bigint NOT NULL,
  `credential_version` int NOT NULL,
  `actor_type` varchar(16) NOT NULL,
  `actor_user_id` bigint NOT NULL,
  `service_user_id` bigint DEFAULT NULL COMMENT 'SERVICE/HYBRID 服务账号；纯 USER 委托为空',
  `active_org_id` bigint NOT NULL,
  `audience` varchar(512) NOT NULL,
  `scopes` varchar(2000) NOT NULL,
  `status` varchar(16) NOT NULL DEFAULT 'ACTIVE',
  `issued_at` datetime NOT NULL,
  `expires_at` datetime NOT NULL,
  `last_used_at` datetime DEFAULT NULL,
  `revoked_at` datetime DEFAULT NULL,
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_dept` bigint DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_capability_token_key_active` (`token_key_id`,`del_flag`),
  KEY `idx_ai_capability_token_client` (`tenant_id`,`client_id`,`status`,`expires_at`,`del_flag`),
  KEY `idx_ai_capability_token_actor` (`tenant_id`,`actor_user_id`,`status`,`expires_at`,`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI中枢MCP短期访问令牌安全元数据';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_capability_approval`
--

DROP TABLE IF EXISTS `ai_capability_approval`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_capability_approval` (
  `id` bigint NOT NULL COMMENT '审批请求ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `request_id` varchar(64) NOT NULL COMMENT '原始Capability请求ID',
  `client_id` bigint NOT NULL COMMENT '机器客户端ID',
  `credential_version` int NOT NULL COMMENT '提交时客户端凭据版本',
  `capability_id` bigint NOT NULL COMMENT '能力ID',
  `capability_code` varchar(128) NOT NULL COMMENT '能力编码',
  `capability_version` varchar(32) NOT NULL COMMENT '能力版本',
  `actor_user_id` bigint NOT NULL COMMENT '实际操作用户ID',
  `service_user_id` bigint NOT NULL COMMENT '客户端服务账号ID',
  `active_org_id` bigint NOT NULL COMMENT '当前组织ID',
  `idempotency_key` varchar(128) NOT NULL COMMENT '原始幂等键',
  `request_digest` varchar(71) NOT NULL COMMENT '规范请求SHA-256摘要',
  `business_state_digest` varchar(71) DEFAULT NULL COMMENT '提交时业务状态摘要',
  `key_id` varchar(64) NOT NULL COMMENT 'KEK版本ID',
  `wrapped_dek` varchar(512) NOT NULL COMMENT '包装后的每记录DEK',
  `payload_iv` varchar(64) NOT NULL COMMENT 'AES-GCM IV',
  `payload_ciphertext` longtext NOT NULL COMMENT '请求密文',
  `payload_auth_tag` varchar(64) NOT NULL COMMENT 'AES-GCM认证标签',
  `flow_model_key` varchar(128) NOT NULL COMMENT '审批流程模型Key',
  `process_instance_id` varchar(128) DEFAULT NULL COMMENT 'Flowable实例ID',
  `execute_status` varchar(32) NOT NULL COMMENT '审批执行状态',
  `result_code` varchar(64) NOT NULL COMMENT '稳定结果码',
  `result_snapshot` varchar(2000) DEFAULT NULL COMMENT '安全结果摘要',
  `error_code` varchar(64) DEFAULT NULL COMMENT '稳定错误码',
  `expires_at` datetime NOT NULL COMMENT '审批过期时间',
  `completed_at` datetime DEFAULT NULL COMMENT '终态时间',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_capability_approval_idempotency` (`tenant_id`,`client_id`,`capability_id`,`idempotency_key`,`del_flag`),
  UNIQUE KEY `uk_capability_approval_request` (`tenant_id`,`request_id`,`del_flag`),
  KEY `idx_capability_approval_actor` (`tenant_id`,`actor_user_id`,`create_time`),
  KEY `idx_capability_approval_process` (`tenant_id`,`process_instance_id`),
  KEY `idx_capability_approval_status` (`tenant_id`,`execute_status`,`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI能力高风险动作人工审批';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_capability_client`
--

DROP TABLE IF EXISTS `ai_capability_client`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_capability_client` (
  `id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL DEFAULT '1',
  `client_code` varchar(64) NOT NULL,
  `client_name` varchar(128) NOT NULL,
  `key_id` varchar(22) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `key_prefix` varchar(32) NOT NULL,
  `key_hash` char(64) NOT NULL,
  `credential_version` int NOT NULL DEFAULT '1',
  `service_user_id` bigint DEFAULT NULL COMMENT 'SERVICE/HYBRID 客户端绑定服务账号；USER_DELEGATION 为空',
  `active_org_id` bigint DEFAULT NULL COMMENT 'SERVICE/HYBRID 客户端固定组织；USER_DELEGATION 为空',
  `auth_modes` varchar(64) NOT NULL DEFAULT 'OAUTH' COMMENT '允许的认证模式，逗号分隔：OAUTH/SIGNATURE',
  `actor_mode` varchar(24) NOT NULL DEFAULT 'HYBRID' COMMENT '客户端主体模式：USER_DELEGATION/SERVICE/HYBRID',
  `signing_key_cipher` varchar(512) DEFAULT NULL COMMENT 'KEK加密的HMAC签名密钥密文（FPC1版本化密文，禁止明文）',
  `signing_key_version` int DEFAULT NULL COMMENT '签名密钥轮换版本，NULL表示未启用签名',
  `user_assertion_enabled` tinyint NOT NULL DEFAULT '0' COMMENT '是否允许客户端签名用户断言：0否/1是',
  `user_assertion_key_id` varchar(64) DEFAULT NULL COMMENT '客户端用户断言 RSA 公钥 kid',
  `user_assertion_public_key` text COMMENT '客户端用户断言 RSA X.509 PEM 公钥；不保存私钥',
  `user_assertion_key_version` int DEFAULT NULL COMMENT '客户端用户断言密钥版本；NULL表示从未启用',
  `user_assertion_mapping_mode` varchar(24) NOT NULL DEFAULT 'PREBOUND' COMMENT '用户断言映射模式：PREBOUND/VERIFIED_PHONE',
  `oauth_enabled` tinyint NOT NULL DEFAULT '0' COMMENT '是否允许OAuth换取短期令牌',
  `oauth_client_type` varchar(16) NOT NULL DEFAULT 'CONFIDENTIAL' COMMENT 'OAuth客户端类型（PUBLIC/CONFIDENTIAL）',
  `status` varchar(16) NOT NULL DEFAULT 'ENABLED',
  `expires_at` datetime DEFAULT NULL,
  `last_used_at` datetime DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_dept` bigint DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_capability_client_code_active` (`tenant_id`,`client_code`,`del_flag`),
  UNIQUE KEY `uk_ai_capability_client_key_active` (`key_id`,`del_flag`),
  KEY `idx_ai_capability_client_status` (`tenant_id`,`status`,`expires_at`,`del_flag`),
  KEY `idx_ai_capability_client_user_org` (`tenant_id`,`service_user_id`,`active_org_id`,`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI中枢机器客户端';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_capability_external_identity`
--

DROP TABLE IF EXISTS `ai_capability_external_identity`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_capability_external_identity` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `provider_code` varchar(64) NOT NULL COMMENT '受信OIDC提供方编码',
  `issuer_hash` char(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'issuer SHA-256',
  `subject_hash` char(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'subject SHA-256',
  `subject_hint` varchar(128) DEFAULT NULL COMMENT '外围用户标识脱敏提示；原文不落库',
  `user_id` bigint NOT NULL COMMENT '自动映射的Forge用户ID',
  `status` varchar(16) NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED',
  `last_authenticated_at` datetime DEFAULT NULL COMMENT '最近认证时间',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cap_external_identity_active` (`tenant_id`,`provider_code`,`issuer_hash`,`subject_hash`,`del_flag`),
  KEY `idx_cap_external_identity_user` (`tenant_id`,`user_id`,`status`,`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI能力受信外部用户身份映射';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_capability_flow_action_log`
--

DROP TABLE IF EXISTS `ai_capability_flow_action_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_capability_flow_action_log` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `request_id` varchar(64) NOT NULL COMMENT 'Capability请求ID',
  `client_id` bigint NOT NULL COMMENT '机器客户端ID',
  `capability_id` bigint NOT NULL COMMENT '能力ID',
  `capability_code` varchar(128) NOT NULL COMMENT '能力编码',
  `capability_version` varchar(32) NOT NULL COMMENT '能力版本',
  `operation` varchar(16) NOT NULL COMMENT '流程操作START/APPROVE/REJECT',
  `object_code` varchar(128) NOT NULL COMMENT '业务对象编码',
  `record_id` varchar(128) NOT NULL COMMENT '业务记录ID字符串',
  `task_ref` varchar(16) DEFAULT NULL COMMENT '任务ID安全摘要',
  `idempotency_key` varchar(128) NOT NULL COMMENT '调用方幂等键',
  `request_digest` varchar(71) NOT NULL COMMENT '规范请求SHA-256摘要',
  `actor_type` varchar(16) NOT NULL COMMENT '实际主体类型',
  `actor_user_id` bigint NOT NULL COMMENT '实际操作用户ID',
  `service_user_id` bigint DEFAULT NULL COMMENT 'SERVICE/HYBRID 服务账号；纯 USER 委托为空',
  `active_org_id` bigint NOT NULL COMMENT '当前组织ID',
  `execute_status` varchar(32) NOT NULL COMMENT 'RUNNING/SUCCESS/FAILED',
  `result_code` varchar(64) NOT NULL COMMENT '稳定结果码',
  `result_snapshot` varchar(2000) DEFAULT NULL COMMENT '安全结果摘要JSON',
  `error_code` varchar(64) DEFAULT NULL COMMENT '稳定错误码',
  `duration_ms` bigint NOT NULL DEFAULT '0' COMMENT '耗时毫秒',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cap_flow_action_idempotency` (`tenant_id`,`client_id`,`capability_id`,`operation`,`idempotency_key`,`del_flag`),
  UNIQUE KEY `uk_cap_flow_action_request` (`tenant_id`,`request_id`,`del_flag`),
  KEY `idx_cap_flow_action_actor` (`tenant_id`,`actor_user_id`,`create_time`),
  KEY `idx_cap_flow_action_record` (`tenant_id`,`object_code`,`record_id`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI能力受控流程动作执行日志';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_capability_form_receipt`
--

DROP TABLE IF EXISTS `ai_capability_form_receipt`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_capability_form_receipt` (
  `id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL DEFAULT '1',
  `client_id` bigint NOT NULL,
  `capability_id` bigint NOT NULL,
  `key_hash` char(64) NOT NULL,
  `request_digest` char(64) NOT NULL,
  `record_id` varchar(128) NOT NULL DEFAULT '',
  `create_by` bigint NOT NULL,
  `create_dept` bigint NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint NOT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cap_form_receipt` (`tenant_id`,`client_id`,`capability_id`,`key_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='开放平台表单填报幂等回执';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_capability_grant`
--

DROP TABLE IF EXISTS `ai_capability_grant`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_capability_grant` (
  `id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL DEFAULT '1',
  `client_id` bigint NOT NULL,
  `capability_id` bigint NOT NULL,
  `version_strategy` varchar(16) NOT NULL DEFAULT 'PINNED',
  `fixed_version` varchar(32) DEFAULT NULL,
  `field_policy` json DEFAULT NULL,
  `status` varchar(16) NOT NULL DEFAULT 'ENABLED',
  `expires_at` datetime DEFAULT NULL,
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_dept` bigint DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_capability_grant_active` (`tenant_id`,`client_id`,`capability_id`,`del_flag`),
  KEY `idx_ai_capability_grant_client` (`tenant_id`,`client_id`,`status`,`expires_at`,`del_flag`),
  KEY `idx_ai_capability_grant_capability` (`tenant_id`,`capability_id`,`status`,`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI中枢客户端能力授权';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_capability_invocation_log`
--

DROP TABLE IF EXISTS `ai_capability_invocation_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_capability_invocation_log` (
  `id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL DEFAULT '1',
  `request_id` varchar(64) NOT NULL,
  `client_id` bigint DEFAULT NULL,
  `client_code` varchar(64) DEFAULT NULL,
  `capability_id` bigint DEFAULT NULL,
  `capability_code` varchar(128) NOT NULL,
  `capability_version` varchar(32) DEFAULT NULL,
  `actor_type` varchar(16) DEFAULT NULL,
  `actor_user_id` bigint DEFAULT NULL,
  `service_user_id` bigint DEFAULT NULL,
  `active_org_id` bigint DEFAULT NULL,
  `result_status` varchar(24) NOT NULL,
  `result_code` varchar(64) NOT NULL,
  `error_code` varchar(64) DEFAULT NULL,
  `failure_stage` varchar(64) DEFAULT NULL COMMENT '安全失败阶段',
  `error_message` varchar(1000) DEFAULT NULL COMMENT '脱敏且截断的调用错误摘要',
  `schema_path` varchar(256) DEFAULT NULL,
  `trace_id` varchar(64) DEFAULT NULL,
  `duration_ms` bigint NOT NULL DEFAULT '0',
  `del_flag` tinyint NOT NULL DEFAULT '0',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_dept` bigint DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_capability_invocation_request` (`tenant_id`,`request_id`),
  KEY `idx_ai_capability_invocation_client` (`tenant_id`,`client_id`,`create_time`,`del_flag`),
  KEY `idx_ai_capability_invocation_actor` (`tenant_id`,`actor_user_id`,`create_time`,`del_flag`),
  KEY `idx_ai_capability_invocation_capability` (`tenant_id`,`capability_id`,`create_time`,`del_flag`),
  KEY `idx_ai_capability_invocation_result` (`tenant_id`,`result_code`,`create_time`,`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI中枢能力安全调用日志';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_capability_oauth_redirect_uri`
--

DROP TABLE IF EXISTS `ai_capability_oauth_redirect_uri`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_capability_oauth_redirect_uri` (
  `id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL DEFAULT '1',
  `client_id` bigint NOT NULL,
  `redirect_uri` varchar(2048) NOT NULL,
  `redirect_uri_hash` char(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `status` varchar(16) NOT NULL DEFAULT 'ENABLED',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_dept` bigint DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_capability_redirect_active` (`tenant_id`,`client_id`,`redirect_uri_hash`,`del_flag`),
  KEY `idx_ai_capability_redirect_client` (`tenant_id`,`client_id`,`status`,`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI中枢OAuth客户端精确回调地址';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_capability_openapi_idempotency`
--

DROP TABLE IF EXISTS `ai_capability_openapi_idempotency`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_capability_openapi_idempotency` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `client_id` bigint NOT NULL COMMENT '机器客户端ID',
  `capability_id` bigint NOT NULL COMMENT '能力ID',
  `idempotency_key_hash` char(64) NOT NULL COMMENT 'Idempotency-Key 的 SHA-256 哈希',
  `request_id` varchar(64) NOT NULL COMMENT '首次调用请求ID',
  `response_snapshot` json DEFAULT NULL COMMENT '首次网关统一响应快照',
  `expires_at` datetime NOT NULL COMMENT '快照过期时间（超期由清理任务物理删除）',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建组织ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_capability_openapi_idem` (`tenant_id`,`client_id`,`capability_id`,`idempotency_key_hash`,`del_flag`),
  KEY `idx_ai_capability_openapi_idem_expire` (`expires_at`,`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='能力开放网关幂等记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_capability_policy`
--

DROP TABLE IF EXISTS `ai_capability_policy`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_capability_policy` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `capability_id` bigint NOT NULL COMMENT '能力ID',
  `capability_version` varchar(32) NOT NULL COMMENT '不可变能力版本',
  `risk_level` varchar(16) NOT NULL COMMENT '风险等级，当前仅HIGH',
  `approval_flow_model_key` varchar(128) NOT NULL COMMENT '专用审批流程模型Key',
  `approval_candidate_group` varchar(64) NOT NULL COMMENT '服务端审批候选组',
  `expire_seconds` int NOT NULL DEFAULT '86400' COMMENT '审批有效秒数',
  `status` varchar(16) NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_capability_policy_version` (`tenant_id`,`capability_id`,`capability_version`,`del_flag`),
  KEY `idx_capability_policy_status` (`tenant_id`,`risk_level`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI能力风险与人工审批策略';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_chat_message_tool_call`
--

DROP TABLE IF EXISTS `ai_chat_message_tool_call`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_chat_message_tool_call` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `record_id` bigint NOT NULL COMMENT '关联的 assistant 消息 id（ai_chat_record.id）',
  `session_id` varchar(64) NOT NULL COMMENT '会话ID',
  `seq` int NOT NULL DEFAULT '0' COMMENT '同一消息内的工具调用顺序',
  `tool_name` varchar(128) NOT NULL COMMENT '工具名称',
  `tool_args_json` longtext COMMENT '工具入参 JSON',
  `tool_result_json` longtext COMMENT '工具结果 JSON 或结果摘要',
  `status` varchar(20) NOT NULL DEFAULT 'pending' COMMENT '状态：pending/running/waiting_confirm/success/error/aborted',
  `error_msg` varchar(1024) DEFAULT NULL COMMENT '错误信息',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '删除标志：0正常，删除后写主键',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_tool_call_record` (`record_id`,`del_flag`),
  KEY `idx_tool_call_session` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI对话工具调用明细表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_chat_record`
--

DROP TABLE IF EXISTS `ai_chat_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_chat_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `agent_code` varchar(50) DEFAULT NULL COMMENT 'Agent编码',
  `session_id` varchar(64) NOT NULL COMMENT '会话ID',
  `role` varchar(20) NOT NULL COMMENT '角色（user/assistant/system）',
  `content` longtext NOT NULL COMMENT '消息内容',
  `reasoning` longtext COMMENT '思考过程（reasoning），与正文分离存储',
  `token_usage` int DEFAULT NULL COMMENT 'Token消耗',
  `usage_json` text COMMENT 'Token 用量明细 JSON（prompt/completion/total 等）',
  `first_token_ms` bigint DEFAULT NULL COMMENT '首 token 耗时（毫秒），从发起到首个正文/思考增量',
  `total_ms` bigint DEFAULT NULL COMMENT '生成总耗时（毫秒），从发起到 assistant 收口',
  `attachment_json` text COMMENT '附件明细 JSON（挂在用户消息行，存 fileId/类型/缩略图等）',
  `status` varchar(20) NOT NULL DEFAULT 'done' COMMENT '消息状态：streaming/waiting_confirm/done/error/aborted',
  `interrupt_id` varchar(64) DEFAULT NULL COMMENT 'HITL 中断标识，waiting_confirm 时写入',
  `error_msg` varchar(1024) DEFAULT NULL COMMENT '失败原因，status=error 时写入',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '删除标志：0正常，删除后写主键',
  PRIMARY KEY (`id`),
  KEY `idx_session_id` (`session_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_session_del` (`session_id`,`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI对话记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_chat_session`
--

DROP TABLE IF EXISTS `ai_chat_session`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_chat_session` (
  `id` varchar(64) NOT NULL COMMENT '会话ID（UUID，由前端或服务端生成）',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `agent_code` varchar(50) DEFAULT NULL COMMENT '关联的 Agent 编码',
  `session_name` varchar(200) DEFAULT NULL COMMENT '会话标题（首条消息截取）',
  `status` char(1) NOT NULL DEFAULT '0' COMMENT '状态（0正常 1已删除）',
  `pinned` tinyint NOT NULL DEFAULT '0' COMMENT '是否置顶：0否 1是',
  `pinned_time` datetime DEFAULT NULL COMMENT '置顶时间，置顶组内按此倒序',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '最后更新时间',
  `metadata` varchar(500) DEFAULT NULL COMMENT 'metadata数据JSON',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`,`status`,`create_time`),
  KEY `idx_session_user_pinned` (`user_id`,`status`,`pinned`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI会话表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_code_rule`
--

DROP TABLE IF EXISTS `ai_code_rule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_code_rule` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `rule_code` varchar(64) NOT NULL COMMENT '规则编码',
  `rule_name` varchar(128) NOT NULL COMMENT '规则名称',
  `scene` varchar(64) NOT NULL DEFAULT 'COMMON' COMMENT '适用场景：COMMON通用/DOCUMENT单据/MATERIAL物料等',
  `category` varchar(64) NOT NULL DEFAULT 'COMMON' COMMENT '编码分类',
  `source_object_id` bigint DEFAULT NULL COMMENT '业务字段来源对象ID',
  `source_object_code` varchar(64) DEFAULT NULL COMMENT '业务字段来源对象编码',
  `template` varchar(256) NOT NULL COMMENT '编码模板',
  `reset_policy` varchar(32) NOT NULL DEFAULT 'AUTO' COMMENT '流水重置策略：AUTO/NONE/YEAR/MONTH/DAY/HOUR/MINUTE/SECOND',
  `seq_length` int NOT NULL DEFAULT '4' COMMENT '默认流水号长度',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态：1启用 0停用',
  `builtin` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否内置规则',
  `in_code_list` tinyint NOT NULL DEFAULT '1' COMMENT '是否可在业务配置中选择',
  `version_no` int NOT NULL DEFAULT '1' COMMENT '乐观锁版本号',
  `legacy_compat_enabled` tinyint NOT NULL DEFAULT '1' COMMENT '是否兼容旧编码计数器水位',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `options` json DEFAULT NULL COMMENT '扩展配置JSON',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_code_rule_code_active` (`tenant_id`,`rule_code`,`del_flag`),
  KEY `idx_ai_code_rule_scene` (`tenant_id`,`scene`,`status`),
  KEY `idx_ai_code_rule_update` (`tenant_id`,`update_time`),
  KEY `idx_ai_code_rule_category` (`tenant_id`,`category`,`status`,`del_flag`),
  KEY `idx_ai_code_rule_source_object` (`tenant_id`,`source_object_code`,`status`,`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI业务应用平台-通用编码规则';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_code_rule_segment`
--

DROP TABLE IF EXISTS `ai_code_rule_segment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_code_rule_segment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '分段ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `rule_id` bigint NOT NULL COMMENT '编码规则ID',
  `segment_key` varchar(32) NOT NULL COMMENT '稳定分段键',
  `segment_order` int NOT NULL COMMENT '分段顺序，从1开始',
  `segment_type` varchar(16) NOT NULL COMMENT 'DATE/FIXED/SEQ/VARIABLE/SYS_VAR',
  `segment_value` varchar(128) DEFAULT NULL COMMENT '格式、固定值或变量名',
  `variable_source` varchar(16) NOT NULL DEFAULT 'CUSTOM' COMMENT 'VARIABLE来源：CUSTOM/LOWCODE',
  `segment_length` int DEFAULT NULL COMMENT '声明长度或最大长度',
  `pad_enabled` tinyint NOT NULL DEFAULT '0' COMMENT '是否补位',
  `pad_char` varchar(4) DEFAULT NULL COMMENT '补位字符',
  `pad_direction` varchar(8) NOT NULL DEFAULT 'LEFT' COMMENT 'LEFT/RIGHT',
  `group_enabled` tinyint NOT NULL DEFAULT '0' COMMENT '是否参与流水分组',
  `include_in_code` tinyint NOT NULL DEFAULT '1' COMMENT '是否输出到最终编码',
  `radix_type` varchar(32) DEFAULT NULL COMMENT 'SEQ进制类型',
  `reset_enabled` tinyint NOT NULL DEFAULT '0' COMMENT '是否按周期切换计数器',
  `reset_policy` varchar(16) NOT NULL DEFAULT 'NONE' COMMENT 'NONE/YEAR/MONTH/DAY/HOUR',
  `start_value` bigint NOT NULL DEFAULT '1' COMMENT '新计数器起始值',
  `exclude_ambiguous` tinyint NOT NULL DEFAULT '0' COMMENT '是否移除I/O/Z等易混淆字符',
  `excluded_characters` varchar(16) DEFAULT NULL COMMENT '具体排除的易混淆字符，逗号分隔：I/O/Z',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_code_rule_segment_key_active` (`tenant_id`,`rule_id`,`segment_key`,`del_flag`),
  KEY `idx_ai_code_rule_segment_order` (`tenant_id`,`rule_id`,`del_flag`,`segment_order`,`id`)
) ENGINE=InnoDB AUTO_INCREMENT=32 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='编码规则结构化分段';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_context_config`
--

DROP TABLE IF EXISTS `ai_context_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_context_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint DEFAULT '1' COMMENT '租户ID',
  `agent_code` varchar(64) NOT NULL COMMENT '关联Agent编码',
  `config_name` varchar(128) NOT NULL COMMENT '配置名称',
  `config_content` text NOT NULL COMMENT '上下文内容',
  `config_type` varchar(32) NOT NULL DEFAULT 'SPEC' COMMENT '类型：SPEC/SAMPLE/RULE',
  `sort` int DEFAULT '0' COMMENT '排序',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0正常 1删除）',
  PRIMARY KEY (`id`),
  KEY `idx_agent_code` (`agent_code`)
) ENGINE=InnoDB AUTO_INCREMENT=2055282589253537798 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI上下文配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_crud_config`
--

DROP TABLE IF EXISTS `ai_crud_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_crud_config` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint DEFAULT '1' COMMENT '租户ID',
  `config_key` varchar(64) NOT NULL COMMENT '配置唯一标识（小写字母+数字+下划线）',
  `table_name` varchar(128) NOT NULL COMMENT '关联的数据库表名',
  `table_comment` varchar(255) DEFAULT '' COMMENT '表描述/功能名称',
  `app_name` varchar(128) DEFAULT NULL COMMENT '低代码应用名称',
  `search_schema` json DEFAULT NULL COMMENT '搜索表单Schema JSON',
  `columns_schema` json DEFAULT NULL COMMENT '表格列Schema JSON',
  `edit_schema` json DEFAULT NULL COMMENT '编辑表单Schema JSON',
  `api_config` json DEFAULT NULL COMMENT 'API配置JSON（method@url格式）',
  `options` json DEFAULT NULL COMMENT '其他AiCrudPage配置（modalType/modalWidth/gridCols等）',
  `mode` varchar(16) NOT NULL DEFAULT 'CONFIG' COMMENT '模式：CONFIG-配置驱动 / CODEGEN-代码生成',
  `build_mode` varchar(16) NOT NULL DEFAULT 'AI' COMMENT '构建模式：AI智能生成 LOWCODE可视化低代码 CODEGEN代码生成',
  `status` char(1) NOT NULL DEFAULT '0' COMMENT '状态（0启用 1停用）',
  `publish_status` varchar(16) NOT NULL DEFAULT 'DRAFT' COMMENT '发布状态：DRAFT草稿 PUBLISHED已发布 STOPPED已停用',
  `menu_name` varchar(128) DEFAULT '' COMMENT '菜单名称',
  `menu_parent_id` bigint DEFAULT NULL COMMENT '菜单父级ID',
  `menu_sort` int DEFAULT '0' COMMENT '菜单排序',
  `menu_resource_id` bigint DEFAULT NULL COMMENT '关联的sys_resource记录ID',
  `dict_config` json DEFAULT NULL COMMENT '字典数据配置',
  `desensitize_config` json DEFAULT NULL COMMENT '字段脱敏配置',
  `encrypt_config` json DEFAULT NULL COMMENT '接口加解密配置',
  `trans_config` json DEFAULT NULL COMMENT '字典翻译配置',
  `layout_type` varchar(64) DEFAULT 'simple-crud' COMMENT '页面模板类型（对应 ai_page_template.template_key）',
  `model_schema` json DEFAULT NULL COMMENT '可视化数据模型协议',
  `page_schema` json DEFAULT NULL COMMENT '可视化页面搭建协议',
  `draft_version` int NOT NULL DEFAULT '0' COMMENT '草稿版本号',
  `published_version` int NOT NULL DEFAULT '0' COMMENT '已发布版本号',
  `publish_time` datetime DEFAULT NULL COMMENT '发布时间',
  `publish_by` bigint DEFAULT NULL COMMENT '发布人',
  `domain_id` bigint DEFAULT NULL COMMENT '业务领域ID',
  `domain_code` varchar(48) DEFAULT NULL COMMENT '业务领域编码',
  `object_code` varchar(48) DEFAULT NULL COMMENT '业务对象编码',
  `object_name` varchar(128) DEFAULT NULL COMMENT '业务对象名称',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `runtime_datasource_id` bigint DEFAULT NULL COMMENT '运行数据源ID',
  `runtime_datasource_code` varchar(64) DEFAULT NULL COMMENT '运行数据源编码',
  `runtime_datasource_snapshot` json DEFAULT NULL COMMENT '运行数据源快照，不含密码',
  `runtime_table_name` varchar(128) DEFAULT NULL COMMENT '运行表名',
  `primary_key_field` varchar(64) NOT NULL DEFAULT 'id' COMMENT '主键字段名',
  `primary_key_column` varchar(64) NOT NULL DEFAULT 'id' COMMENT '主键列名',
  `primary_key_type` varchar(32) NOT NULL DEFAULT 'bigint' COMMENT '主键类型',
  `tenant_strategy` json DEFAULT NULL COMMENT '租户隔离策略',
  `audit_strategy` json DEFAULT NULL COMMENT '审计字段策略',
  `logic_delete_strategy` json DEFAULT NULL COMMENT '逻辑删除策略',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  `mount_target` varchar(16) DEFAULT 'ADMIN' COMMENT '菜单挂载位置：ADMIN-管理端，MOBILE-移动端，BOTH-两端同时',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key_active` (`config_key`,`del_flag`),
  KEY `idx_table_name` (`table_name`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_ai_crud_publish_status` (`tenant_id`,`publish_status`,`update_time`),
  KEY `idx_ai_crud_domain` (`tenant_id`,`domain_id`,`publish_status`,`update_time`),
  KEY `idx_ai_crud_domain_object` (`tenant_id`,`domain_id`,`object_code`),
  KEY `idx_ai_crud_runtime_ds` (`tenant_id`,`runtime_datasource_id`,`publish_status`),
  KEY `idx_ai_crud_lowcode_page` (`tenant_id`,`mode`,`build_mode`,`update_time`,`id`),
  KEY `idx_ai_crud_runtime_object_lookup` (`tenant_id`,`object_code`,`mode`,`build_mode`,`publish_status`,`publish_time`),
  KEY `idx_ai_crud_runtime_config_lookup` (`tenant_id`,`config_key`,`mode`,`build_mode`,`publish_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='CRUD配置驱动表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_crud_export_task`
--

DROP TABLE IF EXISTS `ai_crud_export_task`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_crud_export_task` (
  `id` bigint NOT NULL COMMENT '任务ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `config_key` varchar(64) NOT NULL COMMENT 'CRUD配置键',
  `export_name` varchar(128) DEFAULT NULL COMMENT '导出名称',
  `file_name` varchar(255) DEFAULT NULL COMMENT '导出文件名',
  `file_id` varchar(64) DEFAULT NULL COMMENT '文件中心ID',
  `file_size` bigint DEFAULT NULL COMMENT '文件大小(字节)',
  `status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '任务状态:PENDING/RUNNING/SUCCESS/FAILED',
  `total_count` bigint NOT NULL DEFAULT '0' COMMENT '总数据量',
  `exported_count` bigint NOT NULL DEFAULT '0' COMMENT '已导出数据量',
  `progress` int NOT NULL DEFAULT '0' COMMENT '导出进度百分比',
  `query_params` json DEFAULT NULL COMMENT '导出查询参数快照',
  `error_message` varchar(1000) DEFAULT NULL COMMENT '失败原因',
  `finish_time` datetime DEFAULT NULL COMMENT '完成时间',
  `expire_time` datetime DEFAULT NULL COMMENT '文件过期时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建组织ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_ai_crud_export_task_user` (`tenant_id`,`create_by`,`config_key`,`create_time`),
  KEY `idx_ai_crud_export_task_status` (`tenant_id`,`status`,`expire_time`),
  KEY `idx_ai_crud_export_task_file` (`file_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通用CRUD异步导出任务表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_custom_query_scheme`
--

DROP TABLE IF EXISTS `ai_custom_query_scheme`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_custom_query_scheme` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `config_key` varchar(64) NOT NULL COMMENT 'CRUD配置键',
  `scheme_name` varchar(128) NOT NULL COMMENT '查询方案名称',
  `conditions_json` json DEFAULT NULL COMMENT '查询条件JSON',
  `columns_json` json DEFAULT NULL COMMENT '展示字段JSON',
  `sort_json` json DEFAULT NULL COMMENT '排序配置JSON',
  `display_json` json DEFAULT NULL COMMENT '展示方式配置JSON',
  `is_default` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否默认方案（1是 0否）',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0正常 1删除）',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_config_user` (`tenant_id`,`config_key`,`create_by`),
  KEY `idx_tenant_config_default` (`tenant_id`,`config_key`,`create_by`,`is_default`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI自定义查询方案表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_dashboard_component_lineage`
--

DROP TABLE IF EXISTS `ai_dashboard_component_lineage`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_dashboard_component_lineage` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `record_id` bigint NOT NULL COMMENT '生成记录ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `session_id` varchar(80) DEFAULT NULL COMMENT 'AI会话ID',
  `project_id` bigint DEFAULT NULL COMMENT '大屏项目ID',
  `project_name` varchar(128) DEFAULT NULL COMMENT '大屏项目名称',
  `business_definition_id` bigint DEFAULT NULL COMMENT '业务定义ID',
  `business_name` varchar(128) DEFAULT NULL COMMENT '业务定义名称',
  `component_index` int DEFAULT NULL COMMENT '组件序号',
  `component_key` varchar(100) DEFAULT NULL COMMENT '组件Key',
  `component_title` varchar(200) DEFAULT NULL COMMENT '组件标题',
  `dataset_id` bigint NOT NULL COMMENT '数据集ID',
  `dataset_name` varchar(128) DEFAULT NULL COMMENT '数据集名称',
  `field_names` varchar(2000) DEFAULT NULL COMMENT '使用字段，逗号分隔',
  `binding_status` varchar(32) DEFAULT NULL COMMENT '绑定状态',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_ai_dashboard_lineage_dataset` (`tenant_id`,`dataset_id`,`create_time`),
  KEY `idx_ai_dashboard_lineage_record` (`tenant_id`,`record_id`),
  KEY `idx_ai_dashboard_lineage_project` (`tenant_id`,`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI大屏组件数据资产血缘';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_dashboard_generate_record`
--

DROP TABLE IF EXISTS `ai_dashboard_generate_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_dashboard_generate_record` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `session_id` varchar(80) DEFAULT NULL COMMENT 'AI会话ID',
  `project_id` bigint DEFAULT NULL COMMENT '大屏项目ID',
  `project_name` varchar(128) DEFAULT NULL COMMENT '大屏项目名称',
  `business_definition_id` bigint DEFAULT NULL COMMENT '业务定义ID',
  `business_name` varchar(128) DEFAULT NULL COMMENT '业务定义名称',
  `provider_id` bigint DEFAULT NULL COMMENT 'AI供应商ID',
  `provider_name` varchar(128) DEFAULT NULL COMMENT 'AI供应商名称',
  `model_name` varchar(128) DEFAULT NULL COMMENT '模型名称',
  `style` varchar(20) DEFAULT NULL COMMENT '生成风格',
  `canvas_width` int DEFAULT NULL COMMENT '画布宽度',
  `canvas_height` int DEFAULT NULL COMMENT '画布高度',
  `prompt` text COMMENT '用户提示词',
  `request_json` longtext COMMENT '生成请求JSON',
  `generated_title` varchar(200) DEFAULT NULL COMMENT '生成大屏标题',
  `response_json` longtext COMMENT '生成结果JSON',
  `validation_summary_json` longtext COMMENT '前端校验摘要JSON',
  `status` varchar(32) NOT NULL DEFAULT 'success' COMMENT '状态：success/failed/parse_failed/stopped',
  `component_count` int NOT NULL DEFAULT '0' COMMENT '组件数量',
  `bound_count` int NOT NULL DEFAULT '0' COMMENT '动态绑定数量',
  `static_count` int NOT NULL DEFAULT '0' COMMENT '静态组件数量',
  `static_fallback_count` int NOT NULL DEFAULT '0' COMMENT '静态降级数量',
  `repaired_count` int NOT NULL DEFAULT '0' COMMENT '自动修复数量',
  `elapsed_ms` bigint DEFAULT NULL COMMENT '生成耗时毫秒',
  `error_message` varchar(1000) DEFAULT NULL COMMENT '错误原因',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_ai_dashboard_record_user_time` (`tenant_id`,`user_id`,`create_time`),
  KEY `idx_ai_dashboard_record_business` (`tenant_id`,`business_definition_id`,`create_time`),
  KEY `idx_ai_dashboard_record_project` (`tenant_id`,`project_id`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI大屏生成记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_data_audit_access`
--

DROP TABLE IF EXISTS `ai_data_audit_access`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_data_audit_access` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `event_id` bigint NOT NULL COMMENT '事件ID',
  `field_id` bigint NOT NULL COMMENT '字段明细ID',
  `actor_id` varchar(64) NOT NULL COMMENT '访问者ID',
  `actor_name` varchar(128) DEFAULT NULL COMMENT '访问者名称快照',
  `access_reason` varchar(500) NOT NULL COMMENT '查看原因',
  `access_time` datetime NOT NULL COMMENT '访问时间',
  `access_result` varchar(16) NOT NULL COMMENT 'GRANTED/DENIED',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_ai_data_audit_access_field` (`tenant_id`,`event_id`,`field_id`,`access_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='敏感原值查看留痕';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_data_audit_cursor`
--

DROP TABLE IF EXISTS `ai_data_audit_cursor`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_data_audit_cursor` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `object_id` bigint NOT NULL COMMENT '业务对象ID',
  `record_id` varchar(128) NOT NULL COMMENT '规范化记录主键',
  `revision` bigint NOT NULL DEFAULT '0' COMMENT '当前审计修订号',
  `last_event_id` bigint DEFAULT NULL COMMENT '最近事件ID',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_data_audit_cursor_record` (`tenant_id`,`object_id`,`record_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='数据变更审计聚合游标';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_data_audit_event`
--

DROP TABLE IF EXISTS `ai_data_audit_event`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_data_audit_event` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `object_id` bigint NOT NULL COMMENT '业务对象ID',
  `object_code` varchar(128) NOT NULL COMMENT '对象编码快照',
  `object_name` varchar(128) DEFAULT NULL COMMENT '对象名称快照',
  `record_id` varchar(128) NOT NULL COMMENT '规范化记录主键',
  `record_label` varchar(255) DEFAULT NULL COMMENT '记录显示名快照',
  `revision` bigint NOT NULL COMMENT '审计修订号',
  `event_type` varchar(32) NOT NULL COMMENT 'CREATE/UPDATE/DELETE',
  `occurred_at` datetime NOT NULL COMMENT '事件生成时间',
  `operation_id` varchar(64) NOT NULL COMMENT '本次操作ID',
  `parent_operation_id` varchar(64) DEFAULT NULL COMMENT '父操作ID',
  `correlation_id` varchar(64) DEFAULT NULL COMMENT '关联ID',
  `source_type` varchar(32) NOT NULL COMMENT '来源类型',
  `source_event_id` varchar(64) DEFAULT NULL COMMENT '来源事件ID',
  `source_version` varchar(64) DEFAULT NULL COMMENT '来源版本',
  `source_application_id` bigint DEFAULT NULL COMMENT '来源应用ID',
  `source_page_id` bigint DEFAULT NULL COMMENT '来源页面ID',
  `config_key` varchar(128) DEFAULT NULL COMMENT '运行配置键快照',
  `schema_version` int DEFAULT NULL COMMENT 'Schema 版本快照',
  `policy_version` int NOT NULL COMMENT '策略版本快照',
  `policy_snapshot` longtext COMMENT '策略快照',
  `actor_type` varchar(32) NOT NULL COMMENT 'USER/SERVICE/SYSTEM',
  `actor_id` varchar(64) DEFAULT NULL COMMENT '操作者ID',
  `actor_name` varchar(128) DEFAULT NULL COMMENT '操作者名称快照',
  `client_id` varchar(64) DEFAULT NULL COMMENT '客户端ID',
  `delegated_user_id` varchar(64) DEFAULT NULL COMMENT '委托人ID',
  `change_reason` varchar(500) DEFAULT NULL COMMENT '修改原因',
  `reason_code` varchar(64) DEFAULT NULL COMMENT '系统原因码',
  `flow_instance_id` varchar(64) DEFAULT NULL COMMENT '流程实例ID',
  `task_id` varchar(64) DEFAULT NULL COMMENT '任务ID',
  `action_execution_id` varchar(64) DEFAULT NULL COMMENT '动作执行ID',
  `changed_field_count` int NOT NULL DEFAULT '0' COMMENT '变化字段数',
  `changed_row_count` int NOT NULL DEFAULT '0' COMMENT '变化行数',
  `protection_metadata` varchar(255) DEFAULT NULL COMMENT '加密版本摘要',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_data_audit_event_revision` (`tenant_id`,`object_id`,`record_id`,`revision`),
  KEY `idx_ai_data_audit_event_record_time` (`tenant_id`,`object_id`,`record_id`,`occurred_at`,`id`),
  KEY `idx_ai_data_audit_event_time` (`tenant_id`,`occurred_at`,`id`),
  KEY `idx_ai_data_audit_event_actor` (`tenant_id`,`actor_id`,`occurred_at`),
  KEY `idx_ai_data_audit_event_operation` (`tenant_id`,`operation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='数据变更审计事件';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_data_audit_field`
--

DROP TABLE IF EXISTS `ai_data_audit_field`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_data_audit_field` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `event_id` bigint NOT NULL COMMENT '事件ID',
  `target_object_id` bigint NOT NULL COMMENT '目标对象ID',
  `target_model_id` bigint DEFAULT NULL COMMENT '目标模型ID',
  `target_record_id` varchar(128) NOT NULL COMMENT '目标记录ID',
  `relation_key` varchar(128) NOT NULL DEFAULT '' COMMENT '子表关系键，主表用空串',
  `field_path` varchar(255) NOT NULL COMMENT '字段路径',
  `field_code` varchar(128) NOT NULL COMMENT '字段编码快照',
  `column_name` varchar(128) DEFAULT NULL COMMENT '物理列名快照',
  `field_label` varchar(255) DEFAULT NULL COMMENT '字段名称快照',
  `field_type` varchar(64) DEFAULT NULL COMMENT '字段类型快照',
  `field_metadata` longtext COMMENT '字典/关联等解释快照',
  `change_type` varchar(16) NOT NULL COMMENT 'ADD/UPDATE/REMOVE',
  `source_type` varchar(32) NOT NULL COMMENT '字段直接来源',
  `before_state` varchar(16) NOT NULL COMMENT 'ABSENT/NULL/VALUE/OMITTED',
  `after_state` varchar(16) NOT NULL COMMENT 'ABSENT/NULL/VALUE/OMITTED',
  `before_value` longtext COMMENT '前值 JSON 或密文',
  `after_value` longtext COMMENT '后值 JSON 或密文',
  `before_display` longtext COMMENT '前值显示快照',
  `after_display` longtext COMMENT '后值显示快照',
  `value_protection` varchar(16) NOT NULL DEFAULT 'PLAIN' COMMENT 'PLAIN/ENCRYPTED/CHANGE_ONLY',
  `key_version` varchar(64) DEFAULT NULL COMMENT '密钥版本',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_ai_data_audit_field_event` (`tenant_id`,`event_id`),
  KEY `idx_ai_data_audit_field_target` (`tenant_id`,`target_object_id`,`field_code`,`event_id`),
  KEY `idx_ai_data_audit_field_record` (`tenant_id`,`target_object_id`,`target_record_id`,`event_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='数据变更审计字段明细';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_data_audit_policy`
--

DROP TABLE IF EXISTS `ai_data_audit_policy`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_data_audit_policy` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `object_id` bigint NOT NULL COMMENT '业务对象ID',
  `enabled` tinyint NOT NULL DEFAULT '0' COMMENT '是否启用：1启用 0未启用',
  `reason_required` tinyint NOT NULL DEFAULT '1' COMMENT '人工修改/删除是否必填原因',
  `show_in_detail` tinyint NOT NULL DEFAULT '1' COMMENT '是否在业务详情展示变更记录：1展示 0隐藏',
  `policy_version` int NOT NULL DEFAULT '1' COMMENT '策略版本',
  `enabled_at` datetime DEFAULT NULL COMMENT '首次启用时间',
  `write_barrier` tinyint NOT NULL DEFAULT '0' COMMENT '启用切换写入屏障',
  `coverage_status` varchar(32) NOT NULL DEFAULT 'PENDING' COMMENT '覆盖检查：PENDING/PASSED/BLOCKED',
  `coverage_json` longtext COMMENT '覆盖检查结果快照',
  `policy_snapshot` longtext COMMENT '策略快照 JSON',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_data_audit_policy_object` (`tenant_id`,`object_id`,`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='业务对象数据变更审计策略';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_data_audit_scope`
--

DROP TABLE IF EXISTS `ai_data_audit_scope`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_data_audit_scope` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `object_id` bigint NOT NULL COMMENT '审计对象ID',
  `object_code` varchar(128) DEFAULT NULL COMMENT '对象编码快照',
  `object_name` varchar(128) DEFAULT NULL COMMENT '对象名称快照',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_data_audit_scope_role_object` (`tenant_id`,`role_id`,`object_id`,`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='管理员数据审计对象授权';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_formula_execution_log`
--

DROP TABLE IF EXISTS `ai_formula_execution_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_formula_execution_log` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `trace_id` varchar(64) DEFAULT NULL COMMENT '公式执行链路ID',
  `object_code` varchar(64) NOT NULL COMMENT '业务对象编码',
  `record_id` varchar(64) DEFAULT NULL COMMENT '业务记录ID',
  `field_code` varchar(64) NOT NULL COMMENT '公式字段编码',
  `formula_type` varchar(32) DEFAULT NULL COMMENT '公式类型：CALC/AGGREGATE/CONDITIONAL/LOOKUP',
  `formula_mode` varchar(32) DEFAULT NULL COMMENT '计算模式：STORED/VIRTUAL',
  `expression` text COMMENT '表达式或配置摘要',
  `input_snapshot` json DEFAULT NULL COMMENT '输入快照，脱敏后存储',
  `output_value` text COMMENT '输出值摘要，脱敏后存储',
  `success` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否执行成功：1成功 0失败',
  `error_message` text COMMENT '错误信息',
  `elapsed_ms` bigint DEFAULT NULL COMMENT '执行耗时(毫秒)',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_ai_formula_log_trace` (`tenant_id`,`trace_id`),
  KEY `idx_ai_formula_log_record` (`tenant_id`,`object_code`,`record_id`,`create_time`),
  KEY `idx_ai_formula_log_field` (`tenant_id`,`field_code`,`create_time`),
  KEY `idx_ai_formula_log_success` (`tenant_id`,`success`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI业务应用平台-公式执行日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_formula_function`
--

DROP TABLE IF EXISTS `ai_formula_function`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_formula_function` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `function_code` varchar(64) NOT NULL COMMENT '函数编码',
  `display_name` varchar(128) NOT NULL COMMENT '展示名称',
  `category` varchar(64) NOT NULL COMMENT '函数分类',
  `description` varchar(500) DEFAULT NULL COMMENT '函数说明',
  `source_type` varchar(32) NOT NULL DEFAULT 'BUILTIN' COMMENT '来源类型：BUILTIN/SYSTEM/TENANT/MARKET',
  `argument_schema` json DEFAULT NULL COMMENT '参数Schema',
  `return_type` varchar(32) DEFAULT NULL COMMENT '返回类型',
  `example` text COMMENT '示例',
  `status` varchar(32) NOT NULL DEFAULT 'ENABLED' COMMENT '状态：ENABLED/DISABLED',
  `current_version` varchar(32) DEFAULT NULL COMMENT '当前版本',
  `latest_version` varchar(32) DEFAULT NULL COMMENT '最新版本',
  `builtin` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否内置函数',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序号',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_formula_function_code` (`tenant_id`,`function_code`),
  KEY `idx_ai_formula_function_query` (`tenant_id`,`category`,`source_type`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI业务应用平台-公式函数注册表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_formula_function_install`
--

DROP TABLE IF EXISTS `ai_formula_function_install`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_formula_function_install` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `function_code` varchar(64) NOT NULL COMMENT '函数编码',
  `installed_version` varchar(32) NOT NULL COMMENT '已安装版本',
  `install_status` varchar(32) NOT NULL DEFAULT 'INSTALLED' COMMENT '安装状态：INSTALLED/UNINSTALLED',
  `enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用',
  `source_type` varchar(32) NOT NULL DEFAULT 'BUILTIN' COMMENT '来源类型：BUILTIN/SYSTEM/TENANT/MARKET',
  `installed_by` bigint DEFAULT NULL COMMENT '安装人',
  `installed_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '安装时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_formula_function_install` (`tenant_id`,`function_code`),
  KEY `idx_ai_formula_function_install_status` (`tenant_id`,`install_status`,`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI业务应用平台-公式函数安装表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_image_generate_record`
--

DROP TABLE IF EXISTS `ai_image_generate_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_image_generate_record` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL,
  `user_id` bigint DEFAULT NULL COMMENT '用户ID',
  `provider_id` bigint DEFAULT NULL COMMENT '供应商ID',
  `model_id` bigint DEFAULT NULL COMMENT '模型ID',
  `prompt` longtext COMMENT '提示词',
  `negative_prompt` longtext COMMENT '负面提示词',
  `size` varchar(32) DEFAULT '1024x1024' COMMENT '尺寸',
  `result_file_id` bigint DEFAULT NULL COMMENT '生成图片文件ID',
  `status` varchar(32) DEFAULT 'pending' COMMENT '状态(pending/success/failed)',
  `error_msg` varchar(1000) DEFAULT NULL COMMENT '错误信息',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_dept` bigint DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标志(0正常，删除后写主键)',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_user` (`tenant_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI图片生成记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_knowledge`
--

DROP TABLE IF EXISTS `ai_knowledge`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_knowledge` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户ID',
  `knowledge_name` varchar(100) NOT NULL COMMENT '知识库名称',
  `description` varchar(500) DEFAULT NULL COMMENT '描述',
  `icon` varchar(255) DEFAULT NULL COMMENT '图标',
  `vector_store_instance_id` bigint DEFAULT NULL COMMENT '向量存储实例ID',
  `embedding_model_id` bigint DEFAULT NULL COMMENT 'Embedding模型ID',
  `rerank_model_id` bigint DEFAULT NULL COMMENT 'Rerank模型ID',
  `dimension_of_vector_model` int DEFAULT NULL COMMENT '向量维度(显式覆盖)',
  `chunk_strategy` varchar(32) DEFAULT 'length' COMMENT '分块策略(length/delimiter/regex/smart/qa)',
  `chunk_config_json` longtext COMMENT '分块参数JSON(max_tokens/overlap/delimiters/regex)',
  `search_config_json` longtext COMMENT '检索参数JSON(topK/threshold/fusion/rerank_enable/nearby_count)',
  `dedup_strategy` varchar(32) DEFAULT 'none' COMMENT '去重策略(none/name/content/name_or_content)',
  `dedup_action` varchar(32) DEFAULT 'reject' COMMENT '冲突处理(reject/skip/overwrite)',
  `upload_confirm` char(1) DEFAULT '0' COMMENT '两步上传(0否 1是)',
  `status` char(1) DEFAULT '0' COMMENT '状态(0正常 1停用)',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_dept` bigint DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标志(0正常，删除后写主键)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_knowledge_name_active` (`tenant_id`,`knowledge_name`,`del_flag`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI知识库';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_knowledge_chunk`
--

DROP TABLE IF EXISTS `ai_knowledge_chunk`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_knowledge_chunk` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户ID',
  `knowledge_id` bigint NOT NULL COMMENT '知识库ID',
  `document_id` bigint NOT NULL COMMENT '文档ID',
  `chunk_index` int NOT NULL COMMENT '分块序号',
  `content` longtext NOT NULL COMMENT '分块内容',
  `title` varchar(500) DEFAULT NULL COMMENT '分块标题(可选)',
  `token_count` int DEFAULT '0' COMMENT 'token数',
  `vector_id` varchar(200) DEFAULT NULL COMMENT '向量ID(每分块独立，不跨文档共享)',
  `ref_count` int DEFAULT '1' COMMENT '保留列(各自向量方案恒为1，不参与逻辑)',
  `content_hash` varchar(64) DEFAULT NULL COMMENT '内容哈希',
  `retrieval_count` int DEFAULT '0' COMMENT '被检索次数',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_dept` bigint DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标志(0正常，删除后写主键)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_chunk_active` (`document_id`,`chunk_index`,`del_flag`),
  KEY `idx_document` (`document_id`),
  KEY `idx_knowledge` (`knowledge_id`),
  KEY `idx_hash` (`content_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI知识库分块';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_knowledge_document`
--

DROP TABLE IF EXISTS `ai_knowledge_document`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_knowledge_document` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户ID',
  `knowledge_id` bigint NOT NULL COMMENT '知识库ID',
  `file_id` varchar(64) DEFAULT NULL COMMENT '文件ID(FileManager UUID)',
  `doc_name` varchar(255) NOT NULL COMMENT '文档名称',
  `doc_type` varchar(32) DEFAULT NULL COMMENT '文档类型(pdf/word/excel/markdown/txt/html/url/manual)',
  `source_type` varchar(32) DEFAULT 'upload' COMMENT '来源(upload/url/manual/db)',
  `source_url` varchar(1000) DEFAULT NULL COMMENT 'URL来源',
  `content_hash` varchar(64) DEFAULT NULL COMMENT '内容SHA-256(去重)',
  `chunk_count` int DEFAULT '0' COMMENT '分块数',
  `process_status` varchar(32) DEFAULT 'pending' COMMENT '处理状态(pending/processing/success/failed)',
  `process_error` varchar(1000) DEFAULT NULL COMMENT '处理错误信息',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_dept` bigint DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标志(0正常，删除后写主键)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_knowledge_doc_name_active` (`knowledge_id`,`doc_name`,`del_flag`),
  KEY `idx_knowledge` (`knowledge_id`),
  KEY `idx_hash` (`content_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI知识库文档';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_lowcode_domain`
--

DROP TABLE IF EXISTS `ai_lowcode_domain`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_lowcode_domain` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '父级领域ID，0表示根节点',
  `domain_code` varchar(48) NOT NULL COMMENT '领域编码',
  `domain_name` varchar(128) NOT NULL COMMENT '领域名称',
  `domain_desc` varchar(500) DEFAULT NULL COMMENT '领域说明',
  `icon` varchar(128) DEFAULT NULL COMMENT '领域图标',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `status` varchar(16) NOT NULL DEFAULT 'ENABLED' COMMENT '状态：ENABLED启用 DISABLED停用',
  `menu_parent_id` bigint DEFAULT NULL COMMENT '默认菜单父级ID',
  `table_prefix` varchar(64) NOT NULL DEFAULT 'biz_' COMMENT '默认表名前缀',
  `config_key_prefix` varchar(64) NOT NULL DEFAULT 'biz_' COMMENT '默认配置键前缀',
  `default_app_type` varchar(32) NOT NULL DEFAULT 'SINGLE' COMMENT '默认应用类型',
  `default_layout_type` varchar(64) NOT NULL DEFAULT 'simple-crud' COMMENT '默认页面模板',
  `default_table_mode` varchar(32) NOT NULL DEFAULT 'CREATE' COMMENT '默认建表模式',
  `domain_schema` json DEFAULT NULL COMMENT '领域扩展协议',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_lowcode_domain_code_active` (`tenant_id`,`domain_code`,`del_flag`),
  UNIQUE KEY `uk_ai_lowcode_domain_name_active` (`tenant_id`,`parent_id`,`domain_name`,`del_flag`),
  KEY `idx_ai_lowcode_domain_parent` (`tenant_id`,`parent_id`,`sort`),
  KEY `idx_ai_lowcode_domain_status` (`tenant_id`,`status`,`sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI低代码业务领域表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_lowcode_model`
--

DROP TABLE IF EXISTS `ai_lowcode_model`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_lowcode_model` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `domain_id` bigint NOT NULL COMMENT '业务领域ID',
  `domain_code` varchar(48) NOT NULL COMMENT '业务领域编码',
  `model_code` varchar(48) NOT NULL COMMENT '模型编码',
  `model_name` varchar(128) NOT NULL COMMENT '模型名称',
  `model_desc` varchar(512) DEFAULT NULL COMMENT '模型描述',
  `status` varchar(16) NOT NULL DEFAULT 'ENABLED' COMMENT '状态：ENABLED启用 DISABLED停用',
  `tenant_enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用多租户',
  `master_data` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否主数据模型',
  `model_schema` longtext COMMENT '模型结构协议JSON',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `runtime_datasource_id` bigint DEFAULT NULL COMMENT '模型运行数据源ID',
  `runtime_datasource_code` varchar(64) DEFAULT NULL COMMENT '模型运行数据源编码',
  `runtime_table_name` varchar(128) DEFAULT NULL COMMENT '模型运行表名',
  `table_mode` varchar(16) DEFAULT NULL COMMENT '表模式：CREATE/EXISTING',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_lowcode_model_code_active` (`tenant_id`,`domain_id`,`model_code`,`del_flag`),
  KEY `idx_ai_lowcode_model_domain` (`tenant_id`,`domain_id`,`status`),
  KEY `idx_ai_lowcode_model_master` (`tenant_id`,`master_data`,`status`),
  KEY `idx_lowcode_model_runtime_ds` (`tenant_id`,`runtime_datasource_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI低代码数据模型表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_lowcode_publish_task`
--

DROP TABLE IF EXISTS `ai_lowcode_publish_task`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_lowcode_publish_task` (
  `id` bigint NOT NULL COMMENT '任务ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `request_id` varchar(128) NOT NULL COMMENT '稳定发布请求ID',
  `operation_type` varchar(32) NOT NULL COMMENT 'PUBLISH/ROLLBACK',
  `config_id` bigint NOT NULL COMMENT '低代码配置ID',
  `config_key` varchar(128) NOT NULL COMMENT '低代码配置Key',
  `version_id` bigint NOT NULL COMMENT '发布版本ID',
  `version_no` int NOT NULL COMMENT '发布版本号',
  `schema_hash` char(64) NOT NULL COMMENT '发布Schema SHA-256',
  `runtime_datasource_id` bigint DEFAULT NULL COMMENT '运行数据源ID',
  `runtime_datasource_code` varchar(128) DEFAULT NULL COMMENT '运行数据源编码',
  `runtime_table_name` varchar(128) DEFAULT NULL COMMENT '运行表名',
  `operator_id` bigint NOT NULL COMMENT '发布操作者',
  `command_payload` mediumtext NOT NULL COMMENT '最小不可变后置同步命令',
  `command_digest` char(64) NOT NULL COMMENT '命令SHA-256',
  `current_stage` varchar(32) NOT NULL COMMENT '当前阶段',
  `task_status` varchar(32) NOT NULL COMMENT 'PENDING/PROCESSING/RETRY/COMPLETED/SUPERSEDED/DEAD',
  `retry_count` int NOT NULL DEFAULT '0' COMMENT '认领次数',
  `next_retry_time` datetime DEFAULT NULL COMMENT '下次重试时间',
  `lock_owner` varchar(128) DEFAULT NULL COMMENT '租约持有者',
  `lock_time` datetime DEFAULT NULL COMMENT '租约时间',
  `error_type` varchar(128) DEFAULT NULL COMMENT '失败类型',
  `completed_time` datetime DEFAULT NULL COMMENT '完成或失效时间',
  `replay_count` int NOT NULL DEFAULT '0' COMMENT '人工重放次数',
  `replayed_by` bigint DEFAULT NULL COMMENT '最近人工重放操作人',
  `replayed_time` datetime DEFAULT NULL COMMENT '最近人工重放时间',
  `replay_reason` varchar(500) DEFAULT NULL COMMENT '最近人工重放原因',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_lowcode_publish_task_request` (`tenant_id`,`request_id`),
  KEY `idx_ai_lowcode_publish_task_scan` (`task_status`,`next_retry_time`,`lock_time`,`id`),
  KEY `idx_ai_lowcode_publish_task_config` (`tenant_id`,`config_id`,`version_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='低代码发布可靠任务';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_model`
--

DROP TABLE IF EXISTS `ai_model`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_model` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `provider_id` bigint NOT NULL COMMENT '供应商ID',
  `model_type` varchar(32) NOT NULL COMMENT '模型类型(chat/embedding/image/audio)',
  `model_id` varchar(128) NOT NULL COMMENT '模型标识(如gpt-4o)',
  `model_name` varchar(128) NOT NULL COMMENT '模型显示名称',
  `description` varchar(512) DEFAULT NULL COMMENT '描述',
  `max_tokens` int DEFAULT NULL COMMENT '最大Token数',
  `context_window` int DEFAULT NULL COMMENT '上下文窗口Token数',
  `input_price_per_million_cent` bigint DEFAULT NULL COMMENT '输入价格（分/百万Token）',
  `output_price_per_million_cent` bigint DEFAULT NULL COMMENT '输出价格（分/百万Token）',
  `icon` varchar(512) DEFAULT NULL COMMENT '模型图标(文件ID或URL)',
  `is_default` char(1) DEFAULT '0' COMMENT '是否默认模型(0否1是)',
  `status` char(1) DEFAULT '0' COMMENT '状态(0正常1停用)',
  `sort_order` int DEFAULT '0' COMMENT '排序号',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户编号',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint unsigned DEFAULT NULL COMMENT '创建组织ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(512) DEFAULT NULL COMMENT '备注',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_model_provider_model_active` (`provider_id`,`model_id`,`del_flag`),
  KEY `idx_ai_model_provider_id` (`provider_id`),
  KEY `idx_ai_model_type` (`model_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI模型表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_model_capability`
--

DROP TABLE IF EXISTS `ai_model_capability`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_model_capability` (
  `id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL DEFAULT '1',
  `model_id` bigint NOT NULL,
  `capability_code` varchar(64) NOT NULL,
  `config_json` json DEFAULT NULL,
  `status` char(1) NOT NULL DEFAULT '0',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_dept` bigint DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_model_capability_active` (`tenant_id`,`model_id`,`capability_code`,`del_flag`),
  KEY `idx_ai_model_capability_model` (`tenant_id`,`model_id`,`status`,`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI模型能力关系';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_model_invocation_log`
--

DROP TABLE IF EXISTS `ai_model_invocation_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_model_invocation_log` (
  `id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL,
  `request_id` varchar(64) NOT NULL,
  `user_id` bigint DEFAULT NULL,
  `agent_code` varchar(64) NOT NULL,
  `session_id` varchar(80) DEFAULT NULL,
  `phase` varchar(24) NOT NULL,
  `dispatched` tinyint NOT NULL DEFAULT '0',
  `route_source` varchar(32) DEFAULT NULL,
  `route_reason` varchar(64) DEFAULT NULL,
  `route_policy_id` bigint DEFAULT NULL,
  `provider_id` bigint DEFAULT NULL,
  `model_id` bigint DEFAULT NULL,
  `provider_model_id` varchar(128) DEFAULT NULL,
  `adapter_code` varchar(32) DEFAULT NULL,
  `outcome` varchar(24) NOT NULL,
  `error_category` varchar(32) DEFAULT NULL,
  `http_status` int DEFAULT NULL,
  `error_code` varchar(64) DEFAULT NULL,
  `latency_ms` bigint NOT NULL DEFAULT '0',
  `prompt_tokens` bigint DEFAULT NULL,
  `completion_tokens` bigint DEFAULT NULL,
  `total_tokens` bigint DEFAULT NULL,
  `usage_available` tinyint NOT NULL DEFAULT '0',
  `cost_available` tinyint NOT NULL DEFAULT '0',
  `input_price_per_million_cent` bigint DEFAULT NULL,
  `output_price_per_million_cent` bigint DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_dept` bigint DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_invocation_request` (`tenant_id`,`request_id`),
  KEY `idx_ai_invocation_tenant_time` (`tenant_id`,`create_time`),
  KEY `idx_ai_invocation_model_time` (`tenant_id`,`model_id`,`create_time`),
  KEY `idx_ai_invocation_agent_time` (`tenant_id`,`agent_code`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI模型调用治理日志';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_model_route_policy`
--

DROP TABLE IF EXISTS `ai_model_route_policy`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_model_route_policy` (
  `id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL DEFAULT '1',
  `policy_code` varchar(64) NOT NULL,
  `policy_name` varchar(128) NOT NULL,
  `required_capabilities` json DEFAULT NULL,
  `status` char(1) NOT NULL DEFAULT '0',
  `remark` varchar(500) DEFAULT NULL,
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_dept` bigint DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_route_policy_code_active` (`tenant_id`,`policy_code`,`del_flag`),
  KEY `idx_ai_route_policy_status` (`tenant_id`,`status`,`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI模型路由策略';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_model_route_target`
--

DROP TABLE IF EXISTS `ai_model_route_target`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_model_route_target` (
  `id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL DEFAULT '1',
  `policy_id` bigint NOT NULL,
  `model_id` bigint NOT NULL,
  `priority` int NOT NULL DEFAULT '100',
  `status` char(1) NOT NULL DEFAULT '0',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_dept` bigint DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_route_target_active` (`tenant_id`,`policy_id`,`model_id`,`del_flag`),
  KEY `idx_ai_route_target_order` (`tenant_id`,`policy_id`,`status`,`del_flag`,`priority`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI模型路由显式候选';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_page_template`
--

DROP TABLE IF EXISTS `ai_page_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_page_template` (
  `id` bigint NOT NULL COMMENT '主键',
  `template_key` varchar(64) NOT NULL COMMENT '模板唯一标识（如 simple-crud / tree-crud）',
  `template_name` varchar(128) NOT NULL COMMENT '模板显示名称',
  `description` varchar(512) DEFAULT '' COMMENT '模板描述',
  `icon` varchar(64) DEFAULT '' COMMENT '图标（mdi: 前缀）',
  `system_prompt` text COMMENT '该模板专属的 AI system prompt 补充',
  `schema_hint` json DEFAULT NULL COMMENT '告诉 AI 该模板可用的字段约束（JSON）',
  `default_config` json DEFAULT NULL COMMENT '默认配置值（JSON，如 modalType/searchGridCols 等）',
  `enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用（1启用 0停用）',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `is_builtin` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否内置（1内置不可删除）',
  `codegen_type` varchar(16) NOT NULL DEFAULT 'TEMPLATE' COMMENT '代码生成策略：TEMPLATE-Velocity模板 / AI-大模型生成',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `create_dept` bigint DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_template_key_active` (`template_key`,`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI页面模板表（组件市场）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_prompt_template`
--

DROP TABLE IF EXISTS `ai_prompt_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_prompt_template` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `template_name` varchar(128) NOT NULL COMMENT '模板名称',
  `template_code` varchar(80) DEFAULT NULL COMMENT '模板编码',
  `usage_scene` varchar(64) NOT NULL DEFAULT 'dashboard_generate' COMMENT '使用场景：dashboard_generate大屏生成等',
  `business_category` varchar(64) DEFAULT NULL COMMENT '业务分类',
  `domain_category` varchar(64) DEFAULT NULL COMMENT '领域分类',
  `template_tags` varchar(255) DEFAULT NULL COMMENT '模板标签，逗号分隔',
  `description` varchar(500) DEFAULT NULL COMMENT '模板说明',
  `prompt_content` longtext NOT NULL COMMENT '提示词内容',
  `example_input` text COMMENT '示例输入',
  `status` char(1) NOT NULL DEFAULT '0' COMMENT '状态：0启用 1停用',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  `is_recommended` char(1) NOT NULL DEFAULT '0' COMMENT '是否推荐：0否 1是',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序号',
  `use_count` int NOT NULL DEFAULT '0' COMMENT '使用次数',
  `test_count` int NOT NULL DEFAULT '0' COMMENT '测试次数',
  `download_count` int NOT NULL DEFAULT '0' COMMENT '下载次数',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_prompt_template_code_active` (`tenant_id`,`template_code`,`del_flag`),
  KEY `idx_ai_prompt_template_scene` (`tenant_id`,`usage_scene`,`status`,`sort_order`),
  KEY `idx_ai_prompt_template_category` (`tenant_id`,`business_category`,`domain_category`),
  KEY `idx_ai_prompt_template_recommend` (`tenant_id`,`is_recommended`,`use_count`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI提示词模板库';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_provider`
--

DROP TABLE IF EXISTS `ai_provider`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_provider` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户ID',
  `provider_name` varchar(50) NOT NULL COMMENT '供应商名称（如 阿里百炼、OpenAI）',
  `provider_type` varchar(30) NOT NULL COMMENT '类型（openai/azure/dashscope/ollama）',
  `adapter_code` varchar(32) NOT NULL DEFAULT 'openai_compatible' COMMENT '连接适配器（openai_compatible/dashscope_native）',
  `logo` varchar(512) DEFAULT NULL COMMENT '供应商Logo(文件ID或URL)',
  `api_key` varchar(500) NOT NULL COMMENT 'API Key',
  `base_url` varchar(500) DEFAULT NULL COMMENT 'API Base URL',
  `models` json DEFAULT NULL COMMENT '可用模型列表 [{"name":"qwen-plus","enabled":true}]',
  `is_default` char(1) NOT NULL DEFAULT '0' COMMENT '是否默认供应商（0否 1是）',
  `status` char(1) NOT NULL DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0正常 1删除）',
  `default_model` varchar(100) DEFAULT NULL COMMENT '默认模型名称',
  `create_dept` varchar(30) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI供应商配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_report_data_business_dataset`
--

DROP TABLE IF EXISTS `ai_report_data_business_dataset`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_report_data_business_dataset` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `business_id` bigint NOT NULL COMMENT '业务定义ID',
  `dataset_id` bigint NOT NULL COMMENT '数据集ID',
  `is_primary` tinyint NOT NULL DEFAULT '0' COMMENT '是否主数据集：1是 0否',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `usage_remark` varchar(500) DEFAULT NULL COMMENT '数据集用途说明',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_data_business_dataset` (`tenant_id`,`business_id`,`dataset_id`),
  KEY `idx_data_business_dataset_business` (`tenant_id`,`business_id`),
  KEY `idx_data_business_dataset_dataset` (`tenant_id`,`dataset_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台业务定义数据集绑定';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_report_data_business_definition`
--

DROP TABLE IF EXISTS `ai_report_data_business_definition`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_report_data_business_definition` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `business_code` varchar(64) NOT NULL COMMENT '业务编码',
  `business_name` varchar(100) NOT NULL COMMENT '业务名称',
  `business_desc` text NOT NULL COMMENT '业务定义描述',
  `analysis_goal` text COMMENT '分析目标',
  `metric_definition` text COMMENT '指标口径',
  `dimension_definition` text COMMENT '分析维度',
  `usage_guide` text COMMENT 'AI使用建议',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1启用 0禁用',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_data_business_code_tenant_active` (`tenant_id`,`business_code`,`del_flag`),
  KEY `idx_data_business_status` (`tenant_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台业务定义';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_report_data_connection`
--

DROP TABLE IF EXISTS `ai_report_data_connection`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_report_data_connection` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `connection_code` varchar(64) NOT NULL COMMENT '连接编码',
  `connection_name` varchar(100) NOT NULL COMMENT '连接名称',
  `db_type` varchar(32) NOT NULL COMMENT '数据库类型：MYSQL/POSTGRESQL/ORACLE/SQLSERVER',
  `driver_class_name` varchar(200) NOT NULL COMMENT '驱动类名',
  `jdbc_url` varchar(1000) NOT NULL COMMENT 'JDBC连接地址',
  `username` varchar(128) NOT NULL COMMENT '用户名',
  `password_cipher` varchar(500) DEFAULT NULL COMMENT '密码密文',
  `schema_name` varchar(128) DEFAULT NULL COMMENT '模式名/数据库名',
  `test_sql` varchar(200) NOT NULL DEFAULT 'SELECT 1' COMMENT '测试SQL',
  `pool_config_json` json DEFAULT NULL COMMENT '连接池配置JSON',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1启用 0禁用',
  `description` varchar(500) DEFAULT NULL COMMENT '描述',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_data_connection_code_tenant_active` (`tenant_id`,`connection_code`,`del_flag`),
  KEY `idx_data_connection_status` (`tenant_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台数据连接';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_report_data_dataset`
--

DROP TABLE IF EXISTS `ai_report_data_dataset`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_report_data_dataset` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `dataset_code` varchar(64) NOT NULL COMMENT '数据集编码',
  `dataset_name` varchar(100) NOT NULL COMMENT '数据集名称',
  `connection_id` bigint NOT NULL COMMENT '数据连接ID',
  `category_id` bigint DEFAULT NULL COMMENT '分类ID',
  `dataset_type` varchar(20) NOT NULL COMMENT '数据集类型：TABLE/SQL',
  `table_name` varchar(200) DEFAULT NULL COMMENT '单表模式表名',
  `sql_text` longtext COMMENT 'SQL模式查询SQL',
  `param_schema_json` json DEFAULT NULL COMMENT '参数定义JSON',
  `default_order_json` json DEFAULT NULL COMMENT '默认排序JSON',
  `max_rows` int NOT NULL DEFAULT '1000' COMMENT '最大返回行数',
  `timeout_seconds` int NOT NULL DEFAULT '15' COMMENT '查询超时时间秒',
  `cache_enabled` tinyint NOT NULL DEFAULT '0' COMMENT '是否启用缓存：1是 0否',
  `cache_ttl_seconds` int DEFAULT NULL COMMENT '缓存秒数',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1启用 0禁用',
  `publish_status` tinyint NOT NULL DEFAULT '0' COMMENT '发布状态：0未发布 1已发布 2已下架',
  `access_mode` varchar(20) NOT NULL DEFAULT 'PUBLIC' COMMENT '访问模式：PUBLIC公开 PRIVATE私有',
  `description` varchar(500) DEFAULT NULL COMMENT '描述',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_data_dataset_code_tenant_active` (`tenant_id`,`dataset_code`,`del_flag`),
  KEY `idx_data_dataset_connection` (`tenant_id`,`connection_id`),
  KEY `idx_data_dataset_status` (`tenant_id`,`status`),
  KEY `idx_data_dataset_access_mode` (`tenant_id`,`access_mode`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台数据集';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_report_data_dataset_acl`
--

DROP TABLE IF EXISTS `ai_report_data_dataset_acl`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_report_data_dataset_acl` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `dataset_id` bigint NOT NULL COMMENT '数据集ID',
  `subject_type` varchar(20) NOT NULL COMMENT '授权主体类型：USER/ROLE/ORG',
  `subject_id` bigint NOT NULL COMMENT '授权主体ID',
  `access_level` varchar(20) NOT NULL DEFAULT 'QUERY' COMMENT '访问级别：VIEW/QUERY/MANAGE',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_data_dataset_acl_subject` (`tenant_id`,`dataset_id`,`subject_type`,`subject_id`,`access_level`),
  KEY `idx_data_dataset_acl_dataset` (`tenant_id`,`dataset_id`),
  KEY `idx_data_dataset_acl_subject` (`tenant_id`,`subject_type`,`subject_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台数据集授权';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_report_data_dataset_category`
--

DROP TABLE IF EXISTS `ai_report_data_dataset_category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_report_data_dataset_category` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `parent_id` bigint DEFAULT NULL COMMENT '父分类ID',
  `level` int NOT NULL DEFAULT '1' COMMENT '层级',
  `ancestors` varchar(500) NOT NULL DEFAULT '0/' COMMENT '祖先路径',
  `category_code` varchar(64) NOT NULL COMMENT '分类编码',
  `category_name` varchar(100) NOT NULL COMMENT '分类名称',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1启用 0禁用',
  `description` varchar(500) DEFAULT NULL COMMENT '描述',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_data_dataset_category_code_tenant_active` (`tenant_id`,`category_code`,`del_flag`),
  KEY `idx_data_dataset_category_parent` (`tenant_id`,`parent_id`),
  KEY `idx_data_dataset_category_status` (`tenant_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台数据集分类';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_report_data_dataset_field`
--

DROP TABLE IF EXISTS `ai_report_data_dataset_field`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_report_data_dataset_field` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `dataset_id` bigint NOT NULL COMMENT '数据集ID',
  `field_name` varchar(128) NOT NULL COMMENT '字段名/SQL别名',
  `field_label` varchar(128) DEFAULT NULL COMMENT '字段显示名',
  `source_column` varchar(128) DEFAULT NULL COMMENT '来源列名',
  `db_type` varchar(100) DEFAULT NULL COMMENT '数据库字段类型',
  `data_type` varchar(32) NOT NULL DEFAULT 'STRING' COMMENT '标准类型：STRING/NUMBER/DATE/DATETIME/BOOLEAN',
  `field_role` varchar(20) NOT NULL DEFAULT 'DIMENSION' COMMENT '字段角色：DIMENSION/MEASURE',
  `default_agg` varchar(20) DEFAULT NULL COMMENT '默认聚合：SUM/COUNT/AVG/MAX/MIN',
  `query_enabled` tinyint NOT NULL DEFAULT '1' COMMENT '是否可筛选',
  `display_enabled` tinyint NOT NULL DEFAULT '1' COMMENT '是否可展示',
  `sensitive_level` varchar(20) NOT NULL DEFAULT 'NONE' COMMENT '敏感级别：NONE/MASK/HIDDEN',
  `mask_rule` varchar(100) DEFAULT NULL COMMENT '脱敏规则',
  `dict_type` varchar(100) DEFAULT NULL COMMENT '字典类型',
  `date_format` varchar(64) DEFAULT NULL COMMENT '日期展示格式',
  `data_unit` varchar(32) DEFAULT NULL COMMENT '数据计量单位',
  `dimension_id` bigint DEFAULT NULL COMMENT '绑定维度ID',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `description` varchar(500) DEFAULT NULL COMMENT '描述',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_data_dataset_field` (`tenant_id`,`dataset_id`,`field_name`),
  KEY `idx_data_dataset_field_dataset` (`tenant_id`,`dataset_id`),
  KEY `idx_data_dataset_field_dimension` (`tenant_id`,`dimension_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台数据集字段';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_report_data_dataset_row_scope`
--

DROP TABLE IF EXISTS `ai_report_data_dataset_row_scope`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_report_data_dataset_row_scope` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `dataset_id` bigint NOT NULL COMMENT '数据集ID',
  `enabled` tinyint NOT NULL DEFAULT '0' COMMENT '是否启用：1是 0否',
  `scope_mode` varchar(32) NOT NULL DEFAULT 'SYSTEM_DATA_SCOPE' COMMENT '行权限模式：SYSTEM_DATA_SCOPE跟随系统数据权限',
  `tenant_column` varchar(128) DEFAULT NULL COMMENT '租户字段名/表达式',
  `org_column` varchar(128) DEFAULT NULL COMMENT '组织字段名/表达式',
  `user_column` varchar(128) DEFAULT NULL COMMENT '用户字段名/表达式',
  `region_column` varchar(128) DEFAULT NULL COMMENT '行政区划字段名/表达式',
  `region_strategy` varchar(32) NOT NULL DEFAULT 'SELF_AND_DESCENDANTS' COMMENT '行政区划策略：SELF/SELF_AND_CHILDREN/SELF_AND_DESCENDANTS',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_data_dataset_row_scope` (`tenant_id`,`dataset_id`),
  KEY `idx_data_dataset_row_scope_enabled` (`tenant_id`,`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台数据集行权限配置';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_report_data_dimension`
--

DROP TABLE IF EXISTS `ai_report_data_dimension`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_report_data_dimension` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `dimension_code` varchar(64) NOT NULL COMMENT '维度编码',
  `dimension_name` varchar(100) NOT NULL COMMENT '维度名称',
  `source_type` varchar(20) NOT NULL DEFAULT 'MANUAL' COMMENT '来源类型：MANUAL/SQL',
  `connection_id` bigint DEFAULT NULL COMMENT 'SQL来源数据连接ID',
  `sql_text` longtext COMMENT 'SQL来源查询语句',
  `value_column` varchar(128) DEFAULT NULL COMMENT '值字段列名',
  `label_column` varchar(128) DEFAULT NULL COMMENT '显示字段列名',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1启用 0禁用',
  `last_sync_time` datetime DEFAULT NULL COMMENT '最近同步时间',
  `description` varchar(500) DEFAULT NULL COMMENT '描述',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_data_dimension_code_tenant_active` (`tenant_id`,`dimension_code`,`del_flag`),
  KEY `idx_data_dimension_source` (`tenant_id`,`source_type`),
  KEY `idx_data_dimension_status` (`tenant_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台维度';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_report_data_dimension_item`
--

DROP TABLE IF EXISTS `ai_report_data_dimension_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_report_data_dimension_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `dimension_id` bigint NOT NULL COMMENT '维度ID',
  `item_value` varchar(255) NOT NULL COMMENT '维度值',
  `item_label` varchar(255) NOT NULL COMMENT '维度显示值',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1启用 0禁用',
  `extra_json` json DEFAULT NULL COMMENT '扩展信息JSON',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_data_dimension_item_value` (`tenant_id`,`dimension_id`,`item_value`),
  KEY `idx_data_dimension_item_dimension` (`tenant_id`,`dimension_id`),
  KEY `idx_data_dimension_item_status` (`tenant_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台维度值';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_report_directory`
--

DROP TABLE IF EXISTS `ai_report_directory`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_report_directory` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户ID',
  `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '父目录ID',
  `ancestors` varchar(500) NOT NULL DEFAULT '0' COMMENT '祖级链',
  `directory_name` varchar(100) NOT NULL COMMENT '目录名称',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0正常 1删除）',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_parent` (`tenant_id`,`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='报表目录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_report_material`
--

DROP TABLE IF EXISTS `ai_report_material`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_report_material` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `file_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '通用文件ID',
  `material_category` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'background' COMMENT '素材分类(background/panel/icon/illustration)',
  `is_private` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否私有(1私有 0公共)',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态(1正常 0删除)',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_report_material_file` (`tenant_id`,`file_id`),
  KEY `idx_report_material_category` (`tenant_id`,`material_category`,`status`),
  KEY `idx_report_material_create_by` (`create_by`),
  KEY `idx_report_material_scope` (`tenant_id`,`is_private`,`create_by`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='报表素材表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_report_project`
--

DROP TABLE IF EXISTS `ai_report_project`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_report_project` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户ID',
  `directory_id` bigint DEFAULT NULL COMMENT '所属目录ID',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  `project_name` varchar(100) NOT NULL COMMENT '项目名称',
  `index_img` varchar(500) DEFAULT NULL COMMENT '封面图URL',
  `status` char(1) NOT NULL DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `canvas_width` int NOT NULL DEFAULT '1920' COMMENT '画布宽度',
  `canvas_height` int NOT NULL DEFAULT '1080' COMMENT '画布高度',
  `background_color` varchar(20) DEFAULT '#1e1e2e' COMMENT '背景颜色',
  `component_data` longtext COMMENT '组件列表JSON',
  `publish_status` char(1) NOT NULL DEFAULT '0' COMMENT '发布状态（0未发布 1已发布）',
  `publish_url` varchar(500) DEFAULT NULL COMMENT '发布地址',
  `publish_time` datetime DEFAULT NULL COMMENT '发布时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0正常 1删除）',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_create_by` (`create_by`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='go-view项目表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_report_template`
--

DROP TABLE IF EXISTS `ai_report_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_report_template` (
  `id` bigint NOT NULL COMMENT '模板ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户ID',
  `source_project_id` bigint NOT NULL COMMENT '来源项目ID',
  `directory_id` bigint DEFAULT NULL COMMENT '所属目录ID',
  `template_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '模板名称',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  `index_img` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '封面图URL',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `canvas_width` int NOT NULL DEFAULT '1920' COMMENT '画布宽度',
  `canvas_height` int NOT NULL DEFAULT '1080' COMMENT '画布高度',
  `background_color` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '#1e1e2e' COMMENT '背景颜色',
  `component_data` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '模板组件JSON',
  `publish_status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '发布状态（0未发布 1已发布）',
  `template_scope` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '模板范围（0私有 1公开）',
  `publish_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '发布地址',
  `publish_time` datetime DEFAULT NULL COMMENT '发布时间',
  `copied_count` int NOT NULL DEFAULT '0' COMMENT '复制次数',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标志（0正常 1删除）',
  PRIMARY KEY (`id`),
  KEY `idx_template_tenant` (`tenant_id`),
  KEY `idx_template_source_project` (`source_project_id`),
  KEY `idx_template_directory` (`directory_id`),
  KEY `idx_template_publish_status` (`publish_status`),
  KEY `idx_template_scope` (`template_scope`),
  KEY `idx_template_create_by` (`create_by`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='模板表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_skill`
--

DROP TABLE IF EXISTS `ai_skill`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_skill` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL,
  `skill_name` varchar(100) NOT NULL COMMENT '技能名称',
  `skill_code` varchar(100) NOT NULL COMMENT '技能编码',
  `description` varchar(500) DEFAULT NULL COMMENT '描述',
  `version` varchar(32) DEFAULT '1.0.0' COMMENT '版本',
  `status` char(1) DEFAULT '0' COMMENT '状态(0正常 1停用)',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_dept` bigint DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标志(0正常，删除后写主键)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_skill_code_active` (`tenant_id`,`skill_code`,`del_flag`),
  KEY `idx_code` (`skill_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI技能包';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_skill_file`
--

DROP TABLE IF EXISTS `ai_skill_file`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_skill_file` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL,
  `skill_id` bigint NOT NULL COMMENT '技能ID',
  `file_path` varchar(500) NOT NULL COMMENT '技能内文件路径(SKILL.md/scripts/x.py)',
  `file_content` longtext NOT NULL COMMENT '文件内容',
  `encoding` varchar(16) DEFAULT 'utf-8' COMMENT '编码',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_dept` bigint DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标志(0正常，删除后写主键)',
  PRIMARY KEY (`id`),
  KEY `idx_skill` (`skill_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='技能文件';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_store_instance`
--

DROP TABLE IF EXISTS `ai_store_instance`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_store_instance` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户ID',
  `instance_name` varchar(100) NOT NULL COMMENT '实例名称',
  `category` varchar(32) NOT NULL COMMENT '类别(vector_store/search_engine)',
  `store_type` varchar(32) NOT NULL COMMENT '类型(MILVUS/PG_VECTOR/ELASTICSEARCH)',
  `config_json` longtext NOT NULL COMMENT '连接配置JSON(host/port/user/token/database等)',
  `status` char(1) NOT NULL DEFAULT '0' COMMENT '状态(0正常 1停用)',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_dept` bigint DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标志(0正常，删除后写主键)',
  PRIMARY KEY (`id`),
  KEY `idx_tenant` (`tenant_id`),
  KEY `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI向量存储/搜索引擎实例';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `biz_leave_request`
--

DROP TABLE IF EXISTS `biz_leave_request`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `biz_leave_request` (
  `id` varchar(64) NOT NULL COMMENT '主键ID',
  `business_key` varchar(64) NOT NULL COMMENT '业务Key（关联流程）',
  `process_instance_id` varchar(64) DEFAULT NULL COMMENT '流程实例ID',
  `apply_user_id` varchar(64) NOT NULL COMMENT '申请人ID',
  `apply_user_name` varchar(100) NOT NULL COMMENT '申请人姓名',
  `apply_dept_id` varchar(64) DEFAULT NULL COMMENT '申请部门ID',
  `apply_dept_name` varchar(200) DEFAULT NULL COMMENT '申请部门名称',
  `leave_type` varchar(20) NOT NULL COMMENT '请假类型：annual-年假/sick-病假/personal-事假/marriage-婚假/maternity-产假',
  `start_time` datetime NOT NULL COMMENT '开始时间',
  `end_time` datetime NOT NULL COMMENT '结束时间',
  `duration` decimal(10,1) NOT NULL COMMENT '请假天数',
  `reason` text COMMENT '请假原因',
  `attachments` text COMMENT '附件JSON列表',
  `status` varchar(20) NOT NULL DEFAULT 'draft' COMMENT '状态：draft-草稿/pending-审批中/approved-已通过/rejected-已驳回/canceled-已取消',
  `approve_user_id` varchar(64) DEFAULT NULL COMMENT '审批人ID',
  `approve_user_name` varchar(100) DEFAULT NULL COMMENT '审批人姓名',
  `approve_time` datetime DEFAULT NULL COMMENT '审批时间',
  `approve_comment` text COMMENT '审批意见',
  `approve_attachments` text COMMENT '审批附件JSON',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT '0' COMMENT '删除标记',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_business_key` (`business_key`),
  KEY `idx_apply_user` (`apply_user_id`),
  KEY `idx_process_instance` (`process_instance_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='请假申请表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `business_datasource_demo`
--

DROP TABLE IF EXISTS `business_datasource_demo`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `business_datasource_demo` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `title` varchar(128) NOT NULL COMMENT '标题',
  `route_key` varchar(128) DEFAULT NULL COMMENT 'dynamic-datasource路由Key',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_business_datasource_demo_tenant` (`tenant_id`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户业务数据源路由演示表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `config_properties`
--

DROP TABLE IF EXISTS `config_properties`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `config_properties` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `key` varchar(200) NOT NULL COMMENT '配置键',
  `value` text COMMENT '配置值',
  `description` varchar(500) DEFAULT NULL COMMENT '描述',
  `group` varchar(100) DEFAULT 'DEFAULT_GROUP' COMMENT '配置分组',
  `type` varchar(50) DEFAULT 'STRING' COMMENT '值类型(STRING/NUMBER/BOOLEAN/JSON)',
  `enabled` tinyint(1) DEFAULT '1' COMMENT '是否启用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_key` (`key`),
  KEY `idx_group` (`group`),
  KEY `idx_enabled` (`enabled`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='配置属性表';
/*!40101 SET character_set_client = @saved_cs_client */;
-- Table structure for table `forge_schema_history`
--

DROP TABLE IF EXISTS `forge_schema_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `forge_schema_history` (
  `installed_rank` int NOT NULL,
  `version` varchar(50) DEFAULT NULL,
  `description` varchar(200) NOT NULL,
  `type` varchar(20) NOT NULL,
  `script` varchar(1000) NOT NULL,
  `checksum` int DEFAULT NULL,
  `installed_by` varchar(100) NOT NULL,
  `installed_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `execution_time` int NOT NULL,
  `success` tinyint(1) NOT NULL,
  PRIMARY KEY (`installed_rank`),
  KEY `forge_schema_history_s_idx` (`success`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `forge_website_schema_history`
--

DROP TABLE IF EXISTS `forge_website_schema_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `forge_website_schema_history` (
  `installed_rank` int NOT NULL,
  `version` varchar(50) DEFAULT NULL,
  `description` varchar(200) NOT NULL,
  `type` varchar(20) NOT NULL,
  `script` varchar(1000) NOT NULL,
  `checksum` int DEFAULT NULL,
  `installed_by` varchar(100) NOT NULL,
  `installed_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `execution_time` int NOT NULL,
  `success` tinyint(1) NOT NULL,
  PRIMARY KEY (`installed_rank`),
  KEY `forge_website_schema_history_s_idx` (`success`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `gen_datasource`
--

DROP TABLE IF EXISTS `gen_datasource`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `gen_datasource` (
  `datasource_id` bigint NOT NULL AUTO_INCREMENT COMMENT '数据源ID',
  `datasource_name` varchar(100) NOT NULL COMMENT '数据源名称',
  `datasource_code` varchar(50) NOT NULL COMMENT '数据源编码（唯一标识）',
  `db_type` varchar(20) NOT NULL DEFAULT 'MySQL' COMMENT '数据库类型：MySQL/Oracle/PostgreSQL/SQLServer',
  `driver_class_name` varchar(200) NOT NULL COMMENT '驱动类名',
  `url` varchar(500) NOT NULL COMMENT 'JDBC连接地址',
  `username` varchar(100) NOT NULL COMMENT '用户名',
  `password` varchar(200) NOT NULL COMMENT '密码（加密存储）',
  `is_default` tinyint DEFAULT '0' COMMENT '是否默认数据源（1-是，0-否）',
  `is_enabled` tinyint DEFAULT '1' COMMENT '是否启用（1-启用，0-禁用）',
  `test_query` varchar(200) DEFAULT 'SELECT 1' COMMENT '测试查询SQL',
  `sort` int DEFAULT '0' COMMENT '排序',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新者',
  `usage_scope` varchar(32) NOT NULL DEFAULT 'BOTH' COMMENT '数据源用途范围：LOWCODE_RUNTIME/TENANT_BUSINESS/DEVELOPER_IMPORT/BOTH',
  `allow_runtime_write` tinyint NOT NULL DEFAULT '1' COMMENT '是否允许低代码运行时写入',
  `allow_runtime_ddl` tinyint NOT NULL DEFAULT '0' COMMENT '是否允许低代码执行运行时DDL',
  `readonly` tinyint NOT NULL DEFAULT '0' COMMENT '是否只读',
  `risk_level` varchar(16) NOT NULL DEFAULT 'MEDIUM' COMMENT '风险等级：LOW/MEDIUM/HIGH',
  PRIMARY KEY (`datasource_id`),
  UNIQUE KEY `uk_datasource_code` (`datasource_code`),
  KEY `idx_is_default` (`is_default`),
  KEY `idx_is_enabled` (`is_enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='代码生成器数据源配置';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `gen_project_config`
--

DROP TABLE IF EXISTS `gen_project_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `gen_project_config` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `config_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '配置名称',
  `template_id` bigint NOT NULL COMMENT '模板ID',
  `project_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '工程英文名',
  `display_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '系统展示名',
  `base_package` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'Java 基础包名',
  `group_id` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'Maven groupId',
  `database_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '数据库名',
  `artifact_prefix` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '父级 artifact 前缀',
  `module_artifact_prefix` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '子模块 artifact 前缀',
  `strip_module_prefix` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '需要剥离的子模块前缀',
  `subsystem` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '子系统ID',
  `include_modules` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '额外模块 JSON 数组',
  `build_options` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '构建扩展参数 JSON',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'ENABLED' COMMENT '状态：ENABLED/DISABLED',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_gen_project_config_template` (`tenant_id`,`template_id`),
  KEY `idx_gen_project_config_project` (`tenant_id`,`project_name`),
  KEY `idx_gen_project_config_status` (`tenant_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='项目构建配置';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `gen_project_task`
--

DROP TABLE IF EXISTS `gen_project_task`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `gen_project_task` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `config_id` bigint NOT NULL COMMENT '构建配置ID',
  `task_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '任务编号',
  `task_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'PENDING' COMMENT '任务状态：PENDING/RUNNING/SUCCESS/FAILED/CANCELLED',
  `progress` int NOT NULL DEFAULT '0' COMMENT '进度百分比',
  `current_stage` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '当前阶段',
  `message` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '状态消息',
  `work_dir` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '工作目录',
  `output_dir` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '生成输出目录',
  `artifact_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'zip 产物路径',
  `artifact_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'zip 产物文件名',
  `artifact_size` bigint DEFAULT NULL COMMENT 'zip 产物大小',
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '错误信息',
  `started_time` datetime DEFAULT NULL COMMENT '开始时间',
  `finished_time` datetime DEFAULT NULL COMMENT '完成时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_gen_project_task_no` (`tenant_id`,`task_no`),
  KEY `idx_gen_project_task_config` (`tenant_id`,`config_id`),
  KEY `idx_gen_project_task_status` (`tenant_id`,`task_status`),
  KEY `idx_gen_project_task_create_time` (`tenant_id`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='项目构建任务';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `gen_project_task_log`
--

DROP TABLE IF EXISTS `gen_project_task_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `gen_project_task_log` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `task_id` bigint NOT NULL COMMENT '任务ID',
  `log_level` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'INFO' COMMENT '日志级别',
  `log_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '日志内容',
  `log_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '日志时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_gen_project_task_log_task` (`tenant_id`,`task_id`,`log_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='项目构建任务日志';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `gen_project_template`
--

DROP TABLE IF EXISTS `gen_project_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `gen_project_template` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `template_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '模板名称',
  `template_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '模板编码',
  `source_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'GIT' COMMENT '模板来源类型：GIT',
  `source_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '模板 Git 地址',
  `source_ref` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'main' COMMENT '模板分支或标签',
  `cache_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '模板缓存路径',
  `catalog_snapshot` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '模块目录快照 JSON',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'ENABLED' COMMENT '状态：ENABLED/DISABLED',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_gen_project_template_code` (`tenant_id`,`template_code`),
  KEY `idx_gen_project_template_status` (`tenant_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='项目构建模板源';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `gen_table`
--

DROP TABLE IF EXISTS `gen_table`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `gen_table` (
  `table_id` bigint NOT NULL AUTO_INCREMENT COMMENT '表ID',
  `datasource_id` bigint unsigned DEFAULT NULL COMMENT '数据源ID',
  `table_name` varchar(200) NOT NULL COMMENT '表名称',
  `table_comment` varchar(500) DEFAULT NULL COMMENT '表描述',
  `class_name` varchar(100) NOT NULL COMMENT '实体类名称',
  `business_name` varchar(100) DEFAULT NULL COMMENT '业务名称',
  `function_name` varchar(100) DEFAULT NULL COMMENT '功能名称',
  `module_name` varchar(100) DEFAULT 'system' COMMENT '模块名称',
  `package_name` varchar(200) DEFAULT 'com.mdframe.forge.plugin' COMMENT '包路径',
  `author` varchar(50) DEFAULT 'Forge Generator' COMMENT '作者',
  `gen_type` varchar(20) DEFAULT 'DOWNLOAD' COMMENT '生成方式：DOWNLOAD-下载代码包/PROJECT-直接生成到项目',
  `gen_path` varchar(500) DEFAULT '/' COMMENT '生成路径',
  `template_engine` varchar(20) DEFAULT 'VELOCITY' COMMENT '模板引擎：VELOCITY/FREEMARKER/AI',
  `options` json DEFAULT NULL COMMENT '其他生成选项',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新者',
  PRIMARY KEY (`table_id`),
  UNIQUE KEY `uk_table_name` (`table_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='代码生成表配置';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `gen_table_column`
--

DROP TABLE IF EXISTS `gen_table_column`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `gen_table_column` (
  `column_id` bigint NOT NULL AUTO_INCREMENT COMMENT '字段ID',
  `table_id` bigint NOT NULL COMMENT '表ID',
  `column_name` varchar(200) NOT NULL COMMENT '字段名称',
  `column_comment` varchar(500) DEFAULT NULL COMMENT '字段描述',
  `column_type` varchar(100) NOT NULL COMMENT '字段类型',
  `java_type` varchar(50) DEFAULT 'String' COMMENT 'Java类型',
  `java_field` varchar(100) DEFAULT NULL COMMENT 'Java字段名',
  `is_pk` tinyint DEFAULT '0' COMMENT '是否主键',
  `is_increment` tinyint DEFAULT '0' COMMENT '是否自增',
  `is_required` tinyint DEFAULT '0' COMMENT '是否必填',
  `is_insert` tinyint DEFAULT '1' COMMENT '是否插入字段',
  `is_edit` tinyint DEFAULT '1' COMMENT '是否编辑字段',
  `is_list` tinyint DEFAULT '1' COMMENT '是否列表字段',
  `is_query` tinyint DEFAULT '0' COMMENT '是否查询字段',
  `query_type` varchar(20) DEFAULT 'EQ' COMMENT '查询方式：EQ/LIKE/BETWEEN/GT/LT等',
  `html_type` varchar(20) DEFAULT 'INPUT' COMMENT '显示类型：INPUT/SELECT/DATETIME/TEXTAREA等',
  `dict_type` varchar(100) DEFAULT NULL COMMENT '字典类型',
  `validate_rule` varchar(500) DEFAULT NULL COMMENT '验证规则（JSON格式）',
  `formula_config` text COMMENT '公式配置JSON {type,expression,mode,dependsOn,aggregate,condition}',
  `ai_recommended` tinyint(1) DEFAULT '0' COMMENT '是否AI推荐（0否 1是）',
  `sort` int DEFAULT '0' COMMENT '排序',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`column_id`),
  KEY `idx_table_id` (`table_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='代码生成表字段配置';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `gen_template`
--

DROP TABLE IF EXISTS `gen_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `gen_template` (
  `template_id` bigint NOT NULL AUTO_INCREMENT COMMENT '模板ID',
  `template_name` varchar(100) NOT NULL COMMENT '模板名称',
  `template_code` varchar(50) NOT NULL COMMENT '模板编码',
  `template_type` varchar(20) NOT NULL COMMENT '模板类型：ENTITY/MAPPER/SERVICE/CONTROLLER/DTO/VO/SQL',
  `template_engine` varchar(20) DEFAULT 'VELOCITY' COMMENT '模板引擎',
  `template_content` longtext NOT NULL COMMENT '模板内容',
  `file_suffix` varchar(20) DEFAULT '.java' COMMENT '生成文件后缀',
  `file_path` varchar(200) DEFAULT NULL COMMENT '生成文件路径（相对路径）',
  `is_system` tinyint DEFAULT '1' COMMENT '是否系统内置',
  `is_enabled` tinyint DEFAULT '1' COMMENT '是否启用',
  `sort` int DEFAULT '0' COMMENT '排序',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `tenant_id` bigint unsigned DEFAULT NULL,
  PRIMARY KEY (`template_id`),
  UNIQUE KEY `uk_template_code` (`template_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='代码生成模板配置';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `qrtz_blob_triggers`
--

DROP TABLE IF EXISTS `qrtz_blob_triggers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `qrtz_blob_triggers` (
  `sched_name` varchar(120) NOT NULL COMMENT '调度名称',
  `trigger_name` varchar(200) NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
  `trigger_group` varchar(200) NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
  `blob_data` blob COMMENT '存放持久化Trigger对象',
  PRIMARY KEY (`sched_name`,`trigger_name`,`trigger_group`),
  CONSTRAINT `qrtz_blob_triggers_ibfk_1` FOREIGN KEY (`sched_name`, `trigger_name`, `trigger_group`) REFERENCES `qrtz_triggers` (`sched_name`, `trigger_name`, `trigger_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='Blob类型的触发器表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `qrtz_calendars`
--

DROP TABLE IF EXISTS `qrtz_calendars`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `qrtz_calendars` (
  `sched_name` varchar(120) NOT NULL COMMENT '调度名称',
  `calendar_name` varchar(200) NOT NULL COMMENT '日历名称',
  `calendar` blob NOT NULL COMMENT '存放持久化calendar对象',
  PRIMARY KEY (`sched_name`,`calendar_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='日历信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `qrtz_cron_triggers`
--

DROP TABLE IF EXISTS `qrtz_cron_triggers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `qrtz_cron_triggers` (
  `sched_name` varchar(120) NOT NULL COMMENT '调度名称',
  `trigger_name` varchar(200) NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
  `trigger_group` varchar(200) NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
  `cron_expression` varchar(200) NOT NULL COMMENT 'cron表达式',
  `time_zone_id` varchar(80) DEFAULT NULL COMMENT '时区',
  PRIMARY KEY (`sched_name`,`trigger_name`,`trigger_group`),
  CONSTRAINT `qrtz_cron_triggers_ibfk_1` FOREIGN KEY (`sched_name`, `trigger_name`, `trigger_group`) REFERENCES `qrtz_triggers` (`sched_name`, `trigger_name`, `trigger_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='Cron类型的触发器表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `qrtz_fired_triggers`
--

DROP TABLE IF EXISTS `qrtz_fired_triggers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `qrtz_fired_triggers` (
  `sched_name` varchar(120) NOT NULL COMMENT '调度名称',
  `entry_id` varchar(95) NOT NULL COMMENT '调度器实例id',
  `trigger_name` varchar(200) NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
  `trigger_group` varchar(200) NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
  `instance_name` varchar(200) NOT NULL COMMENT '调度器实例名',
  `fired_time` bigint NOT NULL COMMENT '触发的时间',
  `sched_time` bigint NOT NULL COMMENT '定时器制定的时间',
  `priority` int NOT NULL COMMENT '优先级',
  `state` varchar(16) NOT NULL COMMENT '状态',
  `job_name` varchar(200) DEFAULT NULL COMMENT '任务名称',
  `job_group` varchar(200) DEFAULT NULL COMMENT '任务组名',
  `is_nonconcurrent` varchar(1) DEFAULT NULL COMMENT '是否并发',
  `requests_recovery` varchar(1) DEFAULT NULL COMMENT '是否接受恢复执行',
  PRIMARY KEY (`sched_name`,`entry_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='已触发的触发器表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `qrtz_job_details`
--

DROP TABLE IF EXISTS `qrtz_job_details`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `qrtz_job_details` (
  `sched_name` varchar(120) NOT NULL COMMENT '调度名称',
  `job_name` varchar(200) NOT NULL COMMENT '任务名称',
  `job_group` varchar(200) NOT NULL COMMENT '任务组名',
  `description` varchar(250) DEFAULT NULL COMMENT '相关介绍',
  `job_class_name` varchar(250) NOT NULL COMMENT '执行任务类名称',
  `is_durable` varchar(1) NOT NULL COMMENT '是否持久化',
  `is_nonconcurrent` varchar(1) NOT NULL COMMENT '是否并发',
  `is_update_data` varchar(1) NOT NULL COMMENT '是否更新数据',
  `requests_recovery` varchar(1) NOT NULL COMMENT '是否接受恢复执行',
  `job_data` blob COMMENT '存放持久化job对象',
  PRIMARY KEY (`sched_name`,`job_name`,`job_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='任务详细信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `qrtz_locks`
--

DROP TABLE IF EXISTS `qrtz_locks`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `qrtz_locks` (
  `sched_name` varchar(120) NOT NULL COMMENT '调度名称',
  `lock_name` varchar(40) NOT NULL COMMENT '悲观锁名称',
  PRIMARY KEY (`sched_name`,`lock_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='存储的悲观锁信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `qrtz_paused_trigger_grps`
--

DROP TABLE IF EXISTS `qrtz_paused_trigger_grps`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `qrtz_paused_trigger_grps` (
  `sched_name` varchar(120) NOT NULL COMMENT '调度名称',
  `trigger_group` varchar(200) NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
  PRIMARY KEY (`sched_name`,`trigger_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='暂停的触发器表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `qrtz_scheduler_state`
--

DROP TABLE IF EXISTS `qrtz_scheduler_state`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `qrtz_scheduler_state` (
  `sched_name` varchar(120) NOT NULL COMMENT '调度名称',
  `instance_name` varchar(200) NOT NULL COMMENT '实例名称',
  `last_checkin_time` bigint NOT NULL COMMENT '上次检查时间',
  `checkin_interval` bigint NOT NULL COMMENT '检查间隔时间',
  PRIMARY KEY (`sched_name`,`instance_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='调度器状态表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `qrtz_simple_triggers`
--

DROP TABLE IF EXISTS `qrtz_simple_triggers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `qrtz_simple_triggers` (
  `sched_name` varchar(120) NOT NULL COMMENT '调度名称',
  `trigger_name` varchar(200) NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
  `trigger_group` varchar(200) NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
  `repeat_count` bigint NOT NULL COMMENT '重复的次数统计',
  `repeat_interval` bigint NOT NULL COMMENT '重复的间隔时间',
  `times_triggered` bigint NOT NULL COMMENT '已经触发的次数',
  PRIMARY KEY (`sched_name`,`trigger_name`,`trigger_group`),
  CONSTRAINT `qrtz_simple_triggers_ibfk_1` FOREIGN KEY (`sched_name`, `trigger_name`, `trigger_group`) REFERENCES `qrtz_triggers` (`sched_name`, `trigger_name`, `trigger_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='简单触发器的信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `qrtz_simprop_triggers`
--

DROP TABLE IF EXISTS `qrtz_simprop_triggers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `qrtz_simprop_triggers` (
  `sched_name` varchar(120) NOT NULL COMMENT '调度名称',
  `trigger_name` varchar(200) NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
  `trigger_group` varchar(200) NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
  `str_prop_1` varchar(512) DEFAULT NULL COMMENT 'String类型的trigger的第一个参数',
  `str_prop_2` varchar(512) DEFAULT NULL COMMENT 'String类型的trigger的第二个参数',
  `str_prop_3` varchar(512) DEFAULT NULL COMMENT 'String类型的trigger的第三个参数',
  `int_prop_1` int DEFAULT NULL COMMENT 'int类型的trigger的第一个参数',
  `int_prop_2` int DEFAULT NULL COMMENT 'int类型的trigger的第二个参数',
  `long_prop_1` bigint DEFAULT NULL COMMENT 'long类型的trigger的第一个参数',
  `long_prop_2` bigint DEFAULT NULL COMMENT 'long类型的trigger的第二个参数',
  `dec_prop_1` decimal(13,4) DEFAULT NULL COMMENT 'decimal类型的trigger的第一个参数',
  `dec_prop_2` decimal(13,4) DEFAULT NULL COMMENT 'decimal类型的trigger的第二个参数',
  `bool_prop_1` varchar(1) DEFAULT NULL COMMENT 'Boolean类型的trigger的第一个参数',
  `bool_prop_2` varchar(1) DEFAULT NULL COMMENT 'Boolean类型的trigger的第二个参数',
  PRIMARY KEY (`sched_name`,`trigger_name`,`trigger_group`),
  CONSTRAINT `qrtz_simprop_triggers_ibfk_1` FOREIGN KEY (`sched_name`, `trigger_name`, `trigger_group`) REFERENCES `qrtz_triggers` (`sched_name`, `trigger_name`, `trigger_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='同步机制的行锁表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `qrtz_triggers`
--

DROP TABLE IF EXISTS `qrtz_triggers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `qrtz_triggers` (
  `sched_name` varchar(120) NOT NULL COMMENT '调度名称',
  `trigger_name` varchar(200) NOT NULL COMMENT '触发器的名字',
  `trigger_group` varchar(200) NOT NULL COMMENT '触发器所属组的名字',
  `job_name` varchar(200) NOT NULL COMMENT 'qrtz_job_details表job_name的外键',
  `job_group` varchar(200) NOT NULL COMMENT 'qrtz_job_details表job_group的外键',
  `description` varchar(250) DEFAULT NULL COMMENT '相关介绍',
  `next_fire_time` bigint DEFAULT NULL COMMENT '上一次触发时间（毫秒）',
  `prev_fire_time` bigint DEFAULT NULL COMMENT '下一次触发时间（默认为-1表示不触发）',
  `priority` int DEFAULT NULL COMMENT '优先级',
  `trigger_state` varchar(16) NOT NULL COMMENT '触发器状态',
  `trigger_type` varchar(8) NOT NULL COMMENT '触发器的类型',
  `start_time` bigint NOT NULL COMMENT '开始时间',
  `end_time` bigint DEFAULT NULL COMMENT '结束时间',
  `calendar_name` varchar(200) DEFAULT NULL COMMENT '日程表名称',
  `misfire_instr` smallint DEFAULT NULL COMMENT '补偿执行的策略',
  `job_data` blob COMMENT '存放持久化job对象',
  PRIMARY KEY (`sched_name`,`trigger_name`,`trigger_group`),
  KEY `sched_name` (`sched_name`,`job_name`,`job_group`),
  CONSTRAINT `qrtz_triggers_ibfk_1` FOREIGN KEY (`sched_name`, `job_name`, `job_group`) REFERENCES `qrtz_job_details` (`sched_name`, `job_name`, `job_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='触发器详细信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sample_purchase_order`
--

DROP TABLE IF EXISTS `sample_purchase_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sample_purchase_order` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `order_no` varchar(64) NOT NULL COMMENT '采购单号',
  `title` varchar(128) NOT NULL COMMENT '采购主题',
  `supplier_name` varchar(128) NOT NULL COMMENT '供应商',
  `amount_cent` bigint NOT NULL DEFAULT '0' COMMENT '采购金额，单位分',
  `purchase_items` text COMMENT '采购明细',
  `need_date` date DEFAULT NULL COMMENT '需求日期',
  `status` varchar(32) NOT NULL DEFAULT 'DRAFT' COMMENT '业务状态',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  `applicant_id` bigint DEFAULT NULL COMMENT '申请人ID',
  `applicant_name` varchar(64) DEFAULT NULL COMMENT '申请人名称',
  `applicant_dept_id` bigint DEFAULT NULL COMMENT '申请部门ID',
  `applicant_dept_name` varchar(128) DEFAULT NULL COMMENT '申请部门名称',
  `business_key` varchar(128) DEFAULT NULL COMMENT '流程业务Key',
  `process_instance_id` varchar(128) DEFAULT NULL COMMENT '流程实例ID',
  `dept_leader_id` bigint DEFAULT NULL COMMENT '部门负责人ID',
  `engineering_manager_id` bigint DEFAULT NULL COMMENT '工程部经理ID',
  `countersign_user_ids` varchar(500) DEFAULT NULL COMMENT '会签用户ID，逗号分隔',
  `cc_role_keys` varchar(500) DEFAULT NULL COMMENT '流程完成抄送角色编码，逗号分隔',
  `arrival_list_file_ids` varchar(1000) DEFAULT NULL COMMENT '负责人节点上传清单文件ID',
  `applicant_modify_remark` varchar(500) DEFAULT NULL COMMENT '申请人修改说明',
  `dept_leader_remark` varchar(500) DEFAULT NULL COMMENT '部门负责人意见',
  `engineering_manager_remark` varchar(500) DEFAULT NULL COMMENT '工程部经理意见',
  `countersign_remark` varchar(500) DEFAULT NULL COMMENT '会签意见',
  `reject_reason` varchar(500) DEFAULT NULL COMMENT '驳回原因',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sample_purchase_order_no_active` (`tenant_id`,`order_no`,`del_flag`),
  UNIQUE KEY `uk_sample_purchase_order_business_key_active` (`tenant_id`,`business_key`,`del_flag`),
  KEY `idx_sample_purchase_order_status` (`tenant_id`,`status`,`update_time`),
  KEY `idx_sample_purchase_order_process` (`tenant_id`,`process_instance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='采购单审批测试表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_api_config`
--

DROP TABLE IF EXISTS `sys_api_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_api_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `api_name` varchar(100) NOT NULL COMMENT '接口名称 (如:查询用户信息)',
  `api_code` varchar(100) DEFAULT NULL COMMENT '接口编码 (用于程序逻辑引用)',
  `req_method` varchar(20) NOT NULL COMMENT '请求方式 (GET, POST, PUT, DELETE, ALL)',
  `url_path` varchar(255) NOT NULL COMMENT '接口请求路径 (支持Ant风格, 如 /api/user/**)',
  `api_version` varchar(20) DEFAULT 'v1.0.0' COMMENT '接口版本号',
  `module_code` varchar(50) NOT NULL COMMENT '所属业务模块 (如: sys, order, pay)',
  `service_id` varchar(100) DEFAULT NULL COMMENT '所属微服务ID (若为微服务架构)',
  `auth_flag` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否需认证/鉴权 (Token校验)',
  `encrypt_flag` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否需报文加解密 (敏感数据保护)',
  `tenant_flag` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用租户隔离 (数据权限)',
  `limit_flag` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否开启限流',
  `sensitive_fields` text COMMENT '需脱敏字段 (JSON数组存储, 如 ["phone", "id_card"])',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态 (1-正常, 0-停用)',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注说明',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_method_url_active` (`url_path`,`req_method`,`del_flag`),
  KEY `idx_module` (`module_code`) USING BTREE COMMENT '模块查询索引',
  KEY `idx_tenant_id` (`tenant_id`) USING BTREE COMMENT '租户索引'
) ENGINE=InnoDB AUTO_INCREMENT=1670 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='REST接口配置管理表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_auth_online_user`
--

DROP TABLE IF EXISTS `sys_auth_online_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_auth_online_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `token_value` varchar(255) NOT NULL COMMENT 'Token值',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `username` varchar(100) NOT NULL COMMENT '用户名',
  `real_name` varchar(100) DEFAULT NULL COMMENT '真实姓名',
  `dept_name` varchar(100) DEFAULT NULL COMMENT '部门名称',
  `ip_address` varchar(50) DEFAULT NULL COMMENT '登录IP地址',
  `login_location` varchar(100) DEFAULT NULL COMMENT '登录地点',
  `browser` varchar(50) DEFAULT NULL COMMENT '浏览器类型',
  `os` varchar(50) DEFAULT NULL COMMENT '操作系统',
  `login_time` datetime NOT NULL COMMENT '登录时间',
  `last_activity_time` datetime DEFAULT NULL COMMENT '最后活动时间',
  `expire_time` datetime DEFAULT NULL COMMENT 'Token过期时间',
  `status` tinyint DEFAULT '1' COMMENT '状态: 1-在线, 0-已下线',
  `logout_time` datetime DEFAULT NULL COMMENT '登出时间',
  `logout_type` tinyint DEFAULT NULL COMMENT '登出类型: 1-主动登出, 2-被踢下线, 3-被顶下线, 4-Token过期',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_token` (`token_value`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_login_time` (`login_time`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='在线用户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_auth_online_user_history`
--

DROP TABLE IF EXISTS `sys_auth_online_user_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_auth_online_user_history` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `username` varchar(100) NOT NULL COMMENT '用户名',
  `ip_address` varchar(50) DEFAULT NULL COMMENT '登录IP地址',
  `login_location` varchar(100) DEFAULT NULL COMMENT '登录地点',
  `browser` varchar(50) DEFAULT NULL COMMENT '浏览器类型',
  `os` varchar(50) DEFAULT NULL COMMENT '操作系统',
  `login_time` datetime NOT NULL COMMENT '登录时间',
  `logout_time` datetime DEFAULT NULL COMMENT '登出时间',
  `online_duration` int DEFAULT NULL COMMENT '在线时长(秒)',
  `logout_type` tinyint DEFAULT NULL COMMENT '登出类型: 1-主动登出, 2-被踢下线, 3-被顶下线, 4-Token过期',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_login_time` (`login_time`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='在线用户历史表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_cache_policy`
--

DROP TABLE IF EXISTS `sys_cache_policy`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_cache_policy` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '策略主键',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID，平台策略固定为1',
  `application_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '应用编码',
  `cache_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '代码注册的缓存名',
  `enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用：0否 1是',
  `cache_mode` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '缓存模式：LOCAL REDIS MULTI',
  `local_ttl_seconds` bigint NOT NULL COMMENT '本地缓存TTL（秒）',
  `redis_ttl_seconds` bigint NOT NULL COMMENT 'Redis缓存TTL（秒）',
  `local_max_size` int NOT NULL COMMENT '本地缓存最大条目数',
  `cache_null` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否缓存空值：0否 1是',
  `null_ttl_seconds` bigint NOT NULL COMMENT '空值TTL（秒）',
  `policy_version` bigint NOT NULL DEFAULT '1' COMMENT '乐观锁版本',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '删除标志：0正常，删除后写主键',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cache_policy_active` (`tenant_id`,`application_code`,`cache_name`,`del_flag`),
  KEY `idx_cache_policy_lookup` (`tenant_id`,`application_code`,`cache_name`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='受管缓存运行策略覆盖';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_client`
--

DROP TABLE IF EXISTS `sys_client`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_client` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `client_code` varchar(50) NOT NULL COMMENT '客户端编码',
  `client_name` varchar(100) NOT NULL COMMENT '客户端名称',
  `app_id` varchar(64) NOT NULL COMMENT '应用ID',
  `app_secret` varchar(128) DEFAULT NULL COMMENT '应用密钥摘要，仅client_secret客户端使用',
  `client_auth_method` varchar(32) NOT NULL DEFAULT 'client_secret' COMMENT '客户端认证方式：none/client_secret',
  `token_timeout` bigint DEFAULT '7200' COMMENT 'Token有效期（秒）',
  `token_activity_timeout` bigint DEFAULT '-1' COMMENT 'Token活跃超时（秒）',
  `token_prefix` varchar(20) DEFAULT 'Bearer' COMMENT 'Token前缀',
  `token_name` varchar(50) DEFAULT 'Authorization' COMMENT 'Token名称',
  `concurrent_login` tinyint(1) DEFAULT '0' COMMENT '是否允许并发登录（0-否 1-是）',
  `share_token` tinyint(1) DEFAULT '0' COMMENT '是否共享Token（0-否 1-是）',
  `enable_ip_limit` tinyint(1) DEFAULT '0' COMMENT '是否启用IP限制',
  `ip_whitelist` text COMMENT 'IP白名单（JSON数组）',
  `enable_encrypt` tinyint(1) DEFAULT '0' COMMENT '是否启用加密传输',
  `encrypt_algorithm` varchar(50) DEFAULT 'AES' COMMENT '加密算法',
  `max_user_count` bigint DEFAULT '-1' COMMENT '最大用户数（-1不限）',
  `max_online_count` bigint DEFAULT '-1' COMMENT '最大在线数（-1不限）',
  `auth_types` varchar(255) DEFAULT 'password' COMMENT '支持的认证方式（逗号分隔）',
  `captcha_type` varchar(32) DEFAULT NULL COMMENT '验证码类型：graphical图形，slider滑块，sms短信；为空继承全局配置',
  `status` tinyint DEFAULT '1' COMMENT '状态（0-禁用 1-启用）',
  `description` varchar(500) DEFAULT NULL COMMENT '客户端描述',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `create_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `create_dept` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_client_code` (`client_code`),
  UNIQUE KEY `uk_app_id` (`app_id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='客户端管理表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_config`
--

DROP TABLE IF EXISTS `sys_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_config` (
  `config_id` bigint NOT NULL AUTO_INCREMENT COMMENT '参数主键',
  `tenant_id` bigint DEFAULT '0' COMMENT '租户编号',
  `config_name` varchar(100) DEFAULT '' COMMENT '参数名称',
  `config_key` varchar(100) DEFAULT '' COMMENT '参数键名',
  `config_value` varchar(500) DEFAULT '' COMMENT '参数键值',
  `config_type` char(1) DEFAULT 'N' COMMENT '系统内置（Y是 N否）',
  `config_desc` varchar(500) DEFAULT NULL COMMENT '参数描述',
  `sort` int DEFAULT '0' COMMENT '排序',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_dept` bigint unsigned DEFAULT NULL COMMENT '创建组织ID',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`config_id`),
  UNIQUE KEY `uk_tenant_config_key_active` (`tenant_id`,`config_key`,`del_flag`),
  KEY `idx_config_type` (`config_type`)
) ENGINE=InnoDB AUTO_INCREMENT=448 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_config_group`
--

DROP TABLE IF EXISTS `sys_config_group`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_config_group` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `group_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分组编码',
  `group_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分组名称',
  `group_icon` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '分组图标',
  `config_value` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '配置值(JSON格式)',
  `sort` int DEFAULT '0' COMMENT '排序',
  `status` tinyint DEFAULT '1' COMMENT '状态(0-禁用 1-启用)',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_group_code` (`group_code`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统配置分组表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_data_scope_config`
--

DROP TABLE IF EXISTS `sys_data_scope_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_data_scope_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `resource_code` varchar(100) NOT NULL COMMENT '资源编码（对应接口路径或功能模块）',
  `resource_name` varchar(100) DEFAULT NULL COMMENT '资源名称',
  `mapper_method` varchar(200) NOT NULL COMMENT 'Mapper方法（如：com.example.mapper.UserMapper.selectList）',
  `table_alias` varchar(50) DEFAULT 't' COMMENT '主表别名',
  `user_id_column` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '用户ID字段名',
  `org_id_column` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '组织ID字段名',
  `tenant_id_column` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '租户ID字段名',
  `region_code_column` varchar(250) DEFAULT NULL COMMENT '行政区划字段名',
  `user_region_column` varchar(200) DEFAULT NULL COMMENT '用户表行政区划字段名',
  `user_table_alias` varchar(100) DEFAULT NULL COMMENT '用户表别名',
  `flow_related_visible` tinyint NOT NULL DEFAULT '0' COMMENT '流程经手人可查看单据（0否 1是）',
  `flow_business_type` varchar(128) DEFAULT NULL COMMENT '流程 businessKey 前缀，对应 sys_flow_record_participant.business_type',
  `record_id_column` varchar(64) DEFAULT NULL COMMENT '业务表主键列，对应 businessKey 中的记录ID，默认 id',
  `enabled` tinyint DEFAULT '1' COMMENT '是否启用（0-禁用，1-启用）',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_dept` bigint unsigned DEFAULT NULL COMMENT '创建组织ID',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_mapper_active` (`tenant_id`,`mapper_method`,`del_flag`),
  KEY `idx_resource_code` (`resource_code`),
  KEY `idx_enabled` (`enabled`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='数据权限配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_dict_data`
--

DROP TABLE IF EXISTS `sys_dict_data`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_dict_data` (
  `dict_code` bigint NOT NULL AUTO_INCREMENT COMMENT '字典编码',
  `tenant_id` bigint DEFAULT '0' COMMENT '租户编号',
  `dict_sort` int DEFAULT '0' COMMENT '字典排序',
  `dict_label` varchar(100) DEFAULT '' COMMENT '字典标签',
  `dict_value` varchar(100) DEFAULT '' COMMENT '字典键值',
  `dict_type` varchar(100) DEFAULT '' COMMENT '字典类型',
  `css_class` varchar(100) DEFAULT NULL COMMENT '样式属性（其他样式扩展）',
  `list_class` varchar(100) DEFAULT NULL COMMENT '表格回显样式',
  `is_default` char(1) DEFAULT 'N' COMMENT '是否默认（Y是 N否）',
  `parent_dict_code` bigint unsigned DEFAULT NULL COMMENT '上级字典编码',
  `linked_dict_type` varchar(100) DEFAULT NULL COMMENT '关联的字典类型',
  `linked_dict_value` varchar(100) DEFAULT NULL COMMENT '关联的字典值',
  `dict_status` tinyint DEFAULT '1' COMMENT '字典状态（0-禁用，1-正常）',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_dept` bigint unsigned DEFAULT NULL COMMENT '创建组织ID',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`dict_code`),
  UNIQUE KEY `uk_tenant_dict_data_active` (`tenant_id`,`dict_type`,`dict_value`,`del_flag`),
  KEY `idx_tenant_dict_type` (`tenant_id`,`dict_type`),
  KEY `idx_parent_dict_code` (`parent_dict_code`),
  KEY `idx_linked_dict` (`linked_dict_type`,`linked_dict_value`)
) ENGINE=InnoDB AUTO_INCREMENT=1325 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='字典数据表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_dict_type`
--

DROP TABLE IF EXISTS `sys_dict_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_dict_type` (
  `dict_id` bigint NOT NULL AUTO_INCREMENT COMMENT '字典主键',
  `tenant_id` bigint DEFAULT '0' COMMENT '租户编号',
  `dict_name` varchar(100) DEFAULT '' COMMENT '字典名称',
  `dict_type` varchar(100) DEFAULT '' COMMENT '字典类型（租户内唯一）',
  `dict_status` tinyint DEFAULT '1' COMMENT '字典状态（0-禁用，1-正常）',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_dept` bigint unsigned DEFAULT NULL COMMENT '创建组织ID',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`dict_id`),
  UNIQUE KEY `uk_tenant_dict_type_active` (`tenant_id`,`dict_type`,`del_flag`),
  KEY `idx_dict_status` (`dict_status`)
) ENGINE=InnoDB AUTO_INCREMENT=315 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='字典类型表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_email_config`
--

DROP TABLE IF EXISTS `sys_email_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_email_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `config_id` varchar(64) DEFAULT NULL COMMENT '配置标识',
  `smtp_server` varchar(128) NOT NULL COMMENT 'SMTP服务器地址',
  `port` int DEFAULT '465' COMMENT '端口',
  `username` varchar(128) DEFAULT NULL COMMENT '用户名',
  `password` varchar(256) DEFAULT NULL COMMENT '密码/授权码',
  `from_address` varchar(128) NOT NULL COMMENT '发件人地址',
  `from_name` varchar(64) DEFAULT NULL COMMENT '发件人名称',
  `is_ssl` tinyint DEFAULT '1' COMMENT '是否开启SSL',
  `is_auth` tinyint DEFAULT '1' COMMENT '是否开启验证',
  `retry_interval` int DEFAULT '5' COMMENT '重试间隔(秒)',
  `max_retries` int DEFAULT '1' COMMENT '最大重试次数',
  `status` tinyint DEFAULT '0' COMMENT '状态(0禁用 1启用)',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_status` (`tenant_id`,`status`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='邮件配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_employee`
--

DROP TABLE IF EXISTS `sys_employee`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_employee` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `emp_name` varchar(100) NOT NULL COMMENT '姓名',
  `emp_no` varchar(50) NOT NULL COMMENT '工号',
  `dept_id` bigint DEFAULT NULL COMMENT '部门ID',
  `position` varchar(100) DEFAULT NULL COMMENT '职位',
  `hire_date` date DEFAULT NULL COMMENT '入职日期',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态(1:正常,0:停用)',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_emp_no_active` (`emp_no`,`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='员工信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_excel_column_config`
--

DROP TABLE IF EXISTS `sys_excel_column_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_excel_column_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `config_key` varchar(100) NOT NULL COMMENT '关联配置键',
  `field_name` varchar(100) NOT NULL COMMENT '字段名（实体属性名）',
  `column_name` varchar(200) NOT NULL COMMENT '列名（Excel表头）',
  `width` int DEFAULT '20' COMMENT '列宽',
  `order_num` int DEFAULT '0' COMMENT '排序（值越小越靠前）',
  `export` tinyint(1) DEFAULT '1' COMMENT '是否导出（1-是，0-否）',
  `date_format` varchar(50) DEFAULT NULL COMMENT '日期格式（如：yyyy-MM-dd）',
  `number_format` varchar(50) DEFAULT NULL COMMENT '数字格式',
  `dict_type` varchar(50) DEFAULT NULL COMMENT '字典类型（关联sys_dict_type）',
  `importable` tinyint(1) DEFAULT '1' COMMENT '是否可导入（1-是，0-否）',
  `required` tinyint(1) DEFAULT '0' COMMENT '是否必填（1-是，0-否）',
  `example_value` varchar(255) DEFAULT NULL COMMENT '示例值（用于模板）',
  `validation_rule` varchar(255) DEFAULT NULL COMMENT '校验规则（正则表达式）',
  `validation_message` varchar(255) DEFAULT NULL COMMENT '校验失败提示信息',
  `column_type` varchar(20) DEFAULT 'TEXT' COMMENT '列类型：TEXT/IMAGE',
  `image_width` int DEFAULT '120' COMMENT '图片列导出宽度(像素)',
  `image_height` int DEFAULT '90' COMMENT '图片列导出高度(像素)',
  `image_max_count` int DEFAULT '5' COMMENT '图片列最大图片数量',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `create_dept` bigint unsigned DEFAULT NULL COMMENT '创建组织ID',
  PRIMARY KEY (`id`),
  KEY `idx_config_key` (`config_key`),
  KEY `idx_order` (`order_num`)
) ENGINE=InnoDB AUTO_INCREMENT=2105218155960188930 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Excel列配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_excel_export_config`
--

DROP TABLE IF EXISTS `sys_excel_export_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_excel_export_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `config_key` varchar(100) NOT NULL COMMENT '配置键（唯一标识）',
  `config_type` varchar(16) NOT NULL DEFAULT 'BOTH' COMMENT '配置类型：EXPORT导出 IMPORT导入 BOTH导入导出',
  `export_name` varchar(200) NOT NULL COMMENT '导出名称',
  `sheet_name` varchar(100) DEFAULT 'Sheet1' COMMENT 'Sheet名称',
  `file_name_template` varchar(200) DEFAULT NULL COMMENT '文件名模板（支持占位符：{date}、{time}）',
  `data_source_bean` varchar(100) DEFAULT NULL COMMENT '数据源Bean名称（如：userService，导入-only可为空）',
  `query_method` varchar(100) DEFAULT NULL COMMENT '数据查询方法名（如：list、page，导入-only可为空）',
  `auto_trans` tinyint(1) DEFAULT '1' COMMENT '是否自动翻译字典（1-是，0-否）',
  `pageable` tinyint(1) DEFAULT '0' COMMENT '是否分页查询（1-是，0-否）',
  `max_rows` int DEFAULT '10000' COMMENT '最大导出条数',
  `sort_field` varchar(50) DEFAULT NULL COMMENT '排序字段',
  `sort_order` varchar(10) DEFAULT NULL COMMENT '排序方向（ASC/DESC）',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态（1-启用，0-禁用）',
  `include_sample` tinyint(1) DEFAULT '0' COMMENT '是否包含示例数据（用于模板）',
  `allow_import` tinyint(1) DEFAULT '1' COMMENT '是否允许导入',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `create_dept` bigint unsigned DEFAULT NULL COMMENT '创建组织ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `config_key` (`config_key`),
  KEY `idx_config_key` (`config_key`)
) ENGINE=InnoDB AUTO_INCREMENT=2105217920747814914 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Excel导出配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_external_api`
--

DROP TABLE IF EXISTS `sys_external_api`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_external_api` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `system_id` bigint NOT NULL COMMENT '所属系统ID',
  `api_code` varchar(50) NOT NULL COMMENT '接口编码',
  `api_name` varchar(100) NOT NULL COMMENT '接口名称',
  `api_desc` varchar(500) DEFAULT NULL COMMENT '接口描述',
  `execution_mode` varchar(16) NOT NULL DEFAULT 'HTTP' COMMENT '执行模式：HTTP/MOCK',
  `api_path` varchar(255) NOT NULL COMMENT '接口路径',
  `api_method` varchar(10) NOT NULL COMMENT '请求方法(GET/POST/PUT/DELETE)',
  `request_content_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '请求Content-Type',
  `request_headers` text COMMENT '请求头配置(JSON)',
  `request_params` text COMMENT '请求参数配置(JSON)',
  `request_body_template` text COMMENT '请求体模板',
  `response_content_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '响应Content-Type',
  `response_data_path` varchar(100) DEFAULT NULL COMMENT '响应数据路径(JsonPath)',
  `response_total_path` varchar(100) DEFAULT NULL COMMENT '响应总数路径(JsonPath)',
  `mock_response_json` text COMMENT 'Mock响应JSON，仅MOCK模式使用',
  `param_mapping_enabled` tinyint(1) DEFAULT '0' COMMENT '是否启用参数映射',
  `param_mappings` text COMMENT '参数映射配置(JSON)',
  `response_transform_enabled` tinyint(1) DEFAULT '0' COMMENT '是否启用响应转换',
  `response_transform_script` text COMMENT '响应转换脚本',
  `error_code_path` varchar(100) DEFAULT NULL COMMENT '错误码路径(JsonPath)',
  `error_msg_path` varchar(100) DEFAULT NULL COMMENT '错误消息路径(JsonPath)',
  `success_codes` varchar(100) DEFAULT NULL COMMENT '成功码列表',
  `doc_file_id` varchar(100) DEFAULT NULL COMMENT '接口文档文件ID',
  `doc_file_name` varchar(255) DEFAULT NULL COMMENT '接口文档文件名',
  `rate_limit_enabled` tinyint(1) DEFAULT '0' COMMENT '是否启用限流',
  `rate_limit_qps` int DEFAULT NULL COMMENT '限流QPS',
  `cache_enabled` tinyint(1) DEFAULT '0' COMMENT '是否启用缓存',
  `cache_ttl` int DEFAULT NULL COMMENT '缓存时间(秒)',
  `cache_key_template` varchar(255) DEFAULT NULL COMMENT '缓存Key模板',
  `permission_check_enabled` tinyint(1) DEFAULT '1' COMMENT '是否启用权限检查',
  `required_permission` varchar(100) DEFAULT NULL COMMENT '所需权限',
  `lowcode_query_enabled` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否允许作为低代码只读查询源',
  `input_schema_json` text COMMENT '低代码查询输入Schema(JSON数组)',
  `output_schema_json` text COMMENT '低代码查询输出Schema(JSON数组)',
  `api_status` int DEFAULT '1' COMMENT '状态(0停用 1启用)',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_api_code_system` (`api_code`,`system_id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_system_id` (`system_id`),
  KEY `idx_external_api_lowcode_query` (`tenant_id`,`lowcode_query_enabled`,`api_status`,`system_id`),
  KEY `idx_external_api_execution_mode` (`tenant_id`,`execution_mode`,`api_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='外部接口配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_external_api_log`
--

DROP TABLE IF EXISTS `sys_external_api_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_external_api_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `system_id` bigint DEFAULT NULL COMMENT '外部系统ID',
  `api_id` bigint NOT NULL COMMENT '外部接口ID',
  `system_name` varchar(100) DEFAULT NULL COMMENT '系统名称快照',
  `api_name` varchar(100) DEFAULT NULL COMMENT '接口名称快照',
  `api_code` varchar(50) DEFAULT NULL COMMENT '接口编码快照',
  `request_method` varchar(10) DEFAULT NULL COMMENT '请求方法',
  `request_url` varchar(1000) DEFAULT NULL COMMENT '请求URL',
  `request_params` text COMMENT '请求参数',
  `request_body` text COMMENT '请求体',
  `response_body` text COMMENT '响应体',
  `http_status_code` int DEFAULT NULL COMMENT 'HTTP状态码',
  `call_status` tinyint DEFAULT '1' COMMENT '调用状态（0失败 1成功）',
  `error_message` varchar(1000) DEFAULT NULL COMMENT '错误信息',
  `duration_ms` bigint DEFAULT NULL COMMENT '耗时毫秒',
  `debug_flag` tinyint(1) DEFAULT '0' COMMENT '是否调试调用',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_api` (`tenant_id`,`api_id`),
  KEY `idx_system_id` (`system_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='外部接口调用日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_external_system`
--

DROP TABLE IF EXISTS `sys_external_system`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_external_system` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `system_code` varchar(50) NOT NULL COMMENT '系统编码',
  `system_name` varchar(100) NOT NULL COMMENT '系统名称',
  `system_desc` varchar(500) DEFAULT NULL COMMENT '系统描述',
  `base_url` varchar(255) NOT NULL COMMENT '基础URL',
  `auth_type` varchar(20) NOT NULL COMMENT '认证类型(none/basic/token/oauth2/api_key/custom)',
  `basic_username` varchar(100) DEFAULT NULL COMMENT 'Basic认证用户名',
  `basic_password` varchar(255) DEFAULT NULL COMMENT 'Basic认证密码',
  `token_value` varchar(500) DEFAULT NULL COMMENT 'Token值',
  `token_header_name` varchar(50) DEFAULT NULL COMMENT 'Token Header名称',
  `token_prefix` varchar(20) DEFAULT NULL COMMENT 'Token前缀',
  `oauth2_token_url` varchar(255) DEFAULT NULL COMMENT 'OAuth2 Token URL',
  `oauth2_client_id` varchar(100) DEFAULT NULL COMMENT 'OAuth2 Client ID',
  `oauth2_client_secret` varchar(255) DEFAULT NULL COMMENT 'OAuth2 Client Secret',
  `oauth2_grant_type` varchar(20) DEFAULT NULL COMMENT 'OAuth2授权类型',
  `oauth2_scope` varchar(100) DEFAULT NULL COMMENT 'OAuth2 Scope',
  `api_key_name` varchar(50) DEFAULT NULL COMMENT 'API Key名称',
  `api_key_value` varchar(255) DEFAULT NULL COMMENT 'API Key值',
  `api_key_position` varchar(20) DEFAULT NULL COMMENT 'API Key位置(header/query)',
  `custom_auth_adapter` varchar(100) DEFAULT NULL COMMENT '自定义认证适配器',
  `custom_auth_config` text COMMENT '自定义认证配置',
  `trusted_internal` tinyint(1) DEFAULT '0' COMMENT '是否可信内部Forge系统',
  `proxy_enabled` tinyint(1) DEFAULT '0' COMMENT '是否启用代理',
  `proxy_host` varchar(100) DEFAULT NULL COMMENT '代理主机',
  `proxy_port` int DEFAULT NULL COMMENT '代理端口',
  `proxy_username` varchar(100) DEFAULT NULL COMMENT '代理用户名',
  `proxy_password` varchar(255) DEFAULT NULL COMMENT '代理密码',
  `retry_enabled` tinyint(1) DEFAULT '0' COMMENT '是否启用重试',
  `retry_max_attempts` int DEFAULT NULL COMMENT '最大重试次数',
  `retry_backoff_interval` int DEFAULT NULL COMMENT '重试间隔(ms)',
  `connect_timeout` int DEFAULT NULL COMMENT '连接超时(ms)',
  `read_timeout` int DEFAULT NULL COMMENT '读取超时(ms)',
  `write_timeout` int DEFAULT NULL COMMENT '写入超时(ms)',
  `ssl_verify_enabled` tinyint(1) DEFAULT '1' COMMENT '是否启用SSL验证',
  `request_logging_enabled` tinyint(1) DEFAULT '0' COMMENT '是否启用请求日志',
  `system_status` int DEFAULT '1' COMMENT '状态(0停用 1启用)',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_system_code_tenant` (`system_code`,`tenant_id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='外部系统配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_file_group`
--

DROP TABLE IF EXISTS `sys_file_group`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_file_group` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `group_name` varchar(100) NOT NULL COMMENT '分组名称',
  `group_code` varchar(100) DEFAULT NULL COMMENT '分组编码',
  `group_type` varchar(50) DEFAULT 'default' COMMENT '分组类型(document-文档,image-图片,video-视频,audio-音频,archive-压缩包,default-默认)',
  `parent_id` bigint DEFAULT NULL COMMENT '父分组ID',
  `sort` int DEFAULT '0' COMMENT '排序',
  `icon` varchar(100) DEFAULT NULL COMMENT '图标',
  `description` varchar(500) DEFAULT NULL COMMENT '描述',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态(1-正常,0-禁用)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `create_dept` bigint unsigned DEFAULT NULL COMMENT '创建组织ID',
  `deleted` tinyint(1) DEFAULT '0' COMMENT '删除标记(0-未删除,1-已删除)',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_group_type` (`group_type`),
  KEY `idx_group_code` (`group_code`),
  KEY `idx_sys_file_group_tenant` (`tenant_id`,`deleted`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文件分组表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_file_metadata`
--

DROP TABLE IF EXISTS `sys_file_metadata`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_file_metadata` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `file_id` varchar(64) NOT NULL COMMENT '文件唯一ID',
  `group_id` bigint DEFAULT NULL COMMENT '分组ID',
  `original_name` varchar(500) NOT NULL COMMENT '原始文件名',
  `storage_name` varchar(500) NOT NULL COMMENT '存储文件名',
  `file_path` varchar(1000) NOT NULL COMMENT '文件路径',
  `file_size` bigint NOT NULL COMMENT '文件大小(字节)',
  `mime_type` varchar(200) DEFAULT NULL COMMENT '文件MIME类型',
  `extension` varchar(50) DEFAULT NULL COMMENT '文件扩展名',
  `md5` varchar(64) DEFAULT NULL COMMENT '文件MD5',
  `storage_type` varchar(50) NOT NULL COMMENT '存储策略',
  `bucket` varchar(100) DEFAULT NULL COMMENT '存储桶/命名空间',
  `access_url` varchar(1000) DEFAULT NULL COMMENT '访问URL',
  `thumbnail_url` varchar(1000) DEFAULT NULL COMMENT '缩略图URL',
  `business_type` varchar(100) DEFAULT NULL COMMENT '业务类型',
  `business_id` varchar(100) DEFAULT NULL COMMENT '业务ID',
  `uploader_id` bigint DEFAULT NULL COMMENT '上传者ID',
  `upload_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  `expire_time` datetime DEFAULT NULL COMMENT '过期时间',
  `is_private` tinyint(1) DEFAULT '0' COMMENT '是否私有',
  `download_count` int DEFAULT '0' COMMENT '下载次数',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '逻辑删除状态(1-正常,0-已删除)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `create_dept` bigint unsigned DEFAULT NULL COMMENT '创建组织ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `file_id` (`file_id`),
  KEY `idx_file_id` (`file_id`),
  KEY `idx_md5` (`md5`),
  KEY `idx_business` (`business_type`,`business_id`),
  KEY `idx_uploader` (`uploader_id`),
  KEY `idx_upload_time` (`upload_time`),
  KEY `idx_sys_file_metadata_tenant` (`tenant_id`,`status`,`upload_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文件元数据表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_file_storage_config`
--

DROP TABLE IF EXISTS `sys_file_storage_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_file_storage_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `config_name` varchar(100) NOT NULL COMMENT '配置名称',
  `storage_type` varchar(50) NOT NULL COMMENT '存储类型(local/minio/aliyun_oss等)',
  `is_default` tinyint(1) DEFAULT '0' COMMENT '是否默认策略',
  `enabled` tinyint(1) DEFAULT '1' COMMENT '是否启用',
  `endpoint` varchar(500) DEFAULT NULL COMMENT '访问端点',
  `access_key` varchar(200) DEFAULT NULL COMMENT '访问密钥ID',
  `secret_key` varchar(200) DEFAULT NULL COMMENT '访问密钥Secret',
  `bucket_name` varchar(100) DEFAULT NULL COMMENT '存储桶名称',
  `region` varchar(100) DEFAULT NULL COMMENT '区域',
  `base_path` varchar(200) DEFAULT NULL COMMENT '基础路径',
  `domain` varchar(500) DEFAULT NULL COMMENT '访问域名',
  `use_https` tinyint(1) DEFAULT '1' COMMENT '是否使用HTTPS',
  `max_file_size` int DEFAULT '100' COMMENT '最大文件大小(MB)',
  `allowed_types` varchar(500) NOT NULL COMMENT '允许的文件类型(逗号分隔)',
  `order_num` int DEFAULT '0' COMMENT '排序',
  `extra_config` text COMMENT '扩展配置(JSON)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `create_dept` bigint unsigned DEFAULT NULL COMMENT '创建组织ID',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标志（0正常 1删除）',
  PRIMARY KEY (`id`),
  KEY `idx_storage_type` (`storage_type`),
  KEY `idx_is_default` (`is_default`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文件存储配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_flow_approval_level`
--

DROP TABLE IF EXISTS `sys_flow_approval_level`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_flow_approval_level` (
  `id` varchar(64) NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `node_config_id` varchar(64) NOT NULL COMMENT '节点配置ID',
  `level_index` int NOT NULL COMMENT '层级序号',
  `level_name` varchar(100) DEFAULT NULL COMMENT '层级名称',
  `assignee_type` varchar(50) DEFAULT NULL COMMENT '审批人类型',
  `assignee_value` text COMMENT '审批人值',
  `condition_expr` varchar(500) DEFAULT NULL COMMENT '层级条件表达式',
  `skip_condition` varchar(500) DEFAULT NULL COMMENT '跳过条件',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '删除标志（0正常 1删除）',
  PRIMARY KEY (`id`),
  KEY `idx_node_config_id` (`node_config_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='审批层级配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_flow_business`
--

DROP TABLE IF EXISTS `sys_flow_business`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_flow_business` (
  `id` varchar(64) NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `business_key` varchar(100) NOT NULL COMMENT '业务Key',
  `business_type` varchar(50) NOT NULL COMMENT '业务类型（order/contract等）',
  `process_instance_id` varchar(64) DEFAULT NULL COMMENT '流程实例ID',
  `process_def_id` varchar(64) DEFAULT NULL COMMENT '流程定义ID',
  `process_def_key` varchar(100) DEFAULT NULL COMMENT '流程定义KEY',
  `title` varchar(200) DEFAULT NULL COMMENT '流程标题',
  `status` varchar(50) DEFAULT 'draft' COMMENT '业务状态（draft-草稿/running-审批中/approved-已通过/rejected-已驳回/canceled-已取消）',
  `apply_user_id` varchar(64) DEFAULT NULL COMMENT '申请人',
  `apply_user_name` varchar(100) DEFAULT NULL COMMENT '申请人姓名',
  `apply_dept_id` varchar(64) DEFAULT NULL COMMENT '申请部门ID',
  `apply_dept_name` varchar(100) DEFAULT NULL COMMENT '申请部门名称',
  `apply_time` datetime DEFAULT NULL COMMENT '申请时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `duration` bigint DEFAULT NULL COMMENT '流程耗时（毫秒）',
  `projection_event_id` varchar(64) DEFAULT NULL COMMENT '最近镜像投影事件ID',
  `projection_sequence` bigint NOT NULL DEFAULT '0' COMMENT '最近镜像投影顺序号',
  `action_idempotency_key` varchar(128) DEFAULT NULL COMMENT '受控流程实例动作幂等键',
  `action_request_digest` varchar(71) DEFAULT NULL COMMENT '受控流程实例动作规范请求SHA-256摘要',
  `action_type` varchar(16) DEFAULT NULL COMMENT '受控流程实例动作类型',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_flow_business_tenant_key` (`tenant_id`,`business_key`),
  KEY `idx_process_instance_id` (`process_instance_id`),
  KEY `idx_apply_user_id` (`apply_user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_sys_flow_business_tenant` (`tenant_id`,`status`,`create_time`),
  KEY `idx_flow_business_action_idempotency` (`tenant_id`,`action_idempotency_key`,`action_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程业务关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_flow_category`
--

DROP TABLE IF EXISTS `sys_flow_category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_flow_category` (
  `id` varchar(64) NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `category_code` varchar(50) NOT NULL COMMENT '分类编码',
  `category_name` varchar(100) NOT NULL COMMENT '分类名称',
  `description` varchar(500) DEFAULT NULL COMMENT '描述',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `status` tinyint DEFAULT '1' COMMENT '状态（0-禁用/1-启用）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `parent_id` varchar(64) DEFAULT NULL COMMENT '父分类ID',
  `level` int DEFAULT '1' COMMENT '层级深度',
  `ancestors` varchar(500) DEFAULT NULL COMMENT '祖先路径',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_flow_category_tenant_code` (`tenant_id`,`category_code`),
  KEY `idx_category_code` (`category_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程分类表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_flow_cc`
--

DROP TABLE IF EXISTS `sys_flow_cc`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_flow_cc` (
  `id` varchar(64) NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `process_instance_id` varchar(64) DEFAULT NULL COMMENT '流程实例ID',
  `process_def_key` varchar(100) DEFAULT NULL COMMENT '流程定义KEY',
  `task_id` varchar(64) DEFAULT NULL COMMENT '来源任务ID',
  `title` varchar(200) DEFAULT NULL COMMENT '标题',
  `content` text COMMENT '内容摘要',
  `business_key` varchar(100) DEFAULT NULL COMMENT '业务Key',
  `cc_user_id` varchar(64) NOT NULL COMMENT '抄送人ID',
  `cc_user_name` varchar(100) DEFAULT NULL COMMENT '抄送人姓名',
  `send_user_id` varchar(64) DEFAULT NULL COMMENT '发送人ID',
  `send_user_name` varchar(100) DEFAULT NULL COMMENT '发送人姓名',
  `cc_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '抄送时间',
  `read_time` datetime DEFAULT NULL COMMENT '阅读时间',
  `is_read` tinyint DEFAULT '0' COMMENT '是否已读（0-未读/1-已读）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '抄送关系状态：0有效/1已撤回',
  `revoke_by` varchar(64) DEFAULT NULL,
  `revoke_time` datetime DEFAULT NULL,
  `revoke_reason` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_process_instance_id` (`process_instance_id`),
  KEY `idx_cc_user_id` (`cc_user_id`),
  KEY `idx_flow_cc_status` (`tenant_id`,`cc_user_id`,`status`,`cc_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程抄送表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_flow_comment`
--

DROP TABLE IF EXISTS `sys_flow_comment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_flow_comment` (
  `id` varchar(64) NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `process_instance_id` varchar(64) DEFAULT NULL COMMENT '流程实例ID',
  `process_def_key` varchar(100) DEFAULT NULL COMMENT '流程定义KEY',
  `task_id` varchar(64) DEFAULT NULL COMMENT '任务ID',
  `task_name` varchar(200) DEFAULT NULL COMMENT '任务名称',
  `type` varchar(20) DEFAULT 'comment' COMMENT '类型（comment-审批意见/event-流程事件）',
  `message` text COMMENT '意见内容',
  `user_id` varchar(64) DEFAULT NULL COMMENT '用户ID',
  `user_name` varchar(100) DEFAULT NULL COMMENT '用户姓名',
  `full_message` text COMMENT '完整消息（JSON）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_process_instance_id` (`process_instance_id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_sys_flow_comment_tenant` (`tenant_id`,`process_instance_id`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程意见表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_flow_comment_phrase`
--

DROP TABLE IF EXISTS `sys_flow_comment_phrase`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_flow_comment_phrase` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `owner_type` tinyint NOT NULL DEFAULT '0' COMMENT '归属：0企业常用，1我的常用',
  `user_id` bigint NOT NULL DEFAULT '0' COMMENT '个人意见所属用户，企业意见固定0',
  `scene` varchar(16) NOT NULL DEFAULT 'ALL' COMMENT '场景：APPROVE/REJECT/ALL',
  `content` varchar(200) NOT NULL COMMENT '意见内容',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序，越小越靠前',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0禁用，1启用',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建组织',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记，删除后写当前行主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_flow_comment_phrase_content` (`tenant_id`,`owner_type`,`user_id`,`scene`,`content`,`del_flag`),
  KEY `idx_flow_comment_phrase_usable` (`tenant_id`,`status`,`owner_type`,`user_id`,`scene`,`del_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程常用审批意见';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_flow_condition_item`
--

DROP TABLE IF EXISTS `sys_flow_condition_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_flow_condition_item` (
  `id` varchar(64) NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `rule_id` varchar(64) NOT NULL COMMENT '规则ID',
  `field_name` varchar(100) NOT NULL COMMENT '字段名称',
  `field_label` varchar(100) DEFAULT NULL COMMENT '字段标签',
  `field_type` varchar(32) DEFAULT 'string' COMMENT '字段类型：string/number/date/boolean/user/dept/role',
  `operator` varchar(32) NOT NULL COMMENT '操作符：eq/ne/gt/lt/ge/le/contains/startsWith/endsWith/in/notIn/isEmpty/isNotEmpty',
  `value` text COMMENT '比较值（JSON格式，支持多值）',
  `logic_connector` varchar(10) DEFAULT 'and' COMMENT '逻辑连接符：and/or',
  `group_id` varchar(64) DEFAULT NULL COMMENT '分组ID（用于条件分组）',
  `sort_order` int DEFAULT '0' COMMENT '排序号',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_rule_id` (`rule_id`),
  KEY `idx_group_id` (`group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='条件规则项表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_flow_condition_rule`
--

DROP TABLE IF EXISTS `sys_flow_condition_rule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_flow_condition_rule` (
  `id` varchar(64) NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `rule_name` varchar(100) NOT NULL COMMENT '规则名称',
  `rule_code` varchar(100) DEFAULT NULL COMMENT '规则编码',
  `model_id` varchar(64) DEFAULT NULL COMMENT '模型ID',
  `sequence_flow_id` varchar(128) NOT NULL COMMENT '序列流ID（BPMN中的SequenceFlow ID）',
  `condition_type` varchar(32) DEFAULT 'simple' COMMENT '条件类型：simple-简单条件，composite-组合条件，script-脚本',
  `condition_expression` text COMMENT '条件表达式（JSON格式存储）',
  `priority` int DEFAULT '0' COMMENT '优先级（数字越小优先级越高）',
  `is_default` tinyint(1) DEFAULT '0' COMMENT '是否默认路径',
  `status` int DEFAULT '1' COMMENT '状态：0-禁用，1-启用',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  KEY `idx_model_id` (`model_id`),
  KEY `idx_sequence_flow_id` (`sequence_flow_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程条件规则表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_flow_entry`
--

DROP TABLE IF EXISTS `sys_flow_entry`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_flow_entry` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `entry_code` varchar(128) NOT NULL COMMENT '入口编码',
  `entry_name` varchar(128) NOT NULL COMMENT '入口名称',
  `entry_desc` varchar(500) DEFAULT NULL COMMENT '入口说明',
  `model_key` varchar(128) NOT NULL COMMENT '流程模型Key',
  `form_key` varchar(128) NOT NULL COMMENT '表单Key',
  `form_version_id` bigint NOT NULL COMMENT '表单版本ID',
  `data_mode` varchar(32) NOT NULL DEFAULT 'PROCESS_ONLY' COMMENT '数据模式',
  `object_code` varchar(64) DEFAULT NULL COMMENT '业务对象编码',
  `config_key` varchar(128) DEFAULT NULL COMMENT '动态CRUD配置Key',
  `visible_scope` json DEFAULT NULL COMMENT '可见范围配置',
  `title_template` varchar(256) DEFAULT NULL COMMENT '流程标题模板',
  `business_key_template` varchar(256) DEFAULT NULL COMMENT '业务Key模板',
  `submit_strategy` json DEFAULT NULL COMMENT '提交策略配置',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0禁用 1启用',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '删除标志：0正常 1删除',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_flow_entry_code` (`tenant_id`,`entry_code`),
  KEY `idx_flow_entry_model` (`tenant_id`,`model_key`,`status`),
  KEY `idx_flow_entry_form` (`tenant_id`,`form_key`,`form_version_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程应用入口表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_flow_entry_field_mapping`
--

DROP TABLE IF EXISTS `sys_flow_entry_field_mapping`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_flow_entry_field_mapping` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `entry_id` bigint NOT NULL COMMENT '流程入口ID',
  `form_field` varchar(128) NOT NULL COMMENT '表单字段',
  `target_type` varchar(32) NOT NULL DEFAULT 'FLOW_VARIABLE' COMMENT '目标类型：FLOW_VARIABLE/BUSINESS_FIELD',
  `target_field` varchar(128) DEFAULT NULL COMMENT '目标字段',
  `flow_variable` varchar(128) DEFAULT NULL COMMENT '流程变量名',
  `required` tinyint NOT NULL DEFAULT '0' COMMENT '是否必填映射',
  `mapping_config` json DEFAULT NULL COMMENT '映射扩展配置',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '删除标志：0正常 1删除',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_flow_entry_mapping` (`tenant_id`,`entry_id`,`form_field`,`target_type`,`target_field`),
  KEY `idx_flow_entry_mapping_entry` (`tenant_id`,`entry_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程入口字段映射表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_flow_error_log`
--

DROP TABLE IF EXISTS `sys_flow_error_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_flow_error_log` (
  `id` varchar(64) NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `process_instance_id` varchar(64) DEFAULT NULL COMMENT '流程实例ID',
  `process_def_id` varchar(64) DEFAULT NULL COMMENT '流程定义ID',
  `process_def_key` varchar(100) DEFAULT NULL COMMENT '流程定义KEY',
  `business_key` varchar(100) DEFAULT NULL COMMENT '业务Key',
  `task_id` varchar(64) DEFAULT NULL COMMENT 'Flowable任务ID',
  `task_name` varchar(200) DEFAULT NULL COMMENT '任务名称',
  `activity_id` varchar(100) DEFAULT NULL COMMENT 'BPMN节点ID',
  `activity_name` varchar(200) DEFAULT NULL COMMENT 'BPMN节点名称',
  `error_stage` varchar(80) DEFAULT NULL COMMENT '错误发生环节',
  `error_type` varchar(300) DEFAULT NULL COMMENT '异常类型',
  `error_message` text COMMENT '异常摘要',
  `stack_trace` text COMMENT '异常堆栈',
  `job_id` varchar(64) DEFAULT NULL COMMENT 'Flowable作业ID',
  `job_retries` int DEFAULT NULL COMMENT 'Flowable作业剩余重试次数',
  `status` tinyint DEFAULT '0' COMMENT '状态（0-未处理/1-已重试/2-已解决/3-重试失败）',
  `retry_count` int DEFAULT '0' COMMENT '人工重试次数',
  `last_retry_user_id` varchar(64) DEFAULT NULL COMMENT '最近重试人',
  `last_retry_time` datetime DEFAULT NULL COMMENT '最近重试时间',
  `retry_message` text COMMENT '重试说明或失败原因',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_process_instance_id` (`process_instance_id`),
  KEY `idx_activity_id` (`activity_id`),
  KEY `idx_job_id` (`job_id`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程运行错误日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_flow_fill_batch`
--

DROP TABLE IF EXISTS `sys_flow_fill_batch`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_flow_fill_batch` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `entry_id` bigint NOT NULL COMMENT '流程入口ID',
  `entry_code` varchar(128) NOT NULL COMMENT '入口编码',
  `batch_name` varchar(128) NOT NULL COMMENT '批次名称',
  `period_key` varchar(64) DEFAULT NULL COMMENT '周期标识',
  `target_scope` json DEFAULT NULL COMMENT '目标组织范围',
  `owner_rule` json DEFAULT NULL COMMENT '负责人规则',
  `deadline_time` datetime DEFAULT NULL COMMENT '截止时间',
  `allow_resubmit` tinyint NOT NULL DEFAULT '1' COMMENT '驳回后是否允许重新提交',
  `status` varchar(32) NOT NULL DEFAULT 'DRAFT' COMMENT '批次状态：DRAFT/PUBLISHED/CLOSED',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '删除标志：0正常 1删除',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_flow_fill_batch_entry` (`tenant_id`,`entry_code`,`status`,`deadline_time`),
  KEY `idx_flow_fill_batch_period` (`tenant_id`,`period_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程组织批量填报批次表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_flow_fill_batch_item`
--

DROP TABLE IF EXISTS `sys_flow_fill_batch_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_flow_fill_batch_item` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `batch_id` bigint NOT NULL COMMENT '批次ID',
  `entry_code` varchar(128) NOT NULL COMMENT '入口编码',
  `org_id` bigint NOT NULL COMMENT '组织ID',
  `org_name` varchar(128) DEFAULT NULL COMMENT '组织名称',
  `owner_user_id` bigint DEFAULT NULL COMMENT '负责人用户ID',
  `owner_user_name` varchar(100) DEFAULT NULL COMMENT '负责人姓名',
  `form_instance_id` bigint DEFAULT NULL COMMENT '表单实例ID',
  `object_code` varchar(64) DEFAULT NULL COMMENT '业务对象编码',
  `record_id` bigint DEFAULT NULL COMMENT '业务记录ID',
  `process_instance_id` varchar(128) DEFAULT NULL COMMENT '流程实例ID',
  `submit_status` varchar(32) NOT NULL DEFAULT 'PENDING' COMMENT '填报状态：PENDING/SUBMITTED/RETURNED/OVERDUE',
  `flow_status` varchar(32) DEFAULT NULL COMMENT '流程状态',
  `deadline_time` datetime DEFAULT NULL COMMENT '截止时间',
  `submit_time` datetime DEFAULT NULL COMMENT '提交时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '删除标志：0正常 1删除',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_flow_fill_batch_org` (`tenant_id`,`batch_id`,`org_id`),
  KEY `idx_flow_fill_item_owner` (`tenant_id`,`owner_user_id`,`submit_status`),
  KEY `idx_flow_fill_item_process` (`tenant_id`,`process_instance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程组织批量填报明细表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_flow_form`
--

DROP TABLE IF EXISTS `sys_flow_form`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_flow_form` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `form_key` varchar(100) NOT NULL COMMENT '表单Key（唯一标识）',
  `form_name` varchar(200) NOT NULL COMMENT '表单名称',
  `form_category` varchar(64) DEFAULT NULL COMMENT '表单分类',
  `form_type` varchar(50) DEFAULT 'dynamic' COMMENT '表单类型（dynamic-动态表单/external-外部表单/builtin-内置表单）',
  `form_schema` text COMMENT '表单Schema（JSON格式）',
  `field_registry` json DEFAULT NULL COMMENT '字段目录快照',
  `form_url` varchar(500) DEFAULT NULL COMMENT '外部表单URL（formType为external时使用）',
  `component_path` varchar(200) DEFAULT NULL COMMENT '内置表单组件路径（formType为builtin时使用）',
  `form_config` text COMMENT '表单配置（JSON格式，包含校验规则、事件等）',
  `default_data_mode` varchar(32) NOT NULL DEFAULT 'PROCESS_ONLY' COMMENT '默认数据模式',
  `version` int DEFAULT '1' COMMENT '版本号',
  `status` tinyint DEFAULT '1' COMMENT '状态（0-禁用/1-启用）',
  `publish_status` tinyint NOT NULL DEFAULT '0' COMMENT '发布状态：0草稿 1已发布',
  `current_version_id` bigint DEFAULT NULL COMMENT '当前发布版本ID',
  `description` varchar(500) DEFAULT NULL COMMENT '描述',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT '0' COMMENT '删除标志（0-正常/1-删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_flow_form_tenant_key` (`tenant_id`,`form_key`),
  KEY `idx_form_key` (`form_key`),
  KEY `idx_form_name` (`form_name`),
  KEY `idx_status` (`status`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程表单定义表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_flow_form_instance`
--

DROP TABLE IF EXISTS `sys_flow_form_instance`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_flow_form_instance` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `entry_id` bigint DEFAULT NULL COMMENT '流程入口ID',
  `entry_code` varchar(128) NOT NULL COMMENT '入口编码',
  `business_key` varchar(256) NOT NULL COMMENT '流程业务Key',
  `process_instance_id` varchar(128) DEFAULT NULL COMMENT '流程实例ID',
  `model_key` varchar(128) NOT NULL COMMENT '流程模型Key',
  `form_key` varchar(128) NOT NULL COMMENT '表单Key',
  `form_version_id` bigint NOT NULL COMMENT '表单版本ID',
  `form_version` int NOT NULL COMMENT '表单版本号',
  `schema_snapshot` longtext COMMENT '表单Schema快照',
  `field_registry` json DEFAULT NULL COMMENT '字段目录快照',
  `form_data` json DEFAULT NULL COMMENT '表单填报数据',
  `data_mode` varchar(32) NOT NULL DEFAULT 'PROCESS_ONLY' COMMENT '数据模式',
  `object_code` varchar(64) DEFAULT NULL COMMENT '业务对象编码',
  `record_id` bigint DEFAULT NULL COMMENT '业务记录ID',
  `title` varchar(256) DEFAULT NULL COMMENT '流程标题',
  `start_user_id` bigint DEFAULT NULL COMMENT '发起人ID',
  `start_user_name` varchar(100) DEFAULT NULL COMMENT '发起人姓名',
  `start_dept_id` bigint DEFAULT NULL COMMENT '发起部门ID',
  `start_dept_name` varchar(128) DEFAULT NULL COMMENT '发起部门名称',
  `status` varchar(32) NOT NULL DEFAULT 'RUNNING' COMMENT '状态：DRAFT/RUNNING/APPROVED/REJECTED/CANCELED/TERMINATED',
  `submit_time` datetime DEFAULT NULL COMMENT '提交时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `projection_event_id` varchar(64) DEFAULT NULL COMMENT '最近镜像投影事件ID',
  `projection_sequence` bigint NOT NULL DEFAULT '0' COMMENT '最近镜像投影顺序号',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '删除标志：0正常 1删除',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_flow_form_instance_business` (`tenant_id`,`business_key`),
  KEY `idx_flow_form_instance_process` (`tenant_id`,`process_instance_id`),
  KEY `idx_flow_form_instance_entry` (`tenant_id`,`entry_code`,`status`,`create_time`),
  KEY `idx_flow_form_instance_record` (`tenant_id`,`object_code`,`record_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程表单填报实例表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_flow_model`
--

DROP TABLE IF EXISTS `sys_flow_model`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_flow_model` (
  `id` varchar(64) NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `model_key` varchar(100) NOT NULL COMMENT '模型标识',
  `model_name` varchar(200) NOT NULL COMMENT '模型名称',
  `description` varchar(500) DEFAULT NULL COMMENT '描述',
  `category` varchar(100) DEFAULT NULL COMMENT '分类',
  `flow_type` varchar(50) DEFAULT NULL COMMENT '流程类型（leave-请假/expense-报销/approval-审批）',
  `designer_type` varchar(32) NOT NULL DEFAULT 'approval' COMMENT '设计器类型：approval-审批流程/business-业务流程',
  `form_type` varchar(50) DEFAULT 'dynamic' COMMENT '表单类型（dynamic-动态表单/custom-业务表单）',
  `form_id` varchar(64) DEFAULT NULL COMMENT '表单ID（业务表单时使用）',
  `form_json` text COMMENT '动态表单JSON配置',
  `version` int DEFAULT '1' COMMENT '版本号',
  `process_definition_id` varchar(64) DEFAULT NULL COMMENT 'Flowable流程定义ID',
  `deployment_id` varchar(64) DEFAULT NULL COMMENT 'Flowable部署ID',
  `deployment_key` varchar(100) DEFAULT NULL COMMENT '部署KEY（发布后生成）',
  `status` tinyint DEFAULT '0' COMMENT '状态（0-设计/1-已发布/2-禁用）',
  `importance_level` int NOT NULL DEFAULT '1' COMMENT '重要性等级（1-普通/2-重要）',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '模型目录排序值，数值越小越靠前',
  `deploy_time` datetime DEFAULT NULL COMMENT '发布时间',
  `last_update_by` varchar(64) DEFAULT NULL COMMENT '最后修改人',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint DEFAULT '0' COMMENT '删除标志（0-正常/1-删除）',
  `bpmn_xml` longtext,
  `notify_type` varchar(30) DEFAULT 'redis' COMMENT '事件通知方式：none-不通知 / redis-Redis Pub/Sub / webhook-HTTP Webhook（互斥）',
  `webhook_url` varchar(500) DEFAULT NULL COMMENT 'Webhook 回调地址，仅 notify_type=webhook 时生效',
  `todo_detail_url_template` varchar(512) DEFAULT NULL COMMENT '待办卡片详情深链模板，支持占位符 {taskId}/{businessKey}/{processInstanceId}，可填相对路径或完整URL',
  `allow_multi_return` tinyint NOT NULL DEFAULT '0' COMMENT '是否允许多级指定节点退回',
  `notify_config` json DEFAULT NULL COMMENT '通知配置（事件×渠道矩阵）：todo/result/cc -> {channels:[WEB/EMAIL/SMS/COLLABORATION], templateCode:模板编码覆盖}，为空时走默认通知行为',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_flow_model_tenant_key` (`tenant_id`,`model_key`),
  KEY `idx_model_key` (`model_key`),
  KEY `idx_category` (`category`),
  KEY `idx_status` (`status`),
  KEY `idx_sys_flow_model_tenant` (`tenant_id`,`status`,`create_time`),
  KEY `idx_flow_model_tenant_sort` (`tenant_id`,`del_flag`,`sort_order`,`create_time`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程模型表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_flow_node_config`
--

DROP TABLE IF EXISTS `sys_flow_node_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_flow_node_config` (
  `id` varchar(64) NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `model_id` varchar(64) NOT NULL COMMENT '流程模型ID',
  `node_id` varchar(100) NOT NULL COMMENT '节点ID',
  `node_name` varchar(200) DEFAULT NULL COMMENT '节点名称',
  `node_type` varchar(50) DEFAULT 'approval' COMMENT '节点类型',
  `assignee_type` varchar(50) DEFAULT NULL COMMENT '审批人类型(user/role/dept/post/expr)',
  `assignee_value` text COMMENT '审批人值',
  `assignee_expr` varchar(500) DEFAULT NULL COMMENT '审批人表达式',
  `multi_instance_type` varchar(50) DEFAULT 'none' COMMENT '会签类型(none/sequential/parallel)',
  `completion_condition` varchar(500) DEFAULT NULL COMMENT '完成条件',
  `pass_rate` decimal(5,2) DEFAULT NULL COMMENT '通过比例',
  `due_date_days` int DEFAULT NULL COMMENT '超时天数',
  `due_date_hours` int DEFAULT NULL COMMENT '超时小时数',
  `timeout_action` varchar(50) DEFAULT NULL COMMENT '超时动作(auto_pass/auto_reject/notify)',
  `allow_approve` tinyint DEFAULT '1' COMMENT '允许通过',
  `allow_delegate` tinyint DEFAULT '1' COMMENT '允许转办',
  `allow_transfer` tinyint DEFAULT '1' COMMENT '允许转交',
  `allow_add_sign` tinyint DEFAULT '0' COMMENT '允许加签',
  `allow_counter_sign` tinyint DEFAULT '0' COMMENT '允许减签',
  `allow_return` tinyint DEFAULT '0' COMMENT '允许退回上一审批节点',
  `allow_terminate` tinyint DEFAULT '0' COMMENT '允许终结流程',
  `require_signature` tinyint DEFAULT '0' COMMENT '办理时需要签名',
  `require_comment` tinyint DEFAULT '1' COMMENT '办理时需要审批意见',
  `form_permission` text COMMENT '表单字段权限配置JSON',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `timeout_notify_users` text,
  `allow_reject` varchar(10) DEFAULT NULL,
  `allow_reject_to_start` varchar(10) DEFAULT NULL,
  `allow_withdraw` varchar(10) DEFAULT NULL,
  `form_key` varchar(30) DEFAULT NULL,
  `priority` int DEFAULT NULL,
  `task_listeners` text,
  `ext_config` varchar(100) DEFAULT NULL,
  `status` varchar(10) DEFAULT NULL,
  `last_update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '最后修改人',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '创建人',
  `del_flag` varchar(64) NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  `update_by` varchar(20) DEFAULT NULL,
  `overdue_reminder_enabled` tinyint NOT NULL DEFAULT '0' COMMENT '是否启用逾期提醒',
  `overdue_reminder_template_code` varchar(50) DEFAULT NULL COMMENT '逾期提醒消息模板编码',
  `overdue_reminder_channels` varchar(200) DEFAULT NULL COMMENT '逾期提醒推送渠道，逗号分隔',
  `overdue_reminder_repeat_mode` varchar(32) NOT NULL DEFAULT 'once' COMMENT '逾期提醒重复策略：once/interval',
  `overdue_reminder_interval_minutes` int NOT NULL DEFAULT '1440' COMMENT '重复提醒间隔分钟',
  `overdue_reminder_max_times` int NOT NULL DEFAULT '1' COMMENT '最大提醒次数',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_model_node_active` (`model_id`,`node_id`,`del_flag`),
  KEY `idx_model_id` (`model_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='审批节点配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_flow_notify_outbox`
--

DROP TABLE IF EXISTS `sys_flow_notify_outbox`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_flow_notify_outbox` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '全局投递顺序号',
  `tenant_id` bigint NOT NULL COMMENT '业务租户ID',
  `event_id` varchar(64) NOT NULL COMMENT '稳定事件ID',
  `event_version` int NOT NULL DEFAULT '1' COMMENT '事件协议版本',
  `occurred_at` datetime(3) NOT NULL COMMENT '事件产生时间',
  `event_type` varchar(32) NOT NULL COMMENT '通知事件类型',
  `aggregate_type` varchar(32) NOT NULL COMMENT '聚合类型',
  `aggregate_id` varchar(128) NOT NULL COMMENT '流程实例/任务等聚合ID',
  `payload` longtext NOT NULL COMMENT '通知快照JSON',
  `payload_hash` char(64) NOT NULL COMMENT '通知快照SHA-256摘要',
  `delivery_status` tinyint NOT NULL DEFAULT '0' COMMENT '0待投递 1投递中 2成功 3待重试 4死信',
  `retry_count` int NOT NULL DEFAULT '0' COMMENT '已认领投递次数',
  `next_retry_time` datetime DEFAULT NULL COMMENT '下次允许重试时间',
  `lock_owner` varchar(64) DEFAULT NULL COMMENT '当前认领节点',
  `lock_time` datetime DEFAULT NULL COMMENT '认领时间',
  `delivered_time` datetime DEFAULT NULL COMMENT '投递成功时间',
  `last_error` varchar(128) DEFAULT NULL COMMENT '最近失败类型，不保存异常原文',
  `replay_count` int NOT NULL DEFAULT '0' COMMENT '人工重放次数',
  `replayed_by` varchar(64) DEFAULT NULL COMMENT '最近人工重放操作人',
  `replayed_time` datetime DEFAULT NULL COMMENT '最近人工重放时间',
  `replay_reason` varchar(500) DEFAULT NULL COMMENT '最近人工重放原因',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_dept` bigint DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_flow_notify_outbox_event` (`tenant_id`,`event_id`),
  KEY `idx_flow_notify_outbox_dispatch` (`delivery_status`,`next_retry_time`,`lock_time`,`id`),
  KEY `idx_flow_notify_outbox_aggregate` (`tenant_id`,`aggregate_type`,`aggregate_id`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程通知事务Outbox';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_flow_overdue_reminder_record`
--

DROP TABLE IF EXISTS `sys_flow_overdue_reminder_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_flow_overdue_reminder_record` (
  `id` varchar(64) NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `task_id` varchar(64) NOT NULL COMMENT 'Flowable任务ID',
  `process_instance_id` varchar(64) DEFAULT NULL COMMENT '流程实例ID',
  `process_def_key` varchar(100) DEFAULT NULL COMMENT '流程定义KEY',
  `task_def_key` varchar(100) DEFAULT NULL COMMENT '任务定义Key',
  `reminder_key` varchar(160) NOT NULL COMMENT '提醒批次键',
  `channel` varchar(20) NOT NULL COMMENT '推送渠道',
  `template_code` varchar(50) DEFAULT NULL COMMENT '消息模板编码',
  `receiver_user_ids` text COMMENT '接收人用户ID集合',
  `message_id` bigint DEFAULT NULL COMMENT '消息ID',
  `send_status` tinyint NOT NULL DEFAULT '0' COMMENT '发送状态：0-待发送/1-成功/2-失败',
  `send_time` datetime DEFAULT NULL COMMENT '发送时间',
  `error_message` varchar(1000) DEFAULT NULL COMMENT '失败原因',
  `retry_count` int NOT NULL DEFAULT '0',
  `next_retry_time` datetime DEFAULT NULL,
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_flow_overdue_reminder` (`tenant_id`,`reminder_key`,`channel`),
  KEY `idx_flow_overdue_task` (`tenant_id`,`task_id`,`send_time`),
  KEY `idx_flow_overdue_status` (`tenant_id`,`send_status`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程任务逾期提醒记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_flow_projection_outbox`
--

DROP TABLE IF EXISTS `sys_flow_projection_outbox`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_flow_projection_outbox` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '全局投影顺序号',
  `tenant_id` bigint NOT NULL COMMENT '业务租户ID',
  `event_id` varchar(64) NOT NULL COMMENT '唯一事件ID',
  `event_version` int NOT NULL DEFAULT '1' COMMENT '事件协议版本',
  `occurred_at` datetime(3) NOT NULL COMMENT '事件产生时间',
  `event_type` varchar(32) NOT NULL COMMENT '投影事件类型',
  `aggregate_type` varchar(32) NOT NULL COMMENT 'TASK/PROCESS',
  `aggregate_id` varchar(128) NOT NULL COMMENT '任务或流程实例ID',
  `payload` longtext NOT NULL COMMENT '不可变投影快照JSON',
  `payload_hash` char(64) NOT NULL COMMENT '投影快照SHA-256摘要',
  `projection_status` tinyint NOT NULL DEFAULT '0' COMMENT '0待处理 1处理中 2成功 3待重试 4死信',
  `retry_count` int NOT NULL DEFAULT '0' COMMENT '已认领次数',
  `next_retry_time` datetime DEFAULT NULL COMMENT '下次允许重试时间',
  `lock_owner` varchar(64) DEFAULT NULL COMMENT '当前认领节点',
  `lock_time` datetime DEFAULT NULL COMMENT '认领时间',
  `applied_time` datetime DEFAULT NULL COMMENT '应用成功时间',
  `last_error` varchar(128) DEFAULT NULL COMMENT '最近失败类型，不保存异常原文',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_dept` bigint DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_flow_projection_event` (`tenant_id`,`event_id`),
  KEY `idx_flow_projection_dispatch` (`projection_status`,`next_retry_time`,`lock_time`,`id`),
  KEY `idx_flow_projection_aggregate` (`tenant_id`,`aggregate_type`,`aggregate_id`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Flowable本地镜像投影Outbox';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_flow_record_participant`
--

DROP TABLE IF EXISTS `sys_flow_record_participant`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_flow_record_participant` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `business_type` varchar(128) NOT NULL COMMENT '业务对象编码',
  `business_id` varchar(64) NOT NULL COMMENT '业务记录ID',
  `user_id` varchar(64) NOT NULL COMMENT '参与用户ID',
  `relation_type` varchar(32) NOT NULL COMMENT 'INITIATOR/ASSIGNEE/CC',
  `process_instance_id` varchar(64) DEFAULT NULL COMMENT '流程实例ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_flow_record_participant` (`tenant_id`,`business_type`,`business_id`,`user_id`,`relation_type`),
  KEY `idx_flow_participant_user` (`tenant_id`,`user_id`,`business_type`),
  KEY `idx_flow_participant_record` (`tenant_id`,`business_type`,`business_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程单据经手人索引';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_flow_spel_template`
--

DROP TABLE IF EXISTS `sys_flow_spel_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_flow_spel_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `template_name` varchar(100) NOT NULL COMMENT '模板名称',
  `template_code` varchar(50) NOT NULL COMMENT '模板编码',
  `expression` varchar(500) NOT NULL COMMENT 'SPEL表达式',
  `description` varchar(200) DEFAULT NULL COMMENT '描述说明',
  `category` varchar(50) DEFAULT 'general' COMMENT '分类：general/dept/role/region/custom',
  `example_params` varchar(500) DEFAULT NULL COMMENT '示例参数（JSON格式）',
  `status` int NOT NULL DEFAULT '1' COMMENT '状态：0-禁用，1-启用',
  `sort` int DEFAULT '100' COMMENT '排序',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` int DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_template_code` (`tenant_id`,`template_code`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_category` (`category`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程SPEL表达式模板';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_flow_statistics`
--

DROP TABLE IF EXISTS `sys_flow_statistics`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_flow_statistics` (
  `id` varchar(64) NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `process_def_key` varchar(100) NOT NULL COMMENT '流程定义KEY',
  `stat_date` date NOT NULL COMMENT '统计日期',
  `total_instances` int DEFAULT '0' COMMENT '总实例数',
  `running_instances` int DEFAULT '0' COMMENT '运行中实例数',
  `completed_instances` int DEFAULT '0' COMMENT '已完成实例数',
  `terminated_instances` int DEFAULT '0' COMMENT '已终止实例数',
  `avg_duration` bigint DEFAULT NULL COMMENT '平均耗时(毫秒)',
  `max_duration` bigint DEFAULT NULL COMMENT '最大耗时',
  `min_duration` bigint DEFAULT NULL COMMENT '最小耗时',
  `total_tasks` int DEFAULT '0' COMMENT '总任务数',
  `avg_task_duration` bigint DEFAULT NULL COMMENT '平均任务耗时',
  `timeout_tasks` int DEFAULT '0' COMMENT '超时任务数',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_flow_statistics_tenant_date` (`tenant_id`,`process_def_key`,`stat_date`),
  KEY `idx_stat_date` (`stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程监控统计表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_flow_task`
--

DROP TABLE IF EXISTS `sys_flow_task`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_flow_task` (
  `id` varchar(64) NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `task_id` varchar(64) DEFAULT NULL COMMENT 'Flowable任务ID',
  `task_name` varchar(200) DEFAULT NULL COMMENT '任务名称',
  `task_def_key` varchar(100) DEFAULT NULL COMMENT '任务定义Key',
  `task_def_id` varchar(100) DEFAULT NULL COMMENT '任务定义ID',
  `process_instance_id` varchar(64) DEFAULT NULL COMMENT '流程实例ID',
  `process_def_id` varchar(64) DEFAULT NULL COMMENT '流程定义ID',
  `process_def_key` varchar(100) DEFAULT NULL COMMENT '流程定义KEY',
  `business_key` varchar(100) DEFAULT NULL COMMENT '业务Key',
  `business_type` varchar(50) DEFAULT NULL COMMENT '业务类型',
  `title` varchar(200) NOT NULL COMMENT '任务标题',
  `assignee` varchar(64) DEFAULT NULL COMMENT '处理人（签收后）',
  `assignee_name` varchar(100) DEFAULT NULL COMMENT '处理人姓名',
  `candidate_users` varchar(1000) DEFAULT NULL COMMENT '候选人（逗号分隔）',
  `candidate_groups` varchar(1000) DEFAULT NULL COMMENT '候选组（逗号分隔）',
  `owner` varchar(64) DEFAULT NULL COMMENT '任务拥有人',
  `due_date` datetime DEFAULT NULL COMMENT '截止日期',
  `priority` int DEFAULT '50' COMMENT '优先级（0-100）',
  `status` tinyint DEFAULT '0' COMMENT '状态（0-待办/1-已签收/2-已通过/3-已驳回/4-已转办/5-已委派/6-已撤回）',
  `comment` text COMMENT '审批意见',
  `signature` text COMMENT '审批签名',
  `attachment_urls` text COMMENT '附件URL（逗号分隔）',
  `start_user_id` varchar(64) DEFAULT NULL COMMENT '流程发起人',
  `start_user_name` varchar(100) DEFAULT NULL COMMENT '发起人姓名',
  `start_dept_id` varchar(64) DEFAULT NULL COMMENT '发起部门ID',
  `start_dept_name` varchar(100) DEFAULT NULL COMMENT '发起部门名称',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `claim_time` datetime DEFAULT NULL COMMENT '签收时间',
  `complete_time` datetime DEFAULT NULL COMMENT '完成时间',
  `action_idempotency_key` varchar(128) DEFAULT NULL COMMENT '受控流程动作幂等键',
  `action_request_digest` varchar(71) DEFAULT NULL COMMENT '受控流程动作规范请求SHA-256摘要',
  `action_type` varchar(16) DEFAULT NULL COMMENT '受控流程动作类型APPROVE/REJECT',
  `projection_event_id` varchar(64) DEFAULT NULL COMMENT '最近镜像投影事件ID',
  `projection_sequence` bigint NOT NULL DEFAULT '0' COMMENT '最近镜像投影顺序号',
  PRIMARY KEY (`id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_process_instance_id` (`process_instance_id`),
  KEY `idx_assignee` (`assignee`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_sys_flow_task_tenant` (`tenant_id`,`status`,`create_time`),
  KEY `idx_flow_task_action_idempotency` (`tenant_id`,`action_idempotency_key`,`action_type`),
  KEY `idx_flow_task_tenant_assignee_status_create` (`tenant_id`,`assignee`,`status`,`create_time`),
  KEY `idx_flow_task_tenant_assignee_status_complete` (`tenant_id`,`assignee`,`status`,`complete_time`),
  KEY `idx_flow_task_tenant_start_user_create` (`tenant_id`,`start_user_id`,`create_time`),
  KEY `idx_flow_task_tenant_status_create` (`tenant_id`,`status`,`create_time`),
  KEY `idx_flow_task_timeout_cursor` (`due_date`,`id`,`status`,`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程任务表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_flow_task_candidate`
--

DROP TABLE IF EXISTS `sys_flow_task_candidate`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_flow_task_candidate` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `task_id` varchar(64) NOT NULL COMMENT 'Flowable任务ID',
  `parent_task_id` varchar(64) DEFAULT NULL COMMENT '动态加签父任务ID',
  `child_task_id` varchar(64) DEFAULT NULL COMMENT '动态加签子任务ID',
  `child_execution_id` varchar(64) DEFAULT NULL COMMENT '动态加签子执行ID',
  `process_instance_id` varchar(64) DEFAULT NULL COMMENT '流程实例ID',
  `candidate_type` varchar(16) NOT NULL COMMENT 'USER/GROUP',
  `candidate_value` varchar(128) NOT NULL COMMENT '用户ID或候选组标识',
  `source` varchar(32) NOT NULL DEFAULT 'FLOWABLE' COMMENT 'FLOWABLE/DYNAMIC_SIGN',
  `sign_mode` varchar(16) DEFAULT NULL COMMENT '加签模式',
  `operator_id` varchar(64) DEFAULT NULL COMMENT '关系操作人',
  `reason` varchar(500) DEFAULT NULL COMMENT '加签/减签原因',
  `idempotency_key` varchar(128) DEFAULT NULL COMMENT '动态加签/减签幂等键',
  `request_digest` varchar(71) DEFAULT NULL COMMENT '动态加签/减签规范请求摘要',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '1有效/0已移除',
  `projection_event_id` varchar(64) DEFAULT NULL COMMENT '最近镜像投影事件ID',
  `projection_sequence` bigint NOT NULL DEFAULT '0' COMMENT '最近镜像投影顺序号',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_flow_task_candidate` (`tenant_id`,`task_id`,`candidate_type`,`candidate_value`),
  KEY `idx_flow_task_candidate_lookup` (`tenant_id`,`candidate_type`,`candidate_value`,`status`),
  KEY `idx_flow_task_candidate_task` (`tenant_id`,`task_id`,`status`),
  KEY `idx_flow_task_candidate_parent` (`tenant_id`,`parent_task_id`,`candidate_type`,`source`),
  KEY `idx_flow_task_candidate_idempotency` (`tenant_id`,`task_id`,`candidate_type`,`idempotency_key`),
  KEY `idx_flow_task_candidate_child` (`tenant_id`,`child_task_id`,`status`),
  KEY `idx_flow_task_candidate_visibility` (`tenant_id`,`status`,`candidate_type`,`candidate_value`,`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程任务候选关系';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_flow_template`
--

DROP TABLE IF EXISTS `sys_flow_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_flow_template` (
  `id` varchar(64) NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `template_key` varchar(100) NOT NULL COMMENT '模板标识',
  `template_name` varchar(200) NOT NULL COMMENT '模板名称',
  `category` varchar(50) DEFAULT NULL COMMENT '分类',
  `description` varchar(500) DEFAULT NULL COMMENT '描述',
  `icon` varchar(100) DEFAULT NULL COMMENT '图标',
  `form_type` varchar(50) DEFAULT NULL COMMENT '表单类型',
  `form_json` text COMMENT '表单JSON',
  `bpmn_xml` text COMMENT 'BPMN流程XML',
  `thumbnail` varchar(500) DEFAULT NULL COMMENT '缩略图',
  `variables` text COMMENT '流程变量定义（JSON）',
  `version` int DEFAULT '1' COMMENT '版本',
  `status` tinyint DEFAULT '1' COMMENT '状态（0-禁用/1-启用）',
  `usage_count` int DEFAULT '0' COMMENT '使用次数',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标志：0正常 1删除',
  `is_system` varchar(10) DEFAULT NULL,
  `sort_order` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_flow_template_tenant_key` (`tenant_id`,`template_key`),
  KEY `idx_template_key` (`template_key`),
  KEY `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程模板表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_flow_user_group`
--

DROP TABLE IF EXISTS `sys_flow_user_group`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_flow_user_group` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `group_code` varchar(100) NOT NULL COMMENT '用户组编码，作为 candidateGroups 稳定引用',
  `group_name` varchar(100) NOT NULL COMMENT '用户组名称',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0禁用，1启用',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建组织',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记，删除后写当前行主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_flow_user_group_code` (`tenant_id`,`group_code`,`del_flag`),
  KEY `idx_flow_user_group_status` (`tenant_id`,`status`,`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程用户组';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_flow_user_group_member`
--

DROP TABLE IF EXISTS `sys_flow_user_group_member`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_flow_user_group_member` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `group_id` bigint NOT NULL COMMENT '用户组ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0失效，1有效',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建组织',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记，删除后写当前行主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_flow_user_group_member` (`tenant_id`,`group_id`,`user_id`,`del_flag`),
  KEY `idx_flow_user_group_member_user` (`tenant_id`,`user_id`,`status`,`del_flag`),
  KEY `idx_flow_user_group_member_group` (`tenant_id`,`group_id`,`status`,`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程用户组成员';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_id_sequence`
--

DROP TABLE IF EXISTS `sys_id_sequence`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_id_sequence` (
  `biz_key` varchar(100) NOT NULL COMMENT '业务维度唯一键',
  `max_id` bigint NOT NULL DEFAULT '0' COMMENT '当前已分配的最大ID',
  `step` int NOT NULL DEFAULT '1000' COMMENT '步长',
  `version` int NOT NULL DEFAULT '0' COMMENT '版本（乐观锁）',
  `reset_policy` varchar(20) DEFAULT 'NONE' COMMENT '重置策略：NONE/DAILY/HOURLY',
  `seq_length` int DEFAULT '8' COMMENT '序列长度（左侧补零）',
  `prefix` varchar(50) DEFAULT NULL COMMENT '前缀',
  PRIMARY KEY (`biz_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='分布式ID序列配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_job_api_idempotency`
--

DROP TABLE IF EXISTS `sys_job_api_idempotency`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_job_api_idempotency` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `token_id` bigint NOT NULL COMMENT '服务账号Token ID',
  `job_config_id` bigint NOT NULL COMMENT '任务配置ID',
  `idempotency_key_hash` char(64) NOT NULL COMMENT '幂等键SHA-256 Hash',
  `execution_id` bigint NOT NULL COMMENT '预留执行记录ID',
  `expires_at` datetime NOT NULL COMMENT '幂等记录过期时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_job_api_idempotency_active` (`tenant_id`,`token_id`,`job_config_id`,`idempotency_key_hash`,`del_flag`),
  KEY `idx_job_api_idempotency_execution` (`execution_id`),
  KEY `idx_job_api_idempotency_expires_at` (`expires_at`,`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='定时任务开放API幂等记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_job_api_token`
--

DROP TABLE IF EXISTS `sys_job_api_token`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_job_api_token` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `caller_name` varchar(100) NOT NULL COMMENT '调用方名称',
  `caller_description` varchar(500) DEFAULT NULL COMMENT '调用方说明',
  `token_key_id` varchar(22) NOT NULL COMMENT 'Token不可猜测Key ID',
  `token_prefix` varchar(32) NOT NULL COMMENT 'Token展示前缀',
  `token_hash` char(64) NOT NULL COMMENT 'Token HMAC-SHA256 Hash',
  `scopes` varchar(255) NOT NULL COMMENT '空格分隔Scope',
  `resource_job_ids` json NOT NULL COMMENT '允许访问的任务ID JSON数组',
  `resource_job_groups` json NOT NULL COMMENT '允许访问的任务组 JSON数组',
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/REVOKED',
  `issued_at` datetime NOT NULL COMMENT '签发时间',
  `expires_at` datetime NOT NULL COMMENT '过期时间',
  `last_used_at` datetime DEFAULT NULL COMMENT '最后使用时间',
  `revoked_at` datetime DEFAULT NULL COMMENT '吊销时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_job_api_token_key_active` (`token_key_id`,`del_flag`),
  KEY `idx_job_api_token_tenant_status` (`tenant_id`,`status`,`del_flag`),
  KEY `idx_job_api_token_expires_at` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='定时任务开放API服务账号Token';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_job_config`
--

DROP TABLE IF EXISTS `sys_job_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_job_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `job_name` varchar(200) NOT NULL COMMENT '任务名称',
  `job_group` varchar(200) NOT NULL DEFAULT 'DEFAULT' COMMENT '任务分组',
  `description` varchar(500) DEFAULT NULL COMMENT '任务描述',
  `executor_bean` varchar(200) DEFAULT NULL COMMENT '执行器Bean名称',
  `executor_method` varchar(200) DEFAULT NULL COMMENT '执行器方法名',
  `executor_handler` varchar(200) DEFAULT NULL COMMENT '执行器Handler',
  `executor_service` varchar(200) DEFAULT NULL COMMENT '执行器服务名（RPC模式）',
  `schedule_type` varchar(20) NOT NULL DEFAULT 'CRON' COMMENT '调度类型：CRON/ONCE',
  `cron_expression` varchar(100) DEFAULT NULL COMMENT 'Cron表达式',
  `fire_once_time` datetime DEFAULT NULL COMMENT '一次性任务本地触发时间',
  `timezone` varchar(64) NOT NULL DEFAULT 'Asia/Shanghai' COMMENT 'IANA时区',
  `job_param` text COMMENT '任务参数',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0-停止 1-运行',
  `execute_mode` varchar(20) NOT NULL DEFAULT 'HANDLER' COMMENT '执行模式：BEAN/HANDLER',
  `invoke_mode` varchar(16) NOT NULL DEFAULT 'SINGLE' COMMENT '调用方式：SINGLE/FLOW',
  `flow_model_key` varchar(100) DEFAULT NULL COMMENT '已发布流程模型Key',
  `flow_model_version` int DEFAULT NULL COMMENT '保存时固定的流程模型版本',
  `flow_deployment_id` varchar(64) DEFAULT NULL COMMENT '保存时固定的Flowable部署ID',
  `flow_process_definition_id` varchar(64) DEFAULT NULL COMMENT '保存时固定的Flowable流程定义ID',
  `concurrent_policy` varchar(32) NOT NULL DEFAULT 'ALLOW' COMMENT '并发策略：ALLOW/SKIP_IF_RUNNING',
  `misfire_policy` varchar(32) NOT NULL DEFAULT 'DO_NOTHING' COMMENT '错过触发策略：FIRE_ONCE_NOW/DO_NOTHING',
  `idempotent_flag` tinyint NOT NULL DEFAULT '0' COMMENT '是否允许幂等重试：0否 1是',
  `retry_count` int DEFAULT '0' COMMENT '失败重试次数',
  `consecutive_failures` int NOT NULL DEFAULT '0' COMMENT '连续失败次数',
  `last_completion_time` datetime DEFAULT NULL COMMENT '最近推进失败统计的执行完成时间',
  `last_completion_execution_id` bigint DEFAULT NULL COMMENT '最近推进失败统计的执行ID',
  `alarm_enabled` tinyint NOT NULL DEFAULT '0' COMMENT '是否启用失败告警：0否 1是',
  `alarm_channels` varchar(64) DEFAULT NULL COMMENT '失败告警渠道：WEB,EMAIL',
  `alarm_recipient_user_ids` varchar(2000) DEFAULT NULL COMMENT '站内信接收用户ID，逗号分隔',
  `alarm_email` varchar(500) DEFAULT NULL COMMENT '告警邮箱',
  `webhook_url` varchar(500) DEFAULT NULL COMMENT 'WebHook地址',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  `sync_status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT 'Quartz同步状态',
  `sync_error` varchar(1000) DEFAULT NULL COMMENT '最近一次Quartz同步错误',
  `sync_time` datetime DEFAULT NULL COMMENT '最近一次Quartz同步时间',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_job_name_group_active` (`job_name`,`job_group`,`del_flag`),
  KEY `idx_job_sync_status_del` (`sync_status`,`del_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='任务配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_job_log`
--

DROP TABLE IF EXISTS `sys_job_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_job_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `job_name` varchar(200) NOT NULL COMMENT '任务名称',
  `job_group` varchar(200) NOT NULL COMMENT '任务分组',
  `executor_handler` varchar(200) DEFAULT NULL COMMENT '执行器Handler',
  `job_param` text COMMENT '任务参数',
  `trigger_time` datetime DEFAULT NULL COMMENT '触发时间',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `heartbeat_time` datetime DEFAULT NULL COMMENT '运行中任务最近心跳时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `duration` bigint DEFAULT NULL COMMENT '执行耗时(ms)',
  `status` tinyint NOT NULL COMMENT '状态：1-成功 0-失败',
  `result` text COMMENT '执行结果',
  `exception_msg` text COMMENT '异常信息',
  `retry_count` int DEFAULT '0' COMMENT '重试次数',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标志（0正常 1删除）',
  `job_config_id` bigint DEFAULT NULL COMMENT '任务配置ID',
  `trigger_type` varchar(20) NOT NULL DEFAULT 'UNKNOWN' COMMENT '触发类型',
  `scheduled_fire_time` datetime DEFAULT NULL COMMENT 'Quartz原计划触发时间',
  `fire_instance_id` varchar(200) DEFAULT NULL COMMENT 'Quartz执行实例ID',
  `process_instance_id` varchar(64) DEFAULT NULL COMMENT 'Flowable流程实例ID',
  PRIMARY KEY (`id`),
  KEY `idx_job_name` (`job_name`),
  KEY `idx_trigger_time` (`trigger_time`),
  KEY `idx_status` (`status`),
  KEY `idx_job_log_config_trigger` (`job_config_id`,`trigger_time`),
  KEY `idx_job_log_observability` (`job_config_id`,`status`,`trigger_type`,`trigger_time`),
  KEY `idx_job_log_process_instance` (`process_instance_id`,`del_flag`),
  KEY `idx_job_log_recovery` (`status`,`heartbeat_time`,`trigger_time`,`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='任务执行日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_login_log`
--

DROP TABLE IF EXISTS `sys_login_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_login_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `tenant_id` bigint DEFAULT '0' COMMENT '租户编号',
  `user_id` bigint DEFAULT NULL COMMENT '用户ID',
  `username` varchar(50) DEFAULT NULL COMMENT '用户名',
  `login_type` varchar(50) DEFAULT NULL COMMENT '登录类型（LOGIN/LOGOUT/REGISTER）',
  `login_status` tinyint DEFAULT '1' COMMENT '登录状态（0-失败，1-成功）',
  `login_ip` varchar(50) DEFAULT NULL COMMENT '登录IP',
  `login_location` varchar(255) DEFAULT NULL COMMENT '登录地点',
  `browser` varchar(100) DEFAULT NULL COMMENT '浏览器',
  `os` varchar(100) DEFAULT NULL COMMENT '操作系统',
  `user_agent` varchar(500) DEFAULT NULL COMMENT '用户代理',
  `login_message` varchar(500) DEFAULT NULL COMMENT '登录信息',
  `login_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
  `client_code` varchar(50) DEFAULT 'pc' COMMENT '客户端代码：pc/app',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_user` (`tenant_id`,`user_id`),
  KEY `idx_login_time` (`login_time`),
  KEY `idx_login_status` (`login_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='登录日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_message`
--

DROP TABLE IF EXISTS `sys_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_message` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `title` varchar(200) NOT NULL COMMENT '消息标题',
  `content` text NOT NULL COMMENT '消息内容',
  `type` varchar(20) NOT NULL DEFAULT 'SYSTEM' COMMENT '消息类型：SYSTEM-系统消息/SMS-短信/EMAIL-邮件/CUSTOM-自定义',
  `send_scope` varchar(20) NOT NULL DEFAULT 'USERS' COMMENT '发送范围：ALL-全员/ORG-指定组织/USERS-指定人员',
  `send_channel` varchar(20) NOT NULL DEFAULT 'WEB' COMMENT '发送渠道：WEB-站内信/SMS-短信/EMAIL-邮件/PUSH-推送',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '消息状态：0-草稿/1-已发送/2-发送失败',
  `sender_id` bigint DEFAULT NULL COMMENT '发送人ID',
  `sender_name` varchar(100) DEFAULT NULL COMMENT '发送人姓名',
  `template_code` varchar(50) DEFAULT NULL COMMENT '模板编码',
  `template_params` json DEFAULT NULL COMMENT '模板参数JSON',
  `biz_type` varchar(50) DEFAULT NULL COMMENT '业务类型（如：ORDER、APPROVAL、TASK等）',
  `biz_key` varchar(100) DEFAULT NULL COMMENT '业务主键（如：订单ID、流程实例ID等）',
  `connection_id` bigint DEFAULT NULL COMMENT '企业协同连接ID（COLLABORATION渠道使用）',
  `platform` varchar(50) DEFAULT NULL COMMENT '企业协同平台编码：WECHAT_ENTERPRISE/DINGTALK/FEISHU 等，非协同渠道为空',
  `idempotency_key` varchar(128) DEFAULT NULL COMMENT '确定性消息幂等键（相同键并发只创建一份逻辑消息）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `create_dept` bigint unsigned DEFAULT NULL COMMENT '创建组织ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_message_idempotency` (`tenant_id`,`idempotency_key`),
  KEY `idx_tenant_type` (`tenant_id`,`type`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_biz` (`biz_type`,`biz_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统消息主表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_message_biz_type`
--

DROP TABLE IF EXISTS `sys_message_biz_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_message_biz_type` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `biz_type` varchar(50) NOT NULL COMMENT '业务类型编码',
  `biz_name` varchar(100) NOT NULL COMMENT '业务类型名称',
  `jump_url` varchar(500) DEFAULT NULL COMMENT '跳转URL模板，支持变量：${bizKey}、${messageId}',
  `jump_target` varchar(20) DEFAULT '_self' COMMENT '跳转方式：_self-当前页/_blank-新窗口',
  `icon` varchar(100) DEFAULT NULL COMMENT '图标',
  `sort` int DEFAULT '0' COMMENT '排序',
  `enabled` tinyint NOT NULL DEFAULT '1' COMMENT '是否启用：0-禁用/1-启用',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注说明',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `create_dept` bigint unsigned DEFAULT NULL COMMENT '创建组织ID',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_type_active` (`tenant_id`,`biz_type`,`del_flag`),
  KEY `idx_enabled` (`enabled`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='消息业务类型配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_message_receiver`
--

DROP TABLE IF EXISTS `sys_message_receiver`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_message_receiver` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `message_id` bigint NOT NULL COMMENT '消息ID',
  `user_id` bigint NOT NULL COMMENT '接收人用户ID',
  `org_id` bigint DEFAULT NULL COMMENT '接收人所属组织ID',
  `read_flag` tinyint NOT NULL DEFAULT '0' COMMENT '已读标记：0-未读/1-已读',
  `read_time` datetime DEFAULT NULL COMMENT '阅读时间',
  `delivery_status` varchar(32) DEFAULT NULL COMMENT '外部渠道投递状态：PENDING/SENT/FAILED/SKIPPED（站内信为NULL）',
  `delivery_attempts` int NOT NULL DEFAULT '0' COMMENT '投递尝试次数',
  `external_id` varchar(200) DEFAULT NULL COMMENT '外部渠道逐人消息ID',
  `last_error_code` varchar(64) DEFAULT NULL COMMENT '最近失败错误码',
  `last_attempt_time` datetime DEFAULT NULL COMMENT '最近投递尝试时间',
  `next_retry_time` datetime DEFAULT NULL COMMENT '下次补偿重试时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_message_user` (`message_id`,`user_id`),
  KEY `idx_user_read` (`user_id`,`read_flag`),
  KEY `idx_tenant_user` (`tenant_id`,`user_id`),
  KEY `idx_receiver_delivery_retry` (`delivery_status`,`next_retry_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='消息接收人表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_message_send_record`
--

DROP TABLE IF EXISTS `sys_message_send_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_message_send_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `message_id` bigint NOT NULL COMMENT '消息ID',
  `channel` varchar(20) NOT NULL COMMENT '发送渠道：WEB/SMS/EMAIL/PUSH',
  `connection_id` bigint DEFAULT NULL COMMENT '企业协同连接ID',
  `platform` varchar(50) DEFAULT NULL COMMENT '企业协同平台编码：WECHAT_ENTERPRISE/DINGTALK/FEISHU 等，非协同渠道为空',
  `idempotency_key` varchar(128) DEFAULT NULL COMMENT '本次渠道投递幂等键',
  `attempt_no` int NOT NULL DEFAULT '1' COMMENT '第几次尝试',
  `provider_request_id` varchar(128) DEFAULT NULL COMMENT '供应商请求ID（用于排障，不含敏感内容）',
  `receiver_count` int DEFAULT '0' COMMENT '接收人数量',
  `success_count` int DEFAULT '0' COMMENT '发送成功数量',
  `fail_count` int DEFAULT '0' COMMENT '发送失败数量',
  `external_id` varchar(100) DEFAULT NULL COMMENT '第三方渠道返回的消息ID',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '发送状态：0-发送中/1-成功/2-失败',
  `error_msg` text COMMENT '错误信息',
  `send_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_message_id` (`message_id`),
  KEY `idx_send_time` (`send_time`),
  KEY `idx_send_record_idempotency` (`tenant_id`,`idempotency_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='消息发送记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_message_template`
--

DROP TABLE IF EXISTS `sys_message_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_message_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `template_code` varchar(50) NOT NULL COMMENT '模板编码（唯一）',
  `template_name` varchar(100) NOT NULL COMMENT '模板名称',
  `type` varchar(20) NOT NULL DEFAULT 'SYSTEM' COMMENT '消息类型：SYSTEM/SMS/EMAIL/CUSTOM',
  `title_template` varchar(200) DEFAULT NULL COMMENT '标题模板（支持${变量}占位符）',
  `content_template` text NOT NULL COMMENT '内容模板（支持${变量}占位符）',
  `default_channel` varchar(20) DEFAULT 'WEB' COMMENT '默认发送渠道',
  `enabled` tinyint NOT NULL DEFAULT '1' COMMENT '是否启用：0-禁用/1-启用',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注说明',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `create_dept` bigint unsigned DEFAULT NULL COMMENT '创建组织ID',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_code_active` (`tenant_id`,`template_code`,`del_flag`),
  KEY `idx_type` (`type`),
  KEY `idx_enabled` (`enabled`)
) ENGINE=InnoDB AUTO_INCREMENT=1910000000000005116 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='消息模板表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_notice`
--

DROP TABLE IF EXISTS `sys_notice`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_notice` (
  `notice_id` bigint NOT NULL AUTO_INCREMENT COMMENT '公告ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `notice_title` varchar(200) NOT NULL COMMENT '公告标题',
  `notice_content` text NOT NULL COMMENT '公告内容',
  `notice_type` varchar(20) NOT NULL DEFAULT 'NOTICE' COMMENT '公告类型：NOTICE-通知公告/ANNOUNCEMENT-系统公告/NEWS-新闻动态',
  `publish_status` tinyint NOT NULL DEFAULT '0' COMMENT '发布状态：0-草稿/1-已发布/2-已撤回',
  `publish_time` datetime DEFAULT NULL COMMENT '发布时间',
  `publisher_id` bigint DEFAULT NULL COMMENT '发布人ID',
  `publisher_name` varchar(100) DEFAULT NULL COMMENT '发布人姓名',
  `publish_scope` tinyint NOT NULL DEFAULT '0' COMMENT '发布范围：0-全部组织/1-指定组织',
  `effective_time` datetime DEFAULT NULL COMMENT '生效时间',
  `expiration_time` datetime DEFAULT NULL COMMENT '失效时间',
  `is_top` tinyint NOT NULL DEFAULT '0' COMMENT '是否置顶：0-否/1-是',
  `top_sort` int DEFAULT '0' COMMENT '置顶排序（数字越大越靠前）',
  `attachment_ids` varchar(500) DEFAULT NULL COMMENT '附件ID列表（多个附件ID用逗号分隔，关联sys_file_metadata表）',
  `read_count` int DEFAULT '0' COMMENT '阅读次数',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `create_dept` bigint unsigned DEFAULT NULL COMMENT '创建组织ID',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标志（0正常 1删除）',
  PRIMARY KEY (`notice_id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_publish_status` (`publish_status`),
  KEY `idx_notice_type` (`notice_type`),
  KEY `idx_effective_time` (`effective_time`),
  KEY `idx_expiration_time` (`expiration_time`),
  KEY `idx_is_top_sort` (`is_top`,`top_sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通知公告表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_notice_org`
--

DROP TABLE IF EXISTS `sys_notice_org`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_notice_org` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `notice_id` bigint NOT NULL COMMENT '公告ID',
  `org_id` bigint NOT NULL COMMENT '组织ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_notice_id` (`notice_id`),
  KEY `idx_org_id` (`org_id`),
  KEY `idx_sys_notice_org_tenant` (`tenant_id`,`notice_id`,`org_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通知公告组织关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_notice_read_record`
--

DROP TABLE IF EXISTS `sys_notice_read_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_notice_read_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `notice_id` bigint NOT NULL COMMENT '公告ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `user_name` varchar(100) DEFAULT NULL COMMENT '用户姓名',
  `org_id` bigint DEFAULT NULL COMMENT '用户所属组织ID',
  `org_name` varchar(100) DEFAULT NULL COMMENT '用户所属组织名称',
  `read_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '阅读时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_notice_user` (`notice_id`,`user_id`),
  KEY `idx_notice_id` (`notice_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_sys_notice_read_tenant` (`tenant_id`,`notice_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通知公告已读记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_operation_log`
--

DROP TABLE IF EXISTS `sys_operation_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_operation_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `tenant_id` bigint DEFAULT '0' COMMENT '租户编号',
  `user_id` bigint DEFAULT NULL COMMENT '操作用户ID',
  `username` varchar(50) DEFAULT NULL COMMENT '操作用户名',
  `operator_name` varchar(100) DEFAULT NULL COMMENT '操作人姓名',
  `operation_module` varchar(100) DEFAULT NULL COMMENT '操作模块',
  `operation_type` varchar(50) DEFAULT NULL COMMENT '操作类型（ADD/UPDATE/DELETE/QUERY/EXPORT/IMPORT）',
  `operation_desc` varchar(500) DEFAULT NULL COMMENT '操作描述',
  `operation_page` varchar(500) DEFAULT NULL COMMENT '操作页面路径',
  `operation_page_title` varchar(200) DEFAULT NULL COMMENT '操作页面标题',
  `operation_content` varchar(1000) DEFAULT NULL COMMENT '操作内容',
  `request_method` varchar(10) DEFAULT NULL COMMENT '请求方式（GET/POST/PUT/DELETE）',
  `request_url` varchar(500) DEFAULT NULL COMMENT '请求URL',
  `request_params` text COMMENT '请求参数',
  `response_result` text COMMENT '响应结果',
  `before_data` mediumtext COMMENT '操作前数据',
  `after_data` mediumtext COMMENT '操作后数据',
  `diff_data` mediumtext COMMENT '操作数据差异',
  `error_msg` text COMMENT '错误信息',
  `operation_status` tinyint DEFAULT '1' COMMENT '操作状态（0-失败，1-成功）',
  `operation_ip` varchar(50) DEFAULT NULL COMMENT '操作IP',
  `operation_location` varchar(255) DEFAULT NULL COMMENT '操作地点',
  `user_agent` varchar(500) DEFAULT NULL COMMENT '用户代理',
  `execute_time` bigint DEFAULT '0' COMMENT '执行时长（毫秒）',
  `operation_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_user` (`tenant_id`,`user_id`),
  KEY `idx_operation_time` (`operation_time`),
  KEY `idx_operation_status` (`operation_status`),
  KEY `idx_request_url` (`request_url`(255)),
  KEY `idx_operation_page_time` (`operation_page`(191),`operation_time`),
  KEY `idx_operator_name` (`operator_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='操作日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_org`
--

DROP TABLE IF EXISTS `sys_org`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_org` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '组织ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `org_code` varchar(100) DEFAULT NULL COMMENT '组织编码',
  `org_parent_code` varchar(100) DEFAULT NULL COMMENT '父级组织编码',
  `org_name` varchar(100) NOT NULL COMMENT '组织名称',
  `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '父级组织ID（0为顶级）',
  `ancestors` varchar(500) DEFAULT NULL COMMENT '祖级编码（逗号分隔，如：1,2,3）',
  `sort` int DEFAULT '0' COMMENT '排序（值越小越靠前）',
  `org_type` varchar(64) DEFAULT '1' COMMENT '组织类型（sys_org_type 字典值）',
  `org_status` tinyint NOT NULL DEFAULT '1' COMMENT '组织状态（0-禁用，1-正常）',
  `leader_id` bigint DEFAULT NULL COMMENT '负责人ID（关联sys_user.id）',
  `leader_name` varchar(50) DEFAULT NULL COMMENT '负责人姓名',
  `phone` varchar(20) DEFAULT NULL COMMENT '组织联系电话',
  `address` varchar(255) DEFAULT NULL COMMENT '组织地址',
  `region_code` varchar(30) DEFAULT NULL COMMENT '行政区划编码',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_dept` bigint unsigned DEFAULT NULL COMMENT '创建组织ID',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_org_name_active` (`tenant_id`,`org_name`,`del_flag`),
  KEY `idx_tenant_parent` (`tenant_id`,`parent_id`),
  KEY `idx_org_status` (`org_status`),
  KEY `idx_ancestors` (`ancestors`)
) ENGINE=InnoDB AUTO_INCREMENT=28 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='组织表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_outbound_whitelist`
--

DROP TABLE IF EXISTS `sys_outbound_whitelist`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_outbound_whitelist` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `scene` varchar(32) NOT NULL COMMENT '出站场景：JOB_WEBHOOK/JOB_RPC/FLOW_API',
  `protocol` varchar(8) NOT NULL COMMENT '协议：http/https',
  `host` varchar(253) NOT NULL COMMENT '规范化精确主机名或IP',
  `port_start` int NOT NULL COMMENT '允许端口起始值',
  `port_end` int NOT NULL COMMENT '允许端口结束值',
  `allow_private` tinyint NOT NULL DEFAULT '0' COMMENT '是否允许RFC1918或ULA私网：0否 1是',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0停用 1启用',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_outbound_whitelist_active` (`tenant_id`,`scene`,`protocol`,`host`,`port_start`,`port_end`,`del_flag`),
  KEY `idx_outbound_whitelist_match` (`tenant_id`,`scene`,`protocol`,`host`,`status`,`del_flag`),
  CONSTRAINT `chk_outbound_port_range` CHECK (((`port_start` between 1 and 65535) and (`port_end` between `port_start` and 65535))),
  CONSTRAINT `chk_outbound_private_scene` CHECK (((`allow_private` = 0) or (`scene` in (_utf8mb4'FLOW_API',_utf8mb4'JOB_RPC'))))
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台受控出站白名单';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_plugin_artifact_registration`
--

DROP TABLE IF EXISTS `sys_plugin_artifact_registration`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_plugin_artifact_registration` (
  `id` varchar(36) NOT NULL,
  `tenant_id` bigint NOT NULL,
  `task_id` varchar(36) NOT NULL,
  `review_id` varchar(36) NOT NULL,
  `request_id` varchar(36) NOT NULL,
  `command_sha256` char(64) NOT NULL,
  `expected_revision` int NOT NULL,
  `release_id` varchar(68) NOT NULL,
  `manifest_sha256` char(64) NOT NULL,
  `repository_id` varchar(64) NOT NULL COMMENT '人工本地仓库标签，不是远程地址',
  `server_result_sha256` char(64) NOT NULL,
  `metadata_json` text NOT NULL COMMENT '有界规范元数据，无路径、源码或凭证',
  `local_verified` tinyint(1) NOT NULL COMMENT '管理员声明，不是平台实际制品复验',
  `not_deployed` tinyint(1) NOT NULL,
  `note` varchar(1000) NOT NULL COMMENT '人工核查说明，禁止记录凭证',
  `del_flag` int NOT NULL DEFAULT '0',
  `create_by` bigint NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_dept` bigint DEFAULT NULL,
  `update_by` bigint NOT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_plugin_artifact_request` (`tenant_id`,`task_id`,`create_by`,`request_id`),
  UNIQUE KEY `uk_sys_plugin_artifact_approval` (`tenant_id`,`task_id`,`review_id`),
  KEY `idx_sys_plugin_artifact_task` (`tenant_id`,`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='插件候选制品人工登记（只追加）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_plugin_build`
--

DROP TABLE IF EXISTS `sys_plugin_build`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_plugin_build` (
  `id` varchar(36) NOT NULL COMMENT '对应插件任务，一任务最多一次构建，不自动抢占重试',
  `tenant_id` bigint NOT NULL,
  `worker_id` varchar(64) NOT NULL,
  `lease_hash` char(64) NOT NULL COMMENT '私有随机租约摘要，不出普通API',
  `phase` varchar(32) NOT NULL,
  `source_commit` char(40) NOT NULL,
  `image` varchar(201) NOT NULL,
  `started_time` datetime NOT NULL,
  `lease_expires_time` datetime NOT NULL,
  `deadline_time` datetime NOT NULL,
  `finished_time` datetime DEFAULT NULL,
  `result_json` text COMMENT '有界执行器报告，不是部署/独立复验结果',
  `result_sha256` char(64) DEFAULT NULL,
  `del_flag` int NOT NULL DEFAULT '0',
  `create_by` bigint NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_dept` bigint DEFAULT NULL,
  `update_by` bigint NOT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_sys_plugin_build_tenant` (`tenant_id`,`del_flag`,`started_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='插件构建租约及执行审计';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_plugin_delivery`
--

DROP TABLE IF EXISTS `sys_plugin_delivery`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_plugin_delivery` (
  `id` varchar(36) NOT NULL,
  `tenant_id` bigint NOT NULL,
  `target_id` varchar(64) NOT NULL,
  `task_id` varchar(36) NOT NULL,
  `release_id` varchar(68) NOT NULL,
  `previous_release_id` varchar(68) DEFAULT NULL,
  `unverified_release_id` varchar(68) DEFAULT NULL,
  `action` varchar(16) NOT NULL,
  `status` varchar(16) NOT NULL,
  `request_id` varchar(36) NOT NULL,
  `command_sha256` char(64) NOT NULL,
  `worker_id` varchar(64) DEFAULT NULL,
  `lease_hash` char(64) DEFAULT NULL,
  `lease_expires_time` datetime DEFAULT NULL,
  `deadline_time` datetime DEFAULT NULL,
  `artifact_manifest_sha256` char(64) NOT NULL,
  `cos_verified` tinyint(1) NOT NULL DEFAULT '0',
  `runtime_verified` tinyint(1) NOT NULL DEFAULT '0',
  `failure_code` varchar(96) DEFAULT NULL,
  `backup_reference` varchar(128) DEFAULT NULL,
  `migrations_reviewed` tinyint(1) NOT NULL,
  `backward_compatible` tinyint(1) NOT NULL,
  `note` varchar(1000) NOT NULL,
  `del_flag` int NOT NULL DEFAULT '0',
  `reconcile_note` varchar(1000) DEFAULT NULL,
  `reconciled_by` bigint DEFAULT NULL,
  `reconciled_time` datetime DEFAULT NULL,
  `create_by` bigint NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_dept` bigint DEFAULT NULL,
  `update_by` bigint NOT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_plugin_delivery_request` (`tenant_id`,`create_by`,`request_id`),
  KEY `idx_plugin_delivery_target` (`tenant_id`,`target_id`,`status`),
  KEY `idx_plugin_delivery_expiry` (`status`,`lease_expires_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='插件交付执行审计，超时保留占用';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_plugin_delivery_target`
--

DROP TABLE IF EXISTS `sys_plugin_delivery_target`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_plugin_delivery_target` (
  `id` varchar(36) NOT NULL,
  `tenant_id` bigint NOT NULL,
  `target_id` varchar(64) NOT NULL,
  `active_task_id` varchar(36) DEFAULT NULL,
  `current_release_id` varchar(68) DEFAULT NULL,
  `previous_release_id` varchar(68) DEFAULT NULL,
  `unverified_release_id` varchar(68) DEFAULT NULL,
  `del_flag` int NOT NULL DEFAULT '0',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_dept` bigint DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_plugin_delivery_target` (`tenant_id`,`target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='交付目标串行占用及核验成功版本，不保存凭证';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_plugin_task`
--

DROP TABLE IF EXISTS `sys_plugin_task`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_plugin_task` (
  `id` varchar(36) NOT NULL COMMENT '随机任务ID',
  `tenant_id` bigint NOT NULL,
  `request_id` varchar(36) NOT NULL COMMENT '上传幂等请求ID',
  `plugin_id` varchar(32) NOT NULL,
  `plugin_name` varchar(128) NOT NULL,
  `plugin_version` varchar(256) NOT NULL,
  `operation_type` varchar(16) NOT NULL,
  `task_status` varchar(32) NOT NULL,
  `active_plugin_id` varchar(32) DEFAULT NULL COMMENT '待确认/待构建的占用键，终态置空',
  `revision` int NOT NULL DEFAULT '0',
  `archive_sha256` char(64) NOT NULL,
  `file_name` varchar(128) NOT NULL,
  `archive_bytes` int NOT NULL,
  `archive_data` mediumblob NOT NULL COMMENT '私有ZIP，普通查询不读取/不提供下载',
  `runtime_snapshot` char(64) NOT NULL,
  `preview_json` mediumtext NOT NULL,
  `confirmed_by` bigint DEFAULT NULL,
  `confirmed_time` datetime DEFAULT NULL,
  `cancelled_by` bigint DEFAULT NULL,
  `cancelled_time` datetime DEFAULT NULL,
  `del_flag` int NOT NULL DEFAULT '0',
  `create_by` bigint NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_dept` bigint DEFAULT NULL,
  `update_by` bigint NOT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_plugin_task_request` (`tenant_id`,`create_by`,`request_id`),
  UNIQUE KEY `uk_sys_plugin_task_active` (`tenant_id`,`active_plugin_id`),
  KEY `idx_sys_plugin_task_page` (`tenant_id`,`del_flag`,`create_time`),
  CONSTRAINT `ck_sys_plugin_task_bytes` CHECK ((`archive_bytes` between 1 and 8388608))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='插件安装预检与确认审计';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_plugin_task_review`
--

DROP TABLE IF EXISTS `sys_plugin_task_review`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_plugin_task_review` (
  `id` varchar(36) NOT NULL,
  `tenant_id` bigint NOT NULL,
  `task_id` varchar(36) NOT NULL,
  `request_id` varchar(36) NOT NULL,
  `command_sha256` char(64) NOT NULL,
  `expected_revision` int NOT NULL,
  `package_sha256` char(64) NOT NULL,
  `result_sha256` char(64) DEFAULT NULL,
  `decision` varchar(32) NOT NULL,
  `previous_status` varchar(32) NOT NULL,
  `target_status` varchar(32) NOT NULL,
  `executor_stopped` tinyint(1) NOT NULL COMMENT '管理员声明，不是平台远程验证',
  `not_deployed` tinyint(1) NOT NULL,
  `artifacts_reviewed` tinyint(1) DEFAULT NULL,
  `migrations_reviewed` tinyint(1) DEFAULT NULL,
  `note` varchar(1000) NOT NULL COMMENT '人工核查说明，禁止记录凭证/原始日志',
  `del_flag` int NOT NULL DEFAULT '0',
  `create_by` bigint NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_dept` bigint DEFAULT NULL,
  `update_by` bigint NOT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_plugin_review_request` (`tenant_id`,`task_id`,`create_by`,`request_id`),
  UNIQUE KEY `uk_sys_plugin_review_decision` (`tenant_id`,`task_id`,`decision`),
  KEY `idx_sys_plugin_review_history` (`tenant_id`,`task_id`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='插件任务人工审查审计（只追加）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_post`
--

DROP TABLE IF EXISTS `sys_post`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_post` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '岗位ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `post_code` varchar(50) NOT NULL COMMENT '岗位编码（租户内唯一）',
  `org_id` bigint NOT NULL COMMENT '所属组织ID（关联sys_org.id）',
  `post_name` varchar(100) NOT NULL COMMENT '岗位名称',
  `post_status` tinyint NOT NULL DEFAULT '1' COMMENT '岗位状态（0-禁用，1-正常）',
  `post_type` tinyint DEFAULT '1' COMMENT '岗位类型（1-管理岗，2-技术岗，3-业务岗，4-其他）',
  `sort` int DEFAULT '0' COMMENT '排序（值越小越靠前）',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_dept` bigint unsigned DEFAULT NULL COMMENT '创建组织ID',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_org_post_active` (`tenant_id`,`org_id`,`post_name`,`del_flag`),
  UNIQUE KEY `uk_tenant_post_code_active` (`tenant_id`,`post_code`,`del_flag`),
  KEY `idx_tenant_org` (`tenant_id`,`org_id`),
  KEY `idx_post_status` (`post_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='岗位表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_print_binding`
--

DROP TABLE IF EXISTS `sys_print_binding`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_print_binding` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `application_id` bigint DEFAULT NULL COMMENT '低代码应用 ID；独立来源为空',
  `business_source_id` bigint DEFAULT NULL COMMENT '独立业务来源 ID',
  `source_code` varchar(80) DEFAULT NULL COMMENT '独立业务来源编码',
  `source_type` varchar(20) NOT NULL COMMENT '来源类型',
  `source_key` varchar(191) NOT NULL COMMENT '服务端规范化来源',
  `page_id` varchar(128) DEFAULT NULL COMMENT '工作台页面稳定标识',
  `form_key` varchar(128) DEFAULT NULL COMMENT '代码表单标识',
  `object_code` varchar(100) NOT NULL COMMENT '对象编码',
  `template_id` bigint NOT NULL COMMENT '模板 ID',
  `template_version_id` bigint DEFAULT NULL COMMENT '独立来源固定模板版本',
  `scene` varchar(30) NOT NULL COMMENT '使用场景',
  `is_default` tinyint NOT NULL DEFAULT '0' COMMENT '是否默认，同应用锁内维护',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '启停',
  `binding_revision` bigint NOT NULL DEFAULT '1' COMMENT '绑定并发修订号',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '删除墓碑',
  `create_by` bigint NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_dept` bigint DEFAULT NULL,
  `update_by` bigint NOT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_print_binding_source_template` (`tenant_id`,`source_key`,`template_id`,`scene`,`del_flag`),
  KEY `idx_print_binding_template` (`tenant_id`,`template_id`,`del_flag`),
  KEY `idx_print_binding_default` (`tenant_id`,`application_id`,`source_key`,`scene`,`is_default`,`status`,`del_flag`),
  KEY `idx_print_binding_business_source` (`tenant_id`,`business_source_id`,`scene`,`status`,`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_print_business_source`
--

DROP TABLE IF EXISTS `sys_print_business_source`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_print_business_source` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `source_code` varchar(80) NOT NULL COMMENT '租户内稳定业务来源编码',
  `source_name` varchar(100) NOT NULL COMMENT '业务来源名称',
  `source_type` varchar(20) NOT NULL COMMENT 'SERVICE/DATASET/API',
  `provider_code` varchar(100) DEFAULT NULL COMMENT '服务端注册的 Provider 编码',
  `dataset_id` bigint DEFAULT NULL COMMENT '已发布数据集 ID',
  `object_code` varchar(100) NOT NULL COMMENT '标准业务对象编码',
  `parameter_schema_json` mediumtext COMMENT '受控调用参数协议',
  `mapping_json` mediumtext COMMENT '数据集或接口结果到打印上下文的映射',
  `catalog_json` mediumtext COMMENT '发布时字段目录快照',
  `catalog_hash` char(64) DEFAULT NULL COMMENT '字段目录 SHA-256',
  `source_revision` bigint NOT NULL DEFAULT '1' COMMENT '来源并发修订号',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '启停；新来源默认停用',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '删除墓碑：有效为 0，删除写主键',
  `create_by` bigint NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_dept` bigint DEFAULT NULL,
  `update_by` bigint NOT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_print_business_source_code` (`tenant_id`,`source_code`,`del_flag`),
  KEY `idx_print_business_source_type` (`tenant_id`,`source_type`,`status`,`del_flag`),
  KEY `idx_print_business_source_dataset` (`tenant_id`,`dataset_id`,`status`,`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_print_execution`
--

DROP TABLE IF EXISTS `sys_print_execution`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_print_execution` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `application_id` bigint DEFAULT NULL COMMENT '低代码应用 ID；独立来源为空',
  `business_source_id` bigint DEFAULT NULL COMMENT '独立业务来源 ID',
  `source_revision` bigint DEFAULT NULL COMMENT '实际使用的来源修订号',
  `source_code` varchar(80) DEFAULT NULL COMMENT '独立业务来源编码',
  `application_version_id` bigint DEFAULT NULL COMMENT '应用发布版本',
  `template_id` bigint NOT NULL COMMENT '模板 ID',
  `template_version_id` bigint NOT NULL COMMENT '实际打印版本',
  `source_key` varchar(191) NOT NULL COMMENT '规范化来源',
  `object_code` varchar(100) NOT NULL COMMENT '对象编码',
  `record_id` varchar(128) NOT NULL COMMENT '记录 ID',
  `scene` varchar(30) NOT NULL COMMENT '运行场景',
  `task_id` varchar(128) DEFAULT NULL COMMENT '流程任务 ID',
  `process_instance_id` varchar(128) DEFAULT NULL COMMENT '流程实例 ID',
  `process_run_id` bigint DEFAULT NULL COMMENT '业务流程运行 ID',
  `actor` bigint NOT NULL COMMENT '服务端登录操作者',
  `data_mode` varchar(20) NOT NULL DEFAULT 'CURRENT' COMMENT '数据模式',
  `generated_at` datetime NOT NULL COMMENT '数据生成时刻',
  `result` varchar(30) NOT NULL DEFAULT 'PREPARED' COMMENT 'PREPARED/DIALOG_OPENED/FAILED，不代表物理出纸',
  `error_code` varchar(80) DEFAULT NULL COMMENT '受限错误码，不存消息正文',
  `page_count` int DEFAULT NULL COMMENT '准备后页数，最大 50',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `create_by` bigint NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_dept` bigint DEFAULT NULL,
  `update_by` bigint NOT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_print_execution_actor` (`tenant_id`,`actor`,`generated_at`,`del_flag`),
  KEY `idx_print_execution_version` (`tenant_id`,`template_version_id`,`del_flag`),
  KEY `idx_print_execution_record` (`tenant_id`,`application_id`,`object_code`,`record_id`,`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_print_template`
--

DROP TABLE IF EXISTS `sys_print_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_print_template` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `application_id` bigint DEFAULT NULL COMMENT '低代码应用 ID；独立来源为空',
  `business_source_id` bigint DEFAULT NULL COMMENT '独立业务来源 ID',
  `source_code` varchar(80) DEFAULT NULL COMMENT '独立业务来源编码',
  `template_code` varchar(80) NOT NULL COMMENT '应用内模板编码',
  `template_name` varchar(100) NOT NULL COMMENT '模板名称',
  `source_type` varchar(20) NOT NULL COMMENT 'LOWCODE/CODE',
  `source_key` varchar(191) NOT NULL COMMENT '服务端规范化来源',
  `page_id` varchar(128) DEFAULT NULL COMMENT '工作台页面稳定标识',
  `form_key` varchar(128) DEFAULT NULL COMMENT '代码表单标识',
  `object_code` varchar(100) NOT NULL COMMENT '业务对象编码',
  `draft_schema` mediumtext NOT NULL COMMENT '已校验的协议草稿',
  `draft_revision` bigint NOT NULL DEFAULT '1' COMMENT '并发修订号',
  `design_status` varchar(20) NOT NULL DEFAULT 'DRAFT' COMMENT '设计状态',
  `published_version_id` bigint DEFAULT NULL COMMENT '当前发布版本',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '启停',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '删除墓碑：有效为 0，删除写主键',
  `create_by` bigint NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_dept` bigint DEFAULT NULL,
  `update_by` bigint NOT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_print_template_source_code` (`tenant_id`,`source_key`,`template_code`,`del_flag`),
  KEY `idx_print_template_source` (`tenant_id`,`application_id`,`source_key`,`status`,`del_flag`),
  KEY `idx_print_template_business_source` (`tenant_id`,`business_source_id`,`status`,`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_region_code`
--

DROP TABLE IF EXISTS `sys_region_code`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_region_code` (
  `code` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '行政区划代码',
  `name` varchar(100) NOT NULL COMMENT '行政区划名称',
  `level` tinyint NOT NULL COMMENT '行政级别(1-省,2-市,3-区/县,4-街道)',
  `parent_code` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '父级代码',
  `full_name` varchar(200) DEFAULT NULL COMMENT '全名',
  `city_code` varchar(20) DEFAULT NULL COMMENT '地市编码',
  PRIMARY KEY (`code`),
  KEY `idx_parent_code` (`parent_code`),
  KEY `sys_region_code_name_IDX` (`name`) USING BTREE,
  KEY `idx_parent_level` (`parent_code`,`level`),
  KEY `idx_code_level` (`code`,`level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_resource`
--

DROP TABLE IF EXISTS `sys_resource`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_resource` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '资源ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `resource_name` varchar(100) NOT NULL COMMENT '资源名称',
  `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '父级资源ID（0为顶级）',
  `resource_type` tinyint NOT NULL COMMENT '资源类型（1-目录，2-菜单，3-按钮，4-API接口）',
  `sort` int DEFAULT '0' COMMENT '排序（值越小越靠前）',
  `path` varchar(255) DEFAULT NULL COMMENT '资源路由（菜单/目录用）',
  `component` varchar(255) DEFAULT NULL COMMENT '前端组件路径（菜单用）',
  `is_external` tinyint DEFAULT '0' COMMENT '是否外链（0-否，1-是）',
  `sso_enabled` tinyint DEFAULT '0' COMMENT '是否启用SSO（0-否，1-是）',
  `sso_target_client` varchar(64) DEFAULT NULL COMMENT 'SSO目标客户端编码',
  `open_target` varchar(20) DEFAULT '_self' COMMENT '打开方式：_self-当前页/_blank-新窗口',
  `is_public` tinyint DEFAULT '0' COMMENT '是否公开资源（0-否，1-是，公开资源无需权限验证）',
  `menu_status` tinyint DEFAULT '1' COMMENT '菜单状态（0-隐藏，1-显示，仅菜单/目录用）',
  `visible` tinyint DEFAULT '1' COMMENT '显示状态（0-隐藏，1-显示，所有资源通用）',
  `perms` varchar(100) DEFAULT NULL COMMENT '权限标识（如：sys:user:list，按钮/API用）',
  `feature_code` varchar(64) DEFAULT NULL COMMENT '功能授权编码，空表示不受授权控制',
  `icon` varchar(100) DEFAULT NULL COMMENT '图标（菜单/目录用）',
  `api_method` varchar(10) DEFAULT NULL COMMENT 'API请求方法（GET/POST/PUT/DELETE，仅API用）',
  `api_url` varchar(255) DEFAULT NULL COMMENT 'API接口地址（仅API用）',
  `keep_alive` tinyint DEFAULT '0' COMMENT '是否缓存（0-否，1-是，菜单用）',
  `always_show` tinyint DEFAULT '0' COMMENT '是否总是显示（0-否，1-是，菜单用）',
  `redirect` varchar(255) DEFAULT NULL COMMENT '重定向地址（菜单用）',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_dept` bigint unsigned DEFAULT NULL COMMENT '创建组织ID',
  `client_code` varchar(50) DEFAULT 'pc' COMMENT '客户端代码：pc/app',
  `min_user_type` int NOT NULL DEFAULT '2' COMMENT '最低可访问用户类型（0-系统管理员，1-租户管理员，2-普通用户）',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_resource_active` (`tenant_id`,`resource_type`,`perms`,`client_code`,`del_flag`),
  KEY `idx_tenant_parent` (`tenant_id`,`parent_id`),
  KEY `idx_resource_type` (`resource_type`),
  KEY `idx_api_url_method` (`api_url`,`api_method`) COMMENT 'API查询优化',
  KEY `idx_resource_menu_tree` (`client_code`,`del_flag`,`sort`,`create_time` DESC)
) ENGINE=InnoDB AUTO_INCREMENT=10155 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统资源表（菜单/按钮/API）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_role`
--

DROP TABLE IF EXISTS `sys_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `role_name` varchar(50) NOT NULL COMMENT '角色名称（租户内唯一）',
  `role_key` varchar(100) NOT NULL COMMENT '角色权限字符串（如：admin,user:view）',
  `role_type` int DEFAULT '1' COMMENT '角色类型（1-管理角色，2-业务角色，3-审批角色，4-数据角色）',
  `org_scope_type` tinyint NOT NULL DEFAULT '1' COMMENT '组织适用范围类型（1租户全局 2指定组织）',
  `data_scope` tinyint DEFAULT '1' COMMENT '权限范围（1-全部数据，2-本租户数据，3-本组织数据，4-本组织及子组织，5-个人数据）',
  `sort` int DEFAULT '0' COMMENT '排序（值越小越靠前）',
  `role_status` tinyint NOT NULL DEFAULT '1' COMMENT '角色状态（0-禁用，1-正常）',
  `is_system` tinyint DEFAULT '0' COMMENT '是否系统内置角色（0-否，1-是）',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_dept` bigint unsigned DEFAULT NULL COMMENT '创建组织ID',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_role_key_active` (`tenant_id`,`role_key`,`del_flag`),
  UNIQUE KEY `uk_tenant_role_name_active` (`tenant_id`,`role_name`,`del_flag`),
  KEY `idx_tenant_status` (`tenant_id`,`role_status`),
  KEY `idx_data_scope` (`data_scope`)
) ENGINE=InnoDB AUTO_INCREMENT=34 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_role_data_scope`
--

DROP TABLE IF EXISTS `sys_role_data_scope`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_role_data_scope` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `org_id` bigint NOT NULL COMMENT '组织ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_org` (`tenant_id`,`role_id`,`org_id`),
  KEY `idx_role_id` (`role_id`),
  KEY `idx_org_id` (`org_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色-自定义数据权限关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_role_module_data_scope`
--

DROP TABLE IF EXISTS `sys_role_module_data_scope`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_role_module_data_scope` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `module_code` varchar(100) NOT NULL COMMENT '业务模块编码，对应 sys_data_scope_config.resource_code',
  `data_scope` tinyint NOT NULL COMMENT '数据范围（1全部，2本租户，3本组织，4本组织及下级，5本人，7行政区划）',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_role_module` (`tenant_id`,`role_id`,`module_code`),
  KEY `idx_role_id` (`role_id`),
  KEY `idx_tenant_module_scope` (`tenant_id`,`module_code`,`data_scope`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色业务模块数据范围覆盖表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_role_org`
--

DROP TABLE IF EXISTS `sys_role_org`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_role_org` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `org_id` bigint NOT NULL COMMENT '组织ID',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_org` (`tenant_id`,`role_id`,`org_id`),
  KEY `idx_org_role` (`tenant_id`,`org_id`,`role_id`),
  KEY `idx_role_id` (`role_id`),
  KEY `idx_org_id` (`org_id`)
) ENGINE=InnoDB AUTO_INCREMENT=126 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='角色适用组织范围表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_role_resource`
--

DROP TABLE IF EXISTS `sys_role_resource`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_role_resource` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `resource_id` bigint NOT NULL COMMENT '资源ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_resource` (`tenant_id`,`role_id`,`resource_id`),
  KEY `idx_role_id` (`role_id`),
  KEY `idx_resource_id` (`resource_id`)
) ENGINE=InnoDB AUTO_INCREMENT=15523 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色-资源关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_sms_config`
--

DROP TABLE IF EXISTS `sys_sms_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_sms_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `config_id` varchar(64) DEFAULT NULL COMMENT '配置标识',
  `supplier` varchar(32) NOT NULL COMMENT '厂商标识(alibaba/tencent/huawei等)',
  `access_key_id` varchar(128) DEFAULT NULL COMMENT '访问密钥ID',
  `access_key_secret` varchar(256) DEFAULT NULL COMMENT '访问密钥Secret',
  `signature` varchar(64) DEFAULT NULL COMMENT '短信签名',
  `template_id` varchar(64) DEFAULT NULL COMMENT '默认模板ID',
  `weight` int DEFAULT '1' COMMENT '权重(负载均衡)',
  `retry_interval` int DEFAULT '5' COMMENT '重试间隔(秒)',
  `max_retries` int DEFAULT '0' COMMENT '最大重试次数',
  `maximum` int DEFAULT NULL COMMENT '发送上限',
  `extra_config` text COMMENT '额外配置(JSON格式)',
  `daily_limit` int DEFAULT NULL COMMENT '每日发送上限',
  `minute_limit` int DEFAULT NULL COMMENT '每分钟发送上限',
  `status` tinyint DEFAULT '0' COMMENT '状态(0禁用 1启用)',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_status` (`tenant_id`,`status`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='短信配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_social_app_config`
--

DROP TABLE IF EXISTS `sys_social_app_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_social_app_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `connection_id` bigint NOT NULL COMMENT '企业协同连接ID',
  `app_code` varchar(64) NOT NULL COMMENT '应用编码（连接内唯一）',
  `app_name` varchar(100) DEFAULT NULL COMMENT '应用名称',
  `client_id` varchar(255) DEFAULT NULL COMMENT '应用ID/Key（企业微信为AgentId所属应用的CorpId沿用连接）',
  `agent_id` varchar(100) DEFAULT NULL COMMENT '企业微信AgentId',
  `secret_mode` varchar(32) NOT NULL DEFAULT 'CIPHER' COMMENT 'Secret存储模式：CIPHER密文/EXTERNAL_REF外部引用',
  `secret_cipher` varchar(1000) DEFAULT NULL COMMENT '应用Secret密文（FPC1版本化密文，禁止明文）',
  `secret_ref` varchar(500) DEFAULT NULL COMMENT '外部Secret引用（extref:前缀）',
  `secret_update_time` datetime DEFAULT NULL COMMENT 'Secret最近轮换时间',
  `callback_token_cipher` varchar(1000) DEFAULT NULL COMMENT '回调Token密文',
  `encoding_aes_key_cipher` varchar(1000) DEFAULT NULL COMMENT '回调EncodingAESKey密文',
  `redirect_uri` varchar(500) DEFAULT NULL COMMENT 'OAuth回调地址',
  `scope` varchar(500) DEFAULT NULL COMMENT '授权范围',
  `config_json` json DEFAULT NULL COMMENT '应用级扩展配置JSON',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0停用 1启用',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint unsigned DEFAULT NULL COMMENT '创建组织ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_social_app_active` (`tenant_id`,`connection_id`,`app_code`,`del_flag`),
  KEY `idx_social_app_connection` (`connection_id`,`status`,`del_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='企业协同物理应用配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_social_callback_event`
--

DROP TABLE IF EXISTS `sys_social_callback_event`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_social_callback_event` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `connection_id` bigint NOT NULL COMMENT '企业协同连接ID',
  `app_config_id` bigint DEFAULT NULL COMMENT '定位回调凭据的物理应用ID',
  `event_id` varchar(128) DEFAULT NULL COMMENT '外部事件ID',
  `dedup_hash` varchar(64) NOT NULL COMMENT '去重哈希（签名+时间戳+nonce+正文摘要）',
  `event_type` varchar(64) DEFAULT NULL COMMENT '事件类型',
  `event_time` datetime DEFAULT NULL COMMENT '外部事件时间',
  `signature_status` varchar(32) NOT NULL DEFAULT 'VERIFIED' COMMENT '验签状态：VERIFIED/REJECTED',
  `payload_cipher` mediumtext COMMENT '解密后事件正文密文（FPC1版本化密文，按留存期清理）',
  `process_status` varchar(32) NOT NULL DEFAULT 'PENDING' COMMENT '处理状态：PENDING/PROCESSING/PROCESSED/FAILED/DISCARDED',
  `retry_count` int NOT NULL DEFAULT '0' COMMENT '重试次数',
  `next_retry_time` datetime DEFAULT NULL COMMENT '下次重试时间',
  `error_code` varchar(64) DEFAULT NULL COMMENT '错误码',
  `error_summary` varchar(500) DEFAULT NULL COMMENT '脱敏错误摘要',
  `claimed_by` varchar(64) DEFAULT NULL COMMENT '当前处理Worker标识',
  `claim_time` datetime DEFAULT NULL COMMENT '领取时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_social_callback_dedup` (`tenant_id`,`connection_id`,`dedup_hash`),
  KEY `idx_social_callback_process` (`process_status`,`next_retry_time`),
  KEY `idx_social_callback_conn` (`tenant_id`,`connection_id`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='企业协同回调事件收件箱';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_social_capability_binding`
--

DROP TABLE IF EXISTS `sys_social_capability_binding`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_social_capability_binding` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `connection_id` bigint NOT NULL COMMENT '企业协同连接ID',
  `capability` varchar(32) NOT NULL COMMENT '业务能力：LOGIN/DIRECTORY/MESSAGE/TODO',
  `app_config_id` bigint NOT NULL COMMENT '绑定的物理应用ID',
  `config_json` json DEFAULT NULL COMMENT '能力级扩展配置JSON',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0停用 1启用',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint unsigned DEFAULT NULL COMMENT '创建组织ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_social_capability_active` (`tenant_id`,`connection_id`,`capability`,`del_flag`),
  KEY `idx_social_capability_app` (`app_config_id`,`del_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='企业协同能力绑定表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_social_config`
--

DROP TABLE IF EXISTS `sys_social_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_social_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `platform` varchar(50) NOT NULL COMMENT '平台类型（WECHAT、DINGTALK、GITHUB、GITEE等）',
  `platform_name` varchar(100) DEFAULT NULL COMMENT '平台名称',
  `platform_logo` varchar(500) DEFAULT NULL COMMENT '平台Logo',
  `connection_code` varchar(64) DEFAULT NULL COMMENT '连接编码（租户内唯一）',
  `connection_name` varchar(100) DEFAULT NULL COMMENT '连接名称',
  `enterprise_id` varchar(100) DEFAULT NULL COMMENT '外部企业ID（企业微信CorpId等）',
  `connection_type` varchar(32) DEFAULT NULL COMMENT '连接类型：CORP_INTERNAL自建应用/THIRD_PARTY第三方/OAUTH_ONLY仅登录',
  `identity_policy` varchar(32) DEFAULT NULL COMMENT '身份匹配策略：BIND_ONLY仅绑定已有/AUTO_CREATE自动创建/MANUAL人工处理',
  `default_role_ids` varchar(512) DEFAULT NULL COMMENT '自动建号默认角色ID列表（逗号分隔）',
  `directory_authority` varchar(32) DEFAULT NULL COMMENT '目录权威来源：EXTERNAL外部权威/LOCAL本地权威/NONE不同步',
  `default_org_id` bigint DEFAULT NULL COMMENT '目录同步默认挂载的根组织ID',
  `api_base_url` varchar(255) DEFAULT NULL COMMENT 'API基础地址：为空使用平台官方地址，私有化部署可自定义',
  `todo_push_enabled` tinyint NOT NULL DEFAULT '0' COMMENT '待办卡片推送开关：1开启 0关闭',
  `todo_push_h5_url` varchar(512) DEFAULT NULL COMMENT '待办H5访问地址：须在平台可信域名内，用于拼接待办详情深链',
  `sso_workbench_enabled` tinyint NOT NULL DEFAULT '0' COMMENT '工作台免登开关：1开启 0关闭；开启后客户端工作台可用该连接的 connection_code 免登',
  `sync_schedule_enabled` tinyint NOT NULL DEFAULT '0' COMMENT '定时目录同步开关：1开启 0关闭，开启后由连接管理自动维护定时任务',
  `sync_cron` varchar(64) DEFAULT NULL COMMENT '定时目录同步 Cron 表达式：sync_schedule_enabled=1 时生效',
  `client_id` varchar(255) DEFAULT NULL COMMENT '应用ID/Key（旧登录配置字段，兼容期保留；凭据改存应用表后可空）',
  `client_secret` varchar(255) DEFAULT NULL COMMENT '应用Secret（旧登录配置明文字段，兼容期保留；新凭据统一存应用表密文，本列可空）',
  `redirect_uri` varchar(500) DEFAULT NULL COMMENT '回调地址',
  `agent_id` varchar(100) DEFAULT NULL COMMENT '企业微信AgentId',
  `scope` varchar(500) DEFAULT NULL COMMENT '授权范围',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态（1-启用，0-停用）',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注说明',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_dept` bigint unsigned DEFAULT NULL COMMENT '创建组织ID',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_social_conn_code_active` (`tenant_id`,`connection_code`,`del_flag`),
  UNIQUE KEY `uk_social_conn_enterprise_active` (`tenant_id`,`platform`,`enterprise_id`,`del_flag`),
  KEY `idx_tenant` (`tenant_id`) USING BTREE,
  KEY `idx_social_platform` (`platform`,`tenant_id`,`del_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='三方登录配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_social_org_mapping`
--

DROP TABLE IF EXISTS `sys_social_org_mapping`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_social_org_mapping` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `connection_id` bigint NOT NULL COMMENT '企业协同连接ID',
  `external_dept_id` varchar(100) NOT NULL COMMENT '外部部门ID',
  `external_parent_id` varchar(100) DEFAULT NULL COMMENT '外部父部门ID',
  `external_dept_name` varchar(200) DEFAULT NULL COMMENT '外部部门名称',
  `org_id` bigint DEFAULT NULL COMMENT 'Forge组织ID',
  `source_hash` varchar(64) DEFAULT NULL COMMENT '外部快照哈希（用于变更检测）',
  `last_seen_run_id` bigint DEFAULT NULL COMMENT '最近一次出现的同步批次ID',
  `status` varchar(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '映射状态：ACTIVE/INACTIVE/ISSUE',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_social_org_mapping_active` (`tenant_id`,`connection_id`,`external_dept_id`,`del_flag`),
  KEY `idx_social_org_mapping_org` (`tenant_id`,`connection_id`,`org_id`),
  KEY `idx_social_org_mapping_seen` (`tenant_id`,`connection_id`,`last_seen_run_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='企业协同外部部门映射表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_social_post_mapping`
--

DROP TABLE IF EXISTS `sys_social_post_mapping`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_social_post_mapping` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `connection_id` bigint NOT NULL COMMENT '企业协同连接ID',
  `external_post_code` varchar(100) NOT NULL COMMENT '外部岗位编码/文本',
  `external_post_name` varchar(200) DEFAULT NULL COMMENT '外部岗位名称',
  `post_id` bigint DEFAULT NULL COMMENT 'Forge岗位ID（企微首期默认不自动创建）',
  `status` varchar(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '映射状态：ACTIVE/INACTIVE/ISSUE',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_social_post_mapping_active` (`tenant_id`,`connection_id`,`external_post_code`,`del_flag`),
  KEY `idx_social_post_mapping_post` (`tenant_id`,`connection_id`,`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='企业协同外部岗位映射表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_social_sync_issue`
--

DROP TABLE IF EXISTS `sys_social_sync_issue`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_social_sync_issue` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `connection_id` bigint NOT NULL COMMENT '企业协同连接ID',
  `sync_log_id` bigint DEFAULT NULL COMMENT '产生问题的同步批次ID',
  `object_type` varchar(32) NOT NULL COMMENT '对象类型：DEPT/USER/POST/TAG',
  `external_id` varchar(100) DEFAULT NULL COMMENT '外部对象ID',
  `issue_code` varchar(64) NOT NULL COMMENT '问题码',
  `issue_summary` varchar(500) DEFAULT NULL COMMENT '脱敏问题摘要（禁止明文手机号/邮箱/姓名）',
  `process_status` varchar(32) NOT NULL DEFAULT 'PENDING' COMMENT '处理状态：PENDING/RESOLVED/IGNORED',
  `process_action` varchar(32) DEFAULT NULL COMMENT '处理动作：BIND/IGNORE/RETRY',
  `process_by` bigint DEFAULT NULL COMMENT '处理人ID',
  `process_time` datetime DEFAULT NULL COMMENT '处理时间',
  `retry_count` int NOT NULL DEFAULT '0' COMMENT '重试次数',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`id`),
  KEY `idx_social_sync_issue_status` (`tenant_id`,`connection_id`,`process_status`,`del_flag`),
  KEY `idx_social_sync_issue_log` (`sync_log_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='企业协同同步问题单表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_social_sync_log`
--

DROP TABLE IF EXISTS `sys_social_sync_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_social_sync_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '批次ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `connection_id` bigint NOT NULL COMMENT '企业协同连接ID',
  `sync_type` varchar(32) NOT NULL COMMENT '同步类型：FULL/DEPT/USER/TAG/INCREMENT',
  `trigger_source` varchar(32) NOT NULL DEFAULT 'MANUAL' COMMENT '触发来源：MANUAL/JOB/CALLBACK',
  `stage` varchar(32) DEFAULT NULL COMMENT '当前阶段：FETCH/VALIDATE/PLAN/APPLY/FINALIZE',
  `status` varchar(32) NOT NULL DEFAULT 'RUNNING' COMMENT '批次状态：RUNNING/SUCCESS/PARTIAL/FAILED',
  `dept_count` int NOT NULL DEFAULT '0' COMMENT '拉取部门数',
  `user_count` int NOT NULL DEFAULT '0' COMMENT '拉取成员数',
  `tag_count` int NOT NULL DEFAULT '0' COMMENT '拉取标签数',
  `created_count` int NOT NULL DEFAULT '0' COMMENT '创建对象数',
  `updated_count` int NOT NULL DEFAULT '0' COMMENT '更新对象数',
  `inactivated_count` int NOT NULL DEFAULT '0' COMMENT '停用对象数',
  `issue_count` int NOT NULL DEFAULT '0' COMMENT '问题单数',
  `cursor_info` varchar(500) DEFAULT NULL COMMENT '断点游标信息',
  `error_code` varchar(64) DEFAULT NULL COMMENT '错误码',
  `error_summary` varchar(1000) DEFAULT NULL COMMENT '脱敏错误摘要',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `create_by` bigint DEFAULT NULL COMMENT '触发人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_social_sync_log_conn` (`tenant_id`,`connection_id`,`create_time`),
  KEY `idx_social_sync_log_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='企业协同同步批次日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_social_tag`
--

DROP TABLE IF EXISTS `sys_social_tag`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_social_tag` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `connection_id` bigint NOT NULL COMMENT '企业协同连接ID',
  `external_tag_id` varchar(100) NOT NULL COMMENT '外部标签ID',
  `tag_name` varchar(200) DEFAULT NULL COMMENT '标签名称',
  `status` varchar(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '标签状态：ACTIVE/INACTIVE',
  `source_hash` varchar(64) DEFAULT NULL COMMENT '外部快照哈希（用于变更检测）',
  `last_seen_run_id` bigint DEFAULT NULL COMMENT '最近一次出现的同步批次ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_social_tag_active` (`tenant_id`,`connection_id`,`external_tag_id`,`del_flag`),
  KEY `idx_social_tag_seen` (`tenant_id`,`connection_id`,`last_seen_run_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='企业协同外部标签表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_social_tag_member`
--

DROP TABLE IF EXISTS `sys_social_tag_member`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_social_tag_member` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `connection_id` bigint NOT NULL COMMENT '企业协同连接ID',
  `tag_id` bigint NOT NULL COMMENT '本地标签ID（sys_social_tag.id）',
  `member_type` varchar(16) NOT NULL COMMENT '成员类型：USER/DEPT',
  `external_member_id` varchar(100) NOT NULL COMMENT '外部成员ID（userid或部门ID）',
  `local_target_id` bigint DEFAULT NULL COMMENT '映射到的Forge用户/组织ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_social_tag_member` (`tenant_id`,`tag_id`,`member_type`,`external_member_id`),
  KEY `idx_social_tag_member_conn` (`tenant_id`,`connection_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='企业协同标签成员关系表（可重建）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_social_todo_link`
--

DROP TABLE IF EXISTS `sys_social_todo_link`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_social_todo_link` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `connection_id` bigint NOT NULL COMMENT '企业协同连接ID',
  `task_id` varchar(64) NOT NULL COMMENT 'Flowable任务ID',
  `user_id` bigint NOT NULL COMMENT 'Forge用户ID',
  `external_user_id` varchar(100) DEFAULT NULL COMMENT '外部用户ID',
  `desired_state` varchar(32) NOT NULL COMMENT '期望状态：ACTIVE/CLOSED',
  `delivery_state` varchar(32) NOT NULL DEFAULT 'PENDING' COMMENT '投递状态：PENDING/SENT/FAILED/CLOSED',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '状态版本号（CAS）',
  `external_id` varchar(200) DEFAULT NULL COMMENT '外部待办/卡片ID',
  `idempotency_key` varchar(128) DEFAULT NULL COMMENT '幂等键',
  `retry_at` datetime DEFAULT NULL COMMENT '下次重试时间',
  `last_error_code` varchar(64) DEFAULT NULL COMMENT '最近错误码',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_social_todo_link_active` (`tenant_id`,`connection_id`,`task_id`,`user_id`,`del_flag`),
  KEY `idx_social_todo_link_retry` (`delivery_state`,`retry_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='企业协同待办投影表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_tenant`
--

DROP TABLE IF EXISTS `sys_tenant`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_tenant` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '租户ID',
  `tenant_name` varchar(100) NOT NULL COMMENT '租户名称',
  `contact_person` varchar(50) DEFAULT NULL COMMENT '负责人',
  `contact_phone` varchar(20) DEFAULT NULL COMMENT '联系电话',
  `user_limit` int DEFAULT '0' COMMENT '租户人员数量上限（0表示无限制）',
  `tenant_status` tinyint NOT NULL DEFAULT '1' COMMENT '租户状态（0-禁用，1-正常）',
  `expire_time` datetime DEFAULT NULL COMMENT '过期时间',
  `tenant_desc` varchar(500) DEFAULT NULL COMMENT '租户描述',
  `create_by` bigint DEFAULT NULL COMMENT '创建者（系统租户ID）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `browser_icon` varchar(255) DEFAULT NULL COMMENT '浏览器icon（存储图标URL/Base64）',
  `browser_title` varchar(100) DEFAULT NULL COMMENT '浏览器标签名称',
  `system_name` varchar(100) DEFAULT NULL COMMENT '系统名称',
  `system_logo` varchar(255) DEFAULT NULL COMMENT '系统logo（存储logo URL/Base64）',
  `system_intro` varchar(500) DEFAULT NULL COMMENT '系统介绍',
  `copyright_info` varchar(200) DEFAULT NULL COMMENT '版权显示文本',
  `system_layout` varchar(50) DEFAULT 'business-workbench' COMMENT '系统布局（默认 business-workbench）',
  `system_theme` varchar(50) DEFAULT 'light' COMMENT '系统主题（light-亮色，dark-暗色，auto-跟随系统等）',
  `theme_config` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '主题配置',
  `create_dept` bigint unsigned DEFAULT NULL,
  `default_business_datasource_id` bigint DEFAULT NULL COMMENT '默认业务数据源ID',
  `default_business_datasource_code` varchar(64) DEFAULT NULL COMMENT '默认业务数据源编码',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_name_active` (`tenant_name`,`del_flag`),
  KEY `idx_tenant_status` (`tenant_status`)
) ENGINE=InnoDB AUTO_INCREMENT=9002 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_user`
--

DROP TABLE IF EXISTS `sys_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `username` varchar(50) NOT NULL COMMENT '用户名（租户内唯一）',
  `real_name` varchar(50) DEFAULT NULL COMMENT '用户真实姓名',
  `user_type` tinyint DEFAULT '1' COMMENT '用户类型（0-系统管理员，1-租户管理员，2-普通用户）',
  `user_client` varchar(20) DEFAULT NULL COMMENT '用户触点（app/pc/h5/wechat）',
  `allowed_clients` varchar(500) DEFAULT NULL COMMENT '允许登录的客户端编码，逗号分隔（如 pc,app），空表示不限制',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `id_card` varchar(18) DEFAULT NULL COMMENT '身份证号',
  `gender` tinyint DEFAULT '0' COMMENT '性别（0-未知，1-男，2-女）',
  `password` varchar(100) NOT NULL COMMENT '密码（加密存储）',
  `password_version` bigint NOT NULL DEFAULT '0' COMMENT '密码凭证版本，每次改密原子递增',
  `password_changed_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最近密码变更时间',
  `salt` varchar(50) DEFAULT NULL COMMENT '密码盐值',
  `user_status` tinyint NOT NULL DEFAULT '1' COMMENT '用户状态（0-禁用，1-正常，2-锁定）',
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像URL',
  `region_code` varchar(10) DEFAULT NULL COMMENT '行政区划编码',
  `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
  `last_login_ip` varchar(50) DEFAULT NULL COMMENT '最后登录IP',
  `login_count` int DEFAULT '0' COMMENT '登录次数',
  `force_password_change` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否必须修改密码',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `sys_user_unique_active` (`tenant_id`,`username`,`del_flag`),
  KEY `idx_tenant_status` (`tenant_id`,`user_status`),
  KEY `idx_user_type` (`user_type`),
  KEY `idx_region_code` (`region_code`)
) ENGINE=InnoDB AUTO_INCREMENT=77 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_user_org`
--

DROP TABLE IF EXISTS `sys_user_org`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_user_org` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `org_id` bigint NOT NULL COMMENT '组织ID',
  `is_main` tinyint DEFAULT '0' COMMENT '是否主组织（0-否，1-是）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_org` (`tenant_id`,`user_id`,`org_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_org_id` (`org_id`),
  KEY `idx_user_main_org` (`user_id`,`is_main`) COMMENT '查询用户主组织优化',
  KEY `idx_sys_user_org_user_tenant` (`user_id`,`tenant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=75 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户-组织关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_user_org_role`
--

DROP TABLE IF EXISTS `sys_user_org_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_user_org_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `org_id` bigint NOT NULL COMMENT '组织ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_org_role` (`tenant_id`,`user_id`,`org_id`,`role_id`),
  KEY `idx_user_org` (`tenant_id`,`user_id`,`org_id`),
  KEY `idx_org_role` (`tenant_id`,`org_id`,`role_id`),
  KEY `idx_role_id` (`role_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_org_id` (`org_id`)
) ENGINE=InnoDB AUTO_INCREMENT=41 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户组织内角色授权表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_user_password_history`
--

DROP TABLE IF EXISTS `sys_user_password_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_user_password_history` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL COMMENT '用户归属租户ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `password_hash` varchar(100) NOT NULL COMMENT '历史密码单向哈希',
  `changed_time` datetime NOT NULL COMMENT '该密码被替换的时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_password_history_user_time` (`tenant_id`,`user_id`,`changed_time`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户密码历史（安全保留窗口）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_user_post`
--

DROP TABLE IF EXISTS `sys_user_post`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_user_post` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `post_id` bigint NOT NULL COMMENT '岗位ID',
  `is_main` tinyint DEFAULT '0' COMMENT '是否主岗（0-否，1-是）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_post` (`tenant_id`,`user_id`,`post_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_post_id` (`post_id`),
  KEY `idx_user_main_post` (`user_id`,`is_main`) COMMENT '查询用户主岗优化',
  KEY `idx_sys_user_post_user_tenant` (`user_id`,`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户-岗位关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_user_role`
--

DROP TABLE IF EXISTS `sys_user_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_user_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`tenant_id`,`user_id`,`role_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB AUTO_INCREMENT=39 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户-角色关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_user_social`
--

DROP TABLE IF EXISTS `sys_user_social`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_user_social` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `platform` varchar(50) NOT NULL COMMENT '平台类型',
  `connection_id` bigint DEFAULT NULL COMMENT '企业协同连接ID（应用层迁移回填，歧义进入阻塞清单）',
  `external_enterprise_id` varchar(100) DEFAULT NULL COMMENT '外部企业ID（企业微信CorpId等）',
  `managed_by_sync` tinyint NOT NULL DEFAULT '0' COMMENT '是否由目录同步管理：0否 1是',
  `external_status` varchar(32) DEFAULT NULL COMMENT '外部账号状态：ACTIVE/DISABLED/DELETED',
  `source_hash` varchar(64) DEFAULT NULL COMMENT '外部资料快照哈希（用于变更检测）',
  `last_sync_time` datetime DEFAULT NULL COMMENT '最近同步时间',
  `uuid` varchar(255) NOT NULL COMMENT '第三方用户唯一标识',
  `username` varchar(100) DEFAULT NULL COMMENT '第三方用户名',
  `nickname` varchar(100) DEFAULT NULL COMMENT '第三方昵称',
  `avatar` varchar(500) DEFAULT NULL COMMENT '头像',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `access_token` varchar(500) DEFAULT NULL COMMENT '访问令牌',
  `refresh_token` varchar(500) DEFAULT NULL COMMENT '刷新令牌',
  `expire_time` datetime DEFAULT NULL COMMENT '令牌过期时间',
  `bind_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_social_conn_uuid_active` (`tenant_id`,`connection_id`,`uuid`,`del_flag`),
  UNIQUE KEY `uk_user_social_conn_user_active` (`tenant_id`,`connection_id`,`user_id`,`del_flag`),
  KEY `idx_user_id` (`user_id`) USING BTREE,
  KEY `idx_tenant` (`tenant_id`) USING BTREE,
  KEY `idx_user_social_platform_uuid` (`platform`,`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户三方账号绑定表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_user_tenant`
--

DROP TABLE IF EXISTS `sys_user_tenant`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_user_tenant` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `member_type` tinyint NOT NULL DEFAULT '2' COMMENT '成员类型（1-租户管理员，2-普通成员）',
  `is_default` tinyint NOT NULL DEFAULT '0' COMMENT '是否默认租户（0-否，1-是）',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态（0-禁用，1-正常）',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_tenant` (`tenant_id`,`user_id`),
  KEY `idx_user_tenant_user` (`user_id`,`status`),
  KEY `idx_user_tenant_default` (`user_id`,`is_default`)
) ENGINE=InnoDB AUTO_INCREMENT=59 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户-租户成员关系表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `worker_node`
--

DROP TABLE IF EXISTS `worker_node`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `worker_node` (
  `ID` bigint NOT NULL AUTO_INCREMENT COMMENT 'auto increment id',
  `HOST_NAME` varchar(64) NOT NULL COMMENT 'host name',
  `PORT` varchar(64) NOT NULL COMMENT 'port',
  `TYPE` int NOT NULL COMMENT 'node type: ACTUAL or CONTAINER',
  `LAUNCH_DATE` date NOT NULL COMMENT 'launch date',
  `MODIFIED` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'modified time',
  `CREATED` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='DB WorkerID Assigner for UID Generator';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping events for database 'forge_admin_v120_pure'
--

--
-- Dumping routines for database 'forge_admin_v120_pure'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-10-13 10:55:49

-- Data
-- MySQL dump 10.13  Distrib 5.7.24, for osx11.1 (x86_64)
--
-- Host: 120.48.96.178    Database: forge_admin_v120_pure
-- ------------------------------------------------------
-- Server version	8.0.32-2.0.0.2

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Dumping data for table `ai_agent`
--
-- ORDER BY:  `id`

LOCK TABLES `ai_agent` WRITE;
/*!40000 ALTER TABLE `ai_agent` DISABLE KEYS */;
INSERT INTO `ai_agent` (`id`, `tenant_id`, `agent_name`, `agent_code`, `description`, `system_prompt`, `provider_id`, `model_name`, `model_selection_mode`, `route_policy_id`, `temperature`, `max_tokens`, `extra_config`, `greeting`, `preset_questions`, `max_iters`, `knowledge_ids`, `rag_mode`, `tool_group_mode`, `asr_model_id`, `tts_model_id`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `del_flag`, `create_dept`) VALUES (1,1,'大屏生成助手','dashboard_generator','根据用户需求自动生成数据可视化大屏布局','你是一个资深数据可视化大屏设计专家。你需要自主根据业务场景智能决策大屏布局结构与视觉层级，无需依赖固定模板；必须自动识别并优先使用系统提供的可用组件库，严格从给定组件列表中选择，不使用未注册组件；涉及 KPI、核心指标展示时，4-6 个概览指标优先使用 KpiGroup，1-3 个重点指标再优先使用 KpiCard、FlipperNumber 等专用组件；布局与视觉效果需最大化使用动态背景、装饰边框、PanelFrame 模块框、动态标题、3D/光效类新增组件，尽量复用丰富组件库，让大屏视觉更饱满、科技感更强、层次更立体；整体设计遵循主次分明、重点突出、非对称错落、留白透气的专业指挥舱大屏标准，并且只输出一个合法 JSON 对象。\n\n## 画布\n- 尺寸: {{canvasWidth}}px × {{canvasHeight}}px\n- 坐标原点在左上角，x 向右增大，y 向下增大\n- 背景色: {{backgroundColor}}\n- 风格: {{styleLabel}}，{{backgroundSuggestion}}，{{textColorSuggestion}}\n\n## 绝对硬性要求\n1. 只输出 JSON 对象，不要 markdown、解释、注释、前后缀。\n2. 每个组件对象必须完整包含 key、x、y、w、h、option（或 title）等字段，字段名和值必须一一对应，禁止把值和字段名混写。错误示例: \"h\"y\": 65\"title\": \"xxx\" — 这会导致 JSON 解析失败。正确: \"h\": 370, \"y\": 655, \"title\": \"实时告警记录\"。\n3. JSON 字符串必须使用英文双引号包裹，数字不加引号。\n4. 所有 key 必须来自\"可用组件\"。\n5. JSON 字段名必须独立书写，禁止把字段名拼进字符串值；例如必须写 \"title\": \"产品合格率\", \"option\": {...}，禁止写 \"title\": \"产品合格率option\": {...}。\n6. 所有 x/y/w/h 必须是数字，且 x+w <= {{canvasWidth}}，y+h <= {{canvasHeight}}。\n7. 除\"模块框/边框与其紧随其后的被包裹组件\"外，其他组件矩形禁止重叠。\n8. 模块优先用 PanelFrame 包裹其后一个图表、地图或表格组件；如果不用 PanelFrame 才使用 Border01-Border13。PanelFrame 或边框都必须放在被包裹组件前面。\n9. 如果不能确定某组件是否可用，不要使用它。\n10. 输出前必须自检：JSON 结构完整、括号配对、没有字段值错位、每个组件 key/x/y/w/h 完整。\n\n## 颜色统一要求\n- 为整个大屏选定一个统一的主题色（accentColor），标题、模块框、指标卡、图表的强调色围绕此颜色协调搭配。\n- 如果用户在需求中指定了颜色，优先使用用户指定的颜色；用户未指定时按主题自动匹配：智慧城市/数字孪生→#25d8ff 青蓝、安全生产/工业→#ffcf5a 橙黄、能源环保→#47ffb2 翠绿、金融商务→#ff9f43 金橙、医疗健康→#38bdf8 天蓝、政务管理→#3b82f6 深蓝、科技/互联网→#a78bfa 紫、物流交通→#f59e0b 琥珀。\n- 深色背景下：标题 accentColor=主题色，模块框 accentColor=主题色、borderColor=主题色深变，KpiCard 如使用则 accentColor=主题色、borderColor=主题色深变、numberColor=#f7fbff、labelColor=主题色浅变、backgroundColor=深色半透明。\n\n## 视觉设计目标\n- 禁止简单图表堆叠、禁止均等网格平铺、禁止模块尺寸全部一致，要做成真实指挥舱沉浸式大屏。\n- 标题优先使用 ScreenTitle 系列（ScreenTitle、ScreenTitle02-08），每次选择不同风格。标题风格参考: ScreenTitle03=星环光晕, ScreenTitle04=锋刃工业, ScreenTitle05=两侧装饰框, ScreenTitle06=动态脉冲发光, ScreenTitle07=晶体切面, ScreenTitle08=控制台轨道节点。\n- 图表、地图、表格等模块统一用同一种 PanelFrame 风格包裹，增强整体高级感；优先选用光效、扫描、辉光类边框组件。\n- 顶部如果需要 4-6 个概览指标，优先使用 KpiGroup；如果只需要 1-3 个重点指标，再优先使用 KpiCard / FlipperNumber。\n- 主视觉必须有中心重心：地图、三维地球、大尺寸趋势图、关系图、综合大屏主图，杜绝全屏平均分散小图。\n- 深色大屏且画布足够时，优先添加 1 个 GlowBackdrop 作为低层发光背景，增强科技氛围；GlowBackdrop 不要被 PanelFrame 或 Border 包裹。\n- 两侧及空余区域丰富填充：排行、占比、趋势、漏斗、雷达、桑基图、热力图、词云、滚动表格、时钟装饰等，优先复用新增增强组件，如 SectionHeader、StatusBadgeList、DataPairList、MiniTrendCard、TimelineList、StepFlow、DividerLine、GlowBackdrop。\n- 数据名称、文案贴合业务主题，数值混合整数、小数、百分比，避免单调统一。\n- 图表类型多样化，不局限柱状/折线/饼图，优先多用高级异形图表提升质感。\n\n## 智能高级布局规范\n### 布局核心准则\n无需固定坐标硬编码，按画布宽高**自适应智能分版**；严格遵循「**主次分层、非对称错落、一大带多小、宽窄不等分、留白透气**」，拒绝呆板三列均分、整齐网格平铺。\n\n### 可选高级版式（AI自动匹配业务任选其一）\n1. 中心主图环绕式：中间超大地图/三维地球/主视觉大图，四周错落排布KPI、排行、图表、表格，大小模块穿插，打造指挥舱沉浸感。\n2. 上标题-KPI + 中下非对称宫格：顶部通栏标题，下接横向KPI指标区；中下区域采用左宽右窄、左大右小、多宫格错落布局，不做等宽拆分。\n3. 左主右辅纵深式：左侧占比55%-65%放置核心主图，右侧纵向切分多块小模块，放置排行、分析图、实时列表，形成视觉纵深层次。\n4. 窄侧边栏+宽主内容式：左右一侧做窄边栏，放置滚动表格、排名、时钟、公告装饰；剩余大面积做主视觉+多模块分析，适配政务、监控、智慧城市场景。\n5. 通栏分层悬浮式：中间通栏主图表，上下穿插错落悬浮小模块，搭配动态背景、边角装饰、多层辉光边框，强化立体层次感。\n\n### 通用布局细则\n- 标题通栏居中，左右预留合理边距，不贴边拥挤。\n- KPI指标区按指标数量自适应排布：4-6 个概览指标优先 KpiGroup，1-3 个指标优先 KpiCard / FlipperNumber；可等宽可错落宽窄，不强制固定个数。\n- 所有模块之间保留均匀间距，四周留白不贴边、不挤堆。\n- 优先大小混搭、高低错落，用PanelFrame和装饰边框做视觉嵌套分层。\n- 小画布精简克制，保留标题+核心KPI+1张主图+2-3个辅助图，不强行堆砌组件。\n- 布局时主动复用动态背景、光效边框、装饰角标、3D特效、翻牌数字等组件增强质感；一般搭配 2-4 个增强组件即可，丰富但不要过载，不要在同一角落堆叠多个摘要型模块。\n\n## 当前画布已有内容\n{{canvasContext}}\n\n## 业务定义与绑定数据集上下文\n{{businessContext}}\n\n## 用户需求\n{{prompt}}\n\n## 可用组件\n{{componentCatalog}}\n\n## 输出格式\n{\n  \"title\": \"大屏标题\",\n  \"canvasConfig\": {\n    \"width\": {{canvasWidth}},\n    \"height\": {{canvasHeight}},\n    \"background\": \"{{backgroundColor}}\"\n  },\n  \"components\": [\n    {\n      \"key\": \"组件key\",\n      \"x\": 20,\n      \"y\": 100,\n      \"w\": 500,\n      \"h\": 300,\n      \"title\": \"组件标题\",\n      \"option\": {},\n      \"request\": {\n        \"datasetId\": 123,\n        \"datasetName\": \"数据集名称\",\n        \"datasetFields\": [\"维度字段\", \"指标字段\"],\n        \"datasetMapping\": {\n          \"mode\": \"auto\",\n          \"fieldMap\": {},\n          \"syncHeader\": true\n        }\n      }\n    }\n  ]\n}\n\n## option 规则\n- 如果“业务定义与绑定数据集上下文”中提供了 datasets，图表、表格、排行、指标类组件应优先绑定其中的 datasetId。只有标题、装饰、说明或无法匹配数据集的组件才使用纯静态数据。\n- 绑定数据集时必须在组件上输出 request.datasetId；datasetFields 只能使用该数据集 fields 中存在的 fieldName。request.datasetMapping.fieldMap 可声明 category/value/name/time/series 等组件语义字段到数据集 fieldName 的映射。\n- 绑定了 request.datasetId 的组件仍可提供 option.dataset 作为兜底预览数据，但运行时会由数据集查询刷新。\n- ECharts 和 VChart 图表只填 option.dataset，不要写 series、xAxis、yAxis、tooltip。\n- 普通图表 dataset: { \"dimensions\": [\"类目\", \"系列1\", \"系列2\"], \"source\": [{\"类目\": \"1月\", \"系列1\": 820.5, \"系列2\": 320.2}] }\n- 饼图 PieCommon/PieCircle/VChartPie 只能有 2 个 dimensions: 名称和值。\n- Radar: { \"dataset\": { \"radarIndicator\": [{\"name\": \"指标\", \"max\": 100}], \"seriesData\": [{\"name\": \"当前\", \"value\": [80, 90]}] } }\n- ScreenTitle 系列: { \"dataset\": \"大屏标题\", \"subtitle\": \"实时监控\", \"fontSize\": 46, \"accentColor\": \"#25d8ff\", \"showBorder\": true, \"showBackground\": true, \"showDecorations\": true }\n- KpiGroup: { \"dataset\": [{\"title\": \"今日产量\", \"value\": 12345.6, \"unit\": \"件\", \"trend\": \"+12.5%\"}], \"columns\": 4, \"accentColor\": \"#25d8ff\" }\n- KpiCard: { \"title\": \"今日产量\", \"dataset\": 12345.6, \"unit\": \"件\", \"trendValue\": 12.5, \"trendType\": \"up\", \"iconText\": \"KPI\", \"accentColor\": \"#25d8ff\", \"borderColor\": \"#1c95ff\", \"numberColor\": \"#f7fbff\", \"labelColor\": \"#b8d7ff\", \"backgroundColor\": \"#061a3acc\" }\n- SectionHeader: { \"title\": \"模块标题\", \"subtitle\": \"SECTION OVERVIEW\", \"unit\": \"单位\", \"accentColor\": \"#25d8ff\" }\n- StatusBadgeList: { \"dataset\": [{\"label\": \"运行中\", \"value\": 126, \"color\": \"#47ffb2\"}], \"columns\": 4, \"unit\": \"台\" }\n- DataPairList: { \"dataset\": [{\"label\": \"设备编号\", \"value\": \"CNC-01\"}], \"columns\": 2 }\n- MiniTrendCard: { \"title\": \"实时产量\", \"dataset\": 12850, \"unit\": \"件\", \"trend\": \"+12.5%\", \"points\": [18, 28, 24, 42, 38, 56, 68] }\n- TimelineList: { \"dataset\": [{\"time\": \"10:23\", \"title\": \"CNC-01 温度过高\", \"level\": \"高\", \"status\": \"danger\"}] }\n- StepFlow: { \"dataset\": [{\"title\": \"投料\", \"status\": \"done\"}, {\"title\": \"加工\", \"status\": \"active\"}, {\"title\": \"质检\", \"status\": \"pending\"}] }\n- DividerLine: { \"direction\": \"horizontal\", \"thickness\": 2, \"accentColor\": \"#25d8ff\", \"secondColor\": \"#47ffb2\" }\n- GlowBackdrop: { \"variant\": \"reactor\", \"accentColor\": \"#25d8ff\", \"secondColor\": \"#47ffb2\", \"thirdColor\": \"#ffcf5a\", \"opacity\": 0.9 }。variant 可选 reactor、grid、wing、stargate、radar，且应作为背景低层组件使用。\n- PanelFrame 系列: { \"title\": \"模块标题\", \"unit\": \"单位\", \"accentColor\": \"#25d8ff\", \"borderColor\": \"#1d70ff\" }\n- TextCommon/TextGradient: { \"dataset\": \"文字\", \"fontSize\": 36, \"fontColor\": \"#ffffff\", \"fontWeight\": \"bold\", \"textAlign\": \"center\", \"letterSpacing\": 4 }\n- FlipperNumber: { \"dataset\": 12345.6, \"unit\": \"万元\" }\n- RankProgressList: { \"dataset\": [{\"name\": \"项目A\", \"value\": 96.8}], \"unit\": \"%\", \"max\": 100 }\n- TableList: { \"dataset\": [{\"name\": \"项目A\", \"value\": 100.5}] }\n- TableScrollBoard: { \"header\": [\"列1\", \"列2\"], \"dataset\": [[\"值1\", \"值2\"]] }\n- WordCloud/VChartWordCloud: { \"dataset\": [{\"name\": \"关键词\", \"value\": 100}] }\n- Border01-Border13、Clock、CountDown 不要 option 字段；PanelFrame/PanelFrame02 需要 option.title、option.accentColor、option.borderColor。\n\n## 自检\n输出前检查：JSON 可解析；没有尾逗号；没有中文占位数字；组件不越界；非模块框/边框组件不重叠；PanelFrame/边框只包裹紧随其后的组件；GlowBackdrop 不被 PanelFrame/Border 包裹；标题优先用 ScreenTitle；整体有主图、指标、辅助分析；复用足量动态/特效/装饰组件；版式非对称有错落层级；整个大屏颜色统一协调。',2075737070143303682,'deepseek-v4-pro','PINNED',NULL,0.70,4000,'{\"skills\": [], \"mcpTools\": [], \"contextCount\": 0, \"openingStatement\": \"\", \"suggestedQuestions\": []}',NULL,NULL,10,NULL,'none','all',NULL,NULL,'0',1,'2026-04-13 17:38:55',1,'2026-07-11 14:24:45',0,NULL);
/*!40000 ALTER TABLE `ai_agent` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_agent_event`
--
-- ORDER BY:  `id`

LOCK TABLES `ai_agent_event` WRITE;
/*!40000 ALTER TABLE `ai_agent_event` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_agent_event` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_agent_generate_record`
--
-- ORDER BY:  `id`

LOCK TABLES `ai_agent_generate_record` WRITE;
/*!40000 ALTER TABLE `ai_agent_generate_record` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_agent_generate_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_agent_skill`
--
-- ORDER BY:  `id`

LOCK TABLES `ai_agent_skill` WRITE;
/*!40000 ALTER TABLE `ai_agent_skill` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_agent_skill` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_agent_tool_config`
--
-- ORDER BY:  `id`

LOCK TABLES `ai_agent_tool_config` WRITE;
/*!40000 ALTER TABLE `ai_agent_tool_config` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_agent_tool_config` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_agent_tool_permission`
--
-- ORDER BY:  `id`

LOCK TABLES `ai_agent_tool_permission` WRITE;
/*!40000 ALTER TABLE `ai_agent_tool_permission` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_agent_tool_permission` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_application_capability`
--
-- ORDER BY:  `id`

LOCK TABLES `ai_application_capability` WRITE;
/*!40000 ALTER TABLE `ai_application_capability` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_application_capability` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_application_integration`
--
-- ORDER BY:  `id`

LOCK TABLES `ai_application_integration` WRITE;
/*!40000 ALTER TABLE `ai_application_integration` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_application_integration` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_business_app`
--
-- ORDER BY:  `id`

LOCK TABLES `ai_business_app` WRITE;
/*!40000 ALTER TABLE `ai_business_app` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_business_app` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_business_application`
--
-- ORDER BY:  `id`

LOCK TABLES `ai_business_application` WRITE;
/*!40000 ALTER TABLE `ai_business_application` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_business_application` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_business_application_object`
--
-- ORDER BY:  `id`

LOCK TABLES `ai_business_application_object` WRITE;
/*!40000 ALTER TABLE `ai_business_application_object` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_business_application_object` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_business_application_publish_run`
--
-- ORDER BY:  `id`

LOCK TABLES `ai_business_application_publish_run` WRITE;
/*!40000 ALTER TABLE `ai_business_application_publish_run` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_business_application_publish_run` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_business_binding`
--
-- ORDER BY:  `id`

LOCK TABLES `ai_business_binding` WRITE;
/*!40000 ALTER TABLE `ai_business_binding` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_business_binding` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_business_document_config`
--
-- ORDER BY:  `id`

LOCK TABLES `ai_business_document_config` WRITE;
/*!40000 ALTER TABLE `ai_business_document_config` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_business_document_config` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_business_event_outbox`
--
-- ORDER BY:  `id`

LOCK TABLES `ai_business_event_outbox` WRITE;
/*!40000 ALTER TABLE `ai_business_event_outbox` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_business_event_outbox` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_business_event_sequence`
--
-- ORDER BY:  `aggregate_key`

LOCK TABLES `ai_business_event_sequence` WRITE;
/*!40000 ALTER TABLE `ai_business_event_sequence` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_business_event_sequence` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_business_extension`
--
-- ORDER BY:  `id`

LOCK TABLES `ai_business_extension` WRITE;
/*!40000 ALTER TABLE `ai_business_extension` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_business_extension` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_business_field_template`
--
-- ORDER BY:  `id`

LOCK TABLES `ai_business_field_template` WRITE;
/*!40000 ALTER TABLE `ai_business_field_template` DISABLE KEYS */;
INSERT INTO `ai_business_field_template` (`id`, `tenant_id`, `template_code`, `template_name`, `suite_code`, `field_type`, `field_schema`, `status`, `sort_order`, `description`, `options`, `create_by`, `create_time`, `create_dept`, `update_by`, `update_time`) VALUES (1910000000000000501,1,'common_customer_name','客户名称','COMMON','TEXT','{\"field\": \"customerName\", \"label\": \"客户名称\", \"width\": 180, \"length\": 128, \"remark\": \"客户名称\", \"dataType\": \"varchar\", \"dictType\": \"\", \"readonly\": false, \"required\": true, \"sortable\": false, \"precision\": 2, \"queryType\": \"like\", \"sortOrder\": 1, \"columnName\": \"customer_name\", \"exportable\": true, \"importable\": true, \"primaryKey\": false, \"searchable\": true, \"fieldStatus\": \"ENABLED\", \"formVisible\": true, \"listVisible\": true, \"systemField\": false, \"defaultValue\": null, \"autoIncrement\": false, \"componentType\": \"input\", \"sensitiveType\": \"NONE\", \"encryptAlgorithm\": \"\", \"businessFieldType\": \"TEXT\"}',1,1,'常用客户主字段，可作为对象显示字段','{\"stage\": \"business-object-designer\", \"builtin\": true}',1,'2026-05-29 14:39:55',1,1,'2026-05-29 14:39:55'),(1910000000000000502,1,'common_contact_phone','联系电话','COMMON','PHONE','{\"field\": \"contactPhone\", \"label\": \"联系电话\", \"width\": 140, \"length\": 32, \"remark\": \"联系电话\", \"dataType\": \"varchar\", \"dictType\": \"\", \"readonly\": false, \"required\": false, \"sortable\": false, \"precision\": 2, \"queryType\": \"like\", \"sortOrder\": 2, \"columnName\": \"contact_phone\", \"exportable\": true, \"importable\": true, \"primaryKey\": false, \"searchable\": true, \"fieldStatus\": \"ENABLED\", \"formVisible\": true, \"listVisible\": true, \"systemField\": false, \"defaultValue\": null, \"autoIncrement\": false, \"componentType\": \"input\", \"sensitiveType\": \"PHONE\", \"encryptAlgorithm\": \"\", \"businessFieldType\": \"PHONE\"}',1,2,'常用联系电话字段，默认启用手机号脱敏','{\"stage\": \"business-object-designer\", \"builtin\": true}',1,'2026-05-29 14:39:55',1,1,'2026-05-29 14:39:55'),(1910000000000000503,1,'common_customer_level','客户等级','COMMON','DICT','{\"field\": \"customerLevel\", \"label\": \"客户等级\", \"width\": 140, \"length\": 32, \"remark\": \"客户等级\", \"dataType\": \"varchar\", \"dictType\": \"crm_customer_level\", \"readonly\": false, \"required\": false, \"sortable\": false, \"precision\": 2, \"queryType\": \"eq\", \"sortOrder\": 3, \"columnName\": \"customer_level\", \"exportable\": true, \"importable\": true, \"primaryKey\": false, \"searchable\": true, \"fieldStatus\": \"ENABLED\", \"formVisible\": true, \"listVisible\": true, \"systemField\": false, \"defaultValue\": null, \"autoIncrement\": false, \"componentType\": \"select\", \"sensitiveType\": \"NONE\", \"encryptAlgorithm\": \"\", \"businessFieldType\": \"DICT\"}',1,3,'客户分层字段，默认使用CRM客户等级字典','{\"stage\": \"business-object-designer\", \"builtin\": true}',1,'2026-05-29 14:39:55',1,1,'2026-05-29 14:39:55'),(1910000000000000504,1,'common_owner_user','负责人','COMMON','USER','{\"field\": \"ownerUserId\", \"label\": \"负责人\", \"width\": 140, \"length\": null, \"remark\": \"业务负责人\", \"dataType\": \"bigint\", \"dictType\": \"\", \"readonly\": false, \"required\": false, \"sortable\": false, \"precision\": null, \"queryType\": \"eq\", \"sortOrder\": 4, \"columnName\": \"owner_user_id\", \"exportable\": true, \"importable\": true, \"primaryKey\": false, \"searchable\": true, \"fieldStatus\": \"ENABLED\", \"formVisible\": true, \"listVisible\": true, \"systemField\": false, \"defaultValue\": null, \"autoIncrement\": false, \"componentType\": \"userSelect\", \"sensitiveType\": \"NONE\", \"encryptAlgorithm\": \"\", \"businessFieldType\": \"USER\"}',1,4,'对象负责人字段，用于权限和数据归属','{\"stage\": \"business-object-designer\", \"builtin\": true}',1,'2026-05-29 14:39:55',1,1,'2026-05-29 14:39:55'),(1910000000000000505,1,'common_owner_dept','所属部门','COMMON','DEPT','{\"field\": \"ownerDeptId\", \"label\": \"所属部门\", \"width\": 140, \"length\": null, \"remark\": \"所属部门\", \"dataType\": \"bigint\", \"dictType\": \"\", \"readonly\": false, \"required\": false, \"sortable\": false, \"precision\": null, \"queryType\": \"eq\", \"sortOrder\": 5, \"columnName\": \"owner_dept_id\", \"exportable\": true, \"importable\": true, \"primaryKey\": false, \"searchable\": true, \"fieldStatus\": \"ENABLED\", \"formVisible\": true, \"listVisible\": true, \"systemField\": false, \"defaultValue\": null, \"autoIncrement\": false, \"componentType\": \"orgTreeSelect\", \"sensitiveType\": \"NONE\", \"encryptAlgorithm\": \"\", \"businessFieldType\": \"DEPT\"}',1,5,'对象所属部门字段，用于部门维度筛选','{\"stage\": \"business-object-designer\", \"builtin\": true}',1,'2026-05-29 14:39:55',1,1,'2026-05-29 14:39:55'),(1910000000000000506,1,'common_region_code','所属地区','COMMON','REGION','{\"field\": \"regionCode\", \"label\": \"所属地区\", \"width\": 160, \"length\": 32, \"remark\": \"所属地区\", \"dataType\": \"varchar\", \"dictType\": \"\", \"readonly\": false, \"required\": false, \"sortable\": false, \"precision\": 2, \"queryType\": \"eq\", \"sortOrder\": 6, \"columnName\": \"region_code\", \"exportable\": true, \"importable\": true, \"primaryKey\": false, \"searchable\": true, \"fieldStatus\": \"ENABLED\", \"formVisible\": true, \"listVisible\": true, \"systemField\": false, \"defaultValue\": null, \"autoIncrement\": false, \"componentType\": \"regionTreeSelect\", \"sensitiveType\": \"NONE\", \"encryptAlgorithm\": \"\", \"businessFieldType\": \"REGION\"}',1,6,'行政区划字段，支持区域树选择','{\"stage\": \"business-object-designer\", \"builtin\": true}',1,'2026-05-29 14:39:55',1,1,'2026-05-29 14:39:55'),(1910000000000000507,1,'common_remark','备注','COMMON','MULTILINE','{\"field\": \"remark\", \"label\": \"备注\", \"width\": 220, \"length\": null, \"remark\": \"备注\", \"dataType\": \"text\", \"dictType\": \"\", \"readonly\": false, \"required\": false, \"sortable\": false, \"precision\": 2, \"queryType\": \"like\", \"sortOrder\": 7, \"columnName\": \"remark\", \"exportable\": true, \"importable\": true, \"primaryKey\": false, \"searchable\": false, \"fieldStatus\": \"ENABLED\", \"formVisible\": true, \"listVisible\": true, \"systemField\": false, \"defaultValue\": null, \"autoIncrement\": false, \"componentType\": \"textarea\", \"sensitiveType\": \"NONE\", \"encryptAlgorithm\": \"\", \"businessFieldType\": \"MULTILINE\"}',1,7,'通用备注说明字段','{\"stage\": \"business-object-designer\", \"builtin\": true}',1,'2026-05-29 14:39:55',1,1,'2026-05-29 14:39:55');
/*!40000 ALTER TABLE `ai_business_field_template` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_business_flow_callback_inbox`
--
-- ORDER BY:  `id`

LOCK TABLES `ai_business_flow_callback_inbox` WRITE;
/*!40000 ALTER TABLE `ai_business_flow_callback_inbox` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_business_flow_callback_inbox` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_business_flow_instance_link`
--
-- ORDER BY:  `id`

LOCK TABLES `ai_business_flow_instance_link` WRITE;
/*!40000 ALTER TABLE `ai_business_flow_instance_link` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_business_flow_instance_link` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_business_flow_remote_command`
--
-- ORDER BY:  `id`

LOCK TABLES `ai_business_flow_remote_command` WRITE;
/*!40000 ALTER TABLE `ai_business_flow_remote_command` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_business_flow_remote_command` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_business_object`
--
-- ORDER BY:  `id`

LOCK TABLES `ai_business_object` WRITE;
/*!40000 ALTER TABLE `ai_business_object` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_business_object` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_business_object_relation`
--
-- ORDER BY:  `id`

LOCK TABLES `ai_business_object_relation` WRITE;
/*!40000 ALTER TABLE `ai_business_object_relation` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_business_object_relation` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_business_process`
--
-- ORDER BY:  `id`

LOCK TABLES `ai_business_process` WRITE;
/*!40000 ALTER TABLE `ai_business_process` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_business_process` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_business_process_node_run`
--
-- ORDER BY:  `id`

LOCK TABLES `ai_business_process_node_run` WRITE;
/*!40000 ALTER TABLE `ai_business_process_node_run` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_business_process_node_run` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_business_process_run`
--
-- ORDER BY:  `id`

LOCK TABLES `ai_business_process_run` WRITE;
/*!40000 ALTER TABLE `ai_business_process_run` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_business_process_run` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_business_quantity_balance`
--
-- ORDER BY:  `id`

LOCK TABLES `ai_business_quantity_balance` WRITE;
/*!40000 ALTER TABLE `ai_business_quantity_balance` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_business_quantity_balance` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_business_quantity_ledger`
--
-- ORDER BY:  `id`

LOCK TABLES `ai_business_quantity_ledger` WRITE;
/*!40000 ALTER TABLE `ai_business_quantity_ledger` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_business_quantity_ledger` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_business_quantity_lock`
--
-- ORDER BY:  `id`

LOCK TABLES `ai_business_quantity_lock` WRITE;
/*!40000 ALTER TABLE `ai_business_quantity_lock` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_business_quantity_lock` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_business_suite`
--
-- ORDER BY:  `id`

LOCK TABLES `ai_business_suite` WRITE;
/*!40000 ALTER TABLE `ai_business_suite` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_business_suite` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_business_trigger`
--
-- ORDER BY:  `id`

LOCK TABLES `ai_business_trigger` WRITE;
/*!40000 ALTER TABLE `ai_business_trigger` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_business_trigger` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_capability`
--
-- ORDER BY:  `id`

LOCK TABLES `ai_capability` WRITE;
/*!40000 ALTER TABLE `ai_capability` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_capability` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_capability_access_token`
--
-- ORDER BY:  `id`

LOCK TABLES `ai_capability_access_token` WRITE;
/*!40000 ALTER TABLE `ai_capability_access_token` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_capability_access_token` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_capability_approval`
--
-- ORDER BY:  `id`

LOCK TABLES `ai_capability_approval` WRITE;
/*!40000 ALTER TABLE `ai_capability_approval` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_capability_approval` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_capability_client`
--
-- ORDER BY:  `id`

LOCK TABLES `ai_capability_client` WRITE;
/*!40000 ALTER TABLE `ai_capability_client` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_capability_client` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_capability_external_identity`
--
-- ORDER BY:  `id`

LOCK TABLES `ai_capability_external_identity` WRITE;
/*!40000 ALTER TABLE `ai_capability_external_identity` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_capability_external_identity` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_capability_form_receipt`
--
-- ORDER BY:  `id`

LOCK TABLES `ai_capability_form_receipt` WRITE;
/*!40000 ALTER TABLE `ai_capability_form_receipt` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_capability_form_receipt` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_capability_grant`
--
-- ORDER BY:  `id`

LOCK TABLES `ai_capability_grant` WRITE;
/*!40000 ALTER TABLE `ai_capability_grant` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_capability_grant` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_capability_oauth_redirect_uri`
--
-- ORDER BY:  `id`

LOCK TABLES `ai_capability_oauth_redirect_uri` WRITE;
/*!40000 ALTER TABLE `ai_capability_oauth_redirect_uri` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_capability_oauth_redirect_uri` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_capability_openapi_idempotency`
--
-- ORDER BY:  `id`

LOCK TABLES `ai_capability_openapi_idempotency` WRITE;
/*!40000 ALTER TABLE `ai_capability_openapi_idempotency` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_capability_openapi_idempotency` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_capability_policy`
--
-- ORDER BY:  `id`

LOCK TABLES `ai_capability_policy` WRITE;
/*!40000 ALTER TABLE `ai_capability_policy` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_capability_policy` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_chat_message_tool_call`
--
-- ORDER BY:  `id`

LOCK TABLES `ai_chat_message_tool_call` WRITE;
/*!40000 ALTER TABLE `ai_chat_message_tool_call` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_chat_message_tool_call` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_code_rule`
--
-- ORDER BY:  `id`

LOCK TABLES `ai_code_rule` WRITE;
/*!40000 ALTER TABLE `ai_code_rule` DISABLE KEYS */;
INSERT INTO `ai_code_rule` (`id`, `tenant_id`, `rule_code`, `rule_name`, `scene`, `category`, `source_object_id`, `source_object_code`, `template`, `reset_policy`, `seq_length`, `status`, `builtin`, `in_code_list`, `version_no`, `legacy_compat_enabled`, `remark`, `options`, `create_by`, `create_time`, `create_dept`, `update_by`, `update_time`, `del_flag`) VALUES (1910000000000008601,1,'material_code','物料编码','COMMON','MATERIAL',NULL,NULL,'WL${yyyyMMddHHmmss}${seq:3}','SECOND',3,1,1,1,1,1,'适合物料编码：WL + 年月日时分秒 + 3位同秒流水',NULL,1,'2026-07-02 16:13:52',1,1,'2026-07-16 20:51:34',0),(1910000000000008602,1,'document_daily_no','通用单据日流水','COMMON','DOCUMENT',NULL,NULL,'DOC${yyyyMMdd}${seq:4}','DAY',4,1,1,1,1,1,'适合普通业务单据：DOC + 年月日 + 4位日流水',NULL,1,'2026-07-02 16:13:52',1,1,'2026-07-16 20:51:34',0),(1910000000000008603,1,'order_no','订单编号','COMMON','DOCUMENT',NULL,NULL,'ORD${yyyyMMdd}${seq:5}','DAY',5,1,1,1,1,1,'适合订单类单据：ORD + 年月日 + 5位日流水',NULL,1,'2026-07-02 16:13:52',1,1,'2026-07-16 20:51:34',0),(1910000000000008604,1,'contract_no','合同编号','COMMON','DOCUMENT',NULL,NULL,'HT${yyyyMM}${seq:5}','MONTH',5,1,1,1,1,1,'适合合同类单据：HT + 年月 + 5位月流水',NULL,1,'2026-07-02 16:13:52',1,1,'2026-07-16 20:51:34',0),(1910000000000008605,1,'customer_code','客户编码','COMMON','CUSTOMER',NULL,NULL,'C${seq:6}','NONE',6,1,1,1,1,1,'适合主数据编码：C + 6位全局流水',NULL,1,'2026-07-02 16:13:52',1,1,'2026-07-16 20:51:34',0),(1910000000000008606,1,'org_daily_no','组织日流水','COMMON','DOCUMENT',NULL,NULL,'${orgCode}${yyyyMMdd}${seq:4}','DAY',4,1,1,1,1,1,'适合按组织隔离的日流水：组织编码 + 年月日 + 4位流水',NULL,1,'2026-07-02 16:13:52',1,1,'2026-07-16 20:51:34',0),(1910000000000009301,1,'supplier_code','供应商编号','PROCUREMENT_WAREHOUSE','SUPPLIER',NULL,NULL,'SUP${seq:5}','NONE',5,1,1,1,1,1,'供应商主数据编号',NULL,1,'2026-07-03 16:30:06',1,1,'2026-07-16 20:51:34',0),(1910000000000009302,1,'warehouse_code','仓库编号','PROCUREMENT_WAREHOUSE','WAREHOUSE',NULL,NULL,'WH${seq:4}','NONE',4,1,1,1,1,1,'仓库主数据编号',NULL,1,'2026-07-03 16:30:06',1,1,'2026-07-16 20:51:34',0),(1910000000000009303,1,'purchase_no','采购单号','PROCUREMENT_WAREHOUSE','DOCUMENT',NULL,NULL,'CG${yyyyMMdd}${seq:4}','DAY',4,1,1,1,1,1,'采购单日流水',NULL,1,'2026-07-03 16:30:06',1,1,'2026-07-16 20:51:34',0),(1910000000000009304,1,'outbound_no','出库单号','PROCUREMENT_WAREHOUSE','DOCUMENT',NULL,NULL,'CK${yyyyMMdd}${seq:4}','DAY',4,1,1,1,3,1,'出库单日流水',NULL,1,'2026-07-03 16:30:06',1,1,'2026-08-02 15:45:49',0),(1910000000000009305,1,'transfer_no','调拨单号','PROCUREMENT_WAREHOUSE','DOCUMENT',NULL,NULL,'DB${yyyyMMdd}${seq:4}','DAY',4,1,1,1,1,1,'调拨单日流水',NULL,1,'2026-07-03 16:30:06',1,1,'2026-07-16 20:51:34',0);
/*!40000 ALTER TABLE `ai_code_rule` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_code_rule_segment`
--
-- ORDER BY:  `id`

LOCK TABLES `ai_code_rule_segment` WRITE;
/*!40000 ALTER TABLE `ai_code_rule_segment` DISABLE KEYS */;
INSERT INTO `ai_code_rule_segment` (`id`, `tenant_id`, `rule_id`, `segment_key`, `segment_order`, `segment_type`, `segment_value`, `variable_source`, `segment_length`, `pad_enabled`, `pad_char`, `pad_direction`, `group_enabled`, `include_in_code`, `radix_type`, `reset_enabled`, `reset_policy`, `start_value`, `exclude_ambiguous`, `excluded_characters`, `del_flag`, `create_by`, `create_time`, `create_dept`, `update_by`, `update_time`) VALUES (1,1,1910000000000008601,'date_1',2,'DATE','yyyyMMddHHmmss','CUSTOM',14,0,NULL,'LEFT',0,1,NULL,0,'NONE',1,0,NULL,0,1,'2026-07-16 20:51:34',1,1,'2026-07-16 20:51:34'),(2,1,1910000000000008601,'fixed_1',1,'FIXED','WL','CUSTOM',2,0,NULL,'LEFT',0,1,NULL,0,'NONE',1,0,NULL,0,1,'2026-07-16 20:51:34',1,1,'2026-07-16 20:51:34'),(3,1,1910000000000008601,'sequence_1',3,'SEQ',NULL,'CUSTOM',3,1,'0','LEFT',0,1,'DECIMAL',1,'HOUR',1,0,NULL,0,1,'2026-07-16 20:51:34',1,1,'2026-07-16 20:51:34'),(4,1,1910000000000008602,'date_1',2,'DATE','yyyyMMdd','CUSTOM',8,0,NULL,'LEFT',0,1,NULL,0,'NONE',1,0,NULL,0,1,'2026-07-16 20:51:34',1,1,'2026-07-16 20:51:34'),(5,1,1910000000000008602,'fixed_1',1,'FIXED','DOC','CUSTOM',3,0,NULL,'LEFT',0,1,NULL,0,'NONE',1,0,NULL,0,1,'2026-07-16 20:51:34',1,1,'2026-07-16 20:51:34'),(6,1,1910000000000008602,'sequence_1',3,'SEQ',NULL,'CUSTOM',4,1,'0','LEFT',0,1,'DECIMAL',1,'DAY',1,0,NULL,0,1,'2026-07-16 20:51:34',1,1,'2026-07-16 20:51:34'),(7,1,1910000000000008603,'date_1',2,'DATE','yyyyMMdd','CUSTOM',8,0,NULL,'LEFT',0,1,NULL,0,'NONE',1,0,NULL,0,1,'2026-07-16 20:51:34',1,1,'2026-07-16 20:51:34'),(8,1,1910000000000008603,'fixed_1',1,'FIXED','ORD','CUSTOM',3,0,NULL,'LEFT',0,1,NULL,0,'NONE',1,0,NULL,0,1,'2026-07-16 20:51:34',1,1,'2026-07-16 20:51:34'),(9,1,1910000000000008603,'sequence_1',3,'SEQ',NULL,'CUSTOM',5,1,'0','LEFT',0,1,'DECIMAL',1,'DAY',1,0,NULL,0,1,'2026-07-16 20:51:34',1,1,'2026-07-16 20:51:34'),(10,1,1910000000000008604,'date_1',2,'DATE','yyyyMM','CUSTOM',6,0,NULL,'LEFT',0,1,NULL,0,'NONE',1,0,NULL,0,1,'2026-07-16 20:51:34',1,1,'2026-07-16 20:51:34'),(11,1,1910000000000008604,'fixed_1',1,'FIXED','HT','CUSTOM',2,0,NULL,'LEFT',0,1,NULL,0,'NONE',1,0,NULL,0,1,'2026-07-16 20:51:34',1,1,'2026-07-16 20:51:34'),(12,1,1910000000000008604,'sequence_1',3,'SEQ',NULL,'CUSTOM',5,1,'0','LEFT',0,1,'DECIMAL',1,'MONTH',1,0,NULL,0,1,'2026-07-16 20:51:34',1,1,'2026-07-16 20:51:34'),(13,1,1910000000000008605,'fixed_1',1,'FIXED','C','CUSTOM',1,0,NULL,'LEFT',0,1,NULL,0,'NONE',1,0,NULL,0,1,'2026-07-16 20:51:34',1,1,'2026-07-16 20:51:34'),(14,1,1910000000000008605,'sequence_1',2,'SEQ',NULL,'CUSTOM',6,1,'0','LEFT',0,1,'DECIMAL',0,'NONE',1,0,NULL,0,1,'2026-07-16 20:51:34',1,1,'2026-07-16 20:51:34'),(15,1,1910000000000008606,'date_1',2,'DATE','yyyyMMdd','CUSTOM',8,0,NULL,'LEFT',0,1,NULL,0,'NONE',1,0,NULL,0,1,'2026-07-16 20:51:34',1,1,'2026-07-16 20:51:34'),(16,1,1910000000000008606,'sequence_1',3,'SEQ',NULL,'CUSTOM',4,1,'0','LEFT',0,1,'DECIMAL',1,'DAY',1,0,NULL,0,1,'2026-07-16 20:51:34',1,1,'2026-07-16 20:51:34'),(17,1,1910000000000008606,'system_org_1',1,'SYS_VAR','orgCode','CUSTOM',NULL,0,NULL,'LEFT',1,1,NULL,0,'NONE',1,0,NULL,0,1,'2026-07-16 20:51:34',1,1,'2026-07-16 20:51:34'),(18,1,1910000000000009301,'fixed_1',1,'FIXED','SUP','CUSTOM',3,0,NULL,'LEFT',0,1,NULL,0,'NONE',1,0,NULL,0,1,'2026-07-16 20:51:34',1,1,'2026-07-16 20:51:34'),(19,1,1910000000000009301,'sequence_1',2,'SEQ',NULL,'CUSTOM',5,1,'0','LEFT',0,1,'DECIMAL',0,'NONE',1,0,NULL,0,1,'2026-07-16 20:51:34',1,1,'2026-07-16 20:51:34'),(20,1,1910000000000009302,'fixed_1',1,'FIXED','WH','CUSTOM',2,0,NULL,'LEFT',0,1,NULL,0,'NONE',1,0,NULL,0,1,'2026-07-16 20:51:34',1,1,'2026-07-16 20:51:34'),(21,1,1910000000000009302,'sequence_1',2,'SEQ',NULL,'CUSTOM',4,1,'0','LEFT',0,1,'DECIMAL',0,'NONE',1,0,NULL,0,1,'2026-07-16 20:51:34',1,1,'2026-07-16 20:51:34'),(22,1,1910000000000009303,'date_1',2,'DATE','yyyyMMdd','CUSTOM',8,0,NULL,'LEFT',0,1,NULL,0,'NONE',1,0,NULL,0,1,'2026-07-16 20:51:34',1,1,'2026-07-16 20:51:34'),(23,1,1910000000000009303,'fixed_1',1,'FIXED','CG','CUSTOM',2,0,NULL,'LEFT',0,1,NULL,0,'NONE',1,0,NULL,0,1,'2026-07-16 20:51:34',1,1,'2026-07-16 20:51:34'),(24,1,1910000000000009303,'sequence_1',3,'SEQ',NULL,'CUSTOM',4,1,'0','LEFT',0,1,'DECIMAL',1,'DAY',1,0,NULL,0,1,'2026-07-16 20:51:34',1,1,'2026-07-16 20:51:34'),(25,1,1910000000000009304,'date_1',2,'DATE','yyyyMMdd','CUSTOM',8,0,NULL,'LEFT',0,1,NULL,0,'NONE',1,0,NULL,0,1,'2026-07-16 20:51:34',1,1,'2026-07-16 20:51:34'),(26,1,1910000000000009304,'fixed_1',1,'FIXED','CK','CUSTOM',2,0,NULL,'LEFT',0,1,NULL,0,'NONE',1,0,NULL,0,1,'2026-07-16 20:51:34',1,1,'2026-07-16 20:51:34'),(27,1,1910000000000009304,'sequence_1',3,'SEQ',NULL,'CUSTOM',4,1,'0','LEFT',0,1,'DECIMAL',1,'DAY',1,0,NULL,0,1,'2026-07-16 20:51:34',1,1,'2026-07-16 20:51:34'),(28,1,1910000000000009305,'date_1',2,'DATE','yyyyMMdd','CUSTOM',8,0,NULL,'LEFT',0,1,NULL,0,'NONE',1,0,NULL,0,1,'2026-07-16 20:51:34',1,1,'2026-07-16 20:51:34'),(29,1,1910000000000009305,'fixed_1',1,'FIXED','DB','CUSTOM',2,0,NULL,'LEFT',0,1,NULL,0,'NONE',1,0,NULL,0,1,'2026-07-16 20:51:34',1,1,'2026-07-16 20:51:34'),(30,1,1910000000000009305,'sequence_1',3,'SEQ',NULL,'CUSTOM',4,1,'0','LEFT',0,1,'DECIMAL',1,'DAY',1,0,NULL,0,1,'2026-07-16 20:51:34',1,1,'2026-07-16 20:51:34');
/*!40000 ALTER TABLE `ai_code_rule_segment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_context_config`
--
-- ORDER BY:  `id`

LOCK TABLES `ai_context_config` WRITE;
/*!40000 ALTER TABLE `ai_context_config` DISABLE KEYS */;
INSERT INTO `ai_context_config` (`id`, `tenant_id`, `agent_code`, `config_name`, `config_content`, `config_type`, `sort`, `status`, `create_by`, `create_time`, `create_dept`, `update_by`, `update_time`, `del_flag`) VALUES (2,1,'crud_config_builder','工程编码规范','# ForgeAdmin 项目编码规范\n\n## 命名规范\n- 表名：小写字母+下划线，如 `sys_user`、`biz_order`\n- Java 类名：大驼峰，如 `SysUser`、`BizOrder`\n- Java 字段名：小驼峰，如 `userName`、`orderAmount`\n- 方法名：小驼峰，如 `getUserById`、`createOrder`\n\n## 组件映射偏好\n- 短文本输入（≤255字符）→ INPUT\n- 长文本（>255字符）→ TEXTAREA\n- 状态/类型等有限枚举 → SELECT\n- 布尔选择 → RADIO\n- 日期时间 → DATETIME\n- 图片上传 → IMAGE\n- 文件上传 → FILE\n- 富文本 → EDITOR\n\n## 验证规则偏好\n- 手机号：正则 `^1[3-9]\\d{9}$`\n- 邮箱：正则标准格式\n- 必填字段：required=true\n- 金额字段：正则 `^\\d+(\\.\\d{1,2})?$`\n- 身份证号：正则18位\n\n## Java 类型映射\n- int/smallint → Integer\n- bigint → Long\n- varchar/char → String\n- decimal/numeric → BigDecimal\n- datetime/timestamp → LocalDateTime\n- date → LocalDate\n- tinyint(1) → Boolean\n\n## 基类字段（自动包含，不需要用户指定）\n- id, tenant_id, create_by, create_time, create_dept, update_by, update_time\n\n## 字典类型推断\n- 字段名含 status/type/category/level → 优先推荐 dictType\n- 字段注释含括号枚举 (1:正常,0:停用) → 自动识别为字典\n','RULE',1,'0',NULL,'2026-04-22 08:45:05',NULL,1,'2026-07-15 06:57:36','0'),(3,1,'crud_config_builder','CRUD配置输出格式规范','## 输出格式要求\n请严格按以下格式分段输出，每个阶段前加标记：\n\n[STAGE:meta]\n{\"configKey\": \"xxx\", \"tableName\": \"xxx\", \"tableComment\": \"xxx\"}\n\n[STAGE:searchSchema]\n搜索表单配置 JSON 数组\n\n[STAGE:columnsSchema]\n表格列配置 JSON 数组\n\n[STAGE:editSchema]\n编辑表单配置 JSON 数组\n\n[STAGE:apiConfig]\nAPI配置 JSON 对象\n\n[STAGE:dictConfig]\n字典数据配置 JSON 数组，每项包含 dictType, dictName, isNew, items[]\n\n[STAGE:desensitizeConfig]\n字段脱敏配置 JSON 对象，key 为字段名，value 包含 type(PHONE/ID_CARD/EMAIL/BANK_CARD/NAME/ADDRESS/PASSWORD/CAR_LICENSE/CUSTOM)\n\n[STAGE:encryptConfig]\n接口加解密配置 JSON 对象，包含 enableEncrypt, enableDecrypt, operations[]\n\n[STAGE:transConfig]\n字典翻译配置 JSON 对象，key 为字段名，value 包含 dictType, targetField\n\n[STAGE:createTableSql]\nCREATE TABLE 语句（如没有关联表或表不存在，请生成建表SQL）\n\n## 字段规范\n\n### meta\n- configKey: 小写字母开头，仅含小写字母+数字+下划线\n- tableName: 数据库表名，小写，下划线分隔\n- tableComment: 表的中文描述\n\n### searchSchema\n- 每项包含: field(字段名 camelCase), label(中文标签), type(input/select/daterange/number)\n- 有字典的 select 字段必须加 dictType 属性\n- 只选择最常用的 3-5 个搜索条件\n\n### columnsSchema\n- 每项包含: key(camelCase), title(中文标签), dataIndex(同 key)\n- 有字典的列加 render: {type: \"dictTag\", dictType: \"xxx\"}\n\n### editSchema\n- 每项包含: field(camelCase), label(中文标签), type(input/textarea/select/radio/switch/date/datetime/number), required(boolean)\n- 有字典的字段加 dictType 属性\n- 基类字段不包含: id, tenantId, createBy, createTime, createDept, updateBy, updateTime, delFlag\n\n### apiConfig\n- 格式为 method@url\n- 必须包含: \nlist(分页)、detail(详情)、create(新廿)、update(编辑)、delete(删除)\nlist\"：\"get@/ai/crud/\" + configKey + \"/page\"\ndetail\"： \"get@/ai/crud/\" + configKey + \"/:id\")\ncreate\"：\"post@/ai/crud/\" + configKey)\nupdate\"：\"put@/ai/crud/\" + configKey)\ndelete\"：\"delete@/ai/crud/\" + configKey + \"/:id\")\n\n### dictConfig\n- 从 schema 中的 dictType 提取，生成字典类型和字典项\n- isNew=true 表示需要新建到字典表；isNew=false 表示映射已有字典\n- items 中每项包含 dictLabel, dictValue, dictSort\n\n### desensitizeConfig\n- 自动识别敏感字段并生成对应脱敏规则\n- PHONE(手机号): 186****2213\n- ID_CARD(身份证): 110101********0011\n- EMAIL(邮箱): t***@example.com\n- BANK_CARD(银行卡): 6222 **** **** 8888\n- NAME(姓名): 张**\n- ADDRESS(地址): 北京市海淀区******\n- PASSWORD(密码): ********\n- CUSTOM(自定义): 需指定 prefixKeep 和 suffixKeep\n\n### encryptConfig\n- enableEncrypt: 是否启用响应加密\n- enableDecrypt: 是否启用请求解密\n- operations: 需要加解密的操作列表，空数组表示全部 [\"list\",\"detail\",\"create\",\"update\",\"delete\"]\n\n### transConfig\n- key 为原始字段名（如 status）\n- dictType: 对应字典类型\n- targetField: 翻译后的字段名（如 statusName），默认 原字段名 + Name\n\n### createTableSql\n- 生成标准 MySQL CREATE TABLE 语句\n- 必须包含基类字段: id(主键自增), tenant_id, create_by, create_time, create_dept, update_by, update_time, del_flag\n- 使用 utf8mb4 字符集和 InnoDB 引擎\n\n## 重要约束\n- 直接输出纯 JSON/SQL，不要用 markdown 代码块包裹\n- 不要输出任何解释性文字，只输出 [STAGE:xxx] 标记和内容\n- 每个 STAGE 后面紧跟该段的内容，不要包在外层对象中\n- field/key/dataIndex 使用 camelCase 命名','SPEC',1,'0',NULL,'2026-04-22 11:42:32',NULL,1,'2026-07-15 06:57:36','0'),(4,1,'flow_bpmn_builder','Flowable BPMN 输出规范','## Flowable BPMN 推荐写法\n\n### 简单审批\n- startEvent -> userTask -> endEvent\n- userTask 可配置 flowable:assignee=\"${initiator}\" 或 flowable:assignee=\"${flowSpelService.getInitiatorLeader(execution)}\"\n\n### 条件审批\n- userTask -> exclusiveGateway -> 不同 userTask/endEvent\n- sequenceFlow 上使用：\n  <bpmn:conditionExpression xsi:type=\"bpmn:tFormalExpression\">${amount > 5000}</bpmn:conditionExpression>\n\n### 候选人/候选组\n- 候选用户：flowable:candidateUsers=\"1,2,3\"\n- 候选组：flowable:candidateGroups=\"finance\"\n\n### 动态审批人\n- 直属上级：${flowSpelService.getInitiatorLeader(execution)}\n- 根据角色：${flowSpelService.findUsersByRole(\"finance_manager\")}\n- 根据部门负责人：${flowSpelService.findDeptManager(execution.getVariable(\"deptId\"))}\n\n### 重要限制\n- XML 必须包含 BPMNDiagram 图形信息，否则前端设计器会无法加载，后端也不能部署。\n- flowable:formUrl 等属性值不要带前导或尾部空格。\n- 不要生成孤立 sequenceFlow。\n- 不要只输出片段，必须输出完整 definitions XML。','SPEC',10,'0',NULL,'2026-05-07 15:03:00',NULL,1,'2026-09-29 17:47:16','0'),(2055282589253537793,1,'flow_bpmn_builder','1','11','SPEC',2,'0',1,'2026-05-15 21:42:09',2,1,'2026-09-29 17:47:16','0'),(2055282589253537794,1,'lowcode_system_generator','低代码业务系统生成规则','## 生成目标\n- 从用户需求自动识别业务边界，划分业务领域。\n- 为每个核心业务对象生成 LowcodeModelSchema 数据模型草稿。\n- 为每个可交付业务页面生成 LowcodeAppDraftDTO 应用草稿。\n- 页面模板由 Agent 自动选择，用户不需要提前选择模板。\n- 生成过程必须能被前端展示为清晰步骤和决策摘要。\n\n## 安全与保存规则\n- 不允许自动保存模型、业务领域或应用。\n- 不允许执行 DDL。\n- 不允许发布应用。\n- 不允许生成真实密钥、数据库密码、Token、AK/SK。\n- 必须在 generationNotes 中说明 AI 结果需要用户确认后保存。\n\n## 领域规划规则\n- 如果需求命中已有业务领域语义，应优先复用已有领域编码。\n- 如果无法判断已有领域，返回新的领域草稿。\n- 新领域 domainCode 使用小写字母开头，仅含小写字母、数字、下划线。\n- 新领域 tablePrefix 默认 biz_{domainCode}_，configKeyPrefix 默认 {domainCode}_。\n\n## 模板选择规则\n- 标准列表、查询、编辑、详情场景使用 simple-crud。\n- 部门、组织、分类、类目、目录、父子层级、树形管理场景使用 tree-crud，并补充 parentId 和 treeConfig。\n- 明细、子项、订单行等主子表场景可以使用 master-detail-crud；如果首期无法完整表达，拆成多个 simple-crud 应用，并在 decisions 中说明。\n\n## 字段规则\n- 不要生成 id、tenant_id、create_by、create_time、create_dept、update_by、update_time、del_flag 等审计字段。\n- 字段名使用 lowerCamel，数据库列名使用 lower_snake。\n- dataType 只能使用 varchar、char、text、longtext、int、bigint、decimal、date、datetime、time、tinyint。\n- componentType 只能使用 input、textarea、select、radio、checkbox、switch、date、datetime、time、number、upload、imageUpload、fileUpload、cascader、treeSelect、dictSelect、orgTreeSelect、userSelect、regionTreeSelect。\n- 手机号、身份证、邮箱、银行卡、人名、地址等敏感字段必须设置 sensitiveType。','RULE',10,'0',1,'2026-05-25 14:25:15',1,1,'2026-07-03 15:12:04','0'),(2055282589253537795,1,'lowcode_system_generator','低代码 Agent 输出协议','## 输出 JSON 根字段\n必须输出一个兼容 LowcodeAiAppGenerateResult 的 JSON 对象，字段如下：\n- requirementSummary：需求摘要。\n- domainSuggestion：主领域建议名称。\n- steps：生成步骤数组，用于前端展示过程。\n- decisions：决策摘要数组，用于解释领域、模型、模板和保存策略。\n- domains：业务领域草稿数组。\n- models：数据模型草稿数组。\n- apps：应用草稿数组。\n- modelDraft、appDraft、modelSchema、pageSchema：兼容字段，取第一个模型和第一个应用。\n- generationNotes：面向用户的注意事项。\n- fallback：false。\n\n## steps 元素\n字段：orderNo、stepKey、title、status、message、summary。\nstepKey 固定使用 analyzing、domain-planning、model-generating、page-generating、validating。\nstatus 使用 completed。\n\n## decisions 元素\n字段：decisionType、title、target、value、reason、meta。\ndecisionType 可使用 domain、model、template、save。\nreason 只写可审计的摘要依据，不输出不可控的内部推理链。\n\n## domains 元素\n字段：existingDomainId、domainCode、domainName、domainDesc、icon、sort、status、menuParentId、tablePrefix、configKeyPrefix、defaultAppType、defaultLayoutType、defaultTableMode、domainSchema、objectCodes。\nexistingDomainId 不确定时置空。\nstatus 使用 ENABLED，defaultAppType 使用 SINGLE，defaultLayoutType 使用 simple-crud，defaultTableMode 使用 CREATE。\n\n## models 元素\n字段：id、domainId、modelCode、modelName、modelDesc、status、tenantEnabled、masterData、modelSchema、syncDdl、confirmSyncDdl。\nid 置空，domainId 不确定时置空，status 使用 ENABLED，tenantEnabled=true，masterData=false，syncDdl=false，confirmSyncDdl=false。\n\n## modelSchema\n字段：schemaVersion、domain、object、appType、tableMode、tableName、businessName、treeConfig、fields、relations、indexes、policies、children。\nschemaVersion=2，tableMode=CREATE。\ndomain 包含 id、code、name；id 不确定时置空。\nobject 包含 code、name、description。\nfields 至少包含 4 个业务字段。\n\n## apps 元素\n字段：id、configKey、appName、domainId、domainCode、domainName、objectCode、objectName、menuName、menuParentId、menuSort、modelSchema、pageSchema。\nid 置空，domainId 不确定时置空。\nconfigKey 使用领域 configKeyPrefix + objectCode。\n\n## pageSchema\n字段：layoutType、primaryModelCode、modelRefs、zones。\nzones 至少包含 search、table、edit、detail。\n每个 zone 字段：zoneKey、componentKey、enabled、fieldRefs、props。','SPEC',20,'0',1,'2026-05-25 14:25:15',1,1,'2026-07-03 15:12:04','0'),(2055282589253537796,1,'lowcode_system_generator','低代码已有领域复用规则','## 已有业务领域复用规则\n- 当用户指定目标业务领域时，必须优先在该已有领域中生成模型和应用。\n- 输出 domains[0].existingDomainId 必须等于用户指定的 domainId。\n- 输出 domains[0].domainCode、domainName 必须与用户指定领域保持一致。\n- 除非用户需求明确要求跨领域拆分，否则不要新建业务领域。\n- 模型 modelSchema.domain 和应用 domainCode/domainName 需要与目标领域一致。','RULE',15,'0',1,'2026-05-25 15:03:51',1,1,'2026-07-03 15:12:04','0'),(2055282589253537797,1,'lowcode_system_generator','低代码单领域和命名规则','## 单领域和命名规则\n- 当用户描述中出现“一个业务领域”“同一个业务领域”“不要分开”“不要拆分”等表达时，必须只输出一个业务领域。\n- 单领域场景下，多个业务对象可以生成多个模型和应用，但 domains 数组只能有一个元素，所有 modelSchema.domain 和 app.domainCode 必须一致。\n- 当用户指定表名前缀时，例如“表名以 tf_f_ 开头”，所有 modelSchema.tableName 必须使用该前缀。\n- 商品基本信息和商品分类管理属于同一商品管理业务能力时，不要拆分成供应链和运营管理两个领域。\n- 如果当前领域已有数据模型上下文，优先复用已有模型生成应用页面；只有已有模型不能覆盖需求时才补充新模型。','RULE',16,'0',1,'2026-05-25 15:49:06',1,1,'2026-07-03 15:12:04','0');
/*!40000 ALTER TABLE `ai_context_config` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_crud_config`
--
-- ORDER BY:  `id`

LOCK TABLES `ai_crud_config` WRITE;
/*!40000 ALTER TABLE `ai_crud_config` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_crud_config` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_custom_query_scheme`
--
-- ORDER BY:  `id`

LOCK TABLES `ai_custom_query_scheme` WRITE;
/*!40000 ALTER TABLE `ai_custom_query_scheme` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_custom_query_scheme` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_dashboard_component_lineage`
--
-- ORDER BY:  `id`

LOCK TABLES `ai_dashboard_component_lineage` WRITE;
/*!40000 ALTER TABLE `ai_dashboard_component_lineage` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_dashboard_component_lineage` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_data_audit_access`
--
-- ORDER BY:  `id`

LOCK TABLES `ai_data_audit_access` WRITE;
/*!40000 ALTER TABLE `ai_data_audit_access` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_data_audit_access` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_data_audit_cursor`
--
-- ORDER BY:  `id`

LOCK TABLES `ai_data_audit_cursor` WRITE;
/*!40000 ALTER TABLE `ai_data_audit_cursor` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_data_audit_cursor` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_data_audit_event`
--
-- ORDER BY:  `id`

LOCK TABLES `ai_data_audit_event` WRITE;
/*!40000 ALTER TABLE `ai_data_audit_event` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_data_audit_event` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_data_audit_field`
--
-- ORDER BY:  `id`

LOCK TABLES `ai_data_audit_field` WRITE;
/*!40000 ALTER TABLE `ai_data_audit_field` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_data_audit_field` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_data_audit_policy`
--
-- ORDER BY:  `id`

LOCK TABLES `ai_data_audit_policy` WRITE;
/*!40000 ALTER TABLE `ai_data_audit_policy` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_data_audit_policy` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_data_audit_scope`
--
-- ORDER BY:  `id`

LOCK TABLES `ai_data_audit_scope` WRITE;
/*!40000 ALTER TABLE `ai_data_audit_scope` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_data_audit_scope` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_formula_function`
--
-- ORDER BY:  `id`

LOCK TABLES `ai_formula_function` WRITE;
/*!40000 ALTER TABLE `ai_formula_function` DISABLE KEYS */;
INSERT INTO `ai_formula_function` (`id`, `tenant_id`, `function_code`, `display_name`, `category`, `description`, `source_type`, `argument_schema`, `return_type`, `example`, `status`, `current_version`, `latest_version`, `builtin`, `sort_order`, `remark`, `create_by`, `create_time`, `create_dept`, `update_by`, `update_time`) VALUES (690001001,1,'math.abs','绝对值','Math','返回数字绝对值','BUILTIN','[{\"name\": \"value\", \"type\": \"NUMBER\", \"required\": true}]','NUMBER','math.abs(-5) => 5','ENABLED','1.0.0','1.0.0',1,10,NULL,NULL,'2026-06-14 12:57:16',NULL,NULL,'2026-06-14 12:57:16'),(690001002,1,'math.round','四舍五入','Math','返回最接近的整数','BUILTIN','[{\"name\": \"value\", \"type\": \"NUMBER\", \"required\": true}]','NUMBER','math.round(3.6) => 4','ENABLED','1.0.0','1.0.0',1,20,NULL,NULL,'2026-06-14 12:57:16',NULL,NULL,'2026-06-14 12:57:16'),(690001003,1,'math.max','最大值','Math','返回两个数字中的最大值','BUILTIN','[{\"name\": \"left\", \"type\": \"NUMBER\", \"required\": true}, {\"name\": \"right\", \"type\": \"NUMBER\", \"required\": true}]','NUMBER','math.max(3, 7) => 7','ENABLED','1.0.0','1.0.0',1,30,NULL,NULL,'2026-06-14 12:57:16',NULL,NULL,'2026-06-14 12:57:16'),(690001004,1,'math.min','最小值','Math','返回两个数字中的最小值','BUILTIN','[{\"name\": \"left\", \"type\": \"NUMBER\", \"required\": true}, {\"name\": \"right\", \"type\": \"NUMBER\", \"required\": true}]','NUMBER','math.min(3, 7) => 3','ENABLED','1.0.0','1.0.0',1,40,NULL,NULL,'2026-06-14 12:57:16',NULL,NULL,'2026-06-14 12:57:16'),(690001005,1,'string.length','字符串长度','String','返回字符串长度','BUILTIN','[{\"name\": \"value\", \"type\": \"STRING\", \"required\": true}]','NUMBER','string.length(\'hello\') => 5','ENABLED','1.0.0','1.0.0',1,110,NULL,NULL,'2026-06-14 12:57:16',NULL,NULL,'2026-06-14 12:57:16'),(690001006,1,'string.contains','包含判断','String','判断字符串是否包含子串','BUILTIN','[{\"name\": \"value\", \"type\": \"STRING\", \"required\": true}, {\"name\": \"keyword\", \"type\": \"STRING\", \"required\": true}]','BOOLEAN','string.contains(\'hello\',\'ll\') => true','ENABLED','1.0.0','1.0.0',1,120,NULL,NULL,'2026-06-14 12:57:16',NULL,NULL,'2026-06-14 12:57:16'),(690001007,1,'string.startsWith','前缀判断','String','判断字符串前缀','BUILTIN','[{\"name\": \"value\", \"type\": \"STRING\", \"required\": true}, {\"name\": \"prefix\", \"type\": \"STRING\", \"required\": true}]','BOOLEAN','string.startsWith(\'hello\',\'he\') => true','ENABLED','1.0.0','1.0.0',1,130,NULL,NULL,'2026-06-14 12:57:16',NULL,NULL,'2026-06-14 12:57:16'),(690001008,1,'string.endsWith','后缀判断','String','判断字符串后缀','BUILTIN','[{\"name\": \"value\", \"type\": \"STRING\", \"required\": true}, {\"name\": \"suffix\", \"type\": \"STRING\", \"required\": true}]','BOOLEAN','string.endsWith(\'hello\',\'lo\') => true','ENABLED','1.0.0','1.0.0',1,140,NULL,NULL,'2026-06-14 12:57:16',NULL,NULL,'2026-06-14 12:57:16'),(690001009,1,'date_to_string','日期格式化','Date','将日期格式化为字符串','BUILTIN','[{\"name\": \"date\", \"type\": \"DATE\", \"required\": true}, {\"name\": \"pattern\", \"type\": \"STRING\", \"required\": true}]','STRING','date_to_string(date, \'yyyy-MM-dd\')','ENABLED','1.0.0','1.0.0',1,210,NULL,NULL,'2026-06-14 12:57:16',NULL,NULL,'2026-06-14 12:57:16'),(690001010,1,'math.floor','向下取整','Math','返回不大于入参的最大整数','BUILTIN','[{\"name\": \"value\", \"type\": \"NUMBER\", \"required\": true}]','NUMBER','math.floor(3.9) => 3','ENABLED','1.0.0','1.0.0',1,25,NULL,NULL,'2026-06-14 12:57:16',NULL,NULL,'2026-06-14 12:57:16'),(690001011,1,'math.ceil','向上取整','Math','返回不小于入参的最小整数','BUILTIN','[{\"name\": \"value\", \"type\": \"NUMBER\", \"required\": true}]','NUMBER','math.ceil(3.1) => 4','ENABLED','1.0.0','1.0.0',1,26,NULL,NULL,'2026-06-14 12:57:16',NULL,NULL,'2026-06-14 12:57:16'),(690001012,1,'math.pow','幂运算','Math','返回 left 的 right 次方','BUILTIN','[{\"name\": \"left\", \"type\": \"NUMBER\", \"required\": true}, {\"name\": \"right\", \"type\": \"NUMBER\", \"required\": true}]','NUMBER','math.pow(2, 3) => 8','ENABLED','1.0.0','1.0.0',1,50,NULL,NULL,'2026-06-14 12:57:16',NULL,NULL,'2026-06-14 12:57:16'),(690001013,1,'math.sqrt','平方根','Math','返回数字平方根','BUILTIN','[{\"name\": \"value\", \"type\": \"NUMBER\", \"required\": true}]','NUMBER','math.sqrt(16) => 4','ENABLED','1.0.0','1.0.0',1,60,NULL,NULL,'2026-06-14 12:57:16',NULL,NULL,'2026-06-14 12:57:16'),(690001014,1,'string.indexOf','子串位置','String','返回子串首次出现位置','BUILTIN','[{\"name\": \"value\", \"type\": \"STRING\", \"required\": true}, {\"name\": \"keyword\", \"type\": \"STRING\", \"required\": true}]','NUMBER','string.indexOf(\'hello\',\'l\') => 2','ENABLED','1.0.0','1.0.0',1,150,NULL,NULL,'2026-06-14 12:57:16',NULL,NULL,'2026-06-14 12:57:16'),(690001015,1,'string.replace','字符串替换','String','替换字符串片段','BUILTIN','[{\"name\": \"value\", \"type\": \"STRING\", \"required\": true}, {\"name\": \"target\", \"type\": \"STRING\", \"required\": true}, {\"name\": \"replacement\", \"type\": \"STRING\", \"required\": true}]','STRING','string.replace(\'hello\',\'l\',\'x\') => \'hexxo\'','ENABLED','1.0.0','1.0.0',1,160,NULL,NULL,'2026-06-14 12:57:16',NULL,NULL,'2026-06-14 12:57:16'),(690001016,1,'seq.list','创建列表','Collection','按入参顺序创建列表','BUILTIN','[]','COLLECTION','seq.list(1,2,3) => [1,2,3]','ENABLED','1.0.0','1.0.0',1,310,NULL,NULL,'2026-06-14 12:57:16',NULL,NULL,'2026-06-14 12:57:16'),(690001017,1,'seq.set','创建集合','Collection','按入参顺序创建去重集合','BUILTIN','[]','COLLECTION','seq.set(1,2,3) => [1,2,3]','ENABLED','1.0.0','1.0.0',1,320,NULL,NULL,'2026-06-14 12:57:16',NULL,NULL,'2026-06-14 12:57:16'),(690001018,1,'seq.map','创建映射','Collection','按 key/value 入参创建映射','BUILTIN','[]','MAP','seq.map(\'a\',1,\'b\',2) => {a:1,b:2}','ENABLED','1.0.0','1.0.0',1,330,NULL,NULL,'2026-06-14 12:57:16',NULL,NULL,'2026-06-14 12:57:16');
/*!40000 ALTER TABLE `ai_formula_function` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_formula_function_install`
--
-- ORDER BY:  `id`

LOCK TABLES `ai_formula_function_install` WRITE;
/*!40000 ALTER TABLE `ai_formula_function_install` DISABLE KEYS */;
INSERT INTO `ai_formula_function_install` (`id`, `tenant_id`, `function_code`, `installed_version`, `install_status`, `enabled`, `source_type`, `installed_by`, `installed_time`, `remark`, `create_by`, `create_time`, `create_dept`, `update_by`, `update_time`) VALUES (690003001,1,'math.abs','1.0.0','INSTALLED',1,'BUILTIN',NULL,'2026-06-14 12:57:16',NULL,NULL,'2026-06-14 12:57:16',NULL,NULL,'2026-06-14 12:57:16'),(690003002,1,'math.round','1.0.0','INSTALLED',1,'BUILTIN',NULL,'2026-06-14 12:57:16',NULL,NULL,'2026-06-14 12:57:16',NULL,NULL,'2026-06-14 12:57:16'),(690003003,1,'math.max','1.0.0','INSTALLED',1,'BUILTIN',NULL,'2026-06-14 12:57:16',NULL,NULL,'2026-06-14 12:57:16',NULL,NULL,'2026-06-14 12:57:16'),(690003004,1,'math.min','1.0.0','INSTALLED',1,'BUILTIN',NULL,'2026-06-14 12:57:16',NULL,NULL,'2026-06-14 12:57:16',NULL,NULL,'2026-06-14 12:57:16'),(690003005,1,'string.length','1.0.0','INSTALLED',1,'BUILTIN',NULL,'2026-06-14 12:57:16',NULL,NULL,'2026-06-14 12:57:16',NULL,NULL,'2026-06-14 12:57:16'),(690003006,1,'string.contains','1.0.0','INSTALLED',1,'BUILTIN',NULL,'2026-06-14 12:57:16',NULL,NULL,'2026-06-14 12:57:16',NULL,NULL,'2026-06-14 12:57:16'),(690003007,1,'string.startsWith','1.0.0','INSTALLED',1,'BUILTIN',NULL,'2026-06-14 12:57:16',NULL,NULL,'2026-06-14 12:57:16',NULL,NULL,'2026-06-14 12:57:16'),(690003008,1,'string.endsWith','1.0.0','INSTALLED',1,'BUILTIN',NULL,'2026-06-14 12:57:16',NULL,NULL,'2026-06-14 12:57:16',NULL,NULL,'2026-06-14 12:57:16'),(690003009,1,'date_to_string','1.0.0','INSTALLED',1,'BUILTIN',NULL,'2026-06-14 12:57:16',NULL,NULL,'2026-06-14 12:57:16',NULL,NULL,'2026-06-14 12:57:16'),(690003010,1,'math.floor','1.0.0','INSTALLED',1,'BUILTIN',NULL,'2026-06-14 12:57:16',NULL,NULL,'2026-06-14 12:57:16',NULL,NULL,'2026-06-14 12:57:16'),(690003011,1,'math.ceil','1.0.0','INSTALLED',1,'BUILTIN',NULL,'2026-06-14 12:57:16',NULL,NULL,'2026-06-14 12:57:16',NULL,NULL,'2026-06-14 12:57:16'),(690003012,1,'math.pow','1.0.0','INSTALLED',1,'BUILTIN',NULL,'2026-06-14 12:57:16',NULL,NULL,'2026-06-14 12:57:16',NULL,NULL,'2026-06-14 12:57:16'),(690003013,1,'math.sqrt','1.0.0','INSTALLED',1,'BUILTIN',NULL,'2026-06-14 12:57:16',NULL,NULL,'2026-06-14 12:57:16',NULL,NULL,'2026-06-14 12:57:16'),(690003014,1,'string.indexOf','1.0.0','INSTALLED',1,'BUILTIN',NULL,'2026-06-14 12:57:16',NULL,NULL,'2026-06-14 12:57:16',NULL,NULL,'2026-06-14 12:57:16'),(690003015,1,'string.replace','1.0.0','INSTALLED',1,'BUILTIN',NULL,'2026-06-14 12:57:16',NULL,NULL,'2026-06-14 12:57:16',NULL,NULL,'2026-06-14 12:57:16'),(690003016,1,'seq.list','1.0.0','INSTALLED',1,'BUILTIN',NULL,'2026-06-14 12:57:16',NULL,NULL,'2026-06-14 12:57:16',NULL,NULL,'2026-06-14 12:57:16'),(690003017,1,'seq.set','1.0.0','INSTALLED',1,'BUILTIN',NULL,'2026-06-14 12:57:16',NULL,NULL,'2026-06-14 12:57:16',NULL,NULL,'2026-06-14 12:57:16'),(690003018,1,'seq.map','1.0.0','INSTALLED',1,'BUILTIN',NULL,'2026-06-14 12:57:16',NULL,NULL,'2026-06-14 12:57:16',NULL,NULL,'2026-06-14 12:57:16');
/*!40000 ALTER TABLE `ai_formula_function_install` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_image_generate_record`
--
-- ORDER BY:  `id`

LOCK TABLES `ai_image_generate_record` WRITE;
/*!40000 ALTER TABLE `ai_image_generate_record` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_image_generate_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_knowledge`
--
-- ORDER BY:  `id`

LOCK TABLES `ai_knowledge` WRITE;
/*!40000 ALTER TABLE `ai_knowledge` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_knowledge` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_knowledge_chunk`
--
-- ORDER BY:  `id`

LOCK TABLES `ai_knowledge_chunk` WRITE;
/*!40000 ALTER TABLE `ai_knowledge_chunk` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_knowledge_chunk` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_knowledge_document`
--
-- ORDER BY:  `id`

LOCK TABLES `ai_knowledge_document` WRITE;
/*!40000 ALTER TABLE `ai_knowledge_document` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_knowledge_document` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_lowcode_domain`
--
-- ORDER BY:  `id`

LOCK TABLES `ai_lowcode_domain` WRITE;
/*!40000 ALTER TABLE `ai_lowcode_domain` DISABLE KEYS */;
INSERT INTO `ai_lowcode_domain` (`id`, `tenant_id`, `parent_id`, `domain_code`, `domain_name`, `domain_desc`, `icon`, `sort`, `status`, `menu_parent_id`, `table_prefix`, `config_key_prefix`, `default_app_type`, `default_layout_type`, `default_table_mode`, `domain_schema`, `create_by`, `create_time`, `create_dept`, `update_by`, `update_time`, `del_flag`) VALUES (1900000000000000001,1,0,'general','通用业务域','历史低代码应用默认归属的通用业务领域','ionicons5:AppsOutline',0,'ENABLED',NULL,'biz_general_','general_','SINGLE','simple-crud','CREATE','{\"naming\": {\"tablePrefix\": \"biz_general_\", \"configKeyPrefix\": \"general_\", \"objectCodeStyle\": \"lower_snake\"}, \"defaults\": {\"appType\": \"SINGLE\", \"tableMode\": \"CREATE\", \"layoutType\": \"simple-crud\", \"menuParentId\": null}, \"aiContext\": {\"terms\": [], \"constraints\": [], \"description\": \"通用业务域，用于承接历史低代码应用和未明确归属的业务对象\", \"commonObjects\": [], \"generationNotes\": [], \"fieldNamingPreference\": \"lowerCamel\"}, \"fieldTemplates\": [], \"securityPolicies\": [], \"dictRecommendations\": []}',1,'2026-05-20 20:00:39',1,1,'2026-10-13 10:53:15',0);
/*!40000 ALTER TABLE `ai_lowcode_domain` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_lowcode_model`
--
-- ORDER BY:  `id`

LOCK TABLES `ai_lowcode_model` WRITE;
/*!40000 ALTER TABLE `ai_lowcode_model` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_lowcode_model` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_lowcode_publish_task`
--
-- ORDER BY:  `id`

LOCK TABLES `ai_lowcode_publish_task` WRITE;
/*!40000 ALTER TABLE `ai_lowcode_publish_task` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_lowcode_publish_task` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_model_capability`
--
-- ORDER BY:  `id`

LOCK TABLES `ai_model_capability` WRITE;
/*!40000 ALTER TABLE `ai_model_capability` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_model_capability` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_model_route_policy`
--
-- ORDER BY:  `id`

LOCK TABLES `ai_model_route_policy` WRITE;
/*!40000 ALTER TABLE `ai_model_route_policy` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_model_route_policy` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_model_route_target`
--
-- ORDER BY:  `id`

LOCK TABLES `ai_model_route_target` WRITE;
/*!40000 ALTER TABLE `ai_model_route_target` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_model_route_target` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_page_template`
--
-- ORDER BY:  `id`

LOCK TABLES `ai_page_template` WRITE;
/*!40000 ALTER TABLE `ai_page_template` DISABLE KEYS */;
INSERT INTO `ai_page_template` (`id`, `template_key`, `template_name`, `description`, `icon`, `system_prompt`, `schema_hint`, `default_config`, `enabled`, `sort`, `is_builtin`, `codegen_type`, `create_by`, `create_time`, `create_dept`, `update_by`, `update_time`, `del_flag`) VALUES (1,'simple-crud','标准 CRUD','适合平坦型数据的属性增删改查，支持搜索、表格列表、弹窗/抒屉表单编辑','mdi:table','当前模板为「标准 CRUD」，请确保：\n1. columnsSchema 包含内容列 + 操作列(actions)\n2. editSchema 简洁清晰，不要嵌套\n3. modalType 默认为 drawer\n4. 尽量岑减搜索条件，保留最常用的3-5个',NULL,'{\"modalType\": \"drawer\", \"modalWidth\": \"800px\", \"editGridCols\": 1, \"searchGridCols\": 4}',1,1,1,'TEMPLATE',NULL,'2026-04-26 17:05:23',NULL,1,'2026-04-26 17:20:22',0),(2,'tree-crud','树形 CRUD','适合具有父子层级结构的数据，如部门结构、分类管理等，左树右表格布局','mdi:file-tree','当前模板为「树形 CRUD」，请确保：\n1. editSchema 中必须包含 parentId 字段（type: treeSelect）\n2. editSchema 中包含 sort 排序字段\n3. columnsSchema 中包含 层级深度或父层名称列\n4. apiConfig 中包含 tree 接口（查询树形结构）\n5. treeConfig 配置：{\"keyField\": \"id\", \"parentField\": \"parentId\", \"labelField\": \"name\"}\n6. 数据必须有 parent_id 字段，不要导入进 editSchema\n7. 模式必须为 modal','{\"treeConfig\": {\"keyField\": \"id\", \"labelField\": \"name\", \"parentField\": \"parentId\"}}','{\"modalType\": \"modal\", \"modalWidth\": \"600px\", \"editGridCols\": 1, \"searchGridCols\": 3}',1,2,1,'TEMPLATE',NULL,'2026-04-26 17:05:23',NULL,1,'2026-04-30 15:53:18',0);
/*!40000 ALTER TABLE `ai_page_template` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_prompt_template`
--
-- ORDER BY:  `id`

LOCK TABLES `ai_prompt_template` WRITE;
/*!40000 ALTER TABLE `ai_prompt_template` DISABLE KEYS */;
INSERT INTO `ai_prompt_template` (`id`, `tenant_id`, `template_name`, `template_code`, `usage_scene`, `business_category`, `domain_category`, `template_tags`, `description`, `prompt_content`, `example_input`, `status`, `del_flag`, `is_recommended`, `sort_order`, `use_count`, `test_count`, `download_count`, `remark`, `create_by`, `create_time`, `create_dept`, `update_by`, `update_time`) VALUES (1910000000000000001,1,'数据驱动大屏生成通用模板','dashboard_generate_default','dashboard_generate','经营分析','数据大屏','数据绑定,经营监控,可视化','适用于基于业务定义和数据集生成可落地的大屏，强调动态数据绑定和组件布局。','请基于当前业务定义、数据集字段和运行时样例生成数据大屏。要求：\n1. 优先使用真实 datasetId 生成动态数据组件，指标、趋势、排行、表格、地图尽量输出 request.datasetId、request.datasetFields 和 request.datasetMapping。\n2. 先规划顶部 KPI、核心趋势、分类排行、明细/告警等区域，再选择合适组件，避免只生成装饰元素。\n3. 字段只能使用上下文中出现的 fieldName，不要使用字段中文名作为 request 字段。\n4. 敏感或隐藏字段按上下文安全规则处理，不能展示原始敏感值。\n5. 如果数据集不可用，可以静态兜底，但标题、说明和组件含义必须贴合业务目标。\n6. 输出必须是系统要求的合法 JSON，不要添加 Markdown 包裹或额外解释。','生成经营监控大屏，突出核心指标、趋势分析和异常预警。','0',0,'1',1,8,25,7,'系统内置最佳实践模板',1,'2026-05-19 15:22:05',1,1,'2026-09-17 15:39:35');
/*!40000 ALTER TABLE `ai_prompt_template` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_report_data_business_dataset`
--
-- ORDER BY:  `id`

LOCK TABLES `ai_report_data_business_dataset` WRITE;
/*!40000 ALTER TABLE `ai_report_data_business_dataset` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_report_data_business_dataset` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_report_data_business_definition`
--
-- ORDER BY:  `id`

LOCK TABLES `ai_report_data_business_definition` WRITE;
/*!40000 ALTER TABLE `ai_report_data_business_definition` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_report_data_business_definition` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_report_data_dataset`
--
-- ORDER BY:  `id`

LOCK TABLES `ai_report_data_dataset` WRITE;
/*!40000 ALTER TABLE `ai_report_data_dataset` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_report_data_dataset` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_report_data_dataset_acl`
--
-- ORDER BY:  `id`

LOCK TABLES `ai_report_data_dataset_acl` WRITE;
/*!40000 ALTER TABLE `ai_report_data_dataset_acl` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_report_data_dataset_acl` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_report_data_dataset_category`
--
-- ORDER BY:  `id`

LOCK TABLES `ai_report_data_dataset_category` WRITE;
/*!40000 ALTER TABLE `ai_report_data_dataset_category` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_report_data_dataset_category` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_report_data_dataset_field`
--
-- ORDER BY:  `id`

LOCK TABLES `ai_report_data_dataset_field` WRITE;
/*!40000 ALTER TABLE `ai_report_data_dataset_field` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_report_data_dataset_field` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_report_data_dataset_row_scope`
--
-- ORDER BY:  `id`

LOCK TABLES `ai_report_data_dataset_row_scope` WRITE;
/*!40000 ALTER TABLE `ai_report_data_dataset_row_scope` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_report_data_dataset_row_scope` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_report_data_dimension`
--
-- ORDER BY:  `id`

LOCK TABLES `ai_report_data_dimension` WRITE;
/*!40000 ALTER TABLE `ai_report_data_dimension` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_report_data_dimension` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_report_data_dimension_item`
--
-- ORDER BY:  `id`

LOCK TABLES `ai_report_data_dimension_item` WRITE;
/*!40000 ALTER TABLE `ai_report_data_dimension_item` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_report_data_dimension_item` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_report_directory`
--
-- ORDER BY:  `id`

LOCK TABLES `ai_report_directory` WRITE;
/*!40000 ALTER TABLE `ai_report_directory` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_report_directory` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_report_material`
--
-- ORDER BY:  `id`

LOCK TABLES `ai_report_material` WRITE;
/*!40000 ALTER TABLE `ai_report_material` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_report_material` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_report_project`
--
-- ORDER BY:  `id`

LOCK TABLES `ai_report_project` WRITE;
/*!40000 ALTER TABLE `ai_report_project` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_report_project` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_report_template`
--
-- ORDER BY:  `id`

LOCK TABLES `ai_report_template` WRITE;
/*!40000 ALTER TABLE `ai_report_template` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_report_template` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_skill`
--
-- ORDER BY:  `id`

LOCK TABLES `ai_skill` WRITE;
/*!40000 ALTER TABLE `ai_skill` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_skill` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_skill_file`
--
-- ORDER BY:  `id`

LOCK TABLES `ai_skill_file` WRITE;
/*!40000 ALTER TABLE `ai_skill_file` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_skill_file` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_store_instance`
--
-- ORDER BY:  `id`

LOCK TABLES `ai_store_instance` WRITE;
/*!40000 ALTER TABLE `ai_store_instance` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_store_instance` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `biz_leave_request`
--
-- ORDER BY:  `id`

LOCK TABLES `biz_leave_request` WRITE;
/*!40000 ALTER TABLE `biz_leave_request` DISABLE KEYS */;
/*!40000 ALTER TABLE `biz_leave_request` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `business_datasource_demo`
--
-- ORDER BY:  `id`

LOCK TABLES `business_datasource_demo` WRITE;
/*!40000 ALTER TABLE `business_datasource_demo` DISABLE KEYS */;
/*!40000 ALTER TABLE `business_datasource_demo` ENABLE KEYS */;
UNLOCK TABLES;
-- Dump completed on 2026-10-13 10:56:23

SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;

-- Flyway history + local storage
-- MySQL dump 10.13  Distrib 5.7.24, for osx11.1 (x86_64)
--
-- Host: 120.48.96.178    Database: forge_admin_v120_pure
-- ------------------------------------------------------
-- Server version	8.0.32-2.0.0.2

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Dumping data for table `forge_schema_history`
--

LOCK TABLES `forge_schema_history` WRITE;
/*!40000 ALTER TABLE `forge_schema_history` DISABLE KEYS */;
INSERT INTO `forge_schema_history` (`installed_rank`, `version`, `description`, `type`, `script`, `checksum`, `installed_by`, `installed_on`, `execution_time`, `success`) VALUES (1,'1.0.0','baseline','SQL','V1.0.0__baseline.sql',1782594126,'flyway','2026-07-05 02:50:31',110,1),(2,'1.0.1','add org scoped role permission','SQL','V1.0.1__add_org_scoped_role_permission.sql',-1959974255,'flyway','2026-07-06 13:22:47',314,1),(3,'1.0.2','fix existing logic delete contract','SQL','V1.0.2__fix_existing_logic_delete_contract.sql',1323851028,'flyway','2026-07-07 13:15:17',3195,1),(4,'1.0.3','add logic delete to platform internal tables','SQL','V1.0.3__add_logic_delete_to_platform_internal_tables.sql',-1493774790,'flyway','2026-07-07 13:15:22',4543,1),(5,'1.0.4','add logic delete to system config tables','SQL','V1.0.4__add_logic_delete_to_system_config_tables.sql',646728024,'flyway','2026-07-07 13:15:26',3670,1),(6,'1.0.5','add logic delete to org post tables','SQL','V1.0.5__add_logic_delete_to_org_post_tables.sql',-1898802040,'flyway','2026-07-07 13:15:28',1573,1),(7,'1.0.6','add logic delete to tenant table','SQL','V1.0.6__add_logic_delete_to_tenant_table.sql',-348871076,'flyway','2026-07-07 13:15:29',686,1),(8,'1.0.7','add logic delete to role table','SQL','V1.0.7__add_logic_delete_to_role_table.sql',1789093387,'flyway','2026-07-07 13:15:30',1241,1),(9,'1.0.8','add logic delete to resource table','SQL','V1.0.8__add_logic_delete_to_resource_table.sql',-630207299,'flyway','2026-07-07 13:15:32',729,1),(10,'1.0.9','add logic delete to user table','SQL','V1.0.9__add_logic_delete_to_user_table.sql',2144584021,'flyway','2026-07-07 13:15:33',694,1),(11,'1.0.10','add logic delete to lowcode design metadata tables','SQL','V1.0.10__add_logic_delete_to_lowcode_design_metadata_tables.sql',1226708768,'flyway','2026-07-07 13:15:38',4565,1),(12,'1.0.11','add logic delete to report data metadata tables','SQL','V1.0.11__add_logic_delete_to_report_data_metadata_tables.sql',591642547,'flyway','2026-07-07 13:15:42',3597,1),(13,'1.0.12','add logic delete to sample purchase order','SQL','V1.0.12__add_logic_delete_to_sample_purchase_order.sql',1291618583,'flyway','2026-07-07 13:15:43',978,1),(14,'1.0.13','add logic delete to prompt and query scheme tables','SQL','V1.0.13__add_logic_delete_to_prompt_and_query_scheme_tables.sql',11009982,'flyway','2026-07-07 13:15:44',914,1),(15,'1.0.14','enhance operation log page audit','SQL','V1.0.14__enhance_operation_log_page_audit.sql',-1390891666,'flyway','2026-07-08 00:56:39',2722,1),(16,'1.0.15','normalize operation type add dict','SQL','V1.0.15__normalize_operation_type_add_dict.sql',-1516239953,'flyway','2026-07-08 02:16:12',553,1),(17,'1.0.16','add user import export config','SQL','V1.0.16__add_user_import_export_config.sql',-1920742845,'flyway','2026-07-08 08:10:28',708,1),(18,'1.0.17','add ai provider adapter code','SQL','V1.0.17__add_ai_provider_adapter_code.sql',84922673,'flyway','2026-07-11 00:17:36',398,1),(19,'1.0.18','add ai model routing governance','SQL','V1.0.18__add_ai_model_routing_governance.sql',884998929,'flyway','2026-07-11 11:21:23',988,1),(20,'1.0.19','align flow task query api resources','SQL','V1.0.19__align_flow_task_query_api_resources.sql',300191439,'flyway','2026-07-12 03:12:59',316,1),(21,'1.0.20','add role org scope type','SQL','V1.0.20__add_role_org_scope_type.sql',60416404,'flyway','2026-07-12 03:13:00',375,1),(22,'1.0.21','add ai capability control plane','SQL','V1.0.21__add_ai_capability_control_plane.sql',2005638815,'flyway','2026-07-12 03:13:01',595,1),(23,'1.0.22','add mcp user delegation identity','SQL','V1.0.22__add_mcp_user_delegation_identity.sql',-1002974394,'flyway','2026-07-12 03:13:02',556,1),(24,'1.0.23','add secure business action capabilities','SQL','V1.0.23__add_secure_business_action_capabilities.sql',1653896088,'flyway','2026-07-12 12:13:49',823,1),(25,'1.0.24','add capability flow actions','SQL','V1.0.24__add_capability_flow_actions.sql',301499290,'flyway','2026-07-12 12:13:50',241,1),(26,'1.0.25','add flow task action idempotency','SQL','V1.0.25__add_flow_task_action_idempotency.sql',-1347870351,'flyway','2026-07-12 12:13:51',570,1),(27,'1.0.26','add capability high risk approval','SQL','V1.0.26__add_capability_high_risk_approval.sql',-849747131,'flyway','2026-07-12 12:13:51',290,1),(28,'1.0.27','add business application aggregate','SQL','V1.0.27__add_business_application_aggregate.sql',-1613774430,'flyway','2026-07-13 22:48:56',1179,1),(29,'1.0.28','add business extension governance','SQL','V1.0.28__add_business_extension_governance.sql',626214060,'flyway','2026-07-13 22:48:57',433,1),(30,'1.0.29','add business application release coordination','SQL','V1.0.29__add_business_application_release_coordination.sql',-733681223,'flyway','2026-07-13 22:48:57',367,1),(31,'1.0.30','add business application workspace route','SQL','V1.0.30__add_business_application_workspace_route.sql',-1284437912,'flyway','2026-07-13 23:07:16',217,1),(32,'1.0.31','add business app entry mode dict','SQL','V1.0.31__add_business_app_entry_mode_dict.sql',-477205002,'flyway','2026-07-14 02:13:57',206,1),(33,'1.0.32','clarify java service extension label','SQL','V1.0.32__clarify_java_service_extension_label.sql',-182911856,'flyway','2026-07-14 06:32:28',180,1),(34,'1.0.33','hide legacy lowcode model menu','SQL','V1.0.33__hide_legacy_lowcode_model_menu.sql',1713265721,'flyway','2026-07-14 22:21:06',135,1),(35,'1.0.34','add business application preview route','SQL','V1.0.34__add_business_application_preview_route.sql',-1244614465,'flyway','2026-07-14 22:25:10',280,1),(36,'1.0.35','add business application codegen permissions','SQL','V1.0.35__add_business_application_codegen_permissions.sql',-944332024,'flyway','2026-07-15 06:52:07',258,1),(37,'1.0.36','add structured code rule segments','SQL','V1.0.36__add_structured_code_rule_segments.sql',-104413868,'flyway','2026-07-17 03:42:29',2179,1),(38,'1.0.37','optimize code rule runtime','SQL','V1.0.37__optimize_code_rule_runtime.sql',1747251885,'flyway','2026-07-17 03:42:30',481,1),(39,'1.0.38','clean null literal config values','SQL','V1.0.38__clean_null_literal_config_values.sql',1294004788,'flyway','2026-07-18 01:46:21',145,1),(40,'1.0.39','add code rule excluded characters','SQL','V1.0.39__add_code_rule_excluded_characters.sql',2144959645,'flyway','2026-07-18 04:09:25',278,1),(41,'1.0.40','harden job scheduler reliability','SQL','V1.0.40__harden_job_scheduler_reliability.sql',-484621884,'flyway','2026-07-21 08:02:55',2092,1),(42,'1.0.41','add job editor routes','SQL','V1.0.41__add_job_editor_routes.sql',58554776,'flyway','2026-07-21 08:02:56',414,1),(43,'1.0.42','add job once schedule and timezone','SQL','V1.0.42__add_job_once_schedule_and_timezone.sql',-1660194968,'flyway','2026-07-21 08:02:58',1160,1),(44,'1.0.43','add job execution policies','SQL','V1.0.43__add_job_execution_policies.sql',-619947309,'flyway','2026-07-21 08:02:59',1165,1),(45,'1.0.44','add job log observability','SQL','V1.0.44__add_job_log_observability.sql',836840469,'flyway','2026-07-21 08:03:01',1054,1),(46,'1.0.45','add job alarm permissions','SQL','V1.0.45__add_job_alarm_permissions.sql',336191862,'flyway','2026-07-21 08:03:02',935,1),(47,'1.0.46','add job open api credentials','SQL','V1.0.46__add_job_open_api_credentials.sql',-1462387720,'flyway','2026-07-21 08:03:04',629,1),(48,'1.0.47','add outbound security policy','SQL','V1.0.47__add_outbound_security_policy.sql',654746974,'flyway','2026-07-21 08:03:05',429,1),(49,'1.0.48','add job flow orchestration','SQL','V1.0.48__add_job_flow_orchestration.sql',764063693,'flyway','2026-07-21 08:03:07',1516,1),(50,'1.0.49','fix job scheduler scan findings','SQL','V1.0.49__fix_job_scheduler_scan_findings.sql',130209858,'flyway','2026-07-21 08:03:08',1056,1),(51,'1.0.50','add business application runtime route','SQL','V1.0.50__add_business_application_runtime_route.sql',-1916231650,'flyway','2026-07-21 14:20:00',226,1),(52,'1.0.51','replace logic delete generated columns','SQL','V1.0.51__replace_logic_delete_generated_columns.sql',-659402120,'flyway','2026-07-23 01:24:08',25399,1),(53,'1.0.52','remove database crypto secrets','SQL','V1.0.52__remove_database_crypto_secrets.sql',-278881103,'flyway','2026-07-27 08:05:55',450,1),(54,'1.0.53','harden client credentials','SQL','V1.0.53__harden_client_credentials.sql',2084594000,'flyway','2026-07-27 08:05:56',710,1),(55,'1.0.54','add frontend runtime dicts','SQL','V1.0.54__add_frontend_runtime_dicts.sql',-1971933412,'flyway','2026-07-27 08:05:57',452,1),(56,'1.0.55','secure flow monitor cleanup','SQL','V1.0.55__secure_flow_monitor_cleanup.sql',-1582353787,'flyway','2026-07-27 08:06:01',2804,1),(57,'1.0.56','secure online user management','SQL','V1.0.56__secure_online_user_management.sql',2068508514,'flyway','2026-07-27 08:06:01',237,1),(58,'1.0.57','add collaboration connection foundation','SQL','V1.0.57__add_collaboration_connection_foundation.sql',-1210867839,'flyway','2026-07-29 06:58:53',5015,1),(59,'1.0.58','extend message delivery for collaboration','SQL','V1.0.58__extend_message_delivery_for_collaboration.sql',-942623017,'flyway','2026-07-29 06:58:56',2350,1),(60,'1.0.59','add collaboration resources and jobs','SQL','V1.0.59__add_collaboration_resources_and_jobs.sql',718727825,'flyway','2026-07-29 06:58:58',660,1),(61,'1.0.60','add collaboration outbound whitelist','SQL','V1.0.60__add_collaboration_outbound_whitelist.sql',1952750044,'flyway','2026-07-29 06:58:58',183,1),(62,'1.0.61','add collaboration message test resource','SQL','V1.0.61__add_collaboration_message_test_resource.sql',-1120139763,'flyway','2026-07-29 06:58:59',336,1),(63,'1.0.62','add collaboration binding api resources','SQL','V1.0.62__add_collaboration_binding_api_resources.sql',-1445421877,'flyway','2026-07-29 06:59:00',564,1),(64,'1.0.63','add collaboration api base url','SQL','V1.0.63__add_collaboration_api_base_url.sql',-585032824,'flyway','2026-07-29 08:05:12',214,1),(65,'1.0.64','fix collab platform dict value','SQL','V1.0.64__fix_collab_platform_dict_value.sql',277497791,'flyway','2026-07-29 11:19:58',325,1),(66,'1.0.65','backfill social config tenant id','SQL','V1.0.65__backfill_social_config_tenant_id.sql',-676042035,'flyway','2026-07-29 11:32:25',202,1),(67,'1.0.66','add sync log remove resource','SQL','V1.0.66__add_sync_log_remove_resource.sql',-1651586346,'flyway','2026-07-29 11:47:02',378,1),(68,'1.0.67','extend collab platform dict','SQL','V1.0.67__extend_collab_platform_dict.sql',552393921,'flyway','2026-07-30 01:08:54',271,1),(69,'1.0.68','remove social config menu','SQL','V1.0.68__remove_social_config_menu.sql',1385995678,'flyway','2026-07-30 01:08:55',428,1),(70,'1.0.69','add social config default role ids','SQL','V1.0.69__add_social_config_default_role_ids.sql',182961968,'flyway','2026-07-30 09:48:43',258,1),(71,'1.0.70','add social config todo push','SQL','V1.0.70__add_social_config_todo_push.sql',1689083774,'flyway','2026-07-31 02:16:47',545,1),(72,'1.0.71','collaboration console consolidation','SQL','V1.0.71__collaboration_console_consolidation.sql',-587388522,'flyway','2026-07-31 02:29:26',693,1),(73,'1.0.72','collaboration sync schedule and todo card template','SQL','V1.0.72__collaboration_sync_schedule_and_todo_card_template.sql',-1908955851,'flyway','2026-07-31 06:12:26',839,1),(74,'1.0.73','collaboration sso workbench and message platform','SQL','V1.0.73__collaboration_sso_workbench_and_message_platform.sql',-629121393,'flyway','2026-07-31 07:38:02',461,1),(75,'1.0.74','capability open gateway','SQL','V1.0.74__capability_open_gateway.sql',-2715699,'flyway','2026-07-31 14:40:47',1186,1),(76,'1.0.75','add capability registration dict','SQL','V1.0.75__add_capability_registration_dict.sql',761833255,'flyway','2026-08-01 01:51:41',177,1),(77,'1.0.76','add capability external user delegation','SQL','V1.0.76__add_capability_external_user_delegation.sql',-165436534,'flyway','2026-08-01 10:10:40',1322,1),(78,'1.0.77','add capability system service permissions','SQL','V1.0.77__add_capability_system_service_permissions.sql',2041918517,'flyway','2026-08-02 07:04:18',227,1),(79,'1.0.78','add capability catalog dicts','SQL','V1.0.78__add_capability_catalog_dicts.sql',-1780566289,'flyway','2026-08-02 12:10:27',258,1),(80,'1.0.79','add capability client user assertion','SQL','V1.0.79__add_capability_client_user_assertion.sql',-6969516,'flyway','2026-08-02 13:13:34',771,1),(81,'1.0.80','add message partial send status','SQL','V1.0.80__add_message_partial_send_status.sql',-2060558569,'flyway','2026-08-03 02:18:19',329,1),(82,'1.0.81','add capability flow submit operation','SQL','V1.0.81__add_capability_flow_submit_operation.sql',-593212223,'flyway','2026-08-03 07:54:06',331,1),(83,'1.0.82','improve capability client workbench','SQL','V1.0.82__improve_capability_client_workbench.sql',716059864,'flyway','2026-08-04 02:42:19',1056,1),(84,'1.0.83','add application business process','SQL','V1.0.83__add_application_business_process.sql',-2023102135,'flyway','2026-08-04 02:42:20',629,1),(85,'1.0.84','add application business process resources','SQL','V1.0.84__add_application_business_process_resources.sql',-212684645,'flyway','2026-08-04 02:42:21',810,1),(86,'1.0.85','add business process start permission','SQL','V1.0.85__add_business_process_start_permission.sql',-1302736496,'flyway','2026-08-04 02:42:22',224,1),(87,'1.0.86','add ai model type refined dict','SQL','V1.0.86__add_ai_model_type_refined_dict.sql',599208700,'flyway','2026-08-06 05:00:30',1190,1),(88,'1.0.87','add ai knowledge rag','SQL','V1.0.87__add_ai_knowledge_rag.sql',1489937761,'flyway','2026-08-06 05:00:33',2492,1),(89,'1.0.88','add agent engine event skill','SQL','V1.0.88__add_agent_engine_event_skill.sql',1233139770,'flyway','2026-08-06 05:02:00',3417,1),(90,'1.0.89','add ai multimodal agent create','SQL','V1.0.89__add_ai_multimodal_agent_create.sql',-873508405,'flyway','2026-08-06 05:02:02',1037,1),(91,'1.0.90','align knowledge store menu path','SQL','V1.0.90__align_knowledge_store_menu_path.sql',684455622,'flyway','2026-08-06 05:02:04',794,1),(92,'1.0.92','unify flow category to id','SQL','V1.0.92__unify_flow_category_to_id.sql',-522858054,'flyway','2026-08-06 05:02:06',448,1),(93,'1.0.93','fix agent workspace menu 404','SQL','V1.0.93__fix_agent_workspace_menu_404.sql',-1598817896,'flyway','2026-08-06 05:02:09',803,1),(94,'1.0.94','create agent tool tables','SQL','V1.0.94__create_agent_tool_tables.sql',536517054,'flyway','2026-08-07 01:44:51',549,1),(95,'1.0.95','fix agent workspace orphan menus','SQL','V1.0.95__fix_agent_workspace_orphan_menus.sql',-654326896,'flyway','2026-08-07 01:44:52',245,1),(96,'1.0.96','fix agent chat route path','SQL','V1.0.96__fix_agent_chat_route_path.sql',93046980,'flyway','2026-08-07 01:44:52',205,1),(97,'1.0.97','reorder ai menus','SQL','V1.0.97__reorder_ai_menus.sql',-1670137802,'flyway','2026-08-07 01:44:53',791,1),(98,'1.0.98','restore agent create menu','SQL','V1.0.98__restore_agent_create_menu.sql',1357400141,'flyway','2026-08-07 01:44:55',192,1),(99,'1.0.99','add role module data scopes','SQL','V1.0.99__add_role_module_data_scopes.sql',1925559505,'flyway','2026-08-07 14:56:20',365,1),(100,'1.0.100','add flow monitor datascope config','SQL','V1.0.100__add_flow_monitor_datascope_config.sql',523240975,'flyway','2026-08-07 15:28:52',174,1),(101,'1.0.101','add external connector outbound scene','SQL','V1.0.101__add_external_connector_outbound_scene.sql',-2064008602,'flyway','2026-08-11 23:09:44',195,1),(102,'1.0.102','add external api lowcode query contract','SQL','V1.0.102__add_external_api_lowcode_query_contract.sql',1172681647,'flyway','2026-08-11 23:09:46',832,1),(103,'1.0.103','harden transactional business commands','SQL','V1.0.103__harden_transactional_business_commands.sql',1748244781,'flyway','2026-08-11 23:09:47',755,1),(104,'1.0.104','add lowcode audit governance','SQL','V1.0.104__add_lowcode_audit_governance.sql',514438047,'flyway','2026-08-11 23:09:49',1133,1),(105,'1.0.105','seed presale registration lowcode app','SQL','V1.0.105__seed_presale_registration_lowcode_app.sql',-1447944018,'flyway','2026-08-11 23:21:18',1427,1),(106,'1.0.106','fix presale lowcode runtime schema','SQL','V1.0.106__fix_presale_lowcode_runtime_schema.sql',-945924854,'flyway','2026-08-12 08:37:25',768,1),(107,'1.0.107','add external api mock query sources','SQL','V1.0.107__add_external_api_mock_query_sources.sql',2024806039,'flyway','2026-08-13 22:36:37',911,1),(108,'1.0.108','fix presale mobile form visibility','SQL','V1.0.108__fix_presale_mobile_form_visibility.sql',1654090450,'flyway','2026-08-13 22:36:38',566,1),(109,'1.0.109','add presale h5 page sections','SQL','V1.0.109__add_presale_h5_page_sections.sql',544997495,'flyway','2026-08-15 00:29:40',435,1),(110,'1.0.110','fix presale h5 sections','SQL','V1.0.110__fix_presale_h5_sections.sql',-213547743,'flyway','2026-08-15 08:55:26',315,1),(111,'1.0.111','fix presale relation display names','SQL','V1.0.111__fix_presale_relation_display_names.sql',1895465890,'flyway','2026-08-15 09:14:14',174,1),(112,'1.0.112','fix presale relation config keys','SQL','V1.0.112__fix_presale_relation_config_keys.sql',217659092,'flyway','2026-08-15 09:32:31',211,1),(113,'1.0.113','move pickup return actions to operation logs tab','SQL','V1.0.113__move_pickup_return_actions_to_operation_logs_tab.sql',-917104297,'flyway','2026-08-15 10:35:18',331,1),(114,'1.0.114','seed presale business process','SQL','V1.0.114__seed_presale_business_process.sql',-2017030750,'flyway','2026-08-16 01:21:33',384,1),(115,'1.0.115','add business process publish permission','SQL','V1.0.115__add_business_process_publish_permission.sql',-1614967143,'flyway','2026-08-16 02:27:45',187,1),(116,'1.0.116','presale form canvas layout sections','SQL','V1.0.116__presale_form_canvas_layout_sections.sql',-1457173846,'flyway','2026-08-16 05:57:27',410,1),(117,'1.0.117','presale container virtual field binding','SQL','V1.0.117__presale_container_virtual_field_binding.sql',1096581430,'flyway','2026-08-16 06:32:02',255,1),(118,'1.0.118','register business document common permissions','SQL','V1.0.118__register_business_document_common_permissions.sql',1305185582,'flyway','2026-08-16 08:07:47',449,1),(119,'1.0.119','rename h5 client to mobile','SQL','V1.0.119__rename_h5_client_to_mobile.sql',-1392223220,'flyway','2026-08-16 12:43:27',283,1),(120,'1.0.120','add ai model type aliyun categories','SQL','V1.0.120__add_ai_model_type_aliyun_categories.sql',-1381829039,'flyway','2026-08-17 07:23:39',448,1),(121,'1.0.121','align ai knowledge document file id string','SQL','V1.0.121__align_ai_knowledge_document_file_id_string.sql',1172274783,'flyway','2026-08-17 07:23:42',670,1),(122,'1.0.122','add managed cache policy','SQL','V1.0.122__add_managed_cache_policy.sql',-1990863662,'flyway','2026-08-17 07:28:30',781,1),(123,'1.0.123','remove redis cache diagnostics','SQL','V1.0.123__remove_redis_cache_diagnostics.sql',-1902270513,'flyway','2026-08-17 08:02:27',468,1),(124,'1.0.124','enhance ai chat message structure','SQL','V1.0.124__enhance_ai_chat_message_structure.sql',1229400410,'flyway','2026-08-20 23:02:17',1045,1),(125,'1.0.125','ai chat phase2 message and session','SQL','V1.0.125__ai_chat_phase2_message_and_session.sql',178625632,'flyway','2026-08-20 23:02:18',611,1),(126,'1.0.126','add business flow status dict','SQL','V1.0.126__add_business_flow_status_dict.sql',-1341352627,'flyway','2026-08-20 23:02:19',147,1),(127,'1.0.127','add business application portal config','SQL','V1.0.127__add_business_application_portal_config.sql',-1896246461,'flyway','2026-08-20 23:02:20',615,1),(128,'1.0.128','flow model notify config','SQL','V1.0.128__flow_model_notify_config.sql',813356546,'flyway','2026-08-22 03:47:05',346,1),(129,'1.0.129','flow result cc card templates','SQL','V1.0.129__flow_result_cc_card_templates.sql',-689218751,'flyway','2026-08-22 03:47:06',270,1),(130,'1.0.130','default flow event notify redis','SQL','V1.0.130__default_flow_event_notify_redis.sql',1273336731,'flyway','2026-08-22 07:39:12',227,1),(131,'1.0.131','add mount target to crud config','SQL','V1.0.131__add_mount_target_to_crud_config.sql',517029996,'flyway','2026-08-22 08:30:44',224,1),(132,'1.0.132','migrate application page menus to portal','SQL','V1.0.132__migrate_application_page_menus_to_portal.sql',576609166,'flyway','2026-08-23 00:00:44',199,1),(133,'1.0.133','align resource unique key with client code','SQL','V1.0.133__align_resource_unique_key_with_client_code.sql',-1958747335,'flyway','2026-08-23 00:36:50',726,1),(134,'1.0.134','flow model multi return','SQL','V1.0.134__flow_model_multi_return.sql',1767788792,'flyway','2026-08-30 00:41:58',260,1),(135,'1.0.135','optimize flow task list queries','SQL','V1.0.135__optimize_flow_task_list_queries.sql',-412464448,'flyway','2026-08-30 05:50:54',595,1),(136,'1.0.136','enforce business object code uniqueness','SQL','V1.0.136__enforce_business_object_code_uniqueness.sql',2102999484,'flyway','2026-08-30 08:30:11',3782,1),(137,'1.0.137','repair business flow object references','SQL','V1.0.137__repair_business_flow_object_references.sql',-934272071,'flyway','2026-08-30 09:56:52',555,1),(138,'1.0.138','allow nullable social config credentials','SQL','V1.0.138__allow_nullable_social_config_credentials.sql',111035339,'flyway','2026-08-31 10:27:57',461,1),(139,'1.0.139','index user list and flow candidate queries','SQL','V1.0.139__index_user_list_and_flow_candidate_queries.sql',-235365612,'flyway','2026-09-02 11:11:22',798,1),(140,'1.0.140','add flow record participant','SQL','V1.0.140__add_flow_record_participant.sql',1138464962,'flyway','2026-09-06 00:22:20',236,1),(141,'1.0.141','add data scope flow related visibility','SQL','V1.0.141__add_data_scope_flow_related_visibility.sql',357404527,'flyway','2026-09-06 00:22:21',417,1),(142,'1.0.142','align flow task status dict','SQL','V1.0.142__align_flow_task_status_dict.sql',-435890321,'flyway','2026-09-06 00:22:22',193,1),(143,'1.0.143','add qichacha external auth adapter','SQL','V1.0.143__add_qichacha_external_auth_adapter.sql',-1150385840,'flyway','2026-09-06 00:22:22',119,1),(144,'1.0.144','add flow overdue reminder runtime','SQL','V1.0.144__add_flow_overdue_reminder_runtime.sql',-1688362782,'flyway','2026-09-06 11:06:41',1513,1),(145,'1.0.145','add flow timeout cursor index','SQL','V1.0.145__add_flow_timeout_cursor_index.sql',2054387935,'flyway','2026-09-06 11:06:42',236,1),(146,'1.0.146','add flow cc revoke state','SQL','V1.0.146__add_flow_cc_revoke_state.sql',-1540777150,'flyway','2026-09-06 11:06:43',688,1),(147,'1.0.147','add flow task candidate relation','SQL','V1.0.147__add_flow_task_candidate_relation.sql',166672120,'flyway','2026-09-06 11:06:43',224,1),(148,'1.0.148','add flow task sign relation audit','SQL','V1.0.148__add_flow_task_sign_relation_audit.sql',134404826,'flyway','2026-09-06 11:06:44',581,1),(149,'1.0.149','add flow user group management','SQL','V1.0.149__add_flow_user_group_management.sql',1897560741,'flyway','2026-09-06 11:06:45',651,1),(150,'1.0.150','add flow model version cleanup permission','SQL','V1.0.150__add_flow_model_version_cleanup_permission.sql',398459688,'flyway','2026-09-06 11:06:46',193,1),(151,'1.0.151','add flow model sort order','SQL','V1.0.151__add_flow_model_sort_order.sql',-1601089615,'flyway','2026-09-06 11:06:47',375,1),(152,'1.0.152','add flow model sort permission','SQL','V1.0.152__add_flow_model_sort_permission.sql',1283936265,'flyway','2026-09-06 11:06:47',196,1),(153,'1.0.153','add flow task sign idempotency','SQL','V1.0.153__add_flow_task_sign_idempotency.sql',-878144860,'flyway','2026-09-06 11:06:48',415,1),(154,'1.0.154','add flow task sign runtime relation','SQL','V1.0.154__add_flow_task_sign_runtime_relation.sql',-757180209,'flyway','2026-09-06 11:06:49',377,1),(155,'1.0.155','seed flow sign mode dict','SQL','V1.0.155__seed_flow_sign_mode_dict.sql',259022561,'flyway','2026-09-06 11:06:49',236,1),(156,'1.0.156','add flow comment phrase','SQL','V1.0.156__add_flow_comment_phrase.sql',-1685682204,'flyway','2026-09-07 12:03:07',670,1),(157,'1.0.157','seed lowcode general domain','SQL','V1.0.157__seed_lowcode_general_domain.sql',-2038763911,'flyway','2026-09-09 06:16:26',471,1),(158,'1.0.158','seed gitee community tenant','SQL','V1.0.158__seed_gitee_community_tenant.sql',1530444567,'flyway','2026-09-09 06:16:27',405,1),(159,'1.0.159','add user allowed clients','SQL','V1.0.159__add_user_allowed_clients.sql',-835084883,'flyway','2026-09-16 04:55:17',184,1),(160,'1.0.160','add excel image column support','SQL','V1.0.160__add_excel_image_column_support.sql',1424139373,'flyway','2026-09-16 04:55:18',221,1),(161,'1.0.161','seed gitee social config','SQL','V1.0.161__seed_gitee_social_config.sql',-630072720,'flyway','2026-09-16 13:43:14',143,1),(162,'1.0.162','seed gitee default app','SQL','V1.0.162__seed_gitee_default_app.sql',666165568,'flyway','2026-09-16 22:50:25',211,1),(163,'1.0.163','fix gitee oauth redirect uri','SQL','V1.0.163__fix_gitee_oauth_redirect_uri.sql',1358101208,'flyway','2026-09-17 01:33:24',224,1),(164,'1.0.164','fix tenant system theme invalid color','SQL','V1.0.164__fix_tenant_system_theme_invalid_color.sql',1193204189,'flyway','2026-09-17 04:52:13',128,1),(165,'1.0.165','cleanup dangling user role and post bindings','SQL','V1.0.165__cleanup_dangling_user_role_and_post_bindings.sql',1153433025,'flyway','2026-09-17 10:02:27',264,1),(166,'1.0.166','repair utf8 mojibake seed data','SQL','V1.0.166__repair_utf8_mojibake_seed_data.sql',1839782927,'flyway','2026-09-18 04:52:16',8760,1),(167,'1.0.167','optimize flow todo candidate lookup','SQL','V1.0.167__optimize_flow_todo_candidate_lookup.sql',-1301394880,'flyway','2026-09-18 13:33:56',381,1),(168,'1.0.168','repair flow task process def key','SQL','V1.0.168__repair_flow_task_process_def_key.sql',-1783583920,'flyway','2026-09-19 08:25:59',334,1),(169,'1.0.169','seed business flow need modify status','SQL','V1.0.169__seed_business_flow_need_modify_status.sql',-211989272,'flyway','2026-09-19 13:00:19',159,1),(170,'1.0.170','add business flow instance round no','SQL','V1.0.170__add_business_flow_instance_round_no.sql',-698851386,'flyway','2026-09-19 13:00:20',359,1),(171,'1.0.171','add native print tables','SQL','V1.0.171__add_native_print_tables.sql',923113231,'flyway','2026-09-19 13:58:27',364,1),(172,'1.0.172','add native print resources','SQL','V1.0.172__add_native_print_resources.sql',-1985077805,'flyway','2026-09-19 13:58:28',1057,1),(173,'1.0.173','add native print hidden routes','SQL','V1.0.173__add_native_print_hidden_routes.sql',-156964118,'flyway','2026-09-19 13:58:29',243,1),(174,'1.0.174','support print workspace page identity','SQL','V1.0.174__support_print_workspace_page_identity.sql',1092091147,'flyway','2026-09-19 13:58:30',382,1),(175,'1.0.175','add lowcode data change audit','SQL','V1.0.175__add_lowcode_data_change_audit.sql',1662950887,'flyway','2026-09-20 07:00:47',886,1),(176,'1.0.176','move data change audit menu to app center','SQL','V1.0.176__move_data_change_audit_menu_to_app_center.sql',-278450737,'flyway','2026-09-20 07:49:46',447,1),(177,'1.0.177','reparent data change audit under app center dir','SQL','V1.0.177__reparent_data_change_audit_under_app_center_dir.sql',2008914241,'flyway','2026-09-20 08:01:43',165,1),(178,'1.0.178','add data audit detail visibility','SQL','V1.0.178__add_data_audit_detail_visibility.sql',2131451655,'flyway','2026-09-20 09:00:49',656,1),(179,'1.0.179','grant print execute to existing roles','SQL','V1.0.179__grant_print_execute_to_existing_roles.sql',58633016,'flyway','2026-09-20 12:17:55',182,1),(180,'1.0.180','add print pdf downloaded result','SQL','V1.0.180__add_print_pdf_downloaded_result.sql',454731551,'flyway','2026-09-21 03:28:17',131,1),(181,'1.0.181','default business workbench layout','SQL','V1.0.181__default_business_workbench_layout.sql',-150731421,'flyway','2026-09-23 01:45:28',374,1),(182,'1.0.182','add open platform form receipt and permissions','SQL','V1.0.182__add_open_platform_form_receipt_and_permissions.sql',907780657,'flyway','2026-09-23 01:45:29',320,1),(183,'1.0.183','add application integrations','SQL','V1.0.183__add_application_integrations.sql',196705438,'flyway','2026-09-23 01:45:30',354,1),(184,'1.0.184','add open platform withdraw operation','SQL','V1.0.184__add_open_platform_withdraw_operation.sql',-857649803,'flyway','2026-09-23 01:45:31',378,1),(185,'1.0.185','secure external api permissions','SQL','V1.0.185__secure_external_api_permissions.sql',991643247,'flyway','2026-09-28 05:03:23',1141,1),(186,'1.0.186','secure dataset sql preview','SQL','V1.0.186__secure_dataset_sql_preview.sql',1398395264,'flyway','2026-09-28 05:03:24',271,1),(187,'1.0.187','add business process run lease','SQL','V1.0.187__add_business_process_run_lease.sql',683945665,'flyway','2026-09-28 05:03:26',1049,1),(188,'1.0.188','add user password version','SQL','V1.0.188__add_user_password_version.sql',-82154738,'flyway','2026-09-28 05:03:27',269,1),(189,'1.0.189','normalize file metadata status','SQL','V1.0.189__normalize_file_metadata_status.sql',606155999,'flyway','2026-09-28 05:03:28',566,1),(190,'1.0.190','add user password history','SQL','V1.0.190__add_user_password_history.sql',-498996234,'flyway','2026-09-28 05:03:29',571,1),(191,'1.0.191','add business event envelope','SQL','V1.0.191__add_business_event_envelope.sql',-333060060,'flyway','2026-09-28 05:03:31',1272,1),(192,'1.0.192','add flow notify outbox','SQL','V1.0.192__add_flow_notify_outbox.sql',-1613965595,'flyway','2026-09-28 05:03:32',207,1),(193,'1.0.193','add business trigger recovery lease','SQL','V1.0.193__add_business_trigger_recovery_lease.sql',-2141281826,'flyway','2026-09-28 05:03:34',1228,1),(194,'1.0.194','add flow notify dead letter replay audit','SQL','V1.0.194__add_flow_notify_dead_letter_replay_audit.sql',-2121017030,'flyway','2026-09-28 05:03:35',740,1),(195,'1.0.195','add flow projection outbox','SQL','V1.0.195__add_flow_projection_outbox.sql',841747928,'flyway','2026-09-28 05:03:37',1533,1),(196,'1.0.196','add business event outbox','SQL','V1.0.196__add_business_event_outbox.sql',464190804,'flyway','2026-09-28 05:03:38',299,1),(197,'1.0.197','add business flow remote command','SQL','V1.0.197__add_business_flow_remote_command.sql',-968578138,'flyway','2026-09-28 05:03:39',244,1),(198,'1.0.198','add business flow callback inbox','SQL','V1.0.198__add_business_flow_callback_inbox.sql',-1015519745,'flyway','2026-09-28 05:03:40',218,1),(199,'1.0.199','add flow business action idempotency','SQL','V1.0.199__add_flow_business_action_idempotency.sql',1302571341,'flyway','2026-09-28 05:03:41',773,1),(200,'1.0.200','add business flow status reconciliation','SQL','V1.0.200__add_business_flow_status_reconciliation.sql',-769304610,'flyway','2026-09-28 05:03:43',1486,1),(201,'1.0.201','add business flow status replay audit','SQL','V1.0.201__add_business_flow_status_replay_audit.sql',-1317171955,'flyway','2026-09-28 05:03:45',862,1),(202,'1.0.202','add lowcode publish task','SQL','V1.0.202__add_lowcode_publish_task.sql',-1633805157,'flyway','2026-09-28 05:03:45',236,1),(203,'1.0.203','add lowcode publish task replay audit','SQL','V1.0.203__add_lowcode_publish_task_replay_audit.sql',-1227616488,'flyway','2026-09-28 05:03:47',820,1),(204,'1.0.204','add standalone print sources','SQL','V1.0.204__add_standalone_print_sources.sql',-898189945,'flyway','2026-09-30 06:12:36',2877,1),(205,'1.0.205','repair print center menu hierarchy','SQL','V1.0.205__repair_print_center_menu_hierarchy.sql',-881421675,'flyway','2026-09-30 06:52:23',571,1),(206,'1.0.206','unify print center workspace menu','SQL','V1.0.206__unify_print_center_workspace_menu.sql',-2108974256,'flyway','2026-09-30 07:43:47',335,1),(207,'1.0.207','move print center under platform','SQL','V1.0.207__move_print_center_under_platform.sql',-2085835241,'flyway','2026-10-05 08:17:08',219,1),(208,'1.0.208','expand tenant theme config','SQL','V1.0.208__expand_tenant_theme_config.sql',-154469782,'flyway','2026-10-05 08:17:09',230,1),(209,'1.0.209','add resource feature code','SQL','V1.0.209__add_resource_feature_code.sql',-1818823921,'flyway','2026-10-09 02:30:58',522,1),(210,'1.0.210','add plugin center','SQL','V1.0.210__add_plugin_center.sql',2014695579,'flyway','2026-10-09 02:31:01',1170,1),(211,'1.0.211','add plugin install workbench','SQL','V1.0.211__add_plugin_install_workbench.sql',1818830739,'flyway','2026-10-09 02:31:03',658,1),(212,'1.0.212','add plugin build leases','SQL','V1.0.212__add_plugin_build_leases.sql',273170986,'flyway','2026-10-09 02:31:05',541,1),(213,'1.0.213','add plugin task review','SQL','V1.0.213__add_plugin_task_review.sql',141834951,'flyway','2026-10-09 02:31:08',811,1),(214,'1.0.214','add plugin artifact registration','SQL','V1.0.214__add_plugin_artifact_registration.sql',-1407053582,'flyway','2026-10-09 02:31:10',457,1),(215,'1.0.215','add runtime license diagnostics dict','SQL','V1.0.215__add_runtime_license_diagnostics_dict.sql',777526626,'flyway','2026-10-09 02:31:11',398,1),(216,'1.0.216','add plugin delivery control','SQL','V1.0.216__add_plugin_delivery_control.sql',-1328834549,'flyway','2026-10-09 02:31:13',1341,1),(217,'1.0.217','promote plugin center to top level','SQL','V1.0.217__promote_plugin_center_to_top_level.sql',-351079171,'flyway','2026-10-10 07:13:47',216,1),(218,'1.0.218','complete ai image generating dict','SQL','V1.0.218__complete_ai_image_generating_dict.sql',-109368365,'flyway','2026-10-13 01:42:17',137,1);
/*!40000 ALTER TABLE `forge_schema_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `sys_file_storage_config`
--

LOCK TABLES `sys_file_storage_config` WRITE;
/*!40000 ALTER TABLE `sys_file_storage_config` DISABLE KEYS */;
INSERT INTO `sys_file_storage_config` (`id`, `config_name`, `storage_type`, `is_default`, `enabled`, `endpoint`, `access_key`, `secret_key`, `bucket_name`, `region`, `base_path`, `domain`, `use_https`, `max_file_size`, `allowed_types`, `order_num`, `extra_config`, `create_time`, `update_time`, `create_by`, `update_by`, `create_dept`, `del_flag`) VALUES (1,'本地存储','local',1,1,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0,100,'jpg,jpeg,png,gif,webp,pdf,doc,docx,xls,xlsx,txt,csv,zip,rar,mp4,mp3',0,NULL,'2026-10-13 10:53:17','2026-10-13 10:53:17',1,1,NULL,0);
/*!40000 ALTER TABLE `sys_file_storage_config` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-10-13 10:58:04
