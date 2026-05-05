-- =============================================
-- SPEL表达式模板表
-- 用于流程节点配置的表达式模板管理
-- =============================================

CREATE TABLE IF NOT EXISTS `sys_flow_spel_template` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
    `template_name` varchar(100) NOT NULL COMMENT '模板名称',
    `template_code` varchar(50) NOT NULL COMMENT '模板编码',
    `expression` varchar(500) NOT NULL COMMENT 'SPEL表达式',
    `description` varchar(200) DEFAULT NULL COMMENT '描述说明',
    `category` varchar(50) DEFAULT 'general' COMMENT '分类：general/dept/role/region/custom',
    `example_params` varchar(500) DEFAULT NULL COMMENT '示例参数（JSON格式）',
    `status` int NOT NULL DEFAULT '1' COMMENT '状态：0-禁用，1-启用',
    `sort` int DEFAULT '100' COMMENT '排序',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` int DEFAULT '0' COMMENT '删除标志',
    
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_template_code` (`tenant_id`, `template_code`),
    KEY `idx_tenant_id` (`tenant_id`),
    KEY `idx_category` (`category`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程SPEL表达式模板';

-- 初始化默认模板数据（迁移自NodePropertiesPanel.vue硬编码模板）
INSERT INTO `sys_flow_spel_template` (`id`, `tenant_id`, `template_name`, `template_code`, `expression`, `description`, `category`, `status`, `sort`) VALUES
(1, 1, '发起人', 'initiator', '${initiator}', '流程发起人作为审批人', 'general', 1, 1),
(2, 1, '发起人上级', 'initiatorLeader', '${initiatorLeader}', '流程发起人的直属上级', 'general', 1, 2),
(3, 1, '部门经理', 'deptManager', '${deptManager}', '当前部门的经理', 'general', 1, 3),
(4, 1, 'HR', 'hr', '${hr}', '人力资源部门负责人', 'general', 1, 4),
(5, 1, '发起人的直属上级', 'getInitiatorLeader', '${flowSpelService.getInitiatorLeader(execution)}', '流程发起人的直属上级作为审批人', 'general', 1, 5),
(6, 1, '根据部门查找负责人', 'findDeptManager', '${flowSpelService.findDeptManager(execution.getVariable("deptId"))}', '查找指定部门的负责人', 'dept', 1, 10),
(7, 1, '部门+角色组合查询', 'findUsersByDeptAndRole', '${flowSpelService.findUsersByDeptAndRole(execution.getVariable("deptId"), "dept_manager")}', '查找指定部门的部门经理', 'dept', 1, 15),
(8, 1, '根据角色查找用户', 'findUsersByRole', '${flowSpelService.findUsersByRole(execution.getVariable("roleKey"))}', '查找具有指定角色的所有用户', 'role', 1, 20),
(9, 1, '根据行政区划查找负责人', 'findRegionManager', '${flowSpelService.findRegionManager(execution.getVariable("regionCode"))}', '查找指定行政区划的区域负责人', 'region', 1, 30),
(10, 1, '根据业务规则查找', 'findApproverByBusinessRule', '${flowSpelService.findApproverByBusinessRule(execution, "order")}', '根据复杂业务规则动态确定审批人', 'custom', 1, 40),
(11, 1, '根据订单金额动态审批', 'amountDynamicApproval', '${execution.getVariable("amount") > 10000 ? flowSpelService.findUsersByRole("finance_manager") : flowSpelService.findUsersByRole("finance_staff")}', '订单金额大于1万由财务经理审批，否则由财务专员审批', 'custom', 1, 50)
ON DUPLICATE KEY UPDATE `template_name` = VALUES(`template_name`);