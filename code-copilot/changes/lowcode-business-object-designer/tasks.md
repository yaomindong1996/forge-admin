# 任务清单：lowcode-business-object-designer
> status: apply
> created: 2026-05-29
> 拆分顺序：产品基线 → 数据与协议 → 后端聚合服务 → 字段/布局/发布闭环 → 前端设计器 → 入口菜单 → 验证归档
> 原则：业务对象设计器是普通用户主入口；低代码模型管理保留为高级入口；查询 SQL 写 Mapper XML；分页参数使用 `pageNum/pageSize`；内置数据 `tenant_id=1`；Flyway 脚本必须防重复；普通模式不展示表名、DDL、Schema、configKey。

## 前置条件

- [x] 已确认本变更目标是“字段驱动的业务对象搭建器体验”，不是重写低代码运行时。
- [x] 已确认 `ai_lowcode_model`、`ai_crud_config`、`DynamicCrudController`、`AiCrudPage`、`LowcodePublishService` 继续作为底层能力复用。
- [x] 已确认普通用户主入口走“应用中心 → 业务套件 → 业务对象 → 设计对象”。
- [x] 已确认低代码模型管理、CRUD 配置、代码生成、DDL 预览下沉到高级/开发者入口。
- [x] 已确认业务对象设计器首期优先复用现有 `LowcodeModelSchema`、`LowcodePageSchema` JSON，不新建字段事实表。
- [x] 已确认业务对象设计器路由：新增 `/app-center/object/:objectCode/designer`。
- [x] 已确认已发布字段删除策略：默认隐藏/停用，不直接物理删除。
- [x] 已确认表单画布首期复用 `CanvasFormDesigner` 和现有低代码页面搭建能力。

## 阶段总览

| 阶段 | 目标 | 包含任务 | 交付结果 |
|------|------|----------|----------|
| Phase 0 | 产品基线 | Task 0 | 冻结入口、权限、字段删除、设计器路由等关键决策 |
| Phase 1 | 数据与协议 | Task 1-4 | 数据脚本、DTO/VO、字段/布局/发布检查协议 |
| Phase 2 | 后端主链路 | Task 5-11 | 设计器聚合、字段管理、布局保存、关系同步、发布与版本 |
| Phase 3 | 前端设计器 | Task 12-20 | 设计器壳、字段、表单、列表、详情、关系、发布、高级配置 |
| Phase 4 | 入口与菜单 | Task 21-23 | 应用中心入口、创建向导跳转、开发者菜单收敛 |
| Phase 5 | 质量与验收 | Task 24-27 | 构建、接口、页面、业务闭环、文档回填 |

## 任务总览

| Task | 阶段 | 名称 | 状态 | 优先级 |
|------|------|------|------|--------|
| Task 0 | Phase 0 | 业务对象设计器产品基线确认 | completed | P0 |
| Task 1 | Phase 1 | 设计器数据迁移脚本 | completed | P0 |
| Task 2 | Phase 1 | 设计器 DTO/VO 和状态常量 | completed | P0 |
| Task 3 | Phase 1 | 字段类型、字段模板和默认 Schema 工具 | completed | P0 |
| Task 4 | Phase 1 | 发布检查协议与版本协议 | completed | P0 |
| Task 5 | Phase 2 | 业务对象设计器聚合接口 | completed | P0 |
| Task 6 | Phase 2 | 字段管理后端闭环 | completed | P0 |
| Task 7 | Phase 2 | 表单/列表/详情布局保存接口 | completed | P0 |
| Task 8 | Phase 2 | 关系配置与模型关系同步 | completed | P0 |
| Task 9 | Phase 2 | 自定义操作与权限流程摘要 | completed | P1 |
| Task 10 | Phase 2 | 发布检查与业务对象发布门面 | completed | P0 |
| Task 11 | Phase 2 | 业务对象设计版本和回滚 | completed | P1 |
| Task 12 | Phase 3 | 前端 API 与路由接入 | completed | P0 |
| Task 13 | Phase 3 | 业务对象设计器外壳 | completed | P0 |
| Task 14 | Phase 3 | 业务字段管理组件 | completed | P0 |
| Task 15 | Phase 3 | 表单设计组件业务化包装 | completed | P0 |
| Task 16 | Phase 3 | 列表设计组件业务化包装 | completed | P0 |
| Task 17 | Phase 3 | 详情设计组件 | completed | P1 |
| Task 18 | Phase 3 | 关系、操作、权限流程面板 | completed | P1 |
| Task 19 | Phase 3 | 发布检查、发布与版本面板 | completed | P0 |
| Task 20 | Phase 3 | 高级配置与开发者模式 | completed | P1 |
| Task 21 | Phase 4 | 应用中心对象详情入口重构 | completed | P0 |
| Task 22 | Phase 4 | 创建向导、模板、AI、数据库导入跳转收敛 | completed | P1 |
| Task 23 | Phase 4 | 菜单权限与普通用户信息隔离 | completed | P0 |
| Task 24 | Phase 5 | 后端编译与接口验证 | completed | P0 |
| Task 25 | Phase 5 | 前端构建与页面验证 | completed | P0 |
| Task 26 | Phase 5 | CRM 客户对象端到端验收 | completed | P0 |
| Task 27 | Phase 5 | Spec、任务和执行日志回填 | completed | P0 |

