-- 流程条件规则表
-- 用于存储流程流转条件的可视化配置
DROP TABLE IF EXISTS `sys_flow_condition_rule`;
CREATE TABLE `sys_flow_condition_rule` (
    `id` VARCHAR(64) NOT NULL COMMENT '主键ID',
    `rule_name` VARCHAR(100) NOT NULL COMMENT '规则名称',
    `rule_code` VARCHAR(100) DEFAULT NULL COMMENT '规则编码',
    `model_id` VARCHAR(64) DEFAULT NULL COMMENT '模型ID',
    `sequence_flow_id` VARCHAR(128) NOT NULL COMMENT '序列流ID（BPMN中的SequenceFlow ID）',
    `condition_type` VARCHAR(32) DEFAULT 'simple' COMMENT '条件类型：simple-简单条件，composite-组合条件，script-脚本',
    `condition_expression` TEXT DEFAULT NULL COMMENT '条件表达式（JSON格式存储）',
    `priority` INT DEFAULT 0 COMMENT '优先级（数字越小优先级越高）',
    `is_default` TINYINT(1) DEFAULT 0 COMMENT '是否默认路径',
    `status` INT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) DEFAULT NULL COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT(1) DEFAULT 0 COMMENT '删除标志',
    PRIMARY KEY (`id`),
    KEY `idx_model_id` (`model_id`),
    KEY `idx_sequence_flow_id` (`sequence_flow_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程条件规则表';

-- 条件规则项表
-- 用于存储组合条件中的单个条件项
DROP TABLE IF EXISTS `sys_flow_condition_item`;
CREATE TABLE `sys_flow_condition_item` (
    `id` VARCHAR(64) NOT NULL COMMENT '主键ID',
    `rule_id` VARCHAR(64) NOT NULL COMMENT '规则ID',
    `field_name` VARCHAR(100) NOT NULL COMMENT '字段名称',
    `field_label` VARCHAR(100) DEFAULT NULL COMMENT '字段标签',
    `field_type` VARCHAR(32) DEFAULT 'string' COMMENT '字段类型：string/number/date/boolean/user/dept/role',
    `operator` VARCHAR(32) NOT NULL COMMENT '操作符：eq/ne/gt/lt/ge/le/contains/startsWith/endsWith/in/notIn/isEmpty/isNotEmpty',
    `value` TEXT DEFAULT NULL COMMENT '比较值（JSON格式，支持多值）',
    `logic_connector` VARCHAR(10) DEFAULT 'and' COMMENT '逻辑连接符：and/or',
    `group_id` VARCHAR(64) DEFAULT NULL COMMENT '分组ID（用于条件分组）',
    `sort_order` INT DEFAULT 0 COMMENT '排序号',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_rule_id` (`rule_id`),
    KEY `idx_group_id` (`group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='条件规则项表';