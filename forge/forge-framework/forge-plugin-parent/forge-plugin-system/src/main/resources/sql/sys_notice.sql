-- 通知公告表
CREATE TABLE `sys_notice` (
    `notice_id` bigint NOT NULL AUTO_INCREMENT COMMENT '公告ID',
    `tenant_id` bigint NOT NULL DEFAULT '000000' COMMENT '租户编号',
    `notice_title` varchar(200) NOT NULL COMMENT '公告标题',
    `notice_content` text NOT NULL COMMENT '公告内容',
    `notice_type` varchar(20) NOT NULL DEFAULT 'NOTICE' COMMENT '公告类型：NOTICE-通知公告/ANNOUNCEMENT-系统公告/NEWS-新闻动态',
    `publish_status` tinyint NOT NULL DEFAULT 0 COMMENT '发布状态：0-草稿/1-已发布/2-已撤回',
    `publish_time` datetime DEFAULT NULL COMMENT '发布时间',
    `publisher_id` bigint DEFAULT NULL COMMENT '发布人ID',
    `publisher_name` varchar(100) DEFAULT NULL COMMENT '发布人姓名',
    `effective_time` datetime DEFAULT NULL COMMENT '生效时间',
    `expiration_time` datetime DEFAULT NULL COMMENT '失效时间',
    `is_top` tinyint NOT NULL DEFAULT 0 COMMENT '是否置顶：0-否/1-是',
    `top_sort` int DEFAULT 0 COMMENT '置顶排序（数字越大越靠前）',
    `attachment_ids` varchar(500) DEFAULT NULL COMMENT '附件ID列表（多个附件ID用逗号分隔，关联sys_file_metadata表）',
    `read_count` int DEFAULT 0 COMMENT '阅读次数',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` bigint DEFAULT NULL COMMENT '创建者',
    `update_by` bigint DEFAULT NULL COMMENT '更新者',
    PRIMARY KEY (`notice_id`),
    KEY `idx_tenant_id` (`tenant_id`),
    KEY `idx_publish_status` (`publish_status`),
    KEY `idx_notice_type` (`notice_type`),
    KEY `idx_effective_time` (`effective_time`),
    KEY `idx_expiration_time` (`expiration_time`),
    KEY `idx_is_top_sort` (`is_top`, `top_sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知公告表';

-- 插入示例数据
INSERT INTO sys_notice (tenant_id, notice_title, notice_content, notice_type, publish_status, publish_time, publisher_id, publisher_name, effective_time, is_top, remark) VALUES
(000000, '系统维护通知', '系统将于本周六凌晨2:00-6:00进行维护升级，届时将暂停服务，请各位用户提前做好准备。', 'NOTICE', 1, NOW(), 1, '系统管理员', NOW(), 1, '重要通知'),
(000000, '新功能上线公告', '系统新增通知公告管理功能，支持创建、发布、置顶等操作，欢迎使用！', 'ANNOUNCEMENT', 1, NOW(), 1, '系统管理员', NOW(), 0, '功能公告');
