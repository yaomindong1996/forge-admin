-- 行政区划支持村/社区(level=5)：加宽编码列以容纳 12 位国标码；不预置村社区种子数据
ALTER TABLE `sys_region_code`
  MODIFY COLUMN `code` varchar(20) NOT NULL COMMENT '行政区划代码',
  MODIFY COLUMN `parent_code` varchar(20) DEFAULT NULL COMMENT '父级代码',
  MODIFY COLUMN `level` tinyint NOT NULL COMMENT '行政级别(1-省,2-市,3-区/县,4-乡镇/街道,5-村/社区)';

ALTER TABLE `sys_user`
  MODIFY COLUMN `region_code` varchar(20) DEFAULT NULL COMMENT '行政区划编码';
