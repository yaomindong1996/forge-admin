# Flow Management Hardening Test Spec

## 本轮增量验证（2026-09-05）

范围：Phase 0、Phase 1 的访问边界、撤回 fail-open、催办授权与动作状态校验。

必跑命令：

```bash
git diff --check
cd forge-server && mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow,forge-flow/forge-flow-server -am test
```

重点验证：

- 跨租户 taskId/processInstanceId 读取被拒绝。
- 非发起人或缺失发起人信息时撤回被拒绝。
- 无权限用户催办、委派、转派、终止和删除被拒绝。
- 重复审批请求不会重复推进流程。

跳过项：需要运行 MySQL、Redis、Flowable 外部服务的端到端接口验证，待本地依赖可用后补跑。

## 执行结果（2026-09-05）

- 流程插件编译通过。
- 流程插件测试已启动，静态 SQL 合同及枚举测试通过；其余 Mockito 测试受当前 JDK 环境无法进行 Byte Buddy agent attach 影响而失败，需在允许 agent attach 的 CI/开发机重跑。
- 前端使用 `npm run build` 完成生产构建；仓库的 `pnpm` workspace 配置当前报 `packages field missing or empty`，因此采用等价 npm 脚本验证。

## 增量验证（2026-09-05，继续执行）

- 监控批量任务摘要和高影响任务动作授权契约测试已加入并完成 test-compile。
- 待办、已办、我发起、抄送、监控页面定向 ESLint 通过。
- 前端生产构建通过。

## 增量验证（2026-09-05，抄送安全边界）

- 新增 `FlowCcSecurityContractTest`，约束抄送列表、未读统计和读状态变更必须带租户条件；读状态变更必须限制当前接收人；详情查询必须限制接收人或发送人。
- 已完成 Flow 插件 `test-compile -DskipTests`，测试类编译成功。
- 尚未执行依赖真实登录会话、数据库和 Flowable 的抄送端到端越权验证，待集成环境可用后补跑。

## 增量验证（2026-09-05，统计聚合与管理员任务上下文）

- 新增 `FlowBusinessStatsMapperSqlContractTest`，约束流程实例统计使用 `SUM`/`AVG` 聚合，并同时限制租户与流程定义。
- 管理员监控详情接入实例任务分页接口，前端显示任务节点、处理人、状态和时间字段，接口失败提供局部重试。

## 增量验证（2026-09-05，模型执行节点预检）

- `FlowModelDeploymentValidationContractTest` 已扩展，约束部署前调用执行节点/网关条件校验，并覆盖不支持节点、缺失审批人和缺失网关条件的诊断文案。
- Java 17 下 Flow 插件 compile 与 test-compile 均通过；前端监控页定向 ESLint 通过。
- 使用 `-Dforge.test.groups=` 定向执行 `FlowCcSecurityContractTest`、`FlowBusinessStatsMapperSqlContractTest`、`FlowModelDeploymentValidationContractTest`：3 个测试全部通过。

## 增量验证（2026-09-05，动态加签/减签）

- 新增 `FlowTaskSignContractTest`，约束加签/减签必须经过任务参与人、租户和目标用户校验，使用 Flowable 候选用户 API，并写入评论审计。

## 增量验证（2026-09-05，人工抄送发送安全）

- 人工抄送接口改用会话绑定的 `sendCcByCurrentUser`；流程回调仍使用内部发送入口，避免通知回调因缺少 HTTP 会话被误拒绝。
- 新增 `FlowCcSendSecurityContractTest`，约束人工发送必须校验当前用户、流程实例租户/业务归属、任务参与关系和抄送目标用户租户可用性。
- Flow 插件 `test-compile -DskipTests`：通过（28 个测试类编译成功）。
- `FlowCcSendSecurityContractTest`：1 个测试通过。
- 尚未执行真实数据库、Flowable 和登录会话下的跨租户人工抄送接口测试，待集成环境可用后补跑。

## 增量验证（2026-09-05，启动前节点预览）

- `FlowStartPreviewContractTest` 通过；启动配置响应增加 `nextNodes`，包含首批可达用户任务及 assignee/candidateUsers/candidateGroups 策略。
- Flowable serviceTask 抄送回调使用 activityId 时允许内部已验证参与人继续发送，人工 `/cc/send` 仍拒绝不存在的真实任务 ID。

## 增量验证（2026-09-06，任务状态流转修复）

- 修复流程终结后 Flowable 删除事件将活动任务统一记为 `CANCELED` 的问题；流程级终结和任务级终结现在会把同一租户、同一流程实例中处于待办/已签收/删除后取消状态的任务统一修正为 `TERMINATED`。
- 新增 `FlowTaskStatusTransitionContractTest`，约束 Mapper 批量状态修复 SQL 以及任务服务、实例服务的终结调用。
- Java 17 Flow/Flow Server 依赖编译通过；状态契约、抄送授权、启动前预览 3 个定向测试全部通过。
## 本轮增量验证（2026-09-06）

范围：Phase 6 T6.3 模型与监控管理员操作的前端提交锁、确认反馈、请求编号和成功后刷新。

