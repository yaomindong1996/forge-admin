-- 补已有状态 generating 的展示字典，不新增业务状态、权限或表结构。
-- 回滚只删除 tenant_id=1 / ai_image_generate_status / generating 这一项；不要改写历史迁移。
INSERT INTO sys_dict_data (
    tenant_id, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, dict_status,
    create_by, create_time, update_by, update_time, create_dept, del_flag
)
SELECT 1, 2, '生成中', 'generating', 'ai_image_generate_status', 'info', 'N', 1,
       1, NOW(), 1, NOW(), 1, 0
WHERE EXISTS (
    SELECT 1 FROM sys_dict_type
    WHERE tenant_id = 1 AND dict_type = 'ai_image_generate_status' AND del_flag = 0
)
AND NOT EXISTS (
    SELECT 1 FROM sys_dict_data
    WHERE tenant_id = 1 AND dict_type = 'ai_image_generate_status'
      AND dict_value = 'generating' AND del_flag = 0
);
