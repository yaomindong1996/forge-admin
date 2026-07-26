# 任务拆分 — 修复应用运行页隐藏路由 403
> status: apply
> change: `business-application-runtime-route-permission`

## Task 1: 注册应用运行页隐藏资源并继承角色授权

- **目标**：以可重复执行的 Flyway 迁移把应用运行页加入菜单权限路由树，消除有应用查看权限用户的 403。
- **涉及文件**：
  - `forge-server/db/migration/V1.0.50__add_business_application_runtime_route.sql` — 新增隐藏资源及角色资源绑定。
- **关键 SQL 约束**：
  - 路径固定为 `/app-center/application/:applicationCode/runtime`。
  - 组件固定为 `app-center/application-runtime.[applicationCode]`。
  - `visible=0`，资源权限为 `ai:businessApplication:runtime`；仅从已有 `ai:businessApplication:list` 角色继承授权。
  - 资源和角色绑定均使用 `NOT EXISTS` 防重复；内置 `tenant_id=1`。
- **验收标准**：
  - Flyway 静态检查无 `${...}` 占位符、无 `tenant_id=0`、无重复资源风险。
  - 真实迁移并重新登录后，点击“进入应用”不再跳转 `/403`。
  - 无 `ai:businessApplication:list` 权限的角色不获得该资源。
- **状态**：completed-static，待 repair 失败迁移并完成真实环境验收
