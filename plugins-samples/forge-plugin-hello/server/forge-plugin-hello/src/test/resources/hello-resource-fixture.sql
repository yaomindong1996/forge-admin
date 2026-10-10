-- 只用于随机内存库，字段/唯一键沿用宿主资源协议，绝不读取本地业务库。
CREATE TABLE sys_resource (
    id BIGINT AUTO_INCREMENT PRIMARY KEY, tenant_id BIGINT NOT NULL DEFAULT 1,
    resource_name VARCHAR(100) NOT NULL, parent_id BIGINT NOT NULL DEFAULT 0,
    resource_type TINYINT NOT NULL, sort INT DEFAULT 0, path VARCHAR(255), component VARCHAR(255),
    is_external TINYINT DEFAULT 0, open_target VARCHAR(20) DEFAULT '_self', is_public TINYINT DEFAULT 0,
    menu_status TINYINT DEFAULT 1, visible TINYINT DEFAULT 1, perms VARCHAR(100), feature_code VARCHAR(64),
    icon VARCHAR(100), api_method VARCHAR(10), api_url VARCHAR(255), keep_alive TINYINT DEFAULT 0,
    always_show TINYINT DEFAULT 0, remark VARCHAR(500), create_by BIGINT, create_time DATETIME DEFAULT NOW(),
    update_by BIGINT, update_time DATETIME DEFAULT NOW(), create_dept BIGINT, client_code VARCHAR(50) DEFAULT 'pc',
    min_user_type INT NOT NULL DEFAULT 2, del_flag BIGINT NOT NULL DEFAULT 0,
    UNIQUE (tenant_id, resource_type, perms, del_flag)
);
CREATE TABLE sys_role_resource (id BIGINT PRIMARY KEY, role_id BIGINT, resource_id BIGINT);
INSERT INTO sys_role_resource (id, role_id, resource_id) VALUES (1, 1, 999);
