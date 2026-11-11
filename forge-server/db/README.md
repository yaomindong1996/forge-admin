一、首次初始化数据库

1. 准备环境

需要本机有：

mysql --version
node --version
java -version

推荐：

- MySQL 8+（社区默认）
- 可选 PostgreSQL 14+（业务库 / 低代码 / 可选主库双栈，见下文「PostgreSQL」）
- Node.js 20+
- JDK 17+

如果 java -version 不是 17+，后端编译会报：

无效的目标发行版: 17

需要切换到 JDK 17。

  ---
2. 复制后端本地配置

cp forge-server/forge-admin-server/src/main/resources/application-dev.example.yml \
forge-server/forge-admin-server/src/main/resources/application-dev.yml

然后编辑：

forge-server/forge-admin-server/src/main/resources/application-dev.yml

把 MySQL、Redis 配置改成你的本地环境，例如：

spring:
datasource:
dynamic:
datasource:
master:
url: jdbc:mysql://localhost:3306/forge_admin?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2B8&allowPublicKeyRetrieval=true
username: root
password: your_password

  ---
3. 一键初始化数据库

在项目根目录执行：

bash forge-server/scripts/db/init-db.sh \
--host 127.0.0.1 \
--port 3306 \
--database forge_admin \
--user root \
--password your_password

这个命令会依次执行：

