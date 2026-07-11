# Forge项目踩坑记录

> 记录开发过程中遇到的常见错误和解决方案，避免重复踩坑

---

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

## 5. 报表项目保存/读取接口缺少加解密注解导致配置不生效

**发现日期**: 2026-05-11

**问题描述**:
报表页面发布后重新进入，动态接口配置或组件删除等画布变更看起来没有保存成功。前端请求链路正常，但后台接口缺少 `@ApiEncrypt` / `@ApiDecrypt` 时，响应/请求与前端加密拦截链路不匹配，导致保存或读取结果异常。

**正确用法**:
报表项目保存、读取、发布等需要经过前端加密请求链路的接口，后端必须按项目规范补齐：

```java
@ApiDecrypt
@PutMapping
public RespInfo<Void> update(@RequestBody GoviewProject project) {
    ...
}

@ApiEncrypt
@GetMapping("/{id}")
public RespInfo<GoviewProject> getById(@PathVariable Long id) {
    ...
}
```

**解决方案**:
排查“前端已传参但后台保存/回显不生效”时，除字段映射、租户条件外，必须检查接口是否补齐 `@ApiEncrypt` / `@ApiDecrypt`。

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

## 8. 异步弹窗使用 v-if 首次打开无响应

**发现日期**: 2026-07-03

**问题描述**:
`/generator/table` 页面点击“字段”“预览”没有弹窗响应。父组件使用 `v-if="showXxxModal"` 懒加载弹窗，并通过 `v-model:show` 传入 `show=true`；子弹窗内部如果写 `const visible = ref(false)` 且监听 `props.show` 未开启 `immediate`，组件创建时不会把首个 `show=true` 同步到内部 `visible`。

**解决方案**:
这类弹窗内部可直接用父级初始值初始化，并让 `props.show` watcher 立即执行：

```js
const visible = ref(props.show)

watch(() => props.show, (val) => {
  visible.value = val
}, { immediate: true })
```

**影响范围**:
- 使用 `defineAsyncComponent` 懒加载的弹窗
- 父组件用 `v-if + v-model:show` 控制挂载和显示的弹窗
- 首次打开时需要立即加载数据的弹窗，例如字段配置、代码预览、导入表、AI 建表

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

## 8. 顶部菜单目录不应直接按自身 path 跳转

**发现日期**: 2026-05-31

**问题描述**:
布局设置为“顶部菜单”时点击“流程管理”，会从目录路径 `/flow` 落到无匹配路由，再进入报表 SSO 桥，最终打开 `/report/design`。

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

## 6. 本地文件存储返回相对访问地址导致图片渲染失败

**发现日期**: 2026-05-14

**问题描述**:
`/api/file/url/{fileId}` 在本地存储场景下可能返回 `/api/file/download/{fileId}` 这种相对路径。
如果前端直接把这个值塞给 `img src` 或头像组件，浏览器会去当前站点根路径取资源，导致图片不显示或加载失败。

**解决方案**:
前端统一通过 `resolveFileAccessUrl()` 归一化访问地址，必要时补上 `VITE_REQUEST_PREFIX`。
图片加载失败时，再调用 `removeCachedFileAccessUrl()` 清理旧缓存后重试。

**影响范围**:
- 所有使用文件访问地址渲染图片的前端组件
- 头像、favicon、素材预览、图片上传回显等场景

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

## 7. SSE 流式对话前端解析不完整导致非实时输出

**发现日期**: 2026-05-15

**问题描述**:
智能体测试对话使用 `fetch + ReadableStream` 接收 SSE。如果前端只处理已经按空行切开的完整事件，但流结束时不 flush 剩余 `buffer`，或没有单次完成保护，页面可能表现为输出不稳定、结束后才刷新，或者重复触发完成状态。
另一个常见表现是数据已经追加到消息对象，但消息区域不重绘；窗口缩放或其他状态变化后才显示。常见原因包括：`n-scrollbar` 放在无明确高度的 flex/grid 容器中，滚动容器尺寸没有及时重算；或者把普通对象 push 到 `ref([])` 后，继续通过原始对象引用追加 chunk，未通过 Vue 代理对象触发重绘。

**正确用法**:
- SSE 解析必须支持 `\r\n` / `\n`，按事件块解析 `event:` 和多行 `data:`
- `reader.read()` 返回 `done=true` 时，要先 `decoder.decode()` flush 解码器，再处理剩余 `buffer`
- `complete` / `[DONE]` / 流自然结束必须通过 `completeOnce` 保护，避免重复完成
- 生产代理场景下，后端流式接口应设置 `X-Accel-Buffering: no`，避免 Nginx 缓冲导致前端一次性收到完整响应
- 流式消息对象需要使用 `reactive({...})`，或 push 后取数组中的代理对象再追加内容，避免原始对象引用变更不触发界面更新
- 对话消息流建议参考 `flow/design.vue`：使用原生滚动容器、底部锚点、`nextTick + requestAnimationFrame` 后置滚动，并给父级面板明确高度

**影响范围**:
- 所有基于 SSE 的 AI 流式输出功能
- 智能体测试、AI 代码生成、AI 流程生成等页面

---

## 6. SSO 接口缺少 `@ApiDecrypt` 会导致请求参数为空

**发现日期**: 2026-05-13

**问题描述**:
admin-ui / report-ui 的请求拦截器默认会对未排除的 `POST` JSON 请求体做 SM4/AES 加密。如果后端 SSO 接口仍按普通 `@RequestBody` 接收、但没有补 `@ApiDecrypt`，后台拿到的业务字段会是空值，表现为：

```text
java.lang.RuntimeException: 目标客户端不能为空
```

或：

```text
java.lang.RuntimeException: SSO票据不能为空
```

**典型场景**:
- `POST /auth/sso/ticket`
- `POST /auth/sso/exchange`

**正确用法**:
```java
@ApiDecrypt
@PostMapping("/sso/ticket")
public RespInfo<SsoTicketResult> createSsoTicket(@RequestBody SsoTicketRequest request) {
    ...
}

@SaIgnore
@IgnoreTenant
@ApiDecrypt
@PostMapping("/sso/exchange")
public RespInfo<LoginResult> exchangeSsoTicket(@RequestBody SsoExchangeRequest request) {
    ...
}
```

