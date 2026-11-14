-- 企业协同增强：连接级定时目录同步（需求3）+ 流程待办深链模型覆盖（需求4）+ 待办卡片可配置消息模板（需求5）
-- 全部变更具备 information_schema / NOT EXISTS 防重复保护，内置数据 tenant_id 固定为 1

-- ============================================================
-- 需求3：连接管理配置定时目录同步（保存连接时自动维护对应 sys_job_config）
-- ============================================================
SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_social_config' AND COLUMN_NAME = 'sync_schedule_enabled');
SET @sql = IF(@col = 0, 'ALTER TABLE sys_social_config ADD COLUMN sync_schedule_enabled tinyint NOT NULL DEFAULT 0 COMMENT ''定时目录同步开关：1开启 0关闭，开启后由连接管理自动维护定时任务'' AFTER todo_push_h5_url', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_social_config' AND COLUMN_NAME = 'sync_cron');
SET @sql = IF(@col = 0, 'ALTER TABLE sys_social_config ADD COLUMN sync_cron varchar(64) DEFAULT NULL COMMENT ''定时目录同步 Cron 表达式：sync_schedule_enabled=1 时生效'' AFTER sync_schedule_enabled', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================================
-- 需求4：流程模型可覆盖待办卡片跳转深链（为空时使用全局默认待办详情页）
-- ============================================================
SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_model' AND COLUMN_NAME = 'todo_detail_url_template');
SET @sql = IF(@col = 0, 'ALTER TABLE sys_flow_model ADD COLUMN todo_detail_url_template varchar(512) DEFAULT NULL COMMENT ''待办卡片详情深链模板，支持占位符 {taskId}/{businessKey}/{processInstanceId}，可填相对路径或完整URL'' AFTER webhook_url', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================================
-- 需求5：待办卡片改走可配置消息模板（平台差异化 FLOW_TODO_CARD_{platform}，回退通用 FLOW_TODO_CARD）
-- 内置通用模板与企微/钉钉/飞书差异化模板，均可在「消息中心 - 模板管理」按需编辑或停用
-- content_template 为企业协同 textcard.description 支持的有限 HTML（gray/normal/highlight 三种颜色 div）
-- ============================================================

-- 通用待办卡片模板（默认启用，作为所有平台的兜底）
INSERT INTO sys_message_template
    (tenant_id, template_code, template_name, type, title_template, content_template, default_channel, enabled, remark, create_by, del_flag)
SELECT 1, 'FLOW_TODO_CARD', '流程待办卡片（通用）', 'SYSTEM', '您有新的流程待办',
       '<div class="gray">流程待办提醒</div><div class="normal">任务：${taskTitle}</div><div class="normal">流程：${processName}</div><div class="normal">发起人：${startUserName}</div><div class="highlight">点击卡片查看详情并办理 ›</div>',
       'COLLABORATION', 1, '流程待办企业协同卡片通用模板，支持占位符：taskTitle/processName/startUserName', NULL, 0
WHERE NOT EXISTS (
    SELECT 1 FROM (SELECT template_code FROM sys_message_template WHERE tenant_id = 1 AND template_code = 'FLOW_TODO_CARD' AND del_flag = 0) t
);

-- 企业微信差异化模板（默认停用，需要平台差异化文案时启用；启用后覆盖通用模板）
INSERT INTO sys_message_template
    (tenant_id, template_code, template_name, type, title_template, content_template, default_channel, enabled, remark, create_by, del_flag)
SELECT 1, 'FLOW_TODO_CARD_WECOM', '流程待办卡片（企业微信）', 'SYSTEM', '您有新的流程待办',
       '<div class="gray">流程待办提醒</div><div class="normal">任务：${taskTitle}</div><div class="normal">流程：${processName}</div><div class="normal">发起人：${startUserName}</div><div class="highlight">点击卡片查看详情并办理 ›</div>',
       'COLLABORATION', 0, '企业微信平台差异化待办卡片模板，启用后覆盖 FLOW_TODO_CARD', NULL, 0
WHERE NOT EXISTS (
    SELECT 1 FROM (SELECT template_code FROM sys_message_template WHERE tenant_id = 1 AND template_code = 'FLOW_TODO_CARD_WECOM' AND del_flag = 0) t
);

-- 钉钉差异化模板（默认停用，后续对接钉钉时启用）
INSERT INTO sys_message_template
    (tenant_id, template_code, template_name, type, title_template, content_template, default_channel, enabled, remark, create_by, del_flag)
SELECT 1, 'FLOW_TODO_CARD_DINGTALK', '流程待办卡片（钉钉）', 'SYSTEM', '您有新的流程待办',
       '### 流程待办提醒\n\n- 任务：${taskTitle}\n- 流程：${processName}\n- 发起人：${startUserName}\n\n[点击查看详情并办理](${url})',
       'COLLABORATION', 0, '钉钉平台差异化待办卡片模板（markdown），启用后覆盖 FLOW_TODO_CARD', NULL, 0
WHERE NOT EXISTS (
    SELECT 1 FROM (SELECT template_code FROM sys_message_template WHERE tenant_id = 1 AND template_code = 'FLOW_TODO_CARD_DINGTALK' AND del_flag = 0) t
);

-- 飞书差异化模板（默认停用，后续对接飞书时启用）
INSERT INTO sys_message_template
    (tenant_id, template_code, template_name, type, title_template, content_template, default_channel, enabled, remark, create_by, del_flag)
SELECT 1, 'FLOW_TODO_CARD_FEISHU', '流程待办卡片（飞书）', 'SYSTEM', '您有新的流程待办',
       '流程待办提醒\n任务：${taskTitle}\n流程：${processName}\n发起人：${startUserName}\n点击卡片查看详情并办理',
       'COLLABORATION', 0, '飞书平台差异化待办卡片模板，启用后覆盖 FLOW_TODO_CARD', NULL, 0
WHERE NOT EXISTS (
    SELECT 1 FROM (SELECT template_code FROM sys_message_template WHERE tenant_id = 1 AND template_code = 'FLOW_TODO_CARD_FEISHU' AND del_flag = 0) t
);