---

## 追加修复：fcDesigner 参数到运行态 AiForm 同步

- [x] 表单配置同步：`labelPosition/labelWidth/size/showMessage/inlineMessage/hideRequiredAsterisk/style/class` 转换为运行态 `editLabelPlacement/editLabelWidth/editSize/editShowFeedback/editFormStyle/editFormClass`。
- [x] 组件配置同步：保留 fcDesigner `style/class/wrap` 元信息，并映射为运行态 `componentStyle/componentClass/formItemClass/formItemStyle/showLabel`。
- [x] 校验配置同步：支持 fcDesigner `$required`、`validate`、必填提示语，生成 `required/requiredMessage/rules/trigger`。
- [x] 运行态渲染同步：`AiCrudPage` 透传编辑表单尺寸、反馈和样式；`AiFormItem` 增加统一控制容器承接组件样式。
- [x] 后端运行配置同步：`BusinessObjectDesignerService` 和 `LowcodeRuntimeConfigBuilder` 都补齐同一套字段映射。
- [x] 验证：前端目标文件 eslint 通过；`mvn -pl forge-admin-server -am compile -DskipTests` 通过；`pnpm --dir forge-admin-ui build` 通过（仅保留项目既有 UnoCSS 图标、CSS 注释和 chunk 警告）。

---

## Phase 0：产品基线

### Task 0: 业务对象设计器产品基线确认

**目标**: 在编码前冻结产品主链路，避免后续继续把“数据模型管理”当作普通用户主入口。

**涉及文件**:
- `code-copilot/changes/lowcode-business-object-designer/spec.md` — 回填待澄清项和确认记录。
- `code-copilot/changes/lowcode-business-object-designer/tasks.md` — 根据确认结果调整任务优先级。

**确认事项**:
- 业务对象设计器路由：默认新增 `/app-center/object/:objectCode/designer`。
- 普通用户菜单：默认隐藏“低代码模型管理/CRUD 配置/代码生成”，保留开发者入口。
- 已发布字段删除：默认隐藏/停用，不直接物理删除。
- 字段模板：默认先入库维护常用模板，方便后续扩展。
- 表单画布：默认首期复用 `CanvasFormDesigner`，不引入第二套设计器。
- 在线建表和字段变更：默认需要高级权限。
- 业务对象设计版本：默认新建 `ai_business_object_design_version`，并与 `ai_crud_config_version` 互相追溯。

**验收标准**:
- `spec.md` 第 16 章待澄清项均有明确结论。
- 后续任务不再出现“普通用户直接进模型管理”的实现路径。
- 后续任务不新增第二套低代码运行时。

---

## Phase 1：数据与协议

### Task 1: 设计器数据迁移脚本

**目标**: 补齐业务对象设计状态、运行配置引用、设计版本和字段模板数据结构。

