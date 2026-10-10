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
