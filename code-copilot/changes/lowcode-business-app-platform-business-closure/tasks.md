# 任务清单：lowcode-business-app-platform-business-closure
> status: in_progress
> created: 2026-05-28
> 拆分顺序：运行就绪度 → CRM 核心对象闭环 → 关系运行视图 → 自助搭建 → 引擎闭环 → 嵌入/H5/集成 → 验收归档
> 原则：每个任务可独立提交；查询 SQL 写 Mapper XML；分页参数使用 `pageNum/pageSize`；内置数据 `tenant_id=1`；Flyway 脚本必须防重复；普通业务用户不直接看到 JSON/Schema/configKey。

## 前置条件

- [ ] 已确认第一阶段 `lowcode-business-app-platform` 保持 completed，不在原变更上继续混改。
- [ ] 已确认本阶段目标是“从入口目录到业务闭环”，不是重写动态 CRUD、流程、报表、消息或移动端底层。
- [ ] 已确认 CRM 第一批可运行对象范围：客户、联系人、商机、合同、回款。
- [ ] 已确认第一个第三方推送闭环优先级：建议先通用 Webhook，再企微/飞书/钉钉适配器。
- [ ] 已确认销售看板首期接入方式：建议作为嵌入应用复用现有报表/大屏入口。
- [ ] 已确认合同审批首期闭环：建议流程存在时真实发起，未配置时给配置路径。

## 阶段总览

| 阶段 | 目标 | 包含任务 | 交付结果 |
|------|------|----------|----------|
| Phase 0 | 业务基线 | Task 0 | 冻结二阶段边界、CRM 对象范围、第三方优先级 |
| Phase 1 | 就绪度 | Task 1-3 | 对象/套件可交付状态、打开校验、页面状态展示 |
| Phase 2 | CRM 运行闭环 | Task 4-7 | CRM 核心表、模型、CRUD 配置、导入导出 |
| Phase 3 | 关系运行视图 | Task 8-9 | 客户/合同/商机详情关联入口 |
| Phase 4 | 自助搭建闭环 | Task 10-12 | 空白、模板、数据库导入、AI 生成路径串联 |
| Phase 5 | 引擎闭环 | Task 13-17 | 审批、报表、消息、权限、触发器最小闭环 |
| Phase 6 | 渠道闭环 | Task 18-20 | 嵌入大屏、移动入口、集成事件/Webhook |
| Phase 7 | 体验与权限 | Task 21-22 | 业务化文案、权限菜单、开发者信息隔离 |
| Phase 8 | 验证归档 | Task 23-24 | 编译构建、业务验收、文档回填 |

## 任务总览

| Task | 阶段 | 名称 | 状态 | 优先级 |
|------|------|------|------|--------|
| Task 0 | Phase 0 | 二阶段业务边界确认 | completed | P0 |
| Task 1 | Phase 1 | 业务对象就绪度数据模型与 VO | completed | P0 |
| Task 2 | Phase 1 | 对象就绪度和套件验收后端接口 | completed | P0 |
| Task 3 | Phase 1 | 对象详情和 CRM 套件验收状态展示 | completed | P0 |
| Task 4 | Phase 2 | CRM 核心运行表 Flyway 脚本 | completed | P0 |
| Task 5 | Phase 2 | CRM 核心对象模型与运行配置初始化 | completed | P0 |
| Task 6 | Phase 2 | 应用入口打开前校验运行配置存在 | completed | P0 |
| Task 7 | Phase 2 | CRM 导入导出和运行页验收 | completed | P0 |
| Task 8 | Phase 3 | 对象关系运行入口后端协议 | completed | P1 |
| Task 9 | Phase 3 | 对象详情关联入口前端展示 | completed | P1 |
| Task 10 | Phase 4 | 自助搭建向导状态模型 | completed | P1 |
| Task 11 | Phase 4 | 模板/数据库导入/AI 生成路径串联 | completed | P1 |
| Task 12 | Phase 4 | 发布应用后自动生成业务入口 | completed | P1 |
| Task 13 | Phase 5 | 引擎中心运行状态汇总接口 | completed | P1 |
| Task 14 | Phase 5 | 合同审批最小闭环 | completed | P0 |
| Task 15 | Phase 5 | 报表和大屏能力入口闭环 | completed | P1 |
| Task 16 | Phase 5 | 消息与触发器最小动作闭环 | completed | P1 |
| Task 17 | Phase 5 | 对象权限能力状态摘要 | completed | P1 |
| Task 18 | Phase 6 | 嵌入应用统一 iframe 容器 | completed | P0 |
| Task 19 | Phase 6 | 移动入口可见范围与打开状态 | completed | P1 |
| Task 20 | Phase 6 | 集成事件订阅、Webhook 推送日志和重试 | completed | P0 |
| Task 21 | Phase 7 | 业务化文案和开发者信息隔离 | completed | P1 |
| Task 22 | Phase 7 | 菜单、权限和普通用户入口收敛 | completed | P1 |
| Task 23 | Phase 8 | 构建、接口和页面联调验证 | completed | P0 |
| Task 24 | Phase 8 | Spec、任务和验收记录回填 | completed | P0 |

