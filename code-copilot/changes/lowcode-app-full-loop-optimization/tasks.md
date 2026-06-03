# 任务清单：lowcode-app-full-loop-optimization
> status: proposed
> created: 2026-06-01
> 拆分顺序：边界确认 → 数据模型 → 后端协议 → 流程运行 → 触发器动作 → 消息/报表/权限 → 前端配置入口 → 示例验收 → 构建归档 → Phase 9 运行态 BUG 与配置体验优化 → Phase 10 定时触发闭环
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
| Phase 9 | BUG 与体验优化 | Task 23-33 | 动态菜单选中态、填报入口、编号规则、主流程合并、变量候选、自动发起按钮、触发器前置对象、闭环步骤条 |
| Phase 10 | 定时触发闭环 | Task 34 | 低频扫描、批量上限、到期字段筛选、同日去重、执行日志 |

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
| Task 23 | Phase 9 | 动态应用菜单归属和 open-info 协议 | completed | P0 |
| Task 24 | Phase 9 | 前端菜单选中态和应用入口跳转稳定化 | completed | P0 |
| Task 25 | Phase 9 | 运行态打开模式和单据填报入口 | completed | P0 |
| Task 26 | Phase 9 | 单据编号规则和状态映射后端协议 | completed | P0 |
| Task 27 | Phase 9 | 单据设置面板体验重构 | completed | P0 |
| Task 28 | Phase 9 | 主流程配置合并和兼容迁移 | completed | P0 |
| Task 29 | Phase 9 | 流程变量候选项和自动映射建议 | completed | P0 |
| Task 30 | Phase 9 | 流程绑定面板和标题模板体验重构 | completed | P0 |
| Task 31 | Phase 9 | 运行态自动发起流程按钮 | completed | P0 |
| Task 32 | Phase 9 | 触发器新增前置对象和动作配置优化 | completed | P0 |
| Task 33 | Phase 9 | 单据闭环步骤条、发布检查和验证回填 | completed | P0 |
| Task 34 | Phase 10 | 定时触发扫描器和到期提醒配置 | completed | P0 |

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

---

## Phase 9：BUG 与体验优化

### Task 23: 动态应用菜单归属和 open-info 协议

**状态**: completed

**目标**: 后端返回稳定的应用菜单归属和最终打开路由，消除 `/app-center/app/{id}` 中转导致的菜单选中态丢失。

**涉及文件**:
- 修改 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/vo/businessapp/BusinessAppOpenInfoVO.java` — 增加 `activeMenuKey`、`menuResourceId`、`targetRoute`、`runtimeOpenMode`、`appName`。
- 修改 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/businessapp/BusinessAppDTO.java` — 接收入口运行态打开模式。
- 修改 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/vo/businessapp/BusinessAppVO.java` — 回显入口运行态打开模式和菜单资源 ID。
- 修改 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessAppOpenService.java` — 统一生成目标路由、菜单归属和入口校验结果。
- 修改 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessAppService.java` — 保存 `options.runtimeOpenMode`，同步菜单时保存或回填菜单归属。
- 修改 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/MenuRegisterAdapter.java` — 如需更新已有动态菜单 path，必须保留原 `sys_resource.id` 和角色授权。
- 可选新增 `forge/db/migration/V1.0.55__fix_dynamic_business_app_menu_route.sql` — 仅当已有菜单 path 需要批量修复时新增，脚本必须按菜单 ID 或 path 防重复。

**关键改动**:
- `RUNTIME` 应用 open-info 返回最终可访问路由，例如 `/ai/crud-page/{configKey}?appId={id}&menuKey={menuKey}`。
- `activeMenuKey` 优先使用动态应用菜单自身 path 或资源 ID 派生 key，不使用父级 `/app-center`。
- `runtimeOpenMode` 允许值为 `LIST`、`CREATE_FORM`、`DETAIL`；空值按 `LIST` 兼容。
- 外链、H5、IFRAME、API 入口继续复用现有安全校验，不新增绕过。
- 如果 Flyway 修复历史菜单，只能 `UPDATE` 已有资源，不允许删除重建导致角色授权丢失。

**验收标准**:
- `/ai/business/app/{id}/open-info` 对已发布 `RUNTIME` 应用返回 `targetRoute`、`activeMenuKey`、`menuResourceId` 和 `runtimeOpenMode`。
- 已同步过的动态应用菜单保留原资源 ID，角色菜单授权不丢失。
- 未发布对象、缺失 `configKey` 或运行配置不可用时，open-info 返回明确 message，前端可展示下一步。
- 后端接口仍带现有权限控制和租户隔离，不返回敏感配置。

