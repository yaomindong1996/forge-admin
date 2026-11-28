---
name: forge-resource-permissions
description: Add or review Forge menus, routes, buttons, and API authorization. Use for sys_resource, sys_role_resource, clientCode pc/h5, v-hasPermi, @SaCheckPermission, method/path API resources, or granting selected roles access to an existing list, export, or other business action. Pair with forge-tenant-datascope when records are scoped and forge-flyway-migration when seeding permissions.
---

# Forge 菜单和 API 权限

1. 读根目录 [AGENTS.md](../../../AGENTS.md) 和[关键约定](../../../docs/guides/critical-conventions.md)。先定位现有前端路由、父菜单、Controller 的实际 HTTP 方法和路径，以及角色与客户端。列出每个角色能进的页面、能执行的操作和能读取的数据。
2. 对照[资源与授权配置](references/resource-contract.md)核对目录和页面、按钮权限码、API 方法和路径、角色资源关联。使用现有父菜单。一个已有列表新增导出时，保留列表的角色和路由，增加独立导出权限。
3. 高敏感操作如导出使用显式服务端动作校验（例如 `@SaCheckPermission`），同时登记精确方法、路径的 API 资源。根据当前会话权限控制 UI。检查现有通配 API、匿名配置、权限豁免和前端未加载权限时的渲染。
4. 写种子前读 [Forge Flyway 迁移](../forge-flyway-migration/SKILL.md)。只为明确授权的角色建立当前租户关联。记录资源 ID 如何解析、旧库如何升级、新会话如何取得更新后的权限。
5. 涉及租户、组织、个人或自定义范围时读 [Forge 数据权限](../forge-tenant-datascope/SKILL.md)。让列表和导出使用同一套后台租户、查询条件和范围。复用命名的 Mapper XML；若增加查询方法，为实际执行的方法单独配 DataScope。
6. 按[采购导出验收案例](references/purchase-export.md)用普通账号验证显示与直调都受控，并以实际返回行或工作簿检查组织和租户边界。记录会话中的按钮权限码、API 方法/路径、作用域和结果。
