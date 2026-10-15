-- 节点操作权限配置表（删除节点配置时会级联清理；本地/历史库可能缺失）
CREATE TABLE IF NOT EXISTS `sys_flow_node_operation` (
  `id` varchar(64) NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  `node_config_id` varchar(64) NOT NULL COMMENT '节点配置ID',
  `operation_code` varchar(50) NOT NULL COMMENT '操作代码',
  `operation_name` varchar(100) DEFAULT NULL COMMENT '操作名称',
  `enabled` tinyint NOT NULL DEFAULT 1 COMMENT '是否启用',
  `required_reason` tinyint NOT NULL DEFAULT 0 COMMENT '是否需要填写原因',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序号',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_node_config_id` (`node_config_id`),
  KEY `idx_tenant_node_config` (`tenant_id`, `node_config_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程节点操作权限配置';
