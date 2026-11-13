-- 应用级业务流程编排器：状态字典、应用发布步骤和权限资源。

INSERT INTO sys_dict_type (
  tenant_id, dict_name, dict_type, dict_status, remark,
  create_by, create_time, update_by, update_time, create_dept
)
SELECT seed.tenant_id, seed.dict_name, seed.dict_type, 1, seed.remark,
       1, NOW(), 1, NOW(), 1
FROM (
  SELECT 1 tenant_id, '业务流程设计状态' dict_name,
         'ai_business_process_design_status' dict_type,
         '应用级业务流程草稿、校验、发布和变更状态' remark
  UNION ALL
  SELECT 1, '业务流程运行状态', 'ai_business_process_run_status',
         '应用级业务流程运行实例状态'
  UNION ALL
  SELECT 1, '业务流程节点状态', 'ai_business_process_node_status',
         '应用级业务流程节点尝试状态'
  UNION ALL
  SELECT 1, '业务流程触发来源', 'ai_business_process_trigger_type',
         '手动、事件、定时、流程回调和外部可信触发来源'
) seed
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_dict_type type_row
  WHERE type_row.tenant_id = seed.tenant_id
    AND type_row.dict_type = seed.dict_type
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
  SELECT 1 tenant_id, 1 dict_sort, '草稿' dict_label, 'DRAFT' dict_value,
         'ai_business_process_design_status' dict_type, 'default' list_class, 'Y' is_default,
         '流程草稿正在编辑' remark
  UNION ALL SELECT 1, 2, '校验通过', 'VALIDATED', 'ai_business_process_design_status', 'info', 'N', '当前草稿已通过发布校验'
  UNION ALL SELECT 1, 3, '已发布', 'PUBLISHED', 'ai_business_process_design_status', 'success', 'N', '当前草稿与已发布版本一致'
  UNION ALL SELECT 1, 4, '有未发布变更', 'CHANGED', 'ai_business_process_design_status', 'warning', 'N', '已发布流程存在新草稿变更'

  UNION ALL SELECT 1, 1, '待执行', 'PENDING', 'ai_business_process_run_status', 'default', 'Y', '运行实例已持久化，等待调度'
  UNION ALL SELECT 1, 2, '执行中', 'RUNNING', 'ai_business_process_run_status', 'info', 'N', '业务节点正在执行'
  UNION ALL SELECT 1, 3, '等待审批', 'WAITING', 'ai_business_process_run_status', 'warning', 'N', '等待Flowable或子流程结果'
  UNION ALL SELECT 1, 4, '执行成功', 'SUCCESS', 'ai_business_process_run_status', 'success', 'N', '业务流程已成功结束'
  UNION ALL SELECT 1, 5, '执行失败', 'FAILED', 'ai_business_process_run_status', 'error', 'N', '业务流程失败，可按策略重试'
  UNION ALL SELECT 1, 6, '已取消', 'CANCELED', 'ai_business_process_run_status', 'default', 'N', '业务流程已取消'

  UNION ALL SELECT 1, 1, '待执行', 'PENDING', 'ai_business_process_node_status', 'default', 'Y', '节点尝试等待认领'
  UNION ALL SELECT 1, 2, '执行中', 'RUNNING', 'ai_business_process_node_status', 'info', 'N', '节点尝试正在执行'
  UNION ALL SELECT 1, 3, '等待结果', 'WAITING', 'ai_business_process_node_status', 'warning', 'N', '节点等待审批、子流程或受治理能力结果'
  UNION ALL SELECT 1, 4, '执行成功', 'SUCCESS', 'ai_business_process_node_status', 'success', 'N', '节点尝试已成功完成'
  UNION ALL SELECT 1, 5, '执行失败', 'FAILED', 'ai_business_process_node_status', 'error', 'N', '节点尝试失败'
  UNION ALL SELECT 1, 6, '已取消', 'CANCELED', 'ai_business_process_node_status', 'default', 'N', '节点尝试已取消'

  UNION ALL SELECT 1, 1, '手动触发', 'MANUAL', 'ai_business_process_trigger_type', 'info', 'Y', '当前登录用户通过页面动作触发'
  UNION ALL SELECT 1, 2, '记录事件', 'EVENT', 'ai_business_process_trigger_type', 'success', 'N', '业务事务完成后的可信记录事件触发'
  UNION ALL SELECT 1, 3, '定时扫描', 'SCHEDULE', 'ai_business_process_trigger_type', 'warning', 'N', '统一调度任务以受限服务身份触发'
  UNION ALL SELECT 1, 4, '流程回调', 'PROCESS_CALLBACK', 'ai_business_process_trigger_type', 'default', 'N', 'Flowable结果恢复等待节点'
  UNION ALL SELECT 1, 5, '外部能力', 'EXTERNAL', 'ai_business_process_trigger_type', 'error', 'N', '经过统一能力平台认证授权的触发'
) seed
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_dict_data data_row
  WHERE data_row.tenant_id = seed.tenant_id
    AND data_row.dict_type = seed.dict_type
    AND data_row.dict_value = seed.dict_value
);