**根本原因**:
- admin-ui 默认只排除了 `/auth/login`、`/auth/captcha`、`/crypto/exchange` 等少数路径
- report-ui 也会对 `/forge-report-api/auth/sso/exchange` 默认加密
- 没有 `@ApiDecrypt` 时，请求体实际是 `{ data, algorithm }`，不会映射到 `targetClient` / `ticket`

**解决方案**:
- 对接前端默认加密链路的 SSO `POST` 接口必须补 `@ApiDecrypt`
- 不要优先用前端 `encrypt: false` 规避，除非这个接口明确约定为明文

**影响范围**:
- admin-ui -> report-ui 单点登录
- 所有新增的跨端票据交换、临时令牌类接口

---

## 7. Blob 下载响应被统一错误拦截器误判为未知异常

**发现日期**: 2026-05-15

**问题描述**:
`generator/table` 点击“生成”下载代码包时，后端 `/generator/download/{tableName}` 正常返回 zip 二进制流，但前端报：

```javascript
{ code: undefined, message: '【undefined】: 未知异常!', error: undefined }
```

**根本原因**:
`src/utils/http/interceptors.js` 的响应拦截器在判断 `content-type` 前无条件读取 `response.data.code`。下载接口返回的是 `Blob`，`Blob.code` 为 `undefined`，于是被当成业务异常。

**解决方案**:
响应拦截器必须先识别 `Blob` / `ArrayBuffer` 等二进制响应，直接返回数据；只有 JSON 响应才走 `RespInfo.code` 判断。若 `responseType: 'blob'` 下服务端返回 JSON 错误 Blob，应先解析 Blob，再进入统一错误处理。

**影响范围**:
- 所有通过 `request.get(..., { responseType: 'blob' })` 下载文件的前端功能
- 代码生成、流程图、附件下载等返回非 JSON 的接口

---

## 记录规范

每次发现新的踩坑点，按以下格式添加：

```markdown
## N. 问题标题

**发现日期**: YYYY-MM-DD

**问题描述**:
简述遇到的问题和错误现象。

**错误示例**:
展示错误的代码或配置。

**正确用法**:
展示正确的代码或配置。

**根本原因**:
解释为什么会出错。

**解决方案**:
说明如何避免和修复。

**影响范围**:
说明哪些场景会受影响。
```

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

## 7. X-Inner-Call 只能由可信内部系统配置触发

**发现日期**: 2026-05-11

**问题描述**:
外部接口代理如果允许接口自定义请求头直接传 `X-Inner-Call: true`，调用 Forge 服务时可能绕过 API 加解密和重放校验。

**根本原因**:
`forge-starter-crypto` 在请求头 `X-Inner-Call=true` 时会跳过请求解密、响应加密和重放校验，这个头属于服务间内部调用信任边界，不应由普通接口配置任意设置。

**解决方案**:
在 `sys_external_system` 增加 `trusted_internal` 配置。只有该配置为 true 时，`ExternalProxyService` 才主动添加 `X-Inner-Call: true`；同时过滤接口自定义请求头里的 `X-Inner-Call`。

**影响范围**:
- 外部系统配置
- 外部接口代理调用
- 所有调用 Forge 内部服务并依赖 `X-Inner-Call` 跳过加解密的场景

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

## 9. 跨系统 SSO 首跳前的 `/crypto/exchange` 必须匿名放行

**发现日期**: 2026-05-13

**问题描述**:
从 `admin-ui` 单点进入 `report-ui` 时，目标系统会先请求 `/crypto/public-key` 和 `/crypto/exchange` 建立会话密钥，再调用 `/auth/sso/exchange`。如果 `/crypto/exchange` 仍要求已登录，会直接报：

```text
未登录异常：未能读取到有效 token，请求地址：/crypto/exchange
```

**根本原因**:
`report-ui` 的 SSO 登录页在拿到目标系统 token 之前，也要先走 API 加密链路。此时密钥交换接口只能依赖匿名 `X-Session-Id` 建立临时会话，不能被 `SaTokenConfig` 的登录拦截器拦住。

**解决方案**:
- 在 `forge-starter-auth` 的 Sa-Token 白名单里显式排除 `/crypto/public-key`、`/crypto/exchange`
- 在 `KeyExchangeController.exchangeKey` 上补 `@SaIgnore`，明确该接口允许匿名密钥协商

**影响范围**:
- `admin-ui -> report-ui` 单点登录
- 所有“未登录先协商动态密钥，再换发 token”的跨系统接入场景

## 10. SSE 流式响应解析不能只按 `\n\n` 分割

**发现日期**: 2026-05-15

**问题描述**:
AI 智能体流式接口后端日志已经持续输出，但前端对话框没有任何内容。前端 SSE 解析只按 `\n\n` 切分事件时，如果 WebFlux 或代理链路输出 `\r\n\r\n`，数据会一直留在 buffer 中，不会触发 `onChunk`。

**解决方案**:
前端解析 SSE 时使用兼容 CRLF 的分隔和逐行解析：

```javascript
const events = buffer.split(/\r?\n\r?\n/)
for (const rawLine of block.split(/\r?\n/)) {
  const line = rawLine.trimEnd()
  if (line.startsWith('event:')) eventType = line.slice(6).trim()
  if (line.startsWith('data:')) dataLines.push(line.slice(5).replace(/^ /, ''))
}
```

同时处理 `[DONE]`、`event: done`、`event: error` 和最后未处理完的剩余 buffer，避免完成回调重复触发。

**影响范围**:
- `fetch + response.body.getReader()` 手写 SSE 解析的前端流式接口
- Spring WebFlux `ServerSentEvent` 通过本地代理转发的流式响应

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

## 13. sys_file_metadata 不是标准业务审计表

**发现日期**: 2026-05-18

**问题描述**:
通用文件表 `sys_file_metadata` 的建表脚本只包含 `create_time`、`update_time`、`uploader_id`、`upload_time` 等文件元数据字段，没有 `create_by`、`create_dept`、`update_by` 这些标准业务表审计字段。

