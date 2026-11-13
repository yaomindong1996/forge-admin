-- 外部接口增加受管 Mock 执行模式，并为预售登记提供可替换的默认 Mock 查询源。
-- Mock 只返回管理员配置的 JSON，不执行出站 HTTP、脚本或 SQL；真实联调时将 execution_mode 改为 HTTP 并配置真实连接。

SET @external_api_execution_mode_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_external_api'
    AND COLUMN_NAME = 'execution_mode'
);
SET @add_external_api_execution_mode_sql = IF(
  @external_api_execution_mode_exists = 0,
  'ALTER TABLE `sys_external_api` ADD COLUMN `execution_mode` varchar(16) NOT NULL DEFAULT ''HTTP'' COMMENT ''执行模式：HTTP/MOCK'' AFTER `api_desc`',
  'SELECT 1'
);
PREPARE add_external_api_execution_mode_stmt FROM @add_external_api_execution_mode_sql;
EXECUTE add_external_api_execution_mode_stmt;
DEALLOCATE PREPARE add_external_api_execution_mode_stmt;

SET @external_api_mock_response_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_external_api'
    AND COLUMN_NAME = 'mock_response_json'
);
SET @add_external_api_mock_response_sql = IF(
  @external_api_mock_response_exists = 0,
  'ALTER TABLE `sys_external_api` ADD COLUMN `mock_response_json` text NULL COMMENT ''Mock响应JSON，仅MOCK模式使用'' AFTER `response_total_path`',
  'SELECT 1'
);
PREPARE add_external_api_mock_response_stmt FROM @add_external_api_mock_response_sql;
EXECUTE add_external_api_mock_response_stmt;
DEALLOCATE PREPARE add_external_api_mock_response_stmt;

SET @external_api_execution_mode_index_exists = (
  SELECT COUNT(*)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_external_api'
    AND INDEX_NAME = 'idx_external_api_execution_mode'
);
SET @add_external_api_execution_mode_index_sql = IF(
  @external_api_execution_mode_index_exists = 0,
  'CREATE INDEX `idx_external_api_execution_mode` ON `sys_external_api` (`tenant_id`, `execution_mode`, `api_status`)',
  'SELECT 1'
);
PREPARE add_external_api_execution_mode_index_stmt FROM @add_external_api_execution_mode_index_sql;
EXECUTE add_external_api_execution_mode_index_stmt;
DEALLOCATE PREPARE add_external_api_execution_mode_index_stmt;

INSERT INTO sys_dict_type (
  tenant_id, dict_name, dict_type, dict_status, remark,
  create_by, create_time, create_dept, update_by, update_time
)
SELECT 1, '外部接口执行模式', 'external_api_execution_mode', 1, '外部接口HTTP或Mock执行模式',
       1, NOW(), 1, 1, NOW()
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_dict_type existing
  WHERE existing.tenant_id = 1
    AND existing.dict_type = 'external_api_execution_mode'
    AND existing.del_flag = 0
);

INSERT INTO sys_dict_data (
  tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class,
  is_default, dict_status, remark, create_by, create_time, create_dept, update_by, update_time
)
SELECT seed.tenant_id, seed.dict_sort, seed.dict_label, seed.dict_value, seed.dict_type,
       NULL, seed.list_class, seed.is_default, 1, seed.remark, 1, NOW(), 1, 1, NOW()
FROM (
  SELECT 1 tenant_id, 1 dict_sort, 'HTTP接口' dict_label, 'HTTP' dict_value,
         'external_api_execution_mode' dict_type, 'info' list_class, 'Y' is_default,
         '通过受控出站客户端调用真实外部接口' remark
  UNION ALL
  SELECT 1, 2, 'Mock数据', 'MOCK', 'external_api_execution_mode', 'warning', 'N',
         '直接返回接口配置中的Mock响应JSON，不执行出站调用'
) seed
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_dict_data existing
  WHERE existing.tenant_id = seed.tenant_id
    AND existing.dict_type = seed.dict_type
    AND existing.dict_value = seed.dict_value
    AND existing.del_flag = 0
);

INSERT INTO sys_external_system (
  id, tenant_id, system_code, system_name, system_desc, base_url, auth_type,
  trusted_internal, proxy_enabled, retry_enabled, retry_max_attempts, retry_backoff_interval,
  connect_timeout, read_timeout, write_timeout, ssl_verify_enabled, request_logging_enabled,
  system_status, remark, create_by, create_time, create_dept, update_by, update_time
)
SELECT seed.id, 1, seed.system_code, seed.system_name, seed.system_desc, 'mock-local', 'none',
       0, 0, 0, NULL, NULL, 5000, 10000, 10000, 1, 1,
       1, '预售登记默认Mock系统；真实联调时可替换为HTTP连接', 1, NOW(), 1, 1, NOW()
FROM (
  SELECT 1981000000000001101 id, 'wecom' system_code, '企业微信Mock' system_name, '企业微信用户、工号和门店映射Mock系统' system_desc
  UNION ALL SELECT 1981000000000001102, 'member', '会员Mock', '手机号查询会员Mock系统'
  UNION ALL SELECT 1981000000000001103, 'product', '商品Mock', '条码查询商品Mock系统'
  UNION ALL SELECT 1981000000000001104, 'payment', '收款Mock', '静态码收款查询Mock系统'
) seed
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_external_system existing
  WHERE existing.tenant_id = 1
    AND existing.system_code = seed.system_code
);