必跑命令：

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh && nvm use v20.19.0
npx eslint src/views/flow/model.vue src/views/flow/monitor.vue
NODE_OPTIONS=--max-old-space-size=8192 pnpm build
```

验收重点：

- 模型发布、复制、挂起、激活、删除和发起测试在请求期间不能重复提交。
- 监控清理、删除、挂起、激活、终止、回退、转派、错误重试和标记解决在请求期间互斥。
- 成功后刷新列表/统计/当前实例上下文；异常消息保留服务端 requestId（如有）。

执行说明：仓库现有 `pnpm-workspace.yaml` 未声明 `packages`，直接执行 `pnpm build` 会报 `packages field missing or empty`；本轮临时移出该配置文件后执行等价 `pnpm build`，构建完成后已恢复原文件。

结果：ESLint 0 errors、0 warnings；前端生产构建通过（Vite built in 55.69s）。未启动真实服务，未执行浏览器连续点击、权限变更和接口超时场景。

## 增量验证（2026-09-06，启动前审批人预检）

- `FlowStartConfig` 新增 `preflightPassed`/`diagnostics`，启动配置服务对首批可达用户任务检查静态审批人策略；发起人自选节点作为合法例外。
- 模型测试弹窗展示后端返回的节点级审批人缺失诊断，不把组织解析失败伪装成空流程。
- 首次全量编译发现工作树中 `CaptchaServiceImpl` 缺失 `Profiles` 导入并已补齐；修复后 Flow/Flow Server reactor compile 通过。
- 已完成前端 ESLint 与 `git diff --check`；真实 Flowable/组织服务解析和端到端发起验证待集成环境补跑。

## 增量验证（2026-09-06，详情业务租户回查）

- 流程历史复用 `FlowAccessGuard.requireProcessVisible` 返回的已校验业务记录。
- 任务表单按可见任务租户调用 `selectByProcessInstanceIdAndTenantId`，不再按流程实例 ID 无租户回查。
- 新增 `FlowTaskMutationAuthorizationContractTest` 契约断言。
- `FlowStartPreviewContractTest` 与 `FlowTaskMutationAuthorizationContractTest` 定向执行：3/3 通过。

## 增量验证（2026-09-06，详情返回上限）

- 流程历史、流程图活动和任务摘要均限制最多返回 1000 条；Flow 任务详情查询改用分页读取。
- `FlowStartPreviewContractTest`、`FlowTaskMutationAuthorizationContractTest` 定向执行：4/4 通过。
- Flow/Flow Server reactor compile：通过；Flow 插件 test-compile：通过。

### 必跑项

- `git diff --check`：通过。
- Java 17：`mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow,forge-flow/forge-flow-server -am -DskipTests compile`，通过。
- Flow 插件：`mvn -f forge-framework/forge-plugin-parent/forge-plugin-flow/pom.xml test-compile -DskipTests`，通过，30 个测试类编译成功。
- 定向契约测试：FlowTask 状态修复、Mapper schema、抄送授权、启动预览、模型预检、监控聚合、动作授权、动态签名共 18 个测试，全部通过。
- 前端：`source ~/.nvm/nvm.sh && nvm use v20.19.0 && npx eslint src/views/flow/started.vue src/views/flow/cc.vue src/views/flow/monitor.vue src/views/flow/todo.vue src/views/flow/done.vue src/api/flow.js`，0 errors、8 个模板换行 warning；`npm run build` 通过。
- Flyway 静态检查：新增脚本无 Flyway `${...}` 占位符；历史脚本已有模板占位符，未在本轮修改。

### 未执行与原因

- 未启动 MySQL、Redis、Flowable，未执行跨租户接口、流程启动/终止/撤回、抄送内部回调和迁移实跑；这些需要真实集成环境，不能用契约测试替代。
- 未执行完整 Maven test；仓库既有 Mockito/Byte Buddy attach 与 Surefire profile tag 环境问题仍需独立修复。
- 未执行性能压测、灰度和回滚演练；对应 T7.2/T7.3 保持未完成。

## 本轮增量验证（2026-09-06，模型版本治理与设计器提交锁）

- `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow,forge-flow/forge-flow-server -am -DskipTests compile`：通过。
- `mvn -f forge-framework/forge-plugin-parent/forge-plugin-flow/pom.xml -Dtest=FlowModelVersionGovernanceContractTest,FlowStartPreviewContractTest,FlowTaskMutationAuthorizationContractTest -Dforge.test.groups= test`：7 个测试全部通过。
- `npx eslint src/views/flow/form.vue src/views/flow/design.vue`：0 errors、0 warnings。
- 未启动 MySQL、Redis、Flowable；未执行真实版本清理引用、浏览器连续点击和跨租户接口验证。

## 本轮增量验证（2026-09-06，动态加签边界）

- `mvn -f forge-framework/forge-plugin-parent/forge-plugin-flow/pom.xml -Dtest=FlowTaskSignContractTest -Dforge.test.groups= test`：1/1 通过。
- Flow/Flow Server reactor compile：通过。
- 未执行真实 Flowable 多实例父子加签、数据库关系落库和跨租户接口验证。

## 本轮增量验证（2026-09-06，组织用户回查边界）

- 新增 `FlowOrgIntegrationSecurityContractTest`，约束用户详情解析必须具备租户上下文、启用状态和有效租户成员关系。
- 真实组织服务、禁用用户和跨租户数据库场景待集成环境验证。

## 本轮增量验证（2026-09-06，组织候选查询 SQL）

- Flow/Flow Server reactor compile：通过（Java 17）。
- `mvn -f forge-framework/forge-plugin-parent/forge-plugin-flow/pom.xml -Dtest=FlowOrgIntegrationSecurityContractTest -Dforge.test.groups= test`：1/1 通过。
- 契约覆盖用户详情、选人列表上限、部门/岗位候选 SQL、跨组织角色候选 SQL 和角色键租户条件。
- 未启动真实 MySQL、Redis、Flowable；未执行真实组织成员、禁用成员和跨租户接口链路。

## 本轮增量验证（2026-09-06，表达式模板治理）

- 静态检查：`xmllint --noout forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/resources/mapper/FlowSpelTemplateMapper.xml`，通过。
- 编译：`mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow,forge-flow/forge-flow-server -am -DskipTests compile`，通过；仅有项目既有 deprecated/unchecked 警告。
- 契约测试：`mvn -f forge-framework/forge-plugin-parent/forge-plugin-flow/pom.xml -Dtest=FlowSpelTemplateSecurityContractTest -Dforge.test.groups= test`，2/2 通过。
- 覆盖范围：租户分页/详情/编码唯一、逻辑删除、启停更新、DTO 写接口、页大小和启用列表上限、禁止 Service 层 `LambdaQueryWrapper`。
- 跳过项：未启动 MySQL、Redis、Flowable，未执行真实租户接口、表达式运行时解析、用户组 CRUD 和性能压测。

## 本轮增量验证（2026-09-06，运行时 SpEL 组织解析）

- `xmllint --noout forge-framework/forge-plugin-parent/forge-plugin-system/src/main/resources/mapper/SysUserMapper.xml forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/resources/mapper/FlowSpelTemplateMapper.xml`：通过。
- `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow,forge-flow/forge-flow-server -am -DskipTests compile`：BUILD SUCCESS。
- `mvn -f forge-framework/forge-plugin-parent/forge-plugin-flow/pom.xml -Dtest=FlowSpelTemplateSecurityContractTest,FlowSpelServiceSecurityContractTest -Dforge.test.groups= test`：3/3 通过。
- 覆盖范围：SpEL 角色/区域解析走租户感知组织网关，结果集上限 200，禁止直接 `LambdaQueryWrapper` 和流程变量整包日志。
- 跳过项：未启动 Flowable/数据库，未验证真实表达式编译、执行异常降级、跨租户接口和性能压测。

## 本轮增量验证（2026-09-06，部门角色解析性能收敛）

- `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow -am -DskipTests compile`：BUILD SUCCESS。
- `mvn -f forge-framework/forge-plugin-parent/forge-plugin-flow/pom.xml -Dtest=FlowSpelServiceSecurityContractTest,FlowSpelTemplateSecurityContractTest -Dforge.test.groups= test`：3/3 通过。
- 部门+角色 SpEL 解析改为单次租户 SQL，避免逐用户 `isInDept` 回查；结果仍限制 200 条。
- 未启动数据库和 Flowable，未执行真实执行计划及大规模组织数据压测。

## 本轮最终复验（2026-09-06）

- Flow 插件及依赖编译：BUILD SUCCESS（12.955s）。
- SpEL/模板契约测试：3/3 通过。
- `git diff --check`：通过。

## 本轮增量验证（2026-09-06，超时截止时间与扫描窗口）

- `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow -am -DskipTests compile`：BUILD SUCCESS（14.555s）。
- `mvn -f forge-framework/forge-plugin-parent/forge-plugin-flow/pom.xml -Dtest=FlowTimeoutServiceQueryContractTest,FlowTaskEventListenerTimeoutContractTest -Dforge.test.groups= test`：5/5 通过。
- 覆盖范围：任务创建写入节点配置 dueDate、超时扫描按 dueDate 分页、临近超时按 dueDate 窗口查询、提前分钟边界。
- 跳过项：未启动 Flowable/数据库，旧实例回填、稳定游标、租户级扫描进度和通知幂等未做真实集成验证。

## 本轮增量验证（2026-09-06，节点配置租户边界）

- `xmllint --noout forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/resources/mapper/FlowNodeConfigMapper.xml`：通过。
- `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow -am -DskipTests compile`：BUILD SUCCESS（12.725s）。
- `mvn -f forge-framework/forge-plugin-parent/forge-plugin-flow/pom.xml -Dtest=FlowNodeConfigTenantContractTest,FlowTimeoutServiceQueryContractTest,FlowTaskEventListenerTimeoutContractTest -Dforge.test.groups= test`：6/6 通过。
- 覆盖范围：节点配置运行时读取显式租户条件、任务 dueDate 写入、dueDate 扫描窗口和超时查询边界。
- 跳过项：未启动数据库/Flowable，未验证旧任务回填和多租户调度实链路。

## 本轮增量验证（2026-09-06，节点配置协议与模型租户边界）

范围：节点配置模型引用兼容、节点配置子资源租户归属、流程模型分页/统计租户边界。

- `xmllint --noout forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/resources/mapper/FlowNodeConfigMapper.xml`：通过。
- `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow -am -DskipTests compile`：BUILD SUCCESS。
- `mvn -f forge-framework/forge-plugin-parent/forge-plugin-flow/pom.xml -Dtest=FlowModelMapperSqlContractTest,FlowNodeConfigTenantContractTest -Dforge.test.groups= test`：2/2 通过。
- `git diff --check`：通过。
- 覆盖：设计器 `modelId` 与运行时 `modelKey` 双协议、模型列表/统计显式租户过滤、节点层级/操作/超时/审批人入口归属校验。
- 跳过项：未启动 MySQL、Redis、Flowable，未做真实跨租户接口和 SQL 执行计划验证。

## 本轮增量验证（2026-09-06，模型 ID 操作租户边界）

- `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow -am -DskipTests compile`：BUILD SUCCESS。
- `xmllint --noout forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/resources/mapper/FlowModelMapper.xml forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/resources/mapper/FlowNodeConfigMapper.xml`：通过。
- `mvn -f forge-framework/forge-plugin-parent/forge-plugin-flow/pom.xml -Dtest=FlowModelMapperSqlContractTest,FlowNodeConfigTenantContractTest -Dforge.test.groups= test`：2/2 通过。
- `git diff --check`：通过。
- 覆盖：模型详情、部署、挂起、激活、启停、导出、复制按租户读取；创建/导入/复制写入当前租户。
- 跳过项：未启动真实服务和数据库，未做跨租户接口 E2E、Flowable 部署回滚和 SQL 执行计划验证。

## 本轮增量验证（2026-09-06，超时扫描稳定分页）

- `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow -am -DskipTests compile`：BUILD SUCCESS。
- `mvn -f forge-framework/forge-plugin-parent/forge-plugin-flow/pom.xml -Dtest=FlowTimeoutServiceQueryContractTest -Dforge.test.groups= test`：4/4 通过。
- `git diff --check`：通过。
- 覆盖：活动任务扫描使用 offset 0 + 已处理 ID 集合，避免处理任务后结果集缩短导致漏扫，并避免通知失败任务同轮无限重复。
- 跳过项：未执行真实 Flowable 任务动态变化、并发扫描和多租户调度验证。

- 最终复验说明：`mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow -am -DskipTests compile` 通过；尝试用 reactor 运行契约测试时在仓库既有 `forge-starter-core` Surefire `groups/excludedGroups` 配置处阻断，未将该环境问题记为代码失败。此前同一批 `FlowNodeConfigTenantContractTest`、`FlowTimeoutServiceQueryContractTest`、`FlowModelMapperSqlContractTest` 已分别通过；本轮最后一行兼容性修复未改变契约断言范围。

## 本轮增量验证（2026-09-06，逾期提醒重试与迁移）

范围：T2.3 逾期提醒幂等、失败退避、重试上限和正式数据库迁移。

- `xmllint --noout forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/resources/mapper/FlowOverdueReminderRecordMapper.xml`：通过。
- `rg -n '\$\{[^}]+\}' forge-server/db/migration/V1.0.144__add_flow_overdue_reminder_runtime.sql`：无输出。
- `git diff --check`：通过。
- `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow -am -DskipTests compile`：BUILD SUCCESS。
- 新增 `FlowOverdueReminderRetryContractTest`，覆盖重试上限、指数退避、原子 claim、唯一键和迁移字段；由于仓库既有 Surefire `groups/excludedGroups` 配置及本地依赖缓存问题，最终测试运行需在完整 CI/开发环境复跑。
- 跳过项：未启动 MySQL/Redis/Flowable，未做真实消息发送失败、并发抢占和 Flyway 实跑。

- 追加安全边界：逾期提醒任务缺少可信 `tenant_id` 时 fail-closed，不再默认使用租户 1；该逻辑已纳入 `FlowOverdueReminderRetryContractTest` 静态契约。

## 本轮增量验证（2026-09-06，超时租户游标与流程图用户批量查询）

- `xmllint --noout forge-framework/forge-plugin-parent/forge-plugin-system/src/main/resources/mapper/SysUserMapper.xml forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/resources/mapper/FlowTaskMapper.xml`：通过。
- `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow -am -DskipTests compile`：BUILD SUCCESS。
- `mvn -f forge-framework/forge-plugin-parent/forge-plugin-flow/pom.xml -Dtest=FlowTimeoutServiceQueryContractTest,FlowOrgIntegrationSecurityContractTest -Dforge.test.groups= test`：6/6 通过。
- 覆盖：本地任务快照 `due_date + id` 稳定游标、每条任务恢复租户上下文、批量用户详情 SQL 的租户/启用/成员/组织岗位约束、流程图请求级用户缓存。
- Flyway 静态检查：`V1.0.145__add_flow_timeout_cursor_index.sql` 使用 `information_schema` 防重复创建，未发现 `${...}` 占位符。
- `FlowOverdueReminderRetryContractTest`：3/3 通过，覆盖提醒重试字段与新增超时游标索引迁移。
- `FlowTimeoutServiceQueryContractTest`：6/6 通过，新增覆盖旧任务 `due_date` 缺失回填的 `create_time + id` 游标、租户上下文和本地/Flowable 双写。
- `FlowTaskMutationAuthorizationContractTest`：4/4 通过，新增覆盖管理员回退的运行态、活动节点和 BPMN 目标节点校验。
- `FlowTaskMutationAuthorizationContractTest`：5/5 通过，新增覆盖管理员转派使用租户限定本地任务查询。
- 委派/改派目标用户校验和 `CLAIMED` 镜像状态已纳入同一契约范围；本轮 reactor compile 通过，直接插件测试因本地依赖缓存缺少当前工作树 `EnableStatus` 未重复计入失败。
- `FlowTimeoutServiceImplTest` 曾因本机 Mockito inline Byte Buddy 无法 attach Homebrew JDK 而失败，属于测试运行环境限制；真实 MySQL/Redis/Flowable、跨实例游标和性能压测仍未执行。

## 本轮增量验证（2026-09-06，候选关系与任务锁/历史查询）

- 候选关系迁移静态契约：`FlowTaskCandidateRelationContractTest` 3/3 通过；覆盖关系表双读、幂等回填、激活/失效和租户条件。
- 任务锁与历史 SQL 契约：`FlowLockMapperSqlContractTest`、`FlowTaskMapperSqlContractTest` 共 8/8 通过；覆盖 `task_id + tenant_id FOR UPDATE`、历史查询租户条件、稳定排序和候选关系索引查询。
- 动作授权契约：`FlowTaskMutationAuthorizationContractTest` 7/7 通过；覆盖业务代码不再调用无租户锁查询，以及历史查询使用 XML 分页和批量用户读取。
- 编译：`mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow,forge-flow/forge-flow-server -am -DskipTests compile`，BUILD SUCCESS（JDK 22，项目既有 JDK 17 source/module、deprecated/unchecked 警告）。
- 变更后增量测试命令：`mvn -Penable-tests -Dtest=FlowTaskMapperSqlContractTest,FlowTaskMutationAuthorizationContractTest,FlowLockMapperSqlContractTest,FlowTaskCandidateRelationContractTest -DfailIfNoTests=false test`，18/18 通过。
- 跳过项：未启动 MySQL、Redis、Flowable；未执行真实 Flyway 中断重跑、候选关系迁移前后集合对照、SQL 执行计划、并发锁/幂等、历史分页 HTTP 接口和浏览器验证。

## 本轮追加验证（2026-09-06，候选关系读取边界）

- `FlowTaskMutationAuthorizationContractTest` 增至 8/8，通过 `FlowAccessGuard` 候选关系表查询、租户条件及旧逗号字段兼容回退断言。
- `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow,forge-flow/forge-flow-server -am -DskipTests compile`：BUILD SUCCESS。
- 未启动 MySQL/Redis/Flowable；候选关系表真实读写和迁移后权限矩阵仍待集成环境验证。

## 本轮追加验证（2026-09-06，历史分页 DTO/VO）

- `FlowTaskMutationAuthorizationContractTest`：9/9 通过，覆盖历史分页类型化协议、页大小上限、租户 XML 查询和旧接口兼容适配。
- `mvn -Penable-tests -Dtest=FlowTaskMutationAuthorizationContractTest,FlowTaskMapperSqlContractTest,FlowTaskCandidateRelationContractTest,FlowLockMapperSqlContractTest -DfailIfNoTests=false test`：20/20 通过。
- `pnpm eslint src/api/flow.js`：通过。
- `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow,forge-flow/forge-flow-server -am -DskipTests compile`：BUILD SUCCESS；`xmllint` 和 `git diff --check` 通过。
- 跳过项：未启动 Flow 服务和数据库，未执行分页历史 HTTP 返回、真实总数/并发新增任务一致性、浏览器切换新分页接口验证。

- 追加修正复验：分页 `hasMore` 改按任务查询页游标计算，避免第一页包含发起节点时最后一页误报；修正后同一编译和 20/20 契约测试再次通过。

## 本轮增量验证（2026-09-06，租户回查与任务镜像更新）

- `xmllint --noout src/main/resources/mapper/FlowTaskMapper.xml src/main/resources/mapper/FlowFormInstanceMapper.xml`：通过。
- `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow,forge-flow/forge-flow-server -am -DskipTests compile`（JDK 22）：BUILD SUCCESS；存在项目既有 JDK 17 source/module、deprecated/unchecked 警告，不阻断构建。
- `mvn -Penable-tests -Dtest=FlowTaskMutationAuthorizationContractTest,FlowTaskMapperSqlContractTest,FlowLockMapperSqlContractTest,FlowCcSendSecurityContractTest -DfailIfNoTests=false test`：22/22 通过。
- `mvn -Penable-tests -Dtest=FlowControllerBoundaryContractTest -DfailIfNoTests=false test`：18/18 通过。
- 覆盖：`@IgnoreTenant` 下业务/表单/流程列表读取显式带租户；任务镜像更新必须同时带 `task_id` 与 `tenant_id`；Flow 实例详情、运行态表单和抄送参与人回查不再调用无租户 Mapper。
- 跳过项：未启动 MySQL、Redis、Flowable 或前端；未执行真实 Flyway/HTTP 跨租户、并发锁/幂等、内部回调租户上下文和执行计划验证；本轮未启动服务，无需清理 PID。

## 本轮增量验证（2026-09-06，监控响应 VO 与加签关系审计）

- Flow/Flow Server reactor compile：`mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow,forge-flow/forge-flow-server -am -DskipTests compile`，BUILD SUCCESS。
- 监控/候选/加签契约：`FlowMonitorBatchQueryContractTest`、`FlowMonitorTaskTreeContractTest`、`FlowTaskSignContractTest`、`FlowTaskCandidateRelationContractTest` 共 8/8 通过；收尾回归追加任务动作、超时、抄送和节点租户契约，共 40/40 通过。
- Flow Server 边界契约：`FlowControllerBoundaryContractTest` 20/20 通过，新增监控实例列表、详情和任务树 typed VO 响应断言。
- 前端：`npx eslint src/api/flow.js src/views/flow/monitor.vue`，0 errors、0 warnings。
- 静态检查：候选关系 Mapper、任务 Mapper、表单 Mapper `xmllint` 通过；`V1.0.148__add_flow_task_sign_relation_audit.sql` 无 Flyway `${...}` 占位符；`git diff --check` 通过。
- 覆盖范围：监控固定响应字段改为 VO；实例列表查询异常带 `degraded/errorCode`；动态加签/减签记录父任务、模式、操作者和原因；关系读取按租户和父任务限制最多 50 条；重复加签更新审计字段，减签保留 REVOKED 状态。
- 跳过项：未启动 MySQL、Redis、Flowable，未执行 `V1.0.148` 真实 Flyway 重跑、前后置加签多实例编排、跨租户 HTTP、并发关系写入和浏览器验收。

## 本轮前端构建验证（2026-09-06）

- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm build`：通过，Vite built in 49.84s。
- 因仓库现有 `pnpm-workspace.yaml` 未声明 `packages`，构建前临时移出该配置并在退出时恢复；未留下 workspace 文件变更。
- 构建保留项目既有 Vite 配置、CSS 注释和无效动态导入 warning，不阻断本轮流程改动；未启动浏览器和真实后端服务。

