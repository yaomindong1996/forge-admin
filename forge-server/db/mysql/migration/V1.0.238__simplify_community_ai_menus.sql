-- =============================================================================
-- 社区版 AI 能力菜单精简与权限补齐：
-- 1) 「AI能力」（/ai）下收口为 5 个一级可见入口：
--    1. 智能体（/ai/agent）
--    2. 知识库（/ai/knowledge/list）
--    3. 资源中心（/ai/agent-tool）—— 内含工具目录 / MCP服务 / 技能资料
--    4. 提示词模板（/ai/prompt-template）
--    5. 模型与服务配置（/ai/provider-model）—— 内含供应商与模型 / 向量存储 / 检索默认值
-- 2) 隐藏碎片/已下线/404 脏菜单（保留资源行不断裂历史权限引用）。
-- 3) 将「大屏生成记录」归位至「数据与报表」目录下。
-- 4) 补齐 MCP、SkillPackage、全局 AI 助手权限码并授予已拥有 AI 入口的角色。
-- =============================================================================

SET @ai_root_id := (
  SELECT id FROM sys_resource
  WHERE tenant_id = 1 AND resource_type = 1 AND path = '/ai' AND del_flag = 0
  ORDER BY id LIMIT 1
);

SET @data_report_root_id := (
  SELECT id FROM sys_resource
  WHERE tenant_id = 1 AND resource_type = 1 AND path = '/data-report' AND del_flag = 0
  ORDER BY id LIMIT 1
);

-- 1) 补齐 /ai 目录默认重定向到 /ai/agent
UPDATE sys_resource
SET redirect = '/ai/agent',
    icon = 'ionicons5:SparklesOutline',
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND id = @ai_root_id
  AND del_flag = 0;

-- 2) 规范 5 个核心可见菜单名称、父级、顺序与图标
UPDATE sys_resource
SET resource_name = '智能体',
    parent_id = COALESCE(@ai_root_id, parent_id),
    sort = 1,
    visible = 1,
    menu_status = 1,
    icon = 'ionicons5:SparklesOutline',
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1 AND path = '/ai/agent' AND del_flag = 0;

UPDATE sys_resource
SET resource_name = '知识库',
    parent_id = COALESCE(@ai_root_id, parent_id),
    sort = 2,
    visible = 1,
    menu_status = 1,
    icon = 'ionicons5:LibraryOutline',
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1 AND path = '/ai/knowledge/list' AND del_flag = 0;

UPDATE sys_resource
SET resource_name = '资源中心',
    parent_id = COALESCE(@ai_root_id, parent_id),
    sort = 3,
    visible = 1,
    menu_status = 1,
    icon = 'ionicons5:ConstructOutline',
    remark = '统一管理平台内置工具、MCP远程工具与技能资料包',
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1 AND path = '/ai/agent-tool' AND del_flag = 0;

UPDATE sys_resource
SET resource_name = '提示词模板',
    parent_id = COALESCE(@ai_root_id, parent_id),
    sort = 4,
    visible = 1,
    menu_status = 1,
    icon = 'ionicons5:DocumentTextOutline',
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1 AND path = '/ai/prompt-template' AND del_flag = 0;

UPDATE sys_resource
SET resource_name = '模型与服务配置',
    parent_id = COALESCE(@ai_root_id, parent_id),
    sort = 5,
    visible = 1,
    menu_status = 1,
    icon = 'ionicons5:SettingsOutline',
    remark = '统一配置AI供应商与模型（OpenAI兼容）、向量存储与知识检索默认值',
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1 AND path = '/ai/provider-model' AND del_flag = 0;

-- 3) 隐藏原「Agent对话」和「会话记录」侧栏入口（保留 menu_status=1 供智能体页直达跳转）
UPDATE sys_resource
SET visible = 0,
    always_show = 0,
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND client_code = 'pc'
  AND del_flag = 0
  AND path IN ('/ai/agent-chat', '/ai/agent/chat', '/ai/session');

-- 4) 隐藏并停用冗余碎片、已下线多模态/AI向导与 404 脏菜单
UPDATE sys_resource
SET visible = 0,
    menu_status = 0,
    always_show = 0,
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND client_code = 'pc'
  AND del_flag = 0
  AND (
      path IN (
          '/ai/knowledge',
          '/ai/store-instance/store',
          '/ai/knowledge/store',
          '/ai/skill',
          '/ai/agent-workspace',
          '/ai/agent-workspace/skill',
          '/ai/model-routing',
          '/ai/ai-tools',
          '/ai/ai-tools/image-generate',
          '/ai/ai-tools/voice',
          '/ai/agent-create',
          '/ai/agent-workspace/ai-create',
          '/ai/lowcode-domain/general'
      )
      OR resource_name IN ('AI工具', '图片生成', '语音设置', 'AI创建Agent', '模型治理', '通用业务域')
  );

-- 5) 将「大屏生成记录」移归「数据与报表」目录（不再占用 AI 能力目录）
UPDATE sys_resource
SET parent_id = @data_report_root_id,
    sort = 20,
    update_by = 1,
    update_time = NOW()
