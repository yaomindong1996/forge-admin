# 任务清单：lowcode-app-full-loop-optimization
> status: proposed
> created: 2026-06-01
> 拆分顺序：边界确认 → 数据模型 → 后端协议 → 流程运行 → 触发器动作 → 消息/报表/权限 → 前端配置入口 → 示例验收 → 构建归档
> 原则：每个任务可独立提交；查询 SQL 写 Mapper XML；分页参数使用 `pageNum/pageSize`；内置数据 `tenant_id=1`；Flyway 脚本必须防重复；普通业务用户不直接看到 JSON/Schema/configKey。

## 前置条件

- [x] 已确认本变更不修改 `lowcode-business-app-platform-business-closure` 已完成任务，只在新变更下继续推进。
- [x] 已确认“审批”统一归入流程引擎，不再保留独立审批引擎产品入口。
- [x] 已确认导入导出保留为业务对象运行页能力，不作为独立引擎统计或展示。
- [x] 已确认移动端中心、集成中心首期处理方式：仅隐藏菜单和入口，保留历史页面与路由兼容。
- [x] 已确认单据状态字段默认建议：`documentStatus` / `document_status`，允许高级模式映射到已有字段。
- [x] 已确认商机审批必须真实调用已有 Flowable 流程，不接受模拟流程实例 ID。
- [x] 已确认企微、飞书、钉钉本阶段只返回 `TODO` 执行状态和日志，不做真实网络请求。

## 阶段总览

| 阶段 | 目标 | 包含任务 | 交付结果 |
|------|------|----------|----------|
| Phase 0 | 边界冻结 | Task 0 | 消除待澄清项，确认本阶段硬边界 |
| Phase 1 | 数据模型 | Task 1-2 | 单据配置、流程实例关联、菜单收敛脚本 |
| Phase 2 | 后端协议 | Task 3-5 | 单据配置、单据运行态、流程运行接口 |
| Phase 3 | 流程闭环 | Task 6-7 | 手动/触发器发起流程、结果回写、审批兼容层 |
| Phase 4 | 触发器闭环 | Task 8-10 | XML 查询、场景模板、创建记录/更新字段/Webhook TODO |
| Phase 5 | 消息报表权限 | Task 11-13 | 站内消息、三方通道 TODO、业务指标、按钮权限 |
| Phase 6 | 前端入口 | Task 14-18 | 入口收敛、单据设置、流程动作、触发器、报表页面 |
| Phase 7 | 示例验收 | Task 19-20 | 商机闭环、离职申请闭环 |
| Phase 8 | 验证归档 | Task 21-22 | 编译构建、Spec/Task 回填 |

## 任务总览

| Task | 阶段 | 名称 | 状态 | 优先级 |
|------|------|------|------|--------|
| Task 0 | Phase 0 | 业务边界和待澄清项确认 | completed | P0 |
| Task 1 | Phase 1 | 单据和流程关联 Flyway 脚本 | completed | P0 |
| Task 2 | Phase 1 | 单据与流程关联实体、Mapper、VO/DTO | completed | P0 |
| Task 3 | Phase 2 | 单据配置后端接口 | completed | P0 |
| Task 4 | Phase 2 | 单据运行态后端接口 | completed | P0 |
| Task 5 | Phase 2 | 流程绑定后端协议收敛 | completed | P0 |
| Task 6 | Phase 3 | 真实发起流程和流程实例关联 | completed | P0 |
| Task 7 | Phase 3 | 流程结果回写与审批兼容层 | completed | P0 |
| Task 8 | Phase 4 | 触发器查询 XML 化和配置校验 | completed | P0 |
| Task 9 | Phase 4 | 触发器场景模板与变量映射协议修复 | completed | P0 |
| Task 10 | Phase 4 | 触发器动作执行闭环 | completed | P0 |
| Task 11 | Phase 5 | 消息推送和第三方通道 TODO 适配 | completed | P1 |
| Task 12 | Phase 5 | 业务报表指标后端能力 | completed | P1 |
| Task 13 | Phase 5 | 数据权限与按钮权限摘要 | completed | P1 |
| Task 14 | Phase 6 | 应用中心和引擎中心入口收敛 | completed | P0 |
| Task 15 | Phase 6 | 前端 API 与单据设置面板 | completed | P0 |
| Task 16 | Phase 6 | 流程与自定义操作前端配置 | completed | P0 |
| Task 17 | Phase 6 | 触发器场景化前端配置 | completed | P0 |
| Task 18 | Phase 6 | 业务报表看板前端展示 | completed | P1 |
| Task 19 | Phase 7 | CRM 商机闭环初始化和验收 | completed | P0 |
| Task 20 | Phase 7 | 离职申请闭环初始化和验收 | completed | P1 |
| Task 21 | Phase 8 | 编译、构建、接口和页面验证 | completed | P0 |
| Task 22 | Phase 8 | Spec、任务和验收记录回填 | completed | P0 |

---

## Phase 0：业务基线

