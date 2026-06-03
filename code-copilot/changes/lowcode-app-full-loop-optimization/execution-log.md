# Phase 8 执行日志：lowcode-app-full-loop-optimization

> 执行时间：2026-06-02 08:00 CST
> 范围：Phase 8 验证归档，覆盖后端构建、前端构建、Flyway、核心接口、商机主流程和文档回填。

## 1. 代码与迁移补丁

- 修复 `V1.0.51__seed_crm_opportunity_document_flow.sql`、`V1.0.52__seed_leave_document_flow_demo.sql` 中内嵌模板的 Flyway 占位符冲突，将 seed JSON 模板从 `${field}` 改为 `{field}`。
- `BusinessFlowService`、`BusinessTriggerExecutor`、`MessageTemplateEngine` 已兼容 `{field}` 与历史 `${field}` 两种模板变量格式。
- 新增 `V1.0.53__align_flow_template_logic_delete_column.sql`，为 `sys_flow_template` 补齐 `del_flag` 逻辑删除字段，匹配 `FlowTemplate` 实体。
- 新增 `V1.0.54__patch_opportunity_flow_dept_manager_mapping.sql`，为 CRM 商机默认流程绑定和自动发起流程触发器补齐 `createBy -> deptManager` 变量映射。

## 2. Flyway 与流程部署

- dev 库 `forge_schema_history` 已执行到 `1.0.54`，最新脚本 `patch opportunity flow dept manager mapping` 执行成功。
- dev 库原有 `sys_flow_template(template_key=leave_multi)`，但缺少对应 Flowable 模型和已部署流程定义；Phase 8 已通过流程服务创建并部署模型。
- `leave_multi` 模型 ID：`e7d55f0a4087ec5dc0189784a00204ad`。
- `leave_multi` 部署 ID：`dcbda981-5e14-11f1-a0fd-d67ed5f8e875`。
- `act_re_procdef` 已存在 `leave_multi` version `1`。

## 3. 商机主流程验证

- 调用 `/ai/business/flow/start`，请求 `{"objectCode":"OPPORTUNITY","recordId":10}`，返回 `code=200`。
- 流程实例 ID：`602ad5c2-5e15-11f1-a0fd-d67ed5f8e875`。
- 单据流程关联 ID：`2061597550728736769`。
- `crm_opportunity.id=10` 的 `document_status` 已回写为 `IN_PROCESS`。
- `ai_business_flow_instance_link` 已写入 `OPPORTUNITY:10`，流程状态 `RUNNING`。
- `sys_flow_business` 已写入 `OPPORTUNITY:10`，状态 `running`。
- `sys_flow_task` 已生成 `部门经理审批` 待办，任务 ID `602b2410-5e15-11f1-a0fd-d67ed5f8e875`，assignee `1`。

## 4. 接口验证

以下接口均通过 `X-Inner-Call: true` 验证，返回 `code=200`：

- `/ai/business/document/config/1910000000000000104`：单据配置已启用，默认流程 `leave_multi`。
- `/ai/business/document/OPPORTUNITY/10/runtime`：返回 `documentStatus=IN_PROCESS`、`flowStatus=RUNNING`，可用动作包含 `START_FLOW`、`VIEW_FLOW`。
- `/ai/business/flow/status/OPPORTUNITY/10`：返回 `RUNNING`。
- `/ai/business/trigger/page?pageNum=1&pageSize=10`：返回总数 `5`。
- `/ai/business/trigger/scenario-templates`：返回模板数 `5`。
- `/ai/business/stats/crm_opportunity/metrics`：返回指标 `TOTAL`、`TODAY`、`MONTH`。

## 5. 构建验证

- `git diff --check`：通过。
- 后端：`mvn -pl forge-admin-server -am package -DskipTests`：`BUILD SUCCESS`。
- 前端：`source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm build`：构建通过。
- 前端构建仅保留既有警告：UnoCSS 图标加载、CSS `//` 注释、动态/静态导入混用和 chunk size 提示。

## 6. 结论

Phase 8 验证通过。数据库迁移、流程定义部署、商机单据手动发起流程、运行态状态查询、触发器模板、触发器分页和业务指标接口均已完成最小闭环验证。

---

# Phase 9 增量执行日志：Task 23-25

> 执行时间：2026-06-02 20:34 CST
> 范围：动态应用菜单归属、open-info 协议、菜单选中态、应用入口运行态打开模式和填报入口。

## 1. 本轮改动

