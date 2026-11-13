-- 为动态 CRUD/流程回调事件增加事务 Outbox、聚合顺序、租约恢复和死信状态。
-- 运行态中间表按恢复协议物理保留，不使用逻辑删除；DEAD 会阻塞同一聚合的后续事件，等待人工处置。
-- 回滚：先停用 forge.business.event-outbox.enabled，确认 PENDING/FAILED/PROCESSING 已清空后删除两张表。

CREATE TABLE IF NOT EXISTS `ai_business_event_sequence` (
    `aggregate_key` char(64) NOT NULL COMMENT '租户、对象与记录组成的聚合摘要',
    `tenant_id` bigint NOT NULL COMMENT '租户ID',
    `object_code` varchar(128) NOT NULL COMMENT '业务对象编码',
    `record_id` varchar(128) NOT NULL COMMENT '业务记录ID',
    `last_sequence` bigint NOT NULL DEFAULT 0 COMMENT '聚合内最后分配序号',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`aggregate_key`),
    UNIQUE KEY `uk_ai_business_event_sequence_tenant_aggregate` (`tenant_id`, `aggregate_key`),
    KEY `idx_ai_business_event_sequence_object` (`tenant_id`, `object_code`, `record_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='低代码业务事件聚合序号';

CREATE TABLE IF NOT EXISTS `ai_business_event_outbox` (
    `id` bigint NOT NULL COMMENT '主键',
    `tenant_id` bigint NOT NULL COMMENT '租户ID',
    `event_id` varchar(128) NOT NULL COMMENT '稳定事件ID',
    `event_source` varchar(32) NOT NULL COMMENT '可信事件来源',
    `event_version` int NOT NULL COMMENT '事件协议版本',
    `source_digest` char(64) NOT NULL COMMENT '不含聚合序号的逻辑载荷摘要',
    `event_digest` char(64) NOT NULL COMMENT '完整信封载荷摘要',
    `event_type` varchar(64) NOT NULL COMMENT '事件类型',
    `suite_code` varchar(128) DEFAULT NULL COMMENT '业务套件编码',
    `object_code` varchar(128) NOT NULL COMMENT '业务对象编码',
    `config_key` varchar(128) DEFAULT NULL COMMENT '运行配置键',
    `record_id` varchar(128) NOT NULL COMMENT '业务记录ID',
    `aggregate_key` char(64) NOT NULL COMMENT '聚合摘要',
    `aggregate_sequence` bigint NOT NULL COMMENT '聚合内单调序号',
    `event_payload` json NOT NULL COMMENT '可信事件完整快照',
    `delivery_status` varchar(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PROCESSING/DELIVERED/FAILED/DEAD',
    `retry_count` int NOT NULL DEFAULT 0 COMMENT '认领次数',
    `next_retry_time` datetime DEFAULT NULL COMMENT '下次重试时间',
    `lock_owner` varchar(128) DEFAULT NULL COMMENT '投递租约持有者',
    `lock_time` datetime DEFAULT NULL COMMENT '投递租约时间',
    `error_type` varchar(128) DEFAULT NULL COMMENT '失败异常类型，不保存敏感消息',
    `delivered_time` datetime DEFAULT NULL COMMENT '投递完成时间',
    `create_by` bigint DEFAULT NULL,
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `create_dept` bigint DEFAULT NULL,
    `update_by` bigint DEFAULT NULL,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ai_business_event_outbox_event` (`tenant_id`, `event_id`),
    UNIQUE KEY `uk_ai_business_event_outbox_sequence` (`tenant_id`, `aggregate_key`, `aggregate_sequence`),
    KEY `idx_ai_business_event_outbox_recovery` (`delivery_status`, `next_retry_time`, `lock_time`, `create_time`),
    KEY `idx_ai_business_event_outbox_aggregate` (`tenant_id`, `aggregate_key`, `aggregate_sequence`, `delivery_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='低代码业务事件事务Outbox';