### Task 0: 业务边界和待澄清项确认

**目标**: 在编码前确认会影响数据模型、菜单删除方式和外部推送行为的硬边界。

**涉及文件**:
- `code-copilot/changes/lowcode-app-full-loop-optimization/spec.md` — 回填待澄清项和确认记录。
- `code-copilot/changes/lowcode-app-full-loop-optimization/tasks.md` — 根据确认结果调整任务优先级和范围。

**确认事项**:
- 移动端中心、集成中心首期仅隐藏菜单还是删除页面文件。
- 单据状态字段默认命名是否采用 `documentStatus` / `document_status`。
- 流程回调采用 Flowable 监听器桥接还是新增业务回调接口。
- 商机示例字段和审批流程是否作为内置样板写入 Flyway。
- 企微、飞书、钉钉是否严格只保留 TODO 日志，不做网络请求。

**验收标准**:
- `spec.md` 第 13 章待澄清项全部有结论。
- 后续任务不再同时出现“审批引擎”和“流程引擎”双入口。

---

## Phase 1：数据模型

### Task 1: 单据和流程关联 Flyway 脚本

**目标**: 补齐单据模式和流程实例关联的持久化结构，并收敛菜单入口。

**涉及文件**:
- `forge/db/migration/V1.0.47__add_business_document_flow_closure.sql` — 新增单据配置表、流程实例关联表，扩展触发器字段，隐藏移动端中心和集成中心菜单。

**数据结构**:
- 新增 `ai_business_document_config`。
- 新增 `ai_business_flow_instance_link`。
- 扩展 `ai_business_trigger`：`scenario_type`, `blocking_mode`, `developer_mode`。
- 扩展 `ai_business_trigger_log`：`todo_code`, `correlation_id`。
- 隐藏或废弃 `sys_resource` 中 `/app-center/mobile`、`/app-center/integration` 可见菜单。

**脚本约束**:
- 使用 `CREATE TABLE IF NOT EXISTS`。
- 新增列使用 `information_schema.columns` 判断。
- 菜单更新只写新脚本，不修改历史 `V1.0.27`、`V1.0.31`。
- 所有内置数据 `tenant_id=1`。

**验收标准**:
- 空库和已有库均可执行脚本。
- `ai_business_document_config.object_code`、`ai_business_flow_instance_link.business_key` 有租户维度唯一约束或防重索引。
- 移动端中心、集成中心不再作为普通可见菜单出现。

### Task 2: 单据与流程关联实体、Mapper、VO/DTO

**目标**: 为单据配置和流程关联建立 Java 数据模型和 XML 查询入口。

**涉及文件**:
- 新增 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/domain/entity/AiBusinessDocumentConfig.java` — 单据配置实体。
- 新增 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/domain/entity/AiBusinessFlowInstanceLink.java` — 单据流程实例关联实体。
- 新增 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/mapper/BusinessDocumentConfigMapper.java` — 单据配置 Mapper。
- 新增 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/mapper/BusinessFlowInstanceLinkMapper.java` — 流程关联 Mapper。
- 新增 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/BusinessDocumentConfigMapper.xml` — 单据配置 XML 查询。
- 新增 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/BusinessFlowInstanceLinkMapper.xml` — 流程关联 XML 查询。
- 新增 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/businessapp/BusinessDocumentConfigDTO.java` — 单据配置保存 DTO。
- 新增 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/businessapp/BusinessFlowStartDTO.java` — 发起流程 DTO。
- 新增 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/businessapp/BusinessFlowCallbackDTO.java` — 流程回调 DTO。
- 新增 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/vo/businessapp/BusinessDocumentConfigVO.java` — 单据配置 VO。
- 新增 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/vo/businessapp/BusinessDocumentRuntimeVO.java` — 单据运行态 VO。
- 新增 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/vo/businessapp/BusinessFlowRuntimeVO.java` — 流程运行态 VO。

**关键签名**:
```java
public interface BusinessDocumentConfigMapper extends BaseMapper<AiBusinessDocumentConfig> {
    AiBusinessDocumentConfig selectByObjectCode(@Param("tenantId") Long tenantId,
                                                @Param("objectCode") String objectCode);

    AiBusinessDocumentConfig selectByObjectId(@Param("tenantId") Long tenantId,
                                              @Param("objectId") Long objectId);
}

public interface BusinessFlowInstanceLinkMapper extends BaseMapper<AiBusinessFlowInstanceLink> {
    AiBusinessFlowInstanceLink selectLatestByBusinessKey(@Param("tenantId") Long tenantId,
                                                         @Param("businessKey") String businessKey);

    AiBusinessFlowInstanceLink selectByProcessInstanceId(@Param("tenantId") Long tenantId,
                                                         @Param("processInstanceId") String processInstanceId);
}
```

**验收标准**:
- 新实体继承 `TenantEntity` 或按现有实体约定包含租户字段。
- 所有复杂查询写在 XML。
- DTO/VO 不暴露明文密钥、表结构 JSON 等开发者细节给普通运行态接口。

