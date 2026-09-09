# 验证基线

## 后端

- `cd forge-server && mvn -pl forge-flow/forge-flow-server -am -DskipTests compile`
- `cd forge-server && mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow -am -DskipTests compile`

## 前端

- `source ~/.nvm/nvm.sh && nvm use v20.19.0`
- `cd forge-admin-ui && ./node_modules/.bin/vitest run src/components/flow-designer/converter/__tests__/json-to-bpmn.spec.js src/components/flow-designer/converter/__tests__/user-task-parser-assignee.spec.js`
- `cd forge-admin-ui && ./node_modules/.bin/eslint <本轮修改的前端文件>`
- `cd forge-admin-ui && NODE_OPTIONS=--max-old-space-size=8192 npm run build`

## 关键断言

- 非空 BPMN XML 不被模型初始化覆盖。
- `initiatorSelect` 节点输出 `PROCESS_START_USER` 多实例集合表达式，且 XML 往返保留配置。
- 不开启多级退回时指定非上一节点被拒绝，开启后同流程历史用户任务可退回。
- 改派先写 owner 再写 assignee，本地任务状态仍为待处理。
- 自定义模型 Key 保存并拒绝非法/重复 Key。

仓库当前 `pnpm-workspace.yaml` 未声明 `packages`，本机 pnpm 8 执行 `pnpm exec` 会报 `packages field missing or empty`；本轮使用同一 `node_modules` 下的 Vitest/ESLint 可执行文件和 `npm run build` 完成等价验证。

## Review-fix 增量验证（2026-08-26）

### 后端定向回归测试

使用 `enable-tests` Profile 覆盖根 POM 默认的 `forge.tests.skip=true`，确保测试源码编译并真实执行：

```bash
cd forge-server
JAVA_HOME=$(/usr/libexec/java_home -v 17) PATH="$JAVA_HOME/bin:$PATH" \
  mvn -Penable-tests \
  -pl forge-framework/forge-plugin-parent/forge-plugin-flow,forge-framework/forge-plugin-parent/forge-plugin-generator \
  -am \
  -Dtest=FlowModelServiceImplTest,FlowModelMapperSqlContractTest,FlowTaskServiceImplStateChangeTest,BusinessFlowServiceFormAssetMergeTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

结果：两个目标模块各执行 `10` 个测试，共 `20` 个测试通过，`0` failures/errors/skipped。覆盖模型 Key 租户查询与复制开关、Mapper SQL 租户/逻辑删除边界、启动变量保留字段拒绝、发起人自选变量放行，以及串行直送和并行分支 fail-closed。

### 后端编译复验

```bash
cd forge-server
JAVA_HOME=$(/usr/libexec/java_home -v 17) PATH="$JAVA_HOME/bin:$PATH" \
  mvn -pl forge-framework/forge-plugin-parent/forge-plugin-system,forge-framework/forge-plugin-parent/forge-plugin-flow,forge-framework/forge-plugin-parent/forge-plugin-generator,forge-flow/forge-flow-client,forge-flow/forge-flow-server \
  -am -DskipTests compile
```

结果：`37` 个 Reactor 模块 `BUILD SUCCESS`。保留项目既有 deprecated/unchecked 警告，无本轮编译错误。

### 前端增量验证

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh && nvm use v20.19.0
./node_modules/.bin/vitest run \
  src/components/flow-designer/converter/__tests__/json-to-bpmn.spec.js \
  src/components/flow-designer/converter/__tests__/user-task-parser-assignee.spec.js
./node_modules/.bin/eslint \
  src/components/flow-designer/converter/user-task-writer.js \
  src/components/flow-designer/converter/__tests__/json-to-bpmn.spec.js
NODE_OPTIONS=--max-old-space-size=8192 npm run build
```

结果：Vitest `2` 个文件、`28` 个测试通过；本轮 ESLint `0` errors；生产构建 `✓ built in 52.55s`。构建保留项目既有 CSS 注释、native loader 和动态导入 warning。

### 静态检查与跳过项

- `git diff --check` 通过。
- `rg -n 'selectByModelKey\\(' forge-server` 无残留旧的无租户调用。
- 数据库模板 `${...}` 扫描仍只命中既有 `V1.0.72__collaboration_sync_schedule_and_todo_card_template.sql`，本轮未新增迁移占位符。
- 未启动真实 Admin/Flow/MySQL/Redis，也未执行会改变流程数据的浏览器 E2E；按用户偏好保留给用户环境联调。
