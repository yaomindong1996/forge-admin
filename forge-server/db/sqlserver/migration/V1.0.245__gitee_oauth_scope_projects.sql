-- Gitee 社区登录 Star 校验依赖 /users/{login}/starred，OAuth 需 projects 权限。
-- 仅补齐仍为仅 user_info（或空）的 Gitee 连接/LOGIN 应用；已含 projects 的不改。

UPDATE a
SET a.scope = N'user_info,projects',
    a.remark = CASE
      WHEN a.remark IS NULL OR LTRIM(RTRIM(a.remark)) = N'' THEN
        N'Gitee 社区登录需 user_info,projects；点 Star 后重新授权方可校验'
      WHEN a.remark LIKE N'%user_info,projects%' THEN a.remark
      ELSE a.remark + N'；社区登录 Star 校验需 scope=user_info,projects，改后请用户重新授权'
    END,
    a.update_time = SYSUTCDATETIME()
FROM sys_social_app_config a
INNER JOIN sys_social_config c
  ON c.id = a.connection_id
 AND c.platform = N'GITEE'
 AND c.del_flag = 0
WHERE a.del_flag = 0
  AND (
    a.scope IS NULL
    OR LTRIM(RTRIM(a.scope)) = N''
    OR LTRIM(RTRIM(a.scope)) = N'user_info'
  );

UPDATE sys_social_config
SET scope = N'user_info,projects',
    remark = CASE
      WHEN remark IS NULL OR LTRIM(RTRIM(remark)) = N'' THEN
        N'Gitee 社区登录需 user_info,projects；点 Star 后重新授权方可校验'
      WHEN remark LIKE N'%user_info,projects%' THEN remark
      ELSE remark + N'；社区登录 Star 校验需 scope=user_info,projects，改后请用户重新授权'
    END,
    update_time = SYSUTCDATETIME()
WHERE platform = N'GITEE'
  AND del_flag = 0
  AND (
    scope IS NULL
    OR LTRIM(RTRIM(scope)) = N''
    OR LTRIM(RTRIM(scope)) = N'user_info'
  );
