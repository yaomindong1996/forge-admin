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
