# 执行日志：forge-form-designer-productivity-upgrades

## 2026-06-21 15:28:04 CST

### 变更范围

- 修复从左侧组件库/字段资产拖入栅格、表格内部投放区时，`dragover` 阶段可能读不到 `dataTransfer.getData(...)` payload，导致预览误判“不支持”的问题。
- `designerDragState` 新增共享的拖拽预览组件状态；左侧 `ForgeFieldShelf` 在字段、组件模板、布局拖拽开始时写入预览对象，拖拽结束时清理。
- `ForgeFormCanvasNode` 在解析拖拽组件时优先使用共享预览对象，让左侧拖入和画布内移动使用同一套 slot 命中、错误清除和落点高亮逻辑。

### 命令与结果

- `pnpm --dir forge-admin-ui exec eslint src/views/app-center/components/designer/forge-form-designer/ForgeFieldShelf.vue src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvasNode.vue src/views/app-center/components/designer/forge-form-designer/designerDragState.js`：通过。
- `git diff --check -- forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFieldShelf.vue forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvasNode.vue forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/designerDragState.js`：通过。
- `pnpm --dir forge-admin-ui build`：通过。

### 警告

- 构建保留项目既有 warning：CSS 中存在 `//` 注释。
- 构建保留项目既有 warning：`src/store/index.js` 同时被动态和静态导入，Rollup 不会将其移动到单独 chunk。

### 跳过项

- 未进行浏览器手工验证或 Playwright 截图：本轮未启动 Vite/后端服务。
- 未执行后端编译：本轮只改前端 Vue 表单设计器拖拽预览状态和命中逻辑。

### 服务清理

- 本轮未启动常驻服务，无需清理 PID。

## 2026-06-21 15:21:57 CST

### 变更范围

- 修复栅格/表格内部投放区拖拽预览与实际 drop 不一致：拖拽进入容器时先解析真实 DOM 命中的内部 slot，再回退到最近列/单元格推断。
- 调整 `handleInsideDragOver` 校验顺序：如果已经解析到可接收的内部 slot，直接清除错误并高亮该 slot，不再先按外层容器报“不支持”。
- 拖拽高亮 `dropKey` 改为优先指向具体栅格列/表格单元格的 `:inside`，让灰色/蓝色落点跟随“拖入字段或布局”块，而不是显示到外层容器。

### 命令与结果

- `pnpm --dir forge-admin-ui exec eslint src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvasNode.vue`：通过。
- `git diff --check -- forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvasNode.vue`：通过。
- `pnpm --dir forge-admin-ui build`：通过。

### 警告

- 构建保留项目既有 warning：CSS 中存在 `//` 注释。
- 构建保留项目既有 warning：`src/store/index.js` 同时被动态和静态导入，Rollup 不会将其移动到单独 chunk。

### 跳过项

- 未进行浏览器手工验证或 Playwright 截图：本轮未启动 Vite/后端服务。
- 未执行后端编译：本轮只改前端 Vue 表单设计器拖拽预览命中逻辑。

### 服务清理

- 本轮未启动常驻服务，无需清理 PID。

## 2026-06-21 15:15:26 CST

### 变更范围

- 修复表单画布缩放后拖拽镜像变形：拖拽浮层使用节点未缩放的布局宽高克隆，再按当前画布缩放比例整体缩放，避免输入框和内部布局被压扁。
- 表单画布节点开始拖拽时派发设计器拖拽事件，父级收到后自动收起右侧属性面板，释放中间画布空间。
- 表单设计器右侧属性栏收起后第三列宽度改为 0，并隐藏右侧容器，不再保留空白窄栏。
- 表单设计器外层高度改为跟随父级确定高度；业务表单设计区域改为 `height: calc(100vh - 106px)`，内部使用 `minmax(0, 1fr)` 和 `overflow: hidden`，让滚动集中到左右面板和画布内部，行为更接近列表设计器。

### 命令与结果

