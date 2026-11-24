-- 行政区划支持村/社区(level=5)：加宽编码列以容纳 12 位国标码；不预置村社区种子数据
ALTER TABLE [sys_region_code] ALTER COLUMN [code] nvarchar(20) NOT NULL;
ALTER TABLE [sys_region_code] ALTER COLUMN [parent_code] nvarchar(20) NULL;
ALTER TABLE [sys_user] ALTER COLUMN [region_code] nvarchar(20) NULL;
