-- 代码生成器模块数据库表结构

-- 数据源配置表
CREATE TABLE `gen_datasource` (
    `datasource_id` bigint NOT NULL AUTO_INCREMENT COMMENT '数据源ID',
    `datasource_name` varchar(100) NOT NULL COMMENT '数据源名称',
    `datasource_code` varchar(50) NOT NULL COMMENT '数据源编码（唯一标识）',
    `db_type` varchar(20) NOT NULL DEFAULT 'MySQL' COMMENT '数据库类型：MySQL/Oracle/PostgreSQL/SQLServer',
    `driver_class_name` varchar(200) NOT NULL COMMENT '驱动类名',
    `url` varchar(500) NOT NULL COMMENT 'JDBC连接地址',
    `username` varchar(100) NOT NULL COMMENT '用户名',
    `password` varchar(200) NOT NULL COMMENT '密码（加密存储）',
    `is_default` tinyint DEFAULT 0 COMMENT '是否默认数据源（1-是，0-否）',
    `is_enabled` tinyint DEFAULT 1 COMMENT '是否启用（1-启用，0-禁用）',
    `test_query` varchar(200) DEFAULT 'SELECT 1' COMMENT '测试查询SQL',
    `sort` int DEFAULT 0 COMMENT '排序',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
    `update_by` varchar(64) DEFAULT NULL COMMENT '更新者',
    PRIMARY KEY (`datasource_id`),
    UNIQUE KEY `uk_datasource_code` (`datasource_code`),
    KEY `idx_is_default` (`is_default`),
    KEY `idx_is_enabled` (`is_enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代码生成器数据源配置';

-- 插入默认数据源（当前应用数据源）
INSERT INTO `gen_datasource` (`datasource_name`, `datasource_code`, `db_type`, `driver_class_name`, `url`, `username`, `password`, `is_default`, `is_enabled`, `remark`) VALUES
('默认数据源', 'default', 'MySQL', 'com.mysql.cj.jdbc.Driver', 'jdbc:mysql://localhost:3306/forge?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai', 'root', 'root', 1, 1, '当前应用默认数据源');

-- 代码生成表配置
CREATE TABLE `gen_table` (
    `table_id` bigint NOT NULL AUTO_INCREMENT COMMENT '表ID',
    `datasource_id` bigint DEFAULT NULL COMMENT '数据源ID',
    `table_name` varchar(200) NOT NULL COMMENT '表名称',
    `table_comment` varchar(500) DEFAULT NULL COMMENT '表描述',
    `class_name` varchar(100) NOT NULL COMMENT '实体类名称',
    `business_name` varchar(100) DEFAULT NULL COMMENT '业务名称',
    `function_name` varchar(100) DEFAULT NULL COMMENT '功能名称',
    `module_name` varchar(100) DEFAULT 'system' COMMENT '模块名称',
    `package_name` varchar(200) DEFAULT 'com.mdframe.forge.plugin' COMMENT '包路径',
    `author` varchar(50) DEFAULT 'Forge Generator' COMMENT '作者',
    `gen_type` varchar(20) DEFAULT 'DOWNLOAD' COMMENT '生成方式：DOWNLOAD-下载代码包/PROJECT-直接生成到项目',
    `gen_path` varchar(500) DEFAULT '/' COMMENT '生成路径',
    `template_engine` varchar(20) DEFAULT 'VELOCITY' COMMENT '模板引擎：VELOCITY/FREEMARKER/AI',
    `options` json DEFAULT NULL COMMENT '其他生成选项',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
    `update_by` varchar(64) DEFAULT NULL COMMENT '更新者',
    PRIMARY KEY (`table_id`),
    UNIQUE KEY `uk_datasource_table` (`datasource_id`, `table_name`),
    KEY `idx_datasource_id` (`datasource_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代码生成表配置';

-- 代码生成表字段配置
CREATE TABLE `gen_table_column` (
    `column_id` bigint NOT NULL AUTO_INCREMENT COMMENT '字段ID',
    `table_id` bigint NOT NULL COMMENT '表ID',
    `column_name` varchar(200) NOT NULL COMMENT '字段名称',
    `column_comment` varchar(500) DEFAULT NULL COMMENT '字段描述',
    `column_type` varchar(100) NOT NULL COMMENT '字段类型',
    `java_type` varchar(50) DEFAULT 'String' COMMENT 'Java类型',
    `java_field` varchar(100) DEFAULT NULL COMMENT 'Java字段名',
    `is_pk` tinyint DEFAULT 0 COMMENT '是否主键',
    `is_increment` tinyint DEFAULT 0 COMMENT '是否自增',
    `is_required` tinyint DEFAULT 0 COMMENT '是否必填',
    `is_insert` tinyint DEFAULT 1 COMMENT '是否插入字段',
    `is_edit` tinyint DEFAULT 1 COMMENT '是否编辑字段',
    `is_list` tinyint DEFAULT 1 COMMENT '是否列表字段',
    `is_query` tinyint DEFAULT 0 COMMENT '是否查询字段',
    `query_type` varchar(20) DEFAULT 'EQ' COMMENT '查询方式：EQ/LIKE/BETWEEN/GT/LT等',
    `html_type` varchar(20) DEFAULT 'INPUT' COMMENT '显示类型：INPUT/SELECT/DATETIME/TEXTAREA等',
    `dict_type` varchar(100) DEFAULT NULL COMMENT '字典类型',
    `sort` int DEFAULT 0 COMMENT '排序',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`column_id`),
    KEY `idx_table_id` (`table_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代码生成表字段配置';

-- 代码生成模板配置
CREATE TABLE `gen_template` (
    `template_id` bigint NOT NULL AUTO_INCREMENT COMMENT '模板ID',
    `template_name` varchar(100) NOT NULL COMMENT '模板名称',
    `template_code` varchar(50) NOT NULL COMMENT '模板编码',
    `template_type` varchar(20) NOT NULL COMMENT '模板类型：ENTITY/MAPPER/SERVICE/CONTROLLER/DTO/VO/SQL',
    `template_engine` varchar(20) DEFAULT 'VELOCITY' COMMENT '模板引擎',
    `template_content` longtext NOT NULL COMMENT '模板内容',
    `file_suffix` varchar(20) DEFAULT '.java' COMMENT '生成文件后缀',
    `file_path` varchar(200) DEFAULT NULL COMMENT '生成文件路径（相对路径）',
    `is_system` tinyint DEFAULT 1 COMMENT '是否系统内置',
    `is_enabled` tinyint DEFAULT 1 COMMENT '是否启用',
    `sort` int DEFAULT 0 COMMENT '排序',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`template_id`),
    UNIQUE KEY `uk_template_code` (`template_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代码生成模板配置';
