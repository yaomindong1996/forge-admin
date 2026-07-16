-- 结构化编码规则：保留 ai_code_rule 主表，新增稳定分段定义并迁移既有内置规则。

CREATE TABLE IF NOT EXISTS `ai_code_rule` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  `rule_code` varchar(64) NOT NULL COMMENT '规则编码',
  `rule_name` varchar(128) NOT NULL COMMENT '规则名称',
  `scene` varchar(64) NOT NULL DEFAULT 'COMMON' COMMENT '兼容适用场景',
  `category` varchar(64) NOT NULL DEFAULT 'COMMON' COMMENT '编码分类',
  `source_object_id` bigint DEFAULT NULL COMMENT '业务字段来源对象ID',
  `source_object_code` varchar(64) DEFAULT NULL COMMENT '业务字段来源对象编码',
  `template` varchar(256) NOT NULL COMMENT '兼容模板摘要',
  `reset_policy` varchar(32) NOT NULL DEFAULT 'NONE' COMMENT '兼容重置策略',
  `seq_length` int NOT NULL DEFAULT 4 COMMENT '兼容流水长度',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
  `builtin` tinyint NOT NULL DEFAULT 0 COMMENT '是否内置规则',
  `in_code_list` tinyint NOT NULL DEFAULT 1 COMMENT '是否可在业务配置中选择',
  `version_no` int NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `options` json DEFAULT NULL COMMENT '扩展配置JSON',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志：0正常 1删除',
  `logic_delete_active` tinyint GENERATED ALWAYS AS (CASE WHEN `del_flag` = '0' THEN 1 ELSE NULL END) STORED COMMENT '未删除唯一约束标识',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_code_rule_code_active` (`tenant_id`, `rule_code`, `logic_delete_active`),
  KEY `idx_ai_code_rule_category` (`tenant_id`, `category`, `status`, `del_flag`),
  KEY `idx_ai_code_rule_source_object` (`tenant_id`, `source_object_code`, `status`, `del_flag`),
  KEY `idx_ai_code_rule_update` (`tenant_id`, `update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通用编码规则';

