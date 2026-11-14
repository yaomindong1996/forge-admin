-- 开放平台补充撤回操作；不自动扩大任何客户端已有授权。
INSERT INTO sys_dict_data (
  tenant_id, dict_sort, dict_label, dict_value, dict_type,
  css_class, list_class, is_default, dict_status, remark,
  create_by, create_time, update_by, update_time, create_dept
)
SELECT 1, 5, '撤回申请', 'WITHDRAW', 'ai_capability_flow_operation',
       NULL, 'warning', 'N', 1, '仅流程发起人可撤回仍在运行的业务申请',
       1, NOW(), 1, NOW(), 1
WHERE NOT EXISTS (
  SELECT 1 FROM sys_dict_data
  WHERE tenant_id = 1 AND dict_type = 'ai_capability_flow_operation' AND dict_value = 'WITHDRAW'
);
