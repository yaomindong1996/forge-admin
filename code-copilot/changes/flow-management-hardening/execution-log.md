# Flow Management Hardening Execution Log

## 2026-09-06 — 超时扫描租户游标与流程图用户批量查询

- 超时扫描新增 `FlowTaskMapper.selectTimeoutCandidates`，按 `due_date + id` 稳定游标读取本地活动任务快照；每条任务在其 `tenant_id` 上下文内查询 Flowable 活动任务并执行超时处理，避免 Flowable 全局分页和跨租户节点配置回查。
- 流程组织服务新增 `getUserInfoBatch`，系统用户 Mapper 增加单次 SQL 批量读取用户/主组织/岗位摘要；流程图节点详情复用请求级缓存，消除逐用户 N+1 查询。
- Flow 插件及依赖 `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow -am -DskipTests compile`：BUILD SUCCESS。
- 契约测试 `FlowTimeoutServiceQueryContractTest`（5/5）与 `FlowOrgIntegrationSecurityContractTest`（1/1）通过；测试源码编译通过。
- `FlowTimeoutServiceImplTest` 在本机执行时受既有 Mockito inline Byte Buddy 无法 attach 当前 Homebrew JDK 限制（`Could not self-attach to current VM`），未归因于本次代码；真实 MySQL/Redis/Flowable 调度和跨实例游标仍未验证。
- 修正测试构造器以注入新增 `FlowTaskMapper`；重新执行 `FlowTimeoutServiceQueryContractTest`、`FlowOrgIntegrationSecurityContractTest`：6/6 通过。Flow reactor compile：BUILD SUCCESS。
- 流程图详情在节点遍历前预加载当前任务/历史任务涉及的用户集合，后续节点仅命中请求级缓存；组织服务回查从按用户/节点退化为单次批量 SQL。
- 新增 `V1.0.145__add_flow_timeout_cursor_index.sql`，为 `sys_flow_task(due_date, id, status, tenant_id)` 建立受保护索引，脚本无 Flyway 占位符且可重复执行。
- 复验 `FlowOverdueReminderRetryContractTest`：3/3 通过；同时修正契约断言使用实体访问器 `getNextRetryTime`，避免把字段名误当作源码调用。
- 增加旧任务 `due_date` 限量回填：按 `create_time + id` 稳定分页，依据节点超时配置回写 Flowable 和 `sys_flow_task`，并对已到期任务继续执行超时处理；`FlowTimeoutServiceQueryContractTest` 更新后 6/6 通过，Flow reactor compile：BUILD SUCCESS。
- 回填实现后的最终 Flow reactor compile：BUILD SUCCESS（16.098s）；Mapper XML 与 `git diff --check` 继续通过。
- Review 修复：旧任务回填循环按“已扫描条数”限量，而不是按成功回填条数限量，避免无超时配置的历史任务导致单轮扫描无界扩张；最新 Flow reactor compile：BUILD SUCCESS（16.342s）。
- 管理员流程回退增加运行实例、活动节点、BPMN 目标节点存在性及“目标不能为当前节点”校验，避免任意目标 ID 直接触发 Flowable 状态变更。
- `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow,forge-flow/forge-flow-server -am -DskipTests compile`：BUILD SUCCESS（55.436s）。
- `FlowTaskMutationAuthorizationContractTest`：4/4 通过。
- 管理员转派本地任务回查改为 `selectByTaskIdAndTenant`，避免重复任务 ID 或脏数据下跨租户更新；`FlowTaskMutationAuthorizationContractTest` 更新后 5/5 通过，Flow/Flow Server compile：BUILD SUCCESS（53.005s）。
- 普通委派和发起人改派新增目标用户租户/启用状态校验，并将直接 `setAssignee` 后的任务镜像状态改为 `CLAIMED`；Flow 插件 reactor compile：BUILD SUCCESS（14.375s）。直接插件测试受本地旧 `forge-starter-core` 缓存影响，需在 reactor/CI 环境复跑。

## 2026-09-05 — 变更启动

- 范围：创建流程管理补强 Spec/Tasks/Test Spec，准备实施 P0 访问边界和动作授权。
- 基线：工作树已有 `enterprise-framework-hardening` 相关未提交改动，本轮不覆盖、不回滚这些文件。
- 服务：本轮尚未启动服务。
- 验证：待完成首批代码改动后执行 `git diff --check` 和 Flow 模块 Maven 测试。

## 2026-09-05 — 首批实现与验证

### 已完成实现

- 新增 `FlowAccessGuard`，统一校验租户、登录用户、任务参与关系及流程业务归属；越权读取返回 `FLOW_RESOURCE_NOT_FOUND`。
- 任务详情、流程图、流程图信息、任务/流程表单、流程历史、催办入口接入访问守卫。
- `FlowTaskMapper` 新增按租户读取任务及流程参与人计数 SQL，显式过滤未删除数据；参与人查询不再把候选组 ID 当作用户 ID。
- 撤回缺少可信发起人时 fail-closed，并记录 warning；催办及手动逾期扫描增加 `flow:task:remind` 权限。
- 超时扫描启用定时批处理；监控统计使用任务 `due_date`，实例分页支持 `overdue`，前端卡片传递真实筛选参数并统一使用 `pageNum`。

### 验证命令与结果

1. `JAVA_HOME=.../openjdk@17/... mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow,forge-flow/forge-flow-server -am -DskipTests compile`
   - 结果：通过（BUILD SUCCESS）。
2. `git diff --check`
   - 结果：通过。
3. `mvn -pl ... -am test`
   - 结果：环境阻断：父工程 Surefire 将默认 `profiles.active=dev` 作为 JUnit tag，在无 JUnit engine 的 starter 模块失败。
4. `mvn -f forge-framework/forge-plugin-parent/forge-plugin-flow/pom.xml test -Dforge.test.groups=`
   - 结果：流程模块静态/枚举测试通过；20 个 Mockito 测试因当前 JDK 禁止 Byte Buddy agent attach 失败（`Could not self-attach to current VM`）。
5. `cd forge-admin-ui && npm run build`
   - 结果：通过（Vite built in 57.38s）。存在既有 Vite native config、CSS 注释及 dynamic import 警告，不影响产物。

### 跳过与后续

- 未启动 MySQL、Redis、Flowable 外部服务，未执行端到端越权、通知和迁移验证。
- 稳定游标扫描、通知记录幂等、动态加签/临时抄送、管理员任务、模型部署预检和候选关系迁移仍待后续 Phase 实施。
6. `mvn -f forge-framework/forge-plugin-parent/forge-plugin-flow/pom.xml test-compile -DskipTests`
   - 结果：通过；新增候选组访问与逾期逻辑删除 SQL 合同测试编译成功。

## 2026-09-05 — 监控实例列表批量化

- 新增 `FlowTaskMapper.selectActiveTaskSummaries`，按租户和当前页流程实例 ID 批量读取活动任务摘要。
- `FlowMonitorServiceImpl.getAdminProcessInstances` 先批量加载摘要，再组装列表，移除逐实例 `TaskQuery.list()`，避免监控页 N+1 查询。
- 空实例集合在 SQL 中显式返回空集，防止误扫全表；查询过滤逻辑删除任务。
- `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow -am -DskipTests compile`：通过。

## 2026-09-05 — 监控错误边界

- 统计接口异常改为返回 `degraded=true`、`FLOW_MONITOR_STATS_UNAVAILABLE`，统计值使用 `null`，不再伪装为 0。
- 监控页面增加统计告警/重试和实例列表错误态/重试；只有成功响应才展示空列表。

## 2026-09-05 — 下一阶段验证

- Flow 插件及依赖模块 `-am -DskipTests compile`：通过（BUILD SUCCESS）。
- 前端 `npm run build`：通过（Vite built in 75.7s）。仍有既有 Vite native config、CSS 注释和 dynamic import 警告。
- 新增监控错误态后完成生产构建验证。
- 新增 `FlowMonitorBatchQueryContractTest`，约束监控分页必须使用批量任务摘要查询。
- Flow 插件 `test-compile -DskipTests`：通过（21 个测试类编译成功）。

## 2026-09-05 — 流程列表错误态

- 待办、已办、我发起列表增加错误状态和重试入口；接口失败时保留错误态，不展示误导性的空列表。
- 前端生产构建通过（Vite built in 85.4s）。
- 待办/已办/我发起/抄送/监控页面定向 ESLint：通过（仅 npm 全局配置弃用警告）。

## 2026-09-05 — 批量审批与高影响动作授权

- 批量审批/驳回失败任务不再随部分成功一起关闭；失败任务保留在弹窗并支持重试，避免误以为全部完成。
- 委派和任务终结接入统一租户、业务实例和操作者关系校验，拒绝非处理人/拥有者的高影响操作。
- 新增 `FlowTaskMutationAuthorizationContractTest`。
- Flow 插件依赖编译：通过（BUILD SUCCESS）。
- 待办页面定向 ESLint：通过。
- 最新前端 `npm run build`：通过（Vite built in 44.95s）；保留既有 native config/CSS/dynamic import 警告。

## 2026-09-05 — BPMN 发布前结构校验

- `FlowModelServiceImpl.deployModel` 在部署前校验开始节点、结束节点、sequenceFlow 引用及悬空连线。
- 不满足结构要求时返回包含修复方向的中文错误，不进入 Flowable 部署。
- 新增 `FlowModelDeploymentValidationContractTest`。
- Flow 插件依赖编译及 test-compile：通过。
- 选定契约测试命令在当前 Surefire/JUnit tag 配置下返回 0 tests（构建成功但未执行测试），因此本轮以 test-compile 和源代码契约校验作为证据，待 CI 修复 tag 配置后补跑。

## 2026-09-05 — 管理员实例任务聚合

- 新增管理员按流程实例分页查询全部任务接口：`GET /api/flow/monitor/instance/{processInstanceId}/tasks`。
- 接口要求 `flow:monitor:view`，服务层先校验当前租户下的流程实例，再按租户和逻辑删除条件查询任务。
- 增加 `pageNum/pageSize` 安全边界，单页最大 100 条。
- 前端 `flow.js` 增加接口封装，新增 `FlowAdminTaskMapperSqlContractTest`。
- Flow、Flow Server 依赖编译通过。
- Flow 插件 `test-compile -DskipTests`：通过（24 个测试类编译成功）。
- 前端 `todo.vue`、`monitor.vue`、`api/flow.js` 定向 ESLint：通过。

