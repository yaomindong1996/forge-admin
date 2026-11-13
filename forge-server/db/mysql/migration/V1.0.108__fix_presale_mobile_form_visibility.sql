-- 修复预售登记移动端表单展示和设计器交互元数据：
-- 1. 导购 userid、状态为系统/运行时维护字段，表单填写不展示；
-- 2. 静态码单号、现金金额不做静态隐藏，统一由收款方式 runtimeRules 控制；
-- 3. 收款方式补充兼容 __events，便于设计器点开来源字段时看到它影响的目标字段；
-- 4. 同步当前配置、业务对象设计和已发布快照，避免不同入口继续读取旧元数据。

SET @ps_form_patch_components := JSON_ARRAY(
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
  JSON_OBJECT('id', 'cmp_store_id', 'label', '门店编码', 'componentKey', 'input',
    'props', JSON_OBJECT('placeholder', '自动回填'), 'layout', JSON_OBJECT('span', 1),
    'validation', JSON_OBJECT('required', false, 'rules', JSON_ARRAY()),
    'visibility', JSON_OBJECT('hidden', false, 'readonly', true),
    'fieldBinding', JSON_OBJECT('mode', 'field', 'source', 'field_asset', 'fieldCode', 'storeId', 'columnName', 'store_id'),
    'children', JSON_ARRAY(), 'advancedProps', JSON_OBJECT()),
  JSON_OBJECT('id', 'cmp_store_name', 'label', '门店名称', 'componentKey', 'input',
    'props', JSON_OBJECT('placeholder', '自动回填'), 'layout', JSON_OBJECT('span', 1),
    'validation', JSON_OBJECT('required', false, 'rules', JSON_ARRAY()),
    'visibility', JSON_OBJECT('hidden', false, 'readonly', true),
    'fieldBinding', JSON_OBJECT('mode', 'field', 'source', 'field_asset', 'fieldCode', 'storeName', 'columnName', 'store_name'),
    'children', JSON_ARRAY(), 'advancedProps', JSON_OBJECT()),
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
    'children', JSON_ARRAY(), 'advancedProps', JSON_OBJECT()),
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
    'children', JSON_ARRAY(), 'advancedProps', JSON_OBJECT()),
  JSON_OBJECT('id', 'cmp_remark', 'label', '备注', 'componentKey', 'textarea',
    'props', JSON_OBJECT('placeholder', '请输入备注', 'rows', 3, 'maxlength', 500, 'showWordLimit', true),
    'layout', JSON_OBJECT('span', 2), 'validation', JSON_OBJECT('required', false, 'rules', JSON_ARRAY()),
    'visibility', JSON_OBJECT('hidden', false, 'readonly', false),
    'fieldBinding', JSON_OBJECT('mode', 'field', 'source', 'field_asset', 'fieldCode', 'remark', 'columnName', 'remark'),
    'children', JSON_ARRAY(), 'advancedProps', JSON_OBJECT()),
  JSON_OBJECT('id', 'cmp_status', 'label', '状态', 'componentKey', 'dictSelect',
    'props', JSON_OBJECT('dictType', 'ps_presale_status', 'placeholder', '状态由系统维护', 'defaultValue', 'DRAFT'), 'layout', JSON_OBJECT('span', 1),
    'validation', JSON_OBJECT('required', false, 'rules', JSON_ARRAY()),
    'visibility', JSON_OBJECT('hidden', true, 'readonly', true),
    'fieldBinding', JSON_OBJECT('mode', 'field', 'source', 'field_asset', 'fieldCode', 'status', 'columnName', 'status'),
    'children', JSON_ARRAY(), 'advancedProps', JSON_OBJECT())
);