- `pnpm --dir forge-admin-ui exec eslint src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvasNode.vue src/views/app-center/components/designer/forge-form-designer/ForgeFormDesigner.vue src/views/app-center/components/designer/BusinessFormDesigner.vue`：通过。
- `git diff --check -- forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvasNode.vue forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFormDesigner.vue forge-admin-ui/src/views/app-center/components/designer/BusinessFormDesigner.vue`：通过。
- `pnpm --dir forge-admin-ui build`：通过。

### 警告

- 构建保留项目既有 warning：CSS 中存在 `//` 注释。
- 构建保留项目既有 warning：`src/store/index.js` 同时被动态和静态导入，Rollup 不会将其移动到单独 chunk。

### 跳过项

- 未进行浏览器手工验证或 Playwright 截图：本轮未启动 Vite/后端服务。
- 未执行后端编译：本轮只改前端 Vue 表单设计器拖拽交互、三栏布局和 scoped 样式。

### 服务清理

- 本轮未启动常驻服务，无需清理 PID。

## 2026-06-21 13:22:04 CST

### 变更范围

- 表单画布底部 dock 新增“预览视口和设计宽度”图标入口，交互与列表设计器保持一致。
- 支持桌面、窄屏、弹窗、抽屉、移动端预设，并提供 390/720/768/960/1200/1366/1440/1920 和自定义宽度。
- 表单画布舞台改为按当前设计宽度渲染，并继续保留独立缩放控制和 `Ctrl/⌘ + 滚轮` 缩放。
- 设计宽度作为设计器视口状态维护，不写入表单 schema，避免影响运行态响应式表单协议。

### 命令与结果

- `pnpm --dir forge-admin-ui exec eslint src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvas.vue`：通过。
- `git diff --check -- forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvas.vue`：通过。
- `pnpm --dir forge-admin-ui build`：通过。

### 警告

- 构建保留项目既有 warning：CSS 中存在 `//` 注释。
- 构建保留项目既有 warning：`src/store/index.js` 同时被动态和静态导入，Rollup 不会将其移动到单独 chunk。

### 跳过项

- 未进行浏览器手工验证或 Playwright 截图：本轮未启动 Vite/后端服务。
- 未执行后端编译：本轮只改前端 Vue 表单画布视口控制和 scoped 样式。

### 服务清理

- 本轮未启动常驻服务，无需清理 PID。

## 2026-06-21 13:04:44 CST

### 变更范围

- 表单画布节点 resize 锚点从右、下、右下扩展为上、右、下、左和四个角八向锚点。
- 结构槽位选中时取消外层节点蓝框和阴影，只在内部“拖入字段或布局”投放区显示选中边框，避免出现两个选中框。
- 结构槽位纵向 resize 的高度写入内部投放区 CSS 变量，对应真实允许拖入组件的区域；栅格/表格布局的内部投放区也可继承该最小高度。
- 左侧/上侧锚点按反向拖拽计算宽高，保持表单流式布局不移动节点位置。

### 命令与结果

- `pnpm --dir forge-admin-ui exec eslint src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvasNode.vue`：通过。
- `git diff --check -- forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvasNode.vue`：通过。
- `pnpm --dir forge-admin-ui build`：通过。

### 警告

- 构建保留项目既有 warning：CSS 中存在 `//` 注释。
- 构建保留项目既有 warning：`src/store/index.js` 同时被动态和静态导入，Rollup 不会将其移动到单独 chunk。

### 跳过项

- 未进行浏览器手工验证或 Playwright 截图：本轮未启动 Vite/后端服务。
- 未执行后端编译：本轮只改前端 Vue 设计器节点交互和 scoped 样式。

### 服务清理

- 本轮未启动常驻服务，无需清理 PID。

## 2026-06-21 12:55:17 CST

### 变更范围

- 补强布局组件选中态：栅格、表格、卡片、标签页、折叠面板及结构槽位选中后显示明确蓝色边框和阴影，不再被默认轻边框覆盖。
- 选中画布节点后显示右、下、右下三个 resize 锚点；横向拖拽优先调整表单栅格 span，纵向拖拽写入设计态 `minHeight`。
- 移动已有组件时复用内部槽位命中逻辑，拖到栅格/表格内部时按鼠标位置选择最近列或单元格。
- 组件更多菜单的“背景描边”增加“隐藏边框”，用于隐藏默认边框，同时选中态仍保留编辑反馈。