---

## Phase 0：业务基线

### Task 0: 二阶段业务边界确认

**目标**: 在编码前确认本阶段验收口径，避免把“完整 CRM 产品”“完整移动端”“完整三方平台适配”混入本阶段。

**涉及文件**:
- `code-copilot/changes/lowcode-business-app-platform-business-closure/spec.md` — 回填待澄清项和确认记录。
- `code-copilot/changes/lowcode-business-app-platform-business-closure/tasks.md` — 根据确认结果调整任务优先级。

**确认事项**:
- CRM 第一批可运行对象：客户、联系人、商机、合同、回款。
- 线索、合同明细、跟进记录、销售任务是否进入第一批可运行对象；默认作为关系对象和后续增强。
- 第一个外部推送闭环：默认通用 Webhook。
- 销售看板：默认作为嵌入应用接入已有报表/大屏。
- 合同审批：默认在流程存在时真实发起，未配置时给配置路径。

**验收标准**:
- `spec.md` 第 16 章待澄清项均有明确结论。
- 后续任务不再出现范围冲突。

---

## Phase 1：运行就绪度

### Task 1: 业务对象就绪度数据模型与 VO

**目标**: 定义业务对象、应用入口和能力挂接的真实可运行状态模型。

**涉及文件**:
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/vo/businessapp/BusinessObjectReadinessVO.java` — 新增对象就绪度 VO。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/vo/businessapp/BusinessSuiteAcceptanceVO.java` — 新增套件验收 VO。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/vo/businessapp/BusinessReadinessItemVO.java` — 新增就绪度项 VO。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/constant/BusinessReadinessStatus.java` — 新增状态常量。

**关键状态**:
- `REGISTERED`：已登记，但不一定可运行。
- `CONFIGURED`：已配置必要参数，但未验证运行。
- `RUNNABLE`：可运行。
- `MISSING`：缺少必要配置。
- `ERROR`：配置存在但运行校验失败。

**关键签名**:
```java
public class BusinessObjectReadinessVO {
    private Long objectId;
    private String suiteCode;
    private String objectCode;
    private String objectName;
    private String overallStatus;
    private Integer score;
    private List<BusinessReadinessItemVO> items;
    private String nextAction;
    private String nextActionLabel;
    private String nextActionUrl;
}
```

**验收标准**:
- 状态命名和前端展示一致。
- 不把 `ai_business_app.config_key` 存在直接视为可运行。

### Task 2: 对象就绪度和套件验收后端接口

**目标**: 后端能判断对象和套件是否达到业务可交付标准。

**涉及文件**:
- `BusinessObjectController.java` — 新增 `/readiness` 接口。
- `BusinessSuiteController.java` — 新增 `/acceptance` 接口。
- `BusinessObjectReadinessService.java` — 新增对象就绪度服务。
- `BusinessSuiteAcceptanceService.java` — 新增套件验收服务。
- `BusinessAppMapper.java` / `BusinessAppMapper.xml` — 查询对象运行应用入口和 `ai_crud_config` 状态。
- `AiCrudConfigMapper.java` / `AiCrudConfigMapper.xml` — 补充按 `configKey` 查询已发布配置方法，如已有则复用。

