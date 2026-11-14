# 组织上下文角色权限模型

> 来源：变更 org-scoped-role-permission
> 时间：2026-07-06

## 问题描述

旧权限模型只有 `tenant_id + user_id + role_id` 的全局用户角色关系，无法表达同一账号在同一数据中心下多个组织拥有不同角色。放开多组织后，如果仍按用户全部组织计算角色、菜单、API 和数据权限，会造成组织间串权。

## 解决方案

### 上下文分层

- 数据中心是第一层上下文，继续使用现有 `tenantId`、`sys_user_tenant` 和 `/system/tenant/switch`。
- 组织是第二层上下文，使用 `LoginUser.activeOrgId` 表示当前请求的唯一组织。
- `LoginUser.orgIds` 只表示可切换组织集合，不表示一次请求的数据权限范围。

### 授权模型

- 用户组织成员关系仍由 `sys_user_org` 表达。
- 角色适用组织由 `sys_role_org` 表达，表示角色在哪些组织可被分配。
- 用户在组织内的真实角色授权由 `sys_user_org_role` 表达。
- 旧 `sys_user_role` 只作为迁移和兼容来源，普通权限计算不能回退旧表。

### 权限计算

- 登录、刷新用户信息、切换数据中心和切换组织后，必须按 `tenantId + userId + activeOrgId` 重算 `roleIds/roleKeys/permissions/apiPermissions`。
- `ORG` 数据权限只使用当前组织。
- `ORG_AND_CHILD` 只展开当前组织及子组织。
- `CUSTOM` 只基于当前组织有效角色的自定义范围。
- 消息、流程、数据集等旁路不能直接读取旧 `sys_user_role` 或用户全部 `orgIds`。

### 超级管理员边界

历史超级管理员可能没有 `sys_user_org` 显式绑定。为兼容老账号，登录态可以把当前数据中心全量组织作为兜底组织上下文。

但组织切换选项和切换校验不能直接相信登录态兜底 `orgIds`。应重新查询 `sys_user_org`：

- 有显式绑定时，只返回和允许切换绑定组织。
- 没有显式绑定的历史超级管理员，才保留当前数据中心全量组织兜底。

前端组织切换器可继续用扁平下拉展示，路径文案足够承载少量组织，不需要强制树形控件。

## 相关文件

- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/service/impl/UserLoadServiceImpl.java`
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/service/impl/SysOrgServiceImpl.java`
- `forge-server/forge-framework/forge-starter-parent/forge-starter-datascope/src/main/java/com/mdframe/forge/starter/datascope/service/impl/DataScopeServiceImpl.java`
- `forge-server/forge-framework/forge-starter-parent/forge-starter-datascope/src/main/java/com/mdframe/forge/starter/datascope/handler/DataScopeInterceptor.java`
- `forge-server/db/mysql/migration/V1.0.1__add_org_scoped_role_permission.sql`
