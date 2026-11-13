# PostgreSQL 业务库 + 低代码双栈

## 目标

- 主库与低代码运行库均可选 PostgreSQL；MySQL 仍为社区默认。
- 全量：`db/postgresql/全量初始化SQL.sql`（由转换器从 MySQL 全量生成）。
- 增量：`db/postgresql/migration/`，与 MySQL `V1.0.x` 平行版本号；历史不逐条翻译。
- 低代码业务物理表：`PostgreSqlRuntimeDatabaseDialect` 运行时 DDL（全量 CREATE / 增量 ALTER）。
- 数据插件：`PostgreSqlDialect` 拉表/拉列参数绑定。

## 约定

| 项 | 约定 |
|----|------|
| 标识符 | 小写 + 双引号 |
| 开关/布尔 | `smallint`（0/1） |
| JSON | `jsonb` |
| 时间 | `timestamp`；无 `ON UPDATE`，靠 MetaObjectHandler |
| 主键 | 应用雪花，不依赖 IDENTITY |
| Flyway baseline | `1.0.234` |
| Flowable 表 | 全量跳过 `act_*`/`flw_*` |

## 成对增量治理

新增结构变更必须同时提交 MySQL 与 PG 同号脚本。可用检查：

```bash
node forge-server/scripts/db/check-pg-migration-pairs.mjs
```

## 非目标（本阶段）

- 不把社区默认安装切到 PG。
- 不逐条翻译历史 232 个 MySQL Flyway 文件。
