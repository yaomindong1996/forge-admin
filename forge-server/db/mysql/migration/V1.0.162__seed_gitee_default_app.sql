-- =====================================================================
-- V1.0.162 : 为 Gitee 连接预置默认应用 + LOGIN 能力绑定
-- 用途：V1.0.161 只创建了连接骨架，OAuth 凭据在应用维度管理，
--       本脚本创建默认应用（占位凭据）并绑定 LOGIN 能力，
--       管理员只需在应用管理中替换 clientId / secret 即可使用。
-- =====================================================================

-- 1. 插入默认应用（凭据占位，管理员在 UI 中替换）
INSERT INTO sys_social_app_config (
    tenant_id, connection_id, app_code, app_name,
    client_id, redirect_uri, scope, status,
    remark, del_flag,
    create_by, create_time, update_by, update_time, create_dept
)
SELECT
    1,
    c.id,
    'gitee-login-app',
    'Gitee OAuth 登录应用',
    'YOUR_CLIENT_ID',
    'http://localhost:8580/social/callback/GITEE',
    'user_info',
    1,
    '请替换 clientId 和 secret 为你在 Gitee 开放平台创建的应用凭据',
    0,
    1, NOW(), 1, NOW(), 1
FROM sys_social_config c
WHERE c.platform = 'GITEE'
  AND c.connection_code = 'gitee-default'
  AND c.tenant_id = 1
  AND c.del_flag = 0
  AND NOT EXISTS (
    SELECT 1 FROM sys_social_app_config a
    WHERE a.connection_id = c.id
      AND a.app_code = 'gitee-login-app'
      AND a.tenant_id = 1
      AND a.del_flag = 0
  );

-- 2. 绑定 LOGIN 能力到默认应用
INSERT INTO sys_social_capability_binding (
    tenant_id, connection_id, capability, app_config_id,
    status, remark, del_flag,
    create_by, create_time, update_by, update_time, create_dept
)
SELECT
    1,
    c.id,
    'LOGIN',
    a.id,
    1,
    'Gitee OAuth 登录能力绑定',
    0,
    1, NOW(), 1, NOW(), 1
FROM sys_social_config c
INNER JOIN sys_social_app_config a
    ON a.connection_id = c.id
    AND a.app_code = 'gitee-login-app'
    AND a.tenant_id = 1
    AND a.del_flag = 0
WHERE c.platform = 'GITEE'
  AND c.connection_code = 'gitee-default'
  AND c.tenant_id = 1
  AND c.del_flag = 0
  AND NOT EXISTS (
    SELECT 1 FROM sys_social_capability_binding b
    WHERE b.connection_id = c.id
      AND b.capability = 'LOGIN'
      AND b.tenant_id = 1
      AND b.del_flag = 0
  );
