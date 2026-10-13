# 踩坑：流程 / Flowable / BPMN

> 从 `code-copilot/memory/pitfalls.md` 按主题拆出。新条目追加到本文件。共 57 条。

## 审批/主从子表下拉人员部门开关失效与窄列

**发现日期**：2026-09-25

主表引用子表后，新增行或审批待办里：下拉无数据、人员不回显、组织树空、开关无效、列宽过窄、字段联动不生效；低代码主表单正常。

根因：
1. `ChildTableEditor.toRuntimeCellField` 错误地对整份 field.props 套 `resolveControlProps`，把 `optionSource` / `cascade` / `fieldMappings` / `labelValueField` / 引用配置剥掉，再交给 `AiFormItem`，远程选项与联动全部失效。
2. 子表行过滤 `filterVisibleRecordChildren` 只留业务列编码，丢掉伴随列 `xxxName`，人员/部门/引用无法回显。
3. `sanitizeFieldBasicProps` 未透传 `checkedValue/uncheckedValue/runtimeRules`，子表开关默认 true/false 对不上库里的 0/1。
4. 选择类列默认最小宽 120，看起来比表单控件窄一截。
5. 子表单元格给 `.n-input` 强制 `min-width:160px`，人员选择器输入框把右侧清空按钮挤出单元格被邻列盖住；且 Naive `n-input` 在 `readonly` 时根本不渲染 clearable，不能把清空指望在输入框内置 ✕ 上。
6. 子表 MD 列上的 `sourceField`（等于自身 field）被 `resolveCascadeConfig` 误当成级联父字段，`emptyStrategy=empty` 把下拉选项滤成「无数据」。
7. `useRuntimeCell` 只认精确 `userSelect`，`forgeUserSelect` / `userPicker` 等别名会落到普通输入框。

处理：运行态 cell 保留完整 props；过滤保留 `*Name`；开关默认 1/0；人员/组织列加宽且输入框 `min-width:0`；人员清空用独立 ✕ 按钮；缺名时按 id 调 `getById` 补回显；`isUserSelectLikeField` 路由进 AiFormItem 并规范化 type；`sourceField===自身` 不启用级联；表单 governance.fieldEvents 挂到 childrenConfig。

## 明细页下拉正常但子表引用后失效：optionSource 没回写字段注册表

**发现日期**：2026-09-30

明细对象页面表单里配置的 `select` + `optionSource` 在本页渲染正常；其它页面把该对象当 `subTable` 时下拉空/不好使。不是组件类型本身坏了，而是两条协议不一致：明细页读 `formDesignerSchema.components[].props`，子表 `childrenConfig` 读模型字段 `basicProps`。`buildAutoFieldAssets` 以前只给**新建**字段从组件拷贝 props，已存在字段改成下拉后 `optionSource` 只留在表单组件上；字段重建的 `PRESERVED_BASIC_PROP_KEYS` 也漏了 `optionSource`，保存时还会把已有选项源冲掉。`ChildTableEditor` 对无 `optionSource` 的 `select` 还会落到空 `options` 的原生 `n-select`。

处理：已存在字段也从表单组件镜像 `optionSource`/控件类型；保留键补上 `optionSource` 等；子表 `select` 一律走 `AiFormItem`。修完后需重新保存明细页面，父页 designPreview 或发布刷新 `modelRefs`。

## 审批子表只读列能改却保存报「不允许编辑子表字段」

**发现日期**：2026-09-25

节点把子表 `fieldInput` 配成只读，但审批页仍可编辑，暂存时报「当前节点不允许编辑子表字段: fieldInput」。另有偶发：子表列时有时无、后端有值不回显。

根因：
1. `applyChildTableFieldPermissions` 在字段权限未命中时，用子表 `allowUpdate===true` 把**所有列** `enableChildField`；只要同表另有可写列，只读列也会被放开，保存 payload 带上后被后端按字段白名单拒绝。
2. `ChildTableEditor.normalizeInputValue` 只按精确 key 取数，且只 watch `props.value`；`childrenConfig` 晚到或 key 用 `cgou_*` / `business_object_*` 别名时，会把已有行数据归一成空表。
3. 后端 `validateTaskChildRowPayload` 对行内每个 key 强制可写，只读快照字段一并提交就会抛错；真正落库本就有 `filterTaskChildWriteData`。

处理：未命中字段权限时保留后端 `writable/readonly`，禁止 `allowUpdate` 一刀切；子表编辑器在配置键变化时重绑数据并按别名取行；服务端字段级校验改为过滤而非整行拒绝。

## 审批子表列控件类型不能只依赖发布态 masterDetailConfig

**发现日期**：2026-09-25

设计器里子表列是下拉、人员选择，审批待办里全变成输入框。根因是设计器 `subTable.props.columns` 只存 `fieldCode/fieldLabel/required`，画布实时读子表对象字段注册表显示控件；审批按设计器草稿取列，控件类型却只靠 `mergeFormDesignerChildrenWithPublished` 从发布态 `masterDetailConfig.children[].fields` 匹配补齐。子表未发布、列被发布态过滤（`childFieldCodes` / 编辑区 fieldRefs / 只读）或发布态类型过期为 `input` 时，列没有 `type`，`ChildTableEditor` 落到 `n-input`。

处理原则：子表列类型为空或弱类型（input/text）时，用子表对象 `businessFieldDesignService.listFields` 补 `type/componentType/dictType/basicProps`，与设计器同源；发布态已有强类型保持不变。

子表顺序：发布态 `masterDetailConfig.children` 顺序取自 `pageSchema.modelRefs`，而后端每次保存/发布都在 `syncInlineEditRelationsToPageSchema` 按关系表 `sort_order, id`（约等于创建顺序）重建 `modelRefs`，前端按画布重排会被冲掉。必须在后端重建时按 `formDesignerSchema` 的 `subTable` 组件顺序排序（`sortModelRefsByFormSubTables`）。

子表列缺失：`ChildTableEditor.isInternalIdField` 把所有 `xxxId` 字段当内部主外键隐藏，人员/部门/引用列通常就叫 `userId/deptId/customerId`，选了「全部字段」也看不到。选择/引用类控件不能按命名当内部 ID。发布快照 `toPageModelField` 还漏了 `basicProps`（选项源）和 `referenceObjectCode`，子表下拉/引用即使类型正确也拉不到数据。

审批主表引用下拉：设计器画布用 `mergeRelationPreviewProps` 把字段注册表的引用配置合进组件，审批只拷贝组件 props，引用对象缺失；审批布局读 `settings.layout` 而设计器存在根 `layout`，列数/尺寸不一致导致控件宽度偏小。

## 审批表单字段目录不能只信发布态 editSchema

**发现日期**：2026-09-24

表单设计器新加字段后，待办渲染仍像旧表单；办理时报「请填写必填字段: fieldSwitch」。根因是 `resolveBusinessTaskCrudPageFields` 优先用已发布 `editSchema`，设计器组件进了 `uiDocument` 却不在 `fields[]`，前端 `fieldMap` 缺键静默丢组件；开关未勾选时前端又不提交 `false`，后端把「缺 key」或 `String.valueOf` 空串当成未填。

处理原则：审批主表 schema 优先读对象 `designerOptions.formDesignerSchema`（当前设计），应用页 `formAssets` 只保留 formKey/名称；子表以设计器 `subTable` 组件清单为准（顺序也按画布），发布态 `masterDetailConfig` 只补字段元数据与 `modelCode` 数据键；`filterVisibleRecordChildren` / 前端 normalize 必须按子表键别名取数，不能只做精确匹配。`resolve-ai-form` 有布局树时不得回灌未入画布字段。字段资产「未入当前表单」只表示没放进画布。

补充（同日）：子表「新增 / 选择已有」依赖 `childrenConfig.allowCreate !== false` 与 `recordSelector`。节点未配 `childPermissions` 时不能再把 `allowCreate` 强制写成 `false`，应回落到设计器/发布态（默认允许新增）；`allowSelectExisting` 与选择器配置要从 subTable props 带出，缺 `recordSelector` 时用子表 `modelCode` 合成。`childKeyCandidates` 禁止 `List.of(可能为 null)`，否则 NPE 被吞掉后审批页子表整块消失。审批字段控件类型必须以设计器 `componentKey` 为准，禁止 `mergeNonNull(editSchema)` 把下拉/人员/部门盖成 `input`。

## 嵌入式流程设计器不能由父子组件同时持有

**发现日期**：2026-09-18

低代码审批节点点击“新建并设计”时，如果节点配置子组件自己挂载 `FlowDesignPage`，同时又向业务流程工作台发出打开事件，父组件会再挂载一层设计器。关闭上层后下层仍在，表现为灰色遮罩和页面嵌套；保存/部署事件还会分别触发目录刷新，造成新模型回显与发布状态竞态。

处理原则：嵌入式流程设计器必须只有一个生命周期所有者。节点子组件只负责创建/选择模型并发出包含字符串模型 ID、Key 和表单上下文的事件；工作台统一管理弹窗。保存/部署只刷新目录，显式关闭才销毁弹窗。并发目录刷新使用请求序号拒绝过时响应；选择器只开放已部署模型，但当前刚创建的草稿要作为“待发布”选项保留，发布校验仍必须拒绝草稿。

## 门户外层 deep 样式不能覆盖嵌套加载容器


**发现日期**：2026-08-23

Vue scoped CSS 中的 `.portal-loading-host :deep(.n-spin-content)` 会命中门户下所有层级的 `NSpin`，不只命中门户最外层加载容器。若在这条规则上设置 `height/min-height: 100vh`，待办列表等内部加载容器也会被强制撑到一个视口高，再被自己的父级 `overflow: hidden` 裁掉，表现为列表有内容但无法向下滚动。

处理原则：只需要约束根加载容器时使用直接子级选择器 `.portal-loading-host > :deep(.n-spin-content)`；应用壳建立 `height: calc(100vh - header)`、`min-height: 0` 的明确高度链，实际内容区使用 `overflow-y: auto`。浏览器回归不能只看 `scrollHeight`，还要设置滚动容器的 `scrollTop` 并确认数值确实变化。

## 动态 CRUD 事件不能把运行配置对象码当作流程标准对象码


**发现日期**：2026-08-23

低代码运行配置的 `configKey`、历史 `ai_crud_config.object_code` 和 `ai_business_object.object_code` 可能不同。业务流程发布版本按标准业务对象编码保存 `subject_object_code`；新增记录事件如果直接使用 CRUD 配置对象码，保存接口虽然成功，事件也已发布，但流程查询始终匹配不到，表现为“记录新增后配置了自动发起仍不生效”。