- 后端 `BusinessAppOpenInfoVO` 增加 `targetRoute`、`runtimeOpenMode`、`menuResourceId`、`activeMenuKey`。
- 后端 `BusinessAppDTO`、`BusinessAppVO` 增加 `runtimeOpenMode`，VO 额外回显 `menuResourceId`。
- `BusinessAppService` 复用 `ai_business_app.options` 保存 `runtimeOpenMode`，RUNTIME 应用菜单同步到最终运行页路径，并保留原菜单资源 ID。
- `BusinessAppOpenService` 生成带菜单归属和打开模式 query 的 `targetRoute`。
- 前端菜单 activeKey 优先读取 `menuKey/menuResourceId/appId`，动态运行页 Tab/浏览器标题支持按菜单 key 或 query title 恢复。
- 应用入口抽屉增加 `LIST`、`CREATE_FORM`、`DETAIL` 运行态打开模式。
- 运行页支持 `mode=create` 自动打开新增表单，新增保存成功后移除 `mode=create` 回到列表。

## 2. 验证命令

- `git diff --check`：通过。
- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`：`BUILD SUCCESS`。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm build`：构建通过。

## 3. 警告和跳过项

- Maven 保留既有 deprecation / unchecked 编译提示，未阻断。
- 前端构建保留既有 UnoCSS 图标加载失败、CSS `//` 注释、动态/静态导入混用和 chunk size 提示，未阻断。
- 本轮未启动后端或前端服务，未执行浏览器点击验证；原因是本轮先完成 Task 23-25 代码与构建验证，未要求联调现有 dev 数据菜单。

## 4. 结论

Task 23-25 增量验证通过。动态应用入口协议、菜单选中态恢复和填报入口打开模式已完成代码闭环。

---

# Phase 9 增量执行日志：Task 26-29

> 执行时间：2026-06-02 21:05 CST
> 范围：单据编号规则、状态映射、单据设置面板、主流程合并兼容、流程变量候选项和自动映射建议。

## 1. 本轮改动

- 后端新增单据编号变量接口和编号预览接口：`/ai/business/document/no-rule/tokens`、`/ai/business/document/no-rule/preview`。
- 单据配置协议扩展 `noRuleTemplate`、`statusMappingRows`、`statusActionPolicy`、`noRulePreview`、`mainFlowSummary`，并保持旧 `statusMapping` 兼容。
- 单据配置保存时校验编号未知变量，结构化状态映射写入 `options.statusMappingRows`，运行态标准状态映射继续写入 `status_mapping`。
- 主流程事实来源合并为 `ai_business_binding(binding_type=FLOW)`；历史 `default_flow_key` 只做兼容读取和保存后快照回写。
- 发布检查改为读取主流程摘要，提示未配置主流程、变量映射缺失等具体缺口。
- 新增流程变量解析器，解析内置变量、BPMN 表达式/审批人属性、流程动态表单字段和业务对象字段，并给出映射建议。
- 前端单据设置面板重构为基础配置、编号规则编辑器、状态映射表和发布摘要；移除独立默认流程下拉。
- 流程绑定面板接入变量候选项，下拉选择流程变量，并支持只补空项的推荐映射应用。

## 2. 验证命令

- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`：`BUILD SUCCESS`。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm build`：构建通过。
- `git diff --check`：通过。

## 3. 警告和跳过项

- Maven 保留既有 deprecation / unchecked 编译提示，未阻断。
- 前端构建保留既有 UnoCSS 图标加载失败、CSS `//` 注释、动态/静态导入混用和 chunk size 提示，未阻断。
- 本轮未新增 Flyway：未改表结构，历史主流程通过读取兼容和保存回写渐进迁移。
- 本轮未启动后端、流程服务或前端 dev server，未执行浏览器点击和真实接口联调；原因是本轮先完成 Task 26-29 代码与构建验证，未要求连接现有 dev 数据逐项试跑。

## 4. 结论

Task 26-29 增量验证通过。编号规则、结构化状态映射、单据设置面板、主流程合并兼容和流程变量候选项已完成代码闭环。

---

# Phase 9 增量执行日志：Task 30-33

> 执行时间：2026-06-02 21:44 CST
> 范围：流程绑定面板和标题模板体验、运行态自动发起流程按钮、触发器前置对象、单据闭环步骤条、发布检查和验证回填。

## 1. 本轮改动

