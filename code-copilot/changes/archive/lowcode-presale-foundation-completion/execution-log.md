# 执行记录

## 2026-08-11 初始化

- 范围：动态显示/隐藏、H5 条码扫描、子表字段事件和动态选项、事务门禁。
- 状态：已完成现状审计，尚未执行本轮代码修改和验证。

## 2026-08-11 收尾验证

- 范围：补齐 `ASSERT_RECORD` 设计器分支、动态可见性/扫码/事务门禁回归测试，并验证前端构建和 generator 模块。
- 前端定向测试：
  - `source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm vitest run src/utils/__tests__/collaboration-runtime.spec.js src/components/ai-form/__tests__/AiFormItem.spec.js src/components/lowcode-builder/shared/__tests__/runtime-rules.spec.js src/views/app-center/components/designer/form-first/__tests__/formDesignerSchema.spec.js src/components/ai-form/__tests__/field-event-runtime.spec.js src/views/app-center/components/designer/__tests__/business-action-designer-protocol.spec.js`
  - 结果：6 个测试文件、31 项通过。
- 前端 ESLint：对本轮 AiForm、AiFormItem、ChildTableEditor、RuntimeRulesEditor、协同扫码、H5 扫码、动作设计器和对应测试执行；结果 0 errors，保留 1 个既有 `AiForm.vue` prop warning。
- 前端生产构建：`source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm build`；结果：Vite build 成功，仅有既有动态导入、CSS 注释和组件命名冲突警告。
- 后端聚合编译：`JAVA_HOME=$(/usr/libexec/java_home -v 17) PATH="$JAVA_HOME/bin:$PATH" mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`；结果：`BUILD SUCCESS`，generator 及其依赖模块编译通过。
- 后端聚合测试尝试：`mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am test -Dforge.tests.skip=false -Dforge.compiler.skip=false`；结果：在既有 `forge-plugin-message` 的 `MessageServiceImplTest` 测试编译阶段失败，原因是测试仍按旧构造器参数实例化服务，未进入 generator 测试；未修改该非本轮问题。
- 后端隔离定向测试：`JAVA_HOME=$(/usr/libexec/java_home -v 17) PATH="$JAVA_HOME/bin:$PATH" mvn -Penable-tests -Dtest=BusinessActionCommandPolicyTest,LowcodeRuntimeConfigBuilderTest,BusinessObjectPublishServiceCommandTest,AssertRecordActionStepExecutorTest,AdjustNumberActionStepExecutorTest,TransitionStatusActionStepExecutorTest,DynamicCrudCommandRepositoryTest test`；结果：7 个测试类、30 项通过。
- 静态检查：`git diff --check` 通过；Flyway 扫描只命中历史模板 `${...}` 文本，本轮未新增 Flyway。
- 服务与环境：本轮未启动 Admin/Flow、未连接数据库或 Redis、未启动浏览器服务器；无需清理进程。

## 2026-08-11 兼容链路复核

- 修复旧属性面板 `props.__events` 的 `showHide` / `enableDisable` 配置只保存、不生效的问题：规范化时投影为目标组件运行规则，保存、预览和发布复用同一协议。
- 修复设计器运行路径提前过滤“静态隐藏但带可见性规则”字段的问题，并保留发布态隐藏基线。
- 修复子表扫码上下文和子表字段事件识别缺失；子表动态隐藏字段不再参与必填校验。
- 扫码字段关闭“允许手工输入”后输入框只读；配置 `SCAN_COMPLETE` 时不再重复展示两个扫码按钮。
- 定向 Vitest：6 个测试文件、34 项通过。
- 本轮相关 ESLint：0 errors、0 warnings。
- 前端生产构建：`source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm build`，结果成功；仅保留既有组件命名冲突、动态导入和 CSS 注释警告。

## 2026-08-11 运行编译链路补齐

- 范围：复核业务表单设计器从 `formDesignerSchema` 到页面运行配置的最后一段编译链路。
- 发现并修复：`BusinessFormDesigner.vue` 的运行字段白名单和 `COMPONENT_FIELD_DEFAULTS` 漏含 `barcodeScanner`，扫码组件在设计器中可配置但发布运行态会被过滤；同时补齐 `setting.hidden/formVisible` 对组件静态隐藏基线的透传。
- 回归测试：
  - `source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm vitest run src/utils/__tests__/collaboration-runtime.spec.js src/components/ai-form/__tests__/AiFormItem.spec.js src/components/lowcode-builder/shared/__tests__/runtime-rules.spec.js src/views/app-center/components/designer/form-first/__tests__/formDesignerSchema.spec.js src/components/ai-form/__tests__/field-event-runtime.spec.js src/views/app-center/components/designer/__tests__/business-action-designer-protocol.spec.js src/views/app-center/components/designer/__tests__/business-form-runtime-compile.spec.js`
  - 结果：7 个测试文件、36 项通过。
- ESLint：本轮相关 Vue/JS/测试文件 0 errors、0 warnings；单独纳入 `AiForm.vue` 时仍有 1 个既有 `vue/no-required-prop-with-default` warning。
- 前端生产构建：`source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm build`，结果 `✓ built in 1m 34s`；仅有既有动态导入、组件命名冲突和 CSS 注释警告。
- 静态检查：`git diff --check` 通过；本轮未新增 Flyway、后端接口或数据库变更。
- 服务与环境：未启动 Admin/Flow、未连接 MySQL/Redis、未使用真实摄像头或企业微信生产容器；无需清理进程。
