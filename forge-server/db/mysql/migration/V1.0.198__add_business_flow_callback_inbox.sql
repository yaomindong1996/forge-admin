-- 为 Flow 通知消费增加可靠 Inbox，避免 Redis/Webhook 已投递但本地事务失败后永久丢失。
-- 运行态 Inbox 保留完成与死信审计轨迹，不使用逻辑删除；DEAD 会阻塞同流程后续事件，等待人工核对。
-- 回滚：先停止所有使用本 Inbox 的 Admin 节点，确认无 PENDING/PROCESSING/FAILED 后删除本表并部署旧版本。

CREATE TABLE IF NOT EXISTS `ai_business_flow_callback_inbox` (
    `id` bigint NOT NULL COMMENT '主键',
    `tenant_id` bigint NOT NULL COMMENT '租户ID',
    `event_id` varchar(128) NOT NULL COMMENT 'Flow通知Outbox稳定事件ID',
    `event_version` int NOT NULL COMMENT '事件协议版本',
    `event_sequence` bigint NOT NULL COMMENT 'Flow通知Outbox顺序号',
    `aggregate_key` char(64) NOT NULL COMMENT '流程实例或业务Key聚合摘要',
    `event_type` varchar(64) NOT NULL COMMENT '流程事件类型',
    `process_instance_id` varchar(128) DEFAULT NULL COMMENT '流程实例ID',
    `business_key` varchar(256) DEFAULT NULL COMMENT '远端流程业务Key',
    `task_id` varchar(128) DEFAULT NULL COMMENT '任务ID',
    `event_digest` char(64) NOT NULL COMMENT '规范事件载荷SHA-256摘要',
    `event_payload` json NOT NULL COMMENT '不可变事件快照',
    `consume_status` varchar(32) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PROCESSING/FAILED/COMPLETED/DEAD',
    `retry_count` int NOT NULL DEFAULT 0 COMMENT '消费认领次数',
    `next_retry_time` datetime DEFAULT NULL COMMENT '下次允许恢复时间',
    `lock_owner` varchar(128) DEFAULT NULL COMMENT '消费租约持有者',
    `lock_time` datetime DEFAULT NULL COMMENT '消费租约时间',
    `error_type` varchar(128) DEFAULT NULL COMMENT '最近失败类型，不保存异常消息',
    `completed_time` datetime DEFAULT NULL COMMENT '本地事务消费完成时间',
    `create_by` bigint DEFAULT NULL,
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `create_dept` bigint DEFAULT NULL,
    `update_by` bigint DEFAULT NULL,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ai_business_flow_callback_event` (`tenant_id`, `event_id`),
    KEY `idx_ai_business_flow_callback_recovery` (`consume_status`, `next_retry_time`, `lock_time`, `create_time`),
    KEY `idx_ai_business_flow_callback_order` (`tenant_id`, `aggregate_key`, `event_sequence`, `consume_status`),
    KEY `idx_ai_business_flow_callback_process` (`tenant_id`, `process_instance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='低代码流程回调可靠Inbox';
