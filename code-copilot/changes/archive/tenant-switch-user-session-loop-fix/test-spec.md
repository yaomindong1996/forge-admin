# 测试 Spec — 租户切换用户会话循环修复
> status: complete
> created: 2026-08-04

## 0. 测试原则

- 复用仓库 JUnit 5、Mockito 和 Vitest 基线，不启动真实服务或数据库。
- 先跑聚焦测试，再执行相关编译和前端构建。
- 记录实际命令、结果、警告及跳过项，不把未执行的浏览器冒烟表述为通过。

## 1. P0 核心场景

| 场景 | 输入 | 预期 |
|------|------|------|
| 跨租户刷新用户 | Session 用户 `userId=1`、当前租户为目标租户 | 调用 `loadUserByUserId(1, targetTenantId, activeOrgId)`，不按用户名查询 |
| 刷新会话元数据 | 新构建 `LoginUser` | 保留原登录时间、IP、客户端并写回 Session |
| 未登录刷新 | Session 无 `LoginUser` | 返回空数据，不调用用户加载服务 |
| 前端初始化失败 | 用户/菜单初始化抛错 | 清理登录态一次，路由守卫完成一次，跳转 `/login` 一次 |
| 登录跳转地址 | 原始业务路由带 query | 登录目标携带完整 `fullPath`，不重新进入原权限初始化分支 |

## 2. P1 集成与静态检查

- Auth Starter 定向 JUnit 测试通过。
- 前端定向 Vitest 与相关 JS ESLint 通过。
- 后端 Auth Starter 聚合编译通过。
- 前端生产构建通过。
- `git diff --check` 通过，工作树既有 `.DS_Store` 变更未被纳入。

## 3. 计划命令

```bash
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home \
PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH \
mvn -Penable-tests -pl forge-framework/forge-starter-parent/forge-starter-auth -am \
  -Dtest=AuthControllerTest -Dsurefire.failIfNoSpecifiedTests=false test
```

```bash
source ~/.nvm/nvm.sh
nvm use v20.19.0
pnpm exec vitest run src/router/guards/__tests__/auth-bootstrap-recovery.spec.js
pnpm exec eslint src/router/guards/permission-guard.js \
  src/router/guards/auth-bootstrap-recovery.js \
  src/router/guards/__tests__/auth-bootstrap-recovery.spec.js
pnpm build
```

```bash
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home \
PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH \
mvn -pl forge-framework/forge-starter-parent/forge-starter-auth -am compile -DskipTests
git diff --check
```

## 4. 跳过项

- 不启动 Admin、MySQL、Redis，不执行真实 Token 的跨租户浏览器 E2E。
- 部署环境需冒烟：默认租户登录 → 切换“小米连锁” → 用户信息、菜单正常 → 刷新仍保持目标租户。

## 5. 实际结果

| 范围 | 结果 | 备注 |
|------|------|------|
| 失败基线 | passed（成功复现） | 旧后端调用未命中 `loadUserByUserId`，测试 1/2 失败；前端恢复模块不存在，测试套件失败 |
| 后端定向 JUnit | passed，2/2 | 聚合 16 模块执行；无失败或跳过的目标测试 |
| 前端定向 Vitest | passed，2/2 | 单次恢复与登录页自重定向保护通过 |
| 前端定向 ESLint | passed | 首次发现文件尾多空行，修正后重跑通过 |
| 后端聚合 compile | passed，16/16 modules | 存在既有 Lombok Builder、deprecated 警告 |
| 前端 production build | passed，8848 modules，2m35s | 存在既有组件命名、CSS 注释、动态导入和 chunk 警告 |
| 差异空白检查 | passed | 未包含用户既有 `.DS_Store` 变更 |
