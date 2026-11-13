-- 能力开放平台：增加“一次提交业务申请”流程动作，并把已有记录 START 调整为高级动作。

INSERT INTO sys_dict_data (
  tenant_id, dict_sort, dict_label, dict_value, dict_type,
  css_class, list_class, is_default, dict_status, remark,
  create_by, create_time, update_by, update_time, create_dept
)
SELECT 1, 1, '提交业务申请', 'SUBMIT', 'ai_capability_flow_operation',
       NULL, 'success', 'Y', 1,
       '外围系统提交申请数据，由 Forge 创建业务记录并立即发起主流程',
       1, NOW(), 1, NOW(), 1
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_dict_data data
  WHERE data.tenant_id = 1
    AND data.dict_type = 'ai_capability_flow_operation'
    AND data.dict_value = 'SUBMIT'
);

UPDATE sys_dict_data
SET dict_sort = CASE dict_value
      WHEN 'SUBMIT' THEN 1
      WHEN 'START' THEN 2
      WHEN 'APPROVE' THEN 3
      WHEN 'REJECT' THEN 4
      ELSE dict_sort
    END,
    dict_label = CASE
      WHEN dict_value = 'START' THEN '发起已有记录流程'
      ELSE dict_label
    END,
    is_default = CASE WHEN dict_value = 'SUBMIT' THEN 'Y' ELSE 'N' END,
    remark = CASE
      WHEN dict_value = 'SUBMIT' THEN '外围系统提交申请数据，由 Forge 创建业务记录并立即发起主流程'
      WHEN dict_value = 'START' THEN '仅为 Forge 中已经保存的业务记录发起主流程，调用时必须传真实 recordId'
      ELSE remark
    END,
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND dict_type = 'ai_capability_flow_operation'
  AND dict_value IN ('SUBMIT', 'START', 'APPROVE', 'REJECT');