处理原则：事件入口应以 `configKey` 从 `ai_business_object` 解析标准 `objectCode` 和 `suiteCode`，并按 `<objectCode>:<recordId>` 生成业务 Key；只为尚未迁移到业务对象表的旧 CONFIG 配置回退 `ai_crud_config`。真实回归必须同时检查运行记录的 `triggerType=EVENT`、标准 `businessKey` 和当前审批节点，不能只断言 CRUD 返回成功。

## Flowable 固定审批人不能保存为用户变量表达式


**发现日期**：2026-08-21

流程设计器把指定用户 `45` 写成 `flowable:assignee="${user_45}"` 时，Flowable 会把它解释为流程变量表达式；启动流程若没有 `user_45` 变量，就会抛出 `Unknown property used in expression`，而不是把任务分给用户 45。

处理原则：新模型将固定用户以字符串 ID 写入 `assignee`，并用 `assigneeType="custom"` 标识；前端所有选人和 XML 往返都禁止把用户 ID 转成 JavaScript `Number`。历史已部署模型只能对完整匹配 `^\$\{user_([0-9]+)}$` 的节点受控补入同名变量，不能对普通动态表达式或 SPEL 放宽注入范围。部署兼容后仍应重新打开并保存模型，使后续版本永久使用字面量 ID。

## 业务流程任务表单选项不能只读 value/code


**发现日期**：2026-08-20

`businessFlowFormAssets` 返回的资产主键是 `formKey` / `formName`，不是通用下拉的 `value` / `code`。审批节点若按 `item.value || item.code` 组装选项，列表会被滤空，用户选不到表单，默认绑定也会失败。

处理原则：任务表单选择复用 `BusinessFlowFormAssetSelect`（按 `formKey` 卡片选择）；默认绑定当前对象第一张表单，目录为空时用 `objectCode` 合成对象表单。不要再给审批节点做只认 `value` 的 `n-select`。

## try-with-resources 外的失败审计不能依赖上层租户上下文


**发现日期**：2026-08-03

开放网关使用 `try (scope = contextBridge.open(...))` 建立可信租户时，外层 `catch` 执行前 scope 已经关闭。若失败审计在外层 `catch/failure()` 中执行，成功路径有租户上下文、失败路径却没有，严格租户模式会报“访问租户表时缺少租户上下文”，并掩盖原始业务错误。

审计这类跨成功、失败、异步观察和补偿路径的基础设施服务不能隐式依赖调用方 ThreadLocal。服务方法应接收已验证的显式 `tenantId`，在数据库操作的最小边界内：

- 设置可信租户并强制 `ignoreTenant=false`；
- 对明确不使用登录用户行级权限的审计 Mapper 受控跳过 DataScope；
- 在 `finally` 中恢复原租户、租户忽略值和 DataScope 标记；
- 保留 SQL 中显式 tenant/可信身份条件，禁止把审计表加入全局租户忽略名单。

---

## 3. BPMN XML属性值带前导空格导致匹配失败


**发现日期**: 2026-05-05

**问题描述**:
FlowBusinessForm组件加载外部表单时，formUrl匹配失败。原因是BPMN XML中的属性值带了前导空格。

**错误现象**:
```
formUrl: ' /leave/LeaveApproveForm'   ← 前面有空格
expectedKey: '/src/views /leave/LeaveApproveForm.vue'  ← 中间有空格，无法匹配
```

实际组件路径：
```
'/src/views/leave/LeaveApproveForm.vue'  ← 没有空格
```

**根本原因**:
Flowable BPMN XML解析时，属性值可能包含前导或尾部空格。例如：
```xml
<userTask flowable:formUrl=" /leave/LeaveApproveForm">
```

**解决方案**:
在使用formUrl前，必须trim()去掉前后空格：
```javascript
const cleanUrl = formUrl.split('?')[0].trim()  // ← 添加trim()
```

**影响范围**:
- FlowBusinessForm组件的外部表单加载
- 所有从BPMN XML读取的属性值（formUrl、formKey等）

---

## 4. SPEL表达式执行日志缺失导致排查困难


**发现日期**: 2026-05-05

**问题描述**:
审批人SPEL表达式没有匹配到人，但没有任何日志输出，无法排查问题原因。

**根本原因**:
- FlowNodeConfigServiceImpl.evaluateExpression() 只有错误日志，缺少执行前后的info日志
- FlowTaskEventListener 任务创建时assignee为null，没有警告日志
- FlowInstanceServiceImpl 流程定义不存在时，没有错误日志

**解决方案**:
在关键节点添加详细日志：

1. **FlowNodeConfigServiceImpl.evaluateExpression()**
```java
log.info("[审批人表达式] 开始执行: expression={}, variables={}", expression, variables);
log.info("[审批人表达式] 执行结果: result={}, resultType={}", result, ...);
log.warn("[审批人表达式] 表达式返回null，未匹配到审批人: expression={}", expression);
```

2. **FlowTaskEventListener.handleTaskCreated()**
```java
if (task.getAssignee() == null) {
    log.warn("[审批人分配失败] 任务创建时没有审批人: taskId={}, taskName={}");
    log.warn("[审批人分配失败] 请检查: 1)审批人配置 2)流程变量 3)SPEL表达式");
}
```

3. **FlowInstanceServiceImpl.startProcess()**
```java
if (processDefinition == null) {
    log.error("[流程启动失败] 流程定义不存在: modelKey={}", modelKey);
    throw new RuntimeException(...);
}
```

**影响范围**:
- 所有SPEL表达式审批人计算
- 流程启动、任务创建、审批分配

---

## 7. 代码应用已有业务对象时设计器误走低代码空模型


**发现日期**: 2026-06-30

**问题描述**:
采购审批这类代码应用在应用中心已经有 `ai_business_object` 占位对象，但没有低代码 `modelSchema.fields`。嵌入式对象设计器如果只按 `objectId` 调用普通 `businessObjectDesigner(object.id)`，左侧“表单设计 / 列表设计 / 详情设置”会加载到空低代码模型，看起来像字段没有导入。

**根本原因**:
嵌入式打开设计器时 URL 通常没有 `codeApp=1`，原逻辑又因为已经拿到 `objectId`，直接跳过代码应用虚拟设计器路径。代码应用字段真源应来自 `BusinessCodeFormProvider`、`businessFlowAppConfig.formAssets` 或 `providerCatalog`，不能从空低代码模型读取。

**解决方案**:
对象设计器解析到业务对象后必须检查 `options/designerOptions.codeApp=true`。命中代码应用时，强制走 `businessFlowAppConfig(objectCode)`，用 Provider/formAssets/providerCatalog 字段构造既有设计器需要的 `modelSchema/pageSchema/formDesignerSchema/viewSchema`。

同时，已有 `ai_business_object` 的代码应用保存 `options.codeAppMetadata` 时，后台 `BusinessFlowAppConfigService#saveConfig` 也必须写入流程绑定 options，不能只在“对象不存在”的代码应用分支保存。

---

## 7. 示例流程初始化覆盖用户 BPMN 节点配置


**发现日期**: 2026-06-30

**问题描述**:
采购审批示例在发起流程前会调用 `ensureFlowModel()` 初始化流程模型。旧逻辑只要发现数据库中的 BPMN XML 与代码里的 `SamplePurchaseOrderFlowBpmn.build()` 不一致，就调用 `updateModel` 覆盖模型并重新发布，导致用户在流程设计器节点抽屉中配置的 `formFieldPermissions` 被重置。

**正确做法**:
示例/seed 初始化只能在模型不存在或 BPMN XML 缺失时写入默认 XML。模型已存在且 XML 非空时，必须保留用户在流程设计器中保存的 BPMN；如模型未发布，可以发布当前已有模型，但不能用代码里的默认 BPMN 覆盖。

**影响范围**:
- 采购审批等内置示例流程。
- 所有把流程设计器作为配置主数据源、同时又有代码 seed/init 的流程模型。

---

## 7. form-create 设计器默认锁定字段 ID


**发现日期**: 2026-05-26

**问题描述**:
`@form-create/designer` 默认会把右侧基础配置里的“字段”（即 rule.field）设为只读，业务用户无法把动态表单字段 ID 改成业务模型字段名，导致审批表单变量无法稳定映射到业务表。

**根本原因**:
`FieldInput.vue` 会读取 `designer.setupState.fieldReadonly`；`FcDesigner.vue` 中默认逻辑是 `config.fieldReadonly !== false`，也就是未显式配置时字段只读。

**解决方案**:
所有面向业务配置的 form-create 设计器封装必须显式传入：

```js
const designerConfig = {
  fieldReadonly: false,
}
```

**影响范围**:
- 流程模型动态表单设计器
- 流程表单管理设计器
- 节点专属动态表单在线设计
- 低代码页面 form-create 适配器

**影响范围**:
- `forge-report-ui` 项目保存、发布、详情回显
- 所有启用前端加密拦截的后端接口

## 5. Flowable 管理员转派后待办消失


**发现日期**: 2026-05-06

**问题描述**:
管理员转派任务后，`sys_flow_task`（或误写为 `flow_stak`）中任务 `status` 被更新为 `1`，新处理人在“我的待办”列表查询不到。

**根本原因**:
直接调用 `taskService.setAssignee(taskId, newAssignee)` 会触发 `TASK_ASSIGNED` 监听器；如果 Flowable 任务没有设置 `owner`，监听器会把这次分配误判为“签收”，将本地任务状态改成 `1`。

**解决方案**:
转派前先设置 `owner` 为原处理人或管理员用户，再设置新的 `assignee`；本地 `sys_flow_task` 同步更新 `assignee`、`owner`，并保持 `status=0`。待办查询也应包含“当前用户已签收但未完成”的 `status=1` 任务。

**影响范围**:
- 流程监控中的管理员转派
- 我的待办列表查询
- 所有依赖 `TASK_ASSIGNED` 事件同步本地任务表的场景

---

## 7. Flowable 委派态任务不能直接完成


**发现日期**: 2026-05-06

**问题描述**:
调用 `/api/flow/task/reject` 驳回被 `taskService.delegateTask` 处理过的任务时，Flowable 抛出 `A delegated Task ... cannot be completed, but should be resolved instead.`。

**根本原因**:
`delegateTask` 会把任务置为 `DelegationState.PENDING`。Flowable 禁止对 `PENDING` 委派态任务直接调用 `taskService.complete`，必须先 `taskService.resolveTask(taskId)`。

