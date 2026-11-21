-- 社区版 AI 基础能力升级建表：MCP 服务与工具目录、技能资料包（不可变版本）、知识库默认检索资源与全局助手绑定。

CREATE TABLE IF NOT EXISTS ai_mcp_connection (
    id BIGINT NOT NULL COMMENT '主键ID',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    connection_code VARCHAR(64) NOT NULL COMMENT 'MCP服务编码',
    connection_name VARCHAR(80) NOT NULL COMMENT 'MCP服务名称',
    endpoint VARCHAR(500) NOT NULL COMMENT 'Streamable HTTP服务地址',
    secret_cipher TEXT NULL COMMENT '加密后的Bearer令牌',
    timeout_seconds INT NOT NULL DEFAULT 15 COMMENT '超时时间（秒）',
    enabled INT NOT NULL DEFAULT 1 COMMENT '启用状态（0停用 1启用）',
    row_version INT NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
    sync_state VARCHAR(32) NOT NULL DEFAULT 'UNSYNCED' COMMENT '同步状态（UNSYNCED/SYNCED/FAILED）',
    error_code VARCHAR(64) NULL COMMENT '最近同步错误码',
    synced_at DATETIME NULL COMMENT '最近同步时间',
    create_by BIGINT NULL COMMENT '创建人',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    create_dept BIGINT NULL COMMENT '创建部门',
    update_by BIGINT NULL COMMENT '更新人',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag BIGINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0正常，删除后写主键）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ai_mcp_conn_code (tenant_id, connection_code, del_flag),
    KEY idx_ai_mcp_conn_tenant (tenant_id, del_flag, update_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI MCP 远程服务连接';

CREATE TABLE IF NOT EXISTS ai_mcp_tool (
    id BIGINT NOT NULL COMMENT '主键ID',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    connection_id BIGINT NOT NULL COMMENT '所属MCP连接ID',
    connection_version INT NOT NULL DEFAULT 1 COMMENT '同步时连接版本号',
    tool_key VARCHAR(128) NOT NULL COMMENT '平台工具唯一标识',
    remote_name VARCHAR(128) NOT NULL COMMENT '远端MCP工具名',
    title VARCHAR(160) NOT NULL COMMENT '展示标题',
    description TEXT NULL COMMENT '工具描述',
    input_schema MEDIUMTEXT NULL COMMENT '参数JSON Schema',
    contract_hash CHAR(64) NOT NULL COMMENT '契约SHA-256摘要',
    available INT NOT NULL DEFAULT 1 COMMENT '是否可用（0不可用 1可用）',
    create_by BIGINT NULL COMMENT '创建人',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    create_dept BIGINT NULL COMMENT '创建部门',
    update_by BIGINT NULL COMMENT '更新人',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag BIGINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0正常，删除后写主键）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ai_mcp_tool_key (tenant_id, tool_key, del_flag),
    KEY idx_ai_mcp_tool_conn (tenant_id, connection_id, del_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI MCP 远程工具目录';

CREATE TABLE IF NOT EXISTS ai_skill_package (
    id BIGINT NOT NULL COMMENT '主键ID',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    package_code VARCHAR(80) NOT NULL COMMENT '技能包编码',
    package_name VARCHAR(120) NOT NULL COMMENT '技能包名称',
    description VARCHAR(1000) NULL COMMENT '描述',
    create_by BIGINT NULL COMMENT '创建人',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    create_dept BIGINT NULL COMMENT '创建部门',
    update_by BIGINT NULL COMMENT '更新人',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag BIGINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0正常，删除后写主键）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ai_skill_pkg_code (tenant_id, package_code, del_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 技能资料包主表';

CREATE TABLE IF NOT EXISTS ai_skill_package_version (
    id BIGINT NOT NULL COMMENT '主键ID',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    package_id BIGINT NOT NULL COMMENT '技能包ID',
    version_name VARCHAR(80) NOT NULL COMMENT '语义化版本号',
    package_hash CHAR(64) NOT NULL COMMENT '内容SHA-256摘要',
    archive_hash CHAR(64) NOT NULL COMMENT 'ZIP包SHA-256摘要',
    manifest_json MEDIUMTEXT NOT NULL COMMENT '规范化清单JSON',
    snapshot_json MEDIUMTEXT NULL COMMENT '启用后固定快照JSON',
    file_count INT NOT NULL DEFAULT 0 COMMENT '文本文件数',
    total_bytes INT NOT NULL DEFAULT 0 COMMENT '总字节数',
    status VARCHAR(32) NOT NULL DEFAULT 'IMPORTED' COMMENT '状态（IMPORTED/READY/RETIRED）',
    create_by BIGINT NULL COMMENT '创建人',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    create_dept BIGINT NULL COMMENT '创建部门',
    update_by BIGINT NULL COMMENT '更新人',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag BIGINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0正常，删除后写主键）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ai_skill_pkg_ver (tenant_id, package_id, version_name, del_flag),
    KEY idx_ai_skill_pkg_ver_status (tenant_id, status, del_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 技能资料包版本表';

CREATE TABLE IF NOT EXISTS ai_skill_package_file (
    id BIGINT NOT NULL COMMENT '主键ID',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    version_id BIGINT NOT NULL COMMENT '技能包版本ID',
    file_path VARCHAR(255) NOT NULL COMMENT '相对文件路径',
    content MEDIUMTEXT NOT NULL COMMENT '只读UTF-8文本内容',
    content_hash CHAR(64) NOT NULL COMMENT '文件SHA-256摘要',
    size_bytes INT NOT NULL DEFAULT 0 COMMENT '文件字节数',
    create_by BIGINT NULL COMMENT '创建人',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    create_dept BIGINT NULL COMMENT '创建部门',
    update_by BIGINT NULL COMMENT '更新人',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag BIGINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0正常，删除后写主键）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ai_skill_pkg_file (tenant_id, version_id, file_path, del_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 技能资料包只读文件表';

CREATE TABLE IF NOT EXISTS ai_agent_skill_package (
    id BIGINT NOT NULL COMMENT '主键ID',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    agent_id BIGINT NOT NULL COMMENT '智能体ID',
    package_id BIGINT NOT NULL COMMENT '技能包ID',
    version_id BIGINT NOT NULL COMMENT '固定技能版本ID',
    create_by BIGINT NULL COMMENT '创建人',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    create_dept BIGINT NULL COMMENT '创建部门',
    update_by BIGINT NULL COMMENT '更新人',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag BIGINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0正常，删除后写主键）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ai_agent_skill_pkg (tenant_id, agent_id, package_id, del_flag),
    KEY idx_ai_agent_skill_pkg_agent (tenant_id, agent_id, del_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='智能体绑定技能资料包固定版本';

CREATE TABLE IF NOT EXISTS ai_knowledge_resource_profile (
    id BIGINT NOT NULL COMMENT '主键ID',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    embedding_model_id BIGINT NOT NULL COMMENT '默认向量模型ID',
    rerank_model_id BIGINT NULL COMMENT '默认重排模型ID',
    vector_store_instance_id BIGINT NOT NULL COMMENT '默认向量存储实例ID',
    dimension INT NOT NULL COMMENT '默认向量维度',
    chunk_size INT NOT NULL DEFAULT 512 COMMENT '默认分段大小',
    revision INT NOT NULL DEFAULT 1 COMMENT '乐观锁修订号',
    create_by BIGINT NULL COMMENT '创建人',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    create_dept BIGINT NULL COMMENT '创建部门',
    update_by BIGINT NULL COMMENT '更新人',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag BIGINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0正常，删除后写主键）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ai_kb_res_profile (tenant_id, del_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='租户级知识库默认检索资源配置';

CREATE TABLE IF NOT EXISTS ai_hub_binding (
    id BIGINT NOT NULL COMMENT '主键ID',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    binding_kind VARCHAR(64) NOT NULL COMMENT '绑定类型（如GENERAL_AGENT）',
    binding_value VARCHAR(255) NOT NULL COMMENT '绑定目标编码或ID',
    remark VARCHAR(500) NULL COMMENT '说明',
    create_by BIGINT NULL COMMENT '创建人',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    create_dept BIGINT NULL COMMENT '创建部门',
    update_by BIGINT NULL COMMENT '更新人',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag BIGINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0正常，删除后写主键）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ai_hub_binding (tenant_id, binding_kind, del_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='全局AI助手租户默认绑定';
