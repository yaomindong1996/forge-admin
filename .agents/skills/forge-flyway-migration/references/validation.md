# 迁移验证与故障定位

## 选择迁移入口

- Admin 主库按 `forge-server/db/{mysql,postgresql,sqlserver}/migration/` 维护方言脚本。Admin 与 Flow 共用主库，先通过 Admin 完成迁移再验证消费端。初始化 SQL 单独依照现有生成/导出流程更新。
- PostgreSQL 与 SQL Server 的全量基线冻结点为 `1.0.234`。按 [PostgreSQL 迁移说明](../../../../forge-server/db/postgresql/migration/README.md)和 [SQL Server 迁移说明](../../../../forge-server/db/sqlserver/migration/README.md)选择与实际快照匹配的 profile 和 `classpath:db/<方言>/migration`。导入前检查快照是否内含 `forge_schema_history` 及其水位。有旧历史时 `baseline-on-migrate` 不会替换，需先在隔离库确认当前快照能校验并按本方言迁移。存量库遇到缺失或不匹配应保留历史，核对正确的快照与迁移来源。
- 已注册插件按 `PluginFlywayFactory` 解析迁移目录和历史表。先查插件描述符以及 [插件迁移集成测试](../../../../forge-server/forge-framework/forge-starter-parent/forge-starter-plugin/src/test/java/com/mdframe/forge/starter/plugin/migration/PluginFlywayMigrationIntegrationTest.java)，避免把插件脚本放入全局历史。
- 跨方言实现分别对照现有脚本的身份列、标识符引用、索引、UPSERT 与 DDL 事务行为。SQL Server、PostgreSQL 和 MySQL 的重复保护写法按本库真实语义验证，不能用另一个引擎的兼容模式替代。

## 合并前检查

以下命令在社区模板根目录运行。生成工程按实际服务目录核对脚本与生效配置。

```bash
node forge-server/scripts/db/next-migration-version.mjs
node forge-server/scripts/db/check-pg-migration-pairs.mjs
node forge-server/scripts/db/check-sqlserver-migration-pairs.mjs
python3 scripts/check-mapper-dialect.py
for dialect in mysql postgresql sqlserver; do
  ls "forge-server/db/$dialect/migration/"V*.sql | sed 's/__.*//' | sort | uniq -d
done
```

查重以无输出为通过。编号冲突时查已部署的 `forge_schema_history` 和描述，保留执行过的脚本，只重编号尚未执行的一方并同步三库。评估回滚时区分恢复应用与数据库变化，必要时设计向前修复迁移或从备份恢复。

已获准核对 MySQL 版本时用 `--from-db`，连接信息可通过 `FORGE_FLYWAY_JDBC_URL`、`FORGE_DB_USERNAME`、`FORGE_DB_PASSWORD` 注入。对 PostgreSQL、SQL Server 从目标方言的历史表做只读核对，取文件和历史的最大水位。

对本次新增脚本用 `rg -n '\$\{[^}]+\}'` 检查 Flyway 占位符。要写入字面的业务模板键时按[自动化测试标准](../../../../code-copilot/rules/automated-testing-standard.md)选择方言相应的拼接写法，并检查落库文本。

## 数据库检验

1. 在临时库从当前分支跑空库初始化；确认目标表、索引、种子数据和唯一键正确。
2. 另一临时库先迁移至上一版本，造接近存量的记录（含已有种子和删除行），再升级。保存迁移前后业务查询、角色关联和历史表差异。权限种子以现有父资源与授权角色为条件，验证其他角色和租户的授权数量不变。
3. 重启后确认没有待执行迁移，`validate` 成功。具备重复保护的 DML 在隔离库可单独执行第二遍，确认无二次插入；保留 Flyway 历史。
4. 对于启动、路径或打包变更，核对实际产物的 `BOOT-INF/classes/db/<方言>/migration/`、活跃 profile、环境变量和 JVM 参数。检查历史表的 `version`、`description`、`script`、`checksum` 和 `success`，比对目标库和实际 JAR 的 SQL，排除旧 `filesystem:` 覆盖。
5. 在变更执行日志中记方言、起始/目标版本、执行命令、结果、未覆盖环境和恢复方案。[采购导出验收](../../forge-resource-permissions/references/purchase-export.md)提供旧库授权增量、空库与行范围的本地回归示例。
