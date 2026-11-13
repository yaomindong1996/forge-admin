-- 消息中心企业协同投递扩展：消息主表绑定连接与幂等键，接收人支持逐人投递状态，发送记录支持逐次尝试。

-- ============================================================
-- 1. sys_message：连接与幂等键
-- ============================================================

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_message' AND COLUMN_NAME = 'connection_id');
SET @sql = IF(@col = 0, 'ALTER TABLE sys_message ADD COLUMN connection_id bigint DEFAULT NULL COMMENT ''企业协同连接ID（COLLABORATION渠道使用）'' AFTER biz_key', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_message' AND COLUMN_NAME = 'idempotency_key');
SET @sql = IF(@col = 0, 'ALTER TABLE sys_message ADD COLUMN idempotency_key varchar(128) DEFAULT NULL COMMENT ''确定性消息幂等键（相同键并发只创建一份逻辑消息）'' AFTER connection_id', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 幂等键唯一约束：NULL 不参与唯一性，旧消息不受影响。
SET @idx = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_message' AND INDEX_NAME = 'uk_message_idempotency');
SET @sql = IF(@idx = 0, 'ALTER TABLE sys_message ADD UNIQUE KEY uk_message_idempotency (tenant_id, idempotency_key)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================================
-- 2. sys_message_receiver：逐接收人投递状态与补偿
-- ============================================================

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_message_receiver' AND COLUMN_NAME = 'delivery_status');
SET @sql = IF(@col = 0, 'ALTER TABLE sys_message_receiver ADD COLUMN delivery_status varchar(32) DEFAULT NULL COMMENT ''外部渠道投递状态：PENDING/SENT/FAILED/SKIPPED（站内信为NULL）'' AFTER read_time', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_message_receiver' AND COLUMN_NAME = 'delivery_attempts');
SET @sql = IF(@col = 0, 'ALTER TABLE sys_message_receiver ADD COLUMN delivery_attempts int NOT NULL DEFAULT 0 COMMENT ''投递尝试次数'' AFTER delivery_status', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_message_receiver' AND COLUMN_NAME = 'external_id');
SET @sql = IF(@col = 0, 'ALTER TABLE sys_message_receiver ADD COLUMN external_id varchar(200) DEFAULT NULL COMMENT ''外部渠道逐人消息ID'' AFTER delivery_attempts', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_message_receiver' AND COLUMN_NAME = 'last_error_code');
SET @sql = IF(@col = 0, 'ALTER TABLE sys_message_receiver ADD COLUMN last_error_code varchar(64) DEFAULT NULL COMMENT ''最近失败错误码'' AFTER external_id', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_message_receiver' AND COLUMN_NAME = 'last_attempt_time');
SET @sql = IF(@col = 0, 'ALTER TABLE sys_message_receiver ADD COLUMN last_attempt_time datetime DEFAULT NULL COMMENT ''最近投递尝试时间'' AFTER last_error_code', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_message_receiver' AND COLUMN_NAME = 'next_retry_time');
SET @sql = IF(@col = 0, 'ALTER TABLE sys_message_receiver ADD COLUMN next_retry_time datetime DEFAULT NULL COMMENT ''下次补偿重试时间'' AFTER last_attempt_time', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_message_receiver' AND INDEX_NAME = 'idx_receiver_delivery_retry');
SET @sql = IF(@idx = 0, 'ALTER TABLE sys_message_receiver ADD KEY idx_receiver_delivery_retry (delivery_status, next_retry_time)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================================
-- 3. sys_message_send_record：逐次渠道尝试与供应商请求ID
-- ============================================================

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_message_send_record' AND COLUMN_NAME = 'connection_id');
SET @sql = IF(@col = 0, 'ALTER TABLE sys_message_send_record ADD COLUMN connection_id bigint DEFAULT NULL COMMENT ''企业协同连接ID'' AFTER channel', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_message_send_record' AND COLUMN_NAME = 'idempotency_key');
SET @sql = IF(@col = 0, 'ALTER TABLE sys_message_send_record ADD COLUMN idempotency_key varchar(128) DEFAULT NULL COMMENT ''本次渠道投递幂等键'' AFTER connection_id', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_message_send_record' AND COLUMN_NAME = 'attempt_no');
SET @sql = IF(@col = 0, 'ALTER TABLE sys_message_send_record ADD COLUMN attempt_no int NOT NULL DEFAULT 1 COMMENT ''第几次尝试'' AFTER idempotency_key', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_message_send_record' AND COLUMN_NAME = 'provider_request_id');
SET @sql = IF(@col = 0, 'ALTER TABLE sys_message_send_record ADD COLUMN provider_request_id varchar(128) DEFAULT NULL COMMENT ''供应商请求ID（用于排障，不含敏感内容）'' AFTER attempt_no', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_message_send_record' AND INDEX_NAME = 'idx_send_record_idempotency');
SET @sql = IF(@idx = 0, 'ALTER TABLE sys_message_send_record ADD KEY idx_send_record_idempotency (tenant_id, idempotency_key)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
