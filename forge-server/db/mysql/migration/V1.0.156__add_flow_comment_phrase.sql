-- 常用审批意见：企业模板 + 个人常用语。业务键仅要求未删除记录唯一，删除后允许重建。

CREATE TABLE IF NOT EXISTS `sys_flow_comment_phrase` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `owner_type` tinyint NOT NULL DEFAULT '0' COMMENT '归属：0企业常用，1我的常用',
  `user_id` bigint NOT NULL DEFAULT '0' COMMENT '个人意见所属用户，企业意见固定0',
  `scene` varchar(16) NOT NULL DEFAULT 'ALL' COMMENT '场景：APPROVE/REJECT/ALL',
  `content` varchar(200) NOT NULL COMMENT '意见内容',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序，越小越靠前',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0禁用，1启用',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建组织',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记，删除后写当前行主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_flow_comment_phrase_content` (`tenant_id`, `owner_type`, `user_id`, `scene`, `content`, `del_flag`),
  KEY `idx_flow_comment_phrase_usable` (`tenant_id`, `status`, `owner_type`, `user_id`, `scene`, `del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程常用审批意见';

INSERT INTO sys_dict_type (
    tenant_id, dict_name, dict_type, dict_status, remark,
    create_by, create_time, update_by, update_time, create_dept
)
SELECT 1, '常用审批意见场景', 'flow_comment_phrase_scene', 1,
       '同意、驳回或通用常用审批意见', 1, NOW(), 1, NOW(), 1
WHERE NOT EXISTS (
    SELECT 1 FROM sys_dict_type
    WHERE tenant_id = 1 AND dict_type = 'flow_comment_phrase_scene'
);

INSERT INTO sys_dict_data (
    tenant_id, dict_sort, dict_label, dict_value, dict_type,
    css_class, list_class, is_default, dict_status, remark,
    create_by, create_time, update_by, update_time, create_dept
)
SELECT seed.tenant_id, seed.dict_sort, seed.dict_label, seed.dict_value,
       'flow_comment_phrase_scene', NULL, seed.list_class, seed.is_default,
       1, seed.remark, 1, NOW(), 1, NOW(), 1
FROM (
    SELECT 1 tenant_id, 1 dict_sort, '同意' dict_label, 'APPROVE' dict_value,
           'success' list_class, 'N' is_default, '用于同意/通过' remark
    UNION ALL
    SELECT 1, 2, '驳回', 'REJECT', 'error', 'N', '用于驳回/退回'
    UNION ALL
    SELECT 1, 3, '通用', 'ALL', 'info', 'Y', '同意和驳回都可点选'
) seed
WHERE NOT EXISTS (
    SELECT 1 FROM sys_dict_data d
    WHERE d.tenant_id = seed.tenant_id
      AND d.dict_type = 'flow_comment_phrase_scene'
      AND d.dict_value = seed.dict_value
);

INSERT INTO sys_dict_type (
    tenant_id, dict_name, dict_type, dict_status, remark,
    create_by, create_time, update_by, update_time, create_dept
)
SELECT 1, '常用审批意见归属', 'flow_comment_phrase_owner_type', 1,
       '企业下发或个人自用', 1, NOW(), 1, NOW(), 1
WHERE NOT EXISTS (
    SELECT 1 FROM sys_dict_type
    WHERE tenant_id = 1 AND dict_type = 'flow_comment_phrase_owner_type'
);

INSERT INTO sys_dict_data (
    tenant_id, dict_sort, dict_label, dict_value, dict_type,
    css_class, list_class, is_default, dict_status, remark,
    create_by, create_time, update_by, update_time, create_dept
)
SELECT seed.tenant_id, seed.dict_sort, seed.dict_label, seed.dict_value,
       'flow_comment_phrase_owner_type', NULL, seed.list_class, seed.is_default,
       1, seed.remark, 1, NOW(), 1, NOW(), 1
FROM (
    SELECT 1 tenant_id, 1 dict_sort, '企业常用' dict_label, '0' dict_value,
           'info' list_class, 'Y' is_default, '租户内所有审批人可见' remark
    UNION ALL
    SELECT 1, 2, '我的常用', '1', 'success', 'N', '仅当前用户可见'
) seed
WHERE NOT EXISTS (
    SELECT 1 FROM sys_dict_data d
    WHERE d.tenant_id = seed.tenant_id
      AND d.dict_type = 'flow_comment_phrase_owner_type'
      AND d.dict_value = seed.dict_value
);

INSERT INTO sys_flow_comment_phrase (
    tenant_id, owner_type, user_id, scene, content, sort_order, status,
    create_by, create_time, create_dept, update_by, update_time, del_flag
)
SELECT seed.tenant_id, seed.owner_type, seed.user_id, seed.scene, seed.content,
       seed.sort_order, 1, 1, NOW(), 1, 1, NOW(), 0
FROM (
    SELECT 1 tenant_id, 0 owner_type, 0 user_id, 'APPROVE' scene, '同意' content, 1 sort_order
    UNION ALL SELECT 1, 0, 0, 'APPROVE', '已阅', 2
    UNION ALL SELECT 1, 0, 0, 'APPROVE', '情况属实', 3
    UNION ALL SELECT 1, 0, 0, 'REJECT', '驳回', 1
    UNION ALL SELECT 1, 0, 0, 'REJECT', '请补充材料', 2
    UNION ALL SELECT 1, 0, 0, 'REJECT', '请修改后重提', 3
) seed
WHERE NOT EXISTS (
    SELECT 1 FROM sys_flow_comment_phrase p
    WHERE p.tenant_id = seed.tenant_id
      AND p.owner_type = seed.owner_type
      AND p.user_id = seed.user_id
      AND p.scene = seed.scene
      AND p.content = seed.content
      AND p.del_flag = 0
);

SET @flow_menu_id := (
  SELECT id FROM sys_resource
  WHERE tenant_id = 1 AND resource_type = 1 AND path = '/flow' AND del_flag = 0
  ORDER BY id LIMIT 1
);

INSERT INTO sys_resource (
  tenant_id, resource_name, parent_id, resource_type, sort,
  path, component, is_external, sso_enabled, sso_target_client,
  open_target, is_public, menu_status, visible, perms, icon,
  api_method, api_url, keep_alive, always_show, redirect, remark,
  create_by, create_time, update_by, update_time, create_dept, client_code
)
SELECT 1, '常用审批意见', @flow_menu_id, 2, 41,
       '/flow/commentPhrase', 'flow/commentPhrase', 0, 0, NULL,
       '_self', 0, 1, 1, 'flow:comment-phrase:view', 'ionicons5:Chatbubbles',
       NULL, NULL, 0, 0, NULL, '维护企业和个人常用审批意见',
       1, NOW(), 1, NOW(), 1, 'pc'
WHERE @flow_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_resource r
    WHERE r.tenant_id = 1 AND r.path = '/flow/commentPhrase' AND r.del_flag = 0
  );

SET @phrase_menu_id := (
  SELECT id FROM sys_resource
  WHERE tenant_id = 1 AND path = '/flow/commentPhrase' AND del_flag = 0
  ORDER BY id LIMIT 1
);

INSERT INTO sys_resource (
  tenant_id, resource_name, parent_id, resource_type, sort,
  path, component, is_external, sso_enabled, sso_target_client,
  open_target, is_public, menu_status, visible, perms, icon,
  api_method, api_url, keep_alive, always_show, redirect, remark,
  create_by, create_time, update_by, update_time, create_dept, client_code
)
SELECT seed.tenant_id, seed.resource_name, @phrase_menu_id, 3, seed.sort,
       NULL, NULL, 0, 0, NULL, '_self', 0, 1, 1, seed.perms, NULL,
       NULL, NULL, 0, 0, NULL, seed.remark,
       1, NOW(), 1, NOW(), 1, 'pc'
FROM (
  SELECT 1 tenant_id, '维护常用审批意见' resource_name, 1 sort,
         'flow:comment-phrase:manage' perms, '新增、编辑、停用企业常用审批意见' remark
) seed
WHERE @phrase_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_resource r
    WHERE r.tenant_id = 1 AND r.resource_type = 3
      AND r.perms = seed.perms AND r.del_flag = 0
  );

INSERT INTO sys_resource (
  tenant_id, resource_name, parent_id, resource_type, sort,
  path, component, is_external, sso_enabled, sso_target_client,
  open_target, is_public, menu_status, visible, perms, icon,
  api_method, api_url, keep_alive, always_show, redirect, remark,
  create_by, create_time, update_by, update_time, create_dept, client_code
)
SELECT seed.tenant_id, seed.resource_name, @phrase_menu_id, 4, seed.sort,
       NULL, NULL, 0, 0, NULL, '_self', 0, 1, 1, seed.perms, NULL,
       seed.api_method, seed.api_url, 0, 0, NULL, seed.remark,
       1, NOW(), 1, NOW(), 1, 'pc'
FROM (
  SELECT 1 tenant_id, '查询常用审批意见接口' resource_name, 10 sort,
         'flow:comment-phrase:api:read' perms, 'GET' api_method,
         '/api/flow/comment-phrases/page' api_url, '分页查询常用审批意见' remark
  UNION ALL
  SELECT 1, '查询常用审批意见详情接口', 11,
         'flow:comment-phrase:api:detail', 'GET', '/api/flow/comment-phrases/{id}', '查询常用审批意见详情'
  UNION ALL
  SELECT 1, '新增常用审批意见接口', 12,
         'flow:comment-phrase:api:create', 'POST', '/api/flow/comment-phrases', '新增常用审批意见'
  UNION ALL
  SELECT 1, '修改常用审批意见接口', 13,
         'flow:comment-phrase:api:update', 'PUT', '/api/flow/comment-phrases', '修改常用审批意见'
  UNION ALL
  SELECT 1, '删除常用审批意见接口', 14,
         'flow:comment-phrase:api:delete', 'DELETE', '/api/flow/comment-phrases/{id}', '逻辑删除常用审批意见'
) seed
WHERE @phrase_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_resource r
    WHERE r.tenant_id = seed.tenant_id AND r.resource_type = 4
      AND r.perms = seed.perms AND r.del_flag = 0
  );
