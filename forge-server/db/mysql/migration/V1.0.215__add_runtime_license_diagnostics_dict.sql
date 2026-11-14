-- 只读授权诊断字典，不改变菜单、角色权限或任何已签发授权。
INSERT INTO sys_dict_type (
    tenant_id, dict_name, dict_type, dict_status, create_by, create_time, update_by, update_time, create_dept, del_flag
)
SELECT 1, seed.name, seed.type, 1, 1, NOW(), 1, NOW(), 1, 0
FROM (
    SELECT '运行时授权模式' AS name, 'sys_runtime_license_mode' AS type
    UNION ALL SELECT '运行时授权文件状态', 'sys_runtime_license_state'
) seed
WHERE NOT EXISTS (
    SELECT 1 FROM sys_dict_type existing
    WHERE existing.tenant_id = 1 AND existing.dict_type = seed.type AND existing.del_flag = 0
);

INSERT INTO sys_dict_data (
    tenant_id, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, dict_status,
    create_by, create_time, update_by, update_time, create_dept, del_flag
)
SELECT 1, seed.sort, seed.label, seed.value, seed.type, seed.style, 'N', 1,
       1, NOW(), 1, NOW(), 1, 0
FROM (
    SELECT 1 AS sort, '社区默认' AS label, 'community' AS `value`,
           'sys_runtime_license_mode' AS type, 'info' AS style
    UNION ALL SELECT 2, '许可证验证', 'license', 'sys_runtime_license_mode', 'info'
    UNION ALL SELECT 3, '自定义授权', 'custom', 'sys_runtime_license_mode', 'warning'
    UNION ALL SELECT 4, '授权组件不可用', 'unavailable', 'sys_runtime_license_mode', 'error'
    UNION ALL SELECT 1, '有效', 'valid', 'sys_runtime_license_state', 'success'
    UNION ALL SELECT 2, '尚未生效', 'not_yet_valid', 'sys_runtime_license_state', 'warning'
    UNION ALL SELECT 3, '使用已到期', 'expired', 'sys_runtime_license_state', 'error'
    UNION ALL SELECT 4, '绑定不匹配', 'binding_mismatch', 'sys_runtime_license_state', 'error'
    UNION ALL SELECT 5, '文件无效', 'invalid', 'sys_runtime_license_state', 'error'
) seed
WHERE NOT EXISTS (
    SELECT 1 FROM sys_dict_data existing
    WHERE existing.tenant_id = 1 AND existing.dict_type = seed.type
      AND existing.dict_value = seed.value AND existing.del_flag = 0
);
