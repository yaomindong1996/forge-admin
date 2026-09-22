# 踩坑：低代码 / 设计器 / 业务对象

> 从 `code-copilot/memory/pitfalls.md` 按主题拆出。新条目追加到本文件。共 93 条。

## GET render 设计预览写关系表导致 Lock wait timeout

**发现日期**: 2026-09-22

**问题描述**:
`GET /ai/crud-config/render/{configKey}?designPreview=true` 调用 `prepareRuntimeDraft`，在事务里执行 `ensureChildTableRelations` → `relationMapper.updateById`。并发预览、多区块渲染或与设计器保存重叠时，对 `ai_business_object_relation` 抢锁，抛出 `CannotAcquireLockException: Lock wait timeout exceeded`。

**解决方案**:
渲染链路改走 `prepareRuntimeDraftForPreview`：只编译草稿 schema，不同步写关系。子表关系仍由设计器保存和发布的 `synchronizeFormChildRelations` 落库。关系配置比较改为 JSON 语义相等，避免 key 顺序差异触发无意义 UPDATE。

## 导入已有表新增不自动填充审计字段

**发现日期**: 2026-09-22

**问题描述**:
表单设计导入已有表后新增数据，`create_by` / `create_time` / `create_dept` / `update_by` / `update_time` 为空。导入策略要求五列齐全才设 `FORGE_COLUMNS`，否则整段 `NONE`；且 `saveRuntimeDraft` 不把 `modelSchema.auditStrategy` 同步到 `ai_crud_config.audit_strategy`，运行时跳过自动填充。

**解决方案**:
导入按实际存在的审计列启用策略；草稿保存同步 tenant/audit/logicDelete 策略到配置列；插入时若策略误标 `NONE` 但物理表仍有标准审计列则按列补齐，并写入 `del_flag` 活跃值。

## 表单页面形态必须传到实际 CRUD 组件

**发现日期**：2026-09-20

创建表单页已写入区块 `formOnly=true` 和 `objectRef.pageMode=form`，但 GridBlockRenderer 的运行配置合并与静态兜底均漏传 formOnly。外层按表单自动高度，内层仍渲染列表，配合隐藏工具栏/搜索就表现为空白；只检查模板创建测试无法发现。

形态按区块实例解析：显式 formOnly 优先，其次 pageMode（list/crud 必须覆盖旧 pageKey=form），最后兼容旧配置/运行默认值。编译与静态分支、门户高度使用同一解析规则，不能向同一对象的共享配置写入某一页模式。组件测试断言实际 AiCrudPage props，浏览器确认表单 DOM 与无列表请求；纯列表和列表＋表单同时回归。

## 草稿列表表头与数据查询必须共用运行配置编译结果

**发现日期**: 2026-09-20

**问题描述**:
草稿 render 即时编译 model/page Schema，已经返回子表列；动态 CRUD 的 `getConfig` 却直接返回存储实体，旧 `columnsSchema` 仍只有主表字段。导致页面表头出现子表列，列表查询没有触发 JOIN，返回记录完全缺少子表字段键。详情中存在子表行，不能误判为业务数据未保存。

**解决方案**:
设计预览表头与取数都调用 `AiCrudConfigService.resolveDraftRuntimeConfig`，在内存副本上统一生成搜索、列表、表单、选项和翻译/加密协议。保留设计权限校验，普通请求仍读发布快照；不能为了修复预览而覆盖缓存、自动发布或改业务数据。回归必须检查实际 JOIN SQL 和返回别名/值，不能只测列编译。

## 子表字段不能同时进主表单和主表字段目录

**发现日期**: 2026-09-19

**问题描述**:
审批待办把子表字段渲染了两次：`appendRuntimeChildFieldCatalog` 把子表字段以 `scope=child`、裸字段名放进主字段目录，`buildTaskFormFields` 没有跳过，主表单和 `childrenConfig` 各画一遍。列表设计选出的 `modelCode__field` 在非 `master-detail-crud` 布局下也会进 `editSchema`。节点权限面板 `collectBusinessAssetFields` 按裸字段名去重并丢掉 `childKey`，子表字段权限出不来。应用页区块 `fieldRefs` 仍是主表快照时，会把已编译的子表列滤掉。流程结束回写只查已发布运行配置；对象还是草稿时 `syncConfiguredStatusField` 直接返回，关联却被标成已结束，单据 `flowStatus` 停在 `IN_PROCESS`。

**解决方案**:
主表单跳过 `scope=child` 和 `__` 子表字段引用，子表只走 `childrenConfig`。编辑 schema 一律去掉子表字段引用，列表 columns 保留。流程模型的表单权限目录来自应用页面表单资产，不是运行时 `editSchema`；明细表字段在 `subTable.props.columns`。同一张子表会同时出现关系键和带应用前缀的对象编码（`detail_ujpc` / `cgou_detail_ujpc`），权限面板必须收成一套，审批回放按别名匹配，不能因为业务流程 formKey 和节点 formKey 写法不同就把节点权限清空。字段「可编辑」必须能改已有子表行，不能再被行级 `allowUpdate=false` 盖掉。带子表时节点 `formFieldPermissions` 是 JSON 对象字符串，不能按数组解析，否则 `fields` 整段丢失，暂存会报「不允许编辑子表字段」。主从表单据的字段在 `main` 里，审批标题不能只扫记录最外层，否则 `${fieldInput}` 不会被替换。结束回调用快照 `configKey` 回退草稿配置写 `flowStatus`；关联已结束时仍补写一次。列表设计字段面板要按主表/子表分组；选出的子表列不能被 `listVisible=false` 从表格区滤掉。列表默认按外键把子表聚合成一行再 LEFT JOIN，避免一对多把主表行乘开、分页主键重复。用户可在「子表行展示」里改成拆成多条；拆行时表格行键用 `__listRowKey`，编辑和删除仍按主表主键，批量删除要先去重。

## 子表运行时单元格不能把 class 落到 AiFormItem 碎片根上

**发现日期**: 2026-09-18

**问题描述**:
`ChildTableEditor` 把 `class="child-runtime-cell"` 直接打在 `AiFormItem` 上。`AiFormItem` 模板是多根节点（分隔线 / 分组标题 / `n-form-item`），Vue 无法自动继承 class，控制台报 `Extraneous non-props attributes (class)`；隐藏校验反馈的单元格样式也不生效。同时 `props.value` 的 deep watch 在每次输入回写后重建行对象，未保留 `__rowKey` 时表格行会重挂载，抽屉里子表输入框表现为点不动、一输入就失焦。

**解决方案**:
用 `<div class="child-runtime-cell">` 包住 `AiFormItem`，不要把 class 落到碎片根上。`AiFormItem` 设 `inheritAttrs: false`，把 `$attrs` 绑到 `n-form-item`。回写父级后若编辑器值未变则跳过本地重建，并用上一行的 `__rowKey` 兜底。

## 表单字段资产未使用列表不能只从当前画布抽字段

**发现日期**: 2026-09-17

**问题描述**:
低代码应用「表单设计」里，字段资产货架的字段来自 `resolveFormAssetFields(formDesignerSchema)`，也就是当前画布上已经放了的字段。因此「未使用」在已有表单对象时恒为空；从画布删除字段组件后，该字段同时从货架消失，未使用列表仍没有数据。对象设计器上下文只缓存了 relations/actions，没有把对象字段目录交给货架。表单设计 Tab 默认不进 `formDesignerMode`，还会把对象字段合并整段跳过。

**解决方案**:
字段资产货架以对象字段目录为事实源，当前画布字段只补充尚未保存的新字段。`activeFormDesignerObjectRef` 在表单设计 Tab 也要解析绑定对象；`ensureFormDesignerObjectContext` 必须缓存 `designer.fields` / `modelSchema.fields`，再用 `mergePageFieldCatalogs` 合并进 `activeFormFields`。删除画布组件只移除表单展示，字段资产留在未使用列表，可重新拖入。返回表单设计要按当前选中页挂表单，不能用另一张页的 formAssetId；设计区不能用 auto 行高，否则顶栏消失后画布高度是 0，要先点列表设计再回来才重新撑开。

## 表单发布检查必须展平 row/col 子组件


**发现日期**: 2026-08-31

**问题描述**:
默认 4 列栅格会把业务字段放进 `formDesignerSchema.components[0].children[].children`。发布检查如果只遍历顶层 `components`，会把唯一的 `row` 当成布局容器跳过，然后误报 `FORM_FIELD_COMPONENT_EMPTY`（表单中没有绑定业务字段的组件）。电商应用的店铺、产品、订单都因此被阻断，但字段其实已经绑定。

**解决方案**:
`BusinessObjectPublishService.checkFormDesignerSchema` 必须先展平 `children`，再跳过 `row`/`col`/`card` 等虚拟组件并统计 `fieldBinding.mode=field` 的字段。空栅格仍然阻断；嵌套未绑定或不存在字段继续分别报 `FORM_FIELD_BINDING_EMPTY` / `FORM_FIELD_MISSING`。

## 预览/发布前的派生运行配置不能传播应用设计变更


**发现日期**：2026-08-23

`designPreview=true` 和发布前准备会物化表单、页面、关系等派生运行配置。如果复用普通设计保存方法并无条件调用 `markObjectChanged`，应用刚发布为 `PUBLISHED`，只要外层列表或工作台触发一次设计预览，就会立刻回到 `CHANGED`，显示“有未发布变更”。

处理原则：设计器中的用户保存继续传播变更状态；预览和发布准备只同步派生配置，保持对象当前设计状态且不得标记应用变更。回归顺序必须是“正式发布 → 确认 PUBLISHED → 调用 designPreview → 再确认仍为 PUBLISHED”，只测试发布接口当下状态不够。

## 存量对象型应用不能只按新版页面树判空


**发现日期**：2026-08-23

改版前的低代码对象型应用可能没有独立页面实体，应用只保存 `primaryObjectCode`，页面由 `ai_business_application_object` 的 `PRIMARY` 对象和 `ai_crud_config` 发布配置直接推导。若新版运行时只读取 `options.inAppBuilder.nodes/pages`，对象、入口、CRUD 配置和业务数据即使都在，应用仍会表现为“没有页面”。

处理原则：只对页面树为空、存在 `primaryObjectCode` 且能解析到有效主对象 `configKey` 的存量应用做一次性对象页投影；设计草稿与不可变发布快照必须采用同一兼容规则。恢复后写入显式迁移标记，避免用户后来主动删空页面时反复补回；已有新版页面的应用绝不能覆盖或合并猜测数据。排查时先区分“应用编码被复用/改名”和“真正的旧应用记录”，不要仅凭相似 URL 修改错误应用。

## 对象设计版本不能复用 CRUD 发布版本


**发现日期**：2026-08-12

`ai_business_object_design_version.version_no` 是同一业务对象的设计历史序号，唯一键为 `(tenant_id, object_id, version_no)`；`publish_version` 仅指向关联的 CRUD 发布版本。二者在普通创建时可能都从 1 开始，但种子/导入历史会使它们天然不同步。

处理原则：对象设计快照一律按该对象 `MAX(version_no) + 1` 分配，发布链路只向 `publish_version` 写入 CRUD `publishedVersion`。不要在 DTO 上保留可由调用方指定的对象设计 `versionNo`，否则首次人工发布会与种子快照冲突。

## 移动低代码字段事件不能只实现一次查询和回填


**发现日期**：2026-08-12

H5/企微运行页若只按字段事件调用查询源，会忽略统一协议中的 `skipWhenEmpty`、`clearTargetsOnTrigger`、`debounceMs` 和并发失效语义。用户快速改值时旧请求可能覆盖新结果，清空手机号或条码仍会请求外部系统，明细行复用同一个规则 ID 时还可能互相取消。

处理原则：移动运行时应与管理端保持相同协议语义；按“规则 + 主表/明细行作用域”隔离定时器、取消控制器和序列号，空值时跳过并按映射清理目标字段，CHANGE 事件按配置防抖，旧响应必须失败关闭。移动主子表保存前还要校验所有已渲染明细表单，不能只校验主表。

## 低代码动作种子只有 designer_options 没有发布版本时运行必然失败


**发现日期**：2026-08-12

`BusinessActionExecutionService` 执行动作时只读取 `ai_business_object_design_version` 的不可变已发布快照，不会直接信任 `ai_business_object.designer_options.actions`。如果 Flyway 只把动作 JSON 写进对象当前设计数据和页面按钮，界面能显示按钮，但运行时会报“业务对象缺少可执行的发布快照”；子表行动作还会因发布版本缺少 `relation_snapshot` 无法校验 `relationKey` 和父子归属。

处理原则：内置低代码应用若需要迁移后立即执行动作，必须通过正式发布服务生成版本，或在受控 seed 中同时写入与当前 CRUD 配置一致的 `ai_business_object_design_version`，至少包含 `model_snapshot`、`page_snapshot`、`designer_options_snapshot` 和完整 `relation_snapshot`，并同步对象的 `design_status`、`last_publish_version`、`last_publish_time`。合同测试必须检查动作定义与不可变发布快照同时存在。

## 1. AiCrudPage组件占位符格式错误


**发现日期**: 2026-05-05

**问题描述**:
使用AiCrudPage组件时，api-config配置使用了 `{id}` 花括号格式作为URL占位符，导致删除和详情接口报错"参数类型不匹配: id"。

**错误示例**:
```vue
<AiCrudPage
  :api-config="{
    detail: 'get@/api/flow/spelTemplate/{id}',   ❌ 错误
    delete: 'delete@/api/flow/spelTemplate/{id}', ❌ 错误
  }"
/>
```

**正确用法**:
```vue
<AiCrudPage
  :api-config="{
    detail: 'get@/api/flow/spelTemplate/:id',   ✅ 正确（冒号格式）
    delete: 'delete@/api/flow/spelTemplate/:id', ✅ 正确（冒号格式）
  }"
/>
```

