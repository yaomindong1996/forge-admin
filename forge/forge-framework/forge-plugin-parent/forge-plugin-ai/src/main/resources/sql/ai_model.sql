-- =============================================
-- Forge AI 模型管理模块 - 建表及菜单数据
-- =============================================

-- 1. 创建 ai_model 表
CREATE TABLE IF NOT EXISTS `ai_model` (
    `id`           BIGINT       NOT NULL COMMENT '主键',
    `provider_id`  BIGINT       NOT NULL COMMENT '供应商ID',
    `model_type`   VARCHAR(32)  NOT NULL COMMENT '模型类型(chat/embedding/image/audio)',
    `model_id`     VARCHAR(128) NOT NULL COMMENT '模型标识(如gpt-4o)',
    `model_name`   VARCHAR(128) NOT NULL COMMENT '模型显示名称',
    `description`  VARCHAR(512)          DEFAULT NULL COMMENT '描述',
    `max_tokens`   INT                   DEFAULT NULL COMMENT '最大Token数',
    `icon`         VARCHAR(512)          DEFAULT NULL COMMENT '模型图标(文件ID或URL)',
    `is_default`   CHAR(1)               DEFAULT '0' COMMENT '是否默认模型(0否1是)',
    `status`       CHAR(1)               DEFAULT '0' COMMENT '状态(0正常1停用)',
    `sort_order`   INT                   DEFAULT 0 COMMENT '排序号',
    `tenant_id`    BIGINT                DEFAULT NULL COMMENT '租户编号',
    `create_by`    BIGINT                DEFAULT NULL COMMENT '创建人',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_dept`  BIGINT       UNSIGNED DEFAULT NULL COMMENT '创建组织ID',
    `update_by`    BIGINT                DEFAULT NULL COMMENT '更新人',
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark`       VARCHAR(512)          DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    INDEX `idx_ai_model_provider_id` (`provider_id`),
    INDEX `idx_ai_model_type` (`model_type`),
    UNIQUE INDEX `uk_ai_model_provider_model` (`provider_id`, `model_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI模型表';

-- =============================================
-- 2. 菜单数据（sys_resource）
-- =============================================

-- AI管理一级目录
INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external, is_public, menu_status, visible, icon, keep_alive, always_show, remark, create_time, update_time)
VALUES (0, 'AI管理', 0, 1, 60, '/ai', NULL, 0, 0, 1, 1, 'mdi:robot', 0, 1, 'AI管理目录', NOW(), NOW());

SET @ai_root_id = LAST_INSERT_ID();

-- 供应商管理菜单
INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external, is_public, menu_status, visible, icon, keep_alive, always_show, remark, create_time, update_time)
VALUES (0, '供应商管理', @ai_root_id, 2, 1, '/ai/provider', '/ai/provider', 0, 0, 1, 1, 'mdi:server', 0, 0, 'AI供应商管理', NOW(), NOW());

SET @ai_provider_id = LAST_INSERT_ID();

-- 模型管理菜单
INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external, is_public, menu_status, visible, icon, keep_alive, always_show, remark, create_time, update_time)
VALUES (0, '模型管理', @ai_root_id, 2, 2, '/ai/model', '/ai/model', 0, 0, 1, 1, 'mdi:brain', 0, 0, 'AI模型管理', NOW(), NOW());

SET @ai_model_id = LAST_INSERT_ID();

-- =============================================
-- 3. 按钮权限
-- =============================================

-- 供应商管理按钮
INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, perms, is_external, is_public, menu_status, visible, remark, create_time, update_time)
VALUES
(0, '供应商新增', @ai_provider_id, 3, 1, 'ai:provider:add', 0, 0, 1, 1, '新增AI供应商', NOW(), NOW()),
(0, '供应商编辑', @ai_provider_id, 3, 2, 'ai:provider:edit', 0, 0, 1, 1, '编辑AI供应商', NOW(), NOW()),
(0, '供应商删除', @ai_provider_id, 3, 3, 'ai:provider:delete', 0, 0, 1, 1, '删除AI供应商', NOW(), NOW()),
(0, '供应商测试', @ai_provider_id, 3, 4, 'ai:provider:test', 0, 0, 1, 1, '测试供应商连接', NOW(), NOW());

-- 模型管理按钮
INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, perms, is_external, is_public, menu_status, visible, remark, create_time, update_time)
VALUES
(0, '模型新增', @ai_model_id, 3, 1, 'ai:model:add', 0, 0, 1, 1, '新增AI模型', NOW(), NOW()),
(0, '模型编辑', @ai_model_id, 3, 2, 'ai:model:edit', 0, 0, 1, 1, '编辑AI模型', NOW(), NOW()),
(0, '模型删除', @ai_model_id, 3, 3, 'ai:model:delete', 0, 0, 1, 1, '删除AI模型', NOW(), NOW());

-- =============================================
-- 4. 给超级管理员角色分配AI管理菜单权限
-- =============================================

INSERT INTO sys_role_resource (role_id, resource_id)
SELECT 1, id FROM sys_resource WHERE tenant_id = 0 AND id >= @ai_root_id;

-- =============================================
-- 5. 字典类型数据
-- =============================================

-- AI 供应商类型字典
INSERT INTO sys_dict_type (tenant_id, dict_name, dict_type, dict_status, remark, create_time, update_time)
VALUES (1, 'AI供应商类型', 'ai_provider_type', 1, 'AI供应商类型列表', NOW(), NOW());

SET @dict_ai_provider_type = LAST_INSERT_ID();

-- AI 模型类型字典
INSERT INTO sys_dict_type (tenant_id, dict_name, dict_type, dict_status, remark, create_time, update_time)
VALUES (1, 'AI模型类型', 'ai_model_type', 1, 'AI模型类型列表', NOW(), NOW());

SET @dict_ai_model_type = LAST_INSERT_ID();

-- AI 状态字典（0正常 1停用）
INSERT INTO sys_dict_type (tenant_id, dict_name, dict_type, dict_status, remark, create_time, update_time)
VALUES (1, 'AI状态', 'ai_status', 1, 'AI模块状态(0正常 1停用)', NOW(), NOW());

SET @dict_ai_status = LAST_INSERT_ID();

-- AI 是否默认字典（0否 1是）
INSERT INTO sys_dict_type (tenant_id, dict_name, dict_type, dict_status, remark, create_time, update_time)
VALUES (1, 'AI是否默认', 'ai_is_default', 1, 'AI模块是否默认(0否 1是)', NOW(), NOW());

SET @dict_ai_is_default = LAST_INSERT_ID();

-- =============================================
-- 6. 字典数据
-- =============================================

-- AI 供应商类型字典数据
INSERT INTO sys_dict_data (tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, dict_status, remark, create_time, update_time)
VALUES
(1, 1, '阿里百炼', 'alibaba', 'ai_provider_type', NULL, 'default', 'N', 1, '阿里百炼平台', NOW(), NOW()),
(1, 2, 'OpenAI', 'openai', 'ai_provider_type', NULL, 'success', 'N', 1, 'OpenAI平台', NOW(), NOW()),
(1, 3, '智谱 AI', 'zhipu', 'ai_provider_type', NULL, 'info', 'N', 1, '智谱AI平台', NOW(), NOW()),
(1, 4, 'Moonshot', 'moonshot', 'ai_provider_type', NULL, 'default', 'N', 1, 'Moonshot平台', NOW(), NOW()),
(1, 5, 'DeepSeek', 'deepseek', 'ai_provider_type', NULL, 'default', 'N', 1, 'DeepSeek平台', NOW(), NOW()),
(1, 6, 'Ollama（本地）', 'ollama', 'ai_provider_type', NULL, 'warning', 'N', 1, 'Ollama本地部署', NOW(), NOW()),
(1, 7, 'Azure', 'azure', 'ai_provider_type', NULL, 'info', 'N', 1, 'Azure OpenAI', NOW(), NOW()),
(1, 8, '自定义', 'custom', 'ai_provider_type', NULL, 'default', 'N', 1, '自定义供应商', NOW(), NOW());

-- AI 模型类型字典数据
INSERT INTO sys_dict_data (tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, dict_status, remark, create_time, update_time)
VALUES
(1, 1, '对话', 'chat', 'ai_model_type', NULL, 'success', 'N', 1, '对话模型', NOW(), NOW()),
(1, 2, '向量化', 'embedding', 'ai_model_type', NULL, 'info', 'N', 1, '向量化模型', NOW(), NOW()),
(1, 3, '图像生成', 'image', 'ai_model_type', NULL, 'warning', 'N', 1, '图像生成模型', NOW(), NOW()),
(1, 4, '语音', 'audio', 'ai_model_type', NULL, 'default', 'N', 1, '语音模型', NOW(), NOW());

-- AI 状态字典数据
INSERT INTO sys_dict_data (tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, dict_status, remark, create_time, update_time)
VALUES
(1, 1, '正常', '0', 'ai_status', NULL, 'success', 'Y', 1, '正常状态', NOW(), NOW()),
(1, 2, '停用', '1', 'ai_status', NULL, 'error', 'N', 1, '停用状态', NOW(), NOW());

-- AI 是否默认字典数据
INSERT INTO sys_dict_data (tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, dict_status, remark, create_time, update_time)
VALUES
(1, 1, '否', '0', 'ai_is_default', NULL, 'default', 'Y', 1, '非默认', NOW(), NOW()),
(1, 2, '是', '1', 'ai_is_default', NULL, 'success', 'N', 1, '默认', NOW(), NOW());

-- =============================================
-- 7. ai_provider 表新增 logo 字段
-- =============================================

ALTER TABLE `ai_provider` ADD COLUMN `logo` VARCHAR(512) DEFAULT NULL COMMENT '供应商Logo(文件ID或URL)' AFTER `provider_type`;

-- =============================================
-- 8. 数据迁移：将 ai_provider.models JSON 迁移到 ai_model 表
--    models 字段格式为字符串数组 JSON，如 ["gpt-4o","gpt-4o-mini"]
-- =============================================

-- 迁移脚本（需根据实际 models JSON 格式调整）
-- 以下处理简单字符串数组格式 ["model1","model2"]
-- 对于每条 ai_provider 记录，解析 models JSON 并插入 ai_model

-- 注意：MySQL JSON 解析示例，仅适用于 ["str1","str2"] 格式
-- DELIMITER //
-- CREATE PROCEDURE IF NOT EXISTS migrate_ai_models()
-- BEGIN
--     DECLARE done INT DEFAULT FALSE;
--     DECLARE v_id BIGINT;
--     DECLARE v_models JSON;
--     DECLARE v_default_model VARCHAR(128);
--     DECLARE v_tenant_id BIGINT;
--     DECLARE v_i INT;
--     DECLARE v_model_count INT;
--     DECLARE v_model_value VARCHAR(128);
--
--     DECLARE cur CURSOR FOR SELECT id, models, default_model, tenant_id FROM ai_provider WHERE models IS NOT NULL AND models != '' AND models != '[]';
--     DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
--
--     OPEN cur;
--     read_loop: LOOP
--         FETCH cur INTO v_id, v_models, v_default_model, v_tenant_id;
--         IF done THEN LEAVE read_loop; END IF;
--
--         SET v_model_count = JSON_LENGTH(v_models);
--         SET v_i = 0;
--         WHILE v_i < v_model_count DO
--             SET v_model_value = JSON_UNQUOTE(JSON_EXTRACT(v_models, CONCAT('$[', v_i, ']')));
--             INSERT INTO ai_model (id, provider_id, model_type, model_id, model_name, is_default, status, sort_order, tenant_id, create_time, update_time)
--             VALUES (
--                 FLOOR(RAND() * 9000000000000000000 + 1000000000000000000),
--                 v_id,
--                 'chat',
--                 v_model_value,
--                 v_model_value,
--                 IF(v_model_value = v_default_model, '1', '0'),
--                 '0',
--                 v_i,
--                 v_tenant_id,
--                 NOW(),
--                 NOW()
--             );
--             SET v_i = v_i + 1;
--         END WHILE;
--     END LOOP;
--     CLOSE cur;
-- END //
-- DELIMITER ;
--
-- CALL migrate_ai_models();
-- DROP PROCEDURE IF EXISTS migrate_ai_models;
