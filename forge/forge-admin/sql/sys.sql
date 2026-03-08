-- 通用字段说明：
-- id: 主键自增
-- tenant_id: 租户编号（000000为系统租户）
-- create_dept: 创建部门ID
-- create_by: 创建人ID
-- create_time: 创建时间（自动填充）
-- update_by: 更新人ID
-- update_time: 更新时间（自动填充）
-- 所有表均添加租户+主键联合索引，确保多租户数据隔离和查询性能

-- 1. 租户表（sys_tenant）
CREATE TABLE `sys_tenant` (
                              `id` bigint NOT NULL AUTO_INCREMENT COMMENT '租户ID',
                              `tenant_name` varchar(100) NOT NULL COMMENT '租户名称',
                              `contact_person` varchar(50) DEFAULT NULL COMMENT '负责人',
                              `contact_phone` varchar(20) DEFAULT NULL COMMENT '联系电话',
                              `user_limit` int DEFAULT 0 COMMENT '租户人员数量上限（0表示无限制）',
                              `tenant_status` tinyint NOT NULL DEFAULT 1 COMMENT '租户状态（0-禁用，1-正常）',
                              `expire_time` datetime DEFAULT NULL COMMENT '过期时间',
                              `tenant_desc` varchar(500) DEFAULT NULL COMMENT '租户描述',
                              `create_by` bigint DEFAULT NULL COMMENT '创建者（系统租户ID）',
                              `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                              `update_by` bigint DEFAULT NULL COMMENT '更新者',
                              `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                              PRIMARY KEY (`id`),
                              UNIQUE KEY `uk_tenant_name` (`tenant_name`),
                              KEY `idx_tenant_status` (`tenant_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户表';

-- 2. 用户表（sys_user）
CREATE TABLE `sys_user` (
                            `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
                            `tenant_id` bigint NOT NULL DEFAULT '000000' COMMENT '租户编号',
                            `username` varchar(50) NOT NULL COMMENT '用户名（租户内唯一）',
                            `real_name` varchar(50) DEFAULT NULL COMMENT '用户真实姓名',
                            `user_type` tinyint DEFAULT 1 COMMENT '用户类型（0-系统管理员，1-租户管理员，2-普通用户）',
                            `user_client` varchar(20) DEFAULT NULL COMMENT '用户触点（app/pc/h5/wechat）',
                            `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
                            `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
                            `id_card` varchar(18) DEFAULT NULL COMMENT '身份证号',
                            `gender` tinyint DEFAULT 0 COMMENT '性别（0-未知，1-男，2-女）',
                            `password` varchar(100) NOT NULL COMMENT '密码（加密存储）',
                            `salt` varchar(50) DEFAULT NULL COMMENT '密码盐值',
                            `user_status` tinyint NOT NULL DEFAULT 1 COMMENT '用户状态（0-禁用，1-正常，2-锁定）',
                            `avatar` varchar(255) DEFAULT NULL COMMENT '头像URL',
                            `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
                            `last_login_ip` varchar(50) DEFAULT NULL COMMENT '最后登录IP',
                            `login_count` int DEFAULT 0 COMMENT '登录次数',
                            `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                            `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
                            `create_by` bigint DEFAULT NULL COMMENT '创建者',
                            `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                            `update_by` bigint DEFAULT NULL COMMENT '更新者',
                            `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                            PRIMARY KEY (`id`),
                            UNIQUE KEY `uk_tenant_username` (`tenant_id`,`username`),
                            UNIQUE KEY `uk_phone` (`phone`),
                            UNIQUE KEY `uk_email` (`email`),
                            UNIQUE KEY `uk_id_card` (`id_card`),
                            KEY `idx_tenant_status` (`tenant_id`,`user_status`),
                            KEY `idx_user_type` (`user_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 3. 组织表（sys_org）
CREATE TABLE `sys_org` (
                           `id` bigint NOT NULL AUTO_INCREMENT COMMENT '组织ID',
                           `tenant_id` bigint NOT NULL DEFAULT '000000' COMMENT '租户编号',
                           `org_name` varchar(100) NOT NULL COMMENT '组织名称',
                           `parent_id` bigint NOT NULL DEFAULT 0 COMMENT '父级组织ID（0为顶级）',
                           `ancestors` varchar(500) DEFAULT NULL COMMENT '祖级编码（逗号分隔，如：1,2,3）',
                           `sort` int DEFAULT 0 COMMENT '排序（值越小越靠前）',
                           `org_type` tinyint DEFAULT 1 COMMENT '组织类型（1-公司，2-部门，3-小组）',
                           `org_status` tinyint NOT NULL DEFAULT 1 COMMENT '组织状态（0-禁用，1-正常）',
                           `leader_id` bigint DEFAULT NULL COMMENT '负责人ID（关联sys_user.id）',
                           `leader_name` varchar(50) DEFAULT NULL COMMENT '负责人姓名',
                           `phone` varchar(20) DEFAULT NULL COMMENT '组织联系电话',
                           `address` varchar(255) DEFAULT NULL COMMENT '组织地址',
                           `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                           `create_by` bigint DEFAULT NULL COMMENT '创建者',
                           `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                           `update_by` bigint DEFAULT NULL COMMENT '更新者',
                           `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                           PRIMARY KEY (`id`),
                           UNIQUE KEY `uk_tenant_org_name` (`tenant_id`,`org_name`),
                           KEY `idx_tenant_parent` (`tenant_id`,`parent_id`),
                           KEY `idx_org_status` (`org_status`),
                           KEY `idx_ancestors` (`ancestors`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组织表';

-- 4. 岗位表（sys_post）
CREATE TABLE `sys_post` (
                            `id` bigint NOT NULL AUTO_INCREMENT COMMENT '岗位ID',
                            `tenant_id` bigint NOT NULL DEFAULT '000000' COMMENT '租户编号',
                            `post_code` varchar(50) NOT NULL COMMENT '岗位编码（租户内唯一）',
                            `org_id` bigint NOT NULL COMMENT '所属组织ID（关联sys_org.id）',
                            `post_name` varchar(100) NOT NULL COMMENT '岗位名称',
                            `post_status` tinyint NOT NULL DEFAULT 1 COMMENT '岗位状态（0-禁用，1-正常）',
                            `post_type` tinyint DEFAULT 1 COMMENT '岗位类型（1-管理岗，2-技术岗，3-业务岗，4-其他）',
                            `sort` int DEFAULT 0 COMMENT '排序（值越小越靠前）',
                            `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                            `create_by` bigint DEFAULT NULL COMMENT '创建者',
                            `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                            `update_by` bigint DEFAULT NULL COMMENT '更新者',
                            `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                            PRIMARY KEY (`id`),
                            UNIQUE KEY `uk_tenant_post_code` (`tenant_id`,`post_code`),
                            UNIQUE KEY `uk_tenant_org_post` (`tenant_id`,`org_id`,`post_name`),
                            KEY `idx_tenant_org` (`tenant_id`,`org_id`),
                            KEY `idx_post_status` (`post_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='岗位表';

-- 5. 角色表（sys_role）
CREATE TABLE `sys_role` (
                            `id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色ID',
                            `tenant_id` bigint NOT NULL DEFAULT '000000' COMMENT '租户编号',
                            `role_name` varchar(50) NOT NULL COMMENT '角色名称（租户内唯一）',
                            `role_key` varchar(100) NOT NULL COMMENT '角色权限字符串（如：admin,user:view）',
                            `data_scope` tinyint DEFAULT 1 COMMENT '权限范围（1-全部数据，2-本租户数据，3-本组织数据，4-本组织及子组织，5-个人数据）',
                            `sort` int DEFAULT 0 COMMENT '排序（值越小越靠前）',
                            `role_status` tinyint NOT NULL DEFAULT 1 COMMENT '角色状态（0-禁用，1-正常）',
                            `is_system` tinyint DEFAULT 0 COMMENT '是否系统内置角色（0-否，1-是）',
                            `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                            `create_by` bigint DEFAULT NULL COMMENT '创建者',
                            `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                            `update_by` bigint DEFAULT NULL COMMENT '更新者',
                            `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                            PRIMARY KEY (`id`),
                            UNIQUE KEY `uk_tenant_role_name` (`tenant_id`,`role_name`),
                            UNIQUE KEY `uk_tenant_role_key` (`tenant_id`,`role_key`),
                            KEY `idx_tenant_status` (`tenant_id`,`role_status`),
                            KEY `idx_data_scope` (`data_scope`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 6. 系统资源表（sys_resource）- 统一管理菜单、按钮、API
CREATE TABLE `sys_resource` (
                                `id` bigint NOT NULL AUTO_INCREMENT COMMENT '资源ID',
                                `tenant_id` bigint NOT NULL DEFAULT '000000' COMMENT '租户编号',
                                `resource_name` varchar(100) NOT NULL COMMENT '资源名称',
                                `parent_id` bigint NOT NULL DEFAULT 0 COMMENT '父级资源ID（0为顶级）',
                                `resource_type` tinyint NOT NULL COMMENT '资源类型（1-目录，2-菜单，3-按钮，4-API接口）',
                                `sort` int DEFAULT 0 COMMENT '排序（值越小越靠前）',
                                `path` varchar(255) DEFAULT NULL COMMENT '资源路由（菜单/目录用）',
                                `component` varchar(255) DEFAULT NULL COMMENT '前端组件路径（菜单用）',
                                `is_external` tinyint DEFAULT 0 COMMENT '是否外链（0-否，1-是）',
                                `is_public` tinyint DEFAULT 0 COMMENT '是否公开资源（0-否，1-是，公开资源无需权限验证）',
                                `menu_status` tinyint DEFAULT 1 COMMENT '菜单状态（0-隐藏，1-显示，仅菜单/目录用）',
                                `visible` tinyint DEFAULT 1 COMMENT '显示状态（0-隐藏，1-显示，所有资源通用）',
                                `perms` varchar(100) DEFAULT NULL COMMENT '权限标识（如：sys:user:list，按钮/API用）',
                                `icon` varchar(50) DEFAULT NULL COMMENT '图标（菜单/目录用）',
                                `api_method` varchar(10) DEFAULT NULL COMMENT 'API请求方法（GET/POST/PUT/DELETE，仅API用）',
                                `api_url` varchar(255) DEFAULT NULL COMMENT 'API接口地址（仅API用）',
                                `keep_alive` tinyint DEFAULT 0 COMMENT '是否缓存（0-否，1-是，菜单用）',
                                `always_show` tinyint DEFAULT 0 COMMENT '是否总是显示（0-否，1-是，菜单用）',
                                `redirect` varchar(255) DEFAULT NULL COMMENT '重定向地址（菜单用）',
                                `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                                `create_by` bigint DEFAULT NULL COMMENT '创建者',
                                `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                `update_by` bigint DEFAULT NULL COMMENT '更新者',
                                `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                PRIMARY KEY (`id`),
                                UNIQUE KEY `uk_tenant_resource` (`tenant_id`,`resource_type`,`perms`) COMMENT '租户内资源权限标识唯一',
                                KEY `idx_tenant_parent` (`tenant_id`,`parent_id`),
                                KEY `idx_resource_type` (`resource_type`),
                                KEY `idx_api_url_method` (`api_url`,`api_method`) COMMENT 'API查询优化'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统资源表（菜单/按钮/API）';

-- 7. 用户-角色关联表（sys_user_role）
CREATE TABLE `sys_user_role` (
                                 `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                 `tenant_id` bigint NOT NULL DEFAULT '000000' COMMENT '租户编号',
                                 `user_id` bigint NOT NULL COMMENT '用户ID',
                                 `role_id` bigint NOT NULL COMMENT '角色ID',
                                 `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                 PRIMARY KEY (`id`),
                                 UNIQUE KEY `uk_user_role` (`tenant_id`,`user_id`,`role_id`),
                                 KEY `idx_user_id` (`user_id`),
                                 KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户-角色关联表';

-- 8. 用户-岗位关联表（sys_user_post）
CREATE TABLE `sys_user_post` (
                                 `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                 `tenant_id` bigint NOT NULL DEFAULT '000000' COMMENT '租户编号',
                                 `user_id` bigint NOT NULL COMMENT '用户ID',
                                 `post_id` bigint NOT NULL COMMENT '岗位ID',
                                 `is_main` tinyint DEFAULT 0 COMMENT '是否主岗（0-否，1-是）',
                                 `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                 PRIMARY KEY (`id`),
                                 UNIQUE KEY `uk_user_post` (`tenant_id`,`user_id`,`post_id`),
                                 KEY `idx_user_id` (`user_id`),
                                 KEY `idx_post_id` (`post_id`),
                                 KEY `idx_user_main_post` (`user_id`,`is_main`) COMMENT '查询用户主岗优化'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户-岗位关联表';

-- 9. 用户-组织关联表（sys_user_org）
CREATE TABLE `sys_user_org` (
                                `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                `tenant_id` bigint NOT NULL DEFAULT '000000' COMMENT '租户编号',
                                `user_id` bigint NOT NULL COMMENT '用户ID',
                                `org_id` bigint NOT NULL COMMENT '组织ID',
                                `is_main` tinyint DEFAULT 0 COMMENT '是否主组织（0-否，1-是）',
                                `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                PRIMARY KEY (`id`),
                                UNIQUE KEY `uk_user_org` (`tenant_id`,`user_id`,`org_id`),
                                KEY `idx_user_id` (`user_id`),
                                KEY `idx_org_id` (`org_id`),
                                KEY `idx_user_main_org` (`user_id`,`is_main`) COMMENT '查询用户主组织优化'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户-组织关联表';

-- 10. 角色-资源关联表（sys_role_resource）
CREATE TABLE `sys_role_resource` (
                                     `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                     `tenant_id` bigint NOT NULL DEFAULT '000000' COMMENT '租户编号',
                                     `role_id` bigint NOT NULL COMMENT '角色ID',
                                     `resource_id` bigint NOT NULL COMMENT '资源ID',
                                     `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                     PRIMARY KEY (`id`),
                                     UNIQUE KEY `uk_role_resource` (`tenant_id`,`role_id`,`resource_id`),
                                     KEY `idx_role_id` (`role_id`),
                                     KEY `idx_resource_id` (`resource_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色-资源关联表';

-- 11. 系统配置表（sys_config）
CREATE TABLE `sys_config` (
                              `config_id` bigint NOT NULL AUTO_INCREMENT COMMENT '参数主键',
                              `tenant_id` bigint DEFAULT '000000' COMMENT '租户编号',
                              `config_name` varchar(100) DEFAULT '' COMMENT '参数名称',
                              `config_key` varchar(100) DEFAULT '' COMMENT '参数键名',
                              `config_value` varchar(500) DEFAULT '' COMMENT '参数键值',
                              `config_type` char(1) DEFAULT 'N' COMMENT '系统内置（Y是 N否）',
                              `config_desc` varchar(500) DEFAULT NULL COMMENT '参数描述',
                              `sort` int DEFAULT 0 COMMENT '排序',
                              `create_by` bigint DEFAULT NULL COMMENT '创建者',
                              `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                              `update_by` bigint DEFAULT NULL COMMENT '更新者',
                              `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                              PRIMARY KEY (`config_id`),
                              UNIQUE KEY `uk_tenant_config_key` (`tenant_id`,`config_key`),
                              KEY `idx_config_type` (`config_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- 12. 字典类型表（sys_dict_type）
CREATE TABLE `sys_dict_type` (
                                 `dict_id` bigint NOT NULL AUTO_INCREMENT COMMENT '字典主键',
                                 `tenant_id` bigint DEFAULT '000000' COMMENT '租户编号',
                                 `dict_name` varchar(100) DEFAULT '' COMMENT '字典名称',
                                 `dict_type` varchar(100) DEFAULT '' COMMENT '字典类型（租户内唯一）',
                                 `dict_status` tinyint DEFAULT 1 COMMENT '字典状态（0-禁用，1-正常）',
                                 `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                                 `create_by` bigint DEFAULT NULL COMMENT '创建者',
                                 `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                 `update_by` bigint DEFAULT NULL COMMENT '更新者',
                                 `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                 PRIMARY KEY (`dict_id`),
                                 UNIQUE KEY `uk_tenant_dict_type` (`tenant_id`,`dict_type`),
                                 KEY `idx_dict_status` (`dict_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典类型表';

-- 13. 字典数据表（sys_dict_data）
CREATE TABLE `sys_dict_data` (
                                 `dict_code` bigint NOT NULL AUTO_INCREMENT COMMENT '字典编码',
                                 `tenant_id` bigint DEFAULT '000000' COMMENT '租户编号',
                                 `dict_sort` int DEFAULT '0' COMMENT '字典排序',
                                 `dict_label` varchar(100) DEFAULT '' COMMENT '字典标签',
                                 `dict_value` varchar(100) DEFAULT '' COMMENT '字典键值',
                                 `dict_type` varchar(100) DEFAULT '' COMMENT '字典类型',
                                 `css_class` varchar(100) DEFAULT NULL COMMENT '样式属性（其他样式扩展）',
                                 `list_class` varchar(100) DEFAULT NULL COMMENT '表格回显样式',
                                 `is_default` char(1) DEFAULT 'N' COMMENT '是否默认（Y是 N否）',
                                 `parent_dict_code` bigint unsigned DEFAULT NULL COMMENT '上级字典编码',
                                 `linked_dict_type` varchar(100) DEFAULT NULL COMMENT '关联的字典类型',
                                 `linked_dict_value` varchar(100) DEFAULT NULL COMMENT '关联的字典值',
                                 `dict_status` tinyint DEFAULT 1 COMMENT '字典状态（0-禁用，1-正常）',
                                 `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                                 `create_by` bigint DEFAULT NULL COMMENT '创建者',
                                 `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                 `update_by` bigint DEFAULT NULL COMMENT '更新者',
                                 `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                 PRIMARY KEY (`dict_code`),
                                 UNIQUE KEY `uk_tenant_dict_data` (`tenant_id`,`dict_type`,`dict_value`),
                                 KEY `idx_tenant_dict_type` (`tenant_id`,`dict_type`),
                                 KEY `idx_parent_dict_code` (`parent_dict_code`),
                                 KEY `idx_linked_dict` (`linked_dict_type`,`linked_dict_value`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典数据表';

-- 16. 操作日志表（sys_operation_log）
CREATE TABLE `sys_operation_log` (
                                     `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
                                     `tenant_id` bigint DEFAULT '000000' COMMENT '租户编号',
                                     `user_id` bigint DEFAULT NULL COMMENT '操作用户ID',
                                     `username` varchar(50) DEFAULT NULL COMMENT '操作用户名',
                                     `operation_module` varchar(100) DEFAULT NULL COMMENT '操作模块',
                                     `operation_type` varchar(50) DEFAULT NULL COMMENT '操作类型（ADD/UPDATE/DELETE/QUERY/EXPORT/IMPORT）',
                                     `operation_desc` varchar(500) DEFAULT NULL COMMENT '操作描述',
                                     `request_method` varchar(10) DEFAULT NULL COMMENT '请求方式（GET/POST/PUT/DELETE）',
                                     `request_url` varchar(500) DEFAULT NULL COMMENT '请求URL',
                                     `request_params` text DEFAULT NULL COMMENT '请求参数',
                                     `response_result` text DEFAULT NULL COMMENT '响应结果',
                                     `error_msg` text DEFAULT NULL COMMENT '错误信息',
                                     `operation_status` tinyint DEFAULT 1 COMMENT '操作状态（0-失败，1-成功）',
                                     `operation_ip` varchar(50) DEFAULT NULL COMMENT '操作IP',
                                     `operation_location` varchar(255) DEFAULT NULL COMMENT '操作地点',
                                     `user_agent` varchar(500) DEFAULT NULL COMMENT '用户代理',
                                     `execute_time` bigint DEFAULT 0 COMMENT '执行时长（毫秒）',
                                     `operation_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
                                     PRIMARY KEY (`id`),
                                     KEY `idx_tenant_user` (`tenant_id`,`user_id`),
                                     KEY `idx_operation_time` (`operation_time`),
                                     KEY `idx_operation_status` (`operation_status`),
                                     KEY `idx_request_url` (`request_url`(255))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- 17. 登录日志表（sys_login_log）
CREATE TABLE `sys_login_log` (
                                 `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
                                 `tenant_id` bigint DEFAULT '000000' COMMENT '租户编号',
                                 `user_id` bigint DEFAULT NULL COMMENT '用户ID',
                                 `username` varchar(50) DEFAULT NULL COMMENT '用户名',
                                 `login_type` varchar(50) DEFAULT NULL COMMENT '登录类型（LOGIN/LOGOUT/REGISTER）',
                                 `login_status` tinyint DEFAULT 1 COMMENT '登录状态（0-失败，1-成功）',
                                 `login_ip` varchar(50) DEFAULT NULL COMMENT '登录IP',
                                 `login_location` varchar(255) DEFAULT NULL COMMENT '登录地点',
                                 `browser` varchar(100) DEFAULT NULL COMMENT '浏览器',
                                 `os` varchar(100) DEFAULT NULL COMMENT '操作系统',
                                 `user_agent` varchar(500) DEFAULT NULL COMMENT '用户代理',
                                 `login_message` varchar(500) DEFAULT NULL COMMENT '登录信息',
                                 `login_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
                                 PRIMARY KEY (`id`),
                                 KEY `idx_tenant_user` (`tenant_id`,`user_id`),
                                 KEY `idx_login_time` (`login_time`),
                                 KEY `idx_login_status` (`login_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志表';
