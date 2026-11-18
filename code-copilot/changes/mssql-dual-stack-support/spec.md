# MSSQL 双轨支持 Spec

> 变更名：`mssql-dual-stack-support`
> 状态：`in-progress`
> 创建日期：2026-11-18
> 分支：`feat/mssql-dual-stack-support`

## 1. 需求

同时支持：

1. **业务库**：低代码 / 动态数据源可选 SQL Server（管理端已有 `SQLServer` 选项与驱动名，缺运行时方言）。
2. **平台库**：Admin / Flow / Job 等主库可整体运行在 SQL Server（对标现有 PostgreSQL 可选栈）。

社区默认仍为 MySQL；MSSQL 为可选第三栈。ForgePro 跟随社区增量同步。

## 2. 现状

- 已有 MySQL 默认 + PostgreSQL 可选双栈：`db/{mysql,postgresql}/`、全量转换、baseline=`1.0.234`。
- 可移植基础：`PortableCsvSql`、`DuplicateKeyActions`、`DatabaseIdProvider`（仅 mysql/postgresql）。
- 低代码方言 SPI：`RuntimeDatabaseDialect` + MySQL / PostgreSQL / Oracle；**无 SQL Server 实现**。
- Mapper 高危方言大多已清；仍大量 `NOW()`（约 63 文件）、`FOR UPDATE`（约 25 文件）、`CAST(... AS CHAR)`、协同回调 `DATE_ADD`+`UPDATE LIMIT`。

## 3. 方案要点

### 类型与布尔

- 开关 / 标志继续 **0/1 字面量**（优先 `tinyint`，慎用 `bit` 绑定）。
- 文本倾向 `nvarchar`；`datetime` → `datetime2`；`json`/`longtext` → `nvarchar(max)`。
- 标识符引用：`[name]`。

### Schema

- 对齐 PG：`db/sqlserver/` 全量（由 MySQL 全量转换）+ baseline `1.0.234` + 之后 **mysql/pg/sqlserver 平行增量**。
- 不逐条翻译 233 条历史 MySQL 迁移。

### 静态 SQL

- 可移植优先：`CURRENT_TIMESTAMP`、`CASE`、`CAST(... AS varchar(n))`；退避时间在 Java 计算。
- `FOR UPDATE` / `UPDATE ORDER BY LIMIT` 等锁与批量领取单独改为 SQL Server 可执行形态（或 Java 两段式），并回归租户 JSqlParser。

### 低代码

- 新增 `SqlServerRuntimeDatabaseDialect` 并注册到工厂；覆盖 DDL、分页 `OFFSET/FETCH`、元数据、聚合。

### 引擎

- Flowable：SQL Server schema 与主路径回归。
- Quartz：`MSSQLDelegate` + SQL Server 表脚本。

## 4. 分期

见 `tasks.md`。阶段 1（可移植 SQL + DatabaseId + 方言骨架）可先合入，对现网 MySQL/PG 亦为正收益。

## 5. 不在首批

- 一次宣称生产级性能调优完毕。
- 为 MSSQL 大规模引入 Mapper `_databaseId` 丛林。
- 修改已执行的 Flyway 历史脚本内容。

## 6. 风险

- 全量 Schema 转换质量；租户 SQL 锁重写；Flowable/Quartz 真库行为；低代码 `ALTER COLUMN` 语义。
- H2 无法替代 SQL Server 真库测试。
