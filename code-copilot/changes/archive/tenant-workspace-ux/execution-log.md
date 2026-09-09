# 执行日志 — 租户工作区 UX

## 时间线

| 时间 | 阶段 | 事件 |
|------|------|------|
| 2026-08-31 | apply | 按工作区模型开始改造 |
| 2026-08-31 | test | 拦截器单测 2/2；前端契约 10/10；eslint 通过 |

## 2026-08-31 验证

```text
JAVA_HOME=.../openjdk@17 mvn -pl forge-starter-tenant,forge-plugin-system -am test -P enable-tests -Dtest=TenantInterceptorWorkspaceTest
# TenantInterceptorWorkspaceTest: Tests run: 2, BUILD SUCCESS

./node_modules/.bin/vitest run src/views/system/__tests__/tenant-workspace-ux.spec.js src/views/system/__tests__/system-management-ui-contract.spec.js
# Test Files 2 passed, Tests 10 passed

./node_modules/.bin/eslint --fix src/views/login/index.vue src/layouts/components/TenantSwitcher.vue src/views/system/user.vue src/views/system/role.vue src/views/system/org.vue src/views/system/post.vue
```

跳过：未启动真实服务、未做浏览器点选。登录页「按用户名再判断该用户有几个租户」未做。

## 2026-08-31 第二轮

密码登录先验密，账号多个工作区时返回 4091；登录页默认不拉租户全集。

```text
mvn -pl forge-plugin-system -am test -P enable-tests -Dtest=UserLoadServiceImplPasswordLoginTest
# Tests run: 2, BUILD SUCCESS

vitest run tenant-workspace-ux.spec.js  # 6 passed
eslint login/index.vue login/api.js     # passed
```
