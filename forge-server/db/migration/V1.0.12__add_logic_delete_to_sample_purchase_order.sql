-- 为 Batch 3.3 采购样例业务表补齐逻辑删除契约。
-- 说明：
-- 1. 本批只处理 sample_purchase_order 主业务表，不处理 Flowable/Forge 流程任务表。
-- 2. 采购单号和业务 Key 使用 logic_delete_active 生成列，仅约束未删除记录。

-- sample_purchase_order.del_flag
SET @table_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sample_purchase_order');
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sample_purchase_order' AND COLUMN_NAME = 'del_flag');
SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
    'ALTER TABLE sample_purchase_order ADD COLUMN del_flag char(1) NOT NULL DEFAULT ''0'' COMMENT ''删除标志（0正常 1删除）'' AFTER status',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sample_purchase_order' AND COLUMN_NAME = 'del_flag');
SET @sql = IF(@table_exists > 0 AND @column_exists > 0,
    'UPDATE sample_purchase_order SET del_flag = ''0'' WHERE del_flag IS NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sample_purchase_order' AND COLUMN_NAME = 'logic_delete_active');
SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
    'ALTER TABLE sample_purchase_order ADD COLUMN logic_delete_active tinyint GENERATED ALWAYS AS (CASE WHEN del_flag = ''0'' THEN 1 ELSE NULL END) STORED COMMENT ''未删除唯一约束标识'' AFTER del_flag',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sample_purchase_order' AND INDEX_NAME = 'uk_sample_purchase_order_no');
SET @sql = IF(@table_exists > 0 AND @index_exists > 0,
    'ALTER TABLE sample_purchase_order DROP INDEX uk_sample_purchase_order_no',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sample_purchase_order' AND INDEX_NAME = 'uk_sample_purchase_order_no_active');
SET @sql = IF(@table_exists > 0 AND @index_exists = 0,
    'CREATE UNIQUE INDEX uk_sample_purchase_order_no_active ON sample_purchase_order (tenant_id, order_no, logic_delete_active)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sample_purchase_order' AND INDEX_NAME = 'uk_sample_purchase_order_business_key');
SET @sql = IF(@table_exists > 0 AND @index_exists > 0,
    'ALTER TABLE sample_purchase_order DROP INDEX uk_sample_purchase_order_business_key',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sample_purchase_order' AND INDEX_NAME = 'uk_sample_purchase_order_business_key_active');
SET @sql = IF(@table_exists > 0 AND @index_exists = 0,
    'CREATE UNIQUE INDEX uk_sample_purchase_order_business_key_active ON sample_purchase_order (tenant_id, business_key, logic_delete_active)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
