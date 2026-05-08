-- =============================================
-- 流程模型版本管理功能 - 数据库初始化脚本
-- 创建时间：2026-05-08
-- 说明：创建版本历史表、新增重要性等级字段、新增字典数据
-- =============================================

-- 1. 创建版本历史表 sys_flow_model_version
CREATE TABLE `sys_flow_model_version`
(
    `id`                    varchar(64)  NOT NULL COMMENT '主键',
    `model_id`              varchar(64)  NOT NULL COMMENT '模型ID',
    `version`               int          NOT NULL COMMENT '版本号',
    `version_name`          varchar(100) NOT NULL COMMENT '版本名称',
    `version_tag`           varchar(20)  NOT NULL DEFAULT 'draft' COMMENT '版本标记（draft/test/release/deprecated）',
    `bpmn_xml`              text         NOT NULL COMMENT 'BPMN XML',
    `form_json`             text COMMENT '表单配置',
    `change_description`    varchar(500) COMMENT '变更说明',
    `deployment_id`         varchar(64) COMMENT '部署ID',
    `process_definition_id` varchar(64) COMMENT '流程定义ID',
    `publish_by`            varchar(64)  NOT NULL COMMENT '发布人',
    `publish_time`          datetime     NOT NULL COMMENT '发布时间',
    `tenant_id`             bigint       NOT NULL DEFAULT 1 COMMENT '租户ID',
    `create_time`           datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `del_flag`              int          NOT NULL DEFAULT 0 COMMENT '删除标志（0-正常/1-删除）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_model_version` (`model_id`, `version`),
    KEY                     `idx_model_id` (`model_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程模型版本历史表';


-- 2. 修改 sys_flow_model 表，新增重要性等级字段
ALTER TABLE `sys_flow_model`
    ADD COLUMN `importance_level` int NOT NULL DEFAULT 1 COMMENT '重要性等级（1-普通/2-重要）' AFTER `status`;


-- 3. 新增字典类型：版本标记（flow_version_tag）
INSERT INTO sys_dict_type (tenant_id, dict_name, dict_type, dict_status, remark, create_by, create_time, update_by, update_time, create_dept)
VALUES (1, '流程版本标记', 'flow_version_tag', 1, '流程版本状态标记', NULL, NOW(), NULL, NOW(), NULL);

-- 新增字典数据：版本标记选项
INSERT INTO sys_dict_data (tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, parent_dict_code, linked_dict_type, linked_dict_value, dict_status, remark, create_by, create_time, update_by, update_time, create_dept)
VALUES (1, 1, '草稿', 'draft', 'flow_version_tag', NULL, 'default', 'N', NULL, NULL, NULL, 1, '草稿版本', NULL, NOW(), NULL, NOW(), NULL);

INSERT INTO sys_dict_data (tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, parent_dict_code, linked_dict_type, linked_dict_value, dict_status, remark, create_by, create_time, update_by, update_time, create_dept)
VALUES (1, 2, '测试', 'test', 'flow_version_tag', NULL, 'info', 'N', NULL, NULL, NULL, 1, '测试版本', NULL, NOW(), NULL, NOW(), NULL);

INSERT INTO sys_dict_data (tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, parent_dict_code, linked_dict_type, linked_dict_value, dict_status, remark, create_by, create_time, update_by, update_time, create_dept)
VALUES (1, 3, '正式发布', 'release', 'flow_version_tag', NULL, 'success', 'Y', NULL, NULL, NULL, 1, '正式发布版本', NULL, NOW(), NULL, NOW(), NULL);

INSERT INTO sys_dict_data (tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, parent_dict_code, linked_dict_type, linked_dict_value, dict_status, remark, create_by, create_time, update_by, update_time, create_dept)
VALUES (1, 4, '已废弃', 'deprecated', 'flow_version_tag', NULL, 'warning', 'N', NULL, NULL, NULL, 1, '已废弃版本', NULL, NOW(), NULL, NOW(), NULL);


-- 4. 新增字典类型：流程重要性等级（flow_importance_level）
INSERT INTO sys_dict_type (tenant_id, dict_name, dict_type, dict_status, remark, create_by, create_time, update_by, update_time, create_dept)
VALUES (1, '流程重要性等级', 'flow_importance_level', 1, '流程重要性等级分类', NULL, NOW(), NULL, NOW(), NULL);

-- 新增字典数据：重要性等级选项
INSERT INTO sys_dict_data (tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, parent_dict_code, linked_dict_type, linked_dict_value, dict_status, remark, create_by, create_time, update_by, update_time, create_dept)
VALUES (1, 1, '普通流程', '1', 'flow_importance_level', NULL, 'default', 'Y', NULL, NULL, NULL, 1, '普通流程，模型管理员可回退', NULL, NOW(), NULL, NOW(), NULL);

INSERT INTO sys_dict_data (tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, parent_dict_code, linked_dict_type, linked_dict_value, dict_status, remark, create_by, create_time, update_by, update_time, create_dept)
VALUES (1, 2, '重要流程', '2', 'flow_importance_level', NULL, 'danger', 'N', NULL, NULL, NULL, 1, '重要流程，需超级管理员权限回退', NULL, NOW(), NULL, NOW(), NULL);


-- =============================================
-- 执行完成说明
-- 1. sys_flow_model_version 表创建成功
-- 2. sys_flow_model 表新增 importance_level 字段
-- 3. 字典类型和字典数据创建成功
-- =============================================


-- 5. 新增版本管理菜单和权限配置（sys_resource 表）
-- 注意：以下 SQL 需根据实际 sys_resource 表结构调整字段名

-- 新增版本管理菜单（在流程模型菜单下）
-- INSERT INTO sys_resource (resource_id, resource_name, resource_code, parent_code, resource_type, menu_url, menu_icon, sort, status, tenant_id, create_time, update_time)
-- VALUES ('flow_model_version', '版本历史', 'flow_model_version', 'flow_model', 'menu', '/flow/model/version', 'document', 3, 1, 1, NOW(), NOW());

-- 新增版本回退权限
-- INSERT INTO sys_resource (resource_id, resource_name, resource_code, parent_code, resource_type, permission_code, sort, status, tenant_id, create_time, update_time)
-- VALUES ('flow_model_revert', '版本回退', 'flow_model_revert', 'flow_model_version', 'button', 'flow:model:revert', 4, 1, 1, NOW(), NOW());

-- 新增版本删除权限
-- INSERT INTO sys_resource (resource_id, resource_name, resource_code, parent_code, resource_type, permission_code, sort, status, tenant_id, create_time, update_time)
-- VALUES ('flow_model_version_delete', '版本删除', 'flow_model_version_delete', 'flow_model_version', 'button', 'flow:model:version:delete', 5, 1, 1, NOW(), NOW());


-- =============================================
-- 权限配置说明
-- 权限标识已定义：
-- - flow:model:revert（版本回退权限）
-- - flow:model:version:delete（版本删除权限）
-- 需在系统菜单管理页面手动配置菜单和权限，或根据实际 sys_resource 表结构调整 SQL 后执行
-- =============================================