-- 低代码字段级数据变更审计：策略、游标、事件、字段明细、管理员对象授权与原值访问留痕。
-- 事件/字段/游标/访问为追加式证据，不提供行级删除接口。

CREATE TABLE IF NOT EXISTS `ai_data_audit_policy` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  `object_id` bigint NOT NULL COMMENT '业务对象ID',
  `enabled` tinyint NOT NULL DEFAULT 0 COMMENT '是否启用：1启用 0未启用',
  `reason_required` tinyint NOT NULL DEFAULT 1 COMMENT '人工修改/删除是否必填原因',
  `policy_version` int NOT NULL DEFAULT 1 COMMENT '策略版本',
  `enabled_at` datetime DEFAULT NULL COMMENT '首次启用时间',
  `write_barrier` tinyint NOT NULL DEFAULT 0 COMMENT '启用切换写入屏障',
  `coverage_status` varchar(32) NOT NULL DEFAULT 'PENDING' COMMENT '覆盖检查：PENDING/PASSED/BLOCKED',
  `coverage_json` longtext COMMENT '覆盖检查结果快照',
  `policy_snapshot` longtext COMMENT '策略快照 JSON',
  `del_flag` bigint NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0正常，删除后写主键',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_data_audit_policy_object` (`tenant_id`, `object_id`, `del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务对象数据变更审计策略';

CREATE TABLE IF NOT EXISTS `ai_data_audit_cursor` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  `object_id` bigint NOT NULL COMMENT '业务对象ID',
  `record_id` varchar(128) NOT NULL COMMENT '规范化记录主键',
  `revision` bigint NOT NULL DEFAULT 0 COMMENT '当前审计修订号',
  `last_event_id` bigint DEFAULT NULL COMMENT '最近事件ID',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_data_audit_cursor_record` (`tenant_id`, `object_id`, `record_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据变更审计聚合游标';

CREATE TABLE IF NOT EXISTS `ai_data_audit_event` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  `object_id` bigint NOT NULL COMMENT '业务对象ID',
  `object_code` varchar(128) NOT NULL COMMENT '对象编码快照',
  `object_name` varchar(128) DEFAULT NULL COMMENT '对象名称快照',
  `record_id` varchar(128) NOT NULL COMMENT '规范化记录主键',
  `record_label` varchar(255) DEFAULT NULL COMMENT '记录显示名快照',
  `revision` bigint NOT NULL COMMENT '审计修订号',
  `event_type` varchar(32) NOT NULL COMMENT 'CREATE/UPDATE/DELETE',
  `occurred_at` datetime NOT NULL COMMENT '事件生成时间',
  `operation_id` varchar(64) NOT NULL COMMENT '本次操作ID',
  `parent_operation_id` varchar(64) DEFAULT NULL COMMENT '父操作ID',
  `correlation_id` varchar(64) DEFAULT NULL COMMENT '关联ID',
  `source_type` varchar(32) NOT NULL COMMENT '来源类型',
  `source_event_id` varchar(64) DEFAULT NULL COMMENT '来源事件ID',
  `source_version` varchar(64) DEFAULT NULL COMMENT '来源版本',
  `source_application_id` bigint DEFAULT NULL COMMENT '来源应用ID',
  `source_page_id` bigint DEFAULT NULL COMMENT '来源页面ID',
  `config_key` varchar(128) DEFAULT NULL COMMENT '运行配置键快照',
  `schema_version` int DEFAULT NULL COMMENT 'Schema 版本快照',
  `policy_version` int NOT NULL COMMENT '策略版本快照',
  `policy_snapshot` longtext COMMENT '策略快照',
  `actor_type` varchar(32) NOT NULL COMMENT 'USER/SERVICE/SYSTEM',
  `actor_id` varchar(64) DEFAULT NULL COMMENT '操作者ID',
  `actor_name` varchar(128) DEFAULT NULL COMMENT '操作者名称快照',
  `client_id` varchar(64) DEFAULT NULL COMMENT '客户端ID',
  `delegated_user_id` varchar(64) DEFAULT NULL COMMENT '委托人ID',
  `change_reason` varchar(500) DEFAULT NULL COMMENT '修改原因',
  `reason_code` varchar(64) DEFAULT NULL COMMENT '系统原因码',
  `flow_instance_id` varchar(64) DEFAULT NULL COMMENT '流程实例ID',
  `task_id` varchar(64) DEFAULT NULL COMMENT '任务ID',
  `action_execution_id` varchar(64) DEFAULT NULL COMMENT '动作执行ID',
  `changed_field_count` int NOT NULL DEFAULT 0 COMMENT '变化字段数',
  `changed_row_count` int NOT NULL DEFAULT 0 COMMENT '变化行数',
  `protection_metadata` varchar(255) DEFAULT NULL COMMENT '加密版本摘要',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_data_audit_event_revision` (`tenant_id`, `object_id`, `record_id`, `revision`),
  KEY `idx_ai_data_audit_event_record_time` (`tenant_id`, `object_id`, `record_id`, `occurred_at`, `id`),
  KEY `idx_ai_data_audit_event_time` (`tenant_id`, `occurred_at`, `id`),
  KEY `idx_ai_data_audit_event_actor` (`tenant_id`, `actor_id`, `occurred_at`),
  KEY `idx_ai_data_audit_event_operation` (`tenant_id`, `operation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据变更审计事件';

