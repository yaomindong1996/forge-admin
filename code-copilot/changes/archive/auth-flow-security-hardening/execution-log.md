# 认证与流程身份安全加固 - 执行记录

## 2026-09-02 去掉未使用的 Swagger 匿名白名单

Java 17。`ActuatorAnonymousSurfaceContractTest` 1 passed。Sa-Token 不再放行 `/doc.html`、`/webjars/**`、`/swagger-ui/**`、`/v3/api-docs/**`。仓库无 springdoc/knife4j 依赖。

## 2026-09-02 P1 续 3

Java 17：`JAVA_HOME=$(/usr/libexec/java_home -v 17)`

```bash
cd forge-server
mvn test -P enable-tests -pl :forge-starter-file,:forge-plugin-system -am \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=FileControllerPermissionContractTest,FileManagerTest,FileAndExcelPermissionContractTest,LocalFileStorageTest
```

- FileManagerTest：4 passed（含删除所有权 2 条）
- LocalFileStorageTest：5 passed
- FileControllerPermissionContractTest：3 passed
- FileAndExcelPermissionContractTest：2 passed
- BUILD SUCCESS

跳过：未启动 Admin/报表真实服务；报表 URL 模板无独立 vitest（report-ui 无测试脚本）；图表事件 `new Function` 仍允许访问 `window`。

## 2026-09-02 P1 续 2

`PickerAndInboxPermissionContractTest` 2 passed；`HighRiskAdminPermissionContractTest` 2 passed；`MessagePermissionContractTest` 2 passed。starter-config / api-config / plugin-system / plugin-message 编译通过。

## 2026-09-02 P1 续

Java 17。

1. `HighRiskAdminPermissionContractTest`、`SysTenantPermissionContractTest`：4 passed。
2. `FlowDelegatedIdentityControllerTest`：8 passed。
3. `forge-admin-server` 主代码编译成功。`CryptoMigrationControllerTest` 因既有 `MenuRegisterAdapterImplTest` 测试编译失败未能单独执行。

跳过：未启动服务；报表设计器事件脚本仍允许 `new Function`（仅数据过滤器拦截危险全局对象）。

## 2026-09-02 P1

Java 17：`JAVA_HOME=$(/usr/libexec/java_home -v 17)`

1. `mvn test -P enable-tests -pl :forge-starter-auth,:forge-plugin-system -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=ActuatorAnonymousSurfaceContractTest,SysTenantPermissionContractTest,SystemAuthServiceImplPasswordRecoveryTest,SystemAuthServiceImplClientCredentialTest,RecoveryChannelSupportTest`
   - BUILD SUCCESS；上述用例全部通过。

2. `./node_modules/.bin/vitest run src/utils/__tests__/sanitize-html.spec.js src/views/login/__tests__/reset-password-channel.spec.js src/views/system/__tests__/tenant-workspace-ux.spec.js`
   - 10 passed

跳过：未启动 Admin 服务，未做浏览器联调。报表设计器 `new Function` 过滤脚本未纳入本轮。

## 2026-09-02

### 变更范围
找回密码改为短信/邮件通道（先看全局配置是否启用），流程待办/抄送/签收等改信 Session，匿名注册默认关闭，删除调试匿名接口。

### 命令与结果
Java 17：`JAVA_HOME=$(/usr/libexec/java_home -v 17)`

1. `mvn test -P enable-tests -pl :forge-starter-auth,:forge-plugin-system -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=CaptchaServiceImplTest,RecoveryChannelSupportTest,SystemAuthServiceImplPasswordRecoveryTest,SystemAuthServiceImplClientCredentialTest,MessageEmailCaptchaSenderTest,MessageSmsCaptchaSenderTest`
   - CaptchaServiceImplTest：12 passed
   - RecoveryChannelSupportTest：3 passed
   - SystemAuthServiceImplPasswordRecoveryTest：5 passed
   - SystemAuthServiceImplClientCredentialTest：6 passed
   - MessageEmailCaptchaSenderTest：2 passed
   - MessageSmsCaptchaSenderTest：3 passed

2. `mvn -P enable-tests -pl :forge-flow-server,:forge-admin-server -am -Dmaven.test.skip=true compile`
   - BUILD SUCCESS（含 forge-admin、forge-flow-server）

3. `mvn -P enable-tests -pl :forge-flow-server -Dtest=FlowDelegatedIdentityControllerTest test`
   - Tests run: 6, Failures: 0

### 跳过
- 未启动 Admin/Flow 真实服务，未做浏览器 E2E。
- `ClientCredentialSurfaceContractTest` 因仓库缺少 `forge-admin-server/sql/初始化脚本.sql` 报 NoSuchFile，与本轮改动无关。
- `forge-plugin-ai` 既有测试编译错误（`createEmbeddingModel`），本轮用 `maven.test.skip` 绕开无关模块。