**根本原因**:
AiCrudPage组件内部检查占位符的代码：
```js
const hasIdPlaceholder = deleteApiConfig && deleteApiConfig.includes(':id')
const hasRowKeyPlaceholder = deleteApiConfig && deleteApiConfig.includes(`:${props.rowKey}`)
```

组件只识别 **`:id`**（冒号格式），不识别 **`{id}`**（花括号格式）。

**解决方案**:
所有AiCrudPage的api-config配置，URL占位符必须使用 **冒号格式** (`:id`、`:dictId` 等)，不能用花括号格式 (`{id}`)。

**影响范围**:
- 所有使用AiCrudPage组件的CRUD页面
- 删除接口、详情接口、更新接口等带ID参数的接口

---

## 2. 分页参数名不一致导致分页失效


**发现日期**: 2026-05-05

**问题描述**:
前端点击第二页，加载的还是第一页数据。原因是前端传的参数名和后端接收的参数名不一致。

**错误示例**:
```java
// 后端Controller（错误）
@GetMapping("/page")
public RespInfo getPage(
    @RequestParam(defaultValue = "1") Integer page,      ❌ 用的是page
    @RequestParam(defaultValue = "10") Integer pageSize) {
    ...
}
```

**正确用法**:
```java
// 后端Controller（正确）
@GetMapping("/page")
public RespInfo getPage(
    @RequestParam(defaultValue = "1") Integer pageNum,   ✅ 用的是pageNum
    @RequestParam(defaultValue = "10") Integer pageSize) {
    ...
}
```

**根本原因**:
- 前端 AiCrudPage 组件传的是 `pageNum` 和 `pageSize`
- 项目标准 `PageQuery` 基类用的是 `pageNum` 和 `pageSize`
- 如果Controller用 `page`，前端传的 `pageNum` 参数不会被接收，导致始终使用默认值1

**解决方案**:
所有分页接口的Controller，参数名必须使用 **`pageNum`** 和 **`pageSize`**，与前端和项目标准保持一致。

**影响范围**:
- 所有使用AiCrudPage组件的CRUD页面
- 所有分页查询接口

---

## 6. Vite Outdated Optimize Dep 导致动态路由模块加载失败


**发现日期**: 2026-05-26

**问题描述**:
前端新增较重依赖（例如 `element-plus`、`@form-create/designer`、`@form-create/element-ui`）后，浏览器控制台出现：

```text
Failed to load resource: the server responded with a status of 504 (Outdated Optimize Dep)
TypeError: Failed to fetch dynamically imported module: http://localhost:3000/src/views/flow/model.vue
```

**根本原因**:
Vite dev server 的 `node_modules/.vite` 预构建缓存与浏览器中已加载的依赖图不一致。动态路由模块本身可能没有语法错误，但它依赖的新包触发了重新预构建，旧页面继续请求已失效的优化依赖 URL。

**解决方案**:
1. 停止旧的 Vite dev server。
2. 删除 `forge-admin-ui/node_modules/.vite`。
3. 将新引入的大型运行时依赖加入 `vite.config.js` 的 `optimizeDeps.include`。
4. 使用 Node 20.19.0 重新启动前端 dev server，必要时加 `--force`。

---

## 7. 业务记录选择器空 DTO 导致“选择器缺少业务对象编码”


**发现日期**: 2026-07-04

**问题描述**:
低代码表单设计器、动态渲染页面或关联关系配置进入后，请求 `/ai/business/selector/query?pageSize=100&pageNum=1` 时后端日志里的 `BusinessRecordSelectorQueryDTO` 全字段为空，最终抛出“选择器缺少业务对象编码”。

**根本原因**:
该接口规范是 `POST /ai/business/selector/query?pageNum=1&pageSize=100`，分页参数走 query，但业务对象编码必须在 JSON body 中传 `objectCode`，或传后端兼容的 `businessObjectCode`、`targetObjectCode`、`referenceObjectCode` 等 alias。前端如果把选择器配置当成普通远程选项接口，只拼 URL 和分页 query，没有把 `optionSource.params`、`recordSelector`、`referenceObjectCode` 等配置注入 body，就会得到空 DTO。

**正确做法**:
前端统一走 `queryBusinessRecordSelector(data, params)`，请求前先用 `resolveBusinessRecordSelectorObjectCode()` 从 `props/basicProps/optionSource/referenceConfig/recordSelector` 等位置解析对象编码，并写入 body：

```js
queryBusinessRecordSelector({
  objectCode,
  businessObjectCode: objectCode,
  targetObjectCode: objectCode,
  keyword,
  displayFields,
  keywordFields,
}, { pageNum: 1, pageSize: 100 })
```

**排查要点**:
- 源码里不应再出现 `post@/ai/business/selector/query?pageSize=100&pageNum=1` 这类硬编码调用。
- 浏览器 Network 里该请求可以带 `pageNum/pageSize` query，但 Request Payload 必须包含 `objectCode`。
- 如果修改后仍看到空请求，优先重启 Vite dev server 或清理 `node_modules/.vite` 缓存，避免旧优化缓存继续服务。

---

## 7. 动态菜单路径高亮必须支持路由参数匹配


**发现日期**: 2026-05-31

**问题描述**:
动态 CRUD 菜单打开 `/ai/crud-page/crm_customer` 后页面正常渲染，但子级菜单选中态只闪一下或不明显。

**根本原因**:
菜单高亮逻辑如果只做 path 精确匹配，无法兼容 `/ai/crud-page/:configKey` 这类动态路由配置，也容易让通用渲染页和具体业务页抢选中态。

**解决方案**:
菜单 path 匹配应统一走工具方法，支持 `:param` 动态段，并保持“精确路径优先于动态路径”。

---

## 8. form-create 默认字段名不能沉淀为低代码业务字段编码


**发现日期**: 2026-06-13

**问题描述**:
低代码表单设计器拖入默认输入组件后，form-create 可能给 rule 写入 `field: 'input'`、`select` 等组件类型默认值。如果转换 Forge 表单 Schema 时直接把这些值当成业务字段编码，会在字段资产或表结构同步时生成 `columnName = input`，重复拖入组件后容易报“数据库列名重复: input”。

**解决方案**:
`formCreateToForge` 转换时必须识别并忽略 `input`、`textarea`、`select`、`dictSelect` 等设计器内部默认字段名；对通用标题“输入框”等生成 `fieldInput1` 这类稳定业务字段编码。绑定已有字段资产时，组件标题应优先使用字段资产 `fieldName` / `label`，不要保留默认标题。

---

## 9. 移除前端依赖后必须同步清理 Vite optimizeDeps


**发现日期**: 2026-06-20

**问题描述**:
流程设计器从 `bpmn-js` 迁移到自研 DingFlow 组件后，`package.json` 已移除 `bpmn-js`、`dagre`、`diagram-js`、`inherits-browser`、`tiny-svg` 等依赖，但 `forge-admin-ui/vite.config.js` 的 `optimizeDeps.include` 仍保留这些包。`pnpm build` 可以成功，但 `pnpm dev` 会在预构建阶段报错：

```text
Failed to resolve dependency: bpmn-js/lib/Modeler, present in client 'optimizeDeps.include'
Failed to resolve dependency: dagre, present in client 'optimizeDeps.include'
```

**解决方案**:
删除依赖时必须同时全局搜索 `vite.config.js` / `optimizeDeps.include` / `pnpm-lock.yaml` 中的残留引用。尤其是 Vite dev server 失败而 build 正常时，优先检查 `optimizeDeps.include` 是否还包含已移除依赖。

**影响范围**:
- 前端依赖清理、组件替换、移除大型运行时依赖后的本地开发启动。
- 流程设计器、低代码设计器等历史上加入过 `optimizeDeps.include` 的重依赖模块。

**根本原因**:
目录型菜单本身只是分组，不一定存在可渲染页面；如果直接用目录 path 跳转，会触发无匹配路由兜底逻辑。

**解决方案**:
点击 `module` 类型且有子菜单的目录时，应该跳转到第一个有效子菜单，而不是直接跳转目录自身 path。

纯 `top-menu` 布局没有左侧二级菜单承载下级项，下拉菜单数据应将中间 `module` 目录透传/扁平化到实际页面菜单，否则用户只能看到目录名，看不到可点击子菜单。

菜单工具处理后的 `icon` 可能已经是 Naive UI render 函数，二次加工菜单数据时不能再传给 `IconRenderer`；只有字符串图标值才应该包装成 `h(IconRenderer, { icon })`。

Naive UI 横向菜单的 `responsive` 会把溢出的顶级菜单折进“...”里；动态多级菜单在该折叠层下容易看不到子级。纯顶部菜单布局应禁用 `responsive`，改用带左右箭头的横向滚动承载溢出项，避免用户找不到被挤到右侧的菜单。

---

## 9. 前端全量构建 Node 默认堆内存不足


**发现日期**: 2026-05-30

**问题描述**:
`pnpm --dir forge-admin-ui build` 在 `rendering chunks` 阶段可能触发 Node OOM：

```text
FATAL ERROR: Ineffective mark-compacts near heap limit Allocation failed - JavaScript heap out of memory
```

**解决方案**:
使用 Node 20.19.0 并提高堆内存后重跑：

```bash
source ~/.nvm/nvm.sh && nvm use v20.19.0
NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build
```

**补充说明**:
构建过程中可能仍出现 UnoCSS 图标解析和 chunk size warning，当前不影响构建成功。

**验证方式**:
直接请求动态模块和优化依赖，确认返回 200：

```bash
curl -I http://localhost:3000/src/views/flow/model.vue
curl -I http://localhost:3000/node_modules/.vite/deps/@form-create_designer.js
```

---

## 10. fcDesigner 不能在窄栅格里被动压缩


**发现日期**: 2026-05-31

**问题描述**:
低代码对象设计器接入 `@form-create/designer` 后，如果外层业务设计区仍保留空的右侧栅格列，或让 `fcDesigner` 根容器随父容器无限收缩，1024px 宽度下中间表单画布会被挤到很窄，字段标题和输入框文字出现竖排、遮挡。

**根本原因**:
`fcDesigner` 自身由左侧组件区、中间画布、右侧属性区组成，左侧约 266px，右侧约 320px。外层再额外预留 300px 空列时，真正留给设计器的宽度不足，Element/FormCreate 内部不会自动切换为可用的移动布局，而是继续压缩中间画布。

**解决方案**:
- 业务对象表单设计页不要给主 `fcDesigner` 容器保留空的第二栅格列。
- `fcDesigner` 外层画布容器设置 `overflow: auto`。
- `._fc-designer` / `.fc-designer` 设置稳定 `min-width`，窄屏通过画布内部横向滚动承载完整设计器。
- 移动端只允许局部设计器滚动，不让 body 出现横向溢出。

**影响范围**:
- `BusinessFormCreateDesigner.vue`
- `BusinessFormDesigner.vue`
- 其他复用 `@form-create/designer` 且嵌入后台工作台的页面

---

## 11. fcDesigner 布局组件不能当业务字段处理


**发现日期**: 2026-05-31

**问题描述**:
表单优先对象设计器中拖入 `fcRow`、`col`、`elCard`、`elTabs`、`elCollapse`、`elDivider` 等布局/辅助组件后，保存再进入会退化成普通输入框，运行态编辑页也不会按设计器布局渲染。

**根本原因**:
form-create rule 转 Forge `FormDesignerSchema` 时，如果布局节点没有显式字段，会根据标题自动生成 fieldCode，并按普通字段绑定保存。再次回显到 fcDesigner 时，未知 componentKey 又被降级成 input。

**解决方案**:
- 布局/辅助组件必须归一为 `fieldBinding.mode = virtual`，禁止进入字段注册表和 DDL 同步。
- 双向转换需要保留原始 form-create `type`、`children`、`style/native/wrap` 等元数据。
- 运行态需要单独下发 `formLayout/editFormLayout`，由 `AiForm` 递归渲染布局节点；扁平 `editSchema` 只适合作为字段配置。

---

## 7. 树形低代码模板 beforeRenderForm 丢失行主键导致详情 URL 拼出 undefined


**发现日期**: 2026-05-23

**问题描述**:
低代码动态渲染页点击编辑/查看详情时报错类似 `参数类型不匹配: id .../undefined`，但删除操作正常。原因是删除直接从表格行取 `rowKey`，而树形 CRUD 模板会给 `AiCrudPage` 注入 `beforeRenderForm`；如果该钩子编辑时返回 `{}` 或返回对象不含主键，详情加载会用加工后的行取 ID，导致 `:id` 被替换成 `undefined`。

**解决方案**:
- 树形模板的 `beforeRenderForm` 编辑场景必须从原始行数据开始合并钩子结果，不能直接返回空对象。
- `AiCrudPage` 加载详情、删除、更新和自定义动作路径统一通过 `rowKey`/`id`/`Id` 兜底解析主键。
- URL 占位符替换时跳过 `null`、`undefined`、空字符串以及字符串 `"undefined"`/`"null"`，避免拼出非法详情路径。

**影响范围**:
- `TreeCrudTemplate` 注入 `beforeRenderForm` 的低代码树形应用
- 动态 CRUD 的编辑、查看详情、自定义行操作跳转

## 7. Naive UI 当前版本不导出 NSegmented


**发现日期**: 2026-05-19

**问题描述**:
在低代码页面搭建器中使用 `<n-segmented>` 后，`pnpm build` 失败：

```text
"NSegmented" is not exported by "naive-ui/es/index.mjs"
```

**解决方案**:
当前项目 Naive UI 版本下不要使用 `n-segmented`。需要分段切换效果时，使用项目已支持的：

```vue
<n-radio-group v-model:value="value" size="small">
  <n-radio-button value="simple-crud">标准单表</n-radio-button>
  <n-radio-button value="tree-crud">左树右表</n-radio-button>
</n-radio-group>
```

**影响范围**:
- 所有新增 Vue 页面或组件中需要“分段选择器”的场景。
- 前端生产构建 `pnpm build`。

## 6. 外部接口代理返回二层加密壳导致图表数据异常


**发现日期**: 2026-05-11

