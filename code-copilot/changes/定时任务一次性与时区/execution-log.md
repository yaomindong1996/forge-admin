# 定时任务一次性与时区执行记录
> status: complete

## 2026-07-19 基线

- 依赖 V1 已完成；V2 功能实施完成，UI 验收由用户自行执行。
- V2 基线：Job 模块 44/44、前端定向单测 7/7、Admin Reactor 42 模块、前端生产构建和静态检查通过。
- 当前最大 Flyway 版本为 V1.0.41，V3 迁移使用 V1.0.42。
- 当前明确部署时区基线为 Asia/Shanghai；V3 新任务默认值和存量任务均据此初始化。
- 本阶段不启动真实服务、不执行 UI 自动化，只保留单测、Lint、构建和静态检查。

## 2026-07-19 V3 完成验证

- 变更范围：CRON/ONCE 互斥模型、IANA 时区、DST 解析、Quartz Trigger、一次性完成态、时区目录接口和全屏工作台适配。
- Job 模块：`JAVA_HOME=$(/usr/libexec/java_home -v 17) PATH="$JAVA_HOME/bin:$PATH" mvn -Penable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-job -am test`，63/63 通过，Reactor 14/14 成功。
- 前端单测：Node 20.19.0 下执行 `pnpm exec vitest run src/views/system/job-config/__tests__/job-config-form.test.js src/components/job/__tests__/cron-builder.test.js`，10/10 通过。
- 前端 Lint：V3 API、列表、表单状态、工作台、计划区、预览区和表单测试定向 ESLint 通过。
- 前端构建：Node 20.19.0 下执行 `pnpm build`，成功；保留仓库既有组件命名冲突、动态/静态导入和 CSS `//` 注释警告，不阻断本变更。
- Admin 聚合：Java 17 下执行 `mvn -pl forge-admin-server -am package -DskipTests`，Reactor 42/42 成功；保留仓库既有 deprecation/unchecked 编译提示。
- 静态检查：两个 Job Mapper XML 通过 `xmllint --noout`；V1.0.42 无 Flyway `${...}` 占位符；Job Service 无 `LambdaQueryWrapper/lambdaQuery`；冲突标记扫描和 `git diff --check` 通过。
- UI 验收：按用户要求未执行浏览器、截图和 Playwright 验证，由用户自行验收。
- 环境清理：本轮未启动 MySQL、Redis、Admin、Vite 或浏览器服务，无新增服务 PID 需要清理。
