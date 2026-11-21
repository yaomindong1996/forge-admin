-- 社区版 AI 基础能力升级建表（PostgreSQL 版本）：MCP 服务与工具目录、技能资料包、知识库默认检索资源与全局助手绑定。

CREATE TABLE IF NOT EXISTS ai_mcp_connection (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    connection_code VARCHAR(64) NOT NULL,
    connection_name VARCHAR(80) NOT NULL,
    endpoint VARCHAR(500) NOT NULL,
    secret_cipher TEXT NULL,
    timeout_seconds INT NOT NULL DEFAULT 15,
    enabled INT NOT NULL DEFAULT 1,
    row_version INT NOT NULL DEFAULT 1,
    sync_state VARCHAR(32) NOT NULL DEFAULT 'UNSYNCED',
    error_code VARCHAR(64) NULL,
    synced_at TIMESTAMP NULL,
    create_by BIGINT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_dept BIGINT NULL,
    update_by BIGINT NULL,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    del_flag BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_ai_mcp_conn_code UNIQUE (tenant_id, connection_code, del_flag)
);
CREATE INDEX IF NOT EXISTS idx_ai_mcp_conn_tenant ON ai_mcp_connection (tenant_id, del_flag, update_time);

CREATE TABLE IF NOT EXISTS ai_mcp_tool (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    connection_id BIGINT NOT NULL,
    connection_version INT NOT NULL DEFAULT 1,
    tool_key VARCHAR(128) NOT NULL,
    remote_name VARCHAR(128) NOT NULL,
    title VARCHAR(160) NOT NULL,
    description TEXT NULL,
    input_schema TEXT NULL,
    contract_hash CHAR(64) NOT NULL,
    available INT NOT NULL DEFAULT 1,
    create_by BIGINT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_dept BIGINT NULL,
    update_by BIGINT NULL,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    del_flag BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_ai_mcp_tool_key UNIQUE (tenant_id, tool_key, del_flag)
);
CREATE INDEX IF NOT EXISTS idx_ai_mcp_tool_conn ON ai_mcp_tool (tenant_id, connection_id, del_flag);

CREATE TABLE IF NOT EXISTS ai_skill_package (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    package_code VARCHAR(80) NOT NULL,
    package_name VARCHAR(120) NOT NULL,
    description VARCHAR(1000) NULL,
    create_by BIGINT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_dept BIGINT NULL,
    update_by BIGINT NULL,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    del_flag BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_ai_skill_pkg_code UNIQUE (tenant_id, package_code, del_flag)
);

CREATE TABLE IF NOT EXISTS ai_skill_package_version (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    package_id BIGINT NOT NULL,
    version_name VARCHAR(80) NOT NULL,
    package_hash CHAR(64) NOT NULL,
    archive_hash CHAR(64) NOT NULL,
    manifest_json TEXT NOT NULL,
    snapshot_json TEXT NULL,
    file_count INT NOT NULL DEFAULT 0,
    total_bytes INT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'IMPORTED',
    create_by BIGINT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_dept BIGINT NULL,
    update_by BIGINT NULL,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    del_flag BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_ai_skill_pkg_ver UNIQUE (tenant_id, package_id, version_name, del_flag)
);
CREATE INDEX IF NOT EXISTS idx_ai_skill_pkg_ver_status ON ai_skill_package_version (tenant_id, status, del_flag);

CREATE TABLE IF NOT EXISTS ai_skill_package_file (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    version_id BIGINT NOT NULL,
    file_path VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    content_hash CHAR(64) NOT NULL,
    size_bytes INT NOT NULL DEFAULT 0,
    create_by BIGINT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_dept BIGINT NULL,
    update_by BIGINT NULL,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    del_flag BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_ai_skill_pkg_file UNIQUE (tenant_id, version_id, file_path, del_flag)
);

CREATE TABLE IF NOT EXISTS ai_agent_skill_package (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    agent_id BIGINT NOT NULL,
    package_id BIGINT NOT NULL,
    version_id BIGINT NOT NULL,
    create_by BIGINT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_dept BIGINT NULL,
    update_by BIGINT NULL,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    del_flag BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_ai_agent_skill_pkg UNIQUE (tenant_id, agent_id, package_id, del_flag)
);
CREATE INDEX IF NOT EXISTS idx_ai_agent_skill_pkg_agent ON ai_agent_skill_package (tenant_id, agent_id, del_flag);

CREATE TABLE IF NOT EXISTS ai_knowledge_resource_profile (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    embedding_model_id BIGINT NOT NULL,
    rerank_model_id BIGINT NULL,
    vector_store_instance_id BIGINT NOT NULL,
    dimension INT NOT NULL,
    chunk_size INT NOT NULL DEFAULT 512,
    revision INT NOT NULL DEFAULT 1,
    create_by BIGINT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_dept BIGINT NULL,
    update_by BIGINT NULL,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    del_flag BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_ai_kb_res_profile UNIQUE (tenant_id, del_flag)
);

CREATE TABLE IF NOT EXISTS ai_hub_binding (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    binding_kind VARCHAR(64) NOT NULL,
    binding_value VARCHAR(255) NOT NULL,
    remark VARCHAR(500) NULL,
    create_by BIGINT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_dept BIGINT NULL,
    update_by BIGINT NULL,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    del_flag BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_ai_hub_binding UNIQUE (tenant_id, binding_kind, del_flag)
);