- 新增 `TemplateVariableEditor.vue` 和 `FlowVariableMappingEditor.vue`，主流程配置和触发器 `START_FLOW` 动作复用同一套变量插入、标题预览、变量候选和推荐映射体验。
- 单据运行态 `BusinessDocumentRuntimeVO` 返回 `runtimeActions`，后端按主流程配置、发起方式、状态、进行中流程和权限生成“发起流程”按钮及禁用原因。
- `crud-page.vue` 为运行页当前页记录加载单据运行态，`AiCrudPage.vue` 合并自动行操作并执行 `START_FLOW`，成功后刷新列表。
- 触发器页面支持 `objectCode` query 上下文，新增触发器先选业务对象，切换对象后清理旧字段条件和动作配置。
- 对象设计器新增单据闭环步骤条，单据保存和主流程保存后能引导进入下一步；触发器配置从对象设计器携带当前对象。
- 发布检查和 readiness 增加应用入口、菜单资源、运行打开模式、编号规则、状态映射、主流程完整度、自动按钮和触发器缺口提示。
- 修复单据已启用但主流程摘要为空时，运行态自动按钮和发布检查可能出现空指针的边界。

## 2. 验证命令

- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`：`BUILD SUCCESS`。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm build`：构建通过。
- `git diff --check`：通过。

## 3. 警告和跳过项

- Maven 保留既有 deprecation / unchecked 编译提示，未阻断。
- 前端构建保留既有 UnoCSS 图标加载失败、CSS `//` 注释、动态/静态导入混用和 chunk size 提示，未阻断。
- 本轮未启动后端、Flow 服务、前端 dev server 或数据库，未执行接口联调和浏览器点击验证；原因是本轮按增量范围完成代码编译和前端生产构建验证，未要求连接现有 dev 数据逐项试跑。
- 本轮未启动任何服务，无需停止服务。

## 4. 结论

Task 30-33 增量验证通过。流程配置体验、运行态自动发起流程按钮、触发器对象前置和单据闭环步骤条已完成代码闭环，并已补齐本轮 `test-spec.md`、`tasks.md`、`spec.md` 和执行日志。

---

# Phase 10 增量执行日志：Task 34

> 执行时间：2026-06-02 22:55 CST
> 范围：定时触发扫描器、到期提醒配置、集群部署防重复和验证回填。

## 1. 本轮改动

- 新增 `BusinessTriggerSchedulerService`，使用低频 `@Scheduled` 统一扫描启用的 `SCHEDULE/SCHEDULED` 触发器，默认 5 分钟一次。
- 针对集群部署补齐 Redisson 分布式锁：全局扫描锁防止多个节点同时扫，记录级执行锁防止同一到期记录重复执行。
- 定时触发动作由 scheduler 调用同步单条执行入口，确保记录级锁覆盖动作执行和触发器日志写入。
- 定时扫描只按配置的 `schedule.dueField` 区间读取候选记录，字段必须经过运行态字段白名单校验，单批默认 50、最大 200。
- 新增 `SCHEDULED_DUE` 事件类型，保存时兼容历史 `SCHEDULED` 并归一为 `SCHEDULE`。
- 增加同日 `SUCCESS/TODO` 日志去重、扫描时间回写和前端定时参数配置区。
- `TriggerConditionBuilder` 保留 `eventCondition.schedule`，避免编辑条件时丢失定时参数。

## 2. 验证命令

- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`：`BUILD SUCCESS`。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm build`：构建通过。
- `git diff --check`：通过。

## 3. 警告和跳过项

- Maven 保留既有 deprecation / unchecked 编译提示，未阻断。
- 前端构建保留既有 UnoCSS 图标加载失败、CSS `//` 注释、动态/静态导入混用和 chunk size 提示，未阻断。
- 本轮未启动后端服务、数据库、Redis 或两个 admin 实例，未执行真实扫描落库、多实例抢锁和浏览器点击验证；原因是本轮按增量范围完成代码编译、前端构建和静态补丁验证，集群验证需要可用的 Redis/数据库和双实例运行环境。
- 本轮未启动任何长期服务，无需停止服务。

## 4. 结论

Task 34 增量验证通过。定时触发已从配置占位补齐为低频扫描执行能力，并已针对集群部署问题增加 Redisson 全局扫描锁、记录级执行锁和同日日志去重；真实多实例抢锁和落库去重验证仍需在联调环境补跑。

---

# Phase 10 修正执行日志：Task 34 接入系统任务调度中心

> 执行时间：2026-06-03 07:40 CST
> 范围：将低代码定时触发从 Spring 本地定时迁移到系统任务调度中心，并补齐 Quartz 集群配置。

## 1. 本轮改动