**执行结果**:
- 已在 `BusinessAppDTO`、`BusinessAppVO`、`BusinessAppOpenInfoVO` 增加 `runtimeOpenMode`、`targetRoute`、`menuResourceId`、`activeMenuKey` 协议字段。
- `BusinessAppService` 已将 `runtimeOpenMode` 保存到 `ai_business_app.options`，并在 RUNTIME 应用菜单同步时使用最终运行页路径 `/ai/crud-page/{configKey}?appId=...&runtimeOpenMode=...`；已有菜单通过 `menuResourceId` 或 perms 更新，不删除重建。
- `BusinessAppOpenService` 已返回带 `appId/menuKey/menuResourceId/runtimeOpenMode/mode/title` 的 `targetRoute`，并保留原运行配置、安全和权限校验。
- 本轮未新增 Flyway：数据结构未变，历史菜单会在应用入口保存、状态更新或同步菜单时按原资源 ID 更新。

### Task 24: 前端菜单选中态和应用入口跳转稳定化

**状态**: completed

**目标**: 点击动态应用菜单、刷新运行页和 Tab 切换时，当前业务应用菜单稳定选中，父级目录只展开不闪动为选中项。

**涉及文件**:
- 修改 `forge-admin-ui/src/composables/useMenu.js` — `activeKey` 优先读取 `route.query.menuKey`、`route.query.appId`、`route.meta.parentKey`。
- 修改 `forge-admin-ui/src/utils/menu-utils.js` — 路由匹配支持 query 中的菜单归属，保留原 path 匹配兜底。
- 修改 `forge-admin-ui/src/views/app-center/app-entry.vue` — 调用 open-info 后跳转到带 `appId/menuKey/menuResourceId` 的 `targetRoute`。
- 修改 `forge-admin-ui/src/views/ai/crud-page.vue` — 页面标题和 Tab 标题优先使用应用名称，刷新后恢复菜单归属。
- 修改 `forge-admin-ui/src/api/business-app.js` — open-info 类型字段和目标路由字段对齐。

**关键改动**:
- 应用入口跳转使用 `router.replace({ path, query })` 保留菜单归属 query。
- `useMenu` 的当前 key 解析顺序为：显式 `menuKey` → `appId` 对应动态菜单 → `meta.parentKey` → 当前路径匹配。
- 展开态可以包含父级套件目录，但选中态只落在当前动态应用菜单。
- 浏览器刷新 `/ai/crud-page/{configKey}?appId=...&menuKey=...` 后不依赖中转页即可恢复菜单选中。

**验收标准**:
- 点击 CRM 商机动态菜单不再短暂选中 `/app-center` 或“应用总览”。
- 刷新运行页后，侧边栏仍选中当前商机菜单。
- 浏览器后退、前进和切换 Tab 后，菜单选中态与当前业务应用一致。
- 没有 `appId/menuKey` 的普通动态 CRUD 页面仍按原路径选中。

**执行结果**:
- `useMenu.activeKey` 已优先识别 `menuKey/menuResourceId/appId`，点击动态应用菜单时自动把当前菜单 key、资源 ID 和标题写入路由 query。
- `permission.js` 已在动态路由注册时剥离菜单 path 中的 query，避免 `/ai/crud-page/{configKey}?mode=...` 被注册成非法 route path。
- `tab-guard.js`、`page-title-guard.js` 已支持按 `menuKey` 或 `title` 恢复动态应用 Tab/浏览器标题。
- `app-entry.vue` 兼容历史 `/app-center/app/{id}` 中转页，跳转时使用 `targetRoute` 并保留菜单归属 query。
- 本轮未修改 `menu-utils.js` 和 `business-app.js`；现有路径归一化已满足 query 剥离，API 方法签名无需变化。

### Task 25: 运行态打开模式和单据填报入口

**状态**: completed

**目标**: 应用入口支持列表管理和新增填报两种主要运行模式，业务单据填报类入口点击后直接打开新增表单。

