-- 低代码单据通用动作权限码注册为系统按钮资源。
-- 对象级权限码（ai:business:{objectCode}:*）由对象发布时自动注册；
-- 通用权限码（单据保存/提交、流程发起等）全局共享，此处一次性注册到低代码根目录，
-- 并把授权继承给已拥有应用运行入口（ai:businessApplication:runtime）的角色，
-- 保证存量运行时用户具备单据基础动作能力；细粒度控制由对象级权限码承担。

SET @lowcode_root_id = (
  SELECT id
  FROM sys_resource
  WHERE tenant_id = 1
    AND resource_type = 1
    AND path = '/ai'
    AND del_flag = 0
  ORDER BY id
  LIMIT 1
);

INSERT INTO sys_resource (
  tenant_id, resource_name, parent_id, resource_type, sort,
  is_external, open_target, is_public, menu_status, visible, perms,
  keep_alive, always_show, remark, create_by, create_time,
  update_by, update_time, create_dept, client_code
)
SELECT 1, '查看单据状态', @lowcode_root_id, 3, 1,
       0, '_self', 0, 1, 1, 'ai:businessDocument:view',
       0, 0, '低代码单据通用动作：查看单据状态',
       1, NOW(), 1, NOW(), 1, 'pc'
WHERE @lowcode_root_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_resource resource_row
    WHERE resource_row.tenant_id = 1 AND resource_row.perms = 'ai:businessDocument:view' AND resource_row.del_flag = 0
  );

INSERT INTO sys_resource (
  tenant_id, resource_name, parent_id, resource_type, sort,
  is_external, open_target, is_public, menu_status, visible, perms,
  keep_alive, always_show, remark, create_by, create_time,
  update_by, update_time, create_dept, client_code
)
SELECT 1, '保存单据', @lowcode_root_id, 3, 2,
       0, '_self', 0, 1, 1, 'ai:businessDocument:save',
       0, 0, '低代码单据通用动作：保存单据',
       1, NOW(), 1, NOW(), 1, 'pc'
WHERE @lowcode_root_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_resource resource_row
    WHERE resource_row.tenant_id = 1 AND resource_row.perms = 'ai:businessDocument:save' AND resource_row.del_flag = 0
  );

INSERT INTO sys_resource (
  tenant_id, resource_name, parent_id, resource_type, sort,
  is_external, open_target, is_public, menu_status, visible, perms,
  keep_alive, always_show, remark, create_by, create_time,
  update_by, update_time, create_dept, client_code
)
SELECT 1, '提交单据', @lowcode_root_id, 3, 3,
       0, '_self', 0, 1, 1, 'ai:businessDocument:submit',
       0, 0, '低代码单据通用动作：提交单据',
       1, NOW(), 1, NOW(), 1, 'pc'
WHERE @lowcode_root_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_resource resource_row
    WHERE resource_row.tenant_id = 1 AND resource_row.perms = 'ai:businessDocument:submit' AND resource_row.del_flag = 0
  );

INSERT INTO sys_resource (
  tenant_id, resource_name, parent_id, resource_type, sort,
  is_external, open_target, is_public, menu_status, visible, perms,
  keep_alive, always_show, remark, create_by, create_time,
  update_by, update_time, create_dept, client_code
)
SELECT 1, '撤回单据', @lowcode_root_id, 3, 4,
       0, '_self', 0, 1, 1, 'ai:businessDocument:withdraw',
       0, 0, '低代码单据通用动作：撤回单据',
       1, NOW(), 1, NOW(), 1, 'pc'
WHERE @lowcode_root_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_resource resource_row
    WHERE resource_row.tenant_id = 1 AND resource_row.perms = 'ai:businessDocument:withdraw' AND resource_row.del_flag = 0
  );

INSERT INTO sys_resource (
  tenant_id, resource_name, parent_id, resource_type, sort,
  is_external, open_target, is_public, menu_status, visible, perms,
  keep_alive, always_show, remark, create_by, create_time,
  update_by, update_time, create_dept, client_code
)
SELECT 1, '发起主流程', @lowcode_root_id, 3, 5,
       0, '_self', 0, 1, 1, 'ai:businessFlow:start',
       0, 0, '低代码单据通用动作：发起主流程',
       1, NOW(), 1, NOW(), 1, 'pc'