## 2026-09-05 — 抄送访问边界收敛

- `FlowCcServiceImpl` 的我的抄送、发送抄送、未读统计、单条已读和批量已读均显式校验当前租户；已读变更还校验当前抄送接收人。
- 新增 `getVisibleById`，抄送表单详情仅允许接收人或发送人查看，并在 `FlowCcController` 使用该入口，避免按 ID 直接读取他人抄送。
- 新增 `FlowCcSecurityContractTest`，覆盖上述安全谓词的源码契约。
- 待执行：Flow 插件 test-compile、`git diff --check`，以及可用集成环境中的真实跨租户/非参与人接口验证。

## 2026-09-05 — 统计聚合与管理员任务上下文

- `FlowBusinessMapper` 新增 `selectProcessInstanceStats`，用数据库聚合替代 `getProcessInstanceStats` 对所有历史实例的 JVM 全量加载；查询按租户和流程定义过滤。
- 新增 `FlowBusinessStatsMapperSqlContractTest`。
- 监控实例详情抽屉接入 `getMonitorInstanceTasks`，提供分页任务审批上下文、局部错误态和重试。
- 待验证：Flow 插件编译、前端监控页定向 ESLint 和生产构建。

## 2026-09-05 — 模型执行节点预检

- 部署前新增执行节点白名单和网关条件校验：scriptTask/callActivity/subProcess 直接阻断；serviceTask 仅接受 `flowable:type=cc`；userTask 缺少处理人策略时阻断；互斥/包容网关出口必须配置条件表达式或默认分支。
- `FlowModelDeploymentValidationContractTest` 已覆盖预检调用和诊断约束。
- Java 17 下 `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow -am -DskipTests compile`：通过。
- Java 17 下 `mvn -f forge-framework/forge-plugin-parent/forge-plugin-flow/pom.xml test-compile -DskipTests`：通过（26 个测试类编译成功）。
- `npx eslint src/views/flow/monitor.vue src/api/flow.js`：通过。

## 2026-09-05 — 本轮增量验证完成

- `git diff --check`：通过。
- Java 17 下 Flow/Flow Server 依赖编译：通过（BUILD SUCCESS）。
- Java 17 下 Flow 插件 test-compile：通过（26 个测试类编译成功）。
- 定向契约测试（抄送安全、统计聚合、模型预检）：3 个测试全部通过。
- `npx eslint src/views/flow/monitor.vue src/api/flow.js`：通过。
- `npm run build`：通过（Vite built in 42.91s）；保留仓库既有 native config、CSS 注释和 dynamic import 警告。
- 直接在插件目录运行测试曾因未先构建 reactor 依赖而出现 `forge-starter-core` 类路径缺失；改用 Java 17 的 `-am` 依赖编译后重跑，3 个定向契约测试全部通过。
- 抄送页未读徽标改为调用服务端未读总数接口，批量已读失败增加明确反馈；`npx eslint src/views/flow/cc.vue src/views/flow/monitor.vue src/api/flow.js`：通过。
- 抄送页调整后的 `npm run build`：通过（Vite built in 44.94s）；同样保留既有 native config、CSS 注释和 dynamic import 警告。

## 2026-09-05 — 动态加签与减签

- 新增 `FlowTaskSignDTO`、`FlowTaskService.addSign/reduceSign` 及 Flow 服务端接口 `/api/flow/task/add-sign`、`/api/flow/task/reduce-sign`。
- 动作在服务层校验任务租户、当前处理人/拥有人关系和目标用户租户可用性；底层同步 Flowable 候选用户集合与本地任务快照，并写入 Flowable 评论审计。
- 新增 `FlowTaskSignContractTest`；Flow/Flow Server Java 17 依赖编译通过。
- `npx eslint src/views/flow/started.vue src/api/flow.js`：通过；发起流程详情已接入动态加签/减签弹窗和提交锁。

## 2026-09-05 — 前端构建与人工抄送发送授权

- 动态加签 UI 完成后重新执行 `source ~/.nvm/nvm.sh && nvm use v20.19.0 && npm run build`：通过，Vite built in 42.36s。
- 保留既有 Vite native config、CSS `//` 注释和 ineffective dynamic import 警告；未发现构建错误。
- 人工抄送接口改用 `sendCcByCurrentUser`，服务层增加流程实例租户/业务归属、任务参与人、发起人/历史参与人、目标用户可用性和接收人数上限校验；内部 Flowable 回调继续使用 `sendCc`。
- `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow,forge-flow/forge-flow-server -am -DskipTests compile`：通过（Java 17，BUILD SUCCESS）。
- `mvn -f forge-framework/forge-plugin-parent/forge-plugin-flow/pom.xml test-compile -DskipTests`：通过（28 个测试类编译成功）。
- `mvn -f forge-framework/forge-plugin-parent/forge-plugin-flow/pom.xml -Dtest=FlowCcSendSecurityContractTest -Dforge.test.groups= test`：通过（1/1）。
- `git diff --check` 待本轮全部修改收尾后执行。

## 2026-09-06 — Review 修复与增量验证

### 修复内容

- 移除 `FlowTaskMapper.xml` 对 `sys_flow_task.del_flag` 和 `update_time` 的错误引用；当前运行表只使用已有字段。
- 终止/撤回在调用 Flowable 删除前捕获活动 taskId，删除后按 taskId、租户和活动状态更新，避免历史任务被批量覆盖。
- 修复 `FlowTaskStatus.CANCELED` 的历史动作值；新增 `V1.0.142__align_flow_task_status_dict.sql` 对齐字典 5/7/8 状态；发起列表改为直接使用服务端字典选项。
- 修复内部抄送无会话租户回调、流程启动阶段业务键兜底、显式抄送 tenantId，并允许抄送接收人通过流程可见性校验。
- 签收增加租户、待办状态和候选用户/组成员校验；启动预览读取 UserTask 原生候选集合并复用 visited 集合；BPMN serviceTask 拒绝 raw XML 委托属性。
- 将 `tasks.md` 改为逐项 `[x]/[~]/[ ]` 状态，明确动态父子加签、候选关系迁移和集成验收仍未完成。

### 验证命令与结果

1. `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow,forge-flow/forge-flow-server -am -DskipTests compile`
   - 结果：Java 17 编译通过，Flow/Flow Server 依赖构建 BUILD SUCCESS。
2. `mvn -f forge-framework/forge-plugin-parent/forge-plugin-flow/pom.xml test-compile -DskipTests`
   - 结果：通过，30 个测试类编译成功。
3. 定向契约测试（状态迁移、Mapper schema、管理员任务、超时、抄送安全、启动预览、模型预检、监控批量/聚合、动作授权、动态签名）
   - 结果：18 个测试全部通过。
4. `git diff --check`
   - 结果：通过。
5. `npx eslint src/views/flow/started.vue src/views/flow/cc.vue src/views/flow/monitor.vue src/views/flow/todo.vue src/views/flow/done.vue src/api/flow.js`
   - 结果：0 errors，8 个模板换行 warning。
6. `npm run build`
   - 结果：通过（Vite built in 45.45s）；保留仓库已有 native config、CSS 注释和 dynamic import 警告。
7. `rg -n '\$\{[^}]+\}' forge-server/db/migration`
   - 结果：仅发现历史 V1.0.72 模板脚本，新增 V1.0.142 无占位符。

### 未执行/阻断

- 本机未启动 MySQL、Redis、Flowable，未执行真实流程启动、终止、撤回、抄送和跨租户接口验证。
- 未执行完整 Maven test；既有 Surefire profile tag 与 Mockito/Byte Buddy attach 环境问题仍存在。
- 未执行性能压测、灰度和回滚演练；T7.2/T7.3 保持未完成。

### Review 后快速复验

- `mvn -f forge-framework/forge-plugin-parent/forge-plugin-flow/pom.xml -Dtest=FlowTaskStatusTransitionContractTest,FlowTaskMapperSqlContractTest,FlowTimeoutServiceQueryContractTest -Dforge.test.groups= test`：9 个测试全部通过。
- 新增迁移脚本静态检查（tenant_id、防重复、无新 `${...}`）和 `git diff --check`：通过。
- “我发起”列表状态文案与状态图标修正后，`npx eslint src/views/flow/started.vue`：0 errors、8 个既有模板换行 warning；`npm run build`：通过（Vite built in 43.77s）。

## 2026-09-05 — 启动前节点预览与最终增量验证

- `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow,forge-flow/forge-flow-server -am -DskipTests compile`：通过（Java 17，BUILD SUCCESS）。
- 定向契约测试 9 个全部通过：抄送租户/发送授权、启动前节点预览、统计聚合、管理员任务、模型预检、监控批量查询、动作授权、动态加签。
- `npx eslint src/views/flow/started.vue src/views/flow/cc.vue src/views/flow/monitor.vue src/views/flow/todo.vue src/views/flow/done.vue src/api/flow.js`：0 errors，8 个既有模板换行 warning。
- `git diff --check`：通过。
- 真实 MySQL/Redis/Flowable 端到端测试仍需集成环境；本机未启动外部依赖。

## 2026-09-06 — Review 修复任务终结状态

- Review 发现：`runtimeService.deleteProcessInstance` 触发任务删除监听器后，任务状态会先写为 `CANCELED`，导致业务流程状态 `TERMINATED` 与任务状态不一致。
- 修复为 `FlowTaskMapper.updateProcessTaskStatusByTaskIds`：删除前捕获活动 taskId，按租户和 taskId 修正状态；流程级终止、任务级终止只将活动状态 `0/1/5` 修正为 `TERMINATED`，撤回只将活动状态修正为 `WITHDRAWN`，不覆盖历史任务。
- 同时移除运行表不存在的 `del_flag/update_time` SQL 引用，并对齐任务状态字典与发起列表筛选。
- Java 17 编译：通过（BUILD SUCCESS）；完整定向契约测试 18/18 通过，快速复验 9/9 通过。
- `git diff --check`：通过。
- 任务清单已改为按实际完成度标记 `[x]`、`[~]` 和待执行 Phase 7，避免文档状态与代码证据不一致；真实外部依赖和灰度验收仍未完成。

