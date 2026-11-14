-- forge-admin: 业务表单配置引擎 + 平台对象种子
-- 可重复执行

CREATE TABLE IF NOT EXISTS `crm_biz_form_field` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户',
  `object_type` varchar(64) NOT NULL COMMENT '业务对象 PROJECT/CUSTOMER/...',
  `field_code` varchar(64) NOT NULL COMMENT '字段编码',
  `field_kind` varchar(16) NOT NULL COMMENT 'SYSTEM/CUSTOM',
  `field_label` varchar(100) NOT NULL COMMENT '显示名',
  `component_type` varchar(32) NOT NULL DEFAULT 'input' COMMENT 'input/textarea/number/date/select/dictSelect/customerSelect/section',
  `dict_type` varchar(64) DEFAULT NULL COMMENT '字典类型',
  `options_json` varchar(2000) DEFAULT NULL COMMENT '自定义选项 JSON',
  `section_code` varchar(64) DEFAULT NULL COMMENT '分组 basic/plan/supplement/extra',
  `locked` tinyint NOT NULL DEFAULT 0 COMMENT '系统锁定不可关闭',
  `default_enabled` tinyint NOT NULL DEFAULT 1 COMMENT '默认启用',
  `default_required` tinyint NOT NULL DEFAULT 0 COMMENT '默认必填',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '目录排序',
  `remark` varchar(500) DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `create_dept` bigint DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `del_flag` bigint NOT NULL DEFAULT 0 COMMENT '0未删/主键墓碑',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_crm_biz_form_field` (`tenant_id`,`object_type`,`field_code`,`del_flag`),
  KEY `idx_crm_biz_form_field_obj` (`tenant_id`,`object_type`,`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务表单字段目录';