**解决方案**:
审批通过/驳回统一走封装方法：如果 `task.getDelegationState() == DelegationState.PENDING`，先 `resolveTask` 再 `complete`。业务“转办”不要使用 Flowable 委派语义，应设置 `owner` 后用 `setAssignee` 转派，并同步本地任务表保持 `status=0`。

**影响范围**:
- 转办后再审批通过/驳回的流程任务
- 所有直接调用 `taskService.complete` 的 Flowable 任务操作

## 7. BPMN 设计器 XML 回传会清空撤销栈


**发现日期**: 2026-05-08

**问题描述**:
流程设计器中修改节点后，撤销按钮仍然不可点击。

**根本原因**:
`FlowModeler` 通过 `commandStack.changed` 触发 `emit('change', xml)` 后，父组件更新 `bpmnXml` 又作为 `props.xml` 回传给设计器；如果 watcher 不区分这是自身刚发出的 XML，会再次 `importXML`，而 bpmn-js 重新导入会清空 `commandStack`。

**解决方案**:
设计器发出 XML 前记录规范化后的 `lastEmittedXml`；`props.xml` watcher 收到相同 XML 时跳过导入。属性面板更新时不要再让父组件主动 `getXML -> bpmnXml 回传`，只标记页面有变更，XML 同步交给设计器的 `commandStack.changed`。

**影响范围**:
- `FlowModeler` 撤销/重做状态
- 所有父子组件通过 `:xml` + `@change` 双向同步 BPMN XML 的场景

## 8. AI 生成 BPMN 的 BPMNPlane 指向错误导致画布导入失败


**发现日期**: 2026-05-10

**问题描述**:
AI 生成流程配置后点击“加载到画布”，bpmn-js 报错：
```
导入 BPMN 失败: Error: no process or collaboration to display
加载AI流程配置失败: Error: no process or collaboration to display
```

**根本原因**:
前端原逻辑只检查 XML 是否包含 `BPMNDiagram`，没有校验 `BPMNPlane` 的 `bpmnElement` 是否指向真实存在的 `process` 或 `collaboration`。AI 返回的 XML 即使有图形坐标，只要平面引用了不存在的流程根元素，bpmn-js 就无法展示。

**解决方案**:
导入 AI 草稿前必须归一化 BPMN XML：
- 提取完整 `definitions`，兼容无 XML 声明或非 `bpmn:` 前缀。
- 用 DOM 同步 `process id`、`participant processRef`、`BPMNPlane bpmnElement`。
- 校验存在可展示的 `process/collaboration` 和 `BPMNPlane`。
- 当 BPMNDI 缺失或平面指向错误时，移除旧 BPMNDI 并根据语义节点重建坐标。
- 模型 Key 不能直接使用纯数字（如 `11212`）作为 BPMN `process id`；导入前应自动规范成 `process_11212` 这类合法 id。

**影响范围**:
- `forge-admin-ui/src/views/flow/design.vue` 的 AI 流程生成/加载画布。
- 所有依赖 AI 返回 BPMN XML 并直接导入 bpmn-js 的场景。

## 42. Flowable 节点表达式变量必须由低代码映射提供


**发现日期**: 2026-06-02

**问题描述**:
低代码单据发起 Flowable 流程时，只部署流程定义不够。BPMN 中用户任务 assignee/candidate 表达式引用的变量也必须从业务单据映射到流程变量，否则流程可以启动但任务创建时无法正确分配处理人，或启动阶段因表达式缺变量失败。

**解决方案**:
seed 流程绑定和触发器 `START_FLOW` 动作时，需要对照已部署 BPMN 的 JUEL 表达式补齐变量映射。例如 `leave_multi` 需要提供 `deptManager`，CRM 商机样板用 `createBy -> deptManager` 作为默认部门经理变量映射。

**影响范围**:
- `ai_business_binding` 中 `binding_type=FLOW` 的变量映射
- `ai_business_trigger` 中 `START_FLOW` 动作配置
- 所有使用 Flowable 表达式分配审批人的低代码单据流程

## 44. sys_flow_task.assignee 必须存用户 ID


**发现日期**: 2026-06-04

**问题描述**:
低代码动态页发起 Flowable 主流程时，BPMN assignee 表达式可能返回姓名或账号。如果监听器直接把 `task.getAssignee()` 写入 `sys_flow_task.assignee`，待办表会存成显示名，后续按用户 ID 查询待办、通知或事件消费都会不稳定。

**解决方案**:
`FlowTaskEventListener` 写入 `FlowTask.assignee/owner` 前必须归一为用户 ID：数值 ID 原样保留，非数值值通过 `FlowOrgIntegrationService.getUserList` 精确匹配 `id/username/name/realName`，只能唯一匹配时才替换为用户 ID；创建、分配、完成事件的 payload 也应使用归一后的 `flowTask.getAssignee()`。

**影响范围**:
- `sys_flow_task.assignee`、`sys_flow_task.owner`
- `TASK_CREATED`、`TASK_ASSIGNED`、`TASK_COMPLETED` 业务事件
- 所有通过低代码变量映射或 BPMN 表达式分配审批人的流程

## 45. 单据详情运行态也要归一 objectCode/configKey


**发现日期**: 2026-06-04

**问题描述**:
动态详情页读取 `/ai/business/document/{objectCode}/{recordId}/runtime` 时，前端可能传入运行配置 `configKey` 或历史 `ai_crud_config.object_code`，但单据配置和流程实例关联按标准业务对象 `objectCode` 保存。只修复发起流程链路的对象标识归一还不够，详情页会继续读不到 `ai_business_document_config.options` 中的流程时间轴/流程图开关，也可能用错误的 `businessKey` 查不到流程实例。

**解决方案**:
`BusinessDocumentRuntimeService` 必须和流程发起服务一样做运行态上下文解析：按请求值查发布运行配置、单据配置和业务对象，归一出标准业务对象 `objectCode`；用标准 `objectCode:recordId` 查询流程实例和权限动作，用单据配置 `configKey` 读取动态 CRUD 记录数据。前端也应优先传 `businessObjectCode/options.businessObjectCode/modelSchema.objectCode`，最后再回退历史 `cfg.objectCode/configKey`。

**影响范围**:
- `/ai/business/document/{objectCode}/{recordId}/runtime`
- 动态详情页“业务数据 / 流程进度”Tab
- 单据配置 `options.detailFlowTimelineVisible/detailFlowDiagramVisible`
- `ai_business_flow_instance_link.business_key`

## 46. 低代码 START_FLOW 不能同时走 custom-action 和内置发起


**发现日期**: 2026-06-04

**问题描述**:
动态页操作按钮如果先向外 `emit('custom-action')`，再执行内置 `START_FLOW`，同一次点击可能被运行态外层和 `AiCrudPage` 各发起一次流程，最终创建两个 Flowable 流程实例和两条待办。即使前端消除了双路径，后端“先查运行中实例、再启动流程、最后插入关联”的流程也存在并发竞态。

**解决方案**:
`AiCrudPage` 对内置 `START_FLOW` 必须优先拦截并直接返回，不再抛 `custom-action`。后端 `POST /ai/business/flow/start` 必须按 `tenantId + canonical businessKey` 加流程发起锁，并在锁内重新检查运行中实例；事务开启时锁要延迟到事务完成后释放，避免关联表提交前的第二个请求穿透。Redisson 锁不要设置可能早于事务完成的固定 lease，使用 watchdog 续期；本地 `ReentrantLock` 兜底释放后有等待线程时不能从锁缓存移除。

**影响范围**:
- `AiCrudPage` 自定义操作处理顺序
- `/ai/business/flow/start`
- `ai_business_flow_instance_link`
- Flowable 流程实例和 `sys_flow_task` 待办生成

## 47. Flow 服务 sys_flow_business 也必须做 businessKey 幂等


**发现日期**: 2026-06-04

**问题描述**:
低代码发起流程的 admin 侧即使做了发起锁，Flow 服务 `/api/flow/instance/start/{modelKey}` 也可能因为重试或 admin 关联表写入失败而再次收到同一 `businessKey`。如果 Flow 服务直接插入 `sys_flow_business`，会撞 `uk_flow_business_tenant_key (tenant_id, business_key)`，暴露 `DuplicateKeyException`，例如 `Duplicate entry '1-LEAVE_APPLICATION:5'`。

**解决方案**:
Flow 服务启动前必须按 `tenant_id + business_key` 查询 `sys_flow_business`。已有运行中/草稿或 Flowable runtime 仍存在的记录时，直接返回原 `processInstanceId`；已结束状态不能复用，应返回明确的不可重复发起错误。插入时显式写入 `tenantId`，同 JVM 内按业务 Key 加本地锁并延迟到事务完成后释放，跨实例并发依赖唯一键并捕获 `DuplicateKeyException` 后转换为幂等返回或“流程正在发起，请稍后重试”。

**影响范围**:
- `/api/flow/instance/start/{modelKey}`
- `sys_flow_business.uk_flow_business_tenant_key`
- `FlowBusinessMapper`
- 低代码流程发起重试和 admin 侧关联表补偿

## 48. 同一流程实例重复待办优先检查 BPMN 重复 sequenceFlow


**发现日期**: 2026-06-04

**问题描述**:
动态页发起流程后，如果 `businessKey` 和 `processInstanceId` 都相同，但生成了两条不同 `task_id` 的待办，这通常不是前端重复调用，也不是 `sys_flow_task` 监听器重复 insert。Flowable 可能是在同一个流程定义里发现了两条语义相同的出线，例如同时存在：

```xml
<bpmn:sequenceFlow id="flow1" sourceRef="startEvent" targetRef="deptApprove"/>
<bpmn:sequenceFlow id="Flow_0fnqi4c" sourceRef="startEvent" targetRef="deptApprove"/>
```

即使节点 `<outgoing>` 只引用其中一条，Flowable 仍会按 `sourceRef` 解析实际出线，两条出线会创建两条执行路径，从而在同一个流程实例下产生两个相同节点待办。

**解决方案**:
流程模型保存、导入、复制、部署和版本回退前必须规范化 BPMN XML，删除同一流程/子流程作用域内语义完全相同的重复 `sequenceFlow`。清理时优先保留被 `<incoming>/<outgoing>`、节点 `default` 或 BPMNDI `BPMNEdge` 引用的连线，并同步清理被删除连线对应的引用和图形边。

