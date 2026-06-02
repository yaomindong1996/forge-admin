-- 低代码应用全链路闭环：触发器规则表、触发器执行日志表、消息推送通道表

-- ========== 1. 触发器规则表 ==========
CREATE TABLE IF NOT EXISTS `ai_business_trigger` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  `suite_code` varchar(48) NOT NULL COMMENT '所属业务套件编码',
  `object_code` varchar(48) NOT NULL COMMENT '所属业务对象编码',
  `trigger_name` varchar(128) NOT NULL COMMENT '触发器名称',
  `trigger_desc` varchar(500) DEFAULT NULL COMMENT '触发器说明',
  `trigger_type` varchar(32) NOT NULL DEFAULT 'EVENT' COMMENT '触发器类型：EVENT事件触发/SCHEDULE定时触发/MANUAL手动触发',
  `event_type` varchar(64) DEFAULT NULL COMMENT '事件类型：RECORD_CREATED/RECORD_UPDATED/RECORD_DELETED/STATUS_CHANGED/FIELD_CHANGED',
  `event_condition` json DEFAULT NULL COMMENT '事件条件表达式JSON',
  `action_type` varchar(32) NOT NULL COMMENT '动作类型：START_FLOW/SEND_MESSAGE/CREATE_RECORD/UPDATE_FIELD/WEBHOOK',
  `action_config` json NOT NULL COMMENT '动作配置JSON',
  `status` int NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序',
  `execute_count` bigint NOT NULL DEFAULT 0 COMMENT '累计执行次数',
  `last_execute_time` datetime DEFAULT NULL COMMENT '最近执行时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_ai_business_trigger_object` (`tenant_id`, `suite_code`, `object_code`, `status`),
  KEY `idx_ai_business_trigger_event` (`tenant_id`, `object_code`, `event_type`, `status`),
  KEY `idx_ai_business_trigger_type` (`tenant_id`, `trigger_type`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI业务应用平台-触发器规则表';

-- ========== 2. 触发器执行日志表 ==========
CREATE TABLE IF NOT EXISTS `ai_business_trigger_log` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  `trigger_id` bigint NOT NULL COMMENT '触发器ID',
  `trigger_name` varchar(128) DEFAULT NULL COMMENT '触发器名称',
  `suite_code` varchar(48) NOT NULL COMMENT '业务套件编码',
  `object_code` varchar(48) NOT NULL COMMENT '业务对象编码',
  `record_id` varchar(64) DEFAULT NULL COMMENT '触发记录ID',
  `event_type` varchar(64) NOT NULL COMMENT '触发事件类型',
  `event_data` json DEFAULT NULL COMMENT '事件数据快照',
  `action_type` varchar(32) NOT NULL COMMENT '执行动作类型',
  `action_result` json DEFAULT NULL COMMENT '执行结果',
  `execute_status` varchar(32) NOT NULL DEFAULT 'PENDING' COMMENT '执行状态：PENDING/SUCCESS/FAILED/SKIPPED',
  `error_message` varchar(2000) DEFAULT NULL COMMENT '错误信息',
  `execute_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '执行时间',
  `duration_ms` bigint DEFAULT NULL COMMENT '执行耗时(毫秒)',
  `retry_count` int NOT NULL DEFAULT 0 COMMENT '重试次数',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_ai_trigger_log_trigger` (`tenant_id`, `trigger_id`, `execute_status`),
  KEY `idx_ai_trigger_log_object` (`tenant_id`, `object_code`, `execute_time`),
  KEY `idx_ai_trigger_log_record` (`tenant_id`, `object_code`, `record_id`),
  KEY `idx_ai_trigger_log_time` (`tenant_id`, `execute_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI业务应用平台-触发器执行日志表';

-- ========== 3. 消息推送通道表（第三方平台框架预留） ==========
CREATE TABLE IF NOT EXISTS `ai_business_message_channel` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  `channel_code` varchar(64) NOT NULL COMMENT '通道编码',
  `channel_name` varchar(128) NOT NULL COMMENT '通道名称',
  `channel_type` varchar(32) NOT NULL COMMENT '通道类型：WECHAT_WORK/FEISHU/DINGTALK/WEBHOOK/INTERNAL',
  `channel_config_ref` varchar(256) DEFAULT NULL COMMENT '通道配置安全引用（不存明文密钥）',
  `webhook_url` varchar(512) DEFAULT NULL COMMENT 'Webhook地址（仅WEBHOOK类型）',
  `description` varchar(500) DEFAULT NULL COMMENT '通道说明',
  `status` int NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序',
  `options` json DEFAULT NULL COMMENT '扩展配置',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_message_channel_code` (`tenant_id`, `channel_code`),
  KEY `idx_ai_message_channel_type` (`tenant_id`, `channel_type`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI业务应用平台-消息推送通道表';

-- ========== 4. 初始化内置站内信通道 ==========
INSERT INTO ai_business_message_channel (id, tenant_id, channel_code, channel_name, channel_type, channel_config_ref,
                                          description, status, sort_order, create_by, create_time, create_dept,
                                          update_by, update_time)
SELECT 1910000000000001001, 1, 'internal_websocket', '站内信（WebSocket）', 'INTERNAL', NULL,
       '系统内置站内信通道，通过WebSocket实时推送', 1, 0, 1, NOW(), 1, 1, NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM ai_business_message_channel
  WHERE tenant_id = 1 AND channel_code = 'internal_websocket'
);

-- ========== 5. 初始化第三方通道占位（TODO状态） ==========
INSERT INTO ai_business_message_channel (id, tenant_id, channel_code, channel_name, channel_type, channel_config_ref,
                                          description, status, sort_order, create_by, create_time, create_dept,
                                          update_by, update_time)
SELECT 1910000000000001002, 1, 'wechat_work', '企业微信', 'WECHAT_WORK', NULL,
       '企业微信消息推送通道（待实现）', 0, 1, 1, NOW(), 1, 1, NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM ai_business_message_channel
  WHERE tenant_id = 1 AND channel_code = 'wechat_work'
);

INSERT INTO ai_business_message_channel (id, tenant_id, channel_code, channel_name, channel_type, channel_config_ref,
                                          description, status, sort_order, create_by, create_time, create_dept,
                                          update_by, update_time)
SELECT 1910000000000001003, 1, 'feishu', '飞书', 'FEISHU', NULL,
       '飞书消息推送通道（待实现）', 0, 2, 1, NOW(), 1, 1, NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM ai_business_message_channel
  WHERE tenant_id = 1 AND channel_code = 'feishu'
);