**解决方案**:
从 `sys_file_metadata` 做迁移脚本时，不要直接引用 `create_by` / `create_dept` / `update_by`。需要创建业务表审计字段时，优先用 `uploader_id` 映射创建人/更新人，用 `upload_time` 映射创建时间，`create_dept` 置空或按业务上下文单独补齐。

**影响范围**:
- 文件表迁移到业务表的 SQL
- 报表素材、通知附件等以文件 ID 关联业务数据的场景

## 14. forge-report-ui 图标必须先注册到统一 icon 插件

**发现日期**: 2026-05-19

**问题描述**:
给大屏编辑器顶部“版本”按钮加图标时，组件里直接从 `icon.ionicons5` 解构 `TimeOutlineIcon` 使用。由于 `forge-report-ui/src/plugins/icon.ts` 没有导入并导出 `TimeOutlineIcon`，运行时拿到的是 `undefined`，按钮前面只出现空白占位，没有真实 SVG。

**错误示例**:
```ts
const { TimeOutlineIcon } = icon.ionicons5
```

但 `icon.ts` 中未注册：
```ts
const ionicons5 = {
  // 缺少 TimeOutlineIcon
}
```

**解决方案**:
在使用 `icon.ionicons5` 或 `icon.carbon` 中的图标前，必须先确认该图标已经在 `forge-report-ui/src/plugins/icon.ts` 中完成两步注册：

```ts
import {
  TimeOutline as TimeOutlineIcon,
} from '@vicons/ionicons5'

const ionicons5 = {
  TimeOutlineIcon,
}
```

如果只在单个组件内使用，也可以直接从 `@vicons/ionicons5` 导入，避免经过统一插件时漏注册。

**影响范围**:
- `forge-report-ui` 所有通过 `icon.ionicons5` / `icon.carbon` 使用图标的组件
- 编辑器顶部按钮、项目卡片下拉菜单、项目详情弹窗操作按钮
- 所有表现为“图标位置有空白但没有图标”的 Naive UI 按钮/菜单

## 15. Flyway 已执行版本脚本不能二次修改

**发现日期**: 2026-05-19

**问题描述**:
应用启动时报错：

```text
FlywayValidateException: Migration checksum mismatch for migration version 1.0.1
```

典型表现是 `forge_schema_history` 中已经记录了 `V1.0.1` 的 checksum，但本地 `forge/db/migration/V1.0.1__*.sql` 又被修改，Flyway 在执行新版本前先做校验，因此后续 `V1.0.2` 不会继续执行。

**根本原因**:
Flyway 的版本化 migration 是不可变变更记录。脚本一旦在任意数据库执行成功并写入 `forge_schema_history`，后续改文件内容就会造成数据库记录的 checksum 与本地解析出的 checksum 不一致。

**解决方案**:
- 已执行过的 `Vx.y.z__*.sql` 禁止继续编辑。
- 需要修正表结构、菜单、初始化数据时，新增更高版本脚本，例如 `V1.0.3__fix_dashboard_version_menu.sql`。
- 如果确实要把当前本地脚本作为数据库认可版本，必须明确确认数据库现状无误后执行 `flyway repair`，或等价更新 `forge_schema_history`，这是数据库操作，不能作为常规开发手段。
- 排查“新脚本不执行”时，先看启动日志是否有 `Validate failed`，它通常说明卡在旧版本校验，不是 `locations` 没扫到新脚本。

**影响范围**:
- `forge/db/migration` 下所有 Flyway 版本化 SQL
- 所有启动时依赖 `forge-admin-server` 自动迁移的本地、测试、生产数据库

## 16. 动态查询 SQL 注入检测误判字段名

**发现日期**: 2026-05-19

**问题描述**:
`DynamicQueryGenerator.containsSqlInjection()` 原正则直接匹配 `and` / `or` 等关键字子串，导致 `sort_order`、`order_no` 这类合法字段被误判为 SQL 注入字段，自定义查询和排序会静默跳过这些字段。

**解决方案**:
SQL 关键字匹配必须使用单词边界，只拦截独立关键字或危险字符：

```java
".*(?:\\b(?:insert|update|delete|drop|truncate|exec|execute|union|select|into|from|where|and|or)\\b|--|;|'|\"|\\\\).*"
```

**影响范围**:
- 动态 CRUD 搜索和排序
- 自定义查询字段选择、条件构造和排序

## 17. Vite 懒加载依赖二次预构建导致菜单点击后整页刷新

**发现日期**: 2026-05-19

**问题描述**:
开发环境点击某些前端菜单时，顶部进度条短暂停住，随后页面整页刷新。`forge-admin-ui/server.log` 中可看到：

```text
[vite] (client) ✨ new dependencies optimized: ...
[vite] (client) ✨ optimized dependencies changed. reloading
```

**根本原因**:
部分依赖只在懒加载页面中首次出现，Vite dev server 首次进入这些页面时才发现需要预构建依赖，完成后会触发客户端全量 reload。看起来像菜单点击导致页面强制刷新，但实际是 Vite 开发环境的依赖优化重载。

**解决方案**:
把懒加载页面会用到的重依赖和日志中出现的依赖加入 `forge-admin-ui/vite.config.js` 的 `optimizeDeps.include`，让 Vite 启动时提前预构建，避免菜单点击过程中二次优化。

**影响范围**:
- `forge-admin-ui` 开发环境
- 首次访问使用 `@vicons/ionicons5`、`vue3-slide-verify`、`vue3-intro-step`、`bpmn-js`、`echarts`、`marked`、`highlight.js` 等依赖的懒加载页面

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

## 20. 前端生产构建默认 Node 堆内存不足

**发现日期**: 2026-05-27

**问题描述**:
`forge-admin-ui` 执行 `pnpm build` 时，在 Vite `rendering chunks` 阶段可能出现：

```text
FATAL ERROR: Ineffective mark-compacts near heap limit Allocation failed - JavaScript heap out of memory
```

**解决方案**:
使用 Node 20.19.0 并显式提高构建堆内存：

