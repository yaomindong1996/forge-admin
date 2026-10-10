# 本轮增量验证

- P0：已执行迁移与现有 JAR 同版本、同文件名/描述、同 checksum，无失败记录。
- P0：部署前明确待执行迁移；不跳过校验、不 repair、不变更历史表。
- P0：配置备份可读，JAR 摘要保持不变；只重启 Admin 服务。
- P0：启动日志无 Flyway 校验错误，Admin 进程和 HTTP 可用。
- P1：仓库配置示例不含凭据、只限定迁移来源，部署说明提供回滚步骤。
- 静态验证：配置契约测试、`git diff --check`。
- 不重打业务 JAR、不跑前端构建，本轮没有业务或前端变更。

## 提交前增量检查

- 复跑 `node --test forge-server/scripts/deployment/admin-flyway.test.mjs`。
- `git diff --check`、`git diff --cached --check` 与 `pnpm check:edition`。
- 只暂存本次 9 个文件，不暂存 `.DS_Store`，不提交远程凭据或生产 EnvironmentFile。
- 复用上述生产验收，不为 Git 提交再次重启线上服务。

## 第二阶段增量验证

- 默认配置仅 classpath，无 filesystem 自动回退；目录缺失时失败。
- `FORGE_FLYWAY_LOCATIONS` 显式覆盖仍有效，历史表与校验行为保持不变。
- Maven 资源与所有源 SQL 字节一致，资源未被占位符替换；Docker Admin 不再挂载旧 SQL。
- Admin 配置/资源单元测试不启动 Spring Boot、不连接真实数据库。
- 本地 `forge-admin-server` 打包，并检查新 JAR 中默认配置及迁移文件集/内容。
- `node --test forge-server/scripts/deployment/admin-flyway.test.mjs`、`git diff --check`、开源边界检查。
- 不重新部署生产；不把前一阶段服务器验证作为本轮新产物已部署的证据。

## 两阶段改动推送前检查

- 复用第二阶段发布打包、4 项 Java 测试及 217 份 SQL 逐字节核对结果，不重复启动服务。
- 复跑 5 项 Node 契约测试、差异空白检查和暂存区开源边界检查。
- 获取当前远程分支并确认可快进推送，禁止强推；仅暂存本任务文件，排除 `.DS_Store`。
- 推送后比较远程分支与本地 HEAD，确认无待推送提交。
