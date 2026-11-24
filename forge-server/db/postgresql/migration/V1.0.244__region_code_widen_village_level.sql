-- 行政区划支持村/社区(level=5)：加宽编码列以容纳 12 位国标码；不预置村社区种子数据
ALTER TABLE "sys_region_code" ALTER COLUMN "code" TYPE varchar(20);
ALTER TABLE "sys_region_code" ALTER COLUMN "parent_code" TYPE varchar(20);
COMMENT ON COLUMN "sys_region_code"."code" IS '行政区划代码';
COMMENT ON COLUMN "sys_region_code"."parent_code" IS '父级代码';
COMMENT ON COLUMN "sys_region_code"."level" IS '行政级别(1-省,2-市,3-区/县,4-乡镇/街道,5-村/社区)';

ALTER TABLE "sys_user" ALTER COLUMN "region_code" TYPE varchar(20);
COMMENT ON COLUMN "sys_user"."region_code" IS '行政区划编码';
