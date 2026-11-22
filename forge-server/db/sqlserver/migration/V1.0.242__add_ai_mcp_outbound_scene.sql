-- AI MCP Streamable HTTP 出站场景：供超级管理员在 MCP 页一键写入精确源白名单。

IF NOT EXISTS (
  SELECT 1
  FROM sys_dict_data
  WHERE tenant_id = 1
    AND dict_type = N'sys_outbound_scene'
    AND dict_value = N'AI_MCP'
)
BEGIN
  INSERT INTO sys_dict_data (
    tenant_id, dict_sort, dict_label, dict_value, dict_type,
    css_class, list_class, is_default, dict_status, remark,
    create_by, create_time, update_by, update_time, create_dept
  )
  VALUES (
    1, 5, N'AI MCP 服务', N'AI_MCP', N'sys_outbound_scene',
    NULL, N'info', N'N', 1, N'AI MCP Streamable HTTP 出站场景，禁止私网例外；可由超级管理员在 MCP 页一键放行',
    1, GETDATE(), 1, GETDATE(), 1
  );
END;
