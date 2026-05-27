-- 业务应用平台字典、菜单和按钮权限。

INSERT INTO sys_dict_type (tenant_id, dict_name, dict_type, dict_status, remark, create_by, create_time, update_by, update_time, create_dept)
SELECT seed.tenant_id, seed.dict_name, seed.dict_type, seed.dict_status, seed.remark, 1, NOW(), 1, NOW(), 1
FROM (
  SELECT 1 tenant_id, '业务套件' dict_name, 'ai_business_suite' dict_type, 1 dict_status, '企业应用装配平台业务套件' remark
  UNION ALL SELECT 1, '业务对象类型', 'ai_business_object_type', 1, '业务对象/实体类型'
  UNION ALL SELECT 1, '业务对象关系类型', 'ai_business_relation_type', 1, '业务对象之间的引用、明细和关联列表关系'
  UNION ALL SELECT 1, '业务应用类型', 'ai_business_app_type', 1, '应用入口和渠道类型'
  UNION ALL SELECT 1, '业务应用入口模式', 'ai_business_app_entry_mode', 1, '应用入口打开方式'
  UNION ALL SELECT 1, '业务能力挂接类型', 'ai_business_binding_type', 1, '对象和应用可接入的引擎能力'
) seed
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_dict_type t
  WHERE t.tenant_id = seed.tenant_id
    AND t.dict_type = seed.dict_type
);

INSERT INTO sys_dict_data (tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default,
                           dict_status, remark, create_by, create_time, update_by, update_time, create_dept)
SELECT seed.tenant_id, seed.dict_sort, seed.dict_label, seed.dict_value, seed.dict_type, NULL, seed.list_class,
       seed.is_default, 1, seed.remark, 1, NOW(), 1, NOW(), 1
