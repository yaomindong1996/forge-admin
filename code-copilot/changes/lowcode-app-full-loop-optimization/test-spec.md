# 测试计划：lowcode-app-full-loop-optimization

> created: 2026-06-02
> scope: Phase 9 增量验证，复用 `spec.md`、`tasks.md`、`execution-log.md` 的既有验证基线。

## 1. 既有验证基线

- Phase 8 已完成后端主应用 package、前端 build、Flyway/dev 库、商机流程发起和核心接口验证。
- Phase 9 Task 23-25 已完成插件模块编译、前端 build 和 `git diff --check`。
- Phase 9 Task 26-29 已完成插件模块编译、前端 build 和 `git diff --check`。

## 2. 本轮增量范围：Task 30-33

- Task 30：流程绑定面板与触发器 `START_FLOW` 动作复用变量映射编辑器和标题模板编辑器。
- Task 31：单据运行态返回自动“发起流程”按钮，`AiCrudPage` 渲染并调用 `/ai/business/flow/start`。
- Task 32：触发器新增前置业务对象选择、对象上下文 query 默认值、保存前阻断空对象。
- Task 33：对象设计器单据闭环步骤条、发布检查和 readiness 增加入口/流程/触发器缺口项。

## 3. 必跑验证

| 优先级 | 验证项 | 命令 | 期望 |
|--------|--------|------|------|
| P0 | 后端插件编译 | `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests` | `BUILD SUCCESS` |
| P0 | 前端生产构建 | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm build` | 构建通过，无新增阻断错误 |
| P0 | 补丁空白检查 | `git diff --check` | 无 trailing whitespace / conflict marker |

## 4. 条件增强验证

- 如本地后端、Flow 服务和 dev 数据库已可用，补充验证：
  - `GET /ai/business/document/{objectCode}/{recordId}/runtime` 返回 `runtimeActions`。
  - `POST /ai/business/flow/start` 可由运行态按钮 payload `{objectCode, recordId}` 发起。
  - `GET /ai/business/flow/model/{modelKey}/variables?objectCode=...` 返回候选变量和映射建议。
  - `GET /ai/business/trigger/page?pageNum=1&pageSize=10&objectCode=...` 可按对象进入触发器页。
- 如前端 dev server 可用，补充浏览器验证：
  - 主流程配置页变量下拉、推荐映射和标题模板预览。
  - 触发器新增未选对象时保存按钮阻断。
  - 对象设计器步骤条能跳转到触发器配置。

## 5. 本轮跳过标准

- 未启动后端、Flow 服务、前端 dev server 或数据库时，接口和浏览器点击验证记录为跳过，不写作通过。
- 前端构建中的既有 UnoCSS 图标加载、CSS `//` 注释、动态/静态导入混用和 chunk size 提示，如未新增阻断，记录为非阻断警告。

## 6. Phase 10 Task 34 增量验证