**问题描述**:
报表设计器通过外部接口代理调用 Forge 系统接口时，前端外层响应已解密，但代理返回的业务数据仍可能是目标系统响应的 `{data, algorithm}` 加密壳，ECharts 会拿到密文对象并报 `Invalide sourceFormat: unknown`。

**根本原因**:
`forge-plugin-external` 代理只是转发并解析目标接口响应，没有和目标 Forge 系统做独立密钥交换，也没有对目标系统返回的加密响应做二次解密。

**解决方案**:
代理服务调用外部 Forge 接口前先尝试 `/crypto/public-key` + `/crypto/exchange`，用目标系统会话密钥加密请求体，并在代理层解密目标系统返回的 `{data, algorithm}`，前端图表只接收明文业务数据。

**影响范围**:
- 报表动态请求选择外部 Forge 接口
- 外部接口代理调试
- 所有通过 `ExternalProxyService` 转发到启用 API 加解密服务的场景

---

## 6. AiCrudPage 表格列配置不能直接使用 Naive UI title/key


**发现日期**: 2026-05-11

**问题描述**:
外围系统/接口管理页面列表接口有数据，但表格字段显示为空。

**错误示例**:
```javascript
const tableColumns = [
  { title: '系统名称', key: 'systemName' }
]
```

**正确用法**:
```javascript
const tableColumns = [
  { label: '系统名称', prop: 'systemName' }
]
```

**根本原因**:
`AiCrudPage` 会再交给 `AiTable` 转换列配置，默认渲染逻辑读取 `row[col.prop]`。只配置 `key` 时，列标题能显示，但默认单元格取值会变成 `row[undefined]`，最终显示 `-` 或空值。

**解决方案**:
所有 `AiCrudPage` 的表格列统一使用 `label/prop`；只有直接使用 Naive `NDataTable` 时才使用 `title/key`。

**影响范围**:
- 所有基于 `AiCrudPage` 的 CRUD 页面
- AI 生成或手写的前端管理页面

## 11. Naive Select/TreeSelect 回显 Long ID 必须统一字符串类型


**发现日期**: 2026-05-16

**问题描述**:
数据集私有访问模式下，选择角色、用户或组织授权主体时页面可能闪退。后端 Long ID 可能被序列化成字符串，前端选项值却可能仍是 number，`NSelect` / `NTreeSelect` 在回显、过滤或追加缺失选项时出现值类型不一致。

**解决方案**:
前端用于选择器的 Long ID 统一通过 `String(id)` 归一化，包含：
- 列表选项 `value`
- 树选项 `value/key`
- 详情回显的 `subjectId`
- `appendMissingOption` 和树节点 contains 判断

提交给后端时保留字符串 ID，Jackson 可以反序列化为 `Long`，同时避免 JS 大整数精度问题。

如果选择器位于 `n-modal` 内的复杂表单卡片中，还要避免父级 hover `transform` 影响 Naive 弹层定位：
- 权限卡片所在 `n-form-item` 不要做 `translateY` 这类 hover 位移
- ACL 下拉可设置 `:to="false"`，禁用 teleport，避免弹层挂到 modal body 后被滚动容器/层级影响
- 异步加载选项时先加载完成再插入授权行，避免打开下拉时选项刷新导致弹层闪退

如果字段是在 `AiCrudPage` 的自定义 slot 中维护，不能只在 slot 内直接 `v-model` 修改 `formData` 嵌套属性。`AiForm` 内部使用表单副本，slot 裸改不会同步到父级 `formData`，父级重渲染后会用旧值覆盖，表现为“私有模式跳回公开”。slot 内需要使用 `updateValue` 触发表单整体 `update:value`，或改成显式 `:value + @update:value` 后同步。

**影响范围**:
- 所有后端 Long ID 被用作 Naive UI 选择器值的页面
- 数据集 ACL、用户/角色/组织选择器、树选择器回显场景
- `AiCrudPage` 自定义 slot 内维护非当前字段值的场景

## 12. AiCrudPage 编辑嵌套明细不回显


**发现日期**: 2026-05-17

**问题描述**:
使用 AiCrudPage 做主表列表时，如果编辑表单依赖子表、绑定表、字段清单等嵌套明细，仅把列表行传给 `showEdit(row)` 会导致编辑弹窗里子数据不回显。列表接口通常只返回概要字段，不包含完整明细数组。

**解决方案**:
需要在页面上开启 `:load-detail-on-edit="true"`，并配置 `detail: 'get@/xxx/:id'`。详情接口返回的数据再通过 `beforeRenderDetail` 归一化为表单需要的结构。

下拉回显还要保留已绑定项的名称、编码等显示字段；如果当前选项列表没有该值，应把当前绑定项临时合并进 select options，避免只显示 ID。

**影响范围**:
- 业务定义绑定数据集
- 数据集字段、权限、行级权限等主从表编辑表单
- 所有依赖详情接口回显嵌套数组的 AiCrudPage 页面

## 18. AiForm 树选择必填校验误判已选 ID 为空


**发现日期**: 2026-05-24

**问题描述**:
低代码树形单表中，父级分类字段使用 `treeSelect` 后，表单里已经选择了父级 ID，提交时仍提示“请选择父级分类ID”。

**根本原因**:
运行时 schema 会给必填字段生成 `rules: [{ required: true, ... }]`。`AiForm.vue` 之前只对 `number` 和日期类型做自定义必填校验，`treeSelect`、`orgTreeSelect`、`cascader` 等选择型组件仍走默认 `required` 判断，数字 ID、`0`、数组等有效值存在被误判为空的风险。

**解决方案**:
`AiForm.vue` 的 `normalizeFieldRules()` 和自动生成规则逻辑中，选择型字段也统一使用 `hasFormValue()` 校验，支持数字 ID、`0`、数组和对象值，只把 `null`、`undefined`、空字符串、空数组判为空。

**影响范围**:
- 低代码编辑表单的 `treeSelect` / `orgTreeSelect` / `regionTreeSelect`
- 自定义查询和普通动态表单中的选择型必填字段

## 19. 旧技能示例与 AGENTS.md 规则冲突


**发现日期**: 2026-05-26

**问题描述**:
`.agents/skills/forge-coding-standards/SKILL.md` 中仍存在旧示例，例如 AiCrudPage 使用 `{id}` 占位符、建表模板 `tenant_id DEFAULT 0`。这些示例与当前 `AGENTS.md` 中的强制规则冲突。

**解决方案**:
- 生成或审查 Forge 代码时，以仓库根目录 `AGENTS.md` 为最高优先级。
- AiCrudPage URL 占位符必须使用 `:id` / `:${rowKey}`，禁止 `{id}`。
- 业务内置数据、字典、资源脚本的 `tenant_id` 必须为 `1`，禁止写 `0`。

**影响范围**:
- 使用旧 `forge-coding-standards` 技能示例生成的前端 CRUD 页面
- Flyway 字典、资源、业务内置数据脚本

## 22. 目录式动态路由生成空父记录导致 Component 为 null


**发现日期**: 2026-05-27

**问题描述**:
进入 `/app-center/suite/:suiteCode` 或 `/app-center/object/:objectCode` 时，浏览器报错：

```text
runtime-core.esm-bundler.js:6902 Uncaught (in promise) TypeError: Cannot read properties of null (reading 'component')
```

**根本原因**:
`unplugin-vue-router` 扫描 `src/views/app-center/suite/[suiteCode].vue` 会生成无组件中间记录 `/app-center/suite`，再把 `:suiteCode` 作为子路由。根 `RouterView` 在当前布局和 KeepAlive 包裹下可能拿到空组件，导致 Vue 渲染 `<component :is="Component">` 时异常。

**解决方案**:
使用插件默认开启的 `dotNesting` 扁平动态文件命名，避免额外空父记录：

```text
src/views/app-center/suite.[suiteCode].vue   -> /app-center/suite/:suiteCode
src/views/app-center/object.[objectCode].vue -> /app-center/object/:objectCode
```

同步更新 `sys_resource.component` / Flyway 脚本里的组件路径，例如 `app-center/suite.[suiteCode]`，不要回退到手写路由长期兜底。

**影响范围**:
- `forge-admin-ui` 中由 `unplugin-vue-router` 自动扫描的二级及更深动态路由
- 隐藏菜单路由的 `sys_resource.path` 和 `component` 配置

## 24. ai_crud_config 表 status 字段类型为 char(1) 导致 Flyway 迁移失败


**发现日期**: 2026-05-28

**问题描述**:
执行 `V1.0.32__seed_crm_customer_runtime_link.sql` 脚本时，Flyway 报错：
```
Data truncation: Data too long for column 'status' at row 1
```

**错误示例**:
```sql
INSERT INTO ai_crud_config (..., status, ...)
VALUES (..., 'ENABLED', ...);  -- ❌ 错误，'ENABLED' 是 7 个字符
```

**正确用法**:
```sql
INSERT INTO ai_crud_config (..., status, ...)
VALUES (..., '0', ...);  -- ✅ 正确，'0' 表示启用
```

**根本原因**:
`ai_crud_config` 表的 `status` 字段定义是 `char(1)` 类型，只能存储 1 个字符：
```sql
`status` char(1) NOT NULL DEFAULT '0' COMMENT '状态（0启用 1停用）',
```

而 `ai_lowcode_domain`、`ai_lowcode_model` 等表的 `status` 字段是 `varchar(16)` 类型，可以存储 `'ENABLED'`、`'DISABLED'` 等值。不同表的 `status` 字段类型不一致，容易混淆。

**解决方案**:
- `ai_crud_config.status` 使用 `'0'`（启用）或 `'1'`（停用）
- `ai_lowcode_domain.status`、`ai_lowcode_model.status` 使用 `'ENABLED'` 或 `'DISABLED'`
- 编写 Flyway 脚本前，先检查目标表的字段类型定义

**影响范围**:
- 所有向 `ai_crud_config` 表插入数据的 Flyway 脚本
- 涉及 `status` 字段的 UPDATE 语句

**修复步骤**:
如果 Flyway 迁移已失败，需要手动删除失败记录：
```sql
-- 检查失败的迁移记录
SELECT * FROM forge_schema_history WHERE success = 0;

-- 删除失败的迁移记录
DELETE FROM forge_schema_history WHERE version = '1.0.32';

-- 修复脚本后重新启动应用，Flyway 会自动重新执行
```

## 27. Vue 属性面板 watcher immediate 必须处理空选中项


**发现日期**: 2026-05-29

**问题描述**:
业务对象字段属性面板在页面加载或字段选中状态切换为空时，`watch(..., { immediate: true })` 会立即调用表单重置逻辑。如果 `createFieldForm(field)` 直接读取 `field.fieldName`，会报：
```text
Cannot read properties of null (reading 'fieldName')
```

**解决方案**:
属性面板和类似“左列表 + 右属性”的组件，表单构造函数必须先把空值归一化：
```js
function createFieldForm(field) {
  const currentField = field || {}
  return {
    fieldName: currentField.fieldName || '',
    // ...
  }
}
```

**影响范围**:
- `BusinessFieldPropertyPanel.vue`
- 所有依赖 `watch` immediate 初始化、且允许空选中项的属性面板

## 28. 前端路由 query 中的雪花 ID 禁止转 Number


**发现日期**: 2026-05-29

**问题描述**:
业务对象 ID 是 19 位雪花 ID，前端从 `route.query.objectId` 读取后如果执行 `Number(route.query.objectId)`，会发生 JS 安全整数精度丢失，导致接口路径 ID 错误，设计器加载/发布会指向不存在的对象。

**解决方案**:
路由参数、query 参数和 API path 中的 Long ID 在前端保持字符串传递，只有明确用于数值计算且小于安全整数时才转 Number。

**影响范围**:
- `object-designer.[objectCode].vue` 等从 query 读取对象 ID 的页面
- 所有 18/19 位雪花 ID 前端传参链路

## 29. 业务对象设计器同步页面 Schema 必须保留 modelRefs


**发现日期**: 2026-05-30

**问题描述**:
关系配置开启“编辑表单维护”后，后端会把关联对象写入 `pageSchema.modelRefs`，并把子对象字段引用加入编辑区。如果前端表单、列表或详情设计器只用主对象 `modelSchema.fields` 调用 `syncPageSchemaWithModel`，后续保存布局会把子对象字段引用过滤掉，导致主子表运行态没有 `childrenConfig`。

**解决方案**:
业务对象设计器在同步页面 Schema 时必须基于 `buildPageDesignModelSchema(modelSchema, pageSchema.modelRefs)` 构造设计态字段集合；同时用最新主对象字段刷新 primary model ref，避免新增字段后 primary ref 过期。

**影响范围**:
- `BusinessFormDesigner.vue`
- `BusinessListDesigner.vue`
- `BusinessDetailDesigner.vue`
- 所有依赖 `pageSchema.modelRefs` 的主子表、左树右表或多模型页面设计器

## 30. 业务对象发布前必须重新合并关系到 pageSchema


**发现日期**: 2026-05-30

**问题描述**:
关系配置已保存后，前端发布请求仍可能携带旧的 `pageSchema` 草稿。如果后端直接采用请求里的 `pageSchema`，会覆盖关系同步生成的 `modelRefs` 和 `master-detail-crud` 布局，发布后的动态 CRUD 仍只显示主表字段，新增/编辑看不到联系人、明细等关联对象。

**解决方案**:
业务对象发布检查和发布动作在生成运行配置前，必须以数据库中的当前关系为准重新执行关系同步，把 `ai_business_object_relation` 合并回 `LowcodeModelSchema.relations` 和 `LowcodePageSchema.modelRefs`。

**影响范围**:
- `BusinessObjectPublishService.publishCheck`
- `BusinessObjectPublishService.publish`
- 所有从业务对象关系生成动态 CRUD 子表/明细表单的发布链路

## 31. 运行态字段组件变更必须覆盖主表表单和子表明细


**发现日期**: 2026-05-30

