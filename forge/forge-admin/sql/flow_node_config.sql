-- 流程节点配置表
-- 用于存储审批节点的详细配置信息
DROP TABLE IF EXISTS `sys_flow_node_config`;
CREATE TABLE `sys_flow_node_config` (
    `id` VARCHAR(64) NOT NULL COMMENT '主键ID',
    `model_id` VARCHAR(64) NOT NULL COMMENT '流程模型ID',
    `node_id` VARCHAR(128) NOT NULL COMMENT '节点ID（BPMN中的ID）',
    `node_name` VARCHAR(255) DEFAULT NULL COMMENT '节点名称',
    `assignee_type` VARCHAR(32) NOT NULL COMMENT '审批人类型：user-指定用户，role-角色，dept-部门，post-岗位，leader-直属领导，deptManager-部门负责人，initiator-发起人，expr-表达式',
    `assignee_value` VARCHAR(1000) DEFAULT NULL COMMENT '审批人值（多个用逗号分隔）',
    `multi_instance_type` VARCHAR(32) DEFAULT 'none' COMMENT '多实例类型：none-无，sequential-串行，parallel-并行',
    `completion_condition` VARCHAR(32) DEFAULT 'all' COMMENT '完成条件：all-全部通过，any-任一通过，rate-比例通过',
    `pass_rate` DECIMAL(5,2) DEFAULT NULL COMMENT '通过比例（当completion_condition=rate时使用）',
    `due_date_days` INT DEFAULT NULL COMMENT '到期天数',
    `due_date_hours` INT DEFAULT NULL COMMENT '到期小时数',
    `timeout_action` VARCHAR(32) DEFAULT 'none' COMMENT '超时动作：none-无操作，auto_pass-自动通过，auto_reject-自动拒绝，notify-通知',
    `timeout_notify_type` VARCHAR(32) DEFAULT NULL COMMENT '超时通知类型：email-邮件，sms-短信，system-系统消息',
    `allow_transfer` TINYINT(1) DEFAULT 0 COMMENT '是否允许转办',
    `allow_delegate` TINYINT(1) DEFAULT 0 COMMENT '是否允许委派',
    `allow_add_sign` TINYINT(1) DEFAULT 0 COMMENT '是否允许加签',
    `allow_reduce_sign` TINYINT(1) DEFAULT 0 COMMENT '是否允许减签',
    `allow_withdraw` TINYINT(1) DEFAULT 0 COMMENT '是否允许撤回',
    `skip_enabled` TINYINT(1) DEFAULT 0 COMMENT '是否启用跳过',
    `skip_condition` VARCHAR(500) DEFAULT NULL COMMENT '跳过条件（SpEL表达式）',
    `form_field_permission` TEXT DEFAULT NULL COMMENT '表单字段权限配置（JSON格式）',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) DEFAULT NULL COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT(1) DEFAULT 0 COMMENT '删除标志',
    PRIMARY KEY (`id`),
    KEY `idx_model_id` (`model_id`),
    KEY `idx_model_node` (`model_id`, `node_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程节点配置表';

-- 审批层级配置表
-- 用于动态多级审批场景
DROP TABLE IF EXISTS `sys_flow_approval_level`;
CREATE TABLE `sys_flow_approval_level` (
    `id` VARCHAR(64) NOT NULL COMMENT '主键ID',
    `node_config_id` VARCHAR(64) NOT NULL COMMENT '节点配置ID',
    `level_index` INT NOT NULL COMMENT '层级索引（从1开始）',
    `level_name` VARCHAR(100) DEFAULT NULL COMMENT '层级名称',
    `assignee_type` VARCHAR(32) NOT NULL COMMENT '审批人类型',
    `assignee_value` VARCHAR(1000) DEFAULT NULL COMMENT '审批人值',
    `condition_expr` VARCHAR(500) DEFAULT NULL COMMENT '触发条件（SpEL表达式）',
    `skip_condition` VARCHAR(500) DEFAULT NULL COMMENT '跳过条件（SpEL表达式）',
    `timeout_days` INT DEFAULT NULL COMMENT '超时天数',
    `timeout_hours` INT DEFAULT NULL COMMENT '超时小时数',
    `timeout_action` VARCHAR(32) DEFAULT 'none' COMMENT '超时动作',
    `sort_order` INT DEFAULT 0 COMMENT '排序号',
    `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) DEFAULT NULL COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT(1) DEFAULT 0 COMMENT '删除标志',
    PRIMARY KEY (`id`),
    KEY `idx_node_config_id` (`node_config_id`),
    KEY `idx_level_index` (`node_config_id`, `level_index`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批层级配置表';

-- 节点操作权限配置表
-- 用于配置每个节点允许的操作
DROP TABLE IF EXISTS `sys_flow_node_operation`;
CREATE TABLE `sys_flow_node_operation` (
    `id` VARCHAR(64) NOT NULL COMMENT '主键ID',
    `node_config_id` VARCHAR(64) NOT NULL COMMENT '节点配置ID',
    `operation_code` VARCHAR(64) NOT NULL COMMENT '操作代码：approve-通过，reject-拒绝，transfer-转办，delegate-委派，addSign-加签，reduceSign-减签，withdraw-撤回，comment-评论',
    `operation_name` VARCHAR(100) DEFAULT NULL COMMENT '操作名称',
    `enabled` TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    `required_reason` TINYINT(1) DEFAULT 0 COMMENT '是否需要填写原因',
    `sort_order` INT DEFAULT 0 COMMENT '排序号',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_node_config_id` (`node_config_id`),
    UNIQUE KEY `uk_node_operation` (`node_config_id`, `operation_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='节点操作权限配置表';

-- 初始化一些默认的节点操作配置
-- 这些数据会在创建节点配置时自动初始化，此处仅作为参考