SET @ps_edit_schema_patch := JSON_ARRAY(
  JSON_OBJECT('field', 'presaleNo', 'label', '预售单号', 'type', 'input', 'readonly', true),
  JSON_OBJECT('field', 'salesUserId', 'label', '导购userid', 'type', 'input', 'required', false, 'readonly', true, 'hidden', true, 'formVisible', false),
  JSON_OBJECT('field', 'salesUserName', 'label', '导购姓名', 'type', 'input', 'readonly', true),
  JSON_OBJECT('field', 'staffNo', 'label', '工号', 'type', 'input', 'readonly', true),
  JSON_OBJECT('field', 'storeId', 'label', '门店编码', 'type', 'input', 'readonly', true),
  JSON_OBJECT('field', 'storeName', 'label', '门店名称', 'type', 'input', 'readonly', true),
  JSON_OBJECT('field', 'memberPhone', 'label', '会员手机号', 'type', 'input', 'required', true),
  JSON_OBJECT('field', 'memberId', 'label', '会员ID', 'type', 'input', 'readonly', true),
  JSON_OBJECT('field', 'memberName', 'label', '会员姓名', 'type', 'input', 'readonly', true),
  JSON_OBJECT('field', 'payMethod', 'label', '收款方式', 'type', 'dictSelect', 'dictType', 'ps_presale_pay_method', 'required', true, 'defaultValue', 'STATIC_CODE', 'props', JSON_OBJECT('defaultValue', 'STATIC_CODE')),
  JSON_OBJECT('field', 'staticPaymentNo', 'label', '静态码单号', 'type', 'input', 'runtimeRules', JSON_ARRAY(JSON_OBJECT('id', 'show_static_payment_no', 'enabled', true, 'mode', 'all', 'conditions', JSON_ARRAY(JSON_OBJECT('source', 'formData', 'field', 'payMethod', 'operator', 'eq', 'value', 'STATIC_CODE')), 'effect', JSON_OBJECT('visible', true, 'whenUnmatched', 'hidden')))),
  JSON_OBJECT('field', 'staticPaymentInfo', 'label', '收款信息', 'type', 'textarea', 'readonly', true, 'runtimeRules', JSON_ARRAY(JSON_OBJECT('id', 'show_static_payment_info', 'enabled', true, 'mode', 'all', 'conditions', JSON_ARRAY(JSON_OBJECT('source', 'formData', 'field', 'payMethod', 'operator', 'eq', 'value', 'STATIC_CODE')), 'effect', JSON_OBJECT('visible', true, 'whenUnmatched', 'hidden')))),
  JSON_OBJECT('field', 'cashAmount', 'label', '现金金额', 'type', 'money', 'min', 0.01, 'precision', 2, 'step', 0.01, 'runtimeRules', JSON_ARRAY(JSON_OBJECT('id', 'show_cash_amount', 'enabled', true, 'mode', 'all', 'conditions', JSON_ARRAY(JSON_OBJECT('source', 'formData', 'field', 'payMethod', 'operator', 'eq', 'value', 'CASH')), 'effect', JSON_OBJECT('visible', true, 'whenUnmatched', 'hidden')))),
  JSON_OBJECT('field', 'remark', 'label', '备注', 'type', 'textarea'),
  JSON_OBJECT('field', 'status', 'label', '状态', 'type', 'dictSelect', 'dictType', 'ps_presale_status', 'readonly', true, 'defaultValue', 'DRAFT', 'hidden', true, 'formVisible', false)
);

