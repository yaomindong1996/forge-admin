-- 清理用户关联表中的悬挂绑定（关联对象已逻辑删除或不存在）。
-- 背景：岗位/角色删除时历史版本未清理 sys_user_post、sys_user_role 绑定，
-- 导致用户编辑弹窗岗位/角色回显裸值、角色成员列表与删除校验口径不一致。
-- 影响范围：仅删除指向已删除岗位/已删除角色的无效绑定行，有效绑定不受影响。
-- 回滚方式：无自动回滚；如需恢复，依据业务操作日志重建绑定关系。

DELETE up
FROM sys_user_post up
LEFT JOIN sys_post p
    ON p.id = up.post_id
   AND p.del_flag = 0
WHERE p.id IS NULL;

DELETE usr
FROM sys_user_role usr
LEFT JOIN sys_role r
    ON r.id = usr.role_id
   AND r.del_flag = 0
WHERE r.id IS NULL;

DELETE uor
FROM sys_user_org_role uor
LEFT JOIN sys_role r
    ON r.id = uor.role_id
   AND r.del_flag = 0
WHERE r.id IS NULL;
