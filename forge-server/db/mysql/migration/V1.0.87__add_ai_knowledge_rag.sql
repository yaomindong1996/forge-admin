-- RAG 知识库表结构、字典与菜单
-- V1.0.87 只建 RAG 四表 + 字典 + 菜单，不动 ai_agent（ai_agent 新增列统一在 V1.0.88）

-- ============================================================
-- 1. 向量存储/搜索引擎实例表
-- ============================================================
CREATE TABLE IF NOT EXISTS `ai_store_instance` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `instance_name` varchar(100) NOT NULL COMMENT '实例名称',
  `category` varchar(32) NOT NULL COMMENT '类别(vector_store/search_engine)',
  `store_type` varchar(32) NOT NULL COMMENT '类型(MILVUS/PG_VECTOR/ELASTICSEARCH)',
  `config_json` longtext NOT NULL COMMENT '连接配置JSON(host/port/user/token/database等)',
  `status` char(1) NOT NULL DEFAULT '0' COMMENT '状态(0正常 1停用)',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_dept` bigint DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标志(0正常，删除后写主键)',
  PRIMARY KEY (`id`),
  KEY `idx_tenant` (`tenant_id`),
  KEY `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI向量存储/搜索引擎实例';

-- ============================================================
-- 2. 知识库表
-- ============================================================
CREATE TABLE IF NOT EXISTS `ai_knowledge` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `knowledge_name` varchar(100) NOT NULL COMMENT '知识库名称',
  `description` varchar(500) DEFAULT NULL COMMENT '描述',
  `icon` varchar(255) DEFAULT NULL COMMENT '图标',
  `vector_store_instance_id` bigint DEFAULT NULL COMMENT '向量存储实例ID',
  `embedding_model_id` bigint DEFAULT NULL COMMENT 'Embedding模型ID',
  `rerank_model_id` bigint DEFAULT NULL COMMENT 'Rerank模型ID',
  `dimension_of_vector_model` int DEFAULT NULL COMMENT '向量维度(显式覆盖)',
  `chunk_strategy` varchar(32) DEFAULT 'length' COMMENT '分块策略(length/delimiter/regex/smart/qa)',
  `chunk_config_json` longtext DEFAULT NULL COMMENT '分块参数JSON(max_tokens/overlap/delimiters/regex)',
  `search_config_json` longtext DEFAULT NULL COMMENT '检索参数JSON(topK/threshold/fusion/rerank_enable/nearby_count)',
  `dedup_strategy` varchar(32) DEFAULT 'none' COMMENT '去重策略(none/name/content/name_or_content)',
  `dedup_action` varchar(32) DEFAULT 'reject' COMMENT '冲突处理(reject/skip/overwrite)',
  `upload_confirm` char(1) DEFAULT '0' COMMENT '两步上传(0否 1是)',
  `status` char(1) DEFAULT '0' COMMENT '状态(0正常 1停用)',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_dept` bigint DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标志(0正常，删除后写主键)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_knowledge_name_active` (`tenant_id`, `knowledge_name`, `del_flag`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI知识库';

-- ============================================================
-- 3. 知识库文档表
-- ============================================================
CREATE TABLE IF NOT EXISTS `ai_knowledge_document` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `knowledge_id` bigint NOT NULL COMMENT '知识库ID',
  `file_id` bigint DEFAULT NULL COMMENT '文件ID(sys_file)',
  `doc_name` varchar(255) NOT NULL COMMENT '文档名称',
  `doc_type` varchar(32) DEFAULT NULL COMMENT '文档类型(pdf/word/excel/markdown/txt/html/url/manual)',
  `source_type` varchar(32) DEFAULT 'upload' COMMENT '来源(upload/url/manual/db)',
  `source_url` varchar(1000) DEFAULT NULL COMMENT 'URL来源',
  `content_hash` varchar(64) DEFAULT NULL COMMENT '内容SHA-256(去重)',
  `chunk_count` int DEFAULT 0 COMMENT '分块数',
  `process_status` varchar(32) DEFAULT 'pending' COMMENT '处理状态(pending/processing/success/failed)',
  `process_error` varchar(1000) DEFAULT NULL COMMENT '处理错误信息',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_dept` bigint DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标志(0正常，删除后写主键)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_knowledge_doc_name_active` (`knowledge_id`, `doc_name`, `del_flag`),
  KEY `idx_knowledge` (`knowledge_id`),
  KEY `idx_hash` (`content_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI知识库文档';

-- ============================================================
-- 4. 知识库分块表
-- ============================================================
CREATE TABLE IF NOT EXISTS `ai_knowledge_chunk` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `knowledge_id` bigint NOT NULL COMMENT '知识库ID',
  `document_id` bigint NOT NULL COMMENT '文档ID',
  `chunk_index` int NOT NULL COMMENT '分块序号',
  `content` longtext NOT NULL COMMENT '分块内容',
  `title` varchar(500) DEFAULT NULL COMMENT '分块标题(可选)',
  `token_count` int DEFAULT 0 COMMENT 'token数',
  `vector_id` varchar(200) DEFAULT NULL COMMENT '向量ID(每分块独立，不跨文档共享)',
  `ref_count` int DEFAULT 1 COMMENT '保留列(各自向量方案恒为1，不参与逻辑)',
  `content_hash` varchar(64) DEFAULT NULL COMMENT '内容哈希',
  `retrieval_count` int DEFAULT 0 COMMENT '被检索次数',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_dept` bigint DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标志(0正常，删除后写主键)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_chunk_active` (`document_id`, `chunk_index`, `del_flag`),
  KEY `idx_document` (`document_id`),
  KEY `idx_knowledge` (`knowledge_id`),
  KEY `idx_hash` (`content_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI知识库分块';

-- ============================================================
-- 5. 字典
-- ============================================================

INSERT INTO sys_dict_type (tenant_id, dict_name, dict_type, dict_status, remark, create_by, create_time, update_by, update_time, create_dept)
SELECT seed.tenant_id, seed.dict_name, seed.dict_type, 1, seed.remark, 1, NOW(), 1, NOW(), 1
FROM (
  SELECT 1 tenant_id, 'AI存储实例类别' dict_name, 'ai_store_instance_category' dict_type, '向量存储/搜索引擎' remark
  UNION ALL SELECT 1, 'AI向量存储类型', 'ai_vector_store_type', 'Milvus/PgVector/ES'
  UNION ALL SELECT 1, 'AI知识库处理状态', 'ai_knowledge_process_status', '文档处理状态'
) seed
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type t WHERE t.tenant_id = seed.tenant_id AND t.dict_type = seed.dict_type);

INSERT INTO sys_dict_data (tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, dict_status, remark, create_by, create_time, update_by, update_time, create_dept)
SELECT seed.tenant_id, seed.dict_sort, seed.dict_label, seed.dict_value, seed.dict_type, NULL, seed.list_class, seed.is_default, 1, seed.remark, 1, NOW(), 1, NOW(), 1
FROM (
  SELECT 1 tenant_id, 1 dict_sort, '向量存储' dict_label, 'vector_store' dict_value, 'ai_store_instance_category' dict_type, 'primary' list_class, 'Y' is_default, '向量数据库' remark
  UNION ALL SELECT 1, 2, '搜索引擎', 'search_engine', 'ai_store_instance_category', 'info', 'N', '全文检索引擎'
  UNION ALL SELECT 1, 1, 'Milvus', 'MILVUS', 'ai_vector_store_type', 'success', 'Y', 'Milvus向量数据库'
  UNION ALL SELECT 1, 2, 'PgVector', 'PG_VECTOR', 'ai_vector_store_type', 'info', 'N', 'PostgreSQL PgVector'
  UNION ALL SELECT 1, 3, 'Elasticsearch', 'ELASTICSEARCH', 'ai_vector_store_type', 'warning', 'N', 'Elasticsearch'
  UNION ALL SELECT 1, 1, '待处理', 'pending', 'ai_knowledge_process_status', 'default', 'N', '等待处理'
  UNION ALL SELECT 1, 2, '处理中', 'processing', 'ai_knowledge_process_status', 'primary', 'N', '正在处理'
  UNION ALL SELECT 1, 3, '成功', 'success', 'ai_knowledge_process_status', 'success', 'N', '处理完成'
  UNION ALL SELECT 1, 4, '失败', 'failed', 'ai_knowledge_process_status', 'error', 'N', '处理失败'
) seed
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data d WHERE d.tenant_id = seed.tenant_id AND d.dict_type = seed.dict_type AND d.dict_value = seed.dict_value);