**关键签名**:
```java
@GetMapping("/{id}/readiness")
@SaCheckPermission("ai:businessReadiness:view")
public RespInfo<BusinessObjectReadinessVO> readiness(@PathVariable Long id) { }

@GetMapping("/{suiteCode}/acceptance")
@SaCheckPermission("ai:businessAcceptance:view")
public RespInfo<BusinessSuiteAcceptanceVO> acceptance(@PathVariable String suiteCode) { }
```

**就绪度检查项**:
- 业务对象存在且启用。
- 低代码模型存在。
- 物理数据表存在。
- 已发布 `ai_crud_config` 存在。
- 应用入口存在且启用。
- 导入导出接口可用。
- 对象关系已登记。
- 能力挂接状态可判断。

**验收标准**:
- CRM 客户对象返回 `RUNNABLE`。
- 未发布对象返回明确缺口和下一步。
- 查询 SQL 写在 Mapper XML，不在 Service 用复杂 wrapper 拼查询。

### Task 3: 对象详情和 CRM 套件验收状态展示

**目标**: 前端把“能不能交付”展示给业务和实施人员。

**涉及文件**:
- `forge-admin-ui/src/api/business-app.js` — 新增 readiness 和 acceptance API。
- `forge-admin-ui/src/views/app-center/object.[objectCode].vue` — 显示对象就绪度摘要。
- `forge-admin-ui/src/views/app-center/suite.[suiteCode].vue` — 增加 CRM 交付验收区域。
- `forge-admin-ui/src/views/app-center/components/ReadinessPanel.vue` — 新增就绪度组件。
- `forge-admin-ui/src/views/app-center/components/SuiteAcceptancePanel.vue` — 新增套件验收组件。

**前端展示规则**:
- 业务用户看到“可运行/待发布/缺少数据表/缺少流程配置”等业务语言。
- 开发者模式下才显示 `configKey`、表名、模型编码。
- 每个缺口都要有下一步按钮。

**验收标准**:
- 打开客户对象，顶部展示可运行状态。
- 打开未发布对象，顶部展示缺口和下一步。
- CRM 套件详情能汇总核心对象是否达标。

---

## Phase 2：CRM 核心对象运行闭环

### Task 4: CRM 核心运行表 Flyway 脚本

**目标**: 为 CRM 可交付闭环补齐真实运行表，避免只有模型和入口。

**涉及文件**:
- `forge/db/migration/V1.0.36__add_crm_runtime_tables.sql` — 新增 CRM 运行表。

**新增表**:
- `crm_customer`
- `crm_contact`
- `crm_opportunity`
- `crm_contract`
- `crm_payment`
- `crm_contract_item`
- `crm_follow_record`

**字段要求**:
- 每张表必须包含 `id`, `tenant_id`, `create_by`, `create_time`, `create_dept`, `update_by`, `update_time`, `del_flag`。
- 金额字段使用 `amount_cent`、`unit_price_cent`，类型 `bigint`。
- 关联字段使用 `customer_id`, `opportunity_id`, `contract_id`。
- 添加必要索引：`tenant_id + 关联字段`、`tenant_id + status`、`tenant_id + create_time`。

**验收标准**:
- Flyway 可重复执行。
- 不使用 decimal 金额字段。
- 内置数据 `tenant_id=1`。

### Task 5: CRM 核心对象模型与运行配置初始化

**目标**: 补齐客户、联系人、商机、合同、回款的低代码模型和 `ai_crud_config` 运行配置。

**涉及文件**:
- `forge/db/migration/V1.0.37__seed_crm_runtime_crud_configs.sql` — 新增/修正 CRM 运行配置。
- `forge/db/migration/V1.0.38__normalize_crm_business_object_runtime.sql` — 同步 `ai_business_object.model_id/model_code` 与 `ai_business_app.config_key`。

