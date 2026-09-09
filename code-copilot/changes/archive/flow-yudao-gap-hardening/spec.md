# 流程审批核心能力补强（Yudao 对比后的必要迁移项）

## 1. 文档信息

- **变更名**：`flow-yudao-gap-hardening`
- **状态**：Draft
- **文档类型**：流程平台技术 Spec
- **对比基线**：Yudao `ruoyi-vue-pro` 提交 `2bbe79b34ab8c9c7b0148300599dc8d4881c8db1`
- **Forge 基线**：`flow-enhancement-suite` 已完成实现及其 Review 修复
- **适用范围**：Forge Flowable 7.0.1 流程引擎、流程设计器、待办/审批接口

本 Spec 是对已完成 `flow-enhancement-suite` 的增量补充，不覆盖已经交付的原始需求。

## 2. 决策摘要

Yudao 中有价值的部分不是整套复制，而是以下五项平台能力。前四项属于本轮必须补强的核心能力，第五项属于有明确业务使用场景时实施的增强能力。

| 优先级 | 能力 | 决策 |
|---|---|---|
| P0 | 候选人策略 SPI、统一候选人解析和发布前校验 | 必须迁移，解决审批人配置分散、部署后才发现无人审批的问题 |
| P0 | 审批人自选下一节点 | 必须迁移，补齐“当前审批人决定下一个审批人”的业务场景 |
| P0 | 基于 BPMN 路径的精确退回 | 必须补强，保留当前并行分支 fail-closed 安全策略 |
| P0 | 下一审批节点及候选人预测 | 必须迁移，复用候选人策略，供审批确认和前端预览使用 |
| P1 | 真实委派语义、前后加签和减签 | 有明确业务需求再启用，但数据模型和接口边界按本 Spec 预留 |

## 3. 已有能力，不得重复迁移

以下能力已在 Forge 中存在，本变更只做兼容性验证，不重新设计或引入第二套实现：

- `sys_flow_model.allow_multi_return` 多级退回开关、历史退回节点选择和退回后直送。
- `PROCESS_START_USER` 发起人自选审批人协议，包含设计器、普通流程、低代码流程和应用流程入口。
- `FlowBusinessListDisplayAdapter`、`businessParams`、`displayExtensions` 待办业务展示扩展。
- 发起人、当前处理人、任务 owner 及管理员改派当前任务。
- 模型 Key 自定义、格式校验、租户内唯一性和发布后不可修改。
- 流程评论、站内信/企微通知、Webhook 回调、逾期提醒基础能力。

这些能力的现有实现是唯一配置源，禁止因为参考 Yudao 再增加平行配置表或平行 API。

## 4. 背景与问题

Forge 当前的审批人计算主要集中在 `FlowNodeConfigServiceImpl` 的 `assigneeType` 分支中，存在以下平台性问题：

1. 新增一种审批人规则需要修改中心 Service，策略参数校验、运行时计算和未来节点预测容易出现不一致。
2. BPMN 发布阶段没有统一检查每个人工用户任务的审批人规则，错误模型可能部署成功，启动后才生成无人处理的待办。
3. 当前支持发起人自选，但当前审批人不能为后续“审批人自选”节点设置审批人。
4. 普通任务退回已具备目标节点协议，但管理员回退和用户退回尚未共享同一个路径安全算法；并行、会签场景不能依赖简单的“移动全部活动节点”。
5. 流程图可以展示已完成和当前节点，但没有一个明确的接口返回“下一步可能到达的节点及审批人”。
6. Forge 当前的 `delegate` 实际更接近转派；Flowable 的 `DelegationState.PENDING` 委派语义尚未完整暴露。加签/减签只有配置字段和操作码，尚无完整的父子任务生命周期。

## 5. 目标与非目标

### 5.1 目标

