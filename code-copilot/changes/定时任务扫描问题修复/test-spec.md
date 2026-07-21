# 定时任务扫描问题修复增量测试计划

> status: complete
> version: V10
> created: 2026-07-21
> baseline: V9 Job `48/48`、Flow Client `6/6`、前端 `25/25`、Admin/Flow 聚合构建成功

## 1. 本轮差异

- 内部执行器增加显式启用和专用服务 Token，RPC 改走 `SecureOutboundClient/JOB_RPC`。
- 日志清理保护非终态和有效幂等记录，启动时恢复悬挂执行。
- 配置同步增加任务级串行和版本条件，ONCE Misfire 与并行失败统计补齐顺序语义。
- Flow 5xx/解析失败进入 businessKey 恢复，监控和前端补齐 ACCEPTED/FLOW/权限状态。

## 2. P0 验证

| 编号 | 场景 | 预期 |
|---|---|---|
| P0-01 | 执行器认证 | 端点默认关闭；无效 Bearer 不能获取或执行 Handler |
| P0-02 | RPC 结果协议 | 仅 HTTP 2xx 且 `RespInfo.code=200` 算成功 |
| P0-03 | RPC 出站安全 | 只走 JOB_RPC 受控客户端，目标必须命中独立白名单 |
| P0-04 | 清理竞态 | RUNNING、ACCEPTED、有效幂等引用不会被物理删除 |
| P0-05 | 同步一致性 | 旧版本不能覆盖新版本，逻辑删除后不能复活 Quartz Job |
| P0-06 | Flow 状态未知 | 5xx/解析失败按 businessKey 恢复且不重复启动流程 |

## 3. P1 验证

| 编号 | 场景 | 预期 |
|---|---|---|
| P1-01 | 崩溃恢复 | 超时 RUNNING/ACCEPTED 在启动时幂等终结为 FAILED |
| P1-02 | ONCE Misfire | past + DO_NOTHING 进入已结束且不创建活跃 Trigger |
| P1-03 | 并行统计 | 旧执行晚完成不能覆盖较新连续失败语义 |
| P1-04 | 监控闭合 | total 等于 success/failed/running/skipped/accepted 之和 |
| P1-05 | 前端显示 | FLOW 标签使用 invokeMode；无路由权限不展示流程历史入口 |
| P1-06 | 兼容性 | BEAN/HANDLER、Open API、Flow 编排和日志查询既有测试无回归 |

## 4. 计划命令

```bash
cd forge-server
mvn -pl forge-framework/forge-starter-parent/forge-starter-outbound \
  -Penable-tests -Dforge.test.groups= test
mvn -pl forge-framework/forge-plugin-parent/forge-plugin-job \
  -Penable-tests -Dforge.test.groups= test
mvn -pl forge-flow/forge-flow-client \
  -Penable-tests -Dforge.test.groups= -Dtest=RemoteJobFlowExecutorTest test
mvn -pl forge-admin-server -am package -DskipTests
```

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh
nvm use v20.19.0
pnpm exec vitest run src/views/system/job-config/__tests__ src/views/system/__tests__
NODE_OPTIONS=--max-old-space-size=8192 pnpm build
```

## 5. 静态门禁

```bash
rg -n '\$\{[^}]+\}' forge-server/db/migration
rg -n 'HttpRequest|HttpClient|RestTemplate|WebClient|openConnection' \
  forge-server/forge-framework/forge-plugin-parent/forge-plugin-job/src/main/java
xmllint --noout <changed-mapper-xml>
git diff --check
```

## 6. 跳过项

- 不自动启动真实 MySQL、Redis、Admin、Flow、Vite 或远程执行器服务。
- 真实 Flyway、JOB_RPC 私网白名单、服务 Token、Quartz 集群竞态、登录态 UI 和真实流程 E2E 由用户侧环境验收。
- 不清理或回退 V1-V9 未提交成果，不自动 Push。

## 7. 执行结果

| 验证项 | 结果 |
|---|---|
| Job 全模块测试 | `178/178` 通过 |
| Outbound 全模块测试 | `48/48` 通过 |
| Remote Flow Client | `9/9` 通过 |
| 定时任务前端测试 | `29/29` 通过 |
| Admin / Flow 聚合构建 | `43/43`、`32/32` 模块通过 |
| 前端生产构建 | 通过，保留仓库既有构建警告 |
| 静态门禁 | 全部通过 |

P0/P1 自动化覆盖项全部通过；真实环境项仍按第 6 节执行人工验收。