**配置要求**:
- `crm_customer`、`crm_contact`、`crm_opportunity`、`crm_contract`、`crm_payment` 至少为 `PUBLISHED`。
- `api_config` 使用冒号占位符：`:id`。
- `options` 启用导入、导出、详情。
- `model_schema` 金额字段使用分单位。
- 字典字段使用已有或新增 CRM 字典，不在前端硬编码枚举。

**验收标准**:
- 5 个核心对象都能通过就绪度接口返回 `RUNNABLE` 或明确缺口。
- 运行入口打开不因缺少 `ai_crud_config` 报错。

### Task 6: 应用入口打开前校验运行配置存在

**目标**: 防止业务用户点击入口后才发现 `configKey` 不存在。

**涉及文件**:
- `BusinessAppOpenService.java` — 增强 `RUNTIME` 模式校验。
- `BusinessObjectService.java` — 增强 `runtimeInfo` 校验。
- `BusinessAppOpenInfoVO.java` — 增加 `runtimeStatus`、`runtimeMessage`。
- `BusinessAppMapper.xml` 或 `AiCrudConfigMapper.xml` — 查询 `ai_crud_config`。

**关键逻辑**:
- `entryMode=RUNTIME` 且 `configKey` 为空：不可打开，提示配置运行配置。
- `configKey` 非空但 `ai_crud_config` 不存在：不可打开，提示发布应用。
- `ai_crud_config.publish_status` 非 `PUBLISHED`：不可打开，提示发布应用。

**验收标准**:
- 不存在的 `configKey` 不会被前端当作可打开。
- 提示文案包含下一步：配置模型、配置布局、发布应用。

### Task 7: CRM 导入导出和运行页验收

**目标**: 确认 CRM 核心对象不仅能打开，还能完成业务数据操作。

**涉及文件**:
- `forge-admin-ui/src/views/app-center/object.[objectCode].vue` — 导入导出按钮根据 readiness 控制。
- `forge-admin-ui/src/api/business-app.js` — 复用动态 CRUD 导入导出 API。
- `code-copilot/changes/lowcode-business-app-platform-business-closure/test-spec.md` — 如进入 `/test` 阶段则新增验收清单。

**验收动作**:
- 下载客户导入模板。
- 导入一条客户数据。
- 导出客户数据。
- 打开联系人、商机、合同、回款列表页。

**验收标准**:
- 导入导出仍走 `/ai/crud/{configKey}/import-template`、`/import`、`/export`。
- 不新增第二套导入导出服务。

---

## Phase 3：对象关系运行视图

### Task 8: 对象关系运行入口后端协议

**目标**: 把对象关系从“配置展示”推进到“可进入关联数据”。

**涉及文件**:
- `BusinessObjectRelationController.java` — 新增关系运行接口。
- `BusinessRelationRuntimeService.java` — 新增关系运行解析服务。
- `BusinessObjectRelationMapper.xml` — 查询关系和目标对象入口。
- `BusinessRelationRuntimeVO.java` — 新增关系运行 VO。

**关键签名**:
```java
@GetMapping("/{objectId}/relation-runtime")
@SaCheckPermission("ai:businessRelation:runtime")
public RespInfo<List<BusinessRelationRuntimeVO>> relationRuntime(@PathVariable Long objectId) { }
```

**返回内容**:
- 关系名称。
- 目标对象。
- 目标运行入口。
- 关联字段。
- 是否可打开。
- 默认筛选参数。
- 不可打开原因。

**验收标准**:
- 客户对象能返回联系人、商机、跟进记录关系入口。
- 合同对象能返回合同明细、回款关系入口。

### Task 9: 对象详情关联入口前端展示

**目标**: 业务用户在对象详情里能进入关联数据。

**涉及文件**:
- `forge-admin-ui/src/views/app-center/components/ObjectRelationPanel.vue` — 升级为配置 + 运行入口展示。
- `forge-admin-ui/src/views/app-center/object.[objectCode].vue` — 关系页签接入运行数据。
- `forge-admin-ui/src/api/business-app.js` — 新增 relationRuntime API。

**展示要求**:
- 显示“客户联系人”“客户商机”“合同回款”等业务文案。
- 可打开时跳转目标运行页并携带筛选参数。
- 不可打开时展示缺少运行配置或未发布。

