# 社区纯净版 SQL 导出

面向文档站 / 社区下载的「按版本纯净版初始化 SQL」。与仓库内 `全量初始化SQL.sql` 不同：它来自**已跑完 Flyway 的测试库**，经 `clean-db.sh` 清理后再安全导出。

## 目标结果

- 空库可一次导入成功（导入工具报错数为 0）。
- 仅默认租户、超级管理员 `admin`、默认角色、菜单/字典/参数/区划、任务配置、内置模板、`forge_schema_history`、本地存储默认配置。
- **不含**低代码自动业务表与演示数据、流程运行/模型垃圾数据、多租户与测试账号、日志与文件运行数据。
- **不含** `act_*` / `flw_*` 的建表与数据（见下节）。

## Flowable 表：必须跳过

`dump-safe-full.sh` 的 `full_ignore_regex` 必须为：

```bash
full_ignore_regex="^act_|^flw_|(^|_)version$"
```

只跳过 `^act_` **不够**：`flw_ru_batch`、`flw_event_*`、`flw_channel_definition` 等会进 dump。导入后启动会：

1. `SQLSyntaxErrorException: Table 'flw_ru_batch' already exists`（引擎再执行 CREATE）
2. `FlowableWrongDbException: version mismatch: library version is '7.0.1.1', db version is null`（有部分 `flw_*`，却没有 `act_ge_property.schema.version`）

原因：

- Flowable / 事件注册相关表在 **Admin / Flow 首次启动时自动创建与升级**，不必写进社区初始化 SQL。
- 官方 `全量初始化SQL.sql` 本身也不带完整 `act_*` / `flw_*` 结构。

**禁止**在导出后手工追加：

- `act_*` / `flw_*` 的 `CREATE TABLE`
- `INSERT INTO act_ge_property` / `act_id_property` 等

否则会出现 Navicat / mysql 导入错误：`1146 Table '….act_ge_property' doesn't exist`（有 INSERT、无建表）。

`clean-db.sh` 在**库内清理**时仍会保留 `act_ge_property` / `act_id_property` 等属性行，那是给已存在流程表的运行库用的；**导出社区纯净版时不要把这些行带进 SQL 文件**。

### 已误导入带 flw_* 的纯净版时

在**该目标库**执行（可按库内实际表名调整），再重启 Admin/Flow：

```sql
SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS
  flw_ru_batch_part,
  flw_ru_batch,
  flw_event_resource,
  flw_event_definition,
  flw_event_deployment,
  flw_channel_definition,
  flw_ev_databasechangelog,
  flw_ev_databasechangeloglock;
SET FOREIGN_KEY_CHECKS = 1;
```

更稳妥：空库重导**已去掉全部 flw_*/act_*** 的纯净版文件。

## 推荐流程

在**克隆库**上操作，禁止对共享测试库直接 `--execute` 清理。

```bash
# 1) 克隆测试库 → 临时库（示例名可改）
mysqldump … forge_admin | mysql … forge_admin_vXXX_pure

# 2) 清理成模板库（在 forge-server 下）
MYSQL_PWD=*** bash scripts/db/clean-db.sh \
  --host … --database forge_admin_vXXX_pure --user … --execute --yes

# 3) 安全导出（须跳过 act_* 与 flw_*）
MYSQL_PWD=*** bash scripts/db/dump-safe-full.sh \
  --host … --database forge_admin_vXXX_pure --user … \
  --ssl-mode DISABLED \
  --output /tmp/forge-admin-pure-base.sql

# 4) 仅补充：Flyway 历史 + 本地存储（dump-safe 默认跳过 *_history 数据与敏感存储配置）
mysqldump … --no-create-info forge_admin_vXXX_pure \
  forge_schema_history sys_file_storage_config > /tmp/forge-admin-pure-extra.sql

# 5) 拼接为发布文件，清洗 installed_by 等环境痕迹（如 rdsroot → flyway）
# 文件名建议：ForgeAdmin-纯净版-V{版本}.sql
```

导出后全文检索确认：

- 无 `CREATE TABLE \`flw_` / `CREATE TABLE \`act_`
- **有** `INSERT INTO \`sys_user\`` / `sys_dict_data` / `sys_config` / `sys_resource`（基础数据不可缺）

`sys_file_storage_config` 只允许导出 **local、无 AK/SK** 的默认行；若含 COS/密钥，先改再导出或不要追加该表。

### 禁止用错误方式“手工删 flw_*”

若旧 dump 仍含 `flw_*`，删除时必须**按表一块一块删**（每个 `Table structure` / `Dumping data` 块各自结束），**不能**从第一个 `flw_*` 数据段一路 skip 到文件尾——mysqldump 的 data 段按表名排序，`flw_*` 后面是 `sys_*`，误 skip 会把 admin/字典/参数/菜单等 INSERT 全部干掉。

优先依赖已修复的 `dump-safe-full.sh`（`^act_|^flw_`），避免事后剥离。

## 验收

对空库执行导入后检查：

| 检查项 | 期望 |
|--------|------|
| 导入 ERROR | 0 |
| `sys_user` / `sys_tenant` / `sys_role` | 各 1 行，`admin` |
| `sys_dict_type` / `sys_dict_data` / `sys_config` / `sys_resource` | 有数据（系统基础配置） |
| `forge_schema_history` | 有数据，版本与源库一致 |
| `information_schema` 中 `act_%` / `flw_%` | **0 张**（启动应用后才会出现） |
| 全文检索 | 无 `AKID` / `LTAI` / 真实库密码 / 云密钥 |

启动 Admin：Flyway 显示已是最新；登录 `admin` / `123456`；无低代码演示菜单；本地上传可用。

## 发布到文档站

运营后台「纯净版 SQL」上传 `.sql` 文件，填版本号并启用。文档站 `/start/pure-sql` 匿名列表下载。不要把大 SQL 误提交进业务仓库，除非用户明确要求。
