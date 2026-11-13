-- 企业协同字典、菜单权限、出站场景与 Job 配置。
-- 全部内置数据 tenant_id=1，具备 NOT EXISTS 防重复保护；不写入任何 Secret。

-- ============================================================
-- 1. 字典类型
-- ============================================================

INSERT INTO sys_dict_type (
  tenant_id, dict_name, dict_type, dict_status, remark,
  create_by, create_time, update_by, update_time, create_dept
)
SELECT seed.tenant_id, seed.dict_name, seed.dict_type, 1, seed.remark,
       1, NOW(), 1, NOW(), 1
FROM (
  SELECT 1 tenant_id, '企业协同平台' dict_name, 'sys_collab_platform' dict_type, '企业协同连接平台类型' remark
  UNION ALL SELECT 1, '企业协同能力', 'sys_collab_capability', '连接下可绑定的业务能力'
  UNION ALL SELECT 1, '协同身份匹配策略', 'sys_collab_identity_policy', '目录同步身份匹配策略'
  UNION ALL SELECT 1, '协同目录权威来源', 'sys_collab_directory_authority', '组织目录权威来源'
  UNION ALL SELECT 1, '协同同步状态', 'sys_collab_sync_status', '目录同步批次状态'
  UNION ALL SELECT 1, '协同投递状态', 'sys_collab_delivery_status', '消息/待办外部投递状态'
  UNION ALL SELECT 1, '协同回调状态', 'sys_collab_callback_status', '回调事件处理状态'
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM sys_dict_type data
  WHERE data.tenant_id = seed.tenant_id
    AND data.dict_type = seed.dict_type
);

-- ============================================================
-- 2. 字典数据
-- ============================================================

INSERT INTO sys_dict_data (
  tenant_id, dict_sort, dict_label, dict_value, dict_type,
  css_class, list_class, is_default, dict_status, remark,
  create_by, create_time, update_by, update_time, create_dept
)
SELECT seed.tenant_id, seed.dict_sort, seed.dict_label, seed.dict_value, seed.dict_type,
       NULL, seed.list_class, seed.is_default, 1, seed.remark,
       1, NOW(), 1, NOW(), 1
FROM (
  SELECT 1 tenant_id, 1 dict_sort, '企业微信' dict_label, 'WECOM' dict_value,
         'sys_collab_platform' dict_type, 'info' list_class, 'Y' is_default, '企业微信自建应用' remark
  UNION ALL SELECT 1, 1, '扫码登录', 'LOGIN', 'sys_collab_capability', 'info', 'N', '企业成员OAuth登录'
  UNION ALL SELECT 1, 2, '通讯录同步', 'DIRECTORY', 'sys_collab_capability', 'success', 'N', '组织/成员/标签同步'
  UNION ALL SELECT 1, 3, '消息推送', 'MESSAGE', 'sys_collab_capability', 'warning', 'N', '应用消息与模板卡片'
  UNION ALL SELECT 1, 4, '待办联动', 'TODO', 'sys_collab_capability', 'error', 'N', 'Flowable待办投影'
  UNION ALL SELECT 1, 1, '仅绑定已有用户', 'BIND_ONLY', 'sys_collab_identity_policy', 'info', 'Y', '不自动创建Forge用户'
  UNION ALL SELECT 1, 2, '自动创建用户', 'AUTO_CREATE', 'sys_collab_identity_policy', 'warning', 'N', '未匹配时自动创建Forge用户'
  UNION ALL SELECT 1, 3, '人工处理', 'MANUAL', 'sys_collab_identity_policy', 'default', 'N', '未匹配进入问题单'
  UNION ALL SELECT 1, 1, '外部权威', 'EXTERNAL', 'sys_collab_directory_authority', 'success', 'Y', '外部平台为组织目录权威来源'
  UNION ALL SELECT 1, 2, '本地权威', 'LOCAL', 'sys_collab_directory_authority', 'info', 'N', 'Forge为权威来源，外部只读'
  UNION ALL SELECT 1, 3, '不同步', 'NONE', 'sys_collab_directory_authority', 'default', 'N', '不进行目录同步'
  UNION ALL SELECT 1, 1, '运行中', 'RUNNING', 'sys_collab_sync_status', 'info', 'N', '同步批次执行中'
  UNION ALL SELECT 1, 2, '成功', 'SUCCESS', 'sys_collab_sync_status', 'success', 'N', '同步批次成功'
  UNION ALL SELECT 1, 3, '部分成功', 'PARTIAL', 'sys_collab_sync_status', 'warning', 'N', '存在问题单的批次'
  UNION ALL SELECT 1, 4, '失败', 'FAILED', 'sys_collab_sync_status', 'error', 'N', '同步批次失败'
  UNION ALL SELECT 1, 1, '待投递', 'PENDING', 'sys_collab_delivery_status', 'info', 'N', '等待外部投递'
  UNION ALL SELECT 1, 2, '已投递', 'SENT', 'sys_collab_delivery_status', 'success', 'N', '外部投递成功'
  UNION ALL SELECT 1, 3, '投递失败', 'FAILED', 'sys_collab_delivery_status', 'error', 'N', '外部投递失败待补偿'
  UNION ALL SELECT 1, 4, '已跳过', 'SKIPPED', 'sys_collab_delivery_status', 'default', 'N', '无映射等原因跳过'
  UNION ALL SELECT 1, 5, '已关闭', 'CLOSED', 'sys_collab_delivery_status', 'default', 'N', '投影已关闭'
  UNION ALL SELECT 1, 1, '待处理', 'PENDING', 'sys_collab_callback_status', 'info', 'N', '回调事件待处理'
  UNION ALL SELECT 1, 2, '处理中', 'PROCESSING', 'sys_collab_callback_status', 'warning', 'N', '回调事件处理中'
  UNION ALL SELECT 1, 3, '已处理', 'PROCESSED', 'sys_collab_callback_status', 'success', 'N', '回调事件处理成功'
  UNION ALL SELECT 1, 4, '处理失败', 'FAILED', 'sys_collab_callback_status', 'error', 'N', '回调事件处理失败待重试'
  UNION ALL SELECT 1, 5, '已丢弃', 'DISCARDED', 'sys_collab_callback_status', 'default', 'N', '超过重试上限或人工丢弃'
  -- 消息渠道扩展：企业协同渠道
  UNION ALL SELECT 1, 5, '企业协同', 'COLLABORATION', 'sys_message_channel', 'info', 'N', '企业微信等企业协同渠道'
  -- 出站安全场景扩展：企业协同供应商API，永不允许私网
  UNION ALL SELECT 1, 3, '企业协同供应商', 'COLLABORATION_PROVIDER', 'sys_outbound_scene', 'info', 'N', '企业协同供应商API出站场景，永不允许私网'
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM sys_dict_data data
  WHERE data.tenant_id = seed.tenant_id
    AND data.dict_type = seed.dict_type
    AND data.dict_value = seed.dict_value
);

