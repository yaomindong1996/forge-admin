# 执行日志：forge-list-designer-productivity-upgrades

## 2026-06-20 18:48:00 CST

### 变更范围

- 修复专注画布模式下左右展开 rail 按钮仍被渲染，导致 grid 自动布局残留占位的问题。
- 专注画布下 `canvas-zoom-stage` 改为按内容宽度居中，不再因为 `min-width: 100%` 让真实画布贴左。
- 进入专注画布时自动把横向滚动位置调整到中间，避免大屏下内容挤在左侧。

### 命令与结果

- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/ListPageGridDesigner.vue`：通过；实际使用 `v20.19.5`。
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue`：通过。

### 跳过项

- 未进行浏览器手工验证或 Playwright 截图：本轮为单文件布局修复。
- 未执行前端全量构建：本轮增量执行定向 eslint 和空白检查。

### 服务清理

- 本轮未启动常驻服务，无需清理 PID。

## 2026-06-20 18:46:06 CST

### 变更范围

- 移除列表设计工具栏内重复的“保存列表”按钮。
- 保留 `BusinessListDesigner.saveLayout()` 暴露方法，右上角全局“保存”在列表页仍调用同一保存逻辑。
- 列表页保存入口统一为右上角“保存”，减少同屏两个保存按钮的语义混淆。

### 命令与结果

- `pnpm --dir forge-admin-ui exec eslint src/views/app-center/components/designer/BusinessListDesigner.vue`：通过；本机无精确 `v20.19.0`，实际使用 `v20.19.5`。
- `git diff --check -- forge-admin-ui/src/views/app-center/components/designer/BusinessListDesigner.vue`：通过。

### 跳过项

- 未执行前端全量构建：本轮只移除重复按钮并保留原保存方法，已做定向 lint 与空白检查。
- 未执行后端编译：本轮只改前端 Vue 模板。

### 服务清理

- 本轮未启动常驻服务，无需清理 PID。

## 2026-06-20 18:40:41 CST

### 变更范围

- 单按钮组件的“按钮配置”拆分为“基础样式”和“按钮状态”，基础样式配置按钮文本、类型和尺寸。
- 按钮状态改为逐行开关并补充说明：次要按钮、撑满宽度、禁用状态、加载状态。
- 修复设计器预览中单按钮被写死 `disabled` 的问题，只有打开“禁用状态”时才禁用；打开“加载状态”和“撑满宽度”会在画布预览中立即可见。
- 单按钮默认 schema 补齐 `size`、`disabled`、`loading` 字段。

### 命令与结果

- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/ListPageGridDesigner.vue src/components/lowcode-builder/page/GridBlockRenderer.vue src/components/lowcode-builder/page/page-schema.js`：通过；本机无精确 `v20.19.0`，实际使用 `v20.19.5`。
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/GridBlockRenderer.vue forge-admin-ui/src/components/lowcode-builder/page/page-schema.js`：通过。

### 跳过项

- 未进行浏览器手工验证或 Playwright 截图：本轮未启动 Vite/后端服务。
- 未执行后端编译：本轮只改前端 Vue 设计器和页面 schema。

### 服务清理

- 本轮未启动常驻服务，无需清理 PID。

## 2026-06-20 18:36:59 CST

### 变更范围

- 修复“专注画布”复用左右面板收起状态的问题：专注状态改为独立 `canvasFocusMode`，不再修改 `paletteCollapsed` / `propertyCollapsed`。
- 专注画布时左侧“页面组件”和右侧“配置组件”直接不渲染，避免响应式布局下跑到画布上方/下方。
- 退出专注后保留进入专注前的左右面板展开/收起状态。

### 命令与结果

- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/ListPageGridDesigner.vue`：通过；本机无精确 `v20.19.0`，实际使用 `v20.19.5`。
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue`：通过。

### 跳过项

- 未进行浏览器手工验证或 Playwright 截图：本轮未启动 Vite/后端服务。
- 未执行后端编译：本轮只改前端 Vue 设计器。

