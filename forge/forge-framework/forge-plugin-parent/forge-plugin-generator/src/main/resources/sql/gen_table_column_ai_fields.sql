-- gen_table_column 新增 AI 增强字段
ALTER TABLE `gen_table_column` ADD COLUMN `validate_rule` varchar(500) DEFAULT NULL COMMENT '验证规则（JSON格式）' AFTER `dict_type`;
ALTER TABLE `gen_table_column` ADD COLUMN `ai_recommended` tinyint(1) DEFAULT 0 COMMENT '是否AI推荐（0否 1是）' AFTER `validate_rule`;
