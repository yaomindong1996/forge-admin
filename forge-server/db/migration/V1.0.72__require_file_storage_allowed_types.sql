-- Require every file storage configuration to declare an upload type whitelist.

UPDATE sys_file_storage_config
SET allowed_types = 'jpg,jpeg,png,gif,webp,pdf,doc,docx,xls,xlsx,txt,csv,zip,rar,mp4,mp3',
    update_time = CURRENT_TIMESTAMP
WHERE allowed_types IS NULL
   OR TRIM(allowed_types) = ''
   OR LOWER(CONCAT(',', REPLACE(allowed_types, ' ', ''), ',')) REGEXP ',(svg|html|htm|js|mjs|ts|vue|jsp|jspx|php|asp|aspx|sh|bash|bat|cmd|ps1|exe|dll|so|dylib|jar|war|ear|sql|md),';

SET @allowed_types_nullable = (
  SELECT IS_NULLABLE
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_file_storage_config'
    AND COLUMN_NAME = 'allowed_types'
);
SET @sql = IF(@allowed_types_nullable = 'YES',
  'ALTER TABLE sys_file_storage_config MODIFY COLUMN allowed_types varchar(500) NOT NULL COMMENT ''允许的文件类型(逗号分隔)''',
  'SELECT ''Column sys_file_storage_config.allowed_types is already NOT NULL''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
