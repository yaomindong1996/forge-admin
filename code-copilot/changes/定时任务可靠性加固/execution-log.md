# 定时任务可靠性加固 Execution Log

## 2026-07-19 Apply 启动

- 用户确认按路线图顺序开始开发，首轮只实施 V1。
- 分支：codex/job-scheduler-reliability。
- 范围：平台超级管理员控制面；不增加 owner_org_id、普通租户自助、一次性任务或独立时区。
- 已读取：项目记忆、Forge 编码规范、自动化测试标准、V1 spec.md/tasks.md。
- 当前最大 Flyway 版本：V1.0.39；V1 迁移计划使用 V1.0.40。
- 工作区已有无关改动：.DS_Store、forge/.DS_Store、forge-server/db/全量初始化SQL.sql；本变更不修改、不提交这些文件。
- 服务启动：无。

## 2026-07-19 Task 1 最小数据库迁移

- 变更范围：新增 `V1.0.40__harden_job_scheduler_reliability.sql`、迁移契约测试，并为 job 插件补充测试依赖。
- 首次执行未带 `-Penable-tests` 的模块测试时，Maven 构建成功但根 POM 默认 `forge.tests.skip=true`，测试实际跳过；后续测试命令统一显式启用 `enable-tests` Profile。
- 红灯验证：
  - 命令：`JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -Penable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-job -am -Dtest=JobReliabilityMigrationContractTest -Dsurefire.failIfNoSpecifiedTests=false test`
  - 结果：迁移文件创建前 `Tests run: 7, Errors: 7`，失败原因均为缺少 `V1.0.40__harden_job_scheduler_reliability.sql`，符合先失败预期。
- 实现后首次复跑：`Tests run: 7, Failures: 1`；根因是契约测试未考虑动态 SQL 字符串中的单引号转义，修正断言后再次执行。
- 绿灯验证：使用同一 Maven 命令，`Tests run: 7, Failures: 0, Errors: 0, Skipped: 0`，Reactor `BUILD SUCCESS`。
- Flyway placeholder 扫描：`rg -n '\$\{[^}]+\}' forge-server/db/migration/V1.0.40__harden_job_scheduler_reliability.sql`，无输出。
- 格式检查：`git diff --check -- forge-server/db/migration/V1.0.40__harden_job_scheduler_reliability.sql forge-server/forge-framework/forge-plugin-parent/forge-plugin-job/pom.xml forge-server/forge-framework/forge-plugin-parent/forge-plugin-job/src/test/java/com/mdframe/forge/plugin/job/migration/JobReliabilityMigrationContractTest.java`，通过。
- 数据库实跑：本阶段未执行，遵循现有约定由用户在真实开发库验证 Flyway 和 `forge_schema_history`；不将静态契约测试表述为数据库迁移已实际执行。
- 服务启动：无。

## 2026-07-19 Task 2 DTO、VO、校验和 Mapper XML

- 红灯验证：新增 `JobConfigValidatorTest` 和 `JobMapperXmlContractTest` 后执行定向测试，`testCompile` 因 `JobConfigSaveRequest`、`JobConfigValidator` 尚不存在而失败，符合先失败预期。
- 实现范围：新增任务查询/保存 DTO、日志查询 DTO、任务 VO 和权威校验器；实体映射同步状态、版本、日志关联与触发类型；任务分页/详情/恢复候选/JobKey 查询及日志分页迁移到 Mapper XML。
- 校验范围：Cron 表达式、JSON 参数、BEAN/HANDLER/RPC 执行目标、0/1 状态、非负重试次数、字段长度和更新时 JobKey 不可编辑。
- 首次实现后定向测试：XML 契约 3/3 通过；校验测试因本机 Mockito inline mock-maker 无法附加 JVM 而报错。测试随后改用真实轻量 `GenericApplicationContext`，移除 Mockito 运行时依赖。
- 定向绿灯：`mvn -Penable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-job -am -Dtest=JobConfigValidatorTest,JobMapperXmlContractTest -Dsurefire.failIfNoSpecifiedTests=false test`，`Tests run: 9, Failures: 0, Errors: 0, Skipped: 0`。
- 模块回归：`mvn -Penable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-job -am -Dsurefire.failIfNoSpecifiedTests=false test`，job 模块 `Tests run: 16, Failures: 0, Errors: 0, Skipped: 0`，Reactor `BUILD SUCCESS`。
- XML 检查：`xmllint --noout` 校验 `SysJobConfigMapper.xml` 和 `SysJobLogMapper.xml`，通过。
- 查询规范扫描：`rg -n 'LambdaQueryWrapper' .../service` 无输出。
- 格式检查：`git diff --check -- forge-server/forge-framework/forge-plugin-parent/forge-plugin-job`，通过。
- 服务启动：无。