CREATE TABLE IF NOT EXISTS `ai_data_audit_field` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  `event_id` bigint NOT NULL COMMENT '事件ID',
  `target_object_id` bigint NOT NULL COMMENT '目标对象ID',
  `target_model_id` bigint DEFAULT NULL COMMENT '目标模型ID',
  `target_record_id` varchar(128) NOT NULL COMMENT '目标记录ID',
  `relation_key` varchar(128) NOT NULL DEFAULT '' COMMENT '子表关系键，主表用空串',
  `field_path` varchar(255) NOT NULL COMMENT '字段路径',
  `field_code` varchar(128) NOT NULL COMMENT '字段编码快照',
  `column_name` varchar(128) DEFAULT NULL COMMENT '物理列名快照',
  `field_label` varchar(255) DEFAULT NULL COMMENT '字段名称快照',
  `field_type` varchar(64) DEFAULT NULL COMMENT '字段类型快照',
  `field_metadata` longtext COMMENT '字典/关联等解释快照',
  `change_type` varchar(16) NOT NULL COMMENT 'ADD/UPDATE/REMOVE',
  `source_type` varchar(32) NOT NULL COMMENT '字段直接来源',
  `before_state` varchar(16) NOT NULL COMMENT 'ABSENT/NULL/VALUE/OMITTED',
  `after_state` varchar(16) NOT NULL COMMENT 'ABSENT/NULL/VALUE/OMITTED',
  `before_value` longtext COMMENT '前值 JSON 或密文',
  `after_value` longtext COMMENT '后值 JSON 或密文',
  `before_display` longtext COMMENT '前值显示快照',
  `after_display` longtext COMMENT '后值显示快照',
  `value_protection` varchar(16) NOT NULL DEFAULT 'PLAIN' COMMENT 'PLAIN/ENCRYPTED/CHANGE_ONLY',
  `key_version` varchar(64) DEFAULT NULL COMMENT '密钥版本',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_ai_data_audit_field_event` (`tenant_id`, `event_id`),
  KEY `idx_ai_data_audit_field_target` (`tenant_id`, `target_object_id`, `field_code`, `event_id`),
  KEY `idx_ai_data_audit_field_record` (`tenant_id`, `target_object_id`, `target_record_id`, `event_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据变更审计字段明细';

CREATE TABLE IF NOT EXISTS `ai_data_audit_scope` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `object_id` bigint NOT NULL COMMENT '审计对象ID',
  `object_code` varchar(128) DEFAULT NULL COMMENT '对象编码快照',
  `object_name` varchar(128) DEFAULT NULL COMMENT '对象名称快照',
  `del_flag` bigint NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0正常，删除后写主键',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_data_audit_scope_role_object` (`tenant_id`, `role_id`, `object_id`, `del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员数据审计对象授权';

CREATE TABLE IF NOT EXISTS `ai_data_audit_access` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  `event_id` bigint NOT NULL COMMENT '事件ID',
  `field_id` bigint NOT NULL COMMENT '字段明细ID',
  `actor_id` varchar(64) NOT NULL COMMENT '访问者ID',
  `actor_name` varchar(128) DEFAULT NULL COMMENT '访问者名称快照',
  `access_reason` varchar(500) NOT NULL COMMENT '查看原因',
  `access_time` datetime NOT NULL COMMENT '访问时间',
  `access_result` varchar(16) NOT NULL COMMENT 'GRANTED/DENIED',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_ai_data_audit_access_field` (`tenant_id`, `event_id`, `field_id`, `access_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='敏感原值查看留痕';