## 2026-09-06 — 模型与监控操作提交锁

- 修改 `forge-admin-ui/src/views/flow/model.vue`：为模型级发布、复制、挂起、激活、删除、发起测试增加动作锁；确认取消、成功刷新、失败和异常路径统一释放锁，更多菜单和发布入口在锁定期间禁用。
- 修改 `forge-admin-ui/src/views/flow/monitor.vue`：为清理、删除、挂起、激活、终止、回退、转派、错误重试和标记解决增加互斥 mutation 状态；成功后刷新列表、统计及当前实例上下文，失败反馈显示服务端 requestId（若返回）。
- `npx eslint src/views/flow/model.vue src/views/flow/monitor.vue`：0 errors、0 warnings；同时修复模型弹窗既有模板换行格式。
- `git diff --check`：通过。
- 首次直接执行 `pnpm build` 因仓库既有 `pnpm-workspace.yaml` 缺少 `packages` 字段失败；临时移出该配置后执行同一构建脚本，`pnpm build` 通过，Vite built in 55.69s，构建结束已恢复配置文件。
- 未执行真实浏览器连续点击、权限变化、接口超时和外部服务集成验证；T6.3 标记为部分完成，`form.vue`/`design.vue` 及真实 E2E 保持待补。

## 2026-09-06 — 启动前审批人预检

- `FlowStartConfig` 增加 `preflightPassed` 与 `diagnostics`；`InitiatorSelectedApproverSupport` 对启动后首批可达用户任务检查 assignee、候选用户和候选组配置，发起人自选节点跳过静态缺失告警。
- `/api/flow/model/key/{modelKey}/start-config` 返回节点级诊断；`model.vue` 测试发起弹窗展示诊断，帮助用户在发起前发现空审批节点。
- 全量 Flow reactor compile 尝试失败：工作树已有 `CaptchaServiceImpl` 引用缺失 `Profiles` 符号；插件目录直接 compile 另因本地未安装含新枚举的 `forge-starter-core` 失败。该失败与本轮新增代码无直接关联，待整理工作树后复跑。
- 未启动 Flowable、组织服务或数据库，未执行动态 SpEL、用户组成员和禁用用户实链路验证；T5.3 维持部分完成。

## 2026-09-06 — 详情业务关联租户回查修复

- Review 发现流程历史在完成可见性校验后仍调用无租户 `selectByProcessInstanceId`，任务表单也存在同类回查。
- 修复为复用 `FlowAccessGuard` 返回的业务记录，或按已校验任务租户调用 `selectByProcessInstanceIdAndTenantId`。
- 新增契约测试断言历史/表单不发生无租户业务回查；尚未在真实多租户数据库执行接口验证。

## 2026-09-06 — 编译与租户回查复验

- 修复工作树既有 `CaptchaServiceImpl` 缺失 `org.springframework.core.env.Profiles` 导入，解除 Flow reactor 编译阻断。
- `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow,forge-flow/forge-flow-server -am -DskipTests compile`：通过，Flow/Flow Server BUILD SUCCESS（41.697s）。
- `mvn -f forge-framework/forge-plugin-parent/forge-plugin-flow/pom.xml test-compile -DskipTests`：通过，30 个测试类编译成功。
- 定向契约测试 `FlowStartPreviewContractTest,FlowTaskMutationAuthorizationContractTest`：3/3 通过。
- 本轮仍未启动 MySQL、Redis、Flowable，未执行真实跨租户接口与组织解析链路。

## 2026-09-06 — 历史与流程图详情返回上限

- 流程历史、流程图活动、历史任务和当前任务摘要统一增加 1000 条上限，防止超长流程详情无界读取。
- 新增契约断言；`FlowStartPreviewContractTest`、`FlowTaskMutationAuthorizationContractTest`：4/4 通过。
- Flow/Flow Server reactor compile：通过；Flow 插件 test-compile：通过。
- 用户/组织信息批量解析、已执行连线完整计算和真实性能压测仍未完成。

## 2026-09-06 — 模型版本治理与设计器提交锁

- 模型版本查询增加 tenant_id 条件，版本逻辑删除增加租户条件；存在运行中流程实例时拒绝删除版本；模型/版本分页大小上限为 100。
- 表单管理保存、设计器保存/部署及表单发布、复制、删除、启停增加提交锁，避免重复请求。
- `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow,forge-flow/forge-flow-server -am -DskipTests compile`：通过。
- `mvn -f forge-framework/forge-plugin-parent/forge-plugin-flow/pom.xml -Dtest=FlowModelVersionGovernanceContractTest,FlowStartPreviewContractTest,FlowTaskMutationAuthorizationContractTest -Dforge.test.groups= test`：7/7 通过。
- `npx eslint src/views/flow/form.vue src/views/flow/design.vue`：0 errors、0 warnings。
- 未执行真实数据库/Flowable 版本引用检查、浏览器连续点击和性能压测；T5.4、T6.3 继续保持部分完成。
- 追加复验：Flow reactor compile 再次通过；尝试在 reactor 中重跑单测时被仓库既有 Surefire `groups/excludedGroups` 配置阻断（forge-starter-core 未加载测试引擎），未将该环境失败计入本次代码失败。

## 2026-09-06 — 动态加签边界收敛

- 动态加签/减签增加当前办理人和操作者重复加入校验，并限制单任务动态加签候选人数为 50。
- `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow,forge-flow/forge-flow-server -am -DskipTests compile`：通过。
- `mvn -f forge-framework/forge-plugin-parent/forge-plugin-flow/pom.xml -Dtest=FlowTaskSignContractTest -Dforge.test.groups= test`：1/1 通过。
- 父子加签关系、临时抄送撤回关系和真实 Flowable 多实例验证仍未执行。

## 2026-09-06 — 组织用户回查边界

- `FlowOrgIntegrationServiceImpl.getUserInfo` 增加可信租户上下文、用户启用状态、逻辑删除和有效租户成员关系校验。
- 新增 `FlowOrgIntegrationSecurityContractTest`，约束跨租户或禁用用户不会作为流程审批人详情返回。
- 尚未启动真实组织服务和多租户数据库验证。

## 2026-09-06 — 组织候选查询 SQL 收敛

- `SysUserMapper` 新增流程专用用户详情、选人列表、部门/岗位交集候选查询，所有查询显式绑定 `tenant_id`、有效租户成员、用户/组织/岗位启用状态，并将选人列表上限设为 200。
- `SysUserOrgRoleMapper` 新增跨活动组织角色候选查询，仍按租户、角色有效性、用户有效性和组织范围过滤。
- `SysRoleMapper` 新增按租户和角色编码读取有效角色的方法，流程候选解析不再依赖隐式租户查询。
- Flow/Flow Server reactor compile：通过；`FlowOrgIntegrationSecurityContractTest`：1/1 通过；`git diff --check`：通过。
- 真实 MySQL 执行计划、组织关系脏数据、跨租户接口和用户组/表达式资源管理仍待集成环境与后续任务补齐。

## 2026-09-06 — 表达式模板租户与写接口治理

- 修改 `FlowSpelTemplateMapper`/XML：分页、详情、编码唯一校验、状态更新和逻辑删除均带 `tenant_id` 与 `deleted = 0`；启用列表增加 `LIMIT 200`。
- 修改 `FlowSpelTemplateServiceImpl`：使用 `SessionHelper` 获取可信租户，页大小限制 100，模板编码按租户唯一，创建强制写入租户，删除改为租户内逻辑删除，状态操作拒绝跨租户 ID；复杂分页查询移出 Service 层 Wrapper。
- 修改 `FlowSpelTemplateController`：创建/更新改用 `FlowSpelTemplateCreateDTO`、`FlowSpelTemplateUpdateDTO`，详情改为租户限定读取，避免实体直接作为写请求体。
- 验证：XML 语法通过；`git diff --check` 通过；Flow/Flow Server reactor compile BUILD SUCCESS；`FlowSpelTemplateSecurityContractTest` 2/2 通过。
- 未启动服务、数据库或 Flowable；未声称完成真实租户隔离、运行时表达式解析、用户组能力和性能验证。

## 2026-09-06 — 运行时 SpEL 组织解析收敛

- `FlowSpelService` 的角色、部门角色、区域用户解析统一通过 `FlowOrgIntegrationService`，移除直接调用系统 Service 的 Wrapper 查询；区域查询新增 `SysUserMapper.selectFlowUserIdsByRegion`，显式绑定租户、有效成员、启用状态并限制 200 条。
- 运行时动态审批人结果限制最多 200 个，移除流程变量整包和审批人 ID 结果的 info 日志，降低敏感数据泄露和日志膨胀风险。
- 新增 `FlowSpelServiceSecurityContractTest`，与表达式模板契约测试合计 3/3 通过。
- Flow/Flow Server reactor compile、Mapper XML 语法和 `git diff --check` 均通过；真实 Flowable 表达式执行、跨租户接口、用户组和性能验证仍未执行。

## 2026-09-06 — 部门角色 SpEL 查询去 N+1

- 新增 `FlowOrgIntegrationService.getUserIdsByDeptAndRoleCode`，由租户内有效角色和部门关系 SQL 一次性返回候选用户；`FlowSpelService.findUsersByDeptAndRole` 不再逐用户调用 `isInDept`。
- 编译：`mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow -am -DskipTests compile`，BUILD SUCCESS。
- 契约测试：`FlowSpelServiceSecurityContractTest`、`FlowSpelTemplateSecurityContractTest` 共 3/3 通过。
- 未执行真实 MySQL 执行计划、大规模候选人性能压测和 Flowable 运行链路。

## 2026-09-06 — 本轮最终复验

- `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow -am -DskipTests compile`：BUILD SUCCESS（12.955s）。
- `mvn -f forge-framework/forge-plugin-parent/forge-plugin-flow/pom.xml -Dtest=FlowSpelTemplateSecurityContractTest,FlowSpelServiceSecurityContractTest -Dforge.test.groups= test`：3/3 通过。
- `git diff --check`：通过。
- 本轮未启动任何服务；未执行真实 MySQL/Redis/Flowable、跨租户接口、表达式运行时、性能压测、灰度和回滚演练。

