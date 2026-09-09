# 执行记录

## 2026-08-30

- 已完成只读调用链排查，确认 `enrichTaskUserNames` 与 `enrichTaskBusinessParams` 是主要 N+1 来源。
- 已确认本轮不启动服务，不执行真实数据库运行态验证。

## 2026-08-30（实现与验证）

- 变更：`FlowTaskMapper`/XML 待办、已办、我发起列表增加显式 `tenant_id`，SQL 关联用户姓名；候选组按当前用户租户内角色/组织关系匹配；工作台统计复用同一规则。
- 变更：删除列表链路逐条 `FlowOrgIntegrationService.getUserInfo` 和 `RuntimeService.getVariable(..., businessParams)`；业务侧从 `ai_business_flow_instance_link.variables_snapshot` 的批量关联结果回填参数。
- 变更：新增 `V1.0.135__optimize_flow_task_list_queries.sql`，为处理人/发起人/状态/排序列增加可重复执行的联合索引。
- `git diff --check`：通过。
- `xmllint --noout forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/resources/mapper/FlowTaskMapper.xml`：通过。
- `JAVA_HOME=...openjdk@17... mvn -Penable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-flow -am compile -DskipTests`：通过；仅有项目既有弃用/未检查警告。
- `JAVA_HOME=...openjdk@17... mvn -Penable-tests -Dtest=FlowTaskMapperSqlContractTest -Dsurefire.failIfNoSpecifiedTests=false test`：通过，2 tests passed。
- 在补充“无 activeOrg 时不匹配候选组”SQL 分支后，重复执行上述 Mapper 合同测试：仍通过，2 tests passed。
- 在分类子查询补充租户条件后，重新执行 `xmllint` 与 Mapper 合同测试：XML 解析通过，2 tests passed。
- 最终检查：`git diff --check` 通过；新迁移脚本 `${...}` 占位符扫描通过（无输出）；确认未重复新增基线已有的 `(tenant_id,status,create_time)` 索引。
- 首次带 `-am` 的指定测试命令因上游模块无匹配测试触发 Surefire `failIfNoSpecifiedTests`，随后按 Surefire 提示增加 `-Dsurefire.failIfNoSpecifiedTests=false` 重跑通过；不是代码失败。
- 未启动 Admin/Flow 服务，未执行真实 MySQL/接口和执行计划验证；需在用户环境应用 Flyway 后确认候选组、跨租户用户成员和索引命中情况。

## 2026-08-30（分页 count 解析兼容修复）

- 根因：待办 SQL 的多层候选组 `EXISTS` 条件经过 MyBatis-Plus 分页优化后生成 `COUNT(*)`，
  JSqlParser 4.9 在租户/数据权限拦截器再次解析该 count SQL 时失败；同样条件使用
  `COUNT(1)` 可正常解析。
- 变更：新增 `CountOnePaginationInnerInterceptor`，保留 MyBatis-Plus 原有 count 优化，
  仅将生成 SQL 的首个 `COUNT(*)` 替换为 `COUNT(1)`；`MybatisPlusConfig` 已改用该实现。
- 变更：为 `forge-starter-orm` 增加测试依赖和 `CountOnePaginationInnerInterceptorTest`，
  覆盖嵌套候选组 SQL 的 count 生成与 JSqlParser 解析。
- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home mvn -Penable-tests -pl forge-framework/forge-starter-parent/forge-starter-orm -am -DskipTests compile`：通过。
- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home mvn -Penable-tests -pl forge-framework/forge-starter-parent/forge-starter-orm -am -Dtest=CountOnePaginationInnerInterceptorTest -Dsurefire.failIfNoSpecifiedTests=false test`：通过，1 test passed。
- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home mvn -Penable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-flow -am -DskipTests compile`：通过；仅有项目既有弃用/未检查警告。
- `git diff --check`（本轮 ORM、Flow 和测试变更）：通过。
- 未启动 Admin/Flow 服务，未连接真实 MySQL；需用户重启服务后确认真实分页 count、租户/数据权限拦截链和数据库执行计划。
- 增量复跑：`JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home mvn -Penable-tests -Dtest=FlowTaskMapperSqlContractTest -Dsurefire.failIfNoSpecifiedTests=false -pl forge-framework/forge-plugin-parent/forge-plugin-flow -am test`：通过，`FlowTaskMapperSqlContractTest` 2 tests passed；聚合模块仅有项目既有弃用/未检查警告。
