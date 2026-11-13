-- Flowable 任务、候选人、业务状态和表单状态的可靠本地镜像投影。
-- Outbox 属于运行时中间表，不使用逻辑删除；成功记录由留存任务物理归档。
-- 回滚：先关闭 forge.flow.projection-outbox.enabled 并确认无未完成记录，再删除 Outbox 表；
--       projection_* 字段为兼容性元数据，可保留，也可在停机确认后删除。

CREATE TABLE IF NOT EXISTS sys_flow_projection_outbox (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '全局投影顺序号',
    tenant_id bigint NOT NULL COMMENT '业务租户ID',
    event_id varchar(64) NOT NULL COMMENT '唯一事件ID',
    event_version int NOT NULL DEFAULT 1 COMMENT '事件协议版本',
    occurred_at datetime(3) NOT NULL COMMENT '事件产生时间',
    event_type varchar(32) NOT NULL COMMENT '投影事件类型',
    aggregate_type varchar(32) NOT NULL COMMENT 'TASK/PROCESS',
    aggregate_id varchar(128) NOT NULL COMMENT '任务或流程实例ID',
    payload longtext NOT NULL COMMENT '不可变投影快照JSON',
    payload_hash char(64) NOT NULL COMMENT '投影快照SHA-256摘要',
    projection_status tinyint NOT NULL DEFAULT 0 COMMENT '0待处理 1处理中 2成功 3待重试 4死信',
    retry_count int NOT NULL DEFAULT 0 COMMENT '已认领次数',
    next_retry_time datetime DEFAULT NULL COMMENT '下次允许重试时间',
    lock_owner varchar(64) DEFAULT NULL COMMENT '当前认领节点',
    lock_time datetime DEFAULT NULL COMMENT '认领时间',
    applied_time datetime DEFAULT NULL COMMENT '应用成功时间',
    last_error varchar(128) DEFAULT NULL COMMENT '最近失败类型，不保存异常原文',
    create_by bigint DEFAULT NULL,
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_dept bigint DEFAULT NULL,
    update_by bigint DEFAULT NULL,
    update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_flow_projection_event (tenant_id, event_id),
    KEY idx_flow_projection_dispatch (projection_status, next_retry_time, lock_time, id),
    KEY idx_flow_projection_aggregate (tenant_id, aggregate_type, aggregate_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Flowable本地镜像投影Outbox';

SET @table_exists = (
    SELECT COUNT(*) FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_task'
);
SET @column_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_task'
      AND COLUMN_NAME = 'projection_event_id'
);
SET @sql = IF(@table_exists = 1 AND @column_exists = 0,
    'ALTER TABLE `sys_flow_task` ADD COLUMN `projection_event_id` varchar(64) DEFAULT NULL COMMENT ''最近镜像投影事件ID'' AFTER `action_type`',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @column_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_task'
      AND COLUMN_NAME = 'projection_sequence'
);
SET @sql = IF(@table_exists = 1 AND @column_exists = 0,
    'ALTER TABLE `sys_flow_task` ADD COLUMN `projection_sequence` bigint NOT NULL DEFAULT 0 COMMENT ''最近镜像投影顺序号'' AFTER `projection_event_id`',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @table_exists = (
    SELECT COUNT(*) FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_business'
);
SET @column_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_business'
      AND COLUMN_NAME = 'projection_event_id'
);
SET @sql = IF(@table_exists = 1 AND @column_exists = 0,
    'ALTER TABLE `sys_flow_business` ADD COLUMN `projection_event_id` varchar(64) DEFAULT NULL COMMENT ''最近镜像投影事件ID'' AFTER `duration`',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @column_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_business'
      AND COLUMN_NAME = 'projection_sequence'
);
SET @sql = IF(@table_exists = 1 AND @column_exists = 0,
    'ALTER TABLE `sys_flow_business` ADD COLUMN `projection_sequence` bigint NOT NULL DEFAULT 0 COMMENT ''最近镜像投影顺序号'' AFTER `projection_event_id`',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @table_exists = (
    SELECT COUNT(*) FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_form_instance'
);
SET @column_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_form_instance'
      AND COLUMN_NAME = 'projection_event_id'
);
SET @sql = IF(@table_exists = 1 AND @column_exists = 0,
    'ALTER TABLE `sys_flow_form_instance` ADD COLUMN `projection_event_id` varchar(64) DEFAULT NULL COMMENT ''最近镜像投影事件ID'' AFTER `end_time`',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @column_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_form_instance'
      AND COLUMN_NAME = 'projection_sequence'
);
SET @sql = IF(@table_exists = 1 AND @column_exists = 0,
    'ALTER TABLE `sys_flow_form_instance` ADD COLUMN `projection_sequence` bigint NOT NULL DEFAULT 0 COMMENT ''最近镜像投影顺序号'' AFTER `projection_event_id`',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @table_exists = (
    SELECT COUNT(*) FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_task_candidate'
);
SET @column_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_task_candidate'
      AND COLUMN_NAME = 'projection_event_id'
);
SET @sql = IF(@table_exists = 1 AND @column_exists = 0,
    'ALTER TABLE `sys_flow_task_candidate` ADD COLUMN `projection_event_id` varchar(64) DEFAULT NULL COMMENT ''最近镜像投影事件ID'' AFTER `status`',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @column_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_task_candidate'
      AND COLUMN_NAME = 'projection_sequence'
);
SET @sql = IF(@table_exists = 1 AND @column_exists = 0,
    'ALTER TABLE `sys_flow_task_candidate` ADD COLUMN `projection_sequence` bigint NOT NULL DEFAULT 0 COMMENT ''最近镜像投影顺序号'' AFTER `projection_event_id`',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
