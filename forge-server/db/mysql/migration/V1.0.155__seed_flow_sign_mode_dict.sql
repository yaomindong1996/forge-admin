-- 动态加签模式和关系状态字典，供流程页面按服务端枚举展示。
INSERT INTO sys_dict_type (
    tenant_id, dict_name, dict_type, dict_status, remark,
    create_by, create_time, update_by, update_time, create_dept
)
SELECT 1, '流程动态加签模式', 'flow_task_sign_mode', 1,
       '动态加签/减签的执行模式', 1, NOW(), 1, NOW(), 1
WHERE NOT EXISTS (
    SELECT 1 FROM sys_dict_type
    WHERE tenant_id = 1 AND dict_type = 'flow_task_sign_mode'
);

INSERT INTO sys_dict_data (
    tenant_id, dict_sort, dict_label, dict_value, dict_type,
    css_class, list_class, is_default, dict_status, remark,
    create_by, create_time, update_by, update_time, create_dept
)
SELECT seed.tenant_id, seed.dict_sort, seed.dict_label, seed.dict_value,
       'flow_task_sign_mode', NULL, seed.list_class, seed.is_default,
       1, seed.remark, 1, NOW(), 1, NOW(), 1
FROM (
    SELECT 1 tenant_id, 1 dict_sort, '前加签' dict_label, 'BEFORE' dict_value,
           'warning' list_class, 'N' is_default, '在当前审批关系中增加前置处理人' remark
    UNION ALL
    SELECT 1, 2, '后加签', 'AFTER', 'info', 'N', '在当前审批关系中增加后置处理人'
    UNION ALL
    SELECT 1, 3, '并行加签', 'PARALLEL', 'success', 'Y', '与当前审批任务并行处理'
) seed
WHERE NOT EXISTS (
    SELECT 1 FROM sys_dict_data d
    WHERE d.tenant_id = seed.tenant_id
      AND d.dict_type = 'flow_task_sign_mode'
      AND d.dict_value = seed.dict_value
);

INSERT INTO sys_dict_type (
    tenant_id, dict_name, dict_type, dict_status, remark,
    create_by, create_time, update_by, update_time, create_dept
)
SELECT 1, '流程动态加签关系状态', 'flow_task_sign_relation_status', 1,
       '动态加签关系是否仍然有效', 1, NOW(), 1, NOW(), 1
WHERE NOT EXISTS (
    SELECT 1 FROM sys_dict_type
    WHERE tenant_id = 1 AND dict_type = 'flow_task_sign_relation_status'
);

INSERT INTO sys_dict_data (
    tenant_id, dict_sort, dict_label, dict_value, dict_type,
    css_class, list_class, is_default, dict_status, remark,
    create_by, create_time, update_by, update_time, create_dept
)
SELECT seed.tenant_id, seed.dict_sort, seed.dict_label, seed.dict_value,
       'flow_task_sign_relation_status', NULL, seed.list_class, 'N',
       1, seed.remark, 1, NOW(), 1, NOW(), 1
FROM (
    SELECT 1 tenant_id, 1 dict_sort, '有效' dict_label, 'ACTIVE' dict_value,
           'success' list_class, '动态加签关系仍可参与审批' remark
    UNION ALL
    SELECT 1, 2, '已撤回', 'REVOKED', 'default', '动态加签关系已被减签撤回'
) seed
WHERE NOT EXISTS (
    SELECT 1 FROM sys_dict_data d
    WHERE d.tenant_id = seed.tenant_id
      AND d.dict_type = 'flow_task_sign_relation_status'
      AND d.dict_value = seed.dict_value
);