### 服务清理

- 本轮未启动常驻服务，无需清理 PID。

## 2026-06-20 18:34:05 CST

### 变更范围

- 列表设计器左侧“页面组件”和右侧“配置组件”收起后不再保留 42px 侧栏列，中间画布直接释放并占用对应空间。
- 左右面板展开入口改为画布边缘悬浮小箭头，避免收起后看起来仍有一个独立左右模块。
- AiCrudPage “工具栏与导入导出”配置从混合 checkbox 改为逐行显示开关，开关打开即显示对应顶部工具栏按钮，关闭即隐藏。
- 工具栏开关补充说明文案，明确“新增、批量删除、导入、导出、自定义查询、导出任务入口”分别控制什么，以及接口/工具栏总开关关闭时为什么看不到变化。

### 命令与结果

- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/ListPageGridDesigner.vue`：通过；本机无精确 `v20.19.0`，实际使用 `v20.19.5`。
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue`：通过。

### 警告

- 未执行前端全量构建：本轮只改 `ListPageGridDesigner.vue` 的模板、状态映射和 scoped 样式，已做定向 eslint 与空白检查。

### 跳过项

- 未进行浏览器手工验证或 Playwright 截图：本轮未启动 Vite/后端服务。
- 未执行后端编译：本轮只改前端 Vue 设计器。

### 服务清理

- 本轮未启动常驻服务，无需清理 PID。

## 2026-06-20 18:35:00 CST

### 变更范围

- 筛选树收起态从“筛选树 + 树标题”简化为单个竖向“树”标识，完整标题仅保留在 hover title，降低画布占用和视觉噪声。
- 页面组件面板取消原生 `disabled` 卡片：已存在的唯一组件显示“已在画布中”，点击后选中并滚动到画布中的已有组件。
- 当前布局不支持的组件显示“当前布局不可用”，点击时给出信息提示，避免看起来像按钮坏了。

### 命令与结果

- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/GridBlockRenderer.vue src/components/lowcode-builder/page/ListPageGridDesigner.vue`：通过；实际使用 `v20.19.5`。
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/page/GridBlockRenderer.vue forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue`：通过。

### 跳过项

- 未进行浏览器手工验证或 Playwright 截图：本轮为小范围交互和样式调整。
- 未执行前端全量构建：上一轮已完成 `pnpm build`，本轮增量执行定向 eslint 和空白检查。

### 服务清理

- 本轮未启动常驻服务，无需清理 PID。

## 2026-06-20 18:17:52 CST

### 变更范围

- 移除全局 `n-tree` 选中节点左侧指示条，避免所有树组件被统一插入额外竖线。
- 修复筛选树运行态节点内容短时居中问题：树节点与内容区强制撑满宽度并左对齐。
- 筛选树整块展开/收起按钮改为贴右边缘的窄按钮；节点展开/收起独立为“节点层级”工具条，减少标题区混乱。
- `AiCrudPage` 的“添加下级”行操作改为显式 `enableTreeAddChild` 开关，左树右表筛选布局默认不显示。
- 自由画布增加“专注画布”模式，一键隐藏左右配置面板，释放中间画布宽度。
- 画布新增拖拽边缘自动滚动，并在移动/缩放区块时把滚动距离计入位移计算，避免拖到可视区外时元素跟丢。

### 命令与结果

- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/GridBlockRenderer.vue src/components/lowcode-builder/page/ListPageGridDesigner.vue src/components/ai-form/AiCrudPage.vue src/components/ai-form/AiCrudPageProps.js src/views/ai/crud-page.vue`：通过；本机无精确 `v20.19.0`，实际使用 `v20.19.5`。
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/page/GridBlockRenderer.vue forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/components/ai-form/AiCrudPage.vue forge-admin-ui/src/components/ai-form/AiCrudPageProps.js forge-admin-ui/src/views/ai/crud-page.vue forge-admin-ui/src/styles/global.css`：通过。
- `NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`：通过。

