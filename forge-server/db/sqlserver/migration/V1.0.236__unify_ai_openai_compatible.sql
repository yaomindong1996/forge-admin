-- 社区版统一 AI 供应商连接协议为 OpenAI Compatible（SQL Server，与 MySQL V1.0.236 同语义）
SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;
GO

IF OBJECT_ID(N'ai_provider', N'U') IS NOT NULL
BEGIN
    UPDATE ai_provider
    SET base_url = N'https://dashscope.aliyuncs.com/compatible-mode',
        update_time = SYSUTCDATETIME()
    WHERE adapter_code = N'dashscope_native'
      AND (
            base_url IS NULL
            OR LTRIM(RTRIM(base_url)) = N''
            OR CASE
                 WHEN RIGHT(base_url, 1) = N'/' THEN LEFT(base_url, LEN(base_url) - 1)
                 ELSE base_url
               END = N'https://dashscope.aliyuncs.com'
          );

    UPDATE ai_provider
    SET adapter_code = N'openai_compatible',
        update_time = SYSUTCDATETIME()
    WHERE adapter_code = N'dashscope_native'
       OR adapter_code IS NULL
       OR LTRIM(RTRIM(adapter_code)) = N'';
END
GO

UPDATE sys_dict_data
SET dict_status = 0,
    remark = N'已统一迁移至 OpenAI Compatible 协议',
    update_time = SYSUTCDATETIME()
WHERE dict_type = N'ai_provider_adapter_type'
  AND dict_value = N'dashscope_native'
  AND del_flag = 0;
GO
