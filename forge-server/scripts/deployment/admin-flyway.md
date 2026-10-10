# Admin 独立 JAR 部署与 Flyway 迁移来源

`forge-admin-server.jar` 已通过 Admin POM 打包 `db/migration/*.sql`。
新版默认读取 `classpath:db/migration`，本地、独立 JAR 和 Docker 无需额外复制或挂载 SQL 目录。
目录缺失时启动失败，不自动回退到服务器旧文件。旧版本兼容或需要显式固定来源时可以设置：

```dotenv
FORGE_FLYWAY_LOCATIONS=classpath:db/migration
```

示例见 [forge-admin.env.example](./forge-admin.env.example)。只将这一项合并到该服务实际使用的
EnvironmentFile，不覆盖现有变量。服务管理器须将它传给 Java 进程；修改 EnvironmentFile 后需要重启服务。
若修改了 systemd unit 本身，再执行 `systemctl daemon-reload`。不要把此示例作为 Spring YAML 使用。

## 升级时检查旧配置覆盖

旧版默认使用 filesystem 路径。若外置配置、环境变量或 JVM 参数仍显式指向旧目录，
它们会覆盖新版 JAR 的默认值，导致 JAR 正确但 Flyway 校验失败；升级时应移除覆盖或改为 classpath。
不要同时扫描旧文件系统目录与新 classpath 目录。
不要通过 `repair`、关闭校验、删除历史表或修改历史 checksum 掩盖这一问题。

## 本地开发与 IDE

- SQL 源文件仍维护在 `forge-server/db/migration`，不要复制第二套到 `src/main/resources`。
- Maven 的 resources 阶段会把 SQL 不经占位符过滤地复制到 Admin 的 `target/classes/db/migration`。
- IDE 首次运行或新增 SQL 后，刷新 Maven 项目并构建资源；可在 `forge-server` 执行
  `mvn -pl forge-admin-server -am process-resources -DskipTests`。IDE 输出目录应包含相同的资源根。
- 确有外部路径需求时仍可通过 `FORGE_FLYWAY_LOCATIONS=filesystem:/绝对路径` 覆盖，
  但须自行保证该目录和应用版本一致；错误/缺失路径不会静默跳过。

## 部署检查

1. 确认目标是 Admin 服务，核对 unit、WorkingDirectory、ExecStart、EnvironmentFile 和活动 profile。
2. 核对生效的数据源与 `forge_schema_history`。不要误用门户的 `forge_website_schema_history`。
3. 确认 JAR 内所有已执行脚本与历史记录一致，明确 pending 列表。存在新迁移时，先审查 SQL、
   备份目标数据库并确认升级范围，不能将首次启动当作只读健康检查。
4. 备份该服务 EnvironmentFile、启动脚本及 unit；记录当前 JAR 的 SHA-256。
5. 确认没有旧路径覆盖，按需合并上面的可选配置，再重启 **Admin 单个服务**；不得顺带重启其它模块。
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

Admin 配置绑定及 SQL 原字节复制测试不启动应用、不连接数据库。在 `forge-server` 中执行：

```bash
mvn -pl forge-admin-server -am install -DskipTests
mvn -pl forge-admin-server test -Pdev,enable-tests -Dtest=FlywayClasspathConfigurationTest
```
