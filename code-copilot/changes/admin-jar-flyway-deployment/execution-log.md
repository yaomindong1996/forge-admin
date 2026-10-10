# 执行记录

## 2026-10-10：部署前核对

- 当前分支 `codex/plugin-foundation`，保留已有 `.DS_Store` 修改。
- 使用用户验证过指纹的 SSH 主机密钥，未跳过主机校验。
- 目标为 `spring_forge-admin`，启动 JAR 为 `forge-admin-server.jar`，工作目录为 Admin 部署目录。
- 服务在本轮开始前已为 failed；未修改其它服务。
- 服务 EnvironmentFile 为 0 字节，无额外环境变量。正在核对实际 profile 与数据库历史。
- 不记录任何密码、连接凭据或配置完整内容。

## 2026-10-10 19:31–19:32：配置部署与 Flyway 验证

- 日志确认活动 profile 为 `dev`，未激活的 `config/application-prod.yml` 未修改。
- MySQL 只读查询确认实际历史表为 `forge_schema_history`；217 条记录均成功，脚本名/版本/checksum
  与生产 JAR 全部一致，0 pending。诊断脚本按 Spring 占位符解析用户名后完成查询，凭据仅在远端内存使用。
- 备份目录：`/www/wwwroot/admin-service/backups/admin-flyway-20261010-193154`，权限 0700。
  包含原 EnvironmentFile、unit、启动脚本与部署摘要，不包含业务数据导出。
- 仅将 `/var/tmp/springboot/vhost/env/forge-admin.env` 增加
  `FORGE_FLYWAY_LOCATIONS=classpath:db/migration`，原权限/属主保留。
- 生产 JAR SHA-256 保持 `30034dea215cb4b31aba6b3396c93d05d61d13264c9a2b353d78e36732ab1685`，未替换 JAR。
- `systemctl restart spring_forge-admin` 返回 0，PID 2164738，active/running。
- 读取该进程环境确认 `FORGE_FLYWAY_LOCATIONS=classpath:db/migration`。
- 本次启动日志：`Successfully validated 217 migrations`；`No migration necessary`，版本 1.0.217。
- 未执行 repair/clean、未修改任何迁移历史，未删除旧 SQL，未重启其它服务。
- `node --test forge-server/scripts/deploy/admin-flyway.test.mjs`：3/3 通过；`git diff --check` 通过。
- 首次 HTTP 检查时应用仍在初始化，端口尚未接受连接，继续等待并复查，不将 systemd active 等同于应用就绪。

## 2026-10-10：部署验收完成

- 19:32:43 日志：`Started ForgeAdminApplication in 46.411 seconds`。
- systemd active/running，PID 2164738，NRestarts=0；只保留用户要求部署的生产 Admin 服务运行。
- `/actuator/health/liveness`、`/actuator/health/readiness`、`/actuator/health` 均 HTTP 200 / UP。
- 服务器本机 `/auth/loginConfig` 返回 HTTP 200 / code=200。
- 公网 `http://www.dlforgelab.com:8084/forge-api/auth/loginConfig` 返回 code=200 / 操作成功，
  确认 Nginx 到 Admin 的真实请求链路恢复。仅检查公开配置，不登录、不修改业务数据。
- 部署后只读复核：历史仍为 217 条成功记录，版本 1.0.217，脚本/校验值无差异，无 pending。
- 新启动日志未发现 ERROR / Flyway 校验失败。
- 跳过业务重打包与前端构建：本轮没有改业务类、迁移 SQL 或前端，生产使用原 JAR。
- 未执行任何日志清理；发现旧应用日志约 10 GB，磁盘仍有约 5.7 GB，日志轮转可另行治理。
- 如需恢复本次配置：将备份目录中的 `forge-admin.env` 恢复到原 EnvironmentFile，再重启
  `spring_forge-admin`。这是配置回滚，原 JAR 与 SQL 未变；恢复旧配置会再次遇到原校验错误。
- 仓库新增部署示例、说明与测试，未提交/推送；原 `.DS_Store` 变更未处理。
- 部署示例最终放入 `forge-server/scripts/deployment/`（原 `deploy/` 名称被仓库整体忽略），
  `node --test forge-server/scripts/deployment/admin-flyway.test.mjs` 再次 3/3 通过。

## 2026-10-10：按用户要求提交仓库改动

- 提交范围为部署配置示例、使用/回滚说明、3 项契约测试、变更记录与踩坑记录，共 9 个文件。
- 复跑配置契约测试 3/3 通过；`git diff --check` 与 `pnpm check:edition` 通过。
- 本次不改业务 Java、已有 SQL 或源码默认启动配置；实际生产修正位于服务器 EnvironmentFile。
- 在 `codex/plugin-foundation` 分支提交，不 push；用户已有 `.DS_Store` 修改排除在提交之外。
- 复用本次生产启动与 HTTP 验收结果；此轮仅 Git 提交，不再次连接或重启生产服务。
