-- 外围接口调试、调用日志、接口文档升级脚本
-- 适用于已初始化数据库；新库可直接执行 external_proxy_tables.sql 或初始化脚本。

SET @column_length := (
  SELECT CHARACTER_MAXIMUM_LENGTH
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'sys_external_api'
    AND column_name = 'request_content_type'
);
SET @ddl := IF(@column_length IS NOT NULL AND @column_length < 50,
  'ALTER TABLE sys_external_api MODIFY COLUMN request_content_type VARCHAR(50) DEFAULT NULL COMMENT ''请求Content-Type''',
  'SELECT ''sys_external_api.request_content_type length ok'''
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_length := (
  SELECT CHARACTER_MAXIMUM_LENGTH
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'sys_external_api'
    AND column_name = 'response_content_type'
);
SET @ddl := IF(@column_length IS NOT NULL AND @column_length < 50,
  'ALTER TABLE sys_external_api MODIFY COLUMN response_content_type VARCHAR(50) DEFAULT NULL COMMENT ''响应Content-Type''',
  'SELECT ''sys_external_api.response_content_type length ok'''
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'sys_external_api'
    AND column_name = 'doc_file_id'
);
SET @ddl := IF(@column_exists = 0,
  'ALTER TABLE sys_external_api ADD COLUMN doc_file_id VARCHAR(100) DEFAULT NULL COMMENT ''接口文档文件ID'' AFTER success_codes',
  'SELECT ''sys_external_api.doc_file_id exists'''
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'sys_external_api'
    AND column_name = 'doc_file_name'
);
SET @ddl := IF(@column_exists = 0,
  'ALTER TABLE sys_external_api ADD COLUMN doc_file_name VARCHAR(255) DEFAULT NULL COMMENT ''接口文档文件名'' AFTER doc_file_id',
  'SELECT ''sys_external_api.doc_file_name exists'''
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'sys_external_system'
    AND column_name = 'trusted_internal'
);
SET @ddl := IF(@column_exists = 0,
  'ALTER TABLE sys_external_system ADD COLUMN trusted_internal TINYINT(1) DEFAULT 0 COMMENT ''是否可信内部Forge系统'' AFTER custom_auth_config',
  'SELECT ''sys_external_system.trusted_internal exists'''
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS sys_external_api_log (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户编号',
  system_id BIGINT DEFAULT NULL COMMENT '外部系统ID',
  api_id BIGINT NOT NULL COMMENT '外部接口ID',
  system_name VARCHAR(100) DEFAULT NULL COMMENT '系统名称快照',
  api_name VARCHAR(100) DEFAULT NULL COMMENT '接口名称快照',
  api_code VARCHAR(50) DEFAULT NULL COMMENT '接口编码快照',
  request_method VARCHAR(10) DEFAULT NULL COMMENT '请求方法',
  request_url VARCHAR(1000) DEFAULT NULL COMMENT '请求URL',
  request_params TEXT DEFAULT NULL COMMENT '请求参数',
  request_body TEXT DEFAULT NULL COMMENT '请求体',
  response_body TEXT DEFAULT NULL COMMENT '响应体',
  http_status_code INT DEFAULT NULL COMMENT 'HTTP状态码',
  call_status TINYINT DEFAULT 1 COMMENT '调用状态（0失败 1成功）',
  error_message VARCHAR(1000) DEFAULT NULL COMMENT '错误信息',
  duration_ms BIGINT DEFAULT NULL COMMENT '耗时毫秒',
  debug_flag TINYINT(1) DEFAULT 0 COMMENT '是否调试调用',
  create_by BIGINT DEFAULT NULL COMMENT '创建人ID',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  create_dept BIGINT DEFAULT NULL COMMENT '创建部门ID',
  update_by BIGINT DEFAULT NULL COMMENT '更新人ID',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_tenant_api (tenant_id, api_id),
  KEY idx_system_id (system_id),
  KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='外部接口调用日志表';

INSERT IGNORE INTO sys_dict_type (tenant_id, dict_name, dict_type, dict_status, remark, create_time, update_time)
VALUES (1, '外部认证适配器', 'external_auth_adapter', 1, '外部系统自定义认证适配器字典', NOW(), NOW());

INSERT IGNORE INTO sys_dict_data (tenant_id, dict_sort, dict_label, dict_value, dict_type, dict_status, remark, create_time, update_time)
VALUES (1, 1, '请求头映射', 'header_map', 'external_auth_adapter', 1, '将自定义认证配置中的 KEY:VALUE 写入请求头', NOW(), NOW());
