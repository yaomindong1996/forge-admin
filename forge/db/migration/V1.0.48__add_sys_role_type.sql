-- =============================================
-- V1.0.48: 角色表增加角色类型字段 + 角色类型字典数据
-- =============================================

-- 1. sys_role 表增加 role_type 字段
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_role' AND COLUMN_NAME = 'role_type');
SET @sql = IF(@col_exists = 0,
  'ALTER TABLE sys_role ADD COLUMN role_type INT DEFAULT 1 COMMENT ''角色类型（1-管理角色，2-业务角色，3-审批角色，4-数据角色）'' AFTER role_key',
  'SELECT ''Column role_type already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. 插入字典类型: sys_role_type
INSERT INTO sys_dict_type (tenant_id, dict_name, dict_type, dict_status, remark, create_time, update_time)
SELECT seed.tenant_id, seed.dict_name, seed.dict_type, seed.dict_status, seed.remark, NOW(), NOW()
FROM (
  SELECT 1 tenant_id, '角色类型' dict_name, 'sys_role_type' dict_type, 1 dict_status, '用于区分不同类型的角色以控制业务' remark
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM sys_dict_type WHERE dict_type = 'sys_role_type'
);

-- 3. 插入字典数据: sys_role_type
INSERT INTO sys_dict_data (tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, dict_status, remark, create_time, update_time)
SELECT seed.tenant_id, seed.dict_sort, seed.dict_label, seed.dict_value, seed.dict_type, NULL, seed.list_class, seed.is_default, 1, seed.remark, NOW(), NOW()
FROM (
  SELECT 1 tenant_id, 1 dict_sort, '管理角色' dict_label, '1' dict_value, 'sys_role_type' dict_type, 'info' list_class, 'Y' is_default, '用于系统管理的角色' remark
  UNION ALL SELECT 1, 2, '业务角色', '2', 'sys_role_type', 'success', 'N', '用于业务操作的角色'
  UNION ALL SELECT 1, 3, '审批角色', '3', 'sys_role_type', 'warning', 'N', '用于审批流程的角色'
  UNION ALL SELECT 1, 4, '数据角色', '4', 'sys_role_type', 'default', 'N', '用于数据权限控制的角色'
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM sys_dict_data WHERE dict_type = 'sys_role_type' AND dict_value = seed.dict_value
);
