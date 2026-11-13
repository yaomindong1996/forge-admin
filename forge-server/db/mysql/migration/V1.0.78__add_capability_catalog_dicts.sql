-- 补充能力目录来源类型与行为类型字典，统一管理端中文展示。
-- 内置数据使用默认租户 tenant_id=1，并通过 NOT EXISTS 防止重复写入。

INSERT INTO sys_dict_type (
  tenant_id, dict_name, dict_type, dict_status, remark,
  create_by, create_time, update_by, update_time, create_dept
)
SELECT seed.tenant_id, seed.dict_name, seed.dict_type, 1, seed.remark,
       1, NOW(), 1, NOW(), 1
FROM (
  SELECT 1 tenant_id, 'AI能力来源类型' dict_name, 'ai_capability_source_type' dict_type,
         '能力开放平台受控能力来源类型' remark
  UNION ALL
  SELECT 1, 'AI能力行为类型', 'ai_capability_behavior',
         '能力开放平台调用行为分类'
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM sys_dict_type type
  WHERE type.tenant_id = seed.tenant_id
    AND type.dict_type = seed.dict_type
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
  SELECT 1 tenant_id, 1 dict_sort, '业务动作' dict_label, 'BUSINESS_ACTION' dict_value,
         'ai_capability_source_type' dict_type, 'info' list_class, 'Y' is_default,
         '由低代码业务对象注册的受控业务动作' remark
  UNION ALL
  SELECT 1, 2, '流程动作', 'FLOW_ACTION', 'ai_capability_source_type',
         'warning', 'N', '与业务对象流程绑定关联的受控流程动作'
  UNION ALL
  SELECT 1, 3, '系统服务', 'SYSTEM_SERVICE', 'ai_capability_source_type',
         'success', 'N', '由平台代码显式注册的受控系统服务'
  UNION ALL
  SELECT 1, 1, '只读查询', 'READ_ONLY', 'ai_capability_behavior',
         'info', 'Y', '不产生业务写入的只读能力'
  UNION ALL
  SELECT 1, 2, '执行业务动作', 'ACTION', 'ai_capability_behavior',
         'warning', 'N', '会产生受控业务写入的动作能力'
  UNION ALL
  SELECT 1, 3, '执行流程操作', 'FLOW', 'ai_capability_behavior',
         'success', 'N', '发起或办理流程的受控能力'
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM sys_dict_data data
  WHERE data.tenant_id = seed.tenant_id
    AND data.dict_type = seed.dict_type
    AND data.dict_value = seed.dict_value
);