INSERT INTO sys_dict_type (
    tenant_id, dict_name, dict_type, dict_status, remark,
    create_by, create_time, update_by, update_time, create_dept
)
SELECT 1, '数据审计事件类型', 'sys_data_audit_event_type', 1,
       '数据变更审计事件类型', 1, NOW(), 1, NOW(), 1
WHERE NOT EXISTS (
    SELECT 1 FROM sys_dict_type
    WHERE tenant_id = 1 AND dict_type = 'sys_data_audit_event_type'
);

INSERT INTO sys_dict_data (
    tenant_id, dict_sort, dict_label, dict_value, dict_type,
    css_class, list_class, is_default, dict_status, remark,
    create_by, create_time, update_by, update_time, create_dept
)
SELECT seed.tenant_id, seed.dict_sort, seed.dict_label, seed.dict_value,
       'sys_data_audit_event_type', NULL, seed.list_class, seed.is_default,
       1, seed.remark, 1, NOW(), 1, NOW(), 1
FROM (
    SELECT 1 tenant_id, 1 dict_sort, '新增' dict_label, 'CREATE' dict_value,
           'success' list_class, 'N' is_default, '新增记录' remark
    UNION ALL SELECT 1, 2, '修改', 'UPDATE', 'info', 'Y', '修改记录'
    UNION ALL SELECT 1, 3, '删除', 'DELETE', 'error', 'N', '删除记录'
) seed
WHERE NOT EXISTS (
    SELECT 1 FROM sys_dict_data d
    WHERE d.tenant_id = seed.tenant_id
      AND d.dict_type = 'sys_data_audit_event_type'
      AND d.dict_value = seed.dict_value
);

INSERT INTO sys_dict_type (
    tenant_id, dict_name, dict_type, dict_status, remark,
    create_by, create_time, update_by, update_time, create_dept
)
SELECT 1, '数据审计来源类型', 'sys_data_audit_source_type', 1,
       '数据变更审计来源', 1, NOW(), 1, NOW(), 1
WHERE NOT EXISTS (
    SELECT 1 FROM sys_dict_type
    WHERE tenant_id = 1 AND dict_type = 'sys_data_audit_source_type'
);

INSERT INTO sys_dict_data (
    tenant_id, dict_sort, dict_label, dict_value, dict_type,
    css_class, list_class, is_default, dict_status, remark,
    create_by, create_time, update_by, update_time, create_dept
)
SELECT seed.tenant_id, seed.dict_sort, seed.dict_label, seed.dict_value,
       'sys_data_audit_source_type', NULL, seed.list_class, seed.is_default,
       1, seed.remark, 1, NOW(), 1, NOW(), 1
FROM (
    SELECT 1 tenant_id, 1 dict_sort, '表单' dict_label, 'FORM' dict_value,
           'info' list_class, 'Y' is_default, '普通表单写入' remark
    UNION ALL SELECT 1, 2, '流程表单', 'FLOW_FORM', 'info', 'N', '待办或重提表单'
    UNION ALL SELECT 1, 3, '流程回写', 'FLOW_CALLBACK', 'warning', 'N', '流程状态回写'
    UNION ALL SELECT 1, 4, '业务动作', 'BUSINESS_ACTION', 'info', 'N', '业务动作或命令'
    UNION ALL SELECT 1, 5, '自动化', 'AUTOMATION', 'warning', 'N', '触发器或定时自动化'
    UNION ALL SELECT 1, 6, '导入', 'IMPORT', 'success', 'N', 'Excel 导入'
    UNION ALL SELECT 1, 7, '公式', 'FORMULA', 'info', 'N', '存储公式或聚合刷新'
    UNION ALL SELECT 1, 8, '扩展', 'EXTENSION', 'default', 'N', '扩展或其它平台写入'
) seed
WHERE NOT EXISTS (
    SELECT 1 FROM sys_dict_data d
    WHERE d.tenant_id = seed.tenant_id
      AND d.dict_type = 'sys_data_audit_source_type'
      AND d.dict_value = seed.dict_value
);

