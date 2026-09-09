一、首次初始化数据库

1. 准备环境

需要本机有：

mysql --version
node --version
java -version

推荐：

- MySQL 8+
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

表结构增量变更由主后台服务启动时的 Flyway 执行 `forge-server/db/migration`。

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