---

## Phase 2：后端协议

### Task 3: 单据配置后端接口

**目标**: 提供业务对象单据模式配置的查询和保存能力。

**涉及文件**:
- 新增 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/controller/BusinessDocumentController.java` — 单据配置和运行态接口入口。
- 新增 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessDocumentConfigService.java` — 单据配置服务。
- 修改 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessObjectPublishService.java` — 发布检查加入单据状态字段校验。
- 修改 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessObjectDesignerService.java` — 设计器加载时带出单据配置摘要。

**关键签名**:
```java
@GetMapping("/config/{objectId}")
@SaCheckPermission("ai:businessObject:design")
public RespInfo<BusinessDocumentConfigVO> getConfig(@PathVariable Long objectId) { }

@PutMapping("/config/{objectId}")
@SaCheckPermission("ai:businessObject:design")
public RespInfo<Void> saveConfig(@PathVariable Long objectId,
                                 @RequestBody BusinessDocumentConfigDTO dto) { }

public BusinessDocumentConfigVO getConfig(Long objectId);

public void saveConfig(Long objectId, BusinessDocumentConfigDTO dto);
```

**配置校验**:
- 启用单据模式时必须存在 `statusField`。
- `statusField`、`starterField`、`ownerField` 必须来自对象字段或系统字段。
- `statusMapping` 至少包含 `DRAFT`、`IN_PROCESS`、`APPROVED`、`REJECTED`。
- `defaultFlowKey` 为空时允许保存，但发布检查给出警告。

**验收标准**:
- 已启用单据模式的对象可以保存并回显配置。
- 字段不存在时保存失败并返回明确业务错误。
- 发布检查能定位“单据状态字段缺失”。

### Task 4: 单据运行态后端接口

**目标**: 运行页可查询当前记录的单据状态、流程状态和可执行动作。

**涉及文件**:
- 新增 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessDocumentRuntimeService.java` — 单据运行态服务。
- 修改 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/controller/BusinessDocumentController.java` — 增加运行态接口。
- 修改 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessPermissionService.java` — 增加单据动作权限判断。
- 修改 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/DynamicCrudService.java` — 如需读取记录状态，复用安全读取方法，不新增裸 SQL。

**关键签名**:
```java
@GetMapping("/{objectCode}/{recordId}/runtime")
@SaCheckPermission("ai:businessDocument:view")
public RespInfo<BusinessDocumentRuntimeVO> runtime(@PathVariable String objectCode,
                                                   @PathVariable Long recordId) { }

public BusinessDocumentRuntimeVO getRuntime(String objectCode, Long recordId);

public List<String> resolveAvailableActions(String objectCode,
                                            Long recordId,
                                            Map<String, Object> recordData);
```

**运行态输出**:
- `documentEnabled`
- `documentStatus`
- `documentStatusLabel`
- `businessKey`
- `flowStatus`
- `processInstanceId`
- `availableActions`
- `nextAction`
- `message`

**验收标准**:
- 普通 CRUD 对象返回 `documentEnabled=false`，不影响原页面。
- 单据对象未保存记录时不允许发起流程。
- 无按钮权限时对应动作不出现在 `availableActions`。

### Task 5: 流程绑定后端协议收敛

**目标**: 将流程绑定从触发器路径下收敛为业务流程接口，并统一变量映射协议。

**涉及文件**:
- 新增 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/controller/BusinessFlowController.java` — `/ai/business/flow` 新接口。
- 新增 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/businessapp/BusinessFlowBindingDTO.java` — 流程绑定 DTO。
- 新增 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/vo/businessapp/BusinessFlowBindingVO.java` — 流程绑定 VO。
- 修改 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessFlowService.java` — 增加新协议方法，兼容旧配置。
- 修改 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/controller/BusinessTriggerController.java` — 旧 `/trigger/flow/binding` 接口标记兼容转发。

**关键签名**:
```java
@GetMapping("/binding/{objectCode}")
@SaCheckPermission("ai:businessFlow:config")
public RespInfo<BusinessFlowBindingVO> getBinding(@PathVariable String objectCode) { }

@PutMapping("/binding/{objectCode}")
@SaCheckPermission("ai:businessFlow:config")
public RespInfo<Void> saveBinding(@PathVariable String objectCode,
                                  @RequestBody BusinessFlowBindingDTO dto) { }

public BusinessFlowBindingVO getFlowBinding(String objectCode);