1. 创建数据库 forge_admin
2. 执行 forge-server/db/全量初始化SQL.sql
3. 执行 forge-server/db/seed/required/*.sql

表结构增量变更由主后台服务启动时的 Flyway 执行 `forge-server/db/migration`。加 `--migrate` 可以在脚本里直接执行增量（需要 Maven），效果与后端启动时相同。

说明：

- 密码建议用 `MYSQL_PWD=*** bash ...` 传入。脚本内部通过临时 defaults 文件把密码交给 mysql，不会出现在进程参数里。
- 全量 SQL 只能导入空库。库里已经有表时脚本会直接失败；要重建请加 `--recreate`，执行前会要求输入库名确认。

  ---
3.0 PostgreSQL（可选双栈）

社区默认仍是 MySQL。若业务库 / 低代码运行库 / 主库使用 PostgreSQL：

1. 刷新全量（从 MySQL 全量转换，可重复执行）：

```bash
python3 forge-server/scripts/db/convert-mysql-full-to-pg.py \
  --input forge-server/db/全量初始化SQL.sql \
  --output forge-server/db/postgresql/全量初始化SQL.sql
```

2. 空库初始化：

```bash
PGPASSWORD=*** bash forge-server/scripts/db/init-db-pg.sh \
  --database forge_admin --recreate
```

3. 增量目录：`forge-server/db/migration/postgresql/`，与 MySQL `V1.0.x` 平行版本号；全量冻结 baseline=`1.0.233`。成对检查：

```bash
node forge-server/scripts/db/check-pg-migration-pairs.mjs
```

4. 后端示例配置：`forge-admin-server/src/main/resources/application-postgresql.example.yml`。

5. Docker 可选：`docker compose --profile postgres up -d`（见 `docker-forge-admin/docker-compose.yml`）。

详情：`db/postgresql/README.md`、`db/migration/postgresql/README.md`、`code-copilot/changes/postgresql-business-lowcode/spec.md`。

  ---
3.1 初始化一个干净的模板库（新项目推荐）

```bash
MYSQL_PWD=your_password bash forge-server/scripts/db/init-db.sh \
  --database my_project --recreate --clean
```

依次执行：重建库 → 全量 SQL → required seed → Flyway 增量 → `clean-db.sh` 清理。最终只保留：

- 默认租户、超级管理员 admin（含角色和根组织）；
- 菜单权限、字典、系统参数、行政区划、定时任务配置、内置模板；
- `forge_schema_history`，后续新增的迁移会继续增量执行。

日志、流程、低代码应用及其自动建的表、测试用户/租户/组织/角色、报表、AI 配置、文件记录都会被清空。

没有 Maven 时，分两步做：

1. `init-db.sh --recreate` 导入后，启动一次 admin，让 Flyway 执行增量；
2. 再清理：

```bash
MYSQL_PWD=your_password bash forge-server/scripts/db/clean-db.sh --database my_project           # 预览
MYSQL_PWD=your_password bash forge-server/scripts/db/clean-db.sh --database my_project --execute # 执行
```

注意：

- 清理必须在增量之后。部分迁移脚本会写入演示数据，先清理再迁移，数据会被写回来。增量没执行完时 `clean-db.sh` 会拒绝执行。
- `clean-db.sh` 是物理删除，只能用于模板库或开发库。执行前可以加 `--backup-file backup.sql` 先备份。
- 其它常用参数：
  - `--keep-table` 保护指定表；
  - `--keep-business-tables` 让业务表只清空不删除；
  - `--extra-sql` 追加项目自定义清理 SQL；
  - `--print-sql` 查看完整 SQL。
- 通过 `init-db.sh` 传给 `clean-db.sh` 的参数，用 `--clean-arg` 逐个透传。

  ---
4. 如果需要导入演示数据

bash forge-server/scripts/db/init-db.sh \
--host 127.0.0.1 \
--port 3306 \
--database forge_admin \
--user root \
--password your_password \
--with-demo

会额外执行：

forge-server/db/seed/demo/*.sql

  ---
5. 如果需要导入可选模块数据

bash forge-server/scripts/db/init-db.sh \
--host 127.0.0.1 \
--port 3306 \
--database forge_admin \
--user root \
--password your_password \
--with-optional

会额外执行：

forge-server/db/seed/optional/*.sql

  ---
6. 同时导入 demo 和 optional

bash forge-server/scripts/db/init-db.sh \
--host 127.0.0.1 \
--port 3306 \
--database forge_admin \
--user root \
--password your_password \
--with-demo \
--with-optional

  ---
二、启动项目

1. 启动后端

cd forge-server/forge-admin-server
mvn spring-boot:run

默认地址：

http://localhost:8580

  ---
2. 启动前端

cd forge-admin-ui
pnpm install
pnpm dev

默认登录账号：

admin / 123456

  ---
三、启用 Flyway 自动迁移

默认情况下，主后台服务启动时会执行 Flyway，扫描 `forge-server/db/migration/*.sql`。单独启动 `forge-report-server` 不会执行这些迁移。

如果需要临时关闭：

export FORGE_FLYWAY_ENABLED=false

启动后端：

cd forge-server/forge-admin-server
mvn spring-boot:run

默认迁移目录是：

forge-server/db/migration

配置在：

spring:
flyway:
enabled: ${FORGE_FLYWAY_ENABLED:true}
locations: ${FORGE_FLYWAY_LOCATIONS:filesystem:./db/migration,filesystem:../db/migration,filesystem:forge-server/db/migration}
baseline-on-migrate: true
baseline-version: 1.0.0
table: forge_schema_history

  ---
四、以后新增数据库结构变更怎么做

以后不要再手工导完整 SQL。

比如你要给 data_business 表加字段，就新增文件：

forge-server/db/migration/V1.0.1__alter_data_business_add_ai_fields.sql

内容示例：

ALTER TABLE data_business
ADD COLUMN analysis_goal varchar(1000) DEFAULT NULL COMMENT '分析目标',
ADD COLUMN metric_definition text DEFAULT NULL COMMENT '指标口径说明';

命名规则必须是：

V版本号__说明.sql

例如：

V1.0.1__add_user_profile_table.sql
V1.0.2__alter_sys_resource_add_open_type.sql
V1.0.3__create_dashboard_template.sql

注意：

- 只能用小写英文、数字、下划线。
- 不要用中文文件名。
- 不要写真实业务数据。
- 新建表要包含 Forge 标准字段。

  ---
五、以后新增初始化数据怎么做

初始化数据分三类。

1. 系统必需数据

放到：

forge-server/db/seed/required/

例如：

forge-server/db/seed/required/R__sys_resource.sql
forge-server/db/seed/required/R__sys_dict.sql
forge-server/db/seed/required/R__sys_config.sql

适合放：

菜单
权限
字典
系统配置
AI 模板
框架启动必需数据

  ---
2. 演示数据

放到：

forge-server/db/seed/demo/

例如：

forge-server/db/seed/demo/D__demo_data_business.sql
forge-server/db/seed/demo/D__demo_dashboard_project.sql
forge-server/db/seed/demo/D__demo_dataset.sql

适合放：

示例业务定义
示例数据集
示例大屏
演示账号以外的数据

默认不会导入，只有执行 --with-demo 才会导入。

  ---
3. 可选模块数据

放到：

forge-server/db/seed/optional/

例如：

forge-server/db/seed/optional/O__workflow_demo.sql
forge-server/db/seed/optional/O__message_template.sql

默认不会导入，只有执行 --with-optional 才会导入。

  ---
六、社区数据库导出怎么用

1. 先 dry-run 检查

bash forge-server/scripts/db/export-community-db.sh --dry-run

这个命令会检查：

1. migration 文件命名是否规范
2. seed 文件命名是否规范
3. 白名单表配置
4. 黑名单表配置
5. 敏感字段规则
6. 输出导出摘要

  ---
2. 确认无误后正式导出

bash forge-server/scripts/db/export-community-db.sh

导出结果在：

forge-server/db/community-export/

里面会生成：

forge-server/db/community-export/migration/
forge-server/db/community-export/seed/
forge-server/db/community-export/schema-summary.json
forge-server/db/community-export/seed-summary.json
forge-server/db/community-export/summary.md

这个目录已经加入 .gitignore，默认不会提交。

  ---
七、社区同步白名单和黑名单在哪里改

配置文件：

forge-server/scripts/db/community-db.config.json

里面有三类关键配置。

1. 允许导出的表

"allowTables": [
"sys_resource",
"sys_dict",
"sys_config",
"sys_role",
"sys_role_resource",
"ai_provider_template",
"ai_agent",
"ai_model",
"data_business_template",
"dashboard_template"
]

这些是可以同步到社区版的系统基础数据。

  ---
2. 禁止导出的表

"denyTables": [
"sys_user",
"sys_user_role",
"ai_chat_record",
"ai_chat_session",
"ai_dashboard_generate_record"
]

这些不应该同步到社区版。

  ---
3. 敏感字段

"sensitiveColumns": [
"password",
"token",
"secret",
"key",
"phone",
"email",
"access_key",
"secret_key"
]

导出 SQL 里如果包含这些字段或敏感格式，检查脚本会失败。

  ---
八、常用命令汇总

查看初始化脚本帮助

bash forge-server/scripts/db/init-db.sh --help

初始化数据库

bash forge-server/scripts/db/init-db.sh \
--database forge_admin \
--user root \
--password your_password

初始化数据库并导入 demo

bash forge-server/scripts/db/init-db.sh \
--database forge_admin \
--user root \
--password your_password \
--with-demo

初始化干净模板库（全量 + 增量 + 清理）

MYSQL_PWD=your_password bash forge-server/scripts/db/init-db.sh --database my_project --recreate --clean

预览模板库清理结果

MYSQL_PWD=your_password bash forge-server/scripts/db/clean-db.sh --database my_project

社区导出 dry-run

bash forge-server/scripts/db/export-community-db.sh --dry-run

正式导出社区数据库脚本

bash forge-server/scripts/db/export-community-db.sh

单独检查敏感数据

node forge-server/scripts/db/check-sensitive-data.js forge-server/db/community-export

编译后端

cd forge-server
mvn -pl forge-admin-server -am -DskipTests compile

  ---
九、推荐日常工作流

以后每次改数据库，按这个流程：

1. 表结构变化？
   新增 forge-server/db/migration/Vx.y.z__xxx.sql

2. 系统必须数据变化？
   修改 forge-server/db/seed/required/R__xxx.sql

3. 演示数据变化？
   修改 forge-server/db/seed/demo/D__xxx.sql

4. 本地验证初始化：
   bash forge-server/scripts/db/init-db.sh --database forge_admin --user root --password your_password

5. 社区同步前检查：
   bash forge-server/scripts/db/export-community-db.sh --dry-run

6. 正式生成社区导出结果：
   bash forge-server/scripts/db/export-community-db.sh
