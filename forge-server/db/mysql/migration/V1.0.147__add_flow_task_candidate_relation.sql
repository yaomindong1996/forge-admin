-- 流程任务候选关系规范化：保留逗号字段兼容，新增租户隔离且可索引的关系表。
CREATE TABLE IF NOT EXISTS `sys_flow_task_candidate` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `task_id` varchar(64) NOT NULL COMMENT 'Flowable任务ID',
  `process_instance_id` varchar(64) DEFAULT NULL COMMENT '流程实例ID',
  `candidate_type` varchar(16) NOT NULL COMMENT 'USER/GROUP',
  `candidate_value` varchar(128) NOT NULL COMMENT '用户ID或候选组标识',
  `source` varchar(32) NOT NULL DEFAULT 'FLOWABLE' COMMENT 'FLOWABLE/DYNAMIC_SIGN',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '1有效/0已移除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_flow_task_candidate` (`tenant_id`, `task_id`, `candidate_type`, `candidate_value`),
  KEY `idx_flow_task_candidate_lookup` (`tenant_id`, `candidate_type`, `candidate_value`, `status`),
  KEY `idx_flow_task_candidate_task` (`tenant_id`, `task_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程任务候选关系';

-- 将存量逗号字段回填到关系表。INSERT IGNORE 保证迁移中断后可安全重跑。
INSERT IGNORE INTO `sys_flow_task_candidate`
  (`tenant_id`, `task_id`, `process_instance_id`, `candidate_type`, `candidate_value`, `source`, `status`)
SELECT t.tenant_id, t.task_id, t.process_instance_id, 'USER', TRIM(j.candidate_value), 'FLOWABLE', 1
FROM sys_flow_task t
JOIN JSON_TABLE(
  CONCAT('["', REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(COALESCE(t.candidate_users, ''), ';', ','), '；', ','), '，', ','), '、', ','), ',', '","'), '"]'),
  '$[*]' COLUMNS (candidate_value VARCHAR(128) PATH '$')
) j ON TRIM(j.candidate_value) <> ''
WHERE t.task_id IS NOT NULL
  AND t.tenant_id IS NOT NULL
  AND COALESCE(t.candidate_users, '') <> '';

INSERT IGNORE INTO `sys_flow_task_candidate`
  (`tenant_id`, `task_id`, `process_instance_id`, `candidate_type`, `candidate_value`, `source`, `status`)
SELECT t.tenant_id, t.task_id, t.process_instance_id, 'GROUP', TRIM(j.candidate_value), 'FLOWABLE', 1
FROM sys_flow_task t
JOIN JSON_TABLE(
  CONCAT('["', REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(COALESCE(t.candidate_groups, ''), ';', ','), '；', ','), '，', ','), '、', ','), ',', '","'), '"]'),
  '$[*]' COLUMNS (candidate_value VARCHAR(128) PATH '$')
) j ON TRIM(j.candidate_value) <> ''
WHERE t.task_id IS NOT NULL
  AND t.tenant_id IS NOT NULL
  AND COALESCE(t.candidate_groups, '') <> '';