SET @ps_form_schema_patch := JSON_OBJECT(
  'schemaVersion', '1.0.0',
  'formKey', 'ps_presale_order_form',
  'objectCode', 'PS_PRESALE_ORDER',
  'objectName', '预售单',
  'formName', '预售信息登记',
  'layout', JSON_OBJECT('gridColumns', 2, 'labelPlacement', 'left', 'labelAlign', 'right',
    'labelWidth', 104, 'size', 'medium', 'columnGap', 16, 'rowGap', 12, 'showFeedback', true),
  'settings', JSON_OBJECT(
    'governance', JSON_OBJECT(
      'fieldEvents', JSON_ARRAY(
        JSON_OBJECT(
          'id', 'wecom_user_store', 'name', '当前导购和门店', 'enabled', true,
          'trigger', 'FORM_LOAD', 'sourceField', '', 'sourceType', 'EXTERNAL_API',
          'sourceKey', 'wecom/user-store', 'debounceMs', 0, 'skipWhenEmpty', false,
          'clearTargetsOnTrigger', false,
          'paramMappings', JSON_ARRAY(JSON_OBJECT('param', 'userid', 'source', 'CONTEXT_PATH', 'path', 'currentUser.userId')),
          'resultMode', 'ROOT',
          'resultMappings', JSON_ARRAY(
            JSON_OBJECT('from', 'userid', 'to', 'salesUserId', 'whenMissing', 'CLEAR'),
            JSON_OBJECT('from', 'userName', 'to', 'salesUserName', 'whenMissing', 'CLEAR'),
            JSON_OBJECT('from', 'staffNo', 'to', 'staffNo', 'whenMissing', 'CLEAR'),
            JSON_OBJECT('from', 'storeId', 'to', 'storeId', 'whenMissing', 'CLEAR'),
            JSON_OBJECT('from', 'storeName', 'to', 'storeName', 'whenMissing', 'CLEAR')
          ),
          'errorMode', 'MESSAGE', 'errorMessage', '未能读取当前导购和门店信息'
        ),
        JSON_OBJECT(
          'id', 'member_by_mobile', 'name', '手机号查询会员', 'enabled', true,
          'trigger', 'BLUR', 'sourceField', 'memberPhone', 'sourceType', 'EXTERNAL_API',
          'sourceKey', 'member/member-by-mobile', 'debounceMs', 300, 'skipWhenEmpty', true,
          'clearTargetsOnTrigger', true,
          'paramMappings', JSON_ARRAY(JSON_OBJECT('param', 'mobile', 'source', 'FORM_FIELD', 'field', 'memberPhone')),
          'resultMode', 'ROOT',
          'resultMappings', JSON_ARRAY(
            JSON_OBJECT('from', 'memberId', 'to', 'memberId', 'whenMissing', 'CLEAR'),
            JSON_OBJECT('from', 'memberName', 'to', 'memberName', 'whenMissing', 'CLEAR')
          ),
          'errorMode', 'MESSAGE', 'notFoundMessage', '未查询到会员信息', 'errorMessage', '会员查询失败'
        ),
        JSON_OBJECT(
          'id', 'payment_static_code', 'name', '静态码单号查询收款', 'enabled', true,
          'trigger', 'BLUR', 'sourceField', 'staticPaymentNo', 'sourceType', 'EXTERNAL_API',
          'sourceKey', 'payment/static-code', 'debounceMs', 300, 'skipWhenEmpty', true,
          'clearTargetsOnTrigger', true,
          'paramMappings', JSON_ARRAY(JSON_OBJECT('param', 'paymentNo', 'source', 'FORM_FIELD', 'field', 'staticPaymentNo')),
          'resultMode', 'ROOT',
          'resultMappings', JSON_ARRAY(JSON_OBJECT('from', 'summary', 'to', 'staticPaymentInfo', 'whenMissing', 'CLEAR')),
          'errorMode', 'MESSAGE', 'notFoundMessage', '未查询到静态码收款记录', 'errorMessage', '收款记录查询失败'
        )
      )
    )
  ),
  'components', JSON_EXTRACT(@ps_form_patch_components, '$')
);

