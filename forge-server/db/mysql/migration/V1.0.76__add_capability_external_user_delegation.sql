-- Forge 能力开放平台：无服务账号绑定的外部 USER 委托、OIDC 身份稳定映射。

SET @client_table_exists = (
  SELECT COUNT(1) FROM information_schema.tables
  WHERE table_schema = DATABASE() AND table_name = 'ai_capability_client'
);

SET @column_exists = (
  SELECT COUNT(1) FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'ai_capability_client'
    AND column_name = 'actor_mode'
);
SET @sql = IF(@client_table_exists > 0 AND @column_exists = 0,
  'ALTER TABLE ai_capability_client ADD COLUMN actor_mode varchar(24) NOT NULL DEFAULT ''HYBRID'' COMMENT ''客户端主体模式：USER_DELEGATION/SERVICE/HYBRID'' AFTER auth_modes',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @service_user_not_nullable = (
  SELECT COUNT(1) FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'ai_capability_client'
    AND column_name = 'service_user_id'
    AND is_nullable = 'NO'
);
SET @sql = IF(@client_table_exists > 0 AND @service_user_not_nullable > 0,
  'ALTER TABLE ai_capability_client MODIFY COLUMN service_user_id bigint DEFAULT NULL COMMENT ''SERVICE/HYBRID 客户端绑定服务账号；USER_DELEGATION 为空''',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @active_org_not_nullable = (
  SELECT COUNT(1) FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'ai_capability_client'
    AND column_name = 'active_org_id'
    AND is_nullable = 'NO'
);
SET @sql = IF(@client_table_exists > 0 AND @active_org_not_nullable > 0,
  'ALTER TABLE ai_capability_client MODIFY COLUMN active_org_id bigint DEFAULT NULL COMMENT ''SERVICE/HYBRID 客户端固定组织；USER_DELEGATION 为空''',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @token_service_user_not_nullable = (
  SELECT COUNT(1) FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'ai_capability_access_token'
    AND column_name = 'service_user_id'
    AND is_nullable = 'NO'
);
SET @sql = IF(@token_service_user_not_nullable > 0,
  'ALTER TABLE ai_capability_access_token MODIFY COLUMN service_user_id bigint DEFAULT NULL COMMENT ''SERVICE/HYBRID 服务账号；纯 USER 委托为空''',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @flow_log_service_user_not_nullable = (
  SELECT COUNT(1) FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'ai_capability_flow_action_log'
    AND column_name = 'service_user_id'
    AND is_nullable = 'NO'
);
SET @sql = IF(@flow_log_service_user_not_nullable > 0,
  'ALTER TABLE ai_capability_flow_action_log MODIFY COLUMN service_user_id bigint DEFAULT NULL COMMENT ''SERVICE/HYBRID 服务账号；纯 USER 委托为空''',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS ai_capability_external_identity (
  id bigint NOT NULL COMMENT '主键ID',
  tenant_id bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  provider_code varchar(64) NOT NULL COMMENT '受信OIDC提供方编码',
  issuer_hash char(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'issuer SHA-256',
  subject_hash char(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'subject SHA-256',
  user_id bigint NOT NULL COMMENT '自动映射的Forge用户ID',
  status varchar(16) NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED',
  last_authenticated_at datetime DEFAULT NULL COMMENT '最近认证时间',
  del_flag bigint NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0正常，删除后写主键',
  create_by bigint DEFAULT NULL COMMENT '创建人ID',
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  create_dept bigint DEFAULT NULL COMMENT '创建部门ID',
  update_by bigint DEFAULT NULL COMMENT '更新人ID',
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_cap_external_identity_active (
    tenant_id, provider_code, issuer_hash, subject_hash, del_flag
  ),
  KEY idx_cap_external_identity_user (tenant_id, user_id, status, del_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI能力受信外部用户身份映射';

INSERT INTO sys_dict_type (
  tenant_id, dict_name, dict_type, dict_status, remark,
  create_by, create_time, update_by, update_time, create_dept
)
SELECT 1, 'AI能力客户端主体模式', 'ai_capability_client_actor_mode', 1,
       '外围客户端执行主体来源', 1, NOW(), 1, NOW(), 1
WHERE NOT EXISTS (
  SELECT 1 FROM sys_dict_type t
  WHERE t.tenant_id = 1 AND t.dict_type = 'ai_capability_client_actor_mode'
);

INSERT INTO sys_dict_data (
  tenant_id, dict_sort, dict_label, dict_value, dict_type,
  css_class, list_class, is_default, dict_status, remark,
  create_by, create_time, update_by, update_time, create_dept
)
SELECT seed.tenant_id, seed.dict_sort, seed.dict_label, seed.dict_value, seed.dict_type,
       NULL, seed.list_class, seed.is_default, 1, seed.remark,
       1, NOW(), 1, NOW(), 1
FROM (
  SELECT 1 tenant_id, 1 dict_sort, '用户委托' dict_label, 'USER_DELEGATION' dict_value,
         'ai_capability_client_actor_mode' dict_type, 'success' list_class, 'Y' is_default,
         '通过受信OIDC/JWT动态映射实际用户，无需绑定服务账号' remark
  UNION ALL
  SELECT 1, 2, '服务身份', 'SERVICE', 'ai_capability_client_actor_mode',
         'info', 'N', '固定绑定服务账号和活动组织'
  UNION ALL
  SELECT 1, 3, '混合模式', 'HYBRID', 'ai_capability_client_actor_mode',
         'warning', 'N', '同时支持服务身份和用户委托，需绑定服务账号'
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM sys_dict_data d
  WHERE d.tenant_id = seed.tenant_id
    AND d.dict_type = seed.dict_type
    AND d.dict_value = seed.dict_value
);
