# MSSQL 双轨支持 Tasks

## 阶段 0 — SDD

- [x] 创建变更目录与 `spec.md` / `tasks.md` / `test-spec.md` / `execution-log.md`
- [x] 建立分支 `feat/mssql-dual-stack-support`

## 阶段 1 — 可移植 SQL + DatabaseId + 方言骨架（本批）

- [x] Mapper：`NOW()` → `CURRENT_TIMESTAMP`（业务 XML / 注解 SQL）
- [x] `CAST(... AS CHAR)` → `CAST(... AS varchar(n))`（可移植）
- [x] 协同 `SocialCallbackEventMapper.markFailed`：退避时间改 Java 计算，去掉 `DATE_ADD`/`POW`
- [x] `MybatisPlusConfig.databaseIdProvider` 增加 SQL Server → `sqlserver`
- [x] `DuplicateKeyActions` 识别 SQL Server 唯一冲突（2627 / 2601）
- [x] 新增 `SqlServerRuntimeDatabaseDialect` 并注册；补充单测
- [x] `CHANGELOG.md` 记录阶段 1
- [x] 相关单测 `-Penable-tests`；提交（不推送除非授权）

## 阶段 2 — 业务库方言补齐

- [ ] DDL / 分页 / 元数据 / 聚合等与 Oracle/PG 对标补强
- [ ] 管理端 SQLServer 选项与运行时联调说明

## 阶段 3 — 平台 Schema

- [ ] `db/sqlserver/` 全量转换脚本 + README
- [ ] baseline `1.0.234` + 平行增量约定
- [ ] 示例 `application-*.yml` / 驱动与 Flyway SQL Server 依赖

## 阶段 4 — 引擎与锁语义

- [ ] `FOR UPDATE` / 批量 `UPDATE ... LIMIT` 可移植或方言化
- [ ] Flowable SQL Server
- [ ] Quartz `MSSQLDelegate` + 表脚本

## 阶段 5–6 — 矩阵与 Pro 同步

- [ ] 三栈冒烟与 CI 矩阵
- [ ] 社区合入后增量同步 ForgePro
