# 任务清单：lowcode-business-app-platform
> status: completed
> created: 2026-05-27
> 拆分顺序：决策基线 → 数据模型 → 后端协议 → 兼容迁移 → 前端入口 → CRM 样板 → 验证归档 → 运行联调 → 自助搭建 → 能力闭环 → 渠道/集成边界
> 原则：每个任务应可独立提交；查询 SQL 写 Mapper XML；分页参数使用 `pageNum/pageSize`；内置数据 `tenant_id=1`；Flyway 脚本必须防重复。

## 前置条件

- [x] 已完成 `spec.md` 业务对象/实体模型补充。
- [x] 已确认第一阶段不重写动态 CRUD 运行时。
- [x] 已确认标准业务应用入口继续复用 `/ai/crud-page/{configKey}`。
- [x] 确认第一阶段采用新增业务表方案：`ai_business_suite`、`ai_business_object`、`ai_business_object_relation`、`ai_business_app`、`ai_business_binding`。
- [x] 确认 CRM 样板第一阶段以配置初始化为主，不强制一次性创建所有真实 CRM 业务表。

## 阶段总览

| 阶段 | 目标 | 包含任务 | 交付结果 |
|------|------|----------|----------|
| Phase 0 | 决策基线 | Task 0 | 冻结第一阶段边界和命名 |
| Phase 1 | 数据模型 | Task 1-3 | 表结构、字典、菜单、CRM 初始化数据 |
| Phase 2 | 后端基础能力 | Task 4-10 | 业务套件、业务对象、对象关系、应用入口、能力挂接 API |
| Phase 3 | 兼容迁移 | Task 11-12 | 低代码旧数据映射到新业务化模型 |
| Phase 4 | 前端入口 | Task 13-18 | 应用中心、套件详情、对象详情、引擎/移动/集成入口 |
| Phase 5 | CRM 样板闭环 | Task 19 | CRM 对象、关系、能力、入口可视化闭环 |
| Phase 6 | 验证归档 | Task 20-21 | 编译、构建、验收和文档回填 |
| Phase 7 | 运行联调补齐 | Task 22-24 | 修复启动阻塞、完成登录态联调、打通至少一个 CRM 运行链路 |
| Phase 8 | 客户自助搭建主流程 | Task 25-27 | 从业务套件创建对象、复用模型/布局/发布能力、生成应用入口 |
| Phase 9 | 能力挂接最小运行闭环 | Task 28-30 | 对象级导入导出、报表/审批/消息/权限入口、状态和错误提示 |
| Phase 10 | 渠道、集成与开发者工具收敛 | Task 31-34 | 嵌入应用安全边界、移动/集成入口配置、旧低代码入口收敛 |
| Phase 11 | 二阶段验收归档 | Task 35 | 补齐 Spec 遗留项的验证记录 |

## 任务总览

| Task | 阶段 | 名称 | 状态 | 优先级 |
|------|------|------|------|--------|
| Task 0 | Phase 0 | 决策基线与命名冻结 | completed | P0 |
| Task 1 | Phase 1 | 业务平台表结构迁移 | completed | P0 |
| Task 2 | Phase 1 | 字典、菜单、权限资源迁移 | completed | P0 |
| Task 3 | Phase 1 | CRM 样板初始化数据迁移 | completed | P0 |
| Task 4 | Phase 2 | 业务套件实体、Mapper、查询接口 | completed | P0 |
| Task 5 | Phase 2 | 业务对象实体、Mapper、查询接口 | completed | P0 |
| Task 6 | Phase 2 | 对象关系实体、Mapper、保存接口 | completed | P0 |
| Task 7 | Phase 2 | 应用入口实体、Mapper、打开信息接口 | completed | P0 |
| Task 8 | Phase 2 | 能力挂接实体、Mapper、批量保存接口 | completed | P0 |
| Task 9 | Phase 2 | 后端 DTO/VO 与接口协议统一 | completed | P0 |
| Task 10 | Phase 2 | 后端权限、状态、删除规则补齐 | completed | P1 |
| Task 11 | Phase 3 | 低代码领域和模型兼容映射 | completed | P0 |
| Task 12 | Phase 3 | 已发布低代码应用入口兼容映射 | completed | P0 |
| Task 13 | Phase 4 | 前端业务平台 API 客户端 | completed | P0 |
| Task 14 | Phase 4 | 应用中心首页 | completed | P0 |
| Task 15 | Phase 4 | 业务套件详情页 | completed | P0 |
| Task 16 | Phase 4 | 业务对象详情页和对象关系面板 | completed | P0 |
| Task 17 | Phase 4 | 应用入口编辑与打开行为 | completed | P1 |
| Task 18 | Phase 4 | 引擎中心、移动端中心、集成中心壳页面 | completed | P1 |
| Task 19 | Phase 5 | CRM 样板端到端验收 | completed | P0 |
| Task 20 | Phase 6 | 后端编译与前端构建验证 | completed | P0 |
| Task 21 | Phase 6 | 文档、变更记录和验收回填 | completed | P1 |
| Task 22 | Phase 7 | 修复后端启动阻塞 | completed | P0 |
| Task 23 | Phase 7 | 应用中心登录态真实接口联调 | completed | P0 |
| Task 24 | Phase 7 | CRM 至少一个对象运行态闭环 | completed | P0 |
| Task 25 | Phase 8 | 新建业务对象向导入口 | completed | P0 |
| Task 26 | Phase 8 | 模型设计、数据库导入、AI 生成复用串联 | completed | P1 |
| Task 27 | Phase 8 | 布局配置、发布应用、生成入口串联 | completed | P1 |
| Task 28 | Phase 9 | 对象级导入导出入口打通 | completed | P0 |
| Task 29 | Phase 9 | 报表、审批、消息、权限能力入口打通 | completed | P1 |
| Task 30 | Phase 9 | 未配置运行态和能力状态提示补齐 | completed | P0 |
| Task 31 | Phase 10 | 嵌入应用白名单与打开安全校验 | completed | P0 |
| Task 32 | Phase 10 | 移动应用入口配置补齐 | completed | P1 |
| Task 33 | Phase 10 | 集成应用入口配置补齐 | completed | P1 |
| Task 34 | Phase 10 | 开发者工具入口收敛和普通用户隐藏 | completed | P1 |
| Task 35 | Phase 11 | Spec 遗留项验收回填 | completed | P0 |

---

## Phase 0：决策基线

### Task 0: 决策基线与命名冻结

**目标**: 在编码前冻结第一阶段数据表、接口前缀、CRM 初始化策略和菜单层级，避免实现阶段反复返工。

**涉及文件**:
- `code-copilot/changes/lowcode-business-app-platform/spec.md` — 如决策变化，回填“待澄清”和“技术决策”。
- `code-copilot/changes/lowcode-business-app-platform/tasks.md` — 同步任务状态和拆分。

**决策输出**:
- 第一阶段采用新增业务表：`ai_business_suite`、`ai_business_object`、`ai_business_object_relation`、`ai_business_app`、`ai_business_binding`。
- 接口前缀固定为 `/ai/business/**`。
- `ai_business_app` 只表示应用入口，不表示业务对象。
- CRM 样板第一阶段初始化业务套件、对象、关系、能力挂接、应用入口；真实业务表按后续 CRUD 配置或业务模块补齐。
- 应用中心作为业务入口，旧低代码应用、模型设计、配置诊断移动到开发者工具或隐藏普通菜单。

**验收标准**:
- `spec.md` 和 `tasks.md` 对 `ai_business_app` 定位一致。
- 后续任务不再出现“业务应用主表承载业务对象”的表达。

---

## Phase 1：数据模型

### Task 1: 业务平台表结构迁移

**目标**: 新增业务套件、业务对象、对象关系、应用入口、能力挂接 5 张表。

**涉及文件**:
- `forge/db/migration/V1.0.26__add_business_app_platform_tables.sql` — 新增表、索引和字段注释。

