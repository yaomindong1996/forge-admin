-- 面向用户统一使用“移动端”产品称谓；H5/h5 仅保留为兼容技术编码。

UPDATE sys_client
SET client_name = '移动端',
    description = '移动端浏览器与轻应用客户端',
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND client_code = 'h5';

UPDATE sys_dict_data
SET dict_label = '移动端页面',
    remark = '移动端页面入口（保留 H5 技术编码）',
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND dict_type = 'ai_business_app_entry_mode'
  AND dict_value = 'H5'
  AND del_flag = 0;

UPDATE sys_dict_data
SET dict_label = '移动端',
    remark = '在移动端生成访问入口菜单',
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND dict_type = 'ai_business_app_mount_target'
  AND dict_value = 'MOBILE'
  AND del_flag = 0;