## 本轮增量验证（2026-09-06，组织递归与候选组启动预检）

- `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow,forge-flow/forge-flow-server -am -DskipTests compile`：BUILD SUCCESS（JDK 22；保留项目既有 source/module、deprecated/unchecked 警告）。
- `mvn -Penable-tests -Dtest=FlowOrgIntegrationSecurityContractTest,FlowStartPreviewContractTest -DfailIfNoTests=false test`：3/3 通过。
- 覆盖：组织递归和直接子组织查询均走带 `tenant_id + org_status + del_flag` 的 Mapper XML；启动配置对静态 candidateGroups 执行角色编码/角色 ID/部门 ID/自定义用户组有效成员预检，动态表达式延迟到运行时。
- `git diff --check`、相关 Mapper `xmllint`：通过。
- 跳过项：未启动 MySQL、Redis、Flowable；未执行真实组织树数据和网关条件下的 HTTP E2E。

## 本轮增量验证（2026-09-06，模型版本历史清理）

- 新增 `FlowModelVersionCleanupContractTest`，覆盖清理接口 DTO、租户限定 `SELECT ... FOR UPDATE`、最近版本保留、发布/废弃保护、当前版本保护、运行中实例引用检查和逻辑删除调用。
- `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow,forge-flow/forge-flow-server -am -DskipTests compile`：BUILD SUCCESS（JDK 22）。`mvn -Penable-tests -Dmaven.main.skip=true -Dtest=FlowOrgIntegrationSecurityContractTest,FlowStartPreviewContractTest,FlowModelVersionCleanupContractTest,FlowModelVersionGovernanceContractTest -DfailIfNoTests=false test`：6/6 通过。
- XML `xmllint`、迁移占位符检查和 `git diff --check`：通过；前端流程 API/版本/用户组页面 ESLint：通过。
- 跳过项：未启动真实数据库和 Flowable；reactor 测试入口仍受仓库既有 `forge-starter-core` Surefire groups 配置阻断，改用已完成 reactor 编译产物执行目标契约测试。

