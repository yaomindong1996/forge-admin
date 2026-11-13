-- 企业协同连接底座：升级 sys_social_config/sys_user_social 为连接维度，新建应用、能力绑定、映射、同步、待办与回调表。
-- 约束：只增可空字段和新表；Secret 明文迁移由应用层（Task 4C）完成，本脚本不接触任何明文凭据。

-- ============================================================
-- 1. sys_social_config：升级为企业协同连接根
-- ============================================================

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_social_config' AND COLUMN_NAME = 'connection_code');
SET @sql = IF(@col = 0, 'ALTER TABLE sys_social_config ADD COLUMN connection_code varchar(64) DEFAULT NULL COMMENT ''连接编码（租户内唯一）'' AFTER platform_logo', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_social_config' AND COLUMN_NAME = 'connection_name');
SET @sql = IF(@col = 0, 'ALTER TABLE sys_social_config ADD COLUMN connection_name varchar(100) DEFAULT NULL COMMENT ''连接名称'' AFTER connection_code', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_social_config' AND COLUMN_NAME = 'enterprise_id');
SET @sql = IF(@col = 0, 'ALTER TABLE sys_social_config ADD COLUMN enterprise_id varchar(100) DEFAULT NULL COMMENT ''外部企业ID（企业微信CorpId等）'' AFTER connection_name', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_social_config' AND COLUMN_NAME = 'connection_type');
SET @sql = IF(@col = 0, 'ALTER TABLE sys_social_config ADD COLUMN connection_type varchar(32) DEFAULT NULL COMMENT ''连接类型：CORP_INTERNAL自建应用/THIRD_PARTY第三方/OAUTH_ONLY仅登录'' AFTER enterprise_id', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_social_config' AND COLUMN_NAME = 'identity_policy');
SET @sql = IF(@col = 0, 'ALTER TABLE sys_social_config ADD COLUMN identity_policy varchar(32) DEFAULT NULL COMMENT ''身份匹配策略：BIND_ONLY仅绑定已有/AUTO_CREATE自动创建/MANUAL人工处理'' AFTER connection_type', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_social_config' AND COLUMN_NAME = 'directory_authority');
SET @sql = IF(@col = 0, 'ALTER TABLE sys_social_config ADD COLUMN directory_authority varchar(32) DEFAULT NULL COMMENT ''目录权威来源：EXTERNAL外部权威/LOCAL本地权威/NONE不同步'' AFTER identity_policy', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_social_config' AND COLUMN_NAME = 'default_org_id');
SET @sql = IF(@col = 0, 'ALTER TABLE sys_social_config ADD COLUMN default_org_id bigint DEFAULT NULL COMMENT ''目录同步默认挂载的根组织ID'' AFTER directory_authority', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_social_config' AND COLUMN_NAME = 'create_dept');
SET @sql = IF(@col = 0, 'ALTER TABLE sys_social_config ADD COLUMN create_dept bigint unsigned DEFAULT NULL COMMENT ''创建组织ID'' AFTER update_time', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_social_config' AND COLUMN_NAME = 'del_flag');
SET @sql = IF(@col = 0, 'ALTER TABLE sys_social_config ADD COLUMN del_flag bigint NOT NULL DEFAULT 0 COMMENT ''逻辑删除标记：0正常，删除后写主键'' AFTER create_dept', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 存量行回填确定性连接编码，不猜测企业归属（enterprise_id 留空由管理员补录）。
UPDATE sys_social_config
SET connection_code = LOWER(CONCAT(platform, '-', id))
WHERE connection_code IS NULL;

UPDATE sys_social_config
SET connection_name = COALESCE(platform_name, platform)
WHERE connection_name IS NULL;

-- 旧唯一键 (platform, tenant_id) 阻塞同租户多连接，替换为连接维度活动唯一键。
SET @idx = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_social_config' AND INDEX_NAME = 'uk_platform_tenant');
SET @sql = IF(@idx > 0, 'ALTER TABLE sys_social_config DROP INDEX uk_platform_tenant', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_social_config' AND INDEX_NAME = 'uk_social_conn_code_active');
SET @sql = IF(@idx = 0, 'ALTER TABLE sys_social_config ADD UNIQUE KEY uk_social_conn_code_active (tenant_id, connection_code, del_flag)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_social_config' AND INDEX_NAME = 'uk_social_conn_enterprise_active');
SET @sql = IF(@idx = 0, 'ALTER TABLE sys_social_config ADD UNIQUE KEY uk_social_conn_enterprise_active (tenant_id, platform, enterprise_id, del_flag)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 保留平台维度普通索引供兼容期查询。
SET @idx = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_social_config' AND INDEX_NAME = 'idx_social_platform');
SET @sql = IF(@idx = 0, 'ALTER TABLE sys_social_config ADD KEY idx_social_platform (platform, tenant_id, del_flag)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================================
-- 2. sys_user_social：外部身份绑定增加连接与企业维度
-- ============================================================

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user_social' AND COLUMN_NAME = 'connection_id');
SET @sql = IF(@col = 0, 'ALTER TABLE sys_user_social ADD COLUMN connection_id bigint DEFAULT NULL COMMENT ''企业协同连接ID（应用层迁移回填，歧义进入阻塞清单）'' AFTER platform', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user_social' AND COLUMN_NAME = 'external_enterprise_id');
SET @sql = IF(@col = 0, 'ALTER TABLE sys_user_social ADD COLUMN external_enterprise_id varchar(100) DEFAULT NULL COMMENT ''外部企业ID（企业微信CorpId等）'' AFTER connection_id', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user_social' AND COLUMN_NAME = 'managed_by_sync');
SET @sql = IF(@col = 0, 'ALTER TABLE sys_user_social ADD COLUMN managed_by_sync tinyint NOT NULL DEFAULT 0 COMMENT ''是否由目录同步管理：0否 1是'' AFTER external_enterprise_id', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user_social' AND COLUMN_NAME = 'external_status');
SET @sql = IF(@col = 0, 'ALTER TABLE sys_user_social ADD COLUMN external_status varchar(32) DEFAULT NULL COMMENT ''外部账号状态：ACTIVE/DISABLED/DELETED'' AFTER managed_by_sync', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user_social' AND COLUMN_NAME = 'source_hash');
SET @sql = IF(@col = 0, 'ALTER TABLE sys_user_social ADD COLUMN source_hash varchar(64) DEFAULT NULL COMMENT ''外部资料快照哈希（用于变更检测）'' AFTER external_status', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user_social' AND COLUMN_NAME = 'last_sync_time');
SET @sql = IF(@col = 0, 'ALTER TABLE sys_user_social ADD COLUMN last_sync_time datetime DEFAULT NULL COMMENT ''最近同步时间'' AFTER source_hash', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user_social' AND COLUMN_NAME = 'del_flag');
SET @sql = IF(@col = 0, 'ALTER TABLE sys_user_social ADD COLUMN del_flag bigint NOT NULL DEFAULT 0 COMMENT ''逻辑删除标记：0正常，删除后写主键'' AFTER tenant_id', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 旧唯一键 (platform, uuid) 在多企业下冲突，替换为租户+连接维度活动唯一键；
-- connection_id 未回填的存量行为 NULL，不受新唯一键约束，由 Task 4C 应用层迁移归属。
SET @idx = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user_social' AND INDEX_NAME = 'uk_platform_uuid');
SET @sql = IF(@idx > 0, 'ALTER TABLE sys_user_social DROP INDEX uk_platform_uuid', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user_social' AND INDEX_NAME = 'uk_user_social_conn_uuid_active');
SET @sql = IF(@idx = 0, 'ALTER TABLE sys_user_social ADD UNIQUE KEY uk_user_social_conn_uuid_active (tenant_id, connection_id, uuid, del_flag)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user_social' AND INDEX_NAME = 'uk_user_social_conn_user_active');
SET @sql = IF(@idx = 0, 'ALTER TABLE sys_user_social ADD UNIQUE KEY uk_user_social_conn_user_active (tenant_id, connection_id, user_id, del_flag)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user_social' AND INDEX_NAME = 'idx_user_social_platform_uuid');
SET @sql = IF(@idx = 0, 'ALTER TABLE sys_user_social ADD KEY idx_user_social_platform_uuid (platform, uuid)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================================
-- 3. 新表：连接下的物理应用与能力绑定
-- ============================================================

CREATE TABLE IF NOT EXISTS `sys_social_app_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户编号',
  `connection_id` bigint NOT NULL COMMENT '企业协同连接ID',
  `app_code` varchar(64) NOT NULL COMMENT '应用编码（连接内唯一）',
  `app_name` varchar(100) DEFAULT NULL COMMENT '应用名称',
  `client_id` varchar(255) DEFAULT NULL COMMENT '应用ID/Key（企业微信为AgentId所属应用的CorpId沿用连接）',
  `agent_id` varchar(100) DEFAULT NULL COMMENT '企业微信AgentId',
  `secret_mode` varchar(32) NOT NULL DEFAULT 'CIPHER' COMMENT 'Secret存储模式：CIPHER密文/EXTERNAL_REF外部引用',
  `secret_cipher` varchar(1000) DEFAULT NULL COMMENT '应用Secret密文（FPC1版本化密文，禁止明文）',
  `secret_ref` varchar(500) DEFAULT NULL COMMENT '外部Secret引用（extref:前缀）',
  `secret_update_time` datetime DEFAULT NULL COMMENT 'Secret最近轮换时间',
  `callback_token_cipher` varchar(1000) DEFAULT NULL COMMENT '回调Token密文',
  `encoding_aes_key_cipher` varchar(1000) DEFAULT NULL COMMENT '回调EncodingAESKey密文',
  `redirect_uri` varchar(500) DEFAULT NULL COMMENT 'OAuth回调地址',
  `scope` varchar(500) DEFAULT NULL COMMENT '授权范围',
  `config_json` json DEFAULT NULL COMMENT '应用级扩展配置JSON',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0停用 1启用',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint unsigned DEFAULT NULL COMMENT '创建组织ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` bigint NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_social_app_active` (`tenant_id`, `connection_id`, `app_code`, `del_flag`),
  KEY `idx_social_app_connection` (`connection_id`, `status`, `del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业协同物理应用配置表';

CREATE TABLE IF NOT EXISTS `sys_social_capability_binding` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户编号',
  `connection_id` bigint NOT NULL COMMENT '企业协同连接ID',
  `capability` varchar(32) NOT NULL COMMENT '业务能力：LOGIN/DIRECTORY/MESSAGE/TODO',
  `app_config_id` bigint NOT NULL COMMENT '绑定的物理应用ID',
  `config_json` json DEFAULT NULL COMMENT '能力级扩展配置JSON',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0停用 1启用',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint unsigned DEFAULT NULL COMMENT '创建组织ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` bigint NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_social_capability_active` (`tenant_id`, `connection_id`, `capability`, `del_flag`),
  KEY `idx_social_capability_app` (`app_config_id`, `del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业协同能力绑定表';

-- ============================================================
-- 4. 新表：目录映射（部门/岗位/标签）
-- ============================================================

CREATE TABLE IF NOT EXISTS `sys_social_org_mapping` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户编号',
  `connection_id` bigint NOT NULL COMMENT '企业协同连接ID',
  `external_dept_id` varchar(100) NOT NULL COMMENT '外部部门ID',
  `external_parent_id` varchar(100) DEFAULT NULL COMMENT '外部父部门ID',
  `external_dept_name` varchar(200) DEFAULT NULL COMMENT '外部部门名称',
  `org_id` bigint DEFAULT NULL COMMENT 'Forge组织ID',
  `source_hash` varchar(64) DEFAULT NULL COMMENT '外部快照哈希（用于变更检测）',
  `last_seen_run_id` bigint DEFAULT NULL COMMENT '最近一次出现的同步批次ID',
  `status` varchar(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '映射状态：ACTIVE/INACTIVE/ISSUE',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` bigint NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_social_org_mapping_active` (`tenant_id`, `connection_id`, `external_dept_id`, `del_flag`),
  KEY `idx_social_org_mapping_org` (`tenant_id`, `connection_id`, `org_id`),
  KEY `idx_social_org_mapping_seen` (`tenant_id`, `connection_id`, `last_seen_run_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业协同外部部门映射表';

CREATE TABLE IF NOT EXISTS `sys_social_post_mapping` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户编号',
  `connection_id` bigint NOT NULL COMMENT '企业协同连接ID',
  `external_post_code` varchar(100) NOT NULL COMMENT '外部岗位编码/文本',
  `external_post_name` varchar(200) DEFAULT NULL COMMENT '外部岗位名称',
  `post_id` bigint DEFAULT NULL COMMENT 'Forge岗位ID（企微首期默认不自动创建）',
  `status` varchar(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '映射状态：ACTIVE/INACTIVE/ISSUE',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` bigint NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_social_post_mapping_active` (`tenant_id`, `connection_id`, `external_post_code`, `del_flag`),
  KEY `idx_social_post_mapping_post` (`tenant_id`, `connection_id`, `post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业协同外部岗位映射表';

CREATE TABLE IF NOT EXISTS `sys_social_tag` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户编号',
  `connection_id` bigint NOT NULL COMMENT '企业协同连接ID',
  `external_tag_id` varchar(100) NOT NULL COMMENT '外部标签ID',
  `tag_name` varchar(200) DEFAULT NULL COMMENT '标签名称',
  `status` varchar(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '标签状态：ACTIVE/INACTIVE',
  `source_hash` varchar(64) DEFAULT NULL COMMENT '外部快照哈希（用于变更检测）',
  `last_seen_run_id` bigint DEFAULT NULL COMMENT '最近一次出现的同步批次ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` bigint NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_social_tag_active` (`tenant_id`, `connection_id`, `external_tag_id`, `del_flag`),
  KEY `idx_social_tag_seen` (`tenant_id`, `connection_id`, `last_seen_run_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业协同外部标签表';

-- 标签成员是可重建纯关系表：同步事务内物理替换，不做逻辑删除（原因见变更 Spec 第5节）。
CREATE TABLE IF NOT EXISTS `sys_social_tag_member` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户编号',
  `connection_id` bigint NOT NULL COMMENT '企业协同连接ID',
  `tag_id` bigint NOT NULL COMMENT '本地标签ID（sys_social_tag.id）',
  `member_type` varchar(16) NOT NULL COMMENT '成员类型：USER/DEPT',
  `external_member_id` varchar(100) NOT NULL COMMENT '外部成员ID（userid或部门ID）',
  `local_target_id` bigint DEFAULT NULL COMMENT '映射到的Forge用户/组织ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_social_tag_member` (`tenant_id`, `tag_id`, `member_type`, `external_member_id`),
  KEY `idx_social_tag_member_conn` (`tenant_id`, `connection_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业协同标签成员关系表（可重建）';

-- ============================================================
-- 5. 新表：同步批次日志与问题单
-- ============================================================

-- 运行日志由留存清理任务物理清理，不做逻辑删除。
CREATE TABLE IF NOT EXISTS `sys_social_sync_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '批次ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户编号',
  `connection_id` bigint NOT NULL COMMENT '企业协同连接ID',
  `sync_type` varchar(32) NOT NULL COMMENT '同步类型：FULL/DEPT/USER/TAG/INCREMENT',
  `trigger_source` varchar(32) NOT NULL DEFAULT 'MANUAL' COMMENT '触发来源：MANUAL/JOB/CALLBACK',
  `stage` varchar(32) DEFAULT NULL COMMENT '当前阶段：FETCH/VALIDATE/PLAN/APPLY/FINALIZE',
  `status` varchar(32) NOT NULL DEFAULT 'RUNNING' COMMENT '批次状态：RUNNING/SUCCESS/PARTIAL/FAILED',
  `dept_count` int NOT NULL DEFAULT 0 COMMENT '拉取部门数',
  `user_count` int NOT NULL DEFAULT 0 COMMENT '拉取成员数',
  `tag_count` int NOT NULL DEFAULT 0 COMMENT '拉取标签数',
  `created_count` int NOT NULL DEFAULT 0 COMMENT '创建对象数',
  `updated_count` int NOT NULL DEFAULT 0 COMMENT '更新对象数',
  `inactivated_count` int NOT NULL DEFAULT 0 COMMENT '停用对象数',
  `issue_count` int NOT NULL DEFAULT 0 COMMENT '问题单数',
  `cursor_info` varchar(500) DEFAULT NULL COMMENT '断点游标信息',
  `error_code` varchar(64) DEFAULT NULL COMMENT '错误码',
  `error_summary` varchar(1000) DEFAULT NULL COMMENT '脱敏错误摘要',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `create_by` bigint DEFAULT NULL COMMENT '触发人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_social_sync_log_conn` (`tenant_id`, `connection_id`, `create_time`),
  KEY `idx_social_sync_log_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业协同同步批次日志表';

CREATE TABLE IF NOT EXISTS `sys_social_sync_issue` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户编号',
  `connection_id` bigint NOT NULL COMMENT '企业协同连接ID',
  `sync_log_id` bigint DEFAULT NULL COMMENT '产生问题的同步批次ID',
  `object_type` varchar(32) NOT NULL COMMENT '对象类型：DEPT/USER/POST/TAG',
  `external_id` varchar(100) DEFAULT NULL COMMENT '外部对象ID',
  `issue_code` varchar(64) NOT NULL COMMENT '问题码',
  `issue_summary` varchar(500) DEFAULT NULL COMMENT '脱敏问题摘要（禁止明文手机号/邮箱/姓名）',
  `process_status` varchar(32) NOT NULL DEFAULT 'PENDING' COMMENT '处理状态：PENDING/RESOLVED/IGNORED',
  `process_action` varchar(32) DEFAULT NULL COMMENT '处理动作：BIND/IGNORE/RETRY',
  `process_by` bigint DEFAULT NULL COMMENT '处理人ID',
  `process_time` datetime DEFAULT NULL COMMENT '处理时间',
  `retry_count` int NOT NULL DEFAULT 0 COMMENT '重试次数',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` bigint NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`id`),
  KEY `idx_social_sync_issue_status` (`tenant_id`, `connection_id`, `process_status`, `del_flag`),
  KEY `idx_social_sync_issue_log` (`sync_log_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业协同同步问题单表';

-- ============================================================
-- 6. 新表：待办投影与回调收件箱
-- ============================================================

CREATE TABLE IF NOT EXISTS `sys_social_todo_link` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户编号',
  `connection_id` bigint NOT NULL COMMENT '企业协同连接ID',
  `task_id` varchar(64) NOT NULL COMMENT 'Flowable任务ID',
  `user_id` bigint NOT NULL COMMENT 'Forge用户ID',
  `external_user_id` varchar(100) DEFAULT NULL COMMENT '外部用户ID',
  `desired_state` varchar(32) NOT NULL COMMENT '期望状态：ACTIVE/CLOSED',
  `delivery_state` varchar(32) NOT NULL DEFAULT 'PENDING' COMMENT '投递状态：PENDING/SENT/FAILED/CLOSED',
  `version` bigint NOT NULL DEFAULT 0 COMMENT '状态版本号（CAS）',
  `external_id` varchar(200) DEFAULT NULL COMMENT '外部待办/卡片ID',
  `idempotency_key` varchar(128) DEFAULT NULL COMMENT '幂等键',
  `retry_at` datetime DEFAULT NULL COMMENT '下次重试时间',
  `last_error_code` varchar(64) DEFAULT NULL COMMENT '最近错误码',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` bigint NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_social_todo_link_active` (`tenant_id`, `connection_id`, `task_id`, `user_id`, `del_flag`),
  KEY `idx_social_todo_link_retry` (`delivery_state`, `retry_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业协同待办投影表';

-- 回调收件箱由留存清理任务物理清理，不做逻辑删除；原文只保存密文。
CREATE TABLE IF NOT EXISTS `sys_social_callback_event` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户编号',
  `connection_id` bigint NOT NULL COMMENT '企业协同连接ID',
  `app_config_id` bigint DEFAULT NULL COMMENT '定位回调凭据的物理应用ID',
  `event_id` varchar(128) DEFAULT NULL COMMENT '外部事件ID',
  `dedup_hash` varchar(64) NOT NULL COMMENT '去重哈希（签名+时间戳+nonce+正文摘要）',
  `event_type` varchar(64) DEFAULT NULL COMMENT '事件类型',
  `event_time` datetime DEFAULT NULL COMMENT '外部事件时间',
  `signature_status` varchar(32) NOT NULL DEFAULT 'VERIFIED' COMMENT '验签状态：VERIFIED/REJECTED',
  `payload_cipher` mediumtext COMMENT '解密后事件正文密文（FPC1版本化密文，按留存期清理）',
  `process_status` varchar(32) NOT NULL DEFAULT 'PENDING' COMMENT '处理状态：PENDING/PROCESSING/PROCESSED/FAILED/DISCARDED',
  `retry_count` int NOT NULL DEFAULT 0 COMMENT '重试次数',
  `next_retry_time` datetime DEFAULT NULL COMMENT '下次重试时间',
  `error_code` varchar(64) DEFAULT NULL COMMENT '错误码',
  `error_summary` varchar(500) DEFAULT NULL COMMENT '脱敏错误摘要',
  `claimed_by` varchar(64) DEFAULT NULL COMMENT '当前处理Worker标识',
  `claim_time` datetime DEFAULT NULL COMMENT '领取时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_social_callback_dedup` (`tenant_id`, `connection_id`, `dedup_hash`),
  KEY `idx_social_callback_process` (`process_status`, `next_retry_time`),
  KEY `idx_social_callback_conn` (`tenant_id`, `connection_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业协同回调事件收件箱';
