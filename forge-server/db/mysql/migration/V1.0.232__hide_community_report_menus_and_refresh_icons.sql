-- =============================================================================
-- 社区：隐藏表格报表菜单（Pro 能力）+ 刷新一级/数据域图标
-- 说明：只改 visible/menu_status/icon，不删资源行、不改 perms（避免角色授权断裂）。
-- 页面「菜单管理」里手工删除报表管理也可；本脚本保证新库/未清库环境侧栏不再出现。
-- =============================================================================

-- 1) 社区去掉报表侧栏入口
UPDATE sys_resource
SET visible = 0,
    menu_status = 0,
    always_show = 0,
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND client_code = 'pc'
  AND del_flag = 0
  AND (
      path IN ('/report-center', '/report-center/designer', '/report-center/run')
      OR path LIKE '/report-center/%'
      OR resource_name IN ('报表中心', '报表管理', '报表设计器', '报表运行')
  );

-- 2) 一级目录统一线框图标
UPDATE sys_resource
SET icon = CASE path
        WHEN '/workbench' THEN 'ionicons5:HomeOutline'
        WHEN '/app-center' THEN 'ionicons5:GridOutline'
        WHEN '/flow' THEN 'ionicons5:GitNetworkOutline'
        WHEN '/data-report' THEN 'ionicons5:BarChartOutline'
        WHEN '/ai' THEN 'ionicons5:SparklesOutline'
        WHEN '/platform' THEN 'ionicons5:SettingsOutline'
        ELSE icon
    END,
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND client_code = 'pc'
  AND del_flag = 0
  AND resource_type = 1
  AND parent_id = 0
  AND path IN ('/workbench', '/app-center', '/flow', '/data-report', '/ai', '/platform');

-- 3) 「数据与报表」二级语义图标
UPDATE sys_resource
SET icon = CASE path
        WHEN '/data/connection' THEN 'ionicons5:LinkOutline'
        WHEN '/data/dataset' THEN 'ionicons5:GridOutline'
        WHEN '/data/dataset-category' THEN 'ionicons5:FolderOpenOutline'
        WHEN '/data/dimension' THEN 'ionicons5:AnalyticsOutline'
        WHEN '/data/business' THEN 'ionicons5:BriefcaseOutline'
        WHEN '/project/items' THEN 'ionicons5:TvOutline'
        ELSE icon
    END,
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND client_code = 'pc'
  AND del_flag = 0
  AND resource_type = 2
  AND path IN (
      '/data/connection',
      '/data/dataset',
      '/data/dataset-category',
      '/data/dimension',
      '/data/business',
      '/project/items'
  );
