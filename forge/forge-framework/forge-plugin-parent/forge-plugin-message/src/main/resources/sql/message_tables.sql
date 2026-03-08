-- 消息管理模块数据库表结构

-- 消息主表
CREATE TABLE `sys_message` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id` bigint NOT NULL DEFAULT '000000' COMMENT '租户编号',
    `title` varchar(200) NOT NULL COMMENT '消息标题',
    `content` text NOT NULL COMMENT '消息内容',
    `type` varchar(20) NOT NULL DEFAULT 'SYSTEM' COMMENT '消息类型：SYSTEM-系统消息/SMS-短信/EMAIL-邮件/CUSTOM-自定义',
    `send_scope` varchar(20) NOT NULL DEFAULT 'USERS' COMMENT '发送范围：ALL-全员/ORG-指定组织/USERS-指定人员',
    `send_channel` varchar(20) NOT NULL DEFAULT 'WEB' COMMENT '发送渠道：WEB-站内信/SMS-短信/EMAIL-邮件/PUSH-推送',
    `status` tinyint NOT NULL DEFAULT 0 COMMENT '消息状态：0-草稿/1-已发送/2-发送失败',
    `sender_id` bigint DEFAULT NULL COMMENT '发送人ID',
    `sender_name` varchar(100) DEFAULT NULL COMMENT '发送人姓名',
    `template_code` varchar(50) DEFAULT NULL COMMENT '模板编码',
    `template_params` json DEFAULT NULL COMMENT '模板参数JSON',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` bigint DEFAULT NULL COMMENT '创建者',
    `update_by` bigint DEFAULT NULL COMMENT '更新者',
    PRIMARY KEY (`id`),
    KEY `idx_tenant_type` (`tenant_id`, `type`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统消息主表';

-- 消息接收人表
CREATE TABLE `sys_message_receiver` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id` bigint NOT NULL DEFAULT '000000' COMMENT '租户编号',
    `message_id` bigint NOT NULL COMMENT '消息ID',
    `user_id` bigint NOT NULL COMMENT '接收人用户ID',
    `org_id` bigint DEFAULT NULL COMMENT '接收人所属组织ID',
    `read_flag` tinyint NOT NULL DEFAULT 0 COMMENT '已读标记：0-未读/1-已读',
    `read_time` datetime DEFAULT NULL COMMENT '阅读时间',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_message_user` (`message_id`, `user_id`),
    KEY `idx_user_read` (`user_id`, `read_flag`),
    KEY `idx_tenant_user` (`tenant_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息接收人表';

-- 消息发送记录表
CREATE TABLE `sys_message_send_record` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id` bigint NOT NULL DEFAULT '000000' COMMENT '租户编号',
    `message_id` bigint NOT NULL COMMENT '消息ID',
    `channel` varchar(20) NOT NULL COMMENT '发送渠道：WEB/SMS/EMAIL/PUSH',
    `receiver_count` int DEFAULT 0 COMMENT '接收人数量',
    `success_count` int DEFAULT 0 COMMENT '发送成功数量',
    `fail_count` int DEFAULT 0 COMMENT '发送失败数量',
    `external_id` varchar(100) DEFAULT NULL COMMENT '第三方渠道返回的消息ID',
    `status` tinyint NOT NULL DEFAULT 0 COMMENT '发送状态：0-发送中/1-成功/2-失败',
    `error_msg` text DEFAULT NULL COMMENT '错误信息',
    `send_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_message_id` (`message_id`),
    KEY `idx_send_time` (`send_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息发送记录表';

-- 消息模板表
CREATE TABLE `sys_message_template` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id` bigint NOT NULL DEFAULT '000000' COMMENT '租户编号',
    `template_code` varchar(50) NOT NULL COMMENT '模板编码（唯一）',
    `template_name` varchar(100) NOT NULL COMMENT '模板名称',
    `type` varchar(20) NOT NULL DEFAULT 'SYSTEM' COMMENT '消息类型：SYSTEM/SMS/EMAIL/CUSTOM',
    `title_template` varchar(200) DEFAULT NULL COMMENT '标题模板（支持${变量}占位符）',
    `content_template` text NOT NULL COMMENT '内容模板（支持${变量}占位符）',
    `default_channel` varchar(20) DEFAULT 'WEB' COMMENT '默认发送渠道',
    `enabled` tinyint NOT NULL DEFAULT 1 COMMENT '是否启用：0-禁用/1-启用',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注说明',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` bigint DEFAULT NULL COMMENT '创建者',
    `update_by` bigint DEFAULT NULL COMMENT '更新者',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_code` (`tenant_id`, `template_code`),
    KEY `idx_type` (`type`),
    KEY `idx_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息模板表';

-- 初始化示例模板数据
INSERT INTO sys_message_template (tenant_id, template_code, template_name, type, title_template, content_template, default_channel, remark) VALUES
(000000, 'SYSTEM_NOTICE', '系统通知', 'SYSTEM', '系统通知', '尊敬的${userName}，${content}', 'WEB', '通用系统通知模板'),
(000000, 'TASK_ASSIGN', '任务分配通知', 'SYSTEM', '您有新的任务', '${userName}，您有一个新任务：${taskName}，请及时处理。截止时间：${deadline}', 'WEB', '任务分配通知模板'),
(000000, 'SMS_VERIFY_CODE', '短信验证码', 'SMS', NULL, '【系统】您的验证码是${code}，${expireMinutes}分钟内有效，请勿泄露。', 'SMS', '短信验证码模板'),
(000000, 'APPROVAL_PASS', '审批通过通知', 'SYSTEM', '审批结果通知', '${userName}，您提交的${flowName}已审批通过。审批人：${approver}，审批时间：${approveTime}', 'WEB', '审批通过通知模板');