| 优先级 | 验证项 | 命令 | 期望 |
|--------|--------|------|------|
| P0 | 后端插件编译 | `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator,forge-framework/forge-plugin-parent/forge-plugin-job -am compile -DskipTests` | `BUILD SUCCESS` |
| P0 | 前端生产构建 | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm build` | 构建通过，无新增阻断错误 |
| P0 | 补丁空白检查 | `git diff --check` | 无 trailing whitespace / conflict marker |
| P1 | 任务中心代码审查 | 检查 `BusinessTriggerSchedulerService`、`ScheduleConfig` | 扫描入口使用 `@ScheduledJob` 注册到任务中心，Quartz 显式开启集群配置，Redisson 锁作为兜底 |

条件增强验证：

- 后端和数据库可用时，新增或启用 `triggerType=SCHEDULE`、`eventType=SCHEDULED_DUE`、`eventCondition.schedule.dueField` 的触发器，确认扫描器按到期字段小批量读取记录。
- 后端启动后检查 `sys_job_config`：存在 `job_name=lowcodeBusinessTriggerScanJob`、`job_group=LOWCODE`、`cron_expression=0 0/5 * * * ?`。
- 检查 `ai_business_trigger_log`：同一 `trigger_id + record_id + SCHEDULED_DUE` 当天只出现一次 `SUCCESS/TODO` 结果。
- 验证未配置 `dueField` 的定时触发器只输出跳过日志，不读取业务表。
- 多实例环境可用时，同时启动两个 admin 实例，确认 Quartz JDBC 集群只触发一个节点执行 `LOWCODE.lowcodeBusinessTriggerScanJob`。
- Redis/Redisson 可用时，确认手动并发触发时只有一个执行方抢到 `forge:business-trigger:schedule:scan`；Redis/Redisson 不可用时，确认日志说明仅依赖任务中心集群调度和日志去重。

## 7. Phase 10 BUG 修正增量验证

| 优先级 | 验证项 | 命令 | 期望 |
|--------|--------|------|------|
| P0 | 补丁空白检查 | `git diff --check` | 无 trailing whitespace / conflict marker |
| P0 | 后端插件编译 | `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator,forge-framework/forge-plugin-parent/forge-plugin-job -am compile -DskipTests` | `BUILD SUCCESS` |
| P0 | 前端生产构建 | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build` | 构建通过，无新增阻断错误 |

本轮聚焦编号规则格式、应用入口文案、单据填报 form-only、发布 loading、发起主流程按钮和触发器动作配置简化。未启动后端、Flow 服务、前端 dev server 或数据库时，接口联调和浏览器点击验证记录为跳过。

## 8. Phase 10 BUG 用户反馈跟进验证

| 优先级 | 验证项 | 命令 | 期望 |
|--------|--------|------|------|
| P0 | 补丁空白检查 | `git diff --check` | 无 trailing whitespace / conflict marker |
| P0 | 后端插件编译 | `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator,forge-framework/forge-plugin-parent/forge-plugin-job -am compile -DskipTests` | `BUILD SUCCESS` |
| P0 | 前端生产构建 | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build` | 构建通过，无新增阻断错误 |

本轮聚焦对象类型与入口打开方式关系说明、父级菜单回填、自定义操作发布到运行态列表、单据设置说明文案和操作列兼容识别。未启动后端、Flow 服务、前端 dev server 或数据库时，接口联调和浏览器点击验证记录为跳过。

## 9. Phase 10 BUG 非单据对象发起主流程验证

| 优先级 | 验证项 | 命令 | 期望 |
|--------|--------|------|------|
| P0 | 补丁空白检查 | `git diff --check` | 无 trailing whitespace / conflict marker |
| P0 | 后端插件编译 | `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests` | `BUILD SUCCESS` |

条件增强验证：

- 对未启用单据模式但已发布运行配置、已配置主流程绑定的对象，调用 `POST /ai/business/flow/start`，期望不再返回“业务对象未启用单据模式”，并写入流程实例关联。
- 对启用单据模式的对象，仍验证状态字段、状态映射、权限和重复运行流程的限制。
- 对非单据对象流程回调，期望更新流程实例状态；如存在运行配置，继续发布流程结果业务事件。

## 10. Phase 10 BUG 发布检查与回显一致性验证

| 优先级 | 验证项 | 命令 | 期望 |
|--------|--------|------|------|
| P0 | 补丁空白检查 | `git diff --check` | 无 trailing whitespace / conflict marker |
| P0 | 后端插件编译 | `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests` | `BUILD SUCCESS` |
| P0 | 前端生产构建 | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build` | 构建通过，无新增阻断错误 |

本轮聚焦应用入口发布检查误报、菜单父级回显、菜单资源 ID 精度、主流程绑定读取优先级和旧审批兼容运行态。未启动后端、Flow 服务、前端 dev server 或数据库时，菜单同步落库、发布检查接口和浏览器回显验证记录为跳过。