public void saveFlowBinding(String objectCode, BusinessFlowBindingDTO dto);
```

**协议规则**:
- 新协议变量映射字段固定为 `formField`、`flowVariable`。
- 读取旧配置时兼容 `field`、`variable` 并转换输出新协议。
- 保存时只落新协议。
- `binding_type` 使用 `FLOW`，不再新增 `APPROVAL`。

**验收标准**:
- 新旧接口都能读取已有 `FLOW` 绑定。
- 旧 `field/variable` 配置不会导致流程变量为空。
- 新保存数据不再出现 `field/variable`。

---

## Phase 3：流程闭环

### Task 6: 真实发起流程和流程实例关联

**目标**: 手动按钮和触发器发起流程统一调用真实 FlowClient，并写入单据流程关联。

**涉及文件**:
- 修改 `BusinessFlowService.java` — 实现 `startDocumentFlow`，保留旧 `startFlow` 兼容。
- 修改 `BusinessFlowController.java` — 增加 `/start` 和 `/status`。
- 修改 `BusinessFlowInstanceLinkMapper.java` / `BusinessFlowInstanceLinkMapper.xml` — 查询和防重复。
- 修改 `BusinessDocumentRuntimeService.java` — 发起前校验单据运行状态和权限。
- 修改 `DynamicCrudService.java` 或新增内部读取方法 — 根据对象记录读取变量快照。

**关键签名**:
```java
@PostMapping("/start")
@SaCheckPermission("ai:businessFlow:start")
public RespInfo<BusinessFlowRuntimeVO> start(@RequestBody BusinessFlowStartDTO dto) { }

@GetMapping("/status/{objectCode}/{recordId}")
@SaCheckPermission("ai:businessFlow:view")
public RespInfo<BusinessFlowRuntimeVO> status(@PathVariable String objectCode,
                                             @PathVariable Long recordId) { }

public BusinessFlowRuntimeVO startDocumentFlow(BusinessFlowStartDTO dto);

public BusinessFlowRuntimeVO getFlowStatus(String objectCode, Long recordId);
```

**发起逻辑**:
- 校验对象存在、单据配置启用、记录存在。
- 校验流程绑定存在且流程模型 Key 不为空。
- 根据变量映射从记录数据生成 `variablesSnapshot`。
- 调用 `FlowClient.startProcess(...)`。
- 写入 `ai_business_flow_instance_link`。
- 将单据状态更新为 `IN_PROCESS` 对应值。

**验收标准**:
- 成功返回 `processInstanceId`。
- 重复发起同一业务键时返回明确提示或复用未完成实例，不产生重复流程。
- FlowClient 未注入时返回明确错误，不再模拟成功。

### Task 7: 流程结果回写与审批兼容层

**目标**: 流程结束后回写单据状态，并将旧审批接口转发到流程运行服务。

**涉及文件**:
- 修改 `BusinessFlowController.java` — 增加 `/callback`。
- 修改 `BusinessFlowService.java` — 增加流程回调处理。
- 修改 `BusinessEventPublisher.java` — 增加流程通过、流程驳回业务事件发布。
- 修改 `BusinessApprovalRuntimeService.java` — 兼容转发到 `BusinessFlowService`，删除模拟返回。
- 修改 `BusinessApprovalController.java` — 保留接口但声明兼容层，内部调用流程服务。

**关键签名**:
```java
@PostMapping("/callback")
@SaCheckPermission("ai:businessFlow:callback")
public RespInfo<Void> callback(@RequestBody BusinessFlowCallbackDTO dto) { }

public void handleFlowCallback(BusinessFlowCallbackDTO dto);

public Long startApproval(String targetCode, Long recordId);
```

**回写规则**:
- `APPROVED` 回写为单据配置中的通过状态。
- `REJECTED` 回写为单据配置中的驳回状态。
- `CANCELED` 回写为撤回状态。
- 回调必须按 `processInstanceId` 查关联记录。
- 已结束流程再次回调必须幂等。

**验收标准**:
- `/ai/business/approval/start` 不再返回模拟 `recordId`。
- 流程通过后单据状态改变，并发布流程通过事件。
- 流程驳回后单据状态改变，并发布流程驳回事件。

---

## Phase 4：触发器闭环

### Task 8: 触发器查询 XML 化和配置校验

**目标**: 触发器管理满足项目查询 SQL 规范，并支持场景筛选字段。

**涉及文件**:
- 修改 `BusinessTriggerMapper.java` — 增加分页查询和日志分页 XML 方法。
- 修改 `BusinessTriggerMapper.xml` — 增加 `selectTriggerPage`、`selectTriggerLogPage`。
- 修改 `BusinessTriggerService.java` — 移除分页查询中的 `LambdaQueryWrapper`。
- 修改 `AiBusinessTrigger.java` — 增加 `scenarioType`、`blockingMode`、`developerMode` 字段。
- 修改 `AiBusinessTriggerLog.java` — 增加 `todoCode`、`correlationId` 字段。

**关键签名**:
```java
Page<AiBusinessTrigger> selectTriggerPage(Page<AiBusinessTrigger> page,
                                          @Param("tenantId") Long tenantId,
                                          @Param("objectCode") String objectCode,
                                          @Param("scenarioType") String scenarioType);

Page<AiBusinessTriggerLog> selectTriggerLogPage(Page<AiBusinessTriggerLog> page,
                                                @Param("tenantId") Long tenantId,
                                                @Param("triggerId") Long triggerId);
