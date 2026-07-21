# 定时任务运维日志监控执行记录
> status: complete

## 2026-07-20 基线

- 用户要求继续下个阶段，V5 按 `定时任务运维日志监控` 进入 apply。
- 依赖 V1 和 V4 已完成；V4 基线为 Job 84/84、前端 13/13、Admin Reactor 42/42、前端生产构建和静态检查通过。
- 当前最大 Flyway 版本为 V1.0.43，V5 迁移使用 V1.0.44。
- 范围确认：只建设运维可观测性，不发送告警，不改权限模型，不修改 MySQL 分区。
- 默认不启动真实 MySQL、Redis、Admin 或 Quartz 集群；真实 Flyway 和接口数据由开发环境验收。
- V1-V4 未提交改动与 V5 共用 Job 核心文件，本轮不自动创建边界不清的提交。

## 2026-07-20 实现与自动化验证

### 变更范围

- 增加连续失败计数、日志查询索引、触发来源字典和安全导出配置。
- 增加日志列表/详情/导出分层协议、任务概览和近 24 小时监控聚合。
- 重构日志筛选、详情和导出交互，在任务列表增加全局摘要及单任务执行概览。
- 日志留存物理清理排除运行中记录，保持超级管理员边界，不进入告警、权限、Webhook 和分区范围。

### 后端验证

- Java 17 环境：`JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home`。默认 JDK 首次执行出现“无效的目标发行版 17”，固定 Java 17 后通过。
- V5 定向执行 `JobObservabilityMigrationContractTest`、`JobObservabilityApiContractTest`、`JobObservabilityManagerTest`、`SysJobLogServiceImplTest`、`JobExecutionLifecycleServiceTest` 和 `JobMapperXmlContractTest`：15/15 通过。
- `mvn -Penable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-job -am test`：94/94 通过。
- `mvn -pl forge-admin-server -am package -DskipTests`：Admin Reactor 42/42，构建通过。
- `xmllint --noout forge-framework/forge-plugin-parent/forge-plugin-job/src/main/resources/mapper/SysJobConfigMapper.xml forge-framework/forge-plugin-parent/forge-plugin-job/src/main/resources/mapper/SysJobLogMapper.xml`：通过。
- `rg -n '\$\{[^}]+\}' db/migration`：无输出，Flyway 脚本不存在未处理 placeholder。

### 前端验证

- Node.js 固定为 v20.19.0。
- `pnpm exec vitest run src/views/system/job-config/__tests__/job-config-form.test.js src/views/system/__tests__/job-log-query.test.js`：16/16 通过。
- 对 V5 相关 Vue/JS 文件执行定向 ESLint：通过。
- `NODE_OPTIONS=--max-old-space-size=8192 pnpm build`：生产构建通过，耗时约 1 分 29 秒。
- 构建保留既有非阻断警告：`UserSelectModal` 组件命名冲突、部分动态/静态导入重叠、既有 CSS `//` 注释；后端保留既有 deprecated/unchecked 编译提示。

### 浏览器与静态收口

- 开发服务器曾因 `EMFILE: too many open files, watch` 无法启动；改用已构建产物执行 `vite preview --host 127.0.0.1 --port 4173`。
- `curl -I http://127.0.0.1:4173/forge`：返回 200。
- 桌面 1440x900 和窄屏 390x844 检查均加载到应用启动壳，未记录 console error、page error 或失败的静态资源请求。
- 由于本轮按边界未启动 8580 后端，应用停在初始化加载态，未进入定时任务列表；已登录任务页、真实 API、Flyway 和数据库聚合结果留待开发环境验收。
- `rg -n '^(<<<<<<<|=======|>>>>>>>)'`：无输出。
- `git diff --check`：通过。
- 本轮仅启动前端生产预览，记录完成后停止；未启动 MySQL、Redis、Admin 或 Quartz。

## 2026-07-21 V5.1 运行日志界面优化

### 变更范围

- 保持日志 API、权限、导出和状态协议不变，重新组织 `job-log-list.vue` 的概览、筛选、表格与详情层级；外层日志弹窗从 1400px 收敛到 1120px，并移除标题中的重复任务名。
- 筛选与执行记录合并为单一工作区；单任务上下文隐藏重复任务列，状态和来源组合展示，执行器与结束时间不再占用主列表。
- 有详情权限的记录支持点击和 Enter/Space 键打开右侧抽屉；抽屉按运行概览、时间线、安全摘要和折叠技术信息分区。
- 新增 `resolveJobLogStartedAt`，列表开始时间优先使用 `startTime`，缺失时依次回退 `triggerTime` 和 `scheduledFireTime`。

### 验证证据

- TDD 首次执行 `pnpm exec vitest run src/views/system/__tests__/job-log-query.test.js`：`1 failed / 4 passed`，失败原因为 `resolveJobLogStartedAt is not a function`。
- 实现后最终执行同一目标测试：`1/1` 文件、`5/5` 测试通过。
- Node `v20.19.0` 下执行目标 ESLint：`job-config.vue`、`job-log-list.vue`、`job-log-query.js`、`job-log-query.test.js` 无输出、退出码 0。
- 最终执行 `NODE_OPTIONS=--max-old-space-size=8192 pnpm build`：8725 个模块完成转换，`job-log-list` 和 `job-config` 产物生成，构建用时约 1 分 36 秒。
- 构建保留仓库既有 `UserSelectModal` 命名冲突、动态/静态导入、CSS `//` 注释和大包体提示，均不阻断本轮结果。
- `git diff --check` 通过；目标源码与变更文档尾随空白扫描无输出。

### 浏览器与环境边界

- 按 `webapp-testing` 流程先确认本机已有 5173/8580 监听，再尝试使用 Python Playwright 启动无头 Chromium。
- Chromium 因当前沙箱拒绝注册 macOS MachPort 而退出，本地监听端口也无法从沙箱建立连接，因此没有产出可信桌面/窄屏业务页截图。
- 本轮未启动或停止 Admin、MySQL、Redis、Quartz、Vite 及用户已有进程；真实数据下的桌面和窄屏视觉效果由现有开发环境刷新后验收。