## 2026-07-19 Task 3 Quartz 异常和幂等同步

- 红灯验证：新增 `JobSchedulerTest`、`JobScheduleCoordinatorTest` 后定向测试因 `JobScheduleException` 和 `JobScheduleCoordinator` 尚不存在而 `testCompile` 失败，符合先失败预期。
- 调度核心：`JobScheduler` 的新增、更新、删除、暂停、恢复、触发、Cron 更新和存在性检查不再捕获异常后返回 `false`；统一抛出 `JobScheduleException`。
- 幂等同步：新增 `JobScheduleCoordinator`，按 JobKey 对 Quartz 执行新增或替换，并根据数据库状态暂停/恢复；成功写 `SYNCED`，普通失败写 `FAILED`，删除失败保留 `DELETE_PENDING` 并记录错误。
- 写链路：Service 先落数据库期望状态，再调用协调器，不使用跨数据库/Quartz 的伪原子本地事务；因此 Quartz 失败时数据库记录和同步错误可以保留，异常同时对调用方可见。
- 重试：`retrySynchronization` 会清理普通失败错误并重新同步；删除重试保持 `DELETE_PENDING` 意图，避免误恢复为普通任务。
- 定向验证：`mvn -Penable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-job -am -Dtest=JobSchedulerTest,JobScheduleCoordinatorTest -Dsurefire.failIfNoSpecifiedTests=false test`，`Tests run: 5, Failures: 0, Errors: 0, Skipped: 0`。
- 模块回归：`mvn -Penable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-job -am -Dsurefire.failIfNoSpecifiedTests=false test`，job 模块 `Tests run: 21, Failures: 0, Errors: 0, Skipped: 0`，Reactor `BUILD SUCCESS`。
- 静默失败扫描：`JobScheduler.java` 中无调度操作 `return false` 或 boolean CRUD 签名。
- XML/格式检查：Mapper XML 通过 `xmllint --noout`，job 插件目录通过 `git diff --check`。
- 服务启动：无。

## 2026-07-19 Task 4 统一启动恢复和注解登记

- 实现范围：`JobAutoRegistrar` 只按 `job_name + job_group` 登记数据库期望配置，不覆盖既有配置、不直接操作 Quartz；新增 `JobStartupReconciler` 作为唯一 `ApplicationRunner` 恢复入口；删除旧 `JobConfigLoader`。
- 启动恢复：统一调用 `JobScheduleCoordinator.reconcileOnStartup()`，按数据库期望状态补建或更新 Quartz 任务，禁用任务保持暂停，不扫描或删除数据库之外的 Quartz Job。
- 定向验证：`mvn -Penable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-job -am -Dtest=JobAutoRegistrarTest,JobStartupRecoveryContractTest,JobScheduleCoordinatorTest -Dsurefire.failIfNoSpecifiedTests=false test`，`Tests run: 8, Failures: 0, Errors: 0, Skipped: 0`，Reactor `BUILD SUCCESS`。
- 模块回归：`mvn -Penable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-job -am -Dsurefire.failIfNoSpecifiedTests=false test`，job 模块 `Tests run: 26, Failures: 0, Errors: 0, Skipped: 0`，Reactor `BUILD SUCCESS`。
- 日志说明：故障分支测试会有预期的 Quartz `Scheduler has been shutdown` 错误栈，但断言通过，不是构建失败。
- XML/格式检查：`SysJobConfigMapper.xml`、`SysJobLogMapper.xml` 通过 `xmllint --noout`；job 插件与 starter-job 目录通过 `git diff --check`。
- 服务启动：无。

