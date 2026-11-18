# MSSQL 双轨支持 Test Spec

## 阶段 1（本批）

| 项 | 方式 | 通过标准 |
|----|------|----------|
| `NOW()` 残留 | `rg` Mapper XML/注解 | 业务 Mapper 无裸 `NOW()`（引擎/第三方脚本除外） |
| CHAR cast | `rg` | 无 `CAST(... AS CHAR)` |
| DATE_ADD | `rg` | SocialCallback 无 `DATE_ADD` |
| DatabaseId | 单测或配置审阅 | SQL Server productName → `sqlserver` |
| DuplicateKey | 单测 | 2627/2601 判定为重复键 |
| SqlServer 方言 | 单测 | 工厂可解析；分页/标识符等基础行为正确 |
| 回归 | `-Penable-tests` 相关模块 | 既有 MySQL/H2 路径单测通过 |

## 阶段 2

| 项 | 方式 | 通过标准 |
|----|------|----------|
| 数据方言工厂 | 单测 | SQLSERVER/mssql 别名可解析 |
| SqlServerDialect | 单测 | 标识符/分页/绑参防注入 |
| JDBC URL | 单测 | `databaseName=` 可解析 |
| 低代码方言补强 | 单测 | 注释扩展属性、缺 ORDER BY 注入 |
| 驱动 | pom 审阅 | admin/app/generator 含 mssql-jdbc |

## 阶段 3

| 项 | 方式 | 通过标准 |
|----|------|----------|
| 全量脚本 | `check-sqlserver-full-basics.py` | CREATE TABLE 充足、无 ENGINE、含 GO |
| 平行增量 | `check-sqlserver-migration-pairs.mjs` | `>1.0.234` 与 MySQL 成对（可为空） |
| 配置 | 审阅 | example yml + Admin 拷贝 `db/sqlserver/migration` |
| Flyway 依赖 | 解析/编译 | 含 `flyway-sqlserver`（与 Flyway 10.20.x 构件名一致） |

## 阶段 4

| 项 | 方式 | 通过标准 |
|----|------|----------|
| FOR UPDATE / LIMIT 改写 | 单测 | UPDLOCK / OFFSET FETCH；LIMIT OFFSET 参数交换标记 |
| 协同 claim | XML 契约 | 两段式；UPDATE 无 ORDER BY LIMIT |
| Quartz Delegate | 单测 | SQL Server→MSSQLDelegate |
| Flowable 配置 | 审阅 | sqlserver 示例含 database-schema-update=true |

## 后续阶段

- SQL Server 真库：Flyway migrate、登录、组织权限、流程、定时任务冒烟
- 低代码：建表/改列/分页查询（真库）
- 三栈平行增量脚本一致性检查
