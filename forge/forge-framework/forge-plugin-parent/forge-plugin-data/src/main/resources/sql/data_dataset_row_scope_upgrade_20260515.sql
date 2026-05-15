-- 数据集行权限升级脚本
-- 适用于已初始化数据资产表的环境；新库请直接执行 data_tables.sql。

CREATE TABLE IF NOT EXISTS `ai_report_data_dataset_row_scope` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  `dataset_id` bigint NOT NULL COMMENT '数据集ID',
  `enabled` tinyint NOT NULL DEFAULT 0 COMMENT '是否启用：1是 0否',
  `scope_mode` varchar(32) NOT NULL DEFAULT 'SYSTEM_DATA_SCOPE' COMMENT '行权限模式：SYSTEM_DATA_SCOPE跟随系统数据权限',
  `tenant_column` varchar(128) DEFAULT NULL COMMENT '租户字段名/表达式',
  `org_column` varchar(128) DEFAULT NULL COMMENT '组织字段名/表达式',
  `user_column` varchar(128) DEFAULT NULL COMMENT '用户字段名/表达式',
  `region_column` varchar(128) DEFAULT NULL COMMENT '行政区划字段名/表达式',
  `region_strategy` varchar(32) NOT NULL DEFAULT 'SELF_AND_DESCENDANTS' COMMENT '行政区划策略：SELF/SELF_AND_CHILDREN/SELF_AND_DESCENDANTS',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_data_dataset_row_scope` (`tenant_id`, `dataset_id`),
  KEY `idx_data_dataset_row_scope_enabled` (`tenant_id`, `enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台数据集行权限配置';
