# 定时任务扫描问题修复 Implementation Plan

> **For agentic workers:** 按仓库 code-copilot 工作流在当前会话内联执行；逐项运行目标测试，不创建额外 worktree，不回退 V1-V9 工作区成果。

> status: complete
> scope: V10 only

**Goal:** 关闭整体扫描发现的安全绕过、状态悬挂和并发一致性缺口，使现有定时任务、开放 API 和 Flow 编排在失败与重启场景下保持可判定状态。

**Architecture:** RPC 复用 V8 受控出站客户端并增加独立 JOB_RPC 场景，执行端用显式服务 Token 认证。生命周期修复集中在 Mapper 原子 SQL、启动恢复服务和按任务 ID 串行的协调器中；Flow 与前端只做协议边界补齐。

**Tech Stack:** Java 17、Spring Boot 3、Quartz、MyBatis XML、SecureOutboundClient、Flyway、Vue 3、Naive UI、JUnit 5、Mockito、Vitest。

---

## Task 1: Spec 与测试基线

**Files:**
- Create: `code-copilot/changes/定时任务扫描问题修复/spec.md`
- Create: `code-copilot/changes/定时任务扫描问题修复/tasks.md`
- Create: `code-copilot/changes/定时任务扫描问题修复/test-spec.md`
- Create: `code-copilot/changes/定时任务扫描问题修复/execution-log.md`

- [x] 固化 12 项扫描问题、边界和修复顺序。
- [x] 复用 V9 成功基线并建立 V10 P0/P1 增量测试矩阵。
- [x] 完成后回填执行证据、跳过项、父路线图和可复用决策。

## Task 2: 执行器认证与 RPC 受控出站

**Files:**
- Modify: `JobExecutorEndpoint.java`, `RemoteJobExecutorRouter.java`, `JobProperties.java`
- Modify: `forge-plugin-job/pom.xml`, `application-job-example.yml`
- Modify: `OutboundScenes.java`, `DefaultOutboundPolicyService.java`
- Create: `V1.0.49__fix_job_scheduler_scan_findings.sql`
- Test: endpoint、router、outbound policy 和 migration contract tests

- [x] 先添加端点默认关闭、Bearer 校验、响应脱敏和业务错误状态测试。
- [x] 先添加 RPC 请求进入 JOB_RPC、携带服务 Token、解析 `RespInfo` 和拒绝失败响应测试。
- [x] 先添加 JOB_RPC 私网必须显式授权和迁移字典/约束测试。
- [x] 实现最小认证、受控请求和双层成功判定，删除 Hutool 直连 HTTP。
- [x] 运行 Job 与 Outbound 目标测试。

## Task 3: 日志清理与悬挂执行恢复

**Files:**
- Modify: `JobLogController.java`, `SysJobLogServiceImpl.java`
- Modify: `SysJobLogMapper.java` / `SysJobLogMapper.xml`
- Create: `JobExecutionRecoveryService.java`
- Modify: `JobStartupReconciler.java`, `JobProperties.java`
- Test: service、mapper contract 和 startup recovery tests

- [x] 添加 days 边界、ACCEPTED/RUNNING 排除和有效幂等引用排除测试。
- [x] 添加超时 RUNNING/ACCEPTED 原子失败与重复恢复幂等测试。
- [x] 实现清理 SQL、恢复 SQL 和启动调用，异常原因只使用固定脱敏文本。
- [x] 运行生命周期、日志服务和启动恢复目标测试。

## Task 4: 调度同步、ONCE Misfire 与并行统计

**Files:**
- Modify: `JobScheduleCoordinator.java`, `SysJobConfigMapper.java` / XML
- Modify: `JobScheduler.java`, `JobOnceCompletionService.java`
- Modify: `JobExecutionLifecycleService.java`, `SysJobLogMapper.java` / XML
- Modify: V1.0.49 migration
- Test: coordinator、scheduler、once completion 和 lifecycle tests

- [x] 添加同任务同步串行、版本变化重试和删除不复活测试。
- [x] 添加 past ONCE + DO_NOTHING 直接完成测试。
- [x] 添加 ALLOW 并行执行乱序完成不会覆盖较新统计测试。
- [x] 用任务级锁、版本条件更新和完成顺序条件实现最小修复。
- [x] 运行调度与生命周期目标测试。

## Task 5: Flow、监控和前端契约

**Files:**
- Modify: `RemoteJobFlowExecutor.java` / `RemoteJobFlowExecutorTest.java`
- Modify: `SysJobLogMapper.xml`, `JobMonitorSummaryVO.java`, `JobObservabilityManager.java`
- Modify: `job-config.vue`, `job-log-list.vue` and frontend tests

- [x] 添加 Flow 5xx/解析失败恢复、4xx/业务失败不恢复测试。
- [x] 添加 ACCEPTED 指标闭合测试并扩展前端摘要归一化。
- [x] FLOW 列表使用 `sys_job_invoke_mode + invokeMode`，流程历史按钮只在路由可访问时显示。
- [x] 运行 Flow Client、Job 监控和前端定向测试。

## Task 6: 聚合验证与回填

- [x] 执行 Flyway placeholder、tenant_id、Mapper XML、直连 HTTP 和 `git diff --check` 静态门禁。
- [x] 运行 Job/Outbound/Flow Client 目标测试和 Admin/Flow 聚合构建。
- [x] 运行前端定向测试与生产构建。
- [x] 回填 spec/tasks/test-spec/execution-log 和父路线图；真实 Flyway、Redis、Quartz、RPC、登录态 UI、Flow E2E 标为用户侧验收。