WHERE @data_report_root_id IS NOT NULL
  AND tenant_id = 1
  AND client_code = 'pc'
  AND del_flag = 0
  AND (path LIKE '/ai/dashboard%' OR resource_name = '大屏生成记录');

-- 6) 在「资源中心」下补齐 MCP 与 SkillPackage 权限码，在「智能体」下补齐全局 AI 助手权限码
SET @resource_center_id := (
  SELECT id FROM sys_resource
  WHERE tenant_id = 1 AND path = '/ai/agent-tool' AND del_flag = 0
  ORDER BY id LIMIT 1
);

SET @agent_menu_id := (
  SELECT id FROM sys_resource
  WHERE tenant_id = 1 AND path = '/ai/agent' AND del_flag = 0
  ORDER BY id LIMIT 1
);

INSERT INTO sys_resource (
  tenant_id, resource_name, parent_id, resource_type, sort,
  path, component, is_external, sso_enabled, sso_target_client,
  open_target, is_public, menu_status, visible, perms, icon,
  api_method, api_url, keep_alive, always_show, redirect, remark,
  create_by, create_time, update_by, update_time, create_dept, client_code
)
SELECT 1, seed.resource_name, @resource_center_id, 3, seed.sort,
       NULL, NULL, 0, 0, NULL, '_self', 0, 1, 1, seed.perms, NULL,
       NULL, NULL, 0, 0, NULL, seed.remark,
       1, NOW(), 1, NOW(), 1, 'pc'
FROM (
  SELECT '查看MCP服务' resource_name, 10 sort, 'ai:mcp:connection:list' perms, '查看MCP服务连接与工具目录' remark
  UNION ALL SELECT '新增MCP服务', 11, 'ai:mcp:connection:add', '新增MCP远程服务连接'
  UNION ALL SELECT '编辑MCP服务', 12, 'ai:mcp:connection:edit', '编辑MCP远程服务连接'
  UNION ALL SELECT '删除MCP服务', 13, 'ai:mcp:connection:delete', '删除MCP远程服务连接'
  UNION ALL SELECT '同步MCP目录', 14, 'ai:mcp:connection:discover', '测试MCP连接并同步工具目录'
  UNION ALL SELECT '查看技能资料', 20, 'ai:skill:package:query', '查询技能资料包列表与文件详情'
  UNION ALL SELECT '导入技能资料', 21, 'ai:skill:package:import', '预览并导入ZIP技能资料包'
  UNION ALL SELECT '启停技能资料', 22, 'ai:skill:package:activate', '启用或停用技能资料包固定版本'
  UNION ALL SELECT '绑定技能资料', 23, 'ai:skill:package:bind', '将技能资料包绑定至智能体'
) seed
WHERE @resource_center_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_resource r
    WHERE r.tenant_id = 1 AND r.resource_type = 3 AND r.perms = seed.perms AND r.del_flag = 0
  );

INSERT INTO sys_resource (
  tenant_id, resource_name, parent_id, resource_type, sort,
  path, component, is_external, sso_enabled, sso_target_client,
  open_target, is_public, menu_status, visible, perms, icon,
  api_method, api_url, keep_alive, always_show, redirect, remark,
  create_by, create_time, update_by, update_time, create_dept, client_code
)
SELECT 1, '使用全局AI助手', @agent_menu_id, 3, 20,
       NULL, NULL, 0, 0, NULL, '_self', 0, 1, 1, 'ai:hub:use', NULL,
       NULL, NULL, 0, 0, NULL, '使用全站悬浮AI助手进行对话路由与待办查询',
       1, NOW(), 1, NOW(), 1, 'pc'
WHERE @agent_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_resource r
    WHERE r.tenant_id = 1 AND r.resource_type = 3 AND r.perms = 'ai:hub:use' AND r.del_flag = 0
  );

-- 7) 已有 AI 能力或智能体菜单权限的角色同步获得新权限码
INSERT INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT DISTINCT seed.tenant_id, seed.role_id, target.id, NOW()
FROM (
  SELECT rr.tenant_id, rr.role_id
  FROM sys_role_resource rr
  INNER JOIN sys_resource r
    ON r.id = rr.resource_id
   AND r.tenant_id = rr.tenant_id
   AND r.del_flag = 0
   AND r.path IN ('/ai', '/ai/agent', '/ai/agent-tool', '/ai/provider-model')
) seed
INNER JOIN sys_resource target
  ON target.tenant_id = seed.tenant_id
 AND target.del_flag = 0
 AND target.perms IN (
   'ai:mcp:connection:list', 'ai:mcp:connection:add', 'ai:mcp:connection:edit',
   'ai:mcp:connection:delete', 'ai:mcp:connection:discover',
   'ai:skill:package:query', 'ai:skill:package:import',
   'ai:skill:package:activate', 'ai:skill:package:bind',
   'ai:hub:use'
 )
WHERE NOT EXISTS (
  SELECT 1 FROM sys_role_resource existing
  WHERE existing.tenant_id = seed.tenant_id
    AND existing.role_id = seed.role_id
    AND existing.resource_id = target.id
);