### 命令与结果

- `pnpm --dir forge-admin-ui exec eslint src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvasNode.vue`：通过。
- `git diff --check -- forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvasNode.vue`：通过。
- `pnpm --dir forge-admin-ui build`：通过。

### 警告

- 构建保留项目既有 warning：CSS 中存在 `//` 注释。
- 构建保留项目既有 warning：`src/store/index.js` 同时被动态和静态导入，Rollup 不会将其移动到单独 chunk。

### 跳过项

- 未进行浏览器手工验证或 Playwright 截图：本轮未启动 Vite/后端服务。
- 未执行后端编译：本轮只改前端 Vue 设计器节点交互和 scoped 样式。

### 服务清理

- 本轮未启动常驻服务，无需清理 PID。

## 2026-06-21 15:40:54 CST

### 变更范围

- 修复表格布局“不显示边框”只影响设计器外层节点、不影响真实运行类的问题；`AiFormLayoutNodes.vue` 现在会读取节点 `props.__designerStyle`，并把 `hideInnerBorder` / `borderStyle: none` / `props.bordered === false` 统一映射为 `is-borderless`。
- `af-layout-table.is-borderless` 会隐藏表格外边框，并通过 scoped `:deep` 压掉子级 `af-layout-table-cell` 的右边框和下边框。
- `af-layout-table-cell.is-borderless` 单独配置无边框时也会隐藏自身单元格边框。
- 兼容 `fcTable` / `fcTableGrid` 运行节点类型，避免 schema 转换后表格布局在公共 AiForm 渲染中识别不一致。
- 顺手修正公共布局节点对部分属性的透传：卡片运行态不再硬编码 `bordered=true`，标签页运行态兼容设计器使用的 `placement` 属性，卡片/标签页/折叠面板/表格会读取设计器样式。

### 命令与结果

- `pnpm --dir forge-admin-ui exec eslint src/components/ai-form/AiFormLayoutNodes.vue src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvasNode.vue src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue`：通过。
- `git diff --check -- forge-admin-ui/src/components/ai-form/AiFormLayoutNodes.vue forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvasNode.vue forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue`：通过。
- `pnpm --dir forge-admin-ui build`：通过。

### 警告

- 构建保留项目既有 warning：CSS 中存在 `//` 注释。
- 构建保留项目既有 warning：`src/store/index.js` 同时被动态和静态导入，Rollup 不会将其移动到单独 chunk。

### 跳过项

- 未进行浏览器手工验证或 Playwright 截图：本轮未启动 Vite/后端服务。
- 未执行后端编译：本轮只改前端 Vue 设计器和公共 AiForm 布局渲染。

### 服务清理

- 本轮未启动常驻服务，无需清理 PID。

## 2026-06-21 12:45:37 CST

### 变更范围

- 修复拖到“拖入字段或布局”空槽时外层容器先命中导致的误报：栅格布局按鼠标位置选择最近栅格列，表格布局按鼠标位置选择最近表格单元格。
- 拖拽落点高亮改为落在具体内部槽位上，不再优先显示到外层 before/after 灰色占位块。
- 非法拖拽提示从文档流 sticky 条改为画布悬浮提示，避免提示出现/消失时挤压画布造成抖动。
- 布局容器默认隐藏大部分设计边框，空槽保留轻量提示，hover、选中和投放状态才显示明确边界。

### 命令与结果

- `pnpm --dir forge-admin-ui exec eslint src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvas.vue src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvasNode.vue`：通过。
- `git diff --check -- forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvas.vue forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvasNode.vue`：通过。
- `pnpm --dir forge-admin-ui build`：通过。

### 警告

- 构建保留项目既有 warning：CSS 中存在 `//` 注释。
- 构建保留项目既有 warning：`src/store/index.js` 同时被动态和静态导入，Rollup 不会将其移动到单独 chunk。

### 跳过项

- 未进行浏览器手工验证或 Playwright 截图：本轮未启动 Vite/后端服务。
- 未执行后端编译：本轮只改前端 Vue 设计器拖拽命中、提示层和 scoped 样式。

