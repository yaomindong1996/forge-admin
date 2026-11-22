-- 恢复「对话记录」侧栏入口（V1.0.238 精简菜单时随 Agent 对话一并隐藏）。
-- Agent 对话页仍保持隐藏，仅由智能体页直达；对话记录作为运营审计入口重新可见。

DECLARE @ai_root_id BIGINT;
SELECT TOP 1 @ai_root_id = id
FROM sys_resource
WHERE tenant_id = 1 AND resource_type = 1 AND path = N'/ai' AND del_flag = 0
ORDER BY id;

UPDATE sys_resource
SET resource_name = N'对话记录',
    parent_id = COALESCE(@ai_root_id, parent_id),
    sort = 6,
    visible = 1,
    menu_status = 1,
    always_show = 0,
    icon = COALESCE(NULLIF(icon, N''), N'ionicons5:ChatboxEllipses'),
    remark = COALESCE(NULLIF(remark, N''), N'查看 AI 会话与对话体验指标'),
    component = CASE
      WHEN component IS NULL OR component = N'' OR component LIKE N'/%' THEN N'ai/session'
      ELSE component
    END,
    update_by = 1,
    update_time = SYSUTCDATETIME()
WHERE tenant_id = 1
  AND client_code = N'pc'
  AND del_flag = 0
  AND path = N'/ai/session';