UPDATE ai_lowcode_model
SET model_schema = JSON_SET(
      model_schema,
      '$.fields[3].required', false,
      '$.fields[3].formVisible', false,
      '$.fields[12].formVisible', true,
      '$.fields[13].formVisible', true,
      '$.fields[14].formVisible', true,
      '$.fields[16].required', false,
      '$.fields[16].formVisible', false
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND domain_code = 'PRESALE_REGISTRATION'
  AND model_code = 'ps_presale_order'
  AND del_flag = 0
  AND JSON_VALID(model_schema)
  AND JSON_UNQUOTE(JSON_EXTRACT(model_schema, '$.fields[3].field')) = 'salesUserId'
  AND JSON_UNQUOTE(JSON_EXTRACT(model_schema, '$.fields[16].field')) = 'status';

UPDATE ai_crud_config
SET edit_schema = JSON_EXTRACT(@ps_edit_schema_patch, '$'),
    options = JSON_SET(
      COALESCE(options, JSON_OBJECT()),
      '$.formDesignerSchema', JSON_EXTRACT(@ps_form_schema_patch, '$'),
      '$.masterDetailConfig.children[0].tabTitle', '预售商品',
      '$.masterDetailConfig.children[0].relationName', '预售商品',
      '$.masterDetailConfig.children[1].tabTitle', '操作日志',
      '$.masterDetailConfig.children[1].relationName', '操作日志'
    ),
    page_schema = JSON_SET(
      COALESCE(page_schema, JSON_OBJECT()),
      '$.zones[2].props.formDesignerSchema', JSON_EXTRACT(@ps_form_schema_patch, '$'),
      '$.modelRefs[1].props.tabTitle', '预售商品',
      '$.modelRefs[1].props.relationName', '预售商品',
      '$.modelRefs[2].props.tabTitle', '操作日志',
      '$.modelRefs[2].props.relationName', '操作日志'
    ),
    model_schema = JSON_SET(
      model_schema,
      '$.fields[3].required', false,
      '$.fields[3].formVisible', false,
      '$.fields[12].formVisible', true,
      '$.fields[13].formVisible', true,
      '$.fields[14].formVisible', true,
      '$.fields[16].required', false,
      '$.fields[16].formVisible', false
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND config_key = 'ps_presale_order'
  AND del_flag = 0
  AND JSON_VALID(model_schema);

UPDATE ai_business_object
SET designer_options = JSON_SET(
      COALESCE(designer_options, JSON_OBJECT()),
      '$.formDesignerSchema', JSON_EXTRACT(@ps_form_schema_patch, '$')
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND suite_code = 'PRESALE_REGISTRATION'
  AND object_code = 'PS_PRESALE_ORDER'
  AND del_flag = 0
  AND JSON_UNQUOTE(JSON_EXTRACT(designer_options, '$.seedKey')) = 'presale-registration-v1';

UPDATE ai_business_object_design_version
SET model_snapshot = JSON_SET(
      model_snapshot,
      '$.fields[3].required', false,
      '$.fields[3].formVisible', false,
      '$.fields[12].formVisible', true,
      '$.fields[13].formVisible', true,
      '$.fields[14].formVisible', true,
      '$.fields[16].required', false,
      '$.fields[16].formVisible', false
    ),
    page_snapshot = JSON_SET(
      COALESCE(page_snapshot, JSON_OBJECT()),
      '$.zones[2].props.formDesignerSchema', JSON_EXTRACT(@ps_form_schema_patch, '$'),
      '$.modelRefs[1].props.tabTitle', '预售商品',
      '$.modelRefs[1].props.relationName', '预售商品',
      '$.modelRefs[2].props.tabTitle', '操作日志',
      '$.modelRefs[2].props.relationName', '操作日志'
    ),
    designer_options_snapshot = JSON_SET(
      COALESCE(designer_options_snapshot, JSON_OBJECT()),
      '$.formDesignerSchema', JSON_EXTRACT(@ps_form_schema_patch, '$')
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND object_code = 'PS_PRESALE_ORDER'
  AND publish_status = 'PUBLISHED'
  AND JSON_VALID(model_snapshot);

UPDATE ai_crud_config_version version_row
INNER JOIN ai_crud_config config_row
  ON config_row.tenant_id = version_row.tenant_id
 AND config_row.id = version_row.config_id
 AND config_row.config_key = 'ps_presale_order'
 AND config_row.del_flag = 0
SET version_row.edit_schema = JSON_EXTRACT(@ps_edit_schema_patch, '$'),
    version_row.options = JSON_SET(
      COALESCE(version_row.options, JSON_OBJECT()),
      '$.formDesignerSchema', JSON_EXTRACT(@ps_form_schema_patch, '$'),
      '$.masterDetailConfig.children[0].tabTitle', '预售商品',
      '$.masterDetailConfig.children[0].relationName', '预售商品',
      '$.masterDetailConfig.children[1].tabTitle', '操作日志',
      '$.masterDetailConfig.children[1].relationName', '操作日志'
    ),
    version_row.page_schema = JSON_SET(
      COALESCE(version_row.page_schema, JSON_OBJECT()),
      '$.zones[2].props.formDesignerSchema', JSON_EXTRACT(@ps_form_schema_patch, '$'),
      '$.modelRefs[1].props.tabTitle', '预售商品',
      '$.modelRefs[1].props.relationName', '预售商品',
      '$.modelRefs[2].props.tabTitle', '操作日志',
      '$.modelRefs[2].props.relationName', '操作日志'
    ),
    version_row.model_schema = JSON_SET(
      version_row.model_schema,
      '$.fields[3].required', false,
      '$.fields[3].formVisible', false,
      '$.fields[12].formVisible', true,
      '$.fields[13].formVisible', true,
      '$.fields[14].formVisible', true,
      '$.fields[16].required', false,
      '$.fields[16].formVisible', false
    ),
    version_row.update_by = 1,
    version_row.update_time = NOW()
WHERE version_row.tenant_id = 1
  AND JSON_VALID(version_row.model_schema);