### 警告

- 首次按偏好执行 `nvm use v20.19.0` 返回 N/A；本机安装的是 `v20.19.5` 和 `v20.20.0`，本轮使用 `v20.19.5` 验证。
- 构建保留项目既有 warning：CSS 中存在 `//` 注释。
- 构建保留项目既有 warning：`src/store/index.js` 同时被动态和静态导入，Rollup 不会将其移动到单独 chunk。

### 跳过项

- 未进行浏览器手工验证或 Playwright 截图：本轮未启动 Vite/后端服务。
- 未执行后端编译：本轮只改前端 Vue 设计器和运行态 CRUD 行操作逻辑。

### 服务清理

- 本轮未启动常驻服务，无需清理 PID。

## 2026-06-20 17:47:18 CST

### 变更范围

- 修复默认参数编辑器点击“新增”后空参数行被父级 props 回刷清空的问题。
- 新增空参数行只保留在编辑器本地 UI，不再立即向父级写入空 `{}`，避免触发 `AiCrudPage.publicParams/publicQuery` 监听导致中间画布列表刷新。
- 自由画布和 CRUD 快速配置的默认参数更新入口增加相同值保护，避免重复写入相同配置。

### 命令与结果

- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/CrudDefaultParamsEditor.vue src/components/lowcode-builder/page/ListPageGridDesigner.vue src/components/lowcode-builder/page/StructuredListPageDesigner.vue`：通过。
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/page/CrudDefaultParamsEditor.vue forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/StructuredListPageDesigner.vue`：通过。
- `pnpm --dir forge-admin-ui build`：通过。

### 警告

- 构建保留项目既有 warning：CSS 中存在 `//` 注释。
- 构建保留项目既有 warning：`src/store/index.js` 同时被动态和静态导入，Rollup 不会将其移动到单独 chunk。

### 跳过项

- 未进行浏览器手工验证或 Playwright 截图：本轮未启动 Vite/后端服务。
- 未执行后端编译：本轮只改前端 Vue 默认参数编辑器和父级更新保护。

### 服务清理

- 本轮未启动常驻服务，无需清理 PID。

## 2026-06-20 17:40:49 CST

### 变更范围

- 新增共享 `CrudDefaultParamsEditor`，自由画布 `配置组件` 和 `CRUD 快速配置` 统一使用同一套默认参数配置。
- AiCrudPage 增加 `formDefaultValues` 与 `submitDefaultParams`，并复用已有 `publicParams/publicQuery`：列表查询、URL query、打开表单默认值、提交固定参数四类配置分开维护。
- 新增/编辑打开表单时合入 `formDefaultValues`；编辑场景已有记录值优先，不被默认值覆盖。
- 新增/编辑提交前合入 `submitDefaultParams`，再执行 `beforeSubmit` 回调，允许高级回调继续覆盖固定参数。
- 发布运行页、列表设计器预览、自由画布单区块预览同步传递默认参数，避免设计态和发布态分叉。

### 命令与结果

- `pnpm --dir forge-admin-ui exec eslint src/components/ai-form/AiCrudPage.vue src/components/ai-form/AiCrudPageProps.js src/components/lowcode-builder/page/CrudDefaultParamsEditor.vue src/components/lowcode-builder/page/ListPageGridDesigner.vue src/components/lowcode-builder/page/StructuredListPageDesigner.vue src/components/lowcode-builder/page/GridBlockRenderer.vue src/components/lowcode-builder/page/page-schema.js src/views/app-center/components/designer/BusinessListDesigner.vue src/views/ai/crud-page.vue`：通过。
- `git diff --check -- forge-admin-ui/src/components/ai-form/AiCrudPage.vue forge-admin-ui/src/components/ai-form/AiCrudPageProps.js forge-admin-ui/src/components/lowcode-builder/page/CrudDefaultParamsEditor.vue forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/StructuredListPageDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/GridBlockRenderer.vue forge-admin-ui/src/components/lowcode-builder/page/page-schema.js forge-admin-ui/src/views/app-center/components/designer/BusinessListDesigner.vue forge-admin-ui/src/views/ai/crud-page.vue`：通过。
- `pnpm --dir forge-admin-ui build`：通过。

