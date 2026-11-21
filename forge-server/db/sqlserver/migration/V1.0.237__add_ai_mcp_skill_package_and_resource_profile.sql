-- 社区版 AI 基础能力升级建表（SQL Server，与 MySQL/PG V1.0.237 同语义）
SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;
GO

IF OBJECT_ID(N'ai_mcp_connection', N'U') IS NULL
BEGIN
    CREATE TABLE ai_mcp_connection (
        id BIGINT NOT NULL PRIMARY KEY,
        tenant_id BIGINT NOT NULL CONSTRAINT DF_ai_mcp_conn_tenant DEFAULT (1),
        connection_code NVARCHAR(64) NOT NULL,
        connection_name NVARCHAR(80) NOT NULL,
        endpoint NVARCHAR(500) NOT NULL,
        secret_cipher NVARCHAR(MAX) NULL,
        timeout_seconds INT NOT NULL CONSTRAINT DF_ai_mcp_conn_timeout DEFAULT (15),
        enabled INT NOT NULL CONSTRAINT DF_ai_mcp_conn_enabled DEFAULT (1),
        row_version INT NOT NULL CONSTRAINT DF_ai_mcp_conn_ver DEFAULT (1),
        sync_state NVARCHAR(32) NOT NULL CONSTRAINT DF_ai_mcp_conn_sync DEFAULT (N'UNSYNCED'),
        error_code NVARCHAR(64) NULL,
        synced_at DATETIME2 NULL,
        create_by BIGINT NULL,
        create_time DATETIME2 NOT NULL CONSTRAINT DF_ai_mcp_conn_ctime DEFAULT (SYSUTCDATETIME()),
        create_dept BIGINT NULL,
        update_by BIGINT NULL,
        update_time DATETIME2 NOT NULL CONSTRAINT DF_ai_mcp_conn_utime DEFAULT (SYSUTCDATETIME()),
        del_flag BIGINT NOT NULL CONSTRAINT DF_ai_mcp_conn_del DEFAULT (0)
    );
    CREATE UNIQUE INDEX uk_ai_mcp_conn_code ON ai_mcp_connection (tenant_id, connection_code, del_flag);
    CREATE INDEX idx_ai_mcp_conn_tenant ON ai_mcp_connection (tenant_id, del_flag, update_time);
END
GO

IF OBJECT_ID(N'ai_mcp_tool', N'U') IS NULL
BEGIN
    CREATE TABLE ai_mcp_tool (
        id BIGINT NOT NULL PRIMARY KEY,
        tenant_id BIGINT NOT NULL CONSTRAINT DF_ai_mcp_tool_tenant DEFAULT (1),
        connection_id BIGINT NOT NULL,
        connection_version INT NOT NULL CONSTRAINT DF_ai_mcp_tool_cver DEFAULT (1),
        tool_key NVARCHAR(128) NOT NULL,
        remote_name NVARCHAR(128) NOT NULL,
        title NVARCHAR(160) NOT NULL,
        description NVARCHAR(MAX) NULL,
        input_schema NVARCHAR(MAX) NULL,
        contract_hash NCHAR(64) NOT NULL,
        available INT NOT NULL CONSTRAINT DF_ai_mcp_tool_avail DEFAULT (1),
        create_by BIGINT NULL,
        create_time DATETIME2 NOT NULL CONSTRAINT DF_ai_mcp_tool_ctime DEFAULT (SYSUTCDATETIME()),
        create_dept BIGINT NULL,
        update_by BIGINT NULL,
        update_time DATETIME2 NOT NULL CONSTRAINT DF_ai_mcp_tool_utime DEFAULT (SYSUTCDATETIME()),
        del_flag BIGINT NOT NULL CONSTRAINT DF_ai_mcp_tool_del DEFAULT (0)
    );
    CREATE UNIQUE INDEX uk_ai_mcp_tool_key ON ai_mcp_tool (tenant_id, tool_key, del_flag);
    CREATE INDEX idx_ai_mcp_tool_conn ON ai_mcp_tool (tenant_id, connection_id, del_flag);
END
GO