- 建立可插拔、可校验、可复用的候选人策略内核。
- 在审批完成前校验并保存后续节点自选审批人，确保只允许设置真实可达节点。
- 统一用户退回和管理员回退的 BPMN 路径判断、分支撤回和审计语义。
- 通过同一候选人策略内核提供下一节点候选人预测，避免“预览结果”和“实际分配”不一致。
- 为真实委派及加减签提供清晰的 Flowable 语义边界和本地任务数据镜像。

### 5.2 非目标

- 不迁移 Yudao 的 Simple Designer、表单体系或 DAO 层。
- 不修改 Flowable 原生 `ACT_*` 表，也不替换 Flowable 内部 Behavior。
- 不在本轮实现复杂子流程、多实例子流程或跨流程复制。
- 不改变已发布模型的历史流程定义，不对运行中的流程批量重算审批人。
- 不把审批人、退回目标等动态配置改成 Controller 的 `@RequestBody Map`；固定字段必须使用 DTO，只有 `variables`、`nextAssignees` 这类动态字段可以保留 Map 属性。
- 不以升级 Flowable 版本作为本变更前置条件。当前版本仍按 Flowable 7.0.1 的安全能力实现，不能直接复制 Yudao 针对 7.2/8.0 的状态迁移调用。

## 6. 总体设计

### 6.1 候选人策略 SPI

新增流程内部策略接口和统一调用器，建议放在 `forge-plugin-flow` 的 `candidate` 包：

```java
public interface FlowCandidateStrategy {

    String getType();

    boolean isParameterRequired();

    void validateParameter(String parameter, FlowCandidateContext context);

    Set<String> calculateForTask(FlowCandidateContext context, String parameter);

    Set<String> calculateForActivity(FlowCandidateContext context, String activityId,
                                     String parameter);
}
```

调用器 `FlowCandidateResolver` 必须统一完成以下步骤：

1. 从 BPMN 扩展属性或 `FlowNodeConfig` 解析策略类型和参数，优先使用当前设计器保存的新字段，兼容历史 XML。
2. 调用对应 SPI 计算用户 ID，运行态和未执行节点预测必须复用同一个入口。
3. 归一化为字符串用户 ID，按租户校验用户存在、启用状态和有效期，去重并保持稳定顺序。
4. 根据节点 `extConfig` 读取“审批人为空”策略。至少支持 `BLOCK`、`START_USER`、`ADMIN`、`AUTO_PASS`、`AUTO_REJECT` 五种语义；默认使用 `BLOCK`。
5. 根据节点 `extConfig.skipStartUser` 处理发起人同时作为审批人的情况。只有候选人数量大于 1 时才移除发起人，避免移除后无人审批。
6. 对 `initiatorSelect` 和 `approveUserSelect` 节点从流程变量读取选人结果，不再尝试从固定 `assignee` 计算用户。

首批策略类型如下：

| 策略类型 | 说明 | 阶段 |
|---|---|---|
| `custom` | 指定用户 ID，保持 Forge 现有字面量用户 ID 协议 | P0 |
| `role` / `dept` / `post` | 角色、部门、岗位成员 | P0 |
| `initiator` | 发起人 | P0 |
| `leader` / `deptManager` | 发起人上级、发起部门负责人 | P0 |
| `spel` | 受控 SPEL/表达式模板 | P0 |
| `initiatorSelect` | 发起时由申请人选择，沿用 `PROCESS_START_USER` | 已有，纳入统一解析器 |
| `approveUserSelect` | 当前审批时选择下一节点审批人 | P0 |
| `startUserDeptLeader` / `startUserDeptLeaderMulti` | 发起人部门负责人及连续多级负责人 | P1 |
| `formUser` / `formDeptLeader` | 从表单用户/部门字段计算 | P1 |
| `userGroup` | 可复用用户组 | P2，暂缓 |

SPI 注册冲突、未知策略和参数不完整必须在发布阶段失败，不允许静默退化为空候选人。

### 6.2 发布前校验

`FlowModelServiceImpl.deployModel` 在调用 Flowable 部署前必须调用候选人校验器，遍历主流程及支持的嵌套用户任务：