**涉及文件**:
- `forge/db/migration/V1.0.40__add_business_object_designer.sql` — 新增 Flyway 脚本。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/domain/entity/AiBusinessObject.java` — 增加设计器字段映射。
- 新增 `AiBusinessObjectDesignVersion.java` — 业务对象设计版本实体。
- 新增 `AiBusinessFieldTemplate.java` — 字段模板实体。

**脚本要求**:
- 扩展 `ai_business_object`：`design_status`、`config_key`、`last_publish_time`、`last_publish_version`、`designer_options`。
- 新增 `ai_business_object_design_version`：保存对象、版本号、模型快照、页面快照、关系快照、发布状态。
- 新增 `ai_business_field_template`：保存字段模板编码、名称、字段 Schema、套件编码、状态。
- 新增索引：`tenant_id + object_id + version_no`、`tenant_id + template_code`、`tenant_id + suite_code + status`。
- 字段模板内置常用字段：客户名称、联系电话、客户等级、负责人、所属部门、所属地区、备注。
- 所有内置数据 `tenant_id=1`，脚本具备重复执行保护。

**验收标准**:
- Flyway 脚本满足 `CREATE TABLE IF NOT EXISTS` 和新增列前 `information_schema` 检查。
- 不修改历史迁移脚本。
- 实体字段和数据库字段命名一致。

### Task 2: 设计器 DTO/VO 和状态常量

**目标**: 定义前后端统一的业务对象设计器协议。

**涉及文件**:
- 新增 `dto/businessapp/BusinessObjectDesignerDTO.java`。
- 新增 `dto/businessapp/BusinessFieldDTO.java`。
- 新增 `dto/businessapp/BusinessLayoutDTO.java`。
- 新增 `dto/businessapp/BusinessObjectPublishDTO.java`。
- 新增 `vo/businessapp/BusinessObjectDesignerVO.java`。
- 新增 `vo/businessapp/BusinessFieldVO.java`。
- 新增 `vo/businessapp/BusinessLayoutVO.java`。
- 新增 `vo/businessapp/BusinessPublishCheckVO.java`。
- 新增 `vo/businessapp/BusinessObjectDesignVersionVO.java`。
- 新增 `constant/BusinessObjectDesignStatus.java`。
- 新增 `constant/BusinessPublishCheckLevel.java`。

**关键字段**:
- `BusinessObjectDesignerVO`：对象信息、套件信息、模型 Schema、页面 Schema、关系、字段、设计状态、发布状态、是否有未发布变更。
- `BusinessFieldVO`：字段名称、字段编码、字段类型、组件类型、普通属性、高级属性、系统字段标识、引用状态。
- `BusinessPublishCheckVO`：总体状态、阻断项、警告项、通过项、修复动作。

**验收标准**:
- DTO/VO 不直接暴露敏感配置。
- 普通字段和高级字段在协议中可区分。
- 状态常量与前端标签枚举一致。

### Task 3: 字段类型、字段模板和默认 Schema 工具

**目标**: 提供字段类型定义、字段模板读取和业务字段到低代码字段 Schema 的转换工具。

**涉及文件**:
- 新增 `service/businessapp/BusinessFieldSchemaService.java`。
- 新增 `service/businessapp/BusinessFieldTemplateService.java`。
- 新增 `mapper/BusinessFieldTemplateMapper.java`。
- 新增 `mapper/BusinessFieldTemplateMapper.xml`。
- 增强 `dto/lowcode/LowcodeFieldSchema.java` 使用现有字段属性承载业务字段扩展。

**关键能力**:
- 根据中文字段名自动生成稳定字段编码和数据库列名。
- 生成默认组件类型：文本 → input，金额 → input-number，日期 → date，地区 → RegionTreeSelect，人员 → user select。
- 系统字段不可删除、不可改编码。
- 字典字段必须要求 `dictType`。
- 引用对象字段必须要求目标对象和回显字段。

**验收标准**:
- 新增字段只填字段名称和字段类型时能生成完整字段 Schema。
- 字段模板能按套件和通用模板查询。
- Mapper 查询写在 XML 中。

### Task 4: 发布检查协议与版本协议

**目标**: 定义发布前检查项、修复动作和业务对象设计版本快照协议。

**涉及文件**:
- 新增 `vo/businessapp/BusinessPublishCheckItemVO.java`。
- 新增 `dto/businessapp/BusinessObjectDesignVersionDTO.java`。
- 新增 `mapper/BusinessObjectDesignVersionMapper.java`。
- 新增 `mapper/BusinessObjectDesignVersionMapper.xml`。
- 新增 `service/businessapp/BusinessObjectDesignVersionService.java`。

**检查项分类**:
- 字段检查：字段名称、字段编码、重复字段、字段类型、系统字段。
- 页面检查：表单、列表、详情是否引用不存在字段。
- 关系检查：目标对象是否存在、目标对象是否发布、关系字段是否存在。
- 数据表检查：表是否存在、主键是否存在、字段同步状态。
- 发布检查：运行配置是否能生成、权限是否满足。

**验收标准**:
- 检查项支持 `PASS`、`WARN`、`BLOCK`。
- 阻断项包含 `fixAction`、`fixActionLabel`、`fixTarget`。
- 设计版本快照可以追溯到对象、模型、页面和发布版本。

---

## Phase 2：后端主链路

### Task 5: 业务对象设计器聚合接口

**目标**: 后端提供对象设计器一次性加载和保存接口，聚合业务对象、模型、页面、关系和发布状态。

**涉及文件**:
- 新增 `controller/BusinessObjectDesignerController.java`。
- 新增 `service/businessapp/BusinessObjectDesignerService.java`。
- 修改 `service/businessapp/BusinessObjectService.java`。
- 修改 `mapper/BusinessObjectMapper.java`。
- 修改 `mapper/BusinessObjectMapper.xml`。

**接口签名**:
```java
@GetMapping("/{objectId}/designer")
@SaCheckPermission("ai:businessObject:design")
public RespInfo<BusinessObjectDesignerVO> getDesigner(@PathVariable Long objectId) { }

@PutMapping("/{objectId}/designer")
@SaCheckPermission("ai:businessObject:design")
public RespInfo<Void> saveDesigner(@PathVariable Long objectId,
                                    @RequestBody BusinessObjectDesignerDTO dto) { }
```

**实现要点**:
- 根据 `ai_business_object.id` 获取对象。
- 根据 `model_id/model_code` 读取 `ai_lowcode_model.model_schema`。
- 根据 `config_key` 或已关联低代码配置读取 `page_schema`。
- 没有模型时创建默认业务对象模型草稿。
- 没有页面 Schema 时根据字段生成默认列表、表单、详情 Schema。
- 保存时同步业务对象基础信息、模型 Schema 和页面 Schema。

**验收标准**:
- CRM 客户对象能返回设计器完整草稿。
- 未绑定模型的业务对象能返回可编辑空草稿。
- 普通响应不展示 DDL 和原始 JSON 字符串。

### Task 6: 字段管理后端闭环

**目标**: 实现字段新增、修改、删除/停用、排序和引用检查，字段变更自动同步模型和页面 Schema。

**涉及文件**:
- 新增 `service/businessapp/BusinessFieldDesignService.java`。
- 修改 `controller/BusinessObjectDesignerController.java`。
- 修改 `service/lowcode/LowcodeSchemaValidator.java`。
- 修改 `components` 对应前端 API 时再联调。

**接口签名**:
```java
@GetMapping("/{objectId}/fields")
public RespInfo<List<BusinessFieldVO>> listFields(@PathVariable Long objectId) { }

@PostMapping("/{objectId}/fields")
public RespInfo<BusinessFieldVO> addField(@PathVariable Long objectId,
                                           @RequestBody BusinessFieldDTO dto) { }

