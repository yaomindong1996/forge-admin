-- 将「打印中心」挂到「平台管理」下，避免与业务模块并列成一级目录。
-- 回滚：把打印中心 parent_id 改回 0、sort 改回 7 即可；不删资源与授权。

SET @platform_dir_id := (
    SELECT id
    FROM sys_resource
    WHERE tenant_id = 1
      AND resource_type = 1
      AND parent_id = 0
      AND resource_name = '平台管理'
      AND client_code = 'pc'
      AND del_flag = 0
    ORDER BY id
    LIMIT 1
);

SET @print_center_dir_id := (
    SELECT id
    FROM sys_resource
    WHERE tenant_id = 1
      AND resource_type = 1
      AND resource_name = '打印中心'
      AND client_code = 'pc'
      AND del_flag = 0
    ORDER BY id
    LIMIT 1
);

-- 挂到平台管理：排序放在「运维监控」之后。
UPDATE sys_resource
SET parent_id = @platform_dir_id,
    sort = 80,
    path = '/platform/print',
    visible = 1,
    menu_status = 1,
    always_show = 1,
    icon = 'ionicons5:PrintOutline',
    remark = '平台管理下的打印工作台：业务登记、模板设计与场景挂载',
    update_by = 1,
    update_time = NOW()
WHERE @platform_dir_id IS NOT NULL
  AND @print_center_dir_id IS NOT NULL
  AND id = @print_center_dir_id;

-- 已有打印中心授权的角色补上「平台管理」父目录，否则侧栏树可能看不到子菜单。
INSERT INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT DISTINCT existing.tenant_id, existing.role_id, @platform_dir_id, NOW()
FROM sys_role_resource existing
WHERE @platform_dir_id IS NOT NULL
  AND @print_center_dir_id IS NOT NULL
  AND existing.resource_id = @print_center_dir_id
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_resource assigned
      WHERE assigned.tenant_id = existing.tenant_id
        AND assigned.role_id = existing.role_id
        AND assigned.resource_id = @platform_dir_id
  );