```bash
source ~/.nvm/nvm.sh && nvm use v20.19.0
NODE_OPTIONS=--max-old-space-size=8192 pnpm build
```

**说明**:
构建中出现的 UnoCSS 图标加载警告和少量 CSS `//` 注释警告当前不阻断产物生成；优先区分 OOM 和真实语法错误。

## 21. 菜单路径与文件自动路由不一致导致 404

**发现日期**: 2026-05-27

**问题描述**:
`sys_resource.path` 配置为 `/app-center/engines`、`/app-center/suite/:suiteCode` 等业务路由，但前端页面文件使用 `engine-center.vue`、`suite-detail.vue` 等命名时，`unplugin-vue-router` 自动生成的路由分别是 `/app-center/engine-center`、`/app-center/suite-detail`，菜单点击会进入 404。

**解决方案**:
- 优先让页面文件路径匹配菜单路径，例如 `src/views/app-center/engines.vue`；动态二级路径用 `dotNesting` 扁平命名，例如 `src/views/app-center/suite.[suiteCode].vue` 对应 `/app-center/suite/:suiteCode`。
- 不要为业务菜单长期在 `src/router/index.js` 手写路由；该文件只保留白名单、兼容动态路由、SSO 桥接等少量特殊路由。
- 父级入口有子菜单时优先建成目录资源，另建可点击首页菜单，避免父菜单既当目录又当页面入口。

**影响范围**:
- 使用 `sys_resource` 创建的新业务菜单
- 文件自动路由和动态参数页面

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

## 23. 菜单活跃项函数签名不一致导致选中状态停留

**发现日期**: 2026-05-27

**问题描述**:
在 `top-side-menu` 布局下，先点击“应用总览”，再点击同级的“引擎中心 / 移动端中心 / 集成中心”，页面可以跳转，但菜单选中状态仍停留在“应用总览”。

**根本原因**:
`useMenu()` 返回的 `findMenuIdByPath` 只支持 `(targetPath)`，内部固定从全量菜单查找；`top-side-menu/components/SideMenu.vue` 按 `(sideMenuOptions, route.path)` 调用。第二个参数被忽略后，活跃 key 算不出真实菜单 ID，Naive Menu 受控 `value` 失效，视觉上保留上一次选中项。

同时，数据库里的顶层“应用中心”目录如果残留 `/app-center` 路径、`app-center/index` 组件或 `ai:businessApp:list` 权限，会和子级“应用总览”语义重叠，增加 `/app-center` 抢占选中的概率。

**解决方案**:
- `useMenu()` 暴露的 `findMenuIdByPath` 兼容 `(targetPath)` 和 `(items, targetPath)` 两种调用方式。
- 路径匹配先用 `normalizeLocalPath()` 统一前导斜杠，空路径不参与匹配。
- 新增 Flyway 迁移把顶层“应用中心”规范为纯目录：`path/component/perms = NULL`；真正页面入口保留在子菜单“应用总览”。

**影响范围**:
- `normal`、`top-side-menu`、`immersive` 等依赖 `useMenu()` 的菜单布局
- 应用中心这类“目录 + 默认页 + 同级子页”的菜单结构

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

## 25. 后端 Maven 编译必须使用 JDK 17

**发现日期**: 2026-05-28

**问题描述**:
执行后端编译时，如果当前 shell 使用的不是 JDK 17，会在编译阶段失败：
```text
Fatal error compiling: 无效的目标发行版: 17
```

**解决方案**:
本机可显式指定 OpenJDK 17 后再执行 Maven：
```bash
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home \
PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH \
mvn -pl forge-admin-server -am compile -DskipTests
```

**影响范围**:
- `forge-admin-server` 及其 Maven reactor 编译
- 所有 target/source 配置为 Java 17 的后端模块

## 26. Flyway 已执行版本禁止复用或改写

**发现日期**: 2026-05-28

**问题描述**:
启动 `forge-admin-server` 时，Flyway 校验失败：
```text
Migration checksum mismatch for migration version 1.0.32
Migration checksum mismatch for migration version 1.0.33
```

**根本原因**:
数据库 `forge_schema_history` 已经记录过对应版本的 checksum，但本地 `forge/db/migration/V1.0.32__*.sql` 或 `V1.0.33__*.sql` 内容后来被修改、重新生成或复用了同一个版本号。Flyway 会把它视为历史迁移被篡改，启动阶段直接失败。

**解决方案**:
- 已落库的 Flyway 脚本禁止修改，新增修正必须使用下一个版本号。
- 本地开发库如果确认为临时脚本迭代，可删除从首次变更版本开始的连续尾部 `forge_schema_history` 记录后重跑；不要只删除中间版本。
- `flyway repair` 只更新 schema history checksum，不会重新执行脚本；如果脚本内容包含新的 seed/update 数据，优先重跑迁移或新增后续版本脚本。

**影响范围**:
- 所有 `forge/db/migration/V*.sql` 版本化脚本
- 远程共享开发库、测试库和生产库的 Flyway 启动校验

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

## 43. Flyway 迁移 tenant_id 0 归一化前必须先处理唯一键重复

**发现日期**: 2026-06-04

**问题描述**:
多租户迁移中把历史 `tenant_id = 0` 统一改为默认租户 `1` 时，开发库启动失败：

```text
Validate failed: Detected failed migration to version 1.0.56
Duplicate entry '1-sys_notice_status' for key 'sys_dict_type.uk_tenant_dict_type'
```

**根本原因**:
`sys_dict_type` 同时存在 tenant 0 和 tenant 1 的同名 `dict_type`，直接执行 `UPDATE ... SET tenant_id = 1` 会撞上租户内唯一键。失败后 Flyway 会保留 `success = 0` 的历史记录，后续启动会先卡在 validation。

**解决方案**:
1. 确认失败迁移尚未成功落库，先删除或 repair 对应的失败 history 行。
2. 在迁移脚本的归一化语句前增加去重逻辑，删除 tenant 0/null 中已被 tenant 1 覆盖的重复字典类型和字典数据。
3. 重新通过应用启动或 Flyway migrate 执行迁移，让 `forge_schema_history` 正常记录 `success = 1`。

