-- 新建流程的事件通知默认使用 Redis Pub/Sub。
-- 仅调整数据库默认值，不覆盖已有流程显式保存的通知方式。
SET @flow_model_notify_col = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_flow_model'
      AND COLUMN_NAME = 'notify_type'
);

SET @sql = IF(
    @flow_model_notify_col = 1,
    'ALTER TABLE sys_flow_model MODIFY COLUMN notify_type varchar(30) DEFAULT ''redis'' COMMENT ''事件通知方式：none-不通知 / redis-Redis Pub/Sub / webhook-HTTP Webhook（互斥）''',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
