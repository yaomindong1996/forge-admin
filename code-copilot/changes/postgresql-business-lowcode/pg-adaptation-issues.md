# PostgreSQL 适配问题清单（社区回流）

> 权威约定：[critical-conventions §5.20](../../../docs/guides/critical-conventions.md)。  
> 底座 Spec：[spec.md](./spec.md)。  
> 本文件吸收校园/演示库实战沉淀中的**通用**项；不含校园专属种子与库名。

## 一句话

**SQL 看 `_databaseId`，绑定只信列元数据，DDL 先归一 PG 的 cast 字面量；改完跑方言扫描。**

## 问题总表

| # | 类别 | 现象 | 社区处理 |
|---|------|------|----------|
| A1 | 方言 | `DIV` / `TIMESTAMPDIFF` | Mapper `_databaseId` 分支（如 Job 日志 duration） |
| A2 | 方言 | `FIND_IN_SET` / 多表 UPDATE | 禁止裸写；`PortableCsvSql` / 便携写法 |
| A3 | 方言 | `CAST(... AS CHAR)` 截断 | 跨库用 `CONCAT(x)` |
| A4 | 方言 | `jsonb #>` MyBatis 冲突 | 用 `->` / `jsonb_extract_path` |
| A5 | 方言 | `JSON_LENGTH` | PG：`jsonb_array_length` + 成对分支 |
| A6 | 绑定 | `bigint = varchar`（路径 ID） | `coercePrimaryKeyValue` |
| A7 | 绑定 | `del_flag` 猜 Long | 无元数据保持 `"0"`/`"1"`；`coerceLogicFlagLiteral` |
| A8 | 绑定 | 非 public 列类型读不到 | `getColumns(null, schema, …)`，勿用 catalog 过滤 |
| A9 | 数据权限 | flowRelatedVisible 误塞 businessKey | 约定禁止；业务侧关闭错误开关 |
| A10–A12 | DDL | DEFAULT / 类型别名 / 超时 | 已有 `SqlStoredDefaultNormalizer` + 前端超时 |
| A13 | 性能 | apps 列表慢 | 另波优化 |
| A14–A15 | 初始化/JSON | 全量丢表、jsonb 空格 | 修全量转换；种子写紧凑 JSON |
| A16 | 迁移 | 双库漂移 | `check-pg-migration-pairs.mjs` |

## 门禁

```bash
pnpm db:check-mapper-dialect
node forge-server/scripts/db/check-pg-migration-pairs.mjs
```

## 低代码运行方言（摘要）

| 项 | PG 约定 |
|----|---------|
| 标识符 | 小写 + 双引号 |
| 开关/布尔 | `smallint`（0/1） |
| JSON | `jsonb` |
| 时间 | `timestamp`；无 `ON UPDATE` |
| 主键 | 应用雪花 |
| Flyway baseline | `1.0.234` |
