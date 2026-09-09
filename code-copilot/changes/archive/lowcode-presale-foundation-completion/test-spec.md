# 增量测试计划

## P0

- 运行规则：默认隐藏字段在 `visible` 效果规则匹配时显示，不匹配时隐藏；静态隐藏字段没有规则时仍被过滤；动态隐藏字段不进入验证规则。
- 扫码运行时：H5/浏览器使用注入 scanner 或原生能力，结果归一化，超时/取消/权限拒绝清理资源；`barcodeScanner` 分发 `SCAN_COMPLETE`。
- 子表：事件和扫码只回填当前行；同一字段不同的行并发请求不会串行覆盖；`CURRENT_CHILDREN` 只返回当前父记录集合。
- 发布：可见性规则和 `barcodeScanner` 保留在发布运行配置，非法组件/危险配置失败关闭。

## P1

- `ASSERT_RECORD` 在租户、逻辑删除、数据权限、父子关系或 expected 状态不满足时拒绝执行。
- 数量调整使用单条条件更新，减法不允许低于配置下界。

## 命令

前端：`source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm vitest run <相关测试> && pnpm build`。

后端：先在 generator 模块执行 `mvn -Penable-tests -Dtest=BusinessActionCommandPolicyTest,LowcodeRuntimeConfigBuilderTest,BusinessObjectPublishServiceCommandTest,AssertRecordActionStepExecutorTest,AdjustNumberActionStepExecutorTest,TransitionStatusActionStepExecutorTest,DynamicCrudCommandRepositoryTest test`；再执行 `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`。

静态：`git diff --check`；Flyway 占位符扫描沿用 `rg -n '\$\\{[^}]+\}' forge-server/db/migration`。

## 本轮增量验证结果（2026-08-11）

### P0 已通过

- Vitest：协同扫码、`AiFormItem`/`barcodeScanner`、动态显示规则、旧状态联动兼容、表单设计器协议、字段事件和业务表单运行编译契约共 36 项通过。
- generator：发布运行快照（含静态隐藏 + 可见性规则和扫码字段）、动作策略/发布门禁、状态门禁执行器、数量调整和条件 SQL 共 30 项通过。
- `pnpm build`：Vite 生产构建成功。

### P1 已通过（代码级）

- `ASSERT_RECORD` 执行器复用数据权限、租户、逻辑删除条件并使用 `FOR UPDATE` 行锁读取；目标状态/条件不满足会失败关闭。
- 数量调整测试验证单条条件更新、expected 条件和上下界均进入同一 SQL。

### 未覆盖项

- 未启动 Admin/Flow、未连接真实 MySQL/Redis、未执行 Flyway 实跑；按用户既有分工留待部署环境验证。
- 未使用真实手机摄像头和企业微信生产容器；浏览器摄像头权限、取消、超时和媒体流释放由适配器代码及单测覆盖。
- 聚合 `-am test` 在上游 `forge-plugin-message` 测试编译阶段因既有构造器参数不匹配失败，随后采用 `-Penable-tests` 的 generator 隔离定向测试完成本轮验证。