-- ============================================================
-- 6. 菜单（参照 V1.0.18 完整模板）
-- ============================================================

-- 知识库管理菜单
INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external, open_target, is_public, menu_status, visible, perms, icon, keep_alive, always_show, remark, create_by, create_time, update_by, update_time, create_dept, client_code)
SELECT 1, '知识库', COALESCE((SELECT parent_id FROM (SELECT parent_id FROM sys_resource WHERE path = '/ai/provider-model' LIMIT 1) x), 0), 1, 4,
       '/ai/knowledge', NULL, 0, '_self', 0, 1, 1, NULL, 'ionicons5:BookOutline', 1, 0,
       'AI知识库管理', 1, NOW(), 1, NOW(), 1, 'pc'
WHERE NOT EXISTS (SELECT 1 FROM sys_resource r WHERE r.tenant_id = 1 AND r.path = '/ai/knowledge');

-- 知识库管理子菜单
INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external, open_target, is_public, menu_status, visible, perms, icon, keep_alive, always_show, remark, create_by, create_time, update_by, update_time, create_dept, client_code)
SELECT 1, seed.resource_name, menu.id, seed.resource_type, seed.sort, seed.path, seed.component, 0, '_self', 0, 1, 1, seed.perms, seed.icon, 1, 0, seed.remark, 1, NOW(), 1, NOW(), 1, 'pc'
FROM (SELECT id FROM sys_resource WHERE tenant_id = 1 AND path = '/ai/knowledge' LIMIT 1) menu
JOIN (
  SELECT '知识库管理' resource_name, 2 resource_type, 1 sort, '/ai/knowledge/list' path, 'ai/knowledge/index' component, 'ai:knowledge:list' perms, 'ionicons5:LibraryOutline' icon, '知识库CRUD与文档管理' remark
  UNION ALL SELECT '存储实例', 2, 2, '/ai/knowledge/store', 'ai/store-instance/index' component, 'ai:store:list' perms, 'ionicons5:ServerOutline' icon, '向量存储实例管理'
) seed
WHERE NOT EXISTS (SELECT 1 FROM sys_resource r WHERE r.tenant_id = 1 AND r.path = seed.path);

