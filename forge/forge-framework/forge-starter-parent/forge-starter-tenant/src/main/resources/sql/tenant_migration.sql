-- ====================================================================
-- 多租户字段迁移脚本
-- 用于为现有表添加 tenant_id 字段
-- ====================================================================

-- 1. 为用户表添加租户字段
ALTER TABLE sys_user
ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户ID' AFTER id,
ADD INDEX idx_tenant_id (tenant_id);

-- 2. 为角色表添加租户字段
ALTER TABLE sys_role
ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户ID' AFTER id,
ADD INDEX idx_tenant_id (tenant_id);

-- 3. 为组织表添加租户字段
ALTER TABLE sys_org
ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户ID' AFTER id,
ADD INDEX idx_tenant_id (tenant_id);

-- 4. 为岗位表添加租户字段
ALTER TABLE sys_post
ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户ID' AFTER id,
ADD INDEX idx_tenant_id (tenant_id);

-- 5. 为资源表添加租户字段（如果需要租户隔离）
ALTER TABLE sys_resource
ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户ID' AFTER id,
ADD INDEX idx_tenant_id (tenant_id);

-- 6. 为通知公告表添加租户字段
ALTER TABLE sys_notice
ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户ID' AFTER id,
ADD INDEX idx_tenant_id (tenant_id);

-- ====================================================================
-- 创建租户表（如果不存在）
-- ====================================================================
CREATE TABLE IF NOT EXISTS sys_tenant (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '租户ID',
    tenant_name VARCHAR(100) NOT NULL COMMENT '租户名称',
    contact_person VARCHAR(50) COMMENT '负责人',
    contact_phone VARCHAR(20) COMMENT '联系电话',
    user_limit INT DEFAULT 0 COMMENT '租户人员数量上限（0表示无限制）',
    tenant_status TINYINT DEFAULT 1 COMMENT '租户状态（0-禁用，1-正常）',
    expire_time DATETIME COMMENT '过期时间',
    tenant_desc VARCHAR(500) COMMENT '租户描述',
    browser_icon VARCHAR(500) COMMENT '浏览器icon',
    browser_title VARCHAR(100) COMMENT '浏览器标签名称',
    system_name VARCHAR(100) COMMENT '系统名称',
    system_logo VARCHAR(500) COMMENT '系统logo',
    system_intro VARCHAR(500) COMMENT '系统介绍',
    copyright_info VARCHAR(200) COMMENT '版权显示文本',
    system_layout VARCHAR(50) COMMENT '系统布局',
    system_theme VARCHAR(50) COMMENT '系统主题',
    create_by BIGINT COMMENT '创建人ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT COMMENT '更新人ID',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_name (tenant_name),
    KEY idx_tenant_status (tenant_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户表';

-- ====================================================================
-- 插入默认租户（示例）
-- ====================================================================
INSERT INTO sys_tenant (id, tenant_name, contact_person, contact_phone, tenant_status, system_name)
VALUES 
(1, '系统默认租户', '系统管理员', '13800138000', 1, 'Forge管理系统')
ON DUPLICATE KEY UPDATE id=id;

-- ====================================================================
-- 更新现有数据的租户ID（示例）
-- 将所有现有数据分配给默认租户（ID=1）
-- ====================================================================
UPDATE sys_user SET tenant_id = 1 WHERE tenant_id = 0;
UPDATE sys_role SET tenant_id = 1 WHERE tenant_id = 0;
UPDATE sys_org SET tenant_id = 1 WHERE tenant_id = 0;
UPDATE sys_post SET tenant_id = 1 WHERE tenant_id = 0;
UPDATE sys_resource SET tenant_id = 1 WHERE tenant_id = 0;
UPDATE sys_notice SET tenant_id = 1 WHERE tenant_id = 0;

-- ====================================================================
-- 优化索引（联合索引示例）
-- 如果有频繁的查询条件，建议创建联合索引
-- ====================================================================

-- 示例：用户表的联合索引
ALTER TABLE sys_user ADD INDEX idx_tenant_username (tenant_id, username);
ALTER TABLE sys_user ADD INDEX idx_tenant_status (tenant_id, user_status);

-- 示例：角色表的联合索引
ALTER TABLE sys_role ADD INDEX idx_tenant_rolename (tenant_id, role_name);

-- 示例：组织表的联合索引
ALTER TABLE sys_org ADD INDEX idx_tenant_parent (tenant_id, parent_id);

-- ====================================================================
-- 注意事项：
-- 1. 执行前请备份数据库
-- 2. 根据实际表结构调整字段位置
-- 3. 根据业务需求决定哪些表需要租户隔离
-- 4. 系统配置表、字典表通常不需要租户隔离
-- 5. 日志表根据需求决定是否需要租户隔离
-- ====================================================================
