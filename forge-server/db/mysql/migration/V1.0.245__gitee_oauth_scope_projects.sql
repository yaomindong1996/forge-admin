-- Gitee 社区登录 Star 校验依赖 /users/{login}/starred，OAuth 需 projects 权限。
-- 仅补齐仍为仅 user_info（或空）的 Gitee 连接/LOGIN 应用；已含 projects 的不改。
-- 用户侧需在 Gitee 开放平台勾选 projects，并重新授权后生效。

UPDATE sys_social_app_config a
INNER JOIN sys_social_config c
  ON c.id = a.connection_id
 AND c.platform = 'GITEE'
 AND c.del_flag = 0
SET a.scope = 'user_info,projects',
    a.remark = CASE
      WHEN a.remark IS NULL OR a.remark = '' THEN
        'Gitee 社区登录需 user_info,projects；点 Star 后重新授权方可校验'
      WHEN a.remark LIKE '%user_info,projects%' THEN a.remark
      ELSE CONCAT(a.remark, '；社区登录 Star 校验需 scope=user_info,projects，改后请用户重新授权')
    END,
    a.update_time = NOW()
WHERE a.del_flag = 0
  AND (
    a.scope IS NULL
    OR TRIM(a.scope) = ''
    OR TRIM(a.scope) = 'user_info'
  );

UPDATE sys_social_config
SET scope = 'user_info,projects',
    remark = CASE
      WHEN remark IS NULL OR remark = '' THEN
        'Gitee 社区登录需 user_info,projects；点 Star 后重新授权方可校验'
      WHEN remark LIKE '%user_info,projects%' THEN remark
      ELSE CONCAT(remark, '；社区登录 Star 校验需 scope=user_info,projects，改后请用户重新授权')
    END,
    update_time = NOW()
WHERE platform = 'GITEE'
  AND del_flag = 0
  AND (
    scope IS NULL
    OR TRIM(scope) = ''
    OR TRIM(scope) = 'user_info'
  );