### 警告

- 构建保留项目既有 warning：CSS 中存在 `//` 注释。
- 构建保留项目既有 warning：`src/store/index.js` 同时被动态和静态导入，Rollup 不会将其移动到单独 chunk。

### 跳过项

- 未进行浏览器手工验证或 Playwright 截图：本轮未启动 Vite/后端服务。
- 未执行后端编译：本轮只改前端 Vue 组件、设计器配置和运行态 props。

### 服务清理

- 本轮未启动常驻服务，无需清理 PID。

## 2026-06-20 17:23:51 CST

### 变更范围

- 修复回调规则编辑器“新增规则”看似无反应的问题：编辑态归一化保留空白新行，避免新增后立即被空规则过滤逻辑删除。
- 列表设计器编辑态和预览弹窗的 `AiCrudPage` 运行 props 补齐 `crudHookRules/beforeSubmitRules` 编译结果，提交前、搜索前、加载前等参数处理与发布页保持一致。
- 保持运行态执行逻辑只执行已填写目标字段的规则，避免空白编辑行影响真实请求参数。

### 命令与结果

- `pnpm --dir forge-admin-ui exec eslint src/views/app-center/components/designer/BusinessListDesigner.vue src/components/lowcode-builder/page/CrudHookRulesEditor.vue src/components/lowcode-builder/page/crud-hook-rules.js src/components/lowcode-builder/page/ListPageGridDesigner.vue src/components/lowcode-builder/page/GridBlockRenderer.vue`：通过。
- `git diff --check -- forge-admin-ui/src/views/app-center/components/designer/BusinessListDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/CrudHookRulesEditor.vue forge-admin-ui/src/components/lowcode-builder/page/crud-hook-rules.js forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/GridBlockRenderer.vue`：通过。
- `pnpm --dir forge-admin-ui build`：通过。

### 警告

- 构建保留项目既有 warning：CSS 中存在 `//` 注释。
- 构建保留项目既有 warning：`src/store/index.js` 同时被动态和静态导入，Rollup 不会将其移动到单独 chunk。

### 跳过项

- 未进行浏览器手工验证或 Playwright 截图：本轮未启动 Vite/后端服务。
- 未执行后端编译：本轮只改前端 Vue 设计器预览和共享回调规则编辑器。

### 服务清理

- 本轮未启动常驻服务，无需清理 PID。

## 2026-06-19 21:29:05 CST

### 变更范围

- 修复左树右表运行态边界：右侧 `AiCrudPage` 运行 props 清空 `treeConfig` 与 `options.treeConfig`，避免右表误进入树表模式。
- `tree-panel` 运行态继续使用独立 `apiConfig.tree` 加载树数据；树节点选择只通过 `publicParams` 给右侧列表追加过滤字段。
- 简化筛选树属性面板，移除“高级联动 / 生成联动事件”入口，改为说明树接口和右表过滤字段的关系。
- 保留预览弹窗 `.n-card__content` / `.n-card-content` 的全高 flex 与滚动约束。

### 命令与结果

- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm --dir forge-admin-ui exec eslint ...`：未执行到 eslint，`nvm use v20.19.0` 返回 `N/A: version "N/A" is not yet installed`。
- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/GridBlockRenderer.vue src/components/lowcode-builder/page/ListPageGridDesigner.vue src/views/app-center/components/designer/BusinessListDesigner.vue src/views/ai/crud-page.vue`：通过。
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/GridBlockRenderer.vue forge-admin-ui/src/views/app-center/components/designer/BusinessListDesigner.vue forge-admin-ui/src/views/ai/crud-page.vue`：通过。
- `pnpm --dir forge-admin-ui build`：通过。

### 警告

- 构建保留项目既有 warning：CSS 中存在 `//` 注释。
- 构建保留项目既有 warning：`src/store/index.js` 同时被动态和静态导入，Rollup 不会将其移动到单独 chunk。

