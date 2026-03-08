-- ----------------------------
-- 代码生成器数据源配置表
-- ----------------------------
DROP TABLE IF EXISTS `gen_datasource`;
CREATE TABLE `gen_datasource` (
  `datasource_id` bigint NOT NULL AUTO_INCREMENT COMMENT '数据源ID',
  `datasource_name` varchar(100) NOT NULL COMMENT '数据源名称',
  `datasource_code` varchar(50) NOT NULL COMMENT '数据源编码（唯一标识）',
  `db_type` varchar(20) NOT NULL COMMENT '数据库类型：MySQL/Oracle/PostgreSQL/SQLServer',
  `driver_class_name` varchar(200) NOT NULL COMMENT '驱动类名',
  `url` varchar(500) NOT NULL COMMENT 'JDBC连接地址',
  `username` varchar(100) NOT NULL COMMENT '用户名',
  `password` varchar(200) NOT NULL COMMENT '密码（加密存储）',
  `is_default` tinyint(1) DEFAULT '0' COMMENT '是否默认数据源（1-是，0-否）',
  `is_enabled` tinyint(1) DEFAULT '1' COMMENT '是否启用（1-启用，0-禁用）',
  `test_query` varchar(200) DEFAULT 'SELECT 1' COMMENT '测试查询SQL',
  `sort` int DEFAULT '0' COMMENT '排序',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  PRIMARY KEY (`datasource_id`),
  UNIQUE KEY `uk_datasource_code` (`datasource_code`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='代码生成器数据源配置表';

-- ----------------------------
-- 插入默认数据源（当前数据库）
-- ----------------------------
INSERT INTO `gen_datasource` VALUES (
  1,
  '默认数据源',
  'default',
  'MySQL',
  'com.mysql.cj.jdbc.Driver',
  'jdbc:mysql://120.48.96.178:3306/forge_admin_new?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=true&serverTimezone=GMT%2B8&autoReconnect=true&rewriteBatchedStatements=true&allowPublicKeyRetrieval=true&nullCatalogMeansCurrent=true',
  'rdsroot',
  '!Ymd199606',
  1,
  1,
  'SELECT 1',
  0,
  '系统默认数据源',
  NOW(),
  NOW(),
  'admin',
  'admin'
);
