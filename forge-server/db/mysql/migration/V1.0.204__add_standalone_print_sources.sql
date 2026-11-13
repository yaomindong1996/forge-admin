-- 平台级打印业务来源。保留既有应用来源，新增独立来源引用和绑定版本。
-- 回滚：停用独立打印中心入口并保留来源/模板/审计数据；禁止自动 DROP。

CREATE TABLE IF NOT EXISTS sys_print_business_source (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    source_code VARCHAR(80) NOT NULL COMMENT '租户内稳定业务来源编码',
    source_name VARCHAR(100) NOT NULL COMMENT '业务来源名称',
    source_type VARCHAR(20) NOT NULL COMMENT 'SERVICE/DATASET/API',
    provider_code VARCHAR(100) NULL COMMENT '服务端注册的 Provider 编码',
    dataset_id BIGINT NULL COMMENT '已发布数据集 ID',
    object_code VARCHAR(100) NOT NULL COMMENT '标准业务对象编码',
    parameter_schema_json MEDIUMTEXT NULL COMMENT '受控调用参数协议',
    mapping_json MEDIUMTEXT NULL COMMENT '数据集或接口结果到打印上下文的映射',
    catalog_json MEDIUMTEXT NULL COMMENT '发布时字段目录快照',
    catalog_hash CHAR(64) NULL COMMENT '字段目录 SHA-256',
    source_revision BIGINT NOT NULL DEFAULT 1 COMMENT '来源并发修订号',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '启停；新来源默认停用',
    del_flag BIGINT NOT NULL DEFAULT 0 COMMENT '删除墓碑：有效为 0，删除写主键',
    create_by BIGINT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_dept BIGINT NULL,
    update_by BIGINT NOT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_print_business_source_code (tenant_id, source_code, del_flag),
    KEY idx_print_business_source_type (tenant_id, source_type, status, del_flag),
    KEY idx_print_business_source_dataset (tenant_id, dataset_id, status, del_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

SET @print_source_ddl := IF(EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_print_template'
      AND COLUMN_NAME = 'business_source_id'
), 'SELECT 1', 'ALTER TABLE sys_print_template ADD COLUMN business_source_id BIGINT NULL COMMENT ''独立业务来源 ID'' AFTER application_id');
PREPARE print_source_stmt FROM @print_source_ddl;
EXECUTE print_source_stmt;
DEALLOCATE PREPARE print_source_stmt;

SET @print_source_ddl := IF(EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_print_template'
      AND COLUMN_NAME = 'source_code'
), 'SELECT 1', 'ALTER TABLE sys_print_template ADD COLUMN source_code VARCHAR(80) NULL COMMENT ''独立业务来源编码'' AFTER business_source_id');
PREPARE print_source_stmt FROM @print_source_ddl;
EXECUTE print_source_stmt;
DEALLOCATE PREPARE print_source_stmt;

INSERT INTO sys_dict_data (
    tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default,
    dict_status, remark, create_by, create_time, update_by, update_time, create_dept
)
SELECT 1, 3, '业务服务', 'SERVICE', 'sys_print_source_type', NULL, 'default', 'N',
       1, '独立打印中心', 1, NOW(), 1, NOW(), 1
WHERE NOT EXISTS (
    SELECT 1 FROM sys_dict_data
    WHERE tenant_id = 1 AND dict_type = 'sys_print_source_type' AND dict_value = 'SERVICE'
);

INSERT INTO sys_dict_data (
    tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default,
    dict_status, remark, create_by, create_time, update_by, update_time, create_dept
)
SELECT 1, 4, '数据集', 'DATASET', 'sys_print_source_type', NULL, 'default', 'N',
       1, '独立打印中心', 1, NOW(), 1, NOW(), 1
WHERE NOT EXISTS (
    SELECT 1 FROM sys_dict_data
    WHERE tenant_id = 1 AND dict_type = 'sys_print_source_type' AND dict_value = 'DATASET'
);

INSERT INTO sys_dict_data (
    tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default,
    dict_status, remark, create_by, create_time, update_by, update_time, create_dept
)
SELECT 1, 5, '受管接口', 'API', 'sys_print_source_type', NULL, 'default', 'N',
       0, '协议保留，接入受管连接后开放', 1, NOW(), 1, NOW(), 1
WHERE NOT EXISTS (
    SELECT 1 FROM sys_dict_data
    WHERE tenant_id = 1 AND dict_type = 'sys_print_source_type' AND dict_value = 'API'
);

