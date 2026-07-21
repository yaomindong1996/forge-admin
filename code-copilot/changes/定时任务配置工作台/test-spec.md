# 定时任务配置工作台测试计划
> status: applying
> updated: 2026-07-19

## 1. 验证目标

复用 V1 已通过的 job 模块 37/37、Admin Reactor 42 模块和前端生产构建基线，增量验证 V2 的处理器目录、Cron 预览、表单转换、简单计划生成、隐藏路由和全屏工作台交互。

## 2. 验证矩阵

| 优先级 | 范围 | 验证方式 |
|---|---|---|
| P0 | 处理器目录仅来自显式注解 | JobExecutorCatalogServiceTest、JobAutoRegistrarTest |
| P0 | Cron 校验、描述和未来 5 次执行 | JobCronServiceTest 固定 Clock |
| P0 | 新建默认停用、编辑无损、提交字段清理 | job-config-form.test.js |
| P0 | 五类简单计划与复杂 Cron 保留 | cron-builder.test.js |
| P1 | 列表摘要、下次执行和最近结果 | Mapper 契约、模块测试、定向 ESLint |
| P1 | 隐藏路由继承任务管理角色范围 | Flyway 静态契约测试 |
| P1 | 1366×768、1920×1080、移动端 | Playwright 截图与交互；环境无法启动时记录阻断 |

## 3. 必跑命令

- Job 模块：`mvn -pl forge-framework/forge-plugin-parent/forge-plugin-job -am test`
- 前端单测：`pnpm exec vitest run <V2 定向测试>`
- 前端检查：定向 ESLint、`pnpm build`
- 静态检查：Mapper XML、Flyway placeholder、`git diff --check`
- 聚合验证：`mvn -pl forge-admin-server -am package -DskipTests`

## 4. 边界

- 不启动真实 MySQL、Redis 或 Quartz 集群，Flyway 实跑和真实调度交互由开发环境联调。
- UI 必须尝试浏览器验证；构建通过不替代真实交互结论。

## 5. 本轮增量结果

- Job 模块最终 `44/44` 通过，新增批平文件路由契约。
- 前端表单/Cron 单测 `7/7` 通过，定向 ESLint 和生产构建通过。
- Admin `mvn -pl forge-admin-server -am package -DskipTests` 的 42 个 Reactor 模块全部成功。
- Mapper XML、Flyway placeholder、Service 查询规范和 `git diff --check` 通过。
- Playwright 第一次因 `listen EPERM 127.0.0.1:4173` 无法启动 Vite；改用直接加载 `dist` 后，Chromium 又因 `MachPortRendezvousServer Permission denied` 退出。不将该项记为通过。
