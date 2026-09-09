# 定时任务告警权限审计测试计划
> status: complete
> updated: 2026-07-20

## 1. 验证目标

复用 V5 Job 模块 94/94、前端定向测试 16/16、Admin Reactor 42/42 和生产构建基线，增量验证 V6 的最终失败告警、消息渠道、权限矩阵、Service 二次校验、安全审计和告警配置交互。

## 2. 验证矩阵

| 优先级 | 范围 | 验证方式 |
|---|---|---|
| P0 | 告警字段、渠道字典、按钮/API 权限资源 | Flyway 合约测试、placeholder 扫描 |
| P0 | 重试成功不告警、最终失败只告警一次 | JobExecutionLifecycleServiceTest、JobFailureAlarmServiceTest |
| P0 | WEB/EMAIL 选择、消息失败不覆盖 FAILED | JobFailureAlarmServiceTest |
| P0 | 危险目标、受保护任务、敏感详情二次校验 | JobManagementSecurityServiceTest、Service 测试 |
| P0 | Controller 权限注解和安全审计 | Controller 合约测试、审计快照单测 |
| P1 | 告警表单归一化、校验和操作可见性 | 前端定向 Vitest、ESLint、生产构建 |

## 3. 必跑命令

- Job 模块：`mvn -Penable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-job -am test`。
- 前端单测：Job 配置表单、权限和日志查询相关 Vitest。
- 前端检查：V6 定向 ESLint、`pnpm build`。
- 静态检查：Mapper XML、Flyway placeholder、权限资源、敏感字段、冲突标记和 `git diff --check`。
- 聚合验证：`mvn -pl forge-admin-server -am package -DskipTests`。

## 4. 边界

- 不启动真实 MySQL、Redis、Admin 或 Quartz 集群；Flyway、消息投递、操作日志落库和真实权限分配由开发环境验收。
- 浏览器检查只验证本地前端可达时的结构和交互，不把模拟数据表述为真实后端联调。
- 本轮不增加 Webhook、开放 API、角色模板或普通租户自助任务。

## 5. 执行结果

- Job 模块：Java 17 下执行 `mvn -Penable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-job -am test`，Job 模块 106/106 通过，Reactor 全部成功。
- 前端单测：Node 20.19.0 下执行三组定向 Vitest，3 个测试文件、18/18 通过。
- 前端检查：V6 定向 ESLint 无输出通过；`NODE_OPTIONS=--max-old-space-size=8192 pnpm build` 生产构建通过，保留仓库既有 CSS 注释、组件命名冲突和动态/静态导入警告。
- 聚合验证：Java 17 下执行 `mvn -pl forge-admin-server -am package -DskipTests`，42/42 模块成功，生成 `forge-admin-server.jar`。
- 静态检查：两个 Job Mapper XML 通过 `xmllint`；Flyway placeholder、权限绕过、V6 Webhook/角色模板、冲突标记扫描均无命中；`git diff --check` 通过。
- 生产预览：`vite preview` 返回 HTTP 200，Vue 根节点和 JS/CSS 资源已加载，无 console error 或 page error；因后端未启动，页面停在启动加载壳且未达到 `networkidle`，未表述为业务接口联调通过。
- 跳过项：未执行真实 Flyway、消息投递、角色权限分配、操作日志落库和调度集群验证，按测试边界留待开发环境验收。
