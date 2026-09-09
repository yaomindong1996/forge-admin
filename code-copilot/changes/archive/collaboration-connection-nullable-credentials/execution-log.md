# 执行日志 — 企业协同新增连接允许 client_id/client_secret 为空

## 时间线

| 时间 | 阶段 | 事件 | 备注 |
|------|------|------|------|
| 2026-08-31 | apply | 确认根因：连接表 `client_id`/`client_secret` NOT NULL | 新增连接 DTO 不写这两列 |
| 2026-08-31 | apply | 新增 `V1.0.138`，同步模板 SQL 与实体注释 | 脚本无 Flyway `${}` 占位符 |
| 2026-08-31 | test | collaboration 模块 DTO 单测通过 | 见下方命令 |

## 2026-08-31 增量验证

- **变更范围**：`sys_social_config.client_id/client_secret` 改为可空；连接保存 DTO 不透传凭据。
- **命令与结果**：

```text
export JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home
cd forge-server && mvn -pl forge-framework/forge-plugin-parent/forge-plugin-collaboration -am test -P enable-tests -Dsurefire.failIfNoSpecifiedTests=false -Dtest=CollaborationConnectionSaveRequestTest
```

- `CollaborationConnectionSaveRequestTest`：Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
- BUILD SUCCESS
- Flyway 脚本 `V1.0.138__allow_nullable_social_config_credentials.sql` 无 `${}` 占位符，`MODIFY COLUMN` 带 `IS_NULLABLE = 'NO'` 防重复。

- **跳过项**：未启动 `forge-admin-server`，未对真实库执行迁移。重启主后台后由 Flyway 执行 `V1.0.138`。
- **本轮启动并已停止的服务 PID**：无。
