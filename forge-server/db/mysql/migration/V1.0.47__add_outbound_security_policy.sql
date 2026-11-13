-- 平台级出站 HTTP 白名单、场景字典和管理权限。

CREATE TABLE IF NOT EXISTS `sys_outbound_whitelist` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户编号',
  `scene` varchar(32) NOT NULL COMMENT '出站场景：JOB_WEBHOOK/FLOW_API',
  `protocol` varchar(8) NOT NULL COMMENT '协议：http/https',
  `host` varchar(253) NOT NULL COMMENT '规范化精确主机名或IP',
  `port_start` int NOT NULL COMMENT '允许端口起始值',
  `port_end` int NOT NULL COMMENT '允许端口结束值',
  `allow_private` tinyint NOT NULL DEFAULT 0 COMMENT '是否允许RFC1918或ULA私网：0否 1是',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0停用 1启用',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `del_flag` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志：0正常 1删除',
  `logic_delete_active` tinyint GENERATED ALWAYS AS (IF(`del_flag` = 0, 1, NULL)) STORED COMMENT '逻辑删除唯一键标识',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_outbound_whitelist_active` (`tenant_id`, `scene`, `protocol`, `host`, `port_start`, `port_end`, `logic_delete_active`),
  KEY `idx_outbound_whitelist_match` (`tenant_id`, `scene`, `protocol`, `host`, `status`, `del_flag`),
  CONSTRAINT `chk_outbound_port_range` CHECK (`port_start` BETWEEN 1 AND 65535 AND `port_end` BETWEEN `port_start` AND 65535),
  CONSTRAINT `chk_outbound_private_scene` CHECK (`allow_private` = 0 OR `scene` = 'FLOW_API')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台受控出站白名单';

INSERT INTO sys_dict_type (
  tenant_id, dict_name, dict_type, dict_status, remark,
  create_by, create_time, update_by, update_time, create_dept
)
SELECT seed.tenant_id, seed.dict_name, seed.dict_type, 1, seed.remark,
       1, NOW(), 1, NOW(), 1
FROM (
  SELECT 1 tenant_id, '出站安全场景' dict_name,
         'sys_outbound_scene' dict_type, '受控出站策略隔离场景' remark
  UNION ALL
  SELECT 1, '出站协议', 'sys_outbound_protocol', '受控出站HTTP协议'
  UNION ALL
  SELECT 1, '出站白名单状态', 'sys_outbound_status', '受控出站白名单启停状态'
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM sys_dict_type data
  WHERE data.tenant_id = seed.tenant_id
    AND data.dict_type = seed.dict_type
);

INSERT INTO sys_dict_data (
  tenant_id, dict_sort, dict_label, dict_value, dict_type,
  css_class, list_class, is_default, dict_status, remark,
  create_by, create_time, update_by, update_time, create_dept
)
SELECT seed.tenant_id, seed.dict_sort, seed.dict_label, seed.dict_value, seed.dict_type,
       NULL, seed.list_class, seed.is_default, 1, seed.remark,
       1, NOW(), 1, NOW(), 1
FROM (
  SELECT 1 tenant_id, 1 dict_sort, '任务Webhook' dict_label, 'JOB_WEBHOOK' dict_value,
         'sys_outbound_scene' dict_type, 'warning' list_class, 'N' is_default,
         '定时任务Webhook场景，永不允许私网' remark
  UNION ALL SELECT 1, 2, '流程API', 'FLOW_API', 'sys_outbound_scene', 'info', 'N', '流程API场景，管理员可显式授权私网'
  UNION ALL SELECT 1, 1, 'HTTP', 'http', 'sys_outbound_protocol', 'warning', 'N', 'HTTP协议'
  UNION ALL SELECT 1, 2, 'HTTPS', 'https', 'sys_outbound_protocol', 'success', 'Y', 'HTTPS协议'
  UNION ALL SELECT 1, 1, '启用', '1', 'sys_outbound_status', 'success', 'Y', '白名单生效'
  UNION ALL SELECT 1, 2, '停用', '0', 'sys_outbound_status', 'default', 'N', '白名单停用'
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM sys_dict_data data
  WHERE data.tenant_id = seed.tenant_id
    AND data.dict_type = seed.dict_type
    AND data.dict_value = seed.dict_value
);

SET @outbound_parent_id = (
  SELECT id
  FROM sys_resource
  WHERE tenant_id = 1
    AND resource_type = 1
    AND path = '/platform/config'
    AND del_flag = 0
  ORDER BY id
  LIMIT 1
);

-- 只创建可分配权限，不自动写入 sys_role_resource。
INSERT INTO sys_resource (
  tenant_id, resource_name, parent_id, resource_type, sort,
  path, component, is_external, sso_enabled, sso_target_client,
  open_target, is_public, menu_status, visible, perms, icon,
  api_method, api_url, keep_alive, always_show, redirect, remark,
  create_by, create_time, update_by, update_time, create_dept, client_code
)
SELECT seed.tenant_id, seed.resource_name, COALESCE(@outbound_parent_id, 0), seed.resource_type, seed.sort,
       NULL, NULL, 0, 0, NULL, '_self', 0, 1, 1, seed.perms, NULL,
       seed.api_method, seed.api_url, 0, 0, NULL, seed.remark,
       1, NOW(), 1, NOW(), 1, 'pc'
FROM (
  SELECT 1 tenant_id, '查看出站白名单' resource_name, 3 resource_type, 91 sort,
         'system:outboundWhitelist:list' perms, NULL api_method, NULL api_url,
         '查看平台出站白名单' remark
  UNION ALL SELECT 1, '新增出站白名单', 3, 92, 'system:outboundWhitelist:add', NULL, NULL, '新增平台出站白名单'
  UNION ALL SELECT 1, '修改出站白名单', 3, 93, 'system:outboundWhitelist:edit', NULL, NULL, '修改平台出站白名单'
  UNION ALL SELECT 1, '删除出站白名单', 3, 94, 'system:outboundWhitelist:remove', NULL, NULL, '逻辑删除平台出站白名单'
  UNION ALL SELECT 1, '出站白名单查询接口', 4, 191, 'system:outboundWhitelist:api:list', 'GET', '/system/outbound-whitelist/page', '查询出站白名单接口'
  UNION ALL SELECT 1, '出站白名单详情接口', 4, 192, 'system:outboundWhitelist:api:detail', 'GET', '/system/outbound-whitelist/*', '查询出站白名单详情接口'
  UNION ALL SELECT 1, '出站白名单新增接口', 4, 193, 'system:outboundWhitelist:api:add', 'POST', '/system/outbound-whitelist', '新增出站白名单接口'
  UNION ALL SELECT 1, '出站白名单修改接口', 4, 194, 'system:outboundWhitelist:api:edit', 'PUT', '/system/outbound-whitelist', '修改出站白名单接口'
  UNION ALL SELECT 1, '出站白名单删除接口', 4, 195, 'system:outboundWhitelist:api:remove', 'DELETE', '/system/outbound-whitelist/*', '删除出站白名单接口'
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM sys_resource resource
  WHERE resource.tenant_id = seed.tenant_id
    AND resource.resource_type = seed.resource_type
    AND resource.perms = seed.perms
    AND resource.del_flag = 0
);
