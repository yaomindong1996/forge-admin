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
Flyway 执行 SQL 脚本时会处理 `${...}` 占位符，即使它出现在 SQL 字符串或注释里也可能触发 placeholder 解析。低代码 seed 脚本中如果把消息模板、流程标题模板写成 `${field}`，会被 Flyway 当成配置占位符，导致迁移失败。

**解决方案**:
Flyway 脚本中的内置模板优先使用 `{field}` 这类不触发 Flyway placeholder 的格式；运行时模板引擎可以兼容 `{field}` 和历史 `${field}`。如果必须保留 `${...}`，需要明确关闭或转义 Flyway placeholder，但项目 seed 脚本默认不走这条路。

**影响范围**:
- `forge/db/migration/` 中所有包含 JSON、消息模板、流程标题模板的 SQL 脚本
- 触发器动作配置、消息模板、流程变量标题模板初始化脚本

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
