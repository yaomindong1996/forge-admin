-- 组织上下文角色权限改造：角色适用组织与用户组织内角色授权。
-- 说明：实际 Flyway 迁移目录当前最新为 V1.0.0，本脚本按该目录继续编号。

CREATE TABLE IF NOT EXISTS sys_role_org (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    tenant_id bigint NOT NULL DEFAULT 1 COMMENT '租户编号',
    role_id bigint NOT NULL COMMENT '角色ID',
    org_id bigint NOT NULL COMMENT '组织ID',
    create_by bigint DEFAULT NULL COMMENT '创建者',
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    create_dept bigint DEFAULT NULL COMMENT '创建部门',
    update_by bigint DEFAULT NULL COMMENT '更新者',
    update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_org (tenant_id, role_id, org_id),
    KEY idx_org_role (tenant_id, org_id, role_id),
    KEY idx_role_id (role_id),
    KEY idx_org_id (org_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='角色适用组织范围表';

CREATE TABLE IF NOT EXISTS sys_user_org_role (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    tenant_id bigint NOT NULL DEFAULT 1 COMMENT '租户编号',
    user_id bigint NOT NULL COMMENT '用户ID',
    org_id bigint NOT NULL COMMENT '组织ID',
    role_id bigint NOT NULL COMMENT '角色ID',
    create_by bigint DEFAULT NULL COMMENT '创建者',
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    create_dept bigint DEFAULT NULL COMMENT '创建部门',
    update_by bigint DEFAULT NULL COMMENT '更新者',
    update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_org_role (tenant_id, user_id, org_id, role_id),
    KEY idx_user_org (tenant_id, user_id, org_id),
    KEY idx_org_role (tenant_id, org_id, role_id),
    KEY idx_role_id (role_id),
    KEY idx_user_id (user_id),
    KEY idx_org_id (org_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户组织内角色授权表';

INSERT INTO sys_role_org (
    tenant_id,
    role_id,
    org_id,
    create_time,
    update_time
)
SELECT r.tenant_id,
       r.id,
       o.id,
       NOW(),
       NOW()
FROM sys_role r
INNER JOIN sys_org o
    ON o.tenant_id = r.tenant_id
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_role_org ro
    WHERE ro.tenant_id = r.tenant_id
      AND ro.role_id = r.id
      AND ro.org_id = o.id
);

INSERT INTO sys_user_org_role (
    tenant_id,
    user_id,
    org_id,
    role_id,
    create_time,
    update_time
)
SELECT ur.tenant_id,
       ur.user_id,
       uo.org_id,
       ur.role_id,
       NOW(),
       NOW()
FROM sys_user_role ur
INNER JOIN sys_user_org uo
    ON uo.tenant_id = ur.tenant_id
   AND uo.user_id = ur.user_id
INNER JOIN sys_role_org ro
    ON ro.tenant_id = ur.tenant_id
   AND ro.role_id = ur.role_id
   AND ro.org_id = uo.org_id
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_user_org_role uor
    WHERE uor.tenant_id = ur.tenant_id
      AND uor.user_id = ur.user_id
      AND uor.org_id = uo.org_id
      AND uor.role_id = ur.role_id
);