**涉及文件**:
- 修改 `forge-admin-ui/src/views/app-center/components/AppEditorDrawer.vue` — 增加“运行态打开模式”控件，默认值按应用类型和单据配置建议。
- 修改 `forge-admin-ui/src/api/business-app.js` — 保存和回显 `runtimeOpenMode`。
- 修改 `forge-admin-ui/src/views/ai/crud-page.vue` — 解析 `mode=create`，配置加载完成后触发新增表单。
- 修改 `forge-admin-ui/src/components/ai-form/AiCrudPage.vue` — 暴露或复用新增动作入口，支持父页面触发一次性打开新增表单。
- 修改 `forge-admin-ui/src/components/ai-form/AiCrudPageProps.js` — 如现有 props 不足，补充 `initialMode` 或 `autoOpenCreate`。
- 修改 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessAppOpenService.java` — 生成 `CREATE_FORM` 目标路由 query。

**关键改动**:
- `LIST` 打开 `/ai/crud-page/{configKey}`，保留现有列表管理体验。
- `CREATE_FORM` 打开 `/ai/crud-page/{configKey}?mode=create&appId=...&menuKey=...`，页面只自动打开一次新增表单。
- 新增保存成功后默认回到列表并刷新；后续如支持停留详情或继续新增，配置放入 `options.createSuccessAction`。
- 未发布对象、运行配置缺失、单据必填字段缺失时展示配置缺口，不进入空白表单。

**验收标准**:
- 普通管理类入口默认打开列表。
- 单据填报类入口打开后自动弹出新增表单。
- 新增保存成功后列表刷新并能看到新记录。
- 手动关闭新增弹窗后刷新页面不会重复提交，也不会无限弹窗。

**执行结果**:
- `AppEditorDrawer.vue` 已增加运行态打开模式控件：`LIST`、`CREATE_FORM`、`DETAIL`；新建入口名称包含“填报/申请/提交/录入/上报/登记”时默认建议 `CREATE_FORM`。
- `crud-page.vue` 已支持 `mode=create` 自动调用运行页新增表单，支持 `mode=detail&recordId=...` 打开详情；新增保存成功后移除 `mode=create` 并停留在列表。
- `SimpleCrudTemplate.vue`、`MasterDetailCrudTemplate.vue`、`TreeCrudTemplate.vue` 已透出 `showAdd/showDetail/refresh`，保证不同运行模板都能响应填报入口。
- 本轮复用了 `AiCrudPage` 已有 `showAdd` 暴露能力，未新增 `AiCrudPageProps`；新增成功后的列表刷新沿用 `AiCrudPage` 原有提交成功逻辑。

### Task 26: 单据编号规则和状态映射后端协议

**状态**: completed

**目标**: 后端提供编号规则内置变量、预览校验和结构化状态映射协议，让前端不再要求用户手填隐式语法。

**涉及文件**:
- 修改 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/controller/BusinessDocumentController.java` — 增加编号变量和预览接口。
- 修改 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessDocumentConfigService.java` — 编号规则校验、状态映射标准化、主流程摘要回显。
- 修改 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/businessapp/BusinessDocumentConfigDTO.java` — 扩展 `noRuleTemplate`、结构化 `statusMapping`、状态动作策略。
- 修改 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/vo/businessapp/BusinessDocumentConfigVO.java` — 回显编号规则预览结果、状态映射表和主流程摘要。
- 新增 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/businessapp/BusinessDocumentNoRulePreviewDTO.java` — 编号规则预览入参。
- 新增 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/vo/businessapp/BusinessDocumentNoRuleTokenVO.java` — 内置变量定义。
- 新增 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/vo/businessapp/BusinessDocumentNoRulePreviewVO.java` — 预览结果和错误明细。

**关键改动**:
- 新增 `GET /ai/business/document/no-rule/tokens`，返回 `${yyyy}`、`${yyyyMM}`、`${yyyyMMdd}`、`${HHmmss}`、`${seq}`、`${seq:4}`、`${suiteCode}`、`${objectCode}`、`${starter}`、`${deptCode}`、`${field:<fieldCode>}`。
- 新增 `POST /ai/business/document/no-rule/preview`，只用样例数据生成预览，不占用真实序列号。
- 保存单据配置时校验未知变量、空序列、非法字符和长度风险。
- 状态映射保存为标准状态、存储值、展示名称、标签类型、允许编辑、允许删除、允许发起流程。
- 状态字段绑定字典时，状态存储值优先来自字典项；未绑定字典时允许默认状态集并返回维护字典建议。

**验收标准**:
- 编号变量接口返回分组、说明、示例和可插入文本。
- 编号预览接口对未知变量返回具体变量名和修复建议。
- 保存状态映射后，再次查询能按结构化表格完整回显。
- 预览接口不会写入业务表、不会更新序列号、不会产生真实单据编号。

**执行结果**:
- `BusinessDocumentController` 已新增 `GET /ai/business/document/no-rule/tokens` 和 `POST /ai/business/document/no-rule/preview`，均保留 `ai:businessObject:design` 权限。
- `BusinessDocumentConfigDTO/VO` 已扩展 `noRuleTemplate`、`statusMappingRows`、`statusActionPolicy`、`noRulePreview` 和 `mainFlowSummary`。
- `BusinessDocumentConfigService` 已支持编号变量渲染和未知变量校验；预览只使用样例数据和内存序号，不写业务表、不占用真实序列。
- 状态映射继续写入兼容 `status_mapping` 简单映射，同时把结构化状态行写入 `options.statusMappingRows` 并回显。

### Task 27: 单据设置面板体验重构

**状态**: completed

**目标**: 把单据设置页从松散技术表单调整为可操作工作台，完成编号规则、状态映射和发布摘要的产品化配置。

**涉及文件**:
- 修改 `forge-admin-ui/src/views/app-center/components/designer/BusinessDocumentPanel.vue` — 重构布局，移除独立默认流程选择器。
- 新增 `forge-admin-ui/src/views/app-center/components/designer/DocumentNoRuleEditor.vue` — 编号规则变量插入、预览和错误展示。
- 新增 `forge-admin-ui/src/views/app-center/components/designer/DocumentStatusMappingTable.vue` — 状态映射表格、默认状态集、从字典生成。
- 新增 `forge-admin-ui/src/views/app-center/components/designer/DocumentConfigSummary.vue` — 单据配置和发布检查摘要。
- 修改 `forge-admin-ui/src/api/business-app.js` — 接入编号变量、编号预览、单据配置保存回显。

