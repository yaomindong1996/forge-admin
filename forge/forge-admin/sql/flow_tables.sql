-- 流程相关表结构
-- 包含：流程业务关联表、流程任务表

-- 1. 流程业务关联表（sys_flow_business）
CREATE TABLE IF NOT EXISTS `sys_flow_business` (
    `id` varchar(64) NOT NULL COMMENT '主键ID',
    `business_key` varchar(100) DEFAULT NULL COMMENT '业务Key（唯一）',
    `business_type` varchar(50) DEFAULT NULL COMMENT '业务类型',
    `process_instance_id` varchar(64) DEFAULT NULL COMMENT '流程实例ID',
    `process_def_id` varchar(64) DEFAULT NULL COMMENT '流程定义ID',
    `process_def_key` varchar(100) DEFAULT NULL COMMENT '流程定义KEY',
    `title` varchar(255) DEFAULT NULL COMMENT '流程标题',
    `status` varchar(20) DEFAULT 'draft' COMMENT '业务状态（draft-草稿/running-审批中/approved-已通过/rejected-已驳回/canceled-已取消）',
    `apply_user_id` varchar(64) DEFAULT NULL COMMENT '申请人ID',
    `apply_user_name` varchar(100) DEFAULT NULL COMMENT '申请人姓名',
    `apply_dept_id` varchar(64) DEFAULT NULL COMMENT '申请部门ID',
    `apply_dept_name` varchar(100) DEFAULT NULL COMMENT '申请部门名称',
    `apply_time` datetime DEFAULT NULL COMMENT '申请时间',
    `end_time` datetime DEFAULT NULL COMMENT '结束时间',
    `duration` bigint DEFAULT NULL COMMENT '流程耗时（毫秒）',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_business_key` (`business_key`),
    KEY `idx_process_instance_id` (`process_instance_id`),
    KEY `idx_process_def_key` (`process_def_key`),
    KEY `idx_status` (`status`),
    KEY `idx_apply_user_id` (`apply_user_id`),
    KEY `idx_apply_time` (`apply_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程业务关联表';

-- 2. 流程任务表（sys_flow_task）
CREATE TABLE IF NOT EXISTS `sys_flow_task` (
    `id` varchar(64) NOT NULL COMMENT '主键ID',
    `task_id` varchar(64) NOT NULL COMMENT 'Flowable任务ID',
    `task_name` varchar(200) DEFAULT NULL COMMENT '任务名称',
    `task_def_key` varchar(100) DEFAULT NULL COMMENT '任务定义Key',
    `task_def_id` varchar(64) DEFAULT NULL COMMENT '任务定义ID',
    `process_instance_id` varchar(64) DEFAULT NULL COMMENT '流程实例ID',
    `process_def_id` varchar(64) DEFAULT NULL COMMENT '流程定义ID',
    `process_def_key` varchar(100) DEFAULT NULL COMMENT '流程定义KEY',
    `business_key` varchar(100) DEFAULT NULL COMMENT '业务Key',
    `business_type` varchar(50) DEFAULT NULL COMMENT '业务类型',
    `title` varchar(255) DEFAULT NULL COMMENT '任务标题',
    `assignee` varchar(64) DEFAULT NULL COMMENT '处理人（签收后）',
    `assignee_name` varchar(100) DEFAULT NULL COMMENT '处理人姓名',
    `candidate_users` varchar(500) DEFAULT NULL COMMENT '候选人（逗号分隔）',
    `candidate_groups` varchar(500) DEFAULT NULL COMMENT '候选组（逗号分隔）',
    `owner` varchar(64) DEFAULT NULL COMMENT '任务拥有人',
    `due_date` datetime DEFAULT NULL COMMENT '截止日期',
    `priority` int DEFAULT 50 COMMENT '优先级（0-100）',
    `status` int DEFAULT 0 COMMENT '状态（0-待办/1-已签收/2-已通过/3-已驳回/4-已转办/5-已取消）',
    `comment` varchar(500) DEFAULT NULL COMMENT '审批意见',
    `attachment_urls` varchar(1000) DEFAULT NULL COMMENT '附件URL（逗号分隔）',
    `start_user_id` varchar(64) DEFAULT NULL COMMENT '流程发起人ID',
    `start_user_name` varchar(100) DEFAULT NULL COMMENT '发起人姓名',
    `start_dept_id` varchar(64) DEFAULT NULL COMMENT '发起部门ID',
    `start_dept_name` varchar(100) DEFAULT NULL COMMENT '发起部门名称',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `claim_time` datetime DEFAULT NULL COMMENT '签收时间',
    `complete_time` datetime DEFAULT NULL COMMENT '完成时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_task_id` (`task_id`),
    KEY `idx_process_instance_id` (`process_instance_id`),
    KEY `idx_process_def_key` (`process_def_key`),
    KEY `idx_assignee` (`assignee`),
    KEY `idx_status` (`status`),
    KEY `idx_start_user_id` (`start_user_id`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_complete_time` (`complete_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程任务表';

-- 3. 流程抄送表（sys_flow_cc）
CREATE TABLE IF NOT EXISTS `sys_flow_cc` (
    `id` varchar(64) NOT NULL COMMENT '主键ID',
    `process_instance_id` varchar(64) NOT NULL COMMENT '流程实例ID',
    `process_def_key` varchar(100) DEFAULT NULL COMMENT '流程定义KEY',
    `task_id` varchar(64) DEFAULT NULL COMMENT '来源任务ID',
    `business_key` varchar(100) DEFAULT NULL COMMENT '业务Key',
    `title` varchar(255) DEFAULT NULL COMMENT '流程标题',
    `content` varchar(500) DEFAULT NULL COMMENT '内容摘要',
    `cc_user_id` varchar(64) NOT NULL COMMENT '抄送人ID',
    `cc_user_name` varchar(100) DEFAULT NULL COMMENT '抄送人姓名',
    `send_user_id` varchar(64) DEFAULT NULL COMMENT '发送人ID',
    `send_user_name` varchar(100) DEFAULT NULL COMMENT '发送人姓名',
    `cc_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '抄送时间',
    `read_time` datetime DEFAULT NULL COMMENT '阅读时间',
    `is_read` int DEFAULT 0 COMMENT '是否已读（0-未读/1-已读）',
    PRIMARY KEY (`id`),
    KEY `idx_process_instance_id` (`process_instance_id`),
    KEY `idx_cc_user_id` (`cc_user_id`),
    KEY `idx_send_user_id` (`send_user_id`),
    KEY `idx_is_read` (`is_read`),
    KEY `idx_cc_time` (`cc_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程抄送表';

-- 4. 流程评论表（sys_flow_comment）
CREATE TABLE IF NOT EXISTS `sys_flow_comment` (
    `id` varchar(64) NOT NULL COMMENT '主键ID',
    `process_instance_id` varchar(64) NOT NULL COMMENT '流程实例ID',
    `task_id` varchar(64) DEFAULT NULL COMMENT '任务ID',
    `user_id` varchar(64) NOT NULL COMMENT '评论人ID',
    `user_name` varchar(100) DEFAULT NULL COMMENT '评论人姓名',
    `comment_type` varchar(20) DEFAULT 'comment' COMMENT '评论类型（comment-评论/approve-通过/reject-驳回/delegate-转办/withdraw-撤回）',
    `comment` varchar(1000) DEFAULT NULL COMMENT '评论内容',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_process_instance_id` (`process_instance_id`),
    KEY `idx_task_id` (`task_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程评论表';