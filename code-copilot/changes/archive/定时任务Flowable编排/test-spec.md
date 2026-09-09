# 定时任务 Flowable 编排增量测试计划

> status: complete
> version: V9
> created: 2026-07-21
> completed: 2026-07-21
> baseline: V8 Starter `47/47`、Flow `12/12`、Flow Reactor `32/32`、Admin Reactor `43/43`

## 1. 本轮差异

- Job 配置新增 SINGLE/FLOW 上层编排模式和固定 Flowable 定义快照。
- Flow 新增按 processDefinitionId 校验、技术身份注入和 businessKey 幂等启动能力。
- 独立 Flow 服务调用改走 SecureOutboundClient，不新增 X-Inner-Call 或直连客户端。
- 任务执行日志新增 processInstanceId 并提供现有流程历史入口。

## 2. P0 验证

| 编号 | 场景 | 预期 |
|---|---|---|
| P0-01 | 固定版本 | 发布新版本后旧任务仍按保存的 processDefinitionId 启动 |
| P0-02 | 绑定失败关闭 | 草稿、挂起、无部署、版本或定义不匹配均不能保存 FLOW 任务 |
| P0-03 | 精确启动 | 运行链路不查询 latestVersion，不自动部署草稿或缺失定义 |
| P0-04 | 技术身份 | 请求体不能提交 userId/tenantId/activeOrgId，Flow 端只用服务配置身份 |
| P0-05 | 远程安全 | 只走 SecureOutboundClient/FLOW_API，无 X-Inner-Call 或直连 HTTP 客户端 |
| P0-06 | 幂等恢复 | 相同 businessKey 重试返回同一 processInstanceId，不重复启动 |
| P0-07 | 日志关联 | 定时/手动/Open API FLOW 触发都保存唯一 processInstanceId |
| P0-08 | 结果边界 | 启动成功即任务成功；流程节点后续失败不回写任务状态 |

## 3. P1 验证

| 编号 | 场景 | 预期 |
|---|---|---|
| P1-01 | SINGLE 兼容 | V1-V8 任务默认 SINGLE，原 BEAN/HANDLER/RPC 路由无回归 |
| P1-02 | 参数安全 | jobParam 必须是 JSON 对象并只作为 jobInput 嵌套变量 |
| P1-03 | UI 绑定 | 只能选择已发布模型版本，部署快照只读且身份/节点配置不可编辑 |
| P1-04 | 历史跳转 | 有 processInstanceId 的日志详情可跳转现有流程监控 |
| P1-05 | 聚合装配 | Job、Flow、Flow Client、Flow Server、Admin Server 和前端构建通过 |

## 4. 执行命令

```bash
cd forge-server
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home \
PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH \
mvn -pl forge-framework/forge-plugin-parent/forge-plugin-job \
  -Penable-tests -Dforge.test.groups= \
  -Dtest=JobFlowMigrationContractTest,JobSchedulerTest,JobConfigValidatorTest,JobFlowOrchestrationServiceTest,QuartzJobExecutorTest,JobExecutionLifecycleServiceTest test

mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow \
  -Penable-tests -Dforge.test.groups= \
  -Dtest=JobFlowRuntimeServiceTest test

mvn -pl forge-flow/forge-flow-client \
  -Penable-tests -Dforge.test.groups= \
  -Dtest=RemoteJobFlowExecutorTest test

mvn -pl forge-flow/forge-flow-server \
  -Penable-tests -Dforge.test.groups= \
  -Dtest=JobFlowControllerSecurityContractTest test
```

```bash
mvn -pl forge-flow/forge-flow-server -am package -DskipTests
mvn -pl forge-admin-server -am package -DskipTests
```

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh
nvm use v20.19.0
pnpm exec vitest run \
  src/views/system/job-config/__tests__/job-config-form.test.js \
  src/views/system/job-config/__tests__/job-permission.test.js \
  src/views/system/__tests__/job-log-query.test.js \
  src/views/system/__tests__/job-api-token.test.js
NODE_OPTIONS=--max-old-space-size=8192 pnpm build
```

## 5. 跳过项

- 不自动启动真实 MySQL、Admin、Flow 或外部 HTTP 服务；真实 Flyway、服务 Token、FLOW_API 私网白名单和流程实例 E2E 由用户侧环境验收。
- 不创建或修改 BPMN 节点配置，不新增技术节点库。
- 不修改或清理 V1-V8 未提交成果，不创建 Git 提交。

## 6. 执行结果

| 验证项 | 结果 |
|---|---|
| Job V9 专项 | `48/48` 通过，Failures `0`、Errors `0`、Skipped `0` |
| Flow 运行时 | `7/7` 通过，固定定义、挂起拒绝、快照失效和幂等恢复覆盖完整 |
| Remote Flow Client | `6/6` 通过，FLOW_API、Bearer、无 X-Inner-Call 和响应丢失恢复覆盖完整 |
| Flow API 权限合约 | `1/1` 通过，专用接口固定要求 `system:jobConfig:trigger` |
| 前端任务回归 | `25/25` 通过，4 个测试文件全部成功 |
| Flow Server 装配 | `32/32` Reactor 模块成功，`BUILD SUCCESS` |
| Admin Server 装配 | `43/43` Reactor 模块成功，`BUILD SUCCESS` |
| 前端生产构建 | 成功，`8721 modules transformed` |
| 静态门禁 | Flyway placeholder、tenant_id、Mapper XML、直连 HTTP、X-Inner-Call、latestVersion、脚本入口和 `git diff --check` 全部通过 |

自动化范围已完成；第 5 节所列真实环境项仍由用户侧验收，不计入自动化通过结论。
