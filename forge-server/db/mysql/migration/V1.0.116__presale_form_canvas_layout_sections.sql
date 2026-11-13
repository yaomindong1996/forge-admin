-- 预售单表单画布切换为「布局承载分区」结构：
-- 字段组件按存量分区归组进 card 容器（容器 id 即存量分区 sectionId，派生分区
-- 的 fieldOverrides/visibleInModes 等扩展配置按 sectionId 继承，流程权限不失配）；
-- 关联子表改由 subTable 容器承载（relationKey 锚定存量子表分区）；
-- 单号/导购userid/门店编码/状态等隐藏辅助字段保持顶层散落，派生为“基本信息”默认分区。
-- 只重写 formDesignerSchema.components，pageSections/bottomBar/settings/layout 等
-- 既有配置保持不动；UPDATE 幂等，可重复执行。

SET @ps_layout_components := JSON_ARRAY(
  JSON_OBJECT('id', 'cmp_presale_no', 'label', '预售单号', 'componentKey', 'input',
    'props', JSON_OBJECT('placeholder', '保存后自动生成'), 'layout', JSON_OBJECT('span', 1),
    'validation', JSON_OBJECT('required', false, 'rules', JSON_ARRAY()),
    'visibility', JSON_OBJECT('hidden', false, 'readonly', true),
    'fieldBinding', JSON_OBJECT('mode', 'field', 'source', 'field_asset', 'fieldCode', 'presaleNo', 'columnName', 'presale_no'),
    'children', JSON_ARRAY(), 'advancedProps', JSON_OBJECT()),
  JSON_OBJECT('id', 'cmp_sales_user_id', 'label', '导购userid', 'componentKey', 'input',
    'props', JSON_OBJECT('placeholder', '由当前企业微信用户自动识别'), 'layout', JSON_OBJECT('span', 1),
    'validation', JSON_OBJECT('required', false, 'requiredMessage', '', 'rules', JSON_ARRAY()),
    'visibility', JSON_OBJECT('hidden', true, 'readonly', true),
    'fieldBinding', JSON_OBJECT('mode', 'field', 'source', 'field_asset', 'fieldCode', 'salesUserId', 'columnName', 'sales_user_id'),
    'children', JSON_ARRAY(), 'advancedProps', JSON_OBJECT()),
  JSON_OBJECT('id', 'cmp_store_id', 'label', '门店编码', 'componentKey', 'input',
    'props', JSON_OBJECT('placeholder', '自动回填'), 'layout', JSON_OBJECT('span', 1),
    'validation', JSON_OBJECT('required', false, 'rules', JSON_ARRAY()),
    'visibility', JSON_OBJECT('hidden', false, 'readonly', true),
    'fieldBinding', JSON_OBJECT('mode', 'field', 'source', 'field_asset', 'fieldCode', 'storeId', 'columnName', 'store_id'),
    'children', JSON_ARRAY(), 'advancedProps', JSON_OBJECT()),
  JSON_OBJECT('id', 'cmp_status', 'label', '状态', 'componentKey', 'dictSelect',
    'props', JSON_OBJECT('dictType', 'ps_presale_status', 'placeholder', '状态由系统维护', 'defaultValue', 'DRAFT'), 'layout', JSON_OBJECT('span', 1),
    'validation', JSON_OBJECT('required', false, 'rules', JSON_ARRAY()),
    'visibility', JSON_OBJECT('hidden', true, 'readonly', true),
    'fieldBinding', JSON_OBJECT('mode', 'field', 'source', 'field_asset', 'fieldCode', 'status', 'columnName', 'status'),
    'children', JSON_ARRAY(), 'advancedProps', JSON_OBJECT()),
  JSON_OBJECT('id', 'guide_info', 'label', '导购信息', 'componentKey', 'card',
    'props', JSON_OBJECT('header', '导购信息'), 'layout', JSON_OBJECT('span', 2),
    'children', JSON_ARRAY(
      JSON_OBJECT('id', 'cmp_sales_user_name', 'label', '导购姓名', 'componentKey', 'input',
        'props', JSON_OBJECT('placeholder', '自动回填'), 'layout', JSON_OBJECT('span', 1),
        'validation', JSON_OBJECT('required', false, 'rules', JSON_ARRAY()),
        'visibility', JSON_OBJECT('hidden', false, 'readonly', true),
        'fieldBinding', JSON_OBJECT('mode', 'field', 'source', 'field_asset', 'fieldCode', 'salesUserName', 'columnName', 'sales_user_name'),
        'children', JSON_ARRAY(), 'advancedProps', JSON_OBJECT()),
      JSON_OBJECT('id', 'cmp_staff_no', 'label', '工号', 'componentKey', 'input',
        'props', JSON_OBJECT('placeholder', '自动回填'), 'layout', JSON_OBJECT('span', 1),
        'validation', JSON_OBJECT('required', false, 'rules', JSON_ARRAY()),
        'visibility', JSON_OBJECT('hidden', false, 'readonly', true),
        'fieldBinding', JSON_OBJECT('mode', 'field', 'source', 'field_asset', 'fieldCode', 'staffNo', 'columnName', 'staff_no'),
        'children', JSON_ARRAY(), 'advancedProps', JSON_OBJECT()),
      JSON_OBJECT('id', 'cmp_store_name', 'label', '门店名称', 'componentKey', 'input',
        'props', JSON_OBJECT('placeholder', '自动回填'), 'layout', JSON_OBJECT('span', 1),
        'validation', JSON_OBJECT('required', false, 'rules', JSON_ARRAY()),
        'visibility', JSON_OBJECT('hidden', false, 'readonly', true),
        'fieldBinding', JSON_OBJECT('mode', 'field', 'source', 'field_asset', 'fieldCode', 'storeName', 'columnName', 'store_name'),
        'children', JSON_ARRAY(), 'advancedProps', JSON_OBJECT())
    )),
  JSON_OBJECT('id', 'member_info', 'label', '会员信息', 'componentKey', 'card',
    'props', JSON_OBJECT('header', '会员信息'), 'layout', JSON_OBJECT('span', 2),
    'children', JSON_ARRAY(
      JSON_OBJECT('id', 'cmp_member_phone', 'label', '会员手机号', 'componentKey', 'input',
        'props', JSON_OBJECT('placeholder', '请输入11位手机号', 'maxlength', 11, 'clearable', true), 'layout', JSON_OBJECT('span', 1),
        'validation', JSON_OBJECT('required', true, 'requiredMessage', '请输入会员手机号',
          'rules', JSON_ARRAY(
            JSON_OBJECT('required', true, 'message', '请输入会员手机号', 'trigger', JSON_ARRAY('blur', 'change')),
            JSON_OBJECT('pattern', '^1[3-9][0-9]{9}$', 'message', '请输入正确的11位手机号', 'trigger', JSON_ARRAY('blur'))
          )),
        'visibility', JSON_OBJECT('hidden', false, 'readonly', false),
        'fieldBinding', JSON_OBJECT('mode', 'field', 'source', 'field_asset', 'fieldCode', 'memberPhone', 'columnName', 'member_phone'),
        'children', JSON_ARRAY(), 'advancedProps', JSON_OBJECT()),
      JSON_OBJECT('id', 'cmp_member_id', 'label', '会员ID', 'componentKey', 'input',
        'props', JSON_OBJECT('placeholder', '查询后自动回填'), 'layout', JSON_OBJECT('span', 1),
        'validation', JSON_OBJECT('required', false, 'rules', JSON_ARRAY()),
        'visibility', JSON_OBJECT('hidden', false, 'readonly', true),
        'fieldBinding', JSON_OBJECT('mode', 'field', 'source', 'field_asset', 'fieldCode', 'memberId', 'columnName', 'member_id'),
        'children', JSON_ARRAY(), 'advancedProps', JSON_OBJECT()),
      JSON_OBJECT('id', 'cmp_member_name', 'label', '会员姓名', 'componentKey', 'input',
        'props', JSON_OBJECT('placeholder', '查询后自动回填'), 'layout', JSON_OBJECT('span', 1),
        'validation', JSON_OBJECT('required', false, 'rules', JSON_ARRAY()),
        'visibility', JSON_OBJECT('hidden', false, 'readonly', true),
        'fieldBinding', JSON_OBJECT('mode', 'field', 'source', 'field_asset', 'fieldCode', 'memberName', 'columnName', 'member_name'),
        'children', JSON_ARRAY(), 'advancedProps', JSON_OBJECT())
    )),
  JSON_OBJECT('id', 'payment', 'label', '收款信息', 'componentKey', 'card',
    'props', JSON_OBJECT('header', '收款信息'), 'layout', JSON_OBJECT('span', 2),
    'children', JSON_ARRAY(
      JSON_OBJECT('id', 'cmp_pay_method', 'label', '收款方式', 'componentKey', 'dictSelect',
        'props', JSON_OBJECT('dictType', 'ps_presale_pay_method', 'placeholder', '请选择收款方式', 'clearable', false, 'defaultValue', 'STATIC_CODE',
          '__events', JSON_ARRAY(
            JSON_OBJECT('id', 'evt_show_static_payment_no', 'trigger', 'change', 'action', 'showHide', 'targetId', 'cmp_static_payment_no', 'whenValue', 'STATIC_CODE', 'value', 'true'),
            JSON_OBJECT('id', 'evt_show_static_payment_info', 'trigger', 'change', 'action', 'showHide', 'targetId', 'cmp_static_payment_info', 'whenValue', 'STATIC_CODE', 'value', 'true'),
            JSON_OBJECT('id', 'evt_show_cash_amount', 'trigger', 'change', 'action', 'showHide', 'targetId', 'cmp_cash_amount', 'whenValue', 'CASH', 'value', 'true')
          )),
        'layout', JSON_OBJECT('span', 1),
        'validation', JSON_OBJECT('required', true, 'requiredMessage', '请选择收款方式', 'rules', JSON_ARRAY(JSON_OBJECT('required', true, 'message', '请选择收款方式', 'trigger', JSON_ARRAY('change')))),
        'visibility', JSON_OBJECT('hidden', false, 'readonly', false),
        'fieldBinding', JSON_OBJECT('mode', 'field', 'source', 'field_asset', 'fieldCode', 'payMethod', 'columnName', 'pay_method'),
        'children', JSON_ARRAY(), 'advancedProps', JSON_OBJECT()),
      JSON_OBJECT('id', 'cmp_static_payment_no', 'label', '静态码单号', 'componentKey', 'input',
        'props', JSON_OBJECT('placeholder', '请输入静态码单号', 'clearable', true,
          'runtimeRules', JSON_ARRAY(JSON_OBJECT('id', 'show_static_payment_no', 'enabled', true, 'mode', 'all',
            'conditions', JSON_ARRAY(JSON_OBJECT('source', 'formData', 'field', 'payMethod', 'operator', 'eq', 'value', 'STATIC_CODE')),
            'effect', JSON_OBJECT('visible', true, 'whenUnmatched', 'hidden')))),
        'layout', JSON_OBJECT('span', 1),
        'validation', JSON_OBJECT('required', true, 'requiredMessage', '请输入静态码单号', 'rules', JSON_ARRAY(JSON_OBJECT('required', true, 'message', '请输入静态码单号', 'trigger', JSON_ARRAY('blur', 'change')))),
        'visibility', JSON_OBJECT('hidden', false, 'readonly', false),
        'fieldBinding', JSON_OBJECT('mode', 'field', 'source', 'field_asset', 'fieldCode', 'staticPaymentNo', 'columnName', 'static_payment_no'),
        'children', JSON_ARRAY(), 'advancedProps', JSON_OBJECT()),
      JSON_OBJECT('id', 'cmp_static_payment_info', 'label', '收款信息', 'componentKey', 'textarea',
        'props', JSON_OBJECT('placeholder', '查询后自动回填', 'rows', 2,
          'runtimeRules', JSON_ARRAY(JSON_OBJECT('id', 'show_static_payment_info', 'enabled', true, 'mode', 'all',
            'conditions', JSON_ARRAY(JSON_OBJECT('source', 'formData', 'field', 'payMethod', 'operator', 'eq', 'value', 'STATIC_CODE')),
            'effect', JSON_OBJECT('visible', true, 'whenUnmatched', 'hidden')))),
        'layout', JSON_OBJECT('span', 2), 'validation', JSON_OBJECT('required', false, 'rules', JSON_ARRAY()),
        'visibility', JSON_OBJECT('hidden', false, 'readonly', true),
        'fieldBinding', JSON_OBJECT('mode', 'field', 'source', 'field_asset', 'fieldCode', 'staticPaymentInfo', 'columnName', 'static_payment_info'),
        'children', JSON_ARRAY(), 'advancedProps', JSON_OBJECT()),
      JSON_OBJECT('id', 'cmp_cash_amount', 'label', '现金金额', 'componentKey', 'money',
        'props', JSON_OBJECT('placeholder', '请输入现金金额', 'min', 0.01, 'precision', 2, 'step', 0.01,
          'runtimeRules', JSON_ARRAY(JSON_OBJECT('id', 'show_cash_amount', 'enabled', true, 'mode', 'all',
            'conditions', JSON_ARRAY(JSON_OBJECT('source', 'formData', 'field', 'payMethod', 'operator', 'eq', 'value', 'CASH')),
            'effect', JSON_OBJECT('visible', true, 'whenUnmatched', 'hidden')))),
        'layout', JSON_OBJECT('span', 1),
        'validation', JSON_OBJECT('required', true, 'requiredMessage', '请输入现金金额', 'rules', JSON_ARRAY(JSON_OBJECT('required', true, 'message', '请输入现金金额', 'trigger', JSON_ARRAY('blur', 'change')))),
        'visibility', JSON_OBJECT('hidden', false, 'readonly', false),
        'fieldBinding', JSON_OBJECT('mode', 'field', 'source', 'field_asset', 'fieldCode', 'cashAmount', 'columnName', 'cash_amount'),
        'children', JSON_ARRAY(), 'advancedProps', JSON_OBJECT())
    )),
  JSON_OBJECT('id', 'remark', 'label', '备注', 'componentKey', 'card',
    'props', JSON_OBJECT('header', '备注'), 'layout', JSON_OBJECT('span', 2),
    'children', JSON_ARRAY(
      JSON_OBJECT('id', 'cmp_remark', 'label', '备注', 'componentKey', 'textarea',
        'props', JSON_OBJECT('placeholder', '请输入备注', 'rows', 3, 'maxlength', 500, 'showWordLimit', true),
        'layout', JSON_OBJECT('span', 2), 'validation', JSON_OBJECT('required', false, 'rules', JSON_ARRAY()),
        'visibility', JSON_OBJECT('hidden', false, 'readonly', false),
        'fieldBinding', JSON_OBJECT('mode', 'field', 'source', 'field_asset', 'fieldCode', 'remark', 'columnName', 'remark'),
        'children', JSON_ARRAY(), 'advancedProps', JSON_OBJECT())
    )),
  JSON_OBJECT('id', 'subtable_presale_items', 'label', '商品明细', 'componentKey', 'subTable',
    'props', JSON_OBJECT('sectionId', 'presale_items', 'header', '商品明细', 'relationKey', 'presale_items', 'displayMode', 'inline_grid'),
    'children', JSON_ARRAY()),
  JSON_OBJECT('id', 'subtable_pickup_return', 'label', '提货 / 退货', 'componentKey', 'subTable',
    'props', JSON_OBJECT('sectionId', 'pickup_return', 'header', '提货 / 退货', 'relationKey', 'presale_items', 'displayMode', 'inline_grid'),
    'children', JSON_ARRAY()),
  JSON_OBJECT('id', 'subtable_operation_logs', 'label', '操作日志', 'componentKey', 'subTable',
    'props', JSON_OBJECT('sectionId', 'operation_logs', 'header', '操作日志', 'relationKey', 'operation_logs', 'displayMode', 'bottom_sheet'),
    'children', JSON_ARRAY())
);

