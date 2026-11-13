-- Add an explicit aggregate state for collaboration deliveries where only some recipients succeed.
INSERT INTO sys_dict_data (
    tenant_id, dict_sort, dict_label, dict_value, dict_type,
    css_class, list_class, is_default, dict_status, remark,
    create_by, create_time, update_by, update_time, create_dept
)
SELECT 1, 4, '部分成功', '3', 'sys_message_send_status',
       NULL, 'warning', 'N', 1, '部分接收人投递失败',
       1, NOW(), 1, NOW(), 1
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_dict_data
    WHERE tenant_id = 1
      AND dict_type = 'sys_message_send_status'
      AND dict_value = '3'
      AND del_flag = 0
);
