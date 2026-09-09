# 踩坑：前端 / 构建 / 路由

> 从 `code-copilot/memory/pitfalls.md` 按主题拆出。新条目追加到本文件。共 17 条。

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

处理原则：动态 CSS 应由受控组件通过渲染函数创建 `style` VNode，并 Teleport 到 `document.head`；CSS 内容必须先经过平台校验与作用域重写。回归测试应覆盖样式挂载、更新时移除旧节点、空内容跳过和卸载清理，不能只断言 CSS 字符串生成正确。