```

**验收标准**:
- `/ai/business/trigger/page?pageNum=1&pageSize=10` 正常返回。
- Service 中不再用复杂 wrapper 拼触发器分页查询。
- 场景类型为空时查询全部，非空时按 `scenario_type` 筛选。

### Task 9: 触发器场景模板与变量映射协议修复

**目标**: 后端提供触发器业务场景模板，并统一 `START_FLOW` 变量映射协议。

**涉及文件**:
- 新增 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/vo/businessapp/BusinessTriggerScenarioTemplateVO.java` — 场景模板 VO。
- 修改 `BusinessTriggerController.java` — 新增 `/scenario-templates`。
- 修改 `BusinessTriggerService.java` — 新增模板和保存校验方法。
- 修改 `BusinessTriggerExecutor.java` — 兼容旧变量映射字段。
- 修改 `forge-admin-ui/src/views/app-center/trigger.vue` — 前端保存使用 `formField`、`flowVariable`。

**关键签名**:
```java
@GetMapping("/scenario-templates")
@SaCheckPermission("ai:businessTrigger:list")
public RespInfo<List<BusinessTriggerScenarioTemplateVO>> scenarioTemplates() { }

public List<BusinessTriggerScenarioTemplateVO> scenarioTemplates();

public String normalizeActionConfig(String actionType, String actionConfig);
```

**模板范围**:
- 新增记录后发起流程。
- 状态变更后发送消息。
- 流程通过后创建记录。
- 字段变更后更新字段。
- 到期提醒。

**验收标准**:
- 新建 `START_FLOW` 触发器保存后的 JSON 使用 `formField`、`flowVariable`。
- 历史 `field`、`variable` 配置仍能执行。
- 场景模板接口不暴露技术 JSON 给普通用户。

### Task 10: 触发器动作执行闭环

**目标**: 补齐创建记录、更新字段动作；Webhook 和三方通道保持明确 TODO 状态并记录日志。

**涉及文件**:
- 修改 `BusinessTriggerExecutor.java` — 实现 `executeCreateRecordAction`、`executeUpdateFieldAction`，规范 `executeWebhookAction`。
- 修改 `DynamicCrudService.java` — 提供受控的内部创建/更新方法。
- 修改 `BusinessTriggerService.java` — 保存执行日志时写入 `correlationId`、`todoCode`。
- 修改 `BusinessEvent.java` — 增加流程事件常量。
- 修改 `BusinessEventPublisher.java` — 增加发布流程事件方法。

**关键签名**:
```java
public Map<String, Object> insertInternal(String configKey, Map<String, Object> data);

public void updateFieldsInternal(String configKey, Long id, Map<String, Object> fields);

public void publishFlowApproved(String objectCode,
                                String recordId,
                                Map<String, Object> recordData);

public void publishFlowRejected(String objectCode,
                                String recordId,
                                Map<String, Object> recordData);
```

**动作规则**:
- 创建记录只允许使用目标对象已发布 `configKey`。
- 字段映射源字段必须来自事件数据。
- 更新字段目标字段必须在模型字段中存在。
- Webhook 动作返回 `status=TODO`，写入 `todoCode=WEBHOOK_NOT_IMPLEMENTED`，不发送网络请求。

**验收标准**:
- 离职申请通过后可自动创建离职交接记录。
- 更新字段动作不会接受任意未校验字段名。
- Webhook 动作日志显示 TODO，而不是成功发送。

---

## Phase 5：消息、报表、权限

### Task 11: 消息推送和第三方通道 TODO 适配

**目标**: 站内消息可真实发送，企微/飞书/钉钉保留通道和适配器状态。

**涉及文件**:
- 修改 `BusinessTriggerExecutor.java` — 扩展接收人规则和通道处理。
- 新增 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessMessageChannelService.java` — 业务消息通道查询和状态判断。
- 新增 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/mapper/BusinessMessageChannelMapper.java` — 消息通道 Mapper。
- 新增 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/BusinessMessageChannelMapper.xml` — 消息通道 XML 查询。
- 修改 `AiBusinessMessageChannel.java` — 补齐状态和配置引用字段映射。

**关键签名**:
```java
public BusinessMessageChannelStatus resolveChannel(String channelCode);

public void sendInternalMessage(JSONObject config, BusinessEvent event);

public JSONObject buildThirdPartyTodoResult(String channelType, String channelCode);
```

**接收人规则**:
- `STARTER`：流程发起人。
- `OWNER`：单据负责人。
- `CREATOR`：记录创建人。
- `USERS`：指定用户。
- `ROLES`：指定角色。
- `DEPTS`：指定部门。

**验收标准**:
- 站内消息仍调用 `MessageService.send`。
- 企业微信、飞书、钉钉返回 TODO 状态并写触发器日志。
- 不在日志输出密钥、Token、Webhook Secret。

### Task 12: 业务报表指标后端能力

**目标**: 报表从固定 `status` 分组升级为业务对象可配置指标。

**涉及文件**:
- 修改 `BusinessStatsController.java` — 增加指标配置和指标数据接口。
- 修改 `BusinessStatsService.java` — 增加计数、金额汇总、状态/阶段分布、流程结果分布。
- 新增 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/businessapp/BusinessStatsMetricQueryDTO.java` — 指标查询 DTO。
- 新增 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/vo/businessapp/BusinessStatsMetricVO.java` — 指标数据 VO。
- 修改 `AiCrudConfigMapper.xml` 或新增字段查询 XML — 校验报表字段来自模型。

**关键签名**:
```java
@GetMapping("/{configKey}/metrics")
@SaCheckPermission("ai:businessStats:view")
public RespInfo<List<BusinessStatsMetricVO>> metrics(@PathVariable String configKey,
                                                     BusinessStatsMetricQueryDTO query) { }

