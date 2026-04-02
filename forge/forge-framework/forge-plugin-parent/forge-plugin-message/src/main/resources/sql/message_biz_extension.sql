-- 消息业务关联字段扩展

-- 方案1：在消息表中添加业务字段（推荐）
ALTER TABLE `sys_message` 
ADD COLUMN `biz_type` varchar(50) DEFAULT NULL COMMENT '业务类型（如：ORDER、APPROVAL、TASK等）' AFTER `template_params`,
ADD COLUMN `biz_key` varchar(100) DEFAULT NULL COMMENT '业务主键（如：订单ID、流程实例ID等）' AFTER `biz_type`,
ADD INDEX `idx_biz` (`biz_type`, `biz_key`);

-- 方案2：创建消息业务类型配置表（可选）
CREATE TABLE `sys_message_biz_type` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id` bigint NOT NULL DEFAULT '000000' COMMENT '租户编号',
    `biz_type` varchar(50) NOT NULL COMMENT '业务类型编码',
    `biz_name` varchar(100) NOT NULL COMMENT '业务类型名称',
    `jump_url` varchar(500) DEFAULT NULL COMMENT '跳转URL模板，支持变量：${bizKey}、${messageId}',
    `jump_target` varchar(20) DEFAULT '_self' COMMENT '跳转方式：_self-当前页/_blank-新窗口',
    `icon` varchar(100) DEFAULT NULL COMMENT '图标',
    `sort` int DEFAULT 0 COMMENT '排序',
    `enabled` tinyint NOT NULL DEFAULT 1 COMMENT '是否启用：0-禁用/1-启用',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注说明',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` bigint DEFAULT NULL COMMENT '创建者',
    `update_by` bigint DEFAULT NULL COMMENT '更新者',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_type` (`tenant_id`, `biz_type`),
    KEY `idx_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息业务类型配置表';

-- 初始化示例数据
INSERT INTO sys_message_biz_type (tenant_id, biz_type, biz_name, jump_url, jump_target, icon, remark) VALUES
(000000, 'ORDER', '订单消息', '/order/detail?id=${bizKey}', '_self', 'order', '订单相关消息'),
(000000, 'APPROVAL', '审批消息', '/workflow/task?processInstanceId=${bizKey}', '_self', 'approval', '审批流程消息'),
(000000, 'TASK', '任务消息', '/task/detail?id=${bizKey}', '_self', 'task', '任务相关消息'),
(000000, 'ANNOUNCEMENT', '公告消息', '/notice/detail?id=${bizKey}', '_self', 'notice', '公告通知消息'),
(000000, 'SYSTEM', '系统消息', NULL, '_self', 'system', '系统通知，无需跳转');