**影响范围**:
- 所有把 `tenant_id IS NULL OR tenant_id = 0` 回填到默认租户 `1` 的 Flyway 脚本。
- 带租户内唯一键的表，例如字典、角色编码、流程 key、业务自然键等。

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

## 49. 应用入口套件目录父级不能回填为实际菜单 ID

**发现日期**: 2026-06-04

**问题描述**:
应用入口勾选“同步为菜单 + 套件作为父级目录”时，`adminMenu.parentId/originalParentId` 表示套件目录的上级，`adminMenu.actualParentId/suiteMenuResourceId` 表示已生成的套件目录自身 ID。如果前端回显或保存时把实际套件目录 ID 写回 `parentId/originalParentId`，后端复用套件目录菜单时会把 `sys_resource.parent_id` 更新成自己的 `id`，菜单树无法正常渲染。

**解决方案**:
应用入口保存前必须过滤 `actualParentId/suiteMenuResourceId/menuResourceId`，不能把这些已占用资源 ID 作为套件目录上级。后端同步菜单时也必须归一旧污染配置：当原始父级等于套件目录自身 ID 或应用菜单自身 ID 时，按顶级挂载处理；菜单适配器层还要兜底防止 `parentId == resourceId` 写入数据库。

**影响范围**:
- `ai_business_app.options.adminMenu.parentId/originalParentId`
- `ai_business_app.options.adminMenu.actualParentId/suiteMenuResourceId`
- `sys_resource.parent_id`
- 应用入口编辑抽屉和动态挂载菜单树

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

## 55. Flyway 低版本补脚本会被默认校验拦截

**发现日期**: 2026-06-06

**问题描述**:
启动时报 `jobAutoRegistrar -> jobScheduler -> flywayInitializer` 依赖创建失败，真正根因是 Flyway 校验失败：

```text
Detected resolved migration not applied to database: 1.0.55
Detected resolved migration not applied to database: 1.0.56
```

**根本原因**:
开发库 `forge_schema_history` 已经存在更高版本，例如 `1.0.57/1.0.58`，但本地后来新增或恢复了更低版本的迁移脚本。Flyway 默认 `outOfOrder=false`，会拒绝这种低版本补迁移。

**解决方案**:
先查 `forge_schema_history` 确认缺失版本和已执行高版本；再确认缺失脚本具备重复执行保护。对已经出现历史缺口的开发库，用 Flyway `outOfOrder=true` 正式补跑一次迁移，让历史表记录缺失版本。补跑后再用默认配置执行 Flyway validate，确认 `validationSuccessful=true`。

**注意**:
不要手工插入 `forge_schema_history`，不要修改已经执行过的迁移脚本。后续新增迁移必须继续按当前最高版本顺延，不能再补低版本脚本。

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

## 57. 运行时异常返回给前端前必须去掉异常类名前缀，前端错误提示不能只剩一句 message

**发现日期**: 2026-06-10

**问题描述**:
登录或业务接口报错时，前端有时会直接收到 `com.mdframe.forge.starter.core.exception.BusinessException: 验证码错误或已过期` 这类字符串，既暴露了后端异常类名，也影响用户阅读。与此同时，前端全局错误提示只有一条 `message.error`，缺少请求路径、错误码、原始响应等排查信息，线上定位成本高。

**根本原因**:
部分业务代码会用 `RuntimeException` 包装 `BusinessException` 或其他异常，进入全局运行时异常处理后如果只拿 `getMessage()` 返回，就可能把 `xxxException:` 前缀一并透出。前端响应拦截器又只消费摘要 message，没有统一的“查看详情”入口，导致开发排查时只能靠控制台或后端日志。

**解决方案**:
后端 `GlobalExceptionHandler` 处理 `RuntimeException` 时，先递归解包 `BusinessException` cause，并对返回给前端的 message 做统一清洗，去掉 `com.xxx.BusinessException:`、`java.lang.RuntimeException:` 这类异常类名前缀。前端全局 HTTP 错误处理不要只弹一句 message，应该统一弹错误对话框，默认展示摘要文案，并提供“查看详情”展开区，至少包含错误码、请求方法、请求 URL、traceId 和服务端原始响应。

**影响范围**:
- `forge-server/.../GlobalExceptionHandler`
- `forge-admin-ui/src/utils/http/interceptors.js`
- `forge-admin-ui/src/utils/http/helpers.js`
- 所有依赖统一 axios 拦截器的页面，包括登录页

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

## 59. forge-create minimal-admin 保留 generator 时必须提供 AI 降级适配器并补齐依赖

**发现日期**: 2026-06-17

**问题描述**:
使用 `forge-create --preset minimal-admin` 生成最小后端工程后，启动可能报 `AiCrudConfigGenerateService required a bean of type AiClientAdapter that could not be found`。补上适配器后还可能在 `lingxi-plugin-generator` 编译阶段报缺少 `plugin-message`、`starter-job`、`starter-file`、`starter-excel`、`starter-id` 或 `flow-client` 相关类型。

**根本原因**:
`minimal-admin` 默认保留 `plugin-generator` 但不包含 `plugin-ai`。脚手架裁剪后删除了 admin-server 中原本依赖 `plugin-ai` 的 `AiClientAdapterImpl`，而 `AiCrudConfigGenerateService` 又强制构造注入 `AiClientAdapter`。同时 `scripts/forge-create/module-catalog.json` 中 `plugin-generator.dependencies` 没有覆盖其 POM 和源码直接引用的全部编译依赖，导致生成工程的 POM 被裁剪过头。

**解决方案**:
当选择了 `plugin-generator` 但未选择 `plugin-ai` 时，脚手架应生成一个不依赖 `plugin-ai` 的 `AiClientAdapterImpl` 降级实现：同步 AI 调用返回 fallback，流式调用返回 `Flux.empty()`。同时 `plugin-generator` 的模块目录依赖必须包含 `starter-datascope`、`starter-excel`、`starter-file`、`starter-job`、`starter-id`、`plugin-message` 和 `flow-client`，避免生成工程缺编译依赖。

