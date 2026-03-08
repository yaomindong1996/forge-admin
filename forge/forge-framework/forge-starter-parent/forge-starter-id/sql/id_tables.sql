-- 分布式ID序列配置表
CREATE TABLE IF NOT EXISTS `sys_id_sequence` (
  `biz_key` VARCHAR(100) NOT NULL COMMENT '业务维度唯一键',
  `max_id` BIGINT NOT NULL DEFAULT 0 COMMENT '当前已分配的最大ID',
  `step` INT NOT NULL DEFAULT 1000 COMMENT '步长',
  `version` INT NOT NULL DEFAULT 0 COMMENT '版本（乐观锁）',
  `reset_policy` VARCHAR(20) DEFAULT 'NONE' COMMENT '重置策略：NONE/DAILY/HOURLY',
  `seq_length` INT DEFAULT 8 COMMENT '序列长度（左侧补零）',
  `prefix` VARCHAR(50) COMMENT '前缀',
  PRIMARY KEY (`biz_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分布式ID序列配置表';
