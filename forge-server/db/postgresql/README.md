# PostgreSQL 全量初始化

- `全量初始化SQL.sql`：由 `forge-server/scripts/db/convert-mysql-full-to-pg.py` 从 MySQL `db/全量初始化SQL.sql` 转换生成。
- 仅用于 **空库** 首次安装；已有业务库禁止重导全量。
- Flowable `act_*` / `flw_*` 不在全量中，由引擎启动创建。
- 刷新步骤：

```bash
python3 forge-server/scripts/db/convert-mysql-full-to-pg.py \
  --input forge-server/db/全量初始化SQL.sql \
  --output forge-server/db/postgresql/全量初始化SQL.sql
```

初始化：

```bash
PGPASSWORD=*** bash forge-server/scripts/db/init-db-pg.sh --database forge_admin --recreate
```
