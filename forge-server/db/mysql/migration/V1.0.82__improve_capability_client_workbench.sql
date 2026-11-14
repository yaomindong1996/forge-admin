-- 能力开放平台客户端工作台：可信用户映射规则和安全调用错误摘要。

SET @column_exists = (
  SELECT COUNT(1) FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'ai_capability_client'
    AND column_name = 'user_assertion_mapping_mode'
);
SET @sql = IF(@column_exists = 0,
  'ALTER TABLE ai_capability_client ADD COLUMN user_assertion_mapping_mode varchar(24) NOT NULL DEFAULT ''PREBOUND'' COMMENT ''用户断言映射模式：PREBOUND/VERIFIED_PHONE'' AFTER user_assertion_key_version',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 主导航聚合到客户端工作台；旧授权和日志路由保留用于兼容直达链接，但不再占用独立菜单。
UPDATE sys_resource
SET resource_name = '客户端工作台',
    remark = '按客户端聚合凭据、能力授权、外围用户映射与调用日志',
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND resource_type = 2
  AND path = '/open-platform/capability-client'
  AND del_flag = 0;

UPDATE sys_resource
SET visible = 0,
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND resource_type = 2
  AND path IN ('/open-platform/capability-grant', '/open-platform/capability-invocation')
  AND del_flag = 0;

INSERT INTO sys_dict_type (
  tenant_id, dict_name, dict_type, dict_status, remark,
  create_by, create_time, update_by, update_time, create_dept
)
SELECT 1, 'AI能力外围用户映射模式', 'ai_capability_user_mapping_mode', 1,
       '客户端签名用户断言到 Forge 普通用户的受控映射规则',
       1, NOW(), 1, NOW(), 1
WHERE NOT EXISTS (
  SELECT 1 FROM sys_dict_type t
  WHERE t.tenant_id = 1 AND t.dict_type = 'ai_capability_user_mapping_mode'
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
  SELECT 1 tenant_id, 1 dict_sort, '管理员预绑定' dict_label, 'PREBOUND' dict_value,
         'ai_capability_user_mapping_mode' dict_type, 'success' list_class, 'Y' is_default,
         '管理员预先绑定外围 subject 与 Forge 普通用户，默认安全模式' remark
  UNION ALL
  SELECT 1, 2, '已验签手机号唯一匹配', 'VERIFIED_PHONE',
         'ai_capability_user_mapping_mode', 'warning', 'N',
         '客户端私钥验签成功后按 phone_number 在租户内唯一匹配并固化映射'
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM sys_dict_data d
  WHERE d.tenant_id = seed.tenant_id
    AND d.dict_type = seed.dict_type
    AND d.dict_value = seed.dict_value
);

SET @column_exists = (
  SELECT COUNT(1) FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'ai_capability_invocation_log'
    AND column_name = 'failure_stage'
);
SET @sql = IF(@column_exists = 0,
  'ALTER TABLE ai_capability_invocation_log ADD COLUMN failure_stage varchar(64) DEFAULT NULL COMMENT ''安全失败阶段'' AFTER error_code',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (
  SELECT COUNT(1) FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'ai_capability_invocation_log'
    AND column_name = 'error_message'
);
SET @sql = IF(@column_exists = 0,
  'ALTER TABLE ai_capability_invocation_log ADD COLUMN error_message varchar(1000) DEFAULT NULL COMMENT ''脱敏且截断的调用错误摘要'' AFTER failure_stage',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
