-- 低代码业务流程独立状态字典，由平台托管的 flowStatus 字段使用。

INSERT INTO sys_dict_type (
  tenant_id, dict_name, dict_type, dict_status, remark,
  create_by, create_time, update_by, update_time, create_dept
)
SELECT 1, '业务流程状态', 'business_flow_status', 1,
       '平台托管的业务审批流程状态，独立于业务状态',
       1, NOW(), 1, NOW(), 1
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_dict_type existing
  WHERE existing.tenant_id = 1
    AND existing.dict_type = 'business_flow_status'
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
  SELECT 1 tenant_id, 1 dict_sort, '草稿' dict_label, 'DRAFT' dict_value,
         'business_flow_status' dict_type, 'default' list_class, 'Y' is_default,
         '记录尚未发起审批' remark
  UNION ALL SELECT 1, 2, '审批中', 'IN_PROCESS', 'business_flow_status', 'warning', 'N', '审批流程正在运行'
  UNION ALL SELECT 1, 3, '已通过', 'APPROVED', 'business_flow_status', 'success', 'N', '审批流程已通过'
  UNION ALL SELECT 1, 4, '已驳回', 'REJECTED', 'business_flow_status', 'error', 'N', '审批流程已驳回'
  UNION ALL SELECT 1, 5, '已取消', 'CANCELED', 'business_flow_status', 'default', 'N', '审批流程已取消'
) seed
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_dict_data existing
  WHERE existing.tenant_id = seed.tenant_id
    AND existing.dict_type = seed.dict_type
    AND existing.dict_value = seed.dict_value
);
