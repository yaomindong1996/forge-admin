-- 低代码单据补充「待修改」流程状态：驳回到发起人修改节点时流程仍在运行，
-- 单据需要一个可编辑且可重提的中间态，避免状态停留在「审批中」导致发起人无法操作。

INSERT INTO sys_dict_data (
  tenant_id, dict_sort, dict_label, dict_value, dict_type,
  css_class, list_class, is_default, dict_status, remark,
  create_by, create_time, update_by, update_time, create_dept
)
SELECT seed.tenant_id, seed.dict_sort, seed.dict_label, seed.dict_value, seed.dict_type,
       NULL, seed.list_class, seed.is_default, 1, seed.remark,
       1, NOW(), 1, NOW(), 1
FROM (
  SELECT 1 tenant_id, 6 dict_sort, '待修改' dict_label, 'NEED_MODIFY' dict_value,
         'business_flow_status' dict_type, 'warning' list_class, 'N' is_default,
         '已驳回至发起人修改节点，流程仍在运行，可修改后重新提交' remark
) seed
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_dict_data existing
  WHERE existing.tenant_id = seed.tenant_id
    AND existing.dict_type = seed.dict_type
    AND existing.dict_value = seed.dict_value
);
