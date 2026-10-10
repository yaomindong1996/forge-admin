# Admin 独立 JAR 部署与 Flyway 迁移来源

`forge-admin-server.jar` 已通过 Admin POM 打包 `db/migration/*.sql`。
独立部署时显式设置：

```dotenv
FORGE_FLYWAY_LOCATIONS=classpath:db/migration
```

示例见 [forge-admin.env.example](./forge-admin.env.example)。只将这一项合并到该服务实际使用的
EnvironmentFile，不覆盖现有变量。服务管理器须将它传给 Java 进程；修改 EnvironmentFile 后需要重启服务。
若修改了 systemd unit 本身，再执行 `systemctl daemon-reload`。不要把此示例作为 Spring YAML 使用。

## 为什么不能只替换 JAR

源码本地启动默认使用 filesystem 路径。服务器工作目录中的旧 `db/migration` 可能仍被优先读取，
导致 JAR 正确但 Flyway 校验失败。不要同时扫描旧文件系统目录与新 classpath 目录。
不要通过 `repair`、关闭校验、删除历史表或修改历史 checksum 掩盖这一问题。

## 部署检查

1. 确认目标是 Admin 服务，核对 unit、WorkingDirectory、ExecStart、EnvironmentFile 和活动 profile。
2. 核对生效的数据源与 `forge_schema_history`。不要误用门户的 `forge_website_schema_history`。
3. 确认 JAR 内所有已执行脚本与历史记录一致，明确 pending 列表。存在新迁移时，先审查 SQL、
   备份目标数据库并确认升级范围，不能将首次启动当作只读健康检查。
4. 备份该服务 EnvironmentFile、启动脚本及 unit；记录当前 JAR 的 SHA-256。
5. 合并上面的配置，再重启 **Admin 单个服务**；不得顺带重启其它模块。
6. 确认日志出现 Flyway 校验成功和应用启动完成；检查进程、端口、健康及公开接口。
7. 复查历史表无失败记录。只改来源且没有 pending 时，迁移历史内容不应变化。

已有配置中的 `SPRING_FLYWAY_LOCATIONS`、命令行 `--spring.flyway.locations` 或 JVM 系统属性可能覆盖
这个变量。应核对实际优先级，不能只检查文件里是否有这一行。保留 `spring.flyway.table`、
校验行为和其它数据库连接配置，不因服务器上有一个未激活的 profile 文件就修改其内容。

## 回滚

恢复本次备份的 EnvironmentFile；若更改过 unit 则恢复它并重新加载 systemd，然后重启 Admin。
原 JAR、外部 SQL 和数据库历史都应保留。无 pending 的来源修复不需要数据库回滚；如果版本升级
执行过新迁移，必须按该版本专门的数据库回滚方案处理，不能只恢复配置。

## 配置契约测试

```bash
node --test forge-server/scripts/deployment/admin-flyway.test.mjs
```