## 2026-09-06 — 超时截止时间与扫描窗口优化

- `FlowTaskEventListener` 在 TASK_CREATED 阶段根据节点配置计算并写入 Flowable 原生 `dueDate`，同时保留 Forge `flow_task.due_date` 同步路径；写入失败时记录告警并保留扫描兜底。
- `FlowTimeoutServiceImpl` 的超时扫描改为 `taskDueBefore(now)` + `orderByTaskDueDate()` 分页，临近超时查询改为 `taskDueAfter(now)`/`taskDueBefore(deadline)` 时间窗口，并将提前分钟数限制在 1 分钟至 7 天。
- 编译：Flow 插件及依赖 BUILD SUCCESS（14.555s）。
- 契约测试：`FlowTaskEventListenerTimeoutContractTest` 1/1、`FlowTimeoutServiceQueryContractTest` 4/4 通过。
- 未启动 Flowable/数据库；旧任务 dueDate 回填、稳定游标、租户调度进度、通知重试与真实性能压测仍未完成。

## 2026-09-06 — 节点配置运行时租户边界

- `FlowNodeConfigMapper.selectByModelKeyAndNode` 增加 `tenantId` 参数，XML 同时约束 `sys_flow_model` 和 `sys_flow_node_config` 的租户；`FlowNodeConfigServiceImpl` 缺少可信租户时直接返回空配置，逾期提醒解析同步传递当前租户。
- 该修复避免超时扫描、任务创建和逾期提醒在 `@IgnoreTenant`/内部回调上下文中发生跨租户节点配置回查。
- 编译：Flow 插件及依赖 BUILD SUCCESS（12.725s）。
- 契约测试：`FlowNodeConfigTenantContractTest`、`FlowTimeoutServiceQueryContractTest`、`FlowTaskEventListenerTimeoutContractTest` 共 6/6 通过。
- 未执行真实多租户数据库和 Flowable 事件回调验证。

## 2026-09-06 — 节点配置协议与模型查询租户修复

- 设计器节点配置接口使用 `modelId`，运行时超时/监听器使用 `modelKey`；Mapper 新增模型 ID/Key 兼容查询，避免租户修复后设计器查询为空。
- 节点配置层级、操作权限、超时和审批人计算先通过 `selectByIdAndTenant` 校验节点配置归属；保存、更新、批量保存和删除强制当前租户。
- 流程模型分页和状态统计显式传入当前租户，XML 增加 `tenant_id` 与未删除过滤，防止 `@IgnoreTenant` 控制器泄露跨租户模型。
- `xmllint`：通过；Flow 插件 reactor compile：BUILD SUCCESS；`FlowModelMapperSqlContractTest`、`FlowNodeConfigTenantContractTest`：2/2 通过；`git diff --check`：通过。
- 未执行真实 MySQL/Redis/Flowable、跨租户接口和执行计划验证。

## 2026-09-06 — 模型 ID 管理操作租户修复

- `FlowModelController` 标记 `@IgnoreTenant`，原有模型 ID 操作依赖通用 `getById`；新增 `FlowModelMapper.selectByIdAndTenant` 并替换详情、部署、挂起、激活、启停、导出、复制路径。
- 创建、导入、复制强制写当前租户；更新校验请求租户与会话租户一致；模型分页/统计显式带租户。
- Flow 插件 reactor compile：BUILD SUCCESS；`FlowModelMapperSqlContractTest`、`FlowNodeConfigTenantContractTest`：2/2 通过；两个 Mapper XML `xmllint`：通过；`git diff --check`：通过。
- 未执行真实数据库、Flowable、跨租户 HTTP 接口和回滚演练。

## 2026-09-06 — 超时扫描 offset 漏扫修复

- 修复 `FlowTimeoutServiceImpl`：任务处理会改变活动结果集，原递增 offset 可能跳过任务；改为每轮 `listPage(0, batch)`，用 `scannedTaskIds` 去重并在无新任务时退出。
- 自动通过/拒绝后的后续任务可继续被扫描；通知失败任务不会在同一轮重复处理。
- Flow 插件 reactor compile：BUILD SUCCESS；`FlowTimeoutServiceQueryContractTest`：4/4 通过；`git diff --check`：通过。
- 未执行真实 Flowable 并发调度、数据库和多租户扫描验证。

## 2026-09-06 — 最终编译复验与测试环境说明

- 最终修改后 `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow -am -DskipTests compile`：BUILD SUCCESS。
- `xmllint`（FlowModelMapper/FlowNodeConfigMapper）与 `git diff --check`：通过。
- 直接插件契约测试在本地 Maven 缓存缺少当前工作树 `EnableStatus` 时无法编译；改用 reactor 运行测试又被仓库既有 `forge-starter-core` Surefire `groups/excludedGroups` 配置阻断。此前本轮涉及的契约测试已在相同源码范围分别通过，最后一行仅放宽更新请求缺省 tenantId 的兼容性，不改变 SQL/测试断言。
- 未启动真实 MySQL、Redis、Flowable；未执行跨租户 HTTP、并发扫描、性能压测和灰度回滚。

## 2026-09-06 — 逾期提醒重试与正式迁移

- 逾期提醒记录新增 `retry_count`、`next_retry_time`；失败后按 5/10/20/40/80 分钟退避，最多 5 次，超过后停止自动重试。
- 重试通过 `tenant_id + reminder_key + channel` 唯一键和条件更新 `claimRetry` 原子抢占；成功/失败更新复用原记录 ID，避免重复插入或把重试次数重置。
- 新增 `forge-server/db/migration/V1.0.144__add_flow_overdue_reminder_runtime.sql`，正式创建提醒记录表，补齐节点提醒配置列、重试列、重复策略字典和默认消息模板，脚本具备防重复保护。
- Mapper XML 校验通过；迁移无 Flyway `${...}` 占位符；Flow reactor compile BUILD SUCCESS；`git diff --check` 通过。
- 未执行真实 Flyway、消息服务、Redis、Flowable 和并发重试验证。

- 逾期提醒租户解析改为 fail-closed：任务租户为空时跳过发送并告警，不再默认使用租户 1；避免 `@IgnoreTenant` 扫描上下文发生错误租户投递。
- 最终 Flow reactor compile：BUILD SUCCESS；Mapper XML、迁移占位符扫描和 `git diff --check`：通过；测试类已完成 reactor `test-compile`。

## 2026-09-06 — 临时抄送撤回与逾期提醒停止语义

- 变更范围：`sys_flow_cc` 增加 ACTIVE/REVOKED 状态及撤回审计字段；新增发送人撤回接口和前端操作入口；接收方查询、表单可见性、工作台未读统计过滤已撤回关系；逾期提醒发送前增加任务可办理状态门禁。
- 迁移：新增 `forge-server/db/migration/V1.0.146__add_flow_cc_revoke_state.sql`，列和索引均使用 `information_schema` 防重复保护。
- 验证：`mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow,forge-flow/forge-flow-server -am -DskipTests compile`（JDK 22，BUILD SUCCESS）；`mvn -Penable-tests -Dtest=FlowCcRevokeContractTest,FlowOverdueReminderRetryContractTest -DfailIfNoTests=false test`（4/4 通过）；`pnpm eslint src/views/flow/cc.vue src/api/flow.js`（通过）；`git diff --check`（通过）。
- 状态展示使用新增 `flow_cc_status` 字典，迁移脚本包含字典及数据的 `NOT EXISTS` 防重复写入。
- 同步补齐超时转换时区配置 `forge.flow.timeout.time-zone`，Flow 服务和 Admin 服务默认 `Asia/Shanghai`，非法时区会回退并告警。
- 验证：Flow/Flow Server reactor compile（JDK 22，BUILD SUCCESS）；`FlowTaskEventListenerTimeoutContractTest` 与 `FlowTimeoutServiceQueryContractTest` 共 7/7 通过；未启动服务，未执行真实数据库时区和跨时区 Flowable 验证。
- 警告/跳过：未启动服务，未执行真实 MySQL/Flyway、消息渠道、Flowable 多租户接口和浏览器验收；reactor 编译存在既有 JDK 17 source/module warning，不阻断构建。

## 2026-09-06 — 管理员实例任务树与版本租户修复

- 变更范围：监控实例详情增加租户限定的管理员任务树（节点聚合、当前活动任务 ID、500 条上限和截断标记）；模型版本回退、标签更新和删除引用检查收紧租户边界及 fail-closed 语义。
- 编译：`mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow,forge-flow/forge-flow-server -am -DskipTests compile`（JDK 22，BUILD SUCCESS；存在既有 JDK 17 source/module warning）。
- 契约测试：`mvn -Penable-tests -Dtest=FlowMonitorTaskTreeContractTest,FlowTaskEventListenerTimeoutContractTest,FlowTimeoutServiceQueryContractTest -DfailIfNoTests=false test`，9/9 通过；模型版本治理测试已加入租户限定更新/运行时引用检查断言，待本轮最终测试命令完成。
- 前端检查：`pnpm eslint src/views/flow/monitor.vue src/views/flow/cc.vue src/api/flow.js`，修复任务树模板格式告警后通过。
- 静态检查：`git diff --check` 通过；V1.0.146 迁移未发现 Flyway `${...}` 占位符。
- 跳过项：未启动 MySQL、Redis、Flowable 或浏览器；未执行真实管理员权限、并行会签任务关系、数据库执行计划和灰度回滚验证。

- 追加复验：版本治理修复后首次 reactor 编译发现 `updateVersionTag` 租户变量遗漏，已修复；随后 `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow,forge-flow/forge-flow-server -am -DskipTests compile` BUILD SUCCESS，`FlowModelVersionGovernanceContractTest` 与 `FlowMonitorTaskTreeContractTest` 4/4 通过。前端 `pnpm build` BUILD SUCCESS（仅 Vite 原有 CSS 注释和动态导入提示）。

## 2026-09-06 — 候选关系表、租户锁与历史查询优化

