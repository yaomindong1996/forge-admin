# 执行日志 — 租户切换用户会话循环修复
> status: complete
> created: 2026-08-04

## 1. 基线

- 当前分支：`main`。
- 基线提交：`bf62275b [business-domain-delete-orphan-object-fix] 优化业务域孤立对象删除`。
- 工作树既有变更：`M .DS_Store`、`D forge/.DS_Store`，本轮保持不动。
- 已读取根规则、`code-copilot/AGENTS.md`、三份项目记忆、自动化测试标准、`using-superpowers`、`writing-plans` 和 `forge-coding-standards`。

## 2. 研究结论

| 范围 | 根因 | 证据 |
|------|------|------|
| 后端 | 租户切换按 `userId` 成功，但 `/auth/userInfo` 按 `username + targetTenantId` 刷新；无目标租户绑定的超级管理员被误判不存在 | `SysTenantServiceImpl#switchTenant`、`AuthController#getUserInfo`、`SysUserMapper.xml#selectByUsernameForLogin` |
| 前端 | 初始化失败后仍 `replace` 当前路由，因用户状态为空而无限重入同一分支 | `permission-guard.js` 首次初始化 catch 后的无条件导航 |
| 刷新无效 | Token 持久化保留，刷新后继续进入同一错误链路 | `auth` Store 持久化配置与权限守卫 Token 分支 |

## 3. 已执行记录

| 时间 | 范围 | 命令/动作 | 结果 | 备注 |
|------|------|-----------|------|------|
| 2026-08-04 | 工作树基线 | `git status --short`、`git branch --show-current`、`git log -1` | passed | 仅发现既有 `.DS_Store` 变更 |
| 2026-08-04 | 根因定位 | `rg`/`sed` 检查租户切换、Session、用户加载和权限守卫 | passed | 确认后端身份键不一致与前端路由重入叠加 |
| 2026-08-04 | 变更文档 | 创建 `spec.md`、`tasks.md`、`test-spec.md`、`execution-log.md` | passed | 进入测试与实现阶段 |
| 2026-08-04 | 后端失败基线 | Auth Starter 执行 `AuthControllerTest` | failed，1/2 | 旧实现仍按用户名刷新，未命中按用户 ID 的测试契约 |
| 2026-08-04 | 前端失败基线 | 定向执行 `auth-bootstrap-recovery.spec.js` | failed，0 tests | 恢复模块尚不存在，符合测试先行预期 |
| 2026-08-04 | 后端实现 | `AuthController#getUserInfo` 改用 Session `userId/tenantId/activeOrgId` | passed | 保留登录元数据并回写 Session，不改变目标租户校验 |
| 2026-08-04 | 前端实现 | 新增账号初始化恢复工具并接入权限守卫 | passed | 仅在用户身份仍为空时清理 Token 并单次跳登录页 |
| 2026-08-04 | 后端单模块复跑 | 不带 `-am` 执行 Auth 定向测试 | failed | 本地已安装 WebSocket 依赖缺少当前类；改用项目聚合构建后通过 |
| 2026-08-04 | 后端定向回归 | Java 17 下 `mvn -Penable-tests -pl ...forge-starter-auth -am -Dtest=AuthControllerTest -Dsurefire.failIfNoSpecifiedTests=false test` | passed，2/2，16 modules success | 存在既有 Lombok、deprecated/unchecked 警告 |
| 2026-08-04 | 前端定向回归 | Node 20.19.0 下 `pnpm exec vitest run ...auth-bootstrap-recovery.spec.js` | passed，2/2 | 无测试警告 |
| 2026-08-04 | 前端 Lint | `pnpm exec eslint` 检查权限守卫、恢复工具及测试 | passed | 首次发现新增工具文件尾多空行，修正后重跑通过 |
| 2026-08-04 | 后端聚合编译 | Java 17 下 `mvn -pl ...forge-starter-auth -am compile -DskipTests` | passed，16/16 modules | 存在既有 Lombok Builder 和 deprecated 警告 |
| 2026-08-04 | 前端生产构建 | Node 20.19.0 下 `pnpm build` | passed，8848 modules，2m35s | 存在既有组件命名冲突、CSS 注释、动态导入和 chunk 警告 |
| 2026-08-04 | 差异卫生 | `git diff --check` | passed | 用户既有 `.DS_Store` 变更保持不动 |

## 4. 服务与环境

- 本轮启动服务：无。
- 数据库变更：无。
- 遗留 PID：无。

## 5. 未执行项

- 未启动 Admin、MySQL 或 Redis。
- 未执行真实浏览器跨租户 E2E；部署环境需验证默认租户切换“小米连锁”以及刷新后的会话、菜单和租户保持行为。
