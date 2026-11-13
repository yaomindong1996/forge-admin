-- 开放平台能力注册页面使用的流程动作字典。

INSERT INTO sys_dict_type (
  tenant_id, dict_name, dict_type, dict_status, remark,
  create_by, create_time, update_by, update_time, create_dept
)
SELECT 1, 'AI能力流程动作', 'ai_capability_flow_operation', 1,
       '开放平台允许发布的受控流程动作',
       1, NOW(), 1, NOW(), 1
WHERE NOT EXISTS (
  SELECT 1 FROM sys_dict_type type
  WHERE type.tenant_id = 1
    AND type.dict_type = 'ai_capability_flow_operation'
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
  SELECT 1 tenant_id, 1 dict_sort, '发起流程' dict_label, 'START' dict_value,
         'ai_capability_flow_operation' dict_type, 'info' list_class, 'Y' is_default,
         '以用户委托身份发起业务对象主流程' remark
  UNION ALL SELECT 1, 2, '审批通过', 'APPROVE', 'ai_capability_flow_operation',
         'success', 'N', '以当前任务办理人身份审批通过'
  UNION ALL SELECT 1, 3, '审批驳回', 'REJECT', 'ai_capability_flow_operation',
         'error', 'N', '以当前任务办理人身份审批驳回'
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM sys_dict_data data
  WHERE data.tenant_id = seed.tenant_id
    AND data.dict_type = seed.dict_type
    AND data.dict_value = seed.dict_value
);