public List<BusinessStatsMetricVO> metrics(String configKey, BusinessStatsMetricQueryDTO query);

public BusinessStatsMetricVO sumAmount(String configKey, String amountField);
```

**指标范围**:
- 记录总数。
- 今日新增、本月新增。
- 状态/阶段分布。
- 金额字段求和，单位分转元由前端展示。
- 流程通过/驳回数量。

**验收标准**:
- 非模型字段作为指标字段时返回业务错误。
- 金额字段不使用 decimal 计算新增结构。
- 商机可返回阶段分布和金额汇总。

### Task 13: 数据权限与按钮权限摘要

**目标**: 单据动作和报表/触发器入口都能按权限展示和校验。

**涉及文件**:
- 修改 `BusinessPermissionService.java` — 增加单据动作权限摘要。
- 修改 `BusinessObjectActionService.java` — 自定义操作保存时校验权限标识和动作类型。
- 修改 `BusinessObjectPublishService.java` — 发布检查加入按钮权限缺口。
- 修改 `BusinessObjectDesignerController.java` — 权限摘要接口返回单据动作权限。
- 修改 `BusinessReadinessItemVO.java` 或新增 `BusinessPermissionSummaryVO.java` — 承载权限摘要。

**关键签名**:
```java
public BusinessPermissionSummaryVO documentActionSummary(Long objectId);

public boolean hasDocumentActionPermission(String objectCode, String actionCode);

public BusinessReadinessItemVO permissionSummary(Long objectId);
```

**权限范围**:
- 保存、删除、提交、撤回。
- 发起流程、查看流程。
- 执行触发器。
- 查看报表。

**验收标准**:
- 无 `ai:businessFlow:start` 权限时后端拒绝发起流程。
- 前端隐藏按钮后，直接调用接口仍被拦截。
- 发布检查能提示未配置关键按钮权限。

---

## Phase 6：前端入口

### Task 14: 应用中心和引擎中心入口收敛

**目标**: 前端和菜单展示不再出现独立审批引擎、导入导出引擎、移动端中心、集成中心。

**涉及文件**:
- 修改 `forge-admin-ui/src/views/app-center/index.vue` — 首页入口收敛，业务对象卡片增加单据/流程摘要入口。
- 修改 `forge-admin-ui/src/views/app-center/engines.vue` — 仅展示流程、触发器、消息、权限、报表。
- 修改 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessEngineSummaryService.java` — 后端引擎统计移除 `APPROVAL`、`IMPORT_EXPORT`。
- 修改 `forge-admin-ui/src/router/index.js` — 保留历史路由但不新增可见入口。
- 修改 `forge/db/migration/V1.0.47__add_business_document_flow_closure.sql` — 菜单可见性与权限同步。

**验收标准**:
- 普通菜单中不显示移动端中心、集成中心。
- 引擎中心不显示审批引擎和导入导出。
- 历史 URL 访问不导致前端构建失败。

### Task 15: 前端 API 与单据设置面板

**目标**: 对象设计器可配置单据模式和状态字段。

**涉及文件**:
- 修改 `forge-admin-ui/src/api/business-app.js` — 新增 document/flow/scenario/stats 接口。
- 新增 `forge-admin-ui/src/views/app-center/components/designer/BusinessDocumentPanel.vue` — 单据设置面板。
- 修改 `forge-admin-ui/src/views/app-center/components/designer/BusinessObjectDesignerShell.vue` — 左侧导航增加“单据设置”。
- 修改 `forge-admin-ui/src/views/app-center/object-designer.[objectCode].vue` — 接入单据面板数据加载和保存。
- 修改 `BusinessPermissionFlowPanel.vue` — 保留数据权限，避免承担单据设置主职责。

**前端 API 函数**:
```javascript
export function businessDocumentConfig(objectId) { }
export function saveBusinessDocumentConfig(objectId, data) { }
export function businessDocumentRuntime(objectCode, recordId) { }
export function businessFlowBinding(objectCode) { }
export function saveBusinessFlowBinding(objectCode, data) { }
export function startBusinessDocumentFlow(data) { }
export function businessFlowStatus(objectCode, recordId) { }
export function businessTriggerScenarioTemplates() { }
```

