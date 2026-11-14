-- V1.0.113 将"登记提货"和"登记退货"按钮从"商品明细"tab移到"操作日志"tab
-- 方案：从 presale_items child 的 rowActions 移到 operation_logs child 的 toolbarActions
-- toolbarActions 在子表工具栏显示，即使操作日志tab为空也能看到按钮
-- 动作配置（designer_options）保持不变：actionPosition=CHILD_ROW, relationKey=presale_items
-- 前端通过 handleChildToolbarAction 弹出商品选择器，选定商品行后触发原有 CHILD_ROW 动作

-- 1. 更新 options 列：将 masterDetailConfig.children[0].rowActions 移到 children[1].toolbarActions
--    注意：masterDetailConfig 存储在 options 列，不是 page_schema 列
UPDATE ai_crud_config
SET options = JSON_SET(
      JSON_SET(
        options,
        '$.masterDetailConfig.children[1].toolbarActions',
        JSON_EXTRACT(options, '$.masterDetailConfig.children[0].rowActions')
      ),
      '$.masterDetailConfig.children[0].rowActions', JSON_ARRAY()
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND config_key = 'ps_presale_order'
  AND JSON_LENGTH(JSON_EXTRACT(options, '$.masterDetailConfig.children[0].rowActions')) > 0;

-- 2. 同步更新 page_schema 列：将 modelRefs[1].props.rowActions 移到 modelRefs[2].props.toolbarActions
UPDATE ai_crud_config
SET page_schema = JSON_SET(
      JSON_SET(
        page_schema,
        '$.modelRefs[2].props.toolbarActions',
        JSON_EXTRACT(page_schema, '$.modelRefs[1].props.rowActions')
      ),
      '$.modelRefs[1].props.rowActions', JSON_ARRAY()
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND config_key = 'ps_presale_order'
  AND JSON_LENGTH(JSON_EXTRACT(page_schema, '$.modelRefs[1].props.rowActions')) > 0;

-- 2b. 同步更新已发布版本快照（ai_crud_config_version）：后端 PUBLISHED 配置使用版本快照数据
--     options 列：将 children[0].rowActions 移到 children[1].toolbarActions
UPDATE ai_crud_config_version v
JOIN ai_crud_config c ON v.config_id = c.id AND v.version_no = c.published_version
SET v.options = JSON_SET(
      JSON_SET(
        v.options,
        '$.masterDetailConfig.children[1].toolbarActions',
        JSON_EXTRACT(v.options, '$.masterDetailConfig.children[0].rowActions')
      ),
      '$.masterDetailConfig.children[0].rowActions', JSON_ARRAY()
    )
WHERE c.tenant_id = 1
  AND c.config_key = 'ps_presale_order'
  AND JSON_LENGTH(JSON_EXTRACT(v.options, '$.masterDetailConfig.children[0].rowActions')) > 0;

-- 2c. 版本快照 page_schema 列：将 modelRefs[1].props.rowActions 移到 modelRefs[2].props.toolbarActions
UPDATE ai_crud_config_version v
JOIN ai_crud_config c ON v.config_id = c.id AND v.version_no = c.published_version
SET v.page_schema = JSON_SET(
      JSON_SET(
        v.page_schema,
        '$.modelRefs[2].props.toolbarActions',
        JSON_EXTRACT(v.page_schema, '$.modelRefs[1].props.rowActions')
      ),
      '$.modelRefs[1].props.rowActions', JSON_ARRAY()
    )
WHERE c.tenant_id = 1
  AND c.config_key = 'ps_presale_order'
  AND JSON_LENGTH(JSON_EXTRACT(v.page_schema, '$.modelRefs[1].props.rowActions')) > 0;

-- 3. 确保 designer_options 中 record_pickup 动作配置正确
--    relationKey 必须为 presale_items（操作目标是商品明细行，不是操作日志行）
--    步骤引用：parentRecord.id（父记录）、record.id（商品明细行）
UPDATE ai_business_object
SET designer_options = JSON_SET(
      designer_options,
      '$.actions[1].actionConfig.relationKey', 'presale_items',
      '$.actions[1].actionConfig.steps[0].stepConfig.targetRecordIdField', 'parentRecord.id',
      '$.actions[1].actionConfig.steps[1].stepConfig.targetRecordIdField', 'record.id',
      '$.actions[1].actionConfig.steps[1].stepConfig.expectedFieldMappings',
        JSON_ARRAY(JSON_OBJECT('targetField', 'presaleOrderId', 'sourceType', 'parent', 'sourceField', 'id')),
      '$.actions[1].actionConfig.steps[2].stepConfig.fieldMappings',
        JSON_ARRAY(
          JSON_OBJECT('targetField', 'presaleOrderId', 'sourceType', 'parent', 'sourceField', 'id'),
          JSON_OBJECT('targetField', 'productItemId', 'sourceType', 'record', 'sourceField', 'id'),
          JSON_OBJECT('targetField', 'productName', 'sourceType', 'record', 'sourceField', 'productName'),
          JSON_OBJECT('targetField', 'quantity', 'sourceType', 'form', 'sourceField', 'quantity'),
          JSON_OBJECT('targetField', 'operatorName', 'sourceType', 'system', 'sourceField', 'realName')
        ),
      '$.actions[1].actionConfig.steps[3].stepConfig.targetRecordIdField', 'record.id',
      '$.actions[1].actionConfig.steps[3].stepConfig.expectedFieldMappings',
        JSON_ARRAY(JSON_OBJECT('targetField', 'presaleOrderId', 'sourceType', 'parent', 'sourceField', 'id'))
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND suite_code = 'PRESALE_REGISTRATION'
  AND object_code = 'PS_PRESALE_ORDER';

-- 4. 确保 designer_options 中 record_return 动作配置正确（同上逻辑）
UPDATE ai_business_object
SET designer_options = JSON_SET(
      designer_options,
      '$.actions[2].actionConfig.relationKey', 'presale_items',
      '$.actions[2].actionConfig.steps[0].stepConfig.targetRecordIdField', 'parentRecord.id',
      '$.actions[2].actionConfig.steps[1].stepConfig.targetRecordIdField', 'record.id',
      '$.actions[2].actionConfig.steps[1].stepConfig.expectedFieldMappings',
        JSON_ARRAY(JSON_OBJECT('targetField', 'presaleOrderId', 'sourceType', 'parent', 'sourceField', 'id')),
      '$.actions[2].actionConfig.steps[2].stepConfig.fieldMappings',
        JSON_ARRAY(
          JSON_OBJECT('targetField', 'presaleOrderId', 'sourceType', 'parent', 'sourceField', 'id'),
          JSON_OBJECT('targetField', 'productItemId', 'sourceType', 'record', 'sourceField', 'id'),
          JSON_OBJECT('targetField', 'productName', 'sourceType', 'record', 'sourceField', 'productName'),
          JSON_OBJECT('targetField', 'quantity', 'sourceType', 'form', 'sourceField', 'quantity'),
          JSON_OBJECT('targetField', 'operatorName', 'sourceType', 'system', 'sourceField', 'realName')
        ),
      '$.actions[2].actionConfig.steps[3].stepConfig.targetRecordIdField', 'record.id',
      '$.actions[2].actionConfig.steps[3].stepConfig.expectedFieldMappings',
        JSON_ARRAY(JSON_OBJECT('targetField', 'presaleOrderId', 'sourceType', 'parent', 'sourceField', 'id'))
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND suite_code = 'PRESALE_REGISTRATION'
  AND object_code = 'PS_PRESALE_ORDER';