INSERT INTO sys_external_api (
  id, tenant_id, system_id, api_code, api_name, api_desc, execution_mode,
  api_path, api_method, request_content_type, request_headers, request_params,
  request_body_template, response_content_type, response_data_path, response_total_path,
  mock_response_json, param_mapping_enabled, param_mappings, response_transform_enabled,
  response_transform_script, error_code_path, error_msg_path, success_codes, doc_file_id,
  doc_file_name, rate_limit_enabled, rate_limit_qps, cache_enabled, cache_ttl,
  cache_key_template, permission_check_enabled, required_permission, lowcode_query_enabled,
  input_schema_json, output_schema_json, api_status, sort_order, remark,
  create_by, create_time, create_dept, update_by, update_time
)
SELECT seed.id, 1, system_row.id, seed.api_code, seed.api_name, seed.api_desc, 'MOCK',
       '/mock/' AS api_path, 'POST', 'application/json', '{}', '{}',
       NULL, 'application/json', NULL, NULL,
       seed.mock_response_json, 0, NULL, 0, NULL, NULL, NULL, '0,200', NULL, NULL,
       0, NULL, 0, NULL, NULL, 1, 'ai:businessApp:open', 1,
       seed.input_schema_json, seed.output_schema_json, 1, seed.sort_order,
       '预售登记默认Mock查询源；真实联调时将执行模式切换为HTTP并配置接口',
       1, NOW(), 1, 1, NOW()
FROM (
  SELECT 1982000000000001101 id, 'wecom' system_code, 'user-store' api_code,
         '企微用户与工号门店映射' api_name, '根据当前企业微信userid返回导购和门店' api_desc,
         10 sort_order,
         JSON_ARRAY(JSON_OBJECT('name', 'userid', 'label', '企微userid', 'type', 'string', 'required', false, 'maxLength', 128)) input_schema_json,
         JSON_ARRAY(
           JSON_OBJECT('name', 'userid', 'path', 'userid', 'label', '企微userid', 'type', 'string'),
           JSON_OBJECT('name', 'userName', 'path', 'userName', 'label', '导购姓名', 'type', 'string'),
           JSON_OBJECT('name', 'staffNo', 'path', 'staffNo', 'label', '工号', 'type', 'string'),
           JSON_OBJECT('name', 'storeId', 'path', 'storeId', 'label', '门店ID', 'type', 'string'),
           JSON_OBJECT('name', 'storeName', 'path', 'storeName', 'label', '门店名称', 'type', 'string')
         ) output_schema_json,
         JSON_OBJECT('userid', 'mock-wecom-user', 'userName', '测试导购', 'staffNo', 'S0001', 'storeId', 'STORE001', 'storeName', '测试门店') mock_response_json
  UNION ALL
  SELECT 1982000000000001102, 'member', 'member-by-mobile',
         '手机号查询会员', '根据手机号返回会员ID和姓名', 20,
         JSON_ARRAY(JSON_OBJECT('name', 'mobile', 'label', '手机号', 'type', 'string', 'required', true, 'maxLength', 32)),
         JSON_ARRAY(
           JSON_OBJECT('name', 'memberId', 'path', 'memberId', 'label', '会员ID', 'type', 'string'),
           JSON_OBJECT('name', 'memberName', 'path', 'memberName', 'label', '会员姓名', 'type', 'string')
         ),
         JSON_OBJECT('memberId', 'M000001', 'memberName', '测试会员')
  UNION ALL
  SELECT 1982000000000001103, 'product', 'product-by-barcode',
         '条码查询商品', '根据商品条码返回商品名称', 30,
         JSON_ARRAY(JSON_OBJECT('name', 'barcode', 'label', '商品条码', 'type', 'string', 'required', true, 'maxLength', 128)),
         JSON_ARRAY(JSON_OBJECT('name', 'productName', 'path', 'productName', 'label', '商品名称', 'type', 'string')),
         JSON_OBJECT('productName', '测试商品')
  UNION ALL
  SELECT 1982000000000001104, 'payment', 'static-code',
         '静态码单号查询收款', '根据静态码收款单号返回收款摘要', 40,
         JSON_ARRAY(JSON_OBJECT('name', 'paymentNo', 'label', '收款单号', 'type', 'string', 'required', true, 'maxLength', 64)),
         JSON_ARRAY(JSON_OBJECT('name', 'summary', 'path', 'summary', 'label', '收款摘要', 'type', 'string')),
         JSON_OBJECT('summary', 'Mock收款成功，金额以实际单据为准')
) seed
INNER JOIN sys_external_system system_row
        ON system_row.tenant_id = 1
       AND system_row.system_code = seed.system_code
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_external_api existing
  WHERE existing.tenant_id = 1
    AND existing.system_id = system_row.id
    AND existing.api_code = seed.api_code
);

UPDATE ai_business_binding binding_row
INNER JOIN (
  SELECT 'wecom/user-store' source_key
  UNION ALL SELECT 'member/member-by-mobile'
  UNION ALL SELECT 'product/product-by-barcode'
  UNION ALL SELECT 'payment/static-code'
) seed
   ON seed.source_key = binding_row.binding_key
SET binding_row.status = 1,
    binding_row.description = '已配置默认Mock查询源；真实联调时在外围接口管理切换为HTTP连接',
    binding_row.update_by = 1,
    binding_row.update_time = NOW()
WHERE binding_row.tenant_id = 1
  AND binding_row.target_type = 'APPLICATION'
  AND binding_row.target_code = 'PRESALE_REGISTRATION_APP'
  AND binding_row.binding_type = 'INTEGRATION';
