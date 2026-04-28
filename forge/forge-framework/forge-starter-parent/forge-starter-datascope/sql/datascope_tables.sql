-- 数据权限配置表
CREATE TABLE `sys_data_scope_config` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id` bigint NOT NULL DEFAULT '000000' COMMENT '租户编号',
    `resource_code` varchar(100) NOT NULL COMMENT '资源编码（对应接口路径或功能模块）',
    `resource_name` varchar(100) DEFAULT NULL COMMENT '资源名称',
    `mapper_method` varchar(200) NOT NULL COMMENT 'Mapper方法（如：com.example.mapper.UserMapper.selectList）',
    `table_alias` varchar(50) DEFAULT 't' COMMENT '主表别名',
    `user_id_column` varchar(50) DEFAULT 'user_id' COMMENT '用户ID字段名（简单模式：字段名；复杂模式：<sql>开头的SQL表达式）',
    `org_id_column` varchar(50) DEFAULT 'org_id' COMMENT '组织ID字段名（简单模式：字段名；复杂模式：<sql>开头的SQL表达式）',
    `tenant_id_column` varchar(50) DEFAULT 'tenant_id' COMMENT '租户ID字段名（简单模式：字段名；复杂模式：<sql>开头的SQL表达式）',
    `region_code_column` varchar(255) DEFAULT NULL COMMENT '行政区划字段名（用于 REGION 权限，简单字段或 <sql> 表达式，占位符 #{regionCode}/#{regionLevel}/#{regionAncestors}）',
    `user_region_column` varchar(100) DEFAULT NULL COMMENT '用户表行政区划字段名（可选，与 userTableAlias 配合做 OR 匹配）',
    `user_table_alias` varchar(50) DEFAULT NULL COMMENT '用户表别名（可选，配合 user_region_column）',
    `enabled` tinyint DEFAULT 1 COMMENT '是否启用（0-禁用，1-启用）',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    `create_by` bigint DEFAULT NULL COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` bigint DEFAULT NULL COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_mapper` (`tenant_id`,`mapper_method`),
    KEY `idx_resource_code` (`resource_code`),
    KEY `idx_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据权限配置表';