**验证建议**:
修复 forge-create 裁剪逻辑后，必须重新生成临时 `minimal-admin` 工程，检查生成后的 `admin-server` 存在降级 `AiClientAdapterImpl` 且没有引用 `plugin-ai`，再执行 `mvn -pl <project>-admin-server -am compile -DskipTests` 或 `package -DskipTests`。

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

## 61. window.$message.loading 不返回 Naive 原生销毁句柄

**发现日期**: 2026-06-17

**问题描述**:
页面里写 `const loading = window.$message.loading('处理中...', { duration: 0 })` 后，再调用 `loading.destroy()` 会报错或导致后续逻辑中断。典型现象是接口已经处理完成，但确认弹窗和 loading 提示仍停留在页面上。

**根本原因**:
`window.$message` 不是 Naive UI 原生 `message`，而是 `src/utils/naiveTools.js` 中 `setupMessage()` 包装后的对象。包装类的 `loading()`、`success()`、`error()` 等方法只调用 `showMessage()`，没有把 Naive 原生 `MessageReactive` 返回出去。因此不能依赖 `window.$message.loading()` 的返回值销毁消息。

**解决方案**:
需要手动关闭 loading 时，必须使用固定 `key` 并调用包装对象的 `destroy(key, duration)`：

```js
window.$message.loading('处理中...', { key: 'xxx-loading', duration: 600000 })
try {
  // await request...
}
finally {
  window.$message.destroy?.('xxx-loading', 0)
}
```

若 loading 放在 `window.$dialog` 的 `onPositiveClick` 中，还应避免 finally 中抛错；必要时保存 dialog reactive 并在请求结束后显式 `dialog.destroy()`，否则 Promise reject 时 Naive Dialog 不会自动关闭。

## 62. Vite dev server 启动报 EMFILE: too many open files

**发现日期**: 2026-06-20

**问题描述**:
本地启动 `forge-admin-ui` 的 Vite 预览服务时，Chokidar 可能因为监听文件过多报错：

```text
Error: EMFILE: too many open files, watch
```

**解决方案**:
启动前提高文件句柄上限，并启用 polling：

```bash
ulimit -n 65535
source ~/.nvm/nvm.sh && nvm use v20.19.0
CHOKIDAR_USEPOLLING=true pnpm --dir forge-admin-ui exec vite --host 127.0.0.1 --port 5188 --strictPort true
```

**适用场景**:
- 临时 UI 预览页验证。
- Vite dev server 在 macOS 上启动 watcher 失败。

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

## 71. 异步日志补全用户信息必须忽略租户条件

**发现日期**: 2026-06-22

**问题描述**:
开启租户业务数据源异步上下文传播后，操作日志异步线程可能出现：

```text
DefaultTenantLineHandler - 当前上下文中没有租户ID，请检查租户设置
SystemLogServiceImpl.saveOperationLog - sysUser is null
```

**根本原因**:
日志服务根据 `userId` 补全 `SysUser` 信息时，属于系统级主库查询。如果直接执行 `sysUserMapper.selectById`，会被当前线程的租户拦截器追加 `tenant_id` 条件；异步线程没有租户 ID 或租户上下文被切到业务场景时，就会查不到用户并触发空指针。

**解决方案**:
- 日志、在线用户、登录态补全等系统用户查询使用 `TenantContextHolder.executeIgnore`。
- 查询结果必须做 null 保护，查不到用户时保留日志原始信息并继续落库。
- 线程池 `TaskDecorator` 传播租户上下文时需要同时传播并恢复 `TenantContextHolder.isIgnore()` 状态。

**影响范围**:
- `SystemLogServiceImpl`
- `TenantBusinessDataSourceTaskDecorator`

## 72. 租户业务数据源切换后数据权限不能访问业务库里的平台表

**发现日期**: 2026-06-22

**问题描述**:
业务 Mapper 使用租户默认业务数据源后，数据权限拦截器可能报错：

```text
Table 'forge_admin_test.sys_data_scope_config' doesn't exist
```

**根本原因**:
`DataScopeInterceptor` 在拦截业务 Mapper 时需要读取 `sys_data_scope_config` 等数据权限控制面配置。如果这一步发生在 dynamic-datasource 已经切到租户业务库之后，平台配置查询会被错误路由到业务库。

类似问题不只限于 `sys_data_scope_config`：角色数据范围、自定义组织、组织子级和行政区划子查询也可能访问 `sys_role`、`sys_role_data_scope`、`sys_org`、`sys_region_code` 等平台表。

**解决方案**:
- 数据权限控制面元数据固定从平台主库加载，默认使用 `forge.datascope.metadata-datasource=master`。
- 启动和配置刷新时预加载数据权限配置、角色范围、组织层级和行政区划父子关系到内存快照。
- 业务 Mapper 查询期间只读取内存快照，不在拦截器里实时访问平台表。
- REGION 权限不要生成 `IN (SELECT code FROM sys_region_code ...)`，应提前解析为业务库可执行的字面量 `IN (...)` 条件。

**影响范围**:
- `DataScopeServiceImpl`
- `DataScopeInterceptor`
- 租户业务数据源下所有带数据权限控制的业务 Mapper

## 73. 超级管理员区划树被登录用户 regionCode 误裁剪

**发现日期**: 2026-06-23

**问题描述**:
超级管理员打开用户/组织编辑表单时，行政区划树只能看到当前登录用户 `regionCode` 对应范围，例如只能选内蒙古，无法选择其他省份。

**根本原因**:
`/system/region/treeAll?dataRight=true` 在进入数据权限拦截器之前，服务层先用当前登录用户 `regionCode` 解析默认 `rootCode`。即使数据权限拦截器对超级管理员是 `ALL` 放行，SQL 仍然已经被 `rootCode` 条件限制。

**解决方案**:
- 超级管理员请求区划树时，不启用默认 `regionCode` 根节点裁剪，直接走无数据权限查询。
- 普通用户和租户用户继续按当前行政区划/角色数据权限过滤。

**影响范围**:
- `SysRegionServiceImpl.selectRegionTreeAll`
- 用户/组织编辑表单中的行政区划树