## 2026-07-19 Task 5 日志筛选和安全裁剪

- 红灯验证：新增 `JobLogSanitizerTest`、`JobExecutionLogContractTest` 后执行定向测试，`testCompile` 因 `JobLogSanitizer` 尚不存在而失败，符合先失败预期。
- 安全处理：新增结构化 JSON 递归脱敏和文本兜底规则，覆盖 Authorization/Bearer、Token、密码、Secret、Cookie、手机号、身份证号等；任务参数和执行结果上限 2000 字符，异常上限 4000 字符，截断时保留明确标记。
- 日志链路：`JobMonitor` 写入前脱敏；日志分页和详情读取时再次清洗，历史记录也不会直接返回原始敏感值；`QuartzJobExecutor` 不再向应用日志输出原始结果或异常内容。
- 关联与来源：Quartz JobDataMap 携带 `jobConfigId`，计划触发记录 `SCHEDULED`，立即运行覆盖为 `MANUAL`，最终写入 `sys_job_log.job_config_id/trigger_type`。
- 时间筛选：Mapper XML 明确使用 `trigger_time >= startTime` 和 `trigger_time <= endTime`；前端改为发送本地 `yyyy-MM-dd HH:mm:ss`，避免原 `toISOString()` 的 UTC 偏移和后端格式不匹配。
- 定向绿灯：`mvn -Penable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-job -am -Dtest=JobLogSanitizerTest,JobExecutionLogContractTest,JobMapperXmlContractTest,JobSchedulerTest -Dsurefire.failIfNoSpecifiedTests=false test`，`Tests run: 11, Failures: 0, Errors: 0, Skipped: 0`。
- 模块回归：`mvn -Penable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-job -am -Dsurefire.failIfNoSpecifiedTests=false test`，job 模块 `Tests run: 33, Failures: 0, Errors: 0, Skipped: 0`，Reactor `BUILD SUCCESS`。
- 前端定向检查：Node `v20.19.0` 下执行 `pnpm exec eslint src/views/system/job-log-list.vue`，首次发现一处 `prefer-template`，修正后复跑无输出、退出码 0。
- XML/格式检查：`SysJobLogMapper.xml` 通过 `xmllint --noout`；job 插件和日志页面通过 `git diff --check`。
- 服务启动：无。

## 2026-07-19 Task 6 Controller 和前端同步状态

