-- 企业协同控制台整合
-- 1. 补充同步批次「当前阶段」字典（FETCH/VALIDATE/PLAN/APPLY/FINALIZE），前端表格用 DictTag 翻译；
-- 2. 菜单整合：6 个二级菜单收敛为单一「企业协同」控制台入口（原连接管理菜单升级），
--    同步批次/问题单/映射查询/投递记录/回调事件 5 个菜单降级为控制台入口下的按钮资源，
--    perms 与 sys_role_resource 授权关系全部保留，前端按 route.meta.btns 控制页签可见性；
-- 3. 原 /system/collaboration 目录下线（参照 V1.0.68 模式），API 资源迁挂到控制台菜单下。
-- 幂等性：字典 INSERT 带 NOT EXISTS 防重复；UPDATE/DELETE 按 perms/path 匹配，天然可重复执行。

-- ============================================================
-- 1. 字典：同步阶段
-- ============================================================

INSERT INTO sys_dict_type (
  tenant_id, dict_name, dict_type, dict_status, remark,
  create_by, create_time, update_by, update_time, create_dept
)
SELECT 1, '协同同步阶段', 'sys_collab_sync_stage', 1, '目录同步批次执行阶段',
       1, NOW(), 1, NOW(), 1
WHERE NOT EXISTS (
  SELECT 1 FROM sys_dict_type t
  WHERE t.tenant_id = 1 AND t.dict_type = 'sys_collab_sync_stage'
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
  SELECT 1 tenant_id, 1 dict_sort, '拉取数据' dict_label, 'FETCH' dict_value,
         'sys_collab_sync_stage' dict_type, 'info' list_class, 'N' is_default, '从外部平台拉取通讯录数据' remark
  UNION ALL SELECT 1, 2, '数据校验', 'VALIDATE', 'sys_collab_sync_stage', 'info', 'N', '校验拉取数据完整性与合法性'
  UNION ALL SELECT 1, 3, '差异计算', 'PLAN', 'sys_collab_sync_stage', 'warning', 'N', '计算与本系统目录的差异'
  UNION ALL SELECT 1, 4, '写入应用', 'APPLY', 'sys_collab_sync_stage', 'warning', 'N', '将差异写入本系统组织与用户'
  UNION ALL SELECT 1, 5, '收尾完成', 'FINALIZE', 'sys_collab_sync_stage', 'success', 'N', '统计汇总并收敛批次'
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM sys_dict_data data
  WHERE data.tenant_id = seed.tenant_id
    AND data.dict_type = seed.dict_type
    AND data.dict_value = seed.dict_value
);

-- ============================================================
-- 2. 定位关键资源
-- ============================================================

SET @system_dir_id = (
  SELECT id FROM sys_resource
  WHERE tenant_id = 1 AND resource_type = 1 AND path = '/system' AND del_flag = 0
  ORDER BY id LIMIT 1
);

SET @collab_dir_id = (
  SELECT id FROM sys_resource
  WHERE tenant_id = 1 AND resource_type = 1 AND path = '/system/collaboration' AND del_flag = 0
  ORDER BY id LIMIT 1
);

SET @console_menu_id = (
  SELECT id FROM sys_resource
  WHERE tenant_id = 1 AND resource_type = 2
    AND perms = 'system:collaboration:connection:list' AND del_flag = 0
  ORDER BY id LIMIT 1
);

-- ============================================================
-- 3. 原 5 个运维菜单下的按钮资源迁挂到控制台菜单
-- ============================================================

UPDATE sys_resource btn
JOIN sys_resource menu ON menu.id = btn.parent_id
SET btn.parent_id = @console_menu_id, btn.update_by = 1, btn.update_time = NOW()
WHERE @console_menu_id IS NOT NULL
  AND btn.tenant_id = 1 AND btn.resource_type = 3 AND btn.del_flag = 0
  AND menu.tenant_id = 1 AND menu.resource_type = 2
  AND menu.perms IN (
    'system:collaboration:sync:view',
    'system:collaboration:issue:view',
    'system:collaboration:mapping:view',
    'system:collaboration:delivery:view',
    'system:collaboration:callback:view'
  );

-- ============================================================
-- 4. 5 个运维菜单降级为控制台入口下的按钮资源（保留 perms 与角色授权）
-- ============================================================

UPDATE sys_resource
SET resource_type = 3, parent_id = @console_menu_id,
    path = NULL, component = NULL, icon = NULL, keep_alive = 0,
    update_by = 1, update_time = NOW()
WHERE @console_menu_id IS NOT NULL
  AND tenant_id = 1 AND resource_type = 2 AND del_flag = 0
  AND perms IN (
    'system:collaboration:sync:view',
    'system:collaboration:issue:view',
    'system:collaboration:mapping:view',
    'system:collaboration:delivery:view',
    'system:collaboration:callback:view'
  );

-- ============================================================
-- 5. API 资源从目录迁挂到控制台菜单
-- ============================================================

UPDATE sys_resource
SET parent_id = @console_menu_id, update_by = 1, update_time = NOW()
WHERE @console_menu_id IS NOT NULL AND @collab_dir_id IS NOT NULL
  AND tenant_id = 1 AND resource_type = 4 AND del_flag = 0
  AND parent_id = @collab_dir_id;

-- ============================================================
-- 6. 连接管理菜单升级为「企业协同」控制台入口（直挂 /system 目录）
-- ============================================================

UPDATE sys_resource
SET resource_name = '企业协同', parent_id = COALESCE(@system_dir_id, 0), sort = 95,
    icon = 'ionicons5:GitNetworkOutline', remark = '企业协同控制台：连接配置与同步、投递、回调全链路运维',
    update_by = 1, update_time = NOW()
WHERE @console_menu_id IS NOT NULL AND id = @console_menu_id;

-- ============================================================
-- 7. 下线 /system/collaboration 目录（子资源已全部迁出）
-- ============================================================

DELETE FROM sys_role_resource
WHERE @collab_dir_id IS NOT NULL AND resource_id = @collab_dir_id;

UPDATE sys_resource
SET del_flag = 1, update_by = 1, update_time = NOW()
WHERE @collab_dir_id IS NOT NULL AND id = @collab_dir_id AND del_flag = 0;