UPDATE sys_resource
SET resource_name = '打印中心', visible = 1, update_by = 1, update_time = NOW()
WHERE tenant_id = 1 AND path = '/print' AND resource_type = 2 AND del_flag = 0;

SET @print_center_resource_id = (
    SELECT id FROM sys_resource
    WHERE tenant_id = 1 AND path = '/print' AND resource_type = 2 AND del_flag = 0
    ORDER BY id LIMIT 1
);

INSERT INTO sys_resource (
    tenant_id, resource_name, parent_id, resource_type, sort, is_external, open_target, is_public,
    menu_status, visible, perms, keep_alive, always_show, remark, create_by, create_time, update_by,
    update_time, create_dept, client_code
)
SELECT 1, '查看打印来源', @print_center_resource_id, 3, 1, 0, '_self', 0,
       1, 1, 'print:source:view', 0, 0, '打印来源仍需租户和数据权限校验',
       1, NOW(), 1, NOW(), 1, 'pc'
WHERE @print_center_resource_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM sys_resource
      WHERE tenant_id = 1 AND perms = 'print:source:view' AND del_flag = 0
  );

INSERT INTO sys_resource (
    tenant_id, resource_name, parent_id, resource_type, sort, is_external, open_target, is_public,
    menu_status, visible, perms, keep_alive, always_show, remark, create_by, create_time, update_by,
    update_time, create_dept, client_code
)
SELECT 1, '管理打印来源', @print_center_resource_id, 3, 2, 0, '_self', 0,
       1, 1, 'print:source:manage', 0, 0, '不能配置任意 SQL、URL 或 Bean 名',
       1, NOW(), 1, NOW(), 1, 'pc'
WHERE @print_center_resource_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM sys_resource
      WHERE tenant_id = 1 AND perms = 'print:source:manage' AND del_flag = 0
  );

SET @print_source_ddl := IF(EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_print_binding'
      AND COLUMN_NAME = 'business_source_id'
), 'SELECT 1', 'ALTER TABLE sys_print_binding ADD COLUMN business_source_id BIGINT NULL COMMENT ''独立业务来源 ID'' AFTER application_id');
PREPARE print_source_stmt FROM @print_source_ddl;
EXECUTE print_source_stmt;
DEALLOCATE PREPARE print_source_stmt;

SET @print_source_ddl := IF(EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_print_binding'
      AND COLUMN_NAME = 'source_code'
), 'SELECT 1', 'ALTER TABLE sys_print_binding ADD COLUMN source_code VARCHAR(80) NULL COMMENT ''独立业务来源编码'' AFTER business_source_id');
PREPARE print_source_stmt FROM @print_source_ddl;
EXECUTE print_source_stmt;
DEALLOCATE PREPARE print_source_stmt;

SET @print_source_ddl := IF(EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_print_binding'
      AND COLUMN_NAME = 'template_version_id'
), 'SELECT 1', 'ALTER TABLE sys_print_binding ADD COLUMN template_version_id BIGINT NULL COMMENT ''独立来源固定模板版本'' AFTER template_id');
PREPARE print_source_stmt FROM @print_source_ddl;
EXECUTE print_source_stmt;
DEALLOCATE PREPARE print_source_stmt;

SET @print_source_ddl := IF(EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_print_execution'
      AND COLUMN_NAME = 'business_source_id'
), 'SELECT 1', 'ALTER TABLE sys_print_execution ADD COLUMN business_source_id BIGINT NULL COMMENT ''独立业务来源 ID'' AFTER application_id');
PREPARE print_source_stmt FROM @print_source_ddl;
EXECUTE print_source_stmt;
DEALLOCATE PREPARE print_source_stmt;

SET @print_source_ddl := IF(EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_print_execution'
      AND COLUMN_NAME = 'source_code'
), 'SELECT 1', 'ALTER TABLE sys_print_execution ADD COLUMN source_code VARCHAR(80) NULL COMMENT ''独立业务来源编码'' AFTER business_source_id');
PREPARE print_source_stmt FROM @print_source_ddl;
EXECUTE print_source_stmt;
DEALLOCATE PREPARE print_source_stmt;

