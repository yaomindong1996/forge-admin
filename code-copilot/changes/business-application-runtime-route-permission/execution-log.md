# 执行日志 — 修复应用运行页隐藏路由 403

| 时间 | 范围 | 命令/动作                                                                                                                 | 结果 | 备注 |
|---|---|-----------------------------------------------------------------------------------------------------------------------|---|---|
| 2026-07-21 | 根因调查 | 检查前端路由、权限守卫、V1.0.30/V1.0.34 迁移                                                                                        | completed | runtime 路由/页面已存在；缺少对应隐藏资源迁移，权限守卫因此跳转 403。 |
| 2026-07-21 | SDD | 创建 Spec、Task、Test Spec、执行日志                                                                                           | completed | 用户已授权最小 Flyway 修复；不改 Java/Vue。 |
| 2026-07-21 | Flyway 修复 | 新增 `V1.0.50__add_business_application_runtime_route.sql`                                                              | completed-static | 注册 runtime 隐藏资源，并从 `ai:businessApplication:list` 已授权角色继承资源。 |
| 2026-07-21 | 静态验证 | 版本文件检查、目标路径/组件/权限/幂等条件检查                                                                                              | passed | 迁移版本唯一，资源路径和手写前端路由一致。 |
| 2026-07-21 | Flyway 占位符扫描 | `rg -n '\\$\\{[^}]+\\}' forge-server/db/migration`                                                                    | passed | 无输出，未引入 Flyway 业务占位符。 |
| 2026-07-21 | 空白检查 | `git diff --no-index --check /dev/null forge-server/db/migration/V1.0.50__add_business_application_runtime_route.sql` | passed | 命令因新增文件返回 1，且无空白错误输出。 |
| 2026-07-21 | 真实环境验收 | 未执行                                                                                                                   | pending | 未启动服务、未连接数据库；迁移后需重新登录刷新权限菜单。 |
| 2026-07-21 | 启动失败复盘 | 用户提供 Admin 启动日志 | identified | `V1.0.50` 复用了已有的 `ai:businessApplication:list`，触发 `sys_resource.uk_tenant_resource_active` 唯一键冲突；数据库已留下失败迁移记录。 |
| 2026-07-21 | 迁移脚本修正 | 运行页资源权限改为 `ai:businessApplication:runtime` | completed-static | 角色绑定查询仍以 `ai:businessApplication:list` 为来源，故不会扩大无应用查看权限角色的访问范围。 |
| 2026-07-21 | 修正后静态验证 | 版本唯一性、权限码/继承来源、两处 `NOT EXISTS`、占位符扫描、`git diff --no-index --check` | passed | 输出确认运行页权限为 `ai:businessApplication:runtime`，继承来源保持 `ai:businessApplication:list`；无重复版本、无 Flyway 占位符和空白错误。 |
| 2026-07-21 | 数据库恢复与验收 | 未执行 | pending-user | `forge_admin_new` 已有失败的 `1.0.50` 历史记录；需先 repair 后才能再次启动并执行迁移。 |
