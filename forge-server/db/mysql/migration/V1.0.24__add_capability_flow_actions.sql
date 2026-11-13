-- Forge AI 中枢阶段 2.2：受控流程动作幂等日志与权限资源。

CREATE TABLE IF NOT EXISTS `ai_capability_flow_action_log` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  `request_id` varchar(64) NOT NULL COMMENT 'Capability请求ID',
  `client_id` bigint NOT NULL COMMENT '机器客户端ID',
  `capability_id` bigint NOT NULL COMMENT '能力ID',
  `capability_code` varchar(128) NOT NULL COMMENT '能力编码',
  `capability_version` varchar(32) NOT NULL COMMENT '能力版本',
  `operation` varchar(16) NOT NULL COMMENT '流程操作START/APPROVE/REJECT',
  `object_code` varchar(128) NOT NULL COMMENT '业务对象编码',
  `record_id` varchar(128) NOT NULL COMMENT '业务记录ID字符串',
  `task_ref` varchar(16) DEFAULT NULL COMMENT '任务ID安全摘要',
  `idempotency_key` varchar(128) NOT NULL COMMENT '调用方幂等键',
  `request_digest` varchar(71) NOT NULL COMMENT '规范请求SHA-256摘要',
  `actor_type` varchar(16) NOT NULL COMMENT '实际主体类型',
  `actor_user_id` bigint NOT NULL COMMENT '实际操作用户ID',
  `service_user_id` bigint NOT NULL COMMENT '客户端绑定服务账号ID',
  `active_org_id` bigint NOT NULL COMMENT '当前组织ID',
  `execute_status` varchar(32) NOT NULL COMMENT 'RUNNING/SUCCESS/FAILED',
  `result_code` varchar(64) NOT NULL COMMENT '稳定结果码',
  `result_snapshot` varchar(2000) DEFAULT NULL COMMENT '安全结果摘要JSON',
  `error_code` varchar(64) DEFAULT NULL COMMENT '稳定错误码',
  `duration_ms` bigint NOT NULL DEFAULT 0 COMMENT '耗时毫秒',
  `del_flag` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志（0未删除 1已删除）',
  `logic_delete_active` tinyint GENERATED ALWAYS AS (CASE WHEN `del_flag` = 0 THEN 1 ELSE NULL END) STORED COMMENT '未删除唯一约束标识',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cap_flow_action_idempotency` (`tenant_id`, `client_id`, `capability_id`, `operation`, `idempotency_key`, `logic_delete_active`),
  UNIQUE KEY `uk_cap_flow_action_request` (`tenant_id`, `request_id`, `logic_delete_active`),
  KEY `idx_cap_flow_action_actor` (`tenant_id`, `actor_user_id`, `create_time`),
  KEY `idx_cap_flow_action_record` (`tenant_id`, `object_code`, `record_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI能力受控流程动作执行日志';

SET @ai_parent_id := COALESCE(
  (SELECT parent_id FROM (
    SELECT parent_id FROM sys_resource
    WHERE tenant_id = 1 AND path = '/ai/provider-model' AND del_flag = 0
    LIMIT 1
  ) x),
  0
);

INSERT INTO sys_resource (
  tenant_id, resource_name, parent_id, resource_type, sort,
  is_external, open_target, is_public, menu_status, visible, perms,
  keep_alive, always_show, remark, create_by, create_time,
  update_by, update_time, create_dept, client_code
)
SELECT 1, seed.resource_name, @ai_parent_id, 3, seed.sort,
       0, '_self', 0, 1, 1, seed.perms,
       0, 0, seed.remark, 1, NOW(), 1, NOW(), 1, 'pc'
FROM (
  SELECT '发布受控流程动作' resource_name, 32 sort,
         'ai:capability:flow-action:publish' perms,
         '从已发布业务对象与真实流程绑定创建受控流程能力' remark
  UNION ALL
  SELECT '调用受控流程动作', 33,
         'ai:capability:flow-action:invoke',
         '允许具体用户通过MCP确认后发起或办理流程'
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM sys_resource r
  WHERE r.tenant_id = 1 AND r.perms = seed.perms AND r.del_flag = 0
);

INSERT INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT 1, admin_role.id, resource.id, NOW()
FROM (SELECT id FROM sys_role WHERE tenant_id = 1 AND role_key = 'admin' AND del_flag = 0 ORDER BY id LIMIT 1) admin_role
JOIN sys_resource resource ON resource.tenant_id = 1 AND resource.del_flag = 0
WHERE resource.perms IN (
  'ai:capability:flow-action:publish',
  'ai:capability:flow-action:invoke'
)
AND NOT EXISTS (
  SELECT 1 FROM sys_role_resource existing
  WHERE existing.tenant_id = 1
    AND existing.role_id = admin_role.id
    AND existing.resource_id = resource.id
);
