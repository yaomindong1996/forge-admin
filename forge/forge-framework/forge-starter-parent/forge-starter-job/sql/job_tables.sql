-- 任务配置表
CREATE TABLE IF NOT EXISTS `sys_job_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `job_name` VARCHAR(200) NOT NULL COMMENT '任务名称',
  `job_group` VARCHAR(200) NOT NULL DEFAULT 'DEFAULT' COMMENT '任务分组',
  `description` VARCHAR(500) COMMENT '任务描述',
  `executor_bean` VARCHAR(200) COMMENT '执行器Bean名称',
  `executor_method` VARCHAR(200) COMMENT '执行器方法名',
  `executor_handler` VARCHAR(200) COMMENT '执行器Handler',
  `executor_service` VARCHAR(200) COMMENT '执行器服务名（RPC模式）',
  `cron_expression` VARCHAR(100) NOT NULL COMMENT 'Cron表达式',
  `job_param` TEXT COMMENT '任务参数',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-停止 1-运行',
  `execute_mode` VARCHAR(20) NOT NULL DEFAULT 'HANDLER' COMMENT '执行模式：BEAN-Bean模式 HANDLER-Handler模式 RPC-远程模式',
  `retry_count` INT DEFAULT 0 COMMENT '失败重试次数',
  `alarm_email` VARCHAR(500) COMMENT '告警邮箱',
  `webhook_url` VARCHAR(500) COMMENT 'WebHook地址',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_job_name_group` (`job_name`, `job_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务配置表';

-- 任务执行日志表
CREATE TABLE IF NOT EXISTS `sys_job_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `job_name` VARCHAR(200) NOT NULL COMMENT '任务名称',
  `job_group` VARCHAR(200) NOT NULL COMMENT '任务分组',
  `executor_handler` VARCHAR(200) COMMENT '执行器Handler',
  `job_param` TEXT COMMENT '任务参数',
  `trigger_time` DATETIME COMMENT '触发时间',
  `start_time` DATETIME COMMENT '开始时间',
  `end_time` DATETIME COMMENT '结束时间',
  `duration` BIGINT COMMENT '执行耗时(ms)',
  `status` TINYINT NOT NULL COMMENT '状态：1-成功 0-失败',
  `result` TEXT COMMENT '执行结果',
  `exception_msg` TEXT COMMENT '异常信息',
  `retry_count` INT DEFAULT 0 COMMENT '重试次数',
  PRIMARY KEY (`id`),
  KEY `idx_job_name` (`job_name`),
  KEY `idx_trigger_time` (`trigger_time`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务执行日志表';

-- Quartz集群支持表（可选，启用集群模式时需要）
-- 由Quartz自动创建，这里仅列出表名供参考
-- qrtz_job_details, qrtz_triggers, qrtz_cron_triggers, etc.