INSERT INTO ai_business_message_channel (id, tenant_id, channel_code, channel_name, channel_type, channel_config_ref,
                                          description, status, sort_order, create_by, create_time, create_dept,
                                          update_by, update_time)
SELECT 1910000000000001004, 1, 'dingtalk', '钉钉', 'DINGTALK', NULL,
       '钉钉消息推送通道（待实现）', 0, 3, 1, NOW(), 1, 1, NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM ai_business_message_channel
  WHERE tenant_id = 1 AND channel_code = 'dingtalk'
);

-- ========== 6. 触发器相关菜单权限 ==========
SET @app_center_menu_id := (
  SELECT id FROM sys_resource
  WHERE tenant_id = 1 AND resource_type = 2 AND perms = 'ai:businessApp:list'
  LIMIT 1
);

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external,
                          sso_enabled, sso_target_client, open_target, is_public, menu_status, visible, perms, icon,
                          api_method, api_url, keep_alive, always_show, redirect, remark, create_by, create_time,
                          update_by, update_time, create_dept, client_code)
SELECT 1, '触发器管理', COALESCE(@app_center_menu_id, 0), 3, 20, NULL, NULL, 0,
       0, NULL, '_self', 0, 1, 1, 'ai:businessTrigger:list', NULL,
       NULL, NULL, 0, 0, NULL, '业务触发器列表权限', 1, NOW(), 1, NOW(), 1, 'pc'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_resource WHERE tenant_id = 1 AND resource_type = 3 AND perms = 'ai:businessTrigger:list'
);

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external,
                          sso_enabled, sso_target_client, open_target, is_public, menu_status, visible, perms, icon,
                          api_method, api_url, keep_alive, always_show, redirect, remark, create_by, create_time,
                          update_by, update_time, create_dept, client_code)
SELECT 1, '新增触发器', COALESCE(@app_center_menu_id, 0), 3, 21, NULL, NULL, 0,
       0, NULL, '_self', 0, 1, 1, 'ai:businessTrigger:add', NULL,
       NULL, NULL, 0, 0, NULL, '新增业务触发器按钮权限', 1, NOW(), 1, NOW(), 1, 'pc'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_resource WHERE tenant_id = 1 AND resource_type = 3 AND perms = 'ai:businessTrigger:add'
);

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external,
                          sso_enabled, sso_target_client, open_target, is_public, menu_status, visible, perms, icon,
                          api_method, api_url, keep_alive, always_show, redirect, remark, create_by, create_time,
                          update_by, update_time, create_dept, client_code)
SELECT 1, '编辑触发器', COALESCE(@app_center_menu_id, 0), 3, 22, NULL, NULL, 0,
       0, NULL, '_self', 0, 1, 1, 'ai:businessTrigger:edit', NULL,
       NULL, NULL, 0, 0, NULL, '编辑业务触发器按钮权限', 1, NOW(), 1, NOW(), 1, 'pc'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_resource WHERE tenant_id = 1 AND resource_type = 3 AND perms = 'ai:businessTrigger:edit'
);

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external,
                          sso_enabled, sso_target_client, open_target, is_public, menu_status, visible, perms, icon,
                          api_method, api_url, keep_alive, always_show, redirect, remark, create_by, create_time,
                          update_by, update_time, create_dept, client_code)
SELECT 1, '删除触发器', COALESCE(@app_center_menu_id, 0), 3, 23, NULL, NULL, 0,
       0, NULL, '_self', 0, 1, 1, 'ai:businessTrigger:delete', NULL,
       NULL, NULL, 0, 0, NULL, '删除业务触发器按钮权限', 1, NOW(), 1, NOW(), 1, 'pc'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_resource WHERE tenant_id = 1 AND resource_type = 3 AND perms = 'ai:businessTrigger:delete'
);

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external,
                          sso_enabled, sso_target_client, open_target, is_public, menu_status, visible, perms, icon,
                          api_method, api_url, keep_alive, always_show, redirect, remark, create_by, create_time,
                          update_by, update_time, create_dept, client_code)
SELECT 1, '流程发起', COALESCE(@app_center_menu_id, 0), 3, 24, NULL, NULL, 0,
       0, NULL, '_self', 0, 1, 1, 'ai:businessFlow:start', NULL,
       NULL, NULL, 0, 0, NULL, '从业务记录发起流程权限', 1, NOW(), 1, NOW(), 1, 'pc'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_resource WHERE tenant_id = 1 AND resource_type = 3 AND perms = 'ai:businessFlow:start'
);