**关键改动**:
- 页面布局采用左侧基础配置、中间状态映射、右侧摘要与下一步，不使用嵌套卡片堆叠。
- 编号规则输入支持点击变量插入到光标位置、实时预览和错误定位。
- 状态映射支持“一键使用默认状态集”和“一键从字典生成映射”。
- 主流程只展示摘要和“去配置主流程”按钮，不再在单据设置页出现第二个流程下拉。
- 保存按钮必须有 loading、防重复提交和保存后摘要刷新。

**验收标准**:
- 用户可不记忆 `${seq:4}` 语法完成编号规则配置。
- 状态映射可通过默认状态集或字典生成，仍允许高级用户手动调整。
- 单据设置页没有大面积空白、重复说明、嵌套卡片和裸 JSON。
- 保存失败能定位到具体字段或映射行，保存成功后主流程摘要和发布摘要刷新。

**执行结果**:
- `BusinessDocumentPanel.vue` 已重构为基础配置、编号规则、状态映射和右侧摘要工作台，移除单据页独立默认流程下拉。
- 新增 `DocumentNoRuleEditor.vue`，支持编号变量分组展示、点击插入、实时预览和错误/警告展示。
- 新增 `DocumentStatusMappingTable.vue`，支持默认状态集、从状态字段本地选项生成、结构化动作策略编辑。
- 新增 `DocumentConfigSummary.vue`，展示状态字段、编号规则、状态映射和主流程摘要，并提供“去配置主流程”入口。
- `object-designer.[objectCode].vue` 已把单据摘要的主流程入口切到 `automation` 面板。

### Task 28: 主流程配置合并和兼容迁移

**状态**: completed

**目标**: 合并“单据设置默认流程”和“流程与自动化流程模型”，以主流程绑定为唯一事实来源，同时兼容历史配置。

**涉及文件**:
- 修改 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessFlowService.java` — 主流程绑定读取、保存、完整度检查和兼容转换。
- 修改 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessDocumentConfigService.java` — 单据配置返回主流程摘要，保存时同步兼容字段。
- 修改 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessObjectPublishService.java` — 发布检查以 `ai_business_binding` 主流程为准。
- 修改 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/businessapp/BusinessFlowBindingDTO.java` — 明确 `startMode`、`titleTemplate`、`variableMappings` 和状态回写配置。
- 修改 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/vo/businessapp/BusinessFlowBindingVO.java` — 返回主流程摘要、完整度、缺口列表和兼容来源。
- 可选新增 `forge/db/migration/V1.0.56__backfill_business_main_flow_binding.sql` — 将仅存在于 `default_flow_key` 的历史配置回填到 `ai_business_binding`，脚本必须防重复。

**关键改动**:
- 事实来源顺序：有效 `ai_business_binding(target_type=OBJECT,target_code=objectCode,binding_type=FLOW)` → 历史 `ai_business_document_config.default_flow_key` 兼容读取。
- 保存主流程后，同步更新 `ai_business_document_config.default_flow_key` 作为兼容快照。
- 单据设置页不保存新的独立流程选择；流程与自动化页负责主流程模型、变量映射、标题模板、发起方式、状态回写。
- 发布检查提示“未配置主流程”“变量映射缺失”“发起方式未配置”“手动按钮缺失”“触发器缺失”等具体项。

**验收标准**:
- 历史只配置 `default_flow_key` 的对象仍能打开并看到主流程摘要。
- 新保存主流程后，单据设置页和流程与自动化页显示一致。
- `default_flow_key` 和 `ai_business_binding` 不一致时，以 `ai_business_binding` 为准，并在保存后回写兼容字段。
- 发布检查不再把两处流程配置当作两个独立要求。

**执行结果**:
- `BusinessFlowService#getFlowBinding` 已按 `ai_business_binding(FLOW)` 优先读取；无主绑定但存在历史 `default_flow_key` 时返回兼容主流程 VO。
- `BusinessFlowService#saveFlowBinding` 保存主流程后同步回写 `ai_business_document_config.default_flow_key` 作为兼容快照。
- `BusinessDocumentConfigService` 查询单据配置时返回 `mainFlowSummary`，且保存单据配置时不再以单据页流程选择作为新事实来源。
- `BusinessObjectPublishService` 发布检查已改为读取主流程摘要，提示主流程未配置或变量映射缺失等缺口。
- 本轮未新增 Flyway：未改变表结构；历史 `default_flow_key` 通过读取兼容和保存回写完成渐进迁移。

### Task 29: 流程变量候选项和自动映射建议

**状态**: completed

**目标**: 选择流程模型后，后端自动提取流程变量候选项并给出字段映射建议，减少手填变量名。

