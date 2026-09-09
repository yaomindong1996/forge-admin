# 定时任务 Flowable 编排
> status: complete
> created: 2026-07-19
> complexity: 🟡中等
> parent: code-copilot/changes/定时任务优化/spec.md
> version: V9
> dependency: 定时任务可靠性加固、定时任务出站安全
> ui-baseline: code-copilot/changes/定时任务优化/ui-reference.md

## 1. 目标

允许定时任务启动一个真实、已发布、固定版本的 Flowable 流程，并把任务执行记录与 processInstanceId 关联。本版本不建设第二套流程设计器，也不新增任意脚本或业务节点库。

## 2. 功能范围

- [x] 调用方式增加 SINGLE、FLOW。
- [x] FLOW 任务只能绑定 status=1、已有 deploymentId 和真实流程定义的已发布模型。
- [x] 保存时固定模型 Key、版本和 deploymentId，后续发布新版本不静默改变已有任务。
- [x] 保存并按 Flowable processDefinitionId 启动精确定义，禁止执行时查询 latestVersion。
- [x] 任务插件通过 JobFlowExecutor SPI 启动流程，不直接依赖审批业务 Service。
- [x] 提供 Admin 内嵌 Flowable 和独立 Flow 服务两种适配器，首轮只验收实际部署形态。
- [x] 发起人使用平台配置的技术服务身份，不接受任务参数覆盖 userId、tenantId 或 activeOrgId。
- [x] 独立 Flow 服务调用必须经过 V8 SecureOutboundClient 和 FLOW_API 白名单，不发送 X-Inner-Call。
- [x] 执行日志记录 process_instance_id，详情可以跳转现有流程历史。
- [x] 流程启动成功即表示调度任务成功；后续流程节点失败由 Flowable 历史和流程告警负责，不回写为调度启动失败。

## 3. 明确不做

- 不创建 mes_flow、mes_flow_node 或第二套流程画布。
- 不自动部署草稿或启动最新未绑定版本。
- 不开放任意 Script、Java、Shell、SQL 或不受控 SpEL。
- 不新增采集、转换、校验、API 调用等专用节点；API 节点另立变更并复用 V7。
- 不把审批待办结果回写成任务执行状态。

## 4. 数据变更

| 表 | 字段 | 说明 |
|---|---|---|
| sys_job_config | invoke_mode | SINGLE/FLOW |
| sys_job_config | flow_model_key | 已发布模型 Key |
| sys_job_config | flow_model_version | 保存时固定版本 |
| sys_job_config | flow_deployment_id | 保存时固定部署 |
| sys_job_config | flow_process_definition_id | Flowable 精确定义 ID |
| sys_job_log | process_instance_id | 真实流程实例关联 |

## 5. SPI 契约

- JobFlowBindingVO validateBinding(String modelKey, Integer version)
- JobFlowExecutionResult start(JobFlowExecutionRequest request)
- JobFlowExecutionResult findByBusinessKey(String businessKey)

JobFlowExecutionRequest 只包含可信任务 ID、执行 ID、绑定快照和受控业务参数；身份上下文由适配器服务端注入。

任务流程 businessKey 固定为 `job:<jobConfigId>:<executionId>`。任务参数统一放入 `jobInput` 变量，不允许覆盖 initiator、businessKey、tenantId、activeOrgId 等可信变量。

## 6. 验收标准

- 草稿、挂起、无部署或版本不匹配的模型无法绑定。
- 发布新流程版本后，旧任务仍启动原绑定版本。
- 定时和手动触发都能关联唯一 processInstanceId。
- FLOW 服务不可用时任务启动失败并记录安全错误，不降级为其它版本或本地草稿。
- 远程响应丢失时按 businessKey 查询并恢复原 processInstanceId，不重复启动流程。
- 代码扫描不存在任意脚本执行入口。

## 7. 确认门禁

- [x] 确认任务绑定保存时固定已发布流程版本和 processDefinitionId。
- [x] 确认首验部署形态是独立 Flow 服务；本地适配器只做契约和自动化验证。
- [x] 确认本版本只做流程启动，不新增技术节点库。

## 8. 实施决策

- `invoke_mode` 是 SINGLE/FLOW 上层编排模式，既有 `execute_mode` 只在 SINGLE 模式下解释为 BEAN/HANDLER/RPC。
- JobFlowExecutor 契约放入中立的 forge-starter-job，job 插件不依赖 forge-plugin-flow，避免业务插件循环依赖。
- 绑定保存时由 Flow 端返回可信快照；前端提交的 deploymentId/processDefinitionId 不直接入库。
- 独立部署使用 RemoteJobFlowExecutor，通过 SecureOutboundClient 调用专用 Job Flow API；目标必须命中 FLOW_API 白名单。
- Flow 端从服务配置注入技术身份，远程请求体不接受 userId、tenantId、activeOrgId 或任意扁平流程变量。
- Flow 端按 tenantId + businessKey 幂等；启动成功即完成任务日志，后续流程结果不回写任务状态。
- 专用 Job Flow API 复用 `system:jobConfig:trigger` 权限；远程 Token 必须属于拥有该权限的服务账号。

## 9. 完成结论

- V9 代码、权限复核、目标测试、Flow/Admin 聚合装配、前端构建和静态门禁均已通过。
- Job 专项 `48/48`、Flow 运行时 `7/7`、远程适配 `6/6`、Flow Controller 权限合约 `1/1`、前端任务回归 `25/25` 通过。
- Flow Server `32/32`、Admin Server `43/43` Reactor 模块构建成功；前端生产构建成功，处理 `8721` 个模块。
- 真实 MySQL Flyway、Flow 技术身份、FLOW_API 私网白名单、服务账号 Token 和真实流程 E2E 未在本轮启动，保留为用户侧环境验收项。
