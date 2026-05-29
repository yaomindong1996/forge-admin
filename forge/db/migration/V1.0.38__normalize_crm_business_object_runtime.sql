-- 同步 CRM 业务对象的 model_id/model_code 与 ai_crud_config
-- 确保业务对象关联正确的低代码模型

-- 更新客户对象的 model_code
UPDATE `ai_business_object` 
SET `model_code` = 'crm_customer', `update_time` = NOW()
WHERE `tenant_id` = 1 
  AND `suite_code` = 'CRM' 
  AND `object_code` = 'customer' 
  AND (`model_code` IS NULL OR `model_code` = '');

-- 更新联系人对象的 model_code
UPDATE `ai_business_object` 
SET `model_code` = 'crm_contact', `update_time` = NOW()
WHERE `tenant_id` = 1 
  AND `suite_code` = 'CRM' 
  AND `object_code` = 'contact' 
  AND (`model_code` IS NULL OR `model_code` = '');

-- 更新商机对象的 model_code
UPDATE `ai_business_object` 
SET `model_code` = 'crm_opportunity', `update_time` = NOW()
WHERE `tenant_id` = 1 
  AND `suite_code` = 'CRM' 
  AND `object_code` = 'opportunity' 
  AND (`model_code` IS NULL OR `model_code` = '');

-- 更新合同对象的 model_code
UPDATE `ai_business_object` 
SET `model_code` = 'crm_contract', `update_time` = NOW()
WHERE `tenant_id` = 1 
  AND `suite_code` = 'CRM' 
  AND `object_code` = 'contract' 
  AND (`model_code` IS NULL OR `model_code` = '');

-- 更新回款对象的 model_code
UPDATE `ai_business_object` 
SET `model_code` = 'crm_payment', `update_time` = NOW()
WHERE `tenant_id` = 1 
  AND `suite_code` = 'CRM' 
  AND `object_code` = 'payment' 
  AND (`model_code` IS NULL OR `model_code` = '');

-- 更新应用入口的 config_key（如果为空）
UPDATE `ai_business_app` a
INNER JOIN `ai_crud_config` c ON c.`tenant_id` = a.`tenant_id` AND c.`object_code` = a.`object_code` AND c.`domain_code` = a.`suite_code`
SET a.`config_key` = c.`config_key`, a.`update_time` = NOW()
WHERE a.`tenant_id` = 1 
  AND a.`suite_code` = 'CRM' 
  AND a.`entry_mode` = 'RUNTIME'
  AND (a.`config_key` IS NULL OR a.`config_key` = '');