**问题描述**:
业务对象字段类型为“人员”时，运行态主表表单使用 `AiFormItem` 渲染，主子表内联新增明细使用 `ChildTableEditor` 渲染。只在 `AiFormItem` 支持 `userSelect` 会导致客户页新增跟进记录这类子表明细退化成普通输入框或旧下拉，负责人无法弹出用户筛选列表。

**解决方案**:
字段组件能力要同时覆盖：
- `AiFormItem.vue`：主表新增/编辑/搜索表单
- `ChildTableEditor.vue`：主子表关联明细新增/编辑

人员字段应使用弹窗式用户列表选择组件，不能用远程下拉承载大量用户。

**影响范围**:
- `userSelect` / 人员字段
- 所有主子表、关联明细、动态 CRUD 运行态表单

## 32. 设计器部分保存 DTO 禁止默认空集合


**发现日期**: 2026-05-31

**问题描述**:
业务对象设计器多个面板都会调用 `PUT /ai/business/object/{objectId}/designer` 做部分保存。如果后端 DTO 把 `relations` 这类集合字段默认初始化为 `new ArrayList<>()`，Jackson 在请求未传该字段时仍会保留空集合，Service 的 `dto.getRelations() != null` 会误判为“用户明确保存空关系”，从而清空已有对象关系。

**解决方案**:
用于 PATCH/部分保存语义的 DTO 集合和 Map 字段保持 `null` 默认值，用 `null` 表示“不更新该配置”，用显式 `[]` 表示“清空该配置”。前端通用草稿保存也不要携带关系配置，关系应只由关系面板专门保存。

**影响范围**:
- `BusinessObjectDesignerDTO`
- `BusinessObjectDesignerService.saveDesigner`
- `object-designer.[objectCode].vue`
- 所有通过 designer 聚合接口做局部保存的表单、列表、详情、关系和高级配置面板

## 33. 表单优先设计保存必须同步运行态 fieldSettings


**发现日期**: 2026-05-31

**问题描述**:
业务对象表单设计器保存时如果只写回 `formCreateRule/formCreateOptions`，后端 `saveDesigner` 虽然会把 `FormDesignerSchema` 编译到编辑区 `fieldSettings`，但随后 `saveBusinessObjectFormLayout` 可能用前端旧 pageSchema 覆盖掉编译结果。运行态 `AiCrudPage` 最终读取到的字段顺序、span、labelWidth、align、label 就会和设计态不一致。

**解决方案**:
前端保存表单设计时也要把 `FormDesignerSchema.components` 编译为 `editZone.props.fieldSettings`，并同步 `fieldRefs`、`editGridCols`、`labelPlacement`、`labelWidth`。编译时只替换主表字段设置，必须保留关系字段设置。

**影响范围**:
- `BusinessFormDesigner.vue`
- `BusinessObjectDesignerService.applyFormDesignerSchemaToEditZone`
- `LowcodeRuntimeConfigBuilder.buildEditSchema`
- 所有表单优先业务对象运行态新增/编辑/详情表单

## 34. 组织树 optionSource 为空时必须回退默认数据源


**发现日期**: 2026-05-31

**问题描述**:
表单设计器中的组织/部门树组件可能保存空 `optionSource` 或没有 `api` 的中间属性。运行态如果只判断 `field.props.optionSource` 是否存在，就会跳过默认 `/system/org/tree`，导致组织列表不渲染。

**解决方案**:
运行态解析远程选项源时必须判断 `optionSource` 是否有效；只有存在 `api/url` 或真实静态选项时才使用配置源。组织树组件遇到空配置要回退默认系统组织树接口，并对 `RespInfo.data`、分页 records/list/rows 和嵌套 data 做统一解包。

**影响范围**:
- `AiFormItem.vue`
- 所有动态 CRUD 中的 `orgTreeSelect`、`deptTreeSelect`、`elTreeSelect` 等组织/部门选择别名

## 35. 表单设计新增字段需要触发受控 DDL 同步


**发现日期**: 2026-05-31

**问题描述**:
表单优先设计器拖入新字段后，如果只保存 `LowcodeModelSchema.fields` 和表单布局，运行态新增/编辑提交时数据库物理表仍缺少对应列，最终出现保存失败或字段数据无法落库。

**解决方案**:
表单设计器保存时在字段注册表更新后请求同步 DDL；后端使用已有 `LowcodeDdlService.previewCreateTable/executeCreateTable` 执行受控 CREATE/ALTER，且必须继续校验 `ai:lowcode:deploy-ddl` 权限和二次确认标记。不要在前端直接拼 SQL。

**影响范围**:
- `BusinessFormDesigner.vue`
- `BusinessObjectDesignerDTO`
- `BusinessObjectDesignerService.saveDesigner`
- `LowcodeDdlService`

## 36. 跳转桥接路由不能登记顶部 Tab


**发现日期**: 2026-05-31

**问题描述**:
应用入口菜单挂载到 `/app-center/app/:appId` 这类桥接页时，页面会先进入桥接路由，再 `router.replace` 到真实运行态页面。如果 tab guard 对桥接页也登记 tab，顶部会同时出现“应用入口”和真实业务页两个 tab，用户关闭真实业务页时还会被桥接 tab 干扰。

**解决方案**:
桥接路由、全屏设计器路由这类非最终业务页面应在路由 meta 中标记 `skipTab`，tab guard 遇到后直接跳过并清理同路径遗留 tab。真实动态 CRUD 页需要按运行态页面自身登记 tab，并在唯一业务 tab 场景下也允许关闭。

**影响范围**:
- `router/index.js`
- `router/guards/tab-guard.js`
- `store/modules/tab.js`
- 顶部 tab 组件
- 所有桥接跳转、全屏弹层式设计器和动态 CRUD 运行态页面

## 37. fcDesigner 画布列数必须写入 rule.col.span


**发现日期**: 2026-06-01

**问题描述**:
低代码对象表单设计中已保存 `FormDesignerSchema.layout.gridColumns`，但 fcDesigner 画布仍然按单列展示；拖入 `row/col` 后再次回显，列宽还可能被撑成整行。

**根本原因**:
fcDesigner / form-create 的画布宽度看每个 rule 的 `col.span`，不是看 Forge 自定义的 `gridColumns`。同时 `col.props.span` 是 form-create 的 24 栅格值，不能直接当成 Forge 的 1/2/3 业务列跨度。

**解决方案**:
切换单列/两列/三列、字段追加、字段重置、form-create rule 转 Forge schema 时，都必须统一重算组件 span：

- 单列：普通字段 `col.span=24`
- 两列：普通字段 `col.span=12`
- 三列：普通字段 `col.span=8`
- `col` 布局组件的 `props.span` 也要按当前业务列数换算
- 布局容器、分割线、标题、子表等整行组件应跨满当前业务列数

**影响范围**:
- `BusinessFormDesigner.vue`
- `BusinessFormCreateDesigner.vue`
- `formDesignerSchema.js`
- `forgeToFormCreate.js`
- `formCreateToForge.js`

## 38. fcDesigner 删除组件不会自动删除字段资产


**发现日期**: 2026-06-01

**问题描述**:
表单优先设计器中删除字典、级联、引用等组件后，保存仍可能提示“字典字段必须配置字典类型”或“引用对象字段必须配置目标对象和回显字段”。

**根本原因**:
fcDesigner 删除的是画布 rule / `FormDesignerSchema.components`，字段资产注册表仍保留历史字段。表单保存会把全部主表业务字段提交给后端，如果旧字段还是 `DICT/SELECT/RADIO/CHECKBOX/MULTI_SELECT/REFERENCE` 且缺少配置，后端字段校验会继续拦截。

**解决方案**:
保存表单前必须基于当前画布组件映射归一化字段资产：

- 字段已不在画布且字典/引用配置不完整时，降级为普通 `TEXT/input/like`
- 字段在画布上已从字典/引用改成普通输入时，也要同步降级并清理 `dictType/referenceObjectCode/referenceDisplayField`
- 后端 `BusinessObjectDesignerService.saveDesigner` 也要在重建模型字段前做兜底，不能只依赖前端

**影响范围**:
- `BusinessFormDesigner.vue`
- `BusinessObjectDesignerService.saveDesigner`
- 表单优先字段资产、字典/级联/引用组件保存链路

## 39. fcDesigner 布局组件的 ref_ 临时值不能进入 Forge Schema


**发现日期**: 2026-06-01

**问题描述**:
表单设计器拖入栅格布局、栅格列等布局组件后，画布组件标题前可能出现 `ref_Fs5x...` 这类随机字符串。

**根本原因**:
fcDesigner / form-create 会给布局 rule 生成 `ref_...` 临时 `id/name/title`。布局组件不是业务字段，如果 form-create 转 Forge schema 时把这些临时值当作布局标题或组件 id 保存，回写画布时就会展示出来。

**解决方案**:
布局组件标题归一化时要剥离 `ref_...`；旧 schema 中保存的临时布局 id 要替换为稳定 `cmp_<componentKey>_<index>`；Forge schema 回写 form-create rule 时，非字段布局组件不要写 `name`。

**影响范围**:
- `formDesignerSchema.js`
- `formCreateToForge.js`
- `forgeToFormCreate.js`
- fcDesigner 布局组件保存和回显

## 40. 业务对象编码推理只应自动作用于新建


**发现日期**: 2026-06-01

**问题描述**:
给新建业务对象增加中文名称到对象编码的自动推理时，如果后端更新接口也无条件把 `objectCode` 归一化为 lower_snake，会把历史对象（如 CRM 样板中的 `CUSTOMER`）在普通编辑保存时改成 `customer`，导致关系、应用入口、菜单路由或历史数据引用不一致。

**根本原因**:
`ai_business_object.object_code` 既是用户可维护编码，也是关系和入口绑定键。历史数据中同时存在大写业务对象编码和 lower_snake 运行态 `model_code/config_key`。对象编码推理是“创建默认值”能力，不应在更新已有对象时强制迁移主键式编码。

**解决方案**:
- 新建对象时可以根据中文对象名推理 lower_snake `objectCode`，并生成 `suite_object` 风格 `modelCode`。
- 更新对象时保留原有 `objectCode/modelCode`，除非请求明确传入新值。
- 如果要迁移历史对象编码，必须走单独数据修复脚本，并同步关系、应用入口、权限和菜单引用。

## 41. Flyway 会扫描注释和字符串中的占位符


**发现日期**: 2026-06-02

**问题描述**:
Flyway 执行 SQL 脚本时会处理 `${...}` 占位符，即使它出现在 SQL 字符串或注释里也可能触发 placeholder 解析。低代码 seed 脚本中如果把消息模板、流程标题模板、编码规则模板写成 `${field}`、`WL${yyyyMMddHHmmss}${seq:3}` 这类字符串，会被 Flyway 当成配置占位符，导致迁移失败。

典型错误：

```text
No value provided for placeholder: ${yyyyMMddHHmmss}. Check your configuration!
```

**解决方案**:
- Flyway 脚本中的内置模板优先使用 `{field}` 这类不触发 Flyway placeholder 的格式；运行时模板引擎可以兼容 `{field}` 和历史 `${field}`。
- 如果业务必须保留 `${...}` 入库，SQL 原文不要出现连续的 `${`，用 SQL 拼接生成最终值，例如 `CONCAT('WL', '$', '{yyyyMMddHHmmss}', '$', '{seq:3}')`。
- 不建议为单个业务模板关闭或修改全局 Flyway placeholder 行为，避免影响现有配置约定。
- Flyway 脚本变更后必须执行静态检查：`rg -n '\$\{[^}]+\}' forge-server/db/migration`，结果应为空。

**影响范围**:
- `forge-server/db/migration/` 中所有包含 JSON、消息模板、流程标题模板、编码规则模板的 SQL 脚本
- 触发器动作配置、消息模板、流程变量标题模板、编码规则初始化脚本

## 50. form-create 随机字段 ID 未同步导致低代码页面引用不存在字段


**发现日期**: 2026-06-04

**问题描述**:
表单设计器新增组件后，form-create 会先生成类似 `Frpjmpzgzlc1hfc` 的临时字段 ID。用户还没保存表单设计，就切换到单据设置并保存时，页面区域仍引用这个临时字段，但字段资产没有同步落库，后端校验会报 `页面区域引用了不存在的字段: Frpjmpzgzlc1hfc`。

**根本原因**:
跨设计面板保存只持久化了当前面板配置，没有先把表单设计器里的草稿字段、页面 Schema 和模型字段同步出来；同时基础配置允许直接输入字段 ID，容易把 form-create 临时 `F...` 字段当成正式业务字段。后续又发现另一个同源问题：保存自动字段资产时如果用 `modelSchema.fields` 作为基准，而不是用真实 `draft.fields/props.fields` 字段资产，字段名会从完整的 `fieldName/fieldCode` 退化成页面模型里的 `field/label`，最终字段资产列表显示“未命名字段”。

**解决方案**:
表单设计器基础配置的“组件字段ID”必须优先从已有字段中选择；转换时把 form-create 自动生成的 `F...` 字段视为临时字段，不直接作为永久业务字段。`form-create -> Forge schema` 转换必须同时兼容 `rule.field/name`、`props.fieldCode`、`props.fieldBinding.fieldCode` 和根级 `fieldBinding.fieldCode`，当旧 `_forge.fieldBinding` 与新选择不一致时优先保留用户刚修改的字段。切换出表单设计面板前调用 `syncDesignerDraft()` 同步草稿字段和 Schema；单据、流程、动作等面板保存前如果存在未持久化表单草稿，先静默保存草稿且不 reload 页面，避免当前面板输入丢失。自动字段资产合并必须优先使用真实字段资产 `props.fields`，不能用页面模型 `modelSchema.fields` 反向覆盖字段资产名称。

**影响范围**:
- `BusinessFormDesigner` 面板切换和保存流程
- `form-first/formCreateToForge`
- `form-first/forgeToFormCreate`
- 表单设计器基础配置字段绑定控件
- 单据设置、流程设置、动作设置保存前的草稿同步

## 51. 对象设计保存后必须同步关联运行态入口菜单


**发现日期**: 2026-06-05

