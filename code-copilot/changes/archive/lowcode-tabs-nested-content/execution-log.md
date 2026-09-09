验证记录：
- `pnpm --dir forge-admin-ui exec eslint src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvasNode.vue src/views/app-center/components/designer/form-first/formDesignerSchema.js src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue src/components/lowcode-builder/page/ListPageGridDesigner.vue src/components/lowcode-builder/page/GridBlockRenderer.vue` 通过。
- `NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build` 通过；仅有既有 Vite 分包和 CSS 注释警告。

## 2026-08-12 Tabs 实际入口补修

- 变更范围：
  - `ForgePropertyPanel.vue` 改为调用 schema 原子操作，连续新增页签不会因属性面板重渲染或选中状态切换丢失先前页签。
  - `formDesignerSchema.js` 新增 `appendDesignerLayoutChild`，统一生成唯一页签 ID、页签 `name` 和空 `children`。
  - `ForgeFormCanvasNode.vue` 把页签内容区作为独立 drop target，并禁止普通组件投到 Tabs 外层时默认落入首个页签。
  - `application-runtime.[applicationCode].vue` 根据实际 drop target 的 Tabs ID 与 Tab key，将组件写入该页签的 `children`，不再一律追加到页面根级。
- 验证命令与结果：
  - `pnpm --dir forge-admin-ui exec vitest run src/views/app-center/components/designer/form-first/__tests__/formDesignerSchema.spec.js src/components/lowcode-builder/page/__tests__/page-schema.spec.js`：通过，5/5。
  - `pnpm --dir forge-admin-ui exec eslint src/views/app-center/components/designer/form-first/formDesignerSchema.js src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvasNode.vue 'src/views/app-center/application-runtime.[applicationCode].vue'`：通过。
  - `git diff --check -- ...`：通过。
  - `curl http://localhost:3000/`：HTTP 200，Vite 开发服务可访问。
- 浏览器验收：尝试使用本机 Playwright 访问对象设计页，但 Chromium 在受限执行环境启动后因 `SIGTRAP` 退出，未能执行已登录页面的真实点击/拖放。未将该项标记为通过。

## 2026-08-12 22:24 CST 运行时 Tabs 拖放修复复验

- 准确路由：`http://localhost:3000/app-center/application/PRESALE_REGISTRATION_APP/runtime?edit=1&pageId=page_1`。
- 根因 1：新增 Tabs 等非 CRUD 组件时无条件执行 `preloadPageBlockCrudRuntimeProps`，现有业务对象配置因 `money` 控件返回 500，并弹出全局错误对话框；遮罩拦截后续画布拖放。
- 根因 2：指针模拟拖放会在离开 Naive Popover 时收到 `pointercancel`，且 Popover 外部交互会截断后续 move/up；改为原生 HTML5 `dataTransfer + dragover/drop`。
- Tabs 验收：把“提示面板”拖入“标签一”，根区块保持 `1 -> 1`，页签空提示由 `1 -> 0`，内容显示“提示信息”。
- 普通画布验收：把“提示面板”拖到 Tabs 下方空白画布，根区块 `1 -> 2`，根级 drop 未回归。
- 嵌套处理：`tabDrop` 在栅格、卡片、盒子和 Tabs 递归渲染节点逐层转发；运行页递归查找并更新目标 Tabs。
- 页面保存：未点击“保存草稿”，避免污染用户应用；持久化重载未覆盖。
- `pnpm --dir forge-admin-ui exec eslint 'src/views/app-center/application-runtime.[applicationCode].vue' src/components/lowcode-builder/page/GridBlockRenderer.vue`：通过。
- `pnpm --dir forge-admin-ui exec vitest run src/components/lowcode-builder/page/__tests__/page-schema.spec.js`：通过，1/1。
- `git diff --check`：通过。
- `pnpm --dir forge-admin-ui build`：通过，耗时约 3 分钟；仅有既有动态分包和 CSS `//` 注释警告。
- 浏览器环境：受限 Chromium 仍因 `SIGTRAP` 无法启动，改用本机 Chrome channel 完成真实操作；本轮未启动或停止用户服务。
- 后续第二标签页补验：本地临时 Token 已过期，页面被重定向到登录页；直接 curl 登录又被密码加密策略拒绝，未把该环境阻断误记为功能失败。

## 2026-08-13 Tabs 子组件选中与编辑增量验证

- 根因：运行时属性面板 `ListPageGridDesigner` 同步 `activeBlockId` 时使用顶层 `blocks.some`，Tabs 子组件 ID 被忽略；改为 `findBlockInTree` 递归判断。
- 运行时 Tabs 子组件增加独立交互外壳，点击选中后显示更多操作和八个缩放锚点；删除、复制和缩放通过递归树更新。
- 嵌套组件的数据源、外观更新也改为递归树更新，避免属性面板切换成功但保存操作只改根级。
- `pnpm --dir forge-admin-ui exec eslint ...`：通过（Node v24.13.0；v20.20.0 下 pnpm 11.7 依赖 `node:sqlite` 不兼容）。
- `pnpm --dir forge-admin-ui exec vitest run src/views/app-center/__tests__/application-runtime-load.spec.js src/views/app-center/in-app-builder/__tests__/page-form-data-provisioning.spec.js`：通过，13/13。
- `pnpm --dir forge-admin-ui exec vitest run src/components/lowcode-builder/page/__tests__/list-page-grid-designer-nested-selection.spec.js`：通过，1/1；真实挂载验证 Tabs 子组件 ID 能切换属性标题。
- `pnpm --dir forge-admin-ui build`：通过；仅保留既有动态分包和 CSS 注释警告。
- 浏览器 E2E 未执行：本机后端 `localhost:8580` 未启动，无法登录加载应用数据；Vite 开发服务器启动还受系统 `EMFILE` 文件监听上限影响，未启动用户服务。

## 2026-08-13 application-page-flow 链路修正

- 用户反馈“改动没有效果”后重新追踪：`/app-center/application/.../runtime` 路由确实加载 `application-runtime.[applicationCode].vue`，但页面同时渲染正常区块和拖动预览区块，两套 `.layout-tabs` 会让自动化/落点误命中隐藏 pane；原先仅依赖 `GridBlockRenderer -> tabDrop` 的链路不够可靠。
- `application-page-flow` 现在直接使用 `@dragover.capture` / `@drop.capture`，从事件目标解析最近的可见 `data-grid-container-id` + `data-grid-tab-key`，直接决定写入 Tabs 或根级。
- Tabs 改为受控 active key，点击第二个标签页后保持 `tab2`，不会重新渲染回第一个页签。
- 本机 Chrome 真实验证：点击“标签二”后 active 为“标签二”，可见 pane key 为 `tab2`；拖入“提示面板”后第二页签显示“提示信息”，根级区块保持 `1`，第一页仍为空。
- `pnpm --dir forge-admin-ui exec eslint 'src/views/app-center/application-runtime.[applicationCode].vue' src/components/lowcode-builder/page/GridBlockRenderer.vue`：通过。
- `pnpm --dir forge-admin-ui build`：通过；保留既有 Vite 动态分包和 CSS 注释警告。