### 跳过项

- 未进行浏览器手工验证或 Playwright 截图：本轮未启动 Vite/后端服务。
- 未执行后端编译：本轮只改前端 Vue 运行态和设计器配置面板。

### 服务清理

- 本轮未启动常驻服务，无需清理 PID。

## 2026-06-20 09:24:00 CST

### 变更范围

- 列表设计器编辑态和预览弹窗的 `AiCrudPage` 改为接入当前 `pageSchema.zones.edit` 编译出的运行态表单配置，新增/编辑弹窗优先使用表单设计器同步出的 `fieldSettings`、`formLayout`、校验规则、组件 props 和布局参数。
- `GridBlockRenderer` 的运行态 CRUD 预览改为合并当前区块自身配置，避免传入统一预览 props 后覆盖自由画布里单个 CRUD 区块的接口、按钮、布局和回调配置。
- 自由画布 `配置组件` 中 AiCrudPage 的基础路径和接口地址在未手动配置时显示按当前模型生成的默认接口。
- 回调规则编辑器按钮显式设置 `attr-type="button"` 并阻止外层表单默认行为；规则更新先规范化并立即写入本地状态，再通知父级。

### 命令与结果

- `pnpm --dir forge-admin-ui exec eslint src/views/app-center/components/designer/BusinessListDesigner.vue src/components/lowcode-builder/page/ListPageGridDesigner.vue src/components/lowcode-builder/page/GridBlockRenderer.vue src/components/lowcode-builder/page/CrudHookRulesEditor.vue`：通过。
- `git diff --check -- forge-admin-ui/src/views/app-center/components/designer/BusinessListDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/GridBlockRenderer.vue forge-admin-ui/src/components/lowcode-builder/page/CrudHookRulesEditor.vue`：通过。
- `pnpm --dir forge-admin-ui build`：通过。

### 警告

- 构建保留项目既有 warning：CSS 中存在 `//` 注释。
- 构建保留项目既有 warning：`src/store/index.js` 同时被动态和静态导入，Rollup 不会将其移动到单独 chunk。

### 跳过项

- 未进行浏览器手工验证或 Playwright 截图：本轮未启动 Vite/后端服务。
- 未执行后端编译：本轮只改前端 Vue 设计器预览和配置面板。

### 服务清理

- 本轮未启动常驻服务，无需清理 PID。

## 2026-06-20 09:05:00 CST

### 变更范围

- 修复树模块折叠后内部 `grid-block` 仍按保存的固定宽度渲染的问题：折叠状态强制组件根节点宽度跟随父级 44px。
- 将整块树折叠按钮从模块外沿移回树模块内部右上角，避免被外层 `overflow:hidden` 裁切或被右侧 CRUD 覆盖。
- 调整树标题区布局，给折叠按钮预留空间，并把树节点展开/收起按钮做成更紧凑的横向按钮组。

### 命令与结果

- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/GridBlockRenderer.vue src/components/lowcode-builder/page/ListPageGridDesigner.vue`：通过。
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/page/GridBlockRenderer.vue forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue`：通过。
- `pnpm --dir forge-admin-ui build`：通过。

### 警告

- 构建保留项目既有 warning：CSS 中存在 `//` 注释。
- 构建保留项目既有 warning：`src/store/index.js` 同时被动态和静态导入，Rollup 不会将其移动到单独 chunk。

### 跳过项

- 未进行浏览器手工验证或 Playwright 截图：本轮未启动 Vite/后端服务。
- 未执行后端编译：本轮只改前端 Vue 设计器树模块样式和折叠宽度。

### 服务清理

- 本轮未启动常驻服务，无需清理 PID。

## 2026-06-20 08:57:20 CST

### 变更范围

