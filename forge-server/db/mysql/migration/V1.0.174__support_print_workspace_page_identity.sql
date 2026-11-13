-- 工作台使用 page_* 等字符串标识。已有数字 page_id 等值转为字符串，source_key 不变。
-- 回退保留扩展列；禁止降为 BIGINT 导致新页面标识丢失。无业务数据/权限变更。
SET @print_page_id_ddl := IF(EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_print_template'
      AND COLUMN_NAME = 'page_id' AND DATA_TYPE = 'bigint'
), 'ALTER TABLE sys_print_template MODIFY COLUMN page_id VARCHAR(128) NULL COMMENT ''工作台页面稳定标识''', 'SELECT 1');
PREPARE print_page_id_stmt FROM @print_page_id_ddl;
EXECUTE print_page_id_stmt;
DEALLOCATE PREPARE print_page_id_stmt;

SET @print_page_id_ddl := IF(EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_print_binding'
      AND COLUMN_NAME = 'page_id' AND DATA_TYPE = 'bigint'
), 'ALTER TABLE sys_print_binding MODIFY COLUMN page_id VARCHAR(128) NULL COMMENT ''工作台页面稳定标识''', 'SELECT 1');
PREPARE print_page_id_stmt FROM @print_page_id_ddl;
EXECUTE print_page_id_stmt;
DEALLOCATE PREPARE print_page_id_stmt;
