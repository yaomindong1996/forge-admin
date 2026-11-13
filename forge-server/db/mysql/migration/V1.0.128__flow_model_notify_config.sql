-- 流程模型通知配置通用化：事件×渠道矩阵（新待办/审批结果/抄送 可勾选 站内信/邮件/短信/企微 等渠道）
-- notify_config 为 JSON 结构，示例：
-- {
--   "todo":   {"channels": ["WEB", "COLLABORATION"], "templateCode": null},
--   "result": {"channels": ["WEB", "EMAIL"]},
--   "cc":     {"channels": ["WEB"]}
-- }
-- 未配置（NULL/空）时保持现状行为：站内信 + 连接开启 todoPushEnabled 时推企微卡片

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_model' AND COLUMN_NAME = 'notify_config');
SET @sql = IF(@col = 0, 'ALTER TABLE sys_flow_model ADD COLUMN notify_config json DEFAULT NULL COMMENT ''通知配置（事件×渠道矩阵）：todo/result/cc -> {channels:[WEB/EMAIL/SMS/COLLABORATION], templateCode:模板编码覆盖}，为空时走默认通知行为'' AFTER todo_detail_url_template', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
