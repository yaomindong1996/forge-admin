-- =====================================================================
-- V1.0.161 : 预置 Gitee OAuth 连接配置（企业协同连接）
-- 用途：为 /system/collaboration/connections 页面提供 Gitee 默认连接骨架，
--       管理员在 UI 中修改 clientId / clientSecret / redirectUri 即可使用。
-- =====================================================================

INSERT INTO sys_social_config (
    platform,
    platform_name,
    platform_logo,
    connection_code,
    connection_name,
    connection_type,
    identity_policy,
    directory_authority,
    client_id,
    client_secret,
    redirect_uri,
    scope,
    status,
    tenant_id,
    remark,
    del_flag
)
SELECT
    'GITEE',
    'Gitee',
    'https://gitee.com/favicon.ico',
    'gitee-default',
    'Gitee OAuth 登录',
    'OAUTH_ONLY',
    'AUTO_CREATE',
    'NONE',
    'YOUR_CLIENT_ID',
    'YOUR_CLIENT_SECRET',
    'http://localhost:8580/social/callback/GITEE',
    'user_info',
    1,
    1,
    '请替换 client_id 和 client_secret 为你在 Gitee 开放平台创建的应用凭据',
    0
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM sys_social_config
    WHERE platform = 'GITEE'
      AND tenant_id = 1
      AND del_flag = 0
);