## 74. Spring Boot 3.5 与 Redisson 3.34.1 会触发登录 Redis 适配死循环

**发现日期**: 2026-06-24

**问题描述**:
`/auth/login` 登录链路使用 Redis/Redisson 时可能报 `StackOverflowError`，表现为请求进入 Redis 过期时间相关命令后死循环。

**根本原因**:
Forge 后端升级到 Spring Boot 3.5.x 后，Spring Data Redis 3.5 的 `RedisKeyCommands` 新增了 `pExpire(byte[], Expiration, ExpirationOptions)` 签名。Redisson 3.34.1 的 `redisson-spring-boot-starter` 仍解析到 `redisson-spring-data-33`，该适配层只覆盖旧接口签名，新接口 default 方法会与 Redisson 旧实现互相转发，最终栈溢出。

**解决方案**:
- Spring Boot 3.5.x / Spring Data Redis 3.5.x 必须使用解析到 `redisson-spring-data-35` 的 Redisson 版本。
- 本项目已将 `redisson.version` 从 `3.34.1` 升级到 `3.50.0`，依赖树应显示 `org.redisson:redisson-spring-data-35:3.50.0`。
- 排查类似 Redis 命令栈溢出时，优先执行 `mvn -pl forge-framework/forge-starter-parent/forge-starter-cache -am dependency:tree -Dincludes=org.redisson -DskipTests` 查看适配层是否匹配 Spring Data Redis 小版本。

**影响范围**:
- `/auth/login`
- `forge-starter-cache`
- 所有通过 Redisson/Spring Data Redis 执行 Redis 过期时间命令的链路

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

## 91. MySQL 唯一索引遇到 NULL 不能作为幂等防线

**发现日期**: 2026-07-03

**问题描述**:
动作执行日志用 `tenant_id + object_code + record_id + action_code + idempotency_key` 做唯一键防重复提交时，如果 `record_id` 允许为 `NULL`，MySQL 唯一索引会允许多条 `NULL` 组合记录，导致同一幂等键仍可能重复写入和重复执行。

**解决方案**:
- 幂等唯一键中的业务维度字段尽量设为 `NOT NULL DEFAULT ''`，服务端也要把空值归一化为空字符串。
- 执行业务副作用前先以独立事务写入 `RUNNING` 预占日志，再进入真实步骤事务。
- 重复请求命中 `RUNNING`、`FAILED` 等既有幂等记录时只返回/抛出对应结果，不要再尝试写一条新的失败日志。

**影响范围**:
- 动作执行日志、数量流水、锁定记录等所有依赖数据库唯一键实现幂等的表。
- 任何包含可空业务 ID、详情 ID、来源 ID 的唯一索引设计。

## 92. 根 POM 固定 skip 会让定向测试看起来通过但实际未执行

**发现日期**: 2026-07-03

**问题描述**:
执行 `mvn -Dtest=SomeTest test -DskipTests=false -Dmaven.test.skip=false` 时，如果根 POM 的 compiler/surefire 插件配置固定读取项目属性并默认 skip，Maven 日志可能显示构建成功，但实际出现 `Not compiling test sources`、`Tests are skipped`，新增测试没有运行。

**解决方案**:
- 先看 Maven 日志里是否有 `T E S T S` 和具体 `Tests run` 汇总，不能只看 `BUILD SUCCESS`。
- 为项目提供显式启用测试的 profile，例如 `-Penable-tests` 同时打开 testCompile 和 surefire。
- 如果启用测试后被历史坏测试阻断，先在 profile 中临时 exclude 无关旧测试，并在执行日志中说明原因，避免本变更的定向测试继续被跳过。

**影响范围**:
- `forge-server` 后端 Maven 定向单测。
- SDD `/test`、阶段收尾验证、Review 修复验证和归档前验收。

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

## 102. 超级管理员全量组织兜底不能当成真实绑定组织

**发现日期**: 2026-07-06

**问题描述**:
超级管理员只在 `sys_user_org` 显式绑定了一个组织，例如“内蒙古分公司”，但顶部当前组织下拉仍显示当前数据中心全部组织。

**根本原因**:
为兼容历史无组织绑定的超级管理员，登录态加载可能把当前数据中心全量组织写入 `LoginUser.orgIds` 作为兜底上下文。如果 `/system/org/current/options` 或 `/system/org/switch` 直接信任这个 `orgIds`，就无法区分“用户真实绑定了全量组织”和“历史超级管理员兜底填充了全量组织”。

**解决方案**:
组织切换选项和切换校验必须重新查询 `sys_user_org` 判断真实绑定关系：

- 有显式绑定时，只返回和允许切换绑定组织。
- 无任何显式绑定且当前用户是超级管理员时，才保留当前数据中心全量组织兜底。
- 相关查询写入 Mapper XML，避免在 Service 层新增复杂查询。

**影响范围**:
- `SysOrgServiceImpl#selectCurrentUserOrgOptions`
- `SysOrgServiceImpl#switchCurrentOrg`
- 当前组织切换器、登录态组织上下文和超级管理员历史账号兼容

## 103. AI/MCP 机器调用的数据权限必须按执行器 fail-closed

**发现日期**: 2026-07-10

**问题描述**:
规划 AI 中枢或 MCP 能力出口时，如果笼统认为所有业务查询都会经过 `DataScopeInterceptor`，会错误高估现有数据权限边界。动态 CRUD 使用 `NamedParameterJdbcTemplate`，实际通过 `DynamicDataScopeService` 构造数据范围条件；普通 MyBatis Mapper 才会进入 `DataScopeInterceptor`。后者在用户上下文获取失败、上下文为空、未知数据范围或 SQL 改写异常时存在跳过/放行分支，适合部分后台兼容场景，但不能作为外部机器调用的最终安全边界。

另一个同源风险是部分业务服务在缺少 Session 租户时会回退默认租户 `1`，例如 `BusinessActionExecutionService#resolveTenantId`。机器身份链路如果没有先建立完整用户、租户和当前组织上下文，可能把缺失上下文误变成默认租户调用。