- `BusinessTriggerSchedulerService` 不再使用 Spring `@Scheduled`，改用 `@ScheduledJob` 注册到系统任务调度中心。
- 新任务名：`lowcodeBusinessTriggerScanJob`，分组：`LOWCODE`，默认 cron：`0 0/5 * * * ?`。
- `forge-plugin-generator` 新增 `forge-starter-job` 依赖，只依赖任务注解，不直接耦合 `forge-plugin-job` 实现。
- `ScheduleConfig` 显式写入 `org.quartz.jobStore.isClustered`，并从 `forge.job` 读取线程数、集群 checkin 间隔、misfire 阈值和表前缀。
- `JobProperties` 新增 `threadPoolSize`、`clustered`、`clusterCheckinInterval`、`misfireThreshold`、`tablePrefix` 配置项。
- 保留 Redisson 全局扫描锁、记录级锁和同日日志去重，作为任务中心手动触发、补偿执行或配置缺失时的并发兜底。

## 2. 验证命令

- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator,forge-framework/forge-plugin-parent/forge-plugin-job -am compile -DskipTests`：`BUILD SUCCESS`。

## 3. 警告和跳过项

- Maven 保留既有 deprecation / unchecked 编译提示，未阻断。
- 本轮未启动后端服务、数据库、Quartz 双实例或任务中心页面，未验证 `sys_job_config` 自动注册、Quartz 集群单点触发和真实扫描落库；原因是本轮为架构修正和编译验证，联调验证需要可用数据库和多实例环境。
- 本轮未启动任何长期服务，无需停止服务。

## 4. 结论

Task 34 已改为复用系统自带任务调度能力。定时触发扫描由任务中心统一调度，默认 5 分钟一次，支持后台启停和 cron 调整；Quartz 集群负责主防重，Redisson 锁和业务日志去重继续作为兜底防线。

---

# Phase 10 BUG 修正执行日志：低代码应用功能问题

> 执行时间：2026-06-03 16:22 CST
> 范围：编号规则示例格式、应用入口打开方式文案、单据填报 form-only、发布 loading、发起主流程按钮和触发器动作配置简化。

## 1. 本轮改动

- 编号规则模板统一为 `${}` 格式，后端兼容历史 `{yyyyMMdd}` / `{seq4}` 并归一为 `${yyyyMMdd}` / `${seq:4}`。
- 应用入口把“运行态页面 / 内部路由”统一改为“业务对象页面 / 系统已有页面”，`CREATE_FORM` 说明改为“单据填报，直接显示表单，不显示列表”。
- 运行页 `CREATE_FORM` 改为 `formOnly` 模式，进入页面直接展示填报表单，提交成功显示结果页，不再先渲染列表再弹窗。
- 对象设计发布按钮和发布检查按钮补齐 `loading`，发布中禁用按钮并显示加载态。
- 自定义操作和运行态按钮统一为“发起主流程”，按钮只配置名称、位置、权限和提示，不再重复选择流程。
- 触发器 `START_FLOW` 动作默认使用“流程与自动化”中的主流程，触发器面板不再展示流程模型、标题模板和变量映射；后端执行时空流程 key / 空标题回落到主流程绑定。

## 2. 验证命令

- `git diff --check`：通过。
- 首次执行 `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator,forge-framework/forge-plugin-parent/forge-plugin-job -am compile -DskipTests`：失败，原因是默认 JDK 不支持 target 17，报 `无效的目标发行版: 17`。
- 使用项目指定 Java 17 复跑：`JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator,forge-framework/forge-plugin-parent/forge-plugin-job -am compile -DskipTests`：`BUILD SUCCESS`。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`：构建通过。

## 3. 警告和跳过项

- Maven 保留既有 deprecation / unchecked 编译提示，未阻断。
- 前端构建保留既有 UnoCSS 图标加载失败、CSS `//` 注释、动态/静态导入混用和 chunk size 提示，未阻断。
- 本轮未启动后端服务、Flow 服务、前端 dev server 或数据库，未执行真实接口联调和浏览器点击验证；原因是本轮按用户反馈完成代码修正和构建验证，联调需要可用 dev 数据和服务链路。
- 本轮未启动任何长期服务，无需停止服务。

## 4. 结论

本轮 BUG 修正的后端插件编译、前端生产构建和补丁空白检查均已通过。触发器和自定义操作的发起流程配置已收敛为主流程复用，单据填报入口已改为独立表单页。

---

# Phase 10 BUG 跟进执行日志：入口模式与自定义操作显示