INSERT INTO sys_dict_type (
    tenant_id, dict_name, dict_type, dict_status, remark,
    create_by, create_time, update_by, update_time, create_dept
)
SELECT 1, '数据审计主体类型', 'sys_data_audit_actor_type', 1,
       '数据变更审计操作主体', 1, NOW(), 1, NOW(), 1
WHERE NOT EXISTS (
    SELECT 1 FROM sys_dict_type
    WHERE tenant_id = 1 AND dict_type = 'sys_data_audit_actor_type'
);

INSERT INTO sys_dict_data (
    tenant_id, dict_sort, dict_label, dict_value, dict_type,
    css_class, list_class, is_default, dict_status, remark,
    create_by, create_time, update_by, update_time, create_dept
)
SELECT seed.tenant_id, seed.dict_sort, seed.dict_label, seed.dict_value,
       'sys_data_audit_actor_type', NULL, seed.list_class, seed.is_default,
       1, seed.remark, 1, NOW(), 1, NOW(), 1
FROM (
    SELECT 1 tenant_id, 1 dict_sort, '用户' dict_label, 'USER' dict_value,
           'info' list_class, 'Y' is_default, '登录用户' remark
    UNION ALL SELECT 1, 2, '服务', 'SERVICE', 'warning', 'N', '已认证服务身份'
    UNION ALL SELECT 1, 3, '系统', 'SYSTEM', 'default', 'N', '系统执行'
) seed
WHERE NOT EXISTS (
    SELECT 1 FROM sys_dict_data d
    WHERE d.tenant_id = seed.tenant_id
      AND d.dict_type = 'sys_data_audit_actor_type'
      AND d.dict_value = seed.dict_value
);

INSERT INTO sys_dict_type (
    tenant_id, dict_name, dict_type, dict_status, remark,
    create_by, create_time, update_by, update_time, create_dept
)
SELECT 1, '数据审计值保护策略', 'sys_data_audit_value_protection', 1,
       '历史值保护级别', 1, NOW(), 1, NOW(), 1
WHERE NOT EXISTS (
    SELECT 1 FROM sys_dict_type
    WHERE tenant_id = 1 AND dict_type = 'sys_data_audit_value_protection'
);

INSERT INTO sys_dict_data (
    tenant_id, dict_sort, dict_label, dict_value, dict_type,
    css_class, list_class, is_default, dict_status, remark,
    create_by, create_time, update_by, update_time, create_dept
)
SELECT seed.tenant_id, seed.dict_sort, seed.dict_label, seed.dict_value,
       'sys_data_audit_value_protection', NULL, seed.list_class, seed.is_default,
       1, seed.remark, 1, NOW(), 1, NOW(), 1
FROM (
    SELECT 1 tenant_id, 1 dict_sort, '明文' dict_label, 'PLAIN' dict_value,
           'default' list_class, 'Y' is_default, '普通字段完整值' remark
    UNION ALL SELECT 1, 2, '加密', 'ENCRYPTED', 'warning', 'N', '敏感值加密存储'
    UNION ALL SELECT 1, 3, '仅记录变更', 'CHANGE_ONLY', 'error', 'N', '凭据不保存可恢复原值'
) seed
WHERE NOT EXISTS (
    SELECT 1 FROM sys_dict_data d
    WHERE d.tenant_id = seed.tenant_id
      AND d.dict_type = 'sys_data_audit_value_protection'
      AND d.dict_value = seed.dict_value
);

SET @monitor_menu_id := (
  SELECT id
  FROM sys_resource
  WHERE tenant_id = 1
    AND resource_type IN (1, 2)
    AND path = '/system/monitor'
    AND del_flag = 0
  LIMIT 1
);

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external,
                          sso_enabled, sso_target_client, open_target, is_public, menu_status, visible, perms, icon,
                          api_method, api_url, keep_alive, always_show, redirect, remark, create_by, create_time,
                          update_by, update_time, create_dept, client_code)
SELECT 1, '数据变更记录', @monitor_menu_id, 2, 3, '/system/data-change-audit', 'system/data-change-audit', 0,
       0, NULL, '_self', 0, 1, 1, 'ai:dataAudit:list', 'ionicons5:DocumentsOutline',
       NULL, NULL, 1, 0, NULL, '业务数据字段级变更审计', 1, NOW(), 1, NOW(), 1, 'pc'
