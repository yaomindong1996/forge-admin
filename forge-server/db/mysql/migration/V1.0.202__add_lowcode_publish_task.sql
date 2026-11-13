-- 低代码发布跨阶段可靠任务；运行态队列表按状态留痕，不使用逻辑删除。
-- 本阶段先接管事务提交后的菜单/业务入口同步，后续在线 DDL 编排复用同一任务状态机。
-- 回滚：先关闭 forge.lowcode.publish-task.enabled 并确认无 PENDING/PROCESSING/RETRY，再归档后删除本表。

CREATE TABLE IF NOT EXISTS `ai_lowcode_publish_task` (
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
  `retry_count` int NOT NULL DEFAULT 0 COMMENT '认领次数',
  `next_retry_time` datetime DEFAULT NULL COMMENT '下次重试时间',
  `lock_owner` varchar(128) DEFAULT NULL COMMENT '租约持有者',
  `lock_time` datetime DEFAULT NULL COMMENT '租约时间',
  `error_type` varchar(128) DEFAULT NULL COMMENT '失败类型',
  `completed_time` datetime DEFAULT NULL COMMENT '完成或失效时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_lowcode_publish_task_request` (`tenant_id`, `request_id`),
  KEY `idx_ai_lowcode_publish_task_scan` (`task_status`, `next_retry_time`, `lock_time`, `id`),
  KEY `idx_ai_lowcode_publish_task_config` (`tenant_id`, `config_id`, `version_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='低代码发布可靠任务';