SET @column_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_code_rule' AND COLUMN_NAME = 'category'
);
SET @sql = IF(@column_exists = 0,
  'ALTER TABLE ai_code_rule ADD COLUMN category varchar(64) NOT NULL DEFAULT ''COMMON'' COMMENT ''编码分类'' AFTER scene',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_code_rule' AND COLUMN_NAME = 'source_object_id'
);
SET @sql = IF(@column_exists = 0,
  'ALTER TABLE ai_code_rule ADD COLUMN source_object_id bigint DEFAULT NULL COMMENT ''业务字段来源对象ID'' AFTER category',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_code_rule' AND COLUMN_NAME = 'source_object_code'
);
SET @sql = IF(@column_exists = 0,
  'ALTER TABLE ai_code_rule ADD COLUMN source_object_code varchar(64) DEFAULT NULL COMMENT ''业务字段来源对象编码'' AFTER source_object_id',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_code_rule' AND COLUMN_NAME = 'in_code_list'
);
SET @sql = IF(@column_exists = 0,
  'ALTER TABLE ai_code_rule ADD COLUMN in_code_list tinyint NOT NULL DEFAULT 1 COMMENT ''是否可在业务配置中选择'' AFTER builtin',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_code_rule' AND COLUMN_NAME = 'version_no'
);
SET @sql = IF(@column_exists = 0,
  'ALTER TABLE ai_code_rule ADD COLUMN version_no int NOT NULL DEFAULT 1 COMMENT ''乐观锁版本号'' AFTER in_code_list',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_code_rule' AND COLUMN_NAME = 'del_flag'
);
SET @sql = IF(@column_exists = 0,
  'ALTER TABLE ai_code_rule ADD COLUMN del_flag char(1) NOT NULL DEFAULT ''0'' COMMENT ''删除标志：0正常 1删除'' AFTER options',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @index_exists = (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_code_rule' AND INDEX_NAME = 'idx_ai_code_rule_source_object'
);
SET @sql = IF(@index_exists = 0,
  'CREATE INDEX idx_ai_code_rule_source_object ON ai_code_rule (tenant_id, source_object_code, status, del_flag)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_code_rule' AND COLUMN_NAME = 'logic_delete_active'
);
SET @sql = IF(@column_exists = 0,
  'ALTER TABLE ai_code_rule ADD COLUMN logic_delete_active tinyint GENERATED ALWAYS AS (CASE WHEN del_flag = ''0'' THEN 1 ELSE NULL END) STORED COMMENT ''未删除唯一约束标识'' AFTER del_flag',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @index_exists = (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_code_rule' AND INDEX_NAME = 'uk_ai_code_rule_code_active'
);
SET @sql = IF(@index_exists = 0,
  'CREATE UNIQUE INDEX uk_ai_code_rule_code_active ON ai_code_rule (tenant_id, rule_code, logic_delete_active)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @index_exists = (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_code_rule' AND INDEX_NAME = 'uk_ai_code_rule_code'
);
SET @sql = IF(@index_exists > 0,
  'ALTER TABLE ai_code_rule DROP INDEX uk_ai_code_rule_code',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @index_exists = (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_code_rule' AND INDEX_NAME = 'idx_ai_code_rule_category'
);
SET @sql = IF(@index_exists = 0,
  'CREATE INDEX idx_ai_code_rule_category ON ai_code_rule (tenant_id, category, status, del_flag)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE ai_code_rule
SET category = CASE
  WHEN rule_code = 'material_code' THEN 'MATERIAL'
  WHEN rule_code = 'customer_code' THEN 'CUSTOMER'
  WHEN rule_code = 'supplier_code' THEN 'SUPPLIER'
  WHEN rule_code = 'warehouse_code' THEN 'WAREHOUSE'
  WHEN rule_code IN ('document_daily_no', 'order_no', 'contract_no', 'purchase_no', 'outbound_no', 'transfer_no', 'org_daily_no') THEN 'DOCUMENT'
  WHEN (category IS NULL OR category = '' OR category = 'COMMON')
       AND scene IN ('ORDER', 'CONTRACT') THEN 'DOCUMENT'
  WHEN (category IS NULL OR category = '' OR category = 'COMMON')
       AND scene IN ('COMMON', 'DOCUMENT', 'MATERIAL', 'CUSTOMER', 'SUPPLIER', 'WAREHOUSE',
                     'CARRIER', 'CONTAINER', 'EQUIPMENT', 'LOCATION') THEN scene
  ELSE COALESCE(NULLIF(category, ''), 'COMMON')
END
WHERE category IS NULL
   OR category = ''
   OR (category = 'COMMON' AND rule_code IN (
     'material_code', 'customer_code', 'supplier_code', 'warehouse_code',
     'document_daily_no', 'order_no', 'contract_no', 'purchase_no',
     'outbound_no', 'transfer_no', 'org_daily_no'
   ))
   OR (category = 'COMMON' AND scene IN (
     'DOCUMENT', 'MATERIAL', 'CUSTOMER', 'SUPPLIER', 'WAREHOUSE',
     'CARRIER', 'CONTAINER', 'EQUIPMENT', 'LOCATION', 'ORDER', 'CONTRACT'
   ));

CREATE TABLE IF NOT EXISTS `ai_code_rule_segment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '分段ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  `rule_id` bigint NOT NULL COMMENT '编码规则ID',
  `segment_key` varchar(32) NOT NULL COMMENT '稳定分段键',
  `segment_order` int NOT NULL COMMENT '分段顺序，从1开始',
  `segment_type` varchar(16) NOT NULL COMMENT 'DATE/FIXED/SEQ/VARIABLE/SYS_VAR',
  `segment_value` varchar(128) DEFAULT NULL COMMENT '格式、固定值或变量名',
  `segment_length` int DEFAULT NULL COMMENT '声明长度或最大长度',
  `pad_enabled` tinyint NOT NULL DEFAULT 0 COMMENT '是否补位',
  `pad_char` varchar(4) DEFAULT NULL COMMENT '补位字符',
  `pad_direction` varchar(8) NOT NULL DEFAULT 'LEFT' COMMENT 'LEFT/RIGHT',
  `group_enabled` tinyint NOT NULL DEFAULT 0 COMMENT '是否参与流水分组',
  `include_in_code` tinyint NOT NULL DEFAULT 1 COMMENT '是否输出到最终编码',
  `radix_type` varchar(32) DEFAULT NULL COMMENT 'SEQ进制类型',
  `reset_enabled` tinyint NOT NULL DEFAULT 0 COMMENT '是否按周期切换计数器',
  `reset_policy` varchar(16) NOT NULL DEFAULT 'NONE' COMMENT 'NONE/YEAR/MONTH/DAY/HOUR',
  `start_value` bigint NOT NULL DEFAULT 1 COMMENT '新计数器起始值',
  `exclude_ambiguous` tinyint NOT NULL DEFAULT 0 COMMENT '是否移除I/O/Z等易混淆字符',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志：0正常 1删除',
  `logic_delete_active` tinyint GENERATED ALWAYS AS (CASE WHEN `del_flag` = '0' THEN 1 ELSE NULL END) STORED COMMENT '未删除唯一约束标识',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_code_rule_segment_key_active` (`tenant_id`, `rule_id`, `segment_key`, `logic_delete_active`),
  KEY `idx_ai_code_rule_segment_order` (`tenant_id`, `rule_id`, `segment_order`, `del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='编码规则结构化分段';

INSERT INTO sys_dict_type (
  tenant_id, dict_name, dict_type, dict_status, remark,
  create_by, create_time, update_by, update_time, create_dept
)
SELECT 1, '编码规则分类', 'sys_code_rule_category', 1, '平台级编码规则业务分类',
       1, NOW(), 1, NOW(), 1
WHERE NOT EXISTS (
  SELECT 1 FROM sys_dict_type
  WHERE tenant_id = 1 AND dict_type = 'sys_code_rule_category'
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
  SELECT 1 tenant_id, 1 dict_sort, '通用编码' dict_label, 'COMMON' dict_value,
         'sys_code_rule_category' dict_type, 'default' list_class, 'Y' is_default, '跨业务通用编码' remark
  UNION ALL SELECT 1, 2, '单据编码', 'DOCUMENT', 'sys_code_rule_category', 'info', 'N', '订单、合同和业务单据'
  UNION ALL SELECT 1, 3, '物料编码', 'MATERIAL', 'sys_code_rule_category', 'success', 'N', '物料和产品主数据'
  UNION ALL SELECT 1, 4, '客户编码', 'CUSTOMER', 'sys_code_rule_category', 'info', 'N', '客户主数据'
  UNION ALL SELECT 1, 5, '供应商编码', 'SUPPLIER', 'sys_code_rule_category', 'warning', 'N', '供应商主数据'
  UNION ALL SELECT 1, 6, '仓库编码', 'WAREHOUSE', 'sys_code_rule_category', 'success', 'N', '仓库主数据'
  UNION ALL SELECT 1, 7, '载具编码', 'CARRIER', 'sys_code_rule_category', 'default', 'N', '托盘和载具'
  UNION ALL SELECT 1, 8, '容器编码', 'CONTAINER', 'sys_code_rule_category', 'default', 'N', '料箱和料盒'
  UNION ALL SELECT 1, 9, '设备编码', 'EQUIPMENT', 'sys_code_rule_category', 'warning', 'N', '设备资产'
  UNION ALL SELECT 1, 10, '库位编码', 'LOCATION', 'sys_code_rule_category', 'info', 'N', '库区和库位'
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM sys_dict_data data
  WHERE data.tenant_id = seed.tenant_id
    AND data.dict_type = seed.dict_type
    AND data.dict_value = seed.dict_value
);

INSERT INTO sys_dict_type (
  tenant_id, dict_name, dict_type, dict_status, remark,
  create_by, create_time, update_by, update_time, create_dept
)
SELECT 1, '编码规则适用场景', 'sys_code_rule_scene', 1, '编码规则兼容筛选和适用范围',
       1, NOW(), 1, NOW(), 1
WHERE NOT EXISTS (
  SELECT 1 FROM sys_dict_type
  WHERE tenant_id = 1 AND dict_type = 'sys_code_rule_scene'
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
  SELECT 1 tenant_id, 1 dict_sort, '通用场景' dict_label, 'COMMON' dict_value,
         'sys_code_rule_scene' dict_type, 'default' list_class, 'Y' is_default, '所有低代码业务对象均可筛选' remark
  UNION ALL SELECT 1, 2, '单据场景', 'DOCUMENT', 'sys_code_rule_scene', 'info', 'N', '订单、合同和业务单据'
  UNION ALL SELECT 1, 3, '订单场景', 'ORDER', 'sys_code_rule_scene', 'info', 'N', '兼容历史订单规则筛选'
  UNION ALL SELECT 1, 4, '合同场景', 'CONTRACT', 'sys_code_rule_scene', 'info', 'N', '兼容历史合同规则筛选'
  UNION ALL SELECT 1, 5, '物料场景', 'MATERIAL', 'sys_code_rule_scene', 'success', 'N', '物料和产品主数据'
  UNION ALL SELECT 1, 6, '客户场景', 'CUSTOMER', 'sys_code_rule_scene', 'info', 'N', '客户主数据'
  UNION ALL SELECT 1, 7, '供应商场景', 'SUPPLIER', 'sys_code_rule_scene', 'warning', 'N', '供应商主数据'
  UNION ALL SELECT 1, 8, '仓库场景', 'WAREHOUSE', 'sys_code_rule_scene', 'success', 'N', '仓库主数据'
  UNION ALL SELECT 1, 9, '载具场景', 'CARRIER', 'sys_code_rule_scene', 'default', 'N', '托盘和载具'
  UNION ALL SELECT 1, 10, '容器场景', 'CONTAINER', 'sys_code_rule_scene', 'default', 'N', '料箱和料盒'
  UNION ALL SELECT 1, 11, '设备场景', 'EQUIPMENT', 'sys_code_rule_scene', 'warning', 'N', '设备资产'
  UNION ALL SELECT 1, 12, '库位场景', 'LOCATION', 'sys_code_rule_scene', 'info', 'N', '库区和库位'
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM sys_dict_data data
  WHERE data.tenant_id = seed.tenant_id
    AND data.dict_type = seed.dict_type
    AND data.dict_value = seed.dict_value
);

-- 已知旧规则物化为分段；其它自定义占位模板由运行时 legacy parser 兼容并在首次保存时物化。
INSERT INTO ai_code_rule_segment (
  tenant_id, rule_id, segment_key, segment_order, segment_type, segment_value,
  segment_length, pad_enabled, pad_char, pad_direction, group_enabled, include_in_code,
  radix_type, reset_enabled, reset_policy, start_value, exclude_ambiguous,
  create_by, create_time, create_dept, update_by, update_time
)
SELECT rule.tenant_id, rule.id, seed.segment_key, seed.segment_order, seed.segment_type, seed.segment_value,
       seed.segment_length, seed.pad_enabled, seed.pad_char, 'LEFT', seed.group_enabled, 1,
       seed.radix_type, seed.reset_enabled, seed.reset_policy, 1, 0,
       COALESCE(rule.create_by, 1), NOW(), COALESCE(rule.create_dept, 1), COALESCE(rule.update_by, 1), NOW()
FROM (
  SELECT 'material_code' rule_code, 'fixed_1' segment_key, 1 segment_order, 'FIXED' segment_type, 'WL' segment_value,
         2 segment_length, 0 pad_enabled, NULL pad_char, 0 group_enabled, NULL radix_type, 0 reset_enabled, 'NONE' reset_policy
  UNION ALL SELECT 'material_code', 'date_1', 2, 'DATE', 'yyyyMMddHHmmss', 14, 0, NULL, 0, NULL, 0, 'NONE'
  UNION ALL SELECT 'material_code', 'sequence_1', 3, 'SEQ', NULL, 3, 1, '0', 0, 'DECIMAL', 1, 'HOUR'

  UNION ALL SELECT 'document_daily_no', 'fixed_1', 1, 'FIXED', 'DOC', 3, 0, NULL, 0, NULL, 0, 'NONE'
  UNION ALL SELECT 'document_daily_no', 'date_1', 2, 'DATE', 'yyyyMMdd', 8, 0, NULL, 0, NULL, 0, 'NONE'
  UNION ALL SELECT 'document_daily_no', 'sequence_1', 3, 'SEQ', NULL, 4, 1, '0', 0, 'DECIMAL', 1, 'DAY'

  UNION ALL SELECT 'order_no', 'fixed_1', 1, 'FIXED', 'ORD', 3, 0, NULL, 0, NULL, 0, 'NONE'
  UNION ALL SELECT 'order_no', 'date_1', 2, 'DATE', 'yyyyMMdd', 8, 0, NULL, 0, NULL, 0, 'NONE'
  UNION ALL SELECT 'order_no', 'sequence_1', 3, 'SEQ', NULL, 5, 1, '0', 0, 'DECIMAL', 1, 'DAY'

  UNION ALL SELECT 'contract_no', 'fixed_1', 1, 'FIXED', 'HT', 2, 0, NULL, 0, NULL, 0, 'NONE'
  UNION ALL SELECT 'contract_no', 'date_1', 2, 'DATE', 'yyyyMM', 6, 0, NULL, 0, NULL, 0, 'NONE'
  UNION ALL SELECT 'contract_no', 'sequence_1', 3, 'SEQ', NULL, 5, 1, '0', 0, 'DECIMAL', 1, 'MONTH'

  UNION ALL SELECT 'customer_code', 'fixed_1', 1, 'FIXED', 'C', 1, 0, NULL, 0, NULL, 0, 'NONE'
  UNION ALL SELECT 'customer_code', 'sequence_1', 2, 'SEQ', NULL, 6, 1, '0', 0, 'DECIMAL', 0, 'NONE'

  UNION ALL SELECT 'org_daily_no', 'system_org_1', 1, 'SYS_VAR', 'orgCode', NULL, 0, NULL, 1, NULL, 0, 'NONE'
  UNION ALL SELECT 'org_daily_no', 'date_1', 2, 'DATE', 'yyyyMMdd', 8, 0, NULL, 0, NULL, 0, 'NONE'
  UNION ALL SELECT 'org_daily_no', 'sequence_1', 3, 'SEQ', NULL, 4, 1, '0', 0, 'DECIMAL', 1, 'DAY'

  UNION ALL SELECT 'supplier_code', 'fixed_1', 1, 'FIXED', 'SUP', 3, 0, NULL, 0, NULL, 0, 'NONE'
  UNION ALL SELECT 'supplier_code', 'sequence_1', 2, 'SEQ', NULL, 5, 1, '0', 0, 'DECIMAL', 0, 'NONE'

  UNION ALL SELECT 'warehouse_code', 'fixed_1', 1, 'FIXED', 'WH', 2, 0, NULL, 0, NULL, 0, 'NONE'
  UNION ALL SELECT 'warehouse_code', 'sequence_1', 2, 'SEQ', NULL, 4, 1, '0', 0, 'DECIMAL', 0, 'NONE'

  UNION ALL SELECT 'purchase_no', 'fixed_1', 1, 'FIXED', 'CG', 2, 0, NULL, 0, NULL, 0, 'NONE'
  UNION ALL SELECT 'purchase_no', 'date_1', 2, 'DATE', 'yyyyMMdd', 8, 0, NULL, 0, NULL, 0, 'NONE'
  UNION ALL SELECT 'purchase_no', 'sequence_1', 3, 'SEQ', NULL, 4, 1, '0', 0, 'DECIMAL', 1, 'DAY'

  UNION ALL SELECT 'outbound_no', 'fixed_1', 1, 'FIXED', 'CK', 2, 0, NULL, 0, NULL, 0, 'NONE'
  UNION ALL SELECT 'outbound_no', 'date_1', 2, 'DATE', 'yyyyMMdd', 8, 0, NULL, 0, NULL, 0, 'NONE'
  UNION ALL SELECT 'outbound_no', 'sequence_1', 3, 'SEQ', NULL, 4, 1, '0', 0, 'DECIMAL', 1, 'DAY'

  UNION ALL SELECT 'transfer_no', 'fixed_1', 1, 'FIXED', 'DB', 2, 0, NULL, 0, NULL, 0, 'NONE'
  UNION ALL SELECT 'transfer_no', 'date_1', 2, 'DATE', 'yyyyMMdd', 8, 0, NULL, 0, NULL, 0, 'NONE'
  UNION ALL SELECT 'transfer_no', 'sequence_1', 3, 'SEQ', NULL, 4, 1, '0', 0, 'DECIMAL', 1, 'DAY'
) seed
INNER JOIN ai_code_rule rule
  ON rule.rule_code = seed.rule_code
 AND rule.del_flag = '0'
WHERE NOT EXISTS (
  SELECT 1 FROM ai_code_rule_segment segment
  WHERE segment.tenant_id = rule.tenant_id
    AND segment.rule_id = rule.id
    AND segment.segment_key = seed.segment_key
    AND segment.del_flag = '0'
);

SET @code_rule_menu_id = (
  SELECT id FROM sys_resource
  WHERE tenant_id = 1
    AND resource_type = 2
    AND path = '/app-center/code-rules'
    AND del_flag = 0
  ORDER BY id
  LIMIT 1
);

UPDATE sys_resource
SET perms = 'system:codeRule:list',
    resource_name = '编码规则',
    remark = '平台级结构化编码规则维护页面',
    update_by = 1,
    update_time = NOW()
WHERE id = @code_rule_menu_id
  AND (perms IS NULL OR perms = 'ai:businessObject:code-rules');

INSERT INTO sys_resource (
  tenant_id, resource_name, parent_id, resource_type, sort,
  is_external, open_target, is_public, menu_status, visible, perms,
  keep_alive, always_show, remark, create_by, create_time,
  update_by, update_time, create_dept, client_code
)
SELECT 1, seed.resource_name, @code_rule_menu_id, 3, seed.sort,
       0, '_self', 0, 1, 1, seed.perms,
       0, 0, seed.remark, 1, NOW(), 1, NOW(), 1, 'pc'
FROM (
  SELECT '新增编码规则' resource_name, 1 sort, 'system:codeRule:add' perms, '新增结构化编码规则' remark
  UNION ALL SELECT '修改编码规则', 2, 'system:codeRule:edit', '修改结构化编码规则及分段'
  UNION ALL SELECT '删除编码规则', 3, 'system:codeRule:remove', '逻辑删除自定义编码规则'
  UNION ALL SELECT '使用编码规则', 4, 'system:codeRule:use', '预览和生成业务编码'
) seed
WHERE @code_rule_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_resource resource
    WHERE resource.tenant_id = 1
      AND resource.resource_type = 3
      AND resource.perms = seed.perms
      AND resource.del_flag = 0
  );

INSERT INTO sys_resource (
  tenant_id, resource_name, parent_id, resource_type, sort,
  is_external, open_target, is_public, menu_status, visible, perms,
  api_method, api_url, keep_alive, always_show, remark,
  create_by, create_time, update_by, update_time, create_dept, client_code
)
SELECT 1, seed.resource_name, @code_rule_menu_id, 4, seed.sort,
       0, '_self', 0, 1, 1, seed.perms,
       seed.api_method, seed.api_url, 0, 0, seed.remark,
       1, NOW(), 1, NOW(), 1, 'pc'
FROM (
  SELECT '编码规则分页接口' resource_name, 11 sort, 'system:codeRule:api:page' perms,
         'GET' api_method, '/system/code-rule/page' api_url, '分页查询编码规则' remark
  UNION ALL SELECT '编码规则列表接口', 12, 'system:codeRule:api:list',
         'GET', '/system/code-rule/list', '查询可选择编码规则'
  UNION ALL SELECT '编码规则详情接口', 13, 'system:codeRule:api:detail',
         'POST', '/system/code-rule/getById', '查询编码规则及结构化分段'
  UNION ALL SELECT '编码规则新增接口', 14, 'system:codeRule:api:add',
         'POST', '/system/code-rule/add', '新增编码规则及分段'
  UNION ALL SELECT '编码规则修改接口', 15, 'system:codeRule:api:edit',
         'POST', '/system/code-rule/edit', '修改编码规则及分段'
  UNION ALL SELECT '编码规则删除接口', 16, 'system:codeRule:api:remove',
         'POST', '/system/code-rule/remove/*', '逻辑删除自定义编码规则'
  UNION ALL SELECT '编码规则状态接口', 17, 'system:codeRule:api:status',
         'POST', '/system/code-rule/status', '启停编码规则'
  UNION ALL SELECT '编码规则预览接口', 18, 'system:codeRule:api:preview',
         'POST', '/system/code-rule/preview', '无副作用预览编码规则'
  UNION ALL SELECT '编码规则生成接口', 19, 'system:codeRule:api:generate',
         'POST', '/system/code-rule/generate', '生成真实业务编码'
  UNION ALL SELECT '编码规则能力接口', 20, 'system:codeRule:api:capabilities',
         'GET', '/system/code-rule/capabilities', '查询结构化规则可执行能力'
) seed
WHERE @code_rule_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_resource resource
    WHERE resource.tenant_id = 1
      AND resource.resource_type = 4
      AND resource.perms = seed.perms
      AND resource.del_flag = 0
  );

INSERT INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT DISTINCT 1, menu_role.role_id, button.id, NOW()
FROM sys_role_resource menu_role
INNER JOIN sys_resource button
 ON button.tenant_id = 1
 AND button.parent_id = @code_rule_menu_id
 AND button.resource_type IN (3, 4)
 AND button.perms IN (
   'system:codeRule:add', 'system:codeRule:edit', 'system:codeRule:remove', 'system:codeRule:use',
   'system:codeRule:api:page', 'system:codeRule:api:list', 'system:codeRule:api:detail',
   'system:codeRule:api:add', 'system:codeRule:api:edit', 'system:codeRule:api:remove',
   'system:codeRule:api:status', 'system:codeRule:api:preview', 'system:codeRule:api:generate',
   'system:codeRule:api:capabilities'
 )
 AND button.del_flag = 0
WHERE menu_role.tenant_id = 1
  AND menu_role.resource_id = @code_rule_menu_id
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_resource existing
    WHERE existing.tenant_id = 1
      AND existing.role_id = menu_role.role_id
      AND existing.resource_id = button.id
  );
