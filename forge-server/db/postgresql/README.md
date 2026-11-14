# PostgreSQL 全量初始化

- `全量初始化SQL.sql`：由 `forge-server/scripts/db/convert-mysql-full-to-pg.py` 从 MySQL `db/mysql/全量初始化SQL.sql` 转换生成（含演示/业务种子）。
- `全量初始化SQL.纯净版.sql`：同转换器加 `--clean-seed`，保留全部框架表 DDL，但去掉 clean-db 会清空的日志/流程/低代码/演示等种子行；适合空库装「干净模板」。
- 仅用于 **空库** 首次安装；已有业务库禁止重导全量。
- Flowable `act_*` / `flw_*` 不在全量中，由引擎启动创建。
- 刷新步骤：

```bash
# 完整版（含演示数据）
python3 forge-server/scripts/db/convert-mysql-full-to-pg.py \
  --input forge-server/db/mysql/全量初始化SQL.sql \
  --output forge-server/db/postgresql/全量初始化SQL.sql

# 纯净版（DDL + 基础数据）
python3 forge-server/scripts/db/convert-mysql-full-to-pg.py \
  --input forge-server/db/mysql/全量初始化SQL.sql \
  --output forge-server/db/postgresql/全量初始化SQL.纯净版.sql \
  --clean-seed
```

初始化：

```bash
# 默认导入完整版全量
PGPASSWORD=*** bash forge-server/scripts/db/init-db-pg.sh --database forge_admin --recreate

# 纯净版模板库
PGPASSWORD=*** bash forge-server/scripts/db/init-db-pg.sh \
  --database forge_admin --recreate --pure
```
