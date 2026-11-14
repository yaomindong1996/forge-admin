-- 只增加模型草稿/发布快照关系的展示字典，不回填或修改任何模型/实例状态。
-- 回滚可保留字典；如需移除，只处理本脚本的 dict_type，禁止删除流程版本记录。
INSERT INTO sys_dict_type (
    tenant_id, dict_name, dict_type, dict_status, create_by, create_time, update_by, update_time, create_dept, del_flag
)
SELECT 1, '流程模型发布关系', 'flow_model_publication_state', 1, 1, NOW(), 1, NOW(), 1, 0
WHERE NOT EXISTS (
    SELECT 1 FROM sys_dict_type
    WHERE tenant_id = 1 AND dict_type = 'flow_model_publication_state' AND del_flag = 0
);

INSERT INTO sys_dict_data (
    tenant_id, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, dict_status,
    create_by, create_time, update_by, update_time, create_dept, del_flag
)
SELECT 1, seed.sort_order, seed.dict_label, seed.dict_value, 'flow_model_publication_state', seed.list_class, 'N', 1,
       1, NOW(), 1, NOW(), 1, 0
FROM (
    SELECT 1 sort_order, '尚未发布' dict_label, 'UNPUBLISHED' dict_value, 'default' list_class
    UNION ALL SELECT 2, '与发布版本一致', 'SYNCED', 'default'
    UNION ALL SELECT 3, '有未发布修改', 'CHANGED', 'warning'
    UNION ALL SELECT 4, '发布信息待核对', 'UNKNOWN', 'warning'
) seed
WHERE NOT EXISTS (
    SELECT 1 FROM sys_dict_data
    WHERE tenant_id = 1 AND dict_type = 'flow_model_publication_state'
      AND dict_value = seed.dict_value AND del_flag = 0
);
