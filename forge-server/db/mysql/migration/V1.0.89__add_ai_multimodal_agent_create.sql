-- 多模态与AI创建Agent表结构
-- V1.0.89: ai_image_generate_record, ai_agent_generate_record + ai_agent扩展列(asr_model_id/tts_model_id) + 字典 + 菜单

-- ============================================================
-- 1. 图片生成记录表
-- ============================================================
CREATE TABLE IF NOT EXISTS `ai_image_generate_record` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL,
  `user_id` bigint DEFAULT NULL COMMENT '用户ID',
  `provider_id` bigint DEFAULT NULL COMMENT '供应商ID',
  `model_id` bigint DEFAULT NULL COMMENT '模型ID',
  `prompt` longtext DEFAULT NULL COMMENT '提示词',
  `negative_prompt` longtext DEFAULT NULL COMMENT '负面提示词',
  `size` varchar(32) DEFAULT '1024x1024' COMMENT '尺寸',
  `result_file_id` bigint DEFAULT NULL COMMENT '生成图片文件ID',
  `status` varchar(32) DEFAULT 'pending' COMMENT '状态(pending/success/failed)',
  `error_msg` varchar(1000) DEFAULT NULL COMMENT '错误信息',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_dept` bigint DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标志(0正常，删除后写主键)',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_user` (`tenant_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI图片生成记录';

-- ============================================================
-- 2. AI创建Agent生成记录表
-- ============================================================
CREATE TABLE IF NOT EXISTS `ai_agent_generate_record` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL,
  `user_id` bigint DEFAULT NULL COMMENT '用户ID',
  `description` longtext NOT NULL COMMENT '用户需求描述',
  `generated_config_json` longtext DEFAULT NULL COMMENT '生成结果(名称/描述/问候语/预设问题/指令/推荐绑定)',
  `status` varchar(32) DEFAULT 'pending' COMMENT '状态(generating/success/failed)',
  `error_msg` varchar(1000) DEFAULT NULL COMMENT '错误信息',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_dept` bigint DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标志(0正常，删除后写主键)',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI创建Agent生成记录';

-- ============================================================
-- 3. ai_agent 扩展列：asr_model_id / tts_model_id（幂等）
-- ============================================================
SET @db = DATABASE();

-- asr_model_id
SET @col = 'asr_model_id';
SET @sql = (SELECT IF(COUNT(*)=0,
  CONCAT('ALTER TABLE ai_agent ADD COLUMN `', @col, '` bigint DEFAULT NULL COMMENT ''语音识别模型ID'' AFTER `tool_group_mode`'),
  'SELECT 1') FROM information_schema.columns WHERE table_schema=@db AND table_name='ai_agent' AND column_name=@col);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- tts_model_id
SET @col = 'tts_model_id';
SET @sql = (SELECT IF(COUNT(*)=0,
  CONCAT('ALTER TABLE ai_agent ADD COLUMN `', @col, '` bigint DEFAULT NULL COMMENT ''语音合成模型ID'' AFTER `asr_model_id`'),
  'SELECT 1') FROM information_schema.columns WHERE table_schema=@db AND table_name='ai_agent' AND column_name=@col);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================================
-- 4. 字典
-- ============================================================
INSERT INTO sys_dict_type (tenant_id, dict_name, dict_type, dict_status, remark, create_by, create_time, update_by, update_time, create_dept)
SELECT seed.tenant_id, seed.dict_name, seed.dict_type, 1, seed.remark, 1, NOW(), 1, NOW(), 1
FROM (
  SELECT 1 tenant_id, 'AI图片生成状态' dict_name, 'ai_image_generate_status' dict_type, '图片生成记录状态' remark
  UNION ALL SELECT 1, 'AI创建Agent状态', 'ai_agent_generate_status', 'AI创建Agent生成状态'
) seed
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type t WHERE t.tenant_id = seed.tenant_id AND t.dict_type = seed.dict_type);