## 本轮增量验证（2026-09-06，模型目录批量排序）

- `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow,forge-flow/forge-flow-server -am -DskipTests compile`：BUILD SUCCESS（JDK 22；保留既有 JDK 17 source/module、deprecated/unchecked 警告）。
- `mvn -Penable-tests -Dmaven.main.skip=true -Dtest=FlowModelVersionControllerContractTest,FlowNodeConfigControllerContractTest,FlowControllerBoundaryContractTest -DfailIfNoTests=false test`（Flow Server）：22/22 通过。
- `mvn -Penable-tests -Dmaven.main.skip=true -Dtest=FlowModelSortingContractTest -DfailIfNoTests=false test`（Flow 插件）：3/3 通过，覆盖排序 SQL 的租户锁/逻辑删除、排序字段迁移、独立权限迁移和 Controller DTO/权限契约。
- `npx eslint src/api/flow.js src/views/flow/model.vue`：通过；仅有 npm 用户配置弃用 warning。
- `npm run build --if-present`（Node 20.19.0）：Vite production build 通过（34.17s）；保留既有 CSS 注释、无效动态导入和 Vite config native warning。
- `xmllint --noout forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/resources/mapper/FlowModelMapper.xml`、迁移 `${...}` 扫描和 `git diff --check`：通过。
- 备注：按项目标准尝试执行 `pnpm build` 时，当前仓库 `pnpm-workspace.yaml` 缺少 `packages` 字段而被 pnpm 直接拒绝；未修改该既有配置，使用等价 `npm run build` 完成同一 Vite 构建验证。
- 跳过项：未启动 MySQL、Redis、Flowable，未执行真实 Flyway 首次/重跑/中断恢复、排序并发事务、SQL 执行计划、跨租户 HTTP E2E 和浏览器拖动验收。

