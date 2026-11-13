-- 流程逾期提醒运行记录、失败重试字段及节点提醒配置正式迁移。
-- 逾期提醒依赖该表的租户+提醒批次+渠道唯一键实现幂等发送。

CREATE TABLE IF NOT EXISTS sys_flow_overdue_reminder_record (
    id varchar(64) NOT NULL,
    tenant_id bigint NOT NULL DEFAULT 1,
    task_id varchar(64) NOT NULL,
    process_instance_id varchar(64) DEFAULT NULL,
    process_def_key varchar(100) DEFAULT NULL,
    task_def_key varchar(100) DEFAULT NULL,
    reminder_key varchar(160) NOT NULL,
    channel varchar(20) NOT NULL,
    template_code varchar(50) DEFAULT NULL,
    receiver_user_ids text,
    message_id bigint DEFAULT NULL,
    send_status tinyint NOT NULL DEFAULT 0,
    send_time datetime DEFAULT NULL,
    error_message varchar(1000) DEFAULT NULL,
    retry_count int NOT NULL DEFAULT 0,
    next_retry_time datetime DEFAULT NULL,
    create_by bigint DEFAULT NULL,
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_dept bigint DEFAULT NULL,
    update_by bigint DEFAULT NULL,
    update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_flow_overdue_reminder (tenant_id, reminder_key, channel),
    KEY idx_flow_overdue_task (tenant_id, task_id, send_time),
    KEY idx_flow_overdue_status (tenant_id, send_status, next_retry_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程任务逾期提醒记录';

SET @index_exists := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_flow_overdue_reminder_record'
      AND INDEX_NAME = 'uk_flow_overdue_reminder'
);
SET @ddl := IF(@index_exists = 0,
    'ALTER TABLE sys_flow_overdue_reminder_record ADD UNIQUE KEY uk_flow_overdue_reminder (tenant_id, reminder_key, channel)',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @table_exists := (
    SELECT COUNT(*) FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_node_config'
);

SET @column_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_node_config'
      AND COLUMN_NAME = 'overdue_reminder_enabled'
);
SET @ddl := IF(@table_exists > 0 AND @column_exists = 0,
    'ALTER TABLE sys_flow_node_config ADD COLUMN overdue_reminder_enabled tinyint NOT NULL DEFAULT 0 COMMENT ''是否启用逾期提醒''',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_node_config'
      AND COLUMN_NAME = 'overdue_reminder_template_code'
);
SET @ddl := IF(@table_exists > 0 AND @column_exists = 0,
    'ALTER TABLE sys_flow_node_config ADD COLUMN overdue_reminder_template_code varchar(50) DEFAULT NULL COMMENT ''逾期提醒消息模板编码''',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_node_config'
      AND COLUMN_NAME = 'overdue_reminder_channels'
);
SET @ddl := IF(@table_exists > 0 AND @column_exists = 0,
    'ALTER TABLE sys_flow_node_config ADD COLUMN overdue_reminder_channels varchar(200) DEFAULT NULL COMMENT ''逾期提醒推送渠道''',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_node_config'
      AND COLUMN_NAME = 'overdue_reminder_repeat_mode'
);
SET @ddl := IF(@table_exists > 0 AND @column_exists = 0,
    'ALTER TABLE sys_flow_node_config ADD COLUMN overdue_reminder_repeat_mode varchar(32) NOT NULL DEFAULT ''once'' COMMENT ''逾期提醒重复策略''',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_node_config'
      AND COLUMN_NAME = 'overdue_reminder_interval_minutes'
);
SET @ddl := IF(@table_exists > 0 AND @column_exists = 0,
    'ALTER TABLE sys_flow_node_config ADD COLUMN overdue_reminder_interval_minutes int NOT NULL DEFAULT 1440 COMMENT ''重复提醒间隔分钟''',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_node_config'
      AND COLUMN_NAME = 'overdue_reminder_max_times'
);
SET @ddl := IF(@table_exists > 0 AND @column_exists = 0,
    'ALTER TABLE sys_flow_node_config ADD COLUMN overdue_reminder_max_times int NOT NULL DEFAULT 1 COMMENT ''最大提醒次数''',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_overdue_reminder_record'
      AND COLUMN_NAME = 'retry_count'
);
SET @ddl := IF(@column_exists = 0,
    'ALTER TABLE sys_flow_overdue_reminder_record ADD COLUMN retry_count int NOT NULL DEFAULT 0 AFTER error_message',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_overdue_reminder_record'
      AND COLUMN_NAME = 'next_retry_time'
);
SET @ddl := IF(@column_exists = 0,
    'ALTER TABLE sys_flow_overdue_reminder_record ADD COLUMN next_retry_time datetime DEFAULT NULL AFTER retry_count',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

INSERT INTO sys_dict_type (
    tenant_id, dict_name, dict_type, dict_status, remark,
    create_by, create_time, update_by, update_time, create_dept
)
SELECT 1, '流程逾期提醒重复策略', 'sys_flow_overdue_repeat_mode', 1,
       '审批任务逾期提醒重复发送策略', 1, NOW(), 1, NOW(), 1
WHERE NOT EXISTS (
    SELECT 1 FROM sys_dict_type
    WHERE tenant_id = 1 AND dict_type = 'sys_flow_overdue_repeat_mode'
);

INSERT INTO sys_dict_data (
    tenant_id, dict_sort, dict_label, dict_value, dict_type,
    css_class, list_class, is_default, dict_status, remark,
    create_by, create_time, update_by, update_time, create_dept
)
SELECT seed.tenant_id, seed.dict_sort, seed.dict_label, seed.dict_value,
       'sys_flow_overdue_repeat_mode', NULL, seed.list_class, seed.is_default,
       1, seed.remark, 1, NOW(), 1, NOW(), 1
FROM (
    SELECT 1 tenant_id, 1 dict_sort, '仅一次' dict_label, 'once' dict_value,
           'info' list_class, 'Y' is_default, '逾期后仅提醒一次' remark
    UNION ALL
    SELECT 1, 2, '按间隔重复', 'interval', 'warning', 'N', '逾期后按配置间隔重复提醒'
) seed
WHERE NOT EXISTS (
    SELECT 1 FROM sys_dict_data d
    WHERE d.tenant_id = seed.tenant_id
      AND d.dict_type = 'sys_flow_overdue_repeat_mode'
      AND d.dict_value = seed.dict_value
);

INSERT INTO sys_message_template (
    tenant_id, template_code, template_name, type, title_template,
    content_template, default_channel, enabled, remark, create_by, del_flag
)
SELECT 1, 'FLOW_TASK_OVERDUE', '流程任务逾期提醒', 'SYSTEM', '流程任务逾期提醒',
       CONCAT('流程「', '$', '{processName}」的任务「', '$', '{taskName}」已逾期，截止时间：',
              '$', '{dueDate}，逾期 ', '$', '{overdueMinutes} 分钟。请及时处理：', '$', '{jumpUrl}'),
       'WEB', 1, '流程审批任务逾期后提醒当前审批人', NULL, 0
WHERE NOT EXISTS (
    SELECT 1 FROM sys_message_template
    WHERE tenant_id = 1 AND template_code = 'FLOW_TASK_OVERDUE' AND del_flag = 0
);
