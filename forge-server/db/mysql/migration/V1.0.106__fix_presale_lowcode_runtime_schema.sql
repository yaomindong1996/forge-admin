-- 修复预售登记初始低代码协议：MOBILE 是入口类型，不是模型 appType；主模型 children 必须是模型对象数组。
-- 105 已在部分环境执行，使用独立版本修复，避免修改已执行迁移造成 checksum mismatch。

UPDATE ai_lowcode_domain
SET default_app_type = 'MASTER_DETAIL',
    domain_schema = JSON_SET(domain_schema, '$.defaults.appType', 'MASTER_DETAIL'),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND domain_code = 'PRESALE_REGISTRATION'
  AND del_flag = 0
  AND JSON_VALID(domain_schema)
  AND (
      default_app_type <> 'MASTER_DETAIL'
      OR JSON_UNQUOTE(JSON_EXTRACT(domain_schema, '$.defaults.appType')) <> 'MASTER_DETAIL'
  );

UPDATE ai_lowcode_model
SET model_schema = JSON_SET(
        model_schema,
        '$.appType', 'MASTER_DETAIL',
        '$.children', JSON_ARRAY()
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND domain_code = 'PRESALE_REGISTRATION'
  AND model_code = 'ps_presale_order'
  AND del_flag = 0
  AND JSON_VALID(model_schema)
  AND (
      JSON_UNQUOTE(JSON_EXTRACT(model_schema, '$.appType')) <> 'MASTER_DETAIL'
      OR JSON_TYPE(JSON_EXTRACT(model_schema, '$.children')) <> 'ARRAY'
      OR JSON_LENGTH(JSON_EXTRACT(model_schema, '$.children')) <> 0
  );

UPDATE ai_lowcode_model
SET model_schema = JSON_SET(model_schema, '$.appType', 'SINGLE'),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND domain_code = 'PRESALE_REGISTRATION'
  AND model_code IN ('ps_presale_order_item', 'ps_presale_operation_log')
  AND del_flag = 0
  AND JSON_VALID(model_schema)
  AND JSON_UNQUOTE(JSON_EXTRACT(model_schema, '$.appType')) <> 'SINGLE';

UPDATE ai_lowcode_model
SET model_schema = JSON_SET(
        model_schema,
        '$.fields[2].systemField', false,
        '$.fields[2].formVisible', false
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND domain_code = 'PRESALE_REGISTRATION'
  AND model_code IN ('ps_presale_order_item', 'ps_presale_operation_log')
  AND del_flag = 0
  AND JSON_VALID(model_schema)
  AND JSON_UNQUOTE(JSON_EXTRACT(model_schema, '$.fields[2].field')) = 'presaleOrderId';

UPDATE ai_crud_config
SET model_schema = JSON_SET(
        model_schema,
        '$.appType', 'MASTER_DETAIL',
        '$.children', JSON_ARRAY()
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND config_key = 'ps_presale_order'
  AND del_flag = 0
  AND JSON_VALID(model_schema);

UPDATE ai_crud_config
SET model_schema = JSON_SET(model_schema, '$.appType', 'SINGLE'),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND config_key IN ('ps_presale_order_item', 'ps_presale_operation_log')
  AND del_flag = 0
  AND JSON_VALID(model_schema);

UPDATE ai_crud_config
SET model_schema = JSON_SET(
        model_schema,
        '$.fields[2].systemField', false,
        '$.fields[2].formVisible', false
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND config_key IN ('ps_presale_order_item', 'ps_presale_operation_log')
  AND del_flag = 0
  AND JSON_VALID(model_schema)
  AND JSON_UNQUOTE(JSON_EXTRACT(model_schema, '$.fields[2].field')) = 'presaleOrderId';

UPDATE ai_business_object_design_version
SET model_snapshot = JSON_SET(
        model_snapshot,
        '$.appType', 'MASTER_DETAIL',
        '$.children', JSON_ARRAY()
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND object_code = 'PS_PRESALE_ORDER'
  AND publish_status = 'PUBLISHED'
  AND JSON_VALID(model_snapshot);

UPDATE ai_business_object_design_version
SET model_snapshot = JSON_SET(model_snapshot, '$.appType', 'SINGLE'),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND object_code IN ('PS_PRESALE_ORDER_ITEM', 'PS_PRESALE_OPERATION_LOG')
  AND publish_status = 'PUBLISHED'
  AND JSON_VALID(model_snapshot);

UPDATE ai_business_object_design_version
SET model_snapshot = JSON_SET(
        model_snapshot,
        '$.fields[2].systemField', false,
        '$.fields[2].formVisible', false
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND object_code IN ('PS_PRESALE_ORDER_ITEM', 'PS_PRESALE_OPERATION_LOG')
  AND publish_status = 'PUBLISHED'
  AND JSON_VALID(model_snapshot)
  AND JSON_UNQUOTE(JSON_EXTRACT(model_snapshot, '$.fields[2].field')) = 'presaleOrderId';

UPDATE ai_crud_config_version version_row
INNER JOIN ai_crud_config config_row
  ON config_row.tenant_id = version_row.tenant_id
 AND config_row.id = version_row.config_id
 AND config_row.config_key = 'ps_presale_order'
 AND config_row.del_flag = 0
SET version_row.model_schema = JSON_SET(
        version_row.model_schema,
        '$.appType', 'MASTER_DETAIL',
        '$.children', JSON_ARRAY()
    ),
    version_row.update_by = 1,
    version_row.update_time = NOW()
WHERE version_row.tenant_id = 1
  AND JSON_VALID(version_row.model_schema);

UPDATE ai_crud_config_version version_row
INNER JOIN ai_crud_config config_row
  ON config_row.tenant_id = version_row.tenant_id
 AND config_row.id = version_row.config_id
 AND config_row.config_key IN ('ps_presale_order_item', 'ps_presale_operation_log')
 AND config_row.del_flag = 0
SET version_row.model_schema = JSON_SET(
        version_row.model_schema,
        '$.fields[2].systemField', false,
        '$.fields[2].formVisible', false
    ),
    version_row.update_by = 1,
    version_row.update_time = NOW()
WHERE version_row.tenant_id = 1
  AND JSON_VALID(version_row.model_schema)
  AND JSON_UNQUOTE(JSON_EXTRACT(version_row.model_schema, '$.fields[2].field')) = 'presaleOrderId';

UPDATE ai_crud_config_version version_row
INNER JOIN ai_crud_config config_row
  ON config_row.tenant_id = version_row.tenant_id
 AND config_row.id = version_row.config_id
 AND config_row.config_key IN ('ps_presale_order_item', 'ps_presale_operation_log')
 AND config_row.del_flag = 0
SET version_row.model_schema = JSON_SET(version_row.model_schema, '$.appType', 'SINGLE'),
    version_row.update_by = 1,
    version_row.update_time = NOW()
WHERE version_row.tenant_id = 1
  AND JSON_VALID(version_row.model_schema);
