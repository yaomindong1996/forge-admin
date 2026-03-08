-- 在线用户表
CREATE TABLE `sys_auth_online_user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `token_value` VARCHAR(255) NOT NULL COMMENT 'Token值',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `username` VARCHAR(100) NOT NULL COMMENT '用户名',
    `real_name` VARCHAR(100) COMMENT '真实姓名',
    `dept_name` VARCHAR(100) COMMENT '部门名称',
    `ip_address` VARCHAR(50) COMMENT '登录IP地址',
    `login_location` VARCHAR(100) COMMENT '登录地点',
    `browser` VARCHAR(50) COMMENT '浏览器类型',
    `os` VARCHAR(50) COMMENT '操作系统',
    `login_time` DATETIME NOT NULL COMMENT '登录时间',
    `last_activity_time` DATETIME COMMENT '最后活动时间',
    `expire_time` DATETIME COMMENT 'Token过期时间',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 1-在线, 0-已下线',
    `logout_time` DATETIME COMMENT '登出时间',
    `logout_type` TINYINT COMMENT '登出类型: 1-主动登出, 2-被踢下线, 3-被顶下线, 4-Token过期',
    `tenant_id` BIGINT COMMENT '租户ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_token` (`token_value`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`),
    KEY `idx_login_time` (`login_time`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='在线用户表';

-- 在线用户历史表(用于统计分析)
CREATE TABLE `sys_auth_online_user_history` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `username` VARCHAR(100) NOT NULL COMMENT '用户名',
    `ip_address` VARCHAR(50) COMMENT '登录IP地址',
    `login_location` VARCHAR(100) COMMENT '登录地点',
    `browser` VARCHAR(50) COMMENT '浏览器类型',
    `os` VARCHAR(50) COMMENT '操作系统',
    `login_time` DATETIME NOT NULL COMMENT '登录时间',
    `logout_time` DATETIME COMMENT '登出时间',
    `online_duration` INT COMMENT '在线时长(秒)',
    `logout_type` TINYINT COMMENT '登出类型: 1-主动登出, 2-被踢下线, 3-被顶下线, 4-Token过期',
    `tenant_id` BIGINT COMMENT '租户ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_login_time` (`login_time`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='在线用户历史表';
