-- =============================================
-- 请假流程示例 - 数据库脚本
-- =============================================

-- 1. 创建请假业务表
DROP TABLE IF EXISTS `biz_leave_request`;
CREATE TABLE `biz_leave_request` (
  `id` varchar(64) NOT NULL COMMENT '主键ID',
  `business_key` varchar(64) NOT NULL COMMENT '业务Key（关联流程）',
  `process_instance_id` varchar(64) DEFAULT NULL COMMENT '流程实例ID',
  
  -- 申请人信息
  `apply_user_id` varchar(64) NOT NULL COMMENT '申请人ID',
  `apply_user_name` varchar(100) NOT NULL COMMENT '申请人姓名',
  `apply_dept_id` varchar(64) DEFAULT NULL COMMENT '申请部门ID',
  `apply_dept_name` varchar(200) DEFAULT NULL COMMENT '申请部门名称',
  
  -- 请假信息
  `leave_type` varchar(20) NOT NULL COMMENT '请假类型：annual-年假/sick-病假/personal-事假/marriage-婚假/maternity-产假',
  `start_time` datetime NOT NULL COMMENT '开始时间',
  `end_time` datetime NOT NULL COMMENT '结束时间',
  `duration` decimal(10,1) NOT NULL COMMENT '请假天数',
  `reason` text COMMENT '请假原因',
  `attachments` text COMMENT '附件JSON列表',
  
  -- 审批信息
  `status` varchar(20) NOT NULL DEFAULT 'draft' COMMENT '状态：draft-草稿/pending-审批中/approved-已通过/rejected-已驳回/canceled-已取消',
  `approve_user_id` varchar(64) DEFAULT NULL COMMENT '审批人ID',
  `approve_user_name` varchar(100) DEFAULT NULL COMMENT '审批人姓名',
  `approve_time` datetime DEFAULT NULL COMMENT '审批时间',
  `approve_comment` text COMMENT '审批意见',
  `approve_attachments` text COMMENT '审批附件JSON',
  
  -- 系统字段
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `deleted` tinyint(1) DEFAULT 0 COMMENT '删除标记',
  
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_business_key` (`business_key`),
  KEY `idx_apply_user` (`apply_user_id`),
  KEY `idx_process_instance` (`process_instance_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='请假申请表';

-- 2. 创建请假管理菜单（sys_resource表）
-- 注意：id 需要根据实际情况调整，避免与现有资源冲突
-- resource_type: 1-目录，2-菜单，3-按钮，4-API接口

-- 2.1 请假管理目录
INSERT INTO `sys_resource` (`id`, `tenant_id`, `resource_name`, `parent_id`, `resource_type`, `sort`, `path`, `component`, `is_external`, `is_public`, `menu_status`, `visible`, `perms`, `icon`, `keep_alive`, `always_show`, `remark`, `create_time`)
VALUES (900, 0, '请假管理', 0, 1, 90, '/leave', NULL, 0, 0, 1, 1, NULL, 'calendar-clock', 0, 0, '请假管理目录', NOW());

-- 2.2 请假申请页面
INSERT INTO `sys_resource` (`id`, `tenant_id`, `resource_name`, `parent_id`, `resource_type`, `sort`, `path`, `component`, `is_external`, `is_public`, `menu_status`, `visible`, `perms`, `icon`, `keep_alive`, `always_show`, `remark`, `create_time`)
VALUES (901, 0, '请假申请', 900, 2, 1, 'apply', '/leave/apply', 0, 0, 1, 1, 'leave:apply:view', 'plus-circle', 0, 0, '请假申请页面', NOW());

-- 2.3 我的请假列表
INSERT INTO `sys_resource` (`id`, `tenant_id`, `resource_name`, `parent_id`, `resource_type`, `sort`, `path`, `component`, `is_external`, `is_public`, `menu_status`, `visible`, `perms`, `icon`, `keep_alive`, `always_show`, `remark`, `create_time`)
VALUES (902, 0, '我的请假', 900, 2, 2, 'list', '/leave/list', 0, 0, 1, 1, 'leave:list:view', 'format-list-bulleted', 0, 0, '我的请假列表页面', NOW());

-- 3. 创建按钮权限（resource_type=3）
-- 3.1 请假申请按钮
INSERT INTO `sys_resource` (`id`, `tenant_id`, `resource_name`, `parent_id`, `resource_type`, `sort`, `visible`, `perms`, `remark`, `create_time`)
VALUES (9011, 0, '提交申请', 901, 3, 1, 1, 'leave:apply:submit', '', NOW());

INSERT INTO `sys_resource` (`id`, `tenant_id`, `resource_name`, `parent_id`, `resource_type`, `sort`, `visible`, `perms`, `remark`, `create_time`)
VALUES (9012, 0, '保存草稿', 901, 3, 2, 1, 'leave:apply:draft', '', NOW());

-- 3.2 请假列表按钮
INSERT INTO `sys_resource` (`id`, `tenant_id`, `resource_name`, `parent_id`, `resource_type`, `sort`, `visible`, `perms`, `remark`, `create_time`)
VALUES (9021, 0, '查看详情', 902, 3, 1, 1, 'leave:list:detail', '', NOW());

INSERT INTO `sys_resource` (`id`, `tenant_id`, `resource_name`, `parent_id`, `resource_type`, `sort`, `visible`, `perms`, `remark`, `create_time`)
VALUES (9022, 0, '撤销申请', 902, 3, 2, 1, 'leave:list:cancel', '', NOW());

INSERT INTO `sys_resource` (`id`, `tenant_id`, `resource_name`, `parent_id`, `resource_type`, `sort`, `visible`, `perms`, `remark`, `create_time`)
VALUES (9023, 0, '删除申请', 902, 3, 3, 1, 'leave:list:delete', '', NOW());

-- 4. 创建API接口权限（resource_type=4）
-- 4.1 请假申请相关API
INSERT INTO `sys_resource` (`id`, `tenant_id`, `resource_name`, `parent_id`, `resource_type`, `sort`, `visible`, `perms`, `api_method`, `api_url`, `remark`, `create_time`)
VALUES (9031, 0, '提交请假申请', 901, 4, 1, 1, 'leave:apply:submit', 'POST', '/leave/submit', '', NOW());

INSERT INTO `sys_resource` (`id`, `tenant_id`, `resource_name`, `parent_id`, `resource_type`, `sort`, `visible`, `perms`, `api_method`, `api_url`, `remark`, `create_time`)
VALUES (9032, 0, '保存草稿', 901, 4, 2, 1, 'leave:apply:draft', 'POST', '/leave/draft', '', NOW());

INSERT INTO `sys_resource` (`id`, `tenant_id`, `resource_name`, `parent_id`, `resource_type`, `sort`, `visible`, `perms`, `api_method`, `api_url`, `remark`, `create_time`)
VALUES (9033, 0, '更新草稿', 901, 4, 3, 1, 'leave:apply:draft', 'PUT', '/leave/draft', '', NOW());

-- 4.2 请假列表相关API
INSERT INTO `sys_resource` (`id`, `tenant_id`, `resource_name`, `parent_id`, `resource_type`, `sort`, `visible`, `perms`, `api_method`, `api_url`, `remark`, `create_time`)
VALUES (9041, 0, '获取请假详情', 902, 4, 1, 1, 'leave:list:detail', 'GET', '/leave/detail/*', '', NOW());

INSERT INTO `sys_resource` (`id`, `tenant_id`, `resource_name`, `parent_id`, `resource_type`, `sort`, `visible`, `perms`, `api_method`, `api_url`, `remark`, `create_time`)
VALUES (9042, 0, '分页查询请假列表', 902, 4, 2, 1, 'leave:list:view', 'GET', '/leave/page', '', NOW());

INSERT INTO `sys_resource` (`id`, `tenant_id`, `resource_name`, `parent_id`, `resource_type`, `sort`, `visible`, `perms`, `api_method`, `api_url`, `remark`, `create_time`)
VALUES (9043, 0, '撤销申请', 902, 4, 3, 1, 'leave:list:cancel', 'POST', '/leave/cancel/*', '', NOW());

INSERT INTO `sys_resource` (`id`, `tenant_id`, `resource_name`, `parent_id`, `resource_type`, `sort`, `visible`, `perms`, `api_method`, `api_url`, `remark`, `create_time`)
VALUES (9044, 0, '删除申请', 902, 4, 4, 1, 'leave:list:delete', 'DELETE', '/leave/*', '', NOW());

-- 5. 给管理员角色分配权限（需要根据实际角色资源关联表结构调整）
-- INSERT INTO `sys_role_resource` (`role_id`, `resource_id`) VALUES (1, 900);
-- INSERT INTO `sys_role_resource` (`role_id`, `resource_id`) VALUES (1, 901);
-- INSERT INTO `sys_role_resource` (`role_id`, `resource_id`) VALUES (1, 902);
-- INSERT INTO `sys_role_resource` (`role_id`, `resource_id`) VALUES (1, 9011);
-- INSERT INTO `sys_role_resource` (`role_id`, `resource_id`) VALUES (1, 9012);
-- INSERT INTO `sys_role_resource` (`role_id`, `resource_id`) VALUES (1, 9021);
-- INSERT INTO `sys_role_resource` (`role_id`, `resource_id`) VALUES (1, 9022);
-- INSERT INTO `sys_role_resource` (`role_id`, `resource_id`) VALUES (1, 9023);