SET @print_source_ddl := IF(EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_print_execution'
      AND COLUMN_NAME = 'source_revision'
), 'SELECT 1', 'ALTER TABLE sys_print_execution ADD COLUMN source_revision BIGINT NULL COMMENT ''实际使用的来源修订号'' AFTER business_source_id');
PREPARE print_source_stmt FROM @print_source_ddl;
EXECUTE print_source_stmt;
DEALLOCATE PREPARE print_source_stmt;

SET @print_source_ddl := IF(EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_print_template'
      AND COLUMN_NAME = 'application_id' AND IS_NULLABLE = 'NO'
), 'ALTER TABLE sys_print_template MODIFY COLUMN application_id BIGINT NULL COMMENT ''低代码应用 ID；独立来源为空''', 'SELECT 1');
PREPARE print_source_stmt FROM @print_source_ddl;
EXECUTE print_source_stmt;
DEALLOCATE PREPARE print_source_stmt;

SET @print_source_ddl := IF(EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_print_binding'
      AND COLUMN_NAME = 'application_id' AND IS_NULLABLE = 'NO'
), 'ALTER TABLE sys_print_binding MODIFY COLUMN application_id BIGINT NULL COMMENT ''低代码应用 ID；独立来源为空''', 'SELECT 1');
PREPARE print_source_stmt FROM @print_source_ddl;
EXECUTE print_source_stmt;
DEALLOCATE PREPARE print_source_stmt;

SET @print_source_ddl := IF(EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_print_execution'
      AND COLUMN_NAME = 'application_id' AND IS_NULLABLE = 'NO'
), 'ALTER TABLE sys_print_execution MODIFY COLUMN application_id BIGINT NULL COMMENT ''低代码应用 ID；独立来源为空''', 'SELECT 1');
PREPARE print_source_stmt FROM @print_source_ddl;
EXECUTE print_source_stmt;
DEALLOCATE PREPARE print_source_stmt;

SET @print_source_ddl := IF(EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_print_template'
      AND INDEX_NAME = 'uk_print_template_code'
), 'ALTER TABLE sys_print_template DROP INDEX uk_print_template_code', 'SELECT 1');
PREPARE print_source_stmt FROM @print_source_ddl;
EXECUTE print_source_stmt;
DEALLOCATE PREPARE print_source_stmt;

SET @print_source_ddl := IF(EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_print_template'
      AND INDEX_NAME = 'uk_print_template_source_code'
), 'SELECT 1', 'ALTER TABLE sys_print_template ADD UNIQUE KEY uk_print_template_source_code (tenant_id, source_key, template_code, del_flag)');
PREPARE print_source_stmt FROM @print_source_ddl;
EXECUTE print_source_stmt;
DEALLOCATE PREPARE print_source_stmt;

SET @print_source_ddl := IF(EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_print_binding'
      AND INDEX_NAME = 'uk_print_binding_source'
), 'ALTER TABLE sys_print_binding DROP INDEX uk_print_binding_source', 'SELECT 1');
PREPARE print_source_stmt FROM @print_source_ddl;
EXECUTE print_source_stmt;
DEALLOCATE PREPARE print_source_stmt;

SET @print_source_ddl := IF(EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_print_binding'
      AND INDEX_NAME = 'uk_print_binding_source_template'
), 'SELECT 1', 'ALTER TABLE sys_print_binding ADD UNIQUE KEY uk_print_binding_source_template (tenant_id, source_key, template_id, scene, del_flag)');
PREPARE print_source_stmt FROM @print_source_ddl;
EXECUTE print_source_stmt;
DEALLOCATE PREPARE print_source_stmt;

SET @print_source_ddl := IF(EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_print_template'
      AND INDEX_NAME = 'idx_print_template_business_source'
), 'SELECT 1', 'ALTER TABLE sys_print_template ADD KEY idx_print_template_business_source (tenant_id, business_source_id, status, del_flag)');
PREPARE print_source_stmt FROM @print_source_ddl;
EXECUTE print_source_stmt;
DEALLOCATE PREPARE print_source_stmt;

SET @print_source_ddl := IF(EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_print_binding'
      AND INDEX_NAME = 'idx_print_binding_business_source'
), 'SELECT 1', 'ALTER TABLE sys_print_binding ADD KEY idx_print_binding_business_source (tenant_id, business_source_id, scene, status, del_flag)');
PREPARE print_source_stmt FROM @print_source_ddl;
EXECUTE print_source_stmt;
DEALLOCATE PREPARE print_source_stmt;