-- ============================================================
-- 3. 菜单资源：企业协同目录与页面
-- ============================================================

SET @system_dir_id = (
  SELECT id FROM sys_resource
  WHERE tenant_id = 1 AND resource_type = 1 AND path = '/system' AND del_flag = 0
  ORDER BY id LIMIT 1
);

INSERT INTO sys_resource (
  tenant_id, resource_name, parent_id, resource_type, sort,
  path, component, is_external, open_target, is_public, menu_status, visible,
  perms, icon, keep_alive, always_show, remark,
  create_by, create_time, update_by, update_time, create_dept, client_code
)
SELECT 1, '企业协同', COALESCE(@system_dir_id, 0), 1, 95,
       '/system/collaboration', NULL, 0, '_self', 0, 1, 1,
       NULL, 'ionicons5:GitNetworkOutline', 0, 1, '企业协同集成管理目录',
       1, NOW(), 1, NOW(), 1, 'pc'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_resource r
  WHERE r.tenant_id = 1 AND r.resource_type = 1 AND r.path = '/system/collaboration' AND r.del_flag = 0
);

SET @collab_dir_id = (
  SELECT id FROM sys_resource
  WHERE tenant_id = 1 AND resource_type = 1 AND path = '/system/collaboration' AND del_flag = 0
  ORDER BY id LIMIT 1
);

INSERT INTO sys_resource (
  tenant_id, resource_name, parent_id, resource_type, sort,
  path, component, is_external, open_target, is_public, menu_status, visible,
  perms, icon, keep_alive, always_show, remark,
  create_by, create_time, update_by, update_time, create_dept, client_code
)
SELECT 1, seed.resource_name, @collab_dir_id, 2, seed.sort,
       seed.path, seed.component, 0, '_self', 0, 1, 1,
       seed.perms, seed.icon, 1, 0, seed.remark,
       1, NOW(), 1, NOW(), 1, 'pc'
