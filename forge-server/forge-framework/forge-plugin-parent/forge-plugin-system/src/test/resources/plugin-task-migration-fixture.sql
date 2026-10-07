-- 合成表只校验当前迁移的幂等、权限唯一键和客户菜单保护，不替代目标 MySQL/Flyway。
CREATE TABLE sys_resource (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, tenant_id BIGINT, resource_name VARCHAR(100), parent_id BIGINT,
 resource_type INT, sort INT, path VARCHAR(255), component VARCHAR(255), is_external INT, open_target VARCHAR(20),
 is_public INT, menu_status INT, visible INT, perms VARCHAR(100), icon VARCHAR(100), keep_alive INT, always_show INT,
 remark VARCHAR(500), create_by BIGINT, create_time TIMESTAMP, update_by BIGINT, update_time TIMESTAMP,
 create_dept BIGINT, client_code VARCHAR(50), min_user_type INT, del_flag BIGINT DEFAULT 0,
 api_method VARCHAR(10), api_url VARCHAR(255), UNIQUE(tenant_id, resource_type, perms, client_code, del_flag)
);
CREATE TABLE sys_dict_type (
 dict_id BIGINT AUTO_INCREMENT PRIMARY KEY, tenant_id BIGINT, dict_name VARCHAR(100), dict_type VARCHAR(100),
 dict_status INT, create_by BIGINT, create_time TIMESTAMP, update_by BIGINT, update_time TIMESTAMP,
 create_dept BIGINT, del_flag BIGINT DEFAULT 0, UNIQUE(tenant_id, dict_type, del_flag)
);
CREATE TABLE sys_dict_data (
 dict_code BIGINT AUTO_INCREMENT PRIMARY KEY, tenant_id BIGINT, dict_sort INT, dict_label VARCHAR(100),
 dict_value VARCHAR(100), dict_type VARCHAR(100), list_class VARCHAR(100), is_default VARCHAR(1), dict_status INT,
 create_by BIGINT, create_time TIMESTAMP, update_by BIGINT, update_time TIMESTAMP,
 create_dept BIGINT, del_flag BIGINT DEFAULT 0, UNIQUE(tenant_id, dict_type, dict_value, del_flag)
);
INSERT INTO sys_resource(tenant_id, resource_name, resource_type, parent_id, client_code)
 VALUES (1, '平台管理', 1, 0, 'pc');