-- 知识库管理按钮权限
INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, is_external, open_target, is_public, menu_status, visible, perms, keep_alive, always_show, remark, create_by, create_time, update_by, update_time, create_dept, client_code)
SELECT 1, seed.resource_name, menu.id, 3, seed.sort, 0, '_self', 0, 1, 1, seed.perms, 0, 0, seed.remark, 1, NOW(), 1, NOW(), 1, 'pc'
FROM (SELECT id FROM sys_resource WHERE tenant_id = 1 AND path = '/ai/knowledge/list' LIMIT 1) menu
JOIN (
  SELECT '新增知识库' resource_name, 1 sort, 'ai:knowledge:add' perms, '新增知识库' remark
  UNION ALL SELECT '编辑知识库', 2, 'ai:knowledge:edit', '编辑知识库'
  UNION ALL SELECT '删除知识库', 3, 'ai:knowledge:delete', '删除知识库'
  UNION ALL SELECT '检索调试', 4, 'ai:knowledge:search', '知识库检索调试'
  UNION ALL SELECT 'QA对话', 5, 'ai:knowledge:qa', '知识库QA对话'
) seed
WHERE NOT EXISTS (SELECT 1 FROM sys_resource r WHERE r.tenant_id = 1 AND r.perms = seed.perms);

-- 存储实例按钮权限
INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, is_external, open_target, is_public, menu_status, visible, perms, keep_alive, always_show, remark, create_by, create_time, update_by, update_time, create_dept, client_code)
SELECT 1, seed.resource_name, menu.id, 3, seed.sort, 0, '_self', 0, 1, 1, seed.perms, 0, 0, seed.remark, 1, NOW(), 1, NOW(), 1, 'pc'
FROM (SELECT id FROM sys_resource WHERE tenant_id = 1 AND path = '/ai/knowledge/store' LIMIT 1) menu
JOIN (
  SELECT '新增存储实例' resource_name, 1 sort, 'ai:store:add' perms, '新增存储实例' remark
  UNION ALL SELECT '编辑存储实例', 2, 'ai:store:edit', '编辑存储实例'
  UNION ALL SELECT '删除存储实例', 3, 'ai:store:delete', '删除存储实例'
  UNION ALL SELECT '测试连接', 4, 'ai:store:test', '测试存储实例连接'
) seed
WHERE NOT EXISTS (SELECT 1 FROM sys_resource r WHERE r.tenant_id = 1 AND r.perms = seed.perms);

-- 授予管理员权限
INSERT INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT 1, admin_role.id, resource.id, NOW()
FROM (SELECT id FROM sys_role WHERE tenant_id = 1 AND role_key = 'admin' ORDER BY id LIMIT 1) admin_role
JOIN sys_resource resource ON resource.tenant_id = 1
WHERE resource.client_code = 'pc'
  AND (
    resource.path IN ('/ai/knowledge', '/ai/knowledge/list', '/ai/knowledge/store')
    OR resource.perms IN (
      'ai:knowledge:list', 'ai:knowledge:add', 'ai:knowledge:edit', 'ai:knowledge:delete',
      'ai:knowledge:search', 'ai:knowledge:qa',
      'ai:store:list', 'ai:store:add', 'ai:store:edit', 'ai:store:delete', 'ai:store:test'
    )
  )
  AND NOT EXISTS (
    SELECT 1
    FROM sys_role_resource existing
    WHERE existing.tenant_id = 1
      AND existing.role_id = admin_role.id
      AND existing.resource_id = resource.id
  );
