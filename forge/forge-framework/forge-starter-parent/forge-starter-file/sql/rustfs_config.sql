-- =============================================
-- RustFS文件存储配置
-- =============================================

-- 添加RustFS存储配置
INSERT INTO sys_file_storage_config 
(config_name, storage_type, is_default, enabled, endpoint, access_key, secret_key, bucket_name, use_https, max_file_size, allowed_types, order_num)
VALUES 
('RustFS存储', 'rustfs', 0, 0, 'http://192.168.1.100:9000', 'rustfsadmin', 'rustfssecret', 'forge-files', 0, 500, 'jpg,jpeg,png,gif,pdf,doc,docx,xls,xlsx,zip,rar,mp4,mp3', 3);

-- 说明:
-- 1. endpoint: RustFS服务地址 (根据实际情况修改)
-- 2. access_key/secret_key: RustFS访问凭据 (根据实际情况修改)
-- 3. bucket_name: 存储桶名称 (需要提前在RustFS中创建)
-- 4. 如果要设置为默认存储,可以将 is_default 设置为 1
-- 5. 如果要启用RustFS,可以将 enabled 设置为 1