此外，`DynamicDataScopeService` 当前 REGION 条件仍会在运行时业务 SQL 中子查询 `sys_region_code`。租户业务数据源不包含平台控制面表时会执行失败，也与“数据权限控制面元数据固定在平台主库”的项目决策冲突。

**解决方案**:
- 外部能力统一先校验机器客户端、服务账号、租户、当前组织和授权交集，任何上下文缺失均拒绝。
- 动态低代码查询显式向 `DynamicDataScopeService.buildCondition(..., explicitContext)` 传入已验证的数据权限上下文。
- REGION 范围必须从平台主库的数据权限快照/缓存预解析成区域编码集合，业务 SQL 只使用参数化 `IN`，禁止子查询 `sys_region_code`。
- MyBatis 能力适配器启用 capability 专用 fail-closed 模式；上下文缺失、Mapper 权限配置缺失、未知范围和 SQL 改写失败都必须抛错。
- 代码业务 Provider 必须声明并执行对象级权限策略，能力层先校验、Provider 内再校验。
- 机器调用进入业务 Service 前禁止默认租户兜底，不能把 `tenantId = 1` 当作认证失败的替代值。
- 客户端参数中的 `tenantId/userId/activeOrgId` 不能覆盖认证上下文。

**影响范围**:
- AI 中枢 Capability Registry 与 MCP Server。
- `DynamicCrudService` / `DynamicDataScopeService` 动态查询。
- 通过 MyBatis Mapper 暴露的流程、消息、API 和代码业务能力。
- `BusinessActionExecutionService` 等存在默认租户兼容逻辑的业务服务。

## 104. DashScope Core、Starter 和 Compatible 地址不能混为一体

**发现日期**: 2026-07-10

**问题描述**:
在多租户 AI 供应商系统中，如果直接引入 `spring-ai-alibaba-starter-dashscope`，或者只根据供应商品牌/Base URL 猜测协议，容易同时产生自动配置冲突和请求路径错误。官方根 README 的示例版本还可能落后于 release POM/BOM，不能作为最终依赖基线。

**根本原因**:
- DashScope Starter 的自动配置条件允许缺省启用，会尝试读取 `spring.ai.dashscope.*` 或环境变量创建全局模型 Bean，与 Forge 按租户从数据库动态读取 API Key 的模式冲突；
- DashScope Native 使用官方根地址和原生 generation path，OpenAI Compatible 使用 `/compatible-mode`，同一品牌不代表同一协议；
- 旧应用只会按 OpenAI Compatible 构建模型，无法理解新增的 Native Adapter；
- 项目 release 的真实版本关系由发布 POM/BOM 决定，README 示例可能仍保留旧版本号。

**解决方案**:
- 多租户动态凭据场景只依赖 `spring-ai-alibaba-dashscope` Core，通过运行时 Builder 创建模型，不引入 Starter；
- 用独立 `adapter_code` 显式区分 `openai_compatible` 与 `dashscope_native`，官方 DashScope 域名执行双向 URL 校验，不根据品牌或 URL 自动切协议；
- 历史记录保持 Compatible，切换 Native 必须由管理员显式操作并测试；
- 回退旧应用前先检查 Native 记录，逐条切回 Compatible URL/config 并通过连接测试；
- 依赖版本以 release POM/BOM 和实际 `dependency:tree` 为准。当前验证基线是 Spring AI `1.1.2`、Spring AI Alibaba/Extensions `1.1.2.3`。

**影响范围**:
- `forge-plugin-ai` 的供应商模型构建、连接测试和缓存；
- 多租户 API Key 配置；
- DashScope Native/Compatible 协议切换；
- AI 依赖升级与旧应用回退。

## 105. 路由关联查询转运行时实体时不能丢 tenantId

**发现日期**: 2026-07-11

**问题描述**:
策略候选 Mapper 已查询到模型和供应商，但把结果转换为 `AiProvider`/`AiModel` 时若只复制 ID、名称和连接字段，`ChatClientCache` 会因 Provider 缺少 tenantId 在模型请求前失败。仅依赖 TenantLine 拦截 SQL 也无法在 Router 中解释跨租户脏关联。

**解决方案**:
- 候选关联 SQL 显式返回 target/model/provider 三个 tenantId；
- Router 校验三者必须等于当前已验证租户，不一致候选记录 `TENANT_MISMATCH` 并跳过；
- 转换出的 Provider/Model 必须写入 tenantId，再进入健康键和 ChatClientCache；
- 增加“跨租户候选跳过、本租户候选携带 tenantId”的回归测试。

**影响范围**:
- 模型路由策略候选查询与运行时实体转换；
- ChatClientCache 多租户缓存键；
- HealthRegistry 的 `tenantId/providerPk/modelPk` 健康键。

## 106. AI 治理核心组件不能可选注入，非模型故障不能污染健康状态

**发现日期**: 2026-07-11

**问题描述**:
模型 Router、调用审计、失败分类、能力 Mapper、健康注册表或策略 Mapper 使用 `@Autowired(required = false)` 时，Bean 缺失会让系统静默退回旧解析链或跳过治理能力。与此同时，内容安全拒绝和调用方取消如果落入 `UNKNOWN`，会错误增加模型失败次数并触发熔断。

**解决方案**:
- Router、审计、失败分类、能力、健康和策略组件使用构造器强制注入，应用装配缺失时启动失败，禁止运行时静默降级；
- Resolver 只保留 Router 一条模型选择路径，不保留旧供应商/模型兜底；
- 失败分类遍历 cause chain，识别包装后的 timeout、network 和 `CancellationException`；
- 将 `content_filter`、`safety`、`content_policy_violation` 归类为 `CONTENT_POLICY`；
- `VALIDATION`、`CONTENT_POLICY`、`CANCELLED` 只结束 Lease，不增加模型健康失败；
- 同步失败、流式 ERROR/CANCEL 和准备阶段 abort 必须各自有“一次调用、一次审计、一次 Lease 终态”的回归测试。

**影响范围**:
- AI 模型路由与健康状态机；
- Spring Bean 装配和启动失败边界；
- 同步/流式调用审计；
- 内容安全拒绝、客户端取消和供应商异常分类。