- 变更范围：新增 `sys_flow_task_candidate` 候选关系实体/Mapper/Flyway `V1.0.147`；任务创建、动态加签/减签同步关系；候选列表和参与人判断支持关系表优先、旧逗号字段回退；所有任务动作锁定改为 `task_id + tenant_id FOR UPDATE`；审批历史改为租户限定 XML 分页查询并批量缓存用户详情。
- 编译命令：`cd forge-server && export JAVA_HOME=/Users/yaomindong/Library/Java/JavaVirtualMachines/openjdk-22/Contents/Home && export PATH="$JAVA_HOME/bin:$PATH" && mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow,forge-flow/forge-flow-server -am -DskipTests compile`。
- 编译结果：BUILD SUCCESS；存在项目既有 JDK 17 source/module、deprecated/unchecked 警告，未阻断构建。
- 契约测试命令：`cd forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow && mvn -Penable-tests -Dtest=FlowTaskMapperSqlContractTest,FlowTaskMutationAuthorizationContractTest,FlowLockMapperSqlContractTest,FlowTaskCandidateRelationContractTest -DfailIfNoTests=false test`。
- 测试结果：18/18 通过；覆盖租户锁 SQL、历史分页 SQL、候选关系回填/双读和动作授权调用约束。
- 静态检查：`xmllint --noout forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/resources/mapper/FlowTaskMapper.xml forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/resources/mapper/FlowTaskCandidateMapper.xml`、`git diff --check` 均通过；迁移脚本无 Flyway `${...}` 占位符。
- 警告与跳过项：未启动 MySQL、Redis、Flowable 或前端服务；未执行真实 Flyway 首次/中断重跑、候选集合迁移前后对照、执行计划、并发锁/幂等、历史分页 HTTP 接口和浏览器验证。本轮未启动服务，无需清理 PID。

## 2026-09-06 — 候选关系读取权限回退

- `FlowAccessGuard` 注入候选关系 Mapper，在任务详情和流程可见性判定中按 `tenant_id + task_id + candidate_type + candidate_value + status` 读取关系；迁移期间继续保留逗号字段回退。
- 契约测试新增候选关系读取边界断言，`FlowTaskMutationAuthorizationContractTest` 当前 8/8 通过；Flow/Flow Server reactor compile BUILD SUCCESS。
- 未启动服务；未执行真实候选关系表读写、跨租户接口和迁移后权限矩阵。

## 2026-09-06 — 历史审批时间轴分页 DTO/VO

- 新增 `FlowHistoryItemVO`、`FlowHistoryPageVO` 和 `/api/flow/task/history/{processInstanceId}/page`；按租户、创建时间和主键分页读取任务，页大小限制 1000，返回总数和 `hasMore`。旧 `/history/{processInstanceId}` 接口继续兼容。
- 编译：`mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow,forge-flow/forge-flow-server -am -DskipTests compile`，BUILD SUCCESS。
- 测试：`mvn -Penable-tests -Dtest=FlowTaskMutationAuthorizationContractTest,FlowTaskMapperSqlContractTest,FlowTaskCandidateRelationContractTest,FlowLockMapperSqlContractTest -DfailIfNoTests=false test`，20/20 通过。
- 前端与静态检查：`pnpm eslint src/api/flow.js`、两个 Mapper `xmllint`、`git diff --check` 均通过。
- 警告与跳过项：未启动 MySQL、Redis、Flowable 或前端服务；未执行真实分页 HTTP、并发历史一致性、数据库执行计划和浏览器切换验证。本轮未启动服务，无需清理 PID。

- 追加修正：`hasMore` 改按任务分页游标计算，发起节点仅第一页额外展示时不会把最后一页误报为可继续加载；修正后 Flow/Flow Server reactor compile BUILD SUCCESS，契约测试仍为 20/20 通过。

## 2026-09-06 — 租户回查与任务镜像更新收敛

- 变更范围：`FlowTaskServiceImpl`、`FlowTaskMapper`/XML、`FlowFormInstanceMapper`/XML、`FlowMonitorServiceImpl`、`FlowRuntimeServiceImpl`、`FlowCcServiceImpl` 及对应契约测试。
- 业务/表单详情、流程实例列表、运行态表单实例和人工抄送参与人回查统一使用显式租户参数；终结流程先锁定租户业务记录；任务镜像更新统一改为 `task_id + tenant_id` Mapper 动态更新，覆盖签收、审批、驳回、退回、委派、改派、终结、自动审批和动态加减签。
- `xmllint --noout src/main/resources/mapper/FlowTaskMapper.xml src/main/resources/mapper/FlowFormInstanceMapper.xml`：通过。
- `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow,forge-flow/forge-flow-server -am -DskipTests compile`（JDK 22）：BUILD SUCCESS；保留既有 JDK 17 source/module、deprecated/unchecked 编译警告。
- `mvn -Penable-tests -Dtest=FlowTaskMutationAuthorizationContractTest,FlowTaskMapperSqlContractTest,FlowLockMapperSqlContractTest,FlowCcSendSecurityContractTest -DfailIfNoTests=false test`：22/22 通过。
- `mvn -Penable-tests -Dtest=FlowControllerBoundaryContractTest -DfailIfNoTests=false test`：18/18 通过。
- 未启动任何服务，未产生需要清理的 PID。未验证真实 MySQL/Flyway、Flowable 内部回调租户上下文、跨租户 HTTP、并发更新/执行计划、前端浏览器和灰度回滚；这些仍是 T7.2/T7.3 的阻断项。
- 中间尝试直接编译 Flow 插件时因未带 reactor，解析到本地缓存的旧 `forge-starter-core`，出现 `EnableStatus` 缺失；改用包含依赖模块的 reactor 命令后通过，未归因于本轮代码错误。一次从模块目录执行的 `xmllint` 使用了错误相对路径，随后在模块目录用正确路径复验通过。

## 2026-09-06 — 监控固定响应 VO 与动态加签关系审计

- 监控实例列表、实例详情和实例任务树改用明确 VO：`FlowMonitorProcessInstancePageVO`、`FlowMonitorProcessInstanceDetailVO`、`FlowMonitorTaskPageVO` 及任务树子 VO；保留现有 JSON 字段，列表查询失败返回 `degraded=true` 和 `FLOW_MONITOR_INSTANCES_UNAVAILABLE`，前端据此显示错误态。
- 动态加签/减签关系补齐 `parent_task_id`、`sign_mode`、`operator_id`、`reason` 审计字段；新增 `GET /api/flow/task/{taskId}/sign-relations`，租户限定、任务参与人校验、最多 50 条；减签更新为 REVOKED 并保存操作人和原因，重复加签会刷新审计字段。
- 新增 Flyway `V1.0.148__add_flow_task_sign_relation_audit.sql`，使用 `information_schema` 防重复增加列和索引；新增前端 `flowApi.getSignRelations` 封装。
- 编译：`mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow,forge-flow/forge-flow-server -am -DskipTests compile`，BUILD SUCCESS；仅有项目既有 JDK 17 source/module、deprecated/unchecked 警告。
- 测试：`FlowMonitorBatchQueryContractTest`、`FlowMonitorTaskTreeContractTest`、`FlowTaskSignContractTest`、`FlowTaskCandidateRelationContractTest` 共 8/8 通过；收尾回归追加任务动作、超时、抄送、节点租户和管理员任务 SQL 契约，共 40/40 通过；Flow Server `FlowControllerBoundaryContractTest` 20/20 通过。
- 前端和静态检查：`npx eslint src/api/flow.js src/views/flow/monitor.vue` 通过；三个流程 Mapper `xmllint` 通过；迁移无 `${...}` 占位符；`git diff --check` 通过。
- 跳过项：未启动 MySQL、Redis、Flowable；未执行 `V1.0.148` Flyway 首次/中断重跑、前后置加签的多实例编排、跨租户 HTTP、并发关系写入、SQL 执行计划和浏览器验收。

## 2026-09-06 — 前端生产构建

- 命令：`source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm build`。
- 结果：Vite 构建通过，`built in 49.84s`；保留既有 Vite 配置、CSS 注释和无效动态导入 warning，不阻断本轮改动。
- 兼容处理：仓库 `pnpm-workspace.yaml` 当前只有构建授权配置、未声明 `packages`，构建前临时移出并在退出时恢复；`git status` 未出现该文件或 `dist` 变更。
- 跳过项：未启动浏览器、后端、MySQL、Redis 或 Flowable，未执行 HTTP E2E 和灰度演练；无本轮服务 PID 需要清理。

## 2026-09-06 — 组织递归与候选组启动预检

- 将流程组织的递归/直接子组织查询迁移到 `SysOrgMapper` XML，新增 `selectOrgAndChildrenIdsByTenant`，显式约束租户、启用状态和逻辑删除，避免在流程 Service 中使用 Lambda 构建层级 SQL。
- `FlowModelServiceImpl#getStartConfig` 增加静态 `candidateGroups` 有效成员预检，支持角色编码、角色 ID、部门 ID 和自定义流程用户组；`${...}`/`#{...}` 动态候选组保留运行时求值，预检失败返回节点级诊断。
- 编译：`mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow,forge-flow/forge-flow-server -am -DskipTests compile`，BUILD SUCCESS（JDK 22；项目既有 source/module、deprecated/unchecked 警告）。
- 契约测试：`mvn -Penable-tests -Dtest=FlowOrgIntegrationSecurityContractTest,FlowStartPreviewContractTest -DfailIfNoTests=false test`，3/3 通过。
- 静态检查：相关 Mapper `xmllint`、`git diff --check` 通过；未启动 MySQL、Redis、Flowable，未执行真实组织树和 HTTP E2E。

## 2026-09-06 — 模型版本历史清理接口

- 新增 `VersionCleanupDTO`、`VersionCleanupVO`、`FlowModelVersionService.cleanupVersions` 和 `POST /api/flow/model/version/cleanup`。
- 清理候选版本在当前租户范围内按版本/时间/ID稳定排序并 `FOR UPDATE` 锁定，默认保留最近 10 个（允许 1-100）；发布、废弃、当前模型版本和仍有运行中实例引用的版本分别计数跳过，其他版本执行租户限定逻辑删除。
- 新增 `FlowModelVersionCleanupContractTest`，覆盖租户锁、引用检查、保护条件和 DTO 写接口；待最终编译与契约测试执行。

## 2026-09-06 — 本轮最终验证

