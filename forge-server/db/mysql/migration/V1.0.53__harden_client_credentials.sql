-- 区分公共客户端与机密客户端，并移除内置分发式客户端无法保管的固定密钥。

SET @sys_client_exists = (
    SELECT COUNT(1)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'sys_client'
);

SET @client_auth_method_exists = (
    SELECT COUNT(1)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'sys_client'
      AND column_name = 'client_auth_method'
);

SET @add_client_auth_method_sql = IF(
    @sys_client_exists = 1 AND @client_auth_method_exists = 0,
    'ALTER TABLE sys_client
       ADD COLUMN client_auth_method varchar(32) NOT NULL DEFAULT ''client_secret''
       COMMENT ''客户端认证方式：none/client_secret'' AFTER app_secret',
    'SELECT 1'
);
PREPARE add_client_auth_method_stmt FROM @add_client_auth_method_sql;
EXECUTE add_client_auth_method_stmt;
DEALLOCATE PREPARE add_client_auth_method_stmt;

SET @allow_public_client_sql = IF(
    @sys_client_exists = 1,
    'ALTER TABLE sys_client
       MODIFY COLUMN app_secret varchar(128) NULL
       COMMENT ''应用密钥摘要，仅client_secret客户端使用''',
    'SELECT 1'
);
PREPARE allow_public_client_stmt FROM @allow_public_client_sql;
EXECUTE allow_public_client_stmt;
DEALLOCATE PREPARE allow_public_client_stmt;

SET @mark_builtin_public_clients_sql = IF(
    @sys_client_exists = 1,
    'UPDATE sys_client
        SET client_auth_method = ''none'', app_secret = NULL
      WHERE client_code IN (''pc'', ''forge_report'', ''app'', ''h5'')',
    'SELECT 1'
);
PREPARE mark_builtin_public_clients_stmt FROM @mark_builtin_public_clients_sql;
EXECUTE mark_builtin_public_clients_stmt;
DEALLOCATE PREPARE mark_builtin_public_clients_stmt;

SET @sys_config_group_exists = (
    SELECT COUNT(1)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'sys_config_group'
);

SET @add_legacy_read_control_sql = IF(
    @sys_config_group_exists = 1,
    'UPDATE sys_config_group
        SET config_value = JSON_SET(config_value, ''$.enableLegacyClientSecretRead'', TRUE)
      WHERE group_code = ''auth''
        AND JSON_VALID(config_value)
        AND JSON_CONTAINS_PATH(config_value, ''one'', ''$.enableLegacyClientSecretRead'') = 0',
    'SELECT 1'
);
PREPARE add_legacy_read_control_stmt FROM @add_legacy_read_control_sql;
EXECUTE add_legacy_read_control_stmt;
DEALLOCATE PREPARE add_legacy_read_control_stmt;

INSERT INTO sys_dict_type (
    tenant_id, dict_name, dict_type, dict_status, remark,
    create_by, create_time, update_by, update_time, create_dept
)
SELECT 1, '客户端认证方式', 'sys_client_auth_method', 1,
       '区分无法保管密钥的公共客户端和服务端机密客户端',
       1, NOW(), 1, NOW(), 1
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_dict_type data
    WHERE data.tenant_id = 1
      AND data.dict_type = 'sys_client_auth_method'
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
    SELECT 1 tenant_id, 1 dict_sort, '公共客户端' dict_label, 'none' dict_value,
           'sys_client_auth_method' dict_type, 'info' list_class, 'N' is_default,
           '浏览器、H5或分发式App，不使用无法保管的固定密钥' remark
    UNION ALL
    SELECT 1, 2, '机密客户端', 'client_secret', 'sys_client_auth_method',
           'warning', 'Y', '仅允许能够安全保管密钥的服务端客户端使用'
) seed
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_dict_data data
    WHERE data.tenant_id = seed.tenant_id
      AND data.dict_type = seed.dict_type
      AND data.dict_value = seed.dict_value
);
