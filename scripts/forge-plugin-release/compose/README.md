# Docker Compose 部署包准备

本工具将已经核验、封存的 community Admin JAR / UI 制品复制成只读 Compose 部署包。
它不会启动 Docker、上传 COS、运行数据库迁移或写入“已部署”状态；可视化部署执行器仍需接通。

## 配置

将 `compose.example.json` 复制到私有配置目录。三个目录须预先创建、真实绝对路径、当前普通用户所有、
权限 0700，彼此不得包含或重叠；目录名不使用空格或 `$`。`repositoryId` 必须匹配封存清单。
把镜像占位符换成已审查并预先加载的真实 `image@sha256:<64位摘要>`；不支持 latest、不自动拉取镜像。

`configRoot` 中放置外部 `application.yml` 和 `nginx.conf`，文件权限 0600，禁止软链接或硬链接。
私有配置只挂载，不会复制到部署包和制品库。Java 配置需使用 8580 内部端口；Nginx 使用 8080，
匹配宿主 UI public path 和 API 前缀，并代理到 `server:8580`。没有对应端的制品时只检查该端配置。

目标要求 Linux rootless Docker、兼容固定只读挂载的 Java 17 / Nginx 镜像。
容器 UID 0 只能在 rootless 映射下使用，映射到宿主普通用户；严禁在 rootful Docker 直接执行该包。
镜像不得依赖提权、启动时降权或写入非 `/tmp` 目录。真正部署执行器需要验证 Docker 安全选项和镜像行为。
当前生成器在离线状态记录 `requiresRootless=true`，不是已完成的 Docker 环境检测。

## 预检和打包

从工程根目录运行，使用现有封存输出中的完整 releaseId：

```bash
node scripts/forge-plugin-release/compose/index.mjs check /absolute/path/compose-config.json <releaseId>
node scripts/forge-plugin-release/compose/index.mjs prepare /absolute/path/compose-config.json <releaseId> --reviewed
node scripts/forge-plugin-release/compose/index.mjs verify /absolute/path/compose-config.json <releaseId>
```

`check` 不写部署目录。`prepare` 会逐字节复制并验证制品，输出 `compose.json`、`manifest.json`、
`preparation.json` 以及 `artifacts/`；不覆盖同名包，不创建 latest，不改变原封存回执。
部署包封存为目录 0500 / 文件 0400，其父目录始终私有。中断遗留 `.pending-compose-*` 应人工隔离检查，
工具不会自动删除、抢占或继续未完成的包。
`verify` 重新核对部署包文件权限、实际制品摘要及完整配置和准备回执，不能通过修改回执把它标记为已部署。

## 执行前的剩余检查

部署操作者仍需取得独立目标权限、核验当前审批、完成数据库备份并审查 Flyway 影响，再验证
Compose 配置、rootless 安全边界及容器启动行为。对外服务需经 HTTPS 反向代理，当前端口仅绑定 loopback。
运行验收必须核对实际 classpath 插件版本及 UI 构建摘要，不能只看 HTTP 200 或容器 labels。
旧版恢复也要核验原制品、运行版本和数据兼容性；切换旧包不回滚 Flyway 历史或业务数据。

Compose 属性使用 [Docker 官方服务规范](https://docs.docker.com/reference/compose-file/services/)。