- 后端接口：新增 `POST /job/config/{id}/sync`，Controller 调用 `retrySynchronization`；保存已落库但 Quartz 同步失败时返回“任务配置已保存，但调度同步失败，请在列表中点击‘重新同步’”，避免用户误以为配置完全失败或完全成功。
- 列表信息架构：默认展示任务、执行内容、执行计划、运行状态、调度同步和操作，不再把 Bean、方法、Handler 等技术字段平铺为主列；常见 Cron 显示自然语言摘要，同步失败显示短错误原因。
- 操作可理解性：仅 `FAILED`、`DELETE_PENDING` 提供“重新同步”；编辑时任务名称和分组禁用；立即运行增加“不改变当前启停状态”的确认；启停、删除、重新同步及同步失败保存后刷新列表，避免乐观版本和同步状态停留在旧值。
- 范围控制：日志清理移入“日志维护”菜单；移除当前未真正生效的重试、告警和 Webhook 可见配置；继续使用 V1 弹窗，不提前实施 V2 全屏配置工作台。
- 红灯验证：新增 `JobSyncApiContractTest` 后，接口契约因缺少 `JobConfigController.sync(Long)` 报 `NoSuchMethodException`，UI 契约断言同步入口失败，符合先失败预期。
- 定向绿灯：实现后执行 `JobSyncApiContractTest`，接口和 UI 两项契约 `2/2` 通过。
- job 模块回归：`mvn -Penable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-job -am -Dsurefire.failIfNoSpecifiedTests=false test`，job 模块 `Tests run: 35, Failures: 0, Errors: 0, Skipped: 0`，Reactor `BUILD SUCCESS`。
- 前端定向检查：Node `v20.19.0` 下执行 `pnpm exec eslint src/views/system/job-config.vue src/views/system/job-log-list.vue`，无输出、退出码 0。
- 前端生产构建：Node `v20.19.0` 下执行 `NODE_OPTIONS=--max-old-space-size=8192 pnpm build`，`8699 modules transformed`，`built in 1m 28s`，退出码 0。
- 构建警告：存在仓库既有的动态/静态导入混用、CSS `//` 注释、组件命名冲突和大包体提示；未发现指向本次定时任务页面的编译错误，不阻断本阶段。
- UI 联调：尚未连接真实 Admin、MySQL、Redis 或 Quartz 数据，生产构建通过不等于真实数据交互已验收；Task 7 尝试浏览器静态链路后仍会保留此边界。
- 服务启动：无。

## 2026-07-19 Task 7 最终回归验证

- 收尾审查修正：同步失败或待删除任务原本仍可“运行一次”，可能触发 Quartz 中的旧配置。后端 `triggerJob` 现要求 `sync_status=SYNCED`，否则提示“任务尚未同步到调度服务，请先重新同步”；前端仅对已同步任务展示“运行一次”。
- 操作优先级修正：“重新同步”移到失败任务首要操作；`DELETE_PENDING` 任务只保留“重新同步”和“查看日志”，避免编辑、启停、再次删除或触发执行与删除意图冲突。
- 门禁定向测试：`SysJobConfigServiceImplTest,JobSyncApiContractTest` 共 `4/4` 通过；前端定向 ESLint 和相关 `git diff --check` 同时通过。
- job 模块最终回归：`mvn -Penable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-job -am -Dsurefire.failIfNoSpecifiedTests=false test`，`Tests run: 37, Failures: 0, Errors: 0, Skipped: 0`，Reactor `BUILD SUCCESS`，总耗时约 14 秒。
- Admin 聚合构建：`mvn -pl forge-admin-server -am package -DskipTests`，42 个 Reactor 模块全部 `SUCCESS`，最终 `BUILD SUCCESS`，总耗时约 46 秒。
- 前端最终检查：Node `v20.19.0` 下定向 ESLint 无输出、退出码 0；`pnpm build` 完成 `8699 modules transformed`，`built in 1m 57s`，退出码 0。
- 静态检查：迁移脚本 Flyway placeholder 扫描无输出；`SysJobConfigMapper.xml`、`SysJobLogMapper.xml` 经 `xmllint --noout` 通过；Service 未发现 `LambdaQueryWrapper`；本变更范围 `git diff --check` 通过。
- 已知非阻断警告：Quartz 故障测试打印预期的 `Scheduler has been shutdown` 堆栈；Maven 存在仓库既有废弃 API/未检查操作提示；Vite 存在组件命名冲突、动态/静态导入、CSS `//` 注释和包体提示。
- 浏览器验证尝试：已先执行 `with_server.py --help`。Vite dev 因本机文件监听达到上限报 `EMFILE: too many open files, watch`；改用生产构建 preview 后，本地端口绑定被沙箱以 `listen EPERM` 拒绝。辅助脚本均已停止本轮进程，没有遗留服务。
- 未覆盖边界：未启动真实 Admin、MySQL、Redis 或 Quartz 服务，未实跑 Flyway，未完成真实数据和权限下的 UI 点击验收；这些结论不得表述为已通过。
- 服务启动：无成功启动的服务；失败尝试均已自动或手动停止。