-- 1. 低代码 CRUD 当前配置
UPDATE ai_crud_config
SET options = JSON_SET(
      COALESCE(options, JSON_OBJECT()),
      '$.formDesignerSchema.components', JSON_EXTRACT(@ps_layout_components, '$')
    ),
    page_schema = JSON_SET(
      COALESCE(page_schema, JSON_OBJECT()),
      '$.zones[2].props.formDesignerSchema.components', JSON_EXTRACT(@ps_layout_components, '$')
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND config_key = 'ps_presale_order'
  AND del_flag = 0
  AND JSON_VALID(options)
  AND JSON_VALID(page_schema)
  AND JSON_TYPE(JSON_EXTRACT(options, '$.formDesignerSchema.components')) = 'ARRAY';

-- 2. 业务对象设计态配置
UPDATE ai_business_object
SET designer_options = JSON_SET(
      COALESCE(designer_options, JSON_OBJECT()),
      '$.formDesignerSchema.components', JSON_EXTRACT(@ps_layout_components, '$')
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND suite_code = 'PRESALE_REGISTRATION'
  AND object_code = 'PS_PRESALE_ORDER'
  AND del_flag = 0
  AND JSON_UNQUOTE(JSON_EXTRACT(designer_options, '$.seedKey')) = 'presale-registration-v1'
  AND JSON_VALID(designer_options)
  AND JSON_TYPE(JSON_EXTRACT(designer_options, '$.formDesignerSchema.components')) = 'ARRAY';

-- 3. 业务对象已发布设计版本快照
UPDATE ai_business_object_design_version
SET page_snapshot = JSON_SET(
      COALESCE(page_snapshot, JSON_OBJECT()),
      '$.zones[2].props.formDesignerSchema.components', JSON_EXTRACT(@ps_layout_components, '$')
    ),
    designer_options_snapshot = JSON_SET(
      COALESCE(designer_options_snapshot, JSON_OBJECT()),
      '$.formDesignerSchema.components', JSON_EXTRACT(@ps_layout_components, '$')
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND object_code = 'PS_PRESALE_ORDER'
  AND publish_status = 'PUBLISHED'
  AND JSON_VALID(page_snapshot)
  AND JSON_VALID(designer_options_snapshot)
  AND JSON_TYPE(JSON_EXTRACT(page_snapshot, '$.zones[2].props.formDesignerSchema.components')) = 'ARRAY';

-- 4. 低代码 CRUD 已发布版本快照
UPDATE ai_crud_config_version version_row
INNER JOIN ai_crud_config config_row
  ON config_row.tenant_id = version_row.tenant_id
 AND config_row.id = version_row.config_id
 AND config_row.config_key = 'ps_presale_order'
 AND config_row.del_flag = 0
SET version_row.options = JSON_SET(
      COALESCE(version_row.options, JSON_OBJECT()),
      '$.formDesignerSchema.components', JSON_EXTRACT(@ps_layout_components, '$')
    ),
    version_row.page_schema = JSON_SET(
      COALESCE(version_row.page_schema, JSON_OBJECT()),
      '$.zones[2].props.formDesignerSchema.components', JSON_EXTRACT(@ps_layout_components, '$')
    ),
    version_row.update_by = 1,
    version_row.update_time = NOW()
WHERE version_row.tenant_id = 1
  AND JSON_VALID(version_row.options)
  AND JSON_VALID(version_row.page_schema)
  AND JSON_TYPE(JSON_EXTRACT(version_row.options, '$.formDesignerSchema.components')) = 'ARRAY';