**问题描述**:
应用入口勾选“同步为菜单 + 套件作为父级目录”后，`ai_business_app.options.adminMenu` 会保存实际菜单资源、套件目录和运行态 path。对象设计器保存会重新生成或刷新业务对象运行态 `configKey`，但如果只更新 `ai_business_object.config_key`，不重新同步已关联的 `BUSINESS/RUNTIME` 应用入口，返回应用入口时就可能看不到实际挂载目录，管理端菜单也可能继续引用旧 path。

**解决方案**:
对象设计器保存运行态草稿后，必须按 `tenant_id + suite_code + object_code` 找到所有关联运行态入口，刷新入口 `configKey` 并重新执行管理端菜单同步，回写 `menuResourceId`、`activeMenuKey`、`actualParentId`、`suiteMenuResourceId`、path 和 component。应用入口自身保存时仍要保留自父级归一化保护，避免实际套件目录 ID 反写成“套件目录上级”。

**影响范围**:
- `BusinessObjectDesignerService.saveDraft`
- `BusinessAppService.syncRuntimeAppsForObject`
- `BusinessAppMapper.selectRuntimeAppsByObject`
- `ai_business_app.options.adminMenu`
- 管理端动态菜单渲染和应用入口编辑抽屉回显

## 52. 字段资产全局保存不能强制要求当前选中字段


**发现日期**: 2026-06-05

**问题描述**:
对象设计器顶部全局保存会调用字段资产面板的保存钩子。用户修改字段属性后，如果属性面板关闭或当前没有选中字段，旧逻辑直接提示“请先选择需要保存的字段”，导致整页保存被阻断，即使当前字段资产实际上没有待保存内容。

**解决方案**:
字段属性面板打开时应直接读取当前面板 payload 和字段编码保存，不依赖外层选中行仍然存在。属性面板未打开且没有选中字段时，应视为字段资产无待保存内容，返回成功并允许整页保存继续执行；只有真正保存某个字段失败时才阻断。

**影响范围**:
- `BusinessFieldManager.saveSelectedField`
- `BusinessFieldManager.saveField`
- 对象设计器全局保存流程
- 字段资产属性面板打开、关闭和切换字段场景

## 53. 表单优先 viewSchema 的 fieldCode 也是字段改名/删除引用点


**发现日期**: 2026-06-05

**问题描述**:
低代码对象字段从 form-create 临时编码（如 `Frpjmpzgzlc1hfc` / `frpjmpzgzlc1hfc`）改成正式字段名后，发布检查仍可能报 `查询条件引用了不存在字段: xxx`，但用户在查询条件 UI 里找不到这个字段。

**根本原因**:
发布检查读取的是 `ai_business_object.designer_options.viewSchema.search.fields[].fieldCode`。旧字段改名清理只处理了 `fieldRef`、`field`、`sourceField` 等键，漏掉了表单优先视图 schema 的 `fieldCode`；列表自由布局还可能在 `props.fieldSettings[*].queryField` 里保留隐藏查询映射。

**解决方案**:
字段改名/删除必须递归处理 `fieldCode` 和 `queryField`，并同步清理 `designerOptions`。读取设计器、发布检查和前端保存 payload 时，都要按当前字段资产/模型字段集过滤 `viewSchema.search/list/detail`。运行态构建搜索 schema 时，如果 `queryField` 指向不存在字段，必须回退到当前查询字段或删除该映射。

**影响范围**:
- `BusinessFieldDesignService` 字段改名/删除
- `BusinessObjectDesignerService.resolveViewSchema`
- `BusinessObjectPublishService.checkFormFirstSchemas`
- `form-first/viewSchema.js`
- `page-schema.js` 的 `fieldSettings.queryField`

## 54. flow server 直接引入 generator 插件会暴露管理端桥接依赖


**发现日期**: 2026-06-06

**问题描述**:
`forge-flow-server` 为了复用低代码动态 CRUD 落表能力直接引入 `forge-plugin-generator` 后，启动时会扫描 generator 插件的完整 Service/Controller。由于部分能力原本只在 admin server 中通过 bridge 实现，独立 flow server 会出现 `MenuRegisterAdapter`、`AiClientAdapter` 等 bean 缺失，或 optional 依赖不传递导致 `FlowClient` 类缺失。

**根本原因**:
`forge-plugin-generator` 同时包含运行态动态 CRUD、AI 生成、菜单注册、业务流程绑定等能力；独立 flow server 只需要运行态 CRUD，但组件扫描会装配更多 generator bean。`forge-flow-client` 在 generator 中是 optional 依赖，作为传递依赖不会进入 flow server 启动包。

**解决方案**:
flow server 侧为管理端专属桥接点提供明确 no-op/fallback 实现，例如 `FlowMenuRegisterAdapter`、`FlowAiClientAdapter`；对 generator 运行期会反射到的 optional 类，flow server 必须显式引入对应依赖，例如 `forge-flow-client`。验证时必须跑可执行 jar 启动，而不仅是 `compile`，因为 optional 依赖和 Spring 装配问题可能只在启动包里暴露。

**影响范围**:
- `forge-flow-server` 直接依赖 `forge-plugin-generator`
- `MenuRegisterAdapter`、`AiClientAdapter` 等 admin bridge 接口
- `forge-flow-client` optional 依赖传递
- flow server 可执行 jar 启动验证

## 56. AiCrudPage 详情态字典字段不能依赖禁用控件回显


**发现日期**: 2026-06-10

**问题描述**:
多个页面的详情弹窗或编辑弹窗里，`select/radio/checkbox/transfer` 一类字段会直接显示数字值，例如用户类型、性别、状态显示成 `1/2/0`，没有翻译成中文。表格列表通常正常，问题主要出在 `AiCrudPage` 的表单详情态。

**根本原因**:
`AiCrudPage` 的详情态不是纯文本展示，而是把 `editSchema` 转成 `readonly/disabled` 后继续渲染原表单控件。只要当前值对应的 `options` 没及时加载、被权限逻辑过滤掉，或 `number/string` 类型不一致，Naive UI 的选择类控件就会退化成原始数字值。另一个高频诱因是“为了限制可选范围直接过滤 options”，例如租户管理员只能维护普通用户时，把 `userType=1` 从当前登录用户自己的 `options` 中删掉，结果详情/编辑回显直接变成数字。

**解决方案**:
公共表单层要把详情态的字典类字段当作“文本回显”处理，优先显示 label，不要依赖禁用后的 `select/radio` 自己兜底展示。`AiFormItem.currentOptions` 对静态 `field.options`、`field.props.options`、异步 `options()`、字典和远程选项都要补齐当前值对应的 label，并对 `select/radio/checkbox/transfer` 做统一的值类型归一化，避免 `1` 和 `'1'` 对不上。做权限限制时不要把“当前值”从 options 中删掉，应该保留完整 options 并通过 `disabled`、`visible` 或提交前校验限制修改能力。

**验证建议**:
涉及公共表单、字典加载、选项过滤、租户权限裁剪的改动，必须至少验证两个层面：列表列的 `DictTag` 展示，以及 `AiCrudPage` 详情/编辑弹窗里的同字段回显。只验证列表正常不够，因为列表和弹窗走的是两条不同渲染链路。

**影响范围**:
- `forge-admin-ui/src/components/ai-form/AiCrudPage.vue`
- `forge-admin-ui/src/components/ai-form/AiFormItem.vue`
- 所有通过 `editSchema` 配置 `select/radio/checkbox/transfer` 的 CRUD 页面
- 任何带有“按权限裁剪 options”逻辑的页面，例如 `system/user.vue`

## 58. 手写/隐藏业务路由必须同步 sys_resource 和角色授权


**发现日期**: 2026-06-15

**问题描述**:
从应用中心跳转到 `/app-center/stats`、`/app-center/trigger`、`/message/template`、`/app-center/object/:objectCode` 或 `/ai/crud-page/:configKey` 这类手写/隐藏路由时，页面可能直接跳到 403，即使前端 `router/index.js` 已经注册了路由组件。

**根本原因**:
前端权限守卫不只看 Vue Router 是否存在目标路由，还会用 `/auth/current/menu` 返回的 `sys_resource` 菜单资源生成 `permissionStore.accessRoutes` 作为路由 allowlist。手写路由、动态参数路由和隐藏运行态路由如果没有对应 `sys_resource` 菜单记录，或没有授予当前角色，就会被前端守卫拦截为 403。隐藏路由应依赖 `visible=0/menu_status=1` 进入授权树，再由前端过滤侧边栏展示。

**解决方案**:
新增或恢复手写/隐藏业务路由时，必须同步补齐：
- `sys_resource.resource_type=2` 菜单资源，`path` 使用前端实际路由，动态参数使用 `:param` 格式。
- `component` 使用真实页面组件路径，例如 `app-center/object.[objectCode]`。
- `visible=0`、`menu_status=1`，让路由可授权但不显示在菜单。
- `sys_role_resource` 授权给已经拥有对应业务入口的角色，并对已有资源做 `NOT EXISTS` 防重复保护。

**影响范围**:
- 应用中心运行态、对象详情、对象设计器、引擎中心入口。
- 所有 `src/router/index.js` 手写路由和 `unplugin-vue-router` 动态参数路由。
- `forge-server/db/migration/` 中系统菜单和权限资源脚本。

## 69. 低代码字段编码和数据库列名不能共用 camelCase


**发现日期**: 2026-06-21

**问题描述**:
表单设计器里把字段编码设置为 `userNick` 后，低代码发布或保存可能报错：

```text
数据库列名格式不正确: userNick
```

**根本原因**:
字段编码是前端/运行态字段名，允许 lowerCamel，例如 `userNick`；数据库列名是物理表标识符，新建 Forge 托管字段应默认使用 snake_case，例如 `user_nick`。如果设计器直接把 `fieldCode` 同步给 `fieldBinding.columnName`，就会把两套命名语义混在一起。

另外，导入旧系统已有表时，真实数据库列名可能本来就是 `userNick`。Schema/DDL 的安全校验不能简单限定为全小写下划线，否则会误伤零侵入旧库适配。

**解决方案**:
- 新建字段或修改字段编码时，前端默认用 `camelToSnake(fieldCode)` 生成 `columnName`。
- 后端 Schema 校验和 DDL 安全标识符校验允许安全 SQL 标识符：字母或下划线开头，后续可包含字母、数字、下划线。
- 区分“新建 Forge 托管表默认命名规范”和“旧系统已有表真实列名兼容策略”，不要用同一条正则表达两个目标。

**影响范围**:
- `LowcodeSchemaValidator`
- `LowcodeDdlService`
- `ForgePropertyPanel.vue`

## 70. 应用中心新建业务对象必须选择低代码运行数据源


**发现日期**: 2026-06-21

**问题描述**:
业务对象新建向导只有“从数据库表导入”模式能看到数据源，而且曾经按 `TENANT_BUSINESS` 过滤，导致“从空白对象创建”和“从 AI 描述生成”没有运行数据源选择入口。

**根本原因**:
应用中心低代码对象的运行数据源和 `forge-business` 手写业务模块的租户默认数据源是两套用途。新建低代码业务对象时应选择 `LOWCODE_RUNTIME` 数据源；`TENANT_BUSINESS` 只用于租户默认业务库，不应该驱动低代码对象发布和动态 CRUD。

**解决方案**:
- 新建业务对象向导第二步常驻展示“运行数据源”，三种创建方式都明确目标库。
- 数据源接口调用使用 `genDatasourceEnabled('LOWCODE_RUNTIME')`。
- 数据库表导入模式复用运行数据源加载表列表，导入来源和运行目标保持一致。
- 保存对象时把 `runtimeDatasourceId` 和运行数据源快照写入对象 options；设计器默认模型从 options 回填 `runtimeDatasource`，保存草稿时同步到模型和运行配置表。

**影响范围**:
- `BusinessObjectWizardDrawer.vue`
- `BusinessObjectDTO`
- `BusinessObjectDesignerService`

## 81. 应用中心对象设计器 URL 统一使用 object/:objectCode/designer


**发现日期**: 2026-06-29

**问题描述**:
历史代码里存在 `/app-center/object-designer/:objectCode` 或 `object-designer/sample_purchase_order` 风格入口。当前应用中心对象设计器主链路已经收敛到 `/app-center/object/:objectCode/designer`，如果新增跳转继续使用旧 URL，会出现路由不一致、菜单高亮异常或进入旧兼容组件的问题。

**解决方案**:
- 新增入口统一使用 `/app-center/object/{objectCode}/designer?panel=...`。
- 搜索旧 URL 时，允许保留 `router/index.js` 对旧组件文件的兼容 import，但业务跳转、卡片入口、流程 Banner 返回入口不能再拼旧路径。

**影响范围**:
- 应用中心对象卡片、业务域卡片、流程设计器返回业务应用按钮。
- 代码应用“表单字段”只读面板和流程配置入口。

## 93. 低代码设计器 zone props 保存成功不代表运行态可见


**发现日期**: 2026-07-03

**问题描述**:
详情设计器已经把数量区块保存到 `detail` zone props 的 `quantityPanels`，行展开组件也支持数量面板渲染，但发布后的真实运行页详情弹窗仍不展示数量区块。原因是设计器协议只留在 page schema 中，没有经 `LowcodeRuntimeConfigBuilder` 发布到 `options`，前端运行页、预览页和页面块也没有把该 prop 传给 `AiCrudPage`。

**解决方案**:
- 新增低代码设计器配置项时，必须同时检查保存协议、后端运行配置构建、真实运行页、低代码预览页、页面块渲染器和基础组件 props 六个入口。
- 对详情区块这类 runtime-only 配置，后端建议统一发布到 `options` 下的通用字段，例如 `options.detailPanels`，前端再透传给 `AiCrudPage`。
- 验收不能只看设计器保存成功，还要检查发布运行态 `crudProps` 是否能读到同名配置。

**影响范围**:
- `BusinessDetailDesigner`、`LowcodeRuntimeConfigBuilder`、`views/ai/crud-page.vue`。
- `LowcodePreviewPane`、`GridBlockRenderer` 和所有依赖 `AiCrudPage` 的低代码运行态页面。

