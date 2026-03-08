-- 文件分组表
CREATE TABLE `sys_file_group` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `group_name` varchar(100) NOT NULL COMMENT '分组名称',
  `group_code` varchar(100) DEFAULT NULL COMMENT '分组编码',
  `group_type` varchar(50) DEFAULT 'default' COMMENT '分组类型(document-文档,image-图片,video-视频,audio-音频,archive-压缩包,default-默认)',
  `parent_id` bigint DEFAULT NULL COMMENT '父分组ID',
  `sort` int DEFAULT '0' COMMENT '排序',
  `icon` varchar(100) DEFAULT NULL COMMENT '图标',
  `description` varchar(500) DEFAULT NULL COMMENT '描述',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态(1-正常,0-禁用)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `create_dept` bigint unsigned DEFAULT NULL COMMENT '创建组织ID',
  `deleted` tinyint(1) DEFAULT '0' COMMENT '删除标记(0-未删除,1-已删除)',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_group_type` (`group_type`),
  KEY `idx_group_code` (`group_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文件分组表';

-- 修改文件元数据表，添加分组ID字段
ALTER TABLE `sys_file_metadata` ADD COLUMN `group_id` bigint DEFAULT NULL COMMENT '文件分组ID' AFTER `business_id`;
ALTER TABLE `sys_file_metadata` ADD INDEX `idx_group_id` (`group_id`);

-- 初始化默认分组数据
INSERT INTO `sys_file_group` (`group_name`, `group_code`, `group_type`, `sort`, `icon`, `description`, `status`) VALUES
('全部文件', 'all', 'default', 0, 'i-material-symbols:folder', '全部文件', 1),
('最近上传', 'recent', 'default', 1, 'i-material-symbols:history', '最近上传的文件', 1),
('图片', 'images', 'image', 2, 'i-material-symbols:image', '图片文件', 1),
('文档', 'documents', 'document', 3, 'i-material-symbols:description', '文档文件', 1),
('视频', 'videos', 'video', 4, 'i-material-symbols:movie', '视频文件', 1),
('音频', 'audios', 'audio', 5, 'i-material-symbols:music-note', '音频文件', 1),
('压缩包', 'archives', 'archive', 6, 'i-material-symbols:archive', '压缩包文件', 1);