# 踩坑：前端 / 构建 / 路由

> 从 `code-copilot/memory/pitfalls.md` 按主题拆出。新条目追加到本文件。共 40 条。

## uni-app 微信小程序不能直接复用 H5 Teleport 和动态 component 递归

**发现日期**: 2026-09-20

**问题描述**:
H5 头像裁剪器内部依赖 `Teleport`，动态数组渲染器使用 `<component :is="...">` 递归字段组件时，H5 可以编译，但微信小程序构建会因平台模板能力差异失败或生成不可运行的组件引用。仅在调用处做运行时判断仍会让小程序编译器扫描到这些模板。

**解决方案**:
H5 专用组件必须同时对模板节点和 import 使用 uni-app 条件编译；跨端递归渲染应使用已静态注册的组件引用，并通过 props 递归数据。新组件至少同时执行 H5 与 `mp-weixin` 生产构建，不能只依赖 H5 页面验证。

## Vitest 结构测试读取源码时 new URL 不能内联字面量路径

**发现日期**：2026-09-17

**问题描述**:
用户管理页拆分后新增结构测试，在测试文件里用 `new URL('../user/components/', import.meta.url)` 读取目录并断言 SFC 行数与样式命名空间。Vitest 经 Vite 转换测试文件时，会把 `new URL(<字面量相对路径>, import.meta.url)` 当成静态资源引用处理，运行时 URL 从 `file:` 被改写成 `http://localhost:...`，`readdirSync`/`readFileSync` 抛 `TypeError: The URL must be of scheme file`，结构测试失败但产品代码无问题。

**解决方案**:
把 URL 构造包进帮助函数再调用，让路径以变量形式传入，Vite 无法静态改写：

```js
function sourceUrl(relativePath) {
  return new URL(relativePath, import.meta.url)
}
```

也可改用 `fileURLToPath` 组合 `__dirname` 绝对路径。读取源码文件的结构测试统一走帮助函数，避免内联字面量。

## pnpm 在 forge-admin-ui 执行脚本必须加 --ignore-workspace

**发现日期**：2026-09-17

**问题描述**:
在 `forge-admin-ui` 目录执行 `pnpm dev`、`pnpm --dir forge-admin-ui exec vitest ...` 等命令时报 `ERROR  packages field missing or empty`，命令直接退出；Vitest、生产构建、启动 dev server 全部被卡住。根因是仓库 `forge-admin-ui/pnpm-workspace.yaml` 缺少有效 `packages` 字段，pnpm 按空工作区处理。

**解决方案**:
前端 pnpm 命令统一加 `--ignore-workspace`：

```bash
source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm --dir forge-admin-ui --ignore-workspace exec vitest run <spec>
```

不要为此擅自修改仓库的 `pnpm-workspace.yaml`；若该文件后续被修复为有效工作区，再去掉 `--ignore-workspace` 复验。

## SPA fallback 不能吞掉缺失的哈希静态资源

**发现日期**：2026-09-17

生产发布替换前端产物后，仍打开旧页面的浏览器可能在后续访问懒加载路由时请求上一版 chunk。若 Nginx 对 `/assets/*.js` 也使用 SPA fallback，不存在的 JavaScript 会返回 `200 text/html`，浏览器最终报 `Failed to fetch dynamically imported module`，且状态码会误导排查。

处理原则：哈希资源目录使用独立 `location`，只允许真实文件并在缺失时返回 404，同时设置 `immutable` 长缓存；HTML 使用 `no-cache/no-store`。客户端监听 Vite 预加载错误并由 Router 兜底，在会话级冷却窗口内最多自动刷新一次，避免网络故障造成刷新死循环。

## Naive UI 表格居中不能只设置 `text-align`


**发现日期**：2026-08-07

`n-data-table` 的 `titleAlign` 只改变表头单元格文本对齐；自定义 body renderer 常返回 `inline-flex`、图片/附件容器或操作按钮组，仍可能相对标题中心偏移。可排序/筛选表头还会把图标放进标题 flex 流并增加右侧 padding。