FROM (
  SELECT 1 tenant_id, 1 dict_sort, 'CRM' dict_label, 'CRM' dict_value, 'ai_business_suite' dict_type, 'success' list_class, 'Y' is_default, '客户关系管理业务套件' remark
  UNION ALL SELECT 1, 2, '合同', 'CONTRACT', 'ai_business_suite', 'info', 'N', '合同管理业务套件'
  UNION ALL SELECT 1, 3, '项目', 'PROJECT', 'ai_business_suite', 'primary', 'N', '项目管理业务套件'
  UNION ALL SELECT 1, 4, '采购', 'PURCHASE', 'ai_business_suite', 'warning', 'N', '采购管理业务套件'
  UNION ALL SELECT 1, 5, '服务', 'SERVICE', 'ai_business_suite', 'default', 'N', '服务工单业务套件'
  UNION ALL SELECT 1, 1, '主数据对象', 'MASTER', 'ai_business_object_type', 'success', 'Y', '客户、线索等主数据对象'
  UNION ALL SELECT 1, 2, '明细对象', 'DETAIL', 'ai_business_object_type', 'info', 'N', '合同明细、跟进记录等明细对象'
  UNION ALL SELECT 1, 3, '查找对象', 'LOOKUP', 'ai_business_object_type', 'default', 'N', '字典式或引用型对象'
  UNION ALL SELECT 1, 4, '业务单据', 'TRANSACTION', 'ai_business_object_type', 'warning', 'N', '商机、合同、回款等业务单据'
  UNION ALL SELECT 1, 1, '引用关系', 'REFERENCE', 'ai_business_relation_type', 'info', 'Y', '对象通过业务字段引用另一个对象'
  UNION ALL SELECT 1, 2, '明细关系', 'DETAIL', 'ai_business_relation_type', 'success', 'N', '明细对象依附主对象'
  UNION ALL SELECT 1, 3, '关联列表', 'CHILD_LIST', 'ai_business_relation_type', 'primary', 'N', '详情页展示下级或关联对象列表'
  UNION ALL SELECT 1, 4, '多对多关系', 'MANY_TO_MANY', 'ai_business_relation_type', 'warning', 'N', '通过中间关系表达多对多关联'
  UNION ALL SELECT 1, 1, '业务应用', 'BUSINESS', 'ai_business_app_type', 'success', 'Y', '客户管理、合同管理等标准业务入口'
  UNION ALL SELECT 1, 2, '嵌入应用', 'EMBEDDED', 'ai_business_app_type', 'info', 'N', '大屏、BI、外部页面等嵌入入口'
  UNION ALL SELECT 1, 3, '移动应用', 'MOBILE', 'ai_business_app_type', 'primary', 'N', 'H5、移动审批和移动业务入口'
  UNION ALL SELECT 1, 4, '集成应用', 'INTEGRATION', 'ai_business_app_type', 'warning', 'N', '开放接口、Webhook、第三方平台连接'
  UNION ALL SELECT 1, 1, '运行态页面', 'RUNTIME', 'ai_business_app_entry_mode', 'success', 'Y', '打开动态CRUD运行页'
  UNION ALL SELECT 1, 2, '内部路由', 'ROUTE', 'ai_business_app_entry_mode', 'info', 'N', '打开平台内部路由'
  UNION ALL SELECT 1, 3, '内嵌页面', 'IFRAME', 'ai_business_app_entry_mode', 'primary', 'N', '通过iframe内嵌访问'
  UNION ALL SELECT 1, 4, '外部打开', 'EXTERNAL', 'ai_business_app_entry_mode', 'warning', 'N', '新窗口打开外部地址'
  UNION ALL SELECT 1, 5, 'H5入口', 'H5', 'ai_business_app_entry_mode', 'default', 'N', '移动H5入口'
  UNION ALL SELECT 1, 6, '接口入口', 'API', 'ai_business_app_entry_mode', 'info', 'N', '进入接口或集成配置'
  UNION ALL SELECT 1, 1, '流程', 'FLOW', 'ai_business_binding_type', 'info', 'N', '流程能力'
  UNION ALL SELECT 1, 2, '审批', 'APPROVAL', 'ai_business_binding_type', 'success', 'N', '审批能力'
  UNION ALL SELECT 1, 3, '报表', 'REPORT', 'ai_business_binding_type', 'primary', 'N', '报表和看板能力'
  UNION ALL SELECT 1, 4, '权限', 'PERMISSION', 'ai_business_binding_type', 'warning', 'N', '对象级权限能力'
  UNION ALL SELECT 1, 5, '消息', 'MESSAGE', 'ai_business_binding_type', 'info', 'N', '站内信、邮件、短信等消息能力'
  UNION ALL SELECT 1, 6, '触发器', 'TRIGGER', 'ai_business_binding_type', 'error', 'N', '轻量对象自动化能力'
  UNION ALL SELECT 1, 7, '导入', 'IMPORT', 'ai_business_binding_type', 'default', 'N', '对象级导入能力'
  UNION ALL SELECT 1, 8, '导出', 'EXPORT', 'ai_business_binding_type', 'default', 'N', '对象级导出能力'
  UNION ALL SELECT 1, 9, '移动', 'MOBILE', 'ai_business_binding_type', 'primary', 'N', '移动端入口能力'
  UNION ALL SELECT 1, 10, '集成', 'INTEGRATION', 'ai_business_binding_type', 'warning', 'N', '第三方平台和开放接口能力'
) seed
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_dict_data d
  WHERE d.tenant_id = seed.tenant_id
    AND d.dict_type = seed.dict_type
    AND d.dict_value = seed.dict_value
);

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external,
                          sso_enabled, sso_target_client, open_target, is_public, menu_status, visible, perms, icon,
                          api_method, api_url, keep_alive, always_show, redirect, remark, create_by, create_time,
                          update_by, update_time, create_dept, client_code)
SELECT 1, '应用中心', 0, 2, 6, '/app-center', 'app-center/index', 0,
       0, NULL, '_self', 0, 1, 1, 'ai:businessApp:list', 'ionicons5:GridOutline',
       NULL, NULL, 1, 0, NULL, '企业应用装配平台主入口', 1, NOW(), 1, NOW(), 1, 'pc'
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_resource
  WHERE tenant_id = 1
    AND resource_type = 2
    AND perms = 'ai:businessApp:list'
);

SET @app_center_menu_id := (
  SELECT id
  FROM sys_resource
  WHERE tenant_id = 1
    AND resource_type = 2
    AND perms = 'ai:businessApp:list'
  LIMIT 1
);

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external,
                          sso_enabled, sso_target_client, open_target, is_public, menu_status, visible, perms, icon,
                          api_method, api_url, keep_alive, always_show, redirect, remark, create_by, create_time,
                          update_by, update_time, create_dept, client_code)
SELECT seed.tenant_id, seed.resource_name, @app_center_menu_id, seed.resource_type, seed.sort, seed.path,
       seed.component, 0, 0, NULL, '_self', 0, seed.menu_status, seed.visible, seed.perms, seed.icon,
       NULL, NULL, seed.keep_alive, 0, NULL, seed.remark, 1, NOW(), 1, NOW(), 1, 'pc'