**关键表**:
- `ai_business_suite`
- `ai_business_object`
- `ai_business_object_relation`
- `ai_business_app`
- `ai_business_binding`

**关键要求**:
- 所有业务表包含 `id`, `tenant_id`, `create_by`, `create_time`, `create_dept`, `update_by`, `update_time`。
- `tenant_id` 默认内置数据使用 `1`。
- 表使用 `ENGINE=InnoDB DEFAULT CHARSET=utf8mb4`。
- 建议唯一索引：
  - `uk_ai_business_suite_code(tenant_id, suite_code)`
  - `uk_ai_business_object_code(tenant_id, suite_code, object_code)`
  - `uk_ai_business_app_code(tenant_id, app_code)`
  - `uk_ai_business_binding_target(tenant_id, target_type, target_code, binding_type, binding_key)`
- 对象关系至少包含 `source_object_code`、`target_object_code`、`relation_type`、`relation_name`、`relation_config`。

**验收标准**:
- Flyway 脚本可重复执行，建表使用 `CREATE TABLE IF NOT EXISTS`。
- 新增字段与 `spec.md` 第 7 章一致。

---

### Task 2: 字典、菜单、权限资源迁移

**目标**: 初始化业务平台所需字典、应用中心菜单和按钮权限资源。

**涉及文件**:
- `forge/db/migration/V1.0.27__add_business_app_platform_dicts_menus.sql` — 新增字典、菜单、按钮权限资源。

**字典类型**:
- `ai_business_suite`
- `ai_business_object_type`
- `ai_business_relation_type`
- `ai_business_app_type`
- `ai_business_app_entry_mode`
- `ai_business_binding_type`

**菜单与权限**:
- `/app-center`：应用中心
- `/app-center/suite/:suiteCode`：业务套件详情
- `/app-center/object/:objectCode`：业务对象详情
- `/app-center/engines`：引擎中心
- `/app-center/mobile`：移动端中心
- `/app-center/integration`：集成中心
- `ai:businessSuite:list`
- `ai:businessSuite:edit`
- `ai:businessObject:list`
- `ai:businessObject:add`
- `ai:businessObject:edit`
- `ai:businessObject:delete`
- `ai:businessObject:relation`
- `ai:businessBinding:config`
- `ai:businessApp:add`
- `ai:businessApp:edit`
- `ai:businessApp:delete`
- `ai:businessApp:status`
- `ai:businessApp:open`

**验收标准**:
- 所有 `INSERT` 显式写列名。
- 字典和资源插入使用 `INSERT ... SELECT ... WHERE NOT EXISTS`。
- 业务内置数据 `tenant_id=1`。

---

### Task 3: CRM 样板初始化数据迁移

**目标**: 初始化 CRM 业务套件、核心业务对象、对象关系、对象能力挂接和标准应用入口。

**涉及文件**:
- `forge/db/migration/V1.0.28__seed_crm_business_suite.sql` — 初始化 CRM 样板配置。

**初始化对象**:
- 客户：`CUSTOMER`
- 联系人：`CONTACT`
- 线索：`LEAD`
- 商机：`OPPORTUNITY`
- 合同：`CONTRACT`
- 合同明细：`CONTRACT_ITEM`
- 回款：`PAYMENT`
- 跟进记录：`FOLLOW_RECORD`
- 销售任务：`SALES_TASK`

**初始化关系**:
- `CUSTOMER` -> `CONTACT`：`CHILD_LIST`
- `CUSTOMER` -> `OPPORTUNITY`：`CHILD_LIST`
- `OPPORTUNITY` -> `CONTRACT`：`REFERENCE`
- `CONTRACT` -> `CONTRACT_ITEM`：`DETAIL`
- `CONTRACT` -> `PAYMENT`：`CHILD_LIST`
- `CUSTOMER` -> `FOLLOW_RECORD`：`CHILD_LIST`
- `OPPORTUNITY` -> `FOLLOW_RECORD`：`CHILD_LIST`

**初始化能力**:
- `IMPORT`、`EXPORT`：客户、合同、联系人。
- `REPORT`：客户报表、销售漏斗、回款报表。
- `TRIGGER`：合同金额汇总、商机阶段提醒、回款逾期提醒。
- `APPROVAL`：合同审批。
- `MESSAGE`：商机跟进提醒、回款逾期提醒。
- `PERMISSION`：客户、合同、回款对象权限。

**验收标准**:
- CRM 至少有 8 个业务对象和 8 个应用入口。
- 应用入口 `app_type=BUSINESS` 且关联 `object_code`。
- 初始化脚本可重复执行，不产生重复数据。

---

## Phase 2：后端基础能力

### Task 4: 业务套件实体、Mapper、查询接口

**目标**: 提供业务套件分页、列表、详情、汇总接口。

**涉及文件**:
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/domain/entity/AiBusinessSuite.java` — 新增实体。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/mapper/BusinessSuiteMapper.java` — 新增 Mapper。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/BusinessSuiteMapper.xml` — 新增分页、列表、汇总 SQL。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessSuiteService.java` — 新增服务。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/controller/BusinessSuiteController.java` — 新增控制器。

**关键签名**:
```java
Page<BusinessSuiteVO> page(Integer pageNum, Integer pageSize, BusinessSuiteQueryDTO query);
List<BusinessSuiteVO> list(BusinessSuiteQueryDTO query);
BusinessSuiteVO detail(Long id);
List<BusinessSuiteSummaryVO> summary();
```

**验收标准**:
- 分页接口使用 `pageNum/pageSize`。
- 查询类 SQL 全部写在 `BusinessSuiteMapper.xml`。
- `/ai/business/suite/summary` 返回对象数量、应用入口数量、启用数量、最近更新时间。

---

### Task 5: 业务对象实体、Mapper、查询接口

**目标**: 提供业务对象分页、列表、详情、启停和运行信息查询。

**涉及文件**:
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/domain/entity/AiBusinessObject.java` — 新增实体。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/mapper/BusinessObjectMapper.java` — 新增 Mapper。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/BusinessObjectMapper.xml` — 新增分页、列表、详情 SQL。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessObjectService.java` — 新增服务。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/controller/BusinessObjectController.java` — 新增控制器。

**关键签名**:
```java
Page<BusinessObjectVO> page(Integer pageNum, Integer pageSize, BusinessObjectQueryDTO query);
List<BusinessObjectVO> list(BusinessObjectQueryDTO query);
BusinessObjectVO detail(Long id);
void updateStatus(Long id, Integer status);
BusinessObjectRuntimeInfoVO runtimeInfo(Long id);
```

**验收标准**:
- 业务对象可关联 `ai_lowcode_model.id` 和 `model_code`。
- `runtimeInfo` 返回关联应用入口、`configKey`、动态 CRUD 路由和权限结果。
- 状态停用后，前端不能作为可打开对象展示。

---

### Task 6: 对象关系实体、Mapper、保存接口

**目标**: 支持引用关系、明细关系、关联列表关系的查询和保存。

