# PostgreSQL 全量初始化

- `全量初始化SQL.sql`：空库全量（含演示/业务种子，偏「胖」）。表和列必须覆盖 `全量初始化SQL.纯净版.sql`，并保留纯净版没有的演示表。
- `全量初始化SQL.纯净版.sql`：**正确来源**是社区 MySQL 纯净版 dump（`clean-db.sh` + `dump-safe-full.sh`），再转换；**不是**对仓库全量 SQL 做 `--clean-seed` 裁剪。
  - 当前由 `db/backup/ForgeAdmin-纯净版-latest.sql`（MySQL tip `1.0.234` 经 clean-db + dump-safe-full）转换生成。
  - 仅默认租户 / `admin`、菜单字典参数等基础数据；无 `act_*`/`flw_*`。
- 仅用于 **空库** 首次安装；已有业务库禁止重导全量。

## 刷新完整版（从仓库 MySQL 全量）

当前 `db/mysql/全量初始化SQL.sql` 落后于 Flyway：没有密码历史、流程候选、用户组、业务流程等后来加进迁移的表和列。MySQL 安装导入后还会跑增量，所以能补齐。PostgreSQL 增量从 `1.0.234` 基线起算，导入本目录全量后不会重放更早的迁移。直接把那份 MySQL 全量转成 `全量初始化SQL.sql` 会让新的 PG 库缺表，候选待办、改密、业务流程在运行时失败。

先把 MySQL 全量刷新到与纯净版相同的结构，再转换；转换后必须通过覆盖检查：

```bash
python3 forge-server/scripts/db/convert-mysql-full-to-pg.py \
  --input forge-server/db/mysql/全量初始化SQL.sql \
  --output forge-server/db/postgresql/全量初始化SQL.sql
python3 forge-server/scripts/db/check-pg-full-superset.py
```

## 刷新纯净版（从 MySQL 纯净版 dump）

先按 `.agents/skills/forge-project-init/references/pure-sql-export.md` 导出 MySQL 纯净版，再：

```bash
python3 forge-server/scripts/db/convert-mysql-full-to-pg.py \
  --input forge-server/db/backup/ForgeAdmin-纯净版-latest.sql \
  --output forge-server/db/postgresql/全量初始化SQL.纯净版.sql
```

> 不要用 `--clean-seed` 把「全量」冒充纯净版：那只会按表名丢掉部分 INSERT，测租户/测账号和缺表问题仍在。

## 初始化

```bash
# 完整版
PGPASSWORD=*** bash forge-server/scripts/db/init-db-pg.sh \
  --database forge_admin --recreate

# 纯净版（结构以纯净 dump 为准；若代码更新可再 --migrate 追 PG 增量）
PGPASSWORD=*** bash forge-server/scripts/db/init-db-pg.sh \
  --database forge_admin --recreate --pure

# 纯净版 + 追增量
PGPASSWORD=*** bash forge-server/scripts/db/init-db-pg.sh \
  --database forge_admin --recreate --pure --migrate
```
