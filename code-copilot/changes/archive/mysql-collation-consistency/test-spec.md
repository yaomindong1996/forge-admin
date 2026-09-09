# MySQL 排序规则一致性修复测试计划

> status: complete
> created: 2026-08-25

## 1. 自动化检查

| 编号 | 场景 | 预期 |
|---|---|---|
| P0-01 | 手工初始化脚本 | 同时包含 `CREATE DATABASE` 和 `ALTER DATABASE`，目标均为 `utf8mb4_0900_ai_ci` |
| P0-02 | Docker 初始化 | MySQL 服务端参数为 `utf8mb4` / `utf8mb4_0900_ai_ci` |
| P0-03 | 全量 SQL | 服务端与 Docker 副本不含旧排序规则声明 |
| P0-04 | 活动独立 SQL | 报表和插件初始化 SQL 不含旧排序规则声明 |
| P0-05 | Flyway 兼容 | 已发布迁移文件没有工作区改动，迁移目录不存在占位符 |
| P1-01 | Shell 语法 | `bash -n` 通过 |
| P1-02 | Compose 配置 | `docker compose config` 可解析；未安装 Docker 时明确记录跳过 |
| P1-03 | 补丁质量 | `git diff --check` 通过，用户已有 `.DS_Store` 改动未被纳入 |

## 2. 计划命令

```bash
bash forge-server/scripts/db/check-collation-consistency.sh
bash -n forge-server/scripts/db/init-db.sh
bash -n forge-server/scripts/db/check-collation-consistency.sh
docker compose -f docker-forge-admin/docker-compose.yml config
rg -n '\$\{[^}]+\}' forge-server/db/migration
git diff --check
```

## 3. 人工环境验收

- 在全新 MySQL 8.0 实例运行 `init-db.sh` 后确认数据库默认值、字符列和临时表比较正常。
- 使用全新 Docker volume 启动 Compose，确认 MySQL 默认排序规则和 Admin Flyway 启动正常。
- 本轮不自动启动真实数据库、Admin 或 Flyway，避免影响本地已有数据和服务。

## 4. 执行结果

| 验证项 | 结果 |
|---|---|
| 静态排序规则一致性 | 通过 |
| `init-db.sh` / 检查脚本 Bash 语法 | 通过 |
| `init-db.sh --help` | 通过 |
| Docker Compose 配置解析 | 通过 |
| 公开文档建库/服务端参数命令扫描 | 通过 |
| 已发布 Flyway 文件无改动 | 通过 |
| Flyway placeholder 配置 | `placeholder-replacement: false`，现有业务模板变量按原样保留 |
| `git diff --check` | 通过 |
| ShellCheck | 跳过，本机未安装 |
| 真实 MySQL 8 / Flyway | 按计划留给用户侧环境验收 |