处理原则：最终列配置同时固定 `align`/`titleAlign`，body render 统一包在 `width: 100%; min-width: 0` 的 flex 容器中按 `justify-content` 对齐；居中表头标题脱离图标流并以完整 header 单元格中心定位，图标固定在右侧。回归测试应检查 render VNode 的几何样式，而不只检查列配置值。

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

## 117. Naive 组件内部 CSS 变量不能作为自定义页面主题 Token


**发现日期**: 2026-07-16

**问题描述**:
自定义页面直接使用 `--n-color`、`--n-action-color`、`--n-table-header-color` 等 Naive 组件内部变量作为卡片和表格背景时，变量值取决于所处组件作用域。在自定义主色或 Teleport 抽屉上下文中可能继承为主色，导致整页背景变蓝、文字对比度不足。

**解决方案**:
- 自定义页面背景统一使用 Forge 全局 `--bg-primary/--bg-secondary/--bg-tertiary`；
- 文字和边框使用 `--text-primary/secondary/tertiary/disabled`、`--border-light`；
- 强调色使用动态 `--primary-color`，不要用固定颜色模拟系统主色；
- Naive 的 `--n-*` 只在对应组件自身样式覆盖中使用，不能承担页面级主题契约；
- Teleport 抽屉需要同时显式设置 header、body、body-content 和 footer 的系统背景。

**影响范围**:
- 所有自定义 Vue 页面、卡片、表格和抽屉；
- 亮色、暗色、自定义主色及 Teleport 场景。

## 162. 租户切换后的会话刷新不能从稳定用户 ID 退回用户名查询


**发现日期**: 2026-08-04

**问题描述**:
超级管理员可按设计进入任意启用租户，普通多租户用户也以同一个全局用户主键承载多个租户成员关系。如果租户切换接口先按 Session `userId` 成功重建目标租户身份，随后用户信息接口却改用 `username + targetTenantId` 再查一次，未在目标租户建立成员行的超级管理员会被误报“用户不存在”。若前端权限守卫在用户仍为空时继续 `replace` 原路由，还会立即重入同一初始化分支，形成无限请求；持久化 Token 会让浏览器刷新也无法恢复。

**解决方案**:
- 登录后的用户刷新始终使用 Token Session 中已认证的稳定 `userId`，并携带当前 `tenantId/activeOrgId` 重新构建组织、角色和权限；客户端不得传入或覆盖用户标识；
- 超级管理员跨租户能力仍由服务端用户类型和目标租户状态控制，普通用户仍校验启用的租户成员关系，禁止用全局忽略租户绕过；
- 用户身份初始化失败且 Store 仍无用户时，前端清理半初始化登录态并单次跳转登录页，不能再次导航到原受保护路由；
- 用户身份已建立后出现菜单或租户配置偶发失败时，应与身份失效分开处理，避免无差别强制退出。

**影响范围**:
- 所有支持租户切换的登录会话刷新、权限路由守卫和持久化登录态恢复链路。

## 169. CSS 尺寸变量在独立挂载场景需要兜底


**发现日期**：2026-08-08

Naive UI 的 `--n-height` 可保证同尺寸输入和按钮对齐，但 Teleport 或独立挂载的组件不一定继承该变量，直接写 `height: var(--n-height)` 会让高度声明失效。共享样式应使用与组件主题一致的 fallback，例如 default/small/tiny/large 分别使用 `var(--n-height, 34px)`、`28px`、`22px`、`40px`，并用实际浏览器几何验证弹层内控件。

## Naive Dialog 取消回调不能意外返回 false

**发现日期**：2026-09-07

数据权限开关确认框点击取消后仍留在屏幕上。根因是 `const release = () => confirming.value = false` 隐式返回了 `false`，Naive Dialog 将该返回值解释为阻止关闭。改用块函数 `() => { confirming.value = false }`，只更新状态、不返回 false；浏览器验证取消后遮罩消失，单测同时检查回调返回值与不发送保存请求。

## 182. Vue 客户端组件模板不能直接承载运行时 style 标签


**发现日期**：2026-08-21

在 Vue SFC 的 `<template>` 中使用 `<style v-for>` 动态装载业务 CSS，会触发 `Tags with side effect (<script> and <style>) are ignored in client component templates`，标签会被编译器忽略；开发服务可能只显示警告，但正式运行时样式不会可靠生效。

