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
