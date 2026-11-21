-- 社区版 AI 能力菜单精简与权限补齐（SQL Server，与 MySQL/PG V1.0.238 同语义）
SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;
GO

UPDATE sys_resource
SET redirect = N'/ai/agent',
    icon = N'ionicons5:SparklesOutline',
    update_by = 1,
    update_time = SYSUTCDATETIME()
WHERE tenant_id = 1
  AND resource_type = 1
  AND path = N'/ai'
  AND del_flag = 0;
GO

DECLARE @ai_menu_id BIGINT;
SELECT TOP 1 @ai_menu_id = id
FROM sys_resource
WHERE tenant_id = 1 AND resource_type = 1 AND path = N'/ai' AND del_flag = 0
ORDER BY id;

UPDATE sys_resource
SET resource_name = N'智能体',
    parent_id = COALESCE(@ai_menu_id, parent_id),
    sort = 1,
    visible = 1,
    menu_status = 1,
    icon = N'ionicons5:SparklesOutline',
    update_by = 1,
    update_time = SYSUTCDATETIME()
WHERE tenant_id = 1 AND path = N'/ai/agent' AND del_flag = 0;

UPDATE sys_resource
SET resource_name = N'知识库',
    parent_id = COALESCE(@ai_menu_id, parent_id),
    sort = 2,
    visible = 1,
    menu_status = 1,
    icon = N'ionicons5:LibraryOutline',
    update_by = 1,
    update_time = SYSUTCDATETIME()
WHERE tenant_id = 1 AND path = N'/ai/knowledge/list' AND del_flag = 0;

UPDATE sys_resource
SET resource_name = N'资源中心',
    parent_id = COALESCE(@ai_menu_id, parent_id),
    sort = 3,
    visible = 1,
    menu_status = 1,
    icon = N'ionicons5:ConstructOutline',
    remark = N'统一管理平台内置工具、MCP远程工具与技能资料包',
    update_by = 1,
    update_time = SYSUTCDATETIME()
WHERE tenant_id = 1 AND path = N'/ai/agent-tool' AND del_flag = 0;

UPDATE sys_resource
SET resource_name = N'提示词模板',
    parent_id = COALESCE(@ai_menu_id, parent_id),
    sort = 4,
    visible = 1,
    menu_status = 1,
    icon = N'ionicons5:DocumentTextOutline',
    update_by = 1,
    update_time = SYSUTCDATETIME()
WHERE tenant_id = 1 AND path = N'/ai/prompt-template' AND del_flag = 0;

UPDATE sys_resource
SET resource_name = N'模型与服务配置',
    parent_id = COALESCE(@ai_menu_id, parent_id),
    sort = 5,
    visible = 1,
    menu_status = 1,
    icon = N'ionicons5:SettingsOutline',
    remark = N'统一配置AI供应商与模型（OpenAI兼容）、向量存储与知识检索默认值',
    update_by = 1,
    update_time = SYSUTCDATETIME()
WHERE tenant_id = 1 AND path = N'/ai/provider-model' AND del_flag = 0;
GO

UPDATE sys_resource
SET visible = 0,
    always_show = 0,
    update_by = 1,
    update_time = SYSUTCDATETIME()
WHERE tenant_id = 1
  AND client_code = N'pc'
  AND del_flag = 0
  AND path IN (N'/ai/agent-chat', N'/ai/agent/chat', N'/ai/session');
GO

UPDATE sys_resource
SET visible = 0,
    menu_status = 0,
    always_show = 0,
    update_by = 1,
    update_time = SYSUTCDATETIME()
WHERE tenant_id = 1
  AND client_code = N'pc'
  AND del_flag = 0
  AND (
      path IN (
          N'/ai/knowledge',
          N'/ai/store-instance/store',
          N'/ai/knowledge/store',
          N'/ai/skill',
          N'/ai/agent-workspace',
          N'/ai/agent-workspace/skill',
          N'/ai/model-routing',
          N'/ai/ai-tools',
          N'/ai/ai-tools/image-generate',
          N'/ai/ai-tools/voice',
          N'/ai/agent-create',
          N'/ai/agent-workspace/ai-create',
          N'/ai/lowcode-domain/general'
      )
      OR resource_name IN (N'AI工具', N'图片生成', N'语音设置', N'AI创建Agent', N'模型治理', N'通用业务域')
  );
GO

DECLARE @data_report_id BIGINT;
SELECT TOP 1 @data_report_id = id
FROM sys_resource
WHERE tenant_id = 1 AND resource_type = 1 AND path = N'/data-report' AND del_flag = 0
ORDER BY id;

IF @data_report_id IS NOT NULL
BEGIN
    UPDATE sys_resource
    SET parent_id = @data_report_id,
        sort = 20,
        update_by = 1,
        update_time = SYSUTCDATETIME()
    WHERE tenant_id = 1
      AND client_code = N'pc'
      AND del_flag = 0
      AND (path LIKE N'/ai/dashboard%' OR resource_name = N'大屏生成记录');
