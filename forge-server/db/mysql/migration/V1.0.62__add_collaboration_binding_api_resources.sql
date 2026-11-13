-- 企业协同应用删除与能力绑定 API 资源（Task 18 补充）。
-- 内置数据 tenant_id=1，具备 NOT EXISTS 防重复保护；不写入任何 Secret。

-- 连接类型字典（前端下拉禁止硬编码）
INSERT INTO sys_dict_type (
  tenant_id, dict_name, dict_type, dict_status, remark,
  create_by, create_time, update_by, update_time, create_dept
)
SELECT 1, '协同连接类型', 'sys_collab_connection_type', 1, '企业协同连接接入类型',
       1, NOW(), 1, NOW(), 1
WHERE NOT EXISTS (
  SELECT 1 FROM sys_dict_type data
  WHERE data.tenant_id = 1 AND data.dict_type = 'sys_collab_connection_type'
);

-- 运维页面辅助字典：问题单状态/同步类型/触发来源
INSERT INTO sys_dict_type (
  tenant_id, dict_name, dict_type, dict_status, remark,
  create_by, create_time, update_by, update_time, create_dept
)
SELECT seed.tenant_id, seed.dict_name, seed.dict_type, 1, seed.remark,
       1, NOW(), 1, NOW(), 1
FROM (
  SELECT 1 tenant_id, '协同问题单状态' dict_name, 'sys_collab_issue_status' dict_type, '同步问题单处理状态' remark
  UNION ALL SELECT 1, '协同同步类型', 'sys_collab_sync_type', '目录同步批次类型'
  UNION ALL SELECT 1, '协同触发来源', 'sys_collab_trigger_source', '目录同步触发来源'
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM sys_dict_type data
  WHERE data.tenant_id = seed.tenant_id
    AND data.dict_type = seed.dict_type
);

INSERT INTO sys_dict_data (
  tenant_id, dict_sort, dict_label, dict_value, dict_type,
  css_class, list_class, is_default, dict_status, remark,
  create_by, create_time, update_by, update_time, create_dept
)
SELECT seed.tenant_id, seed.dict_sort, seed.dict_label, seed.dict_value, seed.dict_type,
       NULL, seed.list_class, seed.is_default, 1, seed.remark,
       1, NOW(), 1, NOW(), 1
FROM (
  SELECT 1 tenant_id, 1 dict_sort, '企业自建' dict_label, 'CORP_INTERNAL' dict_value,
         'sys_collab_connection_type' dict_type, 'success' list_class, 'Y' is_default, '企业内部自建应用接入' remark
  UNION ALL SELECT 1, 2, '第三方应用', 'THIRD_PARTY', 'sys_collab_connection_type', 'info', 'N', '第三方服务商应用接入'
  UNION ALL SELECT 1, 3, '仅OAuth登录', 'OAUTH_ONLY', 'sys_collab_connection_type', 'default', 'N', '仅用于OAuth扫码登录'
  UNION ALL SELECT 1, 1, '待处理', 'PENDING', 'sys_collab_issue_status', 'warning', 'N', '待人工处理'
  UNION ALL SELECT 1, 2, '已解决', 'RESOLVED', 'sys_collab_issue_status', 'success', 'N', '已绑定或重试成功'
  UNION ALL SELECT 1, 3, '已忽略', 'IGNORED', 'sys_collab_issue_status', 'default', 'N', '人工忽略不再处理'
  UNION ALL SELECT 1, 1, '全量同步', 'FULL', 'sys_collab_sync_type', 'info', 'N', '全量目录同步'
  UNION ALL SELECT 1, 2, '部门同步', 'DEPT', 'sys_collab_sync_type', 'info', 'N', '仅同步部门'
  UNION ALL SELECT 1, 3, '成员同步', 'USER', 'sys_collab_sync_type', 'info', 'N', '仅同步成员'
  UNION ALL SELECT 1, 4, '标签同步', 'TAG', 'sys_collab_sync_type', 'info', 'N', '仅同步标签'
  UNION ALL SELECT 1, 5, '增量同步', 'INCREMENT', 'sys_collab_sync_type', 'default', 'N', '回调事件增量同步'
  UNION ALL SELECT 1, 1, '手工触发', 'MANUAL', 'sys_collab_trigger_source', 'info', 'N', '管理页手工触发'
  UNION ALL SELECT 1, 2, '定时任务', 'JOB', 'sys_collab_trigger_source', 'success', 'N', '定时任务触发'
  UNION ALL SELECT 1, 3, '回调事件', 'CALLBACK', 'sys_collab_trigger_source', 'warning', 'N', '外部回调触发'
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM sys_dict_data data
  WHERE data.tenant_id = seed.tenant_id
    AND data.dict_type = seed.dict_type
    AND data.dict_value = seed.dict_value
);

SET @collab_dir_id = (
  SELECT id FROM sys_resource
  WHERE tenant_id = 1 AND resource_type = 1 AND path = '/system/collaboration' AND del_flag = 0
  ORDER BY id LIMIT 1
);

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
  SELECT '应用删除接口' resource_name, 19 sort, 'system:collaboration:api:app:remove' perms,
         'DELETE' api_method, '/system/collaboration/connections/*/apps/*' api_url, '删除物理应用' remark
  UNION ALL SELECT '能力绑定接口', 20, 'system:collaboration:api:binding:add', 'POST',
         '/system/collaboration/connections/*/bindings', '绑定能力到物理应用'
  UNION ALL SELECT '能力解绑接口', 21, 'system:collaboration:api:binding:remove', 'DELETE',
         '/system/collaboration/connections/*/bindings/*', '解绑连接能力'
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM sys_resource r
  WHERE r.tenant_id = 1 AND r.resource_type = 4 AND r.perms = seed.perms AND r.del_flag = 0
);

-- 角色授权：仅显式授予默认租户超级管理员
INSERT INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT 1, admin_role.id, resource.id, NOW()
FROM (SELECT id FROM sys_role WHERE tenant_id = 1 AND role_key = 'admin' ORDER BY id LIMIT 1) admin_role
JOIN sys_resource resource ON resource.tenant_id = 1 AND resource.del_flag = 0
WHERE resource.client_code = 'pc'
  AND resource.perms IN (
    'system:collaboration:api:app:remove',
    'system:collaboration:api:binding:add',
    'system:collaboration:api:binding:remove'
  )
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_resource existing
    WHERE existing.tenant_id = 1
      AND existing.role_id = admin_role.id
      AND existing.resource_id = resource.id
  );
