-- Excel 列配置增加图片类型支持
ALTER TABLE sys_excel_column_config
    ADD COLUMN column_type VARCHAR(20) DEFAULT 'TEXT'
        COMMENT '列类型：TEXT/IMAGE' AFTER validation_message;

ALTER TABLE sys_excel_column_config
    ADD COLUMN image_width INT DEFAULT 120
        COMMENT '图片列导出宽度(像素)' AFTER column_type,
    ADD COLUMN image_height INT DEFAULT 90
        COMMENT '图片列导出高度(像素)' AFTER image_width,
    ADD COLUMN image_max_count INT DEFAULT 5
        COMMENT '图片列最大图片数量' AFTER image_height;