处理原则：动态 CSS 应由受控组件通过渲染函数创建 `style` VNode，并 Teleport 到 `document.head`；CSS 内容必须先经过平台校验与作用域重写。回归测试应覆盖样式挂载、 更新时移除旧节点、空内容跳过和卸载清理，不能只断言 CSS 字符串生成正确。

## naive-ui FormItem 挂载测量会清空字段级固定 labelWidth

**发现日期**：2026-09-15

**问题描述**:
动态表单出现 label 列不对齐：空表单时各 label 宽度参差（39.8/53.7/67.6/81.5/100 混杂），随便输入内容后部分"自愈"，详情查看模式全乱。根因是 naive-ui FormItem 的原生缺陷：挂载时的 `invalidateLabelWidth` 为测量 label 自然宽度，会执行 `labelElement.style.width = ''` 直接清空 DOM inline width，事后只恢复 whiteSpace。当「表单级 `label-width='auto'` + 字段级数字 labelWidth」时，`isAutoLabelWidth` 判定只看 form/item 的 labelWidth 是否为字符串 `'auto'`（与字段级数字无关），所以挂载即清空；而 `mergedLabelWidth` 恒为该固定值不再变化，Vue patchStyle 发现新旧值相同直接跳过写入，DOM 宽度就此永久丢失，label 塌缩为各自内容宽。值加载后"自愈"是因为部分字段的 labelWidth prop 异步从 undefined 变为数字，值变化触发了重新 patch；详情模式值填充 + readonly 切换让大量字段落入"清空后值不再变化"死区，故全乱。

**解决方案**:
- 项目层兜底（禁止改 node_modules）：`AiFormItem.vue` 给 n-form-item 加 ref，`restoreFixedLabelWidth` 在挂载后与 `field.labelWidth` 变化后把固定宽度显式写回 label 元素 inline style；`AiForm.vue` 的 `remeasureAutoLabelWidth` 在 `formRef.invalidateLabelWidth()`（Form 级重测同样会清空）之后按 `allFieldSchema` 遍历 `[data-ai-field]` 统一写回（`restoreFixedLabelWidths`）。
- 诊断方法论：「组件响应式值正确但 DOM 不对」类问题，用 Playwright 实测对比 Vue 组件实例 `setupState.mergedLabelStyle` 与 DOM `label.style.width`——两者不一致即证明有代码绕过 Vue 直接操作 DOM。此 bug 当初按"字体加载时序"猜测修复无效，教训是渲染类异常必须先实测复现、拿到证据再改代码。

**影响范围**:
- 所有「表单级 labelWidth='auto' 混用字段级数字 labelWidth」的 naive-ui 表单；AiForm/AiFormItem 已内置兜底，其它直接使用 naive-ui 表单的场景需自行注意。

## window.$message 是 class 实例，方法不能分离调用

**发现日期**：2026-09-09

**问题描述**:
字段事件通知触发控制台报错 `naiveTools.js:69 Uncaught TypeError: Cannot read properties of undefined (reading 'showMessage')`。`window.$message` 是 naiveTools 中 class `Message` 的**实例**（原型方法 error/success/info/warning），写出 `const notify = window.$message?.[type]; notify(message)` 后分离调用，class 严格模式下 `this === undefined`，方法内部 `this.showMessage` 即崩溃。该崩溃还会吞掉真正的业务错误消息，让人误判问题。

**解决方案**:
- 必须在对象上链式调用：`window.$message?.[type]?.(message)`。全库 grep 检查同类写法，其余处均为正确链式调用，只有一处分离调用（AiForm.vue 的 handleFieldEventNotify）。
- 排查口诀：见到 `Cannot read properties of undefined (reading 'xxx方法名')` 且调用目标是全局工具对象时，优先怀疑方法被取出分离调用。

## computed 每次返回新对象时 watch 引用比较恒不等

**发现日期**：2026-09-09

