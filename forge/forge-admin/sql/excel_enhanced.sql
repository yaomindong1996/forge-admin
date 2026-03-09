-- Excel 导入导出增强功能 - 数据库表结构

-- 1. 异步导出任务表
CREATE TABLE IF NOT EXISTS `sys_excel_export_task` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务 ID',
    `task_id` varchar(64) NOT NULL COMMENT '任务 UUID',
    `tenant_id` bigint NOT NULL DEFAULT '000000' COMMENT '租户编号',
    `config_key` varchar(100) NOT NULL COMMENT '配置键',
    `file_name` varchar(255) DEFAULT NULL COMMENT '文件名',
    `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态（0-处理中，1-完成，2-失败）',
    `file_path` varchar(500) DEFAULT NULL COMMENT '文件路径',
    `file_size` bigint DEFAULT 0 COMMENT '文件大小（字节）',
    `data_count` int DEFAULT 0 COMMENT '数据条数',
    `error_message` varchar(1000) DEFAULT NULL COMMENT '错误信息',
    `create_by` bigint DEFAULT NULL COMMENT '创建人 ID',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `finish_time` datetime DEFAULT NULL COMMENT '完成时间',
    `expire_time` datetime DEFAULT NULL COMMENT '过期时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_task_id` (`task_id`),
    KEY `idx_create_by` (`create_by`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_expire_time` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='异步导出任务表';

-- 2. Excel 导入日志表
CREATE TABLE IF NOT EXISTS `sys_excel_import_log` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志 ID',
    `task_id` varchar(64) NOT NULL COMMENT '任务 UUID',
    `tenant_id` bigint NOT NULL DEFAULT '000000' COMMENT '租户编号',
    `config_key` varchar(100) NOT NULL COMMENT '配置键',
    `file_name` varchar(255) DEFAULT NULL COMMENT '原文件名',
    `total_rows` int DEFAULT 0 COMMENT '总行数',
    `success_rows` int DEFAULT 0 COMMENT '成功行数',
    `failed_rows` int DEFAULT 0 COMMENT '失败行数',
    `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态（0-处理中，1-完成，2-失败）',
    `error_report_path` varchar(500) DEFAULT NULL COMMENT '错误报告路径',
    `create_by` bigint DEFAULT NULL COMMENT '创建人 ID',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `finish_time` datetime DEFAULT NULL COMMENT '完成时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_task_id` (`task_id`),
    KEY `idx_create_by` (`create_by`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Excel 导入日志表';

-- 3. Excel 导入错误详情表
CREATE TABLE IF NOT EXISTS `sys_excel_import_error` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '错误 ID',
    `import_log_id` bigint NOT NULL COMMENT '导入日志 ID',
    `tenant_id` bigint NOT NULL DEFAULT '000000' COMMENT '租户编号',
    `row_num` int NOT NULL COMMENT '行号',
    `column_name` varchar(100) DEFAULT NULL COMMENT '列名',
    `raw_value` text DEFAULT NULL COMMENT '原始值',
    `error_type` varchar(50) DEFAULT NULL COMMENT '错误类型',
    `error_message` varchar(1000) DEFAULT NULL COMMENT '错误信息',
    `suggestion` varchar(500) DEFAULT NULL COMMENT '建议修正',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_import_log_id` (`import_log_id`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Excel 导入错误详情表';

-- 4. 更新 sys_excel_export_config 表（添加导入相关字段）
ALTER TABLE `sys_excel_export_config` 
ADD COLUMN `include_sample` tinyint DEFAULT 0 COMMENT '是否包含示例数据（0-否，1-是）',
ADD COLUMN `allow_import` tinyint DEFAULT 0 COMMENT '是否允许导入（0-否，1-是）';

-- 5. 更新 sys_excel_column_config 表（添加导入相关字段）
ALTER TABLE `sys_excel_column_config`
ADD COLUMN `importable` tinyint DEFAULT 0 COMMENT '是否可导入（0-否，1-是）',
ADD COLUMN `required` tinyint DEFAULT 0 COMMENT '是否必填（0-否，1-是）',
ADD COLUMN `example_value` varchar(255) DEFAULT NULL COMMENT '示例值',
ADD COLUMN `validation_rule` varchar(500) DEFAULT NULL COMMENT '校验规则（正则）',
ADD COLUMN `validation_message` varchar(500) DEFAULT NULL COMMENT '校验失败提示';