CREATE TABLE IF NOT EXISTS `crm_biz_form_template` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户',
  `object_type` varchar(64) NOT NULL COMMENT '业务对象',
  `template_code` varchar(64) NOT NULL COMMENT '模版编码',
  `template_name` varchar(100) NOT NULL COMMENT '模版名称',
  `is_default` tinyint NOT NULL DEFAULT 1 COMMENT '是否默认模版',
  `form_enabled` tinyint NOT NULL DEFAULT 1 COMMENT '启用表单',
  `status` varchar(16) NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED',
  `version` int NOT NULL DEFAULT 1 COMMENT '乐观锁/方案版本',
  `remark` varchar(500) DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `create_dept` bigint DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `del_flag` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_crm_biz_form_tpl` (`tenant_id`,`object_type`,`template_code`,`del_flag`),
  KEY `idx_crm_biz_form_tpl_obj` (`tenant_id`,`object_type`,`del_flag`,`is_default`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务表单模版';

CREATE TABLE IF NOT EXISTS `crm_biz_form_template_field` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户',
  `template_id` bigint NOT NULL COMMENT '模版ID',
  `field_id` bigint NOT NULL COMMENT '字段目录ID',
  `enabled` tinyint NOT NULL DEFAULT 1 COMMENT '启用',
  `required` tinyint NOT NULL DEFAULT 0 COMMENT '必填',
  `list_visible` tinyint NOT NULL DEFAULT 0 COMMENT '列表显示',
  `list_sortable` tinyint NOT NULL DEFAULT 0 COMMENT '列表排序',
  `commonly_used` tinyint NOT NULL DEFAULT 0 COMMENT '常用',
  `display_label` varchar(100) DEFAULT NULL COMMENT '覆盖显示名',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '表单排序',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `create_dept` bigint DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `del_flag` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_crm_biz_form_tpl_field` (`tenant_id`,`template_id`,`field_id`,`del_flag`),
  KEY `idx_crm_biz_form_tpl_field_tpl` (`tenant_id`,`template_id`,`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务表单模版字段设置';

-- 表单配置平台化：侧表扩展值 + 系统配置权限
-- 可重复执行

CREATE TABLE IF NOT EXISTS `sys_biz_form_ext_data` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户',
  `object_type` varchar(64) NOT NULL COMMENT '对象 USER/ROLE/...',
  `biz_id` bigint NOT NULL COMMENT '业务主键',
  `ext_json` json DEFAULT NULL COMMENT '自定义扩展字段JSON',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `create_dept` bigint DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `del_flag` bigint NOT NULL DEFAULT 0 COMMENT '0未删/主键墓碑',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_biz_form_ext` (`tenant_id`,`object_type`,`biz_id`,`del_flag`),
  KEY `idx_sys_biz_form_ext_obj` (`tenant_id`,`object_type`,`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统对象表单扩展值';

-- 系统表单配置权限（挂系统管理根或用户菜单）
INSERT INTO sys_resource (
  tenant_id, resource_name, parent_id, resource_type, sort,
  is_external, open_target, is_public, menu_status, visible, perms,
  keep_alive, always_show, remark, create_by, create_time, update_by, update_time, create_dept, client_code
)
SELECT 1, '配置平台表单', COALESCE(menu.id, 0), 3, 91,
       0, '_self', 0, 1, 0, 'system:biz-form:edit',
       0, 0, '用户/角色/组织等平台表单字段配置', 1, NOW(), 1, NOW(), 1, 'pc'
FROM (SELECT id FROM sys_resource WHERE tenant_id=1 AND path='/system/user' AND client_code='pc' ORDER BY id LIMIT 1) menu
WHERE NOT EXISTS (
  SELECT 1 FROM sys_resource x WHERE x.tenant_id=1 AND x.perms='system:biz-form:edit' AND x.client_code='pc'
);

INSERT INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT 1, admin_role.id, resource.id, NOW()
FROM (SELECT id FROM sys_role WHERE tenant_id=1 AND role_key='admin' ORDER BY id LIMIT 1) admin_role
JOIN sys_resource resource ON resource.tenant_id=1 AND resource.perms='system:biz-form:edit' AND resource.client_code='pc'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_role_resource rr
  WHERE rr.tenant_id=1 AND rr.role_id=admin_role.id AND rr.resource_id=resource.id
);


-- ===== USER =====
INSERT INTO crm_biz_form_template
  (id, tenant_id, object_type, template_code, template_name, is_default, form_enabled, status, version,
   create_by, create_time, update_by, update_time, del_flag)
SELECT 3400000000002500000, 1, 'USER', 'default', '默认用户表单', 1, 1, 'ENABLED', 1,
       1, NOW(), 1, NOW(), 0
WHERE NOT EXISTS (
  SELECT 1 FROM crm_biz_form_template WHERE tenant_id=1 AND object_type='USER' AND template_code='default' AND del_flag=0
);

INSERT INTO crm_biz_form_field
  (id, tenant_id, object_type, field_code, field_kind, field_label, component_type, dict_type, section_code,
   locked, default_enabled, default_required, sort_order, create_by, create_time, update_by, update_time, del_flag)
SELECT seed.id, 1, seed.object_type, seed.field_code, 'SYSTEM', seed.field_label, seed.component_type, seed.dict_type, seed.section_code,
       seed.locked, seed.default_enabled, seed.default_required, seed.sort_order, 1, NOW(), 1, NOW(), 0
FROM (
  SELECT 3400000000002500001 id, 'USER' object_type, 'username' field_code, '用户账号' field_label, 'input' component_type, NULL dict_type, 'basic' section_code, 1 locked, 1 default_enabled, 1 default_required, 10 sort_order UNION ALL
  SELECT 3400000000002500002 id, 'USER' object_type, 'nickname' field_code, '用户昵称' field_label, 'input' component_type, NULL dict_type, 'basic' section_code, 0 locked, 1 default_enabled, 1 default_required, 20 sort_order UNION ALL
  SELECT 3400000000002500003 id, 'USER' object_type, 'phonenumber' field_code, '手机号' field_label, 'input' component_type, NULL dict_type, 'basic' section_code, 0 locked, 1 default_enabled, 0 default_required, 30 sort_order UNION ALL
  SELECT 3400000000002500004 id, 'USER' object_type, 'email' field_code, '邮箱' field_label, 'input' component_type, NULL dict_type, 'basic' section_code, 0 locked, 1 default_enabled, 0 default_required, 40 sort_order UNION ALL
  SELECT 3400000000002500005 id, 'USER' object_type, 'sex' field_code, '性别' field_label, 'dictSelect' component_type, 'sys_user_sex' dict_type, 'basic' section_code, 0 locked, 1 default_enabled, 0 default_required, 50 sort_order UNION ALL
  SELECT 3400000000002500006 id, 'USER' object_type, 'status' field_code, '状态' field_label, 'dictSelect' component_type, 'sys_user_status' dict_type, 'basic' section_code, 0 locked, 1 default_enabled, 0 default_required, 60 sort_order UNION ALL
  SELECT 3400000000002500007 id, 'USER' object_type, 'remark' field_code, '备注' field_label, 'textarea' component_type, NULL dict_type, 'extra' section_code, 0 locked, 1 default_enabled, 0 default_required, 90 sort_order
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM crm_biz_form_field f
  WHERE f.tenant_id=1 AND f.object_type=seed.object_type AND f.field_code=seed.field_code AND f.del_flag=0
);

INSERT INTO crm_biz_form_template_field
  (id, tenant_id, template_id, field_id, enabled, required, list_visible, list_sortable, commonly_used,
   display_label, sort_order, create_by, create_time, update_by, update_time, del_flag)
SELECT
  3400000000002500500 + f.sort_order, 1, t.id, f.id,
  f.default_enabled, f.default_required,
  CASE WHEN f.sort_order <= 30 THEN 1 ELSE 0 END,
  0, CASE WHEN f.section_code='basic' THEN 1 ELSE 0 END,
  NULL, f.sort_order, 1, NOW(), 1, NOW(), 0
FROM crm_biz_form_field f
JOIN crm_biz_form_template t ON t.tenant_id=1 AND t.object_type='USER' AND t.template_code='default' AND t.del_flag=0
WHERE f.tenant_id=1 AND f.object_type='USER' AND f.del_flag=0
  AND NOT EXISTS (
    SELECT 1 FROM crm_biz_form_template_field tf
    WHERE tf.tenant_id=1 AND tf.template_id=t.id AND tf.field_id=f.id AND tf.del_flag=0
  );

-- ===== ROLE =====
INSERT INTO crm_biz_form_template
  (id, tenant_id, object_type, template_code, template_name, is_default, form_enabled, status, version,
   create_by, create_time, update_by, update_time, del_flag)
SELECT 3400000000002600000, 1, 'ROLE', 'default', '默认角色表单', 1, 1, 'ENABLED', 1,
       1, NOW(), 1, NOW(), 0
WHERE NOT EXISTS (
  SELECT 1 FROM crm_biz_form_template WHERE tenant_id=1 AND object_type='ROLE' AND template_code='default' AND del_flag=0
);

INSERT INTO crm_biz_form_field
  (id, tenant_id, object_type, field_code, field_kind, field_label, component_type, dict_type, section_code,
   locked, default_enabled, default_required, sort_order, create_by, create_time, update_by, update_time, del_flag)
SELECT seed.id, 1, seed.object_type, seed.field_code, 'SYSTEM', seed.field_label, seed.component_type, seed.dict_type, seed.section_code,
       seed.locked, seed.default_enabled, seed.default_required, seed.sort_order, 1, NOW(), 1, NOW(), 0
FROM (
  SELECT 3400000000002600001 id, 'ROLE' object_type, 'roleName' field_code, '角色名称' field_label, 'input' component_type, NULL dict_type, 'basic' section_code, 1 locked, 1 default_enabled, 1 default_required, 10 sort_order UNION ALL
  SELECT 3400000000002600002 id, 'ROLE' object_type, 'roleKey' field_code, '权限字符' field_label, 'input' component_type, NULL dict_type, 'basic' section_code, 1 locked, 1 default_enabled, 1 default_required, 20 sort_order UNION ALL
  SELECT 3400000000002600003 id, 'ROLE' object_type, 'roleSort' field_code, '显示顺序' field_label, 'number' component_type, NULL dict_type, 'basic' section_code, 0 locked, 1 default_enabled, 0 default_required, 30 sort_order UNION ALL
  SELECT 3400000000002600004 id, 'ROLE' object_type, 'status' field_code, '状态' field_label, 'dictSelect' component_type, 'sys_normal_disable' dict_type, 'basic' section_code, 0 locked, 1 default_enabled, 0 default_required, 40 sort_order UNION ALL
  SELECT 3400000000002600005 id, 'ROLE' object_type, 'remark' field_code, '备注' field_label, 'textarea' component_type, NULL dict_type, 'extra' section_code, 0 locked, 1 default_enabled, 0 default_required, 90 sort_order
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM crm_biz_form_field f
  WHERE f.tenant_id=1 AND f.object_type=seed.object_type AND f.field_code=seed.field_code AND f.del_flag=0
);

INSERT INTO crm_biz_form_template_field
  (id, tenant_id, template_id, field_id, enabled, required, list_visible, list_sortable, commonly_used,
   display_label, sort_order, create_by, create_time, update_by, update_time, del_flag)
SELECT
  3400000000002600500 + f.sort_order, 1, t.id, f.id,
  f.default_enabled, f.default_required,
  CASE WHEN f.sort_order <= 30 THEN 1 ELSE 0 END,
  0, CASE WHEN f.section_code='basic' THEN 1 ELSE 0 END,
  NULL, f.sort_order, 1, NOW(), 1, NOW(), 0
FROM crm_biz_form_field f
JOIN crm_biz_form_template t ON t.tenant_id=1 AND t.object_type='ROLE' AND t.template_code='default' AND t.del_flag=0
WHERE f.tenant_id=1 AND f.object_type='ROLE' AND f.del_flag=0
  AND NOT EXISTS (
    SELECT 1 FROM crm_biz_form_template_field tf
    WHERE tf.tenant_id=1 AND tf.template_id=t.id AND tf.field_id=f.id AND tf.del_flag=0
  );

-- ===== ORG =====
INSERT INTO crm_biz_form_template
  (id, tenant_id, object_type, template_code, template_name, is_default, form_enabled, status, version,
   create_by, create_time, update_by, update_time, del_flag)
SELECT 3400000000002700000, 1, 'ORG', 'default', '默认组织表单', 1, 1, 'ENABLED', 1,
       1, NOW(), 1, NOW(), 0
WHERE NOT EXISTS (
  SELECT 1 FROM crm_biz_form_template WHERE tenant_id=1 AND object_type='ORG' AND template_code='default' AND del_flag=0
);

INSERT INTO crm_biz_form_field
  (id, tenant_id, object_type, field_code, field_kind, field_label, component_type, dict_type, section_code,
   locked, default_enabled, default_required, sort_order, create_by, create_time, update_by, update_time, del_flag)
SELECT seed.id, 1, seed.object_type, seed.field_code, 'SYSTEM', seed.field_label, seed.component_type, seed.dict_type, seed.section_code,
       seed.locked, seed.default_enabled, seed.default_required, seed.sort_order, 1, NOW(), 1, NOW(), 0
FROM (
  SELECT 3400000000002700001 id, 'ORG' object_type, 'orgName' field_code, '组织名称' field_label, 'input' component_type, NULL dict_type, 'basic' section_code, 1 locked, 1 default_enabled, 1 default_required, 10 sort_order UNION ALL
  SELECT 3400000000002700002 id, 'ORG' object_type, 'orgType' field_code, '组织类型' field_label, 'dictSelect' component_type, 'sys_org_type' dict_type, 'basic' section_code, 0 locked, 1 default_enabled, 0 default_required, 20 sort_order UNION ALL
  SELECT 3400000000002700003 id, 'ORG' object_type, 'orderNum' field_code, '排序' field_label, 'number' component_type, NULL dict_type, 'basic' section_code, 0 locked, 1 default_enabled, 0 default_required, 30 sort_order UNION ALL
  SELECT 3400000000002700004 id, 'ORG' object_type, 'status' field_code, '状态' field_label, 'dictSelect' component_type, 'sys_normal_disable' dict_type, 'basic' section_code, 0 locked, 1 default_enabled, 0 default_required, 40 sort_order
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM crm_biz_form_field f
  WHERE f.tenant_id=1 AND f.object_type=seed.object_type AND f.field_code=seed.field_code AND f.del_flag=0
);

INSERT INTO crm_biz_form_template_field
  (id, tenant_id, template_id, field_id, enabled, required, list_visible, list_sortable, commonly_used,
   display_label, sort_order, create_by, create_time, update_by, update_time, del_flag)
SELECT
  3400000000002700500 + f.sort_order, 1, t.id, f.id,
  f.default_enabled, f.default_required,
  CASE WHEN f.sort_order <= 30 THEN 1 ELSE 0 END,
  0, CASE WHEN f.section_code='basic' THEN 1 ELSE 0 END,
  NULL, f.sort_order, 1, NOW(), 1, NOW(), 0
FROM crm_biz_form_field f
JOIN crm_biz_form_template t ON t.tenant_id=1 AND t.object_type='ORG' AND t.template_code='default' AND t.del_flag=0
WHERE f.tenant_id=1 AND f.object_type='ORG' AND f.del_flag=0
  AND NOT EXISTS (
    SELECT 1 FROM crm_biz_form_template_field tf
    WHERE tf.tenant_id=1 AND tf.template_id=t.id AND tf.field_id=f.id AND tf.del_flag=0
  );

-- ===== POST =====
INSERT INTO crm_biz_form_template
  (id, tenant_id, object_type, template_code, template_name, is_default, form_enabled, status, version,
   create_by, create_time, update_by, update_time, del_flag)
SELECT 3400000000002800000, 1, 'POST', 'default', '默认岗位表单', 1, 1, 'ENABLED', 1,
       1, NOW(), 1, NOW(), 0
WHERE NOT EXISTS (
  SELECT 1 FROM crm_biz_form_template WHERE tenant_id=1 AND object_type='POST' AND template_code='default' AND del_flag=0
);

INSERT INTO crm_biz_form_field
  (id, tenant_id, object_type, field_code, field_kind, field_label, component_type, dict_type, section_code,
   locked, default_enabled, default_required, sort_order, create_by, create_time, update_by, update_time, del_flag)
SELECT seed.id, 1, seed.object_type, seed.field_code, 'SYSTEM', seed.field_label, seed.component_type, seed.dict_type, seed.section_code,
       seed.locked, seed.default_enabled, seed.default_required, seed.sort_order, 1, NOW(), 1, NOW(), 0
FROM (
  SELECT 3400000000002800001 id, 'POST' object_type, 'postName' field_code, '岗位名称' field_label, 'input' component_type, NULL dict_type, 'basic' section_code, 1 locked, 1 default_enabled, 1 default_required, 10 sort_order UNION ALL
  SELECT 3400000000002800002 id, 'POST' object_type, 'postCode' field_code, '岗位编码' field_label, 'input' component_type, NULL dict_type, 'basic' section_code, 0 locked, 1 default_enabled, 1 default_required, 20 sort_order UNION ALL
  SELECT 3400000000002800003 id, 'POST' object_type, 'postSort' field_code, '排序' field_label, 'number' component_type, NULL dict_type, 'basic' section_code, 0 locked, 1 default_enabled, 0 default_required, 30 sort_order UNION ALL
  SELECT 3400000000002800004 id, 'POST' object_type, 'status' field_code, '状态' field_label, 'dictSelect' component_type, 'sys_normal_disable' dict_type, 'basic' section_code, 0 locked, 1 default_enabled, 0 default_required, 40 sort_order
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM crm_biz_form_field f
  WHERE f.tenant_id=1 AND f.object_type=seed.object_type AND f.field_code=seed.field_code AND f.del_flag=0
);

INSERT INTO crm_biz_form_template_field
  (id, tenant_id, template_id, field_id, enabled, required, list_visible, list_sortable, commonly_used,
   display_label, sort_order, create_by, create_time, update_by, update_time, del_flag)
SELECT
  3400000000002800500 + f.sort_order, 1, t.id, f.id,
  f.default_enabled, f.default_required,
  CASE WHEN f.sort_order <= 30 THEN 1 ELSE 0 END,
  0, CASE WHEN f.section_code='basic' THEN 1 ELSE 0 END,
  NULL, f.sort_order, 1, NOW(), 1, NOW(), 0
FROM crm_biz_form_field f
JOIN crm_biz_form_template t ON t.tenant_id=1 AND t.object_type='POST' AND t.template_code='default' AND t.del_flag=0
WHERE f.tenant_id=1 AND f.object_type='POST' AND f.del_flag=0
  AND NOT EXISTS (
    SELECT 1 FROM crm_biz_form_template_field tf
    WHERE tf.tenant_id=1 AND tf.template_id=t.id AND tf.field_id=f.id AND tf.del_flag=0
  );

-- ===== DICT_TYPE =====
INSERT INTO crm_biz_form_template
  (id, tenant_id, object_type, template_code, template_name, is_default, form_enabled, status, version,
   create_by, create_time, update_by, update_time, del_flag)
SELECT 3400000000002900000, 1, 'DICT_TYPE', 'default', '默认字典类型表单', 1, 1, 'ENABLED', 1,
       1, NOW(), 1, NOW(), 0
WHERE NOT EXISTS (
  SELECT 1 FROM crm_biz_form_template WHERE tenant_id=1 AND object_type='DICT_TYPE' AND template_code='default' AND del_flag=0
);

INSERT INTO crm_biz_form_field
  (id, tenant_id, object_type, field_code, field_kind, field_label, component_type, dict_type, section_code,
   locked, default_enabled, default_required, sort_order, create_by, create_time, update_by, update_time, del_flag)
SELECT seed.id, 1, seed.object_type, seed.field_code, 'SYSTEM', seed.field_label, seed.component_type, seed.dict_type, seed.section_code,
       seed.locked, seed.default_enabled, seed.default_required, seed.sort_order, 1, NOW(), 1, NOW(), 0
FROM (
  SELECT 3400000000002900001 id, 'DICT_TYPE' object_type, 'dictName' field_code, '字典名称' field_label, 'input' component_type, NULL dict_type, 'basic' section_code, 1 locked, 1 default_enabled, 1 default_required, 10 sort_order UNION ALL
  SELECT 3400000000002900002 id, 'DICT_TYPE' object_type, 'dictType' field_code, '字典类型' field_label, 'input' component_type, NULL dict_type, 'basic' section_code, 1 locked, 1 default_enabled, 1 default_required, 20 sort_order UNION ALL
  SELECT 3400000000002900003 id, 'DICT_TYPE' object_type, 'status' field_code, '状态' field_label, 'dictSelect' component_type, 'sys_normal_disable' dict_type, 'basic' section_code, 0 locked, 1 default_enabled, 0 default_required, 30 sort_order UNION ALL
  SELECT 3400000000002900004 id, 'DICT_TYPE' object_type, 'remark' field_code, '备注' field_label, 'textarea' component_type, NULL dict_type, 'extra' section_code, 0 locked, 1 default_enabled, 0 default_required, 90 sort_order
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM crm_biz_form_field f
  WHERE f.tenant_id=1 AND f.object_type=seed.object_type AND f.field_code=seed.field_code AND f.del_flag=0
);

INSERT INTO crm_biz_form_template_field
  (id, tenant_id, template_id, field_id, enabled, required, list_visible, list_sortable, commonly_used,
   display_label, sort_order, create_by, create_time, update_by, update_time, del_flag)
SELECT
  3400000000002900500 + f.sort_order, 1, t.id, f.id,
  f.default_enabled, f.default_required,
  CASE WHEN f.sort_order <= 30 THEN 1 ELSE 0 END,
  0, CASE WHEN f.section_code='basic' THEN 1 ELSE 0 END,
  NULL, f.sort_order, 1, NOW(), 1, NOW(), 0
FROM crm_biz_form_field f
JOIN crm_biz_form_template t ON t.tenant_id=1 AND t.object_type='DICT_TYPE' AND t.template_code='default' AND t.del_flag=0
WHERE f.tenant_id=1 AND f.object_type='DICT_TYPE' AND f.del_flag=0
  AND NOT EXISTS (
    SELECT 1 FROM crm_biz_form_template_field tf
    WHERE tf.tenant_id=1 AND tf.template_id=t.id AND tf.field_id=f.id AND tf.del_flag=0
  );

-- ===== DICT_DATA =====
INSERT INTO crm_biz_form_template
  (id, tenant_id, object_type, template_code, template_name, is_default, form_enabled, status, version,
   create_by, create_time, update_by, update_time, del_flag)
SELECT 3400000000003000000, 1, 'DICT_DATA', 'default', '默认字典数据表单', 1, 1, 'ENABLED', 1,
       1, NOW(), 1, NOW(), 0
WHERE NOT EXISTS (
  SELECT 1 FROM crm_biz_form_template WHERE tenant_id=1 AND object_type='DICT_DATA' AND template_code='default' AND del_flag=0
);

INSERT INTO crm_biz_form_field
  (id, tenant_id, object_type, field_code, field_kind, field_label, component_type, dict_type, section_code,
   locked, default_enabled, default_required, sort_order, create_by, create_time, update_by, update_time, del_flag)
SELECT seed.id, 1, seed.object_type, seed.field_code, 'SYSTEM', seed.field_label, seed.component_type, seed.dict_type, seed.section_code,
       seed.locked, seed.default_enabled, seed.default_required, seed.sort_order, 1, NOW(), 1, NOW(), 0
FROM (
  SELECT 3400000000003000001 id, 'DICT_DATA' object_type, 'dictLabel' field_code, '字典标签' field_label, 'input' component_type, NULL dict_type, 'basic' section_code, 1 locked, 1 default_enabled, 1 default_required, 10 sort_order UNION ALL
  SELECT 3400000000003000002 id, 'DICT_DATA' object_type, 'dictValue' field_code, '字典键值' field_label, 'input' component_type, NULL dict_type, 'basic' section_code, 1 locked, 1 default_enabled, 1 default_required, 20 sort_order UNION ALL
  SELECT 3400000000003000003 id, 'DICT_DATA' object_type, 'dictSort' field_code, '排序' field_label, 'number' component_type, NULL dict_type, 'basic' section_code, 0 locked, 1 default_enabled, 0 default_required, 30 sort_order UNION ALL
  SELECT 3400000000003000004 id, 'DICT_DATA' object_type, 'status' field_code, '状态' field_label, 'dictSelect' component_type, 'sys_normal_disable' dict_type, 'basic' section_code, 0 locked, 1 default_enabled, 0 default_required, 40 sort_order
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM crm_biz_form_field f
  WHERE f.tenant_id=1 AND f.object_type=seed.object_type AND f.field_code=seed.field_code AND f.del_flag=0
);

INSERT INTO crm_biz_form_template_field
  (id, tenant_id, template_id, field_id, enabled, required, list_visible, list_sortable, commonly_used,
   display_label, sort_order, create_by, create_time, update_by, update_time, del_flag)
SELECT
  3400000000003000500 + f.sort_order, 1, t.id, f.id,
  f.default_enabled, f.default_required,
  CASE WHEN f.sort_order <= 30 THEN 1 ELSE 0 END,
  0, CASE WHEN f.section_code='basic' THEN 1 ELSE 0 END,
  NULL, f.sort_order, 1, NOW(), 1, NOW(), 0
FROM crm_biz_form_field f
JOIN crm_biz_form_template t ON t.tenant_id=1 AND t.object_type='DICT_DATA' AND t.template_code='default' AND t.del_flag=0
WHERE f.tenant_id=1 AND f.object_type='DICT_DATA' AND f.del_flag=0
  AND NOT EXISTS (
    SELECT 1 FROM crm_biz_form_template_field tf
    WHERE tf.tenant_id=1 AND tf.template_id=t.id AND tf.field_id=f.id AND tf.del_flag=0
  );

-- ===== NOTICE =====
INSERT INTO crm_biz_form_template
  (id, tenant_id, object_type, template_code, template_name, is_default, form_enabled, status, version,
   create_by, create_time, update_by, update_time, del_flag)
SELECT 3400000000003100000, 1, 'NOTICE', 'default', '默认通知表单', 1, 1, 'ENABLED', 1,
       1, NOW(), 1, NOW(), 0
WHERE NOT EXISTS (
  SELECT 1 FROM crm_biz_form_template WHERE tenant_id=1 AND object_type='NOTICE' AND template_code='default' AND del_flag=0
);

INSERT INTO crm_biz_form_field
  (id, tenant_id, object_type, field_code, field_kind, field_label, component_type, dict_type, section_code,
   locked, default_enabled, default_required, sort_order, create_by, create_time, update_by, update_time, del_flag)
SELECT seed.id, 1, seed.object_type, seed.field_code, 'SYSTEM', seed.field_label, seed.component_type, seed.dict_type, seed.section_code,
       seed.locked, seed.default_enabled, seed.default_required, seed.sort_order, 1, NOW(), 1, NOW(), 0
FROM (
  SELECT 3400000000003100001 id, 'NOTICE' object_type, 'noticeTitle' field_code, '公告标题' field_label, 'input' component_type, NULL dict_type, 'basic' section_code, 1 locked, 1 default_enabled, 1 default_required, 10 sort_order UNION ALL
  SELECT 3400000000003100002 id, 'NOTICE' object_type, 'noticeType' field_code, '公告类型' field_label, 'dictSelect' component_type, 'sys_notice_type' dict_type, 'basic' section_code, 0 locked, 1 default_enabled, 1 default_required, 20 sort_order UNION ALL
  SELECT 3400000000003100003 id, 'NOTICE' object_type, 'status' field_code, '状态' field_label, 'dictSelect' component_type, 'sys_notice_status' dict_type, 'basic' section_code, 0 locked, 1 default_enabled, 0 default_required, 30 sort_order
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM crm_biz_form_field f
  WHERE f.tenant_id=1 AND f.object_type=seed.object_type AND f.field_code=seed.field_code AND f.del_flag=0
);

INSERT INTO crm_biz_form_template_field
  (id, tenant_id, template_id, field_id, enabled, required, list_visible, list_sortable, commonly_used,
   display_label, sort_order, create_by, create_time, update_by, update_time, del_flag)
SELECT
  3400000000003100500 + f.sort_order, 1, t.id, f.id,
  f.default_enabled, f.default_required,
  CASE WHEN f.sort_order <= 30 THEN 1 ELSE 0 END,
  0, CASE WHEN f.section_code='basic' THEN 1 ELSE 0 END,
  NULL, f.sort_order, 1, NOW(), 1, NOW(), 0
FROM crm_biz_form_field f
JOIN crm_biz_form_template t ON t.tenant_id=1 AND t.object_type='NOTICE' AND t.template_code='default' AND t.del_flag=0
WHERE f.tenant_id=1 AND f.object_type='NOTICE' AND f.del_flag=0
  AND NOT EXISTS (
    SELECT 1 FROM crm_biz_form_template_field tf
    WHERE tf.tenant_id=1 AND tf.template_id=t.id AND tf.field_id=f.id AND tf.del_flag=0
  );

-- ===== CLIENT =====
INSERT INTO crm_biz_form_template
  (id, tenant_id, object_type, template_code, template_name, is_default, form_enabled, status, version,
   create_by, create_time, update_by, update_time, del_flag)
SELECT 3400000000003200000, 1, 'CLIENT', 'default', '默认客户端表单', 1, 1, 'ENABLED', 1,
       1, NOW(), 1, NOW(), 0
WHERE NOT EXISTS (
  SELECT 1 FROM crm_biz_form_template WHERE tenant_id=1 AND object_type='CLIENT' AND template_code='default' AND del_flag=0
);

INSERT INTO crm_biz_form_field
  (id, tenant_id, object_type, field_code, field_kind, field_label, component_type, dict_type, section_code,
   locked, default_enabled, default_required, sort_order, create_by, create_time, update_by, update_time, del_flag)
SELECT seed.id, 1, seed.object_type, seed.field_code, 'SYSTEM', seed.field_label, seed.component_type, seed.dict_type, seed.section_code,
       seed.locked, seed.default_enabled, seed.default_required, seed.sort_order, 1, NOW(), 1, NOW(), 0
FROM (
  SELECT 3400000000003200001 id, 'CLIENT' object_type, 'clientName' field_code, '客户端名称' field_label, 'input' component_type, NULL dict_type, 'basic' section_code, 1 locked, 1 default_enabled, 1 default_required, 10 sort_order UNION ALL
  SELECT 3400000000003200002 id, 'CLIENT' object_type, 'clientId' field_code, '客户端ID' field_label, 'input' component_type, NULL dict_type, 'basic' section_code, 1 locked, 1 default_enabled, 1 default_required, 20 sort_order UNION ALL
  SELECT 3400000000003200003 id, 'CLIENT' object_type, 'status' field_code, '状态' field_label, 'dictSelect' component_type, 'sys_normal_disable' dict_type, 'basic' section_code, 0 locked, 1 default_enabled, 0 default_required, 30 sort_order
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM crm_biz_form_field f
  WHERE f.tenant_id=1 AND f.object_type=seed.object_type AND f.field_code=seed.field_code AND f.del_flag=0
);

INSERT INTO crm_biz_form_template_field
  (id, tenant_id, template_id, field_id, enabled, required, list_visible, list_sortable, commonly_used,
   display_label, sort_order, create_by, create_time, update_by, update_time, del_flag)
SELECT
  3400000000003200500 + f.sort_order, 1, t.id, f.id,
  f.default_enabled, f.default_required,
  CASE WHEN f.sort_order <= 30 THEN 1 ELSE 0 END,
  0, CASE WHEN f.section_code='basic' THEN 1 ELSE 0 END,
  NULL, f.sort_order, 1, NOW(), 1, NOW(), 0
FROM crm_biz_form_field f
JOIN crm_biz_form_template t ON t.tenant_id=1 AND t.object_type='CLIENT' AND t.template_code='default' AND t.del_flag=0
WHERE f.tenant_id=1 AND f.object_type='CLIENT' AND f.del_flag=0
  AND NOT EXISTS (
    SELECT 1 FROM crm_biz_form_template_field tf
    WHERE tf.tenant_id=1 AND tf.template_id=t.id AND tf.field_id=f.id AND tf.del_flag=0
  );

-- ===== TENANT =====
INSERT INTO crm_biz_form_template
  (id, tenant_id, object_type, template_code, template_name, is_default, form_enabled, status, version,
   create_by, create_time, update_by, update_time, del_flag)
SELECT 3400000000003300000, 1, 'TENANT', 'default', '默认租户表单', 1, 1, 'ENABLED', 1,
       1, NOW(), 1, NOW(), 0
WHERE NOT EXISTS (
  SELECT 1 FROM crm_biz_form_template WHERE tenant_id=1 AND object_type='TENANT' AND template_code='default' AND del_flag=0
);

INSERT INTO crm_biz_form_field
  (id, tenant_id, object_type, field_code, field_kind, field_label, component_type, dict_type, section_code,
   locked, default_enabled, default_required, sort_order, create_by, create_time, update_by, update_time, del_flag)
SELECT seed.id, 1, seed.object_type, seed.field_code, 'SYSTEM', seed.field_label, seed.component_type, seed.dict_type, seed.section_code,
       seed.locked, seed.default_enabled, seed.default_required, seed.sort_order, 1, NOW(), 1, NOW(), 0
FROM (
  SELECT 3400000000003300001 id, 'TENANT' object_type, 'tenantName' field_code, '租户名称' field_label, 'input' component_type, NULL dict_type, 'basic' section_code, 1 locked, 1 default_enabled, 1 default_required, 10 sort_order UNION ALL
  SELECT 3400000000003300002 id, 'TENANT' object_type, 'tenantCode' field_code, '租户编码' field_label, 'input' component_type, NULL dict_type, 'basic' section_code, 1 locked, 1 default_enabled, 1 default_required, 20 sort_order UNION ALL
  SELECT 3400000000003300003 id, 'TENANT' object_type, 'status' field_code, '状态' field_label, 'dictSelect' component_type, 'sys_normal_disable' dict_type, 'basic' section_code, 0 locked, 1 default_enabled, 0 default_required, 30 sort_order
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM crm_biz_form_field f
  WHERE f.tenant_id=1 AND f.object_type=seed.object_type AND f.field_code=seed.field_code AND f.del_flag=0
);

INSERT INTO crm_biz_form_template_field
  (id, tenant_id, template_id, field_id, enabled, required, list_visible, list_sortable, commonly_used,
   display_label, sort_order, create_by, create_time, update_by, update_time, del_flag)
SELECT
  3400000000003300500 + f.sort_order, 1, t.id, f.id,
  f.default_enabled, f.default_required,
  CASE WHEN f.sort_order <= 30 THEN 1 ELSE 0 END,
  0, CASE WHEN f.section_code='basic' THEN 1 ELSE 0 END,
  NULL, f.sort_order, 1, NOW(), 1, NOW(), 0
FROM crm_biz_form_field f
JOIN crm_biz_form_template t ON t.tenant_id=1 AND t.object_type='TENANT' AND t.template_code='default' AND t.del_flag=0
WHERE f.tenant_id=1 AND f.object_type='TENANT' AND f.del_flag=0
  AND NOT EXISTS (
    SELECT 1 FROM crm_biz_form_template_field tf
    WHERE tf.tenant_id=1 AND tf.template_id=t.id AND tf.field_id=f.id AND tf.del_flag=0
  );

-- ===== CONFIG =====
INSERT INTO crm_biz_form_template
  (id, tenant_id, object_type, template_code, template_name, is_default, form_enabled, status, version,
   create_by, create_time, update_by, update_time, del_flag)
SELECT 3400000000003400000, 1, 'CONFIG', 'default', '默认参数配置表单', 1, 1, 'ENABLED', 1,
       1, NOW(), 1, NOW(), 0
WHERE NOT EXISTS (
  SELECT 1 FROM crm_biz_form_template WHERE tenant_id=1 AND object_type='CONFIG' AND template_code='default' AND del_flag=0
);

INSERT INTO crm_biz_form_field
  (id, tenant_id, object_type, field_code, field_kind, field_label, component_type, dict_type, section_code,
   locked, default_enabled, default_required, sort_order, create_by, create_time, update_by, update_time, del_flag)
SELECT seed.id, 1, seed.object_type, seed.field_code, 'SYSTEM', seed.field_label, seed.component_type, seed.dict_type, seed.section_code,
       seed.locked, seed.default_enabled, seed.default_required, seed.sort_order, 1, NOW(), 1, NOW(), 0
FROM (
  SELECT 3400000000003400001 id, 'CONFIG' object_type, 'configName' field_code, '参数名称' field_label, 'input' component_type, NULL dict_type, 'basic' section_code, 1 locked, 1 default_enabled, 1 default_required, 10 sort_order UNION ALL
  SELECT 3400000000003400002 id, 'CONFIG' object_type, 'configKey' field_code, '参数键名' field_label, 'input' component_type, NULL dict_type, 'basic' section_code, 1 locked, 1 default_enabled, 1 default_required, 20 sort_order UNION ALL
  SELECT 3400000000003400003 id, 'CONFIG' object_type, 'configValue' field_code, '参数键值' field_label, 'input' component_type, NULL dict_type, 'basic' section_code, 0 locked, 1 default_enabled, 0 default_required, 30 sort_order
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM crm_biz_form_field f
  WHERE f.tenant_id=1 AND f.object_type=seed.object_type AND f.field_code=seed.field_code AND f.del_flag=0
);

INSERT INTO crm_biz_form_template_field
  (id, tenant_id, template_id, field_id, enabled, required, list_visible, list_sortable, commonly_used,
   display_label, sort_order, create_by, create_time, update_by, update_time, del_flag)
SELECT
  3400000000003400500 + f.sort_order, 1, t.id, f.id,
  f.default_enabled, f.default_required,
  CASE WHEN f.sort_order <= 30 THEN 1 ELSE 0 END,
  0, CASE WHEN f.section_code='basic' THEN 1 ELSE 0 END,
  NULL, f.sort_order, 1, NOW(), 1, NOW(), 0
FROM crm_biz_form_field f
JOIN crm_biz_form_template t ON t.tenant_id=1 AND t.object_type='CONFIG' AND t.template_code='default' AND t.del_flag=0
WHERE f.tenant_id=1 AND f.object_type='CONFIG' AND f.del_flag=0
  AND NOT EXISTS (
    SELECT 1 FROM crm_biz_form_template_field tf
    WHERE tf.tenant_id=1 AND tf.template_id=t.id AND tf.field_id=f.id AND tf.del_flag=0
  );

-- ===== STORAGE_CONFIG =====
INSERT INTO crm_biz_form_template
  (id, tenant_id, object_type, template_code, template_name, is_default, form_enabled, status, version,
   create_by, create_time, update_by, update_time, del_flag)
SELECT 3400000000003500000, 1, 'STORAGE_CONFIG', 'default', '默认存储配置表单', 1, 1, 'ENABLED', 1,
       1, NOW(), 1, NOW(), 0
WHERE NOT EXISTS (
  SELECT 1 FROM crm_biz_form_template WHERE tenant_id=1 AND object_type='STORAGE_CONFIG' AND template_code='default' AND del_flag=0
);

INSERT INTO crm_biz_form_field
  (id, tenant_id, object_type, field_code, field_kind, field_label, component_type, dict_type, section_code,
   locked, default_enabled, default_required, sort_order, create_by, create_time, update_by, update_time, del_flag)
SELECT seed.id, 1, seed.object_type, seed.field_code, 'SYSTEM', seed.field_label, seed.component_type, seed.dict_type, seed.section_code,
       seed.locked, seed.default_enabled, seed.default_required, seed.sort_order, 1, NOW(), 1, NOW(), 0
FROM (
  SELECT 3400000000003500001 id, 'STORAGE_CONFIG' object_type, 'configName' field_code, '配置名称' field_label, 'input' component_type, NULL dict_type, 'basic' section_code, 1 locked, 1 default_enabled, 1 default_required, 10 sort_order UNION ALL
  SELECT 3400000000003500002 id, 'STORAGE_CONFIG' object_type, 'storageType' field_code, '存储类型' field_label, 'dictSelect' component_type, 'sys_file_storage_type' dict_type, 'basic' section_code, 0 locked, 1 default_enabled, 1 default_required, 20 sort_order
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM crm_biz_form_field f
  WHERE f.tenant_id=1 AND f.object_type=seed.object_type AND f.field_code=seed.field_code AND f.del_flag=0
);

INSERT INTO crm_biz_form_template_field
  (id, tenant_id, template_id, field_id, enabled, required, list_visible, list_sortable, commonly_used,
   display_label, sort_order, create_by, create_time, update_by, update_time, del_flag)
SELECT
  3400000000003500500 + f.sort_order, 1, t.id, f.id,
  f.default_enabled, f.default_required,
  CASE WHEN f.sort_order <= 30 THEN 1 ELSE 0 END,
  0, CASE WHEN f.section_code='basic' THEN 1 ELSE 0 END,
  NULL, f.sort_order, 1, NOW(), 1, NOW(), 0
FROM crm_biz_form_field f
JOIN crm_biz_form_template t ON t.tenant_id=1 AND t.object_type='STORAGE_CONFIG' AND t.template_code='default' AND t.del_flag=0
WHERE f.tenant_id=1 AND f.object_type='STORAGE_CONFIG' AND f.del_flag=0
  AND NOT EXISTS (
    SELECT 1 FROM crm_biz_form_template_field tf
    WHERE tf.tenant_id=1 AND tf.template_id=t.id AND tf.field_id=f.id AND tf.del_flag=0
  );

-- ===== API_CONFIG =====
INSERT INTO crm_biz_form_template
  (id, tenant_id, object_type, template_code, template_name, is_default, form_enabled, status, version,
   create_by, create_time, update_by, update_time, del_flag)
SELECT 3400000000003600000, 1, 'API_CONFIG', 'default', '默认接口配置表单', 1, 1, 'ENABLED', 1,
       1, NOW(), 1, NOW(), 0
WHERE NOT EXISTS (
  SELECT 1 FROM crm_biz_form_template WHERE tenant_id=1 AND object_type='API_CONFIG' AND template_code='default' AND del_flag=0
);

INSERT INTO crm_biz_form_field
  (id, tenant_id, object_type, field_code, field_kind, field_label, component_type, dict_type, section_code,
   locked, default_enabled, default_required, sort_order, create_by, create_time, update_by, update_time, del_flag)
SELECT seed.id, 1, seed.object_type, seed.field_code, 'SYSTEM', seed.field_label, seed.component_type, seed.dict_type, seed.section_code,
       seed.locked, seed.default_enabled, seed.default_required, seed.sort_order, 1, NOW(), 1, NOW(), 0
FROM (
  SELECT 3400000000003600001 id, 'API_CONFIG' object_type, 'configName' field_code, '配置名称' field_label, 'input' component_type, NULL dict_type, 'basic' section_code, 1 locked, 1 default_enabled, 1 default_required, 10 sort_order UNION ALL
  SELECT 3400000000003600002 id, 'API_CONFIG' object_type, 'apiPath' field_code, '接口路径' field_label, 'input' component_type, NULL dict_type, 'basic' section_code, 0 locked, 1 default_enabled, 1 default_required, 20 sort_order
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM crm_biz_form_field f
  WHERE f.tenant_id=1 AND f.object_type=seed.object_type AND f.field_code=seed.field_code AND f.del_flag=0
);

INSERT INTO crm_biz_form_template_field
  (id, tenant_id, template_id, field_id, enabled, required, list_visible, list_sortable, commonly_used,
   display_label, sort_order, create_by, create_time, update_by, update_time, del_flag)
SELECT
  3400000000003600500 + f.sort_order, 1, t.id, f.id,
  f.default_enabled, f.default_required,
  CASE WHEN f.sort_order <= 30 THEN 1 ELSE 0 END,
  0, CASE WHEN f.section_code='basic' THEN 1 ELSE 0 END,
  NULL, f.sort_order, 1, NOW(), 1, NOW(), 0
FROM crm_biz_form_field f
JOIN crm_biz_form_template t ON t.tenant_id=1 AND t.object_type='API_CONFIG' AND t.template_code='default' AND t.del_flag=0
WHERE f.tenant_id=1 AND f.object_type='API_CONFIG' AND f.del_flag=0
  AND NOT EXISTS (
    SELECT 1 FROM crm_biz_form_template_field tf
    WHERE tf.tenant_id=1 AND tf.template_id=t.id AND tf.field_id=f.id AND tf.del_flag=0
  );

-- ===== MENU =====
INSERT INTO crm_biz_form_template
  (id, tenant_id, object_type, template_code, template_name, is_default, form_enabled, status, version,
   create_by, create_time, update_by, update_time, del_flag)
SELECT 3400000000003700000, 1, 'MENU', 'default', '默认菜单表单', 1, 1, 'ENABLED', 1,
       1, NOW(), 1, NOW(), 0
WHERE NOT EXISTS (
  SELECT 1 FROM crm_biz_form_template WHERE tenant_id=1 AND object_type='MENU' AND template_code='default' AND del_flag=0
);

INSERT INTO crm_biz_form_field
  (id, tenant_id, object_type, field_code, field_kind, field_label, component_type, dict_type, section_code,
   locked, default_enabled, default_required, sort_order, create_by, create_time, update_by, update_time, del_flag)
SELECT seed.id, 1, seed.object_type, seed.field_code, 'SYSTEM', seed.field_label, seed.component_type, seed.dict_type, seed.section_code,
       seed.locked, seed.default_enabled, seed.default_required, seed.sort_order, 1, NOW(), 1, NOW(), 0
FROM (
  SELECT 3400000000003700001 id, 'MENU' object_type, 'resourceName' field_code, '菜单名称' field_label, 'input' component_type, NULL dict_type, 'basic' section_code, 1 locked, 1 default_enabled, 1 default_required, 10 sort_order UNION ALL
  SELECT 3400000000003700002 id, 'MENU' object_type, 'path' field_code, '路由地址' field_label, 'input' component_type, NULL dict_type, 'basic' section_code, 0 locked, 1 default_enabled, 0 default_required, 20 sort_order UNION ALL
  SELECT 3400000000003700003 id, 'MENU' object_type, 'perms' field_code, '权限标识' field_label, 'input' component_type, NULL dict_type, 'basic' section_code, 0 locked, 1 default_enabled, 0 default_required, 30 sort_order
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM crm_biz_form_field f
  WHERE f.tenant_id=1 AND f.object_type=seed.object_type AND f.field_code=seed.field_code AND f.del_flag=0
);

INSERT INTO crm_biz_form_template_field
  (id, tenant_id, template_id, field_id, enabled, required, list_visible, list_sortable, commonly_used,
   display_label, sort_order, create_by, create_time, update_by, update_time, del_flag)
SELECT
  3400000000003700500 + f.sort_order, 1, t.id, f.id,
  f.default_enabled, f.default_required,
  CASE WHEN f.sort_order <= 30 THEN 1 ELSE 0 END,
  0, CASE WHEN f.section_code='basic' THEN 1 ELSE 0 END,
  NULL, f.sort_order, 1, NOW(), 1, NOW(), 0
FROM crm_biz_form_field f
JOIN crm_biz_form_template t ON t.tenant_id=1 AND t.object_type='MENU' AND t.template_code='default' AND t.del_flag=0
WHERE f.tenant_id=1 AND f.object_type='MENU' AND f.del_flag=0
  AND NOT EXISTS (
    SELECT 1 FROM crm_biz_form_template_field tf
    WHERE tf.tenant_id=1 AND tf.template_id=t.id AND tf.field_id=f.id AND tf.del_flag=0
  );

-- ===== JOB_CONFIG =====
INSERT INTO crm_biz_form_template
  (id, tenant_id, object_type, template_code, template_name, is_default, form_enabled, status, version,
   create_by, create_time, update_by, update_time, del_flag)
SELECT 3400000000003800000, 1, 'JOB_CONFIG', 'default', '默认任务配置表单', 1, 1, 'ENABLED', 1,
       1, NOW(), 1, NOW(), 0
WHERE NOT EXISTS (
  SELECT 1 FROM crm_biz_form_template WHERE tenant_id=1 AND object_type='JOB_CONFIG' AND template_code='default' AND del_flag=0
);

INSERT INTO crm_biz_form_field
  (id, tenant_id, object_type, field_code, field_kind, field_label, component_type, dict_type, section_code,
   locked, default_enabled, default_required, sort_order, create_by, create_time, update_by, update_time, del_flag)
SELECT seed.id, 1, seed.object_type, seed.field_code, 'SYSTEM', seed.field_label, seed.component_type, seed.dict_type, seed.section_code,
       seed.locked, seed.default_enabled, seed.default_required, seed.sort_order, 1, NOW(), 1, NOW(), 0
FROM (
  SELECT 3400000000003800001 id, 'JOB_CONFIG' object_type, 'jobName' field_code, '任务名称' field_label, 'input' component_type, NULL dict_type, 'basic' section_code, 1 locked, 1 default_enabled, 1 default_required, 10 sort_order UNION ALL
  SELECT 3400000000003800002 id, 'JOB_CONFIG' object_type, 'jobGroup' field_code, '任务组' field_label, 'input' component_type, NULL dict_type, 'basic' section_code, 0 locked, 1 default_enabled, 0 default_required, 20 sort_order UNION ALL
  SELECT 3400000000003800003 id, 'JOB_CONFIG' object_type, 'status' field_code, '状态' field_label, 'dictSelect' component_type, 'sys_job_status' dict_type, 'basic' section_code, 0 locked, 1 default_enabled, 0 default_required, 30 sort_order
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM crm_biz_form_field f
  WHERE f.tenant_id=1 AND f.object_type=seed.object_type AND f.field_code=seed.field_code AND f.del_flag=0
);

INSERT INTO crm_biz_form_template_field
  (id, tenant_id, template_id, field_id, enabled, required, list_visible, list_sortable, commonly_used,
   display_label, sort_order, create_by, create_time, update_by, update_time, del_flag)
SELECT
  3400000000003800500 + f.sort_order, 1, t.id, f.id,
  f.default_enabled, f.default_required,
  CASE WHEN f.sort_order <= 30 THEN 1 ELSE 0 END,
  0, CASE WHEN f.section_code='basic' THEN 1 ELSE 0 END,
  NULL, f.sort_order, 1, NOW(), 1, NOW(), 0
FROM crm_biz_form_field f
JOIN crm_biz_form_template t ON t.tenant_id=1 AND t.object_type='JOB_CONFIG' AND t.template_code='default' AND t.del_flag=0
WHERE f.tenant_id=1 AND f.object_type='JOB_CONFIG' AND f.del_flag=0
  AND NOT EXISTS (
    SELECT 1 FROM crm_biz_form_template_field tf
    WHERE tf.tenant_id=1 AND tf.template_id=t.id AND tf.field_id=f.id AND tf.del_flag=0
  );

