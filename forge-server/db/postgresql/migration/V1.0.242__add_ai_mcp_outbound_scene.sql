-- AI MCP Streamable HTTP 出站场景：供超级管理员在 MCP 页一键写入精确源白名单。

INSERT INTO sys_dict_data (
  tenant_id, dict_sort, dict_label, dict_value, dict_type,
  css_class, list_class, is_default, dict_status, remark,
  create_by, create_time, update_by, update_time, create_dept
)
SELECT 1, 5, 'AI MCP 服务', 'AI_MCP', 'sys_outbound_scene',
       NULL, 'info', 'N', 1, 'AI MCP Streamable HTTP 出站场景，禁止私网例外；可由超级管理员在 MCP 页一键放行',
       1, NOW(), 1, NOW(), 1
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_dict_data data
  WHERE data.tenant_id = 1
    AND data.dict_type = 'sys_outbound_scene'
    AND data.dict_value = 'AI_MCP'
);
