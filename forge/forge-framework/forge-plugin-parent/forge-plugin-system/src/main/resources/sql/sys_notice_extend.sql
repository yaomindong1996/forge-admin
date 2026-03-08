-- ----------------------------
-- 通知公告组织关联表
-- ----------------------------
DROP TABLE IF EXISTS `sys_notice_org`;
CREATE TABLE `sys_notice_org` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '000000' COMMENT '租户编号',
  `notice_id` bigint NOT NULL COMMENT '公告ID',
  `org_id` bigint NOT NULL COMMENT '组织ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_notice_id` (`notice_id`),
  KEY `idx_org_id` (`org_id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='通知公告组织关联表';

-- ----------------------------
-- 通知公告已读记录表
-- ----------------------------
DROP TABLE IF EXISTS `sys_notice_read_record`;
CREATE TABLE `sys_notice_read_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '000000' COMMENT '租户编号',
  `notice_id` bigint NOT NULL COMMENT '公告ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `user_name` varchar(100) DEFAULT NULL COMMENT '用户姓名',
  `org_id` bigint DEFAULT NULL COMMENT '用户所属组织ID',
  `org_name` varchar(100) DEFAULT NULL COMMENT '用户所属组织名称',
  `read_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '阅读时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_notice_user` (`notice_id`, `user_id`),
  KEY `idx_notice_id` (`notice_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='通知公告已读记录表';

-- ----------------------------
-- 扩展sys_notice表字段
-- ----------------------------
ALTER TABLE `sys_notice` 
ADD COLUMN `publish_scope` tinyint NOT NULL DEFAULT 0 COMMENT '发布范围：0-全部组织/1-指定组织' AFTER `publisher_name`;
