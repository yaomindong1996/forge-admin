-- AI 上下文配置表
CREATE TABLE `ai_context_config` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint DEFAULT 1 COMMENT '租户ID',
  `agent_code` varchar(64) NOT NULL COMMENT '关联Agent编码',
  `config_name` varchar(128) NOT NULL COMMENT '配置名称',
  `config_content` text NOT NULL COMMENT '上下文内容',
  `config_type` varchar(32) NOT NULL DEFAULT 'SPEC' COMMENT '类型：SPEC/SAMPLE/RULE',
  `sort` int DEFAULT 0 COMMENT '排序',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_agent_code` (`agent_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI上下文配置表';
