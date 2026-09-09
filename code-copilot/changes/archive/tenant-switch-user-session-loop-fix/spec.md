# 租户切换用户会话循环修复
> status: complete
> created: 2026-08-04
> complexity: 中等

## 1. 背景与目标

默认租户的超级管理员切换到其它启用租户后，前端重新加载用户信息时持续收到“用户不存在”，权限路由守卫反复替换当前路由，导致接口无限重试和错误弹窗；浏览器刷新后仍使用原 Token，问题无法自行恢复。

本变更目标：

- 切换租户后按已认证用户的稳定主键刷新 `LoginUser`，避免用户名在目标租户下无法重新定位用户。
- 保持目标租户、用户身份、组织角色和权限均由后端重新校验和构建，不放宽租户边界。
- 用户身份初始化失败时清理无效登录态并跳转登录页，只执行一次恢复动作，不再重入当前路由。
- 不新增数据库结构或数据迁移。

## 2. 代码现状与根因

### 2.1 后端会话刷新身份键不一致

- `SysTenantServiceImpl#switchTenant` 使用 Session 中的 `userId` 调用 `loadUserByUserId(userId, targetTenantId)`，超级管理员可按设计进入任意启用租户。
- 切换成功后，`AuthController#getUserInfo` 却使用 `username + currentTenantId` 调用 `loadUserByUsername`。
- `SysUserMapper.xml#selectByUsernameForLogin` 要求用户主租户为目标租户，或存在目标租户的 `sys_user_tenant` 绑定。默认租户超级管理员无需该绑定，因此查询为空并抛出“用户不存在”。

### 2.2 前端失败分支形成路由重入

- `TenantSwitcher.vue` 在切换成功后保留 Token、清空用户和菜单状态并跳转首页，触发权限守卫重新初始化。
- `permission-guard.js` 在用户信息初始化失败后仍执行 `next({ ...to, replace: true })`。
- 因 `userStore.userInfo` 仍为空，新导航再次进入同一初始化分支，形成无限请求；刷新后持久化 Token 仍在，循环继续。

## 3. 功能范围

- [x] `/auth/userInfo` 使用 Session 中的 `userId`、当前 `tenantId` 和 `activeOrgId` 重建用户信息。
- [x] 刷新成功后保留原登录时间、登录 IP 和客户端信息，并覆盖 Token Session 中的 `LoginUser`。
- [x] 首次用户身份初始化失败时，前端清理账号与 Token 状态并单次跳转登录页，携带原目标地址用于重新登录后返回。
- [x] 已有用户信息场景下的菜单补载逻辑保持不变。
- [x] 后端与前端分别增加聚焦回归测试。

## 4. 安全与业务规则

- 只信任服务端 Token Session 中已认证的 `userId`，不接受客户端传入用户标识。
- 目标租户仍由切换服务校验启用状态、有效期以及普通用户租户绑定；不得使用全局忽略租户绕过成员校验。
- `loadUserByUserId` 继续通过 `buildLoginUser` 重建目标租户下的组织、角色、菜单/API 权限。
- 初始化失败按失效登录态处理，防止带着半初始化身份进入业务页面。

## 5. 数据与接口变更

- 数据库：无变更，无 Flyway 脚本。
- 接口路径和响应结构：无变更。
- 行为调整：`GET /auth/userInfo` 的内部刷新键从用户名改为已认证用户 ID。

## 6. 影响范围

- 后端认证 Starter：当前登录用户信息刷新。
- 前端权限路由守卫：账号初始化异常恢复。
- 顶部租户切换入口：协议不变，受上述修复覆盖。

## 7. 风险与回滚

- 风险：初始化阶段任一异常会退出登录；这是安全失败关闭行为，可避免半初始化状态和无限重试。
- 风险：若 Session 中用户已被删除，按用户 ID 刷新仍会失败，并由前端正常退出登录。
- 回滚：恢复 `AuthController#getUserInfo` 原刷新调用和权限守卫原失败分支即可；无数据回滚。

## 8. 测试策略

- 后端 JUnit + Mockito 静态 Session Mock：验证按 `userId` 刷新、目标租户参数、Session 回写和登录元数据保留。
- 前端 Vitest：验证初始化失败恢复只清理一次登录态，并跳转非重入的登录路由。
- 执行 Auth Starter 定向单测、相关模块编译、前端定向测试/ESLint/生产构建、`git diff --check`。
- 不启动真实服务、不连接 MySQL/Redis；真实跨租户切换由部署环境冒烟。

## 9. 确认记录（HARD-GATE）

- 确认时间：2026-08-04
- 确认人：用户
- 确认内容：用户明确要求修复默认租户切换到“小米连锁”后“用户不存在”、无限弹窗且刷新无效的问题。

## 10. 执行与审查结论

- 后端 `AuthControllerTest` 2/2 通过，覆盖稳定用户 ID 刷新、登录元数据保留、Session 回写和空 Session。
- 前端 `auth-bootstrap-recovery.spec.js` 2/2 通过，覆盖单次清理/跳转及登录页自重定向保护。
- Auth 相关 16 模块聚合编译、定向 ESLint、前端生产构建和空白检查通过。
- 未启动真实服务或数据库；部署后仍需按“默认租户 → 小米连锁 → 刷新”执行真实 Token 冒烟。
