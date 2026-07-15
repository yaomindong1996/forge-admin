-- 应用优先低代码工作台 Phase 5：不可变应用版本、协调发布运行单和恢复审计。

CREATE TABLE IF NOT EXISTS ai_business_application_version (
  id bigint NOT NULL COMMENT '主键ID',
  tenant_id bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  application_id bigint NOT NULL COMMENT '业务应用ID',
  version_no int NOT NULL COMMENT '应用内单调版本号',
  snapshot_json longtext NOT NULL COMMENT '不可变应用发布快照，不保存密钥',
  snapshot_hash char(64) NOT NULL COMMENT '快照SHA-256摘要',
  publish_status varchar(24) NOT NULL COMMENT 'PUBLISHED/ROLLBACK',
  publish_summary varchar(1000) DEFAULT NULL COMMENT '脱敏发布摘要',
  source_version_no int DEFAULT NULL COMMENT '回滚来源版本号',
  published_by bigint DEFAULT NULL COMMENT '可信发布人ID',
  published_time datetime NOT NULL COMMENT '发布时间',
  del_flag char(1) NOT NULL DEFAULT '0' COMMENT '删除标志：0正常 1删除',
  logic_delete_active tinyint GENERATED ALWAYS AS (CASE WHEN del_flag = '0' THEN 1 ELSE NULL END) STORED,
  create_by bigint DEFAULT NULL COMMENT '创建人',
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  create_dept bigint DEFAULT NULL COMMENT '创建部门',
  update_by bigint DEFAULT NULL COMMENT '更新人',
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_ai_business_application_version_active (
    tenant_id, application_id, version_no, logic_delete_active
  ),
  KEY idx_ai_business_application_version_hash (
    tenant_id, application_id, snapshot_hash, del_flag
  ),
  KEY idx_ai_business_application_version_time (
    tenant_id, application_id, published_time, del_flag
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='低代码业务应用不可变发布版本';

CREATE TABLE IF NOT EXISTS ai_business_application_publish_run (
  id bigint NOT NULL COMMENT '主键ID',
  tenant_id bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  application_id bigint NOT NULL COMMENT '业务应用ID',
  idempotency_key varchar(128) NOT NULL COMMENT '应用内发布幂等键',
  operation_type varchar(16) NOT NULL DEFAULT 'PUBLISH' COMMENT 'PUBLISH/ROLLBACK',
  target_version_no int NOT NULL COMMENT '本次发布目标版本号',
  source_version_no int DEFAULT NULL COMMENT '回滚来源版本号',
  run_status varchar(24) NOT NULL DEFAULT 'CREATED' COMMENT 'CREATED/RUNNING/PARTIAL/FAILED/SUCCESS',
  current_step varchar(32) DEFAULT NULL COMMENT '当前或失败步骤',
  snapshot_json longtext NOT NULL COMMENT '候选发布快照，不保存密钥',
  snapshot_hash char(64) NOT NULL COMMENT '候选快照SHA-256摘要',
  selection_json json NOT NULL COMMENT '本次选择及自动补齐的依赖资产',
  step_results_json json NOT NULL COMMENT '可恢复步骤结果，不保存原始异常',
  result_version_id bigint DEFAULT NULL COMMENT '成功生成的不可变应用版本ID',
  error_code varchar(64) DEFAULT NULL COMMENT '脱敏错误码',
  error_summary varchar(500) DEFAULT NULL COMMENT '脱敏错误摘要',
  attempt_count int NOT NULL DEFAULT 1 COMMENT '发布或恢复尝试次数',
  started_by bigint DEFAULT NULL COMMENT '可信发起人ID',
  started_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '首次发起时间',
  finished_time datetime DEFAULT NULL COMMENT '完成或最近失败时间',
  del_flag char(1) NOT NULL DEFAULT '0' COMMENT '删除标志：0正常 1删除',
  logic_delete_active tinyint GENERATED ALWAYS AS (CASE WHEN del_flag = '0' THEN 1 ELSE NULL END) STORED,
  create_by bigint DEFAULT NULL COMMENT '创建人',
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  create_dept bigint DEFAULT NULL COMMENT '创建部门',
  update_by bigint DEFAULT NULL COMMENT '更新人',
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_ai_business_publish_run_key_active (
    tenant_id, application_id, idempotency_key, logic_delete_active
  ),
  UNIQUE KEY uk_ai_business_publish_run_version_active (
    tenant_id, application_id, target_version_no, logic_delete_active
  ),
  KEY idx_ai_business_publish_run_status (
    tenant_id, application_id, run_status, update_time, del_flag
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='低代码业务应用协调发布运行单';

INSERT INTO sys_dict_type (
  tenant_id, dict_name, dict_type, dict_status, remark,
  create_by, create_time, update_by, update_time, create_dept
)
SELECT seed.tenant_id, seed.dict_name, seed.dict_type, 1, seed.remark,
       1, NOW(), 1, NOW(), 1
FROM (
  SELECT 1 tenant_id, '业务应用发布状态' dict_name,
         'ai_business_application_publish_status' dict_type,
         '应用协调发布运行单和不可变版本状态' remark
  UNION ALL
  SELECT 1, '业务应用发布步骤', 'ai_business_application_publish_step',
         '应用协调发布的预检查、对象、入口、扩展和提交步骤'
) seed
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_dict_type t
  WHERE t.tenant_id = seed.tenant_id
    AND t.dict_type = seed.dict_type
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
  SELECT 1 tenant_id, 1 dict_sort, '待执行' dict_label, 'CREATED' dict_value,
         'ai_business_application_publish_status' dict_type, 'info' list_class, 'Y' is_default,
         '运行单已预留，等待执行器认领' remark
  UNION ALL SELECT 1, 2, '执行中', 'RUNNING', 'ai_business_application_publish_status', 'info', 'N',
         '协调发布正在执行' remark
  UNION ALL SELECT 1, 3, '部分完成', 'PARTIAL', 'ai_business_application_publish_status', 'warning', 'N', '部分步骤完成，可从失败位置恢复'
  UNION ALL SELECT 1, 4, '发布失败', 'FAILED', 'ai_business_application_publish_status', 'error', 'N', '尚未产生可用发布版本'
  UNION ALL SELECT 1, 5, '发布成功', 'SUCCESS', 'ai_business_application_publish_status', 'success', 'N', '不可变应用版本已经提交'
  UNION ALL SELECT 1, 6, '已发布版本', 'PUBLISHED', 'ai_business_application_publish_status', 'success', 'N', '正常协调发布生成的版本'
  UNION ALL SELECT 1, 7, '回滚版本', 'ROLLBACK', 'ai_business_application_publish_status', 'warning', 'N', '从历史版本恢复生成的新版本'

  UNION ALL SELECT 1, 1, '发布预检查', 'PRECHECK', 'ai_business_application_publish_step', 'default', 'Y', '聚合发布阻断项和依赖'
  UNION ALL SELECT 1, 2, '准备快照', 'SNAPSHOT', 'ai_business_application_publish_step', 'default', 'N', '生成候选快照和稳定摘要'
  UNION ALL SELECT 1, 3, '发布业务对象', 'OBJECTS', 'ai_business_application_publish_step', 'info', 'N', '发布选择对象及自动补齐依赖'
  UNION ALL SELECT 1, 4, '切换页面入口', 'ENTRIES', 'ai_business_application_publish_step', 'info', 'N', '校验入口并同步运行菜单'
  UNION ALL SELECT 1, 5, '启用业务扩展', 'EXTENSIONS', 'ai_business_application_publish_step', 'warning', 'N', '启用已测试扩展版本'
  UNION ALL SELECT 1, 6, '提交应用版本', 'COMMIT', 'ai_business_application_publish_step', 'success', 'N', '写入不可变版本并提交应用状态'
) seed
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_dict_data d
  WHERE d.tenant_id = seed.tenant_id
    AND d.dict_type = seed.dict_type
    AND d.dict_value = seed.dict_value
);

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
  tenant_id, resource_name, parent_id, resource_type, sort,
  is_external, open_target, is_public, menu_status, visible, perms,
  keep_alive, always_show, remark, create_by, create_time,
  update_by, update_time, create_dept, client_code
)
SELECT 1, seed.resource_name, @application_menu_id, 3, seed.sort,
       0, '_self', 0, 1, 1, seed.perms,
       0, 0, seed.remark, 1, NOW(), 1, NOW(), 1, 'pc'
FROM (
  SELECT '发布业务应用' resource_name, 71 sort, 'ai:businessApplication:publish' perms,
         '执行应用就绪度检查和协调发布' remark
  UNION ALL SELECT '恢复应用发布', 72, 'ai:businessApplication:recover', '恢复部分失败的协调发布运行单'
  UNION ALL SELECT '回滚业务应用', 73, 'ai:businessApplication:rollback', '从历史应用快照生成回滚发布版本'
) seed
WHERE @application_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM sys_resource r
    WHERE r.tenant_id = 1
      AND r.perms = seed.perms
      AND r.del_flag = 0
  );

-- 仅继承已有应用编辑权限，不向无设计权限角色扩大发布权限。
INSERT INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT DISTINCT 1, old_role_resource.role_id, new_resource.id, NOW()
FROM (
  SELECT 'ai:businessApplication:edit' old_perms, 'ai:businessApplication:publish' new_perms
  UNION ALL SELECT 'ai:businessApplication:edit', 'ai:businessApplication:recover'
  UNION ALL SELECT 'ai:businessApplication:edit', 'ai:businessApplication:rollback'
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
  FROM sys_role_resource existing
  WHERE existing.tenant_id = 1
    AND existing.role_id = old_role_resource.role_id
    AND existing.resource_id = new_resource.id
);