**涉及文件**:
- 修改 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/controller/BusinessFlowController.java` — 增加流程变量候选项接口。
- 修改 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessFlowService.java` — 组装变量候选项、字段候选项和映射建议。
- 新增 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessFlowVariableResolver.java` — 解析 BPMN、动态表单和内置变量。
- 新增 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/vo/businessapp/BusinessFlowVariableCandidateVO.java` — 流程变量候选项。
- 新增 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/vo/businessapp/BusinessFlowVariableMappingSuggestionVO.java` — 自动映射建议。
- 修改 `forge/forge-flow/forge-flow-client/src/main/java/com/mdframe/forge/flow/client/FlowClient.java` 或业务侧适配服务 — 获取流程模型详情时只返回变量解析所需字段。
- 参考 `forge/forge-flow/forge-flow-server/src/main/java/com/mdframe/forge/flow/controller/FlowModelController.java` — 确认模型详情、BPMN XML 和表单 JSON 可获取字段。

**关键改动**:
- 新增 `GET /ai/business/flow/model/{modelKey}/variables?objectCode=...`。
- 候选来源包括 BPMN 条件表达式 `${variable}`、审批人表达式、`flowable:assignee`、`candidateUsers`、`candidateGroups`、动态表单字段、模板变量配置和内置变量。
- 内置变量至少包括 `initiator`、`startUserId`、`businessKey`、`recordId`、`objectCode`、`deptId`、`deptManager`。
- BPMN XML 解析必须禁用外部实体，避免 XXE；解析失败时返回空候选和可读警告，不阻断高级自定义变量。
- 自动映射按字段编码、字段名称和常见别名匹配，例如 `amount`、`opportunityAmount`、`deptManager`。
- 接口不得返回历史流程实例变量值、业务记录数据或敏感表单数据。

**验收标准**:
- 选择已发布流程模型后接口返回变量候选项、来源、显示名和推荐映射。
- 无法解析 BPMN 时仍返回内置变量并给出 warning。
- 推荐映射不会覆盖用户已手动配置的映射。
- 变量候选项接口有权限控制，普通无配置权限用户不可调用。

**执行结果**:
- `BusinessFlowController` 已新增 `GET /ai/business/flow/model/{modelKey}/variables?objectCode=...`，保留 `ai:businessFlow:config` 权限。
- 新增 `BusinessFlowVariableResolver`，解析内置变量、BPMN 表达式/审批人属性、流程动态表单字段和业务对象字段候选项。
- BPMN XML 解析已禁用外部实体；解析失败时返回内置变量和 warning，不阻断高级自定义变量。
- 新增 `BusinessFlowVariableCandidateVO`、`BusinessFlowVariableMappingSuggestionVO`，并在 `FlowClient` 增加按模型 Key 获取模型详情能力。
- `BusinessFlowBindingPanel.vue` 已接入变量候选项，下拉选择流程变量，并支持“一键应用推荐映射”且不覆盖已有映射。

### Task 30: 流程绑定面板和标题模板体验重构

**状态**: completed

**目标**: 将流程与自动化页面升级为主流程配置工作台，变量映射和标题模板均支持选择、插入、预览和高级输入。

**涉及文件**:
- 修改 `forge-admin-ui/src/views/app-center/components/designer/BusinessFlowBindingPanel.vue` — 主流程模型、发起方式、变量映射、标题模板和状态回写重构。
- 修改 `forge-admin-ui/src/views/app-center/components/TriggerActionConfigPanel.vue` — `START_FLOW` 动作复用流程变量候选项和标题模板编辑器。
- 新增 `forge-admin-ui/src/views/app-center/components/designer/FlowVariableMappingEditor.vue` — 字段到流程变量映射表。
- 新增 `forge-admin-ui/src/views/app-center/components/designer/TemplateVariableEditor.vue` — 标题模板变量插入和预览。
- 修改 `forge-admin-ui/src/api/business-app.js` — 接入主流程保存、变量候选项、映射建议和标题预览所需接口。
- 修改 `forge-admin-ui/src/api/flow.js` — 流程模型选择仅展示可发起或已发布流程。

**关键改动**:
- 流程变量列默认使用下拉选择，候选项来自 Task 29；高级模式允许新增自定义变量。
- 提供“一键应用推荐映射”，只填充空映射，不覆盖用户已有选择。
- 标题模板支持插入单据字段和内置变量，展示实时预览，例如 `商机名称-商机审批-20260602`。
- 保存前校验未知变量，错误文案必须包含具体变量名和修复入口。
- 旧协议 `field/variable` 读取时转换为 `formField/flowVariable`，保存时统一写新协议。

**验收标准**:
- 用户选择流程模型后，变量映射右侧自动出现流程变量候选项。
- 一键推荐能为常见字段生成映射，用户保存刷新后不丢失。
- 标题模板可通过点击变量完成，不需要手写 `${fieldCode}`。
- 触发器里的发起流程动作和主流程配置使用同一套变量选择体验。

**执行结果**:
- 新增 `FlowVariableMappingEditor.vue`，统一支持单据字段选择、流程变量下拉、自定义变量和推荐映射补空。
- 新增 `TemplateVariableEditor.vue`，支持单据字段/流程变量 token 点击插入、标题模板实时预览和未知变量提示。
- `BusinessFlowBindingPanel.vue` 已重构为主流程配置工作台，选择流程后加载候选变量和映射建议。
- `TriggerActionConfigPanel.vue` 的 `START_FLOW` 动作已复用同一套变量映射和标题模板编辑器。

### Task 31: 运行态自动发起流程按钮

**状态**: completed

**目标**: 主流程发起方式为手动或手动+自动时，运行态根据状态、权限和配置自动生成“发起流程”按钮并绑定真实发起事件。

**涉及文件**:
- 修改 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessDocumentRuntimeService.java` — 返回自动流程按钮可用性和禁用原因。
- 修改 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessFlowService.java` — `/ai/business/flow/start` 支持运行态按钮发起并返回状态摘要。
- 修改 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessPermissionService.java` — 校验 `ai:businessFlow:start` 和对象按钮权限。
- 修改 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/vo/businessapp/BusinessDocumentRuntimeVO.java` — 增加 `runtimeActions` 或自动按钮摘要。
- 修改 `forge-admin-ui/src/views/ai/crud-page.vue` — 加载单据运行态并向 `AiCrudPage` 传递自动按钮配置。
- 修改 `forge-admin-ui/src/components/ai-form/AiCrudPage.vue` — 渲染自动“发起流程”按钮，补齐 `START_FLOW` 执行分支。
- 修改 `forge-admin-ui/src/views/app-center/components/designer/BusinessActionDesigner.vue` — 自定义“发起流程”改为覆盖配置，避免重复按钮。

**关键改动**:
- 主流程 `startMode` 为 `MANUAL` 或 `BOTH` 且记录为草稿、无进行中流程、有权限时展示按钮。
- 流程中、已通过、已驳回、已撤回、已关闭默认隐藏或禁用，并展示可理解原因。
- 点击按钮调用 `POST /ai/business/flow/start`，payload 包含 `objectCode`、`recordId`，优先使用主流程绑定，不要求前端传完整流程配置。
- 发起成功后刷新列表行、单据运行态、流程状态和操作按钮。
- 如果自定义操作中已配置 `START_FLOW` 覆盖项，按相同位置去重，只保留一个按钮。

**验收标准**:
- 草稿商机记录行操作或详情页自动出现“发起流程”。
- 点击后真实创建 Flowable 流程实例，并把单据状态刷新为流程中。
- 进行中记录不再出现可重复发起的按钮。
- 无权限用户前端不可见且直接调用接口会被后端拒绝。

**执行结果**:
- `BusinessDocumentRuntimeVO` 新增 `runtimeActions`，返回运行态自动按钮、禁用态和禁用原因。
- `BusinessDocumentRuntimeService` 按主流程配置、发起方式、单据状态、进行中流程和动作权限生成“发起流程”按钮。
- `AiCrudConfigRenderVO/AiCrudConfigService` 已向运行页透出 `objectCode`。
- `crud-page.vue` 加载列表后为当前页记录附加 `_runtimeActions`，`AiCrudPage.vue` 合并行级动作并执行 `START_FLOW`。
- 运行态按钮调用 `POST /ai/business/flow/start`，payload 为 `{ objectCode, recordId }`，发起成功后刷新列表。

### Task 32: 触发器新增前置对象和动作配置优化

**状态**: completed

**目标**: 修复新增触发器空对象提交报错，确保全局新增先选业务对象，对象上下文新增自动带入对象和字段。

**涉及文件**:
- 修改 `forge-admin-ui/src/views/app-center/trigger.vue` — 新增弹窗第一步选择业务对象，支持 query 默认对象和保存前阻断。
- 修改 `forge-admin-ui/src/views/app-center/components/TriggerActionConfigPanel.vue` — 业务对象确定后再加载字段、条件和动作配置。
- 修改 `forge-admin-ui/src/views/app-center/components/designer/BusinessObjectDesignerShell.vue` — 从对象设计器进入触发器配置时携带 `objectCode`。
- 修改 `forge-admin-ui/src/views/app-center/object-designer.[objectCode].vue` — 主链路跳转触发器时传递当前对象上下文。
- 修改 `forge-admin-ui/src/api/business-app.js` — 触发器新增、更新前端请求保持对象编码必填。
- 修改 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessTriggerService.java` — 后端错误文案保持明确，场景模板补齐默认动作配置。

