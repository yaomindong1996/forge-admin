-- 应用优先低代码工作台 Phase 4：受治理扩展、不可变内容版本和脱敏执行审计。

CREATE TABLE IF NOT EXISTS ai_business_extension (
  id bigint NOT NULL COMMENT '主键ID',
  tenant_id bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  application_id bigint NOT NULL COMMENT '所属业务应用ID',
  object_id bigint DEFAULT NULL COMMENT '可选业务对象ID',
  entry_id bigint DEFAULT NULL COMMENT '可选访问入口ID',
  extension_code varchar(64) NOT NULL COMMENT '扩展稳定编码',
  extension_name varchar(128) NOT NULL COMMENT '扩展名称',
  extension_type varchar(32) NOT NULL COMMENT 'VISUAL_RULE/CLIENT_JS/SCOPED_CSS/SERVER_BINDING',
  hook_code varchar(32) NOT NULL COMMENT '标准扩展钩子编码',
  scope_type varchar(32) NOT NULL DEFAULT 'APPLICATION' COMMENT 'APPLICATION/OBJECT/ENTRY/PAGE/COMPONENT',
  scope_key varchar(255) DEFAULT NULL COMMENT '页面或组件作用域键',
  sort_order int NOT NULL DEFAULT 0 COMMENT '同钩子执行顺序',
  failure_policy varchar(16) NOT NULL DEFAULT 'BLOCK' COMMENT 'BLOCK/WARN/IGNORE',
  risk_level varchar(16) NOT NULL DEFAULT 'MEDIUM' COMMENT 'LOW/MEDIUM/HIGH',
  status varchar(16) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/TESTED/ENABLED/DISABLED',
  draft_version int NOT NULL DEFAULT 1 COMMENT '当前草稿版本',
  enabled_version int DEFAULT NULL COMMENT '当前运行版本',
  lock_user_id bigint DEFAULT NULL COMMENT '编辑锁持有人',
  lock_username varchar(128) DEFAULT NULL COMMENT '编辑锁持有人名称',
  lock_token_hash varchar(64) DEFAULT NULL COMMENT '编辑锁令牌SHA-256摘要',
  lock_time datetime DEFAULT NULL COMMENT '编辑锁获得或续期时间',
  lock_expire_time datetime DEFAULT NULL COMMENT '编辑锁过期时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  del_flag char(1) NOT NULL DEFAULT '0' COMMENT '删除标志：0正常 1删除',
  logic_delete_active tinyint GENERATED ALWAYS AS (CASE WHEN del_flag = '0' THEN 1 ELSE NULL END) STORED,
  create_by bigint DEFAULT NULL COMMENT '创建人',
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  create_dept bigint DEFAULT NULL COMMENT '创建部门',
  update_by bigint DEFAULT NULL COMMENT '更新人',
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_ai_business_extension_code_active (
    tenant_id, application_id, extension_code, logic_delete_active
  ),
  KEY idx_ai_business_extension_hook (
    tenant_id, application_id, hook_code, status, sort_order, del_flag
  ),
  KEY idx_ai_business_extension_object (tenant_id, object_id, status, del_flag),
  KEY idx_ai_business_extension_entry (tenant_id, entry_id, status, del_flag),
  KEY idx_ai_business_extension_lock (tenant_id, lock_user_id, lock_expire_time, del_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='低代码受治理业务扩展';

CREATE TABLE IF NOT EXISTS ai_business_extension_version (
  id bigint NOT NULL COMMENT '主键ID',
  tenant_id bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  extension_id bigint NOT NULL COMMENT '业务扩展ID',
  version_no int NOT NULL COMMENT '扩展内单调版本号',
  content longtext NOT NULL COMMENT '扩展原始内容',
  processed_content longtext DEFAULT NULL COMMENT '服务端复核的作用域或编译结果',
  config_json json NOT NULL COMMENT '非敏感结构化配置',
  content_hash char(64) NOT NULL COMMENT '内容SHA-256摘要',
  validation_passed tinyint NOT NULL DEFAULT 0 COMMENT '校验是否通过',
  validation_summary varchar(1000) DEFAULT NULL COMMENT '校验摘要，不含原始输入',
  test_passed tinyint NOT NULL DEFAULT 0 COMMENT '受限测试是否通过',
  test_summary varchar(1000) DEFAULT NULL COMMENT '测试摘要，不含原始输入',
  change_summary varchar(500) DEFAULT NULL COMMENT '版本变更说明',
  del_flag char(1) NOT NULL DEFAULT '0' COMMENT '删除标志：0正常 1删除',
  logic_delete_active tinyint GENERATED ALWAYS AS (CASE WHEN del_flag = '0' THEN 1 ELSE NULL END) STORED,
  create_by bigint DEFAULT NULL COMMENT '创建人',
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  create_dept bigint DEFAULT NULL COMMENT '创建部门',
  update_by bigint DEFAULT NULL COMMENT '更新人',
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_ai_business_extension_version_active (
    tenant_id, extension_id, version_no, logic_delete_active
  ),
  KEY idx_ai_business_extension_version_hash (tenant_id, extension_id, content_hash, del_flag),
  KEY idx_ai_business_extension_version_created (tenant_id, extension_id, create_time, del_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='低代码业务扩展不可变内容版本';

CREATE TABLE IF NOT EXISTS ai_business_extension_execution_log (
  id bigint NOT NULL COMMENT '主键ID',
  tenant_id bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  extension_id bigint NOT NULL COMMENT '业务扩展ID',
  extension_code varchar(64) NOT NULL COMMENT '扩展编码快照',
  version_no int NOT NULL COMMENT '执行版本号',
  application_id bigint NOT NULL COMMENT '业务应用ID',
  object_id bigint DEFAULT NULL COMMENT '业务对象ID',
  entry_id bigint DEFAULT NULL COMMENT '访问入口ID',
  hook_code varchar(32) NOT NULL COMMENT '执行钩子',
  result_status varchar(16) NOT NULL COMMENT 'SUCCESS/FAILED',
  duration_ms bigint NOT NULL DEFAULT 0 COMMENT '执行耗时毫秒',
  error_code varchar(64) DEFAULT NULL COMMENT '脱敏错误编码',
  error_summary varchar(500) DEFAULT NULL COMMENT '脱敏错误摘要',
  actor_user_id bigint DEFAULT NULL COMMENT '可信操作者ID',
  del_flag char(1) NOT NULL DEFAULT '0' COMMENT '删除标志：0正常 1删除',
  create_by bigint DEFAULT NULL COMMENT '创建人',
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  create_dept bigint DEFAULT NULL COMMENT '创建部门',
  update_by bigint DEFAULT NULL COMMENT '更新人',
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_ai_business_extension_audit_extension (tenant_id, extension_id, create_time, del_flag),
  KEY idx_ai_business_extension_audit_app (tenant_id, application_id, hook_code, create_time, del_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='低代码业务扩展脱敏执行审计';

INSERT INTO sys_dict_type (
  tenant_id, dict_name, dict_type, dict_status, remark,
  create_by, create_time, update_by, update_time, create_dept
)
SELECT seed.tenant_id, seed.dict_name, seed.dict_type, 1, seed.remark,
       1, NOW(), 1, NOW(), 1
FROM (
  SELECT 1 tenant_id, '业务扩展类型' dict_name, 'ai_business_extension_type' dict_type,
         '可视化规则、客户端脚本、作用域样式和服务端白名单绑定' remark
  UNION ALL SELECT 1, '业务扩展状态', 'ai_business_extension_status', '扩展草稿、测试、启用和停用状态'
  UNION ALL SELECT 1, '业务扩展钩子', 'ai_business_extension_hook', '对象级和页面级标准扩展钩子'
  UNION ALL SELECT 1, '业务扩展失败策略', 'ai_business_extension_failure_policy', '阻断、警告和忽略失败策略'
  UNION ALL SELECT 1, '业务扩展风险级别', 'ai_business_extension_risk_level', '低、中、高扩展风险级别'
  UNION ALL SELECT 1, '可视化规则操作符', 'ai_business_extension_rule_operator', '可视化条件比较操作符'
  UNION ALL SELECT 1, '可视化规则动作', 'ai_business_extension_rule_action', '可视化规则受限动作'
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
  SELECT 1 tenant_id, 1 dict_sort, '可视化规则' dict_label, 'VISUAL_RULE' dict_value,
         'ai_business_extension_type' dict_type, 'success' list_class, 'Y' is_default, '面向业务人员的条件和动作配置' remark
  UNION ALL SELECT 1, 2, '客户端 JS', 'CLIENT_JS', 'ai_business_extension_type', 'info', 'N', '在专用 Worker 沙箱执行的受限脚本'
  UNION ALL SELECT 1, 3, '作用域 CSS', 'SCOPED_CSS', 'ai_business_extension_type', 'warning', 'N', '只作用于应用和页面根节点的样式'
  UNION ALL SELECT 1, 4, '服务端能力绑定', 'SERVER_BINDING', 'ai_business_extension_type', 'default', 'N', '仅绑定平台注册处理器'

  UNION ALL SELECT 1, 1, '草稿', 'DRAFT', 'ai_business_extension_status', 'default', 'Y', '内容待校验和测试'
  UNION ALL SELECT 1, 2, '已测试', 'TESTED', 'ai_business_extension_status', 'info', 'N', '当前草稿校验和测试通过'
  UNION ALL SELECT 1, 3, '已启用', 'ENABLED', 'ai_business_extension_status', 'success', 'N', '运行态使用已启用版本'
  UNION ALL SELECT 1, 4, '已停用', 'DISABLED', 'ai_business_extension_status', 'warning', 'N', '运行态不再执行'

  UNION ALL SELECT 1, 1, '新增前', 'BEFORE_CREATE', 'ai_business_extension_hook', 'warning', 'N', '新增记录前'
  UNION ALL SELECT 1, 2, '新增后', 'AFTER_CREATE', 'ai_business_extension_hook', 'default', 'N', '新增记录后'
  UNION ALL SELECT 1, 3, '修改前', 'BEFORE_UPDATE', 'ai_business_extension_hook', 'warning', 'N', '修改记录前'
  UNION ALL SELECT 1, 4, '修改后', 'AFTER_UPDATE', 'ai_business_extension_hook', 'default', 'N', '修改记录后'
  UNION ALL SELECT 1, 5, '删除前', 'BEFORE_DELETE', 'ai_business_extension_hook', 'error', 'N', '删除记录前'
  UNION ALL SELECT 1, 6, '删除后', 'AFTER_DELETE', 'ai_business_extension_hook', 'default', 'N', '删除记录后'
  UNION ALL SELECT 1, 7, '导入前', 'BEFORE_IMPORT', 'ai_business_extension_hook', 'warning', 'N', '导入前'
  UNION ALL SELECT 1, 8, '导入后', 'AFTER_IMPORT', 'ai_business_extension_hook', 'default', 'N', '导入后'
  UNION ALL SELECT 1, 9, '导出前', 'BEFORE_EXPORT', 'ai_business_extension_hook', 'warning', 'N', '导出前'
  UNION ALL SELECT 1, 10, '导出后', 'AFTER_EXPORT', 'ai_business_extension_hook', 'default', 'N', '导出后'
  UNION ALL SELECT 1, 11, '列表查询前', 'BEFORE_LIST', 'ai_business_extension_hook', 'warning', 'N', '列表查询前'
  UNION ALL SELECT 1, 12, '列表查询后', 'AFTER_LIST', 'ai_business_extension_hook', 'default', 'N', '列表查询后'
  UNION ALL SELECT 1, 13, '详情查询前', 'BEFORE_DETAIL', 'ai_business_extension_hook', 'warning', 'N', '详情查询前'
  UNION ALL SELECT 1, 14, '详情查询后', 'AFTER_DETAIL', 'ai_business_extension_hook', 'default', 'N', '详情查询后'
  UNION ALL SELECT 1, 15, '汇总前', 'BEFORE_SUMMARY', 'ai_business_extension_hook', 'warning', 'N', '汇总统计前'
  UNION ALL SELECT 1, 16, '汇总后', 'AFTER_SUMMARY', 'ai_business_extension_hook', 'default', 'N', '汇总统计后'
  UNION ALL SELECT 1, 17, '页面初始化', 'PAGE_INIT', 'ai_business_extension_hook', 'info', 'N', '页面根节点初始化'
  UNION ALL SELECT 1, 18, '表单变化', 'FORM_CHANGE', 'ai_business_extension_hook', 'info', 'N', '白名单表单字段变化'
  UNION ALL SELECT 1, 19, '提交前', 'BEFORE_SUBMIT', 'ai_business_extension_hook', 'warning', 'Y', '表单提交前'
  UNION ALL SELECT 1, 20, '提交后', 'AFTER_SUBMIT', 'ai_business_extension_hook', 'default', 'N', '表单提交后'
  UNION ALL SELECT 1, 21, '行操作', 'ROW_ACTION', 'ai_business_extension_hook', 'info', 'N', '受控列表行动作'

  UNION ALL SELECT 1, 1, '阻断', 'BLOCK', 'ai_business_extension_failure_policy', 'error', 'Y', '失败时阻断当前业务动作'
  UNION ALL SELECT 1, 2, '警告并继续', 'WARN', 'ai_business_extension_failure_policy', 'warning', 'N', '记录警告后继续'
  UNION ALL SELECT 1, 3, '忽略并继续', 'IGNORE', 'ai_business_extension_failure_policy', 'default', 'N', '仅低风险后置钩子可选'

  UNION ALL SELECT 1, 1, '低风险', 'LOW', 'ai_business_extension_risk_level', 'success', 'Y', '只读或低影响扩展'
  UNION ALL SELECT 1, 2, '中风险', 'MEDIUM', 'ai_business_extension_risk_level', 'warning', 'N', '可能修改当前页面或业务字段'
  UNION ALL SELECT 1, 3, '高风险', 'HIGH', 'ai_business_extension_risk_level', 'error', 'N', '前置或服务端高影响扩展'

  UNION ALL SELECT 1, 1, '等于', 'EQ', 'ai_business_extension_rule_operator', 'default', 'Y', '字段值等于目标值'
  UNION ALL SELECT 1, 2, '不等于', 'NE', 'ai_business_extension_rule_operator', 'default', 'N', '字段值不等于目标值'
  UNION ALL SELECT 1, 3, '大于', 'GT', 'ai_business_extension_rule_operator', 'default', 'N', '字段值大于目标值'
  UNION ALL SELECT 1, 4, '大于等于', 'GE', 'ai_business_extension_rule_operator', 'default', 'N', '字段值大于等于目标值'
  UNION ALL SELECT 1, 5, '小于', 'LT', 'ai_business_extension_rule_operator', 'default', 'N', '字段值小于目标值'
  UNION ALL SELECT 1, 6, '小于等于', 'LE', 'ai_business_extension_rule_operator', 'default', 'N', '字段值小于等于目标值'
  UNION ALL SELECT 1, 7, '包含', 'CONTAINS', 'ai_business_extension_rule_operator', 'default', 'N', '字段值包含目标值'
  UNION ALL SELECT 1, 8, '为空', 'EMPTY', 'ai_business_extension_rule_operator', 'default', 'N', '字段值为空'

  UNION ALL SELECT 1, 1, '设置字段', 'SET_FIELD', 'ai_business_extension_rule_action', 'info', 'Y', '设置白名单字段值'
  UNION ALL SELECT 1, 2, '显示消息', 'SHOW_MESSAGE', 'ai_business_extension_rule_action', 'default', 'N', '显示页面消息'
  UNION ALL SELECT 1, 3, '触发白名单动作', 'TRIGGER_ACTION', 'ai_business_extension_rule_action', 'warning', 'N', '触发当前应用已注册动作'
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
  SELECT '查看业务扩展' resource_name, 61 sort, 'ai:businessExtension:list' perms, '查询业务扩展和白名单目录' remark
  UNION ALL SELECT '新增业务扩展', 62, 'ai:businessExtension:add', '新增业务扩展草稿'
  UNION ALL SELECT '编辑业务扩展', 63, 'ai:businessExtension:edit', '编辑扩展元数据、内容和编辑锁'
  UNION ALL SELECT '删除业务扩展', 64, 'ai:businessExtension:delete', '逻辑删除停用扩展'
  UNION ALL SELECT '校验业务扩展', 65, 'ai:businessExtension:validate', '执行扩展安全校验'
  UNION ALL SELECT '测试业务扩展', 66, 'ai:businessExtension:test', '执行受限扩展测试'
  UNION ALL SELECT '启停业务扩展', 67, 'ai:businessExtension:status', '启用已测试版本或停用扩展'
  UNION ALL SELECT '回滚业务扩展', 68, 'ai:businessExtension:rollback', '从历史版本生成新草稿'
) seed
WHERE @application_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM sys_resource r
    WHERE r.tenant_id = 1
      AND r.perms = seed.perms
      AND r.del_flag = 0
  );

-- 仅继承已有应用查看/编辑等价权限，不给无应用设计权限的角色扩大扩展权限。
INSERT INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT DISTINCT 1, old_role_resource.role_id, new_resource.id, NOW()
FROM (
  SELECT 'ai:businessApplication:list' old_perms, 'ai:businessExtension:list' new_perms
  UNION ALL SELECT 'ai:businessApplication:add', 'ai:businessExtension:add'
  UNION ALL SELECT 'ai:businessApplication:edit', 'ai:businessExtension:edit'
  UNION ALL SELECT 'ai:businessApplication:delete', 'ai:businessExtension:delete'
  UNION ALL SELECT 'ai:businessApplication:edit', 'ai:businessExtension:validate'
  UNION ALL SELECT 'ai:businessApplication:edit', 'ai:businessExtension:test'
  UNION ALL SELECT 'ai:businessApplication:status', 'ai:businessExtension:status'
  UNION ALL SELECT 'ai:businessApplication:edit', 'ai:businessExtension:rollback'
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