FROM (
  SELECT '连接管理' resource_name, 1 sort, '/system/collaboration/connections' path,
         'system/collaboration/connections' component, 'system:collaboration:connection:list' perms,
         'ionicons5:LinkOutline' icon, '企业协同连接与应用管理' remark
  UNION ALL SELECT '同步批次', 2, '/system/collaboration/sync', 'system/collaboration/sync',
         'system:collaboration:sync:view', 'ionicons5:SyncOutline', '目录同步批次与阶段统计'
  UNION ALL SELECT '问题单', 3, '/system/collaboration/issues', 'system/collaboration/issues',
         'system:collaboration:issue:view', 'ionicons5:AlertCircleOutline', '同步冲突与人工处理队列'
  UNION ALL SELECT '映射查询', 4, '/system/collaboration/mappings', 'system/collaboration/mappings',
         'system:collaboration:mapping:view', 'ionicons5:GitCompareOutline', '部门/用户/岗位/标签映射检索'
  UNION ALL SELECT '投递记录', 5, '/system/collaboration/deliveries', 'system/collaboration/deliveries',
         'system:collaboration:delivery:view', 'ionicons5:PaperPlaneOutline', '消息与待办投递状态'
  UNION ALL SELECT '回调事件', 6, '/system/collaboration/callback-events', 'system/collaboration/callback-events',
         'system:collaboration:callback:view', 'ionicons5:DownloadOutline', '回调收件箱元数据与处理状态'
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM sys_resource r
  WHERE r.tenant_id = 1 AND r.resource_type = 2 AND r.perms = seed.perms AND r.del_flag = 0
);

-- ============================================================
-- 4. 按钮资源
-- ============================================================

INSERT INTO sys_resource (
  tenant_id, resource_name, parent_id, resource_type, sort,
  is_external, open_target, is_public, menu_status, visible,
  perms, keep_alive, always_show, remark,
  create_by, create_time, update_by, update_time, create_dept, client_code
)
SELECT 1, seed.resource_name, menu.id, 3, seed.sort, 0, '_self', 0, 1, 1,
       seed.perms, 0, 0, seed.remark, 1, NOW(), 1, NOW(), 1, 'pc'
FROM (
  SELECT '新建连接' resource_name, 1 sort, 'system:collaboration:connection:create' perms,
         'system:collaboration:connection:list' menu_perms, '创建企业协同连接与应用' remark
  UNION ALL SELECT '修改连接', 2, 'system:collaboration:connection:update', 'system:collaboration:connection:list', '修改连接、应用与Secret轮换'
  UNION ALL SELECT '删除连接', 3, 'system:collaboration:connection:delete', 'system:collaboration:connection:list', '逻辑删除未被引用的连接'
  UNION ALL SELECT '连通测试', 4, 'system:collaboration:connection:test', 'system:collaboration:connection:list', '按能力执行连通验证'
  UNION ALL SELECT '旧凭据迁移', 5, 'system:collaboration:credential:migrate', 'system:collaboration:connection:list', '旧明文凭据与身份兼容迁移'
  UNION ALL SELECT '触发同步', 1, 'system:collaboration:sync:execute', 'system:collaboration:sync:view', '触发全量/部门/成员/标签同步'
  UNION ALL SELECT '处理问题单', 1, 'system:collaboration:sync:resolve', 'system:collaboration:issue:view', '绑定、忽略、重试问题单'
  UNION ALL SELECT '重试投递', 1, 'system:collaboration:delivery:retry', 'system:collaboration:delivery:view', '人工重试失败投递'
  UNION ALL SELECT '重试回调', 1, 'system:collaboration:callback:retry', 'system:collaboration:callback:view', '人工重试失败回调事件'
) seed
JOIN sys_resource menu
  ON menu.tenant_id = 1 AND menu.resource_type = 2 AND menu.perms = seed.menu_perms AND menu.del_flag = 0
WHERE NOT EXISTS (
  SELECT 1 FROM sys_resource r
  WHERE r.tenant_id = 1 AND r.resource_type = 3 AND r.perms = seed.perms AND r.del_flag = 0
);

-- ============================================================
-- 5. API 资源
-- ============================================================

INSERT INTO sys_resource (
  tenant_id, resource_name, parent_id, resource_type, sort,
  is_external, open_target, is_public, menu_status, visible,
  perms, api_method, api_url, keep_alive, always_show, remark,
  create_by, create_time, update_by, update_time, create_dept, client_code
)
SELECT 1, seed.resource_name, COALESCE(@collab_dir_id, 0), 4, seed.sort, 0, '_self', 0, 1, 1,
       seed.perms, seed.api_method, seed.api_url, 0, 0, seed.remark,
       1, NOW(), 1, NOW(), 1, 'pc'
