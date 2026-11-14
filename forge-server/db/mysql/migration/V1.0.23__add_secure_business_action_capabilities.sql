-- Forge AI 中枢阶段 2.1：受控业务动作双身份审计关联与管理权限。

SET @action_log_exists = (
  SELECT COUNT(1) FROM information_schema.tables
  WHERE table_schema = DATABASE() AND table_name = 'ai_business_action_execution_log'
);

SET @column_exists = (
  SELECT COUNT(1) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'ai_business_action_execution_log'
    AND column_name = 'capability_request_id'
);
SET @sql = IF(@action_log_exists > 0 AND @column_exists = 0,
  'ALTER TABLE ai_business_action_execution_log ADD COLUMN capability_request_id varchar(64) DEFAULT NULL COMMENT ''AI能力调用请求ID'' AFTER duration_ms',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (
  SELECT COUNT(1) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'ai_business_action_execution_log'
    AND column_name = 'client_id'
);
SET @sql = IF(@action_log_exists > 0 AND @column_exists = 0,
  'ALTER TABLE ai_business_action_execution_log ADD COLUMN client_id bigint DEFAULT NULL COMMENT ''AI能力机器客户端ID'' AFTER capability_request_id',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (
  SELECT COUNT(1) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'ai_business_action_execution_log'
    AND column_name = 'service_user_id'
);
SET @sql = IF(@action_log_exists > 0 AND @column_exists = 0,
  'ALTER TABLE ai_business_action_execution_log ADD COLUMN service_user_id bigint DEFAULT NULL COMMENT ''AI能力绑定服务账号ID'' AFTER client_id',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (
  SELECT COUNT(1) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'ai_business_action_execution_log'
    AND column_name = 'actor_type'
);
SET @sql = IF(@action_log_exists > 0 AND @column_exists = 0,
  'ALTER TABLE ai_business_action_execution_log ADD COLUMN actor_type varchar(16) DEFAULT NULL COMMENT ''AI能力实际主体类型（USER/SERVICE）'' AFTER service_user_id',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @index_exists = (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'ai_business_action_execution_log'
    AND index_name = 'idx_ai_business_action_capability_request'
);
SET @sql = IF(@action_log_exists > 0 AND @index_exists = 0,
  'CREATE INDEX idx_ai_business_action_capability_request ON ai_business_action_execution_log (tenant_id, capability_request_id)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ai_parent_id := COALESCE(
  (SELECT parent_id FROM (
    SELECT parent_id FROM sys_resource
    WHERE tenant_id = 1 AND path = '/ai/provider-model' AND del_flag = 0
    LIMIT 1
  ) x),
  0
);

INSERT INTO sys_resource (
  tenant_id, resource_name, parent_id, resource_type, sort,
  is_external, open_target, is_public, menu_status, visible, perms,
  keep_alive, always_show, remark, create_by, create_time,
  update_by, update_time, create_dept, client_code
)
SELECT 1, seed.resource_name, @ai_parent_id, 3, seed.sort,
       0, '_self', 0, 1, 1, seed.perms,
       0, 0, seed.remark, 1, NOW(), 1, NOW(), 1, 'pc'
FROM (
  SELECT '发布受控业务动作' resource_name, 30 sort,
         'ai:capability:business-action:publish' perms,
         '从业务对象发布快照创建受控AI动作能力' remark
  UNION ALL
  SELECT '调用受控业务动作', 31,
         'ai:capability:business-action:invoke',
         '允许具体用户通过MCP确认后调用受控业务动作'
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM sys_resource r
  WHERE r.tenant_id = 1 AND r.perms = seed.perms AND r.del_flag = 0
);

INSERT INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT 1, admin_role.id, resource.id, NOW()
FROM (SELECT id FROM sys_role WHERE tenant_id = 1 AND role_key = 'admin' AND del_flag = 0 ORDER BY id LIMIT 1) admin_role
JOIN sys_resource resource ON resource.tenant_id = 1 AND resource.del_flag = 0
WHERE resource.perms IN (
  'ai:capability:business-action:publish',
  'ai:capability:business-action:invoke'
)
AND NOT EXISTS (
  SELECT 1 FROM sys_role_resource existing
  WHERE existing.tenant_id = 1
    AND existing.role_id = admin_role.id
    AND existing.resource_id = resource.id
);
