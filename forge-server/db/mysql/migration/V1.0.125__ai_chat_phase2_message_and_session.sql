-- =====================================================================
-- V1.0.125 : AI 对话体验优化计划 阶段二·消息与会话交互增强
-- 目标：
--   1. 为 ai_chat_record 补齐耗时字段：首 token 耗时、总耗时，支撑
--      前端展示「首字延迟 / 生成总时长」（建议项6：耗时/状态展示）。
--   2. 为 ai_chat_session 补齐会话置顶字段：pinned + pinned_time，支撑
--      左侧会话列表置顶与置顶优先排序（建议项7：会话管理增强）。
-- 约定：
--   - 脚本全程幂等（information_schema 判存 + PREPARE/EXECUTE），可重复执行。
--   - 耗时单位毫秒（bigint，可空：历史行与未采集到的行为 NULL）。
--   - 置顶 pinned：0 未置顶 / 1 已置顶；pinned_time 记录置顶时间用于同组排序。
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. ai_chat_record 加耗时列（建议项6）
-- ---------------------------------------------------------------------
SET @record_exists = (SELECT COUNT(*) FROM information_schema.TABLES
                      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_chat_record');

-- 1.1 first_token_ms：从发起到首个正文/思考 token 的耗时（毫秒）
SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_chat_record' AND COLUMN_NAME = 'first_token_ms');
SET @sql = IF(@record_exists > 0 AND @col = 0,
    'ALTER TABLE ai_chat_record ADD COLUMN first_token_ms bigint NULL COMMENT ''首 token 耗时（毫秒），从发起到首个正文/思考增量'' AFTER usage_json',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1.2 total_ms：从发起到本轮 assistant 收口的总耗时（毫秒）
SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_chat_record' AND COLUMN_NAME = 'total_ms');
SET @sql = IF(@record_exists > 0 AND @col = 0,
    'ALTER TABLE ai_chat_record ADD COLUMN total_ms bigint NULL COMMENT ''生成总耗时（毫秒），从发起到 assistant 收口'' AFTER first_token_ms',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------
-- 2. ai_chat_session 加置顶列（建议项7）
-- ---------------------------------------------------------------------
SET @session_exists = (SELECT COUNT(*) FROM information_schema.TABLES
                       WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_chat_session');

-- 2.1 pinned：是否置顶，0 未置顶 / 1 已置顶
SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_chat_session' AND COLUMN_NAME = 'pinned');
SET @sql = IF(@session_exists > 0 AND @col = 0,
    'ALTER TABLE ai_chat_session ADD COLUMN pinned tinyint NOT NULL DEFAULT 0 COMMENT ''是否置顶：0否 1是'' AFTER status',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2.2 pinned_time：置顶时间，用于置顶组内按置顶时间倒序
SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_chat_session' AND COLUMN_NAME = 'pinned_time');
SET @sql = IF(@session_exists > 0 AND @col = 0,
    'ALTER TABLE ai_chat_session ADD COLUMN pinned_time datetime NULL COMMENT ''置顶时间，置顶组内按此倒序'' AFTER pinned',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2.3 置顶排序复合索引（用户维度：置顶优先 + 更新时间倒序）
SET @idx = (SELECT COUNT(*) FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_chat_session' AND INDEX_NAME = 'idx_session_user_pinned');
SET @sql = IF(@session_exists > 0 AND @idx = 0,
    'ALTER TABLE ai_chat_session ADD INDEX idx_session_user_pinned (user_id, status, pinned)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