**验收标准**:
- 设计器可保存并回显单据名称、状态字段、发起人字段、负责人字段。
- 字段下拉来自业务对象字段，不手写 options。
- 普通用户不看到 `configKey`、表名等开发者字段。

### Task 16: 流程与自定义操作前端配置

**目标**: 自定义操作从“发起审批”升级为“发起流程”，支持流程选择、变量映射和按钮权限。

**涉及文件**:
- 修改 `forge-admin-ui/src/views/app-center/components/designer/BusinessActionDesigner.vue` — 操作类型和配置表单调整。
- 新增 `forge-admin-ui/src/views/app-center/components/designer/BusinessFlowBindingPanel.vue` — 流程绑定和变量映射面板。
- 修改 `BusinessObjectDesignerShell.vue` — 增加“流程与自动化”导航项。
- 修改 `object-designer.[objectCode].vue` — 接入流程绑定面板。
- 修改 `forge-admin-ui/src/api/flow.js` 或复用现有 `flowApi.getModelList` — 只选择已发布流程。

**前端规则**:
- 操作类型显示“发起流程”，底层值使用 `START_FLOW` 或统一映射到后端流程动作。
- 变量映射保存为 `formField`、`flowVariable`。
- 流程标题模板可填写 `${fieldCode}`。
- 未选择流程时发布检查给出提示。

**验收标准**:
- 不再出现“发起审批”作为新动作按钮主文案。
- 新增流程动作后保存、刷新仍能回显变量映射。
- 变量映射字段名与后端一致。

### Task 17: 触发器场景化前端配置

**目标**: 触发器页面从技术 JSON 输入升级为业务场景模板和结构化配置。

**涉及文件**:
- 修改 `forge-admin-ui/src/views/app-center/trigger.vue` — 接入场景模板、条件构造器、动作结构化配置。
- 新增 `forge-admin-ui/src/views/app-center/components/TriggerConditionBuilder.vue` — 条件构造器。
- 新增 `forge-admin-ui/src/views/app-center/components/TriggerActionConfigPanel.vue` — 动作配置面板。
- 修改 `forge-admin-ui/src/api/business-app.js` — 补充场景模板接口。

**配置能力**:
- 选择场景模板后预填事件类型、动作类型和说明。
- 条件支持字段、操作符、目标值、AND/OR。
- 发起流程动作选择流程和变量映射。
- 创建记录动作选择目标对象和字段映射。
- Webhook 显示“待实现”状态。

**验收标准**:
- 创建“新增商机且金额大于阈值时发起流程”不需要手写 JSON。
- 开发者模式仍可查看高级 JSON。
- 执行日志能展示成功、失败、跳过、TODO。

### Task 18: 业务报表看板前端展示

**目标**: 报表页面能按业务对象展示商机场景指标。

**涉及文件**:
- 修改 `forge-admin-ui/src/views/app-center/stats-dashboard.vue` — 指标卡、阶段分布、金额汇总、流程结果分布。
- 修改 `forge-admin-ui/src/api/business-app.js` — 新增 `businessStatsMetrics`。
- 新增 `forge-admin-ui/src/views/app-center/components/BusinessMetricPanel.vue` — 指标展示组件。
- 修改 `forge-admin-ui/src/views/app-center/suite.[suiteCode].vue` — CRM 套件中增加商机看板入口。

**展示规则**:
- 金额字段以分为单位接收，前端格式化为元。
- 阶段/状态类字段优先使用字典展示；没有字典时展示原值和空值兜底。
- 没有指标配置时展示“暂无可统计字段”，不报错。

**验收标准**:
- 商机看板显示总数、阶段分布、预计金额、审批结果。
- 页面不出现 `undefined`、空标题或技术字段名。
- `pnpm build` 通过。

---

## Phase 7：示例验收

### Task 19: CRM 商机闭环初始化和验收

**目标**: 用 CRM 商机场景验证单据、流程、消息、报表和权限闭环。

**涉及文件**:
- 新增 `forge/db/migration/V1.0.51__seed_crm_opportunity_document_flow.sql` — 初始化商机单据配置、流程绑定、触发器样例、报表入口。
- 修改 `forge/db/migration/V1.0.37__seed_crm_runtime_crud_configs.sql` 不允许；如需修正，新增顺延版本脚本中的 UPDATE/INSERT 防重复脚本。
- 修改 `DynamicCrudController.java`, `DynamicCrudService.java` — 新增后返回创建记录用于触发器拿到 `recordId`。
- 修改 `BusinessTriggerExecutor.java` — 补齐 `gt/gte/lt/lte/in/not_in` 条件比较，支撑金额阈值。
- 修改 `code-copilot/changes/lowcode-app-full-loop-optimization/spec.md` — 回填商机验收记录。
- 修改 `code-copilot/changes/lowcode-app-full-loop-optimization/tasks.md` — 回填执行日志。

**验收场景**:
- 新增商机单据。
- 手动发起流程。
- 新增商机且金额大于阈值时触发器自动发起流程。
- 流程通过后商机状态变为通过。
- 站内消息发送给发起人和负责人。
- 商机看板显示阶段和金额统计。

