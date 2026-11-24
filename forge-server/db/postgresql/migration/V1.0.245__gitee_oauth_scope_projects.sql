-- Gitee 社区登录 Star 校验依赖 /users/{login}/starred，OAuth 需 projects 权限。
-- 仅补齐仍为仅 user_info（或空）的 Gitee 连接/LOGIN 应用；已含 projects 的不改。

UPDATE sys_social_app_config a
SET scope = 'user_info,projects',
    remark = CASE
      WHEN a.remark IS NULL OR btrim(a.remark) = '' THEN
        'Gitee 社区登录需 user_info,projects；点 Star 后重新授权方可校验'
      WHEN a.remark LIKE '%user_info,projects%' THEN a.remark
      ELSE a.remark || '；社区登录 Star 校验需 scope=user_info,projects，改后请用户重新授权'
    END,
    update_time = CURRENT_TIMESTAMP
FROM sys_social_config c
WHERE c.id = a.connection_id
  AND c.platform = 'GITEE'
  AND c.del_flag = 0
  AND a.del_flag = 0
  AND (
    a.scope IS NULL
    OR btrim(a.scope) = ''
    OR btrim(a.scope) = 'user_info'
  );

UPDATE sys_social_config
SET scope = 'user_info,projects',
    remark = CASE
      WHEN remark IS NULL OR btrim(remark) = '' THEN
        'Gitee 社区登录需 user_info,projects；点 Star 后重新授权方可校验'
      WHEN remark LIKE '%user_info,projects%' THEN remark
      ELSE remark || '；社区登录 Star 校验需 scope=user_info,projects，改后请用户重新授权'
    END,
    update_time = CURRENT_TIMESTAMP
WHERE platform = 'GITEE'
  AND del_flag = 0
  AND (
    scope IS NULL
    OR btrim(scope) = ''
    OR btrim(scope) = 'user_info'
  );