END
GO

INSERT INTO sys_resource (
  tenant_id, resource_name, parent_id, resource_type, sort,
  path, component, is_external, sso_enabled, sso_target_client,
  open_target, is_public, menu_status, visible, perms, icon,
  api_method, api_url, keep_alive, always_show, redirect, remark,
  create_by, create_time, update_by, update_time, create_dept, client_code
)
SELECT 1, seed.resource_name, menu.id, 3, seed.sort,
       NULL, NULL, 0, 0, NULL, N'_self', 0, 1, 1, seed.perms, NULL,
       NULL, NULL, 0, 0, NULL, seed.remark,
       1, SYSUTCDATETIME(), 1, SYSUTCDATETIME(), 1, N'pc'
FROM (
  SELECT N'查看MCP服务' AS resource_name, 10 AS sort, N'ai:mcp:connection:list' AS perms, N'查看MCP服务连接与工具目录' AS remark
  UNION ALL SELECT N'新增MCP服务', 11, N'ai:mcp:connection:add', N'新增MCP远程服务连接'
  UNION ALL SELECT N'编辑MCP服务', 12, N'ai:mcp:connection:edit', N'编辑MCP远程服务连接'
  UNION ALL SELECT N'删除MCP服务', 13, N'ai:mcp:connection:delete', N'删除MCP远程服务连接'
  UNION ALL SELECT N'同步MCP目录', 14, N'ai:mcp:connection:discover', N'测试MCP连接并同步工具目录'
  UNION ALL SELECT N'查看技能资料', 20, N'ai:skill:package:query', N'查询技能资料包列表与文件详情'
  UNION ALL SELECT N'导入技能资料', 21, N'ai:skill:package:import', N'预览并导入ZIP技能资料包'
  UNION ALL SELECT N'启停技能资料', 22, N'ai:skill:package:activate', N'启用或停用技能资料包固定版本'
  UNION ALL SELECT N'绑定技能资料', 23, N'ai:skill:package:bind', N'将技能资料包绑定至智能体'
) seed
INNER JOIN (
  SELECT TOP 1 id FROM sys_resource
  WHERE tenant_id = 1 AND path = N'/ai/agent-tool' AND del_flag = 0
  ORDER BY id
) menu ON 1 = 1
WHERE NOT EXISTS (
  SELECT 1 FROM sys_resource r
  WHERE r.tenant_id = 1 AND r.resource_type = 3 AND r.perms = seed.perms AND r.del_flag = 0
);
GO

INSERT INTO sys_resource (
  tenant_id, resource_name, parent_id, resource_type, sort,
  path, component, is_external, sso_enabled, sso_target_client,
  open_target, is_public, menu_status, visible, perms, icon,
  api_method, api_url, keep_alive, always_show, redirect, remark,
  create_by, create_time, update_by, update_time, create_dept, client_code
)
SELECT 1, N'使用全局AI助手', menu.id, 3, 20,
       NULL, NULL, 0, 0, NULL, N'_self', 0, 1, 1, N'ai:hub:use', NULL,
       NULL, NULL, 0, 0, NULL, N'使用全站悬浮AI助手进行对话路由与待办查询',
       1, SYSUTCDATETIME(), 1, SYSUTCDATETIME(), 1, N'pc'
FROM (
  SELECT TOP 1 id FROM sys_resource
  WHERE tenant_id = 1 AND path = N'/ai/agent' AND del_flag = 0
  ORDER BY id
) menu
WHERE NOT EXISTS (
  SELECT 1 FROM sys_resource r
  WHERE r.tenant_id = 1 AND r.resource_type = 3 AND r.perms = N'ai:hub:use' AND r.del_flag = 0
);
GO

INSERT INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT DISTINCT seed.tenant_id, seed.role_id, target.id, SYSUTCDATETIME()
FROM (
  SELECT rr.tenant_id, rr.role_id
  FROM sys_role_resource rr
  INNER JOIN sys_resource r
    ON r.id = rr.resource_id
   AND r.tenant_id = rr.tenant_id
   AND r.del_flag = 0
   AND r.path IN (N'/ai', N'/ai/agent', N'/ai/agent-tool', N'/ai/provider-model')
) seed
INNER JOIN sys_resource target
  ON target.tenant_id = seed.tenant_id
 AND target.del_flag = 0
 AND target.perms IN (
   N'ai:mcp:connection:list', N'ai:mcp:connection:add', N'ai:mcp:connection:edit',
   N'ai:mcp:connection:delete', N'ai:mcp:connection:discover',
   N'ai:skill:package:query', N'ai:skill:package:import',
   N'ai:skill:package:activate', N'ai:skill:package:bind',
   N'ai:hub:use'
 )
WHERE NOT EXISTS (
  SELECT 1 FROM sys_role_resource existing
  WHERE existing.tenant_id = seed.tenant_id
    AND existing.role_id = seed.role_id
    AND existing.resource_id = target.id
);
GO
