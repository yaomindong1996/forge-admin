-- 清理数据库配置中的部署级 crypto 密钥。
-- 这些值已属于泄露凭据清理范围，必须物理删除；回滚只能通过外部 Secret 重新注入，禁止恢复入库。

SET @sys_config_exists = (
    SELECT COUNT(1)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'sys_config'
);

SET @cleanup_sys_config_sql = IF(@sys_config_exists > 0,
    'DELETE FROM sys_config
      WHERE config_key IN (
        ''forge.crypto.secret-key'',
        ''forge.crypto.secretKey'',
        ''forge.crypto.rsa-public-key'',
        ''forge.crypto.rsaPublicKey'',
        ''forge.crypto.rsa-private-key'',
        ''forge.crypto.rsaPrivateKey'',
        ''forge.crypto.persistence.active-key'',
        ''forge.crypto.persistence.activeKey'',
        ''forge.crypto.persistence.legacy-key'',
        ''forge.crypto.persistence.legacyKey'',
        ''forge.crypto.persistence.keys'',
        ''forge.crypto.persistence.historical-keys'',
        ''forge.crypto.persistence.historicalKeys''
      )
      OR config_key LIKE ''forge.crypto.persistence.keys.%''
      OR config_key LIKE ''forge.crypto.persistence.keys[%''
      OR config_key LIKE ''forge.crypto.persistence.historical-keys.%''
      OR config_key LIKE ''forge.crypto.persistence.historical-keys[%''
      OR config_key LIKE ''forge.crypto.persistence.historicalKeys.%''
      OR config_key LIKE ''forge.crypto.persistence.historicalKeys[%''',
    'SELECT 1'
);

PREPARE cleanup_sys_config_stmt FROM @cleanup_sys_config_sql;
EXECUTE cleanup_sys_config_stmt;
DEALLOCATE PREPARE cleanup_sys_config_stmt;

SET @sys_config_group_exists = (
    SELECT COUNT(1)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'sys_config_group'
);

SET @cleanup_sys_config_group_sql = IF(@sys_config_group_exists > 0,
    'UPDATE sys_config_group
        SET config_value = CASE
            WHEN JSON_VALID(config_value) THEN JSON_REMOVE(
                config_value,
                ''$.secretKey'',
                ''$.rsaPublicKey'',
                ''$.rsaPrivateKey'',
                ''$.persistence'',
                ''$.activeKey'',
                ''$.legacyKey'',
                ''$.keys'',
                ''$.historicalKeys''
            )
            ELSE JSON_OBJECT()
        END
      WHERE group_code = ''crypto''
        AND config_value IS NOT NULL',
    'SELECT 1'
);

PREPARE cleanup_sys_config_group_stmt FROM @cleanup_sys_config_group_sql;
EXECUTE cleanup_sys_config_group_stmt;
DEALLOCATE PREPARE cleanup_sys_config_group_stmt;