@PutMapping("/{objectId}/fields/{fieldCode}")
public RespInfo<BusinessFieldVO> updateField(@PathVariable Long objectId,
                                              @PathVariable String fieldCode,
                                              @RequestBody BusinessFieldDTO dto) { }

@DeleteMapping("/{objectId}/fields/{fieldCode}")
public RespInfo<Void> deleteField(@PathVariable Long objectId,
                                  @PathVariable String fieldCode) { }
```

**实现要点**:
- 字段新增时生成 `field`、`columnName`、`componentType`、`dataType`。
- 字段重命名只更新 label，不默认改 `field`。
- 字段删除前检查表单、列表、详情、关系、操作引用。
- 已发布字段删除默认设置隐藏/停用，不物理删除。
- 字段排序同步 `model_schema.fields` 顺序，并刷新默认页面 Schema。

**验收标准**:
- 新增“客户等级”字段后，模型 Schema 和页面默认 Schema 都能看到该字段。
- 删除被表单引用的字段时返回明确阻断原因。
- 运行配置生成时不出现空 label 或 `undefined`。

### Task 7: 表单/列表/详情布局保存接口

**目标**: 实现业务化布局保存接口，分别保存表单、列表、详情配置并校验字段引用。

**涉及文件**:
- 新增 `service/businessapp/BusinessLayoutDesignService.java`。
- 修改 `controller/BusinessObjectDesignerController.java`。
- 修改 `service/lowcode/LowcodeSchemaValidator.java`。
- 复用 `dto/lowcode/LowcodePageSchema.java`。

**接口签名**:
```java
@PutMapping("/{objectId}/layout/form")
public RespInfo<Void> saveFormLayout(@PathVariable Long objectId,
                                      @RequestBody BusinessLayoutDTO dto) { }

@PutMapping("/{objectId}/layout/list")
public RespInfo<Void> saveListLayout(@PathVariable Long objectId,
                                      @RequestBody BusinessLayoutDTO dto) { }

@PutMapping("/{objectId}/layout/detail")
public RespInfo<Void> saveDetailLayout(@PathVariable Long objectId,
                                        @RequestBody BusinessLayoutDTO dto) { }

@PostMapping("/{objectId}/layout/preview")
public RespInfo<AiCrudConfigRenderVO> previewLayout(@PathVariable Long objectId,
                                                    @RequestBody BusinessLayoutDTO dto) { }
```

**实现要点**:
- 表单布局更新 `page_schema` 的 edit zone。
- 列表布局更新 search/table/toolbar/row actions。
- 详情布局更新 detail zone 和 relation tabs。
- 保存前校验所有 fieldRef 都存在。
- 预览调用 `LowcodeRuntimeConfigBuilder` 生成临时运行配置，不落库发布。

**验收标准**:
- 表单布局保存后重新进入设计器能恢复分组和字段顺序。
- 列表列配置保存后预览配置包含对应列。
- 删除字段后的脏引用能被保存接口拦截。

### Task 8: 关系配置与模型关系同步

**目标**: 统一业务对象关系配置、模型关系配置和应用中心关系运行入口。

**涉及文件**:
- 修改 `service/businessapp/BusinessObjectRelationService.java`。
- 修改 `controller/BusinessObjectRelationController.java`。
- 修改 `mapper/BusinessObjectRelationMapper.java`。
- 修改 `mapper/BusinessObjectRelationMapper.xml`。
- 修改 `vo/businessapp/BusinessObjectRelationVO.java`。
- 修改 `dto/businessapp/BusinessObjectRelationDTO.java`。
- 修改 `dto/lowcode/LowcodeRelationSchema.java` 如需补字段。

**实现要点**:
- 保存业务关系时同步 `LowcodeModelSchema.relations`。
- 从模型关系打开业务对象设计器时能自动生成应用中心关系配置草稿。
- 关系配置支持目标对象、当前对象字段、目标字段、回显字段、详情页签、默认筛选。
- 冲突检测返回“模型有、业务关系没有”“业务关系有、模型没有”“字段不一致”等差异。

**验收标准**:
- 客户对象配置“客户有多个跟进记录”后，模型关系和 `ai_business_object_relation` 同步。
- 关系运行入口不会错误跳回客户管理。
- 查询 SQL 写 Mapper XML。

### Task 9: 自定义操作与权限流程摘要

**目标**: 把对象操作、权限摘要、流程摘要纳入设计器，但首期不新增复杂规则引擎。

**涉及文件**:
- 新增 `service/businessapp/BusinessObjectActionService.java`。
- 新增 `vo/businessapp/BusinessObjectActionVO.java`。
- 新增 `dto/businessapp/BusinessObjectActionDTO.java`。
- 修改 `BusinessObjectDesignerController.java`。
- 复用 `BusinessBindingService.java`、`BusinessApprovalRuntimeService.java`、现有权限能力。

**接口签名**:
```java
@GetMapping("/{objectId}/actions")
public RespInfo<List<BusinessObjectActionVO>> listActions(@PathVariable Long objectId) { }

