-- 外部连接器统一接入受控出站策略，仅增加场景字典，不预置任何目标域名或凭据。

INSERT INTO sys_dict_data (
  tenant_id, dict_sort, dict_label, dict_value, dict_type,
  css_class, list_class, is_default, dict_status, remark,
  create_by, create_time, update_by, update_time, create_dept
)
SELECT 1, 4, '外部连接器', 'EXTERNAL_CONNECTOR', 'sys_outbound_scene',
       NULL, 'info', 'N', 1, '低代码外部接口连接器出站场景，禁止私网例外',
       1, NOW(), 1, NOW(), 1
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_dict_data data
  WHERE data.tenant_id = 1
    AND data.dict_type = 'sys_outbound_scene'
    AND data.dict_value = 'EXTERNAL_CONNECTOR'
);
