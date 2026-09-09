# 执行日志 — 业务域删除孤立应用入口修复
> status: completed
> created: 2026-08-05

## 1. 基线

- 当前分支：`main`。
- 基线提交：`1812fc69 [tenant-switch-user-session-loop-fix] 修复跨租户切换会话循环`。
- 用户既有工作树变更：`M .DS_Store`、`D forge/.DS_Store`，本轮保持不动。
- 前序业务域删除提交：`bf62275b [business-domain-delete-orphan-object-fix] 优化业务域孤立对象删除`。

## 2. 研究结论

| 范围 | 根因 | 证据 |
|------|------|------|
| 应用删除 | 停用入口只解除 `application_id`，不逻辑删除 | `BusinessApplicationService#delete`、`BusinessAppMapper#detachDisabledByApplicationId` |
| 业务域删除 | `countAppsBySuite` 统计所有未删除入口并在显式对象清理前直接阻断 | `BusinessSuiteService#delete`、`BusinessSuiteMapper.xml` |
| 前端 | 删除确认只看 `objectCount`，忽略汇总已有的 `appCount` | 应用中心列表与业务域详情删除函数 |

## 3. 已执行记录

| 时间 | 范围 | 动作 | 结果 | 备注 |
|------|------|------|------|------|
| 2026-08-05 | 工作树基线 | `git status --short`、最近提交检查 | passed | 仅既有 `.DS_Store` 变更 |
| 2026-08-05 | 根因定位 | 检查业务域、业务应用、访问入口 Service/Mapper/前端删除链路 | passed | 确认为应用删除后保留的孤立入口被硬阻断 |
| 2026-08-05 | 计划 | 创建 Spec、Tasks、Test Spec、Execution Log | passed | 进入 TDD 阶段 |
| 2026-08-05 | Red 回归 | 在旧实现上运行新增孤立入口场景 | expected failed | 8 个用例中 2 failures + 1 error，均被“该业务域下仍有应用入口”旧分支提前阻断 |
| 2026-08-05 | 后端实现 | 新增有效应用引用校验、入口菜单停用和 `del_flag=id` 逻辑删除 | passed | 所有入口/对象引用校验完成后才开始任何清理副作用 |
| 2026-08-05 | 前端实现 | 概览页与业务域详情同时消费 `appCount/objectCount` | passed | 任一数量大于 0 即展示明细并传递兼容参数 `cleanupOrphanObjects=true` |
| 2026-08-05 09:56 | 定向 JUnit | `mvn -Penable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-generator -Dtest=BusinessSuiteServiceTest test` | passed | Tests run: 9, Failures: 0, Errors: 0, Skipped: 0 |
| 2026-08-05 09:56 | Mapper XML | `xmllint --noout .../BusinessSuiteMapper.xml` | passed | XML 语法通过；SQL 显式限定 `tenant_id/suite_code/del_flag=0` |
| 2026-08-05 09:57 | Generator 聚合编译 | JDK 17 `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests` | passed | Reactor 30/30 SUCCESS；仅有现存 deprecated/unchecked/Lombok warnings |
| 2026-08-05 09:57 | 前端目标 ESLint | Node 20.19.0 `pnpm exec eslint ...` | passed with warnings | 0 errors；`index.vue` 保留 3 条既有 `vue/attributes-order` warning |
| 2026-08-05 10:00 | 前端生产构建 | Node 20.19.0 `NODE_OPTIONS=--max-old-space-size=8192 pnpm build` | passed | 8848 modules transformed，3m58s；既有组件命名冲突、混合导入、CSS 注释和 chunk 警告不阻断 |
| 2026-08-05 10:00 | 差异检查 | `git diff --check` | passed | 无空白错误 |
| 2026-08-05 10:04 | 精确暂存 | 显式暂存本变更 12 个文件，执行 `git diff --cached --check` | passed | 用户既有 `.DS_Store` 修改/删除未暂存，本轮不推送远端 |

## 4. 服务与数据库

- 本轮启动服务：无。
- 数据库变更：无。
- 遗留 PID：无。
- 未执行真实删除冒烟：遵循用户偏好，本轮不启动 Admin/MySQL/Redis，由部署环境补充最后一个应用删除后的业务域删除冒烟。
