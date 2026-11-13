-- 统一能力开放平台（一期）：REST 开放网关结构变更与内置数据。
-- 全部内置数据 tenant_id=1，具备防重复保护；不写入任何 Secret。

-- ============================================================
-- 1. ai_capability 新增 required_actor_type（存量 FLOW_ACTION 回填 USER）
-- ============================================================

SET @column_exists = (
  SELECT COUNT(1) FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'ai_capability'
    AND column_name = 'required_actor_type'
);
SET @sql = IF(@column_exists = 0,
  'ALTER TABLE ai_capability ADD COLUMN required_actor_type varchar(16) NOT NULL DEFAULT ''SERVICE'' COMMENT ''要求的调用主体类型：SERVICE/USER/BOTH'' AFTER risk_level',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE ai_capability
SET required_actor_type = 'USER'
WHERE source_type = 'FLOW_ACTION'
  AND required_actor_type = 'SERVICE';

-- ============================================================
-- 2. ai_capability_version 新增 required_actor_type（版本快照同步）
-- ============================================================

SET @column_exists = (
  SELECT COUNT(1) FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'ai_capability_version'
    AND column_name = 'required_actor_type'
);
SET @sql = IF(@column_exists = 0,
  'ALTER TABLE ai_capability_version ADD COLUMN required_actor_type varchar(16) NOT NULL DEFAULT ''SERVICE'' COMMENT ''要求的调用主体类型：SERVICE/USER/BOTH'' AFTER risk_level',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE ai_capability_version
SET required_actor_type = 'USER'
WHERE source_type = 'FLOW_ACTION'
  AND required_actor_type = 'SERVICE';

-- ============================================================
-- 3. ai_capability_client 新增认证模式与签名密钥列
-- ============================================================

SET @column_exists = (
  SELECT COUNT(1) FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'ai_capability_client'
    AND column_name = 'auth_modes'
);
SET @sql = IF(@column_exists = 0,
  'ALTER TABLE ai_capability_client ADD COLUMN auth_modes varchar(64) NOT NULL DEFAULT ''OAUTH'' COMMENT ''允许的认证模式，逗号分隔：OAUTH/SIGNATURE'' AFTER active_org_id',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (
  SELECT COUNT(1) FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'ai_capability_client'
    AND column_name = 'signing_key_cipher'
);
SET @sql = IF(@column_exists = 0,
  'ALTER TABLE ai_capability_client ADD COLUMN signing_key_cipher varchar(512) DEFAULT NULL COMMENT ''KEK加密的HMAC签名密钥密文（FPC1版本化密文，禁止明文）'' AFTER auth_modes',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (
  SELECT COUNT(1) FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'ai_capability_client'
    AND column_name = 'signing_key_version'
);
SET @sql = IF(@column_exists = 0,
  'ALTER TABLE ai_capability_client ADD COLUMN signing_key_version int DEFAULT NULL COMMENT ''签名密钥轮换版本，NULL表示未启用签名'' AFTER signing_key_cipher',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================================
-- 4. 开放网关幂等记录表（墓碑逻辑删除：0正常，删除后写主键）
-- ============================================================

CREATE TABLE IF NOT EXISTS ai_capability_openapi_idempotency (
  id bigint NOT NULL COMMENT '主键ID',
  tenant_id bigint NOT NULL DEFAULT 1 COMMENT '租户编号',
  client_id bigint NOT NULL COMMENT '机器客户端ID',
  capability_id bigint NOT NULL COMMENT '能力ID',
  idempotency_key_hash char(64) NOT NULL COMMENT 'Idempotency-Key 的 SHA-256 哈希',
  request_id varchar(64) NOT NULL COMMENT '首次调用请求ID',
  response_snapshot json DEFAULT NULL COMMENT '首次网关统一响应快照',
  expires_at datetime NOT NULL COMMENT '快照过期时间（超期由清理任务物理删除）',
  del_flag bigint NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0正常，删除后写主键',
  create_by bigint DEFAULT NULL COMMENT '创建人ID',
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  create_dept bigint DEFAULT NULL COMMENT '创建组织ID',
  update_by bigint DEFAULT NULL COMMENT '更新人ID',
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_ai_capability_openapi_idem (tenant_id, client_id, capability_id, idempotency_key_hash, del_flag),
  KEY idx_ai_capability_openapi_idem_expire (expires_at, del_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='能力开放网关幂等记录';

-- ============================================================
-- 5. 字典类型
-- ============================================================

INSERT INTO sys_dict_type (
  tenant_id, dict_name, dict_type, dict_status, remark,
  create_by, create_time, update_by, update_time, create_dept
)
SELECT seed.tenant_id, seed.dict_name, seed.dict_type, 1, seed.remark,
       1, NOW(), 1, NOW(), 1
FROM (
  SELECT 1 tenant_id, 'AI能力调用主体类型' dict_name, 'ai_capability_actor_type' dict_type, '能力要求的调用主体类型' remark
  UNION ALL SELECT 1, 'AI能力认证模式', 'ai_capability_auth_mode', '机器客户端允许的开放网关认证模式'
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM sys_dict_type t
  WHERE t.tenant_id = seed.tenant_id AND t.dict_type = seed.dict_type
);

-- ============================================================
-- 6. 字典数据
-- ============================================================

INSERT INTO sys_dict_data (
  tenant_id, dict_sort, dict_label, dict_value, dict_type,
  css_class, list_class, is_default, dict_status, remark,
  create_by, create_time, update_by, update_time, create_dept
)
SELECT seed.tenant_id, seed.dict_sort, seed.dict_label, seed.dict_value, seed.dict_type,
       NULL, seed.list_class, seed.is_default, 1, seed.remark,
       1, NOW(), 1, NOW(), 1
FROM (
  SELECT 1 tenant_id, 1 dict_sort, '机器身份' dict_label, 'SERVICE' dict_value,
         'ai_capability_actor_type' dict_type, 'info' list_class, 'Y' is_default, '仅机器客户端身份可调用' remark
  UNION ALL SELECT 1, 2, '用户委托', 'USER', 'ai_capability_actor_type', 'warning', 'N', '仅用户委托Token可调用'
  UNION ALL SELECT 1, 3, '双身份', 'BOTH', 'ai_capability_actor_type', 'success', 'N', '机器身份与用户委托均可调用'
  UNION ALL SELECT 1, 1, 'OAuth凭据', 'OAUTH', 'ai_capability_auth_mode', 'info', 'Y', 'OAuth2.1客户端凭据换取短期Token'
  UNION ALL SELECT 1, 2, '请求签名', 'SIGNATURE', 'ai_capability_auth_mode', 'warning', 'N', 'AppId+HMAC-SHA256请求签名'
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM sys_dict_data d
  WHERE d.tenant_id = seed.tenant_id
    AND d.dict_type = seed.dict_type
    AND d.dict_value = seed.dict_value
);

-- ============================================================
-- 7. 菜单资源：开放平台一级目录与 4 个页面
-- ============================================================

INSERT INTO sys_resource (
  tenant_id, resource_name, parent_id, resource_type, sort,
  path, component, is_external, open_target, is_public, menu_status, visible,
  perms, icon, keep_alive, always_show, remark,
  create_by, create_time, update_by, update_time, create_dept, client_code
)
SELECT 1, '开放平台', 0, 1, 96,
       '/open-platform', NULL, 0, '_self', 0, 1, 1,
       NULL, 'ionicons5:PlanetOutline', 0, 1, '统一能力开放平台管理目录',
       1, NOW(), 1, NOW(), 1, 'pc'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_resource r
  WHERE r.tenant_id = 1 AND r.resource_type = 1 AND r.path = '/open-platform' AND r.del_flag = 0
);

SET @open_platform_dir_id = (
  SELECT id FROM sys_resource
  WHERE tenant_id = 1 AND resource_type = 1 AND path = '/open-platform' AND del_flag = 0
  ORDER BY id LIMIT 1
);

INSERT INTO sys_resource (
  tenant_id, resource_name, parent_id, resource_type, sort,
  path, component, is_external, open_target, is_public, menu_status, visible,
  perms, icon, keep_alive, always_show, remark,
  create_by, create_time, update_by, update_time, create_dept, client_code
)
SELECT 1, seed.resource_name, @open_platform_dir_id, 2, seed.sort,
       seed.path, seed.component, 0, '_self', 0, 1, 1,
       seed.perms, seed.icon, 1, 0, seed.remark,
       1, NOW(), 1, NOW(), 1, 'pc'
FROM (
  SELECT '能力目录' resource_name, 1 sort, '/open-platform/capability-catalog' path,
         'ai/capability/catalog' component, 'ai:capability:query' perms,
         'ionicons5:LibraryOutline' icon, '已发布能力目录与版本查看' remark
  UNION ALL SELECT '机器客户端', 2, '/open-platform/capability-client', 'ai/capability/client',
         'ai:capability:client:query', 'ionicons5:HardwareChipOutline', '机器客户端与凭据/签名密钥管理'
  UNION ALL SELECT '授权管理', 3, '/open-platform/capability-grant', 'ai/capability/grant',
         'ai:capability:grant:query', 'ionicons5:KeyOutline', '客户端能力授权与字段策略管理'
  UNION ALL SELECT '调用日志', 4, '/open-platform/capability-invocation', 'ai/capability/invocation',
         'ai:capability:invocation:query', 'ionicons5:ReceiptOutline', '能力安全调用日志查询'
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM sys_resource r
  WHERE r.tenant_id = 1 AND r.resource_type = 2 AND r.path = seed.path AND r.del_flag = 0
);

-- ============================================================
-- 8. 角色授权：仅显式授予默认租户超级管理员
-- ============================================================

INSERT INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT 1, admin_role.id, resource.id, NOW()
FROM (SELECT id FROM sys_role WHERE tenant_id = 1 AND role_key = 'admin' AND del_flag = 0 ORDER BY id LIMIT 1) admin_role
JOIN sys_resource resource ON resource.tenant_id = 1 AND resource.del_flag = 0
WHERE resource.client_code = 'pc'
  AND (
    resource.path = '/open-platform'
    OR (resource.resource_type = 2 AND resource.path LIKE '/open-platform/%')
  )
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_resource existing
    WHERE existing.tenant_id = 1
      AND existing.role_id = admin_role.id
      AND existing.resource_id = resource.id
  );

-- ============================================================
-- 9. Job 配置：幂等快照清理任务，默认停用，网关启用后再开启
-- ============================================================

INSERT INTO sys_job_config (
  job_name, job_group, description, executor_handler,
  schedule_type, cron_expression, timezone, status,
  execute_mode, invoke_mode, concurrent_policy, misfire_policy,
  idempotent_flag, retry_count
)
SELECT seed.job_name, 'CAPABILITY', seed.description, seed.executor_handler,
       'CRON', seed.cron_expression, 'Asia/Shanghai', 0,
       'HANDLER', 'SINGLE', 'SKIP_IF_RUNNING', 'DO_NOTHING',
       1, 0
FROM (
  SELECT '开放网关幂等快照清理' job_name, 'capabilityOpenapiIdempotencyClean' executor_handler,
         '0 0 * * * ?' cron_expression, '物理清理超期的开放网关幂等响应快照；启用开放网关后手动启用' description
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM sys_job_config job
  WHERE job.job_name = seed.job_name
    AND job.job_group = 'CAPABILITY'
    AND job.del_flag = 0
);

-- ============================================================
-- 10. 按钮资源：机器客户端签名密钥维护权限
-- ============================================================

SET @ai_parent_id := COALESCE(
  (SELECT parent_id FROM (
    SELECT parent_id FROM sys_resource
    WHERE tenant_id = 1 AND path = '/ai/provider-model' AND del_flag = 0
    LIMIT 1
  ) x),
  0
);

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, is_external, open_target, is_public, menu_status, visible, perms, keep_alive, always_show, remark, create_by, create_time, update_by, update_time, create_dept, client_code)
SELECT 1, '维护客户端签名密钥', @ai_parent_id, 3, 30, 0, '_self', 0, 1, 1, 'ai:capability:client:edit', 0, 0, '生成或轮换机器客户端签名密钥', 1, NOW(), 1, NOW(), 1, 'pc'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_resource r
  WHERE r.tenant_id = 1 AND r.perms = 'ai:capability:client:edit' AND r.del_flag = 0
);

INSERT INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT 1, admin_role.id, resource.id, NOW()
FROM (SELECT id FROM sys_role WHERE tenant_id = 1 AND role_key = 'admin' AND del_flag = 0 ORDER BY id LIMIT 1) admin_role
JOIN sys_resource resource ON resource.tenant_id = 1 AND resource.del_flag = 0
WHERE resource.perms = 'ai:capability:client:edit'
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_resource existing
    WHERE existing.tenant_id = 1
      AND existing.role_id = admin_role.id
      AND existing.resource_id = resource.id
  );