- `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow,forge-flow/forge-flow-server -am -DskipTests compile`：BUILD SUCCESS（JDK 22；仅保留既有 source/module、deprecated/unchecked 警告）。
- `mvn -Penable-tests -Dmaven.main.skip=true -Dtest=FlowOrgIntegrationSecurityContractTest,FlowStartPreviewContractTest,FlowModelVersionCleanupContractTest,FlowModelVersionGovernanceContractTest -DfailIfNoTests=false test`：6/6 通过。
- `xmllint --noout`（SysOrgMapper、FlowModelVersionMapper）、迁移占位符扫描、`git diff --check`：通过。
- `npx eslint src/api/flow.js src/api/version.js src/views/flow/userGroup.vue src/views/flow/version.vue`：通过；仅有 npm 用户配置弃用 warning。
- reactor 测试入口仍受仓库既有 `forge-starter-core` Surefire groups/excludedGroups 配置阻断；目标契约测试使用已完成 reactor 编译产物并成功执行。未启动 MySQL、Redis、Flowable，未做真实 Flyway/HTTP E2E、并发和执行计划验证。

## 2026-09-06 — 模型目录批量排序与权限迁移

- 变更：新增 `V1.0.151__add_flow_model_sort_order.sql`，为 `sys_flow_model` 增加 `sort_order` 字段和 `(tenant_id, del_flag, sort_order, create_time, id)` 索引，并按迁移前创建时间倒序回填；新增 `V1.0.152__add_flow_model_sort_permission.sql`，独立注册 `flow:model:sort` 与 `flow:model:sort:api`，不自动写入角色权限。
- 后端：`FlowModelSortDTO`/`FlowModelSortItemDTO`、Mapper 租户 `FOR UPDATE` 锁定和租户限定更新、`FlowModelService.sortModels` 校验重复/范围/跨租户及并发更新，`POST /api/flow/model/sort` 使用明确 DTO 和独立权限。
- 前端：`flowApi.sortModels`；`/flow/model` 增加拖动排序模式、分页锁定、保存锁、错误反馈和刷新。
- 编译：`mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow,forge-flow/forge-flow-server -am -DskipTests compile`：BUILD SUCCESS（JDK 22；既有 JDK 17 source/module、deprecated/unchecked warning）。
- 契约测试：Flow Server `FlowModelVersionControllerContractTest,FlowNodeConfigControllerContractTest,FlowControllerBoundaryContractTest`：22/22；Flow 插件 `FlowModelSortingContractTest`：3/3。
- 前端：`npx eslint src/api/flow.js src/views/flow/model.vue` 通过；`npm run build --if-present` Vite production build 通过（34.17s）。按标准尝试 `pnpm build` 时因仓库既有 `pnpm-workspace.yaml` 缺少 `packages` 字段被拒绝，未修改该配置。
- 静态检查：`FlowModelMapper.xml` xmllint、V1.0.151/V1.0.152 Flyway 占位符扫描、`git diff --check` 均通过。
- 警告/跳过项：未启动 MySQL、Redis、Flowable；未执行真实 Flyway 首次/重跑/中断恢复、排序并发事务、执行计划、跨租户 HTTP E2E 和浏览器拖动验收；本轮未启动服务，无需清理 PID。

## 2026-09-06 — 任务动作与候选关系幂等凭证

- 变更范围：`FlowTaskActionDTO`/`FlowTaskController`/`FlowTaskService`/`FlowTaskServiceImpl`/`FlowTaskActionAuthorization`；候选关系实体、Mapper/XML；新增 Flyway `V1.0.153__add_flow_task_sign_idempotency.sql`。
- 行为：转办接口透传 `idempotencyKey/requestDigest`，首次成功写入 `DELEGATE` 动作凭证；原操作者通过 owner 重试同一凭证直接返回成功，复用不同摘要返回 `FLOW_TASK_IDEMPOTENCY_CONFLICT`。动态加签/减签在任务租户锁内读取候选关系幂等凭证，重复同载荷不重复调用 Flowable，冲突或缺少关系持久化能力时拒绝。
- 编译命令：`cd forge-server && export JAVA_HOME=/Users/yaomindong/Library/Java/JavaVirtualMachines/openjdk-22/Contents/Home && export PATH="$JAVA_HOME/bin:$PATH" && mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow,forge-flow/forge-flow-server -am -DskipTests compile`。
- 编译结果：BUILD SUCCESS（约 83 秒；JDK 17 source/module、deprecated/unchecked 为项目既有警告）。
- 契约测试：`cd forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow && mvn -Penable-tests -Dmaven.main.skip=true -Dtest=FlowTaskSignContractTest -DfailIfNoTests=false test`，3/3 通过。
- 静态检查：`xmllint --noout forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/resources/mapper/FlowTaskCandidateMapper.xml`、V1.0.153 Flyway `${...}` 扫描、`git diff --check` 均通过。
- 警告与跳过：未启动 MySQL、Redis、Flowable 或前端；未执行真实 Flyway 首次/重跑、中断恢复、并发动作、跨租户 HTTP 和浏览器验证；无本轮服务 PID 需要清理。

## 2026-09-06 — 超时扫描跨实例互斥复验

- `mvn -Penable-tests -Dmaven.main.skip=true -Dtest=FlowTimeoutServiceQueryContractTest -DfailIfNoTests=false test`：6/6 通过，覆盖 Redis 锁键、`SETNX` 租约、单 JVM 降级、租户上下文、`due_date + id` 稳定游标和 due date 窗口查询。
- Flow/Flow Server reactor compile：同轮 `BUILD SUCCESS`；仅有项目既有 JDK 17 source/module、deprecated/unchecked 警告。
- `git diff --check` 和 FlowTaskCandidateMapper `xmllint`：通过。
- 未启动 Redis/MySQL/Flowable，未执行真实多实例锁竞争和租约过期测试；该项保留至 T7.2/T7.3。

## 2026-09-06 — 流程图连线状态与降级提示

- 变更范围：新增 `ProcessSequenceFlowInfo`；扩展 `ProcessDiagramInfo` 的 `sequenceFlows`、`sequenceFlowStatusAvailable`、`sequenceFlowStatusMessage`；`FlowTaskServiceImpl` 基于 BPMN `SequenceFlow` 定义和 Flowable 历史 `sequenceFlow` 活动计算状态；`ProcessDiagramViewer.vue` 增加连线样式和不可用提示。
- 语义：历史确实记录 sequenceFlow 活动时返回所有连线的已执行/待执行状态；未记录时返回空列表并标记不可用，前端展示原因，避免误报。
- 编译：Flow/Flow Server reactor compile BUILD SUCCESS（JDK 22；保留项目既有 source/module、deprecated/unchecked 警告）。
- 契约测试：`FlowProcessDiagramSequenceContractTest`、`FlowTaskSignContractTest`、`FlowTimeoutServiceQueryContractTest` 共 10/10 通过。
- 前端：`npx eslint src/components/bpmn/ProcessDiagramViewer.vue` 通过；生产构建命令已执行，结果待收尾回填。
- 跳过项：未启动真实服务、数据库、Redis、Flowable 或浏览器；未执行历史级别配置下的真实连线高亮、跨租户 HTTP 和灰度回滚。

## 2026-09-06 — 前端动作幂等凭证接入

- `forge-admin-ui/src/views/flow/todo.vue`：审批、驳回、退回、批量快捷审批和转办请求统一生成 `idempotencyKey`；优先通过 Web Crypto 计算 SHA-256 `requestDigest`，无 Web Crypto 时使用受限 FNV 降级摘要；请求锁仍由现有 loading 状态控制。
- 前端验证：`npx eslint src/views/flow/todo.vue src/components/bpmn/ProcessDiagramViewer.vue` 通过；`npm run build --if-present` BUILD SUCCESS（42.04s）。
- 后端 DTO 清理：移除 `FlowTaskApproveDTO`/`FlowTaskRejectDTO` 重复声明，统一继承 `FlowTaskActionDTO` 字段。
- 跳过项：未启动浏览器、Flowable、MySQL、Redis；未执行真实 HTTP 重试和多标签页并发请求。

## 2026-09-06 — 超时锁原子释放与租约续期

- `FlowTimeoutServiceImpl`：Redis 锁释放改为 compare-and-delete Lua 脚本；扫描分页前使用 compare-and-pexpire 续租；Redis 降级锁记录是否真实持有 Redis 租约，避免恢复期间误删其他实例的锁。
- `FlowTimeoutServiceQueryContractTest`：新增原子释放、续租和不再使用 GET/DELETE 组合的静态断言。
- 验证：Flow 插件 reactor compile（JDK 17）BUILD SUCCESS；`npx eslint src/views/flow/started.vue src/views/flow/todo.vue src/utils/flow-action-idempotency.js` 通过。
- 跳过项：未启动 Redis，未执行真实租约过期、续租失败和多实例竞争。

## 2026-09-06 — 多实例动态加签执行关系

- BPMN 已配置多实例的用户任务使用 Flowable 原生 `addMultiInstanceExecution/deleteMultiInstanceExecution`；候选关系保存 `child_task_id/child_execution_id`，减签按子执行撤销；普通任务保持候选关系兼容路径。
- 新增 Flyway `V1.0.154__add_flow_task_sign_runtime_relation.sql` 和 `V1.0.155__seed_flow_sign_mode_dict.sql`，均采用信息架构/`NOT EXISTS` 防重复保护。
- 发起流程详情增加模式选择、动态关系审计列表和幂等凭证；动作凭证抽取到 `src/utils/flow-action-idempotency.js`，待办和发起详情复用。
- 验证：`mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow -am -DskipTests compile`（OpenJDK 17）BUILD SUCCESS；前端 ESLint 通过；Mapper XML 语法和 `git diff --check` 待本轮收尾执行。
- 跳过项：未启动 MySQL/Flyway、Redis、Flowable；未执行真实多实例顺序/并行、并发加减签、跨租户 HTTP 和浏览器验收。

## 2026-09-06 — 本轮收尾验证补记