IF OBJECT_ID(N'ai_skill_package', N'U') IS NULL
BEGIN
    CREATE TABLE ai_skill_package (
        id BIGINT NOT NULL PRIMARY KEY,
        tenant_id BIGINT NOT NULL CONSTRAINT DF_ai_skill_pkg_tenant DEFAULT (1),
        package_code NVARCHAR(80) NOT NULL,
        package_name NVARCHAR(120) NOT NULL,
        description NVARCHAR(1000) NULL,
        create_by BIGINT NULL,
        create_time DATETIME2 NOT NULL CONSTRAINT DF_ai_skill_pkg_ctime DEFAULT (SYSUTCDATETIME()),
        create_dept BIGINT NULL,
        update_by BIGINT NULL,
        update_time DATETIME2 NOT NULL CONSTRAINT DF_ai_skill_pkg_utime DEFAULT (SYSUTCDATETIME()),
        del_flag BIGINT NOT NULL CONSTRAINT DF_ai_skill_pkg_del DEFAULT (0)
    );
    CREATE UNIQUE INDEX uk_ai_skill_pkg_code ON ai_skill_package (tenant_id, package_code, del_flag);
END
GO

IF OBJECT_ID(N'ai_skill_package_version', N'U') IS NULL
BEGIN
    CREATE TABLE ai_skill_package_version (
        id BIGINT NOT NULL PRIMARY KEY,
        tenant_id BIGINT NOT NULL CONSTRAINT DF_ai_skill_ver_tenant DEFAULT (1),
        package_id BIGINT NOT NULL,
        version_name NVARCHAR(80) NOT NULL,
        package_hash NCHAR(64) NOT NULL,
        archive_hash NCHAR(64) NOT NULL,
        manifest_json NVARCHAR(MAX) NOT NULL,
        snapshot_json NVARCHAR(MAX) NULL,
        file_count INT NOT NULL CONSTRAINT DF_ai_skill_ver_fc DEFAULT (0),
        total_bytes INT NOT NULL CONSTRAINT DF_ai_skill_ver_tb DEFAULT (0),
        status NVARCHAR(32) NOT NULL CONSTRAINT DF_ai_skill_ver_status DEFAULT (N'IMPORTED'),
        create_by BIGINT NULL,
        create_time DATETIME2 NOT NULL CONSTRAINT DF_ai_skill_ver_ctime DEFAULT (SYSUTCDATETIME()),
        create_dept BIGINT NULL,
        update_by BIGINT NULL,
        update_time DATETIME2 NOT NULL CONSTRAINT DF_ai_skill_ver_utime DEFAULT (SYSUTCDATETIME()),
        del_flag BIGINT NOT NULL CONSTRAINT DF_ai_skill_ver_del DEFAULT (0)
    );
    CREATE UNIQUE INDEX uk_ai_skill_pkg_ver ON ai_skill_package_version (tenant_id, package_id, version_name, del_flag);
    CREATE INDEX idx_ai_skill_pkg_ver_status ON ai_skill_package_version (tenant_id, status, del_flag);
END
GO

IF OBJECT_ID(N'ai_skill_package_file', N'U') IS NULL
BEGIN
    CREATE TABLE ai_skill_package_file (
        id BIGINT NOT NULL PRIMARY KEY,
        tenant_id BIGINT NOT NULL CONSTRAINT DF_ai_skill_file_tenant DEFAULT (1),
        version_id BIGINT NOT NULL,
        file_path NVARCHAR(255) NOT NULL,
        content NVARCHAR(MAX) NOT NULL,
        content_hash NCHAR(64) NOT NULL,
        size_bytes INT NOT NULL CONSTRAINT DF_ai_skill_file_sz DEFAULT (0),
        create_by BIGINT NULL,
        create_time DATETIME2 NOT NULL CONSTRAINT DF_ai_skill_file_ctime DEFAULT (SYSUTCDATETIME()),
        create_dept BIGINT NULL,
        update_by BIGINT NULL,
        update_time DATETIME2 NOT NULL CONSTRAINT DF_ai_skill_file_utime DEFAULT (SYSUTCDATETIME()),
        del_flag BIGINT NOT NULL CONSTRAINT DF_ai_skill_file_del DEFAULT (0)
    );
    CREATE UNIQUE INDEX uk_ai_skill_pkg_file ON ai_skill_package_file (tenant_id, version_id, file_path, del_flag);