- 将左侧筛选树的“收起面板”改为区块级折叠：折叠后树区块宽度压缩到 44px，并保留竖向侧栏提示。
- 父级画布接收 `treePanelCollapseChange`，在只读预览/运行态下重算同一行右侧区块位置和宽度，让 CRUD 列表真正补满树释放的空间。
- 树节点展开/收起按钮继续保留在树标题内；整块树折叠改为模块边缘箭头按钮，避免与节点展开收起混淆。
- 修复 CRUD 快速配置搜索区域：配置面板改为“头部 + 搜索 + 滚动内容”三行布局，搜索区域有稳定白色背景和分隔阴影。
- 自由画布配置组件搜索区域补齐固定高度、白底和层级，避免遮挡配置内容。
- 预览弹窗内容区、画布容器、滚动区和内部网格统一白底并撑满高度，避免弹窗下半截透明/无背景。
- 回调参数处理编辑器改为单列规则输入，降低窄面板内输入框横向溢出的概率。

### 命令与结果

- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/GridBlockRenderer.vue src/components/lowcode-builder/page/ListPageGridDesigner.vue src/components/lowcode-builder/page/StructuredListPageDesigner.vue src/components/lowcode-builder/page/CrudHookRulesEditor.vue src/views/app-center/components/designer/BusinessListDesigner.vue`：通过。
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/page/GridBlockRenderer.vue forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/StructuredListPageDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/CrudHookRulesEditor.vue forge-admin-ui/src/views/app-center/components/designer/BusinessListDesigner.vue`：通过。
- `pnpm --dir forge-admin-ui build`：通过。

### 警告

- 构建保留项目既有 warning：CSS 中存在 `//` 注释。
- 构建保留项目既有 warning：`src/store/index.js` 同时被动态和静态导入，Rollup 不会将其移动到单独 chunk。

### 跳过项

- 未进行浏览器手工验证或 Playwright 截图：本轮未启动 Vite/后端服务。
- 未执行后端编译：本轮只改前端 Vue 设计器和预览样式。

### 服务清理

- 本轮未启动常驻服务，无需清理 PID。

## 2026-06-19 22:42:11 CST

### 变更范围

- 抽出共享 `CrudHookRulesEditor.vue` 和 `crud-hook-rules.js`，`CRUD 快速配置` 与自由画布 `AiCrudPage` 属性面板统一复用同一套回调参数处理配置。
- 回调配置从单一 `beforeSubmitRules` 扩展为统一 `crudHookRules`，覆盖加载列表前、搜索前、打开表单前、打开详情前、提交前、构建提交数据后等对象参数处理场景。
- 运行页 `crud-page.vue` 编译 `crudHookRules` 为真实 `AiCrudPage` hook，并从发布画布的 `AiCrudPage.props` 兜底读取；旧 `beforeSubmitRules` 自动兼容迁移。
- 统一 `CRUD 快速配置` 与自由画布 `配置组件` 搜索样式。
- 简化 `CRUD 快速配置` 面板布局：窄面板表单默认一列、开关两列、复杂行纵向排列，避免输入框横向溢出。

### 命令与结果

- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/CrudHookRulesEditor.vue src/components/lowcode-builder/page/crud-hook-rules.js src/components/lowcode-builder/page/GridBlockRenderer.vue src/components/lowcode-builder/page/ListPageGridDesigner.vue src/components/lowcode-builder/page/StructuredListPageDesigner.vue src/components/lowcode-builder/page/page-schema.js src/views/ai/crud-page.vue`：通过。
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/page/CrudHookRulesEditor.vue forge-admin-ui/src/components/lowcode-builder/page/crud-hook-rules.js forge-admin-ui/src/components/lowcode-builder/page/GridBlockRenderer.vue forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/StructuredListPageDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/page-schema.js forge-admin-ui/src/views/ai/crud-page.vue`：通过。
- `pnpm --dir forge-admin-ui build`：通过。

### 警告