FROM (
  SELECT '连接分页接口' resource_name, 1 sort, 'system:collaboration:api:connection:page' perms,
         'GET' api_method, '/system/collaboration/connections/page' api_url, '分页查询协同连接' remark
  UNION ALL SELECT '连接详情接口', 2, 'system:collaboration:api:connection:detail', 'GET', '/system/collaboration/connections/*', '查询连接、能力和凭据状态摘要'
  UNION ALL SELECT '连接新增接口', 3, 'system:collaboration:api:connection:add', 'POST', '/system/collaboration/connections', '创建协同连接'
  UNION ALL SELECT '连接修改接口', 4, 'system:collaboration:api:connection:edit', 'PUT', '/system/collaboration/connections', '修改协同连接'
  UNION ALL SELECT '连接删除接口', 5, 'system:collaboration:api:connection:remove', 'DELETE', '/system/collaboration/connections/*', '逻辑删除协同连接'
  UNION ALL SELECT '应用查询接口', 6, 'system:collaboration:api:app:list', 'GET', '/system/collaboration/connections/*/apps', '查询连接下物理应用'
  UNION ALL SELECT '应用新增接口', 7, 'system:collaboration:api:app:add', 'POST', '/system/collaboration/connections/*/apps', '新增物理应用'
  UNION ALL SELECT '应用修改接口', 8, 'system:collaboration:api:app:edit', 'PUT', '/system/collaboration/connections/*/apps', '修改物理应用与Secret轮换'
  UNION ALL SELECT '连通测试接口', 9, 'system:collaboration:api:connection:test', 'POST', '/system/collaboration/connections/*/test', '按能力执行连通验证'
  UNION ALL SELECT '触发同步接口', 10, 'system:collaboration:api:sync:execute', 'POST', '/system/collaboration/connections/*/sync', '触发目录同步'
  UNION ALL SELECT '同步批次分页接口', 11, 'system:collaboration:api:synclog:page', 'GET', '/system/collaboration/sync-logs/page', '查询同步批次'
  UNION ALL SELECT '问题单分页接口', 12, 'system:collaboration:api:issue:page', 'GET', '/system/collaboration/sync-issues/page', '查询同步问题单'
  UNION ALL SELECT '问题单处理接口', 13, 'system:collaboration:api:issue:resolve', 'POST', '/system/collaboration/sync-issues/*/resolve', '处理同步问题单'
  UNION ALL SELECT '映射查询接口', 14, 'system:collaboration:api:mapping:list', 'GET', '/system/collaboration/mappings/*', '查询目录映射'
  UNION ALL SELECT '投递分页接口', 15, 'system:collaboration:api:delivery:page', 'GET', '/system/collaboration/deliveries/page', '查询投递状态'
  UNION ALL SELECT '投递重试接口', 16, 'system:collaboration:api:delivery:retry', 'POST', '/system/collaboration/deliveries/*/retry', '人工重试失败投递'
  UNION ALL SELECT '回调事件分页接口', 17, 'system:collaboration:api:callback:page', 'GET', '/system/collaboration/callback-events/page', '查询回调事件元数据'
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM sys_resource r
  WHERE r.tenant_id = 1 AND r.resource_type = 4 AND r.perms = seed.perms AND r.del_flag = 0
);

-- ============================================================
-- 6. 角色授权：仅显式授予默认租户超级管理员，不向普通角色自动扩散
-- ============================================================

INSERT INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT 1, admin_role.id, resource.id, NOW()
FROM (SELECT id FROM sys_role WHERE tenant_id = 1 AND role_key = 'admin' ORDER BY id LIMIT 1) admin_role
JOIN sys_resource resource ON resource.tenant_id = 1 AND resource.del_flag = 0
WHERE resource.client_code = 'pc'
  AND (
    resource.path = '/system/collaboration'
    OR resource.perms LIKE 'system:collaboration:%'
  )
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_resource existing
    WHERE existing.tenant_id = 1
      AND existing.role_id = admin_role.id
      AND existing.resource_id = resource.id
  );

-- ============================================================
-- 7. Job 配置：默认停用，配置连接并验证连通后再启用
-- ============================================================

INSERT INTO sys_job_config (
  job_name, job_group, description, executor_handler,
  schedule_type, cron_expression, timezone, status,
  execute_mode, invoke_mode, concurrent_policy, misfire_policy,
  idempotent_flag, retry_count
)
SELECT seed.job_name, 'COLLABORATION', seed.description, seed.executor_handler,
       'CRON', seed.cron_expression, 'Asia/Shanghai', 0,
       'HANDLER', 'SINGLE', 'SKIP_IF_RUNNING', 'DO_NOTHING',
       1, 0
FROM (
  SELECT '企业协同目录全量校准' job_name, 'collaborationDirectorySync' executor_handler,
         '0 0 2 * * ?' cron_expression, '每日全量校准目录，修复丢失的增量事件；配置连接后手动启用' description
  UNION ALL SELECT '企业协同回调事件重试', 'collaborationCallbackRetry',
         '0 */5 * * * ?', '重试回调收件箱中失败/待处理事件；配置连接后手动启用'
  UNION ALL SELECT '企业协同消息投递补偿', 'collaborationDeliveryRetry',
         '0 2/5 * * * ?', '补偿到期的失败消息投递；配置连接后手动启用'
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM sys_job_config job
  WHERE job.job_name = seed.job_name
    AND job.job_group = 'COLLABORATION'
    AND job.del_flag = 0
);