END
GO

IF OBJECT_ID(N'ai_agent_skill_package', N'U') IS NULL
BEGIN
    CREATE TABLE ai_agent_skill_package (
        id BIGINT NOT NULL PRIMARY KEY,
        tenant_id BIGINT NOT NULL CONSTRAINT DF_ai_agent_skill_tenant DEFAULT (1),
        agent_id BIGINT NOT NULL,
        package_id BIGINT NOT NULL,
        version_id BIGINT NOT NULL,
        create_by BIGINT NULL,
        create_time DATETIME2 NOT NULL CONSTRAINT DF_ai_agent_skill_ctime DEFAULT (SYSUTCDATETIME()),
        create_dept BIGINT NULL,
        update_by BIGINT NULL,
        update_time DATETIME2 NOT NULL CONSTRAINT DF_ai_agent_skill_utime DEFAULT (SYSUTCDATETIME()),
        del_flag BIGINT NOT NULL CONSTRAINT DF_ai_agent_skill_del DEFAULT (0)
    );
    CREATE UNIQUE INDEX uk_ai_agent_skill_pkg ON ai_agent_skill_package (tenant_id, agent_id, package_id, del_flag);
    CREATE INDEX idx_ai_agent_skill_pkg_agent ON ai_agent_skill_package (tenant_id, agent_id, del_flag);
END
GO

IF OBJECT_ID(N'ai_knowledge_resource_profile', N'U') IS NULL
BEGIN
    CREATE TABLE ai_knowledge_resource_profile (
        id BIGINT NOT NULL PRIMARY KEY,
        tenant_id BIGINT NOT NULL CONSTRAINT DF_ai_kb_res_tenant DEFAULT (1),
        embedding_model_id BIGINT NOT NULL,
        rerank_model_id BIGINT NULL,
        vector_store_instance_id BIGINT NOT NULL,
        dimension INT NOT NULL,
        chunk_size INT NOT NULL CONSTRAINT DF_ai_kb_res_chunk DEFAULT (512),
        revision INT NOT NULL CONSTRAINT DF_ai_kb_res_rev DEFAULT (1),
        create_by BIGINT NULL,
        create_time DATETIME2 NOT NULL CONSTRAINT DF_ai_kb_res_ctime DEFAULT (SYSUTCDATETIME()),
        create_dept BIGINT NULL,
        update_by BIGINT NULL,
        update_time DATETIME2 NOT NULL CONSTRAINT DF_ai_kb_res_utime DEFAULT (SYSUTCDATETIME()),
        del_flag BIGINT NOT NULL CONSTRAINT DF_ai_kb_res_del DEFAULT (0)
    );
    CREATE UNIQUE INDEX uk_ai_kb_res_profile ON ai_knowledge_resource_profile (tenant_id, del_flag);
END
GO

IF OBJECT_ID(N'ai_hub_binding', N'U') IS NULL
BEGIN
    CREATE TABLE ai_hub_binding (
        id BIGINT NOT NULL PRIMARY KEY,
        tenant_id BIGINT NOT NULL CONSTRAINT DF_ai_hub_bind_tenant DEFAULT (1),
        binding_kind NVARCHAR(64) NOT NULL,
        binding_value NVARCHAR(255) NOT NULL,
        remark NVARCHAR(500) NULL,
        create_by BIGINT NULL,
        create_time DATETIME2 NOT NULL CONSTRAINT DF_ai_hub_bind_ctime DEFAULT (SYSUTCDATETIME()),
        create_dept BIGINT NULL,
        update_by BIGINT NULL,
        update_time DATETIME2 NOT NULL CONSTRAINT DF_ai_hub_bind_utime DEFAULT (SYSUTCDATETIME()),
        del_flag BIGINT NOT NULL CONSTRAINT DF_ai_hub_bind_del DEFAULT (0)
    );
    CREATE UNIQUE INDEX uk_ai_hub_binding ON ai_hub_binding (tenant_id, binding_kind, del_flag);
END
GO