WHERE @monitor_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM sys_resource
    WHERE tenant_id = 1
      AND resource_type = 2
      AND path = '/system/data-change-audit'
      AND del_flag = 0
  );

SET @data_audit_menu_id := (
  SELECT id
  FROM sys_resource
  WHERE tenant_id = 1
    AND resource_type = 2
    AND path = '/system/data-change-audit'
    AND del_flag = 0
  LIMIT 1
);

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external,
                          sso_enabled, sso_target_client, open_target, is_public, menu_status, visible, perms, icon,
                          api_method, api_url, keep_alive, always_show, redirect, remark, create_by, create_time,
                          update_by, update_time, create_dept, client_code)
SELECT seed.tenant_id, seed.resource_name, @data_audit_menu_id, seed.resource_type, seed.sort, NULL, NULL, 0,
       0, NULL, '_self', 0, 1, 1, seed.perms, NULL,
       seed.api_method, seed.api_url, 0, 0, NULL, seed.remark, 1, NOW(), 1, NOW(), 1, 'pc'
FROM (
  SELECT 1 tenant_id, '记录历史查看' resource_name, 3 resource_type, 1 sort, 'ai:dataAudit:record' perms,
         NULL api_method, NULL api_url, '表单详情查看当前记录字段历史' remark
  UNION ALL SELECT 1, '审计查询', 3, 2, 'ai:dataAudit:list',
         NULL, NULL, '管理员按授权对象查询数据变更'
  UNION ALL SELECT 1, '事件详情', 3, 3, 'ai:dataAudit:detail',
         NULL, NULL, '查看获授权对象的事件与字段差异'
  UNION ALL SELECT 1, '敏感原值查看', 3, 4, 'ai:dataAudit:sensitive',
         NULL, NULL, '显式查看可恢复敏感历史值'
  UNION ALL SELECT 1, '审计策略配置', 3, 5, 'ai:dataAudit:config',
         NULL, NULL, '启用和维护对象数据审计策略'
  UNION ALL SELECT 1, '审计对象授权', 3, 6, 'ai:dataAudit:scope',
         NULL, NULL, '管理角色可审计对象范围'
  UNION ALL SELECT 1, '记录历史分页接口', 4, 11, 'ai:dataAudit:api:recordPage',
         'GET', '/ai/data-audit/record/{objectId}/{recordId}/page', '表单详情事件分页'
  UNION ALL SELECT 1, '管理员事件分页接口', 4, 12, 'ai:dataAudit:api:page',
         'GET', '/ai/data-audit/page', '管理员事件分页'
  UNION ALL SELECT 1, '事件详情接口', 4, 13, 'ai:dataAudit:api:detail',
         'GET', '/ai/data-audit/{eventId}', '事件摘要'
  UNION ALL SELECT 1, '字段差异分页接口', 4, 14, 'ai:dataAudit:api:fields',
         'GET', '/ai/data-audit/{eventId}/fields/page', '字段差异分页'
  UNION ALL SELECT 1, '敏感原值查看接口', 4, 15, 'ai:dataAudit:api:reveal',
         'POST', '/ai/data-audit/{eventId}/fields/{fieldId}/reveal', '查看单字段可恢复原值'
  UNION ALL SELECT 1, '策略查询接口', 4, 16, 'ai:dataAudit:api:policyGet',
         'GET', '/ai/data-audit/policy/{objectId}', '查询对象审计策略'
  UNION ALL SELECT 1, '策略更新接口', 4, 17, 'ai:dataAudit:api:policyPut',
         'PUT', '/ai/data-audit/policy/{objectId}', '启用对象审计策略'
  UNION ALL SELECT 1, '授权查询接口', 4, 18, 'ai:dataAudit:api:scopeGet',
         'GET', '/ai/data-audit/scope/{roleId}', '查询角色审计对象授权'
  UNION ALL SELECT 1, '授权更新接口', 4, 19, 'ai:dataAudit:api:scopePut',
         'PUT', '/ai/data-audit/scope/{roleId}', '更新角色审计对象授权'
) seed
WHERE @data_audit_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM sys_resource r
    WHERE r.tenant_id = seed.tenant_id
      AND r.resource_type = seed.resource_type
      AND r.perms = seed.perms
      AND r.del_flag = 0
  );
