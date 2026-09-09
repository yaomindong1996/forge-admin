-- 为外部接口连接器增加企查查签名认证适配器。
INSERT INTO sys_dict_data (
    tenant_id, dict_sort, dict_label, dict_value, dict_type,
    css_class, list_class, is_default, dict_status, remark,
    create_by, create_time, update_by, update_time, create_dept
)
SELECT 1, 8, '企查查签名认证', 'qichacha', 'external_auth_adapter',
       NULL, 'success', 'N', 1, '使用 AppKey、Secret 自动生成 Token、Timespan、Sign 请求头',
       1, NOW(), 1, NOW(), 1
WHERE NOT EXISTS (
    SELECT 1 FROM sys_dict_data
    WHERE tenant_id = 1 AND dict_type = 'external_auth_adapter' AND dict_value = 'qichacha'
);
