# 执行记录

## 2026-08-15 - 第三期增量验证

变更范围：Task 11 节点配置场景模板、Task 12 旧入口只读引导、Task 13 对象参与流程查询。

| 命令 | 结果 |
| --- | --- |
| `cd forge-admin-ui && pnpm exec vitest run src/components/business-process-designer/__tests__/business-process-designer.spec.js src/components/business-process-designer/__tests__/business-process-designer-workbench.spec.js` | 通过：2 个文件、34 项测试。 |
| `cd forge-admin-ui && pnpm exec eslint src/components/business-process-designer/node-templates.js src/components/business-process-designer/StartNodeConfig.vue src/components/business-process-designer/ActionAndApprovalNodeConfig.vue src/components/business-process-designer/__tests__/business-process-designer-workbench.spec.js` | 通过。 |
| `cd forge-admin-ui && pnpm exec vitest run src/views/app-center/__tests__/legacy-process-entry.spec.js src/views/app-center/components/designer/__tests__/object-designer-navigation.spec.js` | 通过：2 个文件、8 项测试。`legacy-process-entry` 验证旧入口只读提示及工作台跳转。 |
| `cd forge-admin-ui && pnpm exec eslint src/views/app-center/trigger.vue src/views/app-center/components/designer/BusinessFlowBindingPanel.vue src/views/app-center/components/designer/BusinessFlowAppConfigPanel.vue 'src/views/app-center/object-designer.[objectCode].vue' src/views/app-center/__tests__/legacy-process-entry.spec.js` | 通过。 |
| `cd forge-admin-ui && pnpm build` | 两次通过。 |
| `cd forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator && JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home /usr/local/apache-maven-3.9.3/bin/mvn -Penable-tests -DskipITs -Dtest=BusinessProcessServiceTest,BusinessProcessMapperContractTest,BusinessObjectProcessControllerTest test` | 通过：21 项测试；目标模块完成 main/test 编译。 |

警告与跳过项：

- 前端构建的 `UserSelectModal` 命名冲突、CSS `//` 注释与静态/动态导入分包提示为既有告警，未由本轮引入。
- Vitest 的 jsdom 环境输出既有 `localStorage` 加密恢复警告，测试仍全部通过。
- 先尝试 Maven 依赖链命令 `mvn -Penable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am ... test`，被无关模块 `forge-plugin-message` 的既有 `MessageServiceImplTest` 编译失败阻断：该测试缺少新增的 `ApplicationEventPublisher` 构造参数。随后直接运行目标模块测试并通过。
- 未启动本地服务，因此未执行真实加密 HTTP、权限 403、MySQL `JSON_TABLE` 或 Flowable 运行时验证。
- 本轮未启动或停止任何服务。

Task 10 未验证且未提交：运行时业务流程编排、手动发起端点和页面运行时投影均不存在，详见 `tasks.md`。