**问题描述**:
表单里点选任意一个远程下拉后，其它所有远程下拉的 loading 图标都转一下（真实重新发起了请求，不是样式联动）。根因：`remoteOptionSource` computed 依赖 `props.formData`（解析 `${field}` 引用参数 + 级联参数），每次重算都**返回新对象**；`watch([remoteOptionSource, ...])` 按引用比较，新对象 !== 旧对象恒成立，于是任意字段值变化都触发所有远程下拉重新加载。watch 数组里虽配了"只盯引用字段值"的第二项，但因第一项恒变而完全失效。

**解决方案**:
- computed 返回新对象时，watch 盯**内容签名**而非对象本身：`JSON.stringify` 关键字段 + 解析后的 params 组成签名 computed，再 `watch(签名, ...)`。
- 签名必须包含 `${field}` 引用解析后的值（params），否则引用字段值变化不会触发对应下拉重载。
- 识别特征：多个实例"集体响应"某个单点变化（全量 loading 闪烁、全量重发请求），基本可断定存在引用比较失效的 watch。

## AiCrudPage 的 api-config.delete 必须指向批量删除接口（数组入参）

**发现日期**：2026-09-17

**问题描述**：
通知公告页批量删除报"缺少必需参数: noticeId"。原因是 `api-config.delete` 写成了单删接口 `post@/system/notice/remove`（后端 `@RequestParam noticeId`）；AiCrudPage 的 `performDelete` 对"无 :id 占位符"的 delete 配置一律按批量契约发请求（POST 配置 URL + body 传主键数组），于是后端参数绑定报缺失。行内单删若也复用了该配置同样会报错，且无 :id 占位符时不会走组件的 `/batch` 分支。

**解决方案**：
- `api-config.delete` 统一指向模块的批量删除接口并传数组，如 `delete: 'post@/system/xxx/removeBatch'`（post/dictType/config/tenant/role/user 等标准页均为此模式）；后端 `removeBatch(@RequestBody Long[] ids)` 天然兼容单条与批量。
- 行内单删如需单独走 `@RequestParam` 接口，用自定义 `onClick: handleDelete`（POST + params）实现，不要复用 api-config.delete。
- 排查口令：批量删除报"缺少必需参数: xxx" == delete 配置指向了 `@RequestParam` 单删接口。

**影响范围**：
所有使用 AiCrudPage 且启用工具栏批量删除的页面（apiConfig、dataScopeConfig 已同步修正为 removeBatch）。

## 打印客户端 PDF 不能用 html2canvas 重排 flex 表格

**发现日期**: 2026-09-21

**问题描述**:
预览「PDF」先把纸张再挂到隐藏 iframe，再用 html2canvas 栅格化。html2canvas 自己重做布局，不认表格单元格的 flex 行高和垂直居中，导出来的 PDF 和预览差一截。同时 `createPattern` 会撞上 0 尺寸画布（细线渐变、空页眉页脚）。

**解决方案**:
导出截取预览里已经排好的 `[data-print-page]`（`html-to-image` 走浏览器排版），缩放 `transform` 在截图时关掉。打印仍走 `window.print()`，不要把 PDF 事件写成 `DIALOG_OPENED`。单元格文字包在 `span` 里，避免匿名 flex 子节点。

## 打印表头不透明底会盖住表格外框上/左边

**发现日期**: 2026-09-21

**问题描述**:
表格外框曾用容器 `inset box-shadow` 画上/左边，右/下边画在单元格上。表头默认 `#f1f5f9` 不透明，会把容器内侧阴影盖住，设计器和打印都缺顶边、左边。贴边 `0.15mm` 的 `border-top/left` 还容易被祖先 `overflow: hidden` 在亚像素处裁没。

**解决方案**:
首行补 `border-top`、首列补 `border-left`，右/下边仍画在每个格子上，四面都用同一条 CSS `border`（同宽同色）。底色用 `background-clip: padding-box`，避免填进边框把线吃细。不要上/左用渐变、右/下用 border，打印时粗细会对不齐。

## 打印表格选中格不能用 !important 盖住表头底色

**发现日期**: 2026-09-21

**问题描述**:
空白表格选中态用 `.static-cell.selected { background-color: ... !important }` 盖住单元格真实底色；明细表 hover/selected 也用 class 改 `background-color`。用户在「样式」里改了表头颜色，一点格子就看不见，误以为色板坏了。另外旧面板把表头色写进 `style` 或 `column.style`，格子只认 `headerStyle`，所以有的色板改了没效果。