> 执行时间：2026-06-03 16:57 CST
> 范围：对象类型与入口打开方式说明、父级菜单回填、自定义操作注入运行态列表、单据设置解释和操作列兼容识别。

## 1. 本轮改动

- 明确产品口径：对象类型只描述数据结构，入口打开方式描述使用场景；单表对象不强制只能“单据填报”，需要列表行操作时选择“列表管理”。
- 应用入口详情 VO 新增 `adminMenuParentId`、`adminMenuSyncEnabled`、`suiteAsMenuParent`、`menuSort`，前端编辑器优先使用详情字段并兼容旧 `options.adminMenu`。
- 菜单同步时保存 `originalParentId`，避免“套件作为父级目录”时实际挂载父级和配置父级混用导致回填失败。
- 发布业务对象时将 `designerOptions.actions` 转为运行态 `customActions` 注入 table zone，运行态配置生成 `rowActions/toolbarActions` 后能进入列表操作列。
- `crud-page.vue` 和 `AiCrudPage.vue` 兼容 `action/actions/operation/operations` 操作列命名；没有操作列但存在行操作时主动创建“操作”列。
- 单据设置补充状态字段、发起人/负责人和主流程的职责说明，状态映射表头改为“允许编辑 / 允许删除 / 允许发起”。

## 2. 验证命令

- `git diff --check`：通过。
- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator,forge-framework/forge-plugin-parent/forge-plugin-job -am compile -DskipTests`：`BUILD SUCCESS`。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`：构建通过。

## 3. 警告和跳过项

- Maven 保留既有 deprecation / unchecked 编译提示，未阻断。
- 前端构建保留既有 UnoCSS 图标加载失败、CSS `//` 注释、动态/静态导入混用和 chunk size 提示，未阻断。
- 本轮未启动后端服务、Flow 服务、前端 dev server 或数据库，未执行真实接口联调和浏览器点击验证；原因是本轮按用户反馈完成代码修正和构建验证，联调需要可用 dev 数据和服务链路。
- 本轮未启动任何长期服务，无需停止服务。

## 4. 结论

用户反馈跟进项的后端插件编译、前端生产构建和补丁空白检查均已通过。入口打开方式已从对象类型中解耦，父级菜单回填字段已拍平，自定义操作按钮已补齐发布注入和运行态操作列渲染兼容。

---

# Phase 10 BUG 跟进执行日志：非单据对象发起主流程

> 执行时间：2026-06-03 17:08 CST
> 范围：修复未启用单据模式的业务对象点击“发起主流程”时报“业务对象未启用单据模式，无法发起主流程”。

## 1. 本轮改动

- `BusinessFlowService.startDocumentFlowInternal` 改为双路径：启用单据模式时保留单据状态和动作权限校验；未启用单据模式时按对象已发布运行配置读取记录，并使用“流程与自动化”的主流程绑定发起流程。
- 新增 `AiCrudConfigMapper.selectPublishedByObjectCode`，从已发布 LOWCODE 运行配置中解析非单据对象的 `configKey`。
- 流程发起后只有单据对象才回写单据状态为 `IN_PROCESS`；普通业务对象只写流程实例关联，不强制要求状态字段。
- 流程回调不再因为对象未启用单据模式而失败；非单据对象回调会更新流程实例状态，并在存在运行配置时继续发布 `FLOW_APPROVED/FLOW_REJECTED/FLOW_CANCELED` 业务事件。

## 2. 验证命令

