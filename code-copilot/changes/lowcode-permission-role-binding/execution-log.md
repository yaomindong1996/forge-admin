# 执行记录

> 本文件只追加本轮实际执行的命令、结果、警告和跳过项。

## 2026-08-16 应用权限统一配置收尾验证

### 前端测试

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh && nvm use v20.19.0
pnpm exec vitest run src/views/app-center/__tests__
```

- 结果：通过，9 个测试文件、55 个用例全部通过。
- 警告：`business-process-workspace.spec.js` 浅挂载时存在既有的 `n-icon`、`n-modal`
  组件未解析警告，不影响测试结果。

```bash
pnpm exec vitest run \
  src/views/app-center/__tests__/application-permission-utils.spec.js \
  src/views/app-center/__tests__/role-permission-settings.spec.js
```

- 结果：通过，2 个测试文件、8 个用例全部通过。
- 覆盖：Long ID 字符串保真、应用模块转换、继承范围不写入保存请求、过期请求隔离、
  页面入口与按钮双向独立授权、角色默认数据范围只读。

### ESLint

```bash
pnpm exec eslint \
  src/views/app-center/application-workspace/ApplicationPermissionsPanel.vue \
  src/views/app-center/components/designer/BusinessPermissionFlowPanel.vue \
  src/views/system/components/RolePermissionSettings.vue \
  src/views/app-center/application-permission-utils.js \
  src/views/app-center/__tests__/application-permission-utils.spec.js \
  src/views/app-center/__tests__/role-permission-settings.spec.js
```

- 结果：通过，无错误和警告。

### 前端生产构建

```bash
NODE_OPTIONS=--max-old-space-size=8192 pnpm build
```

- 结果：通过，Vite 完成 9171 个模块转换并生成 `dist`。
- 既有警告：`UserSelectModal` 组件命名冲突、CSS 中存在 `//` 注释、若干模块同时被静态和动态导入。

### 后端权限相关验证

```bash
cd forge-server/forge-framework/forge-plugin-parent/forge-plugin-system
mvn -q -Penable-tests -Dtest=SysRoleServiceImplScopedPermissionTest test
```

- 结果：通过，2 个用例，确认应用范围保存拒绝越界资源，并只替换声明的资源和模块范围。

```bash
cd forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator
mvn org.apache.maven.plugins:maven-surefire-plugin:3.1.2:test \
  -Penable-tests -Dmaven.test.skip=false -DskipTests=false \
  -Dtest=BusinessApplicationPermissionServiceTest,DynamicDataScopeServiceTest,\
BusinessApplicationRuntimeServiceTest,BusinessApplicationPageMenuPublishServiceTest
```

- 结果：通过，13 个用例，无失败、错误或跳过。
- 覆盖：应用权限目录与保存边界、对象模块数据范围、运行时页面 RBAC 过滤、发布不重建页面角色绑定。

```bash
cd forge-server/forge-admin-server
mvn -q -DskipTests compile
```

- 结果：通过；包含 `ApplicationPermissionAdapterImpl` 缺失租户上下文时失败关闭的编译验证。

### 聚合命令阻断

```bash
cd forge-server
mvn -Penable-tests \
  -pl forge-framework/forge-plugin-parent/forge-plugin-generator,\
forge-framework/forge-plugin-parent/forge-plugin-system,forge-admin-server \
  -am test
```

- 结果：失败于既有 `ClientCredentialSurfaceContractTest`，测试读取已不存在的
  `forge-admin-server/sql/初始化脚本.sql`；generator 和 admin 在该次 Reactor 中被跳过。
- 另一次定向 Reactor 验证被既有 `MessageServiceImplTest` 构造参数缺失阻断；直接进入 generator
  模块执行 `test` 又被既有 `BusinessObjectPublishServiceCommandTest` 和
  `BusinessObjectPublishServiceFieldEventTest` 构造参数缺失阻断。
- 处理：未修改这些与本变更无关的测试，改用上面的 system 定向测试、generator Surefire
  定向执行和 admin 编译完成增量验证。

### 浏览器与服务状态

- `localhost:3000` 前端服务正在监听。
- `localhost:8580` 后端未监听；登录配置、验证码和加密信息请求经前端代理返回 HTTP 500，
  无法建立新登录态进入真实权限页面。
- 因此未把角色切换、保存回显、重新发布后授权保持等真实浏览器交互标记为通过。
- 本轮未启动新的长期运行服务，也没有需要清理的本轮服务进程。

## 2026-08-16 App/Flow 权限适配器装配修复

- 问题：`BusinessApplicationPermissionService` 强制依赖 `ApplicationPermissionAdapter`，但只有
  Admin 服务提供真实实现，App 和独立 Flow 服务启动时报缺少 Bean。
- 处理：分别增加 App/Flow 服务内的失败关闭桥接实现；真实角色、资源和数据范围管理仍只由
  Admin 服务执行，非 Admin 服务误调用时明确拒绝。
- 静态检查：确认直接加载 generator 的三个 Spring Boot 聚合服务均有唯一适配器实现，
  `git diff --check` 无异常。
- 按用户要求未执行测试、Maven 构建、服务启动或浏览器验证。
