# 社区纯净版 SQL 导出

面向文档站 / 社区下载的「按版本纯净版初始化 SQL」。与仓库内 `全量初始化SQL.sql` 不同：它来自**已跑完 Flyway 的测试库**，经 `clean-db.sh` 清理后再安全导出。

## 目标结果

- 空库可一次导入成功（导入工具报错数为 0）。
- 仅默认租户、超级管理员 `admin`、默认角色、菜单/字典/参数/区划、任务配置、内置模板、`forge_schema_history`、本地存储默认配置。
- **不含**低代码自动业务表与演示数据、流程运行/模型垃圾数据、多租户与测试账号、日志与文件运行数据。
- **不含** `act_*` / `flw_*` 的建表与数据（见下节）。

## Flowable 表：必须跳过

`dump-safe-full.sh` 默认 `full_ignore_regex` 会跳过 `^act_|…version$`，这是**正确且必须保持**的行为。

原因：

- Flowable / 事件注册相关表在 **Admin / Flow 首次启动时自动创建与升级**，不必写进社区初始化 SQL。
- 官方 `全量初始化SQL.sql` 本身也不带完整 `act_*` 结构。

**禁止**在导出后手工追加：

- `act_*` / `flw_*` 的 `CREATE TABLE`
- `INSERT INTO act_ge_property` / `act_id_property` 等

否则会出现 Navicat / mysql 导入错误：`1146 Table '….act_ge_property' doesn't exist`（有 INSERT、无建表）。

`clean-db.sh` 在**库内清理**时仍会保留 `act_ge_property` / `act_id_property` 等属性行，那是给已存在流程表的运行库用的；**导出社区纯净版时不要把这些行带进 SQL 文件**。

## 推荐流程

在**克隆库**上操作，禁止对共享测试库直接 `--execute` 清理。

```bash
# 1) 克隆测试库 → 临时库（示例名可改）
mysqldump … forge_admin_new | mysql … forge_admin_vXXX_pure

# 2) 清理成模板库（在 forge-server 下）
MYSQL_PWD=*** bash scripts/db/clean-db.sh \
  --host … --database forge_admin_vXXX_pure --user … --execute --yes

# 3) 安全导出（默认已跳过 act_*）
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

`sys_file_storage_config` 只允许导出 **local、无 AK/SK** 的默认行；若含 COS/密钥，先改再导出或不要追加该表。

## 验收

对空库执行导入后检查：

| 检查项 | 期望 |
|--------|------|
| 导入 ERROR | 0 |
| `sys_user` / `sys_tenant` / `sys_role` | 各 1 行，`admin` |
| `forge_schema_history` | 有数据，版本与源库一致 |
| `information_schema` 中 `act_%` | **0 张**（启动应用后才会出现） |
| 全文检索 | 无 `AKID` / `LTAI` / 真实库密码 / 云密钥 |

启动 Admin：Flyway 显示已是最新；登录 `admin` / `123456`；无低代码演示菜单；本地上传可用。

## 发布到文档站

运营后台「纯净版 SQL」上传 `.sql` 文件，填版本号并启用。文档站 `/start/pure-sql` 匿名列表下载。不要把大 SQL 误提交进业务仓库，除非用户明确要求。
