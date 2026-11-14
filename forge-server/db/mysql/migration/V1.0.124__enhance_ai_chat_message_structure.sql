-- =====================================================================
-- V1.0.124 : AI 对话消息结构化增强（对应《AI 对话体验优化计划》阶段一·变更A）
-- 目标：
--   1. 为 ai_chat_record 补齐结构化字段：思考过程、用量 JSON、附件 JSON、
--      消息状态、更新时间、逻辑删除标志，支撑历史完整回放与错误/HITL 闭环。
--   2. 新增 ai_chat_message_tool_call 工具调用明细表，一次工具调用一行，
--      按 record_id 关联 assistant 消息，便于回放、筛选与审计。
-- 约定：
--   - 脚本全程幂等（information_schema 判存 + PREPARE/EXECUTE），可重复执行。
--   - 数值主键表逻辑删除采用墓碑写法：del_flag=0 表示正常，删除后写入主键值。
--   - 历史行 status 默认置为 'done'，前端回放时旧数据仍可正确展示。
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. ai_chat_record 加列
-- ---------------------------------------------------------------------
SET @table_exists = (SELECT COUNT(*) FROM information_schema.TABLES
                     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_chat_record');

-- 1.1 reasoning：思考过程，与正文分离存储
SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_chat_record' AND COLUMN_NAME = 'reasoning');
SET @sql = IF(@table_exists > 0 AND @col = 0,
    'ALTER TABLE ai_chat_record ADD COLUMN reasoning longtext NULL COMMENT ''思考过程（reasoning），与正文分离存储'' AFTER content',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1.2 usage_json：Token 用量明细 JSON（prompt/completion/total 等）
SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_chat_record' AND COLUMN_NAME = 'usage_json');
SET @sql = IF(@table_exists > 0 AND @col = 0,
    'ALTER TABLE ai_chat_record ADD COLUMN usage_json text NULL COMMENT ''Token 用量明细 JSON（prompt/completion/total 等）'' AFTER token_usage',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1.3 attachment_json：附件明细 JSON，挂在用户消息行（fileId/缩略图等）
SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_chat_record' AND COLUMN_NAME = 'attachment_json');
SET @sql = IF(@table_exists > 0 AND @col = 0,
    'ALTER TABLE ai_chat_record ADD COLUMN attachment_json text NULL COMMENT ''附件明细 JSON（挂在用户消息行，存 fileId/类型/缩略图等）'' AFTER usage_json',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1.4 status：消息状态 streaming/waiting_confirm/done/error/aborted，历史行默认 done
SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_chat_record' AND COLUMN_NAME = 'status');
SET @sql = IF(@table_exists > 0 AND @col = 0,
    'ALTER TABLE ai_chat_record ADD COLUMN status varchar(20) NOT NULL DEFAULT ''done'' COMMENT ''消息状态：streaming/waiting_confirm/done/error/aborted'' AFTER attachment_json',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1.5 interrupt_id：HITL 中断标识，等待确认时写入，恢复后接回同一行
SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_chat_record' AND COLUMN_NAME = 'interrupt_id');
SET @sql = IF(@table_exists > 0 AND @col = 0,
    'ALTER TABLE ai_chat_record ADD COLUMN interrupt_id varchar(64) NULL COMMENT ''HITL 中断标识，waiting_confirm 时写入'' AFTER status',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1.6 error_msg：失败原因，供前端错误块展示
SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_chat_record' AND COLUMN_NAME = 'error_msg');
SET @sql = IF(@table_exists > 0 AND @col = 0,
    'ALTER TABLE ai_chat_record ADD COLUMN error_msg varchar(1024) NULL COMMENT ''失败原因，status=error 时写入'' AFTER interrupt_id',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1.7 update_time：更新时间，应用维护
SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_chat_record' AND COLUMN_NAME = 'update_time');
SET @sql = IF(@table_exists > 0 AND @col = 0,
    'ALTER TABLE ai_chat_record ADD COLUMN update_time datetime NULL COMMENT ''更新时间'' AFTER create_time',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1.8 del_flag：逻辑删除墓碑（0 正常，删除后写主键），支撑重试/重生成软删旧行
SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_chat_record' AND COLUMN_NAME = 'del_flag');
SET @sql = IF(@table_exists > 0 AND @col = 0,
    'ALTER TABLE ai_chat_record ADD COLUMN del_flag bigint NOT NULL DEFAULT 0 COMMENT ''删除标志：0正常，删除后写主键'' AFTER update_time',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1.9 会话+删除标志复合索引，加速按会话过滤未删除消息
SET @idx = (SELECT COUNT(*) FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_chat_record' AND INDEX_NAME = 'idx_session_del');
SET @sql = IF(@table_exists > 0 AND @idx = 0,
    'ALTER TABLE ai_chat_record ADD INDEX idx_session_del (session_id, del_flag)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------
-- 2. ai_chat_message_tool_call 工具调用明细表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ai_chat_message_tool_call (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    record_id BIGINT NOT NULL COMMENT '关联的 assistant 消息 id（ai_chat_record.id）',
    session_id VARCHAR(64) NOT NULL COMMENT '会话ID',
    seq INT NOT NULL DEFAULT 0 COMMENT '同一消息内的工具调用顺序',
    tool_name VARCHAR(128) NOT NULL COMMENT '工具名称',
    tool_args_json longtext NULL COMMENT '工具入参 JSON',
    tool_result_json longtext NULL COMMENT '工具结果 JSON 或结果摘要',
    status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '状态：pending/running/waiting_confirm/success/error/aborted',
    error_msg VARCHAR(1024) NULL COMMENT '错误信息',
    del_flag BIGINT NOT NULL DEFAULT 0 COMMENT '删除标志：0正常，删除后写主键',
    create_by BIGINT NULL COMMENT '创建人',
    create_time DATETIME NULL COMMENT '创建时间',
    create_dept BIGINT NULL COMMENT '创建部门',
    update_by BIGINT NULL COMMENT '更新人',
    update_time DATETIME NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_tool_call_record (record_id, del_flag),
    KEY idx_tool_call_session (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI对话工具调用明细表';
