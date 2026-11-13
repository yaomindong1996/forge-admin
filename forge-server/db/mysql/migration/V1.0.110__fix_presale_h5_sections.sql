-- 预售登记 H5 页面区域修正：
-- 1. 商品明细(presale_items) 仅在 create 模式显示（用于扫码添加商品）
-- 2. 原"操作日志"区域改为"提货 / 退货"，relationKey 改为 presale_items，
--    在 edit/detail 模式显示商品行 + 提货/退货行操作按钮
-- 3. 原操作日志(relationKey=operation_logs) 作为只读历史合并到底部抽屉

SET @ps_fixed_page_sections := JSON_ARRAY(
  JSON_OBJECT(
    'sectionId', 'guide_info',
    'sectionType', 'card',
    'title', '导购信息',
    'collapsible', false,
    'fields', JSON_ARRAY('salesUserName', 'staffNo', 'storeName'),
    'visibleInModes', JSON_ARRAY('create', 'edit', 'detail')
  ),
  JSON_OBJECT(
    'sectionId', 'member_info',
    'sectionType', 'card',
    'title', '会员信息',
    'collapsible', false,
    'fields', JSON_ARRAY('memberPhone', 'memberId', 'memberName'),
    'visibleInModes', JSON_ARRAY('create', 'edit', 'detail')
  ),
  JSON_OBJECT(
    'sectionId', 'payment',
    'sectionType', 'card',
    'title', '收款信息',
    'collapsible', false,
    'fields', JSON_ARRAY('payMethod', 'staticPaymentNo', 'staticPaymentInfo', 'cashAmount'),
    'fieldOverrides', JSON_OBJECT(
      'payMethod', JSON_OBJECT(
        'componentKey', 'pillSelect',
        'props', JSON_OBJECT('clearable', false)
      )
    ),
    'visibleInModes', JSON_ARRAY('create', 'edit', 'detail')
  ),
  JSON_OBJECT(
    'sectionId', 'remark',
    'sectionType', 'card',
    'title', '备注',
    'collapsible', false,
    'fields', JSON_ARRAY('remark'),
    'visibleInModes', JSON_ARRAY('create', 'edit', 'detail')
  ),
  JSON_OBJECT(
    'sectionId', 'presale_items',
    'sectionType', 'child_table',
    'relationKey', 'presale_items',
    'title', '商品明细',
    'displayMode', 'inline_grid',
    'visibleInModes', JSON_ARRAY('create')
  ),
  JSON_OBJECT(
    'sectionId', 'pickup_return',
    'sectionType', 'child_table',
    'relationKey', 'presale_items',
    'title', '提货 / 退货',
    'displayMode', 'inline_grid',
    'visibleInModes', JSON_ARRAY('edit', 'detail')
  ),
  JSON_OBJECT(
    'sectionId', 'operation_logs',
    'sectionType', 'child_table',
    'relationKey', 'operation_logs',
    'title', '操作日志',
    'displayMode', 'bottom_sheet',
    'visibleInModes', JSON_ARRAY('edit', 'detail')
  )
);

SET @ps_fixed_bottom_bar := JSON_OBJECT(
  'actions', JSON_ARRAY(
    JSON_OBJECT(
      'type', 'reset',
      'label', '清空',
      'variant', 'secondary'
    ),
    JSON_OBJECT(
      'type', 'action',
      'actionCode', 'submit_presale',
      'label', '提交',
      'variant', 'primary',
      'displayCondition', 'status == DRAFT',
      'confirmText', '确认提交当前预售单？',
      'successMessage', '预售单已提交'
    )
  )
);

-- 更新运行时配置
UPDATE ai_crud_config
SET options = JSON_SET(
      COALESCE(options, JSON_OBJECT()),
      '$.formDesignerSchema.pageSections', JSON_EXTRACT(@ps_fixed_page_sections, '$'),
      '$.formDesignerSchema.bottomBar', JSON_EXTRACT(@ps_fixed_bottom_bar, '$')
    ),
    page_schema = JSON_SET(
      COALESCE(page_schema, JSON_OBJECT()),
      '$.zones[2].props.formDesignerSchema.pageSections', JSON_EXTRACT(@ps_fixed_page_sections, '$'),
      '$.zones[2].props.formDesignerSchema.bottomBar', JSON_EXTRACT(@ps_fixed_bottom_bar, '$')
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND config_key = 'ps_presale_order'
  AND del_flag = 0
  AND publish_status = 'PUBLISHED'
  AND JSON_VALID(options)
  AND JSON_VALID(page_schema);

-- 更新业务对象设计态
UPDATE ai_business_object
SET designer_options = JSON_SET(
      COALESCE(designer_options, JSON_OBJECT()),
      '$.formDesignerSchema.pageSections', JSON_EXTRACT(@ps_fixed_page_sections, '$'),
      '$.formDesignerSchema.bottomBar', JSON_EXTRACT(@ps_fixed_bottom_bar, '$')
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND suite_code = 'PRESALE_REGISTRATION'
  AND object_code = 'PS_PRESALE_ORDER'
  AND del_flag = 0
  AND JSON_UNQUOTE(JSON_EXTRACT(designer_options, '$.seedKey')) = 'presale-registration-v1'
  AND JSON_VALID(designer_options);

-- 更新发布快照
UPDATE ai_business_object_design_version
SET page_snapshot = JSON_SET(
      COALESCE(page_snapshot, JSON_OBJECT()),
      '$.zones[2].props.formDesignerSchema.pageSections', JSON_EXTRACT(@ps_fixed_page_sections, '$'),
      '$.zones[2].props.formDesignerSchema.bottomBar', JSON_EXTRACT(@ps_fixed_bottom_bar, '$')
    ),
    designer_options_snapshot = JSON_SET(
      COALESCE(designer_options_snapshot, JSON_OBJECT()),
      '$.formDesignerSchema.pageSections', JSON_EXTRACT(@ps_fixed_page_sections, '$'),
      '$.formDesignerSchema.bottomBar', JSON_EXTRACT(@ps_fixed_bottom_bar, '$')
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND object_code = 'PS_PRESALE_ORDER'
  AND publish_status = 'PUBLISHED'
  AND JSON_VALID(page_snapshot)
  AND JSON_VALID(designer_options_snapshot);

-- 更新配置版本表
UPDATE ai_crud_config_version version_row
INNER JOIN ai_crud_config config_row
  ON config_row.tenant_id = version_row.tenant_id
 AND config_row.id = version_row.config_id
 AND config_row.config_key = 'ps_presale_order'
 AND config_row.del_flag = 0
SET version_row.options = JSON_SET(
      COALESCE(version_row.options, JSON_OBJECT()),
      '$.formDesignerSchema.pageSections', JSON_EXTRACT(@ps_fixed_page_sections, '$'),
      '$.formDesignerSchema.bottomBar', JSON_EXTRACT(@ps_fixed_bottom_bar, '$')
    ),
    version_row.page_schema = JSON_SET(
      COALESCE(version_row.page_schema, JSON_OBJECT()),
      '$.zones[2].props.formDesignerSchema.pageSections', JSON_EXTRACT(@ps_fixed_page_sections, '$'),
      '$.zones[2].props.formDesignerSchema.bottomBar', JSON_EXTRACT(@ps_fixed_bottom_bar, '$')
    ),
    version_row.update_by = 1,
    version_row.update_time = NOW()
WHERE version_row.tenant_id = 1
  AND JSON_VALID(version_row.options)
  AND JSON_VALID(version_row.page_schema);