## 本轮增量验证（2026-09-06，任务动作与候选关系幂等）

- 变更覆盖：转办动作增加租户限定幂等凭证写入与 owner 重试；动态加签/减签增加候选关系幂等键、请求摘要和冲突检测；新增 `V1.0.153__add_flow_task_sign_idempotency.sql`。
- `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow,forge-flow/forge-flow-server -am -DskipTests compile`（JDK 22）：BUILD SUCCESS；保留既有 source/module、deprecated/unchecked 编译警告。
- `mvn -Penable-tests -Dmaven.main.skip=true -Dtest=FlowTaskSignContractTest -DfailIfNoTests=false test`：3/3 通过，覆盖 DTO、Controller 透传、服务幂等冲突、关系 Mapper 和迁移字段/索引。
- `xmllint --noout forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/resources/mapper/FlowTaskCandidateMapper.xml`、V1.0.153 Flyway 占位符扫描、`git diff --check`：通过。
- 跳过项：未启动 MySQL/Redis/Flowable，未执行真实并发转办/加签、关系表迁移重跑和 HTTP 重试验收；仍需 T7.2/T7.3 集成验证。

## 本轮增量验证（2026-09-06，超时扫描跨实例互斥）

- 变更覆盖：`FlowTimeoutServiceImpl` 增加 Redis `SETNX` 锁和 token 校验释放，Redis 不可用时保留单 JVM `ReentrantLock` 降级；批内 `due_date + id` 游标逻辑不变。
- `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow,forge-flow/forge-flow-server -am -DskipTests compile`（JDK 22）：BUILD SUCCESS；既有 source/module、deprecated/unchecked 警告不阻断。
- `mvn -Penable-tests -Dmaven.main.skip=true -Dtest=FlowTimeoutServiceQueryContractTest -DfailIfNoTests=false test`：6/6 通过，覆盖锁、租户上下文、稳定游标和 due date 查询。
- `git diff --check`：通过。未启动 Redis，锁竞争、租约过期和多实例扫描仅完成静态契约验证。