**验收标准**:
- 从客户详情能进入联系人列表。
- 从合同详情能进入回款列表。

---

## Phase 4：自助搭建闭环

### Task 10: 自助搭建向导状态模型

**目标**: 让“从空白/模板/数据库/AI 创建”不再只是保存对象档案。

**涉及文件**:
- `BusinessObjectWizardDrawer.vue` — 增加创建方式状态和下一步。
- `BusinessObjectService.java` — 保存创建方式和下一步建议。
- `BusinessObjectReadinessService.java` — 基于创建方式返回下一步。

**状态要求**:
- `BLANK`：下一步配置模型。
- `TEMPLATE`：下一步选择模板并生成草稿。
- `DB_IMPORT`：下一步进入数据库表导入。
- `AI_GENERATE`：下一步进入 AI 生成。

**验收标准**:
- 保存对象后不会停在“对象档案已创建”。
- 前端能直接进入下一步。

### Task 11: 模板/数据库导入/AI 生成路径串联

**目标**: 将自助搭建入口串到现有低代码能力。

**涉及文件**:
- `BusinessObjectWizardDrawer.vue` — 根据创建方式跳转。
- `forge-admin-ui/src/views/ai/lowcode-models.vue` — 接收 `suiteCode/objectCode/returnTo`。
- `forge-admin-ui/src/views/ai/lowcode-builder.vue` — 接收业务对象上下文。
- `LowcodeDataModelDTO.java` — 已有业务对象上下文字段时复用，不足则补齐。
- `LowcodeDataModelService` 相关实现 — 模型保存后回写业务对象。

**跳转规则**:
- `DB_IMPORT` 跳到模型管理并打开数据库表导入。
- `AI_GENERATE` 跳到低代码 AI 生成入口并带入套件/对象上下文。
- `TEMPLATE` 选择模板后生成模型草稿和应用草稿。

**验收标准**:
- 从应用中心新建对象后可以回到对象详情查看模型关联状态。

### Task 12: 发布应用后自动生成业务入口

**目标**: 低代码发布成功后，自动回写应用中心入口。

**涉及文件**:
- `LowcodePublishService` 或现有发布服务实现 — 发布后同步 `ai_business_app`。
- `BusinessBootstrapService.java` — 抽取可复用同步方法。
- `BusinessAppMapper.xml` — 按 `configKey` 幂等查找/插入入口。
- `PublishPanel.vue` — 发布成功后提示“已生成业务入口”并提供返回应用中心。

**验收标准**:
- 从对象进入低代码搭建器发布后，对象详情 readiness 变为可运行。
- 不产生重复 `ai_business_app`。

---

## Phase 5：引擎能力最小闭环

### Task 13: 引擎中心运行状态汇总接口

**目标**: 引擎中心不再只显示静态入口，而是显示接入和可运行状态。

**涉及文件**:
- `BusinessEngineController.java` — 新增引擎汇总接口。
- `BusinessEngineSummaryService.java` — 新增汇总服务。
- `BusinessBindingMapper.xml` — 查询各能力状态。
- `forge-admin-ui/src/views/app-center/engines.vue` — 改为接口驱动。

**关键签名**:
```java
@GetMapping("/ai/business/engine/summary")
@SaCheckPermission("ai:businessEngine:runtime")
public RespInfo<List<BusinessEngineSummaryVO>> summary() { }
```

**验收标准**:
- 引擎中心展示总接入数、可运行数、待配置数、异常数。

### Task 14: 合同审批最小闭环

**目标**: 合同审批能力从标签推进到可判断、可配置、可发起。

**涉及文件**:
- `BusinessApprovalRuntimeService.java` — 新增审批运行服务。
- `BusinessApprovalController.java` — 新增发起审批接口。
- `BusinessBindingService.java` — 审批挂接状态识别。
- `BusinessBindingPanel.vue` — 审批能力卡片展示状态和发起入口。

