# 执行日志 - 在线用户租户上下文修复

| 时间 | 阶段 | 结果 |
|------|------|------|
| 2026-08-03 | Research | 确认登录前旧会话查询、在线记录插入和调度清理缺少显式租户上下文 |
| 2026-08-03 | Proposal | 用户已明确授权修复；不放宽租户严格模式，不忽略在线用户表 |
| 2026-08-03 | Apply | 登录、Token 事件和定时任务分别补齐可信租户或受控跨租户上下文；主动登出改为状态更新 |
| 2026-08-03 | Verify | 插件 13 项定向测试、Admin 1 项定时任务测试、26 模块聚合编译全部通过 |

## 验证

### 环境排查

- 首次使用系统默认 JDK 执行 Maven，失败：`无效的目标发行版: 17`。
- 设置 JDK 17 后未在精简 `PATH` 中包含 Maven，失败：`env: mvn: No such file or directory`。
- 已确认并固定：Maven `/usr/local/apache-maven-3.9.3/bin/mvn`，JDK 17 `/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home`。
- 首次目标测试命令未启用 `enable-tests` Profile，源码编译成功但测试被 POM 默认配置跳过；后续命令均显式使用 `-Penable-tests`。
- 首次启用测试后，新增测试因缺少 MyBatis-Plus Lambda 表元数据初始化失败；补齐测试夹具后复跑通过，业务实现未因此调整。

### 通过项

1. 插件定向测试：

   `mvn -Penable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-system -Dtest=SysOnlineUserServiceSecurityTest,OnlineUserSecurityContractTest test`

   结果：`Tests run: 13, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。

2. 定时清理任务测试：

   `mvn -Penable-tests -pl forge-admin-server -Dtest=OnlineUserCleanTaskTest test`

   结果：`Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。

3. 插件及依赖聚合编译：

   `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-system -am -DskipTests compile`

   结果：26 个 Reactor 模块全部 `SUCCESS`，`BUILD SUCCESS`。

### 警告和跳过项

- 编译存在仓库既有的 deprecated/unchecked 警告，不阻断本变更。
- 未启动真实服务、未执行登录接口或数据库联调；本轮通过单元测试、契约测试和聚合编译覆盖，且无数据库迁移。

## 服务清理

本轮未启动服务。
