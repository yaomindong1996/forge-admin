-- AI 供应商增加显式连接适配器，并初始化管理端字典。
-- 历史记录统一保持 OpenAI Compatible，禁止迁移时自动切换为 DashScope Native。

SET @ai_provider_exists = (
  SELECT COUNT(1)
  FROM information_schema.tables
  WHERE table_schema = DATABASE()
    AND table_name = 'ai_provider'
);

SET @adapter_code_exists = (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'ai_provider'
    AND column_name = 'adapter_code'
);

SET @sql = IF(@ai_provider_exists > 0 AND @adapter_code_exists = 0,
  'ALTER TABLE ai_provider ADD COLUMN adapter_code varchar(32) NOT NULL DEFAULT ''openai_compatible'' COMMENT ''连接适配器（openai_compatible/dashscope_native）'' AFTER provider_type',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 兼容字段已由部分部署创建但仍存在空值的数据库；绝不覆盖已有 Native 记录。
SET @sql = IF(@ai_provider_exists > 0,
  'UPDATE ai_provider SET adapter_code = ''openai_compatible'' WHERE adapter_code IS NULL OR TRIM(adapter_code) = ''''',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

INSERT INTO sys_dict_type (
  tenant_id, dict_name, dict_type, dict_status, remark,
  create_by, create_time, update_by, update_time, create_dept
)
SELECT 1, 'AI供应商连接协议', 'ai_provider_adapter_type', 1, 'AI供应商底层连接协议',
       1, NOW(), 1, NOW(), 1
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_dict_type t
  WHERE t.tenant_id = 1
    AND t.dict_type = 'ai_provider_adapter_type'
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
  SELECT 1 tenant_id, 1 dict_sort, 'OpenAI Compatible' dict_label,
         'openai_compatible' dict_value, 'ai_provider_adapter_type' dict_type,
         'info' list_class, 'Y' is_default, 'OpenAI兼容协议' remark
  UNION ALL
  SELECT 1, 2, 'DashScope 原生', 'dashscope_native', 'ai_provider_adapter_type',
         'success', 'N', '阿里百炼DashScope原生协议'
) seed
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_dict_data d
  WHERE d.tenant_id = seed.tenant_id
    AND d.dict_type = seed.dict_type
    AND d.dict_value = seed.dict_value
);