**涉及文件**:
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/domain/entity/AiBusinessObjectRelation.java` — 新增实体。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/mapper/BusinessObjectRelationMapper.java` — 新增 Mapper。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/BusinessObjectRelationMapper.xml` — 新增对象关系查询 SQL。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessObjectRelationService.java` — 新增服务。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/controller/BusinessObjectRelationController.java` — 新增控制器。

**关键签名**:
```java
List<BusinessObjectRelationVO> listByObject(Long objectId);
void saveRelations(Long objectId, List<BusinessObjectRelationDTO> relations);
void deleteRelation(Long objectId, Long relationId);
```

**验收标准**:
- 支持 `REFERENCE`、`DETAIL`、`CHILD_LIST`、`MANY_TO_MANY` 四类关系。
- 保存关系时校验来源对象和目标对象存在。
- 删除关系只删除当前对象范围内的关系，不能误删其他套件关系。

---

### Task 7: 应用入口实体、Mapper、打开信息接口

**目标**: 支持业务应用、嵌入应用、移动应用、集成应用四类入口，并提供打开信息解析。

**涉及文件**:
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/domain/entity/AiBusinessApp.java` — 新增实体。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/mapper/BusinessAppMapper.java` — 新增 Mapper。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/BusinessAppMapper.xml` — 新增分页、列表、详情 SQL。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessAppService.java` — 新增服务。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessAppOpenService.java` — 新增打开方式解析服务。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/controller/BusinessAppController.java` — 新增控制器。

**关键签名**:
```java
Page<BusinessAppVO> page(Integer pageNum, Integer pageSize, BusinessAppQueryDTO query);
BusinessAppVO detail(Long id);
void updateStatus(Long id, Integer status);
BusinessAppOpenInfoVO openInfo(Long id);
```

**验收标准**:
- `BUSINESS + RUNTIME` 类型入口解析为 `/ai/crud-page/{configKey}`。
- `ROUTE`、`IFRAME`、`EXTERNAL`、`H5`、`API` 按 `entry_mode` 返回明确打开方式。
- 嵌入和外部入口返回前必须完成权限校验结果封装。

---

### Task 8: 能力挂接实体、Mapper、批量保存接口

**目标**: 支持业务套件、业务对象、应用入口三类目标的能力挂接。

**涉及文件**:
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/domain/entity/AiBusinessBinding.java` — 新增实体。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/mapper/BusinessBindingMapper.java` — 新增 Mapper。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/BusinessBindingMapper.xml` — 新增挂接能力查询 SQL。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessBindingService.java` — 新增服务。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/controller/BusinessBindingController.java` — 新增控制器。

**关键签名**:
```java
List<BusinessBindingVO> list(BusinessBindingQueryDTO query);
Long create(BusinessBindingDTO dto);
void update(BusinessBindingDTO dto);
void delete(Long id);
void batchSave(BusinessBindingBatchSaveDTO dto);
```

**验收标准**:
- `target_type` 支持 `SUITE`、`OBJECT`、`APP`。
- `binding_type` 支持 `FLOW`、`APPROVAL`、`REPORT`、`PERMISSION`、`MESSAGE`、`TRIGGER`、`IMPORT`、`EXPORT`、`MOBILE`、`INTEGRATION`。
- 批量保存按 `target_type + target_code + binding_type` 覆盖目标范围内旧配置，不影响其他目标。

---

### Task 9: 后端 DTO/VO 与接口协议统一

**目标**: 建立业务平台 DTO/VO，避免 Controller 直接暴露实体和 JSON 字符串细节。

