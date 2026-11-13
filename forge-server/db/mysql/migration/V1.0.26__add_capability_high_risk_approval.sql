-- Forge AI 中枢阶段 2.3：R3 高风险动作策略与人工审批。

CREATE TABLE IF NOT EXISTS `ai_capability_policy` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  `capability_id` bigint NOT NULL COMMENT '能力ID',
  `capability_version` varchar(32) NOT NULL COMMENT '不可变能力版本',
  `risk_level` varchar(16) NOT NULL COMMENT '风险等级，当前仅HIGH',
  `approval_flow_model_key` varchar(128) NOT NULL COMMENT '专用审批流程模型Key',
  `approval_candidate_group` varchar(64) NOT NULL COMMENT '服务端审批候选组',
  `expire_seconds` int NOT NULL DEFAULT 86400 COMMENT '审批有效秒数',
  `status` varchar(16) NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED',
  `del_flag` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志',
  `logic_delete_active` tinyint GENERATED ALWAYS AS (CASE WHEN `del_flag` = 0 THEN 1 ELSE NULL END) STORED COMMENT '未删除唯一约束',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_capability_policy_version` (`tenant_id`,`capability_id`,`capability_version`,`logic_delete_active`),
  KEY `idx_capability_policy_status` (`tenant_id`,`risk_level`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI能力风险与人工审批策略';

CREATE TABLE IF NOT EXISTS `ai_capability_approval` (
  `id` bigint NOT NULL COMMENT '审批请求ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  `request_id` varchar(64) NOT NULL COMMENT '原始Capability请求ID',
  `client_id` bigint NOT NULL COMMENT '机器客户端ID',
  `credential_version` int NOT NULL COMMENT '提交时客户端凭据版本',
  `capability_id` bigint NOT NULL COMMENT '能力ID',
  `capability_code` varchar(128) NOT NULL COMMENT '能力编码',
  `capability_version` varchar(32) NOT NULL COMMENT '能力版本',
  `actor_user_id` bigint NOT NULL COMMENT '实际操作用户ID',
  `service_user_id` bigint NOT NULL COMMENT '客户端服务账号ID',
  `active_org_id` bigint NOT NULL COMMENT '当前组织ID',
  `idempotency_key` varchar(128) NOT NULL COMMENT '原始幂等键',
  `request_digest` varchar(71) NOT NULL COMMENT '规范请求SHA-256摘要',
  `business_state_digest` varchar(71) DEFAULT NULL COMMENT '提交时业务状态摘要',
  `key_id` varchar(64) NOT NULL COMMENT 'KEK版本ID',
  `wrapped_dek` varchar(512) NOT NULL COMMENT '包装后的每记录DEK',
  `payload_iv` varchar(64) NOT NULL COMMENT 'AES-GCM IV',
  `payload_ciphertext` longtext NOT NULL COMMENT '请求密文',
  `payload_auth_tag` varchar(64) NOT NULL COMMENT 'AES-GCM认证标签',
  `flow_model_key` varchar(128) NOT NULL COMMENT '审批流程模型Key',
  `process_instance_id` varchar(128) DEFAULT NULL COMMENT 'Flowable实例ID',
  `execute_status` varchar(32) NOT NULL COMMENT '审批执行状态',
  `result_code` varchar(64) NOT NULL COMMENT '稳定结果码',
  `result_snapshot` varchar(2000) DEFAULT NULL COMMENT '安全结果摘要',
  `error_code` varchar(64) DEFAULT NULL COMMENT '稳定错误码',
  `expires_at` datetime NOT NULL COMMENT '审批过期时间',
  `completed_at` datetime DEFAULT NULL COMMENT '终态时间',
  `del_flag` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志',
  `logic_delete_active` tinyint GENERATED ALWAYS AS (CASE WHEN `del_flag` = 0 THEN 1 ELSE NULL END) STORED COMMENT '未删除唯一约束',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_capability_approval_idempotency` (`tenant_id`,`client_id`,`capability_id`,`idempotency_key`,`logic_delete_active`),
  UNIQUE KEY `uk_capability_approval_request` (`tenant_id`,`request_id`,`logic_delete_active`),
  KEY `idx_capability_approval_actor` (`tenant_id`,`actor_user_id`,`create_time`),
  KEY `idx_capability_approval_process` (`tenant_id`,`process_instance_id`),
  KEY `idx_capability_approval_status` (`tenant_id`,`execute_status`,`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI能力高风险动作人工审批';

SET @ai_parent_id := COALESCE((SELECT parent_id FROM (SELECT parent_id FROM sys_resource WHERE tenant_id = 1 AND path = '/ai/provider-model' AND del_flag = 0 LIMIT 1) x), 0);
INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, is_external, open_target,
                          is_public, menu_status, visible, perms, keep_alive, always_show, remark,
                          create_by, create_time, update_by, update_time, create_dept, client_code)
SELECT 1, seed.resource_name, @ai_parent_id, 3, seed.sort, 0, '_self', 0, 1, 1, seed.perms,
       0, 0, seed.remark, 1, NOW(), 1, NOW(), 1, 'pc'
FROM (
  SELECT '发布高风险动作' resource_name, 34 sort, 'ai:capability:high-risk:publish' perms, '发布HIGH业务动作与人工审批策略' remark
  UNION ALL SELECT '提交高风险审批', 35, 'ai:capability:approval:submit', '可信USER提交高风险人工审批'
  UNION ALL SELECT '查询高风险审批', 36, 'ai:capability:approval:query', '查询本人客户端审批状态'
) seed
WHERE NOT EXISTS (SELECT 1 FROM sys_resource r WHERE r.tenant_id = 1 AND r.perms = seed.perms AND r.del_flag = 0);

INSERT INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT 1, role.id, resource.id, NOW()
FROM (SELECT id FROM sys_role WHERE tenant_id = 1 AND role_key = 'admin' AND del_flag = 0 ORDER BY id LIMIT 1) role
JOIN sys_resource resource ON resource.tenant_id = 1 AND resource.del_flag = 0
WHERE resource.perms IN ('ai:capability:high-risk:publish','ai:capability:approval:submit','ai:capability:approval:query')
  AND NOT EXISTS (SELECT 1 FROM sys_role_resource rr WHERE rr.tenant_id = 1 AND rr.role_id = role.id AND rr.resource_id = resource.id);
