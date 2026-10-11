# Forge 生产部署手册

本会话「Forge 服务部署与发布」专用于部署、回退、启动故障排查及发布验收。
本文为部署上下文，不保存密码、Token、数据库凭据或私钥。
发版规则以 [根 AGENTS §5.19](../../../AGENTS.md) 和
[版本维护指南](../../../forge-docs/guide/release-version.md) 为准。

## 最后验收基线（2026-10-10）

- 已部署：Admin 后端、Admin UI，发行版本 **1.2.0**。
- 实际构建源码：`100ce072087468f06e5349e2fdfdd9248748ed63`。
- 整理时远端 main 已包含部署记录提交 `56dd50e5`；记录提交不代表重新构建部署。
- 健康接口 UP，公网静态资源及版本文件匹配；217 条迁移校验成功，无待执行项。
- 本次没有部署 App、Flow、Report、H5、Website，没有创建发布标签。
- 生产登录后的“关于系统”弹窗点击尚未验收，浏览器连接超时；其它验收证据见
  [执行记录](../../../code-copilot/changes/system-version-visibility/execution-log.md)。

以上是最后验证结果，不是实时监控；下次部署前重新核对服务器与 Git 状态。

## 环境与路径

| 项目 | 最后核验值 |
| --- | --- |
| SSH | `81.70.22.48:22`，账户 `root` |
| 已由用户核验的 ED25519 指纹 | `SHA256:M5LFfFji4duIIkoycsl9SVBQGO9T5jnKgnaiFupdKyg` |
| 当前源码目录 | `/Users/mini32g/Desktop/project/forge-admin` |
| Admin JAR | `/www/wwwroot/admin-service/forge-admin-server.jar` |
| Admin systemd | `spring_forge-admin` |
| Admin 端口 | `8580` |
| 活动 Spring profile | `dev`（生产现状，不要误认成 `prod`） |
| 活动外置配置 | `/www/wwwroot/admin-service/config/application-dev.yml` |
| EnvironmentFile | `/var/tmp/springboot/vhost/env/forge-admin.env` |
| Admin 日志 | `/www/wwwlogs/java/springboot/forge-admin.log` |
| Admin UI 目录 | `/www/wwwroot/html/dist` |
| 公网 UI | `http://www.dlforgelab.com:8084/forge/` |
| 公网 Admin API | `/forge-api/` → `127.0.0.1:8580` |

SSH 使用可信 known_hosts 校验；上一轮临时 SSH 会话已关闭，临时文件不作为永久连接配置。
旧部署配置中的 `/Applications/yml-develop/...` 不是当前工作目录，构建前要检查实际 checkout。
外置 dev 配置来自旧 JAR 的原配置，已保留原字节及凭据；`application-prod.yml` 未被当前服务启用。

### Flyway

有效来源为 `FORGE_FLYWAY_LOCATIONS=classpath:db/migration`，历史表为 `forge_schema_history`。
最后验证数据库为 `forge_admin_new`，最新迁移 `1.0.217`；不要和 Website 数据库混用。
详见 [Admin JAR / Flyway 部署说明](./admin-flyway.md)。

### 其它模块（历史部署配置，下一次使用前核对）

| 模块 | Maven 模块 | JAR / systemd |
| --- | --- | --- |
| App | `forge-app-server` | `forge-app-server.jar` / `spring_forge-app-server` |
| Flow | `forge-flow/forge-flow-server` | `forge-flow-server.jar` / `spring_forge-flow` |
| Report | `forge-report-server` | `forge-report-server.jar` / `spring_forge-report-server` |
| Website | 独立 forge-website 仓库的 `website-admin-server` | `website-admin-server.jar` / `spring_website-admin-server` |

各模块配置、目录及代理以实际服务定义为准，不用 Admin 的配置覆盖其它服务。
现有 Nginx 中 `/forge-api/api/flow/` 单独转发 Flow 服务，不能只按通用 Admin 前缀判断。

## 下次部署流程

1. 确认本次目标模块、分支和提交；检查本地改动与远端更新，保留其它任务的文件。
2. 按版本维护指南校验版本，从明确提交构建，前后端记录实际提交号；日常更新不自动升版。
3. 只读核对目标服务、活动配置、迁移来源、待执行 SQL、磁盘与 Nginx 路径。
4. 备份目标模块当前产物及相关配置，核对上传摘要后切换；只重启本次目标服务。
5. 检查启动日志、健康、代理与静态资源；Admin / UI 同步核对后端 build-info 和前端 version.json。
6. 记录真实结果、未验证项和可用回退点；数据库升级后的回退单独评估。

Admin UI 当前生产构建变量：`VITE_PUBLIC_PATH=/forge`、`VITE_BASE_URL=/forge`、
`VITE_REQUEST_PREFIX=/forge-api`、`VITE_OUT_DIR=dist`。
Admin 构建使用 `-Dforge.build.commit=<实际构建提交号>`，其它命令见版本维护指南。

## 最新回退点

- 私有备份：`/www/wwwroot/admin-service/backups/system-version-100ce072`。
- 发布暂存：`/www/wwwroot/admin-service/releases/system-version-100ce072`。
- 上一版 UI：上述暂存目录中的 `previous-live-ui`。
- 备份涵盖旧 JAR、UI、配置、环境文件及服务定义；本次没有新增数据库迁移。

这次程序回退需要恢复旧 JAR / UI，并处理本轮新增外置 dev 配置与旧 JAR 内置配置的关系，
具体见执行记录的 T7 回退说明。后续若已有部署或配置变化，先重新核对，不能重放旧脚本覆盖新状态。
