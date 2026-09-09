# 测试计划 — 修复应用运行页隐藏路由 403

## 本轮增量范围

仅新增一条 Flyway 权限资源迁移，不修改 Java 或前端代码。

## P0 静态验证

1. 确认版本 `V1.0.50` 在迁移目录中唯一且大于现有 `V1.0.49`。
2. 检查新脚本包含目标路径、组件、`visible=0`、`ai:businessApplication:runtime`、用于角色继承的 `ai:businessApplication:list` 与两处 `NOT EXISTS`。
3. 执行 `rg -n '\$\{[^}]+\}' forge-server/db/migration`，确认本轮未引入 Flyway 占位符。
4. 对新增 SQL 执行 `git diff --no-index --check /dev/null <file>`，确认无空白错误。

## P1 真实环境验收（待用户执行/授权）

1. 先对失败的 `1.0.50` 执行 Flyway repair，再启动 `forge-admin-server`，确认 `forge_schema_history` 存在成功的 `1.0.50` 记录。
2. 使用拥有 `ai:businessApplication:list` 的账号重新登录。
3. 从应用工作台点击“进入应用”，验证 `/app-center/application/<applicationCode>/runtime` 正常打开而不是 `/403`。
4. 使用不具备该权限的账号验证该路径仍被拒绝。

## 跳过项

- 不启动真实服务、不执行真实 Flyway 和浏览器验收，除非用户提供/授权环境。

## 本轮增量验证（2026-07-21）

- 已根据真实启动日志修正运行页资源权限码；验证独立运行页权限码、角色继承来源、迁移版本唯一性、两处幂等条件、Flyway 占位符扫描和空白检查。
- 未执行数据库 `repair`、真实迁移或浏览器验收，原因是这些步骤会修改用户的远程开发数据库状态。
