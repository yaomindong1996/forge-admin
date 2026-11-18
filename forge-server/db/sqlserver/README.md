# SQL Server 全量初始化

- `全量初始化SQL.sql` / `全量初始化SQL.纯净版.sql`：当前均由社区 MySQL 纯净版 dump（`db/backup/ForgeAdmin-纯净版-latest.sql`，tip `1.0.234`）转换生成，保证结构齐全。
- 仓库内 `db/mysql/全量初始化SQL.sql` 可能落后于 Flyway tip；**不要**直接用它转 SQL Server，否则会缺表。
- 跳过 Flowable `act_*` / `flw_*`（引擎启动自建）；保留 Quartz `qrtz_*` DDL。
- 仅用于 **空库** 首次安装；已有业务库禁止重导全量。
- 脚本含 `GO` 批分隔，请用 `sqlcmd` 执行。

## 刷新（从 MySQL 纯净版 tip dump）

```bash
python3 forge-server/scripts/db/convert-mysql-full-to-sqlserver.py \
  --input forge-server/db/backup/ForgeAdmin-纯净版-latest.sql \
  --output forge-server/db/sqlserver/全量初始化SQL.纯净版.sql

cp forge-server/db/sqlserver/全量初始化SQL.纯净版.sql \
   forge-server/db/sqlserver/全量初始化SQL.sql

python3 forge-server/scripts/db/check-sqlserver-full-basics.py
```

胖演示种子：待 MySQL 全量对齐 tip 后再转换刷新 `全量初始化SQL.sql`。

## 初始化

```bash
# 需要本机 sqlcmd（SQL Server 命令行工具）
SQLCMDPASSWORD=*** bash forge-server/scripts/db/init-db-sqlserver.sh \
  --database forge_admin --recreate

# 纯净版（当前与全量同源）
SQLCMDPASSWORD=*** bash forge-server/scripts/db/init-db-sqlserver.sh \
  --database forge_admin --recreate --pure

# 导入后追平行增量（baseline 1.0.234）
SQLCMDPASSWORD=*** bash forge-server/scripts/db/init-db-sqlserver.sh \
  --database forge_admin --recreate --migrate
```

后端示例：`forge-admin-server/src/main/resources/application-sqlserver.example.yml`  
Flyway：`classpath:db/sqlserver/migration`，`baseline-version=1.0.234`