## 94. 低代码自动编号不能只依赖配置迁移


**发现日期**: 2026-07-03

**问题描述**:
物料新增时报 `Column 'material_code' cannot be null`。前端表单已经把“物料编号”设计为自动生成，但实际新增请求里仍可能带 `materialCode=null`，如果后端只在字段 JSON 中显式存在 `generation` 配置时才生成编号，配置迁移未执行、旧配置未补齐或协议路径不一致时，空值会直接进入动态 `INSERT`。

**解决方案**:
- 自动编号的最终生成必须在后端新增链路完成，前端只负责配置和展示。
- `DynamicCrudService` 除读取显式 `generation` 配置外，还应对 `Code` / `No`、`_code` / `_no`、标签含“编号/单号”的字段做平台级约定兜底。
- 兜底只在字段无值且存在对应编码规则时生效；编码规则不存在时跳过，避免误伤普通 code 字段。
- 字段显式配置了 `generation`，即使是 `enabled=false`，也必须尊重显式配置，不再走约定兜底。
- 定向单测必须检查 `Tests run`，根 POM 默认 skip 时要使用 `-Penable-tests`。

**影响范围**:
- `DynamicCrudService` 低代码新增链路。
- 所有业务对象的编号、单号、编码字段。
- 采购仓储、CRM、合同财务等通过低代码运行配置发布的业务应用。

## 95. 业务对象设计器重建字段必须保留运行态元数据


**发现日期**: 2026-07-03

**问题描述**:
采购仓储对象进入设计器保存后，原本配置好的状态字典下拉、仓库/供应商记录选择器和自动编号配置退化成普通输入框或数字输入框，发布时继续报“选择器缺少业务对象编码”“字典字段必须配置字典类型”，新增时也会要求用户手填编号。

**根本原因**:
设计器保存会按表单/页面协议重新构造模型字段。如果只根据本次前端 payload 生成字段，而不合并数据库里已有 `model_schema`、旧运行态 `edit_schema/search_schema/columns_schema` 和 `page_schema.fieldSettings`，就会丢掉 `dictType`、`basicProps.recordSelector`、`basicProps.generation`、引用字段、公式配置等运行态关键元数据。

**解决方案**:
- `BusinessObjectDesignerService` 在 `BusinessFieldSchemaService.buildFieldSchema()` 前就要把旧字段元数据合并回 `BusinessFieldDTO`，否则字典校验会先失败。
- 旧运行态 schema 需要桥接回 `page_schema.fieldSettings`，发布和预览都以 `page_schema` 作为更完整的配置源。
- 组件类型从具体业务组件退化成通用 input/number 时，应保留旧的业务组件类型和 props，除非用户显式改了字段类型。

**影响范围**:
- 所有低代码业务对象设计器保存/发布链路。
- 字典字段、记录选择器、自动编号、公式字段、对象引用字段。
- 采购仓储、CRM、合同财务、人事等存量低代码应用。

## 96. 表单设计器 schema 归一化不能丢弃校验预设字段


**发现日期**: 2026-07-04

**问题描述**:
低代码表单设计属性面板中，“常用校验”下拉选择后短暂生效，但重新选中组件或保存回显后看起来没有选上。

**根本原因**:
`formDesignerSchema.normalizeValidation()` 如果只保留 `required/requiredMessage/rules`，会在每次 `normalizeFormDesignerSchema()` 时丢弃 `preset/pattern/message`。属性面板虽然已经发出 `updateComponent({ validation: ... })`，下一轮 schema 归一化仍会把常用校验字段清掉。

另一个容易漏掉的点是字段组件绑定了字段资产时，只更新画布组件不够；字段资产回写或重新选择组件时会用字段资产覆盖组件配置。

**解决方案**:
- `normalizeValidation()` 必须保留 `preset`、`pattern`、`message` 等 UI 配置字段。
- 属性面板更新常用校验时，同时 emit `fieldAssetUpdated` 写回字段资产。
- 清空常用校验时要写入空字符串覆盖旧值，不能删除 key，因为组件 patch 合并逻辑会保留旧字段。

**影响范围**:
- `ForgePropertyPanel.vue`
- `form-first/formDesignerSchema.js`
- 后续所有扩展到 `validation` 对象里的设计态配置

## 97. 前端默认加密时后端漏 @ApiDecrypt 会表现为 DTO 字段全空


**发现日期**: 2026-07-04

**问题描述**:
低代码动态 CRUD 新增表单加载对象引用字段选项时，前端 `field/source/selectorConfig` 已经都有 `objectCode=warehouse_management`，但后端 `/ai/business/selector/query` 仍报“选择器缺少业务对象编码”。日志容易误判为前端字段没传上。

**根本原因**:
前端 `cryptoConfig.includePaths` 为空时默认加密所有未排除接口，请求体会变成 `{ data: 加密串, algorithm: 'SM4' }`。如果后端 `@RequestBody` 接口没有 `@ApiDecrypt`，Spring 会直接把加密包装体绑定到业务 DTO，业务字段如 `objectCode/businessObjectCode/referenceObjectCode` 全部为空。

**解决方案**:
- `/ai/business/**` 这类需要接收 JSON 请求体的控制器必须按项目规范补齐 `@ApiDecrypt`，通常同时补 `@ApiEncrypt`。
- 排查“前端日志确认已传字段，但后端 DTO 为空”时，先检查控制器类或方法是否有 `@ApiDecrypt`，不要继续堆前端字段兜底。
- 同一组运行态 POST/PUT 接口要一起检查，避免修完一个接口后其它同链路接口继续出现同类问题。

**影响范围**:
- `BusinessRecordSelectorController`、`BusinessQuantityQueryController`、`BusinessActionExecutionController`、`BusinessTriggerController` 等低代码业务运行接口。
- 所有默认走前端加密拦截器且后端使用 `@RequestBody` 接收 DTO 的接口。

## 113. 表格拖拽滚动不能抢占单元格文本选择


**发现日期**: 2026-07-14

**问题描述**:
全局表格横向拖拽在 `mousedown` 时立即设置 `body.userSelect = 'none'`，并给表头、单元格统一设置 `cursor: grab`，会导致用户拖选复制表格文字时被识别为横向滚动；列头排序和筛选点击也容易被拖拽捕获影响。

**解决方案**:
- 单元格和表头属于内容交互区域，横向拖拽指令不得从这些节点启动；横向滚动保留滚动条、触控板和非内容空白区域拖拽；
- 只有移动距离超过阈值、真正进入拖拽后，才临时禁用文本选择并显示 `grabbing`；
- 单元格使用文本选择光标，排序器和筛选器使用指针光标，禁止给整个表格内容区统一设置 `grab`；
- 远程分页表格如果希望列头在当前已加载数据上立即排序/筛选，需要显式接收 sorter/filter 状态并转换当前数据，不能只声明 Naive UI 的 `remote` 列配置。

**影响范围**:
- 全局 `tableScrollEnhance` 指令；
- `AiTable`、`AiCrudPage` 及所有 Naive UI 数据表格；
- 表格文本复制、列头排序、列筛选和横向滚动交互。

## 115. CREATE TABLE IF NOT EXISTS 不会升级存量表结构


**发现日期**: 2026-07-16

**问题描述**:
Flyway 脚本为新环境写了包含完整字段的 `CREATE TABLE IF NOT EXISTS`，随后代码和 Mapper 开始读取新字段；但升级环境中表已存在，建表语句会整体跳过。如果脚本没有再对新增字段和索引逐项做 `information_schema` 检查，运行时会出现缺列，逻辑删除和唯一键语义也不会生效。

**解决方案**:
- `CREATE TABLE IF NOT EXISTS` 只负责全新数据库；存量升级必须对每个新增字段、生成列和索引分别检查并执行 `ALTER TABLE`。
- 逻辑删除改造必须同时补 `del_flag`、实体 `@TableLogic`、Mapper XML 过滤和有效记录唯一索引，不能只改其中一层。
- 旧唯一键会阻止删除后重建相同业务编码时，应先创建新的有效记录唯一索引，再条件删除旧唯一索引。
- 静态审查需要同时对照历史表定义和新迁移，不能只阅读新脚本顶部的完整建表模板。

**影响范围**:
- 所有从历史备份/基线已存在、后续由正式 Flyway 增量升级的业务表。
- 逻辑删除、生成列、唯一键语义和 Mapper 新增查询字段。

## 116. 业务编码作为 LIKE 前缀时下划线会扩大匹配范围


**发现日期**: 2026-07-16

**问题描述**:
使用 `LIKE CONCAT(#{prefix}, '%')` 查询旧序列 key 时，如果规则编码包含下划线，MySQL 会把 `_` 解释为单字符通配符，可能把其它规则的序列水位一起纳入 `MAX(max_id)`，造成新规则异常跳号。

**解决方案**:
- 业务要求“字符串前缀相等”时优先使用 `LEFT(column, CHAR_LENGTH(#{prefix})) = #{prefix}` 等精确比较；
- 如必须使用 `LIKE`，调用方或 SQL 必须转义 `%`、`_` 和转义符，并显式声明 `ESCAPE`；
- 兼容迁移查询还要同时限定租户、规则身份和周期，不能只凭宽泛前缀取最大值。

**影响范围**:
- `sys_id_sequence` 等把业务编码拼入字符串 key 的兼容查询；
- 所有允许下划线、百分号进入用户可配置编码的 SQL 前缀匹配。

## 118. 业务变量不能与低代码字段映射强制等同


**发现日期**: 2026-07-17

**问题描述**:
编码引擎已经支持从 `fields[variableName]` 读取业务变量，但配置端把所有 VARIABLE 都强制绑定到低代码对象字段。这会让 Java 业务代码、兼容 API 或其它非低代码调用方无法保存“调用时传值”的通用规则，形成底层支持但管理端阻断的半兼容状态。

**解决方案**:
- VARIABLE 显式保存分段级 `variableSource=CUSTOM|LOWCODE`，缺省和 legacy 物化按 CUSTOM。
- CUSTOM 只校验安全变量名，不要求业务对象，运行时继续从 `fields` 取值。
- LOWCODE 才校验来源对象、启用非系统字段和运行时 `objectCode`。
- 规则是否对象专属由“是否含 LOWCODE VARIABLE”决定，不能由“是否含任意 VARIABLE”决定。

**影响范围**:
- 所有兼有低代码运行和代码/API 调用的可配置表达式、模板、编码规则等功能。
- 设计态元数据校验、运行时上下文校验和通用调用协议的职责分层。

## 119. 号段数据库水位不是最后实际使用值


**发现日期**: 2026-07-17

**问题描述**:
号段生成器会一次把数据库 `max_id` 推进一个完整 step。旧编码规则只生成过一个编号时，数据库水位也可能已经从 0 变成 1000。迁移代码若用 `legacyMaxId + 1` 安全续接，三位十进制规则会从 1001 开始并立即超过配置容量；若把水位当成实际已用值回退到较小序号，又可能在服务重启或多实例场景生成重复编号。

**解决方案**:
- 迁移续接必须优先保证唯一性，旧安全起点仍取已分配水位之后，不能猜测进程内号段实际消费位置。
- 固定宽度校验必须增加“旧水位 + 进制容量”组合测试，不能只分别测试号段续接和进制溢出。
- 兼容扩宽只能由旧安全起点决定最小位数；没有旧水位的新规则继续严格溢出失败，后续实际序号超过兼容宽度时也继续失败。
- 只在严格编码发生容量冲突时查询旧水位，避免给正常生成链路增加额外数据库读取。

**影响范围**:
- 所有从旧计数器、Redis/数据库号段或批量预分配序列迁移到固定宽度编码器的场景。
- 编号唯一性、迁移连续性、进制容量和服务重启后的兼容行为。

## 120. 编码规则的 ruleCode 和 SEQ segmentKey 都是计数器永久身份


**发现日期**: 2026-07-17

**问题描述**:
新序列 key 同时包含 `ruleId` 和 SEQ `segmentKey` 摘要。若逻辑删除后允许复用同一 `ruleCode`，新记录会获得不同 ruleId；若编辑时删除旧 SEQ 再新增，分段 key 也会变化。两种操作都会绕开原 `cr:*` 水位并从新 key 起号，而低代码绑定仍按 `ruleCode` 命中新规则，形成无审计重置和历史编号碰撞。

**解决方案**:
- `ruleCode` 在租户内跨逻辑删除历史永久唯一，数据库使用 `(tenant_id, rule_code)` 唯一索引，应用查询不得过滤 `del_flag`。
- 已有 SEQ 更新必须保留同一 `segmentKey` 且仍为 SEQ；排序、长度、进制和周期等属性可以按业务规则调整。
- 原本没有 SEQ 的规则允许首次增加；需要新计数器必须创建不同 `ruleCode`。

**影响范围**:
- 所有把规则 ID、分段 ID/key、模板节点 ID 拼入序列键、幂等键或状态机身份的设计态配置。

## 121. 高基数号段缓存必须有界且乐观重试不能运行在 RR 旧快照中


**发现日期**: 2026-07-17

**问题描述**:
编码规则允许业务字段参与分组并按小时/日切换 key，永久 `ConcurrentHashMap<bizKey, SegmentHolder>` 会随“分组值 × 周期”无界增长。另一方面，把 select→乐观 UPDATE→失败后普通 select 的重试循环放在 MySQL 默认 REPEATABLE_READ 事务中，后续 select 可能持续看到旧 version，最终把正常竞争误判为分配失败。

**解决方案**:
- 号段 holder 使用 maximumSize + expireAfterAccess 的有界缓存；淘汰只丢弃未消费号段并产生允许的空洞，数据库水位不会回退。
- 号段分配继续使用 `REQUIRES_NEW`，同时显式设置 `READ_COMMITTED`，让乐观冲突后的重新查询看到其它实例已提交版本。
- 测试覆盖高基数后缓存规模、淘汰后继续取号唯一性，以及事务传播和隔离级别。