**影响范围**:
- `FlowModelServiceImpl` 保存、导入、复制、部署流程模型
- `FlowModelVersionServiceImpl` 版本回退部署
- Flowable `ACT_RU_TASK` 同一 `PROC_INST_ID_` 下重复活跃用户任务
- 低代码 AI 生成或 BPMN.js 编辑后残留旧连线的流程模型

## 60. Flowable 7 流程取消事件不能强转 FlowableEntityEvent


**发现日期**: 2026-06-17

**问题描述**:
调用 `RuntimeService.deleteProcessInstance` 清理或删除流程实例时，`FlowTaskEventListener` 处理 `PROCESS_CANCELLED` 事件报错：

```text
FlowableProcessCancelledEventImpl cannot be cast to FlowableEntityEvent
```

错误会被记录到 `sys_flow_error_log` 的 `EVENT_PROCESS_CANCELLED` 阶段，导致流程取消后的业务状态同步、表单实例状态同步和事件发布逻辑无法正常执行。

**根本原因**:
Flowable 7.0.1 的 `FlowableProcessCancelledEventImpl` 继承 `FlowableProcessEventImpl`，实现的是 `FlowableCancelledEvent`，并不是 `FlowableEntityEvent`。取消事件的 `processInstanceId` 已通过 `FlowableEngineEvent.getProcessInstanceId()` 设置到事件对象上，不能按 `((FlowableEntityEvent) event).getEntity()` 读取。

**解决方案**:
流程监听器处理 `PROCESS_CANCELLED` 时，优先从 `FlowableEngineEvent.getProcessInstanceId()` 读取流程实例 ID；必要时再兼容 `FlowableProcessEngineEvent.getExecution()` 和 `FlowableEntityEvent`。错误日志记录也应复用同一个解析逻辑，避免非实体事件丢失流程上下文。

**验证建议**:
新增测试直接构造 `FlowableProcessCancelledEventImpl`，断言它不是 `FlowableEntityEvent`，并验证监听器仍能解析出 `processInstanceId`。Maven 验证需要使用 Java 17 且带 `-am`，避免单模块构建拿到本地仓库旧版模块依赖。

## 63. 流程统一表单字段目录为空时条件分支无法选择表单字段


**发现日期**: 2026-06-20

**问题描述**:
流程模型设计里已经配置了统一动态表单，但条件分支配置中“表单字段条件”仍不可用，或提示没有动态表单字段。

**根本原因**:
条件分支依赖 `design.vue` 传入的 `formFieldCatalog`。如果已选统一表单的远端字段目录为空，或者当前模型内表单 schema 包含 form-create 的 `children` 嵌套、`_forge.fieldBinding.fieldCode` 绑定字段，前端只解析第一层 `field/title/type` 会漏字段，导致条件配置器认为字段数为 0。

**解决方案**:
前端本地字段目录解析必须与后端 `FlowFormServiceImpl.collectFields()` 保持同类规则：
- 递归遍历 schema 数组/对象和 `children`。
- 支持 `field`、`fieldCode`、`props.field/fieldCode/prop`、`fieldBinding.fieldCode`、`_forge.fieldBinding.fieldCode`。
- 过滤 form-create 自动生成的 `ref_` 临时字段。
- 远端 `/api/flow/form/field-catalog` 返回空列表时，用已加载的 `formSchema` 本地解析兜底。

## 64. 流程条件分支标签点击必须保留 edgeId


**发现日期**: 2026-06-20

**问题描述**:
流程设计器画布上点击某条条件分支标签时，右侧配置抽屉展示了该网关的所有分支，而不是用户点击的那一条分支。用户需要在多条分支配置中再次定位，容易改错条件。

**根本原因**:
`BranchHeader` 已经在点击事件中发出了当前 `edge`，但父组件如果只用 `edge.source` 找到网关节点再打开抽屉，会丢失“点击的是哪条边”的上下文。`ConditionConfig` 只能按网关出边数组渲染，自然会显示全部分支。

**解决方案**:
分支标签点击链路必须一路透传当前 `edge.id`：
- `DingFlowDesigner` 保存 `drawerFocusEdgeId`，分支标签点击时设置为当前 edgeId。
- `NodeConfigDrawer` 将 `focusEdgeId` 透传给网关配置组件。
- `ConditionConfig` 有 `focusEdgeId` 时只渲染对应分支；点击网关节点本身时清空 `focusEdgeId`，恢复全部分支配置。

**影响范围**:
- `forge-admin-ui/src/components/flow-designer/canvas/BranchHeader.vue`
- `forge-admin-ui/src/components/flow-designer/DingFlowDesigner.vue`
- `forge-admin-ui/src/components/flow-designer/panel/NodeConfigDrawer.vue`
- `forge-admin-ui/src/components/flow-designer/panel/ConditionConfig.vue`

## 65. Flowable 默认分支不能导出 conditionExpression


**发现日期**: 2026-06-20

**问题描述**:
流程模型设计器中如果把某条条件分支设置为默认分支，同时导出的 BPMN 仍给该 sequenceFlow 写入 `conditionExpression`，部署会失败：

```text
flowable-exclusive-gateway-condition-on-seq-flow:
Default sequenceflow has a condition, which is not allowed
```

**根本原因**:
Flowable 的 exclusiveGateway 默认边是兜底流转，不能再携带条件表达式。UI 为了用户切换默认分支时不丢草稿，可以保留 `edge.condition`，但 BPMN 导出时不能把这个条件写进 default 边。

**解决方案**:
默认分支状态只表示该边被标记为默认，处理规则必须分层：
- `ConditionConfig` 可以保留并编辑默认分支上的草稿条件，设置默认分支时只更新 `isDefault` / `defaultFlowId`，不清空 `edge.condition`。
- `json-to-bpmn.writeEdge()` 必须使用 `edge.condition && !edge.isDefault`，默认边永远不写 `conditionExpression`。
- 画布标签默认分支只展示“默认”，不要把草稿条件显示成会执行的条件摘要。
- 如果某个条件需要参与 Flowable 判断，就不能让这条 sequenceFlow 成为 gateway default。

**影响范围**:
- `forge-admin-ui/src/components/flow-designer/panel/ConditionConfig.vue`
- `forge-admin-ui/src/components/flow-designer/converter/json-to-bpmn.js`
- `forge-admin-ui/src/components/flow-designer/converter/branch-parser.js`
- 条件分支画布标签：`BranchHeader.vue`、`EdgePath.vue`

## 66. 条件网关不能把分支数量固定死为 2


**发现日期**: 2026-06-20

**问题描述**:
流程设计器新增条件分支后，如果 `addGatewayNode()` 只硬编码生成两条分支，用户后续无法继续配置第三条、第四条条件路径。实际业务里的排他网关通常是“多条条件分支 + 一条默认分支”，不是固定两个节点。

**根本原因**:
初始创建网关可以默认生成两条分支，但编辑态必须提供追加分支能力。只在 `for (let i = 0; i < 2; i += 1)` 里创建分支，会让分支数量变成建模能力限制，而不是初始模板。

**解决方案**:
- `useFlowDesigner` 提供独立 `addBranch(gatewayId)`，不要通过重复插入网关模拟新增分支。
- 追加分支时沿既有分支链路找到合流节点，把新分支接回同一个 merge target。
- 条件/包容网关追加分支后必须归一化默认分支，保留一个且仅一个 `isDefault/defaultFlowId`；并行网关不设置默认分支。
- 配置面板点击“添加分支”后聚焦新分支的条件配置，减少用户在多分支列表里定位的成本。

**影响范围**:
- `forge-admin-ui/src/components/flow-designer/composables/useFlowDesigner.js`
- `forge-admin-ui/src/components/flow-designer/panel/ConditionConfig.vue`
- `forge-admin-ui/src/components/flow-designer/DingFlowDesigner.vue`

## 67. 条件分支画布标签不要直接展示 SpEL 原文


**发现日期**: 2026-06-20

**问题描述**:
条件分支设置表达式后，如果画布边标签直接展示 `${amount > 1000}`、`${a && b}` 这类原始 SpEL，会让分支连线区域变得拥挤；同时如果 SVG 连线层和 HTML 分支标签都展示条件文本，一条边上会出现重复标签。

**根本原因**:
`edge.condition` 是执行表达式，不是画布展示文案。它适合放在配置抽屉里编辑，不适合作为分支概览直接铺在画布边上。

**解决方案**:
- `BranchHeader` 只展示“条件已设 / N 条条件 / 默认 / 配置条件”这类状态摘要，原始表达式可放到 `title` 或配置抽屉里查看。
- `EdgePath` 对带 `branchId` 的网关分支边不再重复渲染 SVG 文本标签，避免和 `BranchHeader` 叠加。
- 配置抽屉仍保留完整表达式预览，满足调试需要。

**影响范围**:
- `forge-admin-ui/src/components/flow-designer/canvas/BranchHeader.vue`
- `forge-admin-ui/src/components/flow-designer/canvas/EdgePath.vue`

## 68. BPMN 只保留 conditionExpression 时需要反解析表单规则


**发现日期**: 2026-06-20

**问题描述**:
用户通过“表单字段条件”生成 `${amount > 1000}` 后，如果流程经过 BPMN XML 保存/导入，边上通常只剩 `conditionExpression` 字符串，`conditionRules` 和 `conditionMode` 这些前端辅助字段不会天然存在。再次打开条件配置时，如果只看 `conditionRules`，会误进入“高级表达式”模式。

**解决方案**:
`ConditionConfig` 判断模式时应优先使用显式 `conditionMode/conditionRules`；如果缺失，但当前表单字段目录能匹配表达式字段，则对常见表达式反解析为规则行：
- `==`、`!=`、`>`、`>=`、`<`、`<=`
- 区间：`field >= start && field <= end`
- 包含/不包含
- 为空/不为空

字段不在当前表单目录，或表达式结构无法安全识别时，继续使用高级表达式模式。

**影响范围**:
- `forge-admin-ui/src/components/flow-designer/panel/ConditionConfig.vue`

## 75. 流程待办消息完成后必须自动置已读


**发现日期**: 2026-06-26

**问题描述**:
流程审批通过后，任务已经从待办流转到已办，但任务创建时推送的站内信仍保持未读，导致消息中心未读数和实际待办状态不一致。

**根本原因**:
流程任务创建事件通过消息模块发送 `bizType=FLOW_TODO`、`bizKey=taskId` 的待办站内信，但任务完成事件只更新 `flow_task` 状态，没有回调消息模块更新 `sys_message_receiver.read_flag/read_time`。

