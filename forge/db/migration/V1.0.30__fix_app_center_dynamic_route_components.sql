-- 修正应用中心动态详情页的 component 路径。
-- V1.0.27/V1.0.29 已经可能被执行，不能修改历史脚本，只能追加迁移。
UPDATE sys_resource
SET component = CASE perms
                  WHEN 'ai:businessSuite:list' THEN 'app-center/suite.[suiteCode]'
                  WHEN 'ai:businessObject:list' THEN 'app-center/object.[objectCode]'
                  ELSE component
                END,
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND resource_type = 2
  AND perms IN (
    'ai:businessSuite:list',
    'ai:businessObject:list'
  );