**验收标准**:
- CRM 商机全链路可用。
- 失败时能从触发器日志、流程关联表、单据运行态定位原因。
- 不依赖企微、飞书、钉钉真实推送。

### Task 20: 离职申请闭环初始化和验收

**目标**: 用离职申请示例验证“工作表事件触发流程并自动生成交接记录”的参考流程。

**涉及文件**:
- 新增 `forge/db/migration/V1.0.52__seed_leave_document_flow_demo.sql` — 初始化离职申请、离职交接示例对象、单据配置、触发器样例。
- 修改 `BusinessTriggerExecutor.java` — 补齐日期比较条件，支撑“最后工作日小于等于指定日期”。
- 修改 `code-copilot/changes/lowcode-app-full-loop-optimization/spec.md` — 回填离职示例验收记录。
- 修改 `code-copilot/changes/lowcode-app-full-loop-optimization/tasks.md` — 回填执行日志。

**验收场景**:
- 新增离职申请记录。
- 条件满足“最后工作日小于等于指定日期”时自动发起流程。
- 部门主管、人事节点由已有流程模型处理。
- 流程通过后自动创建离职交接记录。

**验收标准**:
- 触发器条件不满足时日志为 `SKIPPED`。
- 条件满足且流程通过后创建交接记录。
- 交接记录字段来自离职申请字段映射。

---

## Phase 8：验证归档

### Task 21: 编译、构建、接口和页面验证

**目标**: 完成本阶段最小质量闭环，确保后端、前端、数据库和主流程可验证。

**涉及文件**:
- `code-copilot/changes/lowcode-app-full-loop-optimization/test-spec.md` — 如进入 `/test` 阶段，新增详细测试清单。
- `code-copilot/changes/lowcode-app-full-loop-optimization/execution-log.md` — 记录执行命令和结果。

**验证命令**:
```bash
cd forge && mvn clean compile
```

```bash
cd forge-admin-ui && source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm build
```

**接口验证**:
- `/ai/business/document/config/{objectId}`
- `/ai/business/document/{objectCode}/{recordId}/runtime`
- `/ai/business/flow/start`
- `/ai/business/flow/status/{objectCode}/{recordId}`
- `/ai/business/trigger/page?pageNum=1&pageSize=10`
- `/ai/business/trigger/scenario-templates`
- `/ai/business/stats/{configKey}/metrics`

**验收标准**:
- 后端编译通过。
- 前端构建通过。
- 商机闭环主路径通过。
- 普通动态 CRUD 不启用单据模式时行为不变。

**执行结果**:
- `git diff --check` 通过。
- `mvn -pl forge-admin-server -am package -DskipTests` 通过。
- `pnpm build` 通过；仅保留既有 UnoCSS 图标、CSS `//` 注释、动态/静态导入混用和 chunk size 警告。
- Flyway 已在 dev 库执行到 `1.0.54`，补齐 `sys_flow_template.del_flag` 和商机样板 `deptManager` 流程变量映射。
- 已通过 Flow 服务创建并部署 `leave_multi`，部署 ID 为 `dcbda981-5e14-11f1-a0fd-d67ed5f8e875`。
- `/ai/business/flow/start` 对 `OPPORTUNITY/10` 返回 `code=200`，流程实例 `602ad5c2-5e15-11f1-a0fd-d67ed5f8e875`，单据状态回写为 `IN_PROCESS`。
- 指定接口 `document/config`、`document/runtime`、`flow/status`、`trigger/page`、`trigger/scenario-templates`、`stats/metrics` 均返回 `code=200`。

### Task 22: Spec、任务和验收记录回填

**目标**: 将实际实现、风险和验收结果回填到变更文档，便于 review 和 archive。

**涉及文件**:
- `code-copilot/changes/lowcode-app-full-loop-optimization/spec.md` — 更新执行日志、审查结论、确认记录。
- `code-copilot/changes/lowcode-app-full-loop-optimization/tasks.md` — 标记任务状态、实际改动文件、验证结果。
- `.opencode/memory/pitfalls.md` — 记录新发现的坑，如流程变量协议、菜单隐藏、触发器异步失败定位。
- `.opencode/memory/decisions.md` — 记录“审批归入流程引擎”等项目决策，如确认后尚未沉淀。

**验收标准**:
- 每个 Task 都有状态和实际改动文件。
- 失败或跳过的验证项说明原因。
- 关键决策沉淀到记忆文件。

**执行结果**:
- 已新增 `execution-log.md` 记录 Phase 8 命令、接口、数据库和流程部署结果。
- 已更新 `spec.md`、`tasks.md` 的 Phase 8 执行记录和 HARD-GATE 结论。
- 已向 `.opencode/memory/pitfalls.md` 记录 Flyway 占位符和流程变量映射验证坑。
- 已向 `.opencode/memory/decisions.md` 记录审批能力统一归入 Flowable 流程引擎的产品/架构决策。