**解决方案**:
消息模块提供按业务类型和业务键标记站内信已读的能力，流程 `TASK_COMPLETED` 事件必须调用：

```java
messageService.markWebReadByBiz("FLOW_TODO", taskId);
```

对应 SQL 必须写在 `SysMessageReceiverMapper.xml` 中，通过 `sys_message.biz_type + biz_key` 定位消息并更新接收人已读状态。

**影响范围**:
- 流程待办站内信
- 消息中心未读数
- 任何新增流程通知类型时，都要同步设计消息生命周期回写逻辑

## 76. 外部审批表单按钮 loading 必须绑定父级提交状态


**发现日期**: 2026-06-26

**问题描述**:
流程节点使用外部业务表单时，表单内部“同意/驳回”按钮只在本地签名上传阶段短暂 loading，触发 `emit('submit')` 后父级真正调用审批接口，子表单 loading 已经结束，用户会误以为没有提交中状态。

**根本原因**:
Vue `emit` 不会等待父组件异步处理。外部表单如果只维护本地 `submitting`，无法覆盖父级 `approveTask/rejectTask` 请求期间。

**解决方案**:
- `FlowBusinessForm` 必须向动态业务表单透传父级 `submitting` 和 `submittingAction`。
- 业务表单按钮 loading 使用“本地提交状态 OR 父级提交状态”，并按 action 精准显示。
- 父级审批方法进入接口调用前设置当前 action，finally 中同时清理 loading 和 action。

**影响范围**:
- `FlowBusinessForm`
- `/views/*/*ApproveForm.vue` 这类外部流程审批表单
- 流程待办详情里的同意、驳回、退回、终结、签收等异步操作按钮

## 77. 流程完成事件必须携带完整变量快照


**发现日期**: 2026-06-28

**问题描述**:
Flowable 的 `TASK_COMPLETED` 和 `PROCESS_COMPLETED` 事件消费顺序不能作为业务前提。若流程完成事件只读取 `ExecutionEntity#getVariables()`，最后一个审批节点提交的业务变量可能没有出现在完成事件消息里；业务侧 `@FlowCallback` 如果依赖这些变量，可能抛错并导致业务状态停留在“审批中”。

**根本原因**:
流程完成后运行时变量可能已经不可读，业务侧 Redis Pub/Sub 订阅器回调失败默认只记录日志，不做持久化重试。完成事件必须尽量自带完整变量快照，不能依赖后续再从运行时实例读取。

**解决方案**:
- `PROCESS_COMPLETED/PROCESS_REJECTED` 发布前合并当前执行变量、运行时变量和历史变量。
- `TASK_COMPLETED` 读取任务变量失败后继续按流程实例和历史变量兜底。
- `FlowInstanceService#getProcessVariables`、监控变量查询等入口必须支持流程结束后读取历史变量。
- 业务回调仍需做幂等和缺失变量兜底；如需强一致状态流转，应后续引入持久化事件重试/死信机制。

**影响范围**:
- `FlowTaskEventListener`
- `FlowInstanceServiceImpl`
- `FlowMonitorServiceImpl`
- 所有依赖 `@FlowCallback` 或流程完成事件变量更新业务状态的模块

## 78. 前端禁止把雪花 Long ID 转成 Number


**发现日期**: 2026-06-28

**问题描述**:
采购单待办外部表单调用 `POST /business/sample-purchase-order/getById` 时返回“采购单不存在”。后端记录存在，但前端从流程变量或记录行读取采购单 ID 后用 `Number()` 转换，雪花 ID 超过 JS `Number.MAX_SAFE_INTEGER` 后发生精度丢失，最终请求传到后端的是错误 ID。

**根本原因**:
Forge 后端已通过 Jackson `BigNumberSerializer` 将超出 JS 安全范围的 `Long` 序列化为字符串，但前端业务页、流程变量处理、用户选择器值归一化如果再次执行 `Number(id)`，仍会破坏 ID 精度。

**解决方案**:
- 前端所有 `Long` / 雪花 ID / 用户 ID / 流程记录 ID 均按字符串保存和传参。
- 详情类接口优先使用稳定业务键，例如 `businessKey=sample_purchase_order:{id}`；`id` 只作为字符串兜底。
- 路径参数拼接前使用 `String(id)` 和 `encodeURIComponent`，不要用 `Number(id)`。
- 金额、数量等真实数值字段可以使用 `Number()`，但变量名包含 `id/Id/recordId/businessKey/purchaseOrderId/userId` 时必须保持字符串。

**影响范围**:
- 采购单审批测试页
- 流程外部表单 `variables`
- 用户选择器返回值
- 所有前端 API 请求中的后端 `Long` 主键和流程业务键

## 79. 代码表单 Provider 不能依赖低代码运行配置 configKey


**发现日期**: 2026-06-28

**问题描述**:
代码实现的复杂业务通过 `BusinessCodeFormProvider` 接入流程节点表单时，业务记录可能没有低代码 `AiCrudConfig.configKey`。如果 `BusinessFlowService.buildTaskFormContext()` 在进入节点表单解析前要求 `runtime.configKey` 不为空，纯代码业务会提前返回“未解析到业务对象、记录或运行配置”，导致 Provider 没有机会加载表单内容。

**根本原因**:
低代码业务表单需要 `configKey` 调用 `DynamicCrudService` 读取和保存记录，但代码表单 Provider 自己负责业务记录加载与保存，不能被低代码运行配置前置条件阻断。

**解决方案**:
- `buildTaskFormContext()` 只在低代码 `BUSINESS_OBJECT_FORM` 分支要求 `configKey`。
- `saveTaskFormContext()` 严格模式下，如果当前节点是 `BUSINESS_CODE_FORM`，允许 `configKey` 为空并交给 Provider 保存。
- `BUSINESS_CODE_FORM` 保存前仍必须由平台按节点字段权限过滤 `dto.data`，Provider 只接收允许写入的字段。

**影响范围**:
- 采购单审批测试等代码优先流程业务
- 任何不通过低代码应用创建、但要复用平台待办/已办表单上下文的复杂业务

## 80. AiForm 字段权限必须由组件和调用端共同接入


**发现日期**: 2026-06-29

**问题描述**:
流程节点字段权限已经保存到 BPMN，并由后端待办表单上下文返回 `fieldPermissions`，但业务托管表单使用 `<AiForm>` 渲染时如果组件本身不消费该 prop，或 `todo.vue/done.vue` 没有把权限传进去，前端仍会展示可编辑字段。后端虽然会过滤不可写字段，但用户感知是“配置不生效”。

**解决方案**:
- `AiForm.vue` 必须支持 `fieldPermissions`，按字段编码处理 `visible/editable/required` 三态。
- 待办页业务托管表单分支必须传入 `taskFormInfo.fieldPermissions` / `businessFormContext.fieldPermissions`。
- 已办、历史详情这类场景必须在调用端强制把所有可见字段覆盖为 `editable=false`，不能只依赖原节点配置。

**影响范围**:
- 流程待办 / 已办中的业务托管表单。
- 任何未来复用 `AiForm` 承载节点级字段权限的页面。

## 82. 流程字段权限新旧键名必须新键优先并双写


**发现日期**: 2026-06-29

**问题描述**:
流程设计器节点抽屉的“表单字段权限”矩阵里，“可见 / 可编辑”点击后看起来没有反应，或短暂变化后立刻回弹。

**根本原因**:
字段权限对象为了兼容运行时同时保留了旧键 `visible/editable` 和新键 `readable/writable`。如果归一化时优先读取旧键，而 UI 点击只更新新键，下一次 computed 重算会继续用旧键覆盖新状态。

**解决方案**:
- 权限归一化统一优先读取 `readable/writable`，只在新键缺失时回退到 `visible/editable`。
- `FormPermissionConfig.update()` 写入 `readable/writable/required` 时同步维护 `visible/editable`。
- BPMN parser/writer 也要使用同一归一化规则，避免设计器保存后再打开出现状态漂移。

**影响范围**:
- 流程设计器字段权限矩阵。
- BPMN `flowable:formFieldPermissions` 的读写往返。
- 待办/已办表单字段权限运行时消费。

## 83. 流程设计器全局保存前必须提交打开中的节点抽屉草稿


**发现日期**: 2026-06-29

**问题描述**:
在流程设计器节点抽屉里修改“表单字段权限”后，点击页面顶部“保存草稿 / 发布部署”，第二次进入流程设计器发现修改丢失。

**根本原因**:
`NodeConfigDrawer` 内部维护 `draftNode` 草稿，字段权限变化只进入抽屉草稿。只有点击抽屉底部“保存”才会 emit 到 `DingFlowDesigner` 主流程 JSON。用户直接点击顶部全局保存/发布时，`getXML()` 从主流程 JSON 序列化，拿到的仍是旧节点配置。

**解决方案**:
- `NodeConfigDrawer` 暴露 `commitDraft()`，复用抽屉保存逻辑但不关闭抽屉。
- `DingFlowDesigner.getXML()` 在 `convertJsonToBpmn()` 前调用 `commitOpenDrawerDraft()`。
- 回归测试覆盖：打开节点抽屉、修改字段权限、不点抽屉保存，直接 `getXML()`，BPMN XML 必须包含最新 `flowable:formFieldPermissions`。

**影响范围**:
- 流程设计器顶部“保存草稿 / 发布部署”。
- 节点表单资产、字段权限、审批人、会签、审批权限等所有由节点抽屉草稿承载的配置。

## 84. 运行时字段权限必须复用共享归一化


**发现日期**: 2026-06-29

**问题描述**:
流程设计器字段权限已经保存到 BPMN，第二次进入也能回显，但待办审批表单渲染时仍然没有按“不可见 / 不可编辑”展示。

**根本原因**:
运行时字段权限消费链路分叉：
- `AiForm` 只接受数组，不解析 Flow 服务 `TaskFormInfo.formFieldPermissions` 返回的 JSON 字符串。
- `FlowFormCreateRenderer`、代码业务页 composable 和后端 `BusinessFlowService` 各自维护归一化逻辑，容易出现新旧键优先级不一致。
- `todo.vue` 用 `businessFormContext.fieldPermissions || taskFormInfo.formFieldPermissions` 取值，空数组是真值，会挡住后面的节点权限。

