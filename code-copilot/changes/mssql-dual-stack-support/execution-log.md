# MSSQL 双轨支持 Execution Log

## 2026-11-18

- 自 `4e5d62ac9` 创建分支 `feat/mssql-dual-stack-support`
- 完成 SDD：`spec.md` / `tasks.md` / `test-spec.md`
- 阶段 1 落地：
  - Mapper `NOW()` → `CURRENT_TIMESTAMP`；`CAST AS CHAR` → `varchar(64)`
  - 协同回调失败退避改 Java；去掉 `DATE_ADD`/`POW`
  - `DatabaseIdProvider` 增加 Microsoft SQL Server → `sqlserver`
  - `DuplicateKeyActions` 识别 2627/2601 / PG 23505 / MySQL 1062
  - 新增并注册 `SqlServerRuntimeDatabaseDialect` + 单测
  - 相关单测通过（orm/generator/job/system）
- 未推送；下一步阶段 2 业务方言补强 / 阶段 3 平台 Schema

## 2026-11-18（阶段 2）

- 数据插件：`SqlServerDialect`、工厂别名、`JdbcUrlSchemaExtractor` 解析 `databaseName`
- 低代码方言：表/列 `MS_Description`、分页缺 ORDER BY 注入、modify 附注释
- 依赖：`mssql-jdbc` 进入 BOM / generator / admin-server / app-server
- 管理端数据源 URL 示例随库类型变化；补充 `business-runtime-notes.md`
- 相关单测通过后提交（不推送）

## 2026-11-18（阶段 3）

- 新增 `convert-mysql-full-to-sqlserver.py` / `check-sqlserver-full-basics.py` / `check-sqlserver-migration-pairs.mjs`
- 由 `ForgeAdmin-纯净版-latest.sql` 生成 `db/sqlserver/` 全量与纯净版（241 表）+ migration README（baseline 1.0.234）
- `init-db-sqlserver.sh`、`application-sqlserver.example.yml`
- Admin：`flyway-database-sqlserver` + classpath 拷贝 `db/sqlserver/migration`
- 更新 `db/README.md` / CHANGELOG；不强制本机 SQL Server 冒烟

## 2026-11-18（阶段 4）

- `SqlServerDialectSqlRewriter` / `SqlServerDialectSqlInterceptor`：FOR UPDATE→UPDLOCK，LIMIT→OFFSET/FETCH
- 协同 `claimPendingEvents` 改为 selectClaimCandidateIds + claimEventsByIds
- Quartz `QuartzDriverDelegateResolver`；Flowable `database-schema-update` 写入 sqlserver 示例
- 单测：rewriter / claim 契约 / Quartz resolver

## 2026-11-19（阶段 4 真库冒烟）

- Docker `mssql/server:2022`（linux/amd64）+ `docker-compose.mssql-test.yml`
- 导入全量/纯净版：需 `N'...'` Unicode 字面量、拆分 >1000 行 INSERT、qrtz empty varbinary→NULL；`sys_resource` 唯一索引与 MySQL NULL 语义差异需临时 drop
- `LIMIT n` 改写改为 `TOP (n)`（含子查询）；分页仍用 OFFSET/FETCH
- Admin/Flow health UP；登录 + 菜单权限 OK；Flowable 自建 act_/flw_ 共 62 表
