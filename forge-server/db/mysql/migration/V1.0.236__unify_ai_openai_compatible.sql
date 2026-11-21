-- 社区版统一 AI 供应商连接协议为 OpenAI Compatible，平滑迁移存量 dashscope_native 记录并停用旧字典项。

SET @ai_provider_exists = (
  SELECT COUNT(1)
  FROM information_schema.tables
  WHERE table_schema = DATABASE()
    AND table_name = 'ai_provider'
);

SET @sql = IF(@ai_provider_exists > 0,
  'UPDATE ai_provider SET base_url = ''https://dashscope.aliyuncs.com/compatible-mode'', update_time = NOW() WHERE adapter_code = ''dashscope_native'' AND (base_url IS NULL OR TRIM(base_url) = '''' OR TRIM(TRAILING ''/'' FROM base_url) = ''https://dashscope.aliyuncs.com'')',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(@ai_provider_exists > 0,
  'UPDATE ai_provider SET adapter_code = ''openai_compatible'', update_time = NOW() WHERE adapter_code = ''dashscope_native'' OR adapter_code IS NULL OR TRIM(adapter_code) = ''''',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE sys_dict_data
SET dict_status = 0,
    remark = '已统一迁移至 OpenAI Compatible 协议',
    update_time = NOW()
WHERE dict_type = 'ai_provider_adapter_type'
  AND dict_value = 'dashscope_native'
  AND del_flag = 0;
