-- Align flow task query APIs with their page menus.
-- Page query APIs should follow menu authorization, while action APIs remain independently controlled.

UPDATE sys_resource api
INNER JOIN sys_resource menu
    ON menu.tenant_id = api.tenant_id
   AND menu.del_flag = 0
   AND menu.resource_type = 2
   AND menu.path = '/flow/todo'
SET api.parent_id = menu.id,
    api.client_code = CASE
        WHEN api.client_code IS NULL OR api.client_code = '' THEN menu.client_code
        ELSE api.client_code
    END
WHERE api.del_flag = 0
  AND api.resource_type = 4
  AND api.perms = 'flow:task:todo'
  AND UPPER(api.api_method) = 'GET'
  AND api.api_url = '/api/flow/task/todo';

UPDATE sys_resource api
INNER JOIN sys_resource menu
    ON menu.tenant_id = api.tenant_id
   AND menu.del_flag = 0
   AND menu.resource_type = 2
   AND menu.path = '/flow/done'
SET api.parent_id = menu.id,
    api.client_code = CASE
        WHEN api.client_code IS NULL OR api.client_code = '' THEN menu.client_code
        ELSE api.client_code
    END
WHERE api.del_flag = 0
  AND api.resource_type = 4
  AND api.perms = 'flow:task:done'
  AND UPPER(api.api_method) = 'GET'
  AND api.api_url = '/api/flow/task/done';

UPDATE sys_resource api
INNER JOIN sys_resource menu
    ON menu.tenant_id = api.tenant_id
   AND menu.del_flag = 0
   AND menu.resource_type = 2
   AND menu.path = '/flow/started'
SET api.parent_id = menu.id,
    api.client_code = CASE
        WHEN api.client_code IS NULL OR api.client_code = '' THEN menu.client_code
        ELSE api.client_code
    END
WHERE api.del_flag = 0
  AND api.resource_type = 4
  AND api.perms = 'flow:task:started'
  AND UPPER(api.api_method) = 'GET'
  AND api.api_url = '/api/flow/task/started';

INSERT INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT DISTINCT rr.tenant_id, rr.role_id, api.id, NOW()
FROM sys_role_resource rr
INNER JOIN sys_resource menu
    ON menu.id = rr.resource_id
   AND menu.tenant_id = rr.tenant_id
   AND menu.del_flag = 0
   AND menu.resource_type = 2
   AND menu.path = '/flow/todo'
INNER JOIN sys_resource api
    ON api.tenant_id = rr.tenant_id
   AND api.del_flag = 0
   AND api.resource_type = 4
   AND api.perms = 'flow:task:todo'
   AND UPPER(api.api_method) = 'GET'
   AND api.api_url = '/api/flow/task/todo'
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_role_resource existing
    WHERE existing.tenant_id = rr.tenant_id
      AND existing.role_id = rr.role_id
      AND existing.resource_id = api.id
);

INSERT INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT DISTINCT rr.tenant_id, rr.role_id, api.id, NOW()
FROM sys_role_resource rr
INNER JOIN sys_resource menu
    ON menu.id = rr.resource_id
   AND menu.tenant_id = rr.tenant_id
   AND menu.del_flag = 0
   AND menu.resource_type = 2
   AND menu.path = '/flow/done'
INNER JOIN sys_resource api
    ON api.tenant_id = rr.tenant_id
   AND api.del_flag = 0
   AND api.resource_type = 4
   AND api.perms = 'flow:task:done'
   AND UPPER(api.api_method) = 'GET'
   AND api.api_url = '/api/flow/task/done'
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_role_resource existing
    WHERE existing.tenant_id = rr.tenant_id
      AND existing.role_id = rr.role_id
      AND existing.resource_id = api.id
);

INSERT INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT DISTINCT rr.tenant_id, rr.role_id, api.id, NOW()
FROM sys_role_resource rr
INNER JOIN sys_resource menu
    ON menu.id = rr.resource_id
   AND menu.tenant_id = rr.tenant_id
   AND menu.del_flag = 0
   AND menu.resource_type = 2
   AND menu.path = '/flow/started'
INNER JOIN sys_resource api
    ON api.tenant_id = rr.tenant_id
   AND api.del_flag = 0
   AND api.resource_type = 4
   AND api.perms = 'flow:task:started'
   AND UPPER(api.api_method) = 'GET'
   AND api.api_url = '/api/flow/task/started'
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_role_resource existing
    WHERE existing.tenant_id = rr.tenant_id
      AND existing.role_id = rr.role_id
      AND existing.resource_id = api.id
);
