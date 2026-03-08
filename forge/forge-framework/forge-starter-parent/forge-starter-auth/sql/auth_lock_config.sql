-- 登录锁定功能配置项
-- 在 sys_config 表中添加以下配置

-- 根据实际的 sys_config 表结构插入配置项
-- 注意：推荐使用中划线格式（kebab-case），系统会自动兼容驼峰格式（camelCase）
INSERT INTO `sys_config` (`tenant_id`, `config_name`, `config_key`, `config_value`, `config_type`, `config_desc`, `sort`) 
VALUES 
(000000, '最大登录失败尝试次数', 'forge.auth.max-login-attempts', '4', 'Y', '超过此次数后账号将被锁定，防止暴力破解', 100),
(000000, '账号锁定时长', 'forge.auth.lock-duration', '30', 'Y', '登录失败超限后账号锁定的时长（分钟）', 101),
(000000, '登录失败记录保留时长', 'forge.auth.fail-record-expire', '15', 'Y', '在此时间内的失败次数会累计（分钟）', 102);

-- 查询验证
-- SELECT * FROM sys_config WHERE config_key LIKE 'forge.auth.%';

-- 说明：
-- 1. tenant_id 默认为 000000（系统级配置）
-- 2. config_type 设置为 'Y' 表示系统内置配置
-- 3. 这些配置会覆盖 application.yml 中的默认值
-- 4. 修改数据库中的值后，需要重启应用或者调用配置刷新接口 /config/refresh
-- 5. config_key 支持两种格式：
--    - 推荐：forge.auth.max-login-attempts（中划线格式）
--    - 兼容：forge.auth.maxLoginAttempts（驼峰格式）
--    系统会自动将中划线格式转换为驼峰格式，实现双向兼容
