# 定时任务扫描问题修复
> status: complete
> created: 2026-07-21
> complexity: 🔴复杂
> parent: code-copilot/changes/定时任务优化/spec.md
> version: V10
> dependency: 定时任务可靠性加固、定时任务开放API、定时任务出站安全、定时任务Flowable编排

## 1. 背景

V1-V9 功能实现完成后的整体静态审查发现，内部 RPC 执行器仍可绕过管理权限，远程执行只判断 HTTP 状态且未进入受控出站链路；日志清理、进程崩溃、配置并发同步、一次性 Misfire 和并行失败统计也存在状态悬挂或乱序覆盖风险。V10 只修复这些已确认问题，不新增任务类型或第二套调度协议。

## 2. 目标

- [x] 内部执行器默认关闭，显式开启后只接受专用服务 Token，并且不记录任务参数、结果或异常明文。
- [x] RPC 调用统一经过 `SecureOutboundClient/JOB_RPC`，同时校验 HTTP 状态和 `RespInfo.code`，失败必须进入重试、失败日志和告警链路。
- [x] `JOB_RPC` 使用独立白名单场景，只有显式白名单才能访问，私网访问必须由该场景单独授权。
- [x] 日志清理不得删除 `RUNNING`、`ACCEPTED` 或仍在幂等有效期内引用的执行记录，留存天数必须限制在 0 到 3650 天。
- [x] 启动时把超过恢复阈值的 `RUNNING/ACCEPTED` 记录终结为失败，避免永久悬挂。
- [x] DB 到 Quartz 同步按任务 ID 串行，并以配置版本作为同步状态更新条件，删除不能被旧同步请求复活。
- [x] ONCE + DO_NOTHING 错过执行时间后进入已结束状态，不保留无 Trigger 的运行中配置。
- [x] 连续失败数只由最新完成的执行结果推进，ALLOW 并行任务乱序完成不能污染统计。
- [x] Flow 远程启动遇到 5xx、传输错误或响应解析错误时按 businessKey 恢复；确定性 4xx 和明确业务失败不恢复。
- [x] 监控摘要显式统计 ACCEPTED；FLOW 列表显示编排模式；流程历史入口受真实路由权限控制。

## 3. 安全协议

- 执行器端点固定为 `POST /job/executor/execute`，但 `forge.job.executor-enabled` 缺省为关闭。
- 调度中心使用 `Authorization: Bearer <forge.job.executor-token>` 调用；Token 必须由环境变量注入且不少于 32 个字符。
- 执行端使用常量时间比较验证 Token。通用登录和 API 资源拦截器对该内部端点忽略，但自定义服务认证失败时返回 HTTP 401；配置缺失返回 HTTP 503。
- RPC 请求体只包含 `handlerName` 和 `param`；服务端日志只记录 handlerName，响应失败不返回内部异常详情。
- `executorService` 继续兼容 `host[:port]`，最终 URL 固定拼接 `/job/executor/execute`，由 `JOB_RPC` 白名单执行 DNS、IP、端口、重定向和响应大小检查。

## 4. 状态一致性

- 清理 SQL 只物理删除终态日志，并用 `NOT EXISTS` 排除 `expires_at > NOW()` 的有效幂等引用。
- 启动恢复阈值默认 15 分钟，可通过 `forge.job.execution-recovery-timeout` 配置，合法范围 1 分钟到 24 小时。
- 悬挂 `RUNNING/ACCEPTED` 统一转为 FAILED，写入脱敏的系统恢复原因和结束时间；恢复动作必须幂等。
- 配置同步使用任务 ID 本地互斥锁；同步结束时 `WHERE id = ? AND version = ?` 更新状态。若版本已变化，不覆盖新状态并重新读取最新版本同步。
- 连续失败统计保存最后计数执行的完成时间和执行 ID，只有更新顺序不早于已记录顺序时才允许修改。

## 5. 数据迁移

- 新增 `V1.0.49__fix_job_scheduler_scan_findings.sql`。
- 出站场景字典新增 `JOB_RPC`，更新私网约束为仅 `FLOW_API/JOB_RPC` 可授权。
- `sys_job_config` 增加失败统计顺序字段，默认不影响既有记录。
- 迁移必须使用 `information_schema` 防重复保护，业务内置数据 `tenant_id=1`。

## 6. 明确不做

- 不实现 Nacos/Eureka/Consul 服务发现；`executorService` 仍由现有配置提供。
- 不允许任意 URL 路径、任意请求头或前端下发服务 Token。
- 不自动启动真实 MySQL、Redis、Admin、Flow 或外部执行器服务。
- 不修改 V1-V9 已执行迁移脚本，不清理现有未提交成果。

## 7. 验收标准

- 无 Token、错误 Token、短 Token 和未显式开启端点均不能执行 Handler。
- RPC HTTP 401/500、HTTP 200 + `RespInfo.code != 200`、非法 JSON 都判为失败；成功只返回 `RespInfo.data`。
- 未配置 JOB_RPC 白名单、解析到未授权私网、重定向越界均被拒绝。
- 清空日志与 Open API ACCEPTED 并发时，预留执行仍能进入 RUNNING；有效幂等键仍可返回原执行。
- 重启恢复后不存在超过阈值的 RUNNING/ACCEPTED；重复恢复不会重复改写终态。
- 并发修改/删除任务后，Quartz 最终状态与数据库最新版本一致。
- 过去时间的 ONCE + DO_NOTHING 配置恢复后为已结束且没有活跃 Trigger。
- Flow 503 后能查回原实例，Flow 400 或明确业务失败不发起恢复查询。
- 后端目标测试、聚合构建、前端定向测试与生产构建通过；真实环境项明确保留给用户验收。

## 8. 实施结果

- Job 模块 `178/178`、Outbound `48/48`、Remote Flow Client `9/9`、定时任务前端 `29/29` 通过。
- Admin Reactor `43/43`、Flow Reactor `32/32` 和前端生产构建通过。
- Flyway 占位符、`tenant_id=0`、Mapper XML、Job 直连 HTTP、迁移版本唯一性、ESLint 和 `git diff --check` 门禁通过。
- 真实 Flyway、Redis/Quartz 集群竞态、JOB_RPC 服务调用、登录态 UI 和 Flow E2E 未在本轮启动，保留为用户侧环境验收。