**关键签名**:
```java
public BusinessApprovalRuntimeVO getApprovalRuntime(String targetCode, Long recordId) { }

public Long startApproval(BusinessApprovalStartDTO dto) { }
```

**验收标准**:
- 合同审批流程未配置时，提示“去流程配置”。
- 流程存在时，可以从合同记录发起审批或进入发起页。

### Task 15: 报表和大屏能力入口闭环

**目标**: CRM 销售看板或经营分析大屏能作为业务入口安全打开。

**涉及文件**:
- `forge/db/migration/V1.0.39__seed_crm_dashboard_embedded_app.sql` — 初始化销售看板嵌入入口。
- `BusinessBindingService.java` — 报表能力识别 `entryUrl/openType`。
- `BusinessBindingPanel.vue` — 报表能力打开。
- `suite.[suiteCode].vue` — CRM 套件场景入口展示销售看板。

**验收标准**:
- CRM 套件下有销售看板入口。
- 未配置白名单或地址时，显示业务化错误。

### Task 16: 消息与触发器最小动作闭环

**目标**: 商机阶段提醒、回款逾期提醒、合同金额汇总至少能执行或记录。

**涉及文件**:
- `BusinessTriggerRuntimeService.java` — 新增触发器执行服务。
- `BusinessTriggerController.java` — 新增触发器执行接口。
- `BusinessActionLogService.java` — 新增动作日志服务。
- `forge/db/migration/V1.0.40__add_business_action_log.sql` — 新增动作日志表。
- `BusinessBindingPanel.vue` — 触发器和消息卡片展示执行结果。

**最小动作**:
- 合同金额汇总：根据合同明细金额更新合同金额。
- 商机阶段提醒：写入动作日志，能对接消息中心时发送站内消息。
- 回款逾期提醒：写入动作日志，能对接消息中心时生成提醒。

**验收标准**:
- 触发器执行有成功/失败日志。
- 不引入完整复杂规则引擎。

### Task 17: 对象权限能力状态摘要

**目标**: 权限能力不只是标签，至少能说明当前对象使用什么权限策略。

**涉及文件**:
- `BusinessObjectReadinessService.java` — 增加权限策略检查。
- `BusinessBindingService.java` — 权限挂接运行状态。
- `BusinessBindingPanel.vue` — 权限摘要和配置入口。

**验收标准**:
- 客户对象显示按负责人/部门/区域等权限策略摘要。
- 可以跳转现有角色/数据权限配置入口。

---

## Phase 6：渠道闭环

### Task 18: 嵌入应用统一 iframe 容器

**目标**: 大屏和外部页面不裸跳，统一经过平台容器和安全提示。

**涉及文件**:
- `forge-admin-ui/src/views/app-center/embed-frame.vue` — 新增 iframe 容器。
- `router` 动态菜单映射或菜单脚本 — 增加 `/app-center/embed/:appCode`。
- `BusinessEmbedController.java` — 新增 frame-info 接口。
- `BusinessEmbedService.java` — 解析 iframe 展示信息。
- `BusinessAppOpenService.java` — IFRAME 模式返回容器路由。

**验收标准**:
- iframe 应用通过容器打开。
- 未配置白名单、未授权、地址异常时展示业务错误页。

### Task 19: 移动入口可见范围与打开状态

**目标**: 移动端中心能说明入口通向哪里、谁可见、是否可打开。

**涉及文件**:
- `BusinessMobileController.java` — 新增移动汇总接口。
- `BusinessAppService.java` — 移动入口 options 校验。
- `mobile.vue` — 展示可见范围、入口状态和 CRM 分组。
- `AppEditorDrawer.vue` — 补齐移动可见范围字段。

**验收标准**:
- H5、移动待办、移动审批、移动业务入口有分类统计。
- 外部 H5 必须配置白名单。
- URL 不允许长期 Token。

### Task 20: 集成事件订阅、Webhook 推送日志和重试

**目标**: 集成中心至少打通通用 Webhook 推送闭环。

