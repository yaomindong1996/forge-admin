-- 访问入口类型中文字典：工作台只展示业务文案，不直接暴露 RUNTIME/ROUTE 等技术枚举。

INSERT INTO sys_dict_type (
  tenant_id, dict_name, dict_type, dict_status, remark,
  create_by, create_time, update_by, update_time, create_dept
)
SELECT 1, '访问入口类型', 'ai_business_app_entry_mode', 1,
       '业务应用页面入口的打开方式',
       1, NOW(), 1, NOW(), 1
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_dict_type t
  WHERE t.tenant_id = 1
    AND t.dict_type = 'ai_business_app_entry_mode'
);

INSERT INTO sys_dict_data (
  tenant_id, dict_sort, dict_label, dict_value, dict_type,
  css_class, list_class, is_default, dict_status, remark,
  create_by, create_time, update_by, update_time, create_dept
)
SELECT seed.tenant_id, seed.dict_sort, seed.dict_label, seed.dict_value, seed.dict_type,
       NULL, seed.list_class, seed.is_default, 1, seed.remark,
       1, NOW(), 1, NOW(), 1
FROM (
  SELECT 1 tenant_id, 1 dict_sort, '业务页面' dict_label, 'RUNTIME' dict_value,
         'ai_business_app_entry_mode' dict_type, 'success' list_class, 'Y' is_default,
         '由低代码运行时渲染的表单、列表或详情页面' remark
  UNION ALL SELECT 1, 2, '系统页面', 'ROUTE', 'ai_business_app_entry_mode', 'info', 'N', '系统内部路由页面'
  UNION ALL SELECT 1, 3, '内嵌页面', 'IFRAME', 'ai_business_app_entry_mode', 'warning', 'N', '通过受控 iframe 打开的页面'
  UNION ALL SELECT 1, 4, '外部链接', 'EXTERNAL', 'ai_business_app_entry_mode', 'default', 'N', '在外部地址打开的页面'
  UNION ALL SELECT 1, 5, '移动端页面', 'H5', 'ai_business_app_entry_mode', 'info', 'N', '移动端 H5 页面'
  UNION ALL SELECT 1, 6, '接口能力', 'API', 'ai_business_app_entry_mode', 'default', 'N', '面向系统集成的接口入口'
) seed
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_dict_data d
  WHERE d.tenant_id = seed.tenant_id
    AND d.dict_type = seed.dict_type
    AND d.dict_value = seed.dict_value
);
