# 修复应用运行页隐藏路由 403
> status: apply
> created: 2026-07-21
> complexity: 🟢简单
> change: `business-application-runtime-route-permission`

## 1. 背景与目标

点击应用工作台“进入应用”会跳转到 `/app-center/application/:applicationCode/runtime`，但前端权限守卫将该地址重定向至 `/403`。本变更通过 Flyway 注册运行页隐藏资源并继承已有“查看业务应用”角色授权，使具备应用查看权限的用户可正常进入运行页。

## 2. 代码现状（Research Findings）

- `forge-admin-ui/src/router/index.js` 已注册 `BusinessApplicationRuntime`，路径为 `/app-center/application/:applicationCode/runtime`，组件为 `application-runtime.[applicationCode].vue`。
- `application.[applicationCode].vue` 的 `openApplicationRuntime()` 已跳转到该命名路由。
- `router/guards/permission-guard.js` 的 `canAccessRoute(...)` 要求目标路径匹配后端菜单下发的 `accessRoutes`，不匹配即跳转 `/403`。
- `V1.0.30__add_business_application_workspace_route.sql` 已为工作台路径写入隐藏资源及角色绑定；`V1.0.34__add_business_application_preview_route.sql` 对预览路径做了同类处理。
- 当前 `forge-server/db/migration/` 中不存在 runtime 路径的资源迁移，故菜单树不包含该路由。

## 3. 功能点

- [x] 新增隐藏资源 `/app-center/application/:applicationCode/runtime`，组件路径与前端手写路由一致。
- [x] 资源使用独立 `ai:businessApplication:runtime` 权限码，避免与已有资源触发唯一索引冲突。
- [x] 继承已绑定“查看业务应用”权限的角色，不向无该权限角色扩大授权范围。

## 4. 业务规则

- 运行页是隐藏路由，不显示在系统侧边菜单。
- 只有具备 `ai:businessApplication:list` 的角色才继承 `ai:businessApplication:runtime` 运行页资源。
- 禁止通过前端白名单绕过 `permission-guard`。

## 5. 数据变更

| 操作 | 表名 | 字段/索引 | 说明 |
|---|---|---|---|
| 新增/补齐 | `sys_resource` | 隐藏路由资源 | 注册应用运行页到权限路由树 |
| 新增/补齐 | `sys_role_resource` | 角色-资源绑定 | 从 `ai:businessApplication:list` 已授权角色继承 |

## 6. 接口变更

无。复用现有菜单获取和应用工作台接口。

## 7. 影响范围

- `forge-server/db/migration/V1.0.50__add_business_application_runtime_route.sql`
- 本变更的 SDD 与验证记录。

不修改 Java、Vue、路由守卫、动态 CRUD 或全局白名单。

## 8. 风险与关注点

- ⚠️ 必须通过 Flyway 数据资源授权，不得放宽 `/app-center/application/**` 的前端路由白名单。
- ⚠️ 当前数据库已有失败的 `1.0.50` 迁移历史，修正脚本后必须先执行 Flyway repair；否则应用仍会因失败记录阻断启动。
- 已登录用户需重新登录或刷新权限菜单缓存后才能收到新隐藏路由。
- 若迁移后仍为 403，应检查当前角色是否真的拥有 `ai:businessApplication:list`，并已继承 `ai:businessApplication:runtime`，以及 Flyway 是否执行成功。

## 8.5 测试策略

- SQL 静态检查：迁移版本唯一、`NOT EXISTS` 幂等、`tenant_id=1`、无 `${...}` Flyway 占位符。
- 真实环境验收：执行 Flyway 后重新登录，点击“进入应用”不得跳转 `/403`；无查看权限用户仍应无法访问。
- 本轮不启动服务、不执行真实数据库迁移；执行和结果记录到 `execution-log.md`。

## 9. 待澄清

无。用户于 2026-07-21 明确授权开始修复。

## 10. 技术决策

沿用 V1.0.30/V1.0.34 的隐藏资源模式，将运行页作为手写动态路由注册到 `sys_resource`。运行页使用独立权限码避免资源唯一索引冲突，但角色授权从既有查看权限继承；不修改前端权限守卫。

## 11. 执行日志

见 `execution-log.md`。

## 12. 审查结论

- 静态检查已通过：版本 `V1.0.50` 唯一、新资源路径/组件/独立运行页权限码一致、资源与角色绑定均有 `NOT EXISTS` 保护、无 Flyway `${...}` 占位符、无新增 `tenant_id=0`。
- 真实环境迁移验收待执行：先 repair 失败的 `1.0.50`，再由用户启动 Admin/Flyway 后重新登录，验证拥有和不拥有 `ai:businessApplication:list` 的角色边界。

## 13. 确认记录（HARD-GATE）

- **确认时间**：2026-07-21
- **确认人**：用户
- **确认内容**：开始修复应用 runtime 路由 403，仅新增最小 Flyway 权限资源迁移。
