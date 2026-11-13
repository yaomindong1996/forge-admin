# PostgreSQL Flyway 增量（与 MySQL 平行版本号）

## 规则

1. **历史 MySQL 脚本不逐条翻译**。PG 空库使用 `db/postgresql/全量初始化SQL.sql` 导入后，将 `forge_schema_history` **baseline 到 `1.0.234`**（全量冻结点，与转换时的 MySQL 全量对齐）。
2. 自下一版结构变更起，必须 **成对提交**：
   - MySQL：`db/mysql/migration/V1.0.xxx__desc.sql`
   - PostgreSQL：`db/postgresql/migration/V1.0.xxx__desc.sql`（同版本号、同业务语义）
3. 禁止在 PG 增量中使用 MySQL 过程式 `PREPARE`/`IFNULL`/`FIND_IN_SET`/`ENGINE=InnoDB` 等方言。
4. 社区默认安装仍走 MySQL；启用 PG 时配置：

```yaml
spring.flyway.locations: classpath:db/postgresql/migration
spring.flyway.baseline-on-migrate: true
spring.flyway.baseline-version: 1.0.234
```

参见 `application-postgresql.example.yml`。

## 当前目录

冻结点之后若尚无新增量，本目录可仅保留本说明。新增第一个平行脚本时从 `V1.0.235`（或当时最新 +1）开始。
