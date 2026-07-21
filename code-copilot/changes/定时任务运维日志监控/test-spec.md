# 定时任务运维日志监控测试计划
> status: complete
> updated: 2026-07-20

## 1. 验证目标

复用 V4 Job 模块 84/84、前端定向测试 13/13、Admin Reactor 42/42 和生产构建基线，增量验证 V5 的组合筛选、安全详情、导出白名单、连续失败计数、任务概览和近 24 小时监控摘要。

## 2. 验证矩阵

| 优先级 | 范围 | 验证方式 |
|---|---|---|
| P0 | consecutive_failures、组合索引和导出配置 | Flyway 合约测试、placeholder 扫描 |
| P0 | 列表/详情不返回 jobParam，导出排除结果与异常 | Mapper XML 和 VO 合约测试 |
| P0 | 成功清零、失败递增、跳过不改变 | JobExecutionLifecycleServiceTest |
| P0 | 时间、状态、来源组合筛选 | JobMapperXmlContractTest |
| P0 | 任务概览和 24 小时摘要 | Service/Mapper 聚合测试 |
| P1 | 筛选、详情、导出、全局摘要和任务概览 | 前端定向 Vitest、ESLint、生产构建、浏览器检查 |

## 3. 必跑命令

- Job 模块：`mvn -Penable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-job -am test`
- 前端单测：Job 日志与任务列表相关 Vitest。
- 前端检查：V5 定向 ESLint、`pnpm build`。
- 静态检查：Mapper XML、Flyway placeholder、Service 查询规范、冲突标记、导出敏感字段和 `git diff --check`。
- 聚合验证：`mvn -pl forge-admin-server -am package -DskipTests`。

## 4. 边界

- 不启动真实 MySQL、Redis、Admin 或 Quartz 集群；Flyway、真实接口数据和运行态聚合由开发环境验收。
- 浏览器检查只验证本地前端可达时的结构和交互，不把模拟数据表述为真实后端联调。
- 本轮不增加告警、权限、Webhook、复杂 BI 或数据库分区。

## 5. 执行结果

| 验证项 | 结果 |
|---|---|
| V5 后端定向测试 | 15/15 通过 |
| Job 模块全量测试 | 94/94 通过 |
| 前端定向 Vitest | 16/16 通过 |
| 前端定向 ESLint | 通过 |
| 前端生产构建 | 通过 |
| Admin Reactor | 42/42，`package -DskipTests` 通过 |
| Mapper XML / Flyway / 冲突标记 / diff 静态检查 | 通过 |
| 桌面与窄屏生产预览 | 入口 200、启动壳正常、无脚本或静态资源错误；任务页因后端未启动未覆盖 |

## 6. 环境验收项

- 在已启动 MySQL、Redis 和 Admin 的开发环境执行 V1.0.44，并检查 `forge_schema_history`、新增字段、索引、字典和导出配置。
- 使用管理员登录后验证日志组合筛选、安全详情、Excel 导出、任务概览和近 24 小时摘要。
- 构造成功、失败、跳过记录，核对连续失败计数和运行中日志不被留存清理删除。

## 7. V5.1 增量验证

| 编号 | 场景 | 预期 |
|---|---|---|
| V5.1-P0-01 | 列表开始时间 | 优先展示 `startTime`，缺失时依次回退 `triggerTime`、`scheduledFireTime` |
| V5.1-P1-01 | 主列表层级 | 默认不展示执行器和结束时间，状态与来源组合展示，详情操作保持权限控制 |
| V5.1-P1-02 | 详情层级 | 右侧抽屉分开展示概览、时间线、结果/异常和默认折叠的技术信息 |
| V5.1-P1-03 | 响应式 | 桌面和窄屏下筛选、表格操作及抽屉内容不重叠 |
| V5.1-P1-04 | 前端质量 | 目标 Vitest、目标 ESLint、生产构建和空白检查通过 |

```bash
source ~/.nvm/nvm.sh && nvm use v20.19.0
pnpm exec vitest run src/views/system/__tests__/job-log-query.test.js
pnpm exec eslint src/views/system/job-log-list.vue \
  src/views/system/job-config.vue \
  src/views/system/job-log-query.js \
  src/views/system/__tests__/job-log-query.test.js
NODE_OPTIONS=--max-old-space-size=8192 pnpm build
```

本轮不修改后端，不启动 MySQL、Redis、Quartz 或 Admin；浏览器检查仅在现有可用前端预览与登录条件允许时执行，不把未覆盖的真实业务数据表述为已验收。

### V5.1 执行结果

- TDD：新增开始时间回退测试首次执行 `1 failed / 4 passed`，失败原因为 `resolveJobLogStartedAt is not a function`；实现后最终 `5/5` 通过。
- 目标 ESLint：四个 Vue/JS/测试文件无输出、退出码 0。
- 生产构建：Node `v20.19.0` 下通过，8725 个模块完成转换，生成 `job-log-list` 产物。
- 静态检查：`git diff --check` 和新增/修改目标文件尾随空白检查无输出。
- 浏览器检查：发现工作区已有 5173 与 8580 监听进程，但当前沙箱无法访问本地监听端口，且 Chromium 因 MachPort 权限被拒绝无法启动；未把桌面/窄屏真实渲染标记为通过，也未停止用户已有服务。