**影响范围**:
- 所有按租户、业务字段、组织、日期周期动态构造 key 的数据库号段/本地缓存生成器。

## 122. AiForm 数字字段类型必须统一归一化


**发现日期**: 2026-07-18

**问题描述**:
页面 `editSchema` 使用 `type: 'input-number'` 时，如果 `AiFormItem`、必填校验、编辑回填和高级查询分别硬编码 `number/inputNumber`，字段会回退为普通 `n-input`，`min/max/step` 约束同时失效。只补渲染分支仍会让校验、回填或查询链路继续出现类型分叉。

**解决方案**:
- 页面 Schema 标准写法统一为 `type: 'number'`；
- 共享层通过单一 `isNumberFieldType` 兼容 `number`、`inputNumber`、`input-number` 历史值；
- AiFormItem、AiForm、AiCrudPage 和 AiCustomQuery 共用同一判断，禁止继续散落字符串数组；
- 批量清理后使用静态扫描保证 `src/views` 中 `type: 'input-number'` 零残留，并增加 NInputNumber 与约束透传组件测试。

**影响范围**:
- AiForm/AiCrudPage 的数字渲染、必填校验、详情回填和高级查询；
- 所有手写、生成或从历史配置恢复的表单 Schema。

## 125. AiCrudPage 的父容器必须提供明确高度


**发现日期**: 2026-07-21

**问题描述**:
分页接口已经返回 `records`，表头和工具栏也能显示，但表格数据行、分页或无数据提示不可见。页面根容器只设置 `min-height: 100%` 时，这个问题容易被误判为分页协议或空状态组件缺失。

**根本原因**:
`AiCrudPage`、`AiTable` 和 Naive UI `NDataTable flex-height` 使用纵向 Flex 高度链路。父页面只有 `min-height`、没有可计算的 `height` 时，表格正文 Flex 区域可能塌陷；数据行和 `NEmpty` 共用该区域，因此会同时消失。页面再传入基于视口估算的固定 `max-height`，还会与主布局实际可用空间发生冲突。

**解决方案**:
- 使用 `AiCrudPage` 的页面根容器设置 `height: 100%`、`min-height: 0`，复杂页面同时设置纵向 Flex 和 `overflow: hidden`。
- 让 `AiCrudPage` 占用 `flex: 1` 并保持 `min-height: 0`，确保正文获得剩余空间。
- 页面无特殊需求时不要硬编码 `max-height="calc(100vh - ...)"`。
- 多列表格优先使用 `AiCrudPage` 的自动 `scroll-x` 计算，避免固定宽度与列配置、固定操作列不一致。
- 排查时先确认父级高度链路，再检查 `records/total` 解析；共享 `AiTable` 已有空状态时不要重复实现。

**影响范围**:
- 所有使用 `AiCrudPage` / `AiTable` Flex 高度模式的列表页面。
- 数据行、分页、加载态和无数据提示的可见性。

## 128. 批次迁移必须先全域预检并让异常逃逸事务回调


**发现日期**: 2026-07-26

**问题描述**:
在事务循环内逐行捕获冲突或异常会让前序 UPDATE 继续提交，表面上的“失败计数”不等于批次回滚。聚合迁移若按 scope 边执行边校验，还可能先完成数据连接写入，再因低代码 `configKeys` 或活动 keyId 不合法失败，形成跨 scope 部分迁移。

**解决方案**:
- 聚合入口在第一次写入前校验所有 included scope 的 keyId、开关和必填参数。
- 每批先完成只读准备，再在 `REQUIRES_NEW` 中执行全部比较更新；任一冲突或异常必须抛出事务回调，批次提交成功后才累计 `MIGRATED`。
- 动态数据源迁移使用实际运行 DataSource 对应的事务管理器，不能误用主库事务。

**影响范围**:
- 所有多来源、多租户、动态数据源的数据修复、重加密和受控回填任务。

## 130. 列表设计器的字段目录与稳定模型分离时会出现“字段点不动”


**发现日期**: 2026-07-28

**问题描述**:
应用页面为了避免切换 CRUD 对象触发布局循环，将 `ListPageGridDesigner.modelSchema` 保持为稳定页面模型，同时通过独立 `fields` prop 传入页面表单或当前对象字段。字段抽屉能看到字段，但点击选择后立即消失，表现为“配置列表字段点不动”。

**根本原因**:
`ListPageGridDesigner` 在 `modelValue` 回写时调用 `syncGridLayoutWithModel(layout, modelSchema)`；该函数按 `modelSchema.fields` 清洗 `fieldRefs/searchFieldRefs`。稳定模型字段为空时，刚写入的字段引用会在下一次 prop 同步中全部被过滤，即使组件收到的 `fields` 非空。

**解决方案**:
- 页面模型中的对象/API 上下文可以保持稳定，但每次布局同步必须通过 `buildGridSyncModelSchema(modelSchema, fields)` 把当前实际字段目录注入清洗模型。
- 不要重新让父页面按当前选中区块动态重建整个 `modelSchema`，否则会恢复“模型变化 → 布局回写 → 父页面变化”的循环。
- 回归测试必须覆盖 `modelSchema.fields=[]`、`fields` 非空时，`AiCrudPage.fieldRefs` 和 `props.searchFieldRefs` 均被保留。

**影响范围**:
- 应用内页面表单字段配置。
- 未发布业务对象设计草稿字段配置。
- 所有把 `ListPageGridDesigner.fields` 与 `modelSchema.fields` 分离传入的页面。

## 131. 页面表单与 CRUD 不能停在“有字段、无数据存储”的半绑定状态


**发现日期**: 2026-07-28

**问题描述**:
页面表单能为 `AiCrudPage` 提供字段目录和新增编辑布局，但如果保存只写页面 Schema，发布时仍会提示未绑定业务对象。再让用户进入数据对象抽屉创建对象，会形成“表单 → 数据对象 → 新建对象”连续弹层；普通用户既无法判断业务对象含义，也容易把字段已显示误认为数据已经可以保存。

**解决方案**:
- 数据页面模板直接创建页面和页面表单，保存表单后调用应用级事务接口自动准备数据存储。
- 以 `applicationId + formAssetId` 标识托管对象；重复保存、网络重试和关联恢复复用同一对象并同步最新字段/表单 Schema。
- 同一表单的所有未绑定 CRUD 一次回写显式对象引用；区块级和页面级手工绑定必须优先，批量回写不能覆盖。
- 自动准备选择默认可写且允许自动建表的运行数据源，先保存设计/关联，再对受控托管对象执行安全建表或追加字段；不自动发布。失败时明确表单草稿已经保存并提供原位重试。
- 业务对象、数据源和角色只在高级数据设置出现，普通界面使用“表单数据 / 数据存储”。

**影响范围**:
- 应用内表单资产、`AiCrudPage` 数据绑定与发布检查。
- 数据页面模板、对象关联恢复和高级数据设置边界。
- 所有把“页面 Schema 已有字段”误当成“数据已具备持久化落点”的低代码流程。

## 132. 草稿渲染接口放行不代表页面 CRUD 已进入设计预览


**发现日期**: 2026-07-28

**问题描述**:
应用内表单自动准备托管对象并回绑 `AiCrudPage` 后，对象仍处于设计草稿状态。页面如果只对 `/ai/crud-config/render/{configKey}` 使用草稿读取，实际挂载的 `AiCrudPage` 仍会自动调用 `/ai/crud/{configKey}/page` 正式接口，从而提示“低代码应用尚未发布”；若绕过门禁继续自动查表，还会在未显式执行 DDL 时访问不存在的表。

**解决方案**:
- 应用 `edit=1` / `draft=1` 的渲染配置和实际 CRUD 端点必须统一携带 `designPreview=1`，列表、详情、新增、修改、删除、导入和导出不能遗漏。
- 正式运行不能携带该标记，继续只使用已发布快照和配置。
- 设计画布默认静态结构预览并设置 `lazy`，只在用户显式开启真实数据预览后请求草稿 CRUD；静态预览同时关闭导入、导出、自定义查询并阻止提交。

**影响范围**:
- 应用页面设计器中的 `AiCrudPage`、对象草稿字段回显和自动托管表单。
- 所有“配置渲染已允许草稿，但实际数据接口仍按正式运行校验”的低代码预览链路。

## 133. 组合编码长度必须按最终落库字段校验


**发现日期**: 2026-07-28

**问题描述**:
页面表单自动准备数据存储时，对象编码本身已限制为 48 位，但模型编码又拼接了业务域前缀并按 64 位截断。`ai_business_object.model_code` 和 `ai_lowcode_model.model_code` 实际均为 `varchar(48)`，长应用编码与长表单名称组合后在对象插入阶段触发 `Data too long for column 'model_code'`，整次准备事务回滚。

**解决方案**:
- 把模型编码最大长度定义在共享命名服务中，归一化和组合生成统一使用 48 位；前端同名辅助函数必须同步该边界。
- 回归测试不能只验证对象编码，要覆盖“业务域 + 对象编码”的最终组合值及真实长应用/表单场景。
- 稳定标识超长优先修复生成规则，不随意扩大数据库字段；字段扩容会同时改变低代码模型协议、索引和外部引用边界。
- 自动创建应用时由后端生成并返回最终编码，避免前端本地规则、重名避让和真实落库值再次漂移。

**影响范围**:
- 页面表单托管对象、应用模板、数据库表导入和业务对象向导。
- 所有先生成局部编码、再拼接命名空间前缀的数据库稳定标识。

## 134. 元数据准备完成不等于物理数据表已经存在


**发现日期**: 2026-07-28

**问题描述**:
页面表单保存接口创建了托管对象、字段、运行配置和应用关联，并反馈“数据存储已准备完成”，但没有调用数据库差异同步。设计态 CRUD 随后访问模型表时只能得到“数据表不存在”，用户重新建对象或重新发布也无法补上缺失 DDL。

**解决方案**:
- 对来源身份一致的 `PAGE_FORM` 托管对象，元数据事务提交后必须调用统一表映射/DDL 服务创建表或追加缺失字段；成功反馈必须晚于数据库同步成功。
- 不把 MySQL DDL 放进元数据原子事务承诺中。DDL 失败时保留已提交设计并支持重复保存幂等重试，不能删除对象制造更多孤儿关联。
- 自动同步使用数据源当前真实配置和 DDL 预检，不直接信任模型中历史保存的 `allowDdl` 快照；执行阶段仍由运行数据源解析器再次校验。
- 仅自动执行现有安全白名单，非追加式变更进入高级数据设置；手工对象同步的权限、版本和二次确认不能复用自动入口绕过。

**影响范围**:
- 应用内页面表单自动托管数据、设计态真实预览和发布前数据库就绪检查。
- 所有把“模型/Schema 已落库”错误等同于“对应物理表已可写”的低代码链路。

## 135. 自动托管表的发布状态不能依赖上一次保存时的同步快照


**发现日期**: 2026-07-28

**问题描述**:
表单字段、页面布局和数据库映射分别保存后，发布检查可能读取到旧的 `OUT_OF_SYNC`。即使当前差异只是首次建表或新增字段，用户仍会被要求离开页面设计器手工处理数据库，造成“表单已经保存、数据也已绑定，但仍不能发布”。

**解决方案**:
- 应用发布检查与最终发布前，对当前应用自身的 `PAGE_FORM` 托管对象重新执行数据库差异预检和安全同步，再生成 readiness 结果。
- 归属校验同时检查应用关联标记和对象自身标记，不能只凭对象 ID 或页面引用进入自动 DDL 通道。
- 页面布局版本、查询字段变化不能作为数据库同步版本；是否同步应由当前模型 Schema 与真实表结构差异决定。
- 仅自动处理首次建表和安全追加列。删除、改名、类型/长度调整等高风险差异继续返回可理解的人工处理提示。

**影响范围**:
- 自动托管页面表单的发布检查、最终发布和数据库就绪状态。
- 所有设计态元数据版本与物理资源状态分别演进的低代码能力。

## 136. Vue 监听器返回新数组会把无关对象替换误判为请求条件变化


**发现日期**: 2026-07-28

**问题描述**:
`GridBlockRenderer` 使用单个 getter 返回 `[previewLiveData, previewMode, previewRecordId]`。预览成功后将状态文案写回区块会替换整个 `block` 对象，getter 重新运行并生成新数组；即使三个值完全相同，`watch` 仍认为返回值变化，再次调用 `loadList()`，形成无限列表请求。

**解决方案**:
- 需要逐项比较时使用 `watch([() => leafA, () => leafB], callback)`，或构造只包含真实请求条件的稳定字符串签名。
- 请求结果状态、错误文案、选择态、布局位置等展示状态不能进入 reload key。
- 回归测试应先证明状态写回前后的请求签名相同，再证明开关、记录或 API 变化确实改变签名。
- 同一组件中的树、详情和通用数据绑定监听也应使用叶子 source，避免任何区块属性写回触发无关接口。

**影响范围**:
- Vue 设计器中通过不可变对象更新整个组件配置的所有 API 监听器。
- 真实数据预览、树数据、详情数据和通用区块数据绑定。

## 137. 页面查询字段与对象查询白名单分裂会造成静默失效


**发现日期**: 2026-07-28

**问题描述**:
动态页面已经通过 `searchFieldRefs` 显示查询条件，输入后请求也携带了业务字段，但动态 CRUD 服务仍只允许对象原始 `searchSchema` 中的字段。页面从列表或表单字段目录选择的新查询字段因此在 SQL 层被静默跳过；`searchFieldSettings` 中的查询方式、查询组件和映射字段如果只保存在页面 JSON，也不会改变真实请求。

**解决方案**:
- 页面运行 Schema 必须同时合并 `searchFieldRefs` 和 `searchFieldSettings`，映射字段只改变请求键，业务标签继续来自原页面字段。
- 查询方式作为独立控制参数传输，不能与用户输入值混在一起；列表和导出使用同一协议。
- 服务端查询白名单可以覆盖动态配置已经公开的查询、列表和编辑字段，但必须继续经过真实列映射，不能接受任意请求字段。
- 页面操作符只允许固定集合；非法字段、非法操作符和损坏元数据安全忽略并回退对象默认查询协议。