INSERT INTO sys_dict_data (
  tenant_id, dict_sort, dict_label, dict_value, dict_type,
  css_class, list_class, is_default, dict_status, remark,
  create_by, create_time, update_by, update_time, create_dept
)
SELECT 1, 3, '发布业务流程', 'PROCESSES', 'ai_business_application_publish_step',
       NULL, 'info', 'N', 1,
       '校验流程草稿、生成不可变版本并固定审批模型和动作依赖',
       1, NOW(), 1, NOW(), 1
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_dict_data data_row
  WHERE data_row.tenant_id = 1
    AND data_row.dict_type = 'ai_business_application_publish_step'
    AND data_row.dict_value = 'PROCESSES'
);

UPDATE sys_dict_data
SET dict_sort = CASE dict_value
      WHEN 'PRECHECK' THEN 1
      WHEN 'SNAPSHOT' THEN 2
      WHEN 'PROCESSES' THEN 3
      WHEN 'OBJECTS' THEN 4
      WHEN 'ENTRIES' THEN 5
      WHEN 'PAGE_MENUS' THEN 6
      WHEN 'EXTENSIONS' THEN 7
      WHEN 'COMMIT' THEN 8
      ELSE dict_sort
    END,
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND dict_type = 'ai_business_application_publish_step'
  AND dict_value IN ('PRECHECK', 'SNAPSHOT', 'PROCESSES', 'OBJECTS', 'ENTRIES', 'PAGE_MENUS', 'EXTENSIONS', 'COMMIT');

SET @application_menu_id = (
  SELECT id
  FROM sys_resource
  WHERE tenant_id = 1
    AND resource_type = 2
    AND path = '/app-center'
    AND del_flag = 0
  ORDER BY id
  LIMIT 1
);

INSERT INTO sys_resource (
  tenant_id, resource_name, parent_id, resource_type, sort, path, component, icon,
  is_external, open_target, is_public, menu_status, visible, perms,
  keep_alive, always_show, remark, create_by, create_time,
  update_by, update_time, create_dept, client_code
)
SELECT 1, '业务流程设计器', @application_menu_id, 2, 90,
       '/app-center/business-process/:processId', 'app-center/business-process.[processId]',
       'ionicons5:GitNetworkOutline', 0, '_self', 0, 1, 0,
       'ai:businessProcess:designer', 0, 0,
       '应用级业务流程全屏设计器隐藏路由', 1, NOW(), 1, NOW(), 1, 'pc'
WHERE @application_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM sys_resource resource_row
    WHERE resource_row.tenant_id = 1
      AND resource_row.resource_type = 2
      AND resource_row.path = '/app-center/business-process/:processId'
      AND resource_row.del_flag = 0
  );