- 构建保留项目既有 warning：CSS 中存在 `//` 注释。
- 构建保留项目既有 warning：`src/store/index.js` 同时被动态和静态导入，Rollup 不会将其移动到单独 chunk。

### 跳过项

- 未进行浏览器手工验证或 Playwright 截图：本轮未启动 Vite/后端服务。
- 未执行后端编译：本轮只改前端 Vue 运行态和设计器配置面板。

### 服务清理

- 本轮未启动常驻服务，无需清理 PID。

## 2026-06-19 22:13:19 CST

### 变更范围

- 筛选树区块增加设计态和运行态的展开/收起控制，运行态 `n-tree` 改为受控 `expandedKeys`。
- 修复 CRUD 快速配置中左树右表预览容器：去掉右侧 CRUD 硬最小宽度，补齐 `min-width: 0` 与内部滚动约束，避免右侧内容显示不全。
- `CRUD 快速配置` 增加配置搜索；自由画布右侧 `配置组件` 增加配置搜索。
- 增加可视化 `beforeSubmitRules` 配置，支持提交前设置固定值、复制字段、尾部拼接和清空字段；运行页编译为 `AiCrudPage.beforeSubmit`。
- 设计态 AiCrudPage 新增/编辑预览优先使用当前对象完整字段集合，减少和应用页表单内容不一致的问题。

### 命令与结果

- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/GridBlockRenderer.vue src/components/lowcode-builder/page/ListPageGridDesigner.vue src/components/lowcode-builder/page/StructuredListPageDesigner.vue src/components/lowcode-builder/page/page-schema.js src/views/ai/crud-page.vue`：通过。
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/page/GridBlockRenderer.vue forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/StructuredListPageDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/page-schema.js forge-admin-ui/src/views/ai/crud-page.vue`：通过。
- `pnpm --dir forge-admin-ui build`：通过。

### 警告

- 构建保留项目既有 warning：CSS 中存在 `//` 注释。
- 构建保留项目既有 warning：`src/store/index.js` 同时被动态和静态导入，Rollup 不会将其移动到单独 chunk。

### 跳过项

- 未进行浏览器手工验证或 Playwright 截图：本轮未启动 Vite/后端服务。
- 未执行后端编译：本轮只改前端 Vue 运行态和设计器配置面板。

### 服务清理

- 本轮未启动常驻服务，无需清理 PID。

## 2026-06-19 21:38:58 CST

### 变更范围

- 针对已发布 `listGridLayout.layoutType = tree-crud` 但运行配置顶层 `layoutType` 仍为 `simple-crud` 的场景，运行页改为优先使用当前画布布局类型。
- `crudProps` 的树表判断改为读取有效布局类型，避免左树右表右侧列表继续把 `apiConfig.list` 替换成 `apiConfig.tree`。
- 删除 `BusinessListDesigner.vue` 中残留的 `.n-card-content { background: red; }` 调试样式。

### 命令与结果

- `pnpm --dir forge-admin-ui exec eslint src/views/ai/crud-page.vue src/components/lowcode-builder/page/ListPageGridDesigner.vue src/components/lowcode-builder/page/GridBlockRenderer.vue src/views/app-center/components/designer/BusinessListDesigner.vue`：通过。
- `git diff --check -- forge-admin-ui/src/views/ai/crud-page.vue forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/GridBlockRenderer.vue forge-admin-ui/src/views/app-center/components/designer/BusinessListDesigner.vue code-copilot/changes/forge-list-designer-productivity-upgrades/tasks.md code-copilot/changes/forge-list-designer-productivity-upgrades/test-spec.md code-copilot/changes/forge-list-designer-productivity-upgrades/execution-log.md`：通过。
- `pnpm --dir forge-admin-ui build`：通过。

### 警告

- 构建保留项目既有 warning：CSS 中存在 `//` 注释。
- 构建保留项目既有 warning：`src/store/index.js` 同时被动态和静态导入，Rollup 不会将其移动到单独 chunk。

### 服务清理

- 本轮未启动常驻服务，无需清理 PID。