- `mvn -Penable-tests -Dmaven.main.skip=true -Dtest=FlowTaskSignContractTest,FlowTimeoutServiceQueryContractTest,FlowProcessDiagramSequenceContractTest -DfailIfNoTests=false test`：10/10 通过。
- `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow,forge-flow/forge-flow-server -am -DskipTests compile`（OpenJDK 17）：BUILD SUCCESS；项目既有 deprecated/unchecked 警告不阻断。
- `npm run build --if-present`（Node 20.19.0）：Vite production build 通过（44.62s）；保留项目既有 Vite native config、CSS 注释和动态导入 warning。
- `xmllint --noout`（FlowTaskCandidateMapper.xml、FlowTaskMapper.xml）、V1.0.154/V1.0.155 Flyway 占位符扫描、`git diff --check`：通过。
- 未启动 MySQL、Redis、Flowable、浏览器；真实多实例执行、Redis 租约竞争、Flyway 重跑/中断恢复和跨租户 HTTP 仍属于 T7.2/T7.3 集成门禁。

## 2026-09-06 — 流程固定字段响应 VO 收敛与表达式安全加固

- 后端接口：监控历史活动 `/activities/{processInstanceId}`、当前任务 `/current-tasks/{processInstanceId}`、模型状态统计 `/model/statistics`、模型版本摘要 `/model/{modelKey}/versions` 和抄送未读 `/cc/unread/count` 改用明确 VO；字段名称与既有响应保持兼容。新增 `FlowMonitorActivityVO`、`FlowMonitorCurrentTaskVO`、`FlowModelStatisticsVO`、`FlowModelVersionSummaryVO`、`FlowCcUnreadCountVO`。
- 表达式安全：节点配置和条件规则移除 `StandardEvaluationContext`，统一使用 `FlowSafeExpressionEvaluator`；限制长度 2000，剥离 `${...}`/`#{...}` 外壳，拒绝类型/Bean/构造器/反射危险链；SPEL 组织查询日志降为不携带业务标识的 debug/warn。
- 编译：`cd forge-server && export JAVA_HOME=/Library/Java/JavaVirtualMachines/openjdk-17.jdk/Contents/Home && export PATH="$JAVA_HOME/bin:$PATH" && mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow -am -DskipTests compile`：BUILD SUCCESS；仅保留项目既有 deprecated/unchecked 警告。
- 契约测试：Flow 插件 `FlowSafeExpressionEvaluatorTest,FlowExpressionSecurityContractTest,FlowSpelServiceSecurityContractTest,FlowUserGroupRuntimeResolutionContractTest`：6/6 通过；Flow Server `FlowControllerBoundaryContractTest`：24/24 通过。
- 前一次边界测试补充：Flow Server 变量 DTO 边界测试已在同类测试中通过；未启动 MySQL、Redis、Flowable、浏览器，未执行真实表达式运行时、HTTP E2E、跨租户矩阵和灰度回滚。

## 2026-09-06 — 动态加签模式 fail-closed

- Flowable 当前原生动态多实例路径只能保证并行关系；服务端拒绝 `BEFORE`/`AFTER`，返回稳定 `FLOW_TASK_SIGN_MODE_UNSUPPORTED`，避免把审计模式误报为严格前置/后置执行。
- 发起流程加签弹窗保留字典展示但禁用不可执行模式，并显示并行能力提示；`PARALLEL` 默认行为不变。
- 验证：Flow 插件 `FlowTaskSignContractTest,FlowSafeExpressionEvaluatorTest,FlowExpressionSecurityContractTest`：6/6 通过；`npx eslint src/views/flow/started.vue` 通过；插件 reactor compile BUILD SUCCESS。
- 未启动 Flowable 实例、Redis、MySQL 或浏览器，真实多实例顺序语义和 HTTP 错误码渲染仍属于 T7.2/T7.3 集成门禁。

## 2026-09-06 — 本阶段收尾回归

- Flow/Flow Server reactor compile：`mvn -pl forge-flow/forge-flow-server -am -DskipTests compile`，BUILD SUCCESS；仅有项目既有 deprecated/unchecked 警告。
- Flow 插件增量回归：任务动作、候选关系、超时扫描、流程图连线、组织预检、模型治理、监控树和表达式安全共 49/49 通过；Flow Server 控制器边界、模型版本和节点配置契约共 27/27 通过。
- 静态检查：`git diff --check`、流程 Mapper `xmllint`、迁移占位符扫描通过；`npx eslint src/views/flow/started.vue` 通过（仅 npm 用户配置弃用 warning）。
- 未启动 MySQL、Redis、Flowable、浏览器；真实 Flyway 重跑/中断恢复、跨租户 HTTP、并发锁/幂等、Flowable 多实例顺序语义和灰度/回滚演练仍归入 T7.2/T7.3 门禁。

## 2026-09-06 — 运营分页响应继续收敛

- 错误日志分页、流程表单分页、表达式模板分页分别改用明确 `FlowErrorLogPageVO`、`FlowFormPageVO`、`FlowSpelTemplatePageVO`；错误日志查询失败不再伪装为空，返回 `degraded=true` 和稳定错误码。
- 验证：Flow/Flow Server reactor compile BUILD SUCCESS；`FlowControllerBoundaryContractTest` 26/26 通过；前端 `npm run build --if-present` BUILD SUCCESS（44.56s）。保留既有 Vite native config、CSS 注释和动态导入 warning。
- 未启动 MySQL、Redis、Flowable、浏览器；真实 HTTP 错误态、跨租户矩阵、数据库执行计划和灰度回滚仍待 T7.2/T7.3。

## 2026-09-06 — 抄送搜索与未读统计补强

- 抄送 `/my`、`/sent` 接口接入 `title` 查询参数。服务层改用 Mapper XML 的租户限定分页查询，搜索标题、内容摘要、流程定义 Key 和业务 Key，并以 `cc_time + id` 稳定排序；搜索词限制为 100 个字符，避免无界模糊查询放大数据库压力。
- 未读数量改用 `countWorkspaceUnread(userId, tenantId)` 聚合，显式过滤有效状态 `status = 0`；撤回抄送不会继续显示为接收人的未读数量。
- 验证：Flow/Flow Server reactor compile BUILD SUCCESS；`FlowCcSecurityContractTest`、`FlowCcSendSecurityContractTest`、`FlowCcRevokeContractTest` 3/3 通过；FlowCcMapper XML 由编译资源校验；未启动 MySQL、Redis、Flowable 或浏览器，真实分页执行计划和跨租户 HTTP 仍待 T7.2/T7.3。

## 2026-09-06 — 抄送全部已读闭环

- 新增 `POST /api/flow/cc/read/all`，由 `FlowCcMapper.markAllRead` 在 SQL 中同时约束 `tenant_id`、`cc_user_id`、`status = 0` 和 `is_read = 0`，避免前端按当前分页逐条提交造成“全部已读”名不副实。
- 单条/批量已读增加有效关系状态条件；单条更新 0 行时返回 `FLOW_CC_NOT_VISIBLE`，撤回抄送不会被接收人重新写入阅读状态。
- 前端抄送页调用全量接口并显示服务端返回的更新条数；`npx eslint src/views/flow/cc.vue src/api/flow.js` 通过。
- 验证：Flow/Flow Server reactor compile BUILD SUCCESS；Flow 插件 CC 契约 3/3、Flow Server `FlowControllerBoundaryContractTest` 28/28 通过；未启动 MySQL、Redis、Flowable 或浏览器，真实批量更新锁竞争与跨租户 HTTP 仍待 T7.2/T7.3。
- 本轮新增前端页面接入验证：候选组配置支持流程用户组，用户组成员搜索使用租户限定用户分页接口；详见后续增量记录。

## 本轮增量验证（2026-09-06，用户组页面与候选组配置接入）

- 变更范围：`ApproverAssigneeForm.vue` 的候选组下拉同时加载当前租户启用的系统角色和流程用户组；`flow.js` 的流程用户组成员搜索改用现有 `/system/user/page`；`userGroup.vue` 兼容分页用户响应，修复成员选择列表为空的问题。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && npx eslint src/components/flow-designer/panel/ApproverAssigneeForm.vue src/views/flow/userGroup.vue src/api/flow.js`：通过。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && npm run build --if-present`：BUILD SUCCESS（Vite 1m58s）。保留项目既有 Vite native config、动态导入和 UnoCSS plugin timing warning，不阻断本轮变更。
- `git diff --check -- forge-admin-ui/src/components/flow-designer/panel/ApproverAssigneeForm.vue forge-admin-ui/src/views/flow/userGroup.vue forge-admin-ui/src/api/flow.js`：通过。
- 可见行为：设计器审批人配置的“候选角色或用户组”可选择 `groupCode`；用户组成员弹窗可按当前租户启用用户搜索。流程用户组菜单仍依赖 `V1.0.149` Flyway 执行及对应权限刷新。
- 跳过项：未启动真实 Admin/Flow 服务、MySQL、Redis、Flowable 或浏览器；未执行用户组跨租户 HTTP 矩阵和真实 BPMN 部署运行验收。

## 本轮增量验证（2026-09-06，用户组搜索关键字修复）

- 后端 `FlowUserGroupQuery` 新增 `keyword`，Mapper 按 `group_code OR group_name` 查询，避免前端同时传编码和名称造成 AND 过滤后结果为空。
- `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow -am -DskipTests compile`：BUILD SUCCESS（OpenJDK 17）。
- `mvn -Penable-tests -Dmaven.main.skip=true -Dtest=FlowUserGroupGovernanceContractTest -DfailIfNoTests=false test`：3/3 通过。
- 前端构建和 ESLint结果沿用上一条用户组页面接入记录；未启动真实数据库、服务和浏览器，未执行实际用户组搜索 HTTP 验收。

## 本轮缺陷修复（2026-09-06，模型排序 MySQL 锁查询）

- 根因：`FlowModelMapper.selectByIdsForUpdate` 同时包含排序和行锁，经过当前 MyBatis-Plus/JSqlParser 租户拦截器改写后，实际 SQL 变成 `FOR UPDATE ORDER BY`，MySQL 报 `SQLSyntaxErrorException`。
- 修复：锁定待排序模型时移除无业务意义的 `ORDER BY m.id ASC`，保留租户、逻辑删除、ID 集合条件和 `FOR UPDATE`；排序值仍由服务层按请求顺序写入。
- `xmllint --noout forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/resources/mapper/FlowModelMapper.xml`：通过。
- `mvn -Penable-tests -Dmaven.main.skip=true -Dtest=FlowModelSortingContractTest -DfailIfNoTests=false test`：3/3 通过。
- `git diff --check`：通过。未启动真实 MySQL，需在目标环境重新点击“保存排序”验证实际锁 SQL 已正常执行。