### 服务清理

- 本轮未启动常驻服务，无需清理 PID。

## 2026-06-21 12:31:03 CST

### 变更范围

- 表单画布拆出 `canvas-scroll` 内部滚动层，外层保持固定视口，缩放控件改为画布右下角绝对定位，避免中间画布和外层页面双滚动时控件被滚走或遮住。
- 布局组件的结构槽位（栅格列、表格单元格、标签面板、折叠项）不再显示复制/删除/更多操作浮层，避免空槽位被当成普通组件误删。
- 卡片、CRUD 区块、栅格列、表格单元格、标签面板、折叠项等内部投放区统一处理 `dragenter/dragover/drop`，减少拖入字段或布局时命中外层节点导致的偶发失败。
- 降低布局组件和空占位区的多层边框，空占位改为轻量背景提示，保留 active 投放反馈。

### 命令与结果

- `pnpm --dir forge-admin-ui exec eslint src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvas.vue src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvasNode.vue`：通过。
- `git diff --check -- forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvas.vue forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvasNode.vue`：通过。
- `pnpm --dir forge-admin-ui build`：通过。

### 警告

- 构建保留项目既有 warning：CSS 中存在 `//` 注释。
- 构建保留项目既有 warning：`src/store/index.js` 同时被动态和静态导入，Rollup 不会将其移动到单独 chunk。

### 跳过项

- 未进行浏览器手工验证或 Playwright 截图：本轮未启动 Vite/后端服务。
- 未执行后端编译：本轮只改前端 Vue 设计器布局、拖拽命中和 scoped 样式。

### 服务清理

- 本轮未启动常驻服务，无需清理 PID。

## 2026-06-21 12:12:25 CST

### 变更范围

- 表单画布缩放控制从常驻面板改为底部图标按钮，点击后弹出缩放选择和加减按钮。
- 保留 `Ctrl/⌘ + 滚轮` 缩放，普通滚轮继续滚动画布。
- 缩放状态仍只保存在当前组件会话内，不写入 `FormDesignerSchema`。

### 命令与结果

- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/ListPageGridDesigner.vue src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvas.vue src/views/app-center/components/designer/BusinessListDesigner.vue`：通过。
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvas.vue forge-admin-ui/src/views/app-center/components/designer/BusinessListDesigner.vue`：通过。
- `pnpm --dir forge-admin-ui build`：通过。

### 警告

- 构建保留项目既有 warning：CSS 中存在 `//` 注释。
- 构建保留项目既有 warning：`src/store/index.js` 同时被动态和静态导入，Rollup 不会将其移动到单独 chunk。

### 跳过项

- 未进行浏览器手工验证或 Playwright 截图：本轮未启动 Vite/后端服务。
- 未执行后端编译：本轮只改前端 Vue 设计器布局与缩放交互。

### 服务清理

- 本轮未启动常驻服务，无需清理 PID。

## 2026-06-21 12:00:09 CST

### 变更范围

- 表单画布新增右上角浮动“画布缩放”控制，支持 67% / 75% / 90% / 100% / 110% / 125%。
- 表单画布支持 `Ctrl/⌘ + 滚轮` 缩放，普通滚轮仍保留滚动画布行为。
- 缩放状态只保存在当前组件会话内，不写入 `FormDesignerSchema`，不影响保存/发布协议。

### 命令与结果

- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvas.vue`：通过。
- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/ListPageGridDesigner.vue src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvas.vue`：通过。
- `pnpm --dir forge-admin-ui build`：通过。

### 警告

- 构建保留项目既有 warning：CSS 中存在 `//` 注释。
- 构建保留项目既有 warning：`src/store/index.js` 同时被动态和静态导入，Rollup 不会将其移动到单独 chunk。

### 跳过项

- 未进行浏览器手工验证或 Playwright 截图：本轮未启动 Vite/后端服务。
- 未执行后端编译：本轮只改前端 Vue 设计器布局与缩放交互。

### 服务清理

- 本轮未启动常驻服务，无需清理 PID。
