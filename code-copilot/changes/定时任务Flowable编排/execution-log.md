# 定时任务 Flowable 编排执行记录

> version: V9
> status: complete
> baseline-created: 2026-07-21

## 2026-07-21 实施启动

- 变更范围：V9 固定 Flowable 定义绑定、技术身份启动、独立 Flow 安全适配、Job 日志关联和前端入口。
- 基线：复用 V8 Starter `47/47`、Flow `12/12`、Flow Reactor `32/32`、Admin Reactor `43/43` 成功记录。
- 门禁：固定 processDefinitionId；首验独立 Flow 服务；只启动流程；禁止新 X-Inner-Call/直连 HTTP 旁路。
- 风险：工作区包含 V1-V8 未提交成果，本阶段只叠加 V9，不回退、不创建 Git 提交。
- 服务：本轮自动化阶段不启动真实 MySQL、Admin、Flow 或外部 HTTP 服务。

## 2026-07-21 Flow 运行时与远程适配验证

- `JobFlowRuntimeServiceTest`：`7/7` 通过，覆盖固定定义校验、挂起/不匹配失败和 businessKey 幂等恢复。
- `RemoteJobFlowExecutorTest`：`6/6` 通过，覆盖 `FLOW_API`、服务端 Bearer Token、无 `X-Inner-Call`、嵌套 `jobInput`、显式失败关闭及响应丢失恢复。
- 修复远程状态查询 businessKey 编码，`job:11:22` 固定输出为 `job%3A11%3A22`；远程基础地址尾部斜杠已归一化。
- 命令：`mvn -Penable-tests -pl forge-flow/forge-flow-client -am -Dtest=RemoteJobFlowExecutorTest -Dsurefire.failIfNoSpecifiedTests=false test`，结果 `BUILD SUCCESS`。
- 命令：`mvn -pl forge-flow/forge-flow-server -am -DskipTests compile`，32 个 Reactor 模块全部 `SUCCESS`。
- 非阻断警告：既有模块仍有 deprecated/unchecked 编译提示，本轮未新增对应调用。
- 服务：未启动真实服务，无 PID 需要清理。

## 2026-07-21 Job 执行链路验证

```bash
cd forge-server
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home \
PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH \
mvn -pl forge-framework/forge-plugin-parent/forge-plugin-job \
  -Penable-tests -Dforge.test.groups= \
  -Dtest=JobFlowMigrationContractTest,JobSchedulerTest,JobConfigValidatorTest,JobFlowOrchestrationServiceTest,QuartzJobExecutorTest,JobExecutionLifecycleServiceTest test
```

- 结果：`48/48` 通过，Failures `0`、Errors `0`、Skipped `0`，`BUILD SUCCESS`。
- 覆盖：V1.0.48 迁移合约、Quartz 固定绑定快照、FLOW 参数对象边界、可信绑定、确定性 businessKey、processInstanceId 日志终态和 SINGLE 兼容。
- 预期日志：`JobSchedulerTest` 主动关闭 Scheduler 后验证异常透传，会输出一次 `SchedulerException`；`QuartzJobExecutorTest` 包含失败分支；生命周期幂等用例会输出“终态更新被忽略” WARN。均为故障注入预期，不是测试失败。

## 2026-07-21 Flow API 权限加固

- 复核发现专用 Job Flow Controller 只有登录门禁、没有业务权限；已在类级增加 `@SaCheckPermission("system:jobConfig:trigger")`，复用既有“立即运行任务”权限。
- 远程 `FORGE_FLOW_JOB_REMOTE_TOKEN` 必须属于拥有该权限的服务账号；Flowable 实际发起身份仍只取 Flow 服务技术身份配置，不取 Token 用户或请求体身份字段。

```bash
cd forge-server
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home \
PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH \
mvn -pl forge-flow/forge-flow-server \
  -Penable-tests -Dforge.test.groups= \
  -Dtest=JobFlowControllerSecurityContractTest test
```

- 结果：`1/1` 通过，Failures `0`、Errors `0`、Skipped `0`，`BUILD SUCCESS`。

## 2026-07-21 前端与聚合构建

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh
nvm use v20.19.0
pnpm exec vitest run \
  src/views/system/job-config/__tests__/job-config-form.test.js \
  src/views/system/job-config/__tests__/job-permission.test.js \
  src/views/system/__tests__/job-log-query.test.js \
  src/views/system/__tests__/job-api-token.test.js
```

- 结果：4 个测试文件、`25/25` 用例通过；覆盖 FLOW payload、JSON 对象边界、Long ID 字符串、权限操作和日志查询。
- 既有前端定向 ESLint 已通过；前端生产构建成功，`8721 modules transformed`，耗时约 `2m 6s`。

```bash
cd forge-server
mvn -pl forge-flow/forge-flow-server -am package -DskipTests
mvn -pl forge-admin-server -am package -DskipTests
```

- Flow Server：`32/32` Reactor 模块成功，`BUILD SUCCESS`。
- Admin Server：`43/43` Reactor 模块成功，`BUILD SUCCESS`。
- 非阻断警告：既有 `UserSelectModal` 自动组件命名冲突、既有 CSS `//` 注释和部分静态/动态 import chunk 提示；均未阻断构建，本轮未扩展这些问题。

## 2026-07-21 最终静态门禁

```bash
rg -n '\$\{[^}]+\}' forge-server/db/migration
rg -n 'tenant_id\s*=\s*0' forge-server/db/migration/V1.0.48__add_job_flow_orchestration.sql
xmllint --noout \
  forge-server/forge-framework/forge-plugin-parent/forge-plugin-job/src/main/resources/mapper/SysJobConfigMapper.xml \
  forge-server/forge-framework/forge-plugin-parent/forge-plugin-job/src/main/resources/mapper/SysJobLogMapper.xml \
  forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/resources/mapper/FlowModelMapper.xml
rg -n 'RestTemplate|WebClient|HttpClient|OkHttpClient|openConnection' <V9生产文件>
rg -n 'X-Inner-Call|X_INNER_CALL' <V9生产文件>
rg -n 'ScriptEngine|GroovyShell|ProcessBuilder|Runtime.getRuntime().exec|SpelExpressionParser|Class.forName|.invoke(' <V9生产文件>
rg -n 'latestVersion|latest(' <V9生产文件>
git diff --check
```

- Flyway placeholder：无匹配，`rg` 按无匹配语义返回 `1`，符合预期。
- V1.0.48：无 `tenant_id=0`，字典数据固定 `tenant_id=1`，INSERT 显式列名并使用 `NOT EXISTS`。
- Mapper XML：3 个文件均由 `xmllint` 校验通过。
- V9 生产链路：无直连 HTTP、无 `X-Inner-Call`、无 latestVersion、无任意脚本/反射执行入口；既有 SINGLE/BEAN 的 `Method.invoke` 不属于 FLOW 新链路。
- `git diff --check`：返回 `0`，无空白错误。

## 跳过项与环境状态

- 未启动真实 MySQL、Admin、Flow、Vite 或外部 HTTP 服务；本轮新增服务 PID：无，无需清理。
- 未执行真实 Flyway；未配置或验证 Flow 技术身份、FLOW_API 私网白名单和服务账号 Token；未执行真实已发布流程的定时/手动/Open API E2E。以上由用户侧部署环境验收。
- 未修改 BPMN 节点配置，未新增技术节点库，未修改或清理 V1-V8 未提交成果，未创建 Git 提交。
