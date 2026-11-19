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

- [x] DDL / 分页 / 元数据 / 聚合等与 Oracle/PG 对标补强（扩展属性注释、OFFSET 缺 ORDER BY 注入）
- [x] 数据资产 `SqlServerDialect` + JDBC `databaseName` 解析 + `mssql-jdbc` 依赖
- [x] 管理端 SQLServer URL 提示与 `business-runtime-notes.md` 联调说明

## 阶段 3 — 平台 Schema

- [x] `db/sqlserver/` 全量转换脚本 + README
- [x] baseline `1.0.234` + 平行增量约定
- [x] 示例 `application-*.yml` / 驱动与 Flyway SQL Server 依赖

## 阶段 4 — 引擎与锁语义

- [x] `FOR UPDATE` / 批量 `UPDATE ... LIMIT` 可移植或方言化（拦截器 + 协同两段式 claim）
- [x] Flowable SQL Server（schema-update 示例配置）
- [x] Quartz `MSSQLDelegate` 按库自动选择（表脚本阶段 3 已有）

## 阶段 5–6 — 矩阵与 Pro 同步

- [x] SQL Server 真库 Docker 冒烟（health / 登录 / 组织 / 任务 / 打印来源 / Flow 模型）+ `smoke-mssql-apis.mjs`
- [x] 转换器一键导入加固与脚本校验（`check-sqlserver-full-basics.py` / `check-sqlserver-migration-pairs.mjs`）
- [ ] 三栈 CI 矩阵（MySQL / PG / MSSQL）流水线挂载（可选后续）
- [ ] 社区合入后增量同步 ForgePro（暂缓）
