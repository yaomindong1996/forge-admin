-- =============================================================================
-- V1.0.38 清理配置中心同步产生的字符串 'null' 脏配置
--
-- 背景：
--   ConfigConverter 将 sys_config_group 中 JSON null 节点经 NullNode.asText()
--   转为 4 字符字符串 "null" 写入 sys_config（如 forge.crypto.rsa-public-key /
--   forge.crypto.rsa-private-key）。DbPropertySourcePostProcessor 将其绑定到
--   CryptoProperties 后 StringUtils.hasText("null") = true，启动时尝试用
--   "null" 解析 RSA 密钥导致失败。
--
-- 说明：
--   这些行是 ConfigSyncService 从 sys_config_group 派生的同步缓存
--   （config_desc 为 '配置中心[xx]同步项'），转换器修复后不会再生；
--   DbPropertySourcePostProcessor 使用原生 JDBC 读取、不过滤 del_flag，
--   因此必须物理删除而非逻辑删除。脚本可重复执行。
-- =============================================================================

DELETE FROM sys_config
WHERE config_value = 'null'
  AND config_desc LIKE '配置中心%同步项';
