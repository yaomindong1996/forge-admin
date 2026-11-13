-- 为低代码流程远程命令增加持久化状态机，消除 FlowClient 成功后本地事务回滚形成的悬空流程。
-- 运行态命令表保留完整恢复轨迹，不使用逻辑删除；DEAD 等待人工核对后重放或终止远端实例。
-- 回滚：先停用 forge.business.flow-remote-command.enabled，确认无 PENDING/PROCESSING/RETRY/REMOTE_SUCCEEDED 后删除本表。

CREATE TABLE IF NOT EXISTS `ai_business_flow_remote_command` (
    `id` bigint NOT NULL COMMENT '主键',
    `tenant_id` bigint NOT NULL COMMENT '租户ID',
    `command_key` char(64) NOT NULL COMMENT '租户、命令类型与远端业务Key的稳定摘要',
    `command_type` varchar(32) NOT NULL COMMENT '命令类型，当前支持 START',
    `request_digest` char(64) NOT NULL COMMENT '规范请求载荷SHA-256摘要',
    `request_payload` json NOT NULL COMMENT '恢复所需的不可变请求快照',
    `object_code` varchar(128) NOT NULL COMMENT '业务对象编码',
    `record_id` bigint NOT NULL COMMENT '业务记录ID',
    `business_key` varchar(160) NOT NULL COMMENT '本地业务Key',
    `flow_business_key` varchar(256) NOT NULL COMMENT '提交给流程服务的幂等业务Key',
    `flow_model_key` varchar(128) NOT NULL COMMENT '流程模型Key',
    `process_instance_id` varchar(128) DEFAULT NULL COMMENT '远端成功后返回或查回的流程实例ID',
    `command_status` varchar(32) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PROCESSING/RETRY/REMOTE_SUCCEEDED/COMPLETED/DEAD',
    `retry_count` int NOT NULL DEFAULT 0 COMMENT '认领及恢复尝试次数',
    `next_retry_time` datetime DEFAULT NULL COMMENT '下次允许自动恢复时间',
    `lock_owner` varchar(128) DEFAULT NULL COMMENT '恢复租约持有者',
    `lock_time` datetime DEFAULT NULL COMMENT '恢复租约时间',
    `error_type` varchar(128) DEFAULT NULL COMMENT '最近失败类型，不保存异常消息和敏感数据',
    `remote_succeeded_time` datetime DEFAULT NULL COMMENT '确认远端成功时间',
    `completed_time` datetime DEFAULT NULL COMMENT '本地关联与状态提交完成时间',
    `create_by` bigint DEFAULT NULL,
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `create_dept` bigint DEFAULT NULL,
    `update_by` bigint DEFAULT NULL,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ai_business_flow_remote_command` (`tenant_id`, `command_key`),
    KEY `idx_ai_business_flow_remote_recovery` (`command_status`, `next_retry_time`, `lock_time`, `create_time`),
    KEY `idx_ai_business_flow_remote_business` (`tenant_id`, `business_key`, `command_type`, `create_time`),
    KEY `idx_ai_business_flow_remote_process` (`tenant_id`, `process_instance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='低代码流程远程命令恢复日志';
