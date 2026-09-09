# 执行记录

## 2026-08-30

### 范围

- 业务对象编码校验改为租户全局唯一，新增稳定对象 ID/配置键解析。
- 待办表单和列表展示按对象 ID、`configKey`、套件编码逐级定位。
- 新增 V1.0.136 历史重复编码修复及唯一索引迁移。

### 命令与结果

- `git diff --check`：通过本轮新增/修改文件检查；工作区已有 `ApproverAssigneeForm.spec.js` EOF 空行警告，与本变更无关。
- `rg -n '\$\\{[^}]+\\}' forge-server/db/migration/V1.0.136__enforce_business_object_code_uniqueness.sql`：无输出，未发现 Flyway 占位符。
- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am -DskipTests compile`：BUILD SUCCESS。
- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home mvn -Penable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-generator -Dtest=BusinessFlowServiceBusinessKeyTest test`：4 tests，0 failures，0 errors。
- 复核并补充低代码同步幂等性：按 `model_id`、`config_key` 和套件内 `model_code` 复用历史对象，避免对象编码迁移后重复创建；为已有对象补写 `config_key`。
- 复核 V1.0.136：迁移引用的业务表均由 V1.0.27/V1.0.40/V1.0.46/V1.0.83 等基线迁移创建；临时表、动态索引 DDL、JSON 更新和 MySQL 8 函数用法符合当前数据库版本。重复对象的新编码改为 `bo_<object_id>`，由主键保证确定性，避免摘要碰撞。
- 同步更新 `forge-server/db/全量初始化SQL.sql` 的业务对象唯一索引，保证不开启 Flyway 的全新数据库也直接使用租户级 `(tenant_id, object_code, del_flag)` 约束。
- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am -DskipTests compile`：BUILD SUCCESS。
- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home mvn -Penable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-generator -Dtest=BusinessFlowServiceBusinessKeyTest -Dsurefire.failIfNoSpecifiedTests=false test`：4 tests，0 failures，0 errors。

## 2026-08-30 增量修复：历史流程表单引用

### 范围

- 新增 V1.0.137，按应用发布快照中的 `objectId -> objectCode` 映射修复流程草稿、流程版本、应用快照、Flowable 模型表单引用及模型版本 BPMN。
- 业务流程服务兼容调用方传入旧对象编码时，按页面 `configKey` 解析当前规范对象。
- 流程模型测试发起页优先使用同应用业务绑定返回的规范对象编码，保证“请假申请 · 测试”表单可加载。
- 待办运行时从应用页面表单 key 恢复 `objectId/configKey/objectCode`，兼容已部署但仍携带旧编码的 Flowable 定义。

### 命令与结果

- 首次使用系统 JDK 8 执行 Maven 编译失败：`无效的目标发行版: 17`；未归因于本轮代码。
- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am -DskipTests compile`：BUILD SUCCESS。
- `pnpm exec eslint src/views/flow/model.vue`：0 errors，4 个既有模板换行 warning。
- `git diff --check`：通过。
- 当前开发库执行 V1.0.137 后核验：流程 `2090384244139360257` 的 subject/dependencies/action 均为 `bo_2089974506884993026`；`low12` 的 `form_json.objectCode` 已为该编码；应用版本快照残留旧编码 0 条。

## 2026-08-30 本轮表单绑定回归验证

- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am -DskipTests compile`：BUILD SUCCESS。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm exec eslint src/components/business-process-designer/ActionAndApprovalNodeConfig.vue src/components/business-process-designer/BusinessProcessDesigner.vue`：通过，0 errors。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm exec vitest run src/components/business-process-designer/__tests__/business-process-designer-workbench.spec.js src/components/business-process-designer/__tests__/business-process-designer.spec.js`：系统全局 pnpm 报 `packages field missing or empty`，未作为有效结果采纳。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && npx --yes pnpm@8.15.9 exec eslint src/components/business-process-designer/ActionAndApprovalNodeConfig.vue src/components/business-process-designer/BusinessProcessDesigner.vue`：0 errors。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && npx --yes pnpm@8.15.9 exec vitest run src/components/business-process-designer/__tests__/business-process-designer-workbench.spec.js src/components/business-process-designer/__tests__/business-process-designer.spec.js`：2 个测试文件、55 个测试全部通过。
- 当前开发库只读核验：流程 `2090384244139360257` 的 `dependencies.formAssets` 和审批节点 `formAsset.formKey` 均为 `app_2089968247981060098_page_page_page_form_form_form`；应用 `2089968247981060098` 的 `page_page` 页面仍引用 `form_form`，表单资产未删除。
- 浏览器/登录态接口本轮未重新启动服务；此前已验证流程设计页审批节点显示“测试”及 4 个字段。Admin/Flow 真实重启和 Flyway 登记由部署环境执行。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 npx --yes pnpm@8.15.9 build`：前端生产构建成功；仅保留既有 Vite 配置、CSS 注释、动态导入和 bundle 体积 warning。
- 浏览器回归（本机已有 3000 端口前端，未启动/停止服务）：登录后打开 `/app-center/business-process/2090384244139360257`，点击审批节点，页面显示“任务表单 / 测试”、滑块/评分/数字/输入框11 四个字段和“流程问题 0”；控制台无错误。
- V1.0.137 迁移脚本在开发库事务中执行后回滚，未产生数据变更；MySQL 仅提示临时表创建不可回滚（脚本末尾已显式删除），未发现 SQL 语法错误。
- 浏览器网络核验：`GET /ai/business/flow/form-assets/bo_2089974506884993026?includeInternal=true&applicationId=2089968247981060098` 返回 200，`formAssets` 包含 `app_2089968247981060098_page_page_page_form_form_form`（名称“测试”）。

### 跳过项

- 未启动 Admin/Flow 服务，未执行登录态接口和浏览器回归；本轮仅使用数据库核验和静态编译检查。

### 跳过项

- 未启动 Admin/Flow 服务，未执行真实数据库迁移和接口回归；按用户偏好由部署环境执行。
- 首次使用系统 JDK 8 编译失败（目标发行版 17），随后切换 JDK 17 重跑通过。