- 自动通过、自动拒绝或服务节点不要求人工审批人。
- 人工用户任务必须声明可识别的候选人策略。
- 策略要求参数时，参数不能为空且必须通过策略自身校验。
- `initiatorSelect` 必须存在合法的多实例集合表达式。
- `approveUserSelect` 必须声明节点 Key，且配置为可被审批接口识别的多实例节点。
- 发现错误时不得创建 Flowable Deployment，也不得更新 `FlowModel` 的发布状态。

错误应使用稳定错误码，至少包含：

- `FLOW_CANDIDATE_STRATEGY_NOT_CONFIGURED`
- `FLOW_CANDIDATE_STRATEGY_UNKNOWN`
- `FLOW_CANDIDATE_PARAMETER_INVALID`
- `FLOW_SELECT_APPROVER_NODE_INVALID`

### 6.3 审批人自选下一节点

#### 变量协议

新增流程变量 `PROCESS_APPROVE_USER`，类型为：

```text
Map<String, List<String>>
```

Map 的 Key 是下一个用户任务的 BPMN `userTask` 节点 Key，Value 是用户 ID 字符串列表。用户 ID 在前端、JSON 和流程变量中均保持字符串，禁止转换为 JavaScript `Number`。

`PROCESS_START_USER` 与 `PROCESS_APPROVE_USER` 独立存在，不能互相覆盖。

#### BPMN 协议

设计器新增“审批人自选（当前审批人选择）”选项。节点使用并行多实例，集合表达式统一为：

```xml
<bpmn:multiInstanceLoopCharacteristics
    isSequential="false"
    flowable:collection="${PROCESS_APPROVE_USER['<nodeKey>']}"
    flowable:elementVariable="assignee" />
```

必须保持 XML → JSON → XML 往返稳定，历史模型中没有该属性时不能误判为审批人自选。

#### 审批接口

在 `FlowTaskApproveDTO` 新增明确属性：

```java
private Map<String, List<String>> nextAssignees;
```

审批服务处理顺序：

1. 读取流程实例已有变量。
2. 合并本次表单 `variables`，本次提交值覆盖同名历史值。
3. 根据当前任务、BPMN 图和合并变量计算下一批真实可达用户任务。
4. 对每个 `approveUserSelect` 后继节点要求 `nextAssignees[nodeKey]` 非空；不允许为非后继节点传值。
5. 校验用户属于当前租户且处于可审批状态，去重后写入 `PROCESS_APPROVE_USER`。
6. 对已经写入且已经创建任务的节点禁止重复覆盖，避免并行分支互相覆盖审批人。
7. 将合并后的变量写回流程实例，再完成当前任务。

稳定错误码至少包含：

- `FLOW_APPROVE_USER_SELECT_REQUIRED`
- `FLOW_APPROVE_USER_SELECT_NODE_NOT_REACHABLE`
- `FLOW_APPROVE_USER_SELECT_USER_INVALID`
- `FLOW_PROCESS_VARIABLE_MERGE_FAILED`

### 6.4 精确退回与管理员回退

新增共享的 `FlowReturnPathResolver`，用户退回和管理员回退都必须调用它。校验和状态迁移必须分成两个阶段：先只读计算和校验，全部通过后再调用 Flowable 状态变更。

#### 校验规则

- 目标必须是同一流程定义中的已执行用户任务节点。
- 目标必须从当前节点沿 BPMN 图存在串行可达路径。
- 目标不能是当前任务、开始事件、结束事件、服务节点、子流程或调用活动。
- 条件网关必须按当前流程变量计算；无法确定的分支不得猜测为可达。
- 多实例会签必须识别执行路径，不能只按 `taskDefinitionKey` 去重。
- 并行分支涉及多个不相关活动时继续 fail-closed，返回明确的 `FLOW_RETURN_PARALLEL_UNSAFE`，不把所有活动强行合并到一个节点。