**涉及文件**:
- `forge/db/migration/V1.0.41__add_business_integration_event_tables.sql` — 新增订阅和日志表。
- `BusinessIntegrationController.java` — 新增事件类型、订阅、日志、重试接口。
- `BusinessIntegrationEventService.java` — 事件投递和重试服务。
- `BusinessEventSubscriptionMapper.java` / XML — 订阅查询。
- `BusinessEventLogMapper.java` / XML — 日志查询。
- `integration.vue` — 增加事件订阅和推送日志页签。

**关键接口**:
```java
@GetMapping("/ai/business/integration/event-types")
public RespInfo<List<BusinessEventTypeVO>> eventTypes() { }

@PostMapping("/ai/business/integration/event-log/{id}/retry")
public RespInfo<Void> retry(@PathVariable Long id) { }
```

**验收标准**:
- 可配置 CRM 客户创建或合同审批事件的 Webhook 订阅。
- 触发事件后生成日志。
- 失败日志可手动重推。
- 企微/飞书/钉钉作为通道类型存在，但不保存明文密钥。

---

## Phase 7：体验与权限

### Task 21: 业务化文案和开发者信息隔离

**目标**: 普通业务用户不需要理解技术配置。

**涉及文件**:
- `object.[objectCode].vue`
- `suite.[suiteCode].vue`
- `AppCard.vue`
- `ObjectCard.vue`
- `BusinessBindingPanel.vue`
- `ReadinessPanel.vue`

**规则**:
- 普通模式显示业务语言。
- 开发者信息折叠在“开发者信息”页签。
- `configKey`、表名、模型编码、JSON 只在开发者信息中展示。

**验收标准**:
- CRM 业务用户路径不出现 JSON/Schema 主文案。

### Task 22: 菜单、权限和普通用户入口收敛

**目标**: 补齐新增闭环能力权限，并保证普通用户不进入开发者工具。

**涉及文件**:
- `forge/db/migration/V1.0.42__add_business_closure_permissions.sql`
- 相关 Controller `@SaCheckPermission`
- 菜单资源脚本

**权限**:
- `ai:businessReadiness:view`
- `ai:businessAcceptance:view`
- `ai:businessRelation:runtime`
- `ai:businessEngine:runtime`
- `ai:businessApproval:start`
- `ai:businessTrigger:execute`
- `ai:businessEmbed:open`
- `ai:businessIntegration:config`
- `ai:businessIntegration:log`
- `ai:businessIntegration:retry`

**验收标准**:
- 权限脚本防重复。
- 普通业务角色可进应用中心和 CRM，不能看到纯开发者配置入口。

---

## Phase 8：验证归档

### Task 23: 构建、接口和页面联调验证

**目标**: 确认业务闭环在真实登录态下可用。

**验证命令**:
```bash
mvn -pl forge-admin-server -am compile -DskipTests
```

```bash
source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm --dir forge-admin-ui build
```

**接口验证**:
- 登录获取 token。
- 调用 `/ai/business/suite/CRM/acceptance`。
- 调用客户、联系人、商机、合同、回款 readiness。
- 调用 CRM 应用入口 open-info。
- 调用嵌入应用 frame-info。
- 调用集成事件日志和重试接口。

**页面验证**:
- 应用中心。
- CRM 套件详情。
- 客户对象详情。
- 合同对象详情。
- 引擎中心。
- 移动端中心。
- 集成中心。
- 嵌入应用容器。

**验收标准**:
- 后端编译通过。
- 前端构建通过。
- 关键页面无明显控制台错误。
- CRM 最小闭环可演示。

### Task 24: Spec、任务和验收记录回填

**目标**: 完成变更文档闭环。

**涉及文件**:
- `code-copilot/changes/lowcode-business-app-platform-business-closure/spec.md`
- `code-copilot/changes/lowcode-business-app-platform-business-closure/tasks.md`
- 可选：`code-copilot/changes/lowcode-business-app-platform-business-closure/test-spec.md`

**回填内容**:
- 实际改动文件。
- 验证命令和结果。
- 未完成项和后续阶段。
- 风险和人工确认记录。

**验收标准**:
- `tasks.md` 状态与实际实现一致。
- `spec.md` 执行日志完整。
- 可以进入 `/review lowcode-business-app-platform-business-closure`。
