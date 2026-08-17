-- 受管注解缓存的运行策略覆盖。代码注册定义仍是缓存名和安全作用域的权威来源。

CREATE TABLE IF NOT EXISTS sys_cache_policy (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '策略主键',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID，平台策略固定为1',
    application_code VARCHAR(64) NOT NULL COMMENT '应用编码',
    cache_name VARCHAR(128) NOT NULL COMMENT '代码注册的缓存名',
    enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用：0否 1是',
    cache_mode VARCHAR(16) NOT NULL COMMENT '缓存模式：LOCAL REDIS MULTI',
    local_ttl_seconds BIGINT NOT NULL COMMENT '本地缓存TTL（秒）',
    redis_ttl_seconds BIGINT NOT NULL COMMENT 'Redis缓存TTL（秒）',
    local_max_size INT NOT NULL COMMENT '本地缓存最大条目数',
    cache_null TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否缓存空值：0否 1是',
    null_ttl_seconds BIGINT NOT NULL COMMENT '空值TTL（秒）',
    policy_version BIGINT NOT NULL DEFAULT 1 COMMENT '乐观锁版本',
    del_flag BIGINT NOT NULL DEFAULT 0 COMMENT '删除标志：0正常，删除后写主键',
    create_by BIGINT NULL COMMENT '创建人',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    create_dept BIGINT NULL COMMENT '创建部门',
    update_by BIGINT NULL COMMENT '更新人',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_cache_policy_active (tenant_id, application_code, cache_name, del_flag),
    KEY idx_cache_policy_lookup (tenant_id, application_code, cache_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='受管缓存运行策略覆盖';

SET @managed_cache_menu_id = (
    SELECT id
    FROM sys_resource
    WHERE tenant_id = 1
      AND resource_type = 2
      AND path = '/system/cache'
      AND del_flag = 0
    ORDER BY id
    LIMIT 1
);

INSERT INTO sys_resource (
    tenant_id, resource_name, parent_id, resource_type, sort,
    is_external, open_target, is_public, menu_status, visible, perms,
    api_method, api_url, keep_alive, always_show, remark,
    create_by, create_time, update_by, update_time, create_dept, client_code
)
SELECT 1, seed.resource_name, @managed_cache_menu_id, 4, seed.sort,
       0, '_self', 0, 1, 1, seed.perms,
       seed.api_method, seed.api_url, 0, 0, seed.remark,
       1, NOW(), 1, NOW(), 1, 'pc'
FROM (
    SELECT '受管缓存-分页查询' resource_name, 10 sort,
           'system:cache:policy:page' perms, 'GET' api_method,
           '/system/cache/policy/page' api_url, '查询代码注册的受管缓存和有效策略' remark
    UNION ALL
    SELECT '受管缓存-编辑策略', 11,
           'system:cache:policy:edit', 'POST',
           '/system/cache/policy/edit', '修改受管缓存运行策略覆盖'
    UNION ALL
    SELECT '受管缓存-恢复默认', 12,
           'system:cache:policy:reset', 'POST',
           '/system/cache/policy/reset', '删除策略覆盖并恢复代码默认值'
    UNION ALL
    SELECT '受管缓存-清空', 13,
           'system:cache:policy:clear', 'POST',
           '/system/cache/policy/clear', '清空指定受管缓存的全部条目'
) seed
WHERE @managed_cache_menu_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM sys_resource resource
      WHERE resource.tenant_id = 1
        AND resource.resource_type = 4
        AND resource.api_method = seed.api_method
        AND resource.api_url = seed.api_url
        AND resource.del_flag = 0
  );