#### 状态迁移规则

- 只撤回目标路径相关的运行任务。
- 当前操作者任务记录退回状态、意见和签名。
- 同一路径上其他被撤回任务记录取消状态和系统原因。
- 保留 `FLOW_RETURN_SOURCE_ACTIVITY_ID`、`FLOW_RETURN_TARGET_ACTIVITY_ID` 及退回防自动审批标记，供退回节点修正后直送和审批策略判断。
- 管理员回退也必须同步本地 `sys_flow_task`、`sys_flow_business` 状态、评论和错误审计，不能只调用 `RuntimeService`。

不允许直接复用当前 `FlowInstanceServiceImpl.rollbackToActivity` 中“把所有当前活动移动到单节点”的逻辑；该方法应改为调用共享路径解析器，或在 P0 实现期间对不安全场景明确拒绝。

### 6.5 下一审批节点与候选人预测

在 `FlowTaskService` 增加只读能力：

```java
List<FlowNextApprovalNodeVO> getNextApprovalNodes(String taskId,
                                                   Map<String, Object> variables);
```

增加接口：

```text
GET /api/flow/task/{taskId}/next-approvals
```

响应至少包含：

- `nodeKey`、`nodeName`、`nodeType`
- `candidateStrategy`
- `candidateSelectable`
- `candidateUserIds`、`candidateUserNames`
- `status`：`NOT_STARTED`、`POSSIBLE`、`UNCERTAIN`
- `conditionStatus`：`MATCHED`、`NOT_MATCHED`、`UNKNOWN`；无法确定的条件必须返回 `UNKNOWN`，不能伪造确定结果

候选人必须通过 `FlowCandidateResolver.calculateForActivity` 计算，不得在预测接口中复制审批人 `switch`。流程详情中的未来节点可以作为同一能力的后续展示入口，但本轮先保证下一节点接口稳定。

## 7. P1：真实委派和加减签

P1 不阻塞 P0，但接口和数据语义必须与当前“转派”区分清楚。

### 7.1 转派与委派

- **转派（transfer/reassign）**：直接修改最终 `assignee`，原处理人不再需要恢复。当前 `/api/flow/task/reassign` 和管理员改派保持此语义。
- **委派（delegate）**：保留原处理人为 `owner`，调用 Flowable `taskService.delegateTask` 进入 `DelegationState.PENDING`；被委派人处理后只调用 `resolveTask` 恢复原处理人，不直接完成主任务。

为避免破坏现有客户端，当前 `/api/flow/task/delegate` 的兼容语义在 P1 完成前仍按转派处理。新增真实委派接口必须使用明确命名（例如 `/api/flow/task/delegate-task`），并提供迁移说明，禁止静默改变旧接口含义。

审批通过、驳回和终止入口必须先判断委派状态：

- `PENDING` 委派任务只能 resolve，不能直接 complete。
- resolve 后本地任务状态恢复为原 owner 的待办状态。
- 委派关系、owner、assignee、delegation state 必须在待办详情和历史中可见。

### 7.2 前加签、后加签、减签

只有节点操作权限开启 `addSign`/`reduceSign` 时才允许调用。加签任务必须使用 Flowable 原生父子任务关系：

- 前加签：主任务进入等待，子任务完成后恢复主任务。
- 后加签：主任务先进入处理中，子任务完成后自动完成或恢复主任务，具体由节点策略决定。
- 减签：只能取消当前加签树中的子任务，不能删除普通审批任务。
- 加签用户不得与当前 assignee/owner 重复，所有用户必须通过租户和启用状态校验。
- 每次加签、减签、子任务完成和父任务恢复必须产生流程评论和本地任务事件。

## 8. 数据模型与接口变更

### 8.1 P0 数据变更

P0 不新增业务表，不修改 Flowable 原生表，优先复用现有字段：

