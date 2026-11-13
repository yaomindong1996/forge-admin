-- Forge 能力开放平台：无 OIDC 客户端的 RS256 用户身份断言与预绑定映射。
-- 私钥绝不落库；客户端表只保存公钥、kid 和轮换版本。

SET @client_table_exists = (
  SELECT COUNT(1) FROM information_schema.tables
  WHERE table_schema = DATABASE() AND table_name = 'ai_capability_client'
);

SET @column_exists = (
  SELECT COUNT(1) FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'ai_capability_client'
    AND column_name = 'user_assertion_enabled'
);
SET @sql = IF(@client_table_exists > 0 AND @column_exists = 0,
  'ALTER TABLE ai_capability_client ADD COLUMN user_assertion_enabled tinyint NOT NULL DEFAULT 0 COMMENT ''是否允许客户端签名用户断言：0否/1是'' AFTER signing_key_version',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (
  SELECT COUNT(1) FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'ai_capability_client'
    AND column_name = 'user_assertion_key_id'
);
SET @sql = IF(@client_table_exists > 0 AND @column_exists = 0,
  'ALTER TABLE ai_capability_client ADD COLUMN user_assertion_key_id varchar(64) DEFAULT NULL COMMENT ''客户端用户断言 RSA 公钥 kid'' AFTER user_assertion_enabled',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (
  SELECT COUNT(1) FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'ai_capability_client'
    AND column_name = 'user_assertion_public_key'
);
SET @sql = IF(@client_table_exists > 0 AND @column_exists = 0,
  'ALTER TABLE ai_capability_client ADD COLUMN user_assertion_public_key text NULL COMMENT ''客户端用户断言 RSA X.509 PEM 公钥；不保存私钥'' AFTER user_assertion_key_id',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (
  SELECT COUNT(1) FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'ai_capability_client'
    AND column_name = 'user_assertion_key_version'
);
SET @sql = IF(@client_table_exists > 0 AND @column_exists = 0,
  'ALTER TABLE ai_capability_client ADD COLUMN user_assertion_key_version int DEFAULT NULL COMMENT ''客户端用户断言密钥版本；NULL表示从未启用'' AFTER user_assertion_public_key',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @identity_table_exists = (
  SELECT COUNT(1) FROM information_schema.tables
  WHERE table_schema = DATABASE() AND table_name = 'ai_capability_external_identity'
);

SET @column_exists = (
  SELECT COUNT(1) FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'ai_capability_external_identity'
    AND column_name = 'subject_hint'
);
SET @sql = IF(@identity_table_exists > 0 AND @column_exists = 0,
  'ALTER TABLE ai_capability_external_identity ADD COLUMN subject_hint varchar(128) DEFAULT NULL COMMENT ''外围用户标识脱敏提示；原文不落库'' AFTER subject_hash',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
