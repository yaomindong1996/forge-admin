-- 流程通知事务 Outbox。
-- 该表属于运行时中间表，不使用逻辑删除；成功记录由后续留存清理任务物理归档/清理。
-- 回滚：先停用 forge.flow.notify-outbox.enabled，确认无 PENDING/PROCESSING/FAILED 记录后删除本表。

CREATE TABLE IF NOT EXISTS sys_flow_notify_outbox (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '全局投递顺序号',
    tenant_id bigint NOT NULL COMMENT '业务租户ID',
    event_id varchar(64) NOT NULL COMMENT '稳定事件ID',
    event_version int NOT NULL DEFAULT 1 COMMENT '事件协议版本',
    occurred_at datetime(3) NOT NULL COMMENT '事件产生时间',
    event_type varchar(32) NOT NULL COMMENT '通知事件类型',
    aggregate_type varchar(32) NOT NULL COMMENT '聚合类型',
    aggregate_id varchar(128) NOT NULL COMMENT '流程实例/任务等聚合ID',
    payload longtext NOT NULL COMMENT '通知快照JSON',
    payload_hash char(64) NOT NULL COMMENT '通知快照SHA-256摘要',
    delivery_status tinyint NOT NULL DEFAULT 0 COMMENT '0待投递 1投递中 2成功 3待重试 4死信',
    retry_count int NOT NULL DEFAULT 0 COMMENT '已认领投递次数',
    next_retry_time datetime DEFAULT NULL COMMENT '下次允许重试时间',
    lock_owner varchar(64) DEFAULT NULL COMMENT '当前认领节点',
    lock_time datetime DEFAULT NULL COMMENT '认领时间',
    delivered_time datetime DEFAULT NULL COMMENT '投递成功时间',
    last_error varchar(128) DEFAULT NULL COMMENT '最近失败类型，不保存异常原文',
    create_by bigint DEFAULT NULL,
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_dept bigint DEFAULT NULL,
    update_by bigint DEFAULT NULL,
    update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_flow_notify_outbox_event (tenant_id, event_id),
    KEY idx_flow_notify_outbox_dispatch (delivery_status, next_retry_time, lock_time, id),
    KEY idx_flow_notify_outbox_aggregate (tenant_id, aggregate_type, aggregate_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程通知事务Outbox';