@GetMapping("/{objectId}/permission-summary")
public RespInfo<BusinessReadinessItemVO> permissionSummary(@PathVariable Long objectId) { }
```

**实现要点**:
- 操作配置先保存在 `designer_options` 或页面 Schema actions 中，不新增复杂动作表。
- 操作类型先支持打开页面、发起审批、执行触发器、打开外部链接。
- 权限摘要优先复用已有权限和能力挂接状态。
- 流程摘要优先复用审批绑定状态。

**验收标准**:
- 设计器能展示对象有哪些工具栏操作和行操作。
- 能展示对象权限/审批是否已配置。
- 普通用户不直接填写接口 JSON。

### Task 10: 发布检查与业务对象发布门面

**目标**: 从业务对象设计器发起发布检查和发布，内部复用现有低代码发布链路。

**涉及文件**:
- 新增 `service/businessapp/BusinessObjectPublishService.java`。
- 修改 `controller/BusinessObjectDesignerController.java`。
- 修改 `service/lowcode/LowcodePublishService.java` 如需增加对象来源参数。
- 修改 `service/lowcode/LowcodeSchemaValidator.java`。
- 修改 `domain/entity/AiBusinessObject.java`。

**接口签名**:
```java
@GetMapping("/{objectId}/publish-check")
@SaCheckPermission("ai:businessObject:publish")
public RespInfo<BusinessPublishCheckVO> publishCheck(@PathVariable Long objectId) { }

@PostMapping("/{objectId}/publish")
@SaCheckPermission("ai:businessObject:publish")
public RespInfo<Long> publish(@PathVariable Long objectId,
                              @RequestBody BusinessObjectPublishDTO dto) { }
```

**检查规则**:
- 字段无空名称、无重复 field、无空 label。
- 表单、列表、详情不引用不存在字段。
- 关系目标对象存在，关系字段存在。
- 发布前能生成运行配置。
- 数据表不存在时，根据权限决定提示同步表结构或阻断。

**验收标准**:
- 发布检查能区分 `PASS/WARN/BLOCK`。
- 阻断项有修复入口。
- 发布成功后更新 `ai_crud_config`、`ai_business_object.config_key`、`last_publish_version`、`last_publish_time`。
- 发布成功后业务应用入口可打开。

### Task 11: 业务对象设计版本和回滚

**目标**: 业务对象设计器保存发布版本快照，支持对象维度查看版本和回滚。

**涉及文件**:
- `BusinessObjectDesignVersionService.java`。
- `BusinessObjectDesignVersionMapper.java`。
- `BusinessObjectDesignVersionMapper.xml`。
- `BusinessObjectDesignerController.java`。
- `BusinessObjectDesignVersionVO.java`。

**接口签名**:
```java
@GetMapping("/{objectId}/versions")
public RespInfo<List<BusinessObjectDesignVersionVO>> versions(@PathVariable Long objectId) { }

@PostMapping("/{objectId}/versions/{versionId}/rollback")
public RespInfo<Void> rollback(@PathVariable Long objectId,
                               @PathVariable Long versionId) { }
