-- 修正 CRM 样板运行配置的启停状态与导入导出开关。
-- ai_crud_config.status 约定为 0 启用、1 停用；V1.0.37 误写为 1 会导致动态 CRUD 打开失败。

UPDATE ai_crud_config
SET status = '0',
    publish_status = 'PUBLISHED',
    options = JSON_SET(
      COALESCE(NULLIF(options, ''), '{}'),
      '$.importEnabled', true,
      '$.exportEnabled', true,
      '$.detailEnabled', true,
      '$.showImport', true,
      '$.showExport', true
    ),
    update_time = NOW()
WHERE tenant_id = 1
  AND config_key IN ('crm_customer', 'crm_contact', 'crm_opportunity', 'crm_contract', 'crm_payment');