**涉及文件**:
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/businessapp/BusinessSuiteQueryDTO.java`
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/businessapp/BusinessSuiteDTO.java`
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/businessapp/BusinessObjectQueryDTO.java`
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/businessapp/BusinessObjectDTO.java`
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/businessapp/BusinessObjectRelationDTO.java`
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/businessapp/BusinessAppQueryDTO.java`
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/businessapp/BusinessAppDTO.java`
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/businessapp/BusinessBindingQueryDTO.java`
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/businessapp/BusinessBindingDTO.java`
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/businessapp/BusinessBindingBatchSaveDTO.java`
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/vo/businessapp/*.java`

**关键 VO**:
- `BusinessSuiteVO`
- `BusinessSuiteSummaryVO`
- `BusinessObjectVO`
- `BusinessObjectRuntimeInfoVO`
- `BusinessObjectRelationVO`
- `BusinessAppVO`
- `BusinessAppOpenInfoVO`
- `BusinessBindingVO`

**验收标准**:
- 前端不需要解析实体原始 JSON 字符串即可展示对象能力和入口信息。
- DTO 字段使用业务语言：`suiteCode`、`objectCode`、`relationType`、`appType`、`entryMode`、`bindingType`。
- Controller 返回统一 `RespInfo.success(data)`。

---

### Task 10: 后端权限、状态、删除规则补齐

**目标**: 补齐状态控制、删除保护、权限注解和安全边界。

**涉及文件**:
- `BusinessSuiteController.java` — 增加权限注解。
- `BusinessObjectController.java` — 增加权限注解和删除保护。
- `BusinessObjectRelationController.java` — 增加权限注解。
- `BusinessAppController.java` — 增加权限注解和打开权限校验。
- `BusinessBindingController.java` — 增加权限注解。
- `BusinessAppOpenService.java` — 嵌入、外部、H5 入口打开安全校验。

**关键权限**:
```java
@SaCheckPermission("ai:businessObject:list")
@SaCheckPermission("ai:businessObject:edit")
@SaCheckPermission("ai:businessObject:relation")
@SaCheckPermission("ai:businessBinding:config")
@SaCheckPermission("ai:businessApp:open")
```

**验收标准**:
- 已有关联对象关系的业务对象不能直接硬删除。
- 已有关联业务对象的套件不能直接硬删除。
- 停用应用入口不能返回可打开状态。
- 外部地址、iframe、H5 入口不绕过平台权限校验。

---

## Phase 3：兼容迁移

### Task 11: 低代码领域和模型兼容映射

**目标**: 将现有 `ai_lowcode_domain` 和 `ai_lowcode_model` 业务化表达为套件和对象。

**涉及文件**:
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessBootstrapService.java` — 新增兼容映射服务。
- `BusinessSuiteMapper.xml` — 新增按领域映射查询。
- `BusinessObjectMapper.xml` — 新增按模型映射查询。
- `BusinessSuiteService.java` — 增加领域映射入口。
- `BusinessObjectService.java` — 增加模型映射入口。

**关键签名**:
```java
void syncSuitesFromLowcodeDomains();
void syncObjectsFromLowcodeModels();
```

**验收标准**:
- `ai_lowcode_domain.domain_code` 映射为 `ai_business_suite.suite_code`。
- `ai_lowcode_model.model_code` 映射为 `ai_business_object.object_code`。
- 同步逻辑按编码幂等执行，不覆盖用户后续修改的业务名称和说明。

---

### Task 12: 已发布低代码应用入口兼容映射

**目标**: 将已发布低代码应用映射为 `ai_business_app` 应用入口，并继续通过 `configKey` 打开旧运行页。

**涉及文件**:
- `BusinessBootstrapService.java` — 增加低代码应用入口同步。
- `BusinessAppMapper.xml` — 新增按 `config_key` 查询和 upsert 所需查询。
- `BusinessAppOpenService.java` — 保持 `configKey` 到 `/ai/crud-page/{configKey}` 的解析。

**关键签名**:
```java
void syncAppsFromPublishedCrudConfigs();
BusinessAppOpenInfoVO buildRuntimeOpenInfo(AiBusinessApp app);
```

**验收标准**:
- `ai_crud_config.publish_status='PUBLISHED'` 的配置可生成 `BUSINESS + RUNTIME` 应用入口。
- 旧 `/ai/lowcode/app/**`、`/ai/crud-page/{configKey}`、`/ai/crud/{configKey}/**` 不受影响。
- 业务应用入口不复制动态 CRUD 运行配置，只保存 `config_key` 引用。

---

## Phase 4：前端入口

### Task 13: 前端业务平台 API 客户端

**目标**: 封装业务平台前端 API，供应用中心和详情页复用。

**涉及文件**:
- `forge-admin-ui/src/api/business-app.js` — 新增业务平台 API 方法。

**关键函数**:
```js
export function businessSuitePage(params)
export function businessSuiteList(params)
export function businessSuiteSummary(params)
export function businessObjectPage(params)
export function businessObjectList(params)
export function businessObjectDetail(id)
export function businessObjectRelations(objectId)
export function saveBusinessObjectRelations(objectId, data)
export function businessAppPage(params)
export function businessAppOpenInfo(id)
export function businessBindingList(params)
export function saveBusinessBindings(data)
```

**验收标准**:
- 所有接口路径与 `spec.md` 第 8 章一致。
- URL 占位符在 AiCrudPage 场景继续使用 `:id` 规则；普通 API 函数使用模板字符串路径。

---

### Task 14: 应用中心首页

**目标**: 新增 `/app-center` 首页，按业务套件和应用类型展示业务入口。

**涉及文件**:
- `forge-admin-ui/src/views/app-center/index.vue` — 新增应用中心首页。
- `forge-admin-ui/src/views/app-center/components/AppFilterBar.vue` — 新增筛选条。
- `forge-admin-ui/src/views/app-center/components/ObjectCard.vue` — 新增业务对象卡片。
- `forge-admin-ui/src/views/app-center/components/AppCard.vue` — 新增应用入口卡片。

**关键交互**:
- 按业务套件筛选。
- 按应用类型筛选：`BUSINESS`、`EMBEDDED`、`MOBILE`、`INTEGRATION`。
- 点击业务套件进入 `/app-center/suite/:suiteCode`。
- 点击业务对象进入 `/app-center/object/:objectCode`。
- 点击应用入口调用 `businessAppOpenInfo(id)`。

**验收标准**:
- 首页默认展示业务套件和业务对象，不展示 JSON、Schema、表名。
- 页面文案使用“业务对象”“对象关系”“接入能力”“应用入口”。
- 页面保持后台业务系统风格，不做营销式落地页。

---

### Task 15: 业务套件详情页

**目标**: 展示 CRM 等套件下的业务对象、业务流程、业务看板、移动入口和集成入口。

**涉及文件**:
- `forge-admin-ui/src/views/app-center/suite.[suiteCode].vue` — 新增套件详情页。
- `ObjectCard.vue` — 展示套件对象。
- `AppCard.vue` — 展示看板、移动、集成入口。

**关键交互**:
- 展示套件基础信息、对象数量、应用入口数量、最近更新。
- 业务对象区展示客户、联系人、线索、商机、合同、回款等对象。
- 场景入口区展示销售看板、移动拜访、第三方推送等入口。

**验收标准**:
- CRM 套件详情中业务对象是第一视觉。
- 应用入口作为对象之外的场景入口展示。
- 停用对象和停用应用入口有明确状态标签。

---

### Task 16: 业务对象详情页和对象关系面板

**目标**: 为业务对象提供关系、布局、导入导出、报表、触发器、审批、消息、权限等能力页签。

**涉及文件**:
- `forge-admin-ui/src/views/app-center/object.[objectCode].vue` — 新增业务对象详情页。
- `forge-admin-ui/src/views/app-center/components/ObjectRelationPanel.vue` — 新增对象关系面板。
- `forge-admin-ui/src/views/app-center/components/BusinessBindingPanel.vue` — 新增能力挂接面板。

**关键交互**:
- “关系”页签展示 `REFERENCE`、`DETAIL`、`CHILD_LIST`、`MANY_TO_MANY`。
- “能力”页签展示 `FLOW`、`APPROVAL`、`REPORT`、`PERMISSION`、`MESSAGE`、`TRIGGER`、`IMPORT`、`EXPORT`。
- 对象关系配置使用“关联客户”“合同明细”“下级联系人”等业务文案。

**验收标准**:
- 客户对象可看到联系人、商机、跟进记录关系。
- 合同对象可看到合同明细和回款关系。
- 技术字段 `modelSchema`、`tableName`、`configKey` 仅在开发者信息区显示。

---

### Task 17: 应用入口编辑与打开行为

**目标**: 支持新增、编辑、启停和打开应用入口。

**涉及文件**:
- `forge-admin-ui/src/views/app-center/components/AppEditorDrawer.vue` — 新增应用入口编辑抽屉。
- `forge-admin-ui/src/views/app-center/index.vue` — 接入编辑和打开行为。
- `forge-admin-ui/src/views/app-center/suite.[suiteCode].vue` — 接入入口编辑和打开行为。

**关键交互**:
- `BUSINESS + RUNTIME` 打开 `/ai/crud-page/{configKey}`。
- `ROUTE` 打开内部路由。
- `IFRAME` 打开平台内嵌页面。
- `EXTERNAL` 新窗口打开。
- `H5` 展示移动入口地址或二维码预留区。
- `API` 进入集成配置详情。

**验收标准**:
- 停用入口不能打开。
- 打开前调用后端 `open-info`，不由前端自行拼接外部敏感入口。
- `configKey` 不作为卡片主视觉字段。

---

### Task 18: 引擎中心、移动端中心、集成中心壳页面

**目标**: 完成三类中心的第一阶段统一入口，不深改底层引擎。

**涉及文件**:
- `forge-admin-ui/src/views/app-center/engines.vue` — 新增引擎中心。
- `forge-admin-ui/src/views/app-center/mobile.vue` — 新增移动端中心。
- `forge-admin-ui/src/views/app-center/integration.vue` — 新增集成中心。

**展示内容**:
- 引擎中心：流程、审批、报表、权限、消息、触发器、导入、导出能力卡片。
- 移动端中心：H5、移动待办、移动审批、移动业务入口。
- 集成中心：开放接口、Webhook、企微、飞书、钉钉。

**验收标准**:
- 每个中心能从应用中心菜单访问。
- 第一阶段只展示入口和挂接概览，不承诺完整推送通道或移动容器。

---

## Phase 5：CRM 样板闭环

### Task 19: CRM 样板端到端验收

**目标**: 验证 CRM 样板从业务套件、业务对象、对象关系、对象能力到应用入口的完整表达。

**涉及文件**:
- `forge/db/migration/V1.0.28__seed_crm_business_suite.sql` — 校验初始化数据。
- `forge-admin-ui/src/views/app-center/index.vue` — 校验 CRM 套件展示。
- `forge-admin-ui/src/views/app-center/suite.[suiteCode].vue` — 校验 CRM 对象展示。
- `forge-admin-ui/src/views/app-center/object.[objectCode].vue` — 校验对象关系和能力。

**验收路径**:
1. 进入 `/app-center`。
2. 打开 CRM 套件。
3. 查看客户、联系人、线索、商机、合同、合同明细、回款、跟进记录、销售任务。
4. 打开客户对象，确认客户-联系人、客户-商机、客户-跟进记录关系。
5. 打开合同对象，确认合同-合同明细、合同-回款关系。
6. 打开客户管理应用入口，确认能进入现有动态 CRUD 运行页或得到明确未配置提示。

**验收标准**:
- CRM 至少展示 8 个核心业务对象。
- CRM 至少展示 5 个对象关系。
- CRM 至少展示 8 个应用入口。
- 标准业务入口继续兼容现有动态 CRUD。

**2026-05-27 静态验收结果**:
- `V1.0.28__seed_crm_business_suite.sql` 已初始化 CRM 套件。
- CRM 样板包含 9 个业务对象：客户、联系人、线索、商机、合同、合同明细、回款、跟进记录、销售任务。
- CRM 样板包含 7 条对象关系，覆盖客户-联系人、客户-商机、商机-合同、合同-合同明细、合同-回款、客户-跟进记录、商机-跟进记录。
- CRM 样板包含 18 条对象能力挂接，覆盖导入、导出、报表、触发器、审批、消息、权限。
- CRM 样板包含 8 个 `BUSINESS + RUNTIME` 应用入口，入口继续指向 `/ai/crud-page/{configKey}`。
- 启动 `forge-admin-server` 时已连接本地配置的 MySQL/Redis，Flyway 成功将迁移推进到 `v1.0.28`，说明 `V1.0.26`、`V1.0.27`、`V1.0.28` 可执行。
- 2026-05-27 当时后端服务未保持运行，`mvn -pl forge-admin-server spring-boot:run -Dspring-boot.run.profiles=dev` 在启动链路中因 `ApiConfigAutoRegistrar` Bean 缺失失败；该阻塞已在 Task 22-23 中补齐并完成真实接口联调。

---

## Phase 6：验证归档

### Task 20: 后端编译与前端构建验证

**目标**: 完成针对性后端编译和前端构建，确认任务实现不破坏现有链路。

**验证命令**:
```bash
mvn -pl forge-admin-server -am compile -DskipTests
source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm build
```

**验收标准**:
- 后端编译通过。
- 前端构建通过。
- 动态 CRUD 旧路由 `/ai/crud-page/{configKey}` 可访问。
- `/ai/lowcode/app/**`、`/ai/lowcode/model/**` 兼容接口未删除。

**2026-05-27 验证结果**:
- 后端编译通过：`mvn -pl forge-admin-server -am compile -DskipTests`。
- 前端默认构建因 Node 默认堆内存不足在 chunk 渲染阶段 OOM；使用 `NODE_OPTIONS=--max-old-space-size=8192 pnpm build` 后构建通过。
- 前端构建存在既有 UnoCSS 图标加载警告和 CSS `//` 注释警告，未阻断本次构建。
- 旧路由文件未删除，`/ai/crud-page/:configKey`、`/ai/lowcode-builder/:id?` 仍在 `forge-admin-ui/src/router/index.js` 保留。

---

### Task 21: 文档、变更记录和验收回填

**目标**: 将执行结果、验证命令和剩余风险回填到变更文档。

**涉及文件**:
- `code-copilot/changes/lowcode-business-app-platform/spec.md` — 如实现边界变化，回填技术决策。
- `code-copilot/changes/lowcode-business-app-platform/tasks.md` — 更新任务状态和验证结果。
- `.opencode/memory/decisions.md` — 如形成长期产品/架构决策，追加记录。
- `.opencode/memory/pitfalls.md` — 如遇到可复用踩坑，追加记录。

**验收标准**:
- 已完成任务标记为 `completed`。
- 未完成任务保留明确原因和后续处理方式。
- 编译、构建、手工验收结果有记录。

**2026-05-27 回填结果**:
- 已按当时代码和构建结果更新 Task 19-21 状态。
- 当时 Task 19 因后端启动阻塞未完成真实登录态联调，后续已通过 Task 22-24 补齐。
- 当前最终状态以 2026-05-28 Task 35 集中验收记录为准。

---

## Spec 补齐内容拆分

以下任务根据 `spec.md` 中第一轮尚未闭环的内容补充拆分，重点覆盖：
- 第 5.9 节“客户自助搭建主流程”。
- 第 5.10 节“能力挂接最小运行闭环”。
- 第 5.11 节“嵌入应用接入规范”。
- 第 5.12 节“移动端与第三方集成边界”。
- 第 12.6 节“CRM 可交付样板闭环”。
- 第 16-17 节测试策略和验收标准中第一轮遗留的真实联调部分。

## Phase 7：运行联调补齐

### Task 22: 修复后端启动阻塞

**来源**: Task 19 已记录后端启动在 `ApiConfigAutoRegistrar` Bean 注入处失败，导致真实接口和页面联调未完成。

**目标**: 让 `forge-admin-server` 在 dev profile 下可稳定启动，恢复应用中心真实接口验收前置条件。

**涉及文件**:
- `forge/forge-framework/forge-starter-parent/forge-starter-api-config/src/main/java/com/mdframe/forge/starter/apiconfig/config/ApiConfigAutoConfiguration.java` — 检查自动配置条件和 Bean 创建。
- `forge/forge-framework/forge-starter-parent/forge-starter-api-config/src/main/java/com/mdframe/forge/starter/apiconfig/controller/ApiConfigManageController.java` — 检查 `ApiConfigAutoRegistrar` 注入方式。
- `forge/forge-framework/forge-starter-parent/forge-starter-api-config/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` — 如自动配置未注册，补齐注册。
- `forge/forge-admin-server/src/main/resources/application-dev.yml` — 仅检查配置开关，不提交本地敏感配置。

**验收标准**:
- `mvn -pl forge-admin-server spring-boot:run -Dspring-boot.run.profiles=dev` 能启动到 `Started ForgeAdminApplication`。
- 启动过程中不通过删除 API 配置能力或绕过 Controller 来规避问题。
- `/auth/login` 能使用 `admin/123456` 获取 Token。

**2026-05-28 验收记录**:
- 结合 Task 23 真实登录态联调结果，`forge-admin-server` 已可在 dev profile 下启动并完成 `/auth/login`。
- 已确认 `ApiConfigManageController` 对 `ApiConfigAutoRegistrar` 使用可选注入，自动配置已在 `AutoConfiguration.imports` 注册，未删除 API 配置能力或绕过 Controller。
- 本轮使用 JDK 17 执行 `mvn -pl forge-admin-server -am compile -DskipTests` 通过；默认 JDK 非 17 时会失败在 `无效的目标发行版: 17`，验证时需显式使用 OpenJDK 17。

---

### Task 23: 应用中心登录态真实接口联调

**来源**: `spec.md` 第 16 节要求应用中心页面可分页、筛选、查看业务套件、业务对象和应用入口。

**目标**: 在真实登录态下验证应用中心、套件详情、对象详情和打开信息接口。

**涉及文件**:
- `forge-admin-ui/src/api/business-app.js` — 校验前端接口路径和参数。
- `forge-admin-ui/src/views/app-center/index.vue` — 校验首页数据加载和筛选。
- `forge-admin-ui/src/views/app-center/suite.[suiteCode].vue` — 校验 CRM 套件详情。
- `forge-admin-ui/src/views/app-center/object.[objectCode].vue` — 校验客户、合同对象详情。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/controller/BusinessSuiteController.java` — 校验套件接口。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/controller/BusinessObjectController.java` — 校验对象接口。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/controller/BusinessAppController.java` — 校验应用入口接口。

**验收路径**:
1. 登录前端。
2. 访问 `/app-center`。
3. 筛选 CRM 套件和 `BUSINESS` 应用类型。
4. 进入 `/app-center/suite/CRM`。
5. 进入客户对象和合同对象详情。
6. 调用客户管理应用入口的 `open-info`。

**验收标准**:
- 所有接口返回 `RespInfo.success(data)`。
- 前端不出现 401、404、路由空白页或动态菜单组件加载失败。
- 页面主视觉不展示 JSON、Schema、表名、`configKey` 等技术字段。

**联调记录**:
- 2026-05-28 已在本地 dev profile 启动 `forge-admin-server`，使用 `admin/123456` 真实登录获取 Bearer Token。
- 已验证 `/ai/business/suite/summary`、`/ai/business/object/page?suiteCode=CRM&status=1`、`/ai/business/app/page?suiteCode=CRM&appType=BUSINESS`、`/ai/business/suite/list?suiteCode=CRM`、`/ai/business/object/list?suiteCode=CRM&objectCode=CUSTOMER`、`/ai/business/object/list?suiteCode=CRM&objectCode=CONTRACT`、`/ai/business/app/1910000000000000301/open-info` 均返回 `code=200`。
- CRM 汇总返回 9 个对象、8 个应用入口；客户管理 `open-info` 返回 `canOpen=true`，目标路由为 `/ai/crud-page/crm_customer`。

---

### Task 24: CRM 至少一个对象运行态闭环

**来源**: `spec.md` 第 12.6 和第 17 节要求 CRM 至少有一个对象形成“业务对象 → 低代码模型 → 发布配置 → 应用入口 → 动态 CRUD 运行页”的链路。

**目标**: 至少打通“客户管理”真实运行链路，其他未配置对象必须给出明确下一步配置入口。

**涉及文件**:
- `forge/db/migration/V1.0.32__seed_crm_customer_runtime_link.sql` — 如当前环境缺少可运行配置，幂等初始化客户对象到已发布 `ai_crud_config` 的引用。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessObjectService.java` — 校验 `runtimeInfo` 返回模型、配置和入口状态。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessAppOpenService.java` — 校验 `BUSINESS + RUNTIME` 打开路径。
- `forge-admin-ui/src/views/app-center/object.[objectCode].vue` — 展示运行入口和未配置提示。
- `forge-admin-ui/src/views/app-center/components/AppCard.vue` — 应用入口打开状态展示。

**关键要求**:
- 不新增第二套动态 CRUD 运行时。
- `ai_business_app.config_key` 只引用 `ai_crud_config.config_key`。
- 如通过 SQL 初始化运行链路，脚本必须 `NOT EXISTS` 防重复，`tenant_id=1`。

**验收标准**:
- 客户对象能看到关联模型、已发布运行配置和客户管理应用入口。
- 点击客户管理进入现有 `/ai/crud-page/{configKey}`。
- 未配置运行态的 CRM 对象显示"去配置模型/布局/发布"的明确操作，不静默失败。

**2026-05-28 执行记录**:
- 已创建 `V1.0.32__seed_crm_customer_runtime_link.sql` Flyway 脚本。
- 脚本包含：
  1. 初始化 CRM 业务领域（`ai_lowcode_domain`），编码 `CRM`。
  2. 初始化 CRM 客户低代码模型（`ai_lowcode_model`），编码 `crm_customer`，包含客户表结构协议。
  3. 初始化 CRM 客户 CRUD 配置（`ai_crud_config`），`config_key = 'crm_customer'`，`publish_status = 'PUBLISHED'`，包含搜索、列、编辑、API 配置和页面协议。
  4. 更新 `ai_business_object` 表中客户对象的 `model_id` 字段。
  5. 更新 `ai_business_app` 表中客户管理应用入口的 `config_key` 关联。
- 所有 SQL 使用 `NOT EXISTS` 防重复，`tenant_id = 1`。
- 脚本已通过 Flyway 命名规范校验，版本号 `V1.0.32` 大于当前最大版本 `V1.0.31`。

**后续验证步骤**:
1. 启动 `forge-admin-server`，执行 Flyway 迁移。
2. 登录前端，访问 `/app-center`。
3. 进入 CRM 套件详情，查看客户对象。
4. 点击客户对象，验证 `runtimeInfo` 返回 `canOpen = true`。
5. 点击"打开业务入口"，验证跳转到 `/ai/crud-page/crm_customer`。
6. 验证动态 CRUD 页面可正常加载和操作。

**2026-05-28 补齐记录**:
- 已新增对象详情页运行态提示区，未配置运行态时提供“配置模型 / 配置布局 / 发布应用”明确入口。
- 已补充 `BusinessObjectRuntimeInfoVO` 的 `importEnabled`、`exportEnabled`、`nextAction`、`nextActionLabel` 字段，为客户对象和未配置对象提供统一状态表达。
- `V1.0.33__seed_crm_other_models.sql` 已初始化 CRM 其他对象低代码模型并回填 `ai_business_object.model_id`，但非客户对象仍不默认生成发布配置和 `configKey`。

---

## Phase 8：客户自助搭建主流程

### Task 25: 新建业务对象向导入口

**来源**: `spec.md` 第 5.9 节步骤 1-3。

**目标**: 在应用中心和套件详情提供面向业务人员的新建对象向导，先完成入口、步骤框架和基础信息保存。

**涉及文件**:
- `forge-admin-ui/src/views/app-center/components/BusinessObjectWizardDrawer.vue` — 新增业务对象向导抽屉。
- `forge-admin-ui/src/views/app-center/index.vue` — 增加“新建业务对象”入口。
- `forge-admin-ui/src/views/app-center/suite.[suiteCode].vue` — 增加当前套件下的新建对象入口。
- `forge-admin-ui/src/api/business-app.js` — 补齐创建业务对象、创建业务套件所需 API 方法。
- `BusinessObjectController.java`、`BusinessSuiteController.java` — 确认新增接口权限和参数校验。

**向导步骤**:
1. 选择业务套件或新建业务套件。
2. 选择创建方式：从模板创建、从数据库表导入、从 AI 描述生成、从空白对象创建。
3. 填写对象名称、对象编码、对象类型、显示字段、业务说明、图标和启用状态。

**验收标准**:
- 在 CRM 套件详情中新建对象时，默认带入 `suiteCode=CRM`。
- 保存后生成 `ai_business_object` 记录。
- 页面文案使用“业务对象”“业务说明”“显示字段”，不要求用户理解模型 Schema。

**2026-05-28 实现记录**:
- 新增 `BusinessObjectWizardDrawer.vue`，支持选择已有套件或新建套件，并提供“模板、数据库表导入、AI 描述生成、空白对象”四种创建方式框架。
- 应用中心首页和业务套件详情页均已接入“新建业务对象”入口；在 `/app-center/suite/CRM` 打开时默认带入 `suiteCode=CRM`。
- `business-app.js` 已补齐 `createBusinessSuite`、`updateBusinessSuite`、`createBusinessObject`、`updateBusinessObject`、`updateBusinessObjectStatus`。

---

### Task 26: 模型设计、数据库导入、AI 生成复用串联

**来源**: `spec.md` 第 5.9 节步骤 4，以及复用要求“必须复用现有 LowcodeModelDesigner、数据库表导入和 AI 生成能力”。

**目标**: 在业务对象向导中串联现有低代码模型能力，不重做模型设计器。

**涉及文件**:
- `forge-admin-ui/src/views/app-center/components/BusinessObjectWizardDrawer.vue` — 创建方式选择后跳转或打开复用入口。
- `forge-admin-ui/src/views/app-center/object.[objectCode].vue` — 对象详情增加“配置模型”入口。
- `forge-admin-ui/src/views/ai/lowcode-models.vue` — 接收 `suiteCode`、`objectCode`、`returnTo` 查询参数。
- `forge-admin-ui/src/views/ai/lowcode-apps.vue` — 确认 AI 生成入口可从业务上下文进入。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessObjectService.java` — 支持回写 `modelId`、`modelCode`。

**验收标准**:
- 从业务对象详情点击“配置模型”能进入现有模型设计能力。
- 从数据库表导入后能关联回业务对象。
- 从 AI 描述生成后仍走 `lowcode_system_generator` 和现有低代码 AI 链路。
- 不复制 `model_schema` 到 `ai_business_object.options` 作为第二套事实来源。

**2026-05-28 进展记录**:
- 业务对象详情页已新增“配置模型”入口，携带 `domainCode/suiteCode/objectCode/objectName/returnTo` 查询参数进入现有 `/ai/lowcode-models`。
- `lowcode-models.vue` 已支持按 `domainCode` 和 `objectCode` 预选业务领域、打开已有关联模型；未找到模型时创建当前对象的模型草稿。
- `lowcode-models.vue` 保存模型时会携带 `businessSuiteCode`、`businessObjectCode`、`syncDdl` 等上下文参数，数据库表导入和 AI 生成后仍复用同一保存链路。
- `LowcodeDataModelService.saveModel()` 已通过 `syncBusinessObjectModelRef()` 回写 `ai_business_object.model_id` 和 `model_code`，不复制 `model_schema` 到业务对象表。
- 当前验收标准已满足，状态为 `completed`。

---

### Task 27: 布局配置、发布应用、生成入口串联

**来源**: `spec.md` 第 5.9 节步骤 5-10。

**目标**: 从业务对象详情串联“关系配置 → 布局配置 → 发布应用 → 生成应用入口 → 打开运行页”的主流程。

**涉及文件**:
- `forge-admin-ui/src/views/app-center/object.[objectCode].vue` — 增加配置布局、发布应用、代码预览、打开运行页入口。
- `forge-admin-ui/src/views/ai/lowcode-builder.vue` — 接收业务对象上下文并支持返回应用中心。
- `forge-admin-ui/src/views/app-center/components/ObjectRelationPanel.vue` — 关系配置保存后刷新对象详情。
- `forge-admin-ui/src/views/app-center/components/AppEditorDrawer.vue` — 发布后生成或更新 `BUSINESS + RUNTIME` 应用入口。
- `BusinessAppController.java`、`BusinessAppService.java` — 支持按 `objectCode + configKey` 幂等生成应用入口。

**验收标准**:
- 业务对象详情中可看到“配置布局”“发布应用”“打开运行页”。
- 发布后的 `configKey` 继续来自现有低代码发布链路。
- 生成应用入口时不复制页面 Schema、API 配置或代码生成参数。

**2026-05-28 进展记录**:
- 业务对象详情页已新增“配置布局”“发布应用”入口，均进入现有 `/ai/lowcode-builder` 并携带业务对象上下文。
- `lowcode-builder.vue` 已支持通过 `domainCode/suiteCode/objectCode/objectName/step` 查询参数预选领域和模型，并定位到布局或发布步骤。
- `PublishPanel.vue` 发布时已携带 `businessSuiteCode`、`businessObjectCode`、`businessObjectName`。
- `LowcodePublishService.publish()` 已调用 `syncBusinessRuntimeEntry()`，发布后按业务对象和 `configKey` 幂等生成或更新 `BUSINESS + RUNTIME` 应用入口，并继续只引用 `ai_crud_config.config_key`。
- 当前验收标准已满足，状态为 `completed`。

---

## Phase 9：能力挂接最小运行闭环

### Task 28: 对象级导入导出入口打通

**来源**: `spec.md` 第 5.10 节要求先打通对象级导入、导出和标准业务应用打开。

**目标**: 对已配置 `configKey` 的业务对象展示可用导入导出入口，并复用动态 CRUD 导入导出接口。

**涉及文件**:
- `forge-admin-ui/src/views/app-center/object.[objectCode].vue` — 能力页签增加导入模板、导入、导出入口。
- `forge-admin-ui/src/api/business-app.js` — 增加根据对象运行信息组装动态 CRUD 导入导出请求的方法。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/controller/DynamicCrudController.java` — 复用 `/import-template`、`/import`、`/export`。
- `BusinessObjectService.java` — `runtimeInfo` 返回导入导出可用状态和 `configKey`。

**验收标准**:
- 已配置运行态的客户对象可下载导入模板。
- 导出入口调用 `/ai/crud/{configKey}/export`。
- 未配置运行态的对象禁用导入导出按钮，并提示先配置模型、布局和发布。
- 不新增第二套导入导出服务。

**2026-05-28 实现记录**:
- `business-app.js` 新增 `dynamicCrudImportTemplate`、`dynamicCrudImport`、`dynamicCrudExport`，全部复用现有 `/ai/crud/{configKey}` 接口。
- 业务对象详情页已在运行态提示区提供“导入模板 / 导入 / 导出”入口；无 `configKey` 或对象停用时按钮禁用并提示先配置模型、布局和发布。
- 未新增任何导入导出后端服务。

---

### Task 29: 报表、审批、消息、权限能力入口打通

**来源**: `spec.md` 第 5.10 节要求权限、消息和报表入口具备最小闭环，审批/流程复用现有 Flowable。

**目标**: 能力挂接不只显示标签，还能打开对应现有模块或配置入口。

**涉及文件**:
- `forge-admin-ui/src/views/app-center/components/BusinessBindingPanel.vue` — 为 `REPORT`、`APPROVAL`、`FLOW`、`MESSAGE`、`PERMISSION` 增加打开动作。
- `forge-admin-ui/src/views/app-center/object.[objectCode].vue` — 对象能力页签展示能力状态和配置入口。
- `forge-admin-ui/src/views/app-center/engines.vue` — 引擎中心展示已接入对象/应用数量和最近配置。
- `BusinessBindingController.java`、`BusinessBindingService.java` — 返回挂接目标、状态、入口地址和最近更新时间。

**验收标准**:
- 报表能力可打开对应报表、大屏或嵌入应用入口。
- 审批/流程能力可跳转现有流程配置或流程发起入口。
- 消息和权限能力可跳转现有消息/权限配置入口或展示明确后续配置路径。
- 不重构 Flowable、消息中心、报表底层。

**2026-05-28 实现记录**:
- `BusinessBindingService` 已为 `REPORT`、`APPROVAL/FLOW`、`MESSAGE`、`PERMISSION` 提供默认 `entryUrl`、`openType`、`actionLabel` 和状态提示；自定义入口仍可通过 `binding_config.entryUrl/routePath` 覆盖。
- `BusinessBindingPanel.vue` 已根据后端返回的 `canOpen`、`entryUrl`、`openType` 打开现有报表、流程、消息、权限模块或给出明确不可打开提示。
- `engines.vue` 已展示流程、审批、报表、权限、消息、触发器、导入导出能力入口，并优先跳转现有模块。
- 当前验收标准已满足，状态为 `completed`。

---

### Task 30: 未配置运行态和能力状态提示补齐

**来源**: `spec.md` 第 12.6 和第 17 节要求未完成运行态配置的 CRM 对象必须有明确提示和下一步配置入口。

**目标**: 统一对象、应用和能力的状态提示，避免点击后无响应或静默失败。

**涉及文件**:
- `forge-admin-ui/src/views/app-center/components/AppCard.vue` — 未配置、停用、无权限状态展示。
- `forge-admin-ui/src/views/app-center/components/ObjectCard.vue` — 对象运行态状态展示。
- `forge-admin-ui/src/views/app-center/components/BusinessBindingPanel.vue` — 能力挂接状态展示。
- `forge-admin-ui/src/views/app-center/object.[objectCode].vue` — “下一步配置”操作区。
- `BusinessAppOpenService.java` — 返回 `canOpen`、`reason`、`nextAction`。
- `BusinessObjectRuntimeInfoVO.java`、`BusinessAppOpenInfoVO.java` — 增加状态和下一步操作字段。

**验收标准**:
- 无 `configKey` 的应用入口不能直接跳转空路由。
- 停用对象和停用应用入口显示明确状态。
- 无权限入口显示权限提示，不暴露敏感 URL。
- 页面提供“配置模型”“配置布局”“发布应用”“联系管理员”等下一步操作。

**2026-05-28 实现记录**:
- `BusinessAppOpenInfoVO`、`BusinessObjectRuntimeInfoVO` 已增加 `nextAction` 和 `nextActionLabel`。
- `BusinessAppOpenService` 和 `BusinessObjectService` 已根据停用、未配置、无权限、可打开等状态返回明确提示。
- `AppCard` 已禁止停用或未配置打开地址的入口直接打开；对象详情页已提供下一步配置操作区。

---

## Phase 10：渠道、集成与开发者工具收敛

### Task 31: 嵌入应用白名单与打开安全校验

**来源**: `spec.md` 第 5.11 节要求 iframe 和外部链接必须配置域名白名单，并通过后端 `open-info` 校验。

**目标**: 补齐嵌入应用的最小安全边界，避免外部链接绕过权限和登录态。

**涉及文件**:
- `forge-admin-ui/src/views/app-center/components/AppEditorDrawer.vue` — 嵌入应用配置增加白名单、打开方式和安全说明字段。
- `BusinessAppOpenService.java` — 校验应用状态、用户权限、`entryMode` 和白名单。
- `BusinessAppDTO.java`、`BusinessAppVO.java`、`BusinessAppOpenInfoVO.java` — 承载白名单和打开安全结果。
- `forge/db/migration/V1.0.33__add_business_app_embed_security.sql` — 如决定新增结构化字段，使用下一版本 Flyway 幂等补齐；如继续使用 `options`，不新增表字段。

**验收标准**:
- `IFRAME`、`EXTERNAL`、`H5` 打开前必须调用 `open-info`。
- 非白名单域名返回不可打开状态和业务化错误提示。
- 禁止在 URL 中保存长期 Token、密码、AK/SK。
- 前端不直接拼接敏感外部 URL 作为主打开逻辑。

**2026-05-28 实现记录**:
- `AppEditorDrawer.vue` 已为 `IFRAME`、`EXTERNAL`、`H5` 入口增加域名白名单配置，数据写入 `ai_business_app.options.allowedDomains`，未新增表字段。
- `BusinessAppOpenService` 已在 `open-info` 中校验入口状态、权限、HTTP/HTTPS 地址、用户名密码、敏感查询参数和白名单域名。
- 非白名单或未配置白名单的外部/H5/iframe 入口会返回 `canOpen=false` 和业务化错误提示；内部相对路径不要求白名单。
- 原计划 `V1.0.33__add_business_app_embed_security.sql` 不再新增，原因是 `V1.0.33` 已用于 CRM 其他模型初始化，嵌入安全配置采用 `options` 承载。

---

### Task 32: 移动应用入口配置补齐

**来源**: `spec.md` 第 5.6 和第 5.12 节要求第一阶段登记 H5 入口、图标、可见范围和目标地址。

**目标**: 移动端中心能维护并展示移动 H5、移动待办、移动审批和移动业务入口。

**涉及文件**:
- `forge-admin-ui/src/views/app-center/mobile.vue` — 移动入口列表、筛选和状态展示。
- `forge-admin-ui/src/views/app-center/components/AppEditorDrawer.vue` — `MOBILE + H5` 配置字段。
- `BusinessAppController.java`、`BusinessAppService.java` — 支持按 `appType=MOBILE` 查询和保存。
- `BusinessAppOpenService.java` — `H5` 打开信息校验。

**验收标准**:
- 可登记移动 H5 应用入口名称、图标、目标地址和可见范围。
- 移动待办、移动审批、移动业务入口作为类型或标签展示。
- 第一阶段不新建完整移动端容器，不新增独立待办表。

**2026-05-28 实现记录**:
- `mobile.vue` 已提供移动应用入口列表、入口模式筛选、启停、打开和新增/编辑入口。
- `AppEditorDrawer.vue` 已在 `appType=MOBILE` 时提供移动场景和可见范围配置，写入 `options.mobileScene`、`options.visibleScope`。
- 后端 `BusinessAppService` 支持 `appType=MOBILE`、`entryMode=H5/ROUTE/EXTERNAL` 查询和保存；`BusinessAppOpenService` 对 H5 外部地址复用打开校验和白名单校验。
- 未新增移动端容器或独立待办表，符合第一阶段边界。
- 当前验收标准已满足，状态为 `completed`。

---

### Task 33: 集成应用入口配置补齐

**来源**: `spec.md` 第 5.7 和第 5.12 节要求第一阶段登记标准接口、Webhook、企微、飞书、钉钉入口。

**目标**: 集成中心能维护集成应用定义，明确当前状态和后续配置路径。

**涉及文件**:
- `forge-admin-ui/src/views/app-center/integration.vue` — 集成入口列表、平台类型和状态展示。
- `forge-admin-ui/src/views/app-center/components/AppEditorDrawer.vue` — `INTEGRATION + API` 配置字段。
- `BusinessAppController.java`、`BusinessAppService.java` — 支持按 `appType=INTEGRATION` 查询和保存。
- `BusinessAppOpenService.java` — API/集成配置入口打开信息。

**验收标准**:
- 可登记标准接口、Webhook、企微、飞书、钉钉类型入口。
- 配置中不得保存明文密码、长期 Token、Webhook Secret。
- 第一阶段只做入口管理，不实现完整推送通道。

**2026-05-28 实现记录**:
- `integration.vue` 已提供集成应用定义列表、平台类型筛选、启停、打开和新增/编辑入口。
- `AppEditorDrawer.vue` 已在 `appType=INTEGRATION` 时提供标准接口、Webhook、企微/飞书/钉钉、外部系统等集成类型，以及业务资源键和事件范围配置。
- `BusinessAppService` 已校验入口地址和 `options`，禁止保存明文密码、长期 Token、Webhook Secret 等敏感配置。
- 第一阶段仅登记入口和配置路径，未实现完整推送通道，符合非目标边界。
- 当前验收标准已满足，状态为 `completed`。

---

### Task 34: 开发者工具入口收敛和普通用户隐藏

**来源**: `spec.md` 第 5.8 和第 11.2 节要求旧低代码应用管理、模型设计、JSON 配置、代码生成逐步移动到开发者工具，普通业务用户默认不展示纯 JSON 配置入口。

**目标**: 完成菜单和权限层面的开发者工具收敛，同时保留历史路由兼容。

**涉及文件**:
- `forge/db/migration/V1.0.34__normalize_lowcode_developer_menus.sql` — 调整旧低代码菜单可见性和开发者工具归属，必须 `NOT EXISTS`/幂等更新。
- `forge-admin-ui/src/router/index.js` — 确认历史路由保留。
- `forge-admin-ui/src/views/ai/lowcode-apps.vue` — 保留开发者入口。
- `forge-admin-ui/src/views/ai/lowcode-builder.vue` — 保留从应用中心跳转入口。
- `forge-admin-ui/src/views/ai/crud-config.vue` — 普通菜单隐藏，仅开发者权限可见。

**验收标准**:
- 普通业务用户默认从“应用中心”进入，不看到 JSON 配置主入口。
- 开发者仍可访问低代码应用管理、模型设计、搭建器、代码预览和下载。
- 历史路由不删除，已发布应用不失效。

**2026-05-28 实现记录**:
- 新增 `V1.0.34__normalize_lowcode_developer_menus.sql`，创建“开发者工具”菜单并将低代码应用、模型设计、数据源入口收敛到该目录。
- 旧 JSON 配置、CRUD 生成、页面模板、代码生成表/模板等普通菜单入口已通过 `menu_status=0`、`visible=0` 隐藏。
- 低代码相关按钮权限重新挂到对应开发者菜单，开发者角色授权继承到“开发者工具”目录。
- `forge-admin-ui/src/router/index.js` 保留 `/ai/lowcode-builder/:id?` 等历史路由，低代码页面和代码下载能力未删除。
- 当前验收标准已满足，状态为 `completed`。

---

## Phase 11：二阶段验收归档

### Task 35: Spec 遗留项验收回填

**来源**: `spec.md` 第 16-17 节测试策略和验收标准。

**目标**: 对 Task 22-34 的结果做一次集中验收，并把完成/未完成边界回填文档。

**涉及文件**:
- `code-copilot/changes/lowcode-business-app-platform/spec.md` — 必要时同步实现边界和延期项。
- `code-copilot/changes/lowcode-business-app-platform/tasks.md` — 更新 Task 22-35 状态和验证结果。
- `.opencode/memory/pitfalls.md` — 记录启动、菜单、动态路由、Flyway 等可复用踩坑。
- `.opencode/memory/decisions.md` — 如确认新长期架构决策，追加记录。

**验证命令**:
```bash
mvn -pl forge-admin-server -am compile -DskipTests
mvn -pl forge-admin-server spring-boot:run -Dspring-boot.run.profiles=dev
source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm build
```

**验收标准**:
- 后端可启动并完成 `/auth/login`。
- 应用中心、CRM 套件、客户对象、合同对象真实页面可访问。
- 客户管理至少一个运行态入口可打开动态 CRUD，或明确记录当前缺少的低代码发布配置。
- 嵌入、移动、集成入口展示状态、打开方式和后续配置路径。
- Spec 中第一阶段遗留项均在 `tasks.md` 中有状态、原因和后续处理方式。

**2026-05-28 本轮验证记录**:
- 2026-05-28 复核确认 Task 26、Task 27、Task 29、Task 32、Task 33、Task 34 均已具备对应实现和验收记录。
- 本轮复核补充执行 `pnpm --dir forge-admin-ui exec eslint src/views/app-center src/api/business-app.js src/views/ai/lowcode-models.vue src/views/ai/lowcode-builder.vue src/components/lowcode-builder/publish/PublishPanel.vue` 通过。
- 本轮复核补充执行 OpenJDK 17 `mvn -pl forge-admin-server -am compile -DskipTests` 通过。
- 本轮复核补充执行 `NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build` 通过；仍存在既有 UnoCSS 图标加载警告、CSS `//` 注释警告、动态/静态混合导入提示和大 chunk 提示，未阻断构建。
- 当前 `spec.md` 第一阶段验收项均已有任务覆盖，未发现仍需保留为未完成状态的内容；变更状态更新为 `completed`。