WHERE @lowcode_root_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_resource resource_row
    WHERE resource_row.tenant_id = 1 AND resource_row.perms = 'ai:businessFlow:start' AND resource_row.del_flag = 0
  );

INSERT INTO sys_resource (
  tenant_id, resource_name, parent_id, resource_type, sort,
  is_external, open_target, is_public, menu_status, visible, perms,
  keep_alive, always_show, remark, create_by, create_time,
  update_by, update_time, create_dept, client_code
)
SELECT 1, '查看流程', @lowcode_root_id, 3, 6,
       0, '_self', 0, 1, 1, 'ai:businessFlow:view',
       0, 0, '低代码单据通用动作：查看流程',
       1, NOW(), 1, NOW(), 1, 'pc'
WHERE @lowcode_root_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_resource resource_row
    WHERE resource_row.tenant_id = 1 AND resource_row.perms = 'ai:businessFlow:view' AND resource_row.del_flag = 0
  );

INSERT INTO sys_resource (
  tenant_id, resource_name, parent_id, resource_type, sort,
  is_external, open_target, is_public, menu_status, visible, perms,
  keep_alive, always_show, remark, create_by, create_time,
  update_by, update_time, create_dept, client_code
)
SELECT 1, '执行触发器', @lowcode_root_id, 3, 7,
       0, '_self', 0, 1, 1, 'ai:businessTrigger:execute',
       0, 0, '低代码单据通用动作：执行触发器',
       1, NOW(), 1, NOW(), 1, 'pc'
WHERE @lowcode_root_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_resource resource_row
    WHERE resource_row.tenant_id = 1 AND resource_row.perms = 'ai:businessTrigger:execute' AND resource_row.del_flag = 0
  );

INSERT INTO sys_resource (
  tenant_id, resource_name, parent_id, resource_type, sort,
  is_external, open_target, is_public, menu_status, visible, perms,
  keep_alive, always_show, remark, create_by, create_time,
  update_by, update_time, create_dept, client_code
)
SELECT 1, '查看报表', @lowcode_root_id, 3, 8,
       0, '_self', 0, 1, 1, 'ai:businessStats:view',
       0, 0, '低代码单据通用动作：查看报表',
       1, NOW(), 1, NOW(), 1, 'pc'
WHERE @lowcode_root_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_resource resource_row
    WHERE resource_row.tenant_id = 1 AND resource_row.perms = 'ai:businessStats:view' AND resource_row.del_flag = 0
  );

-- 已拥有应用运行入口权限的角色继承通用单据动作权限，保证存量用户不被新权限码阻断。
INSERT INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT DISTINCT 1, runtime_role.role_id, generic_resource.id, NOW()
FROM sys_resource runtime_resource
INNER JOIN sys_role_resource runtime_role
  ON runtime_role.tenant_id = 1
 AND runtime_role.resource_id = runtime_resource.id
INNER JOIN sys_resource generic_resource
  ON generic_resource.tenant_id = 1
 AND generic_resource.del_flag = 0
 AND generic_resource.perms IN (
   'ai:businessDocument:view',
   'ai:businessDocument:save',
   'ai:businessDocument:submit',
   'ai:businessDocument:withdraw',
   'ai:businessFlow:start',
   'ai:businessFlow:view',
   'ai:businessTrigger:execute',
   'ai:businessStats:view'
 )
WHERE runtime_resource.tenant_id = 1
  AND runtime_resource.perms = 'ai:businessApplication:runtime'
  AND runtime_resource.del_flag = 0
  AND NOT EXISTS (
    SELECT 1
    FROM sys_role_resource existing_row
    WHERE existing_row.tenant_id = 1
      AND existing_row.role_id = runtime_role.role_id
      AND existing_row.resource_id = generic_resource.id
  );
