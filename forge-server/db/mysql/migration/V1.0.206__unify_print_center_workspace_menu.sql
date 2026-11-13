-- 打印中心三菜单合并为单一工作台入口，减少来回跳转。
-- 回滚：将本脚本隐藏的「业务数据源」「场景绑定」visible 改回 1，并把「打印工作台」改回「打印模板」即可；不删资源与授权。

SET @print_center_dir_id := (
    SELECT id
    FROM sys_resource
    WHERE tenant_id = 1
      AND resource_type = 1
      AND parent_id = 0
      AND resource_name = '打印中心'
      AND client_code = 'pc'
      AND del_flag = 0
    ORDER BY id
    LIMIT 1
);

-- 主入口：原「打印模板」改为「打印工作台」，路径统一 /print。
UPDATE sys_resource
SET resource_name = '打印工作台',
    parent_id = COALESCE(@print_center_dir_id, parent_id),
    sort = 1,
    path = '/print',
    component = 'print/index',
    menu_status = 1,
    visible = 1,
    perms = 'print:template:view',
    icon = 'ionicons5:PrintOutline',
    keep_alive = 1,
    always_show = 0,
    redirect = NULL,
    remark = '左侧选业务，右侧管理模板与场景挂载',
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND resource_type = 2
  AND client_code = 'pc'
  AND del_flag = 0
  AND path IN ('/print', '/print/templates');

-- 旧子菜单保留授权与深链，但侧栏不再展示。
UPDATE sys_resource
SET visible = 0,
    menu_status = 1,
    remark = '已并入打印工作台，保留路由兼容',
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND resource_type = 2
  AND client_code = 'pc'
  AND del_flag = 0
  AND path IN ('/print/sources', '/print/bindings');

-- 目录始终可见，方便找到工作台。
UPDATE sys_resource
SET visible = 1,
    always_show = 1,
    remark = '平台级打印：业务登记、模板设计与场景挂载',
    update_by = 1,
    update_time = NOW()
WHERE @print_center_dir_id IS NOT NULL
  AND id = @print_center_dir_id;