SET @process_designer_menu_id = (
  SELECT id
  FROM sys_resource
  WHERE tenant_id = 1
    AND resource_type = 2
    AND path = '/app-center/business-process/:processId'
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
SELECT 1, seed.resource_name, @process_designer_menu_id, 3, seed.sort,
       0, '_self', 0, 1, 1, seed.perms,
       0, 0, seed.remark, 1, NOW(), 1, NOW(), 1, 'pc'
FROM (
  SELECT '查看业务流程' resource_name, 1 sort, 'ai:businessProcess:list' perms, '查询应用级业务流程定义' remark
  UNION ALL SELECT '新增业务流程', 2, 'ai:businessProcess:add', '新增应用级业务流程草稿'
  UNION ALL SELECT '编辑业务流程', 3, 'ai:businessProcess:edit', '编辑流程基础信息和业务编排草稿'
  UNION ALL SELECT '复制业务流程', 4, 'ai:businessProcess:copy', '在同一应用内复制业务流程草稿'
  UNION ALL SELECT '启停业务流程', 5, 'ai:businessProcess:status', '启用或停用业务流程新触发'
  UNION ALL SELECT '删除业务流程', 6, 'ai:businessProcess:delete', '逻辑删除无运行和发布引用的流程'
  UNION ALL SELECT '校验业务流程', 7, 'ai:businessProcess:validate', '执行流程图、对象、字段和依赖校验'
  UNION ALL SELECT '查询流程运行', 8, 'ai:businessProcess:run:list', '查询业务流程运行记录'
  UNION ALL SELECT '查看运行详情', 9, 'ai:businessProcess:run:detail', '查看运行节点时间线和安全摘要'
  UNION ALL SELECT '重试流程运行', 10, 'ai:businessProcess:run:retry', '人工重试可恢复的失败流程'
  UNION ALL SELECT '取消流程运行', 11, 'ai:businessProcess:run:cancel', '取消尚未进入不可逆终态的流程'
  UNION ALL SELECT '预览流程迁移', 12, 'ai:businessProcess:migration:preview', '预览旧触发器、绑定和动作迁移结果'
  UNION ALL SELECT '执行流程迁移', 13, 'ai:businessProcess:migration:apply', '按一致预览签名执行幂等迁移'
  UNION ALL SELECT '查看迁移问题', 14, 'ai:businessProcess:migration:issues', '查询无法自动转换和字段失效问题'
) seed
WHERE @process_designer_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM sys_resource resource_row
    WHERE resource_row.tenant_id = 1
      AND resource_row.perms = seed.perms
      AND resource_row.del_flag = 0
  );

-- 沿用既有应用设计、发布权限，不向没有应用权限的角色扩大业务流程权限。
INSERT INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT DISTINCT 1, old_role_resource.role_id, new_resource.id, NOW()
FROM (
  SELECT 'ai:businessApplication:list' old_perms, 'ai:businessProcess:designer' new_perms
  UNION ALL SELECT 'ai:businessApplication:list', 'ai:businessProcess:list'
  UNION ALL SELECT 'ai:businessApplication:list', 'ai:businessProcess:run:list'
  UNION ALL SELECT 'ai:businessApplication:list', 'ai:businessProcess:run:detail'
  UNION ALL SELECT 'ai:businessApplication:list', 'ai:businessProcess:migration:issues'
  UNION ALL SELECT 'ai:businessApplication:edit', 'ai:businessProcess:add'
  UNION ALL SELECT 'ai:businessApplication:edit', 'ai:businessProcess:edit'
  UNION ALL SELECT 'ai:businessApplication:edit', 'ai:businessProcess:copy'
  UNION ALL SELECT 'ai:businessApplication:edit', 'ai:businessProcess:status'
  UNION ALL SELECT 'ai:businessApplication:edit', 'ai:businessProcess:delete'
  UNION ALL SELECT 'ai:businessApplication:edit', 'ai:businessProcess:validate'
  UNION ALL SELECT 'ai:businessApplication:edit', 'ai:businessProcess:migration:preview'
  UNION ALL SELECT 'ai:businessApplication:publish', 'ai:businessProcess:run:retry'
  UNION ALL SELECT 'ai:businessApplication:publish', 'ai:businessProcess:run:cancel'
  UNION ALL SELECT 'ai:businessApplication:publish', 'ai:businessProcess:migration:apply'
) permission_mapping
INNER JOIN sys_resource old_resource
  ON old_resource.tenant_id = 1
 AND old_resource.perms = permission_mapping.old_perms
 AND old_resource.del_flag = 0
INNER JOIN sys_role_resource old_role_resource
  ON old_role_resource.tenant_id = 1
 AND old_role_resource.resource_id = old_resource.id
INNER JOIN sys_resource new_resource
  ON new_resource.tenant_id = 1
 AND new_resource.perms = permission_mapping.new_perms
 AND new_resource.del_flag = 0
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_role_resource existing_row
  WHERE existing_row.tenant_id = 1
    AND existing_row.role_id = old_role_resource.role_id
    AND existing_row.resource_id = new_resource.id
);
