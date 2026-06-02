-- Lowcode business document and flow closure foundation.

CREATE TABLE IF NOT EXISTS `ai_business_document_config` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  `object_id` bigint DEFAULT NULL COMMENT '业务对象ID',
  `suite_code` varchar(48) NOT NULL COMMENT '业务套件编码',
  `object_code` varchar(48) NOT NULL COMMENT '业务对象编码',
  `config_key` varchar(128) DEFAULT NULL COMMENT '动态CRUD运行配置键',
  `document_name` varchar(128) NOT NULL COMMENT '单据名称',
  `document_no_rule` varchar(256) DEFAULT NULL COMMENT '单据编号规则',
  `document_enabled` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否启用单据模式',
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
  UNIQUE KEY `uk_ai_business_document_object_code` (`tenant_id`, `object_code`),
  UNIQUE KEY `uk_ai_business_document_object_id` (`tenant_id`, `object_id`),
  KEY `idx_ai_business_document_suite` (`tenant_id`, `suite_code`),
  KEY `idx_ai_business_document_config` (`tenant_id`, `config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI业务应用平台-业务单据配置表';

CREATE TABLE IF NOT EXISTS `ai_business_flow_instance_link` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
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
  `variables_snapshot` json DEFAULT NULL COMMENT '流程变量快照',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_business_flow_process` (`tenant_id`, `process_instance_id`),
  UNIQUE KEY `uk_ai_business_flow_business_process` (`tenant_id`, `business_key`, `process_instance_id`),
  KEY `idx_ai_business_flow_business_key` (`tenant_id`, `business_key`, `flow_status`),
  KEY `idx_ai_business_flow_record` (`tenant_id`, `object_code`, `record_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI业务应用平台-单据流程实例关联表';

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE ai_business_trigger ADD COLUMN scenario_type varchar(64) DEFAULT NULL COMMENT ''业务场景模板类型'' AFTER trigger_type',
    'SELECT 1'
  )
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'ai_business_trigger'
    AND column_name = 'scenario_type'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE ai_business_trigger ADD COLUMN blocking_mode varchar(32) NOT NULL DEFAULT ''ASYNC'' COMMENT ''阻断模式：ASYNC异步/SYNC_BLOCK同步阻断'' AFTER scenario_type',
    'SELECT 1'
  )
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'ai_business_trigger'
    AND column_name = 'blocking_mode'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE ai_business_trigger ADD COLUMN developer_mode tinyint(1) NOT NULL DEFAULT 0 COMMENT ''是否开发者高级模式'' AFTER blocking_mode',
    'SELECT 1'
  )
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'ai_business_trigger'
    AND column_name = 'developer_mode'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE ai_business_trigger_log ADD COLUMN todo_code varchar(64) DEFAULT NULL COMMENT ''TODO状态编码'' AFTER error_message',
    'SELECT 1'
  )
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'ai_business_trigger_log'
    AND column_name = 'todo_code'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE ai_business_trigger_log ADD COLUMN correlation_id varchar(128) DEFAULT NULL COMMENT ''关联流程/消息/外部动作ID'' AFTER todo_code',
    'SELECT 1'
  )
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'ai_business_trigger_log'
    AND column_name = 'correlation_id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE sys_resource
SET menu_status = 0,
    visible = 0,
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND resource_type = 2
  AND perms IN ('ai:businessMobile:list', 'ai:businessIntegration:list');

SET @app_center_menu_id := (
  SELECT id
  FROM sys_resource
  WHERE tenant_id = 1
    AND resource_type = 2
    AND perms = 'ai:businessApp:list'
  ORDER BY id
  LIMIT 1
);

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external,
                          sso_enabled, sso_target_client, open_target, is_public, menu_status, visible, perms, icon,
                          api_method, api_url, keep_alive, always_show, redirect, remark, create_by, create_time,
                          update_by, update_time, create_dept, client_code)
SELECT 1, seed.resource_name, COALESCE(@app_center_menu_id, 0), 3, seed.sort, NULL, NULL, 0,
       0, NULL, '_self', 0, 1, 1, seed.perms, NULL,
       NULL, NULL, 0, 0, NULL, seed.remark, 1, NOW(), 1, NOW(), 1, 'pc'
FROM (
  SELECT '配置单据' resource_name, 25 sort, 'ai:businessDocument:config' perms, '配置业务对象单据模式权限' remark
  UNION ALL SELECT '查看单据运行态', 26, 'ai:businessDocument:view', '查看业务单据运行态权限'
  UNION ALL SELECT '配置业务流程', 27, 'ai:businessFlow:config', '配置业务对象流程绑定权限'
  UNION ALL SELECT '查看业务流程', 28, 'ai:businessFlow:view', '查看业务记录流程状态权限'
) seed
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_resource r
  WHERE r.tenant_id = 1
    AND r.resource_type = 3
    AND r.perms = seed.perms
);