-- 图片生成状态字典数据
INSERT INTO sys_dict_data (tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, dict_status, remark, create_by, create_time, update_by, update_time, create_dept)
SELECT seed.tenant_id, seed.dict_sort, seed.dict_label, seed.dict_value, seed.dict_type, NULL, seed.list_class, seed.is_default, 1, seed.remark, 1, NOW(), 1, NOW(), 1
FROM (
  SELECT 1 tenant_id, 1 dict_sort, '待生成' dict_label, 'pending' dict_value, 'ai_image_generate_status' dict_type, 'default' list_class, 'Y' is_default, '等待生成' remark
  UNION ALL SELECT 1, 2, '生成成功', 'success', 'ai_image_generate_status', 'success', 'N', '生成成功'
  UNION ALL SELECT 1, 3, '生成失败', 'failed', 'ai_image_generate_status', 'error', 'N', '生成失败'
) seed
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data d WHERE d.tenant_id = seed.tenant_id AND d.dict_type = seed.dict_type AND d.dict_value = seed.dict_value);

-- AI创建Agent状态字典数据
INSERT INTO sys_dict_data (tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, dict_status, remark, create_by, create_time, update_by, update_time, create_dept)
SELECT seed.tenant_id, seed.dict_sort, seed.dict_label, seed.dict_value, seed.dict_type, NULL, seed.list_class, seed.is_default, 1, seed.remark, 1, NOW(), 1, NOW(), 1
FROM (
  SELECT 1 tenant_id, 1 dict_sort, '生成中' dict_label, 'generating' dict_value, 'ai_agent_generate_status' dict_type, 'warning' list_class, 'Y' is_default, '正在生成' remark
  UNION ALL SELECT 1, 2, '生成成功', 'success', 'ai_agent_generate_status', 'success', 'N', '生成成功'
  UNION ALL SELECT 1, 3, '生成失败', 'failed', 'ai_agent_generate_status', 'error', 'N', '生成失败'
) seed
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data d WHERE d.tenant_id = seed.tenant_id AND d.dict_type = seed.dict_type AND d.dict_value = seed.dict_value);

-- ============================================================
-- 5. 菜单：AI工具分组（图片生成 / 语音设置）
-- ============================================================

-- AI工具菜单（与Agent工作台同级，挂在AI模块下）
INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external, open_target, is_public, menu_status, visible, perms, icon, keep_alive, always_show, remark, create_by, create_time, update_by, update_time, create_dept, client_code)
SELECT 1, 'AI工具', COALESCE((SELECT parent_id FROM (SELECT parent_id FROM sys_resource WHERE path = '/ai/provider-model' LIMIT 1) x), 0), 1, 6,
       '/ai/ai-tools', NULL, 0, '_self', 0, 1, 1, NULL, 'ionicons5:ColorPaletteOutline', 1, 0,
       'AI多模态工具', 1, NOW(), 1, NOW(), 1, 'pc'
WHERE NOT EXISTS (SELECT 1 FROM sys_resource r WHERE r.tenant_id = 1 AND r.path = '/ai/ai-tools');

-- AI工具子菜单
INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external, open_target, is_public, menu_status, visible, perms, icon, keep_alive, always_show, remark, create_by, create_time, update_by, update_time, create_dept, client_code)
SELECT 1, seed.resource_name, menu.id, seed.resource_type, seed.sort, seed.path, seed.component, 0, '_self', 0, 1, 1, seed.perms, seed.icon, 1, 0, seed.remark, 1, NOW(), 1, NOW(), 1, 'pc'
FROM (SELECT id FROM sys_resource WHERE tenant_id = 1 AND path = '/ai/ai-tools' LIMIT 1) menu
JOIN (
  SELECT '图片生成' resource_name, 2 resource_type, 1 sort, '/ai/ai-tools/image-generate' path, 'ai/image-generate/index' component, 'ai:image:generate' perms, 'ionicons5:ImageOutline' icon, 'AI图片生成' remark
  UNION ALL SELECT '语音设置', 2, 2, '/ai/ai-tools/voice' path, 'ai/voice/index' component, 'ai:voice:config' perms, 'ionicons5:MicOutline' icon, 'ASR/TTS模型配置'
) seed
WHERE NOT EXISTS (SELECT 1 FROM sys_resource r WHERE r.tenant_id = 1 AND r.path = seed.path);

-- 图片生成按钮权限
INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, is_external, open_target, is_public, menu_status, visible, perms, keep_alive, always_show, remark, create_by, create_time, update_by, update_time, create_dept, client_code)
SELECT 1, seed.resource_name, menu.id, 3, seed.sort, 0, '_self', 0, 1, 1, seed.perms, 0, 0, seed.remark, 1, NOW(), 1, NOW(), 1, 'pc'
FROM (SELECT id FROM sys_resource WHERE tenant_id = 1 AND path = '/ai/ai-tools/image-generate' LIMIT 1) menu
JOIN (
  SELECT '生成图片' resource_name, 1 sort, 'ai:image:generate' perms, '生成图片' remark
  UNION ALL SELECT '查看历史', 2, 'ai:image:page', '查看生成历史'
) seed
WHERE NOT EXISTS (SELECT 1 FROM sys_resource r WHERE r.tenant_id = 1 AND r.perms = seed.perms);

