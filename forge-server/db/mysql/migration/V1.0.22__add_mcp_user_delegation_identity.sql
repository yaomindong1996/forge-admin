-- Forge AI 中枢阶段 2.0：MCP OAuth 用户委托身份与短期访问令牌。
-- 原始 access token、authorization code、code verifier 和 Forge 登录 Token 禁止落库。

SET @client_table_exists = (
  SELECT COUNT(1) FROM information_schema.tables
  WHERE table_schema = DATABASE() AND table_name = 'ai_capability_client'
);

SET @column_exists = (
  SELECT COUNT(1) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'ai_capability_client' AND column_name = 'oauth_enabled'
);
SET @sql = IF(@client_table_exists > 0 AND @column_exists = 0,
  'ALTER TABLE ai_capability_client ADD COLUMN oauth_enabled tinyint NOT NULL DEFAULT 0 COMMENT ''是否允许OAuth换取短期令牌'' AFTER active_org_id',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (
  SELECT COUNT(1) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'ai_capability_client' AND column_name = 'oauth_client_type'
);
SET @sql = IF(@client_table_exists > 0 AND @column_exists = 0,
  'ALTER TABLE ai_capability_client ADD COLUMN oauth_client_type varchar(16) NOT NULL DEFAULT ''CONFIDENTIAL'' COMMENT ''OAuth客户端类型（PUBLIC/CONFIDENTIAL）'' AFTER oauth_enabled',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS ai_capability_oauth_redirect_uri (
  id bigint NOT NULL,
  tenant_id bigint NOT NULL DEFAULT 1,
  client_id bigint NOT NULL,
  redirect_uri varchar(2048) NOT NULL,
  redirect_uri_hash char(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  status varchar(16) NOT NULL DEFAULT 'ENABLED',
  del_flag tinyint NOT NULL DEFAULT 0,
  logic_delete_active tinyint GENERATED ALWAYS AS (CASE WHEN del_flag = 0 THEN 1 ELSE NULL END) STORED,
  create_by bigint DEFAULT NULL,
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_dept bigint DEFAULT NULL,
  update_by bigint DEFAULT NULL,
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_ai_capability_redirect_active (tenant_id, client_id, redirect_uri_hash, logic_delete_active),
  KEY idx_ai_capability_redirect_client (tenant_id, client_id, status, del_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI中枢OAuth客户端精确回调地址';

CREATE TABLE IF NOT EXISTS ai_capability_access_token (
  id bigint NOT NULL,
  tenant_id bigint NOT NULL DEFAULT 1,
  token_key_id varchar(22) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  token_prefix varchar(32) NOT NULL,
  token_hash char(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  client_id bigint NOT NULL,
  credential_version int NOT NULL,
  actor_type varchar(16) NOT NULL,
  actor_user_id bigint NOT NULL,
  service_user_id bigint NOT NULL,
  active_org_id bigint NOT NULL,
  audience varchar(512) NOT NULL,
  scopes varchar(2000) NOT NULL,
  status varchar(16) NOT NULL DEFAULT 'ACTIVE',
  issued_at datetime NOT NULL,
  expires_at datetime NOT NULL,
  last_used_at datetime DEFAULT NULL,
  revoked_at datetime DEFAULT NULL,
  del_flag tinyint NOT NULL DEFAULT 0,
  logic_delete_active tinyint GENERATED ALWAYS AS (CASE WHEN del_flag = 0 THEN 1 ELSE NULL END) STORED,
  create_by bigint DEFAULT NULL,
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_dept bigint DEFAULT NULL,
  update_by bigint DEFAULT NULL,
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_ai_capability_token_key_active (token_key_id, logic_delete_active),
  KEY idx_ai_capability_token_client (tenant_id, client_id, status, expires_at, del_flag),
  KEY idx_ai_capability_token_actor (tenant_id, actor_user_id, status, expires_at, del_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI中枢MCP短期访问令牌安全元数据';

INSERT INTO sys_dict_type (
  tenant_id, dict_name, dict_type, dict_status, remark,
  create_by, create_time, update_by, update_time, create_dept
)
SELECT 1, 'AI能力OAuth客户端类型', 'ai_capability_oauth_client_type', 1,
       'MCP OAuth客户端是PUBLIC或CONFIDENTIAL', 1, NOW(), 1, NOW(), 1
WHERE NOT EXISTS (
  SELECT 1 FROM sys_dict_type t
  WHERE t.tenant_id = 1 AND t.dict_type = 'ai_capability_oauth_client_type'
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
  SELECT 1 tenant_id, 1 dict_sort, '机密客户端' dict_label, 'CONFIDENTIAL' dict_value,
         'ai_capability_oauth_client_type' dict_type, 'info' list_class, 'Y' is_default,
         '服务端客户端，换令牌时必须验证客户端密钥' remark
  UNION ALL
  SELECT 1, 2, '公共客户端', 'PUBLIC', 'ai_capability_oauth_client_type',
         'warning', 'N', '桌面或本地客户端，只允许授权码加PKCE S256'
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM sys_dict_data d
  WHERE d.tenant_id = seed.tenant_id
    AND d.dict_type = seed.dict_type
    AND d.dict_value = seed.dict_value
);
