-- 短信多通道：角色、场景模板、租户路由策略、Ping 日志

SET @channel_role_exists := (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_sms_config'
    AND COLUMN_NAME = 'channel_role'
);
SET @sql := IF(
  @channel_role_exists = 0,
  'ALTER TABLE sys_sms_config ADD COLUMN channel_role VARCHAR(16) NOT NULL DEFAULT ''SECONDARY'' COMMENT ''PRIMARY/SECONDARY'' AFTER status',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @scene_templates_exists := (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_sms_config'
    AND COLUMN_NAME = 'scene_templates'
);
SET @sql := IF(
  @scene_templates_exists = 0,
  'ALTER TABLE sys_sms_config ADD COLUMN scene_templates TEXT NULL COMMENT ''场景模板映射 JSON'' AFTER template_id',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 存量：每个租户启用配置中 weight 最高的一条标为 PRIMARY
UPDATE sys_sms_config c
JOIN (
  SELECT id FROM (
    SELECT id,
           ROW_NUMBER() OVER (
             PARTITION BY tenant_id
             ORDER BY weight DESC, id ASC
           ) AS rn
    FROM sys_sms_config
    WHERE status = 1
  ) ranked
  WHERE ranked.rn = 1
) pick ON pick.id = c.id
SET c.channel_role = 'PRIMARY'
WHERE c.status = 1;

CREATE TABLE IF NOT EXISTS sys_sms_route_policy (
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  route_mode VARCHAR(32) NOT NULL DEFAULT 'WEIGHTED_RR' COMMENT 'WEIGHTED_RR/PRIMARY_ONLY',
  captcha_lock_primary TINYINT NOT NULL DEFAULT 1 COMMENT '验证码是否锁主通道',
  failover_enabled TINYINT NOT NULL DEFAULT 1 COMMENT '失败是否切换通道',
  failover_max_attempts INT NOT NULL DEFAULT 2 COMMENT '含首次的最大尝试次数',
  ping_enabled TINYINT NOT NULL DEFAULT 0 COMMENT '是否开启定时 Ping',
  ping_phones VARCHAR(500) DEFAULT NULL COMMENT '运维手机号，逗号分隔',
  ping_cron VARCHAR(64) DEFAULT '0 0 */6 * * ?' COMMENT 'Ping Cron',
  ping_mode VARCHAR(32) NOT NULL DEFAULT 'PER_CHANNEL' COMMENT 'PER_CHANNEL/VIA_ROUTER',
  create_by VARCHAR(64) DEFAULT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64) DEFAULT NULL,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='短信路由与 Ping 策略';

CREATE TABLE IF NOT EXISTS sys_sms_ping_log (
  id BIGINT NOT NULL AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  config_id VARCHAR(64) DEFAULT NULL,
  supplier VARCHAR(32) DEFAULT NULL,
  phone VARCHAR(32) DEFAULT NULL,
  status TINYINT NOT NULL DEFAULT 0 COMMENT '0失败 1成功',
  error_msg VARCHAR(1000) DEFAULT NULL,
  send_time DATETIME DEFAULT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_sms_ping_tenant_time (tenant_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='短信 Ping 探活日志';