**关键改动**:
- 全局触发器页点击新增时，如果没有上下文，弹窗先选择业务对象；未选对象时禁用后续字段和保存按钮。
- 从对象设计器进入时，URL query 携带 `objectCode`，新增弹窗默认选中当前对象并立即加载字段。
- 业务对象变化后重新加载字段选项、条件构造器和动作配置，清理不属于新对象的旧字段映射。
- 选择场景模板后自动填充事件类型、动作类型、默认条件和默认动作配置；缺少主流程时提示“请先配置主流程”。
- 前端提交前校验 `objectCode`、`triggerName`、`eventType`、`actionType`、`actionConfig`，不把空对象编码交给后端。

**验收标准**:
- 全局新增触发器未选择对象时无法保存，并提示“请先选择业务对象”。
- 从商机对象进入触发器配置后，新增弹窗自动带入 `OPPORTUNITY` 并能看到商机字段。
- 切换业务对象后，字段下拉、条件行和动作映射不会保留上一个对象的非法字段。
- 后端仍保留对象编码校验，接口被绕过时返回明确业务错误。

**执行结果**:
- `trigger.vue` 支持 `route.query.objectCode` 作为对象上下文，新建触发器默认带入当前对象。
- 全局新增触发器时对象选择变为前置条件，未选对象保存按钮禁用，提交前提示“请先选择业务对象”。
- 切换业务对象时清空条件、动作配置和字段选项，并按新对象重新加载字段。
- `TriggerActionConfigPanel` 接收 `objectCode`，发起流程动作按当前对象加载流程变量候选和推荐映射。