**解决方案**:
表头/表体颜色只写 `headerStyle` / `style`（「样式」页分开两组）。选中反馈用选择框 overlay，不要 `!important` 覆盖底色。空白表格插入不要默认「表头」灰行；「设为表头」改 `headerStyle`，不要给格子写死灰底。

## 打印示例图不能用 SVG data URL

**发现日期**: 2026-09-21

**问题描述**:
设计器示例上下文把 IMAGE 字段（含 `flow.history.signature`）写成 `data:image/svg+xml,...`。打印资源协议只接受 fileId 或 `data:image/(png|jpeg|webp);base64,...`。打开预览时第一条签名就报「打印图片或签名加载失败（flow.history[0].signature）」。真实签名下载若 `Content-Type` 是 `application/octet-stream`，同样会被类型白名单拒绝。

**解决方案**:
示例图必须是协议允许的 PNG data URL。加载文件时按文件头识别 PNG/JPEG/WEBP，不要只看 HTTP MIME。空签名跳过，不要当成失败。

## 空白表格表头背景会被格子默认白底盖住

**发现日期**: 2026-09-21

**问题描述**:
「样式 → 表头背景」写的是 `headerStyle.backgroundColor`。第一行格子若带默认 `#ffffff` / `#f1f5f9`（旧「表头」行或工具栏色板），`cell.style` 后合并，表头背景看不见，表头文字色仍正常。

**解决方案**:
`staticTableCellLook` 对第一行默认白/灰底让位给 `headerStyle`。属性面板改表头/表体时用 `patchStaticTableBand`，同时删掉对应行格子上的同名覆盖。字号用预设下拉，不要 `NInputNumber`。

## 打印字体栈不能只校验第一个名字

**发现日期**: 2026-09-21

**问题描述**:
`requireLocalFont` 只对 `fontFamily.split(',')[0]` 做 `FontFace local()`。设计器「华文黑体」写成 `STHeiti, sans-serif`，新版 macOS 没有 STHeiti，预览报「打印字体未安装：STHeiti」，看起来像改完字体就保存/预览失败。微软雅黑在 Mac 上同样会中招。

**解决方案**:
按整串字体栈检查，任一具名字体能加载即通过；栈里有 `sans-serif`/`serif` 等 generic 时不要因为第一个名字缺失而拦截。设计器选项写成跨 Windows/macOS 的回退栈。

## 表格选中框八向锚点会挡住外沿改行高列宽

**发现日期**: 2026-09-21

**问题描述**:
空白表格内部格线能拖出行高列宽，四条外边不行。选中框的 n/s/e/w 锚点叠在表格外沿上把命中抢走；行列手柄又只做了 `slice(0, -1)`，最后一行/列本来就没有手柄。快捷面板也按整张表算位置，不跟着选中单元格走。

**解决方案**:
表格元素不要再画 overlay 缩放锚点。四边补上首末行列手柄（左边/上边拖时同步改 `xMm`/`yMm`）。选中单元格时快捷面板用格子的纸面包围盒。

## 打印取色器会写出协议不认的颜色格式

**发现日期**: 2026-09-21

**问题描述**:
快捷面板「无填充」写入 `transparent`，Naive `NColorPicker` 还会给出 `#rrggbbaa` / `rgba()`。格子 `style.backgroundColor` 原校验只认 `#rgb`/`#rrggbb`，保存报「颜色须使用十六进制格式」。

**解决方案**:
写入和 `serialize` 时用 `toPrintColor` 收成 `#rrggbb` 或 `transparent`。前后端校验同时接受这两种以及取色器的 8 位 hex / rgb。命名色如 `red` 仍拒绝。

## 空白表格图片缩放点会被行列拖条盖住

**发现日期**: 2026-09-21

**问题描述**:
格子 `z-index: 1`，行列拖条 `z-index: 6` 且后渲染。单元格图片右下角 8px 缩放点永远点不中，看起来像坏了。

**解决方案**:
选中的图片格提高到拖条之上并允许溢出；手柄挂在图片框右下角，热区约 14px。