**影响范围**:
- 应用内 `AiCrudPage`、独立列表设计器和动态 CRUD 导出。
- 所有允许页面局部覆盖共享对象查询展示与查询方式的低代码能力。

## 141. 整体草稿版本不能作为数据库结构是否同步的证据


**发现日期**: 2026-08-01

**问题描述**:
业务对象的 `draftVersion` 同时覆盖字段、表单、页面、查询等设计内容。数据库同步记录只保存同步时的整体版本后，只要页面或表单发生变化，应用对象摘要就会把版本不一致直接标记成 `OUT_OF_SYNC`，即使物理表没有任何差异；发布门禁复用该摘要后会产生无法解释的假阻断。

**解决方案**:
- 列表摘要中的版本不一致只能表示“待实时检查”，不能断言数据库结构失步。
- 发布门禁必须调用表结构服务实时比较目标表、业务字段、列类型和 DDL 预览。
- 未在业务字段模型中重复声明的 Forge 标准系统列可以展示为只读系统列，但不能计入未映射业务列。
- 系统逻辑删除字段使用 `0/1` 语义时，`char/varchar/tinyint/int/bigint` 可视为兼容存储，不能只按 SQL 基础类型制造假阻断。
- 未映射数据库列必须按写入风险分级：可空、有默认值、`auto_increment` 或生成列保留可见但不阻断；`NOT NULL` 且无默认值、不能自动生成的列继续阻断，并提示添加字段映射或调整数据库默认值。
- 真实缺列、非兼容类型、危险额外列和待执行 DDL 继续阻断，并在消息中展示具体差异。

**影响范围**:
- 低代码对象数据库映射摘要、应用发布检查、表单或页面设计变更后的同步状态展示。

## 145. 页面运行期间加密开关变化会让显式敏感请求绑定为空 DTO


**发现日期**: 2026-08-02

**问题描述**:
页面启动时读取到 API 加密开启，后端随后重启或通过配置中心关闭加密。浏览器如果继续按旧状态发送 `{data, algorithm}`，当前后端不会执行解密，`@RequestBody` 只能把加密信封绑定到业务 DTO，最终表现为多个必填字段同时为 null/blank。

**解决方案**:
- `postEncrypt` 标记的显式敏感请求在真正提交前重新同步 `/crypto/config`，不能只在 SPA 启动时读取一次。
- 服务端关闭加密时按普通 JSON DTO 发送；开启时必须完成密钥协商，不能无密钥降级明文。
- 运行配置无法确认时失败关闭并显示明确提示，避免把协议错位伪装成业务参数校验失败。
- DTO 的 Bean Validation 必须提供字段级中文消息，保留最后一道可理解的错误反馈。

**影响范围**:
- 所有通过 `postEncrypt` 发送的新增、修改、凭据维护等敏感请求。
- 后端重启、配置中心热切换或浏览器长时间不刷新页面的场景。

## 170. 显式空 Schema 不能与缺失 Schema 使用同一个空集合分支


**发现日期**：2026-08-11

兼容旧协议时，解析函数常把“配置字段不存在”和“显式配置空数组”都归一成空集合。若运行时再以 `schema.isEmpty()` 判断是否启用旧兼容逻辑，新动作配置 `inputSchema: []` 后反而会放行任意客户端字段，绕过设计器表达的“无输入”。

处理原则：解析结果之外必须保留字段是否显式声明的信息。显式空 Schema 表示输入集合为空，任何额外字段都应失败关闭；只有配置键完全缺失的存量协议才能进入兼容分支。测试必须同时覆盖“显式空数组拒绝输入”和“缺失字段仍兼容旧输入”，不能只覆盖非空 Schema 的未知字段拒绝。

## 172. 表单设计器旧状态联动不能只保存 `props.__events`


**发现日期**：2026-08-11

旧属性面板会把“显示/隐藏、启用/禁用”配置保存在来源组件的 `props.__events`，但 `AiForm` 运行时只读取目标字段的 `runtimeRules`。只把配置保存在设计态会出现“页面能看到配置、预览和发布页完全不生效”。

处理原则：表单协议规范化时，把旧 `showHide` / `enableDisable` 事件按 `targetId` 投影为目标组件运行规则，并保留稳定迁移标识避免重复；“满足时显示”必须显式声明条件不满足时隐藏。静态隐藏但带可见性规则的字段不能在设计器编译、发布编译或子表字段过滤阶段提前丢弃，隐藏态字段也不能参与必填校验。

## 173. MONEY 组件和数据库金额单位必须形成完整运行时协议


**发现日期**：2026-08-12

只在设计器里把 `money` 渲染成两位小数输入框并不代表金额合同已经成立。若数据库按规范使用 bigint 分存储，而动态 CRUD 直接把页面的元值写库，MySQL 可能截断小数或把元误当分；反向读取也会把分直接展示给用户。另一个隐蔽问题是 `money` 只在设计器编译路径被归一为 `number`，直接使用 CRUD `editSchema` 时会退化成文本输入。

处理原则：显式 `MONEY + 整数存储列` 必须由通用运行时执行“元输入 → 分存储 → 元回显”，严格拒绝超过配置小数位的静默舍入；历史 decimal MONEY 字段保持兼容。`money` 和 `integer` 必须进入共享数字字段识别函数，确保设计器页面与直接 CRUD 页面一致。合同测试至少覆盖正向转换、反向转换、精度越界和 decimal 兼容。

## 175. JSON 快照补丁不能靠肉眼猜字段数组下标


**发现日期**：2026-08-12

低代码模型快照常用 `$.fields[n]` 做迁移补丁。字段数组包含 `id`、`tenantId` 等系统字段时，业务字段下标容易错位；误把 `salesUserId/status` 当成 `fields[1]/fields[14]` 会实际修改租户字段和现金金额字段，造成“现金金额在设计器/运行页仍不可见”这类二次问题。

处理原则：写 JSON 数组下标补丁前必须按真实 `model_schema.fields` 顺序核对目标字段，最好同时用 `JSON_UNQUOTE(JSON_EXTRACT(... '$.fields[n].field'))` 作为 WHERE 保护。合同测试要断言关键字段下标和正反条件，例如系统字段隐藏、条件展示字段保持 `formVisible=true`。

设计器画布与运行页语义也要分开：画布用于配置字段，不能套用 `runtimeRules` 把“满足条件才显示”的字段隐藏掉；真实 H5/运行页再按 `runtimeRules` 控制显隐。否则用户会在设计器里找不到静态码、现金金额等受控字段。

## 176. 数据区块的空 `fieldRefs` 应回退到运行时字段目录


**发现日期**：2026-08-16

页面搭建器中的数据表单、数据表格等区块刚绑定业务对象时，区块通常还没有持久化 `fieldRefs`。如果渲染器始终按 `fieldRefs` 映射字段，即使外层已经成功加载对象 `fieldCatalog`，最终仍会得到空数组，表现为“数据源已连接但画布空白”。

处理原则：数据字段区块在 `fieldRefs` 为空时应使用外层提供的完整字段目录；一旦用户显式配置字段引用，再按引用筛选和排序。字段预加载、loading、缓存和数据源选择器必须复用同一组数据区块类型定义，避免新增区块时只放开其中一条链路。回归测试至少覆盖空引用回退、显式引用筛选、未选数据源引导和同对象缓存复用。

## 177. 业务对象发布不能重置应用入口配置


**发现日期**：2026-08-16

应用发布会连带发布业务对象，业务对象发布又会同步其运行入口。如果同步已有入口时重新写入默认 `appType`、`entryMode`、`entryUrl`、名称、图标、状态和 `options`，会抹掉应用工作台中人工配置的 `mountTarget=MOBILE`、移动菜单和 `clientCode=h5`，表现为重新发布后移动端入口退回管理端。

处理原则：低代码发布自动创建入口时可以写入标准管理端默认值；入口已经存在时，发布链路只补齐 `suiteCode`、`objectCode`、`configKey` 等对象运行绑定。入口的挂载端、菜单、打开方式、名称、图标、状态、排序和扩展配置由应用工作台维护，发布过程不得覆盖。

## 180. 运行字段基线会掩盖表单组件的结构变更


**发现日期**：2026-08-18

页面表单保存时，为避免未展示字段被误判为删除，通常会把完整运行字段目录合并进提交字段列表。但这样只比较字段列表不够：用户删除已锁定组件、修改字段编码或切换组件类型后，旧字段仍可能由运行目录留在请求里，后端字段存在性/存储类型校验会被基线掩盖。对象字段从字段资产重新拖入画布时，如果组件构造器又没有继承 `fieldBinding.locked`，前端也会错误开放结构编辑。

处理原则：运行字段目录可以作为字段保存基线，但已有数据保护还必须按表单组件稳定 ID 对比持久化草稿与提交草稿，独立校验组件是否删除、绑定编码是否变化、组件类型是否变化；字段资产转组件时显式继承结构锁。前端禁用只是体验保护，后端必须对直接请求继续失败关闭。

## 183. 设计预览开关不能决定正式运行态，发布快照必须回填最终对象状态

**发现日期**：2026-09-18

应用页面区块会保存 `previewLiveData`，但它只表达设计画布是否允许请求真实数据。正式运行页若继续用该字段判断“静态预览”，发布应用会被错误显示静态提示并拦截提交。另一方面，应用候选快照在对象发布前创建；只更新应用状态而不更新 `objects[*].designStatus`，正式运行上下文仍会暴露发布前的对象状态。

处理原则：静态预览必须同时满足“非运行交互态”和“未开启真实预览”，正式运行态不读取设计器预览开关决定可提交性。协调发布应在提交前重新校验所有选中对象均为 `PUBLISHED` 且有发布版本，并把最终状态和对象设计版本引用回填到不可变快照。表单子表关系发布前还要从持久化 Schema 重建；遍历必须覆盖根 `components`、布局 `children` 和多表单 `forms[*].schema.components`，不能把 form 容器误当普通组件后停止。

## 低代码应用创建页面必须读取运行数据源并支持引用现有表

**发现日期**: 2026-09-19

**问题描述**:
应用内「创建页面」只生成空白对象并落到默认 LOWCODE_RUNTIME 数据源，没有加载代码生成里配置的运行数据源，也不能先选数据源再选现有表、按字段生成表单。应用初始化向导和对象向导其实已有这套能力，页面创建链路没有接上。

**解决方案**:
创建数据页时读取 `genDatasourceEnabled('LOWCODE_RUNTIME')`，允许选择运行数据源。引用现有表时再拉表列表和列，推断字段类型并生成表单；保存走 `DB_IMPORT`，不要再自动 CREATE TABLE。

## 删除页面后同编码重建业务对象

**发现日期**: 2026-09-22

**问题描述**:
删除应用内页面并手工 DROP 了物理表后，用相同业务对象编码（如 `indicator_group`）再建页面，报「业务对象编码已存在（编码必须在租户内唯一）」。页面删除只会调用 `detachOrphanPageFormObjects` 解除应用关联，**不删** `ai_business_object`；租户级唯一索引 `uk_ai_business_object_code_tenant_active (tenant_id, object_code, del_flag)` 仍被 `del_flag=0` 的残留行占用。

**解决方案**:
- 删除页面解绑孤儿 PAGE_FORM 对象后，若该对象已无任何应用引用，则软删对象及其独占数据模型，并清理对象关系。
- 新建对象编码冲突时，若冲突行是无人引用的 PAGE_FORM 孤儿，自动回收后再创建。
- 物理表仍需用户自行 DROP；重建时会按新设计再同步建表。

## 开关默认值 true/false 会导致发布建表失败

**发现日期**: 2026-09-22

**问题描述**:
应用发布时 CREATE TABLE 报 `Invalid default value for 'field_switch'`。DDL 写成 ``tinyint NOT NULL DEFAULT 'true'``。表单开关默认值被存成布尔 `true`/`false`，MySQL 方言又把非表达式默认值一律加引号。

**解决方案**:
- 开关默认值统一归一成 `0`/`1`；方言对 tinyint/boolean 默认值按数值写出，不再加引号。
- DDL 层：tinyint/数值列把 true/false 规范成 0/1，并输出无引号数字 DEFAULT。
- 设计器：switch 的 checked/unchecked/默认值始终用 0/1。
- 保存表单会尝试同步建表；失败时常只记 warning。发布会对未 IN_SYNC 的 PAGE_FORM 对象再同步一次，所以错误常在发布时才暴露。

## 列表开关列不能只显示 0/1，需 switch 渲染 + 行内更新

**发现日期**: 2026-09-22

**问题描述**:
开关字段在列表里直接显示 `0`/`1`。`LowcodeRuntimeConfigBuilder` 未给 `switch` 生成 `render.type`，`AiCrudPage.resolveColumnRender` 也不认识开关列。

**解决方案**:
后端默认 `render.type=switch`（含 checked/unchecked 值）；前端列表渲染 `NSwitch`，有 update 接口时行内 PUT 部分字段更新。存量已发布配置可从 `editSchema.type=switch` 推断，仍无样式时需重新发布或走 designPreview 草稿编译。

## 审计提交回调不能依赖已退出的业务数据源上下文

**发现日期**：2026-09-20

`@Transactional` 方法内部的 try-with-resources 会先关闭运行数据源 Scope，随后事务代理才执行 `beforeCommit`。审计若此时直接通过动态 Repository 回读最终行，会从业务数据源退回平台数据源；平台连接看到旧快照时会漏记主表变化，而显式标记的子表删除仍可生成摘要。

应在写钩子阶段按行保存数据源读取上下文与实际主键列，自增 ID 未生成时先保存表级信息。最终回读仅在受控 Scope 中临时恢复上下文，正常及异常退出都还原调用方上下文。回归测试必须覆盖完整采集服务与事务回调，不能只给差异引擎传两份人为准备好的 Map；模拟链路通过仍不等于真实数据库事务和跨库原子性已经验收。