### Task 33: 单据闭环步骤条、发布检查和验证回填

**状态**: completed

**目标**: 把单据设置、主流程、发起方式、触发器、发布检查和试运行串成连续配置链路，并完成 Phase 9 验证记录。

**涉及文件**:
- 修改 `forge-admin-ui/src/views/app-center/components/designer/BusinessObjectDesignerShell.vue` — 增加“单据闭环配置”步骤条和完成度摘要。
- 修改 `forge-admin-ui/src/views/app-center/object-designer.[objectCode].vue` — 步骤切换、下一步动作、试运行入口和发布检查刷新。
- 修改 `forge-admin-ui/src/views/app-center/components/designer/BusinessPublishChecklist.vue` — 汇总单据字段、编号规则、状态映射、主流程、变量映射、手动按钮、触发器、菜单入口和权限缺口。
- 修改 `forge-admin-ui/src/views/app-center/components/ReadinessPanel.vue` — 展示闭环配置完成度和运行缺口。
- 修改 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessObjectPublishService.java` — 发布检查增加 Phase 9 缺口项。
- 修改 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessObjectReadinessService.java` — 运行态 readiness 增加菜单入口、打开模式和主流程完整度。
- 修改 `code-copilot/changes/lowcode-app-full-loop-optimization/test-spec.md` — 如进入 `/test` 阶段，补充 Phase 9 增量测试清单。
- 修改 `code-copilot/changes/lowcode-app-full-loop-optimization/execution-log.md` — 记录 Phase 9 实际执行命令、结果、警告和跳过项。
- 修改 `code-copilot/changes/lowcode-app-full-loop-optimization/spec.md` — 回填 Phase 9 实施结论和风险。
- 修改 `code-copilot/changes/lowcode-app-full-loop-optimization/tasks.md` — 回填 Task 23-33 状态和验证结果。

**关键改动**:
- 步骤条顺序固定为：单据设置 → 主流程 → 发起方式 → 自动化触发器 → 发布检查 → 试运行。
- 每一步展示完成状态、阻断项、警告项和下一步按钮。
- 单据设置保存后，如果主流程未配置，下一步直接进入主流程配置。
- 主流程保存后，如果发起方式包含 `TRIGGER` 或 `BOTH`，下一步进入触发器配置并自动带入对象和流程。
- 试运行入口至少包含打开填报入口、创建草稿记录、手动发起流程、查看流程状态。
- 执行阶段验证前必须读取 `code-copilot/rules/automated-testing-standard.md`，复用已有 `test-spec.md`、`execution-log.md`、`spec.md`、`tasks.md` 做增量验证。

**验收标准**:
- 业务对象设计器能连续完成单据设置、主流程、触发器、发布检查和试运行，不需要用户在多个页面来回找入口。
- 发布检查能定位菜单选中态、运行态打开模式、编号规则、状态映射、变量映射、自动按钮和触发器缺口。
- CRM 商机配置完成后，点击动态菜单、填报、发起流程和状态刷新主路径通过。
- Phase 9 的后端构建、前端构建、关键接口和页面验证结果追加到 `execution-log.md`。

**执行结果**:
- `BusinessObjectDesignerShell.vue` 新增“单据闭环配置”步骤条，并支持从更多菜单进入触发器配置。
- `object-designer.[objectCode].vue` 串联单据保存、主流程保存、触发器配置、发布检查和试运行入口。
- `BusinessObjectPublishService` 发布检查补充应用入口、菜单资源、运行打开模式、编号规则、状态映射、自动按钮和触发器缺口项。
- `BusinessObjectReadinessService` 增加 `DOCUMENT_CLOSURE_STATUS`，提示单据、主流程和触发器就绪缺口。
- 已新增 `test-spec.md` 并在 `execution-log.md` 追加 Task 30-33 的后端编译、前端构建和 diff 检查结果。

## Phase 10：定时触发闭环

### Task 34: 定时触发扫描器和到期提醒配置

**状态**: completed

**目标**: 将触发器 `SCHEDULE` 类型从配置占位补齐为真实执行能力，并控制扫描频率、批量大小和重复触发风险。