| 现有数据 | 用途 |
|---|---|
| `sys_flow_node_config.assigneeType/assigneeValue/assigneeExpr` | 候选人策略输入，保留历史值兼容 |
| `sys_flow_node_config.extConfig` | 审批人为空策略、跳过发起人策略等扩展配置 |
| `sys_flow_model.allow_multi_return` | 多级退回开关，保持现有语义 |
| Flowable 流程变量 | `PROCESS_START_USER`、`PROCESS_APPROVE_USER`、退回状态标记 |
| `sys_flow_task` / `sys_flow_business` | 本地待办、业务关联和退回审计同步 |

不为了 SPI 增加一张“审批人策略表”，不把已有 `assigneeType` 重命名为另一套 Yudao 数字枚举。

### 8.2 P0 DTO/VO 变更

- `FlowTaskApproveDTO` 增加 `nextAssignees`，类型为 `Map<String, List<String>>`。
- `FlowTaskService` 增加 `getNextApprovalNodes`。
- 新增 `FlowNextApprovalNodeVO`，不直接复用数据库实体作为接口协议。
- `TaskFormInfo` 必须增加 `nextApprovalNodes` 只读字段；现有 `returnTargets` 保持兼容。
- `ProcessNodeInfo` 必须增加 `candidateStrategy`、`candidateSelectable`、`predictionStatus` 和 `conditionStatus`，已有字段不改类型。

### 8.3 P1 数据变更

真实委派和加减签实施前新增 Flyway 迁移，版本号必须高于当前已执行版本，具体版本号由实施时按仓库迁移历史确定。建议给 `sys_flow_task` 增加可空字段：

| 字段 | 类型 | 用途 |
|---|---|---|
| `delegation_state` | `varchar(20)` | 镜像 Flowable `PENDING/RESOLVED` 等委派状态 |
| `parent_task_id` | `varchar(64)` | 加签父任务 ID |
| `scope_type` | `varchar(20)` | `BEFORE/AFTER` 加签类型 |

建议索引：`(tenant_id, process_instance_id, parent_task_id)`。字段必须可空，历史任务回填为空，不影响普通审批。

Flowable 原生表仍由 Flowable 自己维护；本地字段只能作为展示、查询和审计镜像，不能反向覆盖引擎状态。

### 8.4 不在本轮增加的数据

- 暂不新增独立流程用户组表。
- 暂不新增任务附件表；现有 `attachmentUrls` 不扩展为伪附件系统，待附件权限、文件生命周期和历史展示单独立项。
- 暂不新增 HTTP 节点触发器表；Forge 已有事件和 Webhook，只有出现“节点级同步请求/回调可配置”需求时再设计统一 Trigger SPI。

## 9. 兼容性与安全要求

- 旧 BPMN XML 中的固定用户、SPEL、角色/部门表达式必须可继续解析；读取旧 XML 不能触发自动改写。
- 新设计器写入固定用户时必须写字面量用户 ID，不能写成 `${user_123}` 变量表达式。
- 所有候选人、`nextAssignees`、改派、委派、加签用户都必须校验当前租户、启用状态和有效期。
- `FlowTaskController` 继续使用明确 DTO；`variables`、`nextAssignees` 作为动态字段保留 Map，`taskId`、`userId`、`targetUserId` 等固定字段不得放入 Map。
- 所有流程状态写入使用 `FlowTaskStatus`、`FlowBusinessStatus` 等枚举，禁止新增魔法数字或字符串。
- 指定退回、管理员回退、委派、加减签必须在状态迁移前锁定租户内流程实例，并记录失败审计。
- 预测接口只能返回当前租户有权看到的用户和节点信息，不得通过候选人预测绕过数据权限。
- 外部组织服务调用必须使用现有超时和降级机制；组织服务不可用时，审批人计算应失败闭环或按明确空审批人策略处理，不能返回伪造成功。
- 新增持久化查询必须写入 Mapper XML，并显式带租户和逻辑删除条件；候选人 SPI 不得在 Service 中拼接复杂 SQL。

## 10. 实施顺序

