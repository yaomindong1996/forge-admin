# Test Spec

## 前端

- `forge-admin-ui/src/components/ai-form/__tests__/file-render-utils.spec.js`
  - 支持 png/jpg/jpeg/gif/webp/svg 等图片扩展名。
  - 非图片名称不被误判为图片。
  - 逗号分隔附件值和名称按顺序配对。
- `forge-admin-ui/src/views/system/__tests__/user-role-order.spec.js`
  - 当前角色优先。
  - 当前角色组内稳定排序，未拥有角色保持原顺序。

## 后端

- `JobSchedulerTest`：重建会清除残缺 Quartz 数据并重新生成 Job/Trigger；重建后应用停用状态。
- `JobScheduleCoordinatorTest`：重建走同步状态落库，版本变化时继续收敛。
- `JobSyncApiContractTest`：存在 `/rebuild` Controller 映射、Service 方法和前端调用。
- `SysRoleMapperXmlContractTest`：已授权角色的全局置顶排序同时绑定目标用户、租户和当前组织。

## 命令

```bash
cd forge-admin-ui && source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm exec eslint src/components/ai-form/file-render-utils.js src/components/ai-form/__tests__/file-render-utils.spec.js src/components/ai-form/AiCrudPage.vue src/components/lowcode-builder/preview/LowcodePreviewPane.vue src/views/system/user-role-order.js src/views/system/__tests__/user-role-order.spec.js src/views/system/user.vue src/views/system/job-config.vue
cd forge-admin-ui && source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm vitest run src/components/ai-form/__tests__/file-render-utils.spec.js src/views/system/__tests__/user-role-order.spec.js
cd forge-admin-ui && source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm build
cd forge-server && JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-job -am -DskipTests test-compile
cd forge-server/forge-framework/forge-plugin-parent/forge-plugin-job && JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -Penable-tests -Dtest=JobSchedulerTest,JobScheduleCoordinatorTest,JobSyncApiContractTest test
cd forge-server/forge-framework/forge-plugin-parent/forge-plugin-system && JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -Penable-tests -Dtest=SysRoleMapperXmlContractTest test
xmllint --noout forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/resources/mapper/SysRoleMapper.xml
git diff --check
```

## 跳过项

- 不启动 Admin/Vite 服务，不连接真实 MySQL、Redis 或 Quartz JDBC 库；用户偏好由其自行执行真实服务与数据库联调。
- 本轮没有 Flyway 变更，不执行 Flyway migrate。