## 本轮缺陷修复（2026-09-06，待办审批详情 `emitsOptions`）

- 根因：待办详情中的 `FlowBusinessForm` 通过 `import.meta.glob` 异步加载业务表单；任务快速切换、关闭详情或表单 URL 变化时，旧 loader 仍会写回 `formComponent`，并可能让 Vue 动态组件在空/失效 vnode 上更新，触发 `Cannot read properties of null (reading 'emitsOptions')`。
- 修复：为每次加载分配递增代次；在卸载和 URL 变化后丢弃过期结果；只在当前代次更新 `loading/loadError/formComponent`；校验模块是否为可渲染 Vue 组件；使用 `markRaw` 和基于 URL/代次的稳定 `key` 渲染动态组件。
- 回归测试：`npm test -- --run src/components/common/__tests__/FlowBusinessForm.spec.js src/components/flow/__tests__/FlowReadonlyFormPanel.spec.js`，6/6 通过，新增无效模块导出不渲染动态节点的断言。
- ESLint：`npx eslint src/components/common/FlowBusinessForm.vue src/components/common/__tests__/FlowBusinessForm.spec.js`，通过，0 error、0 warning。
- 构建：`npm run build --if-present`，Vite production build 成功（最后一次约 1 分 21 秒，前一次约 2 分 12 秒）；仅有仓库既有配置、动态导入和 UnoCSS plugin timing warning。
- 静态检查：`git diff --check` 通过。
- 跳过项：未启动 MySQL、Redis、Flowable 或浏览器；真实待办 HTTP、跨页面切换和生产数据表单路径仍待集成环境复验；本轮未启动服务，无 PID 需要清理。

## 本轮缺陷修复（2026-09-06，流程模型排序 `update_by` 列不存在）

- 根因：`sys_flow_model` 表沿用 `last_update_by` 记录最后修改人，实体、初始化 SQL 和查询均使用该列；排序更新 Mapper 新增时误写 `update_by`，点击保存排序才触发 MySQL `Unknown column 'update_by'`。
- 修复：`FlowModelMapper.xml:updateSortOrder` 改为 `last_update_by = #{updateBy}`；排序契约增加按完整字段行匹配的断言，防止 `last_update_by` 被字符串子串检查误判。
- 静态检查：`xmllint --noout forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/resources/mapper/FlowModelMapper.xml`、`git diff --check` 均通过。
- 编译：`export JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home && mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow -am -DskipTests compile`，BUILD SUCCESS。
- 契约测试：`mvn -Penable-tests -Dmaven.main.skip=true -Dtest=FlowModelSortingContractTest -DfailIfNoTests=false test`，3/3 通过。
- 环境说明：先以 Java 8 执行时因 `invalid target release: 17` 失败，已切换 OpenJDK 17 重跑并通过；未启动真实 MySQL/Flyway，目标环境需重新点击保存排序做最终 SQL 验收。
- 参数命名收敛后追加直接插件测试仍为 3/3 通过；一次从 `forge-server` 根目录串行执行的测试被仓库既有 `forge-starter-core` Surefire `groups/excludedGroups` 配置阻断，未归因于本次 Mapper 修复。

## 本轮继续开发（2026-09-06，用户组公共组件和分页参数修复）

- 用户组页面改用项目公共 `AiCrudPage`、`SystemTableCell`、`DictTag`，统一搜索、表单、表格和操作区样式，移除页面自定义 CRUD 外壳。
- 成员维护改用公共 `UserSelectModal`，复用组织树、分页和租户用户查询；确认时按字符串 ID 计算新增/移除成员，避免雪花 ID 转 Number 造成精度丢失。
- `FlowUserGroupMapper.selectPageByTenant` 增加 `@Param("keyword")`，Service 转发 `FlowUserGroupQuery.keyword`，消除 XML 引用未声明参数导致的 MyBatis `BindingException`。
- 补充 Mapper 参数契约测试，覆盖 Java 参数声明和 Service 传参。

## 本轮追加验证（2026-09-06，用户组公共组件和分页参数修复）

- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && npx eslint src/views/flow/userGroup.vue src/api/flow.js`：通过。
- `npm run build --if-present`（Node 20.19.0）：Vite production build 通过（约 1 分 11 秒）；保留仓库既有 Vite native config、无效动态导入和 UnoCSS plugin timing warning。
- `xmllint --noout forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/resources/mapper/FlowUserGroupMapper.xml`：通过。
- `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow -am -DskipTests compile`（OpenJDK 17）：BUILD SUCCESS。
- `mvn -Penable-tests -Dmaven.main.skip=true -Dtest=FlowUserGroupGovernanceContractTest -DfailIfNoTests=false test`：4/4 通过，新增 Mapper `keyword` 参数声明和 Service 转发契约。
- `git diff --check`：通过。
- 未启动 MySQL、Redis、Flowable 或浏览器；真实用户组接口、雪花 ID 成员增删、Flyway 执行和跨租户 HTTP 仍需 T7.2/T7.3 集成门禁。

## 本轮追加修复（2026-09-06，选人公共组件启用用户过滤）

- 公共 `UserSelectModal` 增加可选 `userStatus` 参数，用户组成员选择明确限制为启用用户，仍复用既有组织树、分页和表格样式。
- `npx eslint src/components/common/UserSelectModal.vue src/views/flow/userGroup.vue src/api/flow.js`：通过。
- `npm run build --if-present`（Node 20.19.0）：Vite production build 通过（约 1 分 34 秒）；保留仓库既有 Vite native config、无效动态导入和 UnoCSS plugin timing warning。
- `git diff --check`：通过。

## 本轮追加修复（2026-09-06，用户组允许清空成员）

- 公共 `UserSelectModal` 增加可选 `allowEmpty`，用户组成员可确认空选择以移除全部成员；其他调用方默认行为不变。
- `npx eslint src/components/common/UserSelectModal.vue src/views/flow/userGroup.vue src/api/flow.js`：通过。
- `npm run build --if-present`（Node 20.19.0）：Vite production build 通过（约 1 分 5 秒）；保留仓库既有 Vite native config、无效动态导入和 UnoCSS plugin timing warning。
- `git diff --check`：通过。

## 2026-09-06 — 修复 Maven Surefire 无测试引擎模块初始化失败

- 根因：根 POM 的 Surefire `groups`/`excludedGroups` 对所有 reactor 模块生效；没有 JUnit/TestNG 引擎的依赖模块在测试阶段初始化即失败。
- 修复：根 POM 统一继承 BOM 管理的 `org.junit.jupiter:junit-jupiter-engine`（test scope），保留现有 `dev`/`exclude` 标签过滤语义；依赖-only 模块可安全跳过。
- `mvn -Penable-tests -pl forge-framework/forge-starter-parent/forge-starter-api-config -DskipTests=false test`：BUILD SUCCESS，验证无测试源码模块不再报 provider 错误。
- `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow -am -Penable-tests -Dtest=FlowUserGroupGovernanceContractTest -Dforge.test.groups= -Dsurefire.failIfNoSpecifiedTests=false test`：BUILD SUCCESS，4/4 通过。
- `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow -am -DskipTests compile`：BUILD SUCCESS（OpenJDK 17）；仅保留既有 deprecated/unchecked 警告。
- 额外尝试的 `forge-starter-file`/`forge-starter-idempotent` 测试受本机 Mockito inline Byte Buddy agent attach、Sa-Token 上下文限制失败，未归因于本修复。
- 未启动 MySQL、Redis、Flowable 或浏览器；无本轮服务 PID 需要清理。

## 本轮追加验证（2026-09-07，默认打包跳过测试编译）

- 首次全工程 `mvn package` 虽跳过 Surefire，但仍因父编译器配置覆盖了 `maven.test.skip` 而编译测试源码；`forge-plugin-generator` 既有测试存在编码和构造器不匹配错误。
- 修复根 POM 编译器配置：主源码使用 `skipMain=${forge.compiler.skip}`，测试源码使用 `skip=${maven.test.skip}`；默认 `maven.test.skip` 跟随 `forge.tests.skip=true`，`enable-tests` profile 显式恢复为 `false`。
- `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am package`（OpenJDK 17）：BUILD SUCCESS；测试资源未复制、测试源码未编译、Surefire 输出 `Tests are skipped.`。
- `mvn package`（forge-server 全工程，OpenJDK 17）：BUILD SUCCESS，51/51 模块成功，所有测试阶段均跳过且应用 JAR 正常 repackage。
- `git diff --check`、`xmllint --noout forge-server/pom.xml`：通过。
- 显式测试入口保持可用；本轮未启动 MySQL、Redis、Flowable 或浏览器，未改变既有测试失败和集成门禁结论。

- 回归显式测试入口：`mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow -am -Penable-tests -Dtest=FlowUserGroupGovernanceContractTest -Dforge.test.groups= -Dsurefire.failIfNoSpecifiedTests=false test`，4/4 通过；日志确认测试源码在该 profile 下正常编译并执行。

## 2026-09-07 — 默认打包跳过测试用例

- 用户反馈 `FileManagerTest` 因缺少 Sa-Token 请求上下文导致打包失败，要求打包阶段不执行测试。
- 根 POM `forge.tests.skip` 默认值改为 `true`；`enable-tests` profile 继续覆盖为 `false`，因此默认 `package/install` 跳过 Surefire 测试，显式 `-Penable-tests` 仍可执行测试。
- `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow -am package`（OpenJDK 17）：BUILD SUCCESS；各模块输出 `Tests are skipped.`，流程插件 JAR 生成成功。
- `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow -am -Penable-tests -Dtest=FlowUserGroupGovernanceContractTest -Dforge.test.groups= -Dsurefire.failIfNoSpecifiedTests=false test`：BUILD SUCCESS，4/4 通过。
- `git diff --check`、`xmllint --noout forge-server/pom.xml`：通过。
- 未启动 MySQL、Redis、Flowable 或浏览器；无本轮服务 PID 需要清理。