### 阶段一：P0 候选人内核和发布门禁

1. 建立 `FlowCandidateStrategy`、上下文和 `FlowCandidateResolver`。
2. 将现有 `FlowNodeConfigServiceImpl` 策略迁入适配器，保持旧 `assigneeType` 兼容。
3. 接入租户/用户状态过滤、空审批人策略和发起人跳过策略。
4. 在流程部署前执行逐用户任务校验。

### 阶段二：P0 审批人自选下一节点

1. 扩展设计器解析/写回 `approveUserSelect`。
2. 扩展 `FlowTaskApproveDTO` 和 `PROCESS_APPROVE_USER` 变量协议。
3. 实现后继节点可达性校验和历史变量合并。
4. 增加下一审批节点接口和待办表单展示。

### 阶段三：P0 精确退回

1. 实现 BPMN 路径解析器，只读计算目标路径。
2. 用户退回和管理员回退共用校验与状态迁移服务。
3. 保留 Flowable 7.0.1 并行/多实例 fail-closed 策略。
4. 补齐退回、取消、直送和业务状态回调的一致性测试。

### 阶段四：P1 委派和加减签

在 P0 稳定且有真实业务需求后，先增加本地任务镜像字段，再分别实现真实委派和加减签，不与 P0 混合上线。

## 11. 验收标准

### P0 必须通过

- 错误审批人策略在部署时失败，数据库中不产生新的 Flowable Deployment。
- 停用用户不会进入新任务候选人；候选人为空时按节点策略执行，默认阻断并记录错误。
- 审批人自选下一节点只能设置真实可达用户任务，非法节点、空列表、跨租户用户均被拒绝。
- 当前节点没有可编辑字段时，审批后续节点仍能读取历史流程变量；提交部分字段不会丢失未提交变量。
- 串行流程可从当前节点退回指定历史节点；并行/会签无法安全计算时必须拒绝，不得误合并分支。
- 用户退回和管理员回退产生一致的本地任务状态、评论、业务状态和错误审计。
- 下一审批节点接口与实际启动任务使用同一候选人策略，预测结果至少在串行和条件网关场景一致。
- `PROCESS_START_USER` 旧流程不受 `PROCESS_APPROVE_USER` 新协议影响。

### P1 必须通过

- 真实委派任务进入 `PENDING`，被委派人处理后恢复 owner，不能直接完成主任务。
- 转派仍保持现有待办可见性和兼容接口行为。
- 前加签、后加签、减签的父子任务状态、评论、本地镜像和 Flowable 状态一致。

## 12. 测试要求

必须新增或补充以下测试类型：

- 候选人 SPI 注册、参数校验、停用用户过滤、空审批人兜底和发起人跳过单测。
- 发布前校验的合法模型、缺策略、缺参数、非法自选节点回归测试。
- `PROCESS_APPROVE_USER` 正常审批、非法后继节点、重复覆盖、并行后继和字符串用户 ID 测试。
- 变量合并测试：当前节点提交空变量、提交部分变量、提交新变量三种情况。
- 退回路径测试：串行、条件分支、并行分支、会签、退回后直送和管理员回退。
- 预测接口测试：候选策略与实际任务分配结果一致，条件未知时返回不确定状态。
- P1 委派/加减签测试必须在对应能力实施时追加，不得用未实现的 Mock 结果代替。

本轮仅提交 Spec 时，验证范围为 `git diff --check` 和文档链接/状态一致性检查；不启动 Admin、Flow、MySQL、Redis，也不执行真实流程数据变更。

## 13. 迁移完成定义

当 P0 的候选人策略、发布门禁、审批人自选下一节点、精确退回和下一节点预测均通过验收，并且旧 `flow-enhancement-suite` 回归测试保持通过时，本 Spec 的核心目标完成。

P1 委派、加签和减签是否实施，以真实业务场景和 Flowable 7.0.1 回归结果为准；在未实施前，相关配置项不得在前端显示为可用操作。