**解决方案**:
- 前端统一使用 `forge-admin-ui/src/utils/field-permissions.js`，支持数组、JSON 字符串、`{ fields: [] }` 三种输入。
- 归一化必须优先读取 `readable/writable`，只在缺失时回退 `visible/editable`。
- 待办页权限来源必须取“第一个非空权限源”，不能用简单 `||`。
- 后端 `BusinessFlowService.normalizeFieldPermissions()` 也必须保持相同优先级，不可写字段必须清掉 `required`。

**影响范围**:
- 待办 / 已办业务托管表单。
- 节点动态表单 `FlowFormCreateRenderer`。
- 代码业务页通过 `useBusinessTaskFormContext()` 接入节点字段权限的页面。
- 任何从 BPMN `flowable:formFieldPermissions` 读取运行时权限的后端接口。

## 85. 节点表单资产选择不能清空字段权限


**发现日期**: 2026-06-29

**问题描述**:
流程设计器节点抽屉中，用户已经配置好“表单字段权限”，再点击“节点表单资产”下面的表单卡片后，下方权限配置立即消失或恢复默认。

**根本原因**:
`ApproverConfig.handleFormAssetUpdate()` 把资产选择事件当成整块节点表单配置替换处理，每次选中资产都固定写入：

```js
formFieldPermissions: []
```

所以即使用户只是点了一下当前已选中的表单资产，也会把已经配置好的权限矩阵清空。

**解决方案**:
- 只有“清除绑定”时才清空 `formFieldPermissions`。
- 选中表单资产时按该资产的字段目录重建权限：
  - 同名字段保留已有 `readable/writable/required` 配置。
  - 新字段补默认 `readable=true`、`writable=true`。
  - 表单源字段必填时同步默认 `required=true`。
- 为 `ApproverConfig` 增加回归测试，覆盖点击当前资产卡片后权限不丢。

**影响范围**:
- 流程设计器节点抽屉“表单权限”页签。
- `BusinessFlowFormAssetSelect` 卡片选择事件。
- 任何未来把“选择资产”和“字段权限矩阵”放在同一配置块里的节点配置组件。

## 86. 代码应用配置不能替代 Provider 当前字段基准


**发现日期**: 2026-06-30

**问题描述**:
代码应用进入应用管理后，如果已经保存过 `codeAppMetadata.fields`，再次进入“表单设计 / 列表设计 / 详情设置”时只读取旧配置字段，Provider 或业务表后续新增的字段不会再出现。用户会感觉业务表单配置仍然“写死在代码里”，无法在应用管理扩展。

**根本原因**:
代码应用元数据加载时把 `metadata.fields` 当成唯一事实来源，忽略 Provider 当前返回的字段目录。`metadata` 应该只是用户显示配置覆盖层，不能代替代码 Provider 的字段基准。

**解决方案**:
- 设计器和后端 `getFormAssets` 都必须以 Provider 当前字段为基准。
- `codeAppMetadata.fields/formAssets` 只覆盖 label、visible、formVisible、listVisible、排序、组件显示属性等用户配置。
- Provider 新增公开字段应自动补进默认 `formDesignerSchema/viewSchema/pageSchema`。
- 用户显式隐藏的字段要以 `visible=false` 或 `formVisible=false` 保留在 metadata 中，合并时不能被 Provider 重新带回运行态。

**影响范围**:
- 代码应用应用管理入口。
- `BusinessCodeFormProvider` 字段目录。
- `ai_business_binding.binding_config.options.codeAppMetadata`。
- 采购审批等代码实现业务表单的列表、详情、待办表单。

## 87. 代码表单资产只改设计器不改运行时会导致审批仍走写死 Provider 配置


**发现日期**: 2026-06-30

**问题描述**:
代码应用在应用管理里维护了 `codeAppMetadata.formAssets` 后，如果只有 `BusinessFlowService#getFormAssets` 或前端设计器读取时合并 metadata，而待办运行时 `resolveBusinessTaskFormAsset/collectTaskFormAssets` 仍直接读取 `BusinessCodeFormProvider#formAssets`，流程设计器里看到的新 `formKey/formUrl/providerKey` 和审批页实际解析的表单资产会不一致。

**解决方案**:
代码表单资产配置必须同时覆盖两条链路：

```java
// 设计态：业务配置中心 / 流程设计器表单资产列表
getFormAssets(objectCode) -> mergeCodeAppAssets(providerAssets, codeAppMetadata)

// 运行态：待办上下文 / 节点字段权限解析
collectTaskFormAssets(objectCode) -> mergeCodeAppAssets(providerAssets, codeAppMetadata)
```

`mergeCodeAppAssets` 不能只合并 `formName/description` 这类展示字段，也必须合并 `formKey/formUrl/providerKey/formMode/type/supportsSave` 等引用字段；否则用户在应用管理里改了表单资产，审批运行时仍会命中 Provider 里的默认硬编码值。

**影响范围**:
- 代码应用业务表单资产配置。
- 流程设计器全局表单和节点表单资产选择。
- 待办审批业务表单上下文解析。

## 88. Flowable 流程定义标识不能直接字符串比较


**发现日期**: 2026-06-30

**问题描述**:
待办业务表单加载时报错“流程定义与当前任务不匹配”。同一个流程在不同链路里可能出现三种表示：业务流程模型 Key、Flowable `key:version:id`、历史 UUID 型 `processDefinitionId`。如果业务侧直接比较字符串，会把同一流程误判为不匹配。

**解决方案**:
- 流程任务详情返回前尽量把 `processDefKey` 归一化为业务模型 Key。
- 业务校验流程定义时先抽取 `key:version:id` 的 key，再比较；历史 UUID 型值只作为兼容旧任务的兜底，不作为唯一业务主键。
- 待办详情首个业务表单上下文请求优先只传 `taskId`，由后端任务详情补齐流程实例、业务 Key、节点和流程定义；不要把列表行里的旧 `processDefKey` 当作可信身份字段。
- 流程定义标识表示差异不要作为硬安全边界直接抛错，真正的访问边界应放在任务 ID、办理人/候选人、流程实例、业务 Key 和任务节点校验上。
- 不要取消任务 ID、办理人、流程实例、业务 Key 和任务节点校验，流程定义兼容只解决标识表示差异。

**影响范围**:
- 待办审批业务表单上下文加载。
- `BusinessFlowService#validateTaskAccess` 这类跨流程服务的任务身份校验。
- `FlowTaskServiceImpl#getTaskDetail` 返回给业务侧的流程定义字段。

## 89. 自定义业务表单不要重复请求父级已加载的待办上下文


**发现日期**: 2026-06-30

**问题描述**:
待办审批详情中，父级抽屉为了判断业务表单类型已经加载 `/ai/business/flow/task-form-context`，自定义 Vue 业务表单组件挂载后如果再次调用同一上下文接口，再额外加载代码应用配置和业务详情，会让表单首屏出现明显延迟。

**解决方案**:
- 父级 `FlowBusinessForm` 应把已加载的业务表单上下文作为 `initialTaskContext` 透传给业务组件。
- 业务组件待办模式优先用 `initialTaskContext.recordData` 渲染首屏；只有上下文缺失或记录数据为空时才补查业务详情。
- 待办模式下字段显隐和标签优先使用上下文 `fields`，避免再请求代码应用 metadata。

**影响范围**:
- 待办详情里的代码业务表单。
- `FlowBusinessForm` 动态组件加载协议。
- 使用 `useBusinessTaskFormContext` 的自定义业务表单页面。

## 90. 驳回到修改节点的业务状态不能只依赖 TASK_COMPLETED 变量


**发现日期**: 2026-06-30

**问题描述**:
采购审批普通审批节点点击“驳回”后，流程已经进入“申请人修改”节点，但采购单业务状态仍停留在 `IN_PROCESS`。用户在申请人修改节点重新提交时，业务字段保存先校验状态，报“当前采购单不是待修改状态，不能执行申请人修改节点”。

**根本原因**:
业务状态只监听上一个审批任务的 `TASK_COMPLETED` 事件，并依赖事件变量中的 `approvalResult=reject` 或 `approved=false`。Flowable 任务完成事件与变量读取存在时序差异，或者事件回调已错过时，业务表状态不会同步为 `NEED_MODIFY`，但流程图已经真实流转到申请人修改节点。

**解决方案**:
- 审批动作变量仍应在 `completeTask` 前写入流程实例，保证网关和完成事件尽量读取到本次动作。
- 业务状态机必须同时监听 `TASK_CREATED`：当新建任务节点为 `applicant_modify` 且业务单据仍为 `IN_PROCESS` 时，兜底同步为 `NEED_MODIFY`。
- 对已经错过事件的存量待办，申请人修改节点保存字段时，如果状态仍为 `IN_PROCESS`，应在同一事务内先自愈为 `NEED_MODIFY`，避免重新提交被业务状态拦截。
- 同一套状态机还必须覆盖反向流转：申请人修改后重新提交，进入任一普通审批节点时，如果业务单据仍为 `NEED_MODIFY`，必须兜底同步为 `IN_PROCESS`。
- 对已经进入普通审批节点但业务状态仍为 `NEED_MODIFY` 的存量待办，审批节点保存字段时也要在同一事务内自愈为 `IN_PROCESS`。

**影响范围**:
- 采购审批示例。
- 后续生成类似“驳回修改 / 申请人补正 / 重新提交”流程 skill 的业务状态机模板。
- 所有依赖流程事件回写业务单据状态的代码表单 Provider。

## 98. 审批运行态表单不能重建简化字段配置


**发现日期**: 2026-07-04

**问题描述**:
待办审批页使用低代码业务对象表单时，渲染样式和表单设计器不一致，对象引用字段显示 ID 而不是中文名称。

**根本原因**:
后端待办上下文如果从表单 schema 中抽取字段时只保留 `field/label/componentType/dictType`，再重新组装审批字段，会丢掉设计器原始的 `props`、`span`、`componentKey`、`referenceObjectCode`、`referenceDisplayField`、`recordSelector`、校验和样式配置。前端拿到这种简化字段后会把对象引用、记录选择器等业务组件降级成普通输入框，recordData 也可能缺少引用显示字段。

**解决方案**:
- `BusinessFlowService` 构建审批字段时必须从原始组件配置复制，再叠加节点字段权限的 `readonly/disabled/required`。
- 后端组件类型归一化要覆盖 `AiFormItem` 支持的运行态类型，不能把未知业务组件直接降级为 `input`。
- `filterVisibleRecordData` 除字段自身值外，还要带上对象引用/记录选择器的显示字段，例如 `referenceDisplayField`、`labelField`、`warehouseId -> warehouseName`。
- 前端只读选择类字段的显示候选要读取 `referenceDisplayField/displayField/labelField/targetLabelField`，不能只依赖 `xxxId -> xxxName` 约定。

