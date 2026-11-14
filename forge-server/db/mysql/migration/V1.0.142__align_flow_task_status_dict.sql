-- 对齐流程任务状态枚举与前端字典。sys_flow_task 是运行态快照表，状态历史必须可见且不能复用错误文案。
UPDATE sys_dict_data
SET dict_label = '已取消',
    remark = '流程任务被流程终止或取消',
    list_class = 'default',
    update_time = NOW()
WHERE tenant_id = 1
  AND dict_type IN ('flow_done_status', 'flow_started_status')
  AND dict_value = '5'
  AND del_flag = 0;

INSERT INTO sys_dict_data (
    tenant_id, dict_sort, dict_label, dict_value, dict_type,
    css_class, list_class, is_default, dict_status, remark,
    create_by, create_time, update_by, update_time, create_dept
)
SELECT 1, 8, '已终结', '8', seed.dict_type,
       NULL, 'error', 'N', 1, '审批人或管理员终止流程',
       1, NOW(), 1, NOW(), 1
FROM (
    SELECT 'flow_done_status' AS dict_type
    UNION ALL SELECT 'flow_started_status'
) seed
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_dict_data existing
    WHERE existing.tenant_id = 1
      AND existing.dict_type = seed.dict_type
      AND existing.dict_value = '8'
      AND existing.del_flag = 0
);

INSERT INTO sys_dict_data (
    tenant_id, dict_sort, dict_label, dict_value, dict_type,
    css_class, list_class, is_default, dict_status, remark,
    create_by, create_time, update_by, update_time, create_dept
)
SELECT 1, 7, '已退回', '7', seed.dict_type,
       NULL, 'warning', 'N', 1, '审批任务退回上一节点',
       1, NOW(), 1, NOW(), 1
FROM (
    SELECT 'flow_done_status' AS dict_type
    UNION ALL SELECT 'flow_started_status'
) seed
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_dict_data existing
    WHERE existing.tenant_id = 1
      AND existing.dict_type = seed.dict_type
      AND existing.dict_value = '7'
      AND existing.del_flag = 0
);