## 打印横竖线不能用 SVG viewBox 描边

**发现日期**: 2026-09-21

**问题描述**:
横线默认高 0.5mm、竖线宽 0.5mm。SVG `viewBox="0 0 100 100"` 里画 `<line>`，描边几乎看不见，改背景色、边框色、实线/虚线/点线都像没生效。

**解决方案**:
横竖线改成 CSS `border-top` / `border-left`，粗细用 `borderWidthMm`，样式直接用 `solid`/`dashed`/`dotted`。改颜色时同步 `borderColor` 和 `backgroundColor`，改粗细时同步细轴宽高。

## 打印毫米字段不要用 NInputNumber

**发现日期**: 2026-09-21

**问题描述**:
边框 mm、宽高 mm 等用 `NInputNumber` 时仍能输入汉字，选完像没改。

**解决方案**:
带 mm 的字段改成 `NSelect` + `printMmOptions` 预设，不要可输入的数字框。

## 打印预览新标签不能 router.back

**发现日期**: 2026-09-21

**问题描述**:
单据打印从列表 `window.open` 新标签打开。新标签没有历史，左上角返回调用 `router.back()` 什么也不发生。

**解决方案**:
有 Vue Router `history.state.back` 才后退；否则 `window.close()`，关不掉再 `replace` 到来源应用或流程页。

## 应用中心卡片不要 hover 才展开操作栏

**发现日期**: 2026-09-21

**问题描述**:
应用卡片 footer 默认 `display:none`，hover 再显示操作，还带 `translateY(-1px)`。卡片高宽一变，网格会跟着晃。

**解决方案**:
操作栏用绝对定位叠在卡片底部，默认 `opacity:0`，hover / 聚焦才显示。不要用 `display:none` 或位移改尺寸。

## 发布运行页不要卡住等后台菜单再叠多层 loading

**发现日期**: 2026-09-21

**问题描述**:
新开 `/app/:slug` 时 permission-guard 要等 `getMenu`，App.vue 先盖「正在加载...」，守卫放行后 layout/门户 chunk 未到是白屏，门户 `n-spin` 再转一圈，CRUD 再转一圈。顶部 `$loadingBar` 还会卡住。

**解决方案**:
`/app/`（不含 `/app-center`）不要阻塞等后台菜单；`app-portal` 跳过全局进度条和全屏 overlay；layout 同步加载；门户和 CRUD 用同一套骨架，不要连续两个 `n-spin`。

## 有编辑权限时页面管理左侧菜单要读草稿不能只读发布快照

**发现日期**: 2026-09-22

**问题描述**:
新建表单并保存草稿后，打开 `/app-center/application/.../runtime?pageId=...`（无 `edit=1`）左侧菜单看不到新页面。之前不用发布也能看见。根因是页面管理走了 `businessApplicationRuntimeByCode` 已发布快照，未发布页面不在快照里。

**解决方案**:
`shouldUseApplicationWorkspaceLoad` 在 `edit=1` / `draft=1` 之外，对有应用编辑权限的用户也返回 true，页面管理读 workspace 草稿。正式运行用户仍只读发布快照。页面管理对可编辑用户还需 `design-preview`（或 PortalPageRenderer 在 `configurable` 时优先读草稿），否则刚保存的字段默认值仍来自已发布 CRUD 快照，表现为必须发布应用才生效。

## 打印模板必须跟页面走，设计器不能回到 /print

**发现日期**: 2026-09-21

**问题描述**:
打印模板挂在应用设置或应用卡片上，一个应用多个页面会串到一起。设计器 `/print/designer?templateId=2` 右上角返回走 `history.back()` 或独立 `/print` 列表，回不到来源页面。详情只有弹窗能打印，平铺详情按钮在页脚被裁掉，抽屉详情之前被误开成弹窗。

**解决方案**:
模板列表和绑定按 `pageId` 过滤，配置入口只放在当前页面的「页面设置 → 打印模板」。设计器返回优先用来源页 `from`（必须是该页面的 `edit=1` 运行地址），否则回到该页打印设置；不要 `router.back()` 到 `/print`。详情弹窗、抽屉、平铺都要露出打印动作，平铺放在顶部操作区。
