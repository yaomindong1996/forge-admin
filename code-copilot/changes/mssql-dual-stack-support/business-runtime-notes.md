# 业务库 SQL Server 联调说明（阶段 2）

平台主库仍默认 MySQL（可选 PostgreSQL）。本阶段打通的是 **业务侧** SQL Server：

- 低代码 / 代码生成「数据源」：`dbType=SQLServer`
- 数据资产「数据连接」：字典 `data_db_type` 值为 `SQLSERVER`
- 运行时方言：`SqlServerRuntimeDatabaseDialect`、`SqlServerDialect`

## 依赖

`admin-server` / `app-server` / `forge-plugin-generator` 已引入 `mssql-jdbc`（runtime / compile）。

## 连接串

```text
jdbc:sqlserver://主机:1433;databaseName=库名;encrypt=false
```

- 驱动：`com.microsoft.sqlserver.jdbc.SQLServerDriver`
- 测试语句：`SELECT 1`
- 本机开发常用 `encrypt=false`；生产按证书策略开启加密。

## 管理端操作

1. **系统 / 数据连接**：选 SQLServer，填主机、端口、库名；「测试连接」应成功。
2. **低代码数据源**：数据库类型选 `SQLServer`，驱动与连通语句会自动带出；URL 按上方示例填写。
3. 导入表 / 数据集拉元数据：应能列出当前库 `SCHEMA_NAME()`（通常 `dbo`）下的表。
4. 低代码建表 / 分页：DDL 使用 `[标识符]`、`nvarchar` / `datetime2` / `tinyint`；分页为 `OFFSET/FETCH`。

## 注意

- 开关字段继续用 **0/1 tinyint**，避免 `bit` 绑定差异。
- `sp_addextendedproperty` 写注释；重复执行改注释可能需先 drop 再 add（后续可补强）。
- **平台库** 迁到 SQL Server（Flyway / Flowable / Quartz）属于阶段 3–4，本说明不覆盖。