## 本轮增量验证（2026-09-06，流程图连线状态）

- 变更覆盖：新增 `ProcessSequenceFlowInfo`，`ProcessDiagramInfo` 返回连线状态和不可用原因；Flow 服务按历史 activityType 判断是否可可靠高亮；`ProcessDiagramViewer.vue` 高亮已执行/待执行连线并显示降级提示。
- `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow,forge-flow/forge-flow-server -am -DskipTests compile`：BUILD SUCCESS（JDK 22，既有编译警告）。
- `mvn -Penable-tests -Dmaven.main.skip=true -Dtest=FlowProcessDiagramSequenceContractTest,FlowTaskSignContractTest,FlowTimeoutServiceQueryContractTest -DfailIfNoTests=false test`：10/10 通过。
- `npx eslint src/components/bpmn/ProcessDiagramViewer.vue`：通过；`npm run build --if-present`：Vite production build 通过（42.04s）。
- `git diff --check`：通过；未启动浏览器、MySQL、Redis、Flowable，未执行真实历史连线数据和跨浏览器验收。

## 本轮增量验证（2026-09-06，前端动作幂等凭证）

- `todo.vue` 为详情审批、批量快捷审批和转办生成 UUID 幂等键与 SHA-256 请求摘要；后端公共动作 DTO 统一接收，保留动态 variables Map 例外。
- `npx eslint src/views/flow/todo.vue src/components/bpmn/ProcessDiagramViewer.vue`：通过。
- `npm run build --if-present`（Node 20.19.0）：Vite production build 通过（42.04s）；保留既有 Vite native config、CSS 注释和无效动态导入 warning。
- 跳过项：未启动浏览器和 Flow 服务，未执行真实网络重试、跨标签页重复提交和端到端幂等落库验证。
## 本轮增量验证（2026-09-06，用户组页面与候选组配置）