**涉及文件**:
- 修改 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessTriggerSchedulerService.java` — 新增低频扫描任务，注册到系统任务调度中心，默认每 5 分钟扫描一次启用的定时触发器，并通过 Redisson 全局锁作为并发兜底。
- 修改 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/pom.xml` — 引入 `forge-starter-job`，让低代码模块只依赖任务注解，不直接耦合任务插件实现。
- 修改 `forge/forge-framework/forge-plugin-parent/forge-plugin-job/src/main/java/com/mdframe/forge/plugin/job/config/ScheduleConfig.java`、`JobProperties.java` — 补齐 Quartz 集群配置和可配置项。
- 修改 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessTriggerExecutor.java` — 暴露单条触发器同步执行入口，复用现有动作执行和日志，使记录级锁覆盖完整执行过程。
- 修改 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessTriggerService.java` — 统一 `SCHEDULE/SCHEDULED` 枚举，补齐定时触发器查询、日志去重和扫描时间回写方法。
- 修改 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/mapper/BusinessTriggerMapper.java`、`BusinessTriggerMapper.xml` — XML 查询启用的定时触发器。
- 修改 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/mapper/BusinessTriggerLogMapper.java`、`BusinessTriggerLogMapper.xml` — 查询同一触发器、记录、事件在时间窗口内是否已成功执行。
- 修改 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/DynamicCrudService.java` — 提供定时扫描候选记录的受控读取方法，按到期字段区间查询并限制批量。
- 修改 `forge-admin-ui/src/views/app-center/trigger.vue` — 定时触发配置统一使用 `SCHEDULE`，增加到期字段、提前天数、回看天数、单批数量和最小执行间隔配置。

**关键改动**:
- 不为每个触发器创建秒级 Quartz 任务；采用一个系统任务中心低频扫描任务，默认 cron `0 0/5 * * * ?`。
- 集群部署下优先依赖 Quartz JDBC 集群调度；同时保留 Redisson 全局扫描锁和记录级执行锁，防止手动触发、补偿执行或配置缺失导致重复动作。
- 若运行环境没有 `RedissonClient`，扫描任务仍依赖任务中心集群触发和日志去重；如需手动并发兜底建议启用 Redis/Redisson。
- 每轮最多扫描有限数量的启用定时触发器，单个触发器默认最多处理 50 条候选记录，上限 200。
- 到期提醒必须配置 `schedule.dueField`，扫描器只按到期字段区间查询，不做无条件全表扫描。
- 同一 `triggerId + recordId + eventType` 默认按自然日去重，成功或 TODO 后当天不重复触发。
- 触发器可配置 `schedule.minIntervalMinutes`，默认至少 5 分钟，避免同一触发器被过于频繁扫描。
- 无候选数据、缺少运行配置或配置缺失时也会回写本次扫描时间，避免同一触发器被每轮重复优先扫描。
- 兼容历史前端写入的 `SCHEDULED`，保存时规范为 `SCHEDULE`。

**验收标准**:
- 新增“到期提醒”触发器后，保存的 `triggerType` 为 `SCHEDULE`，且事件类型为 `SCHEDULED_DUE`。
- 扫描任务默认在系统任务中心每 5 分钟执行一次，不进行秒级轮询，并可在任务中心调整 cron。
- 未配置到期字段的定时触发器不会全表扫描，并写出可读警告日志。
- 候选记录只按到期字段窗口读取，单轮处理量受 `batchSize` 限制。
- 同一记录当天成功触发后不会重复发送消息或重复发起动作。
- 后端编译、前端构建和 `git diff --check` 通过，跳过项按测试标准记录。

**执行结果**:
- 已新增 `BusinessTriggerSchedulerService`，使用 `@ScheduledJob(name="lowcodeBusinessTriggerScanJob", group="LOWCODE", cron="0 0/5 * * * ?")` 注册到系统任务调度中心，并通过 `forge.business-trigger.schedule.cluster-lock-enabled=true` 保留 Redisson 全局扫描锁兜底。
- 已为 `forge-plugin-generator` 增加 `forge-starter-job` 依赖，低代码模块不直接依赖 `forge-plugin-job`。
- 已补齐 Quartz 集群配置：`forge.job.clustered` 默认 `true`，并支持线程数、集群 checkin 间隔、misfire 阈值和表前缀配置。
- 已增加记录级分布式锁，锁 key 按 `tenantId + triggerId + recordId + naturalDay` 生成；定时触发动作改为同步执行，确保锁覆盖动作和执行日志写入。
- 已补齐到期字段白名单校验、候选记录批量上限、同日 `SUCCESS/TODO` 去重、扫描时间回写和 `SCHEDULED` 兼容归一。
- 已在前端触发器配置中补齐到期字段、提前天数、回看天数、批量大小和最小执行间隔配置。
- 验证已完成：后端 generator/job 模块 compile 通过，前端 `pnpm build` 通过，`git diff --check` 通过；未启动后端/数据库/多实例做真实任务注册、Quartz 集群触发和扫描落库验证，已在执行日志中记录为跳过。