-- 角色-自定义数据权限关联表
CREATE TABLE `sys_role_data_scope` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id` bigint NOT NULL DEFAULT '000000' COMMENT '租户编号',
    `role_id` bigint NOT NULL COMMENT '角色ID',
    `org_id` bigint NOT NULL COMMENT '组织ID',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_org` (`tenant_id`,`role_id`,`org_id`),
    KEY `idx_role_id` (`role_id`),
    KEY `idx_org_id` (`org_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色-自定义数据权限关联表';

-- 初始化示例配置数据

-- 示例1：简单字段配置（普通场景）
INSERT INTO sys_data_scope_config (
    tenant_id, resource_code, resource_name, mapper_method, 
    table_alias, user_id_column, org_id_column, tenant_id_column, enabled, remark
) VALUES 
(000000, 'system:user:list', '用户列表查询', 'com.mdframe.forge.plugin.system.mapper.SysUserMapper.selectPage', 
    't', 'create_by', 'create_dept', 'tenant_id', 1, '用户列表数据权限控制'),
(000000, 'system:user:query', '用户详情查询', 'com.mdframe.forge.plugin.system.mapper.SysUserMapper.selectById', 
    't', 'create_by', 'create_dept', 'tenant_id', 1, '用户详情数据权限控制');

-- 示例2：复杂SQL表达式 - 法律案件查询（多角色OR关系）
-- 使用 <sql> 标签标识复杂SQL，支持 #{userId}、#{tenantId}、#{orgIds}、#{customOrgIds} 占位符
INSERT INTO sys_data_scope_config (
    tenant_id, resource_code, resource_name, mapper_method, 
    table_alias, user_id_column, org_id_column, tenant_id_column, enabled, remark
) VALUES (
    000000, 
    'legal:case:list', 
    '法律案件列表查询',
    'com.example.mapper.LegalCaseMapper.selectPage',
    'lc',
    '<sql>(lc.current_handler_id = #{userId} OR lc.register_person_id = #{userId} OR EXISTS (SELECT 1 FROM legal_case_flow lcf WHERE lcf.case_id = lc.id AND lcf.handler_id = #{userId}))',
    'dept_id',  -- 组织权限仍用简单字段
    'tenant_id', -- 租户权限仍用简单字段
    1,
    '案件查询：当前处理人、登记人或流程中的处理人均可查看'
);

-- 示例3：复杂SQL表达式 - 订单查询（包含子查询）
INSERT INTO sys_data_scope_config (
    tenant_id, resource_code, resource_name, mapper_method,
    table_alias, user_id_column, org_id_column, tenant_id_column, enabled, remark
) VALUES (
    000000,
    'order:list',
    '订单列表查询',
    'com.example.mapper.OrderMapper.selectPage',
    'o',
    '<sql>(o.user_id = #{userId} OR EXISTS (SELECT 1 FROM order_share os WHERE os.order_id = o.id AND os.share_user_id = #{userId}))',
    '<sql>o.dept_id IN (#{orgIds})',  -- 组织权限也可使用复杂SQL
    '<sql>o.tenant_id = #{tenantId}', -- 租户权限也可使用复杂SQL
    1,
    '订单查询：订单所有人或被分享人均可查看'
);

-- 示例4：混合配置（用户权限用复杂SQL，组织/租户权限用简单字段）
INSERT INTO sys_data_scope_config (
    tenant_id, resource_code, resource_name, mapper_method,
    table_alias, user_id_column, org_id_column, tenant_id_column, enabled, remark
) VALUES (
    000000,
    'customer:list',
    '客户列表查询',
    'com.example.mapper.CustomerMapper.selectPage',
    't',
    '<sql>(t.creator_id = #{userId} OR t.follower_id = #{userId})',  -- 用户权限用复杂SQL
    'dept_id',      -- 组织权限用简单字段
    'tenant_id',    -- 租户权限用简单字段
    1,
    '客户查询：创建人或跟进人均可查看'
);

-- ============================================================
-- 升级脚本：已存在 sys_data_scope_config 表时使用（幂等）
-- ============================================================
ALTER TABLE `sys_data_scope_config`
    ADD COLUMN IF NOT EXISTS `region_code_column` varchar(255) DEFAULT NULL COMMENT '行政区划字段名（用于 REGION 权限，简单字段或 <sql> 表达式，占位符 #{regionCode}/#{regionLevel}/#{regionAncestors}）' AFTER `tenant_id_column`,
    ADD COLUMN IF NOT EXISTS `user_region_column` varchar(100) DEFAULT NULL COMMENT '用户表行政区划字段名（可选，与 userTableAlias 配合做 OR 匹配）' AFTER `region_code_column`,
    ADD COLUMN IF NOT EXISTS `user_table_alias` varchar(50) DEFAULT NULL COMMENT '用户表别名（可选，配合 user_region_column）' AFTER `user_region_column`;

-- 示例5：行政区划数据权限 - 业务表按 area_code 过滤（简单模式）
-- 使用前提：对应角色的 data_scope 设为 7（REGION），且用户 LoginUser 有 regionCode/regionLevel
INSERT INTO sys_data_scope_config (
    tenant_id, resource_code, resource_name, mapper_method,
    table_alias, user_id_column, org_id_column, tenant_id_column,
    region_code_column, user_region_column, user_table_alias,
    enabled, remark
) VALUES (
    1,
    'business:archive:list',
    '档案列表查询（按行政区划）',
    'com.example.mapper.ArchiveMapper.selectPage',
    't', 'create_by', 'create_dept', 'tenant_id',
    'area_code', NULL, NULL,
    1,
    '档案数据按用户所属行政区划及下级区划过滤'
);

-- 示例6：行政区划数据权限 - 同时关联组织表和用户表的 area_code（JOIN 场景）
INSERT INTO sys_data_scope_config (
    tenant_id, resource_code, resource_name, mapper_method,
    table_alias, user_id_column, org_id_column, tenant_id_column,
    region_code_column, user_region_column, user_table_alias,
    enabled, remark
) VALUES (
    1,
    'business:task:list',
    '任务列表查询（行政区划+用户兜底）',
    'com.example.mapper.TaskMapper.selectPage',
    'd', 'd.create_by', 'd.dept_id', 'd.tenant_id',
    'area_code', 'area_code', 'u',
    1,
    '任务：优先按组织area_code，无组织用户按用户自身area_code（OR）'
);
