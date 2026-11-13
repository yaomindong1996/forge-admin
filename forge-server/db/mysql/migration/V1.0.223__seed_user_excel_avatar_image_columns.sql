-- 用户 Excel 导入/导出补充头像 IMAGE 列，并修复列名双重 UTF-8 乱码

UPDATE sys_excel_column_config
SET column_name = '头像',
    column_type = 'IMAGE',
    image_width = IFNULL(image_width, 120),
    image_height = IFNULL(image_height, 90),
    image_max_count = IFNULL(image_max_count, 1),
    update_time = NOW()
WHERE field_name = 'avatar'
  AND config_key IN ('sys_user_export', 'sys_user_import')
  AND (
      column_name <> '头像'
      OR column_name IS NULL
      OR IFNULL(column_type, 'TEXT') <> 'IMAGE'
  );

INSERT INTO sys_excel_column_config (
    config_key, field_name, column_name, width, order_num, export, importable, required,
    column_type, image_width, image_height, image_max_count, create_time, update_time, create_by, update_by
)
SELECT 'sys_user_export', 'avatar', '头像', 20, 0, 1, 0, 0,
       'IMAGE', 120, 90, 1, NOW(), NOW(), 1, 1
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM sys_excel_column_config
    WHERE config_key = 'sys_user_export' AND field_name = 'avatar'
);

INSERT INTO sys_excel_column_config (
    config_key, field_name, column_name, width, order_num, export, importable, required,
    column_type, image_width, image_height, image_max_count, create_time, update_time, create_by, update_by
)
SELECT 'sys_user_import', 'avatar', '头像', 20, 0, 1, 1, 0,
       'IMAGE', 120, 90, 1, NOW(), NOW(), 1, 1
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM sys_excel_column_config
    WHERE config_key = 'sys_user_import' AND field_name = 'avatar'
);