**影响范围**:
- 低代码业务对象待办/已办审批表单。
- 对象引用、记录选择器、字典、级联、人员/组织等选择类字段。
- 表单设计器布局、字段跨度、组件 props 在审批运行态的回显。

## 99. 低代码审批详情不能只渲染主表 AiForm


**发现日期**: 2026-07-04

**问题描述**:
采购单等主子表低代码单据进入待办/已办审批详情时，主表字段能显示，但采购明细等子表数据为空。驳回后重新发起同一单据时，Flowable 报“业务流程已存在且不可重复发起：业务对象:记录ID”。

**根本原因**:
CRUD 详情页的渲染逻辑是“主表 `AiForm` + 子表 `ChildTableEditor`”，数据来自 `DynamicCrudService.selectById()` 返回的 `{ main, children }`。审批页如果只把字段过滤后交给 `AiForm`，并且过滤时丢掉 `recordData.children`，子表永远不会显示。

流程重新发起的错误则来自 Flowable 业务关联表按 `businessKey` 关联实例。低代码单据的业务 key 应该稳定表示“对象 + 记录”，但 Flowable 每次流程实例启动需要一个不会撞旧流程记录的实例 key。

**解决方案**:
- `BusinessTaskFormContextVO` 需要返回 `childrenConfig`，审批上下文过滤主表字段时必须保留 `recordData.children`。
- 待办/已办页面渲染低代码业务表单时，复用 CRUD 详情同类的 `ChildTableEditor` 只读渲染子表。
- 启动流程时保留低代码单据原始 `businessKey/documentBusinessKey/recordBusinessKey`，同时给 Flowable 启动传唯一的 `flowBusinessKey`，回写状态时再按流程实例关联和原始单据 key 更新业务单据。

**影响范围**:
- 低代码主子表单据的待办、已办审批详情。
- 驳回后重新发起、修改后重提等同一业务单据多次进入流程的场景。

## 100. 流程表单资产不能只读取业务对象设计草稿


**发现日期**: 2026-07-06

**问题描述**:
流程设计器选择业务应用表单时，`/ai/business/flow/form-assets/{objectCode}` 返回空；应用中心里已经有低代码单据和表单，但流程里选不到。

**根本原因**:
低代码应用发布后的真实运行表单资产主要在 `ai_crud_config`：`config_key/object_code` 用于定位对象，`options.formDesignerSchema` 保存应用表单设计协议，`edit_schema/model_schema` 是运行态字段兜底。只读取 `ai_business_object.designer_options.formDesignerSchema` 会漏掉已发布运行配置，尤其是 `PW_OUTBOUND_ORDER` 这类 `object_code` 为大写、`config_key` 为小写的单据。

**解决方案**:
- 流程表单资产接口必须同时聚合 `ai_business_object.designer_options.formDesignerSchema` 和已发布 `ai_crud_config`。
- 读取 `ai_crud_config` 时优先使用 `options.formDesignerSchema` 的多表单/表单资产协议，再兜底 `edit_schema`，最后兜底 `model_schema.fields`。
- 待办运行态解析节点表单时也要走同一套资产解析，不能只修设计器列表接口。

**影响范围**:
- 流程设计器节点抽屉的业务表单资产选择。
- `/ai/business/flow/form-assets/{objectCode}` 表单资产接口。
- 低代码单据待办、已办表单上下文解析。

## 101. 低代码业务表单空字段权限不能当成全只读


**发现日期**: 2026-07-06

**问题描述**:
流程表单资产能正常查到，待办表单也能回显业务单据数据，但审批节点里字段不能修改，暂存修改按钮也不出现或保存时报“当前节点没有可编辑业务字段”。

**根本原因**:
流程设计器字段权限面板的交互语义是“未配置时默认全量可写”。后端如果把空 `formFieldPermissions/fieldPermissions` 当成没有可编辑字段，低代码业务表单会被整体渲染为只读，保存时也会被平台字段过滤拦截。

**解决方案**:
- `BUSINESS_OBJECT_FORM` 运行态遇到空字段权限时，应从应用发布态表单字段目录生成默认权限：可读、可写，系统字段和只读/禁用字段除外。
- 返回待办上下文和保存待办字段必须使用同一套默认权限生成逻辑，避免页面看起来可写但保存被过滤。
- `BUSINESS_CODE_FORM` 不能套用低代码默认可写语义，代码 Provider 仍必须依赖显式节点权限做字段保存保护。

**影响范围**:
- 低代码业务对象待办表单上下文 `/ai/business/flow/task-form-context`。
- 低代码业务对象待办字段保存 `/ai/business/flow/task-form-context`。
- 流程设计器节点表单字段权限未配置或历史 BPMN 中权限为空数组的场景。

## 161. 设计态权限校验不能把当前用户授权当成权限资源目录


**发现日期**: 2026-08-03

**问题描述**:
业务流程、页面动作等设计态配置需要判断一个权限编码是否已经注册。如果直接使用 `SessionHelper.getPermissions()` 作为“已知权限”，同一份合法草稿会因当前设计者角色不同而时而通过、时而失败；反过来，代码硬编码加入一个尚未落库的默认权限，又会让校验通过但运行时永远无法授权。

**解决方案**:
- 设计态存在性校验必须按租户查询 `sys_resource` 等真实权限资源目录，只校验草稿实际声明的权限集合；当前用户是否有权编辑流程仍由 Controller/Service 的管理权限单独判断；
- 新默认权限必须通过新的幂等 Flyway 注册并明确最小角色继承，禁止修改已执行迁移或只在校验器中硬编码放行；
- 运行时通用 API 权限只作为第一层门禁，流程发布快照中的具体动作权限、可见条件、记录状态、租户和数据权限仍需服务端二次校验；
- 审批依赖的“存在”还必须包含真实发布/部署状态，只有对象绑定不能证明 Flowable 模型可运行；流程服务不可用时发布校验失败关闭。

**影响范围**:
- 业务流程手动开始、低代码页面动作、应用权限设计器，以及所有区分“权限已注册”和“当前用户已授权”的设计态校验。

## 165. 多结果出口共享同一后继不能按边数直接展开布局


**发现日期**：2026-08-07

**问题描述**：
业务画布在一条线性边上插入审批节点时，会为 `APPROVED/REJECTED/CANCELED/FAILED` 创建四条语义边，并暂时让它们共享原后继节点。若布局算法直接按出边数量展开分支，会把同一个后继放到第一条分支的最左侧，其余边再折返到该节点，形成结束节点偏移、超长水平线和重叠插入按钮。

**解决方案**：
- 业务画布是 DAG，多入边节点应在只读布局适配层显式标记为汇合点；布局提示不能写回 `businessProcessJson` 或污染 BPMN `flowJson`；
- 同源同目标边不能合并或删除，它们仍代表不同运行结果；渲染时按 `sourcePort` 稳定顺序分配独立锚点和插入目标；
- 节点端口标签使用与锚点一致的等宽排列，避免语义顺序与视觉位置错位；
- 回归至少断言共享后继保持主轴居中、每条结果路径唯一、每个插入目标可区分，并组合执行共享 DingFlowDesigner 布局与转换测试。

**影响范围**：
- 审批结果、条件分支及任何“多条语义边先共享同一后继、之后再逐支插入动作”的业务 DAG 画布。

## 166. 条件分支端口是有序语义，不能作为集合排序


**发现日期**：2026-08-07

**问题描述**：
业务流程协议为了稳定 hash 会排序节点、边和依赖集合，但条件分支是“从上到下依次判断”，审批结果出口也有固定业务展示顺序。如果复用依赖集合的字符串归一化逻辑对 `node.ports` 按技术编码排序，`BRANCH_1` 会排到 `MATCHED` 前面，画布标签、锚点和连线顺序随之变成“条件 2、条件 1、默认”，即使 `config.branches` 本身没有变化也会造成认知错乱。

**解决方案**：
- 节点和边 ID、无序依赖集合可以稳定排序；节点 ports 必须去重但保留输入顺序；
- 条件 branches、ports、edge.sourcePort 必须在同一次图更新中同步，渲染与路由均使用同一个有序 port 列表；
- 回归测试同时断言协议规范化后的端口顺序、卡片中文标签顺序、SVG 路径唯一性和插入按钮位置。

**影响范围**：
- 条件顺序判断、审批多结果展示以及任何以端口数组表达优先级或业务顺序的流程协议。

## 167. DAG 分支路由必须同时处理跨层穿卡和三种顺序一致性


**发现日期**：2026-08-07

**问题描述**：
只解决“多个结果共享同一后继”仍不足以保证业务画布可用。当一个条件分支直接跳到较低层汇合节点、另一个分支经过中间动作卡片时，普通正交路由会让直达线穿过中间卡片；即使改为外侧绕行，若它与动作分支的汇合线共用相同纵向或横向线段，视觉上仍像连到了错误节点。另一个常见错位是条件配置顺序、下游卡片横向顺序和路由端口顺序分别计算，最终出现“第一条规则连到第二张卡片”。

**解决方案**：
- 业务 DAG 布局完成后逐条检测路径与除来源/目标外的节点矩形是否相交，命中时走图外侧独立通道；不能只依赖 dagre 的节点位置或边控制点；
- 绕行通道按来源端口和目标入边分配独立 lane，回归测试同时检查不穿卡和不同语义边不共享有效线段；
- 条件 `branches` 顺序是权威顺序，节点 `ports`、同层下游卡片槽位、来源锚点、路由 lane 和展示标签必须共同使用这一顺序；
- 测试至少包含“一支跨层直达、一支经过中间卡片再汇合”的图，不能只覆盖所有分支同层或共享同一后继的简单图。

**影响范围**：
- 条件、审批结果、并行分支、跳过节点和多分支汇合等所有业务 DAG 可视化路由。

## 183. 捕获参与当前事务的下游异常不能清除 rollback-only


**发现日期**：2026-08-23

业务编排器在同一事务中调用带 `@Transactional` 的动态 CRUD；下游抛出运行时异常时，Spring 会先把共享事务标记为 rollback-only。上层即使把异常转换为节点 `FAILED` 并继续返回，事务提交仍会抛出 `UnexpectedRollbackException`，之前写入的失败状态也会一起回滚，最终日志只剩提交异常而不是原始动作错误。