```

**实现要点**:
- 发布成功时记录模型、页面、关系快照。
- 快照关联 `ai_crud_config_version.id`。
- 回滚时恢复模型 Schema、页面 Schema、关系 Schema，并调用现有发布回滚或重新发布。

**验收标准**:
- 发布后能看到业务对象设计版本。
- 回滚后设计器能恢复历史字段和布局。
- 回滚不破坏现有 `ai_crud_config_version`。

---

## Phase 3：前端设计器

### Task 12: 前端 API 与路由接入

**目标**: 前端接入业务对象设计器相关接口和路由。

**涉及文件**:
- `forge-admin-ui/src/api/business-app.js` — 增加设计器、字段、布局、发布检查、版本 API。
- `forge-admin-ui/src/router/index.js` — 增加 `/app-center/object/:objectCode/designer` 静态路由或确认动态路由配置。
- 新增 `forge-admin-ui/src/views/app-center/object-designer.[objectCode].vue`。

**API 方法**:
- `businessObjectDesigner(objectId)`。
- `saveBusinessObjectDesigner(objectId, data)`。
- `businessObjectFields(objectId)`。
- `createBusinessObjectField(objectId, data)`。
- `updateBusinessObjectField(objectId, fieldCode, data)`。
- `deleteBusinessObjectField(objectId, fieldCode)`。
- `saveBusinessObjectFormLayout(objectId, data)`。
- `saveBusinessObjectListLayout(objectId, data)`。
- `saveBusinessObjectDetailLayout(objectId, data)`。
- `businessObjectPublishCheck(objectId)`。
- `publishBusinessObject(objectId, data)`.

**验收标准**:
- 路由可进入设计器页面。
- API 命名与现有 `business-app.js` 风格一致。
- 不引入新的请求工具。

### Task 13: 业务对象设计器外壳

**目标**: 实现左侧导航、顶部工具栏、主体切换和未保存提示。

**涉及文件**:
- 新增 `forge-admin-ui/src/views/app-center/components/designer/BusinessObjectDesignerShell.vue`。
- 修改 `object-designer.[objectCode].vue`。
- 新增或复用 `forge-admin-ui/src/views/app-center/shared-center.css` 样式。

**设计要求**:
- 左侧导航：基本信息、字段管理、表单设计、列表设计、详情设计、关系配置、自定义操作、权限流程、发布检查、高级配置。
- 顶部显示对象名称、套件、设计状态、发布状态、保存时间。
- 顶部按钮：保存、预览、发布、更多。
- 主体区域不使用页面级卡片嵌套卡片。
- 普通模式不展示表名、DDL、Schema、configKey。

**验收标准**:
- 打开 CRM 客户对象设计器，能看到业务对象设计器骨架。
- 左侧切换不丢失本地草稿。
- 离开未保存页面时有提示。

### Task 14: 业务字段管理组件

**目标**: 实现普通用户友好的字段管理，普通属性优先，高级属性折叠。

**涉及文件**:
- 新增 `BusinessFieldManager.vue`。
- 新增 `BusinessFieldList.vue`。
- 新增 `BusinessFieldPropertyPanel.vue`。
- 复用 `FieldTypeSelect.vue`、`DictTypeSelect.vue`、`RegionTreeSelect.vue`。
- 视情况改造 `ModelFieldTable.vue` 的可复用逻辑。

**交互要求**:
- 新增字段只填字段名称和字段类型即可。
- 字段列表展示字段名称、字段类型、表单/列表/查询/导入导出开关、状态。
- 高级属性折叠显示字段编码、列名、长度、小数位、索引、加密、脱敏。
- 系统字段展示但不可删除。
- 删除字段弹出引用风险提示。

**验收标准**:
- 新增“客户等级”字段后字段列表立即出现。
- 字段编码和列名自动生成，不需要普通用户填写。
- 高级属性只有具备高级权限或开启开发者模式时展示。

### Task 15: 表单设计组件业务化包装

**目标**: 实现类截图的表单设计体验，复用现有画布能力。

**涉及文件**:
- 新增 `BusinessFormDesigner.vue`。
- 复用/改造 `CanvasFormDesigner.vue`。
- 复用/改造 `ComponentPalette.vue`。
- 复用/改造 `ComponentPropertyPanel.vue`。

**交互要求**:
- 中间为表单画布，支持分组和字段拖拽。
- 右侧为字段列表和添加字段入口。
- 字段状态区分未使用、已使用、系统字段。
- 支持基础信息、营销数据、联系人信息等分组。
- 保存时调用表单布局接口。

**验收标准**:
- 可以把“客户等级”拖入“基本信息”分组。
- 保存后刷新页面布局仍保留。
- 表单画布不显示 `undefined` 字段。

### Task 16: 列表设计组件业务化包装

**目标**: 实现查询条件、表格列、工具栏和行操作配置。

**涉及文件**:
- 新增 `BusinessListDesigner.vue`。
- 复用 `StructuredListPageDesigner.vue`。
- 复用 `ListPageGridDesigner.vue`。

**交互要求**:
- 支持查询条件配置：字段、控件、默认值、排序、是否折叠。
- 支持表格列配置：字段、列名、宽度、固定列、排序、格式。
- 支持工具栏按钮：新增、导入、导出、自定义查询、批量删除。
- 支持行操作：查看、编辑、删除、自定义操作。

**验收标准**:
- 可以把“客户等级”设为查询条件和表格列。
- 保存后预览运行配置包含对应 searchSchema 和 columnsSchema。
- 不出现空列标题。

### Task 17: 详情设计组件

**目标**: 实现详情分组、详情页签和关联列表配置。

**涉及文件**:
- 新增 `BusinessDetailDesigner.vue`。
- 复用 `ObjectRelationPanel.vue` 或抽取关系入口渲染能力。
- 修改 `LowcodePageBuilder.vue` 或 `page-schema.js` 支持 detail zone 时按需补齐。

**交互要求**:
- 支持详情字段分组、排序、隐藏、只读。
- 支持页签：基本信息、关联数据、操作日志、审批记录。
- 支持关联列表入口挂到详情页签。
- 支持跳转目标运行页并带筛选条件。

**验收标准**:
- 客户详情能配置联系人、商机、跟进记录关联入口。
- 保存后详情配置能进入发布检查。
- 关联目标对象缺失时发布检查给出阻断项。

### Task 18: 关系、操作、权限流程面板

**目标**: 实现关系配置、自定义操作、权限摘要和流程摘要的前端面板。

**涉及文件**:
- 新增 `BusinessRelationDesigner.vue`。
- 新增 `BusinessActionDesigner.vue`。
- 新增 `BusinessPermissionFlowPanel.vue`。
- 修改 `ObjectRelationPanel.vue` 如需抽取展示组件。

**交互要求**:
- 关系配置使用“属于/拥有多个/引用/明细”。
- 操作配置支持工具栏、行、详情三类位置。
- 权限流程面板只展示摘要和配置入口，首期不重写权限/流程底层。
- 普通模式不填写接口 JSON。

**验收标准**:
- 可以配置“客户有多个联系人”。
- 可以配置行操作“发起审批”并进入发布检查。
- 权限摘要能展示对象权限状态。

### Task 19: 发布检查、发布与版本面板

**目标**: 前端提供发布检查、发布、打开应用、版本查看和回滚入口。

**涉及文件**:
- 新增 `BusinessPublishChecklist.vue`。
- 新增 `BusinessVersionTimeline.vue` 或复用 `VersionTimeline.vue`。
- 修改 `object-designer.[objectCode].vue`。

**交互要求**:
- 发布检查按通过、警告、阻断分组。
- 每个阻断项提供修复按钮并切换到对应设计器面板。
- 发布按钮在阻断项存在时禁用。
- 发布成功后显示“打开应用”。
- 版本列表支持查看和回滚。

**验收标准**:
- 字段缺失 label 时发布检查阻断。
- 发布成功后能打开客户管理运行页。
- 版本列表能看到本次发布记录。

### Task 20: 高级配置与开发者模式

**目标**: 把表名、Schema、DDL、configKey、代码生成入口收进高级配置。

**涉及文件**:
- 新增 `BusinessAdvancedConfig.vue`。
- 修改 `lowcode-models.vue` 文案或入口可见性。
- 修改 `crud-config.vue` 菜单入口或说明文案。
- 修改相关权限判断逻辑。

**交互要求**:
- 高级配置需要 `ai:businessObject:advanced` 权限。
- 展示模型编码、表名、字段编码、数据库列名、Schema 预览、DDL 预览、configKey、API 配置。
- JSON 导入导出、数据库表导入、在线建表、代码预览、ZIP 下载放在高级入口。
- 高级变更保存后回流对象设计器。

**验收标准**:
- 无高级权限用户看不到表名、DDL、Schema、configKey。
- 开发者用户仍可查看和使用高级能力。
- 高级配置不形成第二套独立配置。

---

## Phase 4：入口与菜单

### Task 21: 应用中心对象详情入口重构

**目标**: 把对象详情页的“配置模型/配置布局/发布应用”收敛为“设计对象/发布对象/运行应用”。

**涉及文件**:
- `forge-admin-ui/src/views/app-center/object.[objectCode].vue`。
- `forge-admin-ui/src/views/app-center/components/ReadinessPanel.vue`。
- `forge-admin-ui/src/views/app-center/components/ObjectCard.vue`。

**实现要点**:
- 主按钮改为“设计对象”。
- 次按钮保留“打开应用”“查看就绪度”“高级配置”。
- 缺少模型时，点击设计对象进入空草稿，而不是跳到 `/ai/lowcode-models`。
- 缺少发布时，引导到设计器发布检查。

**验收标准**:
- CRM 客户对象从详情页进入业务对象设计器。
- 普通用户不再被直接带到数据模型管理。
- 运行应用仍然可达。

### Task 22: 创建向导、模板、AI、数据库导入跳转收敛

**目标**: 所有创建方式最终进入业务对象设计器，而不是低代码模型管理。

**涉及文件**:
- `BusinessObjectWizardDrawer.vue`。
- `suite.[suiteCode].vue`。
- `index.vue`。
- `lowcode-models.vue`。
- `lowcode-builder.vue`。

**实现要点**:
- 空白创建：创建业务对象草稿后跳转设计器。
- 模板创建：生成对象、字段、布局草稿后跳转设计器。
- AI 生成：生成业务对象草稿后跳转设计器。
- 数据库导入：导入模型后跳转设计器，并使用业务字段语言继续编辑。

**验收标准**:
- 四种入口都能进入同一个业务对象设计器。
- 不出现创建后散落到低代码模型、低代码应用、CRUD 配置不同页面的情况。

### Task 23: 菜单权限与普通用户信息隔离

**目标**: 调整菜单和权限，让普通业务用户看到业务对象搭建器，开发者看到高级低代码资产。

**涉及文件**:
- `forge/db/migration/V1.0.40__add_business_object_designer.sql` 或新增后续菜单脚本。
- 路由和菜单相关前端文件。
- 相关权限校验注解。

**实现要点**:
- 新增设计器菜单/隐藏路由资源。
- 新增按钮权限：设计、字段、布局、关系、操作、发布、高级配置。
- 低代码模型管理、CRUD 配置、代码生成菜单迁移到开发者目录或提高可见门槛。
- 保留旧路由，避免破坏历史链接。

**验收标准**:
- 普通用户能进入应用中心和对象设计器。
- 普通用户看不到高级模型管理菜单。
- 开发者用户能进入高级配置和原低代码模型管理。

---

## Phase 5：质量与验收

### Task 24: 后端编译与接口验证

**目标**: 验证后端代码可编译，核心接口可调用。

**验证命令**:
```bash
mvn -pl forge-admin-server -am compile -DskipTests
```

**接口验证**:
- `GET /ai/business/object/{objectId}/designer`
- `POST /ai/business/object/{objectId}/fields`
- `PUT /ai/business/object/{objectId}/layout/form`
- `GET /ai/business/object/{objectId}/publish-check`
- `POST /ai/business/object/{objectId}/publish`

**验收标准**:
- 后端编译通过。
- 接口返回统一 `RespInfo`。
- 无复杂查询写在 Service wrapper 中。

**验证结果**:
- 2026-05-29 使用 JDK 17 执行 `mvn -pl forge-admin-server -am compile -DskipTests`，结果 `BUILD SUCCESS`。
- 通过 Playwright 复用前端加密链路验证 `GET /ai/business/object/{objectId}/designer`、`GET /publish-check`、`POST /publish` 和运行入口信息接口，CRM 客户对象发布检查 `PASS`，发布成功。

### Task 25: 前端构建与页面验证

**目标**: 验证前端构建通过，设计器页面可打开和基础交互可用。

**验证命令**:
```bash
source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm --dir forge-admin-ui build
```

**页面验证**:
- `/app-center`
- `/app-center/suite/CRM`
- `/app-center/object/CUSTOMER`
- `/app-center/object/CUSTOMER/designer`
- `/ai/lowcode-models` 开发者入口

**验收标准**:
- 前端构建通过。
- 设计器页面不白屏。
- 设计器普通模式不展示表名、DDL、Schema、configKey。
- 字段、表单、列表、详情切换无布局错乱。

**验证结果**:
- 2026-05-29 使用 Node v20.19.0 执行 `NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`，结果构建成功。
- 默认 Node 堆内存构建在 chunk 渲染阶段可能 OOM，增大堆内存后通过；构建期间仍存在项目既有 UnoCSS 图标缺失、CSS `//` 注释和大 chunk 警告。
- Playwright 打开 `/app-center/object/CUSTOMER/designer?objectId=1910000000000000101&panel=fields`，字段列表 18 条，属性面板正常渲染，无 `fieldName/null`、页面错误或控制台错误。