- 必跑：候选组配置组件 ESLint、用户组页面 ESLint、Flow API 模块 ESLint、前端生产构建、变更文件空白检查。
- 已执行：上述 ESLint 通过；`npm run build --if-present` 通过；`git diff --check` 通过。
- 条件验证：需要 Flyway 执行 `V1.0.149`、后端服务和登录权限，才能在 `/flow/userGroup` 看到菜单并实际维护成员；当前未启动真实服务，保留为 T7.2/T7.3 集成门禁。

## 本轮追加验证（2026-09-06，用户组关键字搜索）

- `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow -am -DskipTests compile`：通过。
- `mvn -Penable-tests -Dmaven.main.skip=true -Dtest=FlowUserGroupGovernanceContractTest -DfailIfNoTests=false test`：3/3 通过。
- 覆盖：用户组分页搜索按编码或名称匹配，继续保持租户、启用状态和逻辑删除边界。
- 跳过：真实 MySQL 查询计划、跨租户 HTTP 和浏览器输入搜索。

## 本轮增量验证（2026-09-06，待办审批详情动态表单竞态）

- 变更范围：`forge-admin-ui/src/components/common/FlowBusinessForm.vue` 及其 Vitest 回归测试。
- `npm test -- --run src/components/common/__tests__/FlowBusinessForm.spec.js src/components/flow/__tests__/FlowReadonlyFormPanel.spec.js`：6/6 通过；覆盖表单地址快速切换、组件卸载后异步结果丢弃、找不到表单组件、无效模块导出降级，以及只读审批表单面板兼容行为。
- `npx eslint src/components/common/FlowBusinessForm.vue src/components/common/__tests__/FlowBusinessForm.spec.js`：通过，0 error、0 warning。
- `npm run build --if-present`：Vite production build 成功（最后一次约 1 分 21 秒，前一次约 2 分 12 秒）；保留项目既有 Vite native config、无效动态导入和 UnoCSS plugin timing warning，不阻断本轮变更。
- `git diff --check`：通过。
- 未启动 MySQL、Redis、Flowable 或浏览器；未执行真实待办接口和跨页面浏览器切换验收，需在集成环境确认实际业务表单路径和关闭详情时序。

## 本轮增量验证（2026-09-06，流程模型排序审计字段）

- 变更范围：`FlowModelMapper.xml` 的 `updateSortOrder` 和 `FlowModelSortingContractTest`。
- `xmllint --noout forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/resources/mapper/FlowModelMapper.xml`：通过。
- `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow -am -DskipTests compile`（OpenJDK 17）：BUILD SUCCESS。
- `mvn -Penable-tests -Dmaven.main.skip=true -Dtest=FlowModelSortingContractTest -DfailIfNoTests=false test`（OpenJDK 17）：3/3 通过，覆盖租户行锁、`FOR UPDATE` 后无 `ORDER BY` 以及 `last_update_by` 更新列。
- `git diff --check`：通过。
- 首次验证使用 Java 8 时因项目目标发行版为 17 失败，切换 OpenJDK 17 后编译和测试通过；未启动真实 MySQL，需在目标环境重新点击保存排序确认迁移后的实际表结构。
- 追加重跑：Mapper 参数改名为 `lastUpdateBy` 后，插件模块直接测试仍为 3/3 通过；从 `forge-server` 根目录串行执行测试时被仓库既有 `forge-starter-core` Surefire `groups/excludedGroups` 配置阻断，未作为代码失败。

## 本轮追加验证（2026-09-06，用户组公共组件和分页参数修复）

- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && npx eslint src/views/flow/userGroup.vue src/api/flow.js`：通过。
- `npm run build --if-present`（Node 20.19.0）：Vite production build 通过（约 1 分 11 秒）；保留仓库既有 Vite native config、无效动态导入和 UnoCSS plugin timing warning。
- `xmllint --noout forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/resources/mapper/FlowUserGroupMapper.xml`：通过。
- `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow -am -DskipTests compile`（OpenJDK 17）：BUILD SUCCESS。
- `mvn -Penable-tests -Dmaven.main.skip=true -Dtest=FlowUserGroupGovernanceContractTest -DfailIfNoTests=false test`：4/4 通过，新增 Mapper `keyword` 参数声明和 Service 转发契约。
- `git diff --check`：通过。
- 未启动 MySQL、Redis、Flowable 或浏览器；真实用户组接口、雪花 ID 成员增删、Flyway 执行和跨租户 HTTP 仍需 T7.2/T7.3 集成门禁。

## 本轮追加验证（2026-09-06，选人公共组件启用用户过滤）

- `npx eslint src/components/common/UserSelectModal.vue src/views/flow/userGroup.vue src/api/flow.js`：通过。
- `npm run build --if-present`（Node 20.19.0）：Vite production build 通过（约 1 分 34 秒）；保留仓库既有 Vite native config、无效动态导入和 UnoCSS plugin timing warning。
- `git diff --check`：通过。

## 本轮追加验证（2026-09-06，用户组允许清空成员）

- 公共 `UserSelectModal` 增加可选 `allowEmpty`，用户组成员可确认空选择以移除全部成员；其他调用方默认行为不变。
- `npx eslint src/components/common/UserSelectModal.vue src/views/flow/userGroup.vue src/api/flow.js`：通过。
- `npm run build --if-present`（Node 20.19.0）：Vite production build 通过（约 1 分 5 秒）；保留仓库既有 Vite native config、无效动态导入和 UnoCSS plugin timing warning。
- `git diff --check`：通过。

## 本轮增量验证（2026-09-06，Maven Surefire 测试引擎配置）

- 根因：父 POM 无条件配置 Surefire `groups`/`excludedGroups`，依赖模块没有测试引擎时在 provider 探测阶段失败，报 `groups/excludedGroups require TestNG, JUnit48+ or JUnit 5`。
- 修复：`forge-server/pom.xml` 在所有子模块的 test classpath 提供 BOM 管理的 `junit-jupiter-engine`，保留按环境执行的 `dev` 标签和 `exclude` 标签过滤。
- `mvn -Penable-tests -pl forge-framework/forge-starter-parent/forge-starter-api-config -DskipTests=false test`：BUILD SUCCESS；该模块无测试源码，不再触发 Surefire provider 错误。
- `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow -am -Penable-tests -Dtest=FlowUserGroupGovernanceContractTest -Dforge.test.groups= -Dsurefire.failIfNoSpecifiedTests=false test`：BUILD SUCCESS，4/4 通过。
- `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow -am -DskipTests compile`：BUILD SUCCESS（OpenJDK 17）。
- 额外模块测试仍可能受本机 Mockito inline Byte Buddy 无法 attach 和 Sa-Token 无 HTTP 上下文影响，属于既有环境测试阻断，不归因于 Surefire 配置修复。
- 未启动 MySQL、Redis、Flowable 或浏览器；真实流程和性能门禁仍待 T7.2/T7.3 集成环境执行。

## 本轮增量验证（2026-09-07，默认打包跳过测试）

- 用户要求发布打包不执行测试用例；根 POM `forge.tests.skip` 默认改为 `true`，`enable-tests` profile 仍显式设为 `false`，测试入口未移除。
- `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow -am package`（OpenJDK 17）：BUILD SUCCESS；Reactor 各模块 Surefire 均输出 `Tests are skipped.`，流程插件 JAR 正常生成。
- `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow -am -Penable-tests -Dtest=FlowUserGroupGovernanceContractTest -Dforge.test.groups= -Dsurefire.failIfNoSpecifiedTests=false test`：BUILD SUCCESS，4/4 通过，确认显式测试 profile 仍可执行测试。
- `git diff --check`、`xmllint --noout forge-server/pom.xml`：通过。
- 未启动 MySQL、Redis、Flowable 或浏览器；Mockito/Sa-Token 相关测试仅在显式测试 profile 下执行，仍需按环境准备测试上下文。

## 本轮追加验证（2026-09-07，默认打包跳过测试编译）

- 验证范围：根 POM 的 Maven 生命周期配置，确保发布 `package/install` 不执行也不编译测试用例，同时保留 `-Penable-tests` 测试入口。
- `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am package`（OpenJDK 17）：通过；测试资源未复制、测试源码未编译、Surefire 跳过。
- `mvn package`（forge-server 全工程，OpenJDK 17）：通过，51/51 模块成功；各模块 `testResources`/`testCompile`/Surefire 均处于跳过状态，应用包完成 repackage。
- 低成本检查：`git diff --check`、`xmllint --noout forge-server/pom.xml` 通过。
- 失败/警告：主源码保留既有 deprecated、unchecked 和个别 MCP API deprecated 编译警告，不阻断；此前生成器测试编译错误已通过默认跳过测试编译规避，显式测试 profile 仍需单独修复这些既有测试问题。
- 跳过项：未启动 MySQL、Redis、Flowable 或浏览器；真实流程 E2E 和性能门禁仍按 T7.2/T7.3 执行。

- 显式 profile 回归：`mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow -am -Penable-tests -Dtest=FlowUserGroupGovernanceContractTest -Dforge.test.groups= -Dsurefire.failIfNoSpecifiedTests=false test`，4/4 通过，确认 `-Penable-tests` 可恢复测试编译与执行。