-- 语音设置按钮权限
INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, is_external, open_target, is_public, menu_status, visible, perms, keep_alive, always_show, remark, create_by, create_time, update_by, update_time, create_dept, client_code)
SELECT 1, seed.resource_name, menu.id, 3, seed.sort, 0, '_self', 0, 1, 1, seed.perms, 0, 0, seed.remark, 1, NOW(), 1, NOW(), 1, 'pc'
FROM (SELECT id FROM sys_resource WHERE tenant_id = 1 AND path = '/ai/ai-tools/voice' LIMIT 1) menu
JOIN (
  SELECT '语音识别' resource_name, 1 sort, 'ai:voice:asr' perms, 'ASR语音识别' remark
  UNION ALL SELECT '语音合成', 2, 'ai:voice:tts', 'TTS语音合成'
) seed
WHERE NOT EXISTS (SELECT 1 FROM sys_resource r WHERE r.tenant_id = 1 AND r.perms = seed.perms);

-- AI创建Agent菜单（挂在Agent工作台下）
INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external, open_target, is_public, menu_status, visible, perms, icon, keep_alive, always_show, remark, create_by, create_time, update_by, update_time, create_dept, client_code)
SELECT 1, 'AI创建Agent', menu.id, 2, 4, '/ai/agent-workspace/ai-create', 'ai/agent-create/index', 0, '_self', 0, 1, 1, 'ai:agent:ai-create', 'ionicons5:MagicWandOutline', 1, 0,
       'AI流式创建Agent', 1, NOW(), 1, NOW(), 1, 'pc'
FROM (SELECT id FROM sys_resource WHERE tenant_id = 1 AND path = '/ai/agent-workspace' LIMIT 1) menu
WHERE NOT EXISTS (SELECT 1 FROM sys_resource r WHERE r.tenant_id = 1 AND r.path = '/ai/agent-workspace/ai-create');

-- AI创建Agent按钮权限
INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, is_external, open_target, is_public, menu_status, visible, perms, keep_alive, always_show, remark, create_by, create_time, update_by, update_time, create_dept, client_code)
SELECT 1, seed.resource_name, menu.id, 3, seed.sort, 0, '_self', 0, 1, 1, seed.perms, 0, 0, seed.remark, 1, NOW(), 1, NOW(), 1, 'pc'
FROM (SELECT id FROM sys_resource WHERE tenant_id = 1 AND path = '/ai/agent-workspace/ai-create' LIMIT 1) menu
JOIN (
  SELECT 'AI创建' resource_name, 1 sort, 'ai:agent:ai-create' perms, 'AI流式创建Agent' remark
  UNION ALL SELECT '确认创建', 2, 'ai:agent:ai-create:confirm', '确认创建Agent'
) seed
WHERE NOT EXISTS (SELECT 1 FROM sys_resource r WHERE r.tenant_id = 1 AND r.perms = seed.perms);

-- 授予管理员权限
INSERT INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT 1, admin_role.id, resource.id, NOW()
FROM (SELECT id FROM sys_role WHERE tenant_id = 1 AND role_key = 'admin' ORDER BY id LIMIT 1) admin_role
JOIN sys_resource resource ON resource.tenant_id = 1
WHERE resource.client_code = 'pc'
  AND (
    resource.path IN ('/ai/ai-tools', '/ai/ai-tools/image-generate', '/ai/ai-tools/voice', '/ai/agent-workspace/ai-create')
    OR resource.perms IN (
      'ai:image:generate', 'ai:image:page',
      'ai:voice:config', 'ai:voice:asr', 'ai:voice:tts',
      'ai:agent:ai-create', 'ai:agent:ai-create:confirm'
    )
  )
  AND NOT EXISTS (
    SELECT 1
    FROM sys_role_resource existing
    WHERE existing.tenant_id = 1
      AND existing.role_id = admin_role.id
      AND existing.resource_id = resource.id
  );