处理原则：流程终态事件必须在业务回调事务 `AFTER_COMMIT` 后消费；恢复编排和动作节点分别使用新的事务。动作失败时只回滚动作写入，外层仍可提交节点和运行实例的 `FAILED/errorSummary`。动作级独立事务意味着成功副作用必须使用稳定幂等键，并先于检查点提交；尤其创建记录动作不能依赖跨事务回滚避免重复。监听器必须记录完整原始异常且不反向回滚已经完成的 Flowable 回调。

## 184. 同一审批结果下的不同驳回动作必须使用互斥变量

**发现日期**：2026-09-19

普通驳回和“退回发起人修改”都写 `approvalResult=reject` 时，如果 BPMN 只判断该变量，流程级 `TO_END` 会把两个动作都送到结束节点。Flowable 流程变量又会跨任务保留，旧的 `rejectToStart=true` 若不清除，还可能让后续普通动作误入专用回路。

处理原则：专用动作使用 `${approvalResult == 'reject' && rejectToStart == true}`，自动普通驳回使用 `${approvalResult == 'reject' && rejectToStart != true}`；通过和普通驳回每次都显式写 `rejectToStart=false`，专用动作再覆盖为 `true`。驳回到修改节点时，单据状态与流程关联状态必须同时写为运行态 `NEED_MODIFY`，不能只改其中一处或把它当作终态。

## 新版应用撤回不可依赖单据配置和 Web Session

**发现日期**：2026-09-20

新版应用已有流程实例，但 `documentEnabled=false`。运行态只在旧单据配置分支生成撤回动作，会使发起人只能到“我发送的”撤回。撤回后的消息回调又在动态仓储填充 `update_by` 时调用 Web Session，抛出 `SaTokenContextException`；上层吞掉写入异常后仍会因 rollback-only 整体回滚，最终引擎 canceled 而记录/关联仍在审批中。

处理原则：流程动作依据实例关联、发起人、运行状态和实际接口权限生成，不依赖旧模式或当前待办。审计取值必须兼容显式执行身份和无 Web 的系统后台写入；缺少 Web 上下文不伪造用户，也不放宽租户/数据权限。状态回写失败须传播并记录原始堆栈，不能继续更新关联表。旧失败事件不会因重启自动补发，历史漂移需按实际引擎状态受控补偿。

## 流程 Redis 回调非 Web 异常是 NotWebContextException

**发现日期**：2026-09-23

**问题描述**：启动流程后 `flowRedisListenerContainer` 同步 `flowStatus` 失败，日志为「非 web 上下文无法获取 HttpServletRequest」，随后 `UnexpectedRollbackException: rollback-only`。列表流程状态时有时无：状态字段与实例关联同事务回滚，列表列读库字段为空，批量 `_documentRuntime` 也可能补不齐。

**根因**：
1. Sa-Token Spring 在非 Web 线程抛的是 `NotWebContextException`（直接继承 `SaTokenException`），不是此前只 catch 的 `SaTokenContextException`。
2. 任务事件 catch 吞掉异常后，内层 `@Transactional` 已把外层标成 rollback-only，提交时再炸。

**解决方案**：
- `DynamicCrudRepository.auditSessionValue` 捕获整个 `SaTokenException`。
- `DataScopeServiceImpl` 非 Web 时安全返回 null；内部字段回写无用户会话时跳过写数据权限，仍保留租户条件。
- 任务事件状态同步用 `REQUIRES_NEW`，失败不再毒化外层 FlowCallback 事务。

## 列表流程状态列偶发消失是旧 fieldRefs 快照滤掉了托管字段

**发现日期**：2026-09-23

**问题描述**：审批节点自动补齐 `flowStatus` 并发布后，列表有时有「流程状态」列、有时没有。列表设计器里也不像独立组件，用户以为字段丢了。

**根因**：列表自由布局 `listGridLayout` 的 `AiCrudPage.fieldRefs` 优先于 table zone。后补字段只写了 zone，旧网格快照不含 `flowStatus`，发布 `columnsSchema` 就被滤掉。前端 `includeManagedRuntimeFieldRefs` 又依赖 `advancedProps.managedBy`，而列目录常没有该标记，运行态兜底失效。

**解决方案**：
- 字段可见性同步同时写 zone 与 listGridLayout；ensure 已有字段时补列表选列。
- 运行配置构建强制补托管 `flowStatus` 列（显式隐藏除外）。
- 前端按字段名/字典识别托管字段，并可从字段目录合成缺失列。

**补充（2026-09-25）**：上述修复只在「重新发布对象」时生效。保存流程时 `ensure` 只写对象草稿并建列，正式运行读已发布版本快照；应用发布对已发布对象只固定旧版本、不重建。另外有编辑权限的用户在门户走 `designPreview` 看草稿，所以同一应用有人有列有人没有。现在 `AiCrudConfigService.resolvePublishedRuntimeConfig` 在草稿已有托管 `flowStatus`、发布快照缺失时自愈补进 `modelSchema` 与 `columnsSchema`（表头、取值、回写共用）；流程保存时已绑定节点也会（有 DDL 权限时）重跑幂等 `ensure`。流程按钮仍以已发布流程版本为准。

**补充（2026-09-28）**：把 `flowStatus` 按平台默认列表列处理。运行态自愈按字段名/列名识别（不再要求 `managedBy`/`dictType`），同时补 `pageSchema` 的 table zone 与 `listGridLayout.fieldRefs`；前端字段目录对流程状态默认 `listVisible=true`。字段已存在时，无 DDL 权限也可只修列表选列。列表丢失时不必再依赖「进流程设计点保存」才能恢复。

## 带排序和行数限制的流程锁查询会被 JSqlParser 重排

**发现日期**：2026-09-22

**问题描述**：流程模型版本清理 SQL 原文为 `ORDER BY ... LIMIT ... FOR UPDATE`，经过 MyBatis-Plus 租户拦截器和数据权限拦截器的 JSqlParser 重写后，输出变成 `FOR UPDATE ORDER BY ... LIMIT ...`，MySQL 报 1064，清理接口稳定返回 500。

**解决方案**：需要行锁的 Mapper 查询只保留显式租户/逻辑删除条件和 `FOR UPDATE`，不要在同一条锁 SQL 中拼排序或行数限制；若排序决定业务语义，查询返回后在 Java 中按与原 SQL 相同的字段和方向稳定排序，再执行保留/删除判定。新增锁查询契约测试，禁止 `FOR UPDATE` 与排序/行数限制同时出现。

## Flowable 8 动过库后不能只改版本号回退到 7.x

**发现日期**：2026-10-13

**问题描述**：
同一测试库先被 Spring Boot 4 / Flowable **8.0.0** 启动写过库，再切回仓库 `main` 的 Flowable **7.0.1** 启动 `flow-server`，会连环失败：

1. `Could not update Flowable database schema: unknown version from database: '8.0.0.0'`  
   （`act_ge_property.schema.version` / `act_id_property` 已被标成 `8.0.0.0`）
2. 只把版本号改回 `7.0.1.1` 后，又报 Liquibase：`Table 'flw_event_deployment' already exists`（以及后续 App/CMMN/DMN 同类问题）  
   （8 已建好 `flw_*` / `act_app_*` / `act_cmmn_*` / `act_dmn_*` 表，但 `flw_ev_databasechangelog` 等变更记录为空或与 7.x changelog 不一致，7.x 会再执行 `CREATE TABLE`）

**根因**：Flowable 主版本与**库内 schema 版本标记 + Liquibase 托管引擎表**是一套的。只回写 `act_ge_property` 不够；半升级/半回退会把「有表、无 changelog」留给 7.x。

**解决方案**：
- **代码与库必须同主版本**：继续跑 7.0.1 就不要用 8.x 进程碰共享测试库；要上 SB4/Flowable 8 就整条链路（Admin + Flow + 依赖）一起升，不要只升一半。
- 误用 8 污染测试库、且仍要跑 **7.0.1** 时（测试库、无重要流程运行数据）：
  1. 将 `act_ge_property` / `act_id_property` 中各 `*.schema.version` 恢复为 `7.0.1.1`（含 `common` / `batch` / `job` / `task` / `variable` / `entitylink` / `identitylink` / `eventsubscription` 等 7.x 键；去掉 8 独有的 `app`/`cmmn`/`dmn`/`eventregistry` 版本键或整表按 7.x 种子重建）。
  2. **删除** Liquibase 托管且 changelog 已乱的引擎表，让 7.x 启动时重建：Event Registry（`flw_event_*`、`flw_channel_definition`、`flw_ev_databasechangelog*`）、App（`act_app_*`）、CMMN（`act_cmmn_*`）、DMN（`act_dmn_*`）。
  3. **保留**流程引擎核心表（`act_re_*` / `act_ru_*` / `act_hi_*` / `act_ge_*` / `act_id_*`）及业务侧 `sys_flow_*`，勿整库清空。
- 禁止只插 `INSERT INTO act_ge_property … 8.0.0.0` 的反向操作却不处理 Liquibase 表状态。
- 社区「纯净版 SQL」仍应跳过 `act_*`/`flw_*` 导出（启动自动建）；与本次「版本混用」是不同坑，见 `forge-project-init` 的 `references/pure-sql-export.md`。

## Flow 服务承载业务表单接口时不能沿用旧的 FlowClient 8080 默认端口

**发现日期**：2026-09-28

**问题描述**：H5 网关按约定把 `/ai/business/flow/**` 转到 Flow 服务。Flow 服务同时装配 generator 的 `BusinessFlowController`，该服务内部仍通过 `FlowClient` 读取 `/api/flow/task/form/{taskId}`。若未显式配置客户端地址，旧默认值 `localhost:8080` 与仓库 Flow 基准端口 `8081` 不一致；内部连接失败被表单服务安全收敛为空任务，前端最终只看到“任务不存在或无权访问”，而走 Admin 的 PC 端因为部署时配置了 Flow 地址所以正常。

**解决方案**：
- `FlowClientProperties` 默认地址与仓库 Flow 端口统一为 `http://localhost:8081`，容器或非标准部署仍通过 `forge.flow.client.url` 显式覆盖；
- 自调用继续透传当前 Sa-Token，让 `FlowAccessGuard` 执行租户和参与人校验，禁止为消除报错绕过权限；
- H5 业务表单只消费 `task-form-context`，接口失败时不再从表单资产或流程变量伪造可编辑表单；
- 回归同时覆盖客户端默认地址和前端无资产兜底，真实部署后再用当前用户待办验证接口 200。
