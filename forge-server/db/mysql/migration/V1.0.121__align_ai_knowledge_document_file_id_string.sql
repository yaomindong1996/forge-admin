-- ai_knowledge_document.file_id 类型对齐：forge 文件服务(FileManager)返回的 fileId 是 UUID 字符串
-- 原 bigint（引用不存在的 sys_file 表）导致文档登记接口报 "Cannot deserialize value ... Long from String"
ALTER TABLE ai_knowledge_document MODIFY COLUMN `file_id` varchar(64) DEFAULT NULL COMMENT '文件ID(FileManager UUID)' AFTER `knowledge_id`;
