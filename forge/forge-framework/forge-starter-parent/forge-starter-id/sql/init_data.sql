-- 示例数据：订单序列配置（每天重置）
INSERT INTO sys_id_sequence (biz_key, max_id, step, version, reset_policy, seq_length, prefix)
VALUES ('order', 0, 1000, 0, 'DAILY', 6, 'ORD')
ON DUPLICATE KEY UPDATE biz_key = biz_key;

-- 示例数据：用户序列配置（不重置）
INSERT INTO sys_id_sequence (biz_key, max_id, step, version, reset_policy, seq_length, prefix)
VALUES ('user', 0, 1000, 0, 'NONE', 10, 'U')
ON DUPLICATE KEY UPDATE biz_key = biz_key;

-- 示例数据：商品序列配置（每小时重置）
INSERT INTO sys_id_sequence (biz_key, max_id, step, version, reset_policy, seq_length, prefix)
VALUES ('product', 0, 500, 0, 'HOURLY', 8, 'PRD')
ON DUPLICATE KEY UPDATE biz_key = biz_key;