- `git diff --check`：通过。
- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`：`BUILD SUCCESS`。

## 3. 警告和跳过项

- Maven 保留既有 deprecation / unchecked 编译提示，未阻断。
- 本轮只改后端 Java 和 Mapper XML，未重跑前端生产构建。
- 本轮未启动后端服务、Flow 服务或数据库，未执行真实流程发起和回调联调；原因是本轮按错误来源完成后端逻辑修复和编译验证，联调需要可用运行配置、流程模型和记录数据。
- 本轮未启动任何长期服务，无需停止服务。

## 4. 结论

非单据业务对象现在也可以通过“发起主流程”自定义操作走对象主流程绑定。单据模式仍保留状态字段、状态映射和动作权限校验；普通对象不再被“未启用单据模式”拦截。

---

# Phase 10 BUG 跟进执行日志：发布检查与回显一致性

> 执行时间：2026-06-03 17:33 CST
> 范围：修复应用入口发布检查误报、父级菜单回显、主流程绑定读取和旧审批兼容状态。

## 1. 本轮改动

- `selectRuntimeAppByObject` 调整排序：优先启用、已绑定运行配置、已同步菜单资源、已配置打开模式、最近更新的运行态入口，避免发布检查读到历史旧入口后误报“菜单资源未同步”。
- 发布检查不再因为 `runtimeOpenMode` 缺省报警；缺省按 `LIST` 处理，仅在存在非法值时提示“运行打开模式不合法”。
- 菜单同步写入 `options.adminMenu` 时将 `menuResourceId`、`parentId`、`originalParentId` 字符串化，避免前端 `JSON.parse` 雪花 Long 后精度丢失。
- `MenuParentSelect` 和应用入口编辑器保留菜单 ID 字符串，不再强转 Number；入口编辑器保存时优先保留详情接口拍平的 `menuResourceId`。
- 主流程绑定查询优先启用且有模型的绑定，并兼容历史 `APPROVAL` 绑定，避免旧空绑定抢先命中后提示“请先配置主流程”。
- 旧审批兼容运行态在对象未启用完整单据状态能力但已配置主流程时，不再返回“未启用单据模式”作为不可发起原因。

## 2. 验证命令

- `git diff --check`：通过。
- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`：`BUILD SUCCESS`。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`：构建通过。

## 3. 警告和跳过项

- Maven 保留既有 deprecation / unchecked 编译提示，未阻断。
- 前端构建保留既有 UnoCSS 图标加载失败、CSS `//` 注释、动态/静态导入混用和 chunk size 提示，未阻断。
- 本轮未启动后端服务、Flow 服务、前端 dev server 或数据库，未执行真实发布检查接口、菜单同步落库和浏览器回显验证；原因是本轮按用户反馈完成读取/回显口径修正和构建验证，联调需要可用业务对象、应用入口、菜单资源和流程模型数据。
- 本轮未启动任何长期服务，无需停止服务。

## 4. 结论

发布检查现在会优先读取真正可用的运行态入口，缺省打开模式不再误报；菜单父级和资源 ID 避免 Long 精度丢失；主流程摘要和发起兼容层能识别新 `FLOW` 与历史 `APPROVAL` 绑定。

---

# Phase 10 BUG 跟进执行日志：运行态按钮、主流程诊断和套件目录回显

> 执行时间：2026-06-03 20:06 CST
> 范围：修复 `/ai/crud-page` 头部重复“新增”、主流程发起失败诊断日志、应用入口“套件作为父级目录”实际挂载目录回显。

## 1. 本轮改动

- `/ai/crud-page` 运行态过滤 `add/create/new/新增/新建` 这类无目标路由的标准新增 toolbar action，避免和 `AiCrudPage` 内置新增按钮重复。
- 业务对象自定义操作不再默认预置标准 CRUD 动作；发布运行态时跳过历史保存的标准新增、编辑、详情、删除动作，避免老数据继续生成重复按钮。
- 保存主流程绑定时，已有 `FLOW` 绑定会同步修正 `targetType/targetCode/bindingType` 并置为启用，避免页面保存后仍因停用绑定提示“请先配置主流程”。
- 流程发起解析主流程失败时增加诊断日志，输出租户、对象、记录、运行配置、单据配置、绑定状态、候选 `flowModelKey` 来源和 `binding_config` 截断预览。
- 应用入口菜单同步保存 `adminMenu.actualParentId/suiteMenuResourceId`；入口编辑抽屉在勾选“套件作为父级目录”后只读回显实际生成的套件目录，父级选择框继续表示套件目录的上级。

## 2. 验证命令

- `git diff --check`：通过。
- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`：`BUILD SUCCESS`。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`：构建通过。

## 3. 警告和跳过项

- Maven 保留既有 deprecation / unchecked 编译提示，未阻断。
- 前端构建保留既有 UnoCSS 图标加载失败、CSS `//` 注释、动态/静态导入混用和 chunk size 提示，未阻断。
- 本轮未启动后端服务、Flow 服务、前端 dev server 或数据库，未执行真实流程发起日志、菜单同步落库和浏览器回显验证；原因是本轮按用户反馈完成代码修正和构建验证，联调需要可用业务对象、菜单资源、流程模型和记录数据。
- 本轮未启动任何长期服务，无需停止服务。

## 4. 结论

运行态重复新增按钮已从前端渲染和发布数据两侧兜底去重；主流程保存会重新启用已停用的 `FLOW` 绑定，发起失败时可通过新增日志定位绑定/配置来源；入口配置能回显自动生成的套件目录实际挂载位置。
