-- =============================================
-- 文件存储配置表
-- =============================================
CREATE TABLE IF NOT EXISTS sys_file_storage_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    config_name VARCHAR(100) NOT NULL COMMENT '配置名称',
    storage_type VARCHAR(50) NOT NULL COMMENT '存储类型(local/minio/aliyun_oss等)',
    is_default TINYINT(1) DEFAULT 0 COMMENT '是否默认策略',
    enabled TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    endpoint VARCHAR(500) COMMENT '访问端点',
    access_key VARCHAR(200) COMMENT '访问密钥ID',
    secret_key VARCHAR(200) COMMENT '访问密钥Secret',
    bucket_name VARCHAR(100) COMMENT '存储桶名称',
    region VARCHAR(100) COMMENT '区域',
    base_path VARCHAR(200) COMMENT '基础路径',
    domain VARCHAR(500) COMMENT '访问域名',
    use_https TINYINT(1) DEFAULT 1 COMMENT '是否使用HTTPS',
    max_file_size INT DEFAULT 100 COMMENT '最大文件大小(MB)',
    allowed_types VARCHAR(500) COMMENT '允许的文件类型(逗号分隔)',
    order_num INT DEFAULT 0 COMMENT '排序',
    extra_config TEXT COMMENT '扩展配置(JSON)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_storage_type(storage_type),
    INDEX idx_is_default(is_default)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件存储配置表';

-- =============================================
-- 文件元数据表
-- =============================================
CREATE TABLE IF NOT EXISTS sys_file_metadata (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    file_id VARCHAR(64) NOT NULL UNIQUE COMMENT '文件唯一ID',
    original_name VARCHAR(500) NOT NULL COMMENT '原始文件名',
    storage_name VARCHAR(500) NOT NULL COMMENT '存储文件名',
    file_path VARCHAR(1000) NOT NULL COMMENT '文件路径',
    file_size BIGINT NOT NULL COMMENT '文件大小(字节)',
    mime_type VARCHAR(200) COMMENT '文件MIME类型',
    extension VARCHAR(50) COMMENT '文件扩展名',
    md5 VARCHAR(64) COMMENT '文件MD5',
    storage_type VARCHAR(50) NOT NULL COMMENT '存储策略',
    bucket VARCHAR(100) COMMENT '存储桶/命名空间',
    access_url VARCHAR(1000) COMMENT '访问URL',
    thumbnail_url VARCHAR(1000) COMMENT '缩略图URL',
    business_type VARCHAR(100) COMMENT '业务类型',
    business_id VARCHAR(100) COMMENT '业务ID',
    uploader_id BIGINT COMMENT '上传者ID',
    upload_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    expire_time DATETIME COMMENT '过期时间',
    is_private TINYINT(1) DEFAULT 0 COMMENT '是否私有',
    download_count INT DEFAULT 0 COMMENT '下载次数',
    status TINYINT(1) DEFAULT 1 COMMENT '状态(1-正常,0-已删除)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_file_id(file_id),
    INDEX idx_md5(md5),
    INDEX idx_business(business_type, business_id),
    INDEX idx_uploader(uploader_id),
    INDEX idx_upload_time(upload_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件元数据表';

-- =============================================
-- 初始化默认存储配置
-- =============================================
INSERT INTO sys_file_storage_config 
(config_name, storage_type, is_default, enabled, base_path, max_file_size, allowed_types, order_num)
VALUES 
('本地存储', 'local', 1, 1, '/data/files/', 100, 'jpg,jpeg,png,gif,pdf,doc,docx,xls,xlsx,zip,rar', 1);

-- MinIO配置示例（需根据实际情况修改）
INSERT INTO sys_file_storage_config 
(config_name, storage_type, is_default, enabled, endpoint, access_key, secret_key, bucket_name, use_https, max_file_size, allowed_types, order_num)
VALUES 
('MinIO存储', 'minio', 0, 0, 'http://localhost:9000', 'minioadmin', 'minioadmin', 'forge-files', 0, 500, 'jpg,jpeg,png,gif,pdf,doc,docx,xls,xlsx,zip,rar,mp4,mp3', 2);