### Task 26: CRM 客户对象端到端验收

**目标**: 按业务用户路径验证字段驱动业务对象搭建闭环。

**验收步骤**:
- 从“应用中心 → CRM → 客户 → 设计对象”进入设计器。
- 新增字段“客户等级”，类型为下拉或字典。
- 将“客户等级”拖入表单“基本信息”分组。
- 将“客户等级”配置为列表查询条件和表格列。
- 保存设计器草稿。
- 执行发布检查。
- 发布客户对象。
- 打开客户管理运行页。

**验收标准**:
- 全流程不要求用户理解数据模型、表名、DDL、Schema、configKey。
- 运行页搜索区、表格、表单不出现 `undefined`、空 label、空字段名。
- 新增字段能正常展示和保存。
- 发布成功后业务应用入口可打开。

**验证结果**:
- CRM 客户对象已有“客户等级”业务字段，设计器字段面板显示 `customerLevel`，普通模式主链路没有暴露表名、DDL、Schema、configKey。
- 发布检查结果 `overallStatus=PASS`、`blockCount=0`、`warnCount=0`。
- 发布客户对象成功，生成设计发布版本 `2060296120277417985`。
- 打开运行页 `/ai/crud-page/crm_customer` 成功，搜索区和表格列包含客户等级，页面文本未出现 `undefined`。

