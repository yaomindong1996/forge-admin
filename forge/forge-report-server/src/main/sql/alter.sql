-- ============================================
-- Go-View 数据库变更脚本（幂等性 ALTER）
-- 在 init.sql 初始化后执行此脚本
-- ============================================

-- ============================================
-- 1. ai_provider 表新增 default_model 字段
-- ============================================
ALTER TABLE `ai_provider`
  ADD COLUMN IF NOT EXISTS `default_model` VARCHAR(100) DEFAULT NULL COMMENT '默认模型名称' AFTER `models`;

-- ============================================
-- 2. ai_chat_record 表补充 tenant_id 索引
-- ============================================
ALTER TABLE `ai_chat_record`
  ADD KEY IF NOT EXISTS `idx_tenant_id` (`tenant_id`);

-- ============================================
-- 3. 新建 AI 会话表（ai_chat_session）
-- ============================================
CREATE TABLE IF NOT EXISTS `ai_chat_session` (
  `id`           VARCHAR(64)  NOT NULL COMMENT '会话ID（UUID，由前端或服务端生成）',
  `tenant_id`    BIGINT       NOT NULL DEFAULT 0 COMMENT '租户ID',
  `user_id`      BIGINT       NOT NULL COMMENT '用户ID',
  `agent_code`   VARCHAR(50)  DEFAULT NULL COMMENT '关联的 Agent 编码',
  `session_name` VARCHAR(200) DEFAULT NULL COMMENT '会话标题（首条消息截取）',
  `status`       CHAR(1)      NOT NULL DEFAULT '0' COMMENT '状态（0正常 1已删除）',
  `create_time`  DATETIME     DEFAULT NULL COMMENT '创建时间',
  `update_time`  DATETIME     DEFAULT NULL COMMENT '最后更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`, `status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AI会话表';
