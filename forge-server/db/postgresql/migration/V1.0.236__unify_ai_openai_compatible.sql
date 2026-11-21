-- 社区版统一 AI 供应商连接协议为 OpenAI Compatible，平滑迁移存量 dashscope_native 记录并停用旧字典项。

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = current_schema()
          AND table_name = 'ai_provider'
    ) THEN
        UPDATE ai_provider
        SET base_url = 'https://dashscope.aliyuncs.com/compatible-mode',
            update_time = CURRENT_TIMESTAMP
        WHERE adapter_code = 'dashscope_native'
          AND (base_url IS NULL
               OR BTRIM(base_url) = ''
               OR RTRIM(base_url, '/') = 'https://dashscope.aliyuncs.com');

        UPDATE ai_provider
        SET adapter_code = 'openai_compatible',
            update_time = CURRENT_TIMESTAMP
        WHERE adapter_code = 'dashscope_native'
           OR adapter_code IS NULL
           OR BTRIM(adapter_code) = '';
    END IF;
END $$;

UPDATE sys_dict_data
SET dict_status = 0,
    remark = '已统一迁移至 OpenAI Compatible 协议',
    update_time = CURRENT_TIMESTAMP
WHERE dict_type = 'ai_provider_adapter_type'
  AND dict_value = 'dashscope_native'
  AND del_flag = 0;