### Task 27: Spec、任务和执行日志回填

**目标**: 回填文档，便于后续 `/review` 和归档。

**涉及文件**:
- `code-copilot/changes/lowcode-business-object-designer/spec.md`。
- `code-copilot/changes/lowcode-business-object-designer/tasks.md`。
- 如有测试清单，新增 `test-spec.md`。

**回填内容**:
- 更新执行日志。
- 标记实际改动文件。
- 记录验证命令和结果。
- 回填待澄清项确认结论。
- 记录未完成范围和后续建议。

**验收标准**:
- `tasks.md` 状态与实际完成情况一致。
- `spec.md` 执行日志完整。
- 可以进入 `/review lowcode-business-object-designer`。

**回填结果**:
- 已记录 `BusinessFieldPropertyPanel.vue` 空字段防护、长 ID 字符串传递、CRM 样板关系字段方向修复和 Task 24-26 验证结果。
- 当前变更可进入 `/review lowcode-business-object-designer`。

---

## Phase 6：表单优先易用性增量修正

### Task 28: 字段和对象编码自动推理

**目标**: 用户输入中文字段名或对象名时，系统自动生成可维护的英文编码和数据库列/表模型编码，避免 `field_xxx` 这类随机字段污染表结构。

**实现要点**:
- 新增前端 `namingUtils.js`，按常见业务中文词推理 lowerCamel 字段编码和 lower_snake 对象/模型编码。
- 新增后端 `BusinessNamingService`，保证手动字段创建、对象创建和后端兜底逻辑一致。
- 新增字段弹窗根据“字段名称”自动填充“字段英文名”，用户手动修改后不再覆盖。
- 新建业务对象根据“对象名称”自动填充 lower_snake 对象编码，并创建 `suite_object` 风格的 `modelCode` 用于运行态表名。
- 后端对象更新保留既有对象编码和模型编码，避免历史 CRM 等大写对象编码被更新操作意外改写。

**状态**: 已完成。

### Task 29: fcDesigner 字典配置入口和表单配置优先

**目标**: 表单设计器中下拉、单选、多选、字典选择组件都能直接选择系统字典类型，并在保存时以表单设计配置覆盖字段资产。

**实现要点**:
- fcDesigner 属性面板为 `select`、`radio`、`checkbox` 和业务字典组件追加“系统字典”配置项，选项来自 `/system/dict/type/list`，支持直接输入新字典类型。
- 表单预览时根据组件 `props.dictType` 拉取真实字典项，替换静态占位选项。
- form-create 转 Forge Schema 时保留原生 `select/radio/checkbox` 组件类型，不再把带 `dictType` 的普通选择器强制改成 `dictSelect`。
- 保存表单时，字段资产的 `fieldType/dataType/componentType/queryType/dictType/reference*` 以当前表单组件为准同步。
- 后端保存草稿时再次按 FormDesignerSchema 归一化字段，防止绕过前端时字段资产和表单配置不一致。

**状态**: 已完成。

**验证结果**:
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm --dir forge-admin-ui exec eslint ...` 目标文件 lint 通过。
- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-admin-server -am compile -DskipTests` 通过。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build` 通过；仍有项目既有 UnoCSS 图标缺失、CSS `//` 注释和大 chunk 警告。
