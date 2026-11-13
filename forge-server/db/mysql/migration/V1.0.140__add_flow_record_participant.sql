-- 流程经手可见：记录发起、实际审批、抄送关系，供业务列表只读附加可见。

CREATE TABLE IF NOT EXISTS `sys_flow_record_participant` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `business_type` varchar(128) NOT NULL COMMENT '业务对象编码',
  `business_id` varchar(64) NOT NULL COMMENT '业务记录ID',
  `user_id` varchar(64) NOT NULL COMMENT '参与用户ID',
  `relation_type` varchar(32) NOT NULL COMMENT 'INITIATOR/ASSIGNEE/CC',
  `process_instance_id` varchar(64) DEFAULT NULL COMMENT '流程实例ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_flow_record_participant` (`tenant_id`, `business_type`, `business_id`, `user_id`, `relation_type`),
  KEY `idx_flow_participant_user` (`tenant_id`, `user_id`, `business_type`),
  KEY `idx_flow_participant_record` (`tenant_id`, `business_type`, `business_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程单据经手人索引';
