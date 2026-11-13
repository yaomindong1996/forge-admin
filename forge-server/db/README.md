# Forge 数据库脚本

按数据库类型划分全量与增量脚本：

```text
forge-server/db/
├── mysql/
│   ├── 全量初始化SQL.sql      # MySQL 空库全量（含必需基础数据）
│   └── migration/             # MySQL Flyway 增量 V*.sql
├── postgresql/
│   ├── 全量初始化SQL.sql      # PostgreSQL 空库全量（由 MySQL 全量转换）
│   └── migration/             # PostgreSQL Flyway 增量（与 MySQL 平行版本号）
├── backup/                    # 历史备份 / 旧脚本归档（不参与运行）
└── README.md
```

社区默认安装使用 **MySQL**。PostgreSQL 为可选双栈，详见 `postgresql/README.md`。

---

## 一、首次初始化（MySQL）

准备：MySQL 8+、JDK 17+、Node.js 20+（部分检查脚本需要）。

1. 复制并编辑本地配置：

```bash
cp forge-server/forge-admin-server/src/main/resources/application-dev.example.yml \
  forge-server/forge-admin-server/src/main/resources/application-dev.yml
```

2. 一键初始化：

```bash
MYSQL_PWD=your_password bash forge-server/scripts/db/init-db.sh \
  --host 127.0.0.1 \
  --port 3306 \
  --database forge_admin \
  --user root
```

脚本会：

1. 创建数据库 `forge_admin`
2. 执行 `db/mysql/全量初始化SQL.sql`

表结构增量由 `forge-admin-server` 启动时的 Flyway 执行 `classpath:db/mysql/migration`。也可加 `--migrate` 立即执行（需要 Maven）。

干净模板库（重建 + 增量 + 清理）：

```bash
MYSQL_PWD=your_password bash forge-server/scripts/db/init-db.sh \
  --database my_project --recreate --clean
```

---

## 二、PostgreSQL（可选）

刷新全量（从 MySQL 全量转换）：

```bash
python3 forge-server/scripts/db/convert-mysql-full-to-pg.py \
  --input forge-server/db/mysql/全量初始化SQL.sql \
  --output forge-server/db/postgresql/全量初始化SQL.sql
```

空库初始化：

```bash
PGPASSWORD=*** bash forge-server/scripts/db/init-db-pg.sh \
  --database forge_admin --recreate
```

增量目录：`db/postgresql/migration/`，与 MySQL `V1.0.x` 平行版本号；全量冻结 baseline=`1.0.234`。

成对检查：

```bash
node forge-server/scripts/db/check-pg-migration-pairs.mjs
```

后端示例：`forge-admin-server/src/main/resources/application-postgresql.example.yml`  
Flyway 默认：`classpath:db/postgresql/migration`

---

## 三、Flyway 约定

| 库 | 源目录 | 打包 / 默认扫描 |
|---|---|---|
| MySQL | `db/mysql/migration` | `classpath:db/mysql/migration` |
| PostgreSQL | `db/postgresql/migration` | `classpath:db/postgresql/migration` |

- 仅 `forge-admin-server` 启动时执行迁移；单独启动 `forge-report-server` 不会执行。
- Admin POM 在 `process-resources` 阶段把上述目录原样拷入 JAR（不经占位符过滤）。
- 覆盖扫描路径时使用环境变量 `FORGE_FLYWAY_LOCATIONS`（见 `scripts/deployment/admin-flyway.md`）。
- 新增结构变更：在对应库目录新增 `V1.0.x__desc.sql`；若启用 PG，需成对提交。

历史表：`forge_schema_history`。

---

## 四、常用命令

```bash
bash forge-server/scripts/db/init-db.sh --help

MYSQL_PWD=*** bash forge-server/scripts/db/init-db.sh \
  --database forge_admin --recreate --migrate

MYSQL_PWD=*** bash forge-server/scripts/db/clean-db.sh \
  --database my_project --execute

PGPASSWORD=*** bash forge-server/scripts/db/init-db-pg.sh \
  --database forge_admin --recreate --migrate
```

---

## 五、变更流程

1. 新增 MySQL 增量：`db/mysql/migration/Vx.y.z__xxx.sql`
2. 若维护 PG：同步新增 `db/postgresql/migration/Vx.y.z__xxx.sql`
3. 跑成对检查（启用 PG 时）：`node forge-server/scripts/db/check-pg-migration-pairs.mjs`
4. 本地用 `init-db.sh --migrate` 或启动 Admin 验证

已移除：`db/seed`、`db/community-export`（基础数据以全量脚本为准，演示/社区导出不再作为仓库目录维护）。