FROM (
  SELECT 1 tenant_id, '业务套件详情' resource_name, 2 resource_type, 1 sort, '/app-center/suite/:suiteCode' path, 'app-center/suite-detail' component, 0 menu_status, 0 visible, 'ai:businessSuite:list' perms, 'ionicons5:AlbumsOutline' icon, 0 keep_alive, '业务套件详情隐藏路由' remark
  UNION ALL SELECT 1, '业务对象详情', 2, 2, '/app-center/object/:objectCode', 'app-center/object-detail', 0, 0, 'ai:businessObject:list', 'ionicons5:CubeOutline', 0, '业务对象详情隐藏路由'
  UNION ALL SELECT 1, '引擎中心', 2, 3, '/app-center/engines', 'app-center/engine-center', 1, 1, 'ai:businessEngine:list', 'ionicons5:HardwareChipOutline', 1, '流程、审批、报表、权限和消息引擎统一入口'
  UNION ALL SELECT 1, '移动端中心', 2, 4, '/app-center/mobile', 'app-center/mobile-center', 1, 1, 'ai:businessMobile:list', 'ionicons5:PhonePortraitOutline', 1, '移动H5和移动业务入口'
  UNION ALL SELECT 1, '集成中心', 2, 5, '/app-center/integration', 'app-center/integration-center', 1, 1, 'ai:businessIntegration:list', 'ionicons5:GitNetworkOutline', 1, '开放接口、Webhook和第三方平台连接入口'
) seed
WHERE @app_center_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM sys_resource r
    WHERE r.tenant_id = seed.tenant_id
      AND r.resource_type = seed.resource_type
      AND r.perms = seed.perms
  );

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external,
                          sso_enabled, sso_target_client, open_target, is_public, menu_status, visible, perms, icon,
                          api_method, api_url, keep_alive, always_show, redirect, remark, create_by, create_time,
                          update_by, update_time, create_dept, client_code)
SELECT seed.tenant_id, seed.resource_name, @app_center_menu_id, 3, seed.sort, NULL, NULL, 0,
       0, NULL, '_self', 0, 1, 1, seed.perms, NULL,
       NULL, NULL, 0, 0, NULL, seed.remark, 1, NOW(), 1, NOW(), 1, 'pc'
FROM (
  SELECT 1 tenant_id, '查看业务套件' resource_name, 11 sort, 'ai:businessSuite:list' perms, '查询业务套件按钮权限' remark
  UNION ALL SELECT 1, '维护业务套件', 12, 'ai:businessSuite:edit', '新增或编辑业务套件按钮权限'
  UNION ALL SELECT 1, '查看业务对象', 21, 'ai:businessObject:list', '查询业务对象按钮权限'
  UNION ALL SELECT 1, '新增业务对象', 22, 'ai:businessObject:add', '新增业务对象按钮权限'
  UNION ALL SELECT 1, '编辑业务对象', 23, 'ai:businessObject:edit', '编辑业务对象按钮权限'
  UNION ALL SELECT 1, '删除业务对象', 24, 'ai:businessObject:delete', '删除业务对象按钮权限'
  UNION ALL SELECT 1, '配置对象关系', 25, 'ai:businessObject:relation', '配置业务对象关系按钮权限'
  UNION ALL SELECT 1, '配置接入能力', 31, 'ai:businessBinding:config', '配置对象或应用能力挂接按钮权限'
  UNION ALL SELECT 1, '新增应用入口', 41, 'ai:businessApp:add', '新增应用入口按钮权限'
  UNION ALL SELECT 1, '编辑应用入口', 42, 'ai:businessApp:edit', '编辑应用入口按钮权限'
  UNION ALL SELECT 1, '删除应用入口', 43, 'ai:businessApp:delete', '删除应用入口按钮权限'
  UNION ALL SELECT 1, '启停应用入口', 44, 'ai:businessApp:status', '启停应用入口按钮权限'
  UNION ALL SELECT 1, '打开应用入口', 45, 'ai:businessApp:open', '打开应用入口按钮权限'
) seed
WHERE @app_center_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM sys_resource r
    WHERE r.tenant_id = seed.tenant_id
      AND r.resource_type = 3
      AND r.perms = seed.perms
  );
