-- 下线 Redis 原始 key/value 诊断接口及其按钮资源。
-- 这些接口允许枚举、读取或任意删除共享 Redis 数据；受管缓存策略接口不在清理范围内。
-- sys_role_resource 是关系重建表，按既有资源删除约定物理清理；sys_resource 使用主键墓碑逻辑删除。
-- 如需回滚，必须先经安全复核恢复后端 Controller，再用新迁移恢复资源并重新分配角色权限。

DELETE FROM sys_role_resource
WHERE resource_id IN (
    SELECT id
    FROM (
        SELECT id
        FROM sys_resource
        WHERE tenant_id = 1
          AND del_flag = 0
          AND (
              (resource_type = 3 AND perms IN (
                  'system:cache:query',
                  'system:cache:page',
                  'system:cache:getInfo',
                  'system:cache:remove',
                  'system:cache:removeBatch',
                  'system:cache:clear',
                  'system:cache:metrics'
              ))
              OR
              (resource_type = 4 AND api_url IN (
                  '/system/cache/page',
                  '/system/cache/getInfo',
                  '/system/cache/remove',
                  '/system/cache/removeBatch',
                  '/system/cache/clear',
                  '/system/cache/metrics'
              ))
          )
    ) retired_resource
);

UPDATE sys_resource
SET del_flag = id,
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND del_flag = 0
  AND (
      (resource_type = 3 AND perms IN (
          'system:cache:query',
          'system:cache:page',
          'system:cache:getInfo',
          'system:cache:remove',
          'system:cache:removeBatch',
          'system:cache:clear',
          'system:cache:metrics'
      ))
      OR
      (resource_type = 4 AND api_url IN (
          '/system/cache/page',
          '/system/cache/getInfo',
          '/system/cache/remove',
          '/system/cache/removeBatch',
          '/system/cache/clear',
          '/system/cache/metrics'
      ))
  );
