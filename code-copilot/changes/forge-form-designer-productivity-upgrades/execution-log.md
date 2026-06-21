# 执行日志：forge-form-designer-productivity-upgrades

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
